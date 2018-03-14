package com.mozy.mobile.android.web.containers;

import android.graphics.Bitmap;

public class ThumbnailDownload extends Download 
{
    private Bitmap bitmap;
    
    public Bitmap getBitmap()
    {
        return this.bitmap;
    }
    
    public void setBitmap(Bitmap inputBitmap)
    {
        this.bitmap = inputBitmap;
    }
}
