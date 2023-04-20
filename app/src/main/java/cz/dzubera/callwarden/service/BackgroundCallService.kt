package cz.dzubera.callwarden.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
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
import cz.dzubera.callwarden.service.db.PendingCallEntity
import cz.dzubera.callwarden.utils.Iso2Phone
import cz.dzubera.callwarden.utils.PreferencesUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class BackgroundCallService : Service(), IdleStateCallback { // class end

    private var telephonyManager: TelephonyManager? = null

    private val tag = javaClass.name

    private var receiver: IdleStateReceiverForService? = null


    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "CREATED")

        val defaultHandler: Thread.UncaughtExceptionHandler? =
            Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { p0, p1 ->
            val i = Intent(baseContext, BackgroundCallService::class.java)
            val pending = PendingIntent.getActivity(
                applicationContext, 0,
                i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val mgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            mgr[AlarmManager.RTC, System.currentTimeMillis() + 5000] = pending
            defaultHandler?.uncaughtException(p0, p1)
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

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
                createNotificationChannel("my_service", "Ramicall")
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
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.description = "Evidence hovorů"
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
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

    private fun recordCall(callEndTimestamp: Long, context: Context) {
        ServiceReceiver.ex!!.submit {
            Thread.sleep(200)
            synchronized(ServiceReceiver.ex!!) {

                // prepare data
                val history = CallHistory.getCallLogs(context)
                var duration = history.callDuration?.toIntOrNull() ?: -1
                val number = history.phoneNumber ?: ""
                val callStarted = history.callStartedTimestamp
                val callDirection = history.direction

                // if for xiaomi - it is sending duration >0, call is missed
                if (history.isMissed) {
                    duration = 0
                }

                var callAccepted: Long =
                    if (duration > 0) (callEndTimestamp - duration * 1000) else 0


                // prepare credentials
                val credentials = PreferencesUtils.loadCredentials(context)
                val projectId = PreferencesUtils.loadProjectId(context)
                val projectName = PreferencesUtils.loadProjectName(context)

                // store data
                val call = Call(
                    callStarted,
                    credentials?.user.toString() ?: "",
                    credentials?.domain ?: "",
                    projectId ?: "-1",
                    projectName ?: "<none>",
                    duration,
                    callDirection,
                    number,
                    callStarted,
                    callEndTimestamp,
                    callAccepted
                )

                App.cacheStorage.addCallItem(call)
                saveToAnalyticsTryToUpload(call, context)
                stopSelf()
            }
        }
    }

    private fun saveToAnalyticsTryToUpload(call: Call, context: Context?) {
        context?.let {

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
                App.appDatabase.taskCalls().insert(entity)
            }

            uploadCall(context, listOf(entity)) { success ->
                if (!success) {
                    val pendingEntity = PendingCallEntity(entity.callStarted!!)
                    GlobalScope.launch {
                        App.appDatabase.pendingCalls().insert(pendingEntity)
                    }
                }
            }
        }
    }

    override fun onIdleCalled() {
        Log.d(tag, "Service is informed about idle state from broadcast")
        unregisterPhoneListener() // unregister listeners, state may come twice

        Log.d(tag, "Call finished, prepare to store it ")
        recordCall(System.currentTimeMillis(), this)
    }
}

fun uploadCall(context: Context?, callEntities: List<CallEntity>, success: (Boolean) -> Unit) {
    val credentials = context?.let { PreferencesUtils.loadCredentials(it) }
    if (credentials != null) {
        val recordJson = JSONObject()
        val jsonArray = JSONArray()
        callEntities.forEach { callEntity ->
            var coutnryCode = "CZ"
            Iso2Phone.all.forEach { (k, v) ->
                if (callEntity.phoneNumber != null && callEntity.phoneNumber.contains(v)) {
                    coutnryCode = k
                }
            }

            val recordItem = JSONObject()
            recordItem.put("projectId", callEntity.projectId)
            recordItem.put("projectIdOld", callEntity.projectIdOld)
            recordItem.put("direction", Call.Direction.valueOf(callEntity.direction ?: "").ordinal)
            recordItem.put("number", callEntity.phoneNumber)
            recordItem.put("startTimestamp", callEntity.callStarted)
            recordItem.put("connectTimestamp", callEntity.callAccepted)

            recordItem.put("endTimestamp", callEntity.callEnded)
            recordItem.put("callDuration", callEntity.callDuration)
            recordItem.put("countryCode", coutnryCode)

            jsonArray.put(recordItem)

        }

        recordJson.put("records", jsonArray)
        HttpRequest.sendEntries(
            credentials.domain,
            credentials.user,
            recordJson.toString()
        ) { httpResponse ->
            when (httpResponse.code) {
                200 -> {
                    success(true)
                }
                422 -> {
                    success(true)
                    HttpRequest.getProjects(credentials.domain, credentials.user) {}
                }
                else -> {
                    success(false)
                }
            }

        }
    }

}


/* back up of call state logic

@SuppressLint("MissingPermission")
private fun resolveCall(state: Int) {

    val telecomManager = getSystemService(Service.TELECOM_SERVICE) as TelecomManager
    Log.d("is In call", telecomManager.isInCall.toString())


    if (state == TelephonyManager.CALL_STATE_IDLE) {
        ServiceReceiver.currentCall?.isEnded = true
        ServiceReceiver.currentCall?.callEnded = System.currentTimeMillis()

        recordCall(ServiceReceiver.currentCall, this)
        ServiceReceiver.currentCall = null
    }

    if (ServiceReceiver.currentCall == null) {
        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            ServiceReceiver.currentCall =
                ServiceReceiver.CurrentCall(Call.Direction.OUTGOING)
            ServiceReceiver.currentCall!!.callStarted = System.currentTimeMillis()

        }
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            ServiceReceiver.currentCall =
                ServiceReceiver.CurrentCall(Call.Direction.INCOMING)
            ServiceReceiver.currentCall!!.callStarted = System.currentTimeMillis()


        }
    } else {
        if (state == TelephonyManager.CALL_STATE_OFFHOOK && ServiceReceiver.currentCall!!.direction == Call.Direction.INCOMING) {
            ServiceReceiver.currentCall!!.callAccepted = System.currentTimeMillis()
        }
    }
}

private val psl by lazy {
    object : PhoneStateListener() {
        override fun onBarringInfoChanged(barringInfo: BarringInfo) {
            println("barring: " + barringInfo.toString())
        }

        override fun onCallDisconnectCauseChanged(
            disconnectCause: Int,
            preciseDisconnectCause: Int
        ) {
            println("cdcc: " + disconnectCause.toString() + " " + preciseDisconnectCause.toString())

        }

        override fun onDataConnectionStateChanged(state: Int, networkType: Int) {
            println("dcsc: " + state.toString())
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) {
            println("display: " + telephonyDisplayInfo.toString())
        }

        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
//                resolveCall(state, phoneNumber)
        }
    }
}

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            telephonyManager?.registerTelephonyCallback(
//                mainExecutor,
//                tlpjc
//            )
//        } else {
//            telephonyManager?.listen(
//                ServicePhoneStateListener(this),
//                PhoneStateListener.LISTEN_CALL_STATE
//            )
//        }


    //callback for newer API
    private val tlpjc by lazy {
        @RequiresApi(Build.VERSION_CODES.S)
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                resolveCall(state)
            }
        }
    }

    //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            telephonyManager?.unregisterTelephonyCallback(tlpjc)
//        } else {
//            //https://developer.android.com/reference/android/telephony/TelephonyManager#listen(android.telephony.PhoneStateListener,%20int)
//            telephonyManager?.listen(
//                ServicePhoneStateListener(this),
//                PhoneStateListener.LISTEN_NONE
//            )
//        }

*/


