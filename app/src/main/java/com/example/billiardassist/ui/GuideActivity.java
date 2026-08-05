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
    private static final int MODE_VERTICAL = 0;      // 竖版模式
    private static final int MODE_HORIZONTAL = 1;    // 横版模式
    private static final String TAG = "GuideActivity";

    private TextView tvStatus;
    private Button btnHorizontal, btnVertical;
    private Button btnBattery, btnAppSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // ===== 核心新增：初始化OpenCV并记录全局状态 =====
        boolean isOpenCVReady = OpenCVLoader.initDebug();
        App.getInstance().setOpenCVInitSuccess(isOpenCVReady);
        if (!isOpenCVReady) {
            Log.e(TAG, "OpenCV初始化失败！请检查Gradle依赖是否正确");
            Toast.makeText(this, "OpenCV初始化失败，辅助功能将无法使用", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "OpenCV初始化成功，算法模块加载完成🎱");
        }
        // ================================================

        setContentView(R.layout.activity_guide);

        initViews();
        setupListeners();
        checkOverlayPermission();
        
        // ===== 新增：OpenCV未就绪时禁用功能按钮，避免崩溃 =====
        if (!isOpenCVReady) {
            enableModeButtons(false);
            Toast.makeText(this, "请检查依赖后重启APP", Toast.LENGTH_SHORT).show();
        }
        // ==================================================
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从系统设置返回后刷新状态
        checkOverlayPermission();
    }

    /**
     * 初始化视图控件
     */
    private void initViews() {
        tvStatus = findViewById(R.id.tv_status);
        btnHorizontal = findViewById(R.id.btn_horizontal);
        btnVertical = findViewById(R.id.btn_vertical);
        btnBattery = findViewById(R.id.btn_battery);
        btnAppSettings = findViewById(R.id.btn_app_settings);
    }

    /**
     * 设置点击事件监听
     */
    private void setupListeners() {
        // 横版进
        btnHorizontal.setOnClickListener(v -> {
            // 新增：启动前校验OpenCV状态
            if (!App.getInstance().isOpenCVInitSuccess()) {
                Toast.makeText(GuideActivity.this, "OpenCV未就绪，请重启APP", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasOverlayPermission()) {
                launchService(MODE_HORIZONTAL);
            } else {
                requestOverlayPermission();
            }
        });

        // 竖版进
        btnVertical.setOnClickListener(v -> {
            // 新增：启动前校验OpenCV状态
            if (!App.getInstance().isOpenCVInitSuccess()) {
                Toast.makeText(GuideActivity.this, "OpenCV未就绪，请重启APP", Toast.LENGTH_SHORT).show();
                return;
            }
            if (hasOverlayPermission()) {
                launchService(MODE_VERTICAL);
            } else {
                requestOverlayPermission();
            }
        });

        // 电池优化入口
        btnBattery.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show();
            }
        });

        // 应用设置入口
        btnAppSettings.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开应用设置", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 检查并更新悬浮窗权限状态UI
     */
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

    /**
     * 检查是否有悬浮窗权限
     */
    private boolean hasOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }

    /**
     * 请求悬浮窗权限
     */
    private void requestOverlayPermission() {
        Toast.makeText(this, R.string.toast_request_overlay, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        );
        startActivityForResult(intent, REQUEST_OVERLAY);
    }

    /**
     * 启用/禁用模式选择按钮
     */
    private void enableModeButtons(boolean enabled) {
        btnHorizontal.setEnabled(enabled);
        btnVertical.setEnabled(enabled);
        
        // 视觉反馈
        float alpha = enabled ? 1.0f : 0.5f;
        btnHorizontal.setAlpha(alpha);
        btnVertical.setAlpha(alpha);
    }

    /**
     * 启动录屏授权 → 悬浮服务
     */
    private void launchService(int mode) {
        Intent intent = new Intent(this, CapturePermissionActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
        
        String message = (mode == MODE_HORIZONTAL) 
                ? getString(R.string.toast_starting_horizontal)
                : getString(R.string.toast_starting_vertical);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY) {
            // 用户返回后重新检查权限状态
            checkOverlayPermission();
            
            if (hasOverlayPermission()) {
                Toast.makeText(this, R.string.toast_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }
}
