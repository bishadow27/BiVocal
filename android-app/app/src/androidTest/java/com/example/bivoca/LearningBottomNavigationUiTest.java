package com.example.bivoca;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.bivoca.ui.home.TrangChuActivity;
import com.example.bivoca.ui.topics.*;
import com.example.bivoca.ui.flashcard.*;
import com.example.bivoca.ui.quiz.*;
import com.example.bivoca.ui.navigation.*;
import com.example.bivoca.utils.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Real API GET-only navigation. Never touches Review, Favorite or Quiz answer controls. */
@RunWith(AndroidJUnit4.class)
public class LearningBottomNavigationUiTest {
    @Test public void approvedBarFitsFiveLayoutsAndFontSizeMatrix() {
        main(()->{
            Context base=context();
            for(int layout:new int[]{R.layout.activity_trang_chu,R.layout.activity_chu_de,R.layout.activity_chi_tiet_chu_de,R.layout.activity_the_tu_vung,R.layout.activity_trac_nghiem})
                for(int[] size:new int[][]{{320,640},{412,915},{731,411}})for(float font:new float[]{1f,1.3f}) {
                    Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=font;
                    config.screenWidthDp=size[0];config.screenHeightDp=size[1];
                    config.orientation=size[0]>size[1]?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
                    Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Home_Approved);
                    View root=LayoutInflater.from(themed).inflate(layout,null,false);
                    float density=themed.getResources().getDisplayMetrics().density;
                    int width=Math.round(size[0]*density),height=Math.round(size[1]*density);
                    root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));root.layout(0,0,width,height);
                    ViewGroup bar=root.findViewById(R.id.homeBottomNavigation);
                    assertEquals(4,bar.getChildCount());assertEquals(height,bar.getBottom());
                    assertEquals(Color.parseColor("#FFFFFF"),((android.graphics.drawable.ColorDrawable)bar.getBackground()).getColor());
                    int firstWidth=bar.getChildAt(0).getWidth();
                    for(int i=0;i<4;i++){
                        ViewGroup tab=(ViewGroup)bar.getChildAt(i);
                        assertEquals(firstWidth,tab.getWidth(),1);assertTrue(tab.getWidth()>=48*density);assertTrue(tab.getHeight()>=48*density);
                        for(int j=0;j<tab.getChildCount();j++){
                            View child=tab.getChildAt(j);
                            if(child instanceof ImageView){assertEquals(Math.round(22*density),child.getWidth());assertEquals(Math.round(22*density),child.getHeight());}
                            if(child instanceof TextView){TextView text=(TextView)child;
                                assertEquals(android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP,12,themed.getResources().getDisplayMetrics()),text.getTextSize(),1f);
                                assertNotNull(text.getLayout());assertTrue(text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
                                for(int line=0;line<text.getLineCount();line++)assertTrue("Tab labels must retain an 8dp gap between neighbours",text.getLayout().getLineWidth(line)<=tab.getWidth()-8*density+1);
                            }
                        }
                    }
                }
        });
    }

    @Test public void realTabsKeepBrowseReviewNewAndQuizWithoutDuplicates() throws Exception {
        assertTrue("Existing user login required",new SessionManager(context()).isLoggedIn());
        launch(new Intent(context(),TrangChuActivity.class));TrangChuActivity home=await(TrangChuActivity.class);
        assertNav(home,NavigationRoute.HOME);tap(home,R.id.homeTabHome);assertSame(home,await(TrangChuActivity.class));
        waitHome(home);screenshot("home.png");drawerBack(home);
        tap(home,R.id.buttonTopics);ChuDeActivity topics=await(ChuDeActivity.class);waitTopics(topics);
        ChuDeActivity initialTopics=topics;
        main(()->{
            ChipGroup chips=initialTopics.findViewById(R.id.topicsFilterChips);
            for(int i=0;i<chips.getChildCount();i++)if("SEMANTIC".equals(chips.getChildAt(i).getTag()))chips.getChildAt(i).performClick();
            ((TextView)initialTopics.findViewById(R.id.inputTopicSearch)).setText("Food");
        });waitTopics(topics);screenshot("topics.png");drawerBack(topics);
        ChiTietChuDeActivity detail=openFood(topics);screenshot("topic-detail.png");drawerBack(detail);
        tap(detail,R.id.buttonTopicFlashcards);TheTuVungActivity browse=awaitFlash(NavigationRoute.TOPICS);
        while(browse.getLearningViewModel().getGlobalPosition()>1){TheTuVungActivity current=browse;main(()->current.getLearningViewModel().previous());waitFlash(browse);}
        if(visible(browse,R.id.layoutCardBack))tap(browse,R.id.buttonFlipCard);
        screenshot("flashcard-front.png");
        TheTuVungActivity firstBrowse=browse;main(()->firstBrowse.getLearningViewModel().next());waitFlash(browse);tap(browse,R.id.buttonFlipCard);
        FlashcardViewModel browseVm=browse.getLearningViewModel();Object browseCards=browseVm.getFlashcards().getValue();
        int position=browseVm.getGlobalPosition();assertEquals(2,position);assertTrue(visible(browse,R.id.layoutCardBack));
        screenshot("flashcard.png");drawerBack(browse);
        tap(browse,R.id.homeTabHome);home=await(TrangChuActivity.class);tap(home,R.id.buttonTopics);
        topics=await(ChuDeActivity.class);assertSearch(topics);detail=openFood(topics);
        tap(detail,R.id.buttonTopicFlashcards);browse=awaitFlash(NavigationRoute.TOPICS);
        assertSame(browseVm,browse.getLearningViewModel());assertSame(browseCards,browseVm.getFlashcards().getValue());
        assertEquals(position,browseVm.getGlobalPosition());assertTrue(visible(browse,R.id.layoutCardBack));
        tap(browse,R.id.buttonReviewToday);TheTuVungActivity review=awaitFlash(NavigationRoute.REVIEW_TODAY);
        FlashcardViewModel reviewVm=review.getLearningViewModel();Object queue=reviewVm.getFlashcards().getValue();
        int processed=reviewVm.getSessionProcessed().getValue(),total=reviewVm.getSessionTotal().getValue();
        assertFalse(reviewVm.getReviewCompleted().getValue());screenshot("review-today.png");
        tap(review,R.id.buttonQuiz);TracNghiemActivity quiz=await(TracNghiemActivity.class);waitQuiz(quiz);
        QuizViewModel quizVm=quiz.getLearningViewModel();Object question=quizVm.getCurrentQuestion();
        assertEquals(0,quizVm.getAnsweredCount());screenshot("quiz.png");drawerBack(quiz);
        tap(quiz,R.id.homeTabHome);home=await(TrangChuActivity.class);tap(home,R.id.buttonQuiz);quiz=await(TracNghiemActivity.class);
        assertSame(quizVm,quiz.getLearningViewModel());assertSame(question,quizVm.getCurrentQuestion());assertEquals(0,quizVm.getAnsweredCount());
        tap(quiz,R.id.buttonReviewToday);review=awaitFlash(NavigationRoute.REVIEW_TODAY);
        assertSame(reviewVm,review.getLearningViewModel());assertSame(queue,reviewVm.getFlashcards().getValue());
        assertEquals(processed,(int)reviewVm.getSessionProcessed().getValue());assertEquals(total,(int)reviewVm.getSessionTotal().getValue());
        tap(review,R.id.homeTabHome);home=await(TrangChuActivity.class);openNew(home);
        TheTuVungActivity fresh=awaitFlash(NavigationRoute.HOME);FlashcardViewModel newVm=fresh.getLearningViewModel();Object newQueue=newVm.getFlashcards().getValue();
        if(!newVm.getFlashcards().getValue().isEmpty()&&!visible(fresh,R.id.layoutCardBack))tap(fresh,R.id.buttonFlipCard);
        boolean newBack=visible(fresh,R.id.layoutCardBack);screenshot("new-today.png");
        tap(fresh,R.id.buttonReviewToday);review=awaitFlash(NavigationRoute.REVIEW_TODAY);assertSame(reviewVm,review.getLearningViewModel());
        tap(review,R.id.homeTabHome);home=await(TrangChuActivity.class);openNew(home);fresh=awaitFlash(NavigationRoute.HOME);
        assertSame(newVm,fresh.getLearningViewModel());assertSame(newQueue,newVm.getFlashcards().getValue());assertEquals(newBack,visible(fresh,R.id.layoutCardBack));
        tap(fresh,R.id.buttonTopics);topics=await(ChuDeActivity.class);assertSearch(topics);detail=openFood(topics);
        tap(detail,R.id.buttonTopicFlashcards);browse=awaitFlash(NavigationRoute.TOPICS);
        assertSame(browseVm,browse.getLearningViewModel());assertEquals(position,browseVm.getGlobalPosition());assertTrue(visible(browse,R.id.layoutCardBack));
        assertNoDuplicates();
        tap(browse,R.id.homeTabHome);home=(TrangChuActivity)rotate(await(TrangChuActivity.class),NavigationRoute.HOME);waitHome(home);screenshot("home-rotated.png");
        tap(home,R.id.buttonTopics);topics=(ChuDeActivity)rotate(await(ChuDeActivity.class),NavigationRoute.TOPICS);assertSearch(topics);
        detail=(ChiTietChuDeActivity)rotate(openFood(topics),NavigationRoute.TOPICS);
        tap(detail,R.id.buttonTopicFlashcards);browse=(TheTuVungActivity)rotate(awaitFlash(NavigationRoute.TOPICS),NavigationRoute.TOPICS);
        assertSame(browseVm,browse.getLearningViewModel());assertEquals(position,browseVm.getGlobalPosition());assertTrue(visible(browse,R.id.layoutCardBack));
        tap(browse,R.id.buttonReviewToday);review=awaitFlash(NavigationRoute.REVIEW_TODAY);assertSame(reviewVm,review.getLearningViewModel());
        tap(review,R.id.buttonQuiz);quiz=(TracNghiemActivity)rotate(await(TracNghiemActivity.class),NavigationRoute.QUIZ);
        assertSame(quizVm,quiz.getLearningViewModel());assertSame(question,quizVm.getCurrentQuestion());
        assertNoDuplicates();drawerBack(quiz);
        TracNghiemActivity leaving=quiz;main(()->leaving.getOnBackPressedDispatcher().onBackPressed());settle();
        assertTrue("Zero-answer Back follows existing LEAVE policy",leaving.isFinishing()||leaving.isDestroyed());
        launch(new Intent(context(),TrangChuActivity.class));await(TrangChuActivity.class);assertNoDuplicates();
    }

    @Test public void actualInsetsFitFiveScreensWithoutWaitingForQuizResponses()throws Exception{
        Class<?>[] types={TrangChuActivity.class,ChuDeActivity.class,ChiTietChuDeActivity.class,TheTuVungActivity.class,TracNghiemActivity.class};
        for(Class<?> type:types){
            Intent intent=new Intent(context(),type);
            if(type==ChiTietChuDeActivity.class)intent.putExtra(ChiTietChuDeActivity.EXTRA_TOPIC_CODE,"FOOD");
            if(type==TheTuVungActivity.class)intent.putExtra(TheTuVungActivity.EXTRA_TOPIC_CODE,"FOOD");
            launch(intent);AppCompatActivity a=await(type.asSubclass(AppCompatActivity.class));
            NavigationRoute route=type==TrangChuActivity.class?NavigationRoute.HOME:type==TracNghiemActivity.class?NavigationRoute.QUIZ:NavigationRoute.TOPICS;
            screenshot("insets-"+type.getSimpleName()+"-portrait.png");assertNav(a,route);rotate(a,route);
        }
        launch(new Intent(context(),TrangChuActivity.class));await(TrangChuActivity.class);
    }
    private static void openNew(TrangChuActivity home){waitUntil(()->home.findViewById(R.id.buttonLearnNewToday).isEnabled());tap(home,R.id.buttonLearnNewToday);}
    private static ChiTietChuDeActivity openFood(ChuDeActivity activity){
        waitTopics(activity);
        main(()->{RecyclerView list=activity.findViewById(R.id.recyclerTopics);assertEquals(1,list.getAdapter().getItemCount());list.scrollToPosition(0);});settle();
        main(()->{RecyclerView list=activity.findViewById(R.id.recyclerTopics);assertNotNull(list.findViewHolderForAdapterPosition(0));list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.cardTopic).performClick();});
        ChiTietChuDeActivity detail=await(ChiTietChuDeActivity.class);
        waitUntil(()->{TopicDetailViewModel vm=new ViewModelProvider(detail).get(TopicDetailViewModel.class);return vm.getTopic().getValue()!=null&&!Boolean.TRUE.equals(vm.getPreviewLoading().getValue());});
        assertNav(detail,NavigationRoute.TOPICS);return detail;
    }
    private static void assertSearch(ChuDeActivity a){main(()->{assertEquals("Food",((TextView)a.findViewById(R.id.inputTopicSearch)).getText().toString());ChipGroup chips=a.findViewById(R.id.topicsFilterChips);boolean selected=false;for(int i=0;i<chips.getChildCount();i++)if("SEMANTIC".equals(chips.getChildAt(i).getTag()))selected=((Chip)chips.getChildAt(i)).isChecked();assertTrue(selected);});}
    private static void waitHome(TrangChuActivity a){waitUntil(()->a.findViewById(R.id.progressHomeLoading).getVisibility()!=View.VISIBLE);}
    private static void waitTopics(ChuDeActivity a){waitUntil(()->{TopicsViewModel vm=new ViewModelProvider(a).get(TopicsViewModel.class);return vm.getTopics().getValue()!=null&&!Boolean.TRUE.equals(vm.getLoading().getValue());});settle();}
    private static void waitFlash(TheTuVungActivity a){waitUntil(()->a.getLearningViewModel()!=null&&a.getLearningViewModel().getFlashcards().getValue()!=null&&!Boolean.TRUE.equals(a.getLearningViewModel().getLoading().getValue()));settle();}
    private static void waitQuiz(TracNghiemActivity a){waitUntil(()->a.getLearningViewModel()!=null&&a.getLearningViewModel().getState().getValue()!=QuizViewModel.QuizState.LOADING_QUESTIONS);assertEquals("Quiz GET error: "+a.getLearningViewModel().getErrorMessage().getValue(),QuizViewModel.QuizState.QUESTION,a.getLearningViewModel().getState().getValue());}
    private static TheTuVungActivity awaitFlash(NavigationRoute selected){
        final TheTuVungActivity[] result={null};waitUntil(()->{Activity a=resumed();if(!(a instanceof TheTuVungActivity))return false;TheTuVungActivity f=(TheTuVungActivity)a;View tab=f.findViewById(tabId(selected));if(tab==null||!tab.isSelected()||f.getLearningViewModel()==null)return false;result[0]=f;return f.getLearningViewModel().getFlashcards().getValue()!=null&&!Boolean.TRUE.equals(f.getLearningViewModel().getLoading().getValue());});
        assertNav(result[0],selected);settle();return result[0];
    }
    private static AppCompatActivity rotate(AppCompatActivity activity,NavigationRoute selected)throws Exception{
        Object vm=ownerVm(activity);Class<?> type=activity.getClass();
        main(()->activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        final AppCompatActivity[] landscape={null};try{waitUntil(()->{Activity a=resumed();if(type.isInstance(a)&&a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE){landscape[0]=(AppCompatActivity)a;return true;}return false;});}catch(AssertionError e){throw new AssertionError("Landscape/focus did not settle: "+type.getSimpleName(),e);}settle();
        screenshot("insets-"+type.getSimpleName()+"-landscape.png");assertSame(vm,ownerVm(landscape[0]));assertNav(landscape[0],selected);
        main(()->landscape[0].setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        final AppCompatActivity[] portrait={null};try{waitUntil(()->{Activity a=resumed();if(type.isInstance(a)&&a.getResources().getConfiguration().orientation==Configuration.ORIENTATION_PORTRAIT){portrait[0]=(AppCompatActivity)a;return true;}return false;});}catch(AssertionError e){throw new AssertionError("Portrait/focus did not settle: "+type.getSimpleName(),e);}settle();
        assertSame(vm,ownerVm(portrait[0]));assertNav(portrait[0],selected);return portrait[0];
    }
    private static Object ownerVm(AppCompatActivity a){final Object[] vm={null};main(()->{if(a instanceof TheTuVungActivity)vm[0]=((TheTuVungActivity)a).getLearningViewModel();else if(a instanceof TracNghiemActivity)vm[0]=((TracNghiemActivity)a).getLearningViewModel();else if(a instanceof ChuDeActivity)vm[0]=new ViewModelProvider(a).get(TopicsViewModel.class);else if(a instanceof ChiTietChuDeActivity)vm[0]=new ViewModelProvider(a).get(TopicDetailViewModel.class);else vm[0]=new ViewModelProvider(a).get(com.example.bivoca.ui.profile.ProfileViewModel.class);});return vm[0];}
    private static void drawerBack(AppCompatActivity a){
        if(a instanceof TheTuVungActivity){main(()->((androidx.core.widget.NestedScrollView)a.findViewById(R.id.flashcardPageScroll)).scrollTo(0,0));settle();tap(a,R.id.buttonFlashcardMenu);onView(withText(R.string.nav_open_description)).perform(click());}
        else if(a instanceof TracNghiemActivity)tap(a,R.id.buttonNavigationAvatar);else tap(a,R.id.buttonHomeMenu);
        settle();main(()->assertTrue(((DrawerLayout)a.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END)));
        main(()->a.getOnBackPressedDispatcher().onBackPressed());settle();main(()->{assertFalse(((DrawerLayout)a.findViewById(R.id.navigationDrawerLayout)).isDrawerOpen(GravityCompat.END));assertFalse(a.isFinishing());});
    }
    private static int tabId(NavigationRoute r){return r==NavigationRoute.HOME?R.id.homeTabHome:r==NavigationRoute.TOPICS?R.id.buttonTopics:r==NavigationRoute.REVIEW_TODAY?R.id.buttonReviewToday:R.id.buttonQuiz;}
    private static void assertNav(AppCompatActivity a,NavigationRoute selected){main(()->{
        ViewGroup bar=a.findViewById(R.id.homeBottomNavigation);assertNotNull(bar);assertEquals(4,bar.getChildCount());
        for(int i=0;i<4;i++){ViewGroup tab=(ViewGroup)bar.getChildAt(i);boolean active=tab.getId()==tabId(selected);assertEquals(active,tab.isSelected());for(int j=0;j<tab.getChildCount();j++)if(tab.getChildAt(j)instanceof TextView){TextView label=(TextView)tab.getChildAt(j);assertEquals(Color.parseColor(active?"#2855D5":"#5E6B79"),label.getCurrentTextColor());assertEquals(active,label.getTypeface()!=null&&label.getTypeface().isBold());}}
        Rect visible=new Rect();assertTrue(bar.getGlobalVisibleRect(visible));assertEquals(a.getClass().getSimpleName()+" orientation="+a.getResources().getConfiguration().orientation+" bar="+bar.getHeight()+" visible="+visible,bar.getHeight(),visible.height());
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(a.getWindow().getDecorView());if(insets!=null){int[] xy=new int[2];bar.getLocationOnScreen(xy);assertTrue(xy[1]+bar.getHeight()<=a.getWindow().getDecorView().getHeight()-insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom+1);}
    });}
    private static void assertNoDuplicates(){main(()->{Set<Activity> all=new HashSet<>();for(Stage s:new Stage[]{Stage.CREATED,Stage.STARTED,Stage.RESUMED,Stage.PAUSED,Stage.STOPPED})all.addAll(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(s));for(Class<?> type:new Class[]{TrangChuActivity.class,ChuDeActivity.class,ChiTietChuDeActivity.class,TheTuVungActivity.class,TracNghiemActivity.class}){int count=0;for(Activity a:all)if(type.isInstance(a)&&!a.isFinishing()&&!a.isDestroyed())count++;assertTrue("Duplicate "+type.getSimpleName()+": "+count,count<=1);}});}
    private interface Condition {boolean ready();}
    private static void waitUntil(Condition c){boolean[] ok={false};long deadline=SystemClock.uptimeMillis()+20000;do{main(()->ok[0]=c.ready());if(ok[0])return;SystemClock.sleep(100);}while(SystemClock.uptimeMillis()<deadline);fail("Navigation/API did not settle");}
    private static Activity resumed(){for(Activity a:ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))if(a.getClass().getName().startsWith("com.example.bivoca.")&&!a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus())return a;return null;}
    private static <T extends AppCompatActivity>T await(Class<T> type){final AppCompatActivity[] result={null};waitUntil(()->{Activity a=resumed();if(type.isInstance(a)){result[0]=(AppCompatActivity)a;return true;}return false;});settle();return type.cast(result[0]);}
    private static boolean visible(Activity a,int id){boolean[] result={false};main(()->result[0]=a.findViewById(id).getVisibility()==View.VISIBLE);return result[0];}
    private static void tap(Activity a,int id){main(()->a.findViewById(id).performClick());settle();}
    private static void launch(Intent intent){intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);context().startActivity(intent);settle();}
    private static void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private static Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private static void settle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450);}
    private static void screenshot(String name)throws Exception{settle();Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);File dir=new File(context().getExternalFilesDir(null),"phase-e-navigation");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));}image.recycle();}
}
