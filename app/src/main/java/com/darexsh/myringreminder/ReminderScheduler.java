package com.darexsh.myringreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class ReminderScheduler {

    private static final int NOTIFY_TWO_WEEKS = 0;
    private static final int NOTIFY_ONE_WEEK = 1;
    private static final int NOTIFY_REMOVAL_REMINDER = 2;
    private static final int NOTIFY_REMOVAL_EXACT = 3;
    private static final int NOTIFY_INSERTION_REMINDER = 4;
    private static final int NOTIFY_INSERTION_EXACT = 5;

    private ReminderScheduler() {
    }

    public static void scheduleCurrentCycle(Context context) {
        SettingsRepository repository = new SettingsRepository(context);

        Calendar startDate = repository.getStartDate();
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        int cycleLength = repository.getCycleLength();

        int delayDays = repository.getCycleDelayDays(startDate.getTimeInMillis());
        int ringFreeDays = repository.getRingFreeDaysForCycle(startDate.getTimeInMillis());
        Calendar removalDate = (Calendar) startDate.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        Calendar now = Calendar.getInstance();
        Calendar nowDay = startOfDay(now);
        Calendar reinsertionDay = startOfDay(reinsertionDate);

        while (nowDay.after(reinsertionDay)) {
            startDate.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = repository.getCycleDelayDays(startDate.getTimeInMillis());
            ringFreeDays = repository.getRingFreeDaysForCycle(startDate.getTimeInMillis());
            removalDate = (Calendar) startDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            reinsertionDay = startOfDay(reinsertionDate);
        }

        if (!now.before(reinsertionDate)) {
            return;
        }

        long cycleStartMillis = startDate.getTimeInMillis();
        cancelNotificationsForCycle(context, cycleStartMillis);

        int hour = startDate.get(Calendar.HOUR_OF_DAY);
        int minute = startDate.get(Calendar.MINUTE);

        Calendar twoWeeksRemaining = (Calendar) removalDate.clone();
        twoWeeksRemaining.add(Calendar.DAY_OF_MONTH, -14);
        twoWeeksRemaining.set(Calendar.HOUR_OF_DAY, hour);
        twoWeeksRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 14) {
            scheduleNotification(
                    context,
                    twoWeeksRemaining,
                    context.getString(R.string.notif_cycle_duration_title),
                    context.getString(R.string.notif_two_weeks_remaining),
                    buildRequestCode(cycleStartMillis, NOTIFY_TWO_WEEKS)
            );
        }

        Calendar oneWeekRemaining = (Calendar) removalDate.clone();
        oneWeekRemaining.add(Calendar.DAY_OF_MONTH, -7);
        oneWeekRemaining.set(Calendar.HOUR_OF_DAY, hour);
        oneWeekRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 7) {
            scheduleNotification(
                    context,
                    oneWeekRemaining,
                    context.getString(R.string.notif_cycle_duration_title),
                    context.getString(R.string.notif_one_week_remaining),
                    buildRequestCode(cycleStartMillis, NOTIFY_ONE_WEEK)
            );
        }

        int removalReminderHours = repository.getRemovalReminderHours();
        if (removalReminderHours > 0) {
            Calendar removalReminder = (Calendar) removalDate.clone();
            removalReminder.add(Calendar.HOUR_OF_DAY, -removalReminderHours);
            scheduleNotification(
                    context,
                    removalReminder,
                    context.getString(R.string.notif_remove_title),
                    context.getString(R.string.notif_remove_in_hours, removalReminderHours),
                    buildRequestCode(cycleStartMillis, NOTIFY_REMOVAL_REMINDER)
            );
        }

        Calendar removalExact = (Calendar) removalDate.clone();
        removalExact.set(Calendar.HOUR_OF_DAY, hour);
        removalExact.set(Calendar.MINUTE, minute);
        String removalTimeText = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
        scheduleNotification(
                context,
                removalExact,
                context.getString(R.string.notif_remove_title),
                context.getString(R.string.notif_remove_now, removalTimeText),
                buildRequestCode(cycleStartMillis, NOTIFY_REMOVAL_EXACT)
        );

        int insertionReminderHours = repository.getInsertionReminderHours();
        if (insertionReminderHours > 0) {
            Calendar insertionReminder = (Calendar) reinsertionDate.clone();
            insertionReminder.add(Calendar.HOUR_OF_DAY, -insertionReminderHours);
            scheduleNotification(
                    context,
                    insertionReminder,
                    context.getString(R.string.notif_insert_title),
                    context.getString(R.string.notif_insert_in_hours, insertionReminderHours),
                    buildRequestCode(cycleStartMillis, NOTIFY_INSERTION_REMINDER)
            );
        }

        Calendar insertionExact = (Calendar) reinsertionDate.clone();
        insertionExact.set(Calendar.HOUR_OF_DAY, hour);
        insertionExact.set(Calendar.MINUTE, minute);
        String insertionTimeText = String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
        scheduleNotification(
                context,
                insertionExact,
                context.getString(R.string.notif_insert_title),
                context.getString(R.string.notif_insert_now, insertionTimeText),
                buildRequestCode(cycleStartMillis, NOTIFY_INSERTION_EXACT)
        );

        int settingsHash = repository.getNotificationSettingsHash();
        repository.setNotificationScheduledForCycle(cycleStartMillis);
        repository.setNotificationSettingsHashForCycle(cycleStartMillis, settingsHash);
    }

    public static boolean hasAnyScheduledForCycle(Context context, long cycleStartMillis) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        int[] types = new int[]{
                NOTIFY_TWO_WEEKS,
                NOTIFY_ONE_WEEK,
                NOTIFY_REMOVAL_REMINDER,
                NOTIFY_REMOVAL_EXACT,
                NOTIFY_INSERTION_REMINDER,
                NOTIFY_INSERTION_EXACT
        };
        for (int type : types) {
            int requestCode = buildRequestCode(cycleStartMillis, type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleNotification(Context context, Calendar calendar, String title, String message, int requestCode) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            return;
        }
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private static void cancelNotificationsForCycle(Context context, long cycleStartMillis) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        int[] types = new int[]{
                NOTIFY_TWO_WEEKS,
                NOTIFY_ONE_WEEK,
                NOTIFY_REMOVAL_REMINDER,
                NOTIFY_REMOVAL_EXACT,
                NOTIFY_INSERTION_REMINDER,
                NOTIFY_INSERTION_EXACT
        };
        for (int type : types) {
            int requestCode = buildRequestCode(cycleStartMillis, type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private static int buildRequestCode(long cycleStartMillis, int typeId) {
        long hash = cycleStartMillis ^ (cycleStartMillis >>> 32);
        int base = (int) (hash & 0x7fffffff);
        int code = base + (typeId + 1) * 1000;
        return code < 0 ? base : code;
    }

    private static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    private static Calendar startOfDay(Calendar source) {
        Calendar day = (Calendar) source.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }
}
