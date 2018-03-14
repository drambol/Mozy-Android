package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.util.Date;

/**
 * Key class for the thumbnail cache
 */
public class ThumbnailKey
{
    /**
     * The path of the folder where the photo is located
     */
    private String mPath;

    /**
     * The requested width of the thumbnail
     */
    private int mWidth;

    /**
     * The requested height of the thumbnail
     */
    private int mHeight;

    /**
     * The actual key, in this case a string
     */
    private String mKey;

    /**
     * The created date for this key
     */
    private Date mCacheDate;

    /**
     * The filename
     */
    private String mName;

    /**
     * Size in bytes of the original file
     */
    private long mSize;

    /**
     * Creates a thumbnail key
     */
    public ThumbnailKey(String path, String name, long bytesize, int index, int width, int height)
    {
        mPath = path;
        mName = name;
        mSize = bytesize;
        mWidth = width;
        mHeight = height;
        mCacheDate = new Date();

        mKey = mPath + "(" + mName + "-" + mSize + ")" + mWidth + "x" + mHeight;
    }

    /**
     * Gets the key to use in a cache
     *
     * @return The key (a string)
     */
    public String getKey()
    {
        return mKey;
    }

    /**
     * Checks of this key matches the given parameters.
     *
     * @param path Folder path
     * @param width Width of thumbnail
     * @param height Height of thumbnail
     * @return true if the key matches
     */
    public boolean matches(String path, int width, int height)
    {
        if(mPath.equals(path) && mWidth == width && mHeight == height)
        {
            return true;
        }
        return false;
    }

    /**
     * Gets the width of the associated bitmap image
     *
     * @return Width of thumbnail
     */
    public int getThumbnailWidth()
    {
        return mWidth;
    }

    /**
     * Gets the height of the associated bitmap image
     *
     * @return Height of thumbnail
     */
    public int getThumbnailHeight()
    {
        return mHeight;
    }

    /**
     * Returns the date of creation for this key. Used when doing
     * calculations based on age of chaced item.
     *
     * @return The created date
     */
    public Date getCacheDate()
    {
        return mCacheDate;
    }

    @Override
    public int hashCode()
    {
        return mKey.hashCode();
    }

    @Override
    public boolean equals(Object key)
    {
        if(mKey != null)
            return mKey.equals(((ThumbnailKey) key).getKey());
        else
            return false;
    }
}
