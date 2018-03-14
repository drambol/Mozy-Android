package com.mozy.mobile.android.activities.helper;

import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.SystemState;

import android.content.Context;

// This class just packages up the upload settings in a convenient package
public class UploadSettings 
{
    private static final int PHOTO_UPLOAD_AUTOMATIC         = 0x0001;
    private static final int VIDEO_UPLOAD_AUTOMATIC         = 0x0002;
    private static final int UPLOAD_ONLY_ON_WIFI            = 0x0004;
    private static final int UPLOAD_OFF_WHEN_ROAMING        = 0x0008;
    private static final int UPLOAD_SETTINGS_INITIALIZED    = 0x0010;
    private int settings = 0;
    
    // This constructor reads the settings from the provisioning class
    public UploadSettings(Context context)
    {
        this.settings = Provisioning.getInstance(context).getUploadSettings();
    }

    public void setPreferences(Context context)
    {
        Provisioning.getInstance(context).setUploadSettings(this.settings);
        
        mozyServiceActionOnSettings(context);
    }

    /**
     * @param context
     */
    public void mozyServiceActionOnSettings(Context context) {
        UploadManager.sCurrentSettings = this;
        
        if (getPhotoUploadType() || getVideoUploadType())
        {
            // make sure the service is enabled
            SystemState.setMozyServiceEnabled(true, context);
            UploadManager.startCatchAndRelease(context, MozyService.running);
        }
        else
        {
            if(SystemState.hasEncryptedDevice(SystemState.getDeviceList()) == false)
            {
                MozyService.stopMozyService(context);
                // Disable Mozy Service
                SystemState.setMozyServiceEnabled(false, context);
            }
        }
        
        // Now that we have updated our upload settings, if we enabled uploads
        
        if(UploadManager.sAutoUploadQueue != null && UploadManager.sAutoUploadQueue.getQueueSize() > 0)
        {
            if(this.getAutoCandR())
            {
                // start uploading, callback for the listener
                UploadManager.sAutoUploadQueue.onContentChangedListener(true, UploadManager.getQueueListener());
            }
        }
    }

    public void setVideoUploadType(boolean value)
    {
        if (value)
            this.settings |= VIDEO_UPLOAD_AUTOMATIC;
        else
            this.settings &= ~VIDEO_UPLOAD_AUTOMATIC;
    }

    public boolean getVideoUploadType()
    {
        return (this.settings & VIDEO_UPLOAD_AUTOMATIC) == VIDEO_UPLOAD_AUTOMATIC;
    }

    public void setPhotoUploadType(boolean value)
    {
        if (value)
            this.settings |= PHOTO_UPLOAD_AUTOMATIC;
        else
            this.settings &= ~PHOTO_UPLOAD_AUTOMATIC;
    }
    
    public boolean getPhotoUploadType()
    {
        return (this.settings & PHOTO_UPLOAD_AUTOMATIC) == PHOTO_UPLOAD_AUTOMATIC;
    }

    public void setOffWhenRoaming(boolean value)
    {
        if (value)
            this.settings |= UPLOAD_OFF_WHEN_ROAMING;
        else
            this.settings &= ~UPLOAD_OFF_WHEN_ROAMING;
    }

    public boolean getOffWhenRoaming()
    {
        return (this.settings & UPLOAD_OFF_WHEN_ROAMING) == UPLOAD_OFF_WHEN_ROAMING;
    }
    
    public void setOnlyOnWifi(boolean value)
    {
        if (value)
            this.settings |= UPLOAD_ONLY_ON_WIFI;
        else
            this.settings &= ~UPLOAD_ONLY_ON_WIFI;
    }

    public boolean getOnlyOnWifi()
    {
        return (this.settings & UPLOAD_ONLY_ON_WIFI) == UPLOAD_ONLY_ON_WIFI;
    }

    public void setUploadInitialized()
    {
        this.settings |= UPLOAD_SETTINGS_INITIALIZED;
    }

    public boolean getUploadInitialized()
    {
        return (this.settings & UPLOAD_SETTINGS_INITIALIZED) == UPLOAD_SETTINGS_INITIALIZED;
    }

    public boolean getAutoCandR()
    {
        return (getUploadInitialized() && (getPhotoUploadType() || getVideoUploadType()));
    }
}
