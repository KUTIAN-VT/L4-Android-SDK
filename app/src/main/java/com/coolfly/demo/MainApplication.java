package com.coolfly.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;

import com.coolfly.demo.preference.PreferenceActivity;
import com.coolfly.demo.utils.Constants;
import com.coolfly.demo.utils.ImageUtils;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.aoalibrary.host.UsbDeviceListener;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.loglibrary.Loggers;
import com.fly.medialibrary.MediaHelper;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.SysInfo8030;
import com.fly.station.prorocol.bean.Throughput8030;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2021/12/6 5:39 下午
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    private static final Loggers logger = new Loggers(Loggers.LOG_TYPE_ALL, TAG, Loggers.FILE_LOGGER_NORMAL);
    public static Context applicationContext;
    private ProtocolHelper protocolHelper;
    private UsbDeviceHelper usbDeviceHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        init();
    }

    private void init() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                // Preference
                PreferenceActivity.initPreference();

                // Log
                // We can write SDK logs to logcat/serial port/file
                // If you do not want to show logs produced by SDK, just remove dependency of loglibrary.aar
                if (!PreferenceActivity.preferenceObject.write_log_to_file) {
                    Loggers.addToBlackList(Loggers.LOG_TYPE_FILE);
                }
                if (!PreferenceActivity.preferenceObject.write_log_to_serial) {
                    Loggers.addToBlackList(Loggers.LOG_TYPE_SERIAL);
                }

                // Protocol
                logger.d("ProtocolHelper init");
                // Switch log of VPN
                com.fly.station.prorocol.Constants.isShowAR8030VPNLog = PreferenceActivity.preferenceObject.show_ar8030_vpn_log;
                // Switch log of package parsing
                com.fly.station.prorocol.Constants.isShowAR8030ParseLog = PreferenceActivity.preferenceObject.show_ar8030_parse_log;
                protocolHelper = ProtocolHelper.getInstance();
                logger.d("ProtocolHelper init");
                protocolHelper.addListener(protocolListener);

                // USB
                logger.d("UsbDeviceHelper init");
                UsbDeviceHelper.isShowLog = PreferenceActivity.preferenceObject.show_usb_log;
                usbDeviceHelper = UsbDeviceHelper.getInstance(applicationContext);
                usbDeviceHelper.addListener(usbDeviceListener);
                usbDeviceHelper.onResume();

                /*
                 * Initialize media
                 */
                logger.d("MediaHelper init");
                MediaHelper.init(applicationContext);
                /*
                 * Initialize FFmpeg
                 * @param avLog true-Log output, false-No log output
                 * @param frameIPLastBytes returns the last few bytes of I frame (I in IDR and Slice) and P frame. <=0 means no return. >0 means the number of bytes returned. It is returned through onFrameIPLastBytes callback.
                 */
                FFJNI.init(PreferenceActivity.preferenceObject.show_ffmpeg_log);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
                boolean isHwDecode = sharedPreferences.getBoolean(Constants.PREF_IS_HW_DECODE, true);

                /*
                 * Set whether to hardware decode (default value is true). This method needs to be called before SurfaceView is created to take effect.
                 * @param isHw
                 * @return Whether the setting is successful
                 */
                FFJNI.setHwDecode(isHwDecode);
                FFListenerManager.addListener(MainApplication.applicationContext, ffListener);
            }
        }).start();
    }

    @Keep
    private final UsbDeviceListener usbDeviceListener = new UsbDeviceListener() {
        @Override
        public void onNoUsbDevice() {

        }

        @Override
        public void onStartReadData(com.fly.aoalibrary.DEVICE_TYPE deviceType) {
            switch (deviceType) {
                case TYPE_8020:
                    break;
                case TYPE_8030:
                    // Initialize P401
                    // 1 means 1v1 mode. >1 means 1vN mode, where N is the number of dev.
                    // It must be set before ProtocolHelper initialized. After changed, it will take effect after rebooting the Android system.
                    boolean res = ProtocolHelper.ar8030Set1VNMode(PreferenceActivity.preferenceObject.p401_dev_count);
                    if (!res) {
                        logger.d("set 1vN mode failed, please refer to logcat");
                    }
                    if (PreferenceActivity.preferenceObject.p401_dev_count == 1) {
                        // Set the buffer size for each slot and port. The default value is 60000 for rx and 40000 for tx.
                        res = ProtocolHelper.ar8030SetBufferSize(PreferenceActivity.preferenceObject.p401_rx_buffer_slot0_port2,
                                PreferenceActivity.preferenceObject.p401_tx_buffer_slot0_port2, 0, 2);
                        if (!res) {
                            logger.d("set 1v1 buffer failed, see logcat");
                        }
                        res = ProtocolHelper.ar8030SetBufferSize(PreferenceActivity.preferenceObject.p401_rx_buffer_slot0_port3,
                                PreferenceActivity.preferenceObject.p401_tx_buffer_slot0_port3, 0, 3);
                        if (!res) {
                            logger.d("set 1v1 buffer failed, see logcat");
                        }
                    } else {
                        for (int i = 0; i<8; i++) {
                            res = ProtocolHelper.ar8030SetBufferSize(PreferenceActivity.preferenceObject.p401_1vn_rx_buffer_port2,
                                    PreferenceActivity.preferenceObject.p401_1vn_tx_buffer_port2, i, 2);
                            if (!res) {
                                logger.d("set 1vN buffer failed, see logcat");
                                break;
                            }
                        }
                        for (int i = 0; i<8; i++) {
                            res = ProtocolHelper.ar8030SetBufferSize(PreferenceActivity.preferenceObject.p401_1vn_rx_buffer_port3,
                                    PreferenceActivity.preferenceObject.p401_1vn_tx_buffer_port3, i, 3);
                            if (!res) {
                                logger.d("set 1vN buffer failed, see logcat");
                                break;
                            }
                        }
                    }
                    // Set port for eth, default is 3.
                    ProtocolHelper.ar8030SetPortEth(PreferenceActivity.preferenceObject.p401_port_eth);
                    // Set port for USB passthrough, default is 2.
                    ProtocolHelper.ar8030SetPortPassthrough(PreferenceActivity.preferenceObject.p401_port_passthrough);
                    // Set MTU for P401. Default value is 2500.
                    ProtocolHelper.ar8030SetMTU(PreferenceActivity.preferenceObject.p401_mtu);
                    // Set IP for P401. Default value is 192.168.144.55
                    ProtocolHelper.ar8030SetIP(PreferenceActivity.preferenceObject.p401_ip);
                    // Set SubnetMask for P401. Default value is 255.255.255.0
                    ProtocolHelper.ar8030SetSubnetMask(PreferenceActivity.preferenceObject.p401_subnet_mask);
                    // Set whether to use datagram. Default value is false.
                    ProtocolHelper.ar8030SetUseDatagram(PreferenceActivity.preferenceObject.p401_datagram);
                    break;
            }
            protocolHelper.onStartReadData(deviceType.name());
        }

        @Override
        public void onDisconnect(com.fly.aoalibrary.DEVICE_TYPE deviceType) {
            protocolHelper.onDisconnect(deviceType.name());
        }

        @Override
        public void onVideoData(byte[] data, int length, com.fly.aoalibrary.DEVICE_TYPE deviceType) {

        }

        @Override
        public void onAudioData(byte[] data, int length) {

        }

        @Override
        public void onCtrlData(byte[] data, int length, com.fly.aoalibrary.DEVICE_TYPE deviceType) {
            protocolHelper.parseData(data, length, deviceType.name());
        }
    };

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {
            logger.d("Ready! : " + deviceType);
        }

        @Override
        public void onReadCmd(BaseFlyPacket packet, DEVICE_TYPE deviceType, boolean isRemote) {
            if (packet instanceof SysInfo8030) {
                // AR8030 system info
                Toast.makeText(applicationContext, (isRemote? "dev: ": "ap: ") + packet, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public int onWrite(byte[] data) {
            return usbDeviceHelper.writeData(data);
        }

        @Override
        public void onPairOperated(DEVICE_TYPE deviceType, int slot, boolean isStart) {
            // Now only for 8030, pair manually time out
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                if (isStart) {
                    Toast.makeText(applicationContext, "Pair started on slot " + slot, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(applicationContext, "Pair stopped on slot " + slot, Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually time out
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(applicationContext, "Pair timeout on slot " + slot, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually success
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(applicationContext, "Pair success on slot " + slot, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link ready automatically
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(applicationContext, "Link ready on slot " + slot, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link lost automatically
            if (deviceType == com.fly.station.prorocol.DEVICE_TYPE.TYPE_8030) {
                Toast.makeText(applicationContext, "Link lost on slot " + slot, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onConfigJson(@Nullable String json, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetConfigJson(boolean res, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onResetConfigJson(boolean res, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int slot, String mac) {
            // Now only for 8030
        }

        @Override
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // AR8030 throughput data received
        }

        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String message) {
            // Now only for 8030
        }
    };

    @Keep
    private final FFListener ffListener = new FFListener() {
        // Retrieving result after calling FFJNI.shotFrame
        @Override
        public void onShotFrame(String path, boolean success, int handler) {
            Toast.makeText(applicationContext, success? R.string.take_photo_success: R.string.take_photo_fail, Toast.LENGTH_SHORT).show();
            if (success) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.save2Album(path, "fly", System.currentTimeMillis() + ".jpg", false);
                    }
                }).start();
            }
        }

        // Retrieving record result after calling FFJNI.stopRecord
        @Override
        public void onRecordVideo(String path, boolean success, int handler) {
            Toast.makeText(applicationContext, success? R.string.record_success: R.string.record_fail, Toast.LENGTH_SHORT).show();
            if (success) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.save2Album(path, "fly", System.currentTimeMillis() + ".mp4", true);
                    }
                }).start();
            }
        }

        @Override
        public void onDowngradeToSwDecode(int handler) {
            Toast.makeText(applicationContext, getString(R.string.sw_decode, handler), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onSpsPps(byte[] sps, byte[] pps, int handler) {
            StringBuilder stringBuilder = new StringBuilder(sps.length);
            for (int i = 0; i<sps.length; i++) {
                stringBuilder.append(String.format("%02X ", sps[i]));
            }
            logger.d("onSpsPpsAnnexB sps: " + stringBuilder.toString());

            stringBuilder = new StringBuilder(pps.length);
            for (int i = 0; i<pps.length; i++) {
                stringBuilder.append(String.format("%02X ", pps[i]));
            }
            logger.d("onSpsPpsAnnexB pps: " + stringBuilder.toString());
        }

        @Override
        public void onFrameIPLastBytes(byte[] result, int handler) {
            StringBuilder stringBuilder = new StringBuilder(result.length);
            for (int i = 0; i<result.length; i++) {
                stringBuilder.append(String.format("%02X ", result[i]));
            }
            logger.d("onFrameIPLastBytes: " + stringBuilder.toString());
        }

        @Override
        public void onError(int code, String msg, int handler) {
            Toast.makeText(applicationContext, "code: " + code + ", msg: " + msg, Toast.LENGTH_SHORT).show();
        }
    };
}
