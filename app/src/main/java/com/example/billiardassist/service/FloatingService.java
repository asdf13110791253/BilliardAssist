package com.example.billiardassist.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.core.app.NotificationCompat;

import com.example.billiardassist.App;
import com.example.billiardassist.R;

/**
 * 悬浮窗服务 - 负责在屏幕上绘制辅助线
 * 对应截图：图3、图16（启动悬浮辅助界面）
 * ✅ 已修复：前台服务崩溃问题/Java语法错误/内存泄漏风险
 */
public class FloatingService extends Service {

    private static final int NOTIFICATION_ID = 1; // 前台服务通知ID（不可重复）
    private WindowManager windowManager;
    private View floatingView;
    private ImageView overlayView;
    private WindowManager.LayoutParams params;
    private Handler mainHandler; // 主线程Handler，避免子线程操作UI
    private Paint paintGreen, paintRed, paintYellow;
    private int screenWidth, screenHeight;

    // 可配置参数（从App全局配置读取，避免重复读取SP）
    private int lineColor = Color.GREEN;
    private float lineThickness = 5.0f;
    private boolean showAntLine = false;
    private boolean adsorbNearest = true;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper()); // 初始化主线程Handler
        loadSettings(); // 先加载配置，再初始化画笔
        initPaints(); // 初始化画笔
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 处理停止命令
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        // 启动前台服务（Android 8.0+强制要求，否则直接崩溃）
        startForeground(NOTIFICATION_ID, buildNotification());
        
        // 初始化悬浮窗和绘制逻辑
        initOverlayWindow();
        startDrawingLoop();

        return START_STICKY; // 服务被系统杀死后自动重启
    }

    /**
     * 构建前台服务通知（必须和App中创建的渠道ID一致）
     */
    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, App.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("台球辅助运行中")
                .setContentText("辅助服务正在后台绘制瞄准线")
                .setSmallIcon(R.mipmap.ic_launcher) // 建议使用纯色矢量图标，避免兼容性问题
                .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级，不打扰用户
                .setOngoing(true) // 不可滑动删除
                .build();
    }

    /**
     * 加载用户配置（从App全局配置读取，避免重复IO）
     */
    private void loadSettings() {
        lineColor = App.getInstance().getLineColor();
        lineThickness = App.getInstance().getLineThickness();
        showAntLine = App.getInstance().isAntLineEnabled();
        adsorbNearest = App.getInstance().isAdsorbEnabled();
    }

    /**
     * 初始化画笔（确保配置加载后再初始化）
     */
    private void initPaints() {
        // 绿色辅助线画笔
        paintGreen = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintGreen.setColor(lineColor);
        paintGreen.setStrokeWidth(lineThickness);
        paintGreen.setStyle(Paint.Style.STROKE);

        // 红色瞄准圈画笔
        paintRed = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(lineThickness + 1);
        paintRed.setStyle(Paint.Style.STROKE);

        // 黄色辅助线画笔
        paintYellow = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(3);
        paintYellow.setStyle(Paint.Style.STROKE);
    }

    /**
     * 初始化悬浮窗窗口
     */
    private void initOverlayWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        
        // 兼容新旧版本获取屏幕尺寸的方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.getCurrentWindowMetrics().getBounds();
            screenWidth = windowManager.getCurrentWindowMetrics().getBounds().width();
            screenHeight = windowManager.getCurrentWindowMetrics().getBounds().height();
        } else {
            windowManager.getDefaultDisplay().getMetrics(metrics);
            screenWidth = metrics.widthPixels;
            screenHeight = metrics.heightPixels;
        }

        // 加载悬浮窗布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null);
        overlayView = floatingView.findViewById(R.id.overlay_canvas);
        if (overlayView == null) {
            throw new IllegalStateException("布局文件中必须包含id为overlay_canvas的ImageView");
        }

        // 配置窗口参数
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
    }

    /**
     * 启动绘制循环（模拟瞄准点，实际应由AimProcessor回调驱动）
     */
    private void startDrawingLoop() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                // 模拟瞄准点（实际项目中替换为AimProcessor返回的实时坐标）
                drawGuideLines(screenWidth / 2, screenHeight / 2);
                // 每16ms刷新一次（约60fps，平衡性能和流畅度）
                mainHandler.postDelayed(this, 16);
            }
        });
    }

    /**
     * 绘制辅助线（对外暴露方法，供AimProcessor调用）
     * @param cx 瞄准中心点X坐标
     * @param cy 瞄准中心点Y坐标
     */
    public void drawGuideLines(int cx, int cy) {
        if (overlayView == null || cx < 0 || cy < 0) return;

        // 回收旧Bitmap，避免内存泄漏
        Bitmap oldBitmap = ((BitmapDrawable) overlayView.getDrawable())?.getBitmap();
        if (oldBitmap != null && !oldBitmap.isRecycled()) {
            oldBitmap.recycle();
        }

        // 创建新的Bitmap并绘制
        Bitmap bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        // 1. 水平贯穿辅助线
        canvas.drawLine(0, cy, screenWidth, cy, paintGreen);

        // 2. 垂直贯穿辅助线
        canvas.drawLine(cx, 0, cx, screenHeight, paintGreen);

        // 3. 瞄准外圈
        canvas.drawCircle(cx, cy, 60, paintRed);

        // 4. 瞄准中心点
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
     * 更新辅助线样式（供设置页调用）
     */
    public void updateLineStyle(int color, float thickness) {
        this.lineColor = color;
        this.lineThickness = thickness;
        paintGreen.setColor(color);
        paintGreen.setStrokeWidth(thickness);
        paintRed.setStrokeWidth(thickness + 1);
        // 同步更新全局配置
        App.getInstance().setLineColor(color);
        App.getInstance().setLineThickness(thickness);
    }

    /**
     * 切换蚂蚁线显示状态
     */
    public void setAntLineVisible(boolean visible) {
        this.showAntLine = visible;
        App.getInstance().setAntLineEnabled(visible);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 清理资源，避免内存泄漏
        mainHandler.removeCallbacksAndMessages(null);
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
        }
        // 回收Bitmap
        if (overlayView != null && overlayView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) overlayView.getDrawable()).getBitmap();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        // 停止前台服务
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 非绑定服务，返回null
    }
}
