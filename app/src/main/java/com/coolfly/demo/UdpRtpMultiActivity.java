package com.coolfly.demo;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityUdpRtpMultiBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.fflibrary.MediaConfig;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.medialibrary.MediaHelper;

/**
 * UDP/RTP多通道播放示例
 * FFmpeg（libavformat/udp.c）在 接收端 还有一个容易被忽略的默认行为：
 * 如果 SDP 里写的是
 * m=video <port> UDP/AVP 96
 * FFmpeg 会 再打开 <port+1> 当作 RTCP 端口。
 * 也就是说，当 SDP 中 port=5600 时，它实际上会
 * bind 5600（RTP）
 * bind 5601（RTCP）
 * 同理，当 port=5601 时，它会
 * bind 5601（RTP）
 * bind 5602（RTCP）
 *
 * | 实例 | 预期   | 实际 bind   |
 * | -- | ---- | --------- |
 * | 1  | 5600 | 5600+5601 |
 * | 2  | 5601 | 5601+5602 |
 *
 * 于是 5601 端口被两个实例同时 bind（Linux 允许 SO_REUSEADDR），
 * 结果第 1 个实例把本应给第 2 个实例的 5601 RTP 报文也收走了，
 * 看起来就像“端口串流”。
 * 解决办法
 * 给每条流留出 2 个连续端口
 * 让两条流的 SDP port 至少相隔 2：
 * 第 1 路：SDP port = 5600（RTP 5600，RTCP 5601）
 * 第 2 路：SDP port = 5602（RTP 5602，RTCP 5603）
 */
public class UdpRtpMultiActivity extends AppCompatActivity {
    private ActivityUdpRtpMultiBinding binding;

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
        binding = ActivityUdpRtpMultiBinding.inflate(getLayoutInflater());
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

                MediaHelper.ENCODING encoding = null;
                int port = 0;
                boolean isHw = isHw1;
                boolean isPlaying = isPlaying1;
                switch (selectedChannel) {
                    case DECODE_CHANNEL1:
                        encoding = mediaHelper1.getUDPEncoding();
                        port = mediaHelper1.getUDPPort();
                        isHw = isHw1;
                        isPlaying = isPlaying1;
                        break;
                    case DECODE_CHANNEL2:
                        encoding = mediaHelper2.getUDPEncoding();
                        port = mediaHelper2.getUDPPort();
                        isHw = isHw2;
                        isPlaying = isPlaying2;
                        break;
                    case DECODE_CHANNEL3:
                        encoding = mediaHelper3.getUDPEncoding();
                        port = mediaHelper3.getUDPPort();
                        isHw = isHw3;
                        isPlaying = isPlaying3;
                        break;
                    case DECODE_CHANNEL4:
                        encoding = mediaHelper4.getUDPEncoding();
                        port = mediaHelper4.getUDPPort();
                        isHw = isHw4;
                        isPlaying = isPlaying4;
                        break;
                }

                if (MediaHelper.isPortAvailable(port)) {
                    binding.etPort.setText(port + "");
                } else {
                    binding.etPort.setText("");
                }

                if (encoding == null) {
                    encoding = MediaHelper.ENCODING.H264;
                }
                binding.spCodec.setSelection(encoding == MediaHelper.ENCODING.H264 ? 0 : 1);
                binding.tvDecodeMode.setText(isHw ? R.string.decode_mode_hw : R.string.decode_mode_sw);

                binding.tvOperate.setText(isPlaying? R.string.stop : R.string.play);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String[] codecArr = new String[]{MediaHelper.ENCODING.H264.name(), MediaHelper.ENCODING.H265.name()};
        ArrayAdapter<String> decodeModeAdapter = new ArrayAdapter<String>(this, R.layout.item_select, codecArr);
        decodeModeAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spCodec.setAdapter(decodeModeAdapter);
        binding.spCodec.setSelection(0);

        MediaConfig mediaConfig = PreferenceActivity.preferenceObject.mediaConfig;

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
                    mediaHelper.stopPlayUDP();
                } else {
                    String portStr = binding.etPort.getText().toString().trim();
                    int port = 0;
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    if (!TextUtils.isEmpty(portStr) && MediaHelper.isPortAvailable(port)) {
                        MediaHelper.ENCODING encoding = binding.spCodec.getSelectedItemPosition() == 0 ?
                                MediaHelper.ENCODING.H264 : MediaHelper.ENCODING.H265;
                        boolean res = mediaHelper.playUDP(encoding, port);
                        if (!res) {
                            Toast.makeText(UdpRtpMultiActivity.this, R.string.surface_unavailable, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        setIsPlaying(true);
                        binding.tvOperate.setText(R.string.stop);
                        binding.tvDecodeMode.setText(R.string.decode_mode_hw);
                    } else {
                        Toast.makeText(UdpRtpMultiActivity.this, R.string.url_error, Toast.LENGTH_SHORT).show();
                    }
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