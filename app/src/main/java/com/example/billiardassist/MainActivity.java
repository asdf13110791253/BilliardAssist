package com.example.billiardassist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY = 100;
    private static final int REQUEST_CODE_SCREEN = 101;
    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ===== 核心修正：使用官方方式初始化 OpenCV =====
        // 不再需要 System.loadLibrary("opencv_java4");
        // Maven 依赖会自动加载 so 库，手动加载反而容易找不到
        initOpenCV();

        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        } else {
            initButton();
        }
    }

    private void initOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.e("BilliardAssist", "OpenCV 初始化失败！请检查 Gradle 依赖");
            Toast.makeText(this, "OpenCV 初始化失败", Toast.LENGTH_LONG).show();
        } else {
            Log.d("BilliardAssist", "OpenCV 初始化成功！🎱");
        }
    }

    private void initButton() {
        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
                return;
            }
            requestScreenCapture();
        });
    }

    private void requestScreenCapture() {
        projectionManager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 处理悬浮窗授权结果
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                initButton();
            } else {
                Toast.makeText(this, "必须授予悬浮窗权限才能使用辅助功能", Toast.LENGTH_LONG).show();
            }
            return;
        }

        // 处理录屏授权结果
        if (requestCode == REQUEST_CODE_SCREEN) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                Intent serviceIntent = new Intent(this, OverlayService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }

                Toast.makeText(this, "辅助服务已启动，正在识别瞄准环...", Toast.LENGTH_LONG).show();
                finish(); // 启动服务后可以关闭 Activity
            } else {
                Toast.makeText(this, "截屏权限被拒绝，无法识别画面", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
