package com.darexsh.myringreminder;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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
        List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();
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
            cardParams.setMargins(0, 0, 0, 12);
            cardView.setLayoutParams(cardParams);   // Set layout parameters for the card
            cardView.setCardElevation(0f);
            cardView.setRadius(24);
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent));
            cardView.setUseCompatPadding(false);
            cardView.setPreventCornerOverlap(true);

            // Create a LinearLayout to hold the card content
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            cardContent.setBackgroundResource(R.drawable.bg_app_info_dialog);
            int horizontalPadding = dpToPx(12);
            int verticalPadding = dpToPx(12);
            cardContent.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);

            // Create and configure the TextView for the date
            TextView dateTextView = new TextView(requireContext());
            dateTextView.setTextSize(18);
            dateTextView.setTypeface(null, android.graphics.Typeface.BOLD);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(cycle.getDateMillis());
            String dateText;

            // Format the date based on the cycle type
            if (CycleType.INSERTION == cycle.getType() || CycleType.REMOVAL == cycle.getType()) {
                Calendar endCal = Calendar.getInstance();
                endCal.setTimeInMillis(cycle.getEndDateMillis());

                if (cycle.getEndDateMillis() > 0) {
                    dateText = String.format("%s - %s",
                            dateFormat.format(cal.getTime()),
                            dateFormat.format(endCal.getTime()));
                } else {
                    dateText = dateFormat.format(cal.getTime());
                }
            } else {
                // If the cycle type is unknown, just show the start date
                dateText = dateFormat.format(cal.getTime());
            }
            dateTextView.setText(dateText);
            cardContent.addView(dateTextView);

            // Create and configure the TextView for the status
            TextView statusTextView = new TextView(requireContext());
            statusTextView.setTextSize(14);
            statusTextView.setPadding(0, 12, 0, 0); // 4dp top margin
            if (CycleType.INSERTION == cycle.getType()) {
                statusTextView.setText(R.string.cycles_status_inserted);
                statusTextView.setTextColor(0xFF4CAF50); // Green
            } else {
                statusTextView.setText(R.string.cycles_status_removed);
                statusTextView.setTextColor(0xFFF44336); // Red
            }
            cardContent.addView(statusTextView);

            cardView.addView(cardContent);
            cycleContainer.addView(cardView);
        }
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
}
