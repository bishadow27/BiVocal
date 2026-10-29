export interface ProgressRow {
  flashcard_id: number;
  memory_level: number;
  review_count: number;
  correct_count: number;
  wrong_count: number;
  is_favorite: boolean;
  last_review: Date | null;
  next_review: Date | null;
  created_at: Date;
  updated_at: Date;
}

export interface ProgressDto {
  flashcardId: number;
  memoryLevel: number;
  reviewCount: number;
  correctCount: number;
  wrongCount: number;
  isFavorite: boolean;
  lastReview: string | null;
  nextReview: string | null;
  createdAt: string;
  updatedAt: string;
}

// PostgreSQL COUNT and SUM(integer) are bigint, returned as strings by pg.
export interface ProgressSummaryRow {
  tracked_cards: string;
  reviewed_cards: string;
  favorite_cards: string;
  mastered_cards: string;
  total_reviews: string;
  correct_answers: string;
  wrong_answers: string;
}

export interface ProgressSummaryDto {
  trackedCards: number;
  reviewedCards: number;
  favoriteCards: number;
  masteredCards: number;
  totalReviews: number;
  correctAnswers: number;
  wrongAnswers: number;
  accuracy: number | null;
}

export interface DueCardRow {
  id: number;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  part_of_speech: string | null;
  example: string | null;
  example_vi: string | null;
  level: string | null;
  topic_code: string;
  memory_level: number;
  review_count: number;
  wrong_count: number;
  last_review: Date | null;
  next_review: Date | null;
}

export interface DueCardDto {
  id: number;
  english: string;
  vietnamese: string;
  pronunciation: string | null;
  partOfSpeech: string | null;
  example: string | null;
  exampleVi: string | null;
  level: string | null;
  topicCode: string;
  memoryLevel: number;
  reviewCount: number;
  wrongCount: number;
  lastReview: string | null;
  nextReview: string | null;
}

export interface ReviewScheduleRow {
  tracked_cards: string;
  reviewed_cards: string;
  due_now: string;
  upcoming_24_hours: string;
  reviewed_today: string;
  level_0: string;
  level_1: string;
  level_2: string;
  level_3: string;
  level_4: string;
  level_5: string;
}

export interface ReviewScheduleDto {
  trackedCards: number;
  reviewedCards: number;
  dueNow: number;
  upcoming24Hours: number;
  reviewedToday: number;
  memoryLevels: number[];
}
