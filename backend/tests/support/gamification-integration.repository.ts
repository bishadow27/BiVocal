import type { Pool } from 'pg';
import { createQuizIntegrationRepository } from './quiz-integration.repository.js';

export interface OwnedIdentity { id: number; username: string; email: string }
export interface StoredScore {
  total_xp: number; current_streak: number; longest_streak: number;
  last_activity_date: string | null; created_at: Date; updated_at: Date;
}

// Integration-only SQL. Every mutation targets an exact UUID test identity.
export function createGamificationIntegrationRepository(pool: Pool) {
  return {
    ...createQuizIntegrationRepository(pool),
    async prerequisites() {
      const result = await pool.query<{ present: boolean }>(
        `SELECT to_regclass('users') IS NOT NULL AND to_regclass('user_progress') IS NOT NULL
          AND to_regclass('quiz_results') IS NOT NULL AND to_regclass('user_scores') IS NOT NULL
          AND to_regclass('xp_history') IS NOT NULL AS present`);
      return result.rows[0]?.present === true;
    },
    async utcDay(): Promise<string> {
      const result = await pool.query<{ day: string }>(
        "SELECT to_char((CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date, 'YYYY-MM-DD') AS day");
      return result.rows[0]!.day;
    },
    async score(userId: number): Promise<StoredScore | undefined> {
      const result = await pool.query<StoredScore>(
        `SELECT total_xp, current_streak, longest_streak,
          to_char(last_activity_date, 'YYYY-MM-DD') AS last_activity_date, created_at, updated_at
         FROM user_scores WHERE user_id = $1`, [userId]);
      return result.rows[0];
    },
    async historyCount(userId: number, card: number | null = null, source: 'QUIZ' | 'REVIEW' | null = null) {
      const result = await pool.query<{ count: string }>(
        `SELECT COUNT(*)::text AS count FROM xp_history WHERE user_id = $1
          AND ($2::integer IS NULL OR flashcard_id = $2) AND ($3::text IS NULL OR source_type = $3)`,
        [userId, card, source]);
      return Number(result.rows[0]!.count);
    },
    async setOwnedXp(owner: OwnedIdentity, xp: number) {
      const result = await pool.query(
        `UPDATE user_scores s SET total_xp = $4 FROM users u
         WHERE s.user_id = u.id AND u.id = $1 AND u.username = $2 AND u.email = $3`,
        [owner.id, owner.username, owner.email, xp]);
      if (result.rowCount !== 1) throw new Error('Exact test-owned score not found');
    },
    async setOwnedStreak(owner: OwnedIdentity, daysAgo: number, current: number, longest: number) {
      const result = await pool.query(
        `UPDATE user_scores s SET last_activity_date = (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date - $4::integer,
          current_streak = $5, longest_streak = $6 FROM users u
         WHERE s.user_id = u.id AND u.id = $1 AND u.username = $2 AND u.email = $3`,
        [owner.id, owner.username, owner.email, daysAgo, current, longest]);
      if (result.rowCount !== 1) throw new Error('Exact test-owned streak fixture not found');
    },
    async restoreOwnedScore(owner: OwnedIdentity, score: StoredScore) {
      const result = await pool.query(
        `UPDATE user_scores s SET total_xp = $4, current_streak = $5, longest_streak = $6,
          last_activity_date = $7::date, created_at = $8, updated_at = $9 FROM users u
         WHERE s.user_id = u.id AND u.id = $1 AND u.username = $2 AND u.email = $3`,
        [owner.id, owner.username, owner.email, score.total_xp, score.current_streak,
          score.longest_streak, score.last_activity_date, score.created_at, score.updated_at]);
      if (result.rowCount !== 1) throw new Error('Exact test-owned score restore failed');
    },
  };
}
