

package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.activities.tasks.RemoveDecryptedFilesTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class DecryptedFilesCleanUpAlarmManager extends BroadcastReceiver {

    private static boolean alarmSet = false;
    public final static long AUTO_ERASE_TIME = 24 * 60* 60 * 1000;  // Changed to 24 hours on time of release


    @Override
    public void onReceive(Context context, Intent intent) 
    {
        new RemoveDecryptedFilesTask(context);
    }
    

    public static void SetAlarm(Context context)
    {
        if(alarmSet == false)
        {
            Intent intent = new Intent(context, DecryptedFilesCleanUpAlarmManager.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AUTO_ERASE_TIME, pendingIntent); 
            alarmSet = true;
        }
    }

    public static void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, DecryptedFilesCleanUpAlarmManager.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        alarmSet = false;
    }
}
