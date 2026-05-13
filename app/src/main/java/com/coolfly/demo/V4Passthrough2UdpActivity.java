package com.coolfly.demo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.coolfly.demo.databinding.ActivityV4Passthrough2udpBinding;
import com.coolfly.demo.preference.PreferenceActivity;
import com.fly.station.prorocol.DEVICE_TYPE;
import com.fly.station.prorocol.ProtocolHelper;
import com.fly.station.prorocol.ProtocolListener;
import com.fly.station.prorocol.RADIO_TYPE;
import com.fly.station.prorocol.bean.BaseFlyPacket;
import com.fly.station.prorocol.bean.PassthroughData8030;
import com.fly.station.prorocol.bean.Throughput8030;
import com.fly.station.udp.UdpController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class V4Passthrough2UdpActivity extends AppCompatActivity {

    private static final String TAG = V4Passthrough2UdpActivity.class.getSimpleName();
    private static final int BASE_UDP_PORT = 9000;
    private static final int BASE_LOCAL_PORT = 9100;
    private static final String DEFAULT_UDP_IP = "127.0.0.1";

    private ActivityV4Passthrough2udpBinding binding;
    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();
    private final List<Channel> channelList = new ArrayList<>();
    private int channelCounter = 0;

    private static class Channel {
        int slot;
        int port;
        String udpIp;
        int udpPort;
        int udpLocalPort;
        UdpController udpController;
        boolean passthroughConnected;
        View channelView;
        TextView tvSlot;
        TextView tvPort;
        EditText etUdpIp;
        EditText etUdpPort;
        EditText etLocalPort;
        TextView btnPtConnect;
        TextView btnUdpConnect;
        TextView tvPtStatus;
        TextView tvUdpStatus;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityV4Passthrough2udpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        UdpController.setIsShowLog(PreferenceActivity.preferenceObject.show_udp_log);

        binding.btnAddChannel.setOnClickListener(v -> addChannel());
        binding.btnConnectAll.setOnClickListener(v -> connectAll());
        binding.btnDisconnectAll.setOnClickListener(v -> disconnectAll());

        protocolHelper.addListener(protocolListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        protocolHelper.removeListener(protocolListener);
        for (Channel ch : new ArrayList<>(channelList)) {
            destroyChannel(ch);
        }
        channelList.clear();
    }

    private void addChannel() {
        Channel ch = new Channel();
        ch.slot = 0;
        ch.port = 2;
        ch.udpIp = DEFAULT_UDP_IP;
        ch.udpPort = BASE_UDP_PORT + channelCounter;
        ch.udpLocalPort = BASE_LOCAL_PORT + channelCounter;
        ch.passthroughConnected = false;
        ch.udpController = new UdpController();
        channelCounter++;

        ch.udpController.addListener(createUdpListener(ch));

        ch.channelView = createChannelView(ch);
        binding.channelContainer.addView(ch.channelView);
        channelList.add(ch);
    }

    private void removeChannel(Channel ch) {
        destroyChannel(ch);
        binding.channelContainer.removeView(ch.channelView);
        channelList.remove(ch);
    }

    private void destroyChannel(Channel ch) {
        if (ch.passthroughConnected) {
            protocolHelper.ar8030ClosePassthrough(ch.slot, ch.port);
            ch.passthroughConnected = false;
        }
        if (ch.udpController != null) {
            ch.udpController.release();
        }
    }

    private void connectAll() {
        for (Channel ch : channelList) {
            readChannelConfig(ch);
            connectPassthrough(ch);
            connectUdp(ch);
        }
    }

    private void disconnectAll() {
        for (Channel ch : channelList) {
            disconnectPassthrough(ch);
            disconnectUdp(ch);
        }
    }

    private void connectPassthrough(Channel ch) {
        if (!ch.passthroughConnected) {
            protocolHelper.ar8030OpenPassthrough(ch.slot, ch.port);
            ch.passthroughConnected = true;
            updatePtStatus(ch, true);
        }
    }

    private void disconnectPassthrough(Channel ch) {
        if (ch.passthroughConnected) {
            protocolHelper.ar8030ClosePassthrough(ch.slot, ch.port);
            ch.passthroughConnected = false;
            updatePtStatus(ch, false);
        }
    }

    private void connectUdp(Channel ch) {
        if (ch.udpController.isConnected()) return;
        ch.udpController.setTarget(ch.udpIp, ch.udpPort).setLocalPort(ch.udpLocalPort).connect();
    }

    private void disconnectUdp(Channel ch) {
        if (!ch.udpController.isConnected()) return;
        ch.udpController.disconnect();
    }

    private void readChannelConfig(Channel ch) {
        try {
            String ipText = ch.etUdpIp.getText().toString().trim();
            if (!TextUtils.isEmpty(ipText)) ch.udpIp = ipText;
            String portText = ch.etUdpPort.getText().toString().trim();
            if (!TextUtils.isEmpty(portText)) ch.udpPort = Integer.parseInt(portText);
            String localPortText = ch.etLocalPort.getText().toString().trim();
            if (!TextUtils.isEmpty(localPortText)) ch.udpLocalPort = Integer.parseInt(localPortText);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid config: " + e.getMessage());
        }
    }

    private void updatePtStatus(Channel ch, boolean connected) {
        if (ch.tvPtStatus != null) {
            runOnUiThread(() -> {
                ch.tvPtStatus.setBackgroundColor(connected ? Color.GREEN : Color.DKGRAY);
            });
        }
        if (ch.btnPtConnect != null) {
            runOnUiThread(() -> {
                ch.btnPtConnect.setText(connected ? "PT断开" : "PT连接");
            });
        }
    }

    private void updateUdpStatus(Channel ch, boolean connected) {
        if (ch.tvUdpStatus != null) {
            runOnUiThread(() -> {
                ch.tvUdpStatus.setBackgroundColor(connected ? Color.GREEN : Color.DKGRAY);
            });
        }
        if (ch.btnUdpConnect != null) {
            runOnUiThread(() -> {
                ch.btnUdpConnect.setText(connected ? "UDP断开" : "UDP连接");
            });
        }
    }

    private View createChannelView(Channel ch) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowLp.setMargins(0, 4, 0, 4);
        row.setLayoutParams(rowLp);

        int dp32 = dpToPx(32);
        int dp4 = dpToPx(4);

        ch.tvPtStatus = createStatusDot();
        row.addView(ch.tvPtStatus);

        ch.tvSlot = createLabel("Slot:" + ch.slot, dp4);
        ch.tvSlot.setBackgroundColor(0xFFFF8800);
        ch.tvSlot.setOnClickListener(v -> showSlotPicker(ch));
        row.addView(ch.tvSlot);

        ch.tvPort = createLabel("Port:" + ch.port, dp4);
        ch.tvPort.setBackgroundColor(0xFFFF8800);
        ch.tvPort.setOnClickListener(v -> showPortPicker(ch));
        row.addView(ch.tvPort);

        ch.btnPtConnect = createButton("PT连接", 0xFF4CAF50, dp4);
        ch.btnPtConnect.setOnClickListener(v -> {
            readChannelConfig(ch);
            if (ch.passthroughConnected) {
                disconnectPassthrough(ch);
            } else {
                connectPassthrough(ch);
            }
        });
        row.addView(ch.btnPtConnect);

        row.addView(createSeparator());

        ch.tvUdpStatus = createStatusDot();
        row.addView(ch.tvUdpStatus);

        ch.etUdpIp = createEditText(ch.udpIp, "IP", 0, dp4);
        row.addView(ch.etUdpIp);

        ch.etUdpPort = createEditText(String.valueOf(ch.udpPort), "Port", android.text.InputType.TYPE_CLASS_NUMBER, dp4);
        row.addView(ch.etUdpPort);

        ch.etLocalPort = createEditText(String.valueOf(ch.udpLocalPort), "LPort", android.text.InputType.TYPE_CLASS_NUMBER, dp4);
        row.addView(ch.etLocalPort);

        ch.btnUdpConnect = createButton("UDP连接", 0xFF2196F3, dp4);
        ch.btnUdpConnect.setOnClickListener(v -> {
            readChannelConfig(ch);
            if (ch.udpController.isConnected()) {
                disconnectUdp(ch);
            } else {
                connectUdp(ch);
            }
        });
        row.addView(ch.btnUdpConnect);

        row.addView(createSeparator());

        TextView btnRemove = createButton("X", 0xFFF44336, dp4);
        btnRemove.setOnClickListener(v -> removeChannel(ch));
        row.addView(btnRemove);

        return row;
    }

    private TextView createLabel(String text, int margin) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11);
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(28));
        lp.setMargins(margin, 0, margin, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView createButton(String text, int bgColor, int margin) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(10);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(bgColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(52), dpToPx(28));
        lp.setMargins(margin, 0, margin, 0);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText createEditText(String text, String hint, int inputType, int margin) {
        EditText et = new EditText(this);
        et.setText(text);
        et.setHint(hint);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(0xFF888888);
        et.setTextSize(10);
        et.setGravity(Gravity.CENTER);
        et.setBackgroundColor(0xFF333333);
        et.setSingleLine(true);
        et.setPadding(0, 0, 0, 0);
        et.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        if (inputType != 0) et.setInputType(inputType);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(28), 1f);
        lp.setMargins(margin, 0, margin, 0);
        et.setLayoutParams(lp);
        return et;
    }

    private TextView createStatusDot() {
        TextView dot = new TextView(this);
        dot.setBackgroundColor(Color.DKGRAY);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        lp.setMargins(dpToPx(4), 0, dpToPx(2), 0);
        dot.setLayoutParams(lp);
        return dot;
    }

    private View createSeparator() {
        View sep = new View(this);
        sep.setBackgroundColor(0xFF666666);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(1, dpToPx(20));
        lp.setMargins(dpToPx(4), 0, dpToPx(4), 0);
        sep.setLayoutParams(lp);
        return sep;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void showSlotPicker(Channel ch) {
        String[] slots = getResources().getStringArray(R.array.p401slot_value);
        new AlertDialog.Builder(this)
                .setTitle("slot")
                .setItems(slots, (dialog, which) -> {
                    ch.slot = Integer.parseInt(slots[which]);
                    ch.tvSlot.setText("S:" + ch.slot);
                }).create().show();
    }

    private void showPortPicker(Channel ch) {
        String[] ports = getResources().getStringArray(R.array.p401ports_passthrough_value);
        new AlertDialog.Builder(this)
                .setTitle("passthrough port")
                .setItems(ports, (dialog, which) -> {
                    ch.port = Integer.parseInt(ports[which]);
                    ch.tvPort.setText("P:" + ch.port);
                }).create().show();
    }

    private UdpController.UdpListener createUdpListener(Channel ch) {
        return new UdpController.UdpListener() {
            @Override
            public void onConnectionStateChanged(boolean isConnected, String errorMsg) {
                updateUdpStatus(ch, isConnected);
                if (errorMsg != null) {
                    Log.e(TAG, "UDP[S" + ch.slot + "P" + ch.port + "] error: " + errorMsg);
                }
            }

            @Override
            public void onDataReceived(byte[] data, int length) {
                byte[] copy = Arrays.copyOf(data, length);
                new Thread(() -> {
                    protocolHelper.ar8030WritePassthroughData(ch.slot, ch.port, copy, length);
                }).start();
            }

            @Override
            public void onDataSent(byte[] data, int length) {
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "UDP[S" + ch.slot + "P" + ch.port + "] " + message);
            }
        };
    }

    @Keep
    private final ProtocolListener protocolListener = new ProtocolListener() {
        @Override
        public void onReady(DEVICE_TYPE deviceType) {
        }

        @Override
        public void onReadCmd(BaseFlyPacket baseFlyPacket, DEVICE_TYPE deviceType, boolean isRemote) {
            if (baseFlyPacket instanceof PassthroughData8030 pt) {
                for (Channel ch : channelList) {
                    if (ch.slot == pt.slot && ch.port == pt.port && ch.udpController.isConnected()) {
                        byte[] copy = Arrays.copyOf(pt.data, pt.length);
                        ch.udpController.sendData(copy, pt.length);
                        break;
                    }
                }
            }
        }

        @Override
        public int onWrite(byte[] bytes) {
            return 0;
        }

        @Override
        public void onPairOperated(DEVICE_TYPE deviceType, int slot, boolean isStart) {
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
        }

        @Override
        public void onSetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
        }

        @Override
        public void onResetConfigJson(boolean result, DEVICE_TYPE deviceType, boolean isRemote) {
        }

        @Override
        public void onSlotMac(DEVICE_TYPE deviceType, int i, String s) {
        }

        @Override
        public void onThroughput(DEVICE_TYPE deviceType, Throughput8030 throughput, boolean isRemote) {
        }

        @Override
        public void onSetRadio(DEVICE_TYPE deviceType, RADIO_TYPE radioType, boolean isSuccess, int errCode, String errMessage, boolean isRemote) {
        }

        @Override
        public void onDebugMessage(DEVICE_TYPE deviceType, String s) {
        }
    };
}
