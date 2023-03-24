package cz.dzubera.callwarden

import java.util.concurrent.Executors


class ServiceReceiver {

    companion object {
        var currentCall: CurrentCall? = null
        val ex = Executors.newSingleThreadExecutor()
    }

    class CurrentCall(val direction: Call.Direction) {

        var phoneNumber: String = ""
        var callStarted: Long = 0
        var callEnded: Long = 0
        var callAccepted: Long? = 0
        var isEnded: Boolean = false
        var callType: Call.Type = Call.Type.MISSED

    }


}