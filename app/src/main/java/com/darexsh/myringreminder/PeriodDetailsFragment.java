package com.darexsh.myringreminder;

import android.content.ContentResolver;
import android.content.ClipData;
import android.content.Intent;
import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PeriodDetailsFragment extends Fragment {
    private static final String ARG_YEAR = "year";
    private static final String ARG_MONTH_ONE_BASED = "month_one_based";
    private static final String PDF_NOTIFICATION_CHANNEL_ID = "period_details_pdf_channel_v2";
    private static final int PDF_NOTIFICATION_ID = 2204;

    private SharedViewModel viewModel;
    private int anchorYear;
    private int anchorMonthOneBased;
    private int selectedRangeMonths = 3; // -1 = all
    private String searchQuery = "";
    private boolean filterSymptomsOnly = false;
    private boolean filterPainOnly = false;
    private boolean filterStartOnly = false;
    private boolean filterEndOnly = false;
    private final List<DisplayEntry> visibleEntries = new ArrayList<>();
    private String visibleRangeLabel = "";
    private ActivityResultLauncher<String> createPdfLauncher;

    private static class DisplayEntry {
        final Calendar day;
        final PeriodDayEntry entry;
        final String monthHeader;

        DisplayEntry(Calendar day, PeriodDayEntry entry, String monthHeader) {
            this.day = day;
            this.entry = entry;
            this.monthHeader = monthHeader;
        }
    }

    public static PeriodDetailsFragment newInstance(int year, int monthOneBased) {
        PeriodDetailsFragment fragment = new PeriodDetailsFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_YEAR, year);
        args.putInt(ARG_MONTH_ONE_BASED, monthOneBased);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createPdfNotificationChannel();
        createPdfLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/pdf"),
                this::exportCurrentListToPdf
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_period_details, container, false);
        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        anchorYear = getArguments() != null ? getArguments().getInt(ARG_YEAR, 0) : 0;
        anchorMonthOneBased = getArguments() != null ? getArguments().getInt(ARG_MONTH_ONE_BASED, 0) : 0;

        TextView title = view.findViewById(R.id.tv_period_details_title);
        TextView month = view.findViewById(R.id.tv_period_details_month);
        TextView emptyView = view.findViewById(R.id.tv_period_details_empty);
        TextView rangeLabel = view.findViewById(R.id.tv_period_details_range_label);
        TextView filterLabel = view.findViewById(R.id.tv_period_details_filter_label);
        ImageButton closeButton = view.findViewById(R.id.btn_close_period_details);
        MaterialButton savePdfButton = view.findViewById(R.id.btn_period_details_save_pdf);
        LinearLayout listContainer = view.findViewById(R.id.layout_period_details_list);
        ScrollView scrollView = view.findViewById(R.id.scroll_period_details);
        ChipGroup rangeGroup = view.findViewById(R.id.chip_group_period_range);
        ChipGroup filterGroup = view.findViewById(R.id.chip_group_period_filters);
        EditText searchField = view.findViewById(R.id.et_period_details_search);
        Chip symptomsChip = view.findViewById(R.id.chip_period_filter_symptoms);
        Chip painChip = view.findViewById(R.id.chip_period_filter_pain);
        Chip startChip = view.findViewById(R.id.chip_period_filter_start);
        Chip endChip = view.findViewById(R.id.chip_period_filter_end);

        if (title != null) {
            title.setText(R.string.btn_period_details);
        }
        populateList(listContainer, emptyView, month);
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> closeToCalendar());
        }
        if (savePdfButton != null) {
            savePdfButton.setOnClickListener(v -> requestPdfExport());
        }
        if (rangeGroup != null) {
            rangeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                int checkedId = checkedIds.isEmpty() ? View.NO_ID : checkedIds.get(0);
                if (checkedId == R.id.chip_period_range_3m) {
                    selectedRangeMonths = 3;
                } else if (checkedId == R.id.chip_period_range_6m) {
                    selectedRangeMonths = 6;
                } else if (checkedId == R.id.chip_period_range_12m) {
                    selectedRangeMonths = 12;
                } else if (checkedId == R.id.chip_period_range_all) {
                    selectedRangeMonths = -1;
                }
                populateList(listContainer, emptyView, month);
            });
        }
        if (filterGroup != null) {
            filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                filterSymptomsOnly = symptomsChip != null && symptomsChip.isChecked();
                filterPainOnly = painChip != null && painChip.isChecked();
                filterStartOnly = startChip != null && startChip.isChecked();
                filterEndOnly = endChip != null && endChip.isChecked();
                populateList(listContainer, emptyView, month);
            });
        }
        if (searchField != null) {
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    searchQuery = s != null ? s.toString().trim() : "";
                    populateList(listContainer, emptyView, month);
                }
            });
            setupOutsideTapDismiss(view, scrollView, listContainer, rangeGroup, filterGroup,
                    title, month, rangeLabel, filterLabel, emptyView, closeButton, savePdfButton, searchField);
        }
        applyAccentColor(title, rangeLabel, filterLabel, closeButton, savePdfButton, searchField);
        return view;
    }

    private void setupOutsideTapDismiss(@NonNull View root,
                                        @Nullable ScrollView scrollView,
                                        @Nullable LinearLayout listContainer,
                                        @Nullable ChipGroup rangeGroup,
                                        @Nullable ChipGroup filterGroup,
                                        @Nullable TextView title,
                                        @Nullable TextView month,
                                        @Nullable TextView rangeLabel,
                                        @Nullable TextView filterLabel,
                                        @Nullable TextView emptyView,
                                        @Nullable ImageButton closeButton,
                                        @Nullable MaterialButton savePdfButton,
                                        @NonNull EditText searchField) {
        View.OnTouchListener listener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                dismissSearchInput(searchField);
            }
            return false;
        };
        root.setOnTouchListener(listener);
        if (scrollView != null) {
            scrollView.setOnTouchListener(listener);
        }
        if (listContainer != null) {
            listContainer.setOnTouchListener(listener);
        }
        if (rangeGroup != null) {
            rangeGroup.setOnTouchListener(listener);
        }
        if (filterGroup != null) {
            filterGroup.setOnTouchListener(listener);
        }
        if (title != null) {
            title.setOnTouchListener(listener);
        }
        if (month != null) {
            month.setOnTouchListener(listener);
        }
        if (rangeLabel != null) {
            rangeLabel.setOnTouchListener(listener);
        }
        if (filterLabel != null) {
            filterLabel.setOnTouchListener(listener);
        }
        if (emptyView != null) {
            emptyView.setOnTouchListener(listener);
        }
        if (closeButton != null) {
            closeButton.setOnTouchListener(listener);
        }
        if (savePdfButton != null) {
            savePdfButton.setOnTouchListener(listener);
        }
    }

    private void dismissSearchInput(@NonNull EditText searchField) {
        searchField.clearFocus();
        Context context = requireContext();
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
        }
    }

    private void closeToCalendar() {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
            return;
        }
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }

    private void populateList(@Nullable LinearLayout container,
                              @Nullable TextView emptyView,
                              @Nullable TextView monthLabel) {
        if (container == null || viewModel == null) {
            return;
        }
        container.removeAllViews();
        visibleEntries.clear();
        if (emptyView != null) {
            emptyView.setVisibility(View.GONE);
        }

        Calendar rangeEnd = Calendar.getInstance();
        rangeEnd.set(Calendar.YEAR, anchorYear);
        rangeEnd.set(Calendar.MONTH, Math.max(0, anchorMonthOneBased - 1));
        rangeEnd.set(Calendar.DAY_OF_MONTH, rangeEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        rangeEnd.set(Calendar.HOUR_OF_DAY, 23);
        rangeEnd.set(Calendar.MINUTE, 59);
        rangeEnd.set(Calendar.SECOND, 59);
        rangeEnd.set(Calendar.MILLISECOND, 999);

        Calendar rangeStart = Calendar.getInstance();
        rangeStart.setTimeInMillis(rangeEnd.getTimeInMillis());
        if (selectedRangeMonths > 0) {
            rangeStart.set(Calendar.DAY_OF_MONTH, 1);
            rangeStart.add(Calendar.MONTH, -(selectedRangeMonths - 1));
            rangeStart.set(Calendar.HOUR_OF_DAY, 0);
            rangeStart.set(Calendar.MINUTE, 0);
            rangeStart.set(Calendar.SECOND, 0);
            rangeStart.set(Calendar.MILLISECOND, 0);
        } else {
            rangeStart.set(Calendar.YEAR, 1970);
            rangeStart.set(Calendar.MONTH, Calendar.JANUARY);
            rangeStart.set(Calendar.DAY_OF_MONTH, 1);
            rangeStart.set(Calendar.HOUR_OF_DAY, 0);
            rangeStart.set(Calendar.MINUTE, 0);
            rangeStart.set(Calendar.SECOND, 0);
            rangeStart.set(Calendar.MILLISECOND, 0);

            rangeEnd.set(Calendar.YEAR, 9999);
            rangeEnd.set(Calendar.MONTH, Calendar.DECEMBER);
            rangeEnd.set(Calendar.DAY_OF_MONTH, 31);
            rangeEnd.set(Calendar.HOUR_OF_DAY, 23);
            rangeEnd.set(Calendar.MINUTE, 59);
            rangeEnd.set(Calendar.SECOND, 59);
            rangeEnd.set(Calendar.MILLISECOND, 999);
        }

        if (monthLabel != null) {
            visibleRangeLabel = buildRangeLabel(rangeStart, rangeEnd);
            monthLabel.setText(visibleRangeLabel);
        } else {
            visibleRangeLabel = buildRangeLabel(rangeStart, rangeEnd);
        }

        Map<String, PeriodDayEntry> allEntries = viewModel.getRepository().getAllPeriodDayEntries();
        List<Map.Entry<String, PeriodDayEntry>> rangeEntries = new ArrayList<>();
        for (Map.Entry<String, PeriodDayEntry> item : allEntries.entrySet()) {
            PeriodDayEntry entry = item.getValue();
            if (entry == null || !entry.isPeriodDay()) {
                continue;
            }
            Calendar day = parseDateKey(item.getKey());
            if (day == null) {
                continue;
            }
            long dayMillis = day.getTimeInMillis();
            if (dayMillis >= rangeStart.getTimeInMillis() && dayMillis <= rangeEnd.getTimeInMillis()) {
                rangeEntries.add(item);
            }
        }

        List<Map.Entry<String, PeriodDayEntry>> monthEntries = new ArrayList<>();
        for (Map.Entry<String, PeriodDayEntry> item : rangeEntries) {
            PeriodDayEntry entry = item.getValue();
            Calendar day = parseDateKey(item.getKey());
            if (entry == null || day == null) {
                continue;
            }
            if (matchesActiveFilters(day, entry)) {
                monthEntries.add(item);
            }
        }

        monthEntries.sort((a, b) -> {
            String keyA = a.getKey();
            String keyB = b.getKey();
            String monthA = keyA != null && keyA.length() >= 7 ? keyA.substring(0, 7) : "";
            String monthB = keyB != null && keyB.length() >= 7 ? keyB.substring(0, 7) : "";
            int monthCompare = monthB.compareTo(monthA); // newest month first
            if (monthCompare != 0) {
                return monthCompare;
            }
            if (keyA == null) {
                return 1;
            }
            if (keyB == null) {
                return -1;
            }
            return keyA.compareTo(keyB); // oldest day first within month
        });
        if (monthEntries.isEmpty()) {
            if (emptyView != null) {
                emptyView.setText(rangeEntries.isEmpty()
                        ? R.string.period_details_empty
                        : R.string.period_details_empty_filtered);
                emptyView.setVisibility(View.VISIBLE);
            }
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat monthHeaderFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String lastMonthHeader = null;
        for (Map.Entry<String, PeriodDayEntry> item : monthEntries) {
            PeriodDayEntry entry = item.getValue();
            Calendar day = parseDateKey(item.getKey());
            if (entry == null || day == null) {
                continue;
            }
            String currentMonthHeader = monthHeaderFormat.format(day.getTime());
            if (!currentMonthHeader.equals(lastMonthHeader)) {
                container.addView(createMonthHeaderView(currentMonthHeader));
                lastMonthHeader = currentMonthHeader;
            }
            View card = inflater.inflate(R.layout.item_period_detail_entry, container, false);
            TextView tvDate = card.findViewById(R.id.tv_period_detail_date);
            TextView tvIntensity = card.findViewById(R.id.tv_period_detail_intensity);
            TextView tvPain = card.findViewById(R.id.tv_period_detail_pain);
            TextView tvSymptoms = card.findViewById(R.id.tv_period_detail_symptoms);
            TextView tvMarkers = card.findViewById(R.id.tv_period_detail_markers);

            tvDate.setText(dateFormat.format(day.getTime()));
            tvIntensity.setText(getString(R.string.period_modal_intensity_title) + ": " + intensityLabel(entry.getIntensity()));
            tvPain.setText(getString(R.string.period_modal_pain_title) + ": " + painLabel(entry.getPainSeverity()));
            tvSymptoms.setText(getString(R.string.period_modal_symptoms_title) + ": " + symptomsLabel(entry));
            tvMarkers.setText(getString(R.string.period_modal_markers_title) + ": " + markersLabel(entry));

            container.addView(card);
            visibleEntries.add(new DisplayEntry((Calendar) day.clone(), entry, currentMonthHeader));
        }
    }

    private boolean matchesActiveFilters(@NonNull Calendar day, @NonNull PeriodDayEntry entry) {
        if (filterSymptomsOnly && !(entry.hasAnyAdditionalSymptoms() || entry.hasIllness())) {
            return false;
        }
        if (filterPainOnly && (entry.getPainSeverity() == null || entry.getPainSeverity() == PainSeverity.NONE)) {
            return false;
        }
        if (filterStartOnly && !entry.isStart()) {
            return false;
        }
        if (filterEndOnly && !entry.isEnd()) {
            return false;
        }
        if (searchQuery.isEmpty()) {
            return true;
        }
        String normalizedQuery = normalizeForSearch(searchQuery);
        String searchableText = buildSearchableText(day, entry);
        return searchableText.contains(normalizedQuery);
    }

    @NonNull
    private String buildSearchableText(@NonNull Calendar day, @NonNull PeriodDayEntry entry) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat monthHeaderFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        String text = dateFormat.format(day.getTime())
                + " "
                + monthHeaderFormat.format(day.getTime())
                + " "
                + intensityLabel(entry.getIntensity())
                + " "
                + painLabel(entry.getPainSeverity())
                + " "
                + symptomsLabel(entry)
                + " "
                + markersLabel(entry);
        return normalizeForSearch(text);
    }

    @NonNull
    private String normalizeForSearch(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.getDefault()).trim();
    }

    private String buildRangeLabel(@NonNull Calendar start, @NonNull Calendar end) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        if (selectedRangeMonths == -1) {
            return getString(R.string.period_details_range_all);
        }
        return getString(R.string.period_details_range_between,
                monthFormat.format(start.getTime()),
                monthFormat.format(end.getTime()));
    }

    @NonNull
    private TextView createMonthHeaderView(@NonNull String text) {
        TextView header = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int top = dpToPx(8);
        int bottom = dpToPx(6);
        params.setMargins(0, top, 0, bottom);
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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void requestPdfExport() {
        if (visibleEntries.isEmpty()) {
            Toast.makeText(requireContext(), R.string.period_details_pdf_no_data, Toast.LENGTH_SHORT).show();
            return;
        }
        String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String fileName = getString(R.string.period_details_pdf_file_name, stamp);
        createPdfLauncher.launch(fileName);
    }

    private void exportCurrentListToPdf(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        PdfDocument document = new PdfDocument();
        try {
            renderPdf(document);
            ContentResolver resolver = requireContext().getContentResolver();
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream == null) {
                    throw new IOException("Output stream is null");
                }
                document.writeTo(outputStream);
            }
            showPdfSavedNotification(uri);
        } catch (Exception ignored) {
            Toast.makeText(requireContext(), R.string.period_details_pdf_save_failed, Toast.LENGTH_SHORT).show();
        } finally {
            document.close();
        }
    }

    private void createPdfNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                PDF_NOTIFICATION_CHANNEL_ID,
                getString(R.string.period_details_pdf_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.period_details_pdf_notification_channel_desc));
        notificationManager.createNotificationChannel(channel);
    }

    private void showPdfSavedNotification(@NonNull Uri uri) {
        Intent openIntent = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/pdf")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        openIntent.setClipData(ClipData.newRawUri("", uri));

        PendingIntent pendingIntent = PendingIntent.getActivity(
                requireContext(),
                0,
                Intent.createChooser(openIntent, getString(R.string.period_details_pdf_open_action)),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), PDF_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(getString(R.string.period_details_pdf_notification_title))
                .setContentText(getString(R.string.period_details_pdf_notification_text))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(getString(R.string.period_details_pdf_notification_text)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setDefaults(NotificationCompat.DEFAULT_LIGHTS);

        NotificationManagerCompat.from(requireContext()).notify(PDF_NOTIFICATION_ID, builder.build());
    }

    private void renderPdf(@NonNull PdfDocument document) {
        final int pageWidth = 595;   // A4 at 72dpi
        final int pageHeight = 842;
        final int margin = 34;
        final int contentWidth = pageWidth - (margin * 2);
        final int accentColor = viewModel != null && viewModel.getButtonColor().getValue() != null
                ? viewModel.getButtonColor().getValue()
                : 0xFF2E7D32;

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(accentColor);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(18f);

        Paint subtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtitlePaint.setColor(0xFF1F1F1F);
        subtitlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        subtitlePaint.setTextSize(12f);

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(accentColor);
        sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sectionPaint.setTextSize(13f);

        Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(10.5f);

        Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(0xFF2F2F2F);
        labelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        labelPaint.setTextSize(9.5f);

        Paint subtlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtlePaint.setColor(0xFF4E4E4E);
        subtlePaint.setTextSize(9.5f);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFD6D6D6);
        linePaint.setStrokeWidth(1f);

        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(0xFFE6E6E6);
        dividerPaint.setStrokeWidth(1f);

        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBgPaint.setColor(0xFFF8F8F8);
        Paint cardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBorderPaint.setColor(0xFFD6D6D6);
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(1f);
        Paint softAccentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        softAccentPaint.setColor(applyAlpha(accentColor, 28));

        PdfState state = startPdfPage(document, pageWidth, pageHeight, 1, margin);
        state.y = drawPdfHeader(state.canvas, margin, state.y, contentWidth,
                titlePaint, subtitlePaint, subtlePaint, linePaint);
        state.y += 14f;

        int totalEntries = visibleEntries.size();
        int startCount = 0;
        int endCount = 0;
        int painCount = 0;
        for (DisplayEntry displayEntry : visibleEntries) {
            if (displayEntry.entry.isStart()) {
                startCount++;
            }
            if (displayEntry.entry.isEnd()) {
                endCount++;
            }
            if (displayEntry.entry.getPainSeverity() != null && displayEntry.entry.getPainSeverity() != PainSeverity.NONE) {
                painCount++;
            }
        }

        int longStreak = findLongestPeriodStreak();
        int symptomDays = countSymptomDays();
        int strongPainDays = countStrongPainDays();
        final float gridGap = 10f;
        final float infoCardWidth = (contentWidth - gridGap) / 2f;
        final float infoCardPadding = 8f;
        final float infoCardCorner = 8f;

        float summaryHeight = estimateInfoCardHeight(
                getString(R.string.period_details_pdf_summary_title),
                new String[]{
                        getString(R.string.period_details_pdf_summary_entries, totalEntries),
                        getString(R.string.period_details_pdf_summary_starts, startCount),
                        getString(R.string.period_details_pdf_summary_ends, endCount),
                        getString(R.string.period_details_pdf_summary_pain_days, painCount)
                },
                sectionPaint, normalPaint, infoCardWidth, infoCardPadding
        );
        float legendHeight = estimateInfoCardHeight(
                getString(R.string.period_details_pdf_legend_title),
                new String[]{
                        getString(R.string.period_modal_intensity_light),
                        getString(R.string.period_modal_intensity_medium),
                        getString(R.string.period_modal_intensity_heavy),
                        getString(R.string.period_details_pdf_legend_pain_badge),
                        getString(R.string.period_details_pdf_legend_symptom_badge)
                },
                sectionPaint, subtlePaint, infoCardWidth, infoCardPadding
        );
        float anomaliesHeight = estimateInfoCardHeight(
                getString(R.string.period_details_pdf_anomalies_title),
                new String[]{
                        getString(R.string.period_details_pdf_anomalies_longest_streak, longStreak),
                        getString(R.string.period_details_pdf_anomalies_symptom_days, symptomDays),
                        getString(R.string.period_details_pdf_anomalies_strong_pain_days, strongPainDays)
                },
                sectionPaint, subtlePaint, contentWidth, infoCardPadding
        );

        state = ensurePageSpace(document, state,
                Math.max(summaryHeight, legendHeight) + anomaliesHeight + 16f,
                margin, pageWidth, pageHeight, linePaint, subtlePaint);

        float summaryRowY = state.y;
        drawInfoCard(state.canvas, margin, summaryRowY, infoCardWidth, summaryHeight,
                getString(R.string.period_details_pdf_summary_title),
                new String[]{
                        getString(R.string.period_details_pdf_summary_entries, totalEntries),
                        getString(R.string.period_details_pdf_summary_starts, startCount),
                        getString(R.string.period_details_pdf_summary_ends, endCount),
                        getString(R.string.period_details_pdf_summary_pain_days, painCount)
                },
                sectionPaint, normalPaint, infoCardPadding, infoCardCorner, cardBgPaint, cardBorderPaint, null, null);
        drawInfoCard(state.canvas, margin + infoCardWidth + gridGap, summaryRowY, infoCardWidth, legendHeight,
                getString(R.string.period_details_pdf_legend_title),
                new String[]{
                        getString(R.string.period_modal_intensity_light),
                        getString(R.string.period_modal_intensity_medium),
                        getString(R.string.period_modal_intensity_heavy),
                        getString(R.string.period_details_pdf_legend_pain_badge),
                        getString(R.string.period_details_pdf_legend_symptom_badge)
                },
                sectionPaint, subtlePaint, infoCardPadding, infoCardCorner, cardBgPaint, cardBorderPaint,
                new int[]{0x88EF9A9A, 0x99EF5350, 0xCCB71C1C, 0xFFFFB300, 0xFF40C4FF}, badgePaint);
        state.y += Math.max(summaryHeight, legendHeight) + 8f;

        drawInfoCard(state.canvas, margin, state.y, contentWidth, anomaliesHeight,
                getString(R.string.period_details_pdf_anomalies_title),
                new String[]{
                        getString(R.string.period_details_pdf_anomalies_longest_streak, longStreak),
                        getString(R.string.period_details_pdf_anomalies_symptom_days, symptomDays),
                        getString(R.string.period_details_pdf_anomalies_strong_pain_days, strongPainDays)
                },
                sectionPaint, subtlePaint, infoCardPadding, infoCardCorner, softAccentPaint, cardBorderPaint, null, null);
        state.y += anomaliesHeight + 18f;

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        int index = 0;
        while (index < visibleEntries.size()) {
            String month = visibleEntries.get(index).monthHeader;
            state = ensurePageSpace(document, state, 20f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
            state.canvas.drawText(month, margin, state.y, sectionPaint);
            state.y += 12f;

            List<DisplayEntry> monthEntries = new ArrayList<>();
            while (index < visibleEntries.size() && month.equals(visibleEntries.get(index).monthHeader)) {
                monthEntries.add(visibleEntries.get(index));
                index++;
            }

            final float entryCardPadding = 8f;
            final float entryCardCorner = 8f;
            final float entryCardGap = 6f;
            final int entryColumns = 3;
            final float entryCardWidth = (contentWidth - (entryCardGap * (entryColumns - 1))) / entryColumns;
            for (int rowStart = 0; rowStart < monthEntries.size(); rowStart += entryColumns) {
                int rowEnd = Math.min(rowStart + entryColumns, monthEntries.size());
                float[] rowHeights = new float[rowEnd - rowStart];
                float rowMaxHeight = 0f;
                for (int i = rowStart; i < rowEnd; i++) {
                    float entryHeight = estimateEntryCardHeight(monthEntries.get(i), dayFormat, normalPaint, subtlePaint,
                            labelPaint, entryCardWidth, entryCardPadding);
                    rowHeights[i - rowStart] = entryHeight;
                    if (entryHeight > rowMaxHeight) {
                        rowMaxHeight = entryHeight;
                    }
                }

                state = ensurePageSpace(document, state, rowMaxHeight + entryCardGap, margin, pageWidth, pageHeight, linePaint, subtlePaint);
                for (int i = rowStart; i < rowEnd; i++) {
                    float cardX = margin + ((i - rowStart) * (entryCardWidth + entryCardGap));
                    drawEntryCard(state.canvas, monthEntries.get(i), dayFormat, cardX, state.y, entryCardWidth, rowHeights[i - rowStart],
                            entryCardPadding, entryCardCorner, cardBgPaint, cardBorderPaint, dividerPaint,
                            normalPaint, labelPaint, subtlePaint, badgePaint, accentColor);
                }
                state.y += rowMaxHeight + entryCardGap;
            }
            state.y += 8f;
        }

        final float notesCardPadding = 8f;
        final int notesLineCount = 10;
        final float notesLineSpacing = 10f;
        final float notesCardHeight = notesCardPadding + sectionPaint.getTextSize() + 12f
                + (notesLineCount * notesLineSpacing) + 8f;
        state = ensurePageSpace(document, state, notesCardHeight + 4f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
        android.graphics.RectF notesRect = new android.graphics.RectF(margin, state.y, margin + contentWidth, state.y + notesCardHeight);
        state.canvas.drawRoundRect(notesRect, infoCardCorner, infoCardCorner, cardBgPaint);
        state.canvas.drawRoundRect(notesRect, infoCardCorner, infoCardCorner, cardBorderPaint);

        float notesX = margin + notesCardPadding;
        state.y += notesCardPadding + sectionPaint.getTextSize();
        state.canvas.drawText(getString(R.string.period_details_pdf_notes_title), notesX, state.y, sectionPaint);
        state.y += 12f;
        for (int n = 0; n < notesLineCount; n++) {
            state.canvas.drawLine(notesX, state.y, margin + contentWidth - notesCardPadding, state.y, linePaint);
            state.y += notesLineSpacing;
        }
        state.y = notesRect.bottom + 2f;

        drawPdfFooter(state.canvas, margin, pageWidth, pageHeight, subtlePaint, state.pageNumber);
        document.finishPage(state.page);
    }

    private PdfState startPdfPage(@NonNull PdfDocument document, int pageWidth, int pageHeight, int pageNumber, int margin) {
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        PdfState state = new PdfState();
        state.page = page;
        state.canvas = page.getCanvas();
        state.pageNumber = pageNumber;
        state.y = margin;
        return state;
    }

    private float drawPdfHeader(@NonNull android.graphics.Canvas canvas,
                                int margin,
                                float y,
                                int contentWidth,
                                @NonNull Paint titlePaint,
                                @NonNull Paint subtitlePaint,
                                @NonNull Paint subtlePaint,
                                @NonNull Paint linePaint) {
        int iconSize = dpToPx(16);
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_launcher);
        float iconX = margin + contentWidth - iconSize;
        if (icon != null) {
            icon.setBounds(Math.round(iconX), Math.round(y - 2f), Math.round(iconX + iconSize), Math.round(y - 2f + iconSize));
            icon.draw(canvas);
        }

        float textStartX = margin;
        float maxTextWidth = contentWidth - iconSize - 16f;
        y = drawWrappedText(canvas, getString(R.string.period_details_pdf_export_title),
                textStartX, y + titlePaint.getTextSize(), maxTextWidth, titlePaint);
        y = drawWrappedText(canvas, getString(R.string.app_info_name),
                textStartX, y + 2f, maxTextWidth, subtitlePaint);
        y = drawWrappedText(canvas, getString(R.string.period_details_range_label) + ": " + visibleRangeLabel,
                textStartX, y + 3f, maxTextWidth, subtlePaint);
        String created = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        y = drawWrappedText(canvas, getString(R.string.period_details_pdf_created_at, created),
                textStartX, y + 3f, maxTextWidth, subtlePaint);
        y += 4f;
        canvas.drawLine(margin, y, margin + contentWidth, y, linePaint);
        return y;
    }

    private PdfState ensurePageSpace(@NonNull PdfDocument document,
                                     @NonNull PdfState state,
                                     float requiredSpace,
                                     int margin,
                                     int pageWidth,
                                     int pageHeight,
                                     @NonNull Paint linePaint,
                                     @NonNull Paint subtlePaint) {
        if (state.y + requiredSpace <= pageHeight - margin - 18f) {
            return state;
        }
        drawPdfFooter(state.canvas, margin, pageWidth, pageHeight, subtlePaint, state.pageNumber);
        document.finishPage(state.page);
        PdfState newState = startPdfPage(document, pageWidth, pageHeight, state.pageNumber + 1, margin);
        newState.canvas.drawLine(margin, margin + 4f, pageWidth - margin, margin + 4f, linePaint);
        newState.y = margin + 18f;
        return newState;
    }

    private void drawPdfFooter(@NonNull android.graphics.Canvas canvas, int margin, int pageWidth, int pageHeight, @NonNull Paint paint, int pageNumber) {
        String pageText = getString(R.string.period_details_pdf_page_number, pageNumber);
        float width = paint.measureText(pageText);
        canvas.drawText(pageText, pageWidth - margin - width, pageHeight - margin + 8f, paint);
    }

    private float estimateWrappedBlockHeight(@NonNull String text, @NonNull Paint paint, float maxWidth) {
        List<String> lines = wrapText(text, paint, maxWidth);
        if (lines.isEmpty()) {
            return 0f;
        }
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float lineHeight = (metrics.descent - metrics.ascent) + 2f;
        return lines.size() * lineHeight;
    }

    private float estimateInfoCardHeight(@NonNull String title,
                                         @NonNull String[] lines,
                                         @NonNull Paint titlePaint,
                                         @NonNull Paint bodyPaint,
                                         float cardWidth,
                                         float padding) {
        float innerWidth = cardWidth - (padding * 2f);
        float height = padding;
        height += estimateWrappedBlockHeight(title, titlePaint, innerWidth) + 4f;
        for (String line : lines) {
            height += estimateBulletRowHeight(line, bodyPaint, innerWidth);
        }
        return height + padding;
    }

    private float estimateEntryCardHeight(@NonNull DisplayEntry displayEntry,
                                          @NonNull SimpleDateFormat dayFormat,
                                          @NonNull Paint normalPaint,
                                          @NonNull Paint subtlePaint,
                                          @NonNull Paint labelPaint,
                                          float cardWidth,
                                          float cardPadding) {
        float innerWidth = cardWidth - (cardPadding * 2f);
        String dateText = dayFormat.format(displayEntry.day.getTime());
        String intensityText = intensityLabel(displayEntry.entry.getIntensity());
        String painText = painLabel(displayEntry.entry.getPainSeverity());
        String symptomsText = symptomsLabel(displayEntry.entry);
        String markersText = markersLabel(displayEntry.entry);

        float totalHeight = cardPadding * 2f;
        totalHeight += estimateWrappedBlockHeight(dateText, normalPaint, innerWidth);
        totalHeight += estimateInlineDetailRowHeight(getString(R.string.period_modal_intensity_title), intensityText, subtlePaint, innerWidth);
        totalHeight += estimateInlineDetailRowHeight(getString(R.string.period_modal_pain_title), painText, subtlePaint, innerWidth);
        totalHeight += estimateInlineDetailRowHeight(getString(R.string.period_modal_symptoms_title), symptomsText, subtlePaint, innerWidth);
        totalHeight += estimateInlineDetailRowHeight(getString(R.string.period_modal_markers_title), markersText, subtlePaint, innerWidth);

        return totalHeight - 2f;
    }

    private void drawEntryCard(@NonNull android.graphics.Canvas canvas,
                               @NonNull DisplayEntry displayEntry,
                               @NonNull SimpleDateFormat dayFormat,
                               float x,
                               float y,
                               float cardWidth,
                               float cardHeight,
                               float cardPadding,
                               float cardCorner,
                               @NonNull Paint bgPaint,
                               @NonNull Paint borderPaint,
                               @NonNull Paint dividerPaint,
                               @NonNull Paint normalPaint,
                               @NonNull Paint labelPaint,
                               @NonNull Paint subtlePaint,
                               @NonNull Paint badgePaint,
                               int accentColor) {
        android.graphics.RectF rect = new android.graphics.RectF(x, y, x + cardWidth, y + cardHeight);
        canvas.drawRoundRect(rect, cardCorner, cardCorner, bgPaint);
        canvas.drawRoundRect(rect, cardCorner, cardCorner, borderPaint);

        Paint.FontMetrics normalMetrics = normalPaint.getFontMetrics();
        float cursorY = y + cardPadding + Math.abs(normalMetrics.ascent);
        float innerX = x + cardPadding;
        float innerWidth = cardWidth - (cardPadding * 2f);

        String dateText = dayFormat.format(displayEntry.day.getTime());
        cursorY = drawWrappedText(canvas, dateText, innerX, cursorY, innerWidth, normalPaint);

        // Detail rows
        cursorY = drawInlineDetailRow(canvas, innerX, cursorY, innerWidth,
                getString(R.string.period_modal_intensity_title), intensityLabel(displayEntry.entry.getIntensity()),
                intensityBadgeColor(displayEntry.entry.getIntensity()), subtlePaint, badgePaint);

        cursorY = drawInlineDetailRow(canvas, innerX, cursorY, innerWidth,
                getString(R.string.period_modal_pain_title), painLabel(displayEntry.entry.getPainSeverity()),
                painBadgeColor(displayEntry.entry.getPainSeverity()), subtlePaint, badgePaint);

        cursorY = drawInlineDetailRow(canvas, innerX, cursorY, innerWidth,
                getString(R.string.period_modal_symptoms_title), symptomsLabel(displayEntry.entry),
                (displayEntry.entry.hasAnyAdditionalSymptoms() || displayEntry.entry.hasIllness()) ? 0xFF40C4FF : 0xFFBDBDBD,
                subtlePaint, badgePaint);

        drawInlineDetailRow(canvas, innerX, cursorY, innerWidth,
                getString(R.string.period_modal_markers_title), markersLabel(displayEntry.entry),
                (displayEntry.entry.isStart() || displayEntry.entry.isEnd()) ? 0xFF8E24AA : 0xFFBDBDBD,
                subtlePaint, badgePaint);
    }

    private float drawInfoCard(@NonNull android.graphics.Canvas canvas,
                               float x,
                               float y,
                               float width,
                               float height,
                               @NonNull String title,
                               @NonNull String[] lines,
                               @NonNull Paint titlePaint,
                               @NonNull Paint bodyPaint,
                               float padding,
                               float corner,
                               @NonNull Paint bgPaint,
                               @NonNull Paint borderPaint,
                               @Nullable int[] badgeColors,
                               @Nullable Paint badgePaint) {
        android.graphics.RectF rect = new android.graphics.RectF(x, y, x + width, y + height);
        canvas.drawRoundRect(rect, corner, corner, bgPaint);
        canvas.drawRoundRect(rect, corner, corner, borderPaint);

        Paint.FontMetrics titleMetrics = titlePaint.getFontMetrics();
        float cursorY = y + padding + Math.abs(titleMetrics.ascent);
        float innerX = x + padding;
        float textWidth = width - (padding * 2f);

        cursorY = drawWrappedText(canvas, title, innerX, cursorY, textWidth, titlePaint) + 2f;

        for (int i = 0; i < lines.length; i++) {
            int badgeColor = badgeColors != null && i < badgeColors.length ? badgeColors[i] : 0;
            if (badgePaint != null && badgeColor != 0) {
                badgePaint.setColor(badgeColor);
                cursorY = drawBulletText(canvas, lines[i], innerX, cursorY, textWidth, bodyPaint, badgePaint);
            } else {
                cursorY = drawBulletText(canvas, lines[i], innerX, cursorY, textWidth, bodyPaint, null);
            }
        }
        return y + height;
    }

    private float estimateBulletRowHeight(@NonNull String text,
                                          @NonNull Paint textPaint,
                                          float maxWidth) {
        return estimateWrappedBlockHeight(text, textPaint, maxWidth - 11f) + 1f;
    }

    private float estimateInlineDetailRowHeight(@NonNull String label,
                                                @NonNull String value,
                                                @NonNull Paint valuePaint,
                                                float maxWidth) {
        return estimateBulletRowHeight(label + ": " + value, valuePaint, maxWidth) + 2f;
    }

    private float drawInlineDetailRow(@NonNull android.graphics.Canvas canvas,
                                float x,
                                float y,
                                float maxWidth,
                                @NonNull String label,
                                @NonNull String value,
                                int badgeColor,
                                @NonNull Paint valuePaint,
                                @NonNull Paint badgePaint) {
        badgePaint.setColor(badgeColor);
        return drawBulletText(canvas, label + ": " + value, x, y, maxWidth, valuePaint, badgePaint) + 1f;
    }

    private float drawBulletText(@NonNull android.graphics.Canvas canvas,
                                 @NonNull String text,
                                 float x,
                                 float y,
                                 float maxWidth,
                                 @NonNull Paint textPaint,
                                 @Nullable Paint badgePaint) {
        float badgeX = x + 3f;
        float textX = x + 11f;
        float textWidth = maxWidth - 11f;
        if (badgePaint != null) {
            canvas.drawCircle(badgeX, y - 3f, 2.7f, badgePaint);
        } else {
            Paint bulletPaint = new Paint(textPaint);
            bulletPaint.setColor(0xFF9E9E9E);
            canvas.drawCircle(badgeX, y - 3f, 2.2f, bulletPaint);
        }
        return drawWrappedText(canvas, text, textX, y, textWidth, textPaint) + 1f;
    }

    private int applyAlpha(int color, int alpha) {
        return Color.argb(
                Math.max(0, Math.min(255, alpha)),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
        );
    }

    private int intensityBadgeColor(@Nullable BleedingIntensity intensity) {
        if (intensity == BleedingIntensity.LIGHT) {
            return 0x88EF9A9A;
        }
        if (intensity == BleedingIntensity.HEAVY) {
            return 0xCCB71C1C;
        }
        return 0x99EF5350;
    }

    private int painBadgeColor(@Nullable PainSeverity severity) {
        if (severity == PainSeverity.STRONG) {
            return 0xFFE65100;
        }
        if (severity == PainSeverity.LIGHT) {
            return 0xFFFFCC80;
        }
        return 0xFFFFB300;
    }

    private int countSymptomDays() {
        int count = 0;
        for (DisplayEntry entry : visibleEntries) {
            if (entry.entry.hasAnyAdditionalSymptoms() || entry.entry.hasIllness()) {
                count++;
            }
        }
        return count;
    }

    private int countStrongPainDays() {
        int count = 0;
        for (DisplayEntry entry : visibleEntries) {
            if (entry.entry.getPainSeverity() == PainSeverity.STRONG) {
                count++;
            }
        }
        return count;
    }

    private int findLongestPeriodStreak() {
        if (visibleEntries.isEmpty()) {
            return 0;
        }
        int longest = 1;
        int current = 1;
        Calendar previous = null;
        for (DisplayEntry displayEntry : visibleEntries) {
            Calendar day = displayEntry.day;
            if (previous == null) {
                previous = day;
                continue;
            }
            long diffDays = Math.abs((day.getTimeInMillis() - previous.getTimeInMillis()) / (24L * 60L * 60L * 1000L));
            if (diffDays == 1L) {
                current++;
            } else {
                current = 1;
            }
            if (current > longest) {
                longest = current;
            }
            previous = day;
        }
        return longest;
    }

    private float drawWrappedText(@NonNull android.graphics.Canvas canvas,
                                  @NonNull String text,
                                  float x,
                                  float y,
                                  float maxWidth,
                                  @NonNull Paint paint) {
        List<String> lines = wrapText(text, paint, maxWidth);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float lineHeight = (metrics.descent - metrics.ascent) + 2f;
        float baseline = y;
        for (String line : lines) {
            canvas.drawText(line, x, baseline, paint);
            baseline += lineHeight;
        }
        return baseline;
    }

    @NonNull
    private List<String> wrapText(@NonNull String text, @NonNull Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }
            String candidate = current + " " + word;
            if (paint.measureText(candidate) <= maxWidth) {
                current.append(" ").append(word);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        if (lines.isEmpty()) {
            lines.add(text);
        }
        return lines;
    }

    private static class PdfState {
        PdfDocument.Page page;
        android.graphics.Canvas canvas;
        int pageNumber;
        float y;
    }

    @Nullable
    private Calendar parseDateKey(@Nullable String key) {
        if (key == null || key.length() != 10) {
            return null;
        }
        try {
            int year = Integer.parseInt(key.substring(0, 4));
            int month = Integer.parseInt(key.substring(5, 7));
            int day = Integer.parseInt(key.substring(8, 10));
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String intensityLabel(@Nullable BleedingIntensity intensity) {
        if (intensity == BleedingIntensity.LIGHT) {
            return getString(R.string.period_modal_intensity_light);
        }
        if (intensity == BleedingIntensity.HEAVY) {
            return getString(R.string.period_modal_intensity_heavy);
        }
        return getString(R.string.period_modal_intensity_medium);
    }

    private String painLabel(@Nullable PainSeverity severity) {
        if (severity == PainSeverity.LIGHT) {
            return getString(R.string.period_modal_pain_light);
        }
        if (severity == PainSeverity.STRONG) {
            return getString(R.string.period_modal_pain_strong);
        }
        return getString(R.string.period_modal_pain_medium);
    }

    private String symptomsLabel(@NonNull PeriodDayEntry entry) {
        List<String> labels = new ArrayList<>();
        if (entry.isSymptomIllness() || entry.hasIllness()) {
            labels.add(getString(R.string.period_modal_illness));
        }
        if (entry.isSymptomNausea()) {
            labels.add(getString(R.string.period_modal_symptom_nausea));
        }
        if (entry.isSymptomFatigue()) {
            labels.add(getString(R.string.period_modal_symptom_fatigue));
        }
        if (entry.isSymptomDizziness()) {
            labels.add(getString(R.string.period_modal_symptom_dizziness));
        }
        if (entry.isSymptomDiarrhea()) {
            labels.add(getString(R.string.period_modal_symptom_diarrhea));
        }
        if (labels.isEmpty()) {
            return "-";
        }
        return android.text.TextUtils.join(", ", labels);
    }

    private String markersLabel(@NonNull PeriodDayEntry entry) {
        List<String> labels = new ArrayList<>();
        if (entry.isStart()) {
            labels.add(getString(R.string.period_modal_start_marker));
        }
        if (entry.isEnd()) {
            labels.add(getString(R.string.period_modal_end_marker));
        }
        if (labels.isEmpty()) {
            return "-";
        }
        return android.text.TextUtils.join(", ", labels);
    }

    private void applyAccentColor(@Nullable TextView title,
                                  @Nullable TextView rangeLabel,
                                  @Nullable TextView filterLabel,
                                  @Nullable ImageButton closeButton,
                                  @Nullable MaterialButton savePdfButton,
                                  @Nullable EditText searchField) {
        if (viewModel == null) {
            return;
        }
        Integer color = viewModel.getButtonColor().getValue();
        if (title != null && color != null) {
            title.setTextColor(color);
        } else if (title != null) {
            title.setTextColor(Color.WHITE);
        }
        if (rangeLabel != null && color != null) {
            rangeLabel.setTextColor(color);
        }
        if (filterLabel != null && color != null) {
            filterLabel.setTextColor(color);
        }
        if (closeButton != null && color != null) {
            closeButton.setImageTintList(null);
            closeButton.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            closeButton.setAlpha(1f);
        }
        if (savePdfButton != null && color != null) {
            ButtonColorHelper.applyPrimaryColor(savePdfButton, color);
            savePdfButton.setTextColor(Color.WHITE);
        }
        if (searchField != null) {
            searchField.setTextColor(Color.WHITE);
            searchField.setHintTextColor(0xFFBDBDBD);
        }
    }
}
