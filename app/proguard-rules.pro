# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#aoa
-keep class com.fly.aoalibrary.** { *; }
-keep class aoa.** { *; }

#cf
-keep class com.fly.station.** { *; }
-keep class cf.** { *; }

#ff
-keep class com.fly.fflibrary.** { *; }
-keep class ff.** { *; }

#media
-keep class com.fly.medialibrary.** { *; }
-keep class media.** { *; }


# Fastjson
-dontwarn com.alibaba.fastjson.**
-keep class com.alibaba.fastjson.** { *;}

#serialport
-keep class android.serialport.** { *; }

#jsch
-keep class com.jcraft.jsch.** { *; }

#log
-keep class ch.qos.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes *Annotation*
-dontwarn ch.qos.logback.core.net.*

#mqtt
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.*
-keep class info.mqtt.android.service.** { *; }
-dontwarn info.mqtt.android.service.*