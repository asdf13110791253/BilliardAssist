package com.example.billiardassist;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;

import java.nio.ByteBuffer;

public class ImageUtils {

    /**
     * 将 Android Media Image 转为 Bitmap（稳健实现）
     */
    public static Bitmap imageToBitmap(Image image) {
        if (image == null) return null;
        try {
            // 仅处理常见 RGBA_8888 / Android ImageReader 输出（单 plane 或多 plane 支持）
            Image.Plane[] planes = image.getPlanes();
            if (planes == null || planes.length == 0) return null;
            Image.Plane plane = planes[0];
            ByteBuffer buffer = plane.getBuffer();
            if (buffer == null) return null;
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int width = image.getWidth();
            int height = image.getHeight();

            // 防护：尺寸合理性检查
            if (width <= 0 || height <= 0 || pixelStride <= 0 || rowStride <= 0) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            // 若没有行填充，可一次性拷贝
            if (rowStride == width * pixelStride) {
                buffer.rewind();
                bitmap.copyPixelsFromBuffer(buffer);
                return bitmap;
            }

            // 否则逐行拷贝并转换为 ARGB
            byte[] rowBuffer = new byte[rowStride];
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                buffer.position(y * rowStride);
                buffer.get(rowBuffer, 0, rowStride);
                for (int x = 0; x < width; x++) {
                    int index = x * pixelStride;
                    // 防护索引越界
                    if (index + 3 >= rowBuffer.length) {
                        pixels[y * width + x] = 0; // 透明像素
                        continue;
                    }
                    int r = rowBuffer[index] & 0xFF;
                    int g = rowBuffer[index + 1] & 0xFF;
                    int b = rowBuffer[index + 2] & 0xFF;
                    int a = rowBuffer[index + 3] & 0xFF;
                    pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                image.close();
            } catch (Exception ignored) {}
        }
    }

    public static Bitmap decodeResource(Resources res, int resId) {
        if (resId == 0) return null;
        try {
            return BitmapFactory.decodeResource(res, resId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
