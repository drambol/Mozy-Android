

package com.mozy.mobile.android.web;

import java.io.IOException;
import java.net.URLEncoder;

import java.util.ArrayList;

import org.apache.http.Header;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;

import se.tactel.datahandler.DataManager;

import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.PostRequest;
import se.tactel.datahandler.api.HttpConnectionItem;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.client.MipHttpClient;

import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.provisioning.ProvisioningListener;
import com.mozy.mobile.android.utils.Base64;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.ListDownload;


public class MipAuthAPI implements ProvisioningListener
{
    public static final int ACCESS_TOKEN_INDEX = 0;
    public static final int ACCESS_TOKEN_SECRET = 1;
    public static final int ACCESS_USERNAME_ID = 0;
    public static final int ACCESS_MANAGEDKEY_URL = 1;
    public static final int ACCESS_USER_EMAIL = 2;


    private String mipUrlBeginning;
    
    private static MipAuthAPI sInstance = null;


    public static void setInstance(MipAuthAPI sInstance) {
        MipAuthAPI.sInstance = sInstance;
    }

    protected Context context;

    protected DataManager mipConnection = null;
    protected DataManager dataConnection = null;

    protected String version = null;
  
    
    public static synchronized MipAuthAPI getInstance(Context context)
    {

        if(MipAuthAPI.sInstance == null)
            MipAuthAPI.sInstance = new MipAuthAPI(context.getApplicationContext());

        return MipAuthAPI.sInstance;
    }
    
   
    public class requestInterceptorOAuth implements HttpRequestInterceptor
    {
        private String mClientKey;
        private String mClientSecret;

        public requestInterceptorOAuth(Context contextMipAPI, String clientKey, String clientSecret)
        {
            mClientKey = clientKey;
            mClientSecret = clientSecret;
        }

        public void process(HttpRequest request, HttpContext context)
        {
            if (!request.containsHeader("Authorization"))
            {
               // Encode the login information
                String base64 = Base64.encodeString(mClientKey + ":" + mClientSecret);
                request.addHeader("Authorization", "Basic " + base64);
                for (Header h : request.getAllHeaders())
                {
                    LogUtil.debug("HttpConnectionItem", "Request Headers: " + h.getName() + " :: " + h.getValue());
                }
            }
        }
    }
    
    
    // The following was copied from the DechoAPI class constructor
    protected MipAuthAPI(Context context)
    {
        this.context = context;
       
        Provisioning provisioning = Provisioning.getInstance(context.getApplicationContext());
        
     
        this.mipUrlBeginning = MipCommon.buildMipUrlBeginningStr(this.context);
        
        MipCommon.mMIPClientSecret = provisioning.getOAuthSecret();

        this.mipConnection = getMIPConnectionOAuth(context, MipCommon.mMIPClientKey, MipCommon.mMIPClientSecret);


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
    

    /**
     * @param context
     */
    public DataManager getMIPConnectionOAuth(Context context, String clientKey, String clientSecret) 
    {
        // Just doing HTTP Basic Authorization for now...
        UsernamePasswordCredentials up = new UsernamePasswordCredentials(clientKey,clientSecret);

        AuthScope authScope = new AuthScope(null, -1);
        
        Provisioning provisioning = Provisioning.getInstance(context);
        MipHttpClient client = new MipHttpClient(provisioning.bAcceptAllCertificates(),
                                                    provisioning.getMipPort());
        client.getParams().setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
        client.addRequestInterceptor(new requestInterceptorOAuth(this.context, clientKey, clientSecret));
        /* DEBUG CODE * /
        client.addResponseInterceptor(new responseInterceptor());
        / * END DEBUG CODE */
        return new DataManager(null, new DataManager.HttpParams().setAuthScope(authScope).setHttpClient(client).setCredentials(up), context);
    }


    
    /**
     * Used for getting the token 
     * @return
     */
    public ListDownload authenticateUser(String username, String password)
    {
        ListDownload returnValue=new ListDownload();
        
        String mMIPClientToken = null;   
        String mMIPClientTokenSecret = null;
        
        try
        {      
            String uri = this.mipUrlBeginning + "/" + "token";  
            
            
            // Construct a x-www-form-urlencoded string 
            // for "grant_type=password&client_id=<client_id>&username=<username>&password=<password>"
            
            StringBuilder line = new StringBuilder();
            line.append("grant_type" + "=" + "password");
            line.append("&" + "client_id" + "=" + "Android_Client");
            line.append("&" + "username" + "=" + URLEncoder.encode(username, "UTF-8"));
            line.append("&" + "password" + "=" + URLEncoder.encode(password, "UTF-8"));    

            ArrayList<Header> headers = new ArrayList<Header>();
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));
            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
            headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
            Header[] headersArray = new Header[headers.size()];
            headersArray = headers.toArray(headersArray);

            PostRequest request = new PostRequest(uri, headersArray, new StringEntity(line.toString()));

            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(1);

            HttpConnectionItem connectionItem = this.mipConnection.sendData(request, params);

            byte[] buffer = new byte[2048];
            StringBuilder stringBuilder = new StringBuilder();

            // I expect this loop to only execute once in the vast majority of cases, but just in case we run into an insanely long link...
            while((connectionItem.read(buffer, params)) != -1)
            {
                stringBuilder.append(new String(buffer));
            }

            JSONObject returnJsonObject = new JSONObject(stringBuilder.toString());
            
            if(returnJsonObject.getString("token_type").equalsIgnoreCase("Mozy OAuth"))
            {
                returnValue.list = new ArrayList<Object>(2);
                
                mMIPClientToken =  returnJsonObject.getString("access_token");
                returnValue.list.add(ACCESS_TOKEN_INDEX, mMIPClientToken);
                
                mMIPClientTokenSecret = returnJsonObject.getString("token_secret");
                returnValue.list.add(ACCESS_TOKEN_SECRET, mMIPClientTokenSecret);

                returnValue.totalCount = returnValue.list.size();
                returnValue.errorCode = ErrorCodes.NO_ERROR;
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
            if(mMIPClientToken != null && mMIPClientTokenSecret != null)
                Provisioning.saveTokenAndSecret(Provisioning.getInstance(context), mMIPClientToken, mMIPClientTokenSecret);
            
            //Reset MipAPI instance to prepare for post handshake with token and token secret
            
            MipAuthAPI.setInstance(null);
            uploadFileAPI.setInstance(null);
        }
        
        return returnValue;
    }
    
    /**
     * Used for getting the token 
     * @return
     */
    public ListDownload getRequestForToken(String mozyAuthServer, String partner_subdomain, String authCode, String redirectUri)
    {
        ListDownload returnValue=new ListDownload();
        
        String mMIPClientToken = null;   
        String mMIPClientTokenSecret = null;
        
        try
        {            
            StringBuilder uriBuilder = new StringBuilder(MipAPI.STR_HTTPS);
            uriBuilder.append(mozyAuthServer);
            uriBuilder.append("/");
            uriBuilder.append(partner_subdomain);
            uriBuilder.append("/");
            uriBuilder.append("token");  
            
            
            //POST /<partner_subdomain>/token
            //Authorization: Basic <Base64.encode(client_id + ":" + client_secret)>
            //grant_type=authorization_code&code=<authorization_code>&redirect_uri=<the_same_redirect_uri_used_in_step_2>
            
            StringBuilder line = new StringBuilder();
            line.append("grant_type" + "=" + "authorization_code");
            line.append("&" + "code" + "=" + authCode);
            line.append("&" + "redirect_uri" + "=" + redirectUri);
            line.append("&state=foobar");
 

            ArrayList<Header> headers = new ArrayList<Header>();
            headers.add(new BasicHeader("User-Agent", android.os.Build.DEVICE + " Android-" + android.os.Build.VERSION.RELEASE));
            headers.add(new BasicHeader("User-Agent-Mozy", "Android" + "/"+ this.version));
            headers.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"));
                      
            Header[] headersArray = new Header[headers.size()];
            headersArray = headers.toArray(headersArray);

            PostRequest request = new PostRequest(uriBuilder.toString(), headersArray, new StringEntity(line.toString()));

            DataManager.Params params = new DataManager.Params();
            params.setNumRetry(1);

            HttpConnectionItem connectionItem = this.mipConnection.sendDataSaveReturnData(request, params);

            byte[] buffer = new byte[2048];
            StringBuilder stringBuilder = new StringBuilder();

            // I expect this loop to only execute once in the vast majority of cases, but just in case we run into an insanely long link...
            while((connectionItem.read(buffer, params)) != -1)
            {
                stringBuilder.append(new String(buffer));
            }

            JSONObject returnJsonObject = new JSONObject(stringBuilder.toString());
            
            returnValue.list = new ArrayList<Object>(2);
            mMIPClientToken =  returnJsonObject.getString("access_token");
            returnValue.list.add(ACCESS_TOKEN_INDEX, mMIPClientToken);
            
            mMIPClientTokenSecret = returnJsonObject.getString("token_secret");
            returnValue.list.add(ACCESS_TOKEN_SECRET, mMIPClientTokenSecret);
            
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
        finally
        {
            if(mMIPClientToken != null && mMIPClientTokenSecret != null)
                Provisioning.saveTokenAndSecret(Provisioning.getInstance(context), mMIPClientToken, mMIPClientTokenSecret);  
            
            MipAPI.setInstance(null);
            uploadFileAPI.setInstance(null);
        }

        return returnValue;
    }
   

    @Override
    public void onChange(int id) 
    {

    }
    
}




