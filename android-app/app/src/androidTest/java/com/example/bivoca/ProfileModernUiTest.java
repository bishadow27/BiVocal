package com.example.bivoca;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.ui.profile.HoSoCaNhanActivity;
import com.example.bivoca.ui.profile.ProfileViewModel;
import com.example.bivoca.ui.profile.ProfileOverviewState;
import com.example.bivoca.ui.profile.ProfilePresentation;
import com.example.bivoca.utils.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.Locale;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

/** Current user-selected session, real GET-only repositories. Never submits credentials or learning writes. */
@RunWith(AndroidJUnit4.class)
public class ProfileModernUiTest {
    static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    static String config(){return InstrumentationRegistry.getArguments().getString("config","phone-font1");}
    static Rect bounds(View v){int[] xy=new int[2];v.getLocationOnScreen(xy);return new Rect(xy[0],xy[1],xy[0]+v.getWidth(),xy[1]+v.getHeight());}
    static ProfileViewModel vm(HoSoCaNhanActivity a){return new ViewModelProvider(a).get(ProfileViewModel.class);}
    static void ready(ActivityScenario<HoSoCaNhanActivity> scenario){
        boolean landscape=config().contains("landscape");
        scenario.onActivity(a->a.setRequestedOrientation(landscape?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        long deadline=SystemClock.uptimeMillis()+25000;boolean[] done={false};
        do{scenario.onActivity(a->{ProfileOverviewState s=vm(a).getOverview().getValue();done[0]=s!=null&&s.user.complete&&s.stats.complete&&s.rank.complete&&s.week.complete&&s.goal.complete
                    &&a.getResources().getConfiguration().orientation==(landscape?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT);});
            if(done[0])return;SystemClock.sleep(100);
        }while(SystemClock.uptimeMillis()<deadline);
        fail("Profile real GET loading timed out; check current selected account/backend");
    }
    static void shot(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(250);
        assertTrue(config().matches("[a-zA-Z0-9_-]+"));
        File dir=new File(context().getExternalFilesDir(null),"profile-modern-evidence/"+config());assertTrue(dir.isDirectory()||dir.mkdirs());
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);
        try(FileOutputStream stream=new FileOutputStream(new File(dir,name))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,stream));}b.recycle();
        Configuration c=context().getResources().getConfiguration();
        String metadata=String.format(Locale.ROOT,"{\"widthDp\":%d,\"heightDp\":%d,\"fontScale\":%.1f,\"orientation\":%d}",c.screenWidthDp,c.screenHeightDp,c.fontScale,c.orientation);
        try(FileOutputStream stream=new FileOutputStream(new File(dir,"configuration.json"))){stream.write(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    static void render(HoSoCaNhanActivity a,ProfileOverviewState state){
        try{java.lang.reflect.Method m=HoSoCaNhanActivity.class.getDeclaredMethod("renderOverview",ProfileOverviewState.class);m.setAccessible(true);m.invoke(a,state);}
        catch(Exception e){throw new AssertionError(e);}
    }
    static void textFits(View v){
        if(v instanceof TextView&&v.getVisibility()==View.VISIBLE){TextView t=(TextView)v;
            if(t.getLayout()!=null){for(int i=0;i<t.getLayout().getLineCount();i++)assertEquals("Profile text must not be ellipsized",0,t.getLayout().getEllipsisCount(i));
                assertTrue("Profile text must fit its measured height",t.getHeight()+1>=t.getLayout().getHeight()+t.getCompoundPaddingTop()+t.getCompoundPaddingBottom());}}
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)textFits(((ViewGroup)v).getChildAt(i));
    }
    @Test public void actualDataCalendarLayoutAndRecreation()throws Exception{
        assertTrue("Use existing signed-in session; do not inject test JWT",new SessionManager(context()).getUserId()>0);
        try(ActivityScenario<HoSoCaNhanActivity> scenario=ActivityScenario.launch(HoSoCaNhanActivity.class)){
            ready(scenario);ProfileViewModel[] retained={null};ProfileOverviewState[] before={null};
            scenario.onActivity(a->{
                ProfileOverviewState s=vm(a).getOverview().getValue();assertNull(s.user.error);assertNull(s.stats.error);assertNull(s.rank.error);assertNull(s.week.error);assertNull(s.goal.error);
                assertNotNull(s.user.value);assertNotNull(s.stats.value);retained[0]=vm(a);before[0]=s;
                assertEquals(ProfilePresentation.displayName(s.user.value),((TextView)a.findViewById(R.id.textFullName)).getText().toString());
                assertEquals("@"+s.user.value.getUsername(),((TextView)a.findViewById(R.id.textUsername)).getText().toString());
                assertEquals(NumberFormat.getInstance(Locale.US).format(s.stats.value.getTotalXp()),((TextView)a.findViewById(R.id.textTotalXp)).getText().toString());
                assertEquals(a.getString(R.string.pm_level,s.stats.value.getLevel()),((TextView)a.findViewById(R.id.textLevelBadge)).getText().toString());
                ProgressBar bar=a.findViewById(R.id.profileLevelProgress);assertEquals(s.stats.value.getXpPerLevel(),bar.getMax());assertEquals(s.stats.value.getXpIntoLevel(),bar.getProgress());
                Integer rank=ProfilePresentation.rank(s.rank.value,s.user.value.getId());assertNotNull(rank);assertEquals("#"+rank,((TextView)a.findViewById(R.id.textProfileRank)).getText().toString());
                assertEquals(a.getString(R.string.pm_goal,s.goal.value.getDailyGoal()),((TextView)a.findViewById(R.id.textProfileGoal)).getText().toString());
                LinearLayout days=a.findViewById(R.id.profileWeekDays);assertEquals(7,days.getChildCount());
                java.util.List<ProfilePresentation.Day> week=ProfilePresentation.week(s.week.value);
                for(int i=0;i<7;i++){assertEquals(week.get(i).date,days.getChildAt(i).getTag());assertNotNull(days.getChildAt(i).getContentDescription());}
                assertEquals(Color.parseColor("#F7F7F2"),((ColorDrawable)a.findViewById(R.id.profileRoot).getBackground()).getColor());
                assertEquals(View.GONE,a.findViewById(R.id.buttonLinkGoogle).getVisibility());assertEquals(View.GONE,a.findViewById(R.id.buttonProfileLogout).getVisibility());
                float d=a.getResources().getDisplayMetrics().density;assertTrue(a.findViewById(R.id.buttonChooseAvatar).getWidth()>=48*d-1);assertTrue(a.findViewById(R.id.buttonChooseAvatar).getHeight()>=48*d-1);
                assertTrue(bounds(a.findViewById(R.id.profileHeaderCard)).top>=bounds(a.findViewById(R.id.textNavigationTitle)).bottom-1);
                Rect xp=bounds(a.findViewById(R.id.profileXpContent)),streak=bounds(a.findViewById(R.id.profileStreakContent));assertTrue(xp.right<streak.left);
                textFits(a.findViewById(R.id.scrollProfileContent));
            });
            shot("profile-top.png");
            scenario.onActivity(a->((ScrollView)a.findViewById(R.id.scrollProfileContent)).fullScroll(View.FOCUS_DOWN));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(150);
            scenario.onActivity(a->{Rect viewport=bounds(a.findViewById(R.id.scrollProfileContent)),week=bounds(a.findViewById(R.id.profileWeekCard));assertTrue(week.bottom<=viewport.bottom+1);assertTrue(week.top>=viewport.top-1);});
            shot("profile-week.png");scenario.recreate();ready(scenario);
            scenario.onActivity(a->{assertSame("Recreation retains requests/state instead of refetching",retained[0],vm(a));assertSame(before[0],vm(a).getOverview().getValue());
                TextView name=a.findViewById(R.id.textFullName);name.setText("Nguyễn Hoàng Minh Anh Nguyễn Hoàng Minh Anh Nguyễn Hoàng Minh Anh");});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a->{textFits(a.findViewById(R.id.scrollProfileContent));((TextView)a.findViewById(R.id.textFullName)).setText(ProfilePresentation.displayName(before[0].user.value));a.findViewById(R.id.buttonNavigationBack).performClick();});
            onView(withId(R.id.homeRoot)).check(matches(isDisplayed()));
        }
    }
    @Test public void loadingEmptyErrorAndRealRetryRemainIndependent(){
        try(ActivityScenario<HoSoCaNhanActivity> scenario=ActivityScenario.launch(HoSoCaNhanActivity.class)){
            ready(scenario);scenario.onActivity(a->{ProfileOverviewState s=vm(a).getOverview().getValue();
                render(a,ProfileOverviewState.empty().loading());assertEquals(View.VISIBLE,a.findViewById(R.id.progressProfile).getVisibility());
                render(a,ProfileOverviewState.empty().withUser(ProfileOverviewState.Section.success(null)));assertEquals(View.VISIBLE,a.findViewById(R.id.profileEmpty).getVisibility());
                render(a,ProfileOverviewState.empty().withUser(ProfileOverviewState.Section.failure("Test: unavailable",ProfileOverviewState.Section.empty())));assertEquals(View.VISIBLE,a.findViewById(R.id.errorContainer).getVisibility());
                render(a,s.withRank(ProfileOverviewState.Section.failure("Test: rank unavailable",s.rank)));assertEquals(View.VISIBLE,a.findViewById(R.id.profilePartialError).getVisibility());assertEquals(View.VISIBLE,a.findViewById(R.id.scrollProfileContent).getVisibility());
                a.findViewById(R.id.buttonProfileRetrySections).performClick();});
            ready(scenario);scenario.onActivity(a->{assertNull(vm(a).getOverview().getValue().rank.error);assertEquals(View.GONE,a.findViewById(R.id.profilePartialError).getVisibility());});
        }
    }
    @Test public void drawerSettingsGoogleLinkCancelAndBackKeepSession()throws Exception{
        int user=new SessionManager(context()).getUserId();
        try(ActivityScenario<HoSoCaNhanActivity> scenario=ActivityScenario.launch(HoSoCaNhanActivity.class)){
            ready(scenario);
            onView(withId(R.id.buttonNavigationAvatar)).perform(click());
            onView(withId(R.id.navProfile)).perform(click());
            onView(withId(R.id.profileHeaderCard)).check(matches(isDisplayed()));
            onView(withId(R.id.buttonNavigationAvatar)).perform(click());onView(withId(R.id.navSettings)).perform(click());
            onView(withId(R.id.settingsGoogleLink)).perform(scrollTo());
            shot("settings-google-link.png");
            onView(withId(R.id.settingsGoogleLink)).perform(click());
            onView(withText(R.string.profile_link_google_title)).check(matches(isDisplayed()));
            shot("settings-google-link-dialog.png");
            // Empty password is rejected locally; no challenge/link request is made.
            onView(withText(R.string.profile_link_google_continue)).perform(click());
            SystemClock.sleep(250);
            onView(withText(R.string.profile_link_google_title)).check(matches(isDisplayed()));
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                for(android.app.Activity a:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(a instanceof HoSoCaNhanActivity)a.recreate();
            });
            SystemClock.sleep(400);
            onView(withText(R.string.profile_link_google_title)).check(matches(isDisplayed()));
            onView(withText(android.R.string.cancel)).perform(click());
            onView(withId(R.id.settingsGoogleLink)).check(matches(isDisplayed()));
            assertEquals(user,new SessionManager(context()).getUserId());
        }
    }
    @Test public void avatarSaveLocksConflictingActionsAndRestoresLocalFile()throws Exception{
        com.example.bivoca.utils.LocalAvatarStore store=new com.example.bivoca.utils.LocalAvatarStore(context());
        File target=store.avatarFile(new SessionManager(context()).getUserId());
        byte[] original=target.exists()?java.nio.file.Files.readAllBytes(target.toPath()):null;
        File input=new File(context().getCacheDir(),"profile-preview-instrumentation.png");
        Bitmap image=Bitmap.createBitmap(40,40,Bitmap.Config.ARGB_8888);image.eraseColor(Color.BLUE);
        try(FileOutputStream output=new FileOutputStream(input)){image.compress(Bitmap.CompressFormat.PNG,100,output);}image.recycle();
        java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService[] worker={null};
        try(ActivityScenario<HoSoCaNhanActivity> scenario=ActivityScenario.launch(HoSoCaNhanActivity.class)){
            ready(scenario);
            scenario.onActivity(a->{try{java.lang.reflect.Method preview=HoSoCaNhanActivity.class.getDeclaredMethod("showAvatarPreview",android.net.Uri.class);preview.setAccessible(true);preview.invoke(a,android.net.Uri.fromFile(input));
                java.lang.reflect.Field field=HoSoCaNhanActivity.class.getDeclaredField("avatarDecoder");field.setAccessible(true);worker[0]=(java.util.concurrent.ExecutorService)field.get(a);
            }catch(Exception e){throw new AssertionError(e);}});
            worker[0].submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            worker[0].submit(()->{try{release.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            try{scenario.onActivity(a->{assertEquals(View.VISIBLE,a.findViewById(R.id.avatarPreviewContainer).getVisibility());a.findViewById(R.id.buttonConfirmAvatar).performClick();
                assertFalse("Reset must not race an in-flight local avatar save",a.findViewById(R.id.buttonResetAvatar).isEnabled());
                assertFalse(a.findViewById(R.id.buttonCancelAvatar).isEnabled());assertFalse(a.findViewById(R.id.buttonChooseAvatar).isEnabled());});
                scenario.recreate();ready(scenario);
                scenario.onActivity(a->assertFalse("Recreation must retain the save lock",a.findViewById(R.id.buttonResetAvatar).isEnabled()));
            }finally{release.countDown();
                if(worker[0].isShutdown())assertTrue(worker[0].awaitTermination(5,java.util.concurrent.TimeUnit.SECONDS));
                else worker[0].submit(()->{}).get(5,java.util.concurrent.TimeUnit.SECONDS);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
            scenario.onActivity(a->{assertTrue(target.isFile());assertTrue(a.findViewById(R.id.buttonResetAvatar).isEnabled());a.findViewById(R.id.buttonResetAvatar).performClick();assertFalse(target.exists());});
        }finally{release.countDown();
            if(original==null)java.nio.file.Files.deleteIfExists(target.toPath());else{target.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(target)){out.write(original);}}
            java.nio.file.Files.deleteIfExists(input.toPath());
        }
    }
}
