package com.coolfly.demo;


import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
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
import com.wuadam.medialibrary.H264Saver;
import com.wuadam.medialibrary.MediaHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private AccessoryHelper accessoryHelper;
    private ArlinkListen arlinkListen;
    private UsbDeviceHelper usbDeviceHelper;
    private ProtocolHelper protocolHelper;
    private MediaHelper mediaHelper;
    private FFListenerManager ffListenerManager;
    private BitRateHelper bitRateHelperVideo;
    private final boolean NEED_SAVE_H264 = false;
    private H264Saver h264Saver;
    private PermissionHelper permissionHelper;
    private UpgradeHelper upgradeHelper;

    private SurfaceView surface;
    private TextureView texture;
    private TextView tvBitrateVideo;
    private TextView widgetMap;
    private Button btnShot;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private Button btnHwDecoder;
    private Spinner spDecodeMode;
    private SwitchCompat swHwDecode;
    private SwitchCompat swAoa;
    private SwitchCompat swFpv;
    private Button btnUpgradeGrd;
    private Button btnUpgradeSky;
    private TextView tvUpdateProcess;

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

    private final int REQ_OTA_GRD = 1;
    private final int REQ_OTA_SKY = 2;

    // support 5 channels, from 1 to 5
    private final int DECODE_CHANNEL = 1;
    // support 2 channels, from 0 to 1
    private final int STREAM_CHANNEL = 1;

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

        videoWidgetWidth = outPoint.x;
        videoWidgetHeight = (int) (videoWidgetWidth / MediaHelper.VIDEO_ASPECT);

        surface = findViewById(R.id.surface);
        texture = findViewById(R.id.texture);
        tvBitrateVideo = findViewById(R.id.tv_bitrate_video);
        widgetMap = findViewById(R.id.widget_map);
        btnShot = findViewById(R.id.btn_shot);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);
        btnHwDecoder = findViewById(R.id.btn_hw_decoder);
        spDecodeMode = findViewById(R.id.sp_decode_mode);
        swHwDecode = findViewById(R.id.sw_hw_decode);
        swAoa = findViewById(R.id.sw_aoa);
        swFpv = findViewById(R.id.sw_fpv);
        btnUpgradeGrd = findViewById(R.id.btn_upgrade_grd);
        btnUpgradeSky = findViewById(R.id.btn_upgrade_sky);
        tvUpdateProcess = findViewById(R.id.tv_update_process);

        tvVT = findViewById(R.id.tv_VT);
        tvRC = findViewById(R.id.tv_RC);
        tvRcScore = findViewById(R.id.tv_RC_Score);
        tvVtScore = findViewById(R.id.tv_VT_Score);
        imageVT = findViewById(R.id.image_VT_Score);
        imageRC = findViewById(R.id.image_RC_Score);
        tvOSDLocked = findViewById(R.id.tv_osd_locked);
        tvUart = findViewById(R.id.tv_uart);

        // decode mode start
        // 0. FFmpeg with hw/sw decoder, render in SurfaceView
        // 1. MediaCodec with hw decoder, render in SurfaceView
        // 2. MediaCodec with hw decoder, render in TextureView
        String[] decodeModeArr = new String[]{"FFmpeg-GL-SurfaceView", "MediaCodec-SurfaceView", "MediaCodec-TextureView"};
        ArrayAdapter<String> decodeModeAdapter = new ArrayAdapter<String>(this, R.layout.item_select, decodeModeArr);
        decodeModeAdapter.setDropDownViewResource(R.layout.item_dropdown);
        spDecodeMode.setAdapter(decodeModeAdapter);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        decodeMode = sp.getInt(Constants.PREF_DECODE_MODE, Constants.DECODE_MODE_FF_GL_SURFACE);
        spDecodeMode.setSelection(decodeMode);
        if (decodeMode != Constants.DECODE_MODE_FF_GL_SURFACE) {
            btnShot.setVisibility(View.GONE);
            btnStartRecord.setVisibility(View.GONE);
            btnStopRecord.setVisibility(View.GONE);
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
                    builder.setMessage("使用非FFmpeg方式解码，将失去拍照录像功能。APP重启之后生效").setPositiveButton("确定", new DialogInterface.OnClickListener() {
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

        usbDeviceHelper = UsbDeviceHelper.getInstance(getApplicationContext());
        usbDeviceHelper.addListener(usbDeviceListener);

        protocolHelper = ProtocolHelper.getInstance();
        protocolHelper.addListener(protocolListener);

        switch (decodeMode) {
            case Constants.DECODE_MODE_FF_GL_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_GL_SURFACE, null, surface, null, null, DECODE_CHANNEL);
                break;
            case Constants.DECODE_MODE_MEDIACODEC_SURFACE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_SURFACE, null, surface, null, null, DECODE_CHANNEL);
                break;
            case Constants.DECODE_MODE_MEDIACODEC_TEXTURE:
                mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.MEDIACODEC_TEXTURE, texture, null, null, null, DECODE_CHANNEL);
                break;
        }

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
                builder.setMessage("APP重启之后生效").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        System.exit(0);
                    }
                }).setCancelable(false).show();
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
        h264Saver.stop();
        FFJNI.stop(DECODE_CHANNEL);
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
        } else if (view == btnStartRecord) {
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
        } else if (view == btnStopRecord) {
            FFJNI.stopRecord(DECODE_CHANNEL);
            // 在回调里面toast和保存到相册
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

    private FFListener ffListener = new FFListener() {
        @Override
        public void onShotFrame(String path, boolean success, int handler) {
            Toast.makeText(MainActivity.this, success? "拍照成功": "拍照失败", Toast.LENGTH_SHORT).show();
            if (success) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.save2Album(path, "coolfly", System.currentTimeMillis() + ".jpg", false);
                    }
                }).start();
            }
        }

        @Override
        public void onRecordVideo(String path, boolean success, int handler) {
            Toast.makeText(MainActivity.this, success? "录像成功": "录像失败", Toast.LENGTH_SHORT).show();
            if (success) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.save2Album(path, "coolfly", System.currentTimeMillis() + ".mp4", true);
                    }
                }).start();
            }
        }

        @Override
        public void onSpsPps(byte[] sps, byte[] pps, int handler) {
            StringBuilder stringBuilder = new StringBuilder(sps.length);
            for (int i = 0; i<sps.length; i++) {
                stringBuilder.append(String.format("%02X ", sps[i]));
            }
            Log.d(TAG, "onSpsPpsAnnexB sps: " + stringBuilder.toString());

            stringBuilder = new StringBuilder(pps.length);
            for (int i = 0; i<pps.length; i++) {
                stringBuilder.append(String.format("%02X ", pps[i]));
            }
            Log.d(TAG, "onSpsPpsAnnexB pps: " + stringBuilder.toString());
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
            tvUpdateProcess.setText("升级开始");
        }

        @Override
        public void onProcess(int curFrame, int totalFrame) {
            tvUpdateProcess.setText(curFrame + " / " + totalFrame);
        }

        @Override
        public void onResend(int curFrame, int totalFrame) {
            tvUpdateProcess.setText("重发: " + curFrame + " / " + totalFrame);
        }

        @Override
        public void onFlashing() {
            tvUpdateProcess.setText("更新中");
        }

        @Override
        public void onComplete() {
            tvUpdateProcess.setText("完成");
            btnUpgradeGrd.setEnabled(true);
            btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onFail() {
            tvUpdateProcess.setText("失败");
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
                p.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                p.topToTop = ConstraintLayout.LayoutParams.UNSET;
            }

            view.requestLayout();
        }
    }
}