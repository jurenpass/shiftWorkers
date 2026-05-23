package com.example.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.CandleStickChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.CandleData;
import com.github.mikephil.charting.data.CandleDataSet;
import com.github.mikephil.charting.data.CandleEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.List;

public class StockActivity extends AppCompatActivity {

    private EditText etStockCode;
    private Button btnSearch;
    private TextView tvStockName;
    private TextView tvPrice;
    private CandleStickChart candleChart;
    private LinearLayout tipLayout;
    private Button btnTimeShare;
    private Button btnDay;
    private Button btnWeek;
    private Button btnMonth;

    private String currentCode = "";
    private int currentType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock);

        etStockCode = findViewById(R.id.et_stock_code);
        btnSearch = findViewById(R.id.btn_search);
        tvStockName = findViewById(R.id.tv_stock_name);
        tvPrice = findViewById(R.id.tv_price);
        candleChart = findViewById(R.id.kChartView);
        tipLayout = findViewById(R.id.tip_layout);
        btnTimeShare = findViewById(R.id.btn_time_share);
        btnDay = findViewById(R.id.btn_day);
        btnWeek = findViewById(R.id.btn_week);
        btnMonth = findViewById(R.id.btn_month);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        btnSearch.setOnClickListener(v -> searchStock());

        etStockCode.setOnEditorActionListener((v, actionId, event) -> {
            searchStock();
            return true;
        });

        btnTimeShare.setOnClickListener(v -> switchChartType(0));
        btnDay.setOnClickListener(v -> switchChartType(1));
        btnWeek.setOnClickListener(v -> switchChartType(2));
        btnMonth.setOnClickListener(v -> switchChartType(3));

        setupChart();
    }

    private void setupChart() {
        candleChart.setBackgroundColor(Color.BLACK);
        candleChart.setDrawGridBackground(false);
        candleChart.getDescription().setEnabled(false);
        candleChart.setTouchEnabled(true);
        candleChart.setScaleEnabled(true);
        candleChart.setPinchZoom(true);

        XAxis xAxis = candleChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setValueFormatter(new DateFormatter());

        YAxis leftAxis = candleChart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.GRAY);

        YAxis rightAxis = candleChart.getAxisRight();
        rightAxis.setEnabled(false);

        candleChart.getLegend().setEnabled(false);
    }

    private void searchStock() {
        String code = etStockCode.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "请输入股票代码", Toast.LENGTH_SHORT).show();
            return;
        }

        currentCode = code;
        loadKLineData(code, currentType);
    }

    private void switchChartType(int type) {
        currentType = type;

        btnTimeShare.setBackgroundColor(type == 0 ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.background_light));
        btnDay.setBackgroundColor(type == 1 ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.background_light));
        btnWeek.setBackgroundColor(type == 2 ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.background_light));
        btnMonth.setBackgroundColor(type == 3 ? getResources().getColor(android.R.color.white) : getResources().getColor(android.R.color.background_light));

        btnTimeShare.setTextColor(type == 0 ? getResources().getColor(android.R.color.holo_blue_light) : getResources().getColor(android.R.color.black));
        btnDay.setTextColor(type == 1 ? getResources().getColor(android.R.color.holo_blue_light) : getResources().getColor(android.R.color.black));
        btnWeek.setTextColor(type == 2 ? getResources().getColor(android.R.color.holo_blue_light) : getResources().getColor(android.R.color.black));
        btnMonth.setTextColor(type == 3 ? getResources().getColor(android.R.color.holo_blue_light) : getResources().getColor(android.R.color.black));

        if (!currentCode.isEmpty()) {
            loadKLineData(currentCode, type);
        }
    }

    private void loadKLineData(String code, int type) {
        loadMockData(code);
    }

    private void loadMockData(String code) {
        tvStockName.setText("模拟股票 " + code);
        tvPrice.setText("10.50");
        tvPrice.setTextColor(getResources().getColor(android.R.color.holo_red_light));

        List<CandleEntry> entries = new ArrayList<>();
        float basePrice = 10.0f;

        for (int i = 0; i < 20; i++) {
            float open = basePrice + (float) (Math.random() - 0.5) * 0.5f;
            float close = basePrice + (float) (Math.random() - 0.5) * 0.5f;
            float low = Math.min(open, close) - (float) Math.random() * 0.3f;
            float high = Math.max(open, close) + (float) Math.random() * 0.3f;
            
            entries.add(new CandleEntry(i, high, low, open, close));
            basePrice = close;
        }

        CandleDataSet dataSet = new CandleDataSet(entries, "K线");
        dataSet.setDecreasingColor(Color.RED);
        dataSet.setIncreasingColor(Color.GREEN);
        dataSet.setNeutralColor(Color.GRAY);
        dataSet.setBarSpace(0.3f);
        dataSet.setShadowWidth(1f);
        dataSet.setDrawValues(false);

        CandleData data = new CandleData(dataSet);
        candleChart.setData(data);
        candleChart.invalidate();

        tipLayout.setVisibility(View.GONE);
        candleChart.setVisibility(View.VISIBLE);
    }

    private class DateFormatter extends ValueFormatter {
        @Override
        public String getFormattedValue(float value) {
            int day = (int) value + 1;
            return "01-" + String.format("%02d", day);
        }
    }
}