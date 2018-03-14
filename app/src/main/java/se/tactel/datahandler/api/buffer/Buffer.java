package se.tactel.datahandler.api.buffer;

import java.nio.ByteBuffer;

import com.mozy.mobile.android.utils.LogUtil;

public class Buffer {
    
    private static final int STATE_READY = 0;
    private static final int STATE_FULL = 1;
    
    private ByteBuffer buffer;
    private int totalSize;
    private int currentSize;
    private int id;
    private int state;
    private boolean is_locked;
    private byte[] bytes;
    
    public Buffer(int size, final int id) {
        try
        {
            this.bytes = new byte[size];
            this.buffer = ByteBuffer.wrap(bytes);
            this.totalSize = size;
            this.currentSize = 0;
            this.id = id;
            this.state = STATE_READY;
            this.is_locked = false;
        }
        catch(OutOfMemoryError e)
        {
            LogUtil.debug(this, e.getMessage());
            this.bytes = null;
            this.buffer = null;
            this.id  = id;
            this.state = STATE_FULL;
            this.currentSize = -1;
            this.is_locked = false;
        }
    }
    
    public synchronized int write(byte[] b, int offset, int length) {
        if (state == STATE_FULL) {
            return -1;
        }
        int len = length - offset;
        if ((totalSize - currentSize) < len) {
            len = totalSize - currentSize;
        }
        this.buffer.put(b, offset, len);
        currentSize += len;
        if (currentSize >= totalSize) {
            state = STATE_FULL;
            currentSize = totalSize;
        }
        return len;
    }
    
    public synchronized void lock() {
        this.is_locked = true;
    }
    
    public synchronized int read(int offset) {
        if (offset >= currentSize) {
            return -1;
        }
        if (buffer.position() != offset) {
            buffer.position(offset);
        }
        int result = -1;
        try {
            result = buffer.get(offset) & 0xFF;
        } catch (Exception e) {
        }
        return result;
    }
    
    public synchronized int read(final byte[] b, int offset) {
        if (offset >= currentSize) {
            return -1;
        }
        int oldPosition = buffer.position();
        if (buffer.position() != offset) {
            buffer.position(offset);
        }
        int len = b.length;
        if (len > (currentSize - offset)) {
            len = currentSize - offset;
        }
        buffer.get(b, 0, len);
        buffer.position(oldPosition);
        return len;
    }
    
    public synchronized int read(final byte[] b, int b_offset, int b_length, int offset) {
        if (offset >= currentSize) {
            return -1;
        }
        int oldPosition = buffer.position();
        if (buffer.position() != offset) {
            buffer.position(offset);
        }
        int len = b_length;
        if (len > (currentSize - offset)) {
            len = currentSize - offset;
        }
        buffer.get(b, b_offset, len);
        buffer.position(oldPosition);
        return len;
    }
    
    public synchronized ByteBuffer getContent() {
        if (!is_locked) {
            return null;
        }
        return buffer;
    }
    
    public int getContentLength() {
        return currentSize;
    }
    
    public boolean isFull() {
        return state == STATE_FULL;
    }
    
    synchronized void reset() {
        is_locked = false;
        currentSize = 0;
        buffer.clear();
        state = STATE_READY;
    }
    
    int getId() {
        return id;
    }
}
