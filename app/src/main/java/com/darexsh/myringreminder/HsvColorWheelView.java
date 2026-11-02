package com.darexsh.myringreminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HsvColorWheelView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    private final Paint huePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint saturationPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float centerX;
    private float centerY;
    private float radius;
    private float hue = 0f;
    private float saturation = 0f;
    private OnColorChangeListener listener;

    public HsvColorWheelView(Context context) {
        super(context);
        init();
    }

    public HsvColorWheelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HsvColorWheelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        selectorPaint.setStyle(Paint.Style.FILL);
        selectorPaint.setColor(Color.WHITE);
        selectorStrokePaint.setStyle(Paint.Style.STROKE);
        selectorStrokePaint.setColor(Color.BLACK);
        selectorStrokePaint.setStrokeWidth(2f);
        huePaint.setStyle(Paint.Style.FILL);
        saturationPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnColorChangeListener(@Nullable OnColorChangeListener listener) {
        this.listener = listener;
    }

    public void setColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hue = hsv[0];
        saturation = hsv[1];
        invalidate();
    }

    public int getSelectedColor() {
        return Color.HSVToColor(new float[]{hue, saturation, 1f});
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_width, int old_height) {
        super.onSizeChanged(w, h, old_width, old_height);
        int size = Math.min(w, h);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = size / 2f;
        if (radius <= 0f) {
            huePaint.setShader(null);
            saturationPaint.setShader(null);
            return;
        }
        huePaint.setShader(new SweepGradient(centerX, centerY, new int[]{
                Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN,
                Color.BLUE, Color.MAGENTA, Color.RED
        }, null));
        saturationPaint.setShader(new RadialGradient(centerX, centerY, radius,
                0xFFFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (radius > 0f) {
            canvas.drawCircle(centerX, centerY, radius, huePaint);
            canvas.drawCircle(centerX, centerY, radius, saturationPaint);
        }

        float angleRad = (float) Math.toRadians(hue);
        float selectorRadius = saturation * radius;
        float x = centerX + (float) Math.cos(angleRad) * selectorRadius;
        float y = centerY + (float) Math.sin(angleRad) * selectorRadius;
        canvas.drawCircle(x, y, 10f, selectorPaint);
        canvas.drawCircle(x, y, 10f, selectorStrokePaint);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN
                && event.getAction() != MotionEvent.ACTION_MOVE) {
            return super.onTouchEvent(event);
        }

        float dx = event.getX() - centerX;
        float dy = event.getY() - centerY;
        if (radius <= 0f) {
            return true;
        }
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > radius) {
            dx = dx / dist * radius;
            dy = dy / dist * radius;
            dist = radius;
        }

        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0f) {
            angle += 360f;
        }

        hue = angle;
        saturation = dist / radius;
        int color = getSelectedColor();
        if (listener != null) {
            listener.onColorChanged(color);
        }
        invalidate();
        return true;
    }
}
