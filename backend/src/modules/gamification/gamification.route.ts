import { Router } from 'express';
import { authenticate } from '../../shared/middleware/authenticate.js';
import type { TokenService } from '../auth/token.service.js';
import type { GamificationService } from './gamification.service.js';
import { createGamificationController } from './gamification.controller.js';

export function createGamificationRouter(service: GamificationService, tokens: TokenService): Router {
  const router = Router(); const controller = createGamificationController(service);
  router.use(authenticate(tokens));
  router.get('/me', controller.me);
  router.get('/xp-history', controller.history);
  router.get('/leaderboard', controller.leaderboard);
  return router;
}
