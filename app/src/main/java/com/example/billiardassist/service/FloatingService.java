package com.example.billiardassist.service;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import com.example.billiardassist.R;

/**
 * 悬浮窗服务 - 负责在屏幕上绘制辅助线
 * 对应截图：图3、图16（启动悬浮辅助界面）
 */
public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private ImageView overlayView;
    private WindowManager.LayoutParams params;
    private Handler handler = new Handler();
    private Paint paintGreen, paintRed, paintYellow;
    private int screenWidth, screenHeight;

    // 可配置参数（从 SharedPreferences 读取）
    private int lineColor = Color.GREEN;
    private float lineThickness = 5.0f;
    private boolean showAntLine = false;
    private boolean adsorbNearest = true;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // 加载用户配置
        loadSettings();

        startOverlay();
        startDrawing();

        return START_STICKY;
    }

    private void loadSettings() {
        var prefs = getSharedPreferences("settings", MODE_PRIVATE);
        lineColor = prefs.getInt("line_color", Color.GREEN);
        lineThickness = prefs.getFloat("line_thickness", 5.0f);
        showAntLine = prefs.getBoolean("show_ant_line", false);
        adsorbNearest = prefs.getBoolean("adsorb_nearest", true);
    }

    private void startOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null);
        overlayView = floatingView.findViewById(R.id.overlay_canvas);

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

        windowManager.addView(floatingView, params);

        // 初始化画笔
        paintGreen = new Paint();
        paintGreen.setColor(lineColor);
        paintGreen.setStrokeWidth(lineThickness);
        paintGreen.setAntiAlias(true);
        paintGreen.setStyle(Paint.Style.STROKE);

        paintRed = new Paint();
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(lineThickness + 1);
        paintRed.setAntiAlias(true);

        paintYellow = new Paint();
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3);
        paintYellow.setAntiAlias(true);
    }

    private void startDrawing() {
        // 模拟瞄准点（实际项目中由 AimProcessor 回调驱动）
        handler.post(new Runnable() {
            @Override
            public void run() {
                drawGuideLines(screenWidth / 2, screenHeight / 2);
            }
        });
    }

    /**
     * 绘制辅助线（供外部调用）
     * @param cx 瞄准中心点 X
     * @param cy 瞄准中心点 Y
     */
    public void drawGuideLines(int cx, int cy) {
        if (overlayView == null) return;

        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        // 1. 水平延长线（贯穿全屏）
        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);

        // 2. 垂直延长线
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);

        // 3. 瞄准圆圈
        canvas.drawCircle(cx, cy, 60, paintRed);

        // 4. 中心红点
        paintRed.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, 8, paintRed);
        paintRed.setStyle(Paint.Style.STROKE);

        // 5. 蚂蚁线（可选）
        if (showAntLine) {
            drawDashedLine(canvas, cx - 300, cy - 300, cx + 300, cy + 300, paintYellow);
        }

        // 6. 斜向辅助线
        canvas.drawLine(cx - 200, cy - 200, cx + 200, cy + 200, paintYellow);
        canvas.drawLine(cx + 200, cy - 200, cx - 200, cy + 200, paintYellow);

        overlayView.setImageBitmap(bitmap);
    }

    /**
     * 绘制虚线（蚂蚁线效果）
     */
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

    /**
     * 更新线颜色和粗细（供 SettingsActivity 调用）
     */
    public void updateLineStyle(int color, float thickness) {
        this.lineColor = color;
        this.lineThickness = thickness;
        paintGreen.setColor(color);
        paintGreen.setStrokeWidth(thickness);
        paintRed.setStrokeWidth(thickness + 1);
    }

    /**
     * 切换蚂蚁线显示
     */
    public void setAntLineVisible(boolean visible) {
        this.showAntLine = visible;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
