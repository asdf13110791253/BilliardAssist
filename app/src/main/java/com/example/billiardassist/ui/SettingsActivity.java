package com.example.billiardassist.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.billiardassist.App;
import com.example.billiardassist.R;

public class SettingsActivity extends AppCompatActivity {

    private RadioGroup rgTableCloth;
    private RadioButton rbCloth1, rbCloth2, rbCloth3;

    private RadioGroup rgReflectMode;
    private RadioButton rbCompensation, rbMirror;

    private SeekBar seekCompRatio;
    private TextView tvCompRatio;

    private SeekBar seekBankCount;
    private TextView tvBankCount;

    private SeekBar seekLineThickness;
    private TextView tvLineThickness;

    private Switch swAntLine;
    private Switch swAdsorb;

    private Button btnExitApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        rgTableCloth = findViewById(R.id.rg_table_cloth);
        rbCloth1 = findViewById(R.id.rb_cloth_1);
        rbCloth2 = findViewById(R.id.rb_cloth_2);
        rbCloth3 = findViewById(R.id.rb_cloth_3);

        rgReflectMode = findViewById(R.id.rg_reflect_mode);
        rbCompensation = findViewById(R.id.rb_compensation);
        rbMirror = findViewById(R.id.rb_mirror);

        seekCompRatio = findViewById(R.id.seek_comp_ratio);
        tvCompRatio = findViewById(R.id.tv_comp_ratio);

        seekBankCount = findViewById(R.id.seek_bank_count);
        tvBankCount = findViewById(R.id.tv_bank_count);

        seekLineThickness = findViewById(R.id.seek_line_thickness);
        tvLineThickness = findViewById(R.id.tv_line_thickness);

        swAntLine = findViewById(R.id.sw_ant_line);
        swAdsorb = findViewById(R.id.sw_adsorb);

        btnExitApp = findViewById(R.id.btn_exit_app);
    }

    private void loadSettings() {
        App app = App.getInstance();

        int clothType = app.getTableClothType();
        switch (clothType) {
            case 0: rbCloth1.setChecked(true); break;
            case 1: rbCloth2.setChecked(true); break;
            case 2: rbCloth3.setChecked(true); break;
        }

        int reflectMode = app.getReflectMode();
        if (reflectMode == 0) {
            rbCompensation.setChecked(true);
        } else {
            rbMirror.setChecked(true);
        }

        double ratio = app.getCompRatio();
        int ratioInt = (int) (ratio * 100);
        seekCompRatio.setProgress(ratioInt);
        tvCompRatio.setText(String.format("%.2f", ratio));

        int bankCount = app.getBankCount();
        seekBankCount.setProgress(bankCount);
        tvBankCount.setText(String.valueOf(bankCount));

        float thickness = app.getLineThickness();
        int thicknessInt = (int) thickness;
        seekLineThickness.setProgress(thicknessInt);
        tvLineThickness.setText(String.format("%.1f", thickness));

        swAntLine.setChecked(app.isAntLineEnabled());
        swAdsorb.setChecked(app.isAdsorbEnabled());
    }

    private void setupListeners() {
        rgTableCloth.setOnCheckedChangeListener((group, checkedId) -> {
            int type = 0;
            if (checkedId == R.id.rb_cloth_1) type = 0;
            else if (checkedId == R.id.rb_cloth_2) type = 1;
            else if (checkedId == R.id.rb_cloth_3) type = 2;
            App.getInstance().setTableClothType(type);
            Toast.makeText(this, "已切换桌布", Toast.LENGTH_SHORT).show();
        });

        rgReflectMode.setOnCheckedChangeListener((group, checkedId) -> {
            int mode = (checkedId == R.id.rb_compensation) ? 0 : 1;
            App.getInstance().setReflectMode(mode);
            String name = (mode == 0) ? "反射补偿" : "镜像反射";
            Toast.makeText(this, "已切换到: " + name, Toast.LENGTH_SHORT).show();
        });

        seekCompRatio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    double value = progress / 100.0;
                    tvCompRatio.setText(String.format("%.2f", value));
                    App.getInstance().setCompRatio(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBankCount.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    tvBankCount.setText(String.valueOf(progress));
                    App.getInstance().setBankCount(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekLineThickness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && progress > 0) {
                    float value = progress;
                    tvLineThickness.setText(String.format("%.1f", value));
                    App.getInstance().setLineThickness(value);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        swAntLine.setOnCheckedChangeListener((buttonView, isChecked) -> {
            App.getInstance().setAntLineEnabled(isChecked);
            Toast.makeText(this, isChecked ? "蚂蚁线已开启" : "蚂蚁线已关闭", Toast.LENGTH_SHORT).show();
        });

        swAdsorb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            App.getInstance().setAdsorbEnabled(isChecked);
            Toast.makeText(this, isChecked ? "吸附已开启" : "吸附已关闭", Toast.LENGTH_SHORT).show();
        });

        btnExitApp.setOnClickListener(v -> {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        });
    }
}
