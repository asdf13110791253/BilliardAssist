package com.example.billiardassist.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 全局崩溃捕获器 - APP闪退时自动复制日志到剪贴板
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;
    private Context context;
    private Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {}

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    /**
     * 初始化崩溃捕获
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
        LogHelper.log(TAG, "✅ 全局崩溃捕获已启动");
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // 生成崩溃日志
        String crashLog = buildCrashLog(thread, throwable);

        // 保存到日志
        LogHelper.logError(TAG, "========== APP崩溃 ==========", throwable);

        // 复制到剪贴板
        copyToClipboard(crashLog);

        // 显示Toast提示
        showToast("✅ 故障日志已复制到剪贴板，请粘贴发送给开发者");

        // 延迟让Toast显示
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 交给系统默认处理
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 生成崩溃日志
     */
    private String buildCrashLog(Thread thread, Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        sb.append("========== 🔴 APP崩溃日志 ==========\n");
        sb.append("崩溃时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("设备型号: ").append(Build.MODEL).append("\n");
        sb.append("Android版本: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("线程: ").append(thread.getName()).append("\n");
        sb.append("====================================\n\n");

        sb.append("📍 异常信息:\n");
        sb.append(throwable.toString()).append("\n\n");

        sb.append("📍 堆栈跟踪:\n");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        sb.append(sw.toString());

        sb.append("\n====================================\n");
        sb.append("📋 日志已自动复制到剪贴板\n");
        sb.append("请粘贴发送给开发者排查问题\n");
        sb.append("====================================\n");

        return sb.toString();
    }

    /**
     * 自动复制到剪贴板
     */
    private void copyToClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("CrashLog", text);
            clipboard.setPrimaryClip(clip);
            LogHelper.log(TAG, "✅ 崩溃日志已自动复制到剪贴板");
        } catch (Exception e) {
            Log.e(TAG, "复制到剪贴板失败", e);
        }
    }

    /**
     * 显示Toast提示
     */
    private void showToast(final String message) {
        try {
            Handler mainHandler = new Handler(Looper.getMainLooper());
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "显示Toast失败", e);
        }
    }
}
