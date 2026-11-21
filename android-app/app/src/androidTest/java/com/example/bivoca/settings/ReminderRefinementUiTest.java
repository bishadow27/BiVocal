package com.example.bivoca.settings;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.R;
import com.example.bivoca.ui.settings.CaiDatActivity;
import com.example.bivoca.ui.settings.ChiTietCaiDatActivity;
import com.example.bivoca.ui.settings.ReminderViewModel;
import com.example.bivoca.ui.profile.HoSoCaNhanActivity;
import com.example.bivoca.utils.SessionManager;
import com.google.android.material.materialswitch.MaterialSwitch;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import static org.junit.Assert.*;

/** Local reminder writes only, restored in finally. No auth/review/quiz/favorite mutations. */
@RunWith(AndroidJUnit4.class)
public class ReminderRefinementUiTest {
    private static final int[] IDS={R.id.daySunday,R.id.dayMonday,R.id.dayTuesday,R.id.dayWednesday,R.id.dayThursday,R.id.dayFriday,R.id.daySaturday};
    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static void main(Runnable work){InstrumentationRegistry.getInstrumentation().runOnMainSync(work);}
    private static void settle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450);}
    private static ActivityScenario<ChiTietCaiDatActivity> reminder(){return ActivityScenario.launch(new Intent(context(),ChiTietCaiDatActivity.class).putExtra(ChiTietCaiDatActivity.EXTRA_SECTION,ChiTietCaiDatActivity.SECTION_REMINDER));}
    private static ReminderViewModel model(ChiTietCaiDatActivity a){return new androidx.lifecycle.ViewModelProvider(a).get(ReminderViewModel.class);}
    private static void draft(ChiTietCaiDatActivity a,boolean enabled,int h,int m,int mask){
        ((MaterialSwitch)a.findViewById(R.id.switchReminder)).setChecked(enabled);
        TimePicker picker=a.findViewById(R.id.pickerReminderTime);picker.setHour(h);picker.setMinute(m);
        for(int i=0;i<IDS.length;i++)((CheckBox)a.findViewById(IDS[i])).setChecked(SettingsPreferenceManager.isDaySelected(mask,i+1));
    }
    private static void awaitSaved(ActivityScenario<ChiTietCaiDatActivity> scenario){
        long until=SystemClock.uptimeMillis()+8000;boolean[] complete={false};
        do{scenario.onActivity(a->complete[0]=!model(a).current().saving);if(complete[0])break;SystemClock.sleep(50);}while(SystemClock.uptimeMillis()<until);
        scenario.onActivity(a->{assertEquals(ReminderViewModel.Feedback.SAVED,model(a).current().feedback);assertFalse(model(a).current().dirty());});
    }
    @Test public void explicitSaveValidatesPersistsRecreatesAndRestoresAlarms() throws Exception {
        assertEquals("A user-selected signed-in account is required",true,new SessionManager(context()).getUserId()>0);
        assertEquals(PackageManager.PERMISSION_GRANTED,context().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS));
        try(ReminderBackup backup=new ReminderBackup();ActivityScenario<ChiTietCaiDatActivity> scenario=reminder()) {
            ReminderSchedule before=new SettingsPreferenceManager(context()).readReminderSchedule();
            scenario.onActivity(a->{draft(a,true,21,37,42);assertEquals(before,new SettingsPreferenceManager(a).readReminderSchedule());assertTrue(model(a).current().dirty());});
            shot("reminder-before-save.png");
            scenario.recreate();
            scenario.onActivity(a->{assertEquals(new ReminderSchedule(true,21,37,42),model(a).current().draft);assertEquals(before,model(a).current().saved);a.findViewById(R.id.buttonSaveReminder).performClick();});
            awaitSaved(scenario);assertEquals(new ReminderSchedule(true,21,37,42),new SettingsPreferenceManager(context()).readReminderSchedule());
            assertAlarms(42);shot("reminder-after-save.png");
            scenario.recreate();scenario.onActivity(a->assertEquals(new ReminderSchedule(true,21,37,42),model(a).current().draft));
            scenario.onActivity(a->{draft(a,true,21,37,0);a.findViewById(R.id.buttonSaveReminder).performClick();assertEquals(ReminderViewModel.Feedback.INVALID_DAYS,model(a).current().feedback);assertEquals(42,new SettingsPreferenceManager(a).getReminderDays());});
            shot("reminder-invalid-days.png");
            scenario.onActivity(a->{model(a).discard();draft(a,false,21,38,0);a.findViewById(R.id.buttonSaveReminder).performClick();});
            awaitSaved(scenario);assertAlarms(0);
            scenario.close();
            try(ActivityScenario<ChiTietCaiDatActivity> reopened=reminder()) {reopened.onActivity(a->{assertEquals(new ReminderSchedule(false,21,38,0),model(a).current().draft);assertFalse(model(a).current().dirty());});}
        }
    }
    @Test public void unsavedBackAndLogoutCanBeCancelledAndPermissionDenialDoesNotPersist() {
        try(ActivityScenario<ChiTietCaiDatActivity> scenario=reminder()) {
            ReminderSchedule before=new SettingsPreferenceManager(context()).readReminderSchedule();int owner=new SessionManager(context()).getUserId();
            scenario.onActivity(a->{draft(a,!before.enabled,(before.hour+1)%24,before.minute,127);a.getOnBackPressedDispatcher().onBackPressed();});settle();
            clickDialog("Ở lại");scenario.onActivity(a->assertTrue(model(a).current().dirty()));
            scenario.onActivity(a->a.findViewById(R.id.buttonNavigationAvatar).performClick());settle();
            scenario.onActivity(a->a.findViewById(R.id.navLogout).performClick());settle();clickDialog("Ở lại");
            assertEquals(owner,new SessionManager(context()).getUserId());assertEquals(before,new SettingsPreferenceManager(context()).readReminderSchedule());
            scenario.onActivity(a->{model(a).edit(new ReminderSchedule(true,8,45,42));assertTrue(model(a).save(false,false));model(a).permissionResult(false,false);
                assertEquals(ReminderViewModel.Feedback.PERMISSION,model(a).current().feedback);assertEquals(before,new SettingsPreferenceManager(a).readReminderSchedule());
                assertFalse(model(a).save(true,false));assertEquals(ReminderViewModel.Feedback.PERMISSION,model(a).current().feedback);
                assertEquals(before,new SettingsPreferenceManager(a).readReminderSchedule());model(a).discard();});
            scenario.onActivity(a->{draft(a,!before.enabled,(before.hour+1)%24,before.minute,127);a.getOnBackPressedDispatcher().onBackPressed();});settle();clickDialog("Bỏ thay đổi");
            assertEquals(before,new SettingsPreferenceManager(context()).readReminderSchedule());
        }
    }
    @Test public void drawerHasOnlyThreeRowsAndSettingsHasFourCardsWithWorkingRoutes() throws Exception {
        try(ActivityScenario<CaiDatActivity> scenario=ActivityScenario.launch(CaiDatActivity.class)) {
            scenario.onActivity(a->{assertNotNull(a.findViewById(R.id.settingsStudy));assertNotNull(a.findViewById(R.id.settingsAudio));assertNotNull(a.findViewById(R.id.settingsReminder));assertNotNull(a.findViewById(R.id.settingsAbout));
                assertEquals(0,countText(a.findViewById(android.R.id.content),"CÀI ĐẶT"));assertEquals(0,countText(a.findViewById(android.R.id.content),"Giao diện"));a.findViewById(R.id.buttonNavigationAvatar).performClick();});settle();
            scenario.onActivity(a->{View panel=a.findViewById(R.id.navigationDrawerPanel);assertNotNull(panel.findViewById(R.id.navProfile));assertNotNull(panel.findViewById(R.id.navSettings));assertNotNull(panel.findViewById(R.id.navLogout));
                assertEquals(0,countText(panel,"BiVocal"));for(String label:new String[]{"Tổng quan","Chủ đề từ vựng","Ôn tập hôm nay","Làm Quiz","Tiến độ học tập","Bảng xếp hạng"})assertEquals(label,0,countText(panel,label));
                assertEquals(a.getString(R.string.nav_greeting,new SessionManager(a).getUsername()),((TextView)a.findViewById(R.id.textNavUser)).getText().toString());});
            shot("drawer.png");scenario.onActivity(a->a.findViewById(R.id.buttonNavClose).performClick());settle();
            scenario.onActivity(a->assertFalse(((DrawerLayout)a.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));shot("settings.png");
            scenario.onActivity(a->a.findViewById(R.id.buttonNavigationAvatar).performClick());settle();scenario.onActivity(a->a.findViewById(R.id.navProfile).performClick());
            Activity profile=awaitActivity(HoSoCaNhanActivity.class);main(()->profile.findViewById(R.id.buttonNavigationAvatar).performClick());settle();
            main(()->profile.findViewById(R.id.navSettings).performClick());awaitActivity(CaiDatActivity.class);
        }
    }
    @Test public void responsiveControlsScrollAndVietnameseGlyphsRemainReadable() throws Exception {
        try(ActivityScenario<CaiDatActivity> scenario=ActivityScenario.launch(CaiDatActivity.class)) {
            orient(scenario);
            shot("settings-responsive.png");scenario.onActivity(a->a.findViewById(R.id.buttonNavigationAvatar).performClick());settle();shot("drawer-responsive.png");
        }
        try(ActivityScenario<ChiTietCaiDatActivity> scenario=reminder()) {
            orient(scenario);
            shot("reminder-top.png");
            scenario.onActivity(a->{View root=a.findViewById(R.id.groupReminder);ScrollView scroll=scrollParent(root);scroll.fullScroll(View.FOCUS_DOWN);});settle();
            scenario.onActivity(a->{View grid=a.findViewById(R.id.gridReminderDays);assertTrue(((android.widget.GridLayout)grid).getColumnCount()>=2);assertTrue(((android.widget.GridLayout)grid).getColumnCount()<=4);
                for(int id:IDS){View v=a.findViewById(id);assertTrue("day touch width",v.getWidth()>=48*a.getResources().getDisplayMetrics().density);assertTrue("day touch height",v.getHeight()>=48*a.getResources().getDisplayMetrics().density);Rect rect=new Rect();assertTrue("all day chips reachable",v.getGlobalVisibleRect(rect));assertTrue("chips fit grid",v.getRight()<=grid.getWidth());}});
            shot("reminder-days.png");
        }
        if(InstrumentationRegistry.getArguments().getString("glyphs","false").equals("true")) {
            try(ActivityScenario<CaiDatActivity> scenario=ActivityScenario.launch(CaiDatActivity.class)) {
                for(int font:new int[]{R.font.be_vietnam_pro_regular,R.font.be_vietnam_pro_medium,R.font.be_vietnam_pro_semibold,R.font.be_vietnam_pro_bold,R.font.be_vietnam_pro_extrabold}) {
                    scenario.onActivity(a->{ScrollView scroll=new ScrollView(a);LinearLayout list=new LinearLayout(a);list.setOrientation(LinearLayout.VERTICAL);int p=(int)(20*a.getResources().getDisplayMetrics().density);list.setPadding(p,p*3,p,p*2);list.setBackgroundColor(a.getColor(R.color.bivocal_light_surface));
                        for(String text:new String[]{"KIỂM TRA FONT • CHỈ DÙNG TRONG TEST • "+a.getResources().getResourceEntryName(font).replace("be_vietnam_pro_",""),"Xin chào, bishadow27","Hồ sơ cá nhân","Cài đặt","Đăng xuất","Nhắc tôi học từ vựng","Thời gian nhắc","Ngày nhắc","Lưu giờ nhắc","Trắc nghiệm","Thuần thục"}) {TextView v=new TextView(a);v.setText(text);v.setTextSize(16);v.setTextColor(a.getColor(R.color.bivocal_light_text));v.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(a,font));v.setPadding(0,8,0,8);list.addView(v);}scroll.addView(list);a.setContentView(scroll);});shot("glyphs-"+font+".png");
                }
            }
        }
    }
    private static <T extends Activity> void orient(ActivityScenario<T> scenario) {
        boolean landscape=InstrumentationRegistry.getArguments().getString("config","").startsWith("landscape");
        scenario.onActivity(a->a.setRequestedOrientation(landscape?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        int desired=landscape?android.content.res.Configuration.ORIENTATION_LANDSCAPE:android.content.res.Configuration.ORIENTATION_PORTRAIT;
        boolean[] ready={false};long end=SystemClock.uptimeMillis()+8000;
        do{settle();scenario.onActivity(a->ready[0]=a.getResources().getConfiguration().orientation==desired);if(ready[0])break;}while(SystemClock.uptimeMillis()<end);
        assertTrue("Actual Activity orientation must match screenshot label",ready[0]);
    }
    @Test public void durabilityAcrossProcessRuns() throws Exception {
        String stage=InstrumentationRegistry.getArguments().getString("durability","skip");
        if(stage.equals("skip"))return;
        File file=new File(context().getCacheDir(),"reminder-refinement-durability.dat");
        int owner=new SessionManager(context()).getUserId();assertTrue(owner>0);
        SharedPreferences prefs=context().getSharedPreferences("bivoca_user_settings_"+owner,Context.MODE_PRIVATE);
        String[] keys={"reminder_enabled","reminder_hour","reminder_minute","reminder_days"};
        if(stage.equals("write")) {
            assertTrue("Never overwrite an outstanding test backup",file.createNewFile());
            try(java.io.DataOutputStream out=new java.io.DataOutputStream(new FileOutputStream(file))) {
                out.writeInt(owner);for(int i=0;i<keys.length;i++){out.writeBoolean(prefs.contains(keys[i]));out.writeInt(i==0?(prefs.getBoolean(keys[i],false)?1:0):prefs.getInt(keys[i],0));}
            }
            boolean passed=false;
            try(ActivityScenario<ChiTietCaiDatActivity> scenario=reminder()) {
                scenario.onActivity(a->{draft(a,false,22,41,42);a.findViewById(R.id.buttonSaveReminder).performClick();});awaitSaved(scenario);passed=true;
            } finally {if(!passed)restoreDurability(file,prefs,owner,keys);}
        } else if(stage.equals("read")) {
            assertTrue(file.isFile());
            try(ActivityScenario<ChiTietCaiDatActivity> scenario=reminder()) {
                scenario.onActivity(a->{assertEquals(new ReminderSchedule(false,22,41,42),new SettingsPreferenceManager(a).readReminderSchedule());assertEquals(new ReminderSchedule(false,22,41,42),model(a).current().draft);});
            } finally {restoreDurability(file,prefs,owner,keys);}
        } else throw new AssertionError("Unknown durability stage");
    }
    private static void restoreDurability(File file,SharedPreferences prefs,int owner,String[] keys)throws Exception {
        try(java.io.DataInputStream in=new java.io.DataInputStream(new java.io.FileInputStream(file))) {
            assertEquals("Restore only the original account",owner,in.readInt());SharedPreferences.Editor editor=prefs.edit();
            for(int i=0;i<keys.length;i++){boolean present=in.readBoolean();int value=in.readInt();if(!present)editor.remove(keys[i]);else if(i==0)editor.putBoolean(keys[i],value!=0);else editor.putInt(keys[i],value);}
            assertTrue("Restore original durable fields",editor.commit());ReminderScheduler.reconcile(context());
        }
        assertTrue("Remove only this test backup",file.delete());
    }
    private static ScrollView scrollParent(View view){for(android.view.ViewParent p=view.getParent();p!=null;p=p.getParent())if(p instanceof ScrollView)return (ScrollView)p;throw new AssertionError("Scroll parent missing");}
    private static void assertAlarms(int mask){int owner=new SessionManager(context()).getUserId();for(int day=1;day<=7;day++){
        Intent i=new Intent(context(),ReminderReceiver.class).setAction("com.example.bivoca.ACTION_STUDY_REMINDER").setData(Uri.parse("bivoca://reminder/"+owner+"/"+day));
        PendingIntent p=PendingIntent.getBroadcast(context(),ReminderScheduler.requestCode(owner,day),i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
        assertEquals("Existing scheduler day registration "+day,SettingsPreferenceManager.isDaySelected(mask,day),p!=null);
    }}
    private static void clickDialog(String text){androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(text)).perform(androidx.test.espresso.action.ViewActions.click());settle();}
    private static int countText(View view,String text){int count=view instanceof TextView&&text.contentEquals(((TextView)view).getText())?1:0;if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)count+=countText(g.getChildAt(i),text);}return count;}
    private static Activity awaitActivity(Class<? extends Activity> type){Activity[] found={null};long end=SystemClock.uptimeMillis()+12000;do{main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(a))found[0]=a;});if(found[0]!=null){settle();return found[0];}SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<end);throw new AssertionError("Route did not open "+type.getSimpleName());}
    private static void shot(String name)throws Exception{settle();String config=InstrumentationRegistry.getArguments().getString("config","font1");File directory=new File(context().getExternalFilesDir(null),"refinement-evidence/"+config);assertTrue(directory.isDirectory()||directory.mkdirs());Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);try(FileOutputStream out=new FileOutputStream(new File(directory,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();}
    private static final class ReminderBackup implements AutoCloseable {
        final int owner=new SessionManager(context()).getUserId();final SharedPreferences preferences=context().getSharedPreferences("bivoca_user_settings_"+owner,Context.MODE_PRIVATE);
        final Map<String,?> before=preferences.getAll();final String[] keys={"reminder_enabled","reminder_hour","reminder_minute","reminder_days"};
        public void close(){SharedPreferences.Editor e=preferences.edit();for(String k:keys){Object v=before.get(k);if(!before.containsKey(k))e.remove(k);else if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else e.putInt(k,(Integer)v);}assertTrue("Original reminder restored",e.commit());ReminderScheduler.reconcile(context());for(String k:keys)assertEquals(before.get(k),preferences.getAll().get(k));}
    }
}
