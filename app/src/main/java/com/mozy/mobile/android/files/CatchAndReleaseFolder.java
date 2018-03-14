package com.mozy.mobile.android.files;

import com.mozy.mobile.android.files.Directory;

public class CatchAndReleaseFolder extends Directory 
{
    public CatchAndReleaseFolder(String link, String title, long size)
    {
        super(link, title, size, false, 0, 0, true, null);
    }
}
