package com.example.myapplication;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;
import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftDay;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftRuleManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CalendarFragment extends Fragment {

    private static final String TAG = "CalendarFragment";
    private static final String ARG_RULE = "shift_rule";
    private ShiftRule shiftRule;
    private Calendar currentCalendar;
    private TextView detailDate;
    private TextView detailShift;
    private TextView detailOtherGroups;
    private View lastSelectedView = null;
    private ShiftDay lastSelectedDay = null;
    private OnGroupSwitchListener groupSwitchListener;
    private ShiftRuleManager ruleManager;
    private List<ShiftRule> sameCompanyRules = new ArrayList<>();
    
    private java.util.Map<String, List<ShiftDay>> monthCache = new java.util.HashMap<>();
    private BroadcastReceiver alarmChangedReceiver;
    private long lastClickTime = 0;
    private int holidayRestBgColor;
    private int holidayWorkBgColor;
    private int holidayRedColor;
    private int textSecondaryColor;
    private int transparentColor;
    private int todayBgColor;
    private int backgroundColor;

    public interface OnGroupSwitchListener {
        void onGroupSwitched(ShiftRule newRule);
    }

    public void setOnGroupSwitchListener(OnGroupSwitchListener listener) {
        this.groupSwitchListener = listener;
    }

    public static CalendarFragment newInstance(ShiftRule rule) {
        CalendarFragment fragment = new CalendarFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_RULE, rule);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shiftRule = (ShiftRule) getArguments().getSerializable(ARG_RULE);
        }
        if (shiftRule == null) {
            shiftRule = ShiftCalendarUtil.createDefaultRule();
        }
        ruleManager = ShiftRuleManager.getInstance(getContext());
        loadSameCompanyRules();
        currentCalendar = Calendar.getInstance();
    }

    private void loadSameCompanyRules() {
        sameCompanyRules = ShiftCalendarUtil.getSameCompanyRules(getContext(), shiftRule);
        if (sameCompanyRules.isEmpty()) {
            ShiftRule defaultRule = ShiftCalendarUtil.createDefaultRule();
            sameCompanyRules.add(defaultRule);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView groupName = view.findViewById(R.id.group_name);
        groupName.setText(ShiftCalendarUtil.removeHSM2Prefix(shiftRule.getName()));

        View groupSwitchContainer = view.findViewById(R.id.group_switch_container);
        groupSwitchContainer.setOnClickListener(v -> showGroupPopup());

        TextView todayText = view.findViewById(R.id.today_text);
        todayText.setOnClickListener(v -> jumpToToday());

        detailDate = view.findViewById(R.id.detail_date);
        detailShift = view.findViewById(R.id.detail_shift);
        detailOtherGroups = view.findViewById(R.id.detail_other_groups);

        TextView monthTitle = view.findViewById(R.id.month_title);
        monthTitle.setOnClickListener(v -> showDatePickerDialog());

        initColors();

        updateCalendar(view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerAlarmChangedReceiver();
        monthCache.clear();
        View view = getView();
        if (view != null) {
            updateCalendar(view);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterAlarmChangedReceiver();
    }

    private void registerAlarmChangedReceiver() {
        if (alarmChangedReceiver != null) {
            return;
        }
        alarmChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AlarmManager.ACTION_ALARM_CHANGED.equals(intent.getAction())) {
                    Log.d(TAG, "收到闹钟变化广播，刷新日历视图");
                    monthCache.clear();
                    View view = getView();
                    if (view != null) {
                        updateCalendar(view);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(AlarmManager.ACTION_ALARM_CHANGED);
        if (getContext() != null) {
            getContext().registerReceiver(alarmChangedReceiver, filter);
        }
    }

    private void unregisterAlarmChangedReceiver() {
        if (alarmChangedReceiver != null && getContext() != null) {
            try {
                getContext().unregisterReceiver(alarmChangedReceiver);
            } catch (Exception e) {
                Log.e(TAG, "注销广播接收器失败", e);
            }
            alarmChangedReceiver = null;
        }
    }

    private void showDatePickerDialog() {
        Log.d(TAG, "显示日期选择器弹窗");

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_date_picker, null);

        EditText etYear = dialogView.findViewById(R.id.et_year);
        EditText etMonth = dialogView.findViewById(R.id.et_month);
        ImageView ivYearUp = dialogView.findViewById(R.id.iv_year_up);
        ImageView ivYearDown = dialogView.findViewById(R.id.iv_year_down);
        ImageView ivMonthUp = dialogView.findViewById(R.id.iv_month_up);
        ImageView ivMonthDown = dialogView.findViewById(R.id.iv_month_down);

        final int startYear = 2018;
        final int endYear = 2050;
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH) + 1;

        final int[] year = {currentYear};
        final int[] month = {currentMonth};

        etYear.setText(String.valueOf(year[0]));
        etMonth.setText(String.format("%02d", month[0]));

        ivYearUp.setOnClickListener(v -> {
            year[0] = (year[0] + 1);
            if (year[0] > endYear) year[0] = endYear;
            etYear.setText(String.valueOf(year[0]));
            clearFocusAndHideKeyboard(etYear, etMonth);
        });

        ivYearDown.setOnClickListener(v -> {
            year[0] = (year[0] - 1);
            if (year[0] < startYear) year[0] = startYear;
            etYear.setText(String.valueOf(year[0]));
            clearFocusAndHideKeyboard(etYear, etMonth);
        });

        ivMonthUp.setOnClickListener(v -> {
            month[0] = (month[0] % 12) + 1;
            etMonth.setText(String.format("%02d", month[0]));
            clearFocusAndHideKeyboard(etYear, etMonth);
        });

        ivMonthDown.setOnClickListener(v -> {
            month[0] = (month[0] - 2 + 12) % 12 + 1;
            etMonth.setText(String.format("%02d", month[0]));
            clearFocusAndHideKeyboard(etYear, etMonth);
        });

        final float[] lastY = {0};
        final float[] startY = {0};
        final boolean[] isScrolling = {false};
        final int SCROLL_THRESHOLD = 30;
        final long[] lastClickTime = {0};
        final long DOUBLE_CLICK_THRESHOLD = 300;

        etYear.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    etYear.setFocusable(false);
                    etYear.setFocusableInTouchMode(false);
                    etYear.setCursorVisible(false);
                    startY[0] = event.getY();
                    lastY[0] = event.getY();
                    isScrolling[0] = false;
                    lastClickTime[0] = System.currentTimeMillis();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    etYear.clearFocus();
                    etYear.setFocusable(false);
                    etYear.setFocusableInTouchMode(false);
                    etYear.setCursorVisible(false);
                    etMonth.clearFocus();
                    etMonth.setFocusable(false);
                    etMonth.setFocusableInTouchMode(false);
                    etMonth.setCursorVisible(false);

                    float currentY = event.getY();
                    float deltaY = currentY - startY[0];
                    if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                        isScrolling[0] = true;
                    }
                    if (isScrolling[0]) {
                        float moveDelta = lastY[0] - currentY;
                        if (moveDelta > 30) {
                            year[0] = year[0] + 1;
                            if (year[0] > endYear) year[0] = endYear;
                            etYear.setText(String.valueOf(year[0]));
                            lastY[0] = currentY;
                        } else if (moveDelta < -30) {
                            year[0] = year[0] - 1;
                            if (year[0] < startYear) year[0] = startYear;
                            etYear.setText(String.valueOf(year[0]));
                            lastY[0] = currentY;
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (!isScrolling[0]) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime[0] < DOUBLE_CLICK_THRESHOLD) {
                            etYear.setFocusable(true);
                            etYear.setFocusableInTouchMode(true);
                            etYear.setCursorVisible(true);
                            etYear.requestFocus();
                            int length = etYear.getText().length();
                            etYear.setSelection(0, length);
                        }
                    }
                    isScrolling[0] = false;
                    return true;
            }
            return false;
        });

        etMonth.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    etMonth.setFocusable(false);
                    etMonth.setFocusableInTouchMode(false);
                    etMonth.setCursorVisible(false);
                    startY[0] = event.getY();
                    lastY[0] = event.getY();
                    isScrolling[0] = false;
                    lastClickTime[0] = System.currentTimeMillis();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    etYear.clearFocus();
                    etYear.setFocusable(false);
                    etYear.setFocusableInTouchMode(false);
                    etYear.setCursorVisible(false);
                    etMonth.clearFocus();
                    etMonth.setFocusable(false);
                    etMonth.setFocusableInTouchMode(false);
                    etMonth.setCursorVisible(false);

                    float currentY = event.getY();
                    float deltaY = currentY - startY[0];
                    if (Math.abs(deltaY) > SCROLL_THRESHOLD) {
                        isScrolling[0] = true;
                    }
                    if (isScrolling[0]) {
                        float moveDelta = lastY[0] - currentY;
                        if (moveDelta > 30) {
                            month[0] = (month[0] % 12) + 1;
                            etMonth.setText(String.format("%02d", month[0]));
                            lastY[0] = currentY;
                        } else if (moveDelta < -30) {
                            month[0] = (month[0] - 2 + 12) % 12 + 1;
                            etMonth.setText(String.format("%02d", month[0]));
                            lastY[0] = currentY;
                        }
                        return true;
                    }
                    return false;
                case MotionEvent.ACTION_UP:
                    if (!isScrolling[0]) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime[0] < DOUBLE_CLICK_THRESHOLD) {
                            etMonth.setFocusable(true);
                            etMonth.setFocusableInTouchMode(true);
                            etMonth.setCursorVisible(true);
                            etMonth.requestFocus();
                            int length = etMonth.getText().length();
                            etMonth.setSelection(0, length);
                        }
                    }
                    isScrolling[0] = false;
                    return true;
            }
            return false;
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);
        btnConfirm.setOnClickListener(v -> {
            try {
                int selectedYear = Integer.parseInt(etYear.getText().toString());
                int selectedMonth = Integer.parseInt(etMonth.getText().toString());

                if (selectedYear < startYear) selectedYear = startYear;
                if (selectedYear > endYear) selectedYear = endYear;
                if (selectedMonth < 1) selectedMonth = 1;
                if (selectedMonth > 12) selectedMonth = 12;

                Log.d(TAG, "选择日期: " + selectedYear + "年" + selectedMonth + "月");

                currentCalendar.set(selectedYear, selectedMonth - 1, 1);
                lastSelectedDay = null;
                lastSelectedView = null;
                updateCalendar(getView());
            } catch (NumberFormatException e) {
                Log.e(TAG, "日期解析错误", e);
            }

            dialog.dismiss();
        });

        dialog.show();
    }

    private void showAlarmPopup(ShiftDay day) {
        Context context = getContext();
        if (context == null || day == null) {
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("设置闹钟");
        builder.setMessage(day.getYear() + "年" + day.getMonth() + "月" + day.getDay() + "日");
        
        builder.setPositiveButton("设置", (dialog, which) -> {
            if (getContext() == null) {
                return;
            }
            
            Calendar calendar = Calendar.getInstance();
            int year = day.getYear();
            int monthValue = day.getMonth();
            int dayOfMonth = day.getDay();
            
            if (year < 1970 || year > 2100) {
                year = Calendar.getInstance().get(Calendar.YEAR);
            }
            if (monthValue < 1 || monthValue > 12) {
                monthValue = 1;
            }
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                dayOfMonth = 1;
            }
            
            calendar.set(year, monthValue - 1, dayOfMonth);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            
            Intent intent = new Intent(getContext(), DateAlarmActivity.class);
            intent.putExtra("date", calendar.getTimeInMillis());
            startActivity(intent);
        });
        
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        
        builder.show();
    }

    private void clearFocusAndHideKeyboard(EditText et1, EditText et2) {
        et1.clearFocus();
        et1.setFocusable(false);
        et1.setFocusableInTouchMode(false);
        et1.setCursorVisible(false);
        et2.clearFocus();
        et2.setFocusable(false);
        et2.setFocusableInTouchMode(false);
        et2.setCursorVisible(false);
    }

    private void initColors() {
        holidayRestBgColor = getResources().getColor(R.color.holiday_rest_bg);
        holidayWorkBgColor = getResources().getColor(R.color.holiday_work_bg);
        holidayRedColor = getResources().getColor(R.color.holiday_red);
        textSecondaryColor = getResources().getColor(R.color.text_secondary);
        transparentColor = getResources().getColor(android.R.color.transparent);
        todayBgColor = getResources().getColor(R.color.today_bg);
        backgroundColor = getResources().getColor(R.color.background);
    }

    private void updateCalendar(View view) {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1;

        TextView monthTitleView = view.findViewById(R.id.month_title);
        monthTitleView.setText(year + "年" + month + "月");

        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = year == today.get(Calendar.YEAR) &&
                                 month == today.get(Calendar.MONTH) + 1;
        TextView todayText = view.findViewById(R.id.today_text);
        todayText.setVisibility(isCurrentMonth ? View.GONE : View.VISIBLE);

        String cacheKey = year + "-" + month;
        List<ShiftDay> days = monthCache.get(cacheKey);
        if (days == null) {
            days = ShiftCalendarUtil.generateMonthShiftDays(year, month, shiftRule);
            if (monthCache.size() > 12) {
                monthCache.clear();
            }
            monthCache.put(cacheKey, days);
        }

        LinearLayout calendarContainer = view.findViewById(R.id.calendar_container);
        calendarContainer.removeAllViews();

        for (int week = 0; week < 6; week++) {
            LinearLayout weekRow = new LinearLayout(getContext());
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1));
            weekRow.setWeightSum(7);

            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                int index = week * 7 + dayOfWeek;
                if (index >= days.size()) {
                    View emptyView = new View(getContext());
                    LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    emptyView.setLayoutParams(emptyParams);
                    emptyView.setBackgroundColor(backgroundColor);
                    weekRow.addView(emptyView);
                    continue;
                }

                ShiftDay day = days.get(index);
                View dayView = LayoutInflater.from(getContext()).inflate(R.layout.calendar_day_item, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1);
                dayView.setLayoutParams(params);

                TextView dayText = dayView.findViewById(R.id.day_text);
                TextView lunarText = dayView.findViewById(R.id.lunar_text);
                TextView shiftText = dayView.findViewById(R.id.shift_text);
                TextView holidayMark = dayView.findViewById(R.id.holiday_mark);
                View selectedBg = dayView.findViewById(R.id.selected_bg);

                dayText.setText(String.valueOf(day.getDay()));

                String displayText = "";
                if (day.getHoliday() != null && !day.getHoliday().isEmpty()) {
                    displayText = day.getHoliday();
                    lunarText.setTextColor(holidayRedColor);
                } else if (day.getSolarTerm() != null && !day.getSolarTerm().isEmpty()) {
                    displayText = day.getSolarTerm();
                    lunarText.setTextColor(textSecondaryColor);
                } else if (day.getLunarDate() != null && !day.getLunarDate().isEmpty()) {
                    displayText = day.getLunarDate();
                    lunarText.setTextColor(textSecondaryColor);
                }
                lunarText.setText(displayText);

                if (day.getHolidayMark() != null && !day.getHolidayMark().isEmpty()) {
                    holidayMark.setText(day.getHolidayMark());
                    holidayMark.setVisibility(View.VISIBLE);
                    if ("休".equals(day.getHolidayMark())) {
                        holidayMark.setBackgroundColor(holidayRestBgColor);
                    } else if ("班".equals(day.getHolidayMark())) {
                        holidayMark.setBackgroundColor(holidayWorkBgColor);
                    } else {
                        holidayMark.setBackgroundColor(transparentColor);
                    }
                } else {
                    holidayMark.setVisibility(View.GONE);
                }

                setShiftTextColor(shiftText, day.getShiftType());
                shiftText.setText(getShiftDisplayName(day.getShiftType()));

                ImageView ivAlarmBadge = dayView.findViewById(R.id.iv_alarm_badge);
                if (hasCalendarAlarm(day.getYear(), day.getMonth(), day.getDay())) {
                    ivAlarmBadge.setVisibility(View.VISIBLE);
                } else {
                    ivAlarmBadge.setVisibility(View.GONE);
                }

                boolean isToday = day.getYear() == today.get(Calendar.YEAR) &&
                        day.getMonth() == today.get(Calendar.MONTH) + 1 &&
                        day.getDay() == today.get(Calendar.DAY_OF_MONTH);

                boolean isDayInCurrentMonth = day.getMonth() == month;

                if (isToday) {
                    dayView.setBackgroundColor(todayBgColor);
                }

                if (!isDayInCurrentMonth) {
                    dayView.setAlpha(0.4f);
                }

                if (lastSelectedDay != null &&
                        day.getYear() == lastSelectedDay.getYear() &&
                        day.getMonth() == lastSelectedDay.getMonth() &&
                        day.getDay() == lastSelectedDay.getDay()) {
                    selectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = dayView;
                }

                if (isToday && lastSelectedDay == null) {
                    selectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = dayView;
                    lastSelectedDay = day;
                    updateDetailInfo(day);
                }

                dayView.setOnClickListener(v -> {
                    if (hasCalendarAlarm(day.getYear(), day.getMonth(), day.getDay())) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastClickTime < 300) {
                            AlarmSetting alarm = getCalendarAlarmForDate(day.getYear(), day.getMonth(), day.getDay());
                            if (alarm != null) {
                                Intent intent = new Intent(getContext(), DateAlarmActivity.class);
                                intent.putExtra("alarm", alarm);
                                startActivity(intent);
                            }
                            lastClickTime = 0;
                            return;
                        }
                        lastClickTime = currentTime;
                    }
                    
                    if (lastSelectedView != null) {
                        View prevSelectedBg = lastSelectedView.findViewById(R.id.selected_bg);
                        prevSelectedBg.setVisibility(View.GONE);
                    }
                    selectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = dayView;
                    lastSelectedDay = day;
                    updateDetailInfo(day);
                });

                dayView.setOnLongClickListener(v -> {
                    showAlarmPopup(day);
                    return true;
                });

                weekRow.addView(dayView);
            }

            calendarContainer.addView(weekRow);
        }
    }

    private void updateDetailInfo(ShiftDay day) {
        String weekDay = getWeekDay(day.getYear(), day.getMonth(), day.getDay());
        String lunarDate = day.getLunarDate() != null ? day.getLunarDate() : "";
        String lunarMonth = day.getLunarMonth() != null ? day.getLunarMonth() : "";

        detailDate.setText(day.getYear() + "年" + String.format("%02d", day.getMonth()) + "月" + String.format("%02d", day.getDay()) + "日 " + weekDay + " 农历 " + lunarMonth + lunarDate);
        detailShift.setText("★ 当前班次：" + ShiftCalendarUtil.removeHSM2Prefix(shiftRule.getName()) + " " + day.getShiftType());
        detailOtherGroups.setText(getOtherGroupsShift(day));
    }

    private String getWeekDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return weekDays[dayOfWeek - 1];
    }

    private void setShiftTextColor(TextView textView, String shiftType) {
        switch (shiftType) {
            case ShiftCalendarUtil.SHIFT_WHITE:
                textView.setTextColor(getResources().getColor(R.color.shift_white));
                break;
            case ShiftCalendarUtil.SHIFT_NIGHT:
                textView.setTextColor(getResources().getColor(R.color.shift_night));
                break;
            case ShiftCalendarUtil.SHIFT_EVENING:
                textView.setTextColor(getResources().getColor(R.color.shift_evening));
                break;
            case ShiftCalendarUtil.SHIFT_REST:
                textView.setTextColor(getResources().getColor(R.color.shift_rest));
                break;
            default:
                textView.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private String getShiftDisplayName(String shiftType) {
        switch (shiftType) {
            case ShiftCalendarUtil.SHIFT_WHITE:
                return "白班";
            case ShiftCalendarUtil.SHIFT_NIGHT:
                return "上夜班";
            case ShiftCalendarUtil.SHIFT_EVENING:
                return "下夜班";
            case ShiftCalendarUtil.SHIFT_REST:
                return "正休";
            default:
                return shiftType;
        }
    }

    private String getOtherGroupsShift(ShiftDay selectedDay) {
        if (sameCompanyRules.size() <= 1) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        for (ShiftRule rule : sameCompanyRules) {
            if (rule.getName().equals(shiftRule.getName())) {
                continue;
            }
            String shiftType = ShiftCalendarUtil.getShiftType(rule, selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
            if (result.length() > 0) result.append(" ");
            result.append(ShiftCalendarUtil.removeHSM2Prefix(rule.getName())).append("(").append(getShiftDisplayName(shiftType)).append(")");
        }
        return result.toString();
    }

    private void showGroupPopup() {
        List<String> groupNameList = new ArrayList<>();
        groupNameList.add(shiftRule.getName());
        for (ShiftRule rule : sameCompanyRules) {
            if (!rule.getName().equals(shiftRule.getName())) {
                groupNameList.add(rule.getName());
            }
        }

        final String[] groupsArray = groupNameList.toArray(new String[0]);
        final int currentGroupPosition = 0;

        ListView listView = new ListView(getContext());
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setBackgroundColor(getResources().getColor(android.R.color.white));

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                R.layout.item_group_spinner,
                android.R.id.text1,
                groupsArray) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(ShiftCalendarUtil.removeHSM2Prefix(getItem(position)));
                if (position == currentGroupPosition) {
                    textView.setTextColor(getResources().getColor(R.color.primary));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setText(ShiftCalendarUtil.removeHSM2Prefix(getItem(position)));
                if (position == currentGroupPosition) {
                    textView.setTextColor(getResources().getColor(R.color.primary));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };
        listView.setAdapter(adapter);

        TextView tempText = new TextView(getContext());
        tempText.setText(ShiftCalendarUtil.removeHSM2Prefix(shiftRule.getName()));
        tempText.setTextSize(16);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tempText.measure(widthSpec, heightSpec);
        int textWidth = tempText.getMeasuredWidth();
        int padding = dpToPx(24);
        int calculatedWidth = textWidth + padding;
        int minWidth = dpToPx(80);
        int popupWidth = Math.max(calculatedWidth, minWidth);

        final PopupWindow popupWindow = new PopupWindow(
                listView,
                popupWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_light));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGroup = groupsArray[position];
            if (!selectedGroup.equals(shiftRule.getName())) {
                ShiftRule selectedRule = null;
                for (ShiftRule rule : sameCompanyRules) {
                    if (rule.getName().equals(selectedGroup)) {
                        selectedRule = rule;
                        break;
                    }
                }
                if (selectedRule == null) {
                    selectedRule = createRuleForGroup(selectedGroup);
                }
                lastSelectedDay = null;
                lastSelectedView = null;
                shiftRule = selectedRule;
                TextView groupName = getView().findViewById(R.id.group_name);
                groupName.setText(ShiftCalendarUtil.removeHSM2Prefix(selectedGroup));
                updateCalendar(getView());
                if (groupSwitchListener != null) {
                    groupSwitchListener.onGroupSwitched(shiftRule);
                }
            }
            popupWindow.dismiss();
        });

        View groupNameView = getView().findViewById(R.id.group_name);
        int[] location = new int[2];
        groupNameView.getLocationOnScreen(location);
        popupWindow.showAtLocation(groupNameView, Gravity.NO_GRAVITY, location[0], location[1] + groupNameView.getHeight());
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private boolean hasCalendarAlarm(int year, int month, int day) {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        
        AlarmManager alarmManager = new AlarmManager(context);
        List<AlarmSetting> allAlarms = alarmManager.getAllAlarms();
        
        Calendar targetDate = Calendar.getInstance();
        targetDate.set(year, month - 1, day);
        targetDate.set(Calendar.HOUR_OF_DAY, 0);
        targetDate.set(Calendar.MINUTE, 0);
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);
        long targetTime = targetDate.getTimeInMillis();
        
        for (AlarmSetting alarm : allAlarms) {
            if ("日历闹钟".equals(alarm.getShiftType())) {
                long repeatDate = alarm.getRepeatDate();
                if (repeatDate > 0) {
                    Calendar alarmDate = Calendar.getInstance();
                    alarmDate.setTimeInMillis(repeatDate);
                    alarmDate.set(Calendar.HOUR_OF_DAY, 0);
                    alarmDate.set(Calendar.MINUTE, 0);
                    alarmDate.set(Calendar.SECOND, 0);
                    alarmDate.set(Calendar.MILLISECOND, 0);
                    
                    if (alarmDate.getTimeInMillis() == targetTime) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    private AlarmSetting getCalendarAlarmForDate(int year, int month, int day) {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        
        AlarmManager alarmManager = new AlarmManager(context);
        List<AlarmSetting> allAlarms = alarmManager.getAllAlarms();
        
        Calendar targetDate = Calendar.getInstance();
        targetDate.set(year, month - 1, day);
        targetDate.set(Calendar.HOUR_OF_DAY, 0);
        targetDate.set(Calendar.MINUTE, 0);
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);
        long targetTime = targetDate.getTimeInMillis();
        
        for (AlarmSetting alarm : allAlarms) {
            if ("日历闹钟".equals(alarm.getShiftType())) {
                long repeatDate = alarm.getRepeatDate();
                if (repeatDate > 0) {
                    Calendar alarmDate = Calendar.getInstance();
                    alarmDate.setTimeInMillis(repeatDate);
                    alarmDate.set(Calendar.HOUR_OF_DAY, 0);
                    alarmDate.set(Calendar.MINUTE, 0);
                    alarmDate.set(Calendar.SECOND, 0);
                    alarmDate.set(Calendar.MILLISECOND, 0);
                    
                    if (alarmDate.getTimeInMillis() == targetTime) {
                        return alarm;
                    }
                }
            }
        }
        
        return null;
    }

    public void updateShiftRule(ShiftRule newRule) {
        this.shiftRule = newRule;
        monthCache.clear();
        updateCalendar(getView());
    }

    public void updateCalendarByDate(int year, int month) {
        currentCalendar.set(year, month - 1, 1);
        lastSelectedView = null;
        updateCalendar(getView());
    }

    private void jumpToToday() {
        currentCalendar = Calendar.getInstance();
        lastSelectedDay = null;
        lastSelectedView = null;
        updateCalendar(getView());
    }

    private ShiftRule createRuleForGroup(String groupName) {
        ShiftRule rule = new ShiftRule();
        rule.setName(groupName);
        rule.setCompanyName("HSM2");
        rule.setTag("");
        rule.setCycleDays(4);
        rule.setGroupCount(4);
        rule.setDefault(groupName.equals("丁班"));

        List<ShiftRule.ShiftDetail> details = new ArrayList<>();
        details.add(new ShiftRule.ShiftDetail(1, ShiftCalendarUtil.SHIFT_WHITE, "08:00到20:00"));
        details.add(new ShiftRule.ShiftDetail(2, ShiftCalendarUtil.SHIFT_NIGHT, "20:00到23:59"));
        details.add(new ShiftRule.ShiftDetail(3, ShiftCalendarUtil.SHIFT_EVENING, "00:00到08:00"));
        details.add(new ShiftRule.ShiftDetail(4, ShiftCalendarUtil.SHIFT_REST, "00:00到23:59"));
        rule.setShiftDetails(details);

        return rule;
    }
}
