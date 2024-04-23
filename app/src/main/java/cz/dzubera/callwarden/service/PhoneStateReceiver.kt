package cz.dzubera.callwarden.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import io.sentry.Sentry
import java.util.concurrent.Executors


/* with extra state EXTRA_STATE can be recognized phone/call state
*  https://developer.android.com/reference/android/telephony/TelephonyManager#EXTRA_STATE
*  [IDLE|RINGING|OFFHOOK]
*
* If are Manifest.permission.READ_CALL_LOG and Manifest.permission.READ_PHONE_STATE used in same time
* receiver will by called twice - second is from call log with EXTRA_INCOMING_NUMBER = phone number
* https://developer.android.com/reference/android/telephony/TelephonyManager#ACTION_PHONE_STATE_CHANGED
* https://developer.android.com/reference/android/telephony/TelephonyManager#EXTRA_INCOMING_NUMBER
* */

class PhoneStateReceiver : BroadcastReceiver() {

    private val tag: String = javaClass.name
    private val phoneIntent: String = TelephonyManager.ACTION_PHONE_STATE_CHANGED

    override fun onReceive(context: Context, intent: Intent?) {

        // get extras
        // phone is coming from call log due to permission in manifest
        // phone is used to eliminate second call
        val extraState = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
        val extraPhoneNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        Log.d(tag, "Phone state received: $extraState, phone number $extraPhoneNumber")

        if (extraPhoneNumber != null && extraState != null && extraState.contains("RINGING")) {
            val i = Intent(context, IncomingCallInfoService::class.java)
            i.putExtra("phone_number", extraPhoneNumber);
            context.startService(i)

        }

        // to no start service twice
        if (!isServiceRunning(context)) {
            if (phoneIntent == intent?.action && isExtraStateValid(extraState) && extraPhoneNumber == null) {

                Log.d(tag, "Conditions for staring service ale valid")

                val i = Intent(context, BackgroundCallService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        context.startForegroundService(i)
                    } catch (e: java.lang.Exception) {
                        Log.e(tag, "Error starting service", e)
                        Sentry.addBreadcrumb(e.message.toString())
                    }

                } else {
                    context.startService(i)
                }

            } else {
                Log.d(tag, "Invalid conditions for starting service")

            }
        } else {
            Log.d(tag, "Service is already running")
            if (phoneIntent == intent?.action && extraState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Log.d(tag, "Broadcasting idle state")
                context.sendBroadcast(Intent(IdleStateReceiverForService.ACTION_SERVICE_IDLE_STATE))
            }
        }
    }

    private fun isExtraStateValid(state: String?): Boolean {
        if (state != null) {
            when (state) {
                TelephonyManager.EXTRA_STATE_OFFHOOK -> return true
                TelephonyManager.EXTRA_STATE_RINGING -> return true
            }
        }
        return false
    }

    private fun isServiceRunning(context: Context): Boolean {
        var manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager;

        for (service: RunningServiceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
            if (BackgroundCallService::class.java.name.equals(service.service.className)) {
                return true
            }
        }

        return false;
    }
}