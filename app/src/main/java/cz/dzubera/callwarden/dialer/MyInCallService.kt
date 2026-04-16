package cz.dzubera.callwarden.dialer

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.compose.runtime.mutableStateOf

class MyInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallHolder.currentCall = call
        CallHolder.inCallService = this // Uložíme referenci na službu

        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION) // Důležité: hovor není akce uživatele
        }
        startActivity(intent)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallHolder.audioState.value = audioState
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallHolder.currentCall = null
        CallHolder.inCallService = null
        CallHolder.audioState.value = null
    }
}

object CallHolder {
    var currentCall: Call? = null
    var inCallService: MyInCallService? = null

    // Pro reaktivní UI v Compose
    val audioState = mutableStateOf<CallAudioState?>(null)
}