package com.mozy.mobile.android.activities.adapters;

import java.util.ArrayList;
import java.util.Iterator;

import com.mozy.mobile.android.files.CloudFile;
import com.mozy.mobile.android.files.Directory;
import com.mozy.mobile.android.files.Document;
import com.mozy.mobile.android.files.Music;
import com.mozy.mobile.android.files.Photo;
import com.mozy.mobile.android.files.Video;
import com.mozy.mobile.android.utils.FileUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ListDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ListCacheDatabase.db";
    private static final int DATABASE_VERSION = 11;
    
    private static final String TABLE_LIST_CACHE = "list_cache";
    
    private static final int SPECIAL_CATEGORY_DIRECTORY = 0x0800;
    private static final int SPECIAL_CATEGORY_TITLE = 0x0801;
    
    private static final String KEY_INPUT_LINK = "input_link";
    private static final String KEY_INPUT_PATH = "input_path";
    private static final String KEY_INPUT_TITLE = "input_title";
    private static final String KEY_INPUT_MIMETYPE = "input_mimetype";
    private static final String KEY_INPUT_SIZE = "input_size";
    private static final String KEY_INPUT_UPDATED = "input_updated";
    private static final String KEY_INPUT_CATEGORY = "input_category";
    private static final String KEY_INPUT_DELETED = "input_deleted";
    private static final String KEY_INPUT_SORT = "input_sort"; 
    private static final String KEY_INPUT_VERSION = "input_version";

    /* PUT THE BAD CODE BACK
    private static final String CREATE_LIST_CACHE_DATABASE = "create table " +
        TABLE_LIST_CACHE + " (" + 
        KEY_INPUT_LINK + " text not null, " +
        KEY_INPUT_TITLE + " text not null, " +
        KEY_INPUT_PATH + " text, " +
        KEY_INPUT_MIMETYPE + " text, " +
        KEY_INPUT_SIZE + " integer not null, " +
        KEY_INPUT_UPDATED + " integer not null, " +
        KEY_INPUT_CATEGORY + " integer not null, " + 
        KEY_INPUT_DELETED + " integer not null, PRIMARY KEY(" +
        KEY_INPUT_LINK + "))";
    */
    
    /* 
    private static final String CREATE_LIST_CACHE_DATABASE = "create table " +
    TABLE_LIST_CACHE + " (" +
    "id integer PRIMARY KEY, " +                // This field will be an auto-increment field 
    KEY_INPUT_LINK + " text not null, " +
    KEY_INPUT_TITLE + " text not null, " +
    KEY_INPUT_PATH + " text, " +
    KEY_INPUT_MIMETYPE + " text, " +
    KEY_INPUT_SIZE + " integer not null, " +
    KEY_INPUT_UPDATED + " integer not null, " +
    KEY_INPUT_CATEGORY + " integer not null, " + 
    KEY_INPUT_DELETED + " integer not null)";
    */
    
    private static final String CREATE_LIST_CACHE_DATABASE = "create table " +
    TABLE_LIST_CACHE + " (" +
    "id integer PRIMARY KEY, " +                // This field will be an auto-increment field 
    KEY_INPUT_LINK + " text not null, " +
    KEY_INPUT_TITLE + " text not null, " +
    KEY_INPUT_PATH + " text, " +
    KEY_INPUT_MIMETYPE + " text, " +
    KEY_INPUT_SIZE + " integer not null, " +
    KEY_INPUT_UPDATED + " integer not null, " +
    KEY_INPUT_VERSION + " integer not null, " +
    KEY_INPUT_CATEGORY + " integer not null, " + 
    KEY_INPUT_DELETED + " integer not null," +
    KEY_INPUT_SORT + " integer not null)";        // Column used to sort directories first, will be zero for directories,
                                                // 1 for any others
    
    private static final String CREATE_INDEX = "CREATE INDEX " + TABLE_LIST_CACHE + " ON " + TABLE_LIST_CACHE + "(" + KEY_INPUT_SORT + ")";
    private static final String SORT_TEXT = KEY_INPUT_SORT + " ASC";
    
    
    /*
    private static final String[] PROJECTION_ALL = { KEY_INPUT_LINK, KEY_INPUT_PATH, KEY_INPUT_TITLE, KEY_INPUT_MIMETYPE, KEY_INPUT_SIZE, KEY_INPUT_UPDATED, KEY_INPUT_CATEGORY, KEY_INPUT_DELETED };
    */
    
    private static final String[] PROJECTION_ALL = { "id", KEY_INPUT_LINK, KEY_INPUT_PATH, KEY_INPUT_TITLE, KEY_INPUT_MIMETYPE, KEY_INPUT_SIZE, KEY_INPUT_UPDATED, KEY_INPUT_VERSION, KEY_INPUT_CATEGORY, KEY_INPUT_DELETED };
    
        
    private String nextIndex;
    private String containerLink;
    private String searchText;
    private boolean recurse;
    private Cursor cursor;
    private SQLiteDatabase database=null;
    private int cursorPosition;
    
    // If this.dirsOnly and this.photosOnly are both true, then both types, and only both types, 
    // are included in the selection set.     
    private boolean dirsOnly;
    private boolean photosOnly;
    
    public ListDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.cursor = null;
        this.nextIndex = null;
        this.containerLink = null;
        this.searchText = null;
        this.recurse = false;
        this.dirsOnly = false;
        this.photosOnly = false;
        this.cursorPosition = -1;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            createTables(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LIST_CACHE);
        try {
            createTables(db);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createTables(SQLiteDatabase db) {
        db.execSQL(CREATE_LIST_CACHE_DATABASE);
        db.execSQL(CREATE_INDEX);
    }
    
    public synchronized String getNextIndex(final String containerLink, final String searchQuery, final boolean recurse) {
        if (this.containerLink != null && this.searchText != null && this.recurse == recurse && this.containerLink.equalsIgnoreCase(containerLink) && this.searchText.equalsIgnoreCase(searchQuery)) {
            return nextIndex;
        }
        return null;
    }
    
    // If the directoriesOnly and photosOnly parameters are both true, then both types, and only both types, 
    // are included in the selection set.    
    public synchronized Object getListItem(final String containerLink, final String searchQuery, final boolean directoriesOnly, final boolean photosOnly, boolean recurse, final int position) {
        Object returnValue = null;
        // CloudFile file = null;
        if (position >= 0 && cursorReady(containerLink, searchQuery, recurse, directoriesOnly, photosOnly) && cursor != null) {
            if (position >= this.cursor.getCount()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            cursorPosition = this.cursor.moveToFirst() ? 0 : -1;
            if (cursorPosition >= 0 && (position - cursorPosition == 0 || this.cursor.move(position - cursorPosition))) {
                cursorPosition = position;
                int columnLink = this.cursor.getColumnIndex(KEY_INPUT_LINK);
                int columnPath = this.cursor.getColumnIndex(KEY_INPUT_PATH);
                int columnTitle = this.cursor.getColumnIndex(KEY_INPUT_TITLE);
                int columnMimeType = this.cursor.getColumnIndex(KEY_INPUT_MIMETYPE);
                int columnSize = this.cursor.getColumnIndex(KEY_INPUT_SIZE);
                int columnUpdated = this.cursor.getColumnIndex(KEY_INPUT_UPDATED);
                int columnVersion = this.cursor.getColumnIndex(KEY_INPUT_VERSION);
                int columnCategory = this.cursor.getColumnIndex(KEY_INPUT_CATEGORY);
                int columnDeleted = this.cursor.getColumnIndex(KEY_INPUT_DELETED);
                String link = this.cursor.getString(columnLink);
                String path = this.cursor.getString(columnPath);
                String title = this.cursor.getString(columnTitle);
                String mimeType = this.cursor.getString(columnMimeType);
                long size = this.cursor.getLong(columnSize);
                long updated = this.cursor.getLong(columnUpdated);
                long version = this.cursor.getLong(columnVersion);
                int category = this.cursor.getInt(columnCategory);
                boolean deleted = this.cursor.getInt(columnDeleted) > 0 ? true : false;
                
                switch (category) {
                case SPECIAL_CATEGORY_TITLE:
                    returnValue = new String(link);
                    break;                
                case SPECIAL_CATEGORY_DIRECTORY:
                    returnValue = new Directory(link, title, size, deleted, updated, version, path);
                    break;
                case FileUtils.CATEGORY_MSEXCEL:
                case FileUtils.CATEGORY_MSPOWERPOINT:
                case FileUtils.CATEGORY_MSWORD:
                case FileUtils.CATEGORY_PDF:
                    returnValue = new Document(link, title, size, deleted, updated, version, path, mimeType, category);
                    break;
                case FileUtils.CATEGORY_MUSIC:
                    returnValue = new Music(link, title, size, deleted, updated, version, path, mimeType);
                    break;
                case FileUtils.CATEGORY_PHOTOS:
                    returnValue = new Photo(link, title, size, deleted, updated, version, path, mimeType);
                    break;
                case FileUtils.CATEGORY_VIDEOS:
                    returnValue = new Video(link, title, size, deleted, updated, version, path, mimeType);
                    break;
                default:
                    returnValue = new CloudFile(link, title, size, deleted, updated, version, path, mimeType);
                }
            }
        }
        return returnValue;
    }
    
    // If the directoriesOnly and photosOnly parameters are both true, then both types, and only both types, 
    // are included in the selection set.    
    public synchronized int getCount(final String containerLink, final String searchQuery, final boolean directoriesOnly, final boolean photosOnly, final boolean recurse) {
        if (cursorReady(containerLink, searchQuery, recurse, directoriesOnly, photosOnly) && this.cursor != null) {
            return this.cursor.getCount();
        }
        return -1;
    }
    
    public synchronized int getPhotoCount(int position)
    {
        int returnValue = -1;
        
        if (this.cursor != null)
        {
            // Select the 'id' column, then select the count of all the photos with a lower id.
            if (position >= this.cursor.getCount()) {
                throw new ArrayIndexOutOfBoundsException();
            }
            cursorPosition = this.cursor.moveToFirst() ? 0 : -1;
            if (cursorPosition >= 0 && (position - cursorPosition == 0 || this.cursor.move(position - cursorPosition))) 
            {
                cursorPosition = position;

                int columnId = this.cursor.getColumnIndex("id");
                int id = this.cursor.getInt(columnId);
                
                // Now get the count of all photos whose 'id' value is less then or equal to 'id';
                // SQLiteDatabase db = this.getReadableDatabase();
                
                String selection = "id<=" + Integer.toString(id);
                // photos only
                selection += " AND " + KEY_INPUT_CATEGORY + "=" + Integer.toString(FileUtils.CATEGORY_PHOTOS);    
                
                String[] smallList = { "id" };                
                
                //Cursor tempCursor = db.query(TABLE_LIST_CACHE, smallList, selection, null, null, null, null);
                Cursor tempCursor = this.database.query(TABLE_LIST_CACHE, smallList, selection, null, null, null, null);                
                returnValue = tempCursor.getCount();
                tempCursor.close();
            }
        }
        
        return returnValue;
    }
    
    public synchronized void removeItem(int position, boolean directoriesOnly, boolean photosOnly)
    {
        // I will have to open a write-able database and use it to
        // do a delete. I will also have to close and recreate the cursor
        // So I will have to pass a bunch more parameters here, so I can create
        // the proper cursor, sorting and filtering etc.
        if (position >= this.cursor.getCount()) 
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        this.cursorPosition = this.cursor.moveToFirst() ? 0 : -1;
        if ((this.cursorPosition >= 0) && 
            (position - this.cursorPosition == 0 || this.cursor.move(position - this.cursorPosition))) 
        {
            this.cursorPosition = position;
            
            int columnId = this.cursor.getColumnIndex("id");
            int id = this.cursor.getInt(columnId);            
            
            // Remove the old cursor
            this.clean(false);
    
            // Remove the row from the database.
            String whereClause = "id==" + Integer.toString(id);            
            // SQLiteDatabase db = this.getWritableDatabase();
            // db.delete(TABLE_LIST_CACHE, whereClause, null);
            this.database.delete(TABLE_LIST_CACHE, whereClause, null);            
            
            // Create a new cursor. This will set the member variable referring to the new Cursor
            this.obtainCursor(this.containerLink, this.searchText, this.recurse, directoriesOnly, photosOnly);        
        }
    }

    public synchronized boolean updateList(final ArrayList<Object> list, final String nextIndex,
            final String dirLink, final String searchText, final boolean recurse) {
        if (this.containerLink == null || this.searchText == null || !this.containerLink.equalsIgnoreCase(dirLink) || !this.searchText.equalsIgnoreCase(searchText) || this.recurse != recurse) {
            clean(true);
        }
        
        // DatabaseUtils.InsertHelper insertHelper = new DatabaseUtils.InsertHelper(getWritableDatabase(), TABLE_LIST_CACHE);
        DatabaseUtils.InsertHelper insertHelper = new DatabaseUtils.InsertHelper(this.database, TABLE_LIST_CACHE);
        ContentValues values;
        Iterator<Object> itr = list.iterator();
        long result = 0;
        boolean itemsAdded = false;
        while (itr.hasNext()) {
            values = new ContentValues();
            Object nextItem = itr.next();
            
            // Handle the case where the list contains a String, not a CloudFile
            if (nextItem instanceof String)
            {
                values.put(KEY_INPUT_LINK, (String)nextItem);
                values.put(KEY_INPUT_PATH, "");
                values.put(KEY_INPUT_MIMETYPE, "");
                values.put(KEY_INPUT_TITLE, "");
                values.put(KEY_INPUT_SIZE, 0);
                values.put(KEY_INPUT_UPDATED, 0);
                values.put(KEY_INPUT_VERSION, 0);
                values.put(KEY_INPUT_CATEGORY, SPECIAL_CATEGORY_TITLE);
                values.put(KEY_INPUT_DELETED, 0);
                values.put(KEY_INPUT_SORT, 1);                
                try {
                    result = insertHelper.insert(values);
                } catch (SQLiteConstraintException e) {
                    e.printStackTrace();
                }
                if (result >= 0) {
                    itemsAdded = true;
                }                
            }
            else
            {
                // CloudFile file = (CloudFile)itr.next();
                CloudFile file;                
                try
                {
                    file = (CloudFile)nextItem;
                }
                catch (Throwable t)
                {
                    file = null;
                }
                if (file != null) {
                    boolean isDirectory = false;
                    if (file instanceof Directory) {
                        isDirectory = true;
                    }
                    values.put(KEY_INPUT_LINK, file.getLink());
                    values.put(KEY_INPUT_PATH, file.getPath());
                    values.put(KEY_INPUT_MIMETYPE, file.getMimeType());
                    values.put(KEY_INPUT_TITLE, file.getTitle());
                    values.put(KEY_INPUT_SIZE, file.getSize());
                    values.put(KEY_INPUT_UPDATED, file.getUpdated());
                    values.put(KEY_INPUT_VERSION, file.getVersionId());
                    values.put(KEY_INPUT_CATEGORY, isDirectory ? SPECIAL_CATEGORY_DIRECTORY : file.getCategory());
                    values.put(KEY_INPUT_DELETED, file.isMarkedForDelete() ? 1 : 0);
                    values.put(KEY_INPUT_SORT, isDirectory ? 0 : 1);
                    try {
                        result = insertHelper.insert(values);
                    } catch (SQLiteConstraintException e) {
                        e.printStackTrace();
                    }
                    if (result >= 0) {
                        itemsAdded = true;
                    }
                }
            }
        }
        
        insertHelper.close();
        
        this.containerLink = dirLink;
        this.searchText = searchText;
        this.recurse = recurse;
        this.nextIndex = nextIndex;
        if (itemsAdded) {
            clean(false);
        }
        return itemsAdded;
    }
    
    private synchronized boolean cursorReady(String containerLink, String searchText, boolean recurse, boolean directoriesOnly, boolean photosOnly) {
        boolean result;
        if (
              (this.cursor != null) && 
              (
                 (
                    (this.containerLink != null) && 
                    this.containerLink.equalsIgnoreCase(containerLink)
                 ) || 
                 (
                    (this.containerLink == null) && 
                    (containerLink == null)
                 )
              ) && 
              (
                 (
                    (this.searchText != null) && 
                    this.searchText.equalsIgnoreCase(searchText)
                 ) || 
                 (
                    (this.searchText == null) && 
                    (searchText == null)
                  )
               ) && 
               (this.recurse == recurse) && 
               (this.dirsOnly == directoriesOnly) && 
               (this.photosOnly == photosOnly)
           ) 
        {
            result = true;
        } 
        else 
        {
            // If this.dirsOnly and this.photosOnly are both true, then both types, and only both types, are included in the selection set. 
            this.dirsOnly = directoriesOnly;
            this.photosOnly = photosOnly;
            result = obtainCursor(containerLink, searchText, recurse, directoriesOnly, photosOnly);
        }
        return result;
    }
    
    // If the directoriesOnly and photosOnly parameters are both true, then both types, and only both types, 
    // are included in the selection set.    
    private synchronized boolean obtainCursor(String containerLink, String searchText,
            boolean recurse, boolean directoriesOnly, boolean photosOnly) 
    {
        if (this.database == null)
        {
            // Both this.getWritableDatabase() and this.getReadableDatabase() return the exact same object instance
            // even over multiple calls to both as long as close is not called on the returned database.
            // We will create one database instance here and use it for the life of the application. 
            this.database = this.getWritableDatabase();
        }
        if (this.database != null && ((this.containerLink != null && this.containerLink.equalsIgnoreCase(containerLink)) || ((this.containerLink == null || this.containerLink.length() == 0)  && (containerLink == null || containerLink.length() == 0))) && ((this.searchText != null && this.searchText.equalsIgnoreCase(searchText)) || ((this.searchText == null || this.searchText.length() == 0) && (searchText == null || searchText.length() == 0))) && this.recurse == recurse) {
            String selection = null;
            if (directoriesOnly) {
                selection = KEY_INPUT_CATEGORY + "=" + SPECIAL_CATEGORY_DIRECTORY;
                
                if (photosOnly) {
                    selection += " OR " + KEY_INPUT_CATEGORY + "=" + Integer.toString(FileUtils.CATEGORY_PHOTOS);
                }
            } else if (photosOnly) {
                selection = KEY_INPUT_CATEGORY + "=" + Integer.toString(FileUtils.CATEGORY_PHOTOS);
            }
            if (this.cursor != null) {
                this.cursor.close();
                this.cursor = null;
            }
            
            String sortCommand = null;
            // Only sort if we are not doing a search.
            if ((searchText == null) || (searchText.trim().length() == 0))
            {
                sortCommand = SORT_TEXT;
            }
        
            try
            {
                this.cursor = this.database.query(TABLE_LIST_CACHE, PROJECTION_ALL, selection, null, null, null, sortCommand);
            }
            catch (Throwable t)
            {
                this.cursor = null;
            }
        } else {
            if (this.cursor != null) {
                this.cursor.close();
            }
            this.cursor = null;
        }
         
        return (this.cursor != null);
    }

    public synchronized void clean(boolean emptyCache) 
    {
        if (this.cursor != null) 
        {
            this.cursor.close();
            this.cursor = null;
        }
        
        if (emptyCache) 
        {
            if (this.database == null)
            {
                // Get the database once for the lifetime of the application
                this.database = getWritableDatabase();
            }
            else
            {
                this.database.delete(TABLE_LIST_CACHE, null, null);
            }
        }
    }
}
