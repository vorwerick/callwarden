package cz.dzubera.callwarden.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import cz.dzubera.callwarden.service.AlarmReceiver

/*
* repeating periodic task
* https://guides.codepath.com/android/Repeating-Periodic-Tasks
* using alarm manager for periodic tasks
* https://guides.codepath.com/android/Starting-Background-Services#using-with-alarmmanager-for-periodic-tasks
* official docs
* https://developer.android.com/training/scheduling/alarms#set
* AlarmManager not cancelling alarms correctly
* */

object AlarmUtils {
    val tag = javaClass.name

    //setup alarm manager every hour
    fun scheduleAlarm(context: Context) {
        Log.d(tag, "schedule alarm called")

        cancelAlarm(context)
        val pIntent = createPendingIntent(
            context,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            true
        )

        val firstMillis = System.currentTimeMillis()

        val alarmManager = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager

        if (pIntent != null) {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                firstMillis,
                AlarmManager.INTERVAL_HOUR,
                pIntent
            )
            Log.d(tag, "alarm scheduled")
        }

    }

    private fun cancelAlarm(context: Context) {
        Log.d(tag, "cancel alarm called")

        val pIntent: PendingIntent? = createPendingIntent(
            context,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            false
        )

        val alarmManager = context.getSystemService(Service.ALARM_SERVICE) as AlarmManager
        if (pIntent != null && alarmManager != null) {
            alarmManager.cancel(pIntent)
            Log.d(tag, "alarm canceled")
        } else {
            Log.d(tag, "alarm not canceled")
        }

    }

    private fun createPendingIntent(
        context: Context,
        flag: Int,
        isBroadcast: Boolean
    ): PendingIntent? {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.action = "cz.dzubera.callwarden.ALARM"
        return if (isBroadcast) {
            PendingIntent.getBroadcast(
                context,
                AlarmReceiver.requestCode,
                intent,
                flag
            )
        } else {
            PendingIntent.getService(
                context,
                AlarmReceiver.requestCode,
                intent,
                flag
            )
        }

    }


}