package com.coolfly.demo.entry;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.coolfly.demo.R;
import com.coolfly.demo.V3OtaActivity;
import com.coolfly.demo.chuanyun.ChuanYunActivity;
import com.coolfly.demo.databinding.ActivityP301Binding;

public class P301Activity extends AppCompatActivity {

    private ActivityP301Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityP301Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void onClick(View view) {
        if (view == binding.btnUpgradeV3) {
            Intent intent = new Intent(this, V3OtaActivity.class);
            startActivity(intent);
        } else if (view == binding.btnConfig) {
            Intent intent = new Intent(this, ChuanYunActivity.class);
            startActivity(intent);
        }
    }
}