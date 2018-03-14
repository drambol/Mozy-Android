package com.mozy.mobile.android.catch_release;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class CRReceiver extends BroadcastReceiver
{
    private static final String TAG = CRReceiver.class.getSimpleName();
    public static final String ACTION_START_SERVICE =   "com.mozy.mobile.android.service.MozyService";
    private static final String PREFERENCES_CRRECEIVER = "crreceiver";
    private static final String LAST_USER_INFO_REQUEST_DATE = "last_user_info_request_date";
        
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent != null)
        {
            String action = intent.getAction();
            if (action != null) {
                
                Provisioning provisioning = Provisioning.getInstance(context);
                
                String token = provisioning.getMipAccountToken();
                
                String tokenSecret = provisioning.getMipAccountTokenSecret();

                if((token != null && token.length() != 0 && tokenSecret != null && tokenSecret.length() != 0))
                {
                    if (Intent.ACTION_BOOT_COMPLETED.equalsIgnoreCase(action)
                            || (Intent.ACTION_USER_PRESENT.equalsIgnoreCase(action))
                            || Intent.ACTION_MEDIA_MOUNTED.equalsIgnoreCase(action))
                    {
                        UploadManager.initialize(context);
                        LogUtil.debug(TAG, "onReceive():" + action);     
    
                        if (SystemState.isUploadEnabled())
                        {
                            // Use the cached settings to determine if we need to turn it on
                            if(SystemState.getSyncDevice() != null)
                            {
                                SystemState.setManualUploadEnabled(true, context);
                                //turn on or off service if not needed
                                UploadSettings settings = new UploadSettings(context);
                                settings.mozyServiceActionOnSettings(context);
                                }
                         }
                         if(SystemState.getDeviceList() != null) // enable and start Mozy Service if encrypted device
                         {
                             if(SystemState.hasEncryptedDevice(SystemState.getDeviceList()) == true)
                             {
                                 // Enable Mozy Service
                                 SystemState.setMozyServiceEnabled(true, context);
                                 MozyService.startDecryptedFilesCleanUp(context);
                                 MozyService.enableDecryptedFilesCleanupMgr(context);
                              }
                         }
                    }              
                }
                    
                if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(action))
                {
                    UploadManager.initialize(context);
                    LogUtil.debug(TAG, "onReceive(): CONNECTIVITY_ACTION");
                    boolean noConnectivity =
                        intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                    if (noConnectivity == false)
                    {
                        final SharedPreferences preferences = context.getApplicationContext().getSharedPreferences(CRReceiver.PREFERENCES_CRRECEIVER, 
                                                                                                                   Activity.MODE_PRIVATE);
                        long lastUserInfoRequestTime = preferences.getLong(CRReceiver.LAST_USER_INFO_REQUEST_DATE, 0L);
                        long currentTimeInMillis = System.currentTimeMillis();
                        
                        /*
                         * The following is just a kludge to handle Redmine issue #110089.
                         * Some phones are making thousands of getUserInfo() calls in a day, each of which does an HTTP request to CAS.
                         * The code below just restricts getUserInfo() calls from here to at most one an hour.
                         */
                        if ((currentTimeInMillis - lastUserInfoRequestTime) > 60*60*1000)
                        {
                            if (ServerAPI.getInstance(context).getUserInfo().errorCode == ServerAPI.RESULT_OK)
                            {
                                preferences.edit().putLong(CRReceiver.LAST_USER_INFO_REQUEST_DATE, currentTimeInMillis).commit();
                            }
                        }
                    }
                }
            }
        }
    }
}
