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

import se.tactel.datahandler.DataManager.Params;
import se.tactel.datahandler.api.buffer.Buffer;
import se.tactel.datahandler.api.buffer.BufferManager;
import se.tactel.datahandler.db.FileCache;
import android.content.Context;

public class CacheHandler implements Runnable {

    private static final int BLOCK_SIZE = 2048;
    private static final int SAFETY_YIELD_DELAY = 20;

    private final BufferManager bufferManager;
    private ConnectionItem item;
    private byte[] block;
    private int blockOffset;
    private int blockLength;
    private byte[] tmpBlock;
    private int tmpLength;
    private int tmpOffset;
    private Thread thread;
    private boolean running;
    private final Params params;
    private Buffer buffer;
    private IOException lastException;
    private boolean endOccured;
    private int initialOffset;
    private boolean closed;

    private long startOffset;
    private long contentLength;
    private Object store_lock;
    private Object buffer_lock;
    int storeOffset;

    private final String pin_id;
    private final long pin_timestamp;

    private Context context;
    private Header[] headers;

    CacheHandler(ConnectionItem item, int startOffset, int oneByte, final Params params, final BufferManager bufferManager, String pin_id, long pin_timestamp, Context context) {
        this.startOffset = startOffset;
        this.contentLength = item.getContentLength();
        this.initialOffset = startOffset;
        this.headers = null;

        this.store_lock = new Object();
        this.buffer_lock = new Object();
        this.bufferManager = bufferManager;
        this.item = item;
        this.block = new byte[BLOCK_SIZE];
        this.blockLength = 0;
        this.blockOffset = 0;
        this.closed = false;
        if (oneByte >= 0) {
            synchronized (this) {
                this.block[blockLength] = (byte)oneByte;
                blockLength++;
            }
        } else {
            endOccured = true;
        }
        tmpBlock = null;
        tmpLength = 0;
        tmpOffset = 0;
        this.lastException = null;
        this.endOccured = false;
        this.buffer = null;
        this.params = params;
        this.running = true;
        this.pin_id = pin_id;
        this.pin_timestamp = pin_timestamp;
        this.context = context;
        this.thread = new Thread(this);
        thread.start();
    }

    CacheHandler(ConnectionItem item, int startOffset, byte[] b, int length, final Params params, final BufferManager bufferManager, String pin_id, long pin_timestamp, Context context) {
        this.startOffset = startOffset;
        this.contentLength = item.getContentLength();
        this.initialOffset = startOffset;
        this.headers = null;

        this.store_lock = new Object();
        this.buffer_lock = new Object();
        this.bufferManager = bufferManager;
        this.item = item;
        this.block = new byte[BLOCK_SIZE];
        this.blockLength = 0;
        this.blockOffset = 0;
        tmpBlock = null;
        tmpLength = 0;
        tmpOffset = 0;
        this.endOccured = false;
        if (length >= 0) {
            tmpBlock = b;
            tmpLength = length;
            tmpOffset = 0;
        } else {
            endOccured = true;
        }
        this.closed = false;
        this.lastException = null;
        this.buffer = null;
        this.params = params;
        this.running = true;
        this.pin_id = pin_id;
        this.pin_timestamp = pin_timestamp;
        this.context = context;
        this.thread = new Thread(this);
        thread.start();
    }

    public void close() {
        closed = true;
        if (tmpLength > 0 && tmpBlock != null) {
            synchronized (store_lock) {
                getBuffer(false);
                int written = -1;
                boolean quit = false;
                while (tmpLength > 0 && tmpBlock != null && !quit) {
                    synchronized (buffer_lock) {
                        if (buffer != null) {
                            written = buffer.write(tmpBlock, tmpOffset, tmpLength);
                        }
                    }
                    if (written > 0) {
                        tmpOffset += written;
                        if (tmpOffset >= tmpLength) {
                            tmpLength = 0;
                            tmpOffset = 0;
                            tmpBlock = null;
                        }
                        synchronized (this) {
                            notifyAll();
                        }
                    } else {
                        quit = true;
                    }
                }
            }
        }
        if (blockLength - blockOffset > 0) {
            boolean quit = false;
            synchronized (store_lock) {
                while (blockLength - blockOffset > 0 && !quit) {
                    getBuffer(false);
                    int written = -1;
                    synchronized (buffer_lock) {
                        if (buffer != null) {
                            written = buffer.write(block, blockOffset, blockLength);
                        }
                    }
                    if (written > 0) {
                        blockOffset += written;
                        synchronized (this) {
                            notifyAll();
                        }
                    } else {
                        quit = true;
                    }
                }
            }
        }
        getBuffer(true);
        synchronized (buffer_lock) {
            if (buffer != null) {
                bufferManager.releaseBuffer(buffer);
                buffer = null;
            }
            startOffset = -1;
            running = false;
        }
        synchronized (this) {
            if (item != null) {
                item.abort();
            }

            bufferManager.abort();
            notifyAll();
        }
    }

    long getContentLength() {
        return contentLength;
    }

    boolean hasOffset(int offset) {
        return offset >= initialOffset;
    }

    synchronized int read(int offset) throws CacheException, IOException {
        int result = -1;
        boolean done = false;

        if (offset < contentLength || contentLength < 0) {
            while (!done) {
                synchronized (buffer_lock) {
                    if (startOffset < 0 || closed || startOffset > offset) {
                        throw new CacheException();
                    } else if (buffer != null && buffer.getContentLength() + startOffset > offset) {
                        result = buffer.read((int)(offset - startOffset));
                        if (result >= 0) {
                        }
                        done = true;
                    }
                }
                if (!done) {
                    if (endOccured) {
                        throw new CacheException();
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        getIOException();
                    }
                }
            }
        } else {
            getIOException();
        }

        return result;
    }

    synchronized int read(int offset, byte[] b) throws CacheException, IOException {
        int result = -1;
        boolean done = false;

        if (offset < contentLength || contentLength < 0) {
            while (!done) {
                synchronized (buffer_lock) {
                    if (startOffset < 0 || closed || startOffset > offset) {
                        throw new CacheException();
                    } else if (buffer != null && buffer.getContentLength() + startOffset > offset) {
                        result = buffer.read(b, (int)(offset - startOffset));
                        if (result >= 0) {
                        }
                        done = true;
                    }
                }
                if (!done) {
                    if (endOccured) {
                        throw new CacheException();
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        getIOException();
                    }
                }
            }
        } else {
            getIOException();
        }

        return result;
    }

    synchronized int read(int offset, byte[] b, int b_offset, int b_length) throws CacheException, IOException {
        int result = -1;
        boolean done = false;

        if (offset < contentLength || contentLength < 0) {
            while (!done) {
                synchronized (buffer_lock) {
                    if (startOffset < 0 || closed || startOffset > offset) {
                        throw new CacheException();
                    } else if (buffer != null && buffer.getContentLength() + startOffset > offset) {
                        result = buffer.read(b, b_offset, b_length, (int)(offset - startOffset));
                        if (result >= 0) {
                        }
                        done = true;
                    }
                }
                if (!done) {
                    if (endOccured) {
                        throw new CacheException();
                    } else {
                        try {
                            wait();
                        } catch (InterruptedException ie) {
                            ie.printStackTrace();
                        }
                        getIOException();
                    }
                }
            }
        } else {
            getIOException();
        }

        return result;
    }

    @Override
    public void run() {
        while (running) {
            if (tmpLength > 0) {
                synchronized (store_lock) {
                    if (tmpBlock != null) {
                        getBuffer(false);
                        boolean yield = true;
                        int written = -1;
                        synchronized (buffer_lock) {
                            if (buffer != null) {
                                written = buffer.write(tmpBlock, tmpOffset, tmpLength);
                                yield = false;
                            }
                        }
                        if (written > 0) {
                            tmpOffset += written;
                            if (tmpOffset >= tmpLength) {
                                tmpLength = 0;
                                tmpOffset = 0;
                                tmpBlock = null;
                            }
                            synchronized (this) {
                                notifyAll();
                            }
                        } else if (yield) {
                            synchronized (this) {
                                try {
                                    wait(SAFETY_YIELD_DELAY);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        tmpLength = 0;
                    }
                }
            } else if (blockLength - blockOffset > 0) {
                synchronized (store_lock) {
                    getBuffer(false);
                    boolean yield = true;
                    int written = -1;
                    synchronized (buffer_lock) {
                        if (buffer != null && blockLength - blockOffset > 0) {
                            written = buffer.write(block, blockOffset, blockLength);
                            yield = false;
                        }
                    }
                    if (written > 0) {
                        blockOffset += written;
                        synchronized (this) {
                            notifyAll();
                        }
                    } else if (yield) {
                        synchronized (this) {
                            try {
                                wait(SAFETY_YIELD_DELAY);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } else if (!endOccured) {
                blockLength = 0;
                blockOffset = 0;
                try {
                    blockLength = item.read(block, params);
                    setContentLength(item.getContentLength());
                    if (headers == null) {
                        headers = item.getAllHeaders();
                    }
                    if (blockLength < 0) {
                        blockLength = 0;
                        running = false;
                        setEnd(true);
                        getBuffer(true);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    setIOException(e);
                    synchronized (this) {
                        notifyAll();
                    }
                }
            } else {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {}
                }
            }
        }
    }

    private void getBuffer(boolean forceStore) {
        Buffer tmpBuffer = null;
        int offset;
        FileCache.IntervalLock store_lock = null;
        boolean refreshBuffer = false;
        synchronized (buffer_lock) {
            if (buffer != null && (buffer.isFull() || forceStore)) {
                tmpBuffer = buffer;
                offset = (int) startOffset;
                store_lock = FileCache.getInstance(context.getApplicationContext()).getStoreMethod(tmpBuffer, tmpBuffer.getContentLength(), (int) contentLength, offset, pin_id, pin_timestamp, headers);
                startOffset += tmpBuffer.getContentLength();
                buffer = null;
            } else {
                tmpBuffer = null;
            }
            if (buffer == null && !endOccured) {
                refreshBuffer = true;
            }
        }
        if (store_lock != null && tmpBuffer != null) {
            tmpBuffer.lock();
            try {
                store_lock.executeStore();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bufferManager.releaseBuffer(tmpBuffer);
        }
        synchronized (this) {
            notifyAll();
        }
        if (refreshBuffer) {
            tmpBuffer = bufferManager.getBuffer();
        }
        boolean bufferUsed = false;
        synchronized (buffer_lock) {
            if (buffer == null && tmpBuffer != null) {
                buffer = tmpBuffer;
                bufferUsed = true;
            }
        }
        if (!bufferUsed) {
            bufferManager.releaseBuffer(tmpBuffer);
        }
    }

    private synchronized void setContentLength(long contentLength) {
        if (this.contentLength != contentLength) {
            this.contentLength = contentLength;
            notifyAll();
        }
    }

    private synchronized void setEnd(boolean endReached) {
        if (endReached != endOccured) {
            endOccured = endReached;
            notifyAll();
        }
    }

    private synchronized void setIOException(IOException e) {
        while (lastException != null) {
            try {
                wait();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        if (lastException == null) {
            lastException = e;
        }
    }

    private synchronized void getIOException() throws IOException {
        IOException result = lastException;
        lastException = null;
        notifyAll();
        if (result != null) {
            throw result;
        }
    }
}
