package com.example.billiardassist.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
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

            screenWidth = getResources().getDisplayMetrics().widthPixels;
            screenHeight = getResources().getDisplayMetrics().heightPixels;

            int density = getResources().getDisplayMetrics().densityDpi;

            imageReader = ImageReader.newInstance(screenWidth, screenHeight, ImageFormat.RGB_565, 2);

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
            );

            isRunning = true;
            Log.d(TAG, "✅ 录屏服务启动成功，尺寸: " + screenWidth + "x" + screenHeight);

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
                    handler.postDelayed(this, 50);
                }
            }
        });
    }

    private void captureScreen() {
        try {
            if (imageReader == null) return;

            Image image = imageReader.acquireLatestImage();
            if (image == null) return;

            try {
                Bitmap bitmap = imageToBitmap(image);
                if (bitmap != null) {
                    AimAssistManager.getInstance().analyzeFrame(bitmap);
                    bitmap.recycle();
                }
            } finally {
                image.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "截屏失败", e);
        }
    }

    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            if (planes.length == 0) return null;

            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;

            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight,
                Bitmap.Config.ARGB_8888
            );

            bitmap.copyPixelsFromBuffer(buffer);

            if (rowPadding > 0) {
                return Bitmap.createBitmap(bitmap, 0, 0, screenWidth, screenHeight);
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
