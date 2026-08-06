package com.example.billiardassist;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import com.example.billiardassist.utils.CrashHandler;

public class App extends Application {

    public static final String PREFS_SETTINGS = "settings";
    public static final String PREFS_CALIBRATION = "calibration";
    public static final String PREFS_AIM = "aim_config";
    public static final String PREFS_TABLE = "table_bounds";
    public static final String NOTIFICATION_CHANNEL_ID = "billiard_assist_channel";

    private static App instance;
    private boolean isOpenCVInitSuccess = false;
    private int mediaProjectionResultCode;
    private Intent mediaProjectionData;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // ✅ 初始化崩溃捕获
        CrashHandler.getInstance().init(this);

        initDefaultSettings();
        createNotificationChannel();
    }

    public static App getInstance() {
        return instance;
    }

    // ==================== 录屏数据管理 ====================
    public void setMediaProjectionData(int resultCode, Intent data) {
        this.mediaProjectionResultCode = resultCode;
        this.mediaProjectionData = data;
    }

    public int getMediaProjectionResultCode() {
        return mediaProjectionResultCode;
    }

    public Intent getMediaProjectionData() {
        return mediaProjectionData;
    }

    private void initDefaultSettings() {
        SharedPreferences settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (settingsPrefs.getAll().isEmpty()) {
            settingsPrefs.edit()
                    .putInt("line_color", Color.GREEN)
                    .putInt("comp_ratio", 18)
                    .putInt("bank_count", 2)
                    .putFloat("line_thickness", 5.0f)
                    .putBoolean("ant_line", false)
                    .putBoolean("adsorb", true)
                    .putInt("reflect_mode", 0)
                    .putInt("table_cloth", 1)
                    .apply();
        }

        SharedPreferences aimPrefs = getSharedPreferences(PREFS_AIM, Context.MODE_PRIVATE);
        if (aimPrefs.getAll().isEmpty()) {
            aimPrefs.edit()
                    .putInt("scheme", 0)
                    .putInt("v_value", 232)
                    .putInt("s_value", 15)
                    .putInt("p_value", 15)
                    .apply();
        }

        SharedPreferences tablePrefs = getSharedPreferences(PREFS_TABLE, Context.MODE_PRIVATE);
        if (tablePrefs.getAll().isEmpty()) {
            tablePrefs.edit()
                    .putFloat("left", 0f)
                    .putFloat("top", 0f)
                    .putFloat("right", 1080f)
                    .putFloat("bottom", 2340f)
                    .apply();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "台球辅助服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于保持辅助服务后台运行");
            channel.enableLights(false);
            channel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public void setOpenCVInitSuccess(boolean success) {
        this.isOpenCVInitSuccess = success;
    }

    public boolean isOpenCVInitSuccess() {
        return isOpenCVInitSuccess;
    }

    public int getLineColor() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("line_color", Color.GREEN);
    }

    public void setLineColor(int color) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("line_color", color)
                .apply();
    }

    public double getCompRatio() {
        int ratio = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("comp_ratio", 18);
        return ratio / 100.0;
    }

    public void setCompRatio(int ratio) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("comp_ratio", Math.max(0, Math.min(100, ratio)))
                .apply();
    }

    public int getBankCount() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("bank_count", 2);
    }

    public void setBankCount(int count) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("bank_count", Math.max(0, Math.min(5, count)))
                .apply();
    }

    public float getLineThickness() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getFloat("line_thickness", 5.0f);
    }

    public void setLineThickness(float thickness) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putFloat("line_thickness", Math.max(1.0f, Math.min(20.0f, thickness)))
                .apply();
    }

    public boolean isAntLineEnabled() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getBoolean("ant_line", false);
    }

    public void setAntLineEnabled(boolean enabled) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("ant_line", enabled)
                .apply();
    }

    public boolean isAdsorbEnabled() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getBoolean("adsorb", true);
    }

    public void setAdsorbEnabled(boolean enabled) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("adsorb", enabled)
                .apply();
    }

    public int getReflectMode() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("reflect_mode", 0);
    }

    public void setReflectMode(int mode) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("reflect_mode", mode == 0 ? 0 : 1)
                .apply();
    }

    public int getTableClothType() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("table_cloth", 1);
    }

    public void setTableClothType(int type) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("table_cloth", Math.max(0, Math.min(2, type)))
                .apply();
    }

    public int getCurrentAimScheme() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("scheme", 0);
    }

    public void setCurrentAimScheme(int scheme) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("scheme", Math.max(0, Math.min(7, scheme)))
                .apply();
    }

    public int getAimV() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("v_value", 232);
    }

    public void setAimV(int v) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("v_value", Math.max(0, Math.min(255, v)))
                .apply();
    }

    public int getAimS() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("s_value", 15);
    }

    public void setAimS(int s) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("s_value", Math.max(0, Math.min(100, s)))
                .apply();
    }

    public int getAimP() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("p_value", 15);
    }

    public void setAimP(int p) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("p_value", Math.max(0, Math.min(100, p)))
                .apply();
    }

    public void saveTableBounds(float left, float top, float right, float bottom) {
        getSharedPreferences(PREFS_TABLE, MODE_PRIVATE)
                .edit()
                .putFloat("left", left)
                .putFloat("top", top)
                .putFloat("right", right)
                .putFloat("bottom", bottom)
                .apply();
    }
}
