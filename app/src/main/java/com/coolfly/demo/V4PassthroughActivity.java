package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4PassthroughBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.PassthroughData8030;

public class V4PassthroughActivity extends AppCompatActivity {

    private static final String TAG = "V4Passthrough";

    private ActivityV4PassthroughBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4PassthroughBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvP401PortPassthrough.setText("" + PreferenceActivity.preferenceObject.p401_port_passthrough);
        binding.tvP401PortPassthrough.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] ports = getResources().getStringArray(R.array.p401ports_passthrough_value);
                new AlertDialog.Builder(V4PassthroughActivity.this)
                        .setTitle("passthrough port")
                        .setItems(ports, (dialog, which) -> {
                            int port = Integer.parseInt(ports[which]);
                            ProtocolHelper.ar8030SetPortPassthrough(port);
                            binding.tvP401PortPassthrough.setText(ports[which]);
                            PreferenceActivity.preferenceObject.p401_port_passthrough = port;
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.tvConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open passthrough connection for slot 0
                protocolHelper.ar8030OpenPassthrough(0);
            }
        });

        binding.tvDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Close passthrough connection for slot 0
                protocolHelper.ar8030ClosePassthrough(0);
            }
        });

        binding.tvWrite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String strToWrite = binding.etWrite.getText().toString();
                if (!TextUtils.isEmpty(strToWrite)) {
                    byte[] bytesToWrite = strToWrite.getBytes();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            // Write passthrough data to slot 0
                            protocolHelper.ar8030WritePassthroughData(0, bytesToWrite, bytesToWrite.length);
                        }
                    }).start();
                } else {
                    binding.etWrite.setError("Input cannot be empty");
                }
            }
        });

        protocolHelper.addListener(protocolListener);

        // 设置开关状态监听器
        binding.switchLogMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateLogStatusDisplay(isChecked);
        });

        // 初始化状态显示
        updateLogStatusDisplay(binding.switchLogMode.isChecked());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    /**
     * 更新日志模式状态显示
     */
    private void updateLogStatusDisplay(boolean isLogMode) {
        if (isLogMode) {
            binding.tvLogStatus.setText("Logcat");
            binding.tvLogStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            binding.tvLogStatus.setText("UI print");
            binding.tvLogStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof PassthroughData8030 passthroughData) {
                final StringBuilder stringBuilder = new StringBuilder(passthroughData.length * 3);
                for (int i = 0; i < passthroughData.length; i++) {
                    stringBuilder.append(String.format("%02X ", passthroughData.data[i]));
                }
                final String logMessage = "received " + passthroughData.length + " bytes:\n" + stringBuilder;

                if (binding.switchLogMode.isChecked()) {
                    // Log mode: print to logcat
                    Log.d(TAG, logMessage);
                } else {
                    // UI mode: display on screen
                    runOnUiThread(() -> {
                        binding.tvRead.setText(logMessage);
                    });
                }
            }
        }

        @Override
        public void onWrite(byte[] bytes) {

        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }
    };
}