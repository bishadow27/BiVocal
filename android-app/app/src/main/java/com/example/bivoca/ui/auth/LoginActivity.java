package com.example.bivoca.ui.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.CancellationSignal;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.example.bivoca.BuildConfig;
import com.example.bivoca.R;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.utils.SessionManager;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class LoginActivity extends AppCompatActivity {

    private static final String STATE_PASSWORD_VISIBLE = "password_visible";

    private EditText editEmail;
    private EditText editPassword;

    private TextView textError;
    private TextView textRegister;
    private TextView textForgotPassword;
    private ImageButton buttonTogglePassword;
    private boolean passwordVisible;

    private Button buttonLogin;
    private Button buttonGoogleSignIn;

    private ProgressBar progressBar;

    private LoginViewModel viewModel;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager =
                new SessionManager(this);

        if (sessionManager.isLoggedIn()) {

            openHome();

            return;
        }

        setContentView(
                R.layout.activity_login
        );

        // Crop only the mascot from the official 414 x 172 combined logo.
        // Keep its aspect ratio and the original asset used by the other screens.
        Bitmap wordmark = BitmapFactory.decodeResource(getResources(), R.drawable.bivocal_wordmark);
        Bitmap mascot = Bitmap.createBitmap(wordmark, 0, 0,
                Math.round(wordmark.getWidth() * 128f / 414f), wordmark.getHeight());
        ((ImageView) findViewById(R.id.loginMascot)).setImageBitmap(mascot);

        View root = findViewById(R.id.loginRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right,
                    Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        editEmail =
                findViewById(R.id.editEmail);

        editPassword =
                findViewById(R.id.editPassword);

        textError =
                findViewById(R.id.textError);

        textRegister =
                findViewById(R.id.textRegister);

        textForgotPassword = findViewById(R.id.textForgotPassword);
        buttonTogglePassword = findViewById(R.id.buttonTogglePassword);
        passwordVisible = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_PASSWORD_VISIBLE, false);
        renderPasswordVisibility();

        editEmail.setOnFocusChangeListener((view, focused) ->
                findViewById(R.id.emailFieldContainer).setSelected(focused));
        editPassword.setOnFocusChangeListener((view, focused) ->
                findViewById(R.id.passwordFieldContainer).setSelected(focused));

        buttonLogin =
                findViewById(R.id.buttonLogin);
        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn);

        progressBar =
                findViewById(R.id.progressBar);

        viewModel =
                new ViewModelProvider(this)
                        .get(LoginViewModel.class);

        observeViewModel();

        buttonLogin.setOnClickListener(
                view -> login()
        );
        buttonGoogleSignIn.setOnClickListener(view -> {
            textError.setVisibility(View.GONE);
            viewModel.startGoogleSignIn();
        });

        textRegister.setOnClickListener(
                view -> openRegister()
        );

        textForgotPassword.setOnClickListener(view -> startActivity(
                new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        buttonTogglePassword.setOnClickListener(view -> {
            passwordVisible = !passwordVisible;
            renderPasswordVisibility();
        });
        editPassword.setOnEditorActionListener((view, actionId, event) -> {
            login();
            return true;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_PASSWORD_VISIBLE, passwordVisible);
        super.onSaveInstanceState(outState);
    }

    private void renderPasswordVisibility() {
        int cursor = editPassword.getSelectionStart();
        editPassword.setTransformationMethod(passwordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
        if (cursor >= 0 && cursor <= editPassword.length()) editPassword.setSelection(cursor);
        buttonTogglePassword.setImageResource(passwordVisible
                ? R.drawable.login_eye_off : R.drawable.login_eye);
        buttonTogglePassword.setContentDescription(getString(passwordVisible
                ? R.string.login_hide_password : R.string.login_show_password));
    }

    private void login() {

        textError.setVisibility(
                View.GONE
        );

        viewModel.login(

                editEmail
                        .getText()
                        .toString(),

                editPassword
                        .getText()
                        .toString()
        );
    }

    private void observeViewModel() {

        viewModel
                .getLoading()
                .observe(this, loading -> {

                    boolean isLoading =
                            Boolean.TRUE.equals(
                                    loading
                            );

                    progressBar.setVisibility(
                            isLoading
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    buttonLogin.setEnabled(
                            !isLoading
                    );
                    buttonGoogleSignIn.setEnabled(!isLoading);
                    buttonLogin.setText(isLoading ? R.string.login_loading : R.string.login_button);
                });

        viewModel.getGooglePrompt().observe(this, challenge -> {
            if (challenge == null) return;
            viewModel.consumeGooglePrompt();
            openGoogleAccountChooser(challenge);
        });

        viewModel
                .getLoginResult()
                .observe(this, response -> {

                    if (response == null) {
                        return;
                    }

                    sessionManager.saveLogin(
                            response
                    );

                    Toast.makeText(
                            LoginActivity.this,
                            "Đăng nhập thành công",
                            Toast.LENGTH_SHORT
                    ).show();

                    openHome();
                });

        viewModel
                .getError()
                .observe(this, message -> {

                    if (message == null
                            || message.isEmpty()) {

                        return;
                    }

                    textError.setText(
                            message
                    );

                    textError.setVisibility(
                            View.VISIBLE
                    );
                });
    }

    private void openGoogleAccountChooser(GoogleChallengeResponse challenge) {
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setNonce(challenge.getNonce())
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build();
        CredentialManager.create(this).getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        if (!(response.getCredential() instanceof CustomCredential)) {
                            failGoogleSignIn(getString(R.string.login_google_unavailable));
                            return;
                        }
                        CustomCredential credential = (CustomCredential) response.getCredential();
                        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                .equals(credential.getType())) {
                            failGoogleSignIn(getString(R.string.login_google_unavailable));
                            return;
                        }
                        try {
                            GoogleIdTokenCredential googleCredential =
                                    GoogleIdTokenCredential.createFrom(credential.getData());
                            viewModel.completeGoogleSignIn(googleCredential.getIdToken());
                        } catch (RuntimeException exception) {
                            failGoogleSignIn(getString(R.string.login_google_unavailable));
                        }
                    }

                    @Override
                    public void onError(GetCredentialException exception) {
                        if (exception instanceof GetCredentialCancellationException) {
                            failGoogleSignIn(getString(R.string.login_google_cancelled));
                        } else if (exception instanceof NoCredentialException) {
                            failGoogleSignIn(getString(R.string.login_google_no_account));
                        } else {
                            failGoogleSignIn(getString(R.string.login_google_unavailable));
                        }
                    }
                });
    }

    private void failGoogleSignIn(String message) {
        viewModel.cancelGoogleSignIn();
        textError.setText(message);
        textError.setVisibility(View.VISIBLE);
    }

    private void openRegister() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        RegisterActivity.class
                );

        startActivity(intent);
    }

    private void openHome() {

        Intent intent =
                new Intent(
                        LoginActivity.this,
                        HomeActivity.class
                );

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);

        finish();
    }
}
