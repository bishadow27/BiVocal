package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.ui.auth.DangKyActivity;
import com.example.bivoca.ui.auth.QuenMatKhauActivity;
import com.example.bivoca.ui.home.TrangChuActivity;
import com.example.bivoca.ui.profile.HoSoCaNhanActivity;
import com.example.bivoca.ui.progress.TienDoHocTapActivity;
import com.example.bivoca.ui.leaderboard.BangXepHangActivity;
import com.example.bivoca.ui.settings.CaiDatActivity;
import com.example.bivoca.ui.settings.ChiTietCaiDatActivity;
import com.example.bivoca.ui.search.TimKiemTuVungActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Activities/API GET-only; never taps submit, save, logout, review, answer or favorite. */
@RunWith(AndroidJUnit4.class)
public class LightColorfulScreensUiTest {
    @Test public void existingAccountSearchAndAuthScreensUseRealDataWithoutWrites()throws Exception{
        Activity home=open(TrangChuActivity.class,null);snap("home-account-pass.png");
        main(()->home.findViewById(R.id.buttonHomeMenu).performClick());settle();
        main(()->assertTrue(((DrawerLayout)home.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
        snap("drawer-open.png");main(()->((AppCompatActivity)home).getOnBackPressedDispatcher().onBackPressed());settle();
        capture(HoSoCaNhanActivity.class,"profile.png");capture(TienDoHocTapActivity.class,"progress.png");capture(BangXepHangActivity.class,"leaderboard.png");
        capture(CaiDatActivity.class,"settings.png");
        for(String section:new String[]{ChiTietCaiDatActivity.SECTION_STUDY,ChiTietCaiDatActivity.SECTION_AUDIO,ChiTietCaiDatActivity.SECTION_REMINDER,ChiTietCaiDatActivity.SECTION_ABOUT}){
            Activity a=open(ChiTietCaiDatActivity.class,section);snap("settings-"+section+".png");main(a::finish);settle();
        }
        Activity search=open(TimKiemTuVungActivity.class,null);
        query(search,"apple");snap("search-english.png");query(search,"táo");snap("search-vietnamese.png");query(search,"zzzzzzzzzz");snap("search-empty.png");main(search::finish);settle();
        capture(DangKyActivity.class,"register.png");capture(QuenMatKhauActivity.class,"forgot.png");open(TrangChuActivity.class,null);
    }
    private void capture(Class<? extends Activity> type,String file)throws Exception{Activity a=open(type,null);snap(file);main(a::finish);settle();}
    private Activity open(Class<? extends Activity> type,String section){
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();Intent intent=new Intent(c,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(section!=null)intent.putExtra(ChiTietCaiDatActivity.EXTRA_SECTION,section);c.startActivity(intent);final Activity[] result={null};long deadline=SystemClock.uptimeMillis()+20000;
        do{main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a)&&a.hasWindowFocus())result[0]=a;});if(result[0]!=null){settle();return result[0];}SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("Activity did not open: "+type.getSimpleName());
    }
    private void query(Activity a,String q){main(()->{((EditText)a.findViewById(R.id.inputVocabularySearch)).setText(q);a.findViewById(R.id.buttonVocabularySearch).performClick();});settle();long end=SystemClock.uptimeMillis()+15000;boolean[] ready={false};do{main(()->ready[0]=a.findViewById(R.id.progressVocabularySearch).getVisibility()!=android.view.View.VISIBLE);if(ready[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);throw new AssertionError("GET search did not settle");}
    private static void main(Runnable r){InstrumentationRegistry.getInstrumentation().runOnMainSync(r);}
    private static void settle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(1400);}
    private static void snap(String name)throws Exception{settle();LightColorfulUiTest.shot(name);}
}
