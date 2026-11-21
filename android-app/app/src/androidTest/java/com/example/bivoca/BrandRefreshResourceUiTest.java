package com.example.bivoca;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Detached resource checks only: no Activity navigation, network, auth or learning writes. */
@RunWith(AndroidJUnit4.class)
public class BrandRefreshResourceUiTest {
 @Test public void configurationAndQuizLayoutsFitSmallLargeFontAndNightResources(){
  InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
   Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
   int[][] sizes={{320,640},{360,800},{412,915},{640,320}};
   for(int[] size:sizes)for(float scale:new float[]{1f,1.3f})for(int night:new int[]{Configuration.UI_MODE_NIGHT_NO,Configuration.UI_MODE_NIGHT_YES}){
    Configuration config=new Configuration(base.getResources().getConfiguration());config.fontScale=scale;config.screenWidthDp=size[0];config.screenHeightDp=size[1];config.orientation=size[0]>size[1]?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;config.uiMode=(config.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
    Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca);
    assertEquals("Light palette stays intentional in system night mode",0xFFFFFBF5,themed.getColor(R.color.brand_canvas));
    for(int layout:new int[]{R.layout.view_quiz_configuration,R.layout.view_quiz_session_question,R.layout.view_quiz_session_result,R.layout.activity_cai_dat}){
     View root=LayoutInflater.from(themed).inflate(layout,null,false);float d=themed.getResources().getDisplayMetrics().density;
     int availableDp=layout==R.layout.view_quiz_configuration?Math.min(360,size[0]-32)-40:size[0];
     int width=Math.round(availableDp*d);root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(Math.round(size[1]*d),View.MeasureSpec.AT_MOST));root.layout(0,0,width,root.getMeasuredHeight());
     assertEquals(width,root.getMeasuredWidth());
     if(layout==R.layout.view_quiz_configuration){TextView start=root.findViewById(R.id.quizV2Start);View counts=root.findViewById(R.id.quizV2Counts);assertEquals("Bắt đầu",start.getText().toString());assertSame(counts.getParent(),start.getParent());assertTrue("Count and CTA both have usable width",counts.getWidth()>0&&start.getWidth()>0);assertTrue("48dp CTA",start.getHeight()>=Math.round(48*d));assertTrue("CTA fits row",start.getRight()<=width);assertNotNull(start.getLayout());for(int line=0;line<start.getLineCount();line++)assertEquals("CTA must not ellipsize",0,start.getLayout().getEllipsisCount(line));}
    }
   }
  });
 }
}
