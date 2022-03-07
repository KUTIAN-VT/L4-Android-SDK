package com.coolfly.demo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.coolfly.demo.utils.Constants;
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

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isHwDecode = sharedPreferences.getBoolean(Constants.PREF_IS_HW_DECODE, true);

        /*
         * Set whether to hardware decode (default value is true). This method needs to be called before SurfaceView is created to take effect.
         * @param isHw
         * @return Whether the setting is successful
         */
        FFJNI.setHwDecode(isHwDecode);
    }
}
