/*  Copyright Tactel AB 2009
 *
 *  All copyrights in this software are created and owned by Tactel AB.
 *  This software, or related intellectual property, may under no
 *  circumstances be used, distributed or modified without written
 *  authorization from Tactel AB.
 *  This copyright notice may not be removed or modified and  shall be
 *  displayed in all materials that include the software or portions of such.
 */

package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.RecentFilesListAdapter;


public class RecentPhotoSlideShowActivity extends PhotoSlideShowActivity
{
    /**
     * @param listener
     * @param thumbListener
     */
    @Override
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener,
            ListAdapter.ThumbnailAvailabilityListener thumbListener) {
        RecentFilesListAdapter photoAdapter = new RecentFilesListAdapter(getApplicationContext(),
                R.layout.photo_full_screen,
                this.containerLink,
                this.bDeviceEncrypted,
                false,            // directoriesOnly
                true,            // photosOnly
                recurse,
                false,            // refresh
                listener,
                RecentFilesListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW,
                thumbListener,
                this.getRootDeviceId(),
                this.getRootDeviceTitle());
        
        return photoAdapter;
    }
}
