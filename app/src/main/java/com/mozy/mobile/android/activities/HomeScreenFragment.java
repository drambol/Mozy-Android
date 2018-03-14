
package com.mozy.mobile.android.activities;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.HomeScreenViewListAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;


public class HomeScreenFragment extends android.support.v4.app.Fragment {
       
    private GridView gridView;
    
    private HomeScreenViewListAdapter listAdaptor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        HomeScreenFragment.this.getActivity().getWindow().setFormat(PixelFormat.RGBA_8888);
        HomeScreenFragment.this.getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) 
    {

        View view =  (View) inflater
                .inflate(R.layout.home_screen_layout, container, false);
        
        
        this.gridView  = (GridView) view.findViewById(R.id.gridViewHome);
       
         
      // listAdaptor = (HomeScreenViewListAdapter) getLastNonConfigurationInstance();
       if (listAdaptor == null)
       {
           listAdaptor = new HomeScreenViewListAdapter(HomeScreenFragment.this.getActivity());
           
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKLIST_DOWNLOADED,
                   R.string.quicklists_label_downloaded,
                   R.drawable.home_downloaded);
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKLIST_RECENTLY_ADDED,
                   R.string.quicklists_label_recently_added,
                   R.drawable.home_recent);
        
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_PHOTOS,
                   R.string.search_photos,
                   R.drawable.home_photos);
           
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_DOCS,
                   R.string.search_documents,
                   R.drawable.home_docs);
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_MUSIC,
                   R.string.search_music,
                   R.drawable.home_music);
           
           listAdaptor.addView(HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_VIDEO,
                   R.string.search_video,
                   R.drawable.home_videos);
                   
       }
       
         
       this.gridView.setOnItemClickListener(new OnItemClickListener()
       {
           public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
           {
               Intent intent;
               String titleBarId;
               
               ((FragmentSecuredActivity) HomeScreenFragment.this.getActivity()).updateAlarm();
               
               switch(listAdaptor.getId(position)){
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKLIST_DOWNLOADED:
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", Integer.toString(R.string.quicklists_label_downloaded));
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY); 
                       break;
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKLIST_RECENTLY_ADDED:
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", Integer.toString(R.string.quicklists_label_recently_added));
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);  
                       break;
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_PHOTOS:
                       titleBarId = Integer.toString(R.string.search_photos);
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", titleBarId);
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY); 
                       break;
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_DOCS:
                       titleBarId = Integer.toString(R.string.search_documents);
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", titleBarId);
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY); 
                       break;
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_MUSIC:
                       titleBarId = Integer.toString(R.string.search_music);
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", titleBarId);
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY); 
                       break;
                   case HomeScreenViewListAdapter.MAINSCREEN_QUICKBROWSE_VIDEO:
                       titleBarId = Integer.toString(R.string.search_video);
                       intent = new Intent(HomeScreenFragment.this.getActivity().getApplicationContext(), QuickAccessScreenActivity.class);
                       intent.putExtra("headerTitleId", titleBarId);
                       startActivityForResult(intent, RequestCodes.REQUEST_START_ACTIVITY);  
                       break;
               }
           }
       });
       
       this.gridView.setAdapter(this.listAdaptor);
       
        return view;
    }
    

    @Override
    public void onResume()
    {
        super.onResume();
    }
 
}
