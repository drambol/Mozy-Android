package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.adapters.ListManager;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.containers.ThumbnailDownload;

/**
 * Class for executing downloads as specified by <code>ThumbnailRequest</code>s
 *
 * Requests are submitted to an ExecutorService and notifications
 * containing the original request and the downloaded bitmap (if any) are
 * sent to registered listeners upon download completion.
 */
public class ThumbnailDownloadService
{
    private static final String TAG = ThumbnailDownloadService.class.getSimpleName();

    /**
     * Table of live requests and their Future<?> handles. Used for
     * accessing already enqueued requests, e.g. to cancel them.
     */
    protected Hashtable<ThumbnailRequest, Future<?>> mRequestHandles;

    /**
     * Listeners that will receive updates when requests complete
     */
    private Vector<ThumbnailWorkerListener> mListeners;

    /**
     * The application environment
     */
    protected Context mContext;

    /**
     * Worker pool
     */
    protected final ExecutorService mWorkerPool;

    /**
     * Creates a worker with the supplied cache
     *
     * @param thumbnailCache The cache to use
     */
    public ThumbnailDownloadService(Context context, int nbrWorkers)
    {
        mContext = context;
        mListeners = new Vector<ThumbnailWorkerListener>();
        mWorkerPool = Executors.newFixedThreadPool(nbrWorkers);
        mRequestHandles = new Hashtable<ThumbnailRequest, Future<?>>();
    }

    /**
     * Enqueue a download request. If the request has already been submitted
     * the new one will be ignored (nothing enqueued).
     *
     * @param request The request to enqueue
     */
    
    public void postRequest(ThumbnailRequest request, Object item)
    {
        synchronized(mRequestHandles)
        {
            if(!mRequestHandles.containsKey(request))
            {
                Future<?> requestHandle = mWorkerPool.submit(new ThumbnailDownloadJob(request, item));
                mRequestHandles.put(request, requestHandle);
            }
        }
    }

    /**
     * Clear the request queue. Useful when enqueued requests become obsolete
     * as a result of the user leaving the view in which the thumbnails would
     * be used, for example.
     */
    public void clearQueue()
    {
        synchronized(mRequestHandles)
        {
            for(Future<?> future : mRequestHandles.values())
            {
                future.cancel(false);
            }

            int done = 0, notDone = 0;
            for(Future<?> future : mRequestHandles.values())
            {
                if(future.isDone())
                {
                    done++;
                }
                else
                {
                    notDone++;
                }
            }

            LogUtil.debug(TAG, "[CLEARING QUEUE] " + done + " done, " + notDone + " not done.");

            mRequestHandles.clear();
        }
    }

    /**
     * Adds a listener
     *
     * @param listener The listener
     */
    public void addThumbnailWorkerListener(ThumbnailWorkerListener listener)
    {
        mListeners.add(listener);
    }

    /**
     * Notifies listeners that the download request has completed
     *
     * @param request The request
     * @param bitmap The bitmap that was download, if any (else null)
     */
    void fireRequestComplete(ThumbnailRequest request, ThumbnailBitmap bitmap, int errorCode)
    {
        for(ThumbnailWorkerListener listener : mListeners)
        {
            listener.onRequestComplete(request, bitmap, errorCode);
        }
    }
    
    
    /**
     * @param params
     * @param cloudFile
     */
    public Bitmap compressDecryptedPhoto(LocalFile localFile, String rootDeviceId, ThumbnailParams params) 
    {
        Bitmap bm = null;

        try {
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;

            FileInputStream fis = new FileInputStream(localFile.file);
            BitmapFactory.decodeStream(fis, null, o);
            fis.close();

            int scale = 1;
            if (o.outHeight > params.mHeight || o.outWidth > params.mWidth) {
                
                double ratio = Math.log(((double)  Math.min(params.mHeight,  params.mWidth)  / (double) Math.max(o.outHeight, o.outWidth)));
                
                int scalePow = (int) Math.round( ratio / Math.log(0.5));
                scale = (int) Math.pow(2.0,scalePow);
                LogUtil.debug(this, "compressDecryptedPhoto Scale:" + scale + "scalePow: " + scalePow );
            }

            //Decode with inSampleSize
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = scale;
            
            LogUtil.debug(this, "compressDecryptedPhoto thumbnail Size:" + params.mHeight + "X" + params.mWidth );
            LogUtil.debug(this, "compressDecryptedPhoto Option InSampleSize:" + options.inSampleSize);
            
            fis = new FileInputStream(localFile.file);
            bm = BitmapFactory.decodeStream(fis, null, options);
            fis.close();
            
            
        } catch (FileNotFoundException e) 
        {
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }                            


        return bm;
    }


    /**
     * Interface for download worker listeners to implement
     */
    public static interface ThumbnailWorkerListener
    {
        public void onRequestComplete(ThumbnailRequest request, ThumbnailBitmap bitmap, int errorCode);
    }

    /**
     * Wraps a thumbnail request and executes it.
     */
    public class ThumbnailDownloadJob implements Runnable
    {
        /**
         * The request
         */
        private ThumbnailRequest mRequest;
        
        private Object mItem;

        /**
         * Creates a job from a request
         *
         * @param request The request to execute
         */
        public ThumbnailDownloadJob(ThumbnailRequest request, Object item)
        {
            mRequest = request;
            mItem = item;
        }

        public void run()
        {
            ThumbnailParams params = mRequest.getParams();

            /*
             * Bitmap to be returned
             */
            Bitmap bitmap = null;
            int errorcode = ErrorCodes.NO_ERROR;

            /*
             * Get the file item from server
             */
            
            if(mItem == null)
            {
                mItem = ListManager.getInstance().getListItem(
                        mContext,
                        params.mFolder,
                        params.mSearchText,
                        params.mDirsOnly,
                        params.mPhotosOnly,
                        params.mRecurse,
                        mRequest.getIndex());
            }

            boolean download = false;

            /*
             * No thumbnail required if item is a String
             */
            if(!(mItem instanceof String))
            {
                CloudFile file = (CloudFile) mItem;
                if(file != null)
                {
                    if(file.getCategory() == FileUtils.CATEGORY_PHOTOS)
                    {
                        download = true;

                        ThumbnailDownloadTask task = new ThumbnailDownloadTask(
                                mContext,
                                file.getLink(),
                                params.mWidth,
                                params.mHeight,
                                mRequest.getIndex(),
                                params.mLastModified,
                                params.mVersionId);

                        ThumbnailDownload thumbnailDownload = task.doThumbnailDownload();
                        errorcode = thumbnailDownload.errorCode;
                        
                        if(errorcode == ErrorCodes.NO_ERROR)
                        {
                            bitmap = thumbnailDownload.getBitmap();
                        }
                    }
                    else
                    {
                        /* From old code */
                        // TODO what to do with categories?
                        // Need to call file.getIconId(), and load the resource with that id and create a bitmap.
                        //taskListener.onThumbnailDownloaded(file.getLink(), 0, 0, downloadPos, null);
                    }
                }
            }

            synchronized(mRequestHandles)
            {
                mRequestHandles.remove(mRequest);

                if(download)
                {
                    LogUtil.debug(TAG, "[REQUEST COMPLETED] (Live: " + mRequestHandles.size() + ") " + mRequest.getCacheKey().getKey());
                }
            }

            ThumbnailBitmap result = bitmap != null ? new ThumbnailBitmap(bitmap, params.mLastModified) : null;
            fireRequestComplete(mRequest, result, errorcode);
        }
    }
}
