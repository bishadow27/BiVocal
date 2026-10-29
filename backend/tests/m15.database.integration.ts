import 'dotenv/config';
import assert from 'node:assert/strict'; import {randomUUID} from 'node:crypto'; import {it} from 'node:test';
import {createPool} from '../src/config/database.js'; import {readDatabaseEnv} from '../src/config/env.js';
import {createLearningRepository,recordDailyLearning} from '../src/modules/learning/learning.repository.js';
import {createProgressRepository} from '../src/modules/progress/progress.repository.js';
import {createQuizRepository} from '../src/modules/quiz/quiz.repository.js';

it('M15 isolated daily goals, exact first learning, concurrency, history and rollback',async()=>{
 if(process.env.M15_INTEGRATION_ALLOW_WRITES!=='1'||process.env.DB_NAME!=='bivoca_m15_20261024')throw new Error('M15 integration restricted to isolated DB');
 const env=readDatabaseEnv(process.env);const pool=createPool(env);const learning=createLearningRepository(pool);
 const progress=createProgressRepository(pool);const quiz=createQuizRepository(pool);const tag=randomUUID().replaceAll('-','').slice(0,18);
 const baseline=(await pool.query(`SELECT (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,
   (SELECT count(*)::int FROM quiz_results) quiz,(SELECT count(*)::int FROM user_learned_flashcards) learned`)).rows[0];
 const ids:number[]=[];
 try{
  const legacyMissing=(await pool.query(`SELECT count(*)::int count FROM (
    SELECT user_id,flashcard_id FROM user_progress WHERE review_count>0 UNION SELECT user_id,flashcard_id FROM quiz_results) e
    WHERE NOT EXISTS(SELECT 1 FROM user_learned_flashcards l WHERE l.user_id=e.user_id AND l.flashcard_id=e.flashcard_id AND l.first_source='LEGACY' AND l.first_learned_at IS NULL)`)).rows[0]?.count;
  assert.equal(legacyMissing,0);
  for(const suffix of ['a','b']){const row=(await pool.query<{id:number}>(`INSERT INTO users(username,email,password_hash) VALUES($1,$2,'test-only') RETURNING id`,[`m15_${tag}_${suffix}`,`m15_${tag}_${suffix}@example.com`])).rows[0]!;ids.push(row.id);}
  const [a,b]=ids as [number,number];assert.equal(await learning.getGoal(a),10);assert.equal(await learning.updateGoal(a,5),5);assert.equal(await learning.getGoal(a),5);assert.equal(await learning.getGoal(b),10);
  const cards=(await pool.query<{id:number}>(`SELECT id FROM flashcards WHERE is_active=TRUE ORDER BY id LIMIT 5`)).rows.map(r=>r.id);const [c1,c2,c3,c4,c5]=cards;assert.ok(c1&&c2&&c3&&c4&&c5);
  await progress.favorite(a,c1,true);assert.equal((await learning.today(a)).newWordsLearned,0);
  await progress.review(a,c1,true);let today=await learning.today(a);assert.equal(today.newWordsLearned,1);assert.equal(today.reviewCount,0);assert.equal(today.dailyGoal,5);
  await progress.review(a,c1,false);today=await learning.today(a);assert.equal(today.newWordsLearned,1);assert.equal(today.reviewCount,1);
  await Promise.all([progress.review(a,c2,true),progress.review(a,c2,false)]);today=await learning.today(a);assert.equal(today.newWordsLearned,2);assert.equal(today.reviewCount,2);
  const submitted=await quiz.answer(a,c3,c3);assert.equal(submitted.status,'answered');today=await learning.today(a);assert.equal(today.newWordsLearned,3);
  await progress.review(a,c3,true);today=await learning.today(a);assert.equal(today.newWordsLearned,3);assert.equal(today.reviewCount,3);assert.equal(today.remaining,2);assert.equal(today.completionPercent,60);
  assert.equal((await learning.today(b)).newWordsLearned,0);
  const fresh=await learning.newCards(a,20);assert.ok(!fresh.some(c=>[c1,c2,c3].includes(c.id)));assert.ok(fresh.length<=20);
  const client=await pool.connect();try{await client.query('BEGIN');await recordDailyLearning(client,a,c4,'REVIEW');await client.query('ROLLBACK');}finally{client.release();}
  assert.equal((await pool.query('SELECT count(*)::int count FROM user_learned_flashcards WHERE user_id=$1 AND flashcard_id=$2',[a,c4])).rows[0]?.count,0);
  const history=await learning.history(a,7);assert.equal(history.length,7);assert.equal(history.at(-1)?.newWords,3);assert.equal(history.at(-1)?.status,'NOT_COMPLETED');
  await learning.updateGoal(a,5);await progress.review(a,c4,true);await progress.review(a,c5,true);assert.equal((await learning.today(a)).completed,true);
 }finally{if(ids.length)await pool.query('DELETE FROM users WHERE id=ANY($1::int[])',[ids]);const after=(await pool.query(`SELECT (SELECT count(*)::int FROM users) users,(SELECT count(*)::int FROM user_progress) progress,
   (SELECT count(*)::int FROM quiz_results) quiz,(SELECT count(*)::int FROM user_learned_flashcards) learned`)).rows[0];assert.deepEqual(after,baseline);await pool.end();}
});
