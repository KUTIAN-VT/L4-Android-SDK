package com.coolfly.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.coolfly.demo.preference.PreferenceActivity;
import com.coolfly.demo.utils.Constants;
import com.coolfly.demo.utils.ImageUtils;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.listeners.FFListener;
import com.fly.fflibrary.listeners.FFListenerManager;
import com.fly.loglibrary.Loggers;
import com.fly.medialibrary.MediaHelper;
import com.fly.station.prorocol.ProtocolHelper;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2021/12/6 5:39 下午
 */
public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    public static Context applicationContext;
    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;

        PreferenceActivity.initPreference();

        // We can write SDK logs to logcat/serial port/file
        // If you do not want to show logs produced by SDK, just remove dependency of loglibrary.aar
        if (!PreferenceActivity.preferenceObject.write_log_to_file) {
            Loggers.addToBlackList(Loggers.LOG_TYPE_FILE);
        }
        if (!PreferenceActivity.preferenceObject.write_log_to_serial) {
            Loggers.addToBlackList(Loggers.LOG_TYPE_SERIAL);
        }

        /*
         * Initialize media
         */
        MediaHelper.init(this);

        /*
         * Initialize FFmpeg
         * @param avLog true-Log output, false-No log output
         * @param frameIPLastBytes returns the last few bytes of I frame (I in IDR and Slice) and P frame. <=0 means no return. >0 means the number of bytes returned. It is returned through onFrameIPLastBytes callback.
         */
        FFJNI.init(PreferenceActivity.preferenceObject.show_ffmpeg_log);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isHwDecode = sharedPreferences.getBoolean(Constants.PREF_IS_HW_DECODE, true);

        /*
         * Set whether to hardware decode (default value is true). This method needs to be called before SurfaceView is created to take effect.
         * @param isHw
         * @return Whether the setting is successful
         */
        FFJNI.setHwDecode(isHwDecode);
        FFListenerManager.addListener(MainApplication.applicationContext, ffListener);

        /*
         * Initialize P401
         */
        ProtocolHelper.ar8030SetPortEth(PreferenceActivity.preferenceObject.p401_port_eth);
        ProtocolHelper.ar8030SetPortPassthrough(PreferenceActivity.preferenceObject.p401_port_passthrough);
        // 1 means 1v1 mode. >1 means 1vN mode, where N is the number of dev.
        // It must be set before ProtocolHelper initialized. After changed, it will take effect after rebooting the Android system.
        ProtocolHelper.ar8030Set1VNMode(PreferenceActivity.preferenceObject.p401_dev_count);
        ProtocolHelper.ar8030SetBufferSize(PreferenceActivity.preferenceObject.p401_rx_buffer, PreferenceActivity.preferenceObject.p401_tx_buffer);
        com.fly.station.prorocol.Constants.isShowAR8030VPNLog = PreferenceActivity.preferenceObject.show_ar8030_vpn_log;
        com.fly.station.prorocol.Constants.isShowAR8030ParseLog = PreferenceActivity.preferenceObject.show_ar8030_parse_log;
    }

    FFListener ffListener = new FFListener() {
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
            Log.d(TAG, "onSpsPpsAnnexB sps: " + stringBuilder.toString());

            stringBuilder = new StringBuilder(pps.length);
            for (int i = 0; i<pps.length; i++) {
                stringBuilder.append(String.format("%02X ", pps[i]));
            }
            Log.d(TAG, "onSpsPpsAnnexB pps: " + stringBuilder.toString());
        }

        @Override
        public void onFrameIPLastBytes(byte[] result, int handler) {
            StringBuilder stringBuilder = new StringBuilder(result.length);
            for (int i = 0; i<result.length; i++) {
                stringBuilder.append(String.format("%02X ", result[i]));
            }
            Log.d(TAG, "onFrameIPLastBytes: " + stringBuilder.toString());
        }

        @Override
        public void onError(int code, String msg, int handler) {
            Toast.makeText(applicationContext, "code: " + code + ", msg: " + msg, Toast.LENGTH_SHORT).show();
        }
    };
}
