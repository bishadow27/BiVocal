import type { RequestHandler, Response } from 'express';
import { AppError } from '../../shared/errors/app-error.js';
import { flashcardIdSchema, reviewSchema, favoriteSchema, dueQuerySchema } from './progress.schema.js';
import type { ProgressService } from './progress.service.js';

function currentUserId(response: Response): number {
  const id: unknown = response.locals.userId;
  if (typeof id !== 'number' || !Number.isInteger(id) || id < 1 || id > 2147483647) {
    throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
  }
  return id;
}

function flashcardId(value: unknown): number {
  const parsed = flashcardIdSchema.safeParse(value);
  if (!parsed.success) throw new AppError(400, 'INVALID_FLASHCARD_ID', 'Invalid flashcard id');
  return parsed.data;
}

export function createProgressController(service: ProgressService): {
  list: RequestHandler; summary: RequestHandler; due: RequestHandler;
  schedule: RequestHandler; detail: RequestHandler;
  review: RequestHandler; favorite: RequestHandler;
} {
  return {
    async list(_request, response) {
      response.json(await service.list(currentUserId(response)));
    },
    async summary(_request, response) {
      response.json(await service.summary(currentUserId(response)));
    },
    async due(request, response) {
      const query = dueQuerySchema.safeParse(request.query);
      if (!query.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid due query');
      response.json(await service.due(currentUserId(response), query.data.limit ?? 100, query.data.topicCode));
    },
    async schedule(_request, response) {
      response.json(await service.schedule(currentUserId(response)));
    },
    async detail(request, response) {
      response.json(await service.detail(currentUserId(response), flashcardId(request.params.flashcardId)));
    },
    async review(request, response) {
      const id = flashcardId(request.params.flashcardId);
      const body = reviewSchema.safeParse(request.body);
      if (!body.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid review input');
      response.json(await service.review(currentUserId(response), id, body.data.correct));
    },
    async favorite(request, response) {
      const id = flashcardId(request.params.flashcardId);
      const body = favoriteSchema.safeParse(request.body);
      if (!body.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid favorite input');
      response.json(await service.favorite(currentUserId(response), id, body.data.isFavorite));
    },
  };
}
