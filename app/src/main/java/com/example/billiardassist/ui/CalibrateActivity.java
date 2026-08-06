package com.example.billiardassist.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billiardassist.R;
import com.example.billiardassist.ai.AimProcessor;

/**
 * 图像识别方案选择界面
 * 修复：类名改为 CalibrateActivity
 */
public class CalibrateActivity extends AppCompatActivity {

    private AimProcessor aimProcessor;

    // UI
    private RadioGroup rgScheme;
    private SeekBar seekV, seekS, seekP;
    private TextView tvV, tvS, tvP;
    private Button btnResetV, btnResetS, btnResetP;

    // 默认值
    private static final int DEFAULT_V = 232;
    private static final int DEFAULT_S = 15;
    private static final int DEFAULT_P = 15;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aim_scheme);

        // 初始化 AimProcessor（无模板时传 null，仅用参数调节）
        aimProcessor = new AimProcessor(null);

        initSchemeSelector();
        initSeekBars();
        initResetButtons();
    }

    // ===== 8 种方案选择 =====
    private void initSchemeSelector() {
        rgScheme = findViewById(R.id.rg_scheme);
        if (rgScheme != null) {
            int current = aimProcessor != null ? aimProcessor.getScheme() : 0;
            int childCount = rgScheme.getChildCount();
            if (current >= 0 && current < childCount) {
                if (rgScheme.getChildAt(current) instanceof RadioButton) {
                    ((RadioButton) rgScheme.getChildAt(current)).setChecked(true);
                }
            }

            rgScheme.setOnCheckedChangeListener((group, checkedId) -> {
                if (group == null) return;
                android.view.View child = group.findViewById(checkedId);
                if (child == null) return;
                int index = group.indexOfChild(child);
                if (index < 0) return;
                if (aimProcessor != null) aimProcessor.setScheme(index);
                // 切换方案后更新滑块显示
                updateSeekBarUI();
                Toast.makeText(this, "已切换到方案 " + (index + 1), Toast.LENGTH_SHORT).show();
            });
        }
    }

    // ===== 三个参数滑块 =====
    private void initSeekBars() {
        if (aimProcessor == null) aimProcessor = new AimProcessor(null);

        // V - 亮度 (0~255)
        tvV = findViewById(R.id.tv_v_value);
        seekV = findViewById(R.id.seek_v);
        if (seekV != null && tvV != null) {
            seekV.setMax(255);
            seekV.setProgress(clamp(aimProcessor.getV(), 0, 255));
            tvV.setText(String.valueOf(aimProcessor.getV()));
            seekV.setOnSeekBarChangeListener(makeSeekListener(
                    () -> aimProcessor.getV(),
                    v -> aimProcessor.setV(v),
                    tvV, "%d"
            ));
        }

        // S - 圆白度 (0~100)
        tvS = findViewById(R.id.tv_s_value);
        seekS = findViewById(R.id.seek_s);
        if (seekS != null && tvS != null) {
            seekS.setMax(100);
            seekS.setProgress(clamp(aimProcessor.getS(), 0, 100));
            tvS.setText(String.valueOf(aimProcessor.getS()));
            seekS.setOnSeekBarChangeListener(makeSeekListener(
                    () -> aimProcessor.getS(),
                    v -> aimProcessor.setS(v),
                    tvS, "%d"
            ));
        }

        // P - 灵敏度 (0~100)
        tvP = findViewById(R.id.tv_p_value);
        seekP = findViewById(R.id.seek_p);
        if (seekP != null && tvP != null) {
            seekP.setMax(100);
            seekP.setProgress(clamp(aimProcessor.getP(), 0, 100));
            tvP.setText(String.valueOf(aimProcessor.getP()));
            seekP.setOnSeekBarChangeListener(makeSeekListener(
                    () -> aimProcessor.getP(),
                    v -> aimProcessor.setP(v),
                    tvP, "%d"
            ));
        }
    }

    /**
     * 工厂方法：创建 SeekBar 监听
     */
    private SeekBar.OnSeekBarChangeListener makeSeekListener(
            IntSupplier getter, IntConsumer setter, TextView tv, String fmt) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (setter != null) setter.accept(p);
                if (tv != null) tv.setText(String.format(fmt, p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        };
    }

    // ===== 重置按钮 =====
    private void initResetButtons() {
        btnResetV = findViewById(R.id.btn_reset_v);
        btnResetS = findViewById(R.id.btn_reset_s);
        btnResetP = findViewById(R.id.btn_reset_p);

        if (btnResetV != null) btnResetV.setOnClickListener(v -> { if (seekV != null) seekV.setProgress(DEFAULT_V); });
        if (btnResetS != null) btnResetS.setOnClickListener(v -> { if (seekS != null) seekS.setProgress(DEFAULT_S); });
        if (btnResetP != null) btnResetP.setOnClickListener(v -> { if (seekP != null) seekP.setProgress(DEFAULT_P); });
    }

    /**
     * 切换方案后刷新 UI
     */
    private void updateSeekBarUI() {
        if (seekV != null) seekV.setProgress(aimProcessor.getV());
        if (seekS != null) seekS.setProgress(aimProcessor.getS());
        if (seekP != null) seekP.setProgress(aimProcessor.getP());
        if (tvV != null) tvV.setText(String.valueOf(aimProcessor.getV()));
        if (tvS != null) tvS.setText(String.valueOf(aimProcessor.getS()));
        if (tvP != null) tvP.setText(String.valueOf(aimProcessor.getP()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aimProcessor != null) aimProcessor.release();
    }

    // ===== 函数式接口（兼容 Java 8） =====
    interface IntSupplier { int get(); }
    interface IntConsumer { void accept(int v); }

    private int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }
}
