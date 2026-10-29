package com.example.bivoca.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.util.Patterns;

import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.repository.AuthRepository;

public class LoginViewModel extends ViewModel {

    private final AuthRepository repository =
            new AuthRepository();

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);

    private final MutableLiveData<LoginResponse> loginResult =
            new MutableLiveData<>();

    private final MutableLiveData<String> error =
            new MutableLiveData<>();
    private final MutableLiveData<GoogleChallengeResponse> googlePrompt = new MutableLiveData<>();
    private GoogleChallengeResponse pendingGoogleChallenge;

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<LoginResponse> getLoginResult() {
        return loginResult;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<GoogleChallengeResponse> getGooglePrompt() { return googlePrompt; }

    public void startGoogleSignIn() {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        repository.requestGoogleChallenge(new AuthRepository.ResultCallback<GoogleChallengeResponse>() {
            @Override public void onSuccess(GoogleChallengeResponse value) {
                pendingGoogleChallenge = value;
                googlePrompt.setValue(value);
            }
            @Override public void onError(String message) {
                loading.setValue(false); error.setValue(message);
            }
        });
    }

    public void consumeGooglePrompt() { googlePrompt.setValue(null); }

    public void completeGoogleSignIn(String idToken) {
        GoogleChallengeResponse challenge = pendingGoogleChallenge;
        if (challenge == null) { loading.setValue(false); error.setValue("Phiên đăng nhập Google đã hết hạn."); return; }
        repository.loginWithGoogle(idToken, challenge, new AuthRepository.LoginCallback() {
            @Override public void onSuccess(LoginResponse response) {
                pendingGoogleChallenge = null; loading.setValue(false); loginResult.setValue(response);
            }
            @Override public void onError(String message) {
                pendingGoogleChallenge = null; loading.setValue(false); error.setValue(message);
            }
        });
    }

    public void cancelGoogleSignIn() {
        pendingGoogleChallenge = null; loading.setValue(false);
    }

    public void login(
            String email,
            String password
    ) {

        if (Boolean.TRUE.equals(loading.getValue())) {
            return;
        }

        if (email == null || email.trim().isEmpty()) {

            error.setValue("Vui lòng nhập email.");
            return;
        }

        if (password == null || password.isEmpty()) {

            error.setValue("Vui lòng nhập mật khẩu.");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            error.setValue("Email không hợp lệ.");
            return;
        }

        loading.setValue(true);

        repository.login(
                email.trim(),
                password,
                new AuthRepository.LoginCallback() {

                    @Override
                    public void onSuccess(
                            LoginResponse response
                    ) {

                        loading.setValue(false);
                        loginResult.setValue(response);
                    }

                    @Override
                    public void onError(
                            String message
                    ) {

                        loading.setValue(false);
                        error.setValue(message);
                    }
                }
        );
    }
}
