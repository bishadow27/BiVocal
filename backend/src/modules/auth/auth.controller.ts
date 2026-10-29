import type { RequestHandler } from 'express';
import { AppError } from '../../shared/errors/app-error.js';
import { forgotPasswordSchema, googleAuthSchema, googleLinkSchema, loginSchema, registerSchema,
  resetPasswordSchema } from './auth.schema.js';
import type { AuthService } from './auth.service.js';
import { SlidingWindowRateLimiter } from './rate-limiter.js';

export function createAuthController(service: AuthService): {
  register: RequestHandler; login: RequestHandler; me: RequestHandler;
  googleChallenge: RequestHandler; google: RequestHandler; linkGoogle: RequestHandler;
  forgotPassword: RequestHandler; resetPassword: RequestHandler;
} {
  const challengeLimiter = new SlidingWindowRateLimiter(10, 15 * 60_000);
  const googleLimiter = new SlidingWindowRateLimiter(10, 15 * 60_000);
  const forgotIpLimiter = new SlidingWindowRateLimiter(5, 15 * 60_000);
  const forgotEmailLimiter = new SlidingWindowRateLimiter(3, 15 * 60_000);
  const resetLimiter = new SlidingWindowRateLimiter(8, 15 * 60_000);
  return {
    async register(request, response) {
      const parsed = registerSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid registration data');
      response.status(201).json(await service.register(parsed.data));
    },
    async login(request, response) {
      const parsed = loginSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid login data');
      response.json(await service.login(parsed.data));
    },
    async me(_request, response) {
      const id: unknown = response.locals.userId;
      if (typeof id !== 'number') throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
      response.json(await service.currentUser(id));
    },
    async googleChallenge(request, response) {
      challengeLimiter.consume(`challenge:${request.ip}`);
      response.json(await service.createGoogleChallenge());
    },
    async google(request, response) {
      googleLimiter.consume(`google:${request.ip}`);
      const parsed = googleAuthSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid Google authentication data');
      response.json(await service.googleLogin(parsed.data));
    },
    async linkGoogle(request, response) {
      googleLimiter.consume(`link:${request.ip}`);
      const parsed = googleLinkSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid Google linking data');
      const id: unknown = response.locals.userId;
      if (typeof id !== 'number') throw new AppError(401, 'AUTH_TOKEN_INVALID', 'Invalid or expired authentication token');
      response.json(await service.linkGoogle(id, parsed.data));
    },
    async forgotPassword(request, response) {
      forgotIpLimiter.consume(`forgot-ip:${request.ip}`);
      const parsed = forgotPasswordSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid email');
      forgotEmailLimiter.consume(`forgot-email:${parsed.data.email}`);
      response.json(await service.forgotPassword(parsed.data));
    },
    async resetPassword(request, response) {
      resetLimiter.consume(`reset:${request.ip}`);
      const parsed = resetPasswordSchema.safeParse(request.body);
      if (!parsed.success) throw new AppError(400, 'VALIDATION_ERROR', 'Invalid password reset data');
      response.json(await service.resetPassword(parsed.data));
    },
  };
}
