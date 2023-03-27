package cz.dzubera.callwarden.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.progressindicator.LinearProgressIndicator
import cz.dzubera.callwarden.*
import cz.dzubera.callwarden.model.Credentials
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.service.HttpResponse
import cz.dzubera.callwarden.service.ResponseStatus
import cz.dzubera.callwarden.utils.Config
import cz.dzubera.callwarden.utils.PreferencesUtils


class LoginActivity : AppCompatActivity() {

    val list = listOf(
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.GET_ACCOUNTS,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credentials = PreferencesUtils.loadCredentials(this)
        if(credentials != null){
            HttpRequest.getProjects(credentials.domain, credentials.user) { response: HttpResponse ->
                if (response.status == ResponseStatus.SUCCESS) {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                } else {


                }

            }
        }

        setContentView(R.layout.login_activity)
        supportActionBar?.title = "Příhlášení";
        checkPermissions()



        if(credentials != null){
            findViewById<EditText>(R.id.domain_id).setText(credentials.domain)
           findViewById<EditText>(R.id.user_id).setText( credentials.user.toString())
        }


        val loginButton = findViewById<Button>(R.id.user_log_in)
        loginButton.setOnClickListener {
            val domainId = findViewById<EditText>(R.id.domain_id).text.toString()
            val userId = findViewById<EditText>(R.id.user_id).text.toString().toIntOrNull()
            if(domainId.isEmpty() || userId == null){
                findViewById<TextView>(R.id.error_label).text =
                    "Zadejte doménu a uživetlské id"
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
        for (permission in list) {
            counter += ContextCompat.checkSelfPermission(this, permission)
        }
        return counter
    }


    // Find the first denied permission
    private fun deniedPermission(): String {
        for (permission in list) {
            if (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_DENIED
            ) return permission
        }
        return ""
    }


    // Show alert dialog to request permissions
    private fun showAlert() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need permission(s)")
        builder.setMessage("Some permissions are required to do the task.")
        builder.setPositiveButton("OK") { dialog, which -> requestPermissions() }
        builder.setNeutralButton("Cancel", null)
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
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 29)
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

    fun checkPermissions(): Boolean {
        if (isPermissionsGranted() != PackageManager.PERMISSION_GRANTED) {
            showAlert()
            return false
        } else {
            return true
        }
    }

    private fun login(domain: String, user: Int): Unit {
        Config.signedOut = false
        findViewById<TextView>(R.id.error_label).text = ""
        findViewById<LinearProgressIndicator>(R.id.progressbar_indicator).isIndeterminate = false
        findViewById<Button>(R.id.user_log_in).isEnabled = false
        HttpRequest.getProjects(domain, user) { response: HttpResponse ->
            if (response.status == ResponseStatus.SUCCESS) {
                println("KOKO: "+response.data.toString())
                PreferencesUtils.saveCredentials(this, Credentials(domain, user))
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                when (response.code) {
                    401 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Neznámé ID nebo uživatel "+ response.code
                        }
                    }
                    422 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Uživatel nemá žádný projekt " + response.code

                        }
                    }
                    else -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Chyba " + response.code

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