package com.mozy.mobile.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.RecentFilesListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class RecentDirFileListActivity extends DirFileListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void prepareActivity(Bundle savedInstanceState) {
        
        setContentView(R.layout.generic_list_layout);

        Bundle extras = getIntent().getExtras();
        
        this.recurse = false;
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            containerLink = extras.getString("containerLink");
            LogUtil.debug(this, "containerLink: " + containerLink);
            title = extras.getString("title");
            LogUtil.debug(this, "title: " + title);
            this.canFilesBeDeleted = extras.getBoolean("canFilesBeDeleted");
            LogUtil.debug(this, "canFilesBeDeleted: " + this.canFilesBeDeleted);
            this.rootDeviceId = extras.getString("deviceId");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceId);
            this.rootDeviceTitle = extras.getString("deviceTitle");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceTitle);
            this.recurse = extras.getBoolean("recurse");
            LogUtil.debug(this, "recurse: " + this.recurse);
            this.bDeviceEncrypted = extras.getBoolean("deviceType");
            LogUtil.debug(this, "deviceType: " + this.bDeviceEncrypted);
            platform = extras.getString("platform");
            LogUtil.debug(this, "platform: " + this.platform);
            this.currentPosition = extras.getInt("currentPosition");
            LogUtil.debug(this, "currentPosition: " + this.currentPosition);
            this.isPhotoDirGridEnabled = extras.getBoolean("isPhotoDirGridEnabled");
            LogUtil.debug(this, "photoGridEnabled: " + this.isPhotoDirGridEnabled);
        }

        if (savedInstanceState != null) {
            LogUtil.debug(this, "Loading saved instance state.");
            currentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY);
            LogUtil.debug(this, "position: " + currentPosition);
        }
        
        this.contextMenuFlags = setContextMenuFlags(this.canFilesBeDeleted);

        this.refreshing = false;

        // Remember we are in listview mode.
        sGridMode = false;
        if (title != null)
        {
            setBarTitle(title);
        }
        this.fileListAdapter = (RecentFilesListAdapter) getLastNonConfigurationInstance();

        if (this.fileListAdapter == null) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
        }
    
        initView();
    }


    
    @Override
    public OnItemClickListenerClass  getOnItemClickListenerClass()
    {
        return new OnItemClickListenerClass();
    }
    


    /**
     * @param flushCache
     * @param listener
     */
    @Override
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener) {
        
        if(this.fileListAdapter == null)
        {
                this.fileListAdapter = new RecentFilesListAdapter(getApplicationContext(),
                R.layout.list_item_layout,
                this.containerLink,
                this.bDeviceEncrypted,
                false,        // directoriesOnly
                false,         // photosOnly
                false,            // recurse
                true,    // refresh
                listener,
                RecentFilesListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                null,
                this.getRootDeviceId(),
                this.getRootDeviceTitle());
        }
        
        return this.fileListAdapter;
    }


    /**
     * 
     */
    @Override
    protected void gotoPhotoGrid() {
        Intent intent = new Intent(RecentDirFileListActivity.this, RecentDirPhotoGridActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("title", title);
        intent.putExtra("currentPosition", currentPosition);
        intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.REQUEST_START_ACTIVITY)
        {
            if (sGridMode)
            {
                // Navigation is now in gridview mode.
                if(containerLink.equals(sLastFolder))
                {
                    // Return to old location means we are navigating backward in
                    // gridview mode, so skip this listview-based intent (where we switched to grid).
                    finish();
                }
                else
                {
                    // Switch current container navigation to gridview mode.
                    Intent intent = new Intent(RecentDirFileListActivity.this, RecentDirPhotoGridActivity.class);
                    intent.putExtra("containerLink", containerLink);

                    intent.putExtra("title", title);
                    intent.putExtra("currentPosition", currentPosition);
                    intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
                    intent.putExtra("deviceId", rootDeviceId);
                    intent.putExtra("deviceTitle", rootDeviceTitle);
                    intent.putExtra("deviceType", bDeviceEncrypted);
                    intent.putExtra("platform", platform);
                    startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                    finish();
                }
            }
            else if (resultCode == SecuredActivity.RESULT_CODE_NEED_REFRESH)
            {
                // This means a file has been deleted. Treat it like a DirFileList delete,
                // with a full refresh.
                this.refresh(true);
            }
        }
    }

  
    @Override
    protected void updateViewForNumItems(int numItems)
    {
        // Show/hide the "no items found" message as appropriate.
        // And do the opposite for the footer divider line we are adding manually.
        boolean isListVisible = numItems > 0;
        
        TextView notification = (TextView) findViewById(R.id.notification);
        notification.setVisibility(isListVisible ? View.GONE : View.VISIBLE);
        
        if(isListVisible == false)
            notification.setText(R.string.no_file_updates);
        
        findViewById(R.id.footer_divider).setVisibility(isListVisible ? View.VISIBLE : View.GONE);
    }
   
    
    @Override
    public void onBackPressed() {
        
        int numdevices = SystemState.getDevicePlusSyncCount();
       if(numdevices == 1 )
       {
           NavigationTabActivity.returnToHomescreen(this);
       }
       else
           finish();
    }

    @Override
    protected void goToPhotoSlideShow(int photoPosition, CloudFile cloudFile)
    {
        // Load the preview activity
        Intent intent = new Intent(RecentDirFileListActivity.this, RecentPhotoSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", RecentDirFileListActivity.this.canFilesBeDeleted);
        intent.putExtra("recurse", false);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }


    // override
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        this.contextMenuInfo = (AdapterContextMenuInfo)menuInfo;

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        Object listItem = this.fileListAdapter.getItem(info.position);

        // If this item is not a 'title'
        if (!(listItem instanceof String))
        {
            super.onCreateContextMenu(menu, view, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {    
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuItem.getMenuInfo();

        if (info == null)
        {
            info = this.contextMenuInfo;
        }
        Object listItem = this.fileListAdapter.getItem(info.position);

        // title list item
        if (!(listItem instanceof String))
        {
            CloudFile cloudFile = (CloudFile)listItem;
            super.handleContextMenuItemSelection(menuItem, cloudFile, info.position);
        }
        return true;
    }

     
    protected class OnItemClickListenerClass extends DirFileListActivity.OnItemClickListenerClass {
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
            updateAlarm();
            Object listItem = RecentDirFileListActivity.this.fileListAdapter.getItem(position);

           
            // Only handle the click if this is not a 'title object.
            if (!(listItem instanceof String))
            {
                CloudFile cloudFile = (CloudFile)listItem;

             // This should only be null if the user clicked on the 'get more data' item at the end of the list.
                if (cloudFile == null)
                {
                    if (RecentDirFileListActivity.this.fileListAdapter.isNextItem(position)) {
                        showDialog(DIALOG_LOADING_ID);
                        refreshing = true;
                        RecentDirFileListActivity.this.fileListAdapter.increaseItems();
                    }
                    LogUtil.debug(this, "No file (null) for list item in position: " + Integer.toString(position));
                    return;
                }
                if (cloudFile instanceof Photo)
                {
                    viewPhotoFile(cloudFile, position);
                        
                }
                else  // another file type
                {
                    RecentDirFileListActivity.this.showMozyFile(cloudFile);
                }
            } // if (!(listItem instanceof String)
        }
    }
}
