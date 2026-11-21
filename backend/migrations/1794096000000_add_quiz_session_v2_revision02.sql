-- Up Migration
-- DOCUMENT ONLY. Requires approved runner singleTransaction=true; NOT standalone psql statements.
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='30s';
SET LOCAL search_path=pg_catalog,pg_temp;
DO $$ BEGIN
 IF current_database() IS DISTINCT FROM 'bivoca_m15_7_recovered'
 OR inet_server_port() IS DISTINCT FROM 5432
 OR host(inet_server_addr()) IS DISTINCT FROM '127.0.0.1'
 OR (SELECT system_identifier::text FROM pg_control_system())
      IS DISTINCT FROM '7703087371204641928' THEN
  RAISE EXCEPTION 'Wrong operational Quiz v2 migration target';
 END IF;
 IF pg_is_in_recovery() OR current_setting('transaction_read_only') <> 'off' THEN
  RAISE EXCEPTION 'Writable primary required only after execution approval';
 END IF;
END $$;
LOCK TABLE public.pgmigrations IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
 IF (SELECT count(*) FROM public.pgmigrations) <> 10
 OR EXISTS (SELECT 1 FROM unnest(ARRAY['1791072000000_create_users','1791072001000_create_user_progress','1791072002000_create_quiz_results','1791072003000_create_gamification','1791072004000_add_admin_authorization','1791964800000_schedule_progress_reviews','1792533600000_add_vocabulary_provenance','1792616400000_add_vocabulary_review_workflow','1792702800000_add_federated_auth_and_password_recovery','1792789200000_add_daily_learning_goals']::text[]) AS expected(name)
   WHERE NOT EXISTS (SELECT 1 FROM public.pgmigrations m WHERE m.name=expected.name)) THEN
  RAISE EXCEPTION 'Baseline migration history drift; stop for review';
 END IF;
 IF EXISTS (SELECT 1 FROM public.pgmigrations WHERE name='1794096000000_add_quiz_session_v2_revision02') THEN
  RAISE EXCEPTION 'Revision02 already recorded; inspect instead of rerunning';
 END IF;
END $$;
DO $$ BEGIN
 -- Fail before v2 DDL if any partial, misplaced or colliding object exists.
 IF EXISTS (SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
   WHERE n.nspname='public' AND (c.relname IN ('quiz_actions_v2_one_answer','xp_history_quiz_action_unique','quiz_sessions_v2_user_created')
     OR c.relname ~ '^quiz_(sessions|questions|actions)_v2($|_)'))
 OR EXISTS (SELECT 1 FROM pg_catalog.pg_attribute a JOIN pg_catalog.pg_class c ON c.oid=a.attrelid
   JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relname='xp_history'
   AND a.attname IN ('quiz_action_id','quiz_action_operation') AND a.attnum>0 AND NOT a.attisdropped)
 OR EXISTS (SELECT 1 FROM pg_catalog.pg_trigger t JOIN pg_catalog.pg_class c ON c.oid=t.tgrelid
   JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND t.tgname IN ('quiz_v2_session_transition','quiz_v2_creation_snapshot','quiz_v2_question_snapshot','quiz_v2_action_binding','quiz_v2_xp_binding','quiz_v2_final_receipt'))
 OR EXISTS (SELECT 1 FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace
   WHERE n.nspname='public' AND f.proname IN ('quiz_v2_session_transition','quiz_v2_creation_snapshot','quiz_v2_question_snapshot','quiz_v2_action_binding','quiz_v2_xp_binding','quiz_v2_final_receipt'))
 OR EXISTS (SELECT 1 FROM pg_catalog.pg_constraint k JOIN pg_catalog.pg_namespace n ON n.oid=k.connamespace
   WHERE n.nspname='public' AND (k.conname IN ('xp_history_v2_link','xp_history_v2_owner_answer_fk','quiz_v2_session_transition','quiz_v2_creation_snapshot','quiz_v2_question_snapshot','quiz_v2_action_binding','quiz_v2_xp_binding','quiz_v2_final_receipt')
     OR k.conname ~ '^quiz_(sessions|questions|actions)_v2($|_)'))
 OR EXISTS (SELECT 1 FROM pg_catalog.pg_type t JOIN pg_catalog.pg_namespace n ON n.oid=t.typnamespace
   WHERE n.nspname='public' AND t.typname IN ('quiz_sessions_v2','quiz_questions_v2','quiz_actions_v2')) THEN
  RAISE EXCEPTION 'Partial v2 object or schema drift; stop before DDL';
 END IF;
END $$;
CREATE TABLE public.quiz_sessions_v2 (
 id uuid PRIMARY KEY,
 user_id integer NOT NULL REFERENCES public.users(id),
 creation_id uuid NOT NULL,
 creation_hash text NOT NULL CHECK(creation_hash ~ '^[0-9a-f]{64}$'),
 creation_receipt jsonb NOT NULL CHECK(jsonb_typeof(creation_receipt)='object'),
 method text NOT NULL CHECK(method IN ('MIXED','CHALLENGE','CONTEXT','EN_TO_VI','VI_TO_EN','MATCHING')),
 unit text NOT NULL CHECK(unit IN ('QUESTION','PAIR')),
 configuration jsonb NOT NULL CHECK(jsonb_typeof(configuration)='object'),
 selected_unit_count integer NOT NULL CHECK(selected_unit_count BETWEEN 1 AND 100),
 state text NOT NULL DEFAULT 'ACTIVE' CHECK(state IN ('ACTIVE','COMPLETED','ABANDONED')),
 finish_reason text CHECK(finish_reason IN ('COMPLETED_ALL','INCORRECT','USER_ABANDONED')),
 version integer NOT NULL DEFAULT 0 CHECK(version>=0),
 answered_count integer NOT NULL DEFAULT 0 CHECK(answered_count BETWEEN 0 AND selected_unit_count),
 correct_count integer NOT NULL DEFAULT 0 CHECK(correct_count BETWEEN 0 AND answered_count),
 awarded_xp integer NOT NULL DEFAULT 0 CHECK(awarded_xp>=0),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 finished_at timestamptz,
 UNIQUE(id,user_id), UNIQUE(user_id,creation_id),
 CHECK((method='MATCHING' AND unit='PAIR') OR (method<>'MATCHING' AND unit='QUESTION')),
 CHECK(
  (state='ACTIVE' AND finish_reason IS NULL AND finished_at IS NULL
   AND answered_count<selected_unit_count
   AND (method<>'CHALLENGE' OR correct_count=answered_count))
  OR (state='COMPLETED' AND finished_at IS NOT NULL AND finish_reason IS NOT NULL AND
   ((finish_reason='COMPLETED_ALL' AND answered_count=selected_unit_count
     AND (method<>'CHALLENGE' OR correct_count=answered_count))
    OR (finish_reason='INCORRECT' AND method='CHALLENGE'
        AND answered_count>0 AND correct_count=answered_count-1)))
  OR (state='ABANDONED' AND finish_reason IS NOT NULL AND finish_reason='USER_ABANDONED'
      AND finished_at IS NOT NULL AND answered_count<selected_unit_count
      AND (method<>'CHALLENGE' OR correct_count=answered_count))
 )
);
CREATE INDEX quiz_sessions_v2_user_created ON public.quiz_sessions_v2(user_id,created_at DESC);

CREATE TABLE public.quiz_questions_v2 (
 id uuid PRIMARY KEY,
 session_id uuid NOT NULL,
 user_id integer NOT NULL,
 ordinal integer NOT NULL CHECK(ordinal BETWEEN 0 AND 99),
 kind text NOT NULL CHECK(kind IN ('CONTEXT','EN_TO_VI','VI_TO_EN','MATCHING_PAIR')),
 flashcard_id integer NOT NULL REFERENCES public.flashcards(id),
 round_index integer,
 round_position integer,
 public_payload jsonb NOT NULL CHECK(jsonb_typeof(public_payload)='object'),
 options jsonb NOT NULL CHECK(jsonb_typeof(options)='array'),
 correct_option_id uuid NOT NULL,
 grading_payload jsonb NOT NULL CHECK(jsonb_typeof(grading_payload)='object'),
 FOREIGN KEY(session_id,user_id) REFERENCES public.quiz_sessions_v2(id,user_id),
 UNIQUE(session_id,ordinal), UNIQUE(session_id,user_id,id,flashcard_id),
 UNIQUE(session_id,flashcard_id),
 CHECK((kind='MATCHING_PAIR' AND round_index IS NOT NULL AND round_position IS NOT NULL
        AND round_index=ordinal/4 AND round_position=ordinal%4)
    OR (kind<>'MATCHING_PAIR' AND round_index IS NULL AND round_position IS NULL))
);

CREATE TABLE public.quiz_actions_v2 (
 id uuid PRIMARY KEY,
 session_id uuid NOT NULL,
 user_id integer NOT NULL,
 action_id uuid NOT NULL,
 operation text NOT NULL CHECK(operation IN ('ANSWER','COMPLETE','ABANDON')),
 question_id uuid,
 flashcard_id integer,
 selected_option_id uuid,
 correct boolean,
 progress_snapshot jsonb,
 awarded_xp integer NOT NULL DEFAULT 0 CHECK(awarded_xp IN (0,10)),
 session_version integer NOT NULL CHECK(session_version>=0),
 session_state text NOT NULL CHECK(session_state IN ('ACTIVE','COMPLETED','ABANDONED')),
 finish_reason text,
 request_hash text NOT NULL CHECK(request_hash ~ '^[0-9a-f]{64}$'),
 response_snapshot jsonb NOT NULL CHECK(jsonb_typeof(response_snapshot)='object'),
 created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
 write_txid xid8 NOT NULL DEFAULT pg_current_xact_id(),
 FOREIGN KEY(session_id,user_id) REFERENCES public.quiz_sessions_v2(id,user_id),
 FOREIGN KEY(session_id,user_id,question_id,flashcard_id)
  REFERENCES public.quiz_questions_v2(session_id,user_id,id,flashcard_id),
 UNIQUE(session_id,action_id), UNIQUE(user_id,id,operation,flashcard_id),
 CHECK(
  (operation='ANSWER' AND
   ((session_state='ACTIVE' AND finish_reason IS NULL)
    OR (session_state='COMPLETED' AND finish_reason IS NOT NULL
        AND finish_reason IN ('COMPLETED_ALL','INCORRECT'))))
  OR (operation='COMPLETE' AND session_state='COMPLETED'
      AND finish_reason IS NOT NULL AND finish_reason='COMPLETED_ALL')
  OR (operation='ABANDON' AND session_state='ABANDONED'
      AND finish_reason IS NOT NULL AND finish_reason='USER_ABANDONED')
 ),
 CHECK(
  (operation='ANSWER' AND question_id IS NOT NULL AND flashcard_id IS NOT NULL
   AND selected_option_id IS NOT NULL AND correct IS NOT NULL
   AND progress_snapshot IS NOT NULL AND jsonb_typeof(progress_snapshot)='object'
   AND session_version>0 AND (correct OR awarded_xp=0))
  OR (operation IN ('COMPLETE','ABANDON') AND question_id IS NULL AND flashcard_id IS NULL
   AND selected_option_id IS NULL AND correct IS NULL AND progress_snapshot IS NULL AND awarded_xp=0)
 )
);
CREATE UNIQUE INDEX quiz_actions_v2_one_answer
 ON public.quiz_actions_v2(session_id,question_id) WHERE operation='ANSWER';

ALTER TABLE public.xp_history ADD COLUMN quiz_action_id uuid;
ALTER TABLE public.xp_history ADD COLUMN quiz_action_operation text;
ALTER TABLE public.xp_history ADD CONSTRAINT xp_history_v2_link CHECK(
 (quiz_action_id IS NULL AND quiz_action_operation IS NULL)
 OR (quiz_action_id IS NOT NULL AND quiz_action_operation IS NOT NULL
     AND quiz_action_operation='ANSWER' AND user_id IS NOT NULL
     AND flashcard_id IS NOT NULL AND source_type='QUIZ' AND quiz_result_id IS NULL)
) NOT VALID;
ALTER TABLE public.xp_history ADD CONSTRAINT xp_history_v2_owner_answer_fk
 FOREIGN KEY(user_id,quiz_action_id,quiz_action_operation,flashcard_id)
 REFERENCES public.quiz_actions_v2(user_id,id,operation,flashcard_id) NOT VALID;
CREATE UNIQUE INDEX xp_history_quiz_action_unique ON public.xp_history(quiz_action_id)
 WHERE quiz_action_id IS NOT NULL;

CREATE FUNCTION public.quiz_v2_session_transition() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Creation receipt/session history is immutable'; END IF;
 IF TG_OP='INSERT' THEN
  IF NEW.state<>'ACTIVE' OR NEW.version<>0 OR NEW.answered_count<>0
     OR NEW.correct_count<>0 OR NEW.awarded_xp<>0 THEN
   RAISE EXCEPTION 'Invalid initial session';
  END IF;
  RETURN NEW;
 END IF;
 IF OLD.state<>'ACTIVE' AND NEW IS DISTINCT FROM OLD THEN
  RAISE EXCEPTION 'Terminal session is immutable';
 END IF;
 IF NEW IS DISTINCT FROM OLD THEN
  IF ROW(NEW.id,NEW.user_id,NEW.creation_id,NEW.creation_hash,NEW.creation_receipt,
         NEW.method,NEW.unit,NEW.configuration,NEW.selected_unit_count,NEW.created_at)
      IS DISTINCT FROM
     ROW(OLD.id,OLD.user_id,OLD.creation_id,OLD.creation_hash,OLD.creation_receipt,
         OLD.method,OLD.unit,OLD.configuration,OLD.selected_unit_count,OLD.created_at)
     OR NEW.version<>OLD.version+1 OR NEW.answered_count<OLD.answered_count
     OR NEW.correct_count<OLD.correct_count OR NEW.awarded_xp<OLD.awarded_xp THEN
   RAISE EXCEPTION 'Invalid session mutation/version';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER quiz_v2_session_transition BEFORE INSERT OR UPDATE OR DELETE ON public.quiz_sessions_v2
 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_session_transition();

CREATE FUNCTION public.quiz_v2_creation_snapshot() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
DECLARE s public.quiz_sessions_v2; n integer;
BEGIN
 SELECT * INTO STRICT s FROM public.quiz_sessions_v2 WHERE id=NEW.id;
 SELECT count(*) INTO n FROM public.quiz_questions_v2 WHERE session_id=s.id;
 IF n<>s.selected_unit_count
 OR s.creation_receipt->>'sessionId' IS DISTINCT FROM s.id::text
 OR s.creation_receipt->>'creationId' IS DISTINCT FROM s.creation_id::text
 OR s.creation_receipt->>'method' IS DISTINCT FROM s.method
 OR s.creation_receipt->>'unit' IS DISTINCT FROM s.unit
 OR s.creation_receipt->>'selectedUnitCount' IS DISTINCT FROM s.selected_unit_count::text
 OR s.creation_receipt->>'state' IS DISTINCT FROM 'ACTIVE'
 OR s.creation_receipt->>'version' IS DISTINCT FROM '0' THEN
  RAISE EXCEPTION 'Incomplete creation snapshot/receipt';
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER quiz_v2_creation_snapshot AFTER INSERT ON public.quiz_sessions_v2
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_creation_snapshot();

CREATE FUNCTION public.quiz_v2_question_snapshot() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
DECLARE s public.quiz_sessions_v2; n integer; distinct_ids integer;
BEGIN
 IF TG_OP<>'INSERT' THEN RAISE EXCEPTION 'Question snapshot is immutable'; END IF;
 SELECT * INTO STRICT s FROM public.quiz_sessions_v2 WHERE id=NEW.session_id AND user_id=NEW.user_id;
 IF s.state<>'ACTIVE' OR s.version<>0 OR NEW.ordinal>=s.selected_unit_count
 OR ((s.method='MATCHING') IS DISTINCT FROM (NEW.kind='MATCHING_PAIR'))
 OR (s.method IN ('CONTEXT','EN_TO_VI','VI_TO_EN') AND NEW.kind<>s.method) THEN
  RAISE EXCEPTION 'Question does not fit session';
 END IF;
 -- Casting optionId to uuid also rejects malformed/NULL identifiers.
 SELECT count(*),count(DISTINCT (v->>'optionId')::uuid) INTO n,distinct_ids
 FROM jsonb_array_elements(NEW.options) v;
 IF n<>distinct_ids OR n<>(CASE WHEN NEW.kind='MATCHING_PAIR'
    THEN LEAST(4,s.selected_unit_count-(NEW.ordinal/4)*4) ELSE 4 END)
 OR EXISTS(SELECT 1 FROM jsonb_array_elements(NEW.options) v
    WHERE jsonb_typeof(v->'text') IS DISTINCT FROM 'string')
 OR NOT EXISTS(SELECT 1 FROM jsonb_array_elements(NEW.options) v
    WHERE (v->>'optionId')::uuid=NEW.correct_option_id) THEN
  RAISE EXCEPTION 'Invalid stable options/grading binding';
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER quiz_v2_question_snapshot BEFORE INSERT OR UPDATE OR DELETE ON public.quiz_questions_v2
 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_question_snapshot();

CREATE FUNCTION public.quiz_v2_action_binding() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
DECLARE q public.quiz_questions_v2;
BEGIN
 IF TG_OP='INSERT' THEN NEW.write_txid := pg_current_xact_id(); END IF;
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Action history is immutable'; END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.write_txid<>pg_current_xact_id()
  OR ROW(NEW.id,NEW.session_id,NEW.user_id,NEW.action_id,NEW.operation,NEW.question_id,
         NEW.flashcard_id,NEW.selected_option_id,NEW.correct,NEW.request_hash,NEW.created_at,NEW.write_txid)
     IS DISTINCT FROM
     ROW(OLD.id,OLD.session_id,OLD.user_id,OLD.action_id,OLD.operation,OLD.question_id,
         OLD.flashcard_id,OLD.selected_option_id,OLD.correct,OLD.request_hash,OLD.created_at,OLD.write_txid) THEN
   RAISE EXCEPTION 'Committed action is immutable';
  END IF;
 END IF;
 IF NEW.operation='ANSWER' THEN
  SELECT * INTO STRICT q FROM public.quiz_questions_v2 WHERE id=NEW.question_id
   AND session_id=NEW.session_id AND user_id=NEW.user_id AND flashcard_id=NEW.flashcard_id;
  IF NOT EXISTS(SELECT 1 FROM jsonb_array_elements(q.options) v
     WHERE (v->>'optionId')::uuid=NEW.selected_option_id)
     OR NEW.correct IS DISTINCT FROM (NEW.selected_option_id=q.correct_option_id) THEN
   RAISE EXCEPTION 'Option/grading does not fit question';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER quiz_v2_action_binding BEFORE INSERT OR UPDATE OR DELETE ON public.quiz_actions_v2
 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_action_binding();

CREATE FUNCTION public.quiz_v2_xp_binding() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
DECLARE a public.quiz_actions_v2;
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.quiz_action_id IS NOT NULL THEN RAISE EXCEPTION 'Committed quiz XP is immutable'; END IF;
  RETURN OLD;
 END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.quiz_action_id IS NOT NULL OR NEW.quiz_action_id IS NOT NULL THEN
   RAISE EXCEPTION 'Quiz XP cannot be attached or changed by UPDATE';
  END IF;
  RETURN NEW;
 END IF;
 IF NEW.quiz_action_id IS NOT NULL THEN
  SELECT * INTO STRICT a FROM public.quiz_actions_v2 WHERE id=NEW.quiz_action_id;
  IF a.write_txid<>pg_current_xact_id() OR a.operation<>'ANSWER'
     OR NEW.user_id IS DISTINCT FROM a.user_id
     OR NEW.flashcard_id IS DISTINCT FROM a.flashcard_id THEN
   RAISE EXCEPTION 'Quiz XP must be inserted with its fresh owned answer';
  END IF;
 END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER quiz_v2_xp_binding BEFORE INSERT OR UPDATE OR DELETE ON public.xp_history
 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_xp_binding();

CREATE FUNCTION public.quiz_v2_final_receipt() RETURNS trigger LANGUAGE plpgsql
 SET search_path=pg_catalog,pg_temp AS $$
DECLARE a public.quiz_actions_v2; q public.quiz_questions_v2; total_xp integer;
BEGIN
 -- Re-read final row: deferred INSERT/UPDATE events must not inspect an earlier draft NEW.
 SELECT * INTO STRICT a FROM public.quiz_actions_v2 WHERE id=NEW.id;
 IF a.response_snapshot->'acknowledged' IS DISTINCT FROM 'true'::jsonb
 OR a.response_snapshot->>'actionId' IS DISTINCT FROM a.action_id::text
 OR a.response_snapshot->>'receiptId' IS DISTINCT FROM a.id::text
 OR a.response_snapshot->>'sessionId' IS DISTINCT FROM a.session_id::text
 OR a.response_snapshot->>'operation' IS DISTINCT FROM a.operation
 OR a.response_snapshot->>'sessionVersion' IS DISTINCT FROM a.session_version::text
 OR a.response_snapshot->>'sessionState' IS DISTINCT FROM a.session_state
 OR a.response_snapshot->>'finishReason' IS DISTINCT FROM a.finish_reason THEN
  RAISE EXCEPTION 'Unfinalized/mismatched receipt';
 END IF;
 IF a.operation='ANSWER' THEN
  SELECT * INTO STRICT q FROM public.quiz_questions_v2 WHERE id=a.question_id;
  SELECT COALESCE(sum(xp_amount),0)::integer INTO total_xp FROM public.xp_history WHERE quiz_action_id=a.id;
  IF total_xp<>a.awarded_xp
  OR a.response_snapshot->'progress'->'srsUpdated' IS DISTINCT FROM 'true'::jsonb
  OR a.response_snapshot->'progress'->'current' IS DISTINCT FROM a.progress_snapshot
  OR a.response_snapshot->>'questionId' IS DISTINCT FROM a.question_id::text
  OR a.response_snapshot->>'correct' IS DISTINCT FROM a.correct::text
  OR a.response_snapshot->>'awardedXp' IS DISTINCT FROM a.awarded_xp::text
  OR a.response_snapshot->'selectedAnswer'->>'optionId' IS DISTINCT FROM a.selected_option_id::text
  OR a.response_snapshot->'correctAnswer'->>'optionId' IS DISTINCT FROM q.correct_option_id::text
  OR a.response_snapshot->'selectedAnswer'->>'text' IS DISTINCT FROM
     (SELECT v->>'text' FROM jsonb_array_elements(q.options) v
       WHERE (v->>'optionId')::uuid=a.selected_option_id)
  OR a.response_snapshot->'correctAnswer'->>'text' IS DISTINCT FROM
     (SELECT v->>'text' FROM jsonb_array_elements(q.options) v
       WHERE (v->>'optionId')::uuid=q.correct_option_id)
  OR (a.response_snapshot->>'answeredAt')::timestamptz IS DISTINCT FROM a.created_at THEN
   RAISE EXCEPTION 'Answer receipt/progress/XP mismatch';
  END IF;
 ELSE
  IF a.response_snapshot ?| ARRAY['questionId','selectedAnswer','correct','progress','awardedXp'] THEN
   RAISE EXCEPTION 'Control operation cannot masquerade as answer';
  END IF;
 END IF;
 RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER quiz_v2_final_receipt AFTER INSERT OR UPDATE ON public.quiz_actions_v2
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_final_receipt();

-- Down Migration
-- DOCUMENT ONLY. Requires approved runner singleTransaction=true; NOT standalone psql statements.
SET LOCAL lock_timeout='3s';
SET LOCAL statement_timeout='30s';
SET LOCAL search_path=pg_catalog,pg_temp;
DO $$ BEGIN
 IF current_database() IS DISTINCT FROM 'bivoca_m15_7_recovered'
 OR inet_server_port() IS DISTINCT FROM 5432
 OR host(inet_server_addr()) IS DISTINCT FROM '127.0.0.1'
 OR (SELECT system_identifier::text FROM pg_control_system())
      IS DISTINCT FROM '7703087371204641928' THEN
  RAISE EXCEPTION 'Wrong operational Quiz v2 migration target';
 END IF;
 IF pg_is_in_recovery() OR current_setting('transaction_read_only') <> 'off' THEN
  RAISE EXCEPTION 'Writable primary required only after execution approval';
 END IF;
END $$;
LOCK TABLE public.pgmigrations IN SHARE ROW EXCLUSIVE MODE;
DO $$ BEGIN
 IF (SELECT count(*) FROM public.pgmigrations WHERE name='1794096000000_add_quiz_session_v2_revision02')<>1
 OR (SELECT name FROM public.pgmigrations ORDER BY id DESC LIMIT 1)
      IS DISTINCT FROM '1794096000000_add_quiz_session_v2_revision02' THEN
  RAISE EXCEPTION 'Down requires the exact latest Revision02 history entry';
 END IF;
END $$;
LOCK TABLE public.quiz_sessions_v2,public.quiz_questions_v2,public.quiz_actions_v2,public.xp_history IN ACCESS EXCLUSIVE MODE;
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM public.quiz_sessions_v2) OR EXISTS(SELECT 1 FROM public.quiz_questions_v2)
 OR EXISTS(SELECT 1 FROM public.quiz_actions_v2)
 OR EXISTS(SELECT 1 FROM public.xp_history WHERE quiz_action_id IS NOT NULL) THEN
  RAISE EXCEPTION 'Rollback refused: durable quiz data exists';
 END IF;
END $$;
DROP INDEX public.xp_history_quiz_action_unique;
DROP TRIGGER quiz_v2_xp_binding ON public.xp_history;
ALTER TABLE public.xp_history DROP CONSTRAINT xp_history_v2_owner_answer_fk;
ALTER TABLE public.xp_history DROP CONSTRAINT xp_history_v2_link;
ALTER TABLE public.xp_history DROP COLUMN quiz_action_operation;
ALTER TABLE public.xp_history DROP COLUMN quiz_action_id;
DROP TABLE public.quiz_actions_v2;
DROP TABLE public.quiz_questions_v2;
DROP TABLE public.quiz_sessions_v2;
DROP FUNCTION public.quiz_v2_final_receipt();
DROP FUNCTION public.quiz_v2_xp_binding();
DROP FUNCTION public.quiz_v2_action_binding();
DROP FUNCTION public.quiz_v2_question_snapshot();
DROP FUNCTION public.quiz_v2_session_transition();
DROP FUNCTION public.quiz_v2_creation_snapshot();
