package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.activities.adapters.ListManager;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.SystemState;

/**
 *
 * Requests are submitted to an ExecutorService and notifications
 * containing the original request and the compressed bitmap (if any) are
 * sent to registered listeners upon completion.
 */
public class ThumbnailEncryptedFilesService extends ThumbnailDownloadService
{
    //private static final String TAG = ThumbnailEncryptedFilesService.class.getSimpleName();
    
    private String mDeviceId;
    private String mDeviceTitle;

    /**
     * Creates a worker with the supplied cache
     *
     * @param thumbnailCache The cache to use
     */
    public ThumbnailEncryptedFilesService(Context context, String deviceId, int nbrWorkers)
    {
        super(context, nbrWorkers);
        mDeviceId = deviceId;
        mDeviceTitle = SystemState.getTitleForDevice(mDeviceId);
    }

    /**
     * Enqueue a download request. If the request has already been submitted
     * the new one will be ignored (nothing enqueued).
     *
     * @param request The request to enqueue
     */
    @Override
    public void postRequest(ThumbnailRequest request, Object item)
    {
        synchronized(mRequestHandles)
        {
            if(!mRequestHandles.containsKey(request))
            {
                Future<?> requestHandle = mWorkerPool.submit(new ThumbnailProcessJob(request,mDeviceId));
                mRequestHandles.put(request, requestHandle);
            }
        }
    }


    /**
     * Wraps a thumbnail request and executes it.
     */
    public class ThumbnailProcessJob implements Runnable
    {
        /**
         * The request
         */
        private ThumbnailRequest mRequest;
        
        private String mDeviceId;

        /**
         * Creates a job from a request
         *
         * @param request The request to execute
         */
        public ThumbnailProcessJob(ThumbnailRequest request, String deviceId)
        {
            mRequest = request;
            mDeviceId = deviceId;
        }

        public void run()
        {
            ThumbnailParams params = mRequest.getParams();

            /*
             * Bitmap to be returned
             */
            Bitmap bitmap = null;

            /*
             * Get the file item from server
             */
            Object item = ListManager.getInstance().getListItem(
                    mContext,
                    params.mFolder,
                    params.mSearchText,
                    params.mDirsOnly,
                    params.mPhotosOnly,
                    params.mRecurse,
                    mRequest.getIndex());

            /*
             * No thumbnail required if item is a String
             */
            if(!(item instanceof String))
            {
                CloudFile cloudFile = (CloudFile) item;
                
                if(cloudFile != null)
                {     
                    if(cloudFile.getCategory() == FileUtils.CATEGORY_PHOTOS)
                    {
                        
                      final LocalFile localFile  = FileUtils.getLocalFileForCloudFile(mContext, mDeviceTitle, cloudFile);
                       if(SystemState.isLocalFileDecrypted(localFile, cloudFile, mDeviceId))
                       {
                                bitmap = compressDecryptedPhoto(localFile, mDeviceId, params);
                       }
                    }
                 }
            }

            synchronized(mRequestHandles)
            {
                mRequestHandles.remove(mRequest);
            }

            ThumbnailBitmap result = bitmap != null ? new ThumbnailBitmap(bitmap, params.mLastModified) : null;
            
            fireRequestComplete(mRequest, result, ErrorCodes.NO_ERROR);
        }
    }
}
