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
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.DirPhotoGridActivity;
import com.mozy.mobile.android.activities.adapters.ListAdapter.ViewHolderPhotoFullscreen;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailDownloadService;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailForHiResFilesService;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManager;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManagerForHiResFiles;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailParams;
import com.mozy.mobile.android.activities.adapters.thumbnail.ThumbnailManager.ThumbnailManagerListener;
import com.mozy.mobile.android.activities.tasks.GetDownloadedFileListTask;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.views.SlideView;


public class DownloadListAdapter extends ArrayAdapter<Object> implements ThumbnailManagerListener
{
    private static final int NUMBER_OF_COLUMNS = DirPhotoGridActivity.NUM_COLUMNS;
    
    /**
     * Interface to be implemented by clients interested in listening for
     * data updates from this adapter
     */
    public static interface ListAdapterDataListener
    {
        void onDataRetrieved(DownloadListAdapter callingAdapter);
    }
    
    ArrayList<Object> adapterLists;

    /**
     * Activity types
     */
    public static final int ACTIVITY_TYPE_DIR_FILE_LIST = 0;
    public static final int ACTIVITY_TYPE_DIR_PHOTO_GRID = 1;
    public static final int ACTIVITY_TYPE_PHOTO_SLIDE_SHOW = 2;
    
    
    private static final int MAX_THUMBNAIL_SIZE = 512;

    /**
     * The activity type that is usgin this adapter for its data
     */
    private int mActivityType = -1;

    private final Context mContext;

    private final boolean mDeviceEncrypted;
    
    private final String mRootDeviceId;
    private final String mRootDeviceTitle;
    
    /**
     * The thumbnail download manager that handles download and caching of
     * thumbnails
     */
    private ThumbnailManager mThumbnailManager = null;

    // If this.dirsOnly and this.photosOnly are both true, then both types, and only both types,
    // are included in the list.
    /**
     * Whether to display only folders
     */
    private final boolean mFoldersOnly;

    /**
     * Whether to display only photos
     */
    private final boolean mPhotosOnly;


    /**
     * Resource ID??
     */
    private final int mResourceId;

    /**
     * Width of display
     */
    private final int mDisplayWidth;

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
    private ThumbnailAvailabilityListener mThumbnailAvailabilityListener;
    
    private ListAdapterDataListener mListAdapterDataListener;
    
    /**
     * ErrorCode from getFilelistTask
     */
    private int mErrorCode;;

    public int getErrorCode() {
        return mErrorCode;
    }

    private boolean mDetailsVisible;

    /**
     * Whether the adapter is enabled
     */
    private boolean mEnabled;

    /**
     * Size of thumbnails in the current list view
     */
    private int mThumbnailWidth;

    /**
     * Size of thumbnails in the current list view
     */
    private int mThumbnailHeight;

    /**
     * Size (assumes square) of a grid tile. Includes padding and border
     * but not extra margin.
     */
    private int mGridTileSize;

    /**
     * Dip height of grid elements
     */
    private static final float GRID_ITEM_HEIGHT_IN_DIP = 35.0f;

    /**
     * Dip height of grid elements
     */
    private static final float GRID_ITEM_WIDTH_IN_DIP = 35.0f;
    
    /**
     * Touch movement
     */
    private float mDownX;

    /**
     * Touch movement
     */
    private float mDownY;

  
    /**
     * Item count from server
     */
    private int mTotalCount;
    

    private SlidingWindow mSlidingWindow;

    public DownloadListAdapter(Context context,
            int resourceId,
            final boolean encryptedContainer,
            final boolean directoriesOnly,
            final boolean photosOnly,
            int inputActivityType,
            ListAdapterDataListener listener,
            ThumbnailAvailabilityListener thumbListener,
            String rootDeviceId,
            String rootDeviceTitle)
    {
        super(context, resourceId);
        mContext = context.getApplicationContext();
        mDeviceEncrypted = encryptedContainer;
        mFoldersOnly = directoriesOnly;
        mPhotosOnly = photosOnly;
        mResourceId = resourceId;
        mActivityType = inputActivityType;
        mThumbnailAvailabilityListener = thumbListener;
        mListAdapterDataListener = listener;

        mSlidingWindow = new SlidingWindow(mActivityType);
        
        mRootDeviceId = rootDeviceId;
        mRootDeviceTitle = rootDeviceTitle;

        /*
         * Setup thumbnail manager
         */
        
        ThumbnailDownloadService service  = new ThumbnailForHiResFilesService(context, mRootDeviceId, mDeviceEncrypted, 5);
        mThumbnailManager = ThumbnailManagerForHiResFiles.getInstance( context, service);
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

        if(mActivityType == DownloadListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW)
        {
            /*
             * Get a preview big enough to fill up the screen in either orientation.
             */
            mThumbnailWidth = mThumbnailHeight = mDisplayHeight;
        }
        else if(mActivityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST)
        {
            /*
             * Thumbnail dips to pixels calculation
             */
            mThumbnailHeight = (int) (GRID_ITEM_HEIGHT_IN_DIP * scale + 0.5f);
            mThumbnailWidth = (int) (GRID_ITEM_WIDTH_IN_DIP * scale + 0.5f);
        }
        else // mActivityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID
        {
            /*
             * Want to get a size that will fit NUMBER_OF_COLUMNS on the *short* side (displayWidth)
             */
            int twoMargin = (2 * (int)(DirPhotoGridActivity.GRID_MARGIN * scale + 0.5f));
            mGridTileSize = (mDisplayWidth - twoMargin) / NUMBER_OF_COLUMNS;
            mThumbnailHeight = this.mThumbnailWidth = this.mGridTileSize - twoMargin;
        }

        mDetailsVisible = false;
        mEnabled = true;

        if(mActivityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST)
        {
            prepareInitialList(false);
        }
        else
        {
            prepareInitialList(true);
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
        if(mActivityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID)
        {
            // Number of columns that will fit in the current configuration (use *actual* display width).
            Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int numColumns = display.getWidth() / this.mGridTileSize;
            return (numColumns > 0 ? numColumns : 1);
        }
        return 1;
    }


    @Override
    public int getCount()
    {     
        mTotalCount = 0;
        if(adapterLists != null)
            mTotalCount = adapterLists.size();
         return mTotalCount;
    }

    @Override
    public long getItemId(int position)
    {
        return 0;
    }
    
    
    @Override
    public Object getItem(int position)
    {
        if(adapterLists != null)
            return adapterLists.get(position);
        else
            return null;
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

    public void prepareInitialList(boolean photosOnly)
    {
        this.clear();
        
        new GetDownloadedFileListTask(this.mContext, this.mRootDeviceTitle, photosOnly,  new GetDownloadedFileListTask.Listener() 
        {
            @Override
            public void onCompleted(ArrayList<Object> list) 
            {
                adapterLists = list;
                DownloadListAdapter.this.add(adapterLists);
                DownloadListAdapter.this.mListAdapterDataListener.onDataRetrieved(DownloadListAdapter.this);
                notifyDataSetChanged();                
            }
        }).execute();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = null;
        
        if(this.mActivityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_PHOTO_GRID)
        {
            view = getViewForActivityDirPhotoGrid(position, convertView);
        }
        else if(this.mActivityType == DownloadListAdapter.ACTIVITY_TYPE_PHOTO_SLIDE_SHOW)
        {
            return getViewForActivityPhotoSlideShow(position, convertView,
                    parent);
        }
        else
        {
            // this.activityType == DownloadListAdapter.ACTIVITY_TYPE_DIR_FILE_LIST
            view = getViewForActivityDirFileList(position, convertView);
        }

        return view;
    }


    /**
     * @param position
     * @param convertView
     * @return
     */
    private View getViewForActivityDirFileList(int position, View convertView) {
        View view = null;
        if (position < mTotalCount)
        {
            Object listItem = this.getItem(position);
            
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
            else if(listItem instanceof LocalFile)
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
                
                LocalFile file = (LocalFile) listItem;
                
                if(file != null)
                {
                    
                    String fileTitle =  file.getName();
                    
                    textView.setText(fileTitle);
                    
                    long versionId = 0;
                    long lastModified = 0;
                    long bytesize = System.currentTimeMillis(); // Pick something that doesn't collide
                    String name = String.valueOf(System.currentTimeMillis()); // Pick something that doesn't collide
                    if(file != null)
                    {              
                      lastModified = file.getUpdated();
                      name = file.getName();
                      bytesize = file.getSize();
                      versionId = 0;
                    }

                    Bitmap bm = null;
                    
                    ThumbnailParams params = new ThumbnailParams(
                            mThumbnailWidth, mThumbnailHeight, null, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, false);
                    
                    bm = mThumbnailManager.getThumbnailNoEnqueue(file.getPath(), name, bytesize, position, params);

                    if (bm != null)
                    {
                        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                        imageView.setImageBitmap(bm);
                    }
                    else
                    {
                      imageView.setImageDrawable(FileUtils.getIcon(mContext, file, this.mRootDeviceId, this.mDeviceEncrypted)); // NEW
                    }

                   String  details = FileUtils.getDetails(mContext, file.getUpdated(), file.getSize());

                   detailsView.setText(details);
                   detailsView.setVisibility(details.length() == 0 ? View.GONE : View.VISIBLE);
                }
            }
        }
        return view;
    }

    
    
    /**
     * @param position
     * @param convertView
     * @return
     */
    private View getViewForActivityDirPhotoGrid(int position, View convertView) {
        
        View view = null;
        
        Object listItem = this.getItem(position);
        
        if(listItem instanceof String)
        {
  
        }
        else if(listItem instanceof LocalFile )  // TODO Check for photo
        {
            LocalFile localFile = (LocalFile) listItem;
            
            if(listItem instanceof Directory)
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
                directoryNameView.setText(localFile.getName());
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
                if(localFile != null)
                {
                    lastModified = localFile.getUpdated();
                    name = localFile.getName();
                    bytesize = localFile.getSize();
                    versionId = 0;
                }

                /*
                 * Try to get thumbnail bitmap
                 */
                
                Bitmap bitmap = null;
                
                ThumbnailParams params = new ThumbnailParams(
                        mThumbnailWidth, mThumbnailHeight, null, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, false);
                
                
                bitmap = mThumbnailManager.getThumbnailNoEnqueue(localFile.getPath(), name, bytesize, position, params);
               
    
                if(bitmap != null)
                {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageBitmap(bitmap);
                }
                else
                {
                    imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    imageView.setImageDrawable(FileUtils.getIcon(mContext, localFile, this.mRootDeviceId, this.mDeviceEncrypted));
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
    private View getViewForActivityPhotoSlideShow(int position,
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

        Object listItem = this.getItem(position);
        
        LocalFile localFile = (LocalFile) listItem;

        if(listItem instanceof String)
        {
            LogUtil.error(this, "String object received in slideShow mode!!");
        }
        else
        {
            if(localFile != null)
            {
                String stringFilename = localFile.getName();

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
            lastModified = localFile.getUpdated();
            name = localFile.getName();
            bytesize = localFile.getSize();
            versionId = 0;
        }

        /*
         * Try to get thumbnail
         */
        
        
        Bitmap bitmap = null;

        ThumbnailParams params = new ThumbnailParams(
                mThumbnailWidth, mThumbnailHeight, null, lastModified, versionId, null, mFoldersOnly, mPhotosOnly, false);
        

        if (null != localFile)
            bitmap = mThumbnailManager.getThumbnailNoEnqueue(localFile.getPath(), name, bytesize, position, params);

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
            mThumbnailManager.getThumbnail(localFile.getPath(), name, bytesize, position, params, null);
            LogUtil.debug(this, "Requesting thumbnail again as not in the cache!");
            if (localFile != null)
            {
                holder.photo_fullscreen_img.setImageDrawable(FileUtils.getIcon(mContext, localFile, this.mRootDeviceId, this.mDeviceEncrypted));
            }
        }
        
        holder.photo_fullscreen_img.setScaleType(scaleSetting);
        return convertView;
    }
    
    public synchronized void registerListener(ListAdapterDataListener listener) {
        this.mListAdapterDataListener = listener;
    }

    public synchronized void unregisterListener() {
        this.mListAdapterDataListener = null;
    }


    /**
     * Trigger download of thumbnails for the current "window", i.e. those
     * indexes we should try to keep in the cache (the currently visible ones
     * plus a couple extra in the direction in which we are moving).
     */
    private void preload()
    {

        Vector<Integer> window = mSlidingWindow.getWindow();

       for(int position : window)
       {
           Object listItem = this.getItem(position);
           
           if(listItem instanceof LocalFile)
           {
               LocalFile file = (LocalFile) listItem;
   
               if( FileUtils.isFilePhoto(file) == true)
               {
                   long lastModified = file.getUpdated();
                   String name = file.getName();
                   long bytesize = file.getSize();
                   long version = 0;
    
                   ThumbnailParams params = new ThumbnailParams(
                        mThumbnailWidth, mThumbnailHeight, null, lastModified, version, null, mFoldersOnly, mPhotosOnly, false);
                   
                       mThumbnailManager.getThumbnail(file.getPath(), name, bytesize, position, params, null);
                }
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
//    private String getThumbnailPath()
//    {
//        return null;
//    }

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

        LogUtil.debug("DownloadListAdapter", "NEW VIEW: " + start + " - " + end);

        if(end < 0)
        {
            LogUtil.debug("DownloadListAdapter", "end < 0, trying to use count() instead");

            start = 0;
            end = getCount() > defaultWindowSize ? defaultWindowSize : getCount();

            LogUtil.debug("DownloadListAdapter", "NEW VIEW (ADJUSTED): " + start + " - " + end);
        }

        if(end >= 0)
        {
            mSlidingWindow.newViewport(start, end);
            preload();
        }
    }


    /**
     * Class that represents a "sliding window", i.e. the current viewport
     * plus some extra items as determied by how we are moving.
     */
    private class SlidingWindow
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

            LogUtil.debug("DownloadListAdapter", "DIRECTION: " + (mCurrentDirection == DIRECTION_DOWN ? "DOWN" : "UP") + ", WINDOW: " + start + "-" + end);
        }

        public Vector<Integer> getWindow()
        {
            Vector<Integer> window = new Vector<Integer>();
            boolean downwards = mCurrentDirection == DIRECTION_DOWN;

            LogUtil.debug("DownloadListAdapter", "getWindow(): COUNT: " + mTotalCount);

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
            LogUtil.debug("DownloadListAdapter", "FINAL WINDOW: " + str);

            return window;
        }
    }

 

    private void fireThumbnailAvailable(int position)
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
}
