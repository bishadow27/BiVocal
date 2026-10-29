package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.SystemClock;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import com.example.bivoca.ui.auth.ForgotPasswordActivity;
import com.example.bivoca.ui.auth.LoginActivity;
import com.example.bivoca.ui.auth.RegisterActivity;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/** UI-only acceptance. All submitted forms are invalid locally; no API writes. */
@RunWith(AndroidJUnit4.class)
public class AuthScreensUiTest {
    @Test public void normalFontTallViewportShowsAuthWithoutScrolling() {
        Configuration config = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources().getConfiguration();
        Assume.assumeTrue(config.screenHeightDp >= 780 && config.fontScale <= 1.0f);
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            awaitScreen(R.id.registerRoot);
            scenario.onActivity(activity -> assertFitsViewport((ScrollView) activity.findViewById(R.id.registerScroll)));
        }
        try (ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class)) {
            awaitScreen(R.id.forgotRoot);
            scenario.onActivity(activity -> assertFitsViewport((ScrollView) activity.findViewById(R.id.forgotScroll)));
        }
    }

    private static void assertFitsViewport(ScrollView scroll) {
        assertEquals(0, scroll.getScrollY());
        assertTrue("Auth content must fit the tall viewport without scrolling: " + scroll.getChildAt(0).getHeight() + "/" + scroll.getHeight(),
                scroll.getChildAt(0).getHeight() <= scroll.getHeight());
    }

    private static void awaitScreen(int id) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        int stable = 0;
        int previousHeight = -1;
        while (SystemClock.uptimeMillis() < deadline) {
            final int[] height = {-1};
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    View root = activity.findViewById(id);
                    if (root != null && root.isShown() && root.hasWindowFocus() && !root.isLayoutRequested()) {
                        height[0] = root.getHeight();
                    }
                }
            });
            stable = height[0] > 0 && height[0] == previousHeight ? stable + 1 : 0;
            if (stable >= 2) return;
            previousHeight = height[0];
            SystemClock.sleep(150);
        }
        fail("Screen did not finish its navigation/layout transition: " + id);
    }

    private static void disableTestAutofill(View root) {
        // Suppress Google Password Manager prompts only in tests with synthetic input.
        if (Build.VERSION.SDK_INT >= 26) {
            root.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                for (int index = 0; index < group.getChildCount(); index++) {
                    disableTestAutofill(group.getChildAt(index));
                }
            }
            AutofillManager manager = root.getContext().getSystemService(AutofillManager.class);
            if (manager != null) manager.cancel();
        }
    }

    @Test public void registerValidationToggleAndRecreation() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> disableTestAutofill(activity.findViewById(R.id.registerRoot)));
            onView(withId(R.id.editFullName)).perform(scrollTo(), replaceText("UI Sample"));
            onView(withId(R.id.editUsername)).perform(scrollTo(), replaceText("?"));
            onView(withId(R.id.editEmail)).perform(scrollTo(), replaceText("invalid"));
            onView(withId(R.id.editPassword)).perform(scrollTo(), replaceText("UiOnly123"));
            onView(withId(R.id.editConfirmPassword)).perform(scrollTo(), replaceText("UiOnly123"));
            closeSoftKeyboard();
            onView(withId(R.id.buttonRegister)).perform(scrollTo(), click());
            onView(withId(R.id.registerUsernameError)).check(matches(withText(R.string.register_username_invalid)));
            closeSoftKeyboard();
            scenario.onActivity(activity -> disableTestAutofill(activity.findViewById(R.id.registerRoot)));
            onView(withId(R.id.registerTogglePassword)).perform(scrollTo(), click());
            onView(withId(R.id.registerToggleConfirm)).perform(scrollTo(), click());
            scenario.onActivity(activity -> {
                EditText password = activity.findViewById(R.id.editPassword);
                EditText confirm = activity.findViewById(R.id.editConfirmPassword);
                assertFalse(password.getTransformationMethod() instanceof PasswordTransformationMethod);
                assertFalse("Confirmation toggle did not reveal text; description="
                        + activity.findViewById(R.id.registerToggleConfirm).getContentDescription(),
                        confirm.getTransformationMethod() instanceof PasswordTransformationMethod);
                assertEquals("UiOnly123", password.getText().toString());
            });
            scenario.onActivity(activity -> disableTestAutofill(activity.findViewById(R.id.registerRoot)));
            scenario.recreate();
            scenario.onActivity(activity -> {
                disableTestAutofill(activity.findViewById(R.id.registerRoot));
                assertEquals("UI Sample", ((EditText) activity.findViewById(R.id.editFullName)).getText().toString());
                assertEquals("?", ((EditText) activity.findViewById(R.id.editUsername)).getText().toString());
                assertEquals("invalid", ((EditText) activity.findViewById(R.id.editEmail)).getText().toString());
                assertEquals("UiOnly123", ((EditText) activity.findViewById(R.id.editPassword)).getText().toString());
                assertEquals("UiOnly123", ((EditText) activity.findViewById(R.id.editConfirmPassword)).getText().toString());
                assertFalse(((EditText) activity.findViewById(R.id.editPassword))
                        .getTransformationMethod() instanceof PasswordTransformationMethod);
            });
            onView(withId(R.id.registerTogglePassword)).perform(scrollTo(), click());
            scenario.onActivity(activity -> assertTrue(((EditText) activity.findViewById(R.id.editPassword))
                    .getTransformationMethod() instanceof PasswordTransformationMethod));
            scenario.onActivity(activity -> {
                ((EditText) activity.findViewById(R.id.editPassword)).getText().clear();
                ((EditText) activity.findViewById(R.id.editConfirmPassword)).getText().clear();
                disableTestAutofill(activity.findViewById(R.id.registerRoot));
            });
        }
    }

    @Test public void forgotRejectsEmptyAndMalformedEmailLocally() {
        try (ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class)) {
            scenario.onActivity(activity -> disableTestAutofill(activity.findViewById(R.id.forgotRoot)));
            onView(withId(R.id.forgotButtonSubmit)).perform(scrollTo(), click());
            onView(withId(R.id.forgotFeedback)).check(matches(withText(R.string.forgot_email_required)));
            onView(withId(R.id.forgotEditEmail)).perform(scrollTo(), replaceText("invalid"));
            closeSoftKeyboard();
            onView(withId(R.id.forgotButtonSubmit)).perform(scrollTo(), click());
            onView(withId(R.id.forgotFeedback)).check(matches(withText(R.string.forgot_email_invalid)));
            onView(withId(R.id.forgotBackLogin)).perform(scrollTo(), click());
            long deadline = SystemClock.uptimeMillis() + 3000;
            while (scenario.getState() != Lifecycle.State.DESTROYED && SystemClock.uptimeMillis() < deadline) {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                SystemClock.sleep(50);
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test public void loginNavigationUsesExistingActivities() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            awaitScreen(R.id.loginRoot);
            onView(withId(R.id.textRegister)).perform(scrollTo(), click());
            awaitScreen(R.id.registerRoot);
            onView(withId(R.id.authScreenTitle)).check(matches(withText(R.string.auth_register_title)));
            onView(withId(R.id.textLogin)).perform(scrollTo(), click());
            awaitScreen(R.id.loginRoot);
            onView(withId(R.id.loginRoot)).check(matches(isDisplayed()));
            onView(withId(R.id.textForgotPassword)).perform(scrollTo(), click());
            awaitScreen(R.id.forgotRoot);
            onView(withId(R.id.authScreenTitle)).check(matches(withText(R.string.auth_recovery_title)));
            onView(withId(R.id.forgotBackLogin)).perform(scrollTo(), click());
            awaitScreen(R.id.loginRoot);
            onView(withId(R.id.loginRoot)).check(matches(isDisplayed()));
        }
    }

    @Test public void realLayoutsFitSmallTallLandscapeAndLargeFonts() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
            int[][] sizes = {{320, 640}, {411, 731}, {411, 914}, {731, 411}};
            for (float scale : new float[]{1f, 1.3f}) {
                for (int[] size : sizes) {
                    Configuration configuration = new Configuration(base.getResources().getConfiguration());
                    configuration.fontScale = scale;
                    configuration.screenWidthDp = size[0];
                    configuration.screenHeightDp = size[1];
                    Context themed = new ContextThemeWrapper(base.createConfigurationContext(configuration),
                            R.style.Theme_BiVoca_Login_Reference);
                    for (int layout : new int[]{R.layout.activity_register, R.layout.activity_forgot_password}) {
                        View root = LayoutInflater.from(themed).inflate(layout, null);
                        TextView title = root.findViewById(R.id.authScreenTitle);
                        title.setText(layout == R.layout.activity_register
                                ? R.string.auth_register_title : R.string.auth_recovery_title);
                        float density = themed.getResources().getDisplayMetrics().density;
                        int width = Math.round(size[0] * density);
                        int height = Math.round(size[1] * density);
                        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                        root.layout(0, 0, width, height);
                        String label = size[0] + "x" + size[1] + " scale=" + scale + " layout=" + layout;
                        assertNotNull(label, title.getLayout());
                        int lines = title.getLayout().getLineCount();
                        assertTrue(label, lines <= 2);
                        assertEquals(label + " title truncated", title.getText().length(),
                                title.getLayout().getLineEnd(lines - 1));
                        TextView slogan = root.findViewById(R.id.authSlogan);
                        int sloganLines = slogan.getLayout().getLineCount();
                        assertEquals(label + " slogan truncated", slogan.getText().length(),
                                slogan.getLayout().getLineEnd(sloganLines - 1));
                        assertTrue(label + " slogan height clipped",
                                slogan.getLayout().getHeight() <= slogan.getHeight()
                                        - slogan.getCompoundPaddingTop() - slogan.getCompoundPaddingBottom());
                        ScrollView scroll = root.findViewById(layout == R.layout.activity_register
                                ? R.id.registerScroll : R.id.forgotScroll);
                        View footer = root.findViewById(layout == R.layout.activity_register
                                ? R.id.textLogin : R.id.forgotBackLogin);
                        scroll.scrollTo(0, scroll.getChildAt(0).getHeight());
                        assertTrue(label + " footer cannot be reached",
                                footer.getBottom() - scroll.getScrollY() <= scroll.getHeight());
                    }
                }
            }
        });
    }

    @Test public void registerCanScrollWithRealKeyboard() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            scenario.onActivity(activity -> disableTestAutofill(activity.findViewById(R.id.registerRoot)));
            onView(withId(R.id.editEmail)).perform(scrollTo(), click());
            scenario.onActivity(activity -> ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(activity.findViewById(R.id.editEmail), InputMethodManager.SHOW_IMPLICIT));
            final boolean[] visible = {false};
            long deadline = SystemClock.uptimeMillis() + 3000;
            do {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(activity -> {
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.findViewById(R.id.registerRoot));
                    visible[0] = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
                });
                if (!visible[0]) SystemClock.sleep(150);
            } while (!visible[0] && SystemClock.uptimeMillis() < deadline);
            Assume.assumeTrue("Emulator did not expose a docked IME", visible[0]);
            onView(withId(R.id.editConfirmPassword)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.buttonRegister)).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.textLogin)).perform(scrollTo()).check(matches(isDisplayed()));
            closeSoftKeyboard();
        }
    }
}
