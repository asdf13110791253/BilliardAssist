package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;

import com.example.billiardassist.ai.AimDetector.Ball;

import java.util.ArrayList;
import java.util.List;

/**
 * AI瞄准管理器 - 顶级完整版
 */
public class AimAssistManager {

    private static AimAssistManager instance;
    private AimDetector aimDetector;

    private Point cueBall;
    private Point targetBall;
    private List<Point> pockets;
    private List<Point> allBalls;
    private List<Point> predictedPath;
    private List<ShotSuggestion> suggestions;

    private RectF tableBounds = new RectF(30, 30, 1050, 2310);

    private static final int BALL_RADIUS = 18;
    private static final int POCKET_RADIUS = 28;
    private static final int MAX_BOUNCE = 3;
    private static final double FRICTION = 0.97;
    private static final double CUSHION_ELASTICITY = 0.82;

    private AimAssistManager() {
        aimDetector = new AimDetector(null);
        pockets = new ArrayList<>();
        allBalls = new ArrayList<>();
        predictedPath = new ArrayList<>();
        suggestions = new ArrayList<>();
    }

    public static AimAssistManager getInstance() {
        if (instance == null) {
            instance = new AimAssistManager();
        }
        return instance;
    }

    public void analyzeFrame(Bitmap screenBitmap) {
        if (screenBitmap == null || screenBitmap.isRecycled()) return;

        List<Ball> balls = aimDetector.detectAllBalls(screenBitmap);
        resetData();

        for (Ball ball : balls) {
            allBalls.add(new Point(ball.x, ball.y));
            switch (ball.type) {
                case Ball.TYPE_CUE:
                    cueBall = new Point(ball.x, ball.y);
                    break;
                case Ball.TYPE_TARGET:
                    break;
                case Ball.TYPE_POCKET:
                    pockets.add(new Point(ball.x, ball.y));
                    break;
            }
        }

        if (pockets.isEmpty() && screenBitmap != null) {
            autoDetectPockets(screenBitmap);
        }

        selectBestTarget(balls);

        if (cueBall != null && targetBall != null && !pockets.isEmpty()) {
            calculateAllShots();
        }
    }

    private void resetData() {
        allBalls.clear();
        pockets.clear();
        predictedPath.clear();
        suggestions.clear();
    }

    private void autoDetectPockets(Bitmap screenBitmap) {
        int w = screenBitmap.getWidth();
        int h = screenBitmap.getHeight();
        float m = Math.min(w, h) * 0.045f;

        pockets.add(new Point((int) m, (int) m));
        pockets.add(new Point((int) (w - m), (int) m));
        pockets.add(new Point((int) m, (int) (h - m)));
        pockets.add(new Point((int) (w - m), (int) (h - m)));
        pockets.add(new Point((int) (w / 2), (int) m));
        pockets.add(new Point((int) (w / 2), (int) (h - m)));

        tableBounds.set(m, m, w - m, h - m);
    }

    private void selectBestTarget(List<Ball> balls) {
        if (cueBall == null) return;

        Point best = null;
        double bestScore = Double.MAX_VALUE;

        for (Ball ball : balls) {
            if (ball.type != Ball.TYPE_TARGET) continue;
            Point p = new Point(ball.x, ball.y);

            double dist = distance(cueBall, p);
            double minPocketDist = getMinPocketDistance(p);
            double score = dist * 0.5 + minPocketDist * 0.5;

            for (Point pocket : pockets) {
                if (!hasObstacleBetween(p, pocket)) {
                    score -= 30;
                    break;
                }
            }

            if (score < bestScore) {
                bestScore = score;
                best = p;
            }
        }

        if (best != null) {
            targetBall = best;
        } else if (!balls.isEmpty()) {
            double minDist = Double.MAX_VALUE;
            for (Ball ball : balls) {
                if (ball.type == Ball.TYPE_CUE) continue;
                Point p = new Point(ball.x, ball.y);
                double d = distance(cueBall, p);
                if (d < minDist) {
                    minDist = d;
                    targetBall = p;
                }
            }
        }
    }

    private double getMinPocketDistance(Point p) {
        double min = Double.MAX_VALUE;
        for (Point pocket : pockets) {
            double d = distance(p, pocket);
            if (d < min) min = d;
        }
        return min;
    }

    private void calculateAllShots() {
        for (Point pocket : pockets) {
            ShotSuggestion direct = analyzeDirectShot(pocket);
            if (direct != null) {
                direct.score = calculateScore(direct);
                suggestions.add(direct);
            }

            ShotSuggestion bank1 = analyzeBankShot(pocket, 1);
            if (bank1 != null) {
                bank1.score = calculateScore(bank1);
                suggestions.add(bank1);
            }

            ShotSuggestion bank2 = analyzeBankShot(pocket, 2);
            if (bank2 != null) {
                bank2.score = calculateScore(bank2);
                suggestions.add(bank2);
            }

            ShotSuggestion bank3 = analyzeBankShot(pocket, 3);
            if (bank3 != null) {
                bank3.score = calculateScore(bank3);
                suggestions.add(bank3);
            }

            ShotSuggestion defense = analyzeDefenseShot();
            if (defense != null) {
                defense.score = calculateScore(defense);
                suggestions.add(defense);
            }
        }

        suggestions.sort((a, b) -> Double.compare(b.score, a.score));

        if (!suggestions.isEmpty()) {
            generatePath(suggestions.get(0));
        }
    }

    private ShotSuggestion analyzeDirectShot(Point pocket) {
        if (cueBall == null || targetBall == null) return null;

        if (hasObstacleBetween(targetBall, pocket)) return null;
        if (hasObstacleBetween(cueBall, targetBall)) return null;

        double angle = calculateAngle(cueBall, targetBall, pocket);
        if (Math.abs(angle) > 5.0) return null;

        ShotSuggestion shot = new ShotSuggestion();
        shot.type = "直接进球";
        shot.pocket = pocket;
        shot.angle = angle;
        shot.aimPoint = calculateAimPoint(targetBall, pocket);
        shot.distance = distance(cueBall, targetBall);
        shot.power = calculatePower(shot.distance);
        shot.bounceCount = 0;
        shot.isDefense = false;

        return shot;
    }

    private ShotSuggestion analyzeBankShot(Point pocket, int bounceCount) {
        if (cueBall == null || targetBall == null) return null;

        Point mirror = calculateMirrorPoint(pocket, bounceCount);
        if (mirror == null) return null;

        double dx = mirror.x - targetBall.x;
        double dy = mirror.y - targetBall.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return null;

        double ratio = BALL_RADIUS / len;
        Point hitPoint = new Point(
            (int) (targetBall.x + dx * ratio),
            (int) (targetBall.y + dy * ratio)
        );

        if (!isInTable(hitPoint)) return null;
        if (hasObstacleBetween(targetBall, hitPoint)) return null;
        if (hasObstacleBetween(hitPoint, pocket)) return null;
        if (hasObstacleBetween(cueBall, targetBall)) return null;

        double angle = calculateAngle(cueBall, targetBall, hitPoint);
        if (Math.abs(angle) > 8.0) return null;

        ShotSuggestion shot = new ShotSuggestion();
        shot.type = bounceCount + "库翻袋";
        shot.pocket = pocket;
        shot.angle = angle;
        shot.aimPoint = hitPoint;
        shot.distance = distance(cueBall, targetBall);
        shot.power = (int) (calculatePower(shot.distance) * (1 + bounceCount * 0.05));
        shot.bounceCount = bounceCount;
        shot.isDefense = false;

        return shot;
    }

    private Point calculateMirrorPoint(Point pocket, int bounceCount) {
        if (bounceCount <= 0) return pocket;

        float l = tableBounds.left;
        float r = tableBounds.right;
        float t = tableBounds.top;
        float b = tableBounds.bottom;

        float mx = pocket.x;
        float my = pocket.y;

        for (int i = 0; i < bounceCount; i++) {
            float dLeft = mx - l;
            float dRight = r - mx;
            float dTop = my - t;
            float dBottom = b - my;

            float minD = Math.min(Math.min(dLeft, dRight), Math.min(dTop, dBottom));

            if (minD == dLeft) {
                mx = 2 * l - mx;
            } else if (minD == dRight) {
                mx = 2 * r - mx;
            } else if (minD == dTop) {
                my = 2 * t - my;
            } else {
                my = 2 * b - my;
            }
        }

        Point result = new Point((int) mx, (int) my);
        if (Math.abs(mx) > 3000 || Math.abs(my) > 3000) return null;

        return result;
    }

    private ShotSuggestion analyzeDefenseShot() {
        if (cueBall == null || targetBall == null) return null;

        Point safeSpot = findSafestSpot();

        ShotSuggestion shot = new ShotSuggestion();
        shot.type = "防守";
        shot.pocket = null;
        shot.angle = calculateAngle(cueBall, targetBall, safeSpot);
        shot.aimPoint = safeSpot;
        shot.distance = distance(cueBall, targetBall);
        shot.power = 25;
        shot.bounceCount = 0;
        shot.isDefense = true;

        return shot;
    }

    private Point findSafestSpot() {
        Point best = new Point(500, 500);
        double maxDist = 0;

        int step = 30;
        for (int x = (int) tableBounds.left + 40; x < tableBounds.right - 40; x += step) {
            for (int y = (int) tableBounds.top + 40; y < tableBounds.bottom - 40; y += step) {
                Point p = new Point(x, y);

                boolean occupied = false;
                for (Point ball : allBalls) {
                    if (distance(p, ball) < BALL_RADIUS * 2.5) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) continue;

                double minDist = Double.MAX_VALUE;
                for (Point ball : allBalls) {
                    double d = distance(p, ball);
                    if (d < minDist) minDist = d;
                }

                if (minDist > maxDist) {
                    maxDist = minDist;
                    best = p;
                }
            }
        }

        return best;
    }

    private double calculateScore(ShotSuggestion shot) {
        double score = 80.0;
        score -= Math.abs(shot.angle) * 2.5;
        score -= shot.distance / 60.0;
        score -= shot.bounceCount * 10.0;

        if ("直接进球".equals(shot.type)) {
            score += 20.0;
        }

        if (shot.isDefense) {
            score += 15.0;
        }

        if (shot.pocket != null) {
            double pocketDist = distance(shot.aimPoint, shot.pocket);
            if (pocketDist < 100) score += 10;
        }

        if (shot.pocket != null && !hasObstacleBetween(shot.aimPoint, shot.pocket)) {
            score += 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    private void generatePath(ShotSuggestion shot) {
        predictedPath.clear();
        if (cueBall == null) return;

        double dx = shot.aimPoint.x - cueBall.x;
        double dy = shot.aimPoint.y - cueBall.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return;

        double nx = dx / len;
        double ny = dy / len;

        float px = cueBall.x;
        float py = cueBall.y;
        float vx = (float) (nx * shot.power / 45.0);
        float vy = (float) (ny * shot.power / 45.0);

        for (int i = 0; i < 40; i++) {
            px += vx * 3;
            py += vy * 3;

            vx *= FRICTION;
            vy *= FRICTION;

            // ✅ 修复：强制转换为float
            if (px < tableBounds.left || px > tableBounds.right) {
                vx = (float)(-vx * CUSHION_ELASTICITY);
                px = Math.max(tableBounds.left + 5, Math.min(tableBounds.right - 5, px));
            }
            if (py < tableBounds.top || py > tableBounds.bottom) {
                vy = (float)(-vy * CUSHION_ELASTICITY);
                py = Math.max(tableBounds.top + 5, Math.min(tableBounds.bottom - 5, py));
            }

            if (Math.abs(vx) < 0.1 && Math.abs(vy) < 0.1) break;

            if (i % 2 == 0) {
                predictedPath.add(new Point((int) px, (int) py));
            }
        }
    }

    private double calculateAngle(Point from, Point target, Point to) {
        double a1 = Math.atan2(target.y - from.y, target.x - from.x);
        double a2 = Math.atan2(to.y - target.y, to.x - target.x);
        double diff = a2 - a1;
        double angle = Math.toDegrees(Math.atan2(Math.sin(diff), Math.cos(diff)));
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private Point calculateAimPoint(Point target, Point pocket) {
        double dx = pocket.x - target.x;
        double dy = pocket.y - target.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return target;

        double nx = dx / len * BALL_RADIUS;
        double ny = dy / len * BALL_RADIUS;

        return new Point((int) (target.x - nx), (int) (target.y - ny));
    }

    private int calculatePower(double distance) {
        double maxDist = Math.min(tableBounds.width(), tableBounds.height());
        double ratio = distance / maxDist;
        int power = (int) (25 + ratio * 65);
        return Math.max(20, Math.min(90, power));
    }

    private boolean hasObstacleBetween(Point from, Point to) {
        for (Point ball : allBalls) {
            if (ball.equals(from) || ball.equals(to)) continue;
            double d = pointToLineDistance(ball, from, to);
            if (d < BALL_RADIUS * 1.8) return true;
        }
        return false;
    }

    private double pointToLineDistance(Point p, Point a, Point b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return distance(p, a);
        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (len * len);
        t = Math.max(0, Math.min(1, t));
        double px = a.x + t * dx;
        double py = a.y + t * dy;
        return distance(p, new Point((int) px, (int) py));
    }

    private double distance(Point a, Point b) {
        return Math.sqrt(Math.pow(b.x - a.x, 2) + Math.pow(b.y - a.y, 2));
    }

    private boolean isInTable(Point p) {
        return p.x > tableBounds.left + 10 && p.x < tableBounds.right - 10 &&
               p.y > tableBounds.top + 10 && p.y < tableBounds.bottom - 10;
    }

    // ==================== Getter方法 ====================
    public Point getCueBall() { return cueBall; }
    public Point getTargetBall() { return targetBall; }
    public List<Point> getPockets() { return pockets; }
    public List<Point> getPredictedPath() { return predictedPath; }
    public List<ShotSuggestion> getSuggestions() { return suggestions; }

    public ShotSuggestion getBestShot() {
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }

    public double calculateBestAngle() {
        ShotSuggestion best = getBestShot();
        return best != null ? best.angle : 0;
    }

    public int calculatePower() {
        ShotSuggestion best = getBestShot();
        return best != null ? best.power : 50;
    }

    public boolean hasClearShot() {
        ShotSuggestion best = getBestShot();
        return best != null && !best.isDefense;
    }

    public Point getRecommendedAimPoint() {
        ShotSuggestion best = getBestShot();
        return best != null ? best.aimPoint : null;
    }

    public RectF getTableBounds() { return tableBounds; }

    // ==================== 内部类 ====================
    public static class ShotSuggestion {
        public String type;
        public Point pocket;
        public Point aimPoint;
        public double angle;
        public int power;
        public double distance;
        public int bounceCount;
        public boolean isDefense;
        public double score;

        @Override
        public String toString() {
            return type + " | 角度:" + String.format("%.1f", angle) +
                   "° | 力度:" + power + "% | 评分:" + String.format("%.1f", score);
        }
    }
}
