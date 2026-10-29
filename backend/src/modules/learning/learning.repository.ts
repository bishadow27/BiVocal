import type { Pool, PoolClient } from 'pg';
import type { LearningCardRow, LearningHistoryDayDto, LearningSource, LearningTodayDto } from './learning.types.js';

const LOCAL_DATE = `(clock_timestamp() AT TIME ZONE 'Asia/Ho_Chi_Minh')::date`;

export async function recordDailyLearning(client: PoolClient, userId: number,
  flashcardId: number, source: LearningSource): Promise<boolean> {
  const learned = await client.query<{ inserted: number }>(
    `WITH inserted AS (
       INSERT INTO user_learned_flashcards (user_id, flashcard_id, first_learned_at, first_source)
       VALUES ($1, $2, clock_timestamp(), $3)
       ON CONFLICT (user_id, flashcard_id) DO NOTHING RETURNING 1
     ) SELECT COUNT(*)::int AS inserted FROM inserted`, [userId, flashcardId, source]);
  const isNew = learned.rows[0]?.inserted === 1;
  await client.query(
    `INSERT INTO user_learning_daily AS d
       (user_id, learning_date, goal_snapshot, new_words_count, review_count, updated_at)
     VALUES ($1, ${LOCAL_DATE},
       COALESCE((SELECT daily_goal FROM user_learning_settings WHERE user_id=$1), 10),
       CASE WHEN $2::boolean THEN 1 ELSE 0 END,
       CASE WHEN $2::boolean THEN 0 ELSE 1 END, clock_timestamp())
     ON CONFLICT (user_id, learning_date) DO UPDATE SET
       new_words_count=d.new_words_count + CASE WHEN $2::boolean THEN 1 ELSE 0 END,
       review_count=d.review_count + CASE WHEN $2::boolean THEN 0 ELSE 1 END,
       updated_at=clock_timestamp()`, [userId, isNew]);
  return isNew;
}
export type DailyLearningRecorder = typeof recordDailyLearning;

export interface LearningRepository {
  getGoal(userId: number): Promise<number>;
  updateGoal(userId: number, goal: number): Promise<number>;
  today(userId: number): Promise<LearningTodayDto>;
  history(userId: number, days: number): Promise<LearningHistoryDayDto[]>;
  newCards(userId: number, limit: number, topicCode?: string, level?: string): Promise<LearningCardRow[]>;
}

export function createLearningRepository(pool: Pool): LearningRepository {
  return {
    async getGoal(userId) {
      const result = await pool.query<{ daily_goal: number }>(
        `SELECT COALESCE((SELECT daily_goal FROM user_learning_settings WHERE user_id=$1),10)::int daily_goal`, [userId]);
      return result.rows[0]?.daily_goal ?? 10;
    },
    async updateGoal(userId, goal) {
      const client = await pool.connect(); let discard = false;
      try {
        await client.query('BEGIN');
        await client.query(`INSERT INTO user_learning_settings AS s (user_id,daily_goal,updated_at)
          VALUES ($1,$2,clock_timestamp()) ON CONFLICT(user_id) DO UPDATE
          SET daily_goal=EXCLUDED.daily_goal,updated_at=clock_timestamp()`, [userId, goal]);
        await client.query(`UPDATE user_learning_daily SET goal_snapshot=$2,updated_at=clock_timestamp()
          WHERE user_id=$1 AND learning_date=${LOCAL_DATE}`, [userId, goal]);
        await client.query('COMMIT'); return goal;
      } catch (error) { try { await client.query('ROLLBACK'); } catch { discard = true; } throw error; }
      finally { client.release(discard); }
    },
    async today(userId) {
      const result = await pool.query<{ date:string; daily_goal:number; new_words:number; reviews:number; due_count:number }>(
        `SELECT to_char(${LOCAL_DATE},'YYYY-MM-DD') date,
          COALESCE(d.goal_snapshot,s.daily_goal,10)::int daily_goal,
          COALESCE(d.new_words_count,0)::int new_words,
          COALESCE(d.review_count,0)::int reviews,
          (SELECT COUNT(*)::int FROM user_progress p JOIN flashcards f ON f.id=p.flashcard_id
            WHERE p.user_id=$1 AND p.review_count>0 AND f.is_active=TRUE
              AND (p.next_review IS NULL OR p.next_review<=clock_timestamp())) due_count
         FROM (SELECT $1::integer user_id) u
         LEFT JOIN user_learning_settings s ON s.user_id=u.user_id
         LEFT JOIN user_learning_daily d ON d.user_id=u.user_id AND d.learning_date=${LOCAL_DATE}`, [userId]);
      const row = result.rows[0]!; const remaining = Math.max(0, row.daily_goal-row.new_words);
      return { date: row.date, dailyGoal: row.daily_goal, newWordsLearned: row.new_words, remaining,
        completionPercent: Math.min(100, Math.round(row.new_words/row.daily_goal*100)),
        reviewCount: row.reviews, dueCount: row.due_count, completed: remaining===0 };
    },
    async history(userId, days) {
      const result = await pool.query<{ date:string; goal:number|null; new_words:number; reviews:number; status:LearningHistoryDayDto['status'] }>(
        `WITH dates AS (SELECT generate_series(${LOCAL_DATE}-$2::integer+1,${LOCAL_DATE},INTERVAL '1 day')::date AS learning_date),
         activation AS (SELECT activated_date FROM learning_system_config WHERE singleton=TRUE)
         SELECT to_char(x.learning_date,'YYYY-MM-DD') date,d.goal_snapshot::int goal,
           COALESCE(d.new_words_count,0)::int new_words,COALESCE(d.review_count,0)::int reviews,
           CASE WHEN x.learning_date < a.activated_date THEN 'NO_DATA'
             WHEN d.user_id IS NULL THEN 'NO_ACTIVITY'
             WHEN d.new_words_count>=d.goal_snapshot THEN 'COMPLETED' ELSE 'NOT_COMPLETED' END status
         FROM dates x CROSS JOIN activation a LEFT JOIN user_learning_daily d
           ON d.user_id=$1 AND d.learning_date=x.learning_date ORDER BY x.learning_date`, [userId, days]);
      return result.rows.map(r => ({ date:r.date, goal:r.goal, newWords:r.new_words, reviews:r.reviews, status:r.status }));
    },
    async newCards(userId, limit, topicCode, level) {
      const result = await pool.query<LearningCardRow>(
        `SELECT f.id,f.english,f.vietnamese,f.pronunciation,f.part_of_speech,f.example,f.example_vi,f.level
         FROM flashcards f WHERE f.is_active=TRUE
           AND NOT EXISTS (SELECT 1 FROM user_learned_flashcards l WHERE l.user_id=$1 AND l.flashcard_id=f.id)
           AND ($2::text IS NULL OR EXISTS (SELECT 1 FROM flashcard_topics ft JOIN topics t ON t.id=ft.topic_id
             WHERE ft.flashcard_id=f.id AND t.code=$2))
           AND ($3::text IS NULL OR f.level=$3)
         ORDER BY f.id LIMIT $4`, [userId, topicCode??null, level??null, limit]);
      return result.rows;
    },
  };
}
