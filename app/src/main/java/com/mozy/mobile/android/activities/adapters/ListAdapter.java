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
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.DirPhotoGridActivity;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManager;

import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailDownloadService;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailEncryptedFilesService;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManagerForHiResFiles;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailParams;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManager.ThumbnailManagerListener;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.views.SlideView;


public class ListAdapter extends BaseAdapter implements ListManager.Listener, ThumbnailManagerListener
{
    private static final int NUMBER_OF_COLUMNS = DirPhotoGridActivity.NUM_COLUMNS;

    /**
     * Activity type for DirFileListActivity
     */
    public static final int ACTIVITY_TYPE_DIR_FILE_LIST = 0;

    /**
     * Activity type for DirPhotoGridActivity
     */
    public static final int ACTIVITY_TYPE_DIR_PHOTO_GRID = 1;

    /**
     * Activity type for PhotoSlideShowActivity
     */
    public static final int ACTIVITY_TYPE_PHOTO_SLIDE_SHOW = 2;

    /**
     * The activity type that is usgin this adapter for its data
     */
    protected int mActivityType = -1;

    protected static final int MAX_THUMBNAIL_SIZE = 512;
    
    /**
     * Interface to be implemented by clients interested in listening for
     * data updates from this adapter
     */
    public static interface ListAdapterDataListener
    {
        void onDataRetrieved(ListAdapter callingAdapter);
    }

    /**
     * The application context
     */
    protected final Context mContext;

    /**
     * The folder whose contents are being provided by this adapter
     */
    protected final String mFolder;
    
    
    // Container encrypted ?
    protected final boolean mDeviceEncrypted;
    
    protected final String mRootDeviceId;
    protected final String mRootDeviceTitle;

    /**
     * The current search text
     */
    protected final String mSearchText;
    // Folder where the search took place.
    protected String mSearchDirectory;

    /**
     * The thumbnail download manager that handles download and caching of
     * thumbnails
     */
    protected ThumbnailManager mThumbnailManager = null;

    // If this.dirsOnly and this.photosOnly are both true, then both types, and only both types,
    // are included in the list.
    /**
     * Whether to display only folders
     */
    protected final boolean mFoldersOnly;

    /**
     * Whether to display only photos
     */
    protected final boolean mPhotosOnly;

    /**
     * Recurse?
     */
    protected final boolean mRecurse;

    /**
     * Resource ID??
     */
    protected final int mResourceId;

    /**
     * Width of display
     */
    protected final int mDisplayWidth;

    /**
     * Height of display
     */
    private final int mDisplayHeight;

    /**
     * Thumbnail required??
     */
    //  private final boolean mThumbnailRequired;

    /**
     * Listener that will receive updates when thumbnails are downloaded
     */
    protected ThumbnailAvailabilityListener mThumbnailAvailabilityListener;

    /**
     * Listener that receives updates as data is retrieved. Controls the
     * "Loading" dialog in activities.
     */
    protected ListAdapterDataListener mListAdapterDataListener;
    
    /**
     * ErrorCode from getFilelistTask
     */
    protected int mErrorCode;;

    public int getErrorCode() {
        return mErrorCode;
    }

    /*
     * ??
     */
    protected boolean mDetailsVisible;

    /**
     * Whether the adapter is enabled
     */
    protected boolean mEnabled;

    /**
     * Touch movement
     */
    float mDownX;

    /**
     * Touch movement
     */
    float mDownY;

    /**
     * Size of thumbnails in the current list view
     */
    protected int mThumbnailWidth;

    /**
     * Size of thumbnails in the current list view
     */
    protected int mThumbnailHeight;

    /**
     * Size (assumes square) of a grid tile. Includes padding and border
     * but not extra margin.
     */
    protected int mGridTileSize;
    
    protected int twoMargin;

    /**
     * Dip height of grid elements
     */
    private static final float GRID_ITEM_HEIGHT_IN_DIP = 35.0f;

    /**
     * Dip height of grid elements
     */
    private static final float GRID_ITEM_WIDTH_IN_DIP = 35.0f;

    /**
     * Item count from server
     */
    protected int mTotalCount;

    protected SlidingWindow mSlidingWindow;
    
    protected float resizeFactorForGridView = (float) 1.0;
    
    public int lastUploadPositionInDirFileList = -1;
 

    public ListAdapter(Context context,
            int resourceId,
            final String parentDirLink,
            final boolean encryptedContainer,
            final String searchText,
            final String searchDirectory,
            final boolean directoriesOnly,
            final boolean photosOnly,
            final boolean recurse,
            boolean refresh,
            ListAdapterDataListener listener,
            int inputActivityType,
            ThumbnailAvailabilityListener thumbListener,
            String rootDeviceId,
            String rootDeviceTitle)
    {
        mContext = context.getApplicationContext();
        mFolder = parentDirLink;
        mDeviceEncrypted = encryptedContainer;
        mSearchDirectory = searchDirectory;
        mSearchText = searchText;
        mFoldersOnly = directoriesOnly;
        mRecurse = recurse;
        mPhotosOnly = photosOnly;
        mResourceId = resourceId;
        mListAdapterDataListener = listener;
        mActivityType = inputActivityType;
        mThumbnailAvailabilityListener = thumbListener;

        mSlidingWindow = new SlidingWindow(mActivityType);
        
        mRootDeviceId = rootDeviceId;
        mRootDeviceTitle = rootDeviceTitle;

        /*
         * Setup thumbnail manager
         */
        ThumbnailDownloadService service = null;
        
        if(mDeviceEncrypted == false)    
        {
             service = new ThumbnailDownloadService(context, 5);
             mThumbnailManager = ThumbnailManager.getInstance(context, service);
        }
        else
        {
            service  = new ThumbnailEncryptedFilesService(context, mRootDeviceId, 5);
            mThumbnailManager = ThumbnailManagerForHiResFiles.getInstance(context, service);
        }
        mThumbnailManager.setThumbnailManagerListener(this);
        /*
         * Dimension calculations
         */
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int tmpWidth = display.getWidth();
        int tmpHeight = display.getHeight();
        float scale = context.getResources().getDisplayMetrics().density;


        if(tmpWidth > tmpHeight)
        {
            mDisplayWidth = tmpHeight;
            mDisplayHeight = tmpWidth;
        }
        else
        {
            mDisplayWidth = tmpWidth;
            mDisplayHeight = tmpHeight;
        }

        if(mActivityType == ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW)
        {
            /*
             * Get a preview big enough to fill up the screen in either orientation.
             */
            
            if(mDisplayHeight > 480 || mDisplayWidth > 480) // restrict it to no more than 480
            {
                if(mDisplayWidth > mDisplayHeight)  // keep the aspect ratio
                {
                    mThumbnailHeight = (int) (((float) 480/mDisplayWidth) * mDisplayHeight);
                    mThumbnailWidth = 480;
                     
                }
                else
                {
                    mThumbnailWidth = (int) (((float)480/mDisplayHeight) * mDisplayWidth);
                    mThumbnailHeight = 480;
                }
            }
            else
            {
                mThumbnailWidth = mDisplayWidth;
                mThumbnailHeight = mDisplayHeight;
            }
        }
        else if(mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST)
        {
            /*
             * Thumbnail dips to pixels calculation
             */
            mThumbnailHeight = (int) (GRID_ITEM_HEIGHT_IN_DIP * scale + 0.5f);
            mThumbnailWidth = (int) (GRID_ITEM_WIDTH_IN_DIP * scale + 0.5f);
        }
        else // mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID
        {
            /*
             * Want to get a size that will fit NUMBER_OF_COLUMNS on the *short* side (displayWidth)
             */
            this.twoMargin = (2 * (int)(DirPhotoGridActivity.GRID_MARGIN * scale + 0.5f));
            mGridTileSize = (mDisplayWidth - twoMargin) / NUMBER_OF_COLUMNS;
            
            if(this.mGridTileSize - this.twoMargin > 240) // restrict it to no more than 240
            {
                mThumbnailHeight = this.mThumbnailWidth = 240;
                
                resizeFactorForGridView =  (float) ((this.mGridTileSize - twoMargin) /240.0);
                
            }
            else
            {
                mThumbnailHeight = this.mThumbnailWidth = this.mGridTileSize - this.twoMargin;
            }
        }
        
        if(mThumbnailManager != null)
        {
            mThumbnailManager.setupThumbnailCacheForActivity(mActivityType, mThumbnailWidth, mThumbnailHeight);
        }
        
        /*
         * Re-initialize database cursor
         */
        if(refresh)
        {
            ListManager.getInstance().cleanUp(context, true);
            
            mThumbnailManager.clearCache(getThumbnailPath(), mThumbnailWidth, mThumbnailHeight);
        }

        prepareInitialList();
    }

    /**
     * @param context
     * @param searchText
     */
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
            ListManager.getInstance().prepareList(mContext, mFolder, mSearchText, this.mRecurse, null, this);
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
    public void onThumbnail(String path, int index)
    {
        fireThumbnailAvailable(index);

        synchronized (this)
        {
            if (mEnabled)
            {
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
    }

    public int getNumColumnsForGridView()
    {
        if(mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID)
        {
            // Number of columns that will fit in the current configuration (use *actual* display width).
            Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int numColumns = display.getWidth() / this.mGridTileSize;
            return (numColumns > 0 ? numColumns : 1);
        }
        return 1;
    }

    // Just cause a repaint
    //    public void triggerDataSetChanged()
    //    {
    //        Handler handler = new Handler(mContext.getMainLooper());
    //        handler.post(new Runnable() {
    //
    //            @Override
    //            public void run() {
    //                ListAdapter.this.notifyDataSetChanged();
    //                ListAdapter.this.notifyDataSetInvalidated();
    //            }
    //        });
    //    }

    /**
     * Remove an item from the list
     */
    public void removeItem(int position)
    {
        ListManager.getInstance().removeItem(mContext, position, mFoldersOnly, mPhotosOnly);
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        mTotalCount = ListManager.getInstance().getCount(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
        int count = mTotalCount < 0 ? 0 : mTotalCount;
        if((mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST) &&
                ListManager.getInstance().getNextIndex(mContext, mFolder, mSearchText, mRecurse) != null)
        {
            count++;
        }
        return count;
    }
    

    @Override
    public Object getItem(int index)
    {
        Object item = null;
        if (index < mTotalCount)
        {
            item = ListManager.getInstance().getListItem(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse, index);
        }
        return item;
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }

    public void setDetailsVisible(boolean visible)
    {
        mDetailsVisible = visible;
    }

    /**
     * Enable the adapter. Returns true if a call to prepareList() was made.
     *
     * @return true is prepareList() was called, false otherwise.
     */
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
                ListManager.getInstance().prepareList(mContext, mFolder, mSearchText, mRecurse, null, this);
                return true;
            }
        }
        else
        {
            notifyDataSetChanged();
        }
        return false;
    }

    public synchronized void disable()
    {
        mEnabled = false;
        
        mThumbnailManager.clearQueue();
    }

    public boolean getDetailsVisible()
    {
        return mDetailsVisible;
    }

    public void increaseItems()
    {
        String nextIndex = ListManager.getInstance().getNextIndex(mContext, mFolder, mSearchText, mRecurse);
        if (nextIndex != null)
        {
            ListManager.getInstance().prepareList(mContext, mFolder, mSearchText, mRecurse, nextIndex, this);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;

        if(mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID)
        {
            view = getViewForActivityDirPhotoGrid(position, convertView);
        }
        else if(this.mActivityType == ListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW)
        {
            return getViewForActivityPhotoSlideShow(position, convertView,
                    parent);
        }
        else // this.activityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST
        {
            view = getViewForActivityDirFileList(position, convertView);
        }

        return view;
    }

    /**
     * @param position
     * @param convertView
     * @return
     */
    protected View getViewForActivityDirPhotoGrid(int position, View convertView) {
        View view;
        final CloudFile cloudFile = (CloudFile) ListManager.getInstance().getListItem(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse, position);

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
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
            
            
            bitmap = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);
           

            if(bitmap != null)
            {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                
                resizeBitmapIfNeeded(imageView, bitmap);
            }
            else
            {
                imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                
                imageView.setImageDrawable(FileUtils.getIcon(mContext,cloudFile, mRootDeviceTitle, mRootDeviceId, mDeviceEncrypted ));
            }
        }
        //return view;
        return view;
    }



    protected void resizeBitmapIfNeeded(ImageView imageView, Bitmap bitmap) {
        if(this.mGridTileSize - twoMargin > 240) // expand it to the intended size
        {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, (int) (this.mThumbnailWidth*resizeFactorForGridView), (int) (this.mThumbnailHeight*resizeFactorForGridView), false);
            imageView.setImageBitmap(resizedBitmap);
        }
        else
        {
            imageView.setImageBitmap(bitmap);
        }
    }

    /**
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
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
        Object listItem = ListManager.getInstance().getListItem(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse, position);
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
    protected View getViewForActivityDirFileList(int position, View convertView) {
        View view;
        if (position < mTotalCount)
        {
            Object listItem = ListManager.getInstance().getListItem(mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse, position);

            // Is this a 'title'?
            if(listItem instanceof String)
            {
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
            }
            else
            {
                if ((convertView != null) && (convertView.getId() == R.id.listItemLayout))
                {
                    view = convertView;
                }
                else
                {
                    LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(mResourceId, null);
                }
                ImageView imageView = (ImageView) view.findViewById(R.id.image);
                TextView textView = (TextView) view.findViewById(R.id.name);
                TextView detailsView = (TextView)view.findViewById(R.id.details);

                Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

                int displayWidth = display.getWidth();

                view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));

                CloudFile file = (CloudFile)listItem;
                if(file != null)
                {
                    String fileTitle =  file.getTitle();
                    
                    textView.setText(fileTitle);
                    
                    // imageView.setImageResource(file.getIconId());
                    //                      Bitmap bm = thumbnailManager.getThumbnail(position);

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

                    Bitmap bm = null;
                    
                    ThumbnailParams params = new ThumbnailParams(
                            mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, versionId, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
                    
                    bm = mThumbnailManager.getThumbnailNoEnqueue(getThumbnailPath(), name, bytesize, position, params);

                    if (bm != null)
                    {
                        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        imageView.setImageBitmap(bm);
                    }
                    else
                    {
                       
                        // OLD imageView.setImageResource(file.getIconId());
                        imageView.setImageDrawable(FileUtils.getIcon(mContext,file,mRootDeviceTitle, mRootDeviceId, mDeviceEncrypted)); // NEW
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
                    imageView.setImageResource(R.drawable.placeholder_icon_small);
                    detailsView.setVisibility(View.GONE);
                }
            }
        }
        else // position == totalCount, which means this is the 'get more data' item.
        {
            if ((convertView != null) && (convertView.getId() == R.id.listItemLayout))
            {
                view = convertView;
            }
            else
            {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(mResourceId, null);
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            TextView textView = (TextView) view.findViewById(R.id.name);
            TextView descriptionView = (TextView)view.findViewById(R.id.details);

            Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            int displayWidth = display.getWidth();

            view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));

            textView.setText(mContext.getResources().getString(R.string.next_items));
            imageView.setImageResource(R.drawable.placeholder_icon_small);
            descriptionView.setVisibility(View.GONE);
        }
        return view;
    }


    /**
     * Trigger download of thumbnails for the current "window", i.e. those
     * indexes we should try to keep in the cache (the currently visible ones
     * plus a couple extra in the direction in which we are moving).
     */
    protected void preload()
    {

        Vector<Integer> window = mSlidingWindow.getWindow();

       for(int position : window)
       {
           Object listItem = ListManager.getInstance().getListItem(
                mContext, mFolder, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse, position);
           
           if(listItem != null && listItem instanceof Photo)
           {
               long lastModified = ((Photo) listItem).getUpdated();
               String name = ((Photo) listItem).getTitle();
               long bytesize = ((Photo) listItem).getSize();
               long version = ((Photo) listItem).getVersionId();

               ThumbnailParams params = new ThumbnailParams(
                    mThumbnailWidth, mThumbnailHeight, mFolder, lastModified, version, mSearchText, mFoldersOnly, mPhotosOnly, mRecurse);
               
                   mThumbnailManager.getThumbnail(getThumbnailPath(), name, bytesize, position, params, null);
            }
        }
    }

    /**
     * Returns the path to submit in thumbnail requests. When in search mode,
     * the search text is added to the folder path, i.e. we create a unique
     * "folder" for a search result.
     *
     * @return The path to use when requesting thumbnails from the
     * ThumbnailManager
     */
    protected String getThumbnailPath()
    {
        if(mSearchText != null && mSearchText.length() > 0)
        {
            return mFolder + "_[SEARCH]_" + mSearchText;
        }
        return mFolder;
    }

    /**
     * Update the current viewport, i.e. which indexes in the folder are
     * currently visible.
     *
     * @param start The starting index
     * @param end The end index
     */
    public synchronized void setCurrentView(int start, int end)
    {
        /*
         * For when we don't have a valid 'end' number, typically when we
         * enter a folder and have not moved. Default to start loading the
         * top 15 items.
         *
         * As soon as the user scrolls the window, we'll get new calls with
         * valid start and end numbers.
         */
        int defaultWindowSize = 15;

        LogUtil.debug("ListAdapter", "NEW VIEW: " + start + " - " + end);

        if(end < 0)
        {
            LogUtil.debug("ListAdapter", "end < 0, trying to use count() instead");

            start = 0;
            end = getCount() > defaultWindowSize ? defaultWindowSize : getCount();

            LogUtil.debug("ListAdapter", "NEW VIEW (ADJUSTED): " + start + " - " + end);
        }

        if(end >= 0)
        {
            mSlidingWindow.newViewport(start, end);
            preload();
        }
    }

    public synchronized void setReferencePosition(int referencePosition)
    { }

    /**
     * Class that represents a "sliding window", i.e. the current viewport
     * plus some extra items as determied by how we are moving.
     */
    class SlidingWindow
    {
        private static final int DIRECTION_DOWN = 0;
        private static final int DIRECTION_UP = 1;

        private int mCurrentDirection = DIRECTION_DOWN;
        private int mLastKnownStart;
        private int mLastKnownEnd;
        private int mWindowSide;

        public SlidingWindow(int activityType)
        {
            if(activityType == ACTIVITY_TYPE_PHOTO_SLIDE_SHOW)
            {
                mWindowSide = 2;
            }
            else
            {
                mWindowSide = 3;
            }
            mLastKnownStart = 0;
        }

        public void newViewport(int start, int end)
        {
            mCurrentDirection = start >= mLastKnownStart ? DIRECTION_DOWN : DIRECTION_UP;
            mLastKnownStart = start;
            mLastKnownEnd = end;

            LogUtil.debug("ListAdapter", "DIRECTION: " + (mCurrentDirection == DIRECTION_DOWN ? "DOWN" : "UP") + ", WINDOW: " + start + "-" + end);
        }

        public Vector<Integer> getWindow()
        {
            Vector<Integer> window = new Vector<Integer>();
            boolean downwards = mCurrentDirection == DIRECTION_DOWN;

            LogUtil.debug("ListAdapter", "getWindow(): COUNT: " + mTotalCount);

            if(downwards)
            {
                int from = mLastKnownStart;
                int to = mLastKnownEnd + mWindowSide;

                for(int index = from; index <= to; index += 1)
                {
                    if(index >= 0 && index < mTotalCount)
                    {
                        window.add(index);
                    }
                }
            }
            else
            {
                int from = mLastKnownEnd;
                int to = mLastKnownStart - mWindowSide;

                for(int index = from; index >= to; index -= 1)
                {
                    if(index >= 0 && index < mTotalCount)
                    {
                        window.add(index);
                    }
                }
            }

            String str = "";
            for(int position : window)
            {
                str += position + ",";
            }
            LogUtil.debug("ListAdapter", "FINAL WINDOW: " + str);

            return window;
        }
    }



    // Implementation of the 'ListManager.Listener' interface
    // This method will be called immediately after the list of CloudFiles is returned from the
    // server, but before the list is used (saved to cache, trigger thumbnail downloads etc).
    // This allows a place for the list to be modified at the last moment.
    public void preprocessList(ArrayList<Object> list)
    {
        synchronized (this)
        {
          
            if(list != null  && list.size() > 0 && (this.mResourceId == R.layout.list_item_layout && 
                    (this.mSearchText == null || (this.mSearchText != null && (this.mSearchText == "")))))
            {
                int lastIndex = list.size() - 1;
                
                // Traverse the list from last down to first.
                for (int i = (lastIndex - 1); i >=0; --i)
                {
                    CloudFile file = (CloudFile)list.get(i);

                    if(UploadManager.uploadlastFile != null &&
                            ((CloudFile) file).getName().equalsIgnoreCase(UploadManager.uploadlastFile.getName()))
                    {
                        lastUploadPositionInDirFileList = i;
                    }
                }
                
                
            }
            // If we are doing a search in the DirFileList activity, then we are supposed to display
            // a title whenever there is a transition between directories in the list. To do that we
            // have to add objects that represent the titles in the appropriate places in the list.
            if ((this.mResourceId == R.layout.list_item_layout 
                    || this.mResourceId == R.layout.photo_list_item_layout) 
                    && (this.mSearchText != null) && (this.mSearchText != ""))
            {
                int lastIndex = list.size() - 1;

                if (lastIndex >= 0)
                {
                    CloudFile file = (CloudFile)list.get(lastIndex);

                    // The 'path' is the directory without the filename.
                    String lastPath = file.getPath();
                    String currentPath = null;

                    // Traverse the list from last down to first.
                    for (int i = (lastIndex - 1); i >=0; --i)
                    {
                        file = (CloudFile)list.get(i);
                        currentPath = file.getPath();

                        // If the path has changed, then we need to insert a 'title'(String) object into the list.
                        if (!currentPath.equals(lastPath))
                        {
                            addPathLabelItem(list, i + 1, lastPath);
                            lastPath = currentPath;
                        }
                    }
                    // Always have to add the initial title
                    addPathLabelItem(list, 0, lastPath);
                }
            }
        } // synchronized (this)
    }

    private void addPathLabelItem(ArrayList<Object> list, int index, String path)
    {
        String fullPath = path;
        // Trim off ending / or \
        if ((fullPath.endsWith("\\") && (!(fullPath.equalsIgnoreCase("\\")))) 
                || (fullPath.endsWith("/") && !(fullPath.equalsIgnoreCase("/"))))
        {            
            fullPath = fullPath.substring(0, fullPath.length() - 1);
        }
                 

        // Build up a string that contains the parent folder as the "root". This means
        // throwing out the parent's parent path from the string.
        int prePathLength = (mSearchDirectory == null ? 0 : mSearchDirectory.length());
        
        String pathStr = new String(fullPath.substring(prePathLength, fullPath.length()));
        
        // Prefix "Sync" to the dir path for Sync
        if(SystemState.isSync(mContext, this.mRootDeviceTitle))
        {
            if(path.startsWith("\\") || path.startsWith("/"))
            {
                pathStr = mContext.getResources().getString(R.string.sync_title) + pathStr;
            }
            else
            {
                // it is a directory 
                pathStr = mContext.getResources().getString(R.string.sync_title) + "/" + pathStr;
            }
        }
           
             
        list.add(index, pathStr);
    }

    // Implementation of the 'ListManager.Listener' interface
    public void onListPrepared(boolean success, int errorCode, String nextStartIndex,
            String dirLink, String searchText)
    {
        this.mErrorCode = errorCode;
        
        mTotalCount = ListManager.getInstance().getCount(mContext, this.mFolder, this.mSearchText, this.mFoldersOnly, this.mPhotosOnly, this.mRecurse);
        synchronized (this) {
            if (mListAdapterDataListener != null) {
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

    public synchronized void registerListener(ListAdapterDataListener listener) {
        this.mListAdapterDataListener = listener;
    }

    public synchronized void unregisterListener() {
        this.mListAdapterDataListener = null;
    }

    public void clean(boolean emptyCache) {
        if (emptyCache) {
            //          thumbnailManager.clean();
        }
        ListManager.getInstance().cleanUp(mContext, emptyCache);
    }

    // Is this the 'get more data' item artificially added to the DirFileList if there is more data to get from the server.
    public boolean isNextItem(int position)
    {
        if ((this.mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST) && (position == 0 && mTotalCount <= 0) || (position == mTotalCount && mTotalCount > 0))
        {
            return true;
        }
        return false;
    }

    public boolean isNextPossible(int lastPosition, boolean nextInList) {
        if ((this.mActivityType == ListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST) || !nextInList)
        {
            if (lastPosition >= mTotalCount - 1 && ListManager.getInstance().getNextIndex(mContext, mFolder, mSearchText, mRecurse) != null)
            {
                return true;
            }
        }
        return false;
    }

    // This method takes a position and calculates a new one by removing the count of all non-photos before the position.
    // The position returned is the position in a list of only photos.
    // It assumes that there is already a cache set-up in the database
    // Position is zero based, so subtract one from the count
    public int getPhotosPosition(int position)
    {
        return (ListManager.getInstance().getPhotoCount(mContext, position) - 1);
    }

    protected void fireThumbnailAvailable(int position)
    {
        if(mThumbnailAvailabilityListener != null)
        {
            mThumbnailAvailabilityListener.onThumbnailAvailable(position);
        }
    }

    public interface ThumbnailAvailabilityListener
    {
        public void onThumbnailAvailable(int position);
    }

    static class ViewHolderPhotoFullscreen {
        ImageView photo_fullscreen_img;
        ViewGroup photo_details_layout;
        TextView photo_details_headline;
        TextView photo_detail_label_filename;
        TextView photo_detail_filename;
        TextView photo_detail_label_resolution;
        TextView photo_detail_resolution;
        TextView photo_detail_label_kbsize;
        TextView photo_detail_kbsize;
        TextView photo_detail_label_taken_date;
        TextView photo_detail_taken_date;
        TextView photo_detail_label_camera_manufacturer;
        TextView photo_detail_camera_manufacturer;
        TextView photo_detail_camera_model;
    }

    public void notifyScrollStateChanged(int scrollState)
    {
           switch(scrollState)
           {
               case OnScrollListener.SCROLL_STATE_IDLE:
                  mThumbnailManager.setScrolling(false);   
                   notifyDataSetChanged(); // Fire a notification to start loading thumbnails
                   
                   break;
               case OnScrollListener.SCROLL_STATE_FLING:
               case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                      mThumbnailManager.setScrolling(true);
                    break;
           }
    }

    @Override
    public boolean enabled()
    {
        return mEnabled;
    }
}
