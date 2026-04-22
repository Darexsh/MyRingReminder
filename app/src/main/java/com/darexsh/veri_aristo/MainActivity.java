package com.darexsh.veri_aristo;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jakewharton.threetenabp.AndroidThreeTen;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import com.google.android.material.button.MaterialButton;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import android.util.Log;
import java.util.concurrent.Executor;

// MainActivity serves as the entry point for the app, managing fragments and navigation
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_VERSION = "last_version";
    private static final String KEY_TOUR_SHOWN = "tour_shown";
    private static final String KEY_WELCOME_SHOWN = "welcome_shown";
    private static final String KEY_APP_LOCK_LAST_BACKGROUND_AT = "app_lock_last_background_at";
    private static final long BACK_PRESS_WINDOW_MS = 2000;
    private static final String TAG = "MainActivity";
    private FragmentManager fragmentManager;
    private ImageButton btnNotes;
    private ImageView globalBackgroundImage;
    private View globalBackgroundDimOverlay;
    private BottomNavigationView bottomNavigationView;
    private View appLockOverlay;
    private MaterialButton btnUnlockApp;
    private long lastBackPressedAt = 0L;
    private SharedViewModel viewModel;
    private int navigationAnimationStyle = SettingsRepository.DEFAULT_NAVIGATION_ANIMATION_STYLE;
    private SharedPreferences prefs;
    private GuidedTourOverlay tourOverlay;
    private List<TourStep> tourSteps;
    private int tourIndex = 0;
    private boolean tourCompleted = false;
    private boolean appUnlockInProgress = false;
    private boolean appUnlockedThisSession = false;
    private boolean appMovedToBackground = false;
    private long appBackgroundedAtMillis = 0L;
    private boolean openUpdateBackupOnNextSettings = false;
    private boolean startupUpdateCheckConsumed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastVersion = prefs.getInt(KEY_LAST_VERSION, -1);

        try {
            int currentVersion = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;

            if (currentVersion > lastVersion) {
                clearAllScheduledNotifications();
                new SettingsRepository(this).clearNotificationFlags();
                prefs.edit().putInt(KEY_LAST_VERSION, currentVersion).apply();
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to get package info", e);
        }

        fragmentManager = getSupportFragmentManager();
        btnNotes = findViewById(R.id.btn_notes);
        globalBackgroundImage = findViewById(R.id.global_background_image);
        globalBackgroundDimOverlay = findViewById(R.id.global_background_dim_overlay);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        appLockOverlay = findViewById(R.id.app_lock_overlay);
        btnUnlockApp = findViewById(R.id.btn_unlock_app);
        SharedViewModelFactory factory = new SharedViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(SharedViewModel.class);
        CalendarRenderCache.warmAsync(viewModel.getRepository());
        viewModel.getButtonColor().observe(this, color -> {
            if (color != null) {
                applyBottomNavColors(color);
                if (btnUnlockApp != null) {
                    ButtonColorHelper.applyPrimaryColor(btnUnlockApp, color);
                }
                if (tourOverlay != null) {
                    tourOverlay.setButtonColor(color);
                }
            }
        });
        viewModel.getNavigationAnimationStyle().observe(this, style -> {
            if (style != null) {
                navigationAnimationStyle = style;
            }
        });
        viewModel.getBackgroundImageUri().observe(this, uri -> refreshGlobalBackgroundImage());
        viewModel.getBackgroundAllScreensEnabled().observe(this, enabled -> refreshGlobalBackgroundImage());
        viewModel.getBackgroundDimPercent().observe(this, percent -> refreshGlobalBackgroundDim());
        viewModel.getBackgroundBlurOthersPercent().observe(this, percent -> refreshGlobalBackgroundBlur());

        boolean lockEnabledAtLaunch = viewModel.getRepository().isAppLockEnabled();
        if (!lockEnabledAtLaunch) {
            appUnlockedThisSession = true;
        } else {
            int timeoutMs = viewModel.getRepository().getAppLockTimeoutMs();
            long lastBackgroundAt = prefs.getLong(KEY_APP_LOCK_LAST_BACKGROUND_AT, 0L);
            if (timeoutMs > 0 && lastBackgroundAt > 0L) {
                long elapsed = Math.max(0L, System.currentTimeMillis() - lastBackgroundAt);
                appUnlockedThisSession = elapsed < timeoutMs;
            } else {
                appUnlockedThisSession = false;
            }
        }
        setAppLockOverlayVisible(lockEnabledAtLaunch && !appUnlockedThisSession);
        if (btnUnlockApp != null) {
            btnUnlockApp.setOnClickListener(v -> {
                if (!appUnlockInProgress) {
                    showAppUnlockPrompt();
                }
            });
        }

        // Load default Home-Fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            btnNotes.setVisibility(View.VISIBLE);
        }

        btnNotes.setOnClickListener(v -> {
            loadFragment(new NotesFragment(), true);
            btnNotes.setVisibility(View.GONE);
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
                btnNotes.setVisibility(View.VISIBLE);
            } else if (id == R.id.nav_calendar) {
                selectedFragment = new CalendarFragment();
                btnNotes.setVisibility(View.GONE);
            } else if (id == R.id.nav_cycles) {
                selectedFragment = new CyclesFragment();
                btnNotes.setVisibility(View.GONE);
            } else if (id == R.id.nav_settings) {
                SettingsFragment settingsFragment = new SettingsFragment();
                if (openUpdateBackupOnNextSettings) {
                    Bundle args = new Bundle();
                    args.putBoolean(SettingsFragment.ARG_OPEN_UPDATE_BACKUP_DIALOG, true);
                    settingsFragment.setArguments(args);
                    openUpdateBackupOnNextSettings = false;
                }
                selectedFragment = settingsFragment;
                btnNotes.setVisibility(View.GONE);
            } else {
                btnNotes.setVisibility(View.GONE);
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, true);
                return true;
            }

            return false;
        });

        // Listen for back stack changes to manage visibility of the notes button
        fragmentManager.addOnBackStackChangedListener(this::updateNotesButtonVisibility);
        updateNotesButtonVisibility();

        // Handle back button presses to navigate through fragments
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (!(currentFragment instanceof HomeFragment)) {
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    loadFragment(new HomeFragment(), false);
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    btnNotes.setVisibility(View.VISIBLE);
                    lastBackPressedAt = 0L;
                    return;
                }

                long now = System.currentTimeMillis();
                if (now - lastBackPressedAt < BACK_PRESS_WINDOW_MS) {
                    finish();
                } else {
                    lastBackPressedAt = now;
                    Toast.makeText(MainActivity.this, R.string.main_double_back_exit, Toast.LENGTH_SHORT).show();
                }
            }
        });

        handleOpenHomeIntent(getIntent());

        // Create notification channel and request permission
        createNotificationChannel();
        requestNotificationPermission();
        ReminderScheduler.scheduleCurrentCycle(this);

        maybeStartWelcomeFlow();
    }

    private void refreshGlobalBackgroundImage() {
        if (globalBackgroundImage == null || viewModel == null) {
            return;
        }
        boolean enabled = Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue());
        String uriStr = viewModel.getBackgroundImageUri().getValue();
        if (!enabled) {
            globalBackgroundImage.setImageDrawable(null);
            globalBackgroundImage.setVisibility(View.GONE);
            refreshGlobalBackgroundBlur();
            return;
        }
        if (uriStr == null || uriStr.trim().isEmpty()) {
            globalBackgroundImage.setImageResource(R.drawable.default_bg);
            globalBackgroundImage.setVisibility(View.VISIBLE);
            refreshGlobalBackgroundBlur();
            return;
        }

        try {
            Uri uri = Uri.parse(uriStr);
            String scheme = uri.getScheme();
            Drawable drawable;
            if ("file".equalsIgnoreCase(scheme)) {
                File file = new File(Objects.requireNonNull(uri.getPath()));
                if (!file.exists()) {
                    viewModel.setBackgroundImageUri(null);
                    globalBackgroundImage.setImageResource(R.drawable.default_bg);
                    globalBackgroundImage.setVisibility(View.VISIBLE);
                    refreshGlobalBackgroundBlur();
                    return;
                }
                try (InputStream inputStream = new FileInputStream(file)) {
                    drawable = Drawable.createFromStream(inputStream, uri.toString());
                }
            } else {
                boolean hasPermission = false;
                for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
                    if (permission.getUri().equals(uri) && permission.isReadPermission()) {
                        hasPermission = true;
                        break;
                    }
                }
                if (!hasPermission) {
                    viewModel.setBackgroundImageUri(null);
                    globalBackgroundImage.setImageResource(R.drawable.default_bg);
                    globalBackgroundImage.setVisibility(View.VISIBLE);
                    refreshGlobalBackgroundBlur();
                    return;
                }
                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    drawable = Drawable.createFromStream(inputStream, uri.toString());
                }
            }
            if (drawable == null) {
                globalBackgroundImage.setImageResource(R.drawable.default_bg);
                globalBackgroundImage.setVisibility(View.VISIBLE);
                refreshGlobalBackgroundBlur();
                return;
            }
            globalBackgroundImage.setImageDrawable(drawable);
            globalBackgroundImage.setVisibility(View.VISIBLE);
            refreshGlobalBackgroundBlur();
        } catch (Exception e) {
            Log.e(TAG, "Failed to load global background image", e);
            viewModel.setBackgroundImageUri(null);
            globalBackgroundImage.setImageResource(R.drawable.default_bg);
            globalBackgroundImage.setVisibility(View.VISIBLE);
            refreshGlobalBackgroundBlur();
        }
    }

    private void refreshGlobalBackgroundDim() {
        if (globalBackgroundDimOverlay == null || viewModel == null) {
            return;
        }
        boolean enabled = Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue());
        Integer dimPercent = viewModel.getBackgroundDimPercent().getValue();
        int percent = dimPercent != null ? Math.max(0, Math.min(100, dimPercent)) : 0;
        if (!enabled || percent == 0) {
            globalBackgroundDimOverlay.setVisibility(View.GONE);
            globalBackgroundDimOverlay.setAlpha(0f);
            return;
        }
        globalBackgroundDimOverlay.setAlpha(percent / 100f);
        globalBackgroundDimOverlay.setVisibility(View.VISIBLE);
    }

    private void refreshGlobalBackgroundBlur() {
        if (globalBackgroundImage == null || viewModel == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return;
        }
        boolean enabled = Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue());
        if (!enabled || globalBackgroundImage.getVisibility() != View.VISIBLE) {
            globalBackgroundImage.setRenderEffect(null);
            return;
        }
        Integer blurPercent = viewModel.getBackgroundBlurOthersPercent().getValue();
        int percent = blurPercent != null ? Math.max(0, Math.min(100, blurPercent)) : 50;
        float radiusPx = percentToBlurRadiusPx(percent);
        if (radiusPx <= 0f) {
            globalBackgroundImage.setRenderEffect(null);
            return;
        }
        globalBackgroundImage.setRenderEffect(
                RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP));
    }

    private float percentToBlurRadiusPx(int percent) {
        float density = getResources().getDisplayMetrics().density;
        return (percent / 100f) * 28f * density;
    }

    private void applyBottomNavColors(int selectedColor) {
        int unselectedColor = ContextCompat.getColor(this, android.R.color.darker_gray);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{selectedColor, unselectedColor};
        ColorStateList tintList = new ColorStateList(states, colors);
        bottomNavigationView.setItemIconTintList(tintList);
        bottomNavigationView.setItemTextColor(tintList);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleOpenHomeIntent(intent);
    }

    private void handleOpenHomeIntent(Intent intent) {
        if (intent == null || !intent.getBooleanExtra("open_home", false)) {
            return;
        }
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        btnNotes.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotesButtonVisibility();
        maybeRequestAppUnlock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            appMovedToBackground = true;
            appBackgroundedAtMillis = System.currentTimeMillis();
            if (prefs != null) {
                prefs.edit().putLong(KEY_APP_LOCK_LAST_BACKGROUND_AT, appBackgroundedAtMillis).apply();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void updateNotesButtonVisibility() {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment instanceof HomeFragment) {
            btnNotes.setVisibility(View.VISIBLE);
        } else {
            btnNotes.setVisibility(View.GONE);
        }
    }

    public boolean consumeStartupUpdateCheckRequest() {
        if (startupUpdateCheckConsumed) {
            return false;
        }
        startupUpdateCheckConsumed = true;
        return true;
    }

    public void openUpdateBackupFlowFromHome() {
        openUpdateBackupOnNextSettings = true;
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        }
    }

    private void maybeRequestAppUnlock() {
        if (viewModel == null) {
            return;
        }
        SettingsRepository repository = viewModel.getRepository();
        boolean appLockEnabled = repository.isAppLockEnabled();
        if (!appLockEnabled) {
            appUnlockInProgress = false;
            appUnlockedThisSession = true;
            appMovedToBackground = false;
            appBackgroundedAtMillis = 0L;
            if (prefs != null) {
                prefs.edit().remove(KEY_APP_LOCK_LAST_BACKGROUND_AT).apply();
            }
            setAppLockOverlayVisible(false);
            return;
        }
        if (appUnlockInProgress) {
            return;
        }

        if (appUnlockedThisSession && appMovedToBackground) {
            int timeoutMs = repository.getAppLockTimeoutMs();
            long elapsed = appBackgroundedAtMillis > 0L
                    ? Math.max(0L, System.currentTimeMillis() - appBackgroundedAtMillis)
                    : Long.MAX_VALUE;
            appMovedToBackground = false;
            appBackgroundedAtMillis = 0L;
            if (prefs != null) {
                prefs.edit().remove(KEY_APP_LOCK_LAST_BACKGROUND_AT).apply();
            }
            if (elapsed < timeoutMs) {
                setAppLockOverlayVisible(false);
                return;
            }
            appUnlockedThisSession = false;
        }

        if (appUnlockedThisSession) {
            setAppLockOverlayVisible(false);
            return;
        }
        setAppLockOverlayVisible(true);
        showAppUnlockPrompt();
    }

    private void showAppUnlockPrompt() {
        BiometricManager biometricManager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
                | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int canAuthenticate = biometricManager.canAuthenticate(authenticators);
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.settings_app_lock_requires_secure_lock, Toast.LENGTH_LONG).show();
            appUnlockInProgress = false;
            setAppLockOverlayVisible(true);
            return;
        }

        appUnlockInProgress = true;
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(
                this,
                executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        appUnlockInProgress = false;
                        appUnlockedThisSession = true;
                        appMovedToBackground = false;
                        appBackgroundedAtMillis = 0L;
                        if (prefs != null) {
                            prefs.edit().remove(KEY_APP_LOCK_LAST_BACKGROUND_AT).apply();
                        }
                        setAppLockOverlayVisible(false);
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        appUnlockInProgress = false;
                        Toast.makeText(MainActivity.this, R.string.app_lock_auth_required, Toast.LENGTH_SHORT).show();
                        setAppLockOverlayVisible(true);
                    }
                }
        );

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_auth_title))
                .setSubtitle(getString(R.string.app_lock_auth_subtitle))
                .setAllowedAuthenticators(authenticators)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void setAppLockOverlayVisible(boolean visible) {
        if (appLockOverlay == null) {
            return;
        }
        appLockOverlay.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (bottomNavigationView != null) {
            bottomNavigationView.setEnabled(!visible);
        }
        if (btnNotes != null) {
            btnNotes.setEnabled(!visible);
        }
    }

    private void loadFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        applyFragmentAnimation(transaction, fragment);
        transaction.replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.setReorderingAllowed(true);
        transaction.commit();
    }

    private void applyFragmentAnimation(FragmentTransaction transaction, Fragment targetFragment) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (currentFragment == null) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_NONE) {
            transaction.setCustomAnimations(0, 0, 0, 0);
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_FADE) {
            transaction.setCustomAnimations(
                    android.R.anim.fade_in,
                    0,
                    android.R.anim.fade_in,
                    0
            );
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_ZOOM) {
            transaction.setCustomAnimations(
                    R.anim.zoom_in_fade_in,
                    0,
                    R.anim.zoom_in_fade_in,
                    0
            );
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_SLIDE_UP) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_up,
                    0,
                    R.anim.slide_in_down,
                    0
            );
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_ROTATE) {
            transaction.setCustomAnimations(
                    R.anim.rotate_in_fade_in,
                    0,
                    R.anim.rotate_in_fade_in,
                    0
            );
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_POP) {
            transaction.setCustomAnimations(
                    R.anim.pop_in,
                    0,
                    R.anim.pop_in,
                    0
            );
            return;
        }

        int currentOrder = getFragmentOrder(currentFragment);
        int targetOrder = getFragmentOrder(targetFragment);

        if (targetOrder == currentOrder) {
            transaction.setCustomAnimations(android.R.anim.fade_in, 0,
                    android.R.anim.fade_in, 0);
            return;
        }

        if (targetOrder > currentOrder) {
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,
                    0,
                    R.anim.slide_in_left,
                    0
            );
        } else {
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,
                    0,
                    R.anim.slide_in_right,
                    0
            );
        }
    }

    private int getFragmentOrder(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return 0;
        }
        if (fragment instanceof CalendarFragment) {
            return 1;
        }
        if (fragment instanceof CyclesFragment) {
            return 2;
        }
        if (fragment instanceof SettingsFragment) {
            return 3;
        }
        if (fragment instanceof NotesFragment) {
            return 4;
        }
        return 99;
    }

    private void maybeStartWelcomeFlow() {
        if (prefs.getBoolean(KEY_WELCOME_SHOWN, false)) {
            maybeStartGuidedTour();
            return;
        }

        View content = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        MaterialButton startButton = content.findViewById(R.id.welcome_start);
        MaterialButton skipButton = content.findViewById(R.id.welcome_skip);
        Integer buttonColor = viewModel.getButtonColor().getValue();
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(startButton, buttonColor);
            ButtonColorHelper.applyPrimaryColor(skipButton, buttonColor);
            startButton.setTextColor(Color.WHITE);
            skipButton.setTextColor(Color.WHITE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(false)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        startButton.setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply();
            dialog.dismiss();
            maybeStartGuidedTour();
        });

        skipButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(KEY_WELCOME_SHOWN, true)
                    .putBoolean(KEY_TOUR_SHOWN, true)
                    .apply();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void maybeStartGuidedTour() {
        if (prefs.getBoolean(KEY_TOUR_SHOWN, false)) {
            return;
        }
        getWindow().getDecorView().post(this::startGuidedTourWhenReady);
    }


    private void startGuidedTourWhenReady() {

        if (tourSteps == null) {
            tourSteps = buildTourSteps();
        }

        if (tourOverlay == null) {
            tourOverlay = new GuidedTourOverlay(this);
            Integer buttonColor = viewModel.getButtonColor().getValue();
            if (buttonColor != null) {
                tourOverlay.setButtonColor(buttonColor);
            }
            tourOverlay.setOnNextListener(() -> {
                if (tourSteps != null && tourIndex < tourSteps.size()) {
                    TourStep currentStep = tourSteps.get(tourIndex);
                    if (currentStep.openNotesAfter) {
                        setNotesOpen(true);
                    }
                    if (currentStep.expandAdvancedAfter) {
                        setAdvancedExpanded(true);
                    }
                    if (currentStep.collapseAdvancedAfter) {
                        setAdvancedExpanded(false);
                    }
                }
                if (tourSteps != null && tourIndex >= tourSteps.size() - 1) {
                    tourCompleted = true;
                }
                showGuidedTourStep(tourIndex + 1, 0);
            });
            tourOverlay.setOnSkipListener(() -> {
                tourCompleted = false;
                finishGuidedTour();
            });
            tourOverlay.setOnFinishListener(() ->
                    prefs.edit().putBoolean(KEY_TOUR_SHOWN, true).apply()
            );
            ViewGroup root = findViewById(android.R.id.content);
            root.addView(tourOverlay);
        }

        showGuidedTourStep(0, 0);
    }

    private void showGuidedTourStep(int index, int attempt) {
        if (tourOverlay == null) {
            return;
        }
        if (tourSteps == null || index >= tourSteps.size()) {
            finishGuidedTour();
            return;
        }
        if (attempt > 15) {
            finishGuidedTour();
            return;
        }

        TourStep step = tourSteps.get(index);
        boolean advancedChanged = false;
        if (step.collapseAdvancedBefore) {
            advancedChanged = setAdvancedExpanded(false);
        }
        if (step.requireAdvancedExpanded) {
            advancedChanged = setAdvancedExpanded(true) || advancedChanged;
        }
        if (advancedChanged) {
            getWindow().getDecorView().postDelayed(() ->
                    showGuidedTourStep(index, attempt + 1), 120);
            return;
        }
        boolean notesChanged = false;
        if (step.closeNotesBefore) {
            notesChanged = setNotesOpen(false);
        }
        if (step.requireNotesOpen) {
            notesChanged = setNotesOpen(true) || notesChanged;
        }
        if (notesChanged) {
            getWindow().getDecorView().postDelayed(() ->
                    showGuidedTourStep(index, attempt + 1), 150);
            return;
        }
        if (step.navItemId != 0 && bottomNavigationView.getSelectedItemId() != step.navItemId) {
            bottomNavigationView.setSelectedItemId(step.navItemId);
        }

        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        View fragmentView = currentFragment != null ? currentFragment.getView() : null;
        View target = step.inActivityView ? findViewById(step.targetViewId)
                : (fragmentView != null ? fragmentView.findViewById(step.targetViewId) : null);

        Log.d("TourDebug", "Index: " + index + " Attempt: " + attempt + " TargetID: " + step.targetViewId);
        if (target == null) {
            getWindow().getDecorView().postDelayed(() -> showGuidedTourStep(index, attempt + 1), 120);
            return;
        }

        if (step.scrollViewId != 0 && fragmentView != null) {
            ScrollView scrollView = fragmentView.findViewById(step.scrollViewId);
            if (scrollView != null) {
                if (!isTargetVisible(scrollView, target)) {
                    scrollToView(scrollView, target);
                    getWindow().getDecorView().postDelayed(() ->
                            showGuidedTourStep(index, attempt + 1), 180);
                    return;
                }
            }
        }

        Rect visibleRect = new Rect();
        if (!target.getGlobalVisibleRect(visibleRect)) {
            getWindow().getDecorView().postDelayed(() -> showGuidedTourStep(index, attempt + 1), 120);
            return;
        }

        tourIndex = index;
        tourOverlay.setStep(step.titleRes, step.bodyRes, index == tourSteps.size() - 1, target);
    }

    private void finishGuidedTour() {
        if (tourOverlay != null) {
            tourOverlay.finish();
            tourOverlay = null;
        }
        if (tourCompleted) {
            tourCompleted = false;
            getWindow().getDecorView().post(this::showTourCompletionDialog);
        }
    }

    private void showTourCompletionDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_tour_complete, null);
        MaterialButton doneButton = content.findViewById(R.id.tour_complete_done);
        Integer buttonColor = viewModel.getButtonColor().getValue();
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(doneButton, buttonColor);
            doneButton.setTextColor(Color.WHITE);
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        doneButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    public void restartWelcomeTour() {
        prefs.edit()
                .putBoolean(KEY_WELCOME_SHOWN, false)
                .putBoolean(KEY_TOUR_SHOWN, false)
                .apply();
        tourCompleted = false;
        if (tourOverlay != null) {
            finishGuidedTour();
        }
        maybeStartWelcomeFlow();
    }

    private void scrollToView(ScrollView scrollView, View target) {
        scrollView.post(() -> {
            Rect rect = new Rect();
            target.getDrawingRect(rect);
            scrollView.offsetDescendantRectToMyCoords(target, rect);
            int targetHeight = rect.height();
            int desiredTop = rect.top - (scrollView.getHeight() - targetHeight) / 2;
            int maxScroll = 0;
            View content = scrollView.getChildAt(0);
            if (content != null) {
                maxScroll = Math.max(0, content.getHeight() - scrollView.getHeight());
            }
            if (desiredTop < 0) {
                desiredTop = 0;
            } else if (desiredTop > maxScroll) {
                desiredTop = maxScroll;
            }
            scrollView.scrollTo(0, desiredTop);
        });
    }

    private boolean setAdvancedExpanded(boolean expanded) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        View fragmentView = currentFragment != null ? currentFragment.getView() : null;
        if (fragmentView == null) {
            return false;
        }
        View advancedContent = fragmentView.findViewById(R.id.advanced_content);
        View advancedToggle = fragmentView.findViewById(R.id.btn_advanced_toggle);
        if (advancedContent == null || advancedToggle == null) {
            return false;
        }
        boolean isVisible = advancedContent.getVisibility() == View.VISIBLE;
        if (expanded == isVisible) {
            return false;
        }
        advancedContent.setVisibility(expanded ? View.VISIBLE : View.GONE);
        advancedToggle.setRotation(expanded ? 180f : 0f);
        return true;
    }

    private boolean setNotesOpen(boolean open) {
        Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        boolean isNotes = currentFragment instanceof NotesFragment;
        if (open) {
            if (isNotes) {
                return false;
            }
            loadFragment(new NotesFragment(), true);
            btnNotes.setVisibility(View.GONE);
            return true;
        }
        if (!isNotes) {
            return false;
        }
        openHomeFragment();
        return true;
    }

    private void openHomeFragment() {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        btnNotes.setVisibility(View.VISIBLE);
    }

    private boolean isTargetVisible(ScrollView scrollView, View target) {
        Rect rect = new Rect();
        target.getDrawingRect(rect);
        scrollView.offsetDescendantRectToMyCoords(target, rect);
        int scrollTop = scrollView.getScrollY();
        int scrollBottom = scrollTop + scrollView.getHeight();
        return rect.top >= scrollTop && rect.bottom <= scrollBottom;
    }

    private List<TourStep> buildTourSteps() {
        List<TourStep> steps = new ArrayList<>();
        steps.add(new TourStep(R.id.nav_home, R.id.circularProgress,
                R.string.tour_title_progress, R.string.tour_body_progress, 0, false));
        steps.add(new TourStep(R.id.nav_home, R.id.btn_special_actions_toggle,
                R.string.tour_title_delay, R.string.tour_body_delay, 0, false));
        steps.add(new TourStep(R.id.nav_home, R.id.btn_notes,
                R.string.tour_title_notes, R.string.tour_body_notes, 0, true,
                false, false, false, false, false, true, false));
        steps.add(new TourStep(0, R.id.editText_notes,
                R.string.tour_title_notes_editor, R.string.tour_body_notes_editor, 0, false,
                false, false, false, false, true, false, false));
        steps.add(new TourStep(0, R.id.btn_save_notes,
                R.string.tour_title_notes_save, R.string.tour_body_notes_save, 0, false,
                false, false, false, false, true, false, false));
        steps.add(new TourStep(0, R.id.btn_clear_notes,
                R.string.tour_title_notes_delete, R.string.tour_body_notes_delete, 0, false,
                false, false, false, false, true, false, false));
        steps.add(new TourStep(0, R.id.btn_close_notes,
                R.string.tour_title_notes_close, R.string.tour_body_notes_close, 0, false,
                false, false, false, false, true, false, false));
        steps.add(new TourStep(R.id.nav_home, R.id.bottom_navigation,
                R.string.tour_title_navigation, R.string.tour_body_navigation, 0, true,
                false, false, false, false, false, false, true));

        steps.add(new TourStep(R.id.nav_calendar, R.id.calendarView,
                R.string.tour_title_calendar, R.string.tour_body_calendar, 0, false));
        steps.add(new TourStep(R.id.nav_calendar, R.id.calendar_legend_row,
                R.string.tour_title_calendar_legend, R.string.tour_body_calendar_legend, 0, false));

        steps.add(new TourStep(R.id.nav_cycles, R.id.tv_history_title,
                R.string.tour_title_cycles, R.string.tour_body_cycles, 0, false));
        steps.add(new TourStep(R.id.nav_cycles, R.id.btn_clear_history,
                R.string.tour_title_cycles_clear, R.string.tour_body_cycles_clear, 0, false));

        steps.add(new TourStep(R.id.nav_settings, R.id.btn_settings_info,
                R.string.tour_title_settings_info, R.string.tour_body_settings_info, 0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.settings_cycle_container,
                R.string.tour_title_settings_cycle, R.string.tour_body_settings_cycle,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_calendar_range,
                R.string.tour_title_settings_calendar, R.string.tour_body_settings_calendar,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_group,
                R.string.tour_title_settings_notifications, R.string.tour_body_settings_notifications,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_background,
                R.string.tour_title_settings_background, R.string.tour_body_settings_background,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_all_screens,
                R.string.tour_title_settings_background_all, R.string.tour_body_settings_background_all,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_dim,
                R.string.tour_title_settings_background_dim, R.string.tour_body_settings_background_dim,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_blur,
                R.string.tour_title_settings_background_blur, R.string.tour_body_settings_background_blur,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_button_color,
                R.string.tour_title_settings_button_color, R.string.tour_body_settings_button_color,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_circle_color,
                R.string.tour_title_settings_circle_color, R.string.tour_body_settings_circle_color,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_circle_style,
                R.string.tour_title_settings_circle_style, R.string.tour_body_settings_circle_style,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_navigation_animation,
                R.string.tour_title_settings_animation, R.string.tour_body_settings_animation,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_language,
                R.string.tour_title_settings_language, R.string.tour_body_settings_language,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.advanced_header,
                R.string.tour_title_settings_advanced, R.string.tour_body_settings_advanced,
                0, false, false, true, false, true, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_update_app,
                R.string.tour_title_settings_update, R.string.tour_body_settings_update,
                0, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_backup_manage,
                R.string.tour_title_settings_backup, R.string.tour_body_settings_backup,
                0, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_app_lock,
                R.string.tour_title_settings_app_lock, R.string.tour_body_settings_app_lock,
                0, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_reset_app,
                R.string.tour_title_settings_reset, R.string.tour_body_settings_reset,
                0, false, true, false, true, false, false, false, false));
        return steps;
    }

    private static class TourStep {
        private final int navItemId;
        private final int targetViewId;
        private final int titleRes;
        private final int bodyRes;
        private final int scrollViewId;
        private final boolean inActivityView;
        private final boolean requireAdvancedExpanded;
        private final boolean expandAdvancedAfter;
        private final boolean collapseAdvancedAfter;
        private final boolean collapseAdvancedBefore;
        private final boolean requireNotesOpen;
        private final boolean openNotesAfter;
        private final boolean closeNotesBefore;

        private TourStep(int navItemId, int targetViewId, int titleRes, int bodyRes,
                         int scrollViewId, boolean inActivityView) {
            this(navItemId, targetViewId, titleRes, bodyRes, scrollViewId, inActivityView,
                    false, false, false, false, false, false, false);
        }

        private TourStep(int navItemId, int targetViewId, int titleRes, int bodyRes,
                         int scrollViewId, boolean inActivityView, boolean requireAdvancedExpanded,
                         boolean expandAdvancedAfter, boolean collapseAdvancedAfter,
                         boolean collapseAdvancedBefore, boolean requireNotesOpen,
                         boolean openNotesAfter, boolean closeNotesBefore) {
            this.navItemId = navItemId;
            this.targetViewId = targetViewId;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
            this.scrollViewId = scrollViewId;
            this.inActivityView = inActivityView;
            this.requireAdvancedExpanded = requireAdvancedExpanded;
            this.expandAdvancedAfter = expandAdvancedAfter;
            this.collapseAdvancedAfter = collapseAdvancedAfter;
            this.collapseAdvancedBefore = collapseAdvancedBefore;
            this.requireNotesOpen = requireNotesOpen;
            this.openNotesAfter = openNotesAfter;
            this.closeNotesBefore = closeNotesBefore;
        }
    }

    // Create a notification channel for Android O and above
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "reminder_channel",
                getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.notifications_channel_description));
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    // Request notification permission for Android 13 and above
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void clearAllScheduledNotifications() {
        Intent intent = new Intent(this, NotificationReceiver.class);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        int daysRange = 365;
        for (int d = -daysRange; d < daysRange; d++) {
            for (int offset = 0; offset < 6; offset++) {
                long triggerTime = System.currentTimeMillis() + d * 24L * 60 * 60 * 1000;
                int requestCode = (int) ((triggerTime / 1000) % Integer.MAX_VALUE) + offset;

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
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
}
