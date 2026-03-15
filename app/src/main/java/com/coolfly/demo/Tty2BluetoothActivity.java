package com.coolfly.demo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.coolfly.demo.databinding.ActivityTty2bluetoothBinding;
import com.fly.station.bluetooth.BluetoothSppManager;
import com.fly.station.tty.TtyManager;

import java.util.ArrayList;
import java.util.List;

public class Tty2BluetoothActivity extends AppCompatActivity {

    private ActivityTty2bluetoothBinding binding;

    private BluetoothSppManager bluetoothManager;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    // For tty passthrough
    private TtyManager ttyManager;

    // 请求蓝牙权限的请求码
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;

    // 启用蓝牙的ActivityResultLauncher
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityTty2bluetoothBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化蓝牙管理器
        bluetoothManager = BluetoothSppManager.getInstance(this);

        ttyManager = TtyManager.getInstance();
        ttyManager.onLine();

        // 设置设备列表适配器
        deviceAdapter = new DeviceAdapter(discoveredDevices, device -> {
            // 点击设备时连接
            bluetoothManager.connect(device);
        });
        binding.deviceListView.setAdapter(deviceAdapter);
        binding.deviceListView.setLayoutManager(new LinearLayoutManager(this));

        // 设置按钮点击事件
        binding.btnEnableBluetooth.setOnClickListener(v -> {
            if (!bluetoothManager.isBluetoothEnabled()) {
                if (bluetoothManager.hasBluetoothPermissions()) {
                    // 权限获取成功，启用蓝牙
                    bluetoothManager.requestEnableBluetooth(enableBluetoothLauncher);
                } else {
                    requestBluetoothPermissions();
                }
            }
        });

        binding.btnScan.setOnClickListener(v -> {
            if (bluetoothManager.isBluetoothEnabled()) {
                discoveredDevices.clear();
                deviceAdapter.notifyDataSetChanged();
                if (bluetoothManager.hasBluetoothPermissions()) {
                    // 权限获取成功，搜索蓝牙設備
                    bluetoothManager.startDiscovery();
                } else {
                    requestBluetoothPermissions();
                }
            } else {
                Toast.makeText(this, "请先启用蓝牙", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnAccept.setOnClickListener(v -> {
            if (bluetoothManager.isBluetoothEnabled()) {
                bluetoothManager.accept();
            } else {
                Toast.makeText(this, "请先启用蓝牙", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            EditText messageInput = binding.etMessage;
            String message = messageInput.getText().toString();
            if (!message.isEmpty() && bluetoothManager.isConnected()) {
                byte[] data = message.getBytes();
                bluetoothManager.sendData(data, data.length);
                messageInput.setText("");
            } else if (!bluetoothManager.isConnected()) {
                Toast.makeText(this, "请先连接设备", Toast.LENGTH_SHORT).show();
            }
        });

        binding.btnDisconnect.setOnClickListener(v -> {
            bluetoothManager.disconnect();
        });

        binding.swPassthrough.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                ttyManager.addPassthrough(bluetoothManager);
            } else {
                ttyManager.removePassthrough(bluetoothManager);
            }
        });

        // 添加蓝牙事件监听器
        bluetoothManager.addListener(bluetoothListener);

        if (bluetoothManager.isConnected()) {
            BluetoothDevice device = bluetoothManager.getConnectedDevice();
            if (device != null) {
                binding.tvStatus.setText("已连接到 " + getDeviceName(device));
            }
        } else {
            binding.tvStatus.setText(bluetoothManager.isBluetoothEnabled() ? "蓝牙已启用" : "蓝牙已禁用");
        }

        requestBluetoothPermissions();
    }

    // 请求蓝牙权限
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = BluetoothSppManager.getRequiredBluetoothPermissions();
            requestPermissions(permissions, REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "需要蓝牙权限才能使用该功能", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String getDeviceName(@Nullable BluetoothDevice device) {
        if (device == null) {
            return "SPP Server";
        }
        @SuppressLint("MissingPermission")
        String name = device.getName();
        return name != null ? name : device.getAddress();
    }

    // 蓝牙事件监听器
    private final BluetoothSppManager.BluetoothSppListener bluetoothListener = new BluetoothSppManager.SimpleBluetoothSppListener() {
        @Override
        public void onBluetoothStateChanged(boolean enabled) {
            runOnUiThread(() -> {
                binding.tvStatus.setText(enabled ? "蓝牙已启用" : "蓝牙已禁用");
            });
        }

        @Override
        public void onDeviceDiscovered(BluetoothDevice device) {
            runOnUiThread(() -> {
                // 避免重复添加设备
                for (BluetoothDevice existingDevice : discoveredDevices) {
                    if (existingDevice.getAddress().equals(device.getAddress())) {
                        return;
                    }
                }
                discoveredDevices.add(device);
                deviceAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public void onDiscoveryStarted() {
            runOnUiThread(() -> {
                binding.tvStatus.setText("正在搜索设备...");
                binding.progressBar.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void onDiscoveryFinished() {
            runOnUiThread(() -> {
                binding.tvStatus.setText("搜索完成");
                binding.progressBar.setVisibility(View.GONE);
            });
        }

        @Override
        public void onConnecting(@Nullable BluetoothDevice device) {
            runOnUiThread(() -> {
                if (device != null) {
                    binding.tvStatus.setText("正在连接到 " + getDeviceName(device) + "...");
                } else {
                    binding.tvStatus.setText("Server正在等待连接请求");
                }
            });
        }

        @Override
        public void onConnected(@Nullable BluetoothDevice device) {
            runOnUiThread(() -> {
                binding.tvStatus.setText("已连接到 " + getDeviceName(device));
                Toast.makeText(Tty2BluetoothActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onConnectionFailed(BluetoothDevice device, String message) {
            runOnUiThread(() -> {
                binding.tvStatus.setText("连接失败: " + message);
                Toast.makeText(Tty2BluetoothActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onAcceptFailed(String message) {
            runOnUiThread(() -> {
                binding.tvStatus.setText("Server已停止");
            });
        }

        @Override
        public void onDisconnected() {
            runOnUiThread(() -> {
                binding.tvStatus.setText("已断开连接");
            });
        }

        @Override
        public void onDataReceived(byte[] data, int length) {
            runOnUiThread(() -> {
                String message = new String(data, 0, length);
                binding.tvReceived.append("rev: " + message + "\n");

                // 自动滚动到底部
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            });
        }

        @Override
        public void onDataSent(byte[] data, int length) {
            runOnUiThread(() -> {
                String message = new String(data, 0, length);
                binding.tvReceived.append("send: " + message + "\n");

                // 自动滚动到底部
                binding.scrollView.fullScroll(View.FOCUS_DOWN);
            });
        }

        @Override
        public void onError(String message) {
            runOnUiThread(() -> {
                Toast.makeText(Tty2BluetoothActivity.this, "错误: " + message, Toast.LENGTH_SHORT).show();
            });
        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源
        if (binding.swPassthrough.isChecked()) {
            ttyManager.removePassthrough(bluetoothManager);
        }
        bluetoothManager.removeListener(bluetoothListener);
        bluetoothManager.release();
    }

    // 设备列表适配器
    private static class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {
        private final List<BluetoothDevice> devices;
        private final OnDeviceClickListener listener;

        interface OnDeviceClickListener {
            void onDeviceClick(BluetoothDevice device);
        }

        DeviceAdapter(List<BluetoothDevice> devices, OnDeviceClickListener listener) {
            this.devices = devices;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            BluetoothDevice device = devices.get(position);
            @SuppressLint("MissingPermission")
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "未知设备";
            }
            holder.text1.setText(deviceName);
            holder.text2.setText(device.getAddress());

            holder.itemView.setOnClickListener(v -> listener.onDeviceClick(device));
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;

            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
            }
        }
    }
}