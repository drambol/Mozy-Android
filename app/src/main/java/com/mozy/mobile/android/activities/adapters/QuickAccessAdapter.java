package com.mozy.mobile.android.activities.adapters;


import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.R;
import android.content.Context;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.SystemState;


/*
 * Adapts a list of devices (containers) to a list view
 */
public class QuickAccessAdapter extends DeviceListAdapter {
    private Context context;
    private int viewResourceId;
    private static final int SYNC_HEADER_COUNT = 0;    // One 'sync' devices plus a label
    private static final int DEVICE_HEADER_COUNT = 0; 
    private int syncHeaderLabelItems = SYNC_HEADER_COUNT;        // One 'sync' devices plus a label
    private int deviceHeaderLabelCount = DEVICE_HEADER_COUNT;
    
    @Override
    protected int getDeviceLabelHeaderPosition()
    {
        return -1;
    }
    
    @Override
    protected int getSyncHeaderPosition()
    {
        return -1;
    }
    
    @Override
    public int getSyncHeaderCount() {
        return this.syncHeaderLabelItems;
    }



    @Override
    public int getDeviceHeaderLabelCount() {
        return this.deviceHeaderLabelCount;
    }

    public QuickAccessAdapter(Context context, int viewResourceId) {
        super(context, viewResourceId);
        this.context = context;
        this.viewResourceId = viewResourceId;
    }
    
    
    @Override
    protected boolean isSyncPosition(int position)
    {
        if (isSyncAvailable())
            return (position == 0);
        else
            return false;
    }

    @Override
    public int getCount() 
    {
        int returnValue = getDeviceCount();
        
        return returnValue;
    }

    @Override
    public void refresh()
    {
        super.refresh();
    }
    


    /**
     * @param position
     * @param view
     */
    @Override
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

            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int displayWidth = display.getWidth();
            view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));
            
            // Is the position for one of the 'real' devices
            if (isDevicePosition(position))
            {
                
                Device d = (Device)this.getItem(position);
                if (d != null)
                {
                    if(d.getEncrypted())
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
                }
            }
            else if(isSyncPosition(position))
            {
                Device d = SystemState.cloudContainer;  
                
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
                    imageView.setImageResource(R.drawable.sync_folder);
                }
            }
            
            TextView subtitle = (TextView) view.findViewById(R.id.subtitle);
            TextView quota = (TextView) view.findViewById(R.id.quota);
            
            subtitle.setVisibility(View.GONE);
            quota.setVisibility(View.GONE);
        }
        return view;
    }
}
