package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftRuleManager;

import java.util.List;

public class ShiftRuleListActivity extends AppCompatActivity {

    private LinearLayout rulesContainer;
    private ShiftRuleManager ruleManager;
    private String currentRuleId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_rule_list);

        ruleManager = ShiftRuleManager.getInstance(this);
        rulesContainer = findViewById(R.id.rules_container);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_add).setOnClickListener(v -> {
            Intent intent = new Intent(this, ShiftRuleEditActivity.class);
            startActivity(intent);
        });

        loadRules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRules();
    }

    private void loadRules() {
        rulesContainer.removeAllViews();
        currentRuleId = ruleManager.getCurrentRuleId();
        List<ShiftRule> rules = ruleManager.getRules();

        for (ShiftRule rule : rules) {
            addRuleItem(rule);
        }

        if (rules.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("暂无倒班规则");
            emptyText.setTextSize(16);
            emptyText.setTextColor(0xFF999999);
            emptyText.setPadding(0, 100, 0, 0);
            emptyText.setGravity(android.view.Gravity.CENTER);
            rulesContainer.addView(emptyText);
        }
    }

    private void addRuleItem(ShiftRule rule) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_shift_rule, rulesContainer, false);

        TextView nameView = itemView.findViewById(R.id.tv_rule_name);
        TextView companyView = itemView.findViewById(R.id.tv_company_name);
        TextView cycleView = itemView.findViewById(R.id.tv_cycle_info);
        TextView selectHint = itemView.findViewById(R.id.tv_select_hint);
        View divider = itemView.findViewById(R.id.divider);
        View btnEdit = itemView.findViewById(R.id.btn_edit);
        View btnDelete = itemView.findViewById(R.id.btn_delete);

        nameView.setText(ShiftCalendarUtil.removeHSM2Prefix(rule.getName()));
        companyView.setText(rule.getCompanyName());
        cycleView.setText(String.format("周期: %d天 | 班组: %d个", rule.getCycleDays(), rule.getGroupCount()));

        boolean isCurrent = rule.getId() != null && rule.getId().equals(currentRuleId);
        if (isCurrent) {
            selectHint.setText("正在使用");
            selectHint.setTextColor(0xFF4CAF50);
            itemView.setBackgroundColor(0xFFE8F5E9);
        } else {
            selectHint.setText("设为当前使用");
            selectHint.setTextColor(0xFF1E90FF);
            itemView.setBackgroundColor(0xFFFFFFFF);
        }

        itemView.setOnClickListener(v -> {
            if (!isCurrent) {
                ruleManager.setCurrentRuleId(rule.getId());
                setResult(RESULT_OK);
                finish();
            }
        });

        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, ShiftRuleEditActivity.class);
            intent.putExtra("rule_id", rule.getId());
            startActivity(intent);
        });

        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除该倒班规则吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    if (isCurrent) {
                        Toast.makeText(this, "无法删除正在使用的规则", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ruleManager.deleteRule(rule.getId());
                    loadRules();
                })
                .setNegativeButton("取消", null)
                .show();
        });

        rulesContainer.addView(itemView);
    }
}
