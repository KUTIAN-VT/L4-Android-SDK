package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.station.chuanyun.SensorDevice;
import com.coolfly.station.chuanyun.entity.Calibrate;
import com.coolfly.station.chuanyun.entity.PairResponse;
import com.coolfly.station.chuanyun.entity.Sbus;
import com.coolfly.station.chuanyun.entity.Status;

public class ChuanYunActivity extends AppCompatActivity {
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnReadStatus;
    private Button btnReadSbus;
    private Button btnWriteSbus;
    private Button btnPairDevice;
    private TextView tvLog;

    private Handler handler;
    private SensorDevice sensorDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chuan_yun);

        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnReadStatus = findViewById(R.id.btn_read_status);
        btnReadSbus = findViewById(R.id.btn_read_sbus);
        btnWriteSbus = findViewById(R.id.btn_write_sbus);
        btnPairDevice = findViewById(R.id.btn_pair_device);
        tvLog = findViewById(R.id.tv_log);

        handler = new Handler(Looper.getMainLooper());

        sensorDevice = SensorDevice.getInstance(MainApplication.applicationContext);
        sensorDevice.addListener(sensorDeviceListener);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! sensorDevice.isConnectionAlive()) {
                    // Show dialog for user to choose connection type
                    // SOCKET for 301, SERIAL for 201
                    new AlertDialog.Builder(ChuanYunActivity.this)
                            .setTitle("Choose connection type")
                            .setItems(new String[]{"Socket for P301", "Serial for P201"}, (dialog, which) -> {
                                if (which == 0) {
                                    // default: 192.168.1.100
                                    SensorDevice.setIp("192.168.1.100");
                                    // default: 1235
                                    SensorDevice.setPort(1235);
                                    sensorDevice.onLine(SensorDevice.SOCKET);
                                } else {
                                    // default: /dev/ttyHS0
                                    SensorDevice.setDevicePath("/dev/ttyHS0");
                                    // default: 460800
                                    SensorDevice.setBaudRate("460800");
                                    sensorDevice.onLine(SensorDevice.SERIAL);
                                }
                            }).create().show();
                } else {
                    Toast.makeText(ChuanYunActivity.this, "Already connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sensorDevice.isConnectionAlive()) {
                    sensorDevice.offLine();
                }
            }
        });

        btnReadStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.readStatus();
            }
        });

        btnReadSbus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                readSbus();
            }
        });

        btnWriteSbus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeSbus();
            }
        });

        btnPairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.pairDevice(0);
            }
        });
    }

    private void readSbus() {
        sensorDevice.readSbus();
    }

    private void writeSbus() {
        // 以设置美国手、日本手来举例
        new AlertDialog.Builder(ChuanYunActivity.this)
                .setTitle("美国手、日本手")
                .setItems(new String[]{"美国手", "日本手"}, (dialog, which) -> {
                    Sbus sbus = new Sbus();
                    sbus.action = 1;
                    if (which == 0) {
                        sbus.ch_jp_am = 0;
                    } else {
                        sbus.ch_jp_am = 1;
                    }
                    sensorDevice.writeSbus(sbus);
                }).create().show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorDevice.offLine();
        sensorDevice.removeListener(sensorDeviceListener);
    }

    private SensorDevice.SensorDeviceListener sensorDeviceListener = new SensorDevice.SensorDeviceListener() {
        @Override
        public void onStatus(Status status) {
            handler.post(() -> {
                tvLog.setText("RECEIVE: " + status.toString() + "\n");
            });
        }

        @Override
        public void onPairResponse(PairResponse pairResponse) {
            handler.post(() -> {
                tvLog.setText("RECEIVE: " + pairResponse.toString() + "\n");
            });
        }

        @Override
        public void onSbus(Sbus sbus) {
            handler.post(() -> {
                tvLog.setText("RECEIVE: " + sbus.toString() + "\n");
            });
        }

        @Override
        public void onCalibrate(Calibrate calibrate) {
            handler.post(() -> {
                tvLog.setText("RECEIVE: " + calibrate.toString() + "\n");
            });
        }
    };
}