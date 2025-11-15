package com.coolfly.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4MacSettingsBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.Throughput8030;

/**
 * MAC Settings for V4. Demonstrate the four new MAC setting methods:
 * - ar8030SetApMac: Set AP device's MAC address saved on Sky
 * - ar8030SetLocalMac: Set local device's MAC address
 * - ar8030SetApMacMiniDb: Set AP device's MAC address saved on Sky and save to MiniDB
 * - ar8030SetLocalMacMiniDb: Set local device's MAC address and save to MiniDB
 */
public class V4MacSettingsActivity extends AppCompatActivity {

    private ActivityV4MacSettingsBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4MacSettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set AP MAC address button
        binding.btnSetApMac.setOnClickListener(v -> {
            if (binding.etMac.getText().toString().isEmpty()) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac.getText().toString().length() < 8) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is too short", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac.getText().toString();
            if (!protocolHelper.ar8030SetApMac(mac)) {
                Toast.makeText(V4MacSettingsActivity.this, "AP MAC parse error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4MacSettingsActivity.this, "AP MAC set successfully", Toast.LENGTH_SHORT).show();
            }
        });

        // Set Local MAC address button
        binding.btnSetLocalMac.setOnClickListener(v -> {
            if (binding.etMac.getText().toString().isEmpty()) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac.getText().toString().length() < 8) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is too short", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac.getText().toString();
            if (!protocolHelper.ar8030SetLocalMac(mac)) {
                Toast.makeText(V4MacSettingsActivity.this, "Local MAC parse error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4MacSettingsActivity.this, "Local MAC set successfully", Toast.LENGTH_SHORT).show();
            }
        });

        // Set AP MAC address with MiniDB button
        binding.btnSetApMacMiniDb.setOnClickListener(v -> {
            if (binding.etMac.getText().toString().isEmpty()) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac.getText().toString().length() < 8) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is too short", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac.getText().toString();
            if (!protocolHelper.ar8030SetApMacMiniDb(mac)) {
                Toast.makeText(V4MacSettingsActivity.this, "AP MAC MiniDB parse error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4MacSettingsActivity.this, "AP MAC MiniDB set successfully", Toast.LENGTH_SHORT).show();
            }
        });

        // Set Local MAC address with MiniDB button
        binding.btnSetLocalMacMiniDb.setOnClickListener(v -> {
            if (binding.etMac.getText().toString().isEmpty()) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac.getText().toString().length() < 8) {
                Toast.makeText(V4MacSettingsActivity.this, "MAC address is too short", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac.getText().toString();
            if (!protocolHelper.ar8030SetLocalMacMiniDb(mac)) {
                Toast.makeText(V4MacSettingsActivity.this, "Local MAC MiniDB parse error", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4MacSettingsActivity.this, "Local MAC MiniDB set successfully", Toast.LENGTH_SHORT).show();
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
        public void onReady(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {

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
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
            mainHandler.post(() -> {
                String message;
                switch (radioType) {
                    case AP_MAC:
                        message = isSuccess ? "AP MAC 设置成功" : "AP MAC 设置失败: " + errMessage;
                        break;
                    case LOCAL_MAC:
                        message = isSuccess ? "Local MAC 设置成功" : "Local MAC 设置失败: " + errMessage;
                        break;
                    case AP_MAC_MINIDB:
                        message = isSuccess ? "AP MAC MiniDB 设置成功" : "AP MAC MiniDB 设置失败: " + errMessage;
                        break;
                    case LOCAL_MAC_MINIDB:
                        message = isSuccess ? "Local MAC MiniDB 设置成功" : "Local MAC MiniDB 设置失败: " + errMessage;
                        break;
                    default:
                        message = isSuccess ? "设置成功" : "设置失败: " + errMessage;
                        break;
                }
                Toast.makeText(V4MacSettingsActivity.this, message, Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {

        }
    };

}
