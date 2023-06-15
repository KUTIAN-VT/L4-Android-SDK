package com.coolfly.demo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
        tvLog = findViewById(R.id.tv_log);

        handler = new Handler(Looper.getMainLooper());

        sensorDevice = SensorDevice.getInstance(MainApplication.applicationContext);
        sensorDevice.addListener(sensorDeviceListener);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! sensorDevice.isConnectionAlive()) {
                    // SOCKET for 301, SERIAL for 201
                    sensorDevice.onLine(SensorDevice.SOCKET);
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
                resetSbus();
            }
        });
    }

    private void resetSbus() {
        sensorDevice.readSbus();
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