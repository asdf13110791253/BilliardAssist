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
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.billiardassist.App;
import com.example.billiardassist.R;
import com.example.billiardassist.ai.AimAssistManager;
import com.example.billiardassist.utils.DrawUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

public class FloatingService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private WindowManager windowManager;
    private View floatingView;
    private ImageView overlayView;
    private WindowManager.LayoutParams params;
    private Handler mainHandler;
    private Paint paintGreen, paintRed, paintYellow, paintBlue;
    private int screenWidth, screenHeight;

    private int lineColor = Color.GREEN;
    private float lineThickness = 5.0f;
    private boolean showAntLine = false;

    // 温度控制
    private int frameCount = 0;
    private boolean isCharging = false;
    private float currentTemp = 35.0f;
    private static final float TEMP_HIGH = 35.0f;

    // 模拟球
    private float demoCueX, demoCueY;
    private float demoTargetX, demoTargetY;
    private float demoPocketX, demoPocketY;
    private int demoStep = 0;

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
        Toast.makeText(this, "✅ 辅助服务已启动", Toast.LENGTH_SHORT).show();
        startDrawingLoop();
        return START_STICKY;
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("台球辅助运行中")
                .setContentText("正在识别台球...")
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

        paintBlue = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBlue.setColor(Color.CYAN);
        paintBlue.setStrokeWidth(3);
        paintBlue.setStyle(Paint.Style.STROKE);
    }

    private void checkChargingStatus() {
        try {
            android.os.BatteryManager batteryManager = (android.os.BatteryManager) getSystemService(BATTERY_SERVICE);
            if (batteryManager != null) {
                isCharging = batteryManager.isCharging();
            }
        } catch (Exception e) {
            isCharging = false;
        }
    }

    private float getBatteryTemperature() {
        try {
            String[] paths = {
                "/sys/class/thermal/thermal_zone0/temp",
                "/sys/class/power_supply/battery/temp",
                "/sys/class/power_supply/battery/batt_temp"
            };
            for (String path : paths) {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(path));
                    String line = reader.readLine();
                    reader.close();
                    if (line != null) {
                        float temp = Float.parseFloat(line.trim());
                        if (temp > 1000) {
                            temp = temp / 1000.0f;
                        }
                        return temp;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 35.0f;
    }

    private int calculateSmartDelay() {
        currentTemp = getBatteryTemperature();
        if (isCharging) return 16;
        if (currentTemp >= TEMP_HIGH) return 33;
        if (frameCount < 30) return 16;
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

        // 初始化模拟球位置
        demoCueX = screenWidth * 0.25f;
        demoCueY = screenHeight * 0.7f;
        demoTargetX = screenWidth * 0.5f;
        demoTargetY = screenHeight * 0.45f;
        demoPocketX = screenWidth * 0.8f;
        demoPocketY = screenHeight * 0.15f;

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
                // 模拟球运动
                demoStep++;
                if (demoStep > 200) demoStep = 0;
                double angle = demoStep * 0.03;
                demoTargetX = screenWidth * 0.5f + (float) (Math.cos(angle) * 80);
                demoTargetY = screenHeight * 0.45f + (float) (Math.sin(angle * 0.7) * 60);
                demoPocketX = screenWidth * 0.8f + (float) (Math.sin(angle * 0.5) * 40);
                demoPocketY = screenHeight * 0.15f + (float) (Math.cos(angle * 0.6) * 30);

                drawGuideLines(screenWidth / 2, screenHeight / 2);
                frameCount++;
                int delay = calculateSmartDelay();
                mainHandler.postDelayed(this, delay);
            }
        });
    }

    public void drawGuideLines(int cx, int cy) {
        if (overlayView == null || cx < 0 || cy < 0 || screenWidth <= 0 || screenHeight <= 0) return;

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

        // 十字准星
        Paint crossPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crossPaint.setColor(Color.GREEN);
        crossPaint.setStrokeWidth(2);
        crossPaint.setAlpha(150);
        canvas.drawLine(0, cy, screenWidth, cy, crossPaint);
        canvas.drawLine(cx, 0, cx, screenHeight, crossPaint);
        canvas.drawCircle(cx, cy, 60, paintRed);

        Paint ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(18);
        textPaint.setAlpha(200);

        // 白球
        ballPaint.setColor(Color.WHITE);
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(demoCueX, demoCueY, 20, ballPaint);
        ballPaint.setColor(Color.BLACK);
        ballPaint.setStyle(Paint.Style.STROKE);
        ballPaint.setStrokeWidth(2);
        canvas.drawCircle(demoCueX, demoCueY, 20, ballPaint);
        canvas.drawText("⚪ 白球", demoCueX - 30, demoCueY - 30, textPaint);

        // 目标球
        ballPaint.setColor(Color.YELLOW);
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(demoTargetX, demoTargetY, 20, ballPaint);
        ballPaint.setColor(Color.BLACK);
        ballPaint.setStyle(Paint.Style.STROKE);
        ballPaint.setStrokeWidth(2);
        canvas.drawCircle(demoTargetX, demoTargetY, 20, ballPaint);
        canvas.drawText("🟡 目标球", demoTargetX - 30, demoTargetY - 30, textPaint);

        // 袋口
        ballPaint.setColor(Color.BLACK);
        ballPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(demoPocketX, demoPocketY, 18, ballPaint);
        ballPaint.setColor(Color.RED);
        ballPaint.setStyle(Paint.Style.STROKE);
        ballPaint.setStrokeWidth(2);
        canvas.drawCircle(demoPocketX, demoPocketY, 18, ballPaint);
        canvas.drawText("⚫ 袋口", demoPocketX - 30, demoPocketY - 30, textPaint);

        // 瞄准线
        Paint aimLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        aimLine.setColor(Color.GREEN);
        aimLine.setStyle(Paint.Style.STROKE);
        aimLine.setStrokeWidth(4);
        aimLine.setAlpha(200);
        canvas.drawLine(demoCueX, demoCueY, demoTargetX, demoTargetY, aimLine);

        // 进球路线
        Paint pocketLine = new Paint(Paint.ANTI_ALIAS_FLAG);
        pocketLine.setColor(Color.rgb(200, 100, 255));
        pocketLine.setStyle(Paint.Style.STROKE);
        pocketLine.setStrokeWidth(3);
        pocketLine.setAlpha(180);
        canvas.drawLine(demoTargetX, demoTargetY, demoPocketX, demoPocketY, pocketLine);

        // 瞄准点
        float dx = demoPocketX - demoTargetX;
        float dy = demoPocketY - demoTargetY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0) {
            float nx = dx / len * 20;
            float ny = dy / len * 20;
            float aimX = demoTargetX - nx;
            float aimY = demoTargetY - ny;

            Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            pointPaint.setColor(Color.rgb(255, 100, 255));
            pointPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(aimX, aimY, 10, pointPaint);
            canvas.drawText("🎯 瞄准点", aimX - 30, aimY - 25, textPaint);
        }

        // 走位预测
        Paint pathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pathPaint.setColor(Color.CYAN);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(3);
        pathPaint.setAlpha(150);
        for (int i = 1; i <= 4; i++) {
            int step = i * 30;
            float px = demoTargetX + (demoPocketX - demoTargetX) / len * step;
            float py = demoTargetY + (demoPocketY - demoTargetY) / len * step;
            canvas.drawCircle(px, py, 5, pathPaint);
            if (i < 4) {
                float nextX = demoTargetX + (demoPocketX - demoTargetX) / len * (step + 30);
                float nextY = demoTargetY + (demoPocketY - demoTargetY) / len * (step + 30);
                canvas.drawLine(px, py, nextX, nextY, pathPaint);
            }
        }

        // 中心红点
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);

        // 信息显示
        Paint infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        infoPaint.setColor(Color.WHITE);
        infoPaint.setTextSize(20);
        infoPaint.setAlpha(220);
        double angle = Math.toDegrees(Math.atan2(demoPocketY - demoTargetY, demoPocketX - demoTargetX));
        canvas.drawText("🎱 台球辅助瞄准", 20, 50, infoPaint);
        canvas.drawText("📐 角度: " + String.format("%.1f", angle) + "°", 20, 90, infoPaint);
        canvas.drawText("💪 力度: 65%", 20, 130, infoPaint);

        // 温度
        Paint tempPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tempPaint.setColor(currentTemp >= TEMP_HIGH ? Color.RED : Color.GREEN);
        tempPaint.setTextSize(16);
        tempPaint.setAlpha(200);
        canvas.drawText("🌡️ " + String.format("%.1f", currentTemp) + "°C", 20, 170, tempPaint);

        // 可进球
        Paint shotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shotPaint.setTextSize(24);
        shotPaint.setAlpha(200);
        shotPaint.setColor(Color.rgb(0, 255, 0));
        canvas.drawText("✅ 可进球!", 20, 210, shotPaint);

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
