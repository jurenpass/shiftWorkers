package com.example.myapplication.data;

import java.io.Serializable;
import java.util.List;

public class ShiftRule implements Serializable {
    private String name;
    private String companyName;
    private String tag;
    private int cycleDays;
    private int groupCount;
    private List<ShiftDetail> shiftDetails;
    private boolean isDefault;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getCycleDays() {
        return cycleDays;
    }

    public void setCycleDays(int cycleDays) {
        this.cycleDays = cycleDays;
    }

    public int getGroupCount() {
        return groupCount;
    }

    public void setGroupCount(int groupCount) {
        this.groupCount = groupCount;
    }

    public List<ShiftDetail> getShiftDetails() {
        return shiftDetails;
    }

    public void setShiftDetails(List<ShiftDetail> shiftDetails) {
        this.shiftDetails = shiftDetails;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public static class ShiftDetail implements Serializable {
        private int dayIndex;
        private String shiftName;
        private String timeRange;

        public ShiftDetail() {}

        public ShiftDetail(int dayIndex, String shiftName, String timeRange) {
            this.dayIndex = dayIndex;
            this.shiftName = shiftName;
            this.timeRange = timeRange;
        }

        public int getDayIndex() {
            return dayIndex;
        }

        public void setDayIndex(int dayIndex) {
            this.dayIndex = dayIndex;
        }

        public String getShiftName() {
            return shiftName;
        }

        public void setShiftName(String shiftName) {
            this.shiftName = shiftName;
        }

        public String getTimeRange() {
            return timeRange;
        }

        public void setTimeRange(String timeRange) {
            this.timeRange = timeRange;
        }
    }
}