package com.darexsh.myringreminder;

public class PeriodDayEntry {

    private String dateKey;
    private boolean periodDay;
    private BleedingIntensity intensity;
    // Legacy flags kept for backward-compatibility with older stored entries.
    private boolean pain;
    private boolean illness;
    private PainSeverity painSeverity;
    private boolean symptomIllness;
    private boolean symptomNausea;
    private boolean symptomFatigue;
    private boolean symptomDizziness;
    private boolean symptomDiarrhea;
    private boolean start;
    private boolean end;
    private long updatedAt;

    public PeriodDayEntry() {
    }

    public PeriodDayEntry(String dateKey,
                          boolean periodDay,
                          BleedingIntensity intensity,
                          PainSeverity painSeverity,
                          boolean symptomIllness,
                          boolean symptomNausea,
                          boolean symptomFatigue,
                          boolean symptomDizziness,
                          boolean symptomDiarrhea,
                          boolean start,
                          boolean end,
                          long updatedAt) {
        this.dateKey = dateKey;
        this.periodDay = periodDay;
        this.intensity = intensity;
        this.painSeverity = painSeverity;
        this.symptomIllness = symptomIllness;
        this.symptomNausea = symptomNausea;
        this.symptomFatigue = symptomFatigue;
        this.symptomDizziness = symptomDizziness;
        this.symptomDiarrhea = symptomDiarrhea;
        this.pain = painSeverity != null && painSeverity != PainSeverity.NONE;
        this.illness = symptomIllness;
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
        return painSeverity != null ? painSeverity != PainSeverity.NONE : pain;
    }

    public void setPain(boolean pain) {
        this.pain = pain;
        this.painSeverity = pain ? PainSeverity.MEDIUM : PainSeverity.NONE;
    }

    public boolean hasIllness() {
        return symptomIllness || illness;
    }

    public void setIllness(boolean illness) {
        this.illness = illness;
        this.symptomIllness = illness;
    }

    public PainSeverity getPainSeverity() {
        return painSeverity;
    }

    public void setPainSeverity(PainSeverity painSeverity) {
        this.painSeverity = painSeverity;
        this.pain = painSeverity != null && painSeverity != PainSeverity.NONE;
    }

    public boolean isSymptomIllness() {
        return symptomIllness;
    }

    public void setSymptomIllness(boolean symptomIllness) {
        this.symptomIllness = symptomIllness;
        this.illness = symptomIllness;
    }

    public boolean isSymptomNausea() {
        return symptomNausea;
    }

    public void setSymptomNausea(boolean symptomNausea) {
        this.symptomNausea = symptomNausea;
    }

    public boolean isSymptomFatigue() {
        return symptomFatigue;
    }

    public void setSymptomFatigue(boolean symptomFatigue) {
        this.symptomFatigue = symptomFatigue;
    }

    public boolean isSymptomDizziness() {
        return symptomDizziness;
    }

    public void setSymptomDizziness(boolean symptomDizziness) {
        this.symptomDizziness = symptomDizziness;
    }

    public boolean isSymptomDiarrhea() {
        return symptomDiarrhea;
    }

    public void setSymptomDiarrhea(boolean symptomDiarrhea) {
        this.symptomDiarrhea = symptomDiarrhea;
    }

    public boolean hasAnyAdditionalSymptoms() {
        return symptomIllness || symptomNausea || symptomFatigue || symptomDizziness || symptomDiarrhea;
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
