import { z } from 'zod';

const topicCode = z.string().regex(/^[A-Z][A-Z0-9_]{0,49}$/);
const queryLimit = (maximum: number, defaultValue: number) => z.string().regex(/^[1-9]\d*$/)
  .transform(Number).pipe(z.number().int().min(1).max(maximum)).optional()
  .transform((value) => value ?? defaultValue);
// Unknown query fields are stripped, never used as user identity.
export const questionsQuerySchema = z.object({ topicCode: topicCode.optional(), limit: queryLimit(20, 10) });
export const historyQuerySchema = z.object({ limit: queryLimit(100, 20) });
export const serialIdSchema = z.number().int().min(1).max(2147483647);
export const answerSchema = z.object({ flashcardId: serialIdSchema, selectedFlashcardId: serialIdSchema }).strict();
