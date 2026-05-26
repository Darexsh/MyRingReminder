package com.darexsh.myringreminder;

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
import android.widget.LinearLayout;
import android.content.res.ColorStateList;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CalendarFragment extends Fragment {

    private MaterialCalendarView calendarView;
    private SharedViewModel viewModel;
    private View legendWearView;
    private View legendRingFreeView;
    private View legendRemovalView;
    private View legendInsertionView;
    private View legendTablesRow;
    private View legendPeriodColumn;
    private View legendCalendarColumn;
    private View legendOriginalRow;
    private View legendWearOriginalView;
    private View legendRingFreeOriginalView;
    private View legendRemovalOriginalView;
    private View legendInsertionOriginalView;
    private TextView monthSelectorView;
    private com.google.android.material.button.MaterialButton todayButton;
    private static final int CALENDAR_ALPHA = 127;
    private static final int LEGEND_ALPHA = 255;
    private int[] colorValues;
    private String[] colorLabels;
    private boolean calendarUpdateScheduled = false;
    private Set<CalendarDay> currentPeriodEntryAllowedDays = new HashSet<>();
    private List<RingFreeWindow> currentRingFreeWindows = new ArrayList<>();
    private static final int PERIOD_ENTRY_TOLERANCE_DAYS = 4;
    private AlertDialog activePeriodEntryDialog;
    private View activePeriodEntryDialogView;

    private static class RingFreeWindow {
        final CalendarDay start;
        final CalendarDay end;

        RingFreeWindow(CalendarDay start, CalendarDay end) {
            this.start = start;
            this.end = end;
        }
    }

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
        ImageButton prevMonthButton = view.findViewById(R.id.btn_calendar_prev);
        ImageButton nextMonthButton = view.findViewById(R.id.btn_calendar_next);
        todayButton = view.findViewById(R.id.btn_calendar_today);
        calendarView.setTopbarVisible(false);
        calendarView.setDynamicHeightEnabled(true);
        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        calendarView.setDateTextAppearance(R.style.Theme_MyRingReminder);
        calendarView.post(() -> tintCalendarArrows(resolveCalendarHeaderColor()));
        calendarView.setOnMonthChangedListener((widget, date) -> {
            updateMonthSelectorText();
            updateTodayButtonVisibility();
            updateLegendModeForCurrentMonth();
        });
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (currentPeriodEntryAllowedDays.contains(date)) {
                showPeriodEntryDialog(date);
            } else {
                android.widget.Toast.makeText(
                        requireContext(),
                        R.string.period_modal_only_ring_free_toast,
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                calendarView.clearSelection();
            }
        });
        legendWearView = view.findViewById(R.id.view_legend_wear);
        legendRingFreeView = view.findViewById(R.id.view_legend_ring_free);
        legendRemovalView = view.findViewById(R.id.view_legend_removal);
        legendInsertionView = view.findViewById(R.id.view_legend_insertion);
        legendTablesRow = view.findViewById(R.id.legend_tables_row);
        legendPeriodColumn = view.findViewById(R.id.legend_period_column);
        legendCalendarColumn = view.findViewById(R.id.legend_calendar_column);
        legendOriginalRow = view.findViewById(R.id.legend_original_row);
        legendWearOriginalView = view.findViewById(R.id.view_legend_wear_original);
        legendRingFreeOriginalView = view.findViewById(R.id.view_legend_ring_free_original);
        legendRemovalOriginalView = view.findViewById(R.id.view_legend_removal_original);
        legendInsertionOriginalView = view.findViewById(R.id.view_legend_insertion_original);
        if (monthSelectorView != null) {
            monthSelectorView.setOnClickListener(v -> showMonthYearPickerDialog());
        }
        if (prevMonthButton != null) {
            prevMonthButton.setOnClickListener(v -> stepMonth(-1));
        }
        if (nextMonthButton != null) {
            nextMonthButton.setOnClickListener(v -> stepMonth(1));
        }
        if (todayButton != null) {
            todayButton.setOnClickListener(v -> jumpToToday());
        }

        if (legendWearView != null) {
            legendWearView.setOnClickListener(v -> showLegendColorDialog(
                    R.string.settings_calendar_wear_color_dialog_title,
                    R.string.settings_calendar_wear_color_custom_title,
                    getCalendarWearColor(),
                    viewModel::setCalendarWearColor
            ));
        }
        if (legendWearOriginalView != null) {
            legendWearOriginalView.setOnClickListener(v -> showLegendColorDialog(
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
        if (legendRingFreeOriginalView != null) {
            legendRingFreeOriginalView.setOnClickListener(v -> showLegendColorDialog(
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
        if (legendRemovalOriginalView != null) {
            legendRemovalOriginalView.setOnClickListener(v -> showLegendColorDialog(
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
        if (legendInsertionOriginalView != null) {
            legendInsertionOriginalView.setOnClickListener(v -> showLegendColorDialog(
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
        viewModel.getButtonColor().observe(getViewLifecycleOwner(), value -> applyTodayButtonStyle());
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
        updateTodayButtonVisibility();
        updateLegendModeForCurrentMonth();
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
        applyPeriodTrackingDecorators();
        currentRingFreeWindows = buildRingFreeWindows(snapshot.ringFreeDays);
        currentPeriodEntryAllowedDays = buildAllowedPeriodEntryDays(currentRingFreeWindows, PERIOD_ENTRY_TOLERANCE_DAYS);
    }

    private void applyPeriodTrackingDecorators() {
        Map<String, PeriodDayEntry> allEntries = viewModel.getRepository().getAllPeriodDayEntries();
        if (allEntries == null || allEntries.isEmpty()) {
            return;
        }

        Set<CalendarDay> lightDays = new HashSet<>();
        Set<CalendarDay> mediumDays = new HashSet<>();
        Set<CalendarDay> heavyDays = new HashSet<>();
        Set<CalendarDay> bloodDays = new HashSet<>();
        Set<CalendarDay> painDays = new HashSet<>();
        Set<CalendarDay> illnessDays = new HashSet<>();
        Set<CalendarDay> startDays = new HashSet<>();
        Set<CalendarDay> endDays = new HashSet<>();

        for (Map.Entry<String, PeriodDayEntry> item : allEntries.entrySet()) {
            PeriodDayEntry entry = item.getValue();
            if (entry == null || !entry.isPeriodDay()) {
                continue;
            }
            CalendarDay day = parseCalendarDayKey(item.getKey());
            if (day == null) {
                continue;
            }
            bloodDays.add(day);
            if (entry.getIntensity() == BleedingIntensity.LIGHT) {
                lightDays.add(day);
            } else if (entry.getIntensity() == BleedingIntensity.MEDIUM) {
                mediumDays.add(day);
            } else if (entry.getIntensity() == BleedingIntensity.HEAVY) {
                heavyDays.add(day);
            }
            if (entry.hasPain()) {
                painDays.add(day);
            }
            if (entry.hasIllness()) {
                illnessDays.add(day);
            }
            if (entry.isStart()) {
                startDays.add(day);
            }
            if (entry.isEnd()) {
                endDays.add(day);
            }
        }

        if (!lightDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(0x88EF9A9A, lightDays));
        }
        if (!mediumDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(0x99EF5350, mediumDays));
        }
        if (!heavyDays.isEmpty()) {
            calendarView.addDecorator(new SetDayDecorator(0xCCB71C1C, heavyDays));
        }

        if (!bloodDays.isEmpty()) {
            calendarView.addDecorator(new DaySpanDecorator(bloodDays, new IndicatorDotSpan(0, 0xFFFF5252, 5.6f)));
        }
        if (!painDays.isEmpty()) {
            calendarView.addDecorator(new DaySpanDecorator(painDays, new IndicatorDotSpan(-20f, 0xFFFFB300, 5.6f)));
        }
        if (!illnessDays.isEmpty()) {
            calendarView.addDecorator(new DaySpanDecorator(illnessDays, new IndicatorDotSpan(20f, 0xFF40C4FF, 5.6f)));
        }
        if (!startDays.isEmpty()) {
            calendarView.addDecorator(new DaySpanDecorator(startDays, new CornerLabelSpan("S", -27f, 0xFFFFFFFF)));
        }
        if (!endDays.isEmpty()) {
            calendarView.addDecorator(new DaySpanDecorator(endDays, new CornerLabelSpan("E", 27f, 0xFFFFFFFF)));
        }
    }

    @Nullable
    private CalendarDay parseCalendarDayKey(String key) {
        if (key == null || key.length() != 10) {
            return null;
        }
        try {
            int year = Integer.parseInt(key.substring(0, 4));
            int month = Integer.parseInt(key.substring(5, 7));
            int day = Integer.parseInt(key.substring(8, 10));
            return CalendarDay.from(year, month, day);
        } catch (Exception ignored) {
            return null;
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
        applyLegendColor(legendWearOriginalView, getCalendarWearColor());
        applyLegendColor(legendRingFreeOriginalView, getCalendarRingFreeColor());
        applyLegendColor(legendRemovalOriginalView, getCalendarRemovalColor());
        applyLegendColor(legendInsertionOriginalView, getCalendarInsertionColor());
        applyTodayButtonStyle();
    }

    private void applyTodayButtonStyle() {
        if (todayButton == null || viewModel == null) {
            return;
        }
        Integer color = viewModel.getButtonColor().getValue();
        if (color == null) {
            return;
        }
        ButtonColorHelper.applyPrimaryColor(todayButton, color);
        todayButton.setTextColor(Color.WHITE);
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

    private static class DaySpanDecorator implements DayViewDecorator {
        private final Set<CalendarDay> days;
        private final LineBackgroundSpan span;

        DaySpanDecorator(Set<CalendarDay> days, LineBackgroundSpan span) {
            this.days = days;
            this.span = span;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return days.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(span);
        }
    }

    private static class IndicatorDotSpan implements LineBackgroundSpan {
        private final float xOffset;
        private final int color;
        private final float radius;

        IndicatorDotSpan(float xOffset, int color, float radius) {
            this.xOffset = xOffset;
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void drawBackground(@NonNull android.graphics.Canvas canvas,
                                   @NonNull Paint paint,
                                   int left,
                                   int right,
                                   int top,
                                   int baseline,
                                   int bottom,
                                   @NonNull CharSequence text,
                                   int start,
                                   int end,
                                   int lineNum) {
            int oldColor = paint.getColor();
            Paint.Style oldStyle = paint.getStyle();
            float oldStroke = paint.getStrokeWidth();
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            float cx = (left + right) / 2f + xOffset;
            float cy = bottom + 2f;
            canvas.drawCircle(cx, cy, radius + 2.6f, outlinePaint(0xF0111111));
            canvas.drawCircle(cx, cy, radius + 1.2f, outlinePaint(0xCCFFFFFF));
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(oldColor);
            paint.setStyle(oldStyle);
            paint.setStrokeWidth(oldStroke);
        }
    }

    private static class CornerLabelSpan implements LineBackgroundSpan {
        private final String label;
        private final float xOffset;
        private final int color;

        CornerLabelSpan(String label, float xOffset, int color) {
            this.label = label;
            this.xOffset = xOffset;
            this.color = color;
        }

        @Override
        public void drawBackground(@NonNull android.graphics.Canvas canvas,
                                   @NonNull Paint paint,
                                   int left,
                                   int right,
                                   int top,
                                   int baseline,
                                   int bottom,
                                   @NonNull CharSequence text,
                                   int start,
                                   int end,
                                   int lineNum) {
            int oldColor = paint.getColor();
            float oldTextSize = paint.getTextSize();
            Paint.Align oldAlign = paint.getTextAlign();
            Paint.Style oldStyle = paint.getStyle();
            paint.setColor(color);
            paint.setTextSize(oldTextSize * 0.78f);
            paint.setTextAlign(Paint.Align.CENTER);
            float x = (left + right) / 2f + xOffset;
            float y = top + 21f;
            float circleRadius = paint.getTextSize() * 0.58f;
            canvas.drawCircle(x, y - paint.getTextSize() * 0.34f, circleRadius, outlinePaint(0xB0111111));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawText(label, x, y, paint);
            paint.setColor(oldColor);
            paint.setTextSize(oldTextSize);
            paint.setTextAlign(oldAlign);
            paint.setStyle(oldStyle);
        }
    }


    private static Paint outlinePaint(int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setStyle(Paint.Style.FILL);
        return p;
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

    private void jumpToToday() {
        Calendar today = Calendar.getInstance();
        CalendarDay todayDay = toCalendarDay(today);
        calendarView.setCurrentDate(todayDay);
        updateMonthSelectorText();
        updateTodayButtonVisibility();
    }

    private void updateTodayButtonVisibility() {
        if (todayButton == null || calendarView == null) {
            return;
        }
        CalendarDay currentDay = calendarView.getCurrentDate();
        Calendar now = Calendar.getInstance();
        boolean sameMonth = currentDay != null
                && currentDay.getYear() == now.get(Calendar.YEAR)
                && currentDay.getMonth() == now.get(Calendar.MONTH) + 1;
        todayButton.setVisibility(sameMonth ? View.GONE : View.VISIBLE);
    }

    private void updateLegendModeForCurrentMonth() {
        if (legendTablesRow == null || legendPeriodColumn == null || legendCalendarColumn == null
                || legendOriginalRow == null || calendarView == null || viewModel == null) {
            return;
        }
        LinearLayout legendRow = (LinearLayout) legendTablesRow;
        CalendarDay current = calendarView.getCurrentDate();
        if (current == null) {
            return;
        }
        boolean hasPeriodData = hasPeriodEntryInMonth(current.getYear(), current.getMonth());
        legendPeriodColumn.setVisibility(hasPeriodData ? View.VISIBLE : View.GONE);
        legendOriginalRow.setVisibility(hasPeriodData ? View.GONE : View.VISIBLE);
        legendTablesRow.setVisibility(hasPeriodData ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) legendCalendarColumn.getLayoutParams();
        if (hasPeriodData) {
            legendRow.setGravity(android.view.Gravity.NO_GRAVITY);
            params.width = 0;
            params.weight = 1f;
        }
        legendCalendarColumn.setLayoutParams(params);
    }

    private boolean hasPeriodEntryInMonth(int year, int monthOneBased) {
        Map<String, PeriodDayEntry> entries = viewModel.getRepository().getAllPeriodDayEntries();
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, PeriodDayEntry> item : entries.entrySet()) {
            PeriodDayEntry entry = item.getValue();
            if (entry == null || !entry.isPeriodDay()) {
                continue;
            }
            CalendarDay day = parseCalendarDayKey(item.getKey());
            if (day == null) {
                continue;
            }
            if (day.getYear() == year && day.getMonth() == monthOneBased) {
                return true;
            }
        }
        return false;
    }

    private void showPeriodEntryDialog(@NonNull CalendarDay day) {
        SettingsRepository repository = viewModel.getRepository();
        Calendar selectedDay = Calendar.getInstance();
        selectedDay.set(day.getYear(), day.getMonth() - 1, day.getDay(), 0, 0, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);
        String dateKey = repository.buildPeriodDateKey(selectedDay);
        PeriodDayEntry existing = repository.getPeriodDayEntry(dateKey);
        boolean firstEntryInRingFreeWeek = isFirstPeriodEntryInCurrentRingFreeWeek(repository, day, dateKey);
        boolean hasAnotherStartInRingFreeWeek = hasOtherStartInCurrentRingFreeWeek(repository, day, dateKey);

        View layout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_period_entry, null);
        activePeriodEntryDialogView = layout;
        SwitchMaterial periodDaySwitch = layout.findViewById(R.id.switch_period_day);
        TextView intensityTitle = layout.findViewById(R.id.tv_period_intensity_title);
        TextView symptomsTitle = layout.findViewById(R.id.tv_period_symptoms_title);
        TextView markersTitle = layout.findViewById(R.id.tv_period_markers_title);
        ChipGroup intensityGroup = layout.findViewById(R.id.chip_group_intensity);
        Chip light = layout.findViewById(R.id.chip_intensity_light);
        Chip medium = layout.findViewById(R.id.chip_intensity_medium);
        Chip heavy = layout.findViewById(R.id.chip_intensity_heavy);
        Chip painChip = layout.findViewById(R.id.chip_pain);
        Chip illnessChip = layout.findViewById(R.id.chip_illness);
        Chip startChip = layout.findViewById(R.id.chip_start);
        Chip endChip = layout.findViewById(R.id.chip_end);
        MaterialButton btnDelete = layout.findViewById(R.id.btn_period_delete);
        MaterialButton btnCancel = layout.findViewById(R.id.btn_period_cancel);
        MaterialButton btnSave = layout.findViewById(R.id.btn_period_save);
        stylePeriodIntensityChips(light, medium, heavy);

        final boolean autoStartSuggestionEligible = existing == null && firstEntryInRingFreeWeek;
        if (existing != null) {
            periodDaySwitch.setChecked(existing.isPeriodDay());
            painChip.setChecked(existing.hasPain());
            illnessChip.setChecked(existing.hasIllness());
            startChip.setChecked(existing.isStart());
            endChip.setChecked(existing.isEnd());
            if (existing.getIntensity() == BleedingIntensity.LIGHT) {
                light.setChecked(true);
            } else if (existing.getIntensity() == BleedingIntensity.MEDIUM) {
                medium.setChecked(true);
            } else if (existing.getIntensity() == BleedingIntensity.HEAVY) {
                heavy.setChecked(true);
            }
        }

        Runnable toggleEnabledState = () -> {
            boolean enabled = periodDaySwitch.isChecked();
            intensityTitle.setEnabled(enabled);
            symptomsTitle.setEnabled(enabled);
            markersTitle.setEnabled(enabled);
            light.setEnabled(enabled);
            medium.setEnabled(enabled);
            heavy.setEnabled(enabled);
            painChip.setEnabled(enabled);
            illnessChip.setEnabled(enabled);
            startChip.setEnabled(enabled);
            endChip.setEnabled(enabled);
            if (!enabled) {
                intensityGroup.clearCheck();
                painChip.setChecked(false);
                illnessChip.setChecked(false);
                startChip.setChecked(false);
                endChip.setChecked(false);
                startChip.setEnabled(false);
                endChip.setEnabled(false);
            } else {
                applyMarkerExclusionState(startChip, endChip, hasAnotherStartInRingFreeWeek);
            }
        };
        startChip.setOnCheckedChangeListener((buttonView, isChecked) ->
                applyMarkerExclusionState(startChip, endChip, hasAnotherStartInRingFreeWeek));
        endChip.setOnCheckedChangeListener((buttonView, isChecked) ->
                applyMarkerExclusionState(startChip, endChip, hasAnotherStartInRingFreeWeek));
        periodDaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked
                    && autoStartSuggestionEligible
                    && !startChip.isChecked()
                    && !endChip.isChecked()) {
                startChip.setChecked(true);
            }
            toggleEnabledState.run();
        });
        toggleEnabledState.run();

        String dateLabel = String.format(Locale.getDefault(), "%02d.%02d.%04d", day.getDay(), day.getMonth(), day.getYear());
        Integer accentColor = viewModel != null ? viewModel.getButtonColor().getValue() : null;
        if (accentColor != null) {
            btnSave.setTextColor(accentColor);
            btnCancel.setTextColor(accentColor);
            btnDelete.setTextColor(accentColor);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.period_modal_title_for_date, dateLabel))
                .setView(layout)
                .create();

        btnSave.setOnClickListener(v -> {
            boolean isPeriodDay = periodDaySwitch.isChecked();
            BleedingIntensity intensity = null;
            int checkedId = intensityGroup.getCheckedChipId();
            if (checkedId == light.getId()) {
                intensity = BleedingIntensity.LIGHT;
            } else if (checkedId == medium.getId()) {
                intensity = BleedingIntensity.MEDIUM;
            } else if (checkedId == heavy.getId()) {
                intensity = BleedingIntensity.HEAVY;
            }

            if (isPeriodDay && intensity == null) {
                android.widget.Toast.makeText(
                        requireContext(),
                        R.string.period_modal_validation_intensity_required,
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }

            boolean saved = repository.savePeriodDayEntry(
                    selectedDay,
                    isPeriodDay,
                    intensity,
                    painChip.isChecked(),
                    illnessChip.isChecked(),
                    startChip.isChecked(),
                    endChip.isChecked()
            );
            if (!saved) {
                android.widget.Toast.makeText(
                        requireContext(),
                        R.string.period_modal_save_failed,
                        android.widget.Toast.LENGTH_SHORT
                ).show();
                return;
            }
            android.widget.Toast.makeText(
                    requireContext(),
                    R.string.period_modal_saved_toast,
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            dialog.dismiss();
            calendarView.clearSelection();
            requestCalendarUpdate();
        });

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            calendarView.clearSelection();
        });

        btnDelete.setOnClickListener(v -> {
            repository.deletePeriodDayEntry(dateKey);
            android.widget.Toast.makeText(
                    requireContext(),
                    R.string.period_modal_deleted_toast,
                    android.widget.Toast.LENGTH_SHORT
            ).show();
            dialog.dismiss();
            calendarView.clearSelection();
            requestCalendarUpdate();
        });
        btnDelete.setEnabled(existing != null);
        btnDelete.setAlpha(existing != null ? 1f : 0.4f);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_app_info_dialog);
        }
        activePeriodEntryDialog = dialog;
        dialog.setOnDismissListener(d -> {
            calendarView.clearSelection();
            activePeriodEntryDialog = null;
            activePeriodEntryDialogView = null;
        });
        dialog.show();
    }

    public boolean ensurePeriodDialogVisibleForTour() {
        if (!isAdded()) {
            return false;
        }
        if (activePeriodEntryDialog != null && activePeriodEntryDialog.isShowing()) {
            return true;
        }
        CalendarDay targetDay = null;
        CalendarDay current = calendarView != null ? calendarView.getCurrentDate() : null;
        if (current != null && currentPeriodEntryAllowedDays.contains(current)) {
            targetDay = current;
        } else if (!currentPeriodEntryAllowedDays.isEmpty()) {
            targetDay = currentPeriodEntryAllowedDays.iterator().next();
        }
        if (targetDay == null) {
            return false;
        }
        showPeriodEntryDialog(targetDay);
        return true;
    }

    public void dismissPeriodDialogForTour() {
        if (activePeriodEntryDialog != null && activePeriodEntryDialog.isShowing()) {
            activePeriodEntryDialog.dismiss();
        }
    }

    @Nullable
    public ViewGroup getPeriodDialogTourHost() {
        if (activePeriodEntryDialogView instanceof ViewGroup) {
            return (ViewGroup) activePeriodEntryDialogView;
        }
        return null;
    }

    @Nullable
    public View findTourTargetView(int targetViewId) {
        if (activePeriodEntryDialogView != null) {
            View inDialog = activePeriodEntryDialogView.findViewById(targetViewId);
            if (inDialog != null) {
                return inDialog;
            }
        }
        View root = getView();
        if (root == null) {
            return null;
        }
        return root.findViewById(targetViewId);
    }

    private void applyMarkerExclusionState(Chip startChip, Chip endChip, boolean lockStartSelection) {
        if (startChip.isChecked()) {
            endChip.setChecked(false);
            endChip.setEnabled(false);
            startChip.setEnabled(true);
            return;
        }
        if (endChip.isChecked()) {
            startChip.setChecked(false);
            startChip.setEnabled(false);
            endChip.setEnabled(true);
            return;
        }
        startChip.setEnabled(!lockStartSelection);
        endChip.setEnabled(true);
    }

    private boolean hasOtherStartInCurrentRingFreeWeek(SettingsRepository repository,
                                                       CalendarDay selectedDay,
                                                       String selectedDateKey) {
        RingFreeWindow window = findWindowForDay(selectedDay);
        if (window == null) {
            return false;
        }
        Map<String, PeriodDayEntry> allEntries = repository.getAllPeriodDayEntries();
        for (CalendarDay ringFreeDay : daysForWindowWithTolerance(window, PERIOD_ENTRY_TOLERANCE_DAYS)) {
            Calendar day = Calendar.getInstance();
            day.set(ringFreeDay.getYear(), ringFreeDay.getMonth() - 1, ringFreeDay.getDay(), 0, 0, 0);
            day.set(Calendar.MILLISECOND, 0);
            String dateKey = repository.buildPeriodDateKey(day);
            if (selectedDateKey.equals(dateKey)) {
                continue;
            }
            PeriodDayEntry entry = allEntries.get(dateKey);
            if (entry != null && entry.isPeriodDay() && entry.isStart()) {
                return true;
            }
        }
        return false;
    }

    private void stylePeriodIntensityChips(Chip light, Chip medium, Chip heavy) {
        styleIntensityChip(light, 0x33FF8A80, 0xFFE57373);
        styleIntensityChip(medium, 0x33EF5350, 0xFFEF5350);
        styleIntensityChip(heavy, 0x33C62828, 0xFFC62828);
    }

    private void styleIntensityChip(Chip chip, int uncheckedBg, int checkedBg) {
        int uncheckedText = 0xFFD8D8D8;
        int checkedText = 0xFFFFFFFF;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        int[] bgColors = new int[]{checkedBg, uncheckedBg};
        int[] textColors = new int[]{checkedText, uncheckedText};
        chip.setChipBackgroundColor(ColorStateList.valueOf(uncheckedBg));
        chip.setChipBackgroundColor(new ColorStateList(states, bgColors));
        chip.setTextColor(new ColorStateList(states, textColors));
    }

    private boolean isFirstPeriodEntryInCurrentRingFreeWeek(SettingsRepository repository,
                                                            CalendarDay selectedDay,
                                                            String selectedDateKey) {
        RingFreeWindow window = findWindowForDay(selectedDay);
        if (window == null) {
            return false;
        }
        Map<String, PeriodDayEntry> allEntries = repository.getAllPeriodDayEntries();
        for (CalendarDay ringFreeDay : daysForWindowWithTolerance(window, PERIOD_ENTRY_TOLERANCE_DAYS)) {
            Calendar day = Calendar.getInstance();
            day.set(ringFreeDay.getYear(), ringFreeDay.getMonth() - 1, ringFreeDay.getDay(), 0, 0, 0);
            day.set(Calendar.MILLISECOND, 0);
            String dateKey = repository.buildPeriodDateKey(day);
            if (selectedDateKey.equals(dateKey)) {
                continue;
            }
            PeriodDayEntry entry = allEntries.get(dateKey);
            if (entry != null && entry.isPeriodDay()) {
                return false;
            }
        }
        return true;
    }

    private List<RingFreeWindow> buildRingFreeWindows(Set<CalendarDay> ringFreeDays) {
        if (ringFreeDays == null || ringFreeDays.isEmpty()) {
            return new ArrayList<>();
        }
        List<CalendarDay> sorted = new ArrayList<>(ringFreeDays);
        Collections.sort(sorted, Comparator.comparingLong(this::toDayMillis));
        List<RingFreeWindow> windows = new ArrayList<>();
        CalendarDay runStart = sorted.get(0);
        CalendarDay runEnd = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            CalendarDay current = sorted.get(i);
            long diffDays = (toDayMillis(current) - toDayMillis(runEnd)) / (24L * 60L * 60L * 1000L);
            if (diffDays == 1L) {
                runEnd = current;
            } else {
                windows.add(new RingFreeWindow(runStart, runEnd));
                runStart = current;
                runEnd = current;
            }
        }
        windows.add(new RingFreeWindow(runStart, runEnd));
        return windows;
    }

    private Set<CalendarDay> buildAllowedPeriodEntryDays(List<RingFreeWindow> windows, int toleranceDays) {
        Set<CalendarDay> allowed = new HashSet<>();
        for (RingFreeWindow window : windows) {
            allowed.addAll(daysForWindowWithTolerance(window, toleranceDays));
        }
        return allowed;
    }

    private Set<CalendarDay> daysForWindowWithTolerance(RingFreeWindow window, int toleranceDays) {
        Set<CalendarDay> days = new HashSet<>();
        Calendar start = fromCalendarDay(window.start);
        Calendar end = fromCalendarDay(window.end);
        start.add(Calendar.DAY_OF_MONTH, -toleranceDays);
        end.add(Calendar.DAY_OF_MONTH, toleranceDays);
        while (!start.after(end)) {
            days.add(toCalendarDay(start));
            start.add(Calendar.DAY_OF_MONTH, 1);
        }
        return days;
    }

    private RingFreeWindow findWindowForDay(CalendarDay day) {
        if (currentRingFreeWindows == null || currentRingFreeWindows.isEmpty()) {
            return null;
        }
        long target = toDayMillis(day);
        for (RingFreeWindow window : currentRingFreeWindows) {
            Calendar start = fromCalendarDay(window.start);
            Calendar end = fromCalendarDay(window.end);
            start.add(Calendar.DAY_OF_MONTH, -PERIOD_ENTRY_TOLERANCE_DAYS);
            end.add(Calendar.DAY_OF_MONTH, PERIOD_ENTRY_TOLERANCE_DAYS);
            long startMillis = start.getTimeInMillis();
            long endMillis = end.getTimeInMillis();
            if (target >= startMillis && target <= endMillis) {
                return window;
            }
        }
        return null;
    }

    private Calendar fromCalendarDay(CalendarDay day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(day.getYear(), day.getMonth() - 1, day.getDay(), 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private long toDayMillis(CalendarDay day) {
        return fromCalendarDay(day).getTimeInMillis();
    }

}
