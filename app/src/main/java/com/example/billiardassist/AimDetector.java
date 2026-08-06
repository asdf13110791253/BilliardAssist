package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * 瞄准检测器 - 使用OpenCV检测瞄准中心
 */
public class AimDetector {

    private static final String TAG = "AimDetector";
    private Mat templateMat;

    public AimDetector(Bitmap templateBitmap) {
        if (templateBitmap != null && !templateBitmap.isRecycled()) {
            templateMat = new Mat();
            Utils.bitmapToMat(templateBitmap, templateMat);
            Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGRA2GRAY);
        }
    }

    /**
     * 检测瞄准中心点
     * 使用圆形检测找到屏幕中的目标球
     */
    public android.graphics.Point detectAimCenter(Bitmap screenBitmap) {
        if (screenBitmap == null || screenBitmap.isRecycled()) {
            return null;
        }

        try {
            Mat src = new Mat();
            Utils.bitmapToMat(screenBitmap, src);

            // 转为灰度图
            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY);

            // 高斯模糊去噪
            Mat blurred = new Mat();
            Imgproc.GaussianBlur(gray, blurred, new Size(9, 9), 2, 2);

            // 霍夫圆检测
            Mat circles = new Mat();
            Imgproc.HoughCircles(blurred, circles, Imgproc.HOUGH_GRADIENT, 1, 50, 100, 30, 10, 100);

            // 释放中间变量
            src.release();
            gray.release();
            blurred.release();

            // 检查是否检测到圆
            if (circles.cols() > 0) {
                // 取第一个圆作为目标
                double[] circle = circles.get(0, 0);
                float cx = (float) circle[0];
                float cy = (float) circle[1];
                float radius = (float) circle[2];

                Log.d(TAG, "检测到圆: center=(" + cx + ", " + cy + "), radius=" + radius);

                // 如果模板匹配不为空，进一步验证
                if (templateMat != null && !templateMat.empty()) {
                    // TODO: 使用模板匹配验证
                }

                circles.release();
                return new android.graphics.Point((int) cx, (int) cy);
            }

            circles.release();

            // 如果没有检测到圆，返回屏幕中心
            Log.d(TAG, "未检测到圆，返回屏幕中心");
            return new android.graphics.Point(screenBitmap.getWidth() / 2, screenBitmap.getHeight() / 2);

        } catch (Exception e) {
            Log.e(TAG, "检测失败", e);
            return new android.graphics.Point(screenBitmap.getWidth() / 2, screenBitmap.getHeight() / 2);
        }
    }

    public void release() {
        if (templateMat != null) {
            templateMat.release();
            templateMat = null;
        }
    }
}
