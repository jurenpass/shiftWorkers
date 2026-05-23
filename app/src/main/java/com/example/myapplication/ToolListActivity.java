package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ToolListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.item_measure).setOnClickListener(v -> {
            Intent intent = new Intent(this, MeasureActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.item_stock).setOnClickListener(v -> {
            Intent intent = new Intent(this, StockActivity.class);
            startActivity(intent);
        });
    }
}