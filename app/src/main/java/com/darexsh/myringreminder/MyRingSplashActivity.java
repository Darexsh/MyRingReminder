package com.darexsh.myringreminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyRingSplashActivity extends Activity {
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
        final RectF artworkRect = new RectF();
        Bitmap artwork;
        final Matrix sparkleMatrix = new Matrix();
        RadialGradient sparkleShader;
        float progress;
        SplashView() {
            super(MyRingSplashActivity.this);
            IMAGE_EXECUTOR.execute(() -> {
                Bitmap loaded = null;
                try (InputStream input = getAssets().open("myring_splash_art.jpg")) {
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
            // Orbit sparkle around the artwork's circular halo.
            float theta = (float) (-Math.PI / 3 + Math.PI * 2 * progress);
            float x = left + (450f + 315f * (float) Math.cos(theta)) * s;
            float y = top + (720f + 315f * (float) Math.sin(theta)) * s;
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
}
