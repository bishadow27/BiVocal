package com.example.bivoca.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.example.bivoca.ui.auth.DangNhapActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/** Central handling for an authenticated request rejected with HTTP 401. */
public final class AuthSessionExpiry {

    private static final AtomicBoolean REDIRECTING_TO_LOGIN = new AtomicBoolean(false);

    private AuthSessionExpiry() {
    }

    public static void handleRejectedToken(Context context, SessionManager sessionManager,
                                           String rejectedToken) {
        if (!sessionManager.clearSessionIfTokenMatches(rejectedToken)
                || !REDIRECTING_TO_LOGIN.compareAndSet(false, true)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(() -> {
            if (sessionManager.isLoggedIn()) {
                REDIRECTING_TO_LOGIN.set(false);
                return;
            }
            Intent intent = new Intent(appContext, DangNhapActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        });
    }

    public static void reset() {
        REDIRECTING_TO_LOGIN.set(false);
    }
}
