/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
*                                    _           _
*       _                 _        | |         | |
*     _| |_ _____  ____ _| |_ _____| |    _____| |__
*    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
*      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
*       \__)_____|\____)  \__)_____)\_)  \_____|____/
*
*/
package se.tactel.datahandler.api;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;

import com.mozy.mobile.android.utils.LogUtil;


import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.DataManager.Params;

public class HttpConnectionItem implements ConnectionItem {
    
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 1;
    
    private final HttpUriRequest httpRequest;
    private final HttpAPI httpApi;
    private boolean aborted;
    private InputStream inputStream;
    private HttpEntity entity;
    private long contentLength;
    private long startOffset;
    private int state;
    private Header[] headers;
    private StatusLine httpStatus;
    
    public HttpConnectionItem(final HttpUriRequest httpRequest, long startOffset, final HttpAPI httpApi) {
        this.httpRequest = httpRequest;
        this.httpApi = httpApi;
        this.aborted = false;
        this.inputStream = null;
        this.entity = null;
        this.contentLength = -1;
        this.startOffset = startOffset;
        this.headers = null;
        this.httpStatus = null;
        this.state = STATE_DISCONNECTED;
    }
    
    public static void logHttpRequest(HttpUriRequest req)
    {
        LogUtil.debug("HttpConnectionItem", "URI: " + req.getURI().getPath());
        for (Header h : req.getAllHeaders())
        {
            LogUtil.debug("HttpConnectionItem", "Request Headers: " + h.getName() + " :: " + h.getValue());
        }
    }
    
    public static void logHttpResponse(HttpResponse res)
    {
        LogUtil.debug("HttpConnectionItem", res.getStatusLine().toString());
        for (Header h : res.getAllHeaders())
        {
            LogUtil.debug("HttpConnectionItem", "Response Headers: " + h.getName() + " :: " + h.getValue());
        }
    }
    
    public void execute(final Params params, boolean saveReturnData) throws HttpException, IOException {
        int numRetry = params.getNumRetry();
        boolean success = false;
        HttpResponse httpResponse = null;
        
        if (inputStream == null) {
            while (0 < numRetry && !success) {
                numRetry--;
                try {
                    logHttpRequest(httpRequest);
                    httpResponse = httpApi.execute(httpRequest, params.getHttpContext());
                    httpStatus = httpResponse.getStatusLine();
                    headers = httpResponse.getAllHeaders();
                    logHttpResponse(httpResponse);
                    success = true;
                } catch (HttpException e) {
                    if (numRetry == 0) {
                        throw (e);
                    }
                } catch (IOException e) {
                    if (numRetry == 0) {
                        throw (e);
                    }
                } finally {
                    if (httpResponse != null) {
                        if (saveReturnData) {
                            this.entity = httpResponse.getEntity();
                            if (null != entity) {
                                this.contentLength = entity.getContentLength();
                                this.inputStream = entity.getContent();
                            }
                            this.state = STATE_CONNECTED;                            
                        } else {
                            httpResponse.getEntity().consumeContent();
                        }
                    }
                    if (!success && numRetry == 0) {
                        abort();
                    }
                }
            }
        }
    }
    
    public int read(final Params params) throws IOException {
        if (aborted) {
            throw new IOException("Connection is aborted.");
        }
        int result = -1;
        int numRetry = params.getNumRetry();
        InputStream is = null;
        boolean success = false;
        HttpResponse httpResponse = null;
        switch (state) {
        case STATE_DISCONNECTED:
            while (0 <= numRetry && !success) {
                numRetry--;
                try {
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                    }
                    logHttpRequest(httpRequest);
                    httpResponse = httpApi.execute(httpRequest, params.getHttpContext());
                    httpStatus = httpResponse.getStatusLine();
                    logHttpResponse(httpResponse);
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                        entity = httpResponse.getEntity();
                        headers = httpResponse.getAllHeaders();
                        if (entity != null) {
                            contentLength = entity.getContentLength();
                            inputStream = entity.getContent();
                        } else {
                            contentLength = 0;
                            inputStream = null;
                        }
                    }
                    state = STATE_CONNECTED;
                    success = true;
                } catch (HttpException e) {
                    LogUtil.info(this, "HTTP error code: " + e.getHttpErrorCode());
                    if (numRetry < 0) {
                        throw (e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (numRetry < 0) {
                        throw (e);
                    }
                } finally {
                    if (!success) {
                        abort();
                    }
                }
            }
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read();
            } else {
                result = -1;
            }
            break;
        case STATE_CONNECTED:
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read();
            } else {
                result = -1;
            }
            break;
        }
        
        return result;
    }
    
    public int read(final byte[] b, final Params params) throws IOException {
        if (aborted) {
            throw new IOException("Connection is aborted.");
        }
        int result = -1;
        int numRetry = params.getNumRetry();
        boolean success = false;
        InputStream is = null;
        HttpResponse httpResponse = null;
        switch (state) {
        case STATE_DISCONNECTED:
            while (numRetry >= 0 && !success) {
                numRetry--;
                try {
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                    }
                    logHttpRequest(httpRequest);
                    httpResponse = httpApi.execute(httpRequest, params.getHttpContext());
                    logHttpResponse(httpResponse);
                    httpStatus = httpResponse.getStatusLine();
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                        entity = httpResponse.getEntity();
                        headers = httpResponse.getAllHeaders();
                        if (entity != null) {
                            contentLength = entity.getContentLength();
                            inputStream = entity.getContent();
                        } else {
                            contentLength = 0;
                            inputStream = null;
                        }
                    }
                    state = STATE_CONNECTED;
                    success = true;
                } catch (HttpException e) {
                    LogUtil.info(this, "HTTP error code: " + e.getHttpErrorCode());
                    if (numRetry < 0) {

                        throw (e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (numRetry < 0) {
                        throw (e);
                    }
                } finally {
                    if (!success) {
                        abort();
                    }
                }
            }
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read(b);
            } else {
                result = -1;
            }
            break;
        case STATE_CONNECTED:
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read(b);
            } else {
                result = -1;
            }
            break;
        }
        
        return result;
    }
    
    public int read(final byte[] b, int off, int len, final Params params) throws IOException {
        if (aborted) {
            throw new IOException("Connection is aborted.");
        }
        int result = -1;
        int numRetry = params.getNumRetry();
        boolean success = false;
        HttpResponse httpResponse = null;
        InputStream is = null;
        switch (state) {
        case STATE_DISCONNECTED:
            while (0 <= numRetry && !success) {
                numRetry--;
                try {
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                    }
                    logHttpRequest(httpRequest);
                    httpResponse = httpApi.execute(httpRequest, params.getHttpContext());
                    logHttpResponse(httpResponse);
                    synchronized (this) {
                        if (entity != null) {
                            entity.consumeContent();
                            entity = null;
                        }
                        entity = httpResponse.getEntity();
                        headers = httpResponse.getAllHeaders();
                        contentLength = entity.getContentLength();
                        inputStream = entity.getContent();
                    }
                    state = STATE_CONNECTED;
                    success = true;
                } catch (HttpException e) {
                    LogUtil.info(this, "HTTP error code: " + e.getHttpErrorCode());
                    if (numRetry < 0) {
                        
                        throw (e);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (numRetry < 0) {
                        throw (e);
                    }
                } finally {
                    if (!success) {
                        abort();
                    }
                }
            }
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read(b, off, len);
            } else {
                result = -1;
            }
            break;
        case STATE_CONNECTED:
            synchronized (this) {
                is = inputStream;
            }
            if (is != null) {
                result = is.read(b, off, len);
            } else {
                result = -1;
            }
            break;
        }
        
        return result;
    }
    
    public void abort() {
        aborted = true;
        if (httpRequest != null) {
            httpRequest.abort();
        }
        synchronized (this) {
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                entity = null;
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
                inputStream = null;
            }
        }
    }

    @Override
    public long getContentLength() {
        return contentLength + startOffset;
    }

    @Override
    public Header[] getAllHeaders() {
        return headers;
    }

    @Override
    public StatusLine getHttpStatus() {
        return httpStatus;
    }
}