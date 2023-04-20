package cz.dzubera.callwarden.model

import android.content.Context
import android.provider.CallLog

/*
* Example src
* https://gist.github.com/kannansuresh/57945c9ee3fc29e04f62dc991449d595
 */

object CallHistory {

    fun getCallLogs(context: Context): History {

        // create cursor
        val managedCursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null,
            null,
            null,
            CallLog.Calls.DATE
        )

        // crete pointer to columns
        val number = managedCursor!!.getColumnIndex(CallLog.Calls.NUMBER)
        val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)

        //we need last only last call
        managedCursor.moveToLast()

        // load last call
        val phNumber = managedCursor.getString(number)
        val callType = managedCursor.getString(type)
        val callDateTimestamp = managedCursor.getString(date)
        val callDuration = managedCursor.getString(duration)
        val direction: Int = callType.toInt()

        var callDirection = Call.Direction.OUTGOING;

        // Convert direction
        when (direction) {
            CallLog.Calls.OUTGOING_TYPE -> callDirection = Call.Direction.OUTGOING
            CallLog.Calls.INCOMING_TYPE -> callDirection =  Call.Direction.INCOMING
            CallLog.Calls.MISSED_TYPE -> callDirection =  Call.Direction.INCOMING
        }

        // close cursor safely
        managedCursor.close()

        return History(callDuration, phNumber, callDateTimestamp.toLong(), callDirection)
    }
}

data class History(
    val callDuration: String?,
    val phoneNumber: String?,
    val callStartedTimestamp: Long,
    val direction: Call.Direction
)