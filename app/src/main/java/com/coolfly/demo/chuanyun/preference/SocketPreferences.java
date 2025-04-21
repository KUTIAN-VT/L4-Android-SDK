package com.coolfly.demo.chuanyun.preference;


import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.coolfly.demo.MainApplication;
import com.coolfly.demo.R;
import com.fly.station.chuanyun.SensorDevice;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2022/4/18 18:18
 */
public class SocketPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.socket_preferences);

        // Devices
        final EditTextPreference socketIp = (EditTextPreference) findPreference("SOCKET_IP");
        socketIp.setSummary(socketIp.getText());
        socketIp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                SensorDevice.getInstance(MainApplication.applicationContext).offLine();

                SensorDevice.setIp((String) newValue);
                com.coolfly.demo.preference.PreferenceActivity.preferenceObject.p301_socket_ip = (String) newValue;
                com.coolfly.demo.preference.PreferenceActivity.savePreference();
                return true;
            }
        });

        // Baud rates
        final EditTextPreference socketPort = (EditTextPreference) findPreference("SOCKET_PORT");
        socketPort.setSummary(socketPort.getText());
        socketPort.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);
                SensorDevice.getInstance(MainApplication.applicationContext).offLine();

                SensorDevice.setPort(Integer.parseInt((String) newValue));
                com.coolfly.demo.preference.PreferenceActivity.preferenceObject.p301_socket_port = Integer.parseInt((String) newValue);
                com.coolfly.demo.preference.PreferenceActivity.savePreference();
                return true;
            }
        });
    }
}