# Fly Android SDK Integration Guide

## Project Overview

This is an Android sample project integrating multiple Fly SDKs, supporting device control, video streaming and decoding, wireless transmission, and various other functions.

## Project Structure

```
coollibrary/
├── aoa/
│   └── aoalibrary.aar          # USB Function Library
├── cf/
│   └── cflibrary.aar           # Core Function Library
├── ff/
│   └── fflibrary.aar           # FFmpeg Video Codec Library
├── media/
│   └── medialibrary.aar        # Multimedia Processing Library
└── log/
    └── loglibrary.aar          # Logging Library (Optional)
```

## Environment Requirements

- **Android Studio**: Arctic Fox (2020.3.1) or higher
- **minSdkVersion**: 24
- **targetSdkVersion**: 34
- **compileSdkVersion**: 36
- **Java Version**: 17

## Dependency Configuration

### 1. Project-level Configuration (settings.gradle)

Add the following configuration to the `settings.gradle` file in the project root directory:

```groovy
// Include local AAR libraries
include ':fflibrary'
include ':cflibrary'
include ':aoalibrary'
include ':medialibrary'
include ':loglibrary'

// Configure library paths
project(':fflibrary').projectDir = new File('./coollibrary/ff')
project(':cflibrary').projectDir = new File('./coollibrary/cf')
project(':aoalibrary').projectDir = new File('./coollibrary/aoa')
project(':medialibrary').projectDir = new File('./coollibrary/media')
project(':loglibrary').projectDir = new File('./coollibrary/log')
```

### 2. Application-level Configuration (app/build.gradle)

Add the following dependencies to the `app/build.gradle` file:

```groovy
dependencies {
    // SDK Dependencies
    implementation project(path: ':fflibrary')
    implementation project(path: ':cflibrary')
    implementation project(path: ':aoalibrary')
    implementation project(path: ':medialibrary')
    // Optional
    implementation project(path: ':loglibrary')

    // Android Basic Dependencies
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'

    // Network and Communication
    implementation 'com.alibaba:fastjson:1.2.83'
    implementation 'com.jcraft:jsch:0.1.55'
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'com.github.hannesa2:paho.mqtt.android:4.4.1'

    // Serial Communication
    implementation 'com.licheedev:android-serialport:2.1.5'

    // Logging System (Required by loglibrary SDK)
    implementation 'org.slf4j:slf4j-api:1.7.30'
    implementation 'com.github.tony19:logback-android:1.1.1-12'
}
```

## Permission Configuration

Add the necessary permissions to `AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.coolfly.demo">

    <!-- Network Permissions (Used by MQTT/UDP, etc.)-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- USB Permissions -->
    <uses-permission android:name="android.permission.USB_PERMISSION" />
    <uses-feature android:name="android.hardware.usb.host" android:required="true" />

    <!-- Bluetooth Permissions (Used by BLUETOOTH SPP/Joystick)-->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <!-- USB Device Filter -->
        <activity android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />
        </activity>

    </application>

</manifest>
```

## API

https://app.cecooleye.cn/doc/sdk/API-DOC.html
