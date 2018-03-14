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

import android.os.Bundle;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.PhotoListAdapter;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.utils.LogUtil;

public class PhotoSearchSlideShowActivity extends PhotoSlideShowActivity
{
    private int folderPosition = -1;

    public void onCreate(Bundle savedInstanceState)
    {
        Bundle extras  = getIntent().getExtras();
        if(extras != null)
        {
            this.folderPosition = extras.getInt("folderPosition");
            LogUtil.debug(this, "folderPosition: " + this.folderPosition);
        }
        
        super.onCreate(savedInstanceState);
    }


    // Called by context menu code.
    @Override
    protected void removeItem(int position, final MozyFile cloudFile)
    {

        // Indicate to any calling screens that the underlying cache data has changed
        setResult(SecuredActivity.RESULT_CODE_NEED_REFRESH);

        showDialog(DIALOG_LOADING_ID);
        // Remove the file from the server
        int status = ServerAPI.getInstance(getApplicationContext()).deleteFile((CloudFile) cloudFile);
        
        if(errDialog != null)
            errDialog.dismiss();
        
        if(status == ServerAPI.RESULT_OK)
        {
         // translate to header position        
            int headerPos = ((PhotoListAdapter) this.photoAdapter).getHeaderCountForPosition(this.folderPosition);
            //int photoIndex = ((PhotoListAdapter) this.photoAdapter).getFirstPhotoIndexinListForFolder(headerPos);
            int totalTitleHeaderCount = headerPos + 1;
            
            PhotoListAdapter.itemList.remove(position);  

            //decrement one photo
           
            PhotoListAdapter.photoCountInDirList.set(headerPos, PhotoListAdapter.photoCountInDirList.get(headerPos) - 1);
            
            PhotoListAdapter.photoList.remove(position - totalTitleHeaderCount);
            
            this.currentPosition = Math.max(0, PhotoSearchSlideShowActivity.this.currentPosition - 1);
            this.refresh(true);
        }
        else
        {
            errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }

       
    }

    /**
     * @param listener
     * @param thumbListener
     */
    @Override
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener,
            ListAdapter.ThumbnailAvailabilityListener thumbListener) {
        PhotoListAdapter photoAdapter = new PhotoListAdapter(getApplicationContext(),
                R.layout.photo_full_screen,
                this.containerLink,
                this.bDeviceEncrypted,
                this.searchText,
                "",
                false,            // directoriesOnly
                true,            // photosOnly
                recurse,
                false,            // refresh
                listener,
                ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW,
                thumbListener,
                this.getRootDeviceId(),
                this.getRootDeviceTitle(),
                this.folderPosition);
        return photoAdapter;
    }
}
