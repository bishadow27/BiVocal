import { Router, json } from 'express';
import { authenticate } from '../../shared/middleware/authenticate.js';
import type { TokenService } from '../auth/token.service.js';
import type { ProgressService } from './progress.service.js';
import { createProgressController } from './progress.controller.js';

export function createProgressRouter(service: ProgressService, tokens: TokenService): Router {
  const router = Router();
  const controller = createProgressController(service);
  router.use(authenticate(tokens));
  router.get('/', controller.list);
  // Static route before :flashcardId so "summary" is never parsed as an ID.
  router.get('/summary', controller.summary);
  router.get('/due', controller.due);
  router.get('/schedule', controller.schedule);
  router.get('/:flashcardId', controller.detail);
  router.post('/:flashcardId/review', json({ limit: '16kb' }), controller.review);
  router.put('/:flashcardId/favorite', json({ limit: '16kb' }), controller.favorite);
  return router;
}
