import type { RequestHandler } from 'express';
import type { AdminService } from './admin.service.js';
import { AppError } from '../../shared/errors/app-error.js';

export function requireAdmin(service: AdminService): RequestHandler {
  return async (_request,response,next) => {
    const id: unknown=response.locals.userId;
    if (typeof id !== 'number' || !Number.isInteger(id) || id < 1 || id > 2147483647) {
      throw new AppError(401,'AUTH_TOKEN_INVALID','Invalid or expired authentication token');
    }
    // A fresh database lookup on EVERY request, never trust/cache a JWT role claim.
    response.locals.adminAccount=await service.authorize(id);
    next();
  };
}
