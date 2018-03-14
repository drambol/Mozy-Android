package com.mozy.mobile.android.files;


import com.mozy.mobile.android.R;
import com.mozy.mobile.android.utils.FileUtils;

public class CloudFile extends MozyFile
{
    // This contains the complete MIP link to the file in the format
    // https://domain/account_id/fs/container_id/?path=file_path/file_name
    // For example: "https://mozy.com/912345/fs/165215/?path=F:%2Fimages%2Fheron_1.jpg"
    // 
    protected String link;
    protected long size;
    private String title;
    protected boolean markedForDelete;
    protected int iconResourceId;  
    protected String mimeType;
    protected int category; // e.g. Document, Music, etc... See FileConstants category for values.
    protected long updated; // The date file was last modified on the original device, before it was uploaded to the MIP Account.
                            // measured in milliseconds since 1970
    protected long versionid;

    protected String path;    // The path property returned from MIP, with the file name stripped off
    
    //protected boolean isEncrypted;

    public CloudFile(String inputLink, 
                     String inputTitle, 
                     long inputSize, 
                     boolean deleted, 
                     long inputUpdated,
                     long versionID,
                     String inputPath,
                     String inputMimeType)
    {
        this.link = inputLink;
        this.setTitle(inputTitle);
        this.size = inputSize;
        this.markedForDelete = deleted;
        this.iconResourceId = R.drawable.file_blank;
        this.mimeType = null;  // generated when requested.
        this.category = FileUtils.CATEGORY_UNKNOWN;  // generated when requested.
        this.updated = inputUpdated;
        this.versionid = versionID;
        this.path = inputPath;
        this.mimeType = inputMimeType;
    }
   

    public String getLink()
    {
        return this.link;
    }
    public long getSize()
    {
        return this.size;
    }    
    public String getTitle()
    {
        return this.title;
    }
    
    public String getName()
    {
        return getTitle();
    }
    public boolean isMarkedForDelete()
    {
        return this.markedForDelete;
    }
    public long getUpdated()
    {
        return this.updated;
    }
    
    public void setUpdated(long updated)
    {
         this.updated = updated;
    }
    
    public long getVersionId()
    {
        return this.versionid;
    }
    /**
     * Returns the resource id for the image used for this object's icon.
     * @return an int for the resource id for the image used for this object's icon
     */
    public int getIconId()
    {
        return this.iconResourceId;
    }
    
    /**
     * Returns the file category based on the filename extension
     */
    public int getCategory()
    {
        if (this.category == FileUtils.CATEGORY_UNKNOWN)
            this.category = FileUtils.getCategory(this.mimeType);
        return this.category;
    }

    /**
     *     Returns the mime type of this file based on the filename extension. 
     */
    public String getMimeType()
    {        
        return this.mimeType;
    }
    
    public String getPath()
    {
        return this.path;
    }


    public void setTitle(String title) {
        this.title = title;
    }
}
