package com.example.bivoca.ui.navigation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.bivoca.R;
import com.example.bivoca.ui.home.HomeActivity;
import com.example.bivoca.ui.topics.TopicsActivity;
import com.example.bivoca.ui.flashcard.FlashcardActivity;
import com.example.bivoca.ui.quiz.QuizActivity;

/** One shared four-tab view and guarded, retained Activity routing. */
public final class LearningBottomNavigation {
    public interface NavigationGuard { void request(Runnable destination); }
    private LearningBottomNavigation() { }

    public static void bind(AppCompatActivity activity, NavigationRoute current) {
        bind(activity,current,Runnable::run);
    }
    public static void bind(AppCompatActivity activity, NavigationRoute current, NavigationGuard guard) {
        bind(activity,current,activity.getColor(R.color.home_approved_cyan),activity.getColor(R.color.home_approved_secondary_text),guard);
    }
    public static void bind(AppCompatActivity activity, NavigationRoute current, int activeColor, int inactiveColor) {
        bind(activity,current,activeColor,inactiveColor,Runnable::run);
    }
    private static void bind(AppCompatActivity activity, NavigationRoute current, int activeColor, int inactiveColor, NavigationGuard guard) {
        ViewGroup bar=activity.findViewById(R.id.homeBottomNavigation);
        bar.setTag(R.id.homeBottomNavigation,guard);
        int[] ids={R.id.homeTabHome,R.id.buttonTopics,R.id.buttonReviewToday,R.id.buttonQuiz};
        NavigationRoute[] routes={NavigationRoute.HOME,NavigationRoute.TOPICS,NavigationRoute.REVIEW_TODAY,NavigationRoute.QUIZ};
        NavigationRoute selected=NavigationRoutePolicy.selectedMenuFor(current);
        for(int i=0;i<ids.length;i++) {
            final NavigationRoute route=routes[i];
            ViewGroup tab=activity.findViewById(ids[i]);
            boolean active=route==selected;int color=active?activeColor:inactiveColor;
            tab.setSelected(active);tab.setActivated(active);
            for(int j=0;j<tab.getChildCount();j++) {
                View child=tab.getChildAt(j);
                if(child instanceof ImageView)((ImageView)child).setImageTintList(ColorStateList.valueOf(color));
                if(child instanceof TextView){((TextView)child).setTextColor(color);((TextView)child).setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity,active?R.font.be_vietnam_pro_bold:R.font.be_vietnam_pro_regular));}
            }
            tab.setOnClickListener(v->{if(!NavigationRoutePolicy.shouldReuseCurrentScreen(current,route))navigate(activity,route);});
        }
    }

    public static void navigate(AppCompatActivity activity, NavigationRoute destination) {
        Intent intent;
        switch(destination) {
            case HOME: intent=new Intent(activity,HomeActivity.class);break;
            case TOPICS: intent=new Intent(activity,TopicsActivity.class);break;
            case REVIEW_TODAY: intent=new Intent(activity,FlashcardActivity.class).putExtra(FlashcardActivity.EXTRA_REVIEW_TODAY,true);break;
            case QUIZ: intent=new Intent(activity,QuizActivity.class);break;
            default: throw new IllegalArgumentException("Not a learning tab");
        }
        open(activity,intent);
    }

    public static void open(AppCompatActivity activity, Intent intent) {
        requestNavigation(activity,()->startRetained(activity,intent));
    }

    public static void requestNavigation(AppCompatActivity activity, Runnable destination) {
        if(activity.isFinishing()||activity.isDestroyed())return;
        View bar=activity.findViewById(R.id.homeBottomNavigation);
        Object guard=bar==null?null:bar.getTag(R.id.homeBottomNavigation);
        Runnable checked=()->{if(!activity.isFinishing()&&!activity.isDestroyed())destination.run();};
        if(guard instanceof NavigationGuard)((NavigationGuard)guard).request(checked);else checked.run();
    }

    static void startRetained(AppCompatActivity activity, Intent intent) {
        intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void setEnabled(AppCompatActivity activity, boolean enabled) {
        ViewGroup bar=activity.findViewById(R.id.homeBottomNavigation);
        if(bar==null)return;
        for(int i=0;i<bar.getChildCount();i++){View tab=bar.getChildAt(i);tab.setEnabled(enabled);tab.setAlpha(enabled?1f:0.4f);}
    }
}
