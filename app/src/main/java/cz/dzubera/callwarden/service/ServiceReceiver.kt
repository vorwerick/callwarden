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
    }
}