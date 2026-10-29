import type { Pool } from 'pg';
import { createProgressIntegrationRepository } from './progress-integration.repository.js';
import type { QuizCard, QuizResultRow } from '../../src/modules/quiz/quiz.types.js';

export function createQuizIntegrationRepository(pool: Pool) {
  return {
    ...createProgressIntegrationRepository(pool),
    async quizMigrationExists(): Promise<boolean> {
      const result = await pool.query<{ present: boolean }>(
        "SELECT to_regclass('quiz_results') IS NOT NULL AS present",
      );
      return result.rows[0]?.present === true;
    },
    async foodCards(): Promise<QuizCard[]> {
      const result = await pool.query<QuizCard>(
        `SELECT f.id, f.topic_id, f.english, f.vietnamese FROM flashcards f
         JOIN topics t ON t.id = f.topic_id
         WHERE t.code = 'FOOD' AND f.is_active = TRUE ORDER BY f.id`,
      );
      return result.rows;
    },
    async resultRecord(userId: number, resultId: number) {
      const result = await pool.query<QuizResultRow & { user_id: number }>(
        `SELECT id, user_id, flashcard_id, selected_flashcard_id, is_correct,
           question_type, answered_at FROM quiz_results WHERE user_id = $1 AND id = $2`,
        [userId, resultId],
      );
      return result.rows[0];
    },
    async quizCount(userId: number): Promise<number> {
      const result = await pool.query<{ count: string }>(
        'SELECT COUNT(*)::text AS count FROM quiz_results WHERE user_id = $1', [userId],
      );
      return Number(result.rows[0]?.count);
    },
    // Test-owned data only. Force integer overflow on the subsequent API answer
    // AFTER its result INSERT, proving rollback without a production hook/backdoor.
    async setOwnedCounters(userId: number, username: string, email: string, flashcardId: number,
      reviews: number, correct: number, wrong: number): Promise<void> {
      const result = await pool.query(
        `UPDATE user_progress p SET review_count = $5, correct_count = $6, wrong_count = $7
         FROM users u WHERE p.user_id = u.id AND u.id = $1 AND u.username = $2
           AND u.email = $3 AND p.flashcard_id = $4`,
        [userId, username, email, flashcardId, reviews, correct, wrong],
      );
      if (result.rowCount !== 1) throw new Error('Test-owned progress counter fixture was not found');
    },
  };
}
