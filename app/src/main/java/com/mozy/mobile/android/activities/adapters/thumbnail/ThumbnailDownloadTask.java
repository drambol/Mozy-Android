package com.mozy.mobile.android.activities.adapters.thumbnail;

import android.content.Context;
import android.graphics.Bitmap;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.web.MipAPI.ThumbnailConnection;
import com.mozy.mobile.android.web.containers.ThumbnailDownload;

public class ThumbnailDownloadTask
{
    private final Context mContext;
    private final String mFileLink;
    private final int mWidth;
    private final int mHeight;
    private boolean mAborted;
    private long mLastModified;
    private long mVersionId;
    private ThumbnailConnection mConnection;

    public ThumbnailDownloadTask(Context context, String fileLink, int width, int height, int position, long lastModified, long version)
    {
        mContext = context;
        mFileLink = fileLink;
        mWidth = width;
        mHeight = height;
        mConnection = null;
        mAborted = false;
        mLastModified = lastModified;
        mVersionId = version;
    }

    public ThumbnailDownload doThumbnailDownload()
    {
        ThumbnailDownload download = doThumbnailFetch();
        return  download;
    }

    private ThumbnailDownload doThumbnailFetch()
    {
        ThumbnailDownload download = new ThumbnailDownload();
        synchronized(this)
        {
            if(mAborted)
            {
                download.errorCode = ErrorCodes.ERROR_HTTP_UNKNOWN;
                download.setBitmap(null);

                return download;
            }
            mConnection = ServerAPI.getInstance(mContext).getThumbnail(mFileLink, mWidth, mHeight, mLastModified, mVersionId);
        }

        Bitmap bitmap = null;
        if(mConnection != null)
        {
            bitmap = mConnection.execute();
        }

        download.setBitmap(bitmap);
        download.errorCode = mConnection.getErrorCode();

        return download;
    }

    public synchronized void abort()
    {
        mAborted = true;

        if(mConnection != null)
        {
            mConnection.close();
        }
    }
}
