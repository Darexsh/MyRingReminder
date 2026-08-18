package com.darexsh.myringreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;

public final class ReminderScheduler {

    private ReminderScheduler() {
    }

    public static void scheduleCurrentCycle(Context context) {
        SettingsRepository repository = new SettingsRepository(context);
        Calendar now = Calendar.getInstance();
        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                repository.getStartDate(),
                repository.getCycleLength(),
                now,
                new CycleComputation.CycleConfig() {
                    @Override
                    public int getDelayDays(long cycleStartMillis) {
                        return repository.getCycleDelayDays(cycleStartMillis);
                    }

                    @Override
                    public int getRingFreeDays(long cycleStartMillis) {
                        return repository.getRingFreeDaysForCycle(cycleStartMillis);
                    }
                }
        );

        Calendar startDate = cycleWindow.currentStart;
        Calendar removalDate = cycleWindow.removalDate;
        Calendar reinsertionDate = cycleWindow.reinsertionDate;
        int cycleLength = cycleWindow.cycleLength;

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
                    ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_TWO_WEEKS)
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
                    ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_ONE_WEEK)
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
                    ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_REMOVAL_REMINDER)
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
                ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_REMOVAL_EXACT)
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
                    ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_INSERTION_REMINDER)
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
                ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_INSERTION_EXACT)
        );

        int settingsHash = repository.getNotificationSettingsHash();
        repository.setNotificationScheduledForCycle(cycleStartMillis);
        repository.setNotificationSettingsHashForCycle(cycleStartMillis, settingsHash);
    }

    public static boolean hasAnyScheduledForCycle(Context context, long cycleStartMillis) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        for (int type : ReminderRequestCodes.ALL_TYPES) {
            int requestCode = ReminderRequestCodes.buildRequestCode(cycleStartMillis, type);
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

    public static void cancelAllScheduledNotifications(Context context) {
        SettingsRepository repository = new SettingsRepository(context);

        for (Long cycleStartMillis : repository.getNotificationScheduledCycleStarts()) {
            cancelNotificationsForCycle(context, cycleStartMillis);
        }

        Calendar now = Calendar.getInstance();
        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                repository.getStartDate(),
                repository.getCycleLength(),
                now,
                new CycleComputation.CycleConfig() {
                    @Override
                    public int getDelayDays(long cycleStartMillis) {
                        return repository.getCycleDelayDays(cycleStartMillis);
                    }

                    @Override
                    public int getRingFreeDays(long cycleStartMillis) {
                        return repository.getRingFreeDaysForCycle(cycleStartMillis);
                    }
                }
        );
        cancelNotificationsForCycle(context, cycleWindow.currentStart.getTimeInMillis());
        if (cycleWindow.previousStart != null) {
            cancelNotificationsForCycle(context, cycleWindow.previousStart.getTimeInMillis());
        }
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
        for (int type : ReminderRequestCodes.ALL_TYPES) {
            int requestCode = ReminderRequestCodes.buildRequestCode(cycleStartMillis, type);
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

    private static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }
}
