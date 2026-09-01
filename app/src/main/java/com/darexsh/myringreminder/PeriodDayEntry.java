package com.darexsh.myringreminder;

public class PeriodDayEntry {

    private final String dateKey;
    private final boolean periodDay;
    private final BleedingIntensity intensity;
    // Legacy flags kept for backward-compatibility with older stored entries.
    private final boolean pain;
    private final boolean illness;
    private final PainSeverity painSeverity;
    private final boolean symptomIllness;
    private final boolean symptomNausea;
    private final boolean symptomFatigue;
    private final boolean symptomDizziness;
    private final boolean symptomDiarrhea;
    private final boolean start;
    private final boolean end;
    private final long updatedAt;

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

    public boolean isPeriodDay() {
        return periodDay;
    }

    public BleedingIntensity getIntensity() {
        return intensity;
    }

    public boolean hasPain() {
        return painSeverity != null ? painSeverity != PainSeverity.NONE : pain;
    }

    public boolean hasIllness() {
        return symptomIllness || illness;
    }

    public PainSeverity getPainSeverity() {
        return painSeverity;
    }

    public boolean isSymptomIllness() {
        return symptomIllness;
    }

    public boolean isSymptomNausea() {
        return symptomNausea;
    }

    public boolean isSymptomFatigue() {
        return symptomFatigue;
    }

    public boolean isSymptomDizziness() {
        return symptomDizziness;
    }

    public boolean isSymptomDiarrhea() {
        return symptomDiarrhea;
    }

    public boolean hasAnyAdditionalSymptoms() {
        return symptomIllness || symptomNausea || symptomFatigue || symptomDizziness || symptomDiarrhea;
    }

    public boolean isStart() {
        return start;
    }

    public boolean isEnd() {
        return end;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }
}
