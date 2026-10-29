import { AppError } from '../../shared/errors/app-error.js';
import type { ProgressRepository } from './progress.repository.js';
import type { DueCardDto, ProgressRow, ProgressDto, ProgressSummaryDto, ReviewScheduleDto } from './progress.types.js';

export function toProgress(row: ProgressRow): ProgressDto {
  return {
    flashcardId: row.flashcard_id, memoryLevel: row.memory_level,
    reviewCount: row.review_count, correctCount: row.correct_count, wrongCount: row.wrong_count,
    isFavorite: row.is_favorite, lastReview: row.last_review?.toISOString() ?? null,
    nextReview: row.next_review?.toISOString() ?? null,
    createdAt: row.created_at.toISOString(), updatedAt: row.updated_at.toISOString(),
  };
}

function flashcardNotFound(): never {
  throw new AppError(404, 'FLASHCARD_NOT_FOUND', 'Flashcard not found');
}

function count(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0) throw new Error('Invalid progress aggregate');
  return parsed;
}

export function createProgressService(repository: ProgressRepository) {
  return {
    async detail(userId: number, flashcardId: number): Promise<ProgressDto> {
      if (!await repository.activeFlashcardExists(flashcardId)) flashcardNotFound();
      const row = await repository.findByUserAndFlashcard(userId, flashcardId);
      if (!row) throw new AppError(404, 'PROGRESS_NOT_FOUND', 'Progress not found');
      return toProgress(row);
    },
    async list(userId: number): Promise<ProgressDto[]> {
      return (await repository.listByUser(userId)).map(toProgress);
    },
    async due(userId: number, limit: number, topicCode?: string): Promise<DueCardDto[]> {
      return (await repository.dueByUser(userId, limit, topicCode)).map(row => ({
        id: row.id, english: row.english, vietnamese: row.vietnamese,
        pronunciation: row.pronunciation, partOfSpeech: row.part_of_speech,
        example: row.example, exampleVi: row.example_vi, level: row.level,
        topicCode: row.topic_code, memoryLevel: row.memory_level,
        reviewCount: row.review_count, wrongCount: row.wrong_count,
        lastReview: row.last_review?.toISOString() ?? null,
        nextReview: row.next_review?.toISOString() ?? null,
      }));
    },
    async schedule(userId: number): Promise<ReviewScheduleDto> {
      const row = await repository.scheduleByUser(userId);
      return {
        trackedCards: count(row.tracked_cards), reviewedCards: count(row.reviewed_cards),
        dueNow: count(row.due_now), upcoming24Hours: count(row.upcoming_24_hours),
        reviewedToday: count(row.reviewed_today),
        memoryLevels: [row.level_0, row.level_1, row.level_2, row.level_3, row.level_4, row.level_5].map(count),
      };
    },
    async summary(userId: number): Promise<ProgressSummaryDto> {
      const row = await repository.summaryByUser(userId);
      const totalReviews = count(row.total_reviews);
      const correctAnswers = count(row.correct_answers);
      return {
        trackedCards: count(row.tracked_cards), reviewedCards: count(row.reviewed_cards),
        favoriteCards: count(row.favorite_cards), masteredCards: count(row.mastered_cards),
        totalReviews, correctAnswers, wrongAnswers: count(row.wrong_answers),
        accuracy: totalReviews === 0 ? null : Math.round(correctAnswers / totalReviews * 10000) / 100,
      };
    },
    async review(userId: number, flashcardId: number, correct: boolean): Promise<ProgressDto> {
      const row = await repository.review(userId, flashcardId, correct);
      if (!row) flashcardNotFound();
      return toProgress(row);
    },
    async favorite(userId: number, flashcardId: number, isFavorite: boolean): Promise<ProgressDto> {
      const row = await repository.favorite(userId, flashcardId, isFavorite);
      if (!row) flashcardNotFound();
      return toProgress(row);
    },
  };
}

export type ProgressService = ReturnType<typeof createProgressService>;
