package cz.dzubera.callwarden

import android.app.Application
import androidx.room.Room
import cz.dzubera.callwarden.db.AppDatabase
import cz.dzubera.callwarden.storage.CacheStorage
import cz.dzubera.callwarden.storage.TransmissionService
import cz.dzubera.callwarden.storage.UserSettingsStorage
import java.util.*


class App : Application() {

    companion object {
        val cacheStorage: CacheStorage by lazy { CacheStorage() }
        val userSettingsStorage: UserSettingsStorage by lazy { UserSettingsStorage() }
        val transmissionService: TransmissionService<Call> by lazy { TransmissionService() }
        lateinit var appDatabase: AppDatabase
        var dateFrom = Date()
        var dateTo = Date(System.currentTimeMillis())

        fun toCalendar(date: Date): Calendar {
            val cal = Calendar.getInstance()
            cal.time = date
            return cal
        }

        fun toDate(calendar: Calendar): Date {
            return calendar.time
        }
    }

    override fun onCreate() {
        super.onCreate()

        val calendar = Calendar.getInstance()
        calendar[Calendar.YEAR] = 2021 // for 6 hour

        calendar[Calendar.DAY_OF_MONTH] = 1 // for 0 min

        calendar[Calendar.MONTH] = 0 // for 0 sec

        dateFrom = App.toDate(calendar)
        appDatabase = Room.databaseBuilder(this, AppDatabase::class.java, "call_warden").build()

    }
}

