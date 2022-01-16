package com.coolfly.demo;

import static com.wuadam.aoalibrary.AccessoryHelper.USB_CONNECTED;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;

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
import com.coolfly.station.prorocol.bean.Uart5Rx;
import com.wuadam.aoalibrary.AccessoryHelper;
import com.wuadam.aoalibrary.AccessoryListener;
import com.wuadam.aoalibrary.AoaSwitch;
import com.wuadam.fflibrary.FFJNI;
import com.wuadam.fflibrary.listeners.FFListener;
import com.wuadam.fflibrary.listeners.FFListenerManager;
import com.wuadam.medialibrary.BitRateHelper;
import com.wuadam.medialibrary.H264Saver;
import com.wuadam.medialibrary.MediaHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private AccessoryHelper accessoryHelper;
    private ArlinkListen arlinkListen;
    private ProtocolHelper protocolHelper;
    private MediaHelper mediaHelper;
    private FFListenerManager ffListenerManager;
    private BitRateHelper bitRateHelperVideo;
    private final boolean NEED_SAVE_H264 = false;
    private H264Saver h264Saver;
    private PermissionHelper permissionHelper;
    private UpgradeHelper upgradeHelper;

    private SurfaceView surface;
    private TextView tvBitrateVideo;
    private TextView widgetMap;
    private Button btnShot;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private SwitchCompat swAoa;
    private SwitchCompat swFpv;
    private Button btnUpgradeGrd;
    private Button btnUpgradeSky;
    private TextView tvUpdateProcess;

    private boolean isMapMini = true;

    private int mapWidgetHeight;
    private int mapWidgetWidth;
    private int mapWidgetMarginRight;
    private int mapWidgetMarginBottom;

    private int deviceWidth;
    private int deviceHeight;

    private int videoWidgetWidth;
    private int videoWidgetHeight;

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
        tvBitrateVideo = findViewById(R.id.tv_bitrate_video);
        widgetMap = findViewById(R.id.widget_map);
        btnShot = findViewById(R.id.btn_shot);
        btnStartRecord = findViewById(R.id.btn_start_record);
        btnStopRecord = findViewById(R.id.btn_stop_record);
        swAoa = findViewById(R.id.sw_aoa);
        swFpv = findViewById(R.id.sw_fpv);
        btnUpgradeGrd = findViewById(R.id.btn_upgrade_grd);
        btnUpgradeSky = findViewById(R.id.btn_upgrade_sky);
        tvUpdateProcess = findViewById(R.id.tv_update_process);

        accessoryHelper = AccessoryHelper.getInstance(getApplicationContext(), true);
        accessoryHelper.addListener(accessoryListener);
        arlinkListen = new ArlinkListen();
        arlinkListen.setListener(arlinkDataListener);
        protocolHelper = ProtocolHelper.getInstance();
        protocolHelper.addListener(protocolListener);
        mediaHelper = new MediaHelper(MediaHelper.DECODE_MODE.FF_SURFACE_VIEW, null, surface, null);

        ffListenerManager = FFListenerManager.addListener(this, ffListener);

        bitRateHelperVideo = new BitRateHelper();
        bitRateHelperVideo.setListener(bitRateListenerVideo);

        String path = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_MOVIES).getAbsolutePath() + "/record";
        h264Saver = new H264Saver(path);

        permissionHelper = new PermissionHelper(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                AoaSwitch.AoaMode aoaMode = AoaSwitch.getMode();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
        protocolHelper.removeListener(protocolListener);
        protocolHelper.onDestroy();
        ffListenerManager.removeListener();
        h264Saver.stop();
        FFJNI.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] paramArrayOfInt) {
        super.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
        permissionHelper.onRequestPermissionsResult(requestCode, permissions, paramArrayOfInt);
    }

    public void onClick(View view) {
        if (view == surface && !isMapMini) {
            // 地图缩小，视频变大
            //reorder widgets
//            rootView.removeView(surface);
//            rootView.addView(surface, 0);
            surface.setTranslationZ(1);

            //resize widgets
            resizeMap(false);
            resizeVideo(true);
            //disable user login widget on map
//            widgetMap.getUserAccountLoginWidget().setVisibility(View.GONE);
            isMapMini = true;
        } else if (view == widgetMap && isMapMini) {
            // 地图变大，视频缩小
            //reorder widgets
//            rootView.removeView(surface);
//            rootView.addView(surface, rootView.indexOfChild(widgetMap) + 1);
            surface.setTranslationZ(4);

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
                FFJNI.shotFrame(file.getAbsolutePath());
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
                FFJNI.startRecordVideo(file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (view == btnStopRecord) {
            FFJNI.stopRecord();
            // 在回调里面toast和保存到相册
        } else if (view == btnUpgradeGrd) {
            if (AccessoryHelper.UsbStatus == USB_CONNECTED) {
                Toast.makeText(MainActivity.this, "AOA not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                FileInputStream fis = getUpgradeFis();
                if (fis != null) {
                    upgradeHelper = new UpgradeHelper(fis);
                    upgradeHelper.setListener(upgradeListener);
                    upgradeHelper.startUpgradeApp(false);
                    btnUpgradeGrd.setEnabled(false);
                    btnUpgradeSky.setEnabled(false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        } else if (view == btnUpgradeSky) {
            if (AccessoryHelper.UsbStatus == USB_CONNECTED) {
                Toast.makeText(MainActivity.this, "AOA not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                FileInputStream fis = getUpgradeFis();
                if (fis != null) {
                    upgradeHelper = new UpgradeHelper(fis);
                    upgradeHelper.setListener(upgradeListener);
                    upgradeHelper.startUpgradeApp(true);
                    btnUpgradeGrd.setEnabled(false);
                    btnUpgradeSky.setEnabled(false);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private FileInputStream getUpgradeFis() throws FileNotFoundException {
        String pathDir = MainApplication.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        String filePath = pathDir + "/Arlink.bin";
        File file = new File(filePath);
        if ((!file.isFile()) || (!file.exists())) {
            Toast.makeText(MainActivity.this, "Arlink.bin not found !!!", Toast.LENGTH_SHORT).show();
            File fileDir = new File(pathDir);
            fileDir.mkdirs();
            return null;
        }
        return new FileInputStream(file);
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
            protocolHelper.resetUart5PassThrough();
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
            if (channel == 1) {
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

    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseCoolflyPacket packet) {
            Log.d(TAG, "onReadCmd: " + packet.getClass().getSimpleName());
            if (packet instanceof DeviceInfo) {
                DeviceInfo deviceInfo = (DeviceInfo) packet;
                if (deviceInfo.skyGround == 1) {
                    protocolHelper.startUart5PassThrough();
                    Log.d(TAG, "startUart5PassThrough");
                } else {
                    Log.d(TAG, "deviceInfo.skyGround != 1");
                    protocolHelper.resetUart5PassThrough();
                    Log.d(TAG, "resetUart5PassThrough");
                }
            } else if (packet instanceof Uart5Rx) {
                byte[] data = ((Uart5Rx) packet).data;
                if (data != null && data.length > 0) {
                    // todo
                    //  handle data (such as mavlink packages bytes) read from plane

                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (int i = 0; i<data.length; i++) {
                        stringBuilder.append(String.format("%02X ", data[i]));
                    }
                    Log.d(TAG, "onReadMav: " + stringBuilder.toString());
                }
            } else if (packet instanceof ACK) {
                if (upgradeHelper != null) {
                    upgradeHelper.onAck();
                }
            }
        }

        @Override
        public void onWrite(byte[] data) {
            if (AccessoryHelper.UsbStatus == USB_CONNECTED) {
                accessoryHelper.ArlinkWriteData(data);
            }
        }
    };

    /**
     * todo
     *  call this method to send data (such as mavlink packages bytes) to plane
     * @param data
     * @param length
     */
    private void writeDataToPlane(byte[] data, int length) {
        if (accessoryHelper.getAccesoryStateMonitored() == AccessoryHelper.AccessoryConnected) {
            protocolHelper.sendUart5Tx(data, length);

            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (int i = 0; i<data.length; i++) {
                stringBuilder.append(String.format("%02X ", data[i]));
            }
            Log.d(TAG, "onWriteMav: " + stringBuilder.toString());
        }
    }

    private FFListener ffListener = new FFListener() {
        @Override
        public void onShotFrame(String path, boolean success) {
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
        public void onRecordVideo(String path, boolean success) {
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
        public void onSpsPps(byte[] sps, byte[] pps) {
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
            tvUpdateProcess.setText("start");
        }

        @Override
        public void onProcess(int curFrame, int totalFrame) {
            tvUpdateProcess.setText(curFrame + " / " + totalFrame);
        }

        @Override
        public void onComplete() {
            tvUpdateProcess.setText("complete");
            btnUpgradeGrd.setEnabled(true);
            btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onFail() {
            tvUpdateProcess.setText("fail");
            btnUpgradeGrd.setEnabled(true);
            btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onWrite(byte[] data) {
            if (AccessoryHelper.UsbStatus == USB_CONNECTED) {
                accessoryHelper.ArlinkWriteData(data);
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