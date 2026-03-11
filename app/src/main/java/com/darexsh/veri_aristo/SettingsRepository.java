package com.darexsh.veri_aristo;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsRepository {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_START_DAY = "start_day";
    private static final String KEY_START_MONTH = "start_month";
    private static final String KEY_START_YEAR = "start_year";
    private static final String KEY_SET_TIME_HOUR = "set_time_hour";
    private static final String KEY_SET_TIME_MINUTE = "set_time_minute";
    private static final String KEY_CYCLE_LENGTH = "cycle_length";
    private static final String KEY_BACKGROUND_IMAGE_URI = "background_image_uri";
    private static final String KEY_CYCLE_HISTORY = "cycle_history";
    private static final String KEY_CALENDAR_PAST_MONTHS = "calendar_past_months";
    private static final String KEY_CALENDAR_FUTURE_YEARS = "calendar_future_years";
    private static final String KEY_CALENDAR_PAST_AMOUNT = "calendar_past_amount";
    private static final String KEY_CALENDAR_PAST_UNIT = "calendar_past_unit";
    private static final String KEY_CALENDAR_FUTURE_AMOUNT = "calendar_future_amount";
    private static final String KEY_CALENDAR_FUTURE_UNIT = "calendar_future_unit";
    private static final String KEY_REMOVAL_REMINDER_HOURS = "removal_reminder_hours";
    private static final String KEY_INSERTION_REMINDER_HOURS = "insertion_reminder_hours";
    private static final String KEY_BUTTON_COLOR = "button_color";
    private static final String KEY_HOME_CIRCLE_COLOR = "home_circle_color";
    private static final String KEY_HOME_CIRCLE_STYLE = "home_circle_style";
    private static final String KEY_HOME_CIRCLE_STYLE_VERSION = "home_circle_style_version";
    private static final String KEY_CALENDAR_WEAR_COLOR = "calendar_wear_color";
    private static final String KEY_CALENDAR_RING_FREE_COLOR = "calendar_ring_free_color";
    private static final String KEY_CALENDAR_REMOVAL_COLOR = "calendar_removal_color";
    private static final String KEY_CALENDAR_INSERTION_COLOR = "calendar_insertion_color";
    private static final String KEY_NAVIGATION_ANIMATION_STYLE = "navigation_animation_style";
    private static final String KEY_DEBUG_TOOLS_ENABLED = "debug_tools_enabled";
    private static final String KEY_DEBUG_TIME_ENABLED = "debug_time_enabled";
    private static final String KEY_DEBUG_TIME_MILLIS = "debug_time_millis";
    private static final String KEY_EXACT_ALARM_PROMPTED = "exact_alarm_prompted";
    private static final String KEY_APP_LOCK_ENABLED = "app_lock_enabled";
    private static final String KEY_APP_LOCK_TIMEOUT_MS = "app_lock_timeout_ms";
    public static final int DEFAULT_BUTTON_COLOR = 0xFF6200EE;
    public static final int DEFAULT_HOME_CIRCLE_COLOR = 0xFFBB86FC;
    public static final int DEFAULT_HOME_CIRCLE_STYLE = 0;
    public static final int DEFAULT_CALENDAR_WEAR_COLOR = 0xFF00FF00;
    public static final int DEFAULT_CALENDAR_RING_FREE_COLOR = 0xFFFF0000;
    public static final int DEFAULT_CALENDAR_REMOVAL_COLOR = 0xFFFFFF00;
    public static final int DEFAULT_CALENDAR_INSERTION_COLOR = 0xFF00FFFF;
    public static final int NAV_ANIM_SLIDE = 0;
    public static final int NAV_ANIM_FADE = 1;
    public static final int NAV_ANIM_ZOOM = 2;
    public static final int NAV_ANIM_NONE = 3;
    public static final int NAV_ANIM_SLIDE_UP = 4;
    public static final int NAV_ANIM_ROTATE = 5;
    public static final int NAV_ANIM_POP = 6;
    public static final int DEFAULT_NAVIGATION_ANIMATION_STYLE = NAV_ANIM_SLIDE;

    private final SharedPreferences sharedPreferences;

    public SettingsRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public Calendar getStartDate() {
        Calendar calendar = Calendar.getInstance();
        int day = sharedPreferences.getInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH));
        int month = sharedPreferences.getInt(KEY_START_MONTH, calendar.get(Calendar.MONTH));
        int year = sharedPreferences.getInt(KEY_START_YEAR, calendar.get(Calendar.YEAR));
        int hour = sharedPreferences.getInt(KEY_SET_TIME_HOUR, 18);
        int minute = sharedPreferences.getInt(KEY_SET_TIME_MINUTE, 0);
        calendar.set(year, month, day, hour, minute);
        return calendar;
    }

    public void saveStartDate(Calendar calendar) {
        sharedPreferences.edit()
                .putInt(KEY_START_DAY, calendar.get(Calendar.DAY_OF_MONTH))
                .putInt(KEY_START_MONTH, calendar.get(Calendar.MONTH))
                .putInt(KEY_START_YEAR, calendar.get(Calendar.YEAR))
                .putInt(KEY_SET_TIME_HOUR, calendar.get(Calendar.HOUR_OF_DAY))
                .putInt(KEY_SET_TIME_MINUTE, calendar.get(Calendar.MINUTE))
                .apply();
    }

    public int getCycleLength() {
        return sharedPreferences.getInt(KEY_CYCLE_LENGTH, 21);
    }

    public void saveCycleLength(int length) {
        sharedPreferences.edit().putInt(KEY_CYCLE_LENGTH, length).apply();
    }

    public String getBackgroundImageUri() {
        return sharedPreferences.getString(KEY_BACKGROUND_IMAGE_URI, null);
    }

    public void saveBackgroundImageUri(String uri) {
        sharedPreferences.edit().putString(KEY_BACKGROUND_IMAGE_URI, uri).apply();
    }

    public int getCalendarPastAmount() {
        if (sharedPreferences.contains(KEY_CALENDAR_PAST_AMOUNT)) {
            return sharedPreferences.getInt(KEY_CALENDAR_PAST_AMOUNT, 3);
        }
        return sharedPreferences.getInt(KEY_CALENDAR_PAST_MONTHS, 3);
    }

    public String getCalendarPastUnit() {
        if (sharedPreferences.contains(KEY_CALENDAR_PAST_UNIT)) {
            return sharedPreferences.getString(KEY_CALENDAR_PAST_UNIT, "months");
        }
        return "months";
    }

    public void saveCalendarPastRange(int amount, String unit) {
        sharedPreferences.edit()
                .putInt(KEY_CALENDAR_PAST_AMOUNT, amount)
                .putString(KEY_CALENDAR_PAST_UNIT, unit)
                .apply();
    }

    public int getCalendarFutureAmount() {
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_AMOUNT)) {
            return sharedPreferences.getInt(KEY_CALENDAR_FUTURE_AMOUNT, 1);
        }
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_YEARS)) {
            return sharedPreferences.getInt(KEY_CALENDAR_FUTURE_YEARS, 1);
        }
        return 1;
    }

    public String getCalendarFutureUnit() {
        if (sharedPreferences.contains(KEY_CALENDAR_FUTURE_UNIT)) {
            return sharedPreferences.getString(KEY_CALENDAR_FUTURE_UNIT, "years");
        }
        sharedPreferences.contains(KEY_CALENDAR_FUTURE_YEARS);
        return "years";
    }

    public void saveCalendarFutureRange(int amount, String unit) {
        sharedPreferences.edit()
                .putInt(KEY_CALENDAR_FUTURE_AMOUNT, amount)
                .putString(KEY_CALENDAR_FUTURE_UNIT, unit)
                .apply();
    }

    public List<Cycle> getCycleHistory() {
        Gson gson = new Gson();
        String json = sharedPreferences.getString(KEY_CYCLE_HISTORY, null);
        Type type = new TypeToken<ArrayList<Cycle>>() {}.getType();
        List<Cycle> cycleHistory = gson.fromJson(json, type);
        return cycleHistory != null ? cycleHistory : new ArrayList<>();
    }

    public void saveCycleHistory(List<Cycle> cycleHistory) {
        Gson gson = new Gson();
        String json = gson.toJson(cycleHistory);
        sharedPreferences.edit().putString(KEY_CYCLE_HISTORY, json).apply();
    }

    public boolean wasNotificationScheduledForCycle(long cycleStartMillis) {
        return sharedPreferences.getBoolean("notified_" + cycleStartMillis, false);
    }

    public void setNotificationScheduledForCycle(long cycleStartMillis) {
        sharedPreferences.edit().putBoolean("notified_" + cycleStartMillis, true).apply();
    }

    public void clearNotificationScheduledForCycle(long cycleStartMillis) {
        sharedPreferences.edit().putBoolean("notified_" + cycleStartMillis, false).apply();
    }

    public int getRemovalReminderHours() {
        return sharedPreferences.getInt(KEY_REMOVAL_REMINDER_HOURS, 6);
    }

    public void setRemovalReminderHours(int hours) {
        sharedPreferences.edit().putInt(KEY_REMOVAL_REMINDER_HOURS, hours).apply();
    }

    public int getInsertionReminderHours() {
        return sharedPreferences.getInt(KEY_INSERTION_REMINDER_HOURS, 6);
    }

    public void setInsertionReminderHours(int hours) {
        sharedPreferences.edit().putInt(KEY_INSERTION_REMINDER_HOURS, hours).apply();
    }

    public int getButtonColor() {
        return sharedPreferences.getInt(KEY_BUTTON_COLOR, DEFAULT_BUTTON_COLOR);
    }

    public void saveButtonColor(int color) {
        sharedPreferences.edit().putInt(KEY_BUTTON_COLOR, color).apply();
    }

    public int getHomeCircleColor() {
        return sharedPreferences.getInt(KEY_HOME_CIRCLE_COLOR, DEFAULT_HOME_CIRCLE_COLOR);
    }

    public void saveHomeCircleColor(int color) {
        sharedPreferences.edit().putInt(KEY_HOME_CIRCLE_COLOR, color).apply();
    }

    public int getHomeCircleStyle() {
        int style = sharedPreferences.getInt(KEY_HOME_CIRCLE_STYLE, DEFAULT_HOME_CIRCLE_STYLE);
        int version = sharedPreferences.getInt(KEY_HOME_CIRCLE_STYLE_VERSION, 0);
        if (version == 0) {
            style = remapHomeCircleStyleV1(style);
            sharedPreferences.edit()
                    .putInt(KEY_HOME_CIRCLE_STYLE, style)
                    .putInt(KEY_HOME_CIRCLE_STYLE_VERSION, 1)
                    .apply();
        }
        if (style < 0 || style > 11) {
            return DEFAULT_HOME_CIRCLE_STYLE;
        }
        return style;
    }

    public void saveHomeCircleStyle(int style) {
        sharedPreferences.edit().putInt(KEY_HOME_CIRCLE_STYLE, style).apply();
    }

    public boolean isDebugToolsEnabled() {
        return sharedPreferences.getBoolean(KEY_DEBUG_TOOLS_ENABLED, false);
    }

    public void setDebugToolsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DEBUG_TOOLS_ENABLED, enabled).apply();
    }

    public boolean isDebugTimeEnabled() {
        return sharedPreferences.getBoolean(KEY_DEBUG_TIME_ENABLED, false);
    }

    public void setDebugTimeEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DEBUG_TIME_ENABLED, enabled).apply();
    }

    public long getDebugTimeMillis() {
        return sharedPreferences.getLong(KEY_DEBUG_TIME_MILLIS, 0L);
    }

    public void setDebugTimeMillis(long millis) {
        sharedPreferences.edit().putLong(KEY_DEBUG_TIME_MILLIS, millis).apply();
    }

    public void clearDebugTimeMillis() {
        sharedPreferences.edit().remove(KEY_DEBUG_TIME_MILLIS).apply();
    }

    public boolean wasExactAlarmPrompted() {
        return sharedPreferences.getBoolean(KEY_EXACT_ALARM_PROMPTED, false);
    }

    public void setExactAlarmPrompted(boolean prompted) {
        sharedPreferences.edit().putBoolean(KEY_EXACT_ALARM_PROMPTED, prompted).apply();
    }

    public boolean isAppLockEnabled() {
        return sharedPreferences.getBoolean(KEY_APP_LOCK_ENABLED, false);
    }

    public void setAppLockEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply();
    }

    public int getAppLockTimeoutMs() {
        return sharedPreferences.getInt(KEY_APP_LOCK_TIMEOUT_MS, 0);
    }

    public void setAppLockTimeoutMs(int timeoutMs) {
        sharedPreferences.edit().putInt(KEY_APP_LOCK_TIMEOUT_MS, Math.max(0, timeoutMs)).apply();
    }

    private int remapHomeCircleStyleV1(int style) {
        switch (style) {
            case 0: // Classic
                return 0;
            case 1: // Thin
                return 1;
            case 2: // Halo -> moved to end
                return 11;
            case 3: // Segmented
                return 2;
            case 4: // Gradient
                return 5;
            case 5: // Marker
                return 4;
            case 6: // Arc Only
                return 3;
            case 7: // Glow
                return 6;
            case 8: // Gradient Glow
                return 7;
            case 9: // Pulse Light
                return 8;
            case 10: // Pulse Medium
                return 9;
            case 11: // Pulse Strong
                return 10;
            default:
                return DEFAULT_HOME_CIRCLE_STYLE;
        }
    }

    public int getCalendarWearColor() {
        return sharedPreferences.getInt(KEY_CALENDAR_WEAR_COLOR, DEFAULT_CALENDAR_WEAR_COLOR);
    }

    public void saveCalendarWearColor(int color) {
        sharedPreferences.edit().putInt(KEY_CALENDAR_WEAR_COLOR, color).apply();
    }

    public int getCalendarRingFreeColor() {
        return sharedPreferences.getInt(KEY_CALENDAR_RING_FREE_COLOR, DEFAULT_CALENDAR_RING_FREE_COLOR);
    }

    public void saveCalendarRingFreeColor(int color) {
        sharedPreferences.edit().putInt(KEY_CALENDAR_RING_FREE_COLOR, color).apply();
    }

    public int getCalendarRemovalColor() {
        return sharedPreferences.getInt(KEY_CALENDAR_REMOVAL_COLOR, DEFAULT_CALENDAR_REMOVAL_COLOR);
    }

    public void saveCalendarRemovalColor(int color) {
        sharedPreferences.edit().putInt(KEY_CALENDAR_REMOVAL_COLOR, color).apply();
    }

    public int getCalendarInsertionColor() {
        return sharedPreferences.getInt(KEY_CALENDAR_INSERTION_COLOR, DEFAULT_CALENDAR_INSERTION_COLOR);
    }

    public void saveCalendarInsertionColor(int color) {
        sharedPreferences.edit().putInt(KEY_CALENDAR_INSERTION_COLOR, color).apply();
    }

    public int getNavigationAnimationStyle() {
        int style = sharedPreferences.getInt(KEY_NAVIGATION_ANIMATION_STYLE, DEFAULT_NAVIGATION_ANIMATION_STYLE);
        if (style < NAV_ANIM_SLIDE || style > NAV_ANIM_POP) {
            return DEFAULT_NAVIGATION_ANIMATION_STYLE;
        }
        return style;
    }

    public void saveNavigationAnimationStyle(int style) {
        sharedPreferences.edit().putInt(KEY_NAVIGATION_ANIMATION_STYLE, style).apply();
    }

    public int getNotificationSettingsHash() {
        int cycleLength = getCycleLength();
        int removalHours = getRemovalReminderHours();
        int insertionHours = getInsertionReminderHours();
        int hash = 17;
        hash = 31 * hash + cycleLength;
        hash = 31 * hash + removalHours;
        hash = 31 * hash + insertionHours;
        return hash;
    }

    public int getNotificationSettingsHashForCycle(long cycleStartMillis) {
        return sharedPreferences.getInt("notified_settings_" + cycleStartMillis, 0);
    }

    public void setNotificationSettingsHashForCycle(long cycleStartMillis, int hash) {
        sharedPreferences.edit().putInt("notified_settings_" + cycleStartMillis, hash).apply();
    }

    public void clearAllData() {
        sharedPreferences.edit().clear().apply();
    }

    public void clearNotificationFlags() {
        android.content.SharedPreferences.Editor editor = sharedPreferences.edit();
        for (String key : sharedPreferences.getAll().keySet()) {
            if (key.startsWith("notified_") || key.startsWith("notified_settings_")) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public int getCycleDelayDays(long cycleStartMillis) {
        return sharedPreferences.getInt(cycleDelayKey(cycleStartMillis), 0);
    }

    public void setCycleDelayDays(long cycleStartMillis, int delayDays) {
        sharedPreferences.edit().putInt(cycleDelayKey(cycleStartMillis), delayDays).apply();
    }

    private String cycleDelayKey(long cycleStartMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cycleStartMillis);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        return "cycle_delay_" + year + "_" + month + "_" + day;
    }
}
