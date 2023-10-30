package cz.dzubera.callwarden.ui.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Credentials
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.HttpResponse
import cz.dzubera.callwarden.service.ResponseStatus
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.PowerSaveUtils
import cz.dzubera.callwarden.utils.PreferencesUtils


class LoginActivity : AppCompatActivity() {

    companion object{
        val permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CALL_LOG,
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credentials = PreferencesUtils.loadCredentials(this)

        val hasPermissions =checkPermissions()


        if (credentials != null && hasPermissions) {
            HttpRequest.getProjects(
                credentials.domain,
                credentials.user
            ) { response: HttpResponse ->
                if (response.status == ResponseStatus.SUCCESS) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.d(javaClass.name, "Credential failed")
                }
            }
        }

        setContentView(R.layout.login_activity)
        supportActionBar?.title = "";


        if (credentials != null) {
            findViewById<EditText>(R.id.domain_id).setText(credentials.domain)
            findViewById<EditText>(R.id.user_id).setText(credentials.user.toString())
        }


        val loginButton = findViewById<Button>(R.id.user_log_in)
        loginButton.setOnClickListener {
            val domainId = findViewById<EditText>(R.id.domain_id).text.toString()
            val userId = findViewById<EditText>(R.id.user_id).text.toString().toIntOrNull()
            if (domainId.isEmpty() || userId == null) {
                findViewById<TextView>(R.id.error_label).text =
                    "Zadejte ID a uživatele"
            } else {
                login(domainId, userId)
            }
        }
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
            if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_DENIED
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
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
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

    private fun checkPermissions(): Boolean {
        return if (isPermissionsGranted() != PackageManager.PERMISSION_GRANTED) {
            showAlert()
            false
        } else {
            true
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
                PreferencesUtils.saveCredentials(this, Credentials(domain, user))
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
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
                    in 500 .. 599 -> {
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