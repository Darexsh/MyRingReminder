package com.darexsh.myringreminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class CycleComputationTest {

    @Test
    public void calculateCurrentCycle_keepsPreviousCycleBeforeReinsertionTime() {
        Calendar baseStart = utcCalendar(2026, Calendar.JULY, 21, 18, 0);
        Calendar now = utcCalendar(2026, Calendar.AUGUST, 18, 17, 59);

        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                baseStart,
                21,
                now,
                fixedConfig(0, 7)
        );

        assertEquals(baseStart.getTimeInMillis(), cycleWindow.currentStart.getTimeInMillis());
        assertNull(cycleWindow.previousStart);
    }

    @Test
    public void calculateCurrentCycle_advancesExactlyAtReinsertionTime() {
        Calendar baseStart = utcCalendar(2026, Calendar.JULY, 21, 18, 0);
        Calendar now = utcCalendar(2026, Calendar.AUGUST, 18, 18, 0);

        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                baseStart,
                21,
                now,
                fixedConfig(0, 7)
        );

        assertEquals(utcCalendar(2026, Calendar.AUGUST, 18, 18, 0).getTimeInMillis(),
                cycleWindow.currentStart.getTimeInMillis());
        assertEquals(baseStart.getTimeInMillis(), cycleWindow.previousStart.getTimeInMillis());
    }

    @Test
    public void calculateCurrentCycle_advancesAfterReinsertionTimeOnSameDay() {
        Calendar baseStart = utcCalendar(2026, Calendar.JULY, 21, 18, 0);
        Calendar now = utcCalendar(2026, Calendar.AUGUST, 18, 20, 0);

        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                baseStart,
                21,
                now,
                fixedConfig(0, 7)
        );

        assertEquals(utcCalendar(2026, Calendar.AUGUST, 18, 18, 0).getTimeInMillis(),
                cycleWindow.currentStart.getTimeInMillis());
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
