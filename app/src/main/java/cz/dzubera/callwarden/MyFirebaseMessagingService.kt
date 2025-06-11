package cz.dzubera.callwarden

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import cz.dzubera.callwarden.service.HttpRequest
import cz.dzubera.callwarden.ui.activity.LoginActivity
import cz.dzubera.callwarden.ui.activity.NotificationActivity
import cz.dzubera.callwarden.utils.PreferencesUtils
import cz.dzubera.callwarden.utils.toIntent


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val credentials = PreferencesUtils.loadCredentials(this)
        credentials?.let {
            HttpRequest.sendToken(it.domain, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received: ${remoteMessage.data}")

        remoteMessage.notification?.let {
            sendNotification(
                it.body ?: "Klikněte pro více informací",
                remoteMessage.data.getOrDefault("url", "https://google.com")

            )
        }
    }

    private fun sendNotification(messageBody: String, url: String) {

        Log.d("cz.dzubera.callwarden.MyFirebaseMessagingService", "Send")


        val channelId = "default_channel"
        val notificationId = 9421

        val largeIcon =
            BitmapFactory.decodeResource(this.resources, R.mipmap.ic_launcher_foreground)

        val notificationManager = NotificationManagerCompat.from(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                description = "Kanál pro notifikace"
            }
            notificationManager.createNotificationChannel(channel)
        }


        val activityIntent = Intent(this, NotificationActivity::class.java).apply {
            putExtra("url", url)
            extras?.putString("url", url)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

        }

        val pendingActivity = PendingIntent.getActivity(
            this,
            9421, // Fixed requestCode matching the notificationId to ensure replacement
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )



        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Příchozí hovor od kontaktu")
            .setContentText(messageBody)
            .setContentIntent(pendingActivity)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Xiaomi hack
            .setPriority(NotificationManagerCompat.IMPORTANCE_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("cz.dzubera.callwarden.MyFirebaseMessagingService", "No permission")

            return
        }
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
