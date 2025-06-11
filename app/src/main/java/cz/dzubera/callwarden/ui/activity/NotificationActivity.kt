package cz.dzubera.callwarden.ui.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
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


class NotificationActivity : AppCompatActivity() {

    companion object{

    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        ContextCompat.startActivity(context, intent, null)
        finish()
    }

    private fun openUrlInCustomTab(context: Context, url: String) {
        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
        Log.d("BLABLA", "OPENED")
        finish()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val url = intent.extras?.getString("url") ?: intent.getStringExtra("url")
        if (url != null) {
            Log.d("BLABLA", "HAS URL")
            openUrlInCustomTab(this, intent.extras?.getString("url").toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.extras?.getString("url") ?: intent?.getStringExtra("url")
        if (url != null) {
            Log.d("BLABLA", "HAS URL")
            openUrlInCustomTab(this, intent?.extras?.getString("url").toString())
        }

    }



}
