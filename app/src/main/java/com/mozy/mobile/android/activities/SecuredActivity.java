/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/

package com.mozy.mobile.android.activities;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ResultCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.catch_release.CatchAndReleaseInitialSetupActivity;
import com.mozy.mobile.android.catch_release.CatchAndReleaseSettingsActivity;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.provisioning.ProvisioningListener;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public abstract class SecuredActivity extends ErrorActivity implements ProvisioningListener{

    public final static int RESULT_CODE_NEED_REFRESH = Activity.RESULT_FIRST_USER;
    private static Queue<Activity> activityQueue = new LinkedList<Activity>();
    protected boolean handlingError = false;
    
    protected static final int MENU_HOME = 0;
    protected static final int MENU_HELP = 1;
    protected static final int MENU_FILES = 2;
    protected static final int MENU_SETTINGS = 3;
    protected static final int MENU_LAST = MENU_SETTINGS;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Provisioning.getInstance(this).registerListener(this);

        // Because the Android API does not give enough hooks to manipulate the Activity queue, I have to reproduce
        // it here, because in certain cases I want to clear it.
        synchronized(SecuredActivity.activityQueue)
        {
            SecuredActivity.activityQueue.add(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        synchronized(SecuredActivity.activityQueue)
        {
            SecuredActivity.activityQueue.remove(this);
        }
    }

    // Calls finalize() on all activities stored in the queue.
    protected void clearActivityQueue()
    {
        synchronized(SecuredActivity.activityQueue)
        {
            Iterator<Activity> iterator = SecuredActivity.activityQueue.iterator();
            while (iterator.hasNext())
            {
                Activity activity = iterator.next();
                if (activity != this)
                {
                    activity.finish();
                }
            }
            SecuredActivity.activityQueue.clear();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (SystemState.isPasscodeEnabled() && Provisioning.getInstance(getApplicationContext()).getSecurityMode() == true)
        {
            InactivityAlarmManager.getInstance(this).registerActivity(this);

            updateAlarm();
        
            InactivityAlarmManager.getInstance(this).activate();
        }
        else
        {
            InactivityAlarmManager.getInstance(getApplicationContext()).deactivate(); 
        }

        
        // We crashed need to repopulate the device list
        
        if(SystemState.getDeviceList() == null)
        {
            Provisioning provisioning = Provisioning.getInstance(this);
    
            String token = provisioning.getMipAccountToken();
            
            String tokenSecret = provisioning.getMipAccountTokenSecret();
        
            if((token != null && token.length() != 0 && tokenSecret != null && tokenSecret.length() != 0))
            {
                // Make the app responsive we need to do this asynchronously
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        ServerAPI.getInstance(getApplicationContext()).getUserInfo();
                        initCatchReleaseProcessing();
                    }
                });

                thread.start();
                
            }
        }
        else
        {
            initCatchReleaseProcessing();
        }
        
        SystemState.setResume();
    }

    private void initCatchReleaseProcessing() {
        if (SystemState.isSyncEnabled())
        {
            UploadManager.initialize(getApplicationContext());
            
            if(SystemState.isPasscodeEnabled() && Provisioning.getInstance(getApplicationContext()).getSecurityMode() == true)
            {
                if(InactivityAlarmManager.getInstance(this).isAlarmReset())
                    SecuredActivity.catchAndReleaseProcessing(SecuredActivity.this);
            }
            else
            {
                SecuredActivity.catchAndReleaseProcessing(SecuredActivity.this);
            }
        }
    }

    // Code that checks with the UploadManager to see if there are any new files that the user
    // might want to upload.
    protected static void catchAndReleaseProcessing(Activity activity)
    {
        
        // Do not do any catch and release processing as they may have arrived at this screen in response to
        // catch and release dialogs.
        if(activity instanceof CatchAndReleaseSettingsActivity)
            return;
        
        if (!SystemState.isCrInit())
        {
            UploadManager.sCurrentSettings = new UploadSettings(activity.getApplicationContext());
            SystemState.setCrInit(UploadManager.sCurrentSettings.getUploadInitialized());
        }
        
        if (!SystemState.isCrInit())        
        {       
            // These are files that are not upload yet.        
            int mediaFilesInUploadQueue = UploadManager.sAutoUploadQueue != null ? UploadManager.sAutoUploadQueue.getQueueSize() : 0;              
    
            if (mediaFilesInUploadQueue != 0)       
            {       
                // Have the user configure Catch and Release        
                Intent intent = new Intent(activity, CatchAndReleaseInitialSetupActivity.class);        
                 activity.startActivity(intent);     
             }       
          }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (SystemState.isPasscodeEnabled() && Provisioning.getInstance(getApplicationContext()).getSecurityMode() == true)
            InactivityAlarmManager.getInstance(this).unregisterActivity(this);
        SystemState.setPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        SystemState.setStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.REQ_CODE_PIN_LOGIN) {
            if (resultCode != ResultCodes.PIN_OK) {
                this.setResult(ResultCodes.EXIT);
                InactivityAlarmManager.getInstance(this).resetNotification();
                finish();
            } else {
                InactivityAlarmManager.getInstance(this).reset();
            }
        }
        else if (resultCode == ResultCodes.EXIT) {
            finish();
        }
    }

    public void updateAlarm() {
        InactivityAlarmManager.getInstance(this).update(SystemState.INACTIVITY_DELAY);
    }

    public synchronized static void triggerSecurity(Activity activity, boolean already_handled) {
        LogUtil.enter(activity, "triggerSecurity");

        if (already_handled) 
        {
            // Bring the passcode page to front
            Intent intent = getIntentForPinManActivity(activity);
            if( intent != null)
            {
                activity.startActivityForResult(intent, RequestCodes.REQ_CODE_PIN_LOGIN);
            }
            else
                activity.finish();
        }
        else if (!SystemState.isPasscodeEnabled())
        {
            activity.finish();
        }
        else 
        {
            Intent intent = new Intent(activity.getApplicationContext(), PinManActivity.class);
            intent.putExtra(PinManActivity.ACTION, PinManActivity.ACTION_VALIDATE_WITH_RESET);
            activity.startActivityForResult(intent, RequestCodes.REQ_CODE_PIN_LOGIN);
        }
    }
    
    private static Intent getIntentForPinManActivity(Activity activity) {
        Intent intent = new Intent(activity.getApplicationContext(), PinManActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return intent;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_HOME, 0, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        menu.add(0, MENU_HELP, 1, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_FILES, 2, getResources().getString(R.string.menu_files)).setIcon(R.drawable.allfiles);

        return true;
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_HOME:
            /*
             * Create intent for returning to homescreen
             * while clearing stacked activites to reset "history".
             */
            NavigationTabActivity.returnToHomescreen(this);
            finish();
            return true;
        case MENU_FILES:
            NavigationTabActivity.returnToFilescreen(this);
            return true;
        case MENU_HELP:
            MainSettingsActivity.goToHelp(this);
            return true;
        case MENU_SETTINGS:
            MainSettingsActivity.goToMainSettings(this);
            return true;
         default:
             return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    protected Dialog createErrorDialog(int errorCode)
    {
        if (this.handlingError)
        {
            this.handlingError = false;

            int errorString = 0;
            int errorTitle = R.string.error; 
            int dialogId = ErrorActivity.DIALOG_ERROR_NO_FINISH_ID;

            switch (errorCode)
            {
                case ServerAPI.RESULT_UNAUTHORIZED:
                case ServerAPI.RESULT_FORBIDDEN:
                        errorString = R.string.errormessage_authorization_failure;
                    break;
                case ServerAPI.RESULT_INVALID_CLIENT_VER:
                    errorString = R.string.client_upgrade_required;
                    errorTitle = R.string.client_upgrade_title;
                    // Stop the Mozy Service
                    MozyService.stopMozyService(this.getApplicationContext());
                    SystemState.setMozyServiceEnabled(false, this.getApplicationContext());
                    SystemState.setManualUploadEnabled(false, this.getApplicationContext());
                break;
                case ServerAPI.RESULT_INVALID_TOKEN:
                    errorString = R.string.device_revoked_body;
                    break;
                case ServerAPI.RESULT_AUTHORIZATION_ERROR:
                    errorString = R.string.authorization_error;
                    break;
                case ServerAPI.RESULT_INVALID_USER:
                    errorString = R.string.invalid_user;
                    break;
                case ServerAPI.RESULT_CONNECTION_FAILED:
                case ServerAPI.RESULT_UNKNOWN_PARSER:
                case ServerAPI.RESULT_UNKNOWN_ERROR:
                     errorString = R.string.error_not_available;
                    break;
                 default:
                     errorString = R.string.error_not_available;
                     break;
            }
            if (errorString != 0)
            {
                return createGenericErrorDialog(dialogId, errorTitle, errorString, R.string.ok_button_text);
            }
        }

        return null;
    }
        
    
 // Implementation of ProvisioningListener
    public void onChange(int id)
    {
        // If any of the upload settings has changed, then inform the backup code
        if (id == ProvisioningListener.UPLOAD)
        {
            UploadManager.initialize(this);  // make sure we are initialized
            UploadManager.uploadSettingsChange(new UploadSettings(this));
        }
    }
}
