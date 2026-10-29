import type { Pool, PoolClient } from 'pg';
import type { DueCardRow, ProgressRow, ProgressSummaryRow, ReviewScheduleRow } from './progress.types.js';
import { recordLearningActivity } from '../gamification/gamification.repository.js';
import type { ActivityRecorder } from '../gamification/gamification.types.js';
import { recordDailyLearning, type DailyLearningRecorder } from '../learning/learning.repository.js';

export interface ProgressRepository {
  activeFlashcardExists(flashcardId: number): Promise<boolean>;
  findByUserAndFlashcard(userId: number, flashcardId: number): Promise<ProgressRow | undefined>;
  listByUser(userId: number): Promise<ProgressRow[]>;
  summaryByUser(userId: number): Promise<ProgressSummaryRow>;
  dueByUser(userId: number, limit: number, topicCode?: string): Promise<DueCardRow[]>;
  scheduleByUser(userId: number): Promise<ReviewScheduleRow>;
  review(userId: number, flashcardId: number, correct: boolean): Promise<ProgressRow | undefined>;
  favorite(userId: number, flashcardId: number, isFavorite: boolean): Promise<ProgressRow | undefined>;
}

// Constant SQL fragments only; all request values use query parameters.
const publicColumns = `p.flashcard_id, p.memory_level, p.review_count, p.correct_count,
  p.wrong_count, p.is_favorite, p.last_review, p.next_review, p.created_at, p.updated_at`;

// The existing memory_level rises/falls by one. Correct answers use the resulting level;
// wrong answers return in ten minutes regardless of level, so forgotten words are prompt.
export const REVIEW_DAYS_BY_LEVEL = [0, 1, 3, 7, 14, 30] as const;
export const WRONG_REVIEW_DELAY_MINUTES = 10;

// Same SQL and rule for the standalone Progress API and a Quiz PoolClient transaction.
export async function upsertProgressReview(database: Pool | PoolClient, userId: number,
  flashcardId: number, correct: boolean): Promise<ProgressRow | undefined> {
  const result = await database.query<ProgressRow>(
    `INSERT INTO user_progress AS p
       (user_id, flashcard_id, memory_level, review_count, correct_count, wrong_count,
        last_review, next_review, updated_at)
     SELECT $1, f.id, CASE WHEN $3::boolean THEN 1 ELSE 0 END, 1,
       CASE WHEN $3::boolean THEN 1 ELSE 0 END,
       CASE WHEN $3::boolean THEN 0 ELSE 1 END, clock_timestamp(),
       clock_timestamp() + CASE WHEN $3::boolean THEN ($4::integer[])[2] * INTERVAL '1 day'
                                ELSE $5::integer * INTERVAL '1 minute' END, clock_timestamp()
     FROM flashcards f WHERE f.id = $2 AND f.is_active = TRUE
     ON CONFLICT (user_id, flashcard_id) DO UPDATE SET
       review_count = p.review_count + 1,
       correct_count = p.correct_count + CASE WHEN $3::boolean THEN 1 ELSE 0 END,
       wrong_count = p.wrong_count + CASE WHEN $3::boolean THEN 0 ELSE 1 END,
       memory_level = CASE WHEN $3::boolean THEN LEAST(p.memory_level + 1, 5)
                           ELSE GREATEST(p.memory_level - 1, 0) END,
       last_review = clock_timestamp(), updated_at = clock_timestamp(),
       next_review = clock_timestamp() + CASE WHEN $3::boolean
         THEN ($4::integer[])[LEAST(p.memory_level + 1, 5) + 1] * INTERVAL '1 day'
         ELSE $5::integer * INTERVAL '1 minute' END
     RETURNING ${publicColumns}`, [userId, flashcardId, correct,
      [...REVIEW_DAYS_BY_LEVEL], WRONG_REVIEW_DELAY_MINUTES],
  );
  return result.rows[0];
}

export function createProgressRepository(pool: Pool, recordActivity: ActivityRecorder = recordLearningActivity,
  recordDaily: DailyLearningRecorder = recordDailyLearning): ProgressRepository {
  return {
    async activeFlashcardExists(flashcardId) {
      const result = await pool.query<{ present: boolean }>(
        'SELECT EXISTS (SELECT 1 FROM flashcards WHERE id = $1 AND is_active = TRUE) AS present',
        [flashcardId],
      );
      return result.rows[0]?.present === true;
    },
    async findByUserAndFlashcard(userId, flashcardId) {
      const result = await pool.query<ProgressRow>(
        `SELECT ${publicColumns} FROM user_progress p
         JOIN flashcards f ON f.id = p.flashcard_id AND f.is_active = TRUE
         WHERE p.user_id = $1 AND p.flashcard_id = $2`, [userId, flashcardId],
      );
      return result.rows[0];
    },
    async listByUser(userId) {
      const result = await pool.query<ProgressRow>(
        `SELECT ${publicColumns} FROM user_progress p
         JOIN flashcards f ON f.id = p.flashcard_id AND f.is_active = TRUE
         WHERE p.user_id = $1 ORDER BY p.updated_at DESC, p.flashcard_id ASC`, [userId],
      );
      return result.rows;
    },
    async summaryByUser(userId) {
      const result = await pool.query<ProgressSummaryRow>(
        `SELECT COUNT(*)::text AS tracked_cards,
           COUNT(*) FILTER (WHERE p.review_count > 0)::text AS reviewed_cards,
           COUNT(*) FILTER (WHERE p.is_favorite = TRUE)::text AS favorite_cards,
           COUNT(*) FILTER (WHERE p.memory_level = 5)::text AS mastered_cards,
           COALESCE(SUM(p.review_count), 0)::text AS total_reviews,
           COALESCE(SUM(p.correct_count), 0)::text AS correct_answers,
           COALESCE(SUM(p.wrong_count), 0)::text AS wrong_answers
         FROM user_progress p
         JOIN flashcards f ON f.id = p.flashcard_id AND f.is_active = TRUE
         WHERE p.user_id = $1`, [userId],
      );
      const row = result.rows[0];
      if (!row) throw new Error('Progress summary returned no row');
      return row;
    },
    async dueByUser(userId, limit, topicCode) {
      const result = await pool.query<DueCardRow>(
        `SELECT f.id, f.english, f.vietnamese, f.pronunciation, f.part_of_speech,
           f.example, f.example_vi, f.level, t.code AS topic_code,
           p.memory_level, p.review_count, p.wrong_count, p.last_review, p.next_review
         FROM user_progress p
         JOIN flashcards f ON f.id = p.flashcard_id AND f.is_active = TRUE
         JOIN topics t ON t.id = f.topic_id
         WHERE p.user_id = $1 AND p.review_count > 0
           AND (p.next_review IS NULL OR p.next_review <= clock_timestamp())
           AND ($2::text IS NULL OR t.code = $2)
         ORDER BY p.next_review ASC NULLS FIRST, p.wrong_count DESC, f.id ASC
         LIMIT $3`, [userId, topicCode ?? null, limit],
      );
      return result.rows;
    },
    async scheduleByUser(userId) {
      const result = await pool.query<ReviewScheduleRow>(
        `SELECT COUNT(*)::text AS tracked_cards,
           COUNT(*) FILTER (WHERE p.review_count > 0)::text AS reviewed_cards,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND
             (p.next_review IS NULL OR p.next_review <= clock_timestamp()))::text AS due_now,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.next_review > clock_timestamp()
             AND p.next_review <= clock_timestamp() + INTERVAL '24 hours')::text AS upcoming_24_hours,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND
             (p.last_review AT TIME ZONE 'UTC')::date = (clock_timestamp() AT TIME ZONE 'UTC')::date)::text AS reviewed_today,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 0)::text AS level_0,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 1)::text AS level_1,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 2)::text AS level_2,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 3)::text AS level_3,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 4)::text AS level_4,
           COUNT(*) FILTER (WHERE p.review_count > 0 AND p.memory_level = 5)::text AS level_5
         FROM user_progress p
         JOIN flashcards f ON f.id = p.flashcard_id AND f.is_active = TRUE
         WHERE p.user_id = $1`, [userId],
      );
      const row = result.rows[0];
      if (!row) throw new Error('Review schedule returned no row');
      return row;
    },
    async review(userId, flashcardId, correct) {
      const client = await pool.connect(); let discard = false;
      try {
        await client.query('BEGIN');
        const progress = await upsertProgressReview(client, userId, flashcardId, correct);
        if (!progress) { await client.query('ROLLBACK'); return undefined; }
        await recordActivity(client, { userId, flashcardId, sourceType: 'REVIEW' });
        await recordDaily(client, userId, flashcardId, 'REVIEW');
        await client.query('COMMIT');
        return progress;
      } catch (error: unknown) {
        try { await client.query('ROLLBACK'); } catch { discard = true; }
        throw error;
      } finally { client.release(discard); }
    },
    async favorite(userId, flashcardId, isFavorite) {
      const result = await pool.query<ProgressRow>(
        `INSERT INTO user_progress AS p (user_id, flashcard_id, is_favorite, updated_at)
         SELECT $1, f.id, $3::boolean, CURRENT_TIMESTAMP
         FROM flashcards f WHERE f.id = $2 AND f.is_active = TRUE
         ON CONFLICT (user_id, flashcard_id) DO UPDATE SET
           is_favorite = EXCLUDED.is_favorite, updated_at = CURRENT_TIMESTAMP
         RETURNING ${publicColumns}`, [userId, flashcardId, isFavorite],
      );
      return result.rows[0];
    },
  };
}
