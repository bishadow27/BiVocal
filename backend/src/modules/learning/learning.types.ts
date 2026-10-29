export type LearningSource = 'REVIEW' | 'QUIZ';

export interface LearningGoalDto { dailyGoal: number }
export interface LearningTodayDto {
  date: string; dailyGoal: number; newWordsLearned: number; remaining: number;
  completionPercent: number; reviewCount: number; dueCount: number; completed: boolean;
}
export interface LearningHistoryDayDto {
  date: string; goal: number | null; newWords: number; reviews: number;
  status: 'COMPLETED' | 'NOT_COMPLETED' | 'NO_ACTIVITY' | 'NO_DATA';
}
export interface LearningCardRow {
  id: number; english: string; vietnamese: string; pronunciation: string | null;
  part_of_speech: string | null; example: string | null; example_vi: string | null;
  level: string | null;
}
export interface LearningCardDto {
  id: number; english: string; vietnamese: string; pronunciation: string | null;
  partOfSpeech: string | null; example: string | null; exampleVi: string | null;
  level: string | null;
}
