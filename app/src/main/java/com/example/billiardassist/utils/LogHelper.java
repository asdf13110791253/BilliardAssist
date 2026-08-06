package com.example.billiardassist.utils;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志助手 - 收集和记录日志
 */
public class LogHelper {

    private static final String TAG = "LogHelper";
    private static StringBuilder logBuffer = new StringBuilder();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    /**
     * 记录普通日志
     */
    public static void log(String tag, String message) {
        String timestamp = sdf.format(new Date());
        String logLine = timestamp + " [" + tag + "] " + message + "\n";
        logBuffer.append(logLine);
        Log.d(tag, message);
    }

    /**
     * 记录错误日志（带异常）
     */
    public static void logError(String tag, String message, Throwable e) {
        String timestamp = sdf.format(new Date());
        String logLine = timestamp + " [ERROR-" + tag + "] " + message + "\n";
        if (e != null) {
            logLine += "  " + e.toString() + "\n";
            for (StackTraceElement element : e.getStackTrace()) {
                logLine += "    at " + element.toString() + "\n";
            }
        }
        logBuffer.append(logLine);
        Log.e(tag, message, e);
    }

    /**
     * 记录错误日志（不带异常）
     */
    public static void logError(String tag, String message) {
        logError(tag, message, null);
    }

    /**
     * 获取完整日志
     */
    public static String getFullLog() {
        return logBuffer.toString();
    }

    /**
     * 清空日志
     */
    public static void clearLog() {
        logBuffer = new StringBuilder();
        log(TAG, "日志已清空");
    }

    /**
     * 获取日志大小
     */
    public static int getLogSize() {
        return logBuffer.length();
    }
}
