package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

/**
 * 编辑/新建笔记页面
 * - 支持笔记分类（工作、个人、学习等）
 * - 保存：新增/更新（包含分类）
 * - 删除：软删除进入回收站
 */
public class EditNoteActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private Spinner spinnerCategory;
    private DatabaseHelper dbHelper;
    private String mode;
    private int noteId = -1;
    private int userId;
    private ArrayAdapter<CharSequence> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPrefs.applySavedNightMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        Toolbar toolbar = findViewById(R.id.toolbar_edit);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new DatabaseHelper(this);
        etTitle = findViewById(R.id.et_title);
        etContent = findViewById(R.id.et_content);
        spinnerCategory = findViewById(R.id.spinner_category);

        // 设置分类下拉框的适配器（从 arrays.xml 读取）
        categoryAdapter = ArrayAdapter.createFromResource(this,
                R.array.note_categories, android.R.layout.simple_spinner_item);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        mode = getIntent().getStringExtra("mode");
        userId = getIntent().getIntExtra("user_id", -1);

        if ("edit".equals(mode)) {
            noteId = getIntent().getIntExtra("note_id", -1);
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("编辑笔记");
            loadNote();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle("新建笔记");
        }
    }

    private void loadNote() {
        Cursor cursor = dbHelper.getNoteById(noteId);
        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CATEGORY));
            etTitle.setText(title);
            etContent.setText(content);
            // 设置下拉框选中项
            int position = categoryAdapter.getPosition(category);
            if (position >= 0) {
                spinnerCategory.setSelection(position);
            }
            cursor.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_save) {
            saveNote();
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            if ("edit".equals(mode)) {
                confirmSoftDelete();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString(); // 获取选中的分类

        if (title.isEmpty()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success;
        if ("add".equals(mode)) {
            long id = dbHelper.addNote(userId, title, content, category); // 传入分类
            success = id != -1;
        } else {
            success = dbHelper.updateNote(noteId, title, content, category); // 传入分类
        }

        Toast.makeText(this, success ? "保存成功" : "保存失败", Toast.LENGTH_SHORT).show();
        if (success) finish();
    }

    /**
     * 删除确认弹窗（软删除）
     */
    private void confirmSoftDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这条笔记吗？（将移入回收站）")
                .setPositiveButton("删除", (dialog, which) -> {
                    boolean ok = dbHelper.softDeleteNote(noteId);
                    Toast.makeText(this, ok ? "已移入回收站" : "删除失败", Toast.LENGTH_SHORT).show();
                    if (ok) finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}