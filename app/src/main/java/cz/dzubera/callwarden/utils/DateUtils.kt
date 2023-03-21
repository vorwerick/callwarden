package cz.dzubera.callwarden.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

object DateUtils {

    @RequiresApi(Build.VERSION_CODES.O)
    fun atEndOfDay(date: Date): Date {
        val localDateTime: LocalDateTime = dateToLocalDateTime(date)
        val endOfDay: LocalDateTime = localDateTime.with(LocalTime.MAX)
        return localDateTimeToDate(endOfDay)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun atStartOfDay(date: Date): Date {
        val localDateTime: LocalDateTime = dateToLocalDateTime(date)
        val startOfDay: LocalDateTime = localDateTime.with(LocalTime.MIN)
        return localDateTimeToDate(startOfDay)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun dateToLocalDateTime(date: Date): LocalDateTime {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun localDateTimeToDate(localDateTime: LocalDateTime): Date {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant())
    }
}