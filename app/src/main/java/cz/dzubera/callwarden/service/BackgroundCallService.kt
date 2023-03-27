package cz.dzubera.callwarden.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
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
import cz.dzubera.callwarden.BuildConfig
import cz.dzubera.callwarden.utils.Iso2Phone
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.service.db.CallEntity
import cz.dzubera.callwarden.service.db.PendingCallEntity
import cz.dzubera.callwarden.model.Call
import cz.dzubera.callwarden.model.CallHistory
import cz.dzubera.callwarden.utils.PreferencesUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


class BackgroundCallService() : Service() {

    private val AUTHORITY = BuildConfig.APPLICATION_ID + ".provider"
    private val NOTIFY_ID = 1337
    private val FOREGROUND_ID = 1338

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
                resolveCall(state, phoneNumber)
            }
        }
    }
    private val tlpjc by lazy {
        @RequiresApi(Build.VERSION_CODES.S)
        object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                resolveCall(state, null)
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun resolveCall(state: Int, phoneNumber: String?) {
        if (phoneNumber != null) {
            ServiceReceiver.currentCall?.phoneNumber = phoneNumber
        }

        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        Log.d("is In call", telecomManager.isInCall.toString())


        if (state == TelephonyManager.CALL_STATE_IDLE) {
            ServiceReceiver.currentCall?.isEnded = true
            ServiceReceiver.currentCall?.callEnded = System.currentTimeMillis()
            if (ServiceReceiver.currentCall?.callAccepted != null && ServiceReceiver.currentCall?.direction == Call.Direction.INCOMING) {
                ServiceReceiver.currentCall?.callType == Call.Type.ACCEPTED
            }

            if (ServiceReceiver.currentCall?.callAccepted == null && ServiceReceiver.currentCall?.direction == Call.Direction.INCOMING) {
                ServiceReceiver.currentCall?.callType == Call.Type.MISSED
            }
            recordCall(ServiceReceiver.currentCall, this)
            ServiceReceiver.currentCall = null
        }

        if (ServiceReceiver.currentCall == null) {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
                ServiceReceiver.currentCall =
                    ServiceReceiver.CurrentCall(Call.Direction.OUTGOING)
                ServiceReceiver.currentCall!!.callType = Call.Type.CALLBACK
                ServiceReceiver.currentCall!!.callStarted = System.currentTimeMillis()

            }
            if (state == TelephonyManager.CALL_STATE_RINGING) {
                ServiceReceiver.currentCall =
                    ServiceReceiver.CurrentCall(Call.Direction.INCOMING)
                ServiceReceiver.currentCall!!.callStarted = System.currentTimeMillis()


            }
        } else {
            if (state == TelephonyManager.CALL_STATE_OFFHOOK && ServiceReceiver.currentCall!!.direction == Call.Direction.INCOMING) {
                ServiceReceiver.currentCall!!.callType = Call.Type.ACCEPTED
                ServiceReceiver.currentCall!!.callAccepted = System.currentTimeMillis()
            }
        }
    }

    @SuppressLint("InlinedApi")
    override fun onCreate() {
        super.onCreate()
        Log.d("RURURU", "CREATED")

        val telephonyManager: TelephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        val telecomManager: TelecomManager =
            getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        ServiceReceiver.initialize()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyManager.registerTelephonyCallback(
                mainExecutor,
                tlpjc
            )
        } else {
            telephonyManager.listen(psl, PhoneStateListener.LISTEN_CALL_STATE)
        }


    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        Log.d("RURURU", "START COMMAND")

        startForeground()

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RURURU", "DESTROYED")

    }


    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startForeground() {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel("my_service", "Callwarden")
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

    private fun recordCall(currentCall: ServiceReceiver.CurrentCall?, context: Context) {
        println("HAJDOS: " + currentCall.toString())
        if (currentCall != null) {
            println("XX: " )

            ServiceReceiver.ex!!.submit {
                println("HOHO: " )

                Thread.sleep(3500)
                println("JOJO: " )

                synchronized(ServiceReceiver.ex!!) {
                    println("KAKA: " )

                    val history = CallHistory.getCallLogs(context)
                    println("CUCUC: " )

                    val credentails = PreferencesUtils.loadCredentials(context)
                    val projectId = PreferencesUtils.loadProjectId(context)
                    val projectName =  PreferencesUtils.loadProjectName(context)
                    val call = Call(
                        currentCall.callStarted,
                       credentails?.user.toString() ?: "",
                        credentails?.domain ?: "",
                        projectId ?: "-1",
                        projectName ?: "<none>",
                        currentCall.callType,
                        currentCall.direction,
                        currentCall.phoneNumber,
                        currentCall.callStarted,
                        currentCall.callEnded,
                        currentCall.callAccepted
                    )




                    val durationX= history.callDuration?.toIntOrNull()?: -1
                    if (durationX <= 0) {
                        call.type = Call.Type.DIALED
                    }
                    call.dur =durationX
                    println("CALL DURATION:" + call.dur.toString())
                    if(!history.phoneNumber.isNullOrEmpty()){
                        call.phoneNumber = history.phoneNumber
                    }
                    println("XAVIER: " + call.phoneNumber)
                    println("IRA: " )
                    App.cacheStorage.addCallItem(call)
                    saveToAnalyticsTryToUpload(call, context)
                }
            }
        }
    }

    private fun saveToAnalyticsTryToUpload(call: Call, context: Context?) {
        context?.let {
            var accepted =
                it.getSharedPreferences("XXX", Context.MODE_PRIVATE).getInt("acceptedCount", 0)
            var declined =
                it.getSharedPreferences("XXX", Context.MODE_PRIVATE).getInt("declinedCount", 0)
            var called =
                it.getSharedPreferences("XXX", Context.MODE_PRIVATE).getInt("calledCount", 0)
            var dialed =
                it.getSharedPreferences("XXX", Context.MODE_PRIVATE).getInt("dialedCount", 0)

            when (call.type) {
                Call.Type.ACCEPTED -> accepted += 1
                Call.Type.MISSED -> declined += 1
                Call.Type.CALLBACK -> called += 1
                Call.Type.DIALED -> dialed += 1
            }

            it.getSharedPreferences("XXX", Context.MODE_PRIVATE).edit()
                .putInt("acceptedCount", accepted)
                .putInt("declinedCount", declined)
                .putInt("dialedCount", dialed)
                .putInt("calledCount", called).apply()

            val entity = CallEntity(
                call.callStarted,
                call.userId,
                call.domainId,
                call.projectId,
                "",
                call.projectName,
                call.type.name,
                call.direction.name,
                call.phoneNumber,
                call.callStarted,
                call.callEnded,
                call.callAccepted,
                call.dur,
            )

            GlobalScope.launch {
                App.appDatabase.taskCalls().insert(entity)
            }

            uploadCall(context, listOf(entity)) { success ->
                if(!success){
                    val pendingEntity = PendingCallEntity(entity.callStarted!!)
                    GlobalScope.launch {
                        App.appDatabase.pendingCalls().insert(pendingEntity)
                    }
                }
            }

        }

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

