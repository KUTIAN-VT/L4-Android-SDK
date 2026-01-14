package com.coolfly.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.FreqList8030;
import com.fly.station.prorocol.bean.Throughput8030;

public class V4FreqListActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private TextView tvLocalFreqList;
    private TextView tvRemoteFreqList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_freq_list);

        EditText etFreqList = findViewById(R.id.et_freq_list);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSet = findViewById(R.id.btn_set);
        Button btnGetLocal = findViewById(R.id.btn_get_local);
        Button btnGetRemote = findViewById(R.id.btn_get_remote);
        tvLocalFreqList = findViewById(R.id.tv_local_freq_list);
        tvRemoteFreqList = findViewById(R.id.tv_remote_freq_list);

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

        btnGetLocal.setOnClickListener(v -> {
            protocolHelper.ar8030GetFreqList(false);
            Toast.makeText(this, "正在读取本地频率列表...", Toast.LENGTH_SHORT).show();
        });

        btnGetRemote.setOnClickListener(v -> {
            protocolHelper.ar8030GetFreqList(true);
            Toast.makeText(this, "正在读取远程频率列表...", Toast.LENGTH_SHORT).show();
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
        public void onReady(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof FreqList8030) {
                FreqList8030 freqList = (FreqList8030) baseFlyPacket;
                runOnUiThread(() -> {
                    try {
                        int[] freqs = freqList.freqList;
                        if (freqs != null && freqs.length > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < freqs.length; i++) {
                                if (i > 0) sb.append(" ");
                                sb.append(freqs[i]);
                            }
                            String freqText = sb.toString();
                            if (isRemote) {
                                tvRemoteFreqList.setText(freqText);
                            } else {
                                tvLocalFreqList.setText(freqText);
                            }
                        } else {
                            if (isRemote) {
                                tvRemoteFreqList.setText("远程频率列表: 无数据");
                            } else {
                                tvLocalFreqList.setText("本地频率列表: 无数据");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(V4FreqListActivity.this, "解析频率列表失败", Toast.LENGTH_SHORT).show();
                    }
                });
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
            switch (radioType) {
                case FREQ_LIST:
                    Toast.makeText(V4FreqListActivity.this, "set freq list, isSuccess = " + isSuccess + ", message = " + errMessage, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {

        }
    };
}
