package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.util.Vector;

import android.content.Context;
import android.graphics.Bitmap;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailDownloadService.ThumbnailWorkerListener;
import com.mozy.mobile.android.utils.LogUtil;

/**
 * Class that manages thumbnail download and caching. Written as a singleton
 * to allow for easy cross-activity caching.
 *
 * The "current" activity will register itself as a ThumbnailManagerListener
 * (replacing any previously registered listener) and receive necessary updates.
 */
public class ThumbnailManager
{
    protected static final String TAG = ThumbnailManager.class.getSimpleName();
    
    /**
     * Cache
     */
    protected ThumbnailCache mThumbnailCache;

    /**
     * Maximum size of cache
     */
    protected static final int CACHE_SIZE = 50;

    /**
     * Service for downloading thumbnails
     */
    protected ThumbnailDownloadService mThumbnailDownloadService;

    /**
     * Listeners (download notifications)
     */
    protected Vector<ThumbnailManagerListener> mListeners;

    /**
     * Are we scrolling?
     */
    protected boolean mScrolling;

    /**
     * The singleton instance
     */
    private static ThumbnailManager sInstance;

    /**
     * Get the singleton instance
     *
     * @param context The context
     * @return Singleton instance of this class
     */
    public static ThumbnailManager getInstance(Context context,ThumbnailDownloadService service)
    {
        if(sInstance == null)
        {
            sInstance = new ThumbnailManager(context, service);
        }
        return sInstance;
    }

    /**
     * Creates a manager with a cache and a download worker thread
     *
     * @param context The context to use
     */
    protected ThumbnailManager(Context context,ThumbnailDownloadService service)
    {
        mListeners = new Vector<ThumbnailManagerListener>();
        
        mThumbnailCache = new ThumbnailCache(CACHE_SIZE);
        mThumbnailDownloadService = service;
        mThumbnailDownloadService.addThumbnailWorkerListener(new ThumbnailWorkerListener()
        {
            @Override
            public void onRequestComplete(ThumbnailRequest request, ThumbnailBitmap bitmap, int errorCode)
            {
                if(errorCode == ErrorCodes.ERROR_HTTP_UNKNOWN)
                {
                    //got out of memory error purge the cache
                    mThumbnailCache.clear();
                    System.gc();
                    LogUtil.debug(TAG, "Clearing the Thumbnail Cache");
                }
                else
                {
                    if(bitmap != null)
                    {
                        mThumbnailCache.put(request.getCacheKey(), bitmap);
                        fireNewThumbnail(request.getPath(), request.getIndex());
                    } 
                }
            }
        });
    }

    public void setupThumbnailCacheForActivity(int activityType, int width, int height) {
        
        int mThumbnailCacheType = 0;

        if(ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST == activityType)
        {
            mThumbnailCacheType = ThumbnailCache.TYPE_MINI;
        }
        else if (activityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID)
        {
            mThumbnailCacheType = ThumbnailCache.TYPE_MEDIUM;
        }
        else 
        {
            mThumbnailCacheType = ThumbnailCache.TYPE_LARGE;
        }
        
        if(mThumbnailCache.getThumbnailCacheDimOfType(mThumbnailCacheType) == -1)
        {
            mThumbnailCache.setThumbnailCacheOfType(mThumbnailCacheType, width, height);
        }
         
    }

    /**
     * Get the bitmap for a folder and index within that folder
     *
     * @param path Folder path
     * @param index Index within the item list
     * @return Bitmap if found, or null
     */
    public Bitmap getThumbnail(String path, String name, long size, int index, ThumbnailParams params, Object item)
    {
        ThumbnailBitmap cached = mThumbnailCache.get(path, name, size, index, params.mWidth, params.mHeight);

        if(cached == null || params.mLastModified > cached.getLastModified())
        {
            /*
             * Do not enqueue request if we are scrolling
             */
            if(!mScrolling)
            {
                if(cached != null && params.mLastModified > cached.getLastModified())
                {
                    LogUtil.debug(TAG, "[THUMBNAIL IS STALE] Posting new request... " + path + "(" + index + ")");
                }

                ThumbnailRequest request = new ThumbnailRequest(path, name, size, index, params);

                LogUtil.debug(TAG, "CACHE REQUEST: " + index + ", " + path);

                mThumbnailDownloadService.postRequest(request, item);
            }
        }
        return cached == null ? null : cached.getBitmap();
    }

    public Bitmap getThumbnailNoEnqueue(String path, String name, long size, int index, ThumbnailParams params)
    {
        ThumbnailBitmap cached = mThumbnailCache.get(path, name, size, index, params.mWidth, params.mHeight);
        return cached == null ? null : cached.getBitmap();
    }

    /**
     * Clear the download queue (should be done when switching activities)
     */
    public void clearQueue()
    {
        mThumbnailDownloadService.clearQueue();
    }

    /**
     * Clear the download queue (should be done when switching activities)
     */
    public void clearCache()
    {
        mThumbnailCache.clear();
    }

    /**
     * Clear cache entries matching the given parameters
     *
     * @param path Folder path
     * @param width Thumbnail width
     * @param height Thumbnail height
     */
    public void clearCache(String path, int width, int height)
    {
        mThumbnailCache.clear(path, width, height);
    }

    /**
     * Set scrolling state. This is used for determining if we should
     * enqueue new download requests or wait (for scroll to stop).
     *
     * @param scrolling True enables submitting new requests, false disables
     */
    public void setScrolling(boolean scrolling)
    {
        LogUtil.debug(TAG, "[SCROLLING STATE] " + (scrolling ? "SCROLLING" : "IDLE"));
        mScrolling = scrolling;
    }

    /**
     * Adds a listener that will receive notifications when thumbnails are
     * downloaded
     *
     * @param listener The listener to add
     */
    public void setThumbnailManagerListener(ThumbnailManagerListener listener)
    {
        mListeners.removeAllElements(); // Remove to allow multiple listeners
        mListeners.add(listener);
    }

    /**
     * Notify listeners that a new thumbnail has been downloaded
     *
     * @param path The folder of the thumbnail
     * @param index Index within the folder
     */
    public void fireNewThumbnail(String path, int index)
    {
        for(ThumbnailManagerListener listener : mListeners)
        {
            listener.onThumbnail(path, index);
        }
    }

    /**
     * Interface for subscribers of download updates
     */
    public static interface ThumbnailManagerListener
    {
        public void onThumbnail(String path, int index);
    }
}
