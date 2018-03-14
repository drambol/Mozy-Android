
package com.mozy.mobile.android.activities;

import java.util.Hashtable;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.DeviceListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.SystemState;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class FilesScreenFragment extends android.support.v4.app.Fragment 
{
    private ListView deviceListView;
    private DeviceListAdapter listAdaptor;

    public DeviceListAdapter getListAdaptor() 
    {
        return this.listAdaptor;
    }
    
    private Device cloudDevice = null;
    public Device getCloudDevice()
    {
        return SystemState.cloudContainer;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = (View) inflater
                .inflate(R.layout.generic_list_layout, container, false);
        
        int totalCount = SystemState.getDevicePlusSyncCount();
        
        if(totalCount == 0)
        {
            updateViewForNumItems(view, totalCount);
        }
        else
        {
            
            this.deviceListView  = (ListView) view.findViewById(R.id.generic_list);
            
           // this.listAdaptor = (DeviceListAdapter) getLastNonConfigurationInstance();
            if (this.listAdaptor == null)
            {
                this.listAdaptor = new DeviceListAdapter(FilesScreenFragment.this.getActivity().getApplicationContext(), R.layout.list_item_layout_device);
            }
            
            this.listAdaptor.refresh(); // add devices
            
            this.deviceListView.setAdapter(this.listAdaptor);
            deviceListView.setSelection(0);

            deviceListView.setOnItemClickListener(new OnItemClickListenerClass());
        }
        
        return view;
    }
    
    
    private void navigateToDevice(Directory d, String deviceId,  boolean isEncrypted,String platform, boolean areFilesDeleteable)
    {
        String link = d.getLink();
        String title = d.getTitle();

        // New navigation starts in listview mode.
        Intent intent = new Intent(FilesScreenFragment.this.getActivity(), DirFileListActivity.class);
        intent.putExtra("containerLink", link);
        intent.putExtra("title", title);
        intent.putExtra("canFilesBeDeleted", areFilesDeleteable);
        intent.putExtra("deviceId", deviceId);
        intent.putExtra("deviceTitle", title);
        intent.putExtra("deviceType", isEncrypted);
        intent.putExtra("platform", platform);
        intent.putExtra("isPhotoDirGridEnabled", true);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }

    
    @Override
    public void onResume()
    {
        super.onResume();
    }
    
    
    /**
     * @param d
     */
    public void handleCloudDevice(final Directory d) {
        String deviceId;
        boolean bDeviceEncrypted;
        String platform;
        
        this.cloudDevice = getCloudDevice();
        // This is a directory in the cloud device, so use the saved cloud objects id.
        // We expect to *always* have a cloud device.
        deviceId = this.cloudDevice.getId();
        bDeviceEncrypted =  this.cloudDevice.getEncrypted();
        platform = this.cloudDevice.getPlatform();
        navigateToDevice(d, deviceId, bDeviceEncrypted,platform, true);
    }

    /**
     * @param d
     */
    public void handleNonEncryptedDevice(final Directory d) {
        String deviceId;
        String platform;
        // Not encrypted. Navigate to it.
        deviceId = ((Device)d).getId();
        platform = ((Device)d).getPlatform();
        navigateToDevice(d, deviceId, false,platform, false);
    }
    
    
    /**
     * @param d
     */
    public void handleEncryptedDevice(final Directory d) {
        // If the device is encrypted, put up a warning dialog and
        // allow the user to cancel.
        
        final Hashtable<String, Boolean> accessTableEncryptedContainers = SystemState.getEncryptedContainerAccessTable();
        
        String passphrase = Provisioning.getInstance(FilesScreenFragment.this.getActivity().getApplicationContext()).getPassPhraseForContainer(((Device)d).getId());
        
        
        if(SystemState.isManagedKeyEnabled(FilesScreenFragment.this.getActivity().getApplicationContext()) == false)
        {
            if(passphrase != null && passphrase.length() != 0)   // Passphrase already set for container (visited settings first)
            {
                accessTableEncryptedContainers.put(((Device)d).getTitle(),false);
            }
            
            if((accessTableEncryptedContainers.isEmpty()  == false) 
                && (Boolean) accessTableEncryptedContainers.get(((Device)d).getTitle()))
            {

                 AlertDialog alertDialog = new AlertDialog.Builder(FilesScreenFragment.this.getActivity())
                   .setCancelable(false)
                   .setTitle(((Device) d).getTitle())
                   .setMessage(R.string.encrypted_device_first_time_prompt_message)
                   .setIcon(getResources().getDrawable(R.drawable.warning))
                   .setPositiveButton(R.string.yes_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent intent = new Intent(FilesScreenFragment.this.getActivity().getApplicationContext(), PersonalKeysSettingsActivity.class);
                        intent.putExtra("keyForcontainer", ((Device) d).getTitle());
                        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                    } })
                    .setNegativeButton(R.string.no_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            encrypt_first_time_warn_dialog(d);
                       }

                    private void encrypt_first_time_warn_dialog(final Directory d) {
                       AlertDialog alertDialog = new AlertDialog.Builder(FilesScreenFragment.this.getActivity())
                      .setCancelable(false)
                      .setTitle(((Device) d).getTitle())
                      .setMessage(R.string.encrypted_device_first_time_warn_message)
                      .setIcon(getResources().getDrawable(R.drawable.warning))
                      .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int which) {
                         dialog.cancel();
                         accessTableEncryptedContainers.put(((Device)d).getTitle(),false);
                         navigateEncryptedDevice(d);
                       } })
                      .setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                       } })
                     .create();
                      alertDialog.show();
                      }
                    })
                    .create();
                     alertDialog.show();
            }
            else
                navigateEncryptedDevice(d);
        }
        else
        {
            navigateEncryptedDevice(d);
        }
    }
    
    protected void navigateEncryptedDevice(final Directory d) {
        Device encryptedDevice = (Device) d;
        if(encryptedDevice.equals(getCloudDevice())) {
        	navigateToDevice(encryptedDevice, encryptedDevice.getId(), encryptedDevice.getEncrypted(),encryptedDevice.getPlatform(), true);
        } else {
            navigateToDevice(encryptedDevice, encryptedDevice.getId(), encryptedDevice.getEncrypted(),encryptedDevice.getPlatform(), false);
        }
     }
    
    private void updateViewForNumItems(View view, int numItems)
    {
        // Show/hide the "no items found" message as appropriate.
        // And do the opposite for the footer divider line we are adding manually.
        boolean isListVisible = numItems > 0;
        view.findViewById(R.id.notification).setVisibility(isListVisible ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.footer_divider).setVisibility(isListVisible ? View.VISIBLE : View.GONE);
    }


    
    private final class OnItemClickListenerClass implements
    OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
        {
            final Directory d = (Directory)listAdaptor.getItem(position);
            
            // 'd' can be null if the user has clicked a label
            if (d != null)
            {
                if (d instanceof Device)
                {
                    if (((Device) d).getEncrypted())
                    {
                        handleEncryptedDevice(d);
                    }
                    else
                    {
                        if(((Device)d).equals(getCloudDevice()))
                        {
                            handleCloudDevice(d);
                        }
                        else
                        {
                        handleNonEncryptedDevice(d);
                        }
                    }
                }
            }
        }
    }
}