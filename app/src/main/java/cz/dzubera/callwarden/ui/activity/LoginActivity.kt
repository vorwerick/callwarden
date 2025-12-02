package cz.dzubera.callwarden.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.messaging.FirebaseMessaging
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Credentials
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.HttpResponse
import cz.dzubera.callwarden.service.ResponseStatus
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.PreferencesUtils


class LoginActivity : AppCompatActivity() {

    companion object {
        val permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                listOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC,
                    Manifest.permission.FOREGROUND_SERVICE
                )
            } else {
                listOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.WRITE_CALL_LOG,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        } else {
            listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG
            )
        }

        var fcmToken: String? = null
    }


    var shown = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Uložíme stav, zda byla stránka otevřena
        outState.putBoolean("shown", shown)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)
        val view = findViewById<ConstraintLayout>(R.id.mainLayout)
        view.visibility = View.GONE

        shown = savedInstanceState?.getBoolean("shown") ?: false

        var urlToSend: String? = null

        val opened = savedInstanceState?.getBoolean("opened") ?: false
        if (!shown && !opened) {
            val url = intent?.extras?.getString("url") ?: intent?.getStringExtra("url")
            if (url != null) {
                shown = true
                urlToSend = url
                intent?.extras?.remove("url")
                intent?.removeExtra("url")
                savedInstanceState?.putBoolean("opened", true)
            }
        }



        Log.d("DODOD", "ON CREATE")
        val credentials = PreferencesUtils.loadCredentials(this)

        //requestBatteryOptimizationPermission(this)
        val hasPermissions = checkPermissions()

        if (credentials != null && hasPermissions) {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
                PreferencesUtils.save(this@LoginActivity, "firebase_token", fcmToken)
                //HttpRequest.sendToken(credentials.domain, credentials.user,fcmToken)
            }

            HttpRequest.getProjects(
                credentials.domain, credentials.user
            ) { response: HttpResponse ->
                runOnUiThread {
                    if (response.status == ResponseStatus.SUCCESS) {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra("url", urlToSend)
                            extras?.putString("url", urlToSend)
                        }
                        startActivity(intent)
                    } else {
                        Log.d(javaClass.name, "Credential failed")
                        view.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            view.visibility = View.VISIBLE
        }




        if (credentials != null) {
            findViewById<EditText>(R.id.domain_id).setText(credentials.domain)
            findViewById<EditText>(R.id.user_id).setText(credentials.user.toString())
        }


        val loginButton = findViewById<Button>(R.id.user_log_in)
        loginButton.setOnClickListener {
            val domainId = findViewById<EditText>(R.id.domain_id).text.toString()
            val userId = findViewById<EditText>(R.id.user_id).text.toString().toIntOrNull()
            if (domainId.isEmpty() || userId == null) {
                findViewById<TextView>(R.id.error_label).text = "Zadejte ID a uživatele"
            } else {
                login(domainId, userId)
            }
        }
        val registerButton = findViewById<TextView>(R.id.register)
        registerButton.setOnClickListener {
            openUrlInCustomTab(this, "https://www.ramisys.cz/#kontakt")
        }
    }


    private fun openUrlInCustomTab(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent, null)
    }


    // Check permissions status
    private fun isPermissionsGranted(): Int {
        // PERMISSION_GRANTED : Constant Value: 0
        // PERMISSION_DENIED : Constant Value: -1
        var counter = 0;
        for (permission in permissionList) {
            counter += ContextCompat.checkSelfPermission(this, permission)
        }
        return counter
    }


    // Find the first denied permission
    private fun deniedPermission(): String {
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) == PackageManager.PERMISSION_DENIED
            ) return permission
        }
        return ""
    }


    // Show alert dialog to request permissions
    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Oprávnění")
        builder.setMessage("Pro správné fungování aplikace je potřeba potvrdit některá oprávnění.")
        builder.setPositiveButton("Další") { dialog, which -> requestPermissions() }
        builder.setNeutralButton("Ukončit") { dialog, which -> finish() }
        val dialog = builder.create()
        dialog.show()
    }


    // Request the permissions at run time
    private fun requestPermissions() {
        val permission = deniedPermission()
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            // Show an explanation asynchronously
            Toast.makeText(this, "Should show an explanation.", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, permissionList.toTypedArray(), 29)
        }
    }


    // Process permissions result
    fun processPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ): Boolean {
        var result = 0
        if (grantResults.isNotEmpty()) {
            for (item in grantResults) {
                result += item
            }
        }
        if (result == PackageManager.PERMISSION_GRANTED) return true
        return false
    }

    fun showNewVersionDialog(url: String?, done: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Dostupná aktualizace")
        builder.setMessage("Máme pro vás novější verzi aplikace. Doporučujeme provést aktualizaci co nejdříve, pro správné fungování aplikace.")
        url?.let {
            builder.setPositiveButton("Aktualizovat") { dialog, which ->
                openUrlInCustomTab(this, url)
                dialog.dismiss()
                done()

            }
        }
        builder.setNegativeButton("Zpět") { dialog, which -> dialog.dismiss() }

        val dialog = builder.create()
        dialog.show()
    }

    fun showUpdateNeededDialog(url: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setTitle("Provést aktualizaci")
        builder.setMessage("Pro správné fungování aplikace je nutné nejprve dokončit aktualizaci.")
        url?.let {
            builder.setPositiveButton("Aktualizovat") { dialog, which ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(it)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(browserIntent)
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    fun getVersionState(
        ok: () -> Unit, newVersion: (String?) -> Unit, updateNeeded: (String?) -> Unit
    ) {
        val credentials = PreferencesUtils.loadCredentials(this)
        credentials?.let {
            HttpRequest.sendVersion(it.domain, it.user) { status ->
                println("KOKO: + ${status.url} ${status.state}")
                runOnUiThread {
                    status.state?.let { s ->
                        when (status.getState()) {
                            HttpRequest.VersionState.States.NEW_VERSION -> {
                                newVersion(status.url)
                            }

                            HttpRequest.VersionState.States.UPDATE_NEEDED -> {
                                updateNeeded(status.url)
                            }

                            else -> {
                                ok()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        getVersionState({

        }, {
            showNewVersionDialog(it) {

            }
        }, {
            showUpdateNeededDialog(it)
        })

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 29) {
            if (processPermissionsResult(requestCode, permissions, grantResults)) {
                // Permissions granted, restart the activity to proceed with normal flow
                recreate()
            } else {
                // Permissions denied, show alert again
                showAlert()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (isPermissionsGranted() != PackageManager.PERMISSION_GRANTED) {
            showAlert()
            false
        } else {
            true
        }
    }

    fun requestBatteryOptimizationPermission(context: Context) {
        try {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + context.packageName)
            )
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun login(domain: String, user: Int): Unit {
        Config.signedOut = false
        findViewById<TextView>(R.id.error_label).text = ""
        findViewById<LinearProgressIndicator>(R.id.progressbar_indicator).isIndeterminate = false
        findViewById<Button>(R.id.user_log_in).isEnabled = false
        HttpRequest.getProjects(domain, user) { response: HttpResponse ->
            if (response.status == ResponseStatus.SUCCESS) {
                println("KOKO: " + response.data.toString())
                PreferencesUtils.saveCredentials(this@LoginActivity, Credentials(domain, user))
                runOnUiThread {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                }
            } else {
                when (response.code) {
                    0 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Nelze se připojit k serveru.\nZkontrolujte připojení k internetu."
                        }
                    }

                    401 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Neznámé ID nebo uživatel"
                        }
                    }

                    422 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Uživatel nemá žádný projekt"

                        }
                    }

                    404 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                response.data ?: "Chyba serveru " + response.code

                        }
                    }

                    in 500..599 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                response.data ?: "Chyba serveru" + response.code

                        }
                    }
                }
            }
            runOnUiThread {
                findViewById<Button>(R.id.user_log_in).isEnabled = true
            }

        }
    }
}
