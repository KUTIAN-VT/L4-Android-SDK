package com.coolfly.demo.entry;

import static com.coolfly.demo.utils.Constants.SP_NAME;
import static com.coolfly.demo.utils.ImageUtils.saveBitmap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.MainApplication;
import com.coolfly.demo.R;
import com.coolfly.demo.VideoMock;
import com.coolfly.demo.chuanyun.ChuanYunActivity;
import com.coolfly.demo.databinding.ActivityP201Binding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.coolfly.demo.utils.Constants;
import com.coolfly.demo.utils.ImageUtils;
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
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.UpgradeHelper;
import com.fly.station.prorocol.bean.ACK;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.Throughput8030;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class P201Activity extends AppCompatActivity {

    private ActivityP201Binding binding;

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
    private UpgradeHelper upgradeHelper;
    private int decodeMode;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityP201Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        usbDeviceHelper = UsbDeviceHelper.getInstance(getApplicationContext());
        usbDeviceHelper.addListener(usbDeviceListener);

        protocolHelper = ProtocolHelper.getInstance();
        protocolHelper.addListener(protocolListener);

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

                    AlertDialog.Builder builder = new AlertDialog.Builder(P201Activity.this);
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

        // Get whether hardware decoding now (default value is true)
        binding.swHwDecode.setChecked(FFJNI.isHwDecode());
        binding.swHwDecode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putBoolean(Constants.PREF_IS_HW_DECODE, isChecked);
                editor.apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(P201Activity.this);
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
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Retrieve USB permission while onResume
        usbDeviceHelper.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        usbDeviceHelper.removeListener(usbDeviceListener);
        protocolHelper.removeListener(protocolListener);
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

    public void onClick(View view) {
        if (view == binding.btnShot) {
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
                        Toast.makeText(P201Activity.this, R.string.record_success, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(P201Activity.this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_GRD);
        } else if (view == binding.btnUpgradeSky) {
            if (usbDeviceHelper.getUsbStatus() != UsbDeviceHelper.USB_CONNECTED || usbDeviceHelper.getDeviceType() != DEVICE_TYPE.TYPE_8020) {
                Toast.makeText(P201Activity.this, "USB not connected", Toast.LENGTH_SHORT).show();
                return;
            }
            getUpgradeFis(REQ_OTA_SKY);
        } else if (view == binding.btnConfig) {
            Intent intent = new Intent(this, ChuanYunActivity.class);
            startActivity(intent);
        }
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
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.width = (int) realWidth;
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            viewToChange.requestLayout();
        } else {
            float realHeight = ((float) (binding.rootView.getWidth())) / aspectRatioNew;
            ViewGroup.LayoutParams layoutParams = viewToChange.getLayoutParams();
            layoutParams.height = (int) realHeight;
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            viewToChange.requestLayout();
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

    @Keep
    private final UsbDeviceListener usbDeviceListener = new UsbDeviceListener() {
        @Override
        public void onNoUsbDevice() {

        }

        @Override
        public void onStartReadData(DEVICE_TYPE deviceType) {

        }

        @Override
        public void onDisconnect(DEVICE_TYPE deviceType) {
            bitRateHelperVideo.stop();
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

        }
    };

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(com.fly.station.prorocol.DEVICE_TYPE deviceType) {

        }

        @Override
        public void onReadCmd(BaseFlyPacket packet, com.fly.station.prorocol.DEVICE_TYPE deviceType, boolean isRemote) {
            if (packet instanceof ACK) {
                if (upgradeHelper != null) {
                    upgradeHelper.onAck((ACK) packet);
                }
            }
        }

        @Override
        public int onWrite(byte[] data) {
            return 0;
        }

        @Override
        public void onPairOperated(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot, boolean isStart) {
            // Now only for 8030, pair manually time out
        }

        @Override
        public void onPairTimeOut(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually time out
        }

        @Override
        public void onPairSuccess(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, pair manually success
        }

        @Override
        public void onLinked(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link ready automatically
        }

        @Override
        public void onLinkLost(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot) {
            // Now only for 8030, link lost automatically
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

        @Override
        public void onSlotMac(com.fly.station.prorocol.DEVICE_TYPE deviceType, int slot, String mac) {
            // Now only for 8030
        }

        @Override
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // AR8030 throughput data received
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onDebugMessage(com.fly.station.prorocol.DEVICE_TYPE deviceType, String s) {
            // Now only for 8030
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
                            upgradeHelper.setListener(upgradeListener8020);
                            upgradeHelper.startUpgradeApp(requestCode == REQ_OTA_SKY);
                            binding.btnUpgradeGnd.setEnabled(false);
                            binding.btnUpgradeSky.setEnabled(false);
                            break;
                    }
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private final UpgradeHelper.UpgradeListener8020 upgradeListener8020 = new UpgradeHelper.UpgradeListener8020() {
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
        }

        @Override
        public void onFail(String errMsg) {
            binding.tvUpdateProcess.setText(R.string.ota_fail + "\n" + errMsg);

            binding.btnUpgradeGnd.setEnabled(true);
            binding.btnUpgradeSky.setEnabled(true);
        }

        @Override
        public void onWrite(byte[] data) {
            if (usbDeviceHelper.getUsbStatus() == UsbDeviceHelper.USB_CONNECTED) {
                usbDeviceHelper.writeData(data);
            }
        }
    };
}
