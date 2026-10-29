import type { RequestHandler, Response } from 'express';
import { AppError } from '../../shared/errors/app-error.js';
import { answerSchema, questionsQuerySchema, historyQuerySchema, serialIdSchema } from './quiz.schema.js';
import type { QuizService } from './quiz.service.js';

function currentUserId(response: Response): number {
  const id: unknown = response.locals.userId;
  if (typeof id !== 'number' || !Number.isInteger(id) || id < 1 || id > 2147483647) {
    throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
  }
  return id;
}
export function createQuizController(service: QuizService): {
  questions: RequestHandler; answer: RequestHandler; history: RequestHandler; summary: RequestHandler;
} {
  return {
    async questions(request, response) {
      const query = questionsQuerySchema.safeParse(request.query);
      if (!query.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid quiz query');
      response.json(await service.questions(query.data.topicCode, query.data.limit));
    },
    async answer(request, response) {
      const body = answerSchema.safeParse(request.body);
      if (!body.success) {
        // Missing fields/extra fields are schema errors; supplied invalid IDs have a specific code.
        if (typeof request.body === 'object' && request.body !== null) {
          const input: Record<string, unknown> = request.body;
          if (('flashcardId' in input && !serialIdSchema.safeParse(input.flashcardId).success) ||
              ('selectedFlashcardId' in input && !serialIdSchema.safeParse(input.selectedFlashcardId).success)) {
            throw new AppError(400, 'INVALID_FLASHCARD_ID', 'Invalid flashcard id');
          }
        }
        throw new AppError(400, 'VALIDATION_ERROR', 'Invalid quiz answer input');
      }
      response.json(await service.answer(currentUserId(response), body.data.flashcardId, body.data.selectedFlashcardId));
    },
    async history(request, response) {
      const query = historyQuerySchema.safeParse(request.query);
      if (!query.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid quiz history query');
      response.json(await service.history(currentUserId(response), query.data.limit));
    },
    async summary(_request, response) {
      response.json(await service.summary(currentUserId(response)));
    },
  };
}
