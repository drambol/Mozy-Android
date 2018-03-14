package com.mozy.mobile.android.activities.adapters.thumbnail;

import android.graphics.Bitmap;

/**
 * Value class for the thumbnail cache. Associates a bitmap with a
 * last-modified date.
 */
public class ThumbnailBitmap
{
    /**
     * The bitmap
     */
    private Bitmap mBitmap;

    /**
     * The last-modified date of the bitmap
     */
    private long mLastModified;

    /**
     * Value class for the thumbnail cache
     *
     * @param bitmap The bitmap
     * @param lastModified The last-modified date
     */
    public ThumbnailBitmap(Bitmap bitmap, long lastModified)
    {
        mBitmap = bitmap;
        mLastModified = lastModified;
    }

    /**
     * Get the bitmap
     *
     * @return The bitmap
     */
    public Bitmap getBitmap()
    {
        return mBitmap;
    }
    
    /**
     * Get the bitmap
     * @param  
     *
     * @return The bitmap
     */
    public void setBitmap(Bitmap bitmap)
    {
        mBitmap = bitmap;
    }


    /**
     * Get the last-modified date
     *
     * @return The date
     */
    public long getLastModified()
    {
        return mLastModified;
    }

    public double kbSize()
    {
        if(mBitmap != null)
        {
            return (mBitmap.getWidth()* mBitmap.getHeight() * 4) / 1024.0;
        }
        return 0;
    }
}
