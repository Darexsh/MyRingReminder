package com.darexsh.myringreminder;

// Represents a cycle event with a start date, end date, and type
public class Cycle {
    private final long dateMillis;
    private final long endDateMillis; // Only used for insertion events
    private final CycleType type;

    // Constructor to initialize the cycle with date, end date, and type
    public Cycle(long dateMillis, long endDateMillis, CycleType type) {
        this.dateMillis = dateMillis;
        this.endDateMillis = endDateMillis;
        this.type = type;
    }

    // Getters for the cycle properties
    public long getDateMillis() {
        return dateMillis;
    }

    // Only used for insertion events
    public long getEndDateMillis() {
        return endDateMillis;
    }

    // Get the type of the cycle
    public CycleType getType() {
        return type;
    }
}