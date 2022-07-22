package cz.dzubera.callwarden

import android.R
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
import cz.dzubera.callwarden.db.CallEntity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*


class Deamon() : Service() {

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
        Log.d("EXTREMISMUS", telecomManager.isInCall.toString())


        if (state == TelephonyManager.CALL_STATE_IDLE) {
            ServiceReceiver.currentCall?.isEnded = true
            ServiceReceiver.currentCall?.callEnded = System.currentTimeMillis()
            if (ServiceReceiver.currentCall?.callAccepted != null && ServiceReceiver.currentCall?.direction == Call.Direction.INCOMING) {
                ServiceReceiver.currentCall?.callType == Call.Type.ACCEPTED
            }

            if (ServiceReceiver.currentCall?.callAccepted == null && ServiceReceiver.currentCall?.direction == Call.Direction.INCOMING) {
                ServiceReceiver.currentCall?.callType == Call.Type.MISSED
            }
            sendCall(ServiceReceiver.currentCall, this)
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
                createNotificationChannel("my_service", "My Background Service")
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.drawable.btn_dialog)
            .setPriority(PRIORITY_MIN)
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
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun sendCall(currentCall: ServiceReceiver.CurrentCall?, context: Context?) {
        if (currentCall != null) {
            val call = Call(
                currentCall.callStarted,
                currentCall.callType,
                currentCall.direction,
                currentCall.phoneNumber,
                currentCall.callStarted,
                currentCall.callEnded,
                currentCall.callAccepted
            )

            ServiceReceiver.ex.submit {
                Thread.sleep(5000)
                synchronized(ServiceReceiver.ex) {
                    val dur = LLL.getCallLogs(context)
                    if (dur.isEmpty() || dur == "0") {
                        call.type = Call.Type.DIALED
                    }
                    call.dur = dur
                    App.cacheStorage.addCallItem(call)
                    saveToAnalytics(call, context)
                    sendCallToInternet(call)
                }
            }
        }
    }

    private fun saveToAnalytics(call: Call, context: Context?) {
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
                call.type.name,
                call.direction.name,
                call.phoneNumber,
                call.callStarted,
                call.callEnded,
                call.callAccepted
            )

            GlobalScope.launch {
                App.appDatabase.taskCalls().insert(entity)
            }
        }

    }
}

fun sendCallToInternet(call: Call) {
    println("INTERNET: " + "SENDING TO INTERNET")
    val status = when (call.type) {
        Call.Type.MISSED -> "zmeškaný"
        Call.Type.ACCEPTED -> "přijatý"
        Call.Type.CALLBACK -> "volání"
        Call.Type.DIALED -> "pohyb"
    }
    val dir = when (call.direction) {
        Call.Direction.INCOMING -> "internal"
        Call.Direction.OUTGOING -> "external"
    }
    var callConnectionTime = "0sec"
    var callRinginTime =
        (((call.callEnded - call.callStarted) / 1000).toInt()).toString() + "sec"
    if (call.direction == Call.Direction.INCOMING && call.callAccepted != null && call.type == Call.Type.ACCEPTED) {
        callConnectionTime =
            (((call.callAccepted - call.callStarted) / 1000).toInt()).toString() + "sec"
        callRinginTime =
            (((call.callEnded - call.callStarted) / 1000).toInt() - ((call.callAccepted - call.callStarted) / 1000).toInt()).toString() + "sec"
    }
    if (call.direction == Call.Direction.OUTGOING) {
        if (call.dur.isEmpty() || call.dur == "0") {
            callConnectionTime = "vytáčení"
            callRinginTime =
                ((((call.callEnded - call.callStarted) / 1000).toInt()) - call.dur.toInt(10)).toString() + "sec"
        } else {
            callConnectionTime = call.dur + "sec"
            callRinginTime =
                ((((call.callEnded - call.callStarted) / 1000).toInt()) - call.dur.toInt(10)).toString() + "sec"
        }
    }

    var coutnryCode = "CZ"
    Iso2Phone.all.forEach { (k, v) ->
        if (call.phoneNumber.contains(v)) {
            coutnryCode = k
        }
    }
    val paramMap = mapOf(
        "userName" to App.userSettingsStorage.userName,
        "userNumber" to App.userSettingsStorage.userNumber,
        "direction" to dir,
        "status" to status,
        "callerNumber" to call.phoneNumber,
        "callingStart" to SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(call.callStarted)),
        "callingEnd" to SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(Date(call.callEnded)),
        "callingDuration" to (((call.callEnded - call.callStarted) / 1000).toInt()).toString() + "sec",
        "callingConnectionTime" to callConnectionTime,
        "callingRingingTime" to callRinginTime,
        "countryCode" to coutnryCode,
        "monthAndYear" to SimpleDateFormat("MM-yyyy").format(Date(call.callStarted)),
    )

    val paramBuilder = StringBuilder()
    var index = 0

    paramMap.forEach {
        if (index == 0) {
            paramBuilder.append("")

        }
        index++
        paramBuilder.append(URLEncoder.encode(it.key, "UTF-8"))
        paramBuilder.append("=")
        paramBuilder.append(URLEncoder.encode(it.value, "UTF-8"))
        paramBuilder.append("&")
    }


    val mURL =
        URL(Config.API)
    println(paramBuilder.toString())

    with(mURL.openConnection() as HttpURLConnection) {
        // optional default is GET
        requestMethod = "POST"

        val wr = OutputStreamWriter(outputStream);
        wr.write(paramBuilder.toString());
        wr.flush();

        println("URL : $url")
        println("Response Code : $responseCode")

        /*    if (responseCode > 200) {
                App.transmissionService.insertItem(call);
            }*/

        BufferedReader(InputStreamReader(inputStream)).use {
            val response = StringBuffer()

            var inputLine = it.readLine()
            while (inputLine != null) {
                response.append(inputLine)
                inputLine = it.readLine()
            }
/*
            println("Response : $response")
*/
        }
    }
}

