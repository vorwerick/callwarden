package cz.dzubera.callwarden

import android.content.Context
import android.provider.CallLog
import java.util.*

object CallHistory {


    fun getCallLogs(context: Context): History {
        var callDurati: String? = ""
        var phone: String? = ""
        val managedCursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null,
            null,
            null,
            CallLog.Calls.DATE
        )
        val calllogsBuffer = ArrayList<String>()
        calllogsBuffer.clear()
        val number = managedCursor!!.getColumnIndex(CallLog.Calls.NUMBER)
        val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)
        val missedReason = managedCursor.getColumnIndex(CallLog.Calls.MISSED_REASON)
        while (managedCursor.moveToNext()) {
            val phNumber = managedCursor.getString(number)
            val callType = managedCursor.getString(type)
            val callDate = managedCursor.getString(date)
            val callDayTime = Date(java.lang.Long.valueOf(callDate))
            val callDuration = managedCursor.getString(duration)
            var dir: String? = null
            val dircode = callType.toInt()
            when (dircode) {
                CallLog.Calls.OUTGOING_TYPE -> dir = "OUTGOING"
                CallLog.Calls.INCOMING_TYPE -> dir = "INCOMING"
                CallLog.Calls.MISSED_TYPE -> dir = "MISSED"
            }
            calllogsBuffer.add(
                """
Phone Number: $phNumber 
Call Type: $dir 
Call Date: $callDayTime 
Call duration in sec : $callDuration
 Missed type:$missedReason"""
            )
            callDurati = callDuration
            phone = phNumber
        }
        managedCursor.close()
        return History(callDurati, phone)
    }
}

 class History(val callDuration: String?, val phoneNumber: String?)