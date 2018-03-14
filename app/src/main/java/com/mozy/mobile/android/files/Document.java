package com.mozy.mobile.android.files;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.CloudFile;

public class Document extends CloudFile 
{
    public Document(String link, String title, long size, boolean deleted, long updated, long version, String path, String mimeType, int category)
    {
        super(link, title, size, deleted, updated, version, path, mimeType);
        this.category = category;
        this.iconResourceId = R.drawable.file_blank;
    }
}
