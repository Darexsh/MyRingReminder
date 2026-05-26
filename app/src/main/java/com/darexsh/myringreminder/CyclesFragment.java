package com.darexsh.myringreminder;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;
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
        MaterialButton clearHistoryButton = view.findViewById(R.id.btn_clear_history);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                ButtonColorHelper.applyPrimaryColor(clearHistoryButton, color);
            }
        });

        // Load and display cycle history
        displayCycleHistory(cycleContainer);

        // Set up clear history button
        clearHistoryButton.setOnClickListener(v -> {
            viewModel.getRepository().saveCycleHistory(new ArrayList<>()); // Clear the cycle history from repository
            cycleContainer.removeAllViews();        // Clear the UI
            displayCycleHistory(cycleContainer);    // Reload empty history
        });

        return view;
    }

    // Display the cycle history in the provided LinearLayout
    private void displayCycleHistory(LinearLayout cycleContainer) {
        cycleContainer.removeAllViews(); // Clear existing views
        List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

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

        // Sort cycles by date in descending order (newest first)
        validCycles.sort((c1, c2) -> Long.compare(c2.getDateMillis(), c1.getDateMillis()));

        for (Cycle cycle : validCycles) {
            CardView cardView = new CardView(requireContext());
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 36);     // 12dp bottom margin
            cardView.setLayoutParams(cardParams);   // Set layout parameters for the card
            cardView.setCardElevation(0f);          // 4dp elevation
            cardView.setRadius(24);                 // 8dp corner radius
            cardView.setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent)); // Transparent background
            cardView.setPadding(36, 36, 36, 36);    // 12dp padding

            // Create a LinearLayout to hold the card content
            LinearLayout cardContent = new LinearLayout(requireContext());
            cardContent.setOrientation(LinearLayout.VERTICAL);
            cardContent.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

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
}
