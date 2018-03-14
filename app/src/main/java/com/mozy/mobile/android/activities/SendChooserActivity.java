package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.DialogInterface.OnCancelListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SendChooserActivity extends Activity
{
    private ChooserAdapter adapter = null;
    private static final int CHOOSER_DIALOG = 0;
    private Intent sendIntent = null;

    // The facebook package that claims it does any file on a send intent
    public static final String FACEBOOK_PACKAGE_NAME = "com.facebook.katana";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null)
        {
            sendIntent = (Intent)extras.get("sendIntent");
        }
        this.showDialog(CHOOSER_DIALOG);
    }

    @Override
    public Dialog onCreateDialog(int dialog_id)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.upload_chooser_title);
        ListView sendList = new ListView(this);
        sendList.setBackgroundResource(R.color.white_color);
        sendList.setCacheColorHint(0x00000000);
        sendList.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> list, View view, int position, long id)
            {
                ResolveInfo launchable = adapter.getItem(position);
                ActivityInfo activity = launchable.activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                                                activity.name);
                sendIntent.setComponent(name);
                startActivity(sendIntent);
                SendChooserActivity.this.finish();
            }
        });
        builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
                SendChooserActivity.this.finish();				
			}
        });

        PackageManager pm = getPackageManager();

        List<ResolveInfo> launchables = getIntentApps(); pm.queryIntentActivities(sendIntent, 0);

        Collections.sort(launchables,
                         new ResolveInfo.DisplayNameComparator(pm));

        adapter = new ChooserAdapter(pm, launchables);
        sendList.setAdapter(adapter);
        builder.setView(sendList);

        return builder.create();
    }

    private List<ResolveInfo> getIntentApps()
    {
        int i;
        List<ResolveInfo> apps;
        ArrayList<ResolveInfo> launchables = new ArrayList<ResolveInfo>();
        PackageManager manager = getPackageManager();

        apps = manager.queryIntentActivities(sendIntent, 0);
        i = apps.size();

        if (sendIntent.getAction().equals(Intent.ACTION_SEND))
        {
            for (int x = 0; x < i; x++)
            {
                ResolveInfo app = apps.get(x);
                String packageName = app.activityInfo.packageName;

                try {
                    // Always Filter Mozy here
                    if (!manager.getPackageInfo(getPackageName(), 0).packageName.equals(packageName)
                            && !(((sendIntent.getType() != null) && !(sendIntent.getType().startsWith("image/"))) 
                                    && FACEBOOK_PACKAGE_NAME.equals(packageName)))
                        launchables.add(app);
                }
                catch (NameNotFoundException e) {
                }
            }
        }
        return launchables;
    }

    class ChooserAdapter extends ArrayAdapter<ResolveInfo>
    {
        private PackageManager pm=null;

        ChooserAdapter(PackageManager pm, List<ResolveInfo> apps)
        {
            super(SendChooserActivity.this, R.layout.chooser_row_layout, apps);
            this.pm = pm;
        }

        @Override
        public View getView(int position, View convertView,
                              ViewGroup parent)
        {
            if (convertView == null)
            {
                convertView = newView(parent);
            }

            bindView(position, convertView);

            return(convertView);
        }

        private View newView(ViewGroup parent)
        {
            return(getLayoutInflater().inflate(R.layout.chooser_row_layout, parent, false));
        }

        private void bindView(int position, View row)
        {
            TextView label = (TextView)row.findViewById(R.id.label);

            label.setText(getItem(position).loadLabel(pm));

            ImageView icon = (ImageView)row.findViewById(R.id.icon);

            icon.setImageDrawable(getItem(position).loadIcon(pm));
        }
    }
}
