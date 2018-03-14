package com.mozy.mobile.android.activities.upload;

import java.io.File;

public class uploadFile
{
    protected String fullPath;
    protected String mimeType;
    protected File file;
    protected String destPath;
    
    public uploadFile(String fullPath, String mimeType, String destPath)
    {
        this.fullPath = fullPath;
        this.mimeType = mimeType;
        this.file = new File(this.fullPath);
        this.destPath = destPath;
    }

    public File getFile() {
        return file;
    } 
    
    public String getFullPath() {
        return fullPath;
    }

    public String getMimeType() {
        return mimeType;
    }
    
    public String getDestPath() {
        return destPath;
    }

    public void setDestPath(String dPath) { destPath = dPath; }
    
}