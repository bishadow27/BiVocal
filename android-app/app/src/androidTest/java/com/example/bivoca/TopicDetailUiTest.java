package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.TopicApi;
import com.example.bivoca.data.api.VocabularyApi;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.data.model.FlashcardPage;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.ui.flashcard.TheTuVungActivity;
import com.example.bivoca.ui.quiz.TracNghiemActivity;
import com.example.bivoca.ui.quiz.QuizViewModel;
import com.example.bivoca.ui.topics.ChiTietChuDeActivity;
import com.example.bivoca.ui.topics.TopicDetailViewModel;
import com.example.bivoca.ui.topics.TopicPreviewAdapter;
import com.example.bivoca.ui.topics.ChuDeActivity;
import com.example.bivoca.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** GET-only recovery checks. Fixtures below are inflated, unattached views, never a live Activity. */
@RunWith(AndroidJUnit4.class)
public class TopicDetailUiTest {
    @Test public void realMetadataBoundedPreviewRecreationBrowseAndQuizEntry() throws Exception {
        Topic food=foodFromRealCatalog();
        FlashcardPage page=RetrofitClient.getInstance().create(VocabularyApi.class)
                .search(null,food.getCode(),null,1,20).execute().body();
        assertNotNull("GET preview page missing",page);assertEquals(1,page.getPage());assertEquals(20,page.getLimit());
        List<Flashcard> expected=firstValidThree(page.getItems());
        try(ActivityScenario<ChiTietChuDeActivity> scenario=launch(food)) {
            awaitDetailReady(scenario);
            TopicDetailViewModel[] retained={null};
            List<Flashcard>[] retainedCards=new List[]{null};
            scenario.onActivity(activity->{
                TopicDetailViewModel vm=model(activity);retained[0]=vm;retainedCards[0]=vm.getPreview().getValue();
                assertEquals(food.getCode(),activity.getIntent().getStringExtra(ChiTietChuDeActivity.EXTRA_TOPIC_CODE));
                assertEquals(food.getName(),text(activity,R.id.textTopicDetailHeader));
                assertEquals(food.getName(),text(activity,R.id.textTopicDetailName));
                assertEquals(activity.getString(R.string.topics16_word_count,NumberFormat.getIntegerInstance().format(food.getFlashcardCount())),text(activity,R.id.textTopicDetailCount));
                assertEquals(food.getDescription(),text(activity,R.id.textTopicDetailDescription));
                List<Flashcard> shown=vm.getPreview().getValue();assertNotNull(shown);
                assertEquals(expected.size(),shown.size());assertTrue(shown.size()<=3);
                boolean warning=false;
                for(int i=0;i<shown.size();i++) {
                    assertEquals(expected.get(i).getId(),shown.get(i).getId());
                    warning|=shown.get(i).isImportedUnverified();
                }
                assertEquals(warning,Boolean.TRUE.equals(vm.getShowUnverifiedWarning().getValue()));
                assertEquals(warning?View.VISIBLE:View.GONE,activity.findViewById(R.id.textTopicPreviewWarning).getVisibility());
                RecyclerView list=activity.findViewById(R.id.recyclerTopicPreview);
                assertEquals(shown.size(),list.getAdapter().getItemCount());
                for(int i=0;i<shown.size();i++) {
                    RecyclerView.ViewHolder holder=list.findViewHolderForAdapterPosition(i);assertNotNull(holder);
                    assertEquals(shown.get(i).getEnglish(),((TextView)holder.itemView.findViewById(R.id.textPreviewEnglish)).getText().toString());
                    assertEquals(shown.get(i).getVietnamese(),((TextView)holder.itemView.findViewById(R.id.textPreviewVietnamese)).getText().toString());
                }
                assertTrue(activity.findViewById(R.id.buttonTopics).isSelected());
            });
            screenshot("detail-portrait.png");
            scenario.onActivity(activity->((androidx.core.widget.NestedScrollView)activity.findViewById(R.id.topicDetailScroll)).fullScroll(View.FOCUS_DOWN));
            screenshot("detail-preview.png");
            scenario.onActivity(activity->((androidx.core.widget.NestedScrollView)activity.findViewById(R.id.topicDetailScroll)).scrollTo(0,0));
            scenario.recreate();awaitDetailReady(scenario);
            scenario.onActivity(activity->{assertSame(retained[0],model(activity));assertSame(retainedCards[0],model(activity).getPreview().getValue());});
            int[] originalOrientation={ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED};
            scenario.onActivity(activity->{originalOrientation[0]=activity.getRequestedOrientation();activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);});
            try {
                awaitDetailReady(scenario);SystemClock.sleep(650);
                scenario.onActivity(activity->assertEquals(Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation));
                screenshot("detail-landscape.png");
            } finally {scenario.onActivity(activity->activity.setRequestedOrientation(originalOrientation[0]));}
            awaitDetailReady(scenario);SystemClock.sleep(500);
            scenario.onActivity(activity->activity.findViewById(R.id.buttonTopicFlashcards).performClick());
            Activity browse=awaitActivity(TheTuVungActivity.class);
            try {
                assertEquals(food.getCode(),browse.getIntent().getStringExtra(TheTuVungActivity.EXTRA_TOPIC_CODE));
                assertFalse(browse.getIntent().getBooleanExtra(TheTuVungActivity.EXTRA_NEW_TODAY,false));
                assertFalse(browse.getIntent().getBooleanExtra(TheTuVungActivity.EXTRA_REVIEW_TODAY,false));
            } finally {onMain(browse::finish);}
            awaitActivity(ChiTietChuDeActivity.class);
            scenario.onActivity(activity->activity.findViewById(R.id.buttonTopicQuiz).performClick());
            Activity quiz=awaitActivity(TracNghiemActivity.class);
            try {
                assertEquals(food.getCode(),quiz.getIntent().getStringExtra(TracNghiemActivity.EXTRA_TOPIC_CODE));
                awaitQuizQuestions((TracNghiemActivity)quiz);
                // Deliberately do not touch any answer, Continue, submit or Favorite control.
            } finally {onMain(quiz::finish);}
            awaitActivity(ChiTietChuDeActivity.class);
        }
    }

    @Test public void detailAvatarDrawerAndTopicsDestinationsReturnToList() throws Exception {
        Topic food=foodFromRealCatalog();
        for(int destination:new int[]{R.id.buttonTopics,R.id.buttonTopicDetailBack}) {
            try(ActivityScenario<ChiTietChuDeActivity> scenario=launch(food)) {
                awaitDetailReady(scenario);
                scenario.onActivity(activity->activity.findViewById(R.id.buttonHomeMenu).performClick());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(400);
                scenario.onActivity(activity->assertTrue(((DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
                // Close before capture: the Drawer can contain account information.
                scenario.onActivity(activity->activity.findViewById(R.id.buttonNavClose).performClick());
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(350);
                screenshot("detail-drawer-closed-"+destination+".png");
                scenario.onActivity(activity->activity.findViewById(destination).performClick());
                Activity list=awaitActivity(ChuDeActivity.class);
                try {onMain(()->{assertNotNull(list.findViewById(R.id.topicsRoot));assertNull(list.findViewById(R.id.topicDetailRoot));});}
                finally {onMain(list::finish);}
            }
        }
    }

    @Test public void previewValidAndWarningLayoutFitsSizeAndFontMatrix() { measureFixtures(0); }
    @Test public void previewEmptyLayoutFitsSizeAndFontMatrix() { measureFixtures(1); }
    @Test public void previewErrorKeepsMetadataAndCtasInUnattachedLayout() { measureFixtures(2); }

    private static void measureFixtures(int state) {
        onMain(()->{
            Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            for(int[] size:new int[][]{{320,640},{412,915},{731,411}})for(float font:new float[]{1f,1.3f}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.fontScale=font;config.screenWidthDp=size[0];config.screenHeightDp=size[1];
                Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Home);
                View root=LayoutInflater.from(themed).inflate(R.layout.activity_chi_tiet_chu_de,null,false);
                assertFalse(root.isAttachedToWindow());
                root.findViewById(R.id.topicMetadataContent).setVisibility(View.VISIBLE);
                ((TextView)root.findViewById(R.id.textTopicDetailHeader)).setText("Communication & Media");
                ((TextView)root.findViewById(R.id.textTopicDetailName)).setText("Communication & Media");
                ((TextView)root.findViewById(R.id.textTopicDetailCount)).setText("1.234 từ vựng");
                ((TextView)root.findViewById(R.id.textTopicDetailDescription)).setText("Mô tả dài để kiểm tra khả năng xuống dòng, cỡ chữ lớn và chiều rộng màn hình nhỏ.");
                root.findViewById(R.id.recyclerTopicPreview).setVisibility(state==0?View.VISIBLE:View.GONE);
                root.findViewById(R.id.textTopicPreviewWarning).setVisibility(state==0?View.VISIBLE:View.GONE);
                root.findViewById(R.id.textTopicPreviewEmpty).setVisibility(state==1?View.VISIBLE:View.GONE);
                root.findViewById(R.id.textTopicPreviewError).setVisibility(state==2?View.VISIBLE:View.GONE);
                root.findViewById(R.id.buttonRetryTopicPreview).setVisibility(state==2?View.VISIBLE:View.GONE);
                ((TextView)root.findViewById(R.id.textTopicPreviewError)).setText("Không thể tải từ vựng xem trước. Vui lòng thử lại.");
                if(state==0) {
                    RecyclerView preview=root.findViewById(R.id.recyclerTopicPreview);
                    preview.setLayoutManager(new LinearLayoutManager(themed));
                    TopicPreviewAdapter adapter=new TopicPreviewAdapter();
                    List<Flashcard> fixtures=new ArrayList<>();
                    for(int i=1;i<=3;i++)fixtures.add(new Flashcard(i,"communication and broadcasting","giao tiếp và truyền thông đa phương tiện","/kəˌmjuːnɪˈkeɪʃən/","noun",null,null,"B2"));
                    adapter.setCards(fixtures);preview.setAdapter(adapter);
                }
                float density=themed.getResources().getDisplayMetrics().density;
                int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                measure(root,width,height);
                assertEquals(height,root.findViewById(R.id.homeBottomNavigation).getBottom());
                assertTrue(root.findViewById(R.id.topicDetailScroll).getHeight()>0);
                assertTextAndWidth(root,root,width);
                for(int id:new int[]{R.id.textTopicDetailHeader,R.id.textTopicDetailName,R.id.textTopicDetailDescription,R.id.textTopicPreviewError,R.id.textTopicPreviewEmpty,R.id.textTopicPreviewWarning}) {
                    TextView text=root.findViewById(id);
                    assertEquals("Detail text must grow for font scaling",ViewGroup.LayoutParams.WRAP_CONTENT,text.getLayoutParams().height);
                    if(text.getVisibility()==View.VISIBLE&&text.getLayout()!=null)for(int line=0;line<text.getLayout().getLineCount();line++)assertEquals("Detail text was ellipsized",0,text.getLayout().getEllipsisCount(line));
                }
                for(int id:new int[]{R.id.buttonTopicDetailBack,R.id.buttonHomeMenu,R.id.buttonTopicFlashcards,R.id.buttonTopicQuiz,R.id.buttonRetryTopicPreview,R.id.homeTabHome,R.id.buttonTopics,R.id.buttonReviewToday,R.id.buttonQuiz}) {
                    View target=root.findViewById(id);if(target.getVisibility()!=View.VISIBLE)continue;
                    assertTrue("Touch width below 48dp",target.getWidth()>=Math.round(48*density));
                    assertTrue("Touch height below 48dp",target.getHeight()>=Math.round(48*density));
                }
                if(state==2) {
                    assertEquals(View.VISIBLE,root.findViewById(R.id.topicMetadataContent).getVisibility());
                    assertEquals(View.VISIBLE,root.findViewById(R.id.buttonTopicFlashcards).getVisibility());
                    assertEquals(View.VISIBLE,root.findViewById(R.id.buttonTopicQuiz).getVisibility());
                    assertEquals(View.GONE,root.findViewById(R.id.textTopicMetadataError).getVisibility());
                    // Independent metadata-error fixture, still confined to this detached view.
                    root.findViewById(R.id.textTopicMetadataError).setVisibility(View.VISIBLE);
                    root.findViewById(R.id.buttonRetryTopicMetadata).setVisibility(View.VISIBLE);
                    ((TextView)root.findViewById(R.id.textTopicMetadataError)).setText("Không thể làm mới thông tin chủ đề. Vui lòng thử lại.");
                    measure(root,width,height);assertTextAndWidth(root,root,width);
                    assertTrue(root.findViewById(R.id.buttonRetryTopicMetadata).getHeight()>=Math.round(48*density));
                    assertEquals(View.VISIBLE,root.findViewById(R.id.textTopicPreviewError).getVisibility());
                }
            }
        });
    }

    static void awaitDetailReady(ActivityScenario<ChiTietChuDeActivity> scenario) {
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+15000;
        do {
            scenario.onActivity(activity->{
                TopicDetailViewModel vm=model(activity);
                if(!Boolean.TRUE.equals(vm.getMetadataLoading().getValue())&&!Boolean.TRUE.equals(vm.getPreviewLoading().getValue())) {
                    assertNull("Metadata GET failed",vm.getMetadataError().getValue());
                    assertNull("Preview GET failed",vm.getPreviewError().getValue());
                    ready[0]=vm.getTopic().getValue()!=null&&!activity.findViewById(R.id.topicDetailRoot).isLayoutRequested();
                }
            });
            if(!ready[0])SystemClock.sleep(100);
        }while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Detail real GETs did not settle",ready[0]);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(250);
    }
    private static void awaitQuizQuestions(TracNghiemActivity activity) {
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+15000;
        do {onMain(()->{
            QuizViewModel vm=activity.getLearningViewModel();
            assertNull(vm.getLastAnswer().getValue());
            assertNotEquals(QuizViewModel.QuizState.SUBMITTING,vm.getState().getValue());
            assertNotEquals("Quiz GET failed",QuizViewModel.QuizState.ERROR,vm.getState().getValue());
            ready[0]=vm.getState().getValue()==QuizViewModel.QuizState.QUESTION&&vm.getCurrentQuestion()!=null;
        });if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Read-only Quiz questions did not load",ready[0]);
    }
    private static Topic foodFromRealCatalog() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Existing login required; test does not create or clear a session",new SessionManager(context).isLoggedIn());
        List<Topic> catalog=RetrofitClient.getInstance().create(TopicApi.class).getTopics().execute().body();
        assertNotNull(catalog);for(Topic topic:catalog)if("FOOD".equals(topic.getCode()))return topic;
        throw new AssertionError("FOOD missing from real catalog");
    }
    private static ActivityScenario<ChiTietChuDeActivity> launch(Topic topic) {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        return ActivityScenario.launch(new Intent(context,ChiTietChuDeActivity.class).putExtra(ChiTietChuDeActivity.EXTRA_TOPIC_CODE,topic.getCode()));
    }
    private static TopicDetailViewModel model(ChiTietChuDeActivity activity) {return new ViewModelProvider(activity).get(TopicDetailViewModel.class);}
    private static String text(Activity activity,int id) {return ((TextView)activity.findViewById(id)).getText().toString();}
    private static List<Flashcard> firstValidThree(List<Flashcard> cards) {
        assertNotNull(cards);List<Flashcard> selected=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        for(Flashcard card:cards)if(card!=null&&card.getId()>0&&hasText(card.getEnglish())&&hasText(card.getVietnamese())&&ids.add(card.getId())) {
            selected.add(card);if(selected.size()==3)break;
        }
        return selected;
    }
    private static boolean hasText(String text) {return text!=null&&!text.trim().isEmpty();}
    private static void measure(View root,int width,int height) {
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
    }
    private static void assertTextAndWidth(View root,View view,int width) {
        if(view.getVisibility()!=View.VISIBLE)return;
        Rect bounds=new Rect(0,0,view.getWidth(),view.getHeight());
        if(view!=root)((ViewGroup)root).offsetDescendantRectToMyCoords(view,bounds);
        assertTrue("Control extends outside width",bounds.left>=0&&bounds.right<=width);
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            if(text.length()>0){assertNotNull(text.getLayout());assertTrue("Text clipped vertically",text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());}
        }
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertTextAndWidth(root,((ViewGroup)view).getChildAt(i),width);
    }
    private static void onMain(Runnable action) {InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private static Activity awaitActivity(Class<? extends Activity> type) {
        Activity[] found={null};long deadline=SystemClock.uptimeMillis()+8000;
        do {InstrumentationRegistry.getInstrumentation().waitForIdleSync();onMain(()->{
            for(Activity activity:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(activity)&&activity.hasWindowFocus())found[0]=activity;
        });if(found[0]!=null){SystemClock.sleep(300);return found[0];}SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<deadline);
        throw new AssertionError("Destination missing: "+type.getSimpleName());
    }
    private static void screenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(350);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"phase-c-recovery/detail");
        assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}finally{bitmap.recycle();}
    }
}
