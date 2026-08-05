package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class AimDetector {
    private Mat templateMat;

    public AimDetector(Bitmap templateBitmap) {
        if (templateBitmap != null && !templateBitmap.isRecycled()) {
            templateMat = new Mat();
            Utils.bitmapToMat(templateBitmap, templateMat);
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGRA2GRAY);
        }
    }

    /**
     * 检测瞄准中心点（目前返回屏幕中心，后续可接入模板匹配算法）
     */
    public Point detectAimCenter(Bitmap screenBitmap) {
        if (screenBitmap == null || templateMat == null || templateMat.empty()) {
            return null;
        }
        // TODO: 后续在此接入 OpenCV matchTemplate 算法
        return new Point(screenBitmap.getWidth() / 2, screenBitmap.getHeight() / 2);
    }

    public void release() {
        if (templateMat != null) {
            templateMat.release();
            templateMat = null;
        }
    }
}
