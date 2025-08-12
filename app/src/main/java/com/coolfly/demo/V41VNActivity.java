package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV41vnBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;

/**
 * Example for 1VN mode.
 * In this mode, you can pair up to 8 devices (1V8).
 * This example shows you how to pair devices, read and write and clear MAC addresses, and switch between 1V1 and 1V4 modes.
 */
public class V41VNActivity extends AppCompatActivity {

    private ActivityV41vnBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV41vnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvMode.setText("Current mode: 1V" + PreferenceActivity.preferenceObject.p401_dev_count);

        binding.tv1vn.setOnClickListener(v -> {
            new AlertDialog.Builder(V41VNActivity.this)
                    .setTitle("Alert")
                    .setMessage("Switch to 1V4 mode, must reboot Android system!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // >1 means 1vN mode, where N is the number of dev.
                        // It must be set before ProtocolHelper initialized. After changed, it will take effect after rebooting the Android system.
                        PreferenceActivity.preferenceObject.p401_dev_count = 4;
                        PreferenceActivity.savePreference();
                        Toast.makeText(V41VNActivity.this, "Please reboot Android system", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .create().show();
        });

        binding.tv1v1.setOnClickListener(v -> {
            new AlertDialog.Builder(V41VNActivity.this)
                    .setTitle("Alert")
                    .setMessage("Restore to 1V1 mode, must reboot Android system!")
                    .setPositiveButton("OK", (dialog, which) -> {
                        // 1 means 1v1 mode.
                        // It must be set before ProtocolHelper initialized. After changed, it will take effect after rebooting the Android system.
                        PreferenceActivity.preferenceObject.p401_dev_count = 1;
                        PreferenceActivity.savePreference();
                        Toast.makeText(V41VNActivity.this, "Please reboot Android system", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .create().show();
        });

        binding.tvPair0.setOnClickListener(v -> {
            protocolHelper.ar8030StartPair(0);
        });

        binding.tvPair1.setOnClickListener(v -> {
            protocolHelper.ar8030StartPair(1);
        });

        binding.tvPair2.setOnClickListener(v -> {
            protocolHelper.ar8030StartPair(2);
        });

        binding.tvPair3.setOnClickListener(v -> {
            protocolHelper.ar8030StartPair(3);
        });

        binding.tvReadMac0.setOnClickListener(v -> {
            if (!protocolHelper.ar8030GetSlotMac(0)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvReadMac1.setOnClickListener(v -> {
            if (!protocolHelper.ar8030GetSlotMac(1)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvReadMac2.setOnClickListener(v -> {
            if (!protocolHelper.ar8030GetSlotMac(2)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvReadMac3.setOnClickListener(v -> {
            if (!protocolHelper.ar8030GetSlotMac(3)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvWriteMac0.setOnClickListener(v -> {
            if (binding.etMac0.getText().toString().isEmpty()) {
                Toast.makeText(V41VNActivity.this, "mac is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac0.getText().toString().length() < 8) {
                Toast.makeText(V41VNActivity.this, "mac is not enough", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac0.getText().toString();
            if (!protocolHelper.ar8030SetSlotMac(0, mac)) {
                Toast.makeText(V41VNActivity.this, "mac parse error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvWriteMac1.setOnClickListener(v -> {
            if (binding.etMac1.getText().toString().isEmpty()) {
                Toast.makeText(V41VNActivity.this, "mac is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac1.getText().toString().length() < 8) {
                Toast.makeText(V41VNActivity.this, "mac is not enough", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac1.getText().toString();
            if (!protocolHelper.ar8030SetSlotMac(1, mac)) {
                Toast.makeText(V41VNActivity.this, "mac parse error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvWriteMac2.setOnClickListener(v -> {
            if (binding.etMac2.getText().toString().isEmpty()) {
                Toast.makeText(V41VNActivity.this, "mac is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac2.getText().toString().length() < 8) {
                Toast.makeText(V41VNActivity.this, "mac is not enough", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac2.getText().toString();
            if (!protocolHelper.ar8030SetSlotMac(2, mac)) {
                Toast.makeText(V41VNActivity.this, "mac parse error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvWriteMac3.setOnClickListener(v -> {
            if (binding.etMac3.getText().toString().isEmpty()) {
                Toast.makeText(V41VNActivity.this, "mac is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.etMac3.getText().toString().length() < 8) {
                Toast.makeText(V41VNActivity.this, "mac is not enough", Toast.LENGTH_SHORT).show();
                return;
            }
            String mac = binding.etMac3.getText().toString();
            if (!protocolHelper.ar8030SetSlotMac(3, mac)) {
                Toast.makeText(V41VNActivity.this, "mac parse error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvClearMac0.setOnClickListener(v -> {
            if (!protocolHelper.ar8030ClearSlotMac(0)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvClearMac1.setOnClickListener(v -> {
            if (!protocolHelper.ar8030ClearSlotMac(1)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvClearMac2.setOnClickListener(v -> {
            if (!protocolHelper.ar8030ClearSlotMac(2)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvClearMac3.setOnClickListener(v -> {
            if (!protocolHelper.ar8030ClearSlotMac(3)) {
                Toast.makeText(V41VNActivity.this, "error", Toast.LENGTH_SHORT).show();
            }
        });

        for (int i = 0; i<4; i++) {
            if (protocolHelper.ar8030IsPaired(i)) {
                setPairState(i, true);
            } else {
                setPairState(i, false);
            }
        }

        protocolHelper.addListener(protocolListener);
    }

    private void setPairState(int slot, boolean isPaired) {
        switch (slot) {
            case 0:
                if (isPaired) {
                    binding.tvSlot0.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    binding.tvSlot0.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
                break;
            case 1:
                if (isPaired) {
                    binding.tvSlot1.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    binding.tvSlot1.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
                break;
            case 2:
                if (isPaired) {
                    binding.tvSlot2.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    binding.tvSlot2.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
                break;
            case 3:
                if (isPaired) {
                    binding.tvSlot3.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    binding.tvSlot3.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                }
                break;
        }
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
        public void onPairTimeOut(DEVICE_TYPE deviceType, int slot) {
            setPairState(slot, false);
        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int slot) {
            setPairState(slot, true);
        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int slot) {
            setPairState(slot, true);
        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int slot) {
            setPairState(slot, false);
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
        public void onSlotMac(DEVICE_TYPE deviceType, int slot, String mac) {
            // Now only for 8030
            switch (slot) {
                case 0:
                    binding.etMac0.setText(mac);
                    break;
                case 1:
                    binding.etMac1.setText(mac);
                    break;
                case 2:
                    binding.etMac2.setText(mac);
                    break;
                case 3:
                    binding.etMac3.setText(mac);
                    break;
            }
        }

        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, boolean isRemote) {
            // Now only for 8030
            switch (radioType) {
                case SLOT_MAC:
                    Toast.makeText(V41VNActivity.this, "set slot mac, isSuccess = " + isSuccess, Toast.LENGTH_SHORT).show();
                    break;
                case CANDIDATES:
                    Toast.makeText(V41VNActivity.this, "set candidates, isSuccess = " + isSuccess, Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

}