package cz.dzubera.callwarden.model

import android.content.Context
import android.provider.CallLog
import android.util.Log

/*
* Example src
* https://gist.github.com/kannansuresh/57945c9ee3fc29e04f62dc991449d595
 */

object CallHistory {

    const val GET_X_LAST_CALLS = 100

    fun getLastCallHistory(context: Context): History {

        // create cursor
        val managedCursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null,
            null,
            null,
            CallLog.Calls.DATE
        )

        // create pointer to columns
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
        var callDuration = managedCursor.getString(duration)
        val direction: Int = callType.toInt()

        var callDirection = Call.Direction.OUTGOING;

        // Convert direction
        when (direction) {
            CallLog.Calls.OUTGOING_TYPE -> callDirection = Call.Direction.OUTGOING
            CallLog.Calls.INCOMING_TYPE -> callDirection = Call.Direction.INCOMING
            CallLog.Calls.MISSED_TYPE -> callDirection = Call.Direction.INCOMING
        }


        // if for xiaomi - it is sending duration >0, call is missed
        if (CallLog.Calls.MISSED_TYPE == direction) {
            callDuration = "0"
        }

        // close cursor safely
        managedCursor.close()

        return History(callDuration, phNumber, callDateTimestamp.toLong(), callDirection)
    }

    fun getCallsHistory(context: Context, count: Int): List<History> {

        val historyList = mutableListOf<History>()

        // create cursor
        val managedCursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI, null,
            null,
            null,
            CallLog.Calls.DATE
        )

        // create pointer to columns
        val number = managedCursor!!.getColumnIndex(CallLog.Calls.NUMBER)
        val type = managedCursor.getColumnIndex(CallLog.Calls.TYPE)
        val date = managedCursor.getColumnIndex(CallLog.Calls.DATE)
        val duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION)

        Log.d("CallHistory", "cursor length: ${managedCursor.count}")

        //we need last only last call
        managedCursor.moveToLast()
        var maxCount = count
        if(count > managedCursor.count){
            maxCount = managedCursor.count
        }

        for (i in 0 until maxCount) {

            val phNumber = managedCursor.getString(number)
            val callType = managedCursor.getString(type)
            val callDateTimestamp = managedCursor.getString(date)
            var callDuration = managedCursor.getString(duration)
            Log.d("CallHistory", "callDuration: $callDuration")

            val direction: Int = callType.toInt()

            var callDirection = Call.Direction.OUTGOING;

            // Convert direction
            when (direction) {
                CallLog.Calls.OUTGOING_TYPE -> callDirection = Call.Direction.OUTGOING
                CallLog.Calls.INCOMING_TYPE -> callDirection = Call.Direction.INCOMING
                CallLog.Calls.MISSED_TYPE -> callDirection = Call.Direction.INCOMING
            }

            // if for xiaomi - it is sending duration >0, call is missed
            if (CallLog.Calls.MISSED_TYPE == direction) {
                callDuration = "0"
            }

            historyList.add(
                History(
                    callDuration,
                    phNumber,
                    callDateTimestamp.toLong(),
                    callDirection,
                )
            )

            managedCursor.moveToPrevious()
        }

        // close cursor safely
        managedCursor.close()

        return historyList
    }


}

data class History(
    val callDuration: String?,
    val phoneNumber: String?,
    val callStartedTimestamp: Long,
    val direction: Call.Direction,
)