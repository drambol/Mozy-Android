package com.mozy.mobile.android.activities.tasks;

import java.util.ArrayList;
import com.mozy.mobile.android.activities.adapters.UploadFolderListAdapter;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.upload.UploadFolderScreenActivity.ListAdapterDataListener;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.web.containers.ListDownload;

import android.content.Context;
import android.os.AsyncTask;

public class GetUploadedFileListTask extends AsyncTask<Void, Void, Integer>{
    
    protected final Context context;
    protected final String containerLink;
    protected ArrayList<Object> list;

    protected  UploadFolderListAdapter adapter;
    private ListAdapterDataListener listener;

    public GetUploadedFileListTask(Context context, String containerLink, UploadFolderListAdapter adapter, ListAdapterDataListener listener)
    {
        this.context = context.getApplicationContext();
        this.containerLink = containerLink;
        this.list = null;
        this.adapter  = adapter;
        this.listener = listener;
    }
        
    protected Integer doInBackground(Void... params) {
        ListDownload listDownload = null;
        listDownload = ServerAPI.getInstance(context).getExistingUploadedFiles(context, this.containerLink, false);
        
        int errorCode = listDownload.errorCode;
        list = listDownload.list;

        return errorCode;
    }
    
    public void onPostExecute(Integer errorCode) 
    {
        
        this.adapter.clear();
        
        if(list != null && errorCode == ServerAPI.RESULT_OK)
        {  
            
            ArrayList<Object> tmpList = new ArrayList<Object>(list);
            
            // add all folders first
            for(int i = 0; i < tmpList.size(); i++)
            {
                if(tmpList.get(i) instanceof Directory)
                {
                    this.adapter.add(tmpList.get(i));
                    tmpList.remove(i);
                    i--;
                }
            }
            
            // remaining are all files
            for(int j = 0; j < tmpList.size(); j++)
            {
                this.adapter.add(tmpList.get(j));
            }
        }
        
        this.adapter.notifyDataSetChanged();
        
        synchronized (this) 
        {
            if (this.listener != null) 
            {
                (this.listener).onDataRetrieved(this.adapter, errorCode, list);
            }
        }
        return;
    }
}
