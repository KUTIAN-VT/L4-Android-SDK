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

    private fun showConnStatView(activity: Activity): Boolean {
        if (floatingView != null) {
            return true
        }
        if (!OverlayUtils.checkFloatingWindowPermission(activity)) {
            OverlayUtils.requestFloatingWindowPermission(activity)
            return false
        }
        rootView = Debug8030View(activity)

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

    private fun hideConnStatView() {
        if (floatingView != null) {
            floatingView!!.hide()
            floatingView = null
            rootView = null
        }
    }

    fun show(isShow: Boolean, activity: Activity) {
        if (isShow) {
            showConnStatView(activity)
        } else {
            hideConnStatView()
        }
    }

    fun isShowing(): Boolean {
        return floatingView != null
    }
}