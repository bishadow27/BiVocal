package com.example.bivoca.ui.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.bivoca.R;
import com.example.bivoca.settings.ReminderScheduler;
import com.example.bivoca.settings.SettingsPreferenceManager;
import com.example.bivoca.data.repository.LearningRepository;
import com.example.bivoca.data.model.LearningGoal;
import retrofit2.Call;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.Calendar;

public final class SettingsDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SECTION = "settings_section";
    public static final String SECTION_STUDY = "study";
    public static final String SECTION_AUDIO = "audio";
    public static final String SECTION_REMINDER = "reminder";
    public static final String SECTION_ABOUT = "about";

    private SettingsPreferenceManager settings;
    private MaterialSwitch reminderSwitch;
    private TextView reminderStatus;
    private ReminderViewModel reminderModel;
    private android.widget.TimePicker reminderPicker;
    private NavigationDrawerController drawer;
    private androidx.appcompat.app.AlertDialog leaveDialog;
    private boolean binding;
    private LearningRepository learningRepository;
    private Call<LearningGoal> goalCall;

    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (reminderModel != null) reminderModel.permissionResult(granted,
                        androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_detail);
        settings = new SettingsPreferenceManager(this);
        learningRepository = new LearningRepository(this);
        findViewById(R.id.buttonSettingsBack).setOnClickListener(view -> finish());

        String section = getIntent().getStringExtra(EXTRA_SECTION);
        if (SECTION_STUDY.equals(section)) bindStudy();
        else if (SECTION_AUDIO.equals(section)) bindAudio();
        else if (SECTION_REMINDER.equals(section)) bindReminder();
        else if (SECTION_ABOUT.equals(section)) bindAbout();
        else {
            finish();
            return;
        }
        TextView title = findViewById(R.id.textSettingsTitle);
        drawer = NavigationDrawerController.attach(this, NavigationRoute.SETTINGS,
                R.id.buttonSettingsBack, title.getText(), reminderModel == null ? null : () -> requestLeave(() -> drawer.performDefaultBack()));
        if (reminderModel != null) drawer.setNavigationGuard(this::requestLeave);
    }

    private void showSection(int groupId, String title, String subtitle) {
        findViewById(groupId).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.textSettingsTitle)).setText(title);
        ((TextView) findViewById(R.id.textSettingsSubtitle)).setText(subtitle);
    }

    private void bindStudy() {
        showSection(R.id.groupStudy, "HỌC TẬP", "Mục tiêu cá nhân và cách hiển thị Flashcard.");
        RadioGroup goals = findViewById(R.id.radioDailyGoal);
        setGoalSelection(goals, settings.getDailyGoal());
        ((TextView)findViewById(R.id.textDailyGoalValue)).setText("Đang đồng bộ mục tiêu từ máy chủ…");
        goals.setOnCheckedChangeListener((group, checkedId) -> {
            if (binding) return;
            int value = checkedId == R.id.goal5 ? 5 : checkedId == R.id.goal15 ? 15 : checkedId == R.id.goal20 ? 20 : 10;
            group.setEnabled(false);
            ((TextView)findViewById(R.id.textDailyGoalValue)).setText("Đang lưu mục tiêu…");
            if(goalCall!=null)goalCall.cancel();
            goalCall=learningRepository.updateGoal(value,new LearningRepository.Result<LearningGoal>(){
                @Override public void onSuccess(LearningGoal saved){settings.setDailyGoal(saved.getDailyGoal());group.setEnabled(true);setGoalSelection(group,saved.getDailyGoal());updateGoalLabel();}
                @Override public void onError(String message){group.setEnabled(true);setGoalSelection(group,settings.getDailyGoal());updateGoalLabel();Toast.makeText(SettingsDetailActivity.this,message,Toast.LENGTH_LONG).show();}
            });
        });
        goalCall=learningRepository.getGoal(new LearningRepository.Result<LearningGoal>(){
            @Override public void onSuccess(LearningGoal remote){settings.setDailyGoal(remote.getDailyGoal());setGoalSelection(goals,remote.getDailyGoal());updateGoalLabel();}
            @Override public void onError(String message){updateGoalLabel();Toast.makeText(SettingsDetailActivity.this,message,Toast.LENGTH_LONG).show();}
        });
        RadioGroup fronts = findViewById(R.id.radioFlashcardFront);
        fronts.check(SettingsPreferenceManager.FRONT_VIETNAMESE.equals(settings.getFlashcardFront())
                ? R.id.frontVietnamese : R.id.frontEnglish);
        fronts.setOnCheckedChangeListener((group, checkedId) -> settings.setFlashcardFront(
                checkedId == R.id.frontVietnamese ? SettingsPreferenceManager.FRONT_VIETNAMESE
                        : SettingsPreferenceManager.FRONT_ENGLISH));
    }

    private void updateGoalLabel() {
        ((TextView) findViewById(R.id.textDailyGoalValue)).setText(
                "Mục tiêu đã đồng bộ: " + settings.getDailyGoal() + " từ mới/ngày.");
    }

    private void setGoalSelection(RadioGroup group,int goal){binding=true;group.check(goal==5?R.id.goal5:goal==15?R.id.goal15:goal==20?R.id.goal20:R.id.goal10);binding=false;}

    @Override protected void onDestroy(){if(goalCall!=null)goalCall.cancel();if(leaveDialog!=null)leaveDialog.dismiss();super.onDestroy();}

    private void bindAudio() {
        showSection(R.id.groupAudio, "ÂM THANH", "Điều chỉnh phát âm tiếng Anh trên Flashcard.");
        MaterialSwitch auto = findViewById(R.id.switchAutoPronunciation);
        auto.setChecked(settings.isAutoPronunciationEnabled());
        auto.setOnCheckedChangeListener((button, checked) -> settings.setAutoPronunciationEnabled(checked));
        RadioGroup rates = findViewById(R.id.radioSpeechRate);
        float rate = settings.getSpeechRate();
        rates.check(rate < 0.9f ? R.id.rateSlow : rate > 1.1f ? R.id.rateFast : R.id.rateNormal);
        rates.setOnCheckedChangeListener((group, checkedId) -> settings.setSpeechRate(
                checkedId == R.id.rateSlow ? 0.8f : checkedId == R.id.rateFast ? 1.2f : 1.0f));
    }

    private void bindReminder() {
        showSection(R.id.groupReminder, "NHẮC HỌC", "Nhắc học sẽ được gửi theo múi giờ địa phương bạn đã chọn.");
        reminderModel = new androidx.lifecycle.ViewModelProvider(this).get(ReminderViewModel.class);
        reminderSwitch = findViewById(R.id.switchReminder);
        reminderStatus = findViewById(R.id.textReminderStatus);
        reminderPicker = findViewById(R.id.pickerReminderTime);
        reminderPicker.setIs24HourView(true);
        applyPickerFont(reminderPicker);
        reminderSwitch.setOnCheckedChangeListener((button,checked)->{
            if(!binding)reminderModel.edit(reminderModel.current().draft.withEnabled(checked));
        });
        reminderPicker.setOnTimeChangedListener((picker,h,m)->{
            if(!binding)reminderModel.edit(reminderModel.current().draft.withTime(h,m));
        });
        int[] ids={R.id.dayMonday,R.id.dayTuesday,R.id.dayWednesday,R.id.dayThursday,R.id.dayFriday,R.id.daySaturday,R.id.daySunday};
        int[] days={Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY,Calendar.SUNDAY};
        for(int i=0;i<ids.length;i++) {
            final int day=days[i];
            ((CheckBox)findViewById(ids[i])).setOnCheckedChangeListener((button,checked)->{
                if(!binding)reminderModel.edit(reminderModel.current().draft.withDays(
                        SettingsPreferenceManager.setDaySelected(reminderModel.current().draft.days,day,checked)));
            });
        }
        android.widget.GridLayout grid=findViewById(R.id.gridReminderDays);
        grid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layoutDays(grid));
        findViewById(R.id.buttonSaveReminder).setOnClickListener(v->{reminderPicker.clearFocus();saveReminder();});
        reminderModel.state().observe(this,this::renderReminder);
    }

    private void saveReminder() {
        boolean granted=Build.VERSION.SDK_INT<33 || ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        if(reminderModel.save(granted,androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()))
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void applyPickerFont(View view) {
        if(view instanceof TextView) ((TextView)view).setTypeface(androidx.core.content.res.ResourcesCompat.getFont(this,R.font.be_vietnam_pro_semibold));
        if(view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group=(android.view.ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++)applyPickerFont(group.getChildAt(i));
        }
    }

    private void renderReminder(ReminderViewModel.State state) {
        binding=true;
        boolean editable=!state.saving&&!state.pendingPermission;
        reminderSwitch.setChecked(state.draft.enabled);reminderSwitch.setEnabled(editable);
        if(reminderPicker.getHour()!=state.draft.hour)reminderPicker.setHour(state.draft.hour);
        if(reminderPicker.getMinute()!=state.draft.minute)reminderPicker.setMinute(state.draft.minute);
        setEnabledTree(reminderPicker,editable);
        int[] ids={R.id.daySunday,R.id.dayMonday,R.id.dayTuesday,R.id.dayWednesday,R.id.dayThursday,R.id.dayFriday,R.id.daySaturday};
        for(int i=0;i<ids.length;i++) {
            CheckBox box=findViewById(ids[i]);boolean checked=SettingsPreferenceManager.isDaySelected(state.draft.days,i+1);
            box.setChecked(checked);box.setEnabled(editable);
            box.setCompoundDrawablesRelativeWithIntrinsicBounds(checked?R.drawable.reminder_check:0,0,0,0);
        }
        reminderStatus.setText(state.saved.enabled?"Đang bật • "+String.format(java.util.Locale.getDefault(),"%02d:%02d",state.saved.hour,state.saved.minute)+" • "+Integer.bitCount(state.saved.days)+" ngày/tuần":"Đang tắt • Chưa bật lịch nhắc đã lưu.");
        TextView feedback=findViewById(R.id.textReminderFeedback);
        String message=state.saving?"Đang lưu lịch nhắc…":state.pendingPermission?"Đang chờ quyền thông báo…":state.dirty()?"Thay đổi chưa được lưu.":"";
        boolean error=false;
        switch(state.feedback) {
            case INVALID_DAYS: message="Hãy chọn ít nhất một ngày khi bật nhắc học.";error=true;break;
            case PERMISSION: message="Chưa có quyền thông báo. Lịch đã lưu được giữ nguyên. Bạn có thể bật quyền trong Cài đặt Android rồi lưu lại.";error=true;break;
            case STORAGE: message="Không lưu được lịch nhắc. Cấu hình trước được giữ nguyên; hãy thử lại.";error=true;break;
            case SESSION: message="Phiên tài khoản đã thay đổi. Hãy mở lại Nhắc học.";error=true;break;
            case SCHEDULER: message="Đã lưu cấu hình nhưng chưa cập nhật được lịch hệ thống. Hãy bấm Lưu để thử lại.";error=true;break;
            case SAVED: message="Đã lưu lịch nhắc.";break;
            default: break;
        }
        feedback.setText(message);feedback.setVisibility(message.isEmpty()?View.GONE:View.VISIBLE);
        feedback.setTextColor(ContextCompat.getColor(this,error?R.color.bivocal_light_coral_ink:R.color.bivocal_light_small_text));
        findViewById(R.id.buttonSaveReminder).setEnabled(editable);
        binding=false;
    }

    private void setEnabledTree(View view,boolean enabled) {
        view.setEnabled(enabled);
        if(view instanceof android.view.ViewGroup) {android.view.ViewGroup g=(android.view.ViewGroup)view;for(int i=0;i<g.getChildCount();i++)setEnabledTree(g.getChildAt(i),enabled);}
    }

    private void layoutDays(android.widget.GridLayout grid) {
        int width=grid.getWidth();if(width<=0)return;
        float density=getResources().getDisplayMetrics().density;
        int gap=Math.round(8*density),min=Math.round(72*density*getResources().getConfiguration().fontScale);
        int columns=Math.max(2,Math.min(4,(width+gap)/(min+gap)));
        if(grid.getColumnCount()!=columns) {
            // GridLayout resolves automatic child indices during its first layout.
            // Lowering the count with those old indices still attached is invalid.
            // Reuse the same checkbox Views (state/listeners intact), changing count while empty.
            View focused=grid.findFocus();
            java.util.List<View> children=new java.util.ArrayList<>();
            for(int i=0;i<grid.getChildCount();i++)children.add(grid.getChildAt(i));
            grid.removeAllViews();grid.setColumnCount(columns);
            for(int i=0;i<children.size();i++) {
                View child=children.get(i);android.widget.GridLayout.LayoutParams p=(android.widget.GridLayout.LayoutParams)child.getLayoutParams();
                p.columnSpec=android.widget.GridLayout.spec(i%columns);p.rowSpec=android.widget.GridLayout.spec(i/columns);
                grid.addView(child,p);
            }
            if(focused!=null)focused.requestFocus();
        }
        int cell=(width-gap*(columns-1))/columns;
        for(int i=0;i<grid.getChildCount();i++) {
            View child=grid.getChildAt(i);android.widget.GridLayout.LayoutParams p=(android.widget.GridLayout.LayoutParams)child.getLayoutParams();
            int end=i%columns<columns-1?gap:0;
            if(p.width==cell&&p.rightMargin==end&&p.topMargin==(i>=columns?gap:0))continue;
            p.width=cell;p.height=android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            p.columnSpec=android.widget.GridLayout.spec(i%columns);p.rowSpec=android.widget.GridLayout.spec(i/columns);
            p.setMargins(0,i>=columns?gap:0,end,0);child.setLayoutParams(p);
        }
    }

    private void requestLeave(Runnable destination) {
        ReminderViewModel.State s=reminderModel.current();
        if(s.saving||s.pendingPermission) {Toast.makeText(this,"Vui lòng chờ lưu hoặc cấp quyền xong.",Toast.LENGTH_SHORT).show();return;}
        if(!s.dirty()) {destination.run();return;}
        if(leaveDialog!=null&&leaveDialog.isShowing())return;
        leaveDialog=new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Thay đổi chưa lưu")
                .setMessage("Bạn muốn bỏ thay đổi lịch nhắc và rời màn hình?")
                .setNegativeButton("Ở lại",(dialog,which)->{})
                .setPositiveButton("Bỏ thay đổi",(dialog,which)->{reminderModel.discard();destination.run();}).show();
    }

    private void bindAbout() {
        showSection(R.id.groupAbout, "GIỚI THIỆU", "Thông tin ứng dụng BiVocal.");
        String version = "Không xác định";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            version = info.versionName + " (" + code + ")";
        } catch (PackageManager.NameNotFoundException ignored) { }
        ((TextView) findViewById(R.id.textAppVersion)).setText("Phiên bản " + version);
    }

}
