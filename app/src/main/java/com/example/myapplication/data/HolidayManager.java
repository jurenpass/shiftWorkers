package com.example.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.Map;

public class HolidayManager {
    private static final String PREF_KEY = "holiday_data";
    private static final String PREF_LAST_UPDATE = "last_update_year";
    private static final String PREF_LAST_CHECK_TIME = "last_check_time";
    
    private Map<String, String> holidays = new HashMap<>();
    private Map<String, String> workDays = new HashMap<>();
    private int lastUpdateYear = 0;
    private long lastCheckTime = 0;
    
    private static HolidayManager instance;
    
    private HolidayManager() {
        loadDefaultHolidays();
    }
    
    public static synchronized HolidayManager getInstance() {
        if (instance == null) {
            instance = new HolidayManager();
        }
        return instance;
    }
    
    private void loadDefaultHolidays() {
        holidays.clear();
        workDays.clear();
        
        // 2026年官方数据
        String[][] legalHolidays = {
            {"2026-01-01", "休"}, {"2026-01-02", "休"}, {"2026-01-03", "休"},
            {"2026-02-15", "休"}, {"2026-02-16", "休"}, {"2026-02-17", "休"},
            {"2026-02-18", "休"}, {"2026-02-19", "休"}, {"2026-02-20", "休"},
            {"2026-02-21", "休"}, {"2026-02-22", "休"}, {"2026-02-23", "休"},
            {"2026-04-04", "休"}, {"2026-04-05", "休"}, {"2026-04-06", "休"},
            {"2026-05-01", "休"}, {"2026-05-02", "休"}, {"2026-05-03", "休"},
            {"2026-05-04", "休"}, {"2026-05-05", "休"},
            {"2026-06-19", "休"}, {"2026-06-20", "休"}, {"2026-06-21", "休"},
            {"2026-09-25", "休"}, {"2026-09-26", "休"}, {"2026-09-27", "休"},
            {"2026-10-01", "休"}, {"2026-10-02", "休"}, {"2026-10-03", "休"},
            {"2026-10-04", "休"}, {"2026-10-05", "休"}, {"2026-10-06", "休"}, {"2026-10-07", "休"}
        };
        
        String[][] workWeekend = {
            {"2026-01-04", "班"},
            {"2026-02-14", "班"}, {"2026-02-28", "班"},
            {"2026-05-09", "班"},
            {"2026-09-20", "班"},
            {"2026-10-10", "班"}
        };
        
        for (String[] holiday : legalHolidays) {
            holidays.put(holiday[0], holiday[1]);
        }
        for (String[] work : workWeekend) {
            workDays.put(work[0], work[1]);
        }
    }
    
    public String getHolidayMark(int year, int month, int day) {
        String dateStr = String.format("%d-%02d-%02d", year, month, day);
        
        if (holidays.containsKey(dateStr)) {
            return holidays.get(dateStr);
        }
        if (workDays.containsKey(dateStr)) {
            return workDays.get(dateStr);
        }
        return null;
    }
    
    public void updateHolidays(Map<String, String> newHolidays, Map<String, String> newWorkDays) {
        holidays.putAll(newHolidays);
        workDays.putAll(newWorkDays);
    }
    
    public void saveToPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        StringBuilder holidayStr = new StringBuilder();
        for (Map.Entry<String, String> entry : holidays.entrySet()) {
            if (holidayStr.length() > 0) holidayStr.append("|");
            holidayStr.append(entry.getKey()).append(",").append(entry.getValue());
        }
        editor.putString("holidays", holidayStr.toString());
        
        StringBuilder workStr = new StringBuilder();
        for (Map.Entry<String, String> entry : workDays.entrySet()) {
            if (workStr.length() > 0) workStr.append("|");
            workStr.append(entry.getKey()).append(",").append(entry.getValue());
        }
        editor.putString("work_days", workStr.toString());
        
        editor.putInt(PREF_LAST_UPDATE, lastUpdateYear);
        editor.putLong(PREF_LAST_CHECK_TIME, lastCheckTime);
        
        editor.apply();
    }
    
    public void loadFromPreferences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        
        String holidayStr = prefs.getString("holidays", "");
        if (!holidayStr.isEmpty()) {
            holidays.clear();
            String[] entries = holidayStr.split("\\|");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    holidays.put(parts[0], parts[1]);
                }
            }
        }
        
        String workStr = prefs.getString("work_days", "");
        if (!workStr.isEmpty()) {
            workDays.clear();
            String[] entries = workStr.split("\\|");
            for (String entry : entries) {
                String[] parts = entry.split(",");
                if (parts.length == 2) {
                    workDays.put(parts[0], parts[1]);
                }
            }
        }
        
        lastUpdateYear = prefs.getInt(PREF_LAST_UPDATE, 0);
        lastCheckTime = prefs.getLong(PREF_LAST_CHECK_TIME, 0);
    }
    
    public void setLastUpdateYear(int year) {
        this.lastUpdateYear = year;
    }
    
    public void setLastCheckTime(long time) {
        this.lastCheckTime = time;
    }
    
    public boolean shouldCheckUpdate(int currentYear) {
        long now = System.currentTimeMillis();
        long thirtyDays = 30L * 24 * 60 * 60 * 1000;
        
        if (now - lastCheckTime < thirtyDays) {
            return false;
        }
        
        if (lastUpdateYear >= currentYear) {
            return false;
        }
        
        return needsUpdate(currentYear);
    }
    
    public boolean needsUpdate(int currentYear) {
        return holidays.isEmpty() || !containsYear(currentYear);
    }
    
    private boolean containsYear(int year) {
        String yearPrefix = String.valueOf(year);
        for (String date : holidays.keySet()) {
            if (date.startsWith(yearPrefix)) {
                return true;
            }
        }
        return false;
    }
}
