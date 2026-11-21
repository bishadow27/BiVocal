package com.example.bivoca.ui.quiz;

import com.example.bivoca.data.model.QuizSessionDtos.*;
import com.google.gson.Gson;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class QuizSessionStateTest {
    private QuizSessionState state(String method,int size) {
        Session s=new Session();s.sessionId="session";s.method=method;s.state="ACTIVE";s.selectedUnitCount=size;
        s.startedAt="2026-11-07T10:00:00Z";
        List<Question> qs=new ArrayList<>();
        for(int i=0;i<size;i++){Question q=new Question();q.sessionId=s.sessionId;q.questionId="q"+i;q.ordinal=i;q.kind=method.equals("MATCHING")?"MATCHING_PAIR":"EN_TO_VI";q.roundIndex=i/4;
            for(int j=0;j<4;j++){Option o=new Option();o.optionId="o"+j;o.text="Nghĩa tiếng Việt đầy đủ "+j;q.options.add(o);}qs.add(q);}
        QuizSessionState result=new QuizSessionState();result.acceptSnapshot(s,qs);return result;
    }
    private Receipt receipt(Answer a,int version,boolean correct,int xp) {
        Receipt r=new Receipt();r.sessionId="session";r.questionId=a.questionId;r.actionId=a.actionId;r.receiptId="receipt"+version;r.operation="ANSWER";
        r.sessionVersion=version;r.sessionState="ACTIVE";r.acknowledged=true;r.correct=correct;r.awardedXp=xp;return r;
    }
    @Test public void actualCapacityDefaultNeverUsesVocabularyCount() {
        for(int size:new int[]{0,15,17,20,40,60,80,100}){
            Availability a=new Availability();a.vocabularyCount=20000;a.quizEligibleCount=size;
            assertEquals(size<20?size:20,QuizSessionState.defaultCount(a));
        }
    }
    @Test public void pendingActionSurvivesRetryAndBlocksDuplicateTap() {
        QuizSessionState s=state("EN_TO_VI",20);Answer pending=s.select("q0","o1");assertNotNull(pending);
        assertNotNull(pending.actionId);assertEquals(0,pending.expectedVersion);assertTrue(s.locked("q0"));
        assertNull(s.select("q0","o2"));assertSame(pending,s.pending);
        Answer restored=new Gson().fromJson(new Gson().toJson(pending),Answer.class);assertEquals(pending.actionId,restored.actionId);
        Receipt ack=receipt(pending,1,true,0);assertTrue(s.ack(ack));assertNull(s.pending);assertTrue(s.locked("q0"));
        assertFalse(s.ack(ack));assertEquals(1,s.session.answeredCount);assertEquals(0,s.session.awardedXp);
    }
    @Test public void resumeReconcilesCommittedPendingWithoutSecondSubmission() {
        QuizSessionState s=state("EN_TO_VI",20);Answer a=s.select("q0","o0");assertNotNull(a);
        Session snapshot=new Gson().fromJson(new Gson().toJson(s.session),Session.class);snapshot.receipts.add(receipt(a,1,false,0));snapshot.answeredCount=1;snapshot.version=1;
        s.acceptSnapshot(snapshot,s.questions);assertNull(s.pending);assertNotNull(s.receipt("q0"));assertTrue(s.locked("q0"));
    }
    @Test public void matchingAllowsAnyUnansweredPairOnlyInsideCurrentRound() {
        QuizSessionState s=state("MATCHING",5);assertNull(s.select("q4","o1"));
        for(int i:new int[]{3,1,0,2}){Answer a=s.select("q"+i,"o1");assertNotNull(a);assertTrue(s.ack(receipt(a,s.session.version+1,i!=1,0)));assertNull(s.select("q"+i,"o2"));}
        assertNotNull(s.select("q4","o0"));assertEquals(4,s.session.answeredCount);
    }
    @Test public void terminalSnapshotCannotResurrectAndResultsUseActualAwardedXp() {
        QuizSessionState s=state("CHALLENGE",20);Answer a=s.select("q0","o1");assertNotNull(a);Receipt r=receipt(a,1,false,0);r.sessionState="COMPLETED";r.finishReason="INCORRECT";
        s.ack(r);assertNull(s.select("q1","o0"));assertEquals(0,s.accuracy());assertEquals(0,s.challengeStreak());assertEquals(0,s.session.awardedXp);
        s.session.finishedAt="2026-11-07T10:00:35Z";assertEquals(35,s.durationSeconds(0));
    }
    @Test public void staleUnrelatedAckCannotConsumePendingOrAdvanceCounters() {
        QuizSessionState s=state("EN_TO_VI",20);Answer a=s.select("q0","o1");assertNotNull(a);Receipt stale=receipt(a,1,true,10);stale.sessionId="other";
        assertFalse(s.ack(stale));assertSame(a,s.pending);assertEquals(0,s.session.answeredCount);
    }
    @Test public void unresolvedControlBlocksAnswersAfterRecreation() throws Exception {
        QuizSessionState s=state("MATCHING",5);
        Control control=new Control();control.actionId="pending-abandon";control.expectedVersion=0;
        java.lang.reflect.Field field;
        try{field=QuizSessionState.class.getField("pendingControl");}catch(NoSuchFieldException e){fail("Pending control is not part of protected session state");return;}
        field.set(s,control);assertNull(s.select("q0","o1"));assertTrue(s.locked("q0"));
        s.acceptSnapshot(s.session,s.questions);assertSame(control,field.get(s));assertNull(s.select("q0","o1"));
    }
    @Test public void definitivelyStalePendingRequiresNewExplicitChoice() {
        QuizSessionState s=state("MATCHING",5);Answer old=s.select("q0","o1");assertNotNull(old);
        Session snapshot=new Gson().fromJson(new Gson().toJson(s.session),Session.class);snapshot.version=1;snapshot.answeredCount=1;
        Answer other=new Answer();other.actionId="other";other.questionId="q1";snapshot.receipts.add(receipt(other,1,true,0));
        s.acceptSnapshot(snapshot,s.questions);assertNull("Do not loop retry with obsolete version",s.pending);
        Answer fresh=s.select("q0","o1");assertNotNull(fresh);assertNotEquals(old.actionId,fresh.actionId);assertEquals(1,fresh.expectedVersion);
    }
    @Test public void wrongMatchingAckAdvancesInsideSameRound() {
        QuizSessionState s=state("MATCHING",5);Answer a=s.select("q3","o0");assertNotNull(a);s.ack(receipt(a,1,false,0));
        assertEquals(0,s.current().ordinal);assertTrue(s.locked("q3"));assertNotNull(s.select("q0","o1"));
    }
    @Test public void failedCheckpointRetryCannotDispatchAnUnsavedAction() throws Exception {
        java.lang.reflect.Method method;
        try{method=QuizSessionState.class.getMethod("dispatchDurably",java.util.function.BooleanSupplier.class,Runnable.class);}catch(NoSuchMethodException e){fail("No shared durable dispatch gate");return;}
        QuizSessionState s=state("EN_TO_VI",20);Answer a=s.select("q0","o1");assertNotNull(a);java.util.concurrent.atomic.AtomicInteger calls=new java.util.concurrent.atomic.AtomicInteger();
        java.util.function.BooleanSupplier fail=()->false;
        assertEquals(false,method.invoke(null,fail,(Runnable)calls::incrementAndGet));assertEquals(false,method.invoke(null,fail,(Runnable)calls::incrementAndGet));assertEquals(0,calls.get());
        assertEquals(true,method.invoke(null,(java.util.function.BooleanSupplier)()->true,(Runnable)calls::incrementAndGet));assertEquals(1,calls.get());assertSame(a,s.pending);
    }
    @Test public void definitiveWithdrawalAllowsAbandonWithoutInventingAck() throws Exception {
        QuizSessionState s=state("EN_TO_VI",20);Answer pending=s.select("q0","o1");assertNotNull(pending);
        java.lang.reflect.Method reject;
        try{reject=QuizSessionState.class.getMethod("rejectDefinitively",String.class);}catch(NoSuchMethodException e){fail("Withdrawn question has no recovery from immutable pending answer");return;}
        assertEquals(false,reject.invoke(s,"NETWORK"));assertSame(pending,s.pending);
        assertEquals(false,reject.invoke(s,"SESSION_VERSION_CONFLICT"));assertSame(pending,s.pending);
        assertEquals(true,reject.invoke(s,"QUESTION_WITHDRAWN"));assertNull(s.pending);assertNull(s.receipt("q0"));assertEquals(0,s.session.answeredCount);assertEquals(0,s.session.awardedXp);assertTrue(s.active());
    }
    @Test public void abandonedMcReviewNeverAdvancesIntoUnansweredQuestion() {
        QuizSessionState s=state("EN_TO_VI",20);Answer a=s.select("q0","o1");assertNotNull(a);s.ack(receipt(a,1,true,0));s.session.state="ABANDONED";s.session.finishReason="USER_ABANDONED";
        assertFalse("Read-only review cannot navigate into unanswered question",s.next());assertEquals(0,s.cursor);
    }
    @Test public void abandonedMatchingReviewNeverEntersUnansweredPair() {
        QuizSessionState s=state("MATCHING",5);Answer a=s.select("q3","o1");assertNotNull(a);s.ack(receipt(a,1,false,0));s.session.state="ABANDONED";s.cursor=3;
        assertFalse("Read-only matching review cannot enter unresolved pair",s.next());assertEquals(3,s.cursor);
    }
}
