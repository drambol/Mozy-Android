package com.mozy.mobile.android.catch_release;


import java.util.ArrayList;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.SecuredActivity;
import com.mozy.mobile.android.activities.helper.UploadSettings;
import com.mozy.mobile.android.utils.LogUtil;



public class CatchAndReleaseSettingsActivity extends SecuredActivity
{
    private Toast automatic_toast = null;

    private int videoUploadSettingsAutoPosition;
    private int photoUploadSettingsAutoPosition;
    private int offWhenRoamingPosition=-1;
    private int onlyOnWifiPosition;

    private static final int UPLOAD_MANUAL_HELP = FIRST_USER_DIALOG;
    private UploadSettings settings = null;
    
    /**
     * Adapters
     */
    private UploadSettingsViewListAdapter viewListAdapter;

    // private boolean dirtyDialog = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings_layout);

        setBarTitle(R.string.settings_main_label_upload);

        readPreferences();

        try
        {
            buildList();
        }
        catch (Throwable t)
        {
            LogUtil.exception(this, "setReferencePosition", t);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // The 'Off When Roaming' check-box is dependent on phone system settings, so any time we get focus back on this screen
        // we need to make sure the check-box is in the correct state.
        this.viewListAdapter.updateAutomaticSection();
        this.viewListAdapter.processRoamingCheckBox(this.offWhenRoamingPosition);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.savePreferences();
        
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    // Remember this can be very delayed in when it is called.
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    // Hitting the 'back' key is the only way to leave this screen and remain inside the application
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && (event.getRepeatCount() == 0))
        {
            this.savePreferences();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void readPreferences()
    {
        if (null == settings)
            settings = new UploadSettings(getApplicationContext());
    }

    private void savePreferences()
    {
        settings.setUploadInitialized();
        settings.setPreferences(getApplicationContext());
    }


    public int getOffWhenRoamingPosition()
    {
        return this.offWhenRoamingPosition;
    }

    public int getOnlyOnWifiPosition()
    {
        return this.onlyOnWifiPosition;
    }

    private void buildList() {

        viewListAdapter = new UploadSettingsViewListAdapter(this);

        viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_LABEL,
                R.string.settings_upload_method,
                0,
                false);

        // Photo check-box group
        this.photoUploadSettingsAutoPosition = viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_CHECKBOX,
                                                         R.string.settings_option_auto_photos,
                                                         R.string.settings_option_auto_photo_summary,
                                                         this.settings.getPhotoUploadType());

        // Video check-box group
        this.videoUploadSettingsAutoPosition = viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_CHECKBOX,
                                                                         R.string.settings_option_auto_videos,
                                                                         R.string.settings_option_auto_video_summary,
                                                                         this.settings.getVideoUploadType());

        // Manual Upload Help section
        viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_ITEM,
                                R.string.settings_option_manual_upload_help,
                                R.string.settings_option_manual_upload_help_summary,
                                false);
        
        // Automatic Upload settings
        viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_LABEL,
                R.string.settings_upload_automatic_section,
                0,
                false);

        this.onlyOnWifiPosition = viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_CHECKBOX,
                                                            R.string.settings_option_wifi,
                                                            R.string.settings_option_wifi_summary,
                                                            this.settings.getOnlyOnWifi());
        
        this.offWhenRoamingPosition = viewListAdapter.addView(UploadSettingsViewListAdapter.SETTINGS_CHECKBOX,
                                                              R.string.settings_option_roaming,
                                                              R.string.settings_option_roaming_summary,
                                                              this.settings.getOffWhenRoaming());

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(viewListAdapter);

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
            public void onItemClick(AdapterView<?> list, View view, int position, long id)
            {
                CheckBox cb = null;
                View child = null;
                boolean checkboxIsChecked = false;

                updateAlarm();

                switch(position){
                case 1: // Automatic photo upload checkbox
                    // Change this checkbox state
                    child = list.getChildAt(position);
                    if (child != null)
                    {
                        cb = (CheckBox)child.findViewById(R.id.set_chk);
                        if (cb != null)
                        {
                            // toggle checkbox
                            checkboxIsChecked = !cb.isChecked();
                            cb.setChecked(checkboxIsChecked);
                        }
                    }

                    if (checkboxIsChecked)
                    {
                        CatchAndReleaseSettingsActivity.this.settings.setPhotoUploadType(true);
                        if (automatic_toast == null)
                        {
                            automatic_toast = Toast.makeText(CatchAndReleaseSettingsActivity.this,
                                                             R.string.settings_no_voice_calls,
                                                             Toast.LENGTH_LONG);
                        }
                        automatic_toast.show();
                    }
                    else
                    {
                        CatchAndReleaseSettingsActivity.this.settings.setPhotoUploadType(false);
                        if (automatic_toast != null)
                        {
                            automatic_toast.cancel();
                        }
                    }
                    viewListAdapter.updateAutomaticSection();
                    setPhotoUploadTypeInAdapter(CatchAndReleaseSettingsActivity.this.settings.getPhotoUploadType());
                break;
                case 2: // Automatic video upload checkbox
                    // Change this checkbox state
                    child = list.getChildAt(position);
                    if (child != null)
                    {
                        cb = (CheckBox)child.findViewById(R.id.set_chk);
                        if (cb != null)
                        {
                            // toggle checkbox
                            checkboxIsChecked = !cb.isChecked();
                            cb.setChecked(checkboxIsChecked);
                        }
                    }

                    if (checkboxIsChecked)
                    {
                        CatchAndReleaseSettingsActivity.this.settings.setVideoUploadType(true);
                        if (automatic_toast == null)
                        {
                            automatic_toast = Toast.makeText(CatchAndReleaseSettingsActivity.this,
                                                             R.string.settings_no_voice_calls,
                                                             Toast.LENGTH_LONG);
                        }
                        automatic_toast.show();
                    }
                    else
                    {
                        CatchAndReleaseSettingsActivity.this.settings.setVideoUploadType(false);
                        if (automatic_toast != null)
                        {
                            automatic_toast.cancel();
                        }
                    }
                    viewListAdapter.updateAutomaticSection();
                    setVideoUploadTypeInAdapter(CatchAndReleaseSettingsActivity.this.settings.getVideoUploadType());
                break;
                case 3: // Manual upload help
                    showDialog(CatchAndReleaseSettingsActivity.UPLOAD_MANUAL_HELP);
                    break;
                case 5:
                    // Wifi checkbox
                    if (view != null)
                    {
                        CheckBox box = (CheckBox)view.findViewById(R.id.set_chk);
                        if (box != null && box.isEnabled())
                        {
                            boolean bIsChecked = !box.isChecked();
                            box.setChecked(bIsChecked);
                            viewListAdapter.updateIsChecked(onlyOnWifiPosition, bIsChecked);
                            CatchAndReleaseSettingsActivity.this.settings.setOnlyOnWifi(bIsChecked);
                        }
                    }

                    break;
                case 6:
                    // Roaming checkbox
                    if (view != null)
                    {
                        CheckBox box = (CheckBox)view.findViewById(R.id.set_chk);
                        if (box != null && box.isEnabled())
                        {
                            boolean bIsChecked = !box.isChecked();
                            box.setChecked(bIsChecked);
                            viewListAdapter.updateIsChecked(offWhenRoamingPosition, bIsChecked);
                            CatchAndReleaseSettingsActivity.this.settings.setOffWhenRoaming(bIsChecked);
                        }
                    }

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

        if (id == CatchAndReleaseSettingsActivity.UPLOAD_MANUAL_HELP)
        {
            // Warning error telling the user all about it.
            returnValue = new AlertDialog.Builder(this)
                .setMessage(R.string.settings_manual_upload_help)
                .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dialog.dismiss();
                    }
                })
               .create();
        }
        return returnValue;
    }

    private void setVideoUploadTypeInAdapter(boolean uploadType)
    {
        viewListAdapter.updateIsChecked(videoUploadSettingsAutoPosition, uploadType);
    }

    private void setPhotoUploadTypeInAdapter(boolean uploadType) {
        viewListAdapter.updateIsChecked(photoUploadSettingsAutoPosition, uploadType);
    }

    public UploadSettings uploadSettings()
    {
        return this.settings;
    }
}

class UploadSettingsViewListAdapter extends BaseAdapter {
    static final int SETTINGS_LABEL = 0;
    static final int SETTINGS_CHECKBOX = 1;
    static final int SETTINGS_ITEM = 2;

    private final ArrayList<Integer> ids;
    private final Vector<Boolean> is_checked;
    private final Vector<Integer> res_title;
    private final Vector<Integer> res_summary;
    private CatchAndReleaseSettingsActivity associatedActivity;

    private CheckBox roamingCheckBox = null;
    private CheckBox wifiCheckBox = null;
    
    public UploadSettingsViewListAdapter(CatchAndReleaseSettingsActivity activity) {
        ids = new ArrayList<Integer>();
        is_checked = new Vector<Boolean>();
        res_title = new Vector<Integer>();
        res_summary = new Vector<Integer>();
        this.associatedActivity = activity;
    }

    public int addView(int id, int resource_title, int resource_summary, boolean is_checked)
    {
        int position = ids.size();
        ids.add(Integer.valueOf(id));
        res_title.add(Integer.valueOf(resource_title));
        res_summary.add(Integer.valueOf(resource_summary));
        this.is_checked.add(Boolean.valueOf(is_checked));
        return position;
    }

    public void updateResourceSummary(int position, int resource_summary) {
        res_summary.set(position, resource_summary);
    }

    public void updateIsChecked(int position, boolean is_checked) {
        this.is_checked.set(position, is_checked);
    }

    public Object getItem(int position) {
        if (position < is_checked.size()){return is_checked.get(position);}
        return null;
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
        return (ids.get(position) != SETTINGS_LABEL);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        LayoutInflater inflater = (LayoutInflater)this.associatedActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        switch (ids.get(position))
        {
        case SETTINGS_LABEL:
                //view = inflater.inflate(R.layout.settings_list_header, null);
                //((TextView)view).setText(this.associatedActivity.getString(res_title.get(position)));
                view = inflater.inflate(R.layout.settings_subheader_layout, null);
                TextView textView = (TextView)view.findViewById(R.id.activity_title);
                textView.setText(this.associatedActivity.getString(res_title.get(position)));
            break;
        case SETTINGS_CHECKBOX:
                view = inflater.inflate(R.layout.settings_checkbox_2row, null);
                TextView t_check = (TextView)view.findViewById(R.id.check_title);
                t_check.setText(this.associatedActivity.getString(res_title.get(position)));
                TextView t_check_summary = (TextView) view.findViewById(R.id.check_summary);
                t_check_summary.setText(this.associatedActivity.getString(res_summary.get(position)));
                CheckBox t_box = (CheckBox)view.findViewById(R.id.set_chk);

                // Is this the 'OffWhenRoaming' checkbox
                if (position == this.associatedActivity.getOffWhenRoamingPosition())
                {
                    // Have to save this around, so I can access it at will later.
                    this.roamingCheckBox = t_box;
                    this.processRoamingCheckBox(position);
                    updateAutomaticSection();
                }
                else if (position == this.associatedActivity.getOnlyOnWifiPosition())
                {
                    this.wifiCheckBox = t_box;
                    updateAutomaticSection();
                    t_box.setChecked(is_checked.get(position));
                }
                else
                {
                    t_box.setChecked(is_checked.get(position));
                }
            break;
        default:            
            view = inflater.inflate(R.layout.list_item_layout_main_settings, null);
            TextView title = (TextView) view.findViewById(R.id.name);
            title.setText(this.associatedActivity.getString(res_title.get(position)));
            TextView summary = (TextView) view.findViewById(R.id.subtitle);            
            summary.setText(this.associatedActivity.getString(res_summary.get(position)));
            break;
        }
        return view;
    }

    // Check the system 'roaming allowed' variable and enable/disable the 'Off When Roaming' check-box as appropriate.
    public void processRoamingCheckBox(int position)
    {
        if (this.roamingCheckBox != null)
        {
            int roamingAllowed = 0;

            try
            {
                // Check the state of the system 'roaming' setting
                roamingAllowed = Secure.getInt(this.associatedActivity.getContentResolver(), Secure.DATA_ROAMING);
            }
            catch (Exception e)
            {
                LogUtil.exception(this, "processRoamingCheckBox()", e);
            }

            this.roamingCheckBox.setChecked((roamingAllowed == 0) ? true : is_checked.get(position));
            this.roamingCheckBox.setEnabled(roamingAllowed != 0);
        }
    }

    @Override
    public long getItemId(int position) {

        return 0;
    }

    public void updateAutomaticSection()
    {
        if (null != this.wifiCheckBox && null != this.roamingCheckBox)
        {
            boolean enabled = (this.associatedActivity.uploadSettings().getPhotoUploadType() || this.associatedActivity.uploadSettings().getVideoUploadType());
            
            this.wifiCheckBox.setEnabled(enabled);
            if (enabled)
            {
                // Enable the wifi checkbox
                // Use the function to handle the roaming checkbox
                processRoamingCheckBox(this.associatedActivity.getOffWhenRoamingPosition());
            }
            else
            {
                // disable both checkboxes
                this.roamingCheckBox.setEnabled(false);
            }
        }
    }
} // Class UploadSettingsViewListAdapter()

