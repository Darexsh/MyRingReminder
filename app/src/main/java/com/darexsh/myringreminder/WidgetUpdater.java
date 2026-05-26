package com.darexsh.myringreminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public final class WidgetUpdater {

    public static final String ACTION_WIDGET_UPDATE = "com.darexsh.myringreminder.ACTION_WIDGET_UPDATE";

    private WidgetUpdater() {
    }

    public static void updateAllWidgets(Context context) {
        Context localized = getLocalizedContext(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(localized);
        updateSmallWidgets(localized, manager);
        updateLargeWidgets(localized, manager);
        scheduleNextUpdate(localized);
    }

    private static void updateSmallWidgets(Context context, AppWidgetManager manager) {
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, CycleWidgetSmallProvider.class));
        for (int appWidgetId : widgetIds) {
            CycleWidgetSmallProvider.updateAppWidget(context, manager, appWidgetId);
        }
    }

    private static void updateLargeWidgets(Context context, AppWidgetManager manager) {
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, CycleWidgetLargeProvider.class));
        for (int appWidgetId : widgetIds) {
            CycleWidgetLargeProvider.updateAppWidget(context, manager, appWidgetId);
        }
    }

    public static void scheduleNextUpdate(Context context) {
        Context localized = getLocalizedContext(context);
        AppWidgetManager manager = AppWidgetManager.getInstance(localized);
        int[] smallIds = manager.getAppWidgetIds(new ComponentName(localized, CycleWidgetSmallProvider.class));
        int[] largeIds = manager.getAppWidgetIds(new ComponentName(localized, CycleWidgetLargeProvider.class));
        if ((smallIds == null || smallIds.length == 0) && (largeIds == null || largeIds.length == 0)) {
            cancelScheduledUpdate(localized);
            return;
        }

        long nextAt = CycleWidgetUtils.calculateNextWidgetUpdateMillis(localized);
        if (nextAt <= 0L) {
            return;
        }
        AlarmManager alarmManager = (AlarmManager) localized.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildUpdateIntent(localized, PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent);
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt, pendingIntent);
        }
    }

    public static void cancelScheduledUpdate(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        PendingIntent pendingIntent = buildUpdateIntent(context, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private static PendingIntent buildUpdateIntent(Context context, int flags) {
        Intent intent = new Intent(context, WidgetUpdateReceiver.class);
        intent.setAction(ACTION_WIDGET_UPDATE);
        int resolvedFlags = flags | PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(context, 0, intent, resolvedFlags);
    }

    private static Context getLocalizedContext(Context context) {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return context;
        }
        Locale locale = locales.get(0);
        if (locale == null) {
            return context;
        }
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocales(new LocaleList(locale));
        return context.createConfigurationContext(config);
    }
}
