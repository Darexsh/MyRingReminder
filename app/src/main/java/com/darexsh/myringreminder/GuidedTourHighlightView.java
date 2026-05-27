package com.darexsh.myringreminder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

public class GuidedTourHighlightView extends View {

    private final Paint scrimPaint = new Paint();
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF targetRect = new RectF();
    private final int[] viewLocation = new int[2];
    private final int[] targetLocation = new int[2];
    private float cornerRadiusPx;
    private float paddingPx;
    private boolean hasTarget = false;

    public GuidedTourHighlightView(Context context) {
        super(context);
        init();
    }

    public GuidedTourHighlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GuidedTourHighlightView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        scrimPaint.setColor(0xB3000000);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        outlinePaint.setColor(0xCCFFFFFF);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dpToPx(2));
        paddingPx = dpToPx(12);
        cornerRadiusPx = dpToPx(16);
    }

    public void setTarget(View target) {
        if (target == null) {
            hasTarget = false;
            invalidate();
            return;
        }
        if (!target.isShown() || target.getWidth() <= 0 || target.getHeight() <= 0) {
            hasTarget = false;
            invalidate();
            return;
        }

        getLocationOnScreen(viewLocation);
        target.getLocationOnScreen(targetLocation);
        float left = targetLocation[0] - viewLocation[0];
        float top = targetLocation[1] - viewLocation[1];
        float right = left + target.getWidth();
        float bottom = top + target.getHeight();
        targetRect.set(left, top, right, bottom);
        targetRect.inset(-paddingPx, -paddingPx);
        hasTarget = true;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        canvas.drawRect(0, 0, getWidth(), getHeight(), scrimPaint);
        if (hasTarget) {
            canvas.drawRoundRect(targetRect, cornerRadiusPx, cornerRadiusPx, clearPaint);
            canvas.drawRoundRect(targetRect, cornerRadiusPx, cornerRadiusPx, outlinePaint);
        }
        canvas.restoreToCount(save);
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }
}
