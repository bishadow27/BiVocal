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
    public static final String SECTION_DISPLAY = "display";
    public static final String SECTION_ABOUT = "about";

    private SettingsPreferenceManager settings;
    private MaterialSwitch reminderSwitch;
    private TextView reminderStatus;
    private View reminderOptions;
    private boolean binding;
    private boolean pendingReminderEnable;
    private LearningRepository learningRepository;
    private Call<LearningGoal> goalCall;

    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!pendingReminderEnable) return;
                pendingReminderEnable = false;
                if (granted) enableReminder();
                else {
                    binding = true;
                    reminderSwitch.setChecked(false);
                    binding = false;
                    settings.setReminderEnabled(false);
                    ReminderScheduler.cancelForUser(this, settings.getUserId());
                    reminderStatus.setText(R.string.settings_reminder_permission_denied);
                }
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
        else if (SECTION_DISPLAY.equals(section)) bindDisplay();
        else if (SECTION_ABOUT.equals(section)) bindAbout();
        else {
            finish();
            return;
        }
        TextView title = findViewById(R.id.textSettingsTitle);
        NavigationDrawerController.attach(this, NavigationRoute.SETTINGS,
                R.id.buttonSettingsBack, title.getText());
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

    @Override protected void onDestroy(){if(goalCall!=null)goalCall.cancel();super.onDestroy();}

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
        showSection(R.id.groupReminder, "NHẮC HỌC", "Lịch nhắc gần thời điểm đã chọn theo múi giờ thiết bị.");
        reminderSwitch = findViewById(R.id.switchReminder);
        reminderStatus = findViewById(R.id.textReminderStatus);
        reminderOptions = findViewById(R.id.reminderOptions);
        if (settings.isReminderEnabled()
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            settings.setReminderEnabled(false);
            ReminderScheduler.cancelForUser(this, settings.getUserId());
        }
        binding = true;
        reminderSwitch.setChecked(settings.isReminderEnabled());
        binding = false;
        renderReminderStatus();
        reminderSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (binding) return;
            if (!checked) {
                settings.setReminderEnabled(false);
                ReminderScheduler.cancelForUser(this, settings.getUserId());
                renderReminderStatus();
                return;
            }
            if (settings.getReminderDays() == 0) {
                settings.setReminderDays(SettingsPreferenceManager.ALL_DAYS);
                bindDayChecks();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingReminderEnable = true;
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else enableReminder();
        });
        TextView time = findViewById(R.id.buttonReminderTime);
        time.setText(settings.reminderTimeLabel());
        time.setOnClickListener(view -> showReminderTimePicker(time));
        bindDayChecks();
    }

    private void showReminderTimePicker(TextView time) {
        TimePickerDialog dialog = new TimePickerDialog(this, R.style.Theme_BiVoca_TimePicker,
                (picker, hour, minute) -> {
                    settings.setReminderTime(hour, minute);
                    time.setText(settings.reminderTimeLabel());
                    if (settings.isReminderEnabled()) ReminderScheduler.reconcile(this);
                    renderReminderStatus();
                }, settings.getReminderHour(), settings.getReminderMinute(), true);
        dialog.setOnShowListener(ignored -> {
            int cyan = ContextCompat.getColor(this, R.color.bivocal_cyan);
            dialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setTextColor(cyan);
            dialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setTextColor(cyan);
        });
        dialog.show();
    }

    private void enableReminder() {
        settings.setReminderEnabled(true);
        ReminderScheduler.reconcile(this);
        binding = true;
        reminderSwitch.setChecked(true);
        binding = false;
        renderReminderStatus();
    }

    private void bindDayChecks() {
        int[] ids = {R.id.daySunday, R.id.dayMonday, R.id.dayTuesday, R.id.dayWednesday,
                R.id.dayThursday, R.id.dayFriday, R.id.daySaturday};
        for (int index = 0; index < ids.length; index++) {
            CheckBox box = findViewById(ids[index]);
            int calendarDay = index + 1;
            binding = true;
            box.setChecked(SettingsPreferenceManager.isDaySelected(settings.getReminderDays(), calendarDay));
            binding = false;
            box.setOnCheckedChangeListener((button, checked) -> {
                if (binding) return;
                int updated = SettingsPreferenceManager.setDaySelected(settings.getReminderDays(), calendarDay, checked);
                if (updated == 0 && settings.isReminderEnabled()) {
                    binding = true;
                    button.setChecked(true);
                    binding = false;
                    Toast.makeText(this, "Lịch đang bật cần ít nhất một ngày.", Toast.LENGTH_SHORT).show();
                    return;
                }
                settings.setReminderDays(updated);
                if (settings.isReminderEnabled()) ReminderScheduler.reconcile(this);
                renderReminderStatus();
            });
        }
    }

    private void renderReminderStatus() {
        boolean enabled = settings.isReminderEnabled();
        if (reminderOptions != null) reminderOptions.setAlpha(enabled ? 1f : 0.55f);
        if (reminderStatus != null) reminderStatus.setText(enabled
                ? "Đang bật · " + settings.reminderTimeLabel() + " · " + Integer.bitCount(settings.getReminderDays()) + " ngày/tuần"
                : "Đang tắt. Không có lịch nhắc nào của tài khoản này.");
    }

    private void bindDisplay() {
        showSection(R.id.groupDisplay, "GIAO DIỆN", "Giữ phong cách Dark Night Sky và điều chỉnh khả năng đọc.");
        RadioGroup sizes = findViewById(R.id.radioTextSize);
        sizes.check(SettingsPreferenceManager.TEXT_LARGE.equals(settings.getTextSize()) ? R.id.textLarge : R.id.textStandard);
        sizes.setOnCheckedChangeListener((group, checkedId) -> settings.setTextSize(
                checkedId == R.id.textLarge ? SettingsPreferenceManager.TEXT_LARGE : SettingsPreferenceManager.TEXT_STANDARD));
        MaterialSwitch reduce = findViewById(R.id.switchReduceMotion);
        reduce.setChecked(settings.isReduceMotionEnabled());
        reduce.setOnCheckedChangeListener((button, checked) -> settings.setReduceMotionEnabled(checked));
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
