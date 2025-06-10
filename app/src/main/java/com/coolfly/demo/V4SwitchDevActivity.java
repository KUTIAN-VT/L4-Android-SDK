package com.coolfly.demo;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4SwitchDevBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.bean.BaseFlyPacket;

public class V4SwitchDevActivity extends AppCompatActivity {

    private ActivityV4SwitchDevBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4SwitchDevBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvSwitch.setOnClickListener(v -> {
            if (binding.etMac.getText().toString().isEmpty()) {
                Toast.makeText(V4SwitchDevActivity.this, "mac is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac.getText().toString().length() < 8) {
                Toast.makeText(V4SwitchDevActivity.this, "mac is not enough", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac.getText().toString();
            if (!protocolHelper.ar8030SetSlotMac(mac)) {
                Toast.makeText(V4SwitchDevActivity.this, "mac parse error", Toast.LENGTH_SHORT).show();
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
        public void onPairTimeOut(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType) {

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
    };

}