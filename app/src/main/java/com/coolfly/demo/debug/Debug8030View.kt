package com.coolfly.demo.debug

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.LinearLayout
import com.coolfly.demo.MainApplication
import com.coolfly.demo.databinding.ViewDebug8030Binding
import com.fly.aoalibrary.DEVICE_TYPE
import com.fly.aoalibrary.host.UsbDeviceHelper
import com.fly.aoalibrary.host.UsbDeviceListener
import com.fly.station.prorocol.ProtocolHelper
import com.fly.station.prorocol.ProtocolListener
import com.fly.station.prorocol.RADIO_TYPE
import com.fly.station.prorocol.bean.BaseFlyPacket
import com.fly.station.prorocol.bean.ChanInfo8030
import com.fly.station.prorocol.bean.RcStatus8030
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2023/7/18 17:44
 */
class Debug8030View : LinearLayout {

    private lateinit var usbDeviceHelper: UsbDeviceHelper
    private lateinit var protocolHelper: ProtocolHelper
    private lateinit var binding: ViewDebug8030Binding

    private lateinit var handler: Handler

    constructor(context: Context) : super(context) {
        init(context)
    }

    // 创建作用域
    private val viewJob = SupervisorJob()
    private val viewScope = CoroutineScope(Dispatchers.Main + viewJob)

    private var recordJob: Job? = null

    private var status8030: RcStatus8030? = null
    private var chanInfo8030: ChanInfo8030? = null
    private var pairStatus: Boolean = false
    private var flag: Unit? = null


    private fun init(context: Context) {
        binding = ViewDebug8030Binding.inflate((context as Activity).layoutInflater, this, true)
        usbDeviceHelper = UsbDeviceHelper.getInstance(MainApplication.applicationContext)
        protocolHelper = ProtocolHelper.getInstance()
        handler = Handler(Looper.getMainLooper())

        binding.tvUsb.text = "${if (usbDeviceHelper.usbStatus == UsbDeviceHelper.USB_CONNECTED) "true" else "false"}"

        binding.tvRecordStart.setOnClickListener {
            startRecord()
        }

        binding.tvRecordStop.setOnClickListener {
            stopRecord()
            binding.tvRecordStart.visibility = VISIBLE
            binding.tvRecordStop.visibility = GONE
        }

        binding.tvFlag.setOnClickListener {
            addFlag()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun startRecord() {
        recordJob = viewScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 在Android文件夹下的应用目录下创建以时间戳命名的csv文件,若无权限则跳过
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "$timeStamp.csv"
                    val contentResolver = context.contentResolver

                    // 构建保存文件的目录
                    val relativePath = Environment.DIRECTORY_DOCUMENTS + File.separator + "AR8030"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        } else {
                            // 在 Android 10 以下版本，需要指定绝对路径
                            val directory = Environment.getExternalStoragePublicDirectory(relativePath)
                            if (!directory.exists() && !directory.mkdirs()) {
                                // 创建目录失败
                                return@withContext
                            }
                            val absoluteFilePath = File(directory, fileName).absolutePath
                            put(MediaStore.MediaColumns.DATA, absoluteFilePath)
                        }
                    }

                    val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    if (uri == null) {
                        return@withContext
                    }
                    val outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream == null) {
                        return@withContext
                    }

                    withContext(Dispatchers.Main) {
                        binding.tvRecordStart.visibility = GONE
                        binding.tvRecordStop.visibility = VISIBLE
                    }

                    // csv文件的列为Time,Self SNR,Self LDPC TLV Err Ratio,Self LDPC Num Err Ratio,Self Gain A,Self Gain B,Self TX MCS,Self TX Chan,Self TX Power,Self LNA Inner Bypass,Self LNA Fem Bypass,Peer SNR,Peer LDPC TLV Err Ratio,Peer LDPC Num Err Ratio,Peer Gain A,Peer Gain B,Peer TX MCS,Peer TX Chan,Peer TX Power,Peer LNA Inner Bypass,Peer LNA Fem Bypass,Chan Num,Auto Mode,ACS Chan,Work Chan,Chan 0 Freq,Chan 0 Power,Chan 1 Freq,Chan 1 Power,Chan 2 Freq,Chan 2 Power,Chan 3 Freq,Chan 3 Power,Chan 4 Freq,Chan 4 Power,Chan 5 Freq,Chan 5 Power,Chan 6 Freq,Chan 6 Power,Chan 7 Freq,Chan 7 Power,Chan 8 Freq,Chan 8 Power,Chan 9 Freq,Chan 9 Power
                    // 值示例：2025-03-12 11:25:26,19,0,0,23,32,10,11,15,0,0,19,0,0,40,28,2,3,15,0,0,15,0,3,3,2411000,-68,2422000,-68,2433000,-76,2444000,-71,2455000,-77,2466000,-77,2477000,-76,5101000,-102,5202000,-78,5303000,-88,5404000,-103,5505000,-104,5606000,-102,5707000,-104,5808000,-103

                    outputStream.write("Time,Pair,Self SNR,Self LDPC TLV Err Ratio,Self LDPC Num Err Ratio,Self Gain A,Self Gain B,Self TX MCS,Self TX Chan,Self TX Freq,Self Chan Power,Self TX Power,Peer SNR,Peer LDPC TLV Err Ratio,Peer LDPC Num Err Ratio,Peer Gain A,Peer Gain B,Peer TX MCS,Peer TX Chan,Peer TX Freq,Peer Chan Power,Peer TX Power,Chan Num,Auto Mode,ACS Chan,Work Chan,Chan 0 Freq,Chan 0 Power,Chan 1 Freq,Chan 1 Power,Chan 2 Freq,Chan 2 Power,Chan 3 Freq,Chan 3 Power,Chan 4 Freq,Chan 4 Power,Chan 5 Freq,Chan 5 Power,Chan 6 Freq,Chan 6 Power,Chan 7 Freq,Chan 7 Power,Chan 8 Freq,Chan 8 Power,Chan 9 Freq,Chan 9 Power,Chan 10 Freq,Chan 10 Power,Chan 11 Freq,Chan 11 Power,Chan 12 Freq,Chan 12 Power,Chan 13 Freq,Chan 13 Power,Chan 14 Freq,Chan 14 Power\n".toByteArray())

                    while (isActive) {
                        delay(1000)
                        if (status8030 == null || chanInfo8030 == null) {
                            continue
                        }
                        var line = ""
                        if (flag != null) {
                            flag = null
                            line = "Flag\n"
                        }
                        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        line += "$time,${pairStatus}," +
                                "${status8030!!.localSnrValue},${status8030!!.localLdpcTlvErrRatio}," +
                                "${status8030!!.localLdpcNumErrRatio},${status8030!!.localGainA},${status8030!!.localGainB}," +
                                "${status8030!!.localTxMcs},${status8030!!.localTxChan}," +
                                "${status8030!!.localTxFreqKhz}," +
                                "${if (chanInfo8030!!.power.size > status8030!!.localTxChan) chanInfo8030!!.power[status8030!!.localTxChan].toString() else ""}," +
                                "${status8030!!.localTxPower}," +
//                                "${status8030!!.localLnaInnerBypass},${status8030!!.localLnaFemBypass}," +
                                "${status8030!!.peerSnrValue},${status8030!!.peerLdpcTlvErrRatio}," +
                                "${status8030!!.peerLdpcNumErrRatio},${status8030!!.peerGainA},${status8030!!.peerGainB}," +
                                "${status8030!!.peerTxMcs},${status8030!!.peerTxChan}," +
                                "${status8030!!.peerTxFreqKhz}," +
                                "${if (chanInfo8030!!.power.size > status8030!!.peerTxChan) chanInfo8030!!.power[status8030!!.peerTxChan].toString() else ""}," +
                                "${status8030!!.peerTxPower}," +
//                                "${status8030!!.peerLnaInnerBypass},${status8030!!.peerLnaFemBypass}," +

                                "${chanInfo8030!!.chanNum},${chanInfo8030!!.autoMode},${chanInfo8030!!.acsChan},${chanInfo8030!!.workChan}"

                        for (i in 0..<chanInfo8030!!.freq.size.coerceAtMost(chanInfo8030!!.chanNum)) {
                            line += ",${chanInfo8030!!.freq[i] / 1000},${if (chanInfo8030!!.power.size <= i) "" else chanInfo8030!!.power[i]}"
                        }
                        line += "\n"
                        outputStream.write(line.toByteArray())
                    }
                    outputStream.flush()
                    outputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.tvRecordStart.visibility = VISIBLE
                    binding.tvRecordStop.visibility = GONE
                }
                stopRecord()
            }
        }
    }

    private fun addFlag() {
        flag = Unit
    }

    private fun stopRecord() {
        flag = null
        recordJob?.cancel()
        recordJob = null
    }

    private var read8030ChannelInfoTimer: Timer? = null

    private fun startRead8030ChannelInfoTimer() {
        if (read8030ChannelInfoTimer == null) {
            read8030ChannelInfoTimer = Timer()
            val task: TimerTask = object : TimerTask() {
                override fun run() {
                    protocolHelper.ar8030GetChannelInfo(false)
                }
            }
            read8030ChannelInfoTimer!!.schedule(task, 2000, 2000)
        }
    }

    private fun stopRead8030ChannelInfoTimer() {
        read8030ChannelInfoTimer?.let {
            it.cancel()
            it.purge()
            read8030ChannelInfoTimer = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        usbDeviceHelper.addListener(usbDeviceListener)
        protocolHelper.addListener(protocolListener)
        startRead8030ChannelInfoTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        usbDeviceHelper.removeListener(usbDeviceListener)
        protocolHelper.removeListener(protocolListener)
        stopRecord()
        viewJob.cancel()
        stopRead8030ChannelInfoTimer()
    }

    private fun updateData(status: RcStatus8030) {
        handler.post {
            binding.tvLocalSnr.text = String.format("%.1f", status.localSnrValue)
            binding.tvLocalLdpcTlvErr.text = status.localLdpcTlvErrRatio.toString()
            binding.tvLocalLdpcNumErr.text = status.localLdpcNumErrRatio.toString()
            binding.tvLocalGainA.text = status.localGainA.toString()
            binding.tvLocalGainB.text = status.localGainB.toString()
            binding.tvLocalTxMcs.text = status.localTxMcs.toString()
            binding.tvLocalTxChan.text = status.localTxChan.toString()
            binding.tvLocalTxPower.text = status.localTxPower.toString()
            binding.tvLocalTxFreq.text = status.localTxFreqKhz.div(1000).toString()
            binding.tvLocalLnaInnerBypass.text = status.localLnaInnerBypass.toString()
            binding.tvLocalLnaFemBypass.text = status.localLnaFemBypass.toString()
            binding.tvLocalRx1Tx.text = status.localRf1Tx.toString()
            binding.tvLocalLfsLowSnr.text = status.localLfsLowBandChanSnr.toString()
            binding.tvLocalLfsLowGainA.text = status.localLfsLowBandGainA.toString()
            binding.tvLocalLfsLowGainB.text = status.localLfsLowBandGainB.toString()
            binding.tvLocalLfsHighSnr.text = status.localLfsHighBandChanSnr.toString()
            binding.tvLocalLfsHighGainA.text = status.localLfsHighBandGainA.toString()
            binding.tvLocalLfsHighGainB.text = status.localLfsHighBandGainB.toString()

            binding.tvPeerSnr.text = status.peerSnrValue.toString()
            binding.tvPeerLdpcTlvErr.text = status.peerLdpcTlvErrRatio.toString()
            binding.tvPeerLdpcNumErr.text = status.peerLdpcNumErrRatio.toString()
            binding.tvPeerGainA.text = status.peerGainA.toString()
            binding.tvPeerGainB.text = status.peerGainB.toString()
            binding.tvPeerTxMcs.text = status.peerTxMcs.toString()
            binding.tvPeerTxChan.text = status.peerTxChan.toString()
            binding.tvPeerTxPower.text = status.peerTxPower.toString()
            binding.tvPeerTxFreq.text = status.peerTxFreqKhz.div(1000).toString()
            binding.tvPeerLnaInnerBypass.text = status.peerLnaInnerBypass.toString()
            binding.tvPeerLnaFemBypass.text = status.peerLnaFemBypass.toString()
            binding.tvPeerRx1Tx.text = status.peerRf1Tx.toString()
            binding.tvPeerLfsLowSnr.text = status.peerLfsLowBandChanSnr.toString()
            binding.tvPeerLfsLowGainA.text = status.peerLfsLowBandGainA.toString()
            binding.tvPeerLfsLowGainB.text = status.peerLfsLowBandGainB.toString()
            binding.tvPeerLfsHighSnr.text = status.peerLfsHighBandChanSnr.toString()
            binding.tvPeerLfsHighGainA.text = status.peerLfsHighBandGainA.toString()
            binding.tvPeerLfsHighGainB.text = status.peerLfsHighBandGainB.toString()

            chanInfo8030?.let {
                binding.tvTxChanPower.text = if (it.power.size > status.localTxChan) it.power[status.localTxChan].toString() else ""
                binding.tvRxChanPower.text = if (it.power.size > status.peerTxChan) it.power[status.peerTxChan].toString() else ""
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateData(chanInfo8030: ChanInfo8030) {
        handler.post {
            binding.tvChanNum.text = chanInfo8030.chanNum.toString()
            binding.tvAutoMode.text = chanInfo8030.autoMode.toString()
            binding.tvAcsChan.text = chanInfo8030.acsChan.toString()
            binding.tvWorkchan.text = chanInfo8030.workChan.toString()

            var freqAndPower = ""
            for (i in 0..<chanInfo8030.freq.size.coerceAtMost(chanInfo8030.chanNum)) {
                freqAndPower += "${chanInfo8030.freq[i] / 1000} : ${if (chanInfo8030.power.size <= i) "" else chanInfo8030.power[i]}\n"
            }
            binding.tvFreqAndPower.text = freqAndPower
        }
    }

    private fun updatePair(pairStatus: Boolean) {
        handler.post {
            binding.tvPair.text = pairStatus.toString()
        }
    }

    private val usbDeviceListener: UsbDeviceListener = object : UsbDeviceListener {
        override fun onNoUsbDevice() {
            binding.tvUsb.text = "false"
        }

        override fun onStartReadData(deviceType: DEVICE_TYPE?) {
            binding.tvUsb.text = deviceType.toString()
        }

        override fun onDisconnect(deviceType: DEVICE_TYPE?) {
            binding.tvUsb.text = "${deviceType.toString()} false"
        }

        override fun onVideoData(data: ByteArray?, length: Int, deviceType: DEVICE_TYPE?) {
        }

        override fun onAudioData(data: ByteArray?, length: Int) {
        }

        override fun onCtrlData(data: ByteArray?, length: Int, deviceType: DEVICE_TYPE?) {
        }
    }

    private val protocolListener: ProtocolListener = object : ProtocolListener {
        override fun onReadCmd(
            packet: BaseFlyPacket?,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {
            when (packet) {
                is RcStatus8030 -> {
                    // AR8030 status
                    status8030 = packet
                    updateData(packet)
                }
                is ChanInfo8030 -> {
                    // AR8030 channel info
                    chanInfo8030 = packet
                    updateData(packet)
                }
            }
        }

        override fun onWrite(p0: ByteArray?) {

        }

        override fun onPairTimeOut(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = false
            updatePair(pairStatus)
        }

        override fun onPairSuccess(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = true
            updatePair(pairStatus)
        }

        override fun onLinked(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = true
            updatePair(pairStatus)
        }

        override fun onLinkLost(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = false
            updatePair(pairStatus)
        }

        override fun onConfigJson(
            p0: String?,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onSetConfigJson(
            p0: Boolean,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onResetConfigJson(
            p0: Boolean,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {

        }

        override fun onSlotMac(
            p0: com.fly.station.prorocol.DEVICE_TYPE?,
            p1: Int,
            p2: String?
        ) {

        }

        override fun onSetRadio(
            p0: com.fly.station.prorocol.DEVICE_TYPE?,
            p1: RADIO_TYPE?,
            p2: Boolean
        ) {

        }

    }
}
