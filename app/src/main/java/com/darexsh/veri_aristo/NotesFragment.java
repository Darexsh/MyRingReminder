package com.darexsh.veri_aristo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.PorterDuff;
import android.graphics.Color;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// NotesFragment allows users to create and save personal notes
public class NotesFragment extends Fragment {

    private EditText editTextNotes;
    private MaterialButton btnSave;
    private TextView tvCharCount;
    private TextView tvLastSaved;
    private TextView tvNoteDate;

    private final Handler autoSaveHandler = new Handler(Looper.getMainLooper());
    private Runnable autoSaveRunnable;
    private boolean isProgrammaticNoteUpdate = false;
    private static final Pattern DATE_HEADER_LINE_PATTERN = Pattern.compile("(?m)^(?:\\d{2}\\.\\d{2}\\.\\d{4}|\\p{L}+,\\s\\d{2}\\.\\d{2}\\.\\d{4})$");

    private static final String PREF_NAME = "notes_prefs";
    private static final String NOTES_KEY = "user_notes";
    private static final String NOTES_LAST_SAVED_KEY = "notes_last_saved";
    private static final long AUTOSAVE_DELAY_MS = 800;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        editTextNotes = view.findViewById(R.id.editText_notes);
        btnSave = view.findViewById(R.id.btn_save_notes);
        MaterialButton btnClear = view.findViewById(R.id.btn_clear_notes);
        tvCharCount = view.findViewById(R.id.tv_char_count);
        tvLastSaved = view.findViewById(R.id.tv_last_saved);
        tvNoteDate = view.findViewById(R.id.tv_notes_date);
        ImageButton btnClose = view.findViewById(R.id.btn_close_notes);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        SharedViewModel viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);
        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                ButtonColorHelper.applyPrimaryColor(btnSave, color);
                btnClose.setImageTintList(null);
                btnClose.setColorFilter(color, PorterDuff.Mode.SRC_IN);
                btnClose.setAlpha(1f);
            }
        });

        // Load saved notes when the fragment is created
        loadNotes();
        updateNoteDate();

        autoSaveRunnable = () -> saveNotes(false);

        editTextNotes.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isProgrammaticNoteUpdate) {
                    return;
                }
                applyDateHeaderStyling(s);
                updateCharCount(s.length());
                scheduleAutoSave();
            }
        });

        // Set up the save button to store notes in SharedPreferences
        btnSave.setOnClickListener(v -> saveNotes(true));

        btnClear.setOnClickListener(v -> {
            AlertDialog confirmDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.notes_delete_title)
                .setMessage(R.string.notes_delete_message)
                .setPositiveButton(R.string.notes_delete, (dialog, which) -> {
                    editTextNotes.setText("");
                    saveNotes(true);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
            Integer buttonColor = viewModel.getButtonColor().getValue();
            if (buttonColor != null) {
                Button positive = confirmDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positive != null) {
                    positive.setTextColor(buttonColor);
                }
                Button negative = confirmDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                if (negative != null) {
                    negative.setTextColor(buttonColor);
                }
            }
        });

        btnClose.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoSaveHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateNoteDate();
        applyDailyDateHeaderIfNeeded();
        applyDateHeaderStyling(editTextNotes.getText());
    }

    // Load saved notes from SharedPreferences
    private void loadNotes() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedNotes = prefs.getString(NOTES_KEY, "");
        long lastSaved = prefs.getLong(NOTES_LAST_SAVED_KEY, 0);
        String notesWithHeader = ensureCurrentDateHeader(savedNotes);

        isProgrammaticNoteUpdate = true;
        editTextNotes.setText(notesWithHeader);
        isProgrammaticNoteUpdate = false;
        applyDateHeaderStyling(editTextNotes.getText());
        updateCharCount(notesWithHeader.length());

        if (!notesWithHeader.equals(savedNotes)) {
            long now = System.currentTimeMillis();
            prefs.edit()
                    .putString(NOTES_KEY, notesWithHeader)
                    .putLong(NOTES_LAST_SAVED_KEY, now)
                    .apply();
            lastSaved = now;
        }
        updateLastSavedText(lastSaved);
    }

    // Save notes to SharedPreferences
    private void saveNotes(boolean showToast) {
        String notes = editTextNotes.getText().toString();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        prefs.edit()
                .putString(NOTES_KEY, notes)
                .putLong(NOTES_LAST_SAVED_KEY, now)
                .apply();
        updateLastSavedText(now);

        if (showToast) {
            Toast.makeText(requireContext(), R.string.notes_saved_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void scheduleAutoSave() {
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
        autoSaveHandler.postDelayed(autoSaveRunnable, AUTOSAVE_DELAY_MS);
    }

    private void updateCharCount(int count) {
        tvCharCount.setText(getString(R.string.notes_char_count, count));
    }

    private void updateLastSavedText(long timestamp) {
        if (timestamp <= 0) {
            tvLastSaved.setText(R.string.notes_not_saved);
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("HH:mm, dd.MM.yyyy", Locale.getDefault());
        tvLastSaved.setText(getString(R.string.notes_last_saved, format.format(new Date(timestamp))));
    }

    private void updateNoteDate() {
        SimpleDateFormat format = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault());
        tvNoteDate.setText(format.format(new Date()));
    }

    private void applyDailyDateHeaderIfNeeded() {
        String current = editTextNotes.getText().toString();
        String updated = ensureCurrentDateHeader(current);
        if (updated.equals(current)) {
            applyDateHeaderStyling(editTextNotes.getText());
            return;
        }

        isProgrammaticNoteUpdate = true;
        editTextNotes.setText(updated);
        int cursorPosition = Math.min(updated.length(), getTodayHeader().length() + 2);
        editTextNotes.setSelection(cursorPosition);
        isProgrammaticNoteUpdate = false;

        updateCharCount(updated.length());
        applyDateHeaderStyling(editTextNotes.getText());
        saveNotes(false);
    }

    private String ensureCurrentDateHeader(String noteText) {
        String safeText = noteText == null ? "" : noteText;
        String leadingTrimmed = trimLeadingNewLines(safeText);
        String todayHeader = getTodayHeader();

        String firstLine = leadingTrimmed;
        int firstNewLine = leadingTrimmed.indexOf('\n');
        if (firstNewLine >= 0) {
            firstLine = leadingTrimmed.substring(0, firstNewLine);
        }

        String firstLineTrimmed = firstLine.trim();
        if (firstLineTrimmed.equals(todayHeader) || firstLineTrimmed.equals(getTodayHeaderWithWeekday())) {
            return leadingTrimmed;
        }
        if (leadingTrimmed.trim().isEmpty()) {
            return todayHeader + "\n\n";
        }
        return todayHeader + "\n\n" + leadingTrimmed;
    }

    private String getTodayHeader() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        return format.format(new Date());
    }

    private String getTodayHeaderWithWeekday() {
        SimpleDateFormat format = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.getDefault());
        return format.format(new Date());
    }

    private String trimLeadingNewLines(String value) {
        int index = 0;
        while (index < value.length()) {
            char c = value.charAt(index);
            if (c != '\n' && c != '\r') {
                break;
            }
            index++;
        }
        return value.substring(index);
    }

    private void applyDateHeaderStyling(Editable editable) {
        if (editable == null || editable.length() == 0) {
            return;
        }

        DateHeaderColorSpan[] existingColorSpans = editable.getSpans(0, editable.length(), DateHeaderColorSpan.class);
        for (DateHeaderColorSpan span : existingColorSpans) {
            editable.removeSpan(span);
        }
        DateHeaderSizeSpan[] existingSizeSpans = editable.getSpans(0, editable.length(), DateHeaderSizeSpan.class);
        for (DateHeaderSizeSpan span : existingSizeSpans) {
            editable.removeSpan(span);
        }

        Matcher matcher = DATE_HEADER_LINE_PATTERN.matcher(editable.toString());
        while (matcher.find()) {
            editable.setSpan(new DateHeaderColorSpan(Color.GRAY), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            editable.setSpan(new DateHeaderSizeSpan(0.88f), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static class DateHeaderColorSpan extends ForegroundColorSpan {
        DateHeaderColorSpan(int color) {
            super(color);
        }
    }

    private static class DateHeaderSizeSpan extends RelativeSizeSpan {
        DateHeaderSizeSpan(float proportion) {
            super(proportion);
        }
    }
}
