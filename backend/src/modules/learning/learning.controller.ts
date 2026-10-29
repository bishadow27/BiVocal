import type { RequestHandler,Response } from 'express';
import { AppError } from '../../shared/errors/app-error.js';
import { goalSchema,historySchema,newCardsSchema } from './learning.schema.js';
import type { LearningService } from './learning.service.js';
function userId(response:Response):number { const id:unknown=response.locals.userId;
  if(typeof id!=='number'||!Number.isInteger(id)||id<1) throw new AppError(401,'AUTH_TOKEN_INVALID','Invalid or expired authentication token'); return id; }
export function createLearningController(service:LearningService):Record<string,RequestHandler>{ return {
  goal:async(_q,r)=>r.json(await service.goal(userId(r))),
  updateGoal:async(q,r)=>{const p=goalSchema.safeParse(q.body);if(!p.success)throw new AppError(400,'VALIDATION_ERROR','Invalid daily goal');r.json(await service.updateGoal(userId(r),p.data.dailyGoal));},
  today:async(_q,r)=>r.json(await service.today(userId(r))),
  history:async(q,r)=>{const p=historySchema.safeParse(q.query);if(!p.success)throw new AppError(400,'VALIDATION_ERROR','Invalid history query');r.json(await service.history(userId(r),p.data.days));},
  newCards:async(q,r)=>{const p=newCardsSchema.safeParse(q.query);if(!p.success)throw new AppError(400,'VALIDATION_ERROR','Invalid new cards query');r.json(await service.newCards(userId(r),p.data.limit,p.data.topicCode,p.data.level));},
};}
