package com.example.bivoca.ui.navigation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

/** Shared four-tab presentation and existing destinations for Home/Topics. */
public final class LearningBottomNavigation {
    private LearningBottomNavigation() { }
    public static void bind(AppCompatActivity activity, NavigationRoute current) {
        bind(activity, current, Color.parseColor("#38BDF8"), Color.parseColor("#94A3B8"));
    }
    public static void bind(AppCompatActivity activity, NavigationRoute current, int activeColor, int inactiveColor) {
        int[] ids={R.id.homeTabHome,R.id.buttonTopics,R.id.buttonReviewToday,R.id.buttonQuiz};
        NavigationRoute[] routes={NavigationRoute.HOME,NavigationRoute.TOPICS,NavigationRoute.REVIEW_TODAY,NavigationRoute.QUIZ};
        Class<?>[] targets={HomeActivity.class,TopicsActivity.class,FlashcardActivity.class,QuizActivity.class};
        for(int i=0;i<ids.length;i++) {
            final NavigationRoute route=routes[i]; final Class<?> target=targets[i];
            ViewGroup tab=activity.findViewById(ids[i]);
            boolean active=route==current;
            int color=active?activeColor:inactiveColor;
            tab.setSelected(active);
            for(int child=0;child<tab.getChildCount();child++) {
                View view=tab.getChildAt(child);
                if(view instanceof ImageView)((ImageView)view).setImageTintList(ColorStateList.valueOf(color));
                if(view instanceof TextView) {
                    ((TextView)view).setTextColor(color);
                    ((TextView)view).setTypeface(null,active?Typeface.BOLD:Typeface.NORMAL);
                }
            }
            tab.setOnClickListener(view -> {
                if(route==current)return;
                Intent intent=new Intent(activity,target);
                if(route==NavigationRoute.HOME||route==NavigationRoute.TOPICS||route==NavigationRoute.QUIZ)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                if(route==NavigationRoute.REVIEW_TODAY)intent.putExtra(FlashcardActivity.EXTRA_REVIEW_TODAY,true);
                activity.startActivity(intent);
            });
        }
    }
}
