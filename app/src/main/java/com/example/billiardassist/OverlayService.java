package com.example.billiardassist;

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
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private ImageView overlayView;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private Handler handler = new Handler();
    private Runnable captureRunnable;
    private AimDetector aimDetector;
    private Bitmap templateBitmap;
    private int screenWidth, screenHeight;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            if (data != null) {
                MediaProjectionManager projectionManager =
                        (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                startOverlay();
                initDetector();
                startScreenCapture();
            }
        }
        return START_STICKY;
    }

    private void startOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
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
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlayView, params);
    }

    private void initDetector() {
        // 从 drawable 加载瞄准环模板
        int resId = getResources().getIdentifier("aim_template", "drawable", getPackageName());
        templateBitmap = ImageUtils.decodeResource(getResources(), resId);
        if (templateBitmap != null) {
            aimDetector = new AimDetector(templateBitmap);
        } else {
            android.util.Log.e("OverlayService",
                    "模板图片不存在，请放置 aim_template.png 到 drawable 目录");
        }
    }

    private void startScreenCapture() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int dpi = metrics.densityDpi;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, null);

        captureRunnable = new Runnable() {
            @Override
            public void run() {
                Image image = imageReader.acquireLatestImage();
                if (image != null && aimDetector != null) {
                    Bitmap screenBitmap = ImageUtils.imageToBitmap(image);
                    if (screenBitmap != null) {
                        Point center = aimDetector.detectAimCenter(screenBitmap);
                        if (center != null) {
                            updateOverlay(center);
                        }
                        screenBitmap.recycle();
                    }
                    image.close();
                }
                handler.postDelayed(this, 100); // 每100ms检测一次
            }
        };
        handler.post(captureRunnable);
    }

    // 在检测到的瞄准环位置绘制辅助线（绿色十字 + 红色圆圈）
    private void updateOverlay(Point center) {
        if (overlayView == null) return;

        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Paint paintGreen = new Paint();
        paintGreen.setColor(Color.GREEN);
        paintGreen.setStrokeWidth(4);
        paintGreen.setAntiAlias(true);

        Paint paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(5);
        paintRed.setAntiAlias(true);

        int cx = (int) center.x;
        int cy = (int) center.y;

        // 1. 绿色水平延长线（贯穿全屏）
        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);

        // 2. 绿色垂直延长线（贯穿全屏）
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);

        // 3. 红色瞄准圆圈
        paintRed.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, 60, paintRed);

        // 4. 中心红点
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);

        // 5. 黄色斜线（模拟球杆方向）
        Paint paintYellow = new Paint();
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3);
        paintYellow.setAntiAlias(true);
        canvas.drawLine(cx - 200, cy - 200, cx + 200, cy + 200, paintYellow);
        canvas.drawLine(cx + 200, cy - 200, cx - 200, cy + 200, paintYellow);

        overlayView.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (overlayView != null) windowManager.removeView(overlayView);
        if (aimDetector != null) aimDetector.release();
        if (templateBitmap != null) templateBitmap.recycle();
        handler.removeCallbacks(captureRunnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
