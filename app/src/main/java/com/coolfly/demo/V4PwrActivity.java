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

public class V4PwrActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_pwr);

        androidx.appcompat.widget.SwitchCompat swAuto = findViewById(R.id.sw_auto);
        EditText etPwrInit = findViewById(R.id.et_pwr_init);
        EditText etPwrMin = findViewById(R.id.et_pwr_min);
        EditText etPwrMax = findViewById(R.id.et_pwr_max);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSet = findViewById(R.id.btn_set_pwr);

        btnSet.setOnClickListener(v -> {
            boolean auto = swAuto.isChecked();
            boolean remote = swRemote.isChecked();
            try {
                int pwrInit = 0;
                int pwrMin = 0;
                int pwrMax = 0;
                if (!auto) {
                    if (TextUtils.isEmpty(etPwrInit.getText())) {
                        Toast.makeText(this, "pwrInit is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pwrInit = Integer.parseInt(etPwrInit.getText().toString());
                } else {
                    if (TextUtils.isEmpty(etPwrMin.getText()) || TextUtils.isEmpty(etPwrMax.getText())) {
                        Toast.makeText(this, "pwrMin/pwrMax is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pwrMin = Integer.parseInt(etPwrMin.getText().toString());
                    pwrMax = Integer.parseInt(etPwrMax.getText().toString());
                }
                protocolHelper.ar8030SetPwrMiniDb(auto, pwrInit, pwrMin, pwrMax, remote);
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
                case POWER_MINIDB:
                    Toast.makeText(V4PwrActivity.this, "set power, isSuccess = " + isSuccess + ", message = " + errMessage, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };
}


