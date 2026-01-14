package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.fly.station.prorocol.bean.Throughput8030;

public class V4PassthroughActivity extends AppCompatActivity {

    private static final String TAG = "V4Passthrough";

    private ActivityV4PassthroughBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    // Auto send related
    private Handler autoSendHandler;
    private Runnable autoSendRunnable;
    private boolean isAutoSending = false;

    // Auto send configuration
    private boolean autoSendEnabled = false;
    private int autoSendFrequency = 1; // Hz
    private int autoSendBytes = 50;

    // Display mode configuration
    private boolean displayLengthOnly = false;

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

        // 设置显示模式开关监听器
        binding.switchDisplayMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            displayLengthOnly = isChecked;
            updateDisplayStatusDisplay(isChecked);
        });

        // 初始化状态显示
        updateLogStatusDisplay(binding.switchLogMode.isChecked());
        updateDisplayStatusDisplay(binding.switchDisplayMode.isChecked());

        // 初始化自动发送
        initAutoSend();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
        // 停止自动发送
        stopAutoSend();
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

    /**
     * 更新显示模式状态显示
     */
    private void updateDisplayStatusDisplay(boolean isLengthOnly) {
        if (isLengthOnly) {
            binding.tvDisplayStatus.setText("Length only");
            binding.tvDisplayStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        } else {
            binding.tvDisplayStatus.setText("Full content");
            binding.tvDisplayStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
        }
    }

    /**
     * 初始化自动发送功能
     */
    private void initAutoSend() {
        // 初始化Handler
        autoSendHandler = new Handler(Looper.getMainLooper());
        autoSendRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAutoSending) {
                    sendAutoData();
                    // 计算下一次发送的时间间隔
                    int frequency = getAutoSendFrequency();
                    long delayMillis = 1000 / Math.max(1, frequency); // 转换为毫秒，至少1Hz
                    autoSendHandler.postDelayed(this, delayMillis);
                }
            }
        };

        // 设置开关监听器
        binding.switchAutoSend.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoSendEnabled = isChecked;

            if (isChecked) {
                startAutoSend();
            } else {
                stopAutoSend();
            }
        });

        // 设置UI初始状态
        binding.switchAutoSend.setChecked(autoSendEnabled);
        binding.etAutoSendFrequency.setText(String.valueOf(autoSendFrequency));
        binding.etAutoSendBytes.setText(String.valueOf(autoSendBytes));
    }

    /**
     * 开始自动发送
     */
    private void startAutoSend() {
        if (!isAutoSending) {
            isAutoSending = true;
            // 立即发送一次，然后开始定时发送
            sendAutoData();
            int frequency = getAutoSendFrequency();
            long delayMillis = 1000 / Math.max(1, frequency);
            autoSendHandler.postDelayed(autoSendRunnable, delayMillis);
        }
    }

    /**
     * 停止自动发送
     */
    private void stopAutoSend() {
        isAutoSending = false;
        if (autoSendHandler != null) {
            autoSendHandler.removeCallbacks(autoSendRunnable);
        }
    }

    /**
     * 发送自动数据
     */
    private void sendAutoData() {
        int bytesCount = getAutoSendBytes();
        byte[] data = new byte[bytesCount];
        // 填充0xaa数据
        for (int i = 0; i < bytesCount; i++) {
            data[i] = (byte) 0xaa;
        }

        // 在后台线程发送数据
        new Thread(() -> {
            protocolHelper.ar8030WritePassthroughData(0, data, bytesCount);
        }).start();
    }

    /**
     * 获取自动发送频率
     */
    private int getAutoSendFrequency() {
        try {
            int frequency = Integer.parseInt(binding.etAutoSendFrequency.getText().toString());
            autoSendFrequency = frequency; // 保存到内部变量
            return Math.max(1, frequency); // 最小1Hz
        } catch (NumberFormatException e) {
            return 1; // 默认1Hz
        }
    }

    /**
     * 获取自动发送字节数量
     */
    private int getAutoSendBytes() {
        try {
            int bytes = Integer.parseInt(binding.etAutoSendBytes.getText().toString());
            autoSendBytes = bytes; // 保存到内部变量
            return Math.max(1, bytes); // 最小1字节
        } catch (NumberFormatException e) {
            return 50; // 默认50字节
        }
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof PassthroughData8030 passthroughData) {
                final String logMessage;
                if (displayLengthOnly) {
                    // Length only mode: show only the length
                    logMessage = "received " + passthroughData.length + " bytes";
                } else {
                    // Full content mode: show complete data
                    final StringBuilder stringBuilder = new StringBuilder(passthroughData.length * 3);
                    for (int i = 0; i < passthroughData.length; i++) {
                        stringBuilder.append(String.format("%02X ", passthroughData.data[i]));
                    }
                    logMessage = "received " + passthroughData.length + " bytes:\n" + stringBuilder;
                }

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
        public int onWrite(byte[] bytes) {
            return 0;
        }

        @Override
        public void onPairOperated(DEVICE_TYPE deviceType, int slot, boolean isStart) {

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
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {

        }
    };
}