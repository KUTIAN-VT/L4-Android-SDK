package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4McsBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.GetMcs8030;
import com.fly.station.prorocol.bean.Throughput8030;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class V4McsActivity extends AppCompatActivity {

    private ActivityV4McsBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4McsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetMcs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int dir = (int) binding.spDir.getSelectedItemId();
                int slot = (int) binding.spSlot.getSelectedItemId();
                protocolHelper.ar8030GetMcs(dir, slot);
                appendStatus("Requesting MCS for dir " + dir + ", slot " + slot);
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
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String logMessage = "[" + timestamp + "] " + message;
            String currentText = binding.tvStatus.getText().toString();
            binding.tvStatus.setText(currentText + "\n" + logMessage);
            binding.svStatus.post(() -> binding.svStatus.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {}

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof GetMcs8030) {
                GetMcs8030 mcs = (GetMcs8030) baseFlyPacket;
                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MCS: ").append(mcs.mcs).append("\n");
                    sb.append("Throughput: ").append(mcs.throughput).append(" kbps\n");
                    binding.tvMcsResult.setText(sb.toString());
                    appendStatus("Received MCS data");
                });
            }
        }

        @Override
        public int onWrite(byte[] bytes) { return 0; }
        @Override
        public void onPairOperated(DEVICE_TYPE deviceType, int slot, boolean isStart) {}
        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) {}
        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) {}
        @Override
        public void onLinked(DEVICE_TYPE deviceType, int i) {}
        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int i) {}
        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) {}
        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {}
        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {}
        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {}
        @Override
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {}
        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {}
        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {}
    };
}
