package com.darexsh.myringreminder;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.RenderEffect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.Rect;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.jakewharton.threetenabp.AndroidThreeTen;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.ScrollView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import android.util.Log;
import java.util.concurrent.Executor;

// MainActivity serves as the entry point for the app, managing fragments and navigation
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    private static final String SHORTCUT_SCHEME = "myringreminder";
    private static final String SHORTCUT_HOST = "shortcut";
    private static final String SHORTCUT_CALENDAR = "calendar";
    private static final String SHORTCUT_CYCLES = "cycles";
    private static final String SHORTCUT_SETTINGS = "settings";
    private static final String SHORTCUT_STOCK = "stock";
    private static final String SHORTCUT_NOTES = "notes";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LAST_VERSION = "last_version";
    private static final String KEY_TOUR_SHOWN = "tour_shown";
    private static final String KEY_WELCOME_SHOWN = "welcome_shown";
    private static final String KEY_INITIAL_CYCLE_SETUP_COMPLETED = "initial_cycle_setup_completed";
    private static final String KEY_APP_LOCK_LAST_BACKGROUND_AT = "app_lock_last_background_at";
    private static final long BACK_PRESS_WINDOW_MS = 2000;
    private static final long NAVIGATION_ANIMATION_DURATION_MS = 200L;
    private static final String TAG = "MainActivity";
    private static final String NOTIFICATION_TAG = "ReminderChannel";
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
    private boolean openStockTrackerOnNextSettings = false;
    private boolean startupUpdateCheckConsumed = false;
    private View initialCycleSetupContent;
    private View firstRunWizardOverlay;
    private ViewGroup firstRunWizardHost;
    private boolean firstRunWizardWelcomeScreen;
    private int originalStatusBarColor;
    private int originalNavigationBarColor;
    private boolean originalSystemBarColorsSaved;
    private Calendar initialCycleSetupStartDate;
    private int initialCycleSetupLength;
    private int initialCycleSetupStep;
    private int initialCycleSetupDirection = 1;
    private boolean initialSetupPermissionFlowPending;
    private boolean awaitingInitialExactAlarmSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int lastVersion = prefs.getInt(KEY_LAST_VERSION, -1);
        if (!prefs.contains(KEY_INITIAL_CYCLE_SETUP_COMPLETED)) {
            // Existing installs already have cycle settings; only new installs should see setup.
            boolean existingInstall = lastVersion >= 0 || prefs.getBoolean(KEY_WELCOME_SHOWN, false);
            prefs.edit().putBoolean(KEY_INITIAL_CYCLE_SETUP_COMPLETED, existingInstall).apply();
        }

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
        fragmentManager.registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm,
                                                  @NonNull Fragment fragment) {
                        if (fragment.getId() == R.id.fragment_container) {
                            updateNotesButtonVisibility();
                        }
                    }
                },
                true
        );
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
        viewModel.getBackgroundBlurDashboardPercent().observe(this, percent -> refreshGlobalBackgroundBlur());

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
            loadFragment(new NotesFragment(), false);
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
                if (openUpdateBackupOnNextSettings || openStockTrackerOnNextSettings) {
                    Bundle args = new Bundle();
                    if (openUpdateBackupOnNextSettings) {
                        args.putBoolean(SettingsFragment.ARG_OPEN_UPDATE_BACKUP_DIALOG, true);
                    }
                    if (openStockTrackerOnNextSettings) {
                        args.putBoolean(SettingsFragment.ARG_OPEN_STOCK_TRACKER_DIALOG, true);
                    }
                    settingsFragment.setArguments(args);
                    openUpdateBackupOnNextSettings = false;
                    openStockTrackerOnNextSettings = false;
                }
                selectedFragment = settingsFragment;
                btnNotes.setVisibility(View.GONE);
            } else {
                btnNotes.setVisibility(View.GONE);
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, false);
                return true;
            }

            return false;
        });

        // Listen for back stack changes to manage visibility and background behavior
        fragmentManager.addOnBackStackChangedListener(this::updateNotesButtonVisibility);
        updateNotesButtonVisibility();

        // Handle back button presses to navigate through fragments
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (firstRunWizardOverlay != null) {
                    if (initialCycleSetupContent != null) {
                        if (initialCycleSetupStep > 0) {
                            initialCycleSetupDirection = -1;
                            initialCycleSetupStep--;
                            updateInitialCycleSetupPage(true);
                        } else {
                            closeInitialCycleSetupAndReturnToWelcome();
                        }
                    }
                    return;
                }
                Fragment currentFragment = getVisibleFragment();
                if (!(currentFragment instanceof HomeFragment)) {
                    fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

        handleLaunchIntent(getIntent());

        // Create the notification channel before scheduling reminder jobs.
        createNotificationChannel();
        ReminderScheduler.scheduleCurrentCycle(this);
        StockManager.syncCurrentCycle(this, viewModel.getRepository());
        viewModel.refreshStockState();

        maybeStartWelcomeFlow();
    }

    private void refreshGlobalBackgroundImage() {
        if (globalBackgroundImage == null || viewModel == null) {
            return;
        }
        boolean enabled = Boolean.TRUE.equals(viewModel.getBackgroundAllScreensEnabled().getValue());
        String uriStr = viewModel.getBackgroundImageUri().getValue();
        if (!enabled) {
            BackgroundImageLoader.cancel(globalBackgroundImage);
            globalBackgroundImage.setImageDrawable(null);
            globalBackgroundImage.setVisibility(View.GONE);
            refreshGlobalBackgroundBlur();
            return;
        }
        if (uriStr == null || uriStr.trim().isEmpty()) {
            BackgroundImageLoader.cancel(globalBackgroundImage);
            globalBackgroundImage.setImageResource(R.drawable.default_bg);
            globalBackgroundImage.setVisibility(View.VISIBLE);
            refreshGlobalBackgroundBlur();
            return;
        }

        Uri uri = Uri.parse(uriStr);
        globalBackgroundImage.setVisibility(View.VISIBLE);
        BackgroundImageLoader.load(this, uri, globalBackgroundImage, bitmap -> {
            if (globalBackgroundImage == null) {
                return;
            }
            if (bitmap == null) {
                viewModel.setBackgroundImageUri(null);
                globalBackgroundImage.setImageResource(R.drawable.default_bg);
            } else {
                globalBackgroundImage.setImageBitmap(bitmap);
            }
            globalBackgroundImage.setVisibility(View.VISIBLE);
            refreshGlobalBackgroundBlur();
        });
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
        Integer blurPercent = isHomeFragmentVisible()
                ? viewModel.getBackgroundBlurDashboardPercent().getValue()
                : viewModel.getBackgroundBlurOthersPercent().getValue();
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
        if (bottomNavigationView == null) {
            return;
        }

        int inactive = Color.parseColor("#D4DCE3");
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };

        ColorStateList iconTint = new ColorStateList(states, new int[]{Color.WHITE, inactive});
        ColorStateList textTint = new ColorStateList(states, new int[]{selectedColor, inactive});
        bottomNavigationView.setItemIconTintList(iconTint);
        bottomNavigationView.setItemTextColor(textTint);
        bottomNavigationView.setItemBackground(createBottomNavItemBackground(selectedColor));
    }

    private Drawable createBottomNavItemBackground(int accentColor) {
        int pillColor = Color.argb(108, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
        GradientDrawable pill = new GradientDrawable();
        pill.setShape(GradientDrawable.RECTANGLE);
        pill.setCornerRadius(dpToPx(999));
        pill.setColor(pillColor);

        LayerDrawable checkedLayer = new LayerDrawable(new Drawable[]{pill});
        checkedLayer.setLayerSize(0, dpToPx(58), dpToPx(32));
        checkedLayer.setLayerInset(0, 0, dpToPx(4), 0, 0);
        checkedLayer.setLayerGravity(0, android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_checked}, checkedLayer);
        states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        return states;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleLaunchIntent(intent);
    }

    private void handleLaunchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        if (intent.getBooleanExtra("open_home", false)) {
            openHomeFromExternalTrigger();
            return;
        }
        Uri data = intent.getData();
        if (data == null
                || !SHORTCUT_SCHEME.equals(data.getScheme())
                || !SHORTCUT_HOST.equals(data.getHost())) {
            return;
        }
        String destination = data.getLastPathSegment();
        if (SHORTCUT_CALENDAR.equals(destination)) {
            navigateToPrimaryDestination(R.id.nav_calendar);
        } else if (SHORTCUT_CYCLES.equals(destination)) {
            navigateToPrimaryDestination(R.id.nav_cycles);
        } else if (SHORTCUT_SETTINGS.equals(destination)) {
            navigateToPrimaryDestination(R.id.nav_settings);
        } else if (SHORTCUT_STOCK.equals(destination)) {
            openStockTrackerSettings();
        } else if (SHORTCUT_NOTES.equals(destination)) {
            openNotesShortcut();
        }
    }

    private void openHomeFromExternalTrigger() {
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
        btnNotes.setVisibility(View.VISIBLE);
    }

    private void navigateToPrimaryDestination(int navItemId) {
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (bottomNavigationView != null && bottomNavigationView.getSelectedItemId() != navItemId) {
            bottomNavigationView.setSelectedItemId(navItemId);
            return;
        }
        if (navItemId == R.id.nav_calendar) {
            loadFragment(new CalendarFragment(), false);
        } else if (navItemId == R.id.nav_cycles) {
            loadFragment(new CyclesFragment(), false);
        } else if (navItemId == R.id.nav_settings) {
            loadFragment(new SettingsFragment(), false);
        } else {
            loadFragment(new HomeFragment(), false);
        }
    }

    private void openNotesShortcut() {
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new NotesFragment(), false);
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().setGroupCheckable(0, true, true);
            android.view.MenuItem homeItem = bottomNavigationView.getMenu().findItem(R.id.nav_home);
            if (homeItem != null) {
                homeItem.setChecked(true);
            }
        }
        btnNotes.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNotesButtonVisibility();
        maybeRequestAppUnlock();
        if (viewModel != null) {
            StockManager.syncCurrentCycle(this, viewModel.getRepository());
            viewModel.refreshStockState();
        }
        if (awaitingInitialExactAlarmSettings) {
            awaitingInitialExactAlarmSettings = false;
            finishInitialCycleSetup();
        }
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
        if (globalBackgroundImage != null) {
            BackgroundImageLoader.cancel(globalBackgroundImage);
        }
        super.onDestroy();
    }

    private void updateNotesButtonVisibility() {
        Fragment currentFragment = getVisibleFragment();
        if (currentFragment instanceof HomeFragment) {
            btnNotes.setVisibility(View.VISIBLE);
        } else {
            btnNotes.setVisibility(View.GONE);
        }
        refreshGlobalBackgroundBlur();
    }

    private boolean isHomeFragmentVisible() {
        Fragment currentFragment = getVisibleFragment();
        return currentFragment instanceof HomeFragment;
    }

    public boolean consumeStartupUpdateCheckRequest() {
        if (startupUpdateCheckConsumed) {
            return false;
        }
        startupUpdateCheckConsumed = true;
        return true;
    }

    public void openStockTrackerSettings() {
        openStockTrackerOnNextSettings = true;
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (bottomNavigationView != null && bottomNavigationView.getSelectedItemId() != R.id.nav_settings) {
            bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        } else {
            SettingsFragment settingsFragment = new SettingsFragment();
            Bundle args = new Bundle();
            args.putBoolean(SettingsFragment.ARG_OPEN_STOCK_TRACKER_DIALOG, true);
            settingsFragment.setArguments(args);
            openStockTrackerOnNextSettings = false;
            loadFragment(settingsFragment, false);
        }
        btnNotes.setVisibility(View.GONE);
    }

    public void openUpdateBackupFlowFromHome() {
        openUpdateBackupOnNextSettings = true;
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_settings);
        }
    }

    public void openPeriodDetails(int year, int month) {
        loadFragment(PeriodDetailsFragment.newInstance(year, month), true);
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
        // Resolve the previous navigation before choosing which fragment to hide.
        // This also keeps rapid tab taps and launch shortcuts from leaving two tabs visible.
        fragmentManager.executePendingTransactions();
        String reusableTag = getReusableFragmentTag(fragment);
        Fragment targetFragment = reusableTag != null
                ? fragmentManager.findFragmentByTag(reusableTag)
                : null;
        if (targetFragment == null) {
            targetFragment = fragment;
        }
        Fragment currentFragment = getVisibleFragment();
        if (targetFragment == currentFragment) {
            return;
        }

        clearFragmentTransitionState(currentFragment);
        clearFragmentTransitionState(targetFragment);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        int deferredEnterAnimation = addToBackStack
                ? 0
                : resolveFragmentEnterAnimation(currentFragment, targetFragment);
        if (addToBackStack) {
            applyFragmentAnimation(transaction, targetFragment);
        } else {
            transaction.setCustomAnimations(0, 0, 0, 0);
        }
        if (currentFragment != null) {
            if (!addToBackStack
                    && currentFragment instanceof SettingsFragment
                    && getReusableFragmentTag(currentFragment) == null) {
                transaction.remove(currentFragment);
            } else {
                transaction.hide(currentFragment);
                // STARTED keeps the fragment's view alive while pausing interaction.
                transaction.setMaxLifecycle(currentFragment, Lifecycle.State.STARTED);
            }
        }
        for (Fragment existingFragment : fragmentManager.getFragments()) {
            if (existingFragment != null
                    && existingFragment != currentFragment
                    && existingFragment != targetFragment
                    && existingFragment instanceof SettingsFragment
                    && getReusableFragmentTag(existingFragment) == null) {
                transaction.remove(existingFragment);
            }
        }
        if (targetFragment.isAdded()) {
            transaction.show(targetFragment);
        } else {
            String tag = reusableTag != null
                    ? reusableTag
                    : targetFragment.getClass().getName() + ":"
                    + Integer.toHexString(System.identityHashCode(targetFragment));
            transaction.add(R.id.fragment_container, targetFragment, tag);
        }
        transaction.setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        } else if (deferredEnterAnimation != 0) {
            Fragment enteringFragment = targetFragment;
            int enterAnimation = deferredEnterAnimation;
            transaction.runOnCommit(() -> startFragmentEnterAnimation(enteringFragment, enterAnimation));
        }
        transaction.setReorderingAllowed(true);
        transaction.commit();
    }

    private void startFragmentEnterAnimation(@NonNull Fragment fragment, int animationResource) {
        View enteringView = fragment.getView();
        if (enteringView == null || !fragment.isAdded() || fragment.isHidden()) {
            return;
        }
        clearFragmentTransitionState(fragment);

        int width = enteringView.getWidth();
        int height = enteringView.getHeight();
        if (enteringView.getParent() instanceof View) {
            View parent = (View) enteringView.getParent();
            if (width <= 0) {
                width = parent.getWidth();
            }
            if (height <= 0) {
                height = parent.getHeight();
            }
        }
        if (width <= 0) {
            width = getResources().getDisplayMetrics().widthPixels;
        }
        if (height <= 0) {
            height = getResources().getDisplayMetrics().heightPixels;
        }

        ViewPropertyAnimator animator = enteringView.animate()
                .setDuration(NAVIGATION_ANIMATION_DURATION_MS)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f));
        if (animationResource == R.anim.slide_in_right) {
            enteringView.setTranslationX(width * 0.08f);
            animator.translationX(0f);
        } else if (animationResource == R.anim.slide_in_left) {
            enteringView.setTranslationX(-width * 0.08f);
            animator.translationX(0f);
        } else if (animationResource == R.anim.slide_in_up) {
            enteringView.setTranslationY(height * 0.07f);
            animator.translationY(0f);
        } else if (animationResource == R.anim.zoom_in_fade_in) {
            enteringView.setScaleX(0.97f);
            enteringView.setScaleY(0.97f);
            enteringView.setAlpha(0f);
            animator.scaleX(1f).scaleY(1f).alpha(1f);
        } else if (animationResource == R.anim.pop_in) {
            enteringView.setScaleX(0.97f);
            enteringView.setScaleY(0.97f);
            animator.scaleX(1f).scaleY(1f);
        } else if (animationResource == R.anim.rotate_in_fade_in) {
            enteringView.setRotation(-1.5f);
            animator.rotation(0f);
        } else if (animationResource == R.anim.fade_in_fast) {
            enteringView.setAlpha(0f);
            animator.alpha(1f);
        }
    }

    private void clearFragmentTransitionState(@Nullable Fragment fragment) {
        if (fragment == null || fragment.getView() == null) {
            return;
        }
        View view = fragment.getView();
        view.animate().cancel();
        view.clearAnimation();
        view.setAlpha(1f);
        view.setTranslationX(0f);
        view.setTranslationY(0f);
        view.setScaleX(1f);
        view.setScaleY(1f);
        view.setRotation(0f);
    }

    @Nullable
    private String getReusableFragmentTag(@NonNull Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            return "primary:home";
        }
        if (fragment instanceof CalendarFragment) {
            return "primary:calendar";
        }
        if (fragment instanceof CyclesFragment) {
            return "primary:cycles";
        }
        if (fragment instanceof NotesFragment) {
            return "secondary:notes";
        }
        if (fragment instanceof SettingsFragment) {
            Bundle arguments = fragment.getArguments();
            if (arguments == null || arguments.isEmpty()) {
                return "primary:settings";
            }
        }
        return null;
    }

    @Nullable
    private Fragment getVisibleFragment() {
        List<Fragment> fragments = fragmentManager.getFragments();
        for (int i = fragments.size() - 1; i >= 0; i--) {
            Fragment fragment = fragments.get(i);
            if (fragment != null && fragment.isAdded() && !fragment.isHidden()) {
                return fragment;
            }
        }
        return null;
    }

    private void applyFragmentAnimation(FragmentTransaction transaction, Fragment targetFragment) {
        Fragment currentFragment = getVisibleFragment();
        if (currentFragment == null) {
            transaction.setCustomAnimations(R.anim.fade_in_fast, 0);
            return;
        }

        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_NONE) {
            transaction.setCustomAnimations(0, 0, 0, 0);
            return;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_FADE) {
            transaction.setCustomAnimations(
                    R.anim.fade_in_fast,
                    0,
                    R.anim.fade_in_fast,
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
            transaction.setCustomAnimations(R.anim.fade_in_fast, 0,
                    R.anim.fade_in_fast, 0);
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

    private int resolveFragmentEnterAnimation(@Nullable Fragment currentFragment,
                                             @NonNull Fragment targetFragment) {
        if (currentFragment == null) {
            return R.anim.fade_in_fast;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_NONE) {
            return 0;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_FADE) {
            return R.anim.fade_in_fast;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_ZOOM) {
            return R.anim.zoom_in_fade_in;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_SLIDE_UP) {
            return R.anim.slide_in_up;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_ROTATE) {
            return R.anim.rotate_in_fade_in;
        }
        if (navigationAnimationStyle == SettingsRepository.NAV_ANIM_POP) {
            return R.anim.pop_in;
        }

        int currentOrder = getFragmentOrder(currentFragment);
        int targetOrder = getFragmentOrder(targetFragment);
        if (targetOrder == currentOrder) {
            return R.anim.fade_in_fast;
        }
        return targetOrder > currentOrder ? R.anim.slide_in_right : R.anim.slide_in_left;
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
        if (firstRunWizardOverlay != null && firstRunWizardWelcomeScreen) {
            return;
        }

        View content = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        MaterialButton startButton = content.findViewById(R.id.welcome_start);
        MaterialButton skipButton = content.findViewById(R.id.welcome_skip);
        Integer buttonColor = viewModel.getButtonColor().getValue();
        if (buttonColor != null) {
            ButtonColorHelper.applyPrimaryColor(startButton, buttonColor);
            startButton.setTextColor(Color.WHITE);
        }

        startButton.setOnClickListener(v -> {
            if (prefs.getBoolean(KEY_INITIAL_CYCLE_SETUP_COMPLETED, false)) {
                prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply();
                dismissFirstRunWizardScreen();
                maybeStartGuidedTour();
            } else {
                startInitialCycleSetup();
            }
        });

        skipButton.setOnClickListener(v -> {
            prefs.edit()
                    .putBoolean(KEY_WELCOME_SHOWN, true)
                    .putBoolean(KEY_TOUR_SHOWN, true)
                    .putBoolean(KEY_INITIAL_CYCLE_SETUP_COMPLETED, true)
                    .apply();
            dismissFirstRunWizardScreen();
        });

        showFirstRunWizardScreen(content);
    }

    private void showFirstRunWizardScreen(View screen) {
        if (firstRunWizardHost == null) {
            firstRunWizardHost = findViewById(android.R.id.content);
        }
        if (firstRunWizardHost == null || screen == null) {
            return;
        }
        if (!originalSystemBarColorsSaved) {
            originalStatusBarColor = getWindow().getStatusBarColor();
            originalNavigationBarColor = getWindow().getNavigationBarColor();
            originalSystemBarColorsSaved = true;
        }
        if (firstRunWizardOverlay != null) {
            firstRunWizardHost.removeView(firstRunWizardOverlay);
        }
        firstRunWizardOverlay = screen;
        firstRunWizardWelcomeScreen = screen.findViewById(R.id.welcome_start) != null;
        firstRunWizardHost.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        screen.setFocusableInTouchMode(true);
        screen.requestFocus();
        screen.bringToFront();
        getWindow().setStatusBarColor(Color.rgb(11, 14, 21));
        getWindow().setNavigationBarColor(Color.rgb(11, 14, 21));
    }

    private void dismissFirstRunWizardScreen() {
        if (firstRunWizardOverlay != null && firstRunWizardHost != null) {
            firstRunWizardHost.removeView(firstRunWizardOverlay);
        }
        firstRunWizardOverlay = null;
        firstRunWizardWelcomeScreen = false;
        if (originalSystemBarColorsSaved) {
            getWindow().setStatusBarColor(originalStatusBarColor);
            getWindow().setNavigationBarColor(originalNavigationBarColor);
            originalSystemBarColorsSaved = false;
        }
    }

    private void startInitialCycleSetup() {
        Calendar currentStartDate = viewModel.getStartDate().getValue();
        initialCycleSetupStartDate = currentStartDate != null
                ? (Calendar) currentStartDate.clone()
                : viewModel.getRepository().getStartDate();
        Integer currentCycleLength = viewModel.getCycleLength().getValue();
        initialCycleSetupLength = currentCycleLength != null ? currentCycleLength : 21;
        initialCycleSetupStep = 0;

        initialCycleSetupContent = getLayoutInflater().inflate(R.layout.dialog_initial_cycle_setup, null);
        initialCycleSetupContent.findViewById(R.id.initial_setup_back).setOnClickListener(v -> {
            if (initialCycleSetupStep > 0) {
                initialCycleSetupDirection = -1;
                initialCycleSetupStep--;
                updateInitialCycleSetupPage(true);
            } else {
                closeInitialCycleSetupAndReturnToWelcome();
            }
        });
        initialCycleSetupContent.findViewById(R.id.initial_setup_continue).setOnClickListener(v -> {
            if (initialCycleSetupStep == 2) {
                saveInitialCycleSettings();
                if (hasReminderPermissionToRequest()) {
                    initialCycleSetupDirection = 1;
                    initialCycleSetupStep++;
                    updateInitialCycleSetupPage(true);
                } else {
                    finishInitialCycleSetup();
                }
            } else if (initialCycleSetupStep < 3) {
                initialCycleSetupDirection = 1;
                initialCycleSetupStep++;
                updateInitialCycleSetupPage(true);
            } else if (initialCycleSetupStep == 3) {
                beginInitialSetupPermissionFlow();
            } else {
                finishInitialCycleSetup();
            }
        });
        initialCycleSetupContent.findViewById(R.id.initial_setup_value_card)
                .setOnClickListener(v -> {
                    if (initialCycleSetupStep == 0) {
                        showInitialSetupDatePicker();
                    } else if (initialCycleSetupStep == 1) {
                        showInitialSetupTimePicker();
                    }
                });
        initialCycleSetupContent.findViewById(R.id.initial_setup_cycle_minus)
                .setOnClickListener(v -> {
                    initialCycleSetupLength = Math.max(14, initialCycleSetupLength - 1);
                    updateInitialCycleSetupPage(false);
                });
        initialCycleSetupContent.findViewById(R.id.initial_setup_cycle_plus)
                .setOnClickListener(v -> {
                    initialCycleSetupLength = Math.min(35, initialCycleSetupLength + 1);
                    updateInitialCycleSetupPage(false);
                });

        updateInitialCycleSetupPage(false);
        showFirstRunWizardScreen(initialCycleSetupContent);
    }

    private void updateInitialCycleSetupPage(boolean animate) {
        if (initialCycleSetupContent == null) {
            return;
        }
        View pageContent = initialCycleSetupContent.findViewById(R.id.initial_setup_page_content);
        if (animate) {
            pageContent.animate().cancel();
            pageContent.setAlpha(0f);
            float slideDistance = 18f * getResources().getDisplayMetrics().density;
            pageContent.setTranslationX(initialCycleSetupDirection * slideDistance);
        }

        TextView step = initialCycleSetupContent.findViewById(R.id.initial_setup_step);
        TextView fieldTitle = initialCycleSetupContent.findViewById(R.id.initial_setup_field_title);
        TextView value = initialCycleSetupContent.findViewById(R.id.initial_setup_value);
        TextView cycleValue = initialCycleSetupContent.findViewById(R.id.initial_setup_cycle_value);
        View valueCard = initialCycleSetupContent.findViewById(R.id.initial_setup_value_card);
        View cycleControls = initialCycleSetupContent.findViewById(R.id.initial_setup_cycle_controls);
        View iconContainer = initialCycleSetupContent.findViewById(R.id.initial_setup_icon_container);
        ImageView icon = initialCycleSetupContent.findViewById(R.id.initial_setup_icon);
        MaterialButton continueButton = initialCycleSetupContent.findViewById(R.id.initial_setup_continue);
        MaterialButton backButton = initialCycleSetupContent.findViewById(R.id.initial_setup_back);

        step.setText(String.format(Locale.getDefault(), "%02d / 04", initialCycleSetupStep + 1));
        int iconResource;
        TextView headerTitle = initialCycleSetupContent.findViewById(R.id.initial_setup_header_title);
        TextView headerHint = initialCycleSetupContent.findViewById(R.id.initial_setup_header_hint);
        View permissionsPanel = initialCycleSetupContent.findViewById(R.id.initial_setup_permissions_panel);
        if (initialCycleSetupStep == 0) {
            headerTitle.setText(R.string.initial_setup_title);
            headerHint.setText(R.string.initial_setup_hint);
            fieldTitle.setText(R.string.settings_label_start_date);
            value.setText(DateFormat.getDateFormat(this).format(initialCycleSetupStartDate.getTime()));
            iconResource = R.drawable.ic_nav_calendar_outline;
            valueCard.setVisibility(View.VISIBLE);
            cycleControls.setVisibility(View.GONE);
            permissionsPanel.setVisibility(View.GONE);
        } else if (initialCycleSetupStep == 1) {
            headerTitle.setText(R.string.initial_setup_title);
            headerHint.setText(R.string.initial_setup_hint);
            fieldTitle.setText(R.string.settings_label_time);
            value.setText(DateFormat.getTimeFormat(this).format(initialCycleSetupStartDate.getTime()));
            iconResource = R.drawable.ic_setup_clock;
            valueCard.setVisibility(View.VISIBLE);
            cycleControls.setVisibility(View.GONE);
            permissionsPanel.setVisibility(View.GONE);
        } else if (initialCycleSetupStep == 2) {
            headerTitle.setText(R.string.initial_setup_title);
            headerHint.setText(R.string.initial_setup_hint);
            fieldTitle.setText(R.string.settings_label_cycle_length);
            cycleValue.setText(getString(R.string.settings_cycle_length_value, initialCycleSetupLength));
            iconResource = R.drawable.ic_nav_cycles_outline;
            valueCard.setVisibility(View.GONE);
            cycleControls.setVisibility(View.VISIBLE);
            permissionsPanel.setVisibility(View.GONE);
        } else {
            headerTitle.setText(R.string.initial_setup_reminders_title);
            headerHint.setText(R.string.initial_setup_permissions_intro);
            iconResource = R.drawable.ic_setup_bell;
            valueCard.setVisibility(View.GONE);
            cycleControls.setVisibility(View.GONE);
            fieldTitle.setVisibility(View.GONE);
            permissionsPanel.setVisibility(View.VISIBLE);
            updateInitialSetupPermissionRows();
        }
        if (initialCycleSetupStep < 3) {
            fieldTitle.setVisibility(View.VISIBLE);
        }
        icon.setImageResource(iconResource);
        continueButton.setText(initialCycleSetupStep == 3
                ? R.string.initial_setup_enable_reminders
                : R.string.initial_setup_continue);
        backButton.setVisibility(View.VISIBLE);

        int accentColor = viewModel.getButtonColor().getValue() != null
                ? viewModel.getButtonColor().getValue()
                : SettingsRepository.DEFAULT_BUTTON_COLOR;
        ButtonColorHelper.applyPrimaryColor(continueButton, accentColor);
        continueButton.setTextColor(Color.WHITE);
        iconContainer.getBackground().mutate().setTint(accentColor);
        for (int i = 0; i < 4; i++) {
            View segment = initialCycleSetupContent.findViewById(
                    i == 0 ? R.id.initial_setup_progress_1
                            : i == 1 ? R.id.initial_setup_progress_2
                            : i == 2 ? R.id.initial_setup_progress_3 : R.id.initial_setup_progress_4);
            segment.getBackground().mutate().setTint(i <= initialCycleSetupStep
                    ? accentColor
                    : 0x42FFFFFF);
        }

        if (animate) {
            pageContent.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .setDuration(190L)
                    .start();
        }
    }

    private void showInitialSetupDatePicker() {
        Calendar utcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcDate.clear();
        utcDate.set(initialCycleSetupStartDate.get(Calendar.YEAR),
                initialCycleSetupStartDate.get(Calendar.MONTH),
                initialCycleSetupStartDate.get(Calendar.DAY_OF_MONTH));
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.settings_label_start_date)
                .setSelection(utcDate.getTimeInMillis())
                .build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Calendar selectedUtcDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedUtcDate.setTimeInMillis(selection);
            initialCycleSetupStartDate.set(
                    selectedUtcDate.get(Calendar.YEAR),
                    selectedUtcDate.get(Calendar.MONTH),
                    selectedUtcDate.get(Calendar.DAY_OF_MONTH));
            updateInitialCycleSetupPage(false);
        });
        datePicker.show(getSupportFragmentManager(), "initial_cycle_setup_date");
    }

    private void showInitialSetupTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTitleText(R.string.settings_label_time)
                .setTimeFormat(DateFormat.is24HourFormat(this)
                        ? TimeFormat.CLOCK_24H
                        : TimeFormat.CLOCK_12H)
                .setHour(initialCycleSetupStartDate.get(Calendar.HOUR_OF_DAY))
                .setMinute(initialCycleSetupStartDate.get(Calendar.MINUTE))
                .build();
        timePicker.addOnPositiveButtonClickListener(view -> {
            initialCycleSetupStartDate.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
            initialCycleSetupStartDate.set(Calendar.MINUTE, timePicker.getMinute());
            updateInitialCycleSetupPage(false);
        });
        timePicker.show(getSupportFragmentManager(), "initial_cycle_setup_time");
    }

    private void closeInitialCycleSetupAndReturnToWelcome() {
        initialCycleSetupContent = null;
        maybeStartWelcomeFlow();
    }

    private void updateInitialSetupPermissionRows() {
        if (initialCycleSetupContent == null) {
            return;
        }
        boolean needsNotifications = isNotificationPermissionMissing();
        boolean needsExactAlarms = isExactAlarmPermissionMissing();
        View notificationRow = initialCycleSetupContent.findViewById(R.id.initial_setup_notification_row);
        View exactAlarmRow = initialCycleSetupContent.findViewById(R.id.initial_setup_exact_alarm_row);
        View divider = initialCycleSetupContent.findViewById(R.id.initial_setup_permission_divider);
        notificationRow.setVisibility(needsNotifications ? View.VISIBLE : View.GONE);
        exactAlarmRow.setVisibility(needsExactAlarms ? View.VISIBLE : View.GONE);
        divider.setVisibility(needsNotifications && needsExactAlarms ? View.VISIBLE : View.GONE);
    }

    private boolean hasReminderPermissionToRequest() {
        return isNotificationPermissionMissing() || isExactAlarmPermissionMissing();
    }

    private boolean isNotificationPermissionMissing() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private boolean isExactAlarmPermissionMissing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return false;
        }
        AlarmManager alarmManager = getSystemService(AlarmManager.class);
        return alarmManager != null && !alarmManager.canScheduleExactAlarms();
    }

    private void saveInitialCycleSettings() {
        if (initialCycleSetupStartDate == null) {
            return;
        }
        viewModel.setStartDate((Calendar) initialCycleSetupStartDate.clone());
        viewModel.setCycleLength(initialCycleSetupLength);
        ReminderScheduler.scheduleCurrentCycle(this);
        StockManager.syncCurrentCycle(this, viewModel.getRepository());
        viewModel.refreshStockState();
        WidgetUpdater.updateAllWidgets(this);
    }

    private void beginInitialSetupPermissionFlow() {
        if (isNotificationPermissionMissing()) {
            initialSetupPermissionFlowPending = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION_PERMISSION);
            return;
        }
        continueInitialSetupAfterNotificationPermission();
    }

    private void continueInitialSetupAfterNotificationPermission() {
        if (!isExactAlarmPermissionMissing()) {
            finishInitialCycleSetup();
            return;
        }

        viewModel.getRepository().setExactAlarmPrompted(true);
        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
        intent.setData(Uri.parse("package:" + getPackageName()));
        awaitingInitialExactAlarmSettings = true;
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            awaitingInitialExactAlarmSettings = false;
            finishInitialCycleSetup();
        }
    }

    private void finishInitialCycleSetup() {
        saveInitialCycleSettings();
        prefs.edit()
                .putBoolean(KEY_INITIAL_CYCLE_SETUP_COMPLETED, true)
                .putBoolean(KEY_WELCOME_SHOWN, true)
                .apply();
        initialCycleSetupContent = null;
        dismissFirstRunWizardScreen();
        maybeStartGuidedTour();
    }

    public boolean isInitialCycleSetupPending() {
        return prefs != null && !prefs.getBoolean(KEY_INITIAL_CYCLE_SETUP_COMPLETED, false);
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

        Fragment currentFragment = getVisibleFragment();
        CalendarFragment calendarFragment = currentFragment instanceof CalendarFragment
                ? (CalendarFragment) currentFragment
                : null;
        SettingsFragment settingsFragment = currentFragment instanceof SettingsFragment
                ? (SettingsFragment) currentFragment
                : null;
        if (calendarFragment != null) {
            calendarFragment.ensurePeriodDialogVisibleForTour(step.targetViewId);
        }
        if (settingsFragment != null) {
            settingsFragment.ensureNotificationToolsDialogVisibleForTour(step.targetViewId);
            settingsFragment.ensureBackgroundToolsDialogVisibleForTour(step.targetViewId);
            settingsFragment.ensureStockToolsDialogVisibleForTour(step.targetViewId);
            settingsFragment.ensureRingUsePauseDialogVisibleForTour(step.targetViewId);
        }
        ViewGroup host = findViewById(android.R.id.content);
        if (calendarFragment != null && calendarFragment.isPeriodDialogTourTarget(step.targetViewId)) {
            ViewGroup dialogHost = calendarFragment.getPeriodDialogTourHost();
            if (dialogHost != null) {
                host = dialogHost;
            }
        } else if (settingsFragment != null && settingsFragment.isNotificationToolsDialogTourTarget(step.targetViewId)) {
            ViewGroup dialogHost = settingsFragment.getNotificationToolsDialogTourHost(step.targetViewId);
            if (dialogHost != null) {
                host = dialogHost;
            }
        } else if (settingsFragment != null && settingsFragment.isBackgroundToolsDialogTourTarget(step.targetViewId)) {
            ViewGroup dialogHost = settingsFragment.getBackgroundToolsDialogTourHost(step.targetViewId);
            if (dialogHost != null) {
                host = dialogHost;
            }
        } else if (settingsFragment != null && settingsFragment.isStockToolsDialogTourTarget(step.targetViewId)) {
            ViewGroup dialogHost = settingsFragment.getStockToolsDialogTourHost(step.targetViewId);
            if (dialogHost != null) {
                host = dialogHost;
            }
        } else if (settingsFragment != null && settingsFragment.isRingUsePauseDialogTourTarget(step.targetViewId)) {
            ViewGroup dialogHost = settingsFragment.getRingUsePauseDialogTourHost(step.targetViewId);
            if (dialogHost != null) {
                host = dialogHost;
            }
        }
        reattachTourOverlay(host);
        View fragmentView = currentFragment != null ? currentFragment.getView() : null;
        View target;
        if (step.inActivityView) {
            target = findViewById(step.targetViewId);
        } else {
            if (calendarFragment != null) {
                target = calendarFragment.findTourTargetView(step.targetViewId);
            } else if (settingsFragment != null) {
                target = settingsFragment.findTourTargetView(step.targetViewId);
            } else {
                target = fragmentView != null ? fragmentView.findViewById(step.targetViewId) : null;
            }
            if ((target == null || target.getVisibility() != View.VISIBLE)
                    && step.targetViewId == R.id.legend_period_column
                    && fragmentView != null) {
                View fallbackCalendar = fragmentView.findViewById(R.id.calendarView);
                if (fallbackCalendar != null && fallbackCalendar.getVisibility() == View.VISIBLE) {
                    target = fallbackCalendar;
                }
            }
            if ((target == null || target.getVisibility() != View.VISIBLE)
                    && step.targetViewId == R.id.legend_tables_row
                    && fragmentView != null) {
                View fallbackLegend = fragmentView.findViewById(R.id.legend_original_row);
                if (fallbackLegend != null && fallbackLegend.getVisibility() == View.VISIBLE) {
                    target = fallbackLegend;
                }
            }
        }

        Log.d("TourDebug", "Index: " + index + " Attempt: " + attempt + " TargetID: " + step.targetViewId);
        if (target == null) {
            getWindow().getDecorView().postDelayed(() -> showGuidedTourStep(index, attempt + 1), 120);
            return;
        }

        if (step.scrollViewId != 0 && fragmentView != null) {
            ScrollView scrollView = fragmentView.findViewById(step.scrollViewId);
            if (scrollView != null && isDescendantOf(target, scrollView)) {
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

    private void reattachTourOverlay(ViewGroup host) {
        if (tourOverlay == null || host == null) {
            return;
        }
        android.view.ViewParent currentParent = tourOverlay.getParent();
        if (currentParent == host) {
            return;
        }
        if (currentParent instanceof ViewGroup) {
            ((ViewGroup) currentParent).removeView(tourOverlay);
        }
        host.addView(tourOverlay);
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
                .putBoolean(KEY_WELCOME_SHOWN, true)
                .putBoolean(KEY_TOUR_SHOWN, false)
                .apply();
        tourCompleted = false;
        tourSteps = null;
        tourIndex = 0;
        if (tourOverlay != null) {
            finishGuidedTour();
        }
        maybeStartGuidedTour();
    }

    private void scrollToView(ScrollView scrollView, View target) {
        if (!isDescendantOf(target, scrollView)) {
            return;
        }
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
        Fragment currentFragment = getVisibleFragment();
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
        Fragment currentFragment = getVisibleFragment();
        boolean isNotes = currentFragment instanceof NotesFragment;
        if (open) {
            if (isNotes) {
                return false;
            }
            loadFragment(new NotesFragment(), false);
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
        fragmentManager.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment(), false);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        btnNotes.setVisibility(View.VISIBLE);
    }

    private boolean isTargetVisible(ScrollView scrollView, View target) {
        if (!isDescendantOf(target, scrollView)) {
            return true;
        }
        Rect rect = new Rect();
        target.getDrawingRect(rect);
        scrollView.offsetDescendantRectToMyCoords(target, rect);
        int scrollTop = scrollView.getScrollY();
        int scrollBottom = scrollTop + scrollView.getHeight();
        return rect.top >= scrollTop && rect.bottom <= scrollBottom;
    }

    private boolean isDescendantOf(View child, ViewGroup ancestor) {
        View current = child;
        while (current != null) {
            if (current == ancestor) {
                return true;
            }
            android.view.ViewParent parent = current.getParent();
            if (!(parent instanceof View)) {
                return false;
            }
            current = (View) parent;
        }
        return false;
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
        steps.add(new TourStep(R.id.nav_calendar, R.id.switch_period_day,
                R.string.tour_title_period_entry, R.string.tour_body_period_entry, 0, false));
        steps.add(new TourStep(R.id.nav_calendar, R.id.period_modal_root,
                R.string.tour_title_period_modal, R.string.tour_body_period_modal, 0, false));
        steps.add(new TourStep(R.id.nav_calendar, R.id.legend_period_column,
                R.string.tour_title_period_indicators, R.string.tour_body_period_indicators, 0, false));
        steps.add(new TourStep(R.id.nav_calendar, R.id.legend_tables_row,
                R.string.tour_title_calendar_legend, R.string.tour_body_calendar_legend, 0, false));

        steps.add(new TourStep(R.id.nav_cycles, R.id.tv_history_title,
                R.string.tour_title_cycles, R.string.tour_body_cycles, 0, false));
        steps.add(new TourStep(R.id.nav_cycles, R.id.btn_rebuild_history,
                R.string.tour_title_cycles_rebuild, R.string.tour_body_cycles_rebuild, 0, false));
        steps.add(new TourStep(R.id.nav_cycles, R.id.btn_clear_history,
                R.string.tour_title_cycles_clear, R.string.tour_body_cycles_clear, 0, false));

        steps.add(new TourStep(R.id.nav_settings, R.id.btn_settings_info,
                R.string.tour_title_settings_info, R.string.tour_body_settings_info, 0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_package_leaflet,
                R.string.settings_package_leaflet_label, R.string.tour_body_package_leaflet,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.settings_cycle_fields_container,
                R.string.tour_title_settings_cycle, R.string.tour_body_settings_cycle,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_stock_tracker,
                R.string.settings_stock_tracker_label, R.string.tour_body_settings_stock,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_stock_tools_enabled,
                R.string.settings_stock_tracker_enable_label, R.string.settings_stock_tools_hint_enable,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_stock_tools_count,
                R.string.settings_stock_count_label, R.string.settings_stock_tools_hint_count,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_stock_tools_reminder,
                R.string.settings_stock_reminder_label, R.string.settings_stock_tools_hint_reminder,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_stock_tools_recipe_delay,
                R.string.settings_stock_recipe_delay_label, R.string.settings_stock_tools_hint_recipe_delay,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_ring_use_pause,
                R.string.tour_title_settings_ring_pause, R.string.tour_body_settings_ring_pause,
                R.id.settings_scroll, false));
        if (viewModel != null && viewModel.getRepository().isRingUsePaused()) {
            steps.add(new TourStep(R.id.nav_settings, android.R.id.button1,
                    R.string.tour_title_ring_resume_button, R.string.tour_body_ring_resume_button,
                    0, false));
            steps.add(new TourStep(R.id.nav_settings, android.R.id.button2,
                    R.string.tour_title_ring_pause_cancel_button, R.string.tour_body_ring_pause_cancel_button,
                    0, false));
        } else {
            steps.add(new TourStep(R.id.nav_settings, android.R.id.button1,
                    R.string.tour_title_ring_pause_confirm_button, R.string.tour_body_ring_pause_confirm_button,
                    0, false));
            steps.add(new TourStep(R.id.nav_settings, android.R.id.button3,
                    R.string.tour_title_ring_pause_choose_date_button, R.string.tour_body_ring_pause_choose_date_button,
                    0, false));
            steps.add(new TourStep(R.id.nav_settings, android.R.id.button2,
                    R.string.tour_title_ring_pause_cancel_button, R.string.tour_body_ring_pause_cancel_button,
                    0, false));
        }
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_calendar_range,
                R.string.tour_title_settings_calendar, R.string.tour_body_settings_calendar,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_group,
                R.string.tour_title_settings_notifications, R.string.tour_body_settings_notifications,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_times,
                R.string.btn_set_notification_times, R.string.notification_hint_set_times,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_test,
                R.string.btn_notification_test, R.string.notification_test_hint_short,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_permission,
                R.string.btn_notification_permission, R.string.notification_hint_permission,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_exact,
                R.string.exact_alarm_title, R.string.notification_hint_exact_alarm,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_notification_battery_opt,
                R.string.btn_notification_battery, R.string.notification_hint_battery,
                0, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools,
                R.string.settings_background_tools_label, R.string.tour_body_settings_background,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools_pick,
                R.string.btn_set_background, R.string.settings_background_tools_hint_pick,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools_all_screens,
                R.string.tour_title_settings_background_all, R.string.tour_body_settings_background_all,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools_dim,
                R.string.tour_title_settings_background_dim, R.string.tour_body_settings_background_dim,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools_blur,
                R.string.tour_title_settings_background_blur, R.string.tour_body_settings_background_blur,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_background_tools_remove,
                R.string.settings_background_tools_remove_label, R.string.tour_body_settings_background_reset,
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
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_particle_animation,
                R.string.tour_title_settings_particle_animation,
                R.string.tour_body_settings_particle_animation,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_navigation_animation,
                R.string.tour_title_settings_animation, R.string.tour_body_settings_animation,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_set_language,
                R.string.tour_title_settings_language, R.string.tour_body_settings_language,
                R.id.settings_scroll, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.advanced_header,
                R.string.tour_title_settings_advanced, R.string.tour_body_settings_advanced,
                R.id.settings_scroll, false, false, true, false, true, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_update_app,
                R.string.tour_title_settings_update, R.string.tour_body_settings_update,
                R.id.settings_scroll, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_backup_manage,
                R.string.tour_title_settings_backup, R.string.tour_body_settings_backup,
                R.id.settings_scroll, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_app_lock,
                R.string.tour_title_settings_app_lock, R.string.tour_body_settings_app_lock,
                R.id.settings_scroll, false, true, false, false, false, false, false, false));
        steps.add(new TourStep(R.id.nav_settings, R.id.btn_reset_app,
                R.string.tour_title_settings_reset, R.string.tour_body_settings_reset,
                R.id.settings_scroll, false, true, false, true, false, false, false, false));
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
                Constants.REMINDER_CHANNEL_ID,
                getString(R.string.notifications_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(getString(R.string.notifications_channel_description));
        channel.enableVibration(true);
        channel.setVibrationPattern(Constants.REMINDER_VIBRATION_PATTERN);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) {
            Log.w(NOTIFICATION_TAG, "NotificationManager unavailable, reminder channel not created");
            return;
        }
        manager.createNotificationChannel(channel);

        NotificationChannel created = manager.getNotificationChannel(Constants.REMINDER_CHANNEL_ID);
        if (created != null) {
            long[] pattern = created.getVibrationPattern();
            Log.d(
                    NOTIFICATION_TAG,
                    "Channel ready id=" + created.getId()
                            + ", importance=" + created.getImportance()
                            + ", shouldVibrate=" + created.shouldVibrate()
                            + ", patternLength=" + (pattern != null ? pattern.length : 0)
            );
        }
    }

    // Handle the result of the permission request
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION && initialSetupPermissionFlowPending) {
            initialSetupPermissionFlowPending = false;
            continueInitialSetupAfterNotificationPermission();
        }
    }

    private void clearAllScheduledNotifications() {
        ReminderScheduler.cancelAllScheduledNotifications(this);
    }
}
