package com.coolfly.demo;

import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityUdpRtpBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.MediaConfig;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.medialibrary.MediaHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UdpRtpActivity extends AppCompatActivity {
    private ActivityUdpRtpBinding binding;

    private MediaHelper mediaHelper;
    private boolean isPlaying = false;
    private MediaHelper.ENCODING encoding = MediaHelper.ENCODING.H264;
    private FFListenerManager ffListenerManager;
    /**
     *  Video decode channel, support 5 channels, from 1 to 5
     */
    private final int DECODE_CHANNEL = 2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUdpRtpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        String[] codecArr = new String[]{MediaHelper.ENCODING.H264.name(), MediaHelper.ENCODING.H265.name()};
        ArrayAdapter<String> decodeModeAdapter = new ArrayAdapter<String>(this, R.layout.item_select, codecArr);
        decodeModeAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spCodec.setAdapter(decodeModeAdapter);

        binding.spCodec.setSelection(0);
        binding.spCodec.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    encoding = MediaHelper.ENCODING.H264;
                } else {
                    encoding = MediaHelper.ENCODING.H265;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        MediaConfig mediaConfig = PreferenceActivity.preferenceObject.mediaConfig;

        ffListenerManager = FFListenerManager.addListener(MainApplication.applicationContext, ffListener);
        mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, binding.surface, DECODE_CHANNEL);
        mediaHelper.setMediaConfig(mediaConfig);

        binding.tvOperate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    isPlaying = false;
                    binding.tvOperate.setText(R.string.play);
                    mediaHelper.stopPlayUDP();
                } else {
                    String port = binding.etPort.getText().toString().trim();
                    try {
                        int portInt = Integer.parseInt(port);
                        if (MediaHelper.isPortAvailable(portInt)) {
                            boolean res = mediaHelper.playUDP(encoding, portInt);
                            if (!res) {
                                Toast.makeText(UdpRtpActivity.this, R.string.surface_unavailable, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            isPlaying = true;
                            binding.tvOperate.setText(R.string.stop);
                            binding.tvDecodeMode.setText(R.string.decode_mode_hw);
                        } else {
                            Toast.makeText(UdpRtpActivity.this, R.string.url_error, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        });

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
        isPlaying = false;
        binding.tvOperate.setText(R.string.play);
        ffListenerManager.removeListener();
        mediaHelper.destroy();
    }

    public void onClick(View view) {
        if (view == binding.btnShot) {
            String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/shot";
            File fileDir = new File(path);
            fileDir.mkdirs();
            File file = new File(fileDir, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".jpg");
            try {
                file.createNewFile();
                FFJNI.shotFrame(file.getAbsolutePath(), DECODE_CHANNEL);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (view == binding.btnStartRecord) {
            String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
            File fileDir = new File(path);
            fileDir.mkdirs();
            File file = new File(fileDir, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".mp4");
            try {
                file.createNewFile();
                FFJNI.startRecordVideo(file.getAbsolutePath(), DECODE_CHANNEL);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (view == binding.btnStopRecord) {
            // Retrieve the record result via FFListener.onRecordVideo
            FFJNI.stopRecord(DECODE_CHANNEL);
        }
    }
    private final FFListener ffListener = new FFListener() {
        @Override
        public void onMediaFormat(String format, int width, int height, int frameRateNum, int frameRateDen, long bitRate, int handler) {
            int frameRate = frameRateDen == 0? 0: frameRateNum / frameRateDen;
            if (handler == DECODE_CHANNEL) {
                ViewGroup.LayoutParams layoutParams = binding.surface.getLayoutParams();
                float aspectRatio = ((float) binding.fl.getWidth()) / binding.fl.getHeight();
                float aspectRatioNew = ((float) width) / height;
                if (aspectRatio > aspectRatioNew) {
                    float realWidth = ((float) (binding.fl.getHeight())) * aspectRatioNew;
                    layoutParams.width = (int) realWidth;
                    layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                } else {
                    float realHeight = ((float) (binding.fl.getWidth())) / aspectRatioNew;
                    layoutParams.height = (int) realHeight;
                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                }
                binding.surface.requestLayout();
                mediaHelper.updateVideoSize(width, height);
            }
        }

        @Override
        public void onDowngradeToSwDecode(int handler) {
            binding.tvDecodeMode.setText(R.string.decode_mode_sw);
        }
    };
}