package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
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

    // 时间格式化代码（yyyy-MM-dd HH:mm）
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        lvNotes = findViewById(R.id.lv_notes);
        Button btnAdd = findViewById(R.id.btn_add);

        loadNotes();

        btnAdd.setOnClickListener(v -> {
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
    }

    private void loadNotes() {
        Cursor cursor = dbHelper.getAllNotes(userId);

        // 用 update_time 绑定到 tv_time（然后通过 ViewBinder 格式化成“最后修改：...”）
        String[] from = {DatabaseHelper.COL_NOTE_TITLE, DatabaseHelper.COL_NOTE_UPDATE_TIME};
        int[] to = {R.id.tv_title, R.id.tv_time};

        adapter = new SimpleCursorAdapter(this, R.layout.item_note, cursor, from, to, 0);

        // 列表适配器中显示时间的代码：把毫秒转成 yyyy-MM-dd HH:mm
        adapter.setViewBinder((view, c, columnIndex) -> {
            int updateTimeIndex = c.getColumnIndex(DatabaseHelper.COL_NOTE_UPDATE_TIME);
            if (columnIndex == updateTimeIndex) {
                long millis = c.getLong(columnIndex);
                ((TextView) view).setText("最后修改：" + formatTime(millis));
                return true;
            }
            return false;
        });

        lvNotes.setAdapter(adapter);
    }

    private String formatTime(long millis) {
        return sdf.format(new Date(millis));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
}