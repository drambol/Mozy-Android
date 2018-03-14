package com.mozy.mobile.android.activities.tasks;

import java.util.ArrayList;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.ErrorManager;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.web.containers.ListDownload;

import android.content.Context;
import android.os.AsyncTask;

public class GetFileListTask extends AsyncTask<String, Integer, String> {

    protected static final int MAX_RESULTS = 500;
    
    public static interface Listener {
        void onCompleted(String nextIndex, int errorCode, ArrayList<Object> list);
    }
    
    protected final Context context;
    protected final String containerLink;
    protected final String searchText;
    protected final boolean recurse;
    protected final Listener listener;
    protected int errorCode;
    protected ArrayList<Object> list;
    protected final boolean photosOnly;

    public GetFileListTask(Context context, 
                        String containerLink, 
                        String searchText,
                        boolean recurse,
                        boolean photosOnly,
                        Listener listener) {
        this.context = context.getApplicationContext();

        this.searchText = searchText;
        this.containerLink = containerLink;
        this.errorCode = ErrorCodes.NO_ERROR;
        this.recurse = recurse;
        this.listener = listener;
        this.photosOnly = photosOnly;
        this.list = null;
    }
        
    protected String doInBackground(String... params) {
        ListDownload listDownload = null;
        
        listDownload = ServerAPI.getInstance(context).getFiles(this.containerLink, true, this.recurse, this.searchText, this.searchText == null ? 0 : MAX_RESULTS, params[0], this.photosOnly);
        errorCode = listDownload.errorCode;
        list = listDownload.list;

        return listDownload.nextIndex;
    }
    
    @Override
    public void onPostExecute(String nextIndex) {
        if (errorCode == ErrorCodes.NO_ERROR) {
            listener.onCompleted(nextIndex, errorCode, list);
        } else {
            ErrorManager.getInstance().reportError(errorCode, ErrorManager.ERROR_TYPE_GENERIC);
            listener.onCompleted(null, errorCode, null);
        }
    }
}
