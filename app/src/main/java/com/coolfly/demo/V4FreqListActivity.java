package com.coolfly.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fly.station.prorocol.ProtocolHelper;

public class V4FreqListActivity extends AppCompatActivity {

    private final ProtocolHelper protocolHelper = ProtocolHelper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_v4_freq_list);

        EditText etFreqList = findViewById(R.id.et_freq_list);
        androidx.appcompat.widget.SwitchCompat swRemote = findViewById(R.id.sw_remote);
        Button btnSet = findViewById(R.id.btn_set);

        btnSet.setOnClickListener(v -> {
            String text = etFreqList.getText().toString().trim();
            if (TextUtils.isEmpty(text)) {
                Toast.makeText(this, "freq list is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                String[] parts = text.split("\\s+");
                int[] freqs = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    String s = parts[i].trim();
                    if (s.isEmpty()) {
                        throw new IllegalArgumentException("empty value");
                    }
                    freqs[i] = Integer.parseInt(s);
                }
                protocolHelper.ar8030SetFreqList(freqs, swRemote.isChecked());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "format error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
