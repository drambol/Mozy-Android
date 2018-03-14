package com.mozy.mobile.android.activities;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.startup.FirstRun;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ResultCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.tasks.RemoveDecryptedFilesTask;
import com.mozy.mobile.android.activities.upload.UploadManager;
import com.mozy.mobile.android.activities.upload.UploadStatusActivity;
import com.mozy.mobile.android.catch_release.CatchAndReleaseSettingsActivity;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;


public class MainSettingsActivity extends SecuredActivity
{
    // ids of the particular actions:
    private static final int SETTINGS_LIST_LABEL = 0;
    private static final int SETTINGS_LIST_ABOUT = 1;
    private static final int SETTINGS_LIST_PASSCODE_LOCK = 2;
    private static final int SETTINGS_LIST_PASSCODE_CHANGE = 3;
    private static final int SETTINGS_LIST_PERSONAL_KEYS = 4;
    private static final int SETTINGS_LIST_LOGOFF = 5;
    private static final int SETTINGS_LIST_FEEDBACK = 6;
    private static final int SETTINGS_LIST_UPLOAD_SETTINGS = 7;
    private static final int SETTINGS_LIST_UPLOAD_STATUS = 8;
    private static final int SETTINGS_LIST_UPLOAD_SPACE_USED = 9;
    private static final int SETTINGS_LIST_USER = 10;
//    private static final int SETTINGS_DISPLAY_HIDDEN_FILES = 11;

    /**
     * The action that shall be sent whenever settings interval changes
     */
    public static final String SETTING_BACKUP_INTERVAL_CHANGED_ACTION = "se.tactel.mmc.action.SETTING_BACKUP_INTERVAL_CHANGED_ACTION";

    /**
     * Dialog ID
     */
    private static final int LOGOFF_PROMPT_DIALOG_ID = FIRST_USER_DIALOG;

    private static final int[] ERROR_IDS = new int[] {ErrorManager.ERROR_TYPE_GENERIC};

    /**
     * Adapters
     */
    private MainSettingsViewListAdapter view_list_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

        setBarTitle(R.string.menu_settings);
        buildList();
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



    private boolean pinExists()
    {
        return Provisioning.getInstance(getApplicationContext()).getPin().length() == 4;
    }

    private void buildList() {

        view_list_adapter = new MainSettingsViewListAdapter(this);

        // The order that these views are added is the order that they will be displayed on the screen
        if (SystemState.isPasscodeEnabled())
        {
            view_list_adapter.addView(SETTINGS_LIST_LABEL,
                      R.string.settings_main_label_security,
                      0);
            view_list_adapter.addView(SETTINGS_LIST_PASSCODE_LOCK,
                                    R.string.settings_main_item_title_passcode,
                                    R.string.settings_main_item_subtitle_passcode);
            view_list_adapter.addView(SETTINGS_LIST_PASSCODE_CHANGE,
                                    R.string.settings_main_item_title_passcode_change,
                                    R.string.settings_main_item_subtitle_passcode_change);
            
            
            ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();
            
            if(encryptedDeviceList != null && encryptedDeviceList.isEmpty() == false && (SystemState.isManagedKeyEnabled(this.getApplicationContext()) == false))
            {
                view_list_adapter.addView(SETTINGS_LIST_PERSONAL_KEYS,
                                    R.string.settings_main_item_title_personal_keys,
                                    R.string.settings_main_item_subtitle_personal_keys);
            }
            
//            view_list_adapter.addView(SETTINGS_DISPLAY_HIDDEN_FILES,
//                    R.string.settings_hidden_files_title,
//                    R.string.settings_hidden_files_summary);
        }


        if (SystemState.isSyncEnabled())
        {
            view_list_adapter.addView(SETTINGS_LIST_LABEL,
                                      R.string.settings_main_label_upload,
                                      0);
            view_list_adapter.addView(SETTINGS_LIST_UPLOAD_SETTINGS,
                                                                 R.string.settings_main_item_title_settings,
                                                                 R.string.settings_main_item_subtitle_settings);
            view_list_adapter.addView(SETTINGS_LIST_UPLOAD_STATUS,
                                      R.string.settings_main_item_title_status,
                                      R.string.settings_main_item_subtitle_status);
            
            if (SystemState.isSpaceUsedEnabled())
            {
                view_list_adapter.addView(SETTINGS_LIST_UPLOAD_SPACE_USED,
                                      R.string.settings_main_item_title_space,
                                      R.string.settings_main_item_subtitle_space);
            }
        }

        view_list_adapter.addView(SETTINGS_LIST_LABEL,
                                  R.string.app_name,
                                  0);
        view_list_adapter.addView(SETTINGS_LIST_ABOUT,
                                    R.string.settings_main_item_title_about,
                                    R.string.settings_main_item_subtitle_about);

        view_list_adapter.addView(SETTINGS_LIST_USER,
                                    R.string.settings_main_item_title_user,
                                    0);
        view_list_adapter.addView(SETTINGS_LIST_LOGOFF,
                                                       R.string.settings_main_item_title_logoff,
                                                       R.string.settings_main_item_subtitle_logoff);
        // Out for v 1.0
        /*view_list_adapter.addView(SETTINGS_LIST_FEEDBACK,
                                  R.string.settings_main_item_title_feedback,
                                  R.string.settings_main_item_subtitle_feedback);*/


        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(view_list_adapter);

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

        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> list, View view, int position, long id)
            {
                Intent intent;
                updateAlarm();

                switch(view_list_adapter.getId(position)){
                    case SETTINGS_LIST_LABEL:
                        break;
                    case SETTINGS_LIST_ABOUT: // About
                            intent = new Intent(getApplicationContext(), AboutActivity.class);
                            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                        break;
                    case SETTINGS_LIST_PASSCODE_LOCK:
                            View child = list.getChildAt(position);
                            if (child != null)
                            {
                                CheckBox checkBox = (CheckBox) child.findViewById(R.id.set_chk);
                                boolean newCheckedSetting = !checkBox.isChecked();
                                if (newCheckedSetting)
                                {
                                    if (!pinExists())
                                    {
                                        intent = new Intent(getApplicationContext(), PinManActivity.class);
                                        intent.putExtra(PinManActivity.ACTION, PinManActivity.ACTION_SETNEW);
                                        startActivityForResult(intent, RequestCodes.SETTINGS_CHANGE_PIN);
                                    }
                                    else
                                    {
                                        view_list_adapter.setPasscodeEnabled(true);
                                        Provisioning.getInstance(getApplicationContext()).setSecurityMode(true);
                                        InactivityAlarmManager.getInstance(getApplicationContext()).activate();
                                        checkBox.setChecked(newCheckedSetting);
                                    }
                                }
                                else
                                {
                                    intent = new Intent(getApplicationContext(), PinManActivity.class);
                                    intent.putExtra(PinManActivity.ACTION, PinManActivity.ACTION_VALIDATE);
                                    startActivityForResult(intent, RequestCodes.VALIDATE_PIN);
                                }
                            }
                        break;
//                    case SETTINGS_DISPLAY_HIDDEN_FILES:
//                        View childHiddenFiles = list.getChildAt(position);
//                        if (childHiddenFiles != null)
//                        {
//                            CheckBox checkBox = (CheckBox) childHiddenFiles.findViewById(R.id.set_chk);
//                            boolean newCheckedSetting = !checkBox.isChecked();
//                            view_list_adapter.setHiddenFilesEnabled(newCheckedSetting);
//                            Provisioning.getInstance(getApplicationContext()).setHiddenFilesMode(newCheckedSetting);
//                            InactivityAlarmManager.getInstance(getApplicationContext()).activate();
//                            checkBox.setChecked(newCheckedSetting);
//                        }
//                        break;
                    case SETTINGS_LIST_PASSCODE_CHANGE:
                        intent = new Intent(getApplicationContext(), PinManActivity.class);
                        int pinAction = pinExists() ? PinManActivity.ACTION_RESET : PinManActivity.ACTION_SETNEW;
                        intent.putExtra(PinManActivity.ACTION, pinAction);
                        startActivityForResult(intent, RequestCodes.SETTINGS_CHANGE_PIN);
                        break;
                    case SETTINGS_LIST_PERSONAL_KEYS:
                         intent = new Intent(getApplicationContext(), PersonalKeysSettingsActivity.class);
                         startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                        break;
                    case SETTINGS_LIST_LOGOFF:
                            showDialog(MainSettingsActivity.LOGOFF_PROMPT_DIALOG_ID);
                        break;
                    case SETTINGS_LIST_FEEDBACK:
                            MainSettingsActivity.this.SendFeedback();
                        break;
                    case SETTINGS_LIST_UPLOAD_SETTINGS:
                            intent = new Intent(getApplicationContext(), CatchAndReleaseSettingsActivity.class);
                            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                        break;
                    case SETTINGS_LIST_UPLOAD_STATUS:
                            intent = new Intent(getApplicationContext(), UploadStatusActivity.class);
                            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                        break;
                    case SETTINGS_LIST_UPLOAD_SPACE_USED:
                            intent = new Intent(getApplicationContext(), StorageUsedActivity.class);
                            startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);
                        break;
                }
            }
        });
    }

    // @override
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog returnValue = null;

        if (id == MainSettingsActivity.LOGOFF_PROMPT_DIALOG_ID)
        {
            String titleStr = this.getResources().getString(R.string.settings_main_logoff_prompt_title);
            
            
            // anything in the 3 queues
            if(UploadManager.getNumPendingFiles() > 0 || UploadManager.getNumPendingFilesForQueue(UploadManager.sFailedUploadsQueue) > 0)
            {
                titleStr = this.getResources().getString(R.string.settings_main_logoff_prompt_title_long);
                titleStr = titleStr.replace("$SYNC", this.getResources().getString(R.string.sync_title));
            }
                
            returnValue = new AlertDialog.Builder(this)
            .setMessage(titleStr)
            .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    MainSettingsActivity.signOff(MainSettingsActivity.this);
                }
            })
                .setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        // Do nothing
                    }
                })
            .create();
        }
        return returnValue;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.REQUEST_START_ACTIVITY) {
            //
        }
        else if (requestCode == RequestCodes.VALIDATE_PIN)
        {
            if (resultCode == ResultCodes.PIN_OK)
            {
                Provisioning.getInstance(getApplicationContext()).setSecurityMode(false);
                Provisioning.getInstance(getApplicationContext()).setPin("");
                InactivityAlarmManager.getInstance(getApplicationContext()).deactivate();
                view_list_adapter.setPasscodeEnabled(false);
                view_list_adapter.notifyDataSetChanged();
            }
        }
        else if(requestCode == RequestCodes.SETTINGS_CHANGE_PIN)
        {
            switch(resultCode)
            {
                case ResultCodes.PIN_OK:
                    view_list_adapter.setPasscodeEnabled(true);
                    view_list_adapter.notifyDataSetChanged();
                    break;
                case ResultCodes.PIN_NEW:
                    Intent intent = new Intent(getApplicationContext(), PinManActivity.class);
                    int pinAction = PinManActivity.ACTION_SETNEW;
                    intent.putExtra(PinManActivity.ACTION, pinAction);
                    startActivityForResult(intent, RequestCodes.SETTINGS_CHANGE_PIN);
                    break;
                case ResultCodes.PIN_NEWTWO:
                    intent = new Intent(getApplicationContext(), PinManActivity.class);
                    pinAction = PinManActivity.ACTION_SETNEWTWO;
                    intent.putExtra(PinManActivity.ACTION, pinAction);
                    startActivityForResult(intent, RequestCodes.SETTINGS_CHANGE_PIN);
                    break;
            }
        }
    }

    @Override
    protected int[] getErrorIds()
    {
        return ERROR_IDS;
    }

    @Override
    protected int handleError(int errorCode, int errorType)
    {
        switch (errorCode)
        {
            case ErrorCodes.ERROR_PIN_NOT_YET_INITIALIZED:
                return ACTION_DIALOG_ERROR;
        }
        return ACTION_NONE;
    }

    private void SendFeedback()
    {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_email)});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
        StringBuilder feedback_details = new StringBuilder();
        feedback_details.append("OS VERSION = Android ").append(android.os.Build.VERSION.RELEASE);
        feedback_details.append("\nMODEL = ").append(android.os.Build.MODEL);
        String version = "";
        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
        }
        feedback_details.append("\nCLIENT VERSION = ").append(version);
        feedback_details.append("\nIDENTITY = ").append(Provisioning.getInstance(getApplicationContext()).getEmailId());
        emailIntent.putExtra(Intent.EXTRA_TEXT, feedback_details.toString());
        emailIntent.setType("text/*");


        //check if there is any application which can handle this intent.
        ResolveInfo emailResolveInfo = getPackageManager().resolveActivity(emailIntent, PackageManager.MATCH_DEFAULT_ONLY);
        if(emailResolveInfo == null)
        {
            LogUtil.debug(this, "No application is configured to send emails");
            ErrorManager.getInstance().reportError(ErrorCodes.ERROR_EMAIL_APPLICATION_NOT_CONFIGURED, ErrorManager.ERROR_TYPE_GENERIC);
            return;
        }
        LogUtil.debug(this, "Application used for sending email:"+emailResolveInfo.activityInfo.name);

        String strLogCatFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/logcat.txt";
        String strZipFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/debuglog.zip";

        File logcatFile = new File(strLogCatFilePath);

        FileOutputStream logcatOutStream = null;
        Process mLogcatProc = null;
        BufferedReader reader = null;

        boolean bLoggingSuccessful = true;

        try
        {
            logcatOutStream = new FileOutputStream(logcatFile);
            mLogcatProc  = Runtime.getRuntime().exec("logcat -d");
            reader = new BufferedReader(new InputStreamReader(mLogcatProc.getInputStream()));

            byte[] separator = System.getProperty("line.separator").getBytes();
            String line;

            while((line = reader.readLine()) != null)
            {
                logcatOutStream.write(line.getBytes());
                logcatOutStream.write(separator);
            }
        }
        catch (IOException e)
        {
            bLoggingSuccessful = false;
            e.printStackTrace();
        }
        finally
        {
            if (logcatOutStream != null)
            {
                try
                {
                    logcatOutStream.close();
                }
                catch (IOException e)
                {
                }
            }
            if (reader!=null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                }
            }
        }
        ArrayList<String> listOfFiles = new ArrayList<String>();
        if(bLoggingSuccessful)
        {
            listOfFiles.add(strLogCatFilePath);
        }

        if (zipFiles(listOfFiles,strZipFilePath))
        {
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+strZipFilePath));
            emailIntent.setType("application/zip");
        }

        startActivity(Intent.createChooser(emailIntent, getString(R.string.feedback_chooser_title)));
    }

    private boolean zipFiles(ArrayList<String> listOfFiles, String strZipFilePath)
    {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        int Buffer = 2048;
        try
        {
             fos = new FileOutputStream(strZipFilePath);
             zos = new ZipOutputStream(new BufferedOutputStream(fos));

             byte[] data = new byte[Buffer];

             for(int i=0; i < listOfFiles.size(); i++)
             {
                 ZipEntry entry = new ZipEntry(listOfFiles.get(i));
                 zos.putNextEntry(entry);

                 FileInputStream fis = new FileInputStream(listOfFiles.get(i));
                 BufferedInputStream bis = new BufferedInputStream(fis,Buffer);
                 int count;
                 while( (count = bis.read(data,0,Buffer)) != -1)
                 {
                     zos.write(data,0,count);
                 }
                 bis.close();
             }
             return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if(zos != null)
            {
                try
                {
                    zos.close();
                }
                catch (IOException e)
                {
                }
            }
        }

        return false;
    }
    
    
    /**
     * 
     */
    public static void remoteWipeAndSignOff(Activity activity) {
        String filePath = FileUtils.getStoragePathForMozy();
        if(filePath != null)
        {
             LocalFile localFile  = new LocalFile(filePath);
             deleteRecursive(localFile.file);          
        }  
        
        MainSettingsActivity.signOff(activity);
        
        if(SystemState.mozyFileDB != null)
            SystemState.mozyFileDB.clearDB();
        
        Provisioning.getInstance(activity.getApplicationContext()).clearEmailIds();
    }
    
    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }


    /**
     * 
     */
    public static void signOff(Activity activity) {
        cleanupOnSignedOut(activity);
        
        // Clear out any activities
        if(activity instanceof SecuredActivity)
            ((SecuredActivity) activity).clearActivityQueue();
        
        NavigationTabActivity.clear();

        Intent intent = new Intent(activity.getApplicationContext(), FirstRun.class);
        if(intent != null)
        {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
        }

        activity.finish();
    }

    /**
     * @param activity
     */
    public static void cleanupOnSignedOut(Activity activity) {
        
        final Context context = activity.getApplicationContext();
        UploadManager.cleanupUploadSettingsAndQueue(activity);
        
        RemoveDecryptedFilesTask.removeExpiredDecryptedFiles();
        
        // Revoke the token
        if(Provisioning.getInstance(context).getMipAccountToken() != null)
        {
            // do it asynchronously
            
            Handler handler = new Handler(context.getMainLooper());
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    ServerAPI.getInstance(context).deleteToken(Provisioning.getInstance(context).getMipAccountToken());
                }
            });
        }
            

        // Clear out the users log-in data and take them back to the log-in screen.
        Provisioning.getInstance(context).clearData();
        SystemState.setDeviceList(null);  
        ServerAPI.getInstance(context).setCloudDeviceLink(null);
        
        SystemState.setManualUploadEnabled(false, context); // resetting it back

        if(SystemState.mozyFileDB != null)
            SystemState.mozyFileDB.close();
        
        SystemState.setEncryptedContainerAccessTable(null);
        
        InactivityAlarmManager.getInstance(context).deactivate();
    }

   
    
    /**
     * 
     */
    public static void goToMainSettings(Activity activity) {
        Intent settingsIntent = new Intent(activity.getApplicationContext(), MainSettingsActivity.class);
        activity.startActivityForResult(settingsIntent, RequestCodes.REQUEST_START_ACTIVITY);
    }
    
    
    /**
     * 
     */
    public static void goToHelp(Activity activity) {
        String strHelpUrl = SystemState.getHelpUrl(activity.getApplicationContext());
        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        myIntent.setData(Uri.parse(strHelpUrl));
        activity.startActivity(myIntent);
    } 


    class MainSettingsViewListAdapter extends BaseAdapter {

        /*
        static final int SETTINGS_CATEGORY = 0;
        static final int SETTINGS_RADIO_BUTTON = 1;
        static final int SETTINGS_BUTTON = 2;
        static final int SETTINGS_CHECKBOX = 3;
        static final int SETTINGS_CHECKBOX_PIN = 4;
        static final int SETTINGS_CHANGEPIN_BUTTON = 5;
        */

        private final ArrayList<Integer> ids;
        private final Vector<Integer> res_title;
        private final Vector<Integer> res_summary;
        private Context context;
        private boolean mPasscodeEnabled;
       // private boolean mHiddenFilesEnabled;

        public MainSettingsViewListAdapter(Context context) {
            ids = new ArrayList<Integer>();
            res_title = new Vector<Integer>();
            res_summary = new Vector<Integer>();
            this.context = context;
        }

        public int addView(int id, int resource_title, int resource_summary)
        {
            int position = ids.size();
            ids.add(Integer.valueOf(id));
            res_title.add(Integer.valueOf(resource_title));
            res_summary.add(Integer.valueOf(resource_summary));
            return position;
        }

        public void updateResourceSummary(int position, int resource_summary) {
            res_summary.set(position, resource_summary);
        }

        public Object getItem(int position) {
            // if (position < is_checked.size()){return is_checked.get(position);}
            return null;
        }

        public int getId(int position) {
            return ids.get(position);
        }

        public int getCount() {
            return ids.size();
        }

        public void setPasscodeEnabled(boolean enabled)
        {
            mPasscodeEnabled = enabled;
        }
        
//        public void setHiddenFilesEnabled(boolean enabled)
//        {
//            mHiddenFilesEnabled = enabled;
//        }

        @Override
        public int getViewTypeCount() {
            return ids.size();
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return (ids.get(position) != SETTINGS_LIST_LABEL);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            int itemId = ids.get(position);

            if (itemId == SETTINGS_LIST_LABEL)
            {
                // view = inflater.inflate(R.layout.settings_list_header, null);
                view = inflater.inflate(R.layout.settings_subheader_layout, null);
                TextView textView = (TextView)view.findViewById(R.id.activity_title);
                textView.setText(context.getString(res_title.get(position)));
            }
            else if (itemId == SETTINGS_LIST_PASSCODE_LOCK)
            {
                view = inflater.inflate(R.layout.settings_checkbox_2row, null);
                TextView toggleTitleTextView = (TextView) view.findViewById(R.id.check_title);
                toggleTitleTextView.setText(res_title.get(position));
                TextView toggleSummaryTextView = (TextView) view.findViewById(R.id.check_summary);
                toggleSummaryTextView.setText(context.getString(res_summary.get(position)));
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.set_chk);
                mPasscodeEnabled = Provisioning.getInstance(getApplicationContext()).getSecurityMode();
                checkBox.setChecked(mPasscodeEnabled);
            }
//            else if (itemId == SETTINGS_DISPLAY_HIDDEN_FILES)
//            {
//                view = inflater.inflate(R.layout.settings_checkbox_2row, null);
//                TextView toggleTitleTextView = (TextView) view.findViewById(R.id.check_title);
//                toggleTitleTextView.setText(res_title.get(position));
//                TextView toggleSummaryTextView = (TextView) view.findViewById(R.id.check_summary);
//                toggleSummaryTextView.setText(context.getString(res_summary.get(position)));
//                CheckBox checkBox = (CheckBox) view.findViewById(R.id.set_chk);
//                mHiddenFilesEnabled = Provisioning.getInstance(getApplicationContext()).getHiddenFilesMode();
//                checkBox.setChecked(mHiddenFilesEnabled);
//            }
            else
            {
                view = inflater.inflate(R.layout.list_item_layout_main_settings, null);
                TextView title = (TextView) view.findViewById(R.id.name);
                title.setText(context.getString(res_title.get(position)));
                TextView summary = (TextView) view.findViewById(R.id.subtitle);
                if (itemId != SETTINGS_LIST_USER)
                {
                    summary.setText(context.getString(res_summary.get(position)));
                }
                else
                {
                    summary.setText(Provisioning.getInstance(getApplicationContext()).getEmailId());
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
