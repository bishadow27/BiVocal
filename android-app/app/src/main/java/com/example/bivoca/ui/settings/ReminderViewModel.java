package com.example.bivoca.ui.settings;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import com.example.bivoca.settings.ReminderSchedule;
import com.example.bivoca.settings.ReminderScheduler;
import com.example.bivoca.settings.SettingsPreferenceManager;
import com.example.bivoca.utils.SessionManager;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns the draft across recreation. No Activity, credentials, or notification permission grants. */
public final class ReminderViewModel extends AndroidViewModel {
    public enum Feedback { NONE, INVALID_DAYS, PERMISSION, STORAGE, SAVED, SCHEDULER, SESSION }
    public static final class State {
        public final ReminderSchedule saved,draft;
        public final boolean saving,pendingPermission;
        public final Feedback feedback;
        State(ReminderSchedule saved,ReminderSchedule draft,boolean saving,boolean pending,Feedback f) {
            this.saved=saved;this.draft=draft;this.saving=saving;pendingPermission=pending;feedback=f;
        }
        public boolean dirty() { return !saved.equals(draft); }
    }
    private final SettingsPreferenceManager preferences;
    private final SavedStateHandle handle;
    private final MutableLiveData<State> state=new MutableLiveData<>();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private volatile boolean cleared;
    public ReminderViewModel(Application app,SavedStateHandle handle) {
        super(app);this.handle=handle;preferences=new SettingsPreferenceManager(app);
        ReminderSchedule saved=preferences.readReminderSchedule(),draft=saved;
        Integer owner=handle.get("reminder.owner");
        if (owner!=null && owner==preferences.getUserId() && handle.contains("reminder.enabled")) {
            Boolean enabled=handle.get("reminder.enabled");Integer h=handle.get("reminder.hour"),m=handle.get("reminder.minute"),d=handle.get("reminder.days");
            if(enabled!=null&&h!=null&&m!=null&&d!=null) draft=new ReminderSchedule(enabled,h,m,d);
        }
        Boolean pending=handle.get("reminder.permission");
        publish(saved,draft,false,owner!=null&&owner==preferences.getUserId()&&Boolean.TRUE.equals(pending),Feedback.NONE);
    }
    public LiveData<State> state() { return state; }
    public State current() { return state.getValue(); }
    private void publish(ReminderSchedule saved,ReminderSchedule draft,boolean saving,boolean pending,Feedback f) {
        handle.set("reminder.owner",preferences.getUserId());handle.set("reminder.enabled",draft.enabled);
        handle.set("reminder.hour",draft.hour);handle.set("reminder.minute",draft.minute);handle.set("reminder.days",draft.days);
        handle.set("reminder.permission",pending);state.setValue(new State(saved,draft,saving,pending,f));
    }
    public void edit(ReminderSchedule draft) {
        State s=current();if(s.saving||s.pendingPermission)return;
        publish(s.saved,draft,false,false,Feedback.NONE);
    }
    public void discard() { State s=current();if(!s.saving&&!s.pendingPermission)publish(s.saved,s.saved,false,false,Feedback.NONE); }
    /** Returns true only when the Activity should launch the platform permission request. */
    public boolean save(boolean runtimePermission,boolean notificationsEnabled) {
        State s=current();if(s.saving||s.pendingPermission)return false;
        if(!s.draft.isValid()) {publish(s.saved,s.draft,false,false,Feedback.INVALID_DAYS);return false;}
        if(preferences.getUserId()<=0 || new SessionManager(getApplication()).getUserId()!=preferences.getUserId()) {
            publish(s.saved,s.draft,false,false,Feedback.SESSION);return false;
        }
        if(s.draft.enabled&&!runtimePermission) {publish(s.saved,s.draft,false,true,Feedback.NONE);return true;}
        if(s.draft.enabled&&!notificationsEnabled) {publish(s.saved,s.draft,false,false,Feedback.PERMISSION);return false;}
        ReminderSchedule target=s.draft;
        publish(s.saved,target,true,false,Feedback.NONE);
        worker.execute(()->{
            boolean sameOwner=new SessionManager(getApplication()).getUserId()==preferences.getUserId();
            boolean stored=sameOwner&&preferences.saveReminderSchedule(target);
            boolean scheduled=false;
            if(stored && new SessionManager(getApplication()).getUserId()==preferences.getUserId()) {
                try {ReminderScheduler.reconcile(getApplication());scheduled=true;}catch(RuntimeException ignored) { }
            }
            boolean finalScheduled=scheduled;
            main.post(()->{if(!cleared)publish(stored?target:s.saved,target,false,false,
                    !sameOwner?Feedback.SESSION:!stored?Feedback.STORAGE:finalScheduled?Feedback.SAVED:Feedback.SCHEDULER);});
        });
        return false;
    }
    public void permissionResult(boolean granted,boolean notificationsEnabled) {
        State s=current();if(!s.pendingPermission)return;
        publish(s.saved,s.draft,false,false,granted?Feedback.NONE:Feedback.PERMISSION);
        if(granted)save(true,notificationsEnabled);
    }
    @Override protected void onCleared() {cleared=true;worker.shutdown();super.onCleared();}
}
