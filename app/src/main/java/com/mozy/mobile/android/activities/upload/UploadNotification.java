package com.mozy.mobile.android.activities.upload;

import java.io.File;
import java.util.Locale;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;


import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.DirFileListActivity;
import com.mozy.mobile.android.activities.NavigationTabActivity;

import com.mozy.mobile.android.activities.startup.FirstRun;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CatchAndReleaseFolder;
import com.mozy.mobile.android.provisioning.Provisioning;

import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.web.uploadFileAPI;

public class UploadNotification {
    private PendingIntent contentIntentForUploadStatus = null;
    private Intent notificationIntentForUploadStatus = null;
    
    private PendingIntent contentIntentForUploadComplete = null;
    private Intent notificationIntentForUploadComplete = null;
    private Notification notif = null;
    private Notification notifComplete = null;
    static final int notificationId = 1;
    static final int notificationCompleteId = 2;
    private static final String ns = Context.NOTIFICATION_SERVICE;
    
    
    UploadNotification()
    {
    }

    public void fireNotificationOnUploadComplete(Context context)
    {
        synchronized (this)
        {
            if(UploadManager.sizeFilesUploadedSuccessfully > 0)
            {
                CharSequence tickerText = context.getString(R.string.upload_notification_title);
                String contentText = context.getString(R.string.upload_done_notification_body);
                // Insert the file count and size
                String filesize = null;
    
                double size = UploadManager.sizeFilesUploadedSuccessfully;
                final int decr = 1024;
                int step = 0;
                String[] postFix = context.getResources().getStringArray(R.array.file_sizes_array);
                while((size / decr) > 0.9)
                {
                    size = size / decr;
                    step++;
                }
                filesize = String.format(Locale.getDefault(), "%.1f %s", size, postFix[step]);
                
                contentText = contentText.replace("$NUMFILES", Integer.toString(UploadManager.numFilesUploadedSuccessfully));
                contentText = contentText.replace("$TOTALSIZE", null != filesize ? filesize + " " : "");
    
                if(notifComplete == null)
                {               
                    int icon = R.drawable.mozy_notification_icon;
                    long when = System.currentTimeMillis();
                    notifComplete = new Notification(icon, tickerText, when);
                    notifComplete.flags |= Notification.FLAG_AUTO_CANCEL;
                }
    
                Provisioning provisioning = Provisioning.getInstance(context);
                if((provisioning.getMipAccountToken() != null && provisioning.getMipAccountToken().compareTo("") == 0) 
                        && (provisioning.getMipAccountTokenSecret() != null && provisioning.getMipAccountTokenSecret().compareTo("") == 0))  // handles selection of uploaded file from notification when signed out from Mozy
                { 
                    this.notificationIntentForUploadComplete = new Intent(context,  FirstRun.class);
                }
                else
                {
                    if(UploadManager.uploadlastDestFolder != null)
                    {
                        String title = null;
                        if((UploadManager.uploadManualDestFolder).equalsIgnoreCase(""))
                        {
                            title = context.getResources().getString(R.string.sync_title);
                        }
                        else
                        {
                            title = UploadManager.uploadlastDestFolder;
                        }
                        // Trim off ending / or \
                        if ((title.endsWith("\\") && (!(title.equalsIgnoreCase("\\")))) 
                                || (title.endsWith("/") && !(title.equalsIgnoreCase("/"))))
                        {            
                            title = title.substring(0, title.length() - 1);
                        }
                                 


                        // Figure out what our activity will be        
                        String deviceLink = null;
                        if(UploadManager.uploadlastDestFolder.equalsIgnoreCase("/"))
                        {
                            deviceLink = (uploadFileAPI.getInstance(context)).getCatchAndReleaseLink(ServerAPI.getInstance(context).GetCloudDeviceLink(), "");
                        }
                        else
                        {
                            deviceLink = (uploadFileAPI.getInstance(context)).getCatchAndReleaseLink(ServerAPI.getInstance(context).GetCloudDeviceLink(), UploadManager.uploadlastDestFolder);
                        }
                        
                        if(deviceLink != null && SystemState.cloudContainer != null)
                        {
                            
                            if (title.equalsIgnoreCase("/")) title = context.getResources().getString(R.string.sync_title_s_caps);
                            CatchAndReleaseFolder folder = new CatchAndReleaseFolder(deviceLink, title, 0);

                            notificationIntentForUploadComplete = new Intent(context, DirFileListActivity.class);
                            notificationIntentForUploadComplete.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            notificationIntentForUploadComplete.putExtra("containerLink", folder.getLink());
                            notificationIntentForUploadComplete.putExtra("title", folder.getTitle());
                            notificationIntentForUploadComplete.putExtra("canFilesBeDeleted", true);
                            notificationIntentForUploadComplete.putExtra("isPhotoDirGridEnabled", true);
                            notificationIntentForUploadComplete.putExtra("fromNotification", true);
                            notificationIntentForUploadComplete.putExtra("deviceId", SystemState.cloudContainer.getId());
                            notificationIntentForUploadComplete.putExtra("deviceTitle", SystemState.cloudContainer.getTitle());
                            notificationIntentForUploadComplete.putExtra("deviceType", SystemState.cloudContainer.getEncrypted());
                            notificationIntentForUploadComplete.putExtra("platform", SystemState.cloudContainer.getPlatform());
                        }
                        else
                        {
                            notificationIntentForUploadComplete = new Intent(context, NavigationTabActivity.class);  // default
                        }
                    }
                }
        
                contentIntentForUploadComplete = PendingIntent.getActivity(context, 0, notificationIntentForUploadComplete, PendingIntent.FLAG_UPDATE_CURRENT);
  
                notifComplete.setLatestEventInfo(context, tickerText, contentText, contentIntentForUploadComplete);
                NotificationManager mNotifyMan = (NotificationManager)context.getSystemService(ns);
                mNotifyMan.notify(notificationCompleteId, notifComplete);
            }
        }
    }

    
    public void fireNotificationOnEnqueue(Context context, File file, long uploadedBytesForFile)
    {
        long uploadFileSize =  0;
        final int numPendingFiles = UploadManager.getNumPendingFiles();
        
        if(file != null)
            uploadFileSize = UploadManager.getCurrentFilesSize(file);
        
        if(numPendingFiles <= 0 || uploadFileSize == -1)
          return;
        
        synchronized (this)
        {
            CharSequence tickerText = context.getString(R.string.upload_notification_title);
            String contentText1 = context.getString(R.string.upload_files_remaining_body);
            String contentText2 = context.getString(R.string.upload_pending_notification_body);
            
            // Insert the file count and size
            String filesize = null;

            if (uploadFileSize > 0)
            {
                double fileSize = uploadFileSize;
                double uploadedSize = uploadedBytesForFile;
                final int decr = 1024;
                int step = 0;
                String[] postFix = context.getResources().getStringArray(R.array.file_sizes_array);
                while((fileSize / decr) > 0.9)
                {
                    fileSize = fileSize / decr;
                    uploadedSize = uploadedSize /decr;
                    step++;
                }
                filesize = String.format(Locale.getDefault(), "%.1f/%.1f %s",uploadedSize, fileSize, postFix[step]);
            }
            
            contentText1 = contentText1.replace("$NUMFILES", Integer.toString(numPendingFiles));
            contentText2 = contentText2.replace("$TOTALSIZE", null != filesize ? filesize + " " : "");
            
            String contentText = contentText1 + " " + contentText2;
    
            fireNotificationStatusForCurrentUpload(context, tickerText, contentText);
        } 
    }
    
    public static void clearNotificationOnEnqueue(Context context)
    {
        NotificationManager mNotifyMan = (NotificationManager)context.getSystemService(ns);
        mNotifyMan.cancel(notificationId);
    }
    
    public static void clearNotificationOnComplete(Context context)
    {
        NotificationManager mNotifyMan = (NotificationManager)context.getSystemService(ns);
        mNotifyMan.cancel(notificationCompleteId);
    }
    
    public void fireNotificationOnNoWifiOrRoaming(Context context, int res)
    {
        int numPendingFilesCR = UploadManager.getNumPendingFilesForQueue(UploadManager.sAutoUploadQueue);
        final int numTotalPendingFiles = UploadManager.getNumPendingFiles();
        
        if(numPendingFilesCR <= 0  || numTotalPendingFiles > numPendingFilesCR)
          return;
        
        synchronized (this)
        {
            CharSequence tickerText = context.getString(R.string.upload_notification_title);
            String contentText = context.getString(R.string.upload_files_remaining_body);
            
            contentText = contentText.replace("$NUMFILES", Integer.toString(numPendingFilesCR));
            
            contentText = contentText + " " + context.getString(res);
  
            fireNotificationStatusForCurrentUpload(context, tickerText, contentText);
        } 
    }

    /**
     * @param context
     * @param tickerText
     * @param contentText
     */
    public void fireNotificationStatusForCurrentUpload(Context context, CharSequence tickerText, String contentText) {
        if (null == notif)
        {
            int icon = R.drawable.mozy_notification_icon;
            long when = System.currentTimeMillis();
            notif = new Notification(icon, tickerText, when);
            notif.flags |= Notification.FLAG_AUTO_CANCEL  | Notification.FLAG_ONGOING_EVENT;
         }
        
        Provisioning provisioning = Provisioning.getInstance(context);
        if((provisioning.getMipAccountToken() != null && provisioning.getMipAccountToken().compareTo("") == 0) 
                && (provisioning.getMipAccountTokenSecret() != null && provisioning.getMipAccountTokenSecret().compareTo("") == 0))  // handles selection of uploaded file from notification when signed out from Mozy
        { 
            this.notificationIntentForUploadStatus = new Intent(context,  FirstRun.class);
        }
        else
        {
            this.notificationIntentForUploadStatus = new Intent(context,  UploadStatusActivity.class);
        }
        
        this.notificationIntentForUploadStatus.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        
        contentIntentForUploadStatus = PendingIntent.getActivity(context,0, this.notificationIntentForUploadStatus, PendingIntent.FLAG_UPDATE_CURRENT);
        
        notif.setLatestEventInfo(context, tickerText, contentText, contentIntentForUploadStatus);
        NotificationManager mNotifyMan = (NotificationManager)context.getSystemService(ns);
        mNotifyMan.notify(notificationId, notif);
    }
    
    
    public void fireNotificationOnError(Context context, String str)
    {
        synchronized (this)
        {
            CharSequence tickerText = context.getString(R.string.upload_notification_title);
            String contentText = str;
           
            if(notifComplete == null)
            {
                int icon = R.drawable.mozy_notification_icon;
                long when = System.currentTimeMillis();
                notifComplete = new Notification(icon, tickerText, when);
                notifComplete.flags |= Notification.FLAG_AUTO_CANCEL;
            }
            
            Provisioning provisioning = Provisioning.getInstance(context);
            if((provisioning.getMipAccountToken() != null && provisioning.getMipAccountToken().compareTo("") == 0) 
                    && (provisioning.getMipAccountTokenSecret() != null && provisioning.getMipAccountTokenSecret().compareTo("") == 0))  // handles selection of uploaded file from notification when signed out from Mozy
            { 
                this.notificationIntentForUploadStatus = new Intent(context,  FirstRun.class);
            }
            else
            {
                this.notificationIntentForUploadStatus = new Intent(context,  UploadStatusActivity.class);
            }
            
            this.notificationIntentForUploadStatus.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            contentIntentForUploadStatus = PendingIntent.getActivity(context,0, this.notificationIntentForUploadStatus, PendingIntent.FLAG_UPDATE_CURRENT);
            
            notifComplete.setLatestEventInfo(context, tickerText, contentText, contentIntentForUploadStatus);
            NotificationManager mNotifyMan = (NotificationManager)context.getSystemService(ns);
            mNotifyMan.notify(notificationCompleteId, notifComplete);
        } 
    }


    public void resetNotification()
    {
        synchronized (this)
        {
            UploadManager.numFilesUploadedSuccessfully = 0;
            UploadManager.sizeFilesUploadedSuccessfully = 0;
            notif = null;
            notifComplete = null;
            contentIntentForUploadStatus = null;
            contentIntentForUploadComplete = null;
        }
    }
    
}
