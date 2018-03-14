package com.mozy.mobile.android.service;

import com.mozy.mobile.android.activities.DecryptedFilesCleanUpAlarmManager;
import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.catch_release.CRReceiver;
import com.mozy.mobile.android.utils.SystemState;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class MozyService extends Service {

    public static boolean running = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MozyService.running = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        MozyService.running = true;
        
        Context context = getApplicationContext();
      
        // default we turn it on and disable if no encrypted containers are found
        enableDecryptedFilesCleanupMgr(context);

        // Only need to do this if upload is even allowed
        if (SystemState.isUploadEnabled())
        {
            UploadSettings settings = new UploadSettings(context);
            if (!settings.getUploadInitialized() || settings.getAutoCandR())
            {
                UploadManager.startCatchAndRelease(context, true);
            }
            else // !settings.getUploadInitialized()
            {
                UploadManager.initialize(context);
                MozyService.running = false;
            }
        }
        else
        {
            UploadManager.initialize(context);
            MozyService.running = false;
        }
        
        return START_STICKY; 
    }

    /**
     * @param context
     */
    public static void enableDecryptedFilesCleanupMgr(Context context) {
        
        DecryptedFilesCleanUpAlarmManager.SetAlarm(context);
    }
    
    public static void disableDecryptedFilesCleanupMgr(Context context)
    {
        DecryptedFilesCleanUpAlarmManager.CancelAlarm(context);
    }
    
    public static void startDecryptedFilesCleanUp(Context context)
   {
     if (serviceRunning() == false)  // service not running if both flags are false
     {
         // Start the service and that will take care of the rest
         Intent serviceIntent = new Intent();
         serviceIntent.setAction(CRReceiver.ACTION_START_SERVICE);
         context.startService(serviceIntent);
     }
       }
  
    private static boolean serviceRunning() 
    {
        return running;
    }

    public static void stopMozyService(Context context)
    {
        if (serviceRunning())  
        {
            // Start the service and that will take care of the rest
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CRReceiver.ACTION_START_SERVICE);
            context.stopService(serviceIntent);
            running = false;
        }
    }
   
}

