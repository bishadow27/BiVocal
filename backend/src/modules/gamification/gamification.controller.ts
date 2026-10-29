import type { RequestHandler, Response } from 'express';
import { AppError } from '../../shared/errors/app-error.js';
import { limitQuerySchema } from './gamification.schema.js';
import type { GamificationService } from './gamification.service.js';

function currentUser(response: Response): number {
  const userId: unknown = response.locals.userId;
  if (typeof userId !== 'number' || !Number.isInteger(userId) || userId < 1 || userId > 2147483647) {
    throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
  }
  return userId;
}
function limit(query: unknown): number {
  const parsed = limitQuerySchema.safeParse(query);
  if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid gamification query');
  return parsed.data.limit;
}
export function createGamificationController(service: GamificationService): {
  me: RequestHandler; history: RequestHandler; leaderboard: RequestHandler;
} {
  return {
    async me(_request, response) { response.json(await service.me(currentUser(response))); },
    async history(request, response) { response.json(await service.history(currentUser(response), limit(request.query))); },
    async leaderboard(request, response) { response.json(await service.leaderboard(limit(request.query))); },
  };
}
