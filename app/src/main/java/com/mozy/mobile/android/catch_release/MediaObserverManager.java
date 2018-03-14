package com.mozy.mobile.android.catch_release;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.os.FileObserver;

import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.catch_release.queue.Queue;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;

/**
 * <code>MediaObserverManager</code> manages file observers under dicm folders  
 */
public class MediaObserverManager
{
    private static final String TAG = MediaObserverManager.class.getSimpleName();
    
    /**
     * The MediaObserverManager manages file observers under DICM folder 
     */
    private static MediaObserverManager sMediaObserverManager = null;

    public static MediaObserverManager getsMediaObserverManager() {
        return sMediaObserverManager;
    }


    public static void setsMediaObserverManager(
            MediaObserverManager sMediaObserverManager) {
        MediaObserverManager.sMediaObserverManager = sMediaObserverManager;
    }


    /**
     * FileObserver used for listening for created media files
     */
    private static ArrayList<MediaObserver> mMediaObserver = new ArrayList<MediaObserver>();


    /**
     * Class constructor
     *
     * queue  upload catch and release queue
     */
    public MediaObserverManager(Queue queue)
    {
       LogUtil.debug(TAG, "Creating MediaObserverManager..");
       
       boolean isMediaObserverAdded = false;
       boolean status = false;
       
       status = addExternalSDCardMediaObservers(queue);
       
       isMediaObserverAdded = isMediaObserverAdded || status;

       status = addInternalSDCardMediaObservers(queue);
       
       isMediaObserverAdded = isMediaObserverAdded || status;
       
       if(isMediaObserverAdded == false)
           setsMediaObserverManager(null);
    }


    /**
     * @param queue
     * @return
     */
    public boolean addInternalSDCardMediaObservers(Queue queue) {
        boolean isMediaObserverAdded = false;
        boolean status = false;
        String dicmPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
           
        status = addMediaObserversForDICM(queue, dicmPath);
       
       
       // Also lower case dicm folder used by few phones
       
       if(status == false)
           status = addMediaObserversForDICM(queue, dicmPath.toLowerCase(Locale.getDefault()));
       
       isMediaObserverAdded = isMediaObserverAdded || status;
       
       // find DCIM under one level for external_sd scenario LG optimus and other such devices
       
       File dir = Environment.getExternalStorageDirectory();
       
       status =  getOtherDicmPathInDirAndAddMediaObserver(queue, dir);
    
       isMediaObserverAdded = isMediaObserverAdded || status;
       
       return isMediaObserverAdded;
    }


    /**
     * @param queue
     */
    public boolean addExternalSDCardMediaObservers(Queue queue) 
    {
        boolean isMediaObserverAdded = false;
        HashSet<String> extMnts = getExternalMounts();
        ArrayList<String> extDicmPaths = new ArrayList<String>();
           
        if(extMnts != null)
        {
               //get the Iterator
               Iterator<String> itr = extMnts.iterator();
              
               LogUtil.debug("getExternalMounts", "HashSet contains : ");
               while(itr != null && itr.hasNext())
               {
                   String mountPath = itr.next();
                   LogUtil.debug("getExternalMounts",mountPath);
                   extDicmPaths.add(mountPath);
               }
           }
           
           if(extDicmPaths != null && extDicmPaths.size() > 0)
           {
               for(int k = 0; k < extDicmPaths.size(); k++)
               {
                   boolean status = false;
                   File dir = new File(extDicmPaths.get(k));
                   status =  getDicmPathInDirAndAddMediaObserver(queue, dir);
                   isMediaObserverAdded = isMediaObserverAdded || status;
               }
           }
           
           return isMediaObserverAdded;
    }


    /**
     * @param queue
     * @param dir
     */
    public boolean getOtherDicmPathInDirAndAddMediaObserver(Queue queue, File dir) {
        boolean status = false;
        File[] files = dir.listFiles(); 
           
        for(int i = 0; files != null && i < files.length; i++)
        {
           if(files[i].isDirectory())
           {
              if(files[i].getName().equalsIgnoreCase("DCIM")) 
              {
                  continue;
              }
              else
              {
                  File[] subDirs = files[i].listFiles();
                  
                  for(int j = 0; subDirs != null && j < subDirs.length; j++)
                  {
                      if(subDirs[j].getName().equalsIgnoreCase("DCIM")) 
                      {
                          status = addMediaObserversForDICM(queue, subDirs[j].getPath());
                      }
                  }
              }
           }
        }
           
        return status;
    }
    
    
    /**
     * @param queue
     * @param dir
     */
    public boolean getDicmPathInDirAndAddMediaObserver(Queue queue, File dir) {
        boolean status = false;
        File[] files = dir.listFiles(); 
           
       for(int i = 0; files != null && i < files.length; i++)
       {
           if(files[i].isDirectory())
           {
              if(files[i].getName().equalsIgnoreCase("DCIM")) 
              {
                  status = addMediaObserversForDICM(queue, files[i].getPath());
              }
           }
       }
           
       return status;
    }


    /**
     * @param queue
     * @param dicmPath
     */
    public boolean addMediaObserversForDICM( Queue queue, String dicmPath) {
        
        boolean status = false;
        
        File dicmDir = new File(dicmPath);
           
           if(dicmDir.exists())
           {
               File[] files = dicmDir.listFiles();
               
               if(files != null && files.length != 0)
               {
                   for (File inFile : files) {
                       if (inFile.isDirectory() && inFile.isHidden() == false) {
                           // is directory
                           LogUtil.debug(TAG, "File observer created for " +  dicmPath  + "/" + inFile.getName());
                           mMediaObserver.add(new MediaObserver(dicmPath  + "/" + inFile.getName(), queue));
                       }
                   }
                    
                    for(int i = 0; i < mMediaObserver.size(); i++)
                    {
                        mMediaObserver.get(i).startWatching();
                        status = true;
                    }
               }
           }
           return status;
    }


    public void release(Context context)
    {
        for(int i = 0; i < mMediaObserver.size(); i++)
            mMediaObserver.get(i).stopWatching();
        
        setsMediaObserverManager(null);
    }


    /**
     * Class used for listening for files 
     */
    private static class MediaObserver extends FileObserver
    {
        private static final String TAG = MediaObserver.class.getSimpleName();

        public String absolutePath;
        Queue queueCR;

        public MediaObserver(String path, Queue queue)
        {
            super(path, FileObserver.CLOSE_WRITE | FileObserver.MOVED_TO);
            absolutePath = path;
            queueCR = queue;
        }

 
        @Override
        public void onEvent(int event, String path) 
        {
            if (path == null) {
                return;
            }

            //a new file or subdirectory was created under the monitored directory
            if ((FileObserver.CLOSE_WRITE & event)!=0 || (FileObserver.MOVED_TO & event) != 0) 
            {
                 LogUtil.debug(TAG, absolutePath + "/" + path + " is created\n");
                 
                 String filePath = absolutePath + "/" + path;
                 
                 File file = new File(filePath);
                 
                 String mimeType = FileUtils.getMimeTypeFromFileName(file.getName());
                 
                 
                 int category = FileUtils.getCategory(mimeType);
                 if(((category == FileUtils.CATEGORY_PHOTOS_HOLDER) ) || ((category == FileUtils.CATEGORY_VIDEOS)))
                 {
                     if(UploadManager.sCurrentSettings.getUploadInitialized())  // Upload has been initialized, queue the files
                     {
                         if(((category == FileUtils.CATEGORY_PHOTOS_HOLDER) && UploadManager.automaticPhotoUploadAllowed())
                             || ((category == FileUtils.CATEGORY_VIDEOS) && UploadManager.automaticVideoUploadAllowed()))
                         {     
                             UploadManager.enqueueImageCROrFailUploadQueue(queueCR, filePath,  file.lastModified(), mimeType, UploadManager.uploadCRDestFolder,true);
                         }
                     }
                     else
                     {
                         // queue only the photos
                         if(category == FileUtils.CATEGORY_PHOTOS_HOLDER)
                         {
                             UploadManager.enqueueImageCROrFailUploadQueue(queueCR, filePath, file.lastModified(),mimeType,UploadManager.uploadCRDestFolder, false);
                         }
                         
                     }
                }
            }
        }
    }


    public static int getsMediaObserversSize() 
    {
        if(sMediaObserverManager != null)
            return MediaObserverManager.mMediaObserver.size();
        else
            return 0;
    }
    
    
    public static HashSet<String> getExternalMounts() {
        final HashSet<String> out = new HashSet<String>();
        String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();
            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte[] buffer = new byte[1024];
            while (is.read(buffer) != -1) {
                s = s + new String(buffer);
            }
            is.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }

        // parse output
        final String[] lines = s.split("\n");
        for (String line : lines) {
            if (!line.toLowerCase(Locale.US).contains("asec")) {
                if (line.matches(reg)) {
                    String[] parts = line.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("/"))
                            if (!part.toLowerCase(Locale.US).contains("vold"))
                                out.add(part);
                    }
                }
            }
        }
        
        return out;
    }
};
   
        

