/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
 */

package com.mozy.mobile.android.activities.tasks;

import java.util.ArrayList;
import java.util.Hashtable;

import com.mozy.mobile.android.utils.SystemState;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.ErrorManager;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.application.MozyFilesDatabase;
import com.mozy.mobile.android.web.containers.ListDownload;
import com.mozy.mobile.android.files.Device;


import android.content.Context;
import android.os.AsyncTask;

public class GetDevicesTask extends AsyncTask<Void, Integer, Void> {
    private Context context;
    private ArrayList<Object> encryptedDevices;
    private int errorCode;
    private ListDownload deviceList;
    
    private final Listener listener;
    
    private static Object getDeviceLock = new Object();
    
    public static interface Listener {
        void onCompleted(int errorCode);
    }
    
    public GetDevicesTask(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    public Void doInBackground(Void... params) {
        
        LogUtil.debug(this, "::doInBackground() start");

        // Ensure we aren't reentrant while we fetch the device list
         
        synchronized (getDeviceLock) 
        {
            this.deviceList = ServerAPI.getInstance(context).getDevices();
           
            // retain the current device list in case we loose connectivity
            if( this.deviceList != null && deviceList.errorCode == ServerAPI.RESULT_OK)
                SystemState.set_save_deviceList(this.deviceList);
           
            if(this.deviceList != null && 
                    (this.deviceList.errorCode == ServerAPI.RESULT_CONNECTION_FAILED || this.deviceList.errorCode == ServerAPI.RESULT_INVALID_CLIENT_VER))
            {
                if(SystemState.get_save_deviceList() != null)  // Assign it so we can still see containers for download screen
                   this.deviceList = SystemState.get_save_deviceList();
            }
            

            if(deviceList != null)
            {
                SystemState.setDeviceList(deviceList.list);

                errorCode = deviceList.errorCode;
            }
            else
            {
                errorCode = ErrorCodes.NO_ERROR;
            }
            
            if((this.deviceList.errorCode == ServerAPI.RESULT_OK  || this.deviceList.errorCode == ServerAPI.RESULT_CONNECTION_FAILED)
                    && this.deviceList != null && this.deviceList.list != null  && this.deviceList.list.size() > 0)  // for temporary connection failure we still maintain the cloud device link
            {
                ServerAPI.getInstance(context).setCloudDeviceLink(deviceList);
                ServerAPI.getInstance(context).setDecryptedFilesCleanupMgr(deviceList);
            }
            else
            {
                ServerAPI.getInstance(context).setCloudDeviceLink(null);
                ServerAPI.getInstance(context).setDecryptedFilesCleanupMgr(null);
                SystemState.setMozyServiceEnabled(false, context); // disable mozy service
            }
        }

        LogUtil.debug(this, "::doInBackground() end");

        return null;
    }


    @Override
    protected void onPostExecute(Void result) {

            if (deviceList.errorCode == ErrorCodes.NO_ERROR) 
            {   
                encryptedDevices = SystemState.getEncryptedDeviceList();
                GetDevicesTask.createEncryptedDBAndInitContainerAccessTable(context, this.encryptedDevices);      
            }
           
            
            if (errorCode != ErrorCodes.NO_ERROR) {
                ErrorManager.getInstance().reportError(errorCode, ErrorManager.ERROR_TYPE_GENERIC);
            }
            listener.onCompleted( errorCode);
    }

    /**
     * 
     */
    public static void createEncryptedDBAndInitContainerAccessTable(Context context, ArrayList<Object> encryptedDevices) {
        Hashtable<String, Boolean> accessTable = SystemState.getEncryptedContainerAccessTable();
        
        for(int i = 0; encryptedDevices != null  && i < encryptedDevices.size(); i++)
        {
            // Check if exists
            if(accessTable.containsKey(((Device)encryptedDevices.get(i)).getTitle()) == false)
            {
                if(SystemState.isManagedKeyEnabled(context) == true)
                {
                    accessTable.put(((Device)encryptedDevices.get(i)).getTitle(),false);
                }
                else
                {
                    accessTable.put(((Device)encryptedDevices.get(i)).getTitle(),true);
                }
            }
        }
        
        if(SystemState.getDeviceList() != null && SystemState.getDeviceList().size() != 0)
        {
            if(SystemState.mozyFileDB == null)
                SystemState.mozyFileDB = new MozyFilesDatabase(context);
            else
                SystemState.mozyFileDB.close();  // Need to close and reopen
            
            SystemState.mozyFileDB.open();
            SystemState.setEncryptedContainerAccessTable(accessTable);
        }
    }
}
