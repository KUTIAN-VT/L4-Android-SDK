package com.coolfly.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;

public class V4FreqListActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_freq_list);

        EditText etFreqList = findViewById(R.id.et_freq_list);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSet = findViewById(R.id.btn_set);

        btnSet.setOnClickListener(v -> {
            String text = etFreqList.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "freq list is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String[] parts = text.split("\\s+");
                int[] freqs = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (s.isEmpty()) {
                        throw new IllegalArgumentException("empty value");
                    }
                    freqs[i] = Integer.parseInt(s);
                }
                protocolHelper.ar8030SetFreqList(freqs, swRemote.isChecked());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "format error", Toast.LENGTH_SHORT).show();
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
            switch (radioType) {
                case FREQ_LIST:
                    Toast.makeText(V4FreqListActivity.this, "set freq list, isSuccess = " + isSuccess + ", message = " + errMessage, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
}
