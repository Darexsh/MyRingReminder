package com.darexsh.myringreminder;

public class PeriodDayEntry {

    private String dateKey;
    private boolean periodDay;
    private BleedingIntensity intensity;
    private boolean pain;
    private boolean illness;
    private boolean start;
    private boolean end;
    private long updatedAt;

    public PeriodDayEntry() {
    }

    public PeriodDayEntry(String dateKey,
                          boolean periodDay,
                          BleedingIntensity intensity,
                          boolean pain,
                          boolean illness,
                          boolean start,
                          boolean end,
                          long updatedAt) {
        this.dateKey = dateKey;
        this.periodDay = periodDay;
        this.intensity = intensity;
        this.pain = pain;
        this.illness = illness;
        this.start = start;
        this.end = end;
        this.updatedAt = updatedAt;
    }

    public String getDateKey() {
        return dateKey;
    }

    public void setDateKey(String dateKey) {
        this.dateKey = dateKey;
    }

    public boolean isPeriodDay() {
        return periodDay;
    }

    public void setPeriodDay(boolean periodDay) {
        this.periodDay = periodDay;
    }

    public BleedingIntensity getIntensity() {
        return intensity;
    }

    public void setIntensity(BleedingIntensity intensity) {
        this.intensity = intensity;
    }

    public boolean hasPain() {
        return pain;
    }

    public void setPain(boolean pain) {
        this.pain = pain;
    }

    public boolean hasIllness() {
        return illness;
    }

    public void setIllness(boolean illness) {
        this.illness = illness;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
