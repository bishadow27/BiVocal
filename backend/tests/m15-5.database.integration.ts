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

describe('M15.5 20k vocabulary on isolated PostgreSQL',()=>{
  let pool:Pool,server:Server,base:string;
  before(async()=>{const env=readDatabaseEnv(process.env);assert.equal(env.DB_NAME,'bivoca_m15_5_20261024');pool=createPool(env);
    server=createApp({database:createHealthRepository(pool),topics:createTopicRepository(pool),flashcards:createFlashcardRepository(pool)}).listen(0,'127.0.0.1');
    await new Promise<void>((resolve,reject)=>{server.once('listening',resolve);server.once('error',reject)});
    base=`http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;});
  after(async()=>{await new Promise<void>((resolve,reject)=>{server.close(e=>e?reject(e):resolve());server.closeAllConnections()});await pool.end()});
  it('has exactly 20k unique published senses and preserved user data',async()=>{const r=(await pool.query(`SELECT
    (SELECT count(*)::int FROM flashcards) cards,(SELECT count(*)::int FROM topics) topics,
    (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,
    (SELECT count(*)::int FROM quiz_results) quiz`)).rows[0];assert.deepEqual(r,{cards:20000,topics:20,users:5,progress:14,quiz:12});
    assert.equal((await pool.query(`SELECT count(*)::int n FROM (SELECT lower(english),lower(vietnamese) FROM flashcards GROUP BY 1,2 HAVING count(*)>1)x`)).rows[0].n,0);});
  it('paginates search and topic filters without loading 20k rows',async()=>{const page=await (await fetch(`${base}/flashcards?search=book&page=1&limit=20`)).json() as any;
    assert.equal(page.items.length,Math.min(20,page.total));assert.ok(page.total>0);assert.ok(page.items.every((x:any)=>JSON.stringify(x).toLowerCase().includes('book')));
    const topic=await (await fetch(`${base}/flashcards?topicCode=GENERAL_VOCABULARY&page=2&limit=20`)).json() as any;
    assert.equal(topic.page,2);assert.equal(topic.items.length,20);assert.ok(topic.total>13000);});
  it('preserves NULL CEFR and filters verified CEFR',async()=>{const levels=(await pool.query(`SELECT
      count(*) FILTER(WHERE level IS NULL)::int unknown,count(*) FILTER(WHERE level IS NOT NULL)::int known FROM flashcards`)).rows[0];
    assert.ok(levels.unknown>0);assert.ok(levels.known>0);assert.equal(levels.unknown+levels.known,20000);
    const b1=await (await fetch(`${base}/flashcards?level=B1&limit=20`)).json() as any;assert.ok(b1.total>0);assert.ok(b1.items.every((x:any)=>x.level==='B1'));});
  it('keeps Quiz restricted to approved quiz-eligible provenance',async()=>{const cards=await createQuizRepository(pool).activeCards();
    const expected=(await pool.query(`SELECT count(*)::int n FROM flashcards f WHERE f.is_active=TRUE AND
      (NOT EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id)
       OR EXISTS(SELECT 1 FROM flashcard_provenance p WHERE p.flashcard_id=f.id
         AND p.validation_status IN('APPROVED_AUTO','APPROVED_MANUAL') AND p.quiz_eligible=TRUE))`)).rows[0].n;
    assert.equal(cards.length,expected);assert.ok(cards.length>5000);assert.ok(cards.length<20000);});
  it('does not attach CEFR provenance to cards with unknown CEFR',async()=>{const row=(await pool.query(`SELECT count(*)::int n FROM flashcards f JOIN flashcard_provenance p ON p.flashcard_id=f.id
    JOIN vocabulary_sources s ON s.id=p.source_id WHERE f.level IS NULL AND s.code='WORDS_CEFR_2024'`)).rows[0];assert.equal(row.n,0);});
});
