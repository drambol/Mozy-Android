package com.mozy.mobile.android.activities.tasks;

import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.web.containers.ListDownload;

import android.content.Context;

public class GetRecentFileListTask extends GetFileListTask {

    public GetRecentFileListTask(Context context, 
                        String containerLink,                         
                        Listener listener) {
       super(context,containerLink,null,true,false, listener);
    }
        
    protected String doInBackground(String... params) {
        ListDownload listDownload = null;
           
        listDownload = ServerAPI.getInstance(context).getFilesRecentlyAdded(this.containerLink,
                true,
                MAX_RESULTS,
                params[0],
                false);
        
        errorCode = listDownload.errorCode;
        list = listDownload.list;

        return listDownload.nextIndex;
    }
}
