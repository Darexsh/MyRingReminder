package com.darexsh.veri_aristo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class CycleWidgetUtils {

    private CycleWidgetUtils() {
    }

    public static final class State {
        public final int daysLeft;
        public final int maxProgress;
        public final int currentProgress;
        public final String label;
        public final String removalText;
        public final String insertionText;

        public State(int daysLeft, int maxProgress, int currentProgress, String label, String removalText, String insertionText) {
            this.daysLeft = daysLeft;
            this.maxProgress = maxProgress;
            this.currentProgress = currentProgress;
            this.label = label;
            this.removalText = removalText;
            this.insertionText = insertionText;
        }
    }

    public static State calculateState(Context context) {
        SettingsRepository repository = new SettingsRepository(context);
        Calendar baseStart = repository.getStartDate();
        Calendar now = DebugTimeProvider.now(context);
        int cycleLength = repository.getCycleLength();

        Calendar currentStart = (Calendar) baseStart.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);

        int delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
        int ringFreeDays = repository.getRingFreeDaysForCycle(currentStart.getTimeInMillis());
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        Calendar nowDay = startOfDay(now);
        Calendar reinsertionDay = startOfDay(reinsertionDate);

        int guard = 0;
        while (nowDay.after(reinsertionDay) && guard < 200) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
            ringFreeDays = repository.getRingFreeDaysForCycle(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            reinsertionDay = startOfDay(reinsertionDate);
            guard++;
        }

        int remainingDays;
        int maxProgress;
        int currentProgress;
        String label;

        if (now.before(removalDate)) {
            remainingDays = daysBetweenDays(now, removalDate);
            maxProgress = cycleLength;
            currentProgress = maxProgress - remainingDays;
            label = context.getString(R.string.home_days_left);
        } else if (now.before(reinsertionDate)) {
            remainingDays = daysBetweenDays(now, reinsertionDate);
            maxProgress = Math.max(1, ringFreeDays);
            currentProgress = maxProgress - remainingDays;
            label = context.getString(R.string.home_days_until_insertion);
        } else {
            remainingDays = cycleLength;
            maxProgress = cycleLength;
            currentProgress = 0;
            label = context.getString(R.string.home_days_left);
        }

        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        String removalText = context.getString(R.string.home_removal_on, format.format(removalDate.getTime()));
        String insertionText = context.getString(R.string.home_insertion_on, format.format(reinsertionDate.getTime()));

        return new State(remainingDays, maxProgress, currentProgress, label, removalText, insertionText);
    }

    private static Calendar startOfDay(Calendar source) {
        Calendar day = (Calendar) source.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    private static int daysBetweenDays(Calendar from, Calendar to) {
        Calendar fromDay = startOfDay(from);
        Calendar toDay = startOfDay(to);
        if (!fromDay.before(toDay)) {
            return 0;
        }
        int days = 0;
        while (fromDay.before(toDay)) {
            fromDay.add(Calendar.DAY_OF_MONTH, 1);
            days++;
        }
        return days;
    }

    public static Bitmap buildRingBitmap(Context context, float progressFraction, int ringColor, int style,
                                         int maxSegments) {
        int sizePx = context.getResources().getDimensionPixelSize(R.dimen.widget_ring_size);
        int strokePx = context.getResources().getDimensionPixelSize(R.dimen.widget_ring_stroke);
        float clamped = Math.max(0f, Math.min(1f, progressFraction));

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokePx);
        trackPaint.setColor(Color.WHITE);
        trackPaint.setAlpha(80);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokePx);
        progressPaint.setColor(ringColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        float radius = (sizePx - strokePx) / 2f;
        float cx = sizePx / 2f;
        float cy = sizePx / 2f;

        if (style == HomeCircleView.STYLE_THIN) {
            int thin = Math.max(2, Math.round(strokePx * 0.6f));
            trackPaint.setStrokeWidth(thin);
            progressPaint.setStrokeWidth(thin);
        }

        if (style == HomeCircleView.STYLE_HALO) {
            trackPaint.setColor(withAlpha(ringColor, 70));
        }

        if (style == HomeCircleView.STYLE_GLOW || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            progressPaint.setShadowLayer(strokePx * 0.8f, 0f, 0f, withAlpha(ringColor, 200));
        }
        if (style == HomeCircleView.STYLE_PULSE_LIGHT) {
            progressPaint.setShadowLayer(strokePx * 0.6f, 0f, 0f, withAlpha(ringColor, 160));
        } else if (style == HomeCircleView.STYLE_PULSE_MEDIUM) {
            progressPaint.setShadowLayer(strokePx * 0.9f, 0f, 0f, withAlpha(ringColor, 200));
        } else if (style == HomeCircleView.STYLE_PULSE_STRONG) {
            progressPaint.setShadowLayer(strokePx * 1.2f, 0f, 0f, withAlpha(ringColor, 230));
        }

        if (style == HomeCircleView.STYLE_GRADIENT || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            Shader shader = new SweepGradient(cx, cy,
                    new int[]{withAlpha(ringColor, 60), ringColor, withAlpha(ringColor, 60)},
                    new float[]{0f, 0.7f, 1f});
            progressPaint.setShader(shader);
        }

        if (style == HomeCircleView.STYLE_SEGMENTED) {
            int segments = Math.max(1, maxSegments);
            float gap = 360f / segments * 0.55f;
            float segSweep = 360f / segments - gap;
            float start = -90f;
            for (int i = 0; i < segments; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                        segStart, segSweep, false, trackPaint);
            }
            int filled = Math.round(segments * clamped);
            for (int i = 0; i < filled; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                        segStart, segSweep, false, progressPaint);
            }
            return bitmap;
        }

        if (style == HomeCircleView.STYLE_ARC) {
            canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                    -90f, 360f * clamped, false, progressPaint);
            return bitmap;
        }

        canvas.drawCircle(cx, cy, radius, trackPaint);
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius,
                -90f, 360f * clamped, false, progressPaint);
        if (style == HomeCircleView.STYLE_MARKER) {
            drawMarker(canvas, cx, cy, radius, 360f * clamped, ringColor, strokePx);
        }

        return bitmap;
    }

    public static long calculateNextWidgetUpdateMillis(Context context) {
        SettingsRepository repository = new SettingsRepository(context);
        Calendar baseStart = repository.getStartDate();
        Calendar now = DebugTimeProvider.now(context);
        long nowMillis = now.getTimeInMillis();
        int cycleLength = repository.getCycleLength();

        Calendar currentStart = (Calendar) baseStart.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);

        int delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
        int ringFreeDays = repository.getRingFreeDaysForCycle(currentStart.getTimeInMillis());
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        Calendar nowDay = startOfDay(now);
        Calendar reinsertionDay = startOfDay(reinsertionDate);

        int guard = 0;
        while (nowDay.after(reinsertionDay) && guard < 200) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
            ringFreeDays = repository.getRingFreeDaysForCycle(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            reinsertionDay = startOfDay(reinsertionDate);
            guard++;
        }

        Calendar nextEvent = null;
        if (now.before(removalDate)) {
            nextEvent = removalDate;
        } else if (now.before(reinsertionDate)) {
            nextEvent = reinsertionDate;
        } else {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = repository.getCycleDelayDays(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            nextEvent = removalDate;
        }

        Calendar nextMidnight = startOfDay(now);
        nextMidnight.add(Calendar.DAY_OF_MONTH, 1);

        long nextEventMillis = nextEvent != null ? nextEvent.getTimeInMillis() : Long.MAX_VALUE;
        long nextMidnightMillis = nextMidnight.getTimeInMillis();
        long nextAt = Math.min(nextEventMillis, nextMidnightMillis);

        if (nextAt <= nowMillis) {
            return nowMillis + 60_000L;
        }
        return nextAt;
    }

    private static void drawMarker(Canvas canvas, float cx, float cy, float radius, float sweep,
                                   int color, int strokePx) {
        float angle = (float) Math.toRadians(-90 + sweep);
        float x = cx + (float) Math.cos(angle) * radius;
        float y = cy + (float) Math.sin(angle) * radius;
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.FILL);
        ring.setColor(Color.WHITE);
        Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
        dot.setStyle(Paint.Style.FILL);
        dot.setColor(color);
        float outer = Math.max(3f, strokePx * 0.4f);
        float inner = Math.max(2f, strokePx * 0.28f);
        canvas.drawCircle(x, y, outer, ring);
        canvas.drawCircle(x, y, inner, dot);
    }

    private static int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }
}
