package com.example.billiardassist.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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
import android.util.DisplayMetrics;
import android.view.WindowManager;
import androidx.core.app.NotificationCompat;
import com.example.billiardassist.MainActivity;

/**
 * 录屏服务 - 负责屏幕采集，将数据传递给图像处理模块
 */
public class CaptureService extends Service {

    private static final String CHANNEL_ID = "capture_service_channel";
    private static final int NOTIFICATION_ID = 2001;

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private Handler handler = new Handler();
    private Runnable captureRunnable;
    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            if (data != null && resultCode != -1) {
                startCapture(resultCode, data);
            }
        }
        return START_STICKY;
    }

    private void startCapture(int resultCode, Intent data) {
        MediaProjectionManager mpm = getSystemService(MediaProjectionManager.class);
        mediaProjection = mpm.getMediaProjection(resultCode, data);

        WindowManager wm = getSystemService(WindowManager.class);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int dpi = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2);
        virtualDisplay = mediaProjection.createVirtualDisplay(
                "CaptureDisplay",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null
        );

        isRunning = true;
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                Image image = imageReader.acquireLatestImage();
                if (image != null) {
                    // 这里可以传递给 AimProcessor 进行识别
                    image.close();
                }
                handler.postDelayed(this, 100);
            }
        };
        handler.post(captureRunnable);
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "录屏服务", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("灵喵 录屏服务运行中")
                .setContentText("屏幕采集进行中...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        handler.removeCallbacks(captureRunnable);
        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
