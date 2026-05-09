package com.example.myapplication.data;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ShiftCalendarUtil {
    public static final String SHIFT_WHITE = "白班";
    public static final String SHIFT_NIGHT = "上夜班";
    public static final String SHIFT_EVENING = "下夜班";
    public static final String SHIFT_REST = "正休";

    private static final String[] CHINESE_NUMBERS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"};
    private static final String[] SOLAR_TERMS = {"立春", "雨水", "惊蛰", "春分", "清明", "谷雨", "立夏", "小满", "芒种", "夏至", "小暑", "大暑",
            "立秋", "处暑", "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至", "小寒", "大寒"};

    private static final String[][] HOLIDAYS = {
            {"01-01", "元旦节"},
            {"02-14", "情人节"},
            {"03-08", "妇女节"},
            {"03-12", "植树节"},
            {"04-01", "愚人节"},
            {"04-05", "清明节"},
            {"05-01", "劳动节"},
            {"05-04", "青年节"},
            {"05-12", "护士节"},
            {"06-01", "儿童节"},
            {"06-23", "端午节"},
            {"07-01", "建党节"},
            {"08-01", "建军节"},
            {"09-10", "教师节"},
            {"09-28", "中秋节"},
            {"10-01", "国庆节"},
            {"12-25", "圣诞节"}
    };

    private static final String[][] LEGAL_HOLIDAYS = {
            {"01-01", "休"},
            {"01-02", "休"},
            {"01-03", "休"},
            {"02-15", "休"},
            {"02-16", "休"},
            {"02-17", "休"},
            {"02-18", "休"},
            {"02-19", "休"},
            {"02-20", "休"},
            {"02-21", "休"},
            {"02-22", "休"},
            {"02-23", "休"},
            {"04-04", "休"},
            {"04-05", "休"},
            {"04-06", "休"},
            {"05-01", "休"},
            {"05-02", "休"},
            {"05-03", "休"},
            {"05-04", "休"},
            {"05-05", "休"},
            {"06-19", "休"},
            {"06-20", "休"},
            {"06-21", "休"},
            {"09-25", "休"},
            {"09-26", "休"},
            {"09-27", "休"},
            {"10-01", "休"},
            {"10-02", "休"},
            {"10-03", "休"},
            {"10-04", "休"},
            {"10-05", "休"},
            {"10-06", "休"},
            {"10-07", "休"}
    };

    private static final String[][] WORK_ON_WEEKEND = {
            {"01-04", "班"},
            {"01-14", "班"},
            {"02-28", "班"},
            {"05-09", "班"},
            {"09-20", "班"},
            {"10-10", "班"}
    };

    private static final String[][] SOLAR_TERMS_DATES = {
            {"01-06", "小寒"}, {"01-20", "大寒"},
            {"02-04", "立春"}, {"02-19", "雨水"},
            {"03-06", "惊蛰"}, {"03-21", "春分"},
            {"04-05", "清明"}, {"04-20", "谷雨"},
            {"05-06", "立夏"}, {"05-21", "小满"},
            {"06-06", "芒种"}, {"06-22", "夏至"},
            {"07-07", "小暑"}, {"07-23", "大暑"},
            {"08-08", "立秋"}, {"08-23", "处暑"},
            {"09-08", "白露"}, {"09-23", "秋分"},
            {"10-08", "寒露"}, {"10-24", "霜降"},
            {"11-08", "立冬"}, {"11-22", "小雪"},
            {"12-07", "大雪"}, {"12-22", "冬至"}
    };

    private static final String[][] LUNAR_HOLIDAYS = {
            {"正月初一", "春节"},
            {"正月十五", "元宵节"},
            {"五月初五", "端午节"},
            {"七月初七", "七夕节"},
            {"八月十五", "中秋节"},
            {"九月初九", "重阳节"},
            {"腊月三十", "除夕"}
    };

    public static List<ShiftDay> generateMonthShiftDays(int year, int month, ShiftRule rule) {
        List<ShiftDay> days = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        Calendar startDate = Calendar.getInstance();
        startDate.set(2026, 3, 26);

        int groupOffset = getGroupOffset(rule.getName());
        startDate.add(Calendar.DAY_OF_MONTH, groupOffset);

        Calendar current = Calendar.getInstance();
        current.set(year, month - 1, 1);
        
        int offset = firstDayOfWeek - 2;
        if (offset < 0) {
            offset += 7;
        }
        current.add(Calendar.DAY_OF_MONTH, -offset);

        for (int i = 0; i < 42; i++) {
            int dayYear = current.get(Calendar.YEAR);
            int dayMonth = current.get(Calendar.MONTH) + 1;
            int dayDay = current.get(Calendar.DAY_OF_MONTH);

            ShiftDay shiftDay = new ShiftDay(dayYear, dayMonth, dayDay, "");
            String lunarMonth = getLunarMonth(dayYear, dayMonth, dayDay);
            String lunarDate = getLunarDate(dayYear, dayMonth, dayDay);
            shiftDay.setLunarDate(lunarDate);
            shiftDay.setLunarMonth(lunarMonth);
            
            String holiday = getHoliday(dayMonth, dayDay);
            if (holiday == null || holiday.isEmpty()) {
                holiday = getLunarHoliday(lunarMonth, lunarDate);
            }
            shiftDay.setHoliday(holiday);
            
            shiftDay.setHolidayMark(getHolidayMark(dayYear, dayMonth, dayDay));
            shiftDay.setSolarTerm(getSolarTerm(dayMonth, dayDay));
            shiftDay.setShiftType(getShiftTypeForDay(rule, getDaysFromStart(current.getTime(), startDate.getTime())));

            days.add(shiftDay);
            current.add(Calendar.DAY_OF_MONTH, 1);
        }

        return days;
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

    private static ShiftDay createShiftDay(Calendar calendar, ShiftRule rule, Calendar startDate) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        ShiftDay shiftDay = new ShiftDay(year, month, day, "");
        shiftDay.setLunarDate(getLunarDate(year, month, day));
        shiftDay.setHoliday(getHoliday(month, day));

        return shiftDay;
    }

    private static long getDaysFromStart(Date current, Date start) {
        return (current.getTime() - start.getTime()) / (1000 * 60 * 60 * 24);
    }

    private static String getShiftTypeForDay(ShiftRule rule, long daysFromStart) {
        if (rule == null || rule.getShiftDetails() == null || rule.getShiftDetails().isEmpty()) {
            return SHIFT_WHITE;
        }
        int cycleIndex = (int) (daysFromStart % rule.getCycleDays());
        if (cycleIndex < 0) {
            cycleIndex += rule.getCycleDays();
        }
        if (cycleIndex < rule.getShiftDetails().size()) {
            return rule.getShiftDetails().get(cycleIndex).getShiftName();
        }
        return SHIFT_WHITE;
    }

    private static String getLunarDate(int year, int month, int day) {
        try {
            java.util.Calendar solarCal = java.util.Calendar.getInstance();
            solarCal.set(year, month - 1, day);

            android.icu.util.ChineseCalendar lunarCal = new android.icu.util.ChineseCalendar(
                    android.icu.util.TimeZone.getDefault(),
                    android.icu.util.ULocale.CHINA);
            lunarCal.setTime(solarCal.getTime());

            int lunarDay = lunarCal.get(android.icu.util.Calendar.DAY_OF_MONTH);
            return getChineseDay(lunarDay);
        } catch (Exception e) {
            return getSimpleLunarDay(day);
        }
    }

    private static String getLunarMonth(int year, int month, int day) {
        try {
            java.util.Calendar solarCal = java.util.Calendar.getInstance();
            solarCal.set(year, month - 1, day);

            android.icu.util.ChineseCalendar lunarCal = new android.icu.util.ChineseCalendar(
                    android.icu.util.TimeZone.getDefault(),
                    android.icu.util.ULocale.CHINA);
            lunarCal.setTime(solarCal.getTime());

            int lunarMonth = lunarCal.get(android.icu.util.Calendar.MONTH);
            return getChineseMonth(lunarMonth);
        } catch (Exception e) {
            return "";
        }
    }

    private static String getChineseMonth(int month) {
        String[] months = {"正月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "冬月", "腊月"};
        if (month >= 0 && month < months.length) {
            return months[month];
        }
        return "";
    }

    private static String getChineseDay(int day) {
        String[] days = {"", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};
        if (day > 0 && day < days.length) {
            return days[day];
        }
        return "";
    }

    private static String getSimpleLunarDay(int day) {
        String[] days = {"", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};
        int lunarDay = (day - 1) % 30 + 1;
        if (lunarDay > 0 && lunarDay < days.length) {
            return days[lunarDay];
        }
        return "";
    }

    private static String getHoliday(int month, int day) {
        String dateStr = String.format("%02d-%02d", month, day);
        for (String[] holiday : HOLIDAYS) {
            if (holiday[0].equals(dateStr)) {
                return holiday[1];
            }
        }
        return null;
    }

    private static String getHolidayMark(int year, int month, int day) {
        return HolidayManager.getInstance().getHolidayMark(year, month, day);
    }

    private static String getSolarTerm(int month, int day) {
        String dateStr = String.format("%02d-%02d", month, day);
        for (String[] term : SOLAR_TERMS_DATES) {
            if (term[0].equals(dateStr)) {
                return term[1];
            }
        }
        return null;
    }

    private static String getLunarHoliday(String lunarMonth, String lunarDate) {
        if (lunarMonth == null || lunarDate == null || lunarMonth.isEmpty() || lunarDate.isEmpty()) {
            return null;
        }
        String lunarFullDate = lunarMonth + lunarDate;
        for (String[] holiday : LUNAR_HOLIDAYS) {
            if (holiday[0].equals(lunarFullDate)) {
                return holiday[1];
            }
        }
        return null;
    }

    public static String getWeekDayString(int dayOfWeek) {
        String[] weekDays = {"日", "一", "二", "三", "四", "五", "六"};
        return weekDays[dayOfWeek];
    }

    public static String getShiftGroupName(int groupIndex) {
        String[] groups = {"甲班", "乙班", "丙班", "丁班", "戊班", "己班"};
        if (groupIndex >= 0 && groupIndex < groups.length) {
            return groups[groupIndex];
        }
        return "未知班组";
    }

    public static ShiftRule createDefaultRule() {
        ShiftRule rule = new ShiftRule();
        rule.setName("丁班");
        rule.setCompanyName("武钢二热轧");
        rule.setTag("");
        rule.setCycleDays(4);
        rule.setGroupCount(4);
        rule.setDefault(true);

        List<ShiftRule.ShiftDetail> details = new ArrayList<>();
        details.add(new ShiftRule.ShiftDetail(1, SHIFT_WHITE, "08:00到20:00"));
        details.add(new ShiftRule.ShiftDetail(2, SHIFT_NIGHT, "20:00到23:59"));
        details.add(new ShiftRule.ShiftDetail(3, SHIFT_EVENING, "00:00到08:00"));
        details.add(new ShiftRule.ShiftDetail(4, SHIFT_REST, "00:00到23:59"));
        rule.setShiftDetails(details);

        return rule;
    }
}