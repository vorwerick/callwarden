package cz.dzubera.callwarden.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cz.dzubera.callwarden.utils.PreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingCallInfoService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val projectId = PreferencesUtils.loadProjectId(this)
        val credentials = PreferencesUtils.loadCredentials(this)
        val phoneNumber = intent!!.getStringExtra("phone_number")
        Log.i("testik", "on create " + phoneNumber.toString())
        if (credentials == null || projectId == null) {
            return START_NOT_STICKY
        }
        val token = PreferencesUtils.get(this, "firebase_token") ?: ""
        CoroutineScope(Dispatchers.Main).launch {

            HttpRequest.sendIncomingCall(
                domain = credentials.domain,
                user = credentials.user,
                projectId = projectId,
                number = phoneNumber ?: "?",
                token = token, onResponse = {}
            )
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