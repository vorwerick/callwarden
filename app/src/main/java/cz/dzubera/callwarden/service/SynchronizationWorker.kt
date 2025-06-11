package cz.dzubera.callwarden.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import cz.dzubera.callwarden.R
import cz.dzubera.callwarden.utils.startSynchronization
import kotlin.random.Random

/**
 * Worker class for periodic synchronization of call data.
 * This worker is scheduled to run 5 times a day using WorkManager.
 */
class SynchronizationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val tag = "SynchronizationWorker"
    private val notificationId = Random.nextInt()
    private val channelId = "synchronization_channel"
    private val channelName = "Synchronization Channel"

    /**
     * Creates a ForegroundInfo object that can be used to run this worker in a foreground service.
     */
    private fun createForegroundInfo(progress: String): ForegroundInfo {
        // Create a notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Synchronizace")
            .setContentText(progress)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

        // For Android 14 (API 34) and above, we need to specify a foreground service type
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notificationId, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    override fun doWork(): Result {
        Log.d(tag, "Starting synchronization work")

        try {
            // Set foreground service with initial notification
            setForegroundAsync(createForegroundInfo("Synchronizace dat..."))

            // Use coroutineScope to ensure all work is completed before returning
            val result = runCatching {
                // Call the synchronization function and wait for it to complete
                startSynchronization(applicationContext) { message ->
                    Log.d(tag, "Synchronization status: $message")
                    // Update foreground notification with progress
                    setForegroundAsync(createForegroundInfo(message))
                }
            }

            return if (result.isSuccess) {
                // Return success if synchronization completed without exceptions
                Log.d(tag, "Synchronization work completed successfully")
                Result.success()
            } else {
                // Log the error and return retry if an exception occurred
                Log.e(tag, "Error during synchronization: ${result.exceptionOrNull()?.message}", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            // Log the error and return retry for any other exceptions
            Log.e(tag, "Error during synchronization: ${e.message}", e)
            return Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "synchronization_work"
    }
}
