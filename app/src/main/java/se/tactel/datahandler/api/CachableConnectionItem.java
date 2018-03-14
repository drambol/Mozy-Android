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

import org.apache.http.Header;
import org.apache.http.StatusLine;

import se.tactel.datahandler.DataRequest;
import se.tactel.datahandler.DataManager.Params;
import se.tactel.datahandler.api.CacheAPI.CacheConnection;

public class CachableConnectionItem implements ConnectionItem {
    private final DataRequest request;
    private final CacheAPI cacheApi;
    private int offset;
    private CacheConnection connection;
    private long contentLength;
    private Header[] headers;
    private boolean aborted;
    
    public CachableConnectionItem(final DataRequest request, final CacheAPI cacheApi) {
        this.request = request;
        this.cacheApi = cacheApi;
        this.offset = request.getOffset();
        this.connection = null;
        this.contentLength = -1;
        this.headers = null;
        this.aborted = false;
    }

    @Override
    public void abort() {
        aborted = true;
        synchronized (this) {
            if (connection != null) {
                connection.release();
                connection = null;
            }
        }
    }

    @Override
    public int read(final Params params) throws IOException {
        CacheConnection conn = null;
        synchronized (this) {
            if (connection == null) {
                connection = cacheApi.connect(request);
            }
            conn = connection;
        }
        
        int result = -1;
        try {
            result = conn.read(offset, request, params);
            if (contentLength < 0) {
                contentLength = conn.getContentLength();
            }
            if (headers == null) {
                headers = conn.getAllHeaders();
            }
            if (result >= 0) {
                offset++;
            }
        } catch (CacheException ce) {
            request.setOffset(offset + request.getInitialOffset());
            if (!aborted) {
                synchronized (this) {
                    connection = conn.refresh(request);
                }
            }
        }
        return result;
    }

    @Override
    public int read(final byte[] b, final Params params) throws IOException {
        CacheConnection conn = null;
        synchronized (this) {
            if (connection == null) {
                connection = cacheApi.connect(request);
            }
            conn = connection;
        }
        
        int result = 0;
        try {
            result = conn.read(b, offset, request, params);
            if (contentLength < 0) {
                contentLength = conn.getContentLength();
            }
            if (headers == null) {
                headers = conn.getAllHeaders();
            }
            if (result >= 0) {
                offset+=result;
            }
        } catch (CacheException ce) {
            request.setOffset(offset + request.getInitialOffset());
            if (!aborted) {
                synchronized (this) {
                    connection = conn.refresh(request);
                }
            }
        }
        return result;
    }
    
    @Override
    public int read(final byte[] b, int off, int len, final Params params) throws IOException {
        CacheConnection conn = null;
        synchronized (this) {
            if (connection == null) {
                connection = cacheApi.connect(request);
            }
            conn = connection;
        }
        
        int result = 0;
        try {
            result = conn.read(b, off, len, offset, request, params);
            if (contentLength < 0) {
                contentLength = conn.getContentLength();
            }
            if (headers == null) {
                headers = conn.getAllHeaders();
            }
            if (result >= 0) {
                offset+=result;
            }
        } catch (CacheException ce) {
            request.setOffset(offset + request.getInitialOffset());
            if (!aborted) {
                synchronized (this) {
                    connection = conn.refresh(request);
                }
            }
        }
        return result;
    }

    @Override
    public long getContentLength() {
        long contentLength = -1;
        synchronized (this) {
            if (this.contentLength >= 0) {
                contentLength = this.contentLength;
            } else if (connection != null) {
                contentLength = connection.getContentLength();
            }
        }
        return contentLength;
    }

    @Override
    public Header[] getAllHeaders() {
        Header[] result = null;
        synchronized (this) {
            if (headers != null) {
                result = headers;
            } else if (connection != null) {
                result = connection.getAllHeaders();
            }
        }
        return result;
    }

    @Override
    public StatusLine getHttpStatus() {
        return null;
    }
}
