package com.darexsh.myringreminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyRingSplashActivity extends Activity {
    private static final float ORBIT_CENTER_X = 450f;
    private static final float ORBIT_CENTER_Y = 720f;
    private static final float ORIGINAL_SPARKLE_X = 668f;
    private static final float ORIGINAL_SPARKLE_Y = 492f;
    private static final float ORBIT_RADIUS = (float) Math.hypot(
            ORIGINAL_SPARKLE_X - ORBIT_CENTER_X,
            ORIGINAL_SPARKLE_Y - ORBIT_CENTER_Y);
    private static final float ORBIT_START_ANGLE = (float) Math.atan2(
            ORIGINAL_SPARKLE_Y - ORBIT_CENTER_Y,
            ORIGINAL_SPARKLE_X - ORBIT_CENTER_X);
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            task.run();
        }, "SplashImageLoader");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator animator;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setNavigationBarColor(Color.rgb(47, 11, 73));
        CalendarRenderCache.warmAsync(new SettingsRepository(getApplicationContext()));
        SplashView splash = new SplashView();
        setContentView(splash);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(2400L);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            splash.progress = (float) a.getAnimatedValue();
            splash.invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                if (isFinishing() || isDestroyed()) return;
                // Replace MainActivity if your app uses a different launcher activity.
                startActivity(new Intent(MyRingSplashActivity.this, MainActivity.class));
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
        animator.start();
    }

    @Override protected void onDestroy() {
        if (animator != null) animator.cancel();
        super.onDestroy();
    }

    private final class SplashView extends View {
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Paint taglinePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        final RectF artworkRect = new RectF();
        final String tagline;
        Bitmap artwork;
        final Matrix sparkleMatrix = new Matrix();
        RadialGradient sparkleShader;
        float progress;
        SplashView() {
            super(MyRingSplashActivity.this);
            tagline = getLocalizedSplashTagline();
            taglinePaint.setColor(Color.rgb(222, 185, 255));
            taglinePaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            taglinePaint.setTextAlign(Paint.Align.CENTER);
            IMAGE_EXECUTOR.execute(() -> {
                Bitmap loaded = null;
                try (InputStream input = getAssets().open("myring_splash_art_localized.png")) {
                    loaded = BitmapFactory.decodeStream(input);
                } catch (IOException ignored) { }
                Bitmap result = loaded;
                mainHandler.post(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        artwork = result;
                        invalidate();
                    }
                });
            });
        }

        @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
            super.onSizeChanged(width, height, oldWidth, oldHeight);
            float unit = Math.min(width / 360f, height / 640f);
            sparkleShader = new RadialGradient(0f, 0f, 19f * unit,
                    new int[]{0xEEFFFFFF, 0x77F5A5FF, 0x00F5A5FF},
                    null, Shader.TileMode.CLAMP);
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            canvas.drawColor(Color.rgb(47, 11, 73));
            if (artwork == null || sparkleShader == null) return;
            // Center crop, matching ImageView.ScaleType.CENTER_CROP.
            float base = Math.max(w / (float) artwork.getWidth(), h / (float) artwork.getHeight());
            float zoom = 1.018f - 0.018f * Math.min(1f, progress / .45f);
            float s = base * zoom;
            float left = (w - artwork.getWidth() * s) / 2f;
            float top = (h - artwork.getHeight() * s) / 2f;
            artworkRect.set(left, top,
                    left + artwork.getWidth() * s, top + artwork.getHeight() * s);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            canvas.drawBitmap(artwork, null, artworkRect, paint);
            taglinePaint.setTextSize(42f * s);
            canvas.drawText(tagline, left + artwork.getWidth() * s / 2f, top + 1177f * s, taglinePaint);
            // Orbit sparkle around the artwork's circular halo.
            float theta = (float) (ORBIT_START_ANGLE + Math.PI * 2 * progress);
            float x = left + (ORBIT_CENTER_X + ORBIT_RADIUS * (float) Math.cos(theta)) * s;
            float y = top + (ORBIT_CENTER_Y + ORBIT_RADIUS * (float) Math.sin(theta)) * s;
            float unit = Math.min(w / 360f, h / 640f);
            float radius = 19f * unit;
            sparkleMatrix.setTranslate(x, y);
            sparkleShader.setLocalMatrix(sparkleMatrix);
            paint.setShader(sparkleShader);
            canvas.drawCircle(x, y, radius, paint);
            paint.setShader(null);
            paint.setColor(Color.WHITE);
            canvas.drawCircle(x, y, 2f * unit, paint);
        }
    }

    private String getLocalizedSplashTagline() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (appLocales.isEmpty()) {
            return getString(R.string.splash_tagline);
        }

        Locale appLocale = appLocales.get(0);
        if (appLocale == null) {
            return getString(R.string.splash_tagline);
        }

        Configuration localizedConfiguration = new Configuration(getResources().getConfiguration());
        localizedConfiguration.setLocale(appLocale);
        Context localizedContext = createConfigurationContext(localizedConfiguration);
        return localizedContext.getString(R.string.splash_tagline);
    }
}
