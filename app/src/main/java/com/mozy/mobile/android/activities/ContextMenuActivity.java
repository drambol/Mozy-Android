package com.mozy.mobile.android.activities;

//import java.io.UnsupportedEncodingException;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import com.mozy.mobile.android.security.Cryptation;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.tasks.FileDecrypterTask;
import com.mozy.mobile.android.activities.tasks.SyzygyRestoreTask;
import com.mozy.mobile.android.activities.tasks.WriteFileTask;
import com.mozy.mobile.android.activities.upload.MozyUploadActivity;
import com.mozy.mobile.android.activities.upload.uploadFile;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.web.containers.ListDownload;

/*
 * Common functionality for the DirFileListActivity and the DirPhotoGridActivity
 */
public abstract class ContextMenuActivity  extends SecuredActivity  {

    protected static final int[] ERROR_IDS = new int[] {ErrorManager.ERROR_TYPE_GENERIC};  
    public static final int FIRST_USER_DIALOG = ErrorActivity.FIRST_USER_DIALOG + 1;

    public static final int PROGRESS_DOWNLOADING_ID = ContextMenuActivity.FIRST_USER_DIALOG;        // TODO: fix to have unique value
    public static final int DELETE_FILE_CONFIRMATION_ID = PROGRESS_DOWNLOADING_ID + 1;
    public static final int PRIVATE_KEY_NONE_MSG = DELETE_FILE_CONFIRMATION_ID + 1;
    public static final int PRIVATE_KEY_ENCRYPTED_ERROR_MSG = PRIVATE_KEY_NONE_MSG + 1;
    public static final int LAST_USER_DIALOG = PRIVATE_KEY_ENCRYPTED_ERROR_MSG + 1;
    
    public static final int DIALOG_LOADING_ID = LAST_USER_DIALOG + 1;
    public static final int DIALOG_ERROR_ID = LAST_USER_DIALOG + 2;
    public static final int DIALOG_NORETRY_ERROR_ID = ContextMenuActivity.LAST_USER_DIALOG + 3;
    public static final int DIALOG_DOWNLOAD_PROMPT = LAST_USER_DIALOG + 4;
    public static final int DIALOG_NO_RESULTS_ID = LAST_USER_DIALOG + 5;


    private final int CONTEXT_MENU_DOWNLOAD = 0;
    private final int CONTEXT_MENU_DELETE = 1;
    private final int CONTEXT_MENU_VIEW = 2;
    private final int CONTEXT_MENU_OPEN = 3;
    private final int CONTEXT_MENU_EXPORT = 4;
  

    private static final int SWIPE_MIN_DIST = 75; // Pixels
    private GestureDetector mSwipeDetector;
    CloudFile fileToDownload = null;

    private int filePosition = 0;
    private MozyFile fileToRemove = null;
    
    protected static boolean sGridMode;
    protected static String sLastFolder;

    // The following are returned by checkFileIntent() and indicate whether there is an intent that can handle
    // the remote file, a downloaded version of the file or no handler at all.
    protected static final int NO_INTENT_HANDLER = 0;
    protected static final int LOCAL_INTENT_HANDLER = 1;

    
    protected static final int CONFIG_CONTEXT_MENU_DOWNLOAD       = 0x0001;
    protected static final int CONFIG_CONTEXT_MENU_DELETE         = 0x0002;
    protected static final int CONFIG_CONTEXT_MENU_EXPORT          = 0x0004;
    
    protected int contextMenuFlags  = 0x0000;
    
    protected LocalFile currentDecryptedFile = null;
    
    /**
     * 
     */
    public int setContextMenuFlags(boolean fileDeleteEnabled) {
        
        int menuFlags = 0;
        
        if(fileDeleteEnabled)
        {
            menuFlags = CONFIG_CONTEXT_MENU_DOWNLOAD | CONFIG_CONTEXT_MENU_DELETE | CONFIG_CONTEXT_MENU_EXPORT ;
        }
        else
        {
            menuFlags = CONFIG_CONTEXT_MENU_DOWNLOAD | CONFIG_CONTEXT_MENU_EXPORT;
        }
        
        return menuFlags;
    }
   
   
    // Called from subclass
    protected void buildContextMenu(ContextMenu menu,
                                    View view,
                                    ContextMenu.ContextMenuInfo menuInfo,
                                    MozyFile mozyfile,
                                    boolean bDeviceEncrypted)
    {
        boolean bExportAdded = false;
        boolean bViewOrOpenOptionAdded = false;
        boolean bDownloadAdded = false;
        boolean bDeleteAdded =  false;
        
        if(mozyfile != null)
        {
            
            if(!(mozyfile instanceof Directory))
            {
                // User can only download, share or delete non-directory files
                if (mozyfile instanceof CloudFile)
                {
                    final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(this.getApplicationContext(), getRootDeviceTitle(), (CloudFile) mozyfile);
                    
                    if(localFile != null)  // No SD Card scenario
                    {
                        bExportAdded = addExportOption(menu, mozyfile);
                        bViewOrOpenOptionAdded = addViewOrOpenOption(menu, mozyfile);
                        bDownloadAdded = addDownloadOption(menu, mozyfile);
                    }

                   bDeleteAdded = addDeleteOption(menu);
                    
                    //set header if any options added
                    if(bExportAdded || bViewOrOpenOptionAdded || bDownloadAdded || bDeleteAdded)
                        menu.setHeaderTitle(R.string.file_options_title);
                } 
                else if (mozyfile instanceof LocalFile)
                {
                    bExportAdded = addExportOption(menu, mozyfile);
                    bViewOrOpenOptionAdded = addViewOrOpenOption(menu, mozyfile);
                    bDeleteAdded = addDeleteOption(menu);
                    
                    //set header if any options added
                    if(bExportAdded || bViewOrOpenOptionAdded || bDeleteAdded)
                        menu.setHeaderTitle(R.string.file_options_title);
                }
            }
            else
            {
                bViewOrOpenOptionAdded = addViewOrOpenOption(menu, mozyfile);
                //bDeleteAdded = addDeleteOption(menu);
                
                //set header if any options added
                if(bViewOrOpenOptionAdded)// || bDeleteAdded)
                    menu.setHeaderTitle(R.string.file_options_title);
            }
        }
     }

    /**
     * @param menu
     * @param mozyfile
     */
    protected boolean addViewOrOpenOption(ContextMenu menu, MozyFile mozyfile) {
        int intentType = ContextMenuActivity.NO_INTENT_HANDLER;
        boolean optionAdded = false;
        
        if (((mozyfile instanceof CloudFile)  && (mozyfile instanceof Photo))  || 
                (mozyfile instanceof LocalFile && FileUtils.isFileofType((LocalFile) mozyfile,FileUtils.CATEGORY_PHOTOS)))
        {
            menu.add(0, CONTEXT_MENU_VIEW, 0, this.getString(R.string.view_menu_label));
            optionAdded = true;
        }
        else if(!(mozyfile instanceof Directory))
        {
            // See if there any handlers for this file type
            intentType = this.getFileIntentType(mozyfile, Intent.ACTION_VIEW);
            
            if (intentType != ContextMenuActivity.NO_INTENT_HANDLER)
            {
                menu.add(0, CONTEXT_MENU_OPEN, 0, this.getString(R.string.open_menu_label));
                optionAdded = true;
            }
        }
        else
        {
            menu.add(0, CONTEXT_MENU_OPEN, 0, this.getString(R.string.open_menu_label));
            optionAdded = true;
        }
        
        return optionAdded;
    }

    /**
     * @param menu
     * @param mozyfile
     * @param intentType
     */
    protected boolean addExportOption(ContextMenu menu, MozyFile mozyfile) {
        
        boolean optionAdded = false;
        int intentType = ContextMenuActivity.NO_INTENT_HANDLER;
        
        if (SystemState.isExportEnabled()  && checkFlagSet(this.contextMenuFlags, CONFIG_CONTEXT_MENU_EXPORT))
        {
            // See if there any handlers for this file type
            if(mozyfile != null)
                intentType = this.getFileIntentType(mozyfile, Intent.ACTION_SEND);
            
            if (intentType != ContextMenuActivity.NO_INTENT_HANDLER)
            {
                menu.add(0, CONTEXT_MENU_EXPORT, 0, this.getString(R.string.share_menu_label));
                optionAdded = true;
            }
        }
        
        return optionAdded;
    }
    

    /**
     * @param menu
     */
    protected boolean addDeleteOption(ContextMenu menu) {
        boolean optionAdded = false;
        
        if (checkFlagSet(this.contextMenuFlags, CONFIG_CONTEXT_MENU_DELETE))
        {
            menu.add(0, CONTEXT_MENU_DELETE, 0, this.getString(R.string.menu_delete));
            optionAdded = true;
        }
        
        return optionAdded;
    }

    /**
     * @param menu
     * @param mozyfile
     */
    protected boolean addDownloadOption(ContextMenu menu, MozyFile mozyfile) {
        
        boolean fileExists = false;
        
        // check the file exists on card and same as in the cloud, disable download in such a case
        
        if(checkFlagSet(this.contextMenuFlags, CONFIG_CONTEXT_MENU_DOWNLOAD))   
        {
            if(mozyfile instanceof CloudFile)
            {
                fileExists = isCurrentCloudFileDownloadedOnSDCard(mozyfile);
                
                if(fileExists)
                    menu.add(0, CONTEXT_MENU_DOWNLOAD, 0, this.getString(R.string.menu_download)).setVisible(false);
                else
                    menu.add(0, CONTEXT_MENU_DOWNLOAD, 0, this.getString(R.string.menu_download)).setVisible(true);
            }
        }
        
        return (!fileExists); // we do not add download menu item option if file exists
    }

    /**
     * @param menu
     * @param mozyfile
     * @param optionAdded
     * @return
     */
    protected boolean isCurrentCloudFileDownloadedOnSDCard(MozyFile mozyfile) {
        String cloudFileLink = ((CloudFile) mozyfile).getLink();
        boolean status = false;
        
        final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(this.getApplicationContext(), getRootDeviceTitle(), (CloudFile) mozyfile);
         
        if(localFile != null)
         {
            long localFileTouched = localFile.file.lastModified();
            long cloudFileUpdated = ((CloudFile) mozyfile).getUpdated();
            

            // do not show download option, if file date matches
            if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB(this.getRootDeviceId(), localFile.getName()) &&
                    cloudFileLink.equalsIgnoreCase(SystemState.mozyFileDB.getCloudFileLink(this.getRootDeviceId(), localFile.getName()))
                    && localFileTouched == cloudFileUpdated)
            {
                status = true;
            }
            else
            {
                status = false;
            }
        }
        return status;
    }
  

    // Called from subclass
    protected void handleContextMenuItemSelection(MenuItem menuItem,
                                                  final MozyFile mozyFile,
                                                  final int position)
    {
        int menuItemId = menuItem.getItemId();
        // Clear the error handling state
        this.handlingError = false;

        switch (menuItemId)
        {
            case CONTEXT_MENU_DOWNLOAD:
                    if(mozyFile != null && mozyFile instanceof CloudFile)
                    {
                        final Runnable spinner = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Thread.yield();
                                ContextMenuActivity.this.downloadFile((CloudFile) mozyFile);
                            }
                        };
                        ContextMenuActivity.this.runOnUiThread(spinner);
                    }
                break;

            case CONTEXT_MENU_DELETE:
                if(mozyFile != null)
                {
                    this.filePosition = position;
                    this.fileToRemove = mozyFile;                    
                    this.showDialog(ContextMenuActivity.DELETE_FILE_CONFIRMATION_ID);
                }
                break;
            case CONTEXT_MENU_EXPORT:
                if(mozyFile != null)
                    exportFile(mozyFile);
                break;
            case CONTEXT_MENU_VIEW:
                    // Load the preview activity. We should not be able to get to this code unless it is a picture.
                    if (mozyFile != null && 
                       ((mozyFile instanceof CloudFile && mozyFile instanceof Photo) || 
                       (mozyFile instanceof LocalFile && FileUtils.isFilePhoto((LocalFile) mozyFile))))
                    {
                        viewPhotoFile(mozyFile, position);
                    }
                    break;
            case CONTEXT_MENU_OPEN:
                     if(mozyFile != null)
                     {
                         if(!(mozyFile instanceof Directory))
                         {
                            // We should not be able to get to this code unless there is a valid intent for this file
                            
                            int intentType = 0;
                            
                            intentType  = this.getFileIntentType(mozyFile, Intent.ACTION_VIEW);
                            if (intentType != ContextMenuActivity.NO_INTENT_HANDLER)
                            {
                              this.showMozyFile(mozyFile);        
                            }
                         }
                         else if((mozyFile instanceof Directory) && (mozyFile instanceof CloudFile))
                         {
                             this.browseDirectory((CloudFile)mozyFile);
                         }
                     }
                break;
            default:
                break;
        }
    }

    /**
     * @param mozyFile
     * @param position
     */
    protected void viewPhotoFile(final MozyFile mozyFile, final int position) {
        if(this.isDeviceEncrypted() == false)
        {
            this.viewPhoto(position, mozyFile);
        }
        else
        {
            this.showMozyFile(mozyFile);
        }
    }
    
    /**
     * @param cloudFile
     */
    public void showMozyFile(MozyFile mozyFile) {
        
        if(mozyFile != null && mozyFile instanceof CloudFile)
        {
             this.showCloudFile((CloudFile) mozyFile);
        }
        else if (mozyFile != null && mozyFile instanceof LocalFile)// Download file screen
        {
             this.showLocalFile((LocalFile) mozyFile);
        }
    }
    
    
    public void browseDirectory(CloudFile cloudFile)
    {
        
    }

    
    protected void exportFile(final MozyFile mozyFile)
    {
        if(mozyFile != null)
        {
            if(mozyFile instanceof CloudFile)
            {
                exportCloudFile((CloudFile) mozyFile);
            }
            else if(mozyFile instanceof LocalFile )
            {
                exportLocalFile((LocalFile) mozyFile);
            }
        }
    }

    /**
     * @param mozyFile
     */
    protected void exportLocalFile(final LocalFile localFile) {
        checkForLatestAndHandleLocalFile(localFile, Intent.ACTION_SEND);
        
    }

    /**
     * @param mozyFile
     * @param intentType
     */
    protected void exportCloudFile(final CloudFile cloudFile) {

        checkForLatestAndHandleCloudFile(cloudFile, Intent.ACTION_SEND);
    }
    
    
    
    protected void downloadFile(final CloudFile cloudFile)
    {
        final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(getApplicationContext(), this.getRootDeviceTitle(), cloudFile);
        boolean inSync = SystemState.isSync(getApplicationContext(), this.getRootDeviceTitle());
        
        if(cloudFile != null)
        {
            if(localFile != null)    
            {
                // Does the local file already exists in database
                
                if(localFile.file.exists() 
                        && (SystemState.mozyFileDB.existsFileInDB(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink()) == true))
                {
                    this.fileToDownload = cloudFile;
    
                    long localFileTouched = localFile.file.lastModified();
                    long cloudFileUpdated = cloudFile.getUpdated();
                    
                    if(localFileTouched != cloudFileUpdated)
                    {
                        WriteFileTask writeFileTask = new WriteFileTask( localFile, cloudFile, this, this.isDeviceEncrypted(), inSync,
                                new WriteFileTask.Listener() {
            
                            @Override
                            public void onWriteFileTaskCompleted(int status) {
                                if(status ==  ServerAPI.RESULT_INVALID_CLIENT_VER)
                                {
                                    ContextMenuActivity.this.handlingError = true;
                                    Dialog errDialog = createErrorDialog(status);
                                    errDialog.show();
                                }
                                return;
                            }
                          });
                        writeFileTask.execute();
                    }
                }
                else
                {
                    WriteFileTask writeFileTask = new WriteFileTask( localFile, cloudFile, this, this.isDeviceEncrypted(), inSync,
                            new WriteFileTask.Listener() {
        
                        @Override
                        public void onWriteFileTaskCompleted(int status) {
                            if(status ==  ServerAPI.RESULT_INVALID_CLIENT_VER)
                            {
                                ContextMenuActivity.this.handlingError = true;
                                Dialog errDialog = createErrorDialog(status);
                                errDialog.show();
                            }
                            return;
                        }
                      });
                    writeFileTask.execute();
                }
            }
            else
            {
              cloudFile.alertNoSDCard(this);
            }
        }
    }
    
    public void showLocalFile(final LocalFile localFile)
    {
        checkForLatestAndHandleLocalFile(localFile, Intent.ACTION_VIEW);
    }

    /**
     * @param localFile
     * @param action
     */
    protected void checkForLatestAndHandleLocalFile(final LocalFile localFile,
            final String action) {
        final CloudFile cloudFile;
        if (null != localFile)
        {
            // Note for existing downloaded files in database we would have no mapping to cloudfile
            
            if(localFile.file.exists()) 
            {
                ListDownload fileList =  ServerAPI.getInstance(getApplicationContext()).getCloudFileForLocalFile(this.getRootDeviceId(), localFile);
                
                if(fileList != null)
                {
                    if(fileList.list != null && fileList.list.size() != 0)
                        cloudFile = (CloudFile) fileList.list.get(0);
                    else
                        cloudFile = null;
                    
                    if(cloudFile != null) 
                    {   
                        this.fileToDownload = cloudFile;
                        
                        if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB (this.getRootDeviceId(), localFile.getName()) == true)
                        {
                            if ((fileList.errorCode != ServerAPI.RESULT_CONNECTION_FAILED))
                            {           

                                boolean inSync = SystemState.isSync(getApplicationContext(), this.getRootDeviceTitle());
                                
                                if(inSync) //if it is sync get the very latest, could have been updated by a different client
                                {
                                    long lastLocalFileCreated;
                                    long localFileDate;
                                    LocalFile decryptedUploadFile = null;
                                    LocalFile decryptedFile = null;
                                    
                                    if (isDeviceEncrypted()) {
                                    	decryptedFile = FileUtils.getDecryptedFileForLocalFile(this.getApplicationContext(), localFile);
                                    	
                                    	localFileDate = decryptedFile == null ? -1 : decryptedFile.file.lastModified();
                                    	lastLocalFileCreated = SystemState.mozyFileDB.getDecryptDateForFile(this.getRootDeviceId(), localFile.getName());
                                    } else {
                                    	localFileDate = localFile.file.lastModified();
                                    	lastLocalFileCreated = SystemState.mozyFileDB.getDateForLocalFile(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink());
                                    }
                                    
                                    if((localFileDate > lastLocalFileCreated) && action == Intent.ACTION_VIEW)
                                    {
                                    	if (decryptedFile != null) {
                                    		try {
                                    			decryptedUploadFile = FileUtils.createTempUploadFile(decryptedFile);
                                    		} catch (IOException ie) {
                                    			localFileDate = -1;
                                    		} catch (Exception e) {
                                    			localFileDate = -1;
                                    		}
                                    	}
                                        Dialog alertDlg = suggestUploadDialog(cloudFile, localFile, decryptedUploadFile, action);
                                        alertDlg.show();
                                    }
                                    else
                                    {
                                        checkAndHandleSyncFromCloudForDownloadedFile(localFile, action, cloudFile);
                                    }
                                }
                                else
                                {          
                                    checkAndHandleSyncFromCloudForDownloadedFile(localFile, action, cloudFile);
                                }
                            }
                            else // connection failed so can not check for latest
                            {
                                handleLocalFileForAction(localFile,action);
                            }
                         }
                         else   
                         {
                              // database not updated / redownload file 
                             downloadAndHandleFile(cloudFile, localFile, action);
                         }
                    }   
                    else   // no cloud file available
                    {
                        handleLocalFileForAction(localFile,action);
                    }
                }
                else  // no cloud file available
                {
                    handleLocalFileForAction(localFile,action); 
                }
            }
        }
    }

    /**
     * @param localFile
     * @param action
     * @param cloudFile
     */
    public void checkAndHandleSyncFromCloudForDownloadedFile(final LocalFile localFile, final String action, final CloudFile cloudFile) 
    {
        long cloudFileDate = cloudFile.getUpdated();
                           // last Downloaded is the time the local file is created
        long lastDownloaded  = SystemState.mozyFileDB.getDateForCloudFile(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink());
        
        if(lastDownloaded < cloudFileDate)
        {
            Dialog alertDlg = overWriteFileDialog(cloudFile, localFile, lastDownloaded, cloudFileDate, action);
            if(alertDlg != null) alertDlg.show();
        }
        else
        {
            handleLocalFileForAction(localFile,action);
        }
    }

    public void showCloudFile(final CloudFile cloudFile)
    {    
        checkForLatestAndHandleCloudFile(cloudFile, Intent.ACTION_VIEW);
    }

    /**
     * @param cloudFile
     * @param action
     */
    protected void checkForLatestAndHandleCloudFile(final CloudFile cloudFile,  final String action) 
    {
        if (null != cloudFile)
        {
            final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(getApplicationContext(), this.getRootDeviceTitle(), cloudFile);
            
            if(action != null)
            {
                int intentType = this.getFileIntentType(cloudFile, action);
                
                if(intentType == LOCAL_INTENT_HANDLER)
                {
                    if(localFile != null)    
                    {
                        // Does the local file already exists in database
                        
                        if(localFile.file != null && localFile.file.exists() && (SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink()) == true))
                        {
                            this.fileToDownload = cloudFile;
                            
                            boolean inSync = SystemState.isSync(getApplicationContext(), this.getRootDeviceTitle());

                            if(inSync) //if it is sync get the very latest, could have been updated by a different client
                            {
                                long localFileTouched;
                                long lastLocalFileCreated;
                                LocalFile decryptedUploadFile = null;
                                LocalFile decryptedFile = null;
                                
                                if (isDeviceEncrypted()) {
                                	decryptedFile = FileUtils.getDecryptedFileForLocalFile(this.getApplicationContext(), localFile);
                                	
                                	localFileTouched = decryptedFile == null ? -1 : decryptedFile.file.lastModified();
                                	lastLocalFileCreated = SystemState.mozyFileDB.getDecryptDateForFile(this.getRootDeviceId(), localFile.getName());
                                } else {
                                	localFileTouched = localFile.file.lastModified();
                                	lastLocalFileCreated = SystemState.mozyFileDB.getDateForLocalFile(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink());
                                }
                                
                                if((localFileTouched > lastLocalFileCreated) && action == Intent.ACTION_VIEW)
                                {
                                	if (decryptedFile != null) {
                                		try {
                                			decryptedUploadFile = FileUtils.createTempUploadFile(decryptedFile);
                                		} catch (IOException ie) {
                                			localFileTouched = -1;
                                		} catch (Exception e) {
                                			localFileTouched = -1;
                                		}
                                	}
                                    Dialog alertDlg = suggestUploadDialog(cloudFile, localFile, decryptedUploadFile, action);
                                    alertDlg.show();
                                }
                                else
                                {
                                    checkAndHandleForSyncFromCloud(cloudFile, action, localFile);
                                }
                            }
                            else
                            {
                                checkAndHandleForSyncFromCloud(cloudFile, action, localFile);  
                            }
                        }
                        else  // otherwise download file
                        {
                            downloadAndHandleFile(cloudFile, localFile, action);
                        }
                   }
                   else
                   {
                     cloudFile.alertNoSDCard(this);
                   }
                }
                else
                {      
                    String downLoadPath = FileUtils.getDownloadDirectoryPath(getApplicationContext(), cloudFile,  this.getRootDeviceTitle()) ;
                    
                    if(downLoadPath != null)
                    {
                        // No handlers for this file so prompt the user to see if they want to download this file.
                        // There is no way to pass parameters to the dialog creation code in this version of the SDK.
                        // So I have to do it thus way of just setting some member variables here and using them
                        // in the dialog creation code.
                        this.fileToDownload = cloudFile;
                        showDialog(DIALOG_DOWNLOAD_PROMPT);
                    }
                }
            }
        }
    }

    /**
     * @param cloudFile
     * @param action
     * @param localFile
     */
    public void checkAndHandleForSyncFromCloud(final CloudFile cloudFile, final String action, final LocalFile localFile) 
    {
        long cloudFileUpdated = cloudFile.getUpdated();
        
        ListDownload returnValue = ServerAPI.getInstance(getApplicationContext()).getCloudFileForFileLink(cloudFile.getLink()); //get the latest timestamp on cloudfile
        if(returnValue.list != null)
        {
            CloudFile latestCloudFile = (CloudFile) returnValue.list.get(0);
            cloudFileUpdated = latestCloudFile.getUpdated();
            
            cloudFile.setUpdated(cloudFileUpdated);  // update with the latest time stamp
            
            // last Downloaded is the time the local file is created
            long lastDownloaded  = SystemState.mozyFileDB.getDateForCloudFile(this.getRootDeviceId(), localFile.getName(), cloudFile.getLink());
            
            // lastDownloaded is set to -1 when we had edited and uploaded the file and pulling it back from the cloud
            if(lastDownloaded != cloudFileUpdated && lastDownloaded != -1)
            {
                Dialog alertDlg = overWriteFileDialog(cloudFile, localFile, lastDownloaded, cloudFileUpdated, action);
                if(alertDlg != null) alertDlg.show();
            }
            else 
            {
                if(lastDownloaded == -1)
                {
                    // update database with new cloudfile date and download it even though it is possible that we have the same file locally.
                    // Since it is possible that another device might have updated
                    if(SystemState.mozyFileDB != null)
                        SystemState.mozyFileDB.insertOrUpdateDownloadedFileinDB(this.getRootDeviceId(), cloudFile, localFile, null);
                    downloadAndHandleFile(cloudFile, localFile, action);
                }
                else
                {
                    handleLocalFileForAction(localFile,action);  
                }
            }
        }
        else  // do not bother if nothing in the list
            handleLocalFileForAction(localFile,action);   
    }

    /**
     * @param cloudFile
     * @param localFile
     * @param action
     */
    protected void downloadAndHandleFile(final CloudFile cloudFile,
            final LocalFile localFile, final String action) {
    	boolean inSync = SystemState.isSync(getApplicationContext(), this.getRootDeviceTitle());
        WriteFileTask writeFileTask = new WriteFileTask( localFile, cloudFile, this, this.isDeviceEncrypted(), inSync,
                new WriteFileTask.Listener() {
         
            @Override
            public void onWriteFileTaskCompleted(int status) {
                if(status ==  ServerAPI.RESULT_INVALID_CLIENT_VER)
                { 
                    ContextMenuActivity.this.handlingError = true;
                    Dialog errDialog = createErrorDialog(status);
                    errDialog.show();
                }
                else if(status == ServerAPI.RESULT_OK)
                    handleLocalFileForAction(localFile,action); 
            }
          });
        writeFileTask.execute();
    }

    /**
     * @param cloudFile
     * @param localFile
     * @param action
     */
    protected void handleLocalFileForAction(final LocalFile localFile, final String action) {
        if(localFile != null)
        {
        	if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB(this.getRootDeviceId(), localFile.getName())) {
        		String encodingType = SystemState.mozyFileDB.getEncodingTypeForFile(this.getRootDeviceId(), localFile.getName());
        		
        		if (encodingType.equalsIgnoreCase("x-syzygy")) {
        			encryptedLocalFileForAction(localFile, action, true);
        		} else if (encodingType.equalsIgnoreCase("x-ciphertext")) { 
        			encryptedLocalFileForAction(localFile, action, false);;
        		} else {
        			decryptedLocalFileForAction(localFile,action);
        		}
        	}
        	/*
            if(this.isDeviceEncrypted() == true)
            {
                encryptedLocalFileForAction(localFile, action);
            }
            else
            {
                decryptedLocalFileForAction(localFile,action); 
            }
            */
        }
    }



    @Override
    // Unfortunately the version of the SDK that we are using does not support passing extra data to onCreateDialog()
    protected Dialog onCreateDialog(int id) {

        Dialog returnValue = null;
        
        if (id == ContextMenuActivity.PROGRESS_DOWNLOADING_ID)
        {
                 // I tried displaying the ProgressDialog here, but it would never actually show.
        }
        else if (id == ContextMenuActivity.DELETE_FILE_CONFIRMATION_ID)
        {
            returnValue = deleteFileConfirmDialog();
        }
        else if (id == ContextMenuActivity.PRIVATE_KEY_NONE_MSG)
        {
            if(SystemState.isManagedKeyEnabled(this.getApplicationContext()) == false)
            {
                returnValue = decryptionErrorDialog(R.string.private_key_none_message, id);
            }
            else
            {
                MainSettingsActivity.signOff(ContextMenuActivity.this);
            }
        }
        else if (id == ContextMenuActivity.PRIVATE_KEY_ENCRYPTED_ERROR_MSG)
        {
            if(SystemState.isManagedKeyEnabled(this.getApplicationContext()) == false)
            {
                returnValue = decryptionErrorDialog(R.string.private_key_encrypted_file_decrypt_fail_message, id);
            }
            else
            {
                AlertDialog alertDialog = new AlertDialog.Builder(ContextMenuActivity.this)
                .setCancelable(false)
                .setTitle(R.string.cannot_view_file)
                .setMessage(R.string.managed_key_failed_to_decrypt)
                .setIcon(getResources().getDrawable(R.drawable.error_icon))
                .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    } })
                .create();
                alertDialog.show();   
            }
        }
        else
        {
            returnValue = super.onCreateDialog(id);
        }

        return returnValue;
    }

    private Dialog deleteFileConfirmDialog() {
        
        
        Dialog returnValue = null;
        String promptString = null;
        String title = null;
        
        if( this.fileToRemove instanceof LocalFile)
        {
            title = this.getString(R.string.delete_local_file_title);
            promptString = this.getString(R.string.delete_sdcard_file_body);
            promptString = promptString.replace("$FILENAME", this.fileToRemove.getName());
        }
        else
        {
            promptString = this.getString(R.string.delete_confirmation);
            promptString = promptString.replace("$FILENAME", this.fileToRemove.getName());
        }
        
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(promptString);
        builder.setCancelable(false);
        builder.setPositiveButton(this.getString(R.string.yes_button_text), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                ContextMenuActivity.this.removeItem(ContextMenuActivity.this.filePosition, ContextMenuActivity.this.fileToRemove);
                ContextMenuActivity.this.removeDialog(DELETE_FILE_CONFIRMATION_ID);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
               // dialog.cancel();
                dialog.dismiss();
                ContextMenuActivity.this.removeDialog(DELETE_FILE_CONFIRMATION_ID);
             }
         });

       returnValue = builder.create();
       return returnValue;
   }

/**
 * @param returnValue
 * @return
 */
protected Dialog overWriteFileDialog(CloudFile cloudFile, LocalFile localFile, long deviceTimeStamp, long cloudTimeStamp, String action) {
    Dialog returnValue = null;
   
    if(localFile != null)
    {
        if(cloudFile != null)
        {
            String promptString = this.getString(R.string.cloud_file_newer);
            if (deviceTimeStamp > cloudTimeStamp)
            {
               promptString = this.getString(R.string.phone_file_newer);
            }
            
             returnValue = checkForLatestDialog(cloudFile, localFile, null, promptString, action);
        }
    }
    else
    {
        cloudFile.alertNoSDCard(this);
    }
    return returnValue;
   }

/**
 * @param localFile
 * @param localFileTouched
 * @param cloudFileUpdated
 * @return
 */
protected Dialog checkForLatestDialog(final CloudFile cloudFile, final LocalFile localFile, String titleString, String promptString,
        final String action) {
    Dialog returnValue;
    
     AlertDialog.Builder builder = new AlertDialog.Builder(this);
     builder.setTitle(titleString);
     builder.setMessage(promptString);
     builder.setCancelable(false);
     builder.setPositiveButton(this.getString(R.string.yes_button_text), new DialogInterface.OnClickListener()
     {
         public void onClick(DialogInterface dialog, int id)
         {
             localFile.delete();
             dialog.dismiss();
             Thread.yield();
             ContextMenuActivity.this.downloadAndHandleFile(cloudFile, localFile, action);
          }
      });
      builder.setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
      {
          public void onClick(DialogInterface dialog, int id)
          {
             // dialog.cancel();
             dialog.dismiss();
             handleLocalFileForAction(localFile,action); 
           }
        });
      
        returnValue = builder.create();
    return returnValue;
}


protected Dialog suggestUploadDialog(final CloudFile cloudFile, final LocalFile localFile, final LocalFile decryptedFile, final String action) {
    Dialog returnValue;
    String promptString = this.getString(R.string.upload_sync_newer_file_on_device);
     AlertDialog.Builder builder = new AlertDialog.Builder(this);
     builder.setMessage(promptString);
     builder.setCancelable(false);
     builder.setPositiveButton(this.getString(R.string.menu_upload), new DialogInterface.OnClickListener()
     {
         public void onClick(DialogInterface dialog, int id)
         {
            final Runnable spinner = new Runnable()
            {
              @Override
                public void run()
                {
                  ArrayList <uploadFile> uploadLocalFile = new ArrayList <uploadFile>();
                  String mimeType = FileUtils.getMimeTypeFromFileName(localFile.getName());
                  
                  String destPath = cloudFile.getPath();
                  
                  if(destPath != null)
                  {
                      if(destPath.equalsIgnoreCase(File.separator) == false)
                      {
                          int len  = destPath.length();
                        //remove leading slash
                          destPath = destPath.substring(1, len);
                      }
                  }
                  
                  if (decryptedFile == null)
                	  uploadLocalFile.add(new uploadFile(localFile.getPath(), mimeType, destPath));
                  else
                	  uploadLocalFile.add(new uploadFile(decryptedFile.getPath(), mimeType, destPath));
                  MozyUploadActivity.queueForUpload(uploadLocalFile);
                  
                  localFile.file.setLastModified(decryptedFile.file.lastModified());
                  //update the db with the last modified the date on local file
                  SystemState.mozyFileDB.insertOrUpdateUploadedFileinDB( (ContextMenuActivity.this).getRootDeviceId(), cloudFile, localFile, decryptedFile);
                 }
              };
              ContextMenuActivity.this.runOnUiThread(spinner);
              handleLocalFileForAction(localFile,action); 
          }
      });
     builder.setNeutralButton(this.getString(R.string.replace_button_text), new DialogInterface.OnClickListener()
     {
         public void onClick(DialogInterface dialog, int id)
         {
             localFile.delete();
             dialog.dismiss();
             Thread.yield();
             ContextMenuActivity.this.downloadAndHandleFile(cloudFile, localFile, action);
         }
     });
      builder.setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
      {
          public void onClick(DialogInterface dialog, int id)
          {
              handleLocalFileForAction(localFile,action); 
          }
        });
      
        returnValue = builder.create();
    return returnValue;
}

private Dialog decryptionErrorDialog(int resId, final int dialogId) {
     String errorString = this.getString(resId);
     AlertDialog.Builder builder = new AlertDialog.Builder(this);
     builder.setTitle(R.string.private_key_enter);
     builder.setIcon(getResources().getDrawable(R.drawable.error_icon));
     builder.setMessage(errorString);
     builder.setCancelable(false);
     builder.setPositiveButton(this.getString(R.string.yes_button_text), new DialogInterface.OnClickListener()
     {
          public void onClick(DialogInterface dialog, int id)
          {
             ContextMenuActivity.this.removeDialog(dialogId);
             dialog.dismiss();
             Intent intent = new Intent(getApplicationContext(), PersonalKeysSettingsActivity.class);
             startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
          }
     });
     builder.setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
     {
        public void onClick(DialogInterface dialog, int id)
        {
            ContextMenuActivity.this.removeDialog(dialogId);
            dialog.dismiss();
        }
      });
      
      Dialog returnValue = builder.create();
      return returnValue;
}

    /**
	 * @param localFile
	 * @param successful
	 * @return
	 */
private File encryptedLocalFileForAction(LocalFile localFile, final String action, boolean syzygy) 
{
    
    File decryptedFile = null;
    String passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(this.getRootDeviceId());
    
    if((passphrase != null && passphrase.length() != 0) || SystemState.isManagedKeyEnabled(getApplicationContext()) == true)
    {
    	//boolean inSync = SystemState.isSync(getApplicationContext(), this.getRootDeviceTitle());
    	
    	if (syzygy) {
    		new SyzygyRestoreTask(
    			localFile, this, this.getRootDeviceId(), //this.getPlatform(),
    			new SyzygyRestoreTask.Listener() {
            		@Override
            		public void onRestoreTaskCompleted(LocalFile outputFile) {   
            			if(outputFile != null && outputFile.file != null) {
            				ContextMenuActivity.this.currentDecryptedFile = outputFile;
            				decryptedLocalFileForAction(outputFile, action);
            			}
            		}
    			}
    		).execute();
    	} else {
    		new FileDecrypterTask(
                localFile, this, this.getRootDeviceId(), this.getPlatform(),
                new FileDecrypterTask.Listener() {

                	@Override
                	public void onDecryptionTaskCompleted(LocalFile outputFile) 
                	{   
                		if(outputFile != null && outputFile.file != null)
                		{
                			ContextMenuActivity.this.currentDecryptedFile = outputFile;
                			decryptedLocalFileForAction(outputFile, action);
                		}
                	}
                }).execute();
    	}
    }
    else
    {
        this.showDialog(ContextMenuActivity.PRIVATE_KEY_NONE_MSG);
    }
    
    return decryptedFile;
}

    protected int getFileIntentType(MozyFile mozyfile, String action)
    {
        if(mozyfile instanceof CloudFile )
        {
            return getFileIntentType((CloudFile) mozyfile, action);
        }
        else if(mozyfile instanceof LocalFile )
        {
            return getFileIntentType((LocalFile) mozyfile, action);
        }
         return -1;   
    }
    

    private int getFileIntentType(CloudFile cloudFile, String action)
    {
        int returnValue = ContextMenuActivity.NO_INTENT_HANDLER;

        // The interesting thing about the below three lines of code, is that if they were replaced with the following:
        //         Uri uri = Uri.parse("http://" + cloudFile.getTitle());
        //         Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        //         intent.setType(cloudFile.getMimeType());
        // Then there would be no activities found for the intent.
        // I don't understand what difference it should make, I think it's a bug in the SDK
        /*Intent myIntent = new Intent(action);

        // TEST Uri uri = Uri.parse("http://" + cloudFile.getTitle());
        // Unfortunately, the standard music player is happy to stream from an "http" URI but will not stream from
        // and "https" URI which is what MIP shared links are.
        Uri uri = Uri.parse("https://" + cloudFile.getTitle());
        myIntent.setDataAndType(uri, cloudFile.getMimeType());*/

        /*List<ResolveInfo> apps = manager.queryIntentActivities(myIntent, 0);
        int i = apps.size();*/
        
        String downloadPath = FileUtils.getDownloadDirectoryPath(getApplicationContext(),cloudFile, this.getRootDeviceTitle());
        
        if(downloadPath != null)
        {
            
           String filePath = "file://" + downloadPath  + "/"+ cloudFile.getTitle(); // "\sdcard\Mozy\Downloads\"

            // Check if there is any handler for a local version of the file.
            Intent myIntent = new Intent(action);
            Uri uri = Uri.parse(filePath);
            myIntent.setDataAndType(uri, cloudFile.getMimeType());

            // We found a handler for a local version of the file, now download it and view it.
            returnValue = getAvailableIntentType(action, myIntent, Intent.ACTION_SEND);
        }
        else
        {
            cloudFile.alertNoSDCard(this);
        }

        return returnValue;

    }
    
    
    protected int getFileIntentType(LocalFile downloadFile, String action)
    {
        int returnValue = ContextMenuActivity.NO_INTENT_HANDLER;        
        
        if(downloadFile != null && downloadFile.file != null)
        {
            String downloadPath = downloadFile.file.getParent();
            
            if(downloadPath != null)
            {
                
               String filePath = "file://" + downloadPath  + "/"+ downloadFile.getName(); // "\sdcard\Mozy\Downloads\"
    
                // Check if there is any handler for a local version of the file.
                Intent myIntent = new Intent(action);
                Uri uri = Uri.parse(filePath);
                myIntent.setDataAndType(uri, FileUtils.getMimeTypeFromFileName(downloadFile.getName()));
    
                // We found a handler for a local version of the file, now download it and view it.
                returnValue = getAvailableIntentType(action, myIntent, Intent.ACTION_SEND);
            }
        }

        return returnValue;
    }

    private int getAvailableIntentType(String action, Intent intent, String intentType)
    {
        int retVal = ContextMenuActivity.NO_INTENT_HANDLER;
        int i;
        List<ResolveInfo> apps;
        PackageManager manager = getPackageManager();

        apps = manager.queryIntentActivities(intent, 0);
        i = apps.size();

        if (action.equals(intentType))
        {
            for (int x = 0; x < i; x++)
            {
                ResolveInfo app = apps.get(x);
                String packageName = app.activityInfo.packageName;

                try {
                    // Always Filter Mozy here
                    if (manager.getPackageInfo(getPackageName(), 0).packageName.equals(packageName))
                        i--;
                    else if ((intent.getType() != null && !intent.getType().startsWith("image/")) && SendChooserActivity.FACEBOOK_PACKAGE_NAME.equals(packageName))
                        i--;
                }
                catch (NameNotFoundException e) {
                }
            }
        }

        if (i > 0)
            retVal = ContextMenuActivity.LOCAL_INTENT_HANDLER;

        return retVal;
    }


    // Error handling code

    @Override
    protected int[] getErrorIds()
    {
        return ERROR_IDS;
    }

    @Override
    protected int handleError(int errorCode, int errorType)
    {
        if (this.handlingError)
        {
            switch (errorCode)
            {
                case ServerAPI.RESULT_CONNECTION_FAILED:
                case ServerAPI.RESULT_UNAUTHORIZED:
                case ServerAPI.RESULT_FORBIDDEN:
                case ServerAPI.RESULT_UNKNOWN_PARSER:
                case ServerAPI.RESULT_UNKNOWN_ERROR:
                    return ACTION_DIALOG_ERROR;
            }
        }

        return ACTION_NONE;
    }


   
    @Override
    // Don't need to implement this because all dialogs displayed here are just warning dialogs
    protected void onButtonClick(int dialogId, int buttonId)
    {
        super.onButtonClick(dialogId, buttonId);
        
        // Wipe Mozy data and signout 
        if(dialogId == ErrorActivity.UNAUTHORIZED_USER_DIALOG)
        {
            MainSettingsActivity.remoteWipeAndSignOff(this);  
        }
        
    }

 

    public abstract String getRootDeviceId();
    public abstract String getRootDeviceTitle();
    public abstract boolean isDeviceEncrypted();

    public abstract String getPlatform();;
    protected abstract void removeItem(int position, MozyFile mozyFile);
    protected abstract void viewPhoto(int position, MozyFile mozyFile);

    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        enableSwipeDetection();
    }
    
    @Override
    public void onResume() 
    {
        super.onResume();
    }

    public void enableSwipeDetection()
    {
        if(mSwipeDetector == null)
        {
            mSwipeDetector = new GestureDetector(new SwipeDetector());
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if(mSwipeDetector.onTouchEvent(event))
        {
            return true;
        }
        return false;
    }

    protected GestureDetector getSwipeDetector()
    {
        return mSwipeDetector;
    }

    /**
     * GestureListener that detects swipe navigation gestures.
     */
    public class SwipeDetector extends SimpleOnGestureListener
    {
        @Override
        public boolean onFling(MotionEvent start, MotionEvent stop, float velocityX, float velocityY)
        {
            float xDist = start.getX() - stop.getX();
            float yDist = start.getY() - stop.getY();

            LogUtil.debug("ContextMenuActivity", "Swipe detected: (" + xDist + ", " + yDist + ") (" + velocityX + ", " + velocityY + ")");

            /*
             * If swipe is at least SWIPE_MIN_DIST pixels long, in the
             * correct direction (negative), and at least twice as long
             * horizontally as it is vertically, we consider this a valid
             * swipe for issuing a "BACK" event.
             */
            if(xDist < -SWIPE_MIN_DIST && Math.abs(xDist) > (Math.abs(yDist) * 2))
            {
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                return true;
            }
            return false;
        }
    }
    
    
    private boolean checkFlagSet(int config, int flag)
    {
       return ((config & flag) == flag);
    }
    

    private void getChooser(Intent intent)
    {
        if (Intent.ACTION_SEND.equals(intent.getAction()))
        {
            // Launch our own custom launcher here
            Intent intentChooser = new Intent(ContextMenuActivity.this, SendChooserActivity.class);
            intentChooser.putExtra("sendIntent", intent);
            startActivity(intentChooser);
        }
        else
            this.startActivity(Intent.createChooser(intent, null));        
    }

    /**
     * @param cloudFile
     * @param action
     * @param localFile
     */
    protected void decryptedLocalFileForAction(final LocalFile localFile, final String action) 
    {
        if(localFile != null)
        {
            int intentType = this.getFileIntentType(localFile, action);
            
            String mimeType = FileUtils.getMimeTypeFromFileName(localFile.getName());
            
            if (intentType != ContextMenuActivity.NO_INTENT_HANDLER && mimeType != null)
            { 
                Intent intent = new Intent(action);
                String downLoadPath = localFile.file.getParent();
                 String filePath = downLoadPath +"/"  + Uri.encode(localFile.getName()); // "\sdcard\Mozy\Downloads\"
                 Uri uri = Uri.parse("file://" + filePath);
                 
                 
                 if (action.equalsIgnoreCase(Intent.ACTION_VIEW))
                 {
                      intent.setDataAndType(uri, mimeType);
                }
                else
                {
                     intent.setType(mimeType);
                     intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                }
                if (intent != null)
                {
                    getChooser(intent);
                }
            }
        }
    }
}

