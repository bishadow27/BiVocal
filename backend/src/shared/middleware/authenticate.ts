import type { RequestHandler } from 'express';
import type { TokenService } from '../../modules/auth/token.service.js';
import { AppError } from '../errors/app-error.js';

export function authenticate(tokens: TokenService): RequestHandler {
  return async (request, response, next) => {
    const header = request.get('authorization');
    if (!header) throw new AppError(401, 'AUTH_TOKEN_MISSING', 'Authentication token required');
    const match = /^Bearer ([^\s]+)$/i.exec(header);
    const token = match?.[1];
    if (!token) throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
    try {
      response.locals.userId = await tokens.verifySession(token);
      next();
    } catch (error) { next(error); }
  };
}
