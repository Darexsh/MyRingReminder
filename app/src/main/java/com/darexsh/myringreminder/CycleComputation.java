package com.darexsh.myringreminder;

import java.util.Calendar;

final class CycleComputation {

    interface CycleConfig {
        int getDelayDays(long cycleStartMillis);
        int getRingFreeDays(long cycleStartMillis);
    }

    static final class CycleWindow {
        final Calendar currentStart;
        final Calendar removalDate;
        final Calendar reinsertionDate;
        final int cycleLength;
        final int delayDays;
        final int ringFreeDays;
        final Calendar previousStart;

        CycleWindow(Calendar currentStart,
                    Calendar removalDate,
                    Calendar reinsertionDate,
                    int cycleLength,
                    int delayDays,
                    int ringFreeDays,
                    Calendar previousStart) {
            this.currentStart = currentStart;
            this.removalDate = removalDate;
            this.reinsertionDate = reinsertionDate;
            this.cycleLength = cycleLength;
            this.delayDays = delayDays;
            this.ringFreeDays = ringFreeDays;
            this.previousStart = previousStart;
        }
    }

    private CycleComputation() {
    }

    static CycleWindow calculateCurrentCycle(Calendar baseStart,
                                             int cycleLength,
                                             Calendar now,
                                             CycleConfig cycleConfig) {
        Calendar currentStart = (Calendar) baseStart.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);

        int delayDays = cycleConfig.getDelayDays(currentStart.getTimeInMillis());
        int ringFreeDays = cycleConfig.getRingFreeDays(currentStart.getTimeInMillis());
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        Calendar previousStart = null;
        int guard = 0;
        while (!now.before(reinsertionDate) && guard < 200) {
            previousStart = (Calendar) currentStart.clone();
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = cycleConfig.getDelayDays(currentStart.getTimeInMillis());
            ringFreeDays = cycleConfig.getRingFreeDays(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            guard++;
        }

        return new CycleWindow(
                currentStart,
                removalDate,
                reinsertionDate,
                cycleLength,
                delayDays,
                ringFreeDays,
                previousStart
        );
    }
}
