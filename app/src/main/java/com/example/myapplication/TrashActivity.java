package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 回收站页面：
 * - 只显示 is_deleted=1 的笔记
 * - item 显示：标题 + 删除时间
 * - 点击 item：弹出操作（恢复 / 彻底删除）
 */
public class TrashActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private int userId;

    private ListView lvTrash;
    private SimpleCursorAdapter adapter;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPrefs.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            Toast.makeText(this, "用户ID获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar_trash);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("回收站");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        lvTrash = findViewById(R.id.lv_trash);

        loadTrash();

        // 点击 item -> 选择“恢复/彻底删除”
        lvTrash.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int noteId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            showActionsDialog(noteId);
        });
    }

    private void loadTrash() {
        Cursor cursor = dbHelper.getDeletedNotes(userId);

        String[] from = {DatabaseHelper.COL_NOTE_TITLE, DatabaseHelper.COL_NOTE_DELETE_TIME};
        int[] to = {R.id.tv_trash_title, R.id.tv_trash_time};

        adapter = new SimpleCursorAdapter(this, R.layout.item_trash_note, cursor, from, to, 0);
        adapter.setViewBinder((view, c, columnIndex) -> {
            int deleteTimeIndex = c.getColumnIndex(DatabaseHelper.COL_NOTE_DELETE_TIME);
            if (columnIndex == deleteTimeIndex) {
                long millis = 0L;
                try {
                    millis = c.getLong(columnIndex);
                } catch (Exception ignore) { }

                String text = "删除时间：" + formatTime(millis);
                ((TextView) view).setText(text);
                return true;
            }
            return false;
        });

        lvTrash.setAdapter(adapter);
    }

    private void showActionsDialog(int noteId) {
        String[] items = {"恢复", "彻底删除"};
        new AlertDialog.Builder(this)
                .setTitle("回收站操作")
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        // 恢复
                        boolean ok = dbHelper.restoreNote(noteId);
                        Toast.makeText(this, ok ? "已恢复" : "恢复失败", Toast.LENGTH_SHORT).show();
                        if (ok) loadTrash();
                    } else {
                        // 彻底删除（再确认一次）
                        confirmDeleteForever(noteId);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDeleteForever(int noteId) {
        new AlertDialog.Builder(this)
                .setTitle("彻底删除")
                .setMessage("彻底删除后无法恢复，确定继续吗？")
                .setPositiveButton("删除", (d, w) -> {
                    boolean ok = dbHelper.deleteNoteForever(noteId);
                    Toast.makeText(this, ok ? "已彻底删除" : "删除失败", Toast.LENGTH_SHORT).show();
                    if (ok) loadTrash();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private String formatTime(long millis) {
        return sdf.format(new Date(millis));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTrash();
    }
}