package se.tactel.datahandler.api.buffer;

import com.mozy.mobile.android.utils.LogUtil;

public class BufferManager {
    
    private Buffer[] buffers;
    private boolean[] freeFlags;
    private int bufferAmount;
    private int currentIndex;
    private boolean aborted;
    
    /**
     * Constructor creating specified amount of buffers with specified size in bytes each.
     * @param amountOfBuffers the amount of buffers to be created.
     * @param sizeOfBuffer the size in bytes of each 
     */
    public BufferManager(int amountOfBuffers, int sizeOfBuffer) {
        this.buffers = new Buffer[amountOfBuffers];
        this.freeFlags = new boolean[amountOfBuffers];
        for (int i = 0; i < amountOfBuffers; ++i) {
            buffers[i] = new Buffer(sizeOfBuffer, i);     
           if( buffers[i].getContentLength() == -1)
           {
               this.bufferAmount = 0;
               this.currentIndex = -1;
               abort();
               LogUtil.debug(this, "Aborted");
               return;
           }
           freeFlags[i] = true;
        }
        this.bufferAmount = amountOfBuffers;
        this.currentIndex = 0;
        this.aborted = false;
    }
    
    public Buffer getBuffer() {
        Buffer result = null;
        synchronized (this) {
            while (currentIndex < 0 && !aborted) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (currentIndex >= 0) {
                aborted = false;
                result = buffers[currentIndex];
                freeFlags[currentIndex] = false;
                boolean found = false;
                for (int i = 0; i < bufferAmount && !found; ++i) {
                    if (freeFlags[i]) {
                        currentIndex = i;
                        found = true;
                        notifyAll();
                    }
                }
            }
        }
        return result;
    }
    
    public void releaseBuffer(final Buffer buffer) {
        if (buffer != null) {
            buffer.reset();
            freeFlags[buffer.getId()] = true;
        }
    }
    
    public synchronized void abort() {
        aborted = true;
        this.notifyAll();
    }
}
