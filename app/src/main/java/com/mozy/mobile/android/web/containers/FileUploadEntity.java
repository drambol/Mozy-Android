package com.mozy.mobile.android.web.containers;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.ProgressOutputStream.ProgressListener;

public class FileUploadEntity extends AbstractHttpEntity 
{
    
    private RandomAccessFile randomAccessfile;
    private FileUploadState fileState;
    private long startOffset;
    private ProgressListener listener;

    
    public FileUploadEntity(final FileUploadState fileUploadState, long offset, ProgressListener listener) 
    {
        super();
        
        this.fileState = fileUploadState;
        this.listener = listener;
        
        String contentType = null;
        
        if (this.fileState.localFile == null) 
        {
            throw new IllegalArgumentException("File may not be null");
        }
        
        contentType = FileUtils.getMimeTypeFromFileName(fileState.fileName);
        
        try 
        {
            this.randomAccessfile = new RandomAccessFile(this.fileState.localFile, "r");
        } 
        catch (FileNotFoundException e) 
        {
             throw new IllegalArgumentException("File could not be found");
        }
        
        setContentType(contentType);
        this.startOffset = offset;
    }

    public boolean isRepeatable() 
    {
        return false;
    }

    public long getContentLength() 
    {
        try
        {
            return this.randomAccessfile.length();
        }
        catch (Throwable t)
        {
            LogUtil.exception(getClass().getName(), "file.length() failed:", t);            
        }
        
        return 0;
    }
    
    public InputStream getContent() throws IOException 
    {
        throw new IOException("Not supported");
    }
    
    public void writeTo(final OutputStream outputStream) throws IOException 
    {
        long totBytes = 0;
        
        if (outputStream == null) 
        {
            throw new IllegalArgumentException("Output stream may not be null");
        }
        
        ProgressOutputStream progressOutputStream = new ProgressOutputStream(outputStream, listener);
        
        try 
        {
            this.randomAccessfile.seek(this.startOffset);
            
            byte[] tmp = new byte[2048];
            int numBytes;
            
            long contentLength = this.randomAccessfile.length();
            while ((numBytes = randomAccessfile.read(tmp)) != -1 && totBytes < contentLength) 
            {
                if ((totBytes + numBytes) > contentLength) 
                {
                    progressOutputStream.write(tmp, 0, (int)(contentLength - totBytes));
                    totBytes += (contentLength - totBytes);
                } 
                else 
                {
                    progressOutputStream.write(tmp, 0, numBytes);
                    totBytes += numBytes;
                }
            }
            System.out.println("Total bytes tranferred: " + totBytes);
            progressOutputStream.flush();
        } 
        finally 
        {
            randomAccessfile.close();
            progressOutputStream.close();
        }
    }

    public boolean isStreaming() {
        return false;
    }
} 
