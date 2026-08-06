package com.example.billiardassist.ai;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class AimDetector {

    private static final String TAG = "AimDetector";

    private int minRadius = 8;
    private int maxRadius = 80;

    public AimDetector(Bitmap templateBitmap) {
        // 不需要模板
    }

    public List<Ball> detectAllBalls(Bitmap screenBitmap) {
        List<Ball> balls = new ArrayList<>();
        if (screenBitmap == null || screenBitmap.isRecycled()) return balls;

        try {
            Mat src = new Mat();
            Utils.bitmapToMat(screenBitmap, src);

            Mat hsv = new Mat();
            Imgproc.cvtColor(src, hsv, Imgproc.COLOR_BGRA2HSV);

            Mat brightMask = new Mat();
            Scalar lowerBright = new Scalar(0, 0, 180);
            Scalar upperBright = new Scalar(180, 100, 255);
            Core.inRange(hsv, lowerBright, upperBright, brightMask);

            Mat colorMask = new Mat();
            Scalar lowerColor = new Scalar(0, 50, 50);
            Scalar upperColor = new Scalar(180, 255, 200);
            Core.inRange(hsv, lowerColor, upperColor, colorMask);

            Mat combinedMask = new Mat();
            Core.bitwise_or(brightMask, colorMask, combinedMask);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
            Mat cleaned = new Mat();
            Imgproc.morphologyEx(combinedMask, cleaned, Imgproc.MORPH_OPEN, kernel);

            Mat gray = new Mat();
            Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY);

            Mat circles = new Mat();
            Imgproc.HoughCircles(gray, circles, Imgproc.HOUGH_GRADIENT, 1.5, 30, 100, 30, minRadius, maxRadius);

            if (circles.cols() > 0) {
                for (int i = 0; i < circles.cols(); i++) {
                    double[] circle = circles.get(0, i);
                    float cx = (float) circle[0];
                    float cy = (float) circle[1];
                    float radius = (float) circle[2];

                    int type = classifyBall(src, cx, cy, radius);
                    Ball ball = new Ball((int) cx, (int) cy, (int) radius, type);
                    balls.add(ball);
                }
            }

            src.release();
            hsv.release();
            brightMask.release();
            colorMask.release();
            combinedMask.release();
            cleaned.release();
            gray.release();
            circles.release();

        } catch (Exception e) {
            Log.e(TAG, "检测失败", e);
        }

        return balls;
    }

    private int classifyBall(Mat src, float cx, float cy, float radius) {
        try {
            int centerX = (int) cx;
            int centerY = (int) cy;
            double avgBrightness = getAverageBrightness(src, centerX, centerY, (int) radius);

            if (avgBrightness > 200) {
                return Ball.TYPE_CUE;
            } else if (avgBrightness > 100) {
                return Ball.TYPE_TARGET;
            } else {
                return Ball.TYPE_POCKET;
            }
        } catch (Exception e) {
            return Ball.TYPE_UNKNOWN;
        }
    }

    private double getAverageBrightness(Mat src, int cx, int cy, int radius) {
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGRA2GRAY);

        Mat mask = Mat.zeros(gray.size(), gray.type());
        Core.circle(mask, new org.opencv.core.Point(cx, cy), radius, new Scalar(255), -1);

        Scalar mean = Core.mean(gray, mask);
        gray.release();
        mask.release();

        return mean.val[0];
    }

    public Point detectCueBall(Bitmap screenBitmap) {
        List<Ball> balls = detectAllBalls(screenBitmap);
        for (Ball ball : balls) {
            if (ball.type == Ball.TYPE_CUE) {
                return new Point(ball.x, ball.y);
            }
        }
        return null;
    }

    public Point detectTargetBall(Bitmap screenBitmap, Point cueBall) {
        List<Ball> balls = detectAllBalls(screenBitmap);
        double minDist = Double.MAX_VALUE;
        Point target = null;

        for (Ball ball : balls) {
            if (ball.type == Ball.TYPE_CUE) continue;
            double dist = 0;
            if (cueBall != null) {
                dist = Math.sqrt(Math.pow(ball.x - cueBall.x, 2) + Math.pow(ball.y - cueBall.y, 2));
            }
            if (dist < minDist) {
                minDist = dist;
                target = new Point(ball.x, ball.y);
            }
        }
        return target;
    }

    public void release() {}

    public static class Ball {
        public static final int TYPE_UNKNOWN = 0;
        public static final int TYPE_CUE = 1;
        public static final int TYPE_TARGET = 2;
        public static final int TYPE_POCKET = 3;

        public int x, y, radius;
        public int type;

        public Ball(int x, int y, int radius, int type) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.type = type;
        }

        @Override
        public String toString() {
            return "Ball{x=" + x + ", y=" + y + ", radius=" + radius + ", type=" + type + '}';
        }
    }
}
