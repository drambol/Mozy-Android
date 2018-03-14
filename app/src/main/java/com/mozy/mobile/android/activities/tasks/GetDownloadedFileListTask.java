package com.mozy.mobile.android.activities.tasks;

import java.io.File;
import com.mozy.mobile.android.R;
import java.util.ArrayList;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.FileUtils;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;

public class GetDownloadedFileListTask extends AsyncTask<Void, Void, Void>
{
    public static final int DOWNLOADED_DOCS = 0;
    public static final int DOWNLOADED_MUSIC = 1;
    public static final int DOWNLOADED_PHOTOS = 2;
    public static final int DOWNLOADED_VIDEOS = 3;
    public static final int DOWNLOADED_PHOTOS_OR_VIDEOS = 4;
  
    public static interface Listener {
        void onCompleted(ArrayList<Object> list);
    }
    
    protected final Context mContext;
    protected final String mRootDeviceTitle;
    protected final Listener listener;
    protected boolean  photosOnly;
    protected ArrayList<Object> list;

    public GetDownloadedFileListTask(Context context, String rootDeviceTitle, boolean photosOnly, Listener listener) 
    {
        this.mContext = context.getApplicationContext();
        this.mRootDeviceTitle = rootDeviceTitle;
        this.listener = listener;
        this.list =  new ArrayList<Object>();
        this.photosOnly = photosOnly;
    }
        
    protected Void doInBackground(Void... params) {
        
        if(photosOnly == false)
        {
            ArrayList<LocalFile> docfiles = getFilesForStorageDirectoryPath(this.mContext, this.mRootDeviceTitle, DOWNLOADED_DOCS);
            if(docfiles != null && docfiles.size() != 0)
            {
                this.list.add(this.mContext.getResources().getString(R.string.quicklist_downloaded_label_docs));
                for(int i = 0; i < docfiles.size(); i++)
                    this.list.add(docfiles.get(i));
            }
             
            ArrayList<LocalFile> musicfiles = getFilesForStorageDirectoryPath(this.mContext, this.mRootDeviceTitle, DOWNLOADED_MUSIC);
            if(musicfiles != null && musicfiles.size() != 0)
            {
                this.list.add(this.mContext.getResources().getString(R.string.quicklist_downloaded_label_music));
                for(int i = 0; i < musicfiles.size(); i++)
                    this.list.add(musicfiles.get(i));
                }
            }
    

            ArrayList<LocalFile> videofiles = getFilesForStorageDirectoryPath(this.mContext, this.mRootDeviceTitle, DOWNLOADED_PHOTOS_OR_VIDEOS);
            if(videofiles != null  && videofiles.size() != 0)
            {
                if(photosOnly == false)
                {
                     this.list.add(this.mContext.getResources().getString(R.string.quicklist_downloaded_label_pics));
                }
                for(int i = 0; i < videofiles.size(); i++)
                {
                    if(photosOnly == true)
                    {
                        String mimeType = FileUtils.getMimeTypeFromFileName(videofiles.get(i).getName());
                        int category = FileUtils.getCategory(mimeType);
                        if(category == FileUtils.CATEGORY_PHOTOS_HOLDER)
                            this.list.add(videofiles.get(i));
                    }
                    else
                        this.list.add(videofiles.get(i));
                }
            }
            
        return null;
    }
    
    @Override
    public void onPostExecute(Void postExec) 
    {
         listener.onCompleted(list);
    }
    
    
    /**
     * @param containerTitle
     * @param filetype
     * @return
     */
    private String getStoragePathForFileType(String containerTitle, final int filetype) 
    {
        String path = null;
        final String state = Environment.getExternalStorageState();
        
        if (Environment.MEDIA_MOUNTED.equals(state)) {
        
              File file = Environment.getExternalStorageDirectory();
             
              path = file.getPath();
              
              // Mozy Path
              
              path = path + "/Mozy";
              
              if (filetype == DOWNLOADED_PHOTOS_OR_VIDEOS)
              {
                  path = path  +"/Photos and Videos" + "/" + containerTitle;
              }
              else if (filetype == DOWNLOADED_MUSIC)
              {
                 path = path  + "/Music"+ "/" + containerTitle;
              }
              else if (filetype == DOWNLOADED_DOCS)
              {
                 path = path  + "/Docs" + "/" +  containerTitle;
              }
          }
        return path;
    }
    
    
    public ArrayList<LocalFile> getFilesForStorageDirectoryPath(Context context, String containerTitle,  final int filetype)
    {
        ArrayList <LocalFile> localFiles  = null;
        String path = getStoragePathForFileType(containerTitle, filetype);
        if(path != null)
        {
            File file = new File(path);
            localFiles = new ArrayList<LocalFile>();  
            File[] listOfFiles = file.listFiles();
            
            if(listOfFiles != null)    
            {
                for(int j = 0; j < listOfFiles.length && listOfFiles[j] != null; j++)
                {
                    LocalFile localfile =  new LocalFile(listOfFiles[j].getPath());
                    if(localfile != null && (localfile.file.isDirectory() == false))
                        localFiles.add(localfile);
                }
            }
        }
        return localFiles;
    }
}
