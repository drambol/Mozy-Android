package com.mozy.mobile.android.activities;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.RecentFilesListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

public class RecentDirPhotoGridActivity extends DirPhotoGridActivity {
    
    /**
     * @param listener
     */
    @Override
    protected ListAdapter getAdapter(
            ListAdapter.ListAdapterDataListener listener, boolean flushCache) {
        ListAdapter photoAdapter = new RecentFilesListAdapter(getApplicationContext(),
                0,
                this.containerLink,
                this.bDeviceEncrypted,
                true,        // directoriesOnly
                true,         // photosOnly
                recurse,
                false,        // refresh
                listener,
                RecentFilesListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID,
                null,
                this.getRootDeviceId(),
                this.getRootDeviceTitle());
        
        return photoAdapter;
    }
    
    @Override
    public OnItemClickListenerClass  getOnItemClickListenerClass()
    {
        return new OnItemClickListenerClass();
    }
    
   
    @Override
    protected void goToPhotoSlideShow(int photoPosition, CloudFile cloudFile)
    {
        // Load the preview activity
        Intent intent = new Intent(RecentDirPhotoGridActivity.this, RecentPhotoSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", RecentDirPhotoGridActivity.this.canFilesBeDeleted);
        intent.putExtra("recurse", false);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    
    @Override
    public void onBackPressed() {
        
        int numdevices = SystemState.getDevicePlusSyncCount();
       if(numdevices == 1 )
       {
           NavigationTabActivity.returnToHomescreen(this);
       }
       else
           finish();
    }

    /**
     * 
     */
    @Override
    protected void goToFileList() {
        Intent intent = new Intent(RecentDirPhotoGridActivity.this, RecentDirFileListActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("title", title);
        intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("recurse", false);
        intent.putExtra("isPhotoDirGridEnabled", true);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    
    protected class OnItemClickListenerClass extends DirPhotoGridActivity.OnItemClickListenerClass
    {
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
        {
            updateAlarm();
            // CloudFile f = (CloudFile)photoAdapter.getItem(position);
            Object listItem = photoAdapter.getItem(position);
            if (listItem instanceof String)
            {
                LogUtil.error(this, "String object received in DirPhotoGrid!!");
            }
            else
            {
                CloudFile cloudFile = (CloudFile)listItem;
                if (cloudFile != null) {
                    if (cloudFile instanceof Directory)
                    {
                        Intent intent = new Intent(RecentDirPhotoGridActivity.this, RecentDirPhotoGridActivity.class);
                        intent.putExtra("containerLink", cloudFile.getLink());
                        intent.putExtra("title", cloudFile.getTitle());
                        intent.putExtra("canFilesBeDeleted", RecentDirPhotoGridActivity.this.canFilesBeDeleted);
                        intent.putExtra("recurse", false);
                        intent.putExtra("deviceId", RecentDirPhotoGridActivity.this.rootDeviceId);
                        intent.putExtra("deviceTitle", RecentDirPhotoGridActivity.this.rootDeviceTitle);
                        intent.putExtra("deviceType", RecentDirPhotoGridActivity.this.bDeviceEncrypted);
                        intent.putExtra("platform", RecentDirPhotoGridActivity.this.platform);
                        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                    }
                    else if (cloudFile instanceof Photo)
                    {
                        viewPhotoFile(cloudFile, position);
                    }
                }
            } // if !(listItem instanceof String)
        }
    }
}
