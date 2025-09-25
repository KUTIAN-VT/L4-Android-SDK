package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4BandwidthBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.Throughput8030;

public class V4BandwidthActivity extends AppCompatActivity {

    private ActivityV4BandwidthBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4BandwidthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvSetDev2Ap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etRxBandwidth.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "rx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (binding.etTxBandwidth.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "tx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                protocolHelper.ar8030SetBandwidth(Integer.parseInt(binding.etRxBandwidth.getText().toString()), Integer.parseInt(binding.etTxBandwidth.getText().toString()));
            }
        });

        binding.tvResetDev2Ap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ResetBandwidth();
            }
        });

        binding.tvSetAp2Dev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etRxFramechange.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "rx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (binding.etTxFramechange.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "tx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                protocolHelper.ar8030SetFrameChange(Integer.parseInt(binding.etRxFramechange.getText().toString()), Integer.parseInt(binding.etTxFramechange.getText().toString()));
            }
        });

        binding.tvResetAp2Dev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ResetFrameChange();
            }
        });

        binding.tvSetAp2DevKeepBuffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030SetFrameChangeKeepingBuffer();
            }
        });

        binding.tvResetAp2DevKeepBuffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ResetFrameChangeKeepingBuffer();
            }
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    private void appendStatus(String message) {
        runOnUiThread(() -> {
            // 获取当前时间并格式化
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String logMessage = "[" + timestamp + "] " + message;

            String currentText = binding.tvStatus.getText().toString();
            binding.tvStatus.setText(currentText + "\n" + logMessage);

            // 滚动到底部
            binding.svStatus.post(() -> binding.svStatus.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {

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
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
            switch (radioType) {
                case BANDWIDTH:
                    appendStatus("set bandwidth: " + (isSuccess ? "success" : "fail") + " - " + errMessage);
                    break;
                case FRAME_CHANGE:
                    appendStatus("frame change: " + (isSuccess ? "success" : "fail") + " - " + errMessage);
                    break;
                default:
                    // Handle other radio types that might be added for keep buffer operations
                    appendStatus("radio operation: " + radioType + " - " + (isSuccess ? "success" : "fail") + " - " + errMessage);
                    break;
            }
        }
    };
}