package com.darexsh.myringreminder;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderRescheduleReceiver extends BroadcastReceiver {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            ReminderScheduler.scheduleCurrentCycle(context);
            StockManager.syncCurrentCycle(context);
        } catch (RuntimeException e) {
            android.util.Log.e("ReminderReschedule", "Could not restore scheduled reminders", e);
        }
    }
}
