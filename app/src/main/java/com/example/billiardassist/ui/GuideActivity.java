package com.example.billiardassist.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billiardassist.R;
import com.example.billiardassist.service.CapturePermissionActivity;

/**
 * 引导页 / 主界面
 * 对应截图：图1、图2、图4、图5、图15
 *
 * 功能：
 * - 显示"已激活"状态
 * - 横版进 / 竖版进 按钮
 * - 使用说明
 * - 系统设置建议（电池优化 / 应用设置入口）
 */
public class GuideActivity extends AppCompatActivity {

    private static final int REQUEST_OVERLAY = 1001;

    private TextView tvStatus;
    private Button btnHorizontal, btnVertical;
    private Button btnBattery, btnAppSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        // 绑定控件
        tvStatus = findViewById(R.id.tv_status);
        btnHorizontal = findViewById(R.id.btn_horizontal);
        btnVertical = findViewById(R.id.btn_vertical);
        btnBattery = findViewById(R.id.btn_battery);
        btnAppSettings = findViewById(R.id.btn_app_settings);

        // 检查悬浮窗权限状态
        checkOverlayPermission();

        // 横版进
        btnHorizontal.setOnClickListener(v -> {
            if (checkAndRequestOverlay()) {
                launchService(1); // 1 = 横版
            }
        });

        // 竖版进
        btnVertical.setOnClickListener(v -> {
            if (checkAndRequestOverlay()) {
                launchService(0); // 0 = 竖版
            }
        });

        // 电池优化入口
        btnBattery.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            startActivity(intent);
        });

        // 应用设置入口
        btnAppSettings.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkOverlayPermission();
    }

    /**
     * 检查悬浮窗权限并更新UI
     */
    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                tvStatus.setText("已激活");
                tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            } else {
                tvStatus.setText("未激活");
                tvStatus.setBackgroundResource(R.drawable.bg_status_inactive);
            }
        } else {
            tvStatus.setText("已激活");
            tvStatus.setBackgroundResource(R.drawable.bg_status_active);
        }
    }

    /**
     * 检查并请求悬浮窗权限
     * @return true = 已有权限
     */
    private boolean checkAndRequestOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
            return false;
        }
        return true;
    }

    /**
     * 启动录屏授权 → 悬浮服务
     */
    private void launchService(int mode) {
        Intent intent = new Intent(this, CapturePermissionActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
        Toast.makeText(this, mode == 1 ? "横版模式启动中..." : "竖版模式启动中...", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            checkOverlayPermission();
        }
    }
}
