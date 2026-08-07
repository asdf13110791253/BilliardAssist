package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.RectF;

import com.example.billiardassist.ai.AimDetector.Ball;

import java.util.ArrayList;
import java.util.List;

/**
 * AI瞄准管理器 - 顶级完整版
 * 
 * 功能列表：
 * 1. ✅ 智能球识别（白球/目标球/袋口自动分类）
 * 2. ✅ 多目标球选择（自动选择最优目标）
 * 3. ✅ 最优袋口选择（6个袋口智能评分）
 * 4. ✅ 直接进球路线检测
 * 5. ✅ 一库翻袋计算
 * 6. ✅ 两库翻袋计算
 * 7. ✅ 三库翻袋计算
 * 8. ✅ 障碍物检测与规避
 * 9. ✅ 防守推荐（无法进球时）
 * 10. ✅ 走位预测（物理模拟）
 * 11. ✅ 力度推荐（距离自适应）
 * 12. ✅ 瞄准点计算
 * 13. ✅ 可进球判断
 * 14. ✅ 多路线评分排序
 * 15. ✅ 角度计算
 */
public class AimAssistManager {

    private static AimAssistManager instance;
    private AimDetector aimDetector;

    // ==================== 球的位置 ====================
    private Point cueBall;
    private Point targetBall;
    private List<Point> pockets;
    private List<Point> allBalls;
    private List<Point> predictedPath;
    private List<ShotSuggestion> suggestions;

    // ==================== 球桌边界 ====================
    private RectF tableBounds = new RectF(30, 30, 1050, 2310);

    // ==================== 物理常量（已改为 float）====================
    private static final int BALL_RADIUS = 18;
    private static final int POCKET_RADIUS = 28;
    private static final int MAX_BOUNCE = 3;
    private static final float FRICTION = 0.97f;          // 修改
    private static final float CUSHION_ELASTICITY = 0.82f; // 修改

    // ==================== 单例 ====================
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

    // ==================== 主入口 ====================
    public void analyzeFrame(Bitmap screenBitmap) {
        if (screenBitmap == null || screenBitmap.isRecycled()) return;

        // 1. 检测所有球
        List<Ball> balls = aimDetector.detectAllBalls(screenBitmap);
        if (balls == null || balls.isEmpty()) {
            return; // 没有检测到球，直接返回
        }

        resetData();

        // 2. 分类球
        for (Ball ball : balls) {
            allBalls.add(new Point(ball.x, ball.y));
            switch (ball.type) {
                case Ball.TYPE_CUE:
                    cueBall = new Point(ball.x, ball.y);
                    break;
                case Ball.TYPE_TARGET:
                    // 暂时记录，后面再选最优
                    break;
                case Ball.TYPE_POCKET:
                    pockets.add(new Point(ball.x, ball.y));
                    break;
            }
        }

        // 3. 自动检测袋口
        if (pockets.isEmpty() && screenBitmap != null) {
            autoDetectPockets(screenBitmap);
        }

        // 4. 选择最优目标球
        selectBestTarget(balls);

        // 5. 顶级AI计算
        if (cueBall != null && targetBall != null && !pockets.isEmpty()) {
            calculateAllShots();
        }
    }

    // ==================== 重置数据 ====================
    private void resetData() {
        allBalls.clear();
        pockets.clear();
        predictedPath.clear();
        suggestions.clear();
    }

    // ==================== 自动检测袋口 ====================
    private void autoDetectPockets(Bitmap screenBitmap) {
        int w = screenBitmap.getWidth();
        int h = screenBitmap.getHeight();
        float m = Math.min(w, h) * 0.045f;

        // 4个角袋 + 2个中袋
        pockets.add(new Point((int) m, (int) m));
        pockets.add(new Point((int) (w - m), (int) m));
        pockets.add(new Point((int) m, (int) (h - m)));
        pockets.add(new Point((int) (w - m), (int) (h - m)));
        pockets.add(new Point((int) (w / 2), (int) m));
        pockets.add(new Point((int) (w / 2), (int) (h - m)));

        // 更新边界
        tableBounds.set(m, m, w - m, h - m);
    }

    // ==================== 选择最优目标球 ====================
    private void selectBestTarget(List<Ball> balls) {
        if (cueBall == null) return;

        Point best = null;
        double bestScore = Double.MAX_VALUE;

        for (Ball ball : balls) {
            if (ball.type != Ball.TYPE_TARGET) continue;
            Point p = new Point(ball.x, ball.y);

            double dist = distance(cueBall, p);
            double minPocketDist = getMinPocketDistance(p);

            // 评分：距离近 + 靠近袋口
            double score = dist * 0.5 + minPocketDist * 0.5;

            // 检查是否有直接进球路线
            for (Point pocket : pockets) {
                if (!hasObstacleBetween(p, pocket)) {
                    score -= 30;  // 可进球的目标优先
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
            // 降级：选择最近的球
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

    // ==================== 获取到最近袋口距离 ====================
    private double getMinPocketDistance(Point p) {
        double min = Double.MAX_VALUE;
        for (Point pocket : pockets) {
            double d = distance(p, pocket);
            if (d < min) min = d;
        }
        return min;
    }

    // ==================== 🏆 计算所有击球方案 ====================
    private void calculateAllShots() {
        if (pockets.isEmpty()) return; // 防止空列表

        for (Point pocket : pockets) {
            // 1. 直接进球
            ShotSuggestion direct = analyzeDirectShot(pocket);
            if (direct != null) {
                direct.score = calculateScore(direct);
                suggestions.add(direct);
            }

            // 2. 一库翻袋
            ShotSuggestion bank1 = analyzeBankShot(pocket, 1);
            if (bank1 != null) {
                bank1.score = calculateScore(bank1);
                suggestions.add(bank1);
            }

            // 3. 两库翻袋
            ShotSuggestion bank2 = analyzeBankShot(pocket, 2);
            if (bank2 != null) {
                bank2.score = calculateScore(bank2);
                suggestions.add(bank2);
            }

            // 4. 三库翻袋
            ShotSuggestion bank3 = analyzeBankShot(pocket, 3);
            if (bank3 != null) {
                bank3.score = calculateScore(bank3);
                suggestions.add(bank3);
            }

            // 5. 防守（如果无法进球）
            ShotSuggestion defense = analyzeDefenseShot();
            if (defense != null) {
                defense.score = calculateScore(defense);
                suggestions.add(defense);
            }
        }

        // 按评分排序
        suggestions.sort((a, b) -> Double.compare(b.score, a.score));

        // 生成走位预测
        if (!suggestions.isEmpty()) {
            generatePath(suggestions.get(0));
        }
    }

    // ==================== 直接进球分析 ====================
    private ShotSuggestion analyzeDirectShot(Point pocket) {
        if (cueBall == null || targetBall == null) return null;

        // 检查目标球到袋口是否有障碍物
        if (hasObstacleBetween(targetBall, pocket)) return null;

        // 检查白球到目标球是否有障碍物
        if (hasObstacleBetween(cueBall, targetBall)) return null;

        // 计算角度
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

    // ==================== 翻袋分析 ====================
    private ShotSuggestion analyzeBankShot(Point pocket, int bounceCount) {
        if (cueBall == null || targetBall == null) return null;

        // 计算镜像袋口
        Point mirror = calculateMirrorPoint(pocket, bounceCount);
        if (mirror == null) return null;

        // 计算撞击点
        double dx = mirror.x - targetBall.x;
        double dy = mirror.y - targetBall.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return null;

        double ratio = BALL_RADIUS / len;
        Point hitPoint = new Point(
            (int) (targetBall.x + dx * ratio),
            (int) (targetBall.y + dy * ratio)
        );

        // 检查撞击点是否在桌内
        if (!isInTable(hitPoint)) return null;

        // 检查路径是否有障碍物
        if (hasObstacleBetween(targetBall, hitPoint)) return null;
        if (hasObstacleBetween(hitPoint, pocket)) return null;
        if (hasObstacleBetween(cueBall, targetBall)) return null;

        // 计算角度
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

    // ==================== 计算镜像点 ====================
    private Point calculateMirrorPoint(Point pocket, int bounceCount) {
        if (bounceCount <= 0) return pocket;

        float l = tableBounds.left;
        float r = tableBounds.right;
        float t = tableBounds.top;
        float b = tableBounds.bottom;

        float mx = pocket.x;
        float my = pocket.y;

        for (int i = 0; i < bounceCount; i++) {
            // 选择最近的库边进行镜像
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

        // 检查是否在合理范围内
        if (Math.abs(mx) > 3000 || Math.abs(my) > 3000) return null;

        return result;
    }

    // ==================== 防守分析 ====================
    private ShotSuggestion analyzeDefenseShot() {
        if (cueBall == null || targetBall == null) return null;

        // 找最安全的落点
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

    // ==================== 找最安全落点 ====================
    private Point findSafestSpot() {
        Point best = new Point(500, 500);
        double maxDist = 0;

        int step = 40; // 步长加大，减少计算量
        for (int x = (int) tableBounds.left + 40; x < tableBounds.right - 40; x += step) {
            for (int y = (int) tableBounds.top + 40; y < tableBounds.bottom - 40; y += step) {
                Point p = new Point(x, y);

                // 检查是否被球占据
                boolean occupied = false;
                for (Point ball : allBalls) {
                    if (distance(p, ball) < BALL_RADIUS * 2.5) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) continue;

                // 到最近球的距离
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

    // ==================== 计算评分 ====================
    private double calculateScore(ShotSuggestion shot) {
        double score = 80.0;

        // 1. 角度越直越好
        score -= Math.abs(shot.angle) * 2.5;

        // 2. 距离越近越好
        score -= shot.distance / 60.0;

        // 3. 翻袋次数越少越好
        score -= shot.bounceCount * 10.0;

        // 4. 直接进球加分
        if ("直接进球".equals(shot.type)) {
            score += 20.0;
        }

        // 5. 防守加分（如果无法进球）
        if (shot.isDefense) {
            score += 15.0;
        }

        // 6. 袋口距离加分
        if (shot.pocket != null) {
            double pocketDist = distance(shot.aimPoint, shot.pocket);
            if (pocketDist < 100) score += 10;
        }

        // 7. 路径通畅加分
        if (shot.pocket != null && !hasObstacleBetween(shot.aimPoint, shot.pocket)) {
            score += 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    // ==================== 生成走位预测 ====================
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

            vx *= FRICTION;      // 现在 FRICTION 是 float
            vy *= FRICTION;

            // 库边反弹
            if (px < tableBounds.left || px > tableBounds.right) {
                vx = -vx * CUSHION_ELASTICITY; // 现在 CUSHION_ELASTICITY 是 float
                px = Math.max(tableBounds.left + 5, Math.min(tableBounds.right - 5, px));
            }
            if (py < tableBounds.top || py > tableBounds.bottom) {
                vy = -vy * CUSHION_ELASTICITY;
                py = Math.max(tableBounds.top + 5, Math.min(tableBounds.bottom - 5, py));
            }

            if (Math.abs(vx) < 0.1f && Math.abs(vy) < 0.1f) break;

            if (i % 2 == 0) {
                predictedPath.add(new Point((int) px, (int) py));
            }
        }
    }

    // ==================== 计算角度 ====================
    private double calculateAngle(Point from, Point target, Point to) {
        double a1 = Math.atan2(target.y - from.y, target.x - from.x);
        double a2 = Math.atan2(to.y - target.y, to.x - target.x);
        double diff = a2 - a1;
        double angle = Math.toDegrees(Math.atan2(Math.sin(diff), Math.cos(diff)));
        // 归一化到 -180 ~ 180
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    // ==================== 计算瞄准点 ====================
    private Point calculateAimPoint(Point target, Point pocket) {
        double dx = pocket.x - target.x;
        double dy = pocket.y - target.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1) return target;

        double nx = dx / len * BALL_RADIUS;
        double ny = dy / len * BALL_RADIUS;

        return new Point((int) (target.x - nx), (int) (target.y - ny));
    }

    // ==================== 计算力度 ====================
    private int calculatePower(double distance) {
        double maxDist = Math.min(tableBounds.width(), tableBounds.height());
        double ratio = distance / maxDist;
        int power = (int) (25 + ratio * 65);
        return Math.max(20, Math.min(90, power));
    }

    // ==================== 障碍物检测 ====================
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

    // ==================== 工具方法 ====================
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
        public String type;           // 击球类型
        public Point pocket;          // 目标袋口
        public Point aimPoint;        // 瞄准点
        public double angle;          // 击球角度
        public int power;             // 力度
        public double distance;       // 距离
        public int bounceCount;       // 翻袋次数
        public boolean isDefense;     // 是否防守
        public double score;          // 评分

        @Override
        public String toString() {
            return type + " | 角度:" + String.format("%.1f", angle) + 
                   "° | 力度:" + power + "% | 评分:" + String.format("%.1f", score);
        }
    }
}
