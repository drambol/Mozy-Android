/* Copyright 2010 Tactel AB, Sweden. All rights reserved.
 *                                   _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */

package se.tactel.datahandler.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import se.tactel.datahandler.api.CacheException;
import se.tactel.datahandler.api.buffer.Buffer;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.MemoryStatus;

public class FileCache extends SQLiteOpenHelper {
    
    private static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "fileCache.db";
    
    public static final int MAX_STORAGE_SPACE = 100 * 1000 * 1024; 
    public static final int MIN_FREE_STORAGE = 100 * 1000 * 1024;

    public static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +"Mozy_cache";
    public static final String FILE_EXT = "cache";
    
    public static final int FILE_HEADER_LENGTH = 8;
    public static final long FILE_HEADER = 200;
    
    private boolean cache_fail = false;
    
    /**
     * SINGLETON: An instance of CacheDatabase.
     */
    private static FileCache instance = null;
    
    /**
     * Objects
     */
    private LockManager lock_manager = new LockManager();
    private FileAccessManager file_access_manager = new FileAccessManager();
    
    /**
     * LOCKS
     */
    private static final Object instance_lock = new Object();
    private static final Object db_lock = new Object();
    private static final Object cache_fail_lock = new Object();
    
    /**
     * TABLES
     */
    public static final String  FILE_TABLE         = "file_table"; 
    public static final String  CHUNK_TABLE     = "chunk_table";
    
    /**
     * TRIGGERS
     */
    public static final String AUTO_INCREMENT_TRIGGER_FILE     = "trigger_auto_increment_file_id";
    public static final String DELETE_FK_TRIGGER_FILE         = "trigger_delete_file";
    public static final String INSERT_FK_TRIGGER_CHUNK        = "trigger_fk_insert_chunk";
    public static final String UPDATE_FK_TRIGGER_CHUNK        = "trigger_fk_update_chunk";
    
    
    /**
     * COLUMNS
     */
    //KEY_TABLE
    public static final String     COL_FILE_ID             = "fk_id"; 
    public static final String     COL_PIN_ID                 = "pin_id"; 
    public static final String     COL_PIN_TIMESTAMP         = "pin_timestamp"; 
    public static final String     COL_LAST_ACCESSED         = "last_accessed"; 
    public static final String     COL_FILE_SIZE            = "file_size";
    public static final String     COL_HEADERS                = "headers";
    
    //CHUNK_TABLE
    public static final String  COL_ID                    = "_id"; 
    public static final String  COL_START_INDEX            = "chunk_offset"; 
    public static final String     COL_CHUNK_LENGTH        = "chunk_length";
    
    /**
     * CREATE TABELS
     */
    private static final String FILE_TABLE_CREATE = "create table " +
                                    FILE_TABLE + "(" +     
                                    COL_FILE_ID + " integer, " +
                                    COL_PIN_ID + " text not null, " +
                                    COL_PIN_TIMESTAMP + " integer not null, " +
                                    COL_LAST_ACCESSED + " integer not null, " +
                                    COL_FILE_SIZE + " integer not null, " +
                                    COL_HEADERS + " text, " +
                                    "primary key("+COL_PIN_ID+", "+COL_PIN_TIMESTAMP+"));";
    
    private static final String CHUNK_TABLE_CREATE = "create table " +
                                    CHUNK_TABLE + "(" +     
                                    COL_ID + " integer primary key autoincrement, " +
                                    COL_FILE_ID + " integer not null, " +
                                    COL_START_INDEX + " integer not null, " +
                                    COL_CHUNK_LENGTH + " integer not null);";
    
    /**
     * TRIGGERS
     */
    //fix an auto incrementing integer that is not primary key
    //any writing on database needs to own lock on database_lock since we are using last_insert_rowid()
    private static final String CREATE_AUTO_INCREMENT_TRIGGER_FILE = "create trigger " + AUTO_INCREMENT_TRIGGER_FILE +
                                    " after insert on " + FILE_TABLE + " begin " +
                                    "update " + FILE_TABLE +
                                    " set " + COL_FILE_ID +
                                    " = (select max("+ COL_FILE_ID +") from "+ FILE_TABLE +") + 1 " +
                                    "where rowid = last_insert_rowid();" +
                                    "end;";
    
    //instead of checking if we can delete row in file_table we delete all rows with references in chunk_table
    private static final String CREATE_DELETE_FK_TRIGGER_FILE = "create trigger " + DELETE_FK_TRIGGER_FILE +
                                    " before delete on " + FILE_TABLE + 
                                    " for each row begin" +
                                    " delete from " + CHUNK_TABLE + 
                                    " where " + CHUNK_TABLE + "." + COL_FILE_ID +
                                    " = old." + COL_FILE_ID +
                                    "; " + 
                                    "end;";
    
    //FK triggers for chunk_table
    //http://www.justatheory.com/computers/databases/sqlite/foreign_key_triggers.html
    private static final String CREATE_INSERT_FK_TRIGGER_CHUNK = "create trigger " + INSERT_FK_TRIGGER_CHUNK +
                                    " before insert on " + CHUNK_TABLE + 
                                    " for each row begin" +
                                    " select case " +
                                    " when ((select " + FILE_TABLE + "." + COL_FILE_ID +
                                    " from "+ FILE_TABLE +
                                    " where " + FILE_TABLE + "." + COL_FILE_ID +
                                    " = new." + COL_FILE_ID +
                                    ") is null)" + 
                                    " then raise(abort, 'insert on table " + CHUNK_TABLE + " violates foreign key')" +
                                    "end;" +
                                    "end;";
    
    private static final String CREATE_UPDATE_FK_TRIGGER_CHUNK = "create trigger " + UPDATE_FK_TRIGGER_CHUNK +
                                " before update on " + CHUNK_TABLE +
                                " for each row begin" + 
                                " select case" +
                                " when((select " + COL_FILE_ID + 
                                " from " + FILE_TABLE +
                                " where " + FILE_TABLE + "." + COL_FILE_ID +
                                " = new." + COL_FILE_ID +
                                ") is null)" + 
                                " then raise(abort, 'update on table "+ CHUNK_TABLE +" violates foreign key')" +
                                "end;" + 
                                "end;";
    
    private MountedMediaBroadcastReceiver myReceiver;
    
    private class MountedMediaBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Environment.MEDIA_MOUNTED)) {
                synchronized (cache_fail_lock) {
                    cache_fail = true;
                }
                try {
                    recoverIfNeeded();
                } catch (CacheException e) {
                    LogUtil.debug(this, "Exception when trying to recover media mounted");
                }
            }
        }
    };
    
    
    
    private class FileAccessHolder {
        String pin_id;
        long pin_timestamp;
        int file_id;
        int content_length;
        long last_accessed;
        String headers;
        RandomAccessFile raf = null;
        Vector<Chunk> chunks = null;

        
        public FileAccessHolder(String pin_id, long pin_timestamp, int file_id) throws IOException {
            resetAll();
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
            this.file_id = file_id;
            this.chunks = new Vector<Chunk>();

            try {
                File path = new File(FILE_PATH);
                if(!path.isDirectory()) {
                    path.mkdir();
                }
                boolean write_header = false;
                File f = new File(path, getFileName(file_id));
                if (f.canWrite() && !f.exists()) {
                    write_header = true;
                    f.createNewFile();
                }
                this.raf = new RandomAccessFile(f, "rw");
                if (write_header) {
                    if (raf.getFilePointer() != 0) {
                        raf.seek(0);
                    }
                    raf.writeLong(FILE_HEADER);
                }
            } catch (FileNotFoundException fe) {
                synchronized (cache_fail_lock) {
                    cache_fail = true;
                }
                throw new IOException();
            }
        }
        
        void resetAll() {
            pin_id = "";
            pin_timestamp = -1;
            file_id = -1;
            content_length = -1;
            last_accessed = -1;
            headers = null;

            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ioe) {
            }
            raf = null;
            
            if (chunks != null) {
                Iterator<Chunk> it = chunks.iterator();
                while (it.hasNext()) {
                    Chunk chunk = it.next();
                    chunks.remove(chunk);
                }
            }
        }
        
        Chunk getChunkAtOffset(int offset) {
            synchronized (this) {
                for (Chunk chunk : chunks) {
                    synchronized (chunk) {
                        if (offset >= chunk.start_index && offset <= chunk.start_index + chunk.chunk_size) {
                            return chunk;
                        }
                    }
                }
                return null;
            }
        }
        
        void removeChunk(int start_index, int chunk_size) {
            synchronized (this) {
                Iterator<Chunk> it = chunks.iterator();
                while (it.hasNext()) {
                    Chunk chunk = it.next();
                    synchronized (chunk) {
                        if (start_index == chunk.start_index && chunk_size == chunk.chunk_size) {
                            chunks.remove(chunk);
                        }
                    }
                }
            }
        }
        
        void addChunk(int start_index, int chunk_size) {
            synchronized (this) {
                chunks.add(new Chunk(start_index, chunk_size));
            }
        }

        
        void saveLastAccessed() throws CacheException {
            ContentValues valuesFile = new ContentValues();
            valuesFile.put(COL_LAST_ACCESSED, last_accessed);
            
            synchronized (db_lock) {
                SQLiteDatabase db =getWritableDbOrCacheException();
                
                try {
                    db.update(FILE_TABLE, valuesFile, COL_FILE_ID +" = " + file_id, null);
                } catch (Throwable thr) {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw new CacheException();
                }
            }
        }
    }
    

    private class FileAccessManager {
        
        private static final int MAX_SIZE = 3;
        private Vector<FileAccessHolder> all_access;
        
        public FileAccessManager() {
            all_access = new Vector<FileAccessHolder>();
        }
        
        public FileAccessHolder getFileAccessHolderIfExists(long pin_timestamp, String pin_id) {
            synchronized (this) {
                for (FileAccessHolder fah : all_access) {
                    synchronized (fah) {
                        if (fah.pin_id.equals(pin_id) && fah.pin_timestamp == pin_timestamp) {
                            return fah;
                        }
                    }
                    
                }
            }
            return null;
        }
        
        public void removeFileAccessHolder(FileAccessHolder fah) {
            synchronized (this) {
                all_access.remove(fah);    
            }
        }
        
        public void removeAllFileAccessHolders() {
            synchronized (this) {
                Iterator<FileAccessHolder> it = all_access.iterator();
                while (it.hasNext()) {
                    FileAccessHolder fah = it.next();
                    synchronized (fah) {
                        fah.resetAll();
                        it.remove();
                    }
                }
            }
        }
        
        public FileAccessHolder getFileAccessHolder(long pin_timestamp, String pin_id, int file_id) throws IOException, CacheException {
            
            if (file_id <= 0) {
                throw new CacheException();
            }
            synchronized (this) {
                //if already exists, return old one
                for (FileAccessHolder fah : all_access) {
                    synchronized (fah) {
                        if (fah.file_id == file_id) {
                            return fah;
                        }
                    }
                }
                //remove lru
                if (all_access.size() >= MAX_SIZE) {
                    FileAccessHolder lowest_last_accessed = null;
                    for (FileAccessHolder fah : all_access) {
                        if (lowest_last_accessed == null) {
                            lowest_last_accessed = fah;
                        } else {
                            if (fah.last_accessed <= lowest_last_accessed.last_accessed) {
                                lowest_last_accessed = fah;
                            }
                        }
                    }
                    synchronized (lowest_last_accessed) {
                        lowest_last_accessed.saveLastAccessed();
                        lowest_last_accessed.resetAll();
                        all_access.remove(lowest_last_accessed);
                    }
                }
                
                FileAccessHolder fah = new FileAccessHolder(pin_id, pin_timestamp, file_id);
                all_access.add(fah);
                return fah;
            }
        }
    }
    
    
    private class LockManager extends Vector<IntervalLock> {
        
        private static final long serialVersionUID = 1L;

        public synchronized IntervalLock getLockIntervalToWaitFor(IntervalLock param) {
            IntervalLock returnValue = null;
            
            for (int i = 0; i < this.size(); i++) {
                IntervalLock il = this.elementAt(i);
                synchronized (il) {
                    if (il.is_executing || ((param.type == IntervalLock.TYPE_READ || param.type == IntervalLock.TYPE_READ_ONE) && il.type == IntervalLock.TYPE_STORE) && il != param) {
                        if (param.pin_id.equals(il.pin_id) && param.pin_timestamp == (il.pin_timestamp) ) {
                            if ((il.offset < param.offset && il.offset + il.chunk_size > param.offset) ||
                                (il.offset < param.offset + param.chunk_size && il.offset + il.chunk_size > param.offset + param.chunk_size) ||
                                (il.offset >= param.offset && il.offset + il.chunk_size <= param.offset + param.chunk_size) ||
                                (il.offset < param.offset && il.offset + il.chunk_size > param.offset + param.chunk_size)) {
                                returnValue = il;
                                break;
                            } 
                        }
                    }
                }
            }
            return returnValue;
        }
        
        public synchronized void addIntervalLock(IntervalLock il) {
            this.add(il);
        }
        
        public synchronized void removeLock(IntervalLock param) {
            this.remove(param);
            synchronized (param) {
                param.notifyAll();
                param.hasNotifiedListeners = true;
            }
        }
    }
    
    public class IntervalLock {
        public static final int TYPE_STORE = 58;
        public static final int TYPE_READ = 59;
        public static final int TYPE_READ_ONE = 60;
        
        public int type;
        
        public Buffer bytes_to_store;
        public byte[] bytes_to_read;
        public int length_of_bytes;
        public int chunk_size;
        public String pin_id;
        public long pin_timestamp;
        public int offset;
        public Header[] headers;
        
        public boolean is_executing = false;
        public boolean is_aborted = false;
        
        public boolean hasNotifiedListeners = false;
        
        public IntervalLock(Buffer bytes_to_store, int length_of_bytes, int content_length, int offset, String pin_id, long pin_timestamp, Header[] headers) {
            this.type = TYPE_STORE;
            this.bytes_to_store = bytes_to_store;
            this.length_of_bytes = length_of_bytes;
            this.chunk_size = content_length;
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
            this.offset = offset;
            this.headers = headers;
        }
        
        public IntervalLock(byte[] bytes_to_read, int read_length, int write_offset, int offset, String pin_id, long pin_timestamp) {
            this.type = TYPE_READ;
            this.bytes_to_read = bytes_to_read;
            this.offset = offset;
            this.chunk_size = bytes_to_read.length;
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
        }
        
        public IntervalLock(byte[] bytes_to_read, int offset, String pin_id, long pin_timestamp) {
            this.type = TYPE_READ;
            this.bytes_to_read = bytes_to_read;
            this.offset = offset;
            this.chunk_size = bytes_to_read.length;
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
        }
        
        public IntervalLock(int offset, String pin_id, long pin_timestamp) {
            this.type = TYPE_READ_ONE;
            this.offset = offset;
            this.chunk_size = 1;
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
        }
        
        
        /**
         * @return true if all data from the offset of this store is available. (returns false is abort has already been called on this object or TYPE == READ)
         * @throws IOException
         * @throws CacheException
         */
        public boolean executeStore() throws IOException, CacheException {
            
            boolean returnValue = false;
            IntervalLock lock_owner = null;
            
            if (type == TYPE_STORE) {
                redo:
                while (true) {
                    lock_owner = getLockManager().getLockIntervalToWaitFor(this);
                    if (lock_owner != null) {
                        synchronized (lock_owner) {
                            if (!lock_owner.hasNotifiedListeners) {
                                try {
                                    lock_owner.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    } else {
                        try {
                            if (!is_aborted) {
                                synchronized (this) {
                                    is_executing = true;
                                }
                                returnValue = store(bytes_to_store, length_of_bytes, chunk_size, offset, pin_id, pin_timestamp, headers, (headers == null) ? false : true);
                            }
                        } finally {
                            getLockManager().removeLock(this);
                        }
                        break redo;
                    }
                }
            } else {
                getLockManager().removeLock(this);
            }
            return returnValue;
        }
        
        public void abort() {
            is_aborted = true;
            getLockManager().removeLock(this);
        }
    }
    
    
    
    public static FileCache getInstance(Context context) {
        synchronized (instance_lock) {
            if (instance == null) {
                instance = new FileCache(context.getApplicationContext());
                synchronized (cache_fail_lock) {
                    if (instance != null) {
                        instance.cache_fail = true;
                    }
                }
            }
        }
        return instance;
    }
    
    private FileCache(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addDataScheme("file"); 
        myReceiver = new MountedMediaBroadcastReceiver();
        context.getApplicationContext().registerReceiver(myReceiver, intentFilter);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
        createTriggers(db);
        
        deleteAll();
    }

    
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db); 
    }
    
    private void createTables(SQLiteDatabase db) {
        db.execSQL(FILE_TABLE_CREATE); 
        db.execSQL(CHUNK_TABLE_CREATE);
    }
    
    private void createTriggers(SQLiteDatabase db) {
        db.execSQL(CREATE_AUTO_INCREMENT_TRIGGER_FILE);
        db.execSQL(CREATE_DELETE_FK_TRIGGER_FILE);
        db.execSQL(CREATE_INSERT_FK_TRIGGER_CHUNK);
        db.execSQL(CREATE_UPDATE_FK_TRIGGER_CHUNK);
    }

    private void dropTables(SQLiteDatabase db) {
        db.execSQL("drop table if exists " + FILE_TABLE);
        db.execSQL("drop table if exists " + CHUNK_TABLE);
        db.execSQL("drop trigger if exists " + AUTO_INCREMENT_TRIGGER_FILE);
        db.execSQL("drop trigger if exists " + DELETE_FK_TRIGGER_FILE);
        db.execSQL("drop trigger if exists " + INSERT_FK_TRIGGER_CHUNK);
        db.execSQL("drop trigger if exists " + UPDATE_FK_TRIGGER_CHUNK);
    }
    
    
    public IntervalLock getStoreMethod(Buffer bytes_to_store, int length_of_bytes, int content_length, int offset, String pin_id, long pin_timestamp, Header[] headers) {
        IntervalLock interval_lock = new IntervalLock(bytes_to_store, length_of_bytes, content_length, offset, pin_id, pin_timestamp, headers);
        lock_manager.addIntervalLock(interval_lock);
        return interval_lock;
    }
    
    public int read(byte[] bytes_to_read, int read_length, int write_offset, int offset, String pin_id, long pin_timestamp) throws IOException, CacheException {
        IntervalLock interval_lock = new IntervalLock(bytes_to_read, read_length, write_offset, offset, pin_id, pin_timestamp);
        lock_manager.addIntervalLock(interval_lock);
        
        int returnValue = -1;
        IntervalLock lock_owner = null;
        
        redo:
        while (true) {
            lock_owner = getLockManager().getLockIntervalToWaitFor(interval_lock);
            if (lock_owner != null) {
                synchronized (lock_owner) {
                    if (!lock_owner.hasNotifiedListeners) {
                        try {
                            lock_owner.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else {
                try {
                    synchronized (interval_lock) {
                        if (!interval_lock.is_aborted) {
                            interval_lock.is_executing = true;
                        } 
                        returnValue = readAll(bytes_to_read, read_length, write_offset, offset, pin_id, pin_timestamp);
                    }
                } finally {
                    getLockManager().removeLock(interval_lock);
                }
                break redo;
            }
        }
        return returnValue;
    }
    
    public int read(byte[] bytes_to_read, int offset, String pin_id, long pin_timestamp) throws IOException, CacheException {
        
        IntervalLock interval_lock = new IntervalLock(bytes_to_read, offset, pin_id, pin_timestamp);
        lock_manager.addIntervalLock(interval_lock);
        
        int returnValue = -1;
        IntervalLock lock_owner = null;
        
        redo:
        while (true) {
            lock_owner = getLockManager().getLockIntervalToWaitFor(interval_lock);
            if (lock_owner != null) {
                synchronized (lock_owner) {
                    if (!lock_owner.hasNotifiedListeners) {
                        try {
                            lock_owner.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
            } else {
                try {
                    synchronized (interval_lock) {
                        if (!interval_lock.is_aborted) {
                            interval_lock.is_executing = true;
                        }
                        returnValue = readAll(bytes_to_read, bytes_to_read.length, 0, offset, pin_id, pin_timestamp);
                    }
                } finally {
                    getLockManager().removeLock(interval_lock);
                }
                break redo;
            }
        }
        return returnValue;
    }
    
    public int read(int offset, String pin_id, long pin_timestamp) throws IOException, CacheException {
        
        IntervalLock interval_lock = new IntervalLock(offset, pin_id, pin_timestamp);
        lock_manager.addIntervalLock(interval_lock);
        
        int returnValue = -1;
        IntervalLock lock_owner = null;
        
        redo:
        while (true) {
            lock_owner = getLockManager().getLockIntervalToWaitFor(interval_lock);
            if (lock_owner != null) {
                synchronized (lock_owner) {
                    if (!lock_owner.hasNotifiedListeners) {
                        try {
                            lock_owner.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }
                continue redo;
            } else {
                try {
                    synchronized (interval_lock) {
                        if (!interval_lock.is_aborted) {
                            interval_lock.is_executing = true;
                        }
                        byte[] b = new byte[1];
                        returnValue = this.readAll(b, b.length, 0, offset, pin_id, pin_timestamp);
                        if (returnValue != -1) {
                            returnValue = b[0];
                        } 
                    }
                } finally {
                    getLockManager().removeLock(interval_lock);
                }
                break redo;
            }
        }
        return returnValue;
    }
    
    public int getContentLength(String pin_id, long pin_timestamp) throws IOException, CacheException {
        int returnValue = stringToInt(findPropertyFor(COL_FILE_SIZE, pin_id, pin_timestamp));
        return returnValue;
    }
    
    /**
     * Call as little as possible
     * 
     * @param pin_id
     * @param pin_timestamp
     * @return
     * @throws IOException
     * @throws CacheException
     */
    public Header[] getHeaders(String pin_id, long pin_timestamp) throws IOException, CacheException {
        return stringToHeaders(findPropertyFor(COL_HEADERS, pin_id, pin_timestamp));
    }
    
    /**
     * Check how much continuous data is saved at offset and forward. 
     * 
     * @param offset
     * @param pin_id
     * @param pin_timestamp
     * @return length of continuous data, 0 if no data available at offset
     */
    public int available(int offset, String pin_id, long pin_timestamp) throws IOException, CacheException {
        int availableDataLength = 0;
        int file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
        FileAccessHolder fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
        synchronized (fah) {
            Chunk chunk = fah.getChunkAtOffset(offset);
            if (chunk != null) {
                synchronized (chunk) {
                    availableDataLength = chunk.start_index + chunk.chunk_size - offset;
                }
            }
        }
        
        if (availableDataLength <= 0) {
            synchronized (db_lock) {
                SQLiteDatabase database = getWritableDbOrCacheException();
                Cursor cursor = null;
                int cursor_count = 0;
                String query  = "select * from " + CHUNK_TABLE + 
                                " where " + COL_FILE_ID + 
                                " = " + file_id +
                                " and " + offset + 
                                " >= " + COL_START_INDEX + 
                                " and " + offset + 
                                " < " + COL_START_INDEX + 
                                " + " + COL_CHUNK_LENGTH;
                try {
                    cursor = database.rawQuery(query, null);
                    if (cursor != null) {
                        try {
                            cursor_count = cursor.getCount();
                            if (cursor_count > 0 && cursor.moveToFirst()) {
                                int start_index = cursor.getInt(cursor.getColumnIndex(COL_START_INDEX));
                                int chunk_size = cursor.getInt(cursor.getColumnIndex(COL_CHUNK_LENGTH));
                                
                                availableDataLength = start_index + chunk_size - offset;
                                fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                                synchronized (fah) {
                                    fah.addChunk(start_index, chunk_size);
                                }
                            } 
                        } finally {
                            cursor.close();
                        }
                    }
                } catch (Throwable thr) {
                    LogUtil.exception(this, "exception when reading cursor matching tuples", thr);
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw new CacheException();
                }
            }
        }
        return availableDataLength;
    }
    
    
    private String headersToString(Header[] headers) {
        if (headers != null && headers.length > 0) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < headers.length; i++) {
                sb.append(headers[i].getName()+":"+headers[i].getValue());
                if (i+1 < headers.length) {
                    sb.append(";");
                }
            }
            return sb.toString();
        } else {
            return null;
        }
    }
    
    private Header[] stringToHeaders(String headers) {
        try {
            if (headers != null && headers.length() > 0) {
                String[] pairs = headers.split(";");
                Header[] returnValue = new Header[pairs.length];
                
                int mark;
                String str;
                for (int i = 0; i < returnValue.length; ++i) {
                    str = pairs[i];
                    mark = str.indexOf(':');
                    if (mark >= 0) {
                        returnValue[i] = new BasicHeader(str.substring(0, mark), str.substring(mark));
                    } else {
                        returnValue[i] = new BasicHeader(str, "");
                    }
                }
                return returnValue;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public LockManager getLockManager() {
        return lock_manager;
    }
    
    /**
     * 
     * @param bytes_to_store data to be stored in cache database
     * @param length_of_bytes from position 0 in bytes_to_store that is filled
     * @param content_length the total length of the content (file etc) which bytes_to_store is from
     * @param offset at what offset from byte 0 that bytes_to_store starts at
     * @param pin_id identifier
     * @param pin_timestamp identifier
     * @return true if all bytes after offset are available in cache after this store() is complete
     * @throws CacheException if the database could not be accessed (missing sd card?)
     */
    private boolean store(Buffer buffer, int length_of_bytes, int content_length, int offset, String pin_id, long pin_timestamp, Header[] headers_array, boolean save_headers) throws CacheException, IOException {
        recoverIfNeeded();
        boolean returnValue = false;
        
        int file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
        if (file_id <= 0) {
            addNewFile(content_length, pin_id, pin_timestamp);
            file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
        }
        
        String headers_string = headersToString(headers_array);
    
        FileAccessHolder fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
        synchronized (fah) {
            try {
                if (fah.raf.getFilePointer() != offset + FILE_HEADER_LENGTH) {
                    fah.raf.seek(offset + FILE_HEADER_LENGTH);
                }
                fah.raf.write(buffer.getContent().array(), 0, length_of_bytes);
            } catch (IOException ioe) {
                synchronized (cache_fail_lock) {
                    cache_fail = true;
                }
                throw new CacheException();
            }
        }
        
        
        /**
         * File has been updated do check if there are tuples that now overlap each other
         */
        ArrayList<ChunkWithId> ids = new ArrayList<ChunkWithId>();
        
        String query_intersecting  = "select * " + 
                                    " from " + CHUNK_TABLE + 
                                    " where " + COL_FILE_ID + 
                                    " = " + file_id +
                                    " and ((("+COL_START_INDEX+ " < " + offset + ") and (" +COL_CHUNK_LENGTH+" + "+COL_START_INDEX +" >= " +offset+ "))" +
                                    " or (("+COL_START_INDEX+" <= "+ length_of_bytes +"+" +offset+") and ("+COL_START_INDEX+" + "+COL_CHUNK_LENGTH+" > " +offset+" + "+length_of_bytes +"))" +
                                    " or (("+COL_START_INDEX+" >= "+offset+") and (" +COL_START_INDEX+" + "+COL_CHUNK_LENGTH+" <= "+length_of_bytes+" + "+offset+"))" +
                                    " or (("+COL_START_INDEX+" < "+offset+") and ("+COL_CHUNK_LENGTH+" + "+COL_START_INDEX+" > "+offset+" + "+length_of_bytes+")))" +
                                    " order by " + COL_START_INDEX;
        
        synchronized (db_lock) {
            
            SQLiteDatabase database = getWritableDbOrCacheException();

            int cursor_count = 0;
            int lowest_offset = offset;
            int highest_length = offset + length_of_bytes;
                
            try {
                Cursor cursor = null;
                cursor = database.rawQuery(query_intersecting, null);
                 if (cursor != null) {
                    try {
                        if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                            cursor_count = cursor.getCount();
                            int id = 0;
                            int start_index = 0;
                            int chunk_size = 0;
                            do {
                                id = cursor.getInt(cursor.getColumnIndex(COL_ID));
                                start_index = cursor.getInt(cursor.getColumnIndex(COL_START_INDEX));
                                chunk_size = cursor.getInt(cursor.getColumnIndex(COL_CHUNK_LENGTH));
                                ids.add(new ChunkWithId(id, start_index, chunk_size));
                                lowest_offset = (start_index < lowest_offset) ? start_index : lowest_offset;
                                highest_length = (start_index + chunk_size > highest_length) ? (start_index + chunk_size) : highest_length;
                            } while (cursor.moveToNext());
                        }
                    } finally {
                        cursor.close();
                    }
                }
            
                ContentValues values = new ContentValues();
                values.put(COL_START_INDEX, lowest_offset);
                values.put(COL_CHUNK_LENGTH, highest_length - lowest_offset);
                boolean delete = false;
                if (cursor_count > 1) {
                    delete = true;
                    for (ChunkWithId chunk : ids) {
                        database.delete(CHUNK_TABLE, COL_ID+ " = "+chunk.id, null);
                    }
                    values.put(COL_FILE_ID, file_id);
                    database.insertOrThrow(CHUNK_TABLE, null, values);
                } else if (cursor_count == 1) {
                    database.update(CHUNK_TABLE, values, COL_FILE_ID + " = "+file_id, null);
                } else if (cursor_count == 0) {
                    values.put(COL_FILE_ID, file_id);
                    database.insertOrThrow(CHUNK_TABLE, null, values);
                }
                
                if (save_headers) {
                    ContentValues valuesFile = new ContentValues();
                    valuesFile.put(COL_HEADERS, headers_string);
                    database.update(FILE_TABLE, valuesFile, COL_FILE_ID +" = " + file_id, null);    
                }
                
                fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                synchronized (fah) {
                    fah.content_length = content_length;
                    if (delete) {
                        for (ChunkWithId chunk : ids) {
                            fah.removeChunk(chunk.start_index, chunk.chunk_size);
                        }
                        fah.addChunk(values.getAsInteger(COL_START_INDEX), values.getAsInteger(COL_CHUNK_LENGTH));
                    } else {
                        Chunk chunk = fah.getChunkAtOffset(values.getAsInteger(COL_START_INDEX));
                        if (chunk != null) {
                            synchronized (chunk) {
                                chunk.start_index = values.getAsInteger(COL_START_INDEX);
                                chunk.chunk_size = values.getAsInteger(COL_CHUNK_LENGTH);
                            }
                        } else {
                            fah.addChunk(values.getAsInteger(COL_START_INDEX), values.getAsInteger(COL_CHUNK_LENGTH));
                        }
                    }
                    if (save_headers) {
                        fah.headers = headers_string;
                    }
                    fah.last_accessed = System.nanoTime();
                    
                }
                if (highest_length == content_length) {
                    returnValue = false;
                }
            } catch (Throwable thr) {
                LogUtil.exception(this, "exception in store", thr );
                if (thr instanceof IOException) {
                    throw (IOException)thr;
                } else if (thr instanceof CacheException) {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw (CacheException)thr;
                } else {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw new CacheException();
                }
            }
        }
        return returnValue;
    }
    
    /**
     * 
     * @param bytes_to_read array to will be filled with data
     * @param read_length the number of bytes that will be read from file to bytes_to_read
     * @param write_offset at what index in bytes_to_read to start writing bytes at
     * @param offset defines the point at which bytes_to_read will start reading
     * @param pin_id identifier
     * @param pin_timestamp identifier
     * @return -1 if EOL, else length of read data
     * @throws CacheException if no data is stored at offset or if no item with matching ids
     * @throws IOException if errors reading from database or file
     */
    private int readAll(byte[] bytes_to_read, int read_length, int write_offset, int offset, String pin_id, long pin_timestamp) throws CacheException, IOException {
        recoverIfNeeded();
        
        /**
         * Initial checks
         * -1 == EOL, can not be returned if the file does not exist
         * CacheException if entry does not exist
         */
        
        int file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
        if (file_id <= 0) {
            throw new CacheException();
        }
        
        int content_length = getContentLength(pin_id, pin_timestamp);
        if (content_length == -1) {return -1;}
        if (offset >= content_length) {return -1;}
        
        FileAccessHolder fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
        
        int availableDataLength  = available(offset, pin_id, pin_timestamp);
        if (availableDataLength > read_length) {
            availableDataLength = read_length;
        }
        
        if (availableDataLength > 0) {
            while (true) {
                fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                synchronized (fah) {
                    if (fah.raf != null) {
                        try {
                            if (fah.raf.getFilePointer() != offset + FILE_HEADER_LENGTH) {
                                fah.raf.seek(offset + FILE_HEADER_LENGTH);
                            }
                            fah.raf.read(bytes_to_read, write_offset, availableDataLength);
                            fah.last_accessed = System.nanoTime();
                            break;
                        } catch (IOException ioe) {
                            synchronized (cache_fail_lock) {
                                cache_fail = true;
                            }
                            throw new CacheException();
                        }
                    }
                }
            }
        }
        if (availableDataLength == 0) {
            throw new CacheException();
        }
        return availableDataLength;
    }
    
    
    /**
     * 
    * @param pin_id identifier
     * @param pin_timestamp identifier
     * @throws CacheException if database unavailable
     * @throws IOException if errors reading from database or file
     */
    private void delete(String pin_id, long pin_timestamp) throws IOException, CacheException {
            
        int file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
        if (file_id > 0) {
            synchronized (db_lock) {
                SQLiteDatabase database = getWritableDbOrCacheException();
                
                deleteFile(file_id);
                if (database != null) {
                    try {
                        database.execSQL("delete from " + FILE_TABLE + " where " + COL_FILE_ID + " = " +file_id);
                        FileAccessHolder fah = file_access_manager.getFileAccessHolderIfExists(pin_timestamp, pin_id);
                        if (fah != null) {
                            synchronized (fah) {
                                fah.resetAll();
                                file_access_manager.removeFileAccessHolder(fah);
                            }
                        }
                    } catch (Throwable thr) {
                        LogUtil.exception(this, "Exception in delete", thr);
                        if (thr instanceof IOException) {
                            throw (IOException)thr;
                        } else if (thr instanceof CacheException) {
                            synchronized (cache_fail_lock) {
                                cache_fail = true;
                            }
                            throw (CacheException)thr;
                        } else {
                            synchronized (cache_fail_lock) {
                                cache_fail = true;
                            }
                            throw new CacheException();
                        }
                    }
                } else {
                    throw new CacheException();
                }
            }
        } else {
            throw new CacheException();
        }    
    }
    
    //long start_time = 0;
    private String findPropertyFor(String column, String pin_id, long pin_timestamp) throws IOException, CacheException {
        FileAccessHolder fah = file_access_manager.getFileAccessHolderIfExists(pin_timestamp, pin_id);
        if (fah != null) {
            synchronized (fah) {
                if (column.equals(COL_FILE_ID)) {
                    return Integer.toString(fah.file_id);
                } else if (column.equals(COL_FILE_SIZE) && fah.content_length > 0) {
                    return Integer.toString(fah.content_length);
                } else if (column.equals(COL_HEADERS)) {
                    return fah.headers;
                } 
            }
        }
        String property = null;
        
        /**
         * Does a tuple in C_KEY_TABLE EXIST FOR CURRENT ID?
         */
        String query  = "select * "+ 
                        " from " + FILE_TABLE + 
                        " where " + COL_PIN_ID + 
                        " = '" + pin_id + "'" +
                        " and " + COL_PIN_TIMESTAMP +
                        " = " + pin_timestamp;
        
        synchronized (db_lock) {
            SQLiteDatabase database = getWritableDbOrCacheException();
            
            if (database != null) {
                Cursor cursor = null;
                try {
                    cursor = database.rawQuery(query, null);
                     if (cursor != null) {
                        try {
                            if (cursor.getCount() > 0 && cursor.moveToFirst()) {
                                int file_id = cursor.getInt(cursor.getColumnIndex(COL_FILE_ID));
                                int content_length = cursor.getInt(cursor.getColumnIndex(COL_FILE_SIZE));
                                String headers = cursor.getString(cursor.getColumnIndex(COL_HEADERS));
                                fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                                
                                synchronized (fah) {
                                    fah.content_length = content_length;
                                    fah.headers = cursor.getString(cursor.getColumnIndex(COL_HEADERS));
                                }
                                if (column.equals(COL_FILE_ID)) {
                                    property = Integer.toString(file_id);
                                } else if (column.equals(COL_FILE_SIZE)) {
                                    property = Integer.toString(content_length);
                                } else if (column.equals(COL_HEADERS)) {
                                    property = headers;
                                }
                                //LogUtil.debug(this, "file_id "+(file_id-1)+ " took "+((System.nanoTime()-start_time)/1000000000) );
                                //start_time = System.nanoTime();
                            }
                        } finally {
                            cursor.close();
                        }
                    }
                } catch (Throwable thr) {
                    if (thr instanceof IOException) {
                        throw (IOException)thr;
                    } else if (thr instanceof CacheException) {
                        synchronized (cache_fail_lock) {
                            cache_fail = true;
                        }
                        throw (CacheException)thr;
                    } else {
                        synchronized (cache_fail_lock) {
                            cache_fail = true;
                        }
                        throw new CacheException();
                    }
                }
            } else {
                synchronized (cache_fail_lock) {
                    cache_fail = true;
                }
                throw new CacheException();
            }
        }
        return property;
    }
    
    
    private void addNewFile(int content_length, String pin_id, long pin_timestamp) throws IOException, CacheException {
        if (content_length > MAX_STORAGE_SPACE) {
            throw new CacheException();
        }
        
        if (!MemoryStatus.externalMemoryAvailable()) {
            synchronized (cache_fail_lock) {
                cache_fail = true;
            }
            throw new CacheException();
        }
        
        if (MemoryStatus.getTotalExternalMemorySize() < MIN_FREE_STORAGE) {
            throw new CacheException();
        }
        
        int total_file_size = 0;
        boolean delete_all = false;
        
        synchronized (db_lock) {
            SQLiteDatabase database = getWritableDbOrCacheException();
            
            try {
                Cursor cursorSum = null;
                String queryTotalFileSize = "select sum("+COL_FILE_SIZE+") from "+FILE_TABLE;
                try {
                    cursorSum = database.rawQuery(queryTotalFileSize, null);
                    if (cursorSum != null) {
                        try {
                            if (cursorSum.moveToFirst()) {
                                total_file_size = cursorSum.getInt(0);
                            }
                        } finally {
                            cursorSum.close();
                        }
                    }
                }  catch (Throwable thr) {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    LogUtil.exception(this, "exception when trying to get sum of all content_lengths", thr);
                    throw new CacheException();
                }
            
                if (MIN_FREE_STORAGE > MemoryStatus.getAvailableExternalMemorySize() + total_file_size) {
                    delete_all = true;
                }
                
                if (total_file_size + content_length > MAX_STORAGE_SPACE || 
                    MemoryStatus.getAvailableExternalMemorySize() - content_length < MIN_FREE_STORAGE || 
                    delete_all)
                {
                    //need to delete as many files as needed
                    ArrayList<Row> ids = new ArrayList<Row>();
                    String getAllFiles = "select * from "+FILE_TABLE+" order by "+COL_LAST_ACCESSED+" asc";
                    
                    Cursor cursorAllFiles = null;
        
                    cursorAllFiles = database.rawQuery(getAllFiles, null);
                    if (cursorAllFiles != null) {
                        try {
                            if (cursorAllFiles.moveToFirst()) {
                                int freed_space = 0;
                                do {
                                    if (total_file_size + content_length - freed_space > MAX_STORAGE_SPACE || 
                                            MemoryStatus.getAvailableExternalMemorySize() - content_length + freed_space < MIN_FREE_STORAGE || 
                                            delete_all) {
                                        ids.add(new Row (cursorAllFiles.getString(cursorAllFiles.getColumnIndex(COL_PIN_ID)), 
                                            cursorAllFiles.getLong(cursorAllFiles.getColumnIndex(COL_PIN_TIMESTAMP))));
                                        freed_space += cursorAllFiles.getInt(cursorAllFiles.getColumnIndex(COL_FILE_SIZE));
                                    } else {
                                        break;
                                    }
                                }
                                while (cursorAllFiles.moveToNext());
                            }
                        } finally {
                            cursorAllFiles.close();
                        }
                    }
                    
                    for (Row r : ids) {
                        delete(r.pin_id, r.pin_timestamp);
                    }
                    if (MIN_FREE_STORAGE > MemoryStatus.getAvailableExternalMemorySize() - content_length) {
                        throw new CacheException();
                    }
                }
        
                int file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
            
                /**
                 * add new tuple in db
                 * try to add new file
                 * if success update last accessed
                 * if failure delete row from database
                 */
                ContentValues values = new ContentValues();
                values.put(COL_PIN_ID, pin_id);
                values.put(COL_PIN_TIMESTAMP, pin_timestamp);
                values.put(COL_FILE_SIZE, content_length);
                values.put(COL_LAST_ACCESSED, 0);
                values.put(COL_FILE_ID, 0);
                database.insertOrThrow(FILE_TABLE, null, values);
                
                file_id = stringToInt(findPropertyFor(COL_FILE_ID, pin_id, pin_timestamp));
                
                //create new file
                file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                
                ContentValues valuesFile = new ContentValues();
                valuesFile.put(COL_LAST_ACCESSED, System.nanoTime());
                database.update(FILE_TABLE, valuesFile, COL_FILE_ID +" = " + file_id, null);
                    
                FileAccessHolder fah = file_access_manager.getFileAccessHolder(pin_timestamp, pin_id, file_id);
                synchronized (fah) {
                    fah.content_length = content_length;
                    fah.last_accessed = valuesFile.getAsLong(COL_LAST_ACCESSED);
                }
            } catch (Throwable thr) {
                LogUtil.exception(this, "exception in addNewFile", thr);
                if (thr instanceof IOException) {
                    throw (IOException)thr;
                } else if (thr instanceof CacheException) {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw (CacheException)thr;
                } else {
                    synchronized (cache_fail_lock) {
                        cache_fail = true;
                    }
                    throw new CacheException();
                }
            }
        } 
    }
    
    class Row {
        String pin_id;
        long pin_timestamp;
        Row(String pin_id, long pin_timestamp){
            this.pin_id = pin_id;
            this.pin_timestamp = pin_timestamp;
        }
    }
    
    class Chunk {
        int start_index;
        int chunk_size;
        Chunk(int start_index, int chunk_size) {
            this.start_index = start_index;
            this.chunk_size = chunk_size;
        }
    }
    
    class ChunkWithId {
        int id;
        int start_index;
        int chunk_size;
        ChunkWithId (int id, int start_index, int chunk_size) {
            this.id = id;
            this.start_index = start_index;
            this.chunk_size = chunk_size;
        }
    }

    
    private void deleteAll() {
        try {
            File folder = new File(FILE_PATH);
            if (folder.isDirectory() && folder.exists()) {
                File files[] = folder.listFiles();
                for (File f : files) {
                    if (f.exists()) {
                        f.delete();
                    }
                }
            }
        } catch (Throwable thr) {
            synchronized (cache_fail_lock) {
                cache_fail = true;
            }
        }
    }
    
    private void deleteFile(int file_id) {
        File f = new File(FILE_PATH, getFileName(file_id));
        if (f.exists()) {
            f.delete();
        }
    }
    
    private String getFileName(int file_id) {
        return file_id + "." + FILE_EXT;
    }
    
    private SQLiteDatabase getWritableDbOrCacheException() throws CacheException {
        try {
            return getWritableDatabase();
        } catch (SQLiteException e) {
            throw new CacheException();
        }
    }
    
    private int stringToInt(String string) {
        int returnValue = -1;
        if (string != null) {
            returnValue = Integer.parseInt(string);
        }
        return returnValue;
    }
    
    private void recoverIfNeeded() throws CacheException {
        if (MemoryStatus.externalMemoryAvailable()) {
            synchronized (db_lock) {
                synchronized (cache_fail_lock) {
                    if (cache_fail) {
                        deleteAll();
                        SQLiteDatabase db = null;
                        try {
                            db = getWritableDatabase();
                        } catch (SQLiteException e) {
                            throw new CacheException();
                        }
                        if (db != null) {
                            db.delete(CHUNK_TABLE, null, null);
                            db.delete(FILE_TABLE, null, null);
                            file_access_manager.removeAllFileAccessHolders();
                            cache_fail = false;
                        } else {
                            throw new CacheException();
                        }
                    } 
                }
            }
        } else {
            throw new CacheException();
        }
    }
        
    
    @Override
    protected void finalize() {
        try {
            file_access_manager.removeAllFileAccessHolders();
            close();
            super.finalize();
        } catch (Throwable thr) {
        }
    }
}