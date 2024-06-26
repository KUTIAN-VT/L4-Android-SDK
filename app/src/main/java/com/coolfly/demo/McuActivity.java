package com.coolfly.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityMcuBinding;
import com.coolfly.demo.utils.WidgetUtils;
import com.coolfly.station.mcu.McuManager;
import com.coolfly.station.mcu.McuOtaHelper;
import com.coolfly.station.mcu.McuPacket;
import com.coolfly.station.mcu.entity.ActiveState;
import com.coolfly.station.mcu.entity.HeartBeat;
import com.coolfly.station.mcu.entity.Temperature;
import com.coolfly.station.mcu.entity.Version;
import com.coolfly.station.prorocol.CoolFly;
import com.coolfly.station.wheel.Wheel;
import com.coolfly.station.wheel.WheelListener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class McuActivity extends AppCompatActivity {

    private ActivityMcuBinding binding;

    private McuManager mcuManager;
    private McuOtaHelper mcuOtaHelper;

    private final int REQ_OTA_MCU = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMcuBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // MCU
        mcuManager = McuManager.getInstance();
        mcuManager.addListener(mcuListener);

        // MCU OTA
        mcuOtaHelper = McuOtaHelper.getInstance();
        mcuOtaHelper.setMcuOTAListener(mcuOTAListener);

        // Show Mcu Log in logcat. Disable in production environment.
        binding.swLog.setChecked(McuManager.isShowLog);
        binding.swLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                McuManager.setIsShowLog(isChecked);
            }
        });

        binding.tvConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.onLine();
            }
        });

        binding.tvDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.offLine();
            }
        });

        binding.tvMcuReboot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.writePacket(McuPacket.createWriteRebootMCUPacket());
            }
        });
        binding.tvMcuVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.writePacket(McuPacket.createWriteFetchVersionPacket());
            }
        });
        binding.tvMcuReadHeartBeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.writePacket(McuPacket.createReadHeartBeatPacket());
            }
        });
        binding.swMcuBuzzer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mcuManager.writePacket(McuPacket.createWriteBuzzerSwitchPacket(isChecked));
            }
        });
        binding.tvMcuBuzzerTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(McuActivity.this)
                        .setTitle("蜂鸣器响次数")
                        .setItems(new String[]{"1", "2"}, (dialog, which) -> {
                            McuPacket packet = McuPacket.createWriteBuzzerTimesPacket(which + 1);
                            mcuManager.writePacket(packet);
                        }).create().show();
            }
        });
        binding.swMcuStatus.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mcuManager.writePacket(McuPacket.createWriteCalibrateStatusPacket(isChecked));
            }
        });
        binding.swMcuFan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mcuManager.writePacket(McuPacket.createWriteFanPacket(isChecked, binding.spMcuFanSpeed.getSelectedItemPosition()));
            }
        });

        // 风扇转速，0-100
        String[] fanSpeedArr = new String[101];
        for (int i = 0; i < fanSpeedArr.length; i++) {
            fanSpeedArr[i] = i + "";
        }
        ArrayAdapter<String> fanSpeedAdapter = new ArrayAdapter<String>(this, R.layout.item_select, fanSpeedArr);
        fanSpeedAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spMcuFanSpeed.setAdapter(fanSpeedAdapter);
        binding.spMcuFanSpeed.setSelection(100);
        binding.spMcuFanSpeed.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mcuManager.writePacket(McuPacket.createWriteFanPacket(binding.swMcuFan.isChecked(), position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Active
        binding.tvMcuReadActiveState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.writePacket(McuPacket.createReadActiveStatePacket());
            }
        });
        binding.tvMcuWriteActiveInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mcuManager.writePacket(McuPacket.createWriteActiveInfoPacket(new ActiveState(null, System.currentTimeMillis(), "ABCDEFGHIJKLMNOPQRSTUVWX")));
            }
        });

        // Wheel
        Wheel.setWheelListener(wheelListener);
        Wheel.start();

        // OTA
        binding.tvMcuOta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mcuOtaHelper.isRunning()) {
                    Toast.makeText(McuActivity.this, "OTA is running", Toast.LENGTH_SHORT).show();
                    return;
                }
                getUpgradeFis(REQ_OTA_MCU);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mcuManager.removeListener(mcuListener);

        Wheel.setWheelListener(null);
        Wheel.stop();
    }

    private final McuManager.McuListener mcuListener = new McuManager.McuListener() {
        @Override
        public void onHeartBeat(HeartBeat heartBeat) {
            binding.tvLog.setText(heartBeat.toString());
            WidgetUtils.setSwitchWithoutListener(binding.swMcuBuzzer, heartBeat.isBuzzOn());
            WidgetUtils.setSwitchWithoutListener(binding.swMcuFan, heartBeat.isFanOn());
            WidgetUtils.setSelectionWithoutCallback(binding.spMcuFanSpeed, heartBeat.getFanSpeed());
        }

        @Override
        public void onShutdownOs() {
            try {
                CoolFly.shutDown();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onVersion(Version version) {
            binding.tvLog.setText(version.toString());
        }

        @Override
        public void onTemperature(Temperature temperature) {
            binding.tvLog.setText(temperature.toString());
        }

        @Override
        public void onActiveState(ActiveState activeState) {
            binding.tvLog.setText(activeState.toString());
        }

        @Override
        public void onACK() {

        }

        @Override
        public void onNACK(String error) {
            binding.tvLog.setText(error);
        }
    };

    private final McuOtaHelper.McuOTAListener mcuOTAListener = new McuOtaHelper.McuOTAListener() {
        @Override
        public void onOTAStart() {
            binding.tvLog.setText("OTA start");
        }

        @Override
        public void onOTAProgress(int progress) {
            binding.tvLog.setText("OTA progress: " + progress + "%");
        }

        @Override
        public void onOTASuccess() {
            binding.tvLog.setText("OTA success");
        }

        @Override
        public void onOTAFail(String error) {
            binding.tvLog.setText("OTA fail: " + error);
        }
    };

    private final WheelListener wheelListener = new WheelListener() {
        @Override
        public void onWheel(int value1, int value2) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.tvWheel1.setText("Wheel 1: " + value1);
                    binding.tvWheel2.setText("Wheel 2: " + value2);
                }
            });
        }
    };

    private void getUpgradeFis(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); //设置bin后缀名
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream fis = getContentResolver().openInputStream(uri);
                if (fis != null) {
                    try {
                        mcuOtaHelper.start(fis);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        binding.tvLog.setText("OTA fail: " + e.getMessage());
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}