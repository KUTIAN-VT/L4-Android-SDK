package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.coolfly.demo.databinding.ActivityV4ConfigBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.bean.BaseFlyPacket;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class V4ConfigActivity extends AppCompatActivity {

    private ActivityV4ConfigBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4ConfigBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvRead.setOnClickListener(v -> {
            if (!protocolHelper.ar8030GetConfigJson(binding.swRemote.isChecked())) {
                Toast.makeText(V4ConfigActivity.this, "read config is busy", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvWrite.setOnClickListener(v -> {
            if (binding.etJson.getText().toString().isEmpty()) {
                Toast.makeText(V4ConfigActivity.this, "config json is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            String json = binding.etJson.getText().toString();
            boolean isJsonOK = false;
            try {
                JSONObject jsonObject = JSON.parseObject(json);
                isJsonOK = jsonObject != null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!isJsonOK) {
                Toast.makeText(V4ConfigActivity.this, "json format error", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!protocolHelper.ar8030SetConfigJson(json, binding.swRemote.isChecked())) {
                Toast.makeText(V4ConfigActivity.this, "write config is busy", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvReset.setOnClickListener(v -> {
            protocolHelper.ar8030ResetConfigJson(binding.swRemote.isChecked());
        });

        binding.tvSave.setOnClickListener(v -> {
            // 保存binding.etJson中的内容到APP的私有目录中
            String content = binding.etJson.getText().toString();
            if (content.isEmpty()) {
                Toast.makeText(V4ConfigActivity.this, "config json is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                FileOutputStream fos = openFileOutput((binding.swRemote.isChecked()? "remote-" : "") + "config.json", MODE_PRIVATE);
                fos.write(content.getBytes());
                fos.flush();
                fos.close();
                Toast.makeText(V4ConfigActivity.this, "config saved", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(V4ConfigActivity.this, "failed to save config", Toast.LENGTH_SHORT).show();
            }
        });

        binding.tvRestore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 从APP的私有目录中读取config.json文件内容到binding.etJson中
                try {
                    String content = "";
                    byte[] buffer = new byte[100 * 1024];
                    int len;
                    FileInputStream fis = openFileInput((binding.swRemote.isChecked()? "remote-" : "") + "config.json");
                    while ((len = fis.read(buffer)) != -1) {
                        content += new String(buffer, 0, len);
                    }
                    fis.close();
                    binding.etJson.setText(content);
                    Toast.makeText(V4ConfigActivity.this, "config restored", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(V4ConfigActivity.this, "failed to restore config", Toast.LENGTH_SHORT).show();
                }
            }
        });

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

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
        public void onPairLost(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
            if (jsonString == null) {
                Toast.makeText(V4ConfigActivity.this, "read config json failed", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.etJson.setText(jsonString);
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
            if (result) {
                Toast.makeText(V4ConfigActivity.this, "write config json success", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4ConfigActivity.this, "write config json failed", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
            if (result) {
                Toast.makeText(V4ConfigActivity.this, "reset config json success", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(V4ConfigActivity.this, "reset config json failed", Toast.LENGTH_SHORT).show();
            }
        }
    };

}