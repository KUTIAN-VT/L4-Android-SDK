package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityInfraredBinding;
import com.coolfly.station.chuanyun.SensorDevice;
import com.coolfly.station.chuanyun.entity.Calibrate;
import com.coolfly.station.chuanyun.entity.InfraredConfig;
import com.coolfly.station.chuanyun.entity.PairResponse;
import com.coolfly.station.chuanyun.entity.RFConfig;
import com.coolfly.station.chuanyun.entity.RFConfig2;
import com.coolfly.station.chuanyun.entity.Sbus;
import com.coolfly.station.chuanyun.entity.SbusConfig;
import com.coolfly.station.chuanyun.entity.SbusData;
import com.coolfly.station.chuanyun.entity.Status;
import com.coolfly.station.chuanyun.entity.Version;

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
        sensorDevice.addListener(sensorDeviceListener);

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

        binding.tvReadSn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfraredConfig infraredConfig = new InfraredConfig();
                infraredConfig.sn = new int[]{1,2,3,4,5,6,7};
                sensorDevice.readInfraredConfig(infraredConfig);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorDevice != null) {
            sensorDevice.removeListener(sensorDeviceListener);
        }
    }

    @Keep
    private final SensorDevice.SensorDeviceListener sensorDeviceListener = new SensorDevice.SensorDeviceListener() {
        @Override
        public void onConnected(int i) {

        }

        @Override
        public void onStatus(Status status) {

        }

        @Override
        public void onPairResponse(PairResponse pairResponse) {

        }

        @Override
        public void onSbus(Sbus sbus) {

        }

        @Override
        public void onSbusConfig(SbusConfig sbusConfig) {

        }

        @Override
        public void onSbusData(SbusData sbusData) {

        }

        @Override
        public void onCalibrate(Calibrate calibrate) {

        }

        @Override
        public void onRfConfig(RFConfig rfConfig) {

        }

        @Override
        public void onRfConfig2(RFConfig2 rfConfig2) {

        }

        @Override
        public void onVersion(Version version) {

        }

        @Override
        public void onInfraredConfig(InfraredConfig infraredConfig) {
            binding.tvLog.setText(infraredConfig.toString());
        }
    };
}