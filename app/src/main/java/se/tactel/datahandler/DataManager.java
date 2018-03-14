/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/
package se.tactel.datahandler;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;

import android.content.Context;

import se.tactel.datahandler.api.API;
import se.tactel.datahandler.streams.DataInputStream;
import se.tactel.datahandler.api.HttpConnectionItem;

public class DataManager {
    
    //public static final int MAXIMUM_CACHE_LIMIT_SIZE = 100 * 1000 * 1024;
    /**
     * DataManager modes.
     */
    //private static final int MODE_UNINITIALIZED = 0;
    //private static final int MODE_INITIALIZED = 1;
    
    /**
     * Static integers
     */
    //private static int s_mode = MODE_UNINITIALIZED;
    
    private DataConnectionManager connectionManager = null;
    
    /**
     * Static objects
     */    
    //private static Object s_initLock = new Object();
    //private static DataConnectionManager s_connectionManager = null;
    
    public static class HttpParams {
        private DefaultHttpClient client = null;
        private HttpRequestInterceptor[] requestInterceptors = null;
        private AuthScope authScope = null;
        private Credentials credentials = null;
        
        public HttpParams setHttpClient(final DefaultHttpClient client) {
            this.client = client;
            return this;
        }
        
        public HttpParams setHttpRequestInterceptors(final HttpRequestInterceptor[] requestInterceptors) {
            this.requestInterceptors = requestInterceptors;
            return this;
        }
        
        public HttpParams setAuthScope(final AuthScope authScope) {
            this.authScope = authScope;
            return this;
        }
        
        public HttpParams setCredentials(final Credentials credentials) {
            this.credentials = credentials;
            return this;
        }
        
        public DefaultHttpClient getHttpClient() {
            return client;
        }
        
        public HttpRequestInterceptor[] getHttpRequestInterceptors() {
            return requestInterceptors;
        }
        
        public AuthScope getAuthScope() {
            return authScope;
        }
        
        public Credentials getCredentials() {
            return credentials;
        }
    }
    
    public static class Params {
        private int numRetry;
        private int retryInterval;
        private HttpContext httpContext;
        
        public Params() {
            this.numRetry = 0;
            this.retryInterval = 1000;
            this.httpContext = null;
        }
        
        public int getNumRetry() {
            return numRetry;
        }
        public Params setNumRetry(int numRetry) {
            this.numRetry = numRetry;
            return this;
        }
        public int getRetryInterval() {
            return retryInterval;
        }
        public Params setRetryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }
        public HttpContext getHttpContext() {
            return httpContext;
        }
        public Params setHttpContext(HttpContext httpContext) {
            this.httpContext = httpContext;
            return this;
        }
    }
    
    public DataManager(final API[] apis, final HttpParams httpParams, Context context)
    {
        connectionManager = new DataConnectionManager(apis, httpParams, context);        
    }
    
    public InputStream getData(final DataRequest request, final Params params) throws IOException 
    {
        return new DataInputStream(request, params, this.connectionManager);
    }
    
    public HttpConnectionItem sendData(final DataRequest request, final Params params) throws IOException 
    {
        return this.connectionManager.executeSimpleHttp(request, params, false);
    }
    
    // The difference between this method, and the above sendData() method, is that this method will retain any returned
    // data from the httprequest which is then expected to be consumed by calling the read() method of the returned
    // HttpConnectionItem.
    public HttpConnectionItem sendDataSaveReturnData(final DataRequest request, final Params params) throws IOException 
    {
        return this.connectionManager.executeSimpleHttp(request, params, true);
    }
    
    
    /*
    public static void initialize(final API[] apis, final HttpParams httpParams, Context context) {
        synchronized (s_initLock) {
            if (s_mode == MODE_UNINITIALIZED) {
                s_connectionManager = new DataConnectionManager(apis, httpParams, context);
                s_mode = MODE_INITIALIZED;
            }
        }
    }
    
    public static InputStream getData(final DataRequest request, final Params params) throws IOException {
        int mode;
        InputStream result = null;
        synchronized (s_initLock) {
            mode = s_mode;
        }
        if (mode == MODE_UNINITIALIZED) {
            throw new IOException("DataManager is not initialized.");
        } else {
            result = new DataInputStream(request, params, s_connectionManager);
        }
        return result;
    }
    
    public static void sendData(final DataRequest request, final Params params) throws IOException {
        int mode;
        synchronized (s_initLock) {
            mode = s_mode;
        }
        if (mode == MODE_UNINITIALIZED) {
            throw new IOException("DataManager is not initialized.");
        } else {
            s_connectionManager.executeSimpleHttp(request, params);
        }
    }
    */
}
