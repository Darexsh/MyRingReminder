package com.darexsh.myringreminder;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import java.util.Calendar;

final class StockManager {

    private static final int STOCK_NOTIFICATION_ID = 830421;
    private StockManager() {
    }

    static void syncCurrentCycle(Context context) {
        syncCurrentCycle(context, new SettingsRepository(context));
    }

    static void syncCurrentCycle(Context context, SettingsRepository repository) {
        if (!repository.isStockTrackingEnabled()) {
            cancelRecipeCheckNotification(context, repository.getLastStockDecrementCycleStartMillis());
            return;
        }

        Calendar now = DebugTimeProvider.now(repository);
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

        long currentCycleStartMillis = cycleWindow.currentStart.getTimeInMillis();
        long lastCountedCycleStartMillis = repository.getLastStockDecrementCycleStartMillis();
        if (lastCountedCycleStartMillis <= 0L) {
            repository.setLastStockDecrementCycleStartMillis(currentCycleStartMillis);
            cancelRecipeCheckNotification(context, currentCycleStartMillis);
            return;
        }

        if (currentCycleStartMillis > lastCountedCycleStartMillis) {
            int pendingInsertions = countPendingInsertions(
                    repository.getStartDate(),
                    repository.getCycleLength(),
                    lastCountedCycleStartMillis,
                    currentCycleStartMillis,
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
            if (pendingInsertions > 0) {
                int updatedCount = Math.max(0, repository.getRingStockCount() - pendingInsertions);
                repository.setRingStockCount(updatedCount);
                maybeNotifyStockUpdate(context, repository, updatedCount);
                updateRecipeCheckReminder(context, repository, currentCycleStartMillis, updatedCount);
            } else {
                cancelRecipeCheckNotification(context, currentCycleStartMillis);
            }
            repository.setLastStockDecrementCycleStartMillis(currentCycleStartMillis);
            return;
        }
        updateRecipeCheckReminder(
                context,
                repository,
                repository.getLastStockDecrementCycleStartMillis(),
                repository.getRingStockCount()
        );
    }

    static void adoptCurrentCycleAsCounted(SettingsRepository repository) {
        if (!repository.isStockTrackingEnabled()) {
            repository.setLastStockDecrementCycleStartMillis(0L);
            return;
        }
        Calendar currentStart = getCurrentCycleStart(
                repository,
                repository.getStartDate(),
                repository.getCycleLength()
        );
        repository.setLastStockDecrementCycleStartMillis(currentStart.getTimeInMillis());
    }

    static void adoptCurrentCycleAsCounted(SettingsRepository repository, Calendar startDate, int cycleLength) {
        if (!repository.isStockTrackingEnabled()) {
            repository.setLastStockDecrementCycleStartMillis(0L);
            return;
        }
        Calendar currentStart = getCurrentCycleStart(repository, startDate, cycleLength);
        repository.setLastStockDecrementCycleStartMillis(currentStart.getTimeInMillis());
    }

    static void refreshReminderState(Context context, SettingsRepository repository) {
        if (!repository.isStockTrackingEnabled() || !repository.isLowStockReminderEnabled()) {
            cancelRecipeCheckNotification(context, repository.getLastStockDecrementCycleStartMillis());
            return;
        }
        updateRecipeCheckReminder(
                context,
                repository,
                repository.getLastStockDecrementCycleStartMillis(),
                repository.getRingStockCount()
        );
    }

    static int countPendingInsertions(Calendar baseStart,
                                      int cycleLength,
                                      long lastCountedCycleStartMillis,
                                      long currentCycleStartMillis,
                                      CycleComputation.CycleConfig cycleConfig) {
        if (currentCycleStartMillis <= lastCountedCycleStartMillis) {
            return 0;
        }

        Calendar start = (Calendar) baseStart.clone();
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        int count = 0;
        int guard = 0;
        while (start.getTimeInMillis() <= currentCycleStartMillis && guard < 400) {
            long startMillis = start.getTimeInMillis();
            if (startMillis > lastCountedCycleStartMillis && startMillis <= currentCycleStartMillis) {
                count++;
            }
            int stepDays = cycleLength
                    + cycleConfig.getDelayDays(startMillis)
                    + cycleConfig.getRingFreeDays(startMillis);
            start.add(Calendar.DAY_OF_MONTH, stepDays);
            guard++;
        }
        return count;
    }

    private static Calendar getCurrentCycleStart(SettingsRepository repository, Calendar startDate, int cycleLength) {
        CycleComputation.CycleWindow cycleWindow = CycleComputation.calculateCurrentCycle(
                startDate,
                cycleLength,
                DebugTimeProvider.now(repository),
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
        return cycleWindow.currentStart;
    }

    private static void maybeNotifyStockUpdate(Context context, SettingsRepository repository, int currentCount) {
        if (!repository.isStockTrackingEnabled() || !repository.isLowStockReminderEnabled()) {
            return;
        }
        showNotification(
                context,
                context.getString(R.string.stock_notification_title),
                context.getResources().getQuantityString(
                        R.plurals.stock_notification_message,
                        currentCount,
                        currentCount
                )
        );
    }

    private static void updateRecipeCheckReminder(Context context,
                                                  SettingsRepository repository,
                                                  long cycleStartMillis,
                                                  int currentCount) {
        cancelRecipeCheckNotification(context, cycleStartMillis);
        if (!repository.isStockTrackingEnabled()
                || !repository.isLowStockReminderEnabled()
                || currentCount != 1
                || cycleStartMillis <= 0L) {
            return;
        }

        long triggerAtMillis = cycleStartMillis
                + (long) repository.getStockRecipeCheckDelayDays() * 24L * 60L * 60L * 1000L;
        if (triggerAtMillis <= DebugTimeProvider.now(repository).getTimeInMillis()) {
            return;
        }

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", context.getString(R.string.stock_recipe_check_title));
        intent.putExtra("message", context.getString(R.string.stock_recipe_check_message));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_STOCK_RECIPE_CHECK),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }

    private static void cancelRecipeCheckNotification(Context context, long cycleStartMillis) {
        if (cycleStartMillis <= 0L) {
            return;
        }
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ReminderRequestCodes.buildRequestCode(cycleStartMillis, ReminderRequestCodes.NOTIFY_STOCK_RECIPE_CHECK),
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent == null) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }

    private static void showNotification(Context context, String title, String message) {
        ensureReminderChannel(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                STOCK_NOTIFICATION_ID,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, Constants.REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_SOUND | NotificationCompat.DEFAULT_VIBRATE);

        NotificationManagerCompat.from(context).notify(STOCK_NOTIFICATION_ID, builder.build());
    }

    private static boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        return alarmManager.canScheduleExactAlarms();
    }

    private static void ensureReminderChannel(Context context) {
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel existing = notificationManager.getNotificationChannel(Constants.REMINDER_CHANNEL_ID);
        if (existing != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                Constants.REMINDER_CHANNEL_ID,
                context.getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notifications_channel_description));
        notificationManager.createNotificationChannel(channel);
    }
}
