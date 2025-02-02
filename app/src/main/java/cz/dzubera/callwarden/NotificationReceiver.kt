package cz.dzubera.callwarden

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val url = intent.getStringExtra("URL")
        if (!url.isNullOrEmpty()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        }
    }
}