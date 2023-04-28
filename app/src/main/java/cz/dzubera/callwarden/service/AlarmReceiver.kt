package cz.dzubera.callwarden.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

//this is called every hour

class AlarmReceiver : BroadcastReceiver() {

    val tag = javaClass.name

    companion object {
        const val requestCode = 102030
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "alarm received")

        val intent = Intent(context, BackgroundSyncService::class.java)
        context?.startService(intent)
    }
}
