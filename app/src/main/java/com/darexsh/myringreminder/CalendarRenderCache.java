package com.darexsh.myringreminder;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CalendarRenderCache {

    public static final class Snapshot {
        public final long startMillis;
        public final int cycleLength;
        public final int pastMonths;
        public final int futureMonths;
        public final Set<CalendarDay> insertionDays;
        public final Set<CalendarDay> removalDays;
        public final Set<CalendarDay> wearDays;
        public final Set<CalendarDay> ringFreeDays;

        Snapshot(long startMillis,
                 int cycleLength,
                 int pastMonths,
                 int futureMonths,
                 Set<CalendarDay> insertionDays,
                 Set<CalendarDay> removalDays,
                 Set<CalendarDay> wearDays,
                 Set<CalendarDay> ringFreeDays) {
            this.startMillis = startMillis;
            this.cycleLength = cycleLength;
            this.pastMonths = pastMonths;
            this.futureMonths = futureMonths;
            this.insertionDays = insertionDays;
            this.removalDays = removalDays;
            this.wearDays = wearDays;
            this.ringFreeDays = ringFreeDays;
        }

        boolean matches(long startMillis, int cycleLength, int pastMonths, int futureMonths) {
            return this.startMillis == startMillis
                    && this.cycleLength == cycleLength
                    && this.pastMonths == pastMonths
                    && this.futureMonths == futureMonths;
        }
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile Snapshot cachedSnapshot;

    private CalendarRenderCache() {
    }

    public static void warmAsync(SettingsRepository repository) {
        if (repository == null) {
            return;
        }
        Calendar startDate = repository.getStartDate();
        int cycleLength = repository.getCycleLength();
        int pastMonths = toMonths(repository.getCalendarPastAmount(), repository.getCalendarPastUnit());
        int futureMonths = toMonths(repository.getCalendarFutureAmount(), repository.getCalendarFutureUnit());
        EXECUTOR.execute(() -> {
            Snapshot snapshot = computeSnapshot(repository, startDate, cycleLength, pastMonths, futureMonths);
            setSnapshot(snapshot);
        });
    }

    public static Snapshot getSnapshotIfMatches(Calendar startDate, int cycleLength, int pastMonths, int futureMonths) {
        Snapshot snapshot = cachedSnapshot;
        if (snapshot == null) {
            return null;
        }
        long normalizedStart = normalizeMillis(startDate);
        return snapshot.matches(normalizedStart, cycleLength, pastMonths, futureMonths) ? snapshot : null;
    }

    public static void setSnapshot(Snapshot snapshot) {
        cachedSnapshot = snapshot;
    }

    public static void clear() {
        cachedSnapshot = null;
    }

    public static Snapshot computeSnapshot(SettingsRepository repository,
                                           Calendar startDate,
                                           int cycleLength,
                                           int pastMonths,
                                           int futureMonths) {
        Set<CalendarDay> insertionDays = new HashSet<>();
        Set<CalendarDay> removalDays = new HashSet<>();
        Set<CalendarDay> wearDays = new HashSet<>();
        Set<CalendarDay> ringFreeDays = new HashSet<>();

        Calendar today = Calendar.getInstance();
        Calendar pastLimit = (Calendar) today.clone();
        pastLimit.add(Calendar.MONTH, -Math.max(pastMonths, 0));
        Calendar futureLimit = (Calendar) today.clone();
        futureLimit.add(Calendar.MONTH, Math.max(futureMonths, 0));

        int baseStepDays = cycleLength + Constants.RING_FREE_DAYS;
        if (baseStepDays <= 0) {
            return new Snapshot(
                    normalizeMillis(startDate),
                    cycleLength,
                    pastMonths,
                    futureMonths,
                    insertionDays,
                    removalDays,
                    wearDays,
                    ringFreeDays
            );
        }

        Calendar currentStart = (Calendar) startDate.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);
        if (currentStart.after(pastLimit)) {
            while (currentStart.after(pastLimit)) {
                currentStart.add(Calendar.DAY_OF_MONTH, -baseStepDays);
            }
        } else {
            while (currentStart.before(pastLimit)) {
                int delayDays = getDelayDaysForStart(repository, currentStart);
                currentStart.add(Calendar.DAY_OF_MONTH, baseStepDays + delayDays);
            }
        }

        int guard = 0;
        while (!currentStart.after(futureLimit) && guard < 2000) {
            int delayDays = getDelayDaysForStart(repository, currentStart);
            int ringFreeDaysForCycle = repository.getRingFreeDaysForCycle(currentStart.getTimeInMillis());
            int stepDays = cycleLength + ringFreeDaysForCycle + delayDays;

            Calendar removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            Calendar newInsertionDate = (Calendar) removalDate.clone();
            newInsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDaysForCycle);

            Calendar wearStart = (Calendar) currentStart.clone();
            wearStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar wearEnd = (Calendar) removalDate.clone();
            wearEnd.add(Calendar.DAY_OF_MONTH, -1);

            Calendar ringFreeStart = (Calendar) removalDate.clone();
            ringFreeStart.add(Calendar.DAY_OF_MONTH, 1);
            Calendar ringFreeEnd = (Calendar) removalDate.clone();
            ringFreeEnd.add(Calendar.DAY_OF_MONTH, Math.max(0, ringFreeDaysForCycle - 1));

            if (isWithinRange(currentStart, pastLimit, futureLimit)) {
                insertionDays.add(toCalendarDay(currentStart));
            }
            if (isWithinRange(removalDate, pastLimit, futureLimit)) {
                removalDays.add(toCalendarDay(removalDate));
            }
            if (isWithinRange(newInsertionDate, pastLimit, futureLimit)) {
                insertionDays.add(toCalendarDay(newInsertionDate));
            }
            if (isOverlappingRange(wearStart, wearEnd, pastLimit, futureLimit)) {
                addDaysToSet(wearDays, wearStart, wearEnd, pastLimit, futureLimit);
            }
            if (ringFreeDaysForCycle > 0 && isOverlappingRange(ringFreeStart, ringFreeEnd, pastLimit, futureLimit)) {
                addDaysToSet(ringFreeDays, ringFreeStart, ringFreeEnd, pastLimit, futureLimit);
            }

            currentStart.add(Calendar.DAY_OF_MONTH, stepDays);
            guard++;
        }

        return new Snapshot(
                normalizeMillis(startDate),
                cycleLength,
                pastMonths,
                futureMonths,
                insertionDays,
                removalDays,
                wearDays,
                ringFreeDays
        );
    }

    private static int toMonths(int amount, String unit) {
        return "years".equals(unit) ? amount * 12 : amount;
    }

    private static int getDelayDaysForStart(SettingsRepository repository, Calendar startDate) {
        Calendar normalized = (Calendar) startDate.clone();
        normalized.set(Calendar.MILLISECOND, 0);
        normalized.set(Calendar.SECOND, 0);
        return repository.getCycleDelayDays(normalized.getTimeInMillis());
    }

    private static void addDaysToSet(Set<CalendarDay> target,
                                     Calendar startInclusive,
                                     Calendar endInclusive,
                                     Calendar windowStart,
                                     Calendar windowEnd) {
        Calendar current = (Calendar) startInclusive.clone();
        if (current.before(windowStart)) {
            current = (Calendar) windowStart.clone();
        }
        while (!current.after(endInclusive) && !current.after(windowEnd)) {
            if (!current.before(windowStart)) {
                target.add(toCalendarDay(current));
            }
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private static boolean isWithinRange(Calendar date, Calendar start, Calendar end) {
        return !date.before(start) && !date.after(end);
    }

    private static boolean isOverlappingRange(Calendar rangeStart, Calendar rangeEnd, Calendar windowStart, Calendar windowEnd) {
        return !rangeEnd.before(windowStart) && !rangeStart.after(windowEnd);
    }

    private static CalendarDay toCalendarDay(Calendar calendar) {
        return CalendarDay.from(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private static long normalizeMillis(Calendar calendar) {
        Calendar normalized = (Calendar) calendar.clone();
        normalized.set(Calendar.SECOND, 0);
        normalized.set(Calendar.MILLISECOND, 0);
        return normalized.getTimeInMillis();
    }
}
