package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "notepad.db";

    // 必须递增；你之前设备里已有 version=4，所以这里用 5
    private static final int DB_VERSION = 5;

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

    // 时间戳字段：INTEGER，存 System.currentTimeMillis()
    public static final String COL_NOTE_CREATE_TIME = "create_time";
    public static final String COL_NOTE_UPDATE_TIME = "update_time";

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

        // notes 表：新增 create_time、update_time（INTEGER毫秒）
        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_USER_ID + " INTEGER, " +
                COL_NOTE_CREATE_TIME + " INTEGER, " +
                COL_NOTE_UPDATE_TIME + " INTEGER)";
        db.execSQL(createNotes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 升级到 v5：给 notes 表加两列（如果已存在则忽略异常）
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_CREATE_TIME + " INTEGER");
            } catch (Exception ignore) { }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_UPDATE_TIME + " INTEGER");
            } catch (Exception ignore) { }

            // 给旧数据补默认值（为空就填当前时间）
            long now = System.currentTimeMillis();
            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_CREATE_TIME + " = COALESCE(" + COL_NOTE_CREATE_TIME + ", " + now + ")");
            } catch (Exception ignore) { }
            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_UPDATE_TIME + " = COALESCE(" + COL_NOTE_UPDATE_TIME + ", " + now + ")");
            } catch (Exception ignore) { }
        }
    }

    // 注册
    public boolean register(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.length() < 6)
            return false;
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

    // 获取某用户的所有记事（按最后修改时间倒序）
    public Cursor getAllNotes(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME
        };
        return db.query(TABLE_NOTES, columns, COL_NOTE_USER_ID + "=?",
                new String[]{String.valueOf(userId)}, null, null,
                COL_NOTE_UPDATE_TIME + " DESC");
    }

    // 根据ID获取单条记事
    public Cursor getNoteById(int noteId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME
        };
        return db.query(TABLE_NOTES, columns, COL_NOTE_ID + "=?",
                new String[]{String.valueOf(noteId)}, null, null, null);
    }

    // 添加记事：自动记录 create_time + update_time
    public long addNote(int userId, String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_USER_ID, userId);
        values.put(COL_NOTE_CREATE_TIME, now);
        values.put(COL_NOTE_UPDATE_TIME, now);

        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    // 更新记事：自动更新 update_time
    public boolean updateNote(int noteId, String title, String content) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_UPDATE_TIME, now);

        int rows = db.update(TABLE_NOTES, values, COL_NOTE_ID + "=?", new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }

    // 删除记事
    public boolean deleteNote(int noteId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_NOTES, COL_NOTE_ID + "=?", new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }
}