package cz.dzubera.callwarden

import android.app.Application
import android.os.Build
import androidx.room.Room
import cz.dzubera.callwarden.db.AppDatabase
import cz.dzubera.callwarden.storage.CacheStorage
import cz.dzubera.callwarden.storage.UserSettingsStorage
import cz.dzubera.callwarden.utils.DateUtils
import java.util.*


class App : Application() {

    companion object {
        val cacheStorage: CacheStorage by lazy { CacheStorage() }
        val projectStorage: ProjectStorage by lazy { ProjectStorage() }
        val userSettingsStorage: UserSettingsStorage by lazy { UserSettingsStorage() }
        lateinit var appDatabase: AppDatabase
        var dateFrom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateUtils.atStartOfDay(Date())
        } else {
            Date()
        }

        var dateTo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateUtils.atEndOfDay(Date())
        } else {
            Date()
        }


        fun toDate(calendar: Calendar): Date {
            return calendar.time
        }
    }

    override fun onCreate() {
        super.onCreate()

        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, "call_warden").build()

    }
}

