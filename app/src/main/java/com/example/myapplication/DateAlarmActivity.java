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

public class DateAlarmActivity extends AppCompatActivity {

    private static final String TAG = "DateAlarmActivity";
    private static final int REQUEST_CODE_RINGTONE = 1001;

    private EditText etHour, etMinute, etNote;
    private TextView tvDate, tvRingtone, tvSave;
    private Switch switchVibrate, switchFade;
    private int hour = 7, minute = 30;
    private String ringtone = "default";
    private boolean vibrate = true;
    private boolean fade = true;
    private long targetDate = 0;
    private long lastHourClickTime = 0;
    private long lastMinuteClickTime = 0;
    private float startY = 0;
    private boolean isEditingHour = false;
    private boolean isEditingMinute = false;
    private AlarmSetting editingAlarm = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_alarm);

        initViews();
        loadData();
        setListeners();
    }

    private void initViews() {
        etHour = findViewById(R.id.et_hour);
        etMinute = findViewById(R.id.et_minute);
        etNote = findViewById(R.id.et_note);
        tvDate = findViewById(R.id.tv_date);
        tvRingtone = findViewById(R.id.tv_ringtone);
        tvSave = findViewById(R.id.tv_save);
        switchVibrate = findViewById(R.id.switch_vibrate);
        switchFade = findViewById(R.id.switch_fade);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        tvSave.setOnClickListener(v -> saveAlarm());
        findViewById(R.id.ll_ringtone).setOnClickListener(v -> selectRingtone());
    }

    private void loadData() {
        Intent intent = getIntent();
        
        targetDate = intent.getLongExtra("date", 0);
        if (targetDate == 0) {
            targetDate = System.currentTimeMillis();
        }

        if (intent.hasExtra("alarm")) {
            AlarmSetting alarm = (AlarmSetting) intent.getSerializableExtra("alarm");
            if (alarm != null) {
                editingAlarm = alarm;
                hour = alarm.getHour();
                minute = alarm.getMinute();
                ringtone = alarm.getRingtone();
                vibrate = alarm.isVibrate();
                fade = alarm.isFade();
                
                if (alarm.getRepeatDate() > 0) {
                    targetDate = alarm.getRepeatDate();
                }

                etHour.setText(String.format("%02d", hour));
                etMinute.setText(String.format("%02d", minute));
                etNote.setText(alarm.getReminderType());
                switchVibrate.setChecked(vibrate);
                switchFade.setChecked(fade);
                updateRingtoneDisplay();
            }
        } else {
            Calendar now = Calendar.getInstance();
            hour = now.get(Calendar.HOUR_OF_DAY);
            minute = now.get(Calendar.MINUTE);
            etHour.setText(String.format("%02d", hour));
            etMinute.setText(String.format("%02d", minute));
            switchVibrate.setChecked(vibrate);
            switchFade.setChecked(fade);
        }

        updateDateDisplay();
    }

    private void updateDateDisplay() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(targetDate);
        String dateStr = String.format("%d年%d月%d日", 
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH));
        tvDate.setText(dateStr);
    }

    private void updateRingtoneDisplay() {
        if ("default".equals(ringtone)) {
            tvRingtone.setText("默认铃声");
        } else {
            tvRingtone.setText(ringtone);
        }
    }

    private void selectRingtone() {
        Intent intent = new Intent(this, RingtoneSelectActivity.class);
        intent.putExtra("currentRingtone", ringtone);
        startActivityForResult(intent, REQUEST_CODE_RINGTONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RINGTONE && resultCode == RESULT_OK) {
            ringtone = data.getStringExtra("ringtone");
            updateRingtoneDisplay();
        }
    }

    private void setListeners() {
        etHour.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isEditingHour) return;
                try {
                    int newHour = Integer.parseInt(s.toString());
                    if (newHour >= 0 && newHour <= 23) {
                        hour = newHour;
                    }
                } catch (NumberFormatException e) {
                    hour = 0;
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
                if (!isEditingMinute) return;
                try {
                    int newMinute = Integer.parseInt(s.toString());
                    if (newMinute >= 0 && newMinute <= 59) {
                        minute = newMinute;
                    }
                } catch (NumberFormatException e) {
                    minute = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etHour.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastHourClickTime < 300) {
                isEditingHour = true;
                etHour.selectAll();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(etHour, InputMethodManager.SHOW_IMPLICIT);
            }
            lastHourClickTime = currentTime;
        });

        etMinute.setOnClickListener(v -> {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastMinuteClickTime < 300) {
                isEditingMinute = true;
                etMinute.selectAll();
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.showSoftInput(etMinute, InputMethodManager.SHOW_IMPLICIT);
            }
            lastMinuteClickTime = currentTime;
        });

        findViewById(R.id.iv_hour_up).setOnClickListener(v -> {
            hour = (hour + 1) % 24;
            etHour.setText(String.format("%02d", hour));
        });

        findViewById(R.id.iv_hour_down).setOnClickListener(v -> {
            hour = (hour - 1 + 24) % 24;
            etHour.setText(String.format("%02d", hour));
        });

        findViewById(R.id.iv_minute_up).setOnClickListener(v -> {
            minute = (minute + 1) % 60;
            etMinute.setText(String.format("%02d", minute));
        });

        findViewById(R.id.iv_minute_down).setOnClickListener(v -> {
            minute = (minute - 1 + 60) % 60;
            etMinute.setText(String.format("%02d", minute));
        });

        etHour.setOnTouchListener((v, event) -> handleTouch(event, true));
        etMinute.setOnTouchListener((v, event) -> handleTouch(event, false));
    }

    private boolean handleTouch(MotionEvent event, boolean isHour) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                return false;
            case MotionEvent.ACTION_MOVE:
                float deltaY = startY - event.getY();
                if (Math.abs(deltaY) > 30) {
                    if (deltaY > 0) {
                        if (isHour) {
                            hour = (hour + 1) % 24;
                            etHour.setText(String.format("%02d", hour));
                        } else {
                            minute = (minute + 1) % 60;
                            etMinute.setText(String.format("%02d", minute));
                        }
                    } else {
                        if (isHour) {
                            hour = (hour - 1 + 24) % 24;
                            etHour.setText(String.format("%02d", hour));
                        } else {
                            minute = (minute - 1 + 60) % 60;
                            etMinute.setText(String.format("%02d", minute));
                        }
                    }
                    startY = event.getY();
                }
                return true;
            case MotionEvent.ACTION_UP:
                return true;
        }
        return false;
    }

    private void saveAlarm() {
        vibrate = switchVibrate.isChecked();
        fade = switchFade.isChecked();
        String note = etNote.getText().toString().trim();

        AlarmManager alarmManager = new AlarmManager(this);

        if (editingAlarm != null) {
            editingAlarm.setHour(hour);
            editingAlarm.setMinute(minute);
            editingAlarm.setRingtone(ringtone);
            editingAlarm.setVibrate(vibrate);
            editingAlarm.setFade(fade);
            editingAlarm.setReminderType(note.isEmpty() ? "闹钟" : note);
            editingAlarm.setRepeatType(3);
            editingAlarm.setRepeatDate(targetDate);
            alarmManager.updateAlarm(editingAlarm);
            Toast.makeText(this, "闹钟已更新", Toast.LENGTH_SHORT).show();
        } else {
            AlarmSetting alarm = new AlarmSetting();
            alarm.setTeam("日历闹钟");
            alarm.setShiftType("日历闹钟");
            alarm.setReminderType(note.isEmpty() ? "闹钟" : note);
            alarm.setHour(hour);
            alarm.setMinute(minute);
            alarm.setEnabled(true);
            alarm.setVibrate(vibrate);
            alarm.setFade(fade);
            alarm.setRingtone(ringtone);
            alarm.setRepeatType(3);
            alarm.setRepeatDate(targetDate);
            alarmManager.addAlarm(alarm);
            Toast.makeText(this, "闹钟已添加", Toast.LENGTH_SHORT).show();
        }

        finish();
    }
}