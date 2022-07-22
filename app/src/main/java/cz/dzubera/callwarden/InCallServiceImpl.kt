package cz.dzubera.callwarden

import android.media.AudioManager
import android.telecom.InCallService
import android.util.Log
import cz.dzubera.callwarden.activity.MainActivity


class InCallServiceImpl : InCallService() {
    private var currentRingerMode = 0
    private var mAudioManager: AudioManager? = null

    override fun onCallAdded(call: android.telecom.Call?) {
        super.onCallAdded(call)
        Log.d("BLUBLA", "Number:????????")

    }

}