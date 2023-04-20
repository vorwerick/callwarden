package cz.dzubera.callwarden.service

import cz.dzubera.callwarden.model.Call
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class ServiceReceiver {

    companion object {
        var ex: ExecutorService? = null
        fun initialize() {
            if(ex == null){
                ex = Executors.newSingleThreadExecutor()
            }
        }

//        var currentCall: CurrentCall? = null

    }

//    class CurrentCall(val direction: Call.Direction) {
//
//        var callStarted: Long = 0
//        var callEnded: Long = 0
//        var callAccepted: Long? = 0
//        var isEnded: Boolean = false
//
//    }


}