
package com.mozy.mobile.android.activities.tasks;

import java.io.File;
import java.util.Date;

import com.mozy.mobile.android.activities.DecryptedFilesCleanUpAlarmManager;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;

public class RemoveDecryptedFilesTask extends AsyncTask<Void, Integer, Void> 
{

    private static Object removeDecryptedFilesLock = new Object();
    private final String TAG = getClass().getSimpleName();
    private Context context;
    
    public RemoveDecryptedFilesTask(Context context) 
    {
        this.context = context;
    }

    
    @Override
    public Void doInBackground(Void... params) 
    {
        
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        
        wl.acquire();
        
        LogUtil.debug(this, "::doInBackground() start");
  
        removeExpiredDecryptedFiles();

        wl.release();
        
        return null;
    }

    private static void removeExpiredInFolder(File cryptedDir, long timeNow) {
    	if(cryptedDir.exists()) {
    		File[] cryptedFiles = cryptedDir.listFiles();
    		for (File file : cryptedFiles) {
    			if((timeNow - file.lastModified()) > DecryptedFilesCleanUpAlarmManager.AUTO_ERASE_TIME) {
    				LogUtil.debug("removeExpiredCryptedFiles", "File deleted : " + cryptedDir.getAbsolutePath() + "/" + file.getName());
    				file.delete();
    			}
    		}
    	}
    }
    /**
     * 
     */
    public static void removeExpiredDecryptedFiles() {
        String path = null;
        
        Date now = new Date();
        long timeNow = now.getTime();
        
        path = FileUtils.getStoragePathForMozy();
        
        if(path != null)
        {
            synchronized (removeDecryptedFilesLock) 
            {
                File mozyDir = new File(path);
               
                if(mozyDir.exists())  // Mozy Dir exists
                {
                    File[] mozySubDirs = mozyDir.listFiles(); // list all sub folders
                    
                    if(mozySubDirs != null && mozySubDirs.length != 0)
                    {
                        for (File inFile : mozySubDirs) 
                        {
                            if (inFile.isDirectory() && inFile.isHidden() == false) 
                            {
                                File[] mozyContainerSubDirs = inFile.listFiles();  // list all folders for each container
                                
                                for (File containerDirs : mozyContainerSubDirs) 
                                {
                                    File decryptedDir = new File(containerDirs.getAbsolutePath() + "/" + FileUtils.decryptHiddenDir);
                                    removeExpiredInFolder(decryptedDir, timeNow);
                                    
                                    // Remove temporary encrypted files here as well. 
                                    // TODO: change the class name to RemoveCryptedFilesTask
                                    File encryptedDir = new File(containerDirs.getAbsolutePath() + "/" + FileUtils.encryptHiddenDir);
                                    removeExpiredInFolder(encryptedDir, timeNow);
                                }
                            }
                        }         
                    }
                }
            }
        }
    }
}
