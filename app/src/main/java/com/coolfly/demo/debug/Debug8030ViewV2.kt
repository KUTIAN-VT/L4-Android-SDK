package com.coolfly.demo.debug

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.coolfly.demo.MainApplication
import com.coolfly.demo.R
import com.coolfly.demo.databinding.ViewDebug8030V2Binding
import com.fly.aoalibrary.DEVICE_TYPE
import com.fly.aoalibrary.host.UsbDeviceHelper
import com.fly.aoalibrary.host.UsbDeviceListener
import com.fly.station.prorocol.AR8030Role
import com.fly.station.prorocol.ProtocolHelper
import com.fly.station.prorocol.ProtocolHelper.DIR_RX
import com.fly.station.prorocol.ProtocolHelper.DIR_TX
import com.fly.station.prorocol.ProtocolListener
import com.fly.station.prorocol.RADIO_TYPE
import com.fly.station.prorocol.bean.BaseFlyPacket
import com.fly.station.prorocol.bean.ChanInfo8030
import com.fly.station.prorocol.bean.CurPower8030
import com.fly.station.prorocol.bean.DistcResult8030
import com.fly.station.prorocol.bean.GetMcs8030
import com.fly.station.prorocol.bean.PeerQuality8030
import com.fly.station.prorocol.bean.Quality8030
import com.fly.station.prorocol.bean.RcStatus8030
import com.fly.station.prorocol.bean.Throughput8030
import com.fly.station.prorocol.bean.UserQuality8030
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

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2023/7/18 17:44
 */
class Debug8030ViewV2 : LinearLayout {

    private lateinit var usbDeviceHelper: UsbDeviceHelper
    private lateinit var protocolHelper: ProtocolHelper
    private lateinit var binding: ViewDebug8030V2Binding

    private lateinit var handler: Handler

    constructor(context: Context) : super(context) {
        init(context)
    }

    private val viewJob = SupervisorJob()
    private val viewScope = CoroutineScope(Dispatchers.Main + viewJob)

    private var recordJob: Job? = null
    private var pollMultiJob: Job? = null
    private var pollSingleJob: Job? = null
    private var getDistcTimerJob: Job? = null
    private var refreshPairedSlotsJob: Job? = null

    private var status8030: RcStatus8030? = null
    private var chanInfo8030: ChanInfo8030? = null
    private var chanInfo8030Sky: ChanInfo8030? = null
    private var throughput8030: Throughput8030? = null
    private var userQuality8030: UserQuality8030? = null
    private var peerQuality8030: PeerQuality8030? = null
    private var distcResult8030: DistcResult8030? = null
    private var pairStatus: Boolean = false
    private var flag: Unit? = null

    @Volatile
    private var currentIsMultiSlot: Boolean = false

    private val pairedSlots: BooleanArray = BooleanArray(SLOT_COUNT) { false }

    private val slotRows: MutableList<SlotRowViews> = mutableListOf()
    private val slotMcsAp = IntArray(SLOT_COUNT) { Int.MIN_VALUE }
    private val slotMcsDev = IntArray(SLOT_COUNT) { Int.MIN_VALUE }
    private val slotTpAp = arrayOfNulls<Throughput8030>(SLOT_COUNT)
    private val slotPwrAp = IntArray(SLOT_COUNT) { Int.MIN_VALUE }
    private val slotPwrDev = IntArray(SLOT_COUNT) { Int.MIN_VALUE }

    @Volatile
    private var pendingMcsApSlot: Int = -1

    @Volatile
    private var pendingMcsDevSlot: Int = -1

    @Volatile
    private var pendingTpApSlot: Int = -1

    @Volatile
    private var pendingPwrApSlot: Int = -1

    @Volatile
    private var pendingPwrDevSlot: Int = -1

    private fun init(context: Context) {
        binding = ViewDebug8030V2Binding.inflate((context as Activity).layoutInflater, this, true)
        usbDeviceHelper = UsbDeviceHelper.getInstance(MainApplication.applicationContext)
        protocolHelper = ProtocolHelper.getInstance()
        handler = Handler(Looper.getMainLooper())

        binding.tvUsb.text = "${if (usbDeviceHelper.usbStatus == UsbDeviceHelper.USB_CONNECTED) "true" else "false"}"

        binding.tvRecordStart.setOnClickListener { startRecord() }
        binding.tvRecordStop.setOnClickListener {
            stopRecord()
            binding.tvRecordStart.visibility = VISIBLE
            binding.tvRecordStop.visibility = GONE
        }
        binding.tvFlag.setOnClickListener { addFlag() }

        currentIsMultiSlot = ProtocolHelper.ar8030Get1VNMode()
        refreshPairedSlotsFromHelper()

        buildSlotRows(context)
        applySlotVisibility()
    }

    private fun refreshPairedSlotsFromHelper() {
        val arr = protocolHelper.ar8030GetPairedSlot()
        for (i in 0 until SLOT_COUNT) {
            pairedSlots[i] = if (arr != null && i < arr.size) arr[i] else false
        }
    }

    private fun buildSlotRows(context: Context) {
        slotRows.clear()
        binding.llSlotsContainer.removeAllViews()
        for (i in 0 until SLOT_COUNT) {
            val rowViews = createSlotRow(context, i)
            slotRows.add(rowViews)
            binding.llSlotsContainer.addView(rowViews.root)
        }
    }

    private fun createSlotRow(context: Context, slotIndex: Int): SlotRowViews {
        val container = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val labelTv = TextView(context).apply {
            layoutParams = LayoutParams(dp(40), LayoutParams.MATCH_PARENT)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            text = "Slot $slotIndex:"
            setTextColor(context.resources.getColor(R.color.white, null))
            textSize = 10f
        }
        row.addView(labelTv)

        val rightContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }

        val ap = createValuesRow(context)
        val dev = createValuesRow(context)
        rightContainer.addView(ap.root)
        rightContainer.addView(dev.root)

        row.addView(rightContainer)
        container.addView(row)

        val divider = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(1)).apply {
                topMargin = dp(2)
                bottomMargin = dp(2)
            }
            setBackgroundColor(context.resources.getColor(R.color.white, null))
            alpha = 0.3f
        }
        container.addView(divider)

        return SlotRowViews(root = container, row = row, divider = divider, ap = ap, dev = dev)
    }

    private fun createValuesRow(context: Context): ValuesRow {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        val snr = makeCell(context, dp(42))
        val rssi = makeCell(context, dp(50))
        val mcs = makeCell(context, dp(36))
        val txp = makeCell(context, dp(40))
        val err = makeCell(context, dp(50))
        val tp = makeCell(context, dp(70))
        val freq = makeCell(context, dp(50))
        val disc = makeCell(context, dp(50))
        listOf(snr, rssi, mcs, txp, err, tp, freq, disc).forEach { row.addView(it) }
        return ValuesRow(row, snr, rssi, mcs, txp, err, tp, freq, disc)
    }

    private fun makeCell(context: Context, width: Int): TextView {
        return TextView(context).apply {
            layoutParams = LayoutParams(width, LayoutParams.WRAP_CONTENT)
            gravity = Gravity.START
            setTextColor(context.resources.getColor(R.color.white, null))
            textSize = 10f
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }

    private fun applySlotVisibility() {
        for (i in 0 until SLOT_COUNT) {
            val visible = if (currentIsMultiSlot) {
                pairedSlots[i]
            } else {
                i == 0
            }
            slotRows[i].root.visibility = if (visible) View.VISIBLE else View.GONE
        }
    }

    private fun onModeChanged() {
        applySlotVisibility()
        stopSingleSlotPolling()
        stopMultiSlotPolling()
        if (currentIsMultiSlot) {
            startMultiSlotPolling()
        } else {
            startSingleSlotPolling()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun startRecord() {
        recordJob = viewScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "$timeStamp.csv"
                    val contentResolver = context.contentResolver

                    val relativePath = Environment.DIRECTORY_DOCUMENTS + File.separator + "AR8030"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                        } else {
                            val directory = Environment.getExternalStoragePublicDirectory(relativePath)
                            if (!directory.exists() && !directory.mkdirs()) {
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

                    outputStream.write(("Time,Pair,Self SNR,Self LDPC TLV Err Ratio,Self LDPC Num Err Ratio,Self Gain A,Self Gain B,Self TX MCS,Self TX Chan,Self TX Freq,Self Chan Power,Self TX Power,Peer SNR,Peer LDPC TLV Err Ratio,Peer LDPC Num Err Ratio,Peer Gain A,Peer Gain B,Peer TX MCS,Peer TX Chan,Peer TX Freq,Peer Chan Power,Peer TX Power,TX Real Throughput,RX Real Throughput,TX Phy Throughput,RX Phy Throughput,Chan Num,Auto Mode,ACS Chan,Work Chan," +
                            (0 until 15).joinToString(",") { "Chan $it Freq,Chan $it Power" } + "\n").toByteArray())

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
                                "${status8030!!.peerSnrValue},${status8030!!.peerLdpcTlvErrRatio}," +
                                "${status8030!!.peerLdpcNumErrRatio},${status8030!!.peerGainA},${status8030!!.peerGainB}," +
                                "${status8030!!.peerTxMcs},${status8030!!.peerTxChan}," +
                                "${status8030!!.peerTxFreqKhz}," +
                                "${if (chanInfo8030!!.power.size > status8030!!.peerTxChan) chanInfo8030!!.power[status8030!!.peerTxChan].toString() else ""}," +
                                "${status8030!!.peerTxPower}," +
                                "${throughput8030?.txRealThroughput?.div(1000)?.toInt() ?: ""},${throughput8030?.rxRealThroughput?.div(1000)?.toInt() ?: ""},${throughput8030?.txPhyThroughput?.div(1000)?.toInt() ?: ""},${throughput8030?.rxPhyThroughput?.div(1000)?.toInt() ?: ""}," +
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

    private fun startMultiSlotPolling() {
        pollMultiJob = viewScope.launch {
            while (isActive) {
                try {
                    pendingMcsApSlot = -1
                    pendingMcsDevSlot = -1
                    pendingTpApSlot = -1
                    pendingPwrApSlot = -1
                    pendingPwrDevSlot = -1

                    protocolHelper.ar8030GetUserQuality()
                    delay(80)
                    protocolHelper.ar8030GetPeerQuality()
                    delay(80)

                    for (slot in 0 until SLOT_COUNT) {
                        if (!pairedSlots[slot]) continue

                        pendingMcsApSlot = slot
                        pendingMcsDevSlot = -1
                        protocolHelper.ar8030GetMcs(DIR_TX, slot)
                        delay(60)

                        pendingMcsApSlot = -1
                        pendingMcsDevSlot = slot
                        protocolHelper.ar8030GetMcs(DIR_RX, slot)
                        delay(60)

                        pendingTpApSlot = slot
                        protocolHelper.ar8030GetThroughput(slot)
                        delay(60)

                        pendingPwrApSlot = slot
                        protocolHelper.ar8030GetCurPower(slot, false)
                        delay(60)

                        pendingPwrDevSlot = slot
                        protocolHelper.ar8030GetCurPower(slot, true)
                        delay(60)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500)
            }
        }
    }

    private fun stopMultiSlotPolling() {
        pollMultiJob?.cancel()
        pollMultiJob = null
        pendingMcsApSlot = -1
        pendingMcsDevSlot = -1
        pendingTpApSlot = -1
        pendingPwrApSlot = -1
        pendingPwrDevSlot = -1
    }

    private fun startSingleSlotPolling() {
        pollSingleJob = viewScope.launch {
            while (isActive) {
                try {
                    protocolHelper.ar8030GetThroughput(0)
                    delay(300)
                    if (protocolHelper.isAnySlotPaired && !ProtocolHelper.ar8030Get1VNMode()) {
                        protocolHelper.ar8030Get1v1Info(false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500)
            }
        }
    }

    private fun stopSingleSlotPolling() {
        pollSingleJob?.cancel()
        pollSingleJob = null
    }

    private fun startGetDistcTimer() {
        getDistcTimerJob = viewScope.launch {
            while (isActive) {
                try {
                    protocolHelper.ar8030GetDistcResult()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000)
            }
        }
    }

    private fun stopGetDistcTimer() {
        getDistcTimerJob?.cancel()
        getDistcTimerJob = null
    }

    private fun startRead8030ChannelInfoTimer() {
        if (refreshPairedSlotsJob == null) {
            refreshPairedSlotsJob = viewScope.launch {
                while (isActive) {
                    try {
                        protocolHelper.ar8030GetChannelInfo(false)
                        delay(10)
                        protocolHelper.ar8030GetChannelInfo(true)
                        syncPairedSlots()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun stopRead8030ChannelInfoTimer() {
        refreshPairedSlotsJob?.cancel()
        refreshPairedSlotsJob = null
    }

    private fun syncPairedSlots() {
        val arr = protocolHelper.ar8030GetPairedSlot()
        var changed = false
        for (i in 0 until SLOT_COUNT) {
            val newVal = arr != null && i < arr.size && arr[i]
            if (pairedSlots[i] != newVal) {
                pairedSlots[i] = newVal
                changed = true
            }
        }
        if (changed) {
            applySlotVisibility()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        usbDeviceHelper.addListener(usbDeviceListener)
        protocolHelper.addListener(protocolListener)
        currentIsMultiSlot = ProtocolHelper.ar8030Get1VNMode()
        refreshPairedSlotsFromHelper()
        applySlotVisibility()
        if (currentIsMultiSlot) {
            startMultiSlotPolling()
        } else {
            startSingleSlotPolling()
        }
        startGetDistcTimer()
        startRead8030ChannelInfoTimer()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        usbDeviceHelper.removeListener(usbDeviceListener)
        protocolHelper.removeListener(protocolListener)
        stopRecord()
        stopSingleSlotPolling()
        stopMultiSlotPolling()
        stopGetDistcTimer()
        stopRead8030ChannelInfoTimer()
        viewJob.cancel()
    }

    private fun updateSingleSlot() {
        val row = slotRows.getOrNull(0) ?: return
        val s = status8030
        if (s != null) {
            row.ap.snr.text = String.format(Locale.US, "%.1f", s.localSnrValue)
            row.ap.rssi.text = "${s.localGainA}/${s.localGainB}"
            row.ap.mcs.text = s.localTxMcs.toString()
            row.ap.txp.text = s.localTxPower.toString()
            row.ap.err.text = "${s.localLdpcTlvErrRatio}/${s.localLdpcNumErrRatio}"
            row.ap.freq.text = (s.localTxFreqKhz / 1000).toString()

            row.dev.snr.text = String.format(Locale.US, "%.1f", s.peerSnrValue)
            row.dev.rssi.text = "${s.peerGainA}/${s.peerGainB}"
            row.dev.mcs.text = s.peerTxMcs.toString()
            row.dev.txp.text = s.peerTxPower.toString()
            row.dev.err.text = "${s.peerLdpcTlvErrRatio}/${s.peerLdpcNumErrRatio}"
            row.dev.freq.text = (s.peerTxFreqKhz / 1000).toString()
        }

        val tp = throughput8030
        if (tp != null) {
            row.ap.tp.text = formatTp(tp.txRealThroughput, tp.txPhyThroughput)
            row.dev.tp.text = formatTp(tp.rxRealThroughput, tp.rxPhyThroughput)
        }

        row.ap.disc.text = discText(0)
        row.dev.disc.text = ""
    }

    private fun updateAllSlotsMulti() {
        for (i in 0 until SLOT_COUNT) {
            updateSlotMulti(i)
        }
    }

    private fun updateSlotMulti(slot: Int) {
        val row = slotRows.getOrNull(slot) ?: return
        if (row.root.visibility != View.VISIBLE) return

        val apQuality = userQuality8030?.qualities?.getOrNull(slot)
        val devQuality = peerQuality8030?.qualities?.getOrNull(slot)

        applyQualityToCells(row.ap, apQuality)
        applyQualityToCells(row.dev, devQuality)

        row.ap.mcs.text = if (slotMcsAp[slot] != Int.MIN_VALUE) slotMcsAp[slot].toString() else ""
        row.dev.mcs.text = if (slotMcsDev[slot] != Int.MIN_VALUE) slotMcsDev[slot].toString() else ""

        row.ap.txp.text = if (slotPwrAp[slot] != Int.MIN_VALUE) slotPwrAp[slot].toString() else ""
        row.dev.txp.text = if (slotPwrDev[slot] != Int.MIN_VALUE) slotPwrDev[slot].toString() else ""

        val tp = slotTpAp[slot]
        row.ap.tp.text = if (tp != null) formatTp(tp.txRealThroughput, tp.txPhyThroughput) else ""
        row.dev.tp.text = if (tp != null) formatTp(tp.rxRealThroughput, tp.rxPhyThroughput) else ""

        row.ap.freq.text = chanInfo8030?.let { freqText(it) } ?: ""
        row.dev.freq.text = chanInfo8030Sky?.let { freqText(it) } ?: ""

        row.ap.disc.text = discText(slot)
        row.dev.disc.text = ""
    }

    private fun discText(slot: Int): String {
        val arr = distcResult8030?.distance ?: return ""
        if (slot < 0 || slot >= arr.size) return ""
        return arr[slot].toString()
    }

    private fun applyQualityToCells(row: ValuesRow, q: Quality8030?) {
        if (q == null) {
            row.snr.text = ""
            row.rssi.text = ""
            row.err.text = ""
            return
        }
        row.snr.text = String.format(Locale.US, "%.1f", q.snr)
        row.rssi.text = "${q.gainA}/${q.gainB}"
        row.err.text = "${q.ldpcErr}/${q.ldpcNum}"
    }

    private fun freqText(info: ChanInfo8030): String {
        val freqs = info.freq ?: return ""
        val workChan = info.workChan
        if (workChan < 0 || workChan >= freqs.size) return ""
        return (freqs[workChan] / 1000).toString()
    }

    private fun formatTp(real: Long?, phy: Long?): String {
        val r = real?.div(1000)?.toInt()
        val p = phy?.div(1000)?.toInt()
        return "${r ?: ""}/${p ?: ""}"
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateChanInfoUi(chanInfo: ChanInfo8030) {
        updateChanInfoUi(
            chanInfo,
            binding.tvFreqAndPower24,
            binding.tvFreqAndPower
        )
    }

    private fun updateChanInfoSkyUi(chanInfo: ChanInfo8030) {
        updateChanInfoUi(
            chanInfo,
            binding.tvFreqAndPowerSky24,
            binding.tvFreqAndPowerSky
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateChanInfoUi(chanInfo: ChanInfo8030, text24: TextView, text5g: TextView) {
        val n = chanInfo.freq.size.coerceAtMost(chanInfo.chanNum)
        val list24 = mutableListOf<Pair<Long, Int?>>()
        val list5g = mutableListOf<Pair<Long, Int?>>()
        for (i in 0..<n) {
            val mhz = chanInfo.freq[i] / 1000
            val p = if (chanInfo.power.size > i) chanInfo.power[i] else null
            if (chanInfo.freq[i].toString().startsWith("2")) {
                list24.add(mhz to p)
            } else {
                list5g.add(mhz to p)
            }
        }
        text24.text = formatChanInfoLines(list24)
        text5g.text = formatChanInfoLines(list5g)
    }

    private fun formatChanBlock(freq: Long, p: Int?): String {
        val ps = p?.let { v -> String.format(Locale.US, "%4d", v) } ?: "    "
        return String.format(Locale.US, "%4d:%s", freq, ps)
    }

    private fun formatChanInfoLines(items: List<Pair<Long, Int?>>): String {
        if (items.isEmpty()) return ""
        return items.chunked(8).joinToString("\n") { line ->
            line.joinToString(" ") { (f, p) -> formatChanBlock(f, p) }
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

        override fun onVideoData(data: ByteArray?, length: Int, deviceType: DEVICE_TYPE?) {}
        override fun onAudioData(data: ByteArray?, length: Int) {}
        override fun onCtrlData(data: ByteArray?, length: Int, deviceType: DEVICE_TYPE?) {}
    }

    private val protocolListener: ProtocolListener = object : ProtocolListener {
        override fun onReady(p0: com.fly.station.prorocol.DEVICE_TYPE?) {}

        override fun onReadCmd(
            packet: BaseFlyPacket?,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            isRemote: Boolean
        ) {
            when (packet) {
                is RcStatus8030 -> {
                    status8030 = packet
                    handler.post {
                        if (!currentIsMultiSlot) updateSingleSlot()
                    }
                }
                is ChanInfo8030 -> {
                    if (isRemote) {
                        chanInfo8030Sky = packet
                        handler.post {
                            updateChanInfoSkyUi(packet)
                            if (currentIsMultiSlot) updateAllSlotsMulti()
                        }
                    } else {
                        chanInfo8030 = packet
                        handler.post {
                            updateChanInfoUi(packet)
                            if (currentIsMultiSlot) updateAllSlotsMulti()
                        }
                    }
                }
                is GetMcs8030 -> {
                    if (currentIsMultiSlot) {
                        val apSlot = pendingMcsApSlot
                        val devSlot = pendingMcsDevSlot
                        if (apSlot in 0 until SLOT_COUNT) {
                            slotMcsAp[apSlot] = packet.mcs
                            pendingMcsApSlot = -1
                            handler.post { updateSlotMulti(apSlot) }
                        } else if (devSlot in 0 until SLOT_COUNT) {
                            slotMcsDev[devSlot] = packet.mcs
                            pendingMcsDevSlot = -1
                            handler.post { updateSlotMulti(devSlot) }
                        }
                    }
                }
                is UserQuality8030 -> {
                    userQuality8030 = packet
                    handler.post {
                        if (currentIsMultiSlot) updateAllSlotsMulti()
                    }
                }
                is PeerQuality8030 -> {
                    peerQuality8030 = packet
                    handler.post {
                        if (currentIsMultiSlot) updateAllSlotsMulti()
                    }
                }
                is CurPower8030 -> {
                    if (currentIsMultiSlot) {
                        if (isRemote) {
                            val s = pendingPwrDevSlot
                            if (s in 0 until SLOT_COUNT) {
                                slotPwrDev[s] = packet.pwr
                                handler.post { updateSlotMulti(s) }
                            }
                        } else {
                            val s = if (packet.usr in 0 until SLOT_COUNT) packet.usr else pendingPwrApSlot
                            if (s in 0 until SLOT_COUNT) {
                                slotPwrAp[s] = packet.pwr
                                handler.post { updateSlotMulti(s) }
                            }
                        }
                    }
                }
                is DistcResult8030 -> {
                    distcResult8030 = packet
                    handler.post {
                        if (currentIsMultiSlot) updateAllSlotsMulti() else updateSingleSlot()
                    }
                }
            }
        }

        override fun onWrite(p0: ByteArray?): Int = 0

        override fun onThroughput(
            deviceType: com.fly.station.prorocol.DEVICE_TYPE?,
            throughput: Throughput8030?,
            isRemote: Boolean
        ) {
            if (throughput == null) return
            throughput8030 = throughput
            if (currentIsMultiSlot) {
                val s = pendingTpApSlot
                if (s in 0 until SLOT_COUNT) {
                    slotTpAp[s] = throughput
                    handler.post { updateSlotMulti(s) }
                }
            } else {
                handler.post { updateSingleSlot() }
            }
        }

        override fun onMultiSlotChanged(
            deviceType: com.fly.station.prorocol.DEVICE_TYPE?,
            isMultiSlot: Boolean
        ) {
            if (currentIsMultiSlot != isMultiSlot) {
                currentIsMultiSlot = isMultiSlot
                handler.post { onModeChanged() }
            }
        }

        override fun onPairOperated(
            deviceType: com.fly.station.prorocol.DEVICE_TYPE?,
            slot: Int,
            isStart: Boolean
        ) {}

        override fun onPairTimeOut(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = false
            updatePair(pairStatus)
            handler.post { syncPairedSlots() }
        }

        override fun onPairSuccess(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = true
            updatePair(pairStatus)
            handler.post { syncPairedSlots() }
        }

        override fun onLinked(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = true
            updatePair(pairStatus)
            handler.post { syncPairedSlots() }
        }

        override fun onLinkLost(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: Int) {
            pairStatus = false
            updatePair(pairStatus)
            handler.post { syncPairedSlots() }
        }

        override fun onConfigJson(
            p0: String?,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {}

        override fun onSetConfigJson(
            p0: Boolean,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {}

        override fun onResetConfigJson(
            p0: Boolean,
            p1: com.fly.station.prorocol.DEVICE_TYPE?,
            p2: Boolean
        ) {}

        override fun onSlotMac(
            p0: com.fly.station.prorocol.DEVICE_TYPE?,
            p1: Int,
            p2: String?
        ) {}

        override fun onSetRadio(
            deviceType: com.fly.station.prorocol.DEVICE_TYPE?,
            radioType: RADIO_TYPE?,
            isSuccess: Boolean,
            errcode: Int,
            errMessage: String?,
            isRemote: Boolean
        ) {}

        override fun onRoleChanged(
            deviceType: com.fly.station.prorocol.DEVICE_TYPE?,
            role: AR8030Role?
        ) {}

        override fun onDebugMessage(p0: com.fly.station.prorocol.DEVICE_TYPE?, p1: String?) {}
    }

    private data class SlotRowViews(
        val root: LinearLayout,
        val row: LinearLayout,
        val divider: View,
        val ap: ValuesRow,
        val dev: ValuesRow,
    )

    private data class ValuesRow(
        val root: LinearLayout,
        val snr: TextView,
        val rssi: TextView,
        val mcs: TextView,
        val txp: TextView,
        val err: TextView,
        val tp: TextView,
        val freq: TextView,
        val disc: TextView,
    )

    companion object {
        private const val SLOT_COUNT = 8
    }
}
