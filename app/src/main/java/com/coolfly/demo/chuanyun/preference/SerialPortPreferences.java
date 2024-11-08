package com.coolfly.demo.chuanyun.preference;



import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.serialport.SerialPortFinder;

import com.coolfly.demo.R;
import com.fly.station.chuanyun.SensorDevice;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2022/4/18 18:18
 */
public class SerialPortPreferences extends PreferenceActivity {

    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.serial_port_preferences);

        // Devices
        final ListPreference devices = (ListPreference) findPreference("DEVICE");
        String[] entries = mSerialPortFinder.getAllDevices();
        String[] entryValues = mSerialPortFinder.getAllDevicesPath();
        devices.setEntries(entries);
        devices.setEntryValues(entryValues);
        devices.setSummary(devices.getValue());
        devices.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);

                SensorDevice.setDevicePath((String) newValue);
                return true;
            }
        });

        // Baud rates
        final ListPreference baudrates = (ListPreference) findPreference("BAUDRATE");
        baudrates.setSummary(baudrates.getValue());
        baudrates.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                preference.setSummary((String) newValue);

                SensorDevice.setBaudRate((String) newValue);
                return true;
            }
        });
    }
}