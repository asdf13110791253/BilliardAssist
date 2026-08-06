package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import androidx.annotation.NonNull;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class AimProcessor {

    private static final String TAG = "AimProcessor";

    public static final int SCHEME_0 = 0;
    public static final int SCHEME_1 = 1;
    public static final int SCHEME_2 = 2;
    public static final int SCHEME_3 = 3;
    public static final int SCHEME_4 = 4;
    public static final int SCHEME_5 = 5;
    public static final int SCHEME_6 = 6;
    public static final int SCHEME_7 = 7;

    private int vValue = 232;
    private int sValue = 15;
    private int pValue = 15;
    private int currentScheme = 0;

    public static final int MODE_COMPENSATION = 0;
    public static final int MODE_MIRROR = 1;
    private int reflectMode = MODE_COMPENSATION;

    private AimDetector aimDetector;

    public AimProcessor(Bitmap templateBitmap) {
        if (templateBitmap != null && !templateBitmap.isRecycled()) {
            this.aimDetector = new AimDetector(templateBitmap);
        }
    }

    public void setScheme(int scheme) {
        if (scheme < 0 || scheme > 7) return;
        this.currentScheme = scheme;
        switch (scheme) {
            case 0: vValue = 232; sValue = 15; pValue = 15; break;
            case 1: vValue = 220; sValue = 20; pValue = 18; break;
            case 2: vValue = 200; sValue = 25; pValue = 12; break;
            case 3: vValue = 240; sValue = 10; pValue = 20; break;
            case 4: vValue = 215; sValue = 18; pValue = 16; break;
            case 5: vValue = 225; sValue = 22; pValue = 14; break;
            case 6: vValue = 210; sValue = 30; pValue = 10; break;
            case 7: break;
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

    public Point processFrame(@NonNull Bitmap screenBitmap) {
        if (screenBitmap.isRecycled() || aimDetector == null) return null;
        return aimDetector.detectCueBall(screenBitmap);
    }

    public double[] calculateAimLine(float cueX, float cueY, float targetX, float targetY,
                                     float pocketX, float pocketY) {
        double dx = targetX - cueX;
        double dy = targetY - cueY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 1) return new double[]{cueX, cueY};

        if (reflectMode == MODE_MIRROR) {
            double totalDx = pocketX - cueX;
            double totalDy = pocketY - cueY;
            double len = Math.sqrt(totalDx * totalDx + totalDy * totalDy);
            if (len < 1) return new double[]{cueX, cueY};
            return new double[]{cueX + totalDx / len * 2000, cueY + totalDy / len * 2000};
        } else {
            double compAngle = Math.atan2(pocketY - targetY, pocketX - targetX);
            double offset = (pValue / 100.0) * 0.18;
            double finalAngle = compAngle + offset;
            double aimX = Math.cos(finalAngle) * 2000;
            double aimY = Math.sin(finalAngle) * 2000;
            return new double[]{cueX + aimX, cueY + aimY};
        }
    }

    public double[] calculateBankShot(float cueX, float cueY, float targetX, float targetY,
                                      float pocketX, float pocketY,
                                      @NonNull float[] tableBounds, int bankCount) {
        if (tableBounds.length < 4) {
            return calculateAimLine(cueX, cueY, targetX, targetY, pocketX, pocketY);
        }
        float left = tableBounds[0], top = tableBounds[1];
        float right = tableBounds[2], bottom = tableBounds[3];

        double bestDx = pocketX - targetX;
        double bestDy = pocketY - targetY;
        double bestDist = Math.sqrt(bestDx * bestDx + bestDy * bestDy);

        if (bankCount >= 1) {
            bestDist = checkMirror(pocketX, 2 * top - pocketY, targetX, targetY, bestDist);
            bestDist = checkMirror(pocketX, 2 * bottom - pocketY, targetX, targetY, bestDist);
            bestDist = checkMirror(2 * left - pocketX, pocketY, targetX, targetY, bestDist);
            bestDist = checkMirror(2 * right - pocketX, pocketY, targetX, targetY, bestDist);
        }

        return calculateAimLine(cueX, cueY, targetX, targetY,
                (float) (targetX + bestDx), (float) (targetY + bestDy));
    }

    private double checkMirror(float mx, float my, float tx, float ty, double currentBest) {
        double dx = mx - tx;
        double dy = my - ty;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return Math.min(currentBest, dist);
    }

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    public void release() {
        if (aimDetector != null) {
            aimDetector.release();
            aimDetector = null;
        }
    }

    public Bitmap applyHSVFilter(@NonNull Bitmap input) {
        if (input.isRecycled() || input.getWidth() <= 0 || input.getHeight() <= 0) {
            return input;
        }
        Mat src = null, hsv = null, mask = null, result = null;
        try {
            src = new Mat();
            Utils.bitmapToMat(input, src);

            hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_RGB2HSV);

            Scalar lower = new Scalar(0, 0, vValue);
            Scalar upper = new Scalar(180, 255, 255);
            mask = new Mat();
            Core.inRange(hsv, lower, upper, mask);

            result = new Mat();
            Core.bitwise_and(src, src, result, mask);

            Bitmap output = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(result, output);
            return output;
        } catch (Exception e) {
            Log.e(TAG, "HSV过滤失败", e);
            return input;
        } finally {
            if (src != null) src.release();
            if (hsv != null) hsv.release();
            if (mask != null) mask.release();
            if (result != null) result.release();
        }
    }
}
