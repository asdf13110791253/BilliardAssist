package com.example.billiardassist.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import com.example.billiardassist.App;

/**
 * 录屏授权中转 Activity（透明）
 * 修复：启动 FloatingService 而不是 CaptureService
 */
public class CapturePermissionActivity extends Activity {

    public static final int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MediaProjectionManager mpm = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Object svc = getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (svc instanceof MediaProjectionManager) {
                    mpm = (MediaProjectionManager) svc;
                } else {
                    finish();
                    return;
                }
            } else {
                finish();
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            finish();
            return;
        }

        Intent intent = mpm.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            // ✅ 修复：启动 FloatingService
            Intent serviceIntent = new Intent(this, FloatingService.class);

            if (resultCode == RESULT_OK && data != null) {
                // 保存录屏数据到App全局
                App.getInstance().setMediaProjectionData(resultCode, data);

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // 用户拒绝
                serviceIntent.setAction("STOP");
                try {
                    startService(serviceIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            finish();
        }
    }
}
