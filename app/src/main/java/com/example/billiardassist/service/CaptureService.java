package com.example.billiardassist.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.billiardassist.App;
import com.example.billiardassist.ai.AimAssistManager;

import java.nio.ByteBuffer;

public class CaptureService extends Service {

    private static final String TAG = "CaptureService";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler handler = new Handler();
    private boolean isRunning = false;

    // 屏幕尺寸
    private int screenWidth = 1080;
    private int screenHeight = 1920;

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
            
            // 获取屏幕尺寸
            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;
            
            int density = getResources().getDisplayMetrics().densityDpi;

            // ✅ 修复：创建ImageReader用于截图
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, 
                android.graphics.ImageFormat.RGB_565, 2);

            // ✅ 修复：添加最后一个null参数
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null  // ← 添加这个参数，解决编译错误
            );
            
            isRunning = true;
            Log.d(TAG, "✅ 录屏服务启动成功，尺寸: " + screenWidth + "x" + screenHeight);
            
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
                    captureScreen();
                    handler.postDelayed(this, 50); // 20fps
                }
            }
        });
    }

    private void captureScreen() {
        try {
            if (imageReader == null) return;
            
            // 获取最新的一帧图像
            Image image = imageReader.acquireLatestImage();
            if (image == null) return;

            try {
                // 将Image转换为Bitmap
                android.graphics.Bitmap bitmap = imageToBitmap(image);
                if (bitmap != null) {
                    // 发送给AI分析
                    AimAssistManager.getInstance().analyzeFrame(bitmap);
                    // 回收Bitmap
                    bitmap.recycle();
                }
            } finally {
                image.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "截屏失败", e);
        }
    }

    /**
     * 将Image转换为Bitmap
     */
    private android.graphics.Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            if (planes.length == 0) return null;

            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            // 创建Bitmap
            android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                android.graphics.Bitmap.Config.ARGB_8888
            );

            bitmap.copyPixelsFromBuffer(buffer);
            
            // 裁剪掉填充部分
            if (rowPadding > 0) {
                return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
            }
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "转换Image失败", e);
            return null;
        }
    }

    private void stopCapture() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
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
