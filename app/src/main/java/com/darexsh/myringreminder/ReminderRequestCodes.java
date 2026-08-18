package com.darexsh.myringreminder;

final class ReminderRequestCodes {

    static final int NOTIFY_TWO_WEEKS = 0;
    static final int NOTIFY_ONE_WEEK = 1;
    static final int NOTIFY_REMOVAL_REMINDER = 2;
    static final int NOTIFY_REMOVAL_EXACT = 3;
    static final int NOTIFY_INSERTION_REMINDER = 4;
    static final int NOTIFY_INSERTION_EXACT = 5;

    static final int[] ALL_TYPES = new int[]{
            NOTIFY_TWO_WEEKS,
            NOTIFY_ONE_WEEK,
            NOTIFY_REMOVAL_REMINDER,
            NOTIFY_REMOVAL_EXACT,
            NOTIFY_INSERTION_REMINDER,
            NOTIFY_INSERTION_EXACT
    };

    private ReminderRequestCodes() {
    }

    static int buildRequestCode(long cycleStartMillis, int typeId) {
        long hash = cycleStartMillis ^ (cycleStartMillis >>> 32);
        int base = (int) (hash & 0x7fffffff);
        int code = base + (typeId + 1) * 1000;
        return code < 0 ? base : code;
    }
}
