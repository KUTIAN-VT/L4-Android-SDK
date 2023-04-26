package com.coolfly.demo;

import com.wuadam.medialibrary.MediaHelper;

import java.io.InputStream;

/**
 * @Description: 模拟视频流
 * @Author: zongheng.wu
 * @Date: 2023/3/1 13:38
 */
public class VideoMock {
    private MediaHelper mediaHelper;

    public VideoMock(MediaHelper mediaHelper) {
        this.mediaHelper = mediaHelper;
    }

    public void destroy() {
        isMockFinished = true;
    }

    private boolean isMockFinished = true;

    /**
     * change between avatar and butterfly
     */
    private static final boolean isMockAvatar = true;

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
                        InputStream inputStream = MainApplication.applicationContext.getAssets().open(isMockAvatar? "avatar-1920-1080-30fps.h264": "butterfly-240-320-25fps.h264");
                        byte[] data = new byte[1024];
                        int len = 0;
                        while ((len = inputStream.read(data)) != -1) {
                            if (len > 0) {
                                byte[] buffer = new byte[len];
                                System.arraycopy(data, 0, buffer, 0, len);
                                mediaHelper.offerData(buffer, buffer.length);
                            }
                            try {
                                Thread.sleep(isMockAvatar? 1: 3);
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
