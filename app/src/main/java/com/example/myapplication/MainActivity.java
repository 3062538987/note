package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;   // 关键修改：导入正确的 SearchView
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

    private Spinner spinnerCategory;
    private SearchView searchView;            // 使用 androidx 版本的 SearchView
    private String currentCategory = "全部";
    private String currentKeyword = "";

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        SwitchCompat switchNight = findViewById(R.id.switch_night);
        switchNight.setChecked(AppPrefs.isNightMode(this));
        switchNight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppPrefs.setNightMode(this, isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
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

        spinnerCategory = findViewById(R.id.spinner_category_filter);
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategory = parent.getItemAtPosition(position).toString();
                refreshNotes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentKeyword = query;
                refreshNotes();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentKeyword = newText;
                refreshNotes();
                return true;
            }
        });

        adapter = new SimpleCursorAdapter(this, R.layout.item_note, null,
                new String[]{DatabaseHelper.COL_NOTE_TITLE, DatabaseHelper.COL_NOTE_UPDATE_TIME},
                new int[]{R.id.tv_title, R.id.tv_time}, 0);
        adapter.setViewBinder((view, cursor, columnIndex) -> {
            int updateTimeIndex = cursor.getColumnIndex(DatabaseHelper.COL_NOTE_UPDATE_TIME);
            if (columnIndex == updateTimeIndex) {
                long millis = cursor.getLong(columnIndex);
                ((TextView) view).setText("最后修改：" + sdf.format(new Date(millis)));
                return true;
            }
            return false;
        });
        lvNotes.setAdapter(adapter);

        refreshNotes();
    }

    private void refreshNotes() {
        Cursor cursor;
        boolean hasKeyword = !TextUtils.isEmpty(currentKeyword);
        boolean isAllCategory = "全部".equals(currentCategory);

        if (hasKeyword && !isAllCategory) {
            cursor = dbHelper.searchNotes(userId, currentKeyword, currentCategory);
        } else if (hasKeyword) {
            cursor = dbHelper.searchNotes(currentKeyword, userId);
        } else if (!isAllCategory) {
            cursor = dbHelper.getNotesByCategory(userId, currentCategory);
        } else {
            cursor = dbHelper.getAllNotes(userId);
        }
        adapter.changeCursor(cursor);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_trash) {
            Intent intent = new Intent(MainActivity.this, TrashActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_exit) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotes();
    }
}