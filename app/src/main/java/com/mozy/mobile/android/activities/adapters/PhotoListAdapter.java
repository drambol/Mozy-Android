package com.mozy.mobile.android.activities.adapters;

import java.util.ArrayList;
import java.util.Vector;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.adapters.ListManager.Listener;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailParams;
import com.mozy.mobile.android.activities.tasks.GetFileListTask;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import android.view.View.OnTouchListener;
import com.mozy.mobile.android.views.SlideView;

public class PhotoListAdapter extends ListAdapter
{
    
    public static ArrayList<Object> itemList = new ArrayList<Object>();
    public static ArrayList<Integer> photoCountInDirList = new ArrayList<Integer>();
    public static ArrayList<Object> photoList = new ArrayList<Object>();
    
    private int folderPosition = -1;  // needed for grid and slide show views to track the folder we are in
    
    private String mNextIndex = null;  // tracks "more items"

    public PhotoListAdapter(Context context,
            int resourceId,
            final String parentDirLink,
            final boolean encryptedContainer,
            String searchText, 
            String searchDirectory, 
            final boolean directoriesOnly,
            final boolean photosOnly,
            final boolean recurse,
            boolean refresh,
            ListAdapterDataListener listener,
            int inputActivityType,
            ThumbnailAvailabilityListener thumbListener,
            String rootDeviceId,
            String rootDeviceTitle,
            int folderPosition)
    {
        super(context, resourceId, parentDirLink, encryptedContainer, searchText, searchDirectory, directoriesOnly,
                photosOnly, recurse,refresh, listener, inputActivityType, thumbListener, rootDeviceId, rootDeviceTitle);
        
        this.folderPosition = folderPosition;
    }
    
    /**
     * @param context
     * @param searchText
     */
    @Override
    protected void prepareInitialList() {
        
        mTotalCount = itemList.size();
        mDetailsVisible = false;
        mEnabled = true;
        
        /*
         * If there is no info on item count get a new item list, else fire
         * data changed event to cause refresh
         */
        if (mTotalCount <= 0)
        {
            this.prepareList(mContext, mFolder, mSearchText, this.mRecurse, null, this);
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
    
   
    @Override
    public int getCount()
    {
        int count = 0;
        if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST))
        {
            count = getCountForPhotoSearchDirFileList();
        } else if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID))
        {
            //use the position passed to adapter to determine the photo count
            count = getCountForPhotoSearchGrid();
        } else if((mActivityType == ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW))
        {
            mTotalCount = photoList.size();
            count = mTotalCount < 0 ? 0 : mTotalCount;
        }
       
        return count;
    }

    /**
     * 
     */
    protected int getCountForPhotoSearchDirFileList() {
        if(photoCountInDirList != null && photoCountInDirList.size() != 0)
            mTotalCount = photoCountInDirList.size() * 2;  // Twice the number of title strings
        else
            mTotalCount = -1;
        
        int count = mTotalCount < 0 ? 0 : mTotalCount;
        if(mNextIndex != null)
        {
            count++;
        }
        
        return count;
    }
    
    protected int getCountForPhotoSearchGrid() {
      
        int count = 0;
        
        if(this.folderPosition != -1)
        {
            // translate to header position
            
            int headerPos = this.getHeaderCountForPosition(this.folderPosition);
            
            count = this.getPhotoCountinFolder(headerPos);
            
            this.mTotalCount = count;  
        }
        
        return count;
    }
    
    
    public int getHeaderCountForPosition(int position)
    {  
        return (position / 2);
    }
   
    @Override
    public Object getItem(int position)
    {
        Object item = null;
        synchronized(this)
        {
            if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST))
            {
                item = getItemForPhotoSearchDirFileList(position);
            }
            else if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID))
            {
                //return photo using position and the reference index passed in adapter
                
                item = getItemForPhotoSearchGrid(position);
            }
            else if((mActivityType == ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW))
            {
                item = photoList.get(position);
            }
        }
        
        return item;
    }

    /**
     * @param position
     * @return
     */
    protected Object getItemForPhotoSearchDirFileList(int position) {
        Object item = null;
        
        // translate to header position
        
        int headerPos = getHeaderCountForPosition(position);
        
        if (position < mTotalCount && itemList != null)
        {
            if((position % 2) == 0)  // evens are titles
            {
                return itemList.get(this.getHeaderIndexinListForFolder(headerPos));  // title row
            }
            else  // photos row
            {
                int photoIndex = getFirstPhotoIndexinListForFolder(headerPos);
                return itemList.get(photoIndex);
            }
        }
        return item;
    }
    
    
    /**
     * @param position
     * @return
     */
    protected Object getItemForPhotoSearchGrid(int position) {
        Object item = null;

        if(this.folderPosition != -1)
        {
            // translate to header position        
            int headerPos = getHeaderCountForPosition(this.folderPosition);
            int photoIndex = getFirstPhotoIndexinListForFolder(headerPos);
            
            if(itemList.size() > photoIndex + position) 
                item = itemList.get(photoIndex + position);
        }
        
        return item;
    }
    
    public int getPhotoCountinFolder(int headerPos)
    {     
        int count = 0;
        
        if(headerPos < photoCountInDirList.size())
            count = photoCountInDirList.get(headerPos);
        
        LogUtil.debug("PhotoListAdapter: getPhotoCountinFolder", "Photo Count for folder " + headerPos + " - " + count);
        return count;
    }
    
    public int getFirstPhotoIndexinListForFolder(int headerPos)
    {
        int index = 0;
        
        if(headerPos == 0) return 1;
        
        for(int i = 0; i < headerPos; i++)
        { 
            index = index + (1 + getPhotoCountinFolder(i)); // title + number of photos
        }
        
        index = index + 1; //index to the next list of photos after the title
        
        LogUtil.debug("PhotoListAdapter:getFirstPhotoIndexinListForFolder", "Photo Index for folder" + headerPos +  "- " + index);
        return index;
    }

    
    public int getHeaderIndexinListForFolder(int headerPos)
    {
        int index = 0;
        
        if(headerPos == 0) return 0;
        
        for(int i = 0; i < headerPos; i++)
        {
            index = index + (1 + getPhotoCountinFolder(i)); // title + number of photos
        }
        
        LogUtil.debug("PhotoListAdapter:getFirstPhotoIndexinListForFolder", "Photo Index for folder  - " + index);
        return index;
    }
    

    public String prepareList(final Context context, final String dirLink,
            final String searchText, final boolean recurse, final String startIndex, final Listener listener) {
        new GetFileListTask(context, dirLink, searchText, recurse, this.mPhotosOnly, new GetFileListTask.Listener() {

            @Override
            // @nextIndex - This String parameter is passed to the MIPApi to get the next chunk of files
            // @list      - This parameter is the list of 'CloudFile' based objects returned from the server
            public void onCompleted(String nextIndex, int errorCode,
                    ArrayList<Object> list) {
                if(listener.enabled())
                {
                    if (errorCode == ErrorCodes.NO_ERROR) {
                        boolean success = false;

                        // Call a call-back to allow any final processing of the list that higher level
                        // code wants to do.
                        listener.preprocessList(list);
                        
                        if(itemList != null && itemList.size() > 0)
                        {
                            // append results
                            itemList.addAll(list);
                            list = itemList;
                        }
                        
                        // remove anything other than photo or title string
                        
                        int size = list.size();
                        
                        for(int i = size -1 ; i >= 0 ; i--)
                        {
                            Object item = list.get(i);
                            
                            if(!(item instanceof Photo) && !(item instanceof String))
                            { 
                                
                                 // not a photo or string, must be dir 
                                 list.remove(i);
                                 
                                 if(i - 1 >= 0)
                                     item = list.get(i -1);
                                 
                               //remove its title string as well just above it
                                 if((item instanceof String))
                                 {
                                     i = i -1;
                                     list.remove(i);
                                 }
                            }
                        }

                        
                        itemList = list;
                        mNextIndex = nextIndex;

                        listener.onListPrepared(success, errorCode, nextIndex, dirLink, searchText);
                    } else {
                        listener.onListPrepared(false, errorCode, null, dirLink, searchText);
                    }
                }
                else
                {
                    LogUtil.debug("prepareList", "Adapter not enabled, skipping callback for " + dirLink);
                }
            }
        }).execute(startIndex);
        return null;
    }
    
    @Override
    public void increaseItems()
    {
        String nextIndex = mNextIndex;
        if (nextIndex != null)
        {
           this.prepareList(mContext, mFolder, mSearchText, mRecurse, nextIndex, this);
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

            int count = itemList.size(); 
            
            if (count <= 0)
            {
                this.prepareList(mContext, mFolder, mSearchText, mRecurse, null, this);
                return true;
            }
        }
        notifyDataSetChanged();
        return false;
    }



    /**
     * @param position
     * @param convertView
     * @return
     */
    @Override
    protected  View getViewForActivityDirPhotoGrid(int position, View convertView) {
        View view;

        final CloudFile cloudFile = (CloudFile) getItem(position);
        if(cloudFile instanceof Directory)
        {
            if((convertView != null) && (convertView.getId() == R.id.grid_directory_layout))
            {
                view = convertView;
            }
            else
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.grid_directory_layout, null);
                // Set the fixed width of the block.
                view.setLayoutParams(new GridView.LayoutParams(mGridTileSize, mGridTileSize));
            }
            TextView directoryNameView = (TextView) view.findViewById(R.id.directory_name);
            directoryNameView.setText(cloudFile.getTitle());

           // return view;
        }
        else
        {
            if((convertView != null) && (convertView.getId() != R.id.grid_directory_layout))
            {
                view = convertView;
            }
            else
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.grid_image_layout, null);
                // Set the fixed width of the block.
                view.setLayoutParams(new GridView.LayoutParams(mGridTileSize, mGridTileSize));
            }
            
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
    
            long versionId = 0;
            long lastModified = 0;
            long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
            String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
            if(cloudFile != null)
            {
                lastModified = cloudFile.getUpdated();
                name = cloudFile.getTitle();
                bytesize = cloudFile.getSize();
                versionId = cloudFile.getVersionId();
            }

            /*
             * Try to get thumbnail bitmap
             */
            
            Bitmap bitmap = null;
            ThumbnailParams params = new ThumbnailParams(
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, true);
            
            
            bitmap = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
           

            if(bitmap != null)
            {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
 
                this.resizeBitmapIfNeeded(imageView, bitmap);
            }
            else
            {
                if(cloudFile != null)
                {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);             
                    imageView.setImageDrawable(FileUtils.getIcon(mContext,cloudFile, mRootDeviceTitle, mRootDeviceId, mDeviceEncrypted ));
                }
            }
        }
        return view;
    }
    

    /**
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    protected View getViewForActivityPhotoSlideShow(int position,
            View convertView, ViewGroup parent) {
        final ViewHolderPhotoFullscreen holder;
        if(convertView == null)
        {
            holder = new ViewHolderPhotoFullscreen();
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.photo_full_screen, null);

            holder.photo_fullscreen_img = (ImageView) convertView.findViewById(R.id.photo_fullscreen_img);
            holder.photo_details_layout = (ViewGroup) convertView.findViewById(R.id.photo_details_layout);
            holder.photo_details_headline = (TextView) convertView.findViewById(R.id.photo_details_headline);
            holder.photo_detail_label_filename = (TextView) convertView.findViewById(R.id.photo_detail_label_filename);
            holder.photo_detail_filename = (TextView) convertView.findViewById(R.id.photo_detail_filename);
            holder.photo_detail_label_resolution = (TextView) convertView.findViewById(R.id.photo_detail_label_resolution);
            holder.photo_detail_resolution = (TextView) convertView.findViewById(R.id.photo_detail_resolution);
            holder.photo_detail_label_kbsize = (TextView) convertView.findViewById(R.id.photo_detail_label_kbsize);
            holder.photo_detail_kbsize = (TextView) convertView.findViewById(R.id.photo_detail_kbsize);
            holder.photo_detail_label_taken_date = (TextView) convertView.findViewById(R.id.photo_detail_label_taken_date);
            holder.photo_detail_taken_date = (TextView) convertView.findViewById(R.id.photo_detail_taken_date);
            holder.photo_detail_label_camera_manufacturer = (TextView) convertView.findViewById(R.id.photo_detail_label_camera_manufacturer);
            holder.photo_detail_camera_manufacturer = (TextView) convertView.findViewById(R.id.photo_detail_camera_manufacturer);
            holder.photo_detail_camera_model = (TextView) convertView.findViewById(R.id.photo_detail_camera_model);
            convertView.setTag(holder);
        }
        else
        {
            holder = (ViewHolderPhotoFullscreen) convertView.getTag();
        }

        holder.photo_details_headline.setText(mContext.getResources().getString(R.string.photo_details_headline));
        holder.photo_detail_label_filename.setText(mContext.getResources().getString(R.string.photo_detail_label_filename) + " ");
        holder.photo_detail_label_resolution.setText(mContext.getResources().getString(R.string.photo_detail_label_resolution) + " ");
        holder.photo_detail_label_kbsize.setText(mContext.getResources().getString(R.string.photo_detail_label_kbsize) + " ");
        holder.photo_detail_label_taken_date.setText(mContext.getResources().getString(R.string.photo_detail_label_taken_date) + " ");
        holder.photo_detail_label_camera_manufacturer.setText(mContext.getResources().getString(R.string.photo_detail_label_camera_manufacturer) + " ");

        ((SlideView) parent).setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View view, MotionEvent ev) {
                final int action = ev.getAction();
                switch (action) {
                case MotionEvent.ACTION_UP:
                    if (mDetailsVisible) {
                        //ViewHolderPhotoFullscreen holder = (ViewHolderPhotoFullscreen) ((SlideView) view).getSelectedView().getTag();
                        //holder.photo_details_layout.setVisibility(View.INVISIBLE);
                        mDetailsVisible = false;
                    } else {
                        // if released close to where pressed and relatively
                        // quickly ie tap
                        if (ev.getEventTime() - ev.getDownTime() < 250) {
                            if (Math.max(mDownX, ev.getX()) - Math.min(mDownX, ev.getX()) < 22) {
                                if (Math.max(mDownY, ev.getY()) - Math.min(mDownY, ev.getY()) < 22) {
                                    //ViewHolderPhotoFullscreen holder = (ViewHolderPhotoFullscreen) ((SlideView) view).getSelectedView().getTag();
                                    //holder.photo_details_layout.setVisibility(View.VISIBLE);
                                    mDetailsVisible = true;
                                }
                            }
                        }
                    }
                    break;
                case MotionEvent.ACTION_DOWN:
                    mDownY = ev.getY();
                    mDownX = ev.getX();
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mDetailsVisible) {
                        if (Math.max(ev.getX(), mDownX) - Math.min(ev.getX(), mDownX) > 22) {
                            //ViewHolderPhotoFullscreen holder = (ViewHolderPhotoFullscreen) ((SlideView) view).getSelectedView().getTag();
                            //holder.photo_details_layout.setVisibility(View.INVISIBLE);
                            mDetailsVisible = false;
                        }
                    }
                    break;
                }

                return false;
            }
        });

        CloudFile item = null;
        Object listItem = this.getItem(position);
        if(listItem instanceof String)
        {
            LogUtil.error(this, "String object received in slideShow mode!!");
        }
        else
        {
            item = (CloudFile) listItem;
            if(item != null)
            {
                String stringFilename = item.getTitle();

                if (stringFilename != null)
                {
                    holder.photo_detail_label_filename.setVisibility(View.VISIBLE);
                    holder.photo_detail_filename.setVisibility(View.VISIBLE);
                    holder.photo_detail_filename.setText(stringFilename);
                }
                else
                {
                    holder.photo_detail_label_filename.setVisibility(View.INVISIBLE);
                    holder.photo_detail_filename.setVisibility(View.INVISIBLE);
                    holder.photo_detail_label_filename.setHeight(0);
                    holder.photo_detail_filename.setHeight(0);
                }
            }
        } // if !(listItem instanceof String)

        long versionId = 0;
        long lastModified = 0;
        long bytesize = System.currentTimeMillis();  // Pick something that doesn't collide
        String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
        if(listItem != null && listItem instanceof CloudFile)
        {
            lastModified = ((CloudFile) listItem).getUpdated();
            name = ((CloudFile) listItem).getTitle();
            bytesize = ((CloudFile) listItem).getSize();
            versionId = ((CloudFile) listItem).getVersionId();
        }

        /*
         * Try to get thumbnail
         */
        
        
        Bitmap bitmap = null;
        
        ThumbnailParams params = new ThumbnailParams(
                mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
        

         bitmap = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);

        // Set the appropriate image and its scaling.
        // CENTER means no scaling
        // FIT_CENTER means image is scaled to fit, at least one axis fitting exactly.
        ImageView.ScaleType scaleSetting = ImageView.ScaleType.CENTER;
        if (bitmap != null)
        {
            holder.photo_fullscreen_img.setImageBitmap(bitmap);
            if (bitmap.getWidth() >= mDisplayWidth || bitmap.getHeight() >= mDisplayWidth
                || (mDisplayWidth >= MAX_THUMBNAIL_SIZE))
            {
                // If the image is "big enough", then we want to have it show as big
                // as will fit on the screen while maintaining aspect ratio. "Big enough"
                // happens if the image has a dimension larger than the screen's smaller
                // dimension OR if the image is maximum-sized but is smaller than the screen.
                // In the latter case, the display will zoom the image to fit.
                scaleSetting = ImageView.ScaleType.FIT_CENTER;
            }
            fireThumbnailAvailable(position);
        }
        else
        {
            // Request the thumbnail
            mThumbnailManager.getThumbnail(getThumbnailPath(), name, bytesize, position, params, null);
            LogUtil.debug(this, "Requesting thumbnail again as not in the cache!");
            
            if (item != null)
            {
                holder.photo_fullscreen_img.setImageDrawable(FileUtils.getIcon(mContext,item,mRootDeviceTitle, mRootDeviceId, mDeviceEncrypted));
            }
            else
            {
                holder.photo_fullscreen_img.setImageResource(R.drawable.image_icon_large);
            }
        }
        holder.photo_fullscreen_img.setScaleType(scaleSetting);
        return convertView;
    }

    /**
     * @param position
     * @param convertView
     * @return
     */
    @Override
    protected View getViewForActivityDirFileList(int position, View convertView) {
        View view = null;
        if (isNextItem(position) == false)   // not the "more downloads"
        {
            
            if((position % 2) == 0)  // odds are titles
            {
                Object listItem = getItem(position);
                
             // Is this a 'title'?
                if(listItem instanceof String)
                {
                    view = setViewForTitleHeaders(convertView, listItem);
                }
            }
            else
            {
                // translate to header position
                
                int headerPos = getHeaderCountForPosition(position);
                
                if(this.getPhotoCountinFolder(headerPos) <= 1)
                {
                    view = setViewForOnePhotoFolder(position,convertView);
                }
                else
                {
                    view = setViewForMorethanOnePhotoFolders(position, convertView);
                }
            }
        }
        else // position == totalCount, which means this is the 'get more data' item.
        {
            view = setViewForDownloadMoreItems(convertView);
        }
        return view;
    }

    /**
     * @param convertView
     * @param listItem
     * @return
     */
    public View setViewForTitleHeaders(View convertView, Object listItem) {
        View view;
        if((convertView != null) && (convertView.getId() == R.id.searchSubtitle))
        {
            view = convertView;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.search_subtitle_layout, null);
        }

        TextView textView = (TextView) view.findViewById(R.id.subtitle);
        textView.setText(listItem.toString());
        return view;
    }

    /**
     * @param convertView
     * @return
     */
    public View setViewForDownloadMoreItems(View convertView) {
        View view;
        if ((convertView != null) && (convertView.getId() == R.id.listItemLayout))
        {
            view = convertView;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(mResourceId, null);
        }

        ImageView imageView1 = (ImageView) view.findViewById(R.id.image1);
        TextView textView = (TextView) view.findViewById(R.id.name);
        TextView descriptionView = (TextView)view.findViewById(R.id.details);
        
        ImageView imageView2 = (ImageView) view.findViewById(R.id.image2);
        ImageView imageView3 = (ImageView) view.findViewById(R.id.image3);
        
        imageView2.setVisibility(View.GONE);
        imageView3.setVisibility(View.GONE);
        
        TextView countView = (TextView)view.findViewById(R.id.count);
        countView.setVisibility(View.GONE);

        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int displayWidth = display.getWidth();

        view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));

        textView.setText(mContext.getResources().getString(R.string.next_items));
        imageView1.setImageResource(R.drawable.placeholder_icon_small);
        descriptionView.setVisibility(View.GONE);
        return view;
    }

    /**
     * @param position
     * @param convertView
     * @param listItem
     * @return
     */
    public View setViewForOnePhotoFolder( int position, View convertView) {
        View view;
        
        // translate to header position
        
        int headerPos = getHeaderCountForPosition(position);
        
        if ((convertView != null) && (convertView.getId() == R.id.listItemLayout))
        {
            view = convertView;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(mResourceId, null);
        }
        ImageView imageView1 = (ImageView) view.findViewById(R.id.image1);
        ImageView imageView2 = (ImageView) view.findViewById(R.id.image2);
        ImageView imageView3 = (ImageView) view.findViewById(R.id.image3);
        
        imageView2.setVisibility(View.GONE);
        imageView3.setVisibility(View.GONE);
        
        TextView textView = (TextView) view.findViewById(R.id.name);
        TextView detailsView = (TextView)view.findViewById(R.id.details);
        
        textView.setVisibility(View.VISIBLE);
        
        TextView countView = (TextView)view.findViewById(R.id.count);
        countView.setVisibility(View.GONE);
        
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
   
        int displayWidth = display.getWidth();
   
        view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));
        
        int firstPhotoIndex = this.getFirstPhotoIndexinListForFolder(headerPos);
        CloudFile file = (CloudFile) itemList.get(firstPhotoIndex);
        if(file != null)
        {
            String fileTitle =  file.getTitle();
            
            textView.setText(fileTitle);
   
            long versionId = 0;
            long lastModified = 0;
            long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
            String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
            if(file != null)
            {
                lastModified = file.getUpdated();
                name = file.getTitle();
                bytesize = file.getSize();
                versionId = file.getVersionId();
            }
   
            Bitmap bm1 = null;
            
            ThumbnailParams params = new ThumbnailParams(
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, true);
            
            bm1 = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
   
            if (bm1 != null)
            {
                imageView1.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView1.setImageBitmap(bm1);
            }
            else
            {
                imageView1.setImageDrawable(FileUtils.getIcon(mContext,file, mRootDeviceTitle, mRootDeviceId, mDeviceEncrypted ));
            }
            
            String details = "";
            
            if (!(file instanceof Directory))  // not a directory
            {
                details = FileUtils.getDetails(mContext, file.getUpdated(), file.getSize());
            }
   
            detailsView.setText(details);
            detailsView.setVisibility(details.length() == 0 ? View.GONE : View.VISIBLE);
        }
        else
        {
            textView.setText(mContext.getResources().getString(R.string.progress_bar_loading));
            imageView1.setImageResource(R.drawable.placeholder_icon_small);
            detailsView.setVisibility(View.GONE);
        }
        return view;
    }

    /**
     * @param position
     * @param convertView
     * @return
     */
    public View setViewForMorethanOnePhotoFolders(int position, View convertView) {
        View view;
        
        // translate to header position
        
        int headerPos = getHeaderCountForPosition(position);
       
        if ((convertView != null) && (convertView.getId() == R.id.listItemLayout))
        {
            view = convertView;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(mResourceId, null);
        }
        ImageView imageView1 = (ImageView) view.findViewById(R.id.image1);
        ImageView imageView2 = (ImageView) view.findViewById(R.id.image2);
        ImageView imageView3 = (ImageView) view.findViewById(R.id.image3);
        
        TextView textView = (TextView) view.findViewById(R.id.name);
        textView.setVisibility(View.GONE);
        TextView detailsView = (TextView)view.findViewById(R.id.details);
        detailsView.setVisibility(View.GONE);
        TextView countView = (TextView)view.findViewById(R.id.count);
        countView.setVisibility(View.VISIBLE);
   
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
   
        int displayWidth = display.getWidth();
   
        view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));
   
        boolean isThreeOrMorePhotos = (this.getPhotoCountinFolder(headerPos) > 2) ? true:false;
            
        imageView2.setVisibility(View.VISIBLE);
        
        if(isThreeOrMorePhotos)
            imageView3.setVisibility(View.VISIBLE);
        
        int firstPhotoIndex = this.getFirstPhotoIndexinListForFolder(headerPos);
        
        Object item = itemList.get(firstPhotoIndex);
        
        if(item == null)
        {
            imageView1 = (ImageView) view.findViewById(R.id.image1);
            imageView1.setVisibility(View.GONE);
        }else if(item != null && item instanceof Photo)
        {
            CloudFile file = (CloudFile)item;
   
            long versionId = 0;
            long lastModified = 0;
            long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
            String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
            if(file != null)
            {
                lastModified = file.getUpdated();
                name = file.getTitle();
                bytesize = file.getSize();
                versionId = file.getVersionId();
            }
   
            Bitmap bm1 = null;
            
            ThumbnailParams params = new ThumbnailParams(
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, true);
            
            bm1 = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
   
            if (bm1 != null)
            {
                imageView1.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView1.setImageBitmap(bm1);
            }
            else
            {
                if(mDeviceEncrypted == true)
                {
                    imageView1.setImageResource(R.drawable.imagestack_placeholder_v5_locked);
                }
                else
                {
                    imageView1.setImageResource(R.drawable.imagestack_placeholderv5);
                }
            }
        }

        item = itemList.get(firstPhotoIndex + 1); ;
        
        if(item == null)
        {
            
            imageView2 = (ImageView) view.findViewById(R.id.image2);
            imageView2.setVisibility(View.GONE);
            
        }else if(item != null && item instanceof Photo)
        {
            
            CloudFile file = (CloudFile) item;
   
            long versionId = 0;
            long lastModified = 0;
            long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
            String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
            if(file != null)
            {
                lastModified = file.getUpdated();
                name = file.getTitle();
                bytesize = file.getSize();
                versionId = file.getVersionId();
            }
   
            Bitmap bm2 = null;
            
            ThumbnailParams params = new ThumbnailParams(
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, true);
            
            bm2 = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
            
            if (bm2 != null)
            {
                imageView2.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                imageView2.setImageBitmap(bm2);
            }
            else
            {
                if(mDeviceEncrypted == true)
                {
                    imageView2.setImageResource(R.drawable.imagestack_placeholder_v5_locked);
                }
                else
                {
                    imageView2.setImageResource(R.drawable.imagestack_placeholderv5);
                }
            }                     
        }
        
        if(isThreeOrMorePhotos)
        {
            item = itemList.get(firstPhotoIndex + 2);
            
            if(item == null)
            {
                
                imageView3 = (ImageView) view.findViewById(R.id.image3);
                
                imageView3.setVisibility(View.GONE);
    
                
            }else if(item != null && item instanceof Photo)
            {
                
                CloudFile file = (CloudFile) item;
       
                long versionId = 0;
                long lastModified = 0;
                long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
                String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
                if(file != null)
                {
                    lastModified = file.getUpdated();
                    name = file.getTitle();
                    bytesize = file.getSize();
                    versionId = file.getVersionId();
                }
                Bitmap bm3 = null;
                
                ThumbnailParams params = new ThumbnailParams(
                        mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, true);
                
                bm3 = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
                
                if (bm3 != null)
                {
                    imageView3.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView3.setImageBitmap(bm3);
                }
                else
                {
                    if(mDeviceEncrypted == true)
                    {
                        imageView3.setImageResource(R.drawable.imagestack_placeholder_v5_locked);
                    }
                    else
                    {
                        imageView3.setImageResource(R.drawable.imagestack_placeholderv5);
                    }
                }
            }
        }
        else
        {
            imageView3 = (ImageView) view.findViewById(R.id.image3);
            
            imageView3.setVisibility(View.GONE);
        }
        String count = Integer.toString(this.getPhotoCountinFolder(headerPos));
        countView.setText(count);
       
        return view;
    }


    /**
     * Trigger download of thumbnails for the current "window", i.e. those
     * indexes we should try to keep in the cache (the currently visible ones
     * plus a couple extra in the direction in which we are moving).
     */
    @Override
    protected void preload()
    {
        Vector<Integer> window = mSlidingWindow.getWindow();

       for(int position : window)
       {
           if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST)  )
           {
               Object listItem = getItem(position);
               if(listItem != null && listItem instanceof Photo)
               {
                   getThumbnailForListItem(position, listItem);
                   
                // translate to header position
                   
                   int headerPos = getHeaderCountForPosition(position);
                     
                   boolean isTwoOrMorePhotos = (this.getPhotoCountinFolder(headerPos) > 1) ? true:false;
                   boolean isThreeOrMorePhotos = (this.getPhotoCountinFolder(headerPos) > 2) ? true:false;
                   
                   int firstPhotoIndex = getFirstPhotoIndexinListForFolder(headerPos);
                   
                   if(isTwoOrMorePhotos)
                   {
                       
                        listItem = itemList.get(firstPhotoIndex + 1);
                        
                        if(listItem != null && listItem instanceof Photo)
                        {
                            getThumbnailForListItem(position, listItem);
                        }
                   }
                    
                   if(isThreeOrMorePhotos)
                   {
                        listItem = itemList.get(firstPhotoIndex + 2);  
                        
                        if(listItem != null && listItem instanceof Photo)
                        {
                            getThumbnailForListItem(position, listItem);
                        }
                   }
                }
            }
           else if ((mActivityType == ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW))
           {
          
               Object listItem = photoList.get(position);
               if(listItem != null && listItem instanceof Photo)
               {
                   getThumbnailForListItem(position, listItem);
               }
           }
           else if ((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID))
           {
               Object listItem = this.getItemForPhotoSearchGrid(position);
               if(listItem != null && listItem instanceof Photo)
               {
                   getThumbnailForListItem(position, listItem);
               }
           }
        }
    }
    
    
    

    /**
     * @param position
     * @param listItem
     */
    public void getThumbnailForListItem(int position, Object listItem) {
        long lastModified;
        String name;
        long bytesize;
        long version;
        ThumbnailParams params;
        lastModified = ((Photo) listItem).getUpdated();
        name = ((Photo) listItem).getTitle();
        bytesize = ((Photo) listItem).getSize();
        version = ((Photo) listItem).getVersionId();

        params = new ThumbnailParams(
             mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, version, null, mFoldersOnly, mPhotosOnly, true);
        
            mThumbnailManager.getThumbnail(getThumbnailPath(), name, bytesize, position, params, listItem);
    }
 
    // Implementation of the 'ListManager.Listener' interface
    @Override
    public void onListPrepared(boolean success, int errorCode, String nextStartIndex,
            String dirLink, String searchText)
    {
        this.mErrorCode = errorCode;
        
        //  List has been prepared, access and process it.
        
        boolean status = getAndProcessList();
        
        postProcessList();
 
        synchronized (this) {
            if (mListAdapterDataListener != null && (status == true)) {
                mListAdapterDataListener.onDataRetrieved(this);
            }
        }
        synchronized (this) {
            if (mEnabled) {
                Handler handler = new Handler(mContext.getMainLooper());
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
            }
        }
    }

    private void postProcessList() {
        
        int firstCollapseCandidate = 0;   
        int numCollapseCandidates = 0;
        String rootCommonDir = null;
        
        if(photoCountInDirList != null)
        {
            int size = photoCountInDirList.size();
            
            for (int i = 0; i < size; i++) 
            {
                // if dir is sparse
                if(photoCountInDirList.get(i) < 7)
                {
                    numCollapseCandidates = numCollapseCandidates + 1;
                    
                    if(numCollapseCandidates == 1)
                    {
                        firstCollapseCandidate = i;
                    }
                    
                    if(numCollapseCandidates > 1)
                    {
                        // get common root dir
                        
                        String titleStr1 = getHeaderforIndex(i);
                        
                        String titleStr2 = null;
                        
                        if(rootCommonDir != null)
                             titleStr2 = rootCommonDir;
                        else
                            titleStr2 = getHeaderforIndex(i -1);
                        
                        rootCommonDir = getCommonRootDir(titleStr1, titleStr2);
                        
                        // no common root dir
                        if(rootCommonDir == null)
                        {
                            // do we have enough collapsing dirs
                            if(numCollapseCandidates >= 3)
                            {
                                // collapse all the previous eligible collapse folders and move on

                                collapseList(firstCollapseCandidate, numCollapseCandidates - 1, titleStr2);
                                
                                
                                // update the size of the list and rewind the index
                                size = photoCountInDirList.size();
                                
                                i = i - (numCollapseCandidates - 1);
                            }
 
                            // reset to this as the first collapse candidate
                            firstCollapseCandidate = i;
                            numCollapseCandidates = 1;
                        } 
                    }

                }
                else
                {
                    // see if there are three consecutive sparse directories
                    if(numCollapseCandidates >= 3)
                    {
                        // collapse these directories 
                        collapseList(firstCollapseCandidate, numCollapseCandidates, rootCommonDir);
                        
                        // update the size of the list and rewind the index
                        size = photoCountInDirList.size();
                        
                        i = i - numCollapseCandidates;
                    }
                    // reset the collapse candidate list
                    firstCollapseCandidate = 0;
                    numCollapseCandidates = 0;
                    rootCommonDir = null;
 
                }
            }
            
            // see if there are three consecutive sparse directories
            if(numCollapseCandidates >= 3)
            {
                // collapse these directories 
                collapseList(firstCollapseCandidate, numCollapseCandidates, rootCommonDir);
                
                // update the size of the list 
                size = photoCountInDirList.size();
            }
            // reset the collapse candidate list
            firstCollapseCandidate = 0;
            numCollapseCandidates = 0;
            rootCommonDir = null;
        }     
    }

    
    /**
     * Returns the header for given index
     */
    private String getHeaderforIndex(int index) {
        
        int titleCount = 0;
        
        if(itemList != null && itemList.size() > 0)
        {
            for(int i =0; i < itemList.size(); i++)
            {
                Object item = itemList.get(i);
                
                if(item instanceof String)
                {
                    if(titleCount == index)
                    {
                        return (String) item;
                    }
                    titleCount++;
                }
            }
        }
           
        return null;
    }
    
    
    /**
     * Collapses the list 
     */
    private void collapseList(int firstCollapseCandidate, int numCollapseCandidates, String rootCommonDir) {
        
        int titleCount = 0;
        
        if(itemList != null && itemList.size() > 0)
        {
            int size  = itemList.size();
            
            for(int i =0; i < size; i++)
            {
                Object item = itemList.get(i);
                
                if(item instanceof String)
                {
                    if(titleCount == firstCollapseCandidate)
                    {
                         // replace with common root dir
                        itemList.set(i, rootCommonDir);
                    }
                    else if(titleCount > firstCollapseCandidate && titleCount < (firstCollapseCandidate + numCollapseCandidates))
                    {
                         // discard title
                         itemList.remove(i);
                         
                         // update photo count list after the collapse
                         int newCount = photoCountInDirList.get(titleCount) + photoCountInDirList.get(firstCollapseCandidate);
                         
                         photoCountInDirList.set(firstCollapseCandidate,newCount);
                         
                         photoCountInDirList.remove(titleCount);
                         
                         // new size of list 
                         size  = itemList.size();
                         
                         // decrement indexes
                         i--;
                         titleCount --;
                         numCollapseCandidates --;
                     }
                    else if(titleCount > (firstCollapseCandidate + numCollapseCandidates))
                    {
                        // done
                        return;
                    }
                    
                    titleCount++;
                }       
            }
        }
        
        return;
    }

    private String getCommonRootDir(String folderPathStr1, String folderPathStr2) 
    {
        String commonRootDir = null;
        final char slash = '/';
        boolean hasForwardSlash = false;
        String shorterPathStr = null;
        String largerPathStr = null; 
        
         
        if(folderPathStr1.length() >= folderPathStr2.length())
        {
            shorterPathStr = folderPathStr2.replace('\\', slash);
            largerPathStr = folderPathStr1.replace('\\',slash);
            
            if(shorterPathStr.equalsIgnoreCase(folderPathStr2))
            {
                hasForwardSlash = true;
            }
        }
        else
        {
            shorterPathStr = folderPathStr1.replace('\\', slash);
            largerPathStr = folderPathStr2.replace('\\',slash);
            if(shorterPathStr.equalsIgnoreCase(folderPathStr1))
            {
                hasForwardSlash = true;
            }
        }
        
 
        while(shorterPathStr.lastIndexOf(slash) != -1)
        {
            if(largerPathStr.startsWith(shorterPathStr))
            {
                if(hasForwardSlash == false)
                    commonRootDir = shorterPathStr.replace(slash, '\\');
                else
                    commonRootDir = shorterPathStr;
                return commonRootDir;
            } 
            
            int index = shorterPathStr.lastIndexOf(slash);
            
            shorterPathStr = shorterPathStr.substring(0, index);
        }
                    
        return commonRootDir;
    }

    /**
     * Build up the 3 lists (itemLists (headers + photos), photoList (photo only lists for slide show) 
     * and photoCountInDirList to track photos in each folder
     */
    protected boolean getAndProcessList() 
    {      
        int count = 0;
        
        if(itemList != null && itemList.size() > 0)
        {
            Object item = itemList.get(0);
            
            //clear the photoList and photoCountInDirList, if exists
            
            if(photoList != null && photoList.size() > 0)
                photoList.clear();
            
            if(photoCountInDirList != null && photoCountInDirList.size() > 0)
                photoCountInDirList.clear();
            
            if(!(item instanceof String))
                return false;   // First item has to be title and should always be string
            
            for(int i = 1; i < itemList.size(); i++)
            {
                item = itemList.get(i);
                
                if(item instanceof Photo)
                { 
                    count = count + 1;
                    photoList.add(item);
                }
                else if (item instanceof String)
                {
                    photoCountInDirList.add(count);
                    count = 0; // reset for next 
                }
                else
                {
                      // ignore other than photo or title
                }
            }
            
            photoCountInDirList.add(count);
        }
        return true;
    }
    
    
    // Clear the static lists onDestroy of activity
    public void clearLists()
    {
     // clear out the lists
        if(PhotoListAdapter.itemList != null)
            PhotoListAdapter.itemList.clear();
        
        if(PhotoListAdapter.photoList != null)
            PhotoListAdapter.photoList.clear();
        
        if(PhotoListAdapter.photoCountInDirList != null)
            PhotoListAdapter.photoCountInDirList.clear();
    }
   

    // calculates position for CalculatePhotoPosition task based on folder position (only for the grid) and its actual position
    public int getPhotoPosition(int position, int folderPosition)
    {
        int headerPos = 0;
        int index = 0;
        
        if(folderPosition == -1)  //called from dir file list view
            headerPos = getHeaderCountForPosition(position);   // starts from 0,1,2 ..
        else
            headerPos = getHeaderCountForPosition(folderPosition);   // starts from 0,1,2 ...
        
        
        int totalTitleHeaderCount = headerPos + 1;
        
        int firstPhotoOfFolderIndex = this.getFirstPhotoIndexinListForFolder(headerPos);
        
        if(folderPosition == -1)  //called from dir file list view
            index = firstPhotoOfFolderIndex - totalTitleHeaderCount;
        else
            index =  (firstPhotoOfFolderIndex -totalTitleHeaderCount + position);
        
        return index;
    }
}
