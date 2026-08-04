package com.example.billiardassist.service;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

/**
 * 录屏授权中转 Activity
 *
 * 为什么需要这个 Activity？
 * - MediaProjection.createScreenCaptureIntent() 必须在一个 Activity 中启动
 * - 但我们的 CaptureService 是后台服务，没有 Activity
 * - 所以用一个"透明无UI"的 Activity 来中转：
 *   1. 收到 Service 的 Intent → 启动系统录屏授权弹窗
 *   2. 用户同意 → 把 resultCode + data 传回 Service
 *   3. 用户拒绝 → 通知 Service 停止
 */
public class CapturePermissionActivity extends Activity {

    public static final int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 透明无UI，纯中转
        MediaProjectionManager mpm = getSystemService(MediaProjectionManager.class);
        Intent intent = mpm.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            Intent serviceIntent = new Intent(this, CaptureService.class);
            if (resultCode == RESULT_OK && data != null) {
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startForegroundService(serviceIntent);
            } else {
                // 用户拒绝，通知服务停止
                serviceIntent.setAction("STOP");
                startService(serviceIntent);
            }
            finish();
        }
    }
}
