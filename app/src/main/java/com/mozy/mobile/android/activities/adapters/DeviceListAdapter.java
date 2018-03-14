package com.mozy.mobile.android.activities.adapters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.R;
import android.content.Context;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.SystemState;

/*
 * Adapts a list of devices (containers) to a list view
 */
public class DeviceListAdapter extends ArrayAdapter<Object> {
    private Context context;
    private int viewResourceId;
    private static final int SYNC_HEADER_COUNT = 1;    // One 'sync' devices plus a label
    private static final int DEVICE_HEADER_COUNT = 1;  
    private int syncHeaderLabelItems = SYNC_HEADER_COUNT;        // One 'sync' devices plus a label
    private int deviceHeaderLabelCount = DEVICE_HEADER_COUNT;
    public Context getContext() {
        return context;
    }


    public void setContext(Context context) {
        this.context = context;
    }


    public int getViewResourceId() {
        return viewResourceId;
    }


    public void setViewResourceId(int viewResourceId) {
        this.viewResourceId = viewResourceId;
    }


    public int getSyncHeaderCount() {
        if (!SystemState.isUploadEnabled())
        {
            this.syncHeaderLabelItems = 0;
        }
        return this.syncHeaderLabelItems;
    }

    public int getDeviceHeaderLabelCount() {
        return this.deviceHeaderLabelCount;
    }

    public StringBuffer getmStrTimestamp() {
        return mStrTimestamp;
    }


    public void setmStrTimestamp(StringBuffer mStrTimestamp) {
        this.mStrTimestamp = mStrTimestamp;
    }


    public static int getSyncItemCount() {
        return SYNC_HEADER_COUNT;
    }


//    public static double getBytesPerGb() {
//        return BYTES_PER_GB;
//    }
    
    
    private StringBuffer mStrTimestamp = new StringBuffer();
    
    
    protected int getDeviceLabelHeaderPosition()
    {
        if (isSyncAvailable())
            return 2;
        else
            return 0;
    }
    
    
    protected int getSyncHeaderPosition()
    {
        if (isSyncAvailable())
            return 0;
        else
            return -1;
    }
    
    protected boolean isSyncPosition(int position)
    {
        if (isSyncAvailable())
            return (position == 1);
        else
            return false;
    }
    
    protected boolean isDevicePosition(int position)
    {
        if (isSyncAvailable())
        {
            if(isSyncPosition(position) ==false && isHeaderPosition(position) == false)
              return true;
        }
        else
        {
            if(isHeaderPosition(position) == false)
                return true;
        }
        return false;
    }
    
    
    protected boolean isHeaderPosition(int position)
    {
        if (isSyncAvailable())
        {
            return (getSyncHeaderPosition() == position || getDeviceLabelHeaderPosition() == position);
        }
        else
        {
            return (getDeviceLabelHeaderPosition() == position);
        }
    }
    
    
    protected boolean isSyncAvailable()
    {
        if(SystemState.cloudContainer != null)
            return true;
        else
            return false;
    }
    
    
    protected boolean isSyncOrDeviceAvailable()
    {
        int actualDeviceCount = getDeviceCount();
        if(actualDeviceCount > 0 || isSyncAvailable() )
            return true;
        else
            return false;
    }

    public DeviceListAdapter(Context context, int viewResourceId) {
        super(context, viewResourceId);
        this.context = context;
        this.viewResourceId = viewResourceId;
    }
    
    
    
    // Return a count that represents the number of devices, plus the extra 'dummy' devices I add in this class.
    // This method gets called several times before we have the results from the server. We do not want to draw any
    // items if the server results are not received yet, so if the base class returns zero, this method returns zero.
    @Override
    public int getCount() 
    {
        int returnValue = getDeviceCount();
        
        // We should *always* have a cloud container on the server. But just in case the server is screwed up
        // then return a count for just the regular containers.
        if (SystemState.cloudContainer != null)
        {
            // add sync header
            returnValue += this.getSyncHeaderCount();
        }
        
        if(returnValue != 0)
            returnValue = returnValue + getDeviceHeaderLabelCount();
        
        
        return returnValue;
    }

    public int getDeviceCount()
    {
        return super.getCount();
    }
    

     public Object getItem(int position) 
     {    
        Object returnValue = null;
         if(isHeaderPosition(position) == false)
         {
             // We have a sync folder
             if (isSyncAvailable())
             {
                 if (isSyncPosition(position))    // First position is the sync device
                  {
                     returnValue = SystemState.getSyncDevice();
                  }
                  else
                  {
                     // Rest of the devices 
                    // index accounts for the Sync device and the label headers 
                         returnValue = super.getItem(position - this.getSyncHeaderCount() - getDeviceHeaderLabelCount());
                 }
             }
             else  // No sync folder
            {
                     returnValue = super.getItem(position - getDeviceHeaderLabelCount());
            }
         }
             
        return returnValue;   
    }

    public void refresh()
    {
        addDevices();
    }
    
    
    void addDevices()
    {
        this.clear();
        
        ArrayList<Object> devicelist = SystemState.getDeviceList();
        for(int i = 0; devicelist != null && i < devicelist.size(); i++)
        {
            this.add(devicelist.get(i));
        }
        
        this.notifyDataSetChanged();
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        
        if(position == 0)  // Need to it once
        {
            boolean syncOrDevice = isSyncOrDeviceAvailable();
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.generic_list_layout, null);
            view.findViewById(R.id.notification).setVisibility(syncOrDevice ? View.GONE:View.VISIBLE);
            view.findViewById(R.id.footer_divider).setVisibility(syncOrDevice ? View.VISIBLE:View.GONE);
        }

        if(isSyncOrDeviceAvailable())
        {
            if (isHeaderPosition(position) == true)
            {
                view = getViewForHeaders(position, convertView);
            }
            else
            {
                view = getViewForDeviceOrSync(position, convertView);
            }
        }

        return view;
    }
    
    
    


    /**
     * @param position
     * @param convertView
     * @param view
     * @return
     */
    public View getViewForHeaders(int position, View convertView) {
        View view = null;
        // Is the convertView an instance of the listItems or an instance of the label.
        if ((convertView != null) && (convertView.getId() == R.id.titleBar))
        {
            view = convertView;        // The convertView is the label                 
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           
            if (position == getSyncHeaderPosition())    // First position is the sync device
            {
               view = inflater.inflate(R.layout.device_list_subtitle_layout, null);
                
               // ListView deviceListView  = (ListView) view.findViewById(R.id.generic_list);
                TextView textView = (TextView)view.findViewById(R.id.subtitle);
                textView.setText(R.string.sync_title_s_caps);
            }
            else if (position == getDeviceLabelHeaderPosition())
            {
                view = inflater.inflate(R.layout.device_list_subtitle_layout, null);
                TextView textView = (TextView) view.findViewById(R.id.subtitle);
                textView.setText(R.string.devices_label);
            }
        }
        return view;
    }



    /**
     * @param position
     * @param view
     */
    public View getViewForDeviceOrSync(int position, View convertView) {
        
        View view = null;
        
        if ((convertView != null) && (convertView instanceof android.widget.LinearLayout))
        {
            view = convertView;
        }
        else
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(viewResourceId, null);                
        }
        
        
        if (viewResourceId == R.layout.list_item_layout_device) {
            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            TextView textView = (TextView) view.findViewById(R.id.name);
            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            TextView quota = (TextView) view.findViewById(R.id.quota);
//            String quotaString = null;

            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int displayWidth = display.getWidth();
            view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));
            
            // Is the position for one of the 'real' devices
            if (isDevicePosition(position))
            {
                
                Device d = (Device)this.getItem(position);
                if (d != null)
                {
                    if(d.getEncrypted() && (SystemState.isManagedKeyEnabled(context) == false))
                    {
                        String passphrase = Provisioning.getInstance(this.context).getPassPhraseForContainer(((Device)d).getId());
                        if(passphrase != null && passphrase.length() != 0)   // Passphrase already set for container (visited settings first)
                        {
                            imageView.setImageResource(R.drawable.device_unlocked);
                        }
                        else
                        {
                            imageView.setImageResource(R.drawable.device_locked);
                        }
                    }
                    else
                    {
                        imageView.setImageResource(R.drawable.computer);
                    }
                    textView.setText(d.getTitle());

//                    quotaString = this.getQuotaUsageInPercentage(d);
//
//                    if (quotaString == null || quotaString.equals(""))
//                        quota.setVisibility(View.GONE);
//                    else
//                        quota.setText(quotaString);
                    
                    subtitle.setText(this.getLastBackedUp(d));
                    subtitle.setVisibility(View.VISIBLE);
                    quota.setVisibility(View.GONE);
                }
            }
            else if(isSyncPosition(position))
            {
                Device d = SystemState.cloudContainer;  

 //               quotaString = this.getQuotaUsageInPercentage(SystemState.cloudContainer);

                //imageView.setImageResource(R.drawable.sync_folder);
                if(d != null)  // Fix for crash when device is returned null
                {
                    textView.setText(d.getTitle());
                    if(d.getEncrypted() && (SystemState.isManagedKeyEnabled(context) == false)) {
                        String passphrase = Provisioning.getInstance(this.context).getPassPhraseForContainer(((Device)d).getId());
                        if(passphrase != null && passphrase.length() != 0) {
                            imageView.setImageResource(R.drawable.sync_folder_unlocked);
                        } else {
                            imageView.setImageResource(R.drawable.sync_folder_locked);
                        }
                    } else {
                        imageView.setImageResource(R.drawable.sync_folder);
                    }
                }
                else
                {
                    textView.setText("");
                }
              //  quota.setText(quotaString);
                subtitle.setVisibility(View.GONE);
                quota.setVisibility(View.GONE);
            }
        }
        return view;
    }

    public String getTimeStamp() {
        return mStrTimestamp.toString();
    }
    
//    static final double BYTES_PER_GB = 1024*1024*1024.0;
//    private String getQuotaUsageInPercentage(Device d)
//    {
//        if (0 == d.getQuota())
//            return "";
//        
//        String s = (this.context.getResources().getString(R.string.quota_used));
//        
//        double size = (d.getSize()/BYTES_PER_GB);
//        double quota = (d.getQuota()/BYTES_PER_GB);
//        
//        long size_percent = Math.round(size/quota * 100);
//        s = s.replace("$USED", Long.toString(size_percent));
//        s = s.replace("$QUOTA", Long.toString(Math.round(d.getQuota()/BYTES_PER_GB)));
//        return s;
//    }
    private String getLastBackedUp(Device d)
    {
        if (0 == d.getUpdated())
            return this.context.getResources().getString(R.string.never_backed_up);
        
        StringBuffer s = new StringBuffer(this.context.getResources().getString(R.string.last_backed_up));
        s.append(" ");
        
        Calendar cal = Calendar.getInstance();
        Date parsed = new Date(d.getUpdated());
        Date now = new Date();
        long endL = now.getTime();
        long startL = parsed.getTime() + (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET));
        
        long deltaMS = endL-startL;
        String units; 
        long delta = 0;
        // if less then 1 hour
        if (deltaMS < (1000*60*60))
        {
            delta = deltaMS/(1000*60);
            if (delta>1)
            	units =this.context.getResources().getString(R.string.minutes);
            else
            	units = this.context.getResources().getString(R.string.minute);
        }
        else if (deltaMS < 1000*60*60*24)  // less than one day
        {
            delta = deltaMS/(1000*60*60);
            if (delta>1)
            	units = this.context.getResources().getString(R.string.hours);
            else
            	units = this.context.getResources().getString(R.string.hour);
        } else 
        {
        	delta = deltaMS/(1000*60*60*24);
        	if (delta>1)
        		units = this.context.getResources().getString(R.string.days);
            else
            	units = this.context.getResources().getString(R.string.day);    
        }
        s.append(delta);
        s.append(" ");
        s.append(units);
        s.append(" ");
        s.append(this.context.getResources().getString(R.string.ago));
        return (s.toString());
    }    
}
