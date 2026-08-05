package com.example.billiardassist.service;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

/**
 * 录屏授权中转 Activity（透明）
 */
public class CapturePermissionActivity extends Activity {

    public static final int REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 透明无UI，纯中转
        MediaProjectionManager mpm = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 兼容方式获取 service
                Object svc = getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (svc instanceof MediaProjectionManager) {
                    mpm = (MediaProjectionManager) svc;
                } else {
                    // 运行在较低平台或异常，抛出后直接 finish
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
            Intent serviceIntent = new Intent(this, CaptureService.class);
            if (resultCode == RESULT_OK && data != null) {
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                // 根据版本选择启动前台服务还是普通服务，兼容 Android O+
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } catch (Exception e) {
                    // 回退
                    startService(serviceIntent);
                }
            } else {
                // 用户拒绝，通知服务停止
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
