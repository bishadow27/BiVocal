package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.ui.auth.LoginActivity;
import com.example.bivoca.utils.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Login UI only. Malformed input is rejected locally; no auth POST or session mutation. */
@RunWith(AndroidJUnit4.class)
public class LoginApprovedUiTest {
    @Test public void headerFormAndFooterSurviveSmallScreensLargeFontsAndErrors() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            int[][] sizes={{320,640},{360,800},{411,731},{412,915},{440,915},{467,830},{731,411}};
            for(int[] size:sizes) for(float scale:new float[]{1f,1.3f}) for(boolean error:new boolean[]{false,true}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.fontScale=scale;config.screenWidthDp=size[0];config.screenHeightDp=size[1];
                Context context=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Login_Approved);
                View root=LayoutInflater.from(context).inflate(R.layout.activity_login,null,false);
                if(error) {
                    TextView feedback=root.findViewById(R.id.textError);
                    feedback.setVisibility(View.VISIBLE);
                    feedback.setText(R.string.login_google_unavailable);
                }
                float density=context.getResources().getDisplayMetrics().density;
                int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                root.layout(0,0,w,h);
                String label=size[0]+"x"+size[1]+" font="+scale+" error="+error;
                assertTextFits(root,label);
                TextView slogan=root.findViewById(R.id.loginSlogan);
                assertTrue(label,slogan.getLineCount()<=2);
                TextView title=root.findViewById(R.id.loginHeaderTitle);
                assertTrue(label+" title fragmented",title.getLineCount()<=2);
                View divider=root.findViewById(R.id.loginHeaderDivider);
                assertTrue(label,divider.getRight()<=title.getLeft());
                View toggle=root.findViewById(R.id.buttonTogglePassword);
                assertTrue(label,toggle.getWidth()>=48*density-1&&toggle.getHeight()>=48*density-1);
                ScrollView scroll=root.findViewById(R.id.loginScroll);
                scroll.scrollTo(0,scroll.getChildAt(0).getHeight());
                View footer=root.findViewById(R.id.textRegister);
                assertTrue(label+" footer unreachable",footer.getBottom()-scroll.getScrollY()<=scroll.getHeight());
            }
        });
    }

    @Test public void actualLoginValidationVisibilityRecreationAndKeyboard() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertFalse("User must log out directly before Login runtime test",new SessionManager(context).isLoggedIn());
        try(ActivityScenario<LoginActivity> scenario=ActivityScenario.launch(LoginActivity.class)) {
            awaitLayout(scenario);
            scenario.onActivity(activity -> {
                if(Build.VERSION.SDK_INT>=26) {
                    activity.findViewById(R.id.loginRoot).setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
                    AutofillManager autofill=activity.getSystemService(AutofillManager.class);
                    if(autofill!=null)autofill.cancel();
                }
                assertNotNull(((ImageView)activity.findViewById(R.id.loginMascot)).getDrawable());
                assertTrue(activity.findViewById(R.id.buttonGoogleSignIn).hasOnClickListeners());
                assertTrue(activity.findViewById(R.id.buttonGoogleSignIn).isEnabled());
            });
            screenshot("login-top.png");
            onView(withId(R.id.buttonLogin)).perform(scrollTo(),click());
            onView(withId(R.id.textError)).check(matches(withText("Vui lòng nhập email.")));
            screenshot("login-local-validation.png");
            onView(withId(R.id.editEmail)).perform(scrollTo(),replaceText("invalid"));
            onView(withId(R.id.editPassword)).perform(scrollTo(),replaceText("UiPreview123"));
            closeSoftKeyboard();
            onView(withId(R.id.buttonLogin)).perform(scrollTo(),click());
            onView(withId(R.id.textError)).check(matches(withText("Email không hợp lệ.")));
            onView(withId(R.id.buttonTogglePassword)).perform(scrollTo(),click());
            scenario.onActivity(activity -> assertFalse(((EditText)activity.findViewById(R.id.editPassword)).getTransformationMethod() instanceof PasswordTransformationMethod));
            scenario.recreate();
            awaitLayout(scenario);
            scenario.onActivity(activity -> {
                assertEquals("invalid",((EditText)activity.findViewById(R.id.editEmail)).getText().toString());
                assertEquals(12,((EditText)activity.findViewById(R.id.editPassword)).length());
                assertFalse(((EditText)activity.findViewById(R.id.editPassword)).getTransformationMethod() instanceof PasswordTransformationMethod);
            });
            onView(withId(R.id.buttonTogglePassword)).perform(scrollTo(),click());
            scenario.onActivity(activity -> {
                assertTrue(((EditText)activity.findViewById(R.id.editPassword)).getTransformationMethod() instanceof PasswordTransformationMethod);
                ((EditText)activity.findViewById(R.id.editEmail)).getText().clear();
                ((EditText)activity.findViewById(R.id.editPassword)).getText().clear();
                activity.findViewById(R.id.textError).setVisibility(View.GONE);
            });
            onView(withId(R.id.editEmail)).perform(scrollTo(),click());
            scenario.onActivity(activity -> ((InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(activity.findViewById(R.id.editEmail),InputMethodManager.SHOW_IMPLICIT));
            boolean[] ime={false};long deadline=SystemClock.uptimeMillis()+4000;
            do {
                scenario.onActivity(activity -> {
                    WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(activity.findViewById(R.id.loginRoot));
                    ime[0]=insets!=null&&insets.isVisible(WindowInsetsCompat.Type.ime())
                            && insets.getInsets(WindowInsetsCompat.Type.ime()).bottom>0;
                });
                if(!ime[0])SystemClock.sleep(100);
            } while(!ime[0]&&SystemClock.uptimeMillis()<deadline);
            assertTrue("Docked keyboard with a nonzero IME inset required",ime[0]);
            onView(withId(R.id.editPassword)).perform(scrollTo()).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertAboveKeyboard(activity,R.id.editPassword));
            onView(withId(R.id.buttonLogin)).perform(scrollTo()).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertAboveKeyboard(activity,R.id.buttonLogin));
            onView(withId(R.id.textRegister)).perform(scrollTo()).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertAboveKeyboard(activity,R.id.textRegister));
            screenshot("login-keyboard-footer.png");
            closeSoftKeyboard();
            scenario.onActivity(activity -> ((ScrollView)activity.findViewById(R.id.loginScroll)).scrollTo(0,0));
            assertFalse("UI-only validation must not create a session",new SessionManager(context).isLoggedIn());
        }
    }

    private static void awaitLayout(ActivityScenario<LoginActivity> scenario) {
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+7000;
        do {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> ready[0]=activity.hasWindowFocus()&&!activity.findViewById(R.id.loginRoot).isLayoutRequested());
            if(!ready[0])SystemClock.sleep(100);
        } while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Login did not acquire focus and layout",ready[0]);
    }
    private static void assertAboveKeyboard(Activity activity,int id) {
        View decor=activity.getWindow().getDecorView(),view=activity.findViewById(id);
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(decor);
        assertNotNull(insets);
        int ime=insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
        assertTrue("A floating keyboard cannot prove inset handling",ime>0);
        int[] origin=new int[2],position=new int[2];
        decor.getLocationOnScreen(origin);view.getLocationOnScreen(position);
        assertTrue("Control obscured by the keyboard: "+id,position[1]+view.getHeight()<=origin[1]+decor.getHeight()-ime);
    }
    private static void assertTextFits(View view,String label) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            if(text.getText().length()>0) {
                assertTrue(label+" zero width",text.getWidth()>0);
                assertNotNull(label,text.getLayout());
                assertTrue(label+" text height clipped: "+text.getId(),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                int lines=text.getLayout().getLineCount();
                assertEquals(label+" text truncated: "+text.getId(),text.getText().length(),text.getLayout().getLineEnd(lines-1));
                // getLineWidth includes trailing spaces at a wrap; only the drawn extent can clip.
                for(int i=0;i<lines;i++)assertTrue(label+" text width clipped: "+text.getId(),text.getLayout().getLineMax(i)<=text.getWidth()-text.getCompoundPaddingLeft()-text.getCompoundPaddingRight()+1);
            }
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertTextFits(((ViewGroup)view).getChildAt(i),label);
    }
    private static void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(250);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"login-approved-20261029");
        assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream stream=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,stream));}
        bitmap.recycle();
    }
}
