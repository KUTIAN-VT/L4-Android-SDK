package com.coolfly.demo;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;

public class V4SysRebootActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    private TextView tvLog;
    private Button btnRebootLocal;
    private Button btnRebootRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_sys_reboot);

        tvLog = findViewById(R.id.tv_log);
        btnRebootLocal = findViewById(R.id.btn_reboot_local);
        btnRebootRemote = findViewById(R.id.btn_reboot_remote);

        btnRebootLocal.setOnClickListener(v -> {
            try {
                appendLog("Executing local reboot command (isRemote=false)...");
                protocolHelper.ar8030SysReboot(false);
                appendLog("Local reboot command sent");
                Toast.makeText(this, "Local reboot command sent", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                appendLog("Local reboot failed: " + t.getMessage());
                Toast.makeText(this, "Local reboot failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        btnRebootRemote.setOnClickListener(v -> {
            try {
                appendLog("Executing remote reboot command (isRemote=true)...");
                protocolHelper.ar8030SysReboot(true);
                appendLog("Remote reboot command sent");
                Toast.makeText(this, "Remote reboot command sent", Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                appendLog("Remote reboot failed: " + t.getMessage());
                Toast.makeText(this, "Remote reboot failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    private void appendLog(String message) {
        String currentLog = tvLog.getText().toString();
        String timestamp = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
        String newLog = timestamp + ": " + message + "\n" + currentLog;
        tvLog.setText(newLog);
    }

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(com.fly.station.prorocol.bean.BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            runOnUiThread(() -> {
                appendLog("Protocol response received: " + baseFlyPacket.getClass().getSimpleName() +
                          " (deviceType=" + deviceType + ", isRemote=" + isRemote + ")");
            });
        }

        @Override
        public int onWrite(byte[] bytes) {
            return 0;
        }

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
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, com.fly.station.prorocol.bean.Throughput8030 throughput, boolean isRemote) {}

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, com.fly.station.prorocol.RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) { }
    };
}
