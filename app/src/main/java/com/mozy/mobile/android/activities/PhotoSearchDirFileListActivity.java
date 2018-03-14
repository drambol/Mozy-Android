package com.mozy.mobile.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.PhotoListAdapter;

import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class PhotoSearchDirFileListActivity extends DirFileListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void prepareActivity(Bundle savedInstanceState) {

        setContentView( R.layout.generic_list_layout);

        this.recurse = false;
        this.searchText = "";
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            containerLink = extras.getString("containerLink");
            LogUtil.debug(this, "containerLink: " + containerLink);
            title = extras.getString("title");
            LogUtil.debug(this, "title: " + title);
            searchText = extras.getString("searchText");
            LogUtil.debug(this, "searchText: " + searchText);
            searchDirectory = extras.getString("searchDirectory");
            LogUtil.debug(this, "searchDirectory: " + searchDirectory);
            this.recurse = extras.getBoolean("recurse");
            LogUtil.debug(this, "recurse: " + this.recurse);
            this.canFilesBeDeleted = extras.getBoolean("canFilesBeDeleted");
            LogUtil.debug(this, "canFilesBeDeleted: " + this.canFilesBeDeleted);
            this.rootDeviceId = extras.getString("deviceId");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceId);
            this.rootDeviceTitle = extras.getString("deviceTitle");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceTitle);
            this.bDeviceEncrypted = extras.getBoolean("deviceType");
            LogUtil.debug(this, "deviceType: " + this.bDeviceEncrypted);
            platform = extras.getString("platform");
            LogUtil.debug(this, "platform: " + this.platform);
            this.currentPosition = extras.getInt("currentPosition");
            LogUtil.debug(this, "currentPosition: " + this.currentPosition);
            this.isPhotoDirGridEnabled = extras.getBoolean("isPhotoDirGridEnabled");
            LogUtil.debug(this, "photoGridEnabled: " + this.isPhotoDirGridEnabled);
        }
        
 
        this.contextMenuFlags = setContextMenuFlags(this.canFilesBeDeleted);
 
        this.refreshing = false;

        // Remember we are in listview mode.
        sGridMode = false;

        if (title != null)
        {
            setBarTitle(title);
        }
        
        this.fileListAdapter = (PhotoListAdapter) getLastNonConfigurationInstance();

        if (this.fileListAdapter == null) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
        }

        initView();
    }

    @Override
    public void onDestroy() {
        ((PhotoListAdapter) this.fileListAdapter).clearLists(); 
        super.onDestroy();
    }
    
    @Override
    public void onBackPressed() {
        
       int numdevices = SystemState.getDevicePlusSyncCount();   
       
       if(numdevices == 1 && 
               (this.searchText.equals(FileUtils.photoSearch) ))  // Return to home screen only for the quick browse scenarios
       {
           NavigationTabActivity.returnToHomescreen(this);
       }
       else
           finish();
    }

    @Override
    protected synchronized void refresh(boolean flushCache) {
        if (!refreshing && !searching) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            this.fileListAdapter.disable();
            // fileListAdapter.clean(true); // Already done as a result of 'refresh' parameter in new ListAdaptor
            this.fileListAdapter.unregisterListener();
            PhotoListAdapter.ListAdapterDataListener listener = new PhotoListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(ListAdapter callingAdapter) {
                    PhotoSearchDirFileListActivity.this.refreshing = false;
                    PhotoSearchDirFileListActivity.this.searching = false;
                    
                    if (!isFinishing()) 
                    {
                        removeDialog(DIALOG_LOADING_ID);
                        
                        if(errDialog != null)
                            errDialog.dismiss();
                        
                        if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                        {
                            PhotoSearchDirFileListActivity.this.updateViewForNumItems(callingAdapter.getCount());
                            updateWindow("onDataRetrieved(): refresh()");
                        }
                        else
                        {
                            errDialog = createErrorDialog(callingAdapter.getErrorCode());
                            errDialog.show();
                        }
                    }
                }
            };
            
           if(this.fileListAdapter != null)
           {
               ((PhotoListAdapter) this.fileListAdapter).clearLists();
           }

           this.fileListAdapter = null;  // reset adapter for refresh
           this.fileListAdapter = getAdapter(listener);


            currentPosition = 0;
            listView.setAdapter(this.fileListAdapter);
            listView.setSelection(currentPosition);
            this.fileListAdapter.setReferencePosition(currentPosition);
            
        }
    }


    /**
     * @param listener
     */
    @Override
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener) {
        
        if(this.fileListAdapter == null)
        {
            if (searchText != null && searchText.length() > 0) {
                this.fileListAdapter = new PhotoListAdapter(getApplicationContext(),
                        R.layout.photo_list_item_layout,
                        this.containerLink,
                        this.bDeviceEncrypted,
                        searchText,
                        this.searchDirectory,
                        false,        // directoriesOnly
                        true,        // photosOnly
                        true,            // recurse
                        true,
                        listener,
                        PhotoListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                        null,
                        this.getRootDeviceId(),
                        this.getRootDeviceTitle(),
                        -1);
            } else {
                this.fileListAdapter = new PhotoListAdapter(getApplicationContext(),
                        R.layout.photo_list_item_layout,
                        this.containerLink,
                        this.bDeviceEncrypted,
                        "",
                        "",
                        false,        // directoriesOnly
                        true,         // photosOnly
                        false,         // recurse
                        true,
                        listener,
                        PhotoListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                        null,
                        this.getRootDeviceId(),
                        this.getRootDeviceTitle(),
                        -1);
            }
        }
        
        return this.fileListAdapter;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RequestCodes.REQUEST_START_ACTIVITY) 
        {
            if (resultCode == SecuredActivity.RESULT_CODE_NEED_REFRESH)
            {
                 // This means a file has been deleted. Treat it like a DirFileList delete,
                 // with a full refresh.
                 this.refresh(true);
             }
        }
    }
     
    @Override
    public OnItemClickListenerClass  getOnItemClickListenerClass()
    {
        return new OnItemClickListenerClass();
    }
    
   
    @Override
    protected void goToPhotoSlideShow(int photoPosition, CloudFile cloudFile)
    {
        // Load the preview activity
        Intent intent = new Intent(PhotoSearchDirFileListActivity.this, PhotoSearchSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);
        intent.putExtra("folderPosition", -1);
        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("searchDirectory", cloudFile.getPath());
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", PhotoSearchDirFileListActivity.this.canFilesBeDeleted);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }


    // override
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {   
        // Lame-ass workaround. For sub-menu picks, there is no 'AdapterContextMenuInfo' supplied, so I have to
        // save around the info that was passed in when the context menu was created.
        this.contextMenuInfo = (AdapterContextMenuInfo)menuInfo;

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        Object listItem = this.fileListAdapter.getItem(info.position);

        // If this item is not a 'title'
        if (!(listItem instanceof String))
        {
            
            int lastheader = ((PhotoListAdapter) this.fileListAdapter).getHeaderCountForPosition(info.position);
            if(((PhotoListAdapter) this.fileListAdapter).getPhotoCountinFolder(lastheader) <= 1)
            {
                super.onCreateContextMenu(menu, view, menuInfo);
            }
        }
    }

    @Override
    protected void viewPhoto(int position, MozyFile cloudFile)
    {
        showDialog(DIALOG_LOADING_ID);
        
        // Run a background task to do this as we need to go to the SQLite database and all accesses to the
        // database should be done in the background.
        CalculatePhotoPositionTask calculatePhotoPositionTask = new CalculatePhotoPositionTask(position, (CloudFile) cloudFile, -1);
        calculatePhotoPositionTask.execute();
    }
    
    
    protected class OnItemClickListenerClass extends 
    DirFileListActivity.OnItemClickListenerClass {
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
            updateAlarm();
            Object listItem = PhotoSearchDirFileListActivity.this.fileListAdapter.getItem(position);
            int lastheader = -1;
            int photoCount = -1;
            
            if(listItem != null)
            {
                lastheader = ((PhotoListAdapter) PhotoSearchDirFileListActivity.this.fileListAdapter).getHeaderCountForPosition(position);
                photoCount = ((PhotoListAdapter) PhotoSearchDirFileListActivity.this.fileListAdapter).getPhotoCountinFolder(lastheader);
            }

            // Only handle the click if this is not a 'title object.
            if (!(listItem instanceof String))
            {
                CloudFile cloudFile = (CloudFile)listItem;

                // This should only be null if the user clicked on the 'get more data' item at the end of the list.
                if (cloudFile == null)
                {
                    if (PhotoSearchDirFileListActivity.this.fileListAdapter.isNextItem(position)) {
                        showDialog(DIALOG_LOADING_ID);
                        refreshing = true;
                        PhotoSearchDirFileListActivity.this.fileListAdapter.increaseItems();
                    }
                    LogUtil.debug(this, "No file (null) for list item in position: " + Integer.toString(position));
                    return;
                }
                if (cloudFile instanceof Directory)
                {
                        /*
                         * When a directory is selected we will never want to
                         * keep the search string because all content should
                         * be listed.
                         *
                         * I.e when a search is performed and a result folder
                         * is clicked, we want to se all content in that folder.
                         */

                        Intent intent = new Intent(PhotoSearchDirFileListActivity.this, PhotoSearchGridActivity.class);
                        intent.putExtra("containerLink", cloudFile.getLink());
                        intent.putExtra("searchText", searchText);
                        intent.putExtra("searchDirectory", cloudFile.getPath());
                        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
                        intent.putExtra("title", cloudFile.getTitle());
                        intent.putExtra("canFilesBeDeleted", PhotoSearchDirFileListActivity.this.canFilesBeDeleted);
                        intent.putExtra("deviceId", PhotoSearchDirFileListActivity.this.rootDeviceId);
                        intent.putExtra("deviceTitle", PhotoSearchDirFileListActivity.this.rootDeviceTitle);
                        intent.putExtra("deviceType", PhotoSearchDirFileListActivity.this.bDeviceEncrypted);
                        intent.putExtra("platform", PhotoSearchDirFileListActivity.this.platform);
                        intent.putExtra("isPhotoDirGridEnabled", PhotoSearchDirFileListActivity.this.isPhotoDirGridEnabled);
                        intent.putExtra("folderPosition", position);
                    
                        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                }
                else if (cloudFile instanceof Photo)
                {
                    if(photoCount == 1)
                    {
                        viewPhotoFile(cloudFile, position);
                    }
                    else
                    {
                        // Switch current container navigation to gridview mode.
                        Intent intent = new Intent(PhotoSearchDirFileListActivity.this, PhotoSearchGridActivity.class);
                        
                        intent.putExtra("containerLink", containerLink);
                        intent.putExtra("searchText", searchText);
                        intent.putExtra("searchDirectory", cloudFile.getPath());
                        intent.putExtra("title", cloudFile.getPath());
                        intent.putExtra("recurse", false);
                        intent.putExtra("currentPosition", currentPosition);
                        intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
                        intent.putExtra("deviceId", rootDeviceId);
                        intent.putExtra("deviceTitle", rootDeviceTitle);
                        intent.putExtra("deviceType", bDeviceEncrypted);
                        intent.putExtra("platform", platform);
                        intent.putExtra("folderPosition", position);
                        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);         
                    }         
                }
                else  // another file type
                {
                    PhotoSearchDirFileListActivity.this.showMozyFile(cloudFile);
                }
            } // if (!(listItem instanceof String)
        }
    }

    // This task takes the current position in the list and calculates a new position based on that, that represents
    // the position in a list of only photos.
    private class CalculatePhotoPositionTask extends AsyncTask<Void, Void, Integer>
    {
        private int listPosition;
        CloudFile cloudFile;
        private int folderPosition;

        public CalculatePhotoPositionTask(int position, CloudFile cloudFile, int folderPostion)
        {
            this.listPosition = position;
            this.cloudFile = cloudFile;
            this.folderPosition = folderPostion;
        }

        @Override
        protected Integer doInBackground(Void... params)
        {
            return Integer.valueOf(((PhotoListAdapter) PhotoSearchDirFileListActivity.this.fileListAdapter).getPhotoPosition(this.listPosition, this.folderPosition));
        }


        @Override
        protected void onPostExecute(Integer photosIndex)
        {
            goToPhotoSlideShow(photosIndex.intValue(), this.cloudFile);
        }

    } // class CalculatePhotoPosition
}
