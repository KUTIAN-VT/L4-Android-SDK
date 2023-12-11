package com.coolfly.demo;


import static com.coolfly.demo.utils.ImageUtils.saveBitmap;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.coolfly.demo.utils.Constants;
import com.coolfly.demo.utils.ImageUtils;
import com.coolfly.demo.utils.PermissionHelper;
import com.coolfly.station.listen.ArlinkDataListener;
import com.coolfly.station.listen.ArlinkListen;
import com.coolfly.station.prorocol.ProtocolHelper;
import com.coolfly.station.prorocol.ProtocolListener;
import com.coolfly.station.prorocol.UpgradeHelper;
import com.coolfly.station.prorocol.bean.ACK;
import com.coolfly.station.prorocol.bean.BaseCoolflyPacket;
import com.coolfly.station.prorocol.bean.DeviceInfo;
import com.coolfly.station.prorocol.bean.UartRx;
import com.coolfly.station.prorocol.bean.WirelessInfo;
import com.wuadam.aoalibrary.AoaSwitch;
import com.wuadam.aoalibrary.accessory.AccessoryHelper;
import com.wuadam.aoalibrary.accessory.AccessoryListener;
import com.wuadam.aoalibrary.host.UsbDeviceHelper;
import com.wuadam.aoalibrary.host.UsbDeviceListener;
import com.wuadam.fflibrary.FFJNI;
import com.wuadam.fflibrary.listeners.FFListener;
import com.wuadam.fflibrary.listeners.FFListenerManager;
import com.wuadam.medialibrary.BitRateHelper;
import com.wuadam.medialibrary.H264Extractor;
import com.wuadam.medialibrary.H264Saver;
import com.wuadam.medialibrary.MediaHelper;
import com.wuadam.medialibrary.MediaListener;
import com.wuadam.medialibrary.MuxerUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private AccessoryHelper accessoryHelper;
    private ArlinkListen arlinkListen;
    private UsbDeviceHelper usbDeviceHelper;
    private ProtocolHelper protocolHelper;
    /**
     * First video stream
     */
    private MediaHelper mediaHelper;
    /**
     * Second video stream
     */
    private MediaHelper mediaHelper2;
    private MuxerUtil muxerUtil;
    private FFListenerManager ffListenerManager;
    private BitRateHelper bitRateHelperVideo;
    private final boolean NEED_SAVE_H264 = false;
    private H264Saver h264Saver;
    private VideoMock videoMock, videoMock2;
    private PermissionHelper permissionHelper;
    private UpgradeHelper upgradeHelper;

    private ViewGroup rootView;
    private SurfaceView surface, surface2;
    private TextureView texture;
    private TextView tvBitrateVideo;
    private TextView widgetMap;
    private Button btnShot;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private Button btnHwDecoder;
    private SwitchCompat swMockVideo, swMockVideo2;
    private Spinner spDecodeMode;
    private SwitchCompat swHwDecode;
    private SwitchCompat swAoa;
    private SwitchCompat swFpv;
    private Button btnUpgradeGrd;
    private Button btnUpgradeSky;
    private TextView tvUpdateProcess;
    private Button btnRtsp;
    private Button btnRtspMulti;
    private Button btnChuanyun;

    private TextView tvVT = null;
    private TextView tvRC = null;
    private TextView tvRcScore = null;
    private TextView tvVtScore = null;
    private ImageView imageVT = null;
    private ImageView imageRC = null;
    private TextView tvOSDLocked = null;
    private TextView tvUart;

    private boolean isMapMini = true;

    private int decodeMode;

    private int mapWidgetHeight;
    private int mapWidgetWidth;
    private int mapWidgetMarginRight;
    private int mapWidgetMarginBottom;

    private int deviceWidth;
    private int deviceHeight;

    private int videoWidgetWidth;
    private int videoWidgetHeight;
    private MediaFormat mMediaFormat;
    private boolean isMediaCodecPlaying = false;
    private boolean isMediaCodecRecordFoundIFrame = false;

    private final int REQ_OTA_GRD = 1;
    private final int REQ_OTA_SKY = 2;

    /**
     *  Video decode channel 1, support 5 channels, from 1 to 5, shared between USB and RTSP
     */
    private final int DECODE_CHANNEL = 1;
    /**
     * Video decode channel 2, support 5 channels, from 1 to 5, shared between USB and RTSP
     */
    private final int DECODE_CHANNEL2 = 2;

    /**
     * Video stream channel 1, support 2 channels, from 0 to 1, only for USB AOA mode
     */
    private final int STREAM_CHANNEL = 1;
    /**
     * Video stream channel 2, support 2 channels, from 0 to 1, only for USB AOA mode
     */
    private final int STREAM_CHANNEL2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mapWidgetHeight = (int) getResources().getDimension(R.dimen.mini_map_height);
        mapWidgetWidth = (int) getResources().getDimension(R.dimen.mini_map_width);
        mapWidgetMarginRight = (int) getResources().getDimension(R.dimen.mini_map_margin_right);
        mapWidgetMarginBottom = (int) getResources().getDimension(R.dimen.mini_map_margin_bottom);

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final Display display = windowManager.getDefaultDisplay();
        Point outPoint = new Point();
        display.getRealSize(outPoint);
        deviceHeight = outPoint.y;
        deviceWidth = outPoint.x;

        rootView = findViewById(R.id.root_view);
        surface = findViewById(R.id.surface);
        surface2 = findViewById(R.id.surface2);
        texture = findViewById(R.id.texture);
        tvBitrateVideo = findViewById(R.id.tv_bitrate_video);
        widgetMap = findViewById(R.id.widget_map);
        btnShot = findViewById(R.id.btn_shot);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);
        btnHwDecoder = findViewById(R.id.btn_hw_decoder);
        swMockVideo = findViewById(R.id.sw_mock_video);
        swMockVideo2 = findViewById(R.id.sw_mock_video2);
        spDecodeMode = findViewById(R.id.sp_decode_mode);
        swHwDecode = findViewById(R.id.sw_hw_decode);
        swAoa = findViewById(R.id.sw_aoa);
        swFpv = findViewById(R.id.sw_fpv);
        btnUpgradeGrd = findViewById(R.id.btn_upgrade_grd);
        btnUpgradeSky = findViewById(R.id.btn_upgrade_sky);
        tvUpdateProcess = findViewById(R.id.tv_update_process);
        btnRtsp = findViewById(R.id.btn_rtsp);
        btnRtspMulti = findViewById(R.id.btn_rtsp_multi);
        btnChuanyun = findViewById(R.id.btn_chuanyun);

        tvVT = findViewById(R.id.tv_VT);
        tvRC = findViewById(R.id.tv_RC);
        tvRcScore = findViewById(R.id.tv_RC_Score);
        tvVtScore = findViewById(R.id.tv_VT_Score);
        imageVT = findViewById(R.id.image_VT_Score);
        imageRC = findViewById(R.id.image_RC_Score);
        tvOSDLocked = findViewById(R.id.tv_osd_locked);
        tvUart = findViewById(R.id.tv_uart);

        // decode mode start
        // 0. FFmpeg with hw decoder, direct render in SurfaceView
        // 1. FFmpeg with hw/sw decoder, yuv2rgb with GL, render in SurfaceView
        // 2. MediaCodec with hw decoder, direct render in SurfaceView
        // 3. MediaCodec with hw decoder, direct render in TextureView
        String[] decodeModeArr = new String[]{"FFmpeg-SurfaceView", "FFmpeg-GL-SurfaceView", "MediaCodec-SurfaceView", "MediaCodec-TextureView"};
        ArrayAdapter<String> decodeModeAdapter = new ArrayAdapter<String>(this, R.layout.item_select, decodeModeArr);
        decodeModeAdapter.setDropDownViewResource(R.layout.item_dropdown);
        spDecodeMode.setAdapter(decodeModeAdapter);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        decodeMode = sp.getInt(Constants.PREF_DECODE_MODE, Constants.DECODE_MODE_FF_SURFACE);
        spDecodeMode.setSelection(decodeMode);
        if (decodeMode != Constants.DECODE_MODE_FF_GL_SURFACE && decodeMode != Constants.DECODE_MODE_FF_SURFACE) {
            btnHwDecoder.setVisibility(View.GONE);
            swHwDecode.setVisibility(View.GONE);
        }
        if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
            surface.setVisibility(View.GONE);
        } else {
            texture.setVisibility(View.GONE);
        }

        spDecodeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != decodeMode) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putInt(Constants.PREF_DECODE_MODE, position);
                    editor.apply();

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(R.string.restart_to_work).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    }).setCancelable(false).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        // decode mode ends

        accessoryHelper = AccessoryHelper.getInstance(getApplicationContext(), true);
        accessoryHelper.addListener(accessoryListener);
        arlinkListen = new ArlinkListen();
        arlinkListen.setControlListener(arlinkDataListener);
        arlinkListen.setStreamListener(STREAM_CHANNEL, arlinkDataListener);
        arlinkListen.setStreamListener(STREAM_CHANNEL2, arlinkDataListener);

        usbDeviceHelper = UsbDeviceHelper.getInstance(getApplicationContext());
        usbDeviceHelper.addListener(usbDeviceListener);

        protocolHelper = new ProtocolHelper();
        protocolHelper.addListener(protocolListener);

        switch (decodeMode) {
            case Constants.DECODE_MODE_FF_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, null, surface, null, null, DECODE_CHANNEL);
                break;
            case Constants.DECODE_MODE_FF_GL_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_GL_SURFACE, null, surface, null, null, DECODE_CHANNEL);
                break;
            case Constants.DECODE_MODE_MEDIACODEC_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_SURFACE, null, surface, null, null, DECODE_CHANNEL, 1920, 1080, 30);
                // Other video profile
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_SURFACE, null, surface, null, null, DECODE_CHANNEL, 240, 320, 25);
                break;
            case Constants.DECODE_MODE_MEDIACODEC_TEXTURE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE, texture, null, null, null, DECODE_CHANNEL, 1920, 1080, 30);
                // Other video profile
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE, texture, null, null, null, DECODE_CHANNEL, 240, 320, 25);
                break;
        }
        // Default value is 1024 * 1024
        mediaHelper.setProbeSize(DECODE_CHANNEL, 1024 * 1024);
        mediaHelper.setListener(mediaListener);

        // Second video stream, fixed to FF_DIRECT_SURFACE mode
        mediaHelper2 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, null, surface2, null, null, DECODE_CHANNEL2);
        mediaHelper2.setProbeSize(DECODE_CHANNEL2, 1024 * 1024);

        ffListenerManager = FFListenerManager.addListener(this, ffListener);

        bitRateHelperVideo = new BitRateHelper();
        bitRateHelperVideo.setListener(bitRateListenerVideo);

        String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
        h264Saver = new H264Saver(path);

        permissionHelper = new PermissionHelper(this);

        readAoa();

        // Get whether hardware decoding now (default value is true)
        swHwDecode.setChecked(FFJNI.isHwDecode());
        swHwDecode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(Constants.PREF_IS_HW_DECODE, isChecked);
                editor.apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(R.string.restart_to_work).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                }).setCancelable(false).show();
            }
        });

        swMockVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    videoMock = new VideoMock(mediaHelper);
                    videoMock.start();
                } else {
                    if (videoMock != null) {
                        videoMock.destroy();
                        videoMock = null;
                    }
                }
            }
        });

        swMockVideo2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    videoMock2 = new VideoMock(mediaHelper2);
                    videoMock2.start();
                } else {
                    if (videoMock2 != null) {
                        videoMock2.destroy();
                        videoMock2 = null;
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

        accessoryHelper.onResume();
        usbDeviceHelper.onResume();
        protocolHelper.onResume();
        permissionHelper.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        accessoryHelper.onPause();
        protocolHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        accessoryHelper.removeListener(accessoryListener);
        accessoryHelper.onDestroy();
        usbDeviceHelper.removeListener(usbDeviceListener);
        usbDeviceHelper.onDestroy();
        protocolHelper.removeListener(protocolListener);
        protocolHelper.onDestroy();
        ffListenerManager.removeListener();
        FFJNI.stop(DECODE_CHANNEL);
        FFJNI.stop(DECODE_CHANNEL2);
        h264Saver.stop();
        if (videoMock != null) {
            videoMock.destroy();
            videoMock = null;
        }
        if (videoMock2 != null) {
            videoMock2.destroy();
            videoMock2 = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        super.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_F1) {
            readAoa();
        }
        return super.onKeyDown(keyCode, event);
    }



    public void onClick(View view) {
        if ((view == surface || view == texture) && !isMapMini) {
            // 地图缩小，视频变大
            //reorder widgets
            if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
                texture.setTranslationZ(1);
            } else {
                surface.setTranslationZ(1);
            }

            //resize widgets
            resizeMap(false);
            resizeVideo(true);
            //disable user login widget on map
//            widgetMap.getUserAccountLoginWidget().setVisibility(View.GONE);
            isMapMini = true;
        } else if (view == widgetMap && isMapMini) {
            // 地图变大，视频缩小
            //reorder widgets
            if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
                texture.setTranslationZ(4);
            } else {
                surface.setTranslationZ(4);
            }

            //resize widgets
            resizeMap(true);
            resizeVideo(false);
            //enable user login widget on map
//            widgetMap.getUserAccountLoginWidget().setVisibility(View.VISIBLE);
            isMapMini = false;
        }
        else if (view == btnShot) {
            switch (mediaHelper.getDecodeMode()) {
                case FF_GL_SURFACE: {
                    String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/shot";
                    File fileDir = new File(path);
                    fileDir.mkdirs();
                    File file = new File(fileDir, new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".jpg");
                    try {
                        file.createNewFile();
                        // Retrieve the result via FFListener.onShotFrame
                        FFJNI.shotFrame(file.getAbsolutePath(), DECODE_CHANNEL);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
                case FF_DIRECT_SURFACE:
                case MEDIACODEC_SURFACE: {
                    // 直接渲染到Surface上的情况，无法从buffer中提取图像，只能从Surface上提取
                    Bitmap bitmap = Bitmap.createBitmap(mediaHelper.VIDEO_WIDTH, mediaHelper.VIDEO_HEIGHT, Bitmap.Config.ARGB_8888);
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
                case MEDIACODEC_TEXTURE: {
                    Bitmap bitmap = texture.getBitmap();
                    if (bitmap != null) {
                        Toast.makeText(MainApplication.applicationContext, R.string.take_photo_success, Toast.LENGTH_SHORT)
                                .show();
                        saveBitmap(bitmap);
                    } else {
                        Toast.makeText(MainApplication.applicationContext, R.string.take_photo_fail, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
            }
        } else if (view == btnStartRecord) {
            String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
            switch (mediaHelper.getDecodeMode()) {
                case FF_GL_SURFACE:
                case FF_DIRECT_SURFACE:
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
                case MEDIACODEC_SURFACE:
                case MEDIACODEC_TEXTURE: {
                    if (muxerUtil != null) {
                        Toast.makeText(MainApplication.applicationContext, R.string.record_ing, Toast.LENGTH_SHORT)
                                .show();
                        return;
                    }
                    if (isMediaCodecPlaying && mMediaFormat != null) {
                        muxerUtil = new MuxerUtil(path);
                        muxerUtil.addVideoTrack(mMediaFormat);
                        muxerUtil.start();
                    } else {
                        Toast.makeText(MainApplication.applicationContext, R.string.record_not_playing, Toast.LENGTH_SHORT)
                                .show();
                    }
                }
                break;
            }
        } else if (view == btnStopRecord) {
            switch (mediaHelper.getDecodeMode()) {
                case FF_GL_SURFACE:
                case FF_DIRECT_SURFACE:
                    // Retrieve the record result via FFListener.onRecordVideo
                    FFJNI.stopRecord(DECODE_CHANNEL);
                    break;
                case MEDIACODEC_SURFACE:
                case MEDIACODEC_TEXTURE: {
                    if (muxerUtil != null) {
                        muxerUtil.stop();
                        final String path = muxerUtil.getFilePath();
                        muxerUtil = null;
                        Toast.makeText(MainActivity.this, R.string.record_success, Toast.LENGTH_SHORT).show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ImageUtils.save2Album(path, "coolfly", System.currentTimeMillis() + ".mp4", true);
                            }
                        }).start();
                    }
                }
                break;
            }
        } else if (view == btnHwDecoder) {
            String info = FFJNI.avcodecinfo();
            Toast.makeText(MainActivity.this, info, Toast.LENGTH_LONG).show();
            Log.d("codec info", info);
        } else if (view == btnUpgradeGrd) {
            if (AccessoryHelper.UsbStatus != AccessoryHelper.USB_CONNECTED) {
                Toast.makeText(MainActivity.this, "AOA not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_GRD);
        } else if (view == btnUpgradeSky) {
            if (AccessoryHelper.UsbStatus != AccessoryHelper.USB_CONNECTED) {
                Toast.makeText(MainActivity.this, "AOA not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_SKY);
        } else if (view == btnRtsp) {
            Intent intent = new Intent(this, RtspSingleActivity.class);
            startActivity(intent);
        } else if (view == btnRtspMulti) {
            Intent intent = new Intent(this, RtspMultiActivity.class);
            startActivity(intent);
        } else if (view == btnChuanyun) {
            Intent intent = new Intent(this, ChuanYunActivity.class);
            startActivity(intent);
        }
    }

    private void readAoa() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                AoaSwitch.AoaMode aoaMode = AoaSwitch.getMode();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        swAoa.setOnCheckedChangeListener(null);
                        swFpv.setOnCheckedChangeListener(null);

                        switch (aoaMode) {

                            case USB_FPVOFF:
                                swAoa.setChecked(false);
                                swFpv.setChecked(false);
                                break;
                            case USB_FPVON:
                                swAoa.setChecked(false);
                                swFpv.setChecked(true);
                                break;
                            case AOA_FPVOFF:
                                swAoa.setChecked(true);
                                swFpv.setChecked(false);
                                break;
                            case AOA_FPVON:
                                swAoa.setChecked(true);
                                swFpv.setChecked(true);
                                break;
                            case UNKNOWN:
                                break;
                        }


                        swAoa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AoaSwitch.switchUsb(b);
                                    }
                                }).start();
                            }
                        });

                        swFpv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        AoaSwitch.switchFpv(b);
                                    }
                                }).start();
                            }
                        });
                    }
                });
            }
        }).start();
    }

    private void getUpgradeFis(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); //设置bin后缀名
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream fis = getContentResolver().openInputStream(uri);
                if (fis != null) {
                    upgradeHelper = new UpgradeHelper(fis);
                    upgradeHelper.setListener(upgradeListener);
                    upgradeHelper.startUpgradeApp(requestCode == REQ_OTA_SKY);
                    btnUpgradeGrd.setEnabled(false);
                    btnUpgradeSky.setEnabled(false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    private AccessoryListener accessoryListener = new AccessoryListener() {
        @Override
        public void onReadyToOpenConnect() {
            // todo
            //  if accessoryHelper = AccessoryHelper.getInstance(getApplicationContext(), false);
            //  (the second parameter autoOpenAccessory == false)
            //  then you need to call accessoryHelper.openAccessory(); manually here
        }

        @Override
        public void onDisconnect() {
            bitRateHelperVideo.stop();
            protocolHelper.resetSkyUart3PassThrough();
            protocolHelper.resetSkyUart1PassThrough();
            protocolHelper.resetGndUart3PassThrough();
        }

        @Override
        public void onData(byte[] data, int length) {
            arlinkListen.ArlinkRxPacketDataAnalyze(data, length);
        }
    };

    private ArlinkDataListener arlinkDataListener = new ArlinkDataListener() {

        @Override
        public void onCtrlData(byte[] data, int length) {
            protocolHelper.parseData(data, length);
        }

        @Override
        public void onStreamData(int channel, byte[] data, int length) {
            if (channel == STREAM_CHANNEL) {
                bitRateHelperVideo.receive(length);
                mediaHelper.offerData(data, length);
                if (NEED_SAVE_H264) {
                    byte[] buffer = new byte[length];
                    System.arraycopy(data, 0, buffer, 0, length);
                    h264Saver.writeVideoSampleData(buffer);
                }
            } else if (channel == STREAM_CHANNEL2) {
                mediaHelper2.offerData(data, length);
            }
        }
    };

    private UsbDeviceListener usbDeviceListener = new UsbDeviceListener() {
        @Override
        public void onDisconnect() {
            bitRateHelperVideo.stop();
            protocolHelper.resetSkyUart3PassThrough();
            protocolHelper.resetSkyUart1PassThrough();
            protocolHelper.resetGndUart3PassThrough();
        }

        @Override
        public void onVideoData(byte[] data, int length) {
            bitRateHelperVideo.receive(length);
            mediaHelper.offerData(data, length);
            if (NEED_SAVE_H264) {
                byte[] buffer = new byte[length];
                System.arraycopy(data, 0, buffer, 0, length);
                h264Saver.writeVideoSampleData(buffer);
            }
        }

        @Override
        public void onAudioData(byte[] data, int length) {
            mediaHelper2.offerData(data, length);
        }

        @Override
        public void onCtrlData(byte[] data, int length) {
            protocolHelper.parseData(data, length);
        }
    };

    private DeviceInfo arlinkDevice = new DeviceInfo();
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseCoolflyPacket packet) {
            Log.d(TAG, "onReadCmd: " + packet.getClass().getSimpleName());
            if (packet instanceof DeviceInfo) {
                DeviceInfo deviceInfo = (DeviceInfo) packet;
                arlinkDevice = deviceInfo;
                if (deviceInfo.skyGround == 1) {
                    protocolHelper.startSkyUart3PassThrough();
                    Log.d(TAG, "startSkyUart3PassThrough");
                    protocolHelper.startSkyUart1PassThrough();
                    Log.d(TAG, "startSkyUart1PassThrough");
                    protocolHelper.startGndUart3PassThrough();
                    Log.d(TAG, "startGndUart3PassThrough");

                    // todo
                    //  (optional) query osd info
                    protocolHelper.startQueryWirelessInfo();
                } else {
                    Log.d(TAG, "deviceInfo.skyGround != 1");

                    protocolHelper.startSkyUart3PassThrough();
                    Log.d(TAG, "startSkyUart3PassThrough");
                    protocolHelper.startSkyUart1PassThrough();
                    Log.d(TAG, "startSkyUart1PassThrough");
                    protocolHelper.startGndUart3PassThrough();
                    Log.d(TAG, "startGndUart3PassThrough");
                }

//                // Mock SkyUart3Rx
//                byte[] aa = {(byte) 0xFF, 0x5A, (byte) 0x87, 0x00, 0x01, 0x00, 0x05, 0x00, (byte) 0xA4, 0x01, 0x12, 0x34, 0x56, 0x78, (byte) 0x90};
//                protocolHelper.parseData(aa, aa.length);
            } else if (packet instanceof UartRx) {
                byte[] data = ((UartRx) packet).data;
                if (data != null && data.length > 0) {
                    // todo
                    //  handle data (such as mavlink packages bytes) read through uart bypass (SkyUart1Rx/SkyUart3Rx/GndUart3Rx)

                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i<data.length; i++) {
                        stringBuilder.append(String.format("%02X ", data[i]));
                    }
                    Log.d(TAG, "onRead " + packet.getClass().getSimpleName() + ": " + stringBuilder.toString());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvUart.setText(packet.getClass().getSimpleName() + ": " + stringBuilder.toString());
                        }
                    });
                }
            } else if (packet instanceof WirelessInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderWirelessInfo((WirelessInfo) packet);
                    }
                });
            } else if (packet instanceof ACK) {
                if (upgradeHelper != null) {
                    upgradeHelper.onAck((ACK) packet);
                }
            }
        }

        @Override
        public void onWrite(byte[] data) {
            if (AccessoryHelper.UsbStatus == AccessoryHelper.USB_CONNECTED) {
                accessoryHelper.writeData(data);
            } else if (usbDeviceHelper.getUsbStatus() == UsbDeviceHelper.USB_CONNECTED) {
                usbDeviceHelper.writeData(data);
            }
        }
    };

    private enum UART{
        SkyUart1, SkyUart3, GndUart3;
    }

    /**
     * todo
     *  call this method to send data (such as mavlink packages bytes) through uart bypass (SkyUart1/SkyUart3/GndUart3)
     * @param data
     * @param length
     */
    private void writeDataToUart(UART uart, byte[] data, int length) {
        if (accessoryHelper.getAccesoryStateMonitored() == AccessoryHelper.AccessoryConnected) {
            switch (uart) {

                case SkyUart1:
                    protocolHelper.sendSkyUart1Tx(data, length);
                    break;
                case SkyUart3:
                    protocolHelper.sendSkyUart3Tx(data, length);
                    break;
                case GndUart3:
                    protocolHelper.sendGndUart3Tx(data, length);
                    break;
            }

            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (int i = 0; i<data.length; i++) {
                stringBuilder.append(String.format("%02X ", data[i]));
            }
            Log.d(TAG, "onWrite " + uart + ": " + stringBuilder.toString());
        }
    }

    private void renderWirelessInfo(WirelessInfo wirelessOSD) {
        String modulation = "";
        if (wirelessOSD.lockStatus == 0x00) {
            tvOSDLocked.setTextColor(Color.RED);
            tvOSDLocked.setText("DisConnect");
        } else {
            tvOSDLocked.setTextColor(Color.parseColor("#7CFC00"));
            tvOSDLocked.setText("Connected");
        }


        DecimalFormat format = new DecimalFormat("##0.000");
        String strSkySNR = format.format(wirelessOSD.skySNR);
        tvRC.setText("SNR:     "+ strSkySNR + " dB\n");
        if (wirelessOSD.lockStatus == 0x00) {
            tvRC.setText("SNR:      --- dB\n");
            tvRC.append("Power0: ---\n");
            tvRC.append("Power1: ---\n");

            tvRC.append("Energy0: --- dBm \n");
            tvRC.append("Energy1: --- dBm \n");

            tvRC.append("E_rate:    --- ");
        }
        else {
            tvRC.append("Power0: " + wirelessOSD.skyAgcVal[0] + "\n");
            tvRC.append("Power1: " + wirelessOSD.skyAgcVal[1] + "\n");


            int dBand = arlinkDevice.band;
            if (dBand == 1) {
                tvRC.append("Energy0: " + (1 - wirelessOSD.skyAgcVal[0]) + " dBm\n");
                tvRC.append("Energy1: " + (1 - wirelessOSD.skyAgcVal[1]) + " dBm\n");
            } else if (dBand == 2) {
                tvRC.append("Energy0: " + (9 - wirelessOSD.skyAgcVal[0]) + " dBm\n");
                tvRC.append("Energy1: " + (9 - wirelessOSD.skyAgcVal[1]) + " dBm\n");
            } else {
                tvRC.append("Energy0: --- dBm \n");
                tvRC.append("Energy1: --- dBm \n");
            }

            tvRC.append("E_rate:    " + (100 - wirelessOSD.rcLock));
        }


        switch (wirelessOSD.modulationMode) {
            case 0x00:
                modulation += "BPSK ";
                break;
            case 0x01:
                modulation += "QPSK ";
                break;
            case 0x02:
                modulation += "16QAM ";
                break;
            case 0x03:
                modulation += "64QAM ";
                break;
            default:
                break;
        }

        if (wirelessOSD.codeRate == 0x00)
            modulation += "1/2";
        else if (wirelessOSD.codeRate == 0x01)
            modulation += "2/3";


        if (wirelessOSD.lockStatus == 0x00) {

            //tvVT.setText("SNR0:     --- dB\n");
            tvVT.setText("SNR:     --- dB\n");

            tvVT.append("Power0:   --- \n");
            tvVT.append("Power1:   --- \n");

            tvVT.append("Energy0: --- dBm \n");
            tvVT.append("Energy1: --- dBm \n");
            tvVT.append("MCS:   "  + "--- \n");
            tvVT.append("E_rate:    " + "--- ");

        } else {
            //tvVT.setText("SNR0:     " + format.format(wirelessOSD.snrValue[0]) + " dB\n");
            tvVT.setText("SNR:     " + format.format(wirelessOSD.snrValue[1]) + " dB\n");
            tvVT.append("Power0:   " + wirelessOSD.agcValue[0] + "\n");
            tvVT.append("Power1:   " + wirelessOSD.agcValue[1] + "\n");


            int dBand = arlinkDevice.band;
            if (dBand == 1) {
                tvVT.append("Energy0: " + (1 - wirelessOSD.agcValue[0]) + " dBm\n");
                tvVT.append("Energy1: " + (1 - wirelessOSD.agcValue[1]) + " dBm\n");
            } else if (dBand == 2){

                tvVT.append("Energy0: " + (9 - wirelessOSD.agcValue[0]) + " dBm\n");
                tvVT.append("Energy1: " + (9 - wirelessOSD.agcValue[1]) + " dBm\n");
            } else {
                tvVT.append("Energy0: --- dBm \n");
                tvVT.append("Energy1: --- dBm \n");
            }

            tvVT.append("MCS:   "  + modulation + "\n");
            tvVT.append("E_rate:    " + wirelessOSD.errCnt);

        }

        int rcScore = 0;
        int vtScore = 0;

        float VtSnr = wirelessOSD.snrValue[1];

        vtScore = (int)(3.52 * VtSnr + 19.057);

        if (vtScore > 100)
            vtScore = 100;

        if (wirelessOSD.errCnt >= 55) {
            vtScore -= 55;
        } else if (wirelessOSD.errCnt >= 50 && wirelessOSD.errCnt < 55) {
            vtScore -= 50;
        } else if (wirelessOSD.errCnt >= 45 && wirelessOSD.errCnt < 50) {
            vtScore -= 40;
        } else if (wirelessOSD.errCnt >= 40 && wirelessOSD.errCnt < 45) {
            vtScore -= 40;
        } else if (wirelessOSD.errCnt >= 35 && wirelessOSD.errCnt < 40) {
            vtScore -= 30;
        } else if (wirelessOSD.errCnt >= 30 && wirelessOSD.errCnt < 35) {
            vtScore -= 30;
        } else if (wirelessOSD.errCnt >= 25 && wirelessOSD.errCnt < 30) {
            vtScore -= 20;
        } else if (wirelessOSD.errCnt >= 20 && wirelessOSD.errCnt < 25) {
            vtScore -= 20;
        } else if (wirelessOSD.errCnt >= 15 && wirelessOSD.errCnt < 20) {
            vtScore -= 10;
        } else if (wirelessOSD.errCnt >= 10 && wirelessOSD.errCnt < 15) {
            vtScore -= 10;
        } else if (wirelessOSD.errCnt >= 5 && wirelessOSD.errCnt < 10) {
            vtScore -= 5;
        } else if (wirelessOSD.errCnt > 0 && wirelessOSD.errCnt < 5) {
            vtScore -= 2;
        }

        if (vtScore <= 0)
            vtScore = 2;

        if (vtScore >= 75) {
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_5);
        } else if (vtScore >= 55) {
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_4);
        } else if (vtScore >= 35) {
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_3);
        }  else if (vtScore >= 15) {
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_2);
        } else if (vtScore > 0 && vtScore < 10) {
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_1);
        }

        if (wirelessOSD.lockStatus == 0x00)
            imageVT.setImageResource(R.mipmap.fpv_topbar_signal_level_0);


        float RcSnr = wirelessOSD.skySNR;
        rcScore = (int)(wirelessOSD.rcLock * (7.7 * RcSnr + 27) / 100);

        if (wirelessOSD.lockStatus == 0x00) {
            rcScore = 0;
            vtScore = 0;
        }

        if (rcScore < 0)
            rcScore = 2;

        if (rcScore > 100)
            rcScore = 100;

        if (rcScore >= 75) {
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_5);
        } else if (rcScore >= 55) {
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_4);
        } else if (rcScore >= 30) {
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_3);
        }  else if (rcScore >= 15) {
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_2);
        } else if (rcScore >= 0 && rcScore < 15) {
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_1);
        }

        if (wirelessOSD.lockStatus == 0x00)
            imageRC.setImageResource(R.mipmap.fpv_topbar_signal_level_0);

        tvRcScore.setText("" + rcScore);
        tvVtScore.setText("" + vtScore);
    }

    /**
     * Change the size of the video of channel DECODE_CHANNEL
     * @param videoWidth
     * @param videoHeight
     */
    private void setVideoLayout(int videoWidth, int videoHeight) {
        float aspectRatio = ((float) rootView.getWidth()) / rootView.getHeight();
        float aspectRatioNew = ((float) videoWidth) / videoHeight;
        View viewToChange = mediaHelper.getDecodeMode() == MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE? texture: surface;
        if (aspectRatio > aspectRatioNew) {
            float realWidth = ((float) (rootView.getHeight())) * aspectRatioNew;
            if (isMapMini) {
                ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
                layoutParams.width = (int) realWidth;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                viewToChange.requestLayout();
            }

            videoWidgetWidth = (int) realWidth;
            videoWidgetHeight = rootView.getHeight();
        } else {
            float realHeight = ((float) (rootView.getWidth())) / aspectRatioNew;
            if (isMapMini) {
                ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
                layoutParams.height = (int) realHeight;
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                viewToChange.requestLayout();
            }

            videoWidgetWidth = rootView.getWidth();
            videoWidgetHeight = (int) realHeight;
        }
    }

    /**
     * Change the size of the video of channel DECODE_CHANNEL2
     *  @param videoWidth
     * @param videoHeight
     */
    private void setVideoLayout2(int videoWidth, int videoHeight) {
        float aspectRatio = ((float) surface2.getWidth()) / surface2.getHeight();
        float aspectRatioNew = ((float) videoWidth) / videoHeight;
        View viewToChange = surface2;
        if (aspectRatio > aspectRatioNew) {
            float realWidth = ((float) (surface2.getHeight())) * aspectRatioNew;
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.width = (int) realWidth;
            layoutParams.height = surface2.getHeight();
            viewToChange.requestLayout();
        } else {
            float realHeight = ((float) (surface2.getWidth())) / aspectRatioNew;
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.height = (int) realHeight;
            layoutParams.width = surface2.getWidth();
            viewToChange.requestLayout();
        }
    }

    // For MEDIACODEC_SURFACE and MEDIACODEC_TEXTURE, to record H264 stream
    private MediaListener mediaListener = new MediaListener() {
        @Override
        public void onConfigure(MediaFormat mediaFormat) {
            mMediaFormat = mediaFormat;

            // For MEDIACODEC_SURFACE and MEDIACODEC_TEXTURE, to record H264 stream, you need to set SPS and PPS parameters
            // So if you need to record, we recommend using FF_DIRECT_SURFACE or FF_GL_SURFACE
            // TODO Use your own video parameters
            byte[] sps = {
                    (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x67,
                    (byte)0x64, (byte)0x00, (byte)0x1F, (byte)0xAC, (byte)0xB4,
                    (byte)0x02, (byte)0x80, (byte)0x2D, (byte)0xD8, (byte)0x08,
                    (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x00,
                    (byte)0x80, (byte)0x00, (byte)0x00, (byte)0x1E, (byte)0x07,
                    (byte)0x8C, (byte)0x19, (byte)0x50};
            byte[] pps = {(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01,
                    (byte)0x68, (byte)0xEF, (byte)0x32, (byte)0xC8, (byte)0xB0};
            mMediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
            mMediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(pps));

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setVideoLayout(mMediaFormat.getInteger(MediaFormat.KEY_WIDTH), mMediaFormat.getInteger(MediaFormat.KEY_HEIGHT));
                }
            });
        }

        @Override
        public void onStart() {
            isMediaCodecPlaying = true;
        }

        @Override
        public void onFrameData(H264Extractor.SyncFrame syncFrame) {
            if (muxerUtil != null && muxerUtil.isStart()) {
                if (!isMediaCodecRecordFoundIFrame) {
                    if (!syncFrame.isIframe) {
                        // First frame must be I frame
                        return;
                    } else {
                        isMediaCodecRecordFoundIFrame = true;
                    }
                }
                muxerUtil.writeVideoSampleData(syncFrame.byteBuffer, syncFrame.byteBuffer.capacity(), 30);
            }
        }

        @Override
        public void onBitRate(float bitRate) {

        }

        @Override
        public void onRelease() {
            isMediaCodecPlaying = false;
            isMediaCodecRecordFoundIFrame = false;
        }
    };

    private FFListener ffListener = new FFListener() {
        @Override
        public void onMediaFormat(String format, int width, int height, long bitRate, int handler) {
            if (handler == DECODE_CHANNEL) {
                setVideoLayout(width, height);
                mediaHelper.updateVideoSize(width, height);
            } else if (handler == DECODE_CHANNEL2) {
                setVideoLayout2(width, height);
                mediaHelper2.updateVideoSize(width, height);
            }
        }
    };

    private BitRateHelper.OnBitRateListener bitRateListenerVideo = new BitRateHelper.OnBitRateListener() {
        @Override
        public void onBitRate(long bitRate, String readable) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvBitrateVideo.setText(readable);
                }
            });
        }
    };

    private UpgradeHelper.UpgradeListener upgradeListener = new UpgradeHelper.UpgradeListener() {
        @Override
        public void onStart() {
            tvUpdateProcess.setText(R.string.ota_start);
        }

        @Override
        public void onProcess(int curFrame, int totalFrame) {
            tvUpdateProcess.setText(curFrame + " / " + totalFrame);
        }

        @Override
        public void onResend(int curFrame, int totalFrame) {
            tvUpdateProcess.setText(getString(R.string.ota_resend, curFrame, totalFrame));
        }

        @Override
        public void onFlashing() {
            tvUpdateProcess.setText(R.string.ota_ing);
        }

        @Override
        public void onComplete() {
            tvUpdateProcess.setText(R.string.ota_finish);
            btnUpgradeGrd.setEnabled(true);
            btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onFail(String errMsg) {
            tvUpdateProcess.setText(R.string.ota_fail + "\n" + errMsg);
            btnUpgradeGrd.setEnabled(true);
            btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onWrite(byte[] data) {
            if (AccessoryHelper.UsbStatus == AccessoryHelper.USB_CONNECTED) {
                accessoryHelper.writeData(data);
            } else if (usbDeviceHelper.getUsbStatus() == UsbDeviceHelper.USB_CONNECTED) {
                usbDeviceHelper.writeData(data);
            }
        }
    };

    private void resizeMap(boolean isEnlarge) {
        if (isEnlarge) {
            // enlarge
            ResizeAnimation enlargeAnimation = new ResizeAnimation(true, widgetMap, mapWidgetWidth, mapWidgetHeight, deviceWidth, deviceHeight, 0, 0);
            widgetMap.startAnimation(enlargeAnimation);
        } else {
            // shrink
            ResizeAnimation shrinkAnimation = new ResizeAnimation(false, widgetMap, deviceWidth, deviceHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
            widgetMap.startAnimation(shrinkAnimation);
        }
    }

    private void resizeVideo(boolean isEnlarge) {
        if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
            if (isEnlarge) {
                // enlarge
                ResizeAnimation enlargeAnimation = new ResizeAnimation(true, texture, mapWidgetWidth, mapWidgetHeight, videoWidgetWidth, videoWidgetHeight, 0, 0);
                texture.startAnimation(enlargeAnimation);
            } else {
                // shrink
                ResizeAnimation shrinkAnimation = new ResizeAnimation(false, texture, videoWidgetWidth, videoWidgetHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
                texture.startAnimation(shrinkAnimation);
            }
        } else {
            if (isEnlarge) {
                // enlarge
                ResizeAnimation enlargeAnimation = new ResizeAnimation(true, surface, mapWidgetWidth, mapWidgetHeight, videoWidgetWidth, videoWidgetHeight, 0, 0);
                surface.startAnimation(enlargeAnimation);
            } else {
                // shrink
                ResizeAnimation shrinkAnimation = new ResizeAnimation(false, surface, videoWidgetWidth, videoWidgetHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
                surface.startAnimation(shrinkAnimation);
            }
        }
    }

    /**
     * Animation to change the size of a view.
     */
    private static class ResizeAnimation extends Animation {

        private static final int DURATION = 300;

        private boolean isEnlarge;
        private View view;
        private int toHeight;
        private int fromHeight;
        private int toWidth;
        private int fromWidth;
        private int marginRight;
        private int marginBottom;

        private ResizeAnimation(boolean isEnlarge, View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int marginRight, int marginBottom) {
            this.isEnlarge = isEnlarge;
            this.toHeight = toHeight;
            this.toWidth = toWidth;
            this.fromHeight = fromHeight;
            this.fromWidth = fromWidth;
            view = v;
            this.marginRight = marginRight;
            this.marginBottom = marginBottom;
            setDuration(DURATION);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (toHeight - fromHeight) * interpolatedTime + fromHeight;
            float width = (toWidth - fromWidth) * interpolatedTime + fromWidth;
            ConstraintLayout.LayoutParams p = (ConstraintLayout.LayoutParams) view.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = marginRight;
            p.bottomMargin = marginBottom;

            if (this.isEnlarge) {
                if (interpolatedTime == 1) {
                    ConstraintSet set = new ConstraintSet();
                    set.clone((ConstraintLayout) view.getParent());
                    set.connect(view.getId(), ConstraintSet.TOP, R.id.root_view, ConstraintSet.TOP, 0);
                    set.connect(view.getId(), ConstraintSet.START, R.id.root_view, ConstraintSet.START, 0);
                    set.applyTo((ConstraintLayout) view.getParent());
                }
            } else {
                ConstraintSet set = new ConstraintSet();
                set.clone((ConstraintLayout) view.getParent());
                set.clear(view.getId(), ConstraintSet.TOP);
                set.clear(view.getId(), ConstraintSet.START);
                set.applyTo((ConstraintLayout) view.getParent());
            }

            view.requestLayout();
        }
    }
}