package com.example.bivoca.ui.quiz;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/** Measures intrinsic wrapping height first, then gives all four cards the same height. */
public final class QuizAnswerGrid extends LinearLayout {
    public QuizAnswerGrid(Context context,AttributeSet attrs){super(context,attrs);}
    @Override protected void onMeasure(int width,int height){
        int minimum=Math.round((getResources().getConfiguration().fontScale>=1.3f?84:76)*getResources().getDisplayMetrics().density);
        for(int row=0;row<getChildCount();row++){
            ViewGroup group=(ViewGroup)getChildAt(row);
            for(int col=0;col<group.getChildCount();col++){View cell=group.getChildAt(col);cell.getLayoutParams().height=LayoutParams.WRAP_CONTENT;cell.setMinimumHeight(minimum);}
        }
        super.onMeasure(width,height);int max=minimum;
        for(int row=0;row<getChildCount();row++){ViewGroup group=(ViewGroup)getChildAt(row);for(int col=0;col<group.getChildCount();col++)max=Math.max(max,group.getChildAt(col).getMeasuredHeight());}
        for(int row=0;row<getChildCount();row++){ViewGroup group=(ViewGroup)getChildAt(row);for(int col=0;col<group.getChildCount();col++)group.getChildAt(col).getLayoutParams().height=max;}
        super.onMeasure(width,height);
    }
}
