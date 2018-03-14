package com.mozy.mobile.android.activities.adapters.thumbnail;

import android.content.Context;


/**
 * Class that manages thumbnail process and caching. Written as a singleton
 * to allow for easy cross-activity caching.
 *
 * The "current" activity will register itself as a ThumbnailManagerListener
 * (replacing any previously registered listener) and receive necessary updates.
 */
public class ThumbnailManagerForHiResFiles extends ThumbnailManager
{

    /**
     * The singleton instance
     */
    private static ThumbnailManagerForHiResFiles sInstance;

    /**
     * Get the singleton instance
     *
     * @param context The context
     * @return Singleton instance of this class
     */
    public static ThumbnailManagerForHiResFiles getInstance(Context context, ThumbnailDownloadService service)
    {
        if(sInstance == null)
        {
            sInstance = new ThumbnailManagerForHiResFiles(context, service);
        }
        return sInstance;
    }
    
    /**
     * Creates a manager with a cache and a process worker thread
     *
     * @param context The context to use
     */
    
    private ThumbnailManagerForHiResFiles(Context context, ThumbnailDownloadService service)
    {
        super(context, service);
    }
}
    

