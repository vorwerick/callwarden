package cz.dzubera.callwarden

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Message received: ${remoteMessage.data}")

        remoteMessage.notification?.clickAction
        remoteMessage.notification?.let {
            sendNotification(
                it.body ?: "Nová zpráva",
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
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Příchozí hovor od kontaktu")
            .setContentText(messageBody)
            .setLargeIcon(largeIcon)
            .setPriority(NotificationManagerCompat.IMPORTANCE_MAX)
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