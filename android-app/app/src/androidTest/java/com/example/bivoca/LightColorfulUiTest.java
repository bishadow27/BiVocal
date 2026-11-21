package com.example.bivoca;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.bivoca.ui.auth.DangKyActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Dedicated on-device typography test sheet, not a product screen or business-state fixture. */
@RunWith(AndroidJUnit4.class)
public class LightColorfulUiTest {
    private static final String GLYPHS="Tiếng Việt: ă â ê ô ơ ư đ\nSắc, huyền, hỏi, ngã, nặng.\nĐăng ký tài khoản – Chủ đề – Trắc nghiệm – Thuần thục.\nNguyễn, Trường, Hướng dẫn, Từ vựng.";
    @Test public void editorialHeadingsUseRealExtraBoldNotThemeFallback(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            android.content.Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();
            int[] layouts={R.layout.activity_dang_nhap,R.layout.view_auth_brand_header,R.layout.activity_trang_chu};
            int[] ids={R.id.loginHeaderTitle,R.id.authScreenTitle,R.id.textHomeSlogan};
            int[] themes={R.style.Theme_BiVoca_Login_Approved,R.style.Theme_BiVoca_Login_Reference,R.style.Theme_BiVoca_Home_Approved};
            for(int i=0;i<layouts.length;i++){
                android.content.Context themed=new android.view.ContextThemeWrapper(base,themes[i]);
                View root=android.view.LayoutInflater.from(themed).inflate(layouts[i],null,false);TextView title=root.findViewById(ids[i]);
                if(Build.VERSION.SDK_INT>=28)assertEquals("Heading fell back to theme font: "+ids[i],800,title.getTypeface().getWeight());
                assertFalse(title.getPaint().isFakeBoldText());
            }
        });
    }
    @Test public void actualAuthHeadingUsesDeclaredExtraBold()throws Exception{
        try(ActivityScenario<DangKyActivity> scenario=ActivityScenario.launch(DangKyActivity.class)){
            settle();shot("register-font-diagnostic.png");
            scenario.onActivity(a->{TextView title=a.findViewById(R.id.authScreenTitle);
                if(Build.VERSION.SDK_INT>=28)assertEquals("AppCompat actual Activity heading weight",800,title.getTypeface().getWeight());
                assertFalse(title.getPaint().isFakeBoldText());
            });
        }
    }
    @Test public void vietnameseGlyphsUseFiveRealLocalWeightsWithoutClipping()throws Exception{
        try(ActivityScenario<DangKyActivity> scenario=ActivityScenario.launch(DangKyActivity.class)){
            final ScrollView[] scroll={null};List<TextView> samples=new ArrayList<>();
            scenario.onActivity(a->{
                float d=a.getResources().getDisplayMetrics().density;
                ScrollView page=new ScrollView(a);scroll[0]=page;page.setFillViewport(true);page.setBackgroundColor(a.getColor(R.color.bivocal_light_canvas));
                LinearLayout stack=new LinearLayout(a);stack.setOrientation(LinearLayout.VERTICAL);stack.setPadding((int)(16*d),(int)(32*d),(int)(16*d),(int)(32*d));page.addView(stack);
                int[] ids={R.font.be_vietnam_pro_regular,R.font.be_vietnam_pro_medium,R.font.be_vietnam_pro_semibold,R.font.be_vietnam_pro_bold,R.font.be_vietnam_pro_extrabold};
                for(int i=0;i<ids.length;i++){
                    Typeface font=ResourcesCompat.getFont(a,ids[i]);assertNotNull(font);if(Build.VERSION.SDK_INT>=28)assertEquals(400+i*100,font.getWeight());
                    TextView title=new TextView(a);title.setText("Be Vietnam Pro · "+(400+i*100));title.setTypeface(font);title.setTextSize(12);title.setTextColor(a.getColor(R.color.bivocal_light_cobalt));title.setPadding(0,(int)(12*d),0,(int)(4*d));stack.addView(title);
                    TextView text=new TextView(a);text.setTypeface(font);text.setText(GLYPHS);text.setTextSize(16);text.setTextColor(a.getColor(R.color.bivocal_light_text));text.setIncludeFontPadding(true);text.setLetterSpacing(0);text.setLineSpacing(3*d,1.1f);stack.addView(text);samples.add(text);
                    assertFalse(text.getPaint().isFakeBoldText());for(char c:GLYPHS.toCharArray())if(Character.isLetter(c))assertTrue("Missing glyph: "+c,text.getPaint().hasGlyph(String.valueOf(c)));
                }a.setContentView(page);
            });
            settle();scenario.onActivity(a->{for(TextView t:samples){assertNotNull(t.getLayout());assertTrue(t.getLayout().getLineBottom(t.getLineCount()-1)<=t.getHeight()-t.getExtendedPaddingBottom());}});
            shot("vietnamese-glyphs-top.png");scenario.onActivity(a->scroll[0].fullScroll(View.FOCUS_DOWN));settle();shot("vietnamese-glyphs-bottom.png");
        }
    }
    static void settle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(350);}
    static void shot(String name)throws Exception{settle();Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),"m16-6-light-colorful");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();}
}
