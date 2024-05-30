package com.coolfly.demo.utils;

import android.os.Handler;
import android.os.Looper;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.appcompat.widget.SwitchCompat;

import java.lang.reflect.Field;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2024/5/29 21:55
 */
public class WidgetUtils {
    public static void setSwitchWithoutListener(SwitchCompat switchCompat, boolean isChecked) {
        CompoundButton.OnCheckedChangeListener listener = getOnCheckedChangeListener(switchCompat);
        switchCompat.setOnCheckedChangeListener(null);
        switchCompat.setChecked(isChecked);
        switchCompat.setOnCheckedChangeListener(listener);
    }

    // 反射方法获取当前的OnCheckedChangeListener
    private static CompoundButton.OnCheckedChangeListener getOnCheckedChangeListener(SwitchCompat switchCompat) {
        try {
            Field onCheckedChangeListenerField = CompoundButton.class.getDeclaredField("mOnCheckedChangeListener");
            onCheckedChangeListenerField.setAccessible(true);
            return (CompoundButton.OnCheckedChangeListener) onCheckedChangeListenerField.get(switchCompat);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 设置Spinner选项而不触发回调
    public static void setSelectionWithoutCallback(Spinner spinner, int position) {
        AdapterView.OnItemSelectedListener listener = getOnItemSelectedListener(spinner);
        spinner.setOnItemSelectedListener(null);
        spinner.setSelection(position);
        new Handler(Looper.getMainLooper()).postDelayed(() -> spinner.setOnItemSelectedListener(listener), 100);
    }

    // 反射方法获取当前的OnItemSelectedListener
    private static AdapterView.OnItemSelectedListener getOnItemSelectedListener(Spinner spinner) {
        try {
            Field listenerField = AdapterView.class.getDeclaredField("mOnItemSelectedListener");
            listenerField.setAccessible(true);
            return (AdapterView.OnItemSelectedListener) listenerField.get(spinner);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

}
