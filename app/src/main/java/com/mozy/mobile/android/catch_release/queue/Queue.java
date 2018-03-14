package com.mozy.mobile.android.catch_release.queue;

import java.io.File;
import java.util.Vector;
import android.content.ContentValues;
import android.database.Cursor;

import com.mozy.mobile.android.utils.LogUtil;

public class Queue {
    private static final String TAG = Queue.class.getSimpleName();

    private int index;
 

    private QueueDatabase db_helper;
    private boolean closed;
    private boolean valid;
    private Listener listener;
    private Object callbackLock;

    private String data_path;
    private String mime;
    private int uploadType;
    private String dest_path;

    public int getUploadType() {
        return uploadType;
    }

    public static interface Listener {
        void onContentChanged(int uploadType);
        void onContentRemoved(int uploadType);
    }
    

    Queue(QueueDatabase db_helper, int uploadType) {
        this.index = -1;
        this.db_helper = db_helper;
        this.closed = false;
        this.valid = false;

        this.data_path = null;
        this.dest_path = null;
        this.listener = null;
        this.callbackLock = new Object();
        
        this.uploadType = uploadType;
    }

    public Vector<String> getQueueFiles()
    {
        LogUtil.debug(TAG, "getQueueFiles()");
        Vector<String> files = new Vector<String>();

        if (closed)
        {
            LogUtil.debug(this, "### CLOSED");
        }
        else
        {
            Cursor cursor = db_helper.getQueue();
            try
            {
                int index = 0;
                while(cursor != null && cursor.moveToNext())
                {
                    String path = cursor.getString(cursor.getColumnIndex(QueueDatabase.KEY_DATA_PATH));
                    String mime = cursor.getString(cursor.getColumnIndex(QueueDatabase.KEY_MIME_TYPE));
                    files.add(path);

                    LogUtil.debug(TAG, "Queue[" + index++ + "] " + path + " (" + mime + ")");
                }
            }
            finally
            {
                if(cursor != null)
                {
                    cursor.close();
                }
            }
        }

        return files;
    }
    
    
    public int getQueueSize()
    {        
        int length = 0;

        if (closed)
        {
            LogUtil.debug(this, "### CLOSED");
        }
        else
        {
            Cursor cursor = db_helper.getQueue();
            try
            {
                if(cursor != null)
                    length = cursor.getCount();
            }
            finally
            {
                if(cursor != null)
                {
                    cursor.close();
                }
            }
        }

        return length;
    }

    public boolean moveToFirst() {
        boolean result = false;
        if (closed) {
            LogUtil.debug(this, "### CLOSED");
            return result;
        }

        Cursor cursor = db_helper.getQueue();
        if (cursor != null) {
            try {
                result = cursor.moveToFirst();
                if (result) {
                    index = saveData(cursor) ? 0 : index;
                }
            } finally {
                cursor.close();
            }
        }

        return result;
    }
    
    
    
    public boolean moveToNext() {
        boolean result = false;
        if (closed) {
            LogUtil.debug(this, "### CLOSED");
            return result;
        }

        
        Cursor cursor = db_helper.getQueue();
        if (cursor != null) 
        {
            try {
                result = cursor.moveToNext();       
                if (result) 
                    index = saveData(cursor) ? 0 : index;
                } 
                finally {
                    cursor.close();
                }
        }
 
        return result;
    }

    public synchronized File getFile() {
        if (index < 0 || !valid) {
            return null;
        }
        LogUtil.debug(this, "### getFile: " + data_path);
        if (data_path != null) {
            return new File(data_path);
        } else {
            return null;
        }
    }
    
    public synchronized String getMime() {
        if (index < 0 || !valid) {
            return null;
        }
        LogUtil.debug(this, "### getMime: " + mime);
        if (mime != null) {
            return mime;
        } else {
            return null;
        }
    }
    
    public synchronized String getDestPath() {
        if (index < 0 || !valid) {
            return null;
        }
        LogUtil.debug(this, "### getDestPath: " + dest_path);
        if (dest_path != null) {
            return dest_path;
        } else {
            return null;
        }
    }

    public synchronized boolean isValid() {
        if (index < 0) {
            return false;
        } else {
            return valid;
        }
    }
    
    public boolean removeCurrent() {    
        return removeEntryForPath(data_path, dest_path);
    }

    public boolean removeEntryForPath(String filePath, String destPath) {
        if (index < 0 || !valid) {
            return false;
        }
        
        LogUtil.debug(TAG, "Removing " +  filePath + "Dest Path: " + destPath);
        
        if (filePath != null) {
            if (db_helper.deleteEntry(filePath, destPath)) {
                valid = false;
                index = -1;
                filePath = null;
                synchronized (callbackLock) {
                    if (listener != null) {
                        listener.onContentRemoved(this.uploadType);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void enqueueFilesInManualQueue(ContentValues[] values)
    {
        LogUtil.debug(TAG, "Enqueueing " + (values != null ? values.length : 0) + " media.. ");

        boolean status = false;
        
        if(values != null)
        {
            for(int i = 0; i < values.length; i++)
            {
               status = db_helper.insertValue(values[i]);
            }
        }
       
        synchronized (callbackLock)
        {
            if (listener != null && values.length > 0 && status)
            {
                listener.onContentChanged(this.uploadType);
            }
        }
    }
    
    public void enqueueFilesCROrFailUploaded(ContentValues[] values, boolean allowed, Queue.Listener listener)
    {
        LogUtil.debug(TAG, "Enqueueing CR" + (values != null ? values.length : 0) + " media.. ");
        
        boolean status = false;

        if(values != null)
        {
            for(int i = 0; i < values.length; i++)
            {
                status = db_helper.insertValue(values[i]);
            }
        }
        
        if(values.length > 0  && status)
            onContentChangedListener(allowed, listener);
    }
    
    
    public boolean existsInQueue(String dataPath, String destPath)
    {
        boolean isExists = false;
        if(dataPath != null && destPath != null)
        {
            isExists = db_helper.isExists(dataPath, destPath);
        }
        return isExists;
    }

    
    
    public void replaceImageInQueue(ContentValues value)
    {
        if(value != null)
        {
            db_helper.replaceValue(value);
        }
    }
    

    /**
     * @param allowed
     */
    public void onContentChangedListener(boolean allowed, Queue.Listener listener) {
        synchronized (callbackLock)
        {
            if (listener != null && allowed)
            {
                listener.onContentChanged(this.uploadType);
            }
        }
    }
    
   

    public void dequeueAll()
    {
        db_helper.dequeueAll();
    }
    
    private synchronized boolean saveData(Cursor cursor) {
        if (cursor != null) {
            data_path = cursor.getString(cursor.getColumnIndex(QueueDatabase.KEY_DATA_PATH));
            mime = cursor.getString(cursor.getColumnIndex(QueueDatabase.KEY_MIME_TYPE));
            dest_path = cursor.getString(cursor.getColumnIndex(QueueDatabase.KEY_DEST_PATH));
            valid = data_path != null;
            return valid;
        }
        return false;
    }

    public void registerListener(Listener listener) {
        synchronized (callbackLock) {
            this.listener = listener;
        }
    }

    public void unregisterListener(Listener listener) {
        synchronized (callbackLock) {
            if (this.listener == listener) {
                this.listener = null;
            }
        }
    }

    public void close() {
        db_helper.close();
    }
}
