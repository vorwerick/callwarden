package cz.dzubera.callwarden

import android.os.Bundle
import android.telecom.*
import android.telecom.Call
import android.util.Log


class MyService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        Log.d("LLLLLL", "started")
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("LLLLLL", "removed")

        call.unregisterCallback(callCallback)
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.d("LLLLLL", state.toString())
        }
    }

}