package com.coolfly.demo.chuanyun;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.InfraredActivity;
import com.coolfly.demo.MainApplication;
import com.coolfly.demo.R;
import com.coolfly.demo.chuanyun.preference.SerialPortPreferences;
import com.coolfly.demo.chuanyun.preference.SocketPreferences;
import com.coolfly.demo.databinding.ActivityChuanYunBinding;
import com.coolfly.station.chuanyun.SensorDevice;
import com.coolfly.station.chuanyun.entity.Calibrate;
import com.coolfly.station.chuanyun.entity.InfraredConfig;
import com.coolfly.station.chuanyun.entity.PairResponse;
import com.coolfly.station.chuanyun.entity.RFConfig;
import com.coolfly.station.chuanyun.entity.RFConfig2;
import com.coolfly.station.chuanyun.entity.Sbus;
import com.coolfly.station.chuanyun.entity.Status;
import com.coolfly.station.chuanyun.entity.Version;

public class ChuanYunActivity extends AppCompatActivity {
    private ActivityChuanYunBinding binding;

    private Handler handler;
    private SensorDevice sensorDevice;
    private int videoMode201 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChuanYunBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        handler = new Handler(Looper.getMainLooper());

        sensorDevice = SensorDevice.getInstance(MainApplication.applicationContext);
        sensorDevice.addListener(sensorDeviceListener);

        binding.btnSetSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChuanYunActivity.this, SocketPreferences.class);
                startActivity(intent);
            }
        });

        binding.btnSetSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChuanYunActivity.this, SerialPortPreferences.class);
                startActivity(intent);
            }
        });

        binding.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (! sensorDevice.isConnectionAlive()) {
                    // Show dialog for user to choose connection type
                    // SOCKET for 301, SERIAL for 201
                    new AlertDialog.Builder(ChuanYunActivity.this)
                            .setTitle("Choose connection type")
                            .setItems(new String[]{"Socket for P301", "Serial for P201"}, (dialog, which) -> {
                                if (which == 0) {
                                    // IP and PORT set in SocketPreferences
                                    sensorDevice.onLine(SensorDevice.SOCKET);
                                } else {
                                    // PATH and BAUDRATE set in SerialPortPreferences
                                    sensorDevice.onLine(SensorDevice.SERIAL);
                                }
                            }).create().show();
                } else {
                    Toast.makeText(ChuanYunActivity.this, "Already connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.offLine();
            }
        });

        // Show ChuanYun Log in logcat. Disable in production environment.
        binding.swLog.setChecked(SensorDevice.isShowLog);
        binding.swLog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SensorDevice.setIsShowLog(isChecked);
            }
        });

        binding.btnReadStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.readStatus();
            }
        });

        binding.btnReadSbus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Read all fields of SBUS, different from only reading the joystick value automatically
                sensorDevice.readSbus();
            }
        });

        binding.btnWriteSbus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeSbus();
            }
        });

        binding.btnReadRfConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.readRfConfig();
            }
        });

        binding.btnSetWorkMode7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.writeRfConfig(RFConfig.WorkMode(7));
            }
        });

        binding.btnWriteDevMac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = binding.etDevMac.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    // not available for soft switch
                    sensorDevice.writeRfConfig(RFConfig.DevMac0(value));
                }
            }
        });

        binding.btnWriteRfPowerFix.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value2 = binding.etRfPowerFix2.getText().toString();
                String value5 = binding.etRfPowerFix5.getText().toString();
                if (!TextUtils.isEmpty(value2) && !TextUtils.isEmpty(value5)) {
                    // not available for soft switch
                    sensorDevice.writeRfConfig(RFConfig.RfPower(RFConfig.RfPower.FIX(Integer.parseInt(value2), Integer.parseInt(value5))));
                }
            }
        });

        binding.btnWriteRfPowerAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String valueDown2 = binding.etRfPowerAutoDown2.getText().toString();
                String valueUp2 = binding.etRfPowerAutoUp2.getText().toString();

                String valueDown5 = binding.etRfPowerAutoDown5.getText().toString();
                String valueUp5 = binding.etRfPowerAutoUp5.getText().toString();

                if (!TextUtils.isEmpty(valueDown2) && !TextUtils.isEmpty(valueUp2) && !TextUtils.isEmpty(valueDown5) && !TextUtils.isEmpty(valueUp5)) {
                    // not available for soft switch
                    sensorDevice.writeRfConfig(RFConfig.RfPower(RFConfig.RfPower.AUTO(Integer.parseInt(valueDown2), Integer.parseInt(valueUp2), Integer.parseInt(valueDown5), Integer.parseInt(valueUp5))));
                }
            }
        });

        binding.btnReadRfConfig2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.readRfConfig2();
            }
        });

        binding.btnWritePower2g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = binding.etPower2g.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    int valueInt = Integer.parseInt(value);
                    if (valueInt >= 15 && valueInt <= 28) {
                        // not available for hard switch
                        sensorDevice.writeRfConfig2(RFConfig2.Power2G(valueInt));
                    }
                }
            }
        });

        binding.btnWritePower5g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = binding.etPower5g.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    int valueInt = Integer.parseInt(value);
                    if (valueInt >= 15 && valueInt <= 25) {
                        // not available for hard switch
                        sensorDevice.writeRfConfig2(RFConfig2.Power5G(valueInt));
                    }
                }
            }
        });

        binding.btnWriteApMac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = binding.etApMac.getText().toString();
                if (!TextUtils.isEmpty(value)) {
                    if (isSoftSwitch())
                        sensorDevice.writeRfConfig2(RFConfig2.ApMac(value));
                    else
                        sensorDevice.writeRfConfig(RFConfig.ApMac(value));
                }
            }
        });

        binding.btnWriteBangswitch2g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSoftSwitch())
                    sensorDevice.writeRfConfig2(RFConfig2.BandSwitch("2G"));
                else
                    sensorDevice.writeRfConfig(RFConfig.BandSwitch("2G"));
            }
        });

        binding.btnWriteBangswitch5g.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSoftSwitch())
                    sensorDevice.writeRfConfig2(RFConfig2.BandSwitch("5G"));
                else
                    sensorDevice.writeRfConfig(RFConfig.BandSwitch("5G"));
            }
        });

        binding.swBanghop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isSoftSwitch())
                    sensorDevice.writeRfConfig2(RFConfig2.BandHop(isChecked ? 1 : 0));
                else
                    sensorDevice.writeRfConfig(RFConfig.BandHop(isChecked ? 1 : 0));
            }
        });

        String[] upDownRatioArray = new String[RFConfig2.UP_DOWN_RATIO.values().length];
        for (int i = 0; i<upDownRatioArray.length; i++) {
            upDownRatioArray[i] = RFConfig2.UP_DOWN_RATIO.values()[i].value;
        }
        ArrayAdapter<String> upDownRatioAdapter = new ArrayAdapter<String>(this, R.layout.item_select, upDownRatioArray);
        upDownRatioAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spUpDownRatio.setAdapter(upDownRatioAdapter);
        binding.spUpDownRatio.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSoftSwitch())
                    sensorDevice.writeRfConfig2(RFConfig2.UpDownRatio(RFConfig2.UP_DOWN_RATIO.values()[position]));
                else
                    sensorDevice.writeRfConfig(RFConfig.UpDownRatio(RFConfig2.UP_DOWN_RATIO.values()[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String[] bandwidthArray = new String[RFConfig2.BANDWIDTH.values().length];
        for (int i = 0; i<bandwidthArray.length; i++) {
            bandwidthArray[i] = RFConfig2.BANDWIDTH.values()[i].value;
        }
        ArrayAdapter<String> bandwidthAdapter = new ArrayAdapter<String>(this, R.layout.item_select, bandwidthArray);
        bandwidthAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spBandwidth.setAdapter(bandwidthAdapter);
        binding.spBandwidth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSoftSwitch())
                    sensorDevice.writeRfConfig2(RFConfig2.BandWidth(RFConfig2.BANDWIDTH.values()[position]));
                else
                    sensorDevice.writeRfConfig(RFConfig.BandWidth(RFConfig2.BANDWIDTH.values()[position]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        binding.btnPairDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.pairDevice(0);
            }
        });

        binding.btnCalibrate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calibrate calibrate = new Calibrate();
                calibrate.cal_offset = 1;
                sensorDevice.writeCalibrate(calibrate);
            }
        });

        binding.btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.switch201VideoMode(videoMode201++ % 2);
            }
        });

        binding.btnReadVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorDevice.readVersion();
            }
        });

        binding.btnInfraredConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChuanYunActivity.this, InfraredActivity.class);
                startActivity(intent);
            }
        });
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

    private boolean isSoftSwitch() {
        return binding.swIsSoftSwitch.isChecked();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorDevice.removeListener(sensorDeviceListener);
    }

    private final SensorDevice.SensorDeviceListener sensorDeviceListener = new SensorDevice.SensorDeviceListener() {
        @Override
        public void onStatus(Status status) {
            handler.post(() -> {
                binding.tvStatus.setText("RECEIVE: " + status.toString() + "\n");
            });
        }

        @Override
        public void onPairResponse(PairResponse pairResponse) {
            handler.post(() -> {
                binding.tvOthers.setText("RECEIVE: " + pairResponse.toString() + "\n");
            });
        }

        @Override
        public void onSbus(Sbus sbus) {
            handler.post(() -> {
                binding.tvSbus.setText("RECEIVE: " + sbus.toString() + "\n");
            });
        }

        @Override
        public void onCalibrate(Calibrate calibrate) {
//            Calibrate.cal_offset:
//            0 : 不动作。
//            1 : 开始校准。
//            2 : 校准中，请勿触碰摇杆和波轮。
//            3 : 校准完成。
            handler.post(() -> {
                binding.tvOthers.setText("RECEIVE: " + calibrate.toString() + "\n");
            });
        }

        @Override
        public void onRfConfig(RFConfig rfConfig) {
            handler.post(() -> {
                binding.tvOthers.setText("RECEIVE: " + rfConfig.toString() + "\n");
            });
        }

        @Override
        public void onRfConfig2(RFConfig2 rfConfig2) {
            handler.post(() -> {
                binding.tvOthers.setText("RECEIVE: " + rfConfig2.toString() + "\n");
            });
        }

        @Override
        public void onVersion(Version version) {
            handler.post(() -> {
                binding.tvOthers.setText("RECEIVE: " + version.toString() + "\n");
            });
        }

        @Override
        public void onInfraredConfig(InfraredConfig infraredConfig) {

        }
    };
}