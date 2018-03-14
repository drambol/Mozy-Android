package com.mozy.mobile.android.application;

import java.util.ArrayList;

import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.files.LocalFile;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class MozyFilesDatabase extends SQLiteOpenHelper {
    
    private SQLiteDatabase db;
    private static final String DATABASE_NAME = "EncryptedFilesDatabase.db";
    
    
    public String getDatabaseName() {
        return DATABASE_NAME;
    }


    private static final String FILES_TABLE = "file_table";
    private static final int DATABASE_VERSION = 5;
    
    public static final String KEY_FILE_NAME = "file_name";
    public static final String KEY_CLOUDDATA_PATH = "data_path";
    public static final String KEY_CLOUDFILE_LINK = "file_link";
    public static final String KEY_CLOUDFILE_DATE = "file_date";
    public static final String KEY_DECRYPTED_DATE = "decrypt_date";
    public static final String KEY_LOCAL_FILE_DATE = "local_date";
    public static final String KEY_ENCODING_TYPE = "encoding_type";
    
    private static long NOT_DECRYPTED = -1;
    
    ArrayList <Object> devices = SystemState.getDeviceList();
   
    public static final String[] PROJECTION = {KEY_FILE_NAME, KEY_CLOUDDATA_PATH, KEY_CLOUDFILE_LINK, KEY_CLOUDFILE_DATE, KEY_DECRYPTED_DATE, KEY_LOCAL_FILE_DATE, KEY_ENCODING_TYPE};
    
    private Cursor cursor;
    

    public MozyFilesDatabase(Context context) {
        super(context, DATABASE_NAME , null, DATABASE_VERSION);
        this.cursor = null;
    }

    @Override
    public void onCreate(SQLiteDatabase _db) 
    { 
        
    }
    
    
    @Override
    public void onOpen(SQLiteDatabase _db) 
    {  
        try {
            createTables(_db);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
           
        ArrayList<String> listOfTables = ListOfTables(_db);
       
        if(listOfTables != null)
        {
            // Drop all tables
            for(int i = 0; i < listOfTables.size() ; i++)
            {
                _db.execSQL("DROP TABLE IF EXISTS " + listOfTables.get(i));
            }
        }
        createTables(_db);
    }
    
    public void open() {
        try
        {
            this.db = getWritableDatabase();
        }
        catch (SQLiteException ex)
        {
            LogUtil.debug(this, "Exception: getWritableDatabase: " + ex.getMessage());
            LogUtil.debug(this, "Opening Readable Database instead" );
            this.db = getReadableDatabase();
        }
    }
    
    
    public void close() {
        
       // ArrayList<String> listOfTables = ListOfTables(db);
        
        if(this.cursor != null)
            this.cursor.close();
        
        if(db != null)
        {
            db.close();
            super.close();
        }
      }
    
    private void createTables(SQLiteDatabase _db) {
        
    	ArrayList <Object> devs = SystemState.getDeviceList();
        int numDevices = devs.size();

        for(int i = 0; i < numDevices; i++)
        {
            String containerID = ((Device) devs.get(i)).getId();
               
            _db.execSQL("create table if not exists " +
                    FILES_TABLE + "_" + containerID + " (" +
                    KEY_FILE_NAME + " text not null, " +
                    KEY_CLOUDDATA_PATH + " text not null, " +   
                    KEY_CLOUDFILE_LINK + " text not null, " +   
                    KEY_CLOUDFILE_DATE + " long, " +
                    KEY_DECRYPTED_DATE + " long, " +
                    KEY_LOCAL_FILE_DATE + " long, " +
                    KEY_ENCODING_TYPE + " text, " +
                    "primary key("+ KEY_FILE_NAME + "))");
        }
    }
    
    
    private ArrayList<String> ListOfTables(SQLiteDatabase _db)
    {
        final ArrayList<String> dirArray = new ArrayList<String>();
        
        if(_db == null) return null;

        try
        {
            this.cursor = _db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            
            while(this.cursor.moveToNext())
            {
               String s=cursor.getString(0);
               if(s.equals("android_metadata"))
               {
                 continue;
               }
               else
               {
                  LogUtil.debug(this, "Table Name: " + s);
                  dirArray.add(s);
               }
            }
        }     
           finally
           {
               if(this.cursor != null)
                   this.cursor.close();
           }
        return dirArray;
     }
    
    public long getDecryptDateForFile (String containerID, String fileName) {
        
        long decryptedDate = -1;   
        LogUtil.debug(this, "getDecryptDateForFile query");

        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                             PROJECTION,
                                                             selectBuilder.toString(), 
                                        new String[]{fileName}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                // It could potentially be misleading if more than 1 file with same name, we need to live with it for SDCard file access to work
                decryptedDate = cursor.getLong(cursor.getColumnIndex(KEY_DECRYPTED_DATE));
            }            
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }
        
        return decryptedDate;
    }
    
    public String getEncodingTypeForFile (String containerID, String fileName) {
      
        String encodingType = null;   
        LogUtil.debug(this, "getDecryptDateForFile query");

        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                             PROJECTION,
                                                             selectBuilder.toString(), 
                                        new String[]{fileName}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                // It could potentially be misleading if more than 1 file with same name, we need to live with it for SDCard file access to work
                encodingType = cursor.getString(cursor.getColumnIndex(KEY_ENCODING_TYPE));
            }            
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }
        
        return encodingType;
    }
    
    public long getDecryptDateForFile (String containerID, String fileName,  String fileLink) {
        
        long decryptedDate = -1;   
        LogUtil.debug(this, "getDecryptDateForFile query");

        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?").append(" AND ").append(KEY_CLOUDFILE_LINK).append("=").append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                             PROJECTION,
                                                             selectBuilder.toString(),
                                        new String[]{fileName, fileLink}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                // It could potentially be misleading if more than 1 file with same name, we need to live with it for SDCard file access to work
                decryptedDate = cursor.getLong(cursor.getColumnIndex(KEY_DECRYPTED_DATE));
            }            
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }
        
        return decryptedDate;
    }
    
    public long getDateForCloudFile (String containerID, String fileName, String fileLink) {
        
        long cloudFileDate = -1;
        
        LogUtil.debug(this, "getDateForDecryptedCloudFile query");
        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?").append(" AND ").append(KEY_CLOUDFILE_LINK).append("=").append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                                PROJECTION, 
                                                                selectBuilder.toString(),
                                        new String[]{fileName, fileLink}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                cloudFileDate = this.cursor.getLong(cursor.getColumnIndex(KEY_CLOUDFILE_DATE));
            }
        }
        catch(Exception e)
        {
            LogUtil.debug(this, e.toString());
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }

        
        return cloudFileDate;
    }
    
    
    public long getDateForLocalFile (String containerID, String fileName, String fileLink) {
        
        long localFileDate = -1;
        
        LogUtil.debug(this, "getDateForLocalFile query");
        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?").append(" AND ").append(KEY_CLOUDFILE_LINK).append("=").append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                                PROJECTION, 
                                                                selectBuilder.toString(),
                                        new String[]{fileName, fileLink}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                localFileDate = this.cursor.getLong(cursor.getColumnIndex(KEY_LOCAL_FILE_DATE));
            }
        }
        catch(Exception e)
        {
            LogUtil.debug(this, e.toString());
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }

        
        return localFileDate;
    }
 
    public String getCloudFilePath (String containerID, String fileName) {
        
        String cloudFilePath = null;
        
        LogUtil.debug(this, "getCloudFilePath query");
        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=")
            .append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                                PROJECTION, 
                                                                selectBuilder.toString(),
                                        new String[]{fileName}, null, null, null, null);
           
            
            if(this.cursor.getCount() == 1)
            {
                if(!this.cursor.moveToFirst())
                {
                    throw new SQLException("No entry for file found");
                }
                
                cloudFilePath = this.cursor.getString(cursor.getColumnIndex(KEY_CLOUDDATA_PATH));
            }
        }
        catch(Exception e)
        {
            LogUtil.debug(this, e.toString());
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }

        
        return cloudFilePath;
    }
    
 
    public String getCloudFileLink (String containerID, String fileName) {
     
     String cloudFileLink = null;
     
     LogUtil.debug(this, "getCloudFileLink query");
     try
     {
         
         StringBuilder selectBuilder = new StringBuilder("(")
         .append(KEY_FILE_NAME).append("=")
         .append("?")
         .append(")");
         
         
         this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                                                             PROJECTION, 
                                                             selectBuilder.toString(),
                                     new String[]{fileName}, null, null, null, null);
        
         
         if(this.cursor.getCount() == 1)
         {
             if(!this.cursor.moveToFirst())
             {
                 throw new SQLException("No entry for file found");
             }
             
             cloudFileLink = this.cursor.getString(cursor.getColumnIndex(KEY_CLOUDFILE_LINK));
         }
     }
     catch(Exception e)
     {
         LogUtil.debug(this, e.toString());
     }
     finally
     {
         if(this.cursor != null)
             this.cursor.close();
     }

     
     return cloudFileLink;
 }
 
    public boolean existsFileInDB (String containerID, String fileName) {
        
        boolean bResult = false;
        
        LogUtil.debug(this, "existsFileInDB query for fileName");
        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=").append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                    PROJECTION,
                    selectBuilder.toString(),
                    new String[]{fileName}, null, null, null, null);
            
            int count = this.cursor.getCount();
            
            if(count == 1)
            {
                bResult = true;
            } 
 
            if(count == 0)
            {
                LogUtil.debug(this, "No enteries found");
            }
            else if ( count > 1)
            {
                LogUtil.debug(this, "Multiple enteries found");
            }
        }
        catch(Exception e)
        {
            LogUtil.debug(this, e.toString());
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }

        return bResult;
    }
    
    
    public boolean existsFileInDB (String containerID, String fileName, String fileLink) {
        
        boolean bResult = false;
        
        LogUtil.debug(this, "existsFileInDB for fileName and fileLink query");
        try
        {
            
            StringBuilder selectBuilder = new StringBuilder("(")
            .append(KEY_FILE_NAME).append("=").append("?").append(" AND ").append(KEY_CLOUDFILE_LINK).append("=").append("?")
            .append(")");
            
            
            this.cursor = this.db.query(FILES_TABLE + "_" + containerID, 
                    PROJECTION,
                    selectBuilder.toString(),
                    new String[]{fileName, fileLink}, null, null, null, null);
            
            int count = this.cursor.getCount();
            
            if(count == 1)
            {
                bResult = true;
            } 
 
            if(count == 0)
            {
                LogUtil.debug(this, "No enteries found");
            }
            else if ( count > 1)
            {
                LogUtil.debug(this, "Multiple enteries found");
            }
        }
        catch(Exception e)
        {
            LogUtil.debug(this, e.toString());
        }
        finally
        {
            if(this.cursor != null)
                this.cursor.close();
        }

        return bResult;
    }
    
    public long insertFileInDB(String containerID, String fileName, 
    		String filePath, String fileLink, 
    		long cloudfileDate,  long decryptedDate, 
    		long localFileDate, String encodingType)
    {
        LogUtil.debug(this, "insertFileInDB");
        
        ContentValues newValue = new ContentValues();
        
        newValue.put(KEY_FILE_NAME, fileName);
        newValue.put(KEY_CLOUDDATA_PATH, filePath);
        newValue.put(KEY_CLOUDFILE_LINK, fileLink);
        newValue.put(KEY_CLOUDFILE_DATE, cloudfileDate);
        newValue.put(KEY_DECRYPTED_DATE, decryptedDate);
        newValue.put(KEY_LOCAL_FILE_DATE, localFileDate);
        newValue.put(KEY_ENCODING_TYPE, encodingType);
        
        return this.db.insert(FILES_TABLE + "_" + containerID, null, newValue);
        
    }


    public int updateFileInDB(String containerID, 
    		String fileName, String filePath, 
    		String fileLink, long cloudfileDate,   
    		long decryptedDate, long localFileDate,
    		String encodingType) 
    {
        LogUtil.debug(this, "updateFileInDB");
        ContentValues newValue = new ContentValues();
        
        newValue.put(KEY_CLOUDDATA_PATH, filePath);
        newValue.put(KEY_CLOUDFILE_LINK, fileLink);
        newValue.put(KEY_CLOUDFILE_DATE, cloudfileDate);
        newValue.put(KEY_DECRYPTED_DATE, decryptedDate);
        newValue.put(KEY_LOCAL_FILE_DATE, localFileDate);
        if (encodingType != null)
            newValue.put(KEY_ENCODING_TYPE, encodingType);
        
        StringBuilder selectBuilder = new StringBuilder("(")
        .append(KEY_FILE_NAME).append("=").append("?")
        .append(")");
        
        return this.db.update(FILES_TABLE + "_" + containerID, 
                                newValue,
                                selectBuilder.toString(),
                                new String[]{fileName});
    }
    
    
    public int updateFileWithEncryptionDateInDB(String containerID, String fileName, long decryptedDate)
    {
        LogUtil.debug(this, "updateFileInDB");
        ContentValues newValue = new ContentValues();

        newValue.put(KEY_DECRYPTED_DATE, decryptedDate);
        
        StringBuilder selectBuilder = new StringBuilder("(")
        .append(KEY_FILE_NAME).append("=").append("?")
        .append(")");
        
        return this.db.update(FILES_TABLE + "_" + containerID, 
                                newValue,
                                selectBuilder.toString(),
                                new String[]{fileName});
    }
    
    public boolean removeFileInDB(String containerID, String fileName)
    {
        LogUtil.debug(this, "removeDecryptedFileEntry");
        
        StringBuilder selectBuilder = new StringBuilder("(")
        .append(KEY_FILE_NAME).append("=").append("?")
        .append(")");

        return (this.db.delete(FILES_TABLE + "_" + containerID, 
                selectBuilder.toString(),
                new String[]{fileName}) > 0) ;
    }
    

    /**
     * 
     */
    public void insertOrUpdateDownloadedFileinDB(String containerID, CloudFile cloudFile, LocalFile localFile, String encodingType) {
        String cloudFileLink = null;
                
        //successfully downloaded insert or update database with this file
        if(cloudFile != null)
        {
            cloudFileLink = cloudFile.getLink(); 
        
            if(cloudFileLink != null && containerID != null && localFile != null)
            {
                if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB(containerID ,localFile.getName(), cloudFileLink) == true)
                {
                    SystemState.mozyFileDB.updateFileInDB(containerID, 
                            localFile.getName(),
                            cloudFile.getPath(), cloudFile.getLink(), cloudFile.getUpdated(),
                            NOT_DECRYPTED, localFile.file.lastModified(), encodingType);
                }
                else
                {
                    if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.insertFileInDB(containerID, 
                            localFile.getName(), cloudFile.getPath(), cloudFile.getLink(),cloudFile.getUpdated(),
                            NOT_DECRYPTED, localFile.file.lastModified(), encodingType) == -1)
                        {
                            LogUtil.debug(this, "Insert entery failed");;
                        }
                }
            }
        }
    }

    
    /**
     * 
     */
    public void insertOrUpdateUploadedFileinDB(String containerID, CloudFile cloudFile, LocalFile localFile, LocalFile decryptedFile) {
        String cloudFileLink = null;
        long decryptedDate;
        
        if (decryptedFile == null) {
        	decryptedDate = NOT_DECRYPTED;
        } else {
        	decryptedDate = decryptedFile.file.lastModified();
        }
        	
        //successfully downloaded insert or update database with this file
        if(cloudFile != null)
        {
            cloudFileLink = cloudFile.getLink(); 
        
            if(cloudFileLink != null && containerID != null && localFile != null)
            {
                // update the db with cloud file stamp to -1 as we are uploading this file and do not know what the timestamp would be after upload
                
                if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.existsFileInDB(containerID ,localFile.getName(), cloudFileLink) == true)
                {
                    SystemState.mozyFileDB.updateFileInDB(containerID, 
                            localFile.getName(),
                            cloudFile.getPath(), cloudFile.getLink(), -1,
                            decryptedDate, localFile.file.lastModified(), null);
                }
                else
                {
                    if(SystemState.mozyFileDB != null && SystemState.mozyFileDB.insertFileInDB(containerID, 
                            localFile.getName(), cloudFile.getPath(), cloudFile.getLink(),-1,
                            decryptedDate, localFile.file.lastModified(), null) == -1)
                        {
                            LogUtil.debug(this, "Insert entery failed");;
                        }
                }
            }
        }
    }

    public void clearDB() {
        
        if(db != null && db.isOpen())
        {
            ArrayList<String> listOfTables = ListOfTables(db);
            
            // Drop all tables
            if(listOfTables != null && listOfTables.size() > 0)
            {
                for(int i = 0; i < listOfTables.size() ; i++)
                {
                    db.execSQL("DROP TABLE IF EXISTS " + listOfTables.get(i));
                }
            }
        }
        return;
    }
}
    
    
