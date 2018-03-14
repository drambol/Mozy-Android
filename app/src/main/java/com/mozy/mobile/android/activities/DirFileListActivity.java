package com.mozy.mobile.android.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class DirFileListActivity extends ContextMenuActivity {

    protected static final String CURRENT_POSITION_KEY = "CURRENT_POSITION_KEY";
    protected static final String SEARCH_TEXT_KEY = "SEARCH_TEXT_KEY";
    protected static final String RECURSE_KEY = "RECURSE_KEY";
    protected static final String DIR_LINK_KEY = "DIR_LINK_KEY";

    protected static final int DIALOG_LOADING_ID = ContextMenuActivity.LAST_USER_DIALOG + 1;
    protected static final int DIALOG_DOWNLOAD_PROMPT = ContextMenuActivity.LAST_USER_DIALOG + 3;
    protected static final int DIALOG_NO_RESULTS_ID = ContextMenuActivity.LAST_USER_DIALOG + 6;

    protected static final int MENU_REFRESH = MENU_LAST + 1;
    protected static final int MENU_PHOTO_GRID = MENU_REFRESH + 1;
    // TODO: Will need the 'search' menu pick when I make the search field a 'floating' field that comes and goes
    // as requested by the user.
    // private static final int MENU_SEARCH = 5;
    
    protected static final int  MENU_PHOTOS = MENU_PHOTO_GRID + 1;
    protected static final int  MENU_DOCUMENTS = MENU_PHOTOS + 1;
    protected static final int  MENU_MUSIC = MENU_DOCUMENTS + 1;
    protected static final int  MENU_VIDEO = MENU_MUSIC + 1;

    protected String containerLink;
    protected String title;
    protected String searchText;
    protected String searchDirectory;
    protected boolean recurse;
    protected int currentPosition;
    // Is the user allowed to delete files listed in this activity
    protected boolean canFilesBeDeleted;
    protected boolean fromNotification = false;

    protected boolean searching = false;
    protected boolean refreshing;

    protected boolean isSearchView = false;

    protected ListView listView;
    protected ListAdapter fileListAdapter;
    protected String rootDeviceId = null;
    protected String rootDeviceTitle = null;
    protected boolean bDeviceEncrypted = false;
    protected String platform = "";
    protected CloudFile fileToDownload = null;

    protected boolean initializing = false;

    protected AdapterContextMenuInfo contextMenuInfo = null;

    protected static final int SEARCH_TEXT_MIN_LENGTH = 3;
    
    protected boolean isPhotoDirGridEnabled = true;  //defaults true
    
//    protected boolean isHiddenFilesModeChanged = false;
    
    protected Dialog errDialog = null;
   

    @Override
    public void onCreate(Bundle savedInstanceState) { 
        super.onCreate(savedInstanceState);
        prepareActivity(savedInstanceState);
    }

    /**
     * @param savedInstanceState
     */
    protected void prepareActivity(Bundle savedInstanceState) {
        Provisioning provisioning = Provisioning.getInstance(getApplicationContext());
        

        if((provisioning.getMipAccountToken() != null && provisioning.getMipAccountToken().compareTo("") == 0) 
                && (provisioning.getMipAccountTokenSecret() != null && provisioning.getMipAccountTokenSecret().compareTo("") == 0))  // handles selection of uploaded file from notification when signed out from Mozy
        { 
            finish();
            LogUtil.debug(this, "Finishing Activity going to upload folder");
            return;
        }


        setContentView(BarActivity.SEARCH_BAR, R.layout.generic_list_layout);

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
            this.fromNotification = extras.getBoolean("fromNotification");
            LogUtil.debug(this, "fromNotification: " + this.fromNotification);
        }

        if (savedInstanceState != null) {
            LogUtil.debug(this, "Loading saved instance state.");
            currentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY);
            LogUtil.debug(this, "position: " + currentPosition);
            searchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
            LogUtil.debug(this, "searchText: " + searchText);
        }
        

        this.contextMenuFlags = setContextMenuFlags(this.canFilesBeDeleted);
 
        this.refreshing = false;

        // Remember we are in listview mode.
        sGridMode = false;

        isSearchView = (searchText != null && searchText.length() > 0);
        if (isSearchView)
        {
            String searchTitle = getString(R.string.search_results_title);
            setBarTitle(searchTitle.replace("$NAME", title));
        }
        else if (title != null)
        {
            setBarTitle(title);
        }
        fileListAdapter = (ListAdapter) getLastNonConfigurationInstance();

        if (fileListAdapter == null) {
            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
        }
     
        initSearch();

        initView();
    }


    @Override
    public void onResume() {
        super.onResume();

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
        if(!fileListAdapter.enable() && showDialog)
        {
            removeDialog(DIALOG_LOADING_ID);
            refreshing = false;
        }
        
        updateWindow("onResume()");
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt(CURRENT_POSITION_KEY, currentPosition);
        savedInstanceState.putString(SEARCH_TEXT_KEY, searchText);
        savedInstanceState.putString(DIR_LINK_KEY, containerLink);
        savedInstanceState.putBoolean(RECURSE_KEY, recurse);
    }

    @Override
    public void onPause() {
        super.onPause();
        ContextMenuActivity.sLastFolder = containerLink;
        fileListAdapter.disable();
    }

    @Override
    public void onStop() {
        super.onStop();
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
    public void onBackPressed() {
        
        
        if(fromNotification == false)
        {
           int numdevices = SystemState.getDevicePlusSyncCount();   
           
           if(numdevices == 1 && 
                   (this.searchText != null &&
                       (this.searchText.equals(FileUtils.photoSearch) || 
                        this.searchText.equals(FileUtils.documentSearch) || 
                        this.searchText.equals(FileUtils.videoSearch) || 
                        this.searchText.equals(FileUtils.musicSearch))))  // Return to home screen only for the quick browse scenarios
           {
               NavigationTabActivity.returnToHomescreen(this);
           }
           else
               finish();
        }
        else
        {
            finish();
            NavigationTabActivity.returnToHomescreen(this);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == DIALOG_LOADING_ID)
        {
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(
                    getResources().getString(DirFileListActivity.this.isSearchView
                                                ? R.string.progress_bar_searching
                                                : R.string.progress_bar_loading));

            loadingDialog.setIndeterminate(true);
            loadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        DirFileListActivity.this.finish();
                    }
                    return true;
                }
            });
            return loadingDialog;
        }
        else if (id == DIALOG_DOWNLOAD_PROMPT)
        {
            TelephonyManager teleManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            if (teleManager.isNetworkRoaming())
            {
                builder.setTitle(R.string.warning);
                builder.setMessage(R.string.roaming_download_warning);
            }
            else
            {
                builder.setMessage(R.string.download_prompt);
            }

            builder.setPositiveButton(getString(R.string.yes_button_text), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    DirFileListActivity.this.downloadFile(DirFileListActivity.this.fileToDownload);
                    DirFileListActivity.this.fileToDownload = null;
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(getString(R.string.no_button_text), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    DirFileListActivity.this.fileToDownload = null;
                    dialog.dismiss();
                }
            });
            return builder.create();
        }
        else if(id == DIALOG_NO_RESULTS_ID)
        {
            /*
             * Get and initialize the message text
             */
            String message = getString(R.string.search_no_results_prompt_text);
            message = message.replace("$SEARCH", searchText);

            /*
             * Build the dialog
             */
            return new AlertDialog.Builder(DirFileListActivity.this)
            .setTitle(R.string.search_no_results_prompt_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    /*
                     * On dialog close we reset search text, clear search box
                     * and reload the current directory structure
                     */
                    dialog.dismiss();
                    searchText = "";
                    EditText editText = (EditText) findViewById(R.id.search_bar);
                    editText.setText("");
                    refresh(true);
                }
            })
            .create();
        }

        return super.onCreateDialog(id);
    }

    @Override
    public void onPrepareDialog(int id, Dialog dialog)
    {
        if(id == DIALOG_NO_RESULTS_ID)
        {
            /*
             * Get and initialize the message text
             */
            String message = getString(R.string.search_no_results_prompt_text);
            message = message.replace("$SEARCH", searchText);

            ((AlertDialog) dialog).setMessage(message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FILES, 0, getResources().getString(R.string.menu_files)).setIcon(R.drawable.allfiles);
        menu.add(0, MENU_HOME, 1, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        if(isPhotoDirGridEnabled == true)
        {
            menu.add(0, MENU_PHOTO_GRID, 2, getResources().getString(R.string.photos_only)).setIcon(R.drawable.gallery_view);
        }
        menu.add(0, MENU_REFRESH, 3, getResources().getString(R.string.menu_refresh)).setIcon(R.drawable.refresh);
        menu.add(0, MENU_HELP, 4, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 5, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);

        // TODO: Will need the 'search' menu pick when I make the search field a 'floating' field that comes and goes
        // as requested by the user.
        // menu.add(0, MENU_SEARCH, 0, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_search);

        return true;
    }

    protected synchronized void refresh(boolean flushCache) {
        if (!refreshing && !searching) {

            refreshing = true;
            showDialog(DIALOG_LOADING_ID);
            fileListAdapter.disable();
            // fileListAdapter.clean(true); // Already done as a result of 'refresh' parameter in new ListAdaptor
            fileListAdapter.unregisterListener();
            ListAdapter.ListAdapterDataListener listener = new ListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(ListAdapter callingAdapter) {
                    DirFileListActivity.this.refreshing = false;
                    DirFileListActivity.this.searching = false;
                    
                    if (!isFinishing()) 
                    {
                        removeDialog(DIALOG_LOADING_ID);
                        
                        if(errDialog != null)
                            errDialog.dismiss();
                        
                        if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                        {
                            DirFileListActivity.this.updateViewForNumItems(callingAdapter.getCount());
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
            
            this.fileListAdapter = null;  // reset adapter for refresh
            this.fileListAdapter = getAdapter(listener);
            currentPosition = 0;
            listView.setAdapter(fileListAdapter);
            listView.setSelection(currentPosition);
            fileListAdapter.setReferencePosition(currentPosition);
            
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_REFRESH:
            this.refresh(true);
            return true;
        case MENU_PHOTO_GRID:
            gotoPhotoGrid();
            // Do NOT finish() this activity. Want to be able to go back.
            return true;
         default:
             return super.onOptionsItemSelected(item);
        }
    }
 
    
    protected void gotoPhotoGrid() {
        Intent intent = new Intent(DirFileListActivity.this, DirPhotoGridActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("searchText", searchText);
        intent.putExtra("searchDirectory", searchDirectory);
        intent.putExtra("title", title);
        intent.putExtra("recurse", recurse);
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
                    Intent intent = new Intent(DirFileListActivity.this, DirPhotoGridActivity.class);
                    intent.putExtra("containerLink", containerLink);
                    intent.putExtra("searchText", searchText);
                    intent.putExtra("searchDirectory", searchDirectory);
                    intent.putExtra("title", title);
                    intent.putExtra("recurse", recurse);
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
    protected int handleError(int errorCode, int errorType)
    {
        int returnValue = super.handleError(errorCode, errorType);

        return returnValue;
    }

    @Override
    protected Dialog createErrorDialog(int errorCode)
    {
        Dialog returnValue = super.createErrorDialog(errorCode);

        DirFileListActivity.this.refreshing = false;
        DirFileListActivity.this.searching = false;
        removeDialog(DIALOG_LOADING_ID);

        if (returnValue == null)
        {
            // If we get an error whilie initializing the screen, then use a different dialog ID then if we get an error
            // at other times.
            int dialogId = this.initializing ? ErrorActivity.DIALOG_ERROR_ID : ErrorActivity.DIALOG_ERROR_NO_FINISH_ID;
            int errorString = 0;
            int errorTitle = R.string.error;

            switch (errorCode)
            {
                case ServerAPI.RESULT_UNAUTHORIZED:
                case ServerAPI.RESULT_FORBIDDEN:
                    errorString = R.string.errormessage_authorization_failure; 
                    break;
                case ServerAPI.RESULT_INVALID_CLIENT_VER:
                    errorString = R.string.client_upgrade_required;
                    errorTitle = R.string.client_upgrade_title;
                    break;
                case ServerAPI.RESULT_INVALID_TOKEN:
                    errorString = R.string.device_revoked_body;
                    break;
                case ServerAPI.RESULT_AUTHORIZATION_ERROR:
                    errorString = R.string.authorization_error;
                    break;
                case ServerAPI.RESULT_INVALID_USER:
                    errorString = R.string.invalid_user;
                    break;
                case ServerAPI.RESULT_CONNECTION_FAILED:
                case ServerAPI.RESULT_UNKNOWN_PARSER:
                case ServerAPI.RESULT_UNKNOWN_ERROR:
                    errorString = R.string.error_not_available;
                    break;
                default:
                    errorString = R.string.error_not_available;
                    break;
            }
            if (errorString != 0)
            {
                returnValue = createGenericErrorDialog(dialogId, errorTitle, errorString,  R.string.ok_button_text);
            }
        }

        return returnValue;
    }

    @Override
    protected void onButtonClick(int dialogId, int buttonId)
    {
        if (dialogId == ErrorActivity.DIALOG_ERROR_ID)
        {
            this.finish();
        }
        else if (dialogId == ErrorActivity.DIALOG_ERROR_NO_FINISH_ID)
        {
            // Just a warning dialog, do not close the screen
            //this.refresh(true);
        }

        super.onButtonClick(dialogId, buttonId);
    }

    @Override
    public String getRootDeviceId()
    {
        return this.rootDeviceId;
    }
    
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
    
    
    protected void setReferenceIndex() {
        currentPosition = listView.getFirstVisiblePosition();

        if (currentPosition + 6 < fileListAdapter.getCount()) {
            fileListAdapter.setReferencePosition(currentPosition + 6);
        } else {
            fileListAdapter.setReferencePosition(fileListAdapter.getCount() - 1);
        }
    }
   
    private void initSearch() {
        final EditText editText = (EditText) findViewById(R.id.search_bar);

        String hint = getString(R.string.search) + " " + this.title;
        editText.setHint(hint);
        editText.setText(searchText);
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

        editText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == 66 && event.getAction() == KeyEvent.ACTION_UP)
                {
                    // Make sure the *trimmed* string is long enough.
                    String searchText = ((EditText)v).getEditableText().toString().trim();
                    if (searchText.length() >= SEARCH_TEXT_MIN_LENGTH)
                    {
                        // Make the trimmed version of the string be official.
                        ((EditText)v).setText(searchText);
                        updateSearch();
                    }
                    // Even if we didn't start a search, we return true so that
                    // the focus stays in the edit box.
                    return true;
                }
                return false;
            }
        });

        final ImageView searchButton = (ImageView) findViewById(R.id.search_button);
        registerForContextMenu(searchButton);
        searchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                 EditText searchEdit = (EditText) findViewById(R.id.search_bar);
                // Make sure the *trimmed* string is long enough.
                String searchText = searchEdit.getEditableText().toString().trim();
                if (searchText.length() >= SEARCH_TEXT_MIN_LENGTH)
                {
                    searchEdit.setText(searchText);
                    updateSearch();
                }
            }
        });
    }

    private void updateSearch() {
        LogUtil.debug("SEARCH", "updateSearch()");
        LogUtil.debug("SEARCH", "searching: " + searching);
        LogUtil.debug("SEARCH", "refreshing: " + refreshing);

        if (!searching && !refreshing) {
            updateAlarm();

            final EditText editText = (EditText) findViewById(R.id.search_bar);
            // close soft keyboard
            InputMethodManager inputManager = (InputMethodManager) DirFileListActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(editText.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            // Get the string for searching.
            String newSearchText = editText.getEditableText().toString();
            // Don't want the old view to keep the new search string.
            editText.setText(searchText);

            Intent intent = new Intent(DirFileListActivity.this, DirFileListActivity.class);
            intent.putExtra("containerLink", containerLink);
            // searchText and recurse are set going forward and not in the current intent
            intent.putExtra("searchText", newSearchText);
            intent.putExtra("searchDirectory", searchDirectory);
            intent.putExtra("recurse", true);
            intent.putExtra("title", title);
            intent.putExtra("canFilesBeDeleted", DirFileListActivity.this.canFilesBeDeleted);
            intent.putExtra("deviceId", DirFileListActivity.this.rootDeviceId);
            intent.putExtra("deviceTitle", DirFileListActivity.this.rootDeviceTitle);
            intent.putExtra("deviceType", DirFileListActivity.this.bDeviceEncrypted);
            intent.putExtra("platform", DirFileListActivity.this.platform);
            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
        }
    }
    
  
        private void updateQuickSearch (String newSearchText) {
        boolean isPhotoDirGridEnabled = false;
        LogUtil.debug("SEARCH", "updateSearch()");
        LogUtil.debug("SEARCH", "searching: " + searching);
        LogUtil.debug("SEARCH", "refreshing: " + refreshing);

        if (!searching && !refreshing) {
            updateAlarm();

            final EditText editText = (EditText) findViewById(R.id.search_bar);
            // Don't want the old view to keep the new search string.
            editText.setText(searchText);

            Intent intent = new Intent(DirFileListActivity.this, DirFileListActivity.class);
            intent.putExtra("containerLink", containerLink);
            // searchText and recurse are set going forward and not in the current intent
            intent.putExtra("searchText", newSearchText);
            intent.putExtra("searchDirectory", searchDirectory);
            intent.putExtra("recurse", true);
            intent.putExtra("title", title);
            intent.putExtra("canFilesBeDeleted", DirFileListActivity.this.canFilesBeDeleted);
            intent.putExtra("deviceId", DirFileListActivity.this.rootDeviceId);
            intent.putExtra("deviceTitle", DirFileListActivity.this.rootDeviceTitle);
            intent.putExtra("deviceType", DirFileListActivity.this.bDeviceEncrypted);
            intent.putExtra("platform", DirFileListActivity.this.platform);
            if(newSearchText.equalsIgnoreCase(FileUtils.photoSearch))
                isPhotoDirGridEnabled = true;
            intent.putExtra("isPhotoDirGridEnabled", isPhotoDirGridEnabled);
            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
        }
    }
       
    
    protected void initView() {
        this.initializing = true;
        listView = (ListView) findViewById(R.id.generic_list);

        ListAdapter.ListAdapterDataListener listener = getListAdapterListenerClass();
        
        
        if (fileListAdapter == null) {
                fileListAdapter = getAdapter(listener);
        }
        
        this.updateViewForNumItems(fileListAdapter.getCount());

        listView.setOnScrollListener(getOnScrollListenerClass());

        listView.setOnItemClickListener(getOnItemClickListenerClass());

        listView.setOnItemSelectedListener(getOnItemSelectedListenerClass());

        listView.setOnTouchListener(getOnTouchListenerClass());

        listView.setAdapter(fileListAdapter);
        listView.setSelection(currentPosition);
        fileListAdapter.setReferencePosition(currentPosition);

        this.registerForContextMenu(listView);
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

    /**
     * @param listener
     */
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener) {
        
        if(this.fileListAdapter == null)
        {
            if (searchText != null && searchText.length() > 0) {
                this.fileListAdapter = new ListAdapter(getApplicationContext(),
                        R.layout.list_item_layout,
                        this.containerLink,
                        this.bDeviceEncrypted,
                        searchText,
                        this.searchDirectory,
                        false,        // directoriesOnly
                        false,        // photosOnly
                        true,            // recurse
                        true,    // refresh
                        listener,
                        ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                        null,
                        this.getRootDeviceId(),
                        this.getRootDeviceTitle());
            } else {
                this.fileListAdapter = new ListAdapter(getApplicationContext(),
                        R.layout.list_item_layout,
                        this.containerLink,
                        this.bDeviceEncrypted,
                        "",
                        "",
                        false,        // directoriesOnly
                        false,         // photosOnly
                        false,         // recurse
                        true,     // refresh
                        listener,
                        ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST,
                        null,
                        this.getRootDeviceId(),
                        this.getRootDeviceTitle());
            }
        }
        return this.fileListAdapter;
    }

    protected void updateViewForNumItems(int numItems)
    {
        // Show/hide the "no items found" message as appropriate.
        // And do the opposite for the footer divider line we are adding manually.
        boolean isListVisible = numItems > 0;
        TextView text = (TextView) findViewById(R.id.notification);
        
        if (this.title.equalsIgnoreCase("sync"))
        	text.setText(R.string.empty_sync_container);
        else
        	text.setText(R.string.no_items);
        	
        text.setVisibility(isListVisible ? View.GONE : View.VISIBLE);
        findViewById(R.id.footer_divider).setVisibility(isListVisible ? View.VISIBLE : View.GONE);
    }

    protected void goToPhotoSlideShow(int photoPosition, CloudFile cloudFile)
    {
        // Load the preview activity
        Intent intent = new Intent(DirFileListActivity.this, PhotoSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);

        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("searchDirectory", cloudFile.getPath());
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", DirFileListActivity.this.canFilesBeDeleted);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }


    // override
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo)
    {
        
         if(view == (ImageView) findViewById(R.id.search_button))
         {
             super.onCreateContextMenu(menu, view, menuInfo);
             menu.add(0, MENU_PHOTOS, 0, getResources().getString(R.string.search_photos));
             menu.add(0, MENU_DOCUMENTS, 0, getResources().getString(R.string.search_documents));
             menu.add(0, MENU_MUSIC, 0, getResources().getString(R.string.search_music));
             menu.add(0, MENU_VIDEO, 0, getResources().getString(R.string.search_video));
         }
         else
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
    
                CloudFile cloudFile = (CloudFile)listItem;
                super.buildContextMenu(menu, view, menuInfo, cloudFile, this.bDeviceEncrypted);
            }
         }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem)
    {    
        switch (menuItem.getItemId()) {
        case MENU_PHOTOS:
            updateQuickSearch(FileUtils.photoSearch );
            break;
        case MENU_DOCUMENTS:
            updateQuickSearch(FileUtils.documentSearch );
            break;
        case MENU_MUSIC:
            updateQuickSearch(FileUtils.musicSearch );
            break;
        case MENU_VIDEO:
            updateQuickSearch(FileUtils.videoSearch );
            break;
        default:
            AdapterContextMenuInfo info = (AdapterContextMenuInfo)menuItem.getMenuInfo();

            if (info == null)
            {
                info = this.contextMenuInfo;
            }
            Object listItem = fileListAdapter.getItem(info.position);

            // title list item
            if (!(listItem instanceof String))
            {
                CloudFile cloudFile = (CloudFile)listItem;
                super.handleContextMenuItemSelection(menuItem, cloudFile, info.position);
            }
        }
        return true;
    }

    @Override
    protected void removeItem(int position, final MozyFile cloudFile)
    {
    	if (cloudFile instanceof Directory) {
    		//TODO: ...
    		return;
    	}
        // Remove the file from the server
        int errorCode = ServerAPI.getInstance(getApplicationContext()).deleteFile((CloudFile) cloudFile);

        if (errorCode != ServerAPI.RESULT_OK)
        {
            Dialog errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
        else
        {
         // Remove the item from the adapter
            if(this.fileListAdapter != null)
                this.fileListAdapter.removeItem(position);
        }

        this.refresh(true);
    }

    @Override
    public void onContextMenuClosed(Menu menu)
    {
        super.onContextMenuClosed(menu);
        this.contextMenuInfo = null;
    }

    @Override
    protected void viewPhoto(int position, MozyFile cloudFile)
    {
        showDialog(DIALOG_LOADING_ID);

        // Run a background task to do this as we need to go to the SQLite database and all accesses to the
        // database should be done in the background.
        CalculatePhotoPositionTask calculatePhotoPositionTask = new CalculatePhotoPositionTask(position, cloudFile);
        calculatePhotoPositionTask.execute();
    }

    protected void updateWindow(String comment)
    {
        LogUtil.debug("DirFileListActivity", "updateWindow: " + comment);
        if(fileListAdapter != null && listView != null)
        {
            if(DirFileListActivity.this.fromNotification && this.fileListAdapter.lastUploadPositionInDirFileList >0)
            {
                fileListAdapter.setCurrentView(this.fileListAdapter.lastUploadPositionInDirFileList, this.fileListAdapter.lastUploadPositionInDirFileList + 15);
            }
            else
            {
                fileListAdapter.setCurrentView(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
            }
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
        public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
            setReferenceIndex();
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    protected class OnItemClickListenerClass implements
            OnItemClickListener {
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
            updateAlarm();
            Object listItem = fileListAdapter.getItem(position);

            // Only handle the click if this is not a 'title object.
            if (!(listItem instanceof String))
            {
                CloudFile cloudFile = (CloudFile)listItem;

                // This should only be null if the user clicked on the 'get more data' item at the end of the list.
                if (cloudFile == null)
                {
                    if (fileListAdapter.isNextItem(position)) {
                        showDialog(DIALOG_LOADING_ID);
                        refreshing = true;
                        fileListAdapter.increaseItems();
                    }
                    LogUtil.debug(this, "No file (null) for list item in position: " + Integer.toString(position));
                    return;
                }
                if (cloudFile instanceof Directory)
                {
                    browseDirectory(cloudFile);
                }
                else if (cloudFile instanceof Photo)
                {
                    viewPhotoFile(cloudFile, position);
                        
                }
                else  // another file type
                {
                    DirFileListActivity.this.showMozyFile(cloudFile);
                }
            } // if (!(listItem instanceof String)
        }
    }

    protected class OnScrollListenerClass implements OnScrollListener {
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
                setReferenceIndex();
                updateWindow("SCROLL_STATE_IDLE");
                break;
            case SCROLL_STATE_TOUCH_SCROLL:
                break;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        }
    }

    protected class ListAdapterListenerClass implements
            ListAdapter.ListAdapterDataListener {
        @Override
        public void onDataRetrieved(ListAdapter callingAdapter) {
            
            DirFileListActivity.this.initializing = false;
            DirFileListActivity.this.searching = false;
            DirFileListActivity.this.refreshing = false;
         
            if (!isFinishing()) 
            {
                removeDialog(DIALOG_LOADING_ID);
                
                if(errDialog != null)
                     errDialog.dismiss();
                
                if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                {
                    
                    if(DirFileListActivity.this.fromNotification && callingAdapter.lastUploadPositionInDirFileList >0)
                    {
                        DirFileListActivity.this.listView.setSelection(callingAdapter.lastUploadPositionInDirFileList);
                    }
                    
                    DirFileListActivity.this.updateViewForNumItems(callingAdapter.getCount());
                    updateWindow("onDataRetrieved(): (initView)");  
                }
                else
                {
                    DirFileListActivity.this.handlingError = true;
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
        Intent intent = new Intent(DirFileListActivity.this, DirFileListActivity.class);
        intent.putExtra("containerLink", cloudFile.getLink());
        intent.putExtra("searchText", searchText);
        intent.putExtra("searchDirectory", cloudFile.getPath());
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", DirFileListActivity.this.canFilesBeDeleted);
        intent.putExtra("deviceId", DirFileListActivity.this.rootDeviceId);
        intent.putExtra("deviceTitle", DirFileListActivity.this.rootDeviceTitle);
        intent.putExtra("deviceType", DirFileListActivity.this.bDeviceEncrypted);
        intent.putExtra("platform", DirFileListActivity.this.platform);
        intent.putExtra("isPhotoDirGridEnabled", DirFileListActivity.this.isPhotoDirGridEnabled);
        
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    // This task takes the current position in the list and calculates a new position based on that, that represents
    // the position in a list of only photos.
    private class CalculatePhotoPositionTask extends AsyncTask<Void, Void, Integer>
    {
        private int listPosition;
        CloudFile cloudFile;

        public CalculatePhotoPositionTask(int position, MozyFile inputFile)
        {
            this.listPosition = position;
            this.cloudFile = (CloudFile) inputFile;
        }

        @Override
        protected Integer doInBackground(Void... params)
        {
            return Integer.valueOf(DirFileListActivity.this.fileListAdapter.getPhotosPosition(this.listPosition));
        }


        @Override
        protected void onPostExecute(Integer photosIndex)
        {
            if(photosIndex != -1)
                goToPhotoSlideShow(photosIndex.intValue(), this.cloudFile);
        }

    } // class CalculatePhotoPosition
}
