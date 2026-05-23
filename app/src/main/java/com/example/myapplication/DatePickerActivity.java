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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class DatePickerActivity extends AppCompatActivity {

    public static final String EXTRA_DATE = "date";

    private EditText etYear, etMonth, etDay;
    private TextView tvDateInfo;
    private int year, month, day;
    private long lastYearClickTime = 0;
    private long lastMonthClickTime = 0;
    private long lastDayClickTime = 0;
    private float startY = 0;
    private boolean isEditingYear = false;
    private boolean isEditingMonth = false;
    private boolean isEditingDay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_picker);

        initViews();
        setCurrentDate();
        updateDateInfo();
        setListeners();
    }

    private void initViews() {
        etYear = findViewById(R.id.et_year);
        etMonth = findViewById(R.id.et_month);
        etDay = findViewById(R.id.et_day);
        tvDateInfo = findViewById(R.id.tv_date_info);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_save).setOnClickListener(v -> saveAndReturn());
    }

    private void setCurrentDate() {
        long date = getIntent().getLongExtra(EXTRA_DATE, 0);
        Calendar cal;
        if (date > 0) {
            cal = Calendar.getInstance();
            cal.setTimeInMillis(date);
        } else {
            cal = Calendar.getInstance();
        }
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH) + 1;
        day = cal.get(Calendar.DAY_OF_MONTH);

        etYear.setText(String.format("%04d", year));
        etMonth.setText(String.format("%02d", month));
        etDay.setText(String.format("%02d", day));
    }

    private void updateDateInfo() {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        String dateInfo = String.format("%d年%d月%d日 %s", year, month, day, weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1]);
        tvDateInfo.setText(dateInfo);
    }

    private void setListeners() {
        ImageView ivYearUp = findViewById(R.id.iv_year_up);
        ImageView ivYearDown = findViewById(R.id.iv_year_down);
        ImageView ivMonthUp = findViewById(R.id.iv_month_up);
        ImageView ivMonthDown = findViewById(R.id.iv_month_down);
        ImageView ivDayUp = findViewById(R.id.iv_day_up);
        ImageView ivDayDown = findViewById(R.id.iv_day_down);

        ivYearUp.setOnClickListener(v -> {
            year = Math.min(year + 1, 2050);
            etYear.setText(String.format("%04d", year));
            validateDay();
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        ivYearDown.setOnClickListener(v -> {
            year = Math.max(year - 1, 2018);
            etYear.setText(String.format("%04d", year));
            validateDay();
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        ivMonthUp.setOnClickListener(v -> {
            if (month >= 12) {
                month = 1;
                year++;
                etYear.setText(String.format("%04d", year));
            } else {
                month++;
            }
            etMonth.setText(String.format("%02d", month));
            validateDay();
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        ivMonthDown.setOnClickListener(v -> {
            if (month <= 1) {
                month = 12;
                year--;
                etYear.setText(String.format("%04d", year));
            } else {
                month--;
            }
            etMonth.setText(String.format("%02d", month));
            validateDay();
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        ivDayUp.setOnClickListener(v -> {
            int maxDay = getMaxDay(year, month);
            if (day >= maxDay) {
                day = 1;
                ivMonthUp.performClick();
            } else {
                day++;
            }
            etDay.setText(String.format("%02d", day));
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        ivDayDown.setOnClickListener(v -> {
            if (day <= 1) {
                ivMonthDown.performClick();
                day = getMaxDay(year, month);
            } else {
                day--;
            }
            etDay.setText(String.format("%02d", day));
            updateDateInfo();
            clearFocusAndHideKeyboard();
        });

        etYear.setOnClickListener(v -> handleClick(true, false, false));
        etMonth.setOnClickListener(v -> handleClick(false, true, false));
        etDay.setOnClickListener(v -> handleClick(false, false, true));

        etYear.setOnTouchListener((v, event) -> handleTouch(v, event, true, false, false));
        etMonth.setOnTouchListener((v, event) -> handleTouch(v, event, false, true, false));
        etDay.setOnTouchListener((v, event) -> handleTouch(v, event, false, false, true));

        etYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 4) {
                    try {
                        int newYear = Integer.parseInt(s.toString());
                        if (newYear >= 2018 && newYear <= 2050) {
                            year = newYear;
                            validateDay();
                            updateDateInfo();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etMonth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int newMonth = Integer.parseInt(s.toString());
                        if (newMonth >= 1 && newMonth <= 12) {
                            month = newMonth;
                            validateDay();
                            updateDateInfo();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        etDay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    try {
                        int newDay = Integer.parseInt(s.toString());
                        int maxDay = getMaxDay(year, month);
                        if (newDay >= 1 && newDay <= maxDay) {
                            day = newDay;
                            updateDateInfo();
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void handleClick(boolean isYear, boolean isMonth, boolean isDay) {
        long currentTime = System.currentTimeMillis();
        long lastClickTime = isYear ? lastYearClickTime : (isMonth ? lastMonthClickTime : lastDayClickTime);

        if (currentTime - lastClickTime < 300) {
            EditText et = isYear ? etYear : (isMonth ? etMonth : etDay);
            et.setFocusable(true);
            et.setFocusableInTouchMode(true);
            et.setCursorVisible(true);
            et.selectAll();
            et.requestFocus();
            if (isYear) isEditingYear = true;
            else if (isMonth) isEditingMonth = true;
            else isEditingDay = true;
        }

        if (isYear) lastYearClickTime = currentTime;
        else if (isMonth) lastMonthClickTime = currentTime;
        else lastDayClickTime = currentTime;
    }

    private boolean handleTouch(View v, MotionEvent event, boolean isYear, boolean isMonth, boolean isDay) {
        if ((isYear && isEditingYear) || (isMonth && isEditingMonth) || (isDay && isEditingDay)) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float deltaY = startY - event.getY();
                if (deltaY > 30) {
                    if (isYear) {
                        year = Math.min(year + 1, 2050);
                        etYear.setText(String.format("%04d", year));
                    } else if (isMonth) {
                        if (month >= 12) {
                            month = 1;
                            year++;
                            etYear.setText(String.format("%04d", year));
                        } else {
                            month++;
                        }
                        etMonth.setText(String.format("%02d", month));
                    } else {
                        int maxDay = getMaxDay(year, month);
                        if (day >= maxDay) {
                            day = 1;
                            month = month >= 12 ? 1 : month + 1;
                            if (month == 1) {
                                year++;
                                etYear.setText(String.format("%04d", year));
                            }
                            etMonth.setText(String.format("%02d", month));
                        } else {
                            day++;
                        }
                        etDay.setText(String.format("%02d", day));
                    }
                    validateDay();
                    updateDateInfo();
                    startY = event.getY();
                } else if (deltaY < -30) {
                    if (isYear) {
                        year = Math.max(year - 1, 2018);
                        etYear.setText(String.format("%04d", year));
                    } else if (isMonth) {
                        if (month <= 1) {
                            month = 12;
                            year--;
                            etYear.setText(String.format("%04d", year));
                        } else {
                            month--;
                        }
                        etMonth.setText(String.format("%02d", month));
                    } else {
                        if (day <= 1) {
                            month = month <= 1 ? 12 : month - 1;
                            if (month == 12) {
                                year--;
                                etYear.setText(String.format("%04d", year));
                            }
                            etMonth.setText(String.format("%02d", month));
                            day = getMaxDay(year, month);
                        } else {
                            day--;
                        }
                        etDay.setText(String.format("%02d", day));
                    }
                    validateDay();
                    updateDateInfo();
                    startY = event.getY();
                }
                break;
        }
        return true;
    }

    private void validateDay() {
        int maxDay = getMaxDay(year, month);
        if (day > maxDay) {
            day = maxDay;
            etDay.setText(String.format("%02d", day));
        }
    }

    private int getMaxDay(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    private void clearFocusAndHideKeyboard() {
        etYear.clearFocus();
        etYear.setFocusable(false);
        etYear.setFocusableInTouchMode(false);
        etYear.setCursorVisible(false);
        etMonth.clearFocus();
        etMonth.setFocusable(false);
        etMonth.setFocusableInTouchMode(false);
        etMonth.setCursorVisible(false);
        etDay.clearFocus();
        etDay.setFocusable(false);
        etDay.setFocusableInTouchMode(false);
        etDay.setCursorVisible(false);
        isEditingYear = false;
        isEditingMonth = false;
        isEditingDay = false;
        hideKeyboard();
    }

    private void hideKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
        }
    }

    private void saveAndReturn() {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, day);
        long date = cal.getTimeInMillis();

        Intent result = new Intent();
        result.putExtra(EXTRA_DATE, date);
        setResult(RESULT_OK, result);
        finish();
    }
}