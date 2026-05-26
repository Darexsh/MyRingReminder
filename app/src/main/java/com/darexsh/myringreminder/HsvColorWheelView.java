package com.darexsh.myringreminder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class HsvColorWheelView extends View {

    public interface OnColorChangeListener {
        void onColorChanged(int color);
    }

    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap bitmap;
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
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float dx = x - centerX;
                float dy = y - centerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                int index = y * w + x;
                if (dist <= radius) {
                    float sat = dist / radius;
                    float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                    if (angle < 0f) {
                        angle += 360f;
                    }
                    pixels[index] = Color.HSVToColor(new float[]{angle, sat, 1f});
                } else {
                    pixels[index] = Color.TRANSPARENT;
                }
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0f, 0f, bitmapPaint);
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
