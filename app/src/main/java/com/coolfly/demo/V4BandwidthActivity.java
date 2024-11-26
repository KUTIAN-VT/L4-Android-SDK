package com.coolfly.demo;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4BandwidthBinding;
import com.fly.station.prorocol.ProtocolHelper;

public class V4BandwidthActivity extends AppCompatActivity {

    private ActivityV4BandwidthBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4BandwidthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvSetDev2Ap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etRxBandwidth.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "rx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (binding.etTxBandwidth.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "tx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                protocolHelper.ar8030SetBandwidth(Integer.parseInt(binding.etRxBandwidth.getText().toString()), Integer.parseInt(binding.etTxBandwidth.getText().toString()));
            }
        });

        binding.tvResetDev2Ap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ResetBandwidth();
            }
        });

        binding.tvSetAp2Dev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (binding.etRxFramechange.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "rx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (binding.etTxFramechange.getText().toString().isEmpty()) {
                    Toast.makeText(V4BandwidthActivity.this, "tx buffer size is empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                protocolHelper.ar8030SetFrameChange(Integer.parseInt(binding.etRxFramechange.getText().toString()), Integer.parseInt(binding.etTxFramechange.getText().toString()));
            }
        });

        binding.tvResetAp2Dev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                protocolHelper.ar8030ResetFrameChange();
            }
        });
    }


}