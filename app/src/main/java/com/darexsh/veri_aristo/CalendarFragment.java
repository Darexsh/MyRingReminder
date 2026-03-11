package com.darexsh.veri_aristo;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.text.style.LineBackgroundSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private SharedViewModel viewModel;
    private View legendWearView;
    private View legendRingFreeView;
    private View legendRemovalView;
    private View legendInsertionView;
    private TextView monthSelectorView;
    private ImageButton prevMonthButton;
    private ImageButton nextMonthButton;
    private static final int CALENDAR_ALPHA = 127;
    private static final int LEGEND_ALPHA = 255;
    private int[] colorValues;
    private String[] colorLabels;
    private boolean calendarUpdateScheduled = false;

    private interface ColorConsumer {
        void accept(int color);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate calendar layout
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);
        calendarView = view.findViewById(R.id.calendarView);
        monthSelectorView = view.findViewById(R.id.tv_calendar_month_selector);
        prevMonthButton = view.findViewById(R.id.btn_calendar_prev);
        nextMonthButton = view.findViewById(R.id.btn_calendar_next);
        calendarView.setTopbarVisible(false);
        calendarView.setDateTextAppearance(R.style.Theme_Veri_Aristo);
        calendarView.post(() -> tintCalendarArrows(resolveCalendarHeaderColor()));
        calendarView.setOnMonthChangedListener((widget, date) -> updateMonthSelectorText());
        legendWearView = view.findViewById(R.id.view_legend_wear);
        legendRingFreeView = view.findViewById(R.id.view_legend_ring_free);
        legendRemovalView = view.findViewById(R.id.view_legend_removal);
        legendInsertionView = view.findViewById(R.id.view_legend_insertion);
        if (monthSelectorView != null) {
            monthSelectorView.setOnClickListener(v -> showMonthYearPickerDialog());
        }
        if (prevMonthButton != null) {
            prevMonthButton.setOnClickListener(v -> stepMonth(-1));
        }
        if (nextMonthButton != null) {
            nextMonthButton.setOnClickListener(v -> stepMonth(1));
        }

        if (legendWearView != null) {
            legendWearView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_wear_color_dialog_title,
                    R.string.settings_calendar_wear_color_custom_title,
                    getCalendarWearColor(),
                    viewModel::setCalendarWearColor
            ));
        }
        if (legendRingFreeView != null) {
            legendRingFreeView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_ring_free_color_dialog_title,
                    R.string.settings_calendar_ring_free_color_custom_title,
                    getCalendarRingFreeColor(),
                    viewModel::setCalendarRingFreeColor
            ));
        }
        if (legendRemovalView != null) {
            legendRemovalView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_removal_color_dialog_title,
                    R.string.settings_calendar_removal_color_custom_title,
                    getCalendarRemovalColor(),
                    viewModel::setCalendarRemovalColor
            ));
        }
        if (legendInsertionView != null) {
            legendInsertionView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_insertion_color_dialog_title,
                    R.string.settings_calendar_insertion_color_custom_title,
                    getCalendarInsertionColor(),
                    viewModel::setCalendarInsertionColor
            ));
        }

        // Initialize shared ViewModel
        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        // Set up calendar view properties
        setupObservers();
        updateCalendar();

        return view;
    }

    // Observe cycle data changes and update calendar
    private void setupObservers() {
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getStartDate().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getCalendarPastAmount().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getCalendarPastUnit().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getCalendarFutureAmount().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getCalendarFutureUnit().observe(getViewLifecycleOwner(), value -> requestCalendarUpdate());
        viewModel.getCalendarWearColor().observe(getViewLifecycleOwner(), value -> {
            requestCalendarUpdate();
            updateLegendColors();
        });
        viewModel.getCalendarRingFreeColor().observe(getViewLifecycleOwner(), value -> {
            requestCalendarUpdate();
            updateLegendColors();
        });
        viewModel.getCalendarRemovalColor().observe(getViewLifecycleOwner(), value -> {
            requestCalendarUpdate();
            updateLegendColors();
        });
        viewModel.getCalendarInsertionColor().observe(getViewLifecycleOwner(), value -> {
            requestCalendarUpdate();
            updateLegendColors();
        });
    }

    private void requestCalendarUpdate() {
        if (calendarView == null || calendarUpdateScheduled) {
            return;
        }
        calendarUpdateScheduled = true;
        calendarView.post(() -> {
            calendarUpdateScheduled = false;
            if (!isAdded() || calendarView == null) {
                return;
            }
            updateCalendar();
        });
    }

    // Refresh calendar with updated cycle data
    private void updateCalendar() {
        Calendar startDate = getStartDate();
        int cycleLength = getCycleLength();
        int pastMonths = getCalendarPastMonths();
        int futureMonths = getCalendarFutureMonths();
        calendarView.removeDecorators();
        setupCalendarDecorators(startDate, cycleLength, pastMonths, futureMonths);
        calendarView.addDecorator(new TodayBorderDecorator()); // Add today border decorator
        updateLegendColors();
        updateMonthSelectorText();
    }

    // Retrieve start date
    private Calendar getStartDate() {
        if (viewModel.getStartDate().getValue() != null) {
            return viewModel.getStartDate().getValue();
        }
        return Calendar.getInstance();
    }

    // Retrieve cycle length
    private int getCycleLength() {
        if (viewModel.getCycleLength().getValue() != null) {
            return viewModel.getCycleLength().getValue();
        }
        return 21;
    }

    private int getCalendarPastMonths() {
        Integer amount = viewModel.getCalendarPastAmount().getValue();
        String unit = viewModel.getCalendarPastUnit().getValue();
        if (amount != null && unit != null) {
            return "years".equals(unit) ? amount * 12 : amount;
        }
        return 12;
    }

    private int getCalendarFutureMonths() {
        Integer amount = viewModel.getCalendarFutureAmount().getValue();
        String unit = viewModel.getCalendarFutureUnit().getValue();
        if (amount != null && unit != null) {
            return "years".equals(unit) ? amount * 12 : amount;
        }
        return 24;
    }

    // Setup calendar decorators
    private void setupCalendarDecorators(Calendar startDate, int cycleLength, int pastMonths, int futureMonths) {
        CalendarRenderCache.Snapshot snapshot =
                CalendarRenderCache.getSnapshotIfMatches(startDate, cycleLength, pastMonths, futureMonths);
        if (snapshot == null) {
            snapshot = CalendarRenderCache.computeSnapshot(
                    viewModel.getRepository(),
                    startDate,
                    cycleLength,
                    pastMonths,
                    futureMonths
            );
            CalendarRenderCache.setSnapshot(snapshot);
        }

        int wearColor = ColorUtils.setAlphaComponent(getCalendarWearColor(), CALENDAR_ALPHA);
        int ringFreeColor = ColorUtils.setAlphaComponent(getCalendarRingFreeColor(), CALENDAR_ALPHA);
        int removalColor = ColorUtils.setAlphaComponent(getCalendarRemovalColor(), CALENDAR_ALPHA);
        int insertionColor = ColorUtils.setAlphaComponent(getCalendarInsertionColor(), CALENDAR_ALPHA);

        if (!snapshot.wearDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(wearColor, snapshot.wearDays));
        }
        if (!snapshot.ringFreeDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(ringFreeColor, snapshot.ringFreeDays));
        }
        if (!snapshot.removalDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(removalColor, snapshot.removalDays));
        }
        if (!snapshot.insertionDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(insertionColor, snapshot.insertionDays));
        }
    }

    private void tintCalendarArrows(int color) {
        tintImageViews(calendarView, color);
    }

    private void updateLegendColors() {
        applyLegendColor(legendWearView, getCalendarWearColor());
        applyLegendColor(legendRingFreeView, getCalendarRingFreeColor());
        applyLegendColor(legendRemovalView, getCalendarRemovalColor());
        applyLegendColor(legendInsertionView, getCalendarInsertionColor());
    }

    private void showLegendColorDialog(int titleResId, int customTitleResId, int selectedColor, ColorConsumer onSelect) {
        ensureColorOptionsLoaded();
        int selectedIndex = getColorIndex(selectedColor);
        final int[] pendingColor = new int[]{selectedColor};

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_button_colors);
        com.google.android.material.button.MaterialButton customButton = content.findViewById(R.id.btn_custom_color);
        com.google.android.material.button.MaterialButton cancelButton = content.findViewById(R.id.btn_cancel_color);
        android.widget.TextView widgetNote = content.findViewById(R.id.tv_color_dialog_note);
        if (widgetNote != null) {
            widgetNote.setVisibility(View.GONE);
        }
        Integer buttonColor = viewModel != null ? viewModel.getButtonColor().getValue() : null;
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(customButton, buttonColor);
            ButtonColorHelper.applyPrimaryColor(cancelButton, buttonColor);
            customButton.setTextColor(Color.WHITE);
            cancelButton.setTextColor(Color.WHITE);
        }

        android.widget.ListAdapter adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                R.layout.dialog_button_color_item,
                android.R.id.text1,
                colorLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View swatch = view.findViewById(R.id.view_color_swatch);
                if (swatch != null) {
                    swatch.setBackgroundColor(colorValues[position]);
                }
                return view;
            }
        };

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .create();

        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        if (selectedIndex >= 0) {
            listView.setItemChecked(selectedIndex, true);
        }
        listView.setOnItemClickListener((parent, view, position, id) -> {
            pendingColor[0] = colorValues[position];
            onSelect.accept(pendingColor[0]);
            dialog.dismiss();
        });

        customButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCustomLegendColorDialog(customTitleResId, pendingColor[0], onSelect);
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        applyDialogButtonColors(dialog);
    }

    private void showCustomLegendColorDialog(int titleResId, int initialColor, ColorConsumer onSelect) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_custom, null);
        HsvColorWheelView colorWheel = content.findViewById(R.id.color_wheel);
        View preview = content.findViewById(R.id.view_color_preview);
        final int[] pendingColor = new int[]{initialColor};
        preview.setBackgroundTintList(ColorStateList.valueOf(initialColor));
        colorWheel.setColor(initialColor);
        colorWheel.setOnColorChangeListener(color -> {
            pendingColor[0] = color;
            preview.setBackgroundTintList(ColorStateList.valueOf(color));
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> onSelect.accept(pendingColor[0]))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void applyDialogButtonColors(@Nullable AlertDialog dialog) {
        if (dialog == null || viewModel == null) {
            return;
        }
        Integer color = viewModel.getButtonColor().getValue();
        if (color == null) {
            return;
        }
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setTextColor(color);
        }
        Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negative != null) {
            negative.setTextColor(color);
        }
        Button neutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutral != null) {
            neutral.setTextColor(color);
        }
    }

    private void ensureColorOptionsLoaded() {
        if (colorValues == null || colorLabels == null) {
            colorValues = getResources().getIntArray(R.array.settings_button_color_values);
            colorLabels = getResources().getStringArray(R.array.settings_button_color_labels);
        }
    }

    private int getColorIndex(int color) {
        ensureColorOptionsLoaded();
        for (int i = 0; i < colorValues.length; i++) {
            if (colorValues[i] == color) {
                return i;
            }
        }
        return 0;
    }

    private void applyLegendColor(View view, int color) {
        if (view == null) {
            return;
        }
        int tintedColor = ColorUtils.setAlphaComponent(color, LEGEND_ALPHA);
        view.setBackgroundTintList(ColorStateList.valueOf(tintedColor));
    }

    private int getCalendarWearColor() {
        Integer value = viewModel.getCalendarWearColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_WEAR_COLOR;
    }

    private int getCalendarRingFreeColor() {
        Integer value = viewModel.getCalendarRingFreeColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_RING_FREE_COLOR;
    }

    private int getCalendarRemovalColor() {
        Integer value = viewModel.getCalendarRemovalColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_REMOVAL_COLOR;
    }

    private int getCalendarInsertionColor() {
        Integer value = viewModel.getCalendarInsertionColor().getValue();
        return value != null ? value : SettingsRepository.DEFAULT_CALENDAR_INSERTION_COLOR;
    }

    private void tintImageViews(View view, int color) {
        if (view instanceof ImageView) {
            ((ImageView) view).setColorFilter(color, PorterDuff.Mode.SRC_IN);
            return;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tintImageViews(group.getChildAt(i), color);
            }
        }
    }

    private int resolveThemeColor(int attr) {
        TypedValue typedValue = new TypedValue();
        if (requireContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.resourceId != 0) {
                return requireContext().getColor(typedValue.resourceId);
            }
            return typedValue.data;
        }
        return Color.WHITE;
    }

    private int resolveCalendarHeaderColor() {
        TextView header = findLargestTextView(calendarView);
        if (header != null) {
            return header.getCurrentTextColor();
        }
        return resolveThemeColor(com.google.android.material.R.attr.colorOnSurface);
    }

    private TextView findLargestTextView(View root) {
        if (root instanceof TextView) {
            return (TextView) root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            TextView best = null;
            float bestSize = 0f;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView candidate = findLargestTextView(group.getChildAt(i));
                if (candidate != null) {
                    float size = candidate.getTextSize();
                    if (size > bestSize) {
                        bestSize = size;
                        best = candidate;
                    }
                }
            }
            return best;
        }
        return null;
    }

    private static class SetDayDecorator implements DayViewDecorator {
        private final int color;
        private final Set<CalendarDay> days;

        public SetDayDecorator(int color, Set<CalendarDay> days) {
            this.color = color;
            this.days = days;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            GradientDrawable drawable = createCircleDrawable(color);
            view.setBackgroundDrawable(drawable);
        }
    }

    // Today border decorator
    private static class TodayBorderDecorator implements DayViewDecorator {
        private final CalendarDay today = CalendarDay.today();

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return day.equals(today);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan((LineBackgroundSpan) (canvas, paint, left, right, top, baseline, bottom, text, start, end, lineNum) -> {
                int cx = (left + right) / 2;
                int cy = (top + bottom) / 2;
                int radius = Math.min(right - left, bottom - top) / 2 + 40; // slightly bigger than background
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(5);
                paint.setColor(Color.LTGRAY);
                canvas.drawCircle(cx, cy, radius, paint);
            });
        }
    }

    private static GradientDrawable createCircleDrawable(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        drawable.setSize(40, 40);
        return drawable;
    }

    private CalendarDay toCalendarDay(Calendar calendar) {
        return CalendarDay.from(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
        );
    }

    private void updateMonthSelectorText() {
        if (monthSelectorView == null) {
            return;
        }
        CalendarDay currentDate = calendarView.getCurrentDate();
        Calendar calendar = Calendar.getInstance();
        if (currentDate != null) {
            calendar.set(currentDate.getYear(), currentDate.getMonth() - 1, 1);
        }
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String text = formatter.format(calendar.getTime());
        monthSelectorView.setText(text);
    }

    private void showMonthYearPickerDialog() {
        CalendarDay currentDay = calendarView.getCurrentDate();
        Calendar current = Calendar.getInstance();
        if (currentDay != null) {
            current.set(currentDay.getYear(), currentDay.getMonth() - 1, 1);
        }

        NumberPicker monthPicker = new NumberPicker(requireContext());
        NumberPicker yearPicker = new NumberPicker(requireContext());

        String[] monthNames = DateFormatSymbols.getInstance(Locale.getDefault()).getMonths();
        String[] displayMonths = new String[12];
        System.arraycopy(monthNames, 0, displayMonths, 0, 12);

        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(displayMonths);
        monthPicker.setValue(current.get(Calendar.MONTH));

        yearPicker.setMinValue(1900);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(current.get(Calendar.YEAR));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);

        android.widget.LinearLayout.LayoutParams params =
                new android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        monthPicker.setLayoutParams(params);
        yearPicker.setLayoutParams(params);
        layout.addView(monthPicker);
        layout.addView(yearPicker);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.calendar_month_picker_title)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (d, which) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, yearPicker.getValue());
                    selected.set(Calendar.MONTH, monthPicker.getValue());
                    selected.set(Calendar.DAY_OF_MONTH, 1);
                    selected.set(Calendar.HOUR_OF_DAY, 0);
                    selected.set(Calendar.MINUTE, 0);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    calendarView.setCurrentDate(toCalendarDay(selected));
                    updateMonthSelectorText();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void stepMonth(int delta) {
        CalendarDay currentDay = calendarView.getCurrentDate();
        Calendar target = Calendar.getInstance();
        if (currentDay != null) {
            target.set(currentDay.getYear(), currentDay.getMonth() - 1, 1);
        } else {
            target.set(Calendar.DAY_OF_MONTH, 1);
        }
        target.add(Calendar.MONTH, delta);
        target.set(Calendar.DAY_OF_MONTH, 1);
        calendarView.setCurrentDate(toCalendarDay(target));
        updateMonthSelectorText();
    }
}
