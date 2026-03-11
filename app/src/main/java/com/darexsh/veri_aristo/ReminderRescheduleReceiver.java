package com.darexsh.veri_aristo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderRescheduleReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ReminderScheduler.scheduleCurrentCycle(context);
    }
}
