package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4PassthroughBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.station.mcu.McuManager;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.PassthroughData8030;

public class V4PassthroughActivity extends AppCompatActivity {

    private ActivityV4PassthroughBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4PassthroughBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvP401PortPassthrough.setText(McuManager.DEVICE_PATH);
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
                protocolHelper.ar8030OpenPassthrough();
            }
        });

        binding.tvDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ClosePassthrough();
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
                            protocolHelper.ar8030WritePassthroughData(bytesToWrite, bytesToWrite.length);
                        }
                    }).start();
                } else {
                    binding.etWrite.setError("Input cannot be empty");
                }
            }
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof PassthroughData8030) {
                PassthroughData8030 usbPacket = (PassthroughData8030) baseFlyPacket;
                final StringBuilder stringBuilder = new StringBuilder(usbPacket.length * 3);
                for (int i = 0; i < usbPacket.length; i++) {
                    stringBuilder.append(String.format("%02X ", usbPacket.data[i]));
                }
                runOnUiThread(() -> {
                    binding.tvRead.setText(stringBuilder.toString());
                });
            }
        }

        @Override
        public void onWrite(byte[] bytes) {

        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType) {

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
    };
}