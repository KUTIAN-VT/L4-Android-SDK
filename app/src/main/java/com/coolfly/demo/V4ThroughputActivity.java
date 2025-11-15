package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4ThroughputBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.Throughput8030;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class V4ThroughputActivity extends AppCompatActivity {

    private ActivityV4ThroughputBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4ThroughputBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetThroughput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int slot = (int) binding.spSlot.getSelectedItemId();
                boolean isRemote = binding.swIsRemote.isChecked();
                protocolHelper.ar8030GetThroughput(slot, isRemote);
                appendStatus("Requesting throughput for slot " + slot + " (remote: " + isRemote + ")");
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
        public void onReady(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            // 处理其他packet类型，如果需要的话
        }

        @Override
        public int onWrite(byte[] bytes) {
            return 0;
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
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // AR8030 throughput data received
            runOnUiThread(() -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Throughput Data Received:\n");
                sb.append("TX Physical Throughput: ").append(throughput.txPhyThroughput).append(" bps\n");
                sb.append("TX Real Throughput: ").append(throughput.txRealThroughput).append(" bps\n");
                sb.append("RX Physical Throughput: ").append(throughput.rxPhyThroughput).append(" bps\n");
                sb.append("RX Real Throughput: ").append(throughput.rxRealThroughput).append(" bps\n");

                binding.tvThroughputResult.setText(sb.toString());
                appendStatus("Received throughput data");
            });
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, com.fly.station.prorocol.RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {

        }
    };
}
