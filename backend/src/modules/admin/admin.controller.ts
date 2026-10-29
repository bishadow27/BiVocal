import type { Request, RequestHandler } from 'express';
import { z } from 'zod';
import { AppError } from '../../shared/errors/app-error.js';
import type { AdminService } from './admin.service.js';
import * as schema from './admin.schema.js';

function parse<T>(validator: z.ZodType<T>,input: unknown): T {
  const result=validator.safeParse(input);
  if (!result.success) throw new AppError(400,'VALIDATION_ERROR','Invalid admin request');
  return result.data;
}
const id=(request: Request)=>parse(schema.pathId,request.params.id);
export function createAdminController(service: AdminService): Record<string,RequestHandler> {
  return {
    me:async (_req,res)=>{ res.json(res.locals.adminAccount); },
    dashboard:async (req,res)=>{ res.json(await service.dashboard(parse(schema.dashboardQuery,req.query).range)); },
    topics:async (req,res)=>{ res.json(await service.topics(parse(schema.topicsQuery,req.query))); },
    createTopic:async (req,res)=>{ res.status(201).json(await service.createTopic(parse(schema.topicBody,req.body))); },
    updateTopic:async (req,res)=>{ res.json(await service.updateTopic(id(req),parse(schema.topicBody,req.body))); },
    cards:async (req,res)=>{ res.json(await service.cards(parse(schema.cardsQuery,req.query))); },
    createCard:async (req,res)=>{ res.status(201).json(await service.createCard(parse(schema.createCardBody,req.body))); },
    updateCard:async (req,res)=>{ res.json(await service.updateCard(id(req),parse(schema.cardBody,req.body))); },
    status:async (req,res)=>{ res.json(await service.status(id(req),parse(schema.statusBody,req.body).isActive)); },
    users:async (req,res)=>{ res.json(await service.users(parse(schema.usersQuery,req.query))); },
    user:async (req,res)=>{ res.json(await service.user(id(req))); },
    progress:async (req,res)=>{ res.json(await service.progress(id(req),parse(schema.progressQuery,req.query))); },
    quizHistory:async (req,res)=>{ res.json(await service.quizHistory(id(req),parse(schema.paginationSchema,req.query))); },
    setRole:async (req,res)=>{ res.json(await service.setRole(res.locals.userId as number,id(req),parse(schema.roleBody,req.body).role)); },
    stats:async (req,res)=>{ res.json(await service.stats(parse(schema.statsQuery,req.query))); },
    leaderboard:async (req,res)=>{ res.json(await service.leaderboard(parse(schema.paginationSchema,req.query))); },
  };
}
