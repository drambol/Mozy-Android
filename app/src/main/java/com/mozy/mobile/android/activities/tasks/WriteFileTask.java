package com.mozy.mobile.android.activities.tasks;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ContextMenuActivity;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.activities.helper.DownloadComplete;
import com.mozy.mobile.android.activities.helper.FileDownloader;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.web.containers.ListDownload;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

 public class WriteFileTask extends AsyncTask<Void, Void, Integer> implements DownloadComplete
 {
        private LocalFile localFile = null;
        private CloudFile cloudFile = null;
        private ProgressDialog progressDialog = null;
        private boolean bDeviceEncrypted = false;
        private boolean bSyzygyFormat = false;
        private final Listener listener;
        private SecuredActivity contextActivity;
        protected FileDownloader fileDownloader = null;
        private String contentEncoding = null;
        
        private static final int STATUS_FAIL = -1;
        
        public static interface Listener {
            void onWriteFileTaskCompleted(int statusCode);
        }

        public WriteFileTask( LocalFile inputLocalFile, 
        		CloudFile inputCloudFile, 
        		SecuredActivity activity , 
        		boolean bDeviceEncrypted,
        		boolean bSyzygyFormat,
        		Listener listener)
        {
            this.localFile = inputLocalFile;
            this.cloudFile = inputCloudFile;
            this.bDeviceEncrypted = bDeviceEncrypted;
            this.bSyzygyFormat = bSyzygyFormat;
            this.contextActivity = activity;
            
            this.listener = listener;
        }

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            
            
            String strProgressMessage =  this.contextActivity.getString( R.string.progress_bar_downloading_files);

            // Show the progress dialog
            this.progressDialog = new ProgressDialog( this.contextActivity);
            this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            this.progressDialog.setMessage(strProgressMessage);
            this.progressDialog.setIndeterminate(false);
            this.progressDialog.setCancelable(false);
            this.progressDialog.setMax((int)cloudFile.getSize() / 1024);    // Track size in KB instead of bytes
            this.progressDialog.setProgress(0);
            this.progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                                                this.contextActivity.getText(R.string.cancel_button_text),
                                                new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int id) {
                                                       
                                                            if(WriteFileTask.this.fileDownloader != null)
                                                            {
                                                                WriteFileTask.this.fileDownloader.abort();
                                                                WriteFileTask.this.fileDownloader = null;
                                                            }
                                                            WriteFileTask.this.cancel(true);
                                                    }
            });
            
            synchronized(this)
            {
               this.fileDownloader = new FileDownloader(localFile,
                                                           cloudFile,
                                                           this,
                                                           this.contextActivity.getApplicationContext(),
                                                           this.progressDialog,
                                                           this.bDeviceEncrypted,
                                                           this.bSyzygyFormat);
            }
            
            this.progressDialog.show();
        }

        @Override
        protected Integer doInBackground(Void... params)
        {
            int status =  STATUS_FAIL;
            
            if(null != this.fileDownloader)
                status = writeFile();

            return status;
        }

        @Override
        protected void onPostExecute(Integer status)
        {
            this.progressDialog.dismiss();
            
   
             if(status == ServerAPI.RESULT_OK)
             {
                 // Insert / update entry of file after successful download
                 if(SystemState.mozyFileDB != null)
                     SystemState.mozyFileDB.insertOrUpdateDownloadedFileinDB( 
                    		 ((ContextMenuActivity) this.contextActivity).getRootDeviceId(), 
                    		 this.cloudFile, this.localFile, contentEncoding);
             }
             
             if(listener != null)         
                 listener.onWriteFileTaskCompleted(status);
            
            //onActivityResult(0, status, null);
        }
        
        // Download and write the 'cloudFile' to the 'localFile'.
        private int writeFile()
        {
			StringBuffer encoding_buffer = new StringBuffer("");
            int status = this.fileDownloader.start(encoding_buffer);  
            contentEncoding = encoding_buffer.toString();
            
            return status;
        }
        
        @Override
        // Called when a file download is completed
        public int finished(int status)
        {
            // update the cloudfile object time stamp
            if(status == ErrorCodes.NO_ERROR)
            {
                ListDownload returnValue = ServerAPI.getInstance(((ContextMenuActivity) this.contextActivity)).getCloudFileForFileLink(cloudFile.getLink()); //get the latest timestamp on cloudfile
                
                if(returnValue.list != null)
                {
                    CloudFile latestCloudFile = (CloudFile) returnValue.list.get(0);
                    long cloudFileUpdated = latestCloudFile.getUpdated();
                
                    this.cloudFile.setUpdated(cloudFileUpdated);
                }
            }
            this.fileDownloader = null;
            
            return status;
        }
        
        @Override
        protected void onCancelled() {
            this.progressDialog.dismiss();
            if(listener != null)         
                listener.onWriteFileTaskCompleted(ServerAPI.RESULT_CANCELED);
        }


    } // class WriteFileTask