package com.example.bivoca.ui.progress;
import android.content.Context; import android.graphics.*; import android.util.AttributeSet; import android.view.View;
import androidx.annotation.Nullable; import com.example.bivoca.data.model.LearningDay;
import java.util.Collections; import java.util.List;
public final class WeeklyActivityChartView extends View {
 private List<LearningDay> days=Collections.emptyList(); private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
 public WeeklyActivityChartView(Context c,@Nullable AttributeSet a){super(c,a);paint.setTypeface(Typeface.create(Typeface.DEFAULT,Typeface.BOLD));}
 public void setDays(List<LearningDay> value){days=value==null?Collections.emptyList():value;invalidate();}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(days.isEmpty())return;
  float left=28*getResources().getDisplayMetrics().density,bottom=getHeight()-28*getResources().getDisplayMetrics().density;
  float top=18*getResources().getDisplayMetrics().density,w=(getWidth()-left-8)/days.size();int max=1;
  for(LearningDay d:days)max=Math.max(max,Math.max(d.getNewWords(),d.getReviews()));
  paint.setTextSize(10*getResources().getDisplayMetrics().scaledDensity);paint.setTextAlign(Paint.Align.CENTER);
  for(int i=0;i<days.size();i++){LearningDay d=days.get(i);float x=left+i*w;float h=bottom-top;
   float hn=h*d.getNewWords()/max,hr=h*d.getReviews()/max;paint.setColor(Color.rgb(34,211,238));c.drawRoundRect(x+w*.15f,bottom-hn,x+w*.42f,bottom,5,5,paint);
   paint.setColor(Color.rgb(59,130,246));c.drawRoundRect(x+w*.5f,bottom-hr,x+w*.77f,bottom,5,5,paint);
   paint.setColor(Color.rgb(148,163,184));String date=d.getDate();c.drawText(date==null?"":date.substring(Math.max(0,date.length()-5)),x+w*.46f,getHeight()-7,paint);
   paint.setColor(Color.WHITE);if(d.getNewWords()>0)c.drawText(String.valueOf(d.getNewWords()),x+w*.285f,bottom-hn-4,paint);
   if(d.getReviews()>0)c.drawText(String.valueOf(d.getReviews()),x+w*.635f,bottom-hr-4,paint);
  }}
}
