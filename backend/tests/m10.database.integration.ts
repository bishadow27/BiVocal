import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
import { it } from 'node:test';
import type { AddressInfo } from 'node:net';
import { createApp } from '../src/app.js';
import { readEnv } from '../src/config/env.js';
import { createPool } from '../src/config/database.js';
import { createAuthRepository } from '../src/modules/auth/auth.repository.js';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import { createProgressRepository } from '../src/modules/progress/progress.repository.js';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import { createFlashcardRepository } from '../src/modules/flashcards/flashcard.repository.js';
import { createTopicRepository } from '../src/modules/topics/topic.repository.js';
import { createHealthRepository } from '../src/modules/health/health.repository.js';
import { createProgressIntegrationRepository } from './support/progress-integration.repository.js';

it('M10 PostgreSQL due queue, scheduling, JWT isolation, Quiz, rollback and concurrency', async () => {
  assert.equal(process.env.M10_INTEGRATION_ALLOW_WRITES, '1');
  const env = readEnv(process.env);
  assert.equal(env.DB_NAME, 'bivoca_m10_20261014_a18cbe62d430');
  const pool = createPool(env);
  const checks = createProgressIntegrationRepository(pool);
  const identities = ['a', 'b'].map(suffix => {
    const username = `m10_${randomUUID().replaceAll('-', '').slice(0, 20)}_${suffix}`;
    return { username, email: `${username}@example.com` };
  });
  const password = randomBytes(20).toString('hex');
  const registered: { id: number; token: string; username: string; email: string }[] = [];
  let server: ReturnType<ReturnType<typeof createApp>['listen']> | undefined;
  let baseline: string | undefined;
  try {
    assert.equal((await pool.query('SELECT current_database() AS name')).rows[0]?.name, env.DB_NAME);
    baseline = await checks.datasetSnapshot();
    const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN);
    const auth = { tokens, service: createAuthService(createAuthRepository(pool), createPasswordService(), tokens) };
    server = createApp({ database: createHealthRepository(pool), topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), progress: createProgressRepository(pool),
      quiz: createQuizRepository(pool) }, auth).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => {
      server!.once('listening', resolve); server!.once('error', reject);
    });
    const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
    for (const identity of identities) {
      const response = await fetch(base + '/auth/register', { method: 'POST',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...identity, password }) });
      assert.equal(response.status, 201);
      const body = await response.json() as { user: { id: number }; accessToken: string };
      registered.push({ ...identity, id: body.user.id, token: body.accessToken });
    }
    const a = registered[0]!; const b = registered[1]!;
    const cardIds = await checks.foodFlashcardIds();
    const [first, second, third] = cardIds;
    assert.ok(first && second && third);
    const request = (path: string, token: string | null = a.token, method = 'GET', body?: unknown) => fetch(base + path, {
      method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    assert.equal((await request('/progress/due', null)).status, 401);
    assert.deepEqual(await (await request('/progress/due')).json(), []);
    let schedule = await (await request('/progress/schedule')).json() as { dueNow: number; reviewedCards: number; trackedCards: number; memoryLevels: number[] };
    assert.deepEqual([schedule.trackedCards, schedule.reviewedCards, schedule.dueNow], [0, 0, 0]);
    const favorite = await request(`/progress/${first}/favorite`, a.token, 'PUT', { isFavorite: true });
    assert.equal(favorite.status, 200);
    assert.deepEqual(await (await request('/progress/due')).json(), []);
    const firstReview = await request(`/progress/${first}/review`, a.token, 'POST', { correct: true });
    assert.equal(firstReview.status, 200);
    let progress = await firstReview.json() as { memoryLevel: number; lastReview: string; nextReview: string };
    assert.equal(progress.memoryLevel, 1);
    assert.ok(Date.parse(progress.nextReview) - Date.parse(progress.lastReview) >= 86400000 - 1000);
    assert.deepEqual(await (await request('/progress/due')).json(), []);
    const wrong = await request(`/progress/${first}/review`, a.token, 'POST', { correct: false });
    assert.equal(wrong.status, 200);
    progress = await wrong.json() as typeof progress;
    assert.equal(progress.memoryLevel, 0);
    assert.ok(Date.parse(progress.nextReview) - Date.parse(progress.lastReview) >= 600000 - 1000);
    schedule = await (await request('/progress/schedule')).json() as typeof schedule;
    assert.deepEqual([schedule.trackedCards, schedule.reviewedCards, schedule.dueNow], [1, 1, 0]);
    assert.equal(schedule.memoryLevels[0], 1);
    // Transition policy: pre-M10 reviewed rows without a schedule are due immediately.
    await pool.query('UPDATE user_progress SET next_review=NULL WHERE user_id=$1 AND flashcard_id=$2', [a.id, first]);
    const dueResponse = await request('/progress/due?limit=1&topicCode=FOOD');
    assert.equal(dueResponse.status, 200);
    const due = await dueResponse.json() as { id: number; memoryLevel: number; topicCode: string }[];
    assert.deepEqual(due.map(card => card.id), [first]);
    assert.equal(due[0]?.memoryLevel, 0); assert.equal(due[0]?.topicCode, 'FOOD');
    assert.deepEqual(await (await request('/progress/due', b.token)).json(), []);
    assert.equal((await request('/progress/due?limit=301')).status, 400);
    assert.equal((await request('/progress/due?userId=' + a.id, b.token)).status, 400);
    await pool.query(`UPDATE user_progress SET last_review =
      (date_trunc('day', clock_timestamp() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC') - INTERVAL '1 second'
      WHERE user_id=$1 AND flashcard_id=$2`, [a.id, first]);
    const beforeUtcDay = await (await request('/progress/schedule')).json() as { reviewedToday: number };
    await pool.query(`UPDATE user_progress SET last_review =
      (date_trunc('day', clock_timestamp() AT TIME ZONE 'UTC') AT TIME ZONE 'UTC') + INTERVAL '1 second'
      WHERE user_id=$1 AND flashcard_id=$2`, [a.id, first]);
    const afterUtcDay = await (await request('/progress/schedule')).json() as { reviewedToday: number };
    assert.equal(afterUtcDay.reviewedToday, beforeUtcDay.reviewedToday + 1);
    // Quiz must use exactly the same scheduler in the existing transaction.
    const quiz = await request('/quiz/answer', a.token, 'POST', { flashcardId: second, selectedFlashcardId: second });
    assert.equal(quiz.status, 200);
    const quizBody = await quiz.json() as { progress: { memoryLevel: number; nextReview: string } };
    assert.equal(quizBody.progress.memoryLevel, 1); assert.ok(quizBody.progress.nextReview);
    const concurrent = await Promise.all(Array.from({ length: 6 }, () => request(`/progress/${third}/review`, a.token, 'POST', { correct: true })));
    assert.ok(concurrent.every(response => response.status === 200));
    const stored = await checks.progressRecord(a.id, third);
    assert.ok(stored);
    assert.deepEqual([stored.review_count, stored.memory_level], [6, 5]);
    assert.equal(await checks.pairRowCount(a.id, third), 1);
    assert.ok(stored.next_review);
    assert.ok(Math.abs(stored.next_review.getTime() - stored.last_review!.getTime() - 30 * 86400000) < 1000);
    const xp = await pool.query<{ n: string }>("SELECT count(*)::text AS n FROM xp_history WHERE user_id=$1 AND flashcard_id=$2 AND source_type='REVIEW'", [a.id, third]);
    assert.equal(Number(xp.rows[0]?.n), 1);
    const highWrong = await request(`/progress/${third}/review`, a.token, 'POST', { correct: false });
    assert.equal(highWrong.status, 200);
    const highWrongDto = await highWrong.json() as { memoryLevel: number; lastReview: string; nextReview: string };
    assert.equal(highWrongDto.memoryLevel, 4);
    assert.ok(Math.abs(Date.parse(highWrongDto.nextReview) - Date.parse(highWrongDto.lastReview) - 600000) < 1000);
    const originalNext = highWrongDto.nextReview;
    await pool.query('UPDATE user_progress SET review_count=2147483647 WHERE user_id=$1 AND flashcard_id=$2', [a.id, third]);
    const failed = await request(`/progress/${third}/review`, a.token, 'POST', { correct: true });
    assert.equal(failed.status, 500);
    const afterFailure = await checks.progressRecord(a.id, third);
    assert.equal(afterFailure?.next_review?.toISOString(), originalNext);
    assert.equal(afterFailure?.review_count, 2147483647);
    assert.equal(Number((await pool.query<{ n: string }>("SELECT count(*)::text AS n FROM xp_history WHERE user_id=$1 AND flashcard_id=$2 AND source_type='REVIEW'", [a.id, third])).rows[0]?.n), 1);
    assert.equal((await request('/progress/schedule', b.token)).status, 200);
    assert.deepEqual(await (await request('/progress/due', b.token)).json(), []);
  } finally {
    if (server) await new Promise<void>((resolve, reject) => {
      server!.close(error => error ? reject(error) : resolve()); server!.closeAllConnections();
    });
    for (const user of registered) await checks.cleanupOwnedUser(user.id, user.username, user.email);
    if (baseline !== undefined) assert.equal(await checks.datasetSnapshot(), baseline);
    await pool.end();
  }
});
