package com.example.myapplication.data;

import java.util.Calendar;

public class ShiftDay {
    private int year;
    private int month;
    private int day;
    private String shiftType;
    private String lunarDate;
    private String holiday;
    private String lunarMonth;
    private String holidayMark;

    public ShiftDay(int year, int month, int day, String shiftType) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.shiftType = shiftType;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getShiftType() {
        return shiftType;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public String getLunarDate() {
        return lunarDate;
    }

    public void setLunarDate(String lunarDate) {
        this.lunarDate = lunarDate;
    }

    public String getHoliday() {
        return holiday;
    }

    public void setHoliday(String holiday) {
        this.holiday = holiday;
    }

    public String getLunarMonth() {
        return lunarMonth;
    }

    public void setLunarMonth(String lunarMonth) {
        this.lunarMonth = lunarMonth;
    }

    public String getHolidayMark() {
        return holidayMark;
    }

    public void setHolidayMark(String holidayMark) {
        this.holidayMark = holidayMark;
    }

    public Calendar toCalendar() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day);
        return calendar;
    }
}