package com.coolfly.demo;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityPreferenceBinding;
import com.coolfly.station.prorocol.AR8030Utils;
import com.coolfly.station.prorocol.AR8030VpnReader;
import com.wuadam.aoalibrary.host.UsbDeviceHelper;
import com.wuadam.fflibrary.FFJNI;

public class PreferenceActivity extends AppCompatActivity {
    public static boolean isShowFfmpegLog = false;

    private ActivityPreferenceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.swLogFfmpeg.setChecked(isShowFfmpegLog);
        binding.swLogFfmpeg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FFJNI.setLog(isChecked);
            isShowFfmpegLog = isChecked;
        });

        binding.swLogUsb.setChecked(UsbDeviceHelper.isShowLog);
        binding.swLogUsb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UsbDeviceHelper.isShowLog = isChecked;
        });

        binding.swLogAr8030Vpn.setChecked(AR8030VpnReader.isShowLog);
        binding.swLogAr8030Vpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AR8030VpnReader.isShowLog = isChecked;
        });

        binding.swLogAr8030Parse.setChecked(AR8030Utils.isShowLog);
        binding.swLogAr8030Parse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AR8030Utils.isShowLog = isChecked;
        });
    }
}