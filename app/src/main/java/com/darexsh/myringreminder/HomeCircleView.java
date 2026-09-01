package com.darexsh.myringreminder;

import android.annotation.SuppressLint;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HomeCircleView extends View {
    public static final int STYLE_CLASSIC = 0;
    public static final int STYLE_THIN = 1;
    public static final int STYLE_SEGMENTED = 2;
    public static final int STYLE_ARC = 3;
    public static final int STYLE_MARKER = 4;
    public static final int STYLE_GRADIENT = 5;
    public static final int STYLE_GLOW = 6;
    public static final int STYLE_GRADIENT_GLOW = 7;
    public static final int STYLE_PULSE_LIGHT = 8;
    public static final int STYLE_PULSE_MEDIUM = 9;
    public static final int STYLE_PULSE_STRONG = 10;
    public static final int STYLE_HALO = 11;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint periodDropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint calmParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path innerRainClipPath = new Path();
    private final RectF arcRect = new RectF();
    private final Matrix gradientMatrix = new Matrix();

    private int style = STYLE_CLASSIC;
    private int indicatorColor = SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
    private final int trackColor = Color.parseColor("#E0E0E0");
    private int max = 1;
    private int progress = 0;
    private float pulsePhase = 0f;
    private float animatedProgressFraction = 0f;
    private boolean hasAnimatedProgress = false;
    private boolean periodActive = false;
    private long innerParticleStartTimeMs = 0L;
    @Nullable
    private ValueAnimator progressAnimator;

    public HomeCircleView(Context context) {
        super(context);
        init();
    }

    public HomeCircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HomeCircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStyle(Paint.Style.STROKE);
        markerPaint.setStyle(Paint.Style.FILL);
        edgeGlowPaint.setStyle(Paint.Style.FILL);
        edgeCorePaint.setStyle(Paint.Style.FILL);
        edgeParticlePaint.setStyle(Paint.Style.FILL);
        periodDropPaint.setStyle(Paint.Style.FILL);
        calmParticlePaint.setStyle(Paint.Style.FILL);
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void setStyle(int style) {
        this.style = style;
        invalidate();
    }

    public void setIndicatorColor(int color) {
        this.indicatorColor = color;
        invalidate();
    }

    public void setMax(int max) {
        this.max = Math.max(1, max);
        if (!hasAnimatedProgress) {
            animatedProgressFraction = clampProgressFraction(progress);
        }
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        float targetFraction = clampProgressFraction(this.progress);
        if (!hasAnimatedProgress) {
            animateProgress(0f, targetFraction);
            hasAnimatedProgress = true;
            return;
        }
        animateProgress(animatedProgressFraction, targetFraction);
    }

    public void setPulsePhase(float phase) {
        this.pulsePhase = phase;
        invalidate();
    }

    public void setPeriodActive(boolean periodActive) {
        if (this.periodActive == periodActive) {
            return;
        }
        this.periodActive = periodActive;
        innerParticleStartTimeMs = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        float contentWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float contentHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        float size = Math.min(contentWidth, contentHeight);
        float thickness = dpToPx(16);
        if ((style == STYLE_PULSE_LIGHT || style == STYLE_PULSE_MEDIUM || style == STYLE_PULSE_STRONG
                || style == STYLE_GLOW || style == STYLE_GRADIENT_GLOW)) {
            dpToPx(10);
        } else {
            dpToPx(4);
        }

        trackPaint.setColor(trackColor);
        progressPaint.setColor(indicatorColor);
        trackPaint.setPathEffect(null);
        progressPaint.setPathEffect(null);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setShader(null);
        progressPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);

        float left = getPaddingLeft() + (contentWidth - size) / 2f + thickness / 2f;
        float top = getPaddingTop() + (contentHeight - size) / 2f + thickness / 2f;
        float right = left + size - thickness;
        float bottom = top + size - thickness;
        arcRect.set(left, top, right, bottom);

        float progressFraction = hasAnimatedProgress
                ? animatedProgressFraction
                : clampProgressFraction(progress);
        if (style == STYLE_SEGMENTED) {
            trackPaint.setStrokeWidth(thickness);
            progressPaint.setStrokeWidth(thickness);
            drawSegmented(canvas, thickness, progressFraction);
            if (progressFraction > 0f) {
                drawSegmentedLeadingEdgeHighlight(canvas, thickness, progressFraction);
            }
            drawInnerParticleEffect(canvas, thickness);
            postInvalidateOnAnimation();
            return;
        }

        switch (style) {
            case STYLE_THIN:
                thickness = dpToPx(10);
                break;
            case STYLE_HALO:
                trackPaint.setColor(withAlpha(indicatorColor, 70));
                break;
            case STYLE_GRADIENT:
                @SuppressLint("DrawAllocation") SweepGradient gradient = new SweepGradient(
                        getWidth() / 2f,
                        getHeight() / 2f,
                        new int[]{withAlpha(indicatorColor, 60), indicatorColor, withAlpha(indicatorColor, 60)},
                        new float[]{0f, 0.7f, 1f});
                gradientMatrix.setRotate(-90, getWidth() / 2f, getHeight() / 2f);
                gradient.setLocalMatrix(gradientMatrix);
                progressPaint.setShader(gradient);
                break;
            case STYLE_ARC:
                trackPaint.setColor(Color.TRANSPARENT);
                break;
            case STYLE_GLOW:
                progressPaint.setShadowLayer(dpToPx(10), 0f, 0f, withAlpha(indicatorColor, 220));
                break;
            case STYLE_GRADIENT_GLOW:
                @SuppressLint("DrawAllocation") SweepGradient gradientGlow = new SweepGradient(
                        getWidth() / 2f,
                        getHeight() / 2f,
                        new int[]{withAlpha(indicatorColor, 50), indicatorColor, withAlpha(indicatorColor, 50)},
                        new float[]{0f, 0.7f, 1f});
                gradientMatrix.setRotate(-90, getWidth() / 2f, getHeight() / 2f);
                gradientGlow.setLocalMatrix(gradientMatrix);
                progressPaint.setShader(gradientGlow);
                progressPaint.setShadowLayer(dpToPx(10), 0f, 0f, withAlpha(indicatorColor, 220));
                break;
            case STYLE_PULSE_LIGHT:
                progressPaint.setShadowLayer(dpToPx(8 + 6 * pulsePhase), 0f, 0f,
                        withAlpha(indicatorColor, 120 + Math.round(70 * pulsePhase)));
                break;
            case STYLE_PULSE_MEDIUM:
                progressPaint.setShadowLayer(dpToPx(12 + 10 * pulsePhase), 0f, 0f,
                        withAlpha(indicatorColor, 160 + Math.round(90 * pulsePhase)));
                break;
            case STYLE_PULSE_STRONG:
                progressPaint.setShadowLayer(dpToPx(18 + 14 * pulsePhase), 0f, 0f,
                        withAlpha(indicatorColor, 190 + Math.round(110 * pulsePhase)));
                break;
            case STYLE_CLASSIC:
            default:
                break;
        }

        trackPaint.setStrokeWidth(thickness);
        progressPaint.setStrokeWidth(thickness);
        markerPaint.setColor(indicatorColor);

        float sweep = 360f * progressFraction;
        if ((style == STYLE_PULSE_LIGHT || style == STYLE_PULSE_MEDIUM || style == STYLE_PULSE_STRONG)
                && sweep <= 0f) {
            sweep = 2f;
        }

        if (style != STYLE_ARC) {
            canvas.drawArc(arcRect, 0f, 360f, false, trackPaint);
        }
        if (sweep > 0f) {
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint);
            drawLeadingEdgeHighlight(canvas, thickness, progressFraction);
        }

        drawInnerParticleEffect(canvas, thickness);
        postInvalidateOnAnimation();

        if (style == STYLE_MARKER) {
            float angle = (float) Math.toRadians(-90 + sweep);
            float cx = arcRect.centerX();
            float cy = arcRect.centerY();
            float outer = dpToPx(7);
            float inner = dpToPx(5);
            float markerRadius = arcRect.width() / 2f;
            float x = cx + (float) Math.cos(angle) * markerRadius;
            float y = cy + (float) Math.sin(angle) * markerRadius;
            @SuppressLint("DrawAllocation") Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.FILL);
            ring.setColor(Color.WHITE);
            canvas.drawCircle(x, y, outer, ring);
            canvas.drawCircle(x, y, inner, markerPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (progressAnimator != null) {
            progressAnimator.cancel();
            progressAnimator = null;
        }
        super.onDetachedFromWindow();
    }

    private void drawSegmented(Canvas canvas, float thickness, float fraction) {
        int segments = Math.max(1, max);
        float gap = 360f / segments * 0.55f;
        float sweep = 360f / segments - gap;
        float start = -90f;

        trackPaint.setStrokeWidth(thickness);
        progressPaint.setStrokeWidth(thickness);

        for (int i = 0; i < segments; i++) {
            float segStart = start + i * (sweep + gap);
            canvas.drawArc(arcRect, segStart, sweep, false, trackPaint);
        }

        float filledFloat = Math.max(0f, Math.min(segments, segments * fraction));
        int filledWhole = Math.max(0, Math.min(segments, (int) Math.floor(filledFloat + 0.0001f)));
        for (int i = 0; i < filledWhole; i++) {
            float segStart = start + i * (sweep + gap);
            canvas.drawArc(arcRect, segStart, sweep, false, progressPaint);
        }

        if (filledWhole < segments) {
            float partialFraction = filledFloat - filledWhole;
            if (partialFraction > 0.0001f) {
                float segStart = start + (filledWhole * (sweep + gap));
                canvas.drawArc(arcRect, segStart, sweep * partialFraction, false, progressPaint);
            }
        }
    }

    private void drawSegmentedLeadingEdgeHighlight(@NonNull Canvas canvas, float thickness, float fraction) {
        int segments = Math.max(1, max);
        float filledFloat = Math.max(0f, Math.min(segments, segments * fraction));
        int filledWhole = Math.max(0, Math.min(segments, (int) Math.floor(filledFloat + 0.0001f)));
        float partialFraction = filledFloat - filledWhole;
        if (filledWhole == 0 && partialFraction == 0f) {
            return;
        }

        float gap = 360f / segments * 0.55f;
        float sweep = 360f / segments - gap;
        float slot = sweep + gap;
        float endAngleDegrees;
        if (partialFraction > 0.0001f && filledWhole < segments) {
            endAngleDegrees = -90f + (filledWhole * slot) + (sweep * partialFraction);
        } else {
            endAngleDegrees = -90f + ((filledWhole - 1) * slot) + sweep;
        }
        float angle = (float) Math.toRadians(endAngleDegrees);
        drawLeadingEdgeHighlightAtAngle(canvas, thickness, angle);
    }

    private void drawLeadingEdgeHighlight(@NonNull Canvas canvas, float thickness, float progressFraction) {
        float sweep = 360f * progressFraction;
        float angle = (float) Math.toRadians(-90f + sweep);
        drawLeadingEdgeHighlightAtAngle(canvas, thickness, angle);
    }

    private void drawLeadingEdgeHighlightAtAngle(@NonNull Canvas canvas, float thickness, float angle) {
        float radius = arcRect.width() / 2f;
        float cx = arcRect.centerX();
        float cy = arcRect.centerY();
        float x = cx + (float) Math.cos(angle) * radius;
        float y = cy + (float) Math.sin(angle) * radius;

        float glowRadius = thickness * 0.82f;
        float coreRadius = thickness * 0.36f;
        float particleRadius = thickness * 0.13f;
        float tangentX = (float) -Math.sin(angle);
        float tangentY = (float) Math.cos(angle);

        edgeGlowPaint.setColor(withAlpha(indicatorColor, 150));
        edgeGlowPaint.setShadowLayer(thickness * 0.75f, 0f, 0f, withAlpha(indicatorColor, 210));
        canvas.drawCircle(x, y, glowRadius, edgeGlowPaint);

        edgeCorePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        edgeCorePaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, coreRadius, edgeCorePaint);

        edgeParticlePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
        edgeParticlePaint.setColor(withAlpha(indicatorColor, 210));
        canvas.drawCircle(x - tangentX * (thickness * 0.58f), y - tangentY * (thickness * 0.58f),
                particleRadius * 1.45f, edgeParticlePaint);
        canvas.drawCircle(x - tangentX * (thickness * 1.02f), y - tangentY * (thickness * 1.02f),
                particleRadius, edgeParticlePaint);
    }

    private void drawInnerParticleEffect(@NonNull Canvas canvas, float thickness) {
        if (periodActive) {
            drawPeriodRain(canvas, thickness);
        } else {
            drawCalmFloat(canvas, thickness);
        }
    }

    private void drawPeriodRain(@NonNull Canvas canvas, float thickness) {
        float innerRadius = (arcRect.width() / 2f) - (thickness * 0.95f);
        if (innerRadius <= 0f) {
            return;
        }
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        float clipRadius = innerRadius * 0.9f;
        long elapsed = Math.max(0L, SystemClock.uptimeMillis() - innerParticleStartTimeMs);
        int saveCount = canvas.save();
        innerRainClipPath.reset();
        innerRainClipPath.addCircle(centerX, centerY, clipRadius, Path.Direction.CW);
        canvas.clipPath(innerRainClipPath);

        final int particleCount = 11;
        for (int i = 0; i < particleCount; i++) {
            float laneSeed = (i + 0.5f) / particleCount;
            float speed = 0.18f + (pseudoRandomUnit(i * 53 + 7) * 0.16f);
            float offset = pseudoRandomUnit(i * 71 + 19);
            float drift = (pseudoRandomUnit(i * 29 + 5) - 0.5f) * clipRadius * 0.16f;
            float progress = (offset + ((elapsed / 1000f) * speed)) % 1f;
            float x = centerX - clipRadius + (laneSeed * clipRadius * 2f) + drift;
            float y = centerY - clipRadius + (progress * clipRadius * 2f);
            float dropHeight = dpToPx(7.5f) + (pseudoRandomUnit(i * 17 + 3) * dpToPx(4.5f));
            float dropWidth = dropHeight * 0.68f;
            int alpha = 108 + Math.round(pseudoRandomUnit(i * 97 + 13) * 82f);
            periodDropPaint.setColor(withAlpha(0xFF7A1026, alpha));
            canvas.drawOval(x - dropWidth / 2f, y - dropHeight / 2f, x + dropWidth / 2f, y + dropHeight / 2f, periodDropPaint);
            periodDropPaint.setColor(withAlpha(0xFFB3203E, Math.min(255, alpha + 26)));
            canvas.drawCircle(x, y - (dropHeight * 0.2f), dropWidth * 0.28f, periodDropPaint);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawCalmFloat(@NonNull Canvas canvas, float thickness) {
        float innerRadius = (arcRect.width() / 2f) - (thickness * 0.95f);
        if (innerRadius <= 0f) {
            return;
        }
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        float clipRadius = innerRadius * 0.9f;
        long elapsed = Math.max(0L, SystemClock.uptimeMillis() - innerParticleStartTimeMs);
        int saveCount = canvas.save();
        innerRainClipPath.reset();
        innerRainClipPath.addCircle(centerX, centerY, clipRadius, Path.Direction.CW);
        canvas.clipPath(innerRainClipPath);

        final int particleCount = 10;
        for (int i = 0; i < particleCount; i++) {
            float laneSeed = (i + 0.5f) / particleCount;
            float speed = 0.08f + (pseudoRandomUnit(i * 41 + 9) * 0.08f);
            float offset = pseudoRandomUnit(i * 67 + 15);
            float sway = (pseudoRandomUnit(i * 23 + 4) - 0.5f) * clipRadius * 0.2f;
            float progress = (offset + ((elapsed / 1000f) * speed)) % 1f;
            float x = centerX - clipRadius + (laneSeed * clipRadius * 2f)
                    + (float) Math.sin((elapsed / 700f) + i) * sway;
            float y = centerY + clipRadius - (progress * clipRadius * 2f);
            float particleRadius = dpToPx(2.4f) + (pseudoRandomUnit(i * 31 + 6) * dpToPx(1.8f));
            int glowAlpha = 44 + Math.round(pseudoRandomUnit(i * 79 + 12) * 42f);
            int coreAlpha = 72 + Math.round(pseudoRandomUnit(i * 91 + 3) * 56f);

            calmParticlePaint.setColor(withAlpha(indicatorColor, glowAlpha));
            calmParticlePaint.setShadowLayer(dpToPx(6f), 0f, 0f, withAlpha(indicatorColor, 90));
            canvas.drawCircle(x, y, particleRadius * 1.6f, calmParticlePaint);

            calmParticlePaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT);
            calmParticlePaint.setColor(withAlpha(0xFFFFFFFF, coreAlpha));
            canvas.drawCircle(x, y, particleRadius, calmParticlePaint);
        }

        canvas.restoreToCount(saveCount);
    }

    private void animateProgress(float startFraction, float targetFraction) {
        float clampedStart = Math.max(0f, Math.min(1f, startFraction));
        float clampedTarget = Math.max(0f, Math.min(1f, targetFraction));
        if (progressAnimator != null) {
            progressAnimator.cancel();
        }
        if (Math.abs(clampedStart - clampedTarget) < 0.0001f) {
            animatedProgressFraction = clampedTarget;
            invalidate();
            return;
        }
        progressAnimator = ValueAnimator.ofFloat(clampedStart, clampedTarget);
        progressAnimator.setDuration(1000L);
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.addUpdateListener(animation -> {
            animatedProgressFraction = (float) animation.getAnimatedValue();
            invalidate();
        });
        progressAnimator.start();
    }

    private float clampProgressFraction(int value) {
        return Math.max(0f, Math.min(1f, (float) value / (float) max));
    }

    private float pseudoRandomUnit(int seed) {
        double value = Math.sin(seed * 12.9898 + 78.233) * 43758.5453;
        return (float) (value - Math.floor(value));
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }
}
