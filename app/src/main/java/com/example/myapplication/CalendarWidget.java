package com.example.myapplication;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.example.myapplication.data.ShiftCalendarUtil;
import com.example.myapplication.data.ShiftDay;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftRuleManager;
import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

import java.util.Calendar;
import java.util.List;

public class CalendarWidget extends AppWidgetProvider {

    public static final String ACTION_PREV_MONTH = "com.example.myapplication.ACTION_PREV_MONTH";
    public static final String ACTION_NEXT_MONTH = "com.example.myapplication.ACTION_NEXT_MONTH";
    public static final String ACTION_OPEN_APP = "com.example.myapplication.ACTION_OPEN_APP";
    public static final String ACTION_GO_TODAY = "com.example.myapplication.ACTION_GO_TODAY";

    private static final String PREFS_NAME = "widget_prefs";
    private static final String KEY_YEAR = "widget_year_";
    private static final String KEY_MONTH = "widget_month_";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_PREV_MONTH.equals(action) || ACTION_NEXT_MONTH.equals(action) || 
            ACTION_OPEN_APP.equals(action) || ACTION_GO_TODAY.equals(action)) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                if (ACTION_PREV_MONTH.equals(action)) {
                    changeMonth(context, appWidgetId, -1);
                } else if (ACTION_NEXT_MONTH.equals(action)) {
                    changeMonth(context, appWidgetId, 1);
                } else if (ACTION_GO_TODAY.equals(action)) {
                    goToToday(context, appWidgetId);
                } else if (ACTION_OPEN_APP.equals(action)) {
                    Intent openIntent = new Intent(context, MainActivity.class);
                    openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(openIntent);
                }
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId);
            }
        }
    }

    private static void changeMonth(Context context, int appWidgetId, int delta) {
        Calendar cal = getWidgetCalendar(context, appWidgetId);
        cal.add(Calendar.MONTH, delta);
        saveWidgetCalendar(context, appWidgetId, cal);
    }

    private static void goToToday(Context context, int appWidgetId) {
        Calendar cal = Calendar.getInstance();
        saveWidgetCalendar(context, appWidgetId, cal);
    }

    private static Calendar getWidgetCalendar(Context context, int appWidgetId) {
        Calendar cal = Calendar.getInstance();
        int year = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_YEAR + appWidgetId, cal.get(Calendar.YEAR));
        int month = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_MONTH + appWidgetId, cal.get(Calendar.MONTH));
        cal.set(year, month, 1);
        return cal;
    }

    private static void saveWidgetCalendar(Context context, int appWidgetId, Calendar cal) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putInt(KEY_YEAR + appWidgetId, cal.get(Calendar.YEAR))
                .putInt(KEY_MONTH + appWidgetId, cal.get(Calendar.MONTH))
                .apply();
    }

    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_calendar);

        Calendar cal = getWidgetCalendar(context, appWidgetId);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH) + 1;

        if (year == todayYear && month == todayMonth) {
            views.setViewVisibility(R.id.widget_today, android.view.View.INVISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_today, android.view.View.VISIBLE);
        }

        views.setTextViewText(R.id.widget_title, year + "年" + month + "月");

        views.setOnClickPendingIntent(R.id.widget_prev_month, getPendingIntent(context, appWidgetId, ACTION_PREV_MONTH));
        views.setOnClickPendingIntent(R.id.widget_next_month, getPendingIntent(context, appWidgetId, ACTION_NEXT_MONTH));
        views.setOnClickPendingIntent(R.id.widget_today, getPendingIntent(context, appWidgetId, ACTION_GO_TODAY));
        views.setOnClickPendingIntent(R.id.widget_root, getPendingIntent(context, appWidgetId, ACTION_OPEN_APP));

        updateCalendarDays(context, views, cal);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static void updateCalendarDays(Context context, RemoteViews views, Calendar cal) {
        ShiftRuleManager ruleManager = ShiftRuleManager.getInstance(context);
        ShiftRule rule = ruleManager.getCurrentRule();
        if (rule == null) {
            rule = ShiftCalendarUtil.createDefaultRule();
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;

        List<ShiftDay> days = ShiftCalendarUtil.generateMonthShiftDays(year, month, rule);
        Calendar today = Calendar.getInstance();
        int todayYear = today.get(Calendar.YEAR);
        int todayMonth = today.get(Calendar.MONTH) + 1;
        int todayDay = today.get(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < 42; i++) {
            int dayId = getDayResourceId(i + 1);
            int lunarId = getLunarResourceId(i + 1);
            int shiftId = getShiftResourceId(i + 1);
            int holidayId = getHolidayResourceId(i + 1);
            int cellId = getCellResourceId(i + 1);
            int alarmId = getAlarmResourceId(i + 1);

            if (dayId == -1 || lunarId == -1 || shiftId == -1 || holidayId == -1 || cellId == -1 || alarmId == -1) {
                continue;
            }

            if (i < days.size()) {
                ShiftDay day = days.get(i);
                views.setViewVisibility(dayId, android.view.View.VISIBLE);
                views.setViewVisibility(lunarId, android.view.View.VISIBLE);
                views.setViewVisibility(shiftId, android.view.View.VISIBLE);
                views.setViewVisibility(holidayId, android.view.View.VISIBLE);
                views.setViewVisibility(cellId, android.view.View.VISIBLE);

                views.setTextViewText(dayId, String.valueOf(day.getDay()));

                String displayText = "";
                int lunarColor = 0xFF666666;
                if (day.getHoliday() != null && !day.getHoliday().isEmpty()) {
                    displayText = day.getHoliday();
                    lunarColor = 0xFFFF4444;
                } else if (day.getSolarTerm() != null && !day.getSolarTerm().isEmpty()) {
                    displayText = day.getSolarTerm();
                    lunarColor = 0xFF666666;
                } else if (day.getLunarDate() != null && !day.getLunarDate().isEmpty()) {
                    displayText = day.getLunarDate();
                    lunarColor = 0xFF666666;
                }
                views.setTextViewText(lunarId, displayText);
                views.setTextColor(lunarId, lunarColor);

                String holidayMark = day.getHolidayMark();
                if (holidayMark != null && !holidayMark.isEmpty()) {
                    views.setTextViewText(holidayId, holidayMark);
                    views.setViewVisibility(holidayId, android.view.View.VISIBLE);
                    if ("休".equals(holidayMark)) {
                        views.setInt(holidayId, "setBackgroundColor", 0xFF2E7D32);
                        views.setTextColor(holidayId, 0xFFFFFFFF);
                    } else if ("班".equals(holidayMark)) {
                        views.setInt(holidayId, "setBackgroundColor", 0xFFC62828);
                        views.setTextColor(holidayId, 0xFFFFFFFF);
                    } else {
                        views.setInt(holidayId, "setBackgroundColor", 0x00000000);
                        views.setTextColor(holidayId, 0xFFFF4444);
                    }
                } else {
                    views.setViewVisibility(holidayId, android.view.View.GONE);
                }

                String shiftType = day.getShiftType();
                String shiftDisplay = getShiftDisplayName(shiftType);
                views.setTextViewText(shiftId, shiftDisplay);

                int shiftColor = 0xFF666666;
                switch (shiftType) {
                    case ShiftCalendarUtil.SHIFT_WHITE:
                        shiftColor = 0xFF00E676;
                        break;
                    case ShiftCalendarUtil.SHIFT_NIGHT:
                        shiftColor = 0xFF7C4DFF;
                        break;
                    case ShiftCalendarUtil.SHIFT_EVENING:
                        shiftColor = 0xFFFF9800;
                        break;
                    case ShiftCalendarUtil.SHIFT_REST:
                        shiftColor = 0xFFE53935;
                        break;
                }
                views.setTextColor(shiftId, shiftColor);

                boolean hasAlarm = hasCalendarAlarm(context, day.getYear(), day.getMonth(), day.getDay());
                if (hasAlarm) {
                    views.setViewVisibility(alarmId, android.view.View.VISIBLE);
                } else {
                    views.setViewVisibility(alarmId, android.view.View.GONE);
                }

                boolean isToday = (day.getYear() == todayYear && day.getMonth() == todayMonth && day.getDay() == todayDay);
                boolean isDayInCurrentMonth = day.getMonth() == month;

                if (isToday) {
                    views.setInt(cellId, "setBackgroundResource", R.drawable.widget_today_border);
                    views.setTextColor(dayId, 0xFF1E90FF);
                } else {
                    views.setInt(cellId, "setBackgroundResource", R.drawable.widget_cell_border);
                    views.setTextColor(dayId, 0xFFFFFFFF);
                }

                if (!isDayInCurrentMonth) {
                    views.setFloat(dayId, "setAlpha", 0.4f);
                } else {
                    views.setFloat(dayId, "setAlpha", 1.0f);
                }
                
                views.setFloat(lunarId, "setAlpha", 1.0f);
                views.setFloat(shiftId, "setAlpha", 1.0f);
                views.setFloat(holidayId, "setAlpha", 1.0f);

            } else {
                views.setViewVisibility(dayId, android.view.View.INVISIBLE);
                views.setViewVisibility(lunarId, android.view.View.INVISIBLE);
                views.setViewVisibility(shiftId, android.view.View.INVISIBLE);
                views.setViewVisibility(holidayId, android.view.View.INVISIBLE);
                views.setViewVisibility(cellId, android.view.View.INVISIBLE);
                views.setViewVisibility(alarmId, android.view.View.INVISIBLE);
            }
        }
    }

    private static String getShiftDisplayName(String shiftType) {
        if (shiftType == null) {
            return "";
        }
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

    private static boolean hasCalendarAlarm(Context context, int year, int month, int day) {
        try {
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
        } catch (Exception e) {
            // Ignore errors, return false
        }
        return false;
    }

    private static int getDayResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_day_1;
            case 2 -> R.id.widget_day_2;
            case 3 -> R.id.widget_day_3;
            case 4 -> R.id.widget_day_4;
            case 5 -> R.id.widget_day_5;
            case 6 -> R.id.widget_day_6;
            case 7 -> R.id.widget_day_7;
            case 8 -> R.id.widget_day_8;
            case 9 -> R.id.widget_day_9;
            case 10 -> R.id.widget_day_10;
            case 11 -> R.id.widget_day_11;
            case 12 -> R.id.widget_day_12;
            case 13 -> R.id.widget_day_13;
            case 14 -> R.id.widget_day_14;
            case 15 -> R.id.widget_day_15;
            case 16 -> R.id.widget_day_16;
            case 17 -> R.id.widget_day_17;
            case 18 -> R.id.widget_day_18;
            case 19 -> R.id.widget_day_19;
            case 20 -> R.id.widget_day_20;
            case 21 -> R.id.widget_day_21;
            case 22 -> R.id.widget_day_22;
            case 23 -> R.id.widget_day_23;
            case 24 -> R.id.widget_day_24;
            case 25 -> R.id.widget_day_25;
            case 26 -> R.id.widget_day_26;
            case 27 -> R.id.widget_day_27;
            case 28 -> R.id.widget_day_28;
            case 29 -> R.id.widget_day_29;
            case 30 -> R.id.widget_day_30;
            case 31 -> R.id.widget_day_31;
            case 32 -> R.id.widget_day_32;
            case 33 -> R.id.widget_day_33;
            case 34 -> R.id.widget_day_34;
            case 35 -> R.id.widget_day_35;
            case 36 -> R.id.widget_day_36;
            case 37 -> R.id.widget_day_37;
            case 38 -> R.id.widget_day_38;
            case 39 -> R.id.widget_day_39;
            case 40 -> R.id.widget_day_40;
            case 41 -> R.id.widget_day_41;
            case 42 -> R.id.widget_day_42;
            default -> -1;
        };
    }

    private static int getLunarResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_lunar_1;
            case 2 -> R.id.widget_lunar_2;
            case 3 -> R.id.widget_lunar_3;
            case 4 -> R.id.widget_lunar_4;
            case 5 -> R.id.widget_lunar_5;
            case 6 -> R.id.widget_lunar_6;
            case 7 -> R.id.widget_lunar_7;
            case 8 -> R.id.widget_lunar_8;
            case 9 -> R.id.widget_lunar_9;
            case 10 -> R.id.widget_lunar_10;
            case 11 -> R.id.widget_lunar_11;
            case 12 -> R.id.widget_lunar_12;
            case 13 -> R.id.widget_lunar_13;
            case 14 -> R.id.widget_lunar_14;
            case 15 -> R.id.widget_lunar_15;
            case 16 -> R.id.widget_lunar_16;
            case 17 -> R.id.widget_lunar_17;
            case 18 -> R.id.widget_lunar_18;
            case 19 -> R.id.widget_lunar_19;
            case 20 -> R.id.widget_lunar_20;
            case 21 -> R.id.widget_lunar_21;
            case 22 -> R.id.widget_lunar_22;
            case 23 -> R.id.widget_lunar_23;
            case 24 -> R.id.widget_lunar_24;
            case 25 -> R.id.widget_lunar_25;
            case 26 -> R.id.widget_lunar_26;
            case 27 -> R.id.widget_lunar_27;
            case 28 -> R.id.widget_lunar_28;
            case 29 -> R.id.widget_lunar_29;
            case 30 -> R.id.widget_lunar_30;
            case 31 -> R.id.widget_lunar_31;
            case 32 -> R.id.widget_lunar_32;
            case 33 -> R.id.widget_lunar_33;
            case 34 -> R.id.widget_lunar_34;
            case 35 -> R.id.widget_lunar_35;
            case 36 -> R.id.widget_lunar_36;
            case 37 -> R.id.widget_lunar_37;
            case 38 -> R.id.widget_lunar_38;
            case 39 -> R.id.widget_lunar_39;
            case 40 -> R.id.widget_lunar_40;
            case 41 -> R.id.widget_lunar_41;
            case 42 -> R.id.widget_lunar_42;
            default -> -1;
        };
    }

    private static int getShiftResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_shift_1;
            case 2 -> R.id.widget_shift_2;
            case 3 -> R.id.widget_shift_3;
            case 4 -> R.id.widget_shift_4;
            case 5 -> R.id.widget_shift_5;
            case 6 -> R.id.widget_shift_6;
            case 7 -> R.id.widget_shift_7;
            case 8 -> R.id.widget_shift_8;
            case 9 -> R.id.widget_shift_9;
            case 10 -> R.id.widget_shift_10;
            case 11 -> R.id.widget_shift_11;
            case 12 -> R.id.widget_shift_12;
            case 13 -> R.id.widget_shift_13;
            case 14 -> R.id.widget_shift_14;
            case 15 -> R.id.widget_shift_15;
            case 16 -> R.id.widget_shift_16;
            case 17 -> R.id.widget_shift_17;
            case 18 -> R.id.widget_shift_18;
            case 19 -> R.id.widget_shift_19;
            case 20 -> R.id.widget_shift_20;
            case 21 -> R.id.widget_shift_21;
            case 22 -> R.id.widget_shift_22;
            case 23 -> R.id.widget_shift_23;
            case 24 -> R.id.widget_shift_24;
            case 25 -> R.id.widget_shift_25;
            case 26 -> R.id.widget_shift_26;
            case 27 -> R.id.widget_shift_27;
            case 28 -> R.id.widget_shift_28;
            case 29 -> R.id.widget_shift_29;
            case 30 -> R.id.widget_shift_30;
            case 31 -> R.id.widget_shift_31;
            case 32 -> R.id.widget_shift_32;
            case 33 -> R.id.widget_shift_33;
            case 34 -> R.id.widget_shift_34;
            case 35 -> R.id.widget_shift_35;
            case 36 -> R.id.widget_shift_36;
            case 37 -> R.id.widget_shift_37;
            case 38 -> R.id.widget_shift_38;
            case 39 -> R.id.widget_shift_39;
            case 40 -> R.id.widget_shift_40;
            case 41 -> R.id.widget_shift_41;
            case 42 -> R.id.widget_shift_42;
            default -> -1;
        };
    }

    private static int getHolidayResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_holiday_1;
            case 2 -> R.id.widget_holiday_2;
            case 3 -> R.id.widget_holiday_3;
            case 4 -> R.id.widget_holiday_4;
            case 5 -> R.id.widget_holiday_5;
            case 6 -> R.id.widget_holiday_6;
            case 7 -> R.id.widget_holiday_7;
            case 8 -> R.id.widget_holiday_8;
            case 9 -> R.id.widget_holiday_9;
            case 10 -> R.id.widget_holiday_10;
            case 11 -> R.id.widget_holiday_11;
            case 12 -> R.id.widget_holiday_12;
            case 13 -> R.id.widget_holiday_13;
            case 14 -> R.id.widget_holiday_14;
            case 15 -> R.id.widget_holiday_15;
            case 16 -> R.id.widget_holiday_16;
            case 17 -> R.id.widget_holiday_17;
            case 18 -> R.id.widget_holiday_18;
            case 19 -> R.id.widget_holiday_19;
            case 20 -> R.id.widget_holiday_20;
            case 21 -> R.id.widget_holiday_21;
            case 22 -> R.id.widget_holiday_22;
            case 23 -> R.id.widget_holiday_23;
            case 24 -> R.id.widget_holiday_24;
            case 25 -> R.id.widget_holiday_25;
            case 26 -> R.id.widget_holiday_26;
            case 27 -> R.id.widget_holiday_27;
            case 28 -> R.id.widget_holiday_28;
            case 29 -> R.id.widget_holiday_29;
            case 30 -> R.id.widget_holiday_30;
            case 31 -> R.id.widget_holiday_31;
            case 32 -> R.id.widget_holiday_32;
            case 33 -> R.id.widget_holiday_33;
            case 34 -> R.id.widget_holiday_34;
            case 35 -> R.id.widget_holiday_35;
            case 36 -> R.id.widget_holiday_36;
            case 37 -> R.id.widget_holiday_37;
            case 38 -> R.id.widget_holiday_38;
            case 39 -> R.id.widget_holiday_39;
            case 40 -> R.id.widget_holiday_40;
            case 41 -> R.id.widget_holiday_41;
            case 42 -> R.id.widget_holiday_42;
            default -> -1;
        };
    }

    private static int getAlarmResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_alarm_1;
            case 2 -> R.id.widget_alarm_2;
            case 3 -> R.id.widget_alarm_3;
            case 4 -> R.id.widget_alarm_4;
            case 5 -> R.id.widget_alarm_5;
            case 6 -> R.id.widget_alarm_6;
            case 7 -> R.id.widget_alarm_7;
            case 8 -> R.id.widget_alarm_8;
            case 9 -> R.id.widget_alarm_9;
            case 10 -> R.id.widget_alarm_10;
            case 11 -> R.id.widget_alarm_11;
            case 12 -> R.id.widget_alarm_12;
            case 13 -> R.id.widget_alarm_13;
            case 14 -> R.id.widget_alarm_14;
            case 15 -> R.id.widget_alarm_15;
            case 16 -> R.id.widget_alarm_16;
            case 17 -> R.id.widget_alarm_17;
            case 18 -> R.id.widget_alarm_18;
            case 19 -> R.id.widget_alarm_19;
            case 20 -> R.id.widget_alarm_20;
            case 21 -> R.id.widget_alarm_21;
            case 22 -> R.id.widget_alarm_22;
            case 23 -> R.id.widget_alarm_23;
            case 24 -> R.id.widget_alarm_24;
            case 25 -> R.id.widget_alarm_25;
            case 26 -> R.id.widget_alarm_26;
            case 27 -> R.id.widget_alarm_27;
            case 28 -> R.id.widget_alarm_28;
            case 29 -> R.id.widget_alarm_29;
            case 30 -> R.id.widget_alarm_30;
            case 31 -> R.id.widget_alarm_31;
            case 32 -> R.id.widget_alarm_32;
            case 33 -> R.id.widget_alarm_33;
            case 34 -> R.id.widget_alarm_34;
            case 35 -> R.id.widget_alarm_35;
            case 36 -> R.id.widget_alarm_36;
            case 37 -> R.id.widget_alarm_37;
            case 38 -> R.id.widget_alarm_38;
            case 39 -> R.id.widget_alarm_39;
            case 40 -> R.id.widget_alarm_40;
            case 41 -> R.id.widget_alarm_41;
            case 42 -> R.id.widget_alarm_42;
            default -> -1;
        };
    }

    private static int getCellResourceId(int day) {
        return switch (day) {
            case 1 -> R.id.widget_cell_1;
            case 2 -> R.id.widget_cell_2;
            case 3 -> R.id.widget_cell_3;
            case 4 -> R.id.widget_cell_4;
            case 5 -> R.id.widget_cell_5;
            case 6 -> R.id.widget_cell_6;
            case 7 -> R.id.widget_cell_7;
            case 8 -> R.id.widget_cell_8;
            case 9 -> R.id.widget_cell_9;
            case 10 -> R.id.widget_cell_10;
            case 11 -> R.id.widget_cell_11;
            case 12 -> R.id.widget_cell_12;
            case 13 -> R.id.widget_cell_13;
            case 14 -> R.id.widget_cell_14;
            case 15 -> R.id.widget_cell_15;
            case 16 -> R.id.widget_cell_16;
            case 17 -> R.id.widget_cell_17;
            case 18 -> R.id.widget_cell_18;
            case 19 -> R.id.widget_cell_19;
            case 20 -> R.id.widget_cell_20;
            case 21 -> R.id.widget_cell_21;
            case 22 -> R.id.widget_cell_22;
            case 23 -> R.id.widget_cell_23;
            case 24 -> R.id.widget_cell_24;
            case 25 -> R.id.widget_cell_25;
            case 26 -> R.id.widget_cell_26;
            case 27 -> R.id.widget_cell_27;
            case 28 -> R.id.widget_cell_28;
            case 29 -> R.id.widget_cell_29;
            case 30 -> R.id.widget_cell_30;
            case 31 -> R.id.widget_cell_31;
            case 32 -> R.id.widget_cell_32;
            case 33 -> R.id.widget_cell_33;
            case 34 -> R.id.widget_cell_34;
            case 35 -> R.id.widget_cell_35;
            case 36 -> R.id.widget_cell_36;
            case 37 -> R.id.widget_cell_37;
            case 38 -> R.id.widget_cell_38;
            case 39 -> R.id.widget_cell_39;
            case 40 -> R.id.widget_cell_40;
            case 41 -> R.id.widget_cell_41;
            case 42 -> R.id.widget_cell_42;
            default -> -1;
        };
    }

    private static PendingIntent getPendingIntent(Context context, int appWidgetId, String action) {
        Intent intent = new Intent(context, CalendarWidget.class);
        intent.setAction(action);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        return PendingIntent.getBroadcast(context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void updateAllWidgets(Context context) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, CalendarWidget.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }
}
