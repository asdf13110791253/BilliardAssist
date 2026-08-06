package com.example.billiardassist.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import java.util.List;

public class DrawUtils {

    private static final Paint paintCue = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint paintTarget = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint paintPath = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint paintPocket = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint paintPower = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        paintCue.setColor(Color.WHITE);
        paintCue.setStyle(Paint.Style.FILL);
        paintCue.setStrokeWidth(3);

        paintTarget.setColor(Color.YELLOW);
        paintTarget.setStyle(Paint.Style.FILL);

        paintPath.setColor(Color.CYAN);
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setStrokeWidth(2);
        paintPath.setAlpha(180);

        paintPocket.setColor(Color.BLACK);
        paintPocket.setStyle(Paint.Style.FILL);

        paintPower.setColor(Color.rgb(255, 200, 0));
        paintPower.setStyle(Paint.Style.FILL);
    }

    public static void drawCueBall(Canvas canvas, Point ball, int radius) {
        if (ball == null) return;
        canvas.drawCircle(ball.x, ball.y, radius, paintCue);
    }

    public static void drawTargetBall(Canvas canvas, Point ball, int radius) {
        if (ball == null) return;
        canvas.drawCircle(ball.x, ball.y, radius, paintTarget);
    }

    public static void drawPath(Canvas canvas, List<Point> path) {
        if (path == null || path.size() < 2) return;
        for (int i = 0; i < path.size() - 1; i++) {
            Point start = path.get(i);
            Point end = path.get(i + 1);
            canvas.drawLine(start.x, start.y, end.x, end.y, paintPath);
        }
        Point last = path.get(path.size() - 1);
        canvas.drawCircle(last.x, last.y, 5, paintPath);
    }

    public static void drawPocket(Canvas canvas, Point pocket, int radius) {
        if (pocket == null) return;
        canvas.drawCircle(pocket.x, pocket.y, radius, paintPocket);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        canvas.drawCircle(pocket.x, pocket.y, radius + 5, paint);
    }

    public static void drawAimLine(Canvas canvas, Point from, Point to) {
        if (from == null || to == null) return;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setAlpha(200);
        canvas.drawLine(from.x, from.y, to.x, to.y, paint);
    }

    public static void drawPowerBar(Canvas canvas, int x, int y, int power, int maxWidth) {
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.GRAY);
        bgPaint.setAlpha(100);
        canvas.drawRect(x, y, x + maxWidth, y + 20, bgPaint);

        int fillWidth = (int) ((power / 100.0) * maxWidth);
        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (power < 30) {
            fillPaint.setColor(Color.GREEN);
        } else if (power < 60) {
            fillPaint.setColor(Color.YELLOW);
        } else {
            fillPaint.setColor(Color.RED);
        }
        canvas.drawRect(x, y, x + fillWidth, y + 20, fillPaint);

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1);
        canvas.drawRect(x, y, x + maxWidth, y + 20, borderPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12);
        canvas.drawText("力度: " + power + "%", x + maxWidth + 10, y + 15, textPaint);
    }

    public static void drawAngleIndicator(Canvas canvas, Point center, double angle, int radius) {
        if (center == null) return;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(255, 100, 100));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        double rad = Math.toRadians(angle);
        float endX = (float) (center.x + Math.cos(rad) * radius);
        float endY = (float) (center.y + Math.sin(rad) * radius);
        canvas.drawLine(center.x, center.y, endX, endY, paint);
    }

    public static void drawCrosshair(Canvas canvas, Point center, int size) {
        if (center == null) return;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.rgb(0, 255, 0));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setAlpha(150);
        canvas.drawLine(center.x - size, center.y, center.x + size, center.y, paint);
        canvas.drawLine(center.x, center.y - size, center.x, center.y + size, paint);

        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(Color.rgb(255, 0, 0));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(1);
        circlePaint.setAlpha(180);
        canvas.drawCircle(center.x, center.y, 20, circlePaint);

        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.rgb(255, 0, 0));
        dotPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(center.x, center.y, 3, dotPaint);
    }
}
