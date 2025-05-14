package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityUdpBinding;
import com.fly.station.udp.UdpController;

public class UdpActivity extends AppCompatActivity {

    private ActivityUdpBinding binding;

    private UdpController udpController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUdpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 初始化UDP控制器
        udpController = new UdpController();
        udpController.setListener(new UdpController.UdpListener() {
            @Override
            public void onConnectionStateChanged(boolean isConnected, String errorMsg) {
                if (isConnected) {
                    binding.tvStatus.setText("已连接");
                    binding.btnConnect.setText("断开");
                } else {
                    binding.tvStatus.setText(errorMsg != null ? "未连接: " + errorMsg : "未连接");
                    binding.btnConnect.setText("连接");
                }
            }

            @Override
            public void onDataReceived(byte[] data, String address, int port) {
                runOnUiThread(() -> {
                    String message = new String(data);
                    binding.tvReceived.append(address + ":" + port + " : " + message + "\n");

                    // 自动滚动到底部
                    binding.scrollView.fullScroll(View.FOCUS_DOWN);
                });
            }

            @Override
            public void onError(String s) {

            }

//            @Override
//            public void onDataSent(boolean success, String errorMsg) {
//                if (success) {
//                    Toast.makeText(UdpActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
//                } else {
//                    Toast.makeText(UdpActivity.this, "发送失败: " + errorMsg, Toast.LENGTH_SHORT).show();
//                }
//            }
        });

        // 设置按钮点击事件
        binding.btnConnect.setOnClickListener(v -> {
            if (udpController.isConnected()) {
                udpController.disconnect();
            } else {
                String ip = binding.etIp.getText().toString();
                int port = Integer.parseInt(binding.etPort.getText().toString());
                if (binding.etLocalPort.getText().toString().length() > 0) {
                    int localPort = Integer.parseInt(binding.etLocalPort.getText().toString());
                    udpController.setTarget(ip, port).setLocalPort(localPort).connect();
                } else {
                    udpController.setTarget(ip, port).connect();
                }
            }
        });

        binding.btnSend.setOnClickListener(v -> {
            String message = binding.etMessage.getText().toString();
            if (!message.isEmpty()) {
                udpController.sendString(message);
                binding.etMessage.setText("");
            }
        });

    }

    @Override
    protected void onDestroy() {
        if (udpController != null && udpController.isConnected()) {
            udpController.disconnect();
        }
        super.onDestroy();
    }
}