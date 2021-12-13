package com.coolfly.demo;

import android.app.Application;
import android.content.Context;

import com.wuadam.fflibrary.FFJNI;
import com.wuadam.medialibrary.MediaHelper;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2021/12/6 5:39 下午
 */
public class MainApplication extends Application {

    public static Context applicationContext;
    @Override
    public void onCreate() {
        super.onCreate();
        applicationContext = this;
        MediaHelper.init(this);
        FFJNI.init();
    }
}
