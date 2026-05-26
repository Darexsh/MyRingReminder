package com.darexsh.myringreminder;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class GuidedTourOverlay extends FrameLayout {

    private final GuidedTourHighlightView highlightView;
    private final MaterialCardView cardView;
    private final TextView titleView;
    private final TextView bodyView;
    private final MaterialButton nextButton;
    private final MaterialButton skipButton;
    private Runnable onNextListener;
    private Runnable onSkipListener;
    private Runnable onFinishListener;

    public GuidedTourOverlay(@NonNull Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.view_guided_tour_overlay, this, true);
        highlightView = findViewById(R.id.tour_highlight);
        cardView = findViewById(R.id.tour_card);
        titleView = findViewById(R.id.tour_title);
        bodyView = findViewById(R.id.tour_body);
        nextButton = findViewById(R.id.tour_next);
        skipButton = findViewById(R.id.tour_skip);

        setClickable(true);
        setFocusable(true);

        nextButton.setOnClickListener(v -> {
            if (onNextListener != null) {
                onNextListener.run();
            }
        });
        skipButton.setOnClickListener(v -> {
            if (onSkipListener != null) {
                onSkipListener.run();
            } else {
                finish();
            }
        });
    }

    public void setOnNextListener(Runnable listener) {
        this.onNextListener = listener;
    }

    public void setOnSkipListener(Runnable listener) {
        this.onSkipListener = listener;
    }

    public void setStep(int titleRes, int bodyRes, boolean isLast, View target) {
        titleView.setText(titleRes);
        bodyView.setText(bodyRes);
        nextButton.setText(isLast ? R.string.tour_finish : R.string.tour_next);
        highlightView.setTarget(target);
        positionCardNearTarget(target);
    }

    public void setButtonColor(int color) {
        ButtonColorHelper.applyPrimaryColor(nextButton, color);
        ButtonColorHelper.applyPrimaryColor(skipButton, color);
        nextButton.setTextColor(Color.WHITE);
        skipButton.setTextColor(Color.WHITE);
    }

    public void setOnFinishListener(Runnable listener) {
        this.onFinishListener = listener;
    }

    public void finish() {
        if (onFinishListener != null) {
            onFinishListener.run();
        }
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) {
            parent.removeView(this);
        }
    }

    private void positionCardNearTarget(View target) {
        if (target == null) {
            return;
        }

        post(() -> {
            Rect targetRect = new Rect();
            Rect overlayRect = new Rect();
            if (!target.getGlobalVisibleRect(targetRect) || !getGlobalVisibleRect(overlayRect)) {
                return;
            }

            int cardHeight = cardView.getHeight();
            int overlayHeight = overlayRect.height();
            int margin = dpToPx(16);
            int gap = dpToPx(12);

            int targetTop = targetRect.top - overlayRect.top;
            int targetBottom = targetRect.bottom - overlayRect.top;

            int spaceAbove = targetTop - margin;
            int spaceBelow = overlayHeight - targetBottom - margin;

            int desiredTop;
            if (spaceBelow >= cardHeight + gap || spaceBelow >= spaceAbove) {
                desiredTop = targetBottom + gap;
            } else {
                desiredTop = targetTop - cardHeight - gap;
            }

            int maxTop = overlayHeight - cardHeight - margin;
            if (desiredTop < margin) {
                desiredTop = margin;
            } else if (desiredTop > maxTop) {
                desiredTop = maxTop;
            }

            LayoutParams params = (LayoutParams) cardView.getLayoutParams();
            params.topMargin = desiredTop;
            params.leftMargin = margin;
            params.rightMargin = margin;
            params.gravity = android.view.Gravity.TOP;
            cardView.setLayoutParams(params);
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
