import type { Pool } from 'pg';
import type { ActivityRecorder, ScoreRow, XpHistoryRow, LeaderboardRow } from './gamification.types.js';

// Caller owns BEGIN/COMMIT/ROLLBACK. No nested transaction and no client-provided day/XP.
export const recordLearningActivity: ActivityRecorder = async (client, activity) => {
  if (activity.sourceType === 'QUIZ' && (!Number.isInteger(activity.quizResultId) ||
      activity.quizResultId < 1 || activity.quizResultId > 2147483647)) {
    throw new Error('Quiz activity requires a valid result reference');
  }
  const requestedXp = activity.sourceType === 'REVIEW' ? 5 : activity.correct ? 10 : 0;
  let awardedXp = 0;
  if (requestedXp > 0) {
    const inserted = await client.query<{ xp_amount: number }>(
      `INSERT INTO xp_history (user_id, flashcard_id, quiz_result_id, source_type, xp_amount, activity_date)
       VALUES ($1, $2, $3, $4, $5, (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)
       ON CONFLICT DO NOTHING RETURNING xp_amount`,
      [activity.userId, activity.flashcardId, activity.sourceType === 'QUIZ' ? activity.quizResultId : null,
        activity.sourceType, requestedXp],
    );
    awardedXp = inserted.rows[0]?.xp_amount ?? 0;
  }
  // Repeated or older transaction dates never advance/reset a newer streak.
  // CURRENT_TIMESTAMP is the transaction start time, including around midnight.
  const nextStreak = `CASE
    WHEN s.last_activity_date IS NULL THEN 1
    WHEN EXCLUDED.last_activity_date = s.last_activity_date + 1 THEN s.current_streak + 1
    WHEN EXCLUDED.last_activity_date > s.last_activity_date + 1 THEN 1
    ELSE s.current_streak END`;
  await client.query(
    `INSERT INTO user_scores AS s (user_id, total_xp, current_streak, longest_streak, last_activity_date)
     VALUES ($1, $2, 1, 1, (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date)
     ON CONFLICT (user_id) DO UPDATE SET
       total_xp = s.total_xp + EXCLUDED.total_xp,
       current_streak = ${nextStreak},
       longest_streak = GREATEST(s.longest_streak, ${nextStreak}),
       last_activity_date = GREATEST(s.last_activity_date, EXCLUDED.last_activity_date),
       updated_at = CURRENT_TIMESTAMP`, [activity.userId, awardedXp],
  );
  return awardedXp;
};

export interface GamificationRepository {
  findScore(userId: number): Promise<ScoreRow | undefined>;
  history(userId: number, limit: number): Promise<XpHistoryRow[]>;
  leaderboard(limit: number): Promise<LeaderboardRow[]>;
}
export function createGamificationRepository(pool: Pool): GamificationRepository {
  return {
    async findScore(userId) {
      const result = await pool.query<ScoreRow>(
        `SELECT total_xp, current_streak, longest_streak,
           to_char(last_activity_date, 'YYYY-MM-DD') AS last_activity_date
         FROM user_scores WHERE user_id = $1`, [userId],
      );
      return result.rows[0];
    },
    async history(userId, limit) {
      const result = await pool.query<XpHistoryRow>(
        `SELECT h.id, h.source_type, h.flashcard_id, f.english AS word, h.xp_amount,
           to_char(h.activity_date, 'YYYY-MM-DD') AS activity_date, h.created_at
         FROM xp_history h LEFT JOIN flashcards f ON f.id = h.flashcard_id
         WHERE h.user_id = $1 ORDER BY h.created_at DESC, h.id DESC LIMIT $2`, [userId, limit],
      );
      return result.rows;
    },
    async leaderboard(limit) {
      const result = await pool.query<LeaderboardRow>(
        `SELECT (RANK() OVER (ORDER BY s.total_xp DESC))::text AS rank,
           s.user_id, u.username, u.full_name, u.avatar_url, s.total_xp, s.current_streak, s.longest_streak
         FROM user_scores s JOIN users u ON u.id = s.user_id
         WHERE s.total_xp > 0 ORDER BY s.total_xp DESC, s.user_id ASC LIMIT $1`, [limit],
      );
      return result.rows;
    },
  };
}
