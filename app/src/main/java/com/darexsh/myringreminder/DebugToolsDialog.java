package com.darexsh.myringreminder;

import android.app.AlertDialog;
import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public final class DebugToolsDialog {

    public interface Listener {
        void onDebugToolsChanged(boolean enabled);
    }

    private DebugToolsDialog() {
    }

    public static AlertDialog show(Context context, SettingsRepository repository, Listener listener) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        TextView message = new TextView(context);
        message.setText(R.string.debug_tools_desc);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        layout.addView(message);

        SwitchMaterial toggle = new SwitchMaterial(context);
        toggle.setText(R.string.debug_tools_enable);
        toggle.setChecked(repository.isDebugToolsEnabled());
        layout.addView(toggle);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.debug_tools_title)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_app_info_dialog);
        }

        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            repository.setDebugToolsEnabled(isChecked);
            if (listener != null) {
                listener.onDebugToolsChanged(isChecked);
            }
        });

        return dialog;
    }
}
