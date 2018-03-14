package com.mozy.mobile.android.activities.adapters;


import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.utils.FileUtils;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


/*
 * Adapts a list of sync folders to a list view
 */
public class UploadFolderListAdapter extends ArrayAdapter<Object> {
    private Context context;
    private int mResourceId; 

    public UploadFolderListAdapter(Context context) {
        super(context, R.layout.upload_folder_list_item_layout);
        this.mResourceId = R.layout.upload_folder_list_item_layout;
        this.context = context; 
    }
    
    

     public Object getItem(int position) 
     {    
        Object returnValue = null;
       
        returnValue = super.getItem(position);
        
        return returnValue;   
    }

    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        
        if(position == 0)  // Need to it once
        {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.generic_list_layout, null);
            
            TextView textView = (TextView) view.findViewById(R.id.notification);
            textView.setText(R.string.no_items);
            
            view.findViewById(R.id.notification).setVisibility(this.getCount() > 0 ? View.GONE:View.VISIBLE);
            view.findViewById(R.id.footer_divider).setVisibility(this.getCount() > 0 ? View.VISIBLE:View.GONE);
        }
        
        if (position < this.getCount())
        {
            CloudFile listItem = (CloudFile) this.getItem(position);

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(mResourceId, null);
 
            TextView textView = (TextView) view.findViewById(R.id.name);
            
            Drawable img = null;
            
            if(!(listItem instanceof Directory))   
            {
                img = FileUtils.getIconForUploadFile(context, listItem);
                img.setAlpha(150);
                textView.setTextColor(Color.GRAY);
            }
            else
            {
                img = context.getResources().getDrawable(R.drawable.folder);
            }

            textView.setCompoundDrawablesWithIntrinsicBounds( img, null, null, null );

            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

            int displayWidth = display.getWidth();

            view.setLayoutParams(new ListView.LayoutParams(displayWidth, LayoutParams.WRAP_CONTENT));

            if(listItem != null)
            {
                String fileTitle =  ((CloudFile) listItem).getName();
                textView.setText(fileTitle);
            }
        }
        return view;
    }
       
   
    @Override
    public boolean isEnabled(int position) 
    {
        Object item = getItem(position);
        return (item instanceof Directory);
    }
}
