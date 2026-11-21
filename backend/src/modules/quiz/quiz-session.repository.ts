import type { Pool, PoolClient } from "pg";
import { randomUUID, createHash } from "node:crypto";
import { AppError } from "../../shared/errors/app-error.js";
import { upsertProgressReview } from "../progress/progress.repository.js";
import { toProgress } from "../progress/progress.service.js";
import { recordDailyLearning } from "../learning/learning.repository.js";
import { awardSessionXp } from "./quiz-session.xp.js";
import {
  availability,
  generate,
  type Card,
  type Method,
} from "./quiz-session.generator.js";

export interface Filter {
  topicCode?: string | undefined;
  posCode?: string | undefined;
}
export interface CreateInput {
  creationId: string;
  method: Method;
  filter: Filter;
  selectedUnitCount: number;
  useAllAvailable?: boolean | undefined;
  availabilityVersion?: string | undefined;
}
export interface AnswerInput {
  actionId: string;
  questionId: string;
  selectedOptionId: string;
  expectedVersion: number;
}
export interface ControlInput {
  actionId: string;
  expectedVersion: number;
}
type Json = Record<string, unknown>;
interface Session {
  id: string;
  user_id: number;
  created_at: Date;
  finished_at: Date | null;
  creation_hash: string;
  creation_receipt: Json;
  method: Method;
  unit: string;
  selected_unit_count: number;
  state: string;
  finish_reason: string | null;
  version: number;
  answered_count: number;
  correct_count: number;
  awarded_xp: number;
  configuration: Json;
}
interface Question {
  id: string;
  session_id: string;
  ordinal: number;
  kind: string;
  flashcard_id: number;
  public_payload: { prompt: string; metadata: Json };
  options: Array<{ optionId: string; text: string }>;
  correct_option_id: string;
  grading_payload: {
    source?: { example: string | null; exampleVi: string | null };
  };
  round_index: number | null;
  round_position: number | null;
}
function fail(code: string, message: string, status = 409): never {
  throw new AppError(status, code, message);
}
function canonical(value: unknown): string {
  if (Array.isArray(value)) return "[" + value.map(canonical).join(",") + "]";
  if (value && typeof value === "object")
    return (
      "{" +
      Object.entries(value)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([k, v]) => JSON.stringify(k) + ":" + canonical(v))
        .join(",") +
      "}"
    );
  return JSON.stringify(value) ?? "null";
}
const digest = (v: unknown) =>
  createHash("sha256").update(canonical(v)).digest("hex");
const eligible = `(NOT EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id)
 OR EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id AND p.validation_status IN('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE))`;
export function createQuizSessionRepository(
  pool: Pool,
  writeGuard: (client: PoolClient) => Promise<void>,
) {
  async function cards(db: Pool | PoolClient, filter: Filter): Promise<Card[]> {
    const code = filter.topicCode ?? filter.posCode ?? null;
    if (
      code &&
      !(await db.query("SELECT id FROM topics WHERE code=$1", [code])).rows
        .length
    )
      fail("TOPIC_NOT_FOUND", "Topic not found", 404);
    return (
      await db.query<Card>(
        `SELECT f.id,f.english,f.vietnamese,f.example,f.example_vi,f.pronunciation,f.part_of_speech,f.level,${eligible} AS eligible,
   COALESCE(array_agg(DISTINCT t.code ORDER BY t.code) FILTER(WHERE t.code IS NOT NULL),ARRAY[]::text[]) AS topic_codes
   FROM flashcards f LEFT JOIN flashcard_topics ft ON ft.flashcard_id=f.id LEFT JOIN topics t ON t.id=ft.topic_id
   WHERE f.is_active=TRUE AND($1::text IS NULL OR EXISTS(SELECT 1 FROM flashcard_topics f2 JOIN topics t2 ON t2.id=f2.topic_id WHERE f2.flashcard_id=f.id AND t2.code=$1))
   GROUP BY f.id ORDER BY f.id`,
        [code],
      )
    ).rows;
  }
  async function transaction<T>(
    work: (client: PoolClient) => Promise<T>,
  ): Promise<T> {
    const client = await pool.connect();
    let discard = false;
    try {
      await writeGuard(client);
      await client.query("BEGIN");
      const result = await work(client);
      await client.query("COMMIT");
      return result;
    } catch (error) {
      try {
        await client.query("ROLLBACK");
      } catch {
        discard = true;
      }
      throw error;
    } finally {
      client.release(discard);
    }
  }
  async function owned(
    db: Pool | PoolClient,
    user: number,
    id: string,
    lock = false,
  ) {
    const row = (
      await db.query<Session>(
        `SELECT * FROM quiz_sessions_v2 WHERE id=$1 AND user_id=$2${lock ? " FOR UPDATE" : ""}`,
        [id, user],
      )
    ).rows[0];
    if (!row) fail("SESSION_NOT_FOUND", "Session not found", 404);
    return row;
  }
  async function replay(
    client: PoolClient,
    id: string,
    key: string,
    hash: string,
  ) {
    const row = (
      await client.query<{ request_hash: string; response_snapshot: Json }>(
        "SELECT request_hash,response_snapshot FROM quiz_actions_v2 WHERE session_id=$1 AND action_id=$2",
        [id, key],
      )
    ).rows[0];
    if (!row) return null;
    if (row.request_hash !== hash)
      fail(
        "ACTION_PAYLOAD_MISMATCH",
        "Action ID already used with different payload",
      );
    return row.response_snapshot;
  }
  return {
    async availability(method: Method, filter: Filter) {
      return availability(await cards(pool, filter), method);
    },
    async create(user: number, input: CreateInput) {
      return transaction(async (client) => {
        const requestHash = digest({
          method: input.method,
          filter: input.filter,
          selectedUnitCount: input.selectedUnitCount,
          useAllAvailable: input.useAllAvailable ?? false,
          availabilityVersion: input.availabilityVersion ?? null,
        });
        // Transaction-scoped database lock, not process memory: serializes the same owner/creation ID.
        await client.query(
          "SELECT pg_advisory_xact_lock(hashtextextended($1,0))",
          [user + ":" + input.creationId],
        );
        const existing = (
          await client.query<Session>(
            "SELECT * FROM quiz_sessions_v2 WHERE user_id=$1 AND creation_id=$2",
            [user, input.creationId],
          )
        ).rows[0];
        if (existing) {
          if (existing.creation_hash !== requestHash)
            fail(
              "CREATION_PAYLOAD_MISMATCH",
              "Creation ID already used with different configuration",
            );
          return { receipt: existing.creation_receipt, replayed: true };
        }
        // Shared source locks keep the count/start snapshot consistent until COMMIT.
        await client.query(
          "LOCK TABLE flashcards,flashcard_topics,flashcard_provenance,topics IN SHARE MODE",
        );
        const source = await cards(client, input.filter),
          capacity = availability(source, input.method);
        if (
          input.availabilityVersion &&
          input.availabilityVersion !== capacity.availabilityVersion
        )
          fail("AVAILABILITY_CHANGED", "Availability changed");
        if (
          input.useAllAvailable &&
          (capacity.quizEligibleCount >= 20 ||
            capacity.quizEligibleCount !== input.selectedUnitCount)
        )
          fail(
            "AVAILABILITY_CHANGED",
            "Fallback count must match availability",
          );
        const questions = generate(
            source,
            input.method,
            input.selectedUnitCount,
          ),
          id = randomUUID(),
          unit = input.method === "MATCHING" ? "PAIR" : "QUESTION";
        const receipt = {
          sessionId: id,
          creationId: input.creationId,
          method: input.method,
          unit,
          selectedUnitCount: questions.length,
          state: "ACTIVE",
          finishReason: null,
          version: 0,
          questionsUrl: `/api/v1/quiz/sessions/${id}/questions`,
        };
        const savedCreation = await client.query<{ creation_receipt: Json }>(
          "INSERT INTO quiz_sessions_v2(id,user_id,creation_id,creation_hash,creation_receipt,method,unit,configuration,selected_unit_count)VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING creation_receipt",
          [
            id,
            user,
            input.creationId,
            requestHash,
            receipt,
            input.method,
            unit,
            {
              filter: input.filter,
              availabilityVersion: capacity.availabilityVersion,
            },
            questions.length,
          ],
        );
        for (const q of questions)
          await client.query(
            "INSERT INTO quiz_questions_v2(id,session_id,user_id,ordinal,kind,flashcard_id,round_index,round_position,public_payload,options,correct_option_id,grading_payload)VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12)",
            [
              q.id,
              id,
              user,
              q.ordinal,
              q.kind,
              q.cardId,
              q.roundIndex,
              q.roundPosition,
              { prompt: q.prompt, metadata: q.metadata },
              JSON.stringify(q.options),
              q.correctOptionId,
              {
                source: {
                  example: source.find((c) => c.id === q.cardId)!.example,
                  exampleVi:
                    source.find((c) => c.id === q.cardId)!.example_vi ?? null,
                },
              },
            ],
          );
        return {
          receipt: savedCreation.rows[0]!.creation_receipt,
          replayed: false,
        };
      });
    },
    async resume(user: number, id: string) {
      const client = await pool.connect();
      try {
        await client.query("BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY");
        const s = await owned(client, user, id);
        const receipts = (
          await client.query<{ response_snapshot: Json }>(
            "SELECT response_snapshot FROM quiz_actions_v2 WHERE session_id=$1 AND user_id=$2 ORDER BY session_version,created_at,id",
            [id, user],
          )
        ).rows.map((r) => r.response_snapshot);
        const result = {
          sessionId: s.id,
          startedAt: s.created_at.toISOString(),
          finishedAt: s.finished_at?.toISOString() ?? null,
          method: s.method,
          unit: s.unit,
          selectedUnitCount: s.selected_unit_count,
          state: s.state,
          finishReason: s.finish_reason,
          version: s.version,
          answeredCount: s.answered_count,
          correctCount: s.correct_count,
          awardedXp: s.awarded_xp,
          configuration: s.configuration,
          receipts,
        };
        await client.query("COMMIT");
        return result;
      } catch (error) {
        await client.query("ROLLBACK");
        throw error;
      } finally {
        client.release();
      }
    },
    async questions(user: number, id: string, cursor = 0, limit = 100) {
      await owned(pool, user, id);
      const rows = (
        await pool.query<Question & { answered: boolean }>(
          "SELECT q.*,EXISTS(SELECT 1 FROM quiz_actions_v2 a WHERE a.session_id=q.session_id AND a.question_id=q.id AND a.operation='ANSWER') AS answered FROM quiz_questions_v2 q WHERE q.session_id=$1 AND q.user_id=$2 AND q.ordinal>=$3 ORDER BY q.ordinal LIMIT $4",
          [id, user, cursor, limit],
        )
      ).rows;
      return rows.map((q) => ({
        sessionId: id,
        questionId: q.id,
        ordinal: q.ordinal,
        kind: q.kind,
        prompt: q.public_payload.prompt,
        metadata: q.public_payload.metadata,
        options: q.options,
        roundIndex: q.round_index,
        roundPosition: q.round_position,
        answered: q.answered,
      }));
    },
    async answer(user: number, id: string, input: AnswerInput) {
      return transaction(async (client) => {
        const s = await owned(client, user, id, true),
          requestHash = digest({
            operation: "ANSWER",
            sessionId: id,
            ...input,
          });
        const old = await replay(client, id, input.actionId, requestHash);
        if (old) return { receipt: old, replayed: true };
        if (s.version !== input.expectedVersion)
          fail("SESSION_VERSION_CONFLICT", "Session version changed");
        if (s.state !== "ACTIVE") fail("SESSION_TERMINAL", "Session has ended");
        const q = (
          await client.query<Question>(
            "SELECT * FROM quiz_questions_v2 WHERE id=$1 AND session_id=$2 AND user_id=$3",
            [input.questionId, id, user],
          )
        ).rows[0];
        if (!q) fail("QUESTION_NOT_FOUND", "Question not found", 404);
        if (
          (
            await client.query(
              "SELECT id FROM quiz_actions_v2 WHERE session_id=$1 AND question_id=$2 AND operation='ANSWER'",
              [id, q.id],
            )
          ).rows.length
        )
          fail("QUESTION_ALREADY_ANSWERED", "Question already answered");
        if (
          s.method === "MATCHING"
            ? q.round_index !== Math.floor(s.answered_count / 4)
            : q.ordinal !== s.answered_count
        )
          fail("QUESTION_OUT_OF_ORDER", "Answer the current unit");
        const chosen = q.options.find(
          (o) => o.optionId === input.selectedOptionId,
        );
        if (!chosen)
          fail(
            "INVALID_QUIZ_OPTION",
            "Option does not belong to question",
            400,
          );
        const active = await client.query(
          `SELECT f.id FROM flashcards f WHERE f.id=$1 AND f.is_active=TRUE AND ${eligible} FOR SHARE`,
          [q.flashcard_id],
        );
        if (!active.rows.length)
          fail("QUESTION_WITHDRAWN", "Question is no longer eligible");
        const correct = q.correct_option_id === chosen.optionId,
          progressRow = await upsertProgressReview(
            client,
            user,
            q.flashcard_id,
            correct,
          );
        if (!progressRow)
          fail("QUESTION_WITHDRAWN", "Progress update unavailable");
        const progress = toProgress(progressRow);
        const next = s.answered_count + 1,
          reason =
            s.method === "CHALLENGE" && !correct
              ? "INCORRECT"
              : next === s.selected_unit_count
                ? "COMPLETED_ALL"
                : null,
          state = reason ? "COMPLETED" : "ACTIVE",
          version = s.version + 1,
          actionRowId = randomUUID();
        const inserted = (
          await client.query<{ answered_at: string }>(
            `INSERT INTO quiz_actions_v2(id,session_id,user_id,action_id,operation,question_id,flashcard_id,selected_option_id,correct,progress_snapshot,awarded_xp,session_version,session_state,finish_reason,request_hash,response_snapshot)
    VALUES($1,$2,$3,$4,'ANSWER',$5,$6,$7,$8,$9,0,$10,$11,$12,$13,'{}') RETURNING to_char(created_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') AS answered_at`,
            [
              actionRowId,
              id,
              user,
              input.actionId,
              q.id,
              q.flashcard_id,
              chosen.optionId,
              correct,
              progress,
              version,
              state,
              reason,
              requestHash,
            ],
          )
        ).rows[0]!;
        const awardedXp = await awardSessionXp(
          client,
          user,
          q.flashcard_id,
          actionRowId,
          correct,
        );
        await recordDailyLearning(client, user, q.flashcard_id, "QUIZ");
        await client.query(
          "UPDATE quiz_sessions_v2 SET version=$2,answered_count=$3,correct_count=correct_count+$4,awarded_xp=awarded_xp+$5,state=$6,finish_reason=$7,finished_at=CASE WHEN $7::text IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END WHERE id=$1",
          [id, version, next, correct ? 1 : 0, awardedXp, state, reason],
        );
        const receipt = {
          receiptId: actionRowId,
          actionId: input.actionId,
          sessionId: id,
          questionId: q.id,
          operation: "ANSWER",
          acknowledged: true,
          correct,
          correctAnswer: q.options.find(
            (o) => o.optionId === q.correct_option_id,
          )!,
          selectedAnswer: chosen,
          source: q.grading_payload.source ?? null,
          progress: { srsUpdated: true, current: progress },
          awardedXp,
          answeredAt: inserted.answered_at,
          sessionState: state,
          finishReason: reason,
          sessionVersion: version,
        };
        const saved = await client.query<{ response_snapshot: Json }>(
          "UPDATE quiz_actions_v2 SET awarded_xp=$2,response_snapshot=$3 WHERE id=$1 RETURNING response_snapshot",
          [actionRowId, awardedXp, receipt],
        );
        return { receipt: saved.rows[0]!.response_snapshot, replayed: false };
      });
    },
    async control(
      user: number,
      id: string,
      operation: "COMPLETE" | "ABANDON",
      input: ControlInput,
    ) {
      return transaction(async (client) => {
        const s = await owned(client, user, id, true),
          requestHash = digest({ operation, sessionId: id, ...input });
        const old = await replay(client, id, input.actionId, requestHash);
        if (old) return { receipt: old, replayed: true };
        if (s.version !== input.expectedVersion)
          fail("SESSION_VERSION_CONFLICT", "Session version changed");
        if (operation === "COMPLETE" && s.state === "ACTIVE")
          fail("SESSION_INCOMPLETE", "Session has unanswered units");
        if (
          operation === "COMPLETE" &&
          (s.state !== "COMPLETED" || s.finish_reason !== "COMPLETED_ALL")
        )
          fail("INVALID_TERMINAL_REASON", "Session did not complete all units");
        if (operation === "ABANDON" && s.state !== "ACTIVE")
          fail("SESSION_TERMINAL", "Session has ended");
        const version = s.version + (operation === "ABANDON" ? 1 : 0),
          state = operation === "ABANDON" ? "ABANDONED" : "COMPLETED",
          reason = operation === "ABANDON" ? "USER_ABANDONED" : "COMPLETED_ALL";
        if (operation === "ABANDON")
          await client.query(
            "UPDATE quiz_sessions_v2 SET state='ABANDONED',finish_reason='USER_ABANDONED',version=$2,finished_at=CURRENT_TIMESTAMP WHERE id=$1",
            [id, version],
          );
        const rowId = randomUUID(),
          receipt = {
            receiptId: rowId,
            actionId: input.actionId,
            sessionId: id,
            operation,
            acknowledged: true,
            sessionState: state,
            finishReason: reason,
            sessionVersion: version,
            acknowledgedAt: "",
          };
        const ack = (
          await client.query<{ at: string }>(
            `INSERT INTO quiz_actions_v2(id,session_id,user_id,action_id,operation,session_version,session_state,finish_reason,request_hash,response_snapshot)VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10) RETURNING to_char(created_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.US"Z"') AS at`,
            [
              rowId,
              id,
              user,
              input.actionId,
              operation,
              version,
              state,
              reason,
              requestHash,
              receipt,
            ],
          )
        ).rows[0]!;
        receipt.acknowledgedAt = ack.at;
        const saved = await client.query<{ response_snapshot: Json }>(
          "UPDATE quiz_actions_v2 SET response_snapshot=$2 WHERE id=$1 RETURNING response_snapshot",
          [rowId, receipt],
        );
        return { receipt: saved.rows[0]!.response_snapshot, replayed: false };
      });
    },
  };
}
export type QuizSessionRepository = ReturnType<
  typeof createQuizSessionRepository
>;
