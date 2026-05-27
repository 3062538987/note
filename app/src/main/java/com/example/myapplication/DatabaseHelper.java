package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类：
 * - notes 表支持时间戳 create_time/update_time（毫秒）
 * - 新增回收站：is_deleted/delete_time（软删除）
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "notepad.db";

    /**
     * 注意：版本号只能递增不能降低。
     * 你前面时间戳方案我给的是 5，这里再加回收站字段 -> 6
     */
    private static final int DB_VERSION = 6;

    // 用户表
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USERNAME = "username";
    public static final String COL_PASSWORD = "password";

    // 记事表
    public static final String TABLE_NOTES = "notes";
    public static final String COL_NOTE_ID = "id";
    public static final String COL_NOTE_TITLE = "title";
    public static final String COL_NOTE_CONTENT = "content";
    public static final String COL_NOTE_USER_ID = "user_id";

    // 时间戳（毫秒）
    public static final String COL_NOTE_CREATE_TIME = "create_time";
    public static final String COL_NOTE_UPDATE_TIME = "update_time";

    // 回收站字段
    public static final String COL_NOTE_IS_DELETED = "is_deleted";   // 0未删除，1已删除
    public static final String COL_NOTE_DELETE_TIME = "delete_time"; // 删除时间毫秒（可为null）

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_USERNAME + " TEXT UNIQUE, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUsers);

        // notes：包含时间戳 + 回收站字段
        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_USER_ID + " INTEGER, " +
                COL_NOTE_CREATE_TIME + " INTEGER, " +
                COL_NOTE_UPDATE_TIME + " INTEGER, " +
                COL_NOTE_IS_DELETED + " INTEGER DEFAULT 0, " +
                COL_NOTE_DELETE_TIME + " INTEGER)";
        db.execSQL(createNotes);
    }

    /**
     * 数据库升级：
     * - 旧版本可能没有 create_time/update_time（v5引入）
     * - 本次 v6 引入 is_deleted/delete_time
     *
     * 采用 ALTER TABLE 增字段的方式，尽量不丢数据。
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        long now = System.currentTimeMillis();

        // 如果从更老版本升级到 5+，补时间戳字段
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_CREATE_TIME + " INTEGER");
            } catch (Exception ignore) { }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_UPDATE_TIME + " INTEGER");
            } catch (Exception ignore) { }

            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_CREATE_TIME + " = COALESCE(" + COL_NOTE_CREATE_TIME + ", " + now + ")");
            } catch (Exception ignore) { }
            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_UPDATE_TIME + " = COALESCE(" + COL_NOTE_UPDATE_TIME + ", " + now + ")");
            } catch (Exception ignore) { }
        }

        // 升级到 6：补回收站字段
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignore) { }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_DELETE_TIME + " INTEGER");
            } catch (Exception ignore) { }

            // 把旧数据默认设为未删除
            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_IS_DELETED + " = COALESCE(" + COL_NOTE_IS_DELETED + ", 0)");
            } catch (Exception ignore) { }
        }
    }

    // 注册
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 6) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        long result = db.insert(TABLE_USERS, null, values);
        db.close();
        return result != -1;
    }

    // 登录验证
    public boolean login(String username, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, null,
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password}, null, null, null);
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isValid;
    }

    // 获取用户ID
    public int getUserId(String username) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS, new String[]{COL_USER_ID},
                COL_USERNAME + "=?", new String[]{username}, null, null, null);
        int id = -1;
        if (cursor.moveToFirst()) id = cursor.getInt(0);
        cursor.close();
        db.close();
        return id;
    }

    /**
     * 查询未删除的记事（主列表）
     * 要求：AND is_deleted = 0，按 update_time 倒序
     */
    public Cursor getAllNotes(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME
        };
        return db.query(TABLE_NOTES, columns,
                COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_NOTE_UPDATE_TIME + " DESC");
    }

    /**
     * 查询回收站记事（只显示已删除的）
     * 按 delete_time 倒序
     */
    public Cursor getDeletedNotes(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_DELETE_TIME
        };
        return db.query(TABLE_NOTES, columns,
                COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=1",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_NOTE_DELETE_TIME + " DESC");
    }

    // 根据ID获取单条记事（编辑页用：也可以拿到是否删除）
    public Cursor getNoteById(int noteId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_IS_DELETED,
                COL_NOTE_DELETE_TIME
        };
        return db.query(TABLE_NOTES, columns,
                COL_NOTE_ID + "=?",
                new String[]{String.valueOf(noteId)},
                null, null, null);
    }

    // 添加记事：自动写 create_time 和 update_time，默认未删除
    public long addNote(int userId, String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_USER_ID, userId);
        values.put(COL_NOTE_CREATE_TIME, now);
        values.put(COL_NOTE_UPDATE_TIME, now);
        values.put(COL_NOTE_IS_DELETED, 0);
        values.putNull(COL_NOTE_DELETE_TIME);

        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // 更新记事：自动更新 update_time（仅对未删除的笔记更新更合理）
    public boolean updateNote(int noteId, String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_UPDATE_TIME, now);

        int rows = db.update(TABLE_NOTES, values,
                COL_NOTE_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0",
                new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }

    /**
     * 软删除：is_deleted=1，delete_time=now
     */
    public boolean softDeleteNote(int noteId) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_IS_DELETED, 1);
        values.put(COL_NOTE_DELETE_TIME, now);

        int rows = db.update(TABLE_NOTES, values,
                COL_NOTE_ID + "=?",
                new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }

    /**
     * 回收站恢复：is_deleted=0，delete_time=NULL
     */
    public boolean restoreNote(int noteId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_IS_DELETED, 0);
        values.putNull(COL_NOTE_DELETE_TIME);

        int rows = db.update(TABLE_NOTES, values,
                COL_NOTE_ID + "=?",
                new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }

    /**
     * 彻底删除：物理删除
     */
    public boolean deleteNoteForever(int noteId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_NOTES, COL_NOTE_ID + "=?", new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }
}