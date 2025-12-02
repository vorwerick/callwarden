package cz.dzubera.callwarden.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


// renew alarm after boot

class BootReceiver : BroadcastReceiver() {

    private val tag: String = javaClass.name

    override fun onReceive(context: Context?, intent: Intent?) {

    }
}
