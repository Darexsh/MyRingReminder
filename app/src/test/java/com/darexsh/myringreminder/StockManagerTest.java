package com.darexsh.myringreminder;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class StockManagerTest {

    @Test
    public void countPendingInsertions_countsNormalCycleStartsOnceEach() {
        Calendar baseStart = utcCalendar(2026, Calendar.JANUARY, 1, 18, 0);
        long lastCounted = baseStart.getTimeInMillis();
        long currentCycleStart = utcCalendar(2026, Calendar.JANUARY, 29, 18, 0).getTimeInMillis();

        int count = StockManager.countPendingInsertions(
                baseStart,
                21,
                lastCounted,
                currentCycleStart,
                fixedConfig(0, 7)
        );

        assertEquals(1, count);
    }

    @Test
    public void countPendingInsertions_countsImmediateReinsertionForSkippedRingFreeWeek() {
        Calendar baseStart = utcCalendar(2026, Calendar.JANUARY, 1, 18, 0);
        long lastCounted = baseStart.getTimeInMillis();
        long currentCycleStart = utcCalendar(2026, Calendar.JANUARY, 22, 18, 0).getTimeInMillis();

        int count = StockManager.countPendingInsertions(
                baseStart,
                21,
                lastCounted,
                currentCycleStart,
                new CycleComputation.CycleConfig() {
                    @Override
                    public int getDelayDays(long cycleStartMillis) {
                        return 0;
                    }

                    @Override
                    public int getRingFreeDays(long cycleStartMillis) {
                        if (cycleStartMillis == baseStart.getTimeInMillis()) {
                            return 0;
                        }
                        return 7;
                    }
                }
        );

        assertEquals(1, count);
    }

    @Test
    public void countPendingInsertions_doesNotCountWearRingLongerAsExtraInsertion() {
        Calendar baseStart = utcCalendar(2026, Calendar.JANUARY, 1, 18, 0);
        long lastCounted = baseStart.getTimeInMillis();
        long currentCycleStart = utcCalendar(2026, Calendar.JANUARY, 31, 18, 0).getTimeInMillis();

        int count = StockManager.countPendingInsertions(
                baseStart,
                21,
                lastCounted,
                currentCycleStart,
                fixedConfig(3, 7)
        );

        assertEquals(1, count);
    }

    private static CycleComputation.CycleConfig fixedConfig(int delayDays, int ringFreeDays) {
        return new CycleComputation.CycleConfig() {
            @Override
            public int getDelayDays(long cycleStartMillis) {
                return delayDays;
            }

            @Override
            public int getRingFreeDays(long cycleStartMillis) {
                return ringFreeDays;
            }
        };
    }

    private static Calendar utcCalendar(int year, int month, int dayOfMonth, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(year, month, dayOfMonth, hourOfDay, minute, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
}
