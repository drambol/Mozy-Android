package com.mozy.mobile.android.activities.upload;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.upload.UploadStatusActivity.ResponseReceiver;
import com.mozy.mobile.android.catch_release.CRResultCodes;
import com.mozy.mobile.android.catch_release.queue.Queue;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.uploadFileAPI;
import com.mozy.mobile.android.web.containers.FileUploadState;
import com.mozy.mobile.android.web.containers.SyzygyFileUploadState;
import com.mozy.mobile.android.web.containers.ListDownload;
import com.mozy.mobile.android.web.containers.StringDownload;
import com.mozy.mobile.android.web.containers.ProgressOutputStream.ProgressListener;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.security.SyzygyVbiAPI;

public class UploadWorker implements Runnable {
    private static final String TAG = UploadWorker.class.getSimpleName();

    static final int PRIORITY_NORMAL = 0;
    static final int PRIORITY_LOW = 1;

    private static final int SLEEP_ERROR_NORMAL_PRIORITY = 60 * 1000;

    private Thread thread;
    private boolean cancelled;
    public Queue queueCR;
    private Queue queueManual;
    private Queue queueFailedUploads;
    private Context context;
    private ServerAPI.ListenerWithObject callbackListener;
    private int resultCode;

    private final Object callbackLock;
    private final Object connectionLockManual;
    private final Object connectionLockCR;
    private final Object connectionLockAirplaneModeOn;
    private final Object connectionLockPaused;
    private FileUploadState fileState;
    private boolean is_wifi_only;
    private boolean is_roaming_allowed;
    private ConnectivityBroadcastReceiver networkReceiver;
    private boolean uploading;
    private boolean fileQueuedManual;
    private boolean fileQueuedCR;
    private boolean fileQueuedFailedUploads;
    private UploadNotification notif = null;
    
    private ConnectivityManager cm = null;
    private NetworkInfo info = null;
    private boolean airplaneModeOn = false;
    
    private Queue.Listener listener = null;
    

  
    public Queue.Listener getListener() {
        return listener;
    }

    /**
     * Connectivity broadcast receiver for connection broadcasts.
     *
     */
    private class ConnectivityBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if(action != null && ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(action))
            {
                airplaneModeOn = SystemState.isAirplaneModeOn(context);
            }

            if (action != null && action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                UploadWorker.this.cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
                
                if(UploadWorker.this.cm != null)
                {
                    UploadWorker.this.info = cm.getActiveNetworkInfo();
                }
                else
                {
                    LogUtil.debug(this, "Connectivity Manager returned null");
                }
                
                synchronized (connectionLockAirplaneModeOn)
                {
                    if(airplaneModeOn == false)
                    {
                        connectionLockAirplaneModeOn.notifyAll();
                    }
                }

                
               /* - Check for available wifi connection.
                - If no wifi available, examine state of "only on wifi" flag.
                - If "only on wifi" false, check for available cellular connection.
                - If cellular connection available, check the state of the "never when roaming" flag.
                - If "never when roaming" is true, check roaming status of the cellular connection.
                - If roaming postpone upload, else upload over cellular.*/
                
                boolean notify = false;
                synchronized (connectionLockCR) {
                    if (info != null && info.isConnected()) {

                        if (info.getType() == ConnectivityManager.TYPE_WIFI)  // Mobile will Always try Wifi first anyway as its free anywhere, if wifi fails, will try mobile UNLESS Wifi only. 
                        {
                            notify = true;
                        }
                        else if (!is_wifi_only) 
                        {
                            if (is_roaming_allowed || !info.isRoaming()) {
                                notify = true;
                            }
                        }

                    }
                    if (notify) {
                        connectionLockCR.notifyAll();
                    }
                }
                
                synchronized (connectionLockManual) {
                    if (info != null && info.isConnected()) 
                    {
                        connectionLockManual.notifyAll();
                        synchronized (connectionLockCR)
                        {
                            connectionLockCR.notifyAll();  // Notify the CR waiting job as well so that if waiting it can allow manual uploads
                        }
                    }
                }
                
                UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
            }
        }
    };


    UploadWorker(Context context, UploadNotification notif, Queue queueCR,  Queue queueManual, Queue queueFailedUploads) {
        
        /**
         * Default settings
         */
        this.uploading = false;
        this.fileQueuedManual = false;
        this.fileQueuedCR = false;
        this.fileQueuedFailedUploads = false;
       
        this.is_wifi_only = false;
        this.is_roaming_allowed = false;
        this.notif = notif;
        this.queueManual = queueManual;
        this.queueCR = queueCR;
        this.queueFailedUploads = queueFailedUploads;
        this.context = context;
        this.callbackLock = new Object();
        this.connectionLockCR = new Object();
        this.connectionLockManual = new Object();
        this.connectionLockAirplaneModeOn = new Object();
        this.connectionLockPaused = new Object();
        this.fileState = null;
        
        this.cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(this.cm != null)
            this.info = this.cm.getActiveNetworkInfo();
        else
        {
            LogUtil.debug(this, "Connectivity Manager returned null");
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        this.networkReceiver = new ConnectivityBroadcastReceiver();
        context.registerReceiver(networkReceiver, filter);
        this.callbackListener = new ServerAPI.ListenerWithObject()
        {
            @Override
            public void onResult(int result, Object data) {
                switch (result) {
                case ServerAPI.RESULT_CONNECTION_FAILED:
                    resultCode = CRResultCodes.CR_ERROR_CONNECTION_FAILED;
                    break;
                case ServerAPI.RESULT_OK:
                    resultCode = CRResultCodes.CR_RESULT_OK;
                    break;
                case ServerAPI.RESULT_UNAUTHORIZED:
                    resultCode = CRResultCodes.CR_ERROR_UNAUTHORIZED;
                    break;
                case ServerAPI.RESULT_FORBIDDEN:
                    resultCode = CRResultCodes.CR_ERROR_EXCEEDED_QUOTA;
                    break;
                case ServerAPI.RESULT_INVALID_CLIENT_VER:
                    resultCode = CRResultCodes.CR_ERROR_INVALID_VER;
                    break;
                case ServerAPI.RESULT_INVALID_TOKEN:
                    resultCode = CRResultCodes.CR_ERROR_INVALID_TOKEN;
                    break;
                case ServerAPI.RESULT_UNKNOWN_PARSER:
                    resultCode = CRResultCodes.CR_ERROR_UNKNOWN_RESPONSE_PARSER;
                    break;
                default:
                    resultCode = CRResultCodes.CR_ERROR_SERVER_GENERIC;
                }
                
                if(resultCode != ServerAPI.RESULT_OK)
                {
                    // last failed error code does not match than categorize the errors as generic in the failed upload queue
                    if(UploadManager.lastFailedErrorCodeForUpload  != 0)
                    {
                        if(resultCode != UploadManager.lastFailedErrorCodeForUpload)
                            UploadManager.lastFailedErrorCodeForUpload = CRResultCodes.CR_ERROR_SERVER_GENERIC;  
                    }    
                    else
                        UploadManager.lastFailedErrorCodeForUpload = resultCode;
                }
                
                LogUtil.debug(this, "OnResult()  result: " + result + "code:  " + resultCode);
                
                synchronized (callbackLock) {
                    uploading = false;
                    callbackLock.notifyAll();
                }
            }
        };
        this.resultCode = CRResultCodes.CR_RESULT_OK;
        
        if (SystemState.isSyncEnabled())
        {
            this.thread = new Thread(this);
            this.cancelled = false;
            this.thread.start();
        }
    }

    public void setWifiOnly(boolean active) {
        LogUtil.debug(TAG, "Setting WIFI_ONLY = " + active);
        synchronized (connectionLockCR) {
            this.is_wifi_only = active;
            connectionLockCR.notifyAll();
        }
    }

    public void setAllowRoaming(boolean allow) {
        LogUtil.debug(TAG, "Setting ALLOW_ROAMING = " + allow);
        synchronized (connectionLockCR) {
            this.is_roaming_allowed = allow;
            connectionLockCR.notifyAll();
        }
    }

    public void cancel() {
        cancelled = true;
        notifyThis();
    }

    private void notifyThis() {
        synchronized (this) {
            notifyAll();
        }
    }
    

    @Override
    public void run() {
        
        listener = new Queue.Listener() {

            @Override
            public void onContentChanged(int uploadType) {
                if(uploadType == UploadManager.CATCH_AND_RELEASE_UPLOAD)
                {
                    fileQueuedCR = true;
                    
                    // Check if we are waiting for wifi or on no roaming update notification on queuing new file
                    // taken care by ongoing uploads when we are not blocked by any of the above constraints
                    
                    UploadWorker.this.info = cm.getActiveNetworkInfo();
                    if (info != null && info.isConnected()) {
                        
                        if (info.getType() != ConnectivityManager.TYPE_WIFI && (is_roaming_allowed || !info.isRoaming())) 
                        {
                            if (is_wifi_only) 
                            {
                                UploadNotification.clearNotificationOnComplete(context);  // Need to make sure no complete notification at this point
                                notif.fireNotificationOnNoWifiOrRoaming(context, R.string.upload_no_wifi_notification_body);
                            }
                        }
                        else if(info.getType() != ConnectivityManager.TYPE_WIFI)
                        {
                            UploadNotification.clearNotificationOnComplete(context);  // Need to make sure no complete notification at this point
                             notif.fireNotificationOnNoWifiOrRoaming(context, R.string.upload_no_roaming_notification_body);
                        }
                    }
                }
                else if(uploadType == UploadManager.MANUAL_UPLOAD)
                {
                    fileQueuedManual = true;
                    // need to notify at this point if Catch and Release is blocking to make way for upload job
                    synchronized (connectionLockCR)
                    {
                        connectionLockCR.notifyAll();
                    }
                }
                else if(uploadType == UploadManager.FAILED_UPLOAD)
                {
                    fileQueuedFailedUploads = true;
                }
                
                notifyThis();
            }
            
            @Override
            public void onContentRemoved(int uploadType) {
                notifyThis();
            }
        };
        
        registerQueuesForListener(listener);
        
           
        while (!cancelled) 
        {
            // Already initialized
            if((UploadManager.sCurrentSettings != null && UploadManager.sCurrentSettings.getUploadInitialized()) 
                    && queueCR != null
                    && (queueCR.getQueueSize() > 0))
            {
                fileQueuedCR = true;
            }
            else
            {
                fileQueuedCR = false;
            }
            
            // Are there any manual jobs in the queue already , usually we are here when recovering from a crash
            
            if((queueManual != null && queueManual.getQueueSize() > 0))
            {
                fileQueuedManual = true;
            }
            else
            {
                fileQueuedManual = false;
            }
            
            if(UploadManager.failedUploadQueueSizeOnRetry > 0 && queueFailedUploads != null && queueFailedUploads.getQueueSize() > 0)
            {
                fileQueuedFailedUploads = true;
            }
            else
            {
                fileQueuedFailedUploads = false;
            }
            
            if(fileQueuedFailedUploads)
                handleRetryForFailedUpload();

            if(fileQueuedManual)
                handleManualUpload();  // Manual Upload take precedence   
            if(fileQueuedCR)
                handleCRUpload();
            
        
            handleUploadNotifOnSuccess();
            
            
            // update the upload status
            UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
            
            // Reset the count and size of successful uploads
            
            UploadManager.numFilesUploadedSuccessfully = 0;
            UploadManager.sizeFilesUploadedSuccessfully = 0;

            
            synchronized (this) {
                try {
                    if (!fileQueuedManual && !fileQueuedCR && !fileQueuedFailedUploads)
                        this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
      
        
        // reset our notification information
        notif.resetNotification();
        
        unRegisterQueuesForListener(listener);
    }

    /**
     * @param listener
     */
    public void unRegisterQueuesForListener(Queue.Listener listener) 
    {
        if(queueCR != null)
            queueCR.unregisterListener(listener);
        if(queueManual != null)
            queueManual.unregisterListener(listener);
        if(queueFailedUploads != null)
            queueFailedUploads.unregisterListener(listener);
    }

    /**
     * @param listener
     */
    public void registerQueuesForListener(Queue.Listener listener) {
        if(queueCR != null)
            queueCR.registerListener(listener);
        
        if(queueManual != null)
            queueManual.registerListener(listener);
        
        if(queueFailedUploads != null)
            queueFailedUploads.registerListener(listener);
    }

    /**
     * 
     */
    public void handleCRUpload() {
        
        if(queueCR != null)
        {
            // Catch and Release Queue
                
            boolean moveStatus = queueCR.moveToFirst();
            
            while (moveStatus) {
                
                int uploadStatus = uploadFile(queueCR);
                
                LogUtil.debug(this, "Upload Status for job: "+ uploadStatus);
                
                if(fileQueuedManual == true || this.fileQueuedFailedUploads == true) return; // We break out of this loop to handle manual/ retry failed uploads first
                
                
                // Every time we pick the first in the queue, since we remove the current both for success/failure
                moveStatus = queueCR.moveToFirst();
                
                synchronized (this) {
                    fileQueuedCR = false;
                }
            } 
        }
    }

    /**
     * 
     */
    public void handleManualUpload() {
        
        boolean moveStatus = queueManual.moveToFirst();
        while (moveStatus) 
        {        
            int uploadStatus =  uploadFile(queueManual);
            
            LogUtil.debug(this, "Upload Status for job: "+ uploadStatus);
           
            
         // Every time we pick the first in the queue, since we remove the current both for success/failure

            moveStatus = queueManual.moveToFirst();

            synchronized (this) 
            {
                fileQueuedManual = false;
            }
        } 
        
    }
    
    
    /**
     * 
     */
    public void handleRetryForFailedUpload() {
        
        boolean moveStatus = this.queueFailedUploads.moveToFirst();
        while (moveStatus && this.fileQueuedFailedUploads) 
        {        
            int uploadStatus =  uploadFile(queueFailedUploads);
            
            LogUtil.debug(this, "Upload Status for job: "+ uploadStatus);
           
            
         // Every time we pick the first in the queue, since we remove the current both for success/failure

            moveStatus = queueFailedUploads.moveToFirst();

            
            synchronized (this) 
            {
                UploadManager.failedUploadQueueSizeOnRetry = UploadManager.failedUploadQueueSizeOnRetry - 1;
                if(UploadManager.failedUploadQueueSizeOnRetry <= 0)
                    this.fileQueuedFailedUploads = false;
            }
        } 
        
    }

    /**
     *
     * @return True if upload has been done whether it was successful or queue became invalid, false if next item should be uploaded.
     */
    private int uploadFile(Queue queue) {
   
        int result = CRResultCodes.CR_ERROR_FILE_NOT_FOUND;
        
        result = uploadForQueueType(queue);
      
        return result;
    }
 
    /**
     * @param result
     * @return
     */
    public int uploadForQueueType(final Queue queue) 
    {
        int result = CRResultCodes.CR_ERROR_OUTDATED_QUEUE;
     
        final int uploadType = queue.getUploadType();
        String uploadStr = null;
        
        uploadStr = UploadManager.getUploadStr(uploadType);
        
        if (queue.isValid()) {
            File file = queue.getFile();
            final String mime = queue.getMime();
            final String destPath = queue.getDestPath();
                       
            if (file == null) {
                if (queue.isValid()) {
                    result = CRResultCodes.CR_ERROR_FILE_NOT_FOUND;
                } else {
                    result = CRResultCodes.CR_ERROR_OUTDATED_QUEUE;
                }
            } 
            else if (file.isFile() && file.exists()) {
                
                boolean retry = true;
                int numOfRetries = 3;
                while (retry && numOfRetries > 0) 
                {
                    UploadNotification.clearNotificationOnComplete(this.context);  // Need to make sure no complete notification at this point
                    
                    lockWhenDisconnected(uploadType);  
                    
                    if(numOfRetries == 3)  // 1st attempt
                    {
                        String uploadFileName = file.getName();  // No more cloud file checks simply overwrite, user can access file versions. 
                        
                        UploadManager.setCurrentUploadFileWithType(queue, file, mime, destPath);
                        
                        UploadManager.uploadlastDestFolder = destPath;
                        LogUtil.debug(this, "File to upload:" + uploadStr + uploadFileName + " Dest: " + destPath);
                        if (fileState != null) {
                            if (!fileState.fileName.equals(uploadFileName)) {
                                fileState = new FileUploadState(file, uploadFileName, file.length());
                            }
                        } else {
                            fileState = new FileUploadState(file, uploadFileName, file.length());
                        }
                    }
                
                    
                    // Is currently CR job scheduled ? manual job queued up gets priority break out of loop
                    if(fileQueuedManual && (queueCR != null && queueCR.getQueueSize() != 0 && queue.getUploadType() == UploadManager.CATCH_AND_RELEASE_UPLOAD))  
                        return CRResultCodes.CR_RESULT_OK;  
                    
                    result = processFileForUpload(file, uploadType, destPath);
                    if ((result == CRResultCodes.CR_ERROR_CONNECTION_FAILED) || (result == CRResultCodes.CR_ERROR_UNKNOWN_RESPONSE_PARSER)) {
                        airplaneModeOn = SystemState.isAirplaneModeOn(context);
                        if(airplaneModeOn == false && UploadManager.pausedUpload == false)
                        {
                            numOfRetries--;
                            LogUtil.debug(this, "Upload CR_ERROR_CONNECTION_FAILED Retries left" + uploadStr  + numOfRetries);
                            
                            if(result == CRResultCodes.CR_ERROR_UNKNOWN_RESPONSE_PARSER)
                                fileState.linkToCloud = null;
                                     
                            // We drop the upload job after 3 attempts
                            if(numOfRetries == 0)
                            {
                                removeCurrentJobFromUploadAndAddToFailQueue(queue, file, mime, destPath);
                                handleUploadNotifOnFailure(file);
                                break;
                            }
                        }
                        else
                        {
                            continue;
                        }
                    } else if (result == CRResultCodes.CR_ERROR_UNAUTHORIZED) {
                        numOfRetries = 0;
                        fileState.linkToCloud = null;
                        retry = false;
                        removeCurrentJobFromUploadAndAddToFailQueue(queue, file, mime, destPath);
                        String s = context.getResources().getString(R.string.upload_failed_forbidden_error);
                        s = s.replace("$SYNC", context.getResources().getString(R.string.sync_title));
                        handleUploadNotifOnError(file, s);
                        break;
                    }else if (result == CRResultCodes.CR_RESULT_OK) {
                        
                        UploadManager.uploadlastFile = file;
                        boolean status = UploadManager.removeCurrentJob(queue,file);
                        if(status == false) break; 
                        
                        UploadManager.numFilesUploadedSuccessfully = UploadManager.numFilesUploadedSuccessfully + 1;
                        UploadManager.sizeFilesUploadedSuccessfully = UploadManager.sizeFilesUploadedSuccessfully + ((file != null) ? file.length():0);
                        
                        UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
                        retry = false;
                    } else if (result == CRResultCodes.CR_ERROR_INVALID_VER) {
                        retry = false;
                        UploadManager.clearQueue();
                        String s = context.getResources().getString(R.string.client_upgrade_required);
                        handleUploadNotifOnError(file, s);
                        // Stop the Mozy Service
                        MozyService.stopMozyService(this.context);
                        SystemState.setManualUploadEnabled(false, this.context); 
                        SystemState.setMozyServiceEnabled(false, this.context);
                        break;
                    } else if (result == CRResultCodes.CR_ERROR_INVALID_TOKEN) {
                        retry = false;
                        UploadManager.clearQueue();
                        String s = context.getResources().getString(R.string.device_revoked_body);
                        handleUploadNotifOnError(file, s);
                        // Stop the Mozy Service
                        SystemState.setManualUploadEnabled(false, this.context); 
                        SystemState.setMozyServiceEnabled(false, this.context);
                        break;
                    //}  else if (result == CRResultCodes.CR_ERROR_INVALID_TOKEN) {
                    }  else if (result != CRResultCodes.CR_ERROR_WIFI_CONNECTION_ONLY && result != CRResultCodes.CR_ERROR_ROAMING_NETWORK) {
                        retry = false;
                        removeCurrentJobFromUploadAndAddToFailQueue(queue, file, mime, destPath);
                        handleUploadNotifOnFailure(file);
                        break;
                    }
                    
                    // No need to sleep if we already exhausted our retries
                    if (numOfRetries > 0  && CRResultCodes.isCommunicationError(result) && retry) {
                        synchronized (this) {
                            try {
                                this.wait(SLEEP_ERROR_NORMAL_PRIORITY);
                            } catch (Exception e) {                                
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } 
            else 
            {
                result = CRResultCodes.CR_ERROR_FILE_NOT_FOUND;
                UploadManager.removeCurrentJob(queue, file);
                handleUploadNotifOnFailure(file);
            }
        }
        return result;
    }

   

    
    /**
     * @param queue
     * @param uploadStr
     * @param file
     * @return
     */
    public boolean removeCurrentJobFromUploadAndAddToFailQueue(final Queue queue, final File file, String mime, String destPath) {
        boolean status = queue.removeCurrent();
        LogUtil.debug(this, "Remove Current:" +  file.getName() + ":" + status);
        
        if(status)
            UploadManager.enqueueImageCROrFailUploadQueue(this.queueFailedUploads, file.getAbsolutePath(),file.lastModified(), mime, destPath, false);
        
        return status;
    }

    /**
     * @param numPendingFiles
     * @param result
     * @param file
     * @return
     */
    private int processFileForUpload( final File file, int uploadType, String destPath) {
        int result = CRResultCodes.CR_RESULT_OK;
        synchronized (callbackLock) {
            uploading = true;
            
            Device syncDevice = SystemState.getSyncDevice(); 
            // check if destPath folder exists, if not create the folder and its sub folder
            if(uploadType == UploadManager.MANUAL_UPLOAD || uploadType == UploadManager.CATCH_AND_RELEASE_UPLOAD)
            {
                ListDownload listDownload = null;
                listDownload = ServerAPI.getInstance(context).getExistingUploadedFiles(context, syncDevice.getLink(), false);
                
                int errorCode = listDownload.errorCode;
                ArrayList<Object> list = listDownload.list;
                
                if (listDownload.errorCode == ServerAPI.RESULT_OK) 
                {
                    StringTokenizer tokens = new StringTokenizer(destPath, "/");
                    
                    if(tokens != null && tokens.hasMoreTokens())
                    {
                        String folderName = tokens.nextToken();
        
                        if(folderName != null && folderName.equalsIgnoreCase("") == false)
                        {
                            boolean dupFound = false;
                            if(list != null)
                            {
                                for (int i = 0; i < list.size(); i++)
                                {
                                    if(list.get(i) != null && list.get(i) instanceof Directory)
                                    {
                                        if(((CloudFile) list.get(i)).getTitle().equalsIgnoreCase(folderName))
                                        {
                                            dupFound = true;
                                            break;
                                        }
                                    }
                                }
                            }
                                
                            if(dupFound == false)  // create a folder of that name as it might have been deleted
                            {
                                StringDownload resultCreateUploadDir;
                                try {
                                        if (folderName != null && folderName.equalsIgnoreCase("") == false)
                                        {
                                            resultCreateUploadDir = (uploadFileAPI.getInstance(this.context)).createUploadDir(syncDevice.getLink(),"", destPath);
                                            errorCode = resultCreateUploadDir.errorCode;
                                            
                                            if(errorCode != ErrorCodes.NO_ERROR) 
                                                LogUtil.debug (this, "Upload folder create failed: " + destPath);  // We failed creating directory break out of the loop
                                        }
                                } catch (UnsupportedEncodingException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                   }
                }
            }
            
            
            // We do not want to append the extra slash in the upload URL for uploads at the root level
            if (destPath.equalsIgnoreCase("/")) destPath = "";
            
            if (syncDevice.getEncrypted()) {
            	String encryptDir = FileUtils.getStoragePathForMozy() + "/" + FileUtils.encryptHiddenDir;
            	String encryptPath = encryptDir + "/" + file.getName();
            	String passphrase = Provisioning.getInstance(context).getPassPhraseForContainer(syncDevice.getId());
            	if ((!SystemState.isManagedKeyEnabled(this.context)) && (passphrase == null)) {
            		return CRResultCodes.CR_ERROR_NO_PRIVATE_KEY;
            	}
            	
            	File dir = new File(encryptDir);
            	dir.mkdirs();
            	
            	SyzygyVbiAPI api = new SyzygyVbiAPI();
            	int ret;
            	if(SystemState.isManagedKeyEnabled(this.context)) {
            		ret = api.baseline(file.getPath(), encryptPath, SystemState.getManagedKey(this.context));
            	} else {
            		ret = api.baseline(file.getPath(), encryptPath, api.compressUserKey(passphrase));
            	}
            	
            	if (ret != 0) {
            		;//something wrong, log it. Ideally it won't come here.
            	}
            	
            	final FileUploadState encrytpedFileState = new SyzygyFileUploadState(
            			new File(encryptPath), fileState.fileName, fileState.localFile.length(), fileState);
            	encrytpedFileState.linkToCloud = fileState.linkToCloud;
            	
                ServerAPI.getInstance(context).uploadFile(encrytpedFileState, destPath, true, callbackListener, new ProgressListener() {
                    @Override
                    public void onProgressUpdate(long transferred) 
                    {
                        if(transferred != 0)
                        {
                            UploadWorker.this.notif.fireNotificationOnEnqueue(context, encrytpedFileState.localFile, transferred);
                        }
                    }
                });
            } else {
                // We upload irrespective of errors in creating upload folder
                ServerAPI.getInstance(context).uploadFile(fileState, destPath, false, callbackListener, new ProgressListener() {
                    @Override
                    public void onProgressUpdate(long transferred) 
                    {
                        if(transferred != 0)
                        {
                            UploadWorker.this.notif.fireNotificationOnEnqueue(context, file, transferred);
                        }
                    }
                });
            }
            try {
                if (uploading)
                    callbackLock.wait();
                result = this.resultCode;
            } catch (InterruptedException e) {
                result = CRResultCodes.CR_ERROR_INTERRUPTED;
                e.printStackTrace();
            }
        }
        return result;
    }
    
    /**
     * @param file
     * @param retry
     * @return
     */
    private void handleUploadNotifOnSuccess() {
  
        UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);   
        UploadNotification.clearNotificationOnEnqueue(this.context);
        notif.fireNotificationOnUploadComplete(this.context);
        
    }
    
    
    /**
     * @param file
     * @param retry
     * @return
     */
    private void handleUploadNotifOnFailure(final File file) {
  
        LogUtil.debug(this, "Upload for file: " + file.getAbsolutePath() + " failed");
       
        UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
        UploadNotification.clearNotificationOnEnqueue(this.context);
        
    }
    
    
    /**
     * @param file
     * @param retry
     * @return
     */
    private void handleUploadNotifOnError(final File file, String str) {
  
        LogUtil.debug(this, "Upload for file: " + file.getAbsolutePath() + " failed");
       
        UploadManager.sendBroadcastForUpload(context, ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
        UploadNotification.clearNotificationOnEnqueue(this.context);
        notif.fireNotificationOnError(context, str);
        
    }

    
    private void lockWhenDisconnected(int uploadType) {
        
        lockWhenPaused();
        
        lockWhenInAirplaneMode();

        if(uploadType == UploadManager.CATCH_AND_RELEASE_UPLOAD)
        {
            lockWhenDisconnectedForCR(); 
        }
        else
        {
            lockWhenDisConnectedForManual();
        }
    }

    /**
     * @param connectionApproved
     * @param info
     * @return
     */
    public void lockWhenDisConnectedForManual() {
        
        boolean connectionApproved = false;
        while (!connectionApproved) {

            synchronized (connectionLockManual) {
                UploadWorker.this.info = cm.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    connectionApproved = true;
                }
                if (!connectionApproved) {
                    try {
                        connectionLockManual.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return;
    }
    
   
    
    /**
     * @param connectionApproved
     * @param info
     * @return
     */
    public void lockWhenInAirplaneMode() {
        
        boolean connectionApproved = false;
        while (!connectionApproved) {

            synchronized (connectionLockAirplaneModeOn) {
                if (airplaneModeOn == false) {
                    connectionApproved = true;
                }
                if (!connectionApproved) {
                    try {
                        connectionLockAirplaneModeOn.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return;
    }
    
    
    /**
     * @param connectionApproved
     * @param info
     * @return
     */
    public void lockWhenPaused() {
        
        boolean connectionApproved = false;
        while (!connectionApproved) {

            synchronized (connectionLockPaused) {
                if (UploadManager.pausedUpload == false) {
                    connectionApproved = true;
                }
                if (!connectionApproved) {
                    try {
                        connectionLockPaused.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return;
    }

    /**
     * @param connectionApproved
     * @param info
     * @return
     */
    public void lockWhenDisconnectedForCR() {
        
        boolean connectionApproved = false;
        
        while (!connectionApproved) {
            synchronized (connectionLockCR) {
                UploadWorker.this.info = cm.getActiveNetworkInfo();
                if (info != null && info.isConnected()) {
                    if (info.getType() == ConnectivityManager.TYPE_WIFI)  // Mobile will Always try Wifi first anyway as its free anywhere, if wifi fails, will try mobile UNLESS Wifi only.
                    {
                        connectionApproved = true;
                    } 
                    else if (is_roaming_allowed || !info.isRoaming()) 
                    {
                        if (!is_wifi_only) {
                            connectionApproved = true;
                        }
                        else
                        {
                            notif.fireNotificationOnNoWifiOrRoaming(context, R.string.upload_no_wifi_notification_body);
                        }
                    }
                    else 
                    {
                            notif.fireNotificationOnNoWifiOrRoaming(context, R.string.upload_no_roaming_notification_body);
                    }
                }
                if (!connectionApproved) {
                    try {
                        connectionLockCR.wait(); 
                        UploadNotification.clearNotificationOnEnqueue(context);
                        if(fileQueuedManual)  // we have a manual job waiting
                            return;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return;
    }
}
