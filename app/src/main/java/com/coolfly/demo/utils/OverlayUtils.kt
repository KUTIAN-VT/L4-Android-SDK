package com.coolfly.demo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings


object OverlayUtils {
    /**
     * @Description:
     * @Date: 2025/1/14 17:32
     */
    /**
     * 检查是否有悬浮窗权限
     */
    fun checkFloatingWindowPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context)
        }
        return true
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestFloatingWindowPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.setData(Uri.parse("package:" + activity.packageName))
            try {
                activity.startActivity(
                    intent,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                // 跳转到应用设置页面
                intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.setData(Uri.parse("package:" + activity.packageName))
                activity.startActivity(
                    intent,
                )
            }
        }
    }
}
