package com.coolfly.demo.debug

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import com.coolfly.demo.MainApplication
import com.coolfly.demo.utils.OverlayUtils
import com.wuadam.floatingview.FloatingView
import com.wuadam.floatingview.FloatingViewConfig

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2024/12/5 21:37
 */
@SuppressLint("StaticFieldLeak")
object DebugViewManager {
    private var floatingView: FloatingView? = null
    private var rootView: View? = null

    // Log8030View related variables
    private var logFloatingView: FloatingView? = null
    private var logRootView: View? = null

    fun showConnStatView(activity: Activity): Boolean {
        if (floatingView != null) {
            return true
        }
        if (!OverlayUtils.checkFloatingWindowPermission(activity)) {
            OverlayUtils.requestFloatingWindowPermission(activity)
            return false
        }
        rootView = Debug8030ViewV2(activity)

        val config = FloatingViewConfig.Builder()
            .setGravity(FloatingViewConfig.GRAVITY.CENTER)
            .build()
        floatingView = FloatingView(
            MainApplication.applicationContext,
            rootView,
            config
        )
        floatingView!!.showOverlaySystem(activity)
        return true
    }

    fun hideConnStatView() {
        if (floatingView != null) {
            floatingView!!.hide()
            floatingView = null
            rootView = null
        }
    }

    fun isShowingConnStatView(): Boolean {
        return floatingView != null
    }

    // Functions for Log8030View
    fun showLogView(activity: Activity): Boolean {
        if (logFloatingView != null) {
            return true
        }
        if (!OverlayUtils.checkFloatingWindowPermission(activity)) {
            OverlayUtils.requestFloatingWindowPermission(activity)
            return false
        }
        logRootView = Log8030View(activity)

        val config = FloatingViewConfig.Builder()
            .setGravity(FloatingViewConfig.GRAVITY.CENTER)
            .build()
        logFloatingView = FloatingView(
            MainApplication.applicationContext,
            logRootView,
            config
        )
        logFloatingView!!.showOverlaySystem(activity)
        return true
    }

    fun hideLogView() {
        if (logFloatingView != null) {
            logFloatingView!!.hide()
            logFloatingView = null
            logRootView = null
        }
    }

    fun isShowingLogView(): Boolean {
        return logFloatingView != null
    }
}