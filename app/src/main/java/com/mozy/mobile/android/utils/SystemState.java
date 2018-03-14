package com.mozy.mobile.android.utils;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.ErrorManager;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.tasks.GetDevicesTask;
import com.mozy.mobile.android.activities.upload.FilteringMozyUploadActivity;
import com.mozy.mobile.android.application.MozyFilesDatabase;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.web.containers.ListDownload;

/*
 * This class is the equivalent of global variables.
 * Don't abuse it!
 */

public class SystemState
{
    public static Device cloudContainer;
    
    public final static int CIPHER_BLOWFISH = 0;
    public final static int CIPHER_AES = 1;
    public final static int PLATFORM_WINDOWS = 0;
    public final static int PLATFORM_LINUX = 1;
    public final static int SRCTYPE_PASSPHRASE = 0; 
    public final static int SRCTYPE_FILE = 1;

    // Activity State flags for determining if we trigger security
    // Whenever a Secured Activity is stopped without another Secured Acctivity being resumed
    // We will trigger security on that activity
    private static final int ACTIVITY_STATE_NONE    = 0X0000;
    private static final int ACTIVITY_STATE_RESUMED = 0x0001;
    private static final int ACTIVITY_STATE_PAUSED  = 0x0002;
    private static int activityState = ACTIVITY_STATE_NONE;
    
    // Configuration flags, tracking, and querying
    private static final int MOZYCONFIG_UPLOAD      = 0x0001;
    private static final int MOZYCONFIG_SYNC_FOLDER = 0x0002;
    private static final int MOZYCONFIG_EXPORT       = 0x0004;
    private static final int MOZYCONFIG_PASSCODE    = 0x0008;
    private static final int MOZYCONFIG_SPACE_USED  = 0x0010;

    // Sync available and Enabled configuration
    private static final int MOZYCONFIG_SYNC_ENABLED = (MOZYCONFIG_UPLOAD | MOZYCONFIG_SYNC_FOLDER);

    // $TODO: initialization needs to happen for real.
    private static int mozyConfig = MOZYCONFIG_EXPORT | MOZYCONFIG_PASSCODE | MOZYCONFIG_UPLOAD;
    
 // Wait at least thirty seconds between prompts to upload files.
    public static final long UPLOAD_PROMPT_GAP = 30 * 1000;

    /**
     * Delay until inactivity is triggered.
     */
    public static final long INACTIVITY_DELAY = 5 * 60 * 1000;
    
    private static boolean crInit = false;
    
    private static ArrayList<Object> devicelist;
   
    
    private static Object deviceQueryLock = new Object();

    public static Device getSyncDevice() {
        Device t = null;
        synchronized (deviceQueryLock) 
        {
            t = SystemState.cloudContainer;
        }
        return t;
    }


    private static ListDownload save_deviceList;

    public static int getDevicePlusSyncCount()
    {
        ArrayList <Object> devices = SystemState.getDeviceList();
 
        if(devices != null)
        {
            return devices.size();
        }
        else
            return 0;
    }
    

    private static Hashtable<String, Boolean>  EncryptedContainerAccessTable; 
    
    public static MozyFilesDatabase mozyFileDB;

    public static boolean isUploadEnabled()
    {
        return (mozyConfig & MOZYCONFIG_UPLOAD) == MOZYCONFIG_UPLOAD;
    }
    public static boolean isSyncAvailable()
    {
        return (mozyConfig & MOZYCONFIG_SYNC_FOLDER) == MOZYCONFIG_SYNC_FOLDER;
    }
    public static boolean isExportEnabled()
    {
        return (mozyConfig & MOZYCONFIG_EXPORT) == MOZYCONFIG_EXPORT;
    }
    public static boolean isPasscodeEnabled()
    {
        return (mozyConfig & MOZYCONFIG_PASSCODE) == MOZYCONFIG_PASSCODE;
    }
    public static boolean isSpaceUsedEnabled()
    {
        return (mozyConfig & MOZYCONFIG_SPACE_USED) == MOZYCONFIG_SPACE_USED;
    }

    public static boolean isSyncEnabled()
    {
        return (mozyConfig & MOZYCONFIG_SYNC_ENABLED) == MOZYCONFIG_SYNC_ENABLED;
    }
    
    
    private static String managedKeyUrl = null;
    
    public static String getManagedKeyUrl() {
        return managedKeyUrl;
    }
    public static void setManagedKeyUrl(String managedKeyUrl) {
        SystemState.managedKeyUrl = managedKeyUrl;
    }
    public static byte[] getManagedKey(Context context)
    {
       byte[] managed_key = Provisioning.getInstance(context).getManagedKey();
       return managed_key;
    }
    
    public static void setManagedKey(Context context, byte[] ckey)
    {
        Provisioning.getInstance(context).setManagedKey(ckey);
    }
    
    public static boolean isManagedKeyEnabled(Context context)
    {
        if(SystemState.getManagedKey(context) == null)
          return false;
        return true;
    }
    
    /**
     * Toggles 'Mozy' option when sharing an image in the Gallery app on/off.
     *
     * It is not possible to enable/disable intent filters, but we solve this
     * by having two identical activities (one being a clean extension of
     * the other). This way we have an intent filter on the extending class
     * and enable/disable this class using the package manager.
     *
     * In essence, the extending class becomes a wrapper that provides nothing
     * but the intent-listening functionality.
     *
     * @param enabled Whether Mozy option for image sharing should be enabled
     * @param context The context to use
     */
    public static void setManualUploadEnabled(boolean enabled, Context context)
    {
        /*
         * Update config
         */
        if(enabled)
        {
            mozyConfig = mozyConfig | MOZYCONFIG_SYNC_FOLDER;
        }
        else
        {
            mozyConfig = mozyConfig & ~MOZYCONFIG_SYNC_FOLDER;
        }

        /*
         * Get the component name for the "intent-listening" activity
         */
        ComponentName componentName = new ComponentName(context, FilteringMozyUploadActivity.class);

        /*
         * Enable/disable the activity
         */
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);

    }

    public static void setMozyServiceEnabled(boolean enabled, Context context)
    {
        /*
         * Get the component name for the "intent-listening" activity
         */
        ComponentName componentName = new ComponentName(context, MozyService.class);

        /*
         * Enable/disable the activity
         */
        int state = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        context.getPackageManager().setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);

    }

    public static void setResume()
    {
        activityState |= ACTIVITY_STATE_RESUMED;
    }
    
    public static void setPause()
    {
        activityState |= ACTIVITY_STATE_PAUSED;        
    }
    
    public static void setStop()
    {
        activityState = ACTIVITY_STATE_NONE;
    }
    
    public static boolean getStopped()
    {
        return (ACTIVITY_STATE_NONE == activityState);
    }
    

    public static ArrayList<Object> getDeviceList() {
        ArrayList<Object> t = null;
        synchronized (deviceQueryLock) 
        {
            t = devicelist;
        }
        return t;
    }
    

    public static void setDeviceList(ArrayList<Object> list) {
        synchronized (deviceQueryLock) 
        {
            devicelist = list;
        }
    }

    public synchronized static Hashtable<String, Boolean> getEncryptedContainerAccessTable(){
        
       if( SystemState.EncryptedContainerAccessTable == null)
       {
           SystemState.EncryptedContainerAccessTable = new Hashtable<String, Boolean>(); 
       }
        return SystemState.EncryptedContainerAccessTable;
      }
    
    public static void setEncryptedContainerAccessTable(Hashtable<String, Boolean> t)
    {
        SystemState.EncryptedContainerAccessTable = t;
    }
    public static String getHelpUrl(Context context)
    {
        Locale loc = Locale.getDefault();
        String lang = loc.getLanguage();
        String country = loc.getCountry();

        String strHelpUrl = context.getString(R.string.help_link);
        strHelpUrl += "&lang=";
        if (lang.equals("it") || lang.equals("fr") || lang.equals("de") ||  lang.equals("es")
                || lang.equals("ja"))
            strHelpUrl += lang;
        else if (lang.equals("nl"))
            strHelpUrl += "nl_NL";
        else if (lang.equals("pt"))
            strHelpUrl += "pt_BR";
        else if (lang.equals("en") && country.equals("GB"))
            strHelpUrl += "en_GB";
        else
            strHelpUrl += "en_US";

        return strHelpUrl;
    }
    
    
    public static ArrayList<Object> getEncryptedDeviceList()
    {
        ArrayList<Object> deviceList = SystemState.getDeviceList();
        ArrayList<Object> encryptedDeviceList = new ArrayList<Object>();
        
        
        int numDevices = 0;
        
        if(deviceList != null)
            numDevices = deviceList.size();

        
        for (int i = 0; i < numDevices; i++) 
        {
            Device d = (Device) deviceList.get(i);
            if (((Device) d).getEncrypted())
            {
                encryptedDeviceList.add(d);
            }
        }
        return encryptedDeviceList;
    }
    
    
    public static boolean hasEncryptedDevice(ArrayList<Object> deviceList)
    {
        int numDevices = deviceList.size();
        
        boolean result = false;
        
        for (int i = 0; i < numDevices; i++) 
        {
            Device d = (Device) deviceList.get(i);
            if (((Device) d).getEncrypted())
            {
                result = true;
                break;
            }
        }
        return result;
    }
    
    
    public static boolean isSync(Context context, String deviceTitle)
    {
        if(deviceTitle != null)
            return (deviceTitle.equalsIgnoreCase(context.getResources().getString(R.string.sync_title)));
        return false;
    }
    
    public static String getTitleForDevice(String deviceId)
    {
        ArrayList<Object> deviceList = SystemState.getDeviceList();
        int numDevices = (deviceList != null) ? deviceList.size() : 0;
        
        String result = null;
        
        for (int i = 0; i < numDevices; i++) 
        {
            Device d = (Device) deviceList.get(i);
            if (((Device) d).getEncrypted())
            {
                if(d.getId().equals(deviceId))
                {
                    result = d.getTitle();
                    break;
                }
            }
        }
        return result;
    }
    
    public static boolean isLocalFileDecrypted(LocalFile localFile , CloudFile cloudFile, String containerId)
    {
        if(cloudFile != null)
        {        
            String cloudFileLink = cloudFile.getLink();
            if (localFile != null && localFile.file.exists() 
                    && (SystemState.mozyFileDB != null)
                    && (SystemState.mozyFileDB.existsFileInDB(containerId, localFile.getName(), cloudFileLink) == true)
                    && (SystemState.mozyFileDB.getDecryptDateForFile(containerId, localFile.getName(), cloudFileLink) != -1))
            {
                return true;
            }
        }
        return false;
        
    }
    
    // Specific call to handle downloaded files on SDCard, note we do not pass in the cloud file path here to figure out 
    // Decrypted file date.
    
    public static boolean  isDownloadedFileDecrypted(LocalFile localFile, String containerId)
    {
        if (localFile != null && localFile.file.exists() && (SystemState.mozyFileDB != null)
                && (SystemState.mozyFileDB.existsFileInDB(containerId, localFile.getName()) == true)
                && (SystemState.mozyFileDB.getDecryptDateForFile(containerId, localFile.getName()) != -1))
        {
            return true;
        }
        
        return false;
    }
    public static void set_save_deviceList(ListDownload save_deviceList) {
        SystemState.save_deviceList = save_deviceList;
    }
    public static ListDownload get_save_deviceList() {            
        return save_deviceList;
    }

    public static void cacheDevicesAndCreateDB(Context context, ListDownload deviceList)
    {
        int errorCode =  ErrorCodes.NO_ERROR;
        ArrayList<Object> encryptedDevices = null; 
        
        if (null != deviceList)
        {
            // retain the current device list in case we loose connectivity
            if(deviceList.errorCode == ServerAPI.RESULT_OK)
                SystemState.set_save_deviceList(deviceList);

            // Cache the devices in a list
            SystemState.setDeviceList(deviceList.list);
             
            if (deviceList.errorCode == ErrorCodes.NO_ERROR) 
            {       
                 encryptedDevices = SystemState.getEncryptedDeviceList();
                 GetDevicesTask.createEncryptedDBAndInitContainerAccessTable(context, encryptedDevices);
            } else {
                 errorCode = deviceList.errorCode;
                 ErrorManager.getInstance().reportError(errorCode, ErrorManager.ERROR_TYPE_GENERIC);
            }
        }
    }
    
   
    public static void setCrInit(boolean crInit) {
        SystemState.crInit = crInit;
    }
    public static boolean isCrInit() {
        return crInit;
    }
    
    public static boolean isAirplaneModeOn(Context context) 
    {
        return android.provider.Settings.System.getInt(context.getContentResolver(),
                android.provider.Settings.System.AIRPLANE_MODE_ON, 0) != 0;
     }
    
    public static void setFileSelectedForMozyUpload(
            boolean fileSelectedForMozyUpload) {
        SystemState.fileSelectedForMozyUpload = fileSelectedForMozyUpload;
    }
    public static boolean isFileSelectedForMozyUpload() {
        return fileSelectedForMozyUpload;
    }


    private static boolean fileSelectedForMozyUpload = false;
   
}
