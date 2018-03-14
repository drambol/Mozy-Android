package com.mozy.mobile.android.files;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.CloudFile;

public class Directory extends CloudFile 
{
    boolean filesAreDeletable;
    
    public Directory(String link, String title, boolean deleted, String path)
    {
        super(link, title, 0, deleted, 0, 0, path, null);            // A directory has zero size and no mime-type
        this.filesAreDeletable = false;        
    }
    
    public Directory(String link, String title, long size, boolean deleted, long updated, long version, String path)
    {
        super(link, title, size, deleted, updated, version, path, null);        // Classes derived from a directory, for example a device, may have a size    
        this.iconResourceId = R.drawable.folder;
        this.filesAreDeletable = false;        
    }
    
    public Directory(String link, String title, long size, boolean deleted, long updated, long version, boolean areFilesDeletable, String path)
    {
        super(link, title, size, deleted, updated, version, path, null);        // Classes derived from a directory, for example a device, may have a size    
        this.iconResourceId = R.drawable.folder;
        this.filesAreDeletable = areFilesDeletable;        
    }    
    
    public boolean areFilesDeletable()
    {
        return this.filesAreDeletable;
    }
}
