package com.example.bivoca.ui.navigation;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.bivoca.R;
import com.example.bivoca.ui.auth.LoginActivity;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardActivity;
import com.example.bivoca.ui.profile.ProfileActivity;
import com.example.bivoca.ui.progress.ProgressActivity;
import com.example.bivoca.ui.quiz.QuizActivity;
import com.example.bivoca.ui.settings.SettingsActivity;
import com.example.bivoca.ui.topics.TopicsActivity;
import com.example.bivoca.utils.SessionManager;
import com.example.bivoca.utils.GoogleCredentialState;
import com.example.bivoca.utils.LocalAvatarStore;
import com.example.bivoca.settings.ReminderScheduler;

import java.util.EnumMap;
import java.util.Map;

/** Adds the single shared authenticated drawer around an Activity's existing content view. */
public final class NavigationDrawerController {

    private static final float DRAWER_WIDTH_RATIO = 0.84f;
    private static final int DRAWER_MAX_WIDTH_DP = 360;

    private final AppCompatActivity activity;
    private final NavigationRoute currentRoute;
    private final DrawerLayout drawerLayout;
    private final View screen;
    private final View drawerPanel;
    private final ImageView topAvatarImage;
    private final TextView topAvatarFallback;
    private final TextView drawerAvatarFallback;
    private final Runnable customBackAction;
    private final OnBackPressedCallback backCallback;
    private LearningBottomNavigation.NavigationGuard navigationGuard = Runnable::run;
    private NavigationRoute pendingDestination;

    private NavigationDrawerController(AppCompatActivity activity,
                                       NavigationRoute currentRoute,
                                       View legacyNavigationButton,
                                       CharSequence screenTitle,
                                       Runnable customBackAction) {
        this(activity,currentRoute,legacyNavigationButton,screenTitle,customBackAction,false);
    }

    private NavigationDrawerController(AppCompatActivity activity, NavigationRoute currentRoute,
                                       View legacyNavigationButton, CharSequence screenTitle,
                                       Runnable customBackAction, boolean sessionHeader) {
        this.activity = activity;
        this.currentRoute = currentRoute;
        this.customBackAction = customBackAction;
        ViewGroup activityContent = activity.findViewById(android.R.id.content);
        this.screen = activityContent.getChildAt(0);
        this.drawerLayout = wrapCurrentContent(activity);

        ViewGroup screenContainer = (ViewGroup) drawerLayout.getChildAt(0);
        boolean customHeader = sessionHeader || currentRoute == NavigationRoute.HOME || currentRoute == NavigationRoute.TOPICS;
        View sharedTopBar = customHeader ? null : LayoutInflater.from(activity)
                .inflate(R.layout.view_navigation_top_bar, screenContainer, false);
        if (sharedTopBar != null) screenContainer.addView(sharedTopBar);
        applySharedInsets(sharedTopBar);

        TextView backButton = customHeader ? null : sharedTopBar.findViewById(R.id.buttonNavigationBack);
        View avatarButton = customHeader ? null : sharedTopBar.findViewById(R.id.buttonNavigationAvatar);
        topAvatarImage = customHeader ? activity.findViewById(R.id.imageHomeAvatar) : sharedTopBar.findViewById(R.id.imageNavigationAvatar);
        topAvatarFallback = customHeader ? activity.findViewById(R.id.textHomeAvatar) : sharedTopBar.findViewById(R.id.textNavigationAvatarFallback);
        if (!customHeader) {
            legacyNavigationButton.setVisibility(View.INVISIBLE);
            ((TextView) sharedTopBar.findViewById(R.id.textNavigationTitle)).setText(screenTitle);
        }

        drawerPanel = LayoutInflater.from(activity)
                .inflate(R.layout.view_navigation_drawer, drawerLayout, false);
        DrawerLayout.LayoutParams drawerParams = new DrawerLayout.LayoutParams(
                drawerWidthPx(activity), ViewGroup.LayoutParams.MATCH_PARENT);
        drawerParams.gravity = GravityCompat.END;
        drawerLayout.addView(drawerPanel, drawerParams);
        ViewCompat.setElevation(drawerPanel, dp(activity, 16));
        applyDrawerInsets(drawerPanel);

        drawerLayout.setScrimColor(activity.getColor(R.color.bivocal_light_scrim));
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

        drawerAvatarFallback = drawerPanel.findViewById(R.id.textNavAvatarFallback);

        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) close();
                else handleScreenBack();
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, backCallback);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                refreshAvatar();
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                NavigationRoute destination = pendingDestination;
                pendingDestination = null;
                if (destination != null) {
                    navigate(destination);
                }
            }
        });

        if (sessionHeader) legacyNavigationButton.setOnClickListener(view -> handleScreenBack());
        else if (customHeader) legacyNavigationButton.setOnClickListener(view -> open());
        else {
            backButton.setOnClickListener(view -> handleScreenBack());
            avatarButton.setOnClickListener(view -> open());
        }
        drawerPanel.findViewById(R.id.buttonNavClose).setOnClickListener(view -> close());

        bindUser(drawerPanel);
        refreshAvatar();
        bindDestinations(drawerPanel);
        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override public void onResume(@NonNull LifecycleOwner owner) {
                bindUser(drawerPanel);
                refreshAvatar();
            }
        });
    }

    public static NavigationDrawerController attach(AppCompatActivity activity,
                                                     NavigationRoute currentRoute,
                                                     int menuButtonId) {
        return attach(activity, currentRoute, menuButtonId,
                defaultTitle(activity, currentRoute), null);
    }

    public static NavigationDrawerController attach(AppCompatActivity activity,
                                                     NavigationRoute currentRoute,
                                                     int menuButtonId,
                                                     CharSequence title) {
        return attach(activity, currentRoute, menuButtonId, title, null);
    }

    public static NavigationDrawerController attach(AppCompatActivity activity,
                                                     NavigationRoute currentRoute,
                                                     int menuButtonId,
                                                     CharSequence title,
                                                     Runnable customBackAction) {
        View legacyButton = activity.findViewById(menuButtonId);
        if (legacyButton == null) {
            throw new IllegalStateException("Navigation menu button is missing");
        }
        return new NavigationDrawerController(activity, currentRoute, legacyButton,
                title, customBackAction);
    }

    /** Flashcard opts into its own Back/title/progress header; existing routes are unchanged. */
    public static NavigationDrawerController attachSession(AppCompatActivity activity,
            NavigationRoute route, int backId, Runnable onBack) {
        return new NavigationDrawerController(activity,route,activity.findViewById(backId),"",onBack,true);
    }

    private DrawerLayout wrapCurrentContent(AppCompatActivity activity) {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content.getChildCount() != 1) {
            throw new IllegalStateException("Expected one Activity content view");
        }
        View screen = content.getChildAt(0);
        content.removeView(screen);

        DrawerLayout drawer = new DrawerLayout(activity);
        drawer.setId(R.id.navigationDrawerLayout);
        FrameLayout screenContainer = new FrameLayout(activity);
        screenContainer.setId(R.id.navigationScreenContainer);
        screenContainer.addView(screen, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        drawer.addView(screenContainer, new DrawerLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        content.addView(drawer, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return drawer;
    }

    private void applySharedInsets(View topBar) {
        int screenStart = screen.getPaddingStart();
        int screenTop = screen.getPaddingTop();
        int screenEnd = screen.getPaddingEnd();
        int screenBottom = screen.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            screen.setPaddingRelative(
                    screenStart + bars.left,
                    screenTop + bars.top,
                    screenEnd + bars.right,
                    screenBottom + bars.bottom);
            if (topBar != null) {
                topBar.setPaddingRelative(0, bars.top, 0, 0);
                ViewGroup.LayoutParams params = topBar.getLayoutParams();
                params.height = dp(activity, 64) + bars.top;
                topBar.setLayoutParams(params);
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(drawerLayout);
    }

    private void applyDrawerInsets(View panel) {
        int start = panel.getPaddingStart();
        int top = panel.getPaddingTop();
        int end = panel.getPaddingEnd();
        int bottom = panel.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(panel, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            view.setPaddingRelative(start, top + bars.top, end, bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(panel);
    }

    private void bindUser(View drawerPanel) {
        String username = new SessionManager(activity).getUsername();
        TextView userView = drawerPanel.findViewById(R.id.textNavUser);
        if (username == null || username.trim().isEmpty()) {
            userView.setText(R.string.nav_signed_in);
        } else {
            userView.setText(activity.getString(R.string.nav_greeting, username));
        }
    }

    public void refreshAvatar() {
        SessionManager session = new SessionManager(activity);
        Bitmap bitmap = session.getUserId() > 0 ? new LocalAvatarStore(activity).load(session.getUserId()) : null;
        String initial = AvatarLabel.initial(session.getUsername());
        bindAvatarPair(topAvatarImage, topAvatarFallback, bitmap, initial);
        drawerAvatarFallback.setText(initial);
    }

    private static void bindAvatarPair(ImageView image, TextView fallback,
                                       Bitmap bitmap, String initial) {
        if (image == null || fallback == null) return;
        image.setImageBitmap(bitmap);
        image.setVisibility(bitmap == null ? View.GONE : View.VISIBLE);
        fallback.setText(initial);
        fallback.setVisibility(bitmap == null ? View.VISIBLE : View.GONE);
    }

    private void bindDestinations(View drawerPanel) {
        Map<NavigationRoute, Integer> rows = new EnumMap<>(NavigationRoute.class);
        rows.put(NavigationRoute.PROFILE, R.id.navProfile);
        rows.put(NavigationRoute.SETTINGS, R.id.navSettings);
        rows.put(NavigationRoute.LOGOUT, R.id.navLogout);

        NavigationRoute selected = NavigationRoutePolicy.selectedMenuFor(currentRoute);
        for (Map.Entry<NavigationRoute, Integer> entry : rows.entrySet()) {
            View row = drawerPanel.findViewById(entry.getValue());
            boolean isSelected = entry.getKey() == selected;
            applySelectionState(row, isSelected);
            if (isSelected) {
                CharSequence description = row.getContentDescription();
                row.setContentDescription(activity.getString(
                        R.string.nav_selected_description, description));
            }
            row.setOnClickListener(view -> select(entry.getKey()));
        }
    }

    private void applySelectionState(View view, boolean selected) {
        view.setSelected(selected);
        view.setActivated(selected);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applySelectionState(group.getChildAt(index), selected);
            }
        }
    }

    private void select(NavigationRoute destination) {
        if (NavigationRoutePolicy.shouldReuseCurrentScreen(currentRoute, destination)) {
            close();
            return;
        }
        pendingDestination = destination;
        close();
    }

    public void setNavigationGuard(LearningBottomNavigation.NavigationGuard guard) {
        navigationGuard = guard == null ? Runnable::run : guard;
    }

    private void navigate(NavigationRoute destination) {
        navigationGuard.request(() -> LearningBottomNavigation.requestNavigation(activity, () -> performNavigation(destination)));
    }

    private void performNavigation(NavigationRoute destination) {
        if (destination == NavigationRoute.LOGOUT) {
            SessionManager session = new SessionManager(activity);
            ReminderScheduler.cancelForUser(activity, session.getUserId());
            session.logout();
            GoogleCredentialState.clearAfterLogout(activity);
            Intent login = new Intent(activity, LoginActivity.class);
            login.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(login);
            activity.finish();
            return;
        }

        Intent intent;
        switch (destination) {
            case HOME:
                intent = new Intent(activity, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case TOPICS:
                intent = new Intent(activity, TopicsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case REVIEW_TODAY:
                intent = new Intent(activity, FlashcardActivity.class);
                intent.putExtra(FlashcardActivity.EXTRA_REVIEW_TODAY, true);
                break;
            case QUIZ:
                intent = new Intent(activity, QuizActivity.class);
                break;
            case PROGRESS:
                intent = new Intent(activity, ProgressActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case LEADERBOARD:
                intent = new Intent(activity, LeaderboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case PROFILE:
                intent = new Intent(activity, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            case SETTINGS:
                intent = new Intent(activity, SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                break;
            default:
                return;
        }
        LearningBottomNavigation.startRetained(activity, intent);
    }

    public void open() {
        refreshAvatar();
        drawerLayout.openDrawer(GravityCompat.END);
    }

    public void close() {
        drawerLayout.closeDrawer(GravityCompat.END);
    }

    private void handleScreenBack() {
        if (customBackAction != null) customBackAction.run();
        else performDefaultBack();
    }

    public void performDefaultBack() {
        if (currentRoute != NavigationRoute.HOME && activity.isTaskRoot()) {
            Intent home = new Intent(activity, HomeActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            activity.startActivity(home);
            activity.finish();
            return;
        }
        backCallback.setEnabled(false);
        activity.getOnBackPressedDispatcher().onBackPressed();
        backCallback.setEnabled(true);
    }

    private static CharSequence defaultTitle(AppCompatActivity activity, NavigationRoute route) {
        switch (route) {
            case TOPICS: return activity.getString(R.string.nav_topics);
            case FLASHCARDS: return activity.getString(R.string.flashcard_title);
            case REVIEW_TODAY: return activity.getString(R.string.nav_review_today);
            case QUIZ: return activity.getString(R.string.nav_quiz);
            case PROGRESS: return activity.getString(R.string.nav_progress);
            case LEADERBOARD: return activity.getString(R.string.nav_leaderboard);
            case PROFILE: return activity.getString(R.string.nav_profile);
            case SETTINGS: return activity.getString(R.string.nav_settings);
            default: return "";
        }
    }

    private static int drawerWidthPx(AppCompatActivity activity) {
        int screenWidth = activity.getResources().getDisplayMetrics().widthPixels;
        return Math.min(dp(activity, DRAWER_MAX_WIDTH_DP),
                Math.round(screenWidth * DRAWER_WIDTH_RATIO));
    }

    private static int dp(AppCompatActivity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
