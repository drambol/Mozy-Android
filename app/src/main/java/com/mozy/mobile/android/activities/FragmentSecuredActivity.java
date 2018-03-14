
package com.mozy.mobile.android.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;

import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ResultCodes;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.provisioning.ProvisioningListener;
import com.mozy.mobile.android.utils.SystemState;

public abstract class FragmentSecuredActivity extends FragmentActivity implements ProvisioningListener{

    public final static int RESULT_CODE_NEED_REFRESH = Activity.RESULT_FIRST_USER; 
    protected static final int MENU_HOME = 0;
    protected static final int MENU_HELP = 1;
    protected static final int MENU_FILES = 2;
    protected static final int MENU_SETTINGS = 3;
    protected static final int MENU_LAST = MENU_SETTINGS;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Provisioning.getInstance(this).registerListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

        if (SystemState.isSyncEnabled())
        {
            UploadManager.initialize(getApplicationContext());
            
            if(SystemState.isPasscodeEnabled() && Provisioning.getInstance(getApplicationContext()).getSecurityMode() == true)
            {
                if(InactivityAlarmManager.getInstance(this).isAlarmReset())
                    SecuredActivity.catchAndReleaseProcessing(FragmentSecuredActivity.this);
            }
            else
            {
                SecuredActivity.catchAndReleaseProcessing(FragmentSecuredActivity.this);
            }
        }
        
        
        SystemState.setResume();
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
}
