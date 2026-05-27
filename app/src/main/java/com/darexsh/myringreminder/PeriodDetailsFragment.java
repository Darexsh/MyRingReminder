package com.darexsh.myringreminder;

import android.content.ContentResolver;
import android.content.ClipData;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
        ImageButton closeButton = view.findViewById(R.id.btn_close_period_details);
        MaterialButton savePdfButton = view.findViewById(R.id.btn_period_details_save_pdf);
        LinearLayout listContainer = view.findViewById(R.id.layout_period_details_list);
        ChipGroup rangeGroup = view.findViewById(R.id.chip_group_period_range);

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
        applyAccentColor(title, closeButton, savePdfButton);
        return view;
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
        }

        if (monthLabel != null) {
            visibleRangeLabel = buildRangeLabel(rangeStart, rangeEnd);
            monthLabel.setText(visibleRangeLabel);
        } else {
            visibleRangeLabel = buildRangeLabel(rangeStart, rangeEnd);
        }

        Map<String, PeriodDayEntry> allEntries = viewModel.getRepository().getAllPeriodDayEntries();
        List<Map.Entry<String, PeriodDayEntry>> monthEntries = new ArrayList<>();
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
        final int margin = 36;
        final int contentWidth = pageWidth - (margin * 2);
        final int accentColor = viewModel != null && viewModel.getButtonColor().getValue() != null
                ? viewModel.getButtonColor().getValue()
                : 0xFF2E7D32;

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(accentColor);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextSize(20f);

        Paint sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(accentColor);
        sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        sectionPaint.setTextSize(13f);

        Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalPaint.setColor(Color.BLACK);
        normalPaint.setTextSize(10.5f);

        Paint subtlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        subtlePaint.setColor(0xFF4E4E4E);
        subtlePaint.setTextSize(9.5f);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFFD6D6D6);
        linePaint.setStrokeWidth(1f);
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        PdfState state = startPdfPage(document, pageWidth, pageHeight, 1, margin);
        state.y = drawPdfHeader(state.canvas, margin, state.y, contentWidth, titlePaint, subtlePaint, linePaint);
        state.y += 8f;

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

        state = ensurePageSpace(document, state, 56f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
        state.canvas.drawText(getString(R.string.period_details_pdf_summary_title), margin, state.y, sectionPaint);
        state.y += 14f;
        state.canvas.drawText(getString(R.string.period_details_pdf_summary_entries, totalEntries), margin, state.y, normalPaint);
        state.y += 12f;
        state.canvas.drawText(getString(R.string.period_details_pdf_summary_starts, startCount), margin, state.y, normalPaint);
        state.y += 12f;
        state.canvas.drawText(getString(R.string.period_details_pdf_summary_ends, endCount), margin, state.y, normalPaint);
        state.y += 12f;
        state.canvas.drawText(getString(R.string.period_details_pdf_summary_pain_days, painCount), margin, state.y, normalPaint);
        state.y += 14f;
        state.canvas.drawLine(margin, state.y, pageWidth - margin, state.y, linePaint);
        state.y += 12f;

        state = ensurePageSpace(document, state, 64f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
        state.canvas.drawText(getString(R.string.period_details_pdf_legend_title), margin, state.y, sectionPaint);
        state.y += 14f;
        state.y = drawLegendRow(state.canvas, margin, state.y, getString(R.string.period_modal_intensity_light), 0x88EF9A9A, subtlePaint, badgePaint);
        state.y = drawLegendRow(state.canvas, margin, state.y, getString(R.string.period_modal_intensity_medium), 0x99EF5350, subtlePaint, badgePaint);
        state.y = drawLegendRow(state.canvas, margin, state.y, getString(R.string.period_modal_intensity_heavy), 0xCCB71C1C, subtlePaint, badgePaint);
        state.y = drawLegendRow(state.canvas, margin, state.y, getString(R.string.period_details_pdf_legend_pain_badge), 0xFFFFB300, subtlePaint, badgePaint);
        state.y = drawLegendRow(state.canvas, margin, state.y, getString(R.string.period_details_pdf_legend_symptom_badge), 0xFF40C4FF, subtlePaint, badgePaint);
        state.y += 4f;
        state.canvas.drawLine(margin, state.y, pageWidth - margin, state.y, linePaint);
        state.y += 12f;

        int longStreak = findLongestPeriodStreak();
        int symptomDays = countSymptomDays();
        int strongPainDays = countStrongPainDays();
        state = ensurePageSpace(document, state, 56f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
        state.canvas.drawText(getString(R.string.period_details_pdf_anomalies_title), margin, state.y, sectionPaint);
        state.y += 14f;
        state.canvas.drawText(getString(R.string.period_details_pdf_anomalies_longest_streak, longStreak), margin, state.y, subtlePaint);
        state.y += 12f;
        state.canvas.drawText(getString(R.string.period_details_pdf_anomalies_symptom_days, symptomDays), margin, state.y, subtlePaint);
        state.y += 12f;
        state.canvas.drawText(getString(R.string.period_details_pdf_anomalies_strong_pain_days, strongPainDays), margin, state.y, subtlePaint);
        state.y += 10f;
        state.canvas.drawLine(margin, state.y, pageWidth - margin, state.y, linePaint);
        state.y += 12f;

        final float gridGap = 10f;
        final float cardWidth = (contentWidth - gridGap) / 2f;
        final float cardPadding = 8f;
        final Paint cardBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBgPaint.setColor(0xFFF8F8F8);
        final Paint cardBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardBorderPaint.setColor(0xFFD6D6D6);
        cardBorderPaint.setStyle(Paint.Style.STROKE);
        cardBorderPaint.setStrokeWidth(1f);
        final float cardCorner = 6f;

        SimpleDateFormat dayFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        int index = 0;
        while (index < visibleEntries.size()) {
            String month = visibleEntries.get(index).monthHeader;
            state = ensurePageSpace(document, state, 24f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
            state.canvas.drawText(month, margin, state.y, sectionPaint);
            state.y += 14f;

            List<DisplayEntry> monthEntries = new ArrayList<>();
            while (index < visibleEntries.size() && month.equals(visibleEntries.get(index).monthHeader)) {
                monthEntries.add(visibleEntries.get(index));
                index++;
            }

            for (int i = 0; i < monthEntries.size(); i += 2) {
                DisplayEntry left = monthEntries.get(i);
                DisplayEntry right = i + 1 < monthEntries.size() ? monthEntries.get(i + 1) : null;

                float leftHeight = estimateEntryCardHeight(left, dayFormat, normalPaint, subtlePaint, cardWidth, cardPadding);
                float rightHeight = right != null
                        ? estimateEntryCardHeight(right, dayFormat, normalPaint, subtlePaint, cardWidth, cardPadding)
                        : 0f;
                float rowHeight = Math.max(leftHeight, rightHeight);

                state = ensurePageSpace(document, state, rowHeight + 8f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
                drawEntryCard(state.canvas, left, dayFormat, margin, state.y, cardWidth, rowHeight, cardPadding,
                        cardCorner, cardBgPaint, cardBorderPaint, normalPaint, subtlePaint, badgePaint);
                if (right != null) {
                    drawEntryCard(state.canvas, right, dayFormat, margin + cardWidth + gridGap, state.y, cardWidth,
                            rowHeight, cardPadding, cardCorner, cardBgPaint, cardBorderPaint, normalPaint, subtlePaint, badgePaint);
                }
                state.y += rowHeight + 8f;
            }
            state = ensurePageSpace(document, state, 52f, margin, pageWidth, pageHeight, linePaint, subtlePaint);
            state.canvas.drawText(getString(R.string.period_details_pdf_notes_title), margin, state.y, sectionPaint);
            state.y += 10f;
            for (int n = 0; n < 3; n++) {
                state.canvas.drawLine(margin, state.y, margin + contentWidth, state.y, linePaint);
                state.y += 12f;
            }
            state.y += 4f;
        }

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
                                @NonNull Paint subtlePaint,
                                @NonNull Paint linePaint) {
        int iconSize = dpToPx(24);
        android.graphics.drawable.Drawable icon = ContextCompat.getDrawable(requireContext(), R.mipmap.ic_launcher);
        if (icon != null) {
            icon.setBounds(margin, Math.round(y - 18f), margin + iconSize, Math.round(y - 18f) + iconSize);
            icon.draw(canvas);
        }

        float textStartX = margin + iconSize + dpToPx(12);
        canvas.drawText(getString(R.string.app_info_name), textStartX, y, titlePaint);
        y += 16f;
        canvas.drawText(getString(R.string.app_info_developer_label) + " " + getString(R.string.app_info_developer_name), textStartX, y, subtlePaint);
        y += 16f;
        canvas.drawText(getString(R.string.period_details_range_label) + ": " + visibleRangeLabel, textStartX, y, subtlePaint);
        y += 12f;
        String created = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date());
        canvas.drawText(getString(R.string.period_details_pdf_created_at, created), textStartX, y, subtlePaint);
        y += 10f;
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

    private float drawLegendRow(@NonNull android.graphics.Canvas canvas,
                                float x,
                                float y,
                                @NonNull String label,
                                int color,
                                @NonNull Paint textPaint,
                                @NonNull Paint badgePaint) {
        badgePaint.setColor(color);
        canvas.drawCircle(x + 4f, y - 3f, 3.8f, badgePaint);
        canvas.drawText(label, x + 14f, y, textPaint);
        return y + 11f;
    }

    private float estimateWrappedBlockHeight(@NonNull String text, @NonNull Paint paint, float maxWidth) {
        List<String> lines = wrapText(text, paint, maxWidth);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float lineHeight = (metrics.descent - metrics.ascent) + 1.5f;
        return (lines.size() * lineHeight) + 1f;
    }

    private float estimateEntryCardHeight(@NonNull DisplayEntry displayEntry,
                                          @NonNull SimpleDateFormat dayFormat,
                                          @NonNull Paint normalPaint,
                                          @NonNull Paint subtlePaint,
                                          float cardWidth,
                                          float cardPadding) {
        float innerWidth = cardWidth - (cardPadding * 2f);
        String dateText = dayFormat.format(displayEntry.day.getTime());
        String intensityText = getString(R.string.period_modal_intensity_title) + ": " + intensityLabel(displayEntry.entry.getIntensity());
        String painText = getString(R.string.period_modal_pain_title) + ": " + painLabel(displayEntry.entry.getPainSeverity());
        String symptomsText = getString(R.string.period_modal_symptoms_title) + ": " + symptomsLabel(displayEntry.entry);
        String markersText = getString(R.string.period_modal_markers_title) + ": " + markersLabel(displayEntry.entry);
        return estimateWrappedBlockHeight(dateText, normalPaint, innerWidth)
                + estimateWrappedBlockHeight(intensityText, subtlePaint, innerWidth)
                + estimateWrappedBlockHeight(painText, subtlePaint, innerWidth)
                + estimateWrappedBlockHeight(symptomsText, subtlePaint, innerWidth)
                + estimateWrappedBlockHeight(markersText, subtlePaint, innerWidth)
                + cardPadding * 2f + 4f;
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
                               @NonNull Paint normalPaint,
                               @NonNull Paint subtlePaint,
                               @NonNull Paint badgePaint) {
        android.graphics.RectF rect = new android.graphics.RectF(x, y, x + cardWidth, y + cardHeight);
        canvas.drawRoundRect(rect, cardCorner, cardCorner, bgPaint);
        canvas.drawRoundRect(rect, cardCorner, cardCorner, borderPaint);

        float cursorY = y + cardPadding + 2f;
        float innerX = x + cardPadding;
        float innerWidth = cardWidth - (cardPadding * 2f);

        String dateText = dayFormat.format(displayEntry.day.getTime());
        String intensityText = getString(R.string.period_modal_intensity_title) + ": " + intensityLabel(displayEntry.entry.getIntensity());
        String painText = getString(R.string.period_modal_pain_title) + ": " + painLabel(displayEntry.entry.getPainSeverity());
        String symptomsText = getString(R.string.period_modal_symptoms_title) + ": " + symptomsLabel(displayEntry.entry);
        String markersText = getString(R.string.period_modal_markers_title) + ": " + markersLabel(displayEntry.entry);

        cursorY = drawWrappedText(canvas, dateText, innerX, cursorY, innerWidth, normalPaint);
        badgePaint.setColor(intensityBadgeColor(displayEntry.entry.getIntensity()));
        cursorY = drawWrappedTextWithBadge(canvas, intensityText, innerX, cursorY, innerWidth, subtlePaint, badgePaint);
        badgePaint.setColor(painBadgeColor(displayEntry.entry.getPainSeverity()));
        cursorY = drawWrappedTextWithBadge(canvas, painText, innerX, cursorY, innerWidth, subtlePaint, badgePaint);
        badgePaint.setColor((displayEntry.entry.hasAnyAdditionalSymptoms() || displayEntry.entry.hasIllness()) ? 0xFF40C4FF : 0xFFBDBDBD);
        cursorY = drawWrappedTextWithBadge(canvas, symptomsText, innerX, cursorY, innerWidth, subtlePaint, badgePaint);
        badgePaint.setColor((displayEntry.entry.isStart() || displayEntry.entry.isEnd()) ? 0xFF8E24AA : 0xFFBDBDBD);
        drawWrappedTextWithBadge(canvas, markersText, innerX, cursorY, innerWidth, subtlePaint, badgePaint);
    }

    private float drawWrappedTextWithBadge(@NonNull android.graphics.Canvas canvas,
                                           @NonNull String text,
                                           float x,
                                           float y,
                                           float maxWidth,
                                           @NonNull Paint textPaint,
                                           @NonNull Paint badgePaint) {
        float badgeX = x + 3f;
        float textX = x + 10f;
        float textWidth = maxWidth - 10f;
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = y;
        canvas.drawCircle(badgeX, baseline - 3f, 2.5f, badgePaint);
        List<String> lines = wrapText(text, textPaint, textWidth);
        float lineHeight = (metrics.descent - metrics.ascent) + 1.5f;
        for (String line : lines) {
            canvas.drawText(line, textX, baseline, textPaint);
            baseline += lineHeight;
        }
        return baseline + 1f;
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
        float lineHeight = (metrics.descent - metrics.ascent) + 1.5f;
        float baseline = y;
        for (String line : lines) {
            canvas.drawText(line, x, baseline, paint);
            baseline += lineHeight;
        }
        return baseline + 1f;
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
                                  @Nullable ImageButton closeButton,
                                  @Nullable MaterialButton savePdfButton) {
        if (viewModel == null) {
            return;
        }
        Integer color = viewModel.getButtonColor().getValue();
        if (title != null && color != null) {
            title.setTextColor(color);
        } else if (title != null) {
            title.setTextColor(Color.WHITE);
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
    }
}
