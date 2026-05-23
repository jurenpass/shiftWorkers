package com.example.myapplication;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftRuleManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ShiftRuleEditActivity extends AppCompatActivity {

    private AutoCompleteTextView actDefaultGroupName;
    private AutoCompleteTextView actTodayShift;
    private TextView tvCycleDays;
    private ImageView btnSubCycleDay;
    private ImageView btnAddCycleDay;
    private LinearLayout shiftsContainer;
    private LinearLayout otherGroupsContainer;
    private Switch switchDefault;
    private TextView tvTitle;
    private TextView btnSave;

    private ShiftRuleManager ruleManager;
    private ShiftRule existingRule;
    private String ruleId;
    private int cycleDays = 4;

    private TextView currentTimeTextView;

    private static final String[] SHIFT_ARRAY = {"白班", "上夜班", "下夜班", "正休"};
    private static final String[] GROUP_ARRAY = {"甲班", "乙班", "丙班", "丁班"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_rule_edit);

        ruleManager = ShiftRuleManager.getInstance(this);
        ruleId = getIntent().getStringExtra("rule_id");

        tvCycleDays = findViewById(R.id.tv_cycle_days);
        btnSubCycleDay = findViewById(R.id.btn_sub_cycle_day);
        btnAddCycleDay = findViewById(R.id.btn_add_cycle_day);
        shiftsContainer = findViewById(R.id.shifts_container);
        otherGroupsContainer = findViewById(R.id.other_groups_container);
        actDefaultGroupName = findViewById(R.id.act_default_group_name);
        actTodayShift = findViewById(R.id.act_today_shift);
        switchDefault = findViewById(R.id.switch_default);
        tvTitle = findViewById(R.id.tv_title);
        btnSave = findViewById(R.id.btn_save);

        ArrayAdapter<String> shiftAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, SHIFT_ARRAY);
        ArrayAdapter<String> groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, GROUP_ARRAY);

        actDefaultGroupName.setAdapter(groupAdapter);
        actTodayShift.setAdapter(shiftAdapter);

        actDefaultGroupName.setOnClickListener(v -> actDefaultGroupName.showDropDown());
        actTodayShift.setOnClickListener(v -> actTodayShift.showDropDown());

        if (ruleId != null) {
            existingRule = ruleManager.getRuleById(ruleId);
            if (existingRule != null) {
                tvTitle.setText("编辑倒班规则");
                cycleDays = existingRule.getCycleDays();
                tvCycleDays.setText(cycleDays + "");

                actDefaultGroupName.setText(existingRule.getDefaultGroupName() != null ? existingRule.getDefaultGroupName() : "");
                actTodayShift.setText(existingRule.getTodayShift() != null ? existingRule.getTodayShift() : "");
                switchDefault.setChecked(existingRule.isDefault());

                if (existingRule.getShiftDetails() != null) {
                    for (ShiftRule.ShiftDetail detail : existingRule.getShiftDetails()) {
                        addShiftItem(detail, shiftAdapter);
                    }
                }

                if (existingRule.getOtherGroups() != null) {
                    for (ShiftRule.OtherGroup group : existingRule.getOtherGroups()) {
                        addOtherGroupItem(group, groupAdapter, shiftAdapter);
                    }
                }
            }
        }

        if (shiftsContainer.getChildCount() == 0) {
            for (int i = 0; i < cycleDays; i++) {
                addShiftItem(null, shiftAdapter);
            }
        }

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveRule());
        btnAddCycleDay.setOnClickListener(v -> addCycleDay(shiftAdapter));
        btnSubCycleDay.setOnClickListener(v -> subtractCycleDay());
        findViewById(R.id.btn_add_other_group).setOnClickListener(v -> addOtherGroupItem(null, groupAdapter, shiftAdapter));
    }

    private void addCycleDay(ArrayAdapter<String> shiftAdapter) {
        cycleDays++;
        tvCycleDays.setText(cycleDays + "");
        addShiftItem(null, shiftAdapter);
    }

    private void subtractCycleDay() {
        if (cycleDays > 1) {
            cycleDays--;
            tvCycleDays.setText(cycleDays + "");
            if (shiftsContainer.getChildCount() > 0) {
                shiftsContainer.removeViewAt(shiftsContainer.getChildCount() - 1);
            }
        }
    }

    private void addShiftItem(ShiftRule.ShiftDetail detail, ArrayAdapter<String> shiftAdapter) {
        int dayIndex = shiftsContainer.getChildCount() + 1;

        View itemView = LayoutInflater.from(this).inflate(R.layout.item_shift_detail, shiftsContainer, false);

        TextView tvDayIndex = itemView.findViewById(R.id.tv_day_index);
        AutoCompleteTextView actShiftName = itemView.findViewById(R.id.act_shift_name);
        TextView tvStartTime = itemView.findViewById(R.id.tv_start_time);
        TextView tvEndTime = itemView.findViewById(R.id.tv_end_time);

        tvDayIndex.setText("第" + dayIndex + "天");
        actShiftName.setAdapter(shiftAdapter);
        actShiftName.setOnClickListener(v -> actShiftName.showDropDown());

        if (detail != null) {
            actShiftName.setText(detail.getShiftName());
            String timeRange = detail.getTimeRange();
            if (timeRange != null && timeRange.contains("到")) {
                String[] times = timeRange.split("到");
                if (times.length >= 2) {
                    tvStartTime.setText(times[0]);
                    tvEndTime.setText(times[1]);
                }
            }
        }

        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));

        shiftsContainer.addView(itemView);
    }

    private void showTimePicker(TextView textView) {
        currentTimeTextView = textView;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
            (view, hourOfDay, minute1) -> {
                String time = String.format("%02d:%02d", hourOfDay, minute1);
                currentTimeTextView.setText(time);
            }, hour, minute, true);
        timePickerDialog.show();
    }

    private void addOtherGroupItem(ShiftRule.OtherGroup group, ArrayAdapter<String> groupAdapter, ArrayAdapter<String> shiftAdapter) {
        View itemView = LayoutInflater.from(this).inflate(R.layout.item_other_group, otherGroupsContainer, false);

        AutoCompleteTextView actGroupName = itemView.findViewById(R.id.act_group_name);
        AutoCompleteTextView actTodayShift = itemView.findViewById(R.id.act_today_shift);
        ImageView btnRemove = itemView.findViewById(R.id.btn_remove_other_group);

        actGroupName.setAdapter(groupAdapter);
        actTodayShift.setAdapter(shiftAdapter);

        actGroupName.setOnClickListener(v -> actGroupName.showDropDown());
        actTodayShift.setOnClickListener(v -> actTodayShift.showDropDown());

        if (group != null) {
            actGroupName.setText(group.getGroupName());
            actTodayShift.setText(group.getTodayShift());
        }

        btnRemove.setOnClickListener(v -> {
            otherGroupsContainer.removeView(itemView);
        });

        otherGroupsContainer.addView(itemView);
    }

    private void saveRule() {
        String name = getIntent().getStringExtra("rule_name");
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "请输入倒班名称", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ShiftRule.ShiftDetail> shiftDetails = new ArrayList<>();
        for (int i = 0; i < shiftsContainer.getChildCount(); i++) {
            View itemView = shiftsContainer.getChildAt(i);
            AutoCompleteTextView actShiftName = itemView.findViewById(R.id.act_shift_name);
            TextView tvStartTime = itemView.findViewById(R.id.tv_start_time);
            TextView tvEndTime = itemView.findViewById(R.id.tv_end_time);

            String shiftName = actShiftName.getText().toString().trim();
            String startTime = tvStartTime.getText().toString();
            String endTime = tvEndTime.getText().toString();
            String timeRange = startTime + "到" + endTime;

            if (TextUtils.isEmpty(shiftName)) {
                Toast.makeText(this, "请输入第" + (i + 1) + "天的班次名称", Toast.LENGTH_SHORT).show();
                return;
            }

            ShiftRule.ShiftDetail detail = new ShiftRule.ShiftDetail(i + 1, shiftName, timeRange);
            shiftDetails.add(detail);
        }

        if (shiftDetails.isEmpty()) {
            Toast.makeText(this, "请至少添加一个班次", Toast.LENGTH_SHORT).show();
            return;
        }

        List<ShiftRule.OtherGroup> otherGroups = new ArrayList<>();
        for (int i = 0; i < otherGroupsContainer.getChildCount(); i++) {
            View itemView = otherGroupsContainer.getChildAt(i);
            AutoCompleteTextView actGroupName = itemView.findViewById(R.id.act_group_name);
            AutoCompleteTextView actTodayShift = itemView.findViewById(R.id.act_today_shift);

            String groupName = actGroupName.getText().toString().trim();
            String todayShift = actTodayShift.getText().toString().trim();

            if (!TextUtils.isEmpty(groupName)) {
                ShiftRule.OtherGroup group = new ShiftRule.OtherGroup(groupName, todayShift);
                otherGroups.add(group);
            }
        }

        ShiftRule rule = new ShiftRule();
        rule.setName(name);
        rule.setCycleDays(cycleDays);
        rule.setShiftDetails(shiftDetails);
        rule.setDefaultGroupName(actDefaultGroupName.getText().toString().trim());
        rule.setTodayShift(actTodayShift.getText().toString().trim());
        rule.setOtherGroups(otherGroups);
        rule.setDefault(switchDefault.isChecked());

        if (existingRule != null) {
            rule.setId(existingRule.getId());
            ruleManager.updateRule(rule);
        } else {
            rule.setId(String.valueOf(System.currentTimeMillis()));
            ruleManager.addRule(rule);
        }

        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        finish();
    }
}