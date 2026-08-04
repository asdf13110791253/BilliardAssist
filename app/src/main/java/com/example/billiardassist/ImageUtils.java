package com.example.billiardassist;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import java.nio.ByteBuffer;

public class ImageUtils {

    /**
     * 将 Android Media Image 转为 Bitmap（RGBA_8888格式）
     */
    public static Bitmap imageToBitmap(Image image) {
        if (image == null) return null;

        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        if (rowStride == width * pixelStride) {
            // 无行填充，直接拷贝
            bitmap.copyPixelsFromBuffer(buffer);
        } else {
            // 有行填充，逐行处理
            int[] pixels = new int[width * height];
            byte[] rowBuffer = new byte[rowStride];
            for (int y = 0; y < height; y++) {
                buffer.position(y * rowStride);
                buffer.get(rowBuffer, 0, rowStride);
                for (int x = 0; x < width; x++) {
                    int index = x * pixelStride;
                    int r = rowBuffer[index] & 0xFF;
                    int g = rowBuffer[index + 1] & 0xFF;
                    int b = rowBuffer[index + 2] & 0xFF;
                    int a = rowBuffer[index + 3] & 0xFF;
                    pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }

        return bitmap;
    }

    /**
     * 从资源加载 Bitmap（用于模板）
     */
    public static Bitmap decodeResource(Resources res, int resId) {
        if (resId == 0) return null;
        return BitmapFactory.decodeResource(res, resId);
    }
}
