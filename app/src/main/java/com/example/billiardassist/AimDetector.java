package com.example.billiardassist;

import android.graphics.Bitmap;
import android.graphics.Point;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AimDetector {
    // 匹配阈值，可根据识别灵敏度调整
    private static final double MATCH_THRESHOLD = 0.75;
    private Mat templateMat;

    // 初始化识别模板
    public AimDetector(Bitmap templateBitmap) {
        if (templateBitmap == null || templateBitmap.isRecycled()) {
            throw new IllegalArgumentException("模板Bitmap为空或已回收");
        }
        templateMat = new Mat();
        Utils.bitmapToMat(templateBitmap, templateMat);
        Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGRA2GRAY);
    }

    // 核心识别方法，返回瞄准中心点
    public Point detectAimCenter(Bitmap screenBitmap) {
        // 空值拦截
        if (screenBitmap == null || screenBitmap.isRecycled() || templateMat == null || templateMat.empty()) {
            return null;
        }

        Mat screenMat = new Mat();
        Utils.bitmapToMat(screenBitmap, screenMat);
        Imgproc.cvtColor(screenMat, screenMat, Imgproc.COLOR_BGRA2GRAY);

        // 边界校验：截图尺寸必须大于模板
        int resultCols = screenMat.cols() - templateMat.cols() + 1;
        int resultRows = screenMat.rows() - templateMat.rows() + 1;
        if (resultCols <= 0 || resultRows <= 0) {
            screenMat.release();
            return null;
        }

        Mat resultMat = new Mat(resultRows, resultCols, CvType.CV_32FC1);
        Imgproc.matchTemplate(screenMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat);
        // 及时释放Mat，防止内存溢出
        screenMat.release();
        resultMat.release();

        // 低于阈值判定无匹配目标
        if (mmr.maxVal < MATCH_THRESHOLD) {
            return null;
        }

        // 计算模板中心坐标
        int centerX = (int) (mmr.maxLoc.x + templateMat.cols() / 2.0);
        int centerY = (int) (mmr.maxLoc.y + templateMat.rows() / 2.0);
        return new Point(centerX, centerY);
    }

    // 释放模板内存，页面销毁时必须调用
    public void release() {
        if (templateMat != null) {
            templateMat.release();
            templateMat = null;
        }
    }
}
