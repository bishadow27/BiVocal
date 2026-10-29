import { Router, json } from 'express';
import { authenticate } from '../../shared/middleware/authenticate.js';
import type { TokenService } from '../auth/token.service.js';
import type { QuizService } from './quiz.service.js';
import { createQuizController } from './quiz.controller.js';

export function createQuizRouter(service: QuizService, tokens: TokenService): Router {
  const router = Router();
  const controller = createQuizController(service);
  router.use(authenticate(tokens));
  router.get('/questions', controller.questions);
  router.post('/answer', json({ limit: '16kb' }), controller.answer);
  router.get('/history', controller.history);
  router.get('/summary', controller.summary);
  return router;
}
