package com.mozy.mobile.android.activities.adapters.thumbnail;

/**
 * Wraps all folder and item data needed for download in one object
 */
public class ThumbnailParams
{
    public final int mWidth;
    public final int mHeight;

    public final String mFolder;
    public final String mSearchText;

    public final boolean mDirsOnly;
    public final boolean mPhotosOnly;
    public final boolean mRecurse;

    public long mLastModified;
    public long mVersionId;

    public ThumbnailParams(int width, int height, String folder, long lastModified,
            long version, String searchText, boolean dirsOnly, boolean photosOnly,
            boolean recurse)
    {
        mWidth = width;
        mHeight = height;

        mFolder = folder;
        mLastModified = lastModified;
        mVersionId = version;

        mSearchText = searchText;
        mDirsOnly = dirsOnly;
        mPhotosOnly = photosOnly;
        mRecurse = recurse;
    }
}
