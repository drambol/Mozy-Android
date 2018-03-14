package com.mozy.mobile.android.activities.tasks;

import java.io.File;


import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ContextMenuActivity;
import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.security.SyzygyVbiAPI;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;


public class SyzygyRestoreTask extends AsyncTask<Void, Integer, Integer> {
	
	private ProgressDialog progressDialog = null;
	private LocalFile localFile;
    private LocalFile outputFile;
    private String deviceId;
    //private String platform;
    private String passPhrase;
    private SecuredActivity contextActivity;
    private final Listener listener;
    private int restore_ret; 
    
    public static interface Listener {
        void onRestoreTaskCompleted(LocalFile outputFile);
    }
	
	public SyzygyRestoreTask(LocalFile localFile, SecuredActivity activity ,String deviceId, Listener listener) {
        
        this.localFile = localFile;
        this.deviceId = deviceId;
        //this.platform = platform;
        this.contextActivity = activity;
        this.passPhrase  = Provisioning.getInstance(this.contextActivity).getPassPhraseForContainer(this.deviceId);
        
        this.listener = listener;
    }
	
	protected void onPreExecute() {
		super.onPreExecute();
		
		String strProgressMessage = this.contextActivity.getString( R.string.Decrypting_File);
		// Show the progress dialog
        this.progressDialog = new ProgressDialog(this.contextActivity);
       // this.progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        this.progressDialog.setMessage(strProgressMessage);
        this.progressDialog.setIndeterminate(true);
        this.progressDialog.setCancelable(true);
        this.progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                this.contextActivity.getText(R.string.cancel_button_text),
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    cancel(true);
                                                }
        });
        this.progressDialog.show();
        
	}
	
	@Override
    protected Integer doInBackground(Void... params) {
		
		File outputDir = new File(localFile.file.getParent() + "/" + FileUtils.decryptHiddenDir);  // saved in hidden decrypted folder
        outputDir.mkdirs();
        
        outputFile = new LocalFile(outputDir.getAbsolutePath() + "/" + localFile.file.getName());
        if (outputFile.file.exists()) {
        	//if (outputFile.file.lastModified() > localFile.file.lastModified()) {
        	//	restore_ret = 0;
        	//	return 0;
        	//}
        	outputFile.file.delete();
        }
        
        SyzygyVbiAPI syzygyAPI = new SyzygyVbiAPI();
        byte [] decryptKey;
        if(SystemState.isManagedKeyEnabled(this.contextActivity)) {
        	decryptKey = SystemState.getManagedKey(this.contextActivity);
        } else {
        	decryptKey = syzygyAPI.compressUserKey(passPhrase);
        }
  
        restore_ret = syzygyAPI.restore(localFile.getPath(), outputFile.getPath(), decryptKey);
        
        if (restore_ret != 0) {
        	if (outputFile.file.exists()) {
            	outputFile.file.delete();
            }
        	outputFile.file = null;
        }
        LogUtil.debug("SyzygyRestoreTask", "doInBackground end jni\n");
		
		return 0;
	}
	
	@Override
    protected void onPostExecute(Integer status) {
		super.onPostExecute(status);
		this.progressDialog.dismiss();
        
        //updateEncryptedFileDB();
		if (isCancelled() == false && restore_ret == 0) {
			updateDBPostDecryption(this.outputFile.file.lastModified());
		} else {
			updateDBPostDecryption(-1);
			if(restore_ret!= 0 && contextActivity != null) {
                  ((Activity) contextActivity).showDialog(ContextMenuActivity.PRIVATE_KEY_ENCRYPTED_ERROR_MSG);
            }
		}
        
        listener.onRestoreTaskCompleted(this.outputFile);
        LogUtil.debug("SyzygyRestoreTask", "onPostExecute end\n");
	}
	
	@Override
    protected void onCancelled() {
		this.progressDialog.dismiss();
        if(listener != null)  
        {
            if(this.outputFile.file != null)
                deleteFile(this.outputFile);
            
            listener.onRestoreTaskCompleted(null);
        }
        LogUtil.debug("SyzygyRestoreTask", "onCancelled end\n");
	}
	
	private boolean deleteFile(LocalFile outputFile2)
	{
		// Make sure the file or directory exists and isn't write protected
		if (!outputFile2.file.exists())
			throw new IllegalArgumentException(
					"Delete: no such file or directory: " + outputFile2.getPath());

		if (!outputFile2.file.canWrite())
			throw new IllegalArgumentException("Delete: write protected: "
					+ outputFile2.getPath());
        
		return (outputFile2.delete());

	}
	
	protected void updateDBPostDecryption(long decryptedDate) 
	{
		// File should exists in DB as it as been successfully downloaded previously
		if((SystemState.mozyFileDB != null) && SystemState.mozyFileDB.existsFileInDB(this.deviceId,localFile.getName()) == true) {
			SystemState.mozyFileDB.updateFileWithEncryptionDateInDB(this.deviceId, 
					localFile.getName(), decryptedDate);
		}
	}
}
