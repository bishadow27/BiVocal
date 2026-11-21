package com.example.bivoca;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HomeResponsiveUiTest {
    @Test public void homeFitsReferenceSmallLandscapeAndLargeFontWithoutClipping() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            int[][] sizes={{320,640},{360,800},{411,731},{412,915},{440,915},{467,830},{731,411}};
            for(int[] size:sizes) for(float scale:new float[]{1f,1.3f}) {
                Configuration config=new Configuration(base.getResources().getConfiguration());
                config.fontScale=scale; config.screenWidthDp=size[0]; config.screenHeightDp=size[1];
                config.orientation=size[0]>size[1]?Configuration.ORIENTATION_LANDSCAPE:Configuration.ORIENTATION_PORTRAIT;
                Context themed=new ContextThemeWrapper(base.createConfigurationContext(config),R.style.Theme_BiVoca_Home_Approved);
                View root=LayoutInflater.from(themed).inflate(R.layout.activity_trang_chu,null,false);
                ((TextView)root.findViewById(R.id.textHomeStreakBadge)).setText("120 day");
                ((TextView)root.findViewById(R.id.textDailyGoal)).setText("10 / 10 từ mới");
                ((TextView)root.findViewById(R.id.textDailyGoalPercent)).setText("100%");
                float density=themed.getResources().getDisplayMetrics().density;
                int w=Math.round(size[0]*density),h=Math.round(size[1]*density);
                root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                root.layout(0,0,w,h);
                String label=size[0]+"x"+size[1]+" scale="+scale;
                View bottom=root.findViewById(R.id.homeBottomNavigation);
                ScrollView scroll=root.findViewById(R.id.homeScroll);
                assertEquals(label,h,bottom.getBottom());
                assertTrue(label,scroll.getBottom()<=bottom.getTop());
                assertTrue(label,bottom.getHeight()>=Math.round(64*density));
                assertTrue(label+" bottom nav horizontal overflow",bottom.getRight()<=w);
                TextView slogan=root.findViewById(R.id.textHomeSlogan);
                assertTrue(label+" editorial slogan must wrap rather than truncate",slogan.getLineCount()<=6);
                assertEquals(label+" slogan truncated",slogan.getText().length(),slogan.getLayout().getLineEnd(slogan.getLineCount()-1));
                View info=root.findViewById(R.id.buttonHomeSrsInfo);
                assertTrue(label+" info touch target",info.getWidth()>=Math.round(48*density)&&info.getHeight()>=Math.round(48*density));
                TextView streak=root.findViewById(R.id.textHomeStreakBadge);
                assertEquals("0 day",themed.getString(R.string.home_approved_streak,"0"));
                assertEquals("120 day",themed.getString(R.string.home_approved_streak,"120"));
                View pill=root.findViewById(R.id.homeStreakPill);
                assertNotNull(label+" streak background",pill.getBackground());
                assertTrue(label+" count must stay on one line",streak.getLineCount()==1);
                assertTrue(label+" pill must fit header",pill.getWidth()+root.findViewById(R.id.buttonHomeMenu).getWidth()<w);
                View avatar=root.findViewById(R.id.buttonHomeMenu);
                assertTrue(label+" avatar outside header",avatar.getWidth()>=Math.round(48*density));
                assertTextFits(root,label);
                int originalBottom=bottom.getTop();
                scroll.scrollTo(0,scroll.getChildAt(0).getHeight());
                assertEquals(label+" fixed navigation moved",originalBottom,bottom.getTop());
                View lastCard=root.findViewById(R.id.cardHomeLeaderboard);
                assertTrue(label+" last card unreachable",lastCard.getBottom()-scroll.getScrollY()<=scroll.getHeight());
            }
        });
    }
    private static void assertTextFits(View view,String label) {
        if(view.getVisibility()!=View.VISIBLE)return;
        if(view instanceof TextView) {
            TextView text=(TextView)view;
            if(text.getText().length()>0) {
                assertTrue(label+" zero text width",text.getWidth()>0);
                assertNotNull(label,text.getLayout());
                assertTrue(label+" text clipped: "+text.getText(),text.getLayout().getHeight()<=text.getHeight()-text.getCompoundPaddingTop()-text.getCompoundPaddingBottom());
            }
        }
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++)assertTextFits(((ViewGroup)view).getChildAt(i),label);
    }
}

