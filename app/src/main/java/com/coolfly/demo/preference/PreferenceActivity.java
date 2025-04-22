package com.coolfly.demo.preference;

import static com.coolfly.demo.utils.Constants.PREF_APP_CONFIG;
import static com.coolfly.demo.utils.Constants.SP_NAME;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.serialport.SerialPortFinder;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.coolfly.demo.MainApplication;
import com.coolfly.demo.R;
import com.coolfly.demo.chuanyun.preference.SerialPortPreferences;
import com.coolfly.demo.chuanyun.preference.SocketPreferences;
import com.coolfly.demo.databinding.ActivityPreferenceBinding;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.MediaConfig;
import com.fly.station.chuanyun.SensorDevice;
import com.fly.station.mcu.McuManager;
import com.fly.station.prorocol.Constants;
import com.fly.station.prorocol.ProtocolHelper;

public class PreferenceActivity extends AppCompatActivity {
    public static PreferenceObject preferenceObject = null;

    private ActivityPreferenceBinding binding;
    private static SharedPreferences sp;

    public static void initPreference() {
        sp = MainApplication.applicationContext.getSharedPreferences(SP_NAME, MODE_PRIVATE);
        try {
            preferenceObject = JSON.parseObject(sp.getString(PREF_APP_CONFIG, null), PreferenceObject.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (preferenceObject == null) {
            preferenceObject = new PreferenceObject();
        }
    }

    public static void savePreference() {
        if (preferenceObject != null) {
            sp.edit().putString(PREF_APP_CONFIG, JSON.toJSONString(preferenceObject)).apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvMcuPath.setText(McuManager.DEVICE_PATH);
        binding.tvMcuPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] paths = new SerialPortFinder().getAllDevicesPath();
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("device path")
                        .setItems(paths, (dialog, which) -> {
                            McuManager.setDevicePath(paths[which]);
                            binding.tvMcuPath.setText(McuManager.DEVICE_PATH);
                            preferenceObject.mcu_serial_path = McuManager.DEVICE_PATH;
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.tvMcuBaudrate.setText(McuManager.BAUDRATE);
        binding.tvMcuBaudrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] baudrates = getResources().getStringArray(R.array.baudrates_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("baudrate")
                        .setItems(baudrates, (dialog, which) -> {
                            McuManager.setBaudRate(baudrates[which]);
                            binding.tvMcuBaudrate.setText(McuManager.BAUDRATE);
                            preferenceObject.mcu_serial_baudrate = Integer.parseInt(McuManager.BAUDRATE);
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.tvP201.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PreferenceActivity.this, SerialPortPreferences.class);
                startActivity(intent);
            }
        });

        binding.tvP301.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PreferenceActivity.this, SocketPreferences.class);
                startActivity(intent);
            }
        });

        binding.tvP401Port.setText(String.valueOf(preferenceObject.p401_port));
        binding.tvP401Port.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] ports = getResources().getStringArray(R.array.p401portss_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("port")
                        .setItems(ports, (dialog, which) -> {
                            int port = Integer.parseInt(ports[which]);
                            ProtocolHelper.ar8030SetPort(port);
                            binding.tvP401Port.setText(ports[which]);
                            preferenceObject.p401_port = port;
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.swLogMcu.setChecked(McuManager.isShowLog);
        binding.swLogMcu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            McuManager.setIsShowLog(isChecked);
            preferenceObject.show_mcu_log = isChecked;
            savePreference();
        });

        binding.swLogFfmpeg.setChecked(preferenceObject.show_ffmpeg_log);
        binding.swLogFfmpeg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FFJNI.setLog(isChecked);
            preferenceObject.show_ffmpeg_log = isChecked;
            savePreference();
        });

        binding.swLogUsb.setChecked(UsbDeviceHelper.isShowLog);
        binding.swLogUsb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UsbDeviceHelper.isShowLog = isChecked;
            preferenceObject.show_usb_log = isChecked;
            savePreference();
        });

        binding.swLogChuanyun.setChecked(SensorDevice.isShowLog);
        binding.swLogChuanyun.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorDevice.isShowLog = isChecked;
            preferenceObject.show_chuanyun_log = isChecked;
            savePreference();
        });

        binding.swLogAr8030Vpn.setChecked(Constants.isShowAR8030VPNLog);
        binding.swLogAr8030Vpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030VPNLog = isChecked;
            preferenceObject.show_ar8030_vpn_log = isChecked;
            savePreference();
        });

        binding.swLogAr8030Parse.setChecked(Constants.isShowAR8030ParseLog);
        binding.swLogAr8030Parse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030ParseLog = isChecked;
            preferenceObject.show_ar8030_parse_log = isChecked;
            savePreference();
        });

        /*
         * MediaConfig类用于配置媒体流的参数。
         * 字段说明：
         * - rtsp_timeout_us: 表示socket超时时间，单位为微秒。默认值为2000000微秒（2秒）。
         * - is_rtsp_tcp: 表示RTSP协议是否使用TCP传输。默认值为false，表示使用UDP传输。
         * - probe_size: 表示从输入中读取以确定流属性的最大字节数。默认值为1048576字节（1MB）。
         * - max_analyze_duration: 表示最大分析持续时间。0表示自动检测，其他值表示AV_TIME_BASE的倍数。默认值为0，表示自动。
         * - notify_i_p_frame_last_bytes: 表示返回I帧（IDR和Slice中的I）和P帧的最后几个字节，一般用于存储视频帧中的AI标记信息。<=0表示不返回，>0表示返回的字节数。默认值为0。
         * - is_fast_resume: SurfaceView重新创建后是否快速恢复播放。默认值为true。原理：SurfaceView销毁后，不释放流，并缓存流信息。SurfaceView重新创建后，如果流可用，直接用缓存的流信息恢复播放。如果流不可用，降级为重新创建流，并重新解析流信息。
         */
        binding.etMediaConfig.setText(JSON.toJSONString(preferenceObject.mediaConfig));

        binding.tvSaveMediaConfig.setOnClickListener(v -> {
            MediaConfig mediaConfig = null;
            try {
                mediaConfig = JSON.parseObject(binding.etMediaConfig.getText().toString(), MediaConfig.class);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (mediaConfig == null) {
                Toast.makeText(PreferenceActivity.this, "mediaConfig is invalid", Toast.LENGTH_SHORT).show();
                return;
            }
            preferenceObject.mediaConfig = mediaConfig;
            savePreference();
            Toast.makeText(PreferenceActivity.this, "save mediaConfig success", Toast.LENGTH_SHORT).show();
        });
    }
}