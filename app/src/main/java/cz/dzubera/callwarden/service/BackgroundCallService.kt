package cz.dzubera.callwarden.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.telephony.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import androidx.core.content.ContextCompat
import cz.dzubera.callwarden.App
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.model.CallHistory
import cz.dzubera.callwarden.service.db.CallEntity
import cz.dzubera.callwarden.storage.ProjectStorage
import cz.dzubera.callwarden.utils.PreferencesUtils
import cz.dzubera.callwarden.utils.startSynchronization
import cz.dzubera.callwarden.utils.uploadCall
import io.sentry.Sentry
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


class BackgroundCallService : Service(), IdleStateCallback { // class end

    private val tag = javaClass.name

    private var receiver: IdleStateReceiverForService? = null

    companion object {
        const val CHANNEL_ID = "my_service"
        const val CHANNEL_NAME = "Ramicall"
    }


    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "CREATED")

        ServiceReceiver.initialize()

        receiver = IdleStateReceiverForService(this)

        registerPhoneListener()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(tag, "START COMMAND")
        startForeground()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "DESTROYED")
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }


        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.primary))
            .setPriority(PRIORITY_MIN)
            .setSubText("Evidence hovorů")
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val chan = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.description = "Evidence hovorů"
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return CHANNEL_ID
    }


    private fun registerPhoneListener() {
        Log.d(tag, "registering phone listeners")

        registerReceiver(
            receiver,
            IntentFilter(IdleStateReceiverForService.ACTION_SERVICE_IDLE_STATE)
        )


    }

    private fun unregisterPhoneListener() {
        Log.d(tag, "unregistering phone listeners")
        unregisterReceiver(receiver)

    }

    override fun onIdleCalled() {
        Log.d(tag, "Service is informed about idle state from broadcast")
        unregisterPhoneListener() // unregister listeners, state may come twice

        stopSelf()

        Log.d(tag, "Call finished, prepare to store it ")
        val callEndTimestamp = System.currentTimeMillis()

        GlobalScope.launch {
            delay(1300)
            startSynchronization(this@BackgroundCallService) {
                Log.i(tag, it)
            }
        }
    }

    private fun recordCall(callEndTimestamp: Long) {
        synchronized(ServiceReceiver.ex!!) {

            // prepare data
            val history = CallHistory.getLastCallHistory(this@BackgroundCallService)
            val duration = history.callDuration?.toIntOrNull() ?: -1
            val number = history.phoneNumber ?: ""
            val callStarted = history.callStartedTimestamp
            val callDirection = history.direction


            val callAccepted: Long =
                if (duration > 0) (callEndTimestamp - duration * 1000) else 0


            // prepare credentials
            val credentials = PreferencesUtils.loadCredentials(this@BackgroundCallService)
            val projectId = PreferencesUtils.loadProjectId(this@BackgroundCallService)
            val projectName = PreferencesUtils.loadProjectName(this@BackgroundCallService)


            // store data
            val call = Call(
                callStarted,
                credentials?.user.toString(),
                credentials?.domain ?: "",
                projectId ?: ProjectStorage.EMPTY_PROJECT.id,
                projectName ?: ProjectStorage.EMPTY_PROJECT.name,
                duration,
                callDirection,
                number,
                callStarted,
                callEndTimestamp,
                callAccepted
            )

            App.cacheStorage.addCallItem(call)
            val entity = CallEntity(
                call.callStarted,
                call.userId,
                call.domainId,
                call.projectId,
                "",
                call.projectName,
                null,
                call.direction.name,
                call.phoneNumber,
                call.callStarted,
                call.callEnded,
                call.callAccepted,
                call.duration,
            )
            GlobalScope.launch {
                try {
                    val results = App.appDatabase.taskCalls()
                    if (results.get(entity.uid) == null) {
                        App.appDatabase.taskCalls().insert(entity)
                        uploadCall(this@BackgroundCallService, listOf(entity)) { success ->
                        }
                    }

                } catch (e: SQLiteConstraintException) {
                    Log.e(tag, "Call already stored")
                    Sentry.captureException(e)
                }
            }


        }
    }
}

