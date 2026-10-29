package com.example.bivoca.ui.profile;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.InputType;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.example.bivoca.BuildConfig;
import com.example.bivoca.R;
import com.example.bivoca.data.model.GamificationStats;
import com.example.bivoca.data.model.GoogleChallengeResponse;
import com.example.bivoca.data.model.User;
import com.example.bivoca.ui.auth.LoginActivity;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.utils.GoogleCredentialState;
import com.example.bivoca.utils.LocalAvatarStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class ProfileActivity extends AppCompatActivity {

    private static final String STATE_PENDING_AVATAR = "pending_avatar_uri";

    private ProfileViewModel viewModel;
    private SessionManager sessionManager;

    private ProgressBar progressProfile;
    private LinearLayout errorContainer;
    private TextView textProfileError;
    private MaterialButton buttonProfileRetry;
    private ScrollView scrollProfileContent;

    private TextView textAvatarLetter;
    private ImageView imageProfileAvatar;
    private ImageView imageAvatarPreview;
    private NavigationDrawerController navigationController;
    private View avatarPreviewContainer;
    private MaterialButton buttonChooseAvatar;
    private MaterialButton buttonResetAvatar;
    private Uri pendingAvatarUri;
    private Bitmap pendingAvatarBitmap;
    private LocalAvatarStore avatarStore;
    private final ActivityResultLauncher<String[]> avatarPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::onAvatarPicked);
    private TextView textFullName;
    private TextView textUsername;
    private TextView textEmail;
    private TextView textLevelBadge;

    private TextView textTotalXp;
    private TextView textCurrentStreak;
    private TextView textLongestStreak;
    private MaterialButton buttonProfileLogout;
    private MaterialButton buttonLinkGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        avatarStore = new LocalAvatarStore(this);
        if (!sessionManager.isLoggedIn()) {
            openLogin();
            return;
        }

        setContentView(R.layout.activity_profile);

        // Bind Views
        progressProfile = findViewById(R.id.progressProfile);
        errorContainer = findViewById(R.id.errorContainer);
        textProfileError = findViewById(R.id.textProfileError);
        buttonProfileRetry = findViewById(R.id.buttonProfileRetry);
        scrollProfileContent = findViewById(R.id.scrollProfileContent);

        textAvatarLetter = findViewById(R.id.textAvatarLetter);
        imageProfileAvatar = findViewById(R.id.imageProfileAvatar);
        imageAvatarPreview = findViewById(R.id.imageAvatarPreview);
        avatarPreviewContainer = findViewById(R.id.avatarPreviewContainer);
        buttonChooseAvatar = findViewById(R.id.buttonChooseAvatar);
        buttonResetAvatar = findViewById(R.id.buttonResetAvatar);
        textFullName = findViewById(R.id.textFullName);
        textUsername = findViewById(R.id.textUsername);
        textEmail = findViewById(R.id.textEmail);
        textLevelBadge = findViewById(R.id.textLevelBadge);

        textTotalXp = findViewById(R.id.textTotalXp);
        textCurrentStreak = findViewById(R.id.textCurrentStreak);
        textLongestStreak = findViewById(R.id.textLongestStreak);
        buttonProfileLogout = findViewById(R.id.buttonProfileLogout);
        buttonLinkGoogle = findViewById(R.id.buttonLinkGoogle);

        navigationController = NavigationDrawerController.attach(
                this,
                NavigationRoute.PROFILE,
                R.id.buttonProfileBack
        );
        buttonProfileRetry.setOnClickListener(v -> viewModel.loadProfile());
        buttonProfileLogout.setOnClickListener(v -> logout());
        buttonLinkGoogle.setOnClickListener(v -> showGoogleLinkDialog());
        buttonChooseAvatar.setOnClickListener(v -> avatarPicker.launch(new String[]{"image/*"}));
        findViewById(R.id.buttonCancelAvatar).setOnClickListener(v -> clearAvatarPreview());
        findViewById(R.id.buttonConfirmAvatar).setOnClickListener(v -> confirmAvatar());
        buttonResetAvatar.setOnClickListener(v -> resetAvatar());

        if (savedInstanceState != null) {
            String pending = savedInstanceState.getString(STATE_PENDING_AVATAR);
            if (pending != null) showAvatarPreview(Uri.parse(pending));
        }

        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        observeViewModel();

        viewModel.loadProfile();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(this, loading -> {
            boolean isLoading = Boolean.TRUE.equals(loading);
            progressProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (isLoading) {
                errorContainer.setVisibility(View.GONE);
                scrollProfileContent.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                textProfileError.setText(errorMsg);
                errorContainer.setVisibility(View.VISIBLE);
                scrollProfileContent.setVisibility(View.GONE);
            }
        });

        viewModel.getUser().observe(this, user -> {
            if (user != null) {
                renderUser(user);
                scrollProfileContent.setVisibility(View.VISIBLE);
                errorContainer.setVisibility(View.GONE);
            }
        });

        viewModel.getStats().observe(this, stats -> {
            if (stats != null) {
                renderStats(stats);
            }
        });
        viewModel.getGoogleLinkLoading().observe(this, loading ->
                buttonLinkGoogle.setEnabled(!Boolean.TRUE.equals(loading)));
        viewModel.getGoogleLinkPrompt().observe(this, challenge -> {
            if (challenge == null) return;
            viewModel.consumeGoogleLinkPrompt();
            openGoogleLinkChooser(challenge);
        });
        viewModel.getGoogleLinkResult().observe(this, response -> {
            if (response == null) return;
            sessionManager.saveLogin(response);
            Toast.makeText(this, R.string.profile_link_google_success, Toast.LENGTH_LONG).show();
        });
        viewModel.getGoogleLinkError().observe(this, message -> {
            if (message != null && !message.isEmpty()) Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    private void showGoogleLinkDialog() {
        EditText password = new EditText(this);
        password.setHint(R.string.profile_link_google_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        password.setPadding(padding, padding / 2, padding, padding / 2);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_link_google_title)
                .setMessage(R.string.profile_link_google_message)
                .setView(password)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.profile_link_google_continue,
                        (dialog, which) -> viewModel.startGoogleLink(password.getText().toString()))
                .show();
    }

    private void openGoogleLinkChooser(GoogleChallengeResponse challenge) {
        GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                BuildConfig.GOOGLE_WEB_CLIENT_ID).setNonce(challenge.getNonce()).build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(option).build();
        CredentialManager.create(this).getCredentialAsync(this, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override public void onResult(GetCredentialResponse response) {
                        if (response.getCredential() instanceof CustomCredential) {
                            CustomCredential value = (CustomCredential) response.getCredential();
                            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(value.getType())) {
                                try {
                                    viewModel.completeGoogleLink(GoogleIdTokenCredential
                                            .createFrom(value.getData()).getIdToken());
                                    return;
                                } catch (RuntimeException ignored) { }
                            }
                        }
                        viewModel.cancelGoogleLink();
                        Toast.makeText(ProfileActivity.this, R.string.login_google_unavailable,
                                Toast.LENGTH_LONG).show();
                    }
                    @Override public void onError(GetCredentialException exception) {
                        viewModel.cancelGoogleLink();
                        Toast.makeText(ProfileActivity.this, R.string.login_google_cancelled,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void renderUser(User user) {
        String name = user.getFullName();
        if (name == null || name.trim().isEmpty()) {
            name = user.getUsername();
        }
        textFullName.setText(name);

        String username = user.getUsername();
        textUsername.setText("@" + (username != null ? username : ""));

        // Chữ cái đầu avatar
        if (username != null && !username.trim().isEmpty()) {
            textAvatarLetter.setText(username.substring(0, 1).toUpperCase());
        } else {
            textAvatarLetter.setText("U");
        }

        String email = user.getEmail();
        textEmail.setText(email != null ? email : "");
        renderStoredAvatar();
    }

    private void renderStats(GamificationStats stats) {
        textTotalXp.setText(String.valueOf(stats.getTotalXp()));
        textCurrentStreak.setText(stats.getCurrentStreak() + " ngày");
        textLongestStreak.setText(stats.getLongestStreak() + " ngày");
        textLevelBadge.setText("Cấp độ " + stats.getLevel());
    }

    private void logout() {
        sessionManager.logout();
        GoogleCredentialState.clearAfterLogout(this);
        openLogin();
    }

    private void onAvatarPicked(Uri uri) {
        if (uri == null) return;
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // The selected provider may grant access only for this Activity lifetime.
        }
        showAvatarPreview(uri);
    }

    private void showAvatarPreview(Uri uri) {
        try {
            pendingAvatarBitmap = avatarStore.decodePreview(getContentResolver(), uri);
            pendingAvatarUri = uri;
            imageAvatarPreview.setImageBitmap(pendingAvatarBitmap);
            avatarPreviewContainer.setVisibility(View.VISIBLE);
        } catch (Exception error) {
            clearAvatarPreview();
            Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmAvatar() {
        if (pendingAvatarBitmap == null) return;
        try {
            avatarStore.save(sessionManager.getUserId(), pendingAvatarBitmap);
            clearAvatarPreview();
            renderStoredAvatar();
            navigationController.refreshAvatar();
            Toast.makeText(this, "Đã lưu ảnh trên thiết bị", Toast.LENGTH_SHORT).show();
        } catch (Exception error) {
            Toast.makeText(this, "Không thể lưu ảnh đại diện", Toast.LENGTH_LONG).show();
        }
    }

    private void resetAvatar() {
        if (avatarStore.reset(sessionManager.getUserId())) {
            renderStoredAvatar();
            navigationController.refreshAvatar();
            Toast.makeText(this, "Đã dùng avatar mặc định", Toast.LENGTH_SHORT).show();
        }
    }

    private void renderStoredAvatar() {
        Bitmap bitmap = avatarStore.load(sessionManager.getUserId());
        imageProfileAvatar.setImageBitmap(bitmap);
        imageProfileAvatar.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
        textAvatarLetter.setVisibility(bitmap == null ? View.VISIBLE : View.GONE);
        buttonResetAvatar.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
    }

    private void clearAvatarPreview() {
        pendingAvatarUri = null;
        pendingAvatarBitmap = null;
        imageAvatarPreview.setImageDrawable(null);
        avatarPreviewContainer.setVisibility(View.GONE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingAvatarUri != null) outState.putString(STATE_PENDING_AVATAR, pendingAvatarUri.toString());
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
