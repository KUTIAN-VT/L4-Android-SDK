package com.coolfly.demo;

import static com.coolfly.demo.utils.Constants.PREF_RTSP_URI;
import static com.coolfly.demo.utils.ImageUtils.saveBitmap;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wuadam.fflibrary.FFJNI;
import com.wuadam.fflibrary.listeners.FFListener;
import com.wuadam.fflibrary.listeners.FFListenerManager;
import com.wuadam.medialibrary.MediaHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class RtspSingleActivity extends AppCompatActivity {

    private EditText etUri;
    private CheckBox cbTcp;
    private TextView tvOperate;
    private TextView tvDecodeMode;
    private FrameLayout fl;
    private SurfaceView surface;

    private Button btnShot;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private Spinner spDecodeMode;

    private MediaHelper mediaHelper;
    private boolean isPlaying = false;
    private FFListenerManager ffListenerManager;
    /**
     *  support 5 channels, from 1 to 5
     */
    private final int DECODE_CHANNEL = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rtsp_single);

        etUri = findViewById(R.id.et_uri);
        cbTcp = findViewById(R.id.cb_tcp);
        tvOperate = findViewById(R.id.tv_operate);
        tvDecodeMode = findViewById(R.id.tv_decode_mode);
        fl = findViewById(R.id.fl);
        surface = findViewById(R.id.surface);

        btnShot = findViewById(R.id.btn_shot);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);
        spDecodeMode = findViewById(R.id.sp_decode_mode);

        String packageName = MainApplication.applicationContext.getPackageName();
        SharedPreferences sp = MainApplication.applicationContext.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);
        String uri = sp.getString(PREF_RTSP_URI, "rtsp://127.0.0.1:8554/main");
        if (!TextUtils.isEmpty(uri)) {
            etUri.setText(uri);
        }

        ffListenerManager = FFListenerManager.addListener(MainApplication.applicationContext, ffListener);
        mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE_PATH, null, surface, null, null, DECODE_CHANNEL);

        tvOperate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPlaying) {
                    isPlaying = false;
                    tvOperate.setText(R.string.play);
                    mediaHelper.stopPlayFile();
                } else {
                    String uri = etUri.getText().toString().trim();
                    if (!TextUtils.isEmpty(uri) && uri.startsWith("rtsp://")) {
                        boolean res = mediaHelper.playFile(uri);
                        if (!res) {
                            Toast.makeText(RtspSingleActivity.this, R.string.surface_unavailable, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        isPlaying = true;
                        tvOperate.setText(R.string.stop);
                        tvDecodeMode.setText(R.string.decode_mode_hw);


                        String packageName = MainApplication.applicationContext.getPackageName();
                        SharedPreferences sp = MainApplication.applicationContext.getSharedPreferences(packageName + "_preferences", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString(PREF_RTSP_URI, uri);
                        editor.apply();
                    } else {
                        Toast.makeText(RtspSingleActivity.this, R.string.url_error, Toast.LENGTH_SHORT).show();
                    }
                }

                tvOperate.setEnabled(false);
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (tvOperate != null) {
                            tvOperate.setEnabled(true);
                        }
                    }
                }, 1000);
            }
        });

        cbTcp.setChecked(mediaHelper.isRtspTcp());
        cbTcp.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mediaHelper.setRtspTcp(isChecked);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cbTcp.setOnCheckedChangeListener(null);
        isPlaying = false;
        tvOperate.setText(R.string.play);
        ffListenerManager.removeListener();
        mediaHelper.destroy();
    }

    public void onClick(View view) {
        if (view == btnShot) {
            switch (mediaHelper.getDecodeMode()) {
                case FF_SWS_SURFACE_PATH:
                case FF_GL_SURFACE_PATH: {
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
                }
                break;
                case FF_NDK_MEDIACODEC_SURFACE_PATH:
                case FF_DIRECT_SURFACE_PATH: {
                    // 直接渲染到Surface上的情况，无法从buffer中提取图像，只能从Surface上提取
                    Bitmap bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        PixelCopy.request(
                                surface, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                                    @Override
                                    public void onPixelCopyFinished(int copyResult) {
                                        if (copyResult == PixelCopy.SUCCESS) {
                                            Toast.makeText(MainApplication.applicationContext, R.string.take_photo_success, Toast.LENGTH_SHORT)
                                                    .show();
                                            saveBitmap(bitmap);
                                        } else {
                                            Toast.makeText(MainApplication.applicationContext, R.string.take_photo_fail, Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    }
                                }, new Handler(Looper.getMainLooper())
                        );
                    } else {
                        Toast.makeText(MainApplication.applicationContext, getString(R.string.take_photo_tip, mediaHelper.getDecodeMode().name()), Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
            }
        } else if (view == btnStartRecord) {
            String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
            switch (mediaHelper.getDecodeMode()) {
                case FF_SWS_SURFACE_PATH:
                case FF_GL_SURFACE_PATH:
                case FF_DIRECT_SURFACE_PATH:
                    File fileDir = new File(path);
                    fileDir.mkdirs();
                    File file = new File(fileDir, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".mp4");
                    try {
                        file.createNewFile();
                        FFJNI.startRecordVideo(file.getAbsolutePath(), DECODE_CHANNEL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case FF_NDK_MEDIACODEC_SURFACE_PATH:
                    // Not supported
                    break;
            }
        } else if (view == btnStopRecord) {
            switch (mediaHelper.getDecodeMode()) {
                case FF_SWS_SURFACE_PATH:
                case FF_GL_SURFACE_PATH:
                case FF_DIRECT_SURFACE_PATH:
                    FFJNI.stopRecord(DECODE_CHANNEL);
                    // 在回调里面toast和保存到相册
                    break;
                case FF_NDK_MEDIACODEC_SURFACE_PATH:
                    // Not supported
                    break;
            }
        }
    }
    private FFListener ffListener = new FFListener() {
        @Override
        public void onMediaFormat(String format, int width, int height, long bitRate, int handler) {
            if (handler == 5) {
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
            }
        }

        @Override
        public void onDowngradeToSwDecode(int handler) {
            tvDecodeMode.setText(R.string.decode_mode_sw);
        }
    };
}