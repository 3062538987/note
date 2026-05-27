package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * SharedPreferences 保存/读取 夜间模式设置
 */
public class AppPrefs {
    private static final String SP_NAME = "app_prefs";
    private static final String KEY_NIGHT = "night_mode";

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
}