package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.data.api.RetrofitClient;
import com.example.bivoca.data.api.TopicApi;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.ui.topics.*;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.flashcard.FlashcardViewModel;
import com.example.bivoca.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class TopicsUiTest {
    @Test @SuppressWarnings({"rawtypes","unchecked"}) public void smallReferenceLandscapeAndFont13Fit() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            for(int[] size:new int[][]{{320,640},{412,915},{731,411}})for(float font:new float[]{1f,1.3f}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.fontScale=font;config.screenWidthDp=size[0];config.screenHeightDp=size[1];
                Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Home);
                View root=LayoutInflater.from(themed).inflate(R.layout.activity_topics,null,false);
                ((TextView)root.findViewById(R.id.textTopicsSubtitle)).setText("29 chủ đề · 7 nhóm loại từ");
                ChipGroup chips=root.findViewById(R.id.topicsFilterChips);
                for(String label:new String[]{"Tất cả (36)","Chủ đề (29)","Loại từ (7)"}) {
                    Chip chip=(Chip)LayoutInflater.from(themed).inflate(R.layout.item_topic_chip,chips,false);
                    chip.setText(label);chips.addView(chip);
                }
                float density=themed.getResources().getDisplayMetrics().density;
                int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
                ViewGroup tabs=root.findViewById(R.id.homeBottomNavigation);
                assertEquals(4,tabs.getChildCount());assertEquals(height,tabs.getBottom());
                assertTrue(root.findViewById(R.id.recyclerTopics).getHeight()>0);
                assertTextFits(root);
                RecyclerView.Adapter adapter=new TopicAdapter(topic->{});
                adapter.setHasStableIds(true);
                ((TopicAdapter)adapter).setTopics(Collections.singletonList(new Topic(1,"COMMUNICATION","Communication & Media","Từ vựng về giao tiếp và truyền thông",null,1068)));
                RecyclerView.ViewHolder holder=adapter.onCreateViewHolder((ViewGroup)root,0);adapter.onBindViewHolder(holder,0);
                View card=holder.itemView;int cardWidth=width-Math.round(48*density);
                card.measure(View.MeasureSpec.makeMeasureSpec(cardWidth,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));card.layout(0,0,cardWidth,card.getMeasuredHeight());
                assertTextFits(card);
                assertEquals(Math.round(48*density),card.findViewById(R.id.topicIconBox).getWidth());
            }
        });
    }

    @Test public void realCatalogFiltersNavigationAndScreenshots() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertTrue("Existing login required",new SessionManager(context).isLoggedIn());
        List<Topic> catalog=RetrofitClient.getInstance().create(TopicApi.class).getTopics().execute().body();
        assertNotNull(catalog);assertFalse(catalog.isEmpty());
        try(ActivityScenario<TopicsActivity> scenario=ActivityScenario.launch(TopicsActivity.class)) {
            awaitTopics(scenario,catalog.size());
            scenario.onActivity(activity->{
                RecyclerView list=activity.findViewById(R.id.recyclerTopics);
                RecyclerView parts=activity.findViewById(R.id.recyclerTopicPartsOfSpeech);
                assertEquals(catalog.size(),list.getAdapter().getItemCount()+parts.getAdapter().getItemCount());
                List<Topic> expected=TopicFilter.apply(catalog,"");
                int semanticCount=list.getAdapter().getItemCount();
                for(int i=0;i<expected.size();i++)assertEquals(expected.get(i).getId(),i<semanticCount?list.getAdapter().getItemId(i):parts.getAdapter().getItemId(i-semanticCount));
                assertFourRows(list);assertFourRows(parts);
                assertTrue(activity.findViewById(R.id.buttonTopics).isSelected());
                assertFalse(activity.findViewById(R.id.homeTabHome).isSelected());
            });
            screenshot("topics-top.png");
            int[] fixedX={0};
            scenario.onActivity(activity->{int[] xy=new int[2];activity.findViewById(R.id.chipTopicsAll).getLocationOnScreen(xy);fixedX[0]=xy[0];});
            onView(withId(R.id.topicsFilterScroll)).perform(swipeLeft());
            scenario.onActivity(activity->{int[] xy=new int[2];activity.findViewById(R.id.chipTopicsAll).getLocationOnScreen(xy);assertEquals("All chip moved",fixedX[0],xy[0]);assertTrue(((android.widget.HorizontalScrollView)activity.findViewById(R.id.topicsFilterScroll)).getScrollX()>0);});
            screenshot("topics-fixed-all.png");
            scenario.onActivity(activity->((android.widget.HorizontalScrollView)activity.findViewById(R.id.topicsFilterScroll)).scrollTo(0,0));
            onView(withId(R.id.recyclerTopics)).perform(swipeVisible(false));
            scenario.onActivity(activity->{assertEquals("Inner swipe scrolled page",0,activity.findViewById(R.id.topicsSectionsScroll).getScrollY());assertTrue(((androidx.recyclerview.widget.LinearLayoutManager)((RecyclerView)activity.findViewById(R.id.recyclerTopics)).getLayoutManager()).findFirstVisibleItemPosition()>0);});
            screenshot("topics-middle.png");
            onView(withId(R.id.topicsSectionsScroll)).perform(swipeVisible(true));
            scenario.onActivity(activity->assertTrue("Outer page cannot reach POS frame",activity.findViewById(R.id.topicsSectionsScroll).getScrollY()>0));
            screenshot("topics-two-frames.png");
            select(scenario,TopicFilter.Group.PART_OF_SPEECH);
            int pos=TopicFilter.apply(catalog,"",TopicFilter.Group.PART_OF_SPEECH).size();
            awaitTopics(scenario,pos);screenshot("topics-pos.png");
            scenario.recreate();awaitTopics(scenario,pos);
            select(scenario,TopicFilter.Group.ALL);
            onView(withId(R.id.inputTopicSearch)).perform(replaceText("Food"),closeSoftKeyboard());
            awaitTopics(scenario,TopicFilter.apply(catalog,"Food").size());screenshot("topics-search.png");
            scenario.onActivity(activity->((RecyclerView)activity.findViewById(R.id.recyclerTopics)).findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.cardTopic).performClick());
            Activity flashcard=awaitActivity(FlashcardActivity.class);
            assertEquals("FOOD",flashcard.getIntent().getStringExtra(FlashcardActivity.EXTRA_TOPIC_CODE));
            long deadline=SystemClock.uptimeMillis()+12000;boolean[] cardsReady={false};
            while(!cardsReady[0]&&SystemClock.uptimeMillis()<deadline) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
                    FlashcardViewModel vm=new ViewModelProvider((FlashcardActivity)flashcard).get(FlashcardViewModel.class);
                    if(vm.getFlashcards().getValue()!=null&&!vm.getFlashcards().getValue().isEmpty()) {
                        assertTrue(vm.getFlashcards().getValue().size()<=100);cardsReady[0]=true;
                    }
                });SystemClock.sleep(100);
            }
            assertTrue("Existing paginated Food cards did not load",cardsReady[0]);
            InstrumentationRegistry.getInstrumentation().runOnMainSync(flashcard::finish);awaitActivity(TopicsActivity.class);
            onView(withId(R.id.inputTopicSearch)).perform(replaceText("zzzz_m16_4_nomatch"),closeSoftKeyboard());
            awaitTopics(scenario,0);screenshot("topics-empty.png");
            onView(withId(R.id.inputTopicSearch)).perform(replaceText(""),closeSoftKeyboard());awaitTopics(scenario,catalog.size());
            onView(withId(R.id.buttonHomeMenu)).perform(click());
            scenario.onActivity(activity->assertTrue(((DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
            screenshot("topics-drawer.png");onView(withId(R.id.buttonNavClose)).perform(click());
            scenario.onActivity(activity->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            awaitTopics(scenario,catalog.size());SystemClock.sleep(800);screenshot("topics-landscape.png");
            scenario.onActivity(activity->{assertEquals(Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);});
            awaitTopics(scenario,catalog.size());SystemClock.sleep(700);
            onView(withId(R.id.homeTabHome)).perform(click());awaitActivity(HomeActivity.class);
            onView(withId(R.id.buttonTopics)).perform(click());awaitActivity(TopicsActivity.class);
        }
    }
    private static void select(ActivityScenario<TopicsActivity> scenario,TopicFilter.Group filter){
        scenario.onActivity(activity->{
            ChipGroup chips=activity.findViewById(R.id.topicsFilterChips);
            if(filter==TopicFilter.Group.ALL)activity.findViewById(R.id.chipTopicsAll).performClick();
            else for(int i=0;i<chips.getChildCount();i++)if(filter.name().equals(chips.getChildAt(i).getTag()))chips.getChildAt(i).performClick();
            ((RecyclerView)activity.findViewById(R.id.recyclerTopics)).scrollToPosition(0);
            ((RecyclerView)activity.findViewById(R.id.recyclerTopicPartsOfSpeech)).scrollToPosition(0);
        });
    }
    private static void awaitTopics(ActivityScenario<TopicsActivity> scenario,int size){
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+15000;
        do{scenario.onActivity(activity->{
            TopicsViewModel vm=new ViewModelProvider(activity).get(TopicsViewModel.class);
            ready[0]=vm.getTopics().getValue()!=null&&!Boolean.TRUE.equals(vm.getLoading().getValue())&&((RecyclerView)activity.findViewById(R.id.recyclerTopics)).getAdapter().getItemCount()+((RecyclerView)activity.findViewById(R.id.recyclerTopicPartsOfSpeech)).getAdapter().getItemCount()==size;
            if(ready[0])for(int id:new int[]{R.id.recyclerTopics,R.id.recyclerTopicPartsOfSpeech}) {
                RecyclerView list=activity.findViewById(id);
                if(list.getAdapter().getItemCount()>0)ready[0]&=list.getWidth()>0&&!list.isLayoutRequested()&&list.getHeight()==list.getLayoutParams().height;
            }
            if(ready[0])assertNull(vm.getError().getValue());
        });if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Real catalog/filter did not settle",ready[0]);InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(250);
    }
    private static void assertFourRows(RecyclerView list){
        int expected=Math.min(4,list.getAdapter().getItemCount());
        assertNotNull(list.getChildAt(0));
        assertEquals("Section viewport must contain at most four equal rows",expected*list.getChildAt(0).getHeight(),list.getHeight());
    }
    private static androidx.test.espresso.ViewAction swipeVisible(boolean leftEdge){
        return new androidx.test.espresso.ViewAction(){
            @Override public org.hamcrest.Matcher<View> getConstraints(){return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();}
            @Override public String getDescription(){return "Swipe within visible bounds";}
            @Override public void perform(androidx.test.espresso.UiController controller,View view){
                Rect rect=new Rect();assertTrue(view.getGlobalVisibleRect(rect));
                float x=leftEdge?rect.left+4:rect.centerX();
                new androidx.test.espresso.action.GeneralSwipeAction(androidx.test.espresso.action.Swipe.SLOW,
                        v->new float[]{x,rect.bottom-16},v->new float[]{x,rect.top+16},androidx.test.espresso.action.Press.FINGER).perform(controller,view);
                controller.loopMainThreadForAtLeast(800);
            }
        };
    }
    private static Activity awaitActivity(Class<? extends Activity> type){
        Activity[] found={null};long deadline=SystemClock.uptimeMillis()+6000;
        do{InstrumentationRegistry.getInstrumentation().waitForIdleSync();InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            for(Activity activity:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(type.isInstance(activity)&&activity.hasWindowFocus())found[0]=activity;
        });if(found[0]!=null){SystemClock.sleep(500);return found[0];}SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<deadline);
        fail("Destination missing: "+type.getSimpleName());return null;
    }
    private static void screenshot(String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(350);
        Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"m16-4-topics");assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
    private static void assertTextFits(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView){TextView text=(TextView)view;if(text.length()>0){assertTrue("Text has no width",text.getWidth()>0);assertNotNull(text.getLayout());assertTrue("Text clipped vertically: "+text.getText(),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());}}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertTextFits(((ViewGroup)view).getChildAt(i));
    }
}
