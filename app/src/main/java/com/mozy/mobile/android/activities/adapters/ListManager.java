package com.mozy.mobile.android.activities.adapters;

import java.util.ArrayList;

import android.content.Context;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.tasks.GetFileListTask;
import com.mozy.mobile.android.activities.tasks.GetRecentFileListTask;
import com.mozy.mobile.android.utils.LogUtil;

public class ListManager {

    public interface Listener {
        boolean enabled();
        void preprocessList(ArrayList<Object> list);
        void onListPrepared(boolean success, int errorCode, String nextStartIndex, final String dirLink, final String searchText);
    }
    private static final ListManager INSTANCE = new ListManager();

    public static ListManager getInstance() {
        return INSTANCE;
    }

    private ListDatabase database;

    private ListManager() {
        database = null;
    }

    public synchronized String getNextIndex(final Context context, final String containerLink, final String searchQuery, final boolean recurse) {
        return getDatabase(context).getNextIndex(containerLink, searchQuery, recurse);
    }

    public synchronized Object getListItem(final Context context, final String containerLink, final String searchQuery, final boolean includeDirectories, final boolean photosOnly, final boolean recurse, int index) {
        return getDatabase(context).getListItem(containerLink, searchQuery, includeDirectories, photosOnly, recurse, index);
    }

    public synchronized int getCount(final Context context, final String dirLink, final String searchQuery, final boolean includeDirectories, final boolean photosOnly, final boolean recurse) {
        return getDatabase(context).getCount(dirLink, searchQuery, includeDirectories, photosOnly, recurse);
    }

    public synchronized int getPhotoCount(final Context context, int position)
    {
        return getDatabase(context).getPhotoCount(position);
    }

    public String prepareList(final Context context, final String dirLink,
            final String searchText, final boolean recurse, final String startIndex, final Listener listener) {
        new GetFileListTask(context, dirLink, searchText, recurse, false, new GetFileListTask.Listener() {

            @Override
            // @nextIndex - This String parameter is passed to the MIPApi to get the next chunk of files
            // @list      - This parameter is the list of 'CloudFile' based objects returned from the server
            public void onCompleted(String nextIndex, int errorCode,
                    ArrayList<Object> list) {
                if(listener.enabled())
                {
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        boolean success = false;

                        // Call a call-back to allow any final processing of the list that higher level
                        // code wants to do.
                        listener.preprocessList(list);

                        if (getDatabase(context).updateList(list, nextIndex, dirLink, searchText, recurse)) {
                            success = true;
                        }
                        
                        
                        listener.onListPrepared(success, errorCode, nextIndex, dirLink, searchText);
                    } else {
                        listener.onListPrepared(false, errorCode, null, dirLink, searchText);
                    }
                }
                else
                {
                    LogUtil.debug("ListManager", "Adapter not enabled, skipping callback for " + dirLink);
                }
            }
        }).execute(startIndex);
        return null;
    }

    
    public String prepareRecentFilesList(final Context context, final String dirLink,
            final String searchText, final boolean recurse, final String startIndex, final Listener listener) {
        new GetRecentFileListTask(context, dirLink, new GetRecentFileListTask.Listener() {

            @Override
            // @nextIndex - This String parameter is passed to the MIPApi to get the next chunk of files
            // @list      - This parameter is the list of 'CloudFile' based objects returned from the server
            public void onCompleted(String nextIndex, int errorCode,
                    ArrayList<Object> list) {
                if(listener.enabled())
                {
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        boolean success = false;

                        // Call a call-back to allow any final processing of the list that higher level
                        // code wants to do.
                        listener.preprocessList(list);

                        if (getDatabase(context).updateList(list, nextIndex, dirLink, searchText, recurse)) {
                            success = true;
                        }
                        listener.onListPrepared(success, errorCode, nextIndex, dirLink, searchText);
                    } else {
                        listener.onListPrepared(false, errorCode, null, dirLink, searchText);
                    }
                }
                else
                {
                    LogUtil.debug("ListManager", "Adapter not enabled, skipping callback for " + dirLink);
                }
            }
        }).execute(startIndex);
        return null;
    }

    private ListDatabase getDatabase(Context context) {
        if (database == null) {
            database = new ListDatabase(context.getApplicationContext());
        }
        return database;
    }

    public void removeItem(Context context, int position, boolean directoriesOnly, boolean photosOnly)
    {
        getDatabase(context).removeItem(position, directoriesOnly, photosOnly);
    }

    public void cleanUp(final Context context, boolean emptyCache) {
        getDatabase(context).clean(emptyCache);
    }
}
