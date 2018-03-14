package com.mozy.mobile.android.files;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.Directory;

public class Device extends Directory 
{
    protected String id;
    protected long quota;
    protected String platform;  // e.g. 'windows'
    protected String published; 
    protected boolean sync;
    protected boolean encrypted;

    /**
     * 
     * @param inputUpdated
     * @param title
     * @param link
     * @param size
     * @param inputId
     * @param quota
     * @param published
     * @param platform
     */
    public Device(long updated, String title, String link, long size, String inputId, 
            long quota, String published, String platform, boolean sync, boolean encrypted)
    {
        super(link, title, size, false, updated, 0, null);
        
        this.id = inputId;
        this.quota = quota;
        this.published = published;
        this.platform = platform;
        this.sync = sync;
        this.encrypted = encrypted;
        
        this.iconResourceId = R.drawable.computer;        
    }
    
    public String getId()
    {
        return this.id;
    }
    public long getQuota()
    {
        return this.quota;
    }
    public String getPublished()
    {
        return this.published;
    }
    public String getPlatform()
    {
        return this.platform;
    }
    public boolean getSync()
    {
        return this.sync;
    }
    public boolean getEncrypted()
    {
        return this.encrypted;
    }
}
