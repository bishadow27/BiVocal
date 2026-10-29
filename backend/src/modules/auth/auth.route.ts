import { Router, json } from 'express';
import { authenticate } from '../../shared/middleware/authenticate.js';
import { createAuthController } from './auth.controller.js';
import type { AuthModule } from './auth.service.js';

export function createAuthRouter(auth: AuthModule): Router {
  const router = Router();
  const controller = createAuthController(auth.service);
  router.post('/register', json({ limit: '16kb' }), controller.register);
  router.post('/login', json({ limit: '16kb' }), controller.login);
  router.post('/google/challenge', controller.googleChallenge);
  router.post('/google', json({ limit: '16kb' }), controller.google);
  router.post('/google/link', authenticate(auth.tokens), json({ limit: '16kb' }), controller.linkGoogle);
  router.post('/forgot-password', json({ limit: '4kb' }), controller.forgotPassword);
  router.post('/reset-password', json({ limit: '4kb' }), controller.resetPassword);
  router.get('/me', authenticate(auth.tokens), controller.me);
  return router;
}
