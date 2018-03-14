package com.mozy.mobile.android.activities.adapters;

import android.content.Context;
import android.os.Handler;



public class RecentFilesListAdapter extends ListAdapter
{
    public RecentFilesListAdapter(Context context,
            int resourceId,
            final String parentDirLink,
            final boolean encryptedContainer,
            final boolean directoriesOnly,
            final boolean photosOnly,
            boolean recurse,
            boolean refresh,
            ListAdapterDataListener listener,
            int inputActivityType,
            ThumbnailAvailabilityListener thumbListener,
            String rootDeviceId,
            String rootDeviceTitle)
    {
        
        super(context, resourceId, parentDirLink, encryptedContainer, "", "", 
                directoriesOnly, photosOnly, recurse, refresh, listener, inputActivityType, thumbListener, rootDeviceId, rootDeviceTitle);
    }
    
    
    /**
     * @param context
     * @param searchText
     */
    @Override
    protected void prepareInitialList() {
        
        mTotalCount = ListManager.getInstance().getCount(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
        mDetailsVisible = false;
        mEnabled = true;
        
        
        /*
         * If there is no info on item count get a new item list, else fire
         * data changed event to cause refresh
         */
        if (mTotalCount <= 0)
        {
            ListManager.getInstance().prepareRecentFilesList(mContext, mFolder, mSearchText, this.mRecurse, null, this);
        }
        else
        {
            if(mListAdapterDataListener != null)
            {
                mListAdapterDataListener.onDataRetrieved(this);
            }

            Handler handler = new Handler(mContext.getMainLooper());
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    notifyDataSetChanged();
                }
            });
        }
    }
    
    /**
     * Enable the adapter. Returns true if a call to prepareList() was made.
     *
     * @return true is prepareList() was called, false otherwise.
     */
    @Override
    public synchronized boolean enable()
    {
        if (!mEnabled)
        {
            mEnabled = true;

            // Claim the spot as listener to thumbnail download events
               mThumbnailManager.setThumbnailManagerListener(this);

            int count = ListManager.getInstance().getCount(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
            if (count != mTotalCount)
            {
                notifyDataSetChanged();
            }
            if (count < 0)
            {
                ListManager.getInstance().prepareRecentFilesList(mContext, mFolder, mSearchText, mRecurse, null, this);
                return true;
            }
        }
        notifyDataSetChanged();
        return false;
    }

    @Override
    public void increaseItems()
    {
        String nextIndex = ListManager.getInstance().getNextIndex(mContext, mFolder, mSearchText, mRecurse);
        if (nextIndex != null)
        {
            ListManager.getInstance().prepareRecentFilesList(mContext, mFolder, mSearchText, mRecurse, nextIndex, this);
        }
    }
}
