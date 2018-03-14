package com.mozy.mobile.android.activities.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

import org.apache.http.Header;

import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.streams.DataInputStream;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.ErrorManager;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.files.Video;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.StreamDownload;

public class FileDownloader 
{

    private CloudFile cloudFile;
    private LocalFile localFile;
    private DownloadComplete completedInterface;
    private boolean downloadAbort = false;
    private Context context;
    private ProgressDialog progress;
    private boolean isDeviceEncrypted;
    private boolean isSyzygyFormat;
    
    public FileDownloader(LocalFile inputLocalFile, 
                          CloudFile inputCloudFile, 
                          DownloadComplete inputCompletedInterface,
                          Context inputContext,
                          ProgressDialog progressDialog,
                          boolean isDeviceEncrypted,
                          boolean isSyzygyFormat)
    {
        this.localFile = inputLocalFile;
        this.cloudFile = inputCloudFile;
        this.completedInterface = inputCompletedInterface;
        this.context = inputContext;
        this.progress = progressDialog;
        this.isDeviceEncrypted = isDeviceEncrypted;
        this.isSyzygyFormat = isSyzygyFormat;
    }
    
    public int start(StringBuffer contentEncoding)
    {
        boolean succeeded = false;
        int status = ServerAPI.RESULT_CANCELED;  
        
        StreamDownload streamDownload = null;
        
        if (!this.downloadAbort)
        {
            streamDownload = ServerAPI.getInstance(this.context).downloadFile(cloudFile, isDeviceEncrypted, isSyzygyFormat);
            status = streamDownload.errorCode;   
        
            try
            {
                if (streamDownload.errorCode == ErrorCodes.NO_ERROR)
                {
                    localFile.createNewFile();
                    
                    if (!this.downloadAbort)
                    {
                        FileOutputStream outStream = new FileOutputStream(localFile.file);
            
                        byte[] tmp = new byte[2048];
                        int numBytes;
                        int totalBytes = 0;
                        
                        while ((numBytes = streamDownload.stream.read(tmp)) != -1) 
                        {
                            if (this.downloadAbort)
                            {
                                break;
                            }
                            outStream.write(tmp, 0, numBytes);
                            this.progress.setProgress((totalBytes += numBytes)/1024);
                        }
                        this.progress.setProgress(this.progress.getMax());
                        outStream.flush();
                        outStream.close();
                        
						//String contentEncoding;
                        Header[] responseHeaders = ((DataInputStream) streamDownload.stream).getAllHeaders();
                        for (int j = 0; j < responseHeaders.length; j++) {
                        	if (responseHeaders[j].getName().equals("Content-Encoding")) {
                        		//contentEncoding = responseHeaders[j].getValue();
                        		contentEncoding.append(responseHeaders[j].getValue());
                        		break;
                        	}
                        }
                        
                        if (this.downloadAbort)
                        {
                            localFile.delete();
                        }
                        else
                        {
                            succeeded = true;   
                          //  localFile.file.setLastModified(cloudFile.getUpdated());  We do not set the lastModified date anymore since it is unreliable
                          // for certain devices, bookkeeping is done in the database
                        }
                    }
                } // if (streamDownload.errorCode == ErrorCodes.NO_ERROR)
                else
                {          
                    ErrorManager.getInstance().reportError(streamDownload.errorCode, ErrorManager.ERROR_TYPE_GENERIC);
                }
            }
            catch (HttpException e)
            {
                if (e != null && e.getHttpErrorCode() == 415)
                {
                    ErrorManager.getInstance().reportError(ServerAPI.RESULT_ENCRYPTED_FILE, ErrorManager.ERROR_TYPE_GENERIC);
                }
                else
                {
                    ErrorManager.getInstance().reportError(ServerAPI.RESULT_UNKNOWN_ERROR, ErrorManager.ERROR_TYPE_GENERIC);
                }
            }
            catch (Throwable e)
            {
                LogUtil.exception(this, "WriteFile()", e);
            }    
        }
        
        // Do not call finished if the down-load was aborted.
        if (!this.downloadAbort)
        {
            try
            {
                if (succeeded)
                {
                    storePhotoOrVideo(this.cloudFile, this.localFile, this.context);
                }
                else
                {
                    localFile.delete();
                    succeeded = false;
                }
            }
            catch (Throwable e)
            {
                LogUtil.exception(this, "WriteFile()", e);
            }
        }
        else
        {
            succeeded = false;
             try
             {
                localFile.delete();
             }
             catch (Throwable e)
             {
                 LogUtil.exception(this, "WriteFile()", e);
             }
        }
        
        if(this.completedInterface != null) 
            status = this.completedInterface.finished(status);
        
        
        return status;
    }

    /**
     * 
     */
    private static void storePhotoOrVideo(CloudFile cloudFile, LocalFile localFile, Context context) {
        if (cloudFile instanceof Photo || cloudFile instanceof Video)
        {
            ContentValues image = new ContentValues();
            File parent = localFile.file.getParentFile();
            String path = parent.toString().toLowerCase(Locale.getDefault());
            String name = parent.getName().toLowerCase(Locale.getDefault());

            if (cloudFile instanceof Photo)
            {
                image.put(Images.Media.TITLE, localFile.file.getName());
                image.put(Images.Media.DISPLAY_NAME, localFile.file.getName());
                image.put(Images.Media.DESCRIPTION, localFile.file.getName());
                image.put(Images.Media.DATE_ADDED, localFile.file.lastModified());
                image.put(Images.Media.DATE_TAKEN, localFile.file.lastModified());
                image.put(Images.Media.DATE_MODIFIED, localFile.file.lastModified());
                image.put(Images.Media.MIME_TYPE, cloudFile.getMimeType());
                image.put(Images.Media.ORIENTATION, 0);
                image.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
                image.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
                image.put(Images.Media.SIZE, localFile.file.length());
                image.put(Images.ImageColumns.DATA, localFile.file.getAbsolutePath());
                context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
            }
            else
            {
                image.put(MediaStore.Video.Media.TITLE, localFile.file.getName());
                image.put(MediaStore.Video.Media.DISPLAY_NAME, localFile.file.getName());
                image.put(MediaStore.Video.Media.DESCRIPTION, localFile.file.getName());
                image.put(MediaStore.Video.Media.DATE_ADDED, localFile.file.lastModified());
                image.put(MediaStore.Video.Media.DATE_TAKEN, localFile.file.lastModified());
                image.put(MediaStore.Video.Media.DATE_MODIFIED, localFile.file.lastModified());
                image.put(MediaStore.Video.Media.MIME_TYPE, cloudFile.getMimeType());
                image.put(MediaStore.Video.VideoColumns.BUCKET_ID, path.hashCode());
                image.put(MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME, name);
                image.put(MediaStore.Video.Media.SIZE, localFile.file.length());
                image.put(MediaStore.Video.VideoColumns.DATA, localFile.file.getAbsolutePath());
                context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, image);
            }
        }
    }
    
    public void abort()
    {
        this.downloadAbort = true;
    }    
}
