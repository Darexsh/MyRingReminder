package com.darexsh.veri_aristo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Button;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.LocaleListCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import android.content.res.ColorStateList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.Set;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Executor;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// SettingsFragment allows users to configure app settings such as cycle start date, time, length, and background image
public class SettingsFragment extends Fragment {

    private MaterialButton btnSetTime;
    private MaterialButton btnSetStartDate;
    private MaterialButton btnSetCycleLength;
    private MaterialButton btnSetBackground;
    private MaterialButton btnSetCalendarRange;
    private MaterialButton btnBackupManage;
    private MaterialButton btnUpdateApp;
    private MaterialButton btnNotificationGroup;
    private MaterialButton btnSetLanguage;
    private MaterialButton btnSetButtonColor;
    private MaterialButton btnSetCircleColor;
    private MaterialButton btnSetCircleStyle;
    private MaterialButton btnSetNavigationAnimation;
    private MaterialButton btnAppLock;
    private MaterialButton btnWelcomeTour;
    private View debugSection;
    private TextView tvDebugTimeStatus;
    private SwitchMaterial switchDebugTime;
    private View advancedContent;
    private android.widget.ImageButton btnAdvancedToggle;
    private TextView btnSettingsInfo;
    private SharedViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<String> requestNotificationPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> createBackupLauncher;
    private ActivityResultLauncher<String[]> restoreBackupLauncher;
    private ActivityResultLauncher<Intent> manageUnknownSourcesLauncher;
    private int[] buttonColorValues;
    private String[] buttonColorLabels;
    private String[] circleStyleLabels;
    private String[] navigationAnimationLabels;
    private int[] navigationAnimationValues;
    private String[] appLockTimeoutLabels;
    private int[] appLockTimeoutValues;
    private Drawable[] circleStylePreviews;
    private static final String RELEASES_URL = "https://api.github.com/repos/Darexsh/Veri_Aristo_App/releases";
    private File pendingApkFile;
    private AlertDialog downloadDialog;
    private ProgressBar downloadProgressBar;
    private TextView downloadProgressText;
    private AlertDialog notificationToolsDialog;

    private interface ColorConsumer {
        void accept(int color);
    }


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize ActivityResultLauncher for permission requests
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                openGallery();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.settings_permission_title)
                        .setMessage(R.string.settings_permission_message)
                        .setPositiveButton(R.string.dialog_ok, null)
                        .show();
                applyDialogButtonColors(dialog);
            }
        });

        requestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        showToast(R.string.notification_permission_granted);
                    } else if (isAdded()) {
                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.notification_permission_required_title)
                                .setMessage(R.string.notification_permission_required_message)
                                .setPositiveButton(R.string.notification_open_settings, (d, w) -> openAppNotificationSettings())
                                .setNegativeButton(R.string.dialog_cancel, null)
                                .show();
                        applyDialogButtonColors(dialog);
                        showToast(R.string.notification_permission_denied);
                    }
                    updateExactAlarmButtonLabel(notificationToolsDialog);
                }
        );

        // Initialize ActivityResultLauncher for picking images
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != Activity.RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    String savedUri = saveBackgroundImage(uri);
                    if (savedUri != null) {
                        viewModel.setBackgroundImageUri(savedUri);
                    }
                }
        );

        createBackupLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) {
                writeBackup(uri);
            }
        });

        restoreBackupLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) {
                readBackup(uri);
            }
        });

        manageUnknownSourcesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (requireContext().getPackageManager().canRequestPackageInstalls() && pendingApkFile != null) {
                        promptInstall(pendingApkFile);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView settingsTitle = view.findViewById(R.id.tv_settings_title);
        btnSettingsInfo = view.findViewById(R.id.btn_settings_info);
        btnSetTime = view.findViewById(R.id.btn_set_time);
        btnSetStartDate = view.findViewById(R.id.btn_set_start_date);
        btnSetCycleLength = view.findViewById(R.id.btn_set_cycle_length);
        btnSetBackground = view.findViewById(R.id.btn_set_background);
        btnSetCalendarRange = view.findViewById(R.id.btn_set_calendar_range);
        MaterialButton btnResetApp = view.findViewById(R.id.btn_reset_app);
        btnBackupManage = view.findViewById(R.id.btn_backup_manage);
        btnUpdateApp = view.findViewById(R.id.btn_update_app);
        btnAppLock = view.findViewById(R.id.btn_app_lock);
        btnWelcomeTour = view.findViewById(R.id.btn_welcome_tour);
        advancedContent = view.findViewById(R.id.advanced_content);
        View advancedHeader = view.findViewById(R.id.advanced_header);
        btnAdvancedToggle = view.findViewById(R.id.btn_advanced_toggle);
        btnNotificationGroup = view.findViewById(R.id.btn_notification_group);
        btnSetLanguage = view.findViewById(R.id.btn_set_language);
        btnSetButtonColor = view.findViewById(R.id.btn_set_button_color);
        btnSetCircleColor = view.findViewById(R.id.btn_set_circle_color);
        btnSetCircleStyle = view.findViewById(R.id.btn_set_circle_style);
        btnSetNavigationAnimation = view.findViewById(R.id.btn_set_navigation_animation);
        debugSection = view.findViewById(R.id.debug_section);
        tvDebugTimeStatus = view.findViewById(R.id.tv_debug_time_status);
        switchDebugTime = view.findViewById(R.id.switch_debug_time);
        MaterialButton btnDebugSetTime = view.findViewById(R.id.btn_debug_set_time);
        MaterialButton btnDebugClearTime = view.findViewById(R.id.btn_debug_clear_time);
        MaterialButton btnDebugInfo = view.findViewById(R.id.btn_debug_info);
        MaterialButton btnDebugRefresh = view.findViewById(R.id.btn_debug_refresh);
        MaterialButton btnDebugJumpMidnight = view.findViewById(R.id.btn_debug_jump_midnight);
        MaterialButton btnDebugMinusDay = view.findViewById(R.id.btn_debug_minus_day);
        MaterialButton btnDebugPlusDay = view.findViewById(R.id.btn_debug_plus_day);
        MaterialButton btnDebugMinusHour = view.findViewById(R.id.btn_debug_minus_hour);
        MaterialButton btnDebugPlusHour = view.findViewById(R.id.btn_debug_plus_hour);
        MaterialButton btnDebugPresetRemovalBefore = view.findViewById(R.id.btn_debug_preset_removal_before);
        MaterialButton btnDebugPresetRemovalAt = view.findViewById(R.id.btn_debug_preset_removal_at);
        MaterialButton btnDebugPresetInsertionBefore = view.findViewById(R.id.btn_debug_preset_insertion_before);
        MaterialButton btnDebugPresetInsertionAt = view.findViewById(R.id.btn_debug_preset_insertion_at);

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);

        updateDebugSectionVisibility();
        switchDebugTime.setChecked(viewModel.getRepository().isDebugTimeEnabled());
        updateDebugTimeStatus();

        viewModel.getStartDate().observe(getViewLifecycleOwner(), calendar -> {
            if (calendar != null) {
                updateDateButtonText(calendar);
                updateTimeButtonText(calendar);
            }
        });

        viewModel.getCycleLength().observe(getViewLifecycleOwner(), length -> {
            if (length != null) {
                updateCycleLengthButtonText(length);
            }
        });

        viewModel.getCalendarPastAmount().observe(getViewLifecycleOwner(), amount -> {
            String unit = viewModel.getCalendarPastUnit().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(amount, unit, futureAmount, futureUnit);
        });

        viewModel.getCalendarPastUnit().observe(getViewLifecycleOwner(), unit -> {
            Integer amount = viewModel.getCalendarPastAmount().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(amount, unit, futureAmount, futureUnit);
        });

        viewModel.getCalendarFutureAmount().observe(getViewLifecycleOwner(), amount -> {
            Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
            String pastUnit = viewModel.getCalendarPastUnit().getValue();
            String futureUnit = viewModel.getCalendarFutureUnit().getValue();
            updateCalendarRangeButtonText(pastAmount, pastUnit, amount, futureUnit);
        });

        viewModel.getCalendarFutureUnit().observe(getViewLifecycleOwner(), unit -> {
            Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
            String pastUnit = viewModel.getCalendarPastUnit().getValue();
            Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
            updateCalendarRangeButtonText(pastAmount, pastUnit, futureAmount, unit);
        });

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                applyPrimaryButtonColor(color);
                updateButtonColorButtonText();
                btnSettingsInfo.setTextColor(Color.WHITE);
                if (btnSettingsInfo.getBackground() != null) {
                    btnSettingsInfo.getBackground().setTint(color);
                }
            }
        });

        viewModel.getHomeCircleColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                updateCircleColorButtonText();
                circleStylePreviews = null;
            }
        });

        viewModel.getHomeCircleStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                updateCircleStyleButtonText(style);
            }
        });

        viewModel.getNavigationAnimationStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                updateNavigationAnimationButtonText(style);
            }
        });


        // Set up button click listeners
        btnSetTime.setOnClickListener(v -> showTimePicker());
        btnSetStartDate.setOnClickListener(v -> showDatePicker());
        btnSetCycleLength.setOnClickListener(v -> showCycleLengthDialog());
        btnSetBackground.setOnClickListener(v -> checkStoragePermission());
        btnSetCalendarRange.setOnClickListener(v -> showCalendarRangeDialog());
        btnResetApp.setOnClickListener(v -> showResetDialog());
        btnBackupManage.setOnClickListener(v -> showBackupDialog());
        btnNotificationGroup.setOnClickListener(v -> showNotificationToolsDialog());
        btnSetLanguage.setOnClickListener(v -> showLanguageDialog());
        btnSetButtonColor.setOnClickListener(v -> showButtonColorDialog());
        btnSetCircleColor.setOnClickListener(v -> showCircleColorDialog());
        btnSetCircleStyle.setOnClickListener(v -> showCircleStyleDialog());
        btnSetNavigationAnimation.setOnClickListener(v -> showNavigationAnimationDialog());
        btnAppLock.setOnClickListener(v -> showAppLockToolsDialog());
        btnWelcomeTour.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).restartWelcomeTour();
            }
        });
        switchDebugTime.setOnCheckedChangeListener((buttonView, isChecked) -> {
            viewModel.getRepository().setDebugTimeEnabled(isChecked);
            updateDebugTimeStatus();
            Calendar current = viewModel.getStartDate().getValue();
            if (current != null) {
                viewModel.setStartDate((Calendar) current.clone());
            }
            WidgetUpdater.updateAllWidgets(requireContext());
        });
        btnDebugSetTime.setOnClickListener(v -> showDebugTimePicker());
        btnDebugClearTime.setOnClickListener(v -> {
            viewModel.getRepository().clearDebugTimeMillis();
            viewModel.getRepository().setDebugTimeEnabled(false);
            switchDebugTime.setChecked(false);
            updateDebugTimeStatus();
            WidgetUpdater.updateAllWidgets(requireContext());
            Toast.makeText(requireContext(), getString(R.string.debug_time_cleared), Toast.LENGTH_SHORT).show();
        });
        btnDebugInfo.setOnClickListener(v -> showDebugDialog());
        btnDebugRefresh.setOnClickListener(v -> {
            updateDebugTimeStatus();
            Calendar current = viewModel.getStartDate().getValue();
            if (current != null) {
                viewModel.setStartDate((Calendar) current.clone());
            }
            WidgetUpdater.updateAllWidgets(requireContext());
            Toast.makeText(requireContext(), getString(R.string.debug_refreshed), Toast.LENGTH_SHORT).show();
        });
        btnDebugJumpMidnight.setOnClickListener(v -> adjustDebugTimeToMidnight());
        btnDebugMinusDay.setOnClickListener(v -> shiftDebugTime(Calendar.DAY_OF_MONTH, -1));
        btnDebugPlusDay.setOnClickListener(v -> shiftDebugTime(Calendar.DAY_OF_MONTH, 1));
        btnDebugMinusHour.setOnClickListener(v -> shiftDebugTime(Calendar.HOUR_OF_DAY, -1));
        btnDebugPlusHour.setOnClickListener(v -> shiftDebugTime(Calendar.HOUR_OF_DAY, 1));
        btnDebugPresetRemovalBefore.setOnClickListener(v -> applyDebugPreset(DebugPreset.REMOVAL_BEFORE));
        btnDebugPresetRemovalAt.setOnClickListener(v -> applyDebugPreset(DebugPreset.REMOVAL_AT));
        btnDebugPresetInsertionBefore.setOnClickListener(v -> applyDebugPreset(DebugPreset.INSERTION_BEFORE));
        btnDebugPresetInsertionAt.setOnClickListener(v -> applyDebugPreset(DebugPreset.INSERTION_AT));

        bindDebugHint(btnDebugSetTime, R.string.debug_hint_set_time);
        bindDebugHint(btnDebugClearTime, R.string.debug_hint_clear_time);
        bindDebugHint(btnDebugInfo, R.string.debug_hint_info);
        bindDebugHint(btnDebugRefresh, R.string.debug_hint_refresh);
        bindDebugHint(btnDebugJumpMidnight, R.string.debug_hint_jump_midnight);
        bindDebugHint(btnDebugMinusDay, R.string.debug_hint_minus_day);
        bindDebugHint(btnDebugPlusDay, R.string.debug_hint_plus_day);
        bindDebugHint(btnDebugMinusHour, R.string.debug_hint_minus_hour);
        bindDebugHint(btnDebugPlusHour, R.string.debug_hint_plus_hour);
        bindDebugHint(btnDebugPresetRemovalBefore, R.string.debug_hint_preset_removal_before);
        bindDebugHint(btnDebugPresetRemovalAt, R.string.debug_hint_preset_removal_at);
        bindDebugHint(btnDebugPresetInsertionBefore, R.string.debug_hint_preset_insertion_before);
        bindDebugHint(btnDebugPresetInsertionAt, R.string.debug_hint_preset_insertion_at);
        btnUpdateApp.setOnClickListener(v -> showUpdateBackupConfirmDialog());
        advancedHeader.setOnClickListener(v -> toggleAdvancedSection());
        btnAdvancedToggle.setOnClickListener(v -> toggleAdvancedSection());
        btnSettingsInfo.setOnClickListener(v -> showAppInfoDialog());
        settingsTitle.setOnLongClickListener(v -> {
            AlertDialog dialog = DebugToolsDialog.show(requireContext(), viewModel.getRepository(),
                    enabled -> updateDebugSectionVisibility());
            applyDialogButtonColors(dialog);
            return true;
        });

        updateLanguageButtonText();
        updateAppLockButtonText();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAppLockButtonText();
        if (notificationToolsDialog != null && notificationToolsDialog.isShowing()) {
            updateExactAlarmButtonLabel(notificationToolsDialog);
        }
    }

    // Check if the app has permission to read media images, and open the gallery if granted
    private void checkStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    // Open the gallery to select a background image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    @Nullable
    private String saveBackgroundImage(@NonNull Uri sourceUri) {
        File directory = new File(requireContext().getFilesDir(), "backgrounds");
        if (!directory.exists() && !directory.mkdirs()) {
            return null;
        }
        File targetFile = new File(directory, "background_image");
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(sourceUri);
             OutputStream outputStream = new FileOutputStream(targetFile)) {
            if (inputStream == null) {
                return null;
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            return Uri.fromFile(targetFile).toString();
        } catch (IOException e) {
            return null;
        }
    }

    private void updateTimeButtonText(Calendar calendar) {
        String timeText = getString(R.string.settings_time_button_format,
                calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        btnSetTime.setText(timeText);
    }

    private void updateDateButtonText(Calendar calendar) {
        String dateText = getString(R.string.settings_date_button_format,
                calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.YEAR));
        btnSetStartDate.setText(dateText);
    }

    private void updateCycleLengthButtonText(int cycleLength) {
        String text = getString(R.string.settings_cycle_length_button, cycleLength);
        btnSetCycleLength.setText(text);
    }

    private void updateCalendarRangeButtonText(Integer pastAmount, String pastUnit,
                                               Integer futureAmount, String futureUnit) {
        if (pastAmount == null || pastUnit == null || futureAmount == null || futureUnit == null) {
            return;
        }
        String text = getString(R.string.settings_calendar_range_button,
                pastAmount, unitLabel(pastUnit), futureAmount, unitLabel(futureUnit));
        btnSetCalendarRange.setText(text);
    }

    // Show a TimePickerDialog to select the time
    private void showTimePicker() {
        final Calendar currentCalendar = viewModel.getStartDate().getValue() != null
                ? (Calendar) viewModel.getStartDate().getValue().clone()
                : Calendar.getInstance();

        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (TimePicker view, int selectedHour, int selectedMinute) -> {
            currentCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            currentCalendar.set(Calendar.MINUTE, selectedMinute);
            viewModel.setStartDate(currentCalendar);
            WidgetUpdater.updateAllWidgets(requireContext());
        }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), true);

        dialog.show();
        applyDialogButtonColors(dialog);
    }

    // Show a DatePickerDialog to select the start date
    private void showDatePicker() {
        final Calendar currentCalendar = viewModel.getStartDate().getValue() != null
                ? (Calendar) viewModel.getStartDate().getValue().clone()
                : Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (DatePicker view, int selectedYear, int selectedMonth, int selectedDay) -> {
                    currentCalendar.set(selectedYear, selectedMonth, selectedDay);
                    viewModel.setStartDate(currentCalendar);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH));

        dialog.show();
        applyDialogButtonColors(dialog);
    }

    // Show a dialog to set the cycle length
    private void showCycleLengthDialog() {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(14);
        picker.setMaxValue(35);

        Integer currentCycleLength = viewModel.getCycleLength().getValue();
        if (currentCycleLength != null) {
            int clamped = Math.max(14, Math.min(35, currentCycleLength));
            picker.setValue(clamped);
        } else {
            picker.setValue(21);
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(picker);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_cycle_length_title)
                .setMessage(R.string.settings_cycle_length_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    viewModel.setCycleLength(picker.getValue());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showCalendarRangeDialog() {
        NumberPicker pastValuePicker = new NumberPicker(requireContext());
        NumberPicker pastUnitPicker = new NumberPicker(requireContext());
        NumberPicker futureValuePicker = new NumberPicker(requireContext());
        NumberPicker futureUnitPicker = new NumberPicker(requireContext());

        String[] unitValues = new String[]{getString(R.string.settings_unit_month), getString(R.string.settings_unit_year)};
        pastUnitPicker.setDisplayedValues(unitValues);
        pastUnitPicker.setMinValue(0);
        pastUnitPicker.setMaxValue(unitValues.length - 1);

        futureUnitPicker.setDisplayedValues(unitValues);
        futureUnitPicker.setMinValue(0);
        futureUnitPicker.setMaxValue(unitValues.length - 1);

        Integer currentPastAmount = viewModel.getCalendarPastAmount().getValue();
        String currentPastUnit = viewModel.getCalendarPastUnit().getValue();
        Integer currentFutureAmount = viewModel.getCalendarFutureAmount().getValue();
        String currentFutureUnit = viewModel.getCalendarFutureUnit().getValue();

        pastUnitPicker.setValue("years".equals(currentPastUnit) ? 1 : 0);
        futureUnitPicker.setValue("months".equals(currentFutureUnit) ? 0 : 1);

        configureValuePicker(pastValuePicker, pastUnitPicker.getValue(), currentPastAmount);
        configureValuePicker(futureValuePicker, futureUnitPicker.getValue(), currentFutureAmount);

        pastUnitPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                configureValuePicker(pastValuePicker, newVal, pastValuePicker.getValue()));
        futureUnitPicker.setOnValueChangedListener((picker, oldVal, newVal) ->
                configureValuePicker(futureValuePicker, newVal, futureValuePicker.getValue()));

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(buildPickerRow(getString(R.string.settings_calendar_range_back), pastValuePicker, pastUnitPicker));
        layout.addView(buildPickerRow(getString(R.string.settings_calendar_range_forward), futureValuePicker, futureUnitPicker));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_calendar_range_title)
                .setMessage(R.string.settings_calendar_range_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    int pastAmount = pastValuePicker.getValue();
                    String pastUnit = pastUnitPicker.getValue() == 1 ? "years" : "months";
                    int futureAmount = futureValuePicker.getValue();
                    String futureUnit = futureUnitPicker.getValue() == 1 ? "years" : "months";
                    viewModel.setCalendarPastRange(pastAmount, pastUnit);
                    viewModel.setCalendarFutureRange(futureAmount, futureUnit);
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showNotificationTimesDialog() {
        NumberPicker removalPicker = new NumberPicker(requireContext());
        NumberPicker insertionPicker = new NumberPicker(requireContext());
        removalPicker.setMinValue(0);
        removalPicker.setMaxValue(24);
        insertionPicker.setMinValue(0);
        insertionPicker.setMaxValue(24);

        Integer currentRemoval = viewModel.getRemovalReminderHours().getValue();
        Integer currentInsertion = viewModel.getInsertionReminderHours().getValue();
        removalPicker.setValue(currentRemoval != null ? currentRemoval : 6);
        insertionPicker.setValue(currentInsertion != null ? currentInsertion : 6);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(buildPickerRow(getString(R.string.settings_notification_before_removal), removalPicker, null));
        layout.addView(buildPickerRow(getString(R.string.settings_notification_before_insertion), insertionPicker, null));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_notification_times_title)
                .setMessage(R.string.settings_notification_times_message)
                .setView(layout)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> {
                    viewModel.setRemovalReminderHours(removalPicker.getValue());
                    viewModel.setInsertionReminderHours(insertionPicker.getValue());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showLanguageDialog() {
        String[] options = {
                getString(R.string.language_system_default),
                getString(R.string.language_german),
                getString(R.string.language_english)
        };
        int selected = getSelectedLanguageIndex();
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.language_title)
                .setSingleChoiceItems(options, selected, (dlg, which) -> {
                    if (which == 0) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
                    } else {
                        String tag = which == 1 ? "de" : "en";
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
                    }
                    WidgetUpdater.updateAllWidgets(requireContext());
                    updateLanguageButtonText();
                    dlg.dismiss();
                    requireActivity().recreate();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void updateLanguageButtonText() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        String label;
        if (locales.isEmpty()) {
            label = getString(R.string.language_system_default);
        } else {
            String language = Objects.requireNonNull(locales.get(0)).getLanguage();
            label = "de".equals(language) ? getString(R.string.language_german) : getString(R.string.language_english);
        }
        btnSetLanguage.setText(getString(R.string.language_button, label));
    }

    private int getSelectedLanguageIndex() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        if (locales.isEmpty()) {
            return 0;
        }
        String language = Objects.requireNonNull(locales.get(0)).getLanguage();
        if ("de".equals(language)) {
            return 1;
        }
        return 2;
    }

    private void showButtonColorDialog() {
        Integer currentColor = viewModel.getButtonColor().getValue();
        int selectedColor = currentColor != null ? currentColor : SettingsRepository.DEFAULT_BUTTON_COLOR;
        showColorDialog(
                R.string.settings_button_color_dialog_title,
                selectedColor,
                R.string.settings_button_color_custom_title,
                R.string.settings_button_color_widget_note,
                color -> {
                    viewModel.setButtonColor(color);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }
        );
    }

    private void showCircleColorDialog() {
        Integer currentColor = viewModel.getHomeCircleColor().getValue();
        int selectedColor = currentColor != null ? currentColor : SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
        showColorDialog(
                R.string.settings_circle_color_dialog_title,
                selectedColor,
                R.string.settings_circle_color_custom_title,
                R.string.settings_circle_color_widget_note,
                color -> {
                    viewModel.setHomeCircleColor(color);
                    WidgetUpdater.updateAllWidgets(requireContext());
                }
        );
    }

    private void showColorDialog(int titleResId, int selectedColor, int customTitleResId, int noteResId, ColorConsumer onSelect) {
        ensureButtonColorOptionsLoaded();
        int selectedIndex = getButtonColorIndex(selectedColor);
        final int[] pendingColor = new int[]{selectedColor};

        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_button_colors);
        MaterialButton customButton = content.findViewById(R.id.btn_custom_color);
        MaterialButton cancelButton = content.findViewById(R.id.btn_cancel_color);
        android.widget.TextView widgetNote = content.findViewById(R.id.tv_color_dialog_note);
        if (widgetNote != null) {
            widgetNote.setText(noteResId);
            widgetNote.setVisibility(View.VISIBLE);
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
                buttonColorLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                View swatch = view.findViewById(R.id.view_color_swatch);
                if (swatch != null) {
                    swatch.setBackgroundColor(buttonColorValues[position]);
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
            pendingColor[0] = buttonColorValues[position];
            onSelect.accept(pendingColor[0]);
            dialog.dismiss();
        });

        customButton.setOnClickListener(v -> {
            dialog.dismiss();
            showCustomColorDialog(customTitleResId, pendingColor[0], onSelect);
        });
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        applyDialogButtonColors(dialog);
    }

    private void showCustomColorDialog(int titleResId, int initialColor, ColorConsumer onSelect) {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_button_color_custom, null);
        HsvColorWheelView colorWheel = content.findViewById(R.id.color_wheel);
        View preview = content.findViewById(R.id.view_color_preview);
        final int[] pendingColor = new int[]{initialColor};
        preview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(initialColor));
        colorWheel.setColor(initialColor);
        colorWheel.setOnColorChangeListener(color -> {
            pendingColor[0] = color;
            preview.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(titleResId)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, (dlg, which) -> onSelect.accept(pendingColor[0]))
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void applyPrimaryButtonColor(int color) {
        ButtonColorHelper.applyPrimaryColor(btnSetTime, color);
        ButtonColorHelper.applyPrimaryColor(btnSetStartDate, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCycleLength, color);
        ButtonColorHelper.applyPrimaryColor(btnSetBackground, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCalendarRange, color);
        ButtonColorHelper.applyPrimaryColor(btnBackupManage, color);
        ButtonColorHelper.applyPrimaryColor(btnUpdateApp, color);
        ButtonColorHelper.applyPrimaryColor(btnAppLock, color);
        ButtonColorHelper.applyPrimaryColor(btnWelcomeTour, color);
        ButtonColorHelper.applyPrimaryColor(btnNotificationGroup, color);
        ButtonColorHelper.applyPrimaryColor(btnSetLanguage, color);
        ButtonColorHelper.applyPrimaryColor(btnSetButtonColor, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCircleColor, color);
        ButtonColorHelper.applyPrimaryColor(btnSetCircleStyle, color);
        ButtonColorHelper.applyPrimaryColor(btnSetNavigationAnimation, color);
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

    private void toggleAdvancedSection() {
        boolean isVisible = advancedContent.getVisibility() == View.VISIBLE;
        animateAdvancedSection(!isVisible);
        btnAdvancedToggle.animate()
                .rotation(isVisible ? 0f : 180f)
                .setDuration(150)
                .start();
    }

    private void animateAdvancedSection(boolean show) {
        float offset = 10f * getResources().getDisplayMetrics().density;
        advancedContent.animate().cancel();

        if (show) {
            advancedContent.setVisibility(View.VISIBLE);
            advancedContent.setAlpha(0f);
            advancedContent.setTranslationY(-offset);
            advancedContent.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(180)
                    .start();
            return;
        }

        advancedContent.animate()
                .alpha(0f)
                .translationY(-offset)
                .setDuration(150)
                .withEndAction(() -> {
                    advancedContent.setVisibility(View.GONE);
                    advancedContent.setAlpha(1f);
                    advancedContent.setTranslationY(0f);
                })
                .start();
    }

    private void updateButtonColorButtonText() {
        btnSetButtonColor.setText(R.string.settings_button_color_format);
    }

    private void updateCircleColorButtonText() {
        btnSetCircleColor.setText(R.string.settings_circle_color_format);
    }

    private void updateCircleStyleButtonText(int style) {
        ensureCircleStyleOptionsLoaded();
        String label = style >= 0 && style < circleStyleLabels.length
                ? circleStyleLabels[style]
                : circleStyleLabels[0];
        btnSetCircleStyle.setText(getString(R.string.settings_circle_style_format, label));
    }

    private void updateNavigationAnimationButtonText(int style) {
        ensureNavigationAnimationOptionsLoaded();
        int safeStyle = styleToNavigationAnimationIndex(style);
        btnSetNavigationAnimation.setText(
                getString(R.string.settings_navigation_animation_format, navigationAnimationLabels[safeStyle])
        );
    }

    private void updateAppLockButtonText() {
        btnAppLock.setText(R.string.settings_app_lock_label);
    }

    private void showAppLockToolsDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_app_lock_tools, null);
        MaterialButton btnToggle = content.findViewById(R.id.btn_app_lock_toggle);
        MaterialButton btnDelay = content.findViewById(R.id.btn_app_lock_delay);
        TextView tvDelayHint = content.findViewById(R.id.tv_app_lock_delay_hint);

        Integer color = viewModel.getButtonColor().getValue();
        if (color != null) {
            ButtonColorHelper.applyPrimaryColor(btnToggle, color);
            btnDelay.setStrokeColor(ColorStateList.valueOf(color));
            btnDelay.setTextColor(color);
        }

        Runnable refresh = () -> {
            boolean enabled = viewModel.getRepository().isAppLockEnabled();
            btnToggle.setText(enabled ? R.string.app_lock_tools_toggle_on : R.string.app_lock_tools_toggle_off);
            String currentLabel = getAppLockTimeoutLabel(viewModel.getRepository().getAppLockTimeoutMs());
            btnDelay.setText(getString(R.string.settings_app_lock_timeout_format, currentLabel));
            tvDelayHint.setText(getString(R.string.app_lock_tools_delay_hint)
                    + "\n"
                    + getString(R.string.app_lock_tools_delay_current, currentLabel));
        };
        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_lock_tools_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);

        btnToggle.setOnClickListener(v -> {
            boolean enabled = viewModel.getRepository().isAppLockEnabled();
            authenticateForAppLockChange(() -> {
                viewModel.getRepository().setAppLockEnabled(!enabled);
                refresh.run();
            });
        });
        btnDelay.setOnClickListener(v -> showAppLockTimeoutDialog(refresh));
    }

    private void authenticateForAppLockChange(Runnable onSuccess) {
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(requireContext(), R.string.settings_app_lock_requires_secure_lock, Toast.LENGTH_LONG).show();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(requireContext());
        BiometricPrompt prompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        if (isAdded() && onSuccess != null) {
                            onSuccess.run();
                        }
                    }
                }
        );

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_auth_title))
                .setSubtitle(getString(R.string.app_lock_auth_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build();
        prompt.authenticate(promptInfo);
    }

    private void ensureAppLockTimeoutOptionsLoaded() {
        if (appLockTimeoutLabels == null) {
            appLockTimeoutLabels = getResources().getStringArray(R.array.settings_app_lock_timeout_labels);
        }
        if (appLockTimeoutValues == null) {
            appLockTimeoutValues = getResources().getIntArray(R.array.settings_app_lock_timeout_values);
        }
    }

    private void showAppLockTimeoutDialog(@Nullable Runnable onChanged) {
        ensureAppLockTimeoutOptionsLoaded();
        int current = viewModel.getRepository().getAppLockTimeoutMs();
        int checkedIndex = 0;
        for (int i = 0; i < appLockTimeoutValues.length; i++) {
            if (appLockTimeoutValues[i] == current) {
                checkedIndex = i;
                break;
            }
        }
        final int[] pendingIndex = new int[]{checkedIndex};
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_app_lock_timeout_dialog_title)
                .setSingleChoiceItems(appLockTimeoutLabels, checkedIndex, (d, which) -> pendingIndex[0] = which)
                .setPositiveButton(R.string.dialog_ok, (d, which) -> {
                    int selected = pendingIndex[0];
                    if (selected >= 0 && selected < appLockTimeoutValues.length) {
                        viewModel.getRepository().setAppLockTimeoutMs(appLockTimeoutValues[selected]);
                        if (onChanged != null) {
                            onChanged.run();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private String getAppLockTimeoutLabel(int timeoutMs) {
        ensureAppLockTimeoutOptionsLoaded();
        for (int i = 0; i < appLockTimeoutValues.length && i < appLockTimeoutLabels.length; i++) {
            if (appLockTimeoutValues[i] == timeoutMs) {
                return appLockTimeoutLabels[i];
            }
        }
        return appLockTimeoutLabels[0];
    }

    private void ensureCircleStyleOptionsLoaded() {
        if (circleStyleLabels == null) {
            circleStyleLabels = getResources().getStringArray(R.array.settings_circle_style_labels);
            circleStylePreviews = new Drawable[circleStyleLabels.length];
        }
    }

    private void showCircleStyleDialog() {
        ensureCircleStyleOptionsLoaded();
        Integer currentStyle = viewModel.getHomeCircleStyle().getValue();
        int selected = currentStyle != null ? currentStyle : SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE;
        if (selected >= circleStyleLabels.length) {
            selected = 0;
        }
        android.widget.ListAdapter adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                R.layout.item_circle_style_preview,
                android.R.id.text1,
                circleStyleLabels
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.ImageView preview = view.findViewById(R.id.img_style_preview);
                if (preview != null) {
                    preview.setImageDrawable(getCircleStylePreview(position));
                }
                return view;
            }
        };
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_circle_style_list, null);
        android.widget.ListView listView = content.findViewById(R.id.list_circle_styles);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        listView.setAdapter(adapter);
        listView.setItemChecked(selected, true);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_circle_style_dialog_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.setHomeCircleStyle(position);
            WidgetUpdater.updateAllWidgets(requireContext());
            dialog.dismiss();
        });
        applyDialogButtonColors(dialog);
    }

    private void showNavigationAnimationDialog() {
        ensureNavigationAnimationOptionsLoaded();
        Integer currentStyle = viewModel.getNavigationAnimationStyle().getValue();
        int selected = styleToNavigationAnimationIndex(currentStyle != null
                ? currentStyle
                : SettingsRepository.DEFAULT_NAVIGATION_ANIMATION_STYLE);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_navigation_animation_dialog_title)
                .setSingleChoiceItems(navigationAnimationLabels, selected, (dlg, which) -> {
                    viewModel.setNavigationAnimationStyle(navigationAnimationValues[which]);
                    dlg.dismiss();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private Drawable getCircleStylePreview(int style) {
        if (circleStylePreviews != null && style >= 0 && style < circleStylePreviews.length) {
            if (circleStylePreviews[style] != null) {
                return circleStylePreviews[style];
            }
            int color = viewModel != null && viewModel.getHomeCircleColor().getValue() != null
                    ? viewModel.getHomeCircleColor().getValue()
                    : SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
            circleStylePreviews[style] = buildCircleStylePreview(style, color);
            return circleStylePreviews[style];
        }
        return null;
    }

    private Drawable buildCircleStylePreview(int style, int color) {
        int size = dpToPx(36);
        int padding = dpToPx(4);
        int stroke = dpToPx(3);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint prog = new Paint(Paint.ANTI_ALIAS_FLAG);
        track.setStyle(Paint.Style.STROKE);
        prog.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        prog.setStrokeWidth(stroke);
        track.setColor(withAlpha(color, 80));
        prog.setColor(color);
        track.setStrokeCap(Paint.Cap.ROUND);
        prog.setStrokeCap(Paint.Cap.ROUND);

        RectF rect = new RectF(padding, padding, size - padding, size - padding);
        float sweep = 270f;

        if (style == HomeCircleView.STYLE_ARC) {
            canvas.drawArc(rect, -90f, sweep, false, prog);
            return new BitmapDrawable(getResources(), bitmap);
        }

        if (style == HomeCircleView.STYLE_SEGMENTED) {
            int segments = 10;
            float gap = 360f / segments * 0.55f;
            float segSweep = 360f / segments - gap;
            float start = -90f;
            for (int i = 0; i < segments; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(rect, segStart, segSweep, false, track);
            }
            int filled = 6;
            for (int i = 0; i < filled; i++) {
                float segStart = start + i * (segSweep + gap);
                canvas.drawArc(rect, segStart, segSweep, false, prog);
            }
            return new BitmapDrawable(getResources(), bitmap);
        }

        if (style == HomeCircleView.STYLE_GRADIENT || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            Shader shader = new SweepGradient(size / 2f, size / 2f,
                    new int[]{withAlpha(color, 90), color, withAlpha(color, 90)},
                    new float[]{0f, 0.7f, 1f});
            prog.setShader(shader);
        }
        if (style == HomeCircleView.STYLE_GLOW || style == HomeCircleView.STYLE_GRADIENT_GLOW) {
            prog.setShadowLayer(dpToPx(4), 0f, 0f, withAlpha(color, 180));
        }
        if (style == HomeCircleView.STYLE_PULSE_LIGHT) {
            prog.setShadowLayer(dpToPx(3), 0f, 0f, withAlpha(color, 140));
        } else if (style == HomeCircleView.STYLE_PULSE_MEDIUM) {
            prog.setShadowLayer(dpToPx(5), 0f, 0f, withAlpha(color, 180));
        } else if (style == HomeCircleView.STYLE_PULSE_STRONG) {
            prog.setShadowLayer(dpToPx(7), 0f, 0f, withAlpha(color, 220));
        } else if (style == HomeCircleView.STYLE_HALO) {
            track.setColor(withAlpha(color, 120));
        } else if (style == HomeCircleView.STYLE_THIN) {
            track.setStrokeWidth(dpToPx(2));
            prog.setStrokeWidth(dpToPx(2));
        }

        canvas.drawArc(rect, 0f, 360f, false, track);
        canvas.drawArc(rect, -90f, sweep, false, prog);

        if (style == HomeCircleView.STYLE_MARKER) {
            Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
            dot.setColor(color);
            float angle = (float) Math.toRadians(-90 + sweep);
            float cx = rect.centerX();
            float cy = rect.centerY();
            float r = rect.width() / 2f;
            float x = cx + (float) Math.cos(angle) * r;
            float y = cy + (float) Math.sin(angle) * r;
            canvas.drawCircle(x, y, dpToPx(3), dot);
        }

        return new BitmapDrawable(getResources(), bitmap);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private int withAlpha(int color, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (clamped << 24);
    }

    private void ensureButtonColorOptionsLoaded() {
        if (buttonColorValues == null || buttonColorLabels == null) {
            buttonColorValues = getResources().getIntArray(R.array.settings_button_color_values);
            buttonColorLabels = getResources().getStringArray(R.array.settings_button_color_labels);
        }
    }

    private void ensureNavigationAnimationOptionsLoaded() {
        if (navigationAnimationLabels == null) {
            navigationAnimationLabels = getResources().getStringArray(R.array.settings_navigation_animation_labels);
            navigationAnimationValues = getResources().getIntArray(R.array.settings_navigation_animation_values);
            if (navigationAnimationLabels.length != navigationAnimationValues.length) {
                navigationAnimationLabels = new String[]{"Slide", "Fade", "Zoom", "Slide Up", "Rotate", "Pop", "None"};
                navigationAnimationValues = new int[]{
                        SettingsRepository.NAV_ANIM_SLIDE,
                        SettingsRepository.NAV_ANIM_FADE,
                        SettingsRepository.NAV_ANIM_ZOOM,
                        SettingsRepository.NAV_ANIM_SLIDE_UP,
                        SettingsRepository.NAV_ANIM_ROTATE,
                        SettingsRepository.NAV_ANIM_POP,
                        SettingsRepository.NAV_ANIM_NONE
                };
            }
        }
    }

    private int styleToNavigationAnimationIndex(int style) {
        ensureNavigationAnimationOptionsLoaded();
        for (int i = 0; i < navigationAnimationValues.length; i++) {
            if (navigationAnimationValues[i] == style) {
                return i;
            }
        }
        return 0;
    }

    private int getButtonColorIndex(int color) {
        ensureButtonColorOptionsLoaded();
        for (int i = 0; i < buttonColorValues.length; i++) {
            if (buttonColorValues[i] == color) {
                return i;
            }
        }
        return 0;
    }

    private String unitLabel(String unit) {
        if ("years".equals(unit)) {
            return getString(R.string.settings_unit_year);
        }
        return getString(R.string.settings_unit_month);
    }

    private void checkForUpdates() {
        Toast.makeText(requireContext(), R.string.update_checking_toast, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                ReleaseInfo releaseInfo = fetchLatestReleaseInfo();
                if (releaseInfo == null) {
                    showToast(R.string.update_failed_toast);
                    return;
                }
                if (releaseInfo.downloadUrl == null) {
                    showToast(R.string.update_no_apk_toast);
                    return;
                }
                String currentVersion = getCurrentVersionName();
                int compare = compareVersions(currentVersion, releaseInfo.versionName);
                if (compare >= 0) {
                    showToast(R.string.update_latest_toast);
                    return;
                }
                showUpdateConfirmDialog(releaseInfo);
            } catch (Exception e) {
                showToast(R.string.update_failed_toast);
            }
        }).start();
    }

    private void showToast(int messageResId) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() ->
                Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show()
        );
    }

    private void showUpdateBackupConfirmDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_backup_title)
                .setMessage(R.string.update_backup_message)
                .setPositiveButton(R.string.update_backup_yes, (dlg, which) -> checkForUpdates())
                .setNegativeButton(R.string.update_backup_no, (dlg, which) -> showBackupDialog())
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showUpdateConfirmDialog(ReleaseInfo releaseInfo) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            String version = releaseInfo.versionName != null ? releaseInfo.versionName : "";
            String message = getString(R.string.update_available_message, version);
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_available_title)
                    .setMessage(message)
                    .setPositiveButton(R.string.update_install, (dlg, which) -> downloadAndInstall(releaseInfo))
                    .setNegativeButton(R.string.update_later, null)
                    .show();
            applyDialogButtonColors(dialog);
        });
    }

    private void downloadAndInstall(ReleaseInfo releaseInfo) {
        showDownloadDialog();
        new Thread(() -> {
            File apkFile = downloadApk(releaseInfo.downloadUrl, releaseInfo.versionName, this::updateDownloadProgress);
            dismissDownloadDialog();
            if (apkFile == null) {
                showToast(R.string.update_download_failed_toast);
                return;
            }
            promptInstall(apkFile);
        }).start();
    }

    private void showDownloadDialog() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadDialog != null && downloadDialog.isShowing()) {
                return;
            }
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            LinearLayout container = new LinearLayout(requireContext());
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(padding, padding, padding, padding);

            downloadProgressBar = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            downloadProgressBar.setIndeterminate(true);
            downloadProgressBar.setMax(100);

            downloadProgressText = new TextView(requireContext());
            downloadProgressText.setText("0%");
            downloadProgressText.setPadding(0, padding / 2, 0, 0);

            container.addView(downloadProgressBar);
            container.addView(downloadProgressText);

            downloadDialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.update_download_title)
                    .setView(container)
                    .setCancelable(false)
                    .create();
            downloadDialog.show();
        });
    }

    private void dismissDownloadDialog() {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadDialog != null) {
                downloadDialog.dismiss();
                downloadDialog = null;
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateDownloadProgress(int percent) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (downloadProgressBar == null || downloadProgressText == null) {
                return;
            }
            if (percent < 0) {
                downloadProgressBar.setIndeterminate(true);
                downloadProgressText.setText("");
            } else {
                downloadProgressBar.setIndeterminate(false);
                downloadProgressBar.setProgress(percent);
                downloadProgressText.setText(percent + "%");
            }
        });
    }

    private ReleaseInfo fetchLatestReleaseInfo() throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(RELEASES_URL);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Veri-Aristo-App");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                return null;
            }
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    body.append(line);
                }
            }

            JsonArray releases = JsonParser.parseString(body.toString()).getAsJsonArray();
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                boolean isDraft = release.get("draft").getAsBoolean();
                boolean isPrerelease = release.get("prerelease").getAsBoolean();
                if (isDraft || isPrerelease) {
                    continue;
                }
                String tag = release.has("tag_name") && !release.get("tag_name").isJsonNull()
                        ? release.get("tag_name").getAsString()
                        : null;
                String versionName = normalizeVersion(tag);
                JsonArray assets = release.getAsJsonArray("assets");
                String downloadUrl = findApkAssetUrl(assets);
                return new ReleaseInfo(versionName, downloadUrl);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private String findApkAssetUrl(JsonArray assets) {
        if (assets == null) {
            return null;
        }
        for (JsonElement assetElement : assets) {
            JsonObject asset = assetElement.getAsJsonObject();
            String name = asset.has("name") && !asset.get("name").isJsonNull()
                    ? asset.get("name").getAsString()
                    : "";
            if (name.toLowerCase(Locale.US).endsWith(".apk")) {
                return asset.get("browser_download_url").getAsString();
            }
        }
        return null;
    }

    private interface ProgressListener {
        void onProgress(int percent);
    }

    private File downloadApk(String downloadUrl, String versionName, ProgressListener progressListener) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(downloadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Veri-Aristo-App");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            int totalBytes = connection.getContentLength();
            if (progressListener != null) {
                progressListener.onProgress(totalBytes > 0 ? 0 : -1);
            }
            File target = new File(requireContext().getCacheDir(),
                    "veri_aristo_update_" + (versionName != null ? versionName : "latest") + ".apk");
            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = new FileOutputStream(target)) {
                byte[] buffer = new byte[8192];
                int read;
                long downloaded = 0;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    downloaded += read;
                    if (progressListener != null && totalBytes > 0) {
                        int percent = (int) ((downloaded * 100) / totalBytes);
                        progressListener.onProgress(percent);
                    }
                }
                outputStream.flush();
            }
            return target;
        } catch (IOException e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void promptInstall(File apkFile) {
        if (!isAdded()) {
            return;
        }
        pendingApkFile = apkFile;
        if (!requireContext().getPackageManager().canRequestPackageInstalls()) {
            showToast(R.string.update_install_prompt_toast);
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + requireContext().getPackageName()));
            manageUnknownSourcesLauncher.launch(intent);
            return;
        }
        Uri apkUri = FileProvider.getUriForFile(requireContext(),
                requireContext().getPackageName() + ".fileprovider", apkFile);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
        pendingApkFile = null;
    }

    private String normalizeVersion(String tag) {
        if (tag == null) {
            return null;
        }
        String trimmed = tag.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    private int compareVersions(String current, String latest) {
        if (latest == null) {
            return 0;
        }
        int[] currentParts = parseVersionParts(current);
        int[] latestParts = parseVersionParts(latest);
        int max = Math.max(currentParts.length, latestParts.length);
        for (int i = 0; i < max; i++) {
            int c = i < currentParts.length ? currentParts[i] : 0;
            int l = i < latestParts.length ? latestParts[i] : 0;
            if (c != l) {
                return Integer.compare(c, l);
            }
        }
        return 0;
    }

    private int[] parseVersionParts(String version) {
        if (version == null || version.isEmpty()) {
            return new int[]{0};
        }
        String[] parts = version.split("\\.");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static class ReleaseInfo {
        final String versionName;
        final String downloadUrl;

        ReleaseInfo(String versionName, String downloadUrl) {
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
        }
    }

    private void configureValuePicker(NumberPicker picker, int unitIndex, Integer currentValue) {
        int max = unitIndex == 1 ? 10 : 60;
        picker.setMinValue(0);
        picker.setMaxValue(max);
        int value = currentValue != null ? Math.min(currentValue, max) : 0;
        picker.setValue(value);
    }

    private String getCurrentVersionName() {
        try {
            android.content.pm.PackageManager packageManager = requireContext().getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                android.content.pm.PackageInfo info = packageManager.getPackageInfo(
                        requireContext().getPackageName(),
                        android.content.pm.PackageManager.PackageInfoFlags.of(0));
                return info.versionName;
            }
            android.content.pm.PackageInfo info = packageManager.getPackageInfo(
                    requireContext().getPackageName(), 0);
            return info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private android.widget.LinearLayout buildPickerRow(String labelText, NumberPicker valuePicker, NumberPicker unitPicker) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, 12, 0, 12);

        TextView label = new TextView(requireContext());
        label.setText(labelText);
        label.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        valuePicker.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        if (unitPicker != null) {
            unitPicker.setLayoutParams(new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }

        row.addView(label);
        row.addView(valuePicker);
        if (unitPicker != null) {
            row.addView(unitPicker);
        }
        return row;
    }

    private void showResetDialog() {
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_message)
                .setPositiveButton(R.string.settings_reset_confirm, (dlg, which) -> resetAppData())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showBackupDialog() {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup_tools, null);
        MaterialButton btnCreate = content.findViewById(R.id.btn_backup_create);
        MaterialButton btnRestore = content.findViewById(R.id.btn_backup_restore);
        Integer color = viewModel.getButtonColor().getValue();
        if (color != null) {
            ButtonColorHelper.applyPrimaryColor(btnCreate, color);
            btnRestore.setStrokeColor(ColorStateList.valueOf(color));
            btnRestore.setTextColor(color);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_title)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        applyDialogButtonColors(dialog);

        btnCreate.setOnClickListener(v -> {
            dialog.dismiss();
            createBackupLauncher.launch("veri_aristo_backup.json");
        });
        btnRestore.setOnClickListener(v -> {
            dialog.dismiss();
            restoreBackupLauncher.launch(new String[]{"application/json"});
        });
    }

    private void showAppInfoDialog() {
        String versionName = "1.0";
        try {
            android.content.pm.PackageInfo info = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0);
            versionName = info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
        }

        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_app_info, null);
        TextView appName = content.findViewById(R.id.tv_app_name);
        TextView appVersion = content.findViewById(R.id.tv_app_version);
        TextView appDescription = content.findViewById(R.id.tv_app_description);
        TextView appDeveloper = content.findViewById(R.id.tv_app_developer);
        com.google.android.material.button.MaterialButton openEmail = content.findViewById(R.id.btn_open_email);
        com.google.android.material.button.MaterialButton openGithub = content.findViewById(R.id.btn_open_github);
        com.google.android.material.button.MaterialButton openTelegramBot = content.findViewById(R.id.btn_open_telegram_bot);
        com.google.android.material.button.MaterialButton openGithubProfile = content.findViewById(R.id.btn_open_github_profile);
        com.google.android.material.button.MaterialButton openCoffee = content.findViewById(R.id.btn_open_coffee);
        Integer buttonColor = viewModel.getButtonColor().getValue();
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(openEmail, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openGithub, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openTelegramBot, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openGithubProfile, buttonColor);
            ButtonColorHelper.applyPrimaryColor(openCoffee, buttonColor);
        }

        appName.setText(R.string.app_info_name);
        appVersion.setText(getString(R.string.app_info_version, versionName));
        appDescription.setText(R.string.app_info_description);
        String developerLabel = getString(R.string.app_info_developer_label);
        String developerName = getString(R.string.app_info_developer_name);
        SpannableString developerText = new SpannableString(
                String.format(Locale.getDefault(), "%s %s", developerLabel, developerName));
        int labelEnd = developerLabel.length();
        int labelColor = buttonColor != null
                ? buttonColor
                : MaterialColors.getColor(appDeveloper, com.google.android.material.R.attr.colorPrimary);
        developerText.setSpan(new ForegroundColorSpan(labelColor), 0, labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        developerText.setSpan(new StyleSpan(Typeface.BOLD), 0, labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        appDeveloper.setText(developerText);

        openEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:sichler.daniel@gmail.com"));
            startActivity(intent);
        });

        openGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://linktr.ee/darexsh"));
            startActivity(intent);
        });

        openTelegramBot.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/darexsh_bot"));
            startActivity(intent);
        });

        openGithubProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Darexsh"));
            startActivity(intent);
        });

        openCoffee.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeacoffee.com/darexsh"));
            startActivity(intent);
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.app_info_title)
                .setView(content)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void resetAppData() {
        cancelAllScheduledNotifications();

        String uriStr = viewModel.getBackgroundImageUri().getValue();
        if (uriStr != null) {
            try {
                requireContext().getContentResolver().releasePersistableUriPermission(
                        Uri.parse(uriStr), Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
        }

        viewModel.getRepository().clearAllData();
        requireContext().getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
                .edit().clear().apply();
        requireContext().deleteSharedPreferences("app_prefs");
        requireContext().deleteSharedPreferences("notes_prefs");

        viewModel.getRepository().saveCycleHistory(new ArrayList<>());
        viewModel.setBackgroundImageUri(null);
        viewModel.setCycleLength(21);
        Calendar resetStart = Calendar.getInstance();
        resetStart.set(Calendar.HOUR_OF_DAY, 18);
        resetStart.set(Calendar.MINUTE, 0);
        resetStart.set(Calendar.SECOND, 0);
        resetStart.set(Calendar.MILLISECOND, 0);
        viewModel.setStartDate(resetStart);
        viewModel.setCalendarPastRange(12, "months");
        viewModel.setCalendarFutureRange(2, "years");
        viewModel.setRemovalReminderHours(6);
        viewModel.setInsertionReminderHours(6);
        viewModel.setButtonColor(SettingsRepository.DEFAULT_BUTTON_COLOR);
        viewModel.setHomeCircleColor(SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR);
        viewModel.setHomeCircleStyle(SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE);

        WidgetUpdater.updateAllWidgets(requireContext());
        Toast.makeText(requireContext(), R.string.settings_reset_done, Toast.LENGTH_SHORT).show();
    }

    private void cancelAllScheduledNotifications() {
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        int daysRange = 365;
        for (int d = -daysRange; d < daysRange; d++) {
            for (int offset = 0; offset < 6; offset++) {
                long triggerTime = System.currentTimeMillis() + d * 24L * 60 * 60 * 1000;
                int requestCode = (int) ((triggerTime / 1000) % Integer.MAX_VALUE) + offset;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        requireContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );

                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                }
            }
        }
    }

    private void writeBackup(Uri uri) {
        BackupData data = new BackupData();
        data.version = 1;
        data.appPrefs = readPrefs("app_prefs");
        data.notesPrefs = readPrefs("notes_prefs");

        try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {
            if (outputStream == null) {
                Toast.makeText(requireContext(), R.string.backup_failed_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            String json = new Gson().toJson(data);
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
            Toast.makeText(requireContext(), R.string.backup_created_toast, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.backup_failed_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void readBackup(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), R.string.backup_read_failed_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] bufferData = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(bufferData, 0, bufferData.length)) != -1) {
                buffer.write(bufferData, 0, nRead);
            }
            String json = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            BackupData backupData = new Gson().fromJson(json, BackupData.class);
            if (backupData == null) {
                Toast.makeText(requireContext(), R.string.backup_invalid_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            showBackupValidationDialog(backupData);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.backup_read_failed_toast, Toast.LENGTH_SHORT).show();
        }
    }

    private void showBackupValidationDialog(BackupData backupData) {
        int appEntries = backupData.appPrefs != null ? backupData.appPrefs.size() : 0;
        int notesEntries = backupData.notesPrefs != null ? backupData.notesPrefs.size() : 0;
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        int smallSpacing = (int) (8 * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout content = new android.widget.LinearLayout(requireContext());
        content.setOrientation(android.widget.LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        TextView summary = new TextView(requireContext());
        summary.setText(getString(R.string.backup_import_check_message));
        summary.setPadding(0, 0, 0, smallSpacing);
        content.addView(summary);

        content.addView(buildBackupPreviewLine(
                getString(R.string.backup_import_section_app),
                appEntries,
                backupData.appPrefs
        ));
        content.addView(buildBackupPreviewLine(
                getString(R.string.backup_import_section_notes),
                notesEntries,
                backupData.notesPrefs
        ));

        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        scrollView.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.backup_import_check_title)
                .setView(scrollView)
                .setPositiveButton(R.string.backup_import_yes, (dlg, which) -> applyBackupData(backupData))
                .setNegativeButton(R.string.backup_import_no, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private View buildBackupPreviewLine(String sectionTitle, int count, Map<String, PrefValue> values) {
        int verticalSpacing = (int) (8 * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout section = new android.widget.LinearLayout(requireContext());
        section.setOrientation(android.widget.LinearLayout.VERTICAL);
        section.setPadding(0, verticalSpacing / 2, 0, verticalSpacing / 2);

        TextView toggle = new TextView(requireContext());
        toggle.setPadding(0, verticalSpacing / 2, 0, verticalSpacing / 2);
        toggle.setBackgroundResource(android.R.drawable.list_selector_background);
        toggle.setClickable(true);
        toggle.setFocusable(true);

        TextView details = new TextView(requireContext());
        details.setText(buildBackupDetailsText(values));
        details.setVisibility(View.GONE);
        details.setPadding(0, verticalSpacing / 2, 0, 0);

        updateBackupSectionToggleText(toggle, sectionTitle, count, false);

        toggle.setOnClickListener(v -> {
            boolean expanded = details.getVisibility() == View.VISIBLE;
            details.setVisibility(expanded ? View.GONE : View.VISIBLE);
            updateBackupSectionToggleText(toggle, sectionTitle, count, !expanded);
        });

        section.addView(toggle);
        section.addView(details);
        return section;
    }

    private void updateBackupSectionToggleText(TextView toggle, String title, int count, boolean expanded) {
        String action = expanded
                ? getString(R.string.backup_import_hide_details)
                : getString(R.string.backup_import_show_details);
        toggle.setText(getString(R.string.backup_import_line_format, title, count, action));
    }

    private String buildBackupDetailsText(Map<String, PrefValue> values) {
        if (values == null || values.isEmpty()) {
            return getString(R.string.backup_import_no_entries);
        }

        Map<String, PrefValue> sorted = new TreeMap<>(values);
        Set<String> consumed = new HashSet<>();
        StringBuilder builder = new StringBuilder();

        appendAggregatedBackupLines(sorted, consumed, builder);

        for (Map.Entry<String, PrefValue> entry : sorted.entrySet()) {
            String key = entry.getKey();
            if (consumed.contains(key)) {
                continue;
            }
            String line = formatBackupLine(key, entry.getValue(), sorted, consumed);
            if (line == null || line.isEmpty()) {
                continue;
            }
            builder.append(line)
                    .append('\n');
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }

    private void appendAggregatedBackupLines(Map<String, PrefValue> all, Set<String> consumed, StringBuilder builder) {
        if (all.containsKey("cycle_history")) {
            consumed.add("cycle_history");
            appendLine(builder, getString(R.string.backup_field_cycle_history) + ": " + formatCycleHistory(all.get("cycle_history")));
        }

        int notificationFlags = 0;
        int notificationFlagsTrue = 0;
        int notifiedSettings = 0;
        int cycleDelayEntries = 0;
        int skipRingFreeEntries = 0;
        for (Map.Entry<String, PrefValue> entry : all.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("notified_settings_")) {
                notifiedSettings++;
                consumed.add(key);
            } else if (key.startsWith("notified_")) {
                notificationFlags++;
                if (toBoolean(entry.getValue() != null ? entry.getValue().value : null)) {
                    notificationFlagsTrue++;
                }
                consumed.add(key);
            } else if (key.startsWith("cycle_delay_")) {
                cycleDelayEntries++;
                consumed.add(key);
            } else if (key.startsWith("skip_ring_free_")) {
                skipRingFreeEntries++;
                consumed.add(key);
            }
        }

        if (notificationFlags > 0) {
            appendLine(builder, getString(
                    R.string.backup_field_notification_flags_format,
                    notificationFlags,
                    notificationFlagsTrue
            ));
        }
        if (notifiedSettings > 0) {
            appendLine(builder, getString(R.string.backup_field_notification_hashes_format, notifiedSettings));
        }
        if (cycleDelayEntries > 0) {
            appendLine(builder, getString(R.string.backup_field_cycle_delay_entries_format, cycleDelayEntries));
        }
        if (skipRingFreeEntries > 0) {
            appendLine(builder, getString(R.string.backup_field_skip_ring_free_entries_format, skipRingFreeEntries));
        }
    }

    private void appendLine(StringBuilder builder, String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        builder.append(text).append('\n');
    }

    private String formatBackupLine(String key, PrefValue value, Map<String, PrefValue> all, Set<String> consumed) {
        switch (key) {
            case "start_day":
            case "start_month":
            case "start_year":
                consumed.add("start_day");
                consumed.add("start_month");
                consumed.add("start_year");
                return getString(R.string.backup_field_start_date) + ": " + formatStartDate(all);
            case "set_time_hour":
            case "set_time_minute":
                consumed.add("set_time_hour");
                consumed.add("set_time_minute");
                return getString(R.string.backup_field_start_time) + ": " + formatStartTime(all);
            case "calendar_future_amount":
                consumed.add("calendar_future_amount");
                consumed.add("calendar_future_unit");
                return getString(R.string.backup_field_calendar_future) + ": " + formatRangeValue(
                        prefToInt(value),
                        prefToString(all.get("calendar_future_unit"), "years")
                );
            case "calendar_future_years":
                if (all.containsKey("calendar_future_amount")) {
                    return null;
                }
                consumed.add("calendar_future_years");
                return getString(R.string.backup_field_calendar_future) + ": " + formatRangeValue(prefToInt(value), "years");
            case "calendar_past_amount":
                consumed.add("calendar_past_amount");
                consumed.add("calendar_past_unit");
                return getString(R.string.backup_field_calendar_past) + ": " + formatRangeValue(
                        prefToInt(value),
                        prefToString(all.get("calendar_past_unit"), "months")
                );
            case "calendar_past_months":
                if (all.containsKey("calendar_past_amount")) {
                    return null;
                }
                consumed.add("calendar_past_months");
                return getString(R.string.backup_field_calendar_past) + ": " + formatRangeValue(prefToInt(value), "months");
            case "calendar_future_unit":
            case "calendar_past_unit":
                return null;
            case "cycle_length":
                return getString(R.string.backup_field_cycle_length) + ": " + prefToInt(value) + " " + getString(R.string.backup_unit_days);
            case "removal_reminder_hours":
                return getString(R.string.backup_field_reminder_removal) + ": " + prefToInt(value) + "h";
            case "insertion_reminder_hours":
                return getString(R.string.backup_field_reminder_insertion) + ": " + prefToInt(value) + "h";
            case "button_color":
                return getString(R.string.backup_field_button_color) + ": " + formatColor(value);
            case "home_circle_color":
                return getString(R.string.backup_field_home_circle_color) + ": " + formatColor(value);
            case "home_circle_style":
                return getString(R.string.backup_field_home_circle_style) + ": " + formatCircleStyle(value);
            case "navigation_animation_style":
                return getString(R.string.backup_field_navigation_animation) + ": " + formatNavigationAnimation(value);
            case "background_image_uri":
                return getString(R.string.backup_field_background_image) + ": " + formatBackgroundImage(value);
            case "user_notes":
                return getString(R.string.backup_field_notes_text) + ": " + formatNotesPreview(value);
            case "notes_last_saved":
                return getString(R.string.backup_field_notes_last_saved) + ": " + formatTimestamp(value);
            case "debug_tools_enabled":
                return getString(R.string.backup_field_debug_tools) + ": " + formatBoolean(value);
            case "debug_time_enabled":
                return getString(R.string.backup_field_debug_time) + ": " + formatBoolean(value);
            case "debug_time_millis":
                return getString(R.string.backup_field_debug_time_value) + ": " + formatTimestamp(value);
            case "last_version":
                return getString(R.string.backup_field_last_version) + ": " + prefToInt(value);
            case "tour_shown":
                return getString(R.string.backup_field_tour_shown) + ": " + formatBoolean(value);
            case "welcome_shown":
                return getString(R.string.backup_field_welcome_shown) + ": " + formatBoolean(value);
            case "exact_alarm_prompted":
                return getString(R.string.backup_field_exact_alarm_prompted) + ": " + formatBoolean(value);
            case "app_lock_enabled":
                return getString(R.string.backup_field_app_lock) + ": " + formatBoolean(value);
            case "app_lock_timeout_ms":
                return getString(R.string.backup_field_app_lock_timeout) + ": " + formatAppLockTimeout(value);
            case "home_circle_style_version":
                return null; // internal migration marker
            default:
                return key + " = " + formatPrefValue(value);
        }
    }

    private String formatPrefValue(PrefValue value) {
        if (value == null) {
            return "null";
        }
        if (value.value == null) {
            return "null";
        }
        return String.valueOf(value.value);
    }

    private int prefToInt(PrefValue value) {
        return toInt(value != null ? value.value : null);
    }

    private String prefToString(PrefValue value, String fallback) {
        if (value == null || value.value == null) {
            return fallback;
        }
        return String.valueOf(value.value);
    }

    private String formatStartDate(Map<String, PrefValue> all) {
        int day = prefToInt(all.get("start_day"));
        int month = prefToInt(all.get("start_month")) + 1;
        int year = prefToInt(all.get("start_year"));
        if (year <= 0 || month <= 0 || day <= 0) {
            return getString(R.string.backup_unknown);
        }
        return String.format(Locale.getDefault(), "%02d.%02d.%04d", day, month, year);
    }

    private String formatStartTime(Map<String, PrefValue> all) {
        int hour = prefToInt(all.get("set_time_hour"));
        int minute = prefToInt(all.get("set_time_minute"));
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    private String formatRangeValue(int amount, String unit) {
        if ("years".equals(unit)) {
            String label = amount == 1 ? getString(R.string.backup_unit_year) : getString(R.string.backup_unit_years);
            return amount + " " + label;
        }
        String label = amount == 1 ? getString(R.string.backup_unit_month) : getString(R.string.backup_unit_months);
        return amount + " " + label;
    }

    private String formatColor(PrefValue value) {
        int color = prefToInt(value);
        return String.format(Locale.US, "#%08X", color);
    }

    private String formatCircleStyle(PrefValue value) {
        int style = prefToInt(value);
        String[] labels = getResources().getStringArray(R.array.settings_circle_style_labels);
        if (style >= 0 && style < labels.length) {
            return labels[style];
        }
        return String.valueOf(style);
    }

    private String formatNavigationAnimation(PrefValue value) {
        int style = prefToInt(value);
        int[] values = getResources().getIntArray(R.array.settings_navigation_animation_values);
        String[] labels = getResources().getStringArray(R.array.settings_navigation_animation_labels);
        for (int i = 0; i < values.length && i < labels.length; i++) {
            if (values[i] == style) {
                return labels[i];
            }
        }
        return String.valueOf(style);
    }

    private String formatAppLockTimeout(PrefValue value) {
        int timeoutMs = prefToInt(value);
        int[] values = getResources().getIntArray(R.array.settings_app_lock_timeout_values);
        String[] labels = getResources().getStringArray(R.array.settings_app_lock_timeout_labels);
        for (int i = 0; i < values.length && i < labels.length; i++) {
            if (values[i] == timeoutMs) {
                return labels[i];
            }
        }
        return timeoutMs + "ms";
    }

    private String formatBackgroundImage(PrefValue value) {
        String raw = prefToString(value, "");
        if (raw == null || raw.trim().isEmpty() || "null".equalsIgnoreCase(raw.trim())) {
            return getString(R.string.backup_no);
        }
        return getString(R.string.backup_yes);
    }

    private String formatNotesPreview(PrefValue value) {
        String raw = prefToString(value, "");
        if (raw.trim().isEmpty()) {
            return getString(R.string.backup_import_no_entries);
        }
        String singleLine = raw.replace('\n', ' ').trim();
        if (singleLine.length() > 80) {
            return singleLine.substring(0, 80) + "...";
        }
        return singleLine;
    }

    private String formatTimestamp(PrefValue value) {
        long millis = toLong(value != null ? value.value : null);
        if (millis <= 0) {
            return getString(R.string.backup_unknown);
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        return format.format(new java.util.Date(millis));
    }

    private String formatBoolean(PrefValue value) {
        return toBoolean(value != null ? value.value : null)
                ? getString(R.string.backup_yes)
                : getString(R.string.backup_no);
    }

    private String formatCycleHistory(PrefValue value) {
        String raw = prefToString(value, "");
        if (raw.trim().isEmpty()) {
            return getString(R.string.backup_import_no_entries);
        }
        try {
            JsonArray history = JsonParser.parseString(raw).getAsJsonArray();
            int entries = history.size();
            long minStart = Long.MAX_VALUE;
            long maxEnd = Long.MIN_VALUE;
            for (JsonElement element : history) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject obj = element.getAsJsonObject();
                if (obj.has("dateMillis") && !obj.get("dateMillis").isJsonNull()) {
                    long start = obj.get("dateMillis").getAsLong();
                    if (start > 0 && start < minStart) {
                        minStart = start;
                    }
                }
                if (obj.has("endDateMillis") && !obj.get("endDateMillis").isJsonNull()) {
                    long end = obj.get("endDateMillis").getAsLong();
                    if (end > 0 && end > maxEnd) {
                        maxEnd = end;
                    }
                }
            }
            if (entries == 0) {
                return getString(R.string.backup_import_no_entries);
            }
            if (minStart == Long.MAX_VALUE || maxEnd == Long.MIN_VALUE) {
                return getString(R.string.backup_field_cycle_history_entries_format, entries);
            }
            SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            return getString(
                    R.string.backup_field_cycle_history_range_format,
                    entries,
                    format.format(new java.util.Date(minStart)),
                    format.format(new java.util.Date(maxEnd))
            );
        } catch (Exception ignored) {
            return getString(R.string.backup_unknown);
        }
    }

    private void applyBackupData(BackupData backupData) {
        writePrefs("app_prefs", backupData.appPrefs);
        writePrefs("notes_prefs", backupData.notesPrefs);

        viewModel.getRepository().clearNotificationFlags();

        SettingsRepository restoredRepository = new SettingsRepository(requireContext());
        viewModel.setBackgroundImageUri(restoredRepository.getBackgroundImageUri());
        viewModel.setCycleLength(restoredRepository.getCycleLength());
        viewModel.setStartDate(restoredRepository.getStartDate());
        viewModel.setCalendarPastRange(restoredRepository.getCalendarPastAmount(),
                restoredRepository.getCalendarPastUnit());
        viewModel.setCalendarFutureRange(restoredRepository.getCalendarFutureAmount(),
                restoredRepository.getCalendarFutureUnit());
        viewModel.setRemovalReminderHours(restoredRepository.getRemovalReminderHours());
        viewModel.setInsertionReminderHours(restoredRepository.getInsertionReminderHours());
        viewModel.setNavigationAnimationStyle(restoredRepository.getNavigationAnimationStyle());

        WidgetUpdater.updateAllWidgets(requireContext());
        Toast.makeText(requireContext(), R.string.backup_restored_toast, Toast.LENGTH_SHORT).show();
    }

    private Map<String, PrefValue> readPrefs(String name) {
        Map<String, ?> all = requireContext().getSharedPreferences(name, Context.MODE_PRIVATE).getAll();
        Map<String, PrefValue> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Boolean) {
                result.put(entry.getKey(), new PrefValue("boolean", value));
            } else if (value instanceof Integer) {
                result.put(entry.getKey(), new PrefValue("int", value));
            } else if (value instanceof Long) {
                result.put(entry.getKey(), new PrefValue("long", value));
            } else if (value instanceof Float) {
                result.put(entry.getKey(), new PrefValue("float", value));
            } else if (value instanceof String) {
                result.put(entry.getKey(), new PrefValue("string", value));
            }
        }
        return result;
    }

    private void writePrefs(String name, Map<String, PrefValue> values) {
        if (values == null) {
            return;
        }
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        for (Map.Entry<String, PrefValue> entry : values.entrySet()) {
            PrefValue prefValue = entry.getValue();
            if (prefValue == null || prefValue.type == null) {
                continue;
            }
            switch (prefValue.type) {
                case "boolean":
                    editor.putBoolean(entry.getKey(), toBoolean(prefValue.value));
                    break;
                case "int":
                    editor.putInt(entry.getKey(), toInt(prefValue.value));
                    break;
                case "long":
                    editor.putLong(entry.getKey(), toLong(prefValue.value));
                    break;
                case "float":
                    editor.putFloat(entry.getKey(), toFloat(prefValue.value));
                    break;
                case "string":
                    editor.putString(entry.getKey(), prefValue.value != null ? String.valueOf(prefValue.value) : null);
                    break;
                default:
                    break;
            }
        }
        editor.apply();
    }

    private boolean toBoolean(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }

    private int toInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private long toLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private float toFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        return 0f;
    }

    private static class BackupData {
        int version;
        Map<String, PrefValue> appPrefs;
        Map<String, PrefValue> notesPrefs;
    }

    private static class PrefValue {
        String type;
        Object value;

        PrefValue(String type, Object value) {
            this.type = type;
            this.value = value;
        }
    }

    private void showDebugDialog() {
        TextView content = new TextView(requireContext());
        content.setTypeface(Typeface.MONOSPACE);
        content.setTextSize(12f);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);
        content.setText(buildDebugInfo());

        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.debug_dialog_title)
                .setView(scrollView)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
        applyDialogButtonColors(dialog);
    }

    private void showDebugTimePicker() {
        Calendar base = DebugTimeProvider.now(viewModel.getRepository());
        DatePickerDialog datePicker = new DatePickerDialog(
                requireContext(),
                (DatePicker view, int year, int month, int dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(Calendar.YEAR, year);
                    selected.set(Calendar.MONTH, month);
                    selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePicker = new TimePickerDialog(
                            requireContext(),
                            (TimePicker timeView, int hourOfDay, int minute) -> {
                                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selected.set(Calendar.MINUTE, minute);
                                selected.set(Calendar.SECOND, 0);
                                selected.set(Calendar.MILLISECOND, 0);

                                viewModel.getRepository().setDebugTimeMillis(selected.getTimeInMillis());
                                viewModel.getRepository().setDebugTimeEnabled(true);
                                switchDebugTime.setChecked(true);
                                updateDebugTimeStatus();
                                WidgetUpdater.updateAllWidgets(requireContext());
                                Toast.makeText(requireContext(), getString(R.string.debug_time_set), Toast.LENGTH_SHORT).show();
                            },
                            base.get(Calendar.HOUR_OF_DAY),
                            base.get(Calendar.MINUTE),
                            true
                    );
                    timePicker.show();
                },
                base.get(Calendar.YEAR),
                base.get(Calendar.MONTH),
                base.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Toast.makeText(requireContext(), getString(R.string.exact_alarm_not_supported), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private void openBatteryOptimizationSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void showNotificationToolsDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_notification_tools, null);
        MaterialButton btnTimes = content.findViewById(R.id.btn_notification_times);
        MaterialButton btnPermission = content.findViewById(R.id.btn_notification_permission);
        MaterialButton btnExact = content.findViewById(R.id.btn_notification_exact);
        MaterialButton btnBattery = content.findViewById(R.id.btn_notification_battery_opt);
        MaterialButton btnTest = content.findViewById(R.id.btn_notification_test);

        Integer color = viewModel.getButtonColor().getValue();
        if (color != null) {
            ButtonColorHelper.applyPrimaryColor(btnTimes, color);
            btnPermission.setStrokeColor(ColorStateList.valueOf(color));
            btnPermission.setTextColor(color);
            btnExact.setStrokeColor(ColorStateList.valueOf(color));
            btnExact.setTextColor(color);
            btnBattery.setStrokeColor(ColorStateList.valueOf(color));
            btnBattery.setTextColor(color);
            ButtonColorHelper.applyPrimaryColor(btnTest, color);
        }

        updateExactAlarmButtonLabel(content);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.section_notifications)
                .setView(content)
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
        notificationToolsDialog = dialog;
        applyDialogButtonColors(dialog);

        btnTimes.setOnClickListener(v -> {
            dialog.dismiss();
            showNotificationTimesDialog();
        });
        btnPermission.setOnClickListener(v -> {
            dialog.dismiss();
            handleNotificationPermissionAction();
        });
        btnExact.setOnClickListener(v -> {
            dialog.dismiss();
            openExactAlarmSettings();
        });
        btnBattery.setOnClickListener(v -> {
            dialog.dismiss();
            openBatteryOptimizationSettings();
        });
        btnTest.setOnClickListener(v -> sendTestNotification());
    }

    private void updateExactAlarmButtonLabel(View root) {
        if (root == null) {
            return;
        }
        MaterialButton btnExact = root.findViewById(R.id.btn_notification_exact);
        if (btnExact == null) {
            return;
        }
        MaterialButton btnPermission = root.findViewById(R.id.btn_notification_permission);
        if (btnPermission != null) {
            btnPermission.setText(getString(R.string.notification_permission_button_label, getNotificationPermissionStatusText()));
        }
        btnExact.setText(getString(R.string.exact_alarm_button_label, getExactAlarmStatusText()));
        MaterialButton btnBattery = root.findViewById(R.id.btn_notification_battery_opt);
        if (btnBattery != null) {
            btnBattery.setText(getString(R.string.battery_opt_button_label, getBatteryOptStatusText()));
        }
        updateNotificationTimesButtonLabel(root);
    }

    private String getNotificationPermissionStatusText() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return getString(R.string.notification_permission_status_not_required);
        }
        boolean granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
        return granted
                ? getString(R.string.notification_permission_status_allowed)
                : getString(R.string.notification_permission_status_blocked);
    }

    private void handleNotificationPermissionAction() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            openAppNotificationSettings();
            return;
        }
        boolean granted = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            openAppNotificationSettings();
        } else {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void openAppNotificationSettings() {
        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            startActivity(intent);
            return;
        }
        Intent appDetails = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        appDetails.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(appDetails);
    }

    private void sendTestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            showToast(R.string.notification_test_blocked_permission);
            return;
        }

        NotificationManagerCompat managerCompat = NotificationManagerCompat.from(requireContext());
        if (!managerCompat.areNotificationsEnabled()) {
            showToast(R.string.notification_test_blocked_system);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                NotificationChannel channel = manager.getNotificationChannel("reminder_channel");
                if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    showToast(R.string.notification_test_blocked_system);
                    return;
                }
            }
        }

        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", getString(R.string.notification_test_title));
        intent.putExtra("message", getString(R.string.notification_test_message));
        requireContext().sendBroadcast(intent);
        showToast(R.string.notification_test_sent);
    }

    private void updateExactAlarmButtonLabel(AlertDialog dialog) {
        if (dialog == null) {
            return;
        }
        updateExactAlarmButtonLabel(dialog.getWindow() != null ? dialog.getWindow().getDecorView() : null);
    }

    private String getExactAlarmStatusText() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return getString(R.string.exact_alarm_status_not_required);
        }
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
            return getString(R.string.exact_alarm_status_allowed);
        }
        return getString(R.string.exact_alarm_status_blocked);
    }

    private String getBatteryOptStatusText() {
        android.os.PowerManager powerManager =
                (android.os.PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return getString(R.string.battery_opt_status_unknown);
        }
        boolean ignoring = powerManager.isIgnoringBatteryOptimizations(requireContext().getPackageName());
        return ignoring
                ? getString(R.string.battery_opt_status_disabled)
                : getString(R.string.battery_opt_status_enabled);
    }

    private void updateNotificationTimesButtonLabel(View root) {
        if (root == null) {
            return;
        }
        MaterialButton btnTimes = root.findViewById(R.id.btn_notification_times);
        if (btnTimes == null) {
            return;
        }
        Integer removalHours = viewModel.getRemovalReminderHours().getValue();
        Integer insertionHours = viewModel.getInsertionReminderHours().getValue();
        if (removalHours == null || insertionHours == null) {
            btnTimes.setText(getString(R.string.notification_times_button_unknown));
            return;
        }
        btnTimes.setText(getString(R.string.notification_times_button_label,
                removalHours, insertionHours));
    }

    private void adjustDebugTimeToMidnight() {
        Calendar base = DebugTimeProvider.now(viewModel.getRepository());
        base.set(Calendar.HOUR_OF_DAY, 0);
        base.set(Calendar.MINUTE, 0);
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);
        applyDebugTime(base);
        Toast.makeText(requireContext(), getString(R.string.debug_jump_midnight), Toast.LENGTH_SHORT).show();
    }

    private void shiftDebugTime(int field, int amount) {
        Calendar base = DebugTimeProvider.now(viewModel.getRepository());
        base.add(field, amount);
        applyDebugTime(base);
        int message = amount >= 0 ? R.string.debug_time_shift_forward : R.string.debug_time_shift_back;
        Toast.makeText(requireContext(), getString(message), Toast.LENGTH_SHORT).show();
    }

    private enum DebugPreset {
        REMOVAL_BEFORE,
        REMOVAL_AT,
        INSERTION_BEFORE,
        INSERTION_AT
    }

    private void applyDebugPreset(DebugPreset preset) {
        Calendar baseStart = viewModel.getStartDate().getValue();
        Integer cycleLength = viewModel.getCycleLength().getValue();
        if (baseStart == null || cycleLength == null) {
            return;
        }

        Calendar currentStart = (Calendar) baseStart.clone();
        currentStart.set(Calendar.SECOND, 0);
        currentStart.set(Calendar.MILLISECOND, 0);
        int delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
        int ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        int guard = 0;
        Calendar systemNow = Calendar.getInstance();
        while (systemNow.after(reinsertionDate) && guard < 300) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
            ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            guard++;
        }

        Calendar selected;
        switch (preset) {
            case REMOVAL_BEFORE:
                selected = (Calendar) removalDate.clone();
                selected.add(Calendar.HOUR_OF_DAY, -1);
                break;
            case REMOVAL_AT:
                selected = (Calendar) removalDate.clone();
                break;
            case INSERTION_BEFORE:
                selected = (Calendar) reinsertionDate.clone();
                selected.add(Calendar.HOUR_OF_DAY, -1);
                break;
            case INSERTION_AT:
                selected = (Calendar) reinsertionDate.clone();
                break;
            default:
                return;
        }

        applyDebugTime(selected);
        Toast.makeText(requireContext(), getString(R.string.debug_preset_applied), Toast.LENGTH_SHORT).show();
    }

    private void applyDebugTime(Calendar calendar) {
        viewModel.getRepository().setDebugTimeMillis(calendar.getTimeInMillis());
        viewModel.getRepository().setDebugTimeEnabled(true);
        switchDebugTime.setChecked(true);
        updateDebugTimeStatus();
        WidgetUpdater.updateAllWidgets(requireContext());
        Calendar current = viewModel.getStartDate().getValue();
        if (current != null) {
            viewModel.setStartDate((Calendar) current.clone());
        }
    }

    private void updateDebugSectionVisibility() {
        if (debugSection == null) {
            return;
        }
        boolean enabled = viewModel.getRepository().isDebugToolsEnabled();
        debugSection.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (enabled) {
            switchDebugTime.setChecked(viewModel.getRepository().isDebugTimeEnabled());
            updateDebugTimeStatus();
        }
    }

    private void updateDebugTimeStatus() {
        if (tvDebugTimeStatus == null) {
            return;
        }
        SettingsRepository repository = viewModel.getRepository();
        if (!repository.isDebugToolsEnabled() || !repository.isDebugTimeEnabled()) {
            tvDebugTimeStatus.setText(R.string.debug_time_status_system);
            return;
        }
        long millis = repository.getDebugTimeMillis();
        if (millis <= 0L) {
            tvDebugTimeStatus.setText(R.string.debug_time_status_system);
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        String timeText = format.format(millis);
        tvDebugTimeStatus.setText(getString(R.string.debug_time_status_override, timeText));
    }

    private void bindDebugHint(View view, int stringRes) {
        if (view == null) {
            return;
        }
        view.setOnLongClickListener(v -> {
            Toast.makeText(requireContext(), getString(stringRes), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    private String buildDebugInfo() {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        Calendar now = DebugTimeProvider.now(viewModel.getRepository());

        Calendar baseStart = viewModel.getStartDate().getValue();
        if (baseStart == null) {
            baseStart = Calendar.getInstance();
        }
        int cycleLength = getSafeCycleLength();
        int ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(baseStart.getTimeInMillis());
        int removalReminderHours = viewModel.getRepository().getRemovalReminderHours();
        int insertionReminderHours = viewModel.getRepository().getInsertionReminderHours();
        int delayDays = viewModel.getRepository().getCycleDelayDays(baseStart.getTimeInMillis());

        Calendar currentStart = (Calendar) baseStart.clone();
        Calendar removalDate = (Calendar) currentStart.clone();
        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
        Calendar reinsertionDate = (Calendar) removalDate.clone();
        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

        int guard = 0;
        while (now.after(reinsertionDate) && guard < 500) {
            currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
            delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
            ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
            removalDate = (Calendar) currentStart.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
            reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
            guard++;
        }

        Integer pastAmount = viewModel.getCalendarPastAmount().getValue();
        String pastUnit = viewModel.getCalendarPastUnit().getValue();
        Integer futureAmount = viewModel.getCalendarFutureAmount().getValue();
        String futureUnit = viewModel.getCalendarFutureUnit().getValue();

        Calendar pastLimit = (Calendar) now.clone();
        pastLimit.add(Calendar.MONTH, -Math.max(convertToMonths(pastAmount, pastUnit), 0));
        Calendar futureLimit = (Calendar) now.clone();
        futureLimit.add(Calendar.MONTH, Math.max(convertToMonths(futureAmount, futureUnit), 0));

        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.debug_now)).append(": ").append(format.format(now.getTime())).append("\n");
        sb.append(getString(R.string.debug_base_start)).append(": ").append(format.format(baseStart.getTime())).append("\n");
        sb.append(getString(R.string.debug_cycle_length, cycleLength)).append("\n");
        sb.append(getString(R.string.debug_ring_free, ringFreeDays)).append("\n");
        sb.append(getString(R.string.debug_removal_reminder, removalReminderHours)).append("\n");
        sb.append(getString(R.string.debug_insertion_reminder, insertionReminderHours)).append("\n");
        sb.append(getString(R.string.debug_current_start)).append(": ").append(format.format(currentStart.getTime())).append("\n");
        sb.append(getString(R.string.debug_removal)).append(": ").append(format.format(removalDate.getTime())).append("\n");
        sb.append(getString(R.string.debug_insertion)).append(": ").append(format.format(reinsertionDate.getTime())).append("\n");
        sb.append(getString(R.string.debug_notified_for_cycle)).append(": ")
                .append(viewModel.getRepository().wasNotificationScheduledForCycle(currentStart.getTimeInMillis()))
                .append("\n");
        sb.append(getString(R.string.debug_calendar_range)).append(": ")
                .append(pastAmount).append(" ").append(unitLabel(pastUnit))
                .append(" ").append(getString(R.string.settings_calendar_range_back))
                .append(", ")
                .append(futureAmount).append(" ").append(unitLabel(futureUnit))
                .append(" ").append(getString(R.string.settings_calendar_range_forward))
                .append("\n");
        sb.append(getString(R.string.debug_calendar_limits)).append(": ")
                .append(format.format(pastLimit.getTime()))
                .append(" -> ")
                .append(format.format(futureLimit.getTime()))
                .append("\n\n");

        sb.append(getString(R.string.debug_scheduled_notifications)).append(":\n");
        appendNotificationLine(sb, getString(R.string.debug_two_weeks_remaining), shiftDays(removalDate, -14), cycleLength >= 14);
        appendNotificationLine(sb, getString(R.string.debug_one_week_remaining), shiftDays(removalDate, -7), cycleLength >= 7);
        appendNotificationLine(sb, getString(R.string.debug_removal_reminder_line, removalReminderHours),
                shiftHours(removalDate, -removalReminderHours), removalReminderHours > 0);
        appendNotificationLine(sb, getString(R.string.debug_removal), removalDate, true);
        appendNotificationLine(sb, getString(R.string.debug_insertion_reminder_line, insertionReminderHours),
                shiftHours(reinsertionDate, -insertionReminderHours), insertionReminderHours > 0);
        appendNotificationLine(sb, getString(R.string.debug_insertion), reinsertionDate, true);

        return sb.toString();
    }

    private int getSafeCycleLength() {
        Integer cycleLength = viewModel.getCycleLength().getValue();
        return cycleLength != null && cycleLength > 0 ? cycleLength : 21;
    }

    private int convertToMonths(Integer amount, String unit) {
        if (amount == null || unit == null) {
            return 0;
        }
        return "years".equals(unit) ? amount * 12 : amount;
    }

    private Calendar shiftDays(Calendar base, int days) {
        Calendar cal = (Calendar) base.clone();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal;
    }

    private Calendar shiftHours(Calendar base, int hours) {
        Calendar cal = (Calendar) base.clone();
        cal.add(Calendar.HOUR_OF_DAY, hours);
        return cal;
    }

    private void appendNotificationLine(StringBuilder sb, String label, Calendar time, boolean enabled) {
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        boolean inPast = time.getTimeInMillis() <= System.currentTimeMillis();
        sb.append("- ").append(label).append(": ").append(format.format(time.getTime()));
        sb.append(enabled ? "" : getString(R.string.debug_disabled_suffix));
        sb.append(inPast ? getString(R.string.debug_past_suffix) : getString(R.string.debug_future_suffix));
        sb.append("\n");
    }
}
