package com.darexsh.myringreminder;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.TypedValue;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.graphics.ColorUtils;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import androidx.core.content.ContextCompat;

// CyclesFragment displays a history of cycle events (insertions/removals) in a card format
public class CyclesFragment extends Fragment {

    private SharedViewModel viewModel;
    private LinearLayout cycleContainer;
    private TextView emptyView;
    private TextView summaryCountView;
    private boolean sortNewestFirst = true;

    public CyclesFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cycles, container, false);

        // Initialize UI components
        cycleContainer = view.findViewById(R.id.cycle_container);
        emptyView = view.findViewById(R.id.tv_cycles_empty);
        summaryCountView = view.findViewById(R.id.tv_cycles_summary_count);
        TextView titleView = view.findViewById(R.id.tv_history_title);
        TextView sortLabel = view.findViewById(R.id.tv_cycles_sort_label);
        MaterialButton rebuildHistoryButton = view.findViewById(R.id.btn_rebuild_history);
        MaterialButton clearHistoryButton = view.findViewById(R.id.btn_clear_history);
        ChipGroup sortGroup = view.findViewById(R.id.chip_group_cycles_sort);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                ButtonColorHelper.applyPrimaryColor(rebuildHistoryButton, color);
                if (titleView != null) {
                    titleView.setTextColor(color);
                }
                if (sortLabel != null) {
                    sortLabel.setTextColor(color);
                }
                if (cycleContainer != null) {
                    displayCycleHistory(cycleContainer);
                }
            }
        });
        viewModel.getCalendarWearColor().observe(getViewLifecycleOwner(), color -> {
            if (cycleContainer != null) {
                displayCycleHistory(cycleContainer);
            }
        });
        viewModel.getCalendarRingFreeColor().observe(getViewLifecycleOwner(), color -> {
            if (cycleContainer != null) {
                displayCycleHistory(cycleContainer);
            }
        });

        // Load and display cycle history
        displayCycleHistory(cycleContainer);
        if (sortGroup != null) {
            sortGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
                sortNewestFirst = checkedId != R.id.chip_cycles_sort_oldest;
                displayCycleHistory(cycleContainer);
            });
        }

        rebuildHistoryButton.setOnClickListener(v -> showRebuildHistoryDialog());

        // Set up clear history button
        clearHistoryButton.setOnClickListener(v -> {
            viewModel.getRepository().clearCycleHistory(); // Clear the cycle history from repository
            cycleContainer.removeAllViews();        // Clear the UI
            displayCycleHistory(cycleContainer);    // Reload empty history
        });

        return view;
    }

    private void showRebuildHistoryDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.cycles_rebuild_history_title)
                .setMessage(R.string.cycles_rebuild_history_message)
                .setPositiveButton(R.string.cycles_rebuild_history_confirm, (d, which) -> {
                    rebuildCycleHistory();
                    displayCycleHistory(cycleContainer);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_app_info_dialog);
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
    }

    private void rebuildCycleHistory() {
        SettingsRepository repository = viewModel.getRepository();
        repository.setCycleHistoryCleared(false);
        Calendar startDate = repository.getStartDate();
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        int cycleLength = repository.getCycleLength();
        Calendar now = DebugTimeProvider.now(repository);

        List<Cycle> rebuilt = new ArrayList<>();
        Calendar cycleStart = (Calendar) startDate.clone();
        int maxCycles = 240;
        int count = 0;

        while (count < maxCycles) {
            int delayDays = repository.getCycleDelayDays(cycleStart.getTimeInMillis());
            int ringFreeDays = repository.getRingFreeDaysForCycle(cycleStart.getTimeInMillis());

            Calendar removalDate = (Calendar) cycleStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);

            Calendar reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            if (!now.before(removalDate)) {
                rebuilt.add(new Cycle(
                        cycleStart.getTimeInMillis(),
                        removalDate.getTimeInMillis(),
                        CycleType.INSERTION
                ));
            }
            if (!now.before(reinsertionDate)) {
                rebuilt.add(new Cycle(
                        removalDate.getTimeInMillis(),
                        reinsertionDate.getTimeInMillis(),
                        CycleType.REMOVAL
                ));
            }

            if (now.before(reinsertionDate)) {
                break;
            }

            cycleStart = (Calendar) reinsertionDate.clone();
            count++;
        }

        repository.saveCycleHistory(rebuilt);
    }

    // Display the cycle history in the provided LinearLayout
    private void displayCycleHistory(LinearLayout cycleContainer) {
        cycleContainer.removeAllViews(); // Clear existing views
        SettingsRepository repository = viewModel.getRepository();
        List<Cycle> cycleHistory = repository.getCycleHistory();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat monthHeaderFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        // Filter out invalid cycles and deduce by date+endDate+type
        List<Cycle> validCycles = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (Cycle cycle : cycleHistory) {
            if (cycle.getType() == null) {
                continue;
            }
            String key = cycle.getDateMillis() + ":" + cycle.getEndDateMillis() + ":" + cycle.getType().name();
            if (seen.add(key)) {
                validCycles.add(cycle);
            }
        }

        // Update repository if invalid or duplicate cycles were removed
        if (validCycles.size() < cycleHistory.size()) {
            viewModel.getRepository().saveCycleHistory(validCycles);
        }

        updateSummary(validCycles);

        Map<Long, Long> cycleStartByRemovalMillis = new HashMap<>();
        Set<Long> seamlessInsertionStarts = new HashSet<>();
        for (Cycle cycle : validCycles) {
            if (CycleType.INSERTION == cycle.getType() && cycle.getEndDateMillis() > 0) {
                cycleStartByRemovalMillis.put(cycle.getEndDateMillis(), cycle.getDateMillis());
            }
        }
        for (Cycle cycle : validCycles) {
            if (CycleType.REMOVAL != cycle.getType()) {
                continue;
            }
            Long sourceCycleStartMillisValue = cycleStartByRemovalMillis.get(cycle.getDateMillis());
            long sourceCycleStartMillis = sourceCycleStartMillisValue != null
                    ? sourceCycleStartMillisValue
                    : 0L;
            if (sourceCycleStartMillis > 0
                    && viewModel.getRepository().getRingFreeDaysForCycle(sourceCycleStartMillis) == 0
                    && cycle.getEndDateMillis() > 0) {
                seamlessInsertionStarts.add(cycle.getEndDateMillis());
            }
        }

        // Sort cycles by date in descending order (newest first)
        validCycles.sort((c1, c2) -> sortNewestFirst
                ? Long.compare(c2.getDateMillis(), c1.getDateMillis())
                : Long.compare(c1.getDateMillis(), c2.getDateMillis()));

        if (validCycles.isEmpty()) {
            if (emptyView != null) {
                emptyView.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        String lastMonthHeader = null;
        for (Cycle cycle : validCycles) {
            Calendar cycleDay = Calendar.getInstance();
            cycleDay.setTimeInMillis(cycle.getDateMillis());
            String currentMonthHeader = monthHeaderFormat.format(cycleDay.getTime());
            if (!currentMonthHeader.equals(lastMonthHeader)) {
                cycleContainer.addView(createMonthHeaderView(currentMonthHeader));
                lastMonthHeader = currentMonthHeader;
            }

            CardView cardView = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 16);
            cardView.setLayoutParams(cardParams);   // Set layout parameters for the card
            cardView.setCardElevation(0f);
            cardView.setRadius(24);
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            cardView.setUseCompatPadding(false);
            cardView.setPreventCornerOverlap(true);

            LinearLayout cardShell = new LinearLayout(requireContext());
            cardShell.setOrientation(LinearLayout.HORIZONTAL);
            cardShell.setGravity(Gravity.CENTER_VERTICAL);
            cardShell.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            cardShell.setBackgroundResource(R.drawable.bg_app_info_dialog);
            int horizontalPadding = dpToPx(18);
            int verticalPadding = dpToPx(12);
            cardShell.setPadding(0, 0, 0, 0);

            CycleCardPresentation presentation = buildCyclePresentation(
                    cycle,
                    cycleStartByRemovalMillis,
                    seamlessInsertionStarts,
                    dateFormat
            );
            cardShell.setBackground(createHistoryCardBackground(presentation.accentColor));

            LinearLayout textColumn = new LinearLayout(requireContext());
            textColumn.setOrientation(LinearLayout.VERTICAL);
            textColumn.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            textColumn.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

            LinearLayout headerRow = new LinearLayout(requireContext());
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
            headerRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            TextView dateTextView = new TextView(requireContext());
            dateTextView.setTextSize(18);
            dateTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            dateTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            dateTextView.setText(presentation.titleText);
            headerRow.addView(dateTextView);

            View badgeView = createBadgeView(presentation.badgeText, presentation.accentColor);
            headerRow.addView(badgeView);
            textColumn.addView(headerRow);

            TextView statusTextView = new TextView(requireContext());
            statusTextView.setTextSize(14);
            statusTextView.setPadding(0, dpToPx(presentation.statusTopPaddingDp), 0, 0);
            statusTextView.setText(presentation.statusText);
            statusTextView.setTextColor(presentation.statusColor);
            textColumn.addView(statusTextView);

            String specialCaseText = presentation.detailText;
            if (specialCaseText != null) {
                TextView specialCaseTextView = new TextView(requireContext());
                specialCaseTextView.setTextSize(13);
                specialCaseTextView.setTextColor(presentation.detailColor);
                specialCaseTextView.setPadding(0, dpToPx(presentation.detailTopPaddingDp), 0, 0);
                specialCaseTextView.setText(specialCaseText);
                textColumn.addView(specialCaseTextView);
            }

            cardShell.addView(textColumn);
            cardView.addView(cardShell);
            cycleContainer.addView(cardView);
        }
    }

    @NonNull
    private CycleCardPresentation buildCyclePresentation(@NonNull Cycle cycle,
                                                         @NonNull Map<Long, Long> cycleStartByRemovalMillis,
                                                         @NonNull Set<Long> seamlessInsertionStarts,
                                                         @NonNull SimpleDateFormat dateFormat) {
        SettingsRepository repository = viewModel.getRepository();
        int wearColor = getWearPhaseColor();
        int pauseColor = getPausePhaseColor();
        int specialColor = getSpecialPhaseColor();

        long cycleStartMillis;
        if (CycleType.INSERTION == cycle.getType()) {
            cycleStartMillis = cycle.getDateMillis();
        } else if (CycleType.REMOVAL == cycle.getType()) {
            Long cycleStartMillisValue = cycleStartByRemovalMillis.get(cycle.getDateMillis());
            cycleStartMillis = cycleStartMillisValue != null ? cycleStartMillisValue : 0L;
        } else {
            cycleStartMillis = 0L;
        }

        boolean skippedRingFree = CycleType.REMOVAL == cycle.getType()
                && cycleStartMillis > 0
                && repository.getRingFreeDaysForCycle(cycleStartMillis) == 0;
        boolean wornLonger = CycleType.INSERTION == cycle.getType()
                && cycleStartMillis > 0
                && repository.getCycleDelayDays(cycleStartMillis) > 0;
        boolean seamlessInsertion = CycleType.INSERTION == cycle.getType()
                && seamlessInsertionStarts.contains(cycle.getDateMillis());

        String titleText = formatCycleTitle(cycle, dateFormat);
        String statusText;
        String badgeText;
        String detailText = null;
        int accentColor;
        int statusColor;
        int detailColor;
        int statusTopPaddingDp = 6;
        int detailTopPaddingDp = 6;

        if (skippedRingFree || seamlessInsertion) {
            accentColor = specialColor;
            statusColor = specialColor;
            detailColor = lightenColor(specialColor, 0.28f);
            badgeText = getString(R.string.cycles_badge_direct_switch);
            detailText = getString(R.string.cycles_special_skip_ring_free);
            if (seamlessInsertion || isSameDay(cycle.getDateMillis(), cycle.getEndDateMillis())) {
                titleText = getString(R.string.cycles_title_seamless_ring_change);
                String dateText = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        .format(cycle.getDateMillis());
                statusText = dateText + " • " + getString(R.string.cycles_status_new_wear_phase_started);
            } else {
                statusText = getString(R.string.cycles_status_removed);
            }
        } else if (CycleType.INSERTION == cycle.getType()) {
            accentColor = wearColor;
            statusColor = wearColor;
            detailColor = lightenColor(wearColor, 0.28f);
            badgeText = getString(R.string.cycles_badge_wear_phase);
            statusText = getString(R.string.cycles_status_inserted);
            if (wornLonger) {
                detailText = getString(R.string.cycles_special_wear_longer_days,
                        repository.getCycleDelayDays(cycleStartMillis));
            }
        } else {
            accentColor = pauseColor;
            statusColor = pauseColor;
            detailColor = lightenColor(pauseColor, 0.28f);
            badgeText = getString(R.string.cycles_badge_pause);
            statusText = getString(R.string.cycles_status_removed);
        }

        return new CycleCardPresentation(titleText, statusText, badgeText, detailText,
                accentColor, statusColor, detailColor, statusTopPaddingDp, detailTopPaddingDp);
    }

    @NonNull
    private String formatCycleTitle(@NonNull Cycle cycle, @NonNull SimpleDateFormat dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(cycle.getDateMillis());
        if (cycle.getEndDateMillis() > 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTimeInMillis(cycle.getEndDateMillis());
            if (isSameDay(cycle.getDateMillis(), cycle.getEndDateMillis())) {
                return dateFormat.format(cal.getTime());
            }
            return String.format("%s - %s",
                    dateFormat.format(cal.getTime()),
                    dateFormat.format(endCal.getTime()));
        }
        return dateFormat.format(cal.getTime());
    }

    private boolean isSameDay(long firstMillis, long secondMillis) {
        if (firstMillis <= 0 || secondMillis <= 0) {
            return false;
        }
        Calendar first = Calendar.getInstance();
        first.setTimeInMillis(firstMillis);
        Calendar second = Calendar.getInstance();
        second.setTimeInMillis(secondMillis);
        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR)
                && first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR);
    }

    private int getWearPhaseColor() {
        Integer color = viewModel.getCalendarWearColor().getValue();
        return color != null ? color : viewModel.getRepository().getCalendarWearColor();
    }

    private int getPausePhaseColor() {
        Integer color = viewModel.getCalendarRingFreeColor().getValue();
        return color != null ? color : viewModel.getRepository().getCalendarRingFreeColor();
    }

    private int getSpecialPhaseColor() {
        return ContextCompat.getColor(requireContext(), R.color.purple_200);
    }

    @NonNull
    private View createBadgeView(@NonNull String text, int accentColor) {
        LinearLayout badgeContainer = new LinearLayout(requireContext());
        badgeContainer.setOrientation(LinearLayout.HORIZONTAL);
        badgeContainer.setGravity(Gravity.CENTER_VERTICAL);
        badgeContainer.setPadding(dpToPx(12), dpToPx(6), dpToPx(14), dpToPx(6));
        badgeContainer.setBackground(createTintedShape(
                R.drawable.bg_cycles_history_badge,
                ColorUtils.setAlphaComponent(accentColor, 48),
                ColorUtils.setAlphaComponent(accentColor, 56),
                dpToPx(1)
        ));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dpToPx(12), 0, 0, 0);
        badgeContainer.setLayoutParams(params);

        View dotView = new View(requireContext());
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(10), dpToPx(10));
        dotParams.setMargins(0, 0, dpToPx(8), 0);
        dotView.setLayoutParams(dotParams);
        dotView.setBackground(createTintedShape(
                R.drawable.bg_cycles_history_badge_dot,
                lightenColor(accentColor, 0.08f),
                0,
                0
        ));
        badgeContainer.addView(dotView);

        TextView badgeTextView = new TextView(requireContext());
        badgeTextView.setText(text);
        badgeTextView.setTextSize(11);
        badgeTextView.setTypeface(null, android.graphics.Typeface.NORMAL);
        badgeTextView.setTextColor(lightenColor(accentColor, 0.42f));
        badgeTextView.setGravity(Gravity.CENTER_VERTICAL);
        badgeContainer.addView(badgeTextView);

        return badgeContainer;
    }

    @NonNull
    private GradientDrawable createTintedShape(int drawableRes, int fillColor, int strokeColor, int strokeWidthPx) {
        android.graphics.drawable.Drawable baseDrawable =
                ContextCompat.getDrawable(requireContext(), drawableRes);
        GradientDrawable drawable;
        if (baseDrawable instanceof GradientDrawable) {
            drawable = (GradientDrawable) baseDrawable.mutate();
        } else {
            drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
        }
        drawable.setColor(fillColor);
        if (strokeWidthPx > 0) {
            drawable.setStroke(strokeWidthPx, strokeColor);
        } else {
            drawable.setStroke(0, android.graphics.Color.TRANSPARENT);
        }
        return drawable;
    }

    @NonNull
    private LayerDrawable createHistoryCardBackground(int accentColor) {
        GradientDrawable accentLayer = new GradientDrawable();
        accentLayer.setShape(GradientDrawable.RECTANGLE);
        accentLayer.setColor(accentColor);
        accentLayer.setCornerRadius(dpToPx(24));

        GradientDrawable contentLayer = new GradientDrawable();
        contentLayer.setShape(GradientDrawable.RECTANGLE);
        contentLayer.setColor(resolveThemeColor(com.google.android.material.R.attr.colorBackgroundFloating));
        contentLayer.setCornerRadius(dpToPx(24));

        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{
                accentLayer,
                contentLayer
        });
        layers.setLayerInset(1, dpToPx(4), 0, 0, 0);
        return layers;
    }

    private int resolveThemeColor(int attrRes) {
        TypedValue value = new TypedValue();
        requireContext().getTheme().resolveAttribute(attrRes, value, true);
        return value.data;
    }

    private int lightenColor(int color, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        return ColorUtils.blendARGB(color, 0xFFFFFFFF, amount);
    }

    @NonNull
    private TextView createMonthHeaderView(@NonNull String text) {
        TextView header = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dpToPx(8));
        header.setLayoutParams(params);
        header.setText(text);
        header.setTextSize(15f);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        Integer color = viewModel != null ? viewModel.getButtonColor().getValue() : null;
        if (color != null) {
            header.setTextColor(color);
        }
        return header;
    }

    private void updateSummary(@NonNull List<Cycle> cycles) {
        if (summaryCountView == null) {
            return;
        }

        int trackedCycles = cycles.size() / 2;
        summaryCountView.setText(getString(R.string.cycles_summary_count, trackedCycles));
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private static final class CycleCardPresentation {
        final String titleText;
        final String statusText;
        final String badgeText;
        @Nullable final String detailText;
        final int accentColor;
        final int statusColor;
        final int detailColor;
        final int statusTopPaddingDp;
        final int detailTopPaddingDp;

        CycleCardPresentation(@NonNull String titleText,
                              @NonNull String statusText,
                              @NonNull String badgeText,
                              @Nullable String detailText,
                              int accentColor,
                              int statusColor,
                              int detailColor,
                              int statusTopPaddingDp,
                              int detailTopPaddingDp) {
            this.titleText = titleText;
            this.statusText = statusText;
            this.badgeText = badgeText;
            this.detailText = detailText;
            this.accentColor = accentColor;
            this.statusColor = statusColor;
            this.detailColor = detailColor;
            this.statusTopPaddingDp = statusTopPaddingDp;
            this.detailTopPaddingDp = detailTopPaddingDp;
        }
    }

}
