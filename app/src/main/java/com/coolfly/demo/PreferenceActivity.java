package com.coolfly.demo;

import static com.coolfly.demo.utils.Constants.PREF_MEDIA_CONFIG;
import static com.coolfly.demo.utils.Constants.SP_NAME;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.coolfly.demo.databinding.ActivityPreferenceBinding;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.MediaConfig;
import com.fly.station.prorocol.Constants;

public class PreferenceActivity extends AppCompatActivity {
    public static boolean isShowFfmpegLog = false;

    private ActivityPreferenceBinding binding;
    private SharedPreferences sp;
    private MediaConfig mediaConfig = null;

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

        binding.swLogAr8030Vpn.setChecked(Constants.isShowAR8030VPNLog);
        binding.swLogAr8030Vpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030VPNLog = isChecked;
        });

        binding.swLogAr8030Parse.setChecked(Constants.isShowAR8030ParseLog);
        binding.swLogAr8030Parse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030ParseLog = isChecked;
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
        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        try {
            mediaConfig = JSON.parseObject(sp.getString(PREF_MEDIA_CONFIG, null), MediaConfig.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (mediaConfig == null) {
            mediaConfig = new MediaConfig();
        }
        binding.etMediaConfig.setText(JSON.toJSONString(mediaConfig));

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
            sp.edit().putString(PREF_MEDIA_CONFIG, JSON.toJSONString(mediaConfig)).apply();
            Toast.makeText(PreferenceActivity.this, "save mediaConfig success", Toast.LENGTH_SHORT).show();
        });
    }
}