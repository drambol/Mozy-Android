package com.mozy.mobile.android.activities.upload;

import java.util.ArrayList;
import android.app.Activity;
import android.app.AlertDialog;
 
import android.content.DialogInterface;
 

public class MozyUploadExistsInQueue
{
    private Activity mActivity;
    private ArrayList <uploadFile> existingFilesInManualQueue = new ArrayList <uploadFile>();
    private ArrayList <uploadFile> existingFilesInCRQueue = new ArrayList <uploadFile>();
    private ArrayList <uploadFile> existingFilesInFailedUploadQueue = new ArrayList <uploadFile>();
    private AlertDialog dialog;
    
    
    protected MozyUploadExistsInQueue(Activity activity) 
    {
        mActivity = activity;
    }

       
    public boolean isExistsInQueue()
    {
        boolean status = false;
        if((existingFilesInManualQueue != null && existingFilesInManualQueue.size() != 0)
                || (existingFilesInCRQueue != null && existingFilesInCRQueue.size() != 0)
                || (existingFilesInFailedUploadQueue != null && existingFilesInFailedUploadQueue.size() != 0))
             {
                 if(existingFilesInManualQueue.size() + existingFilesInCRQueue.size() + existingFilesInFailedUploadQueue.size() > 0)
                 {
                     for(int j = 0; j < existingFilesInManualQueue.size(); j++)
                     {
                         removeFromCurrentQueue(existingFilesInManualQueue.get(j), UploadManager.MANUAL_UPLOAD);
                         
                         MozyUploadActivity.uploadList.add(existingFilesInManualQueue.get(j));  // goes to end of the list
                     }   
                     
                     for(int k = 0; k < existingFilesInCRQueue.size(); k++)
                     {
                         removeFromCurrentQueue(existingFilesInCRQueue.get(k), UploadManager.CATCH_AND_RELEASE_UPLOAD);
                         
                         MozyUploadActivity.uploadList.add(existingFilesInCRQueue.get(k));  // goes to end of the list
                     }   
                     
                     for(int l = 0; l < existingFilesInFailedUploadQueue.size(); l++)
                     {
                         removeFromCurrentQueue(existingFilesInFailedUploadQueue.get(l), UploadManager.FAILED_UPLOAD);
                         
                         MozyUploadActivity.uploadList.add(existingFilesInFailedUploadQueue.get(l));  // goes to end of the list
                     }   
                 }
                 
                 uploadAndDismiss(dialog);

                 status = true;
             }
        return status;
    }
    
    
 

    /**
     * @param dataPath
     * @param mime
     */
    public void checkExistsInQueues(String dataPath, String mime, String destPath) 
    {
        // Check for existing files in each of the queues and make a list
         
         if( UploadManager.sManualQueue != null && UploadManager.sManualQueue.existsInQueue(dataPath, destPath))
         {
           existingFilesInManualQueue.add(new uploadFile(dataPath, mime, destPath));
         }
         else if(UploadManager.sAutoUploadQueue != null &&  UploadManager.sAutoUploadQueue.existsInQueue(dataPath, destPath))
         {
           existingFilesInCRQueue.add(new uploadFile(dataPath, mime, destPath));
         }
         else if (UploadManager.sFailedUploadsQueue!= null && UploadManager.sFailedUploadsQueue.existsInQueue(dataPath, destPath))
         {
            existingFilesInFailedUploadQueue.add(new uploadFile(dataPath, mime, destPath));
         }
    }



    public void removeFromCurrentQueue( uploadFile duplicateFile, int uploadType) 
    {
        if(UploadManager.getQueueForUploadType(uploadType) != null && duplicateFile != null && duplicateFile.fullPath != null)
        {
            UploadManager.getQueueForUploadType(uploadType).removeEntryForPath(duplicateFile.fullPath, duplicateFile.destPath);
        }
    }
    
   
    /**
     * @param dialog
     */
    public void uploadAndDismiss(DialogInterface dialog) {
        UploadManager.pausedUpload = false;
        
        // clear the lists
        existingFilesInManualQueue.clear();
        existingFilesInCRQueue.clear();
        existingFilesInFailedUploadQueue.clear();
        
        if(this.dialog != null)
            dialog.cancel();  
        if(MozyUploadActivity.uploadList != null)
        {
            MozyUploadActivity.queueForUpload(MozyUploadActivity.uploadList);
        }
        
        if(this.dialog != null)
            dialog.dismiss();
        this.dialog = null;
        
        if(MozyUploadActivity.uploadList != null)
            MozyUploadActivity.uploadList.clear();
        
        mActivity.finish();
    }

}
