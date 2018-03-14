

package com.mozy.mobile.android.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import se.tactel.datahandler.DataManager;
import se.tactel.datahandler.HeadRequest;
import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.PutRequest;
import se.tactel.datahandler.api.HttpConnectionItem;
import android.content.Context;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.FileUploadEntity;
import com.mozy.mobile.android.web.containers.FileUploadState;
import com.mozy.mobile.android.web.containers.ProgressOutputStream.ProgressListener;
import com.mozy.mobile.android.web.containers.StringDownload;

public class uploadFileAPI extends MipAPI
{
    static uploadFileAPI sInstance;
    private PutRequest putrequest;
    FileUploadEntity entity;
    HttpConnectionItem  connect;
    
    private uploadFileAPI(Context context)
    {
        super(context);
        putrequest = null;
    }
    
    public static synchronized uploadFileAPI getInstance(Context context)
    {
        if(uploadFileAPI.sInstance == null)
            uploadFileAPI.sInstance = new uploadFileAPI(context.getApplicationContext());

        return uploadFileAPI.sInstance;
    }
    
    public static void setInstance(uploadFileAPI sInstance) {
        uploadFileAPI.sInstance = sInstance;
    }
    
    // Returns zero on success, else ErrorCodes.ERROR_*
    //
    // @param FileUploadState    file on phone to be uploaded
    // @param device            Files can only be uploaded to the "CLOUD" device.
    //
    // public int uploadFile(File localFile, Device device)
    public int uploadFile(FileUploadState fileState, String deviceLink, String syncPath, boolean syzygy, ProgressListener progressListener)
    {
        int returnCode=ErrorCodes.NO_ERROR;
        String getUrl=null;
        long fileStartOffset=0;

        try
        {
            ArrayList<Header> headers = new ArrayList<Header>();
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));

            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));

            // If fileState.linkToCloud is not null, then we are resuming a partially uploaded file.
            if (fileState.linkToCloud == null)
            {
                
                StringDownload getUrlResult = this.getUploadUrl(deviceLink, fileState.fileName, fileState.localFile.length(), fileState.getFinalFileSize(), syncPath, syzygy);
                getUrl = getUrlResult.string;
                returnCode = getUrlResult.errorCode;
            }
            else
            {
                getUrl = fileState.linkToCloud;

                Header[] headersArray = new Header[headers.size()];
                headersArray = headers.toArray(headersArray);

                // Find out how much of the file was previously uploaded
                HeadRequest request = new HeadRequest(getUrl, headersArray);
                DataManager.Params params = new DataManager.Params();
                params.setNumRetry(3);
                HttpConnectionItem connectionItem = this.dataConnection.sendDataSaveReturnData(request, params);

                Header[] responseHeaders = connectionItem.getAllHeaders();

                // Search all the headers for the "Content-Length", which will be the URI that we 'put' the file to.
                for (int j = 0; j < responseHeaders.length; j++)
                {
                    if (responseHeaders[j].getName().equals("Content-Length"))
                    {
                        fileStartOffset = Long.valueOf(responseHeaders[j].getValue()).longValue();
                        break;
                    }
                }
                // Add a new header for the later call
                headers.add(new BasicHeader("Content-Range", "bytes " + fileStartOffset + "-" + (fileState.localFile.length() - 1) + "/" + fileState.localFile.length()));
            }

            if (getUrl != null && returnCode == ErrorCodes.NO_ERROR)
            {

                entity = new FileUploadEntity(fileState, fileStartOffset, progressListener);

                if (syzygy) {
                	headers.add(new BasicHeader("content-encoding", "x-syzygy"));
                }
                Header[] headersArray = new Header[headers.size()];
                headersArray = headers.toArray(headersArray);
                this.putrequest = new PutRequest(getUrl, headersArray, entity);
                DataManager.Params params = new DataManager.Params();
                params.setNumRetry(3);
                connect = this.dataConnection.sendData(putrequest, params);
            }
        }
        catch (HttpException e)
        {
            returnCode = MipCommon.handleHttpException(e);
            fileState.setLinkToCloud(getUrl);      // May be null
        }
        catch (IOException ie)
        {
            returnCode = ErrorCodes.ERROR_HTTP_IO;
            fileState.setLinkToCloud(getUrl);        // May be null
        }
        catch (Throwable thr)
        {
            returnCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
            fileState.setLinkToCloud(getUrl);        // May be null
        }

        return returnCode;
    }
    
    
    // Files are only uploaded into a particular directory in the "cloud" container.
    private StringDownload getUploadUrl(String deviceLink, String fileName, long fileLength, long finalFileSize, String syncPath, boolean syzygy) throws IOException, UnsupportedEncodingException
    {
        StringDownload returnValue =  new StringDownload();
        returnValue.errorCode =  ErrorCodes.NO_ERROR;

        try
        {

            StringBuilder uriBuilder = new StringBuilder(getCatchAndReleaseLink(deviceLink, syncPath));
    
            // The name of the uploaded photo is the file name with a timestamp tacked on.
            StringBuilder cloudPathBuilder = new StringBuilder(fileName);
    
            // All files are uploaded rooted in a 'CatchAndRelease' folder in the cloud container.
            // String cloudPath = URLEncoder.encode(this.context.getString(R.string.upload_sync_path) + localFile.getCanonicalPath().replace(java.io.File.separatorChar, '/'), "UTF-8");
            String cloudPath = URLEncoder.encode(cloudPathBuilder.toString());
    
            // Make sure any slashes face the way that MIP expects them...
            //uriBuilder.append(URLEncoder.encode(localFile.getCanonicalPath().replace(java.io.File.separatorChar, '/'), "UTF-8"));
            uriBuilder.append(cloudPath);
    
            String uri = uriBuilder.toString();
    
            ArrayList<Header> headers = new ArrayList<Header>();
    
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));
    
            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
            
            if (syzygy) {
            	headers.add(new BasicHeader("content-encoding", "x-syzygy"));
            	headers.add(new BasicHeader("X-Final-File-Size", Long.toString(finalFileSize)));
            	uri += "&content-encoding=x-syzygy&type=baseline"; //also add content-encoding in uri to walk around a CAS bug
            }

            // Tell the server the size of the file
            headers.add(new BasicHeader("X-Eventual-Content-Length", Long.toString(fileLength)));
    
            Header[] headersArray = new Header[headers.size()];
            headersArray = headers.toArray(headersArray);
    
            PutRequest request = new PutRequest(uri, headersArray);
    
            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(3);
            HttpConnectionItem connectionItem = null;
            
            
            if (this.mipConnection != null) 
            {
                connectionItem = this.mipConnection.sendData(request, params);
            }
            else
            {
               throw new IOException("No http connection made.");
            }
    
            Header[] responseHeaders = connectionItem.getAllHeaders();
    
            // Search all the headers for the "Location", which will be the URI that we 'put' the file to.
            for (int j = 0; j < responseHeaders.length; j++)
            {
                if (responseHeaders[j].getName().equals("Location"))
                {
                    returnValue.string = responseHeaders[j].getValue();
                    break;
                }
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
            LogUtil.debug(this, "ErrorCode (HTTP): " + e.getHttpErrorCode());
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }

        return returnValue;
    }
    
    
    
    // Files are only uploaded into a particular directory in the "cloud" container.
    public StringDownload createUploadDir(String deviceLink, String syncPath, String newDirName) throws IOException, UnsupportedEncodingException
    {
        StringDownload returnValue =  new StringDownload();
        returnValue.errorCode =  ErrorCodes.NO_ERROR;
        if (newDirName.endsWith("/"))
        	newDirName = newDirName.substring(0, newDirName.length() - 1);

        try
        {

            StringBuilder uriBuilder = new StringBuilder(getCatchAndReleaseLink(deviceLink, syncPath));
   
            String dirName = URLEncoder.encode(newDirName.toString());
            
            uriBuilder.append(dirName);
            
            String parameterSeparator = "&";
            
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(uploadFileAPI.METADATA_CONTENT_TYPE_DIRECTORY);
            
            
          //  /1832065/fs/1338895?path=%2Fsync%2F1%2FMobile+Uploads%2FA+NEW+DIRECTORY&type=directory
    
            String uri = uriBuilder.toString();
    
            ArrayList<Header> headers = new ArrayList<Header>();
    
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));
    
            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
    
            Header[] headersArray = new Header[headers.size()];
            headersArray = headers.toArray(headersArray);
    
            PutRequest request = new PutRequest(uri, headersArray);
    
            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(3);
           
            if (this.mipConnection != null) 
            {
                this.mipConnection.sendData(request, params);
            }
            else
            {
               throw new IOException("No http connection made.");
            }

        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.translateHttpError(e.getHttpErrorCode());
            LogUtil.debug(this, "ErrorCode (HTTP): " + e.getHttpErrorCode());
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }

        return returnValue;
    }
    
    public String getCatchAndReleaseLink(String cloudDeviceLink, String syncPath)
    {
        StringBuilder uriBuilder = new StringBuilder(cloudDeviceLink);
        
        if(syncPath == null)
            syncPath = this.context.getString(R.string.upload_sync_path) + "/"; // end it with a slash
            
        uriBuilder.append(URLEncoder.encode(syncPath));

        return uriBuilder.toString();
    }
    
    public void abort()
    {
        if(this.putrequest != null)
            this.putrequest.abort();
    }
}



