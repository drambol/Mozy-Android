package com.mozy.mobile.android.files;

import android.app.Activity;
import android.app.AlertDialog;
import com.mozy.mobile.android.R;

public abstract class MozyFile 
{
       public MozyFile()
       {
       }
       
       public abstract String getName();
       public abstract String getPath();
       public abstract  long getUpdated();
       public abstract  long getSize();
       
       public void alertNoSDCard(Activity activity)
       {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setMessage(R.string.alert_sdcard_absent);
            builder.setNegativeButton(R.string.cancel_button_text, null);
            builder.create();
            builder.show();
       }
}
