package com.coolfly.demo;

import com.fly.medialibrary.MediaHelper;

import java.io.InputStream;

/**
 * @Description: Mock video source
 * @Author: zongheng.wu
 * @Date: 2023/3/1 13:38
 */
public class VideoMock {
    private static final int H264_BUTTERFLY = 1, H264_AVATAR = 2, MJPEG = 3, HEVC_IPCAM = 4;
    private final MediaHelper mediaHelper;

    public VideoMock(MediaHelper mediaHelper) {
        this.mediaHelper = mediaHelper;
    }

    public void destroy() {
        isMockFinished = true;
    }

    private boolean isMockFinished = true;

    /**
     * change mock source
     */
    private static final int MOCK_SOURCE = H264_AVATAR;

    private String getMockSourceFileName() {
        switch (MOCK_SOURCE) {
            case H264_BUTTERFLY:
                return "butterfly-240-320-25fps.h264";
            case H264_AVATAR:
                return "avatar-1920-1080-30fps.h264";
            case MJPEG:
                return "sample_960x540.mjpeg";
            case HEVC_IPCAM:
                return "ipcam-1920-1080-60fps.hevc";
            default:
                return "butterfly-240-320-25fps.h264";
        }
    }

    private int getMockSourceSleepTime() {
        switch (MOCK_SOURCE) {
            case H264_BUTTERFLY:
                return 3;
            case H264_AVATAR:
                return 1;
            case MJPEG:
                return 3;
            case HEVC_IPCAM:
                return 1;
            default:
                return 3;
        }
    }

    public void start() {
        isMockFinished = false;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                while (!isMockFinished) {
                    try {
                        InputStream inputStream = MainApplication.applicationContext.getAssets().open(getMockSourceFileName());
                        byte[] data = new byte[1024];
                        int len = 0;
                        while ((len = inputStream.read(data)) != -1) {
                            if (len > 0) {
                                byte[] buffer = new byte[len];
                                System.arraycopy(data, 0, buffer, 0, len);
                                mediaHelper.offerData(buffer, buffer.length);
                            }
                            try {
                                Thread.sleep(getMockSourceSleepTime());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (isMockFinished) {
                                return;
                            }
                        }
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        }).start();
    }
}
