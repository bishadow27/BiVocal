import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomBytes,randomUUID } from 'node:crypto';
import { it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { z } from 'zod';
import { createApp } from '../src/app.js';
import { readEnv } from '../src/config/env.js';
import { createPool } from '../src/config/database.js';
import { createHealthRepository } from '../src/modules/health/health.repository.js';
import { createTopicRepository } from '../src/modules/topics/topic.repository.js';
import { createFlashcardRepository } from '../src/modules/flashcards/flashcard.repository.js';
import { createAuthRepository } from '../src/modules/auth/auth.repository.js';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import { createProgressRepository } from '../src/modules/progress/progress.repository.js';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import { createGamificationRepository } from '../src/modules/gamification/gamification.repository.js';
import { createAdminRepository } from '../src/modules/admin/admin.repository.js';
import { createAdminService } from '../src/modules/admin/admin.service.js';
import { createAdminIntegrationRepository } from './support/admin-integration.repository.js';

const adminMe=z.object({id:z.number().int().positive(),username:z.string(),email:z.string(),fullName:z.string().nullable(),
  avatarUrl:z.string().nullable(),role:z.enum(['USER','ADMIN'])}).strict();
const topicDto=z.object({id:z.number().int().positive(),code:z.string(),name:z.string(),description:z.string().nullable(),
  imageUrl:z.string().nullable(),flashcardCount:z.number().int().nonnegative()}).strict();
const cardDto=z.object({id:z.number().int().positive(),topicId:z.number().int().positive(),topicCode:z.string(),topicName:z.string(),
  english:z.string(),vietnamese:z.string(),pronunciation:z.string().nullable(),partOfSpeech:z.string().nullable(),example:z.string().nullable(),
  exampleVi:z.string().nullable(),level:z.string().nullable(),isActive:z.boolean(),createdAt:z.string().datetime().nullable(),updatedAt:z.string().datetime().nullable()}).strict();
const leaderboardDto=z.object({rank:z.number().int().positive(),userId:z.number().int().positive(),username:z.string(),fullName:z.string().nullable(),
  avatarUrl:z.string().nullable(),totalXp:z.number().int().positive(),level:z.number().int().positive(),currentStreak:z.number().int().nonnegative(),longestStreak:z.number().int().nonnegative()}).strict();
const page=<T extends z.ZodType>(item:T)=>z.object({items:z.array(item),page:z.number().int(),limit:z.number().int(),total:z.number().int(),totalPages:z.number().int()}).strict();

it('M6A real PostgreSQL authorization, Admin APIs, learning compatibility and exact cleanup',async()=>{
  if(process.env.ADMIN_INTEGRATION_ALLOW_WRITES!=='1')throw new Error('Requires ADMIN_INTEGRATION_ALLOW_WRITES=1 after M6 migration');
  // No Pool exists until explicit opt-in has passed.
  const env=readEnv(process.env),pool=createPool(env),checks=createAdminIntegrationRepository(pool);
  const tag=randomUUID().replaceAll('-','').slice(0,24);
  const code=`ADM_${tag.toUpperCase()}`;
  const identities=['a','b'].map(s=>({username:`ad_${tag}_${s}`,email:`ad_${tag}_${s}@example.com`}));
  const password=randomBytes(20).toString('hex');
  const ownedUserIds=new Map<string,number>(),ownedCardIds=new Set<number>();
  let ownedTopicId:number|undefined,server:Server|undefined,baseline:string|undefined,cleanupAllowed=false;
  try{
    assert.equal(await checks.adminPrerequisites(),true,'DATABASE MIGRATION NOT APPLIED or baseline prerequisites absent');
    baseline=await checks.datasetSnapshot();
    for(const identity of identities)assert.equal(await checks.testUserRecord(identity.username,identity.email),undefined);
    assert.equal(await checks.ownedTopic(code),undefined);cleanupAllowed=true;
    const tokens=createTokenService(env.JWT_SECRET,env.JWT_EXPIRES_IN);
    const auth={tokens,service:createAuthService(createAuthRepository(pool),createPasswordService(),tokens)};
    const admin=createAdminRepository(pool);
    server=createApp({database:createHealthRepository(pool),topics:createTopicRepository(pool),flashcards:createFlashcardRepository(pool),
      progress:createProgressRepository(pool),quiz:createQuizRepository(pool),gamification:createGamificationRepository(pool),admin},auth).listen(0,'127.0.0.1');
    const running=server;await new Promise<void>((resolve,reject)=>{running.once('listening',resolve);running.once('error',reject);});
    const base=`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
    const registered:z.infer<typeof registration>[]=[];
    for(const identity of identities){
      const r=await fetch(base+'/auth/register',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...identity,password})});
      assert.equal(r.status,201);const u=registration.parse(await r.json());registered.push(u);ownedUserIds.set(identity.username,u.user.id);
      assert.equal((await admin.account(u.user.id))!.role,'USER');
    }
    const a=registered[0]!,b=registered[1]!;
    await checks.promoteOwned({...identities[0]!,id:a.user.id});
    const request=(path:string,method='GET',body?:unknown,token=a.accessToken)=>fetch(base+path,{method,
      headers:{Authorization:`Bearer ${token}`,...(body===undefined?{}:{'Content-Type':'application/json'})},
      ...(body===undefined?{}:{body:JSON.stringify(body)})});
    async function ok(path:string,method='GET',body?:unknown,token=a.accessToken,status=200){const r=await request(path,method,body,token);assert.equal(r.status,status);return r.json();}
    assert.equal((await request('/admin/me','GET',undefined,b.accessToken)).status,403);
    assert.equal(adminMe.parse(await ok('/admin/me')).role,'ADMIN');
    // Existing login remains the sole login flow and returns unchanged auth DTO.
    const login=await fetch(base+'/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:identities[0]!.email,password})});
    assert.equal(login.status,200);registration.parse(await login.json());
    const dashboardBefore=await ok('/admin/dashboard');assert.ok(dashboardBefore.users.total>=2);
    const t=topicDto.parse(await ok('/admin/topics','POST',{code,name:`Admin test ${tag}`,description:'owned test',imageUrl:null},a.accessToken,201));
    ownedTopicId=t.id;
    const inputs=Array.from({length:5},(_,i)=>({topicCode:code,english:`admin ${tag} word ${i}`,vietnamese:`nghĩa ${tag} ${i}`,
      pronunciation:null,partOfSpeech:'noun',example:null,exampleVi:null,level:'A1'}));
    for(const input of inputs){const f=cardDto.parse(await ok('/admin/flashcards','POST',input,a.accessToken,201));ownedCardIds.add(f.id);}
    const ids=[...ownedCardIds];assert.equal(ids.length,5);
    const cards=page(cardDto).parse(await ok(`/admin/flashcards?topicCode=${code}&status=ACTIVE&level=A1&search=${tag}&page=2&limit=2`));
    assert.equal(cards.total,5);assert.equal(cards.totalPages,3);assert.equal(cards.items.length,2);
    assert.deepEqual(cards.items.map(c=>c.id),ids.slice(2,4));
    const topicList=page(topicDto).parse(await ok(`/admin/topics?search=${code.toLowerCase()}`));assert.equal(topicList.total,1);assert.equal(topicList.items[0]!.flashcardCount,5);
    assert.equal((await request('/admin/topics','POST',{code,name:`Duplicate ${tag}`})).status,409);
    assert.equal((await request('/admin/flashcards','POST',{...inputs[0]!,english:inputs[0]!.english.toUpperCase()})).status,409);
    const changed=topicDto.parse(await ok(`/admin/topics/${t.id}`,'PUT',{code,name:`Updated ${tag}`,description:null,imageUrl:null}));assert.equal(changed.code,code);
    const cardChanged=cardDto.parse(await ok(`/admin/flashcards/${ids[0]}`,'PUT',{...inputs[0]!,example:'Updated example'}));assert.equal(cardChanged.example,'Updated example');
    await ok(`/admin/flashcards/${ids[4]}/status`,'PATCH',{isActive:false});
    const learning=await ok(`/topics/${code}/flashcards`);assert.equal(learning.length,4);assert.ok(!learning.some((f:{id:number})=>f.id===ids[4]));
    assert.equal((await request(`/flashcards/${ids[4]}`)).status,404);
    assert.equal((await request('/quiz/answer','POST',{flashcardId:ids[4],selectedFlashcardId:ids[0]},b.accessToken)).status,404);
    const generated=await ok(`/quiz/questions?topicCode=${code}&limit=4`,'GET',undefined,b.accessToken);
    assert.equal(generated.length,4);assert.ok(generated.every((q:{flashcardId:number})=>q.flashcardId!==ids[4]));
    await ok(`/progress/${ids[0]}/review`,'POST',{correct:true},b.accessToken);
    await ok('/quiz/answer','POST',{flashcardId:ids[0],selectedFlashcardId:ids[0]},b.accessToken);
    const me=await ok('/gamification/me','GET',undefined,b.accessToken);assert.equal(me.totalXp,15);
    const detail=await ok(`/admin/users/${b.user.id}`);assert.equal(detail.user.id,b.user.id);assert.equal(detail.user.role,'USER');
    assert.equal(detail.progress.totalReviews,2);assert.equal(detail.quiz.totalAnswers,1);assert.equal(detail.gamification.totalXp,15);
    assert.doesNotMatch(JSON.stringify(detail),/password_hash|accessToken|JWT_SECRET/);
    const progress=await ok(`/admin/users/${b.user.id}/progress?search=${tag}&limit=1`);assert.equal(progress.total,1);assert.equal(progress.items[0].reviewCount,2);
    const history=await ok(`/admin/users/${b.user.id}/quiz-history?limit=1`);assert.equal(history.total,1);assert.equal(history.items[0].correct,true);
    const filteredUsers=await ok(`/admin/users?role=USER&search=${encodeURIComponent(identities[1]!.email)}`);
    assert.equal(filteredUsers.total,1);assert.equal(filteredUsers.items[0].id,b.user.id);assert.doesNotMatch(JSON.stringify(filteredUsers),/password_hash/);
    const stats=await ok(`/admin/stats/quiz?range=7d&topicCode=${code}`);assert.equal(stats.totalAnswers,1);assert.equal(stats.accuracy,100);
    assert.equal(stats.byDay.length,7);assert.equal(stats.byTopic[0].code,code);
    const statsAll=await ok(`/admin/stats/quiz?range=all&topicCode=${code}`);assert.equal(statsAll.byDay.length,1);
    // Full production repository ranking for test lookup; public API remains <=100/page.
    const full=await createAdminService(admin).leaderboard({page:1,limit:2147483647});
    const ranked=full.items.find(u=>u.userId===b.user.id);assert.ok(ranked);assert.equal(ranked.totalXp,15);
    const board=page(leaderboardDto).parse(await ok('/admin/leaderboard?limit=1'));
    assert.deepEqual(board.items,full.items.slice(0,1));assert.equal(board.total,full.total);
    if(full.total>1)assert.deepEqual(page(leaderboardDto).parse(await ok('/admin/leaderboard?page=2&limit=1')).items,full.items.slice(1,2));
    // Disabling a learned card must hide learning/progress views, not erase history/XP.
    await ok(`/admin/flashcards/${ids[0]}/status`,'PATCH',{isActive:false});
    assert.equal((await ok('/progress','GET',undefined,b.accessToken)).length,0);
    assert.equal((await ok('/progress/summary','GET',undefined,b.accessToken)).trackedCards,0);
    assert.equal((await ok(`/admin/users/${b.user.id}/progress`)).total,1);
    assert.equal((await ok(`/admin/users/${b.user.id}/quiz-history`)).total,1);
    assert.equal((await ok('/gamification/me','GET',undefined,b.accessToken)).totalXp,15);
    await ok(`/admin/users/${b.user.id}/role`,'PATCH',{role:'ADMIN'});
    assert.equal(adminMe.parse(await ok('/admin/me','GET',undefined,b.accessToken)).role,'ADMIN');
    await ok(`/admin/users/${b.user.id}/role`,'PATCH',{role:'USER'});
    assert.equal((await request('/admin/me','GET',undefined,b.accessToken)).status,403);
    const self=await request(`/admin/users/${a.user.id}/role`,'PATCH',{role:'USER'});assert.equal(self.status,409);
    assert.equal((await self.json()).error.code,'ADMIN_SELF_ROLE_CHANGE_FORBIDDEN');
    assert.equal((await admin.account(a.user.id))!.role,'ADMIN');
    for(const path of [`/admin/topics/${t.id}`,`/admin/flashcards/${ids[0]}`])assert.equal((await request(path,'DELETE')).status,404);
    assert.equal(await checks.baselineExcludingOwned(t.id,code),baseline);
  }finally{
    const errors:unknown[]=[];
    if(server){const running=server;try{await new Promise<void>((resolve,reject)=>{running.close(e=>e?reject(e):resolve());running.closeAllConnections();});}catch(e){errors.push(e);}}
    if(cleanupAllowed){
      // Users first: cascades remove their progress/results/scores/history before cards.
      for(const identity of identities){try{
        const row=await checks.testUserRecord(identity.username,identity.email),id=row?.id??ownedUserIds.get(identity.username);
        if(id!==undefined){await checks.cleanupOwnedUser(id,identity.username,identity.email);
          assert.equal(await checks.testUserRecord(identity.username,identity.email),undefined);
          assert.equal(await checks.userRowCount(id),0);assert.equal(await checks.quizCount(id),0);
          assert.equal(await checks.score(id),undefined);assert.equal(await checks.historyCount(id),0);}
      }catch(e){errors.push(e);}}
      try{
        // Recovery covers a committed create whose HTTP response was lost/invalid.
        const row=await checks.ownedTopic(code);const id=row?.id??ownedTopicId;
        if(id!==undefined){for(const card of await checks.ownedCards(id,code))ownedCardIds.add(card);
          for(const card of ownedCardIds){try{await checks.deleteOwnedCard(card,id,code);}catch(e){errors.push(e);}}
          assert.deepEqual(await checks.ownedCards(id,code),[]);
          await checks.deleteOwnedTopic(id,code);assert.equal(await checks.ownedTopic(code),undefined);}
      }catch(e){errors.push(e);}
    }
    if(baseline!==undefined){try{assert.equal(await checks.datasetSnapshot(),baseline,'Baseline rows must remain byte-equivalent after cleanup');}catch(e){errors.push(e);}}
    try{await pool.end();}catch(e){errors.push(e);}
    if(errors.length)throw new AggregateError(errors,'Admin integration cleanup/baseline verification failed');
  }
});

const registration=z.object({user:z.object({id:z.number().int().positive(),username:z.string(),email:z.string(),
  fullName:z.string().nullable(),avatarUrl:z.string().nullable()}).strict(),accessToken:z.string()}).strict();
