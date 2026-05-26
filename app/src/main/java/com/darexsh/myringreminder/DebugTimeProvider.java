package com.darexsh.myringreminder;

import android.content.Context;
import java.util.Calendar;

public final class DebugTimeProvider {

    private DebugTimeProvider() {
    }

    public static Calendar now(Context context) {
        return now(new SettingsRepository(context));
    }

    public static Calendar now(SettingsRepository repository) {
        if (repository != null
                && repository.isDebugToolsEnabled()
                && repository.isDebugTimeEnabled()) {
            long debugMillis = repository.getDebugTimeMillis();
            if (debugMillis > 0L) {
                Calendar debug = Calendar.getInstance();
                debug.setTimeInMillis(debugMillis);
                return debug;
            }
        }
        return Calendar.getInstance();
    }
}
