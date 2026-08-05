package com.example.billiardassist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

public class OverlayService extends Service {

    private static final int NOTIFICATION_ID = 3001;
    private static final String CHANNEL_ID = App.NOTIFICATION_CHANNEL_ID;

    private WindowManager windowManager;
    private ImageView overlayView;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable captureRunnable;
    private AimDetector aimDetector;
    private Bitmap templateBitmap;
    private int screenWidth, screenHeight;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 启动前台通知，防止系统在 Android O+ 抛出异常
        startForeground(NOTIFICATION_ID, buildNotification());

        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            if (data != null && resultCode != -1) {
                Object svc = getSystemService(MEDIA_PROJECTION_SERVICE);
                if (svc instanceof MediaProjectionManager) {
                    MediaProjectionManager projectionManager = (MediaProjectionManager) svc;
                    mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                }
                // 只有在 mediaProjection 创建成功或后续降级场景才继续
                startOverlay();
                initDetector();
                startScreenCapture();
            }
        }
        return START_STICKY;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "台球辅助服务", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("用于保持辅助服务后台运行");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
        Intent intent = new Intent(this, MainActivity.class);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, flags);
        return new androidx.core.app.NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("台球辅助 - 界面监测")
                .setContentText("辅助服务运行中")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void startOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;
        DisplayMetrics metrics = new DisplayMetrics();
        try {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        overlayView = new ImageView(this);
        overlayView.setBackgroundColor(Color.TRANSPARENT);

        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        try {
            if (overlayView.getParent() == null) {
                windowManager.addView(overlayView, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 如果添加失败，置空 overlayView 以避免后续 NullPointer 异常
            overlayView = null;
        }
    }

    private void initDetector() {
        int resId = getResources().getIdentifier("aim_template", "drawable", getPackageName());
        templateBitmap = ImageUtils.decodeResource(getResources(), resId);
        if (templateBitmap != null) {
            aimDetector = new AimDetector(templateBitmap);
        } else {
            android.util.Log.e("OverlayService",
                    "模板图片不存在，请放置 aim_template 到 drawable 目录或检查资源名");
        }
    }

    private void startScreenCapture() {
        if (mediaProjection == null) return;
        if (windowManager == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        try {
            windowManager.getDefaultDisplay().getMetrics(metrics);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int dpi = metrics.densityDpi;

        try {
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                    width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(), null, null);
        } catch (Exception e) {
            e.printStackTrace();
            cleanupCapture();
            return;
        }

        captureRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (imageReader == null) return;
                    Image image = imageReader.acquireLatestImage();
                    if (image != null) {
                        try {
                            if (aimDetector != null) {
                                Bitmap screenBitmap = ImageUtils.imageToBitmap(image);
                                if (screenBitmap != null) {
                                    Point center = aimDetector.detectAimCenter(screenBitmap);
                                    if (center != null) {
                                        updateOverlay(center);
                                    }
                                    screenBitmap.recycle();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            try { image.close(); } catch (Exception ignore) {}
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(captureRunnable);
    }

    // 在检测到的瞄准环位置绘制辅助线（绿色十字 + 红色圆圈）
    private void updateOverlay(Point center) {
        if (overlayView == null || screenWidth <= 0 || screenHeight <= 0) return;

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            return;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paintGreen = new Paint();
        paintGreen.setColor(Color.GREEN);
        paintGreen.setStrokeWidth(4);
        paintGreen.setAntiAlias(true);
        paintGreen.setStyle(Paint.Style.STROKE);

        Paint paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(5);
        paintRed.setAntiAlias(true);

        int cx = (int) center.x;
        int cy = (int) center.y;

        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);

        paintRed.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, 60, paintRed);
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);

        Paint paintYellow = new Paint();
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3);
        paintYellow.setAntiAlias(true);
        canvas.drawLine(cx - 200, cy - 200, cx + 200, cy + 200, paintYellow);
        canvas.drawLine(cx + 200, cy - 200, cx - 200, cy + 200, paintYellow);

        // 在主线程安全地更新 overlayView
        overlayView.post(() -> {
            try {
                // 回收旧 bitmap 引用（如果有）
                if (overlayView.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                    Bitmap old = ((android.graphics.drawable.BitmapDrawable) overlayView.getDrawable()).getBitmap();
                    overlayView.setImageDrawable(null);
                    if (old != null && !old.isRecycled()) {
                        old.recycle();
                    }
                }
            } catch (Exception ignored) {}
            try {
                overlayView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        cleanupCapture();

        try {
            if (overlayView != null && windowManager != null && overlayView.getParent() != null) {
                windowManager.removeView(overlayView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (aimDetector != null) {
            try { aimDetector.release(); } catch (Exception ignored) {}
            aimDetector = null;
        }
        if (templateBitmap != null && !templateBitmap.isRecycled()) {
            templateBitmap.recycle();
            templateBitmap = null;
        }
        try {
            stopForeground(true);
        } catch (Exception ignored) {}
    }

    private void cleanupCapture() {
        try {
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
        } catch (Exception ignored) {}
        try {
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
        } catch (Exception ignored) {}
        try {
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
