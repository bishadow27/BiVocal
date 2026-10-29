package com.example.bivoca.data.repository;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;
import com.google.gson.stream.MalformedJsonException;

import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Shared execution policy for the two public learning repositories, not a data source. */
final class NetworkFirstLoader<T> {
    interface Result<T> {
        void success(List<T> values, boolean fromCache);
        void error(String message);
    }

    private final Executor disk;
    private final Executor main;
    private final Predicate<List<T>> valid;
    private final String contentName;

    NetworkFirstLoader(Executor disk, Executor main, Predicate<List<T>> valid, String contentName) {
        this.disk = disk;
        this.main = main;
        this.valid = valid;
        this.contentName = contentName;
    }

    Call<List<T>> load(Call<List<T>> call, Supplier<List<T>> read,
                       Consumer<List<T>> write, Result<T> result) {
        call.enqueue(new Callback<List<T>>() {
            @Override public void onResponse(@NonNull Call<List<T>> request,
                                              @NonNull Response<List<T>> response) {
                if (request.isCanceled()) {
                    if (response.errorBody() != null) response.errorBody().close();
                    return;
                }
                if (response.isSuccessful()) {
                    List<T> body = response.body();
                    if (body == null || !valid.test(body)) {
                        deliver(request, () -> result.error(invalidData()));
                        return;
                    }
                    List<T> snapshot = immutable(body);
                    disk.execute(() -> {
                        if (request.isCanceled()) return;
                        try {
                            write.accept(snapshot);
                        } catch (RuntimeException cacheFailure) {
                            // Cache is optional. A valid API response must still reach the learner.
                        }
                        deliver(request, () -> result.success(snapshot, false));
                    });
                    return;
                }
                if (response.errorBody() != null) response.errorBody().close();
                if (isTemporaryServerError(response.code())) {
                    fallback(request, read, result);
                } else {
                    // Business/auth errors must not be hidden by an older local snapshot.
                    deliver(request, () -> result.error("Không tải được " + contentName
                            + " (HTTP " + response.code() + "). Hãy thử lại hoặc chọn chủ đề khác."));
                }
            }

            @Override public void onFailure(@NonNull Call<List<T>> request,
                                             @NonNull Throwable failure) {
                if (request.isCanceled()) return;
                if (isConnectionFailure(failure)) fallback(request, read, result);
                else deliver(request, () -> result.error(invalidData()));
            }
        });
        return call;
    }

    private void fallback(Call<List<T>> request, Supplier<List<T>> read, Result<T> result) {
        disk.execute(() -> {
            if (request.isCanceled()) return;
            final List<T> cached;
            try {
                cached = read.get();
            } catch (RuntimeException cacheFailure) {
                deliver(request, () -> result.error("Không kết nối được máy chủ và không đọc được bộ nhớ đệm. Hãy kết nối lại và thử lại."));
                return;
            }
            if (cached == null) {
                deliver(request, () -> result.error("Không kết nối được máy chủ. Chưa có "
                        + contentName + " được lưu trên thiết bị. Hãy kết nối mạng và thử lại."));
            } else if (!valid.test(cached)) {
                deliver(request, () -> result.error("Dữ liệu đã lưu không hợp lệ. Hãy kết nối mạng và tải lại."));
            } else {
                List<T> snapshot = immutable(cached);
                deliver(request, () -> result.success(snapshot, true));
            }
        });
    }

    private void deliver(Call<List<T>> call, Runnable callback) {
        main.execute(() -> { if (!call.isCanceled()) callback.run(); });
    }

    private String invalidData() {
        return "Dữ liệu " + contentName + " từ máy chủ không hợp lệ. Hãy thử lại.";
    }

    private List<T> immutable(List<T> values) {
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    static boolean isTemporaryServerError(int code) {
        return code == 500 || code == 502 || code == 503 || code == 504;
    }

    static boolean isConnectionFailure(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonParseException || cause instanceof MalformedJsonException
                    || cause instanceof EOFException || cause instanceof ProtocolException
                    || cause instanceof SSLException) return false;
        }
        return failure instanceof IOException;
    }
}
