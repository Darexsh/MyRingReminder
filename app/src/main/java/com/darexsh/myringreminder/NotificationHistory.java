package com.darexsh.myringreminder;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class NotificationHistory {

    static final String EXTRA_TYPE = "notification_type";
    static final String EXTRA_CYCLE_START = "cycle_start_millis";
    static final String EXTRA_SCHEDULED_AT = "scheduled_at_millis";

    private static final String PREFS_NAME = "notification_history_prefs";
    private static final String KEY_ENTRIES = "entries";
    private static final int MAX_ENTRIES = 20;

    private NotificationHistory() {
    }

    static synchronized void record(Context context,
                                    int type,
                                    long cycleStartMillis,
                                    long scheduledAtMillis,
                                    long deliveredAtMillis,
                                    String title,
                                    String message,
                                    String outcome) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray entries;
        try {
            entries = new JSONArray(preferences.getString(KEY_ENTRIES, "[]"));
        } catch (JSONException ignored) {
            entries = new JSONArray();
        }

        JSONObject entry = new JSONObject();
        try {
            entry.put("type", type);
            entry.put("cycle_start", cycleStartMillis);
            entry.put("scheduled_at", scheduledAtMillis);
            entry.put("delivered_at", deliveredAtMillis);
            entry.put("title", title);
            entry.put("message", message);
            entry.put("outcome", outcome);
        } catch (JSONException ignored) {
            return;
        }
        entries.put(entry);

        JSONArray boundedEntries = new JSONArray();
        int firstEntry = Math.max(0, entries.length() - MAX_ENTRIES);
        for (int i = firstEntry; i < entries.length(); i++) {
            JSONObject existing = entries.optJSONObject(i);
            if (existing != null) {
                boundedEntries.put(existing);
            }
        }
        preferences.edit().putString(KEY_ENTRIES, boundedEntries.toString()).apply();
    }

    static synchronized List<Entry> getRecent(Context context, int limit) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        JSONArray entries;
        try {
            entries = new JSONArray(preferences.getString(KEY_ENTRIES, "[]"));
        } catch (JSONException ignored) {
            return new ArrayList<>();
        }

        List<Entry> result = new ArrayList<>();
        for (int i = entries.length() - 1; i >= 0 && result.size() < limit; i--) {
            JSONObject item = entries.optJSONObject(i);
            if (item == null) {
                continue;
            }
            result.add(new Entry(
                    item.optInt("type", -1),
                    item.optLong("cycle_start", 0L),
                    item.optLong("scheduled_at", 0L),
                    item.optLong("delivered_at", 0L),
                    item.optString("title", ""),
                    item.optString("message", ""),
                    item.optString("outcome", "")
            ));
        }
        return result;
    }

    static void clear(Context context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    static final class Entry {
        final int type;
        final long cycleStartMillis;
        final long scheduledAtMillis;
        final long deliveredAtMillis;
        final String title;
        final String message;
        final String outcome;

        Entry(int type,
              long cycleStartMillis,
              long scheduledAtMillis,
              long deliveredAtMillis,
              String title,
              String message,
              String outcome) {
            this.type = type;
            this.cycleStartMillis = cycleStartMillis;
            this.scheduledAtMillis = scheduledAtMillis;
            this.deliveredAtMillis = deliveredAtMillis;
            this.title = title;
            this.message = message;
            this.outcome = outcome;
        }
    }
}
