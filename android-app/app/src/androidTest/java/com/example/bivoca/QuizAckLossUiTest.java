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
public class QuizAckLossUiTest {
    private TracNghiemActivity activity;
    private String prefix;
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private void main(Runnable run){InstrumentationRegistry.getInstrumentation().runOnMainSync(run);}
    @Before public void prepare() throws Exception {
        Assume.assumeTrue(BuildConfig.QUIZ_SESSION_V2_ENABLED);
        assertEquals("com.example.bivoca.e2e",context().getPackageName());assertEquals("http://10.0.2.2:3002/api/v1/",ApiBaseUrl.current());
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
        assertFalse("Test cannot abandon an active session",vm().sessionState.active());
        if(vm().sessionState.session!=null){main(()->vm().newSessionV2());idle();}
        clickTag("method:"+name);idle();clickTag("topic:"+topic);idle();
    }
    private void start(){click(R.id.quizV2Start);idle();assertEquals(QuizViewModel.SessionScreen.QUESTIONS,vm().sessionScreen);}
    private Option correct(Question q){
        Matcher m=Pattern.compile("(?:v2term|number\\s+|[Nn]ghĩa kiểm thử\\s+)(\\d+)").matcher(q.prompt);assertTrue("Expected synthetic fixture source",m.find());int n=Integer.parseInt(m.group(1));
        String answer=null;
        try {com.google.gson.JsonArray oracle=com.google.gson.JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"fixture-oracle.json").toPath()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            for(com.google.gson.JsonElement element:oracle){com.google.gson.JsonObject card=element.getAsJsonObject();if(card.get("english").getAsString().equals(String.format(Locale.ROOT,"v2term%03d",n)))answer=card.get("EN_TO_VI".equals(q.kind)||"MATCHING_PAIR".equals(q.kind)?"vietnamese":"english").getAsString();}
        }catch(Exception e){throw new AssertionError("Diagnostic fixture oracle required");}
        assertNotNull("Oracle card missing",answer);
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
        clickTag(tag);idle();assertNotNull(vm().sessionState.receipt(q.questionId));assertEquals("Server grade must match diagnostic fixture oracle",valid,vm().sessionState.receipt(q.questionId).correct);
    }
    private void capture(String name) throws Exception {
        File dir=new File(context().getFilesDir(),"m16-7/"+prefix);assertTrue(dir.isDirectory()||dir.mkdirs());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(200);
        AtomicReference<com.google.gson.JsonObject> metadata=new AtomicReference<>();main(()->{
            com.google.gson.JsonObject record=new com.google.gson.JsonObject();record.addProperty("screen",vm().sessionScreen.name());record.addProperty("capturedAtMs",System.currentTimeMillis());record.addProperty("screenWidthDp",activity.getResources().getConfiguration().screenWidthDp);record.addProperty("fontScale",activity.getResources().getConfiguration().fontScale);record.addProperty("durationSeconds",vm().sessionState.durationSeconds(System.currentTimeMillis()));record.add("session",new Gson().toJsonTree(vm().sessionState.session));metadata.set(record);
        });
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".json"))){output.write(metadata.get().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}bitmap.recycle();
    }
    private void write(String name,Object value)throws Exception{try(FileOutputStream out=new FileOutputStream(new File(context().getFilesDir(),name))){out.write(new Gson().toJson(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
    private String armed(){try{return new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"h-armed.txt").toPath()),java.nio.charset.StandardCharsets.UTF_8).trim();}catch(Exception e){return "";}}
    @Test public void realAckLossAfterCommit()throws Exception{
        if(vm().sessionState.active()){assertEquals("EN_TO_VI",vm().sessionState.session.method);assertEquals(20,vm().sessionState.session.selectedUnitCount);assertEquals(0,vm().sessionState.session.answeredCount);assertNull(vm().sessionState.pending);}else{method("EN_TO_VI","V2_COUNT_100");start();}Question q=vm().sessionState.current();String id=vm().sessionState.session.sessionId;com.google.gson.JsonObject ready=new com.google.gson.JsonObject();ready.addProperty("sessionId",id);ready.addProperty("questionId",q.questionId);ready.addProperty("qaUserId",new SessionManager(context()).getUserId());write("h-ready.json",ready);capture("h-session-ready");await(()->(id+":attempt2").equals(armed()),90000);
        Option selected=correct(q);AtomicReference<Answer> original=new AtomicReference<>();main(()->{View button=activity.findViewById(android.R.id.content).findViewWithTag(selected.optionId);assertNotNull(button);button.performClick();original.set(new Gson().fromJson(new Gson().toJson(vm().sessionState.pending),Answer.class));});assertNotNull(original.get());write("h-native-pending.json",original.get());
        await(()->!vm().sessionBusy,90000);boolean retryButton=false;
        if(vm().sessionState.pending!=null){assertEquals(original.get().actionId,vm().sessionState.pending.actionId);assertEquals(original.get().selectedOptionId,vm().sessionState.pending.selectedOptionId);assertEquals(original.get().expectedVersion,vm().sessionState.pending.expectedVersion);capture("h-lost-ack-pending");retryButton=true;click(R.id.quizV2Retry);idle();}
        Receipt receipt=vm().sessionState.receipt(q.questionId);assertNotNull(receipt);assertNull(vm().sessionState.pending);assertEquals(original.get().actionId,receipt.actionId);assertEquals(1,vm().sessionState.session.answeredCount);assertEquals(1,vm().sessionState.session.version);capture("h-retry-ack");com.google.gson.JsonObject result=new Gson().toJsonTree(receipt).getAsJsonObject();result.addProperty("retryButtonUsed",retryButton);result.addProperty("pendingCleared",true);write("h-native-result.json",result);
    }
    private void reveal(View v){v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true);}
}

