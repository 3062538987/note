package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvRegisterLink;
    private CheckBox chkRememberPassword;      // 记住密码复选框
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppPrefs.applySavedNightMode(this); // 先应用上次保存的夜间/日间模式
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvRegisterLink = findViewById(R.id.tv_register_link);
        chkRememberPassword = findViewById(R.id.chk_remember_password);

        // 恢复“记住密码”复选框的状态
        boolean isRememberChecked = AppPrefs.isRememberPasswordChecked(this);
        chkRememberPassword.setChecked(isRememberChecked);

        // 如果之前选择了“记住密码”，则自动填充保存的账号和密码
        if (isRememberChecked) {
            String savedUsername = AppPrefs.getSavedUsername(this);
            String savedPassword = AppPrefs.getSavedPassword(this);
            etUsername.setText(savedUsername);
            etPassword.setText(savedPassword);
        }

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
                Toast.makeText(LoginActivity.this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dbHelper.login(username, password)) {
                // ---------- 记住密码逻辑 ----------
                if (chkRememberPassword.isChecked()) {
                    // 保存账号密码及复选框状态
                    AppPrefs.saveCredentials(this, username, password);
                    AppPrefs.setRememberPasswordChecked(this, true);
                } else {
                    // 清除已保存的账号密码，并保存复选框状态
                    AppPrefs.clearCredentials(this);
                    AppPrefs.setRememberPasswordChecked(this, false);
                }
                // --------------------------------

                int userId = dbHelper.getUserId(username);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("user_id", userId);
                intent.putExtra("username", username);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "用户名或密码错误", Toast.LENGTH_SHORT).show();
            }
        });

        tvRegisterLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }
}