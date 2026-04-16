package cz.dzubera.callwarden.dialer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import cz.dzubera.callwarden.R


private const val CHANNEL_ID = "active_call_channel"
private const val NOTIFICATION_ID = 105

class MyInCallService : InCallService() {

    override fun onCreate() {
        super.onCreate()
        // Kanál vytvoříme hned při startu servisu
        createCallNotificationChannel()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallHolder.currentCall = call
        CallHolder.inCallService = this

        // Klíčové: Nejdříve notifikace, pak aktivita
        updateNotification(call)

        val intent = Intent(this, CallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(intent)
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        CallHolder.audioState.value = audioState
        CallHolder.currentCall?.let { updateNotification(it) }
    }

    private fun updateNotification(call: Call) {
        // Kontrola, zda hovor ještě trvá
        if (call.state == Call.STATE_DISCONNECTED || call.state == Call.STATE_DISCONNECTING) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            return
        }

        val number = call.details.handle?.schemeSpecificPart ?: "Neznámé číslo"

        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, CallActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hangupIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MyInCallService::class.java).setAction("ACTION_HANGUP"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val fullScreenIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, CallActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setOngoing(true)
            .setSmallIcon(R.drawable.call_made) // Ujisti se, že ic_call_made existuje v drawable
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Zvýšeno pro hovory
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setSubText("Current call")
            .setContentTitle(number)
            .setContentText("Click to show call details")
            .setGroup("cz.dzubera.callwarden.ACTIVE_CALL") // Unikátní klíč pro tuto skupinu
            .setGroupSummary(false) // Tato notifikace NENÍ souhrnem skupiny
            .setSortKey("A") // Zajistí, že bude v rámci své (jednočlenné) skupiny nahoře
            .addAction(R.drawable.call_received, "Hang up", hangupIntent)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(fullScreenIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        // Spuštění Foreground Service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_HANGUP") {
            CallHolder.currentCall?.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else {
            CallHolder.currentCall?.let { updateNotification(it) }
        }
        return START_NOT_STICKY
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallHolder.currentCall = null
        CallHolder.inCallService = null
        val intent = Intent("ACTION_FINISH_CALL_ACTIVITY")
        intent.setPackage(packageName) // Zabezpečení, aby to šlo jen do tvé appky
        sendBroadcast(intent)

        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Calls"
                setSound(null, null)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
object CallHolder {
    var currentCall: Call? = null
    var inCallService: MyInCallService? = null

    // Pro reaktivní UI v Compose
    val audioState = mutableStateOf<CallAudioState?>(null)
}


