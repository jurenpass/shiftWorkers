package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import java.util.Calendar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

public class AlarmSettingActivity extends AppCompatActivity {

    private EditText etHour, etMinute;
    private TextView tvRingtone;
    private int hour, minute;
    private String shiftType = "白班";
    private String reminderType = "上班";
    private String ringtone = "default";
    private boolean vibrate = true;
    private boolean fade = true;
    private AlarmManager alarmManager;
    private AlarmSetting editAlarm;
    private float lastY = 0;
    private float startY = 0;
    private boolean isScrolling = false;
    private static final int SCROLL_THRESHOLD = 30;
    private static final int REQUEST_CODE_RINGTONE = 1001;
    private long lastClickTime = 0;
    private static final long DOUBLE_CLICK_THRESHOLD = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_setting);

        alarmManager = new AlarmManager(this);

        Calendar now = Calendar.getInstance();
        hour = now.get(Calendar.HOUR_OF_DAY);
        minute = now.get(Calendar.MINUTE);

        etHour = findViewById(R.id.et_hour);
        etMinute = findViewById(R.id.et_minute);
        tvRingtone = findViewById(R.id.tv_ringtone);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_save).setOnClickListener(v -> saveAlarm());
        findViewById(R.id.ll_ringtone).setOnClickListener(v -> selectRingtone());
        findViewById(R.id.iv_hour_up).setOnClickListener(v -> {
            hour = (hour + 1) % 24;
            updateTimeDisplay();
            clearFocusAndHideKeyboard();
        });
        findViewById(R.id.iv_hour_down).setOnClickListener(v -> {
            hour = (hour - 1 + 24) % 24;
            updateTimeDisplay();
            clearFocusAndHideKeyboard();
        });
        findViewById(R.id.iv_minute_up).setOnClickListener(v -> {
            minute = (minute + 1) % 60;
            updateTimeDisplay();
            clearFocusAndHideKeyboard();
        });
        findViewById(R.id.iv_minute_down).setOnClickListener(v -> {
            minute = (minute - 1 + 60) % 60;
            updateTimeDisplay();
            clearFocusAndHideKeyboard();
        });

        setupTouchListeners();
        setupFocusListeners();
        setupInputFilters();
        setupTextWatchers();
        initSpinners();

        editAlarm = (AlarmSetting) getIntent().getSerializableExtra("alarm");
        if (editAlarm != null) {
            hour = editAlarm.getHour();
            minute = editAlarm.getMinute();
            shiftType = editAlarm.getShiftType();
            reminderType = editAlarm.getReminderType();
            ringtone = editAlarm.getRingtone();
            vibrate = editAlarm.isVibrate();
            fade = editAlarm.isFade();
            updateTimeDisplay();
            updateRingtoneDisplay();
            ((Spinner) findViewById(R.id.sp_shift_type)).setSelection(getShiftIndex(shiftType));
            ((Spinner) findViewById(R.id.sp_reminder_type)).setSelection(getReminderIndex(reminderType));
            ((Switch) findViewById(R.id.switch_vibrate)).setChecked(vibrate);
            ((Switch) findViewById(R.id.switch_fade)).setChecked(fade);
        } else {
            updateTimeDisplay();
            updateRingtoneDisplay();
            ((Spinner) findViewById(R.id.sp_reminder_type)).setSelection(0);
            ((Switch) findViewById(R.id.switch_vibrate)).setChecked(true);
            ((Switch) findViewById(R.id.switch_fade)).setChecked(true);
        }

        ((Switch) findViewById(R.id.switch_vibrate)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            vibrate = isChecked;
        });

        ((Switch) findViewById(R.id.switch_fade)).setOnCheckedChangeListener((buttonView, isChecked) -> {
            fade = isChecked;
        });
    }

    private void setupTouchListeners() {
        etHour.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        etHour.setFocusable(false);
                        etHour.setFocusableInTouchMode(false);
                        etHour.setCursorVisible(false);
                        startY = event.getY();
                        lastY = event.getY();
                        isScrolling = false;
                        lastClickTime = System.currentTimeMillis();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        etHour.clearFocus();
                        etHour.setFocusable(false);
                        etHour.setFocusableInTouchMode(false);
                        etHour.setCursorVisible(false);
                        etMinute.clearFocus();
                        etMinute.setFocusable(false);
                        etMinute.setFocusableInTouchMode(false);
                        etMinute.setCursorVisible(false);
                        hideKeyboard();

                        float currentY = event.getY();
                        float deltaY = currentY - startY;
                        if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                            isScrolling = true;
                        }
                        if (isScrolling) {
                            float moveDelta = lastY - currentY;
                            if (moveDelta > 30) {
                                hour = (hour + 1) % 24;
                                updateTimeDisplay();
                                lastY = currentY;
                            } else if (moveDelta < -30) {
                                hour = (hour - 1 + 24) % 24;
                                updateTimeDisplay();
                                lastY = currentY;
                            }
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        if (!isScrolling) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
                                etHour.setFocusable(true);
                                etHour.setFocusableInTouchMode(true);
                                etHour.setCursorVisible(true);
                                etHour.requestFocus();
                                int length = etHour.getText().length();
                                etHour.setSelection(0, length);
                                showKeyboard();
                            }
                        }
                        isScrolling = false;
                        return true;
                }
                return false;
            }
        });

        etMinute.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        etMinute.setFocusable(false);
                        etMinute.setFocusableInTouchMode(false);
                        etMinute.setCursorVisible(false);
                        startY = event.getY();
                        lastY = event.getY();
                        isScrolling = false;
                        lastClickTime = System.currentTimeMillis();
                        return false;
                    case MotionEvent.ACTION_MOVE:
                        etHour.clearFocus();
                        etHour.setFocusable(false);
                        etHour.setFocusableInTouchMode(false);
                        etHour.setCursorVisible(false);
                        etMinute.clearFocus();
                        etMinute.setFocusable(false);
                        etMinute.setFocusableInTouchMode(false);
                        etMinute.setCursorVisible(false);
                        hideKeyboard();

                        float currentY = event.getY();
                        float deltaY = currentY - startY;
                        if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                            isScrolling = true;
                        }
                        if (isScrolling) {
                            float moveDelta = lastY - currentY;
                            if (moveDelta > 30) {
                                minute = (minute + 1) % 60;
                                updateTimeDisplay();
                                lastY = currentY;
                            } else if (moveDelta < -30) {
                                minute = (minute - 1 + 60) % 60;
                                updateTimeDisplay();
                                lastY = currentY;
                            }
                            return true;
                        }
                        return false;
                    case MotionEvent.ACTION_UP:
                        if (!isScrolling) {
                            long currentTime = System.currentTimeMillis();
                            if (currentTime - lastClickTime < DOUBLE_CLICK_THRESHOLD) {
                                etMinute.setFocusable(true);
                                etMinute.setFocusableInTouchMode(true);
                                etMinute.setCursorVisible(true);
                                etMinute.requestFocus();
                                int length = etMinute.getText().length();
                                etMinute.setSelection(0, length);
                                showKeyboard();
                            }
                        }
                        isScrolling = false;
                        return true;
                }
                return false;
            }
        });
    }

    private void setupFocusListeners() {
        etHour.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (!isHourValid()) {
                    etHour.setFocusable(true);
                    etHour.setFocusableInTouchMode(true);
                    etHour.setCursorVisible(true);
                    etHour.requestFocus();
                    etHour.selectAll();
                } else {
                    etHour.setFocusable(false);
                    etHour.setFocusableInTouchMode(false);
                    etHour.setCursorVisible(false);
                    updateHourFromEditText();
                }
            }
        });

        etMinute.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                if (!isMinuteValid()) {
                    etMinute.setFocusable(true);
                    etMinute.setFocusableInTouchMode(true);
                    etMinute.setCursorVisible(true);
                    etMinute.requestFocus();
                    etMinute.selectAll();
                } else {
                    etMinute.setFocusable(false);
                    etMinute.setFocusableInTouchMode(false);
                    etMinute.setCursorVisible(false);
                    updateMinuteFromEditText();
                }
            }
        });
    }

    private void setupInputFilters() {
        InputFilter hourFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                String current = dest.toString();
                String newStr = current.substring(0, dstart) + 
                               source.toString() + 
                               current.substring(dend);
                
                if (newStr.isEmpty()) {
                    return source;
                }
                
                if (newStr.length() > 2) {
                    return "";
                }
                
                try {
                    int value = Integer.parseInt(newStr);
                    if (value < 0 || value > 23) {
                        return "";
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
                
                return source;
            }
        };

        InputFilter minuteFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                String current = dest.toString();
                String newStr = current.substring(0, dstart) + 
                               source.toString() + 
                               current.substring(dend);
                
                if (newStr.isEmpty()) {
                    return source;
                }
                
                if (newStr.length() > 2) {
                    return "";
                }
                
                try {
                    int value = Integer.parseInt(newStr);
                    if (value < 0 || value > 59) {
                        return "";
                    }
                } catch (NumberFormatException e) {
                    return "";
                }
                
                return source;
            }
        };

        etHour.setFilters(new InputFilter[]{hourFilter});
        etMinute.setFilters(new InputFilter[]{minuteFilter});
    }

    private void setupTextWatchers() {
        etHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etMinute.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void validateAndFormatHour() {
        String hourStr = etHour.getText().toString().trim();
        if (hourStr.isEmpty()) {
            etHour.setText("00");
            return;
        }
        
        try {
            int h = Integer.parseInt(hourStr);
            if (h < 0) {
                etHour.setText("00");
            } else if (h > 23) {
                etHour.setText("23");
            } else if (hourStr.length() == 1) {
                etHour.setText("0" + hourStr);
            }
        } catch (NumberFormatException e) {
            etHour.setText("00");
        }
    }

    private void validateAndFormatMinute() {
        String minuteStr = etMinute.getText().toString().trim();
        if (minuteStr.isEmpty()) {
            etMinute.setText("00");
            return;
        }
        
        try {
            int m = Integer.parseInt(minuteStr);
            if (m < 0) {
                etMinute.setText("00");
            } else if (m > 59) {
                etMinute.setText("59");
            } else if (minuteStr.length() == 1) {
                etMinute.setText("0" + minuteStr);
            }
        } catch (NumberFormatException e) {
            etMinute.setText("00");
        }
    }

    private boolean isHourValid() {
        String hourStr = etHour.getText().toString().trim();
        if (hourStr.isEmpty()) return false;
        try {
            int h = Integer.parseInt(hourStr);
            return h >= 0 && h <= 23;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isMinuteValid() {
        String minuteStr = etMinute.getText().toString().trim();
        if (minuteStr.isEmpty()) return false;
        try {
            int m = Integer.parseInt(minuteStr);
            return m >= 0 && m <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void updateHourFromEditText() {
        String hourStr = etHour.getText().toString().trim();
        if (!hourStr.isEmpty()) {
            try {
                int h = Integer.parseInt(hourStr);
                if (h >= 0 && h <= 23) {
                    hour = h;
                }
            } catch (NumberFormatException e) {
            }
        }
    }

    private void updateMinuteFromEditText() {
        String minuteStr = etMinute.getText().toString().trim();
        if (!minuteStr.isEmpty()) {
            try {
                int m = Integer.parseInt(minuteStr);
                if (m >= 0 && m <= 59) {
                    minute = m;
                }
            } catch (NumberFormatException e) {
            }
        }
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
        hideKeyboard();
    }

    private void initSpinners() {
        String[] shiftTypes = {"白班", "上夜班", "下夜班", "正休"};
        ArrayAdapter<String> shiftAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, shiftTypes);
        shiftAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner shiftSpinner = findViewById(R.id.sp_shift_type);
        shiftSpinner.setAdapter(shiftAdapter);
        shiftSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                shiftType = shiftTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        String[] reminderTypes = {"上班", "起床", "打卡", "接班"};
        ArrayAdapter<String> reminderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, reminderTypes);
        reminderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner reminderSpinner = findViewById(R.id.sp_reminder_type);
        reminderSpinner.setAdapter(reminderAdapter);
        reminderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                reminderType = reminderTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void updateTimeDisplay() {
        etHour.setText(String.format("%02d", hour));
        etMinute.setText(String.format("%02d", minute));
    }

    private void updateRingtoneDisplay() {
        if ("default".equals(ringtone)) {
            tvRingtone.setText("默认铃声");
        } else {
            tvRingtone.setText("自定义铃声");
        }
    }

    private void selectRingtone() {
        Intent intent = new Intent(this, RingtoneSelectActivity.class);
        intent.putExtra("selected_ringtone", ringtone);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK && data != null) {
            ringtone = data.getStringExtra("ringtone");
            updateRingtoneDisplay();
        }
    }

    private void saveAlarm() {
        updateHourFromEditText();
        updateMinuteFromEditText();

        if (hour < 0 || hour > 23) {
            Toast.makeText(this, "小时范围0-23", Toast.LENGTH_SHORT).show();
            return;
        }
        if (minute < 0 || minute > 59) {
            Toast.makeText(this, "分钟范围0-59", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editAlarm != null) {
            editAlarm.setHour(hour);
            editAlarm.setMinute(minute);
            editAlarm.setShiftType(shiftType);
            editAlarm.setReminderType(reminderType);
            editAlarm.setRingtone(ringtone);
            editAlarm.setVibrate(vibrate);
            editAlarm.setFade(fade);
            alarmManager.updateAlarm(editAlarm);
            Toast.makeText(this, "闹钟已更新", Toast.LENGTH_SHORT).show();
        } else {
            AlarmSetting alarm = new AlarmSetting(null, shiftType, hour, minute, reminderType, true);
            alarm.setRingtone(ringtone);
            alarm.setVibrate(vibrate);
            alarm.setFade(fade);
            alarmManager.addAlarm(alarm);
            Toast.makeText(this, "闹钟已添加", Toast.LENGTH_SHORT).show();
        }

        alarmManager.scheduleAllAlarms();
        finish();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.showSoftInput(getCurrentFocus(), InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }

    private int getShiftIndex(String shiftType) {
        String[] types = {"白班", "上夜班", "下夜班", "正休"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(shiftType)) {
                return i;
            }
        }
        return 0;
    }

    private int getReminderIndex(String reminderType) {
        String[] types = {"上班", "起床", "打卡", "接班"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(reminderType)) {
                return i;
            }
        }
        return 0;
    }
}