

package com.mozy.mobile.android.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import se.tactel.datahandler.DataManager;
import se.tactel.datahandler.DeleteRequest;
import se.tactel.datahandler.GetRequest;
import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.PostRequest;
import se.tactel.datahandler.api.HttpConnectionItem;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.client.MipHttpClient;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.Document;
import com.mozy.mobile.android.files.Music;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.files.Video;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.provisioning.ProvisioningListener;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.StreamUtil;
import com.mozy.mobile.android.web.containers.IntDownload;
import com.mozy.mobile.android.web.containers.ListDownload;
import com.mozy.mobile.android.web.containers.StreamDownload;
import com.mozy.mobile.android.web.containers.StringDownload;

public class MipAPI implements ProvisioningListener
{
    public static final String STR_HTTPS = "https://";

    private static final String METADATA_LINK_JSON_TAG = "link";
    private static final String METADATA_COUNT_JSON_TAG = "count";
    private static final String METADATA_ID_JSON_TAG = "id";
    private static final String METADATA_UPDATED_JSON_TAG = "updated";
    private static final String METADATA_TITLE_JSON_TAG = "title";
    private static final String METADATA_SIZE_JSON_TAG = "size";
    private static final String METADATA_ENTRY_JSON_TAG = "entry";
    private static final String METADATA_CUSTOM_PERMISSION_JSON_TAG = "custom_permission";
    private static final String METADATA_CONTAINER_JSON_TAG = "container";
    private static final String METADATA_EMAIL_ID_TAG = "email";
    private static final String METADATA_DELETED_JSON_TAG = "deleted";
    private static final String METADATA_NEXT_INDEX_JSON_TAG = "next-index";
    private static final String METADATA_JSON_VALUE_FALSE = "false";
    private static final String METADATA_CONTENT_TYPE_JSON_TAG = "content-type";
    protected static final String METADATA_CONTENT_TYPE_DIRECTORY = "type=directory";
    private static final String METADATA_PLATFORM = "platform";
    private static final String METADATA_QUOTA = "quota";
    private static final String METADATA_PUBLISHED = "published";
    private static final String METADATA_SYNC = "sync";
    private static final String METADATA_PATH = "path";
    private static final String METADATA_VERSIONS_JSON_TAG = "versions";
    private static final String METADATA_HIDDEN_FLAG_JSON_TAG = "hidden";
    private static final String METADATA_ENCRYPTED_FLAG_JSON_TAG = "encrypted";


    private static final String METADATA_VERSION_ID = "version-id";
    private static final String METADATA_MANAGED_KEY_URL = "managed-key-url";
    
    private static final String CONTAINER = "/container/";

    private static final String MIP_SYNTAX_CONTAINER = "container";
    private static final String MIP_SYNTAX_USER_INFO = "user_info";
    private static final String MIP_SYNTAX_FS = "/fs/";
    private static final String MIP_PARAMETER_INCLUDE_DIRECTORY = "include=dirs";
    private static final String MIP_PARAMETER_EXCLUDE_EMPTY_DIRS = "exclude=empty_dirs";
    private static final String MIP_PARAMETER_EXCLUDE_HIDDEN_FILES = "exclude=hidden";
    private static final String MIP_PARAMETER_REPR = "repr=";
    //private static final String MIP_PARAMETER_CIPHER = "Ciphertext=1";
    private static final String MIP_PARAMETER_CONTENT = "content";
    private static final String MIP_PARAMETER_SELECT = "q=SELECT%20";
    private static final String MIP_PARAMETER_RECURSE = "recurse=1";
    private static final String MIP_PARAMETER_COUNT = "count()";
    private static final String MIP_PARAMETER_ALL_PROPERTIES = "*";
    private static final String MIP_PARAMETER_MATCH = "%20MATCH%20";
    private static final String MIP_PARAMETER_START_INDEX = "start-index=";
    private static final String MIP_PARAMETER_MAX_RESULTS = "max-results=";
    private static final String MIP_PARAMETER_WHERE = "%20WHERE%20";
    private static final String MIP_PARAMETER_THUMBNAIL = "thumbnail:";
    private static final String MIP_PARAMETER_MODIFIEDSINCE = "modified-since=";
    //private static final String MIP_PARAMETER_CKEY = "config/ckey?container_id=";

    private static final String MIP_SHARE_URL = "share/link";
    private static final String MIP_SYNC_NAMESPACE = "sync/1/";
    private static final String MOBILE_ACCESS_PERMISSION = "enable_mobile_access";
    
    public static final int ACCESS_USERNAME_ID = 0;
    public static final int ACCESS_MANAGEDKEY_URL = 1;
    public static final int ACCESS_USER_EMAIL = 2;

    
    /*
    private static final String SHARED_LINK_JASON = "{" +
                                                        "\"container\":null," +
                                                        "\"path\":null," +
                                                        "\"expiration\":null," +
                                                        // The 'receivers_tag' field requires a value or we get a server error
                                                        "\"receivers_tag\":\"DummyName@DummyAddress\"," +
                                                        "\"version\":null" +
                                                    "}";
    */

    private String mipUrlBeginning;
    

    private static MipAPI sInstance = null;


    public static void setInstance(MipAPI sInstance) {
        MipAPI.sInstance = sInstance;
    }

    protected Context context;

    protected DataManager mipConnection = null;
    protected DataManager dataConnection = null;

   
    private String mMIPClientToken = null;   
    private String mMIPClientTokenSecret = null;

    protected String version = null;
   

    public static class ThumbnailConnection {
        private int errorCode;
        private boolean closed;
        private GetRequest request;
        private DataManager mipConnection;

        private ThumbnailConnection(DataManager mipConnection, GetRequest request) {
            closed = false;
            this.errorCode = ErrorCodes.NO_ERROR;
            this.request = request;
            this.mipConnection = mipConnection;
        }

        public void close() {
            closed = true;
            if (request != null) {
                request.abort();
            }
        }

        public int getErrorCode() {
            return errorCode;
        }

        public Bitmap execute() {
            if (closed) {
                if (request != null) {
                    request.abort();
                }
                return null;
            }
            Bitmap newBitmap = null;
            InputStream istream = null;
            try
            {
                if (request != null)
                {
                    if (this.mipConnection != null) 
                    {
                        istream = this.mipConnection.getData(request, new DataManager.Params());
                    }
                    else
                    {
                       throw new IOException("No http connection made.");
                    }

                    if (istream != null) {
                        newBitmap = BitmapFactory.decodeStream(istream);
                        if (newBitmap == null)
                        {
                            if (istream.available() == 0)
                            {
                                errorCode = ErrorCodes.ERROR_ENCRYPTED_FILE;
                                LogUtil.debug(this, "### EMPTY BITMAP FILE");
                            }
                            else
                            {
                                errorCode = ErrorCodes.ERROR_DOWNLOAD_UNKNOWN;
                                LogUtil.debug(this, "### BITMAP CREATION FAILED");
                            }
                        }
                        else
                        {
                            errorCode = ErrorCodes.NO_ERROR;
                            LogUtil.debug(this, "### BITMAP WITHOUT ERROR");
                        }
                    } else {
                        LogUtil.debug(this, "### NULL STREAM");
                    }
                }
                else
                {
                    LogUtil.debug(this, "### NULL REQUEST");
                    errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
                }
            }
            catch (HttpException e)
            {
                errorCode = MipCommon.translateHttpError(e.getHttpErrorCode());
                LogUtil.debug(this, "ErrorCode (HTTP): " + e.getHttpErrorCode());
                e.printStackTrace();
            }
            catch (IOException ie)
            {
                errorCode = ErrorCodes.ERROR_HTTP_IO;
                ie.printStackTrace();
            }
            catch (Throwable thr)
            {
                errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
                thr.printStackTrace();
            }
            finally
            {
                try
                {
                    istream.close();
                    closed = true;
                }
                catch (Exception e)
                {
                    LogUtil.debug(this, "Exception closing stream");
                }
            }
            return newBitmap;
        }
    }
    
    public static synchronized MipAPI getInstance(Context context)
    {

        if(MipAPI.sInstance == null)
            MipAPI.sInstance = new MipAPI(context.getApplicationContext());

        return MipAPI.sInstance;
    }
    

    /* START DEBUG CODE * /

    public class responseInterceptor implements HttpResponseInterceptor
    {
        public void process(HttpResponse response, HttpContext context)
        {
            int junk;
            junk = 4;
        }
    }
    / * END DEBUG CODE */

   

   
    public class requestInterceptorPostHandShake implements HttpRequestInterceptor
    {
        private String mClientKey;
        private String mClientToken;
        private String mOauth_Signature;
        private String mOauth_version="1.0" ;
            

        public requestInterceptorPostHandShake(Context contextMipAPI, String clientKey, String clientSecret, String token, String tokenSecret)
        { 
            mOauth_Signature = clientSecret  + "&" +  tokenSecret;
            mClientToken = token;
            mClientKey = clientKey;
        }

        public void process(HttpRequest request, HttpContext context)
        {
            if (!request.containsHeader("Authorization"))
            {
                request.addHeader("Authorization", "OAuth " + "oauth_consumer_key=" + mClientKey 
                        + ",oauth_token=" + mClientToken + ",oauth_signature="
                        + mOauth_Signature + ",oauth_version=" + mOauth_version 
                        + ",oauth_signature_method=PLAINTEXT");
                for (Header h : request.getAllHeaders())
                {
                    LogUtil.debug("HttpConnectionItem", "Request Headers: " + h.getName() + " :: " + h.getValue());
                }
            }
        }
    }

    // The following was copied from the DechoAPI class constructor
    protected MipAPI(Context context)
    {
        this.context = context;
       
        Provisioning provisioning = Provisioning.getInstance(context.getApplicationContext());
        
     
        this.mipUrlBeginning = MipCommon.buildMipUrlBeginningStr(this.context);
        
        MipCommon.mMIPClientSecret = provisioning.getOAuthSecret();

        mMIPClientToken = provisioning.getMipAccountToken();
        mMIPClientTokenSecret = provisioning.getMipAccountTokenSecret();
        
        // We should always have a token and token secret at this point
        if((mMIPClientToken != null && mMIPClientToken.length() != 0)  
                && (mMIPClientTokenSecret != null && mMIPClientTokenSecret.length() != 0))
        {
            this.mipConnection = getMIPConnectionPostHandShake(context, MipCommon.mMIPClientKey, MipCommon.mMIPClientSecret, 
                    mMIPClientToken, mMIPClientTokenSecret);
        }


        if(this.dataConnection == null)
            this.dataConnection = MipCommon.getDataConnection(context);

        Provisioning.getInstance(this.context).registerListener(this);
        
        
        try {
            this.version = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    
    private DataManager getMIPConnectionPostHandShake(Context context, String key, String secret,  String token, String tokenSecret)
    {
        UsernamePasswordCredentials up = new UsernamePasswordCredentials(key, secret);

        AuthScope authScope = new AuthScope(null, -1);
        
        Provisioning provisioning = Provisioning.getInstance(context);
        MipHttpClient client = new MipHttpClient(provisioning.bAcceptAllCertificates(),
                                                    provisioning.getMipPort());
        client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
        client.addRequestInterceptor(new requestInterceptorPostHandShake(this.context,key, secret, token, tokenSecret));
        /* DEBUG CODE * /
        client.addResponseInterceptor(new responseInterceptor());
        / * END DEBUG CODE */
        return new DataManager(null, new DataManager.HttpParams().setAuthScope(authScope).setHttpClient(client).setCredentials(up), context);
    }
    


 
    

    // Implementation of ProvisioningListener. Need listen to changes in certain provision settings and
    // recreate this instance if so.
    public void onChange(int id)
    {
//        if (id == ProvisioningListener.ACCOUNT_INFO_CHANGE)
//        {
//            Provisioning provisioning = Provisioning.getInstance(this.context);
//            provisioning.unregisterListener(this);
//            MipAPI.sInstance = new MipAPI(this.context, provisioning.getEmailId(), provisioning.getMipAccountPassword());
//        }
    }


    /*
     * Returns the list of devices for a user
     * The data returned from the server is in JSON format and is translated into a list of 'Device' instances.
     * For example:
     * {
     *     "entry": [
     *         {
     *             "category": "cloud",
     *             "path-separator": "/",
     *             "title": "Cloud",
     *             "file-count": 309147,
     *             "platform": "linux",
     *             "link": "https://mozy.com/912345/container/165215",
     *             "published": "2010-01-05T20:07:38Z",
     *             "id": 165215,
     *             "size": 2667473728
     *         }
     *     ]
     * }
     * Note that the "link" field for a device is in a different format than the "link" field for directories
     * or files. This method will convert the "link" field to be consistent with directories and files
    */
    public ListDownload getDevices(String username)
    {
        ListDownload returnValue = new ListDownload();
        ArrayList<Object> list = null;

        String uri = this.mipUrlBeginning + "/" + username + "/" + MipAPI.MIP_SYNTAX_CONTAINER + "/";

        // String uri = Provisioning.getInstance(context).getAccountIdUrl() + MipAPI.MIP_SYNTAX_CONTAINER;
        
        GetRequest request = getRequestForURI(uri);

        InputStream istream = null;
        try
        {
            if (this.mipConnection != null) 
            {
                istream = this.mipConnection.getData(request, new DataManager.Params());
            }
            else
            {
               throw new IOException("No http connection made.");
            }


            if (istream != null)
            {
                String jsonString = StreamUtil.JsonStreamToString(istream);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();

                JSONArray listOfDevices = jsonObject.getJSONArray(METADATA_ENTRY_JSON_TAG);

                int numberOfEntries = listOfDevices.length();
                list = new ArrayList<Object>(numberOfEntries);

                for (int i = 0; i < numberOfEntries; ++i)
                {
                    createDeviceAndAddInList(list, listOfDevices, i);
                }

                returnValue.setData(numberOfEntries, list);
            }
            else
            {
                returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Exception e)
            {
                LogUtil.debug(this, "Exception closing stream");
            }
        }

        LogUtil.debug(this, "ErrorCode: " + returnValue.errorCode);

        return returnValue;
    }


    /**
     * @param list
     * @param listOfDevices
     * @param i
     * @throws JSONException
     */
    public void createDeviceAndAddInList(ArrayList<Object> list,
            JSONArray listOfDevices, int i) throws JSONException {
        JSONObject jsonObject;
        Device device;
        jsonObject = listOfDevices.getJSONObject(i);

        // 'cloud' containers have no "updated" tag
        String updatedString = jsonObject.has(METADATA_UPDATED_JSON_TAG) ? jsonObject.getString(METADATA_UPDATED_JSON_TAG) : null;
        long updated = 0;
        if (updatedString != null)
        {
            try
            {
                SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = dateFormatter.parse(updatedString);
                updated = date.getTime();
            }
            catch (Exception e)
            {
                LogUtil.exception(this, "getDevices()", e);
            }
        }
        String title = jsonObject.getString(METADATA_TITLE_JSON_TAG);
        String id = jsonObject.getString(METADATA_ID_JSON_TAG);
        String platform = jsonObject.getString(METADATA_PLATFORM);
        String published = jsonObject.getString(METADATA_PUBLISHED);
        String quotaS = jsonObject.has(METADATA_QUOTA) ? jsonObject.getString(METADATA_QUOTA) : null;
        //String quotaS = jsonObject.getString(METADATA_QUOTA);
        boolean sync = jsonObject.has(METADATA_SYNC) ? jsonObject.getBoolean(METADATA_SYNC): false;
        long quota = (quotaS != null) ? Long.parseLong(quotaS) : 0;

        // 'Devices' can be on any data-center, so the only way of getting the correct domain to access the device
        // is to use the 'link' field that is returned in the device meta-data.
        String link = jsonObject.getString(METADATA_LINK_JSON_TAG);
        // Convert the 'link'
        // from
        // "https://mozy.com/912345/container/165215"
        // to
        // "https://mozy.com/912345/fs/165215"
        // to be consistent with other objects 'link'
        link = link.replace(CONTAINER, MIP_SYNTAX_FS);
        // Need to drill down through the sync namespace
        if (sync)
        {
            link += "?path=%2Fsync%2F1%2F";
        }
        else
        {
            link += "/";
        }

        long size = Long.parseLong(jsonObject.getString(METADATA_SIZE_JSON_TAG));

        String encryptedString = jsonObject.has(METADATA_ENCRYPTED_FLAG_JSON_TAG) ? jsonObject.getString(METADATA_ENCRYPTED_FLAG_JSON_TAG) : null;
        boolean encrypted = ((encryptedString != null) && (!encryptedString.equalsIgnoreCase(METADATA_JSON_VALUE_FALSE)));
        encrypted = encrypted || (jsonObject.has(METADATA_MANAGED_KEY_URL) && jsonObject.getString(METADATA_MANAGED_KEY_URL).equalsIgnoreCase("") == false);

        device = new Device(updated, title, link, size, id, quota, published, platform, sync, encrypted);

        list.add(device);
    }

    /**
     * @param uri
     * @return
     */
    protected GetRequest getRequestForURI(String uri) {
        ArrayList<Header> headers = new ArrayList<Header>();
        
        headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));

        headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
        
        Header[] headersArray = new Header[headers.size()];
        headersArray = headers.toArray(headersArray);

        GetRequest request = new GetRequest(uri, headersArray);
        return request;
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
     * The data returned from the server is in JSON format and is translated into a list of 'File' instances.
     * For example:
     * {
       * "category": [],
     *   "count": 4,
     *   "stored": "2010-04-07T17:21:04Z",
     *   "link": "https://mozy.com/912345/fs/165215/F:/images/",
     *   "etag": "0725d46590add9a41c4b4831eb4cd78da494a5a0",
     *   "entry": [
     *     {
     *       "path": "D:\\a sub dir",
     *       "content-type": "application/json; type=directory",
     *       "link": "https://mozy.com/912345/fs/165216/ D:/a%20sub%20dir/",
     *       "id": "fs/165216/D:/a sub dir/",
     *       "title": "a sub dir"
     *     },
     *     {
     *       "version-id": "1270069027",
     *       "updated": "2010-03-31T20:56:28Z",
     *       "title": "flower.jpg",
     *       "deleted": false,
     *       "stored": "2010-03-31T20:57:07Z",
     *       "etag": "\"1270069027000000\"",
     *       "link": "https://mozy.com/912345/fs/165215?path=F%3A/images/flower.jpg&version=12345",
     *       "published": "2010-03-31T20:56:28Z",
     *       "path": "F:\\images\\flower.jpg",
     *       "content-type": "image/jpeg",
     *       "id": "fs/165215/F:/images/flower.jpg",
     *       "size": 45656
     *     }
     *   ],
     *   "id": "fs/165215/F:/images/"
     * }
     */
    public ListDownload getFiles(String containerLink,
                                 boolean includeDirectories,
                                 boolean recurse,
                                 String titleQuery,
                                 int maxResults,
                                 String startIndex,
                                 boolean photosOnly)
    {


        // Provisioning provisioning = Provisioning.getInstance(context);
       // boolean showHidden = provisioning.getHiddenFilesMode();

        String uri = this.buildUri(containerLink, includeDirectories, false, recurse, titleQuery, false, maxResults, startIndex);

        return getFilesRequest(includeDirectories, photosOnly, uri);
    }
    
    public ListDownload getFilesIncludingEmptyDirs(String containerLink,
            boolean includeDirectories,
            boolean recurse,
            String titleQuery,
            int maxResults,
            String startIndex,
            boolean photosOnly)
    {


       // Provisioning provisioning = Provisioning.getInstance(context);
       // boolean showHidden = provisioning.getHiddenFilesMode();

        String uri = this.buildUriIncludeEmptyDir(containerLink, includeDirectories, false, recurse, titleQuery, false, maxResults, startIndex);

        return getFilesRequest(includeDirectories, photosOnly, uri);
        
    }
    
    
    public ListDownload getRecentlyAddedFiles(String containerLink,
            boolean includeDirectories,
            int maxResults,
            String startIndex,
            boolean photosOnly)
    {

        String uri = this.buildUriForFilesSinceLastUpdate(containerLink, true, null, false, maxResults,startIndex);
        
        return getFilesRequest(includeDirectories, photosOnly, uri);
    }


    /**
     * @param includeDirectories
     * @param photosOnly
     * @param uri
     * @return
     */
    public ListDownload getFilesRequest(boolean includeDirectories, boolean photosOnly, String uri) 
    {
        ListDownload returnValue = new ListDownload();
        ArrayList<Object> list = null;
        String nextIndex = null;
        
        GetRequest request = getRequestForURI(uri);

        InputStream istream = null;
        try
        {
            if (this.mipConnection != null) 
            {
                istream = this.mipConnection.getData(request, new DataManager.Params());
            }
            else
            {
               throw new IOException("No http connection made.");
            }

            if (istream != null)
            {
                String jsonString = StreamUtil.JsonStreamToString(istream);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();

                // Return the 'next-index' property if it exists. This is used for pagination
                if (jsonObject.has(METADATA_NEXT_INDEX_JSON_TAG))
                {
                    nextIndex = jsonObject.getString(METADATA_NEXT_INDEX_JSON_TAG);
                }

                JSONArray listOfFiles = jsonObject.getJSONArray(METADATA_ENTRY_JSON_TAG);

                int numberOfEntries = listOfFiles.length();
                list = new ArrayList<Object>(numberOfEntries);

                CloudFile file;
                int actualNumberOfFiles = 0;
                for (int i = 0; i < numberOfEntries; ++i)
                {
                    file = this.createCloudFileObject(listOfFiles.getJSONObject(i), includeDirectories, photosOnly);
                    if (file != null)
                    {
                        list.add(file);
                        actualNumberOfFiles++;
                    }
                }

                returnValue.setData(actualNumberOfFiles, list, nextIndex);
            }
            else
            {
                returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Exception e)
            {
                LogUtil.debug(this, "Exception closing stream");
            }
        }

        LogUtil.debug(this, "ErrorCode: " + returnValue.errorCode);

        return returnValue;
    }
    
    
    
    
    /*
     * Returns a list file and/or directory objects
     * @param fileLink         Is the 'link' field of file; 
     *                               
     *
     * The data returned from the server is in JSON format and is translated into a list of 'File' instances.
     * For example:
     *     {
     *       "version-id": "1270069027",
     *       "updated": "2010-03-31T20:56:28Z",
     *       "title": "flower.jpg",
     *       "deleted": false,
     *       "stored": "2010-03-31T20:57:07Z",
     *       "etag": "\"1270069027000000\"",
     *       "link": "https://mozy.com/912345/fs/165215?path=F%3A/images/flower.jpg&version=12345",
     *       "published": "2010-03-31T20:56:28Z",
     *       "path": "F:\\images\\flower.jpg",
     *       "content-type": "image/jpeg",
     *       "id": "fs/165215/F:/images/flower.jpg",
     *       "size": 45656
     *     }
     *   ],
     *   "id": "fs/165215/F:/images/"
     * }
     */
    public ListDownload getCloudFileForFileLink(String fileLink)
    {
        ListDownload returnValue = new ListDownload();
        ArrayList<Object> list = null;
        CloudFile file = null;
        
        
        String uri = this.buildUri(fileLink, false, false, false, null, false, 0, null);

        GetRequest request = getRequestForURI(uri);

        InputStream istream = null;
        try
        {

            if (this.mipConnection != null) 
            {
                istream = this.mipConnection.getData(request, new DataManager.Params());
            }
            else
            {
               throw new IOException("No http connection made.");
            }

            if (istream != null)
            {
                String jsonString = StreamUtil.JsonStreamToString(istream);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();


             

                list = new ArrayList<Object>(1);

                file = this.createCloudFileObject(jsonObject, false, false);
                list.add(file);

                returnValue.setData(1, list);
            }
            else
            {
                returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Exception e)
            {
                LogUtil.debug(this, "Exception closing stream");
            }
        }

        LogUtil.debug(this, "ErrorCode: " + returnValue.errorCode);

        return returnValue;
    }
    

    
    private String buildUri(String containerLink,
            boolean includeDirectories,
            boolean includeHidden,
            boolean recurse,
            String titleQuery,
            boolean count,
            int maxResults,
            String startIndex)
    {
        return buildUriBase(containerLink,
                includeDirectories,
                true,  // default
                includeHidden,
                recurse,
                titleQuery,
                count,
                maxResults,
                startIndex);
    }
    
    private String buildUriIncludeEmptyDir(String containerLink,
            boolean includeDirectories,
            boolean includeHidden,
            boolean recurse,
            String titleQuery,
            boolean count,
            int maxResults,
            String startIndex)
    {
        return buildUriBase(containerLink,
                includeDirectories,
                false,  // default
                includeHidden,
                recurse,
                titleQuery,
                count,
                maxResults,
                startIndex);
    }
    
    

    private String buildUriBase(String containerLink,
                            boolean includeDirectories,
                            boolean excludeEmptyDirs,
                            boolean includeHidden,
                            boolean recurse,
                            String titleQuery,
                            boolean count,
                            int maxResults,
                            String startIndex)
    {
        String parameterSeparator = (containerLink.lastIndexOf('?') != -1) ? "&" : "?";


        // Don't want to urlEncode the containerLink as it may already be urlEncoded
        StringBuilder uriBuilder = new StringBuilder();

        // Should include directories in the results...
        if (includeDirectories)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MipAPI.MIP_PARAMETER_INCLUDE_DIRECTORY);
            parameterSeparator = "&";
            if ((titleQuery == null) || (titleQuery.trim().length() == 0))
            {
                if(includeHidden == false)
                {
                    uriBuilder.append(parameterSeparator);
                    uriBuilder.append(MipAPI.MIP_PARAMETER_EXCLUDE_HIDDEN_FILES);
                }
                
                if(excludeEmptyDirs)
                {
                    uriBuilder.append(parameterSeparator);
                    uriBuilder.append(MipAPI.MIP_PARAMETER_EXCLUDE_EMPTY_DIRS);
                }
            }
        }
        else if(includeHidden == false)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MipAPI.MIP_PARAMETER_EXCLUDE_HIDDEN_FILES);
            parameterSeparator = "&";
        }

        // Recurse into subdirectories...
        if (recurse)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MipAPI.MIP_PARAMETER_RECURSE);
            parameterSeparator = "&";
        }

        // Return the number of results, not the results themselves...
        if (count)
        {
            uriBuilder.append(parameterSeparator);
            StringBuilder tempBuilder = new StringBuilder(MipAPI.MIP_PARAMETER_SELECT);
            tempBuilder.append(MipAPI.MIP_PARAMETER_COUNT);
            uriBuilder.append(tempBuilder.toString());
            parameterSeparator = "&";
        }

        // Only return results whose 'title' property starts with a given string...
        if ((titleQuery != null) && (titleQuery.trim().length() != 0))
        {
            StringBuilder tempBuilder = new StringBuilder();

            // The 'count' parameter will already have added the "SELECT" statement...
            if (!count)
            {
                uriBuilder.append(parameterSeparator);
                tempBuilder.append(MipAPI.MIP_PARAMETER_SELECT);
                tempBuilder.append(MipAPI.MIP_PARAMETER_ALL_PROPERTIES);
            }
            tempBuilder.append(MipAPI.MIP_PARAMETER_WHERE);
            tempBuilder.append(METADATA_TITLE_JSON_TAG);
            tempBuilder.append(MIP_PARAMETER_MATCH);
            tempBuilder.append("'");

            // Split the 'query' string on spaces and search for each word ORed
            String[] words = titleQuery.split(" +");
            
            for (int i = 0; i < words.length; ++i)
            {          
                words[i] = URLEncoder.encode(words[i]);
            }


            tempBuilder.append(words[0]);
            // NOT SUPPORTED YET ON SERVER tempBuilder.append("*");            // Wildcard, so we search for all titles beginning with the word

            for (int i = 1; i < words.length; ++i)
            {
                tempBuilder.append("%20OR%20");
                tempBuilder.append(words[i]);
             //   tempBuilder.append("*");
            }

            tempBuilder.append("'");

            // uriBuilder.append(URLEncoder.encode(tempBuilder.toString()));
            uriBuilder.append(tempBuilder.toString());

            parameterSeparator = "&";
        }

        // Set the index to start returning results from, used for pagination...
        if (startIndex != null)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MIP_PARAMETER_START_INDEX);
            String uri = Uri.encode(startIndex);
            uriBuilder.append(uri);
            parameterSeparator = "&";
        }

        // Limit the number of results
        if (maxResults != 0)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MIP_PARAMETER_MAX_RESULTS);
            uriBuilder.append(Integer.toString(maxResults));
            parameterSeparator = "&";
        }

        return containerLink + uriBuilder.toString();
    }
    
    
    private String buildUriForFilesSinceLastUpdate(String containerLink,
            boolean recurse,
            String titleQuery,
            boolean count,
            int maxResults,
            String startIndex)
    {
        String parameterSeparator = (containerLink.lastIndexOf('?') != -1) ? "&" : "?";
        
        
        // Don't want to urlEncode the containerLink as it may already be urlEncoded
        StringBuilder uriBuilder = new StringBuilder();
       
        
        // Recurse into subdirectories...
        if (recurse)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MipAPI.MIP_PARAMETER_RECURSE);
            parameterSeparator = "&";
        }
        
        // Return the number of results, not the results themselves...
        if (count)
        {
            uriBuilder.append(parameterSeparator);
            StringBuilder tempBuilder = new StringBuilder(MipAPI.MIP_PARAMETER_SELECT);
            tempBuilder.append(MipAPI.MIP_PARAMETER_COUNT);
            uriBuilder.append(tempBuilder.toString());
            parameterSeparator = "&";
        }
        
        // Only return results whose 'title' property starts with a given string...
        if ((titleQuery != null) && (titleQuery.trim().length() != 0))
        {
        StringBuilder tempBuilder = new StringBuilder();
        
        // The 'count' parameter will already have added the "SELECT" statement...
        if (!count)
        {
            uriBuilder.append(parameterSeparator);
            tempBuilder.append(MipAPI.MIP_PARAMETER_SELECT);
            tempBuilder.append(MipAPI.MIP_PARAMETER_ALL_PROPERTIES);
        }
        tempBuilder.append(MipAPI.MIP_PARAMETER_WHERE);
        tempBuilder.append(METADATA_TITLE_JSON_TAG);
        tempBuilder.append(MIP_PARAMETER_MATCH);
        tempBuilder.append("'");
        
        // Split the 'query' string on spaces and search for each word ORed
        String[] words = titleQuery.split(" +");
        
        tempBuilder.append(words[0]);
        // NOT SUPPORTED YET ON SERVER tempBuilder.append("*");            // Wildcard, so we search for all titles beginning with the word
        
        for (int i = 1; i < words.length; ++i)
        {
        tempBuilder.append("%20OR%20");
        tempBuilder.append(words[i]);
        //   tempBuilder.append("*");
        }
        
        tempBuilder.append("'");
        
        // uriBuilder.append(URLEncoder.encode(tempBuilder.toString()));
        uriBuilder.append(tempBuilder.toString());
        
        parameterSeparator = "&";
        }
        
     // Set the index to start returning results from, used for pagination...
        if (startIndex != null)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MIP_PARAMETER_START_INDEX);
            String uri = Uri.encode(startIndex);
            uriBuilder.append(uri);
            parameterSeparator = "&";
        }
       
        
        // Limit the number of results
        if (maxResults != 0)
        {
            uriBuilder.append(parameterSeparator);
            uriBuilder.append(MIP_PARAMETER_MAX_RESULTS);
            uriBuilder.append(Integer.toString(maxResults));
            parameterSeparator = "&";
        }
        
        Date now = new Date();
        long nowTimeL = now.getTime();
        
        long sevendaystime = 24*60*60*1000*7;
        
        long SevenDaysAgoTimeL = nowTimeL - sevendaystime;
        
        Date dateLastMod = new Date(SevenDaysAgoTimeL);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
        String lastModifiedDate = dateFormatter.format(dateLastMod);       
               
        if (lastModifiedDate != null)
        {
            try
            {
                uriBuilder.append(parameterSeparator);
                uriBuilder.append(MIP_PARAMETER_MODIFIEDSINCE);
                uriBuilder.append(lastModifiedDate);
                parameterSeparator = "&";
            }
            catch (Exception e)
            {
                LogUtil.exception(this, "getDevices()", e);
            }
        }

        return containerLink + uriBuilder.toString();
}

    // Parse the JSON object
    private CloudFile createCloudFileObject(JSONObject jsonObject, boolean includeDirectories, boolean photosOnly) throws Exception
    {
        CloudFile returnValue = null;

        String title = jsonObject.getString(METADATA_TITLE_JSON_TAG);
        String link = jsonObject.getString(METADATA_LINK_JSON_TAG);
        String path = jsonObject.getString(METADATA_PATH);
        
        
        String mimeType = jsonObject.getString(METADATA_CONTENT_TYPE_JSON_TAG);

        // Strip the filename off the path. Remember paths may have slashes in either direction depending on the OS
        // that they were uploaded from.
        int fileNameIndex = path.lastIndexOf(title);
        if (fileNameIndex != -1)    // Should never happen
        {
            path = path.substring(0, fileNameIndex);
            
            if (path.startsWith("/" + MIP_SYNC_NAMESPACE)) {
                if (mimeType.endsWith(METADATA_CONTENT_TYPE_DIRECTORY))
                {
                    path = path.replace("/" + MIP_SYNC_NAMESPACE, title + "/");
                }
                else
                {
                    path = path.replace("/" + MIP_SYNC_NAMESPACE, "/");
                }
            }
        }

        String deletedString = jsonObject.has(METADATA_DELETED_JSON_TAG) ? jsonObject.getString(METADATA_DELETED_JSON_TAG) : null;
        boolean deleted = ((deletedString != null) && (!deletedString.equalsIgnoreCase(METADATA_JSON_VALUE_FALSE)));

        if (includeDirectories)
        {
            if (mimeType.endsWith(METADATA_CONTENT_TYPE_DIRECTORY))
            {
                if (!title.equals(""))
                    returnValue = new Directory(link, title, deleted, path);
                else
                    return null;
            }
        }

        if (returnValue == null)
        {
            long updated = 0;
            String updatedString = jsonObject.has(METADATA_UPDATED_JSON_TAG) ? jsonObject.getString(METADATA_UPDATED_JSON_TAG) : null;
            if (updatedString != null)
            {
                try
                {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",  Locale.getDefault());
                    Date date = dateFormatter.parse(updatedString);
                    updated = date.getTime();
                }
                catch (Exception e)
                {
                    LogUtil.exception(this, "createFileObject()", e);
                }
            }

            // File Version
            long version = jsonObject.has(METADATA_VERSION_ID) ? jsonObject.getLong(METADATA_VERSION_ID) : 0;

            // Hidden File Check
            JSONArray versions = jsonObject.getJSONArray(METADATA_VERSIONS_JSON_TAG);
            JSONObject item = versions.getJSONObject(0);
            String hiddenString = item.has(METADATA_HIDDEN_FLAG_JSON_TAG) ? item.getString(METADATA_HIDDEN_FLAG_JSON_TAG) : null;
            boolean hidden = ((hiddenString != null) && (!hiddenString.equalsIgnoreCase(METADATA_JSON_VALUE_FALSE)));
            
            
       //     Provisioning provisioning = Provisioning.getInstance(context);
       //     boolean showHidden = provisioning.getHiddenFilesMode();
            
            
            // We continue to parse the meta data till triton decides to support the query for exclude hidden files
//            if (!hidden || (hidden && showHidden))
            if(!hidden)
            {
                long size = Long.parseLong(jsonObject.getString(METADATA_SIZE_JSON_TAG));

                int fileType = FileUtils.getCategory(mimeType);

                if (photosOnly && (fileType != FileUtils.CATEGORY_PHOTOS))
                {
                    returnValue = null;
                }
                else
                {
                    switch (fileType)
                    {
                        case FileUtils.CATEGORY_PHOTOS:
                                returnValue = new Photo(link, title, size, deleted, updated, version, path, mimeType);
                                break;
                        case FileUtils.CATEGORY_MUSIC:
                                returnValue = new Music(link, title, size, deleted, updated, version, path, mimeType);
                                break;
                        case FileUtils.CATEGORY_TEXT_FILE:
                        case FileUtils.CATEGORY_MSEXCEL:
                        case FileUtils.CATEGORY_MSPOWERPOINT:
                        case FileUtils.CATEGORY_MSWORD:
                        case FileUtils.CATEGORY_PDF:
                                returnValue = new Document(link, title, size, deleted, updated, version, path, mimeType, fileType);
                                break;
                        case FileUtils.CATEGORY_VIDEOS:
                                returnValue = new Video(link, title, size, deleted, updated, version, path, mimeType);
                                break;
                        default:
                                returnValue = new CloudFile(link, title, size, deleted, updated, version, path, mimeType);
                    }
                }
            }
        }
        return returnValue;
    }

    /*
     * Returns the number of files that match the parameters passed in
     *
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
     * {
     *     "count": 3,
     *     "etag": "2010-04-27T19:54:19Z"
     * }
     */
    public IntDownload getFileCount(String containerLink,
                                     boolean includeDirectories,
                                     boolean recurse,
                                     String titleQuery)
    {
        IntDownload returnValue = new IntDownload();
        
      //  Provisioning provisioning = Provisioning.getInstance(context);
      //  boolean showHidden = provisioning.getHiddenFilesMode();

        String uri = this.buildUri(containerLink, includeDirectories, false, recurse, titleQuery, true, 0, null);

        GetRequest request = getRequestForURI(uri);

        InputStream istream = null;
        try
        {

            if (this.mipConnection != null) 
            {
                istream = this.mipConnection.getData(request, new DataManager.Params());
            }
            else
            {
               throw new IOException("No http connection made.");
            }

            if (istream != null)
            {
                String jsonString = StreamUtil.JsonStreamToString(istream);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();

                returnValue.count = jsonObject.getInt(METADATA_COUNT_JSON_TAG);
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Exception e)
            {
                LogUtil.debug(this, "Exception closing stream");
            }
        }

        return returnValue;
    }

    // Returns the thumbnail for the specified file
    public ThumbnailConnection getThumbnail(String fileLink, int width, int height, long lastModified, long version)
    {
        StringBuilder uriBuilder = new StringBuilder(fileLink);

        // The uri may or may not already have an HTTP parameter in it
        if (fileLink.lastIndexOf('?') != -1)
        {
            uriBuilder.append("&");
        }
        else
        {
            uriBuilder.append("?");
        }

        uriBuilder.append(MIP_PARAMETER_REPR);
        uriBuilder.append(MIP_PARAMETER_THUMBNAIL);
        uriBuilder.append(Integer.toString(width));
        uriBuilder.append("x");
        uriBuilder.append(Integer.toString(height));
        /* NEW IAN CODE */
        
        if(width != height)
            uriBuilder.append(":l");    // Says to scale the full image down to the dimensions requested, not clip it
        else
            uriBuilder.append(":c");
        /* END NEW IAN CODE */
        Date dateLastMod = new Date(lastModified);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",  Locale.getDefault());
        String lastModifiedDate = dateFormatter.format(dateLastMod);
        uriBuilder.append("&" + "last_modified=" + lastModifiedDate);
        uriBuilder.append("&" + "version=" + version);

        String uri = uriBuilder.toString();

        LogUtil.debug("MipAPI", "[FETCHING THUMBNAIL] (" + new Date(lastModified) + ") " + uri);

        ArrayList<Header> headers = new ArrayList<Header>();

        headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));

        headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));

        Header[] headersArray = new Header[headers.size()];
        headersArray = headers.toArray(headersArray);

        GetRequest request = null;

        request = new GetRequest(uri, headersArray);

        ThumbnailConnection connection = new ThumbnailConnection(mipConnection, request);

        return connection;
    }

    // Returns an errorCode, zero if no errors
    public int deleteFile(String fileLink)
    {

        return sendDeleteRequestForUri(fileLink);
    }
    
    
    // Returns an errorCode, zero if no errors
    public int deleteToken(String token)
    {
        String uri = this.mipUrlBeginning + "/" + "token"  + "/" + token;  

        int statusCode = sendDeleteRequestForUri(uri);
        
        
        // clear instance 
        MipAPI.setInstance(null);
        MipAuthAPI.setInstance(null);
        uploadFileAPI.setInstance(null);
        
        return statusCode;
    }

    /**
     * @param returnCode
     * @param uri
     * @return
     */
    protected int sendDeleteRequestForUri(String uri) 
    {
        int returnCode = ErrorCodes.NO_ERROR;
        ArrayList<Header> headers = new ArrayList<Header>();

        headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));

        headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));

        Header[] headersArray = new Header[headers.size()];
        headersArray = headers.toArray(headersArray);

        DeleteRequest request = new DeleteRequest(uri, headersArray);

        try
        {
            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(1);
            
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
            returnCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        return returnCode;
    }

    // Returns an InputStream to the downloaded file data. It is up to the caller to do something useful with this
    // data, and to close the stream when they are done.
    // Returns null on error.
    public StreamDownload downloadFile(String fileLink, boolean isDeviceEncrypted, boolean isSyzygyFormat)
    {
        StreamDownload returnValue  = new StreamDownload();
        StringBuilder uriBuilder = new StringBuilder(fileLink);

        // The uri may or may not already have an HTTP parameter in it
        if (fileLink.lastIndexOf('?') != -1)
        {
            uriBuilder.append("&");
        }
        else
        {
            uriBuilder.append("?");
        }

        uriBuilder.append(MIP_PARAMETER_REPR);        // "repr="
        //uriBuilder.append(MIP_PARAMETER_REPR+"metadata"); 
        uriBuilder.append(MIP_PARAMETER_CONTENT);    // "content"
        

        String uri = uriBuilder.toString();

        // String uri = Provisioning.getInstance(context).getAccountIdUrl() + MipAPI.MIP_SYNTAX_CONTAINER;
         ArrayList<Header> headers = new ArrayList<Header>();
             
         headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));
     
         headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
                 
         Header[] headersArray = new Header[headers.size()];
         headersArray = headers.toArray(headersArray);
       
         GetRequest request = new GetRequest(uri, headersArray);


        try
        {
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

            String getUrl=null;

            Header[] responseHeaders = connectionItem.getAllHeaders();
            for (int j = 0; j < responseHeaders.length; j++)
            {
                if (responseHeaders[j].getName().equals("Location"))
                {
                    getUrl = responseHeaders[j].getValue();
                    break;
                }
            }

            if (getUrl != null)
            {
            	/*
                if(isDeviceEncrypted)
                {
                	if (isSyzygyFormat) {
                		headers.add(new BasicHeader("Accept-Encoding", "x-syzygy"));
                		headersArray = headers.toArray(headersArray);
                	} else {
                	    getUrl = getUrl + "&" + MIP_PARAMETER_CIPHER;
                	}
                }
                */
            	//headers.add(new BasicHeader("Accept-Encoding", "x-syzygy"));
            	headers.add(new BasicHeader("Accept-Encoding", "x-syzygy,identity,x-ciphertext"));
            	headersArray = headers.toArray(headersArray);
            	
                request = new GetRequest(getUrl, headersArray);

                // Use dataConnection on URLs returned from MIP
                returnValue.stream = this.dataConnection.getData(request, new DataManager.Params());
            }

            /* BEGIN DEBUG CODE * /
            BufferedReader in = new BufferedReader(new InputStreamReader(returnValue));
            StringBuffer sb = new StringBuffer("");
            String line = "";
            String NL = System.getProperty("line.separator");
            while ((line = in.readLine()) != null) {
                sb.append(line + NL);
            }
            in.close();
            String page = sb.toString();
            System.out.println(page);

            / * END DEBUG CODE */

        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
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
    

    /*
     *
       *    @device            device containing object to be shared
       *  @path            full path to object
       *  @expiration     0 (for eternal), or seconds since epoch (integer UTC) of the time when this link should be removed automatically
       *  @receivers      comma/semicolon-separated list of mailids. OPTIONAL parameter
       *  @version        standard Mozy version, or null for latest version
     */
    public StringDownload getSharedLink(String username, String rootDeviceId, String path, long expiration, String receivers, String version)
    {
        StringDownload returnValue=new StringDownload();

        try
        {
            JSONObject jsonObject = new JSONObject();

            jsonObject.put("container", rootDeviceId);
            jsonObject.put("path", path);
            jsonObject.put("expiration", expiration);
            // The 'receivers_tag' field requires a value or we get a server error.
            jsonObject.put("receivers_tag", (receivers != null) ? receivers : "DummyName@DummyAddress.com");
            jsonObject.put("version", (version != null) ? version : JSONObject.NULL);

            ArrayList<Header> headers = new ArrayList<Header>();
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));

            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
            
            headers.add(new BasicHeader("Content-Type", "application/json"));
            Header[] headersArray = new Header[headers.size()];
            headersArray = headers.toArray(headersArray);

            String getShareLinkUri = this.mipUrlBeginning + "/" + username + "/" + MipAPI.MIP_SHARE_URL + "/";
            PostRequest request = new PostRequest(getShareLinkUri, headersArray, new StringEntity(jsonObject.toString()));

            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(1);

            HttpConnectionItem connectionItem = null;
            
            if (this.mipConnection != null) 
            {
                connectionItem = this.mipConnection.sendDataSaveReturnData(request, params);
            }
            else
            {
               throw new IOException("No http connection made.");
            }

            byte[] buffer = new byte[2048];
            StringBuilder stringBuilder = new StringBuilder();

            // I expect this loop to only execute once in the vast majority of cases, but just in case we run into an insanely long link...
            while(connectionItem != null && (connectionItem.read(buffer, params)) != -1)
            {
                stringBuilder.append(new String(buffer));
            }

            JSONObject returnJsonObject = new JSONObject(stringBuilder.toString());

            StringBuilder uriBuilder = new StringBuilder(MipAPI.STR_HTTPS);
            Provisioning provisioning = Provisioning.getInstance(context);
            uriBuilder.append(provisioning.getDomainName());
            uriBuilder.append("/");
            uriBuilder.append(MipAPI.MIP_SHARE_URL);
            uriBuilder.append("/");
            uriBuilder.append(returnJsonObject.getString("guid"));

            returnValue.string = uriBuilder.toString();

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
    

    

    
    /**
     * Used For username for horizon files case and otherwise needed for custom permissions
     * @return
     */
    public ListDownload getUserInfo()
    {
        ListDownload returnValue = new ListDownload();
        
        boolean mobileEnabled = false;

        String uri = this.mipUrlBeginning + "/" +  MipAPI.MIP_SYNTAX_USER_INFO + "/";

        GetRequest request = getRequestForURI(uri);

        InputStream istream = null;
        try
        {
            if (this.mipConnection != null) 
            {
                istream = this.mipConnection.getData(request, new DataManager.Params());
            }
            else
            {
               throw new IOException("No http connection made.");
            }


            if (istream != null)
            {
                String jsonString = StreamUtil.JsonStreamToString(istream);
                JSONObject jsonObject = (JSONObject)new JSONTokener(jsonString).nextValue();
                
                
                JSONArray listOfPermissions = jsonObject.getJSONArray(METADATA_CUSTOM_PERMISSION_JSON_TAG);

                int numberOfEntries = listOfPermissions.length();

                for (int i = 0; i < numberOfEntries; ++i)
                {
                  jsonString = listOfPermissions.getString(i);
                    if (jsonString.equalsIgnoreCase(MOBILE_ACCESS_PERMISSION))
                    {
                        mobileEnabled = true;
                        break;
                    }
                }
                if (!mobileEnabled)
                    returnValue.errorCode = ErrorCodes.ERROR_AUTH_INVALID_PARTNER;
                
                returnValue.list = new ArrayList<Object>();
                
                // get user id
                returnValue.list.add(MipAPI.ACCESS_USERNAME_ID, jsonObject.getString(METADATA_ID_JSON_TAG));
                
                
                // get managed key url
                returnValue.list.add (MipAPI.ACCESS_MANAGEDKEY_URL, getManagedKeyUrl(jsonObject));
                
                // get user email
                
                returnValue.list.add(MipAPI.ACCESS_USER_EMAIL, jsonObject.getString(METADATA_EMAIL_ID_TAG));
                
                // Get the container info
                ArrayList<Object> devices = getDevicesFromUserInfo(jsonObject);
                
                returnValue.list.addAll(devices);

            }
            else
            {
                returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
            }
        }
        catch (HttpException e)
        {
            returnValue.errorCode = MipCommon.handleHttpException(e);
        }
        catch (IOException ie)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_IO;
        }
        catch (Throwable thr)
        {
            returnValue.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        }
        finally
        {
            try
            {
                istream.close();
            }
            catch (Exception e)
            {
                LogUtil.debug(this, "Exception closing stream");
            }
        }

        LogUtil.debug(this, "ErrorCode: " + returnValue.errorCode);

        return returnValue;
    }

   
    
    public String getManagedKeyUrl(JSONObject jsonObject) throws JSONException
    {
        String managed_key_url = "";
        
        JSONArray listOfDevices = jsonObject.getJSONArray(METADATA_CONTAINER_JSON_TAG);

        int numberOfEntries = listOfDevices.length();

        for (int i = 0; i < numberOfEntries; ++i)
        {
            jsonObject = listOfDevices.getJSONObject(i);

            if (jsonObject.has(METADATA_MANAGED_KEY_URL)) {
                   managed_key_url = jsonObject.getString(METADATA_MANAGED_KEY_URL); 
                   break;
            } else {
                continue;
            }
        }
        return managed_key_url; 
    }
    
    
    public ArrayList<Object> getDevicesFromUserInfo(JSONObject jsonObject) throws JSONException
    {
        ArrayList<Object> list = null;

        JSONArray listOfDevices = jsonObject.getJSONArray(METADATA_CONTAINER_JSON_TAG);

        int numberOfEntries = listOfDevices.length();
        list = new ArrayList<Object>(numberOfEntries);

        for (int i = 0; i < numberOfEntries; ++i)
        {
            createDeviceAndAddInList(list, listOfDevices, i);
        }

        return list;
    }
    
}




