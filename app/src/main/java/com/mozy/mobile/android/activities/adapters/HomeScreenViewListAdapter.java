package com.mozy.mobile.android.activities.adapters;

import java.util.ArrayList;
import java.util.Vector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mozy.mobile.android.R;

public class HomeScreenViewListAdapter extends BaseAdapter {

    private final ArrayList<Integer> ids;
    private final Vector<Integer> res_title;
    private final Vector<Integer> res_image;

    private Context context;
     
    public static final int MAINSCREEN_QUICKLIST_DOWNLOADED = 0;
    public static final int MAINSCREEN_QUICKLIST_RECENTLY_ADDED = 1;
    public static final int MAINSCREEN_QUICKBROWSE_PHOTOS = 2;
    public static final int MAINSCREEN_QUICKBROWSE_DOCS = 3;
    public static final int MAINSCREEN_QUICKBROWSE_MUSIC = 4;
    public static final int MAINSCREEN_QUICKBROWSE_VIDEO = 5;
    
      

    public HomeScreenViewListAdapter(Context context) {
        ids = new ArrayList<Integer>();
        res_title= new Vector<Integer>();
        res_image = new Vector<Integer>();
        this.context = context;
    }

    public int addView(int id, int resource_title, int resource_image)
    {
        int position = ids.size();
        ids.add(Integer.valueOf(id));
        res_title.add(Integer.valueOf(resource_title));
        res_image.add(Integer.valueOf(resource_image));
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


    public static String padRight(String text, int n) {
        for(int i = 0; i < n; i++)
            text =  text + " ";     
        return text;
   }

   public static String padLeft(String text, int n) {   
       for(int i = 0; i < n; i++)
           text = " " + text; 
       return text;  
   }
   
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        view = inflater.inflate(R.layout.list_item_layout_mainscreen, null);
        TextView TitleTextView = (TextView) view.findViewById(R.id.name);
        
        String text =this.context.getResources().getString(res_title.get(position));
        
        if(text.length() < 9)
        {
            //calculate the padding needed
            int paddingNeeded = 9 - text.length();
            
            text = padLeft(text, (int) paddingNeeded);    
            text = padRight(text, (int) paddingNeeded/2);
            TitleTextView.setText(text);
        }
        else
        {
            TitleTextView.setText(res_title.get(position));
        }
        
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        
        if(imageView != null)
            imageView.setImageResource(res_image.get(position));

        return view;
    } 

    @Override
    public long getItemId(int position) {
        return 0;
    }
}