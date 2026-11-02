package com.darexsh.myringreminder;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

import java.util.Random;

/** A small, one-shot rocket and firework overlay triggered from the Home screen. */
public final class RocketEasterEggView extends View {

    private static final long FLIGHT_DURATION_MS = 560L;
    private static final long BURST_DURATION_MS = 1250L;
    private static final long TOTAL_DURATION_MS = FLIGHT_DURATION_MS + BURST_DURATION_MS;
    private static final int PARTICLE_COUNT = 76;
    private static final int[] PARTICLE_COLORS = {
            0xFFFFD166, 0xFFFF6B6B, 0xFF7BDFF2, 0xFFB2F7EF,
            0xFFB8A1FF, 0xFFFF9CEE, 0xFFFFFFFF
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rocketBody = new RectF();
    private final Path rocketPath = new Path();
    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private final float launchX;
    private final float launchY;
    private final int accentColor;
    private final Runnable onFinished;
    private final float density;

    private float burstX;
    private float burstY;
    private float animationProgress;
    private boolean particlesInitialized;
    private boolean finished;
    private boolean detaching;

    public RocketEasterEggView(@NonNull Context context,
                               float launchX,
                               float launchY,
                               int accentColor,
                               @NonNull Runnable onFinished) {
        super(context);
        this.launchX = launchX;
        this.launchY = launchY;
        this.accentColor = accentColor;
        this.onFinished = onFinished;
        density = getResources().getDisplayMetrics().density;

        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        animator.setDuration(TOTAL_DURATION_MS);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(valueAnimator -> {
            animationProgress = (float) valueAnimator.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishAndRemove();
            }
        });
    }

    public void start() {
        post(() -> {
            if (finished) {
                return;
            }
            if (getWidth() <= 0 || getHeight() <= 0) {
                finishAndRemove();
                return;
            }
            initializeParticles();
            animator.start();
        });
    }

    public void cancel() {
        if (animator.isStarted()) {
            animator.cancel();
        } else {
            finishAndRemove();
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        burstX = width * 0.5f;
        burstY = Math.max(dp(52f), Math.min(height * 0.22f, launchY - dp(150f)));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        long elapsedMs = Math.round(animationProgress * TOTAL_DURATION_MS);
        if (elapsedMs < FLIGHT_DURATION_MS) {
            drawRocketFlight(canvas, elapsedMs / (float) FLIGHT_DURATION_MS);
        } else {
            drawExplosion(canvas, elapsedMs - FLIGHT_DURATION_MS);
        }
    }

    private void drawRocketFlight(@NonNull Canvas canvas, float progress) {
        float eased = 1f - (1f - progress) * (1f - progress);
        float x = launchX + (burstX - launchX) * eased;
        float y = launchY + (burstY - launchY) * eased;
        float flamePulse = 0.85f + (float) Math.sin(progress * Math.PI * 12f) * 0.15f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(dp(5f));
        paint.setColor(withAlpha(accentColor, 95));
        canvas.drawLine(x, y + dp(14f), x, y + dp(30f) * flamePulse, paint);
        paint.setStrokeWidth(dp(2.5f));
        paint.setColor(0xFFFFD166);
        canvas.drawLine(x, y + dp(15f), x, y + dp(25f) * flamePulse, paint);

        canvas.save();
        canvas.translate(x, y);
        canvas.rotate((float) Math.sin(progress * Math.PI) * 7f);
        drawRocket(canvas, flamePulse);
        canvas.restore();
    }

    private void drawRocket(@NonNull Canvas canvas, float flamePulse) {
        paint.setStyle(Paint.Style.FILL);

        rocketPath.reset();
        rocketPath.moveTo(-dp(5f), dp(10f));
        rocketPath.lineTo(0f, dp(24f) * flamePulse);
        rocketPath.lineTo(dp(5f), dp(10f));
        rocketPath.close();
        paint.setColor(0xFFFF8C42);
        canvas.drawPath(rocketPath, paint);

        rocketPath.reset();
        rocketPath.moveTo(-dp(2.4f), dp(12f));
        rocketPath.lineTo(0f, dp(20f) * flamePulse);
        rocketPath.lineTo(dp(2.4f), dp(12f));
        rocketPath.close();
        paint.setColor(0xFFFFE082);
        canvas.drawPath(rocketPath, paint);

        rocketPath.reset();
        rocketPath.moveTo(-dp(6f), dp(4f));
        rocketPath.lineTo(-dp(15f), dp(14f));
        rocketPath.lineTo(-dp(6f), dp(13f));
        rocketPath.close();
        rocketPath.moveTo(dp(6f), dp(4f));
        rocketPath.lineTo(dp(15f), dp(14f));
        rocketPath.lineTo(dp(6f), dp(13f));
        rocketPath.close();
        paint.setColor(0xFFFF6B6B);
        canvas.drawPath(rocketPath, paint);

        rocketBody.set(-dp(8f), -dp(13f), dp(8f), dp(13f));
        paint.setColor(0xFFF7F4FF);
        canvas.drawRoundRect(rocketBody, dp(7f), dp(7f), paint);

        rocketPath.reset();
        rocketPath.moveTo(-dp(8f), -dp(5f));
        rocketPath.lineTo(0f, -dp(25f));
        rocketPath.lineTo(dp(8f), -dp(5f));
        rocketPath.close();
        paint.setColor(0xFFFF6B6B);
        canvas.drawPath(rocketPath, paint);

        paint.setColor(0xFF67D8F4);
        canvas.drawCircle(0f, 0f, dp(3.6f), paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(1.4f));
        paint.setColor(0xFF7A5CA8);
        canvas.drawCircle(0f, 0f, dp(3.6f), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawExplosion(@NonNull Canvas canvas, long burstElapsedMs) {
        float burstSeconds = burstElapsedMs / 1000f;
        float flashProgress = Math.min(1f, burstElapsedMs / 180f);

        if (flashProgress < 1f) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3f) * (1f - flashProgress) + dp(1f));
            paint.setColor(withAlpha(0xFFFFFFFF, Math.round((1f - flashProgress) * 180f)));
            canvas.drawCircle(burstX, burstY, dp(12f + flashProgress * 76f), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(0xFFFFE8A3, Math.round((1f - flashProgress) * 110f)));
            canvas.drawCircle(burstX, burstY, dp(10f + flashProgress * 28f), paint);
        }

        if (!particlesInitialized) {
            initializeParticles();
        }
        for (Particle particle : particles) {
            float life = burstElapsedMs / (float) particle.lifetimeMs;
            if (life < 0f || life >= 1f) {
                continue;
            }
            float distanceSeconds = Math.max(0f, burstSeconds);
            float x = burstX + (float) Math.cos(particle.angle) * particle.speed * distanceSeconds;
            float y = burstY
                    + (float) Math.sin(particle.angle) * particle.speed * distanceSeconds
                    + 0.5f * particle.gravity * distanceSeconds * distanceSeconds;
            float alphaFraction = (1f - life) * (1f - life * 0.2f);
            int alpha = Math.max(0, Math.min(255, Math.round(255f * alphaFraction)));

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(withAlpha(particle.color, Math.round(alpha * 0.28f)));
            canvas.drawCircle(x, y, particle.radius * 2.2f, paint);
            paint.setColor(withAlpha(particle.color, alpha));
            canvas.drawCircle(x, y, particle.radius, paint);

            if (particle.sparkle && life < 0.72f) {
                float sparkleAlpha = alpha * (1f - life / 0.72f);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1.2f));
                paint.setColor(withAlpha(Color.WHITE, Math.round(sparkleAlpha)));
                float arm = particle.radius * 2.6f;
                canvas.drawLine(x - arm, y, x + arm, y, paint);
                canvas.drawLine(x, y - arm, x, y + arm, paint);
            }
        }
    }

    private void initializeParticles() {
        if (particlesInitialized) {
            return;
        }
        Random random = new Random(SystemClock.uptimeMillis());
        for (int i = 0; i < particles.length; i++) {
            Particle particle = new Particle();
            particle.angle = (float) (random.nextFloat() * Math.PI * 2d);
            particle.speed = dp(230f + random.nextFloat() * 500f);
            particle.gravity = dp(170f + random.nextFloat() * 250f);
            particle.radius = dp(1.4f + random.nextFloat() * 2.5f);
            particle.lifetimeMs = 720L + random.nextInt(620);
            particle.color = random.nextInt(5) == 0
                    ? accentColor
                    : PARTICLE_COLORS[random.nextInt(PARTICLE_COLORS.length)];
            particle.sparkle = random.nextFloat() < 0.28f;
            particles[i] = particle;
        }
        particlesInitialized = true;
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)),
                Color.red(color), Color.green(color), Color.blue(color));
    }

    private float dp(float value) {
        return value * density;
    }

    private void finishAndRemove() {
        if (finished) {
            return;
        }
        finished = true;
        onFinished.run();
        if (!detaching) {
            ViewParent parent = getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(this);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        detaching = true;
        if (animator.isStarted()) {
            animator.cancel();
        }
        if (!finished) {
            finishAndRemove();
        }
        super.onDetachedFromWindow();
    }

    private static final class Particle {
        float angle;
        float speed;
        float gravity;
        float radius;
        long lifetimeMs;
        int color;
        boolean sparkle;
    }
}
