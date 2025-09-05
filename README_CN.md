# Fly Android SDK 接入指南

## 项目概述

这是一个集成多个Fly SDK的Android示例工程，支持设备控制、视频拉流解码、无线传输等多种功能。

## 项目结构

```
coollibrary/
├── aoa/
│   └── aoalibrary.aar          # USB 功能库
├── cf/
│   └── cflibrary.aar           # 核心功能库
├── ff/
│   └── fflibrary.aar           # FFmpeg 视频编解码库
├── media/
│   └── medialibrary.aar        # 多媒体处理库
└── log/
    └── loglibrary.aar          # 日志功能库（可选）
```

## 环境要求

- **Android Studio**: Arctic Fox (2020.3.1) 或更高版本
- **minSdkVersion**: 24
- **targetSdkVersion**: 34
- **compileSdkVersion**: 36
- **Java Version**: 17

## 依赖配置

### 1. 项目级配置 (settings.gradle)

在项目根目录的 `settings.gradle` 文件中添加以下配置：

```groovy
// 引入本地 AAR 库
include ':fflibrary'
include ':cflibrary'
include ':aoalibrary'
include ':medialibrary'
include ':loglibrary'

// 配置库路径
project(':fflibrary').projectDir = new File('./coollibrary/ff')
project(':cflibrary').projectDir = new File('./coollibrary/cf')
project(':aoalibrary').projectDir = new File('./coollibrary/aoa')
project(':medialibrary').projectDir = new File('./coollibrary/media')
project(':loglibrary').projectDir = new File('./coollibrary/log')
```

### 2. 应用级配置 (app/build.gradle)

在 `app/build.gradle` 文件中添加以下依赖：

```groovy
dependencies {
    // SDK 依赖
    implementation project(path: ':fflibrary')
    implementation project(path: ':cflibrary')
    implementation project(path: ':aoalibrary')
    implementation project(path: ':medialibrary')
    // 可选
    implementation project(path: ':loglibrary')

    // Android 基础依赖
    implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.1.0'

    // 网络和通信
    implementation 'com.alibaba:fastjson:1.2.83'
    implementation 'com.jcraft:jsch:0.1.55'
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'com.github.hannesa2:paho.mqtt.android:4.4.1'

    // 串口通信
    implementation 'com.licheedev:android-serialport:2.1.5'

    // 日志系统 (loglibrary SDK 需要)
    implementation 'org.slf4j:slf4j-api:1.7.30'
    implementation 'com.github.tony19:logback-android:1.1.1-12'
}
```

## 权限配置

在 `AndroidManifest.xml` 中添加必要的权限：

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.coolfly.demo">

    <!-- 网络权限 MQTT/UDP等使用-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- USB 权限 -->
    <uses-permission android:name="android.permission.USB_PERMISSION" />
    <uses-feature android:name="android.hardware.usb.host" android:required="true" />

    <!-- 蓝牙权限 BLUETOOTH SPP/Joystick使用-->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/AppTheme">

        <!-- USB 设备过滤器 -->
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
