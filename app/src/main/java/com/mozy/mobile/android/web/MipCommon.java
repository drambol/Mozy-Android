

package com.mozy.mobile.android.web;


import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;

import se.tactel.datahandler.DataManager;
import se.tactel.datahandler.HttpException;
import android.content.Context;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.client.DataHttpClient;

import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.LogUtil;


public class MipCommon 
{
    public static final String STR_HTTPS = "https://";

    protected static final String METADATA_CONTENT_TYPE_DIRECTORY = "type=directory";

    
    private static final String METADATA_OAUTH_ERROR_TAG = "Oauth_error";
    private static final String METADATA_OAUTH_ERROR_DESC_TAG = "Oauth_description";
    private static final String METADATA_OAUTH_ERROR_INVALID_REQUEST = "invalid_request";
    private static final String METADATA_OAUTH_ERROR_DESC = "This token does not exist.";
    private static final String METADATA_OAUTH_ERROR_INVALID_USER = "invalid_user";
    private static final String METADATA_OAUTH_ERROR_MULTIPLE_USERS = "Multiple users found.";
    
    
    
    private static final String METADATA_INVALID_PARTNER_TAG = "invalid_partner";
    private static final String METADATA_ACCOUNTS_CONFLICT = "accounts";

    private static final String METADATA_REQUIRED_CLIENT_TAG = "required_mobile_client";
    
    public static String mMIPClientKey = "Android_Client";
    // private static String mMIPClientKey = "IOS_CLIENT";
     
     public static String getmMIPClientKey() {
         return mMIPClientKey;
     }

     
     // QA5 external facing ip and port
     //mipHost=@"10.135.16.137";  // MIP02
     //mipHost=@"mip01.qa5.mozyops.com";  // MIP01 dns name

     //QA 7
     //mipHost=@"10.135.33.105";  // mip 3
     //mipHost=@"10.135.33.138";
     
     // QA13
     //mipHost = @"10.29.73.204";
     //mipHost = @"10.29.73.236"; *
     //mozyAuthHost= @"auth01.qa13.mozyops.com";
    
     // STD1 
     // mipHost "mip.test.mozy.com";
     // mozyAuthHost "auth.test.mozy.com";
    

    //private static String mMIPClientSecret = "q6fJRvLhuIpKKnI2";  // QA5 //QA13 //QA7
     
    //private static String mMIPClientSecret = "NkIRTmbrwTC2hNr8";  // STD
     
     //private static String mMIPClientSecret = "pB6sl87k2EXhF2r1";   // iOS
     
     public static String mMIPClientSecret = null;
     

     
     /**
      * @param context
      */
     public static DataManager getDataConnection(Context context) {
         AuthScope authScope;
         DataHttpClient dataClient = new DataHttpClient(Provisioning.getInstance(context).bAcceptAllCertificates());
         /* DEBUG CODE * /
         dataClient.addRequestInterceptor(new requestInterceptor());
         dataClient.addResponseInterceptor(new responseInterceptor());
         / * END DEBUG CODE */
         authScope = new AuthScope(null, -1);
         // Urls accessed via this.dataConnection are self authorizing, so no credentials needed
         return new DataManager(null, new DataManager.HttpParams().setAuthScope(authScope).setHttpClient(dataClient), context);
     }

    /**
     * @param context
     * @return
     */
    public static String buildMipUrlBeginningStr(Context context) 
    {
        StringBuilder uriBuilder = new StringBuilder(MipCommon.STR_HTTPS);
        Provisioning provisioning = Provisioning.getInstance(context);
        uriBuilder.append(provisioning.getDomainName());
       
        
        if(provisioning.getMipPort() != null || provisioning.getMipPort().length() != 0)
        {
            uriBuilder.append(":");    
            uriBuilder.append(provisioning.getMipPort());
        }
        return uriBuilder.toString();
        
    }


    /**
     * @param e
     * @return
     */
    protected static int handleHttpException(HttpException e) {
        int returnCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        
        if( e != null)
        {
            returnCode = MipCommon.translateHttpError(e.getHttpErrorCode());
            LogUtil.debug("handleHttpException", "ErrorCode (HTTP): " + e.getHttpErrorCode());
        
            if(e.getHttpErrorCode() >= 400 && e.getHttpErrorCode() < 500)
            {
                
                Header[] responseHeaders = e.getAllHeaders();
                
                if (returnCode == ErrorCodes.ERROR_HTTP_FORBIDDEN)
                {
                    // Which kind of forbidden is this? Override with special if appropriate.
                    for (int i = responseHeaders.length - 1; i >= 0; i--)
                    {
                        if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_INVALID_PARTNER_TAG)) 
                        {
                            // ASSUMES: existence of an "invalid_partner" header means an invalid partner
                            // (i.e. responseHeaders[i].getValue() is assumed to be TRUE)
                            returnCode = ErrorCodes.ERROR_AUTH_INVALID_PARTNER;
                            break;
                        }
                        else if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_ACCOUNTS_CONFLICT))
                        {
                            // Account conflict
                            returnCode = ErrorCodes.ERROR_AUTH_ACCOUNT_CONFLICT;
                            break;
                        }
                        else if(responseHeaders[i].getName().equalsIgnoreCase(METADATA_REQUIRED_CLIENT_TAG))
                        {
                            returnCode = ErrorCodes.ERROR_INVALID_CLIENT_VER;
                            break;
                        }
                    }
                }
                else if(returnCode ==  ErrorCodes.ERROR_HTTP_SERVER)
                {
                    boolean invalidReq = false;
                    boolean invalidToken = false;
                    
                    for (int i = responseHeaders.length - 1; i >= 0; i--)
                    {
                        if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_OAUTH_ERROR_TAG)) 
                        {
                            if(responseHeaders[i].getValue().equalsIgnoreCase(METADATA_OAUTH_ERROR_INVALID_USER))
                            {
                                invalidReq = true;
                            }
                        }

                        if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_OAUTH_ERROR_DESC_TAG))
                        {
                            if (responseHeaders[i].getValue().equalsIgnoreCase(METADATA_OAUTH_ERROR_MULTIPLE_USERS))
                            {
                                invalidToken = true;
                            }
                        }
                        
                        if(invalidToken && invalidReq)
                        {
                            returnCode = ErrorCodes.ERROR_AUTH_ACCOUNT_CONFLICT;
                            break;
                        }
                    }
                }
                else
                {
                    boolean invalidReq = false;
                    boolean invalidToken = false;
                    
                    for (int i = responseHeaders.length - 1; i >= 0; i--)
                    {
                        if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_OAUTH_ERROR_TAG)) 
                        {
                            returnCode = ErrorCodes.ERROR_AUTHORIZATION_ERROR;
                            
                            if (responseHeaders[i].getValue().equalsIgnoreCase(METADATA_OAUTH_ERROR_INVALID_REQUEST))
                            {
                                invalidReq = true;
                            }
                            
                            
                            if(responseHeaders[i].getValue().equalsIgnoreCase(METADATA_OAUTH_ERROR_INVALID_USER))
                            {
                                returnCode = ErrorCodes.ERROR_INVALID_USER;
                            }
                        }
                        if (responseHeaders[i].getName().equalsIgnoreCase(METADATA_OAUTH_ERROR_DESC_TAG))
                        {
                            if (responseHeaders[i].getValue().equalsIgnoreCase(METADATA_OAUTH_ERROR_DESC))
                            {
                                invalidToken = true;
                            }
                        }
                        if(invalidToken && invalidReq)
                        {
                            returnCode = ErrorCodes.ERROR_INVALID_TOKEN;
                            break;
                        }
                    }
                }
            }
        }

        return returnCode;
    }

  

    public static int translateHttpError(int httpError) {
        int errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
        switch (httpError) {
        case HttpStatus.SC_OK:
        case HttpStatus.SC_CREATED:
        case HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION:
        case HttpStatus.SC_NO_CONTENT:
        case HttpStatus.SC_RESET_CONTENT:
        case HttpStatus.SC_PARTIAL_CONTENT:
            errorCode = ErrorCodes.NO_ERROR;
            break;
        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
            errorCode = ErrorCodes.ERROR_HTTP_SERVER; // 500
            break;
        case HttpStatus.SC_BAD_GATEWAY:
            errorCode = ErrorCodes.ERROR_HTTP_BAD_GATEWAY; // 502
            break;
        case HttpStatus.SC_SERVICE_UNAVAILABLE:
            errorCode = ErrorCodes.ERROR_HTTP_SERVICE_UNAVAILABLE; // 503
            break;
        case HttpStatus.SC_GATEWAY_TIMEOUT:
            errorCode = ErrorCodes.ERROR_HTTP_GATEWAY_TIMEOUT; // 504
            break;
        case HttpStatus.SC_FORBIDDEN:  // 403
            errorCode = ErrorCodes.ERROR_HTTP_FORBIDDEN;
            break;
        case HttpStatus.SC_BAD_REQUEST:  // 400
            errorCode = ErrorCodes.ERROR_HTTP_SERVER;
            break;
        case HttpStatus.SC_UNAUTHORIZED: // 401
            errorCode = ErrorCodes.ERROR_HTTP_UNAUTHORIZED;
            break;
        case HttpStatus.SC_REQUEST_TIMEOUT:  // 408
            errorCode = ErrorCodes.ERROR_HTTP_REQUEST_TIMEOUT;
            break;
        case HttpStatus.SC_NOT_FOUND:  // 404
            errorCode = ErrorCodes.ERROR_HTTP_NOT_FOUND;
            break;
        case HttpStatus.SC_CONFLICT:  //409
            errorCode = ErrorCodes.ERROR_HTTP_FORBIDDEN;
            break;
        }
        return errorCode;
    }
    
}




