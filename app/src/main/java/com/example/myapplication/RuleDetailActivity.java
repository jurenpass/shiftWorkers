package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.ShiftRule;

import java.util.List;

public class RuleDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rule_detail);

        ShiftRule rule = (ShiftRule) getIntent().getSerializableExtra("rule");
        if (rule == null) {
            finish();
            return;
        }

        TextView title = findViewById(R.id.title);
        title.setText(rule.getName());

        TextView cycleInfo = findViewById(R.id.cycle_info);
        cycleInfo.setText(rule.getName() + "(周期: " + rule.getCycleDays() + "天, 班组: " + rule.getGroupCount() + ")");

        TextView companyName = findViewById(R.id.company_name);
        companyName.setText("公司名称:" + rule.getCompanyName());

        TextView tag = findViewById(R.id.tag);
        tag.setText("标签:" + (rule.getTag() != null && !rule.getTag().isEmpty() ? rule.getTag() : "无"));

        LinearLayout rulesContainer = findViewById(R.id.rules_container);

        List<ShiftRule.ShiftDetail> details = rule.getShiftDetails();
        if (details != null) {
            for (ShiftRule.ShiftDetail detail : details) {
                View ruleItem = getLayoutInflater().inflate(R.layout.rule_item, rulesContainer, false);

                TextView dayText = ruleItem.findViewById(R.id.day_text);
                dayText.setText("第" + detail.getDayIndex() + "天");

                TextView shiftName = ruleItem.findViewById(R.id.shift_name);
                shiftName.setText(detail.getShiftName());

                TextView timeRange = ruleItem.findViewById(R.id.time_range);
                timeRange.setText(detail.getTimeRange());

                rulesContainer.addView(ruleItem);
            }
        }

        findViewById(R.id.btn_view_year_calendar).setOnClickListener(v -> {
            Toast.makeText(this, "年历视图功能开发中", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.btn_view_month_calendar).setOnClickListener(v -> {
            finish();
        });

        findViewById(R.id.btn_back).setOnClickListener(v -> {
            finish();
        });
    }
}