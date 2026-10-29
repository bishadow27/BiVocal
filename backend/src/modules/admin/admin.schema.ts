import { z } from 'zod';

export const pathId = z.string().regex(/^[1-9]\d*$/).transform(Number).pipe(z.number().int().min(1).max(2147483647));
const canonical = z.string().regex(/^[1-9]\d*$/).transform(Number).pipe(z.number().int().min(1).max(2147483647));
export const paginationSchema = z.object({ page: canonical.optional().default(1),
  limit: canonical.pipe(z.number().max(100)).optional().default(20) });
const search = z.string().trim().max(200).optional().default('');
// Matches the already-applied dataset CHECK; do not broaden it in application code.
export const topicCode = z.string().trim().max(50).regex(/^[A-Z][A-Z0-9_]*$/);
export const roleSchema = z.enum(['USER', 'ADMIN']);
export const rangeSchema = z.enum(['7d', '30d', 'all']).optional().default('7d');
export const dashboardQuery = z.object({ range: rangeSchema });
export const statsQuery = dashboardQuery.extend({ topicCode: topicCode.optional() });
export const topicsQuery = paginationSchema.extend({ search });
export const usersQuery = topicsQuery.extend({ role: z.enum(['USER', 'ADMIN', 'ALL']).optional().default('ALL') });
export const progressQuery = topicsQuery;
export const cardsQuery = topicsQuery.extend({ topicCode: topicCode.optional(),
  status: z.enum(['ACTIVE', 'INACTIVE', 'ALL']).optional().default('ALL'),
  level: z.enum(['A1', 'A2', 'B1', 'B2']).optional() });
const nullableText = (max: number) => z.string().trim().max(max).nullable().optional().default(null);
const imageUrl = z.url().max(2048).refine((value) => /^https?:\/\//i.test(value)).nullable().optional().default(null);
export const topicBody = z.object({ code: topicCode, name: z.string().trim().min(1).max(100),
  description: nullableText(5000), imageUrl }).strict();
export const cardBody = z.object({ topicCode, english: z.string().trim().min(1).max(150),
  vietnamese: z.string().trim().min(1).max(255), pronunciation: nullableText(150),
  partOfSpeech: z.enum(['noun', 'verb', 'adjective', 'adverb', 'preposition', 'conjunction', 'pronoun', 'phrase'])
    .nullable().optional().default(null), example: nullableText(5000), exampleVi: nullableText(5000),
  level: z.enum(['A1', 'A2', 'B1', 'B2']) }).strict();
export const createCardBody = cardBody.extend({ isActive: z.boolean().optional().default(true) }).strict();
export const statusBody = z.object({ isActive: z.boolean() }).strict();
export const roleBody = z.object({ role: roleSchema }).strict();
export type Pagination = z.infer<typeof paginationSchema>;
export type TopicInput = z.infer<typeof topicBody>;
export type CardInput = z.infer<typeof cardBody>;
export type CreateCardInput = z.infer<typeof createCardBody>;
export type TopicQuery = z.infer<typeof topicsQuery>;
export type CardQuery = z.infer<typeof cardsQuery>;
export type UserQuery = z.infer<typeof usersQuery>;
export type StatsQuery = z.infer<typeof statsQuery>;
export type Role = z.infer<typeof roleSchema>;

export function parseRoleArguments(args: string[]) {
  if (args.length !== 4 || args[0] !== '--email' || args[2] !== '--role') {
    throw new Error('Usage: admin:set-role -- --email existing@example.com --role ADMIN|USER');
  }
  return z.object({ email: z.string().trim().max(150).email().transform((v) => v.toLowerCase()),
    role: roleSchema }).parse({ email: args[1], role: args[3] });
}
