import { Router,json } from 'express';
import { authenticate } from '../../shared/middleware/authenticate.js';
import type { TokenService } from '../auth/token.service.js';
import { createLearningController } from './learning.controller.js';
import type { LearningService } from './learning.service.js';
export function createLearningRouter(service:LearningService,tokens:TokenService):Router{
 const router=Router();const c=createLearningController(service);router.use(authenticate(tokens));
 router.get('/goal',c.goal!);router.put('/goal',json({limit:'4kb'}),c.updateGoal!);router.get('/today',c.today!);
 router.get('/history',c.history!);router.get('/new-cards',c.newCards!);return router;
}
