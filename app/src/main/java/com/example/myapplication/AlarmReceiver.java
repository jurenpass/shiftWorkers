package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmSetting;

import java.util.Calendar;
import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    private static final String SHIFT_PREFS_NAME = "shift_prefs";
    private static final String PREF_CURRENT_GROUP = "current_group";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "========== 闹钟触发了！==========");

        String alarmId = intent.getStringExtra("alarmId");

        Log.d(TAG, "收到参数: alarmId=" + alarmId);

        if (alarmId != null && !alarmId.isEmpty()) {
            AlarmManager alarmManager = new AlarmManager(context);
            AlarmSetting alarm = alarmManager.getAlarmById(alarmId);

            if (alarm != null && alarm.isEnabled()) {
                String todayShift = getTodayShiftType(context);
                String alarmShift = alarm.getShiftType();

                Log.d(TAG, "今天班次: " + todayShift + ", 闹钟要求班次: " + alarmShift);

                if (todayShift.equals(alarmShift)) {
                    Log.d(TAG, "班次匹配，启动闹钟响铃");
                    startAlarm(context, alarm);
                } else {
                    Log.d(TAG, "班次不匹配，重新设置闹钟");
                    alarmManager.scheduleAlarm(alarm);
                }
            } else {
                Log.d(TAG, "闹钟不存在或未启用");
            }
        } else {
            Log.d(TAG, "缺少闹钟ID参数");
        }
    }

    private void startAlarm(Context context, AlarmSetting alarm) {
        Log.d(TAG, "准备启动AlarmService: " + alarm.getShiftType() + " " + alarm.getReminderType() + " " + alarm.getHour() + ":" + alarm.getMinute());

        Intent serviceIntent = new Intent(context, AlarmService.class);
        serviceIntent.putExtra("alarm_id", alarm.getId());
        serviceIntent.putExtra("shift_type", alarm.getShiftType());
        serviceIntent.putExtra("reminder_type", alarm.getReminderType());
        serviceIntent.putExtra("hour", alarm.getHour());
        serviceIntent.putExtra("minute", alarm.getMinute());
        serviceIntent.putExtra("ringtone", alarm.getRingtone());
        serviceIntent.putExtra("vibrate", alarm.isVibrate());
        serviceIntent.putExtra("fade", alarm.isFade());

        Log.d(TAG, "发送参数: alarm_id=" + alarm.getId() + ", shift_type=" + alarm.getShiftType() + 
              ", reminder_type=" + alarm.getReminderType() + ", hour=" + alarm.getHour() + 
              ", minute=" + alarm.getMinute());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
            Log.d(TAG, "使用 startForegroundService 启动服务");
        } else {
            context.startService(serviceIntent);
            Log.d(TAG, "使用 startService 启动服务");
        }
    }

    private String getTodayShiftType(Context context) {
        String currentTeam = getCurrentTeam(context);
        Log.d(TAG, "当前班组: " + currentTeam);

        Calendar startDate = Calendar.getInstance();
        startDate.set(2026, 3, 26);
        Log.d(TAG, "原始起始日期: " + startDate.get(Calendar.YEAR) + "-" + (startDate.get(Calendar.MONTH) + 1) + "-" + startDate.get(Calendar.DAY_OF_MONTH));

        int groupOffset = getGroupOffset(currentTeam);
        startDate.add(Calendar.DAY_OF_MONTH, groupOffset);
        Log.d(TAG, "偏移后起始日期(" + currentTeam + ", offset=" + groupOffset + "): " + startDate.get(Calendar.YEAR) + "-" + (startDate.get(Calendar.MONTH) + 1) + "-" + startDate.get(Calendar.DAY_OF_MONTH));

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        Log.d(TAG, "今天日期: " + today.get(Calendar.YEAR) + "-" + (today.get(Calendar.MONTH) + 1) + "-" + today.get(Calendar.DAY_OF_MONTH));

        long daysFromStart = getDaysFromStart(today.getTime(), startDate.getTime());
        Log.d(TAG, "今天距离起始日期天数: " + daysFromStart);

        String shiftType = getShiftTypeForDay(4, daysFromStart);
        Log.d(TAG, "计算得出今天班次: " + shiftType);

        return shiftType;
    }

    private String getCurrentTeam(Context context) {
        SharedPreferences shiftPrefs = context.getSharedPreferences(SHIFT_PREFS_NAME, Context.MODE_PRIVATE);
        return shiftPrefs.getString(PREF_CURRENT_GROUP, "丁班");
    }

    private int getGroupOffset(String groupName) {
        switch (groupName) {
            case "丁班": return 0;
            case "丙班": return 1;
            case "乙班": return 2;
            case "甲班": return 3;
            default: return 0;
        }
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
        return diff / (1000 * 60 * 60 * 24);
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
}