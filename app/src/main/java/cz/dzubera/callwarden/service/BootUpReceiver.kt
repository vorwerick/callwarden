package cz.dzubera.callwarden.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build


class BootUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Intent.ACTION_BOOT_COMPLETED == intent?.action) {
            val i = Intent(context, BackgroundCallService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

    }
}