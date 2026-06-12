package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * SharedPreferences 保存/读取 夜间模式设置 + 记住账号密码
 */
public class AppPrefs {
    private static final String SP_NAME = "app_prefs";
    private static final String KEY_NIGHT = "night_mode";

    // 新增：记住账号密码相关的键
    private static final String KEY_SAVED_USERNAME = "saved_username";
    private static final String KEY_SAVED_PASSWORD = "saved_password";
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";

    // ========== 夜间模式相关方法（原内容） ==========
    /** 读取是否夜间模式（默认 false = 日间） */
    public static boolean isNightMode(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_NIGHT, false);
    }

    /** 保存夜间模式开关 */
    public static void setNightMode(Context context, boolean night) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_NIGHT, night).apply();
    }

    /**
     * 应用启动时套用已保存的模式：
     * 必须在 Activity 的 super.onCreate(savedInstanceState) 之前调用
     */
    public static void applySavedNightMode(Context context) {
        boolean night = isNightMode(context);
        AppCompatDelegate.setDefaultNightMode(
                night ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // ========== 新增：记住账号密码相关方法 ==========
    /** 保存用户名和密码（勾选记住密码时调用） */
    public static void saveCredentials(Context context, String username, String password) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .putString(KEY_SAVED_USERNAME, username)
                .putString(KEY_SAVED_PASSWORD, password)
                .apply();
    }

    /** 清除保存的用户名和密码（取消勾选时调用） */
    public static void clearCredentials(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit()
                .remove(KEY_SAVED_USERNAME)
                .remove(KEY_SAVED_PASSWORD)
                .apply();
    }

    /** 获取保存的用户名，若无则返回空字符串 */
    public static String getSavedUsername(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SAVED_USERNAME, "");
    }

    /** 获取保存的密码，若无则返回空字符串 */
    public static String getSavedPassword(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getString(KEY_SAVED_PASSWORD, "");
    }

    /** 设置“记住密码”复选框的勾选状态 */
    public static void setRememberPasswordChecked(Context context, boolean isChecked) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_REMEMBER_PASSWORD, isChecked).apply();
    }

    /** 获取“记住密码”复选框的勾选状态，默认 false */
    public static boolean isRememberPasswordChecked(Context context) {
        SharedPreferences sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_REMEMBER_PASSWORD, false);
    }
}