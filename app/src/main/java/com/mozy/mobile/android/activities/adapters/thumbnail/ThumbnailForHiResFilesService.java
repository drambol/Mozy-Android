package com.mozy.mobile.android.activities.adapters.thumbnail;

import java.util.concurrent.Future;

import android.content.Context;
import android.graphics.Bitmap;

import com.mozy.mobile.android.activities.ErrorCodes;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.SystemState;

/**
 *
 * Requests are submitted to an ExecutorService and notifications
 * containing the original request and the compressed bitmap (if any) are
 * sent to registered listeners upon completion.
 */
public class ThumbnailForHiResFilesService extends ThumbnailDownloadService
{
    //private static final String TAG = ThumbnailEncryptedFilesService.class.getSimpleName();
    
    private String mDeviceId;
    private boolean mEncrypted;

    /**
     * Creates a worker with the supplied cache
     *
     * @param thumbnailCache The cache to use
     */
    public ThumbnailForHiResFilesService(Context context, String deviceId,boolean isEncrypted, int nbrWorkers)
    {
        super(context, nbrWorkers);
        mDeviceId = deviceId;
        mEncrypted = isEncrypted;
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
                Future<?> requestHandle = mWorkerPool.submit(new ThumbnailHiResProcessJob(request,mDeviceId, mEncrypted));
                mRequestHandles.put(request, requestHandle);
            }
        }
    }


    /**
     * Wraps a thumbnail request and executes it.
     */
    public class ThumbnailHiResProcessJob implements Runnable
    {
        /**
         * The request
         */
        private ThumbnailRequest mRequest;
        
        private String mDeviceId;
        
        private String mPath;
        
        boolean mEncrypted;

        /**
         * Creates a job from a request
         *
         * @param request The request to execute
         */
        public ThumbnailHiResProcessJob(ThumbnailRequest request, String deviceId,  boolean isEncrypted)
        {
            mRequest = request;
            mDeviceId = deviceId;
            mPath = request.getPath();
            mEncrypted = isEncrypted;
            
        }

        public void run()
        {
            ThumbnailParams params = mRequest.getParams();

            /*
             * Bitmap to be returned
             */
            Bitmap bitmap = null;
            
            LocalFile localFile = new LocalFile(mPath);
            
            /*
             * No thumbnail required if item is a String
             */
            if(FileUtils.isFilePhoto(localFile))
            {
               if(mEncrypted == true  && SystemState.isLocalFileDecrypted(localFile, null, mDeviceId)
                       || mEncrypted == false)
               {
                  bitmap = compressDecryptedPhoto(localFile, mDeviceId, params);
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
