package com.coolfly.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.ProtocolHelper;

public class V4PwrActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_pwr);

        androidx.appcompat.widget.SwitchCompat swAuto = findViewById(R.id.sw_auto);
        EditText etPwrInit = findViewById(R.id.et_pwr_init);
        EditText etPwrMin = findViewById(R.id.et_pwr_min);
        EditText etPwrMax = findViewById(R.id.et_pwr_max);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSet = findViewById(R.id.btn_set_pwr);

        btnSet.setOnClickListener(v -> {
            boolean auto = swAuto.isChecked();
            boolean remote = swRemote.isChecked();
            try {
                int pwrInit = 0;
                int pwrMin = 0;
                int pwrMax = 0;
                if (!auto) {
                    if (TextUtils.isEmpty(etPwrInit.getText())) {
                        Toast.makeText(this, "pwrInit is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pwrInit = Integer.parseInt(etPwrInit.getText().toString());
                } else {
                    if (TextUtils.isEmpty(etPwrMin.getText()) || TextUtils.isEmpty(etPwrMax.getText())) {
                        Toast.makeText(this, "pwrMin/pwrMax is empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    pwrMin = Integer.parseInt(etPwrMin.getText().toString());
                    pwrMax = Integer.parseInt(etPwrMax.getText().toString());
                }
                protocolHelper.ar8030SetPwr(auto, pwrInit, pwrMin, pwrMax, remote);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "format error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}


