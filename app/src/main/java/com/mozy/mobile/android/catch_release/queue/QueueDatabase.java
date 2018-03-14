package com.mozy.mobile.android.catch_release.queue;

import com.mozy.mobile.android.utils.LogUtil;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class QueueDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "QueueDatabase";
    private static final int DATABASE_VERSION = 2;
    
    private static final String QUEUE_TABLE = "queue_table";
    
    public static final String KEY_DATA_PATH = "data_path";
    public static final String KEY_MODIFIED_DATE = "modified_date";
    public static final String KEY_MIME_TYPE = "mime_type";
    public static final String KEY_DEST_PATH = "dest_path";
    
    public static final String[] PROJECTION = { KEY_DATA_PATH, KEY_DEST_PATH, KEY_MODIFIED_DATE, KEY_MIME_TYPE};
    
    private static final String CREATE_QUEUE_TABLE = "create table " +
        QUEUE_TABLE + " (" +
        KEY_DATA_PATH + " text not null, " +
        KEY_DEST_PATH + " text not null, " +
        KEY_MODIFIED_DATE + " integer not null, " +
        KEY_MIME_TYPE + " text , PRIMARY KEY(" + 
        KEY_DATA_PATH + ", " + KEY_DEST_PATH + "))";
    
    private SQLiteDatabase db;

    QueueDatabase(Context context, int uploadType) {
        super(context, DATABASE_NAME + uploadType + ".db", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase _db) {
        createTables(_db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
        _db.execSQL("DROP TABLE IF EXISTS " + QUEUE_TABLE);
        createTables(_db);
    }
    
    public Cursor getQueue() {

        this.db = getReadableDatabase();
        if (this.db != null) {
            return this.db.query(QUEUE_TABLE, PROJECTION, null, null, null, null, null);
        }
        return null;
    }
    
    private void createTables(SQLiteDatabase _db) {
        _db.execSQL(CREATE_QUEUE_TABLE);
    }
    
    boolean deleteEntry(String data_path, String dest_path) {
        
        if(this.db != null)
            this.db.close();
        
        this.db = getWritableDatabase();
        if (this.db != null) {
            return this.db.delete(QUEUE_TABLE, KEY_DATA_PATH + "='" + data_path.replaceAll("'", "''") + "'" + " AND " +
                    KEY_DEST_PATH +  "='" + dest_path.replaceAll("'", "''") + "'" , null) > 0;
        }
        return false;
    }
    
    boolean insertValue(ContentValues value) {

        boolean bResult = false;

        this.db = getWritableDatabase();
        if (this.db != null && value != null ) 
        {
                DatabaseUtils.InsertHelper insert_helper = new DatabaseUtils.InsertHelper(this.db, QUEUE_TABLE);

                try 
                {
                    LogUtil.debug(this, "### INSERTING");
                    if(insert_helper.insert(value) != -1)
                        bResult = true;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        
        return bResult;
        }
        
        
    void replaceValue(ContentValues value) {
        LogUtil.debug(this, "### REPLACE VALUES: " + (value != null ? 1 : 0));
        
        this.db = getWritableDatabase();
        if (this.db != null && value != null) {
        DatabaseUtils.InsertHelper insert_helper = new DatabaseUtils.InsertHelper(this.db, QUEUE_TABLE);
            try {
                LogUtil.debug(this, "### INSERTING");
                insert_helper.replace(value);
            } 
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    boolean isExists(String dataPath, String destPath)
    {
        boolean bResult = false;
        Cursor cursor = null;

        this.db = getWritableDatabase();

        if (this.db != null && dataPath != null) 
        {
            try
            {
                StringBuilder selectBuilder = new StringBuilder("(")
                .append(KEY_DATA_PATH).append("=")
                .append("?").append(" AND ").append(KEY_DEST_PATH).append("=").append("?")
                .append(")");
                
                
                cursor = this.db.query(QUEUE_TABLE, PROJECTION, selectBuilder.toString(), 
                                            new String[]{dataPath, destPath}, null, null, null, null);
               
                
                if(cursor.getCount() >= 1)
                {
                    if(!cursor.moveToFirst())
                    {
                        throw new SQLException("No entry for file found");
                    }
                    bResult = true;
                }            
            }
            finally
            {
                if(cursor != null)
                    cursor.close();
            }
        }
        return bResult;
    }

    
    void dequeueAll() {
        
        this.db = getWritableDatabase();
        if (this.db != null) {
            this.db.delete(QUEUE_TABLE, null, null);
        }
    }
    
    public void close() {

         if(this.db != null)
             this.db.close();
       }  
}
