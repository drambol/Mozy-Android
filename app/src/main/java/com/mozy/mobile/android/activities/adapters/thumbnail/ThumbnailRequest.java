package com.mozy.mobile.android.activities.adapters.thumbnail;

/**
 * Request class used for enqueueing thumbnail downloads.
 */
public class ThumbnailRequest
{
    /**
     * The path of the folder
     */
    private String mPath;

    /**
     * The index within the folder
     */
    private int mIndex;

    /**
     * The cache key for the bitmap of this request
     */
    private ThumbnailKey mCacheKey;

    /**
     * Download params
     */
    private ThumbnailParams mParams;

    /**
     * Creates a download request for the given folder and index
     *
     * @param path The folder of the item to be downloaded
     * @param index The index within the folder
     */
    public ThumbnailRequest(String path, String name, long size, int index, ThumbnailParams params)
    {
        mPath = path;
        mParams = params;
        mIndex = index;
        mCacheKey = new ThumbnailKey(path, name, size, index, params.mWidth, params.mHeight);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object)
    {
        ThumbnailRequest request = (ThumbnailRequest) object;
        if(mCacheKey != null && mCacheKey.equals(request.getCacheKey()))
        {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return getCacheKey().hashCode();
    }

    /**
     * Returns the thumbnail params for this download request
     *
     * @return Params
     */
    public ThumbnailParams getParams()
    {
        return mParams;
    }

    /**
     * Returns the path of the folder of the request item
     *
     * @return The folder
     */
    public String getPath()
    {
        return mPath;
    }

    /**
     * Returns the index within the folder of the item
     *
     * @return The index
     */
    public int getIndex()
    {
        return mIndex;
    }

    /**
     * Returns the cache key to be used for the downloaded bitmap
     *
     * @return The key
     */
    public ThumbnailKey getCacheKey()
    {
        return mCacheKey;
    }
}
