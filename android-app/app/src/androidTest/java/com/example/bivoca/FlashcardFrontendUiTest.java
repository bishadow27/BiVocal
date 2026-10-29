package com.example.bivoca;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.data.model.Flashcard;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.flashcard.FlashcardViewModel;
import com.example.bivoca.settings.SettingsPreferenceManager;
import com.example.bivoca.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Read-only API acceptance: never clicks a rating, Favorite or starts a Quiz. */
@RunWith(AndroidJUnit4.class)
public class FlashcardFrontendUiTest {
    @Test public void fourStatesFitSmallReferenceLandscapeAndFont13() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base=context();
            for(int[] size:new int[][]{{320,640},{412,915},{731,411}})for(float font:new float[]{1f,1.3f}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.fontScale=font;config.screenWidthDp=size[0];config.screenHeightDp=size[1];
                Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Home);
                View root=LayoutInflater.from(themed).inflate(R.layout.activity_flashcard,null,false);
                // Layout-only fixture, never displayed as production data or submitted to any API.
                text(root,R.id.textTopicTitle,"Học từ mới hôm nay");text(root,R.id.textPosition,"Thẻ 10 / 20");
                text(root,R.id.textSessionMode,"HỌC TỪ MỚI");text(root,R.id.textSessionProgress,"10 / 20");
                text(root,R.id.textEnglish,"refrigerator");text(root,R.id.textPronunciation,"/rɪˈfrɪdʒəreɪtə/");
                text(root,R.id.textMetadata,"noun · B1 · Chưa kiểm duyệt ngữ nghĩa");
                text(root,R.id.textBackWord,"refrigerator");text(root,R.id.textBackPronunciation,"/rɪˈfrɪdʒəreɪtə/");
                text(root,R.id.textVietnamese,"tủ lạnh");text(root,R.id.textCardBackLabel,"NGHĨA TIẾNG VIỆT");
                text(root,R.id.textBackMetadata,"noun · B1 · Chưa kiểm duyệt ngữ nghĩa");
                text(root,R.id.textExample,"Keep the milk in the refrigerator.");
                text(root,R.id.textExampleVi,"Để sữa trong tủ lạnh.");
                text(root,R.id.textSessionProcessed,"20");text(root,R.id.textSessionFinalProgress,"20 / 20");
                for(int state=0;state<4;state++) {
                    root.findViewById(R.id.flashcardContent).setVisibility(state==3?View.GONE:View.VISIBLE);
                    root.findViewById(R.id.layoutCardFront).setVisibility(state==0?View.VISIBLE:View.GONE);
                    root.findViewById(R.id.layoutCardBack).setVisibility(state==1||state==2?View.VISIBLE:View.GONE);
                    root.findViewById(R.id.reviewSection).setVisibility(state==2?View.VISIBLE:View.GONE);
                    root.findViewById(R.id.flashcardControls).setVisibility(state==3?View.GONE:View.VISIBLE);
                    root.findViewById(R.id.flashcardComplete).setVisibility(state==3?View.VISIBLE:View.GONE);
                    float density=themed.getResources().getDisplayMetrics().density;
                    int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));
                    root.layout(0,0,width,height);
                    assertTextFits(root);
                    assertTrue(root.findViewById(state==3?R.id.flashcardComplete:R.id.flashcardContent).getHeight()>=Math.round(280*density));
                    View title=root.findViewById(R.id.textTopicTitle);
                    float titleCenter=title.getWidth()/2f;
                    for(View ancestor=title;ancestor!=root;ancestor=(View)ancestor.getParent())
                        titleCenter+=ancestor.getLeft();
                    assertEquals("Topic title must be centered on the screen",width/2f,titleCenter,2f);
                    if(state!=3)assertTrue(root.findViewById(R.id.buttonFlipCard).getHeight()>=Math.round(48*density));
                }
            }
        });
    }

    @Test public void realTopicFrontBackPagingRotationAndAudioControls() throws Exception {
        requireSession();
        Intent intent=new Intent(context(),FlashcardActivity.class).putExtra(FlashcardActivity.EXTRA_TOPIC_CODE,"FOOD")
                .putExtra(FlashcardActivity.EXTRA_TOPIC_NAME,"Food");
        try(ActivityScenario<FlashcardActivity> scenario=ActivityScenario.launch(intent)) {
            awaitLoaded(scenario,true);
            int[] firstId={0};
            int[] frontFlipBounds=new int[4];
            scenario.onActivity(activity->{
                FlashcardViewModel vm=model(activity);Flashcard current=vm.getFlashcards().getValue().get(0);firstId[0]=current.getId();
                assertTrue(vm.getFlashcards().getValue().size()<=100);
                assertEquals(View.VISIBLE,activity.findViewById(R.id.layoutCardFront).getVisibility());
                assertEquals(View.GONE,activity.findViewById(R.id.reviewSection).getVisibility());
                View page=activity.findViewById(R.id.flashcardPageScroll);
                View flip=activity.findViewById(R.id.buttonFlipCard);
                int[] pageXY=new int[2],flipXY=new int[2];page.getLocationOnScreen(pageXY);flip.getLocationOnScreen(flipXY);
                frontFlipBounds[0]=flipXY[0];frontFlipBounds[1]=flipXY[1];frontFlipBounds[2]=flip.getWidth();frontFlipBounds[3]=flip.getHeight();
                float available=page.getHeight()-page.getPaddingTop()-page.getPaddingBottom();
                float fraction=(flipXY[1]+flip.getHeight()/2f-pageXY[1]-page.getPaddingTop())/available;
                assertEquals("Flip action should sit slightly lower, near four fifths",0.80f,fraction,0.035f);
                boolean viFirst=SettingsPreferenceManager.FRONT_VIETNAMESE.equals(new SettingsPreferenceManager(activity).getFlashcardFront());
                assertEquals(viFirst?current.getVietnamese():current.getEnglish(),((TextView)activity.findViewById(R.id.textEnglish)).getText().toString());
            });
            screenshot("flashcard-front.png");
            scenario.onActivity(activity->activity.findViewById(R.id.buttonFlipCard).performClick());settle();
            scenario.onActivity(activity->{
                assertEquals(View.VISIBLE,activity.findViewById(R.id.layoutCardBack).getVisibility());
                assertEquals(View.VISIBLE,activity.findViewById(R.id.reviewSection).getVisibility());
                assertEquals(View.INVISIBLE,activity.findViewById(R.id.buttonNext).getVisibility());
                assertTrue("Rating panel must be below the flip action",activity.findViewById(R.id.reviewSection).getTop()
                        >=activity.findViewById(R.id.flashcardControls).getBottom());
                View back=activity.findViewById(R.id.layoutCardBack);
                View examples=activity.findViewById(R.id.exampleSection);
                if(examples.getVisibility()==View.VISIBLE)assertTrue("Loaded examples are clipped by the card viewport",
                        back.getTop()+examples.getBottom()+((View)back.getParent()).getPaddingBottom()
                                <=activity.findViewById(R.id.flashcardInnerScroll).getHeight());
                android.graphics.Rect flipBounds=new android.graphics.Rect();
                View flip=activity.findViewById(R.id.buttonFlipCard);
                int[] backFlipXY=new int[2];flip.getLocationOnScreen(backFlipXY);
                assertEquals("Flip X changed between faces",frontFlipBounds[0],backFlipXY[0]);
                assertEquals("Flip Y changed between faces",frontFlipBounds[1],backFlipXY[1]);
                assertEquals("Flip width changed between faces",frontFlipBounds[2],flip.getWidth());
                assertEquals("Flip height changed between faces",frontFlipBounds[3],flip.getHeight());
                assertTrue("Return action is not visible",flip.getGlobalVisibleRect(flipBounds));
                assertEquals("Return action is clipped at the bottom",flip.getHeight(),flipBounds.height());
                Flashcard current=model(activity).getFlashcards().getValue().get(0);
                assertEquals(current.getExample()==null||current.getExample().trim().isEmpty()?View.GONE:View.VISIBLE,
                        activity.findViewById(R.id.buttonSpeakExample).getVisibility());
                activity.findViewById(R.id.buttonSpeakBack).performClick();
                if(activity.findViewById(R.id.buttonSpeakExample).isEnabled())
                    activity.findViewById(R.id.buttonSpeakExample).performClick();
            });settle();screenshot("flashcard-back-review.png");
            scenario.recreate();awaitLoaded(scenario,true);
            scenario.onActivity(activity->{
                assertEquals(firstId[0],model(activity).getFlashcards().getValue().get(0).getId());
                assertEquals(View.VISIBLE,activity.findViewById(R.id.layoutCardBack).getVisibility());
            });
            scenario.onActivity(activity->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
            settle();awaitLoaded(scenario,true);screenshot("flashcard-landscape.png");
            // Exercise an actual outer-page gesture. Espresso's generic scrollTo is unreliable
            // for this nested card/page combination and does not prove gesture reachability.
            onView(withId(R.id.flashcardPageScroll)).perform(swipePageEdge());
            screenshot("flashcard-landscape-actions.png");
            scenario.onActivity(activity->{android.graphics.Rect visible=new android.graphics.Rect();View button=activity.findViewById(R.id.buttonFlipCard);
                View page=activity.findViewById(R.id.flashcardPageScroll);
                assertTrue("Actions unreachable; outer scrollY="+page.getScrollY()+", height="+page.getHeight()+", content="+activity.findViewById(R.id.flashcardRoot).getHeight(),button.getGlobalVisibleRect(visible));
                assertEquals(button.getHeight(),visible.height());
            });
            scenario.onActivity(activity->{
                assertEquals(Configuration.ORIENTATION_LANDSCAPE,activity.getResources().getConfiguration().orientation);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            });settle();awaitLoaded(scenario,true);
            scenario.onActivity(activity->activity.findViewById(R.id.buttonFlipCard).performClick());settle();
            scenario.onActivity(activity->{assertTrue(activity.findViewById(R.id.buttonNext).isEnabled());activity.findViewById(R.id.buttonNext).performClick();});settle();
            scenario.onActivity(activity->{
                FlashcardViewModel vm=model(activity);assertEquals(Integer.valueOf(1),vm.getCurrentIndex().getValue());
                assertNotEquals(firstId[0],vm.getFlashcards().getValue().get(1).getId());
                assertEquals(View.VISIBLE,activity.findViewById(R.id.layoutCardFront).getVisibility());
                activity.findViewById(R.id.buttonPrevious).performClick();
                assertEquals(Integer.valueOf(0),vm.getCurrentIndex().getValue());
            });
            scenario.onActivity(activity->activity.findViewById(R.id.buttonFlashcardMenu).performClick());
            onView(withText(R.string.nav_open_description)).perform(click());settle();
            scenario.onActivity(activity->assertTrue(((DrawerLayout)activity.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
            onView(withId(R.id.buttonNavClose)).perform(click());settle();
        }
    }

    @Test public void realNewAndDueQueuesCannotBeSkippedOrFalselyCompleted() throws Exception {
        requireSession();
        for(boolean due:new boolean[]{false,true}) {
            Intent intent=new Intent(context(),FlashcardActivity.class)
                    .putExtra(due?FlashcardActivity.EXTRA_REVIEW_TODAY:FlashcardActivity.EXTRA_NEW_TODAY,true);
            try(ActivityScenario<FlashcardActivity> scenario=ActivityScenario.launch(intent)) {
                awaitLoaded(scenario,false);
                boolean[] populated={false};
                scenario.onActivity(activity->{
                    FlashcardViewModel vm=model(activity);List<Flashcard> cards=vm.getFlashcards().getValue();
                    assertEquals(Integer.valueOf(cards.size()),vm.getSessionTotal().getValue());
                    assertEquals(Integer.valueOf(0),vm.getSessionProcessed().getValue());
                    assertFalse(vm.getReviewCompleted().getValue());
                    assertEquals(View.GONE,activity.findViewById(R.id.flashcardComplete).getVisibility());
                    populated[0]=!cards.isEmpty();
                    if(populated[0]){
                        assertEquals(View.GONE,activity.findViewById(R.id.buttonNext).getVisibility());
                        assertEquals(View.GONE,activity.findViewById(R.id.reviewSection).getVisibility());
                        activity.findViewById(R.id.buttonFlipCard).performClick();
                    }else assertEquals(View.VISIBLE,activity.findViewById(R.id.textFlashcardEmpty).getVisibility());
                });settle();
                if(populated[0])scenario.onActivity(activity->{
                    assertEquals(View.VISIBLE,activity.findViewById(R.id.reviewSection).getVisibility());
                    assertTrue(activity.findViewById(R.id.buttonNotRemember).isEnabled());
                    assertTrue(activity.findViewById(R.id.buttonRemembered).isEnabled());
                    assertEquals(Integer.valueOf(0),model(activity).getSessionProcessed().getValue());
                });
                screenshot(due?"review-today.png":"new-today.png");
            }
        }
    }

    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static void requireSession(){assertTrue("Existing login required; test never creates a session",new SessionManager(context()).isLoggedIn());}
    private static FlashcardViewModel model(FlashcardActivity activity){return new ViewModelProvider(activity).get(FlashcardViewModel.class);}
    private static void awaitLoaded(ActivityScenario<FlashcardActivity> scenario,boolean requireCards){
        boolean[] ready={false};long deadline=SystemClock.uptimeMillis()+15000;
        do{scenario.onActivity(activity->{FlashcardViewModel vm=model(activity);
            if(!Boolean.TRUE.equals(vm.getLoading().getValue())&&vm.getFlashcards().getValue()!=null){
                assertNull(vm.getError().getValue());ready[0]=true;
                if(requireCards)assertFalse("Real topic is empty",vm.getFlashcards().getValue().isEmpty());
            }
        });if(!ready[0])SystemClock.sleep(100);}while(!ready[0]&&SystemClock.uptimeMillis()<deadline);
        assertTrue("Real Flashcard API did not load",ready[0]);settle();
    }
    private static void text(View root,int id,String value){((TextView)root.findViewById(id)).setText(value);}
    private static void settle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450);}
    private static androidx.test.espresso.ViewAction swipePageEdge(){
        return new androidx.test.espresso.ViewAction(){
            @Override public org.hamcrest.Matcher<View> getConstraints(){return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();}
            @Override public String getDescription(){return "Swipe the outer page margin to its actions";}
            @Override public void perform(androidx.test.espresso.UiController controller,View view){
                for(int i=0;i<3;i++){
                    android.graphics.Rect rect=new android.graphics.Rect();assertTrue(view.getGlobalVisibleRect(rect));
                    float density=view.getResources().getDisplayMetrics().density;
                    // Stay in the app's 24dp margin, outside system cutout/gesture insets.
                    float x=rect.left+view.getPaddingLeft()+12*density;
                    new androidx.test.espresso.action.GeneralSwipeAction(androidx.test.espresso.action.Swipe.SLOW,
                            v->new float[]{x,rect.bottom-view.getPaddingBottom()-24*density},
                            v->new float[]{x,rect.top+view.getPaddingTop()+24*density},androidx.test.espresso.action.Press.FINGER).perform(controller,view);
                    controller.loopMainThreadForAtLeast(600);
                }
            }
        };
    }
    private static void assertTextFits(View view){
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView){TextView text=(TextView)view;if(text.length()>0){
            assertTrue("Text has no width",text.getWidth()>0);assertNotNull(text.getLayout());
            assertTrue("Text clipped: "+text.getText(),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
        }}
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertTextFits(((ViewGroup)view).getChildAt(i));
    }
    private static void screenshot(String name)throws Exception{
        settle();Bitmap bitmap=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(bitmap);
        File dir=new File(context().getExternalFilesDir(null),"m16-5-flashcard");assertTrue(dir.isDirectory()||dir.mkdirs());
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,out));}bitmap.recycle();
    }
}
