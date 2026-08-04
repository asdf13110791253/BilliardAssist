package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import com.example.billiardassist.AimDetector;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * 图像处理与瞄准计算核心
 * 对应截图：图13（图像识别方案选择 + V/S/P 三个参数）
 *
 * 功能：
 * - 管理 8 种识别方案（零一到零八）
 * - V（亮度阈值）、S（圆白度）、P（圆形检测灵敏度）
 * - 调用 AimDetector 做模板匹配
 * - 计算辅助线角度（角度补偿 / 镜像反射）
 */
public class AimProcessor {

    // ===== 8 种识别方案 =====
    public static final int SCHEME_0 = 0; // 零一：标准台球
    public static final int SCHEME_1 = 1; // 零二：九球
    public static final int SCHEME_2 = 2; // 零三：斯诺克
    public static final int SCHEME_3 = 3; // 零四：开伦
    public static final int SCHEME_4 = 4; // 零五：黑八
    public static final int SCHEME_5 = 5; // 零六：十球
    public static final int SCHEME_6 = 6; // 零七：十四一
    public static final int SCHEME_7 = 7; // 零八：自定义

    // ===== 可调参数 =====
    private int vValue = 232;       // 亮度阈值 (0~255)
    private int sValue = 15;        // 圆白度 (0~100)
    private int pValue = 15;        // 圆形检测灵敏度 (0~100)
    private int currentScheme = 0;  // 当前方案

    // ===== 反射模式 =====
    public static final int MODE_COMPENSATION = 0;  // 角度补偿
    public static final int MODE_MIRROR = 1;         // 镜像反射
    private int reflectMode = MODE_COMPENSATION;

    // ===== 瞄准检测器 =====
    private AimDetector aimDetector;

    /**
     * 构造函数
     * @param templateBitmap 瞄准环模板图
     */
    public AimProcessor(Bitmap templateBitmap) {
        if (templateBitmap != null) {
            this.aimDetector = new AimDetector(templateBitmap);
        }
    }

    // ===== 设置参数 =====

    public void setScheme(int scheme) {
        if (scheme < 0 || scheme > 7) return;
        this.currentScheme = scheme;
        // 不同方案预设不同参数
        switch (scheme) {
            case 0: vValue = 232; sValue = 15; pValue = 15; break;
            case 1: vValue = 220; sValue = 20; pValue = 18; break;
            case 2: vValue = 200; sValue = 25; pValue = 12; break;
            case 3: vValue = 240; sValue = 10; pValue = 20; break;
            case 4: vValue = 215; sValue = 18; pValue = 16; break;
            case 5: vValue = 225; sValue = 22; pValue = 14; break;
            case 6: vValue = 210; sValue = 30; pValue = 10; break;
            case 7: /* 自定义，不重置 */ break;
        }
    }

    public void setV(int v) { this.vValue = clamp(v, 0, 255); }
    public void setS(int s) { this.sValue = clamp(s, 0, 100); }
    public void setP(int p) { this.pValue = clamp(p, 0, 100); }
    public void setReflectMode(int mode) { this.reflectMode = mode; }

    public int getV() { return vValue; }
    public int getS() { return sValue; }
    public int getP() { return pValue; }
    public int getScheme() { return currentScheme; }
    public int getReflectMode() { return reflectMode; }

    // ===== 核心处理 =====

    /**
     * 处理一帧截图，返回瞄准中心点
     */
    public Point processFrame(Bitmap screenBitmap) {
        if (screenBitmap == null || aimDetector == null) return null;
        return aimDetector.detectAimCenter(screenBitmap);
    }

    /**
     * 计算辅助线角度（角度补偿模式）
     * @param cueX 白球X
     * @param cueY 白球Y
     * @param targetX 目标球X
     * @param targetY 目标球Y
     * @param pocketX 袋口X
     * @param pocketY 袋口Y
     * @return 辅助线终点坐标 [endX, endY]
     */
    public double[] calculateAimLine(float cueX, float cueY, float targetX, float targetY,
                                      float pocketX, float pocketY) {
        double dx = targetX - cueX;
        double dy = targetY - cueY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return new double[]{cueX, cueY};

        if (reflectMode == MODE_MIRROR) {
            // 镜像反射：入射角 = 出射角
            double totalDx = pocketX - cueX;
            double totalDy = pocketY - cueY;
            double len = Math.sqrt(totalDx * totalDx + totalDy * totalDy);
            return new double[]{cueX + totalDx / len * 2000, cueY + totalDy / len * 2000};
        } else {
            // 角度补偿模式
            double compAngle = Math.atan2(pocketY - targetY, pocketX - targetX);
            double offset = (pValue / 100.0) * 0.18; // 补偿比例
            double finalAngle = compAngle + offset;
            double aimX = Math.cos(finalAngle) * 2000;
            double aimY = Math.sin(finalAngle) * 2000;
            return new double[]{cueX + aimX, cueY + aimY};
        }
    }

    /**
     * 计算翻袋路径（库边镜像法）
     */
    public double[] calculateBankShot(float cueX, float cueY, float targetX, float targetY,
                                      float pocketX, float pocketY,
                                      float[] tableBounds, int bankCount) {
        // 对袋口做镜像
        float left = tableBounds[0], top = tableBounds[1];
        float right = tableBounds[2], bottom = tableBounds[3];

        double bestDx = pocketX - targetX;
        double bestDy = pocketY - targetY;
        double bestDist = Math.sqrt(bestDx * bestDx + bestDy * bestDy);

        if (bankCount >= 1) {
            // 上下镜像
            checkMirror(pocketX, 2 * top - pocketY, targetX, targetY);
            checkMirror(pocketX, 2 * bottom - pocketY, targetX, targetY);
            // 左右镜像
            checkMirror(2 * left - pocketX, pocketY, targetX, targetY);
            checkMirror(2 * right - pocketX, pocketY, targetX, targetY);
        }

        // 简化：返回第一个镜像方向
        return calculateAimLine(cueX, cueY, targetX, targetY,
                (float) (targetX + bestDx), (float) (targetY + bestDy));
    }

    private void checkMirror(float mx, float my, float tx, float ty) {
        // 留作扩展：选最近的镜像袋口
    }

    // ===== 工具方法 =====

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    /**
     * 释放资源
     */
    public void release() {
        if (aimDetector != null) {
            aimDetector.release();
            aimDetector = null;
        }
    }

    /**
     * 应用 HSV 阈值过滤（用于白球/目标球检测）
     */
    public Bitmap applyHSVFilter(Bitmap input) {
        if (input == null) return null;
        Mat src = new Mat();
        Utils.bitmapToMat(input, src);
        Mat hsv = new Mat();
        Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGRA2HSV);

        // V 通道阈值
        Mat mask = new Mat();
        Core.inRange(hsv, new org.opencv.core.Scalar(0, 0, vValue),
                new org.opencv.core.Scalar(180, 255, 255), mask);

        Mat result = new Mat();
        src.copyTo(result, mask);

        Bitmap out = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, out);

        src.release(); hsv.release(); mask.release(); result.release();
        return out;
    }
}
