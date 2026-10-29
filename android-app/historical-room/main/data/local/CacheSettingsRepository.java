package com.example.bivoca.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Reads and clears only the M8 public learning cache. */
public final class CacheSettingsRepository {
    public interface Callback {
        void onSuccess(CacheStats stats);
        void onError(String message);
    }

    private static final String DATABASE_NAME = "bivoca-learning-cache.db";
    private final Context context;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public CacheSettingsRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void load(Callback callback) {
        executor.execute(() -> run(false, callback));
    }

    public void clearPublicCache(Callback callback) {
        executor.execute(() -> run(true, callback));
    }

    public void close() {
        executor.shutdownNow();
    }

    private void run(boolean clear, Callback callback) {
        try {
            LearningCacheDao dao = LearningDatabase.getInstance(context).learningCacheDao();
            if (clear) dao.clearPublicCache();
            CacheStats stats = new CacheStats(dao.countTopics(), dao.countFlashcards(), cacheBytes(context));
            main.post(() -> callback.onSuccess(stats));
        } catch (RuntimeException error) {
            main.post(() -> callback.onError("Không thể đọc dữ liệu ngoại tuyến trên thiết bị."));
        }
    }

    static long cacheBytes(Context context) {
        File file = context.getDatabasePath(DATABASE_NAME);
        return length(file) + length(new File(file.getPath() + "-wal"))
                + length(new File(file.getPath() + "-shm"));
    }

    private static long length(File file) {
        return file.exists() && file.isFile() ? file.length() : 0L;
    }
}
