import 'dotenv/config';
import assert from 'node:assert/strict';
import { after,before,describe,it } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { Server } from 'node:http';
import type { Pool } from 'pg';
import { createApp } from '../src/app.js';
import { readDatabaseEnv } from '../src/config/env.js';
import { createPool } from '../src/config/database.js';
import { createHealthRepository } from '../src/modules/health/health.repository.js';
import { createTopicRepository } from '../src/modules/topics/topic.repository.js';
import { createFlashcardRepository } from '../src/modules/flashcards/flashcard.repository.js';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import { createAdminRepository } from '../src/modules/admin/admin.repository.js';
import { createVocabularyReviewRepository } from '../src/modules/vocabulary-review/vocabulary-review.repository.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import { createAuthRepository } from '../src/modules/auth/auth.repository.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';

describe('M13.6 review API against isolated PostgreSQL',()=>{
  let pool:Pool,server:Server,base:string,adminToken:string,userToken:string,userId:number;
  before(async()=>{const env=readDatabaseEnv(process.env);assert.match(env.DB_NAME,/^bivoca_m13_6_20261021_[a-z0-9]+$/);pool=createPool(env);
    const admin=(await pool.query(`SELECT id FROM users WHERE username='m13_6_admin'`)).rows[0];assert.ok(admin);
    const user=(await pool.query(`INSERT INTO users(username,email,password_hash,role) VALUES('m13_6_api_user','m13-6-api-user@local.test','not-used','USER') RETURNING id`)).rows[0];userId=user.id;
    const tokens=createTokenService('m13-6-isolated-secret-at-least-32-characters',3600);adminToken=tokens.sign(admin.id);userToken=tokens.sign(user.id);
    const database=createHealthRepository(pool);server=createApp({database,topics:createTopicRepository(pool),flashcards:createFlashcardRepository(pool),quiz:createQuizRepository(pool),admin:createAdminRepository(pool),vocabularyReview:createVocabularyReviewRepository(pool)},
      {tokens,service:createAuthService(createAuthRepository(pool),createPasswordService(),tokens)}).listen(0,'127.0.0.1');
    await new Promise<void>((resolve,reject)=>{server.once('listening',resolve);server.once('error',reject);});base=`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;});
  after(async()=>{await pool.query('DELETE FROM users WHERE id=$1',[userId]);await new Promise<void>((resolve,reject)=>{server.close(e=>e?reject(e):resolve());server.closeAllConnections();});await pool.end();});
  const auth=(token:string)=>({headers:{Authorization:`Bearer ${token}`}});
  it('enforces authentication and live ADMIN role',async()=>{assert.equal((await fetch(`${base}/admin/vocabulary-review/stats`)).status,401);assert.equal((await fetch(`${base}/admin/vocabulary-review/stats`,auth(userToken))).status,403);assert.equal((await fetch(`${base}/admin/vocabulary-review/stats`,auth(adminToken))).status,200);});
  it('returns review totals and server-side filtered paging',async()=>{const stats=await (await fetch(`${base}/admin/vocabulary-review/stats`,auth(adminToken))).json() as any;assert.deepEqual(stats,{total:24388,pending:23990,approved:0,rejected:340,published:58});
    const page=await (await fetch(`${base}/admin/vocabulary-review/candidates?status=PUBLISHED&level=A1&page=1&limit=10`,auth(adminToken))).json() as any;assert.equal(page.limit,10);assert.ok(page.total>0);assert.ok(page.items.every((v:any)=>v.status==='PUBLISHED'&&v.cefr==='A1'));});
  it('exposes audit records and rejects unknown query fields through the typed endpoint',async()=>{const id=(await pool.query(`SELECT candidate_id FROM vocabulary_review_candidates WHERE status='PUBLISHED' ORDER BY candidate_id LIMIT 1`)).rows[0].candidate_id;
    const audit=await (await fetch(`${base}/admin/vocabulary-review/candidates/${id}/audit?page=1&limit=20`,auth(adminToken))).json() as any;assert.ok(audit.total>=2);assert.ok(audit.items.some((v:any)=>v.action==='PUBLISH'));
    assert.equal((await fetch(`${base}/admin/vocabulary-review/candidates?page=0`,auth(adminToken))).status,400);});
  it('serves published cards to public search and Quiz eligibility',async()=>{const page=await (await fetch(`${base}/flashcards?search=breakfast&limit=20`)).json() as any;assert.ok(page.items.some((v:any)=>v.english==='breakfast'&&v.id>300));const cards=await createQuizRepository(pool).activeCards();assert.equal(cards.length,358);assert.equal(cards.filter(v=>v.id>300).length,58);});
});

