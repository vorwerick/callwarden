package cz.dzubera.callwarden

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat.CallStyle.CallType
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.dzubera.callwarden.service.BackgroundCallService
import cz.dzubera.callwarden.service.db.AppDatabase
import cz.dzubera.callwarden.storage.CacheStorage
import cz.dzubera.callwarden.storage.ProjectObject
import cz.dzubera.callwarden.storage.ProjectStorage
import cz.dzubera.callwarden.storage.UserSettingsStorage
import cz.dzubera.callwarden.ui.activity.MainActivity
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

        var projectFilter: ProjectObject? = null
        var callTypeFilter: MutableList<Boolean> = mutableListOf(true, true, true, true)

        fun toDate(calendar: Calendar): Date {
            return calendar.time
        }
    }

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE call_record ADD COLUMN projectIdOld VARCHAR")
        }
    }



    override fun onCreate() {
        super.onCreate()

        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, "call_warden")
            .addMigrations(MIGRATION_1_2).build()

        App.cacheStorage.initialize()



    }
}

