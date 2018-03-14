/*  Copyright Tactel AB 2009
 *
 *  All copyrights in this software are created and owned by Tactel AB.
 *  This software, or related intellectual property, may under no
 *  circumstances be used, distributed or modified without written
 *  authorization from Tactel AB.
 *  This copyright notice may not be removed or modified and  shall be
 *  displayed in all materials that include the software or portions of such.
 */

package com.mozy.mobile.android.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnKeyListener;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.views.ActionView;
import com.mozy.mobile.android.views.SlideView;
import com.mozy.mobile.android.views.SlideView.SlideViewListener;

public class PhotoSlideShowActivity extends ContextMenuActivity
{
    protected static final String CURRENT_POSITION_KEY = "CURRENT_POSITION_KEY";
    protected static final String SEARCH_TEXT_KEY = "SEARCH_TEXT_KEY";
    protected static final String RECURSE_KEY = "RECURSE_KEY";

    protected static final int MENU_EXPORT = 0;
    protected static final int MENU_DOWNLOAD = 1;
    protected static final int MENU_DELETE = 2;
    protected static final int MENU_HIRES = 3;
    
    // Initialize with an invalid position
    protected int currentPosition=-1;
    protected String searchText;
    protected String containerLink;
    protected String rootDeviceId;
    protected String rootDeviceTitle;
    protected boolean bDeviceEncrypted;
    protected String platform = "";
    protected boolean recurse;
    protected boolean refreshing = false;

    protected boolean canFilesBeDeleted;
    protected boolean canFilesBeShared = false;            // Assume no and turn on later.

    protected ListAdapter photoAdapter;
    protected SlideView slider;
    protected ActionView mActionView;

    protected int filePosition = 0;
    protected CloudFile fileToRemove = null;
    
    protected Dialog errDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(BarActivity.NO_BAR, R.layout.photo_slideshow_layout);

        Bundle extras  = getIntent().getExtras();
        if(extras != null)
        {
            LogUtil.debug(this, "Loading extras from bundle:");
            containerLink = extras.getString("containerLink");
            LogUtil.debug(this, "containerLink: " + containerLink);
            currentPosition = extras.getInt("position");
            LogUtil.debug(this, "position: " + currentPosition);
            //this.firstFileDisplayed = extras.getString("firstFileDisplayed");
            //LogUtil.debug(this, "firstFileDisplayed: " + firstFileDisplayed);
            searchText = extras.getString("searchText");
            LogUtil.debug(this, "searchText: " + searchText);
            rootDeviceId = extras.getString("deviceId");
            LogUtil.debug(this, "deviceId: " + this.rootDeviceId);
            rootDeviceTitle = extras.getString("deviceTitle");
            LogUtil.debug(this, "deviceTitle: " + this.rootDeviceTitle);
            bDeviceEncrypted = extras.getBoolean("deviceType");
            LogUtil.debug(this, "deviceType: " + this.bDeviceEncrypted);
            platform = extras.getString("platform");
            LogUtil.debug(this, "platform: " + this.platform);
            this.canFilesBeDeleted = extras.getBoolean("canFilesBeDeleted");
            this.recurse = extras.getBoolean("recurse");
            this.searchText = extras.getString("searchText");
            this.canFilesBeShared = SystemState.isExportEnabled();
        }else {
            //canFilesBeDeleted = false;
            rootDeviceId = null;
            rootDeviceTitle = null;
        }

        if (savedInstanceState != null)
        {
            LogUtil.debug(this, "Loading saved instance state:");
            currentPosition = savedInstanceState.getInt(CURRENT_POSITION_KEY);
            LogUtil.debug(this, "currentPosition: " + currentPosition);
            searchText = savedInstanceState.getString(SEARCH_TEXT_KEY);
            LogUtil.debug(this, "searchText: " + searchText);
            this.recurse = savedInstanceState.getBoolean(RECURSE_KEY);
        }
 
        this.contextMenuFlags = setContextMenuFlags(this.canFilesBeDeleted);
 

        photoAdapter = (ListAdapter) getLastNonConfigurationInstance();

        initView();
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
    public void onResume()
    {
        super.onResume();
        photoAdapter.enable();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState)
    {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(SEARCH_TEXT_KEY, searchText);
        savedInstanceState.putInt(CURRENT_POSITION_KEY, currentPosition);
        savedInstanceState.putBoolean(RECURSE_KEY, recurse);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        photoAdapter.disable();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public Object onRetainNonConfigurationInstance()
    {
        return photoAdapter;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        switch (id)
        {
        case DIALOG_LOADING_ID:
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(getResources().getString(R.string.progress_bar_loading));
            loadingDialog.setIndeterminate(true);
            loadingDialog.setOnKeyListener(new OnKeyListener() {

                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                    if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        PhotoSlideShowActivity.this.finish();
                    }
                    return true;
                }
            });
            return loadingDialog;
        case DELETE_FILE_CONFIRMATION_ID:
            String promptString = this.getString(R.string.delete_confirmation);
            promptString = promptString.replace("$FILENAME", this.fileToRemove.getTitle());
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(promptString);
            builder.setCancelable(false);
            builder.setPositiveButton(this.getString(R.string.yes_button_text), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    PhotoSlideShowActivity.this.removeItem(PhotoSlideShowActivity.this.filePosition, PhotoSlideShowActivity.this.fileToRemove);
                    PhotoSlideShowActivity.this.removeDialog(DELETE_FILE_CONFIRMATION_ID);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int id)
                {
                    // dialog.cancel();
                    dialog.dismiss();
                    PhotoSlideShowActivity.this.removeDialog(DELETE_FILE_CONFIRMATION_ID);
                }
            });
            
            return builder.create();                
        }

        return super.onCreateDialog(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_DOWNLOAD, 0, getResources().getString(R.string.menu_download)).setIcon(R.drawable.ic_menu_save);
        menu.add(0, MENU_HIRES, 1, getResources().getString(R.string.menu_hires)).setIcon(R.drawable.ic_menu_hires);

        if (this.canFilesBeDeleted)
        {
            menu.add(0, MENU_DELETE, 2, getResources().getString(R.string.menu_delete)).setIcon(R.drawable.ic_menu_delete);
        }

        if (this.canFilesBeShared)
        {
            // $TODO: figure out what sharing is available.
            menu.add(0, MENU_EXPORT, 3, getResources().getString(R.string.share_menu_label)).setIcon(R.drawable.ic_menu_share);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);
        
        boolean fileExists = isCurrentCloudFileDownloadedOnSDCard((CloudFile)photoAdapter.getItem(currentPosition));
        
        if(fileExists)
            menu.getItem(0).setEnabled(false);
        else
            menu.getItem(0).setEnabled(true);
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case MENU_DOWNLOAD:
            this.downloadFile((CloudFile)photoAdapter.getItem(currentPosition));
            return true;
        case MENU_HIRES:
            this.showMozyFile((CloudFile)photoAdapter.getItem(currentPosition));
            return true;
        case MENU_DELETE:
            this.filePosition = currentPosition;
            this.fileToRemove = (CloudFile)photoAdapter.getItem(currentPosition);                    
            showDialog(DELETE_FILE_CONFIRMATION_ID);
            return true;
        case MENU_EXPORT:
            this.exportFile((CloudFile)photoAdapter.getItem(currentPosition));
            return true;
        }
        return false;
    }

    // Called by context menu code.
    @Override
    protected void removeItem(int position, final MozyFile cloudFile)
    {
        // Indicate to any calling screens that the underlying cache data has changed
        setResult(SecuredActivity.RESULT_CODE_NEED_REFRESH);

        showDialog(DIALOG_LOADING_ID);
        
        // Remove the file from the server
        
        int status = ServerAPI.getInstance(getApplicationContext()).deleteFile((CloudFile) cloudFile);
        
        if(status == ServerAPI.RESULT_OK)
        {
        
            // Remove the item from the adapter
            if(this.photoAdapter != null)
                this.photoAdapter.removeItem(position);
            
            this.currentPosition = Math.max(0, PhotoSlideShowActivity.this.currentPosition - 1);
            this.refresh(true);
        }
        else
        {
            Dialog errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
    }

    @Override
    protected int handleError(int errorCode, int errorType)
    {
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
//            case ErrorCodes.ERROR_ENCRYPTED_FILE:
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
        PhotoSlideShowActivity.this.refreshing = false;
        removeDialog(DIALOG_LOADING_ID);
        return super.createErrorDialog(errorCode);
    }

    @Override
    protected void onButtonClick(int dialogId, int buttonId)
    {
        if (dialogId == DIALOG_ERROR_ID)
        {
            if (buttonId == BUTTON_TWO)
            {
                finish();
            }
            else if (buttonId == BUTTON_ONE)
            {
                Intent intent = this.getIntent();
                startActivity(intent);
                finish();
            }
        }
        else if(dialogId == DIALOG_NORETRY_ERROR_ID)
        {
            if(buttonId == BUTTON_ONE) finish();
        }
        super.onButtonClick(dialogId, buttonId);
    }

    protected void initView()
    {
        if (photoAdapter == null)
        {
            //showDialog(DIALOG_LOADING_ID);
        }

        slider = (SlideView)findViewById(R.id.slideshow_slideview);
        ListAdapter.ListAdapterDataListener listener = new ListAdapter.ListAdapterDataListener() {

            @Override
            public void onDataRetrieved(ListAdapter callingAdapter) {
                
                if (!isFinishing()) 
                {
                    removeDialog(DIALOG_LOADING_ID);
                    
                    if(errDialog != null)
                        errDialog.dismiss();
                    
                    if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                    {
                        if(callingAdapter.getCount() == 0)  // No photos
                            finish();
                        
                        updateWindow("onDataRetrieved(): initView()");
                    }
                    else
                    {
                        PhotoSlideShowActivity.this.handlingError = true;
                        errDialog = createErrorDialog(callingAdapter.getErrorCode());
                        errDialog.show();
                    }
                }
            }
        };

       final int initialPosition = currentPosition;
       ListAdapter.ThumbnailAvailabilityListener thumbListener = new ListAdapter.ThumbnailAvailabilityListener()
        {
            @Override
            public void onThumbnailAvailable(int position)
            {
                if(position == initialPosition)
                {
                    removeDialog(DIALOG_LOADING_ID);
                }
            }
        };

        if (photoAdapter == null)
        {
            photoAdapter = getAdapter(listener, thumbListener);
        }

        slider.setSlideViewListener(new SliderViewListenerClass());

        slider.setAdapter(photoAdapter);
        slider.setSelection(currentPosition);
        photoAdapter.setReferencePosition(currentPosition);

        setUpActionView();

        updateWindow("initView()");
    }

    /**
     * 
     */
    protected void setUpActionView() {
        // Set up the fading options view
        mActionView = (ActionView) findViewById(R.id.actionview);
        mActionView.addOnClickListener(new ActionView.OnClickListener()
        {
            public void exportClicked()
            {
                // $TODO: other sharing types...
                   PhotoSlideShowActivity.this.exportFile((CloudFile)photoAdapter.getItem(currentPosition));
            }

            public void HiResClicked()
            {
                PhotoSlideShowActivity.this.showMozyFile((CloudFile)photoAdapter.getItem(currentPosition));
            }

            public void deleteClicked()
            {
            	PhotoSlideShowActivity.this.filePosition = currentPosition;
                PhotoSlideShowActivity.this.fileToRemove = (CloudFile)photoAdapter.getItem(currentPosition);
                if (null != PhotoSlideShowActivity.this.fileToRemove)
                	showDialog(DELETE_FILE_CONFIRMATION_ID);
            }
        });

        mActionView.init();
        mActionView.showAction(ActionView.ACTION_DELETE, this.canFilesBeDeleted);
        mActionView.showAction(ActionView.ACTION_SHARE, this.canFilesBeShared);
    }

    /**
     * @param listener
     * @param thumbListener
     */
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener,
            ListAdapter.ThumbnailAvailabilityListener thumbListener) {
        ListAdapter photoAdapter = new ListAdapter(getApplicationContext(),
                                       R.layout.photo_full_screen,
                                       this.containerLink,
                                       this.bDeviceEncrypted,
                                       this.searchText,
                                       "",
                                       false,            // directoriesOnly
                                       true,            // photosOnly
                                       recurse,
                                       false,            // refresh
                                       listener,
                                       ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW,
                                       thumbListener,
                                       this.getRootDeviceId(),
                                       this.getRootDeviceTitle());
        
        return photoAdapter;
    }


    // The following is used when deleting items
    protected synchronized void refresh(boolean flushCache)
    {
        try
        {
        if (!refreshing)
        {
            this.refreshing = true;
            this.photoAdapter.disable();
            //this.photoAdapter.clean(true);
            this.photoAdapter.unregisterListener();
            ListAdapter.ListAdapterDataListener listener = new ListAdapter.ListAdapterDataListener() {

                @Override
                public void onDataRetrieved(ListAdapter callingAdapter) {
                    PhotoSlideShowActivity.this.refreshing = false;
                    if (!isFinishing()) 
                    {
                        removeDialog(DIALOG_LOADING_ID);
                        
                        if(errDialog != null)
                            errDialog.dismiss();
                        
                        if (callingAdapter.getErrorCode() ==  ServerAPI.RESULT_OK)
                        {
                            if(callingAdapter.getCount() == 0)  // No photos
                                finish();
                            updateWindow("onDataRetrieved(): refresh()");
                        }
                        else
                        {
                            PhotoSlideShowActivity.this.handlingError = true;
                            errDialog = createErrorDialog(callingAdapter.getErrorCode());
                            errDialog.show();
                        }
                    }
                }
            };

            this.photoAdapter = getAdapter(listener, null);

            slider.setSlideViewListener(new SliderViewListenerClass());
            slider.setAdapter(this.photoAdapter);
            slider.setSelection(this.currentPosition);
            photoAdapter.setReferencePosition(this.currentPosition);
            
            setUpActionView();
        }
        }
        catch (Throwable t)
        {
            slider.setSelection(this.currentPosition);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
    }
    
    protected void viewPhoto(int position, MozyFile mozyFile)
    {
        // Not needed had to create since we implement ContextMenuActivity
        return;
    }
    
    @Override
    public String getPlatform()
    {
        return this.platform;
    }

    protected void updateWindow(String comment)
    {
        LogUtil.debug("PhotoSlideShowActivity", "updateWindow: " + comment);
        if(photoAdapter != null && slider != null)
        {
            photoAdapter.setCurrentView(currentPosition, currentPosition);
        }
    }
    
    private final class SliderViewListenerClass implements SlideViewListener {
        @Override
        public void onNewItemSnapped(int position)
        {
            updateAlarm();

            currentPosition = position;
            photoAdapter.setReferencePosition(position);
            updateWindow("onNewItemSnapped()");
        }

        @Override
        public void onWrapAnimationStarted(boolean bEnd)
        {
            String strToasterMsg;
            if(bEnd)
            {
                strToasterMsg = getString(R.string.endoflist_reached);
            }
            else
            {
                strToasterMsg = getString(R.string.startoflist_reached);
            }
            Toast.makeText(PhotoSlideShowActivity.this, strToasterMsg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onMoving() {
            mActionView.notifyAction();
        }
        
        @Override
        public void onActionDown()
        {
            onMoving();
        }
    }
}
