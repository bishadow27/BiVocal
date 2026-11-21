package com.example.bivoca;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.ui.quiz.TracNghiemActivity;
import com.example.bivoca.ui.quiz.QuizViewModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** UI navigation and existing availability GET only; never starts/answers/abandons a session. */
@RunWith(AndroidJUnit4.class)
public class BrandRefreshQuizGetUiTest {
 private Object creation(QuizViewModel vm){try{java.lang.reflect.Field field=QuizViewModel.class.getDeclaredField("sessionCreation");field.setAccessible(true);return field.get(vm);}catch(ReflectiveOperationException e){throw new AssertionError("Cannot verify pending creation guard",e);}}
 @Test public void homeHeaderAndSelectedTabRemainClear()throws Exception{
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
  try(ActivityScenario<com.example.bivoca.ui.home.TrangChuActivity> scenario=ActivityScenario.launch(new Intent(c,com.example.bivoca.ui.home.TrangChuActivity.class))){
   scenario.onActivity(a->{assertNotNull(a.findViewById(R.id.homeHeaderWordmark));View marker=((android.view.ViewGroup)a.findViewById(R.id.homeTabHome)).getChildAt(0);assertEquals(a.getColor(R.color.brand_logo_accent),marker.getBackgroundTintList().getDefaultColor());});SystemClock.sleep(1500);LightColorfulUiTest.shot("brand-home-final.png");
  }
 }
 @Test public void sixMethodConfigurationsUseRealAvailabilityWithoutStarting() throws Exception {
  Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();assertEquals("com.example.bivoca",c.getPackageName());
  try(ActivityScenario<TracNghiemActivity> scenario=ActivityScenario.launch(new Intent(c,TracNghiemActivity.class))){
   scenario.onActivity(a->{QuizViewModel vm=a.getLearningViewModel();assertFalse("Do not disturb an active session",vm.sessionState.active());assertNull(vm.sessionState.pending);assertNull(vm.sessionState.pendingControl);assertNull(creation(vm));if(vm.sessionScreen==QuizViewModel.SessionScreen.RESULTS)vm.newSessionV2();assertEquals(QuizViewModel.SessionScreen.METHODS,vm.sessionScreen);});
   LightColorfulUiTest.shot("brand-quiz-methods.png");
   for(String method:new String[]{"MIXED","CHALLENGE","CONTEXT","EN_TO_VI","VI_TO_EN","MATCHING"}){
    scenario.onActivity(a->{View card=a.findViewById(R.id.quizV2Content).findViewWithTag("method:"+method);assertNotNull(card);assertTrue(card.performClick());});
    long end=SystemClock.uptimeMillis()+90000;boolean[] ready={false};
    do{scenario.onActivity(a->{QuizViewModel vm=a.getLearningViewModel();assertNull("No session creation",creation(vm));assertEquals(QuizViewModel.SessionScreen.CONFIGURATION,vm.sessionScreen);if(!vm.sessionAvailabilityLoading&&vm.sessionAvailability!=null){assertTrue(vm.sessionAvailability.quizEligibleCount>=0);ready[0]=true;}else if(!vm.sessionAvailabilityLoading&&vm.sessionError!=null)throw new AssertionError("Availability GET failed: "+vm.sessionErrorCode);});if(!ready[0])SystemClock.sleep(200);}while(!ready[0]&&SystemClock.uptimeMillis()<end);
    assertTrue(method+" availability GET timeout",ready[0]);LightColorfulUiTest.shot("brand-quiz-config-"+method.toLowerCase()+".png");
    scenario.onActivity(a->a.getLearningViewModel().backSessionV2());
   }
  }
 }
}

