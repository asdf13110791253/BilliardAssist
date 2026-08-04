package com.example.billiardassist.ui;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.billiardassist.R;
import com.example.billiardassist.service.FloatingService;

/**
 * 设置面板
 * 对应截图：图11、图12
 *
 * 功能：
 * - 桌布选择（桌布1/2/3）
 * - 反射方案（反射补偿 / 镜像反射）
 * - 颜色选择（10种颜色）
 * - 补偿比例滑块
 * - 手动翻袋库数
 * - 辅助线粗细
 * - 显示蚂蚁线开关
 * - 吸附最近球开关
 * - 退出应用按钮
 */
public class SettingsActivity extends AppCompatActivity {

    // 10种可选颜色
    private static final int[] COLORS = {
            Color.BLACK, Color.WHITE, Color.GRAY, Color.RED,
            Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN,
            Color.MAGENTA, 0xFFFF8C00 // Orange
    };

    private SharedPreferences prefs;
    private int selectedColor = Color.GREEN;
    private int compensationRatio = 18; // 0.18 存为整数 18
    private int bankCount = 2;
    private float lineThickness = 5.0f;
    private boolean antLineEnabled = false;
    private boolean adsorbEnabled = true;
    private int reflectMode = 0; // 0=反射补偿, 1=镜像反射
    private int tableCloth = 1; // 0/1/2 对应桌布1/2/3

    // UI 控件
    private TextView tvCompValue, tvBankValue, tvThicknessValue;
    private SeekBar seekComp, seekBank, seekThickness;
    private Switch switchAntLine, switchAdsorb;
    private RadioGroup rgTableCloth, rgReflect;
    private Button btnExit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        loadSettings();

        initTableClothUI();
        initReflectUI();
        initColorButtons();
        initSeekBars();
        initSwitches();
        initExitButton();
    }

    /**
     * 加载已保存的设置
     */
    private void loadSettings() {
        selectedColor = prefs.getInt("line_color", Color.GREEN);
        compensationRatio = prefs.getInt("comp_ratio", 18);
        bankCount = prefs.getInt("bank_count", 2);
        lineThickness = prefs.getFloat("line_thickness", 5.0f);
        antLineEnabled = prefs.getBoolean("ant_line", false);
        adsorbEnabled = prefs.getBoolean("adsorb", true);
        reflectMode = prefs.getInt("reflect_mode", 0);
        tableCloth = prefs.getInt("table_cloth", 1);
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        prefs.edit()
                .putInt("line_color", selectedColor)
                .putInt("comp_ratio", compensationRatio)
                .putInt("bank_count", bankCount)
                .putFloat("line_thickness", lineThickness)
                .putBoolean("ant_line", antLineEnabled)
                .putBoolean("adsorb", adsorbEnabled)
                .putInt("reflect_mode", reflectMode)
                .putInt("table_cloth", tableCloth)
                .apply();
    }

    // ===== 桌布选择 =====
    private void initTableClothUI() {
        rgTableCloth = findViewById(R.id.rg_table_cloth);
        ((RadioButton) rgTableCloth.getChildAt(tableCloth)).setChecked(true);
        rgTableCloth.setOnCheckedChangeListener((group, checkedId) -> {
            int index = group.indexOfChild(group.findViewById(checkedId));
            tableCloth = index;
            saveSettings();
        });
    }

    // ===== 反射方案 =====
    private void initReflectUI() {
        rgReflect = findViewById(R.id.rg_reflect);
        ((RadioButton) rgReflect.getChildAt(reflectMode)).setChecked(true);
        rgReflect.setOnCheckedChangeListener((group, checkedId) -> {
            int index = group.indexOfChild(group.findViewById(checkedId));
            reflectMode = index;
            saveSettings();
        });
    }

    // ===== 颜色选择（10个色块） =====
    private void initColorButtons() {
        int[] btnIds = {
                R.id.btn_color_0, R.id.btn_color_1, R.id.btn_color_2, R.id.btn_color_3,
                R.id.btn_color_4, R.id.btn_color_5, R.id.btn_color_6, R.id.btn_color_7,
                R.id.btn_color_8, R.id.btn_color_9
        };
        for (int i = 0; i < btnIds.length; i++) {
            Button btn = findViewById(btnIds[i]);
            btn.setBackgroundColor(COLORS[i]);
            final int color = COLORS[i];
            btn.setOnClickListener(v -> {
                selectedColor = color;
                saveSettings();
                Toast.makeText(this, "颜色已切换", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // ===== 三个滑块 =====
    private void initSeekBars() {
        // 补偿比例 0~50，显示 0.00~0.50
        tvCompValue = findViewById(R.id.tv_comp_value);
        seekComp = findViewById(R.id.seek_comp);
        seekComp.setMax(50);
        seekComp.setProgress(compensationRatio);
        tvCompValue.setText(String.format("%.2f", compensationRatio / 100.0));
        seekComp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                compensationRatio = p;
                tvCompValue.setText(String.format("%.2f", p / 100.0));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { saveSettings(); }
        });

        // 翻袋库数 1~5
        tvBankValue = findViewById(R.id.tv_bank_value);
        seekBank = findViewById(R.id.seek_bank);
        seekBank.setMax(5);
        seekBank.setProgress(bankCount);
        tvBankValue.setText(String.valueOf(bankCount));
        seekBank.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                bankCount = Math.max(1, p);
                tvBankValue.setText(String.valueOf(bankCount));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { saveSettings(); }
        });

        // 线粗细 1~20，步长 0.5
        tvThicknessValue = findViewById(R.id.tv_thickness_value);
        seekThickness = findViewById(R.id.seek_thickness);
        seekThickness.setMax(40);
        seekThickness.setProgress((int) (lineThickness * 2));
        tvThicknessValue.setText(String.format("%.1f", lineThickness));
        seekThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                lineThickness = p / 2.0f;
                tvThicknessValue.setText(String.format("%.1f", lineThickness));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { saveSettings(); }
        });
    }

    // ===== 两个开关 =====
    private void initSwitches() {
        switchAntLine = findViewById(R.id.switch_ant_line);
        switchAdsorb = findViewById(R.id.switch_adsorb);

        switchAntLine.setChecked(antLineEnabled);
        switchAdsorb.setChecked(adsorbEnabled);

        switchAntLine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            antLineEnabled = isChecked;
            saveSettings();
        });
        switchAdsorb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adsorbEnabled = isChecked;
            saveSettings();
        });
    }

    // ===== 退出应用 =====
    private void initExitButton() {
        btnExit = findViewById(R.id.btn_exit_app);
        btnExit.setOnClickListener(v -> {
            // 停止所有服务
            stopService(new Intent(this, com.example.billiardassist.service.FloatingService.class));
            stopService(new Intent(this, com.example.billiardassist.service.CaptureService.class));
            finishAffinity(); // 关闭所有 Activity
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveSettings();
    }
}
