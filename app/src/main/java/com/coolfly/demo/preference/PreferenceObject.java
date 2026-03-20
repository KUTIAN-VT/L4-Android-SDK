package com.coolfly.demo.preference;

import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.fflibrary.MediaConfig;
import com.fly.station.chuanyun.SensorDevice;
import com.fly.station.mcu.McuManager;
import com.fly.station.prorocol.AR8030VpnReader;
import com.fly.station.prorocol.Constants;

import java.io.Serializable;

/**
 * @Description:
 * @Author: zongheng.wu
 * @Date: 2025/4/21 20:32
 */
public class PreferenceObject implements Serializable {
    public String mcu_serial_path = McuManager.DEVICE_PATH;
    public Integer mcu_serial_baudrate = Integer.parseInt(McuManager.BAUDRATE);

    public String p301_socket_ip = SensorDevice.getIP();
    public Integer p301_socket_port = SensorDevice.getPORT();

    public String p201_serial_path = SensorDevice.DEVICE_PATH;
    public Integer p201_serial_baudrate = Integer.parseInt(SensorDevice.BAUDRATE);

    public Integer p401_port_eth = 3;
    public Integer p401_port_passthrough = 2;
    public Integer p401_dev_count = 1;
    public Integer p401_rx_buffer_slot0_port0 = 60000;
    public Integer p401_tx_buffer_slot0_port0 = 40000;
    public Integer p401_rx_buffer_slot0_port1 = 60000;
    public Integer p401_tx_buffer_slot0_port1 = 40000;
    public Integer p401_rx_buffer_slot0_port2 = 60000;
    public Integer p401_tx_buffer_slot0_port2 = 40000;
    public Integer p401_rx_buffer_slot0_port3 = 60000;
    public Integer p401_tx_buffer_slot0_port3 = 40000;
    public Integer p401_1vn_rx_buffer_port0 = 6000;
    public Integer p401_1vn_tx_buffer_port0 = 4000;
    public Integer p401_1vn_rx_buffer_port1 = 6000;
    public Integer p401_1vn_tx_buffer_port1 = 4000;
    public Integer p401_1vn_rx_buffer_port2 = 6000;
    public Integer p401_1vn_tx_buffer_port2 = 4000;
    public Integer p401_1vn_rx_buffer_port3 = 6000;
    public Integer p401_1vn_tx_buffer_port3 = 4000;
    public Integer p401_mtu = AR8030VpnReader.MTU;
    public String p401_ip = AR8030VpnReader.IP;
    public String p401_subnet_mask = AR8030VpnReader.SubnetMask;
    public Boolean p401_datagram = false;

    public Boolean show_mcu_log = McuManager.isShowLog;
    public Boolean show_ffmpeg_log = false;
    public Boolean show_usb_log = UsbDeviceHelper.isShowLog;
    public Boolean show_chuanyun_log = SensorDevice.isShowLog;
    public Boolean show_ar8030_vpn_log = Constants.isShowAR8030VPNLog;
    public Boolean show_ar8030_parse_log = Constants.isShowAR8030ParseLog;

    public Boolean write_log_to_file = false;
    public Boolean write_log_to_serial = false;

    public MediaConfig mediaConfig = new MediaConfig();

    public PreferenceObject() {
    }
}
