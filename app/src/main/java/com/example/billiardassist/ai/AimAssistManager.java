package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;

import com.example.billiardassist.ai.AimDetector.Ball;

import java.util.ArrayList;
import java.util.List;

public class AimAssistManager {

    private static AimAssistManager instance;
    private AimDetector aimDetector;

    private Point cueBall;
    private Point targetBall;
    private List<Point> pockets;
    private List<Point> allBalls;
    private List<Point> predictedPath;

    private RectF tableBounds = new RectF(0, 0, 1080, 2340);

    private AimAssistManager() {
        aimDetector = new AimDetector(null);
        pockets = new ArrayList<>();
        allBalls = new ArrayList<>();
        predictedPath = new ArrayList<>();
    }

    public static AimAssistManager getInstance() {
        if (instance == null) {
            instance = new AimAssistManager();
        }
        return instance;
    }

    public void analyzeFrame(Bitmap screenBitmap) {
        if (screenBitmap == null || screenBitmap.isRecycled()) {
            return;
        }

        List<Ball> balls = aimDetector.detectAllBalls(screenBitmap);

        allBalls.clear();
        pockets.clear();
        cueBall = null;
        targetBall = null;

        for (Ball ball : balls) {
            allBalls.add(new Point(ball.x, ball.y));

            switch (ball.type) {
                case Ball.TYPE_CUE:
                    cueBall = new Point(ball.x, ball.y);
                    break;
                case Ball.TYPE_TARGET:
                    if (cueBall != null) {
                        double dist = distance(ball.x, ball.y, cueBall.x, cueBall.y);
                        if (targetBall == null || dist < distance(targetBall.x, targetBall.y, cueBall.x, cueBall.y)) {
                            targetBall = new Point(ball.x, ball.y);
                        }
                    } else {
                        if (targetBall == null) {
                            targetBall = new Point(ball.x, ball.y);
                        }
                    }
                    break;
                case Ball.TYPE_POCKET:
                    pockets.add(new Point(ball.x, ball.y));
                    break;
            }
        }

        if (pockets.isEmpty() && screenBitmap != null) {
            pockets.add(new Point(50, 50));
            pockets.add(new Point(screenBitmap.getWidth() - 50, 50));
            pockets.add(new Point(50, screenBitmap.getHeight() - 50));
            pockets.add(new Point(screenBitmap.getWidth() - 50, screenBitmap.getHeight() - 50));
        }

        predictPath();
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private void predictPath() {
        predictedPath.clear();
        if (cueBall == null || targetBall == null || pockets.isEmpty()) return;

        Point pocket = pockets.get(0);
        double dx = targetBall.x - cueBall.x;
        double dy = targetBall.y - cueBall.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return;

        double nx = dx / length;
        double ny = dy / length;

        for (int i = 1; i <= 5; i++) {
            int step = i * 30;
            int px = (int) (targetBall.x + nx * step);
            int py = (int) (targetBall.y + ny * step);
            predictedPath.add(new Point(px, py));
        }
    }

    public double calculateBestAngle() {
        if (cueBall == null || targetBall == null || pockets.isEmpty()) return 0;
        Point pocket = pockets.get(0);

        double dx = pocket.x - targetBall.x;
        double dy = pocket.y - targetBall.y;
        double targetAngle = Math.atan2(dy, dx);

        double cx = targetBall.x - cueBall.x;
        double cy = targetBall.y - cueBall.y;
        double cueAngle = Math.atan2(cy, cx);

        double angleDiff = targetAngle - cueAngle;
        return Math.toDegrees(angleDiff);
    }

    public int calculatePower() {
        if (cueBall == null || targetBall == null) return 50;
        double distance = Math.sqrt(
            Math.pow(targetBall.x - cueBall.x, 2) +
            Math.pow(targetBall.y - cueBall.y, 2)
        );
        double maxDistance = Math.min(tableBounds.width(), tableBounds.height());
        int power = (int) ((distance / maxDistance) * 100);
        return Math.max(10, Math.min(100, power));
    }

    public boolean hasClearShot() {
        if (cueBall == null || targetBall == null || pockets.isEmpty()) return false;
        Point pocket = pockets.get(0);

        double dx1 = targetBall.x - cueBall.x;
        double dy1 = targetBall.y - cueBall.y;
        double dx2 = pocket.x - targetBall.x;
        double dy2 = pocket.y - targetBall.y;

        double dot = dx1 * dx2 + dy1 * dy2;
        double len1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        double len2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

        if (len1 < 1 || len2 < 1) return false;
        double cosAngle = dot / (len1 * len2);
        double angle = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosAngle))));
        return angle < 15;
    }

    public Point getRecommendedAimPoint() {
        if (cueBall == null || targetBall == null || pockets.isEmpty()) return null;
        Point pocket = pockets.get(0);

        double dx = pocket.x - targetBall.x;
        double dy = pocket.y - targetBall.y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1) return null;

        double radius = 10;
        double nx = dx / length * radius;
        double ny = dy / length * radius;

        return new Point((int) (targetBall.x - nx), (int) (targetBall.y - ny));
    }

    public Point getCueBall() { return cueBall; }
    public Point getTargetBall() { return targetBall; }
    public List<Point> getPockets() { return pockets; }
    public List<Point> getPredictedPath() { return predictedPath; }
    public RectF getTableBounds() { return tableBounds; }
    public void setTableBounds(RectF bounds) { this.tableBounds = bounds; }
    public void reset() {
        cueBall = null;
        targetBall = null;
        pockets.clear();
        allBalls.clear();
        predictedPath.clear();
    }
}
