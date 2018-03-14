package com.mozy.mobile.android.activities;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.views.DownloadMoreView;

public class DirPhotoGridActivity extends ContextMenuActivity {

    protected static final String CURRENT_POSITION_KEY = "CURRENT_POSITION_KEY";
    protected static final String SEARCH_TEXT_KEY = "SEARCH_TEXT_KEY";
    protected static final String RECURSE_KEY = "RECURSE_KEY";
    
    // The margin is derived from grid_directory_layout and grid_image_layout and grid_border.xml.
    // It is the amount of padding added around an item, both for border and inter-item spacing.
    public final static int GRID_MARGIN = 3;    // in DIP
    public final static int NUM_COLUMNS = 3;

    protected static final int MENU_REFRESH = MENU_LAST + 1;
    protected static final int MENU_DIR_FILE_LIST = MENU_REFRESH + 2;


    protected String containerLink;
    protected String title;
    protected String searchText;
    protected String rootDeviceId;
    protected String rootDeviceTitle;
    protected boolean bDeviceEncrypted;
    protected String platform = "";
    protected int currentPosition;
    protected boolean refreshing;
    protected boolean canFilesBeDeleted;
    protected boolean recurse;
    protected ListAdapter photoAdapter;
    protected GridView gridView;
//    protected boolean isHiddenFilesModeChanged = false;
    
    protected Dialog errDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(BarActivity.SIMPLE_BAR, R.layout.photo_layout);

        DownloadMoreView downloadMoreView = (DownloadMoreView) findViewById(R.id.gridlayout_view);
        downloadMoreView.setButtonOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                DirPhotoGridActivity.this.refreshing = true;
                showDialog(DIALOG_LOADING_ID);
                photoAdapter.increaseItems();
            }
        });
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            containerLink = extras.getString("containerLink");
            LogUtil.debug(this, "containerLink: " + containerLink);
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
            canFilesBeDeleted = extras.getBoolean("canFilesBeDeleted");
            this.recurse = extras.getBoolean("recurse");
            this.searchText = extras.getString("searchText");
        } else {
            canFilesBeDeleted = false;
            rootDeviceId = null;
            rootDeviceTitle = null;
        }
        this.refreshing = false;

        // Remember we are in gridview mode.
        sGridMode = true;

        if (savedInstanceState != null) {
            LogUtil.debug(this, "Loading saved instance state.");
            currentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY);
            LogUtil.debug(this, "position: " + currentPosition);
            searchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
            LogUtil.debug(this, "searchText: " + searchText);
            this.recurse = savedInstanceState.getBoolean(RECURSE_KEY);
        }
        
        this.contextMenuFlags = setContextMenuFlags(this.canFilesBeDeleted);


        setTitleForPhotoGrid();

        photoAdapter = (ListAdapter) getLastNonConfigurationInstance();

        if (photoAdapter == null)
        {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
        }

        initView();
    }


    /**
     * 
     */
    public void setTitleForPhotoGrid() {
        if (title != null)
        {
            setBarTitle(title);
        }
    }

    
    protected void initView()
    {
        ListAdapter.ListAdapterDataListener listener = getListAdapterListenerClass();
        if (photoAdapter == null)
        {
            photoAdapter = getAdapter(listener, false);
        }
        else
        {
            if (photoAdapter.getCount() == 0)
            {
                TextView text = (TextView) findViewById(R.id.notification);
                text.setVisibility(View.VISIBLE);
            }
        }

        photoAdapter.registerListener(listener);
        DownloadMoreView downloadMoreView = (DownloadMoreView) findViewById(R.id.gridlayout_view);
        this.gridView = (GridView)downloadMoreView.getGridView();
        gridView.setNumColumns(photoAdapter.getNumColumnsForGridView());
        gridView.setVerticalSpacing(0);
        gridView.setHorizontalSpacing(0);
        gridView.setPadding(GRID_MARGIN, GRID_MARGIN, GRID_MARGIN, GRID_MARGIN);

        gridView.setOnTouchListener(getOnTouchListenerClass());

        gridView.setOnScrollListener(getOnScrollListenerClass());

        gridView.setOnItemClickListener(getOnItemClickListenerClass());

        gridView.setOnItemSelectedListener(getOnItemSelectedListenerClass());

        // This is needs to exist in order for the context menu to work. I don't know why.
        gridView.setOnTouchListener(getOnTouchListenerClass());

        gridView.setAdapter(photoAdapter);
        gridView.setSelection(currentPosition);
        photoAdapter.setReferencePosition(currentPosition);

        this.registerForContextMenu(gridView);
    }

    /**
     * @param listener
     */
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener, boolean flushCache) {
        ListAdapter photoAdapter = new ListAdapter(getApplicationContext(),
                R.id.grid_directory_layout,
                this.containerLink,
                this.bDeviceEncrypted,
                this.searchText,
                "",
                true,        // directoriesOnly
                true,         // photosOnly
                recurse,
                flushCache,        // refresh
                listener,
                ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID,
                null,
                this.getRootDeviceId(),
                this.getRootDeviceTitle());
        
        return photoAdapter;
    }
    
    
    public ListAdapterListenerClass  getListAdapterListenerClass()
    {
        return new ListAdapterListenerClass();
    }
    
    public OnScrollListenerClass  getOnScrollListenerClass()
    {
        return new OnScrollListenerClass();
    }
    
    
    public OnItemClickListenerClass  getOnItemClickListenerClass()
    {
        return new OnItemClickListenerClass();
    }
    
    
    public OnItemSelectedListenerClass  getOnItemSelectedListenerClass()
    {
        return new OnItemSelectedListenerClass();
    }
    
    public OnTouchListenerClass  getOnTouchListenerClass()
    {
        return new OnTouchListenerClass();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        
        int lastPosition = gridView.getLastVisiblePosition();
        int numCols = photoAdapter.getNumColumnsForGridView();
        if(currentPosition != 0)  // We need revise the position
        {
            currentPosition = ((int) (currentPosition / numCols) + 1) * numCols;
            lastPosition = ((int) (lastPosition / numCols) + 1) * numCols + numCols - 1;
        }
        gridView.setSelection(currentPosition);
        if (this.gridView != null && photoAdapter != null)
        {
            gridView.setNumColumns(numCols);
        }
       
        updateWindow("onConfigurationChanged()", currentPosition, lastPosition);
        
    }

    protected void goToPhotoSlideShow(int photoPosition, CloudFile cloudFile)
    {
        // Load the preview activity
        Intent intent = new Intent(DirPhotoGridActivity.this, PhotoSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);

        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.trim().length() > 0);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", DirPhotoGridActivity.this.canFilesBeDeleted);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    protected void setReferenceIndex()
    {
        DownloadMoreView downloadMoreView = (DownloadMoreView)findViewById(R.id.gridlayout_view);
        GridView gridView = (GridView) downloadMoreView.getGridView();
        currentPosition = gridView.getFirstVisiblePosition();
        int lastPosition = gridView.getLastVisiblePosition();
        int centerPosition = (currentPosition + lastPosition) / 2;
        centerPosition -= centerPosition % 3 - 1;

        if (centerPosition < photoAdapter.getCount())
        {
            photoAdapter.setReferencePosition(centerPosition);
        }
        else
        {
            photoAdapter.setReferencePosition(photoAdapter.getCount() - 1);
        }
    }

    @Override
    public void onResume() {
        boolean showDialog = false;
        
//        if(this.isHiddenFilesModeChanged)
//        {
//            this.isHiddenFilesModeChanged = false;
//            refresh(true);
//        }
        
        if (!refreshing)
        {
            refreshing = true;

            showDialog(DIALOG_LOADING_ID);
            showDialog = true;
        }

        /*
         * If we did not call prepareList() but opened a dialog,
         * remove that dialog.
         */
        if(!photoAdapter.enable() && showDialog)
        {
            removeDialog(DIALOG_LOADING_ID);
            refreshing = false;
        }
        
        
        setReferenceIndex();
        updateWindow("onResume()");
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        savedInstanceState.putString(SEARCH_TEXT_KEY, searchText);
        savedInstanceState.putInt(CURRENT_POSITION_KEY, currentPosition);
        savedInstanceState.putBoolean(RECURSE_KEY, recurse);
    }

    @Override
    public void onPause() {
        photoAdapter.disable();
        photoAdapter.clean(false);
        ContextMenuActivity.sLastFolder = containerLink;
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        photoAdapter.clean(false);
        photoAdapter.unregisterListener();
        return photoAdapter;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
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
                        DirPhotoGridActivity.this.finish();
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
        menu.add(0, MENU_REFRESH, 3, getResources().getString(R.string.menu_refresh)).setIcon(R.drawable.refresh);
        menu.add(0, MENU_HELP, 4, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 5, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_REFRESH:
            this.refresh(true);
            return true;
        case MENU_DIR_FILE_LIST:
            goToFileList();

            // Do NOT finish() this activity. Want to be able to go back.
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    protected void goToFileList() {
        Intent intent = new Intent(DirPhotoGridActivity.this, DirFileListActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("title", title);
        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
        intent.putExtra("isPhotoDirGridEnabled", true); // this how we got to the grid view so is enabled
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);

    }

    @Override
    public String getRootDeviceId()
    {
        return this.rootDeviceId;
    }
    
    @Override
    public String getRootDeviceTitle()
    {
        return this.rootDeviceTitle;
    }
    
    @Override
    public boolean isDeviceEncrypted()
    {
        return this.bDeviceEncrypted;
    }
    
    
    @Override
    public String getPlatform()
    {
        return this.platform;
    }

    protected synchronized void refresh(boolean flushCache) {
        if (!refreshing) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            photoAdapter.disable();
            // photoAdapter.clean(true); // Already done as a result of 'refresh' parameter in new ListAdaptor
            photoAdapter.unregisterListener();
            ListAdapter.ListAdapterDataListener listener = new ListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(ListAdapter callingAdapter) {
                    DirPhotoGridActivity.this.refreshing = false;
                    
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
                        
                        if(errDialog != null)
                            errDialog.dismiss();
                        
                        if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                        {
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
            photoAdapter = getAdapter(listener, flushCache);

            DownloadMoreView downloadMoreView = (DownloadMoreView) findViewById(R.id.gridlayout_view);
            GridView gridView = (GridView) downloadMoreView.getGridView();
            currentPosition = 0;
            gridView.setAdapter(photoAdapter);
            gridView.setSelection(currentPosition);
            photoAdapter.setReferencePosition(currentPosition);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.REQUEST_START_ACTIVITY)
        {
            if (!sGridMode)
            {
                // Navigation is now in listview mode.
                if (containerLink.equals(sLastFolder))
                {
                    // Return to old location means we are navigating backward in
                    // listview mode, so skip this gridview-based view (where we switched to list).
                    finish();
                }
                else
                {  
                    goToFileList();
                    finish(); 
                }
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
        switch (errorCode)
        {
        case ErrorCodes.ERROR_HTTP_IO:
        case ErrorCodes.ERROR_HTTP_NOT_FOUND:
        case ErrorCodes.ERROR_HTTP_PARSER:
        case ErrorCodes.ERROR_HTTP_SERVER:
        case ErrorCodes.ERROR_HTTP_BAD_GATEWAY:
        case ErrorCodes.ERROR_HTTP_GATEWAY_TIMEOUT:
        case ErrorCodes.ERROR_HTTP_SERVICE_UNAVAILABLE:
        case ErrorCodes.ERROR_HTTP_UNAUTHORIZED:
//        case ErrorCodes.ERROR_ENCRYPTED_FILE:
        case ServerAPI.RESULT_CONNECTION_FAILED:
        case ServerAPI.RESULT_UNAUTHORIZED:
        case ServerAPI.RESULT_FORBIDDEN:
        case ServerAPI.RESULT_UNKNOWN_PARSER:
        case ServerAPI.RESULT_UNKNOWN_ERROR:
        case ServerAPI.RESULT_ENCRYPTED_FILE:
        case ErrorCodes.ERROR_HTTP_UNKNOWN:
            return ACTION_DIALOG_ERROR;
        }

        return ACTION_NONE;
    }

    @Override
    protected Dialog createErrorDialog(int errorCode) {
        removeDialog(DIALOG_LOADING_ID);
        return super.createErrorDialog(errorCode);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuInfo;
        Object listItem = photoAdapter.getItem(info.position);
        if (listItem instanceof String)
        {
            LogUtil.error(this, "String object received in DirPhotoGrid!!");
        }
        else
        {
            super.onCreateContextMenu(menu, view, menuInfo);

            CloudFile cloudFile = (CloudFile)listItem;
            super.buildContextMenu(menu, view, menuInfo, cloudFile, this.bDeviceEncrypted);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuItem.getMenuInfo();
        CloudFile cloudFile = (CloudFile)photoAdapter.getItem(info.position);
        super.handleContextMenuItemSelection(menuItem, cloudFile,info.position);

        return true;
    }

    @Override
    protected void viewPhoto(int position, MozyFile cloudFile)
    {
        showDialog(DIALOG_LOADING_ID);

        // Run a background task to do this as we need to go to the SQLite database and all accesses to the
        // database should be done in the background.
        CalculatePhotoPositionTask calculatePhotoPositionTask = new CalculatePhotoPositionTask(position, (CloudFile)cloudFile);
        calculatePhotoPositionTask.execute();
    }

    // Called by context menu code.
    @Override
    protected void removeItem(int position, final MozyFile cloudFile)
    {
        // Indicate to any calling screens that the underlying cache data has changed
        setResult(SecuredActivity.RESULT_CODE_NEED_REFRESH);

        // Remove the file from the server
        int status = ServerAPI.getInstance(getApplicationContext()).deleteFile((CloudFile) cloudFile);
        
        if(status == ServerAPI.RESULT_OK)
        {
            // Remove the item from the adapter
            if(this.photoAdapter != null)
                this.photoAdapter.removeItem(position);
            
            this.refresh(true);
        }
        else
        {
            Dialog errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
    }

    protected void updateWindow(String comment)
    {
        LogUtil.debug("DirPhotoGridActivity", "updateWindow: " + comment);
        if(photoAdapter != null && gridView != null)
        {
            photoAdapter.setCurrentView(gridView.getFirstVisiblePosition(), gridView.getLastVisiblePosition());
        }
    }
    
    
    
    protected void updateWindow(String comment, int firstVisPosition, int endVisPosition)
    {
        LogUtil.debug("DirPhotoGridActivity", "updateWindow: " + comment);
        if(photoAdapter != null && gridView != null)
        {
            photoAdapter.setCurrentView(firstVisPosition, endVisPosition);
        }
    }
  
    
    protected class OnTouchListenerClass implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            if(getSwipeDetector().onTouchEvent(event))
            {
                return true;
            }
            return false;
        }
    }

    protected class OnItemSelectedListenerClass implements
            OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
        {
            setReferenceIndex();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0)
        {

        }
    }

    protected class OnItemClickListenerClass implements
            OnItemClickListener {
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
        {
            updateAlarm();
            // CloudFile f = (CloudFile)photoAdapter.getItem(position);
            Object listItem = photoAdapter.getItem(position);
            if (listItem instanceof String)
            {
                LogUtil.error(this, "String object received in DirPhotoGrid!!");
            }
            else
            {
                CloudFile cloudFile = (CloudFile)listItem;
                if (cloudFile != null) {
                    if (cloudFile instanceof Directory)
                    {
                        browseDirectory(cloudFile);
                    }
                    else if (cloudFile instanceof Photo)
                    {
                        viewPhotoFile(cloudFile, position);
                    }
                }
            } // if !(listItem instanceof String)
        }
    }

    protected class OnScrollListenerClass implements OnScrollListener {
        public void onScrollStateChanged(AbsListView view, int scrollState)
        {
            updateAlarm();

            photoAdapter.notifyScrollStateChanged(scrollState);  // No thumbnail requests while scrolling
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
            DownloadMoreView downloadMoreView = (DownloadMoreView) DirPhotoGridActivity.this.findViewById(R.id.gridlayout_view);
            if (photoAdapter.isNextPossible(firstVisibleItem + visibleItemCount, false)) {
                downloadMoreView.displayButton();
            } else {
                downloadMoreView.hideButton();
            }
            setReferenceIndex();
        }
    }

    protected class ListAdapterListenerClass implements
            ListAdapter.ListAdapterDataListener {
        @Override
        public void onDataRetrieved(ListAdapter callingAdapter) {
            DirPhotoGridActivity.this.refreshing = false;
            
            if (!isFinishing()) 
            {
                removeDialog(DIALOG_LOADING_ID);
                
                if (callingAdapter.getCount() == 0)
                {
                    TextView text = (TextView) findViewById(R.id.notification);
                    text.setVisibility(View.VISIBLE);
                }
                
                if(errDialog != null)
                    errDialog.dismiss();
                
                if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                {
                    updateWindow("onDataRetrieved(): initView()");
                }
                else
                {
                    DirPhotoGridActivity.this.handlingError = true;
                    errDialog = createErrorDialog(callingAdapter.getErrorCode());
                    errDialog.show();
                }
            }
        }
    }
    
    // Implementation of ProvisioningListener
    @Override
    public void onChange(int id)
    {
        // If any of the upload settings has changed, then inform the backup code
//        if (id == ProvisioningListener.HIDDEN_FILES_MODE)
//        {
//            this.isHiddenFilesModeChanged = true;
//        }
    }
    
    
    /**
     * @param cloudFile
     */
    @Override
    public void browseDirectory(CloudFile cloudFile) {
        Intent intent = new Intent(DirPhotoGridActivity.this, DirPhotoGridActivity.class);
        intent.putExtra("containerLink", cloudFile.getLink());
        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", DirPhotoGridActivity.this.canFilesBeDeleted);
        intent.putExtra("isPhotoDirGridEnabled", true); // this how we got to the grid view so is enabled
        intent.putExtra("deviceId", DirPhotoGridActivity.this.rootDeviceId);
        intent.putExtra("deviceTitle", DirPhotoGridActivity.this.rootDeviceTitle);
        intent.putExtra("deviceType", DirPhotoGridActivity.this.bDeviceEncrypted);
        intent.putExtra("platform", DirPhotoGridActivity.this.platform);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }




    // This task takes the current position in the list and calculates a new position based on that, that represents
    // the position in a list of only photos.
    private class CalculatePhotoPositionTask extends AsyncTask<Void, Void, Integer>
    {
        private int listPosition;
        CloudFile cloudFile;

        public CalculatePhotoPositionTask(int position, CloudFile inputFile)
        {
            this.listPosition = position;
            this.cloudFile = inputFile;
        }


        @Override
        protected Integer doInBackground(Void... params)
        {
            return Integer.valueOf(DirPhotoGridActivity.this.photoAdapter.getPhotosPosition(this.listPosition));
        }


        @Override
        protected void onPostExecute(Integer photosIndex)
        {
            goToPhotoSlideShow(photosIndex.intValue(), this.cloudFile);
        }

    } // class CalculatePhotoPosition
}
