import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomBytes, randomUUID } from 'node:crypto';
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
import { createProgressIntegrationRepository } from './support/progress-integration.repository.js';

const authSchema = z.object({
  user: z.object({ id: z.number().int().positive(), username: z.string(), email: z.string(),
    fullName: z.string().nullable(), avatarUrl: z.string().nullable() }).strict(),
  accessToken: z.string(),
}).strict();
const progressSchema = z.object({ flashcardId: z.number().int().positive(),
  memoryLevel: z.number().int().min(0).max(5), reviewCount: z.number().int().nonnegative(),
  correctCount: z.number().int().nonnegative(), wrongCount: z.number().int().nonnegative(),
  isFavorite: z.boolean(), lastReview: z.string().datetime().nullable(),
  nextReview: z.string().datetime().nullable(), createdAt: z.string().datetime(), updatedAt: z.string().datetime() }).strict();
const summarySchema = z.object({ trackedCards: z.number(), reviewedCards: z.number(),
  favoriteCards: z.number(), masteredCards: z.number(), totalReviews: z.number(),
  correctAnswers: z.number(), wrongAnswers: z.number(), accuracy: z.number().nullable() }).strict();

it('Milestone 3 PostgreSQL lifecycle, concurrent reviews and dataset preservation', async () => {
  if (process.env.PROGRESS_INTEGRATION_ALLOW_WRITES !== '1') {
    throw new Error('Progress integration requires PROGRESS_INTEGRATION_ALLOW_WRITES=1 after user_progress migration is applied');
  }
  const env = readEnv(process.env);
  const pool = createPool(env);
  const checks = createProgressIntegrationRepository(pool);
  const tag = `pg_${randomUUID().replaceAll('-', '').slice(0, 24)}`;
  const identities = ['a', 'b'].map((suffix) => {
    const username = `${tag}_${suffix}`;
    return { username, email: `${username}@example.com` };
  });
  const password = randomBytes(20).toString('hex');
  let server: Server | undefined;
  let baseline: string | undefined;
  let mayCleanup = false;
  try {
    assert.equal(await checks.progressMigrationExists(), true,
      'DATABASE MIGRATION NOT APPLIED: explicitly apply user_progress before progress integration');
    assert.equal(await checks.usersMigrationExists(), true, 'Existing users migration is required');
    baseline = await checks.datasetSnapshot();
    for (const identity of identities) {
      assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined,
        'Test UUID identity must not preexist');
    }
    mayCleanup = true;
    const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN);
    const auth = { tokens, service: createAuthService(createAuthRepository(pool), createPasswordService(), tokens) };
    server = createApp({ database: createHealthRepository(pool), topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), progress: createProgressRepository(pool) }, auth)
      .listen(0, '127.0.0.1');
    const running = server;
    await new Promise<void>((resolve, reject) => {
      running.once('listening', resolve); running.once('error', reject);
    });
    const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
    const registered: z.infer<typeof authSchema>[] = [];
    for (const identity of identities) {
      const response = await fetch(base + '/auth/register', { method: 'POST',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...identity, password }) });
      assert.equal(response.status, 201);
      registered.push(authSchema.parse(await response.json()));
    }
    const [a, b] = registered;
    assert.ok(a && b);
    const foodIds = await checks.foodFlashcardIds();
    assert.equal(foodIds.length, 30);
    const [card, secondCard] = foodIds;
    assert.ok(card !== undefined && secondCard !== undefined);

    const request = (path: string, token: string, method = 'GET', body?: unknown) => fetch(base + path, {
      method, headers: { Authorization: `Bearer ${token}`,
        ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    async function review(correct: boolean) {
      const response = await request(`/progress/${card}/review`, a!.accessToken, 'POST', { correct });
      assert.equal(response.status, 200); return progressSchema.parse(await response.json());
    }
    async function favorite(value: boolean, id: number = card!) {
      const response = await request(`/progress/${id}/favorite`, a!.accessToken, 'PUT', { isFavorite: value });
      assert.equal(response.status, 200); return progressSchema.parse(await response.json());
    }

    const first = await review(true);
    assert.deepEqual([first.memoryLevel, first.reviewCount, first.correctCount, first.wrongCount], [1, 1, 1, 0]);
    let stored = await checks.progressRecord(a.user.id, card);
    assert.ok(stored);
    assert.deepEqual([stored.user_id, stored.flashcard_id, stored.memory_level,
      stored.review_count, stored.correct_count, stored.wrong_count], [a.user.id, card, 1, 1, 1, 0]);
    assert.ok(stored.last_review instanceof Date); assert.ok(stored.updated_at instanceof Date);
    assert.equal(stored.last_review.toISOString(), first.lastReview);
    assert.equal(stored.updated_at.toISOString(), first.updatedAt);
    assert.equal(stored.created_at.toISOString(), first.createdAt);
    assert.equal(stored.next_review?.toISOString(), first.nextReview);
    assert.ok(first.nextReview && Date.parse(first.nextReview) - Date.parse(first.lastReview!) >= 86400000 - 1000);

    for (let n = 0; n < 6; n++) {
      const next = await review(true);
      assert.equal(next.memoryLevel, Math.min(n + 2, 5));
      assert.equal(next.reviewCount, n + 2); assert.equal(next.correctCount, n + 2);
      assert.equal(next.createdAt, first.createdAt);
    }
    for (let n = 0; n < 7; n++) {
      const next = await review(false);
      assert.equal(next.memoryLevel, Math.max(4 - n, 0));
      assert.equal(next.reviewCount, 8 + n); assert.equal(next.wrongCount, n + 1);
      assert.equal(next.correctCount, 7);
    }

    // This verifies actual database atomicity; a repository double cannot do so.
    const concurrent = await Promise.all(Array.from({ length: 8 }, () => review(true)));
    assert.ok(concurrent.every((dto) => dto.nextReview !== null));
    stored = await checks.progressRecord(a.user.id, card);
    assert.ok(stored);
    assert.deepEqual([stored.memory_level, stored.review_count, stored.correct_count, stored.wrong_count], [5, 22, 15, 7]);
    assert.equal(await checks.pairRowCount(a.user.id, card), 1);

    const marked = await favorite(true);
    assert.equal(marked.isFavorite, true);
    const cleared = await favorite(false);
    assert.equal(cleared.isFavorite, false);
    assert.deepEqual([cleared.reviewCount, cleared.correctCount, cleared.wrongCount, cleared.memoryLevel], [22, 15, 7, 5]);
    assert.equal(cleared.lastReview, marked.lastReview); assert.equal(cleared.createdAt, first.createdAt);
    assert.ok(new Date(cleared.updatedAt).getTime() >= new Date(first.updatedAt).getTime());
    await favorite(true, secondCard);
    const second = await checks.progressRecord(a.user.id, secondCard);
    assert.ok(second);
    assert.deepEqual([second.memory_level, second.review_count, second.correct_count, second.wrong_count], [0, 0, 0, 0]);
    assert.equal(second.last_review, null); assert.equal(second.next_review, null);

    const detail = await request(`/progress/${card}`, a.accessToken);
    assert.equal(detail.status, 200); assert.deepEqual(progressSchema.parse(await detail.json()), cleared);
    const list = await request('/progress', a.accessToken);
    assert.equal(list.status, 200);
    const listed = z.array(progressSchema).parse(await list.json());
    assert.equal(listed.length, 2);
    assert.ok(listed.every((dto) => [card, secondCard].includes(dto.flashcardId)));
    for (let n = 1; n < listed.length; n++) {
      const previous = listed[n - 1]!; const next = listed[n]!;
      assert.ok(previous.updatedAt > next.updatedAt ||
        (previous.updatedAt === next.updatedAt && previous.flashcardId < next.flashcardId));
    }
    const summary = await request('/progress/summary', a.accessToken);
    assert.equal(summary.status, 200);
    assert.deepEqual(summarySchema.parse(await summary.json()), { trackedCards: 2, reviewedCards: 1,
      favoriteCards: 1, masteredCards: 1, totalReviews: 22, correctAnswers: 15, wrongAnswers: 7, accuracy: 68.18 });

    const bDetail = await request(`/progress/${card}`, b.accessToken);
    assert.equal(bDetail.status, 404);
    assert.deepEqual(await bDetail.json(), { error: { code: 'PROGRESS_NOT_FOUND', message: 'Progress not found' } });
    const bList = await request(`/progress?userId=${a.user.id}`, b.accessToken);
    assert.equal(bList.status, 200); assert.deepEqual(await bList.json(), []);
    const bSummary = await request('/progress/summary', b.accessToken);
    assert.equal(bSummary.status, 200);
    assert.deepEqual(summarySchema.parse(await bSummary.json()), { trackedCards: 0, reviewedCards: 0,
      favoriteCards: 0, masteredCards: 0, totalReviews: 0, correctAnswers: 0, wrongAnswers: 0, accuracy: null });
    const override = await request(`/progress/${card}/review`, b.accessToken, 'POST', { correct: true, userId: a.user.id });
    assert.equal(override.status, 400);
    const bReview = await request(`/progress/${card}/review?userId=${a.user.id}`, b.accessToken, 'POST', { correct: false });
    assert.equal(bReview.status, 200);
    const bDto = progressSchema.parse(await bReview.json());
    assert.deepEqual([bDto.memoryLevel, bDto.reviewCount, bDto.correctCount, bDto.wrongCount], [0, 1, 0, 1]);
    assert.equal(await checks.pairRowCount(b.user.id, card), 1);
    assert.equal(await checks.pairRowCount(a.user.id, card), 1);
    assert.equal((await checks.progressRecord(a.user.id, card))?.review_count, 22);

    const topics = await fetch(base + '/topics'); assert.equal(topics.status, 200);
    assert.equal(z.array(z.unknown()).parse(await topics.json()).length, 10);
    const food = await fetch(base + '/topics/FOOD/flashcards'); assert.equal(food.status, 200);
    assert.equal(z.array(z.unknown()).parse(await food.json()).length, 30);
    assert.equal(await checks.datasetSnapshot(), baseline);
  } finally {
    // Try every UUID-owned cleanup even if an assertion or another cleanup fails.
    // No broad DELETE and no manual baseline/progress table modification.
    let cleanupFailed = false;
    try {
      if (mayCleanup) {
        for (const identity of identities) {
          try {
            const owned = await checks.testUserRecord(identity.username, identity.email);
            if (owned) {
              await checks.cleanupOwnedUser(owned.id, identity.username, identity.email);
              assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
              assert.equal(await checks.userRowCount(owned.id), 0, 'Test progress must be deleted by FK cascade');
            }
          } catch {
            cleanupFailed = true;
          }
        }
      }
      if (baseline !== undefined) assert.equal(await checks.datasetSnapshot(), baseline);
      if (cleanupFailed) throw new Error('Progress integration cleanup did not complete; inspect the UUID test identities locally');
    } finally {
      try {
        if (server) {
          const running = server;
          await new Promise<void>((resolve, reject) => {
            running.close((error) => error ? reject(error) : resolve()); running.closeAllConnections();
          });
        }
      } finally {
        await pool.end();
      }
    }
  }
});
