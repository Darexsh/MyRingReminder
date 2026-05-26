package com.darexsh.myringreminder;

import android.content.res.ColorStateList;
import com.google.android.material.button.MaterialButton;

public final class ButtonColorHelper {

    private ButtonColorHelper() {
    }

    public static void applyPrimaryColor(MaterialButton button, int color) {
        if (button == null) {
            return;
        }
        button.setBackgroundTintList(ColorStateList.valueOf(color));
    }
}
