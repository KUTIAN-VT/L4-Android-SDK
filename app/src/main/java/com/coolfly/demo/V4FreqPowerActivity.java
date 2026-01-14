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
import com.fly.station.prorocol.bean.FreqPower8030;
import com.fly.station.prorocol.bean.Throughput8030;

public class V4FreqPowerActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private TextView tvFreqPower;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_freq_power);

        EditText etFreqPower = findViewById(R.id.et_freq_power);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSetImmediate = findViewById(R.id.btn_set_immediate);
        Button btnSetMiniDb = findViewById(R.id.btn_set_minidb);
        Button btnGet = findViewById(R.id.btn_get);
        tvFreqPower = findViewById(R.id.tv_freq_power);

        btnSetImmediate.setOnClickListener(v -> {
            String text = etFreqPower.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "freq power is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String[] parts = text.split("\\s+");
                int[] pwrPlusArray = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (s.isEmpty()) {
                        throw new IllegalArgumentException("empty value");
                    }
                    pwrPlusArray[i] = Integer.parseInt(s);
                }
                protocolHelper.ar8030SetFreqPowerImmediate(pwrPlusArray, swRemote.isChecked());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "format error", Toast.LENGTH_SHORT).show();
            }
        });

        btnSetMiniDb.setOnClickListener(v -> {
            String text = etFreqPower.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "freq power is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String[] parts = text.split("\\s+");
                int[] pwrPlusArray = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (s.isEmpty()) {
                        throw new IllegalArgumentException("empty value");
                    }
                    pwrPlusArray[i] = Integer.parseInt(s);
                }
                protocolHelper.ar8030SetFreqPowerMiniDb(pwrPlusArray, swRemote.isChecked());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "format error", Toast.LENGTH_SHORT).show();
            }
        });

        btnGet.setOnClickListener(v -> {
            boolean isRemote = swRemote.isChecked();
            protocolHelper.ar8030GetFreqPowerMiniDb(isRemote);
            String remoteText = isRemote ? "远程" : "本地";
            Toast.makeText(this, "正在读取" + remoteText + "功率设置...", Toast.LENGTH_SHORT).show();
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
            if (baseFlyPacket instanceof FreqPower8030) {
                FreqPower8030 freqPower = (FreqPower8030) baseFlyPacket;
                runOnUiThread(() -> {
                    try {
                        int[] pwrPlusArray = freqPower.pwrPlusArray;
                        String prefix = isRemote ? "远程功率设置: " : "本地功率设置: ";
                        if (pwrPlusArray != null && pwrPlusArray.length > 0) {
                            StringBuilder sb = new StringBuilder(prefix);
                            for (int i = 0; i < pwrPlusArray.length; i++) {
                                if (i > 0) sb.append(" ");
                                sb.append(pwrPlusArray[i]);
                            }
                            tvFreqPower.setText(sb.toString());
                        } else {
                            tvFreqPower.setText(prefix + "无数据");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(V4FreqPowerActivity.this, "解析功率设置失败", Toast.LENGTH_SHORT).show();
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
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
            switch (radioType) {
                case FREQ_POWER_IMMEDIATE:
                    Toast.makeText(V4FreqPowerActivity.this, "set freq power immediate, isSuccess = " + isSuccess + ", message = " + errMessage, Toast.LENGTH_SHORT).show();
                    break;
                case FREQ_POWER_MINIDB:
                    Toast.makeText(V4FreqPowerActivity.this, "set freq power minidb, isSuccess = " + isSuccess + ", message = " + errMessage, Toast.LENGTH_SHORT).show();
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