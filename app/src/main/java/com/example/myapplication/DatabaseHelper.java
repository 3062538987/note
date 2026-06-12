package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类：
 * - notes 表支持时间戳 create_time/update_time（毫秒）
 * - 支持回收站：is_deleted/delete_time（软删除）
 * - 支持搜索功能（关键词搜索）
 * - 支持笔记分类（category）
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "notepad.db";

    // 版本号：7（包含分类字段）
    private static final int DB_VERSION = 7;

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

    // 分类字段
    public static final String COL_NOTE_CATEGORY = "category";

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

        String createNotes = "CREATE TABLE " + TABLE_NOTES + " (" +
                COL_NOTE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NOTE_TITLE + " TEXT, " +
                COL_NOTE_CONTENT + " TEXT, " +
                COL_NOTE_USER_ID + " INTEGER, " +
                COL_NOTE_CREATE_TIME + " INTEGER, " +
                COL_NOTE_UPDATE_TIME + " INTEGER, " +
                COL_NOTE_IS_DELETED + " INTEGER DEFAULT 0, " +
                COL_NOTE_DELETE_TIME + " INTEGER, " +
                COL_NOTE_CATEGORY + " TEXT DEFAULT '其他')";
        db.execSQL(createNotes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        long now = System.currentTimeMillis();
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
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignore) { }
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_DELETE_TIME + " INTEGER");
            } catch (Exception ignore) { }
            try {
                db.execSQL("UPDATE " + TABLE_NOTES +
                        " SET " + COL_NOTE_IS_DELETED + " = COALESCE(" + COL_NOTE_IS_DELETED + ", 0)");
            } catch (Exception ignore) { }
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN " + COL_NOTE_CATEGORY + " TEXT DEFAULT '其他'");
            } catch (Exception ignore) { }
        }
    }

    // 注册、登录等方法保持不变
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

    public Cursor getAllNotes(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_CATEGORY
        };
        return db.query(TABLE_NOTES, columns,
                COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_NOTE_UPDATE_TIME + " DESC");
    }

    public Cursor getNotesByCategory(int userId, String category) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_CATEGORY
        };
        String selection = COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0 AND " + COL_NOTE_CATEGORY + "=?";
        String[] selectionArgs = new String[]{String.valueOf(userId), category};
        return db.query(TABLE_NOTES, columns, selection, selectionArgs,
                null, null, COL_NOTE_UPDATE_TIME + " DESC");
    }

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

    public Cursor getNoteById(int noteId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_IS_DELETED,
                COL_NOTE_DELETE_TIME,
                COL_NOTE_CATEGORY
        };
        return db.query(TABLE_NOTES, columns,
                COL_NOTE_ID + "=?",
                new String[]{String.valueOf(noteId)},
                null, null, null);
    }

    public long addNote(int userId, String title, String content, String category) {
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
        if (category == null || category.trim().isEmpty()) {
            category = "其他";
        }
        values.put(COL_NOTE_CATEGORY, category);
        long id = db.insert(TABLE_NOTES, null, values);
        db.close();
        return id;
    }

    public boolean updateNote(int noteId, String title, String content, String category) {
        SQLiteDatabase db = getWritableDatabase();
        long now = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put(COL_NOTE_TITLE, title);
        values.put(COL_NOTE_CONTENT, content);
        values.put(COL_NOTE_UPDATE_TIME, now);
        if (category != null && !category.trim().isEmpty()) {
            values.put(COL_NOTE_CATEGORY, category);
        }
        int rows = db.update(TABLE_NOTES, values,
                COL_NOTE_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0",
                new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }

    // 原有的两参数搜索方法（关键词 + userId）
    public Cursor searchNotes(String keyword, int userId) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_CATEGORY
        };
        String selection = COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0 AND (" +
                COL_NOTE_TITLE + " LIKE ? OR " + COL_NOTE_CONTENT + " LIKE ?)";
        String[] selectionArgs = new String[]{
                String.valueOf(userId),
                "%" + keyword + "%",
                "%" + keyword + "%"
        };
        return db.query(TABLE_NOTES, columns, selection, selectionArgs,
                null, null, COL_NOTE_UPDATE_TIME + " DESC");
    }

    // 新增的三参数搜索方法（userId + 关键词 + 分类）
    public Cursor searchNotes(int userId, String keyword, String category) {
        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {
                COL_NOTE_ID + " as _id",
                COL_NOTE_TITLE,
                COL_NOTE_CONTENT,
                COL_NOTE_CREATE_TIME,
                COL_NOTE_UPDATE_TIME,
                COL_NOTE_CATEGORY
        };
        String selection = COL_NOTE_USER_ID + "=? AND " + COL_NOTE_IS_DELETED + "=0 AND " +
                COL_NOTE_CATEGORY + "=? AND (" +
                COL_NOTE_TITLE + " LIKE ? OR " + COL_NOTE_CONTENT + " LIKE ?)";
        String[] selectionArgs = new String[]{
                String.valueOf(userId),
                category,
                "%" + keyword + "%",
                "%" + keyword + "%"
        };
        return db.query(TABLE_NOTES, columns, selection, selectionArgs,
                null, null, COL_NOTE_UPDATE_TIME + " DESC");
    }

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

    public boolean deleteNoteForever(int noteId) {
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_NOTES, COL_NOTE_ID + "=?", new String[]{String.valueOf(noteId)});
        db.close();
        return rows > 0;
    }
}
