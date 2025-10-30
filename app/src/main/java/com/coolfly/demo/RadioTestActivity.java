package com.coolfly.demo;

import static com.coolfly.demo.utils.Constants.SP_NAME;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityRadioTestBinding;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.ChanInfo8030;
import com.fly.station.prorocol.bean.RcStatus8030;
import com.fly.station.prorocol.bean.Throughput8030;

public class RadioTestActivity extends AppCompatActivity {

    private static final String PREF_RX_SELECTED_BAND = "radio_test_rx_selected_band";
    private static final String PREF_RX_FREQUENCY_INDEX = "radio_test_rx_frequency_index";
    private static final String PREF_TX_SELECTED_BAND = "radio_test_tx_selected_band";
    private static final String PREF_TX_FREQUENCY_INDEX = "radio_test_tx_frequency_index";
    private static final String PREF_POWER = "radio_test_power";

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private ActivityRadioTestBinding binding;
    
    private final String[] bands = {"1G", "2G", "5G"};
    
    // RX (Local) 相关变量
    private int selectedRxBandIndex = 0;
    private int selectedRxFrequencyIndex = 0;
    
    // TX (Remote) 相关变量
    private int selectedTxBandIndex = 0;
    private int selectedTxFrequencyIndex = 0;
    
    // 频率相关变量
    private long[] freq = null;
    private String[] frequencyOptions = null;

    private static final int INTERVAL = 500; // 500ms间隔
    
    // 设置操作的状态管理
    private enum SetStep {
        SET_RX_BAND_MODE,
        SET_RX_BAND,
        SET_RX_CHANNEL_MODE,
        SET_RX_CHANNEL,
        SET_TX_BAND,
        SET_TX_CHANNEL,
        SET_POWER_AUTO,
        SET_POWER,
        START_PAIR,
        COMPLETED
    }
    
    // 重置操作的状态管理
    private enum ResetStep {
        SET_RX_BAND_MODE,
        SET_RX_CHANNEL_MODE,
        SET_TX_BAND_RESET,
        SET_TX_CHANNEL_RESET,
        SET_POWER_AUTO,
        COMPLETED
    }
    
    private SetStep currentSetStep;
    private ResetStep currentResetStep;
    private boolean isSettingInProgress = false;
    private boolean isResetInProgress = false;
    
    private int frequencyValue;
    private int powerValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRadioTestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupSpinner();
        loadSavedSettings();
        setupClickListeners();
        
        protocolHelper.addListener(protocolListener);
        
        // 获取频率信息
        protocolHelper.ar8030GetChannelInfo(false);
    }



    private void setupSpinner() {
        // 设置 RX 频段选择器
        ArrayAdapter<String> rxBandAdapter = new ArrayAdapter<>(this, R.layout.item_select, bands);
        rxBandAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spRxBand.setAdapter(rxBandAdapter);
        
        binding.spRxBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRxBandIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        // 设置 TX 频段选择器
        ArrayAdapter<String> txBandAdapter = new ArrayAdapter<>(this, R.layout.item_select, bands);
        txBandAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spTxBand.setAdapter(txBandAdapter);
        
        binding.spTxBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedTxBandIndex = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        
        // 设置频率选择器（初始时为空，等待获取频率数据）
        setupFrequencySpinner();
    }
    
    private void setupFrequencySpinner() {
        if (frequencyOptions == null) {
            // 如果还没有频率数据，设置空适配器
            ArrayAdapter<String> emptyAdapter = new ArrayAdapter<>(this, R.layout.item_select, new String[]{"正在加载频率信息..."});
            emptyAdapter.setDropDownViewResource(R.layout.item_dropdown);
            
            // RX 频率选择器
            binding.spRxFrequency.setAdapter(emptyAdapter);
            binding.spRxFrequency.setEnabled(false);
            
            // TX 频率选择器
            binding.spTxFrequency.setAdapter(emptyAdapter);
            binding.spTxFrequency.setEnabled(false);
        } else {
            // 设置 RX 频率数据
            ArrayAdapter<String> rxFrequencyAdapter = new ArrayAdapter<>(this, R.layout.item_select, frequencyOptions);
            rxFrequencyAdapter.setDropDownViewResource(R.layout.item_dropdown);
            binding.spRxFrequency.setAdapter(rxFrequencyAdapter);
            binding.spRxFrequency.setEnabled(true);
            
            binding.spRxFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedRxFrequencyIndex = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            
            // 设置 TX 频率数据
            ArrayAdapter<String> txFrequencyAdapter = new ArrayAdapter<>(this, R.layout.item_select, frequencyOptions);
            txFrequencyAdapter.setDropDownViewResource(R.layout.item_dropdown);
            binding.spTxFrequency.setAdapter(txFrequencyAdapter);
            binding.spTxFrequency.setEnabled(true);
            
            binding.spTxFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedTxFrequencyIndex = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void loadSavedSettings() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        selectedRxBandIndex = sp.getInt(PREF_RX_SELECTED_BAND, 0);
        selectedRxFrequencyIndex = sp.getInt(PREF_RX_FREQUENCY_INDEX, 0);
        selectedTxBandIndex = sp.getInt(PREF_TX_SELECTED_BAND, 0);
        selectedTxFrequencyIndex = sp.getInt(PREF_TX_FREQUENCY_INDEX, 0);
        String power = sp.getString(PREF_POWER, "");
        
        binding.spRxBand.setSelection(selectedRxBandIndex);
        binding.spTxBand.setSelection(selectedTxBandIndex);
        // 频率选择器的选择将在频率数据加载后设置
        binding.etPower.setText(power);
    }

    private void saveSettings() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_RX_SELECTED_BAND, selectedRxBandIndex);
        editor.putInt(PREF_RX_FREQUENCY_INDEX, selectedRxFrequencyIndex);
        editor.putInt(PREF_TX_SELECTED_BAND, selectedTxBandIndex);
        editor.putInt(PREF_TX_FREQUENCY_INDEX, selectedTxFrequencyIndex);
        editor.putString(PREF_POWER, binding.etPower.getText().toString());
        editor.apply();
    }

    private void setupClickListeners() {
        binding.btnSet.setOnClickListener(v -> onSetClick());
        binding.btnReset.setOnClickListener(v -> onResetClick());
    }

    private void onSetClick() {
        if (isSettingInProgress || isResetInProgress) {
            Toast.makeText(this, "操作进行中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!validateInputs()) {
            return;
        }
        
        saveSettings();
        
        // 开始设置流程
        isSettingInProgress = true;
        currentSetStep = SetStep.SET_RX_BAND_MODE;
        binding.btnSet.setEnabled(false);
        binding.btnReset.setEnabled(false);
        
        appendStatus("开始设置流程...");
        appendStatus("步骤1: 设置RX频段模式为手动");
        
        protocolHelper.ar8030SetBandMode(false, false);
    }

    private void onResetClick() {
        if (isSettingInProgress || isResetInProgress) {
            Toast.makeText(this, "操作进行中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 开始重置流程
        isResetInProgress = true;
        currentResetStep = ResetStep.SET_RX_BAND_MODE;
        binding.btnSet.setEnabled(false);
        binding.btnReset.setEnabled(false);
        
        appendStatus("开始重置流程...");
        appendStatus("步骤1: 设置RX频段模式为自动");
        
        protocolHelper.ar8030SetBandMode(true, false);
    }

    private boolean validateInputs() {
        String powerStr = binding.etPower.getText().toString().trim();
        
        // 检查频率数据是否已加载
        if (freq == null || frequencyOptions == null) {
            Toast.makeText(this, "频率信息尚未加载完成，请稍候", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (TextUtils.isEmpty(powerStr)) {
            Toast.makeText(this, "请输入功率", Toast.LENGTH_SHORT).show();
            binding.etPower.requestFocus();
            return false;
        }
        
        try {
            frequencyValue = selectedRxFrequencyIndex; // 使用RX选择的索引作为频率值
            powerValue = Integer.parseInt(powerStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的功率数字", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        return true;
    }

    private void appendStatus(String message) {
        runOnUiThread(() -> {
            String currentText = binding.tvStatus.getText().toString();
            if (currentText.equals("状态信息将在此显示")) {
                binding.tvStatus.setText(message);
            } else {
                binding.tvStatus.setText(currentText + "\n" + message);
            }
            
            // 滚动到底部
            binding.svStatus.post(() -> binding.svStatus.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void updateGainDisplay(int localGainA, int localGainB, int peerGainA, int peerGainB) {
        runOnUiThread(() -> {
            binding.tvLocalGainA.setText(String.valueOf(localGainA));
            binding.tvLocalGainB.setText(String.valueOf(localGainB));
            binding.tvPeerGainA.setText(String.valueOf(peerGainA));
            binding.tvPeerGainB.setText(String.valueOf(peerGainB));
        });
    }

    // 在非UI线程执行
    private void handleSetStepSuccess() throws InterruptedException {
        switch (currentSetStep) {
            case SET_RX_BAND_MODE:
                appendStatus("✓ RX频段模式设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤2: 设置RX频段为 " + bands[selectedRxBandIndex]);
                currentSetStep = SetStep.SET_RX_BAND;
                protocolHelper.ar8030SetBand(selectedRxBandIndex, false);
                break;
                
            case SET_RX_BAND:
                appendStatus("✓ RX频段设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤3: 设置RX信道模式为手动");
                currentSetStep = SetStep.SET_RX_CHANNEL_MODE;
                protocolHelper.ar8030SetChanMode(false, false);
                break;
                
            case SET_RX_CHANNEL_MODE:
                appendStatus("✓ RX信道模式设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤4: 设置RX信道频率");
                currentSetStep = SetStep.SET_RX_CHANNEL;
                protocolHelper.ar8030SetChan(selectedRxFrequencyIndex, false);
                break;
                
            case SET_RX_CHANNEL:
                appendStatus("✓ RX信道设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤5: 设置TX频段为 " + bands[selectedTxBandIndex]);
                currentSetStep = SetStep.SET_TX_BAND;
                protocolHelper.ar8030SetBandRemote(false, selectedTxBandIndex, 0);
                break;
                
            case SET_TX_BAND:
                appendStatus("✓ TX频段设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤6: 设置TX信道频率");
                currentSetStep = SetStep.SET_TX_CHANNEL;
                protocolHelper.ar8030SetChanRemote(false, selectedTxFrequencyIndex, 0);
                break;
                
            case SET_TX_CHANNEL:
                appendStatus("✓ TX信道设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤7: 设置功率为手动模式");
                currentSetStep = SetStep.SET_POWER_AUTO;
                protocolHelper.ar8030SetPwrAuto(false, false);
                break;
                
            case SET_POWER_AUTO:
                appendStatus("✓ 功率模式设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤8: 设置功率为 " + powerValue + " dBm");
                currentSetStep = SetStep.SET_POWER;
                protocolHelper.ar8030SetPwr(powerValue, false);
                break;
                
            case SET_POWER:
                appendStatus("✓ 功率设置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤9: 检查对频状态");
                
                // 检查是否已经对频
                if (protocolHelper.ar8030IsPaired(0)) {
                    appendStatus("✓ 已经对频，跳过对频步骤");
                    appendStatus("🎉 所有设置已完成！");
                    currentSetStep = SetStep.COMPLETED;
                    finishSetOperation(true);
                } else {
                    appendStatus("未对频，开始对频...");
                    Thread.sleep(INTERVAL);
                    currentSetStep = SetStep.START_PAIR;
                    // ar8030StartPair and ar8030StopPair should be called in non-UI thread
                    protocolHelper.ar8030StartPair();
                }
                break;
                
            case START_PAIR:
                // 对频是异步操作，启动后等待onPairSuccess或onPairTimeOut回调
                appendStatus("✓ 对频命令已发送，等待对频完成...");
                break;
        }
    }

    // 在非UI线程执行
    private void handleResetStepSuccess() throws InterruptedException {
        switch (currentResetStep) {
            case SET_RX_BAND_MODE:
                appendStatus("✓ RX频段模式重置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤2: 设置RX信道模式为自动");
                currentResetStep = ResetStep.SET_RX_CHANNEL_MODE;
                protocolHelper.ar8030SetChanMode(true, false);
                break;
                
            case SET_RX_CHANNEL_MODE:
                appendStatus("✓ RX信道模式重置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤3: 设置TX频段为自动");
                currentResetStep = ResetStep.SET_TX_BAND_RESET;
                protocolHelper.ar8030SetBandRemote(true, 0, 0);
                break;
                
            case SET_TX_BAND_RESET:
                appendStatus("✓ TX频段重置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤4: 设置TX信道为自动");
                currentResetStep = ResetStep.SET_TX_CHANNEL_RESET;
                protocolHelper.ar8030SetChanRemote(true, 0, 0);
                break;
                
            case SET_TX_CHANNEL_RESET:
                appendStatus("✓ TX信道重置成功");
                Thread.sleep(INTERVAL);
                appendStatus("步骤5: 重置功率设置为自动模式");
                currentResetStep = ResetStep.SET_POWER_AUTO;
                protocolHelper.ar8030SetPwrAuto(true, false);
                break;
                
            case SET_POWER_AUTO:
                appendStatus("✓ 功率重置成功");
                appendStatus("🎉 所有重置已完成！");
                currentResetStep = ResetStep.COMPLETED;
                finishResetOperation(true);
                break;
        }
    }

    private void finishSetOperation(boolean success) {
        runOnUiThread(() -> {
            isSettingInProgress = false;
            binding.btnSet.setEnabled(true);
            binding.btnReset.setEnabled(true);

            if (success) {
                Toast.makeText(this, "设置完成", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show();
                appendStatus("❌ 设置操作失败");
            }
        });
    }

    private void finishResetOperation(boolean success) {
        runOnUiThread(() -> {
            isResetInProgress = false;
            binding.btnSet.setEnabled(true);
            binding.btnReset.setEnabled(true);

            if (success) {
                Toast.makeText(this, "重置完成", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show();
                appendStatus("❌ 重置操作失败");
            }
        });
    }

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
                runOnUiThread(() -> {
                    // 处理频道信息
                    ChanInfo8030 chanInfo8030 = (ChanInfo8030) baseFlyPacket;
                    freq = chanInfo8030.freq;
                    
                    // 构建频率选项列表，格式：索引 -- 频率值
                    if (freq != null && freq.length > 0) {
                        frequencyOptions = new String[freq.length];
                        for (int i = 0; i < freq.length; i++) {
                            frequencyOptions[i] = i + " -- " + freq[i];
                        }
                        
                        // 更新频率选择器
                        setupFrequencySpinner();
                        
                        // 如果有保存的频率索引，恢复选择
                        if (selectedRxFrequencyIndex < frequencyOptions.length) {
                            binding.spRxFrequency.setSelection(selectedRxFrequencyIndex);
                        }
                        if (selectedTxFrequencyIndex < frequencyOptions.length) {
                            binding.spTxFrequency.setSelection(selectedTxFrequencyIndex);
                        }
                        
                        appendStatus("✓ 频率信息加载完成，共 " + freq.length + " 个频点");
                    } else {
                        appendStatus("❌ 获取频率信息失败");
                    }
                });
            } else if (baseFlyPacket instanceof RcStatus8030) {
                // 处理RcStatus8030数据，显示增益值
                RcStatus8030 rcStatus = (RcStatus8030) baseFlyPacket;
                updateGainDisplay(rcStatus.localGainA, rcStatus.localGainB, 
                                rcStatus.peerGainA, rcStatus.peerGainB);
            }
        }

        @Override
        public int onWrite(byte[] bytes) {
            return 0;
        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) {
            // 配对超时
            appendStatus("❌ 对频超时 (slot: " + i + ")");

            // 如果正在执行设置流程的对频步骤，结束设置流程
            if (isSettingInProgress && currentSetStep == SetStep.START_PAIR) {
                appendStatus("❌ 设置失败: 对频超时");
                finishSetOperation(false);
            }
        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) {
            // 配对成功
            appendStatus("✓ 对频成功 (slot: " + i + ")");

            // 如果正在执行设置流程的对频步骤，继续下一步
            if (isSettingInProgress && currentSetStep == SetStep.START_PAIR) {
                appendStatus("🎉 所有设置已完成！");
                currentSetStep = SetStep.COMPLETED;
                finishSetOperation(true);
            }
        }

        @Override
        public void onLinked(DEVICE_TYPE deviceType, int i) {
            // 连接成功
        }

        @Override
        public void onLinkLost(DEVICE_TYPE deviceType, int i) {
            // 连接丢失
        }

        @Override
        public void onConfigJson(@Nullable String jsonString, DEVICE_TYPE deviceType, boolean isRemote) {
            // 配置JSON回调
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // 设置配置JSON回调
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
            // 重置配置JSON回调
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {
            // 槽位MAC回调
        }

        @Override
        public void onThroughput(com.fly.station.prorocol.DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
            // 吞吐率
        }

        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // 这是主要的回调方法，处理所有无线电设置的结果
            String message = "回调: " + radioType + ", 成功: " + isSuccess;
            if (!isSuccess) {
                message += ", 错误: " + errMessage;
            }
            appendStatus(message);

            if (isSuccess) {
                // 根据当前操作继续下一步，延迟500ms执行
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (isSettingInProgress) {
                            try {
                                handleSetStepSuccess();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        } else if (isResetInProgress) {
                            try {
                                handleResetStepSuccess();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }).start();
            } else {
                // 操作失败，停止当前流程
                if (isSettingInProgress) {
                    appendStatus("❌ 设置失败: " + errMessage);
                    finishSetOperation(false);
                } else if (isResetInProgress) {
                    appendStatus("❌ 重置失败: " + errMessage);
                    finishResetOperation(false);
                }
            }
        }
    };
}
