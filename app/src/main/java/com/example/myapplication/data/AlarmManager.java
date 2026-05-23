package com.example.myapplication.data;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.AlarmReceiver;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AlarmManager {
    private static final String PREF_NAME = "alarm_settings";
    private static final String ALARMS_KEY = "alarms";
    private static final String SHIFT_PREFS_NAME = "shift_prefs";
    private static final String PREF_CURRENT_GROUP = "current_group";
    private static final String TAG = "AlarmManager";
    public static final String ACTION_ALARM_CHANGED = "com.example.myapplication.ALARM_CHANGED";

    private static final int REPEAT_ONCE = 0;
    private static final int REPEAT_DAILY = 1;
    private static final int REPEAT_WEEKLY = 2;
    private static final int REPEAT_DATE = 3;

    private Context context;
    private List<AlarmSetting> alarmList;
    private SharedPreferences preferences;

    public AlarmManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadAlarms();
    }

    private void loadAlarms() {
        alarmList = new ArrayList<>();
        String json = preferences.getString(ALARMS_KEY, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                AlarmSetting alarm = new AlarmSetting();
                alarm.setId(obj.getString("id"));
                alarm.setTeam(obj.optString("team", "丁班"));
                alarm.setShiftType(obj.getString("shiftType"));
                alarm.setHour(obj.getInt("hour"));
                alarm.setMinute(obj.getInt("minute"));
                alarm.setReminderType(obj.getString("reminderType"));
                alarm.setEnabled(obj.getBoolean("enabled"));
                alarm.setRingtone(obj.optString("ringtone", "default"));
                alarm.setVibrate(obj.optBoolean("vibrate", true));
                alarm.setFade(obj.optBoolean("fade", true));
                alarm.setRepeatType(obj.optInt("repeatType", 0));
                alarm.setWeekdays(obj.optString("weekdays", ""));
                alarm.setRepeatDate(obj.optLong("repeatDate", 0));
                alarmList.add(alarm);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveAlarms() {
        JSONArray array = new JSONArray();
        for (AlarmSetting alarm : alarmList) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", alarm.getId());
                obj.put("team", alarm.getTeam());
                obj.put("shiftType", alarm.getShiftType());
                obj.put("hour", alarm.getHour());
                obj.put("minute", alarm.getMinute());
                obj.put("reminderType", alarm.getReminderType());
                obj.put("enabled", alarm.isEnabled());
                obj.put("ringtone", alarm.getRingtone());
                obj.put("vibrate", alarm.isVibrate());
                obj.put("fade", alarm.isFade());
                obj.put("repeatType", alarm.getRepeatType());
                obj.put("weekdays", alarm.getWeekdays());
                obj.put("repeatDate", alarm.getRepeatDate());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        preferences.edit().putString(ALARMS_KEY, array.toString()).apply();
        notifyAlarmChanged();
    }

    private void notifyAlarmChanged() {
        Intent intent = new Intent(ACTION_ALARM_CHANGED);
        context.sendBroadcast(intent);
    }

    public AlarmSetting getAlarmById(String id) {
        for (AlarmSetting alarm : alarmList) {
            if (alarm.getId().equals(id)) {
                return alarm;
            }
        }
        return null;
    }

    public List<AlarmSetting> getAllAlarms() {
        return alarmList;
    }

    public void toggleAlarm(String id) {
        for (AlarmSetting alarm : alarmList) {
            if (alarm.getId().equals(id)) {
                alarm.setEnabled(!alarm.isEnabled());
                saveAlarms();
                if (alarm.isEnabled() && isAlarmServiceEnabled()) {
                    scheduleAlarm(alarm);
                } else {
                    cancelAlarm(alarm);
                }
                break;
            }
        }
    }

    private static final String PREF_ALARM_SERVICE_ENABLED = "alarm_service_enabled";

    public boolean isAlarmServiceEnabled() {
        return preferences.getBoolean(PREF_ALARM_SERVICE_ENABLED, true);
    }

    public void setAlarmServiceEnabled(boolean enabled) {
        preferences.edit().putBoolean(PREF_ALARM_SERVICE_ENABLED, enabled).apply();
        if (enabled) {
            scheduleAllAlarms();
            startPersistentAlarmService();
        } else {
            cancelAllAlarms();
            AlarmNotificationHelper.cancelNextAlarmNotification(context);
        }
        notifyAlarmChanged();
    }

    private void startPersistentAlarmService() {
        Intent serviceIntent = new Intent(context, com.example.myapplication.PersistentAlarmService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.d(TAG, "启动PersistentAlarmService");
    }

    public void cancelAllAlarms() {
        for (AlarmSetting alarm : alarmList) {
            cancelAlarm(alarm);
        }
    }

    public void addAlarm(AlarmSetting alarm) {
        if (alarm.getId() == null || alarm.getId().isEmpty()) {
            alarm.setId(UUID.randomUUID().toString());
        }
        alarmList.add(alarm);
        saveAlarms();
        if (alarm.isEnabled() && isAlarmServiceEnabled()) {
            scheduleAlarm(alarm);
        }
    }

    public void updateAlarm(AlarmSetting alarm) {
        for (int i = 0; i < alarmList.size(); i++) {
            if (alarmList.get(i).getId().equals(alarm.getId())) {
                alarmList.set(i, alarm);
                saveAlarms();
                if (alarm.isEnabled() && isAlarmServiceEnabled()) {
                    scheduleAlarm(alarm);
                } else {
                    cancelAlarm(alarm);
                }
                break;
            }
        }
    }

    public void removeAlarm(AlarmSetting alarm) {
        alarmList.remove(alarm);
        saveAlarms();
        cancelAlarm(alarm);
    }

    public void deleteAlarm(String id) {
        for (int i = 0; i < alarmList.size(); i++) {
            if (alarmList.get(i).getId().equals(id)) {
                AlarmSetting alarm = alarmList.get(i);
                alarmList.remove(i);
                saveAlarms();
                cancelAlarm(alarm);
                break;
            }
        }
    }

    private void cancelAlarm(AlarmSetting alarm) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);

            PendingIntent activityPendingIntent = PendingIntent.getActivity(
                    context,
                    alarm.getId().hashCode() + 1000,
                    new Intent(context, AlarmReceiver.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(activityPendingIntent);
        }
    }

    public static long getNextAlarmTime(AlarmSetting alarm) {
        Calendar now = Calendar.getInstance();
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmCal.set(Calendar.MINUTE, alarm.getMinute());
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);

        if (alarmCal.after(now)) {
            return alarmCal.getTimeInMillis();
        }

        Calendar nextDay = (Calendar) alarmCal.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        return nextDay.getTimeInMillis();
    }

    public long getNextCustomAlarmTime(AlarmSetting alarm) {
        Calendar now = Calendar.getInstance();
        
        if ("日历闹钟".equals(alarm.getShiftType())) {
            return getNextDateAlarmTime(alarm, now);
        }
        
        int repeatType = alarm.getRepeatType();

        switch (repeatType) {
            case REPEAT_ONCE:
                return getNextOnceAlarmTime(alarm, now);

            case REPEAT_DAILY:
                return getNextDailyAlarmTime(alarm, now);

            case REPEAT_WEEKLY:
                return getNextWeeklyAlarmTime(alarm, now);

            case REPEAT_DATE:
                return getNextDateAlarmTime(alarm, now);

            default:
                return getNextOnceAlarmTime(alarm, now);
        }
    }

    private long getNextOnceAlarmTime(AlarmSetting alarm, Calendar now) {
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmCal.set(Calendar.MINUTE, alarm.getMinute());
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);

        if (alarmCal.after(now)) {
            return alarmCal.getTimeInMillis();
        }

        return alarmCal.getTimeInMillis() + 24 * 60 * 60 * 1000;
    }

    private long getNextDailyAlarmTime(AlarmSetting alarm, Calendar now) {
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmCal.set(Calendar.MINUTE, alarm.getMinute());
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);

        if (alarmCal.after(now)) {
            return alarmCal.getTimeInMillis();
        }

        alarmCal.add(Calendar.DAY_OF_MONTH, 1);
        return alarmCal.getTimeInMillis();
    }

    private long getNextWeeklyAlarmTime(AlarmSetting alarm, Calendar now) {
        String weekdaysStr = alarm.getWeekdays();
        if (weekdaysStr == null || weekdaysStr.isEmpty()) {
            return getNextDailyAlarmTime(alarm, now);
        }

        Set<Integer> weekdays = new HashSet<>();
        for (String day : weekdaysStr.split(",")) {
            weekdays.add(Integer.parseInt(day));
        }

        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmCal.set(Calendar.MINUTE, alarm.getMinute());
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i <= 7; i++) {
            int dayOfWeek = alarmCal.get(Calendar.DAY_OF_WEEK);
            if (weekdays.contains(dayOfWeek) && alarmCal.after(now)) {
                return alarmCal.getTimeInMillis();
            }
            alarmCal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return alarmCal.getTimeInMillis();
    }

    private long getNextDateAlarmTime(AlarmSetting alarm, Calendar now) {
        long date = alarm.getRepeatDate();
        if (date <= 0) {
            return getNextOnceAlarmTime(alarm, now);
        }

        Calendar targetDate = Calendar.getInstance();
        targetDate.setTimeInMillis(date);
        targetDate.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        targetDate.set(Calendar.MINUTE, alarm.getMinute());
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);

        if (targetDate.after(now)) {
            return targetDate.getTimeInMillis();
        }

        return 0;
    }

    public long getNextAlarmTimeForShift(AlarmSetting alarm) {
        Calendar now = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();
        startDate.set(2026, 3, 26);
        Log.d(TAG, "原始起始日期: " + startDate.get(Calendar.YEAR) + "-" + (startDate.get(Calendar.MONTH) + 1) + "-" + startDate.get(Calendar.DAY_OF_MONTH));

        String currentTeam = getCurrentTeam();
        int groupOffset = getGroupOffset(currentTeam);
        startDate.add(Calendar.DAY_OF_MONTH, groupOffset);
        Log.d(TAG, "偏移后起始日期(" + currentTeam + ", offset=" + groupOffset + "): " + startDate.get(Calendar.YEAR) + "-" + (startDate.get(Calendar.MONTH) + 1) + "-" + startDate.get(Calendar.DAY_OF_MONTH));

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Log.d(TAG, "今天日期: " + today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.DAY_OF_MONTH));

        long todayDaysFromStart = getDaysFromStart(today.getTime(), startDate.getTime());
        Log.d(TAG, "今天距离起始日期天数: " + todayDaysFromStart);
        String todayShift = getShiftTypeForDay(4, todayDaysFromStart);

        Calendar alarmCal = Calendar.getInstance();
        alarmCal.set(Calendar.HOUR_OF_DAY, alarm.getHour());
        alarmCal.set(Calendar.MINUTE, alarm.getMinute());
        alarmCal.set(Calendar.SECOND, 0);
        alarmCal.set(Calendar.MILLISECOND, 0);
        Log.d(TAG, "闹钟原始时间: " + alarmCal.get(Calendar.YEAR) + "-" + (alarmCal.get(Calendar.MONTH) + 1) + "-" + alarmCal.get(Calendar.DAY_OF_MONTH) + " " + alarmCal.get(Calendar.HOUR_OF_DAY) + ":" + alarmCal.get(Calendar.MINUTE));

        if (alarmCal.before(now)) {
            alarmCal.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "闹钟时间已过，调整为: " + alarmCal.get(Calendar.YEAR) + "-" + (alarmCal.get(Calendar.MONTH) + 1) + "-" + alarmCal.get(Calendar.DAY_OF_MONTH) + " " + alarmCal.get(Calendar.HOUR_OF_DAY) + ":" + alarmCal.get(Calendar.MINUTE));
        }

        long alarmDaysFromStart = getDaysFromStart(alarmCal.getTime(), startDate.getTime());
        Log.d(TAG, "闹钟日期距离起始日期天数: " + alarmDaysFromStart);
        String alarmDayShift = getShiftTypeForDay(4, alarmDaysFromStart);
        Log.d(TAG, "今天班次: " + todayShift + ", 闹钟要求班次: " + alarm.getShiftType() + ", 闹钟日期班次: " + alarmDayShift);

        if (alarmDayShift.equals(alarm.getShiftType())) {
            return alarmCal.getTimeInMillis();
        }

        for (int i = 1; i <= 365; i++) {
            Calendar nextCal = (Calendar) alarmCal.clone();
            nextCal.add(Calendar.DAY_OF_MONTH, i);

            long nextDaysFromStart = getDaysFromStart(nextCal.getTime(), startDate.getTime());
            String nextShift = getShiftTypeForDay(4, nextDaysFromStart);

            if (nextShift.equals(alarm.getShiftType())) {
                Log.d(TAG, "找到匹配班次的日期: " + nextCal.get(Calendar.YEAR) + "-" + (nextCal.get(Calendar.MONTH) + 1) + "-" + nextCal.get(Calendar.DAY_OF_MONTH) + ", 班次: " + nextShift);
                return nextCal.getTimeInMillis();
            }
        }

        return alarmCal.getTimeInMillis() + 24 * 60 * 60 * 1000;
    }

    private String getCurrentTeam() {
        SharedPreferences shiftPrefs = context.getSharedPreferences(SHIFT_PREFS_NAME, Context.MODE_PRIVATE);
        return shiftPrefs.getString(PREF_CURRENT_GROUP, "丁班");
    }

    private static int getGroupOffset(String groupName) {
        switch (groupName) {
            case "丁班": return 0;
            case "丙班": return 1;
            case "乙班": return 2;
            case "甲班": return 3;
            default: return 0;
        }
    }

    private static String getShiftTypeForDay(int cycleDays, long daysFromStart) {
        String[] defaultShifts = {"白班", "上夜班", "下夜班", "正休"};
        int cycleIndex = (int) (daysFromStart % cycleDays);
        if (cycleIndex < 0) {
            cycleIndex += cycleDays;
        }
        if (cycleIndex < defaultShifts.length) {
            return defaultShifts[cycleIndex];
        }
        return "白班";
    }

    private static long getDaysFromStart(Date current, Date start) {
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTime(current);
        currentCal.set(Calendar.HOUR_OF_DAY, 0);
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        currentCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(start);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        long diff = currentCal.getTimeInMillis() - startCal.getTimeInMillis();
        long days = diff / (1000 * 60 * 60 * 24);

        return days;
    }

    public void scheduleAllAlarms() {
        for (AlarmSetting alarm : alarmList) {
            if (alarm.isEnabled()) {
                scheduleAlarm(alarm);
            }
        }
    }

    public AlarmSetting getNextAlarm() {
        long now = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        AlarmSetting nextAlarm = null;
        long nextTime = Long.MAX_VALUE;

        for (AlarmSetting alarm : alarmList) {
            if (!alarm.isEnabled()) {
                continue;
            }

            long alarmTime;
            if ("普通闹钟".equals(alarm.getShiftType()) || "日历闹钟".equals(alarm.getShiftType())) {
                alarmTime = getNextCustomAlarmTime(alarm);
            } else {
                alarmTime = getNextAlarmTimeForShift(alarm);
            }

            if (alarmTime > 0 && alarmTime > now && alarmTime < nextTime) {
                nextTime = alarmTime;
                nextAlarm = alarm;
            }
        }

        return nextAlarm;
    }

    public void scheduleAlarm(AlarmSetting alarm) {
        cancelAlarm(alarm);

        long alarmTime;
        if ("普通闹钟".equals(alarm.getShiftType()) || "日历闹钟".equals(alarm.getShiftType())) {
            alarmTime = getNextCustomAlarmTime(alarm);
            Log.d(TAG, "调度普通/日历闹钟: " + alarm.getShiftType() + " " + alarm.getReminderType() + ", 时间: " + alarmTime);
        } else {
            alarmTime = getNextAlarmTimeForShift(alarm);
            Log.d(TAG, "调度倒班闹钟: " + alarm.getReminderType() + ", 类型: " + alarm.getShiftType() + ", 时间: " + alarmTime);
        }

        if (alarmTime <= 0) {
            Log.d(TAG, "闹钟时间无效，跳过调度: " + alarm.getShiftType() + " " + alarm.getReminderType());
            return;
        }

        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("alarmId", alarm.getId());

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.getId().hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        pendingIntent
                );
            }
        }
    }
}