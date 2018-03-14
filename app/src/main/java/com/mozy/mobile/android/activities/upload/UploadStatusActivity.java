package com.mozy.mobile.android.activities.upload;

import java.util.Locale;
import java.util.Vector;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.catch_release.CRResultCodes;
import com.mozy.mobile.android.catch_release.queue.Queue;

import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.web.uploadFileAPI;


public class UploadStatusActivity extends SecuredActivity
{
    private int sizePendingFiles = 0;
    private int estimatedTimeToUpload = -1; // -1 indicates phone not connected

    // A couple of transfer rates. These are nonsense numbers because we do not control the networks.
    // The transfer rates will be whatever the carriers, or local WiFi networks allow/can handle. Which you
    // can bet will be lower than these.
    private final static int WIFI_KILO_BYTE_PER_SECOND = 550;
    private final static int THREE_G_KILO_BYTE_PER_SECOND = 240;
    /**
     * Adapters
     */
    private AboutViewListAdapter aboutAdapter;
    
    private ResponseReceiver receiver;
    
    private ConnectivityManager cm;
    private NetworkInfo info;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Provisioning provisioning = Provisioning.getInstance(getApplicationContext());
        if((provisioning.getMipAccountToken() != null && provisioning.getMipAccountToken().compareTo("") == 0) 
                && (provisioning.getMipAccountTokenSecret() != null && provisioning.getMipAccountTokenSecret().compareTo("") == 0))  // handles selection of uploaded file from notification when signed out from Mozy
        { 
            // We are signed out already
            finish();
        }
        else
        {
            setContentView(R.layout.settings_layout);
    
            setBarTitle(R.string.settings_main_item_title_status);
            
            receiver = new ResponseReceiver(); 
           
        }

    }
    

    @Override
    public void onResume()
    {
        super.onResume();
        
        updateStatus();
            
          
        IntentFilter filter = new IntentFilter();
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        filter.addAction(ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE);
        registerReceiver(receiver, filter);
    }

    /**
     * 
     */
    public void updateStatus() {
        cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if(cm != null)
        {
            info = cm.getActiveNetworkInfo();
            refreshWithNetworkStatus();
        }
        else
        {
            LogUtil.debug(this, "Connectivity Manager returned null");
        }
    }

    /**
     * Refreshes based on network status
     */
    protected void refreshWithNetworkStatus() 
    {
        if(UploadManager.sAutoUploadQueue != null && UploadManager.sAutoUploadQueue.getQueueSize() > 0)  // for auto upload
        {
            if (info != null && info.isConnected()) {
    
                if (UploadManager.sCurrentSettings.getOffWhenRoaming() || !info.isRoaming()) 
                {
                    if (!UploadManager.sCurrentSettings.getOnlyOnWifi() || info.getType() == ConnectivityManager.TYPE_WIFI) 
                    {
                        refreshStatusAvailableNetwork(info);
                    }
                    else
                    {
                        String str = getResources().getString(R.string.upload_no_wifi_notification_body);
                        refreshStatusUnAvailableNetwork(str);
                    }
                }
                else
                {
                    String str = getResources().getString(R.string.upload_no_roaming_notification_body);
                    refreshStatusUnAvailableNetwork(str);
                }
            }
            else
                refreshStatusAvailableNetwork(info);
        }
        else
        {
            refreshStatusAvailableNetwork(info);
        }
    }

    /**
     * @param uploadFileInfoStr
     */
    private void refreshStatusUnAvailableNetwork(String statusString) 
    {
        String uploadFileInfoStr = uploadStatusFilesString();
        String failedUploadFilesStr = failedUploadString(UploadManager.getNumFailedUploadFiles());
        refresh(uploadFileInfoStr, statusString, failedUploadFilesStr);
    }

    /**
     * 
     */
    private void refreshStatusAvailableNetwork(NetworkInfo networkInfo) {
        // default 
        String uploadFileInfoStr = uploadStatusFilesString();
        String estimatedTimeToUploadStr = estimatedTimeToUploadString(networkInfo);
        String failedUploadFilesStr = failedUploadString(UploadManager.getNumFailedUploadFiles());
        refresh(uploadFileInfoStr, estimatedTimeToUploadStr, failedUploadFilesStr);
    }
    /**
     * 
     */
    protected void refresh(String uploadFileInfoStr, String estimatedTimeToUploadStr, String uploadFailedFilesStr) 
    {
        buildList(uploadFileInfoStr, estimatedTimeToUploadStr, uploadFailedFilesStr);
        this.aboutAdapter.notifyDataSetChanged();
    }
    
 
   

    @Override
    public void onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
    

    private void buildList(String uploadFileInfoStr, String uploadEstimatedTimeStr, String uploadFailedFilesStr) {


        
        this.aboutAdapter = new AboutViewListAdapter(this);
            
        this.aboutAdapter.addView(R.string.upload_status_screen_number_label,
                uploadFileInfoStr);
        
        this.aboutAdapter.addView(R.string.upload_status_screen_time_label,
                uploadEstimatedTimeStr);
        
        this.aboutAdapter.addView(R.string.upload_status_failed_uploads_label,
                uploadFailedFilesStr);

        ListView list = (ListView)findViewById(R.id.list);


        list.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
                updateAlarm();
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                updateAlarm();
            }

        });
        
        list.setOnItemClickListener(new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, final int position, long id)
            {
                Integer resId = (Integer) aboutAdapter.getItem(position);
                
                if(resId != null)
                {
                    String str1  = getResources().getString(resId);
                    LogUtil.debug(this, getResources().getString(resId));
                    
                    if(str1 != null && str1.equalsIgnoreCase(getResources().getString(R.string.upload_status_failed_uploads_label)))
                    {
                        String summary = aboutAdapter.getSummaryForItem(position);
                        if(summary != null && summary.equalsIgnoreCase("") == false 
                                && summary.endsWith(getResources().getString(R.string.upload_tap_to_retry_text)))
                        {
                            retryFailedUploadsDlg(position);
                        }
                    }
                    else if(str1 != null && str1.equalsIgnoreCase(getResources().getString(R.string.upload_status_screen_number_label)))
                    {
                        String summary = aboutAdapter.getSummaryForItem(position);
                        if(summary != null && summary.equalsIgnoreCase("") == false 
                                && summary.endsWith(getResources().getString(R.string.upload_status_tap_to_cancel)))
                        {
                            cancelUploadsDlg(position);
                        }
                    }
                }
            }
        });
        
        list.setAdapter(this.aboutAdapter);
    }

    /**
     * @param position
     */
    public void retryFailedUploadsDlg(final int position) {
        String titleStr = this.getResources().getString(R.string.upload_failed_generic_title_text);
        titleStr = titleStr.replace("$SYNC", this.getResources().getString(R.string.sync_title));
        
        if(UploadManager.lastFailedErrorCodeForUpload != ServerAPI.RESULT_OK)
        {
            switch(UploadManager.lastFailedErrorCodeForUpload)
            {
                case ServerAPI.RESULT_CONNECTION_FAILED:
                    titleStr = this.getResources().getString(R.string.upload_failed_internet_connectivity_title_text);
                    break;
                case CRResultCodes.CR_ERROR_EXCEEDED_QUOTA:
                    titleStr = this.getResources().getString(R.string.upload_failed_quota_exceeded_title_text);
                    titleStr = titleStr.replace("$SYNC", this.getResources().getString(R.string.sync_title));
                    break;
                case ServerAPI.RESULT_UNKNOWN_PARSER:
                    titleStr = this.getResources().getString(R.string.upload_failed_server_not_found_title_text);
                    break;
                case ServerAPI.RESULT_INVALID_CLIENT_VER:
                    titleStr = this.getResources().getString(R.string.client_upgrade_required);
                    break;
                case CRResultCodes.CR_ERROR_SERVER_GENERIC:
                    break;
                default:
                    break;
            }
            
        }
        // launch a dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(UploadStatusActivity.this);
        builder.setTitle(R.string.upload_failed_title_text);
        builder.setMessage(titleStr);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.upload_retry_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                UploadManager.failedUploadQueueSizeOnRetry = UploadManager.sFailedUploadsQueue != null ? UploadManager.sFailedUploadsQueue.getQueueSize() : 0;
                updateStatus();
                if(UploadManager.sFailedUploadsQueue != null)
                    UploadManager.sFailedUploadsQueue.onContentChangedListener(true, UploadManager.getQueueListener());
            }
        });
        builder.setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                // do nothing stay in the screen
            }
        });
        builder.setNeutralButton(R.string.upload_remove_from_queue_text, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                if(UploadManager.sFailedUploadsQueue != null)
                    UploadManager.sFailedUploadsQueue.dequeueAll();
                UploadManager.lastFailedErrorCodeForUpload = ServerAPI.RESULT_OK; // reset status code
                // update the  summary text
                updateStatus();
            }
        });
        builder.create().show();
    }

    
    
    
    public void cancelUploadsDlg(final int position) {
       
        // launch a dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(UploadStatusActivity.this);
        builder.setTitle(R.string.upload_cancel_title);
        builder.setMessage(R.string.upload_cancel_message);
        builder.setCancelable(true);
        builder.setPositiveButton(R.string.upload_cancel_current, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                Queue queue = UploadManager.getQueueForUploadType(UploadManager.currentUploadFileWithType.uploadType);
                UploadManager.removeCurrentJob(queue, UploadManager.currentUploadFileWithType.getFile());
                uploadFileAPI.getInstance(UploadStatusActivity.this).abort();
                updateStatus();
            }
        });
        builder.setNegativeButton(R.string.upload_cancel_none, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                // stay in the same screen
            }
        });
        builder.setNeutralButton(R.string.upload_cancel_all, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) 
            {
                if(UploadManager.sManualQueue != null)
                    UploadManager.sManualQueue.dequeueAll();
                if(UploadManager.sAutoUploadQueue != null)
                    UploadManager.sAutoUploadQueue.dequeueAll();
                Queue queue = UploadManager.getQueueForUploadType(UploadManager.currentUploadFileWithType.uploadType);
                UploadManager.removeCurrentJob(queue, UploadManager.currentUploadFileWithType.getFile());
                uploadFileAPI.getInstance(UploadStatusActivity.this).abort();
                updateStatus();
                // Clear any notifications hanging around 
                UploadNotification.clearNotificationOnEnqueue(UploadStatusActivity.this);
            }
        });
        builder.create().show();
    }
    
    /**
     * @return
     */
    protected String uploadStatusFilesString() {
        // The order that these views are added is the order that they will be displayed on the screen
        int numPendingFiles = UploadManager.getNumPendingFiles();
        
        if(numPendingFiles < 0) numPendingFiles = 0;
        
        String info = Integer.toString( numPendingFiles);

        if ( numPendingFiles != 1)
            info += " " + this.getString(R.string.upload_status_screen_files);
        else
            info += " " + this.getString(R.string.upload_status_screen_file);

        info += " " + calculateSizeString();
        return info;
    }
    /**
     * @return
     */
    protected String estimatedTimeToUploadString(NetworkInfo networkInfo) {
        this.sizePendingFiles = UploadManager.getPendingFilesTotalSize();

        // Now calculate some silly random number that is supposed to represent the time to upload the files.

        if ((networkInfo == null) || (!networkInfo.isConnected()))
        {
            this.estimatedTimeToUpload = -1;
        }
        else
        {
             if(this.sizePendingFiles > 0)
             {
                 int bytesPerSecond = ((networkInfo.getType() == ConnectivityManager.TYPE_WIFI) ? WIFI_KILO_BYTE_PER_SECOND :
                                                                                           THREE_G_KILO_BYTE_PER_SECOND) * 1024;
    
                 this.estimatedTimeToUpload = this.sizePendingFiles / bytesPerSecond;
                 this.estimatedTimeToUpload *= 4;    // Correction to take into account read access and other overhead.
             }
             else
                 this.estimatedTimeToUpload = 0;
        }

        StringBuilder tempString = new StringBuilder();
        if (this.estimatedTimeToUpload == -1)
        {
            tempString.append(this.getString(R.string.error_not_available));
        }
        else if( this.estimatedTimeToUpload > 0)
        {
            int numHours = this.estimatedTimeToUpload / 3600;
            int remainder = this.estimatedTimeToUpload % 3600;
            int numMinutes = remainder / 60;
            if ((remainder % 60) >= 30)
            {
                ++numMinutes;
            }

            if (numHours > 0)
            {
                tempString.append(Integer.toString(numHours));
                tempString.append(" ");
                int stringId = (numHours == 1) ? R.string.hour : R.string.hours;
                tempString.append(this.getString(stringId));
                tempString.append(", ");
            }
            if (this.estimatedTimeToUpload < 60 && this.estimatedTimeToUpload > 0)
                numMinutes = 1;
            if (numMinutes > 0)
            {
                tempString.append(Integer.toString(numMinutes));
                tempString.append(" ");
                int stringId = (numMinutes == 1) ? R.string.minute : R.string.minutes;
                tempString.append(this.getString(stringId));
                tempString.append(" ");
            }
        }
        return tempString.toString();
    }
    
    /**
     * @return
     */
    protected String failedUploadString(int uploadFailedFileCount) {

        String uploadFailedFilesString = new String();
        if (uploadFailedFileCount == 0)
        {
            uploadFailedFilesString = "";
        }
        else
        {
            String singleSpace = " ";
            uploadFailedFilesString = this.getString(R.string.upload_status_fail_upload_text) + singleSpace + this.getString(R.string.upload_tap_to_retry_text);
            uploadFailedFilesString = uploadFailedFilesString.replace("$NUMFILES", Integer.toString(uploadFailedFileCount));
        }

        return uploadFailedFilesString;
    }

    private String calculateSizeString()
    {
        // File size
        double fileSize = UploadManager.getPendingFilesTotalSize();
        final int decr = 1024;
        int step = 0;
        //R.array.file_sizes_array is {"bytes", "KB", "MB", "GB", "TB", "PB"}
        String[] postFix = getResources().getStringArray(R.array.file_sizes_array);
        while((fileSize / decr) > 0.9)
        {
            fileSize = fileSize / decr;
            step++;
        }
        
        String sizeString = String.format(Locale.getDefault(),step == 0 ? "%.1f %s" : "%.1f %s", fileSize, postFix[step]);
        if(fileSize == 0)
            return sizeString;
        else
            return sizeString + getResources().getString(R.string.upload_status_tap_to_cancel);
            
    }
    
    
    class AboutViewListAdapter extends BaseAdapter
    {
        private final Vector<Integer> res_title;
        private final Vector<String> res_summary;
        private Context context;

        public AboutViewListAdapter(Context context) {
            res_title = new Vector<Integer>();
            res_summary = new Vector<String>();
            this.context = context;
        }

        public int addView(int resource_title, String resource_summary)
        {
            int position = res_title.size();
            res_title.add(Integer.valueOf(resource_title));
            res_summary.add(resource_summary);
            return position;
        }

        public Object getItem(int position) {
            return res_title.get(position);
        }
        
        public String getSummaryForItem(int position)
        {
            return res_summary.get(position);
        }

        public int getCount() {
            return res_title.size();
        }

        @Override
        public int getViewTypeCount() {
            return res_title.size();
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) 
        {
            Integer resId = (Integer) aboutAdapter.getItem(position);
            String str1  = getResources().getString(resId);
            LogUtil.debug(this, getResources().getString(resId));
            String str2 = aboutAdapter.getSummaryForItem(position);
            
            if(str1 != null && str1.equalsIgnoreCase(getResources().getString(R.string.upload_status_failed_uploads_label)))
            {
                if(str2 != null && str2.endsWith(getResources().getString(R.string.upload_tap_to_retry_text)))
                    return true;
            } 
            else if (str1 != null && str1.equalsIgnoreCase(getResources().getString(R.string.upload_status_screen_number_label)))
            {
               if(str2 != null && str2.endsWith(getResources().getString(R.string.upload_status_tap_to_cancel)))
                   return true;
            }
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = null;
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // All items are instances of SETTINGS_LIST_ITEM
            view = inflater.inflate(R.layout.list_item_layout_main_settings, null);
            TextView title = (TextView) view.findViewById(R.id.name);
            title.setText(context.getString(res_title.get(position)));
            TextView summary = (TextView) view.findViewById(R.id.subtitle);
            summary.setText(res_summary.get(position));

            title.setEnabled(false);
            view.setEnabled(false);

            return view;
        }

        @Override
        public long getItemId(int position) {

            return 0;
        }
    }
    
    public class ResponseReceiver extends BroadcastReceiver {
        public static final String ACTION_FILE_UPLOADED_STATUS_UPDATE = "com.mozy.mobile.android.activities.UploadStatusActivity.STATUS_UPDATE";

        @Override
         public void onReceive(Context context, Intent intent) {
            
            if( intent.getAction().equals(ResponseReceiver.ACTION_FILE_UPLOADED_STATUS_UPDATE))
            {
               updateStatus();
            }
         }
    }   
}
