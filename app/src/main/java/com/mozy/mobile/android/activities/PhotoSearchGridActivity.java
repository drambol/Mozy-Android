package com.mozy.mobile.android.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.ListAdapter;
import com.mozy.mobile.android.activities.adapters.PhotoListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.MozyFile;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.LogUtil;


public class PhotoSearchGridActivity extends DirPhotoGridActivity {

    private int folderPosition = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            this.folderPosition = extras.getInt("folderPosition");
            LogUtil.debug(this, "folderPosition: " + this.folderPosition);
        } 
        
        super.onCreate(savedInstanceState);
    }
    
    /**
     * 
     */
    @Override
    public void setTitleForPhotoGrid() {
        if (title != null)
        {
            String syncTitle = getResources().getString(R.string.sync_title);
            if(rootDeviceTitle.equalsIgnoreCase(syncTitle))
            {
                if(title.equalsIgnoreCase(syncTitle) == false)
                {
                    title = syncTitle +  title;
                }
            }
            setBarTitle(title);
        }
    }
    
    /**
     * @param listener
     */
    @Override
    protected ListAdapter getAdapter(ListAdapter.ListAdapterDataListener listener, boolean flushCache) {
        ListAdapter photoAdapter = new PhotoListAdapter(getApplicationContext(),
                R.id.grid_directory_layout,
                this.containerLink,
                this.bDeviceEncrypted,
                this.searchText,
                "",
                true,        // directoriesOnly
                true,         // photosOnly
                recurse,
                false,        // refresh
                listener,
                ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID,
                null,
                this.getRootDeviceId(),
                this.getRootDeviceTitle(),
                this.folderPosition);
        
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
        Intent intent = new Intent(PhotoSearchGridActivity.this, PhotoSearchSlideShowActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("position", photoPosition);
        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.trim().length() > 0);
        intent.putExtra("title", cloudFile.getTitle());
        intent.putExtra("canFilesBeDeleted", PhotoSearchGridActivity.this.canFilesBeDeleted);
        intent.putExtra("folderPosition", this.folderPosition);

        removeDialog(DIALOG_LOADING_ID);

        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    
    @Override
    protected void goToFileList()
    {
     // Switch current container navigation to listview mode.
        Intent intent = new Intent(PhotoSearchGridActivity.this, PhotoSearchDirFileListActivity.class);
        intent.putExtra("containerLink", containerLink);
        intent.putExtra("title", title);
        intent.putExtra("searchText", searchText);
        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
        intent.putExtra("canFilesBeDeleted", canFilesBeDeleted);
        intent.putExtra("isPhotoDirGridEnabled", false); // this how we got to the grid view so is enabled
        intent.putExtra("deviceId", rootDeviceId);
        intent.putExtra("deviceTitle", rootDeviceTitle);
        intent.putExtra("deviceType", bDeviceEncrypted);
        intent.putExtra("platform", platform);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    
    @Override
    public void onBackPressed() {
       
        PhotoSearchGridActivity.this.finish();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_FILES, 0, getResources().getString(R.string.menu_files)).setIcon(R.drawable.allfiles);
        menu.add(0, MENU_HOME, 1, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        menu.add(0, MENU_DIR_FILE_LIST, 2, getResources().getString(R.string.menu_files_view)).setIcon(R.drawable.all_files);
        menu.add(0, MENU_HELP, 3, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 4, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);

        return true;
    }

    @Override
    protected void viewPhoto(int position, MozyFile cloudFile)
    {
        showDialog(DIALOG_LOADING_ID);
        
        // Run a background task to do this as we need to go to the SQLite database and all accesses to the
        // database should be done in the background.
        CalculatePhotoPositionTask calculatePhotoPositionTask = new CalculatePhotoPositionTask(position, (CloudFile)cloudFile, folderPosition);
        calculatePhotoPositionTask.execute();
    }

    // Called by context menu code.
    @Override
    protected void removeItem(int position, final MozyFile cloudFile)
    {
        // Indicate to any calling screens that the underlying cache data has changed
        setResult(SecuredActivity.RESULT_CODE_NEED_REFRESH);

        // Remove the file from the server
        int status = ServerAPI.getInstance(getApplicationContext()).deleteFile((CloudFile) cloudFile);
        
        if(errDialog != null)
            errDialog.dismiss();
        
        if(status == ServerAPI.RESULT_OK)
        {
         // translate to header position        
            int headerPos = ((PhotoListAdapter) this.photoAdapter).getHeaderCountForPosition(this.folderPosition);
            int photoIndex = ((PhotoListAdapter) this.photoAdapter).getFirstPhotoIndexinListForFolder(headerPos);
            int totalTitleHeaderCount = headerPos + 1;
            
            PhotoListAdapter.itemList.remove(photoIndex + position);

            //decrement one photo
           
            PhotoListAdapter.photoCountInDirList.set(headerPos, PhotoListAdapter.photoCountInDirList.get(headerPos) - 1);
            
            PhotoListAdapter.photoList.remove(photoIndex + position - totalTitleHeaderCount);
            
            this.refresh(true);
        }
        else
        {
            errDialog = createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.delete_fail_body,  R.string.ok_button_text);
            errDialog.show();
        }
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
                        Intent intent = new Intent(PhotoSearchGridActivity.this, PhotoSearchGridActivity.class);
                        intent.putExtra("containerLink", cloudFile.getLink());
                        intent.putExtra("searchText", searchText);
                        intent.putExtra("recurse", searchText != null && searchText.length() > 0);
                        intent.putExtra("title", cloudFile.getTitle());
                        intent.putExtra("canFilesBeDeleted", PhotoSearchGridActivity.this.canFilesBeDeleted);
                        intent.putExtra("isPhotoDirGridEnabled", false); // this how we got to the grid view so is enabled
                        intent.putExtra("deviceId", PhotoSearchGridActivity.this.rootDeviceId);
                        intent.putExtra("deviceTitle", PhotoSearchGridActivity.this.rootDeviceTitle);
                        intent.putExtra("deviceType", PhotoSearchGridActivity.this.bDeviceEncrypted);
                        intent.putExtra("platform", PhotoSearchGridActivity.this.platform);
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

    
    // This task takes the current position in the list and calculates a new position based on that, that represents
    // the position in a list of only photos.
    private class CalculatePhotoPositionTask extends AsyncTask<Void, Void, Integer>
    {
        private int listPosition;
        CloudFile cloudFile;
        private int folderPosition;

        public CalculatePhotoPositionTask(int position, CloudFile inputFile, int folderPosition)
        {
            this.listPosition = position;
            this.cloudFile = inputFile;
            this.folderPosition = folderPosition;
        }


        @Override
        protected Integer doInBackground(Void... params)
        {
            return Integer.valueOf(((PhotoListAdapter) PhotoSearchGridActivity.this.photoAdapter).getPhotoPosition(this.listPosition, this.folderPosition));
        }


        @Override
        protected void onPostExecute(Integer photosIndex)
        {
            goToPhotoSlideShow(photosIndex.intValue(), this.cloudFile);
        }

    } // class CalculatePhotoPosition
}
