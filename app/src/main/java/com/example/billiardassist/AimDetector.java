package com.example.billiardassist;

import android.graphics.Bitmap;
import android.graphics.Point;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class AimDetector {

    private static final double MATCH_THRESHOLD = 0.75;
    private Mat templateMat;

    public AimDetector(Bitmap templateBitmap) {
        if (templateBitmap == null) {
            throw new IllegalArgumentException("模板图片不能为空");
        }
        templateMat = new Mat();
        Utils.bitmapToMat(templateBitmap, templateMat);
        Imgproc.cvtColor(templateMat, templateMat, Imgproc.COLOR_BGRA2GRAY);
    }

    public Point detectAimCenter(Bitmap screenBitmap) {
        if (screenBitmap == null || templateMat == null) return null;

        Mat screenMat = new Mat();
        Utils.bitmapToMat(screenBitmap, screenMat);
        Imgproc.cvtColor(screenMat, screenMat, Imgproc.COLOR_BGRA2GRAY);

        int resultCols = screenMat.cols() - templateMat.cols() + 1;
        int resultRows = screenMat.rows() - templateMat.rows() + 1;
        if (resultCols <= 0 || resultRows <= 0) {
            screenMat.release();
            return null;
        }

        Mat resultMat = new Mat(resultRows, resultCols, CvType.CV_32FC1);
        Imgproc.matchTemplate(screenMat, templateMat, resultMat, Imgproc.TM_CCOEFF_NORMED);

        Core.MinMaxLocResult mmr = Core.minMaxLoc(resultMat);
        screenMat.release();
        resultMat.release();

        if (mmr.maxVal < MATCH_THRESHOLD) return null;

        int centerX = (int) (mmr.maxLoc.x + templateMat.cols() / 2.0);
        int centerY = (int) (mmr.maxLoc.y + templateMat.rows() / 2.0);
        return new Point(centerX, centerY);
    }

    public void release() {
        if (templateMat != null) {
            templateMat.release();
        }
    }
}
