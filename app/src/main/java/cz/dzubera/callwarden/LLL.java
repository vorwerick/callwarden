package cz.dzubera.callwarden;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog;

import java.util.ArrayList;
import java.util.Date;

public class LLL {
    public static String getCallLogs(Context context) {
        String callDurati = "";

        Cursor managedCursor = context.getContentResolver().query(
                CallLog.Calls.CONTENT_URI,                null,
                null,
                null,
                CallLog.Calls.DATE
        );
        ArrayList<String> calllogsBuffer = new ArrayList<String>();
        calllogsBuffer.clear();
        int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
        int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
        int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
        int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);
        int missedReason = managedCursor.getColumnIndex(CallLog.Calls.MISSED_REASON);
        while (managedCursor.moveToNext()) {
            String phNumber = managedCursor.getString(number);
            String callType = managedCursor.getString(type);
            String callDate = managedCursor.getString(date);
            Date callDayTime = new Date(Long.valueOf(callDate));
            String callDuration = managedCursor.getString(duration);
            String dir = null;
            int dircode = Integer.parseInt(callType);
            switch (dircode) {
                case CallLog.Calls.OUTGOING_TYPE:
                    dir = "OUTGOING";
                    break;
                case CallLog.Calls.INCOMING_TYPE:
                    dir = "INCOMING";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    dir = "MISSED";
                    break;
            }
            calllogsBuffer.add("\nPhone Number: " + phNumber + " \nCall Type: "
                    + dir + " \nCall Date: " + callDayTime
                    + " \nCall duration in sec : " + callDuration + "\n" + " Missed type:" + missedReason);
            callDurati = callDuration;


        }

        managedCursor.close();
        return callDurati;
    }
}
