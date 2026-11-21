package com.example.bivoca.ui.quiz;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import com.example.bivoca.R;
import com.example.bivoca.data.model.Topic;
import com.example.bivoca.data.model.QuizSessionDtos.*;
import com.example.bivoca.ui.navigation.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.*;

/** Native XML renderer; all requests, receipts and session decisions remain in QuizViewModel. */
public final class QuizSessionScreen {
    public static final String[] METHODS={"MIXED","CHALLENGE","CONTEXT","EN_TO_VI","VI_TO_EN","MATCHING"};
    public static final String[] TITLES={"Tổng hợp","Vượt ải","Trắc nghiệm (Ngữ cảnh)","Trắc nghiệm (Từ-Nghĩa)","Trắc nghiệm (Nghĩa-Từ)","Nối từ với nghĩa"};
    private static final String[] DESCRIPTIONS={"Kết hợp nhiều dạng câu hỏi trong cùng một bài luyện tập.","Trả lời liên tiếp để vượt qua các thử thách.","Đọc câu và chọn từ phù hợp với ngữ cảnh.","Nhìn từ tiếng Anh và chọn nghĩa tiếng Việt đúng.","Nhìn nghĩa tiếng Việt và chọn từ tiếng Anh đúng.","Kết nối từ tiếng Anh với nghĩa tiếng Việt tương ứng."};
    private static final int[] BACKGROUNDS={R.drawable.quiz_v2_method_0,R.drawable.quiz_v2_method_1,R.drawable.quiz_v2_method_2,R.drawable.quiz_v2_method_3,R.drawable.quiz_v2_method_4,R.drawable.quiz_v2_method_5};
    private static final int[] ICONS={R.drawable.quiz_v2_icon_0,R.drawable.quiz_v2_icon_1,R.drawable.quiz_v2_icon_2,R.drawable.quiz_v2_icon_3,R.drawable.quiz_v2_icon_4,R.drawable.quiz_v2_icon_5};
    private final int NAVY,COBALT,BORDER;
    private static final int GREEN=Color.rgb(4,120,87),RED=Color.rgb(167,43,62);
    private final int SECONDARY;
    private final AppCompatActivity activity;
    private final QuizViewModel vm;
    private final LinearLayout content;
    private QuizViewModel.SessionScreen rendered;
    private View section;
    private AlertDialog dialog;
    private AlertDialog configurationDialog;
    private TextView configurationError;
    private MaterialButton configurationRetry;
    private boolean reviewExpanded;
    private final android.os.Handler timerHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable timer=new Runnable(){public void run(){updateTimer();timerHandler.postDelayed(this,1000);}};
    private NavigationDrawerController drawer;
    public QuizSessionScreen(AppCompatActivity activity,QuizViewModel vm,String topic){
        this.activity=activity;NAVY=activity.getColor(R.color.brand_ink);COBALT=activity.getColor(R.color.brand_teal_ink);BORDER=activity.getColor(R.color.brand_border);SECONDARY=activity.getColor(R.color.quiz_v2_secondary);this.vm=vm;content=activity.findViewById(R.id.quizV2Content);
        drawer=NavigationDrawerController.attachSession(activity,NavigationRoute.QUIZ,R.id.quizV2Back,this::back);
        drawer.setNavigationGuard(this::navigate);
        activity.findViewById(R.id.buttonHomeMenu).setOnClickListener(v->drawer.open());
        LearningBottomNavigation.bind(activity,NavigationRoute.QUIZ,this::navigate);
        activity.findViewById(R.id.quizV2Retry).setOnClickListener(v->vm.retrySessionV2());
        vm.getSessionRender().observe(activity,value->render());vm.initSessionV2(topic);timerHandler.post(timer);
    }
    private int dp(float n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    @SuppressWarnings("unchecked")
    private <T extends View>T find(int id){View view=section==null?null:section.findViewById(id);return view==null?activity.findViewById(id):(T)view;}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(activity);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setLetterSpacing(0);
        t.setTypeface(ResourcesCompat.getFont(activity,bold?R.font.be_vietnam_pro_bold:R.font.be_vietnam_pro_regular));t.setIncludeFontPadding(true);t.setLineSpacing(dp(3),1);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return t;
    }
    private GradientDrawable bg(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));d.setStroke(dp(1),stroke);return d;}
    private MaterialButton button(String label){
        MaterialButton b=new MaterialButton(activity,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);b.setAllCaps(false);b.setTextSize(13);b.setLetterSpacing(0);b.setTypeface(ResourcesCompat.getFont(activity,R.font.be_vietnam_pro_semibold));b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));b.setCornerRadius(dp(12));b.setTextColor(NAVY);b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));b.setStrokeColor(ColorStateList.valueOf(BORDER));b.setStrokeWidth(dp(1));b.setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);b.setPadding(dp(12),dp(10),dp(12),dp(10));
        b.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));return b;
    }
    public void render(){
        boolean loading=vm.sessionBusy||vm.sessionAvailabilityLoading||vm.sessionCatalogLoading;
        find(R.id.quizV2Loading).setVisibility(loading?View.VISIBLE:View.GONE);
        String error=vm.sessionError;
        if(error==null&&vm.sessionState.pendingControl!=null&&!vm.sessionBusy)error="Yêu cầu kết thúc phiên đang chờ xác nhận. Hãy đồng bộ trước khi tiếp tục.";
        if(error==null&&vm.sessionState.pending!=null&&!vm.sessionBusy)error="Đáp án đang chờ xác nhận. Thử lại bằng đúng mã yêu cầu đã lưu.";
        TextView errorView=find(R.id.quizV2Error);errorView.setText(error);errorView.setVisibility(error==null||vm.sessionScreen==QuizViewModel.SessionScreen.CONFIGURATION?View.GONE:View.VISIBLE);
        find(R.id.quizV2Retry).setVisibility(error==null||vm.sessionScreen==QuizViewModel.SessionScreen.CONFIGURATION?View.GONE:View.VISIBLE);find(R.id.quizV2Retry).setEnabled(!vm.sessionBusy);
        LearningBottomNavigation.setEnabled(activity,!vm.sessionBusy&&vm.sessionState.pending==null&&vm.sessionState.pendingControl==null);
        TextView title=find(R.id.quizV2Title),subtitle=find(R.id.quizV2Subtitle);
        title.setText(vm.sessionScreen==QuizViewModel.SessionScreen.METHODS?"Trắc nghiệm":vm.sessionScreen==QuizViewModel.SessionScreen.CONFIGURATION?"Trắc nghiệm":vm.sessionScreen==QuizViewModel.SessionScreen.RESULTS?"Kết quả luyện tập":title(vm.sessionMethod));
        subtitle.setText(vm.sessionBusy?"Đang đồng bộ với máy chủ…":"Luyện tập theo cách của bạn");
        if(rendered!=vm.sessionScreen){
            dismissConfigurationDialog();section=null;content.removeAllViews();rendered=vm.sessionScreen;reviewExpanded=false;
            if(rendered==QuizViewModel.SessionScreen.METHODS){renderMethods();return;}
            if(rendered==QuizViewModel.SessionScreen.CONFIGURATION){
                renderMethods();section=activity.getLayoutInflater().inflate(R.layout.view_quiz_configuration,content,false);
                bindConfiguration();showConfigurationDialog();
            }else{
                int layout=rendered==QuizViewModel.SessionScreen.QUESTIONS?R.layout.view_quiz_session_question:R.layout.view_quiz_session_result;
                section=activity.getLayoutInflater().inflate(layout,content,false);content.addView(section);
                ((androidx.core.widget.NestedScrollView)find(R.id.quizV2Scroll)).scrollTo(0,0);
            }
        }
        if(rendered==QuizViewModel.SessionScreen.CONFIGURATION){
            renderConfiguration();
            configurationError.setText(error);configurationError.setVisibility(error==null?View.GONE:View.VISIBLE);
            configurationRetry.setVisibility(error==null?View.GONE:View.VISIBLE);configurationRetry.setEnabled(!vm.sessionBusy);
            configurationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!vm.sessionBusy);
        }
        else if(rendered==QuizViewModel.SessionScreen.QUESTIONS)renderQuestion();
        else if(rendered==QuizViewModel.SessionScreen.RESULTS)renderResult();
    }
    private void showConfigurationDialog(){
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(20),dp(16),dp(20),dp(8));
        configurationError=text("",13,RED,false);configurationError.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);configurationError.setVisibility(View.GONE);body.addView(configurationError);
        configurationRetry=button("Thử lại");configurationRetry.setVisibility(View.GONE);configurationRetry.setOnClickListener(v->vm.retrySessionV2());body.addView(configurationRetry);
        body.addView(section,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);scroll.setFillViewport(false);
        configurationDialog=new AlertDialog.Builder(activity).setView(scroll).setNegativeButton("Hủy",null).create();
        configurationDialog.setCancelable(false);configurationDialog.setCanceledOnTouchOutside(false);
        configurationDialog.setOnKeyListener((d,key,event)->{if(key!=android.view.KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==android.view.KeyEvent.ACTION_UP)cancelConfiguration();return true;});
        configurationDialog.show();
        if(android.os.Build.VERSION.SDK_INT>=33)configurationDialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::cancelConfiguration);
        configurationDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v->cancelConfiguration());
        if(configurationDialog.getWindow()!=null)configurationDialog.getWindow().setLayout(Math.min(dp(360),activity.getResources().getDisplayMetrics().widthPixels-dp(32)),ViewGroup.LayoutParams.WRAP_CONTENT);
    }
    private void cancelConfiguration(){
        if(vm.sessionBusy)return;
        vm.backSessionV2();
        if(vm.sessionScreen==QuizViewModel.SessionScreen.CONFIGURATION)Toast.makeText(activity,"Yêu cầu đang chờ xác nhận. Hãy thử đồng bộ lại.",Toast.LENGTH_SHORT).show();
    }
    private void dismissConfigurationDialog(){
        if(configurationDialog!=null){AlertDialog old=configurationDialog;configurationDialog=null;old.dismiss();}
        configurationError=null;configurationRetry=null;
    }
    public static String title(String method){for(int i=0;i<METHODS.length;i++)if(METHODS[i].equals(method))return TITLES[i];return "Trắc nghiệm";}
    private void renderMethods(){
        TextView heading=text("Chọn phương thức luyện tập",15,NAVY,true);heading.setPadding(0,dp(8),0,dp(16));content.addView(heading);
        for(int i=0;i<METHODS.length;i++){
            final String method=METHODS[i];MaterialCardView card=(MaterialCardView)activity.getLayoutInflater().inflate(R.layout.item_quiz_method,content,false);
            card.setTag("method:"+method);card.findViewById(R.id.quizMethodBackground).setBackgroundResource(BACKGROUNDS[i]);
            ((ImageView)card.findViewById(R.id.quizMethodIcon)).setImageResource(ICONS[i]);
            ((TextView)card.findViewById(R.id.quizMethodTitle)).setText(TITLES[i]);((TextView)card.findViewById(R.id.quizMethodDescription)).setText(DESCRIPTIONS[i]);
            card.setContentDescription(TITLES[i]+". "+DESCRIPTIONS[i]);card.setOnClickListener(v->vm.chooseSessionMethod(method));content.addView(card);
        }
        TextView tip=text("Bạn có thể chọn số lượng câu hoặc cặp ở bước tiếp theo.",12,SECONDARY,false);tip.setPadding(0,dp(12),0,dp(12));content.addView(tip);
    }
    private void bindConfiguration(){
        EditText search=find(R.id.quizV2Search);search.setText(vm.sessionSearch);search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){vm.sessionSearch=s.toString();renderTopics();}public void afterTextChanged(Editable s){}});
        ChipGroup groups=find(R.id.quizV2TopicGroups);
        String[] keys={"ALL","SEMANTIC","POS"},labels={"Tất cả","Chủ đề","Loại từ"};
        for(int i=0;i<keys.length;i++){String key=keys[i];Chip chip=chip(labels[i]);chip.setChecked(key.equals(vm.sessionGroup));chip.setTag("group:"+key);chip.setOnClickListener(v->{vm.sessionGroup=key;renderTopics();});groups.addView(chip);}
        find(R.id.quizV2Start).setOnClickListener(v->vm.startSessionV2());
    }
    private Chip chip(String label){Chip c=new Chip(activity);c.setText(label);c.setCheckable(true);c.setEnsureMinTouchTargetSize(true);c.setMinHeight(dp(48));c.setTextSize(12);c.setTextColor(NAVY);return c;}
    private void renderTopics(){
        LinearLayout list=find(R.id.quizV2TopicList);if(list==null)return;list.removeAllViews();
        if(vm.sessionSearch.trim().isEmpty())addTopic(list,null,"Tất cả chủ đề",-1);
        int shown=0;String query=vm.sessionSearch.toLowerCase(java.util.Locale.ROOT).trim();
        for(Topic topic:vm.sessionTopics){
            boolean pos=topic.isPartOfSpeechGroup()||topic.getCode().startsWith("POS_");
            if(("POS".equals(vm.sessionGroup)&&!pos)||("SEMANTIC".equals(vm.sessionGroup)&&pos))continue;
            if(!(topic.getName()+" "+topic.getCode()).toLowerCase(java.util.Locale.ROOT).contains(query))continue;
            addTopic(list,topic.getCode(),topic.getName(),topic.getFlashcardCount());shown++;
        }
        if(shown==0&&!vm.sessionCatalogLoading)list.addView(text("Không có chủ đề phù hợp. Hãy thử từ khóa khác.",13,SECONDARY,false));
    }
    private void addTopic(LinearLayout list,String code,String name,int count){
        MaterialButton b=button(name+(count<0?"":"\nKho từ vựng: "+count+" từ"));b.setTag("topic:"+(code==null?"ALL":code));b.setEnabled(!vm.sessionBusy);
        if(Objects.equals(code,vm.sessionTopicCode)){b.setStrokeColor(ColorStateList.valueOf(COBALT));b.setBackgroundTintList(ColorStateList.valueOf(activity.getColor(R.color.brand_teal_soft)));b.setSelected(true);}
        b.setOnClickListener(v->vm.chooseSessionTopic(code,name));list.addView(b);
    }
    private void renderConfiguration(){
        ((TextView)find(R.id.quizV2MethodBanner)).setText(title(vm.sessionMethod));Availability a=vm.sessionAvailability;
        boolean pair="MATCHING".equals(vm.sessionMethod);String unit=pair?"cặp":"câu";
        ((TextView)find(R.id.quizV2CountHeading)).setText("Bạn muốn làm bao nhiêu "+unit+"?");
        ((TextView)find(R.id.quizV2Capacity)).setText(a==null?(vm.sessionAvailabilityLoading?"Đang kiểm tra số lượng khả dụng…":"Chưa tải được số lượng khả dụng."):a.quizEligibleCount+" "+unit+" khả dụng");
        LinearLayout counts=find(R.id.quizV2Counts);counts.removeAllViews();
        MaterialButton selector=button((a==null?"Đang tải…":vm.sessionCount+" "+unit)+"  ▾");
        selector.setTag("quiz-count-selector");selector.setMinHeight(dp(48));selector.setMinimumHeight(dp(48));selector.setMinWidth(0);selector.setMinimumWidth(0);selector.setCornerRadius(dp(12));selector.setInsetTop(0);selector.setInsetBottom(0);
        selector.setTextColor(NAVY);selector.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));selector.setStrokeColor(ColorStateList.valueOf(BORDER));selector.setStrokeWidth(dp(1));
        selector.setEnabled(a!=null&&a.quizEligibleCount>0&&!vm.sessionBusy);
        selector.setContentDescription("Số lượng "+unit+": "+(a==null?"đang tải":vm.sessionCount)+". Nhấn để chọn.");
        selector.setOnClickListener(v->{
            PopupMenu menu=new PopupMenu(activity,selector);
            if(a!=null&&a.quizEligibleCount>0&&a.quizEligibleCount<20)menu.getMenu().add(0,a.quizEligibleCount,0,a.quizEligibleCount+" "+unit+" (tất cả)");
            for(int n:new int[]{20,40,60,80,100})menu.getMenu().add(0,n,n,n+" "+unit).setEnabled(a!=null&&a.quizEligibleCount>=n);
            menu.setOnMenuItemClickListener(item->{vm.sessionCount=item.getItemId();renderConfiguration();return true;});menu.show();
        });
        counts.addView(selector,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView fallback=find(R.id.quizV2Fallback);fallback.setVisibility(a!=null&&a.quizEligibleCount<20?View.VISIBLE:View.GONE);
        fallback.setText(a==null?"":a.quizEligibleCount==0?"Chưa có dữ liệu đủ điều kiện trong phạm vi hiện tại.":"Sử dụng tất cả "+a.quizEligibleCount+" "+unit+" khả dụng.");
        find(R.id.quizV2Start).setEnabled(a!=null&&vm.sessionCount>0&&!vm.sessionBusy);
    }
    private void renderQuestion(){
        QuizSessionState state=vm.sessionState;Session s=state.session;Question q=state.current();if(s==null||q==null)return;
        ((TextView)find(R.id.quizV2ProgressText)).setText(("MATCHING".equals(s.method)?"Cặp ":"Câu ")+(q.ordinal+1)+" / "+s.selectedUnitCount+" · Đã trả lời "+s.answeredCount);
        ProgressBar progress=find(R.id.quizV2Progress);progress.setMax(s.selectedUnitCount);progress.setProgress(s.answeredCount);
        ((TextView)find(R.id.quizV2Kind)).setText(title(s.method)+("CHALLENGE".equals(s.method)?" · Chuỗi đúng "+state.challengeStreak():" · "+kind(q.kind)));
        boolean matching="MATCHING_PAIR".equals(q.kind);
        ((TextView)find(R.id.quizV2Prompt)).setText(matching?"Nối từ với nghĩa":q.prompt);
        ((TextView)find(R.id.quizV2Instruction)).setText(matching?"Chọn một từ tiếng Anh ở cột trái, rồi chọn nghĩa ở cột phải.":"CONTEXT".equals(q.kind)?"Chọn từ phù hợp để hoàn thành câu.":"Chọn một đáp án đúng.");
        find(R.id.quizV2Grid).setVisibility(matching?View.GONE:View.VISIBLE);find(R.id.quizV2Matching).setVisibility(matching?View.VISIBLE:View.GONE);
        Receipt receipt=state.receipt(q.questionId);
        if(matching)renderMatching(q);else{
            int[] ids={R.id.quizV2AnswerA,R.id.quizV2AnswerB,R.id.quizV2AnswerC,R.id.quizV2AnswerD};
            for(int i=0;i<4;i++){MaterialButton b=find(ids[i]);if(i>=q.options.size()){b.setVisibility(View.GONE);continue;}Option option=q.options.get(i);b.setText((char)('A'+i)+"\n"+option.text);b.setTag(option.optionId);b.setEnabled(!vm.sessionBusy&&state.canSelect(q));styleAnswer(b,receipt,option.optionId,state.pending!=null&&option.optionId.equals(state.pending.selectedOptionId));b.setOnClickListener(v->vm.answerSessionV2(q.questionId,option.optionId));}
        }
        Receipt feedbackReceipt=receipt;
        if(matching&&feedbackReceipt==null&&state.lastReceipt!=null&&!state.lastReceipt.correct){for(Question prior:state.questions)if(prior.questionId.equals(state.lastReceipt.questionId)&&Objects.equals(prior.roundIndex,q.roundIndex))feedbackReceipt=state.lastReceipt;}
        TextView feedback=find(R.id.quizV2Feedback);feedback.setVisibility(feedbackReceipt==null?View.GONE:View.VISIBLE);
        if(feedbackReceipt!=null){Receipt shown=feedbackReceipt;feedback.setText((shown.correct?"Chính xác!":"Chưa đúng. Đáp án đúng: "+optionText(shown.correctAnswer))+"\nXP đã ghi nhận: +"+shown.awardedXp+(shown.source!=null&&shown.source.example!=null?"\nVí dụ nguồn: "+shown.source.example:"")+(shown.source!=null&&shown.source.exampleVi!=null?"\n"+shown.source.exampleVi:""));feedback.setTextColor(shown.correct?GREEN:RED);feedback.setBackground(bg(shown.correct?Color.rgb(227,245,235):Color.rgb(252,231,224),BORDER));}
        boolean roundDone=true;if(matching&&state.active())for(Question item:state.questions)if(Objects.equals(item.roundIndex,q.roundIndex)&&state.receipt(item.questionId)==null)roundDone=false;
        find(R.id.quizV2Next).setEnabled(!vm.sessionBusy&&state.pending==null&&(matching?roundDone:receipt!=null));
        ((MaterialButton)find(R.id.quizV2Next)).setText(!state.active()&&!state.hasLaterReviewUnit()?"Xem kết quả":"Tiếp theo →");
        find(R.id.quizV2Next).setOnClickListener(v->vm.nextSessionV2());find(R.id.quizV2Previous).setEnabled(!vm.sessionBusy&&state.pending==null&&state.cursor>0);find(R.id.quizV2Previous).setOnClickListener(v->vm.previousSessionV2());
        find(R.id.quizV2PartialResult).setEnabled(!vm.sessionBusy&&state.pending==null);find(R.id.quizV2PartialResult).setOnClickListener(v->vm.showSessionResults());
    }
    private String kind(String value){return "CONTEXT".equals(value)?"Ngữ cảnh":"EN_TO_VI".equals(value)?"Từ → Nghĩa":"VI_TO_EN".equals(value)?"Nghĩa → Từ":"Nối cặp";}
    private void styleAnswer(MaterialButton b,Receipt r,String id,boolean pending){
        int color=NAVY,fill=Color.WHITE,stroke=BORDER;
        if(pending){fill=activity.getColor(R.color.brand_teal_soft);stroke=COBALT;}
        if(r!=null&&r.correctAnswer!=null&&id.equals(r.correctAnswer.optionId)){color=GREEN;fill=Color.rgb(227,245,235);stroke=GREEN;}
        else if(r!=null&&!r.correct&&r.selectedAnswer!=null&&id.equals(r.selectedAnswer.optionId)){color=RED;fill=Color.rgb(252,231,224);stroke=RED;}
        b.setTextColor(color);b.setBackgroundTintList(ColorStateList.valueOf(fill));b.setStrokeColor(ColorStateList.valueOf(stroke));b.setStrokeWidth(dp(1));
    }
    private void renderMatching(Question q){
        LinearLayout container=find(R.id.quizV2Matching);container.removeAllViews();LinearLayout left=new LinearLayout(activity),right=new LinearLayout(activity);left.setOrientation(LinearLayout.VERTICAL);right.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMarginEnd(dp(6));left.setLayoutParams(lp);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMarginStart(dp(6));right.setLayoutParams(rp);container.addView(left);container.addView(right);
        Set<String> committedMeanings=new HashSet<>();QuizSessionState state=vm.sessionState;
        for(Question item:state.questions)if(Objects.equals(item.roundIndex,q.roundIndex)){
            Receipt r=state.receipt(item.questionId);if(r!=null&&r.correctAnswer!=null)committedMeanings.add(r.correctAnswer.optionId);
            MaterialButton b=button(item.prompt);b.setTag("left:"+item.questionId);b.setMinimumHeight(dp(58));b.setMinHeight(dp(58));b.setEnabled(!vm.sessionBusy&&state.pending==null&&(state.active()?r==null&&item.roundIndex==state.session.answeredCount/4:r!=null));
            if(item.questionId.equals(q.questionId)){b.setStrokeColor(ColorStateList.valueOf(COBALT));b.setBackgroundTintList(ColorStateList.valueOf(activity.getColor(R.color.brand_teal_soft)));}
            if(r!=null){b.setText((r.correct?"✓ ":"✗ ")+item.prompt);b.setTextColor(r.correct?GREEN:RED);}
            b.setOnClickListener(v->vm.chooseMatchingLeft(item.ordinal));left.addView(b);
        }
        Receipt current=state.receipt(q.questionId);
        for(Option option:q.options){MaterialButton b=button(option.text);b.setTag("right:"+option.optionId);b.setMinHeight(dp(58));b.setMinimumHeight(dp(58));b.setEnabled(!vm.sessionBusy&&state.canSelect(q)&&!committedMeanings.contains(option.optionId));
            styleAnswer(b,current,option.optionId,false);if(committedMeanings.contains(option.optionId)){b.setStrokeColor(ColorStateList.valueOf(GREEN));b.setTextColor(GREEN);}
            b.setOnClickListener(v->vm.answerSessionV2(q.questionId,option.optionId));right.addView(b);}
    }
    private String optionText(Option option){return option==null?"":option.text;}
    static final class ResultWording {
        static String collapsedReviewLabel(String method,int incorrectCount){
            String unit="MATCHING".equals(method)?"cặp":"câu";
            return "Xem chi tiết bài làm ("+incorrectCount+" "+unit+" sai) ▾";
        }
        static String emptyReviewLabel(String method){return "MATCHING".equals(method) ? "Chưa có cặp sai trong phiên này." : "Chưa có câu trả lời sai trong phiên này.";}
    }
    private void renderResult(){
        QuizSessionState state=vm.sessionState;Session s=state.session;if(s==null)return;
        String reason="INCORRECT".equals(s.finishReason)?"Vượt ải kết thúc ở câu sai đầu tiên.":"USER_ABANDONED".equals(s.finishReason)?"Bài luyện tập đã kết thúc theo lựa chọn của bạn.":"COMPLETED_ALL".equals(s.finishReason)?"Bạn đã hoàn thành toàn bộ bài luyện tập.":"Tiến độ hiện tại · Phiên vẫn được giữ lại.";
        find(R.id.quizV2Trophy).setVisibility("COMPLETED_ALL".equals(s.finishReason)?View.VISIBLE:View.GONE);
        ((TextView)find(R.id.quizV2ResultReason)).setText(reason);((TextView)find(R.id.quizV2ResultAccuracy)).setText(state.accuracy()+"%");
        LinearLayout stats=find(R.id.quizV2ResultStats);stats.removeAllViews();
        String unit="MATCHING".equals(s.method)?"cặp":"câu";
        LinearLayout primary=new LinearLayout(activity);primary.setOrientation(LinearLayout.HORIZONTAL);primary.setBaselineAligned(false);
        metric(primary,s.correctCount+" / "+s.answeredCount,"Trả lời đúng",GREEN);metric(primary,state.accuracy()+"%","Chính xác",COBALT);metric(primary,"+"+s.awardedXp,"XP thực nhận",Color.rgb(180,83,9));stats.addView(primary);
        LinearLayout supplement=find(R.id.quizV2ResultSupplement);supplement.removeAllViews();stat(supplement,"Đã trả lời",s.answeredCount+" / "+s.selectedUnitCount+" "+unit,NAVY);stat(supplement,"Chưa đúng",(s.answeredCount-s.correctCount)+" "+unit,RED);
        long seconds=state.durationSeconds(System.currentTimeMillis());stat(supplement,"Thời gian phiên",String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),NAVY);
        MaterialButton toggle=find(R.id.quizV2ReviewToggle);toggle.setText(reviewExpanded?"Thu gọn chi tiết bài làm ▴":ResultWording.collapsedReviewLabel(s.method,state.incorrect().size()));toggle.setOnClickListener(v->{reviewExpanded=!reviewExpanded;renderResult();});
        LinearLayout review=find(R.id.quizV2ReviewList);review.setVisibility(reviewExpanded?View.VISIBLE:View.GONE);review.removeAllViews();
        if(state.incorrect().isEmpty())review.addView(text(ResultWording.emptyReviewLabel(s.method),13,SECONDARY,false));
        for(Receipt receipt:state.incorrect()){
            Question q=null;for(Question candidate:state.questions)if(candidate.questionId.equals(receipt.questionId))q=candidate;
            TextView row=text((q==null?"Câu đã trả lời":q.prompt)+"\nBạn chọn: "+optionText(receipt.selectedAnswer)+"\nĐáp án đúng: "+optionText(receipt.correctAnswer)+(receipt.source!=null&&receipt.source.example!=null?"\nVí dụ nguồn: "+receipt.source.example:""),14,NAVY,false);android.text.SpannableString colored=new android.text.SpannableString(row.getText());String literal=colored.toString();int chosen=literal.indexOf("Bạn chọn: ")+"Bạn chọn: ".length(),correct=literal.indexOf("Đáp án đúng: ");if(chosen>=0&&correct>chosen)colored.setSpan(new android.text.style.ForegroundColorSpan(RED),chosen,correct,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);int answer=correct+"Đáp án đúng: ".length(),source=literal.indexOf("\nVí dụ nguồn:");if(correct>=0)colored.setSpan(new android.text.style.ForegroundColorSpan(GREEN),answer,source<0?literal.length():source,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);row.setText(colored);row.setPadding(dp(16),dp(16),dp(16),dp(16));row.setBackground(bg(Color.WHITE,BORDER));review.addView(row);
        }
        boolean complete="COMPLETED_ALL".equals(s.finishReason);boolean confirmed=false;for(Receipt r:s.receipts)if("COMPLETE".equals(r.operation))confirmed=true;
        find(R.id.quizV2Complete).setVisibility(complete?View.VISIBLE:View.GONE);find(R.id.quizV2Complete).setEnabled(!vm.sessionBusy&&!confirmed);find(R.id.quizV2Complete).setOnClickListener(v->vm.controlSessionV2(false));
        find(R.id.quizV2ReturnToSession).setEnabled(!vm.sessionBusy&&(state.active()||s.answeredCount>0));find(R.id.quizV2ReturnToSession).setOnClickListener(v->vm.reviewSessionV2());
        find(R.id.quizV2NewSession).setEnabled(!vm.sessionBusy&&!state.active());find(R.id.quizV2NewSession).setOnClickListener(v->vm.newSessionV2());
        find(R.id.quizV2Abandon).setVisibility(state.active()?View.VISIBLE:View.GONE);find(R.id.quizV2Abandon).setEnabled(!vm.sessionBusy&&state.pending==null);find(R.id.quizV2Abandon).setOnClickListener(v->confirmAbandon());
    }
    private void stat(LinearLayout parent,String label,String value,int color){
        LinearLayout row=new LinearLayout(activity);row.setPadding(dp(16),dp(14),dp(16),dp(14));row.setBackground(bg(Color.WHITE,BORDER));row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView l=text(label,13,SECONDARY,false),v=text(value,16,color,true);l.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));v.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));v.setGravity(android.view.Gravity.END);row.addView(l);row.addView(v);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);row.setLayoutParams(lp);parent.addView(row);
    }
    private void confirmAbandon(){dialog=new AlertDialog.Builder(activity).setTitle("Kết thúc bài đang làm?").setMessage("Các câu đã được máy chủ xác nhận vẫn được giữ lại. Bài này sẽ kết thúc.").setNegativeButton("Tiếp tục học",null).setPositiveButton("Kết thúc bài",(d,w)->vm.controlSessionV2(true)).show();}
    public void back(){
        if(vm.sessionBusy||vm.sessionState.pending!=null||vm.sessionState.pendingControl!=null){Toast.makeText(activity,"Đang chờ xác nhận. Hãy đồng bộ phiên trước.",Toast.LENGTH_SHORT).show();return;}
        if(vm.sessionScreen==QuizViewModel.SessionScreen.METHODS){LearningBottomNavigation.navigate(activity,NavigationRoute.HOME);return;}
        if(vm.sessionScreen==QuizViewModel.SessionScreen.QUESTIONS){vm.showSessionResults();return;}
        vm.backSessionV2();
    }
    private void navigate(Runnable destination){
        if(vm.sessionBusy||vm.sessionState.pending!=null||vm.sessionState.pendingControl!=null){Toast.makeText(activity,"Hãy xác nhận đáp án đang chờ trước khi chuyển tab.",Toast.LENGTH_SHORT).show();return;}
        if(vm.sessionState.active()){dialog=new AlertDialog.Builder(activity).setTitle("Giữ phiên luyện tập?").setMessage("Tiến độ đã xác nhận được giữ lại. Bạn có thể quay lại tab Trắc nghiệm để tiếp tục.").setNegativeButton("Ở lại",null).setPositiveButton("Giữ phiên và chuyển tab",(d,w)->destination.run()).show();}else destination.run();
    }
    public void destroy(){timerHandler.removeCallbacks(timer);dismissConfigurationDialog();if(dialog!=null)dialog.dismiss();}
    private void updateTimer(){TextView view=find(R.id.quizV2Timer);if(view!=null){long seconds=vm.sessionState.durationSeconds(System.currentTimeMillis());view.setText(String.format(java.util.Locale.ROOT,"%02d:%02d",seconds/60,seconds%60));}}
    private void metric(LinearLayout row,String value,String label,int color){
        LinearLayout card=new LinearLayout(activity);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(android.view.Gravity.CENTER);card.setPadding(dp(4),dp(14),dp(4),dp(14));card.setBackground(bg(Color.WHITE,BORDER));
        TextView number=text(value,20,color,true),caption=text(label,11,SECONDARY,false);number.setGravity(android.view.Gravity.CENTER);caption.setGravity(android.view.Gravity.CENTER);card.addView(number);card.addView(caption);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(dp(3),0,dp(3),dp(12));card.setLayoutParams(lp);row.addView(card);
    }
    public void receiveTopic(String code,String name){if(code!=null&&!vm.openSessionTopic(code,name))Toast.makeText(activity,"Phiên đang được giữ lại. Hãy kết thúc phiên trước khi chọn chủ đề mới.",Toast.LENGTH_LONG).show();}
}
