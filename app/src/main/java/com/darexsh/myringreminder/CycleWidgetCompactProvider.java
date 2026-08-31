package com.darexsh.myringreminder;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

public class CycleWidgetCompactProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
        WidgetUpdater.scheduleNextUpdate(context);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
                                          int appWidgetId, android.os.Bundle newOptions) {
        updateAppWidget(context, appWidgetManager, appWidgetId);
        WidgetUpdater.scheduleNextUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        WidgetUpdater.scheduleNextUpdate(context);
    }

    @Override
    public void onDisabled(Context context) {
        WidgetUpdater.scheduleNextUpdate(context);
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        boolean useTallLayout = options != null
                && options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) >= 88;
        int layoutRes = useTallLayout ? R.layout.widget_ring_compact_tall : R.layout.widget_ring_compact;
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutRes);
        CycleWidgetUtils.State state = CycleWidgetUtils.calculateState(context);
        SettingsRepository repository = new SettingsRepository(context);
        views.setInt(R.id.widget_bg_image, "setColorFilter", repository.getButtonColor());

        views.setTextViewText(R.id.tv_widget_compact_app_name, context.getString(R.string.app_name));
        views.setTextViewText(R.id.tv_widget_compact_status_label, state.label);
        views.setTextViewText(R.id.tv_widget_compact_days_number, String.valueOf(state.daysLeft));
        views.setTextViewText(R.id.tv_widget_compact_removal, state.removalText);
        views.setTextViewText(R.id.tv_widget_compact_insertion, state.insertionText);
        views.setTextViewText(R.id.tv_widget_compact_stock, state.stockText);
        views.setViewVisibility(
                R.id.tv_widget_compact_stock,
                state.stockTrackingEnabled ? android.view.View.VISIBLE : android.view.View.GONE
        );

        PendingIntent launchIntent = buildLaunchIntent(context, appWidgetId);
        views.setOnClickPendingIntent(R.id.widget_root, launchIntent);
        views.setOnClickPendingIntent(R.id.widget_bg_image, launchIntent);
        views.setOnClickPendingIntent(R.id.img_widget_compact_logo, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_app_name, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_status_label, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_days_number, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_removal, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_insertion, launchIntent);
        views.setOnClickPendingIntent(R.id.tv_widget_compact_stock, launchIntent);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private static PendingIntent buildLaunchIntent(Context context, int requestCode) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_home", true);
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
