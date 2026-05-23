package com.example.myapplication.data;

import java.io.Serializable;

public class AlarmSetting implements Serializable {
    private String id;
    private String team;
    private String shiftType;
    private int hour;
    private int minute;
    private String reminderType;
    private boolean enabled;
    private String ringtone;
    private boolean vibrate;
    private boolean fade = true;
    private int repeatType = 0;
    private String weekdays = "";
    private long repeatDate = 0;

    public AlarmSetting() {
        this.team = "丁班";
    }

    public AlarmSetting(String id, String shiftType, int hour, int minute, String reminderType, boolean enabled) {
        this.id = id;
        this.shiftType = shiftType;
        this.hour = hour;
        this.minute = minute;
        this.reminderType = reminderType;
        this.enabled = enabled;
        this.ringtone = "default";
        this.vibrate = true;
        this.fade = true;
        this.team = "丁班";
    }

    public boolean isFade() {
        return fade;
    }

    public void setFade(boolean fade) {
        this.fade = fade;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getRingtone() {
        return ringtone;
    }

    public void setRingtone(String ringtone) {
        this.ringtone = ringtone;
    }

    public boolean isVibrate() {
        return vibrate;
    }

    public void setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
    }

    public int getRepeatType() {
        return repeatType;
    }

    public void setRepeatType(int repeatType) {
        this.repeatType = repeatType;
    }

    public String getWeekdays() {
        return weekdays;
    }

    public void setWeekdays(String weekdays) {
        this.weekdays = weekdays;
    }

    public long getRepeatDate() {
        return repeatDate;
    }

    public void setRepeatDate(long repeatDate) {
        this.repeatDate = repeatDate;
    }

    public String getTimeString() {
        return String.format("%02d:%02d", hour, minute);
    }
}