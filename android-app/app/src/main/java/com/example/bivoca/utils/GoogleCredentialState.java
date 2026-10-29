package com.example.bivoca.utils;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

/** Keeps Credential Manager's provider state in sync after a BiVoca logout. */
public final class GoogleCredentialState {
    private GoogleCredentialState() {}

    public static void clearAfterLogout(Context context) {
        Context appContext = context.getApplicationContext();
        CredentialManager.create(appContext).clearCredentialStateAsync(
                new ClearCredentialStateRequest(),
                new CancellationSignal(),
                ContextCompat.getMainExecutor(appContext),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void result) { }
                    @Override public void onError(ClearCredentialException exception) { }
                });
    }
}
