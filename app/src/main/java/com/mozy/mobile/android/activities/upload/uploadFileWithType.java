package com.mozy.mobile.android.activities.upload;

public class uploadFileWithType extends uploadFile
{
    public int uploadType;
    public uploadFile uploadfile;

    
    public uploadFileWithType(uploadFile uploadfile, int uploadType)
    {
       super(uploadfile.fullPath, uploadfile.mimeType, uploadfile.destPath);
       this.uploadType = uploadType;
       this.uploadfile = uploadfile;
    } 
    
    public uploadFileWithType(String fullPath, String mimeType, String destPath, int uploadType)
    {
       super(fullPath, mimeType, destPath);
       this.uploadType = uploadType;
    } 
    
    public uploadFile getUploadFileFromList()
    {
        return uploadfile;
    }
}