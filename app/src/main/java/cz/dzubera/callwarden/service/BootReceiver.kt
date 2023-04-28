package cz.dzubera.callwarden.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.dzubera.callwarden.utils.AlarmUtils

// renew alarm after boot

class BootReceiver : BroadcastReceiver() {

    private val tag: String = javaClass.name

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "boot received")
        AlarmUtils.scheduleAlarm(context!!)
    }
}
