package com.example.billiardassist.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.billiardassist.App;

public class CaptureService extends Service {

    private static final String TAG = "CaptureService";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private Handler handler = new Handler();
    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopCapture();
            stopSelf();
            return START_NOT_STICKY;
        }

        startCapture();
        return START_STICKY;
    }

    private void startCapture() {
        if (isRunning) return;

        try {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            
            int resultCode = App.getInstance().getMediaProjectionResultCode();
            Intent data = App.getInstance().getMediaProjectionData();
            
            if (data == null) {
                Log.e(TAG, "录屏数据为空");
                return;
            }

            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            
            // 创建虚拟显示
            int density = getResources().getDisplayMetrics().densityDpi;
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                1080,  // 宽度
                1920,  // 高度
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                null,
                null
            );
            
            isRunning = true;
            Log.d(TAG, "✅ 录屏服务启动成功");
            
            // 开始循环截屏
            startScreenCaptureLoop();
            
        } catch (Exception e) {
            Log.e(TAG, "启动录屏失败", e);
        }
    }

    private void startScreenCaptureLoop() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    // TODO: 在这里截取屏幕并处理
                    captureScreen();
                    handler.postDelayed(this, 50); // 20fps
                }
            }
        });
    }

    private void captureScreen() {
        // TODO: 实现屏幕截图逻辑
        // 使用 ImageReader 获取屏幕图像
    }

    private void stopCapture() {
        isRunning = false;
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.d(TAG, "录屏服务已停止");
    }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
