import type { Request,RequestHandler } from 'express';
import type { z } from 'zod';
import { AppError } from '../../shared/errors/app-error.js';
import * as schema from './vocabulary-review.schema.js';
import type { VocabularyReviewService } from './vocabulary-review.service.js';

function parse<T>(s:z.ZodType<T>,value:unknown):T{const r=s.safeParse(value);if(!r.success)throw new AppError(400,'VALIDATION_ERROR','Invalid vocabulary review request');return r.data;}
const candidate=(req:Request)=>parse(schema.candidateId,req.params.candidateId);
const batch=(req:Request)=>parse(schema.batchId,req.params.batchId);
const actor=(res:Parameters<RequestHandler>[1])=>res.locals.userId as number;
export function createVocabularyReviewController(service:VocabularyReviewService):Record<string,RequestHandler>{return{
  stats:async(_req,res)=>res.json(await service.stats()),filters:async(_req,res)=>res.json(await service.filters()),
  list:async(req,res)=>res.json(await service.list(parse(schema.reviewQuery,req.query))),
  one:async(req,res)=>res.json(await service.one(candidate(req))),
  edit:async(req,res)=>res.json(await service.edit(actor(res),candidate(req),parse(schema.editCandidateBody,req.body))),
  status:async(req,res)=>res.json(await service.status(actor(res),candidate(req),parse(schema.statusBody,req.body))),
  bulk:async(req,res)=>res.json(await service.bulk(actor(res),parse(schema.bulkBody,req.body))),
  preview:async(req,res)=>res.status(201).json(await service.preview(actor(res),parse(schema.previewBody,req.body).candidateIds)),
  publish:async(req,res)=>{parse(schema.confirmBody,req.body);res.json(await service.publish(actor(res),batch(req)));},
  audit:async(req,res)=>{const q=parse(schema.reviewQuery.pick({page:true,limit:true}),req.query);res.json(await service.audit(candidate(req),q.page,q.limit));},
};}

