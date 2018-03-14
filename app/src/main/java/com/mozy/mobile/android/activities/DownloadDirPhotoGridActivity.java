package com.mozy.mobile.android.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.DownloadListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;


public class DownloadDirPhotoGridActivity extends ContextMenuActivity {

    // The margin is derived from grid_directory_layout and grid_image_layout and grid_border.xml.
    // It is the amount of padding added around an item, both for border and inter-item spacing.
    public final static int GRID_MARGIN = 3;    // in DIP
    public final static int NUM_COLUMNS = 3;

    private static final int MENU_DIR_FILE_LIST = MENU_LAST + 1;

    private String title;
    private String rootDeviceId;
    private String rootDeviceTitle;
    private boolean bDeviceEncrypted;
    private String platform = "";
    private int currentPosition;
    private boolean refreshing;
    private DownloadListAdapter photoAdapter;
    private GridView gridView;
    

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(BarActivity.NO_BAR, R.layout.photo_layout);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            title = extras.getString("title");
            LogUtil.debug(this, "title: " + title);
            rootDeviceId = extras.getString("deviceId");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceId);
            rootDeviceTitle = extras.getString("deviceTitle");
            LogUtil.debug(this, "deviceTitle: " + this.rootDeviceTitle);
            bDeviceEncrypted = extras.getBoolean("deviceType");
            LogUtil.debug(this, "deviceType: " + this.bDeviceEncrypted);
            platform = extras.getString("platform");
            LogUtil.debug(this, "platform: " + this.platform);
        } else {
            rootDeviceId = null;
            rootDeviceTitle = null;
        }
        this.refreshing = false;

        // Remember we are in gridview mode.
        sGridMode = true;

        if (title != null)
        {
            setBarTitle(title);
        }
        
 
        this.contextMenuFlags = setContextMenuFlags(true);

        photoAdapter = (DownloadListAdapter) getLastNonConfigurationInstance();

        if (photoAdapter == null)
        {
            showDialog(DIALOG_LOADING_ID);
            refreshing = true;
        }

        initView();
    }

    private void initView()
    {
        
        DownloadListAdapter.ListAdapterDataListener listener = getListAdapterListenerClass();
        
        if (photoAdapter == null)
        {
            photoAdapter = new DownloadListAdapter(getApplicationContext(),
                    0,
                    this.bDeviceEncrypted,
                    true,        // directoriesOnly
                    true,         // photosOnly
                    DownloadListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID,
                    listener,
                    null,
                    this.getRootDeviceId(),
                    this.getRootDeviceTitle());
        }
        else
        {
            if (photoAdapter.getCount() == 0)
            {
                TextView text = (TextView) findViewById(R.id.notification);
                text.setVisibility(View.VISIBLE);
            }
        }


        this.gridView = (GridView) findViewById(R.id.photo_gridview);
        gridView.setNumColumns(photoAdapter.getNumColumnsForGridView());
        gridView.setVerticalSpacing(0);
        gridView.setHorizontalSpacing(0);
        gridView.setPadding(GRID_MARGIN, GRID_MARGIN, GRID_MARGIN, GRID_MARGIN);

        gridView.setOnScrollListener(new OnScrollListener()
        {
            public void onScrollStateChanged(AbsListView view, int scrollState)
            {
                updateAlarm();

                switch (scrollState)
                {
                case SCROLL_STATE_FLING:
                    photoAdapter.disable();
                    break;
                case SCROLL_STATE_IDLE:
                    photoAdapter.enable();
                    updateWindow("SCROLL_STATE_IDLE");
                    break;
                case SCROLL_STATE_TOUCH_SCROLL:
                    break;
                }
            }

            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount)
            {
                setReferenceIndex();
            }
        });

        gridView.setOnItemClickListener(new OnItemClickListener()
        {  
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                updateAlarm();
                Object listItem = photoAdapter.getItem(position);

                // Only handle the click if this is not a 'title object.
                if ((listItem instanceof LocalFile))
                {
                    LocalFile localFile = (LocalFile)listItem;
                     
                    // Note for existing downloaded files in database we would have no mapping to cloudfile
                    
                    DownloadDirPhotoGridActivity.this.showMozyFile(localFile);
                }
            }
       });



        gridView.setOnItemSelectedListener(new OnItemSelectedListener()
        {

            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
            {
                setReferenceIndex();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0)
            {

            }
        });


        gridView.setAdapter(photoAdapter);
        gridView.setSelection(currentPosition);
 

        this.registerForContextMenu(gridView);
    }
   
    
    public ListAdapterListenerClass  getListAdapterListenerClass()
    {
        return new ListAdapterListenerClass();
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

            returnValue =  DownloadDirPhotoGridActivity.this.checkForLatestDialog(cloudfile, localfile, titleString, promptString, action);
        }

        this.fileToDownload = null;
      
        return returnValue;
   }
    
   

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        if (this.gridView != null && photoAdapter != null)
        {
            gridView.setNumColumns(photoAdapter.getNumColumnsForGridView());
        }
    }

    private void setReferenceIndex()
    {
        GridView gridView = (GridView) findViewById(R.id.photo_gridview);
        currentPosition = gridView.getFirstVisiblePosition();
        int lastPosition = gridView.getLastVisiblePosition();
        int centerPosition = (currentPosition + lastPosition) / 2;
        centerPosition -= centerPosition % 3 - 1;
    }

    @Override
    public void onResume() {
        boolean showDialog = false;
        if (!refreshing)
        {
            showDialog(DIALOG_LOADING_ID);
            refreshing = true;
            showDialog = true;
        }

        /*
         * If we did not call prepareList() but opened a dialog,
         * remove that dialog.
         */
        if(!photoAdapter.enable() && showDialog)
        {
            refreshing = false;
            removeDialog(DIALOG_LOADING_ID);
        }
        
        photoAdapter.enable();
        setReferenceIndex();
        updateWindow("onResume()");
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        photoAdapter.disable();
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return photoAdapter;
    }

    @Override
    protected Dialog onCreateDialog(int id) 
    {
        switch (id) {
        case DIALOG_LOADING_ID:
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(getResources().getString( R.string.progress_bar_loading));
            loadingDialog.setIndeterminate(true);
            loadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        DownloadDirPhotoGridActivity.this.finish();
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
        menu.add(0, MENU_DIR_FILE_LIST, 2, getResources().getString(R.string.menu_files_view)).setIcon(R.drawable.all_files);
        menu.add(0, MENU_HELP, 3, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 4, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DIR_FILE_LIST:
            Intent intent = new Intent(DownloadDirPhotoGridActivity.this, DownloadDirFileListActivity.class);
            intent.putExtra("title", title);
            intent.putExtra("deviceId", rootDeviceId);
            intent.putExtra("deviceTitle", rootDeviceTitle);
            intent.putExtra("deviceType", bDeviceEncrypted);
            intent.putExtra("platform", platform);
            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);

            // Do NOT finish() this activity. Want to be able to go back.
            return true;
         default:
             return super.onOptionsItemSelected(item);
        }
    }
    
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        Object listItem = photoAdapter.getItem(info.position);
        if (listItem instanceof String)
        {
            LogUtil.error(this, "String object received in DownloadDirPhotoGrid!!");
        }
        else
        {
            super.onCreateContextMenu(menu, view, menuInfo);

           LocalFile localFile = (LocalFile)listItem;
            super.buildContextMenu(menu, view, menuInfo, localFile, this.bDeviceEncrypted);
        }
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuItem.getMenuInfo();
        LocalFile localFile = (LocalFile)photoAdapter.getItem(info.position);
        super.handleContextMenuItemSelection(menuItem, localFile,info.position);

        return true;
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

    private synchronized void refresh(boolean flushCache) {
        if (!refreshing) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            
            photoAdapter.disable();
            // photoAdapter.clean(true); // Already done as a result of 'refresh' parameter in new ListAdaptor
            photoAdapter.unregisterListener();
            DownloadListAdapter.ListAdapterDataListener listener = new DownloadListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(DownloadListAdapter callingAdapter) {
                    DownloadDirPhotoGridActivity.this.refreshing = false;
                    if (!isFinishing()) 
                    {
                        removeDialog(DIALOG_LOADING_ID);
                        
                        if (callingAdapter.getCount() == 0)
                        {
                            TextView text = (TextView) findViewById(R.id.notification);
                            text.setVisibility(View.VISIBLE);
                        } 
                        else
                        {
                            TextView text = (TextView) findViewById(R.id.notification);
                            text.setVisibility(View.GONE);
                        }
    
                        updateWindow("onDataRetrieved(): refresh()");
                    }
                }
            };
            photoAdapter = new DownloadListAdapter(getApplicationContext(),
                    0,
                    this.bDeviceEncrypted,
                    true,        // directoriesOnly
                    true,         // photosOnly
                    DownloadListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID,
                    listener,
                    null,
                    this.getRootDeviceId(),
                    this.getRootDeviceTitle());

            GridView gridView = (GridView) findViewById(R.id.photo_gridview);
            currentPosition = 0;
            gridView.setAdapter(photoAdapter);
            gridView.setSelection(currentPosition);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.REQUEST_START_ACTIVITY)
        {
            if (!sGridMode)
            {
                // Switch current container navigation to listview mode.
                Intent intent = new Intent(DownloadDirPhotoGridActivity.this, DownloadDirFileListActivity.class);
                intent.putExtra("title", title);
                intent.putExtra("deviceId", rootDeviceId);
                intent.putExtra("deviceTitle", rootDeviceTitle);
                intent.putExtra("deviceType", bDeviceEncrypted);
                intent.putExtra("platform", platform);
                startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                finish();
            }
            else if (resultCode == SecuredActivity.RESULT_CODE_NEED_REFRESH)
            {
                // This indicates that the data in the cache has changed so we need to do a repaint.
                //setResult(HomescreenActivity.RESULT_CODE_NEED_REFRESH);

                // This means a file has been deleted. Treat it like a DirPhotoGrid delete,
                // with a full refresh.
                this.refresh(true);
            }
        }
    }
    

    @Override
    protected int handleError(int errorCode, int errorType) {
        return ACTION_NONE;
    }

    @Override
    protected Dialog createErrorDialog(int errorCode) {
        DownloadDirPhotoGridActivity.this.refreshing = false;
        return null;
    }

   
    private void updateWindow(String comment)
    {
        LogUtil.debug("DirPhotoGridActivity", "updateWindow: " + comment);
        if(photoAdapter != null && gridView != null)
        {
            photoAdapter.setCurrentView(gridView.getFirstVisiblePosition(), gridView.getLastVisiblePosition());
        }
    }
    
    
    @Override
    protected void viewPhoto(int position, MozyFile localFile)
    {
        showDialog(DIALOG_LOADING_ID);
        DownloadDirPhotoGridActivity.this.showMozyFile(localFile);
    }

    // Called by context menu code.
    @Override
    protected void removeItem(int position, final MozyFile localFile)
    {
        boolean deleted = ((LocalFile) localFile).delete();
        
        
        if(deleted)
        {
            refreshing = false;
            
            if(SystemState.mozyFileDB != null)
                SystemState.mozyFileDB.removeFileInDB(this.getRootDeviceId(), localFile.getName());
            this.refresh(true);
        }
        else
        {
            Dialog errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
    }
    
    protected class ListAdapterListenerClass implements
        DownloadListAdapter.ListAdapterDataListener {
        @Override
        public void onDataRetrieved(DownloadListAdapter callingAdapter) {
            DownloadDirPhotoGridActivity.this.refreshing = false;
            
            if (!isFinishing()) 
            {
                removeDialog(DIALOG_LOADING_ID);
                
                if (callingAdapter.getCount() == 0)
                {
                    TextView text = (TextView) findViewById(R.id.notification);
                    text.setVisibility(View.VISIBLE);
                }
                
                updateWindow("onDataRetrieved(): initView()");
            }
        }
    }
}
