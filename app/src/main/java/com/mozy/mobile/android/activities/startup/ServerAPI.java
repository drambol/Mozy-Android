
package com.mozy.mobile.android.activities.startup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.os.Bundle;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.client.DechoHttpClient;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.web.MipAPI;
import com.mozy.mobile.android.web.MipAPI.ThumbnailConnection;
import com.mozy.mobile.android.web.MipAuthAPI;
import com.mozy.mobile.android.web.uploadFileAPI;
import com.mozy.mobile.android.web.containers.FileUploadState;
import com.mozy.mobile.android.web.containers.IntDownload;
import com.mozy.mobile.android.web.containers.ListDownload;
import com.mozy.mobile.android.web.containers.ProgressOutputStream.ProgressListener;
import com.mozy.mobile.android.web.containers.StreamDownload;
import com.mozy.mobile.android.web.containers.StringDownload;


/**
 *
 * Various wrapper functions for calls that go to one of the servers. UI should call these methods as opposed
 * to calling the MipAPI functions directly
 *
 */
public class ServerAPI {

    private static ServerAPI sInstance = null;
    private Context context;
    private String cloudDeviceLink=null;

    private final String TAG = getClass().getName();


    /**
     * Result code for connection issue.
     */
    public static final int RESULT_CONNECTION_FAILED = -1;

    /**
     * Result ok code.
     */
    public static final int RESULT_OK = 0;

    /**
     * Result code notifying of mandatory update from this.checkForUpdate().
     */
    public static final int RESULT_MANDATORY_UPDATE_EXIST = 1;

    /**
     * Result code notifying of no update available
     */
    public static final int RESULT_NO_UPDATE_AVAILABLE = 2;

    /**
     * Result code notifying that account license is valid.
     */
    public static final int RESULT_ACCOUNT_LICENSE_VALID = 3;

    /**
     * Result code notifying that trial has expired.
     */
    public static final int RESULT_TRIAL_EXPIRED = 4;

    /**
     * Result code notifying that account has been suspended.
     */
    public static final int RESULT_ACCOUNT_SUSPENDED = 5;

    /**
     * Result code notifying that license is used elsewhere.
     */
    public static final int RESULT_LICENSE_USED_ELSEWHERE = 6;

    /**
     * Result code notifying that provisioning data was fetched.
     */
    public static final int RESULT_DATA_FETCHED = 7;

    /**
     * Result code notifying that user was not authorized.
     */
    public static final int RESULT_UNAUTHORIZED = 8;

    /**
     * Result code notifying that user was not authorized.
     */
    public static final int RESULT_UNKNOWN_PARSER = 9;

    /**
     * Result code catch-all for various exceptions that *should* never happen.
     */
    public static final int RESULT_UNKNOWN_ERROR = 10;

    /**
     * Result code notifying that the phone is roaming, and the user has specified that no uploads are allowed when roaming.
     */
    public static final int RESULT_ROAMING_NOT_ALLOWED = 11;

    /**
     * Result code notifying that phone is not on WIFI, and the user has specified that uploads are only allowed over WIFI.
     */
    public static final int RESULT_WIFI_ONLY = 12;

    /**
     * Result code notifying that the operation was canceled.
     */
    public static final int RESULT_CANCELED = 13;

    /**
     * Result code for empty file downloaded.
     */
    public static final int RESULT_ENCRYPTED_FILE = 14;
    
    /**
     * Result for trying to access an account on an unsupported partner.
     */
    public static final int RESULT_INVALID_PARTNER = 15;

    /**
     * Result for trying to access an account that has conflicting credentials
     */
    public static final int RESULT_ACCOUNT_CONFLICT = 16;
    
    
    public static final int RESULT_INVALID_CLIENT_VER = 17;
   
    
    public static final int RESULT_INVALID_TOKEN = 18;
    
    public static final int  RESULT_AUTHORIZATION_ERROR = 19;
    
    public static final int  RESULT_INVALID_USER = 20;
    
    public static final int  RESULT_FORBIDDEN = 21;
    
    public static final int  RESULT_CERTIFICATE_INVALID = 22;
    
    

    private ServerAPI(Context context)
    {
        LogUtil.debug(TAG, "Initializing ServerAPI");
        this.context = context;
        new DechoHttpClient(Provisioning.getInstance(context).bAcceptAllCertificates());
    }

    public static synchronized ServerAPI getInstance(Context context)
    {
        if (sInstance == null)
        {
            sInstance = new ServerAPI(context.getApplicationContext());
        }

        return sInstance;
    }

    private int ErrorCodeToResult(int errorCode)
    {
        int returnValue = RESULT_OK;

        switch(errorCode)
        {
            case ErrorCodes.ERROR_HTTP_BAD_GATEWAY:
            case ErrorCodes.ERROR_HTTP_SERVICE_UNAVAILABLE:
            case ErrorCodes.ERROR_HTTP_GATEWAY_TIMEOUT:
            case ErrorCodes.ERROR_HTTP_NOT_FOUND:
            case ErrorCodes.ERROR_HTTP_IO:
                returnValue = ServerAPI.RESULT_CONNECTION_FAILED;
                break;
            case ErrorCodes.ERROR_AUTH_INVALID_PARTNER:
                returnValue = ServerAPI.RESULT_INVALID_PARTNER;
                break;
            case ErrorCodes.ERROR_AUTH_ACCOUNT_CONFLICT:
                returnValue = ServerAPI.RESULT_ACCOUNT_CONFLICT;
                break;
            case ErrorCodes.ERROR_INVALID_CLIENT_VER:
                returnValue = ServerAPI.RESULT_INVALID_CLIENT_VER;
                break;
            case ErrorCodes.ERROR_HTTP_FORBIDDEN:
                returnValue = ServerAPI.RESULT_FORBIDDEN;
                
            case ErrorCodes.ERROR_HTTP_UNAUTHORIZED:
            case ErrorCodes.ERROR_HTTP_SERVER:
                returnValue = ServerAPI.RESULT_UNAUTHORIZED;
                break;
            case ErrorCodes.NO_ERROR:
                returnValue = ServerAPI.RESULT_OK;
                break;
            case ErrorCodes.ERROR_HTTP_PARSER:
            case ErrorCodes.ERROR_HTTP_UNKNOWN:
                returnValue = ServerAPI.RESULT_UNKNOWN_PARSER;
                break;
            case ErrorCodes.ERROR_ENCRYPTED_FILE:
                returnValue = ServerAPI.RESULT_ENCRYPTED_FILE;
                break;
            case ErrorCodes.ERROR_INVALID_TOKEN:
                returnValue = ServerAPI.RESULT_INVALID_TOKEN;
                break;
            case ErrorCodes.ERROR_AUTHORIZATION_ERROR:
                returnValue = ServerAPI.RESULT_AUTHORIZATION_ERROR;
                break;
            case ErrorCodes.ERROR_INVALID_USER:
                returnValue = ServerAPI.RESULT_INVALID_USER;
                break;
            default:
                returnValue = ServerAPI.RESULT_UNKNOWN_ERROR;
                break;
        }

        return returnValue;
    }

    /**
     * Listener objects for results of communications with server.
     */
    public interface Listener {
        public void onResult(int resultCode, Bundle data);
    }
    public interface ListenerWithObject {
        public void onResult(int resultCode, Object data);
    }


    // Returns ServerAPI.RESULT_OK on success, else ServerAPI.RESULT_* return-code.
    // Used to upload a file to the server. If there is an error, the caller can call this method again, passing in the
    // same 'fileState' parameter, and the upload will start uploading only the data left to upload.
    //
    // @param FileUploadState    Has a reference to file to be uploaded
    // @param listener            Callback interface to pass back the results
    //
    //
    public void uploadFile(final FileUploadState fileState, final String syncPath, final boolean syzygy, final ListenerWithObject listener, final ProgressListener progressListener)
    {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run()
            {
                int errorCode = ServerAPI.RESULT_OK;

                if (ServerAPI.this.cloudDeviceLink == null)
                {
                    errorCode = ServerAPI.this.setCloudDeviceLink(null);
                }
                if (errorCode == ServerAPI.RESULT_OK)
                {
                    int returnCode = (uploadFileAPI.getInstance(context)).uploadFile(fileState, ServerAPI.this.cloudDeviceLink, syncPath, syzygy, progressListener);
                    if (listener != null)
                    {
                        listener.onResult(ServerAPI.this.ErrorCodeToResult(returnCode), fileState);
                    }
                }
                else
                {
                    listener.onResult(errorCode, fileState);
                }
            }
        });

        thread.start();
    }
    
    
    /*
     * Returns the list of devices for the current user
     */
    public ListDownload getDevices()
    {
        ListDownload returnValue = MipAPI.getInstance(context).getDevices( Provisioning.getInstance(context).getMipAccountUserId());

        if (returnValue.errorCode == 0)
        {
            returnValue = orderDevices(returnValue);    
        }
              
        // Translate the error code
        if(returnValue != null)
            returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }
    
    
    
    private ListDownload orderDevices(ListDownload returnValue) {
        
        ArrayList <Object> devicesWithNoSync = new ArrayList<Object>();
        Device syncDevice = null;
        
        
        //Separate out the sync from list of devices
        
        if(returnValue != null && returnValue.list.size() != 0)
        {
            for (int i = 0; i < returnValue.list.size(); ++i)
            {
                Device device = (Device)returnValue.list.get(i);
    
                // Is this the cloud device?
                if (device.getSync())
                {
                    syncDevice = device;
                    SystemState.cloudContainer = device;
                }
                else
                {
                    devicesWithNoSync.add(device);
                }
                    
            }
            
            
            Collections.sort(devicesWithNoSync, new Comparator<Object>() {
                @Override
                public int compare(Object obj1, Object obj2) {
                    
                    Device d1 = (Device) obj1;
                    Device d2 = (Device) obj2;
                    if(d1 != null)
                    {
                        return (d1.getName()).compareToIgnoreCase(d2.getName());
                    }
                    return 0;
                }
            });
            
            returnValue.list.clear();
            if(syncDevice != null)
            {
                returnValue.list.add(syncDevice);
                
            }
            
            returnValue.list.addAll(devicesWithNoSync);
        }
        
        return returnValue;
    }

    public String getDeviceId(String title)
    {
       String deviceId = null;
        
       if(title != null)
       {
        
           ListDownload deviceList = this.getDevices();
           
           
           if(deviceList != null && deviceList.list != null)
           {
               for(int i = 0; i < deviceList.list.size(); i++)
                {
                   Device d = (Device) (deviceList.list.get(i));
                   if(title.equals(d.getTitle()) == true)
                   {
                       deviceId = d.getId();
                       break;
                   }
                }
           }
       }
       
       return deviceId;
    }

     
    public ListDownload authenticateUserWithCreds(String username, String password)
    {
        ListDownload returnValue = null;
        
        returnValue = MipAuthAPI.getInstance(context).authenticateUser(username, password);

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }
    
    
    public ListDownload getRequestForToken(String authServer, String partner_subdomain, String authCode, String redirectUri)
    {
        ListDownload returnValue = null;
        
        returnValue = MipAuthAPI.getInstance(context).getRequestForToken(authServer, partner_subdomain,authCode,redirectUri);
       

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }
    
    
 
    public ListDownload getUserInfo()
    {
        ListDownload returnValue = MipAPI.getInstance(context).getUserInfo();
        
        if(returnValue.errorCode != ServerAPI.RESULT_OK)  // clear token
        {
            this.setDecryptedFilesCleanupMgr(null);
        }
        else
        {
            // Save UserName
            Provisioning.getInstance(context).setMipAccountUserId((String) returnValue.list.get(0));
            
            // Save Managed Key URL, if any
            if((returnValue.list.size() >= 3) && (returnValue.list.get(1) != null))
            {
                SystemState.setManagedKeyUrl((String) returnValue.list.get(1));
            }
            
            Provisioning.getInstance(context).setEmailId((String) returnValue.list.get(2));
            
            if(returnValue.list.size() > 3) 
            {
                returnValue.list.subList(0, 3).clear();  // user id and managed key url
                SystemState.cacheDevicesAndCreateDB(context, returnValue);
                setCloudDeviceLink(returnValue);
                this.setDecryptedFilesCleanupMgr(returnValue);
            }
        }

        // Translate the error code
        if(returnValue != null)
            returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }
    

    // Returns one of ServerAPI.RESULT_*
    public int setCloudDeviceLink(ListDownload deviceList)
    {
        int returnCode = ServerAPI.RESULT_OK;
        

        if (deviceList != null && deviceList.list != null && (deviceList.errorCode == ErrorCodes.NO_ERROR || deviceList.errorCode == ServerAPI.RESULT_CONNECTION_FAILED))
        {
            for (int i = 0; i < deviceList.list.size(); ++i)
            {
                Device device = (Device)deviceList.list.get(i);

                // Is this the cloud device?
                /* Work around for server issues, we will look for the name 'sync' or the category cloud
                */
                if (device.getSync())
                {
                    synchronized(this)
                    {
                        this.cloudDeviceLink = device.getLink();
                        SystemState.cloudContainer = device;
                    }
                    enableUploadForSync();
                    break;
                }
            }
        }
        else
        {
            this.cloudDeviceLink = null;
            SystemState.cloudContainer = null;
            
            if(deviceList != null)
                returnCode = this.ErrorCodeToResult(deviceList.errorCode);
        }
           
        return returnCode;
    }

    /**
     * @param device
     */
    public void enableUploadForSync() {
        if (SystemState.isUploadEnabled())
        {
            // Enable/Disable Catch and Release
            SystemState.setManualUploadEnabled(true, context);
            // Check upload settings before we do the automatic C&R
            UploadSettings settings = new UploadSettings(context);
            if (settings.getAutoCandR())
            {
                UploadManager.startCatchAndRelease(context, MozyService.running);
            }
            else
            {
                UploadManager.initialize(context);  // No Catch and Release, but still support manual uploads so initialize
            }
        }
        else
        {
            // Enable/Disable Catch and Release
            SystemState.setManualUploadEnabled(false, context);
        }
    }
    
    
    
    public int setDecryptedFilesCleanupMgr(ListDownload deviceList)
    {
        int returnCode= ServerAPI.RESULT_OK;
        
        boolean disableCleanupMgr = true;
        
        if(deviceList != null)
        {
            if (deviceList.errorCode == ErrorCodes.NO_ERROR)
            {
                if(SystemState.hasEncryptedDevice(deviceList.list) == true)
                {
                 // Enable Mozy Service
                    SystemState.setMozyServiceEnabled(true, context);
                    MozyService.startDecryptedFilesCleanUp(context);
                    MozyService.enableDecryptedFilesCleanupMgr(context);
                    disableCleanupMgr = false;
                }
            }
            else
            {
                returnCode = this.ErrorCodeToResult(deviceList.errorCode);
            }
        }
        
        if(disableCleanupMgr)
        {
            MozyService.disableDecryptedFilesCleanupMgr(context);
        }
        
        return returnCode;
    }

    public String GetCloudDeviceLink()
    {
        return this.cloudDeviceLink;
    }

    /*
     * Downloads the cloud file and writes it to the standard download directory
     * This routine may call AlertDialog() and as such needs to run on the UI thread.
     *
     * @param cloudFile        File to be downloaded
     *
     * @param listener        Callback interface
     *
     * @param act            Activity that called from.
     *
     *
     */
    public StreamDownload downloadFile(CloudFile cloudFile, boolean isDeviceEncrypted, boolean isSyzygyFormat)
    {
        StreamDownload returnValue = MipAPI.getInstance(context).downloadFile(cloudFile.getLink(), isDeviceEncrypted, isSyzygyFormat);

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }

    // Returns the thumbnail for the specified file
    public ThumbnailConnection getThumbnail(String fileLink, int width, int height, long lastModified, long version)
    {
        ThumbnailConnection returnValue = MipAPI.getInstance(context).getThumbnail(fileLink, width, height, lastModified, version);

        return returnValue;
    }

    /*
     * Returns a list file and/or directory objects
     * @param containerLink         Is the 'link' field of the container of the files; This is a device or a directory.
     *                                 "https://server/userId/fs/deviceId/directory/"
     *                                 For Example: "https://mozy.com/912345/fs/165216/C:/"
     *
     * @param includeDirectories    If 'true' then include directories as well as files in the returned list.
     *
     * @param recurse                If 'true' then walk the whole tree rooted at containerLink to build the list of
     *                                 files. Else just return files contained directly in containerLink.
     *
     * @param titleQuery            Only include files in the returned list that have 'title' fields that start with
     *                                 titleQuery.
     *
      * @param maxResults            The maximum number of files that should be returned
      *
     * @param startIndex            The index of the first file to be returned in the list. This is used for pagination.
     *                                 Each 'page' of data that is returned from the server will also return an 'index'
     *                                 which will identify    the file that should start the next page.
     *
     * @param photosOnly            Only return 'photo' Cloud file objects
     *
     * query .
     */
    public ListDownload getFiles(String containerLink,
                                  boolean includeDirectories,
                                  boolean recurse,
                                  String titleQuery,
                                  int maxResults,
                                  String startIndex,
                                  boolean photosOnly)
    {
        ListDownload returnValue = MipAPI.getInstance(context).getFiles(containerLink,
                                                     includeDirectories,
                                                     recurse,
                                                     titleQuery,
                                                     maxResults,
                                                     startIndex,
                                                     photosOnly);

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }
    
    
    public ListDownload getFilesIncludingEmptyDirs(String containerLink,
            boolean includeDirectories,
            boolean recurse,
            String titleQuery,
            int maxResults,
            String startIndex,
            boolean photosOnly)
    {
        ListDownload returnValue = MipAPI.getInstance(context).getFilesIncludingEmptyDirs(containerLink,
                                   includeDirectories,
                                   recurse,
                                   titleQuery,
                                   maxResults,
                                   startIndex,
                                   photosOnly);
    
        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);
    
        return returnValue;
    }
    
    /*
     * Returns a list file and/or directory objects
     * @param containerLink         Is the 'link' field of the container of the files; This is a device or a directory.
     *                                 "https://server/userId/fs/deviceId/directory/"
     *                                 For Example: "https://mozy.com/912345/fs/165216/C:/"
     *
     * @param includeDirectories    If 'true' then include directories as well as files in the returned list.
     *
     * @param recurse                If 'true' then walk the whole tree rooted at containerLink to build the list of
     *                                 files. Else just return files contained directly in containerLink.
     *
     * @param titleQuery            Only include files in the returned list that have 'title' fields that start with
     *                                 titleQuery.
     *
      * @param maxResults            The maximum number of files that should be returned
      *
     * @param startIndex            The index of the first file to be returned in the list. This is used for pagination.
     *                                 Each 'page' of data that is returned from the server will also return an 'index'
     *                                 which will identify    the file that should start the next page.
     *
     * @param photosOnly            Only return 'photo' Cloud file objects
     *
     * query .
     */
    public ListDownload getFilesRecentlyAdded(String containerLink,
                                  boolean includeDirectories,
                                  int maxResults,
                                  String startIndex,
                                  boolean photosOnly)
    {
        ListDownload returnValue = MipAPI.getInstance(context).getRecentlyAddedFiles(containerLink,
                                                     includeDirectories,
                                                     maxResults,
                                                     startIndex,
                                                     photosOnly);
        

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }

    
    public ListDownload getExistingUploadedFiles(final Context context, final String dirLink, boolean foldersOnly) 
    {
       // get the list of all files in sync upload folder
        
        String startIndex  = null;
        ArrayList<Object>itemList= new ArrayList<Object>();

        ListDownload listDownload = null;
        
        final int MAX_RESULTS = 500;
        
        do
        {
            listDownload = ServerAPI.getInstance(context).getFilesIncludingEmptyDirs(dirLink, true, false, null, MAX_RESULTS, startIndex, false);
    
            if (listDownload.errorCode == ServerAPI.RESULT_OK) 
            {
                if(listDownload.list != null)
                {
                    
                    if(foldersOnly)
                    {
                       
                        for(int j = 0; j < listDownload.list.size(); j++)
                        {
                            CloudFile cloudFile = (CloudFile) listDownload.list.get(j);
                            if(cloudFile instanceof Directory)
                            {
                                itemList.add(cloudFile);
                            }
                        }
                        
                    }
                    else
                    {
                        // append results
                        itemList.addAll(listDownload.list);
                    }
                }
    
                startIndex = listDownload.nextIndex;
           }
           else
           {
               itemList = null;
               LogUtil.debug("getExistingUploadedFiles", "GetFiles error:" + listDownload.errorCode);
               startIndex = null;
           } 
        } 
        while(startIndex != null);
        
        //clear listDownload list and reinsert the itemlist
        
        if(listDownload != null && listDownload.list != null)
        {
            listDownload.list.clear();
        
            listDownload.list.addAll(itemList);
        }
            
        return listDownload;     
    }
   
    
    
    /*
     * Returns a Cloud file in a list
     * @param fileLink         
     */
    public ListDownload getCloudFileForFileLink(String fileLink)
    {
        ListDownload returnValue = MipAPI.getInstance(context).getCloudFileForFileLink(fileLink);
        
        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);
        
        return returnValue;
    }
    
    
    /**
     * @param localFile
     */
    public ListDownload getCloudFileForLocalFile(String deviceId, LocalFile localFile) {
        
        ListDownload list = null;

        String fileLink =  SystemState.mozyFileDB.getCloudFileLink(deviceId, localFile.getName());
        
        if(fileLink != null)
        {
    
            list = this.getCloudFileForFileLink(fileLink);
        }

        return list;
    }
    
    
    /*
     * Returns the number of files that match the parameters passed in
     *
     * @param containerLink         Is the 'link' field of the container of the files; This is a device or a directory.
     *                                 "https://server/userId/fs/deviceId?path=directory/"
     *                                 For Example: "https://mozy.com/912345/fs/165216/?path=C:/"
     *
     * @param includeDirectories    If 'true' then include directories as well as files in the returned list.
     *
     * @param recurse                If 'true' then walk the whole tree rooted at containerLink to build the list of
     *                                 files. Else just return files contained directly in containerLink.
     *
     * @param titleQuery            Only include files in the returned list that have 'title' fields that start with
     *                                 titleQuery.
     */
    public IntDownload getFileCount(String containerLink,
                boolean includeDirectories,
                boolean recurse,
                String titleQuery)
    {
        IntDownload returnValue = MipAPI.getInstance(context).getFileCount(containerLink, includeDirectories, recurse, titleQuery);

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }

    public int deleteFile(CloudFile cloudFile)
    {
        int returnValue = MipAPI.getInstance(context).deleteFile(cloudFile.getLink());

        // Translate the error code
        returnValue = this.ErrorCodeToResult(returnValue);

        return returnValue;
    }
    
    
    public int deleteToken(String token)
    {
        int returnValue = MipAPI.getInstance(context).deleteToken(token);

        // Translate the error code
        returnValue = this.ErrorCodeToResult(returnValue);

        return returnValue;
    }

    /*
     *
       *    @device            device containing object to be shared
       *  @path            full path to object
       *  @expiration     0 (for eternal), or seconds since epoch (integer UTC) of the time when this link should be removed automatically
       *  @receivers      comma/semicolon-separated list of mailids. OPTIONAL parameter
       *  @version        standard Mozy version, or null for latest version
     */
    public StringDownload getSharedLink(String deviceId, String path, long expiration, String receivers, String version)
    {
        StringDownload returnValue = MipAPI.getInstance(context).getSharedLink(Provisioning.getInstance(context).getMipAccountUserId(), deviceId, path, expiration, receivers, version);

        // Translate the error code
        returnValue.errorCode = this.ErrorCodeToResult(returnValue.errorCode);

        return returnValue;
    }

    /* Test code, walks cloud filesystem */
    public void WalkTree()
    {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run()
            {
                ListDownload deviceList = MipAPI.getInstance(context).getDevices(Provisioning.getUsername(context));

                for (int i = 0; i < deviceList.totalCount; ++i)
                {
                    Device device = (Device)deviceList.list.get(i);

                    LogUtil.debug(this, "Device Title: " + device.getTitle());

                    ListDownload fileList = MipAPI.getInstance(context).getFiles(device.getLink(),
                                                                                 true,  // Get directories also
                                                                                 false, // no recursion
                                                                                 null,    // no query
                                                                                 0,        // no limit on number of results
                                                                                 null,  // no starting index
                                                                                 false);// return all file types, not just photos

                    this.processFileList(fileList);
                }
            }

            public void processFileList(ListDownload fileList)
            {
                for (int i = 0; i < fileList.totalCount; ++i)
                {
                    CloudFile file = (CloudFile)fileList.list.get(i);

                    if (file instanceof Directory)
                    {
                        LogUtil.debug(this, "Directory Title: " + file.getTitle());

                        ListDownload nextList =  MipAPI.getInstance(context).getFiles(file.getLink(),
                                                                                    true,      // Get directories also
                                                                                    false,     // no recursion
                                                                                    null,    // no query
                                                                                    0,        // no limit on number of results
                                                                                    null,     // no starting index
                                                                                    false); // return all file types, not just photos

                        this.processFileList(nextList);
                    }
                    else
                    {
                        LogUtil.debug(this, "File Title: " + file.getTitle());
                    }
                }
            }
        });

        thread.start();
    }
}



