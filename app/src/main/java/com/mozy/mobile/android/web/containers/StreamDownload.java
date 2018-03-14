package com.mozy.mobile.android.web.containers;

import java.io.InputStream;

import com.mozy.mobile.android.activities.ErrorCodes;

public class StreamDownload extends Download {
    public InputStream stream;
    
    public StreamDownload()
    {
        this.errorCode = ErrorCodes.NO_ERROR;
    }
}
