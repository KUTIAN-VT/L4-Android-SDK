package com.coolfly.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.GetStatus8030;
import com.fly.station.prorocol.bean.Throughput8030;

public class V4StatusActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    private TextView tvMode;
    private TextView tvStatus;
    private CheckBox cb14Ghz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_status);

        Button btnGetStatus = findViewById(R.id.btn_get_status);
        tvMode = findViewById(R.id.tv_mode);
        tvStatus = findViewById(R.id.tv_status);
        cb14Ghz = findViewById(R.id.cb_14ghz);

        btnGetStatus.setOnClickListener(v -> {
            try {
                protocolHelper.ar8030GetStatus();
            } catch (Throwable t) {
                tvStatus.setText("call ar8030GetStatus failed: " + t.getMessage());
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
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            // 仅处理新回调 GetStatus8030
            if (baseFlyPacket instanceof GetStatus8030) {
                tvStatus.setText(baseFlyPacket.toString());
                // 解析 FrameMode
                boolean is14Ghz = cb14Ghz.isChecked();
                ProtocolHelper.AR8030FrameMode frameMode = ProtocolHelper.parseFrameModeFromStatus((GetStatus8030) baseFlyPacket, is14Ghz);
                tvMode.setText("Mode: " + frameMode);
            }
        }

        @Override
        public int onWrite(byte[] bytes) {
            return 0;}

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) { }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) { }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int i) { }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int i) { }

        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) { }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) { }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) { }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) { }

        @Override
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {}

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) { }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {

        }
    };
}


