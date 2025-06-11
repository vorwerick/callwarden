package cz.dzubera.callwarden

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import cz.dzubera.callwarden.service.PhoneStateReceiver
import cz.dzubera.callwarden.service.SynchronizationWorker
import cz.dzubera.callwarden.service.db.AppDatabase
import cz.dzubera.callwarden.storage.CacheStorage
import cz.dzubera.callwarden.storage.ProjectObject
import cz.dzubera.callwarden.storage.ProjectStorage
import cz.dzubera.callwarden.storage.UserSettingsStorage
import cz.dzubera.callwarden.utils.DateUtils
import java.util.*
import java.util.concurrent.TimeUnit


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
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE call_record ADD COLUMN projectIdOld VARCHAR")
        }
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        appDatabase =
            Room.databaseBuilder(this, AppDatabase::class.java, "call_warden").addMigrations(MIGRATION_1_2).build()

        App.cacheStorage.initialize()

        // Register the TelephonyCallback for Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                Log.d("App", "Registering TelephonyCallback for Android 12+")
                PhoneStateReceiver.registerTelephonyCallback(this)
            } catch (e: Exception) {
                Log.e("App", "Failed to register TelephonyCallback: ${e.message}")
            }
        } else {
            Log.d("App", "Using BroadcastReceiver for phone state changes (Android < 12)")
        }

        // Initialize WorkManager for periodic synchronization
        setupPeriodicSynchronization()
    }

    /**
     * Sets up periodic synchronization using WorkManager.
     * Schedules the SynchronizationWorker to run 5 times a day (every ~4.8 hours).
     */
    private fun setupPeriodicSynchronization() {
        try {
            Log.d("App", "Setting up periodic synchronization")


            // Define network constraints - worker should only run when network is available
            val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED) // Requires any network connection
                    .build()


            val syncWorkRequest = PeriodicWorkRequestBuilder<SynchronizationWorker>(
                20, TimeUnit.MINUTES
            ).setConstraints(constraints).build()

            // Schedule the work request with WorkManager
            // Use KEEP policy to ensure we don't reschedule if already exists
            WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
                SynchronizationWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, syncWorkRequest
            )

            Log.d("App", "Periodic synchronization scheduled successfully")
        } catch (e: Exception) {
            Log.e("App", "Failed to set up periodic synchronization: ${e.message}", e)
        }
    }
}
