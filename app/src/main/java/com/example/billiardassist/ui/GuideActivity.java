package com.example.billiardassist.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billiardassist.App;
import com.example.billiardassist.R;
import com.example.billiardassist.service.CapturePermissionActivity;
import org.opencv.android.OpenCVLoader;

public class GuideActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 1001;
    private static final int MODE_VERTICAL = 0;
    private static final int MODE_HORIZONTAL = 1;
    private static final String TAG = "GuideActivity";

    private TextView tvStatus;
    private Button btnHorizontal, btnVertical;
    private Button btnBattery, btnAppSettings;
    private Button btnSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isOpenCVReady = OpenCVLoader.initDebug();
        App.getInstance().setOpenCVInitSuccess(isOpenCVReady);
        if (!isOpenCVReady) {
            Log.e(TAG, "OpenCV初始化失败");
            Toast.makeText(this, "OpenCV初始化失败", Toast.LENGTH_LONG).show();
        }

        setContentView(R.layout.activity_guide);
        initViews();
        setupListeners();
        checkOverlayPermission();

        if (!isOpenCVReady) {
            enableModeButtons(false);
            Toast.makeText(this, "请检查依赖后重启APP", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOverlayPermission();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        btnHorizontal = findViewById(R.id.btn_horizontal);
        btnVertical = findViewById(R.id.btn_vertical);
        btnBattery = findViewById(R.id.btn_battery);
        btnAppSettings = findViewById(R.id.btn_app_settings);
        btnSettings = findViewById(R.id.btn_settings);
    }

    private void setupListeners() {
        btnHorizontal.setOnClickListener(v -> {
            if (!App.getInstance().isOpenCVInitSuccess()) {
                Toast.makeText(this, "OpenCV未就绪", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasOverlayPermission()) {
                launchService(MODE_HORIZONTAL);
            } else {
                requestOverlayPermission();
            }
        });

        btnVertical.setOnClickListener(v -> {
            if (!App.getInstance().isOpenCVInitSuccess()) {
                Toast.makeText(this, "OpenCV未就绪", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasOverlayPermission()) {
                launchService(MODE_VERTICAL);
            } else {
                requestOverlayPermission();
            }
        });

        btnBattery.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show();
            }
        });

        btnAppSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开应用设置", Toast.LENGTH_SHORT).show();
            }
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void checkOverlayPermission() {
        if (hasOverlayPermission()) {
            tvStatus.setText(R.string.status_active);
            tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            enableModeButtons(true);
        } else {
            tvStatus.setText(R.string.status_inactive);
            tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
            enableModeButtons(false);
        }
    }

    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    private void requestOverlayPermission() {
        Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_OVERLAY);
    }

    private void enableModeButtons(boolean enabled) {
        btnHorizontal.setEnabled(enabled);
        btnVertical.setEnabled(enabled);
        float alpha = enabled ? 1.0f : 0.5f;
        btnHorizontal.setAlpha(alpha);
        btnVertical.setAlpha(alpha);
    }

    private void launchService(int mode) {
        Intent intent = new Intent(this, CapturePermissionActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
        String message = (mode == MODE_HORIZONTAL) ? "正在启动横版模式..." : "正在启动竖版模式...";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            checkOverlayPermission();
            Toast.makeText(this, hasOverlayPermission() ? "悬浮窗权限已授予" : "悬浮窗权限被拒绝", Toast.LENGTH_SHORT).show();
        }
    }
}
