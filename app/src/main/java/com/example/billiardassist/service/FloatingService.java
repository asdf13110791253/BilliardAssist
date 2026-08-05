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

    // 配置
    private int lineColor = Color.GREEN;
    private float lineThickness = 5.0f;
    private boolean showAntLine = false;

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
                // 回退到旧方法
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

        // addView 可能抛 IllegalStateException（重复添加）或其他异常，捕获处理
        try {
            // 如果已经添加则跳过
            if (floatingView.getParent() == null) {
                windowManager.addView(floatingView, params);
            }
        } catch (Exception e) {
            // 记录/忽略，避免崩溃
            e.printStackTrace();
        }
    }

    private void startDrawingLoop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                drawGuideLines(screenWidth / 2, screenHeight / 2);
                mainHandler.postDelayed(this, 16);
            }
        });
    }

    public void drawGuideLines(int cx, int cy) {
        if (overlayView == null || cx < 0 || cy < 0 || screenWidth <= 0 || screenHeight <= 0) return;

        // 回收旧Bitmap（小心并发/正在显示的情况）
        try {
            if (overlayView.getDrawable() instanceof BitmapDrawable) {
                Bitmap oldBitmap = ((BitmapDrawable) overlayView.getDrawable()).getBitmap();
                if (oldBitmap != null && !oldBitmap.isRecycled()) {
                    // 先把 ImageView 清空引用，保证不会正在使用同一 bitmap
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
            // OOM 时跳过本次绘制
            oom.printStackTrace();
            return;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);
        canvas.drawCircle(cx, cy, 60, paintRed);

        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);
        paintRed.setStyle(Paint.Style.STROKE);

        if (showAntLine) {
            drawDashedLine(canvas, cx - 300, cy - 300, cx + 300, cy + 300, paintYellow);
        }

        canvas.drawLine(cx - 200, cy - 200, cx + 200, cy + 200, paintYellow);
        canvas.drawLine(cx + 200, cy - 200, cx - 200, cy + 200, paintYellow);

        // 确保在主线程/视图线程更新 ImageView
        if (overlayView != null) {
            overlayView.post(() -> {
                try {
                    overlayView.setImageBitmap(bitmap);
                } catch (Exception e) {
                    // 如果设置失败，回收 bitmap
                    e.printStackTrace();
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            });
        } else {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private void drawDashedLine(Canvas canvas, float x1, float y1, float x2, float y2, Paint paint) {
        float dashLen = 12f;
        float gapLen = 8f;
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        float ux = dx / len;
        float uy = dy / len;
        float drawn = 0;

        while (drawn < len) {
            float segEnd = Math.min(drawn + dashLen, len);
            canvas.drawLine(
                    x1 + ux * drawn, y1 + uy * drawn,
                    x1 + ux * segEnd, y1 + uy * segEnd,
                    paint
            );
            drawn = segEnd + gapLen;
        }
    }

    public void updateLineStyle(int color, float thickness) {
        this.lineColor = color;
        this.lineThickness = thickness;
        if (paintGreen != null) {
            paintGreen.setColor(color);
            paintGreen.setStrokeWidth(thickness);
        }
        if (paintRed != null) {
            paintRed.setStrokeWidth(thickness + 1);
        }
        App.getInstance().setLineColor(color);
        App.getInstance().setLineThickness(thickness);
    }

    public void setAntLineVisible(boolean visible) {
        this.showAntLine = visible;
        App.getInstance().setAntLineEnabled(visible);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mainHandler.removeCallbacksAndMessages(null);
        try {
            if (floatingView != null && floatingView.getParent() != null && windowManager != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (overlayView != null && overlayView.getDrawable() instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) overlayView.getDrawable()).getBitmap();
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }
        } catch (Exception ignored) {}

        try {
            stopForeground(true);
        } catch (Exception ignored) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
