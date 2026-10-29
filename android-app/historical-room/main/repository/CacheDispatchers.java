package com.example.bivoca.data.repository;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Serializes cache work off the UI thread; Retrofit/UI callbacks stay on main. */
final class CacheDispatchers {
    static final Executor DISK = Executors.newSingleThreadExecutor();
    private CacheDispatchers() { }
    static Executor main() {
        Handler handler = new Handler(Looper.getMainLooper());
        return command -> handler.post(command);
    }
}
