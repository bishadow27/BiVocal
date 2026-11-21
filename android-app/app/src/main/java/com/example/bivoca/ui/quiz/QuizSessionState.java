package com.example.bivoca.ui.quiz;

import com.example.bivoca.data.model.QuizSessionDtos.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

/** Pure, server-authoritative session state. Never grades or computes XP. */
public final class QuizSessionState {
    public Session session;
    public List<Question> questions = new ArrayList<>();
    public Answer pending;
    public Control pendingControl;
    public Receipt lastReceipt;
    public boolean pendingNeedsReselect;
    public int cursor;
    private final Map<String,Receipt> receipts = new LinkedHashMap<>();
    public static int defaultCount(Availability a) {
        return Math.min(20,Math.max(0,a.quizEligibleCount));
    }
    public void acceptSnapshot(Session s,List<Question> q) {
        if(s==null||s.sessionId==null)throw new IllegalArgumentException("Missing session");
        session=s;questions=new ArrayList<>(q);receipts.clear();lastReceipt=null;pendingNeedsReselect=false;
        if(s.receipts!=null)for(Receipt r:s.receipts)
            if("ANSWER".equals(r.operation)&&s.sessionId.equals(r.sessionId)){receipts.put(r.questionId,r);lastReceipt=r;}
        if(pending!=null){Receipt found=receipts.get(pending.questionId);if(found!=null)pending=null;else if(!active()||s.version!=pending.expectedVersion){pending=null;pendingNeedsReselect=active();}}
        cursor=Math.max(0,Math.min(cursor,Math.max(0,questions.size()-1)));
    }
    public Question current() { return cursor>=0&&cursor<questions.size()?questions.get(cursor):null; }
    public Receipt receipt(String questionId) { return receipts.get(questionId); }
    public boolean active() { return session!=null&&"ACTIVE".equals(session.state); }
    public boolean locked(String questionId) { return pending!=null||pendingControl!=null||receipts.containsKey(questionId)||!active(); }
    public boolean canSelect(Question q) {
        if(q==null||locked(q.questionId))return false;
        return "MATCHING".equals(session.method)?q.roundIndex!=null&&q.roundIndex==session.answeredCount/4:q.ordinal==session.answeredCount;
    }
    public Answer select(String questionId,String optionId) {
        Question q=null;for(Question candidate:questions)if(questionId.equals(candidate.questionId))q=candidate;
        if(!canSelect(q))return null;
        boolean belongs=false;for(Option o:q.options)if(optionId.equals(o.optionId))belongs=true;
        if(!belongs)return null;
        Answer a=new Answer();a.actionId=UUID.randomUUID().toString();a.questionId=questionId;a.selectedOptionId=optionId;a.expectedVersion=session.version;
        pending=a;cursor=q.ordinal;return a;
    }
    public boolean ack(Receipt r) {
        if(session==null||r==null||!r.acknowledged||!session.sessionId.equals(r.sessionId)||!"ANSWER".equals(r.operation)||receipts.containsKey(r.questionId))return false;
        if(pending==null||!pending.actionId.equals(r.actionId)||!pending.questionId.equals(r.questionId))return false;
        receipts.put(r.questionId,r);pending=null;session.receipts.add(r);lastReceipt=r;
        session.version=r.sessionVersion;session.state=r.sessionState;session.finishReason=r.finishReason;
        session.answeredCount=receipts.size();session.correctCount=0;session.awardedXp=0;
        for(Receipt committed:receipts.values()){if(committed.correct)session.correctCount++;session.awardedXp+=committed.awardedXp;}
        if("MATCHING".equals(session.method)&&!r.correct){
            Question old=current();if(old!=null)for(Question candidate:questions)if(Objects.equals(candidate.roundIndex,old.roundIndex)&&receipt(candidate.questionId)==null){cursor=candidate.ordinal;break;}
        }
        return true;
    }
    public boolean next() {
        if(session==null||pending!=null||pendingControl!=null)return false;
        Question q=current();if(q==null||receipt(q.questionId)==null)return false;
        if(!active()){
            for(Question candidate:questions)if(receipt(candidate.questionId)!=null&&("MATCHING".equals(session.method)?candidate.roundIndex>q.roundIndex:candidate.ordinal>q.ordinal)){cursor=candidate.ordinal;return true;}
            return false;
        }
        if("MATCHING".equals(session.method)){
            int round=q.roundIndex==null?0:q.roundIndex;
            for(Question candidate:questions)if(candidate.roundIndex!=null&&candidate.roundIndex==round&&receipt(candidate.questionId)==null){cursor=candidate.ordinal;return true;}
            int index=(round+1)*4;if(index<questions.size()){cursor=index;return true;}
        }else if(cursor+1<questions.size()&&(!"CHALLENGE".equals(session.method)||!"INCORRECT".equals(session.finishReason)||cursor+1<session.answeredCount)){cursor++;return true;}
        return false;
    }
    public void previous() {
        if(pending!=null||pendingControl!=null||cursor<=0)return;
        if(!active()){
            Question current=current();if(current==null)return;
            for(int i=cursor-1;i>=0;i--){Question candidate=questions.get(i);if(receipt(candidate.questionId)!=null&&(!"MATCHING".equals(session.method)||candidate.roundIndex<current.roundIndex)){cursor=i;return;}}
        }else cursor="MATCHING".equals(session.method)?Math.max(0,((cursor/4)-1)*4):cursor-1;
    }
    public boolean review(){
        if(session==null||pending!=null||pendingControl!=null)return false;
        if(active())return current()!=null;
        for(Question q:questions)if(receipt(q.questionId)!=null){cursor=q.ordinal;return true;}
        return false;
    }
    public boolean hasLaterReviewUnit(){
        Question current=current();if(current==null||session==null)return false;
        for(Question q:questions)if(receipt(q.questionId)!=null&&("MATCHING".equals(session.method)?q.roundIndex>current.roundIndex:q.ordinal>current.ordinal))return true;
        return false;
    }
    public int accuracy() { return session==null||session.answeredCount==0?0:Math.round(100f*session.correctCount/session.answeredCount); }
    public int challengeStreak() {
        int streak=0;for(Receipt r:receipts.values()){if(r.correct)streak++;else streak=0;}return streak;
    }
    public long durationSeconds(long now) {
        if(session==null||session.startedAt==null)return 0;
        try{return Math.max(0,((session.finishedAt==null?now:isoMillis(session.finishedAt))-isoMillis(session.startedAt))/1000);}catch(RuntimeException ignored){return 0;}
    }
    public List<Receipt> incorrect() { List<Receipt> result=new ArrayList<>();for(Receipt r:receipts.values())if(!r.correct)result.add(r);return result; }
    private static long isoMillis(String value) {
        for(String pattern:new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'"}){
            SimpleDateFormat format=new SimpleDateFormat(pattern,Locale.ROOT);format.setLenient(false);format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try{return Objects.requireNonNull(format.parse(value)).getTime();}catch(ParseException ignored){ }
        }
        throw new IllegalArgumentException("Invalid server timestamp");
    }
    public static boolean dispatchDurably(java.util.function.BooleanSupplier checkpoint,Runnable dispatch) {
        if(!checkpoint.getAsBoolean())return false;
        dispatch.run();return true;
    }
    /** Only a definitive server rejection before progress/history writes can release an unanswered request. */
    public boolean rejectDefinitively(String code){
        if(!"QUESTION_WITHDRAWN".equals(code)||pending==null)return false;
        pending=null;return true;
    }
}
