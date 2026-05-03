package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftDay;
import com.example.myapplication.data.ShiftRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class CalendarFragment extends Fragment {

    private static final String ARG_RULE = "shift_rule";
    private ShiftRule shiftRule;
    private Calendar currentCalendar;
    private TextView detailDate;
    private TextView detailShift;
    private TextView detailOtherGroups;
    private View lastSelectedView = null;
    private ShiftDay lastSelectedDay = null;

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
        currentCalendar = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView monthTitle = view.findViewById(R.id.month_title);
        monthTitle.setText(currentCalendar.get(Calendar.YEAR) + "年" + (currentCalendar.get(Calendar.MONTH) + 1) + "月");
        monthTitle.setOnClickListener(v -> showDatePickerDialog());

        detailDate = view.findViewById(R.id.detail_date);
        detailShift = view.findViewById(R.id.detail_shift);
        detailOtherGroups = view.findViewById(R.id.detail_other_groups);

        TextView todayText = view.findViewById(R.id.today_text);
        todayText.setOnClickListener(v -> {
            currentCalendar = Calendar.getInstance();
            int todayYear = currentCalendar.get(Calendar.YEAR);
            int todayMonth = currentCalendar.get(Calendar.MONTH) + 1;
            int todayDay = currentCalendar.get(Calendar.DAY_OF_MONTH);
            lastSelectedDay = new ShiftDay(todayYear, todayMonth, todayDay, "");
            updateCalendar(view);
        });

        TextView groupName = view.findViewById(R.id.group_name);
        groupName.setText(shiftRule.getName());
        
        View groupSwitchContainer = view.findViewById(R.id.group_switch_container);
        groupSwitchContainer.setOnClickListener(v -> showGroupPopup());

        view.findViewById(R.id.group_statistics).setOnClickListener(v -> {
            Toast.makeText(getContext(), "统计功能开发中", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btn_view_year).setOnClickListener(v -> {
            Toast.makeText(getContext(), "全年视图开发中", Toast.LENGTH_SHORT).show();
        });

        LinearLayout calendarContainer = view.findViewById(R.id.calendar_container);

        updateCalendar(view);

        return view;
    }

    public void updateCalendarByDate(int year, int month) {
        currentCalendar.set(year, month - 1, 1);
        updateCalendar(getView());
    }

    private void updateCalendar(View view) {
        TextView monthTitle = view.findViewById(R.id.month_title);
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1;
        monthTitle.setText(year + "年" + month + "月");

        List<ShiftDay> days = ShiftCalendarUtil.generateMonthShiftDays(year, month, shiftRule);

        LinearLayout calendarContainer = view.findViewById(R.id.calendar_container);
        calendarContainer.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        Calendar today = Calendar.getInstance();
        
        TextView todayText = view.findViewById(R.id.today_text);
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH) + 1;
        if (year != todayYear || month != todayMonth) {
            todayText.setVisibility(View.VISIBLE);
        } else {
            todayText.setVisibility(View.GONE);
        }

        // 保存当前选中的日期
        int selectedYear = -1;
        int selectedMonth = -1;
        int selectedDay = -1;
        if (lastSelectedDay != null) {
            selectedYear = lastSelectedDay.getYear();
            selectedMonth = lastSelectedDay.getMonth();
            selectedDay = lastSelectedDay.getDay();
        } else {
            // 如果没有保存的选中日期，默认选中今天
            selectedYear = todayYear;
            selectedMonth = todayMonth;
            selectedDay = today.get(Calendar.DAY_OF_MONTH);
        }

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        int headerHeight = (int) (screenHeight * 0.12);
        int infoBarHeight = (int) (screenHeight * 0.13);
        int weekHeaderHeight = (int) (screenHeight * 0.055);
        int calendarHeight = screenHeight - statusBarHeight - headerHeight - infoBarHeight - weekHeaderHeight - 20;
        int rowHeight = calendarHeight / 6;

        for (int week = 0; week < 6; week++) {
            LinearLayout weekRow = new LinearLayout(getContext());
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    rowHeight);
            rowParams.setMargins(0, 0, 0, 0);
            rowParams.topMargin = 0;
            rowParams.bottomMargin = 0;
            weekRow.setLayoutParams(rowParams);
            weekRow.setPadding(0, 0, 0, 0);
            calendarContainer.addView(weekRow);

            if (week < 5) {
                View divider = new View(getContext());
                LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        3);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(getResources().getColor(R.color.primary));
                calendarContainer.addView(divider);
            }

            for (int dayOfWeek = 0; dayOfWeek < 7; dayOfWeek++) {
                int index = week * 7 + dayOfWeek;
                if (index >= days.size()) {
                    View emptyView = new View(getContext());
                    LinearLayout.LayoutParams emptyParams = new LinearLayout.LayoutParams(0, rowHeight, 1);
                    emptyView.setLayoutParams(emptyParams);
                    emptyView.setBackgroundColor(getResources().getColor(R.color.background));
                    weekRow.addView(emptyView);
                    continue;
                }

                ShiftDay day = days.get(index);
                View dayView = inflater.inflate(R.layout.calendar_day_item, null);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, rowHeight, 1);
                dayView.setLayoutParams(params);

                TextView dayText = dayView.findViewById(R.id.day_text);
                TextView lunarText = dayView.findViewById(R.id.lunar_text);
                TextView shiftText = dayView.findViewById(R.id.shift_text);
                TextView holidayMark = dayView.findViewById(R.id.holiday_mark);
                View selectedBg = dayView.findViewById(R.id.selected_bg);
                View dayContainer = dayView.findViewById(R.id.day_container);

                dayText.setText(String.valueOf(day.getDay()));

                if (day.getHolidayMark() != null) {
                    holidayMark.setText(day.getHolidayMark());
                    holidayMark.setVisibility(View.VISIBLE);
                } else {
                    holidayMark.setVisibility(View.GONE);
                }

                if (day.getHoliday() != null) {
                    lunarText.setText(day.getHoliday());
                    lunarText.setTextColor(getResources().getColor(R.color.holiday));
                    dayText.setTextColor(getResources().getColor(R.color.holiday));
                } else {
                    lunarText.setText(day.getLunarDate() != null ? day.getLunarDate() : "");
                    lunarText.setTextColor(getResources().getColor(R.color.text_hint));
                }

                shiftText.setText(day.getShiftType());

                boolean isCurrentMonth = day.getMonth() == month;
                if (!isCurrentMonth) {
                    dayText.setTextColor(getResources().getColor(R.color.text_hint));
                    if (day.getHoliday() == null) {
                        lunarText.setTextColor(getResources().getColor(R.color.text_hint));
                    }
                } else {
                    if (day.getHoliday() == null) {
                        dayText.setTextColor(getResources().getColor(R.color.text_primary));
                    }
                }

                boolean isToday = day.getYear() == today.get(Calendar.YEAR) &&
                        day.getMonth() == today.get(Calendar.MONTH) + 1 &&
                        day.getDay() == today.get(Calendar.DAY_OF_MONTH);
                if (isToday) {
                    dayContainer.setBackgroundResource(R.drawable.bg_today);
                }

                // 检查是否是当前选中的日期
                boolean isSelected = false;
                if (selectedYear == day.getYear() && selectedMonth == day.getMonth() && selectedDay == day.getDay()) {
                    isSelected = true;
                }

                if (isSelected) {
                    selectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = dayView;
                    lastSelectedDay = day;
                } else {
                    selectedBg.setVisibility(View.GONE);
                }

                setShiftColor(shiftText, day.getShiftType());

                final ShiftDay finalDay = day;
                final View finalDayView = dayView;
                final View finalSelectedBg = selectedBg;
                dayView.setOnClickListener(v -> {
                    if (lastSelectedView != null) {
                        View prevSelectedBg = lastSelectedView.findViewById(R.id.selected_bg);
                        if (prevSelectedBg != null) {
                            prevSelectedBg.setVisibility(View.GONE);
                        }
                    }
                    finalSelectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = finalDayView;
                    lastSelectedDay = finalDay;
                    updateDetailInfo(finalDay);
                });

                weekRow.addView(dayView);
            }
        }

        if (lastSelectedDay != null) {
            updateDetailInfo(lastSelectedDay);
        } else {
            updateDetailInfo(days.get(days.size() / 2));
        }
    }

    private void updateDetailInfo(ShiftDay day) {
        String weekDay = getWeekDay(day.getYear(), day.getMonth(), day.getDay());
        String lunarDate = day.getLunarDate() != null ? day.getLunarDate() : "";
        String lunarMonth = day.getLunarMonth() != null ? day.getLunarMonth() : "";

        detailDate.setText(day.getYear() + "年" + String.format("%02d", day.getMonth()) + "月" + String.format("%02d", day.getDay()) + "日 " + weekDay + " 农历 " + lunarMonth + lunarDate);
        detailShift.setText("★ 当前班次：" + shiftRule.getName() + " " + day.getShiftType());
        detailOtherGroups.setText(getOtherGroupsShift(day.getShiftType()));
    }

    private String getWeekDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        return weekDays[calendar.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String getOtherGroupsShift(String currentShift) {
        String[] allGroups = {"丁班", "丙班", "乙班", "甲班"};
        String[] displayGroups = {"甲班", "乙班", "丙班"};
        String[] shifts = {ShiftCalendarUtil.SHIFT_WHITE, ShiftCalendarUtil.SHIFT_NIGHT, ShiftCalendarUtil.SHIFT_EVENING, ShiftCalendarUtil.SHIFT_REST};

        int currentIndex = -1;
        for (int i = 0; i < shifts.length; i++) {
            if (shifts[i].equals(currentShift)) {
                currentIndex = i;
                break;
            }
        }

        StringBuilder result = new StringBuilder();
        for (String group : displayGroups) {
            if (group.equals(shiftRule.getName())) continue;
            int groupIndex = -1;
            for (int i = 0; i < allGroups.length; i++) {
                if (allGroups[i].equals(group)) {
                    groupIndex = i;
                    break;
                }
            }
            int shiftIndex = (currentIndex - groupIndex + shifts.length) % shifts.length;
            if (result.length() > 0) result.append(" ");
            result.append(group).append("(").append(shifts[shiftIndex]).append(")");
        }
        return result.toString();
    }

    private void setShiftColor(TextView textView, String shiftType) {
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

    private void showGroupPopup() {
        String[] allGroups = {"甲班", "乙班", "丙班", "丁班"};
        String currentGroup = shiftRule.getName();
        
        List<String> groupList = new ArrayList<>();
        groupList.add(currentGroup);
        for (String group : allGroups) {
            if (!group.equals(currentGroup)) {
                groupList.add(group);
            }
        }
        
        final String[] groupsArray = groupList.toArray(new String[0]);
        final int currentGroupPosition = 0;
        
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.item_group_spinner, null);
        ListView listView = new ListView(getContext());
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setBackgroundColor(getResources().getColor(android.R.color.white));
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
            getContext(),
            R.layout.item_group_spinner,
            android.R.id.text1,
            groupsArray
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                if (position == currentGroupPosition) {
                    textView.setTextColor(getResources().getColor(R.color.primary));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };
        listView.setAdapter(adapter);
        
        // 精确测量2个汉字宽度，并增加0.5倍
        TextView tempText = new TextView(getContext());
        tempText.setText("丁班");
        tempText.setTextSize(16);
        tempText.setPadding(12, 12, 12, 12);
        int widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        tempText.measure(widthSpec, heightSpec);
        int textWidth = tempText.getMeasuredWidth();
        // 在现有宽度基础上增加0.3倍
        int finalWidth = (int) (textWidth * 1.3);
        // 打印宽度信息
        Toast.makeText(getContext(), "原始宽度: " + textWidth + "px, 最终宽度: " + finalWidth + "px", Toast.LENGTH_SHORT).show();
        
        final PopupWindow popupWindow = new PopupWindow(
            listView,
            finalWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        );
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_light));
        
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGroup = groupsArray[position];
            if (!selectedGroup.equals(currentGroup)) {
                shiftRule = createRuleForGroup(selectedGroup);
                TextView groupName = getView().findViewById(R.id.group_name);
                groupName.setText(selectedGroup);
                updateCalendar(getView());
            }
            popupWindow.dismiss();
        });
        
        View groupNameView = getView().findViewById(R.id.group_name);
        int[] location = new int[2];
        groupNameView.getLocationOnScreen(location);
        popupWindow.showAtLocation(groupNameView, Gravity.NO_GRAVITY, location[0], location[1] + groupNameView.getHeight());
    }
    
    private ShiftRule createRuleForGroup(String groupName) {
        ShiftRule rule = new ShiftRule();
        rule.setName(groupName);
        rule.setCompanyName("武钢二热轧");
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
    
    private void showDatePickerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_date_picker, null);

        final CustomNumberPicker yearPicker = dialogView.findViewById(R.id.picker_year);
        final CustomNumberPicker monthPicker = dialogView.findViewById(R.id.picker_month);
        final CustomNumberPicker dayPicker = dialogView.findViewById(R.id.picker_day);

        Calendar today = Calendar.getInstance();
        int currentYear = today.get(Calendar.YEAR);
        int currentMonth = today.get(Calendar.MONTH) + 1;
        int currentDay = today.get(Calendar.DAY_OF_MONTH);

        yearPicker.setMinValue(currentYear - 10);
        yearPicker.setMaxValue(currentYear + 10);
        yearPicker.setValue(currentYear);

        monthPicker.setMinValue(1);
        monthPicker.setMaxValue(12);
        monthPicker.setValue(currentMonth);
        monthPicker.setWrapEnabled(true);

        updateDayPicker(dayPicker, currentYear, currentMonth);
        dayPicker.setValue(currentDay);
        dayPicker.setWrapEnabled(true);

        monthPicker.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int year = yearPicker.getValue();
                int month = monthPicker.getValue();
                int selectedDay = dayPicker.getValue();
                updateDayPicker(dayPicker, year, month);
                int maxDay = getMaxDay(year, month);
                if (selectedDay > maxDay) {
                    dayPicker.setValue(maxDay);
                } else {
                    dayPicker.setValue(selectedDay);
                }
            }
            return false;
        });

        yearPicker.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int year = yearPicker.getValue();
                int month = monthPicker.getValue();
                int selectedDay = dayPicker.getValue();
                updateDayPicker(dayPicker, year, month);
                int maxDay = getMaxDay(year, month);
                if (selectedDay > maxDay) {
                    dayPicker.setValue(maxDay);
                } else {
                    dayPicker.setValue(selectedDay);
                }
            }
            return false;
        });

        builder.setView(dialogView);
        android.app.AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            int year = yearPicker.getValue();
            int month = monthPicker.getValue();
            int day = dayPicker.getValue();
            currentCalendar.set(year, month - 1, day);
            lastSelectedDay = new ShiftDay(year, month, day, "");
            updateCalendar(getView());
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateDayPicker(CustomNumberPicker dayPicker, int year, int month) {
        int maxDay = getMaxDay(year, month);
        dayPicker.setMinValue(1);
        dayPicker.setMaxValue(maxDay);
    }

    private int getMaxDay(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}
