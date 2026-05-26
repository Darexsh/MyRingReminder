package com.darexsh.myringreminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

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
    private final RectF arcRect = new RectF();
    private final Matrix gradientMatrix = new Matrix();

    private int style = STYLE_CLASSIC;
    private int indicatorColor = SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
    private final int trackColor = Color.parseColor("#E0E0E0");
    private int max = 1;
    private int progress = 0;
    private float pulsePhase = 0f;

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
        invalidate();
    }

    public void setProgress(int progress) {
        this.progress = Math.max(0, progress);
        invalidate();
    }

    public void setPulsePhase(float phase) {
        this.pulsePhase = phase;
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

        float progressFraction = Math.max(0f, Math.min(1f, (float) progress / (float) max));
        if (style == STYLE_SEGMENTED) {
            trackPaint.setStrokeWidth(thickness);
            progressPaint.setStrokeWidth(thickness);
            drawSegmented(canvas, thickness, progressFraction);
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
            @SuppressLint("DrawAllocation") Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            ring.setStyle(Paint.Style.FILL);
            ring.setColor(Color.WHITE);
            canvas.drawCircle(x, y, outer, ring);
            canvas.drawCircle(x, y, inner, markerPaint);
        }
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

        int filled = Math.round(segments * fraction);
        for (int i = 0; i < filled; i++) {
            float segStart = start + i * (sweep + gap);
            canvas.drawArc(arcRect, segStart, sweep, false, progressPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }
}
