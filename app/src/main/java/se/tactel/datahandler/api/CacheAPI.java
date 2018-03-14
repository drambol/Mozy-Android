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
import java.util.Locale;
import java.util.Vector;

import org.apache.http.Header;

import android.content.Context;

import se.tactel.datahandler.DataRequest;
import se.tactel.datahandler.DataManager.Params;
import se.tactel.datahandler.api.buffer.BufferManager;
import se.tactel.datahandler.db.FileCache;

public class CacheAPI implements API {
    
    private final HttpAPI httpApi;
    private final BufferManager bufferManager;
    private Vector<CacheConnection> connections;
    private Context context;
    
    public class CacheConnection {
        
        private static final int STATE_CONNECTED = 0;
        private static final int STATE_ABORTED = 1;
        private static final int STATE_OFFLINE = 2;
        private static final int STATE_TERMINATED = 3;
        
        private int ref_counter;
        private int state;
        private final HttpAPI httpApi;
        private final BufferManager bufferManager;
        private final Vector<CacheHandler> handlers;
        private final String id;
        private final long lastModified;
        private ConnectionItem connectionItem;
        private int connectionOffset;
        private long contentLength;
        private Header[] headers;
        
        public CacheConnection(final String id, final long lastModified, final HttpAPI httpApi, final BufferManager bufferManager, boolean connected) {
            this.ref_counter = 0;
            this.httpApi = httpApi;
            this.bufferManager = bufferManager;
            this.handlers = new Vector<CacheHandler>();
            if (connected) {
                this.state = STATE_CONNECTED;
            } else {
                this.state = STATE_OFFLINE;
            }
            this.id = id.toLowerCase(Locale.getDefault());
            this.lastModified = lastModified;
            this.connectionItem = null;
            this.connectionOffset = 0;
            this.contentLength = -1;
            this.headers = null;
            addRef();
        }
        
        void addRef() {
            ref_counter++;
        }
        
        public long getContentLength() {
            return contentLength;
        }
        
        public Header[] getAllHeaders() {
            Header[] headers = null;
            if (state == STATE_OFFLINE) {
                synchronized (this) {
                    if (connectionItem != null) {
                        headers = connectionItem.getAllHeaders();
                    }
                }
            } else {
                headers = this.headers;
            }
            return headers;
        }
        
        public boolean isMatch(String id, long lastModified) {
            return this.id.equals(id.toLowerCase(Locale.getDefault())) && this.lastModified == lastModified;
        }
        
        public boolean isMatch(CacheConnection connection) {
            return this.id.equals(connection.id) && this.lastModified == connection.lastModified;
        }
        
        int release() {
            ref_counter--;
            if (ref_counter <= 0 && state != STATE_TERMINATED) {
                state = STATE_TERMINATED;
                
                removeConnection(this);
                synchronized (handlers) {
                    while (handlers.size() > 0) {
                        handlers.firstElement().close();
                        handlers.remove(0);
                    }
                }
                
                if (connectionItem != null) {
                    connectionItem.abort();
                    connectionItem = null;
                }
            }
            
            return ref_counter;
        }
        
        public int read(int offset, final DataRequest request, final Params params) throws IOException, CacheException {
            int result = -1;
            if (state == STATE_CONNECTED) {
                boolean offsetFound = false;
                boolean exceptionOccured = false;
                boolean requireNewHandler = false;
                int index = -1;
                for (int i = 0; i < handlers.size() && !offsetFound; ++i) {
                    offsetFound = handlers.get(i).hasOffset(offset);
                    index = i;
                }
                if (offsetFound) {
                    try {
                        result = handlers.get(index).read(offset);
                    } catch (CacheException ce) {
                        exceptionOccured = true;
                    } catch (ArrayIndexOutOfBoundsException ae) {
                        offsetFound = false;
                    }
                }
                if (!offsetFound || exceptionOccured) {
                    try {
                        result = FileCache.getInstance(context.getApplicationContext()).read(offset, request.getId(), request.getLastModified());
                        if (contentLength < 0) {
                            contentLength = FileCache.getInstance(context.getApplicationContext()).getContentLength(request.getId(), request.getLastModified());
                        }
                        if (headers == null) {
                            headers = FileCache.getInstance(context.getApplicationContext()).getHeaders(request.getId(), request.getLastModified());
                        }
                    } catch (CacheException ce) {
                        if (offsetFound) {
                            state = STATE_ABORTED;
                            throw ce;
                        } else {
                            requireNewHandler = true;
                        }
                    }
                }
                if (requireNewHandler) {
                    request.setOffset(offset);
                    ConnectionItem item = httpApi.getConnection(request);
                    if (item != null) {
                        try {
                            result = item.read(params);
                            if (contentLength < 0) {
                                contentLength = item.getContentLength();
                            }
                            if (headers == null) {
                                headers = item.getAllHeaders();
                            }
                            if (result >=0) {
                                synchronized (handlers) {
                                    if (state != STATE_TERMINATED) {
                                        handlers.add(new CacheHandler(item, request.getOffset(), result, params, bufferManager, request.getId(), request.getLastModified(), context));
                                    } else {
                                        item.abort();
                                    }
                                }
                            } else {
                                item.abort();
                            }
                        } catch (IOException e) {
                            item.abort();
                            throw e;
                        }
                    }
                }
            } else if (state == STATE_OFFLINE) {
                request.setOffset(offset);
                synchronized (this) {
                    if (connectionItem == null) {
                        connectionItem = httpApi.getConnection(request);
                        connectionOffset = request.getOffset();
                        if (connectionItem != null) {
                            result = connectionItem.read(params);
                            if (contentLength < 0) {
                                contentLength = connectionItem.getContentLength();
                            }
                            if (headers == null) {
                                headers = connectionItem.getAllHeaders();
                            }
                            connectionOffset++;
                        } else {
                            throw new IOException("No http connection made.");
                        }
                    } else {
                        if (connectionOffset > offset) {
                            connectionItem.abort();
                            connectionItem = null;
                            connectionOffset = 0;
                            throw new IOException("Illegal offset");
                        } else {
                            int offset_diff = offset - connectionOffset;
                            int val = 0;
                            while (offset_diff > 0 && val >= 0) {
                                val = connectionItem.read(params);
                                connectionOffset++;
                                offset_diff--;
                            }
                            if (val < 0) {
                                result = val;
                            } else {
                                result = connectionItem.read(params);
                                if (contentLength < 0) {
                                    contentLength = connectionItem.getContentLength();
                                }
                                if (headers == null) {
                                    headers = connectionItem.getAllHeaders();
                                }
                                connectionOffset++;
                            }
                        }
                    }
                }
            } else if (state == STATE_TERMINATED) {
                throw new IOException("Terminated cache connection.");
            } else if (state == STATE_ABORTED) {
                throw new CacheException();
            }
            return result;
        }
        
        public int read(final byte[] b, int offset, final DataRequest request, final Params params) throws IOException, CacheException {
            int result = -1;
            if (state == STATE_CONNECTED) {
                //LogUtil.debug(this, "### CONNECTED");
                boolean offsetFound = false;
                boolean exceptionOccured = false;
                boolean requireNewHandler = false;
                int index = -1;
                for (int i = 0; i < handlers.size() && !offsetFound; ++i) {
                    offsetFound = handlers.get(i).hasOffset(offset);
                    index = i;
                }
                if (offsetFound) {
                    try {
                        //LogUtil.debug(this, "### Offset found");
                        result = handlers.get(index).read(offset, b);
                    } catch (CacheException ce) {
                        exceptionOccured = true;
                    } catch (ArrayIndexOutOfBoundsException ae) {
                        offsetFound = false;
                    }
                }
                if (!offsetFound || exceptionOccured) {
                    try {
                        //LogUtil.debug(this, "### Read File cache");
                        result = FileCache.getInstance(context.getApplicationContext()).read(b, offset, request.getId(), request.getLastModified());
                        if (contentLength < 0) {
                            contentLength = FileCache.getInstance(context.getApplicationContext()).getContentLength(request.getId(), request.getLastModified());
                        }
                        if (headers == null) {
                            FileCache.getInstance(context.getApplicationContext()).getHeaders(request.getId(), request.getLastModified());
                        }
                    } catch (CacheException ce) {
                        if (offsetFound) {
                            state = STATE_ABORTED;
                            throw ce;
                        } else {
                            requireNewHandler = true;
                        }
                    }
                }
                if (requireNewHandler) {
                    request.setOffset(offset);
                    ConnectionItem item = httpApi.getConnection(request);
                    if (item != null) {
                        try {
                            result = item.read(b, params);
                            if (contentLength < 0) {
                                contentLength = item.getContentLength();
                            }
                            if (headers == null) {
                                headers = item.getAllHeaders();
                            }
                            if (result >=0) {
                                synchronized (handlers) {
                                    if (state != STATE_TERMINATED) {
                                        handlers.add(new CacheHandler(item, request.getOffset(), b, result, params, bufferManager, request.getId(), request.getLastModified(), context));
                                    } else {
                                        item.abort();
                                    }
                                }
                            } else {
                                item.abort();
                            }
                        } catch (IOException e) {
                            item.abort();
                            throw e;
                        }
                    }
                }
            } else if (state == STATE_OFFLINE) {
                request.setOffset(offset);
                synchronized (this) {
                    if (connectionItem == null) {
                        connectionItem = httpApi.getConnection(request);
                        connectionOffset = request.getOffset();
                        if (connectionItem != null) {
                            result = connectionItem.read(b, params);
                            if (result > 0) {
                                connectionOffset+=result;
                            }
                        } else {
                            throw new IOException("No http connection made.");
                        }
                    } else {
                        if (connectionOffset > offset) {
                            connectionItem.abort();
                            connectionItem = null;
                            connectionOffset = 0;
                            throw new IOException("Illegal offset");
                        } else {
                            int offset_diff = offset - connectionOffset;
                            int val = 0;
                            while (offset_diff > 0 && val >= 0) {
                                val = connectionItem.read(params);
                                offset_diff--;
                                connectionOffset++;
                            }
                            if (val < 0) {
                                result = val;
                            } else {
                                result = connectionItem.read(b, params);
                                if (result > 0) {
                                    connectionOffset+=result;
                                }
                            }
                        }
                    }
                }
            } else if (state == STATE_TERMINATED) {
                throw new IOException("Terminated cache connection.");
            } else if (state == STATE_ABORTED) {
                throw new CacheException();
            }
            return result;
        }
        
        public int read(final byte[] b, int b_offset, int b_length, int offset, final DataRequest request, final Params params) throws IOException, CacheException {
            int result = -1;
            if (state == STATE_CONNECTED) {
                boolean offsetFound = false;
                boolean exceptionOccured = false;
                boolean requireNewHandler = false;
                int index = -1;
                for (int i = 0; i < handlers.size() && !offsetFound; ++i) {
                    offsetFound = handlers.get(i).hasOffset(offset);
                    index = i;
                }
                if (offsetFound) {
                    try {
                        result = handlers.get(index).read(offset, b, b_offset, b_length);
                    } catch (CacheException ce) {
                        exceptionOccured = true;
                    } catch (ArrayIndexOutOfBoundsException ae) {
                        offsetFound = false;
                    }
                }
                if (!offsetFound || exceptionOccured) {
                    try {
                        result = FileCache.getInstance(context.getApplicationContext()).read(b, b_length, b_offset, offset, request.getId(), request.getLastModified());
                        if (contentLength < 0) {
                            contentLength = FileCache.getInstance(context.getApplicationContext()).getContentLength(request.getId(), request.getLastModified());
                        }
                        if (headers == null) {
                            headers = FileCache.getInstance(context.getApplicationContext()).getHeaders(request.getId(), request.getLastModified());
                        }
                    } catch (CacheException ce) {
                        if (offsetFound) {
                            state = STATE_ABORTED;
                            throw ce;
                        } else {
                            requireNewHandler = true;
                        }
                    }
                }
                if (requireNewHandler) {
                    request.setOffset(offset);
                    ConnectionItem item = httpApi.getConnection(request);
                    if (item != null) {
                        try {
                            result = item.read(b, b_offset, b_length, params);
                            if (contentLength < 0) {
                                contentLength = item.getContentLength();
                            }
                            if (headers == null) {
                                headers = item.getAllHeaders();
                            }
                            if (result >=0) {
                                synchronized (handlers) {
                                    if (state != STATE_TERMINATED) {
                                        handlers.add(new CacheHandler(item, request.getOffset(), b, result, params, bufferManager, request.getId(), request.getLastModified(), context));
                                    } else {
                                        item.abort();
                                    }
                                }
                            } else {
                                item.abort();
                            }
                        } catch (IOException e) {
                            item.abort();
                            throw e;
                        }
                    }
                }
            } else if (state == STATE_OFFLINE) {
                request.setOffset(offset);
                synchronized (this) {
                    if (connectionItem == null) {
                        connectionItem = httpApi.getConnection(request);
                        connectionOffset = request.getOffset();
                        if (connectionItem != null) {
                            result = connectionItem.read(b, b_offset, b_length, params);
                            if (result > 0) {
                                connectionOffset+=result;
                            }
                        } else {
                            throw new IOException("No http connection made.");
                        }
                    } else {
                        if (connectionOffset > offset) {
                            connectionItem.abort();
                            connectionItem = null;
                            connectionOffset = 0;
                            throw new IOException("Illegal offset");
                        } else {
                            int offset_diff = offset - connectionOffset;
                            int val = 0;
                            while (offset_diff > 0 && val >= 0) {
                                val = connectionItem.read(params);
                                offset_diff--;
                                connectionOffset++;
                            }
                            if (val < 0) {
                                result = val;
                            } else {
                                result = connectionItem.read(b, b_offset, b_length, params);
                                if (result > 0) {
                                    connectionOffset+=result;
                                }
                            }
                        }
                    }
                }
            } else if (state == STATE_TERMINATED) {
                throw new IOException("Terminated cache connection.");
            } else if (state == STATE_ABORTED) {
                throw new CacheException();
            }
            return result;
        }
        
        public CacheConnection refresh(final DataRequest request) {
            this.release();
            return new CacheConnection(request.getId(), request.getLastModified(), httpApi, bufferManager, false);
        }
    }

    public CacheAPI(final HttpAPI httpApi, final Context context) {
        this.context = context;
        this.httpApi = httpApi;
        this.bufferManager = new BufferManager(4, 64 * 1024);
        this.connections = new Vector<CacheConnection>();
    }
    
    @Override
    public ConnectionItem getConnection(final DataRequest request) throws IOException {
        return new CachableConnectionItem(request, this);
    }
    
    private synchronized void removeConnection(CacheConnection connection) {
        int index = 0; 
        while (index < connections.size()) {
            if (connections.get(index).isMatch(connection)) {
                connections.remove(index);
            } else {
                index++;
            }
        }
    }
    
    CacheConnection connect(final DataRequest request) {
        boolean found = false;
        CacheConnection result = null;
        synchronized (this) {
            for (int i = 0; i < connections.size() && !found; ++i) {
                if (connections.get(i) != null && connections.get(i).isMatch(request.getId(), request.getLastModified())) {
                    result = connections.get(i);
                    found = true;
                }
            }
        }
        if (result != null) {
            result.addRef();
        } else {
            result = new CacheConnection(request.getId(), request.getLastModified(), httpApi, bufferManager, true);
            connections.add(result);
        }
        return result;
    }
}
