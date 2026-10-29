import type { Role } from './admin.schema.js';
export interface AdminAccount {
  id: number; username: string; email: string; full_name: string | null; avatar_url: string | null;
  role: Role; created_at: Date; last_login: Date | null;
}
export interface AdminUserRow extends AdminAccount {
  total_xp: number; current_streak: number; longest_streak: number;
}
export interface TopicRow {
  id: number; code: string; name: string; description: string | null; image_url: string | null; flashcard_count: string;
}
export interface CardRow {
  id: number; topic_id: number; topic_code: string; topic_name: string; english: string; vietnamese: string;
  pronunciation: string | null; part_of_speech: string | null; example: string | null; example_vi: string | null;
  level: string | null; is_active: boolean | null; created_at: Date | null; updated_at: Date | null;
}
export interface AdminProgressRow {
  flashcard_id: number; english: string; vietnamese: string; topic_code: string; memory_level: number;
  review_count: number; correct_count: number; wrong_count: number; is_favorite: boolean;
  last_review: Date | null; next_review: Date | null;
}
export interface AdminQuizRow {
  id: number; flashcard_id: number; question: string; selected_answer: string | null;
  correct_answer: string; is_correct: boolean; answered_at: Date;
}
export interface QuizAggregate { total_answers: string; correct_answers: string; wrong_answers: string }
export interface DayRow { date: string; answers: string; correct: string; wrong: string }
export interface TopicStatRow extends QuizAggregate { code: string; name: string }
export interface PageRows<T> { rows: T[]; total: string }
