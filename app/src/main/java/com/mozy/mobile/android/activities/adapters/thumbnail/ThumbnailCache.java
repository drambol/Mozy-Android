package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Map.Entry;

import android.graphics.Bitmap;

import com.mozy.mobile.android.utils.LogUtil;

/**
 * Represents a cache of bitmaps as requested by the activities in the
 * application. The cache key is based on folder path, index of the file
 * within the folder, and the size of the bitmap.
 *
 */
public class ThumbnailCache
{
    private static final String TAG = ThumbnailCache.class.getSimpleName();

    /**
     * Maximum size of cache
     */
    private int mMaxSize;

    /**
     * For converting bytes to kilobytes
     */
    private static final double BYTES_TO_KB_FACTOR = 1.0 / 1024.0;

    /**
     * The cache data structure
     */
    private LinkedHashMap<ThumbnailKey, ThumbnailBitmap> mCache;

    /**
     * Enforces cache limits on put actions
     */
    private CacheStrategyEnforcer mEnforcer;

    /**
     * Total max size of the smallest thumbnail class
     */
    private static final int MAX_TOTAL_SIZE_KB_MINI = 2000;

    /**
     * Total max size of medium thumbnail class
     */
    private static final int MAX_TOTAL_SIZE_KB_MEDIUM = 5000;

    /**
     * Total max size of large thumbnail class
     */
    private static final int MAX_TOTAL_SIZE_KB_LARGE = 12000;

    /**
     * Hashtable used for tracking usage of cache items, i.e. we keep track
     * of which are the least recently used items.
     */
    private Hashtable<ThumbnailKey, Date> mUsageTracker;
    
    public Hashtable<Integer, Double> mThumbnailType;
    
    public static final int TYPE_MINI = 0;
    public static final int TYPE_MEDIUM = 1;
    public static final int TYPE_LARGE = 2;
    
    private static final int HASH_TABLE_THUMBNAIL_TYPE_SIZE = 3;

    /**
     * Creates a thumbnail cache
     *
     * @param maxSize The maximum size of the cache
     */
    @SuppressWarnings("serial")
    public ThumbnailCache(int maxSize)
    {
        mMaxSize = maxSize;
        mEnforcer = new CacheStrategyEnforcer(MAX_TOTAL_SIZE_KB_MINI, MAX_TOTAL_SIZE_KB_MEDIUM, MAX_TOTAL_SIZE_KB_LARGE);
        mUsageTracker = new Hashtable<ThumbnailKey, Date>();
        mThumbnailType = new Hashtable<Integer,Double>(HASH_TABLE_THUMBNAIL_TYPE_SIZE);
        mCache = new LinkedHashMap<ThumbnailKey, ThumbnailBitmap>(mMaxSize, 0.75f, true)
        {
            @Override
            public ThumbnailBitmap put(ThumbnailKey key, ThumbnailBitmap value)
            {
                mUsageTracker.put(key, new Date());
                return super.put(key, value);
            }

            @Override
            public ThumbnailBitmap get(Object key)
            {
                mUsageTracker.put((ThumbnailKey) key, new Date());
                return super.get(key);
            }

            @Override
            public ThumbnailBitmap remove(Object key)
            {
                mUsageTracker.remove(key);
                return super.remove(key);
            }

            @Override
            protected boolean removeEldestEntry(Entry<ThumbnailKey, ThumbnailBitmap> eldest)
            {
                return false;
            }
        };
    }

    /**
     * Clear all elements in the cache
     */
    public synchronized void clear()
    {
        LogUtil.debug(TAG, "[CLEARING CACHE] " + mCache.size() + " items will be removed");
        mCache.clear();
    }
    

    /**
     * Clear matching elements
     *
     * @param path Folder path
     * @param width Width of thumbnail
     * @param height Height of thumbnail
     */
    public synchronized void clear(String path, int width, int height)
    {
        LogUtil.debug(TAG, "[CLEARING CACHE (PARTIAL)] Path: " + path);

        Vector<ThumbnailKey> keys = new Vector<ThumbnailKey>();
        for(ThumbnailKey key : mCache.keySet())
        {
            if(key.matches(path, width, height))
            {
                keys.add(key);
            }
        }
        LogUtil.debug(TAG, "Cleared " + keys.size() + " items");

        for(ThumbnailKey key : keys)
        {
            mCache.remove(key);
        }
    }
    

    /**
     * Puts key and value into cache
     *
     * @param key The key
     * @param value The value
     */
    public synchronized void put(ThumbnailKey key, ThumbnailBitmap value)
    {
        LogUtil.debug(TAG, "[CACHING] (" + key.getThumbnailWidth() + "," + key.getThumbnailHeight() + ", " + value.kbSize() + " KB) " + key.getKey());

        mCache.put(key, value);

        mEnforcer.makeRoomFor(key, value);
        mEnforcer.enforceMaxItemCount();

        LogUtil.debug(TAG, "[CACHE SIZE] " + getTotalBitmapSize() * BYTES_TO_KB_FACTOR + " KB (" + mCache.size() + " objects)");
    }

    /**
     * Gets bitmap from cache, if available. If the cache is not hit a
     * download request will be enqueued IF we are not scrolling.
     *
     * @param path Folder
     * @param index Index in file list
     * @return Bitmap if available, otherwise null
     */
    public synchronized ThumbnailBitmap get(String path, String name, long size, int index, int width, int height)
    {
        ThumbnailKey key = new ThumbnailKey(path, name, size, index, width, height);
        return mCache.get(key);
    }

    /**
     * Returns total size of cached bitmaps in bytes
     *
     * @return Total bitmaps size (bytes)
     */
    private synchronized long getTotalBitmapSize()
    {
        long totalBytes = 0;
        for(ThumbnailBitmap value : mCache.values())
        {
            Bitmap bitmap = value.getBitmap();
            totalBytes += bitmap.getWidth() * bitmap.getHeight() * 4;
        }
        return totalBytes;
    }

    /**
     * Enforces cache limits per thumbnail class, i.e. ensures we do not have
     * too large amounts of any specific kind, like having the entire cache
     * filled with large fullscreen thumbnails.
     */
    private class CacheStrategyEnforcer
    {
        /**
         * Array of max sizes per thumbnail class
         */
        private int[] mMaxTotalSize;

        public CacheStrategyEnforcer(int maxMini, int maxMedium, int maxLarge)
        {
            mMaxTotalSize = new int[] { maxMini, maxMedium, maxLarge };
        }

        /**
         * If necessary, removes old items from the cache to make room for
         * new items. Works per thumbnail type, i.e. if we are inserting a
         * fullscreen thumbnail, items of that type and no other, are removed.
         *
         * @param incomingKey The key of the incoming thumbnail
         * @param incomingBitmap The ThumbnailBitmap object that will be
         * inserted (not by this method).
         */
        public void makeRoomFor(ThumbnailKey incomingKey, ThumbnailBitmap incomingBitmap)
        {
            
            int height = incomingKey.getThumbnailHeight();
            int width = incomingKey.getThumbnailWidth();
            
            int type = getType(height, width);

            Vector<ThumbnailKey> keys = new Vector<ThumbnailKey>();
            int totalSize = 0;

            synchronized(mCache)
            {
                for(Entry<ThumbnailKey, ThumbnailBitmap> entry : mCache.entrySet())
                {
                    ThumbnailKey key = entry.getKey();
                    ThumbnailBitmap bitmap = entry.getValue();

                    if(getType(key.getThumbnailHeight(), key.getThumbnailWidth()) == type)
                    {
                        totalSize += bitmap.kbSize();

                        boolean inserted = false;
                        for(int k = 0; k < keys.size(); k++)
                        {
                          //if(key.getTouchDate().before(keys.elementAt(k).getTouchDate()))
                            if(before(key, keys.elementAt(k)))
                            {
                                keys.insertElementAt(key, k);
                                inserted = true;
                                break;
                            }
                        }
                        if(!inserted)
                        {
                            keys.add(key);
                        }
                    }
                }

                LogUtil.debug(TAG, "makeRoomFor(" + typeToString(type) + ": " + Math.round(incomingBitmap.kbSize()) +
                        "): Currently " + keys.size() + ", " + totalSize + " KB");

                double excess = totalSize - mMaxTotalSize[type];
                if(excess < 0)
                {
                    return;
                }
                else
                {
                    for(int k = 0; k < keys.size(); k++)
                    {
                        ThumbnailKey key = keys.elementAt(k);
                        double sz = mCache.get(key).kbSize();
                        
                        if(mCache.get(key) != null && mCache.get(key).getBitmap() != null)
                        {
                            mCache.get(key).setBitmap(null);
                            mCache.put(key, null); // explicitly null the bitmap
                        }
                        
                        mCache.remove(key);
                        excess -= sz;

                        LogUtil.debug(TAG, "makeRoomFor(): Removing " + sz + ", Left: " + (excess + sz) + " -> " + excess);

                        if(excess < 0)
                        {
                            break;
                        }
                    }
                    
                    System.gc();
                }
            }
        }
        
        
        /**
         * Get the type (thumbnail class) for a thumbnail (mini, medium..)
         *
         * @param bitmap The ThumbnailBitmap
         * @return The type
         */
        private int getType(int height, int width)
        {  
            double dimension = width* height;
            
            if(dimension <= mThumbnailType.get(TYPE_MINI))
            {
                return TYPE_MINI;
            }
            else if(mThumbnailType.get(TYPE_MEDIUM) != null && 
                    dimension <= mThumbnailType.get(TYPE_MEDIUM))
            {
                return TYPE_MEDIUM;
            }
            else
            {
                return TYPE_LARGE;
            }
        }
 

        /**
         * Get the display string for a thumbnail class type
         *
         * @param type The type
         * @return The string
         */
        private String typeToString(int type)
        {
            switch(type)
            {
            case TYPE_MINI: return "MINI";
            case TYPE_MEDIUM: return "MEDIUM";
            case TYPE_LARGE: return "LARGE";
            }
            return "-";
        }

        /**
         * Checks if the first key was used earlier than the second one.
         *
         * @param key1 Key one
         * @param key2 Key two
         * @return true if key one was used earlier than key two
         */
        private boolean before(ThumbnailKey key1, ThumbnailKey key2)
        {
            Date date1 = mUsageTracker.get(key1);
            Date date2 = mUsageTracker.get(key2);
            if(date1 == null || date2 == null)
            {
                return false;
            }
            else
            {
                return date1.before(date2);
            }
        }

        /**
         * Removes any items needed to shrink the cache down to mMaxSize.
         * Items are removed in least-recently-used order.
         */
        private void enforceMaxItemCount()
        {
            if(mCache.size() > mMaxSize)
            {
                LogUtil.debug(TAG, "makeRoomFor(): Cache has too many items (" + mCache.size() + "), removing last touched item");
            }
            else
            {
                return;
            }

            synchronized(mCache)
            {
                Vector<ThumbnailKey> keys = new Vector<ThumbnailKey>();

                for(Entry<ThumbnailKey, ThumbnailBitmap> entry : mCache.entrySet())
                {
                    ThumbnailKey key = entry.getKey();

                    boolean inserted = false;
                    for(int k = 0; k < keys.size(); k++)
                    {
                        if(before(key, keys.elementAt(k)))
                        {
                            keys.insertElementAt(key, k);
                            inserted = true;
                            break;
                        }
                    }
                    if(!inserted)
                    {
                        keys.add(key);
                    }
                }

                for(int k = 0; k < keys.size(); k++)
                {
                    if(mCache.size() > mMaxSize)
                    {
                        ThumbnailKey key = keys.elementAt(k);

                        LogUtil.debug(TAG, "enforceMaxItemCount(): Removing item with touch date " + mUsageTracker.get(key));
                        mCache.remove(key);
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
    }

    public double getThumbnailCacheDimOfType(int type) {
        if(this.mThumbnailType != null && this.mThumbnailType.get(type) != null)
        {
            double dim = this.mThumbnailType.get(type);
            return dim;
        }
        return -1;
    }
    
    public Object setThumbnailCacheOfType(int type, int width, int height) {
        if(this.mThumbnailType != null)
        {
            double dim = width * height;
            return this.mThumbnailType.put(type, dim);
        }
        return null;
    }
}
