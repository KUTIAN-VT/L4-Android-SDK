package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4PeerQualityBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.PeerQuality8030;
import com.fly.station.prorocol.bean.Quality8030;
import com.fly.station.prorocol.bean.Throughput8030;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class V4PeerQualityActivity extends AppCompatActivity {

    private ActivityV4PeerQualityBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4PeerQualityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetPeerQuality.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030GetPeerQuality();
                appendResult("Requesting peer quality");
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
            if (baseFlyPacket instanceof PeerQuality8030) {
                PeerQuality8030 pq = (PeerQuality8030) baseFlyPacket;
                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Peer Quality Data:\n");
                    if (pq.qualities != null) {
                        for (int i = 0; i < pq.qualities.length; i++) {
                            Quality8030 q = pq.qualities[i];
                            if (q.snr == 0 && q.ldpcErr == 0 && q.ldpcNum == 0 && q.gainA == 0 && q.gainB == 0) {
                                continue;
                            }
                            sb.append("Slot ").append(i).append(": ");
                            sb.append("SNR=").append(q.snr);
                            sb.append(" LDPC_ERR=").append(q.ldpcErr);
                            sb.append("/").append(q.ldpcNum);
                            sb.append(" GainA=").append(q.gainA);
                            sb.append(" GainB=").append(q.gainB);
                            sb.append("\n");
                        }
                    }
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
