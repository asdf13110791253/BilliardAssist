package com.example.billiardassist;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * 全局 Application 类
 * 负责初始化全局配置、加载保存的用户偏好
 */
public class App extends Application {

    public static final String PREFS_SETTINGS = "settings";
    public static final String PREFS_CALIBRATION = "calibration";
    public static final String PREFS_AIM = "aim_config";

    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initDefaultSettings();
    }

    public static App getInstance() {
        return instance;
    }

    /**
     * 初始化默认设置（首次安装时写入）
     */
    private void initDefaultSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (prefs.getAll().isEmpty()) {
            prefs.edit()
                    .putInt("line_color", Color.GREEN)
                    .putInt("comp_ratio", 18)      // 0.18
                    .putInt("bank_count", 2)
                    .putFloat("line_thickness", 5.0f)
                    .putBoolean("ant_line", false)
                    .putBoolean("adsorb", true)
                    .putInt("reflect_mode", 0)      // 0=补偿, 1=镜像
                    .putInt("table_cloth", 1)       // 0/1/2
                    .apply();
        }

        SharedPreferences aimPrefs = getSharedPreferences(PREFS_AIM, Context.MODE_PRIVATE);
        if (aimPrefs.getAll().isEmpty()) {
            aimPrefs.edit()
                    .putInt("scheme", 0)            // 当前方案
                    .putInt("v_value", 232)         // 亮度
                    .putInt("s_value", 15)          // 圆白度
                    .putInt("p_value", 15)          // 灵敏度
                    .apply();
        }
    }

    /**
     * 获取当前辅助线颜色
     */
    public int getLineColor() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).getInt("line_color", Color.GREEN);
    }

    /**
     * 获取当前辅助线粗细
     */
    public float getLineThickness() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).getFloat("line_thickness", 5.0f);
    }

    /**
     * 是否显示蚂蚁线
     */
    public boolean isAntLineEnabled() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).getBoolean("ant_line", false);
    }
}
