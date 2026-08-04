package com.example.billiardassist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY = 100;
    private static final int REQUEST_CODE_SCREEN = 101;
    private MediaProjectionManager projectionManager;

    static {
        // 加载OpenCV本地库（需要提前把.so文件放到jniLibs）
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_OVERLAY);
        } else {
            requestScreenCapture();
        }

        Button btnStart = findViewById(R.id.btn_start);
        btnStart.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                requestScreenCapture();
            } else {
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestScreenCapture() {
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE_SCREEN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCREEN) {
            if (resultCode == Activity.RESULT_OK) {
                Intent serviceIntent = new Intent(this, OverlayService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
                Toast.makeText(this, "辅助服务已启动，正在识别瞄准环...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "截屏权限被拒绝", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
