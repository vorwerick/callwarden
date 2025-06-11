package cz.dzubera.callwarden.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.sentry.Sentry
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/* Modern approach for monitoring phone state changes:
*  - For Android 12+ (API 31+): Use TelephonyCallback
*  - For Android < 12: Use BroadcastReceiver with ACTION_PHONE_STATE_CHANGED
*
* If Manifest.permission.READ_CALL_LOG and Manifest.permission.READ_PHONE_STATE are used at the same time,
* receiver will be called twice - second is from call log with EXTRA_INCOMING_NUMBER = phone number
*/

class PhoneStateReceiver : BroadcastReceiver() {

    private val tag: String = javaClass.name
    private val phoneIntent: String = TelephonyManager.ACTION_PHONE_STATE_CHANGED

    // This is used for Android < 12 (API 31)
    override fun onReceive(context: Context, intent: Intent?) {
        // get extras
        // phone is coming from call log due to permission in manifest
        // phone is used to eliminate second call
        val extraState = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
        val extraPhoneNumber = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d(tag, "Phone state received: $extraState, phone number $extraPhoneNumber")

        handlePhoneState(context, extraState, extraPhoneNumber, intent?.action)
    }

    // Common method to handle phone state changes for both old and new APIs
    private fun handlePhoneState(context: Context, state: String?, phoneNumber: String?, action: String?) {
        if (phoneNumber != null && state != null && state.contains("RINGING")) {
            val i = Intent(context, IncomingCallInfoService::class.java)
            i.putExtra("phone_number", phoneNumber)
            context.startService(i)
        }

        // to not start service twice
        if (!isServiceRunning(context)) {
            if (phoneIntent == action && isExtraStateValid(state) && phoneNumber == null) {
                Log.d(tag, "Conditions for starting service are valid")

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
            if (phoneIntent == action && state == TelephonyManager.EXTRA_STATE_IDLE) {
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
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager

        for (service: RunningServiceInfo in manager.getRunningServices(Int.MAX_VALUE)) {
            if (BackgroundCallService::class.java.name == service.service.className) {
                return true
            }
        }

        return false
    }

    companion object {
        // Register the modern TelephonyCallback for Android 12+ (API 31+)
        @RequiresApi(Build.VERSION_CODES.S)
        fun registerTelephonyCallback(context: Context) {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val executor = Executors.newSingleThreadExecutor()

            try {
                telephonyManager.registerTelephonyCallback(
                    executor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            val stateString = when (state) {
                                TelephonyManager.CALL_STATE_IDLE -> TelephonyManager.EXTRA_STATE_IDLE
                                TelephonyManager.CALL_STATE_RINGING -> TelephonyManager.EXTRA_STATE_RINGING
                                TelephonyManager.CALL_STATE_OFFHOOK -> TelephonyManager.EXTRA_STATE_OFFHOOK
                                else -> null
                            }

                            // Handle the call state change
                            val receiver = PhoneStateReceiver()
                            receiver.handlePhoneState(context, stateString, null, TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                        }
                    }
                )
                Log.d("PhoneStateReceiver", "TelephonyCallback registered successfully")
            } catch (e: SecurityException) {
                Log.e("PhoneStateReceiver", "Failed to register TelephonyCallback: ${e.message}")
                Sentry.captureException(e)
            } catch (e: Exception) {
                Log.e("PhoneStateReceiver", "Error registering TelephonyCallback: ${e.message}")
                Sentry.captureException(e)
            }
        }
    }
}
