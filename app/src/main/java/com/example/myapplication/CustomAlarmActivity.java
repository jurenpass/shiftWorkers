package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

public class CustomAlarmActivity extends AppCompatActivity {

    private static final String TAG = "CustomAlarmActivity";
    private static final int REQUEST_CODE_RINGTONE = 1001;
    private static final int REQUEST_CODE_REPEAT = 1002;

    private static final int REPEAT_ONCE = 0;
    private static final int REPEAT_DAILY = 1;
    private static final int REPEAT_WEEKLY = 2;
    private static final int REPEAT_DATE = 3;

    private EditText etHour, etMinute, etNote;
    private TextView tvTimeRemaining, tvRingtone, tvRepeat;
    private Switch switchVibrate;
    private int hour = 7, minute = 30;
    private String ringtone = "default";
    private boolean vibrate = true;
    private boolean fade = true;
    private int repeatType = REPEAT_ONCE;
    private String weekdays = "";
    private long date = 0;
    private long lastHourClickTime = 0;
    private long lastMinuteClickTime = 0;
    private float startY = 0;
    private boolean isEditingHour = false;
    private boolean isEditingMinute = false;
    private AlarmSetting editingAlarm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_alarm);

        initViews();
        loadAlarmData();
        updateTimeRemaining();
        setListeners();
    }

    private void initViews() {
        etHour = findViewById(R.id.et_hour);
        etMinute = findViewById(R.id.et_minute);
        etNote = findViewById(R.id.et_note);
        tvTimeRemaining = findViewById(R.id.tv_time_remaining);
        tvRingtone = findViewById(R.id.tv_ringtone);
        tvRepeat = findViewById(R.id.tv_repeat);
        switchVibrate = findViewById(R.id.switch_vibrate);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_save).setOnClickListener(v -> saveAlarm());
        findViewById(R.id.ll_ringtone).setOnClickListener(v -> selectRingtone());
        findViewById(R.id.ll_repeat).setOnClickListener(v -> selectRepeat());
    }

    private void loadAlarmData() {
        Intent intent = getIntent();
        if (intent.hasExtra("alarm")) {
            AlarmSetting alarm = (AlarmSetting) intent.getSerializableExtra("alarm");
            if (alarm != null) {
                editingAlarm = alarm;
                hour = alarm.getHour();
                minute = alarm.getMinute();
                ringtone = alarm.getRingtone();
                vibrate = alarm.isVibrate();
                fade = alarm.isFade();
                repeatType = alarm.getRepeatType();
                weekdays = alarm.getWeekdays();
                date = alarm.getRepeatDate();

                etHour.setText(String.format("%02d", hour));
                etMinute.setText(String.format("%02d", minute));
                etNote.setText(alarm.getReminderType());
                switchVibrate.setChecked(vibrate);
                updateRingtoneDisplay();
                updateRepeatDisplay();
            }
        } else {
            Calendar calendar = Calendar.getInstance();
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            minute = calendar.get(Calendar.MINUTE);
            etHour.setText(String.format("%02d", hour));
            etMinute.setText(String.format("%02d", minute));
        }
    }

    private void updateTimeRemaining() {
        Calendar now = Calendar.getInstance();
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.set(Calendar.HOUR_OF_DAY, hour);
        alarmTime.set(Calendar.MINUTE, minute);
        alarmTime.set(Calendar.SECOND, 0);
        alarmTime.set(Calendar.MILLISECOND, 0);

        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 1);
        }

        long diff = alarmTime.getTimeInMillis() - now.getTimeInMillis();
        long hours = diff / (1000 * 60 * 60);
        long minutes = (diff % (1000 * 60 * 60)) / (1000 * 60);

        String remaining;
        if (hours > 0) {
            remaining = String.format("%d小时%d分钟后响铃", hours, minutes);
        } else {
            remaining = String.format("%d分钟后响铃", minutes);
        }
        tvTimeRemaining.setText(remaining);
    }

    private void setListeners() {
        ImageView ivHourUp = findViewById(R.id.iv_hour_up);
        ImageView ivHourDown = findViewById(R.id.iv_hour_down);
        ImageView ivMinuteUp = findViewById(R.id.iv_minute_up);
        ImageView ivMinuteDown = findViewById(R.id.iv_minute_down);

        ivHourUp.setOnClickListener(v -> {
            hour = (hour + 1) % 24;
            etHour.setText(String.format("%02d", hour));
            updateTimeRemaining();
            clearFocusAndHideKeyboard();
        });

        ivHourDown.setOnClickListener(v -> {
            hour = (hour - 1 + 24) % 24;
            etHour.setText(String.format("%02d", hour));
            updateTimeRemaining();
            clearFocusAndHideKeyboard();
        });

        ivMinuteUp.setOnClickListener(v -> {
            minute = (minute + 1) % 60;
            etMinute.setText(String.format("%02d", minute));
            updateTimeRemaining();
            clearFocusAndHideKeyboard();
        });

        ivMinuteDown.setOnClickListener(v -> {
            minute = (minute - 1 + 60) % 60;
            etMinute.setText(String.format("%02d", minute));
            updateTimeRemaining();
            clearFocusAndHideKeyboard();
        });

        etHour.setOnClickListener(v -> handleTimeClick(true));
        etMinute.setOnClickListener(v -> handleTimeClick(false));

        etHour.setOnTouchListener((v, event) -> handleTouch(v, event, true));
        etMinute.setOnTouchListener((v, event) -> handleTouch(v, event, false));

        etHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int newHour = Integer.parseInt(s.toString());
                        if (newHour >= 0 && newHour <= 23) {
                            hour = newHour;
                            updateTimeRemaining();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMinute.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int newMinute = Integer.parseInt(s.toString());
                        if (newMinute >= 0 && newMinute <= 59) {
                            minute = newMinute;
                            updateTimeRemaining();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        switchVibrate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrate = isChecked;
        });
    }

    private void handleTimeClick(boolean isHour) {
        long currentTime = System.currentTimeMillis();
        long lastClickTime = isHour ? lastHourClickTime : lastMinuteClickTime;

        if (currentTime - lastClickTime < 300) {
            EditText et = isHour ? etHour : etMinute;
            et.setFocusable(true);
            et.setFocusableInTouchMode(true);
            et.setCursorVisible(true);
            et.selectAll();
            et.requestFocus();
            if (isHour) {
                isEditingHour = true;
            } else {
                isEditingMinute = true;
            }
        }

        if (isHour) {
            lastHourClickTime = currentTime;
        } else {
            lastMinuteClickTime = currentTime;
        }
    }

    private boolean handleTouch(View v, MotionEvent event, boolean isHour) {
        if ((isHour && isEditingHour) || (!isHour && isEditingMinute)) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = startY - event.getY();
                if (deltaY > 30) {
                    if (isHour) {
                        hour = (hour + 1) % 24;
                        etHour.setText(String.format("%02d", hour));
                    } else {
                        minute = (minute + 1) % 60;
                        etMinute.setText(String.format("%02d", minute));
                    }
                    updateTimeRemaining();
                    startY = event.getY();
                } else if (deltaY < -30) {
                    if (isHour) {
                        hour = (hour - 1 + 24) % 24;
                        etHour.setText(String.format("%02d", hour));
                    } else {
                        minute = (minute - 1 + 60) % 60;
                        etMinute.setText(String.format("%02d", minute));
                    }
                    updateTimeRemaining();
                    startY = event.getY();
                }
                break;
        }
        return true;
    }

    private void clearFocusAndHideKeyboard() {
        etHour.clearFocus();
        etHour.setFocusable(false);
        etHour.setFocusableInTouchMode(false);
        etHour.setCursorVisible(false);
        etMinute.clearFocus();
        etMinute.setFocusable(false);
        etMinute.setFocusableInTouchMode(false);
        etMinute.setCursorVisible(false);
        isEditingHour = false;
        isEditingMinute = false;
        hideKeyboard();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    private void selectRingtone() {
        Intent intent = new Intent(this, RingtoneSelectActivity.class);
        intent.putExtra("selected_ringtone", ringtone);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    private void selectRepeat() {
        Intent intent = new Intent(this, RepeatSettingActivity.class);
        intent.putExtra(RepeatSettingActivity.EXTRA_REPEAT_TYPE, repeatType);
        intent.putExtra(RepeatSettingActivity.EXTRA_WEEKDAYS, weekdays);
        intent.putExtra(RepeatSettingActivity.EXTRA_DATE, date);
        startActivityForResult(intent, REQUEST_CODE_REPEAT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK && data != null) {
            ringtone = data.getStringExtra("ringtone");
            updateRingtoneDisplay();
        } else if (requestCode == REQUEST_CODE_REPEAT && resultCode == RESULT_OK && data != null) {
            repeatType = data.getIntExtra(RepeatSettingActivity.EXTRA_REPEAT_TYPE, REPEAT_ONCE);
            weekdays = data.getStringExtra(RepeatSettingActivity.EXTRA_WEEKDAYS);
            date = data.getLongExtra(RepeatSettingActivity.EXTRA_DATE, 0);
            updateRepeatDisplay();
        }
    }

    private void updateRingtoneDisplay() {
        if ("default".equals(ringtone)) {
            tvRingtone.setText("默认铃声");
        } else {
            tvRingtone.setText("自定义铃声");
        }
    }

    private void updateRepeatDisplay() {
        tvRepeat.setText(getRepeatDisplayText(repeatType, weekdays, date));
        tvRepeat.setTextColor(getResources().getColor(android.R.color.black));
    }

    private String getRepeatDisplayText(int repeatType, String weekdays, long date) {
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
                int count = 0;
                for (int i = 0; i < dayValues.length; i++) {
                    if (days.contains(dayValues[i])) {
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
            case REPEAT_DATE:
                if (date > 0) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(date);
                    String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                    return String.format("%d年%d月%d日 %s",
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH),
                            weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]);
                }
                return "指定日期";
            default:
                return "只响一次";
        }
    }

    private void saveAlarm() {
        String note = etNote.getText().toString().trim();

        AlarmManager alarmManager = new AlarmManager(this);

        if (editingAlarm != null) {
            editingAlarm.setReminderType(note.isEmpty() ? "闹钟" : note);
            editingAlarm.setHour(hour);
            editingAlarm.setMinute(minute);
            editingAlarm.setVibrate(vibrate);
            editingAlarm.setRingtone(ringtone);
            editingAlarm.setFade(fade);
            editingAlarm.setRepeatType(repeatType);
            editingAlarm.setWeekdays(weekdays);
            editingAlarm.setRepeatDate(date);
            alarmManager.updateAlarm(editingAlarm);
            Toast.makeText(this, "闹钟已更新", Toast.LENGTH_SHORT).show();
        } else {
            AlarmSetting alarm = new AlarmSetting();
            alarm.setTeam("普通闹钟");
            alarm.setShiftType("普通闹钟");
            alarm.setReminderType(note.isEmpty() ? "闹钟" : note);
            alarm.setHour(hour);
            alarm.setMinute(minute);
            alarm.setEnabled(true);
            alarm.setVibrate(vibrate);
            alarm.setRingtone(ringtone);
            alarm.setFade(fade);
            alarm.setRepeatType(repeatType);
            alarm.setWeekdays(weekdays);
            alarm.setRepeatDate(date);
            alarmManager.addAlarm(alarm);
            Toast.makeText(this, "闹钟已添加", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}