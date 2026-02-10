package com.coolfly.demo.entry;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.coolfly.demo.R;
import com.coolfly.demo.RadioTestActivity;
import com.coolfly.demo.V41VNActivity;
import com.coolfly.demo.V4BandwidthActivity;
import com.coolfly.demo.V4ConfigActivity;
import com.coolfly.demo.V4FreqListActivity;
import com.coolfly.demo.V4FreqPowerActivity;
import com.coolfly.demo.V4MacSettingsActivity;
import com.coolfly.demo.V4PassthroughActivity;
import com.coolfly.demo.V4PwrActivity;
import com.coolfly.demo.V4SetChannelActivity;
import com.coolfly.demo.V4StatusActivity;
import com.coolfly.demo.V4SwitchDevActivity;
import com.coolfly.demo.V4SysRebootActivity;
import com.coolfly.demo.V4ThroughputActivity;
import com.coolfly.demo.databinding.ActivityP401Binding;
import com.coolfly.demo.debug.DebugViewManager;
import com.fly.aoalibrary.DEVICE_TYPE;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.UpgradeHelper;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.SysInfo8030;
import com.fly.station.prorocol.bean.Throughput8030;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class P401Activity extends AppCompatActivity {

    private ActivityP401Binding binding;
    private ProtocolHelper protocolHelper;
    private UsbDeviceHelper usbDeviceHelper;

    private final int REQ_OTA_V4 = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityP401Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        protocolHelper = ProtocolHelper.getInstance();
        protocolHelper.addListener(protocolListener);

        usbDeviceHelper = UsbDeviceHelper.getInstance(getApplicationContext());

        binding.btnUpgradeV4.setOnClickListener(v -> {
            if (usbDeviceHelper.getUsbStatus() != UsbDeviceHelper.USB_CONNECTED || usbDeviceHelper.getDeviceType() != DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_V4);
        });

        initButtons();

        binding.swV4Debug.setChecked(DebugViewManager.INSTANCE.isShowingConnStatView());
        binding.swV4Debug.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                DebugViewManager.INSTANCE.showConnStatView(P401Activity.this);
            } else {
                DebugViewManager.INSTANCE.hideConnStatView();
            }
        });

        binding.swV4Log.setChecked(DebugViewManager.INSTANCE.isShowingLogView());
        binding.swV4Log.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                DebugViewManager.INSTANCE.showLogView(P401Activity.this);
            } else {
                DebugViewManager.INSTANCE.hideLogView();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    private void initButtons() {
        addButton("Pair", v -> new Thread(() -> protocolHelper.ar8030StartPair()).start());
        addButton("Set Channel", v -> startActivity(new Intent(this, V4SetChannelActivity.class)));
        addButton("Bandwidth", v -> startActivity(new Intent(this, V4BandwidthActivity.class)));
        addButton("Config", v -> startActivity(new Intent(this, V4ConfigActivity.class)));
        addButton("Passthrough", v -> startActivity(new Intent(this, V4PassthroughActivity.class)));
        addButton("Freq List", v -> startActivity(new Intent(this, V4FreqListActivity.class)));
        addButton("Freq Power", v -> startActivity(new Intent(this, V4FreqPowerActivity.class)));
        addButton("Power", v -> startActivity(new Intent(this, V4PwrActivity.class)));
        addButton("Radio Test", v -> startActivity(new Intent(this, RadioTestActivity.class)));
        addButton("MAC Settings", v -> startActivity(new Intent(this, V4MacSettingsActivity.class)));
        addButton("Switch Dev", v -> startActivity(new Intent(this, V4SwitchDevActivity.class)));
        addButton("1VN", v -> startActivity(new Intent(this, V41VNActivity.class)));
        addButton("SysInfo", v -> showSysInfoDialog());
        addButton("Throughput", v -> startActivity(new Intent(this, V4ThroughputActivity.class)));
        addButton("Status", v -> startActivity(new Intent(this, V4StatusActivity.class)));
        addButton("Sys Reboot", v -> startActivity(new Intent(this, V4SysRebootActivity.class)));
    }

    private void addButton(String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setOnClickListener(listener);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);

        binding.gridButtons.addView(btn);
    }

    private void showSysInfoDialog() {
        String[] sides = {"AP", "DEV"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("get fw version")
                .setItems(sides, (dialog, which) -> protocolHelper.ar8030GetSysInfo(which == 1))
                .create().show();
    }

    private void getUpgradeFis(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream fis = getContentResolver().openInputStream(uri);
                if (fis != null) {
                    protocolHelper.ar8030Upgrade(fis, upgradeListener8030);
                    binding.btnUpgradeV4.setEnabled(false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(com.fly.station.prorocol.DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket packet, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            if (packet instanceof SysInfo8030) {
                // AR8030 system info
                binding.tvSysInfo.setText((isRemote? "dev: ": "ap: ") + packet);
            }
        }

        @Override
        public int onWrite(byte[] data) {
            return 0;
        }

        @Override
        public void onPairOperated(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot, boolean isStart) {
            // Now only for 8030, pair manually time out
        }

        @Override
        public void onPairTimeOut(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually time out
        }

        @Override
        public void onPairSuccess(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually success
        }

        @Override
        public void onLinked(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link ready automatically
        }

        @Override
        public void onLinkLost(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link lost automatically
        }

        @Override
        public void onConfigJson(@Nullable String jsonString, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetConfigJson(boolean result, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onResetConfigJson(boolean result, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSlotMac(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot, String mac) {
            // Now only for 8030
        }

        @Override
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // AR8030 throughput data received
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onDebugMessage(com.fly.station.prorocol.DEVICE_TYPE deviceType, String s) {
            // Now only for 8030
        }

    };
    @Keep
    private final UpgradeHelper.UpgradeListener upgradeListener8030 = new UpgradeHelper.UpgradeListener() {
        @Override
        public void onStart() {
            binding.tvUpdateProcess.setText(R.string.ota_start);
        }

        @Override
        public void onProcess(int curFrame, int totalFrame) {
            binding.tvUpdateProcess.setText(curFrame + " / " + totalFrame);
        }

        @Override
        public void onFlashing() {
            binding.tvUpdateProcess.setText(R.string.ota_ing);
        }

        @Override
        public void onComplete() {
            binding.tvUpdateProcess.setText(R.string.ota_finish);
            binding.btnUpgradeV4.setEnabled(true);
        }

        @Override
        public void onFail(String errMsg) {
            binding.tvUpdateProcess.setText(R.string.ota_fail + "\n" + errMsg);
            binding.btnUpgradeV4.setEnabled(true);
        }
    };
}
