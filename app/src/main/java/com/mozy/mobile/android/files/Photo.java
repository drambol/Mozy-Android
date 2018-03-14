package com.mozy.mobile.android.files;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.utils.FileUtils;

public class Photo extends CloudFile
{
    public Photo(String link, String title, long size, boolean deleted, long updated, long version, String path, String mimeType)
    {
        super(link, title, size, deleted, updated, version, path, mimeType);
        this.iconResourceId = R.drawable.photo_small;
        this.category = FileUtils.CATEGORY_PHOTOS;
    }
}
