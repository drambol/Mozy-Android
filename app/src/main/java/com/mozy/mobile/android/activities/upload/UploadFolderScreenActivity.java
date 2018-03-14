package com.mozy.mobile.android.activities.upload;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.text.InputType;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.BarActivity;

import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.activities.adapters.UploadFolderListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.startup.StartupHelper;
import com.mozy.mobile.android.activities.tasks.CreateUploadedFolderTask;
import com.mozy.mobile.android.activities.tasks.GetUploadedFileListTask;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;




public class UploadFolderScreenActivity  extends SecuredActivity
{
    /**
     * Interface to be implemented by clients interested in listening for
     * data updates from this adapter
     */
    public static interface ListAdapterDataListener
    {
        void onDataRetrieved(UploadFolderListAdapter callingAdapter, int errorCode, ArrayList<Object> list);
    }
    
    public static interface UploadFolderCreateListener
    {
        void onCompleted(UploadFolderListAdapter callingAdapter, int errorCode);
    }
    
    private ListView listView;
    private UploadFolderListAdapter listAdaptor;
    private ListAdapterDataListener listener;
    private UploadFolderCreateListener folderCreatelistener;
    private String mSyncFolder;
    private String mSyncLink;
    private ArrayList<Object> mList;
    
    private static final int DIALOG_LOADING_ID = 1;
    
    private static final String FOLDER_NAME_PATTERN ="[^\\/:*?\"<>|]*";


    @Override 
    public void onCreate(Bundle savedInstanceState) 
    {           
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) 
        {
            LogUtil.debug(this, "Loading extras from intent:");
            mSyncFolder = extras.getString("syncFolder");
        }
        
        setContentView(BarActivity.NO_BAR, R.layout.upload_folder_list_item_layout_generic);
        
        TextView mCurrentUploadFolderTextView = (TextView)findViewById(R.id.currentUploadFolderTextView);
        
        if(mSyncFolder == null)
        {
            mCurrentUploadFolderTextView.setText(getResources().getString(R.string.sync_title));
        }
        else
        {
            String folderName = mSyncFolder;
            
            folderName = FileUtils.getFolderNameFromPath(folderName);
            
            mCurrentUploadFolderTextView.setText(folderName);
        }

        
        listAdaptor = (UploadFolderListAdapter) getLastNonConfigurationInstance();

        this.listView  = (ListView) findViewById(R.id.generic_list);
        
        listener = new ListAdapterDataListener() 
        {
            @Override
            public void onDataRetrieved(UploadFolderListAdapter callingAdapter, int errorCode, ArrayList<Object> list) 
            {
                UploadFolderScreenActivity.this.updateViewForNumItems(callingAdapter.getCount());
                
                mList = list;
                
                if (!isFinishing()) 
                {
                    removeDialog(DIALOG_LOADING_ID);
            
                    if (errorCode !=  ServerAPI.RESULT_OK)
                    {
                        UploadFolderScreenActivity.this.handlingError = true;
                        Dialog errDialog = createErrorDialog(errorCode);
                        errDialog.show();
                    }
                }
            }
        };
        
        folderCreatelistener = new UploadFolderCreateListener() 
        {
            @Override
            public void onCompleted(UploadFolderListAdapter callingAdapter, int errorCode) 
            {
                UploadFolderScreenActivity.this.updateViewForNumItems(callingAdapter.getCount());
                
                if (!isFinishing()) 
                {
                    removeDialog(DIALOG_LOADING_ID);
                    
                    if (errorCode !=  ServerAPI.RESULT_OK)
                    {
                        UploadFolderScreenActivity.this.handlingError = true;
                        Dialog errDialog = createErrorDialog(errorCode);
                        errDialog.show();
                    }
                    else
                    {
                        showDialog(DIALOG_LOADING_ID);
                        onListPrepared();
                    }
                }
            }
        };

        if (listAdaptor == null)
        {
            showDialog(DIALOG_LOADING_ID);
            listAdaptor = new UploadFolderListAdapter(getApplicationContext());
        }
        
        UploadFolderScreenActivity.this.updateViewForNumItems(listAdaptor.getCount());
        
        
        listView.setOnItemClickListener(new OnItemClickListener() 
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
            {
                final CloudFile d = (CloudFile)listAdaptor.getItem(position);
                
                if (d != null && d instanceof Directory)
                {       
                    Intent intent = new Intent(UploadFolderScreenActivity.this, UploadFolderScreenActivity.class);

                    if(d.getName() != null)
                    { 
                        String selectedFolder = null;
                        if(UploadFolderScreenActivity.this.mSyncFolder != null)
                            selectedFolder = UploadFolderScreenActivity.this.mSyncFolder + d.getName() + "/";
                        else
                            selectedFolder = d.getName() + "/";
                        
                        intent.putExtra("syncFolder", selectedFolder);
                    }
                    

                    startActivityForResult(intent, RequestCodes.UPLOAD_FOLDER_PICKER_SCREEN_ACTIVITY_RESULT);
                }
            }
        });
        
        
        final  ImageView v  = (ImageView) findViewById(R.id.FolderImageView);
        v.setImageResource(R.drawable.create_folder_plus);
        v.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) 
            {
                createNewFolderDialog();
            }

            /**
             * 
             */
            public void createNewFolderDialog() {
                AlertDialog.Builder builder = new AlertDialog.Builder(UploadFolderScreenActivity.this);
                String titleStr = UploadFolderScreenActivity.this.getResources().getString(R.string.upload_new_sync_folder_title);
                titleStr = titleStr.replace("$SYNC", UploadFolderScreenActivity.this.getResources().getString(R.string.sync_title));
                builder.setTitle(titleStr);
                builder.setMessage(getResources().getString(R.string.upload_new_sync_folder_message));
                builder.setCancelable(false);
                // Set up the input
                final EditText input = new EditText(UploadFolderScreenActivity.this);
                // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton(getResources().getString(R.string.ok_button_text), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        String newDirName = input.getText().toString();


                        if(newDirName != null && newDirName.equalsIgnoreCase("") == false)
                        {
                            boolean dupFound = false;
                            if(mList != null)
                            {
                                for (int i = 0; i < mList.size(); i++)
                                {
                                    if(mList.get(i) != null)
                                    {
                                        if(((CloudFile) mList.get(i)).getTitle().equalsIgnoreCase(newDirName))
                                        {
                                            dupFound = true;
                                            break;
                                        }
                                    }
                                }
                            }
                                
                            if(dupFound)
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(UploadFolderScreenActivity.this);
                                builder.setTitle(getResources().getString(R.string.upload_new_sync_folder_duplicate_title));
                                builder.setMessage(getResources().getString(R.string.upload_new_sync_folder_duplicate_mesg));
                                builder.setCancelable(false);
                                builder.setPositiveButton(getResources().getString(R.string.ok_button_text), new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int id)
                                    {
                                        dialog.dismiss();
                                        createNewFolderDialog();
                                     }
                                 });
                                builder.create();
                                builder.show();
                            }
                            else
                            {
                                if(!validateFolderName(newDirName))
                                {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(UploadFolderScreenActivity.this);
                                    builder.setTitle(getResources().getString(R.string.upload_new_sync_folder_invalid_name_title));
                                    builder.setMessage(getResources().getString(R.string.upload_new_sync_folder_invalid_name_mesg));
                                    builder.setCancelable(false);
                                    builder.setPositiveButton(getResources().getString(R.string.ok_button_text), new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialog, int id)
                                        {
                                            dialog.dismiss();
                                            createNewFolderDialog();
                                         }
                                     });
                                    builder.create();
                                    builder.show();
                                }
                                else
                                {
                                    dialog.dismiss();
                                    showDialog(DIALOG_LOADING_ID);
                                    
                                    if(mSyncFolder == null)
                                    {
                                        mSyncFolder = "";
                                    }
                                    new CreateUploadedFolderTask(getApplicationContext(), newDirName, mSyncFolder, listAdaptor, folderCreatelistener).execute();
                                }
                            }
                        }
                    }
                });
                builder.setNegativeButton(getResources().getString(R.string.cancel_button_text), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        dialog.dismiss();
                     }
                 });

               builder.create();
               builder.show();
            }
        });
        
        
        final Button neg_button = (Button) findViewById(R.id.negative_button);
        neg_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) 
            {
                UploadFolderScreenActivity.this.setResult(ServerAPI.RESULT_CANCELED);
                UploadFolderScreenActivity.this.finish();
            }
        });

        final Button choose_button = (Button) findViewById(R.id.choose_button);
        choose_button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) 
            {
                if(mSyncFolder == null)
                {
                    UploadManager.uploadManualDestFolder = "";
                }
                else
                {
                    UploadManager.uploadManualDestFolder = UploadFolderScreenActivity.this.mSyncFolder;
                }
                
                UploadFolderScreenActivity.this.setResult(ServerAPI.RESULT_CANCELED);
                UploadFolderScreenActivity.this.finish();
              
            }
        });
        
        this.listView.setAdapter(this.listAdaptor);
        
        listView.setSelection(0);   
    }
    
    
    public static boolean validateFolderName(String folderName)
    {
        if(StartupHelper.IsStringNullOrEmtpy(folderName))
            return false;
        Pattern pattern = Pattern.compile(FOLDER_NAME_PATTERN);
        if(!pattern.matcher(folderName.trim()).matches())
            return false;
        return true;
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == ServerAPI.RESULT_CANCELED)
        {
            if(requestCode == RequestCodes.UPLOAD_FOLDER_PICKER_SCREEN_ACTIVITY_RESULT)
            {
                UploadFolderScreenActivity.this.setResult(ServerAPI.RESULT_CANCELED);
                this.finish();
            }
            else
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    
    public void updateViewForNumItems(int numItems)
    {  
        TextView textView = (TextView) findViewById(R.id.notification);
        
        findViewById(R.id.notification).setVisibility(numItems > 0 ? View.GONE:View.VISIBLE);
        
        if(numItems <= 0)
            textView.setText(R.string.no_items);
        
        findViewById(R.id.footer_divider).setVisibility(numItems > 0 ? View.VISIBLE:View.GONE);
    }

   
    @Override
    public void onResume() {
        super.onResume();
        showDialog(DIALOG_LOADING_ID);
        onListPrepared();
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        this.listAdaptor = null;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        
        if (id == DIALOG_LOADING_ID)
        {
            ProgressDialog loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(
                    getResources().getString(R.string.progress_bar_loading));

            loadingDialog.setIndeterminate(true);
            loadingDialog.setOnKeyListener(new DialogInterface.OnKeyListener()
            {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event)
                {
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        UploadFolderScreenActivity.this.finish();
                    }
                    return true;
                }
            });
            return loadingDialog;
        }
        return super.onCreateDialog(id);
    }
    
    public void onListPrepared()
    {        
        if(mSyncLink == null)
        {
            Device syncDevice = SystemState.getSyncDevice();
            
            if(syncDevice != null)
            {
                mSyncLink = syncDevice.getLink();
            }
            
            if(this.mSyncFolder != null)
            {
                this.mSyncLink = this.mSyncLink + URLEncoder.encode(this.mSyncFolder);
            }
        }
        
        if(this.mSyncLink != null)
        {
            new GetUploadedFileListTask(getApplicationContext(), this.mSyncLink, this.listAdaptor, this.listener).execute();
        }
    }
    
}
