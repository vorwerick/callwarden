package cz.dzubera.callwarden.service

import android.telephony.BarringInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyDisplayInfo
import android.util.Log

interface PhoneStateCallback {
    fun onPhoneStateChanged(state: Int)

}

// Just wrapper

class ServicePhoneStateListener(private val callback: PhoneStateCallback) : PhoneStateListener() {

    private val tag = javaClass.name

    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
        callback.onPhoneStateChanged(state)
    }

    override fun onBarringInfoChanged(barringInfo: BarringInfo) {
        Log.d(tag, "barring: $barringInfo")
    }

    override fun onCallDisconnectCauseChanged(
        disconnectCause: Int,
        preciseDisconnectCause: Int
    ) {
        Log.d(tag,"cdcc: $disconnectCause  $preciseDisconnectCause")

    }

    override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
        Log.d(tag, "dcsc: $state")
    }

    override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
        Log.d(tag, "display: $telephonyDisplayInfo")
    }
}

/*
* original src
* */

//private val psl by lazy {
//    object : PhoneStateListener() {
//        override fun onBarringInfoChanged(barringInfo: BarringInfo) {
//            println("barring: " + barringInfo.toString())
//        }
//
//        override fun onCallDisconnectCauseChanged(
//            disconnectCause: Int,
//            preciseDisconnectCause: Int
//        ) {
//            println("cdcc: " + disconnectCause.toString() + " " + preciseDisconnectCause.toString())
//
//        }
//
//        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
//            println("dcsc: " + state.toString())
//        }
//
//        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
//            println("display: " + telephonyDisplayInfo.toString())
//        }
//
//        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
////                resolveCall(state, phoneNumber)
//        }
//    }
//}