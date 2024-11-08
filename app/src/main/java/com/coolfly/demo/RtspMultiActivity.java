package com.coolfly.demo;


import static com.coolfly.demo.utils.Constants.DEFAULT_MULTI_RTSP_URI_1;
import static com.coolfly.demo.utils.Constants.DEFAULT_MULTI_RTSP_URI_2;
import static com.coolfly.demo.utils.Constants.DEFAULT_MULTI_RTSP_URI_3;
import static com.coolfly.demo.utils.Constants.DEFAULT_MULTI_RTSP_URI_4;
import static com.coolfly.demo.utils.Constants.PREF_MEDIA_CONFIG;
import static com.coolfly.demo.utils.Constants.PREF_MULTI_RTSP_URI_1;
import static com.coolfly.demo.utils.Constants.PREF_MULTI_RTSP_URI_2;
import static com.coolfly.demo.utils.Constants.PREF_MULTI_RTSP_URI_3;
import static com.coolfly.demo.utils.Constants.PREF_MULTI_RTSP_URI_4;
import static com.coolfly.demo.utils.Constants.SP_NAME;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.coolfly.demo.databinding.ActivityRtspMultiBinding;
import com.fly.fflibrary.MediaConfig;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.medialibrary.MediaHelper;

public class RtspMultiActivity extends AppCompatActivity {
    private ActivityRtspMultiBinding binding;

    private MediaHelper mediaHelper1;
    private MediaHelper mediaHelper2;
    private MediaHelper mediaHelper3;
    private MediaHelper mediaHelper4;

    private boolean isHw1 = true;
    private boolean isHw2 = true;
    private boolean isHw3 = true;
    private boolean isHw4 = true;

    private boolean isPlaying1 = false;
    private boolean isPlaying2 = false;
    private boolean isPlaying3 = false;
    private boolean isPlaying4 = false;

    private FFListenerManager ffListenerManager;
    /**
     *  Video decode channel, support 5 channels, from 1 to 5
     */
    private final int DECODE_CHANNEL1 = 2;
    private final int DECODE_CHANNEL2 = 3;
    private final int DECODE_CHANNEL3 = 4;
    private final int DECODE_CHANNEL4 = 5;

    private int selectedChannel = DECODE_CHANNEL1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRtspMultiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String[] channels = getResources().getStringArray(R.array.channel);
        ArrayAdapter<String> jpAmAdapter = new ArrayAdapter<String>(this, R.layout.item_select_light, channels);
        jpAmAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spChannel.setAdapter(jpAmAdapter);

        binding.spChannel.setSelection(0);
        binding.spChannel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedChannel = position + 2;

                String prefKey = PREF_MULTI_RTSP_URI_1;
                boolean isTcp = false;
                boolean isHw = isHw1;
                boolean isPlaying = isPlaying1;
                String defaultUrl = DEFAULT_MULTI_RTSP_URI_1;
                switch (selectedChannel) {
                    case DECODE_CHANNEL1:
                        prefKey = PREF_MULTI_RTSP_URI_1;
                        isTcp = mediaHelper1.isRtspTcp();
                        isHw = isHw1;
                        isPlaying = isPlaying1;
                        defaultUrl = DEFAULT_MULTI_RTSP_URI_1;
                        break;
                    case DECODE_CHANNEL2:
                        prefKey = PREF_MULTI_RTSP_URI_2;
                        isTcp = mediaHelper2.isRtspTcp();
                        isHw = isHw2;
                        isPlaying = isPlaying2;
                        defaultUrl = DEFAULT_MULTI_RTSP_URI_2;
                        break;
                    case DECODE_CHANNEL3:
                        prefKey = PREF_MULTI_RTSP_URI_3;
                        isTcp = mediaHelper3.isRtspTcp();
                        isHw = isHw3;
                        isPlaying = isPlaying3;
                        defaultUrl = DEFAULT_MULTI_RTSP_URI_3;
                        break;
                    case DECODE_CHANNEL4:
                        prefKey = PREF_MULTI_RTSP_URI_4;
                        isTcp = mediaHelper4.isRtspTcp();
                        isHw = isHw4;
                        isPlaying = isPlaying4;
                        defaultUrl = DEFAULT_MULTI_RTSP_URI_4;
                        break;
                }

                String packageName = MainApplication.applicationContext.getPackageName();
                SharedPreferences sp = MainApplication.applicationContext.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);
                String uri = sp.getString(prefKey, defaultUrl);
                if (!TextUtils.isEmpty(uri)) {
                    binding.etUri.setText(uri);
                }

                binding.cbTcp.setOnCheckedChangeListener(null);
                binding.cbTcp.setChecked(isTcp);
                setTcpListener();

                binding.tvDecodeMode.setText(isHw ? R.string.decode_mode_hw : R.string.decode_mode_sw);

                binding.tvOperate.setText(isPlaying? R.string.stop : R.string.play);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        String uri = sp.getString(PREF_MULTI_RTSP_URI_1, "rtsp://127.0.0.1:8554/main");
        if (!TextUtils.isEmpty(uri)) {
            binding.etUri.setText(uri);
        }

        MediaConfig mediaConfig = null;
        try {
            mediaConfig = JSON.parseObject(sp.getString(PREF_MEDIA_CONFIG, null), MediaConfig.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (mediaConfig == null) {
            mediaConfig = new MediaConfig();
        }

        ffListenerManager = FFListenerManager.addListener(MainApplication.applicationContext, ffListener);
        mediaHelper1 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, binding.vv1, DECODE_CHANNEL1);
        mediaHelper1.setMediaConfig(mediaConfig);

        mediaHelper2 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, binding.vv2, DECODE_CHANNEL2);
        mediaHelper2.setMediaConfig(mediaConfig);

        mediaHelper3 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, binding.vv3, DECODE_CHANNEL3);
        mediaHelper3.setMediaConfig(mediaConfig);

        mediaHelper4 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, binding.vv4, DECODE_CHANNEL4);
        mediaHelper4.setMediaConfig(mediaConfig);

        binding.tvOperate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isPlaying = false;
                MediaHelper mediaHelper = mediaHelper1;
                switch (selectedChannel) {
                    case DECODE_CHANNEL1:
                        isPlaying = isPlaying1;
                        mediaHelper = mediaHelper1;
                        break;
                    case DECODE_CHANNEL2:
                        isPlaying = isPlaying2;
                        mediaHelper = mediaHelper2;
                        break;
                    case DECODE_CHANNEL3:
                        isPlaying = isPlaying3;
                        mediaHelper = mediaHelper3;
                        break;
                    case DECODE_CHANNEL4:
                        isPlaying = isPlaying4;
                        mediaHelper = mediaHelper4;
                        break;
                }

                if (isPlaying) {
                    setIsPlaying(false);
                    binding.tvOperate.setText(R.string.play);
                    mediaHelper.stopPlayFile();
                } else {
                    String uri = binding.etUri.getText().toString().trim();
                    if (!TextUtils.isEmpty(uri) && uri.startsWith("rtsp://")) {
                        boolean res = mediaHelper.playFile(uri);
                        if (!res) {
                            Toast.makeText(RtspMultiActivity.this, R.string.surface_unavailable, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setIsPlaying(true);
                        binding.tvOperate.setText(R.string.stop);
                        binding.tvDecodeMode.setText(R.string.decode_mode_hw);


                        String prefKey = PREF_MULTI_RTSP_URI_1;
                        switch (selectedChannel) {
                            case DECODE_CHANNEL1:
                                prefKey = PREF_MULTI_RTSP_URI_1;
                                break;
                            case DECODE_CHANNEL2:
                                prefKey = PREF_MULTI_RTSP_URI_2;
                                break;
                            case DECODE_CHANNEL3:
                                prefKey = PREF_MULTI_RTSP_URI_3;
                                break;
                            case DECODE_CHANNEL4:
                                prefKey = PREF_MULTI_RTSP_URI_4;
                                break;
                        }
                        String packageName = MainApplication.applicationContext.getPackageName();
                        SharedPreferences sp = MainApplication.applicationContext.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(prefKey, uri);
                        editor.apply();
                    } else {
                        Toast.makeText(RtspMultiActivity.this, R.string.url_error, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        setTcpListener();
    }

    private void setTcpListener() {
        binding.cbTcp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                switch (selectedChannel) {
                    case DECODE_CHANNEL1:
                        mediaHelper1.setRtspTcp(isChecked);
                        break;
                    case DECODE_CHANNEL2:
                        mediaHelper2.setRtspTcp(isChecked);
                        break;
                    case DECODE_CHANNEL3:
                        mediaHelper3.setRtspTcp(isChecked);
                        break;
                    case DECODE_CHANNEL4:
                        mediaHelper4.setRtspTcp(isChecked);
                        break;
                }
            }
        });
    }

    private void setIsPlaying(boolean isPlaying) {
        switch (selectedChannel) {
            case DECODE_CHANNEL1:
                isPlaying1 = isPlaying;
                break;
            case DECODE_CHANNEL2:
                isPlaying2 = isPlaying;
                break;
            case DECODE_CHANNEL3:
                isPlaying3 = isPlaying;
                break;
            case DECODE_CHANNEL4:
                isPlaying4 = isPlaying;
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.cbTcp.setOnCheckedChangeListener(null);
        isPlaying1 = false;
        isPlaying2 = false;
        isPlaying3 = false;
        isPlaying4 = false;
        binding.tvOperate.setText(R.string.play);
        ffListenerManager.removeListener();
        mediaHelper1.destroy();
        mediaHelper2.destroy();
        mediaHelper3.destroy();
        mediaHelper4.destroy();
    }

    private final FFListener ffListener = new FFListener() {
        @Override
        public void onMediaFormat(String format, int width, int height, int frameRateNum, int frameRateDen, long bitRate, int handler) {
            SurfaceView surface = null;
            FrameLayout fl = null;
            MediaHelper mediaHelper = null;
            switch (handler) {
                case DECODE_CHANNEL1:
                    surface = binding.vv1;
                    fl = binding.fl1;
                    mediaHelper = mediaHelper1;
                    break;
                case DECODE_CHANNEL2:
                    surface = binding.vv2;
                    fl = binding.fl2;
                    mediaHelper = mediaHelper2;
                    break;
                case DECODE_CHANNEL3:
                    surface = binding.vv3;
                    fl = binding.fl3;
                    mediaHelper = mediaHelper3;
                    break;
                case DECODE_CHANNEL4:
                    surface = binding.vv4;
                    fl = binding.fl4;
                    mediaHelper = mediaHelper4;
                    break;
            }
            ViewGroup.LayoutParams layoutParams = surface.getLayoutParams();
            float aspectRatio = ((float) fl.getWidth()) / fl.getHeight();
            float aspectRatioNew = ((float) width) / height;
            if (aspectRatio > aspectRatioNew) {
                float realWidth = ((float) (fl.getHeight())) * aspectRatioNew;
                layoutParams.width = (int) realWidth;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            } else {
                float realHeight = ((float) (fl.getWidth())) / aspectRatioNew;
                layoutParams.height = (int) realHeight;
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            }
            surface.requestLayout();
            mediaHelper.updateVideoSize(width, height);
        }

        @Override
        public void onDowngradeToSwDecode(int handler) {
            switch (handler) {
                case DECODE_CHANNEL1:
                    isHw1 = false;
                    break;
                case DECODE_CHANNEL2:
                    isHw2 = false;
                    break;
                case DECODE_CHANNEL3:
                    isHw3 = false;
                    break;
                case DECODE_CHANNEL4:
                    isHw4 = false;
                    break;
            }
            if (selectedChannel == handler) {
                binding.tvDecodeMode.setText(R.string.decode_mode_sw);
            }
        }
    };
}