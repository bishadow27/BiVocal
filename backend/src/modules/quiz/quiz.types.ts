import type { ProgressRow, ProgressDto } from '../progress/progress.types.js';

export interface QuizCard {
  id: number;
  topic_id: number;
  english: string;
  vietnamese: string;
  part_of_speech?: string | null;
  level?: string | null;
}
export interface QuizQuestionDto {
  flashcardId: number;
  questionType: 'EN_TO_VI';
  prompt: string;
  options: { flashcardId: number; text: string }[];
}
export interface QuizResultRow {
  id: number;
  flashcard_id: number;
  selected_flashcard_id: number | null;
  is_correct: boolean;
  question_type: 'EN_TO_VI';
  answered_at: Date;
}
export interface QuizHistoryRow extends QuizResultRow {
  question: string;
  correct_answer: string;
  selected_answer: string | null;
}
export interface QuizSummaryRow {
  total_answers: string;
  correct_answers: string;
  wrong_answers: string;
}
export type QuizSubmission = { status: 'question_missing' } | { status: 'option_invalid' } | {
  status: 'answered'; result: QuizResultRow; progress: ProgressRow;
  correctAnswer: string; selectedAnswer: string;
};
export interface QuizAnswerDto {
  resultId: number;
  flashcardId: number;
  selectedFlashcardId: number | null;
  correct: boolean;
  correctAnswer: string;
  selectedAnswer: string;
  answeredAt: string;
  progress: ProgressDto;
}
export interface QuizHistoryDto {
  id: number;
  flashcardId: number;
  question: string;
  selectedFlashcardId: number | null;
  selectedAnswer: string | null;
  correctAnswer: string;
  correct: boolean;
  questionType: 'EN_TO_VI';
  answeredAt: string;
}
export interface QuizSummaryDto {
  totalAnswers: number;
  correctAnswers: number;
  wrongAnswers: number;
  accuracy: number | null;
}
