package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.item_shift_rules).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShiftRuleListActivity.class);
            startActivity(intent);
        });

    }
}
