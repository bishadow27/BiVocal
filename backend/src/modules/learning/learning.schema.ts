import { z } from 'zod';
export const goalSchema=z.object({dailyGoal:z.union([z.literal(5),z.literal(10),z.literal(15),z.literal(20)])}).strict();
export const historySchema=z.object({days:z.string().regex(/^\d+$/).transform(Number).pipe(z.number().int().min(1).max(30)).default(7)}).strict();
export const newCardsSchema=z.object({
  limit:z.string().regex(/^[1-9]\d*$/).transform(Number).pipe(z.number().int().min(1).max(20)).default(10),
  topicCode:z.string().regex(/^[A-Z][A-Z0-9_]{1,31}$/).optional(),
  level:z.enum(['A1','A2','B1','B2','C1','C2']).optional(),
}).strict();
