package cz.dzubera.callwarden.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

interface IdleStateCallback {
    fun onIdleCalled()
}

class IdleStateReceiverForService(private val callback: IdleStateCallback) : BroadcastReceiver() {

    private val tag = javaClass.name

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(tag, "recieved IDLE state")
        callback.onIdleCalled()
    }

    companion object {
        const val ACTION_SERVICE_IDLE_STATE: String =
            "cz.dzubera.callwarden.action.service.idle.state"
    }
}