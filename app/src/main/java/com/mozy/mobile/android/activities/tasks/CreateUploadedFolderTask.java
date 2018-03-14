package com.mozy.mobile.android.activities.tasks;

import java.io.IOException;
import java.util.ArrayList;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.adapters.UploadFolderListAdapter;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.upload.UploadFolderScreenActivity.UploadFolderCreateListener;
import com.mozy.mobile.android.web.uploadFileAPI;
import com.mozy.mobile.android.web.containers.StringDownload;
import android.content.Context;
import android.os.AsyncTask;

public class CreateUploadedFolderTask extends AsyncTask<Void, Void, Integer>{
    
    protected final Context context;
    protected final String newDirName;
    protected final String syncFolder;
    protected ArrayList<Object> list;
    protected  UploadFolderListAdapter adapter;
    private UploadFolderCreateListener listener;

    public CreateUploadedFolderTask(Context context, String newDirName, String syncFolder, UploadFolderListAdapter adapter, UploadFolderCreateListener listener)
    {
        this.context = context.getApplicationContext();
        this.newDirName = newDirName;
        this.syncFolder = syncFolder;
        this.list = null;
        this.adapter  = adapter;
        this.listener = listener;
    }
        
    protected Integer doInBackground(Void... params) {
        int errorCode =  ErrorCodes.NO_ERROR;
        if(newDirName != null && newDirName.equalsIgnoreCase("") == false)
        {
            String deviceLink = ServerAPI.getInstance(this.context).GetCloudDeviceLink();
            if(deviceLink != null)
            {
                try {
                     StringDownload result = (uploadFileAPI.getInstance(this.context)).createUploadDir(deviceLink,this.syncFolder, this.newDirName);
                     errorCode = result.errorCode;
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        
        return errorCode;
    }
    
    public void onPostExecute(Integer errorCode) 
    {
        this.adapter.notifyDataSetChanged();
        
        synchronized (this) 
        {
            if (this.listener != null) 
            {
                (this.listener).onCompleted(this.adapter, errorCode);
            }
        }
        return;
    }
}
