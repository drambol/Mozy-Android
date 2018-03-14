package com.mozy.mobile.android.catch_release;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.helper.UploadSettings;


// This screen will be shown if the user has not set any of the upload settings and the C & R code has seen
// new files on the system.
public class CatchAndReleaseInitialSetupActivity extends Activity
{

    private static final int UPLOAD_SETUP_DIALOG = 0;
    private static final int UPLOAD_MANUAL_HELP = 1;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.mozy_upload);

        this.showDialog(UPLOAD_SETUP_DIALOG);
    }

    @Override
    public Dialog onCreateDialog(int dialog_id) {

        AlertDialog dialog = null;

        if (UPLOAD_SETUP_DIALOG == dialog_id)
        {
            dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.c_and_r_initial_prompt)
                .setNegativeButton(this.getString(R.string.no_button_text), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        UploadSettings settings = new UploadSettings(getApplicationContext());

                        settings.setVideoUploadType(false);
                        settings.setPhotoUploadType(false);
                        settings.setOffWhenRoaming(true);
                        settings.setOnlyOnWifi(true);
                        settings.setUploadInitialized();

                        settings.setPreferences(getApplicationContext());

                        dialog.dismiss();
                        CatchAndReleaseInitialSetupActivity.this.showDialog(UPLOAD_MANUAL_HELP);
                    }
                })
                .setPositiveButton(R.string.yes_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        UploadSettings settings = new UploadSettings(getApplicationContext());

                        settings.setVideoUploadType(false);
                        settings.setPhotoUploadType(true);
                        settings.setOffWhenRoaming(true);
                        settings.setOnlyOnWifi(false);
                        settings.setUploadInitialized();
                        
                        settings.setPreferences(getApplicationContext());

                        // Open catch and release settings screen
                        Intent intent = new Intent(CatchAndReleaseInitialSetupActivity.this, CatchAndReleaseSettingsActivity.class);
                        startActivity(intent);

                        dialog.dismiss();
                        CatchAndReleaseInitialSetupActivity.this.finish();
                    }
                })
               .create();
        }
        else if (UPLOAD_MANUAL_HELP == dialog_id)
        {
            // Warning error telling the user all about it.
            dialog = new AlertDialog.Builder(this)
                .setMessage(R.string.settings_manual_upload_help)
                .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        dialog.dismiss();
                        CatchAndReleaseInitialSetupActivity.this.finish();
                    }
                })
               .create();
        }

           return dialog;
    }
}
