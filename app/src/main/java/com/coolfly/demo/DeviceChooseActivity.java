package com.coolfly.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coolfly.demo.databinding.ActivityDeviceChooseBinding;
import com.coolfly.demo.entry.P201Activity;
import com.coolfly.demo.entry.P301Activity;
import com.coolfly.demo.entry.P401Activity;
import com.coolfly.demo.preference.PreferenceActivity;
import com.coolfly.demo.utils.PermissionHelper;
import com.fly.station.gpio.AoaSwitch;
import com.fly.station.gpio.HostSwitch;
import com.fly.station.prorocol.Fly;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class DeviceChooseActivity extends AppCompatActivity {

    private ActivityDeviceChooseBinding binding;
    private PermissionHelper permissionHelper;
    private final String[] mainEntryNames = {"201", "301", "401"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityDeviceChooseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setupMainEntries();
        setupFeatureEntries();

        permissionHelper = new PermissionHelper(this);

        if (Fly.isRk()) {
            binding.swLink.setVisibility(View.GONE);
        }

        readAoa();
    }

    @Override
    protected void onResume() {
        super.onResume();
        permissionHelper.onResume();

        try {
            binding.tvSn.setText(String.format("RCSN: %s", Fly.getRCSerialNumber()));
            binding.tvSysVersion.setText(String.format("RCSysVer: %s", Fly.getRCSysVersion()));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }
        binding.tvAppVersion.setText("Built at " + BuildConfig.COMPILE_TIME);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        super.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F1) {
            readAoa();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void readAoa() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Fly.isRk()) {
                    HostSwitch.AoaMode aoaMode = HostSwitch.getMode();
                    runOnUiThread(() -> {
                        binding.swPower.setOnCheckedChangeListener(null);

                        switch (aoaMode) {

                            case POWER_OFF:
                                binding.swPower.setChecked(false);
                                break;
                            case POWER_ON:
                                binding.swPower.setChecked(true);
                                break;
                            case UNKNOWN:
                                break;
                        }

                        binding.swPower.setOnCheckedChangeListener((compoundButton, b) -> new Thread(new Runnable() {
                            @Override
                            public void run() {
                                HostSwitch.switchPower(b);
                            }
                        }).start());
                    });
                } else {
                    AoaSwitch.AoaMode aoaMode = AoaSwitch.getMode();
                    runOnUiThread(() -> {
                        binding.swLink.setOnCheckedChangeListener(null);
                        binding.swPower.setOnCheckedChangeListener(null);

                        switch (aoaMode) {

                            case LINK_OFF_POWER_OFF:
                                binding.swLink.setChecked(false);
                                binding.swPower.setChecked(false);
                                break;
                            case LINK_OFF_POWER_ON:
                                binding.swLink.setChecked(false);
                                binding.swPower.setChecked(true);
                                break;
                            case LINK_ON_POWER_OFF:
                                binding.swLink.setChecked(true);
                                binding.swPower.setChecked(false);
                                break;
                            case LINK_ON_POWER_ON:
                                binding.swLink.setChecked(true);
                                binding.swPower.setChecked(true);
                                break;
                            case UNKNOWN:
                                break;
                        }

                        binding.swLink.setOnCheckedChangeListener((compoundButton, b) -> new Thread(new Runnable() {
                            @Override
                            public void run() {
                                AoaSwitch.switchLink(b);
                            }
                        }).start());

                        binding.swPower.setOnCheckedChangeListener((compoundButton, b) -> new Thread(new Runnable() {
                            @Override
                            public void run() {
                                AoaSwitch.switchPower(b);
                            }
                        }).start());
                    });
                }
            }
        }).start();
    }

    private void setupMainEntries() {
        for (String name : mainEntryNames) {
            Button btn = new Button(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (56 * getResources().getDisplayMetrics().density), 1f);
            lp.setMargins(dp(6), 0, dp(6), 0);
            btn.setLayoutParams(lp);
            btn.setText(name);
            btn.setTextSize(20);
            btn.setOnClickListener(v -> onMainEntryClick(name));
            binding.layoutMainEntries.addView(btn);
        }
    }

    private void onMainEntryClick(String name) {
        switch (name) {
            case "201":
                startActivity(new Intent(DeviceChooseActivity.this, P201Activity.class));
                break;
            case "301":
                startActivity(new Intent(DeviceChooseActivity.this, P301Activity.class));
                break;
            case "401":
                startActivity(new Intent(DeviceChooseActivity.this, P401Activity.class));
                break;
        }
    }

    private void setupFeatureEntries() {
        List<FeatureEntry> entries = new ArrayList<>();
        entries.add(new FeatureEntry("MCU", McuActivity.class));
        entries.add(new FeatureEntry("TTY 2 BLUETOOTH", Tty2BluetoothActivity.class));
        entries.add(new FeatureEntry("TTY 2 UDP", Tty2UdpActivity.class));
        entries.add(new FeatureEntry("RTSP Multi", RtspMultiActivity.class));
        entries.add(new FeatureEntry("RTSP Single", RtspSingleActivity.class));
        entries.add(new FeatureEntry("RTP Multi", UdpRtpMultiActivity.class));
        entries.add(new FeatureEntry("RTP Single", UdpRtpActivity.class));
        entries.add(new FeatureEntry("MQTT", MqttActivity.class));
        entries.add(new FeatureEntry("Preference", PreferenceActivity.class));

        binding.rvFeatureEntries.setLayoutManager(new GridLayoutManager(this, 3));
        binding.rvFeatureEntries.setAdapter(new FeatureAdapter(entries));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static class FeatureEntry {
        String name;
        Class<?> target;

        FeatureEntry(String name, Class<?> target) {
            this.name = name;
            this.target = target;
        }
    }

    private class FeatureAdapter extends RecyclerView.Adapter<FeatureAdapter.VH> {
        private final List<FeatureEntry> items;

        FeatureAdapter(List<FeatureEntry> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Button btn = new Button(parent.getContext());
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            btn.setLayoutParams(lp);
            btn.setTextSize(12);
            return new VH(btn);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            FeatureEntry entry = items.get(position);
            ((Button) holder.itemView).setText(entry.name);
            holder.itemView.setOnClickListener(v -> {
                startActivity(new Intent(DeviceChooseActivity.this, entry.target));
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class VH extends RecyclerView.ViewHolder {
            VH(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
