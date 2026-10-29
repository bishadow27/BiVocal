import assert from 'node:assert/strict'; import {describe,it} from 'node:test'; import express from 'express'; import type {Server} from 'node:http'; import type {AddressInfo} from 'node:net'; import jwt from 'jsonwebtoken';
import {createLearningRouter} from '../src/modules/learning/learning.route.js'; import {createLearningService} from '../src/modules/learning/learning.service.js';
import type {LearningRepository} from '../src/modules/learning/learning.repository.js'; import {createTokenService} from '../src/modules/auth/token.service.js'; import {errorHandler} from '../src/shared/middleware/error-handler.js';

describe('M15 learning HTTP API',()=>{
 const secret='m15-learning-tests-secret-at-least-32-characters';
 async function fixture(){let seenUser=0;let goal=10;const repo:LearningRepository={
  async getGoal(userId){seenUser=userId;return goal;},async updateGoal(userId,value){seenUser=userId;goal=value;return goal;},
  async today(userId){seenUser=userId;return{date:'2026-10-24',dailyGoal:goal,newWordsLearned:1,remaining:goal-1,completionPercent:10,reviewCount:2,dueCount:3,completed:false};},
  async history(userId){seenUser=userId;return[];},async newCards(userId){seenUser=userId;return[{id:1,english:'apple',vietnamese:'táo',pronunciation:null,part_of_speech:'noun',example:null,example_vi:null,level:'A1'}];}};
  const app=express();app.use('/api/v1/learning',createLearningRouter(createLearningService(repo),createTokenService(secret,3600)));app.use(errorHandler);
  const server=app.listen(0,'127.0.0.1');await new Promise<void>((resolve,reject)=>{server.once('listening',resolve);server.once('error',reject);});
  return{server,base:`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1/learning`,token:createTokenService(secret,3600).sign(42),seen:()=>seenUser};}
 async function close(server:Server){await new Promise<void>((resolve,reject)=>server.close(e=>e?reject(e):resolve()));}
 it('requires a valid JWT and rejects an expired JWT',async()=>{const f=await fixture();try{
  assert.equal((await fetch(f.base+'/today')).status,401);const expired=jwt.sign({av:1},secret,{subject:'42',expiresIn:-1});
  assert.equal((await fetch(f.base+'/today',{headers:{Authorization:`Bearer ${expired}`}})).status,401);
 }finally{await close(f.server);}});
 it('uses only JWT identity and returns today/goal/new card DTOs',async()=>{const f=await fixture();try{const h={Authorization:`Bearer ${f.token}`};
  assert.equal((await fetch(f.base+'/today?userId=99',{headers:h})).status,200);assert.equal(f.seen(),42);
  const goal=await fetch(f.base+'/goal',{headers:h});assert.equal(goal.status,200);assert.deepEqual(await goal.json(),{dailyGoal:10});
  const cards=await fetch(f.base+'/new-cards?limit=10',{headers:h});assert.equal(cards.status,200);assert.equal((await cards.json() as unknown[]).length,1);
 }finally{await close(f.server);}});
 it('accepts only the four supported goals and strict queries',async()=>{const f=await fixture();try{const h={Authorization:`Bearer ${f.token}`,'Content-Type':'application/json'};
  assert.equal((await fetch(f.base+'/goal',{method:'PUT',headers:h,body:JSON.stringify({dailyGoal:5})})).status,200);
  for(const body of [{dailyGoal:7},{dailyGoal:10,userId:2},{}])assert.equal((await fetch(f.base+'/goal',{method:'PUT',headers:h,body:JSON.stringify(body)})).status,400);
  assert.equal((await fetch(f.base+'/history?days=7&days=8',{headers:h})).status,400);
  assert.equal((await fetch(f.base+'/new-cards?limit=5000',{headers:h})).status,400);
 }finally{await close(f.server);}});
});
