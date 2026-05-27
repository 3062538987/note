package com.example.myapplication;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class EditNoteActivity extends AppCompatActivity {
    private EditText etTitle, etContent;
    private DatabaseHelper dbHelper;
    private String mode;
    private int noteId = -1;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        mode = getIntent().getStringExtra("mode");
        userId = getIntent().getIntExtra("user_id", -1);

        if ("edit".equals(mode)) {
            noteId = getIntent().getIntExtra("note_id", -1);
            getSupportActionBar().setTitle("编辑笔记");
            loadNote();
        } else {
            getSupportActionBar().setTitle("新建笔记");
        }
    }

    private void loadNote() {
        Cursor cursor = dbHelper.getNoteById(noteId);
        if (cursor != null && cursor.moveToFirst()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_TITLE));
            String content = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_NOTE_CONTENT));
            etTitle.setText(title);
            etContent.setText(content);
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
                confirmDelete();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveNote() {
        String title = etTitle.getText().toString().trim();
        String content = etContent.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean success;
        if ("add".equals(mode)) {
            long id = dbHelper.addNote(userId, title, content);
            success = id != -1;
        } else {
            success = dbHelper.updateNote(noteId, title, content);
        }
        Toast.makeText(this, success ? "保存成功" : "保存失败", Toast.LENGTH_SHORT).show();
        if (success) finish();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("删除笔记")
                .setMessage("确定要删除这条笔记吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (dbHelper.deleteNote(noteId)) {
                        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show();
                    }
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