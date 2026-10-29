package com.example.bivoca.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.bivoca.R;
import com.example.bivoca.data.model.LoginResponse;
import com.example.bivoca.data.repository.AuthRepository;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class RegisterViewModel extends ViewModel {

    public enum Field { FULL_NAME, USERNAME, EMAIL, PASSWORD, CONFIRM_PASSWORD }

    public static final class ValidationIssue {
        public final Field field;
        @StringRes public final int message;

        ValidationIssue(Field field, @StringRes int message) {
            this.field = field;
            this.message = message;
        }
    }

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,30}");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AuthRepository repository;
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<LoginResponse> registerResult = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<ValidationIssue> validationIssue = new MutableLiveData<>();

    public RegisterViewModel() { this(new AuthRepository()); }

    RegisterViewModel(AuthRepository repository) { this.repository = repository; }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<LoginResponse> getRegisterResult() { return registerResult; }
    public LiveData<String> getError() { return error; }
    public LiveData<ValidationIssue> getValidationIssue() { return validationIssue; }

    public void clearMessages() {
        if (validationIssue.getValue() != null) validationIssue.setValue(null);
        if (error.getValue() != null) error.setValue(null);
    }

    static ValidationIssue validate(String fullName, String username, String email,
                                    String password, String confirmPassword) {
        String name = fullName == null ? "" : fullName.trim();
        String user = username == null ? "" : username.trim();
        String address = email == null ? "" : email.trim();
        if (name.length() > 100) return new ValidationIssue(Field.FULL_NAME, R.string.register_name_too_long);
        if (user.isEmpty()) return new ValidationIssue(Field.USERNAME, R.string.register_username_required);
        if (!USERNAME.matcher(user).matches())
            return new ValidationIssue(Field.USERNAME, R.string.register_username_invalid);
        if (address.isEmpty()) return new ValidationIssue(Field.EMAIL, R.string.register_email_required);
        if (address.length() > 150 || !EMAIL.matcher(address).matches())
            return new ValidationIssue(Field.EMAIL, R.string.register_email_invalid);
        if (password == null || password.length() < 8
                || password.codePointCount(0, password.length()) < 8)
            return new ValidationIssue(Field.PASSWORD, R.string.register_password_short);
        if (password.length() > 72 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            return new ValidationIssue(Field.PASSWORD, R.string.register_password_long);
        if (!password.equals(confirmPassword))
            return new ValidationIssue(Field.CONFIRM_PASSWORD, R.string.register_password_mismatch);
        return null;
    }

    public void register(String fullName, String username, String email,
                         String password, String confirmPassword) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        clearMessages();
        ValidationIssue issue = validate(fullName, username, email, password, confirmPassword);
        if (issue != null) {
            validationIssue.setValue(issue);
            return;
        }
        loading.setValue(true);
        repository.register(username.trim(), email.trim(), password,
                fullName == null || fullName.trim().isEmpty() ? null : fullName.trim(),
                new AuthRepository.RegisterCallback() {
                    @Override public void onSuccess(LoginResponse response) {
                        loading.setValue(false);
                        registerResult.setValue(response);
                    }

                    @Override public void onError(String message) {
                        loading.setValue(false);
                        error.setValue(message);
                    }
                });
    }
}
