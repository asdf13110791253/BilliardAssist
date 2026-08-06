package com.example.billiardassist.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

import com.example.billiardassist.App;
import com.example.billiardassist.R;

/**
 * 悬浮窗服务 - 负责在屏幕上绘制辅助线
 */
public class FloatingService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private WindowManager windowManager;
    private View floatingView;
    private ImageView overlayView;
    private WindowManager.LayoutParams params;
    private Handler mainHandler;
    private Paint paintGreen, paintRed, paintYellow;
    private int screenWidth, screenHeight;

    private int lineColor = Color.GREEN;
    private float lineThickness = 5.0f;
    private boolean showAntLine = false;
    private boolean isDrawing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        loadSettings();
        initPaints();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopDrawing();
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(NOTIFICATION_ID, buildNotification());
        initOverlayWindow();
        startDrawingLoop();

        return START_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("台球辅助运行中")
                .setContentText("辅助服务正在后台绘制瞄准线")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void loadSettings() {
        lineColor = App.getInstance().getLineColor();
        lineThickness = App.getInstance().getLineThickness();
        showAntLine = App.getInstance().isAntLineEnabled();
    }

    private void initPaints() {
        paintGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGreen.setColor(lineColor);
        paintGreen.setStrokeWidth(lineThickness);
        paintGreen.setStyle(Paint.Style.STROKE);

        paintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(lineThickness + 1);
        paintRed.setStyle(Paint.Style.STROKE);

        paintYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3);
        paintYellow.setStyle(Paint.Style.STROKE);
    }

    private void initOverlayWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                screenWidth = windowManager.getCurrentWindowMetrics().getBounds().width();
                screenHeight = windowManager.getCurrentWindowMetrics().getBounds().height();
            } catch (Exception e) {
                windowManager.getDefaultDisplay().getMetrics(metrics);
                screenWidth = metrics.widthPixels;
                screenHeight = metrics.heightPixels;
            }
        } else {
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null);
        overlayView = floatingView.findViewById(R.id.overlay_canvas);
        if (overlayView == null) {
            throw new IllegalStateException("布局文件中必须包含id为overlay_canvas的ImageView");
        }

        int windowType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                windowType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        try {
            if (floatingView.getParent() == null) {
                windowManager.addView(floatingView, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startDrawingLoop() {
        if (isDrawing) return;
        isDrawing = true;

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isDrawing) return;
                drawGuideLines(screenWidth / 2, screenHeight / 2);
                mainHandler.postDelayed(this, 16);
            }
        });
    }

    private void stopDrawing() {
        isDrawing = false;
        mainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 绘制辅助线 - 完整版
     */
    public void drawGuideLines(int cx, int cy) {
        if (overlayView == null || cx < 0 || cy < 0 || screenWidth <= 0 || screenHeight <= 0) return;

        // 回收旧Bitmap
        try {
            if (overlayView.getDrawable() instanceof BitmapDrawable) {
                Bitmap oldBitmap = ((BitmapDrawable) overlayView.getDrawable()).getBitmap();
                if (oldBitmap != null && !oldBitmap.isRecycled()) {
                    overlayView.setImageDrawable(null);
                    oldBitmap.recycle();
                }
            }
        } catch (Exception ignored) {
        }

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            return;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // ✅ 绘制十字准星
        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);

        // ✅ 绘制瞄准圈
        paintRed.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(cx, cy, 60, paintRed);

        // ✅ 绘制中心点
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);

        // ✅ 绘制角度刻度
        for (int i = 0; i < 360; i += 30) {
            double rad = Math.toRadians(i);
            float startX = (float) (cx + Math.cos(rad) * 70);
            float startY = (float) (cy + Math.sin(rad) * 70);
            float endX = (float) (cx + Math.cos(rad) * 85);
            float endY = (float) (cy + Math.sin(rad) * 85);
            canvas.drawLine(startX, startY, endX, endY, paintYellow);
        }

        // ✅ 绘制蚂蚁线（如果开启）
        if (showAntLine) {
            paintYellow.setStyle(Paint.Style.STROKE);
            paintYellow.setStrokeWidth(2);
            float radius = 60;
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                float dotX = (float) (cx + Math.cos(rad) * radius);
                float dotY = (float) (cy + Math.sin(rad) * radius);
                canvas.drawCircle(dotX, dotY, 3, paintYellow);
            }
        }

        overlayView.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        stopDrawing();
        try {
            if (floatingView != null && floatingView.getParent() != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
