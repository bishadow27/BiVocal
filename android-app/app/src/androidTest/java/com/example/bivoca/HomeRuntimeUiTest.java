package com.example.bivoca;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.data.model.*;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.ui.profile.ProfileActivity;
import com.example.bivoca.ui.profile.ProfileViewModel;
import com.example.bivoca.ui.progress.ProgressViewModel;
import com.example.bivoca.ui.progress.ProgressActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardViewModel;
import com.example.bivoca.ui.topics.TopicsActivity;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.quiz.QuizActivity;
import com.example.bivoca.utils.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/** Read-only runtime acceptance with the existing signed-in account. Never submits learning/quiz. */
@RunWith(AndroidJUnit4.class)
public class HomeRuntimeUiTest {
    @Test public void realApisNavigationDrawerAndScreenshots() throws Exception {
        assertTrue("Existing user login required",new SessionManager(InstrumentationRegistry.getInstrumentation().getTargetContext()).isLoggedIn());
        try(ActivityScenario<HomeActivity> scenario=ActivityScenario.launch(HomeActivity.class)) {
            awaitActivity(HomeActivity.class);
            final boolean[] ready={false};
            long deadline=SystemClock.uptimeMillis()+15000;
            do {
                scenario.onActivity(activity -> {
                    ProfileViewModel profile=new ViewModelProvider(activity).get(ProfileViewModel.class);
                    ProgressViewModel progress=new ViewModelProvider(activity).get(ProgressViewModel.class);
                    LeaderboardViewModel leaderboard=new ViewModelProvider(activity).get(LeaderboardViewModel.class);
                    ready[0]=profile.getUser().getValue()!=null && profile.getStats().getValue()!=null
                        && progress.getLearningToday().getValue()!=null && progress.getSummary().getValue()!=null
                        && progress.getSchedule().getValue()!=null && leaderboard.getLeaderboard().getValue()!=null;
                });
                if(!ready[0])SystemClock.sleep(150);
            } while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
            assertTrue("Real Backend Home APIs did not load",ready[0]);
            scenario.onActivity(activity -> {
                ProfileViewModel profile=new ViewModelProvider(activity).get(ProfileViewModel.class);
                ProgressViewModel progress=new ViewModelProvider(activity).get(ProgressViewModel.class);
                assertNull(profile.getError().getValue());
                assertNull(progress.getError().getValue());
                assertNull(progress.getLearningError().getValue());
                assertNull(progress.getScheduleError().getValue());
                assertEquals(View.GONE,activity.findViewById(R.id.homeErrorContainer).getVisibility());
                assertEquals(activity.getString(R.string.home16_streak_badge,
                        java.text.NumberFormat.getIntegerInstance().format(profile.getStats().getValue().getCurrentStreak())),
                        ((TextView) activity.findViewById(R.id.textHomeStreakBadge)).getText().toString());
                ReviewSchedule schedule=progress.getSchedule().getValue();
                assertEquals(schedule.getReviewedCards(),java.util.Arrays.stream(schedule.getMemoryLevels()).sum());
                int[] groups=com.example.bivoca.ui.home.HomePresentation.srsGroups(schedule.getMemoryLevels());
                int[] labels={R.id.textHomeNew,R.id.textHomeRemembering,R.id.textHomeGood,R.id.textHomeMastered};
                for(int i=0;i<groups.length;i++)assertEquals(java.text.NumberFormat.getIntegerInstance().format(groups[i]),
                        ((TextView)activity.findViewById(labels[i])).getText().toString());
                assertEquals(schedule.getDueNow()>0,activity.findViewById(R.id.buttonHomeSrsReview).isEnabled());
                assertEquals(schedule.getDueNow()>0
                        ? activity.getString(R.string.home_srs_due,java.text.NumberFormat.getIntegerInstance().format(schedule.getDueNow()))
                        : activity.getString(R.string.home_srs_no_due),
                        ((TextView)activity.findViewById(R.id.textHomeSrsDueLabel)).getText().toString());
                ScrollView scroll=activity.findViewById(R.id.homeScroll);
                scroll.scrollTo(0,0);
            });
            screenshot("home-top.png");
            onView(withId(R.id.cardHomeSrs)).perform(scrollTo());
            screenshot("home-srs.png");
            onView(withId(R.id.buttonHomeSrsInfo)).perform(click());
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.home_srs_help_close)).perform(click());
            onView(withId(R.id.cardHomeLeaderboard)).perform(scrollTo());
            screenshot("home-leaderboard.png");
            scenario.onActivity(activity -> {ScrollView scroll=activity.findViewById(R.id.homeScroll);scroll.scrollTo(0,scroll.getChildAt(0).getHeight());});
            screenshot("home-bottom-navigation.png");
            onView(withId(R.id.buttonHomeMenu)).perform(click());
            scenario.onActivity(activity -> assertTrue(((DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
            screenshot("home-drawer-open.png");
            onView(withId(R.id.buttonNavClose)).perform(click());
            navigate(R.id.buttonTopics,TopicsActivity.class,false,null);
            navigate(R.id.buttonReviewToday,FlashcardActivity.class,false,FlashcardActivity.EXTRA_REVIEW_TODAY);
            navigate(R.id.buttonQuiz,QuizActivity.class,false,null);
            onView(withId(R.id.buttonHomeMenu)).perform(click());
            navigate(R.id.navProfile,ProfileActivity.class,true,null);
            navigate(R.id.buttonProgress,ProgressActivity.class,true,null);
            navigate(R.id.buttonLeaderboard,LeaderboardActivity.class,true,null);
            scenario.onActivity(activity -> ((ScrollView)activity.findViewById(R.id.homeScroll)).scrollTo(0,0));
            navigate(R.id.buttonLearnNewToday,FlashcardActivity.class,false,FlashcardActivity.EXTRA_NEW_TODAY);
            scenario.onActivity(activity -> ((ScrollView)activity.findViewById(R.id.homeScroll)).scrollTo(0,0));
        }
    }
    private static void navigate(int id,Class<? extends Activity> destination,boolean scroll,String extra) {
        if(scroll)onView(withId(id)).perform(scrollTo(),click());
        else onView(withId(id)).perform(click());
        Activity target=awaitActivity(destination);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            if(extra!=null)assertTrue(target.getIntent().getBooleanExtra(extra,false));
            target.finish();
        });
        awaitActivity(HomeActivity.class);
    }
    private static Activity awaitActivity(Class<? extends Activity> type) {
        Activity[] found={null};
        long deadline=SystemClock.uptimeMillis()+5000;
        while(SystemClock.uptimeMillis()<deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for(Activity activity:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                    if(type.isInstance(activity) && (type != HomeActivity.class || activity.hasWindowFocus())
                            && !activity.findViewById(android.R.id.content).isLayoutRequested()) found[0]=activity;
            });
            if(found[0]!=null){SystemClock.sleep(600);return found[0];}
            SystemClock.sleep(100);
        }
        fail("Destination did not open: "+type.getSimpleName());return null;
    }
    private static void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        SystemClock.sleep(300);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"m16-3-home");
        assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}
        bitmap.recycle();
    }
}
