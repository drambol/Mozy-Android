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

import java.io.InputStream;
import java.util.Locale;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicHeader;

import se.tactel.datahandler.api.ConnectionItem;

public class DataRequest {
    
    private static final String RANGE_HEADER = "range";
    
    public static final int METHOD_HEAD = 0;
    public static final int METHOD_POST = 1;
    public static final int METHOD_GET = 2;
    public static final int METHOD_PUT = 3;    
    public static final int METHOD_DELETE = 4;    
    public static final int METHOD_UNKNOWN = -1;
    
    /**
     * Internal data variables.
     */
    private String uri;
    private Header[] headers;
    private boolean cachable;
    private String id;
    private long lastModified;
    private HttpEntity entity;
    private int method = METHOD_UNKNOWN;
    private int initialOffset;
    private int initialLength;
    private int currentOffset;
    
    private ConnectionItem connection = null;
    private boolean aborted = false;
    
    DataRequest(String uri, Header[] headers) {
        this.uri = uri;
        this.headers = headers;
        this.cachable = false;
        this.initialOffset = 0;
        this.entity = null;
        this.initialLength = -1;
        this.currentOffset = 0;
        
        calculateRange();
    }
    
    DataRequest(String uri, Header[] headers, InputStream dataInputStream, int dataLength) {
        this.uri = uri;
        this.headers = headers;
        this.cachable = false;
        this.entity = null;
        this.initialOffset = 0;
        this.initialLength = -1;
        this.currentOffset = 0;
        
        calculateRange();
    }
    
    DataRequest(String uri, Header[] headers, HttpEntity entity) {
        this.uri = uri;
        this.headers = headers;
        this.cachable = false;
        this.entity = entity;
        this.initialOffset = 0;
        this.initialLength = -1;
        this.currentOffset = 0;
        
        calculateRange();
    }
    
    DataRequest(String uri, Header[] headers, String id, long lastModified) {
        this.uri = uri;
        this.headers = headers;
        this.cachable = true;
        this.id = id;
        this.entity = null;
        this.lastModified = lastModified;
        this.initialOffset = 0;
        this.initialLength = -1;
        this.currentOffset = 0;
        
        calculateRange();
    }
    
    public void abort() {
        if (!aborted) {
            aborted = true;
            
            synchronized (this) {
                if (connection != null) {
                    connection.abort();
                }
            }
        }
    }
    
    void setMethod(int method) {
        this.method = method;
    }
    
    public Header[] getConnectionHeaders()
    {
        return this.connection.getAllHeaders();
    }
    
    public synchronized void setOffset(int offset) {
        Header header = new BasicHeader(RANGE_HEADER, "bytes=" + offset + "-" + (initialLength > 0 ? initialLength : ""));
        boolean found = false;
        if (headers != null) {
            for (int i = 0; i < headers.length && !found; ++i) {
                if (headers[i].getName().equalsIgnoreCase(RANGE_HEADER)) {
                    found = true;
                    headers[i] = header;
                }
            }
        }
        if (!found) {
            int size = 1;
            if (headers != null) {
                size += headers.length;
            }
            Header[] new_headers = new Header[size];
            if (null != headers)
            {
                for (int i = 0; i < headers.length; ++i) {
                    new_headers[i] = headers[i];
                }
            }
            new_headers[size-1] = header;
            headers = new_headers;
        }
        currentOffset = offset;
    }
    
    public synchronized int getOffset() {
        return currentOffset;
    }
    
    public int getInitialOffset() {
        return initialOffset;
    }
    
    public boolean isCachable() {
        return cachable;
    }
    
    synchronized void setConnectionItem(ConnectionItem connection) {
        this.connection = connection;
    }
    
    public String getURI() {
        return uri;
    }
    
    public HttpEntity getEntity() {
        return entity;
    }
    
    public Header[] getHeaders() {
        return headers;
    }
    
    public boolean isAborted() {
        return aborted;
    }

    public int getMethod() {
        return method;
    }
    
    public String getId() {
        return id;
    }
    
    public long getLastModified() {
        return lastModified;
    }
    
    long calculateRealOffset(int offset) {
        return initialOffset + offset;
    }
    
    void calculateRange() {
        if (headers != null) {
            boolean found = false;
            for (int i = 0; i < headers.length && !found; ++i) {
                if (headers[i].getName().equalsIgnoreCase(RANGE_HEADER)) {
                    String value = headers[i].getValue().toLowerCase(Locale.getDefault());
                    found = true;
                    if (value.startsWith("bytes")) {
                        int index = value.indexOf('=');
                        int index_div = value.indexOf('-');
                        if (index >= 0) {
                            if (index_div >= 0) {
                                try {
                                    initialOffset = Integer.parseInt(value.substring(index, index_div));
                                    currentOffset = initialOffset;
                                    initialLength = Integer.parseInt(value.substring(index_div+1));
                                } catch (Exception e) {}
                            } else {
                                initialOffset = Integer.parseInt(value.substring(index));
                                currentOffset = initialOffset;
                            }
                        }
                    }
                }
            }
        }
    }
}
