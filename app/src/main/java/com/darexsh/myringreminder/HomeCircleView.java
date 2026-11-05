package com.darexsh.myringreminder;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HomeCircleView extends View {
    private static final int CALM_PARTICLE_COUNT = 16;
    private static final int PERIOD_PARTICLE_COUNT = 18;

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
    private final Paint markerRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgeParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint periodDropPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint calmParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] calmLaneSeeds = new float[CALM_PARTICLE_COUNT];
    private final float[] calmSpeeds = new float[CALM_PARTICLE_COUNT];
    private final float[] calmOffsets = new float[CALM_PARTICLE_COUNT];
    private final float[] calmSwayFactors = new float[CALM_PARTICLE_COUNT];
    private final float[] calmRadii = new float[CALM_PARTICLE_COUNT];
    private final int[] calmGlowAlphas = new int[CALM_PARTICLE_COUNT];
    private final int[] calmCoreAlphas = new int[CALM_PARTICLE_COUNT];
    private final float[] periodLaneSeeds = new float[PERIOD_PARTICLE_COUNT];
    private final float[] periodSpeeds = new float[PERIOD_PARTICLE_COUNT];
    private final float[] periodOffsets = new float[PERIOD_PARTICLE_COUNT];
    private final float[] periodDriftFactors = new float[PERIOD_PARTICLE_COUNT];
    private final float[] periodDropHeights = new float[PERIOD_PARTICLE_COUNT];
    private final int[] periodAlphas = new int[PERIOD_PARTICLE_COUNT];
    private final Path innerRainClipPath = new Path();
    private final RectF arcRect = new RectF();
    private final Matrix gradientMatrix = new Matrix();
    @Nullable
    private SweepGradient gradientShader;
    @Nullable
    private SweepGradient gradientGlowShader;
    private int gradientShaderColor = Integer.MIN_VALUE;
    private int gradientShaderWidth = -1;
    private int gradientShaderHeight = -1;

    private int style = STYLE_CLASSIC;
    private int indicatorColor = SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
    private final int trackColor = Color.parseColor("#E0E0E0");
    private int max = 1;
    private int progress = 0;
    private float pulsePhase = 0f;
    private float animatedProgressFraction = 0f;
    private boolean hasAnimatedProgress = false;
    private boolean periodActive = false;
    private boolean pauseMode = false;
    private boolean animationEnabled = true;
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
        markerRingPaint.setStyle(Paint.Style.FILL);
        markerRingPaint.setColor(Color.WHITE);
        edgeGlowPaint.setStyle(Paint.Style.FILL);
        edgeCorePaint.setStyle(Paint.Style.FILL);
        edgeParticlePaint.setStyle(Paint.Style.FILL);
        progressGlowPaint.setStyle(Paint.Style.STROKE);
        progressGlowPaint.setStrokeCap(Paint.Cap.ROUND);
        periodDropPaint.setStyle(Paint.Style.FILL);
        calmParticlePaint.setStyle(Paint.Style.FILL);
        initializeParticleMotion();
    }

    private void initializeParticleMotion() {
        for (int i = 0; i < CALM_PARTICLE_COUNT; i++) {
            calmLaneSeeds[i] = (i + 0.5f) / CALM_PARTICLE_COUNT;
            calmSpeeds[i] = 0.08f + (pseudoRandomUnit(i * 41 + 9) * 0.08f);
            calmOffsets[i] = pseudoRandomUnit(i * 67 + 15);
            calmSwayFactors[i] = (pseudoRandomUnit(i * 23 + 4) - 0.5f) * 0.2f;
            calmRadii[i] = dpToPx(2.4f) + (pseudoRandomUnit(i * 31 + 6) * dpToPx(1.8f));
            calmGlowAlphas[i] = 44 + Math.round(pseudoRandomUnit(i * 79 + 12) * 42f);
            calmCoreAlphas[i] = 72 + Math.round(pseudoRandomUnit(i * 91 + 3) * 56f);
        }

        for (int i = 0; i < PERIOD_PARTICLE_COUNT; i++) {
            periodLaneSeeds[i] = (i + 0.5f) / PERIOD_PARTICLE_COUNT;
            periodSpeeds[i] = 0.18f + (pseudoRandomUnit(i * 53 + 7) * 0.16f);
            periodOffsets[i] = pseudoRandomUnit(i * 71 + 19);
            periodDriftFactors[i] = (pseudoRandomUnit(i * 29 + 5) - 0.5f) * 0.16f;
            periodDropHeights[i] = dpToPx(7.5f) + (pseudoRandomUnit(i * 17 + 3) * dpToPx(4.5f));
            periodAlphas[i] = 108 + Math.round(pseudoRandomUnit(i * 97 + 13) * 82f);
        }
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

    public void setAnimationEnabled(boolean enabled) {
        if (animationEnabled == enabled) {
            return;
        }
        animationEnabled = enabled;
        if (enabled) {
            innerParticleStartTimeMs = SystemClock.uptimeMillis();
            invalidate();
        }
    }

    public void setPeriodActive(boolean periodActive) {
        if (this.periodActive == periodActive) {
            return;
        }
        this.periodActive = periodActive;
        innerParticleStartTimeMs = SystemClock.uptimeMillis();
        invalidate();
    }

    public void setPauseMode(boolean pauseMode) {
        if (this.pauseMode == pauseMode) {
            return;
        }
        this.pauseMode = pauseMode;
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
        boolean useManualGlow = Build.VERSION.SDK_INT < Build.VERSION_CODES.P
                && canvas.isHardwareAccelerated();
        if (style == STYLE_SEGMENTED) {
            trackPaint.setStrokeWidth(thickness);
            progressPaint.setStrokeWidth(thickness);
            drawSegmented(canvas, thickness, progressFraction);
            if (progressFraction > 0f) {
                drawSegmentedLeadingEdgeHighlight(canvas, thickness, progressFraction);
            }
            if (animationEnabled) {
                drawInnerParticleEffect(canvas, thickness);
                scheduleParticleFrame(canvas);
            }
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
                ensureGradientShaders();
                progressPaint.setShader(gradientShader);
                break;
            case STYLE_ARC:
                trackPaint.setColor(Color.TRANSPARENT);
                break;
            case STYLE_GLOW:
                if (!useManualGlow) {
                    progressPaint.setShadowLayer(dpToPx(10), 0f, 0f, withAlpha(indicatorColor, 220));
                }
                break;
            case STYLE_GRADIENT_GLOW:
                ensureGradientShaders();
                progressPaint.setShader(gradientGlowShader);
                if (!useManualGlow) {
                    progressPaint.setShadowLayer(dpToPx(10), 0f, 0f, withAlpha(indicatorColor, 220));
                }
                break;
            case STYLE_PULSE_LIGHT:
                if (!useManualGlow) {
                    progressPaint.setShadowLayer(dpToPx(8 + 6 * pulsePhase), 0f, 0f,
                            withAlpha(indicatorColor, 120 + Math.round(70 * pulsePhase)));
                }
                break;
            case STYLE_PULSE_MEDIUM:
                if (!useManualGlow) {
                    progressPaint.setShadowLayer(dpToPx(12 + 10 * pulsePhase), 0f, 0f,
                            withAlpha(indicatorColor, 160 + Math.round(90 * pulsePhase)));
                }
                break;
            case STYLE_PULSE_STRONG:
                if (!useManualGlow) {
                    progressPaint.setShadowLayer(dpToPx(18 + 14 * pulsePhase), 0f, 0f,
                            withAlpha(indicatorColor, 190 + Math.round(110 * pulsePhase)));
                }
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
            if (useManualGlow) {
                drawProgressGlowFallback(canvas, thickness, sweep);
            }
            canvas.drawArc(arcRect, -90f, sweep, false, progressPaint);
            drawLeadingEdgeHighlight(canvas, thickness, progressFraction);
        }

        if (animationEnabled) {
            drawInnerParticleEffect(canvas, thickness);
            scheduleParticleFrame(canvas);
        }

        if (style == STYLE_MARKER) {
            float angle = (float) Math.toRadians(-90 + sweep);
            float cx = arcRect.centerX();
            float cy = arcRect.centerY();
            float outer = dpToPx(7);
            float inner = dpToPx(5);
            float markerRadius = arcRect.width() / 2f;
            float x = cx + (float) Math.cos(angle) * markerRadius;
            float y = cy + (float) Math.sin(angle) * markerRadius;
            canvas.drawCircle(x, y, outer, markerRingPaint);
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

        // Layer a few translucent circles instead of blurring a software shadow each frame.
        edgeGlowPaint.setColor(withAlpha(indicatorColor, 22));
        canvas.drawCircle(x, y, glowRadius * 1.75f, edgeGlowPaint);
        edgeGlowPaint.setColor(withAlpha(indicatorColor, 42));
        canvas.drawCircle(x, y, glowRadius * 1.35f, edgeGlowPaint);
        edgeGlowPaint.setColor(withAlpha(indicatorColor, 150));
        canvas.drawCircle(x, y, glowRadius, edgeGlowPaint);

        edgeCorePaint.setColor(Color.WHITE);
        canvas.drawCircle(x, y, coreRadius, edgeCorePaint);

        edgeParticlePaint.setColor(withAlpha(indicatorColor, 210));
        canvas.drawCircle(x - tangentX * (thickness * 0.58f), y - tangentY * (thickness * 0.58f),
                particleRadius * 1.45f, edgeParticlePaint);
        canvas.drawCircle(x - tangentX * (thickness * 1.02f), y - tangentY * (thickness * 1.02f),
                particleRadius, edgeParticlePaint);
    }

    private void drawProgressGlowFallback(@NonNull Canvas canvas, float thickness, float sweep) {
        float spread;
        int shadowAlpha;
        switch (style) {
            case STYLE_GLOW:
            case STYLE_GRADIENT_GLOW:
                spread = dpToPx(10f);
                shadowAlpha = 220;
                break;
            case STYLE_PULSE_LIGHT:
                spread = dpToPx(8f + 6f * pulsePhase);
                shadowAlpha = 120 + Math.round(70f * pulsePhase);
                break;
            case STYLE_PULSE_MEDIUM:
                spread = dpToPx(12f + 10f * pulsePhase);
                shadowAlpha = 160 + Math.round(90f * pulsePhase);
                break;
            case STYLE_PULSE_STRONG:
                spread = dpToPx(18f + 14f * pulsePhase);
                shadowAlpha = 190 + Math.round(110f * pulsePhase);
                break;
            default:
                return;
        }

        progressGlowPaint.setShader(null);
        progressGlowPaint.setColor(withAlpha(indicatorColor, Math.max(12, shadowAlpha / 8)));
        progressGlowPaint.setStrokeWidth(thickness + spread * 2f);
        canvas.drawArc(arcRect, -90f, sweep, false, progressGlowPaint);
        progressGlowPaint.setColor(withAlpha(indicatorColor, Math.max(20, shadowAlpha / 4)));
        progressGlowPaint.setStrokeWidth(thickness + spread);
        canvas.drawArc(arcRect, -90f, sweep, false, progressGlowPaint);
    }

    private void drawInnerParticleEffect(@NonNull Canvas canvas, float thickness) {
        if (pauseMode) {
            drawPauseBreathingGlow(canvas, thickness);
        } else if (periodActive) {
            drawPeriodRain(canvas, thickness);
        } else {
            drawCalmFloat(canvas, thickness);
        }
    }

    private void drawPauseBreathingGlow(@NonNull Canvas canvas, float thickness) {
        float innerRadius = (arcRect.width() / 2f) - (thickness * 0.95f);
        if (innerRadius <= 0f) {
            return;
        }
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        float clipRadius = innerRadius * 0.9f;
        long elapsed = Math.max(0L, SystemClock.uptimeMillis() - innerParticleStartTimeMs);
        float time = elapsed / 1000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(time * 0.9f);
        float radius = clipRadius * (0.48f + pulse * 0.06f);
        int accentAlpha = 26 + Math.round(pulse * 24f);
        int whiteAlpha = 18 + Math.round(pulse * 14f);

        calmParticlePaint.setStyle(Paint.Style.FILL);
        calmParticlePaint.setColor(withAlpha(indicatorColor, 12 + Math.round(pulse * 16f)));
        canvas.drawCircle(centerX, centerY, radius * 1.65f, calmParticlePaint);
        calmParticlePaint.setColor(withAlpha(indicatorColor, accentAlpha));
        canvas.drawCircle(centerX, centerY, radius, calmParticlePaint);

        calmParticlePaint.setColor(withAlpha(0xFFFFFFFF, whiteAlpha));
        canvas.drawCircle(centerX, centerY, radius * 0.68f, calmParticlePaint);
    }

    private void drawPeriodRain(@NonNull Canvas canvas, float thickness) {
        float clipRadius = getParticleClipRadius(thickness);
        if (clipRadius <= 0f) {
            return;
        }
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        long elapsed = Math.max(0L, SystemClock.uptimeMillis() - innerParticleStartTimeMs);
        int saveCount = canvas.save();
        innerRainClipPath.reset();
        innerRainClipPath.addCircle(centerX, centerY, clipRadius, Path.Direction.CW);
        canvas.clipPath(innerRainClipPath);

        for (int i = 0; i < PERIOD_PARTICLE_COUNT; i++) {
            float progress = (periodOffsets[i] + ((elapsed / 1000f) * periodSpeeds[i])) % 1f;
            float x = centerX - clipRadius + (periodLaneSeeds[i] * clipRadius * 2f)
                    + periodDriftFactors[i] * clipRadius;
            float y = centerY - clipRadius + (progress * clipRadius * 2f);
            float dropHeight = periodDropHeights[i];
            float dropWidth = dropHeight * 0.68f;
            int alpha = periodAlphas[i];
            periodDropPaint.setColor(withAlpha(0xFF7A1026, alpha));
            canvas.drawOval(x - dropWidth / 2f, y - dropHeight / 2f, x + dropWidth / 2f, y + dropHeight / 2f, periodDropPaint);
            periodDropPaint.setColor(withAlpha(0xFFB3203E, Math.min(255, alpha + 26)));
            canvas.drawCircle(x, y - (dropHeight * 0.2f), dropWidth * 0.28f, periodDropPaint);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawCalmFloat(@NonNull Canvas canvas, float thickness) {
        float clipRadius = getParticleClipRadius(thickness);
        if (clipRadius <= 0f) {
            return;
        }
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        long elapsed = Math.max(0L, SystemClock.uptimeMillis() - innerParticleStartTimeMs);
        int saveCount = canvas.save();
        innerRainClipPath.reset();
        innerRainClipPath.addCircle(centerX, centerY, clipRadius, Path.Direction.CW);
        canvas.clipPath(innerRainClipPath);

        for (int i = 0; i < CALM_PARTICLE_COUNT; i++) {
            float progress = (calmOffsets[i] + ((elapsed / 1000f) * calmSpeeds[i])) % 1f;
            float sway = calmSwayFactors[i] * clipRadius;
            float x = centerX - clipRadius + (calmLaneSeeds[i] * clipRadius * 2f)
                    + (float) Math.sin((elapsed / 700f) + i) * sway;
            float y = centerY + clipRadius - (progress * clipRadius * 2f);
            float particleRadius = calmRadii[i];

            calmParticlePaint.setColor(withAlpha(indicatorColor, calmGlowAlphas[i]));
            canvas.drawCircle(x, y, particleRadius * 1.6f, calmParticlePaint);

            calmParticlePaint.setColor(withAlpha(0xFFFFFFFF, calmCoreAlphas[i]));
            canvas.drawCircle(x, y, particleRadius, calmParticlePaint);
        }

        canvas.restoreToCount(saveCount);
    }

    private void scheduleParticleFrame(@NonNull Canvas canvas) {
        if (canvas.isHardwareAccelerated()) {
            postInvalidateOnAnimation();
        } else {
            postInvalidateDelayed(33L);
        }
    }

    private float getParticleClipRadius(float thickness) {
        // arcRect radius is the ring centerline; subtract half the stroke to reach its inner edge.
        return Math.max(0f, (arcRect.width() / 2f) - (thickness / 2f));
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

    private void ensureGradientShaders() {
        int width = getWidth();
        int height = getHeight();
        if (gradientShader != null && gradientGlowShader != null
                && gradientShaderColor == indicatorColor
                && gradientShaderWidth == width
                && gradientShaderHeight == height) {
            return;
        }
        float centerX = width / 2f;
        float centerY = height / 2f;
        gradientMatrix.setRotate(-90f, centerX, centerY);
        gradientShader = new SweepGradient(centerX, centerY,
                new int[]{withAlpha(indicatorColor, 60), indicatorColor, withAlpha(indicatorColor, 60)},
                new float[]{0f, 0.7f, 1f});
        gradientGlowShader = new SweepGradient(centerX, centerY,
                new int[]{withAlpha(indicatorColor, 50), indicatorColor, withAlpha(indicatorColor, 50)},
                new float[]{0f, 0.7f, 1f});
        gradientShader.setLocalMatrix(gradientMatrix);
        gradientGlowShader.setLocalMatrix(gradientMatrix);
        gradientShaderColor = indicatorColor;
        gradientShaderWidth = width;
        gradientShaderHeight = height;
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
