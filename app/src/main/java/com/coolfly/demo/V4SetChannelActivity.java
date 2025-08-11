package com.coolfly.demo;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4SetChannelBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BandInfo8030;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.ChanInfo8030;

public class V4SetChannelActivity extends AppCompatActivity {

    private ActivityV4SetChannelBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    private long[] freq = null;
    private int selectedChannelIndex = 0;

    private final String[] band = {"1G", "2G", "5G"};
    private int selectedBandIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4SetChannelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.tvReadBand.setOnClickListener(v -> {
            protocolHelper.ar8030GetBandInfo(false);
        });

        binding.tvReadChannel.setOnClickListener(v -> {
            protocolHelper.ar8030GetChannelInfo(false);
        });

        binding.tvSetBand.setOnClickListener(v -> {
            if (!binding.swBandAuto.isEnabled()) {
                Toast.makeText(V4SetChannelActivity.this, "Read band first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.swBandAuto.isChecked()) {
                Toast.makeText(V4SetChannelActivity.this, "Turn off band auto mode first", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(V4SetChannelActivity.this)
                    .setTitle("set band")
                    .setItems(band, (dialog, which) -> {
                        selectedBandIndex = which;
                        protocolHelper.ar8030SetBand(selectedBandIndex);
                        // Set dev at the same time
                        protocolHelper.ar8030SetBandRemote(binding.swBandAuto.isChecked(), binding.swBandAuto.isChecked()? 0: selectedBandIndex, 0);
                    }).create().show();
        });

        binding.tvSetChannel.setOnClickListener(v -> {
            if (!binding.swChanAuto.isEnabled()) {
                Toast.makeText(V4SetChannelActivity.this, "Read channel first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (binding.swChanAuto.isChecked()) {
                Toast.makeText(V4SetChannelActivity.this, "Turn off channel auto mode first", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] channels = new String[freq.length];
            for (int i = 0; i<freq.length; i++) {
                channels[i] = i + " -- " + freq[i];
            }
            new AlertDialog.Builder(V4SetChannelActivity.this)
                    .setTitle("set channel")
                    .setItems(channels, (dialog, which) -> {
                        selectedChannelIndex = which;
                        protocolHelper.ar8030SetChan(selectedChannelIndex);
                        // Set dev at the same time
                        protocolHelper.ar8030SetChanRemote(binding.swChanAuto.isChecked(), binding.swChanAuto.isChecked()? 0: selectedChannelIndex, 0);
                    }).create().show();
        });

        binding.tvSetBandMinidbLocal.setOnClickListener(v -> {
            String[] channels = new String[3];
            channels[0] = "2G";
            channels[1] = "5G";
            channels[2] = "Auto";
            new AlertDialog.Builder(V4SetChannelActivity.this)
                    .setTitle("set band in local minidb")
                    .setItems(channels, (dialog, which) -> {
                        boolean isAuto = which == 2;
                        int bandIndex = which + 1;
                        protocolHelper.ar8030SetBandMiniDb(isAuto, bandIndex, false);
                    }).create().show();
        });

        binding.tvSetBandMinidbPeer.setOnClickListener(v -> {
            String[] channels = new String[3];
            channels[0] = "2G";
            channels[1] = "5G";
            channels[2] = "Auto";
            new AlertDialog.Builder(V4SetChannelActivity.this)
                    .setTitle("set band in peer minidb")
                    .setItems(channels, (dialog, which) -> {
                        boolean isAuto = which == 2;
                        int bandIndex = which + 1;
                        protocolHelper.ar8030SetBandMiniDb(isAuto, bandIndex, true);
                    }).create().show();
        });

        protocolHelper.addListener(protocolListener);
    }

    private CompoundButton.OnCheckedChangeListener onChannelCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            protocolHelper.ar8030SetChanMode(isChecked);
            // Set dev at the same time
            protocolHelper.ar8030SetChanRemote(isChecked, isChecked? 0: selectedChannelIndex, 0);
        }
    };

    private CompoundButton.OnCheckedChangeListener onBandCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            protocolHelper.ar8030SetBandMode(isChecked);
            // Set dev at the same time
            protocolHelper.ar8030SetBandRemote(isChecked, isChecked? 0: selectedBandIndex, 0);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
    }

    @Keep
    private ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof ChanInfo8030) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // AR8030 channel info
                        ChanInfo8030 chanInfo8030 = (ChanInfo8030) baseFlyPacket;
                        binding.tvChannelInfo.setText(chanInfo8030.toString());
                        // autoMode 1：自适应 0：手动
                        if (chanInfo8030.autoMode == 1) {
                            binding.swChanAuto.setOnCheckedChangeListener(null);
                            binding.swChanAuto.setEnabled(true);
                            binding.swChanAuto.setChecked(true);
                            binding.swChanAuto.setOnCheckedChangeListener(onChannelCheckedChangeListener);
                        } else if (chanInfo8030.autoMode == 0) {
                            binding.swChanAuto.setOnCheckedChangeListener(null);
                            binding.swChanAuto.setEnabled(true);
                            binding.swChanAuto.setChecked(false);
                            binding.swChanAuto.setOnCheckedChangeListener(onChannelCheckedChangeListener);
                        }
                        freq = chanInfo8030.freq;
                        selectedChannelIndex = chanInfo8030.workChan;
                    }
                });
            } else if (baseFlyPacket instanceof BandInfo8030) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // AR8030 band info
                        BandInfo8030 bandInfo8030 = (BandInfo8030) baseFlyPacket;
                        binding.tvBandInfo.setText(bandInfo8030.toString());
                        // bandMode 1：自适应 0：手动
                        if (bandInfo8030.bandMode == 1) {
                            binding.swBandAuto.setOnCheckedChangeListener(null);
                            binding.swBandAuto.setEnabled(true);
                            binding.swBandAuto.setChecked(true);
                            binding.swBandAuto.setOnCheckedChangeListener(onBandCheckedChangeListener);
                        } else if (bandInfo8030.bandMode == 0) {
                            binding.swBandAuto.setOnCheckedChangeListener(null);
                            binding.swBandAuto.setEnabled(true);
                            binding.swBandAuto.setChecked(false);
                            binding.swBandAuto.setOnCheckedChangeListener(onBandCheckedChangeListener);
                        }
                        selectedBandIndex = bandInfo8030.workBand;
                    }
                });
            }
        }

        @Override
        public void onWrite(byte[] bytes) {

        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int i) {

        }

        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // Now only for 8030
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {
            // Now only for 8030
        }

        @Override
        public void onSetRadio(com.fly.station.prorocol.DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, boolean isRemote) {
            // Now only for 8030
            binding.tvCallback.append("set radio: radioType = " + radioType + ", isSuccess = " + isSuccess + "\n");
            // 自动滚动到底部
            binding.scrollView.fullScroll(View.FOCUS_DOWN);
        }
    };

}