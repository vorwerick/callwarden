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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val credentials = PreferencesUtils.loadCredentials(this)
        if (credentials != null) {
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
        supportActionBar?.title = "Příhlášení";

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
                    "Zadejte doménu a uživetlské id"
            } else {
                login(domainId, userId)
            }
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
                    401 -> {
                        runOnUiThread {
                            findViewById<TextView>(R.id.error_label).text =
                                "Neznámé ID nebo uživatel " + response.code
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