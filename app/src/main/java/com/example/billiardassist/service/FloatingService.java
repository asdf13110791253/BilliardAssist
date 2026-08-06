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
import com.example.billiardassist.ai.AimAssistManager;
import com.example.billiardassist.utils.DrawUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

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

    // ===== 温度控制 =====
    private int frameCount = 0;
    private boolean isCharging = false;
    private float currentTemp = 0f;

    // 温度阈值：超过35°C降30帧
    private static final float TEMP_HIGH = 35.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        loadSettings();
        initPaints();
        checkChargingStatus();
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

    private void checkChargingStatus() {
        try {
            android.os.BatteryManager batteryManager = 
                (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
            if (batteryManager != null) {
                isCharging = batteryManager.isCharging();
            }
        } catch (Exception e) {
            isCharging = false;
        }
    }

    // ===== 读取手机温度 =====
    private float getBatteryTemperature() {
        try {
            android.os.BatteryManager batteryManager = 
                (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
            if (batteryManager != null) {
                int temp = batteryManager.getIntProperty(
                    android.os.BatteryManager.BATTERY_PROPERTY_TEMPERATURE
                );
                return temp / 10.0f;
            }
        } catch (Exception e) {
            try {
                BufferedReader reader = new BufferedReader(
                    new FileReader("/sys/class/thermal/thermal_zone0/temp")
                );
                String line = reader.readLine();
                reader.close();
                if (line != null) {
                    float temp = Float.parseFloat(line) / 1000.0f;
                    return temp;
                }
            } catch (Exception ignored) {}
        }
        return 35.0f;
    }

    // ===== 智能计算刷新率 =====
    private int calculateSmartDelay() {
        currentTemp = getBatteryTemperature();

        // 1. 充电时60帧
        if (isCharging) {
            return 16;
        }

        // 2. 温度超过35°C → 30帧（降频降温）
        if (currentTemp >= TEMP_HIGH) {
            return 33;
        }

        // 3. 前30帧60帧（快速响应）
        if (frameCount < 30) {
            return 16;
        }

        // 4. 温度正常 → 60帧
        return 16;
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
        frameCount = 0;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                drawGuideLines(screenWidth / 2, screenHeight / 2);
                frameCount++;

                int delay = calculateSmartDelay();
                mainHandler.postDelayed(this, delay);
            }
        });
    }

    /**
     * 绘制辅助线 - 完整版（含AI辅助）
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
        } catch (Exception ignored) {}

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
            return;
        }

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // ===== 基础辅助线（十字准星 + 瞄准圈） =====
        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);
        canvas.drawCircle(cx, cy, 60, paintRed);

        // ===== AI辅助绘制 =====
        AimAssistManager aimManager = AimAssistManager.getInstance();

        // 1. 获取AI推荐瞄准点
        android.graphics.Point aimPoint = aimManager.getRecommendedAimPoint();
        if (aimPoint != null) {
            Paint aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            aimPaint.setColor(Color.rgb(0, 255, 255));
            aimPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(aimPoint.x, aimPoint.y, 8, aimPaint);
            DrawUtils.drawAimLine(canvas, new android.graphics.Point(cx, cy), aimPoint);
        }

        // 2. 绘制走位预测
        List<android.graphics.Point> path = aimManager.getPredictedPath();
        if (path != null && !path.isEmpty()) {
            DrawUtils.drawPath(canvas, path);
        }

        // 3. 绘制力度条（右下角）
        int power = aimManager.calculatePower();
        DrawUtils.drawPowerBar(canvas, screenWidth - 250, screenHeight - 60, power, 150);

        // 4. 绘制角度指示器
        double angle = aimManager.calculateBestAngle();
        DrawUtils.drawAngleIndicator(canvas, new android.graphics.Point(cx, cy), angle, 80);

        // 5. 显示辅助信息（左上角）
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);
        textPaint.setAlpha(180);
        String info = "角度: " + String.format("%.1f", angle) + "° | 力度: " + power + "%";
        canvas.drawText(info, 20, 60, textPaint);

        // 6. 显示是否可进球
        Paint shotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shotPaint.setTextSize(24);
        shotPaint.setAlpha(200);
        if (aimManager.hasClearShot()) {
            shotPaint.setColor(Color.rgb(0, 255, 0));
            canvas.drawText("✅ 可进球!", 20, 100, shotPaint);
        } else {
            shotPaint.setColor(Color.rgb(255, 100, 0));
            canvas.drawText("⚠️ 调整角度", 20, 100, shotPaint);
        }

        // 7. 绘制准星
        DrawUtils.drawCrosshair(canvas, new android.graphics.Point(cx, cy), 100);

        // 8. 中心红点
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 10, paintRed);

        // 9. 🆕 显示温度（左上角，方便查看）
        Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempPaint.setColor(currentTemp >= TEMP_HIGH ? Color.RED : Color.GREEN);
        tempPaint.setTextSize(16);
        tempPaint.setAlpha(200);
        String tempInfo = "🌡️ " + String.format("%.1f", currentTemp) + "°C";
        canvas.drawText(tempInfo, 20, 140, tempPaint);

        overlayView.setImageBitmap(bitmap);
    }

    @Override
    public void onDestroy() {
        try {
            if (floatingView != null && floatingView.getParent() != null) {
                windowManager.removeView(floatingView);
            }
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
