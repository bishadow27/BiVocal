import type { RequestHandler } from 'express';
import { z } from 'zod';
import { AppError } from '../../shared/errors/app-error.js';
import type { FlashcardService } from './flashcard.service.js';

const codeSchema = z.string().regex(/^[A-Z][A-Z0-9_]{0,49}$/);
// PostgreSQL SERIAL uses positive signed 32-bit integers; do not accept 1abc or 1.5.
const idSchema = z.string().regex(/^[1-9]\d*$/).transform(Number)
  .pipe(z.number().int().min(1).max(2147483647));
const canonicalInteger = z.string().regex(/^[1-9]\d*$/).transform(Number);
const searchSchema = z.object({
  search: z.string().trim().min(1).max(80).optional(),
  topicCode: codeSchema.optional(),
  level: z.enum(['A1', 'A2', 'B1', 'B2']).optional(),
  page: canonicalInteger.pipe(z.number().max(2147483647)).default(1),
  limit: canonicalInteger.pipe(z.number().max(100)).default(20),
}).strict();

export function createFlashcardController(service: FlashcardService): {
  listByTopic: RequestHandler; search: RequestHandler; detail: RequestHandler;
} {
  return {
    async listByTopic(request, response) {
      const parsed = codeSchema.safeParse(request.params.code);
      if (!parsed.success) throw new AppError(400, 'INVALID_TOPIC_CODE', 'Invalid topic code');
      response.json(await service.listByTopic(parsed.data));
    },
    async search(request, response) {
      const parsed = searchSchema.safeParse(request.query);
      if (!parsed.success) throw new AppError(400, 'INVALID_FLASHCARD_QUERY', 'Invalid flashcard query');
      response.json(await service.search({ search: parsed.data.search ?? null,
        topicCode: parsed.data.topicCode ?? null, level: parsed.data.level ?? null,
        page: parsed.data.page, limit: parsed.data.limit }));
    },
    async detail(request, response) {
      const parsed = idSchema.safeParse(request.params.id);
      if (!parsed.success) throw new AppError(400, 'INVALID_FLASHCARD_ID', 'Invalid flashcard id');
      response.json(await service.detail(parsed.data));
    },
  };
}
