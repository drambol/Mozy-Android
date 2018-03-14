package com.mozy.mobile.android.activities;

import java.util.ArrayList;
import java.util.Hashtable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.DeviceListAdapter;
import com.mozy.mobile.android.activities.adapters.QuickAccessAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;


public class QuickAccessScreenActivity  extends SecuredActivity
{
    private ListView deviceListView;
    private QuickAccessAdapter listAdaptor;
    private int headerTitleId;
    private int totalCount; 


    @Override 
    public void onCreate(Bundle savedInstanceState) 
    {    
        String headerTitleIdStr = null;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.generic_list_layout);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            headerTitleIdStr = extras.getString("headerTitleId");
            LogUtil.debug(this, "headerTitleId: " + headerTitleIdStr);
        }
        
        if(headerTitleIdStr != null)
            headerTitleId = Integer.parseInt(headerTitleIdStr);
        
        totalCount = SystemState.getDevicePlusSyncCount();
        
              
        if(totalCount > 1)
        {
            initView(headerTitleId);        

            deviceListView.setOnItemClickListener(new OnItemClickListener() 
            {
                @Override
                public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
                {
                    final Directory d = (Directory)listAdaptor.getItem(position);
                    
                    // 'd' can be null if the user has clicked a label
                    if (d != null)
                    {
                        if (d instanceof Device)
                        {
                            navigateQuickAccess(headerTitleId, d);
                        }
                    }
                }
            });
        }
        else if (totalCount == 1)
        {
            Directory d = null;
            ArrayList<Object> devicelist = SystemState.getDeviceList();
            if (devicelist != null && 1 == devicelist.size())
                d = (Device)devicelist.get(0);

            navigateQuickAccess(headerTitleId, d);
        }

        updateViewForNumItems(this.totalCount);
        
    }
    
    private void navigateQuickAccess(int headerTitleId, Directory d)
    {
        switch( headerTitleId)
        {
            case R.string.quicklists_label_downloaded:
                navigateToDownloadScreen(d);
                 break;
            case R.string.quicklists_label_recently_added:
                recentlyAdded(d);
                break;
            case R.string.search_photos:
                quickbrowse( d, FileUtils.photoSearch, false);
                break;
            case R.string.search_documents:
                quickbrowse( d, FileUtils.documentSearch, false);
                break;
            case R.string.search_music:
                quickbrowse(d, FileUtils.musicSearch, false);
                break;
            case R.string.search_video:
                quickbrowse( d, FileUtils.videoSearch, false);
                break;
        }
    }
    
    
    private void updateViewForNumItems(int numItems)
    {
        // Show/hide the "no items found" message as appropriate.
        // And do the opposite for the footer divider line we are adding manually.
        boolean isListVisible = numItems > 0;
        findViewById(R.id.notification).setVisibility(isListVisible ? View.GONE : View.VISIBLE);
        findViewById(R.id.footer_divider).setVisibility(isListVisible ? View.VISIBLE : View.GONE);
    }
    
    private void navigateToDownloadScreen(Directory d)
    {
        String link = d.getLink();
        String title = d.getTitle();
        boolean isEncrypted = false;
        String platform = null;
        String id = null;
    
        updateAlarm();
        
        if(d instanceof Device)
        {
            isEncrypted = ((Device) d).getEncrypted();
            platform = ((Device) d).getPlatform();
            id = ((Device) d).getId();   
        }


        Intent intent = new Intent(QuickAccessScreenActivity.this, DownloadDirFileListActivity.class);
        intent.putExtra("containerLink", link);
        intent.putExtra("recurse", true);
        intent.putExtra("title", title);
        intent.putExtra("deviceId", id);
        intent.putExtra("deviceTitle", d.getTitle());
        intent.putExtra("deviceType",isEncrypted );
        intent.putExtra("platform",platform );
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    
    
    private void quickbrowse(Directory d, String searchText, boolean isPhotoDirGridEnabled)
    {  
        updateAlarm();
        
        if(d instanceof Device)
        { 
            if(((Device) d).getSync())
                handleDevice(d, searchText, isPhotoDirGridEnabled, true);     // Sync
            else
            {
                boolean isEncrypted = ((Device) d).getEncrypted();
     
                if(isEncrypted)
                    handleEncryptedDevice(d, searchText, isPhotoDirGridEnabled, false);
                else
                    handleDevice(d, searchText, isPhotoDirGridEnabled, false);
            }
        }
      
        
    }

    /**
     * @param d
     * @param searchText
     * @param isPhotoDirGridEnabled
     * @param link
     * @param title
     * @param isEncrypted
     * @param isDeletable
     * @param platform
     * @param id
     */
    public void handleDevice(Directory d, String searchText, boolean isPhotoDirGridEnabled , boolean isFileDeletable) {
        
        String link = d.getLink();
        String title = d.getTitle();
        boolean isEncrypted = false;

        String platform = null;
        String id = null;
        
        
        if(d instanceof Device)
        {
            isEncrypted = ((Device) d).getEncrypted();
            platform = ((Device) d).getPlatform();
            id = ((Device) d).getId();
            
        }

        Intent intent;
        
        if(searchText.equalsIgnoreCase(FileUtils.photoSearch))
        {
            intent = new Intent(QuickAccessScreenActivity.this, PhotoSearchDirFileListActivity.class);
        }
        else
        {    
            intent = new Intent(QuickAccessScreenActivity.this, DirFileListActivity.class);
        }
        intent.putExtra("containerLink", link);
        // searchText and recurse are set going forward and not in the current intent
        intent.putExtra("searchText", searchText);
        intent.putExtra("searchDirectory", d.getPath());
        intent.putExtra("recurse", true);
        intent.putExtra("title", title);
        intent.putExtra("canFilesBeDeleted", isFileDeletable);
        intent.putExtra("deviceId", id);
        intent.putExtra("deviceTitle", d.getTitle());
        intent.putExtra("deviceType",isEncrypted );
        intent.putExtra("platform",platform );
        intent.putExtra("isPhotoDirGridEnabled", isPhotoDirGridEnabled);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    
    
    private void handleEncryptedDevice(final Directory d, final String searchText, final boolean isPhotoDirGridEnabled, final boolean isFileDeletable)
    {
        final Hashtable<String, Boolean> accessTableEncryptedContainers = SystemState.getEncryptedContainerAccessTable();
        
        String passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(((Device)d).getId());
        
        
        if(SystemState.isManagedKeyEnabled(getApplicationContext()) == false)
        {
            if(passphrase != null && passphrase.length() != 0)   // Passphrase already set for container (visited settings first)
            {
                accessTableEncryptedContainers.put(((Device)d).getTitle(),false);
            }
            
            if((accessTableEncryptedContainers.isEmpty()  == false) 
                && (Boolean) accessTableEncryptedContainers.get(((Device)d).getTitle()))
            {
                 AlertDialog alertDialog = new AlertDialog.Builder(QuickAccessScreenActivity.this)
                   .setCancelable(false)
                   .setTitle(((Device) d).getTitle())
                   .setMessage(R.string.encrypted_device_first_time_prompt_message)
                   .setIcon(getResources().getDrawable(R.drawable.warning)) 
                   .setPositiveButton(R.string.yes_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Intent intent = new Intent(getApplicationContext(), PersonalKeysSettingsActivity.class);
                        intent.putExtra("keyForcontainer", ((Device) d).getTitle());
                        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                    } })
                    .setNegativeButton(R.string.no_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            encrypt_first_time_warn_dialog(d, searchText, isPhotoDirGridEnabled);
                       }

                    private void encrypt_first_time_warn_dialog(final Directory d, final String searchText, final boolean isPhotoDirGridEnabled) {
                       AlertDialog alertDialog = new AlertDialog.Builder(QuickAccessScreenActivity.this)
                      .setCancelable(false)
                      .setTitle(((Device) d).getTitle())
                      .setMessage(R.string.encrypted_device_first_time_warn_message)
                      .setIcon(getResources().getDrawable(R.drawable.warning))
                      .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int which) {
                         dialog.cancel();
                         accessTableEncryptedContainers.put(((Device)d).getTitle(),false);
                         handleDevice(d, searchText, isPhotoDirGridEnabled, isFileDeletable);
                       } })
                      .setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if(SystemState.getDevicePlusSyncCount() == 1)
                            finish(); // return to home screen
                       } })
                     .create();
                      alertDialog.show();
                      }
                    })
                    .create();
                     alertDialog.show();
            }
            else
                handleDevice(d, searchText, isPhotoDirGridEnabled, isFileDeletable);
        }
        else
        {
            handleDevice(d, searchText, isPhotoDirGridEnabled, isFileDeletable);
        }

        Provisioning.getInstance(this).registerListener(this);
    }
    
    private void recentlyAdded(Directory d)
    {
        String link = d.getLink();
        String title = d.getTitle();
        boolean isEncrypted = false;
        boolean isDeletable = false;
        String platform = null;
        String id = null;
    
        updateAlarm();
        
        if(d instanceof Device)
        {
            isEncrypted = ((Device) d).getEncrypted();
            isDeletable = d.areFilesDeletable();
            platform = ((Device) d).getPlatform();
            id = ((Device) d).getId();
            
            if(((Device) d).getSync() == true)  // sync
            {
                isDeletable = true;
            }
            
        }
       


        Intent intent = new Intent(QuickAccessScreenActivity.this, RecentDirFileListActivity.class);
        intent.putExtra("containerLink", link);
        // searchText and recurse are set going forward and not in the current intent
        intent.putExtra("searchText", "");
        intent.putExtra("searchDirectory", d.getPath());
        intent.putExtra("recurse", true);
        intent.putExtra("title", title);
        intent.putExtra("canFilesBeDeleted", isDeletable);
        intent.putExtra("deviceId", id);
        intent.putExtra("deviceTitle", d.getTitle());
        intent.putExtra("deviceType",isEncrypted );
        intent.putExtra("platform",platform );
        intent.putExtra("recurse", false);
        intent.putExtra("isPhotoDirGridEnabled", true);
        startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    

    
    private void initView(int barTitleId) {
        setBarTitle(barTitleId);
        
        this.deviceListView  = (ListView) findViewById(R.id.generic_list);

        this.listAdaptor = (QuickAccessAdapter) getLastNonConfigurationInstance();
        if (listAdaptor == null)
        {
            listAdaptor = new QuickAccessAdapter(getApplicationContext(), R.layout.list_item_layout_device);
        }
        
        this.listAdaptor.refresh(); // add devices
        
        this.deviceListView.setAdapter(this.listAdaptor);
        deviceListView.setSelection(0);
    }

    
    public DeviceListAdapter getListAdaptor() {
        return this.listAdaptor;
    }
    
    
    @Override
    public void onResume()
    {
        super.onResume();
        totalCount = SystemState.getDevicePlusSyncCount();
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        this.listAdaptor = null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        menu.add(0, MENU_HELP, 0, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_HOME, 1, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        menu.add(0, MENU_SETTINGS, 2, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);
        return true;
    }
    
}
