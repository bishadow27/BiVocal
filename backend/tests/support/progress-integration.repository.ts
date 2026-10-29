import type { Pool } from 'pg';
import type { ProgressRow } from '../../src/modules/progress/progress.types.js';
import { createAuthIntegrationRepository } from './auth-integration.repository.js';

export interface StoredProgress extends ProgressRow {
  id: number;
  user_id: number;
}

// Integration-only SQL stays in repositories. Cleanup reuses exact ID +
// UUID username + email predicates from the auth integration repository.
export function createProgressIntegrationRepository(pool: Pool) {
  return {
    ...createAuthIntegrationRepository(pool),
    async progressMigrationExists(): Promise<boolean> {
      const result = await pool.query<{ present: boolean }>(
        "SELECT to_regclass('user_progress') IS NOT NULL AS present",
      );
      return result.rows[0]?.present === true;
    },
    async foodFlashcardIds(): Promise<number[]> {
      const result = await pool.query<{ id: number }>(
        `SELECT f.id FROM flashcards f JOIN topics t ON t.id = f.topic_id
         WHERE t.code = 'FOOD' AND f.is_active = TRUE ORDER BY f.id`,
      );
      return result.rows.map((row) => row.id);
    },
    async progressRecord(userId: number, flashcardId: number): Promise<StoredProgress | undefined> {
      const result = await pool.query<StoredProgress>(
        `SELECT id, user_id, flashcard_id, memory_level, review_count, correct_count,
           wrong_count, is_favorite, last_review, next_review, created_at, updated_at
         FROM user_progress WHERE user_id = $1 AND flashcard_id = $2`, [userId, flashcardId],
      );
      return result.rows[0];
    },
    async pairRowCount(userId: number, flashcardId: number): Promise<number> {
      const result = await pool.query<{ count: string }>(
        'SELECT COUNT(*)::text AS count FROM user_progress WHERE user_id = $1 AND flashcard_id = $2',
        [userId, flashcardId],
      );
      return Number(result.rows[0]?.count);
    },
    async userRowCount(userId: number): Promise<number> {
      const result = await pool.query<{ count: string }>(
        'SELECT COUNT(*)::text AS count FROM user_progress WHERE user_id = $1', [userId],
      );
      return Number(result.rows[0]?.count);
    },
  };
}
