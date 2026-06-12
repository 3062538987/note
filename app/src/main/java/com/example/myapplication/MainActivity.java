package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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

    // 时间格式：yyyy-MM-dd HH:mm
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

        // Toolbar 作为 ActionBar
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
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES
                            : AppCompatDelegate.MODE_NIGHT_NO
            );

            // 重新创建 Activity 让 UI 立刻刷新
            recreate();
        });

        lvNotes = findViewById(R.id.lv_notes);

        // 新建笔记
        findViewById(R.id.btn_add).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
            intent.putExtra("user_id", userId);
            intent.putExtra("mode", "add");
            startActivity(intent);
        });

        // 点击某条笔记进入编辑
        lvNotes.setOnItemClickListener((parent, view, position, id) -> {
            Cursor cursor = (Cursor) parent.getItemAtPosition(position);
            int noteId = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
            intent.putExtra("note_id", noteId);
            intent.putExtra("user_id", userId);
            intent.putExtra("mode", "edit");
            startActivity(intent);
        });

        // ===================== 新增：搜索功能 =====================
        SearchView searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                performSearch(newText);
                return true;
            }
        });
        // ========================================================

        // 初始化适配器（先给一个空 cursor，loadNotes 中会填充）
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

        loadNotes();
    }

    /**
     * 加载右上角菜单（回收站 / 退出登录）
     * 需要 res/menu/menu_main.xml
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * 菜单点击事件
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_trash) {
            // 打开回收站
            Intent intent = new Intent(MainActivity.this, TrashActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_exit) {
            // 退出登录：回到 LoginActivity，并清空返回栈（按返回不会回到 MainActivity）
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 加载所有笔记（未删除的）
     */
    private void loadNotes() {
        Cursor cursor = dbHelper.getAllNotes(userId);
        adapter.changeCursor(cursor);
    }

    /**
     * 根据关键词搜索笔记
     */
    private void performSearch(String keyword) {
        if (TextUtils.isEmpty(keyword)) {
            // 关键词为空，显示所有笔记
            loadNotes();
        } else {
            // 搜索匹配的笔记
            Cursor cursor = dbHelper.searchNotes(keyword, userId);
            adapter.changeCursor(cursor);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
    }
}