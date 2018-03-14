package com.mozy.mobile.android.web.containers;

import com.mozy.mobile.android.activities.ErrorCodes;

public class IntDownload extends Download 
{
    
    public int count;
    
    public IntDownload()
    {
        this.count = 0;
        this.errorCode = ErrorCodes.NO_ERROR;
    }
}
