package com.mozy.mobile.android.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.DownloadListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class DownloadDirFileListActivity extends ContextMenuActivity  {
    private static final int MENU_PHOTO_GRID = MENU_LAST + 1;

    private String title;
    private boolean refreshing;

    private ListView listView;
    private DownloadListAdapter fileListAdapter;
    
    private AdapterContextMenuInfo contextMenuInfo = null;
    
    private String rootDeviceId = null;
    private String rootDeviceTitle = null;
    private boolean bDeviceEncrypted = false;
    private String platform = "";
   

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list_layout);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) 
        {
            LogUtil.debug(this, "Loading extras from intent:");
            title = extras.getString("title");
            LogUtil.debug(this, "title: " + title);
            this.rootDeviceId = extras.getString("deviceId");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceId);
            this.rootDeviceTitle = extras.getString("deviceTitle");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceTitle);
            this.bDeviceEncrypted = extras.getBoolean("deviceType");
            LogUtil.debug(this, "deviceType: " + this.bDeviceEncrypted);
            platform = extras.getString("platform");
            LogUtil.debug(this, "platform: " + this.platform);
        }

        if (savedInstanceState != null) {
            LogUtil.debug(this, "Loading saved instance state.");
        }
        
 
        this.contextMenuFlags = setContextMenuFlags(true);
 
        

        this.refreshing = false;

        // Remember we are in listview mode.
        sGridMode = false;

        if (title != null)
        {
            setBarTitle(title);
        }
        fileListAdapter = (DownloadListAdapter) getLastNonConfigurationInstance();

        if (fileListAdapter == null) 
        {
            showDialog(DIALOG_LOADING_ID);
            refreshing = true;
        }

        initView();
    }
    
    

    @Override
    public void onResume() {
        super.onResume();
        
        boolean showDialog = false;

        if (!refreshing)
        {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            showDialog = true;
        }

        if(!fileListAdapter.enable() && showDialog)
        {
            refreshing = false;
            removeDialog(DIALOG_LOADING_ID);
        }

        updateWindow("onResume()");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        fileListAdapter.disable();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    
    
    public ListAdapterListenerClass  getListAdapterListenerClass()
    {
        return new ListAdapterListenerClass();
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
    public Object onRetainNonConfigurationInstance() {
        return fileListAdapter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        
        if (id == DIALOG_LOADING_ID)
        {
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(
                    getResources().getString(R.string.progress_bar_loading));

            loadingDialog.setIndeterminate(true);
            loadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        DownloadDirFileListActivity.this.finish();
                    }
                    return true;
                }
            });
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FILES, 0, getResources().getString(R.string.menu_files)).setIcon(R.drawable.allfiles);
        menu.add(0, MENU_HOME, 1, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        menu.add(0, MENU_PHOTO_GRID, 2, getResources().getString(R.string.photos_only)).setIcon(R.drawable.gallery_view);
        menu.add(0, MENU_HELP, 3, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 4, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);

        return true;
    }

    private synchronized void refresh(boolean flushCache) {
        if (!refreshing ) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            fileListAdapter.disable();

            // fileListAdapter.clean(true); // Already done as a result of 'refresh' parameter in new ListAdaptor
            fileListAdapter.unregisterListener();
            DownloadListAdapter.ListAdapterDataListener listener = new DownloadListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(DownloadListAdapter callingAdapter) {
                    DownloadDirFileListActivity.this.refreshing = false;
                    if (!isFinishing()) 
                    {
                        removeDialog(DIALOG_LOADING_ID);
                        DownloadDirFileListActivity.this.updateViewForNumItems(callingAdapter.getCount());
                        updateWindow("onDataRetrieved(): refresh()");  
                    }
                }
            };
          
            fileListAdapter = new DownloadListAdapter(getApplicationContext(),
                    R.layout.list_item_layout,
                    this.bDeviceEncrypted,
                    false,        // directoriesOnly
                    false,         // photosOnly
                    DownloadListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                    listener,
                    null,
                    this.getRootDeviceId(),
                    this.getRootDeviceTitle());

            listView.setAdapter(fileListAdapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_PHOTO_GRID:
            Intent intent = new Intent(DownloadDirFileListActivity.this, DownloadDirPhotoGridActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("deviceId", rootDeviceId);
            intent.putExtra("deviceTitle", rootDeviceTitle);
            intent.putExtra("deviceType", bDeviceEncrypted);
            intent.putExtra("platform", platform);
            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

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

    public String getRootDeviceId()
    {
        return this.rootDeviceId;
    }
    
    public String getRootDeviceTitle()
    {
        return this.rootDeviceTitle;
    }
    
    public boolean isDeviceEncrypted()
    {
        return this.bDeviceEncrypted;
    }
    
    public String getPlatform()
    {
        return this.platform;
    }
    
    
    private void initView() {
        listView = (ListView) findViewById(R.id.generic_list);
    
        DownloadListAdapter.ListAdapterDataListener listener = getListAdapterListenerClass();
        
        if (fileListAdapter == null) {
            fileListAdapter = new DownloadListAdapter(getApplicationContext(),
                    R.layout.list_item_layout,
                    this.bDeviceEncrypted,
                    false,            // directoriesOnly
                    false,            // photosOnly
                    DownloadListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                    listener,
                    null,
                    this.getRootDeviceId(),
                    this.getRootDeviceTitle());
        }
        this.updateViewForNumItems(fileListAdapter.getCount());

        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                updateAlarm();
                fileListAdapter.notifyScrollStateChanged(scrollState);

                switch (scrollState) {
                case SCROLL_STATE_FLING:
                    fileListAdapter.disable();
                    break;
                case SCROLL_STATE_IDLE:
                    fileListAdapter.enable();
                    updateWindow("SCROLL_STATE_IDLE");
                    break;
                case SCROLL_STATE_TOUCH_SCROLL:
                    break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

        listView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                updateAlarm();
                Object listItem = fileListAdapter.getItem(position);

                // Only handle the click if this is not a 'title object.
                if ((listItem instanceof LocalFile))
                {
                    LocalFile localFile = (LocalFile)listItem;
                     
                    // Note for existing downloaded files in database we would have no mapping to cloudfile
                    DownloadDirFileListActivity.this.showMozyFile(localFile);
                   
                }
            }
        });

        listView.setAdapter(fileListAdapter);

        this.registerForContextMenu(listView);
    }
    
  
    
    /**
     * @param returnValue
     * @return
     */
    @Override
    protected Dialog overWriteFileDialog(CloudFile cloudfile, LocalFile localfile, long deviceTimeStamp, long cloudTimeStamp, String action) 
    {
        Dialog returnValue = null;
        
        String titleString = this.getString(R.string.update_local_file_title);
        
        if(deviceTimeStamp < cloudTimeStamp)
        {
            String promptString = this.getString(R.string.mozy_ver_recent_body);
        
            returnValue =  DownloadDirFileListActivity.this.checkForLatestDialog(cloudfile, localfile,titleString, promptString, action);
        }
      
        return returnValue;
   }
    
    
    
    
    private void updateViewForNumItems(int numItems)
    {
        // Show/hide the "no items found" message as appropriate.
        // And do the opposite for the footer divider line we are adding manually.
        boolean isListVisible = numItems > 0;
        
        TextView notification = (TextView) findViewById(R.id.notification);
        notification.setVisibility(isListVisible ? View.GONE : View.VISIBLE);
        
        if(isListVisible == false)
            notification.setText(R.string.no_downloads);
            
        findViewById(R.id.footer_divider).setVisibility(isListVisible ? View.VISIBLE : View.GONE);
    }

    
    // override
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        // Lame-ass workaround. For sub-menu picks, there is no 'AdapterContextMenuInfo' supplied, so I have to
        // save around the info that was passed in when the context menu was created.
        this.contextMenuInfo = (AdapterContextMenuInfo)menuInfo;

        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        Object listItem = fileListAdapter.getItem(info.position);

        // If this item is not a 'title'
        if (!(listItem instanceof String))
        {
            super.onCreateContextMenu(menu, view, menuInfo);

            LocalFile f = (LocalFile)listItem;

            super.buildContextMenu(menu, view, menuInfo, f, this.bDeviceEncrypted);
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
        Object listItem = fileListAdapter.getItem(info.position);

        // title list item
        if (!(listItem instanceof String))
        {
            LocalFile f = (LocalFile)listItem;
            super.handleContextMenuItemSelection(menuItem, f, info.position);
        }
        return true;
    }

    @Override
    protected void removeItem(int position, final MozyFile localFile)
    {
        boolean deleted = ((LocalFile) localFile).delete();
            
        if(deleted)
        {
            if(SystemState.mozyFileDB != null)
                SystemState.mozyFileDB.removeFileInDB(this.getRootDeviceId(), localFile.getName());
            refreshing = false;
            this.refresh(true);
        }
        else
        {
            Dialog errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
    }

    @Override
    public void onContextMenuClosed(Menu menu)
    {
        super.onContextMenuClosed(menu);
        this.contextMenuInfo = null;
    }


    private void updateWindow(String comment)
    {
        LogUtil.debug("DownloadDirFileListActivity", "updateWindow: " + comment);
        if(fileListAdapter != null && listView != null)
        {
            fileListAdapter.setCurrentView(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
        }
    }
    
    @Override
    protected void viewPhoto(int position, MozyFile localFile)
    {
        showDialog(DIALOG_LOADING_ID);
        DownloadDirFileListActivity.this.showMozyFile(localFile);
    }
    
    
    protected class ListAdapterListenerClass implements
    DownloadListAdapter.ListAdapterDataListener {
        @Override
        public void onDataRetrieved(DownloadListAdapter callingAdapter) {
            DownloadDirFileListActivity.this.refreshing = false;
            if (!isFinishing()) 
            {
                removeDialog(DIALOG_LOADING_ID);
                DownloadDirFileListActivity.this.updateViewForNumItems(callingAdapter.getCount());
                updateWindow("onDataRetrieved(): (initView)"); 
            }

        }
    }
}
