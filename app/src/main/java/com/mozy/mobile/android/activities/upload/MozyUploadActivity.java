package com.mozy.mobile.android.activities.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.BarActivity;
import com.mozy.mobile.android.activities.ErrorActivity;
import com.mozy.mobile.android.activities.startup.FirstRun;
import com.mozy.mobile.android.activities.startup.ServerAPI;

import com.mozy.mobile.android.catch_release.queue.QueueDatabase;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

import android.app.AlertDialog;


import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


public class MozyUploadActivity extends ErrorActivity {

    private static final String KEY_STREAM = "com.mozy.mobile.android.activities.MozyUploadActivity.KEY_STREAM";

    // Oddly enough MediaStore.Images.Media.DATA contains the path to the file.
    private static final String[] PROJECTION = { MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.MIME_TYPE };

    private ArrayList<Uri> uris;
    private Bitmap bb = null;

    private static final String SCHEME_CONTENT = "content";
    
    public static ArrayList <uploadFile> uploadList = new ArrayList <uploadFile>();
    
    MozyUploadExistsInQueue existsInQueue;
    

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        existsInQueue = new MozyUploadExistsInQueue(this);
        
        uploadList.clear();

        this.setContentView(BarActivity.NO_BAR,R.layout.upload_dialog);
        
        String titleStr = MozyUploadActivity.this.getResources().getString(R.string.upload_dialog_title);
        titleStr = titleStr.replace("$SYNC", MozyUploadActivity.this.getResources().getString(R.string.sync_title));
        
        
        TextView txtView = (TextView)findViewById(R.id.uploadTitleTextView);
        txtView.setText(titleStr);
        
    
        Bundle extras = getIntent().getExtras();
        Uri uri = null;
        if (extras != null && extras.size() > 0) {
            try {
                uri = (Uri)extras.get(Intent.EXTRA_STREAM);
            }
            catch (Exception e)
            {
            }
            if (null != uri)
            {
                uris = new ArrayList<Uri>();
                uris.add(uri);
            }
            else
            {
                try {
                    uris = (ArrayList<Uri>)extras.get(Intent.EXTRA_STREAM);
                }
                catch (Exception e)
                {
                }
            }
        } 
    }
    

    
    @Override
    public void onResume() {
        super.onResume();
        
        final TextView destFolderTextView = (TextView) findViewById(R.id.FolderDestTextView);
        
        String title = null;
        
        
       
        if(UploadManager.uploadManualDestFolder != null) 
        {
            if((UploadManager.uploadManualDestFolder).equalsIgnoreCase(""))
            {
                title = getResources().getString(R.string.sync_title);
            }
            else
            {
                title = UploadManager.uploadManualDestFolder;
                title = FileUtils.getFolderNameFromPath(title);
            }
        }
        else
        {
            title = getResources().getString(R.string.upload_sync_path);
        }
        
        destFolderTextView.setText(title);
        

       if (uris != null && 0 < uris.size()) 
       {
           // Make sure the user is logged into Mozy Mobile
           Provisioning provisioning = Provisioning.getInstance(this);

           String token = provisioning.getMipAccountToken();
           
           String tokenSecret = provisioning.getMipAccountTokenSecret();
       
           if((token != null && token.length() != 0 && tokenSecret != null && tokenSecret.length() != 0))
           {
               LogUtil.debug(this, "Cloud Device Link is " + ServerAPI.getInstance(this).GetCloudDeviceLink());
               if(ServerAPI.getInstance(this).GetCloudDeviceLink() == null || ServerAPI.getInstance(this).GetCloudDeviceLink().equalsIgnoreCase(""))
               {
                   String titleStr = MozyUploadActivity.this.getResources().getString(R.string.upload_sync_not_setup);
                   titleStr = titleStr.replace("$SYNC_UPPERCASE", MozyUploadActivity.this.getResources().getString(R.string.sync_folders_label));
                   AlertDialog noSyncDlg = new AlertDialog.Builder(MozyUploadActivity.this)
                   .setTitle(titleStr)
                   .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int whichButton) {
                           MozyUploadActivity.this.finish();
                           }
                       })
                   .create();
                   noSyncDlg.show();
                   
               }
               else
               {
                   SystemState.setFileSelectedForMozyUpload(false);
                   imageUploadDialog();
               }
           }
           else
           {
               notLoggedInForShareDialog();
           } 
       } 
       else 
       {
           // Toast notify user that no file was found.
           LogUtil.debug(this, "### No Extras!");
           finish();
       }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        
        if (MozyUploadActivity.this.bb != null)
          {
              MozyUploadActivity.this.bb.recycle();
              MozyUploadActivity.this.bb = null;
          }
    }

    /**
     * @return
     */
    public void notLoggedInForShareDialog() {
        AlertDialog notLoggedInDlg = new AlertDialog.Builder(MozyUploadActivity.this)
        .setTitle(R.string.not_logged_in_for_share)
        .setOnKeyListener(new DialogInterface.OnKeyListener() {

            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                    SystemState.setFileSelectedForMozyUpload(false);
                    MozyUploadActivity.this.finish();
                }
                return true;
            }})
        .setPositiveButton(R.string.login, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                
                Intent intent = new Intent(MozyUploadActivity.this.getApplicationContext(), FirstRun.class);
                if(intent != null)
                {
                    SystemState.setFileSelectedForMozyUpload(true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                }
            }
        })
         .setNegativeButton(R.string.cancel_button_text, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    SystemState.setFileSelectedForMozyUpload(false);
                    MozyUploadActivity.this.finish();
                }
         })
        .create();
        notLoggedInDlg.show();
    }

    /**
     * @param context
     * @return
     */
    public void imageUploadDialog() {
         
        if (uris != null && 0 < uris.size()) 
        {
            prepareForManualUploadDlg();
            
            final View v = (View) findViewById(R.id.relativeLayoutFolderButtton);
            v.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(MozyUploadActivity.this, UploadFolderScreenActivity.class);
                    startActivity(intent);
                }
            });

            final Button neg_button = (Button) findViewById(R.id.negative_button);
            neg_button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    MozyUploadActivity.this.finish();
                }
            });

            final Button pos_button = (Button) findViewById(R.id.positive_button);
            pos_button.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    
                    // Need to initialize since the database could have been freed up
                    UploadManager.initialize(MozyUploadActivity.this);
                    
                    prepareManualUploadList();
                    
                    // check any are in the upload queues else check if already in the cloud
                    
                    if(existsInQueue != null && existsInQueue.isExistsInQueue() == false)
                    {
                        MozyUploadActivity.this.uploadAndDismiss();
                    }
                  
                }
            });
        }
        else {
            // TODO create dialog alerting user no image found.
        }
    }

    /**
     * @param file_name
     * @return
     */
    public void prepareManualUploadList() {
        String dataPath = null;
        String mime = null;

        for (int x = 0; x < uris.size(); x++)
        {
            uploadFile uFile = AndroidUriHelper.getPath(MozyUploadActivity.this.getApplicationContext(), uris.get(x));
            uFile.setDestPath(UploadManager.uploadManualDestFolder);
            uploadList.add(uFile);
            existsInQueue.checkExistsInQueues(dataPath, uFile.getMimeType(), UploadManager.uploadManualDestFolder);
        }
        
        return;
    }

    
    public void prepareForManualUploadDlg() {
        double size = 0;
        String file_name = null;
        String mime = null;
        String dataPath = null;
         
        for (int x = 0; x < uris.size(); x++)
        {
            if (SCHEME_CONTENT.equalsIgnoreCase(uris.get(x).getScheme()))
            {
                String authority = uris.get(x) != null ? uris.get(x).getAuthority() : "";
                if (authority.equalsIgnoreCase(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getAuthority()) ||
                        authority.equalsIgnoreCase(MediaStore.Images.Media.INTERNAL_CONTENT_URI.getAuthority())) {
                    Cursor cursor = getContentResolver().query(uris.get(x), PROJECTION, null, null, null);
                    if (cursor != null) {
                        try {
                            if (cursor.moveToFirst()) {
                                
                                int column_name = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
                                file_name = cursor.getString(column_name);
                                LogUtil.debug(this, "### File name: " + file_name);

                                // Total up the size
                                int column_size = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
                                size += cursor.getInt(column_size);
                                
                             // The DATA column contains the path, now that's intuitively obvious.
                                int column_data = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                                dataPath = cursor.getString(column_data);
                                
                             // Grab the mime type
                                int column_mimetype = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);
                                mime = cursor.getString(column_mimetype);
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                }
            }
            else
            {
                file_name = uris.get(x) != null ? uris.get(x).getLastPathSegment() : "";
                
                if(uris.get(x) != null)
                {
                    File localFile = new File( uris.get(x).getPath());
                    dataPath = uris.get(x).getPath();
                    size += localFile.length();  
                    mime = FileUtils.getMimeTypeFromFileName(file_name);
                }
            }
        }
        
        if (1 == uris.size())
        {
            uploadDialogForOneFile(file_name, dataPath, mime);
        }
        else
        {
            uploadDialogMultiFiles(size);
        }
        return;
    }

    /**
     * @param size
     */
    public void uploadDialogMultiFiles(double size) {
        // Insert the file count and size
        String filesize = null;
        if (size > 0)
        {
            final int decr = 1024;
            int step = 0;
            String[] postFix = getResources().getStringArray(R.array.file_sizes_array);
            while((size / decr) > 0.9)
            {
                size = size / decr;
                step++;
            }
            String sizeString = String.format(Locale.getDefault(), "%.1f %s", size, postFix[step]);
            filesize = this.getString(R.string.upload_dialog_size);
            filesize = filesize.replace("$FILESIZE", sizeString);
        }

        String s = this.getString(R.string.upload_dialog_summary_multi);
        s = s.replace("$NUMFILES", Integer.toString(uris.size()));
        s = s.replace("$TOTALSIZE", null != filesize ? filesize + " " : "");
        s = s.replace("$SYNC", this.getString(R.string.sync_title));
        TextView text = (TextView) findViewById(R.id.file_name);
        text.setText(s);
    }


    /**
     * @param file_name
     * @param display
     * @param imageView
     */
    public void uploadDialogForOneFile(String file_name, String fullPath, String mime) {
        
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        ImageView imageView = (ImageView) findViewById(R.id.image);
        if(FileUtils.CATEGORY_PHOTOS == FileUtils.getCategory(mime))
        {
            File file = new File(fullPath);
            InputStream in = null;
            try {
                in = new BufferedInputStream(new FileInputStream(file));
                byte[] data = new byte[(int)file.length()];
                in.read(data);
                try {
                    this.bb = BitmapFactory.decodeByteArray(data, 0, (int)file.length());
                } catch (OutOfMemoryError oom) {
                    
                }
            } catch(Exception ex) {
                
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (Exception ex) 
                    {
                    }
            }
        }
        else  // display the default thumbnail for the mime type
        {
            this.bb = null;
        }
        
        if(null != imageView)
        {
            if (null != this.bb)
            {
                imageView.setImageBitmap(bb);
                imageView.setAdjustViewBounds(true);
                imageView.setMaxHeight(display.getHeight() / 3);
            }
            else   // default thumbnail
            {   
                int fileCategory = 0;
                fileCategory = FileUtils.getCategory(mime);
                
                if(fileCategory != 0)                 
                { 
                    int iconId = FileUtils.getUploadIconIDForNonEncryptedFiles(fileCategory);       
                    Drawable returnValue = this.getApplicationContext().getResources().getDrawable(iconId);
                    imageView.setImageDrawable(returnValue);
                    imageView.setAdjustViewBounds(true);
                    imageView.setMaxHeight(display.getHeight() / 3);
                }
                else
                    imageView.setVisibility(View.GONE);
            }
        }
        TextView fileNameView = (TextView) findViewById(R.id.file_name);
        if(fileNameView != null)
            fileNameView.setText(file_name);

        // Image is not shown in horizontal mode.
        // $TODO: should probably be dependent on height rather than orientation...
        Configuration config = getResources().getConfiguration();
        if (null != this.bb && null != imageView)
        {
            imageView.setVisibility(config.orientation == Configuration.ORIENTATION_LANDSCAPE
                                        ? View.GONE
                                        : View.VISIBLE);
        }
    }



    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    
        if (uris != null && 0 < uris.size()) {
            savedInstanceState.putParcelableArrayList(KEY_STREAM, uris);
        }
    }


    
    public static void queueForUpload(ArrayList<uploadFile> uploadFiles)
    {
        ContentValues[] values = null;
        int size = (uploadFiles != null) ? uploadFiles.size() : 0;
        values = new ContentValues[size];

        for(int i = 0; i < size; i++)
        {
            values[i] = new ContentValues();
            values[i].put(QueueDatabase.KEY_DATA_PATH, uploadFiles.get(i).fullPath);
            values[i].put(QueueDatabase.KEY_DEST_PATH, uploadFiles.get(i).destPath);
            values[i].put(QueueDatabase.KEY_MODIFIED_DATE, uploadFiles.get(i).file.lastModified());
            values[i].put(QueueDatabase.KEY_MIME_TYPE, uploadFiles.get(i).mimeType);
        }
        
        UploadManager.sManualQueue.enqueueFilesInManualQueue(values);
    }
    
    /**
     * @param dialog
     */
    public void uploadAndDismiss() {
        UploadManager.pausedUpload = false;
 
        if(MozyUploadActivity.uploadList != null)
        {
            MozyUploadActivity.queueForUpload(MozyUploadActivity.uploadList);
        }
        
        if(MozyUploadActivity.uploadList != null)
            MozyUploadActivity.uploadList.clear();
        
        finish();
        
    }
   
}
