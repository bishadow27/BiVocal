import { Router, json } from 'express';
import type { AdminService } from './admin.service.js';
import type { TokenService } from '../auth/token.service.js';
import { authenticate } from '../../shared/middleware/authenticate.js';
import { requireAdmin } from './require-admin.js';
import { createAdminController } from './admin.controller.js';

export function createAdminRouter(service: AdminService,tokens: TokenService): Router {
  const router=Router(); const c=createAdminController(service);
  router.use(authenticate(tokens),requireAdmin(service));
  // Authentication and live role check happen before parsing mutation bodies.
  router.use(json({limit:'32kb'}));
  router.get('/me',c.me!); router.get('/dashboard',c.dashboard!);
  router.get('/topics',c.topics!); router.post('/topics',c.createTopic!); router.put('/topics/:id',c.updateTopic!);
  router.get('/flashcards',c.cards!); router.post('/flashcards',c.createCard!); router.put('/flashcards/:id',c.updateCard!);
  router.patch('/flashcards/:id/status',c.status!);
  router.get('/users',c.users!); router.get('/users/:id',c.user!);
  router.get('/users/:id/progress',c.progress!); router.get('/users/:id/quiz-history',c.quizHistory!);
  router.patch('/users/:id/role',c.setRole!);
  router.get('/stats/quiz',c.stats!); router.get('/leaderboard',c.leaderboard!);
  return router;
}
