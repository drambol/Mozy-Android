package com.mozy.mobile.android.activities;

import java.util.Vector;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.content.pm.PackageManager.NameNotFoundException;

import com.mozy.mobile.android.R;


public class AboutActivity extends SecuredActivity
{
    private static final int ITEM_HEADER = 0;
    private static final int ITEM_VERSION = 1;
    private static final int ITEM_PUBLISHER = 2;
    private static final int ITEM_COPYRIGHT = 3;

    /**
     * Adapters
     */
    private AboutViewListAdapter aboutAdapter;

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


    private void buildList() {

        this.aboutAdapter = new AboutViewListAdapter(this);

        // The order that these views are added is the order that they will be displayed on the screen
        this.aboutAdapter.addView(ITEM_HEADER,
                                    R.string.settings_main_item_title_about,
                                    0);
        this.aboutAdapter.addView(ITEM_VERSION,
                                R.string.about_screen_version_label,
                                0);
        this.aboutAdapter.addView(ITEM_PUBLISHER,
                R.string.about_screen_publisher_label,
                  R.string.about_screen_publisher_value);
        this.aboutAdapter.addView(ITEM_COPYRIGHT,
                                R.string.about_screen_copyright_label,
                                R.string.about_screen_copyright_value);

        ListView list = (ListView)findViewById(R.id.list);
        list.setAdapter(this.aboutAdapter);

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

    class AboutViewListAdapter extends BaseAdapter
    {
        private final Vector<Integer> res_title;
        private final Vector<Integer> res_summary;
        private Context context;

        public AboutViewListAdapter(Context context) {
            res_title = new Vector<Integer>();
            res_summary = new Vector<Integer>();
            this.context = context;
        }

        public int addView(int id, int resource_title, int resource_summary)
        {
            int position = res_title.size();
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

        public int getCount() {
            return res_title.size();
        }

        @Override
        public int getViewTypeCount() {
            return res_title.size();
        }

        public boolean areAllItemsSelectable() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            // In the About screen, nothing is actional
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            View view = null;
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (position == ITEM_HEADER)
            {
                // view = inflater.inflate(R.layout.settings_list_header, null);
                view = inflater.inflate(R.layout.settings_subheader_layout, null);
                TextView textView = (TextView)view.findViewById(R.id.activity_title);
                textView.setText(context.getString(res_title.get(position)));
            }
            else
            {
                // All items are instances of SETTINGS_LIST_ITEM
                view = inflater.inflate(R.layout.list_item_layout_main_settings, null);
                TextView title = (TextView) view.findViewById(R.id.name);
                title.setText(context.getString(res_title.get(position)));
                TextView summary = (TextView) view.findViewById(R.id.subtitle);
                if (position != ITEM_VERSION)
                {
                    summary.setText(context.getString(res_summary.get(position)));
                }
                else
                {
                    try {
                        summary.setText(getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                    } catch (NameNotFoundException e) {
                    }
                }
                title.setEnabled(false);
                view.setEnabled(false);
            }
            return view;
        }

        @Override
        public long getItemId(int position) {

            return 0;
        }
    }

}
