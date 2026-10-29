package com.example.bivoca.ui.auth;

import android.util.Patterns;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.R;
import com.example.bivoca.data.repository.AuthRepository;

public class ForgotPasswordViewModel extends ViewModel {

    public static final class Feedback {
        @StringRes public final int message;
        public final String messageText;
        public final boolean validationError;

        Feedback(@StringRes int message, boolean validationError) {
            this.message = message;
            this.messageText = null;
            this.validationError = validationError;
        }
        Feedback(String messageText, boolean validationError) {
            this.message = 0; this.messageText = messageText; this.validationError = validationError;
        }
    }

    private final MutableLiveData<Feedback> feedback = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final AuthRepository repository = new AuthRepository();

    public LiveData<Feedback> getFeedback() {
        return feedback;
    }
    public LiveData<Boolean> getLoading() { return loading; }

    public void submit(String email) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        String value = email == null ? "" : email.trim();
        if (value.isEmpty()) {
            feedback.setValue(new Feedback(R.string.forgot_email_required, true));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
            feedback.setValue(new Feedback(R.string.forgot_email_invalid, true));
            return;
        }
        loading.setValue(true);
        repository.forgotPassword(value, new AuthRepository.ResultCallback<String>() {
            @Override public void onSuccess(String message) {
                loading.setValue(false); feedback.setValue(new Feedback(message, false));
            }
            @Override public void onError(String message) {
                loading.setValue(false); feedback.setValue(new Feedback(message, true));
            }
        });
    }

    public void clearFeedback() {
        feedback.setValue(null);
    }
}
