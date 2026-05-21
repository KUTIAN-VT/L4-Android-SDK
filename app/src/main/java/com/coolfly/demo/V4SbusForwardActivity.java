package com.coolfly.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4SbusForwardBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.station.chuanyun.entity.SbusConfig;
import com.fly.station.chuanyun.entity.SbusData;
import com.fly.station.mcu.McuManager;
import com.fly.station.mcu.McuPacket;
import com.fly.station.mcu.SbusConverter;
import com.fly.station.mcu.entity.ActiveState;
import com.fly.station.mcu.entity.CalibrateState;
import com.fly.station.mcu.entity.HeartBeat;
import com.fly.station.mcu.entity.OperateMode;
import com.fly.station.mcu.entity.SbusProtect;
import com.fly.station.mcu.entity.SbusValid;
import com.fly.station.mcu.entity.Version;
import com.fly.station.prorocol.ProtocolHelper;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * SBUS Forward 示例：
 * - sbus_default 来源于 MCU 的 SbusProtect（通过 McuManager 监听 onSbusProtect 获得）；
 * - 开启后，对所有已对频的 slot 打开 port=0 的透传通道；
 * - 通过定时器(30ms)向"未被选中"的 slot 周期性发送 SBUS 保护帧；
 * - 被选中的 slot 不发送，留给上层注入真实 SBUS 数据。
 */
public class V4SbusForwardActivity extends AppCompatActivity {

    private static final int PORT = 0;
    private static final long INTERVAL_MS = 30L;

    private ActivityV4SbusForwardBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private McuManager mcuManager;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private Thread pollingThread;
    private volatile boolean forwardOn = false;
    private volatile int selectedSlot = 0;
    private volatile SbusProtect sbusProtect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4SbusForwardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        McuManager.setDevicePath(PreferenceActivity.preferenceObject.mcu_serial_path);
        McuManager.setBaudRate(PreferenceActivity.preferenceObject.mcu_serial_baudrate.toString());
        McuManager.setIsShowLog(PreferenceActivity.preferenceObject.show_mcu_log);
        mcuManager = McuManager.getInstance();
        mcuManager.addListener(mcuListener);

        binding.spSlot.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedSlot = position;
                log("selected slot = " + selectedSlot);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        binding.tvConnect.setOnClickListener(v -> mcuManager.onLine());
        binding.tvDisconnect.setOnClickListener(v -> mcuManager.offLine());

        binding.btnReadProtect.setOnClickListener(v -> {
            mcuManager.writePacket(McuPacket.createReadSbusProtectPacket());
            log("request sbus protect");
        });

        binding.swForward.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sbusForwardOn();
            } else {
                sbusForwardOff();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sbusForwardOff();
        mcuManager.removeListener(mcuListener);
    }

    private void sbusForwardOn() {
        int slotCount = ProtocolHelper.ar8030GetSlotSize();
        int opened = 0;
        for (int slot = 0; slot < slotCount; slot++) {
            if (!protocolHelper.ar8030IsPaired(slot)) continue;
            protocolHelper.ar8030OpenPassthrough(slot, PORT);
            opened++;
        }
        forwardOn = true;
        mcuManager.writePacket(McuPacket.createWriteSbusDataSwitchPacket(true));
        startPolling();
        log("forward ON, opened " + opened + "/" + slotCount + " slot(s)");
    }

    private void sbusForwardOff() {
        forwardOn = false;
        stopPolling();
        int slotCount = ProtocolHelper.ar8030GetSlotSize();
        for (int slot = 0; slot < slotCount; slot++) {
            protocolHelper.ar8030ClosePassthrough(slot, PORT);
        }
        log("forward OFF");
    }

    private void startPolling() {
        stopPolling();
        pollingThread = new Thread(() -> {
            while (forwardOn && !Thread.currentThread().isInterrupted()) {
                SbusProtect cur = sbusProtect;
                int[] defaultData = cur != null ? cur.sbus_default : null;
                if (defaultData != null) {
                    int sel = selectedSlot;
                    int slotCount = ProtocolHelper.ar8030GetSlotSize();
                    byte[] frame = SbusConverter.packSbusFrame(defaultData);
                    for (int slot = 0; slot < slotCount; slot++) {
                        if (slot == sel) continue;
                        protocolHelper.ar8030WritePassthroughData(slot, PORT, frame, frame.length);
                    }
                }
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "v4-sbus-forward");
        pollingThread.start();
    }

    private void stopPolling() {
        if (pollingThread != null) {
            pollingThread.interrupt();
            pollingThread = null;
        }
    }

    private void log(String msg) {
        uiHandler.post(() -> {
            String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            String cur = binding.tvLog.getText().toString();
            binding.tvLog.setText(cur + "\n[" + ts + "] " + msg);
            binding.svLog.post(() -> binding.svLog.fullScroll(View.FOCUS_DOWN));
        });
    }

    @Keep
    private final McuManager.McuListener mcuListener = new McuManager.McuListener() {
        @Override
        public void onConnected() {
            mcuManager.writePacket(McuPacket.createReadSbusProtectPacket());
        }

        @Override
        public void onGiveUpConnect() {}

        @Override
        public void onHeartBeat(HeartBeat heartBeat) {}

        @Override
        public void onShutdownOs() {}

        @Override
        public void onVersion(Version version) {}

        @Override
        public void onActiveState(ActiveState activeState) {}

        @Override
        public void onSbusConfig(SbusConfig sbusConfig) {}

        @Override
        public void onSbusData(SbusData sbusData) {
            if (!forwardOn) return;
            int[] channelData = sbusData.sbus_data;
            if (channelData == null) return;
            int sel = selectedSlot;
            byte[] frame = SbusConverter.packSbusFrame(channelData);
            protocolHelper.ar8030WritePassthroughData(sel, PORT, frame, frame.length);
        }

        @Override
        public void onOperateMode(OperateMode operateMode) {}

        @Override
        public void onSbusProtect(SbusProtect sp) {
            sbusProtect = sp;
            String desc = sp.sbus_default != null
                    ? Arrays.toString(sp.sbus_default)
                    : "(null)";
            uiHandler.post(() -> binding.tvDefault.setText("sbus_default: " + desc));
            log("onSbusProtect: " + desc);
        }

        @Override
        public void onSbusValid(SbusValid sbusValid) {}

        @Override
        public void onCalibrateState(CalibrateState calibrateState) {}

        @Override
        public void onACK() {}

        @Override
        public void onNACK(String error) {
            log("NACK: " + error);
        }
    };
}
