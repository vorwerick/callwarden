package cz.dzubera.callwarden.ui.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent


class NotificationActivity : AppCompatActivity() {


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
            handleUrl(this, intent.extras?.getString("url").toString())
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
            handleUrl(this, intent?.extras?.getString("url").toString())
        }

    }

    fun handleUrl(context: Context, url: String) {
        val customSchemeUrl = url.replace("https://", "ramisyscrm://")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(customSchemeUrl))

        val canOpenCustom = intent.resolveActivity(packageManager) != null

        if (canOpenCustom) {
            startActivity(intent)
            Log.d("BLABLA", "Opened in other app via custom scheme: $customSchemeUrl")
        } else {
            openUrlInCustomTab(context, url)
            Log.d("BLABLA", "Opened in Custom Tab: $url")
        }

        finish()
    }


}
