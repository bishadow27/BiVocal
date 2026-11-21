import {test} from 'node:test';
import assert from 'node:assert/strict';
import {assertQuizSessionSchema} from '../src/modules/quiz/quiz-session.runtime.js';
test('missing v2 schema is explicit503 and never runs DDL',async()=>{
 const sql:string[]=[];
 await assert.rejects(assertQuizSessionSchema({async query(q:string){sql.push(q);return {rows:[{ready:false}]};}}), (e:any)=>e.status===503&&e.code==='QUIZ_V2_SCHEMA_REQUIRED');
 assert.equal(sql.length,1);assert.match(sql[0]!,/^SELECT/);assert.doesNotMatch(sql[0]!,/INSERT|UPDATE|DELETE|CREATE|ALTER|DROP/);
});
test('ready catalog permits existing repository wiring',async()=>{
 await assertQuizSessionSchema({async query(){return {rows:[{ready:true}]};}});
});

import express from 'express';
import {randomBytes} from 'node:crypto';
import {createQuizSessionRouter} from '../src/modules/quiz/quiz-session.route.js';
import {createTokenService} from '../src/modules/auth/token.service.js';
import type {QuizSessionRepository} from '../src/modules/quiz/quiz-session.repository.js';
test('v2 authentication precedes schema readiness; legacy quiz path unaffected',async()=>{
 const tokens=createTokenService(randomBytes(48).toString('hex'),60);
 let probes=0,calls=0;
 const repository=new Proxy({}, {get(){return async()=>{calls++;return {};};}}) as QuizSessionRepository;
 const app=express();app.use('/quiz',createQuizSessionRouter(repository,tokens,async()=>{probes++;await assertQuizSessionSchema({async query(){return {rows:[{ready:false}]};}});}));
 app.get('/quiz/questions',(_req,res)=>res.json({legacy:true}));
 app.use((e:any,_req:any,res:any,_next:any)=>res.status(e.status??500).json({code:e.code}));
 const server=app.listen(0,'127.0.0.1');await new Promise<void>(resolve=>server.once('listening',resolve));
 try{const a=server.address() as {port:number};const base='http://127.0.0.1:'+a.port+'/quiz';
 assert.equal((await fetch(base+'/availability?method=MIXED')).status,401);assert.equal(probes,0);
 for(const method of ['MIXED','CHALLENGE','CONTEXT','EN_TO_VI','VI_TO_EN','MATCHING']){
 const response=await fetch(base+'/availability?method='+method,{headers:{Authorization:'Bearer '+tokens.sign(1)}});assert.equal(response.status,503);assert.equal((await response.json()).code,'QUIZ_V2_SCHEMA_REQUIRED');}
 assert.equal(calls,0);assert.equal((await fetch(base+'/questions')).status,200);
 }finally{await new Promise<void>(resolve=>server.close(()=>resolve()));}
});

test('readiness checks enabled receipt/owner/XP protections as well as tables',async()=>{
 let sql='';await assertQuizSessionSchema({async query(q:string){sql=q;return {rows:[{ready:true}]};}});
 for(const name of ['pg_trigger','tgenabled','pg_constraint','pg_index','indisvalid','quiz_v2_action_binding','quiz_v2_xp_binding','quiz_v2_final_receipt','xp_history_v2_owner_answer_fk','xp_history_quiz_action_unique'])assert.ok(sql.includes(name),'Missing protection check: '+name);
});
