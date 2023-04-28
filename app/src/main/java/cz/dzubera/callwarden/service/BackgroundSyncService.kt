package cz.dzubera.callwarden.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cz.dzubera.callwarden.utils.startSynchronization

//very simple service that starts synchronization and stops itself when finished

class BackgroundSyncService : Service() {

    private val tag = javaClass.name

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(tag, "sync service created")

        startSynchronization(this) {
            Log.d(tag, "synchronization finished with state: $it")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "sync service destroyed")
    }


}