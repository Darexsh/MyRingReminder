package com.darexsh.myringreminder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class BackgroundImageLoader {
    interface Callback {
        void onLoaded(@Nullable Bitmap bitmap);
    }

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            task.run();
        }, "BackgroundImageLoader");
        thread.setDaemon(true);
        return thread;
    });
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final int CACHE_SIZE_KB = (int) Math.max(1024L,
            Runtime.getRuntime().maxMemory() / 1024L / 8L);
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_SIZE_KB) {
        @Override
        protected int sizeOf(@NonNull String key, @NonNull Bitmap bitmap) {
            return bitmap.getAllocationByteCount() / 1024;
        }
    };

    private BackgroundImageLoader() {
    }

    static void load(@NonNull Context context,
                     @NonNull Uri uri,
                     @NonNull ImageView target,
                     @NonNull Callback callback) {
        Object requestToken = new Object();
        target.setTag(requestToken);
        int targetWidth = Math.max(1, target.getWidth());
        int targetHeight = Math.max(1, target.getHeight());
        if (target.getWidth() <= 0 || target.getHeight() <= 0) {
            android.util.DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            targetWidth = Math.max(1, metrics.widthPixels);
            targetHeight = Math.max(1, metrics.heightPixels);
        }
        String cacheKey = uri + "@" + targetWidth + "x" + targetHeight;
        Bitmap cached = CACHE.get(cacheKey);
        if (cached != null) {
            deliver(target, requestToken, cached, callback);
            return;
        }

        Context appContext = context.getApplicationContext();
        int requestedWidth = targetWidth;
        int requestedHeight = targetHeight;
        EXECUTOR.execute(() -> {
            Bitmap bitmap = CACHE.get(cacheKey);
            if (bitmap == null) {
                bitmap = decode(appContext, uri, requestedWidth, requestedHeight);
            }
            if (bitmap != null) {
                CACHE.put(cacheKey, bitmap);
            }
            deliver(target, requestToken, bitmap, callback);
        });
    }

    static void cancel(@NonNull ImageView target) {
        target.setTag(null);
    }

    private static void deliver(ImageView target, Object requestToken,
                                @Nullable Bitmap bitmap, Callback callback) {
        MAIN_HANDLER.post(() -> {
            if (target.getTag() == requestToken) {
                callback.onLoaded(bitmap);
            }
        });
    }

    @Nullable
    private static Bitmap decode(Context context, Uri uri, int targetWidth, int targetHeight) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream input = open(context, uri)) {
            if (input == null) {
                return null;
            }
            BitmapFactory.decodeStream(input, null, bounds);
        } catch (IOException | SecurityException ignored) {
            return null;
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (bounds.outWidth / (options.inSampleSize * 2) >= targetWidth
                && bounds.outHeight / (options.inSampleSize * 2) >= targetHeight) {
            options.inSampleSize *= 2;
        }
        try (InputStream input = open(context, uri)) {
            return input != null ? BitmapFactory.decodeStream(input, null, options) : null;
        } catch (IOException | SecurityException ignored) {
            return null;
        }
    }

    @Nullable
    private static InputStream open(Context context, Uri uri) throws IOException {
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            File file = new File(path);
            return file.isFile() ? new FileInputStream(file) : null;
        }
        return context.getContentResolver().openInputStream(uri);
    }
}
