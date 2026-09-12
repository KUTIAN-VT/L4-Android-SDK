package com.coolfly.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4SocketEncryptBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.GetSocketEncrypt8030;
import com.fly.station.prorocol.bean.GetSocketEncryptMiniDb8030;
import com.fly.station.prorocol.bean.Throughput8030;

public class V4SocketEncryptActivity extends AppCompatActivity {

    private ActivityV4SocketEncryptBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4SocketEncryptBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSetRuntime.setOnClickListener(v -> {
            Params p = readEnableParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030SetSocketEncrypt(p.slot, p.port, p.mode, p.key, p.remote, p.remoteSlot);
            if (!ok) {
                Toast.makeText(this, "set runtime rejected", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnDisableRuntime.setOnClickListener(v -> {
            Params p = readTargetParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030DisableSocketEncrypt(p.slot, p.port, p.remote, p.remoteSlot);
            if (!ok) {
                Toast.makeText(this, "disable runtime rejected", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnGetRuntime.setOnClickListener(v -> {
            Params p = readTargetParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030GetSocketEncrypt(p.slot, p.port, p.remote, p.remoteSlot);
            Toast.makeText(this, ok ? "getting runtime..." : "get runtime rejected", Toast.LENGTH_SHORT).show();
        });

        binding.btnSetMinidb.setOnClickListener(v -> {
            Params p = readEnableParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030SetSocketEncryptMiniDb(p.slot, p.port, p.mode, p.key, p.remote, p.remoteSlot);
            if (!ok) {
                Toast.makeText(this, "set MiniDB rejected", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnDisableMinidb.setOnClickListener(v -> {
            Params p = readTargetParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030DisableSocketEncryptMiniDb(p.slot, p.port, p.remote, p.remoteSlot);
            if (!ok) {
                Toast.makeText(this, "disable MiniDB rejected", Toast.LENGTH_SHORT).show();
            }
        });
        binding.btnGetMinidb.setOnClickListener(v -> {
            Params p = readTargetParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030GetSocketEncryptMiniDb(p.slot, p.port, p.remote, p.remoteSlot);
            Toast.makeText(this, ok ? "getting MiniDB..." : "get MiniDB rejected", Toast.LENGTH_SHORT).show();
        });

        binding.btnSetOpen.setOnClickListener(v -> {
            Params p = readEnableParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030SetSocketOpenEncrypt(p.slot, p.port, p.mode, p.key);
            Toast.makeText(this, ok ? "open encrypt remembered" : "set open rejected", Toast.LENGTH_SHORT).show();
        });
        binding.btnClearOpen.setOnClickListener(v -> {
            Params p = readTargetParams();
            if (p == null) {
                return;
            }
            boolean ok = protocolHelper.ar8030ClearSocketOpenEncrypt(p.slot, p.port);
            Toast.makeText(this, ok ? "open encrypt cleared" : "clear open rejected", Toast.LENGTH_SHORT).show();
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    private Params readTargetParams() {
        Params p = new Params();
        p.slot = (int) binding.spSlot.getSelectedItemId();
        p.port = (int) binding.spPort.getSelectedItemId();
        p.mode = spinnerMode();
        p.remote = binding.swRemote.isChecked();
        p.remoteSlot = (int) binding.spRemoteSlot.getSelectedItemId();
        return p;
    }

    private Params readEnableParams() {
        Params p = readTargetParams();
        try {
            p.key = parseKey(p.mode);
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
        return p;
    }

    private int spinnerMode() {
        int idx = (int) binding.spMode.getSelectedItemId();
        if (idx == 1) {
            return ProtocolHelper.SOCK_ENCRYPT_MODE_AES128;
        }
        if (idx == 2) {
            return ProtocolHelper.SOCK_ENCRYPT_MODE_AES256;
        }
        return ProtocolHelper.SOCK_ENCRYPT_MODE_DEFAULT;
    }

    private byte[] parseKey(int mode) {
        String text = binding.etKey.getText() == null ? "" : binding.etKey.getText().toString().trim();
        if (mode == ProtocolHelper.SOCK_ENCRYPT_MODE_DEFAULT) {
            if (!TextUtils.isEmpty(text)) {
                throw new IllegalArgumentException("default must not include key");
            }
            return null;
        }
        if (TextUtils.isEmpty(text)) {
            throw new IllegalArgumentException("key is empty");
        }
        String hex = text.replaceAll("\\s+", "");
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("key hex length is odd");
        }
        byte[] key = new byte[hex.length() / 2];
        try {
            for (int i = 0; i < key.length; i++) {
                key[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("key hex invalid");
        }
        return key;
    }

    private static String modeName(int mode) {
        if (mode == ProtocolHelper.SOCK_ENCRYPT_MODE_AES128) {
            return "aes128";
        }
        if (mode == ProtocolHelper.SOCK_ENCRYPT_MODE_AES256) {
            return "aes256";
        }
        if (mode == ProtocolHelper.SOCK_ENCRYPT_MODE_DEFAULT) {
            return "default";
        }
        return String.valueOf(mode);
    }

    private static String bytesToHex(byte[] data) {
        if (data == null || data.length == 0) {
            return "none";
        }
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }

    private static final class Params {
        int slot;
        int port;
        int mode;
        byte[] key;
        boolean remote;
        int remoteSlot;
    }

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {
        }

        @Override
        public void onReadCmd(BaseFlyPacket packet, DEVICE_TYPE deviceType, boolean isRemote) {
            if (packet instanceof GetSocketEncrypt8030) {
                GetSocketEncrypt8030 bean = (GetSocketEncrypt8030) packet;
                runOnUiThread(() -> binding.tvResult.setText(
                        (isRemote ? "remote " : "local ")
                                + "runtime slot=" + bean.slot
                                + " port=" + bean.port
                                + " enabled=" + bean.enabled
                                + " mode=" + modeName(bean.mode)));
            } else if (packet instanceof GetSocketEncryptMiniDb8030) {
                GetSocketEncryptMiniDb8030 bean = (GetSocketEncryptMiniDb8030) packet;
                runOnUiThread(() -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append(isRemote ? "remote " : "local ");
                    sb.append("MiniDB slot=").append(bean.slot)
                            .append(" port=").append(bean.port)
                            .append(" unset=").append(bean.unset)
                            .append(" enabled=").append(bean.enabled)
                            .append(" mode=").append(modeName(bean.mode));
                    if (!bean.unset && bean.enabled) {
                        sb.append(" key=").append(bytesToHex(bean.key));
                    }
                    binding.tvResult.setText(sb.toString());
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
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {
        }

        @Override
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
        }

        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            switch (radioType) {
                case SOCKET_ENCRYPT:
                    Toast.makeText(V4SocketEncryptActivity.this,
                            "runtime encrypt, isSuccess=" + isSuccess + ", message=" + errMessage,
                            Toast.LENGTH_SHORT).show();
                    break;
                case SOCKET_ENCRYPT_MINIDB:
                    Toast.makeText(V4SocketEncryptActivity.this,
                            "MiniDB encrypt, isSuccess=" + isSuccess + ", message=" + errMessage,
                            Toast.LENGTH_SHORT).show();
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
