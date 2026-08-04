package com.example.billiardassist.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billiardassist.R;

/**
 * 校准界面 - 桌布对齐
 * 对应截图：图14（8×1052 网格 + 与球桌内侧对齐提示）
 */
public class CalibrateActivity extends AppCompatActivity {

    private ImageView gridOverlay;
    private TextView tvStatus;
    private int gridCols = 8;
    private int gridRows = 8;
    private int gridLineColor = Color.YELLOW;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibrate);

        gridOverlay = findViewById(R.id.grid_overlay);
        tvStatus = findViewById(R.id.tv_calibrate_status);

        // 绘制校准网格
        drawCalibrationGrid();

        // 确认按钮
        findViewById(R.id.btn_calibrate_confirm).setOnClickListener(v -> {
            Toast.makeText(this, "校准完成，正在保存参数...", Toast.LENGTH_SHORT).show();
            saveCalibration();
            finish();
        });

        // 取消按钮
        findViewById(R.id.btn_calibrate_cancel).setOnClickListener(v -> finish());
    }

    /**
     * 绘制 N×M 校准网格（供用户对齐球桌内缘）
     */
    private void drawCalibrationGrid() {
        int w = 1080; // 默认宽度，实际应从屏幕获取
        int h = 1920;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(gridLineColor);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        int cellW = w / gridCols;
        int cellH = h / gridRows;

        // 画竖线
        for (int i = 0; i <= gridCols; i++) {
            int x = i * cellW;
            canvas.drawLine(x, 0, x, h, paint);
        }
        // 画横线
        for (int j = 0; j <= gridRows; j++) {
            int y = j * cellH;
            canvas.drawLine(0, y, w, y, paint);
        }

        // 画外框（粗线）
        paint.setStrokeWidth(4);
        paint.setColor(Color.RED);
        canvas.drawRect(0, 0, w - 1, h - 1, paint);

        gridOverlay.setImageBitmap(bitmap);
        tvStatus.setText("请将网格与球桌内缘对齐");
    }

    /**
     * 保存校准参数到 SharedPreferences
     */
    private void saveCalibration() {
        var prefs = getSharedPreferences("calibration", MODE_PRIVATE);
        prefs.edit()
                .putInt("grid_cols", gridCols)
                .putInt("grid_rows", gridRows)
                .putInt("grid_color", gridLineColor)
                .putLong("timestamp", System.currentTimeMillis())
                .apply();
    }
}
