package com.coolfly.demo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityMqttBinding;
import com.coolfly.demo.utils.Constants;
import com.fly.station.mqtt.MqttHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * MQTT连接和数据收发示例Activity
 * 支持配置broker地址、端口，多topic订阅和发送，支持明文和二进制数据
 */
public class MqttActivity extends AppCompatActivity {

    private ActivityMqttBinding binding;
    private MqttHelper mqttHelper;
    
    // 消息类型
    private enum MessageType {
        TEXT("明文"), BINARY("二进制");
        
        private final String displayName;
        
        MessageType(String displayName) {
            this.displayName = displayName;
        }
        
        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private MessageType currentMessageType = MessageType.TEXT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMqttBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取MqttHelper单例
        mqttHelper = MqttHelper.getInstance();
        
        initViews();
        setupEventListeners();
        
        // 检查并恢复连接状态
        restoreConnectionState();
    }

    private void initViews() {
        // 从SharedPreferences加载保存的配置
        loadSavedConfig();
        
        // 如果客户端ID为空，则生成默认值
        if (TextUtils.isEmpty(binding.etClientId.getText().toString().trim())) {
            binding.etClientId.setText("coolfly_demo_" + System.currentTimeMillis());
        }
        
        // 设置消息类型选择器
        List<MessageType> messageTypes = new ArrayList<>();
        messageTypes.add(MessageType.TEXT);
        messageTypes.add(MessageType.BINARY);
        
        ArrayAdapter<MessageType> adapter = new ArrayAdapter<>(this, R.layout.item_select, messageTypes);
        adapter.setDropDownViewResource(R.layout.item_dropdown);
        binding.spMessageType.setAdapter(adapter);
        binding.spMessageType.setSelection(0);
        
        // 初始状态
        updateConnectionState(false, "未连接");
        updateSubscribeButtonState();
        updatePublishButtonState();
    }

    private void setupEventListeners() {
        // MQTT连接/断开
        binding.btnConnect.setOnClickListener(v -> {
            if (mqttHelper != null && mqttHelper.isConnected()) {
                disconnectMqtt();
            } else {
                connectMqtt();
            }
        });
        
        // 订阅主题
        binding.btnSubscribe.setOnClickListener(v -> subscribeTopic());
        
        // 取消订阅
        binding.btnUnsubscribe.setOnClickListener(v -> unsubscribeTopic());
        
        // 发布消息
        binding.btnPublish.setOnClickListener(v -> publishMessage());
        
        // 清空消息显示
        binding.btnClear.setOnClickListener(v -> {
            binding.tvMessages.setText("");
        });
        
        // 消息类型选择
        binding.spMessageType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentMessageType = (MessageType) parent.getItemAtPosition(position);
                updateMessageInputHint();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        
        // 主题输入框变化监听
        binding.etTopic.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updateSubscribeButtonState();
                updatePublishButtonState();
            }
        });
        
        // 消息输入框变化监听
        binding.etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePublishButtonState();
            }
        });
        
        // 发布主题输入框变化监听
        binding.etPublishTopic.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                updatePublishButtonState();
            }
        });
    }

    private void connectMqtt() {
        String host = binding.etBrokerHost.getText().toString().trim();
        String portStr = binding.etBrokerPort.getText().toString().trim();
        String clientId = binding.etClientId.getText().toString().trim();
        String username = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(host)) {
            Toast.makeText(this, "请输入Broker地址", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(portStr)) {
            Toast.makeText(this, "请输入端口号", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(clientId)) {
            Toast.makeText(this, "请输入客户端ID", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port <= 0 || port > 65535) {
                throw new NumberFormatException("端口号超出范围");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "端口号格式错误", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建MQTT配置
        MqttHelper.MqttConfig config = new MqttHelper.MqttConfig(host, port);
        config.setClientId(clientId);
        
        if (!TextUtils.isEmpty(username)) {
            config.setUsername(username);
        }
        if (!TextUtils.isEmpty(password)) {
            config.setPassword(password);
        }
        
        // 初始化MQTT Helper
        mqttHelper.initialize(this, config);
        mqttHelper.setEventListener(mqttEventListener);
        mqttHelper.setAutoReconnect(true, 5000);
        
        // 开始连接
        updateConnectionState(false, "正在连接...");
        mqttHelper.connect();
    }

    private void disconnectMqtt() {
        if (mqttHelper != null) {
            mqttHelper.disconnect();
            updateSubscribedTopicsList();
        }
        updateConnectionState(false, "已断开");
    }

    private void subscribeTopic() {
        String topic = binding.etTopic.getText().toString().trim();
        String qosStr = binding.etQos.getText().toString().trim();
        
        if (TextUtils.isEmpty(topic)) {
            Toast.makeText(this, "请输入主题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否已订阅
        MqttHelper.ConnectionStatus status = mqttHelper.getConnectionStatus();
        if (status.getSubscribedTopics().contains(topic)) {
            Toast.makeText(this, "主题已订阅", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int qos;
        try {
            qos = Integer.parseInt(qosStr);
            if (qos < 0 || qos > 2) {
                throw new NumberFormatException("QoS超出范围");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "QoS格式错误（0-2）", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 创建主题监听器
        MqttHelper.MqttTopicListener listener = createTopicListener();
        
        // 订阅主题
        mqttHelper.subscribe(topic, qos, listener);
        updateSubscribedTopicsList();
        
        // 保存主题配置
        saveTopicConfig(topic, qos);
        
        Toast.makeText(this, "已订阅主题: " + topic, Toast.LENGTH_SHORT).show();
    }

    private void unsubscribeTopic() {
        String topic = binding.etTopic.getText().toString().trim();
        
        if (TextUtils.isEmpty(topic)) {
            Toast.makeText(this, "请输入主题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 检查是否已订阅
        MqttHelper.ConnectionStatus status = mqttHelper.getConnectionStatus();
        if (!status.getSubscribedTopics().contains(topic)) {
            Toast.makeText(this, "主题未订阅", Toast.LENGTH_SHORT).show();
            return;
        }
        
        mqttHelper.unsubscribe(topic);
        updateSubscribedTopicsList();
        
        Toast.makeText(this, "已取消订阅主题: " + topic, Toast.LENGTH_SHORT).show();
    }

    private void publishMessage() {
        String topic = binding.etPublishTopic.getText().toString().trim();
        String message = binding.etMessage.getText().toString();
        String qosStr = binding.etQos.getText().toString().trim();
        boolean retained = binding.cbRetained.isChecked();
        
        if (TextUtils.isEmpty(topic)) {
            Toast.makeText(this, "请输入发布主题", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int qos;
        try {
            qos = Integer.parseInt(qosStr);
            if (qos < 0 || qos > 2) {
                throw new NumberFormatException("QoS超出范围");
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "QoS格式错误（0-2）", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentMessageType == MessageType.TEXT) {
            // 发送明文消息
            mqttHelper.publishText(topic, message, qos, retained);
            appendMessage("发送 -> 主题: " + topic + "\n类型: 明文\n消息: " + message + "\nQoS: " + qos + 
                    "\n保留: " + (retained ? "是" : "否") + "\n时间: " + 
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "\n\n");
            // 保存发布主题配置
            savePublishTopicConfig(topic);
        } else {
            // 发送二进制消息（将十六进制字符串转换为字节数组）
            try {
                byte[] payload = parseHexString(message);
                mqttHelper.publishBinary(topic, payload, qos, retained);
                appendMessage("发送 -> 主题: " + topic + "\n类型: 二进制\n消息: " + message + 
                        "\n长度: " + payload.length + " bytes\nQoS: " + qos + 
                        "\n保留: " + (retained ? "是" : "否") + "\n时间: " + 
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "\n\n");
                // 保存发布主题配置
                savePublishTopicConfig(topic);
            } catch (Exception e) {
                Toast.makeText(this, "二进制数据格式错误，请输入十六进制字符串，如：48656C6C6F", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // 清空消息输入框
        binding.etMessage.setText("");
    }

    private byte[] parseHexString(String hexString) throws Exception {
        // 移除空格和特殊字符
        hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");
        
        if (hexString.length() % 2 != 0) {
            throw new Exception("十六进制字符串长度必须为偶数");
        }
        
        byte[] result = new byte[hexString.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }

    private void appendMessage(String message) {
        binding.tvMessages.append(message);
        // 自动滚动到底部
        binding.scrollView.fullScroll(View.FOCUS_DOWN);
    }

    private void updateConnectionState(boolean connected, String status) {
        binding.tvStatus.setText("状态: " + status);
        binding.btnConnect.setText(connected ? "断开连接" : "连接");
        
        // 更新其他按钮状态
        updateSubscribeButtonState();
        updatePublishButtonState();
    }

    private void updateSubscribeButtonState() {
        boolean canSubscribe = mqttHelper != null && mqttHelper.isConnected() && 
                !TextUtils.isEmpty(binding.etTopic.getText().toString().trim());
        binding.btnSubscribe.setEnabled(canSubscribe);
        
        String topic = binding.etTopic.getText().toString().trim();
        boolean canUnsubscribe = false;
        if (mqttHelper != null && canSubscribe) {
            MqttHelper.ConnectionStatus status = mqttHelper.getConnectionStatus();
            canUnsubscribe = status.getSubscribedTopics().contains(topic);
        }
        binding.btnUnsubscribe.setEnabled(canUnsubscribe);
    }

    private void updatePublishButtonState() {
        boolean canPublish = mqttHelper != null && mqttHelper.isConnected() && 
                !TextUtils.isEmpty(binding.etPublishTopic.getText().toString().trim()) &&
                !TextUtils.isEmpty(binding.etMessage.getText().toString());
        binding.btnPublish.setEnabled(canPublish);
    }

    private void updateSubscribedTopicsList() {
        if (mqttHelper == null) {
            binding.tvSubscribedTopics.setText("已订阅主题: 无");
            return;
        }
        
        MqttHelper.ConnectionStatus status = mqttHelper.getConnectionStatus();
        java.util.Set<String> topics = status.getSubscribedTopics();
        
        if (topics.isEmpty()) {
            binding.tvSubscribedTopics.setText("已订阅主题: 无");
        } else {
            StringBuilder sb = new StringBuilder("已订阅主题: ");
            for (String topic : topics) {
                sb.append(topic).append(", ");
            }
            // 移除最后的逗号和空格
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            binding.tvSubscribedTopics.setText(sb.toString());
        }
    }

    private void updateMessageInputHint() {
        if (currentMessageType == MessageType.TEXT) {
            binding.etMessage.setHint("输入要发送的文本消息...");
        } else {
            binding.etMessage.setHint("输入十六进制数据，如：48656C6C6F");
        }
    }

    // MQTT事件监听器
    private final MqttHelper.MqttEventListener mqttEventListener = new MqttHelper.MqttEventListener() {
        @Override
        public void onConnected() {
            runOnUiThread(() -> {
                updateConnectionState(true, "已连接");
                // 连接成功后保存连接配置
                saveConnectionConfig();
                Toast.makeText(MqttActivity.this, "MQTT连接成功", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onConnectionLost(Throwable cause) {
            runOnUiThread(() -> {
                updateConnectionState(false, "连接丢失: " + (cause != null ? cause.getMessage() : "未知原因"));
                Toast.makeText(MqttActivity.this, "MQTT连接丢失", Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onConnectFailed(org.eclipse.paho.client.mqttv3.MqttException exception) {
            runOnUiThread(() -> {
                updateConnectionState(false, "连接失败: " + exception.getMessage());
                Toast.makeText(MqttActivity.this, "MQTT连接失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public void onMessageDelivered(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
            // 消息发送完成，可以在这里添加日志
        }
    };

    /**
     * 加载保存的配置
     */
    private void loadSavedConfig() {
        SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        
        // 加载连接配置
        String savedHost = sp.getString(Constants.PREF_MQTT_BROKER_HOST, "192.168.1.100");
        String savedPort = sp.getString(Constants.PREF_MQTT_BROKER_PORT, "1883");
        String savedClientId = sp.getString(Constants.PREF_MQTT_CLIENT_ID, "");
        String savedUsername = sp.getString(Constants.PREF_MQTT_USERNAME, "");
        String savedPassword = sp.getString(Constants.PREF_MQTT_PASSWORD, "");
        
        binding.etBrokerHost.setText(savedHost);
        binding.etBrokerPort.setText(savedPort);
        binding.etClientId.setText(savedClientId);
        binding.etUsername.setText(savedUsername);
        binding.etPassword.setText(savedPassword);
        
        // 加载主题配置
        String savedTopic = sp.getString(Constants.PREF_MQTT_TOPIC, "test/topic");
        String savedQos = sp.getString(Constants.PREF_MQTT_QOS, "1");
        String savedPublishTopic = sp.getString(Constants.PREF_MQTT_PUBLISH_TOPIC, "publish/topic");
        
        binding.etTopic.setText(savedTopic);
        binding.etQos.setText(savedQos);
        binding.etPublishTopic.setText(savedPublishTopic);
    }

    /**
     * 保存连接配置
     */
    private void saveConnectionConfig() {
        SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        
        editor.putString(Constants.PREF_MQTT_BROKER_HOST, binding.etBrokerHost.getText().toString().trim());
        editor.putString(Constants.PREF_MQTT_BROKER_PORT, binding.etBrokerPort.getText().toString().trim());
        editor.putString(Constants.PREF_MQTT_CLIENT_ID, binding.etClientId.getText().toString().trim());
        editor.putString(Constants.PREF_MQTT_USERNAME, binding.etUsername.getText().toString().trim());
        editor.putString(Constants.PREF_MQTT_PASSWORD, binding.etPassword.getText().toString().trim());
        
        editor.apply();
    }

    /**
     * 保存主题配置
     * @param topic 主题名称
     * @param qos QoS级别
     */
    private void saveTopicConfig(String topic, int qos) {
        SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        
        editor.putString(Constants.PREF_MQTT_TOPIC, topic);
        editor.putString(Constants.PREF_MQTT_QOS, String.valueOf(qos));
        
        editor.apply();
    }

    /**
     * 保存发布主题配置
     * @param publishTopic 发布主题名称
     */
    private void savePublishTopicConfig(String publishTopic) {
        SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        
        editor.putString(Constants.PREF_MQTT_PUBLISH_TOPIC, publishTopic);
        
        editor.apply();
    }

    /**
     * 创建主题消息监听器
     * @return 主题监听器实例
     */
    private MqttHelper.MqttTopicListener createTopicListener() {
        return new MqttHelper.MqttTopicListener() {
            @Override
            public void onMessageReceived(String topic, String message) {
                runOnUiThread(() -> {
                    appendMessage("主题: " + topic + "\n类型: 明文\n消息: " + message + "\n时间: " + 
                            new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "\n\n");
                });
            }

            @Override
            public void onBinaryMessageReceived(String topic, byte[] payload) {
                runOnUiThread(() -> {
                    StringBuilder hexString = new StringBuilder();
                    for (byte b : payload) {
                        hexString.append(String.format("%02X ", b));
                    }
                    appendMessage("主题: " + topic + "\n类型: 二进制\n消息: " + hexString.toString() + 
                            "\n长度: " + payload.length + " bytes\n时间: " + 
                            new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "\n\n");
                });
            }
        };
    }

    /**
     * 恢复已订阅主题的消息监听器
     * @param status 连接状态信息
     */
    private void restoreTopicListeners(MqttHelper.ConnectionStatus status) {
        java.util.Set<String> subscribedTopics = status.getSubscribedTopics();
        if (subscribedTopics.isEmpty()) {
            return;
        }
        
        // 创建新的监听器映射
        java.util.Map<String, MqttHelper.MqttTopicListener> topicListenerMap = new java.util.HashMap<>();
        
        for (String topic : subscribedTopics) {
            // 为每个已订阅的主题创建新的监听器
            MqttHelper.MqttTopicListener listener = createTopicListener();
            topicListenerMap.put(topic, listener);
        }
        
        // 批量更新监听器
        mqttHelper.updateTopicListeners(topicListenerMap);
        
        if (!topicListenerMap.isEmpty()) {
            appendMessage("已恢复 " + topicListenerMap.size() + " 个主题的消息监听\n");
        }
    }

    /**
     * 恢复连接状态
     */
    private void restoreConnectionState() {
        if (mqttHelper == null) {
            return;
        }
        
        // 设置事件监听器
        mqttHelper.setEventListener(mqttEventListener);
        
        // 获取当前连接状态
        MqttHelper.ConnectionStatus status = mqttHelper.getConnectionStatus();
        
        // 更新连接状态显示
        if (status.isConnected()) {
            updateConnectionState(true, "已连接");
            MqttHelper.MqttConfig config = status.getConfig();
            if (config != null) {
                // 如果有配置，可以选择性地更新UI显示（但不覆盖用户可能正在编辑的内容）
                appendMessage("恢复连接状态: 已连接到 " + config.getBrokerHost() + ":" + config.getBrokerPort() + "\n");
            }
        } else if (status.isConnecting()) {
            updateConnectionState(false, "正在连接...");
        } else {
            updateConnectionState(false, "未连接");
        }
        
        // 更新订阅状态显示
        updateSubscribedTopicsList();
        
        // 恢复已订阅主题的消息监听器
        restoreTopicListeners(status);
        
        // 如果有订阅的主题，显示恢复信息
        if (!status.getSubscribedTopics().isEmpty()) {
            appendMessage("已恢复订阅状态，共 " + status.getSubscribedTopics().size() + " 个主题\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 不调用destroy()，因为是单例，其他地方可能还在使用
        // 只移除事件监听器
        if (mqttHelper != null) {
            mqttHelper.setEventListener(null);
        }
    }
}
