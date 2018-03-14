package com.mozy.mobile.android.web.containers;

import java.io.File;

public class FileUploadState 
{
    public FileUploadState(File file, String fileName, long fileLength)
    {
        this.localFile = file;
        this.fileName = fileName;
    }
    public String linkToCloud;
    public File localFile;
    public String fileName;
    
	public void setLinkToCloud(String linkToCloud) {
		this.linkToCloud = linkToCloud;
	}
	
	public long getFinalFileSize() {
		return this.localFile.length();
	}
};

