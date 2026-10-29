package com.example.bivoca.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.R;

public class RegisterActivity extends AppCompatActivity {

    private static final String STATE_PASSWORD_VISIBLE = "register_password_visible";
    private static final String STATE_CONFIRM_VISIBLE = "register_confirm_visible";

    private EditText editFullName;
    private EditText editUsername;
    private EditText editEmail;
    private EditText editPassword;
    private EditText editConfirmPassword;
    private TextView textError;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private ImageButton togglePassword;
    private ImageButton toggleConfirm;
    private RegisterViewModel viewModel;
    private boolean passwordVisible;
    private boolean confirmVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Bitmap wordmark = BitmapFactory.decodeResource(getResources(), R.drawable.bivocal_wordmark);
        Bitmap mascot = Bitmap.createBitmap(wordmark, 0, 0,
                Math.round(wordmark.getWidth() * 128f / 414f), wordmark.getHeight());
        ((ImageView) findViewById(R.id.authMascot)).setImageBitmap(mascot);
        ((TextView) findViewById(R.id.authScreenTitle)).setText(R.string.auth_register_title);

        View root = findViewById(R.id.registerRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        editFullName = findViewById(R.id.editFullName);
        editUsername = findViewById(R.id.editUsername);
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        textError = findViewById(R.id.textError);
        buttonRegister = findViewById(R.id.buttonRegister);
        progressBar = findViewById(R.id.progressBar);
        togglePassword = findViewById(R.id.registerTogglePassword);
        toggleConfirm = findViewById(R.id.registerToggleConfirm);

        passwordVisible = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PASSWORD_VISIBLE);
        confirmVisible = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_CONFIRM_VISIBLE);
        renderPasswordVisibility(editPassword, togglePassword, passwordVisible);
        renderPasswordVisibility(editConfirmPassword, toggleConfirm, confirmVisible);

        for (RegisterViewModel.Field field : RegisterViewModel.Field.values()) {
            EditText input = inputFor(field);
            input.setOnFocusChangeListener((view, focused) -> updateFieldBorder(field));
            input.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                    clearFieldError(field);
                    textError.setVisibility(View.GONE);
                    if (viewModel != null) viewModel.clearMessages();
                }
                @Override public void afterTextChanged(Editable text) { }
            });
        }

        viewModel = new ViewModelProvider(this).get(RegisterViewModel.class);
        observeViewModel();
        buttonRegister.setOnClickListener(view -> register());
        findViewById(R.id.textLogin).setOnClickListener(view -> openLogin());
        togglePassword.setOnClickListener(view -> {
            passwordVisible = !passwordVisible;
            renderPasswordVisibility(editPassword, togglePassword, passwordVisible);
        });
        toggleConfirm.setOnClickListener(view -> {
            confirmVisible = !confirmVisible;
            renderPasswordVisibility(editConfirmPassword, toggleConfirm, confirmVisible);
        });
        editConfirmPassword.setOnEditorActionListener((view, actionId, event) -> {
            register();
            return true;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PASSWORD_VISIBLE, passwordVisible);
        outState.putBoolean(STATE_CONFIRM_VISIBLE, confirmVisible);
        super.onSaveInstanceState(outState);
    }

    private void renderPasswordVisibility(EditText input, ImageButton toggle, boolean visible) {
        int cursor = input.getSelectionStart();
        input.setTransformationMethod(visible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        if (cursor >= 0 && cursor <= input.length()) input.setSelection(cursor);
        toggle.setImageResource(visible ? R.drawable.login_eye_off : R.drawable.login_eye);
        toggle.setContentDescription(getString(visible
                ? R.string.login_hide_password : R.string.login_show_password));
    }

    private EditText inputFor(RegisterViewModel.Field field) {
        switch (field) {
            case FULL_NAME: return editFullName;
            case USERNAME: return editUsername;
            case EMAIL: return editEmail;
            case PASSWORD: return editPassword;
            case CONFIRM_PASSWORD: return editConfirmPassword;
            default: throw new IllegalArgumentException();
        }
    }

    private TextView errorFor(RegisterViewModel.Field field) {
        switch (field) {
            case FULL_NAME: return findViewById(R.id.registerFullNameError);
            case USERNAME: return findViewById(R.id.registerUsernameError);
            case EMAIL: return findViewById(R.id.registerEmailError);
            case PASSWORD: return findViewById(R.id.registerPasswordError);
            case CONFIRM_PASSWORD: return findViewById(R.id.registerConfirmError);
            default: throw new IllegalArgumentException();
        }
    }

    private View containerFor(RegisterViewModel.Field field) {
        switch (field) {
            case FULL_NAME: return findViewById(R.id.registerFullNameContainer);
            case USERNAME: return findViewById(R.id.registerUsernameContainer);
            case EMAIL: return findViewById(R.id.registerEmailContainer);
            case PASSWORD: return findViewById(R.id.registerPasswordContainer);
            case CONFIRM_PASSWORD: return findViewById(R.id.registerConfirmContainer);
            default: throw new IllegalArgumentException();
        }
    }

    private void updateFieldBorder(RegisterViewModel.Field field) {
        View container = containerFor(field);
        if (errorFor(field).getVisibility() == View.VISIBLE) {
            container.setBackgroundResource(R.drawable.register_field_error);
        } else {
            container.setBackgroundResource(R.drawable.login_ui_field);
            container.setSelected(inputFor(field).hasFocus());
        }
    }

    private void clearFieldError(RegisterViewModel.Field field) {
        errorFor(field).setVisibility(View.GONE);
        updateFieldBorder(field);
    }

    private void register() {
        for (RegisterViewModel.Field field : RegisterViewModel.Field.values()) clearFieldError(field);
        textError.setVisibility(View.GONE);
        viewModel.register(editFullName.getText().toString(), editUsername.getText().toString(),
                editEmail.getText().toString(), editPassword.getText().toString(),
                editConfirmPassword.getText().toString());
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean busy = Boolean.TRUE.equals(loading);
            progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
            buttonRegister.setEnabled(!busy);
            buttonRegister.setText(busy ? R.string.auth_register_loading : R.string.auth_register_button);
        });
        viewModel.getValidationIssue().observe(this, issue -> {
            if (issue == null) return;
            TextView error = errorFor(issue.field);
            error.setText(issue.message);
            error.setVisibility(View.VISIBLE);
            updateFieldBorder(issue.field);
            inputFor(issue.field).requestFocus();
        });
        viewModel.getError().observe(this, message -> {
            if (message == null || message.isEmpty()) return;
            textError.setText(message);
            textError.setVisibility(View.VISIBLE);
        });
        viewModel.getRegisterResult().observe(this, response -> {
            if (response == null) return;
            Toast.makeText(this, R.string.register_success, Toast.LENGTH_LONG).show();
            openLogin();
        });
    }

    private void openLogin() {
        if (isTaskRoot()) startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
