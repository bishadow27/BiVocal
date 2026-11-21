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
public class QuizFunctionalAiTest {
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
            com.google.gson.JsonObject record=new com.google.gson.JsonObject();record.addProperty("screen",vm().sessionScreen.name());record.addProperty("capturedAtMs",System.currentTimeMillis());record.addProperty("screenWidthDp",activity.getResources().getConfiguration().screenWidthDp);record.addProperty("fontScale",activity.getResources().getConfiguration().fontScale);record.addProperty("durationSeconds",vm().sessionState.durationSeconds(System.currentTimeMillis()));record.add("session",new Gson().toJsonTree(vm().sessionState.session));record.add("questions",new Gson().toJsonTree(vm().sessionState.questions));metadata.set(record);
        });
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".json"))){output.write(metadata.get().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        try(FileOutputStream output=new FileOutputStream(new File(dir,name+".png"))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,output));}bitmap.recycle();
    }
    private void completeCurrent(String name) throws Exception {
        int done=0;java.util.Map<String,Integer> kinds=new java.util.HashMap<>();
        while(vm().sessionState.active()){
            Question q=vm().sessionState.current();int seen=kinds.getOrDefault(q.kind,0);kinds.put(q.kind,seen+1);
            boolean valid="CHALLENGE".equals(vm().sessionMethod)?done==0:"MATCHING".equals(vm().sessionMethod)?done!=0:seen%2==0;
            answer(valid);done++;capture(name+"-ack-"+done);
            if(vm().sessionScreen==QuizViewModel.SessionScreen.QUESTIONS){if("MATCHING".equals(vm().sessionMethod)&&vm().sessionState.active()&&vm().sessionState.session.answeredCount%4!=0){Question next=null;for(Question candidate:vm().sessionState.questions)if(candidate.roundIndex.equals(q.roundIndex)&&vm().sessionState.receipt(candidate.questionId)==null){next=candidate;break;}assertNotNull(next);clickTag("left:"+next.questionId);}else{click(R.id.quizV2Next);idle();}}
        }
        if(vm().sessionScreen!=QuizViewModel.SessionScreen.RESULTS){main(()->vm().showSessionResults());idle();}
        capture(name+"-result");
        int incorrect=vm().sessionState.incorrect().size();
        if(incorrect>0){main(()->assertTrue(((TextView)activity.findViewById(R.id.quizV2ReviewToggle)).getText().toString().contains("MATCHING".equals(vm().sessionMethod)?"cặp sai":"câu sai")));click(R.id.quizV2ReviewToggle);capture(name+"-review-open");click(R.id.quizV2ReviewToggle);capture(name+"-review-closed");}
        assertEquals("CHALLENGE".equals(vm().sessionMethod)?"INCORRECT":"COMPLETED_ALL",vm().sessionState.session.finishReason);
    }
    @Test public void sixMethodsAndResults() throws Exception {
        assertNull("Fresh baseline must not silently replace old session",vm().sessionState.session);
        for(int count:new int[]{0,5,15,17,20,40,60,80,100}){
            if(vm().sessionScreen==QuizViewModel.SessionScreen.CONFIGURATION){main(()->vm().backSessionV2());idle();}
            clickTag("method:EN_TO_VI");idle();main(()->vm().chooseSessionTopic("V2_COUNT_"+count,"Synthetic "+count));idle();assertEquals(count,vm().sessionAvailability.quizEligibleCount);
            if(count==0){assertEquals(0,vm().sessionCount);main(()->assertFalse(activity.findViewById(R.id.quizV2Start).isEnabled()));}else assertEquals(count<20?count:20,vm().sessionCount);
            capture("capacity-"+count);
        }
        main(()->vm().backSessionV2());idle();clickTag("method:EN_TO_VI");idle();clickTag("topic:POS_V2_NOUN");idle();assertEquals(100,vm().sessionAvailability.quizEligibleCount);capture("capacity-pos");main(()->vm().backSessionV2());idle();
        for(String method:QuizSessionScreen.METHODS){method(method,"V2_COUNT_100");start();assertEquals(20,vm().sessionState.session.selectedUnitCount);assertEquals("MATCHING".equals(method)?"PAIR":"QUESTION",vm().sessionState.session.unit);capture(method+"-created");completeCurrent(method);}
    }
    @Test public void resumeApprovedCreationAndFinishSixMethods() throws Exception {
        assertNotNull(vm().sessionState.session);assertEquals("MIXED",vm().sessionState.session.method);assertEquals(20,vm().sessionState.session.selectedUnitCount);assertEquals(0,vm().sessionState.session.answeredCount);capture("MIXED-created-after-exact-retry");completeCurrent("MIXED");
        for(String method:QuizSessionScreen.METHODS){if("MIXED".equals(method))continue;method(method,"V2_COUNT_100");start();assertEquals(20,vm().sessionState.session.selectedUnitCount);assertEquals("MATCHING".equals(method)?"PAIR":"QUESTION",vm().sessionState.session.unit);capture(method+"-created");completeCurrent(method);}
    }
    @Test public void retryPendingAnswerAndFinishSuite() throws Exception {
        assertNotNull(vm().sessionState.session);assertEquals("MIXED",vm().sessionState.session.method);assertEquals(0,vm().sessionState.session.answeredCount);assertNotNull(vm().sessionState.pending);Answer pending=new Gson().fromJson(new Gson().toJson(vm().sessionState.pending),Answer.class);capture("pending-answer-before-permission-retry");
        main(()->vm().retrySessionV2());idle();assertNull(vm().sessionState.pending);assertEquals(pending.actionId,vm().sessionState.receipt(pending.questionId).actionId);assertEquals(1,vm().sessionState.session.answeredCount);capture("first-answer-exact-retry-ack");click(R.id.quizV2Next);idle();completeCurrent("MIXED");
        for(String method:QuizSessionScreen.METHODS){if("MIXED".equals(method))continue;method(method,"V2_COUNT_100");start();assertEquals(20,vm().sessionState.session.selectedUnitCount);assertEquals("MATCHING".equals(method)?"PAIR":"QUESTION",vm().sessionState.session.unit);capture(method+"-created");completeCurrent(method);}
    }
    @Test public void matchingFiveFifteenAndChallengeFirstWrong() throws Exception {
        if(vm().sessionState.active()){
            assertEquals("MATCHING",vm().sessionState.session.method);assertEquals(5,vm().sessionState.session.selectedUnitCount);assertEquals(2,vm().sessionState.session.answeredCount);String existing=vm().sessionState.session.sessionId;Question answered=vm().sessionState.questions.get(3);assertNotNull(vm().sessionState.receipt(answered.questionId));assertEquals("QUESTION_ALREADY_ANSWERED",post("quiz/sessions/"+existing+"/answers",answerRequest(answered,vm().sessionState.session.version),409).getAsJsonObject("error").get("code").getAsString());
            for(int ordinal:new int[]{2,1}){final int index=ordinal;main(()->vm().chooseMatchingLeft(index));answer(true);capture("matching5-resumed-ack-"+vm().sessionState.session.answeredCount);}click(R.id.quizV2Next);idle();answer(true);capture("matching5-resumed-last-ack");click(R.id.quizV2Next);idle();assertEquals(5,vm().sessionState.session.answeredCount);assertEquals(4,vm().sessionState.session.correctCount);capture("matching5-result");click(R.id.quizV2ReviewToggle);capture("matching5-review");click(R.id.quizV2ReviewToggle);
        }
        for(int count:new int[]{15}){
            method("MATCHING","V2_COUNT_"+count);start();assertEquals(count,vm().sessionState.questions.size());String sessionId=vm().sessionState.session.sessionId;Question future=vm().sessionState.questions.get(4);assertEquals("QUESTION_OUT_OF_ORDER",post("quiz/sessions/"+sessionId+"/answers",answerRequest(future,0),409).getAsJsonObject("error").get("code").getAsString());
            for(int round=0;round*4<count;round++){
                int start=round*4,limit=Math.min(4,count-start);int[] order=limit==4?new int[]{3,0,2,1}:limit==3?new int[]{2,0,1}:new int[]{0};
                for(int pos:order){final int ordinal=start+pos;main(()->vm().chooseMatchingLeft(ordinal));answer(!(round==0&&pos==3));if(round==0&&pos==3){Question answered=vm().sessionState.questions.get(ordinal);assertEquals("QUESTION_ALREADY_ANSWERED",post("quiz/sessions/"+sessionId+"/answers",answerRequest(answered,vm().sessionState.session.version),409).getAsJsonObject("error").get("code").getAsString());}capture("matching"+count+"-ack-"+(vm().sessionState.session.answeredCount));}
                click(R.id.quizV2Next);idle();
            }
            assertEquals(count,vm().sessionState.session.answeredCount);assertEquals(count-1,vm().sessionState.session.correctCount);capture("matching"+count+"-result");click(R.id.quizV2ReviewToggle);capture("matching"+count+"-review");click(R.id.quizV2ReviewToggle);
        }
        method("CHALLENGE","V2_COUNT_100");start();answer(false);idle();assertEquals("INCORRECT",vm().sessionState.session.finishReason);assertEquals(1,vm().sessionState.session.answeredCount);capture("challenge-first-wrong");String id=vm().sessionState.session.sessionId;assertEquals("SESSION_TERMINAL",post("quiz/sessions/"+id+"/answers",answerRequest(vm().sessionState.questions.get(1),vm().sessionState.session.version),409).getAsJsonObject("error").get("code").getAsString());
    }
    @Test public void beginResumeScenario() throws Exception {
        method("EN_TO_VI","V2_COUNT_100");start();answer(true);capture("resume-before-recreate");String session=vm().sessionState.session.sessionId;int version=vm().sessionState.session.version;QuizViewModel retained=vm();
        main(()->activity.recreate());await(()->{for(Activity current:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(current instanceof TracNghiemActivity&&current!=activity){activity=(TracNghiemActivity)current;return true;}return false;},30000);idle();assertSame(retained,vm());assertEquals(session,vm().sessionState.session.sessionId);assertEquals(version,vm().sessionState.session.version);capture("resume-after-recreate");
    }
    @Test public void verifyFreshProcessResume() throws Exception {
        assertNotNull(vm().sessionState.session);assertEquals(1,vm().sessionState.session.answeredCount);assertNull(vm().sessionState.pending);capture("resume-fresh-process");
    }
    private com.google.gson.JsonObject lastCreation;
    private com.google.gson.JsonObject answerRequest(Question q,int version){com.google.gson.JsonObject body=new com.google.gson.JsonObject();body.addProperty("actionId",UUID.randomUUID().toString());body.addProperty("questionId",q.questionId);body.addProperty("selectedOptionId",correct(q).optionId);body.addProperty("expectedVersion",version);return body;}
    private com.google.gson.JsonObject post(String path,com.google.gson.JsonObject body,int expected)throws Exception{
        assertTrue(path.equals("quiz/sessions")||path.matches("quiz/sessions/[0-9a-f-]{36}/answers"));
        java.net.HttpURLConnection c=(java.net.HttpURLConnection)new java.net.URL(ApiBaseUrl.current()+path).openConnection();c.setRequestMethod("POST");c.setInstanceFollowRedirects(false);c.setConnectTimeout(60000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");c.setRequestProperty("Authorization","Bearer "+new SessionManager(context()).getAccessToken());
        try{try(OutputStream output=c.getOutputStream()){output.write(body.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}int status=c.getResponseCode();com.google.gson.JsonObject response;try(InputStream input=status<400?c.getInputStream():c.getErrorStream()){response=com.google.gson.JsonParser.parseReader(new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
            com.google.gson.JsonObject record=new com.google.gson.JsonObject();record.addProperty("path",path);record.addProperty("status",status);record.addProperty("replay",c.getHeaderField("Idempotent-Replay"));record.add("request",body.deepCopy());record.add("response",response.deepCopy());File dir=new File(context().getFilesDir(),"m16-7/"+prefix);assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,"direct-http-"+UUID.randomUUID()+".json"))){out.write(record.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}assertEquals("Unexpected HTTP status",expected,status);return response;
        }finally{c.disconnect();}
    }
    @Test public void idempotencyAndConcurrentDuplicate()throws Exception{
        method("EN_TO_VI","V2_COUNT_100");
        main(()->{View button=activity.findViewById(R.id.quizV2Start);button.performClick();try{java.lang.reflect.Field field=QuizViewModel.class.getDeclaredField("sessionCreation");field.setAccessible(true);lastCreation=new Gson().toJsonTree(field.get(vm())).getAsJsonObject();}catch(Exception e){throw new AssertionError("Creation snapshot unavailable",e);}});idle();assertNotNull(lastCreation);
        String id=vm().sessionState.session.sessionId;com.google.gson.JsonObject replay=post("quiz/sessions",lastCreation,200);assertEquals(id,replay.get("sessionId").getAsString());com.google.gson.JsonObject mismatch=lastCreation.deepCopy();mismatch.addProperty("selectedUnitCount",40);assertEquals("CREATION_PAYLOAD_MISMATCH",post("quiz/sessions",mismatch,409).getAsJsonObject("error").get("code").getAsString());
        Question first=vm().sessionState.current();answer(true);Receipt original=vm().sessionState.receipt(first.questionId);com.google.gson.JsonObject old=answerRequest(first,0);old.addProperty("actionId",original.actionId);old.addProperty("selectedOptionId",original.selectedAnswer.optionId);com.google.gson.JsonObject same=post("quiz/sessions/"+id+"/answers",old,200);assertEquals(original.receiptId,same.get("receiptId").getAsString());assertEquals(original.awardedXp,same.get("awardedXp").getAsInt());
        com.google.gson.JsonObject bad=old.deepCopy();for(Option o:first.options)if(!o.optionId.equals(original.selectedAnswer.optionId)){bad.addProperty("selectedOptionId",o.optionId);break;}assertEquals("ACTION_PAYLOAD_MISMATCH",post("quiz/sessions/"+id+"/answers",bad,409).getAsJsonObject("error").get("code").getAsString());
        click(R.id.quizV2Next);idle();Question second=vm().sessionState.current();com.google.gson.JsonObject request=answerRequest(second,vm().sessionState.session.version);java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newFixedThreadPool(2);try{java.util.concurrent.Future<com.google.gson.JsonObject> one=executor.submit(()->post("quiz/sessions/"+id+"/answers",request,200));java.util.concurrent.Future<com.google.gson.JsonObject> two=executor.submit(()->post("quiz/sessions/"+id+"/answers",request,200));assertEquals(one.get(90,java.util.concurrent.TimeUnit.SECONDS),two.get(90,java.util.concurrent.TimeUnit.SECONDS));}finally{executor.shutdown();}
        main(()->vm().loadSessionV2());idle();assertEquals(2,vm().sessionState.session.answeredCount);assertEquals(2,vm().sessionState.session.version);capture("concurrent-duplicate-one-commit");
    }
    @Test public void recreateConcurrentSession()throws Exception{
        assertEquals(3,vm().sessionState.session.answeredCount);String id=vm().sessionState.session.sessionId;String questions=new Gson().toJson(vm().sessionState.questions);int version=vm().sessionState.session.version;QuizViewModel retained=vm();capture("resume-before-recreate");main(()->activity.recreate());await(()->{for(Activity current:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(current instanceof TracNghiemActivity&&current!=activity){activity=(TracNghiemActivity)current;return true;}return false;},30000);idle();assertSame(retained,vm());assertEquals(id,vm().sessionState.session.sessionId);assertEquals(version,vm().sessionState.session.version);assertEquals(questions,new Gson().toJson(vm().sessionState.questions));capture("resume-after-recreate");
    }
    @Test public void verifyConcurrentSessionInFreshProcess()throws Exception{assertNotNull(vm().sessionState.session);assertEquals(3,vm().sessionState.session.answeredCount);assertEquals(3,vm().sessionState.session.version);assertNull(vm().sessionState.pending);capture("resume-fresh-process");}
    @Test public void finishConcurrentSessionForAckLoss()throws Exception{assertEquals(3,vm().sessionState.session.answeredCount);while(vm().sessionState.receipt(vm().sessionState.current().questionId)!=null){click(R.id.quizV2Next);idle();}completeCurrent("pre-ack-loss-completed");}
    @Test public void capacitySessionBoundaries()throws Exception{
        assertFalse(vm().sessionState.active());
        for(int count:new int[]{5,15,17,20,40,60,80,100}){com.google.gson.JsonObject request=new com.google.gson.JsonObject();request.addProperty("creationId",UUID.randomUUID().toString());request.addProperty("method","EN_TO_VI");request.addProperty("selectedUnitCount",count);request.addProperty("useAllAvailable",count<20);com.google.gson.JsonObject filter=new com.google.gson.JsonObject();filter.addProperty("topicCode","V2_COUNT_"+count);request.add("filter",filter);com.google.gson.JsonObject response=post("quiz/sessions",request,201);assertEquals(count,response.get("selectedUnitCount").getAsInt());}
        for(int capacity:new int[]{0,15,17}){com.google.gson.JsonObject request=new com.google.gson.JsonObject();request.addProperty("creationId",UUID.randomUUID().toString());request.addProperty("method","EN_TO_VI");request.addProperty("selectedUnitCount",20);request.addProperty("useAllAvailable",false);com.google.gson.JsonObject filter=new com.google.gson.JsonObject();filter.addProperty("topicCode","V2_COUNT_"+capacity);request.add("filter",filter);assertEquals("INSUFFICIENT_ELIGIBLE",post("quiz/sessions",request,422).getAsJsonObject("error").get("code").getAsString());}
        capture("capacity-api-boundaries-complete");
    }
    @Test public void verifyExistingConcurrentSessionAndReplay()throws Exception{
        assertEquals(2,vm().sessionState.session.answeredCount);assertNull(vm().sessionState.pending);String id=vm().sessionState.session.sessionId;com.google.gson.JsonObject creation=com.google.gson.JsonParser.parseString(new String(java.nio.file.Files.readAllBytes(new File(context().getFilesDir(),"G-creation-request.json").toPath()),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();assertEquals(id,post("quiz/sessions",creation,200).get("sessionId").getAsString());com.google.gson.JsonObject createBad=creation.deepCopy();createBad.addProperty("selectedUnitCount",40);assertEquals("CREATION_PAYLOAD_MISMATCH",post("quiz/sessions",createBad,409).getAsJsonObject("error").get("code").getAsString());
        Question first=vm().sessionState.questions.get(0);Receipt original=vm().sessionState.receipt(first.questionId);com.google.gson.JsonObject replay=answerRequest(first,0);replay.addProperty("actionId",original.actionId);replay.addProperty("selectedOptionId",original.selectedAnswer.optionId);assertEquals(original.receiptId,post("quiz/sessions/"+id+"/answers",replay,200).get("receiptId").getAsString());com.google.gson.JsonObject bad=replay.deepCopy();for(Option o:first.options)if(!o.optionId.equals(original.selectedAnswer.optionId)){bad.addProperty("selectedOptionId",o.optionId);break;}assertEquals("ACTION_PAYLOAD_MISMATCH",post("quiz/sessions/"+id+"/answers",bad,409).getAsJsonObject("error").get("code").getAsString());
        Question third=vm().sessionState.questions.get(2);com.google.gson.JsonObject request=answerRequest(third,2);java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2);try{java.util.concurrent.Future<com.google.gson.JsonObject> one=pool.submit(()->post("quiz/sessions/"+id+"/answers",request,200));java.util.concurrent.Future<com.google.gson.JsonObject> two=pool.submit(()->post("quiz/sessions/"+id+"/answers",request,200));assertEquals(one.get(90,java.util.concurrent.TimeUnit.SECONDS),two.get(90,java.util.concurrent.TimeUnit.SECONDS));}finally{pool.shutdown();}
        main(()->vm().loadSessionV2());idle();assertEquals(3,vm().sessionState.session.answeredCount);assertEquals(3,vm().sessionState.session.version);capture("concurrent-duplicate-verified");
    }
    private void reveal(View v){v.requestRectangleOnScreen(new android.graphics.Rect(0,0,v.getWidth(),v.getHeight()),true);}
}









