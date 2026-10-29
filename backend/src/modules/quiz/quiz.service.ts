import { randomInt } from 'node:crypto';
import { AppError } from '../../shared/errors/app-error.js';
import { toProgress } from '../progress/progress.service.js';
import type { QuizRepository } from './quiz.repository.js';
import type { QuizCard, QuizQuestionDto, QuizAnswerDto, QuizHistoryDto, QuizSummaryDto } from './quiz.types.js';

function shuffle<T>(input: readonly T[]): T[] {
  const result = [...input];
  for (let i = result.length - 1; i > 0; i--) {
    const j = randomInt(i + 1);
    const current = result[i]!;
    result[i] = result[j]!; result[j] = current;
  }
  return result;
}
const normalizedText = (text: string) => text.normalize('NFKC').trim().toLocaleLowerCase('vi')
  .replace(/[“”‘’]/g, "'").replace(/\s+/g, ' ');
const meaningKey = (text: string) => normalizedText(text).replace(/[.!?]+$/g, '');
const englishKey = (text: string) => normalizedText(text);
const metadataKey = (text: string | null | undefined) => normalizedText(text ?? '');

function meaningParts(text: string): Set<string> {
  const normalized = meaningKey(text).replace(/\([^)]*\)/g, ' ');
  return new Set(normalized.split(/[,;/|]+/).map((part) => part.trim()).filter(Boolean));
}

function meaningsConflict(left: string, right: string): boolean {
  const leftKey = meaningKey(left); const rightKey = meaningKey(right);
  if (leftKey === rightKey) return true;
  const leftParts = meaningParts(left); const rightParts = meaningParts(right);
  for (const part of leftParts) if (rightParts.has(part)) return true;
  return false;
}

function distractorScore(question: QuizCard, candidate: QuizCard): number {
  let score = 0;
  const questionPos = metadataKey(question.part_of_speech);
  const candidatePos = metadataKey(candidate.part_of_speech);
  const questionLevel = metadataKey(question.level);
  const candidateLevel = metadataKey(candidate.level);
  if (questionPos && questionPos === candidatePos) score += 2;
  if (questionLevel && questionLevel === candidateLevel) score += 1;
  return score;
}
function count(value: string): number {
  const parsed = Number(value);
  if (!Number.isSafeInteger(parsed) || parsed < 0) throw new Error('Invalid quiz aggregate');
  return parsed;
}

export function createQuizService(repository: QuizRepository) {
  return {
    async questions(topicCode: string | undefined, limit: number): Promise<QuizQuestionDto[]> {
      if (topicCode !== undefined && !await repository.topicExists(topicCode)) {
        throw new AppError(404, 'TOPIC_NOT_FOUND', 'Topic not found');
      }
      const groups = new Map<number, QuizCard[]>();
      for (const card of await repository.activeCards(topicCode)) {
        if (!card.english.trim() || !card.vietnamese.trim()) continue;
        const group = groups.get(card.topic_id) ?? [];
        group.push(card); groups.set(card.topic_id, group);
      }
      // A duplicated English prompt with different meanings is ambiguous without more context.
      // Keep all four options inside the topic so answer validation and presentation agree.
      const candidateOptions = new Map<number, QuizCard[]>();
      for (const group of groups.values()) {
        const englishCounts = new Map<string, number>();
        for (const card of group) englishCounts.set(englishKey(card.english),
          (englishCounts.get(englishKey(card.english)) ?? 0) + 1);
        for (const question of group) {
          if (englishCounts.get(englishKey(question.english)) !== 1) continue;
          const ranked = shuffle(group.filter((candidate) => candidate.id !== question.id
            && !meaningsConflict(question.vietnamese, candidate.vietnamese)))
            .sort((left, right) => distractorScore(question, right) - distractorScore(question, left));
          const distractors: QuizCard[] = [];
          for (const candidate of ranked) {
            if (distractors.some((chosen) => meaningsConflict(chosen.vietnamese, candidate.vietnamese))) continue;
            distractors.push(candidate);
            if (distractors.length === 3) break;
          }
          if (distractors.length === 3) candidateOptions.set(question.id, distractors);
        }
      }
      const seenPrompts = new Set<string>();
      const candidates = shuffle([...candidateOptions.keys()].map((id) =>
        [...groups.values()].flat().find((card) => card.id === id)!)).filter((card) => {
          const key = englishKey(card.english);
          if (seenPrompts.has(key)) return false;
          seenPrompts.add(key); return true;
        });
      if (candidates.length < limit) {
        throw new AppError(400, 'QUIZ_NOT_ENOUGH_FLASHCARDS',
          `Only ${candidates.length} safe quiz question(s) are available; ${limit} requested`);
      }
      return candidates.slice(0, limit).map((question) => {
        const distractors = candidateOptions.get(question.id)!;
        return { flashcardId: question.id, questionType: 'EN_TO_VI', prompt: question.english,
          options: shuffle([question, ...distractors]).map((card) => ({ flashcardId: card.id, text: card.vietnamese })) };
      });
    },
    async answer(userId: number, flashcardId: number, selectedFlashcardId: number): Promise<QuizAnswerDto> {
      const submitted = await repository.answer(userId, flashcardId, selectedFlashcardId);
      if (submitted.status === 'question_missing') {
        throw new AppError(404, 'FLASHCARD_NOT_FOUND', 'Flashcard not found');
      }
      if (submitted.status === 'option_invalid') {
        throw new AppError(400, 'INVALID_QUIZ_OPTION', 'Invalid quiz option');
      }
      return { resultId: submitted.result.id, flashcardId: submitted.result.flashcard_id,
        selectedFlashcardId: submitted.result.selected_flashcard_id, correct: submitted.result.is_correct,
        correctAnswer: submitted.correctAnswer, selectedAnswer: submitted.selectedAnswer,
        answeredAt: submitted.result.answered_at.toISOString(), progress: toProgress(submitted.progress) };
    },
    async history(userId: number, limit: number): Promise<QuizHistoryDto[]> {
      return (await repository.history(userId, limit)).map((row) => ({ id: row.id,
        flashcardId: row.flashcard_id, question: row.question,
        selectedFlashcardId: row.selected_flashcard_id, selectedAnswer: row.selected_answer,
        correctAnswer: row.correct_answer, correct: row.is_correct, questionType: row.question_type,
        answeredAt: row.answered_at.toISOString() }));
    },
    async summary(userId: number): Promise<QuizSummaryDto> {
      const row = await repository.summary(userId);
      const totalAnswers = count(row.total_answers); const correctAnswers = count(row.correct_answers);
      return { totalAnswers, correctAnswers, wrongAnswers: count(row.wrong_answers),
        accuracy: totalAnswers === 0 ? null : Math.round(correctAnswers / totalAnswers * 10000) / 100 };
    },
  };
}
export type QuizService = ReturnType<typeof createQuizService>;
