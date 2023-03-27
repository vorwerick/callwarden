package cz.dzubera.callwarden

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.dzubera.callwarden.service.db.AppDatabase
import cz.dzubera.callwarden.storage.CacheStorage
import cz.dzubera.callwarden.storage.ProjectStorage
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

