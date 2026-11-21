package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.ViewGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.data.api.ApiBaseUrl;
import com.example.bivoca.data.model.QuizSessionDtos.*;
import com.example.bivoca.ui.quiz.*;
import com.example.bivoca.utils.SessionManager;
import com.google.gson.Gson;
import com.google.android.material.button.MaterialButton;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Writes only through actual v2 HTTP, gated to E2E package/host and synthetic private login. */
@RunWith(AndroidJUnit4.class)
public class QuizSessionV2UiTest {
    private TracNghiemActivity activity;
    private String prefix;
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private void main(Runnable run){InstrumentationRegistry.getInstrumentation().runOnMainSync(run);}
    @Before public void prepare() throws Exception {
        Assume.assumeTrue(BuildConfig.QUIZ_SESSION_V2_ENABLED);
        assertEquals("com.example.bivoca.e2e",context().getPackageName());assertEquals("http://10.0.2.2:3001/api/v1/",ApiBaseUrl.current());
        assertEquals("Fresh E2E UI login must be explicitly confirmed before functional tests", "true",
                InstrumentationRegistry.getArguments().getString("freshSyntheticLoginConfirmed"));
        assertFalse("Legacy imported login files must not be consumed",
                new File(context().getFilesDir(),"quiz-v2-test-login.json").isFile());
        SessionManager syntheticSession=new SessionManager(context());
        assertTrue("Sign in through the E2E UI with a fresh synthetic account",syntheticSession.isLoggedIn());
        java.net.HttpURLConnection connection=(java.net.HttpURLConnection)
                new java.net.URL(ApiBaseUrl.current()+"auth/me").openConnection();
        connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
        connection.setRequestProperty("Authorization","Bearer "+syntheticSession.getAccessToken());
        try {
            assertEquals("Fresh synthetic authentication required",200,connection.getResponseCode());
            try(java.io.InputStream input=connection.getInputStream()) {
                com.google.gson.JsonObject user=com.google.gson.JsonParser.parseReader(
                        new java.io.InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                int authenticatedId=user.get("id").getAsInt();
                String email=user.get("email").getAsString();
                assertTrue(authenticatedId>0);
                assertTrue("Only approved synthetic accounts may run E2E",
                        "qa_ft_a@fixture.invalid".equals(email)||"qa_ft_b@fixture.invalid".equals(email));
                assertEquals(authenticatedId,syntheticSession.getUserId());
            }
        } finally { connection.disconnect(); }
        prefix=InstrumentationRegistry.getArguments().getString("capturePrefix","native");
        Intent intent=new Intent(context(),TracNghiemActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity=(TracNghiemActivity)InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        await(()->!vm().sessionBusy,60000);assertNoError();
    }
    private QuizViewModel vm(){return activity.getLearningViewModel();}
    private interface Check { boolean ready(); }
    private void await(Check check,long timeout){
        long start=SystemClock.elapsedRealtime();while(SystemClock.elapsedRealtime()-start<timeout){AtomicReference<Boolean> ready=new AtomicReference<>(false);main(()->ready.set(check.ready()));if(ready.get())return;SystemClock.sleep(150);}
        fail("Timed out waiting for native state; no token or answer payload logged");
    }
    private void assertNoError(){AtomicReference<String> error=new AtomicReference<>();main(()->error.set(vm().sessionError));assertNull(error.get(),error.get());}
    private void idle(){await(()->!vm().sessionBusy&&!vm().sessionAvailabilityLoading&&!vm().sessionCatalogLoading,60000);assertNoError();}
    private void clickTag(String tag){main(()->{View v=activity.findViewById(android.R.id.content).findViewWithTag(tag);assertNotNull("Missing native control "+tag,v);assertTrue(v.isEnabled());reveal(v);assertTrue(v.performClick());});}
    private void click(int id){main(()->{View v=activity.findViewById(id);assertNotNull(v);assertTrue("Disabled control",v.isEnabled());reveal(v);v.performClick();});}
    private void method(String name,String topic) {
        if(vm().sessionState.active()){main(()->vm().controlSessionV2(true));idle();}
        if(vm().sessionState.session!=null){main(()->vm().newSessionV2());idle();}
        clickTag("method:"+name);idle();clickTag("topic:"+topic);idle();
    }
    private void start(){click(R.id.quizV2Start);idle();assertEquals(QuizViewModel.SessionScreen.QUESTIONS,vm().sessionScreen);}
    private Option correct(Question q){
        Matcher m=Pattern.compile("(?:v2term|number\\s+|[Nn]ghĩa kiểm thử\\s+)(\\d+)").matcher(q.prompt);assertTrue("Expected synthetic fixture source",m.find());int n=Integer.parseInt(m.group(1));
        String answer="EN_TO_VI".equals(q.kind)||"MATCHING_PAIR".equals(q.kind)?String.format(Locale.ROOT,"Nghĩa kiểm thử %03d",n):String.format(Locale.ROOT,"v2term%03d",n);
        for(Option o:q.options)if(answer.equals(o.text))return o;throw new AssertionError("Fixture answer unavailable");
    }
    private void answer(boolean valid){
        Question q=vm().sessionState.current();Option good=correct(q),selected=good;
        if(!valid)for(Option option:q.options)if(!option.optionId.equals(good.optionId)){
            AtomicReference<Boolean> enabled=new AtomicReference<>(true);
            if("MATCHING_PAIR".equals(q.kind))main(()->{View v=activity.findViewById(android.R.id.content).findViewWithTag("right:"+option.optionId);enabled.set(v!=null&&v.isEnabled());});
            if(enabled.get()){selected=option;break;}
        }
        String tag="MATCHING_PAIR".equals(q.kind)?"right:"+selected.optionId:selected.optionId;
        clickTag(tag);idle();assertNotNull(vm().sessionState.receipt(q.questionId));
    }
    private void capture(String name) throws Exception {
        File dir=new File(context().getExternalFilesDir(null),"m16-7/"+prefix);assertTrue(dir.isDirectory()||dir.mkdirs());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(200);
        AtomicReference<com.google.gson.JsonObject> metadata=new AtomicReference<>();main(()->{
            com.google.gson.JsonObject record=new com.google.gson.JsonObject();record.addProperty("screen",vm().sessionScreen.name());record.addProperty("capturedAtMs",System.currentTimeMillis());record.addProperty("screenWidthDp",activity.getResources().getConfiguration().screenWidthDp);record.addProperty("fontScale",activity.getResources().getConfiguration().fontScale);record.addProperty("durationSeconds",vm().sessionState.durationSeconds(System.currentTimeMillis()));record.add("session",new Gson().toJsonTree(vm().sessionState.session));metadata.set(record);
        });
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".json"))){output.write(metadata.get().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}bitmap.recycle();
    }
    @Test public void configurationAndNativeGrid() throws Exception {
        String expectedWidth=InstrumentationRegistry.getArguments().getString("expectedWidth");
        String expectedFont=InstrumentationRegistry.getArguments().getString("expectedFont");
        if(expectedWidth!=null)assertTrue(Math.abs(Integer.parseInt(expectedWidth)-activity.getResources().getConfiguration().screenWidthDp)<=1);
        if(expectedFont!=null)assertEquals(Float.parseFloat(expectedFont),activity.getResources().getConfiguration().fontScale,0.01f);
        method("EN_TO_VI","V2_COUNT_15");assertEquals(15,vm().sessionCount);assertEquals(15,vm().sessionAvailability.quizEligibleCount);capture("configuration-fallback15");
        main(()->vm().chooseSessionTopic("V2_COUNT_0","Synthetic empty"));idle();assertEquals(0,vm().sessionCount);main(()->assertFalse(activity.findViewById(R.id.quizV2Start).isEnabled()));capture("configuration-empty");
        main(()->vm().chooseSessionTopic("V2_COUNT_100","Synthetic100"));idle();assertEquals(20,vm().sessionCount);capture("configuration100");start();capture("mc-front");
        main(()->{
            int[] ids={R.id.quizV2AnswerA,R.id.quizV2AnswerB,R.id.quizV2AnswerC,R.id.quizV2AnswerD};int height=activity.findViewById(ids[0]).getHeight();
            for(int id:ids){View cell=activity.findViewById(id);assertEquals(height,cell.getHeight());assertTrue(height>=Math.round((activity.getResources().getConfiguration().fontScale>=1.3f?84:76)*activity.getResources().getDisplayMetrics().density));}
        });
        String session=vm().sessionState.session.sessionId;QuizViewModel retained=vm();main(()->activity.recreate());
        await(()->{for(Activity current:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(current instanceof TracNghiemActivity&&current!=activity){activity=(TracNghiemActivity)current;return true;}return false;},30000);
        idle();assertSame(retained,vm());assertEquals(session,vm().sessionState.session.sessionId);capture("mc-recreated");
        main(()->vm().controlSessionV2(true));idle();capture("zero-answer-abandoned-result");
    }
    @Test public void sixMethodsRealAcknowledgements() throws Exception {
        if(vm().sessionState.active()){main(()->vm().controlSessionV2(true));idle();}if(vm().sessionState.session!=null){main(()->vm().newSessionV2());idle();}
        capture("methods-top");main(()->{View card=activity.findViewById(android.R.id.content).findViewWithTag("method:MATCHING");card.requestRectangleOnScreen(new android.graphics.Rect(0,0,card.getWidth(),card.getHeight()),true);});capture("methods-bottom");
        for(String method:QuizSessionScreen.METHODS){
            method(method,"V2_COUNT_15");start();capture(method.toLowerCase(Locale.ROOT)+"-question");
            boolean challenge="CHALLENGE".equals(method);
            if("MATCHING".equals(method)){main(()->vm().chooseMatchingLeft(3));answer(false);main(()->vm().chooseMatchingLeft(0));answer(true);}
            else answer(!challenge);
            capture(method.toLowerCase(Locale.ROOT)+"-ack");
            Receipt receipt=vm().sessionState.receipt(vm().sessionState.current().questionId);assertNotNull(receipt);
            int version=vm().sessionState.session.version;int count=vm().sessionState.session.answeredCount;
            main(()->{Question q=vm().sessionState.current();vm().answerSessionV2(q.questionId,q.options.get(0).optionId);});assertEquals(count,vm().sessionState.session.answeredCount);assertEquals(version,vm().sessionState.session.version);
            if(challenge){assertEquals("INCORRECT",vm().sessionState.session.finishReason);assertEquals(1,count);}
            else {main(()->vm().controlSessionV2(true));idle();}
            capture(method.toLowerCase(Locale.ROOT)+"-result");click(R.id.quizV2ReviewToggle);capture(method.toLowerCase(Locale.ROOT)+"-review");
        }
    }
    @Test public void completeTwentyAndMatchingFifteen() throws Exception {
        method("EN_TO_VI","V2_COUNT_20");start();
        for(int i=0;i<20;i++){answer(i!=1);if(i<19)click(R.id.quizV2Next);}
        click(R.id.quizV2Next);idle();assertEquals(20,vm().sessionState.session.answeredCount);assertEquals(19,vm().sessionState.session.correctCount);assertEquals("COMPLETED_ALL",vm().sessionState.session.finishReason);capture("completed20");click(R.id.quizV2ReviewToggle);capture("completed20-review");click(R.id.quizV2Complete);idle();
        method("MATCHING","V2_COUNT_15");start();
        for(int round=0;round<4;round++){
            int end=Math.min(15,round*4+4);
            for(int i=round*4;i<end;i++){final int ordinal=i;main(()->vm().chooseMatchingLeft(ordinal));answer(i!=2);}
            capture("matching-round-"+round);click(R.id.quizV2Next);if(round==3)idle();
        }
        assertEquals(15,vm().sessionState.session.answeredCount);assertEquals(14,vm().sessionState.session.correctCount);assertEquals("COMPLETED_ALL",vm().sessionState.session.finishReason);capture("matching-complete15");
    }
    @Test public void resumeInFreshTargetProcess() throws Exception {
        assertNotNull(vm().sessionState.session);capture("fresh-process-resume");
        assertEquals(vm().sessionState.session.answeredCount,vm().sessionState.session.receipts.stream().filter(r->"ANSWER".equals(r.operation)).count());
    }
    @Test public void matchingFiveUsesFourPlusOneRounds() throws Exception {
        method("MATCHING","V2_COUNT_5");assertEquals(5,vm().sessionCount);start();
        for(int i:new int[]{3,0,2,1}){final int ordinal=i;main(()->vm().chooseMatchingLeft(ordinal));answer(i!=0);}
        capture("matching-four");click(R.id.quizV2Next);assertEquals(4,vm().sessionState.current().ordinal);answer(true);capture("matching-last-one");click(R.id.quizV2Next);idle();assertEquals(5,vm().sessionState.session.answeredCount);assertEquals(4,vm().sessionState.session.correctCount);capture("matching5-result");
    }
    private void phase(String value) throws Exception {
        try(FileOutputStream out=new FileOutputStream(new File(context().getFilesDir(),"quiz-v2-native-phase.txt"))){out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    private String control() {
        try{return new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"quiz-v2-native-control.txt").toPath()),java.nio.charset.StandardCharsets.UTF_8).trim();}catch(Exception ignored){return "";}
    }
    @Test public void createPendingWhileNetworkDrops() throws Exception {
        method("EN_TO_VI","V2_COUNT_15");click(R.id.quizV2Start);capture("creation-loading");idle();
        phase("NETWORK_READY");await(()->"OFFLINE".equals(control()),60000);
        Question q=vm().sessionState.current();Option option=correct(q);clickTag(option.optionId);
        await(()->!vm().sessionBusy&&vm().sessionState.pending!=null&&vm().sessionError!=null,60000);
        Answer pending=vm().sessionState.pending;assertNotNull(pending);assertEquals(0,vm().sessionState.session.version);
        try(FileOutputStream out=new FileOutputStream(new File(context().getFilesDir(),"quiz-v2-pending-evidence.json"))){com.google.gson.JsonObject record=new Gson().toJsonTree(pending).getAsJsonObject();record.addProperty("processId",android.os.Process.myPid());out.write(record.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        capture("network-error-pending");phase("PENDING_SAVED");
    }
    @Test public void pendingSurvivesFreshProcessThenRetriesExactAction() throws Exception {
        Answer expected=new Gson().fromJson(new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"quiz-v2-pending-evidence.json").toPath()),java.nio.charset.StandardCharsets.UTF_8),Answer.class);
        com.google.gson.JsonObject original=com.google.gson.JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"quiz-v2-pending-evidence.json").toPath()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        assertNotEquals(original.get("processId").getAsInt(),android.os.Process.myPid());
        assertNotNull(vm().sessionState.pending);assertEquals(expected.actionId,vm().sessionState.pending.actionId);assertEquals(expected.selectedOptionId,vm().sessionState.pending.selectedOptionId);assertEquals(expected.expectedVersion,vm().sessionState.pending.expectedVersion);
        capture("pending-after-process-recreation");click(R.id.quizV2Retry);main(()->vm().retrySessionV2());idle();
        assertNull(vm().sessionState.pending);Receipt receipt=vm().sessionState.receipt(expected.questionId);assertNotNull(receipt);assertEquals(expected.actionId,receipt.actionId);assertEquals(1,vm().sessionState.session.answeredCount);capture("pending-retry-ack");
        main(()->vm().controlSessionV2(true));idle();phase("NETWORK_TEST_COMPLETE");
    }
    @Test public void longVietnameseTextExpandsEquallyWithoutClipping() {
        main(()->{
            android.content.res.Configuration configuration=new android.content.res.Configuration(activity.getResources().getConfiguration());configuration.fontScale=1.3f;
            android.view.ContextThemeWrapper themed=new android.view.ContextThemeWrapper(activity.createConfigurationContext(configuration),R.style.Theme_BiVoca);
            View grid=android.view.LayoutInflater.from(themed).inflate(R.layout.view_quiz_answer_grid,null,false);
            String longText="A\nMột khái niệm mô tả những trải nghiệm trong cuộc sống, giao tiếp và công việc; đầy đủ dấu tiếng Việt: ă â ê ô ơ ư đ, hỏi ngã nặng.";
            int[] ids={R.id.quizV2AnswerA,R.id.quizV2AnswerB,R.id.quizV2AnswerC,R.id.quizV2AnswerD};
            for(int i=0;i<4;i++)((TextView)grid.findViewById(ids[i])).setText(i==0?longText:"Đáp án ngắn");
            int width=Math.round(320*themed.getResources().getDisplayMetrics().density);grid.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));grid.layout(0,0,width,grid.getMeasuredHeight());
            int height=grid.findViewById(ids[0]).getHeight();for(int id:ids)assertEquals(height,grid.findViewById(id).getHeight());
            TextView text=grid.findViewById(ids[0]);assertNotNull(text.getLayout());assertTrue(text.getLayout().getLineCount()>3);assertTrue(text.getLayout().getLineBottom(text.getLayout().getLineCount()-1)<=height-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        });
    }
    private void topicIntent(String code) {
        Intent intent=new Intent(context(),TracNghiemActivity.class).putExtra(TracNghiemActivity.EXTRA_TOPIC_CODE,code).putExtra(TracNghiemActivity.EXTRA_TOPIC_NAME,code).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        main(()->context().startActivity(intent));SystemClock.sleep(500);
    }
    @Test public void incomingTopicIntentPreservesActiveAndAppliesWhenIdle() throws Exception {
        method("EN_TO_VI","V2_COUNT_15");start();String id=vm().sessionState.session.sessionId;
        topicIntent("V2_COUNT_17");assertEquals(id,vm().sessionState.session.sessionId);
        main(()->vm().controlSessionV2(true));idle();main(()->vm().newSessionV2());idle();
        topicIntent("V2_COUNT_17");await(()->"V2_COUNT_17".equals(vm().sessionTopicCode),10000);
        clickTag("method:EN_TO_VI");idle();assertEquals(17,vm().sessionAvailability.quizEligibleCount);capture("topic-intent17");
    }
    @Test public void configurationFiltersQuantitiesAndStaleVersion() throws Exception {
        method("EN_TO_VI","V2_COUNT_15");
        for(int size:new int[]{17,20,40,60,80,100}){
            main(()->vm().chooseSessionTopic("V2_COUNT_"+size,"Synthetic"+size));idle();assertEquals(size,vm().sessionAvailability.quizEligibleCount);
            for(int n:new int[]{20,40,60,80,100}){AtomicReference<Boolean> enabled=new AtomicReference<>();main(()->{View chip=activity.findViewById(android.R.id.content).findViewWithTag("count:"+n);assertNotNull(chip);enabled.set(chip.isEnabled());});assertEquals(n<=size,enabled.get());}
        }
        clickTag("group:POS");clickTag("topic:POS_V2_NOUN");idle();assertEquals(100,vm().sessionAvailability.quizEligibleCount);
        main(()->((android.widget.EditText)activity.findViewById(R.id.quizV2Search)).setText("no such topic"));main(()->assertNull(activity.findViewById(android.R.id.content).findViewWithTag("topic:POS_V2_NOUN")));capture("search-empty");
        main(()->((android.widget.EditText)activity.findViewById(R.id.quizV2Search)).setText(""));clickTag("group:ALL");clickTag("topic:V2_COUNT_20");idle();
        main(()->vm().sessionAvailability.availabilityVersion="0000000000000000000000000000000000000000000000000000000000000000");click(R.id.quizV2Start);await(()->!vm().sessionBusy,60000);assertEquals("AVAILABILITY_CHANGED",vm().sessionErrorCode);assertNull(vm().sessionState.session);capture("stale-availability");click(R.id.quizV2Refresh);idle();assertEquals(20,vm().sessionCount);
    }
    private Activity resumed(){AtomicReference<Activity> ref=new AtomicReference<>();main(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))ref.set(a);});return ref.get();}
    @Test public void fourTabsAndDrawerRetainSession() throws Exception {
        method("EN_TO_VI","V2_COUNT_20");start();String id=vm().sessionState.session.sessionId;QuizViewModel retained=vm();
        int[] tabs={R.id.homeTabHome,R.id.buttonTopics,R.id.buttonReviewToday};
        for(int tab:tabs){
            click(tab);androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Giữ phiên và chuyển tab")).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(androidx.test.espresso.action.ViewActions.click());
            await(()->{for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(!(a instanceof TracNghiemActivity))return true;return false;},30000);
            Activity other=resumed();assertNotNull(other);main(()->{View button=other.findViewById(R.id.buttonQuiz);assertNotNull(button);button.performClick();});
            await(()->ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).contains(activity),30000);assertSame(retained,vm());assertEquals(id,vm().sessionState.session.sessionId);
        }
        click(R.id.buttonHomeMenu);await(()->((androidx.drawerlayout.widget.DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(androidx.core.view.GravityCompat.END),5000);
        main(()->activity.getOnBackPressedDispatcher().onBackPressed());await(()->!((androidx.drawerlayout.widget.DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(androidx.core.view.GravityCompat.END),5000);capture("tabs-drawer-retained");
        main(()->vm().controlSessionV2(true));idle();
    }
    private void reveal(View view){
        view.requestRectangleOnScreen(new android.graphics.Rect(0,0,view.getWidth(),view.getHeight()),true);
        android.graphics.Rect visible=new android.graphics.Rect();assertTrue("Control must be reachable through native scrolling",view.getGlobalVisibleRect(visible));assertTrue(visible.width()>0&&visible.height()>0);
    }
    @Test public void sparseAbandonedMatchingReviewUsesOnlyCommittedPairs() throws Exception {
        method("MATCHING","V2_COUNT_5");start();main(()->vm().chooseMatchingLeft(3));answer(false);String answered=vm().sessionState.questions.get(3).questionId;
        main(()->vm().controlSessionV2(true));idle();assertEquals(1,vm().sessionState.session.answeredCount);click(R.id.quizV2ReturnToSession);assertEquals(answered,vm().sessionState.current().questionId);capture("sparse-matching-readonly");
        click(R.id.quizV2Next);idle();assertEquals(QuizViewModel.SessionScreen.RESULTS,vm().sessionScreen);assertEquals(1,vm().sessionState.session.answeredCount);capture("sparse-matching-result");
    }
}
