/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
 *                                    _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */
package se.tactel.datahandler.streams;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.StatusLine;

import se.tactel.datahandler.DataConnectionManager;
import se.tactel.datahandler.DataRequest;
import se.tactel.datahandler.HttpException;
import se.tactel.datahandler.DataManager.Params;
import se.tactel.datahandler.api.ConnectionItem;

/**
 * Stream for retrieving data from file system, cache or http depending on where data is available.
 * @author danielolofsson
 *
 */
public class DataInputStream extends InputStream {

    private final DataRequest request;
    private Params params;
    private DataConnectionManager connectionManager;
    private ConnectionItem connection;
    private int index;
    private boolean closed;

    /**
     * Constructor for DataInputStream that is used internally for creating streams. These will themselves connect to the proper location for retrieving data.
     * 
     * @param request The request used to retrieve data, contains necessary data to connect through http.
     * @param params Specific parameters designating how to handle http communication.
     * @param connectionManager A manager that allows the stream to stream to connect to data through ConnectionItems.
     */
    public DataInputStream(final DataRequest request, final Params params, final DataConnectionManager connectionManager) {
        this.request = request;
        this.params = params;
        this.connectionManager = connectionManager;
        this.connection = null;
        this.index = request.getInitialOffset();
        this.closed = false;

    }

    @Override
    public int read() throws IOException {
        if (closed) {
            throw new IOException("InputStream is closed.");
        }
        ConnectionItem item = null;
        synchronized (this) {
            item = connection;
        }
        boolean freshConnection = false;
        if (item == null && !closed) {
            freshConnection = true;
            synchronized (this) {
                connection = connectionManager.connect(request, params, index);
                item = connection;
            }
        }
        if (item != null) {
            int ret = -1;
            try {
                ret = item.read(params);
            } catch (HttpException he) {
                throw he;
            } catch (IOException ie) {
                ie.printStackTrace();
                synchronized (this) {
                    item = connection;
                    connection = null;
                }
                if (item != null) {
                    item.abort();
                }
                if (!freshConnection) {
                    synchronized (this) {
                        connection = connectionManager.connect(request, params, index);
                        item = connection;
                    }
                }

                if (item != null) {
                    ret = item.read(params);
                } else {
                    throw ie;
                }
            }
            if (ret >= 0) {
                index++;
            }
            return ret;
        } else {
            throw new IOException("No connection to read single byte.");
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (closed) {
            throw new IOException("InputStream is closed.");
        }
        ConnectionItem item = null;
        synchronized (this) {
            item = connection;
        }
        boolean freshConnection = false;
        if (item == null && !closed) {
            freshConnection = true;
            synchronized (this) {
                connection = connectionManager.connect(request, params, index);
                item = connection;
            }
        }
        if (item != null) {
            int ret = -1;
            try {
                ret = item.read(b, params);
            } catch (HttpException he) {
                throw he;
            } catch (IOException ie) {
                ie.printStackTrace();
                synchronized (this) {
                    item = connection;
                    connection = null;
                }
                if (item != null) {
                    item.abort();
                }
                if (!freshConnection) {
                    synchronized (this) {
                        connection = connectionManager.connect(request, params, index);
                        item = connection;
                    }
                }

                if (item != null) {
                    ret = item.read(b, params);
                } else {
                    throw ie;
                }
            }
            if (ret >= 0) {
                index += ret;
            }
            return ret;
        } else {
            throw new IOException("No connection to read byte array.");
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (closed) {
            throw new IOException("InputStream is closed.");
        }

        ConnectionItem item = null;
        synchronized (this) {
            item = connection;
        }
        boolean freshConnection = false;
        if (item == null && !closed) {
            freshConnection = true;
            synchronized (this) {
                connection = connectionManager.connect(request, params, index);
                item = connection;
            }
        }
        if (item != null) {
            int ret = -1;
            try {
                ret = item.read(b, off, len, params);
            } catch (HttpException he) {
                throw he;
            } catch (IOException ie) {
                ie.printStackTrace();
                synchronized (this) {
                    item = connection;
                    connection = null;
                }
                if (item != null) {
                    item.abort();
                }
                if (!freshConnection) {
                    synchronized (this) {
                        connection = connectionManager.connect(request, params, index);
                        item = connection;
                    }
                }

                if (item != null) {
                    ret = item.read(b, off, len, params);
                } else {
                    throw ie;
                }
            }
            if (ret >= 0) {
                index += ret;
            }
            return ret;
        } else {
            throw new IOException("No connection to read byte array.");
        }
    }

    @Override
    public void close() {
        try {
            if (request != null) {
                request.abort();
            }
            this.closed = true;
            ConnectionItem item = null;
            synchronized (this) {
                if (connection != null) {
                    item = connection;
                    connection = null;
                }
            }

            if (item != null) {
                item.abort();
            }
        } catch (Exception e) {
        }
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    /**
     * Returns the content length if this is known, should only be called after a successful read command. Cant assure that the stream has connected to any data if no successful read has been performed.
     * 
     * @return -1 if content length is not yet known, else returns the content length of the data.
     */
    public long getContentLength() {
        long content_length = -1;
        synchronized (this) {
            if (connection != null) {
                content_length = connection.getContentLength();
            }
        }
        return content_length;
    }

    /**
     * Returns any header stored either in cache database or through http connection, there is no insurance that these headers are the latest known and can be custom created in certain situations. Only guarantee is that they do contain a content-type when this is known.
     * @return an array of headers when known, returns null if no header was stored or stream has not connected to any data.
     */
    public Header[] getAllHeaders() {
        Header[] headers = null;
        synchronized (this) {
            if (connection != null) {
                headers = connection.getAllHeaders();
            }
        }
        return headers;
    }

    /**
     * Retrieves the known content type of current data, returns null if no known data is found or stream has not connected to data.
     * @return null if no known content type, else returns the content type of this data.
     */
    public String getContentType() {
        Header[] headers = getAllHeaders();
        String result = null;
        if (headers != null) {
            boolean done = false;
            for (int i = 0; i < headers.length && !done; ++i) {
                if (headers[i].getName().equalsIgnoreCase("Content-Type")) {
                    result = headers[i].getValue();
                    int lastIndex = result.indexOf(';');
                    if (lastIndex >= 0) {
                        result = result.substring(0, lastIndex);
                    }
                    done = true;
                }
            }
        }
        return result;
    }

    public StatusLine getHttpStatus() {
        StatusLine result = null;
        synchronized (this) {
            if (connection != null && !closed) {
                result = connection.getHttpStatus();
            }
        }
        return result;
    }

    /**
     * Not yet implemented.
     * @return 0
     */
    public int available() {
        return 0;
    }
}
