package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftDay;
import com.example.myapplication.data.ShiftRule;

import java.util.ArrayList;
import java.util.Calendar;
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
    private OnGroupSwitchListener groupSwitchListener;

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
        currentCalendar = Calendar.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        TextView groupName = view.findViewById(R.id.group_name);
        groupName.setText(shiftRule.getName());

        View groupSwitchContainer = view.findViewById(R.id.group_switch_container);
        groupSwitchContainer.setOnClickListener(v -> showGroupPopup());

        TextView todayText = view.findViewById(R.id.today_text);
        todayText.setOnClickListener(v -> jumpToToday());

        detailDate = view.findViewById(R.id.detail_date);
        detailShift = view.findViewById(R.id.detail_shift);
        detailOtherGroups = view.findViewById(R.id.detail_other_groups);

        updateCalendar(view);

        return view;
    }

    private void updateCalendar(View view) {
        int year = currentCalendar.get(Calendar.YEAR);
        int month = currentCalendar.get(Calendar.MONTH) + 1;

        TextView monthTitle = view.findViewById(R.id.month_title);
        monthTitle.setText(year + "年" + month + "月");

        Calendar today = Calendar.getInstance();
        boolean isCurrentMonth = year == today.get(Calendar.YEAR) &&
                                 month == today.get(Calendar.MONTH) + 1;
        TextView todayText = view.findViewById(R.id.today_text);
        todayText.setVisibility(isCurrentMonth ? View.GONE : View.VISIBLE);

        List<ShiftDay> days = ShiftCalendarUtil.generateMonthShiftDays(year, month, shiftRule);

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
                    emptyView.setBackgroundColor(getResources().getColor(R.color.background));
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
                    lunarText.setTextColor(getResources().getColor(R.color.shift_night));
                } else if (day.getSolarTerm() != null && !day.getSolarTerm().isEmpty()) {
                    displayText = day.getSolarTerm();
                    lunarText.setTextColor(getResources().getColor(R.color.shift_white));
                } else if (day.getLunarDate() != null && !day.getLunarDate().isEmpty()) {
                    displayText = day.getLunarDate();
                    lunarText.setTextColor(getResources().getColor(R.color.text_secondary));
                }
                lunarText.setText(displayText);

                if (day.getHolidayMark() != null && !day.getHolidayMark().isEmpty()) {
                    holidayMark.setText(day.getHolidayMark());
                    holidayMark.setVisibility(View.VISIBLE);
                } else {
                    holidayMark.setVisibility(View.GONE);
                }

                setShiftTextColor(shiftText, day.getShiftType());
                shiftText.setText(getShiftDisplayName(day.getShiftType()));

                boolean isToday = day.getYear() == today.get(Calendar.YEAR) &&
                        day.getMonth() == today.get(Calendar.MONTH) + 1 &&
                        day.getDay() == today.get(Calendar.DAY_OF_MONTH);

                boolean isDayInCurrentMonth = day.getMonth() == month;

                if (isToday) {
                    dayView.setBackgroundColor(getResources().getColor(R.color.today_bg));
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
                    if (lastSelectedView != null) {
                        View prevSelectedBg = lastSelectedView.findViewById(R.id.selected_bg);
                        prevSelectedBg.setVisibility(View.GONE);
                    }
                    selectedBg.setVisibility(View.VISIBLE);
                    lastSelectedView = dayView;
                    lastSelectedDay = day;
                    updateDetailInfo(day);
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
        detailShift.setText("★ 当前班次：" + shiftRule.getName() + " " + day.getShiftType());
        detailOtherGroups.setText(getOtherGroupsShift(day.getShiftType()));
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

    private String getOtherGroupsShift(String currentShift) {
        String[] allGroups = {"丁班", "丙班", "乙班", "甲班"};
        String[] shifts = {ShiftCalendarUtil.SHIFT_WHITE, ShiftCalendarUtil.SHIFT_NIGHT, ShiftCalendarUtil.SHIFT_EVENING, ShiftCalendarUtil.SHIFT_REST};

        int currentShiftIndex = -1;
        for (int i = 0; i < shifts.length; i++) {
            if (shifts[i].equals(currentShift)) {
                currentShiftIndex = i;
                break;
            }
        }

        int currentGroupIndex = -1;
        for (int i = 0; i < allGroups.length; i++) {
            if (allGroups[i].equals(shiftRule.getName())) {
                currentGroupIndex = i;
                break;
            }
        }

        String[] displayOrder;
        switch (shiftRule.getName()) {
            case "丁班":
                displayOrder = new String[]{"甲班", "乙班", "丙班"};
                break;
            case "甲班":
                displayOrder = new String[]{"乙班", "丙班", "丁班"};
                break;
            case "乙班":
                displayOrder = new String[]{"甲班", "丙班", "丁班"};
                break;
            case "丙班":
                displayOrder = new String[]{"甲班", "乙班", "丁班"};
                break;
            default:
                displayOrder = new String[]{"甲班", "乙班", "丙班"};
        }

        StringBuilder result = new StringBuilder();
        for (String group : displayOrder) {
            int groupIndex = -1;
            for (int i = 0; i < allGroups.length; i++) {
                if (allGroups[i].equals(group)) {
                    groupIndex = i;
                    break;
                }
            }
            int shiftIndex = (currentShiftIndex - groupIndex + currentGroupIndex + shifts.length) % shifts.length;
            if (result.length() > 0) result.append(" ");
            result.append(group).append("(").append(getShiftDisplayName(shifts[shiftIndex])).append(")");
        }
        return result.toString();
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

        TextView tempText = new TextView(getContext());
        tempText.setText("丁班");
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
                true
        );
        popupWindow.setBackgroundDrawable(getResources().getDrawable(android.R.drawable.screen_background_light));

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedGroup = groupsArray[position];
            if (!selectedGroup.equals(currentGroup)) {
                lastSelectedDay = null;
                lastSelectedView = null;
                shiftRule = createRuleForGroup(selectedGroup);
                TextView groupName = getView().findViewById(R.id.group_name);
                groupName.setText(selectedGroup);
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