package com.coolfly.demo.v3ota;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewParent;
import android.widget.ScrollView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.coolfly.demo.chuanyun.preference.SocketPreferences;
import com.coolfly.demo.databinding.ActivityV3OtaBinding;
import com.coolfly.station.ssh.OtaWorker;
import com.coolfly.station.ssh.VersionWorker;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class V3OtaActivity extends AppCompatActivity {
    private ActivityV3OtaBinding binding;
    private final int REQ_OTA = 1;
    private final int REQUEST_EXTERNAL_STORAGE = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV3OtaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSetSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(V3OtaActivity.this, SocketPreferences.class);
                startActivity(intent);
            }
        });

        binding.btnGndVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.tvMessage.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        VersionWorker.version(getIpPref(), false, onVersionListener);
                    }
                }).start();
            }
        });

        binding.btnSkyVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.tvMessage.setText("");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        VersionWorker.version(getIpPref(), true, onVersionListener);
                    }
                }).start();
            }
        });

        binding.btnOta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUpgradeFis(REQ_OTA);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_OTA && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream fis = getContentResolver().openInputStream(uri);
                if (fis != null) {
                    binding.btnOta.setEnabled(false);
                    binding.tvMessage.setText("");
                    OtaWorker.ota(V3OtaActivity.this, getIpPref(), fis, otaListener);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void getUpgradeFis(int requestCode) {
        if (verifyStoragePermissions()) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/octet-stream"); //设置bin/img后缀名
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, requestCode);
        }
    }

    private boolean verifyStoragePermissions() {
        // Check if we have permission
        int permission = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    private String getIpPref() {
        String packageName = getPackageName();
        SharedPreferences sp = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);
        return sp.getString("SOCKET_IP", "192.168.144.100").trim();
    }

    private final VersionWorker.OnVersionListener onVersionListener = new VersionWorker.OnVersionListener() {
        @Override
        public void onVersion(String version, boolean isSky) {
            binding.tvMessage.setText(version);
        }

        @Override
        public void onError(String error) {
            if (!TextUtils.isEmpty(error)) {
                binding.tvMessage.append("\n" + error);
            }
        }
    };

    private ProgressDialog otaProgressDialog;
    private final OtaWorker.OtaListener otaListener = new OtaWorker.OtaListener() {
        @Override
        public void onStart() {

        }

        @Override
        public void uploadStart() {
            otaProgressDialog = new ProgressDialog(V3OtaActivity.this, 0);
            otaProgressDialog.setIndeterminate(false);
            otaProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            otaProgressDialog.setMax(100);
            otaProgressDialog.show();
        }

        @Override
        public void uploadProgress(int progress) {
            if (otaProgressDialog != null) {
                otaProgressDialog.setProgress(progress);
            }
        }

        @Override
        public void uploadSuccess() {
            if (otaProgressDialog != null) {
                otaProgressDialog.dismiss();
            }
        }

        @Override
        public void onMessage(String message) {
            binding.tvMessage.append("\r\n" + message);

            ViewParent parent = binding.tvMessage.getParent();
            ScrollView parentView = (ScrollView) parent;
            parentView.smoothScrollTo(0, binding.tvMessage.getHeight());
        }

        @Override
        public void onFinish(boolean success, String errMsg) {
            binding.btnOta.setEnabled(true);
            if (!success) {
                if (otaProgressDialog != null) {
                    otaProgressDialog.dismiss();
                }
            }
        }
    };
}