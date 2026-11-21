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
import com.example.bivoca.ui.auth.DangNhapActivity;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.utils.GoogleCredentialState;
import com.example.bivoca.utils.LocalAvatarStore;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public class HoSoCaNhanActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_GOOGLE_LINK = "profile_open_google_link_from_settings";
    private User lastRenderedUser;
    private com.example.bivoca.data.model.LearningHistory lastRenderedHistory;
    private final java.text.NumberFormat numberFormat = java.text.NumberFormat.getInstance(java.util.Locale.US);

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
    private final java.util.concurrent.ExecutorService avatarDecoder = java.util.concurrent.Executors.newSingleThreadExecutor();
    private int avatarPreviewVersion;
    private androidx.appcompat.app.AlertDialog googleLinkDialog;
    private boolean wasAvatarSaving;
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

        setContentView(R.layout.activity_ho_so_ca_nhan);

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
                R.id.buttonProfileBack,
                getString(R.string.pm_title),
                isAccountLinkEntry() ? this::finish : null
        );
        buttonProfileRetry.setOnClickListener(v -> viewModel.loadOverview());
        findViewById(R.id.buttonProfileRetrySections).setOnClickListener(v -> viewModel.loadOverview());
        configureProfileLayout();
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

        if (viewModel.getOverview().getValue() == null) viewModel.loadOverview();
        if (isAccountLinkEntry() && !Boolean.TRUE.equals(viewModel.getGoogleLinkLoading().getValue())) {
            getWindow().getDecorView().post(this::showGoogleLinkDialog);
        }
    }

    private void observeViewModel() {
        viewModel.getAvatarSaving().observe(this, saving -> {
            setAvatarActionsEnabled(!Boolean.TRUE.equals(saving));
            if (wasAvatarSaving && Boolean.FALSE.equals(saving)) clearAvatarPreview();
            wasAvatarSaving = Boolean.TRUE.equals(saving);
            if (Boolean.FALSE.equals(saving)) renderStoredAvatar();
        });
        viewModel.getOverview().observe(this, this::renderOverview);
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
            if (isAccountLinkEntry()) finish();
        });
        viewModel.getGoogleLinkError().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                reopenGoogleLinkEntry();
            }
        });
    }

    private void reopenGoogleLinkEntry() {
        if (isAccountLinkEntry()) getWindow().getDecorView().post(() -> {
            if (!isFinishing() && !isDestroyed()) showGoogleLinkDialog();
        });
    }

    private void showGoogleLinkDialog() {
        if (googleLinkDialog != null && googleLinkDialog.isShowing()) return;
        EditText password = new EditText(this);
        password.setHint(R.string.profile_link_google_password);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        password.setPadding(padding, padding / 2, padding, padding / 2);
        googleLinkDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.profile_link_google_title)
                .setMessage(R.string.profile_link_google_message)
                .setView(password)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> { if (isAccountLinkEntry()) finish(); })
                .setOnCancelListener(dialog -> { if (isAccountLinkEntry()) finish(); })
                .setPositiveButton(R.string.profile_link_google_continue,
                        (dialog, which) -> viewModel.startGoogleLink(password.getText().toString()))
                .show();
        googleLinkDialog.setOnDismissListener(dialog -> googleLinkDialog = null);
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
                        Toast.makeText(HoSoCaNhanActivity.this, R.string.login_google_unavailable,
                                Toast.LENGTH_LONG).show();
                        reopenGoogleLinkEntry();
                    }
                    @Override public void onError(GetCredentialException exception) {
                        viewModel.cancelGoogleLink();
                        Toast.makeText(HoSoCaNhanActivity.this, R.string.login_google_cancelled,
                                Toast.LENGTH_LONG).show();
                        reopenGoogleLinkEntry();
                    }
                });
    }

    private boolean isAccountLinkEntry() {
        return getIntent().getBooleanExtra(EXTRA_OPEN_GOOGLE_LINK, false);
    }

    private TextView text(int id) { return findViewById(id); }

    private void configureProfileLayout() {
        int width = getResources().getConfiguration().screenWidthDp;
        boolean stacked = width < 360 || (width < 600 && getResources().getConfiguration().fontScale > 1.15f);
        int gap = Math.round(12 * getResources().getDisplayMetrics().density);
        for (int id : new int[]{R.id.profileXpContent,R.id.profileStreakContent,R.id.profileBestContent,R.id.profileRankContent}) {
            LinearLayout content = findViewById(id);
            content.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
            content.setGravity(stacked ? android.view.Gravity.START : android.view.Gravity.CENTER_VERTICAL);
            View icon = content.getChildAt(0),copy = content.getChildAt(1);
            LinearLayout.LayoutParams iconParams = (LinearLayout.LayoutParams) icon.getLayoutParams();
            iconParams.setMarginEnd(stacked ? 0 : gap);iconParams.bottomMargin = stacked ? Math.round(8 * getResources().getDisplayMetrics().density) : 0;
            icon.setLayoutParams(iconParams);
            LinearLayout.LayoutParams copyParams = (LinearLayout.LayoutParams) copy.getLayoutParams();
            copyParams.width = stacked ? LinearLayout.LayoutParams.MATCH_PARENT : 0;copyParams.weight = stacked ? 0 : 1;copy.setLayoutParams(copyParams);
        }
        if (stacked) {
            LinearLayout labels = findViewById(R.id.profileLevelLabels);labels.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams level = (LinearLayout.LayoutParams) textLevelBadge.getLayoutParams();
            level.width = LinearLayout.LayoutParams.MATCH_PARENT;level.weight = 0;textLevelBadge.setLayoutParams(level);
        }
    }

    private void renderOverview(ProfileOverviewState state) {
        if (state == null) return;
        boolean hasUser = state.user.value != null;
        boolean empty = hasUser && ProfilePresentation.displayName(state.user.value).isEmpty();
        progressProfile.setVisibility(!hasUser && state.user.loading ? View.VISIBLE : View.GONE);
        errorContainer.setVisibility(!hasUser && state.user.error != null ? View.VISIBLE : View.GONE);
        if (state.user.error != null) textProfileError.setText(state.user.error);
        findViewById(R.id.profileEmpty).setVisibility(empty || (!hasUser && state.user.complete && state.user.error == null) ? View.VISIBLE : View.GONE);
        scrollProfileContent.setVisibility(hasUser && !empty ? View.VISIBLE : View.GONE);
        buttonProfileRetry.setEnabled(!state.user.loading);
        if (!hasUser || empty) return;
        if (lastRenderedUser != state.user.value) { renderUser(state.user.value);lastRenderedUser = state.user.value; }
        renderStats(state.stats.value);
        findViewById(R.id.progressProfileStats).setVisibility(state.stats.loading || state.rank.loading ? View.VISIBLE : View.GONE);
        Integer rank = ProfilePresentation.rank(state.rank.value, state.user.value.getId());
        text(R.id.textProfileRank).setText(rank == null ? getString(R.string.pm_missing) : "#" + numberFormat.format(rank));
        TextView rankHint = text(R.id.textProfileRankHint);
        rankHint.setVisibility(rank == null && state.rank.complete && state.rank.error == null ? View.VISIBLE : View.GONE);
        rankHint.setText(R.string.pm_rank_unavailable);
        renderWeek(state);
        java.util.LinkedHashSet<String> errors = new java.util.LinkedHashSet<>();
        for (String message : new String[]{state.user.error,state.stats.error,state.rank.error,state.week.error,state.goal.error}) if (message != null && !message.trim().isEmpty()) errors.add(message);
        findViewById(R.id.profilePartialError).setVisibility(errors.isEmpty() ? View.GONE : View.VISIBLE);
        text(R.id.textProfilePartialError).setText(android.text.TextUtils.join("\n",errors));
        findViewById(R.id.buttonProfileRetrySections).setEnabled(!(state.user.loading || state.stats.loading || state.rank.loading || state.week.loading || state.goal.loading));
    }

    private void renderUser(User user) {
        String name = ProfilePresentation.displayName(user);
        textFullName.setText(name);
        String username = user.getUsername();
        textUsername.setText(username == null || username.isEmpty() ? getString(R.string.pm_missing) : "@" + username);
        textAvatarLetter.setText(ProfilePresentation.initials(name));
        textEmail.setText(user.getEmail() == null ? "" : user.getEmail());
        renderStoredAvatar();
    }

    private void renderStats(GamificationStats stats) {
        textTotalXp.setText(stats == null ? getString(R.string.pm_missing) : numberFormat.format(stats.getTotalXp()));
        textCurrentStreak.setText(stats == null ? getString(R.string.pm_missing) : getString(R.string.pm_days, stats.getCurrentStreak()));
        textLongestStreak.setText(stats == null ? getString(R.string.pm_missing) : getString(R.string.pm_days, stats.getLongestStreak()));
        boolean level = stats != null && stats.getLevel() > 0;
        findViewById(R.id.profileLevelContainer).setVisibility(level ? View.VISIBLE : View.GONE);
        if (level) textLevelBadge.setText(getString(R.string.pm_level, stats.getLevel()));
        boolean progress = ProfilePresentation.hasLevelProgress(stats);
        findViewById(R.id.profileLevelProgress).setVisibility(progress ? View.VISIBLE : View.GONE);
        text(R.id.textProfileLevelXp).setVisibility(progress ? View.VISIBLE : View.GONE);
        if (progress) {
            ProgressBar bar = findViewById(R.id.profileLevelProgress);bar.setMax(stats.getXpPerLevel());bar.setProgress(stats.getXpIntoLevel());
            text(R.id.textProfileLevelXp).setText(getString(R.string.pm_level_xp, stats.getXpIntoLevel(), stats.getXpPerLevel()));
        }
    }

    private void renderWeek(ProfileOverviewState state) {
        java.util.List<ProfilePresentation.Day> days = ProfilePresentation.week(state.week.value);
        boolean available = days.size() == 7;
        findViewById(R.id.profileWeekSection).setVisibility(available || state.week.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.profileWeekCard).setVisibility(available ? View.VISIBLE : View.GONE);
        findViewById(R.id.progressProfileWeek).setVisibility(state.week.loading || state.goal.loading ? View.VISIBLE : View.GONE);
        text(R.id.textProfileGoal).setText(state.goal.value != null && state.goal.value.getDailyGoal() > 0
                ? getString(R.string.pm_goal,state.goal.value.getDailyGoal())
                : getString(state.goal.loading ? R.string.pm_goal_loading : R.string.pm_goal_unavailable));
        if (!available || lastRenderedHistory == state.week.value) return;
        lastRenderedHistory = state.week.value;
        LinearLayout row = findViewById(R.id.profileWeekDays);row.removeAllViews();
        String[] labels = getResources().getStringArray(R.array.pm_weekdays);
        for (int i = 0; i < days.size(); i++) {
            ProfilePresentation.Day day = days.get(i);
            View item = getLayoutInflater().inflate(R.layout.item_profile_modern_day,row,false);
            ((TextView) item.findViewById(R.id.profileDayLabel)).setText(labels[i]);
            ImageView icon = item.findViewById(R.id.profileDayIcon);View circle = item.findViewById(R.id.profileDayCircle);
            int description;
            switch(day.state) {
                case COMPLETED: circle.setBackgroundResource(R.drawable.pm_day_complete);icon.setImageResource(R.drawable.pm_ic_check);description=R.string.pm_day_complete;break;
                case ACTIVE: circle.setBackgroundResource(R.drawable.pm_day_active);icon.setImageResource(R.drawable.pm_ic_flame);description=R.string.pm_day_active;break;
                case INACTIVE: icon.setImageResource(R.drawable.pm_ic_dash);description=R.string.pm_day_inactive;break;
                case FUTURE: icon.setImageResource(R.drawable.pm_ic_dot);description=R.string.pm_day_future;break;
                default: icon.setImageResource(R.drawable.pm_ic_unknown);description=R.string.pm_day_unknown;
            }
            String date = day.date.substring(8,10) + "/" + day.date.substring(5,7) + "/" + day.date.substring(0,4);
            if (day.state == ProfilePresentation.DayState.ACTIVE) item.setContentDescription(getString(R.string.pm_day_active,date,day.newWords,day.reviews));
            else if (day.state == ProfilePresentation.DayState.COMPLETED) item.setContentDescription(getString(R.string.pm_day_complete,date,day.newWords,day.reviews));
            else item.setContentDescription(getString(description,date));
            item.setTag(day.date);row.addView(item);
        }
    }

    private void logout() {
        sessionManager.logout();
        GoogleCredentialState.clearAfterLogout(this);
        openLogin();
    }

    private void onAvatarPicked(Uri uri) {
        if (uri == null) return;
        showAvatarPreview(uri);
    }

    private void showAvatarPreview(Uri uri) {
        final int version = ++avatarPreviewVersion;
        pendingAvatarUri = uri;
        pendingAvatarBitmap = null;
        avatarDecoder.execute(() -> {
            Bitmap decoded = null;
            try {
                try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                catch (SecurityException ignored) { /* Some providers grant temporary access only. */ }
                decoded = avatarStore.decodePreview(getContentResolver(), uri);
            } catch (Exception ignored) { /* Show a safe UI error without logging the selected URI. */ }
            Bitmap result = decoded;
            ContextCompat.getMainExecutor(this).execute(() -> {
                if (isDestroyed() || isFinishing() || version != avatarPreviewVersion) {
                    if (result != null) result.recycle();
                    return;
                }
                if (result == null) {
                    clearAvatarPreview();
                    Toast.makeText(this, "Không thể đọc ảnh đã chọn", Toast.LENGTH_LONG).show();
                } else {
                    pendingAvatarBitmap = result;
                    imageAvatarPreview.setImageBitmap(result);
                    avatarPreviewContainer.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void confirmAvatar() {
        if (pendingAvatarBitmap == null) return;
        Bitmap selected = pendingAvatarBitmap;
        int userId = sessionManager.getUserId();
        viewModel.setAvatarSaving(true);
        avatarDecoder.execute(() -> {
            boolean success;
            try { avatarStore.save(userId, selected);success = true; }
            catch (Exception ignored) { success = false; }
            boolean saved = success;
            ContextCompat.getMainExecutor(this).execute(() -> {
                viewModel.setAvatarSaving(false);
                if (isDestroyed() || isFinishing()) return;
                if (saved) { clearAvatarPreview();renderStoredAvatar();navigationController.refreshAvatar(); }
                Toast.makeText(this, saved ? "Đã lưu ảnh trên thiết bị" : "Không thể lưu ảnh đại diện", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void setAvatarActionsEnabled(boolean enabled) {
        for (int id : new int[]{R.id.buttonConfirmAvatar,R.id.buttonCancelAvatar,R.id.buttonResetAvatar,R.id.buttonChooseAvatar}) {
            findViewById(id).setEnabled(enabled);
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
        avatarPreviewVersion++;
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

    @Override protected void onDestroy() {
        avatarPreviewVersion++;
        avatarDecoder.shutdown();
        if (googleLinkDialog != null) googleLinkDialog.dismiss();
        super.onDestroy();
    }

    private void openLogin() {
        Intent intent = new Intent(this, DangNhapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
