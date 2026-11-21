package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.data.api.ApiBaseUrl;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.TopicApi;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.ui.home.TrangChuActivity;
import com.example.bivoca.ui.profile.ProfileViewModel;
import com.example.bivoca.ui.progress.ProgressViewModel;
import com.example.bivoca.ui.leaderboard.LeaderboardViewModel;
import com.example.bivoca.ui.topics.ChuDeActivity;
import com.example.bivoca.ui.topics.TopicsViewModel;
import com.example.bivoca.ui.topics.ChiTietChuDeActivity;
import com.example.bivoca.ui.topics.TopicDetailViewModel;
import com.example.bivoca.ui.flashcard.TheTuVungActivity;
import com.example.bivoca.ui.quiz.TracNghiemActivity;
import com.example.bivoca.ui.quiz.QuizViewModel;
import com.example.bivoca.data.model.QuizSessionDtos.Question;
import com.example.bivoca.utils.SessionManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real QA GET-only matrix. Display/font/orientation and fresh UI login belong to the runner.
 * Never creates a Quiz, submits answers/reviews/favorites, or reads Auth preferences.
 * The sole test seam selects an existing owned session ID before normal VM GET hydration.
 */
@RunWith(AndroidJUnit4.class)
public class M16_8UiMatrixTest {
    private final Bundle args = InstrumentationRegistry.getArguments();
    private SharedPreferences quizPreferences;
    private String ownerKey, originalOwnerValue;
    private boolean ownerKeyExisted, ownerKeyProtected;
    private File output;
    private JsonObject plan;
    private JsonArray captures = new JsonArray();
    private ActivityScenario<?> currentScenario;
    private Activity currentActivity;
    private String testStatus = "NOT_RUN";
    private boolean ownerKeyRestored;

    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private interface Ready { boolean check(); }
    private void await(String label, Ready ready) {
        long deadline = SystemClock.elapsedRealtime() + 60000;
        while (SystemClock.elapsedRealtime() < deadline) {
            AtomicReference<Boolean> result = new AtomicReference<>(false);
            main(() -> result.set(ready.check()));
            if (result.get()) { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); return; }
            SystemClock.sleep(100);
        }
        fail("Timed out: " + label + "; no credentials or payloads logged");
    }

    @Before public void requireSafeRealQa() throws Exception {
        assertEquals("com.example.bivoca.e2e", context().getPackageName());
        assertEquals(37, Build.VERSION.SDK_INT);
        assertTrue(BuildConfig.QUIZ_SESSION_V2_ENABLED);
        assertEquals("http://10.0.2.2:3001/api/v1/", ApiBaseUrl.current());
        assertEquals("Fresh QA UI login required", "true", args.getString("freshSyntheticLoginConfirmed"));
        String label = args.getString("matrixLabel");
        assertNotNull("matrixLabel is required", label);
        assertTrue("Safe matrix label", label.matches("[A-Za-z0-9_.-]{1,100}"));
        output = new File(context().getFilesDir(), "m16-8/" + label);
        assertTrue(output.isDirectory() || output.mkdirs());
        String planName = args.getString("sessionPlanFile", "m16-8-owned-session-plan.json");
        assertTrue("Plan must be a direct internal filename", planName.matches("[A-Za-z0-9_.-]+"));
        plan = JsonParser.parseString(new String(Files.readAllBytes(
                new File(context().getFilesDir(), planName).toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
        SessionManager session = new SessionManager(context());
        assertTrue("Fresh QA UI session required", session.isLoggedIn());
        int owner = plan.get("qaUserId").getAsInt();
        assertEquals(owner, session.getUserId());
        // Normal SessionManager token stays in RAM; never enumerate/read Auth preference storage.
        HttpURLConnection connection = (HttpURLConnection) new URL(ApiBaseUrl.current() + "auth/me").openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
        connection.setRequestProperty("Authorization", "Bearer " + session.getAccessToken());
        try {
            assertEquals("QA identity GET", 200, connection.getResponseCode());
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject user = JsonParser.parseReader(reader).getAsJsonObject();
                assertEquals(owner, user.get("id").getAsInt());
                assertTrue("Only the approved QA A account may run the matrix",
                        "qa_ft_a@fixture.invalid".equals(user.get("email").getAsString()));
            }
        } finally { connection.disconnect(); }
        quizPreferences = context().getSharedPreferences("quiz_session_v2", Context.MODE_PRIVATE);
        ownerKey = "user_" + owner;
        ownerKeyExisted = quizPreferences.contains(ownerKey);
        originalOwnerValue = quizPreferences.getString(ownerKey, null);
        ownerKeyProtected = true;
        // Isolate only this owner's local resume key; never clear the preference file.
        assertTrue(quizPreferences.edit().remove(ownerKey).commit());
        writeManifest("RUNNING");
    }

    @After public void restoreOnlyOwnerQuizKey() throws Exception {
        try { closeCurrent(); }
        finally {
            if (ownerKeyProtected) {
                SharedPreferences.Editor edit = quizPreferences.edit();
                if (ownerKeyExisted) edit.putString(ownerKey, originalOwnerValue); else edit.remove(ownerKey);
                assertTrue("Restore original H1 owner resume key", edit.commit());
                assertEquals(ownerKeyExisted, quizPreferences.contains(ownerKey));
                assertTrue("Original owner resume value restored without exporting it", java.util.Objects.equals(originalOwnerValue, quizPreferences.getString(ownerKey, null)));
                ownerKeyRestored = true;
                writeManifest(testStatus);
            }
        }
    }

    @Test public void realApiScreensAtCurrentMatrixConfiguration() throws Exception {
        try {
            if ("true".equals(args.getString("errorStatesOnly"))) { errorStates(); writeManifest("PASS"); return; }
            List<Topic> topics = RetrofitClient.getInstance().create(TopicApi.class).getTopics().execute().body();
            assertNotNull("Real QA catalog", topics);
            Topic topic = null;
            for (Topic candidate : topics) if (candidate.getFlashcardCount() > 0) { topic = candidate; break; }
            assertNotNull("QA catalog must contain a populated topic", topic);
            final Topic chosen = topic;

            TrangChuActivity home = launch(new Intent(context(), TrangChuActivity.class), TrangChuActivity.class);
            await("Home real profile/progress/leaderboard", () -> {
                ProfileViewModel p = new ViewModelProvider(home).get(ProfileViewModel.class);
                ProgressViewModel r = new ViewModelProvider(home).get(ProgressViewModel.class);
                LeaderboardViewModel l = new ViewModelProvider(home).get(LeaderboardViewModel.class);
                return !Boolean.TRUE.equals(p.getLoading().getValue()) && !Boolean.TRUE.equals(r.getLoading().getValue())
                        && !Boolean.TRUE.equals(l.getLoading().getValue()) && p.getUser().getValue() != null
                        && r.getSummary().getValue() != null && l.getLeaderboard().getValue() != null
                        && p.getStats().getValue() != null && r.getSchedule().getValue() != null
                        && r.getLearningToday().getValue() != null
                        && p.getError().getValue() == null && r.getError().getValue() == null
                        && r.getScheduleError().getValue() == null && l.getError().getValue() == null;
            });
            capture("home-top"); navigationBounds();
            reveal(R.id.cardHomeLeaderboard); capture("home-leaderboard");

            ChuDeActivity catalog = launch(new Intent(context(), ChuDeActivity.class), ChuDeActivity.class);
            await("Topics catalog", () -> {
                TopicsViewModel vm = new ViewModelProvider(catalog).get(TopicsViewModel.class);
                return !Boolean.TRUE.equals(vm.getLoading().getValue()) && vm.getTopics().getValue() != null;
            });
            capture("topics"); navigationBounds();
            assertText(R.id.textTopicsTitle);

            ChiTietChuDeActivity detail = launch(new Intent(context(), ChiTietChuDeActivity.class)
                    .putExtra(ChiTietChuDeActivity.EXTRA_TOPIC_CODE, chosen.getCode()), ChiTietChuDeActivity.class);
            await("Topic metadata and preview", () -> {
                TopicDetailViewModel vm = new ViewModelProvider(detail).get(TopicDetailViewModel.class);
                return !Boolean.TRUE.equals(vm.getMetadataLoading().getValue())
                        && !Boolean.TRUE.equals(vm.getPreviewLoading().getValue()) && vm.getTopic().getValue() != null
                        && vm.getPreview().getValue() != null && !vm.getPreview().getValue().isEmpty();
            });
            capture("topic-detail-top"); navigationBounds();
            reveal(R.id.buttonTopicFlashcards); capture("topic-detail-actions"); assertVisible(R.id.buttonTopicFlashcards);

            TheTuVungActivity cards = launch(new Intent(context(), TheTuVungActivity.class)
                    .putExtra(TheTuVungActivity.EXTRA_TOPIC_CODE, chosen.getCode())
                    .putExtra(TheTuVungActivity.EXTRA_TOPIC_NAME, chosen.getName()), TheTuVungActivity.class);
            await("Real browse cards", () -> cards.getLearningViewModel().getFlashcards().getValue() != null
                    && !cards.getLearningViewModel().getFlashcards().getValue().isEmpty()
                    && !Boolean.TRUE.equals(cards.getLearningViewModel().getLoading().getValue()));
            capture("flashcard-front"); navigationBounds();
            reveal(R.id.buttonFlipCard); assertVisible(R.id.buttonFlipCard); click(R.id.buttonFlipCard);
            await("Card flipped without rating", () -> cards.findViewById(R.id.layoutCardBack).getVisibility() == View.VISIBLE);
            capture("flashcard-back"); reveal(R.id.buttonFlipCard); assertVisible(R.id.buttonFlipCard);

            TracNghiemActivity quiz = launch(new Intent(context(), TracNghiemActivity.class), TracNghiemActivity.class);
            QuizViewModel vm = quiz.getLearningViewModel();
            idleQuiz(vm);
            assertEquals(QuizViewModel.SessionScreen.METHODS, vm.sessionScreen);
            capture("quiz-methods-top"); navigationBounds();
            revealTag("method:MATCHING"); capture("quiz-methods-bottom"); assertVisibleTag("method:MATCHING");
            // This only fetches catalog/availability, never presses Start.
            main(() -> vm.chooseSessionMethod("EN_TO_VI")); idleQuiz(vm);
            capture("quiz-configuration-top");
            reveal(R.id.quizV2Start); capture("quiz-configuration-bottom"); assertVisible(R.id.quizV2Start);

            hydrate(vm, ownedSession("EN_TO_VI", "ACTIVE", 20, true));
            assertEquals(QuizViewModel.SessionScreen.QUESTIONS, vm.sessionScreen);
            capture("quiz-mc-top"); navigationBounds(); assertText(R.id.quizV2Prompt);
            for (int id : new int[]{R.id.quizV2AnswerA,R.id.quizV2AnswerB,R.id.quizV2AnswerC,R.id.quizV2AnswerD}) {
                reveal(id); assertVisible(id); assertText(id);
            }
            capture("quiz-mc-grid");

            hydrate(vm, ownedSession("MATCHING", "COMPLETED", 15, false));
            assertEquals(15, vm.sessionState.session.answeredCount);
            capture("matching15-result-top"); captureResultReview(vm, "matching15");
            main(vm::reviewSessionV2);
            capture("matching15-round1"); matchingColumns(4);
            main(() -> vm.chooseMatchingLeft(12));
            reveal(R.id.quizV2Matching); capture("matching15-last-round3"); matchingColumns(3);
            assertTrue("Completed matching options are locked", !vm.sessionState.active());

            hydrate(vm, ownedSession("CHALLENGE", "COMPLETED", 20, false));
            assertEquals("INCORRECT", vm.sessionState.session.finishReason);
            capture("challenge-result-top"); captureResultReview(vm, "challenge");
            main(vm::reviewSessionV2); capture("challenge-acknowledged-question");
            assertText(R.id.quizV2Prompt);
            for (Question q : vm.sessionState.questions) {
                if (vm.sessionState.receipt(q.questionId) != null) {
                    assertTrue("Review receipt keeps acknowledged question locked", vm.sessionState.locked(q.questionId));
                }
            }
            writeManifest("PASS");
        } catch (Throwable failure) {
            if (currentActivity != null) try { capture("failure-current-screen"); } catch (Exception ignored) { }
            writeManifest("FAIL");
            if (failure instanceof Exception) throw (Exception) failure;
            if (failure instanceof Error) throw (Error) failure;
            throw new RuntimeException(failure);
        }
    }

    private void errorStates() throws Exception {
        retrofit2.Retrofit original=com.example.bivoca.data.api.AuthenticatedRetrofitClient.getInstance(context());
        Field field=com.example.bivoca.data.api.AuthenticatedRetrofitClient.class.getDeclaredField("retrofit"); field.setAccessible(true);
        AtomicReference<String> mode=new AtomicReference<>("NONE");
        java.util.concurrent.CountDownLatch release=new java.util.concurrent.CountDownLatch(1);
        okhttp3.OkHttpClient base=(okhttp3.OkHttpClient)original.callFactory();
        okhttp3.OkHttpClient client=base.newBuilder().addInterceptor(chain->{
            if(!chain.request().url().encodedPath().endsWith("/quiz/availability"))return chain.proceed(chain.request());
            String fault=mode.get();
            if("LOADING".equals(fault))try{if(!release.await(20,java.util.concurrent.TimeUnit.SECONDS))throw new java.io.IOException("TEST_DELAY_EXPIRED");}catch(InterruptedException e){throw new java.io.IOException("TEST_DELAY_INTERRUPTED");}
            if("TIMEOUT".equals(fault))throw new java.net.SocketTimeoutException("TEST_ONLY_TIMEOUT");
            if("OFFLINE".equals(fault))throw new java.io.IOException("TEST_ONLY_NETWORK_DISCONNECTED");
            if("500".equals(fault)||"403".equals(fault))return new okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(Integer.parseInt(fault)).message("TEST_ONLY_FAULT").body(okhttp3.ResponseBody.create(okhttp3.MediaType.parse("application/json"),"{\"error\":{\"code\":\"TEST_ONLY_HTTP_"+fault+"\",\"message\":\"Lỗi kiểm thử HTTP "+fault+"\"}}")).build();
            return chain.proceed(chain.request());
        }).build();
        try {
            field.set(null,new retrofit2.Retrofit.Builder().baseUrl(ApiBaseUrl.current()).client(client).addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build());
            TracNghiemActivity quiz=launch(new Intent(context(),TracNghiemActivity.class),TracNghiemActivity.class);
            QuizViewModel vm=quiz.getLearningViewModel();idleQuiz(vm);main(()->vm.chooseSessionMethod("EN_TO_VI"));idleQuiz(vm);
            mode.set("LOADING");main(vm::refreshSessionAvailability);await("Loading visible",()->vm.sessionAvailabilityLoading);
            main(()->assertEquals(View.VISIBLE,currentActivity.findViewById(R.id.quizV2Loading).getVisibility()));capture("test-only-loading");release.countDown();idleQuiz(vm);
            for(String fault:new String[]{"500","403","TIMEOUT","OFFLINE"}){
                mode.set(fault);main(vm::refreshSessionAvailability);await("Expected test-only "+fault,()->!vm.sessionAvailabilityLoading&&vm.sessionError!=null);
                reveal(R.id.quizV2Error);capture("test-only-error-"+fault.toLowerCase(java.util.Locale.ROOT));
                main(()->{assertEquals(View.VISIBLE,currentActivity.findViewById(R.id.quizV2Error).getVisibility());assertFalse(currentActivity.findViewById(R.id.quizV2Start).isEnabled());});
                mode.set("NONE");main(vm::refreshSessionAvailability);idleQuiz(vm);assertEquals(100,vm.sessionAvailability.quizEligibleCount);
            }
            capture("real-qa-retry-recovered");
            // The real catalog omits empty topics; use the existing approved fixture directly through the VM.
            main(()->vm.chooseSessionTopic("V2_COUNT_0","Synthetic 0"));idleQuiz(vm);assertEquals(0,vm.sessionAvailability.quizEligibleCount);
            reveal(R.id.quizV2Start);navigationBounds();capture("real-qa-zero-eligible");main(()->assertFalse(currentActivity.findViewById(R.id.quizV2Start).isEnabled()));
            if("true".equals(args.getString("navTouchBeforeSearch"))) { touchNavigationHome(); return; }
            main(()->((android.widget.EditText)currentActivity.findViewById(R.id.quizV2Search)).setText("M168_NO_MATCH_XYZ"));
            reveal(R.id.quizV2TopicList);capture("search-no-results");
            main(()->{ViewGroup list=currentActivity.findViewById(R.id.quizV2TopicList);assertEquals(1,list.getChildCount());assertTrue(((TextView)list.getChildAt(0)).getText().toString().contains("Không có chủ đề phù hợp"));});
            touchNavigationHome();
        } finally { release.countDown();closeCurrent();field.set(null,original); }
    }
    private void touchNavigationHome() throws Exception {
            navigationBounds();
            final int[] point=new int[2];main(()->{View home=currentActivity.findViewById(R.id.homeTabHome);home.getLocationOnScreen(point);point[0]+=home.getWidth()/2;point[1]+=home.getHeight()/2;});
            long time=SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,android.view.MotionEvent.ACTION_DOWN,point[0],point[1],0),up=android.view.MotionEvent.obtain(time,time+80,android.view.MotionEvent.ACTION_UP,point[0],point[1],0);
            try{assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(down,true));assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().injectInputEvent(up,true));}finally{down.recycle();up.recycle();}
            await("Home reached by real navigation touch",()->{for(Activity a:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(a instanceof TrangChuActivity){currentActivity=a;return true;}return false;});capture("home-via-navigation-touch");
    }
    private JsonObject ownedSession(String method, String state, int count, boolean noAnswers) {
        for (JsonElement element : plan.getAsJsonArray("sessions")) {
            JsonObject s = element.getAsJsonObject();
            if (method.equals(s.get("method").getAsString()) && state.equals(s.get("state").getAsString())
                    && s.get("user_id").getAsInt() == plan.get("qaUserId").getAsInt()
                    && s.get("selected_unit_count").getAsInt() == count
                    && (!noAnswers || s.get("answered_count").getAsInt() == 0)) return s;
        }
        throw new AssertionError("Approved owned session missing: " + method + "/" + state + "/" + count);
    }
    private void hydrate(QuizViewModel vm, JsonObject expected) throws Exception {
        idleQuiz(vm);
        Field id = QuizViewModel.class.getDeclaredField("sessionId"); id.setAccessible(true);
        main(() -> {
            assertNull("No pending answer may be replaced", vm.sessionState.pending);
            assertNull("No pending control may be replaced", vm.sessionState.pendingControl);
            try { id.set(vm, expected.get("id").getAsString()); }
            catch (IllegalAccessException error) { throw new AssertionError(error); }
            vm.sessionState.cursor = 0;
            vm.loadSessionV2(); // Existing real repository performs only session/questions GETs.
        });
        idleQuiz(vm);
        assertNotNull(vm.sessionState.session);
        assertEquals(expected.get("id").getAsString(), vm.sessionState.session.sessionId);
        assertEquals(expected.get("version").getAsInt(), vm.sessionState.session.version);
        assertEquals(expected.get("answered_count").getAsInt(), vm.sessionState.session.answeredCount);
        assertEquals(expected.get("state").getAsString(), vm.sessionState.session.state);
    }
    private void idleQuiz(QuizViewModel vm) {
        await("Quiz GET hydration/availability", () -> !vm.sessionBusy && !vm.sessionAvailabilityLoading && !vm.sessionCatalogLoading);
        main(() -> assertNull("Quiz API error code: " + vm.sessionErrorCode, vm.sessionError));
    }
    private void captureResultReview(QuizViewModel vm, String prefix) throws Exception {
        reveal(R.id.quizV2ReviewToggle); capture(prefix + "-result-controls"); assertVisible(R.id.quizV2ReviewToggle);
        assertFalse("Existing result must include actual wrong-answer receipts", vm.sessionState.incorrect().isEmpty());
        click(R.id.quizV2ReviewToggle);
        main(() -> { ViewGroup list=currentActivity.findViewById(R.id.quizV2ReviewList); assertTrue(list.getChildCount()>0); revealView(list.getChildAt(0)); });
        capture(prefix + "-inline-review-top");
        main(() -> { ViewGroup list=currentActivity.findViewById(R.id.quizV2ReviewList); revealView(list.getChildAt(list.getChildCount()-1)); });
        capture(prefix + "-inline-review-bottom");
        main(() -> assertEquals(View.VISIBLE, currentActivity.findViewById(R.id.quizV2ReviewList).getVisibility()));
    }
    private <T extends Activity> T launch(Intent intent, Class<T> type) {
        closeCurrent();
        ActivityScenario<T> scenario = ActivityScenario.launch(intent);
        currentScenario = scenario;
        AtomicReference<T> activity = new AtomicReference<>();
        scenario.onActivity(value -> { currentActivity = value; activity.set(value); });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        main(this::configuration);
        return type.cast(activity.get());
    }
    private void closeCurrent() {
        if (currentScenario != null) { currentScenario.close(); currentScenario = null; currentActivity = null; }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private void configuration() {
        Configuration c = currentActivity.getResources().getConfiguration();
        String width = args.getString("expectedShortSideDp"), font = args.getString("expectedFont");
        String orientation = args.getString("expectedOrientation");
        assertNotNull("expectedShortSideDp required", width); assertNotNull("expectedFont required", font);
        assertNotNull("expectedOrientation required", orientation);
        assertTrue("Physical config short side differs", Math.abs(Integer.parseInt(width) - Math.min(c.screenWidthDp,c.screenHeightDp)) <= 3);
        assertEquals(Float.parseFloat(font), c.fontScale, 0.01f);
        assertTrue("Use portrait or landscape", "portrait".equals(orientation) || "landscape".equals(orientation));
        assertEquals("portrait".equals(orientation) ? Configuration.ORIENTATION_PORTRAIT : Configuration.ORIENTATION_LANDSCAPE, c.orientation);
    }
    private void navigationBounds() { for (int id : new int[]{R.id.homeTabHome,R.id.buttonTopics,R.id.buttonReviewToday,R.id.buttonQuiz}) assertVisible(id); }
    private void click(int id) { main(() -> { View v = currentActivity.findViewById(id); assertNotNull(v); assertTrue(v.isEnabled()); assertTrue(v.performClick()); }); settle(); }
    private void reveal(int id) { main(() -> revealView(currentActivity.findViewById(id))); settle(); }
    private void revealTag(String tag) { main(() -> revealView(currentActivity.findViewById(android.R.id.content).findViewWithTag(tag))); settle(); }
    private void revealView(View v) { assertNotNull("Missing scroll target",v); v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true); }
    private void settle() { InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(180); }
    private void assertVisible(int id) { main(() -> fullyVisible(currentActivity.findViewById(id))); }
    private void assertVisibleTag(String tag) { main(() -> fullyVisible(currentActivity.findViewById(android.R.id.content).findViewWithTag(tag))); }
    private void fullyVisible(View view) {
        assertNotNull("Missing control", view); assertTrue("Control not laid out", view.getWidth()>0 && view.getHeight()>0);
        Rect visible = new Rect(); assertTrue("Control outside viewport", view.getGlobalVisibleRect(visible));
        assertTrue("Control clipped horizontally", visible.width() >= view.getWidth()-2);
        assertTrue("Control clipped vertically after scroll", visible.height() >= view.getHeight()-2);
        Rect window = new Rect(); currentActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(window);
        assertTrue("Control beyond visible display", visible.left >= window.left-2 && visible.right <= window.right+2 && visible.bottom <= window.bottom+2);
    }
    private void assertText(int id) { main(() -> {
        TextView text = currentActivity.findViewById(id); assertNotNull(text); assertNotNull("Text has no layout", text.getLayout());
        assertTrue("Critical text empty", text.length()>0);
        int lines = text.getLayout().getLineCount(); assertTrue(lines>0);
        for(int n=0;n<lines;n++) assertEquals("Critical text ellipsized",0,text.getLayout().getEllipsisCount(n));
        assertTrue("Critical text clipped vertically", text.getLayout().getLineBottom(lines-1) <= text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom()+2);
    }); }
    private void matchingColumns(int expectedRows) { main(() -> {
        ViewGroup matching = currentActivity.findViewById(R.id.quizV2Matching);
        assertEquals(2, matching.getChildCount());
        ViewGroup left = (ViewGroup) matching.getChildAt(0), right = (ViewGroup) matching.getChildAt(1);
        assertEquals(expectedRows,left.getChildCount()); assertEquals(expectedRows,right.getChildCount());
        assertTrue("Matching columns overlap",left.getRight()<=right.getLeft());
        assertTrue("Matching columns overflow",left.getLeft()>=0 && right.getRight()<=matching.getWidth());
        assertTrue("Matching columns collapsed",left.getWidth()>0 && right.getWidth()>0);
    }); }
    private void capture(String name) throws Exception {
        settle();
        Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot(); assertNotNull(screenshot);
        try(FileOutputStream stream = new FileOutputStream(new File(output,name+".png"))) { assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG,100,stream)); }
        finally { screenshot.recycle(); }
        main(() -> {
            JsonObject record = new JsonObject(); Configuration c = currentActivity.getResources().getConfiguration();
            record.addProperty("name",name); record.addProperty("activity",currentActivity.getClass().getSimpleName());
            record.addProperty("widthDp",c.screenWidthDp); record.addProperty("heightDp",c.screenHeightDp);
            record.addProperty("fontScale",c.fontScale); record.addProperty("orientation",c.orientation);
            record.addProperty("capturedAtMs",System.currentTimeMillis());
            if(currentActivity instanceof TracNghiemActivity) {
                QuizViewModel vm=((TracNghiemActivity)currentActivity).getLearningViewModel();
                record.addProperty("quizScreen",vm.sessionScreen.name()); record.addProperty("method",vm.sessionMethod);
                if(vm.sessionState.session!=null) { record.addProperty("state",vm.sessionState.session.state);
                    record.addProperty("version",vm.sessionState.session.version); record.addProperty("answered",vm.sessionState.session.answeredCount); }
            }
            captures.add(record);
        });
        writeManifest("RUNNING");
    }
    private void writeManifest(String status) throws Exception {
        if(output==null) return;
        testStatus=status;
        JsonObject manifest=new JsonObject(); manifest.addProperty("status",status); manifest.addProperty("apiPort",3001);
        manifest.addProperty("readOnlyBackend",true); manifest.addProperty("authStorageExported",false);
        manifest.addProperty("ownerQuizPreferenceRestored",ownerKeyRestored); manifest.add("captures",captures);
        try(FileOutputStream stream=new FileOutputStream(new File(output,"matrix.json"))) { stream.write(manifest.toString().getBytes(StandardCharsets.UTF_8)); }
    }
}