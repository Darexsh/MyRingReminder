package com.darexsh.veri_aristo;

import android.annotation.SuppressLint;
import android.animation.ObjectAnimator;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import androidx.annotation.Nullable;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;
import com.google.android.material.button.MaterialButton;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import android.util.Log;
import android.view.animation.LinearInterpolator;

// HomeFragment displays the current cycle status and allows users to manage their cycle settings
public class HomeFragment extends Fragment {

    private static final int NOTIFY_TWO_WEEKS = 0;
    private static final int NOTIFY_ONE_WEEK = 1;
    private static final int NOTIFY_REMOVAL_REMINDER = 2;
    private static final int NOTIFY_REMOVAL_EXACT = 3;
    private static final int NOTIFY_INSERTION_REMINDER = 4;
    private static final int NOTIFY_INSERTION_EXACT = 5;
    private SharedViewModel viewModel;
    private int currentCircleStyle = SettingsRepository.DEFAULT_HOME_CIRCLE_STYLE;
    private int currentCircleColor = SettingsRepository.DEFAULT_HOME_CIRCLE_COLOR;
    private float pulsePhase = 0f;
    private boolean pulseUp = true;
    private static final long SPECIAL_ACTIONS_AUTO_HIDE_MS = 7000L;
    private static final long UPDATE_HERO_AUTO_HIDE_MS = 3000L;
    private static final long UPDATE_HERO_MIN_CHECK_MS = 1000L;
    private static final String RELEASES_URL = "https://api.github.com/repos/Darexsh/MyRingReminder/releases";
    // Temporary UI test switch: force "update available" state even when versions match.
    private static final boolean FORCE_UPDATE_AVAILABLE_FOR_TEST = false;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    @Nullable
    private Runnable specialActionsAutoHideRunnable;
    @Nullable
    private Runnable updateHeroAutoHideRunnable;
    @Nullable
    private ObjectAnimator updateOrbitAnimator;
    @Nullable
    private ReleaseInfo lastReleaseInfo;
    @Nullable
    private String currentVersionLabel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        final HomeCircleView circularProgress = view.findViewById(R.id.circularProgress); // Home circle for cycle status
        final TextView tvRemovalDate = view.findViewById(R.id.tv_removal_date); // TextView to display the removal date
        final TextView tvSecondaryDate = view.findViewById(R.id.tv_secondary_date);
        final TextView tvDaysNumber = view.findViewById(R.id.tv_days_number);   // TextView to display the number of days left
        final TextView tvDaysLabel = view.findViewById(R.id.tv_days_left_label);    // TextView to display the label for days left
        final ImageView backgroundImageView = view.findViewById(R.id.background_image); // ImageView for the background image
        final View backgroundDimOverlay = view.findViewById(R.id.background_dim_overlay);
        final MaterialButton btnDelayCycle = view.findViewById(R.id.btn_delay_cycle);
        final MaterialButton btnSkipRingFree = view.findViewById(R.id.btn_skip_ring_free_week);
        final TextView btnDelayInfo = view.findViewById(R.id.btn_delay_info);
        final TextView btnSkipRingFreeInfo = view.findViewById(R.id.btn_skip_ring_free_info);
        final LinearLayout specialActionsPanel = view.findViewById(R.id.home_delay_row);
        final MaterialButton btnSpecialActionsToggle = view.findViewById(R.id.btn_special_actions_toggle);
        final View updateHeroContainer = view.findViewById(R.id.update_hero_container);
        final View updateHeroCard = view.findViewById(R.id.update_hero_card);
        final TextView tvUpdateHeroTitle = view.findViewById(R.id.tv_update_hero_title);
        final LottieAnimationView lavUpdateHero = view.findViewById(R.id.lav_update_hero_status);
        final View updateHeroOrbitIcon = view.findViewById(R.id.update_hero_orbit_icon);
        final TextView tvUpdateHeroStatus = view.findViewById(R.id.tv_update_hero_status);
        final boolean[] specialActionsExpanded = new boolean[]{false};

        SharedViewModelFactory factory = new SharedViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(SharedViewModel.class);
        currentVersionLabel = getCurrentVersionLabel();
        applyUpdateHeroPillWidth(updateHeroContainer, updateHeroCard);

        viewModel.getButtonColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                ButtonColorHelper.applyPrimaryColor(btnDelayCycle, color);
                ButtonColorHelper.applyPrimaryColor(btnSkipRingFree, color);
                ButtonColorHelper.applyPrimaryColor(btnSpecialActionsToggle, color);
                applyDelayInfoIconTint(btnDelayInfo, color);
                applyDelayInfoIconTint(btnSkipRingFreeInfo, color);
            }
        });
        btnDelayInfo.setOnClickListener(v -> {
            showDelayInfoDialog();
            resetSpecialActionsAutoHide(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
        });
        btnSkipRingFreeInfo.setOnClickListener(v -> {
            showSkipRingFreeInfoDialog();
            resetSpecialActionsAutoHide(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
        });
        btnSpecialActionsToggle.setOnClickListener(v -> {
            specialActionsExpanded[0] = !specialActionsExpanded[0];
            if (specialActionsExpanded[0]) {
                specialActionsPanel.setAlpha(0f);
                specialActionsPanel.setScaleX(0.94f);
                specialActionsPanel.setScaleY(0.94f);
                specialActionsPanel.setTranslationY(10f);
                specialActionsPanel.setVisibility(View.VISIBLE);
                specialActionsPanel.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(180L)
                        .start();
                btnSpecialActionsToggle.setContentDescription(getString(R.string.home_special_actions_toggle_close));
                resetSpecialActionsAutoHide(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
            } else {
                hideSpecialActionsPanel(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
            }
        });
        viewModel.getHomeCircleColor().observe(getViewLifecycleOwner(), color -> {
            if (color != null) {
                currentCircleColor = color;
                circularProgress.setIndicatorColor(color);
                applyHomeCircleStyle(circularProgress, currentCircleStyle, currentCircleColor);
            }
        });

        viewModel.getHomeCircleStyle().observe(getViewLifecycleOwner(), style -> {
            if (style != null) {
                currentCircleStyle = style;
                applyHomeCircleStyle(circularProgress, currentCircleStyle, currentCircleColor);
            }
        });

        // Set background image if available
        Runnable loadBackgroundImage = () -> {
            boolean useGlobalBackground = Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue());
            if (useGlobalBackground) {
                backgroundImageView.setVisibility(View.GONE);
                backgroundDimOverlay.setVisibility(View.GONE);
                backgroundDimOverlay.setAlpha(0f);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    backgroundImageView.setRenderEffect(null);
                }
                return;
            }
            backgroundImageView.setVisibility(View.VISIBLE);
            String uriStr = viewModel.getBackgroundImageUri().getValue();

            if (uriStr != null) {
                try {
                    Uri uri = Uri.parse(uriStr);
                    String scheme = uri.getScheme();
                    if ("file".equalsIgnoreCase(scheme)) {
                        File file = new File(Objects.requireNonNull(uri.getPath()));
                        if (file.exists()) {
                            try (InputStream inputStream = new FileInputStream(file)) {
                                Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());
                                backgroundImageView.setImageDrawable(drawable);
                            }
                        } else {
                            backgroundImageView.setImageResource(R.drawable.default_bg);
                            viewModel.setBackgroundImageUri(null);
                        }
                    } else {
                        boolean hasPermission = false;
                        for (UriPermission perm : requireContext().getContentResolver().getPersistedUriPermissions()) {
                            if (perm.getUri().equals(uri) && perm.isReadPermission()) {
                                hasPermission = true;
                                break;
                            }
                        }

                        if (hasPermission) {
                            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
                                Drawable drawable = Drawable.createFromStream(inputStream, uri.toString());
                                backgroundImageView.setImageDrawable(drawable);
                            }
                        } else {
                            backgroundImageView.setImageResource(R.drawable.default_bg);
                            viewModel.setBackgroundImageUri(null);
                        }
                    }

                } catch (SecurityException | IOException e) {
                    Log.e("HomeFragment", "Failed to load background image", e);
                    backgroundImageView.setImageResource(R.drawable.default_bg);
                    viewModel.setBackgroundImageUri(null);
                }
            } else {
                backgroundImageView.setImageResource(R.drawable.default_bg);
            }
        };

        // Observe changes in the ViewModel
        viewModel.getBackgroundImageUri().observe(getViewLifecycleOwner(), uri -> loadBackgroundImage.run());
        viewModel.getBackgroundAllScreensEnabled().observe(getViewLifecycleOwner(), enabled -> loadBackgroundImage.run());
        viewModel.getBackgroundDimPercent().observe(getViewLifecycleOwner(), percent -> {
            if (Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue())) {
                backgroundDimOverlay.setVisibility(View.GONE);
                backgroundDimOverlay.setAlpha(0f);
                return;
            }
            int safePercent = percent != null ? Math.max(0, Math.min(100, percent)) : 0;
            if (safePercent == 0) {
                backgroundDimOverlay.setVisibility(View.GONE);
                backgroundDimOverlay.setAlpha(0f);
                return;
            }
            backgroundDimOverlay.setAlpha(safePercent / 100f);
            backgroundDimOverlay.setVisibility(View.VISIBLE);
        });
        viewModel.getBackgroundBlurDashboardPercent().observe(getViewLifecycleOwner(), percent -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return;
            }
            if (Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue())) {
                backgroundImageView.setRenderEffect(null);
                return;
            }
            int safePercent = percent != null ? Math.max(0, Math.min(100, percent)) : 0;
            float radiusPx = percentToBlurRadiusPx(safePercent);
            if (radiusPx <= 0f) {
                backgroundImageView.setRenderEffect(null);
                return;
            }
            backgroundImageView.setRenderEffect(
                    RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP));
        });

        // Observe changes in the ViewModel and update the UI accordingly
        Runnable updateUi = () -> {
            Calendar vmStartDate = viewModel.getStartDate().getValue();
            Integer cycleLength = viewModel.getCycleLength().getValue();

            if (vmStartDate == null || cycleLength == null) {
                // Data not yet loaded
                return;
            }

            // Initialize calendar instances for cycle calculations
            Calendar displayNow = DebugTimeProvider.now(viewModel.getRepository());
            Calendar systemNow = Calendar.getInstance();
            Calendar startDate = (Calendar) vmStartDate.clone();
            startDate.set(Calendar.SECOND, 0);
            startDate.set(Calendar.MILLISECOND, 0);

            int delayDays = viewModel.getRepository().getCycleDelayDays(startDate.getTimeInMillis());
            int ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(startDate.getTimeInMillis());
            Calendar removalDate = (Calendar) startDate.clone();
            removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);

            Calendar reinsertionDate = (Calendar) removalDate.clone();
            reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

            // Retrieve cycle history from preferences
            List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();
            Calendar tempStartDate = (Calendar) startDate.clone();
            Calendar tempRemovalDate = (Calendar) removalDate.clone();
            Calendar tempReinsertionDate = (Calendar) reinsertionDate.clone();

            Calendar lastSavedReinsertionDate = getLastSavedReinsertionDate(cycleHistory);
            long lastSavedReinsertionMillis = lastSavedReinsertionDate != null ? lastSavedReinsertionDate.getTimeInMillis() : 0;

            int maxCycles = 100;
            int count = 0;

            // Calculate cycles until today if last reinsertion date is in the past
            while (tempReinsertionDate.getTimeInMillis() <= systemNow.getTimeInMillis() && count < maxCycles) {
                if (tempReinsertionDate.getTimeInMillis() > lastSavedReinsertionMillis) {
                    saveCycleToHistory(viewModel, tempStartDate.getTimeInMillis(), tempRemovalDate.getTimeInMillis(), CycleType.INSERTION);
                    saveCycleToHistory(viewModel, tempRemovalDate.getTimeInMillis(), tempReinsertionDate.getTimeInMillis(), CycleType.REMOVAL);
                }

                int tempDelayDays = viewModel.getRepository().getCycleDelayDays(tempStartDate.getTimeInMillis());
                int tempRingFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(tempStartDate.getTimeInMillis());
                tempStartDate.add(Calendar.DAY_OF_MONTH, cycleLength + tempRingFreeDays + tempDelayDays);
                tempRemovalDate = (Calendar) tempStartDate.clone();
                tempRemovalDate.add(Calendar.DAY_OF_MONTH, cycleLength + tempDelayDays);
                tempReinsertionDate = (Calendar) tempRemovalDate.clone();
                tempReinsertionDate.add(Calendar.DAY_OF_MONTH, viewModel.getRepository()
                        .getRingFreeDaysForCycle(tempStartDate.getTimeInMillis()));
                count++;
            }

            Calendar nowDay = startOfDay(displayNow);
            Calendar reinsertionDay = startOfDay(reinsertionDate);

            // Adjust start date to the current cycle window (day-based to avoid skipping the current day)
            while (nowDay.after(reinsertionDay)) {
                startDate.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
                delayDays = viewModel.getRepository().getCycleDelayDays(startDate.getTimeInMillis());
                ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(startDate.getTimeInMillis());
                removalDate = (Calendar) startDate.clone();
                removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                reinsertionDate = (Calendar) removalDate.clone();
                reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
                reinsertionDay = startOfDay(reinsertionDate);
            }

            // Add phases as soon as each phase ends, even if the app wasn't opened at the exact time.
            if (systemNow.equals(removalDate) || systemNow.after(removalDate)) {
                saveCycleToHistory(viewModel, startDate.getTimeInMillis(),
                        removalDate.getTimeInMillis(), CycleType.INSERTION);
            }
            if (systemNow.equals(reinsertionDate) || systemNow.after(reinsertionDate)) {
                saveCycleToHistory(viewModel, removalDate.getTimeInMillis(),
                        reinsertionDate.getTimeInMillis(), CycleType.REMOVAL);
            }

            int remainingDays;
            String labelText;
            String primaryTextDate;
            String secondaryTextDate;
            int progressMax;
            int progressValue;

            // Calculate remaining days and progress based on day changes (midnight), except at removal/insertion time
            if (displayNow.before(removalDate)) {
                remainingDays = daysBetweenDays(displayNow, removalDate);

                labelText = getString(R.string.home_days_left);
                progressMax = cycleLength;
                progressValue = progressMax - remainingDays;

                @SuppressLint("DefaultLocale") String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                @SuppressLint("DefaultLocale") String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_removal_on, removalDateText);
                secondaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
            } else if (displayNow.before(reinsertionDate)) {
                remainingDays = daysBetweenDays(displayNow, reinsertionDate);

                labelText = getString(R.string.home_days_until_insertion);
                progressMax = Math.max(1, ringFreeDays);
                progressValue = progressMax - remainingDays;

                @SuppressLint("DefaultLocale") String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                @SuppressLint("DefaultLocale") String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
                secondaryTextDate = getString(R.string.home_removal_on, removalDateText);
            } else {
                remainingDays = cycleLength;
                labelText = getString(R.string.home_days_left);
                progressMax = cycleLength;
                progressValue = 0;

                @SuppressLint("DefaultLocale") String removalDateText = String.format("%02d.%02d.%d",
                        removalDate.get(Calendar.DAY_OF_MONTH),
                        removalDate.get(Calendar.MONTH) + 1,
                        removalDate.get(Calendar.YEAR));
                @SuppressLint("DefaultLocale") String reinsertionDateText = String.format("%02d.%02d.%d",
                        reinsertionDate.get(Calendar.DAY_OF_MONTH),
                        reinsertionDate.get(Calendar.MONTH) + 1,
                        reinsertionDate.get(Calendar.YEAR));
                primaryTextDate = getString(R.string.home_removal_on, removalDateText);
                secondaryTextDate = getString(R.string.home_insertion_on, reinsertionDateText);
            }

            circularProgress.setMax(progressMax);
            circularProgress.setProgress(progressValue);
            tvDaysNumber.setText(String.valueOf(remainingDays));
            tvDaysLabel.setText(labelText);
            @SuppressLint("DefaultLocale") String timeText = String.format("%02d:%02d", startDate.get(Calendar.HOUR_OF_DAY), startDate.get(Calendar.MINUTE));
            String timeSuffix = getString(R.string.home_at_time, timeText);
            primaryTextDate = primaryTextDate + " " + timeSuffix;
            secondaryTextDate = secondaryTextDate + " " + timeSuffix;
            tvRemovalDate.setText(primaryTextDate);
            tvSecondaryDate.setText(secondaryTextDate);

            long cycleStartMillis = startDate.getTimeInMillis();
            int settingsHash = viewModel.getRepository().getNotificationSettingsHash();
            int scheduledHash = viewModel.getRepository().getNotificationSettingsHashForCycle(cycleStartMillis);
            boolean hasPendingAlarms = ReminderScheduler.hasAnyScheduledForCycle(requireContext(), cycleStartMillis);
            if (systemNow.before(reinsertionDate)
                    && (!viewModel.getRepository().wasNotificationScheduledForCycle(cycleStartMillis)
                    || scheduledHash != settingsHash
                    || !hasPendingAlarms)) {
                scheduleRingCycleNotifications(
                        viewModel,
                        (Calendar) startDate.clone(),
                        (Calendar) removalDate.clone(),
                        (Calendar) reinsertionDate.clone(),
                        cycleLength
                );
                viewModel.getRepository().setNotificationScheduledForCycle(cycleStartMillis);
                viewModel.getRepository().setNotificationSettingsHashForCycle(cycleStartMillis, settingsHash);
            }
        };

        // Observe changes in the ViewModel and update the UI
        viewModel.getStartDate().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getCycleLength().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getRemovalReminderHours().observe(getViewLifecycleOwner(), val -> updateUi.run());
        viewModel.getInsertionReminderHours().observe(getViewLifecycleOwner(), val -> updateUi.run());

        btnDelayCycle.setOnClickListener(v -> {
            showDelayDialog(viewModel);
            resetSpecialActionsAutoHide(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
        });
        btnSkipRingFree.setOnClickListener(v -> {
            showSkipRingFreeWeekDialog(viewModel);
            resetSpecialActionsAutoHide(specialActionsPanel, btnSpecialActionsToggle, specialActionsExpanded);
        });
        updateHeroCard.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.98f)
                    .scaleY(0.98f)
                    .setDuration(70L)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(90L).start())
                    .start();
            showUpdateHeroDetailsDialog();
        });

        updateUi.run();
        maybeStartUpdateHeroCheck(
                updateHeroContainer,
                tvUpdateHeroTitle,
                lavUpdateHero,
                updateHeroOrbitIcon,
                tvUpdateHeroStatus
        );
        return view;
    }

    private void maybeStartUpdateHeroCheck(View container,
                                           TextView titleBadge,
                                           LottieAnimationView statusLottie,
                                           View orbitIcon,
                                           TextView statusText) {
        if (!isAdded()) {
            return;
        }
        if (requireActivity() instanceof MainActivity) {
            MainActivity activity = (MainActivity) requireActivity();
            if (!activity.consumeStartupUpdateCheckRequest()) {
                container.setVisibility(View.GONE);
                return;
            }
        }

        showUpdateHero(container, titleBadge, statusLottie, orbitIcon, statusText);
        new Thread(() -> {
            try {
                long startedAt = System.currentTimeMillis();
                ReleaseInfo releaseInfo = fetchLatestReleaseInfo();
                long elapsed = System.currentTimeMillis() - startedAt;
                long waitRemaining = UPDATE_HERO_MIN_CHECK_MS - elapsed;
                if (waitRemaining > 0L) {
                    Thread.sleep(waitRemaining);
                }
                lastReleaseInfo = releaseInfo;
                if (releaseInfo == null || releaseInfo.downloadUrl == null) {
                    showUpdateHeroStatusAndHide(container, titleBadge, statusLottie, orbitIcon, statusText, R.string.update_startup_failed, "!");
                    return;
                }
                String currentVersion = getCurrentVersionName();
                int compare = FORCE_UPDATE_AVAILABLE_FOR_TEST
                        ? -1
                        : compareVersions(currentVersion, releaseInfo.versionName);
                if (compare >= 0) {
                    showUpdateHeroStatusAndHide(container, titleBadge, statusLottie, orbitIcon, statusText, R.string.update_startup_latest, "✓");
                    return;
                }
                showUpdateHeroAvailablePersistent(container, titleBadge, statusLottie, orbitIcon, statusText);
            } catch (Exception e) {
                Log.e("HomeFragment", "Dashboard update check failed", e);
                showUpdateHeroStatusAndHide(container, titleBadge, statusLottie, orbitIcon, statusText, R.string.update_startup_failed, "!");
            }
        }).start();
    }

    private void showUpdateHero(View container,
                                TextView titleBadge,
                                LottieAnimationView statusLottie,
                                View orbitIcon,
                                TextView statusText) {
        if (!isAdded()) {
            return;
        }
        container.setVisibility(View.VISIBLE);
        container.setAlpha(0f);
        container.setTranslationY(-100f);
        container.animate().alpha(1f).setDuration(220L).start();
        SpringAnimation springAnimation = new SpringAnimation(container, DynamicAnimation.TRANSLATION_Y, 0f);
        SpringForce springForce = new SpringForce(0f);
        springForce.setDampingRatio(0.8f);
        springForce.setStiffness(400f);
        springAnimation.setSpring(springForce);
        springAnimation.start();
        if (titleBadge != null && statusLottie != null) {
            morphUpdateHeroBadge(titleBadge, statusLottie, orbitIcon, "↻", true);
        }
        if (statusText != null) {
            setUpdateStatusText(statusText, getString(R.string.update_startup_checking));
        }
    }

    private void showUpdateHeroStatusAndHide(View container,
                                             TextView titleBadge,
                                             LottieAnimationView statusLottie,
                                             View orbitIcon,
                                             TextView statusText,
                                             int statusResId,
                                             String badgeSymbol) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            if (statusText != null) {
                setUpdateStatusText(statusText, getString(statusResId));
            }
            if (titleBadge != null && statusLottie != null) {
                morphUpdateHeroBadge(titleBadge, statusLottie, orbitIcon, badgeSymbol, false);
            }
            if (updateHeroAutoHideRunnable != null) {
                uiHandler.removeCallbacks(updateHeroAutoHideRunnable);
            }
            updateHeroAutoHideRunnable = () -> hideUpdateHero(container);
            uiHandler.postDelayed(updateHeroAutoHideRunnable, UPDATE_HERO_AUTO_HIDE_MS);
        });
    }

    private void showUpdateHeroAvailablePersistent(View container,
                                                   TextView titleBadge,
                                                   LottieAnimationView statusLottie,
                                                   View orbitIcon,
                                                   TextView statusText) {
        if (!isAdded()) {
            return;
        }
        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) {
                return;
            }
            if (statusText != null) {
                setUpdateStatusText(statusText, getString(R.string.update_available_title));
            }
            if (titleBadge != null && statusLottie != null) {
                morphUpdateHeroBadge(titleBadge, statusLottie, orbitIcon, "↑", false);
            }
            // Keep banner visible when an update is available.
            if (updateHeroAutoHideRunnable != null) {
                uiHandler.removeCallbacks(updateHeroAutoHideRunnable);
                updateHeroAutoHideRunnable = null;
            }
            container.setAlpha(1f);
            container.setTranslationY(0f);
        });
    }

    private void hideUpdateHero(View container) {
        if (!isAdded()) {
            return;
        }
        container.animate()
                .alpha(0f)
                .translationY(-24f)
                .setDuration(360L)
                .withEndAction(() -> {
                    container.setVisibility(View.GONE);
                    container.setTranslationY(0f);
                })
                .start();
    }

    private void applyUpdateHeroPillWidth(@Nullable View container, @Nullable View card) {
        if (container == null || card == null) {
            return;
        }
        container.post(() -> {
            if (!isAdded()) {
                return;
            }
            int screenWidth = getResources().getDisplayMetrics().widthPixels;
            int targetWidth = (int) (screenWidth * 0.55f);
            ViewGroup.LayoutParams params = card.getLayoutParams();
            if (params != null) {
                params.width = targetWidth;
                card.setLayoutParams(params);
            }
        });
    }

    private void morphUpdateHeroBadge(TextView badgeIcon,
                                      LottieAnimationView statusLottie,
                                      View orbitIcon,
                                      String symbol,
                                      boolean showSpinner) {
        if (badgeIcon == null || statusLottie == null) {
            return;
        }
        if (showSpinner) {
            stopOrbitAnimation(orbitIcon);
            if (orbitIcon != null) {
                orbitIcon.setVisibility(View.GONE);
            }
            badgeIcon.setAlpha(0f);
            statusLottie.setVisibility(View.VISIBLE);
            statusLottie.setAlpha(1f);
            statusLottie.setAnimation("lottie_update_checking.json");
            statusLottie.setRepeatCount(LottieDrawable.INFINITE);
            statusLottie.playAnimation();
            badgeIcon.setText(symbol);
            return;
        }
        statusLottie.cancelAnimation();
        if ("↑".equals(symbol)) {
            statusLottie.setVisibility(View.GONE);
            statusLottie.setAlpha(1f);
            badgeIcon.setAlpha(0f);
            if (orbitIcon != null) {
                orbitIcon.setVisibility(View.VISIBLE);
                startOrbitAnimation(orbitIcon);
            }
            return;
        }
        stopOrbitAnimation(orbitIcon);
        if (orbitIcon != null) {
            orbitIcon.setVisibility(View.GONE);
        }
        boolean playDoneAnimation = "✓".equals(symbol);
        if (playDoneAnimation) {
            statusLottie.setAnimation("lottie_update_done.json");
            statusLottie.setRepeatCount(0);
            statusLottie.playAnimation();
        }
        statusLottie.animate()
                .alpha(0f)
                .setStartDelay(playDoneAnimation ? 260L : 60L)
                .setDuration(playDoneAnimation ? 180L : 140L)
                .withEndAction(() -> {
                    statusLottie.cancelAnimation();
                    statusLottie.setVisibility(View.GONE);
                    statusLottie.setAlpha(1f);
                })
                .start();
        badgeIcon.setText(symbol);
        badgeIcon.setScaleX(0.85f);
        badgeIcon.setScaleY(0.85f);
        badgeIcon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(160L)
                .start();
    }

    private void startOrbitAnimation(@Nullable View orbitIcon) {
        if (orbitIcon == null) {
            return;
        }
        stopOrbitAnimation(orbitIcon);
        updateOrbitAnimator = ObjectAnimator.ofFloat(orbitIcon, View.ROTATION, 0f, 360f);
        updateOrbitAnimator.setDuration(10000L);
        updateOrbitAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        updateOrbitAnimator.setInterpolator(new LinearInterpolator());
        updateOrbitAnimator.start();
    }

    private void stopOrbitAnimation(@Nullable View orbitIcon) {
        if (updateOrbitAnimator != null) {
            updateOrbitAnimator.cancel();
            updateOrbitAnimator = null;
        }
        if (orbitIcon != null) {
            orbitIcon.setRotation(0f);
        }
    }

    private void setUpdateStatusText(TextView target, String newText) {
        if (target == null) {
            return;
        }
        target.animate()
                .alpha(0f)
                .setDuration(150L)
                .withEndAction(() -> {
                    target.setText(newText);
                    target.animate().alpha(1f).setDuration(150L).start();
                })
                .start();
    }

    private void showUpdateHeroDetailsDialog() {
        if (!isAdded()) {
            return;
        }
        String current = currentVersionLabel != null ? currentVersionLabel : "—";
        String latest = (lastReleaseInfo != null && lastReleaseInfo.versionName != null)
                ? getString(R.string.update_hero_version_format, lastReleaseInfo.versionName, getCurrentVersionCode())
                : "—";

        StringBuilder body = new StringBuilder();
        body.append(getString(R.string.update_hero_details_current, current));
        body.append("\n");
        body.append(getString(R.string.update_hero_details_latest, latest));

        if (lastReleaseInfo != null && lastReleaseInfo.releaseNotes != null && !lastReleaseInfo.releaseNotes.trim().isEmpty()) {
            body.append("\n\n");
            body.append(getString(R.string.update_hero_details_changelog));
            body.append("\n");
            body.append(lastReleaseInfo.releaseNotes.trim());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.update_hero_details_title)
                .setMessage(body.toString())
                .setPositiveButton(R.string.dialog_ok, null);

        if (lastReleaseInfo != null && lastReleaseInfo.downloadUrl != null && lastReleaseInfo.versionName != null) {
            builder.setNegativeButton(R.string.update_install, (d, w) -> {
                if (isAdded() && requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).openUpdateBackupFlowFromHome();
                }
            });
        }
        AlertDialog dialog = builder.show();
        applyDialogButtonColors(dialog);
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
                String releaseNotes = release.has("body") && !release.get("body").isJsonNull()
                        ? release.get("body").getAsString()
                        : null;
                JsonArray assets = release.getAsJsonArray("assets");
                String downloadUrl = findApkAssetUrl(assets);
                return new ReleaseInfo(versionName, downloadUrl, releaseNotes);
            }
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private String getCurrentVersionName() {
        if (!isAdded()) {
            return null;
        }
        try {
            return requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private int getCurrentVersionCode() {
        if (!isAdded()) {
            return 0;
        }
        try {
            PackageInfo info = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) info.getLongVersionCode();
            }
            return info.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getCurrentVersionLabel() {
        String currentName = getCurrentVersionName();
        if (currentName == null || currentName.trim().isEmpty()) {
            return null;
        }
        return getString(R.string.update_hero_version_format, currentName, getCurrentVersionCode());
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
        final String releaseNotes;

        ReleaseInfo(String versionName, String downloadUrl, String releaseNotes) {
            this.versionName = versionName;
            this.downloadUrl = downloadUrl;
            this.releaseNotes = releaseNotes;
        }
    }

    private void showDelayInfoDialog() {
        Spanned message = Html.fromHtml(
                getString(R.string.home_delay_info_message),
                Html.FROM_HTML_MODE_LEGACY
        );
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_delay_info_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private void showSkipRingFreeInfoDialog() {
        Spanned message = Html.fromHtml(
                getString(R.string.home_skip_ring_free_info_message),
                Html.FROM_HTML_MODE_LEGACY
        );
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_delay_info_title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, null)
                .show();
    }

    private void applyDelayInfoIconTint(@Nullable TextView button, int buttonColor) {
        if (button == null) {
            return;
        }
        button.setBackgroundTintList(ColorStateList.valueOf(buttonColor));
        button.setTextColor(android.graphics.Color.WHITE);
    }

    private void resetSpecialActionsAutoHide(LinearLayout panel,
                                             MaterialButton toggle,
                                             boolean[] expandedState) {
        if (specialActionsAutoHideRunnable != null) {
            uiHandler.removeCallbacks(specialActionsAutoHideRunnable);
        }
        specialActionsAutoHideRunnable = () ->
                hideSpecialActionsPanel(panel, toggle, expandedState);
        uiHandler.postDelayed(specialActionsAutoHideRunnable, SPECIAL_ACTIONS_AUTO_HIDE_MS);
    }

    private void hideSpecialActionsPanel(LinearLayout panel,
                                         MaterialButton toggle,
                                         boolean[] expandedState) {
        if (specialActionsAutoHideRunnable != null) {
            uiHandler.removeCallbacks(specialActionsAutoHideRunnable);
            specialActionsAutoHideRunnable = null;
        }
        expandedState[0] = false;
        panel.animate()
                .alpha(0f)
                .scaleX(0.95f)
                .scaleY(0.95f)
                .translationY(8f)
                .setDuration(140L)
                .withEndAction(() -> {
                    panel.setVisibility(View.GONE);
                    panel.setAlpha(1f);
                    panel.setScaleX(1f);
                    panel.setScaleY(1f);
                    panel.setTranslationY(0f);
                })
                .start();
        toggle.setContentDescription(getString(R.string.home_special_actions_toggle_open));
    }

    private float percentToBlurRadiusPx(int percent) {
        float density = getResources().getDisplayMetrics().density;
        return (percent / 100f) * 28f * density;
    }

    @Override
    public void onDestroyView() {
        if (specialActionsAutoHideRunnable != null) {
            uiHandler.removeCallbacks(specialActionsAutoHideRunnable);
            specialActionsAutoHideRunnable = null;
        }
        if (updateHeroAutoHideRunnable != null) {
            uiHandler.removeCallbacks(updateHeroAutoHideRunnable);
            updateHeroAutoHideRunnable = null;
        }
        stopOrbitAnimation(null);
        super.onDestroyView();
    }

    // Save a cycle event to the history
    private void saveCycleToHistory(SharedViewModel viewModel, long dateMillis, long endDateMillis, CycleType type) {
        List<Cycle> cycleHistory = viewModel.getRepository().getCycleHistory();

        for (Cycle c : cycleHistory) {
            if (c.getDateMillis() == dateMillis
                    && c.getEndDateMillis() == endDateMillis
                    && c.getType() == type) {
                return; // Duplicate
            }
        }

        cycleHistory.add(new Cycle(dateMillis, endDateMillis, type));
        viewModel.getRepository().saveCycleHistory(cycleHistory);
    }

    // Get the last saved reinsertion date from the cycle history
    private Calendar getLastSavedReinsertionDate(List<Cycle> cycleHistory) {
        long latestReinsertionDate = 0;
        for (Cycle cycle : cycleHistory) {
            if (CycleType.REMOVAL == cycle.getType() && cycle.getEndDateMillis() > latestReinsertionDate) {
                latestReinsertionDate = cycle.getEndDateMillis();
            }
        }
        if (latestReinsertionDate == 0) return null;

        Calendar reinsertionDate = Calendar.getInstance();
        reinsertionDate.setTimeInMillis(latestReinsertionDate);
        return reinsertionDate;
    }

    // Schedule notifications for the ring cycle events
    private void scheduleRingCycleNotifications(SharedViewModel viewModel, Calendar startDate, Calendar removalDate,
                                                Calendar reinsertionDate, int cycleLength) {
        int hour = startDate.get(Calendar.HOUR_OF_DAY);
        int minute = startDate.get(Calendar.MINUTE);
        long cycleStartMillis = startDate.getTimeInMillis();

        // Cancel previous notifications for this cycle to prevent duplicates
        cancelNotificationsForCycle(cycleStartMillis);

        // ---- Cycle Progress Notifications ----
        Calendar twoWeeksRemaining = (Calendar) removalDate.clone();
        twoWeeksRemaining.add(Calendar.DAY_OF_MONTH, -14);
        twoWeeksRemaining.set(Calendar.HOUR_OF_DAY, hour);
        twoWeeksRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 14) {
            scheduleNotification(twoWeeksRemaining,
                    getString(R.string.notif_cycle_duration_title),
                    getString(R.string.notif_two_weeks_remaining),
                    buildRequestCode(cycleStartMillis, NOTIFY_TWO_WEEKS));
        }

        Calendar oneWeekRemaining = (Calendar) removalDate.clone();
        oneWeekRemaining.add(Calendar.DAY_OF_MONTH, -7);
        oneWeekRemaining.set(Calendar.HOUR_OF_DAY, hour);
        oneWeekRemaining.set(Calendar.MINUTE, minute);
        if (cycleLength >= 7) {
            scheduleNotification(oneWeekRemaining,
                    getString(R.string.notif_cycle_duration_title),
                    getString(R.string.notif_one_week_remaining),
                    buildRequestCode(cycleStartMillis, NOTIFY_ONE_WEEK));
        }

        // ---- Ring Removal Notifications ----
        int removalReminderHours = viewModel.getRepository().getRemovalReminderHours();
        if (removalReminderHours > 0) {
            Calendar removalReminder = (Calendar) removalDate.clone();
            removalReminder.add(Calendar.HOUR_OF_DAY, -removalReminderHours);
            scheduleNotification(removalReminder,
                    getString(R.string.notif_remove_title),
                    getString(R.string.notif_remove_in_hours, removalReminderHours),
                    buildRequestCode(cycleStartMillis, NOTIFY_REMOVAL_REMINDER));
        }

        Calendar removalExact = (Calendar) removalDate.clone();
        removalExact.set(Calendar.HOUR_OF_DAY, hour);
        removalExact.set(Calendar.MINUTE, minute);
        @SuppressLint("DefaultLocale") String removalTimeText = String.format("%02d:%02d", hour, minute);
        scheduleNotification(removalExact,
                getString(R.string.notif_remove_title),
                getString(R.string.notif_remove_now, removalTimeText),
                buildRequestCode(cycleStartMillis, NOTIFY_REMOVAL_EXACT));

        // ---- Ring Insertion Notifications ----
        int insertionReminderHours = viewModel.getRepository().getInsertionReminderHours();
        if (insertionReminderHours > 0) {
            Calendar reinsertionReminder = (Calendar) reinsertionDate.clone();
            reinsertionReminder.add(Calendar.HOUR_OF_DAY, -insertionReminderHours);
            scheduleNotification(reinsertionReminder,
                    getString(R.string.notif_insert_title),
                    getString(R.string.notif_insert_in_hours, insertionReminderHours),
                    buildRequestCode(cycleStartMillis, NOTIFY_INSERTION_REMINDER));
        }

        Calendar reinsertionExact = (Calendar) reinsertionDate.clone();
        reinsertionExact.set(Calendar.HOUR_OF_DAY, hour);
        reinsertionExact.set(Calendar.MINUTE, minute);
        @SuppressLint("DefaultLocale") String reinsertionTimeText = String.format("%02d:%02d", hour, minute);
        scheduleNotification(reinsertionExact,
                getString(R.string.notif_insert_title),
                getString(R.string.notif_insert_now, reinsertionTimeText),
                buildRequestCode(cycleStartMillis, NOTIFY_INSERTION_EXACT));
    }

    // Schedule a single notification with a unique ID
    private void scheduleNotification(Calendar calendar, String title, String message, int requestCode) {
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            return;
        }
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule exact alarm with AlarmManager
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    // Cancel all notifications for a specific cycle to avoid duplicates
    private void cancelNotificationsForCycle(long cycleStartMillis) {
        Intent intent = new Intent(requireContext(), NotificationReceiver.class);
        int[] types = new int[]{
                NOTIFY_TWO_WEEKS,
                NOTIFY_ONE_WEEK,
                NOTIFY_REMOVAL_REMINDER,
                NOTIFY_REMOVAL_EXACT,
                NOTIFY_INSERTION_REMINDER,
                NOTIFY_INSERTION_EXACT
        };
        for (int type : types) {
            int requestCode = buildRequestCode(cycleStartMillis, type);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    private int buildRequestCode(long cycleStartMillis, int typeId) {
        long hash = cycleStartMillis ^ (cycleStartMillis >>> 32);
        int base = (int) (hash & 0x7fffffff);
        int code = base + (typeId + 1) * 1000;
        return code < 0 ? base : code;
    }

    private boolean canScheduleExactAlarms(AlarmManager alarmManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        if (alarmManager.canScheduleExactAlarms()) {
            return true;
        }
        if (!viewModel.getRepository().wasExactAlarmPrompted()) {
            viewModel.getRepository().setExactAlarmPrompted(true);
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.exact_alarm_title)
                    .setMessage(R.string.exact_alarm_message)
                    .setPositiveButton(R.string.exact_alarm_open_settings, (d, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show();
            applyDialogButtonColors(dialog);
        }
        return false;
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

    private void showDelayDialog(SharedViewModel viewModel) {
        NumberPicker picker = new NumberPicker(requireContext());
        picker.setMinValue(0);
        picker.setMaxValue(21);
        int currentDelay = viewModel.getRepository()
                .getCycleDelayDays(Objects.requireNonNull(viewModel.getStartDate().getValue()).getTimeInMillis());
        if (currentDelay <= 0) {
            picker.setValue(7);
        } else {
            picker.setValue(Math.min(21, currentDelay));
        }

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, padding);
        layout.addView(picker);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_delay_title)
                .setMessage(R.string.home_delay_message)
                .setView(layout)
                .setPositiveButton(R.string.home_delay_confirm, (dialog, which) -> {
                    Calendar baseStart = viewModel.getStartDate().getValue();
                    Integer cycleLength = viewModel.getCycleLength().getValue();
                    if (baseStart == null || cycleLength == null) {
                        return;
                    }

                    Calendar now = Calendar.getInstance();
                    Calendar currentStart = (Calendar) baseStart.clone();
                    currentStart.set(Calendar.SECOND, 0);
                    currentStart.set(Calendar.MILLISECOND, 0);
                    int delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                    int ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
                    Calendar removalDate = (Calendar) currentStart.clone();
                    removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                    Calendar reinsertionDate = (Calendar) removalDate.clone();
                    reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

                    while (now.after(reinsertionDate)) {
                        currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
                        delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                        ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
                        removalDate = (Calendar) currentStart.clone();
                        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                        reinsertionDate = (Calendar) removalDate.clone();
                        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
                    }

                    long cycleStartMillis = currentStart.getTimeInMillis();
                    viewModel.getRepository().pruneCycleHistoryFrom(cycleStartMillis);
                    cancelNotificationsForCycle(cycleStartMillis);

                    viewModel.getRepository().setCycleDelayDays(cycleStartMillis, picker.getValue());
                    viewModel.getRepository().clearNotificationScheduledForCycle(cycleStartMillis);

                    int newDelayDays = picker.getValue();
                    Calendar newRemoval = (Calendar) currentStart.clone();
                    newRemoval.add(Calendar.DAY_OF_MONTH, cycleLength + newDelayDays);
                    Calendar newReinsertion = (Calendar) newRemoval.clone();
                    newReinsertion.add(Calendar.DAY_OF_MONTH, ringFreeDays);
                    if (now.before(newReinsertion)) {
                        scheduleRingCycleNotifications(
                                viewModel,
                                (Calendar) currentStart.clone(),
                                (Calendar) newRemoval.clone(),
                                (Calendar) newReinsertion.clone(),
                                cycleLength
                        );
                        viewModel.getRepository().setNotificationScheduledForCycle(cycleStartMillis);
                    viewModel.getRepository().setNotificationSettingsHashForCycle(
                            cycleStartMillis, viewModel.getRepository().getNotificationSettingsHash());
                    }

                    viewModel.setStartDate((Calendar) baseStart.clone());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private void showSkipRingFreeWeekDialog(SharedViewModel viewModel) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_skip_ring_free_title)
                .setMessage(R.string.home_skip_ring_free_message)
                .setPositiveButton(R.string.home_skip_ring_free_confirm, (dialog, which) -> {
                    Calendar baseStart = viewModel.getStartDate().getValue();
                    Integer cycleLength = viewModel.getCycleLength().getValue();
                    if (baseStart == null || cycleLength == null) {
                        return;
                    }

                    Calendar now = Calendar.getInstance();
                    Calendar currentStart = (Calendar) baseStart.clone();
                    currentStart.set(Calendar.SECOND, 0);
                    currentStart.set(Calendar.MILLISECOND, 0);
                    int delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                    int ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
                    Calendar removalDate = (Calendar) currentStart.clone();
                    removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                    Calendar reinsertionDate = (Calendar) removalDate.clone();
                    reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);

                    while (now.after(reinsertionDate)) {
                        currentStart.add(Calendar.DAY_OF_MONTH, cycleLength + ringFreeDays + delayDays);
                        delayDays = viewModel.getRepository().getCycleDelayDays(currentStart.getTimeInMillis());
                        ringFreeDays = viewModel.getRepository().getRingFreeDaysForCycle(currentStart.getTimeInMillis());
                        removalDate = (Calendar) currentStart.clone();
                        removalDate.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                        reinsertionDate = (Calendar) removalDate.clone();
                        reinsertionDate.add(Calendar.DAY_OF_MONTH, ringFreeDays);
                    }

                    long cycleStartMillis = currentStart.getTimeInMillis();
                    viewModel.getRepository().pruneCycleHistoryFrom(cycleStartMillis);
                    cancelNotificationsForCycle(cycleStartMillis);

                    viewModel.getRepository().setSkipRingFreeWeek(cycleStartMillis, true);
                    viewModel.getRepository().clearNotificationScheduledForCycle(cycleStartMillis);

                    Calendar newRemoval = (Calendar) currentStart.clone();
                    newRemoval.add(Calendar.DAY_OF_MONTH, cycleLength + delayDays);
                    Calendar newReinsertion = (Calendar) newRemoval.clone();
                    newReinsertion.add(Calendar.DAY_OF_MONTH, 0);

                    if (now.before(newReinsertion)) {
                        scheduleRingCycleNotifications(
                                viewModel,
                                (Calendar) currentStart.clone(),
                                (Calendar) newRemoval.clone(),
                                (Calendar) newReinsertion.clone(),
                                cycleLength
                        );
                        viewModel.getRepository().setNotificationScheduledForCycle(cycleStartMillis);
                        viewModel.getRepository().setNotificationSettingsHashForCycle(
                                cycleStartMillis, viewModel.getRepository().getNotificationSettingsHash());
                    }

                    viewModel.setStartDate((Calendar) baseStart.clone());
                    WidgetUpdater.updateAllWidgets(requireContext());
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private static Calendar startOfDay(Calendar source) {
        Calendar day = (Calendar) source.clone();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day;
    }

    private static int daysBetweenDays(Calendar from, Calendar to) {
        Calendar fromDay = startOfDay(from);
        Calendar toDay = startOfDay(to);
        if (!fromDay.before(toDay)) {
            return 0;
        }
        int days = 0;
        while (fromDay.before(toDay)) {
            fromDay.add(Calendar.DAY_OF_MONTH, 1);
            days++;
        }
        return days;
    }

    private void applyHomeCircleStyle(HomeCircleView progress, int style, int color) {
        if (progress == null) {
            return;
        }
        progress.setStyle(style);
        progress.setIndicatorColor(color);
        if (style == HomeCircleView.STYLE_PULSE_LIGHT
                || style == HomeCircleView.STYLE_PULSE_MEDIUM
                || style == HomeCircleView.STYLE_PULSE_STRONG) {
            startPulse(progress);
        } else {
            stopPulse();
        }
    }

    private void startPulse(HomeCircleView progress) {
        progress.removeCallbacks(pulseRunnable);
        progress.post(pulseRunnable);
    }

    private void stopPulse() {
        if (getView() != null) {
            getView().removeCallbacks(pulseRunnable);
        }
    }

    private final Runnable pulseRunnable = new Runnable() {
        @Override
        public void run() {
            if (currentCircleStyle != HomeCircleView.STYLE_PULSE_LIGHT
                    && currentCircleStyle != HomeCircleView.STYLE_PULSE_MEDIUM
                    && currentCircleStyle != HomeCircleView.STYLE_PULSE_STRONG) {
                return;
            }
            pulsePhase += pulseUp ? 0.06f : -0.06f;
            if (pulsePhase >= 1f) {
                pulsePhase = 1f;
                pulseUp = false;
            } else if (pulsePhase <= 0f) {
                pulsePhase = 0f;
                pulseUp = true;
            }
            HomeCircleView circle = getView() != null ? getView().findViewById(R.id.circularProgress) : null;
            if (circle != null) {
                circle.setPulsePhase(pulsePhase);
                circle.postDelayed(this, 40);
            }
        }
    };

}
