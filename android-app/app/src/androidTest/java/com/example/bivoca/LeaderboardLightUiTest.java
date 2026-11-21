package com.example.bivoca;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.data.model.LeaderboardUser;
import com.example.bivoca.ui.leaderboard.BangXepHangActivity;
import com.example.bivoca.ui.leaderboard.LeaderboardViewModel;
import com.example.bivoca.utils.SessionManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.io.File;
import java.io.FileOutputStream;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.click;
import static org.junit.Assert.*;

/** Uses the existing user-selected session and actual GET-only leaderboard. No account/XP mutations. */
@RunWith(AndroidJUnit4.class)
public class LeaderboardLightUiTest {
    private static Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static Rect bounds(View v) { int[] p=new int[2];v.getLocationOnScreen(p);return new Rect(p[0],p[1],p[0]+v.getWidth(),p[1]+v.getHeight()); }
    private static void awaitData(ActivityScenario<BangXepHangActivity> scenario) {
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+20000;
        boolean landscape=InstrumentationRegistry.getArguments().getString("config","").equals("landscape-font13");
        if(landscape)scenario.onActivity(a->a.setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        do { scenario.onActivity(a->ready[0]=a.findViewById(R.id.layoutLeaderboardContent).getVisibility()==View.VISIBLE
                &&(!landscape||a.getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE));
            if(ready[0])return;SystemClock.sleep(100);
        }while(SystemClock.uptimeMillis()<deadline);
        fail("GET-only leaderboard did not load; check the current user-selected session/backend before testing");
    }
    private static NestedScrollView scroller(View v) {
        if(v instanceof NestedScrollView)return (NestedScrollView)v;
        if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){NestedScrollView found=scroller(((ViewGroup)v).getChildAt(i));if(found!=null)return found;}
        return null;
    }
    private static void shot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(350);
        String config=InstrumentationRegistry.getArguments().getString("config","font1");
        assertTrue(config.matches("[a-zA-Z0-9_-]+"));
        File directory=new File(context().getExternalFilesDir(null),"leaderboard-light-evidence/"+config);
        assertTrue(directory.isDirectory()||directory.mkdirs());
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream output=new FileOutputStream(new File(directory,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}
        bitmap.recycle();
    }
    @Test public void approvedPodiumPaletteAndFooterUseRealRanks() throws Exception {
        assertTrue("Existing signed-in account required; never inject a test JWT",new SessionManager(context()).getUserId()>0);
        try(ActivityScenario<BangXepHangActivity> scenario=ActivityScenario.launch(BangXepHangActivity.class)) {
            awaitData(scenario);
            scenario.onActivity(a->{
                Rect one=bounds(a.findViewById(R.id.podiumCol1)),two=bounds(a.findViewById(R.id.podiumCol2)),three=bounds(a.findViewById(R.id.podiumCol3));
                assertTrue("Approved physical podium order must be2-1-3",two.centerX()<one.centerX()&&one.centerX()<three.centerX());
                assertTrue(a.findViewById(R.id.blockPodium1).getHeight()>a.findViewById(R.id.blockPodium2).getHeight());
                assertTrue(a.findViewById(R.id.blockPodium2).getHeight()>a.findViewById(R.id.blockPodium3).getHeight());
                assertTrue("Podium rank must remain exposed to accessibility",a.findViewById(R.id.blockPodium1).isImportantForAccessibility());
                assertTrue(a.findViewById(R.id.blockPodium2).isImportantForAccessibility());
                assertTrue(a.findViewById(R.id.blockPodium3).isImportantForAccessibility());
                assertEquals(Color.parseColor("#F7F7F2"),((ColorDrawable)a.findViewById(R.id.leaderboardRoot).getBackground()).getColor());
                assertEquals("Lịch sử điểm",((TextView)a.findViewById(R.id.buttonHistoryPoints)).getText().toString());
                float density=a.getResources().getDisplayMetrics().density;
                assertTrue(a.findViewById(R.id.buttonHistoryPoints).getHeight()>=48*density-1);
                assertTrue(a.findViewById(R.id.buttonLeaderboardHelp).getHeight()>=48*density-1);
                assertTrue(bounds(a.findViewById(R.id.layoutUserStickyBar)).bottom<=bounds(a.findViewById(R.id.homeBottomNavigation)).top+1);
                assertTrue("Shared top bar must not obscure filters",bounds(a.findViewById(R.id.tabLeaderboardAll)).top>=bounds(a.findViewById(R.id.textNavigationTitle)).bottom-1);
                List<LeaderboardUser> rows=new ViewModelProvider(a).get(LeaderboardViewModel.class).getLeaderboard().getValue();
                assertNotNull(rows);assertTrue(rows.size()>=3);
                assertEquals(rows.get(0).getDisplayName(),((TextView)a.findViewById(R.id.textPodiumName1)).getText().toString());
                assertEquals(rows.get(1).getDisplayName(),((TextView)a.findViewById(R.id.textPodiumName2)).getText().toString());
                int current=new SessionManager(a).getUserId();
                for(LeaderboardUser user:rows)if(user.getUserId()==current){
                    assertEquals("#"+user.getRank()+" · Bạn",((TextView)a.findViewById(R.id.textStickyUserRank)).getText().toString());
                    assertEquals(NumberFormat.getInstance(Locale.US).format(user.getTotalXp()),((TextView)a.findViewById(R.id.textStickyUserScore)).getText().toString());
                }
                assertNotNull("One scroll region must include podium and list",scroller(a.findViewById(R.id.leaderboardRoot)));
            });
            shot("leaderboard-top.png");
        }
    }
    @Test public void historyCallbackAndFullScrollRemainReachable() throws Exception {
        try(ActivityScenario<BangXepHangActivity> scenario=ActivityScenario.launch(BangXepHangActivity.class)) {
            awaitData(scenario);
            scenario.onActivity(a->a.findViewById(R.id.buttonHistoryPoints).performClick());
            onView(withText("Lịch sử điểm tích lũy")).check(matches(isDisplayed()));
            onView(withText("Đã hiểu")).perform(click());
            scenario.onActivity(a->{NestedScrollView scroll=scroller(a.findViewById(R.id.leaderboardRoot));assertNotNull(scroll);scroll.fullScroll(View.FOCUS_DOWN);});
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(300);
            scenario.onActivity(a->{
                NestedScrollView scroll=scroller(a.findViewById(R.id.leaderboardRoot));assertTrue(scroll.getScrollY()>0);
                assertTrue(a.findViewById(R.id.buttonHistoryPoints).isShown());
                RecyclerView list=a.findViewById(R.id.recyclerLeaderboard);
                RecyclerView.ViewHolder last=list.findViewHolderForAdapterPosition(list.getAdapter().getItemCount()-1);
                assertNotNull("Last API row must be laid out",last);
                Rect viewport=bounds(scroll),row=bounds(last.itemView);
                assertTrue("Last row must fit entirely above footer: row="+row+", viewport="+viewport,
                        row.top>=viewport.top-1&&row.bottom<=viewport.bottom+1);
            });
            shot("leaderboard-bottom.png");
        }
    }
}
