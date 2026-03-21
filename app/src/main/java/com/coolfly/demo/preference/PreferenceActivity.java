package com.coolfly.demo.preference;

import static com.coolfly.demo.utils.Constants.PREF_APP_CONFIG;
import static com.coolfly.demo.utils.Constants.SP_NAME;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.serialport.SerialPortFinder;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.coolfly.demo.MainApplication;
import com.coolfly.demo.R;
import com.coolfly.demo.chuanyun.preference.SerialPortPreferences;
import com.coolfly.demo.chuanyun.preference.SocketPreferences;
import com.coolfly.demo.databinding.ActivityPreferenceBinding;
import com.fly.aoalibrary.host.UsbDeviceHelper;
import com.fly.fflibrary.FFJNI;
import com.fly.fflibrary.MediaConfig;
import com.fly.loglibrary.Loggers;
import com.fly.station.chuanyun.SensorDevice;
import com.fly.station.mcu.McuManager;
import com.fly.station.prorocol.Constants;
import com.fly.station.prorocol.ProtocolHelper;

public class PreferenceActivity extends AppCompatActivity {
    public static PreferenceObject preferenceObject = null;

    private ActivityPreferenceBinding binding;
    private static SharedPreferences sp;

    public static void initPreference() {
        sp = MainApplication.applicationContext.getSharedPreferences(SP_NAME, MODE_PRIVATE);
        try {
            preferenceObject = JSON.parseObject(sp.getString(PREF_APP_CONFIG, null), PreferenceObject.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        if (preferenceObject == null) {
            preferenceObject = new PreferenceObject();
        }
    }

    public static void savePreference() {
        if (preferenceObject != null) {
            sp.edit().putString(PREF_APP_CONFIG, JSON.toJSONString(preferenceObject)).commit();
            Toast.makeText(MainApplication.applicationContext, "Preference saved", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainApplication.applicationContext, "Preference is null", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.exit(0);
            }
        });

        binding.tvMcuPath.setText(McuManager.DEVICE_PATH);
        binding.tvMcuPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] paths = new SerialPortFinder().getAllDevicesPath();
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("device path")
                        .setItems(paths, (dialog, which) -> {
                            McuManager.setDevicePath(paths[which]);
                            binding.tvMcuPath.setText(McuManager.DEVICE_PATH);
                            preferenceObject.mcu_serial_path = McuManager.DEVICE_PATH;
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.tvMcuBaudrate.setText(McuManager.BAUDRATE);
        binding.tvMcuBaudrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] baudrates = getResources().getStringArray(R.array.baudrates_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("baudrate")
                        .setItems(baudrates, (dialog, which) -> {
                            McuManager.setBaudRate(baudrates[which]);
                            binding.tvMcuBaudrate.setText(McuManager.BAUDRATE);
                            preferenceObject.mcu_serial_baudrate = Integer.parseInt(McuManager.BAUDRATE);
                            PreferenceActivity.savePreference();
                        }).create().show();
            }
        });

        binding.tvP201.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PreferenceActivity.this, SerialPortPreferences.class);
                startActivity(intent);
            }
        });

        binding.tvP301.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PreferenceActivity.this, SocketPreferences.class);
                startActivity(intent);
            }
        });

        binding.tvP401PortEth.setText(String.valueOf(preferenceObject.p401_port_eth));
        binding.tvP401PortEth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] ports = getResources().getStringArray(R.array.p401ports_eth_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("eth port")
                        .setItems(ports, (dialog, which) -> {
                            int port = Integer.parseInt(ports[which]);
                            boolean res = ProtocolHelper.ar8030SetPortEth(port);
                            if (res) {
                                binding.tvP401PortEth.setText(ports[which]);
                                preferenceObject.p401_port_eth = port;
                                PreferenceActivity.savePreference();
                            } else {
                                Toast.makeText(PreferenceActivity.this, "set eth port failed", Toast.LENGTH_SHORT).show();
                            }
                        }).create().show();
            }
        });

        binding.tvP401PortPassthrough.setText(String.valueOf(preferenceObject.p401_port_passthrough));
        binding.tvP401PortPassthrough.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] ports = getResources().getStringArray(R.array.p401ports_passthrough_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("passthrough port")
                        .setItems(ports, (dialog, which) -> {
                            int port = Integer.parseInt(ports[which]);
                            boolean res = ProtocolHelper.ar8030SetPortPassthrough(port);
                            if (res) {
                                binding.tvP401PortPassthrough.setText(ports[which]);
                                preferenceObject.p401_port_passthrough = port;
                                PreferenceActivity.savePreference();
                            } else {
                                Toast.makeText(PreferenceActivity.this, "set passthrough port failed", Toast.LENGTH_SHORT).show();
                            }
                        }).create().show();
            }
        });

        binding.etP401Mtu.setText(String.valueOf(preferenceObject.p401_mtu));
        binding.tvP401MtuSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int mtu = Integer.parseInt(binding.etP401Mtu.getText().toString());

                boolean res = ProtocolHelper.ar8030SetMTU(mtu);
                if (res) {
                    preferenceObject.p401_mtu = mtu;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP401Ip.setText(preferenceObject.p401_ip);
        binding.tvP401IpSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = binding.etP401Ip.getText().toString();

                boolean res = ProtocolHelper.ar8030SetIP(ip);
                if (res) {
                    preferenceObject.p401_ip = ip;
                    PreferenceActivity.savePreference();
                    if (preferenceObject.p401_dev_count == 1) {
                        // Delete existing tap
                        ProtocolHelper.getInstance().ar8030CloseEth(0, preferenceObject.p401_port_eth);
                        // Restart eth for users who not restarting the application after changing the IP
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            ProtocolHelper.getInstance().ar8030OpenEth(0, preferenceObject.p401_port_eth);
                        }, 3000);
                    }
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP401SubnetMask.setText(preferenceObject.p401_subnet_mask);
        binding.tvP401SubnetMaskSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String subnetMask = binding.etP401SubnetMask.getText().toString();

                boolean res = ProtocolHelper.ar8030SetSubnetMask(subnetMask);
                if (res) {
                    preferenceObject.p401_subnet_mask = subnetMask;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.tvP401DevCount.setText(String.valueOf(preferenceObject.p401_dev_count));
        binding.tvP401DevCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] counts = getResources().getStringArray(R.array.p401dev_count_value);
                new AlertDialog.Builder(PreferenceActivity.this)
                        .setTitle("dev count")
                        .setItems(counts, (dialog, which) -> {
                            int count = Integer.parseInt(counts[which]);
                            binding.tvP401DevCount.setText(counts[which]);
                            preferenceObject.p401_dev_count = count;
                            PreferenceActivity.savePreference();

                            // >1 means 1vN mode, where N is the number of dev.
                            // It will take effect after rebooting the ground image transmission.
                            boolean res = ProtocolHelper.getInstance().ar8030Set1VNMode(count);
                            if (res) {
                                Toast.makeText(PreferenceActivity.this, "Please turn the ground image transmission power off and then on again", Toast.LENGTH_SHORT).show();
                            }
                        }).create().show();
            }
        });

        binding.etP401RxBufferPort0.setText(String.valueOf(preferenceObject.p401_rx_buffer_slot0_port0));
        binding.etP401TxBufferPort0.setText(String.valueOf(preferenceObject.p401_tx_buffer_slot0_port0));
        binding.tvP401BufferPort0Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP401RxBufferPort0.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP401TxBufferPort0.getText().toString());

                boolean res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, 0, 0);
                if (res) {
                    preferenceObject.p401_rx_buffer_slot0_port0 = rxBuffer;
                    preferenceObject.p401_tx_buffer_slot0_port0 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP401RxBufferPort1.setText(String.valueOf(preferenceObject.p401_rx_buffer_slot0_port1));
        binding.etP401TxBufferPort1.setText(String.valueOf(preferenceObject.p401_tx_buffer_slot0_port1));
        binding.tvP401BufferPort1Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP401RxBufferPort1.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP401TxBufferPort1.getText().toString());

                boolean res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, 0, 1);
                if (res) {
                    preferenceObject.p401_rx_buffer_slot0_port1 = rxBuffer;
                    preferenceObject.p401_tx_buffer_slot0_port1 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP401RxBufferPort2.setText(String.valueOf(preferenceObject.p401_rx_buffer_slot0_port2));
        binding.etP401TxBufferPort2.setText(String.valueOf(preferenceObject.p401_tx_buffer_slot0_port2));
        binding.tvP401BufferPort2Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP401RxBufferPort2.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP401TxBufferPort2.getText().toString());

                boolean res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, 0, 2);
                if (res) {
                    preferenceObject.p401_rx_buffer_slot0_port2 = rxBuffer;
                    preferenceObject.p401_tx_buffer_slot0_port2 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP401RxBufferPort3.setText(String.valueOf(preferenceObject.p401_rx_buffer_slot0_port3));
        binding.etP401TxBufferPort3.setText(String.valueOf(preferenceObject.p401_tx_buffer_slot0_port3));
        binding.tvP401BufferPort3Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP401RxBufferPort3.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP401TxBufferPort3.getText().toString());

                boolean res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, 0, 3);
                if (res) {
                    preferenceObject.p401_rx_buffer_slot0_port3 = rxBuffer;
                    preferenceObject.p401_tx_buffer_slot0_port3 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP4011vnRxBufferPort0.setText(String.valueOf(preferenceObject.p401_1vn_rx_buffer_port0));
        binding.etP4011vnTxBufferPort0.setText(String.valueOf(preferenceObject.p401_1vn_tx_buffer_port0));
        binding.tvP4011vnBufferPort0Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP4011vnRxBufferPort0.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP4011vnTxBufferPort0.getText().toString());

                boolean res = false;
                for (int i = 0; i<8; i++) {
                    res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, i, 0);
                    if (!res) {
                        break;
                    }
                }
                if (res) {
                    preferenceObject.p401_1vn_rx_buffer_port0 = rxBuffer;
                    preferenceObject.p401_1vn_tx_buffer_port0 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP4011vnRxBufferPort1.setText(String.valueOf(preferenceObject.p401_1vn_rx_buffer_port1));
        binding.etP4011vnTxBufferPort1.setText(String.valueOf(preferenceObject.p401_1vn_tx_buffer_port1));
        binding.tvP4011vnBufferPort1Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP4011vnRxBufferPort1.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP4011vnTxBufferPort1.getText().toString());

                boolean res = false;
                for (int i = 0; i<8; i++) {
                    res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, i, 1);
                    if (!res) {
                        break;
                    }
                }
                if (res) {
                    preferenceObject.p401_1vn_rx_buffer_port1 = rxBuffer;
                    preferenceObject.p401_1vn_tx_buffer_port1 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP4011vnRxBufferPort2.setText(String.valueOf(preferenceObject.p401_1vn_rx_buffer_port2));
        binding.etP4011vnTxBufferPort2.setText(String.valueOf(preferenceObject.p401_1vn_tx_buffer_port2));
        binding.tvP4011vnBufferPort2Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP4011vnRxBufferPort2.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP4011vnTxBufferPort2.getText().toString());

                boolean res = false;
                for (int i = 0; i<8; i++) {
                    res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, i, 2);
                    if (!res) {
                        break;
                    }
                }
                if (res) {
                    preferenceObject.p401_1vn_rx_buffer_port2 = rxBuffer;
                    preferenceObject.p401_1vn_tx_buffer_port2 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.etP4011vnRxBufferPort3.setText(String.valueOf(preferenceObject.p401_1vn_rx_buffer_port3));
        binding.etP4011vnTxBufferPort3.setText(String.valueOf(preferenceObject.p401_1vn_tx_buffer_port3));
        binding.tvP4011vnBufferPort3Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rxBuffer = Integer.parseInt(binding.etP4011vnRxBufferPort3.getText().toString());
                int txBuffer = Integer.parseInt(binding.etP4011vnTxBufferPort3.getText().toString());

                boolean res = false;
                for (int i = 0; i<8; i++) {
                    res = ProtocolHelper.ar8030SetBufferSize(rxBuffer, txBuffer, i, 3);
                    if (!res) {
                        break;
                    }
                }
                if (res) {
                    preferenceObject.p401_1vn_rx_buffer_port3 = rxBuffer;
                    preferenceObject.p401_1vn_tx_buffer_port3 = txBuffer;
                    PreferenceActivity.savePreference();
                } else {
                    Toast.makeText(PreferenceActivity.this, "Error! See logcat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.swP401Datagram.setChecked(ProtocolHelper.ar8030IsUseDatagram());
        binding.swP401Datagram.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ProtocolHelper.ar8030SetUseDatagram(isChecked);
            preferenceObject.p401_datagram = isChecked;
            savePreference();
        });

        binding.swLogMcu.setChecked(McuManager.isShowLog);
        binding.swLogMcu.setOnCheckedChangeListener((buttonView, isChecked) -> {
            McuManager.setIsShowLog(isChecked);
            preferenceObject.show_mcu_log = isChecked;
            savePreference();
        });

        binding.swLogFfmpeg.setChecked(preferenceObject.show_ffmpeg_log);
        binding.swLogFfmpeg.setOnCheckedChangeListener((buttonView, isChecked) -> {
            FFJNI.setLog(isChecked);
            preferenceObject.show_ffmpeg_log = isChecked;
            savePreference();
        });

        binding.swLogUsb.setChecked(UsbDeviceHelper.isShowLog);
        binding.swLogUsb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            UsbDeviceHelper.isShowLog = isChecked;
            preferenceObject.show_usb_log = isChecked;
            savePreference();
        });

        binding.swLogChuanyun.setChecked(SensorDevice.isShowLog);
        binding.swLogChuanyun.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorDevice.isShowLog = isChecked;
            preferenceObject.show_chuanyun_log = isChecked;
            savePreference();
        });

        binding.swLogAr8030Vpn.setChecked(Constants.isShowAR8030VPNLog);
        binding.swLogAr8030Vpn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030VPNLog = isChecked;
            preferenceObject.show_ar8030_vpn_log = isChecked;
            savePreference();
        });

        binding.swLogAr8030Parse.setChecked(Constants.isShowAR8030ParseLog);
        binding.swLogAr8030Parse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Constants.isShowAR8030ParseLog = isChecked;
            preferenceObject.show_ar8030_parse_log = isChecked;
            savePreference();
        });

        binding.swLogsFile.setChecked(preferenceObject.write_log_to_file);
        binding.swLogsFile.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Loggers.addToBlackList(Loggers.LOG_TYPE_FILE);
            } else {
                Loggers.removeFromBlackList(Loggers.LOG_TYPE_FILE);
            }
            preferenceObject.write_log_to_file = isChecked;
            savePreference();
        });

        binding.swLogsSerial.setChecked(preferenceObject.write_log_to_serial);
        binding.swLogsSerial.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Loggers.addToBlackList(Loggers.LOG_TYPE_SERIAL);
            } else {
                Loggers.removeFromBlackList(Loggers.LOG_TYPE_SERIAL);
            }
            preferenceObject.write_log_to_serial = isChecked;
            savePreference();
        });

        /*
         * MediaConfig类用于配置媒体流的参数。
         * 字段说明：
         * - rtsp_timeout_us: 表示socket超时时间，单位为微秒。默认值为2000000微秒（2秒）。
         * - is_rtsp_tcp: 表示RTSP协议是否使用TCP传输。默认值为false，表示使用UDP传输。
         * - probe_size: 表示从输入中读取以确定流属性的最大字节数。默认值为1048576字节（1MB）。
         * - max_analyze_duration: 表示最大分析持续时间。0表示自动检测，其他值表示AV_TIME_BASE的倍数。默认值为0，表示自动。
         * - notify_i_p_frame_last_bytes: 表示返回I帧（IDR和Slice中的I）和P帧的最后几个字节，一般用于存储视频帧中的AI标记信息。<=0表示不返回，>0表示返回的字节数。默认值为0。
         * - is_fast_resume: SurfaceView重新创建后是否快速恢复播放。默认值为true。原理：SurfaceView销毁后，不释放流，并缓存流信息。SurfaceView重新创建后，如果流可用，直接用缓存的流信息恢复播放。如果流不可用，降级为重新创建流，并重新解析流信息。
         */
        binding.etMediaConfig.setText(JSON.toJSONString(preferenceObject.mediaConfig));

        binding.tvSaveMediaConfig.setOnClickListener(v -> {
            MediaConfig mediaConfig = null;
            try {
                mediaConfig = JSON.parseObject(binding.etMediaConfig.getText().toString(), MediaConfig.class);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            if (mediaConfig == null) {
                Toast.makeText(PreferenceActivity.this, "mediaConfig is invalid", Toast.LENGTH_SHORT).show();
                return;
            }
            preferenceObject.mediaConfig = mediaConfig;
            savePreference();
        });
    }
}