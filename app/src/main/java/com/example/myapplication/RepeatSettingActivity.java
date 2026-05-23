package com.example.myapplication;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class RepeatSettingActivity extends AppCompatActivity {

    public static final String EXTRA_REPEAT_TYPE = "repeat_type";
    public static final String EXTRA_WEEKDAYS = "weekdays";
    public static final String EXTRA_DATE = "date";

    private static final int REPEAT_ONCE = 0;
    private static final int REPEAT_DAILY = 1;
    private static final int REPEAT_WEEKLY = 2;
    private static final int REPEAT_DATE = 3;
    private static final int REQUEST_CODE_DATE = 1001;

    private int repeatType = REPEAT_ONCE;
    private Set<Integer> selectedWeekdays = new HashSet<>();
    private long selectedDate = 0;

    private ImageView ivOnce, ivDaily, ivWeekday, ivDate;
    private TextView tvWeekdays, tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repeat_setting);

        ivOnce = findViewById(R.id.iv_once);
        ivDaily = findViewById(R.id.iv_daily);
        ivWeekday = findViewById(R.id.iv_weekday);
        ivDate = findViewById(R.id.iv_date);
        tvWeekdays = findViewById(R.id.tv_weekdays);
        tvDate = findViewById(R.id.tv_date);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        repeatType = getIntent().getIntExtra(EXTRA_REPEAT_TYPE, REPEAT_ONCE);
        if (getIntent().hasExtra(EXTRA_WEEKDAYS)) {
            String weekdaysStr = getIntent().getStringExtra(EXTRA_WEEKDAYS);
            if (weekdaysStr != null && !weekdaysStr.isEmpty()) {
                String[] days = weekdaysStr.split(",");
                for (String day : days) {
                    selectedWeekdays.add(Integer.parseInt(day));
                }
            }
        }
        selectedDate = getIntent().getLongExtra(EXTRA_DATE, 0);

        updateSelection();

        findViewById(R.id.ll_daily).setOnClickListener(v -> selectDaily());
        findViewById(R.id.ll_weekday).setOnClickListener(v -> selectWeekly());
        findViewById(R.id.ll_date).setOnClickListener(v -> selectDate());
        ((View) findViewById(R.id.iv_once).getParent()).setOnClickListener(v -> selectOnce());
    }

    private void updateSelection() {
        ivOnce.setVisibility(repeatType == REPEAT_ONCE ? View.VISIBLE : View.GONE);
        ivDaily.setVisibility(repeatType == REPEAT_DAILY ? View.VISIBLE : View.GONE);
        ivWeekday.setVisibility(repeatType == REPEAT_WEEKLY ? View.VISIBLE : View.GONE);
        ivDate.setVisibility(repeatType == REPEAT_DATE ? View.VISIBLE : View.GONE);

        if (repeatType == REPEAT_WEEKLY) {
            tvWeekdays.setText(formatWeekdays());
            tvWeekdays.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            tvWeekdays.setText("选择星期");
            tvWeekdays.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        if (repeatType == REPEAT_DATE && selectedDate > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDate);
            tvDate.setText(String.format("%d年%d月%d日",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH)));
            tvDate.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            tvDate.setText("选择日期");
            tvDate.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }
    }

    private void selectOnce() {
        repeatType = REPEAT_ONCE;
        selectedWeekdays.clear();
        selectedDate = 0;
        updateSelection();
        saveAndReturn();
    }

    private void selectDaily() {
        repeatType = REPEAT_DAILY;
        selectedWeekdays.clear();
        selectedDate = 0;
        updateSelection();
        saveAndReturn();
    }

    private void selectWeekly() {
        showWeekdayPicker();
    }

    private void selectDate() {
        Intent intent = new Intent(this, DatePickerActivity.class);
        intent.putExtra(DatePickerActivity.EXTRA_DATE, selectedDate);
        startActivityForResult(intent, REQUEST_CODE_DATE);
    }

    private void showWeekdayPicker() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_weekday_picker, null);

        CheckBox cbMonday = dialogView.findViewById(R.id.cb_monday);
        CheckBox cbTuesday = dialogView.findViewById(R.id.cb_tuesday);
        CheckBox cbWednesday = dialogView.findViewById(R.id.cb_wednesday);
        CheckBox cbThursday = dialogView.findViewById(R.id.cb_thursday);
        CheckBox cbFriday = dialogView.findViewById(R.id.cb_friday);
        CheckBox cbSaturday = dialogView.findViewById(R.id.cb_saturday);
        CheckBox cbSunday = dialogView.findViewById(R.id.cb_sunday);

        cbMonday.setChecked(selectedWeekdays.contains(Calendar.MONDAY));
        cbTuesday.setChecked(selectedWeekdays.contains(Calendar.TUESDAY));
        cbWednesday.setChecked(selectedWeekdays.contains(Calendar.WEDNESDAY));
        cbThursday.setChecked(selectedWeekdays.contains(Calendar.THURSDAY));
        cbFriday.setChecked(selectedWeekdays.contains(Calendar.FRIDAY));
        cbSaturday.setChecked(selectedWeekdays.contains(Calendar.SATURDAY));
        cbSunday.setChecked(selectedWeekdays.contains(Calendar.SUNDAY));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("选择重复日期")
                .setView(dialogView)
                .setPositiveButton("确定", (d, which) -> {
                    selectedWeekdays.clear();
                    if (cbMonday.isChecked()) selectedWeekdays.add(Calendar.MONDAY);
                    if (cbTuesday.isChecked()) selectedWeekdays.add(Calendar.TUESDAY);
                    if (cbWednesday.isChecked()) selectedWeekdays.add(Calendar.WEDNESDAY);
                    if (cbThursday.isChecked()) selectedWeekdays.add(Calendar.THURSDAY);
                    if (cbFriday.isChecked()) selectedWeekdays.add(Calendar.FRIDAY);
                    if (cbSaturday.isChecked()) selectedWeekdays.add(Calendar.SATURDAY);
                    if (cbSunday.isChecked()) selectedWeekdays.add(Calendar.SUNDAY);

                    if (selectedWeekdays.isEmpty()) {
                        Toast.makeText(this, "请至少选择一天", Toast.LENGTH_SHORT).show();
                    } else {
                        repeatType = REPEAT_WEEKLY;
                        selectedDate = 0;
                        updateSelection();
                        saveAndReturn();
                    }
                })
                .setNegativeButton("取消", null)
                .create();

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DATE && resultCode == RESULT_OK && data != null) {
            selectedDate = data.getLongExtra(DatePickerActivity.EXTRA_DATE, 0);
            repeatType = REPEAT_DATE;
            selectedWeekdays.clear();
            updateSelection();
            saveAndReturn();
        }
    }

    private String formatWeekdays() {
        if (selectedWeekdays.isEmpty()) {
            return "选择星期";
        }

        String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        int[] dayValues = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};

        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < dayValues.length; i++) {
            if (selectedWeekdays.contains(dayValues[i])) {
                if (count > 0 && count % 4 == 0) {
                    sb.append("\n");
                } else if (count > 0) {
                    sb.append(" ");
                }
                sb.append(dayNames[i]);
                count++;
            }
        }

        return sb.toString();
    }

    private void saveAndReturn() {
        Intent result = new Intent();
        result.putExtra(EXTRA_REPEAT_TYPE, repeatType);

        if (repeatType == REPEAT_WEEKLY) {
            StringBuilder sb = new StringBuilder();
            for (int day : selectedWeekdays) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(day);
            }
            result.putExtra(EXTRA_WEEKDAYS, sb.toString());
        }

        if (repeatType == REPEAT_DATE) {
            result.putExtra(EXTRA_DATE, selectedDate);
        }

        setResult(RESULT_OK, result);
        finish();
    }

    public static String getRepeatDisplayText(int repeatType, String weekdays, long date) {
        switch (repeatType) {
            case REPEAT_ONCE:
                return "只响一次";
            case REPEAT_DAILY:
                return "每天";
            case REPEAT_WEEKLY:
                if (weekdays == null || weekdays.isEmpty()) {
                    return "星期";
                }
                Set<Integer> days = new HashSet<>();
                for (String day : weekdays.split(",")) {
                    days.add(Integer.parseInt(day));
                }
                String[] dayNames = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                int[] dayValues = {Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                        Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY};
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < dayValues.length; i++) {
                    if (days.contains(dayValues[i])) {
                        if (sb.length() > 0) {
                            sb.append("、");
                        }
                        sb.append(dayNames[i]);
                    }
                }
                return sb.toString();
            case REPEAT_DATE:
                if (date > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(date);
                    return String.format("%d年%d月%d日",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH));
                }
                return "指定日期";
            default:
                return "只响一次";
        }
    }
}