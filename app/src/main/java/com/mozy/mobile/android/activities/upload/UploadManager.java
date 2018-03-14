package com.mozy.mobile.android.activities.upload;

import java.io.File;
import java.util.Iterator;
import java.util.Vector;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.catch_release.CRReceiver;
import com.mozy.mobile.android.catch_release.MediaObserverManager;
import com.mozy.mobile.android.catch_release.queue.Queue;
import com.mozy.mobile.android.catch_release.queue.QueueDatabase;
import com.mozy.mobile.android.catch_release.queue.QueueManager;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;


/**
 * @class UploadManager
 *
 * Handles settings and initialization of catch and release
 * functionality including both CR and uploading threads.
 * Remember to always having initialized UploadManager prior to
 * use. UploadManager will be initialized upon bootup and at first
 * time launch of application for situations when no bootup has
 * occured.
 *
 * @author Daniel Olofsson (daniel.olofsson@tactel.se)
 *
 */
public class UploadManager
{
    private static final String TAG = UploadManager.class.getSimpleName();

    /**
     * Upload runnable
     */
    private static UploadWorker sWorker = null;
    

    /**
     * Catch and Release upload queue.  
     */
    public static Queue sAutoUploadQueue = null;

    /**
     * The manual upload queue. Manual uploads are enqueued here
     * which in turn causes the worker thread to upload the items.
     */    
    public static Queue sManualQueue = null;
    
    public static Queue sFailedUploadsQueue = null;
    
    public static int lastFailedErrorCodeForUpload = 0;
    public static int failedUploadQueueSizeOnRetry = 0;

    /**
     * The current upload settings
     */
    public static UploadSettings sCurrentSettings;

    /**
     * The Catch and Release Notification Object
     */
    private static UploadNotification sNotification = null;
    
    private static Context appContext = null;
    
    public static int MANUAL_UPLOAD = 0;
    public static int CATCH_AND_RELEASE_UPLOAD = 1;
    public static int FAILED_UPLOAD = 2;
    
    public static int numFilesUploadedSuccessfully = 0;
    
    public static long sizeFilesUploadedSuccessfully = 0;
    
    public static boolean pausedUpload = false;
    
    private static final Object instance_lock = new Object();
    
    public static String  uploadManualDestFolder  = null;
    public static String  uploadCRDestFolder  = null;
    public static String  uploadlastDestFolder = null;
    public static File  uploadlastFile = null;
    
    public static  uploadFileWithType currentUploadFileWithType = null;
   
    
    /**
     * Initializes UploadManager starting file observer as well as upload worker threads.
     * This must be called before UploadManager may function normally, any change attempts to
     * settings or function calls will be rendered useless until initialized.
     * (Will be called on bootup or at first application launch)
     * @param context Context.
     */
    public synchronized static void initialize(final Context context)
    {
        LogUtil.debug(TAG, "initialize()");
        
        synchronized (instance_lock)
        {
            if (sWorker == null
                  || MediaObserverManager.getsMediaObserverManager() == null
                  || sAutoUploadQueue == null
                  || sManualQueue == null
                  || sFailedUploadsQueue == null
                  || sCurrentSettings == null
                  || sNotification == null
                  || MediaObserverManager.getsMediaObserverManager() == null 
                  || MediaObserverManager.getsMediaObserversSize() == 0)
            {
                // Initialize settings
                appContext = context.getApplicationContext();
    
                if (null == sCurrentSettings)
                    sCurrentSettings = new UploadSettings(appContext);
    
                if (null == sNotification)
                    sNotification = new UploadNotification();
    
                if (null == sAutoUploadQueue)
                {
                    sAutoUploadQueue = QueueManager.getQueue(appContext, CATCH_AND_RELEASE_UPLOAD);
                }
                
                if (null == sManualQueue)
                    sManualQueue = QueueManager.getQueue(appContext, MANUAL_UPLOAD);
                
                if(null == sFailedUploadsQueue)
                    sFailedUploadsQueue = QueueManager.getQueue(appContext, FAILED_UPLOAD);
    
                if (null == MediaObserverManager.getsMediaObserverManager() || MediaObserverManager.getsMediaObserversSize() == 0)
                {
                    MediaObserverManager.setsMediaObserverManager(new MediaObserverManager(sAutoUploadQueue));
                }
            }
            
            if (null == sWorker && SystemState.isSyncEnabled())
            {
                sWorker = new UploadWorker(appContext, sNotification, sAutoUploadQueue, sManualQueue, sFailedUploadsQueue);
                sWorker.setWifiOnly(sCurrentSettings != null && sCurrentSettings.getOnlyOnWifi());
                sWorker.setAllowRoaming(sCurrentSettings != null && !sCurrentSettings.getOffWhenRoaming());
            }
            
            if(uploadManualDestFolder == null)
                uploadManualDestFolder = context.getResources().getString(R.string.upload_sync_path) + "/";;
            
            if(uploadCRDestFolder == null)
                uploadCRDestFolder = context.getResources().getString(R.string.upload_sync_path) + "/";;

        }
    }
    
    /**
     * @param activity
     */
    public static void cleanupUploadSettingsAndQueue(Activity activity) {
        // Clear any pending uploads
        UploadManager.clearQueue();
        
        UploadManager.failedUploadQueueSizeOnRetry = 0;
        
        UploadManager.uploadlastDestFolder = null;
        UploadManager.uploadManualDestFolder = null;
        
        // Clear any notifications
        UploadNotification.clearNotificationOnComplete(activity.getApplicationContext());       
        UploadNotification.clearNotificationOnEnqueue(activity.getApplicationContext());
        
        SystemState.setCrInit(false);
        
        if(MediaObserverManager.getsMediaObserverManager()  != null)
        {
             MediaObserverManager.getsMediaObserverManager().release(activity.getApplicationContext());
        }
        
      //stop service if running
        MozyService.stopMozyService(activity.getApplicationContext());
    }

    private static boolean isCatchReleaseInitialized()
    {
        return (sWorker != null
                    && MediaObserverManager.getsMediaObserverManager() != null
                    && sAutoUploadQueue != null
                    && sCurrentSettings != null
                    && sNotification != null);
    }
    
    public static void startCatchAndRelease(Context context, boolean runningCR)
    {
        if (!runningCR)
        {
            // Start the service and that will take care of the rest
            Intent serviceIntent = new Intent();
            serviceIntent.setAction(CRReceiver.ACTION_START_SERVICE);
            context.startService(serviceIntent);
        }
        else
        {
            // Enable the Catch and Release Manager
            UploadManager.initialize(context);
        }
    }

    
    public static Vector<String> getQueueFilesForQueue(Queue queue)
    {
        if(queue != null)
            return queue.getQueueFiles();
        return null;
    }
    
    
    public static void flushAutoUploadQueue()
    {
        if(UploadManager.sAutoUploadQueue != null && UploadManager.sAutoUploadQueue.getQueueSize() > 0)
        {
            // flush out the photos in the CR queue
            UploadManager.sAutoUploadQueue.dequeueAll();
        }
    }

    

    /**
     * Upload settings have changed. Until this has been called at least once,
     * no upload will be performed.
     *
     * Calling this method will trigger upload of already found files if
     * auto mode is enabled in the received settings.
     *
     * @param settings The new settings to use
     */
    public static void uploadSettingsChange(UploadSettings settings)
    {
        if(!isCatchReleaseInitialized())
        {
            throw new IllegalStateException("UploadManager has not been initialized! Run UploadManager.initialize() before using it.");
        }

        LogUtil.debug(TAG, "uploadSettingsChange(): New settings received");


        if(sCurrentSettings != null && uploadInitialized())
        {
            LogUtil.debug(TAG, "uploadSettingsChange(): SETTING_PHOTO_UPLOAD = " + sCurrentSettings.getPhotoUploadType());
            LogUtil.debug(TAG, "uploadSettingsChange(): SETTING_VIDEO_UPLOAD = " + sCurrentSettings.getVideoUploadType());
        }
        else
        {
            LogUtil.debug(TAG, "Warning: sCurrentSettings is null, no settings saved");
        }

        sCurrentSettings = settings;
        sWorker.setWifiOnly(sCurrentSettings != null && sCurrentSettings.getOnlyOnWifi());
        sWorker.setAllowRoaming(sCurrentSettings != null && !sCurrentSettings.getOffWhenRoaming());

    }

    public static void clearQueue()
    {
        if (null != sManualQueue)
        {
            sManualQueue.dequeueAll();
            sManualQueue.close();
        }
        if (null != sAutoUploadQueue)
        {
            sAutoUploadQueue.dequeueAll();
            sAutoUploadQueue.close();
        }
        
        if (null != sFailedUploadsQueue)
        {
            sFailedUploadsQueue.dequeueAll();
            sFailedUploadsQueue.close();
        }
        
        
        if(sWorker != null)
        {
            sWorker.cancel();
        }
        
        sWorker = null;
    }
    


    /**
     * Checks if current upload settings allow automatic photo upload
     *
     * @return true if allowed
     */
    public static boolean automaticPhotoUploadAllowed()
    {
        boolean ret = false;
        
        if (isCatchReleaseInitialized())
            ret = sCurrentSettings.getPhotoUploadType();

        return ret;
    }

    /**
     * Checks if current upload settings allow automatic video upload
     *
     * @return true if allowed
     */
    public static boolean automaticVideoUploadAllowed()
    {
        boolean ret = false;
        
        if (isCatchReleaseInitialized())
            ret = sCurrentSettings.getVideoUploadType();

        return ret;
    }

    public static boolean uploadInitialized()
    {
        return sCurrentSettings.getUploadInitialized();
    }
    
    
    /**
     * @return
     */
    public static int getPendingFilesTotalSize() {
        
        int newTotalSizeFailedRetired = 0;
        
        int newTotalSizeCR = calculateTotalFileSizeForQueue(UploadManager.sAutoUploadQueue);
        
        int newTotalSizeManaul = calculateTotalFileSizeForQueue(UploadManager.sManualQueue);
        
        if(UploadManager.failedUploadQueueSizeOnRetry > 0)
        {
            newTotalSizeFailedRetired = calculateTotalFileSizeForQueue(UploadManager.sFailedUploadsQueue);
        }
            
        return newTotalSizeCR + newTotalSizeManaul + newTotalSizeFailedRetired;
    }
    
    

    /**
     * @param filePaths
     * @return
     */
    public static int calculateTotalFileSizeForQueue(Queue queue) {
        
        Vector<String> filePaths = UploadManager.getQueueFilesForQueue(queue);
        int newTotalSize = 0;
        
        if(filePaths != null)
        {
            // Calculate the total size of all files.
            Iterator<String> itr = filePaths.iterator();
    
            while (itr.hasNext())
            {
                String nextFilePath = itr.next();
    
                File uploadFile = new File(nextFilePath);
                newTotalSize += uploadFile.length();
            }
        }
        return newTotalSize;
    }
    
    
    /**
     * @return
     */
    public static long getCurrentFilesSize(File file) {
     
         if(file != null)
         {
             return file.length();
         }

        return -1;
    }
    /**
     * @return
     */
    public static int getNumPendingFiles() {
        
        int numPendingFilesCR = 0;
        int numPendingFilesManual = 0;
        
        numPendingFilesCR = (sAutoUploadQueue != null) ? getNumPendingFilesForQueue(sAutoUploadQueue) : 0;
        
        numPendingFilesManual = (sManualQueue != null) ? getNumPendingFilesForQueue(sManualQueue) : 0;
       
        
        return numPendingFilesManual + numPendingFilesCR +  UploadManager.failedUploadQueueSizeOnRetry; // manual , CR and failed retried.
    }
    
    
    /**
     * @return
     */
    public static int getNumFailedUploadFiles() 
    {
        
        int numFailedUploadFiles = 0;
        
        
        // total failed - the failed retried ones
        numFailedUploadFiles = getNumPendingFilesForQueue(sFailedUploadsQueue) - UploadManager.failedUploadQueueSizeOnRetry;
        
        if(numFailedUploadFiles >= 0)     
            return numFailedUploadFiles;
        else
            return 0;
    }

    /**
     * @param numPendingFilesManual
     * @return
     */
    public static int getNumPendingFilesForQueue(Queue queue) {
        int numPendingFilesForQueue = 0;
        
        if (queue != null && queue.moveToFirst())
        {
            
            numPendingFilesForQueue = queue.getQueueSize();
        }
        return numPendingFilesForQueue;
    }
    
    
    /**
     * 
     */
    public static void sendBroadcastForUpload(Context context, String action) {
        Intent intent = new Intent(action);

        if (intent != null)
        {
            context.getApplicationContext().sendBroadcast(intent);
        }
    }
    
    public static Queue getQueueForUploadType(int uploadType)
    {
        Queue queue = null;
        
        if(uploadType == UploadManager.MANUAL_UPLOAD)
        {
            queue = UploadManager.sManualQueue;
        } 
        else if (uploadType == UploadManager.CATCH_AND_RELEASE_UPLOAD)
        {
            queue = UploadManager.sAutoUploadQueue;
        }
        else if (uploadType == UploadManager.FAILED_UPLOAD)
        {
            queue = UploadManager.sFailedUploadsQueue;
        }

        return queue;
    }
    
    
    /**
     * @param uploadType
     * @return
     */
    public static String getUploadStr(final int uploadType) {
        String uploadStr;
        if(uploadType == UploadManager.MANUAL_UPLOAD) 
        {
            uploadStr = "(Manual) ";

        }
        else if (uploadType == UploadManager.CATCH_AND_RELEASE_UPLOAD) 
        {
            uploadStr = "(CR) ";
        }
        else
        {
            uploadStr = "(Failed Retries) ";
        }
        return uploadStr;
    }
    
    public static void setCurrentUploadFileWithType(Queue queue, File file, String mimeType, String destPath) 
    {
        int uploadType = queue.getUploadType();
        
        UploadManager.currentUploadFileWithType = new uploadFileWithType ((queue.getFile()).getPath(),queue.getMime(), queue.getDestPath(), uploadType);
    }
    
    /**
     * @param queue
     * @param uploadStr
     * @param file
     * @return
     */
    public static boolean removeCurrentJob(final Queue queue, final File file) {
        
        final String uploadStr = UploadManager.getUploadStr(queue.getUploadType());
        boolean status = queue.removeCurrent();

        LogUtil.debug("removeCurrentJob", "Remove Current:" + uploadStr + file.getName() + ":" + status);
        return status;
    }

    
    /**
     * @param filePath
     * @param file
     * @param mimeType
     */
    public static void enqueueImageCROrFailUploadQueue(Queue queue, String filePath,  long lastModified, String mimeType, String destPath, boolean uploadAllow) 
    {
        ContentValues[] values;
         
         values = new ContentValues[1];
         values[0] = new ContentValues();
         values[0].put(QueueDatabase.KEY_DATA_PATH, filePath);
         values[0].put(QueueDatabase.KEY_DEST_PATH, destPath);
         values[0].put(QueueDatabase.KEY_MODIFIED_DATE, lastModified);
         values[0].put(QueueDatabase.KEY_MIME_TYPE, mimeType);
         
         if(values != null)
         {
             
             Queue.Listener listener = getQueueListener();
             queue.enqueueFilesCROrFailUploaded(values, uploadAllow, listener);
         }
    }

    /**
     * @return
     */
    public static Queue.Listener getQueueListener() {
        Queue.Listener listener = null;
         
         if(UploadManager.sWorker != null)
         {
             listener = UploadManager.sWorker.getListener();
         }
        return listener;
    }

}
