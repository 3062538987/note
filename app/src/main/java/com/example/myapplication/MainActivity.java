package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView lvNotes;
    private SimpleCursorAdapter adapter;
    private int userId;
    private String username;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 关键：在 super.onCreate 之前应用保存的夜间模式
        AppPrefs.applySavedNightMode(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userId = getIntent().getIntExtra("user_id", -1);
        username = getIntent().getStringExtra("username");

        if (userId == -1) {
            Toast.makeText(this, "用户ID获取失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(username + "的笔记");
        }

        // 夜间模式开关
        SwitchCompat switchNight = findViewById(R.id.switch_night);
        switchNight.setChecked(AppPrefs.isNightMode(this));
        switchNight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // 保存偏好
            AppPrefs.setNightMode(this, isChecked);

            // 立即切换模式
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    isChecked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
                            : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );

            // 重新创建 Activity 让 UI 生效
            recreate();
        });

        lvNotes = findViewById(R.id.lv_notes);

        findViewById(R.id.btn_add).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("mode", "add");
            startActivity(intent);
        });

        lvNotes.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int noteId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
            intent.putExtra("note_id", noteId);
            intent.putExtra("user_id", userId);
            intent.putExtra("mode", "edit");
            startActivity(intent);
        });

        loadNotes();
    }

    private void loadNotes() {
        Cursor cursor = dbHelper.getAllNotes(userId);

        // 这里假设你用 update_time 显示“最后修改：...”
        String[] from = {DatabaseHelper.COL_NOTE_TITLE, DatabaseHelper.COL_NOTE_UPDATE_TIME};
        int[] to = {R.id.tv_title, R.id.tv_time};
        adapter = new SimpleCursorAdapter(this, R.layout.item_note, cursor, from, to, 0);

        adapter.setViewBinder((view, c, columnIndex) -> {
            int updateTimeIndex = c.getColumnIndex(DatabaseHelper.COL_NOTE_UPDATE_TIME);
            if (columnIndex == updateTimeIndex) {
                long millis = c.getLong(columnIndex);
                ((TextView) view).setText("最后修改：" + sdf.format(new Date(millis)));
                return true;
            }
            return false;
        });

        lvNotes.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
}