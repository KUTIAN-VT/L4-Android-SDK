package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4CurPowerBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.CurPower8030;
import com.fly.station.prorocol.bean.Throughput8030;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class V4CurPowerActivity extends AppCompatActivity {

    private ActivityV4CurPowerBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4CurPowerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetCurPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int slot = (int) binding.spSlot.getSelectedItemId();
                boolean isRemote = binding.swIsRemote.isChecked();
                protocolHelper.ar8030GetCurPower(slot, isRemote);
                appendResult("Requesting current power for slot " + slot + " (remote: " + isRemote + ")");
            }
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    private void appendResult(String message) {
        runOnUiThread(() -> {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());
            String logMessage = "[" + timestamp + "] " + message;
            String currentText = binding.tvResult.getText().toString();
            binding.tvResult.setText(currentText + "\n" + logMessage);
            binding.svStatus.post(() -> binding.svStatus.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {}

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof CurPower8030) {
                CurPower8030 cp = (CurPower8030) baseFlyPacket;
                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Current Power:\n");
                    sb.append("User: ").append(cp.usr).append("\n");
                    sb.append("Power: ").append(cp.pwr).append("\n");
                    appendResult(sb.toString());
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
