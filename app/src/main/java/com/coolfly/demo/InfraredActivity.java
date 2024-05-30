package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityInfraredBinding;
import com.coolfly.station.chuanyun.SensorDevice;
import com.coolfly.station.chuanyun.entity.InfraredConfig;

public class InfraredActivity extends AppCompatActivity {

    private ActivityInfraredBinding binding;

    private SensorDevice sensorDevice;

    private boolean firstEnter = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInfraredBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sensorDevice = SensorDevice.getInstance(this);

        String[] fanSpeedArr = new String[24];
        for (int i = 0; i < fanSpeedArr.length; i++) {
            fanSpeedArr[i] = "      " + i + "      ";
        }
        ArrayAdapter<String> fanSpeedAdapter = new ArrayAdapter<String>(this, R.layout.item_select, fanSpeedArr);
        fanSpeedAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spColor.setAdapter(fanSpeedAdapter);
        binding.spColor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstEnter) {
                    firstEnter = false;
                    return;
                }
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.setColor(position);
                sensorDevice.writeInfraredConfig(infraredConfig);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.sbScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.setScaleValue(seekBar.getProgress() / 100f);
                sensorDevice.writeInfraredConfig(infraredConfig);
            }
        });

        binding.sbLum.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.setLum(seekBar.getProgress());
                sensorDevice.writeInfraredConfig(infraredConfig);
            }
        });

        binding.sbRadio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.setRadio(seekBar.getProgress());
                sensorDevice.writeInfraredConfig(infraredConfig);
            }
        });

        binding.sbSharpness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.setSharpness(seekBar.getProgress());
                sensorDevice.writeInfraredConfig(infraredConfig);
            }
        });
    }

}