package com.coolfly.demo;

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

import static com.coolfly.demo.utils.Constants.SP_NAME;

public class RadioTestActivity extends AppCompatActivity {

    private static final String PREF_SELECTED_BAND = "radio_test_selected_band";
    private static final String PREF_FREQUENCY_INDEX = "radio_test_frequency_index";
    private static final String PREF_POWER = "radio_test_power";

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private ActivityRadioTestBinding binding;
    
    private final String[] bands = {"1G", "2G", "5G"};
    private int selectedBandIndex = 0;
    
    // 频率相关变量
    private long[] freq = null;
    private String[] frequencyOptions = null;
    private int selectedFrequencyIndex = 0;
    
    // 设置操作的状态管理
    private enum SetStep {
        SET_BAND_MODE,
        SET_BAND,
        SET_CHANNEL_MODE,
        SET_CHANNEL,
        SET_POWER_AUTO,
        SET_POWER,
        START_PAIR,
        COMPLETED
    }
    
    // 重置操作的状态管理
    private enum ResetStep {
        SET_BAND_MODE,
        SET_CHANNEL_MODE,
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
        // 设置频段选择器
        ArrayAdapter<String> bandAdapter = new ArrayAdapter<>(this, R.layout.item_select, bands);
        bandAdapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spBand.setAdapter(bandAdapter);
        
        binding.spBand.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBandIndex = position;
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
            binding.spFrequency.setAdapter(emptyAdapter);
            binding.spFrequency.setEnabled(false);
        } else {
            // 设置频率数据
            ArrayAdapter<String> frequencyAdapter = new ArrayAdapter<>(this, R.layout.item_select, frequencyOptions);
            frequencyAdapter.setDropDownViewResource(R.layout.item_dropdown);
            binding.spFrequency.setAdapter(frequencyAdapter);
            binding.spFrequency.setEnabled(true);
            
            binding.spFrequency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedFrequencyIndex = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
    }

    private void loadSavedSettings() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        selectedBandIndex = sp.getInt(PREF_SELECTED_BAND, 0);
        selectedFrequencyIndex = sp.getInt(PREF_FREQUENCY_INDEX, 0);
        String power = sp.getString(PREF_POWER, "");
        
        binding.spBand.setSelection(selectedBandIndex);
        // 频率选择器的选择将在频率数据加载后设置
        binding.etPower.setText(power);
    }

    private void saveSettings() {
        SharedPreferences sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putInt(PREF_SELECTED_BAND, selectedBandIndex);
        editor.putInt(PREF_FREQUENCY_INDEX, selectedFrequencyIndex);
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
        currentSetStep = SetStep.SET_BAND_MODE;
        binding.btnSet.setEnabled(false);
        binding.btnReset.setEnabled(false);
        
        appendStatus("开始设置流程...");
        appendStatus("步骤1: 设置频段模式为手动");
        
        protocolHelper.ar8030SetBandMode(false);
    }

    private void onResetClick() {
        if (isSettingInProgress || isResetInProgress) {
            Toast.makeText(this, "操作进行中，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 开始重置流程
        isResetInProgress = true;
        currentResetStep = ResetStep.SET_BAND_MODE;
        binding.btnSet.setEnabled(false);
        binding.btnReset.setEnabled(false);
        
        appendStatus("开始重置流程...");
        appendStatus("步骤1: 设置频段模式为自动");
        
        protocolHelper.ar8030SetBandMode(true);
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
            frequencyValue = selectedFrequencyIndex; // 使用选择的索引作为频率值
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

    private void handleSetStepSuccess() {
        switch (currentSetStep) {
            case SET_BAND_MODE:
                appendStatus("✓ 频段模式设置成功");
                appendStatus("步骤2: 设置频段为 " + bands[selectedBandIndex]);
                currentSetStep = SetStep.SET_BAND;
                protocolHelper.ar8030SetBand(selectedBandIndex);
                break;
                
            case SET_BAND:
                appendStatus("✓ 频段设置成功");
                appendStatus("步骤3: 设置信道模式为手动");
                currentSetStep = SetStep.SET_CHANNEL_MODE;
                protocolHelper.ar8030SetChanMode(false);
                break;
                
            case SET_CHANNEL_MODE:
                appendStatus("✓ 信道模式设置成功");
                appendStatus("步骤4: 设置信道频率");
                currentSetStep = SetStep.SET_CHANNEL;
                // 这里使用频率值作为信道索引，实际项目中可能需要转换
                protocolHelper.ar8030SetChan(frequencyValue);
                break;
                
            case SET_CHANNEL:
                appendStatus("✓ 信道设置成功");
                appendStatus("步骤5: 设置功率为手动模式");
                currentSetStep = SetStep.SET_POWER_AUTO;
                protocolHelper.ar8030SetPwrAuto(false, false);
                break;
                
            case SET_POWER_AUTO:
                appendStatus("✓ 功率模式设置成功");
                appendStatus("步骤6: 设置功率为 " + powerValue + " dBm");
                currentSetStep = SetStep.SET_POWER;
                protocolHelper.ar8030SetPwr(powerValue, false);
                break;
                
            case SET_POWER:
                appendStatus("✓ 功率设置成功");
                appendStatus("步骤7: 检查对频状态");
                
                // 检查是否已经对频
                if (protocolHelper.ar8030IsPaired(0)) {
                    appendStatus("✓ 已经对频，跳过对频步骤");
                    appendStatus("🎉 所有设置已完成！");
                    currentSetStep = SetStep.COMPLETED;
                    finishSetOperation(true);
                } else {
                    appendStatus("未对频，开始对频...");
                    currentSetStep = SetStep.START_PAIR;
                    protocolHelper.ar8030StartPair();
                }
                break;
                
            case START_PAIR:
                // 对频是异步操作，启动后等待onPairSuccess或onPairTimeOut回调
                appendStatus("✓ 对频命令已发送，等待对频完成...");
                break;
        }
    }

    private void handleResetStepSuccess() {
        switch (currentResetStep) {
            case SET_BAND_MODE:
                appendStatus("✓ 频段模式重置成功");
                appendStatus("步骤2: 设置信道模式为自动");
                currentResetStep = ResetStep.SET_CHANNEL_MODE;
                protocolHelper.ar8030SetChanMode(true);
                break;
                
            case SET_CHANNEL_MODE:
                appendStatus("✓ 信道模式重置成功");
                appendStatus("步骤3: 重置功率设置为自动模式");
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
        isSettingInProgress = false;
        binding.btnSet.setEnabled(true);
        binding.btnReset.setEnabled(true);
        
        if (success) {
            Toast.makeText(this, "设置完成", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "设置失败", Toast.LENGTH_SHORT).show();
            appendStatus("❌ 设置操作失败");
        }
    }

    private void finishResetOperation(boolean success) {
        isResetInProgress = false;
        binding.btnSet.setEnabled(true);
        binding.btnReset.setEnabled(true);
        
        if (success) {
            Toast.makeText(this, "重置完成", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "重置失败", Toast.LENGTH_SHORT).show();
            appendStatus("❌ 重置操作失败");
        }
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
                        if (selectedFrequencyIndex < frequencyOptions.length) {
                            binding.spFrequency.setSelection(selectedFrequencyIndex);
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
        public void onWrite(byte[] bytes) {
            // 数据写入回调
        }

        @Override
        public void onPairTimeOut(DEVICE_TYPE deviceType, int i) {
            // 配对超时
            runOnUiThread(() -> {
                appendStatus("❌ 对频超时 (slot: " + i + ")");
                
                // 如果正在执行设置流程的对频步骤，结束设置流程
                if (isSettingInProgress && currentSetStep == SetStep.START_PAIR) {
                    appendStatus("❌ 设置失败: 对频超时");
                    finishSetOperation(false);
                }
            });
        }

        @Override
        public void onPairSuccess(DEVICE_TYPE deviceType, int i) {
            // 配对成功
            runOnUiThread(() -> {
                appendStatus("✓ 对频成功 (slot: " + i + ")");
                
                // 如果正在执行设置流程的对频步骤，继续下一步
                if (isSettingInProgress && currentSetStep == SetStep.START_PAIR) {
                    appendStatus("🎉 所有设置已完成！");
                    currentSetStep = SetStep.COMPLETED;
                    finishSetOperation(true);
                }
            });
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
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
            // 这是主要的回调方法，处理所有无线电设置的结果
            runOnUiThread(() -> {
                String message = "回调: " + radioType + ", 成功: " + isSuccess;
                if (!isSuccess) {
                    message += ", 错误: " + errMessage;
                }
                appendStatus(message);
                
                if (isSuccess) {
                    // 根据当前操作继续下一步
                    if (isSettingInProgress) {
                        handleSetStepSuccess();
                    } else if (isResetInProgress) {
                        handleResetStepSuccess();
                    }
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
            });
        }
    };
}
