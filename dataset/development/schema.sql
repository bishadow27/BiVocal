--
-- PostgreSQL database dump
--

\restrict rZ94hW2EYQeqgbaa4FCWLNo06L9G3McD14kjGGzIQxIRotUBfBWKWufYC1H5nBR

-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: dataset_unicode; Type: COLLATION; Schema: public; Owner: -
--

CREATE COLLATION public.dataset_unicode (provider = icu, locale = 'und');


--
-- Name: quiz_v2_action_binding(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_action_binding() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


--
-- Name: quiz_v2_creation_snapshot(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_creation_snapshot() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


--
-- Name: quiz_v2_final_receipt(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_final_receipt() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


--
-- Name: quiz_v2_question_snapshot(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_question_snapshot() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


--
-- Name: quiz_v2_session_transition(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_session_transition() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


--
-- Name: quiz_v2_xp_binding(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.quiz_v2_xp_binding() RETURNS trigger
    LANGUAGE plpgsql
    SET search_path TO 'pg_catalog', 'pg_temp'
    AS $$
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


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: flashcard_provenance; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flashcard_provenance (
    flashcard_id integer NOT NULL,
    source_id integer NOT NULL,
    source_record_id text NOT NULL,
    source_word_id bigint,
    source_definition_id bigint,
    cefr_scope text NOT NULL,
    validation_status text NOT NULL,
    validation_reasons jsonb DEFAULT '[]'::jsonb NOT NULL,
    quiz_eligible boolean DEFAULT false NOT NULL,
    imported_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT flashcard_provenance_validation_status_check CHECK ((validation_status = ANY (ARRAY['APPROVED_AUTO'::text, 'APPROVED_MANUAL'::text, 'NEEDS_REVIEW'::text, 'REJECTED'::text])))
);


--
-- Name: flashcard_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flashcard_topics (
    flashcard_id integer NOT NULL,
    topic_id integer NOT NULL,
    is_primary boolean DEFAULT false NOT NULL
);


--
-- Name: flashcards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flashcards (
    id integer NOT NULL,
    topic_id integer NOT NULL,
    english character varying(150) NOT NULL COLLATE public.dataset_unicode,
    vietnamese character varying(255) NOT NULL COLLATE public.dataset_unicode,
    pronunciation character varying(150),
    part_of_speech character varying(50),
    example text,
    example_vi text,
    level character varying(10),
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT flashcards_english_check CHECK ((((english)::text = btrim((english)::text)) AND ((english)::text <> ''::text))),
    CONSTRAINT flashcards_level_check CHECK (((level IS NULL) OR ((level)::text = ANY (ARRAY[('A1'::character varying)::text, ('A2'::character varying)::text, ('B1'::character varying)::text, ('B2'::character varying)::text])))),
    CONSTRAINT flashcards_part_of_speech_check CHECK (((part_of_speech IS NULL) OR ((part_of_speech)::text = ANY (ARRAY[('noun'::character varying)::text, ('verb'::character varying)::text, ('adjective'::character varying)::text, ('adverb'::character varying)::text, ('preposition'::character varying)::text, ('conjunction'::character varying)::text, ('pronoun'::character varying)::text, ('phrase'::character varying)::text])))),
    CONSTRAINT flashcards_vietnamese_check CHECK ((((vietnamese)::text = btrim((vietnamese)::text)) AND ((vietnamese)::text <> ''::text)))
);


--
-- Name: flashcards_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.flashcards_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: flashcards_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.flashcards_id_seq OWNED BY public.flashcards.id;


--
-- Name: google_auth_challenges; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.google_auth_challenges (
    id uuid NOT NULL,
    nonce_hash character(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: learning_system_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.learning_system_config (
    singleton boolean DEFAULT true NOT NULL,
    activated_date date NOT NULL,
    CONSTRAINT learning_system_config_singleton_check CHECK (singleton)
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    token_hash character(64) NOT NULL,
    expires_at timestamp with time zone NOT NULL,
    used_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: pgmigrations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.pgmigrations (
    id integer NOT NULL,
    name character varying(255) NOT NULL,
    run_on timestamp without time zone NOT NULL
);


--
-- Name: pgmigrations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.pgmigrations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pgmigrations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pgmigrations_id_seq OWNED BY public.pgmigrations.id;


--
-- Name: quiz_actions_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_actions_v2 (
    id uuid NOT NULL,
    session_id uuid NOT NULL,
    user_id integer NOT NULL,
    action_id uuid NOT NULL,
    operation text NOT NULL,
    question_id uuid,
    flashcard_id integer,
    selected_option_id uuid,
    correct boolean,
    progress_snapshot jsonb,
    awarded_xp integer DEFAULT 0 NOT NULL,
    session_version integer NOT NULL,
    session_state text NOT NULL,
    finish_reason text,
    request_hash text NOT NULL,
    response_snapshot jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    write_txid xid8 DEFAULT pg_current_xact_id() NOT NULL,
    CONSTRAINT quiz_actions_v2_awarded_xp_check CHECK ((awarded_xp = ANY (ARRAY[0, 10]))),
    CONSTRAINT quiz_actions_v2_check CHECK ((((operation = 'ANSWER'::text) AND (((session_state = 'ACTIVE'::text) AND (finish_reason IS NULL)) OR ((session_state = 'COMPLETED'::text) AND (finish_reason IS NOT NULL) AND (finish_reason = ANY (ARRAY['COMPLETED_ALL'::text, 'INCORRECT'::text]))))) OR ((operation = 'COMPLETE'::text) AND (session_state = 'COMPLETED'::text) AND (finish_reason IS NOT NULL) AND (finish_reason = 'COMPLETED_ALL'::text)) OR ((operation = 'ABANDON'::text) AND (session_state = 'ABANDONED'::text) AND (finish_reason IS NOT NULL) AND (finish_reason = 'USER_ABANDONED'::text)))),
    CONSTRAINT quiz_actions_v2_check1 CHECK ((((operation = 'ANSWER'::text) AND (question_id IS NOT NULL) AND (flashcard_id IS NOT NULL) AND (selected_option_id IS NOT NULL) AND (correct IS NOT NULL) AND (progress_snapshot IS NOT NULL) AND (jsonb_typeof(progress_snapshot) = 'object'::text) AND (session_version > 0) AND (correct OR (awarded_xp = 0))) OR ((operation = ANY (ARRAY['COMPLETE'::text, 'ABANDON'::text])) AND (question_id IS NULL) AND (flashcard_id IS NULL) AND (selected_option_id IS NULL) AND (correct IS NULL) AND (progress_snapshot IS NULL) AND (awarded_xp = 0)))),
    CONSTRAINT quiz_actions_v2_operation_check CHECK ((operation = ANY (ARRAY['ANSWER'::text, 'COMPLETE'::text, 'ABANDON'::text]))),
    CONSTRAINT quiz_actions_v2_request_hash_check CHECK ((request_hash ~ '^[0-9a-f]{64}$'::text)),
    CONSTRAINT quiz_actions_v2_response_snapshot_check CHECK ((jsonb_typeof(response_snapshot) = 'object'::text)),
    CONSTRAINT quiz_actions_v2_session_state_check CHECK ((session_state = ANY (ARRAY['ACTIVE'::text, 'COMPLETED'::text, 'ABANDONED'::text]))),
    CONSTRAINT quiz_actions_v2_session_version_check CHECK ((session_version >= 0))
);


--
-- Name: quiz_questions_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_questions_v2 (
    id uuid NOT NULL,
    session_id uuid NOT NULL,
    user_id integer NOT NULL,
    ordinal integer NOT NULL,
    kind text NOT NULL,
    flashcard_id integer NOT NULL,
    round_index integer,
    round_position integer,
    public_payload jsonb NOT NULL,
    options jsonb NOT NULL,
    correct_option_id uuid NOT NULL,
    grading_payload jsonb NOT NULL,
    CONSTRAINT quiz_questions_v2_check CHECK ((((kind = 'MATCHING_PAIR'::text) AND (round_index IS NOT NULL) AND (round_position IS NOT NULL) AND (round_index = (ordinal / 4)) AND (round_position = (ordinal % 4))) OR ((kind <> 'MATCHING_PAIR'::text) AND (round_index IS NULL) AND (round_position IS NULL)))),
    CONSTRAINT quiz_questions_v2_grading_payload_check CHECK ((jsonb_typeof(grading_payload) = 'object'::text)),
    CONSTRAINT quiz_questions_v2_kind_check CHECK ((kind = ANY (ARRAY['CONTEXT'::text, 'EN_TO_VI'::text, 'VI_TO_EN'::text, 'MATCHING_PAIR'::text]))),
    CONSTRAINT quiz_questions_v2_options_check CHECK ((jsonb_typeof(options) = 'array'::text)),
    CONSTRAINT quiz_questions_v2_ordinal_check CHECK (((ordinal >= 0) AND (ordinal <= 99))),
    CONSTRAINT quiz_questions_v2_public_payload_check CHECK ((jsonb_typeof(public_payload) = 'object'::text))
);


--
-- Name: quiz_results; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_results (
    id integer NOT NULL,
    user_id integer NOT NULL,
    flashcard_id integer NOT NULL,
    selected_flashcard_id integer,
    is_correct boolean NOT NULL,
    question_type character varying(30) DEFAULT 'EN_TO_VI'::character varying NOT NULL,
    answered_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT quiz_results_question_type CHECK (((question_type)::text = 'EN_TO_VI'::text))
);


--
-- Name: quiz_results_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.quiz_results_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: quiz_results_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.quiz_results_id_seq OWNED BY public.quiz_results.id;


--
-- Name: quiz_sessions_v2; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quiz_sessions_v2 (
    id uuid NOT NULL,
    user_id integer NOT NULL,
    creation_id uuid NOT NULL,
    creation_hash text NOT NULL,
    creation_receipt jsonb NOT NULL,
    method text NOT NULL,
    unit text NOT NULL,
    configuration jsonb NOT NULL,
    selected_unit_count integer NOT NULL,
    state text DEFAULT 'ACTIVE'::text NOT NULL,
    finish_reason text,
    version integer DEFAULT 0 NOT NULL,
    answered_count integer DEFAULT 0 NOT NULL,
    correct_count integer DEFAULT 0 NOT NULL,
    awarded_xp integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    finished_at timestamp with time zone,
    CONSTRAINT quiz_sessions_v2_awarded_xp_check CHECK ((awarded_xp >= 0)),
    CONSTRAINT quiz_sessions_v2_check CHECK (((answered_count >= 0) AND (answered_count <= selected_unit_count))),
    CONSTRAINT quiz_sessions_v2_check1 CHECK (((correct_count >= 0) AND (correct_count <= answered_count))),
    CONSTRAINT quiz_sessions_v2_check2 CHECK ((((method = 'MATCHING'::text) AND (unit = 'PAIR'::text)) OR ((method <> 'MATCHING'::text) AND (unit = 'QUESTION'::text)))),
    CONSTRAINT quiz_sessions_v2_check3 CHECK ((((state = 'ACTIVE'::text) AND (finish_reason IS NULL) AND (finished_at IS NULL) AND (answered_count < selected_unit_count) AND ((method <> 'CHALLENGE'::text) OR (correct_count = answered_count))) OR ((state = 'COMPLETED'::text) AND (finished_at IS NOT NULL) AND (finish_reason IS NOT NULL) AND (((finish_reason = 'COMPLETED_ALL'::text) AND (answered_count = selected_unit_count) AND ((method <> 'CHALLENGE'::text) OR (correct_count = answered_count))) OR ((finish_reason = 'INCORRECT'::text) AND (method = 'CHALLENGE'::text) AND (answered_count > 0) AND (correct_count = (answered_count - 1))))) OR ((state = 'ABANDONED'::text) AND (finish_reason IS NOT NULL) AND (finish_reason = 'USER_ABANDONED'::text) AND (finished_at IS NOT NULL) AND (answered_count < selected_unit_count) AND ((method <> 'CHALLENGE'::text) OR (correct_count = answered_count))))),
    CONSTRAINT quiz_sessions_v2_configuration_check CHECK ((jsonb_typeof(configuration) = 'object'::text)),
    CONSTRAINT quiz_sessions_v2_creation_hash_check CHECK ((creation_hash ~ '^[0-9a-f]{64}$'::text)),
    CONSTRAINT quiz_sessions_v2_creation_receipt_check CHECK ((jsonb_typeof(creation_receipt) = 'object'::text)),
    CONSTRAINT quiz_sessions_v2_finish_reason_check CHECK ((finish_reason = ANY (ARRAY['COMPLETED_ALL'::text, 'INCORRECT'::text, 'USER_ABANDONED'::text]))),
    CONSTRAINT quiz_sessions_v2_method_check CHECK ((method = ANY (ARRAY['MIXED'::text, 'CHALLENGE'::text, 'CONTEXT'::text, 'EN_TO_VI'::text, 'VI_TO_EN'::text, 'MATCHING'::text]))),
    CONSTRAINT quiz_sessions_v2_selected_unit_count_check CHECK (((selected_unit_count >= 1) AND (selected_unit_count <= 100))),
    CONSTRAINT quiz_sessions_v2_state_check CHECK ((state = ANY (ARRAY['ACTIVE'::text, 'COMPLETED'::text, 'ABANDONED'::text]))),
    CONSTRAINT quiz_sessions_v2_unit_check CHECK ((unit = ANY (ARRAY['QUESTION'::text, 'PAIR'::text]))),
    CONSTRAINT quiz_sessions_v2_version_check CHECK ((version >= 0))
);


--
-- Name: topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.topics (
    id integer NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    image_url text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT topics_code_check CHECK ((((code)::text = btrim((code)::text)) AND ((code)::text ~ '^[A-Z][A-Z0-9_]*$'::text)))
);


--
-- Name: topics_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.topics_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: topics_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.topics_id_seq OWNED BY public.topics.id;


--
-- Name: user_auth_identities; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_auth_identities (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    provider character varying(32) NOT NULL,
    provider_subject character varying(255) NOT NULL,
    email_at_link character varying(150),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT user_auth_identities_provider_check CHECK (((provider)::text = 'GOOGLE'::text))
);


--
-- Name: user_auth_identities_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_auth_identities_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_auth_identities_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_auth_identities_id_seq OWNED BY public.user_auth_identities.id;


--
-- Name: user_learned_flashcards; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_learned_flashcards (
    user_id integer NOT NULL,
    flashcard_id integer NOT NULL,
    first_learned_at timestamp with time zone,
    first_source character varying(16) NOT NULL,
    CONSTRAINT user_learned_flashcards_check CHECK (((((first_source)::text = 'LEGACY'::text) AND (first_learned_at IS NULL)) OR (((first_source)::text = ANY (ARRAY[('REVIEW'::character varying)::text, ('QUIZ'::character varying)::text])) AND (first_learned_at IS NOT NULL)))),
    CONSTRAINT user_learned_flashcards_first_source_check CHECK (((first_source)::text = ANY (ARRAY[('REVIEW'::character varying)::text, ('QUIZ'::character varying)::text, ('LEGACY'::character varying)::text])))
);


--
-- Name: user_learning_daily; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_learning_daily (
    user_id integer NOT NULL,
    learning_date date NOT NULL,
    goal_snapshot smallint NOT NULL,
    new_words_count integer DEFAULT 0 NOT NULL,
    review_count integer DEFAULT 0 NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT user_learning_daily_goal_snapshot_check CHECK ((goal_snapshot = ANY (ARRAY[5, 10, 15, 20]))),
    CONSTRAINT user_learning_daily_new_words_count_check CHECK ((new_words_count >= 0)),
    CONSTRAINT user_learning_daily_review_count_check CHECK ((review_count >= 0))
);


--
-- Name: user_learning_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_learning_settings (
    user_id integer NOT NULL,
    daily_goal smallint DEFAULT 10 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT user_learning_settings_daily_goal_check CHECK ((daily_goal = ANY (ARRAY[5, 10, 15, 20])))
);


--
-- Name: user_progress; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_progress (
    id integer NOT NULL,
    user_id integer NOT NULL,
    flashcard_id integer NOT NULL,
    memory_level smallint DEFAULT 0 NOT NULL,
    review_count integer DEFAULT 0 NOT NULL,
    correct_count integer DEFAULT 0 NOT NULL,
    wrong_count integer DEFAULT 0 NOT NULL,
    is_favorite boolean DEFAULT false NOT NULL,
    last_review timestamp with time zone,
    next_review timestamp with time zone,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT user_progress_correct_count_nonnegative CHECK ((correct_count >= 0)),
    CONSTRAINT user_progress_memory_level_range CHECK (((memory_level >= 0) AND (memory_level <= 5))),
    CONSTRAINT user_progress_review_count_nonnegative CHECK ((review_count >= 0)),
    CONSTRAINT user_progress_wrong_count_nonnegative CHECK ((wrong_count >= 0))
);


--
-- Name: user_progress_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.user_progress_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: user_progress_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.user_progress_id_seq OWNED BY public.user_progress.id;


--
-- Name: user_scores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_scores (
    user_id integer NOT NULL,
    total_xp integer DEFAULT 0 NOT NULL,
    current_streak integer DEFAULT 0 NOT NULL,
    longest_streak integer DEFAULT 0 NOT NULL,
    last_activity_date date,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT user_scores_current_streak_check CHECK ((current_streak >= 0)),
    CONSTRAINT user_scores_longest_at_least_current CHECK ((longest_streak >= current_streak)),
    CONSTRAINT user_scores_longest_streak_check CHECK ((longest_streak >= 0)),
    CONSTRAINT user_scores_total_xp_check CHECK ((total_xp >= 0))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id integer NOT NULL,
    username character varying(50) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash text,
    full_name character varying(100),
    avatar_url text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login timestamp without time zone,
    role character varying(20) DEFAULT 'USER'::character varying NOT NULL,
    auth_version integer DEFAULT 1 NOT NULL,
    CONSTRAINT users_auth_version_check CHECK ((auth_version >= 1)),
    CONSTRAINT users_email_trimmed CHECK ((((email)::text = btrim((email)::text)) AND ((email)::text <> ''::text))),
    CONSTRAINT users_role_check CHECK (((role)::text = ANY (ARRAY[('USER'::character varying)::text, ('ADMIN'::character varying)::text]))),
    CONSTRAINT users_username_format CHECK (((username)::text ~ '^[A-Za-z0-9_]{3,30}$'::text))
);


--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.users_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: vocabulary_publish_batch_items; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_publish_batch_items (
    batch_id character varying(36) NOT NULL,
    candidate_id character varying(80) NOT NULL,
    candidate_version integer NOT NULL,
    disposition text NOT NULL,
    duplicate_flashcard_id integer,
    reason text,
    CONSTRAINT vocabulary_publish_batch_items_disposition_check CHECK ((disposition = ANY (ARRAY['INSERT'::text, 'SKIP_DUPLICATE'::text, 'BLOCKED'::text])))
);


--
-- Name: vocabulary_publish_batches; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_publish_batches (
    batch_id character varying(36) NOT NULL,
    fingerprint character(64) NOT NULL,
    status text NOT NULL,
    created_by integer NOT NULL,
    confirmed_by integer,
    preview jsonb NOT NULL,
    inserted_count integer DEFAULT 0 NOT NULL,
    skipped_count integer DEFAULT 0 NOT NULL,
    blocked_count integer DEFAULT 0 NOT NULL,
    error_code text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    confirmed_at timestamp with time zone,
    completed_at timestamp with time zone,
    CONSTRAINT vocabulary_publish_batches_status_check CHECK ((status = ANY (ARRAY['PREVIEWED'::text, 'PUBLISHING'::text, 'COMPLETED'::text, 'FAILED'::text])))
);


--
-- Name: vocabulary_review_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_review_audit (
    audit_id bigint NOT NULL,
    candidate_id character varying(80),
    actor_user_id integer NOT NULL,
    action text NOT NULL,
    before_value jsonb,
    after_value jsonb,
    batch_id character varying(36),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT vocabulary_review_audit_action_check CHECK ((action = ANY (ARRAY['EDIT'::text, 'APPROVE'::text, 'REJECT'::text, 'RESET_PENDING'::text, 'BULK_APPROVE'::text, 'BULK_REJECT'::text, 'BULK_RESET_PENDING'::text, 'PREVIEW'::text, 'PUBLISH'::text, 'PUBLISH_SKIPPED'::text])))
);


--
-- Name: vocabulary_review_audit_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vocabulary_review_audit_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vocabulary_review_audit_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vocabulary_review_audit_audit_id_seq OWNED BY public.vocabulary_review_audit.audit_id;


--
-- Name: vocabulary_review_candidates; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_review_candidates (
    candidate_id character varying(80) NOT NULL,
    english text NOT NULL,
    vietnamese text NOT NULL,
    pronunciation text,
    part_of_speech character varying(30),
    cefr character varying(2),
    primary_topic_code character varying(50),
    topic_codes text[] DEFAULT ARRAY[]::text[] NOT NULL,
    example text,
    example_vi text,
    source_code character varying(80) NOT NULL,
    source_name text NOT NULL,
    source_url text NOT NULL,
    source_version text NOT NULL,
    source_license text NOT NULL,
    source_record_id text NOT NULL,
    source_word_id bigint,
    source_definition_id bigint,
    cefr_scope text NOT NULL,
    validation_reasons text[] DEFAULT ARRAY[]::text[] NOT NULL,
    quality_score integer DEFAULT 0 NOT NULL,
    quality_verified boolean DEFAULT false NOT NULL,
    license_status text NOT NULL,
    status text NOT NULL,
    reviewed_by integer,
    reviewed_at timestamp with time zone,
    published_flashcard_id integer,
    row_version integer DEFAULT 1 NOT NULL,
    original_payload jsonb NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT vocabulary_review_candidates_cefr_check CHECK (((cefr)::text = ANY (ARRAY[('A1'::character varying)::text, ('A2'::character varying)::text, ('B1'::character varying)::text, ('B2'::character varying)::text]))),
    CONSTRAINT vocabulary_review_candidates_license_status_check CHECK ((license_status = ANY (ARRAY['ATTRIBUTION_REQUIRED'::text, 'VERIFIED'::text, 'BLOCKED'::text]))),
    CONSTRAINT vocabulary_review_candidates_status_check CHECK ((status = ANY (ARRAY['PENDING'::text, 'APPROVED'::text, 'REJECTED'::text, 'PUBLISHED'::text])))
);


--
-- Name: vocabulary_review_topics; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_review_topics (
    code character varying(50) NOT NULL,
    name character varying(100) NOT NULL,
    description text,
    source_payload jsonb DEFAULT '{}'::jsonb NOT NULL
);


--
-- Name: vocabulary_sources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.vocabulary_sources (
    id integer NOT NULL,
    code character varying(80) NOT NULL,
    name text NOT NULL,
    version text NOT NULL,
    source_url text NOT NULL,
    license text NOT NULL,
    checksum_sha256 character(64) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: vocabulary_sources_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.vocabulary_sources_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: vocabulary_sources_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.vocabulary_sources_id_seq OWNED BY public.vocabulary_sources.id;


--
-- Name: xp_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.xp_history (
    id integer NOT NULL,
    user_id integer NOT NULL,
    flashcard_id integer,
    quiz_result_id integer,
    source_type character varying(20) NOT NULL,
    xp_amount integer NOT NULL,
    activity_date date NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    quiz_action_id uuid,
    quiz_action_operation text,
    CONSTRAINT xp_history_review_has_no_quiz CHECK ((((source_type)::text <> 'REVIEW'::text) OR (quiz_result_id IS NULL))),
    CONSTRAINT xp_history_source_type_check CHECK (((source_type)::text = ANY (ARRAY[('QUIZ'::character varying)::text, ('REVIEW'::character varying)::text]))),
    CONSTRAINT xp_history_xp_amount_check CHECK ((xp_amount > 0))
);


--
-- Name: xp_history_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.xp_history_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: xp_history_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.xp_history_id_seq OWNED BY public.xp_history.id;


--
-- Name: flashcards id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcards ALTER COLUMN id SET DEFAULT nextval('public.flashcards_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: pgmigrations id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pgmigrations ALTER COLUMN id SET DEFAULT nextval('public.pgmigrations_id_seq'::regclass);


--
-- Name: quiz_results id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results ALTER COLUMN id SET DEFAULT nextval('public.quiz_results_id_seq'::regclass);


--
-- Name: topics id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics ALTER COLUMN id SET DEFAULT nextval('public.topics_id_seq'::regclass);


--
-- Name: user_auth_identities id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auth_identities ALTER COLUMN id SET DEFAULT nextval('public.user_auth_identities_id_seq'::regclass);


--
-- Name: user_progress id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_progress ALTER COLUMN id SET DEFAULT nextval('public.user_progress_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: vocabulary_review_audit audit_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_audit ALTER COLUMN audit_id SET DEFAULT nextval('public.vocabulary_review_audit_audit_id_seq'::regclass);


--
-- Name: vocabulary_sources id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_sources ALTER COLUMN id SET DEFAULT nextval('public.vocabulary_sources_id_seq'::regclass);


--
-- Name: xp_history id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history ALTER COLUMN id SET DEFAULT nextval('public.xp_history_id_seq'::regclass);


--
-- Name: flashcard_provenance flashcard_provenance_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_provenance
    ADD CONSTRAINT flashcard_provenance_pkey PRIMARY KEY (flashcard_id, source_id);


--
-- Name: flashcard_provenance flashcard_provenance_source_id_source_record_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_provenance
    ADD CONSTRAINT flashcard_provenance_source_id_source_record_id_key UNIQUE (source_id, source_record_id);


--
-- Name: flashcard_topics flashcard_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_topics
    ADD CONSTRAINT flashcard_topics_pkey PRIMARY KEY (flashcard_id, topic_id);


--
-- Name: flashcards flashcards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcards
    ADD CONSTRAINT flashcards_pkey PRIMARY KEY (id);


--
-- Name: google_auth_challenges google_auth_challenges_nonce_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.google_auth_challenges
    ADD CONSTRAINT google_auth_challenges_nonce_hash_key UNIQUE (nonce_hash);


--
-- Name: google_auth_challenges google_auth_challenges_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.google_auth_challenges
    ADD CONSTRAINT google_auth_challenges_pkey PRIMARY KEY (id);


--
-- Name: learning_system_config learning_system_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.learning_system_config
    ADD CONSTRAINT learning_system_config_pkey PRIMARY KEY (singleton);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_hash_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash);


--
-- Name: pgmigrations pgmigrations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pgmigrations
    ADD CONSTRAINT pgmigrations_pkey PRIMARY KEY (id);


--
-- Name: quiz_actions_v2 quiz_actions_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_actions_v2
    ADD CONSTRAINT quiz_actions_v2_pkey PRIMARY KEY (id);


--
-- Name: quiz_actions_v2 quiz_actions_v2_session_id_action_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_actions_v2
    ADD CONSTRAINT quiz_actions_v2_session_id_action_id_key UNIQUE (session_id, action_id);


--
-- Name: quiz_actions_v2 quiz_actions_v2_user_id_id_operation_flashcard_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_actions_v2
    ADD CONSTRAINT quiz_actions_v2_user_id_id_operation_flashcard_id_key UNIQUE (user_id, id, operation, flashcard_id);


--
-- Name: quiz_questions_v2 quiz_questions_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_pkey PRIMARY KEY (id);


--
-- Name: quiz_questions_v2 quiz_questions_v2_session_id_flashcard_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_session_id_flashcard_id_key UNIQUE (session_id, flashcard_id);


--
-- Name: quiz_questions_v2 quiz_questions_v2_session_id_ordinal_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_session_id_ordinal_key UNIQUE (session_id, ordinal);


--
-- Name: quiz_questions_v2 quiz_questions_v2_session_id_user_id_id_flashcard_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_session_id_user_id_id_flashcard_id_key UNIQUE (session_id, user_id, id, flashcard_id);


--
-- Name: quiz_results quiz_results_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT quiz_results_pkey PRIMARY KEY (id);


--
-- Name: quiz_sessions_v2 quiz_sessions_v2_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_sessions_v2
    ADD CONSTRAINT quiz_sessions_v2_id_user_id_key UNIQUE (id, user_id);


--
-- Name: quiz_sessions_v2 quiz_sessions_v2_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_sessions_v2
    ADD CONSTRAINT quiz_sessions_v2_pkey PRIMARY KEY (id);


--
-- Name: quiz_sessions_v2 quiz_sessions_v2_user_id_creation_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_sessions_v2
    ADD CONSTRAINT quiz_sessions_v2_user_id_creation_id_key UNIQUE (user_id, creation_id);


--
-- Name: topics topics_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_code_key UNIQUE (code);


--
-- Name: topics topics_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_name_key UNIQUE (name);


--
-- Name: topics topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.topics
    ADD CONSTRAINT topics_pkey PRIMARY KEY (id);


--
-- Name: user_auth_identities user_auth_identities_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auth_identities
    ADD CONSTRAINT user_auth_identities_pkey PRIMARY KEY (id);


--
-- Name: user_auth_identities user_auth_identities_provider_subject_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auth_identities
    ADD CONSTRAINT user_auth_identities_provider_subject_unique UNIQUE (provider, provider_subject);


--
-- Name: user_auth_identities user_auth_identities_user_provider_unique; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auth_identities
    ADD CONSTRAINT user_auth_identities_user_provider_unique UNIQUE (user_id, provider);


--
-- Name: user_learned_flashcards user_learned_flashcards_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learned_flashcards
    ADD CONSTRAINT user_learned_flashcards_pkey PRIMARY KEY (user_id, flashcard_id);


--
-- Name: user_learning_daily user_learning_daily_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learning_daily
    ADD CONSTRAINT user_learning_daily_pkey PRIMARY KEY (user_id, learning_date);


--
-- Name: user_learning_settings user_learning_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learning_settings
    ADD CONSTRAINT user_learning_settings_pkey PRIMARY KEY (user_id);


--
-- Name: user_progress user_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_progress
    ADD CONSTRAINT user_progress_pkey PRIMARY KEY (id);


--
-- Name: user_scores user_scores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_scores
    ADD CONSTRAINT user_scores_pkey PRIMARY KEY (user_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: vocabulary_publish_batch_items vocabulary_publish_batch_items_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batch_items
    ADD CONSTRAINT vocabulary_publish_batch_items_pkey PRIMARY KEY (batch_id, candidate_id);


--
-- Name: vocabulary_publish_batches vocabulary_publish_batches_fingerprint_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batches
    ADD CONSTRAINT vocabulary_publish_batches_fingerprint_key UNIQUE (fingerprint);


--
-- Name: vocabulary_publish_batches vocabulary_publish_batches_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batches
    ADD CONSTRAINT vocabulary_publish_batches_pkey PRIMARY KEY (batch_id);


--
-- Name: vocabulary_review_audit vocabulary_review_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_audit
    ADD CONSTRAINT vocabulary_review_audit_pkey PRIMARY KEY (audit_id);


--
-- Name: vocabulary_review_candidates vocabulary_review_candidates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_candidates
    ADD CONSTRAINT vocabulary_review_candidates_pkey PRIMARY KEY (candidate_id);


--
-- Name: vocabulary_review_candidates vocabulary_review_candidates_source_code_source_record_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_candidates
    ADD CONSTRAINT vocabulary_review_candidates_source_code_source_record_id_key UNIQUE (source_code, source_record_id);


--
-- Name: vocabulary_review_topics vocabulary_review_topics_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_topics
    ADD CONSTRAINT vocabulary_review_topics_pkey PRIMARY KEY (code);


--
-- Name: vocabulary_sources vocabulary_sources_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_sources
    ADD CONSTRAINT vocabulary_sources_code_key UNIQUE (code);


--
-- Name: vocabulary_sources vocabulary_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_sources
    ADD CONSTRAINT vocabulary_sources_pkey PRIMARY KEY (id);


--
-- Name: xp_history xp_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history
    ADD CONSTRAINT xp_history_pkey PRIMARY KEY (id);


--
-- Name: xp_history xp_history_v2_link; Type: CHECK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE public.xp_history
    ADD CONSTRAINT xp_history_v2_link CHECK ((((quiz_action_id IS NULL) AND (quiz_action_operation IS NULL)) OR ((quiz_action_id IS NOT NULL) AND (quiz_action_operation IS NOT NULL) AND (quiz_action_operation = 'ANSWER'::text) AND (user_id IS NOT NULL) AND (flashcard_id IS NOT NULL) AND ((source_type)::text = 'QUIZ'::text) AND (quiz_result_id IS NULL)))) NOT VALID;


--
-- Name: idx_flashcard_topics_one_primary; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_flashcard_topics_one_primary ON public.flashcard_topics USING btree (flashcard_id) WHERE is_primary;


--
-- Name: idx_flashcard_topics_topic_card; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flashcard_topics_topic_card ON public.flashcard_topics USING btree (topic_id, flashcard_id);


--
-- Name: idx_flashcards_english_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flashcards_english_lower ON public.flashcards USING btree (lower((english)::text));


--
-- Name: idx_flashcards_level_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flashcards_level_active ON public.flashcards USING btree (level, id) WHERE (is_active = true);


--
-- Name: idx_flashcards_topic_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flashcards_topic_id ON public.flashcards USING btree (topic_id);


--
-- Name: idx_flashcards_unique_word_meaning; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_flashcards_unique_word_meaning ON public.flashcards USING btree (lower((english)::text), lower((vietnamese)::text));


--
-- Name: idx_flashcards_vietnamese_lower; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_flashcards_vietnamese_lower ON public.flashcards USING btree (lower((vietnamese)::text));


--
-- Name: idx_google_auth_challenges_expires_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_google_auth_challenges_expires_at ON public.google_auth_challenges USING btree (expires_at);


--
-- Name: idx_password_reset_tokens_user_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_password_reset_tokens_user_active ON public.password_reset_tokens USING btree (user_id, expires_at) WHERE (used_at IS NULL);


--
-- Name: idx_quiz_results_flashcard_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quiz_results_flashcard_id ON public.quiz_results USING btree (flashcard_id);


--
-- Name: idx_quiz_results_user_answered_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quiz_results_user_answered_at ON public.quiz_results USING btree (user_id, answered_at DESC);


--
-- Name: idx_quiz_results_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quiz_results_user_id ON public.quiz_results USING btree (user_id);


--
-- Name: idx_user_auth_identities_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_auth_identities_user_id ON public.user_auth_identities USING btree (user_id);


--
-- Name: idx_user_learned_flashcards_flashcard; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_learned_flashcards_flashcard ON public.user_learned_flashcards USING btree (flashcard_id, user_id);


--
-- Name: idx_user_learning_daily_user_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_learning_daily_user_date ON public.user_learning_daily USING btree (user_id, learning_date DESC);


--
-- Name: idx_user_progress_due; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_progress_due ON public.user_progress USING btree (user_id, next_review, flashcard_id) WHERE (review_count > 0);


--
-- Name: idx_user_progress_flashcard_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_progress_flashcard_id ON public.user_progress USING btree (flashcard_id);


--
-- Name: idx_user_progress_user_flashcard_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_user_progress_user_flashcard_unique ON public.user_progress USING btree (user_id, flashcard_id);


--
-- Name: idx_user_progress_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_progress_user_id ON public.user_progress USING btree (user_id);


--
-- Name: idx_user_scores_total_xp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_user_scores_total_xp ON public.user_scores USING btree (total_xp DESC);


--
-- Name: idx_users_email_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_users_email_unique ON public.users USING btree (lower((email)::text));


--
-- Name: idx_users_username_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_users_username_unique ON public.users USING btree (lower((username)::text));


--
-- Name: idx_vocabulary_review_audit_candidate; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_audit_candidate ON public.vocabulary_review_audit USING btree (candidate_id, created_at DESC);


--
-- Name: idx_vocabulary_review_english; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_english ON public.vocabulary_review_candidates USING btree (lower(english));


--
-- Name: idx_vocabulary_review_filters; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_filters ON public.vocabulary_review_candidates USING btree (cefr, primary_topic_code, part_of_speech);


--
-- Name: idx_vocabulary_review_reasons; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_reasons ON public.vocabulary_review_candidates USING gin (validation_reasons);


--
-- Name: idx_vocabulary_review_status_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_status_id ON public.vocabulary_review_candidates USING btree (status, candidate_id);


--
-- Name: idx_vocabulary_review_vietnamese; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vocabulary_review_vietnamese ON public.vocabulary_review_candidates USING btree (lower(vietnamese));


--
-- Name: idx_xp_history_daily_award_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_xp_history_daily_award_unique ON public.xp_history USING btree (user_id, source_type, flashcard_id, activity_date) WHERE (flashcard_id IS NOT NULL);


--
-- Name: idx_xp_history_flashcard_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_xp_history_flashcard_id ON public.xp_history USING btree (flashcard_id);


--
-- Name: idx_xp_history_quiz_result_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX idx_xp_history_quiz_result_unique ON public.xp_history USING btree (quiz_result_id) WHERE (quiz_result_id IS NOT NULL);


--
-- Name: idx_xp_history_user_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_xp_history_user_created_at ON public.xp_history USING btree (user_id, created_at DESC);


--
-- Name: idx_xp_history_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_xp_history_user_id ON public.xp_history USING btree (user_id);


--
-- Name: quiz_actions_v2_one_answer; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX quiz_actions_v2_one_answer ON public.quiz_actions_v2 USING btree (session_id, question_id) WHERE (operation = 'ANSWER'::text);


--
-- Name: quiz_sessions_v2_user_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX quiz_sessions_v2_user_created ON public.quiz_sessions_v2 USING btree (user_id, created_at DESC);


--
-- Name: xp_history_quiz_action_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX xp_history_quiz_action_unique ON public.xp_history USING btree (quiz_action_id) WHERE (quiz_action_id IS NOT NULL);


--
-- Name: quiz_actions_v2 quiz_v2_action_binding; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER quiz_v2_action_binding BEFORE INSERT OR DELETE OR UPDATE ON public.quiz_actions_v2 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_action_binding();


--
-- Name: quiz_sessions_v2 quiz_v2_creation_snapshot; Type: TRIGGER; Schema: public; Owner: -
--

CREATE CONSTRAINT TRIGGER quiz_v2_creation_snapshot AFTER INSERT ON public.quiz_sessions_v2 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_creation_snapshot();


--
-- Name: quiz_actions_v2 quiz_v2_final_receipt; Type: TRIGGER; Schema: public; Owner: -
--

CREATE CONSTRAINT TRIGGER quiz_v2_final_receipt AFTER INSERT OR UPDATE ON public.quiz_actions_v2 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_final_receipt();


--
-- Name: quiz_questions_v2 quiz_v2_question_snapshot; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER quiz_v2_question_snapshot BEFORE INSERT OR DELETE OR UPDATE ON public.quiz_questions_v2 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_question_snapshot();


--
-- Name: quiz_sessions_v2 quiz_v2_session_transition; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER quiz_v2_session_transition BEFORE INSERT OR DELETE OR UPDATE ON public.quiz_sessions_v2 FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_session_transition();


--
-- Name: xp_history quiz_v2_xp_binding; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER quiz_v2_xp_binding BEFORE INSERT OR DELETE OR UPDATE ON public.xp_history FOR EACH ROW EXECUTE FUNCTION public.quiz_v2_xp_binding();


--
-- Name: flashcard_provenance flashcard_provenance_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_provenance
    ADD CONSTRAINT flashcard_provenance_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: flashcard_provenance flashcard_provenance_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_provenance
    ADD CONSTRAINT flashcard_provenance_source_id_fkey FOREIGN KEY (source_id) REFERENCES public.vocabulary_sources(id) ON DELETE RESTRICT;


--
-- Name: flashcard_topics flashcard_topics_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_topics
    ADD CONSTRAINT flashcard_topics_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: flashcard_topics flashcard_topics_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcard_topics
    ADD CONSTRAINT flashcard_topics_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.topics(id) ON DELETE RESTRICT;


--
-- Name: flashcards flashcards_topic_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flashcards
    ADD CONSTRAINT flashcards_topic_id_fkey FOREIGN KEY (topic_id) REFERENCES public.topics(id) ON DELETE CASCADE;


--
-- Name: password_reset_tokens password_reset_tokens_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quiz_actions_v2 quiz_actions_v2_session_id_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_actions_v2
    ADD CONSTRAINT quiz_actions_v2_session_id_user_id_fkey FOREIGN KEY (session_id, user_id) REFERENCES public.quiz_sessions_v2(id, user_id);


--
-- Name: quiz_actions_v2 quiz_actions_v2_session_id_user_id_question_id_flashcard_i_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_actions_v2
    ADD CONSTRAINT quiz_actions_v2_session_id_user_id_question_id_flashcard_i_fkey FOREIGN KEY (session_id, user_id, question_id, flashcard_id) REFERENCES public.quiz_questions_v2(session_id, user_id, id, flashcard_id);


--
-- Name: quiz_questions_v2 quiz_questions_v2_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id);


--
-- Name: quiz_questions_v2 quiz_questions_v2_session_id_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_questions_v2
    ADD CONSTRAINT quiz_questions_v2_session_id_user_id_fkey FOREIGN KEY (session_id, user_id) REFERENCES public.quiz_sessions_v2(id, user_id);


--
-- Name: quiz_results quiz_results_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT quiz_results_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: quiz_results quiz_results_selected_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT quiz_results_selected_flashcard_id_fkey FOREIGN KEY (selected_flashcard_id) REFERENCES public.flashcards(id) ON DELETE SET NULL;


--
-- Name: quiz_results quiz_results_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_results
    ADD CONSTRAINT quiz_results_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: quiz_sessions_v2 quiz_sessions_v2_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quiz_sessions_v2
    ADD CONSTRAINT quiz_sessions_v2_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: user_auth_identities user_auth_identities_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_auth_identities
    ADD CONSTRAINT user_auth_identities_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_learned_flashcards user_learned_flashcards_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learned_flashcards
    ADD CONSTRAINT user_learned_flashcards_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: user_learned_flashcards user_learned_flashcards_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learned_flashcards
    ADD CONSTRAINT user_learned_flashcards_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_learning_daily user_learning_daily_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learning_daily
    ADD CONSTRAINT user_learning_daily_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_learning_settings user_learning_settings_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_learning_settings
    ADD CONSTRAINT user_learning_settings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_progress user_progress_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_progress
    ADD CONSTRAINT user_progress_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE CASCADE;


--
-- Name: user_progress user_progress_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_progress
    ADD CONSTRAINT user_progress_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: user_scores user_scores_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_scores
    ADD CONSTRAINT user_scores_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: vocabulary_publish_batch_items vocabulary_publish_batch_items_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batch_items
    ADD CONSTRAINT vocabulary_publish_batch_items_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.vocabulary_publish_batches(batch_id) ON DELETE RESTRICT;


--
-- Name: vocabulary_publish_batch_items vocabulary_publish_batch_items_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batch_items
    ADD CONSTRAINT vocabulary_publish_batch_items_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.vocabulary_review_candidates(candidate_id) ON DELETE RESTRICT;


--
-- Name: vocabulary_publish_batch_items vocabulary_publish_batch_items_duplicate_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batch_items
    ADD CONSTRAINT vocabulary_publish_batch_items_duplicate_flashcard_id_fkey FOREIGN KEY (duplicate_flashcard_id) REFERENCES public.flashcards(id) ON DELETE RESTRICT;


--
-- Name: vocabulary_publish_batches vocabulary_publish_batches_confirmed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batches
    ADD CONSTRAINT vocabulary_publish_batches_confirmed_by_fkey FOREIGN KEY (confirmed_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: vocabulary_publish_batches vocabulary_publish_batches_created_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_publish_batches
    ADD CONSTRAINT vocabulary_publish_batches_created_by_fkey FOREIGN KEY (created_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: vocabulary_review_audit vocabulary_review_audit_actor_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_audit
    ADD CONSTRAINT vocabulary_review_audit_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: vocabulary_review_audit vocabulary_review_audit_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_audit
    ADD CONSTRAINT vocabulary_review_audit_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.vocabulary_publish_batches(batch_id) ON DELETE RESTRICT;


--
-- Name: vocabulary_review_audit vocabulary_review_audit_candidate_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_audit
    ADD CONSTRAINT vocabulary_review_audit_candidate_id_fkey FOREIGN KEY (candidate_id) REFERENCES public.vocabulary_review_candidates(candidate_id) ON DELETE RESTRICT;


--
-- Name: vocabulary_review_candidates vocabulary_review_candidates_published_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_candidates
    ADD CONSTRAINT vocabulary_review_candidates_published_flashcard_id_fkey FOREIGN KEY (published_flashcard_id) REFERENCES public.flashcards(id) ON DELETE RESTRICT;


--
-- Name: vocabulary_review_candidates vocabulary_review_candidates_reviewed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.vocabulary_review_candidates
    ADD CONSTRAINT vocabulary_review_candidates_reviewed_by_fkey FOREIGN KEY (reviewed_by) REFERENCES public.users(id) ON DELETE RESTRICT;


--
-- Name: xp_history xp_history_flashcard_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history
    ADD CONSTRAINT xp_history_flashcard_id_fkey FOREIGN KEY (flashcard_id) REFERENCES public.flashcards(id) ON DELETE SET NULL;


--
-- Name: xp_history xp_history_quiz_result_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history
    ADD CONSTRAINT xp_history_quiz_result_id_fkey FOREIGN KEY (quiz_result_id) REFERENCES public.quiz_results(id) ON DELETE SET NULL;


--
-- Name: xp_history xp_history_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history
    ADD CONSTRAINT xp_history_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id) ON DELETE CASCADE;


--
-- Name: xp_history xp_history_v2_owner_answer_fk; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.xp_history
    ADD CONSTRAINT xp_history_v2_owner_answer_fk FOREIGN KEY (user_id, quiz_action_id, quiz_action_operation, flashcard_id) REFERENCES public.quiz_actions_v2(user_id, id, operation, flashcard_id) NOT VALID;


--
-- PostgreSQL database dump complete
--

\unrestrict rZ94hW2EYQeqgbaa4FCWLNo06L9G3McD14kjGGzIQxIRotUBfBWKWufYC1H5nBR

