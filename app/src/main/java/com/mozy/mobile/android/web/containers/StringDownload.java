package com.mozy.mobile.android.web.containers;

import com.mozy.mobile.android.activities.ErrorCodes;

public class StringDownload extends Download {
    public String string = null;
    
    public StringDownload()
    {
        this.errorCode = ErrorCodes.NO_ERROR;
    }
}
