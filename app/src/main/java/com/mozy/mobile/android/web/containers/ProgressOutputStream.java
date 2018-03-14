package com.mozy.mobile.android.web.containers;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutputStream extends FilterOutputStream {
    
    public static interface ProgressListener {
        public void onProgressUpdate(long transferred);
    }
    
    private static final int UPDATE_TIMER_DELAY = 200;
    private final ProgressListener listener;
    private long transferred;
    private long lastUpdate;
    
    public ProgressOutputStream(OutputStream out, ProgressListener listener) {
        super(out);
        this.listener = listener;
        transferred = 0;
        lastUpdate = 0;
    }
    
    public void write(byte[] b, int off, int len) throws IOException
    {
        super.write(b, off, len);
    }

    public void write(int b) throws IOException {
        super.write(b);

        if(this.listener != null)
        {
            this.transferred++;
            long updateTime = System.currentTimeMillis();
            if (updateTime - lastUpdate > UPDATE_TIMER_DELAY) {
                this.listener.onProgressUpdate(transferred);
                lastUpdate = updateTime;
            }
        }
    } 
}
