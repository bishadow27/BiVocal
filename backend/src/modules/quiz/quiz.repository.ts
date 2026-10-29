import type { Pool } from 'pg';
import { upsertProgressReview } from '../progress/progress.repository.js';
import { recordLearningActivity } from '../gamification/gamification.repository.js';
import type { ActivityRecorder } from '../gamification/gamification.types.js';
import type { QuizCard, QuizSubmission, QuizResultRow, QuizHistoryRow, QuizSummaryRow } from './quiz.types.js';
import { recordDailyLearning, type DailyLearningRecorder } from '../learning/learning.repository.js';

export interface QuizRepository {
  topicExists(topicCode: string): Promise<boolean>;
  activeCards(topicCode?: string): Promise<QuizCard[]>;
  answer(userId: number, flashcardId: number, selectedFlashcardId: number): Promise<QuizSubmission>;
  history(userId: number, limit: number): Promise<QuizHistoryRow[]>;
  summary(userId: number): Promise<QuizSummaryRow>;
}

export function createQuizRepository(pool: Pool, recordActivity: ActivityRecorder = recordLearningActivity,
  recordDaily: DailyLearningRecorder = recordDailyLearning): QuizRepository {
  const quizEligible = `(NOT EXISTS (SELECT 1 FROM flashcard_provenance fp_any WHERE fp_any.flashcard_id=f.id)
    OR EXISTS (SELECT 1 FROM flashcard_provenance fp WHERE fp.flashcard_id=f.id
      AND fp.validation_status IN ('APPROVED_AUTO','APPROVED_MANUAL') AND fp.quiz_eligible=TRUE))`;
  return {
    async topicExists(code) {
      const result = await pool.query<{ present: boolean }>(
        'SELECT EXISTS (SELECT 1 FROM topics WHERE code = $1) AS present', [code],
      );
      return result.rows[0]?.present === true;
    },
    async activeCards(code) {
      const result = await pool.query<QuizCard>(
        `SELECT f.id, f.topic_id, f.english, f.vietnamese, f.part_of_speech, f.level
         FROM flashcards f JOIN topics t ON t.id = f.topic_id
         WHERE f.is_active = TRUE AND ${quizEligible}
           AND ($1::text IS NULL OR t.code = $1) ORDER BY f.id`,
        [code ?? null],
      );
      return result.rows;
    },
    async answer(userId, flashcardId, selectedFlashcardId) {
      const client = await pool.connect();
      let discard = false;
      try {
        await client.query('BEGIN');
        // SHARE prevents disable/edit/delete between validation and COMMIT.
        // Deterministic ID order also keeps lock acquisition consistent.
        const cards = await client.query<QuizCard & { is_active: boolean | null }>(
          `SELECT f.id, f.topic_id, f.english, f.vietnamese, f.part_of_speech, f.level, f.is_active FROM flashcards f
           WHERE f.id = ANY($1::integer[]) AND ${quizEligible} ORDER BY f.id FOR SHARE`,
          [[flashcardId, selectedFlashcardId]],
        );
        const question = cards.rows.find((card) => card.id === flashcardId);
        if (!question || question.is_active !== true) {
          await client.query('ROLLBACK'); return { status: 'question_missing' };
        }
        const selected = cards.rows.find((card) => card.id === selectedFlashcardId);
        if (!selected || selected.is_active !== true || selected.topic_id !== question.topic_id) {
          await client.query('ROLLBACK'); return { status: 'option_invalid' };
        }
        const correct = selected.id === question.id;
        const inserted = await client.query<QuizResultRow>(
          `INSERT INTO quiz_results (user_id, flashcard_id, selected_flashcard_id, is_correct)
           VALUES ($1, $2, $3, $4)
           RETURNING id, flashcard_id, selected_flashcard_id, is_correct, question_type, answered_at`,
          [userId, question.id, selected.id, correct],
        );
        const result = inserted.rows[0];
        if (!result) throw new Error('Quiz result insert returned no row');
        const progress = await upsertProgressReview(client, userId, question.id, correct);
        if (!progress) throw new Error('Quiz progress update returned no row');
        await recordActivity(client, { userId, flashcardId: question.id,
          sourceType: 'QUIZ', quizResultId: result.id, correct });
        await recordDaily(client, userId, question.id, 'QUIZ');
        await client.query('COMMIT');
        return { status: 'answered', result, progress,
          correctAnswer: question.vietnamese, selectedAnswer: selected.vietnamese };
      } catch (error: unknown) {
        try { await client.query('ROLLBACK'); } catch { discard = true; }
        throw error;
      } finally {
        client.release(discard);
      }
    },
    async history(userId, limit) {
      const result = await pool.query<QuizHistoryRow>(
        `SELECT q.id, q.flashcard_id, q.selected_flashcard_id, q.is_correct,
           q.question_type, q.answered_at, f.english AS question,
           f.vietnamese AS correct_answer, selected.vietnamese AS selected_answer
         FROM quiz_results q JOIN flashcards f ON f.id = q.flashcard_id
         LEFT JOIN flashcards selected ON selected.id = q.selected_flashcard_id
         WHERE q.user_id = $1 ORDER BY q.answered_at DESC, q.id DESC LIMIT $2`, [userId, limit],
      );
      return result.rows;
    },
    async summary(userId) {
      const result = await pool.query<QuizSummaryRow>(
        `SELECT COUNT(*)::text AS total_answers,
           COUNT(*) FILTER (WHERE is_correct = TRUE)::text AS correct_answers,
           COUNT(*) FILTER (WHERE is_correct = FALSE)::text AS wrong_answers
         FROM quiz_results WHERE user_id = $1`, [userId],
      );
      const row = result.rows[0];
      if (!row) throw new Error('Quiz summary returned no row');
      return row;
    },
  };
}
