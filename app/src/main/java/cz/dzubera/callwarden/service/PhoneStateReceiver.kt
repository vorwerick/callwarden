package cz.dzubera.callwarden.service

import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ACTIVITY_SERVICE
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import io.sentry.Sentry
import java.util.concurrent.Executors

class PhoneStateReceiver : BroadcastReceiver() {

    private val tag = "PhoneStateReceiver"

    override fun onReceive(context: Context, intent: Intent?) {
        val state = intent?.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phone = intent?.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
        val action = intent?.action

        Log.d(tag, "BR state=$state, phone=$phone")

        handlePhoneState(context, state, phone, action)
    }

    fun handlePhoneState(context: Context, state: String?, phone: String?, action: String?) {

        // Incoming call → open info screen
        if (state == TelephonyManager.EXTRA_STATE_RINGING && !phone.isNullOrBlank()) {
            Intent(context, IncomingCallInfoService::class.java).also {
                it.putExtra("phone_number", phone)
                context.startService(it)
            }
            return
        }

        // Start service on RINGING/OFFHOOK (but only once)
        if (state == TelephonyManager.EXTRA_STATE_RINGING ||
            state == TelephonyManager.EXTRA_STATE_OFFHOOK
        ) {
            if (!isServiceRunning(context)) startBackgroundService(context)
            return
        }

        // When call ends → send IDLE broadcast to service
        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            Log.d(tag, "Broadcasting idle")
            context.sendBroadcast(Intent(IdleStateReceiverForService.ACTION_SERVICE_IDLE_STATE))
        }
    }

    private fun startBackgroundService(context: Context) {
        Log.d(tag, "Starting BackgroundCallService...")

        val intent = Intent(context, BackgroundCallService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start service", e)
            Sentry.captureException(e)
        }
    }

    private fun isServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == BackgroundCallService::class.java.name
        }
    }

    companion object {

        @RequiresApi(Build.VERSION_CODES.S)
        fun registerTelephonyCallback(context: Context) {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val executor = Executors.newSingleThreadExecutor()

            try {
                tm.registerTelephonyCallback(executor, object :
                    TelephonyCallback(),
                    TelephonyCallback.CallStateListener {

                    override fun onCallStateChanged(state: Int) {
                        val stateString = when (state) {
                            TelephonyManager.CALL_STATE_IDLE -> TelephonyManager.EXTRA_STATE_IDLE
                            TelephonyManager.CALL_STATE_RINGING -> TelephonyManager.EXTRA_STATE_RINGING
                            TelephonyManager.CALL_STATE_OFFHOOK -> TelephonyManager.EXTRA_STATE_OFFHOOK
                            else -> null
                        }

                        val receiver = PhoneStateReceiver()
                        receiver.handlePhoneState(context, stateString, null,
                            TelephonyManager.ACTION_PHONE_STATE_CHANGED
                        )
                    }
                })

                Log.d("PhoneStateReceiver", "TelephonyCallback registered")

            } catch (e: Exception) {
                Log.e("PhoneStateReceiver", "Callback failed: ${e.message}")
                Sentry.captureException(e)
            }
        }
    }
}