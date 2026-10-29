import { z } from 'zod';

export const flashcardIdSchema = z.string().regex(/^[1-9]\d*$/).transform(Number)
  .pipe(z.number().int().min(1).max(2147483647));
export const reviewSchema = z.object({ correct: z.boolean() }).strict();
export const favoriteSchema = z.object({ isFavorite: z.boolean() }).strict();
export const dueQuerySchema = z.object({
  limit: z.string().regex(/^[1-9]\d*$/).transform(Number).pipe(z.number().int().min(1).max(300)).optional(),
  topicCode: z.string().regex(/^[A-Z][A-Z0-9_]{1,31}$/).optional(),
}).strict();
