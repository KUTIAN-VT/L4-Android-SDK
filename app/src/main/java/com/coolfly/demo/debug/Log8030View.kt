package com.coolfly.demo.debug

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.ScrollView
import com.coolfly.demo.databinding.ViewLog8030Binding
import com.fly.station.prorocol.DEVICE_TYPE
import com.fly.station.prorocol.ProtocolHelper
import com.fly.station.prorocol.ProtocolListener
import com.fly.station.prorocol.RADIO_TYPE
import com.fly.station.prorocol.bean.BaseFlyPacket
import com.fly.station.prorocol.bean.Throughput8030
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @Description: 8030 调试日志显示视图
 * @Author: zongheng.wu
 * @Date: 2025/11/15
 */
class Log8030View : LinearLayout {

    private lateinit var protocolHelper: ProtocolHelper
    private lateinit var binding: ViewLog8030Binding

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context)
    }

    // 创建作用域
    private val viewJob = SupervisorJob()
    private val viewScope = CoroutineScope(Dispatchers.Main + viewJob)

    private var collectDebugJob: Job? = null
    private var autoScroll: Boolean = true
    private val logBuffer = StringBuilder()

    private fun init(context: Context) {
        binding = ViewLog8030Binding.inflate((context as Activity).layoutInflater, this, true)
        protocolHelper = ProtocolHelper.getInstance()

        binding.tvPause.setOnClickListener {
            pauseAutoScroll()
        }

        binding.tvResume.setOnClickListener {
            resumeAutoScroll()
        }
    }

    private fun addLogMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "$timestamp: $message\n"

        // 添加到缓冲区
        logBuffer.append(logLine)

        // 限制缓冲区大小，避免内存溢出
        if (logBuffer.length > 50000) { // 约5000行日志
            val lines = logBuffer.split("\n")
            if (lines.size > 1000) { // 保留最近1000行
                logBuffer.clear()
                logBuffer.append(lines.takeLast(1000).joinToString("\n"))
            }
        }

        // 更新UI
        viewScope.launch {
            binding.tvLogContent.text = logBuffer.toString()
            if (autoScroll) {
                binding.tvLogContent.post {
                    val scrollView = binding.tvLogContent.parent as? ScrollView
                    scrollView?.fullScroll(FOCUS_DOWN)
                }
            }
        }
    }

    private fun pauseAutoScroll() {
        autoScroll = false
        binding.tvPause.visibility = GONE
        binding.tvResume.visibility = VISIBLE
    }

    private fun resumeAutoScroll() {
        autoScroll = true
        binding.tvPause.visibility = VISIBLE
        binding.tvResume.visibility = GONE
        // 恢复时立即滚动到最新
        binding.tvLogContent.post {
            val scrollView = binding.tvLogContent.parent as? ScrollView
            scrollView?.fullScroll(FOCUS_DOWN)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        protocolHelper.addListener(protocolListener)
        // 开启调试日志
        protocolHelper.ar8030StartDebug()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        protocolHelper.removeListener(protocolListener)
        // 关闭调试日志
        protocolHelper.ar8030StopDebug()
        // 取消协程
        collectDebugJob?.cancel()
        collectDebugJob = null
        viewJob.cancel()
    }

    private val protocolListener: ProtocolListener = object : ProtocolListener {
        override fun onReady(p0: DEVICE_TYPE?) {

        }

        override fun onReadCmd(
            packet: BaseFlyPacket?,
            p1: DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onWrite(p0: ByteArray?): Int {
            return 0
        }

        override fun onPairOperated(
            deviceType: DEVICE_TYPE?,
            slot: Int,
            isStart: Boolean
        ) {

        }

        override fun onPairTimeOut(p0: DEVICE_TYPE?, p1: Int) {

        }

        override fun onPairSuccess(p0: DEVICE_TYPE?, p1: Int) {

        }

        override fun onLinked(p0: DEVICE_TYPE?, p1: Int) {

        }

        override fun onLinkLost(p0: DEVICE_TYPE?, p1: Int) {

        }

        override fun onConfigJson(
            p0: String?,
            p1: DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onSetConfigJson(
            p0: Boolean,
            p1: DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onResetConfigJson(
            p0: Boolean,
            p1: DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onSlotMac(
            p0: DEVICE_TYPE?,
            p1: Int,
            p2: String?
        ) {

        }

        override fun onThroughput(
            deviceType: DEVICE_TYPE?,
            throughput: Throughput8030?,
            isRemote: Boolean
        ) {

        }

        override fun onSetRadio(
            deviceType: DEVICE_TYPE?,
            radioType: RADIO_TYPE?,
            isSuccess: Boolean,
            errcode: Int,
            errMessage: String?,
            isRemote: Boolean
        ) {

        }

        override fun onDebugMessage(p0: DEVICE_TYPE?, p1: String?) {
            p1?.let{addLogMessage(it)}
        }

    }
}
