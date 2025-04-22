package com.coolfly.demo;


import static com.coolfly.demo.utils.Constants.SP_NAME;
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
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.coolfly.demo.chuanyun.ChuanYunActivity;
import com.coolfly.demo.databinding.ActivityMainBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.coolfly.demo.utils.Constants;
import com.coolfly.demo.utils.ImageUtils;
import com.coolfly.demo.utils.PermissionHelper;
import com.coolfly.demo.v3ota.V3OtaActivity;
import com.fly.aoalibrary.DEVICE_TYPE;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.aoalibrary.host.UsbDeviceListener;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.FormatProfile;
import com.fly.fflibrary.MediaConfig;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.medialibrary.BitRateHelper;
import com.fly.medialibrary.H264Extractor;
import com.fly.medialibrary.H264Saver;
import com.fly.medialibrary.MediaHelper;
import com.fly.medialibrary.MediaListener;
import com.fly.medialibrary.MuxerUtil;
import com.fly.station.gpio.AoaSwitch;
import com.fly.station.gpio.HostSwitch;
import com.fly.station.prorocol.Fly;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.UpgradeHelper;
import com.fly.station.prorocol.bean.ACK;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.ChanInfo8030;
import com.fly.station.prorocol.bean.DeviceInfo;
import com.fly.station.prorocol.bean.RcStatus8030;
import com.fly.station.prorocol.bean.SysInfo8030;
import com.fly.station.prorocol.bean.UartRx;
import com.fly.station.prorocol.bean.UsbRx;
import com.fly.station.prorocol.bean.WirelessInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
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
    private final int REQ_OTA_V4 = 3;

    /**
     *  Video decode channel 1, support 5 channels, from 1 to 5, shared between USB and RTSP
     */
    private final int DECODE_CHANNEL = 1;
    /**
     * Video decode channel 2, support 5 channels, from 1 to 5, shared between USB and RTSP
     */
    private final int DECODE_CHANNEL2 = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
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

        // decode mode start
        // 0. FFmpeg with hw decoder, direct render in SurfaceView
        // 1. FFmpeg with hw/sw decoder, yuv2rgb with GL, render in SurfaceView
        // 2. MediaCodec with hw decoder, direct render in SurfaceView
        // 3. MediaCodec with hw decoder, direct render in TextureView
        String[] decodeModeArr = new String[]{"FFmpeg-SurfaceView", "FFmpeg-GL-SurfaceView", "MediaCodec-SurfaceView", "MediaCodec-TextureView"};
        ArrayAdapter<String> decodeModeAdapter = new ArrayAdapter<String>(this, R.layout.item_select, decodeModeArr);
        decodeModeAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spDecodeMode.setAdapter(decodeModeAdapter);

        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        decodeMode = sp.getInt(Constants.PREF_DECODE_MODE, Constants.DECODE_MODE_FF_SURFACE);
        binding.spDecodeMode.setSelection(decodeMode);
        if (decodeMode != Constants.DECODE_MODE_FF_GL_SURFACE && decodeMode != Constants.DECODE_MODE_FF_SURFACE) {
            binding.btnFfInfo.setVisibility(View.GONE);
            binding.swHwDecode.setVisibility(View.GONE);
        }
        if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
            binding.surface.setVisibility(View.GONE);
        } else {
            binding.texture.setVisibility(View.GONE);
        }

        binding.spDecodeMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != decodeMode) {
                    SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
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

        usbDeviceHelper = UsbDeviceHelper.getInstance(getApplicationContext());
        usbDeviceHelper.addListener(usbDeviceListener);

        protocolHelper = ProtocolHelper.getInstance();
        protocolHelper.addListener(protocolListener);

        MediaConfig mediaConfig = PreferenceActivity.preferenceObject.mediaConfig;
        switch (decodeMode) {
            case Constants.DECODE_MODE_FF_SURFACE:
                // 1. If you know video format(encoding/width/height), and if it does not change, use FormatProfile to accelerate first frame rendering
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, binding.surface, DECODE_CHANNEL, new FormatProfile(FormatProfile.FORMAT.FORMAT_H264, 1920, 1080, 30));
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, surface, DECODE_CHANNEL, new FormatProfile(FormatProfile.FORMAT.FORMAT_H264, 1278, 720, 30));
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, surface, DECODE_CHANNEL, new FormatProfile(FormatProfile.FORMAT.FORMAT_H264, 382, 288, 30));
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, surface, DECODE_CHANNEL, new FormatProfile(FormatProfile.FORMAT.FORMAT_H265, 1920, 1080, 30));

                // 2. If not, it's OK, let SDK detect video format for you
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, surface, DECODE_CHANNEL);
                break;
            case Constants.DECODE_MODE_FF_GL_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_GL_SURFACE, binding.surface, DECODE_CHANNEL, new FormatProfile(FormatProfile.FORMAT.FORMAT_H264, 1920, 1080, 30));
                break;
            case Constants.DECODE_MODE_MEDIACODEC_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_SURFACE, binding.surface, DECODE_CHANNEL, 1920, 1080, 30);
                // Other video profile
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_SURFACE, surface, DECODE_CHANNEL, 240, 320, 25);
                break;
            case Constants.DECODE_MODE_MEDIACODEC_TEXTURE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE, binding.texture, DECODE_CHANNEL, 1920, 1080, 30);
                // Other video profile
//                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE, texture, DECODE_CHANNEL, 240, 320, 25);
                break;
        }
        // Default value is 1024 * 1024, when FormatProfile is not used, decrease this value to accelerate first frame rendering.
        // But don’t set the value too small, otherwise the video format will not be parsed.
        mediaHelper.setMediaConfig(mediaConfig);

        // Only necessary for MEDIACODEC_SURFACE and MEDIACODEC_TEXTURE modes, to record H264 stream
        mediaHelper.setListener(mediaListener);

        // Second video stream, best practice, use FF_DIRECT_SURFACE mode, use FormatProfile to accelerate first frame rendering
        mediaHelper2 = new MediaHelper(MediaHelper.DECODE_MODE.FF_DIRECT_SURFACE, binding.surface2, DECODE_CHANNEL2, new FormatProfile(FormatProfile.FORMAT.FORMAT_H264, 1920, 1080, 30));

        // Default value is 1024 * 1024, when FormatProfile is not used, decrease this value to accelerate first frame rendering.
        // But don’t set the value too small, otherwise the video format will not be parsed.
        mediaHelper2.setMediaConfig(mediaConfig);

        ffListenerManager = FFListenerManager.addListener(this, ffListener);

        bitRateHelperVideo = new BitRateHelper();
        bitRateHelperVideo.setListener(bitRateListenerVideo);

        String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
        h264Saver = new H264Saver(path);

        permissionHelper = new PermissionHelper(this);

        if (Fly.isRk()) {
            binding.swLink.setVisibility(View.GONE);
        }

        readAoa();

        // Get whether hardware decoding now (default value is true)
        binding.swHwDecode.setChecked(FFJNI.isHwDecode());
        binding.swHwDecode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
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

        binding.swMockVideo.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

        binding.swMockVideo2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
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

        usbDeviceHelper.onResume();
        permissionHelper.onResume();

        try {
            binding.tvSn.setText(String.format("RCSN: %s", Fly.getRCSerialNumber()));
            binding.tvSysVersion.setText(String.format("RCSysVer: %s", Fly.getRCSysVersion()));
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException | IOException e) {
            throw new RuntimeException(e);
        }
        binding.tvAppVersion.setText("Built at " + BuildConfig.COMPILE_TIME);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        usbDeviceHelper.removeListener(usbDeviceListener);
        usbDeviceHelper.onDestroy();
        protocolHelper.removeListener(protocolListener);
        protocolHelper.onDestroy();
        ffListenerManager.removeListener();
        mediaHelper.destroy();
        mediaHelper2.destroy();

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
        if ((view == binding.surface || view == binding.texture) && !isMapMini) {
            // 地图缩小，视频变大
            //reorder widgets
            if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
                binding.texture.setTranslationZ(1);
            } else {
                binding.surface.setTranslationZ(1);
            }

            //resize widgets
            resizeMap(false);
            resizeVideo(true);
            //disable user login widget on map
//            widgetMap.getUserAccountLoginWidget().setVisibility(View.GONE);
            isMapMini = true;
        } else if (view == binding.widgetMap && isMapMini) {
            // 地图变大，视频缩小
            //reorder widgets
            if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
                binding.texture.setTranslationZ(4);
            } else {
                binding.surface.setTranslationZ(4);
            }

            //resize widgets
            resizeMap(true);
            resizeVideo(false);
            //enable user login widget on map
//            widgetMap.getUserAccountLoginWidget().setVisibility(View.VISIBLE);
            isMapMini = false;
        }
        else if (view == binding.btnShot) {
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
                                binding.surface, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
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
                    Bitmap bitmap = binding.texture.getBitmap();
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
        } else if (view == binding.btnStartRecord) {
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
        } else if (view == binding.btnStopRecord) {
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
                                ImageUtils.save2Album(path, "fly", System.currentTimeMillis() + ".mp4", true);
                            }
                        }).start();
                    }
                }
                break;
            }
        } else if (view == binding.btnFfInfo) {
            String info = FFJNI.avcodecinfo();
            Log.d("codec info", info);
            info = FFJNI.avformatinfo();
            Log.d("format info", info);
            info = FFJNI.urlprotocolinfo();
            Log.d("protocol info", info);
        } else if (view == binding.btnUpgradeGnd) {
            if (usbDeviceHelper.getUsbStatus() != UsbDeviceHelper.USB_CONNECTED || usbDeviceHelper.getDeviceType() != DEVICE_TYPE.TYPE_8020) {
                Toast.makeText(MainActivity.this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_GRD);
        } else if (view == binding.btnUpgradeSky) {
            if (usbDeviceHelper.getUsbStatus() != UsbDeviceHelper.USB_CONNECTED || usbDeviceHelper.getDeviceType() != DEVICE_TYPE.TYPE_8020) {
                Toast.makeText(MainActivity.this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_SKY);
        } else if (view == binding.btnUpgradeV3) {
            Intent intent = new Intent(this, V3OtaActivity.class);
            startActivity(intent);
        } else if (view == binding.btnUpgradeV4) {
            if (usbDeviceHelper.getUsbStatus() != UsbDeviceHelper.USB_CONNECTED || usbDeviceHelper.getDeviceType() != DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(MainActivity.this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_V4);
        } else if (view == binding.btnPairV4) {
            protocolHelper.ar8030StartPair();
        } else if (view == binding.btnGetChannelInfoV4) {
            protocolHelper.ar8030GetChannelInfo(false);
        } else if (view == binding.btnSetBandwidthV4) {
            Intent intent = new Intent(this, V4BandwidthActivity.class);
            startActivity(intent);
        } else if (view == binding.btnConfigV4) {
            Intent intent = new Intent(this, V4ConfigActivity.class);
            startActivity(intent);
        } else if (view == binding.btnSysinfoV4) {
            protocolHelper.ar8030GetSysInfo(false);
        } else if (view == binding.btnRtsp) {
            Intent intent = new Intent(this, RtspSingleActivity.class);
            startActivity(intent);
        } else if (view == binding.btnRtspMulti) {
            Intent intent = new Intent(this, RtspMultiActivity.class);
            startActivity(intent);
        } else if (view == binding.btnUdp) {
            Intent intent = new Intent(this, UdpRtpActivity.class);
            startActivity(intent);
        } else if (view == binding.btnChuanyun) {
            Intent intent = new Intent(this, ChuanYunActivity.class);
            startActivity(intent);
        } else if (view == binding.btnMcu) {
            Intent intent = new Intent(this, McuActivity.class);
            startActivity(intent);
        } else if (view == binding.btnPreference) {
            Intent intent = new Intent(this, PreferenceActivity.class);
            startActivity(intent);
        }
    }

    private void readAoa() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (Fly.isRk()) {
                    HostSwitch.AoaMode aoaMode = HostSwitch.getMode();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.swPower.setOnCheckedChangeListener(null);

                            switch (aoaMode) {

                                case POWER_OFF:
                                    binding.swPower.setChecked(false);
                                    break;
                                case POWER_ON:
                                    binding.swPower.setChecked(true);
                                    break;
                                case UNKNOWN:
                                    break;
                            }

                            binding.swPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            HostSwitch.switchPower(b);
                                        }
                                    }).start();
                                }
                            });
                        }
                    });
                } else {
                    AoaSwitch.AoaMode aoaMode = AoaSwitch.getMode();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.swLink.setOnCheckedChangeListener(null);
                            binding.swPower.setOnCheckedChangeListener(null);

                            switch (aoaMode) {

                                case LINK_OFF_POWER_OFF:
                                    binding.swLink.setChecked(false);
                                    binding.swPower.setChecked(false);
                                    break;
                                case LINK_OFF_POWER_ON:
                                    binding.swLink.setChecked(false);
                                    binding.swPower.setChecked(true);
                                    break;
                                case LINK_ON_POWER_OFF:
                                    binding.swLink.setChecked(true);
                                    binding.swPower.setChecked(false);
                                    break;
                                case LINK_ON_POWER_ON:
                                    binding.swLink.setChecked(true);
                                    binding.swPower.setChecked(true);
                                    break;
                                case UNKNOWN:
                                    break;
                            }

                            binding.swLink.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AoaSwitch.switchLink(b);
                                        }
                                    }).start();
                                }
                            });

                            binding.swPower.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                                @Override
                                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            AoaSwitch.switchPower(b);
                                        }
                                    }).start();
                                }
                            });
                        }
                    });
                }
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
                    switch (requestCode) {
                        case REQ_OTA_GRD:
                        case REQ_OTA_SKY:
                            upgradeHelper = new UpgradeHelper(fis);
                            upgradeHelper.setListener(upgradeListener);
                            upgradeHelper.startUpgradeApp(requestCode == REQ_OTA_SKY);
                            binding.btnUpgradeGnd.setEnabled(false);
                            binding.btnUpgradeSky.setEnabled(false);
                            break;
                        case REQ_OTA_V4:
                            protocolHelper.ar8030Upgrade(fis, upgradeListener);
                            binding.btnUpgradeV4.setEnabled(false);
                            break;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Keep
    private final UsbDeviceListener usbDeviceListener = new UsbDeviceListener() {
        @Override
        public void onNoUsbDevice() {

        }

        @Override
        public void onStartReadData(DEVICE_TYPE deviceType) {
            switch (deviceType) {
                case TYPE_8020:
                    break;
                case TYPE_8030:
                    // If you use port 2 for AR8030 socket, set it here. Default port is 3.
//                    ProtocolHelper.ar8030SetPort(2);
                    break;
            }
            protocolHelper.onStartReadData(deviceType.name());
        }

        @Override
        public void onDisconnect(DEVICE_TYPE deviceType) {
            bitRateHelperVideo.stop();
            protocolHelper.onDisconnect(deviceType.name());
        }

        @Override
        public void onVideoData(byte[] data, int length, DEVICE_TYPE deviceType) {
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
        public void onCtrlData(byte[] data, int length, DEVICE_TYPE deviceType) {
            protocolHelper.parseData(data, length, deviceType.name());
        }
    };

    private DeviceInfo arlinkDevice = new DeviceInfo();

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseFlyPacket packet, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
//            Log.d(TAG, "onReadCmd: " + packet.getClass().getSimpleName() + "\n" + packet.toString());
            if (packet instanceof DeviceInfo) {
                DeviceInfo deviceInfo = (DeviceInfo) packet;
                arlinkDevice = deviceInfo;

                if (deviceInfo.skyGround == 1) {
                    // todo
                    //  (optional) query osd info
                    protocolHelper.startQueryWirelessInfo();
                } else {
                    Log.d(TAG, "deviceInfo.skyGround != 1");
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
                            binding.tvRx.setText(packet.getClass().getSimpleName() + ": " + stringBuilder.toString());
                        }
                    });
                }
            } else if (packet instanceof UsbRx) {
                byte[] data = ((UsbRx) packet).data;
                if (data != null && data.length > 0) {
                    // todo
                    //  handle data (such as mavlink packages bytes) read through usb bypass

                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i<data.length; i++) {
                        stringBuilder.append(String.format("%02X ", data[i]));
                    }
                    Log.d(TAG, "onRead UsbRx: " + stringBuilder.toString());

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            binding.tvRx.setText("UsbRx: " + stringBuilder.toString());
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
            } else if (packet instanceof RcStatus8030) {
                // AR8030 status
            } else if (packet instanceof ChanInfo8030) {
                // AR8030 channel info
            } else if (packet instanceof SysInfo8030) {
                // AR8030 system info
                Toast.makeText(MainActivity.this, (isRemote? "dev: ": "ap: ") + packet, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onWrite(byte[] data) {
            usbDeviceHelper.writeData(data);
        }

        @Override
        public void onPairTimeOut(com.fly.station.prorocol.DEVICE_TYPE deviceType) {
            // Now only for 8030, pair manually time out
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(MainActivity.this, "Pair time out", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPairSuccess(com.fly.station.prorocol.DEVICE_TYPE deviceType) {
            // Now only for 8030, pair manually success
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(MainActivity.this, "Pair success", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLinked(com.fly.station.prorocol.DEVICE_TYPE deviceType) {
            // Now only for 8030, link ready automatically
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(MainActivity.this, "Link ready", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLinkLost(com.fly.station.prorocol.DEVICE_TYPE deviceType) {
            // Now only for 8030, link lost automatically
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(MainActivity.this, "Link lost", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onConfigJson(@Nullable String jsonString, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetConfigJson(boolean result, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onResetConfigJson(boolean result, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

    };

    private void renderWirelessInfo(WirelessInfo wirelessOSD) {
        String modulation = "";
        if (wirelessOSD.lockStatus == 0x00) {
            binding.tvOsdLocked.setTextColor(Color.RED);
            binding.tvOsdLocked.setText("DisConnect");
        } else {
            binding.tvOsdLocked.setTextColor(Color.parseColor("#7CFC00"));
            binding.tvOsdLocked.setText("Connected");
        }


        DecimalFormat format = new DecimalFormat("##0.000");
        String strSkySNR = format.format(wirelessOSD.skySNR);
        binding.tvRC.setText("SNR:     "+ strSkySNR + " dB\n");
        if (wirelessOSD.lockStatus == 0x00) {
            binding.tvRC.setText("SNR:      --- dB\n");
            binding.tvRC.append("Power0: ---\n");
            binding.tvRC.append("Power1: ---\n");

            binding.tvRC.append("Energy0: --- dBm \n");
            binding.tvRC.append("Energy1: --- dBm \n");

            binding.tvRC.append("E_rate:    --- ");
        }
        else {
            binding.tvRC.append("Power0: " + wirelessOSD.skyAgcVal[0] + "\n");
            binding.tvRC.append("Power1: " + wirelessOSD.skyAgcVal[1] + "\n");


            int dBand = arlinkDevice.band;
            if (dBand == 1) {
                binding.tvRC.append("Energy0: " + (1 - wirelessOSD.skyAgcVal[0]) + " dBm\n");
                binding.tvRC.append("Energy1: " + (1 - wirelessOSD.skyAgcVal[1]) + " dBm\n");
            } else if (dBand == 2) {
                binding.tvRC.append("Energy0: " + (9 - wirelessOSD.skyAgcVal[0]) + " dBm\n");
                binding.tvRC.append("Energy1: " + (9 - wirelessOSD.skyAgcVal[1]) + " dBm\n");
            } else {
                binding.tvRC.append("Energy0: --- dBm \n");
                binding.tvRC.append("Energy1: --- dBm \n");
            }

            binding.tvRC.append("E_rate:    " + (100 - wirelessOSD.rcLock));
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

            //binding.tvVT.setText("SNR0:     --- dB\n");
            binding.tvVT.setText("SNR:     --- dB\n");

            binding.tvVT.append("Power0:   --- \n");
            binding.tvVT.append("Power1:   --- \n");

            binding.tvVT.append("Energy0: --- dBm \n");
            binding.tvVT.append("Energy1: --- dBm \n");
            binding.tvVT.append("MCS:   "  + "--- \n");
            binding.tvVT.append("E_rate:    " + "--- ");

        } else {
            //binding.tvVT.setText("SNR0:     " + format.format(wirelessOSD.snrValue[0]) + " dB\n");
            binding.tvVT.setText("SNR:     " + format.format(wirelessOSD.snrValue[1]) + " dB\n");
            binding.tvVT.append("Power0:   " + wirelessOSD.agcValue[0] + "\n");
            binding.tvVT.append("Power1:   " + wirelessOSD.agcValue[1] + "\n");


            int dBand = arlinkDevice.band;
            if (dBand == 1) {
                binding.tvVT.append("Energy0: " + (1 - wirelessOSD.agcValue[0]) + " dBm\n");
                binding.tvVT.append("Energy1: " + (1 - wirelessOSD.agcValue[1]) + " dBm\n");
            } else if (dBand == 2){

                binding.tvVT.append("Energy0: " + (9 - wirelessOSD.agcValue[0]) + " dBm\n");
                binding.tvVT.append("Energy1: " + (9 - wirelessOSD.agcValue[1]) + " dBm\n");
            } else {
                binding.tvVT.append("Energy0: --- dBm \n");
                binding.tvVT.append("Energy1: --- dBm \n");
            }

            binding.tvVT.append("MCS:   "  + modulation + "\n");
            binding.tvVT.append("E_rate:    " + wirelessOSD.errCnt);

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
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_5);
        } else if (vtScore >= 55) {
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_4);
        } else if (vtScore >= 35) {
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_3);
        }  else if (vtScore >= 15) {
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_2);
        } else if (vtScore > 0 && vtScore < 10) {
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_1);
        }

        if (wirelessOSD.lockStatus == 0x00)
            binding.imageVTScore.setImageResource(R.mipmap.fpv_topbar_signal_level_0);


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
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_5);
        } else if (rcScore >= 55) {
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_4);
        } else if (rcScore >= 30) {
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_3);
        }  else if (rcScore >= 15) {
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_2);
        } else if (rcScore >= 0 && rcScore < 15) {
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_1);
        }

        if (wirelessOSD.lockStatus == 0x00)
            binding.imageRCScore.setImageResource(R.mipmap.fpv_topbar_signal_level_0);

        binding.tvRCScore.setText("" + rcScore);
        binding.tvVTScore.setText("" + vtScore);
    }

    /**
     * Change the size of the video of channel DECODE_CHANNEL
     * @param videoWidth
     * @param videoHeight
     */
    private void setVideoLayout(int videoWidth, int videoHeight) {
        float aspectRatio = ((float) binding.rootView.getWidth()) / binding.rootView.getHeight();
        float aspectRatioNew = ((float) videoWidth) / videoHeight;
        View viewToChange = mediaHelper.getDecodeMode() == MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE? binding.texture: binding.surface;
        if (aspectRatio > aspectRatioNew) {
            float realWidth = ((float) (binding.rootView.getHeight())) * aspectRatioNew;
            if (isMapMini) {
                ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
                layoutParams.width = (int) realWidth;
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                viewToChange.requestLayout();
            }

            videoWidgetWidth = (int) realWidth;
            videoWidgetHeight = binding.rootView.getHeight();
        } else {
            float realHeight = ((float) (binding.rootView.getWidth())) / aspectRatioNew;
            if (isMapMini) {
                ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
                layoutParams.height = (int) realHeight;
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                viewToChange.requestLayout();
            }

            videoWidgetWidth = binding.rootView.getWidth();
            videoWidgetHeight = (int) realHeight;
        }
    }

    /**
     * Change the size of the video of channel DECODE_CHANNEL2
     *  @param videoWidth
     * @param videoHeight
     */
    private void setVideoLayout2(int videoWidth, int videoHeight) {
        float aspectRatio = ((float) binding.surface2.getWidth()) / binding.surface2.getHeight();
        float aspectRatioNew = ((float) videoWidth) / videoHeight;
        View viewToChange = binding.surface2;
        if (aspectRatio > aspectRatioNew) {
            float realWidth = ((float) (binding.surface2.getHeight())) * aspectRatioNew;
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.width = (int) realWidth;
            layoutParams.height = binding.surface2.getHeight();
            viewToChange.requestLayout();
        } else {
            float realHeight = ((float) (binding.surface2.getWidth())) / aspectRatioNew;
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.height = (int) realHeight;
            layoutParams.width = binding.surface2.getWidth();
            viewToChange.requestLayout();
        }
    }

    // For MEDIACODEC_SURFACE and MEDIACODEC_TEXTURE modes, to record H264 stream
    private final MediaListener mediaListener = new MediaListener() {
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

    private final FFListener ffListener = new FFListener() {
        @Override
        public void onMediaFormat(String format, int width, int height, int frameRateNum, int frameRateDen, long bitRate, int handler) {
            if (handler == DECODE_CHANNEL) {
                setVideoLayout(width, height);
                mediaHelper.updateVideoSize(width, height);
            } else if (handler == DECODE_CHANNEL2) {
                setVideoLayout2(width, height);
                mediaHelper2.updateVideoSize(width, height);
            }
        }
    };

    private final BitRateHelper.OnBitRateListener bitRateListenerVideo = new BitRateHelper.OnBitRateListener() {
        @Override
        public void onBitRate(long bitRate, String readable) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    binding.tvBitrateVideo.setText(readable);
                }
            });
        }
    };

    private final UpgradeHelper.UpgradeListener upgradeListener = new UpgradeHelper.UpgradeListener() {
        @Override
        public void onStart() {
            binding.tvUpdateProcess.setText(R.string.ota_start);
        }

        @Override
        public void onProcess(int curFrame, int totalFrame) {
            binding.tvUpdateProcess.setText(curFrame + " / " + totalFrame);
        }

        @Override
        public void onResend(int curFrame, int totalFrame) {
            binding.tvUpdateProcess.setText(getString(R.string.ota_resend, curFrame, totalFrame));
        }

        @Override
        public void onFlashing() {
            binding.tvUpdateProcess.setText(R.string.ota_ing);
        }

        @Override
        public void onComplete() {
            binding.tvUpdateProcess.setText(R.string.ota_finish);

            binding.btnUpgradeGnd.setEnabled(true);
            binding.btnUpgradeSky.setEnabled(true);

            binding.btnUpgradeV4.setEnabled(true);
        }

        @Override
        public void onFail(String errMsg) {
            binding.tvUpdateProcess.setText(R.string.ota_fail + "\n" + errMsg);

            binding.btnUpgradeGnd.setEnabled(true);
            binding.btnUpgradeSky.setEnabled(true);

            binding.btnUpgradeV4.setEnabled(true);
        }

        @Override
        public void onWrite(byte[] data) {
            if (usbDeviceHelper.getUsbStatus() == UsbDeviceHelper.USB_CONNECTED) {
                usbDeviceHelper.writeData(data);
            }
        }
    };

    private void resizeMap(boolean isEnlarge) {
        if (isEnlarge) {
            // enlarge
            ResizeAnimation enlargeAnimation = new ResizeAnimation(true, binding.widgetMap, mapWidgetWidth, mapWidgetHeight, deviceWidth, deviceHeight, 0, 0);
            binding.widgetMap.startAnimation(enlargeAnimation);
        } else {
            // shrink
            ResizeAnimation shrinkAnimation = new ResizeAnimation(false, binding.widgetMap, deviceWidth, deviceHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
            binding.widgetMap.startAnimation(shrinkAnimation);
        }
    }

    private void resizeVideo(boolean isEnlarge) {
        if (decodeMode == Constants.DECODE_MODE_MEDIACODEC_TEXTURE) {
            if (isEnlarge) {
                // enlarge
                ResizeAnimation enlargeAnimation = new ResizeAnimation(true, binding.texture, mapWidgetWidth, mapWidgetHeight, videoWidgetWidth, videoWidgetHeight, 0, 0);
                binding.texture.startAnimation(enlargeAnimation);
            } else {
                // shrink
                ResizeAnimation shrinkAnimation = new ResizeAnimation(false, binding.texture, videoWidgetWidth, videoWidgetHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
                binding.texture.startAnimation(shrinkAnimation);
            }
        } else {
            if (isEnlarge) {
                // enlarge
                ResizeAnimation enlargeAnimation = new ResizeAnimation(true, binding.surface, mapWidgetWidth, mapWidgetHeight, videoWidgetWidth, videoWidgetHeight, 0, 0);
                binding.surface.startAnimation(enlargeAnimation);
            } else {
                // shrink
                ResizeAnimation shrinkAnimation = new ResizeAnimation(false, binding.surface, videoWidgetWidth, videoWidgetHeight, mapWidgetWidth, mapWidgetHeight, mapWidgetMarginRight, mapWidgetMarginBottom);
                binding.surface.startAnimation(shrinkAnimation);
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