package com.example.bivoca.ui.quiz;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.example.bivoca.data.model.QuizAnswerResponse;
import com.example.bivoca.data.model.QuizQuestion;
import com.example.bivoca.data.repository.QuizRepository;

import java.util.List;

import retrofit2.Call;

/**
 * ViewModel cho TracNghiemActivity.
 * Vòng đời: LOADING_QUESTIONS → QUESTION → SUBMITTING → RESULT → (câu tiếp / FINISHED)
 * Không tự chấm điểm — backend trả kết quả đúng/sai.
 *
 * topicCode được lưu qua SavedStateHandle để tồn tại sau process death.
 */
public class QuizViewModel extends AndroidViewModel {

    /** Trạng thái toàn bộ màn hình quiz */
    public enum QuizState {
        LOADING_QUESTIONS,  // Đang tải câu hỏi
        QUESTION,           // Đang hiển thị câu hỏi, chờ chọn đáp án
        SUBMITTING,         // Đang gửi đáp án lên backend
        RESULT,             // Hiển thị kết quả một câu (đúng/sai)
        FINISHED,           // Hết câu hỏi — hiển thị màn hình tổng kết
        ERROR               // Lỗi tải câu hỏi không thể tiếp tục
    }

    static final String KEY_TOPIC_CODE = "quiz_topic_code";

    private final QuizRepository repository;
    private final SavedStateHandle savedState;

    private final MutableLiveData<QuizState> state = new MutableLiveData<>(QuizState.LOADING_QUESTIONS);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<QuizAnswerResponse> lastAnswer = new MutableLiveData<>();

    private List<QuizQuestion> questions;

    private int correctCount = 0;
    private int answeredCount = 0;

    @Nullable private Call<?> activeCall;
    private int requestVersion = 0;

    /** Constructor gọi bởi SavedStateViewModelFactory (Android tự inject SavedStateHandle). */
    public QuizViewModel(@NonNull Application application, @NonNull SavedStateHandle savedState) {
        super(application);
        this.savedState = savedState;
        this.repository = new QuizRepository(application.getApplicationContext());
    }

    // Getters

    public LiveData<QuizState> getState() { return state; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Integer> getCurrentIndex() { return currentIndex; }
    public LiveData<QuizAnswerResponse> getLastAnswer() { return lastAnswer; }

    @Nullable
    public QuizQuestion getCurrentQuestion() {
        Integer idx = currentIndex.getValue();
        if (questions == null || idx == null || idx < 0 || idx >= questions.size()) return null;
        return questions.get(idx);
    }

    public int getQuestionCount() { return questions == null ? 0 : questions.size(); }
    public int getCorrectCount() { return correctCount; }
    public int getAnsweredCount() { return answeredCount; }

    // Actions

    /**
     * Gọi từ Activity khi khởi động — chỉ tải nếu chưa có câu hỏi.
     * topicCode: null = tất cả chủ đề.
     */
    public void initIfNeeded(@Nullable String topicCode) {
        // Lưu topic nếu mới
        if (topicCode != null) savedState.set(KEY_TOPIC_CODE, topicCode);
        // Chỉ load nếu chưa có dữ liệu (tránh load lại sau rotation)
        QuizState current = state.getValue();
        if (current == QuizState.LOADING_QUESTIONS && (questions == null || questions.isEmpty())) {
            loadQuestions();
        }
    }

    /** Người dùng nhấn Thử lại sau khi gặp lỗi tải câu hỏi. */
    public void retry() {
        loadQuestions();
    }

    private void loadQuestions() {
        cancelActiveCall();
        int version = ++requestVersion;
        state.setValue(QuizState.LOADING_QUESTIONS);
        errorMessage.setValue(null);
        String topicCode = savedState.get(KEY_TOPIC_CODE);

        activeCall = repository.loadQuestions(topicCode, 10,
                new QuizRepository.QuestionsCallback() {
                    @Override
                    public void onSuccess(List<QuizQuestion> result) {
                        if (version != requestVersion) return;
                        questions = result;
                        correctCount = 0;
                        answeredCount = 0;
                        currentIndex.setValue(0);
                        state.setValue(QuizState.QUESTION);
                    }

                    @Override
                    public void onError(String message) {
                        if (version != requestVersion) return;
                        errorMessage.setValue(message);
                        state.setValue(QuizState.ERROR);
                    }
                });
    }

    /**
     * Người dùng chọn một đáp án.
     * @param selectedFlashcardId ID flashcard ứng với lựa chọn
     */
    public void submitAnswer(int selectedFlashcardId) {
        QuizQuestion current = getCurrentQuestion();
        if (current == null) return;
        if (state.getValue() != QuizState.QUESTION) return;

        cancelActiveCall();
        int version = ++requestVersion;
        errorMessage.setValue(null);
        state.setValue(QuizState.SUBMITTING);

        activeCall = repository.submitAnswer(
                current.getFlashcardId(),
                selectedFlashcardId,
                new QuizRepository.AnswerCallback() {
                    @Override
                    public void onSuccess(QuizAnswerResponse response) {
                        if (version != requestVersion) return;
                        answeredCount++;
                        if (response.isCorrect()) correctCount++;
                        lastAnswer.setValue(response);
                        state.setValue(QuizState.RESULT);
                    }

                    @Override
                    public void onError(String message) {
                        if (version != requestVersion) return;
                        // Thất bại → quay về QUESTION để người dùng thử lại
                        errorMessage.setValue(message);
                        state.setValue(QuizState.QUESTION);
                    }
                });
    }

    /** Người dùng nhấn Tiếp tục sau khi xem kết quả một câu. */
    public void nextQuestion() {
        if (state.getValue() != QuizState.RESULT) return;
        Integer idx = currentIndex.getValue();
        if (idx == null) return;
        int next = idx + 1;
        if (questions == null || next >= questions.size()) {
            state.setValue(QuizState.FINISHED);
        } else {
            currentIndex.setValue(next);
            lastAnswer.setValue(null);
            state.setValue(QuizState.QUESTION);
        }
    }

    /** Người dùng muốn chơi lại. */
    public void playAgain() {
        loadQuestions();
    }

    private void cancelActiveCall() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }
    }

    @Override
    protected void onCleared() {
        requestVersion++;
        sessionEpoch++;availabilityEpoch++;
        if(sessionCall!=null)sessionCall.cancel();if(availabilityCall!=null)availabilityCall.cancel();if(catalogCall!=null)catalogCall.cancel();
        cancelActiveCall();
    }

    // Session v2 is opt-in only for the isolated E2E application. Legacy flow above remains intact.
    public enum SessionScreen { METHODS, CONFIGURATION, QUESTIONS, RESULTS }
    private final MutableLiveData<Integer> sessionRender = new MutableLiveData<>(0);
    public final QuizSessionState sessionState = new QuizSessionState();
    public SessionScreen sessionScreen = SessionScreen.METHODS;
    public String sessionMethod="MIXED", sessionTopicCode, sessionTopicName="Tất cả chủ đề",sessionSearch="",sessionGroup="ALL";
    public String sessionError, sessionErrorCode;
    public int sessionCount=20;
    public boolean sessionBusy, sessionAvailabilityLoading,sessionCatalogLoading;
    public com.example.bivoca.data.model.QuizSessionDtos.Availability sessionAvailability;
    public java.util.List<com.example.bivoca.data.model.Topic> sessionTopics=new java.util.ArrayList<>();
    private final com.google.gson.Gson sessionGson=new com.google.gson.Gson();
    private android.content.SharedPreferences sessionPrefs;
    private String sessionOwnerKey, sessionId;
    private String entrySessionTopic;
    private com.example.bivoca.data.model.QuizSessionDtos.Create sessionCreation;
    private boolean sessionAbandon;
    private int sessionEpoch, availabilityEpoch;
    private Call<?> sessionCall, availabilityCall, catalogCall;
    public LiveData<Integer> getSessionRender(){return sessionRender;}
    private void emitSession(){Integer value=sessionRender.getValue();sessionRender.setValue(value==null?1:value+1);}
    public void initSessionV2(String topicCode) {
        if(!com.example.bivoca.BuildConfig.QUIZ_SESSION_V2_ENABLED)throw new IllegalStateException("Session v2 requires isolated E2E build");
        if(sessionPrefs!=null)return;
        entrySessionTopic=topicCode;
        int user=new com.example.bivoca.utils.SessionManager(getApplication()).getUserId();
        sessionOwnerKey="user_"+user;
        sessionPrefs=getApplication().getSharedPreferences("quiz_session_v2",android.content.Context.MODE_PRIVATE);
        String saved=sessionPrefs.getString(sessionOwnerKey,null);
        if(saved!=null)try{
            com.google.gson.JsonObject o=com.google.gson.JsonParser.parseString(saved).getAsJsonObject();
            sessionId=string(o,"sessionId");sessionMethod=string(o,"method")==null?"MIXED":string(o,"method");
            sessionState.cursor=o.has("cursor")?o.get("cursor").getAsInt():0;
            sessionState.pending=sessionGson.fromJson(o.get("pending"),com.example.bivoca.data.model.QuizSessionDtos.Answer.class);
            sessionCreation=sessionGson.fromJson(o.get("creation"),com.example.bivoca.data.model.QuizSessionDtos.Create.class);
            sessionState.pendingControl=sessionGson.fromJson(o.get("control"),com.example.bivoca.data.model.QuizSessionDtos.Control.class);
            sessionAbandon=o.has("abandon")&&o.get("abandon").getAsBoolean();
        }catch(RuntimeException ignored){sessionError="Không đọc được trạng thái phiên cục bộ. Không tự gửi đáp án mới.";}
        if(sessionId!=null){loadSessionV2();return;}
        if(sessionCreation!=null){sessionMethod=sessionCreation.method;sessionScreen=SessionScreen.CONFIGURATION;retrySessionV2();return;}
        if(topicCode!=null){sessionTopicCode=topicCode;sessionTopicName=topicCode;}
        entrySessionTopic=null;
        emitSession();
    }
    private static String string(com.google.gson.JsonObject o,String key){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():null;}
    private boolean saveSessionV2(){
        if(sessionPrefs==null)return false;
        com.google.gson.JsonObject o=new com.google.gson.JsonObject();o.addProperty("sessionId",sessionId);o.addProperty("method",sessionMethod);o.addProperty("cursor",sessionState.cursor);
        o.add("pending",sessionGson.toJsonTree(sessionState.pending));o.add("creation",sessionGson.toJsonTree(sessionCreation));o.add("control",sessionGson.toJsonTree(sessionState.pendingControl));o.addProperty("abandon",sessionAbandon);
        boolean ok=sessionPrefs.edit().putString(sessionOwnerKey,o.toString()).commit();
        if(!ok){sessionError="Không lưu được mã yêu cầu cục bộ; chưa gửi lên máy chủ.";sessionErrorCode="LOCAL_STATE";emitSession();}return ok;
    }
    public void chooseSessionMethod(String method){
        if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null||sessionCreation!=null)return;
        sessionMethod=method;sessionScreen=SessionScreen.CONFIGURATION;sessionCount=20;sessionError=null;sessionAvailability=null;
        loadSessionTopics();refreshSessionAvailability();emitSession();
    }
    public void loadSessionTopics(){
        if(catalogCall!=null)catalogCall.cancel();sessionCatalogLoading=true;
        catalogCall=new com.example.bivoca.data.repository.TopicRepository((android.content.Context)getApplication()).getTopics(new com.example.bivoca.data.repository.TopicRepository.TopicsCallback(){
            @Override public void onSuccess(java.util.List<com.example.bivoca.data.model.Topic> list){sessionCatalogLoading=false;sessionTopics=list;emitSession();}
            @Override public void onError(String message){sessionCatalogLoading=false;sessionError=message;sessionErrorCode="CATALOG";emitSession();}
        });
    }
    public void chooseSessionTopic(String code,String name){if(sessionBusy||sessionCreation!=null)return;sessionTopicCode=code;sessionTopicName=name;sessionCount=20;refreshSessionAvailability();}
    public void refreshSessionAvailability(){
        if(sessionBusy||sessionCreation!=null)return;
        if(availabilityCall!=null)availabilityCall.cancel();final int epoch=++availabilityEpoch;
        sessionAvailabilityLoading=true;sessionAvailability=null;sessionError=null;
        boolean pos=sessionTopicCode!=null&&sessionTopicCode.startsWith("POS_");
        availabilityCall=repository.availability(sessionMethod,pos?null:sessionTopicCode,pos?sessionTopicCode:null,new QuizRepository.V2Callback<com.example.bivoca.data.model.QuizSessionDtos.Availability>(){
            @Override public void onSuccess(com.example.bivoca.data.model.QuizSessionDtos.Availability value){if(epoch!=availabilityEpoch)return;sessionAvailabilityLoading=false;sessionAvailability=value;sessionCount=QuizSessionState.defaultCount(value);emitSession();}
            @Override public void onError(String code,String message){if(epoch!=availabilityEpoch)return;sessionAvailabilityLoading=false;sessionError=message;sessionErrorCode=code;emitSession();}
        });emitSession();
    }
    public void startSessionV2(){
        if(sessionBusy||sessionCreation!=null||sessionAvailability==null||sessionCount<1)return;
        com.example.bivoca.data.model.QuizSessionDtos.Create input=new com.example.bivoca.data.model.QuizSessionDtos.Create();
        input.creationId=java.util.UUID.randomUUID().toString();input.method=sessionMethod;input.selectedUnitCount=sessionCount;
        input.availabilityVersion=sessionAvailability.availabilityVersion;input.useAllAvailable=sessionCount<20;
        if(sessionTopicCode!=null){if(sessionTopicCode.startsWith("POS_"))input.filter.posCode=sessionTopicCode;else input.filter.topicCode=sessionTopicCode;}
        sessionCreation=input;if(saveSessionV2())sendCreationV2();
    }
    private void sendCreationV2(){
        if(sessionBusy||sessionCreation==null)return;QuizSessionState.dispatchDurably(this::saveSessionV2,()->{sessionBusy=true;sessionError=null;final int epoch=++sessionEpoch;
        sessionCall=repository.createSession(sessionCreation,new QuizRepository.V2Callback<com.example.bivoca.data.model.QuizSessionDtos.Creation>(){
            @Override public void onSuccess(com.example.bivoca.data.model.QuizSessionDtos.Creation value){if(epoch!=sessionEpoch)return;sessionBusy=false;sessionId=value.sessionId;sessionCreation=null;sessionState.cursor=0;saveSessionV2();loadSessionV2();}
            @Override public void onError(String code,String message){if(epoch!=sessionEpoch)return;sessionBusy=false;sessionErrorCode=code;sessionError=message;if("AVAILABILITY_CHANGED".equals(code)||"INSUFFICIENT_ELIGIBLE".equals(code)){sessionCreation=null;sessionAvailability=null;saveSessionV2();}emitSession();}
        });emitSession();
        });
    }
    public void loadSessionV2(){
        if(sessionId==null||sessionBusy)return;sessionBusy=true;sessionError=null;final int epoch=++sessionEpoch;
        sessionCall=repository.resumeSession(sessionId,new QuizRepository.V2Callback<com.example.bivoca.data.model.QuizSessionDtos.Session>(){
            @Override public void onSuccess(com.example.bivoca.data.model.QuizSessionDtos.Session value){if(epoch!=sessionEpoch)return;
                sessionCall=repository.sessionQuestions(sessionId,new QuizRepository.V2Callback<java.util.List<com.example.bivoca.data.model.QuizSessionDtos.Question>>(){
                    @Override public void onSuccess(java.util.List<com.example.bivoca.data.model.QuizSessionDtos.Question> questions){if(epoch!=sessionEpoch)return;sessionState.acceptSnapshot(value,questions);sessionMethod=value.method;sessionBusy=false;
                        sessionScreen="ACTIVE".equals(value.state)?SessionScreen.QUESTIONS:SessionScreen.RESULTS;
                        if(sessionState.pendingControl!=null){for(com.example.bivoca.data.model.QuizSessionDtos.Receipt r:value.receipts)if(sessionState.pendingControl.actionId.equals(r.actionId)){sessionState.pendingControl=null;break;}}
                        sessionErrorCode=null;
                        if(entrySessionTopic!=null){String requested=entrySessionTopic;entrySessionTopic=null;
                            if(!sessionState.active()&&sessionState.pending==null&&sessionState.pendingControl==null){openSessionTopic(requested,requested);return;}
                        }
                        if(sessionState.pendingControl!=null&&(!sessionState.active()||sessionState.pendingControl.expectedVersion!=value.version)){
                            sessionState.pendingControl=null;sessionError="Phiên đã thay đổi; yêu cầu cũ chưa được ghi nhận. Bạn có thể chọn lại thao tác kết thúc.";sessionErrorCode="RESELECT";
                        }
                        if(sessionState.pendingNeedsReselect){sessionError="Phiên đã cập nhật từ một yêu cầu khác. Đáp án cũ chưa được ghi nhận; vui lòng chọn lại để tạo yêu cầu mới.";sessionErrorCode="RESELECT";}
                        saveSessionV2();emitSession();}
                    @Override public void onError(String code,String message){sessionFailure(epoch,code,message);}
                });
            }
            @Override public void onError(String code,String message){sessionFailure(epoch,code,message);}
        });emitSession();
    }
    private void sessionFailure(int epoch,String code,String message){if(epoch!=sessionEpoch)return;sessionBusy=false;sessionErrorCode=code;sessionError=message;emitSession();}
    public void answerSessionV2(String question,String option){
        if(sessionBusy)return;com.example.bivoca.data.model.QuizSessionDtos.Answer input=sessionState.select(question,option);if(input==null)return;
        if(saveSessionV2())sendAnswerV2();
    }
    private void sendAnswerV2(){
        if(sessionBusy||sessionState.pending==null||sessionId==null)return;QuizSessionState.dispatchDurably(this::saveSessionV2,()->{sessionBusy=true;sessionError=null;final int epoch=++sessionEpoch;
        sessionCall=repository.answerSession(sessionId,sessionState.pending,new QuizRepository.V2Callback<com.example.bivoca.data.model.QuizSessionDtos.Receipt>(){
            @Override public void onSuccess(com.example.bivoca.data.model.QuizSessionDtos.Receipt receipt){if(epoch!=sessionEpoch)return;sessionBusy=false;
                if(!sessionState.ack(receipt)){sessionError="Kết quả không khớp yêu cầu đang chờ. Hãy tải lại phiên.";sessionErrorCode="ACK_MISMATCH";}
                saveSessionV2();
                if("CHALLENGE".equals(sessionMethod)&&"INCORRECT".equals(receipt.finishReason)){loadSessionV2();return;}
                emitSession();}
            @Override public void onError(String code,String message){
                if(epoch!=sessionEpoch)return;
                if(sessionState.rejectDefinitively(code)){
                    sessionBusy=false;sessionScreen=SessionScreen.RESULTS;sessionErrorCode=code;
                    sessionError="Một thẻ không còn đủ điều kiện làm Quiz. Đáp án này chưa được ghi nhận. Hãy kết thúc phiên và tạo bài mới; các kết quả đã xác nhận vẫn được giữ lại.";
                    saveSessionV2();emitSession();return;
                }
                sessionFailure(epoch,code,message);
            }
        });emitSession();
        });
    }
    public void retrySessionV2(){
        if(sessionBusy)return;
        if("SESSION_TERMINAL".equals(sessionErrorCode)||"ACTION_PAYLOAD_MISMATCH".equals(sessionErrorCode)||"SESSION_VERSION_CONFLICT".equals(sessionErrorCode)||"QUESTION_ALREADY_ANSWERED".equals(sessionErrorCode)||"ACK_MISMATCH".equals(sessionErrorCode)){loadSessionV2();return;}
        if(sessionState.pendingControl!=null){sendControlV2();return;}if(sessionState.pending!=null){sendAnswerV2();return;}if(sessionCreation!=null){sendCreationV2();return;}
        if(sessionId!=null){loadSessionV2();return;}if("CATALOG".equals(sessionErrorCode))loadSessionTopics();refreshSessionAvailability();
    }
    public void nextSessionV2(){
        if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null)return;
        if(!sessionState.next()){
            if(sessionState.session!=null&&!sessionState.active()){sessionScreen=SessionScreen.RESULTS;loadSessionV2();}
        }saveSessionV2();emitSession();
    }
    public void previousSessionV2(){if(sessionBusy)return;sessionState.previous();saveSessionV2();emitSession();}
    public void chooseMatchingLeft(int ordinal){
        if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null)return;
        if(ordinal>=0&&ordinal<sessionState.questions.size()){sessionState.cursor=ordinal;saveSessionV2();emitSession();}
    }
    public void controlSessionV2(boolean abandon){
        if(sessionBusy||sessionState.pending!=null||sessionState.session==null)return;
        if(sessionState.pendingControl==null){sessionState.pendingControl=new com.example.bivoca.data.model.QuizSessionDtos.Control();sessionState.pendingControl.actionId=java.util.UUID.randomUUID().toString();sessionState.pendingControl.expectedVersion=sessionState.session.version;sessionAbandon=abandon;}
        if(saveSessionV2())sendControlV2();
    }
    private void sendControlV2(){
        if(sessionBusy||sessionState.pendingControl==null||sessionId==null)return;QuizSessionState.dispatchDurably(this::saveSessionV2,()->{sessionBusy=true;sessionError=null;final int epoch=++sessionEpoch;
        sessionCall=repository.controlSession(sessionId,sessionAbandon,sessionState.pendingControl,new QuizRepository.V2Callback<com.example.bivoca.data.model.QuizSessionDtos.Receipt>(){
            @Override public void onSuccess(com.example.bivoca.data.model.QuizSessionDtos.Receipt receipt){if(epoch!=sessionEpoch)return;sessionBusy=false;sessionState.pendingControl=null;saveSessionV2();loadSessionV2();}
            @Override public void onError(String code,String message){sessionFailure(epoch,code,message);}
        });emitSession();
        });
    }
    public void newSessionV2(){
        if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null||sessionState.active())return;
        sessionId=null;sessionCreation=null;sessionState.session=null;sessionState.questions.clear();sessionState.cursor=0;
        sessionScreen=SessionScreen.METHODS;sessionError=null;saveSessionV2();emitSession();
    }
    public void backSessionV2(){
        if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null)return;
        if(sessionScreen==SessionScreen.CONFIGURATION&&sessionCreation==null){sessionScreen=SessionScreen.METHODS;emitSession();}
        else if(sessionScreen==SessionScreen.RESULTS){sessionScreen=SessionScreen.QUESTIONS;emitSession();}
        else previousSessionV2();
    }

    public void showSessionResults(){if(sessionBusy||sessionState.pending!=null||sessionState.pendingControl!=null||sessionState.session==null)return;sessionScreen=SessionScreen.RESULTS;emitSession();}
    public boolean openSessionTopic(String code,String name){
        if(code==null)return true;
        if(sessionBusy||sessionState.active()||sessionState.pending!=null||sessionState.pendingControl!=null||sessionCreation!=null)return false;
        if(sessionState.session!=null)newSessionV2();
        sessionTopicCode=code;sessionTopicName=name==null?code:name;sessionScreen=SessionScreen.METHODS;sessionError=null;emitSession();return true;
    }
    public void reviewSessionV2(){if(sessionBusy||!sessionState.review())return;sessionScreen=SessionScreen.QUESTIONS;emitSession();}
}
