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
 * 对应截图：图13
 *
 * 功能：
 * - 8 种识别方案单选（零一到零八）
 * - V（亮度阈值）滑块 0~255
 * - S（圆白度）滑块 0~100
 * - P（圆形检测灵敏度）滑块 0~100
 * - 每个参数有"重置"按钮
 */
public class AimSchemeActivity extends AppCompatActivity {

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
        int current = aimProcessor.getScheme();
        ((RadioButton) rgScheme.getChildAt(current)).setChecked(true);

        rgScheme.setOnCheckedChangeListener((group, checkedId) -> {
            int index = group.indexOfChild(group.findViewById(checkedId));
            aimProcessor.setScheme(index);
            // 切换方案后更新滑块显示
            updateSeekBarUI();
            Toast.makeText(this, "已切换到方案 " + (index + 1), Toast.LENGTH_SHORT).show();
        });
    }

    // ===== 三个参数滑块 =====
    private void initSeekBars() {
        // V - 亮度 (0~255)
        tvV = findViewById(R.id.tv_v_value);
        seekV = findViewById(R.id.seek_v);
        seekV.setMax(255);
        seekV.setProgress(aimProcessor.getV());
        tvV.setText(String.valueOf(aimProcessor.getV()));
        seekV.setOnSeekBarChangeListener(makeSeekListener(
                () -> aimProcessor.getV(),
                v -> aimProcessor.setV(v),
                tvV, "%d"
        ));

        // S - 圆白度 (0~100)
        tvS = findViewById(R.id.tv_s_value);
        seekS = findViewById(R.id.seek_s);
        seekS.setMax(100);
        seekS.setProgress(aimProcessor.getS());
        tvS.setText(String.valueOf(aimProcessor.getS()));
        seekS.setOnSeekBarChangeListener(makeSeekListener(
                () -> aimProcessor.getS(),
                v -> aimProcessor.setS(v),
                tvS, "%d"
        ));

        // P - 灵敏度 (0~100)
        tvP = findViewById(R.id.tv_p_value);
        seekP = findViewById(R.id.seek_p);
        seekP.setMax(100);
        seekP.setProgress(aimProcessor.getP());
        tvP.setText(String.valueOf(aimProcessor.getP()));
        seekP.setOnSeekBarChangeListener(makeSeekListener(
                () -> aimProcessor.getP(),
                v -> aimProcessor.setP(v),
                tvP, "%d"
        ));
    }

    /**
     * 工厂方法：创建 SeekBar 监听
     */
    private SeekBar.OnSeekBarChangeListener makeSeekListener(
            IntSupplier getter, IntConsumer setter, TextView tv, String fmt) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                setter.accept(p);
                tv.setText(String.format(fmt, p));
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

        btnResetV.setOnClickListener(v -> { seekV.setProgress(DEFAULT_V); });
        btnResetS.setOnClickListener(v -> { seekS.setProgress(DEFAULT_S); });
        btnResetP.setOnClickListener(v -> { seekP.setProgress(DEFAULT_P); });
    }

    /**
     * 切换方案后刷新 UI
     */
    private void updateSeekBarUI() {
        seekV.setProgress(aimProcessor.getV());
        seekS.setProgress(aimProcessor.getS());
        seekP.setProgress(aimProcessor.getP());
        tvV.setText(String.valueOf(aimProcessor.getV()));
        tvS.setText(String.valueOf(aimProcessor.getS()));
        tvP.setText(String.valueOf(aimProcessor.getP()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (aimProcessor != null) aimProcessor.release();
    }

    // ===== 函数式接口（兼容 Java 8） =====
    interface IntSupplier { int get(); }
    interface IntConsumer { void accept(int v); }
}
