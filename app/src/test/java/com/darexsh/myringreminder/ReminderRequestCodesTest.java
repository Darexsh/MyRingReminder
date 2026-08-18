package com.darexsh.myringreminder;

import static org.junit.Assert.assertNotEquals;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class ReminderRequestCodesTest {

    @Test
    public void buildRequestCode_doesNotMatchLegacyTimestampGuessingScheme() {
        Calendar cycleStart = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cycleStart.set(2026, Calendar.JULY, 21, 18, 0, 0);
        cycleStart.set(Calendar.MILLISECOND, 0);
        long cycleStartMillis = cycleStart.getTimeInMillis();

        Calendar triggerBase = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        triggerBase.set(2026, Calendar.AUGUST, 18, 12, 0, 0);
        triggerBase.set(Calendar.MILLISECOND, 0);

        for (int typeId : ReminderRequestCodes.ALL_TYPES) {
            int actual = ReminderRequestCodes.buildRequestCode(cycleStartMillis, typeId);
            int guessed = (int) ((triggerBase.getTimeInMillis() / 1000) % Integer.MAX_VALUE) + typeId;
            assertNotEquals(actual, guessed);
        }
    }
}
