package com.example.billiardassist;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/**
 * 全局 Application 类
 * 负责初始化全局配置、加载保存的用户偏好、跨组件数据共享
 * ⚠️ 注意：静态实例生命周期与应用一致，请勿持有Activity/View引用，避免内存泄漏
 */
public class App extends Application {

    // ==================== 全局常量 ====================
    public static final String PREFS_SETTINGS = "settings";       // 通用设置
    public static final String PREFS_CALIBRATION = "calibration";  // 校准数据
    public static final String PREFS_AIM = "aim_config";           // 瞄准参数
    public static final String PREFS_TABLE = "table_bounds";      // 球桌边界

    private static App instance;
    private boolean isOpenCVInitSuccess = false;  // OpenCV初始化状态标记

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        initDefaultSettings();
    }

    public static App getInstance() {
        return instance;
    }

    // ==================== 初始化默认配置 ====================
    private void initDefaultSettings() {
        // 通用设置（辅助线、反射模式等）
        SharedPreferences settingsPrefs = getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        if (settingsPrefs.getAll().isEmpty()) {
            settingsPrefs.edit()
                    .putInt("line_color", Color.GREEN)       // 辅助线颜色：默认绿色
                    .putInt("comp_ratio", 18)               // 补偿比例：0.18（18/100）
                    .putInt("bank_count", 2)                // 默认翻袋次数：2次
                    .putFloat("line_thickness", 5.0f)       // 辅助线粗细：5px
                    .putBoolean("ant_line", false)          // 蚂蚁线：关闭
                    .putBoolean("adsorb", true)              // 瞄准吸附：开启
                    .putInt("reflect_mode", 0)              // 反射模式：0=补偿/1=镜像
                    .putInt("table_cloth", 1)               // 球桌布类型：0/1/2
                    .apply();
        }

        // 瞄准算法参数（V/S/P）
        SharedPreferences aimPrefs = getSharedPreferences(PREFS_AIM, Context.MODE_PRIVATE);
        if (aimPrefs.getAll().isEmpty()) {
            aimPrefs.edit()
                    .putInt("scheme", 0)                    // 当前方案：0=标准台球
                    .putInt("v_value", 232)                 // 亮度阈值：232
                    .putInt("s_value", 15)                  // 圆白度：15
                    .putInt("p_value", 15)                  // 检测灵敏度：15
                    .apply();
        }

        // 球桌边界（默认全屏，校准后可更新）
        SharedPreferences tablePrefs = getSharedPreferences(PREFS_TABLE, Context.MODE_PRIVATE);
        if (tablePrefs.getAll().isEmpty()) {
            tablePrefs.edit()
                    .putFloat("left", 0f)
                    .putFloat("top", 0f)
                    .putFloat("right", 1080f)  // 默认1080P宽度，校准后会覆盖
                    .putFloat("bottom", 2340f) // 默认1080P高度，校准后会覆盖
                    .apply();
        }
    }

    // ==================== OpenCV状态管理 ====================
    public void setOpenCVInitSuccess(boolean success) {
        this.isOpenCVInitSuccess = success;
    }

    public boolean isOpenCVInitSuccess() {
        return isOpenCVInitSuccess;
    }

    // ==================== 通用设置 Getter/Setter ====================
    /** 获取辅助线颜色 */
    public int getLineColor() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("line_color", Color.GREEN);
    }

    /** 设置辅助线颜色 */
    public void setLineColor(int color) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("line_color", color)
                .apply();
    }

    /** 获取补偿比例（0.0~1.0） */
    public double getCompRatio() {
        int ratio = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("comp_ratio", 18);
        return ratio / 100.0;  // 18 → 0.18，适配AimProcessor计算逻辑
    }

    /** 设置补偿比例（传入0~100的整数，如18代表0.18） */
    public void setCompRatio(int ratio) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("comp_ratio", Math.max(0, Math.min(100, ratio)))
                .apply();
    }

    /** 获取翻袋次数 */
    public int getBankCount() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("bank_count", 2);
    }

    /** 设置翻袋次数 */
    public void setBankCount(int count) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("bank_count", Math.max(0, Math.min(5, count))) // 最多5次翻袋
                .apply();
    }

    /** 获取辅助线粗细 */
    public float getLineThickness() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getFloat("line_thickness", 5.0f);
    }

    /** 设置辅助线粗细 */
    public void setLineThickness(float thickness) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putFloat("line_thickness", Math.max(1.0f, Math.min(20.0f, thickness)))
                .apply();
    }

    /** 是否开启蚂蚁线 */
    public boolean isAntLineEnabled() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getBoolean("ant_line", false);
    }

    /** 设置蚂蚁线开关 */
    public void setAntLineEnabled(boolean enabled) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("ant_line", enabled)
                .apply();
    }

    /** 是否开启瞄准吸附 */
    public boolean isAdsorbEnabled() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getBoolean("adsorb", true);
    }

    /** 设置吸附开关 */
    public void setAdsorbEnabled(boolean enabled) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putBoolean("adsorb", enabled)
                .apply();
    }

    /** 获取反射模式（0=补偿/1=镜像） */
    public int getReflectMode() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("reflect_mode", 0);
    }

    /** 设置反射模式 */
    public void setReflectMode(int mode) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("reflect_mode", mode == 0 ? 0 : 1)
                .apply();
    }

    /** 获取球桌布类型 */
    public int getTableClothType() {
        return getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .getInt("table_cloth", 1);
    }

    /** 设置球桌布类型 */
    public void setTableClothType(int type) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit()
                .putInt("table_cloth", Math.max(0, Math.min(2, type)))
                .apply();
    }

    // ==================== 瞄准参数 Getter/Setter ====================
    /** 获取当前瞄准方案 */
    public int getCurrentAimScheme() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("scheme", 0);
    }

    /** 设置当前瞄准方案 */
    public void setCurrentAimScheme(int scheme) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("scheme", Math.max(0, Math.min(7, scheme)))
                .apply();
    }

    /** 获取亮度阈值V */
    public int getAimV() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("v_value", 232);
    }

    /** 设置亮度阈值V */
    public void setAimV(int v) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("v_value", Math.max(0, Math.min(255, v)))
                .apply();
    }

    /** 获取圆白度S */
    public int getAimS() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("s_value", 15);
    }

    /** 设置圆白度S */
    public void setAimS(int s) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("s_value", Math.max(0, Math.min(100, s)))
                .apply();
    }

    /** 获取检测灵敏度P */
    public int getAimP() {
        return getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .getInt("p_value", 15);
    }

    /** 设置检测灵敏度P */
    public void setAimP(int p) {
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE)
                .edit()
                .putInt("p_value", Math.max(0, Math.min(100, p)))
                .apply();
    }

    // ==================== 球桌边界管理 ====================
    /** 保存球桌边界（校准后调用） */
    public void saveTableBounds(float left, float top, float right, float bottom) {
        getSharedPreferences(PREFS_TABLE, MODE_PRIVATE)
                .edit()
                .putFloat("left", left)
                .putFloat("top", top)
                .putFloat("right", right)
                .putFloat("bottom", bottom)
                .apply();
    }

    /** 获取球桌边界（返回[left, top, right, bottom]） */
    public float[] getTableBounds() {
        SharedPreferences prefs = getSharedPreferences(PREFS_TABLE, MODE_PRIVATE);
        return new float[]{
                prefs.getFloat("left", 0f),
                prefs.getFloat("top", 0f),
                prefs.getFloat("right", 1080f),
                prefs.getFloat("bottom", 2340f)
        };
    }

    // ==================== 工具方法 ====================
    /** 重置所有配置为默认值 */
    public void resetAllSettings() {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PREFS_AIM, MODE_PRIVATE).edit().clear().apply();
        getSharedPreferences(PREFS_TABLE, MODE_PRIVATE).edit().clear().apply();
        initDefaultSettings(); // 重新写入默认值
    }
}
