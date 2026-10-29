import type { PoolClient } from 'pg';

export type LearningActivity = { userId: number; flashcardId: number } & (
  { sourceType: 'QUIZ'; quizResultId: number; correct: boolean } | { sourceType: 'REVIEW' }
);
export type ActivityRecorder = (client: PoolClient, activity: LearningActivity) => Promise<number>;
export interface ScoreRow {
  total_xp: number;
  current_streak: number;
  longest_streak: number;
  last_activity_date: string | null;
}
export interface XpHistoryRow {
  id: number;
  source_type: 'QUIZ' | 'REVIEW';
  flashcard_id: number | null;
  word: string | null;
  xp_amount: number;
  activity_date: string;
  created_at: Date;
}
export interface LeaderboardRow {
  rank: string;
  user_id: number;
  username: string;
  full_name: string | null;
  avatar_url: string | null;
  total_xp: number;
  current_streak: number;
  longest_streak: number;
}
