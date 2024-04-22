package cz.dzubera.callwarden.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cz.dzubera.callwarden.storage.ProjectStorage
import cz.dzubera.callwarden.utils.PreferencesUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class IncomingCallInfoService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("testik", "on create")
        val credentials = PreferencesUtils.loadCredentials(this)
        val projectId = PreferencesUtils.loadProjectId(this) ?: ProjectStorage.EMPTY_PROJECT.id
        val phoneNumber = intent!!.getStringExtra("phone_number")
        if (credentials == null) {
            return START_NOT_STICKY
        }
        GlobalScope.launch {

            HttpRequest.sendIncomingCall(
                credentials.domain,
                credentials.user,
                projectId,
                phoneNumber ?: "?"
            ) {}
            Log.i("testik", "new taskys")
            Log.i("testik", "danone")
            PreferencesUtils.save(
                this@IncomingCallInfoService,
                "XXX",
                System.currentTimeMillis().toString()
            );
        }
        return START_NOT_STICKY
    }


}