package com.mozy.mobile.android.providers;

import java.io.File;
import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.mozy.mobile.android.utils.FileUtils;
import com.mozy.mobile.android.utils.LogUtil;

public class FileContentProvider extends ContentProvider{

    
    public static final String AUTHORITY_URI = "com.mozy.mobile.android.filecontentprovider";    
    
    public static final String CONTENT_URI = "content://"+AUTHORITY_URI;

    @Override
    public int delete(Uri uri, String s, String[] as) {
        LogUtil.debug(this, "delete");
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        LogUtil.debug(this, "getType");
        return FileUtils.getMimeTypeFromFileName(uri.getPath());
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentvalues) {
        LogUtil.debug(this, "insert");
        return null;
    }

    @Override
    public boolean onCreate() {
        LogUtil.debug(this, "oncreate: returning true");
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] as, String s, String[] as1, String s1) {
        LogUtil.debug(this, "query");
        return null;
    }

    @Override
    public int update(Uri uri, ContentValues contentvalues, String s,
            String[] as) {
        LogUtil.debug(this, "update");
        return 0;
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException 
    {
         try
         {
             LogUtil.debug(this, "openFile");
             String strPath = uri.getPath();
             LogUtil.debug(this, "Path of the file to be opened:"+strPath);
             File tmpFile = new File(strPath);
             File file = new File(tmpFile.getCanonicalPath());

             ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_WRITE);
             return parcel;
         }
         catch(FileNotFoundException fileNotFoundException)
         {
             LogUtil.exception(this, "Open File:", fileNotFoundException);
             throw fileNotFoundException;
         }
         catch (java.io.IOException e)
         {
             LogUtil.exception(this, "Open File:", e);
             throw new FileNotFoundException();
         }
    }
     
}
