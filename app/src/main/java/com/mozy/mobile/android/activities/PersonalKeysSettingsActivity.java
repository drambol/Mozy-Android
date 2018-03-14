package com.mozy.mobile.android.activities;


import java.util.ArrayList;
import java.util.Hashtable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.provisioning.Provisioning;

public class PersonalKeysSettingsActivity extends SecuredActivity
{
    
    private String selectedContainer = null;
    
    private ListView list;
    
    ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();  
    

    /**
     * Adapters
     */
    private PersonalKeysSettingsViewListAdapter view_list_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        setBarTitle(R.string.personal_key_settings);
        
        ListView list = (ListView)findViewById(R.id.list);
        
        if(this.encryptedDeviceList != null)
        {
            buildList();
        
            registerForContextMenu(list); 
        
        
            // Focus on selected container
            Bundle extras = getIntent().getExtras();
            
            String keyForcontainer = null;
            
            LogUtil.debug(this, "Loading extras from intent:");
            if(extras != null)
                keyForcontainer = extras.getString("keyForcontainer");
            
    
            
            if(keyForcontainer != null && keyForcontainer.length() != 0)
            {
                for(int i = 0; i < encryptedDeviceList.size(); i++)
                {
                    if(((Device )this.encryptedDeviceList.get(i)).getTitle().equals(keyForcontainer))
                    {
                        list.setSelection(i);
                        break;
                    }
                }
            }
            
            
            list.setOnItemClickListener(new OnItemClickListener() {

            ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();  
            @Override
              public void onItemClick(AdapterView<?> list, View view, int position, long id)
              {
                  updateAlarm();
                
                  String deviceTitle = null;
                  String deviceId = null;
                  
                  if(encryptedDeviceList != null)
                  {
                      deviceTitle = ((Device )encryptedDeviceList.get(position)).getTitle();     
                      deviceId =((Device )encryptedDeviceList.get(position)).getId();  
                  }
    
                  if(deviceTitle != null && deviceId != null)
                      showPrivateKeyEditDialog(deviceTitle, deviceId);
              }
            });
    
            
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
            }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        
        View view = info.targetView;
        
        TextView title = (TextView) view.findViewById(R.id.name);
        selectedContainer = title.getText().toString();

        menu.setHeaderTitle(selectedContainer);
      
        menu.add(Menu.NONE, v.getId(), 0, R.string.private_key_edit);
            
        Device d = (this.encryptedDeviceList != null) ? (Device) encryptedDeviceList.get(info.position) : null;

        String passphrase = null;
        
        if(d != null)
        {
            passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(d.getId());
        }
        
        if(passphrase != null && passphrase.length() != 0)
          menu.add(Menu.NONE, v.getId(), 0, R.string.private_key_forget).setEnabled(true);
        else
          menu.add(Menu.NONE, v.getId(), 0, R.string.private_key_forget).setEnabled(false);

    }
    
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
      
      updateAlarm();
      
      final String deviceTitle = this.selectedContainer;
      final String deviceId = ServerAPI.getInstance(this.getApplicationContext()).getDeviceId(selectedContainer);
      
      if(deviceTitle != null && deviceId != null)
      {
          if(item.getTitle().toString().equalsIgnoreCase(this.getApplicationContext().getResources().getString(R.string.private_key_edit)))
          {
              showPrivateKeyEditDialog(deviceTitle, deviceId);
          
          }else  if(item.getTitle().toString().equalsIgnoreCase(this.getApplicationContext().getResources().getString(R.string.private_key_forget)))
          {
               showPrivateKeyEraseAlertDialog(deviceTitle, deviceId);
          } 
      }
      else
      {
         return false;
      }
 
      return true;
    }

    private void showPrivateKeyEditDialog(final String deviceTitle, final String deviceId) {
          
        LayoutInflater inflater = LayoutInflater.from(this);
        final View textEntryView = inflater.inflate(R.layout.personal_key_entry, null);

        AlertDialog.Builder alertPrivateKeyEditDialog = new AlertDialog.Builder(this); 

        alertPrivateKeyEditDialog.setTitle(deviceTitle); 

        TextView textView2 = (TextView) textEntryView.findViewById(R.id.textbox_title);
        textView2.setText(R.string.private_key_settings_dialog_message);
        
        final EditText input = (EditText) textEntryView.findViewById(R.id.pkEditText);
        
        
        final CheckBox checkbox = (CheckBox) textEntryView.findViewById(R.id.pkCheckBox);
        
        checkbox.setText(R.string.private_key_settings_dialog_checkbox_title);
        checkbox.setTextAppearance(getApplicationContext(), R.style.MediumWhiteTextAppearance);
        
        checkbox.setChecked(false);  // Set it  unchecked by default
        
        String passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(deviceId);
        
        if(passphrase != null && passphrase.length() != 0)
        {
             input.setText(passphrase);
             input.setSelection(passphrase.length());  // cursor at end of passphrase
        }
            
        
         input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        
        // Set an EditText view to get user input  
         alertPrivateKeyEditDialog.setView(textEntryView); 

         alertPrivateKeyEditDialog.setPositiveButton(R.string.save_button_text, new DialogInterface.OnClickListener() { 
           public void onClick(DialogInterface dialog, int whichButton) { 
               saveKeyAndShowDialog(textEntryView, deviceTitle, deviceId);

               
            // Reset the flag only if any passphrase is set
               String passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(deviceId);
               if(passphrase != null && passphrase.length() != 0)
               {
                   Hashtable<String, Boolean> accessTableEncryptedContainers = SystemState.getEncryptedContainerAccessTable();
                   accessTableEncryptedContainers.put(deviceTitle,false);
               }
           }

           /**
            * @param textEntryView
            * @param deviceTitle
            */
           private void saveKeyAndShowDialog(final View textEntryView, final String deviceTitle, final String deviceId) {
            final EditText input = (EditText) textEntryView.findViewById(R.id.pkEditText);
            Provisioning.getInstance(getApplicationContext()).setPassPhraseForContainer(deviceId, input.getText().toString());
            AlertDialog.Builder alertSavedKeyDialog = new AlertDialog.Builder(PersonalKeysSettingsActivity.this)
            .setCancelable(false)
            .setTitle(deviceTitle)
            .setMessage(R.string.private_key_save_confirmation)
            .setIcon(getResources().getDrawable(R.drawable.settings))  //TODO: replace right icon
            .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener(){
                public void onClick(DialogInterface dialog, int id)
                {
                    dialog.dismiss();
                }
            });
            
            
            // Show Dialog only when we have any input text
            if ( ! (input.getText().toString().equals("")))
            {
                alertSavedKeyDialog.show();
            }
            
            list.setAdapter(view_list_adapter);
           } 
        }); 

         alertPrivateKeyEditDialog.setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() { 
          public void onClick(DialogInterface dialog, int whichButton) { 
            // Canceled. 
          } 
        }); 
        

        checkbox.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on clicks, depending on whether it's now checked
                if (((CheckBox) v).isChecked() && null != input) {
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);  
                    if(input.length() != 0)
                    {
                         input.setSelection(input.length());  // cursor at end of passphrase
                    }
                } else
                {
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    if(input.length() != 0)
                    {
                         input.setSelection(input.length());  // cursor at end of passphrase
                    }
                }
            }
        });
        alertPrivateKeyEditDialog.show();
    }

    private void showPrivateKeyEraseAlertDialog(final String deviceTitle, final String deviceId) {
           AlertDialog.Builder alertEraseKeyDialog = new AlertDialog.Builder(PersonalKeysSettingsActivity.this)
          .setCancelable(true)
          .setTitle(deviceTitle)
          .setMessage(R.string.private_key_erase_warning)
          .setIcon(getResources().getDrawable(R.drawable.warning))  
          .setPositiveButton(R.string.yes_button_text, new DialogInterface.OnClickListener(){
              public void onClick(DialogInterface dialog, int id)
              {
                    Provisioning.getInstance(getApplicationContext()).setPassPhraseForContainer(deviceId, "");    
                    list.setAdapter(view_list_adapter);
              }
          })
          .setNegativeButton(R.string.no_button_text, new DialogInterface.OnClickListener() { 
          public void onClick(DialogInterface dialog, int id) { 
                   // Canceled. 
                 dialog.dismiss();
              } 
          }); 
          alertEraseKeyDialog.show();
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_HOME, 0, getResources().getString(R.string.menu_home)).setIcon(R.drawable.mymozy);
        menu.add(0, MENU_HELP, 1, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_FILES, 2, getResources().getString(R.string.menu_files)).setIcon(R.drawable.allfiles);
        return true;
    }


    private void buildList() {
        
        view_list_adapter = new PersonalKeysSettingsViewListAdapter(this, encryptedDeviceList);
        
        // The order that these views are added is the order that they will be displayed on the screen
        for(int i = 0; i < this.encryptedDeviceList.size(); i++)
        {
            view_list_adapter.addView(i);  
        }
        
        list = (ListView)findViewById(R.id.list);
        list.setAdapter(view_list_adapter);
    }
    // @override

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    }
   
    
    class PersonalKeysSettingsViewListAdapter extends BaseAdapter {

        private final ArrayList<Integer> ids;
        private Context context;
        private ArrayList<Object>  deviceList;

        public PersonalKeysSettingsViewListAdapter(Context context, ArrayList<Object>  deviceList) {
            ids = new ArrayList<Integer>();
            this.context = context;
            this.deviceList = deviceList;
        }

        public int addView(int id)
        {
            int position = ids.size();
            ids.add(Integer.valueOf(id));
            return position;
        }

        public Object getItem(int position) {
            return null;
        }

        public int getId(int position) {
            return ids.get(position);
        }

        public int getCount() {
            return ids.size();
        }


        @Override
        public int getViewTypeCount() {
            return ids.size();
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int itemId = ids.get(position);

           view = inflater.inflate(R.layout.private_key_list_item_layout_main_settings, null);
           TextView title = (TextView) view.findViewById(R.id.name);
           title.setText(((Device )this.deviceList.get(itemId)).getTitle());
           TextView summary = (TextView) view.findViewById(R.id.subtitle);
           summary.setText(this.context.getResources().getString(R.string.private_key_settings_container_subtitle));
           
           
           ImageView imageView = (ImageView) view.findViewById(R.id.imageView);
           
           Device d = (Device )this.deviceList.get(itemId);
           
           String passphrase = Provisioning.getInstance(getApplicationContext()).getPassPhraseForContainer(d.getId());

           if (null != imageView)
           {
               if(d.getEncrypted() && passphrase != null &&  passphrase.length() != 0)   // Passphrase already set for container (visited settings first)
               {
                   imageView.setVisibility(View.VISIBLE);
               }
               else
               {
                   imageView.setVisibility(View.INVISIBLE);
               }
           }
           
           return view;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }
}
