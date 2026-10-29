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
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import { createGamificationRepository } from '../src/modules/gamification/gamification.repository.js';
import { createGamificationService } from '../src/modules/gamification/gamification.service.js';
import { createGamificationIntegrationRepository } from './support/gamification-integration.repository.js';

const progressSchema = z.object({ flashcardId: z.number().int().positive(), memoryLevel: z.number().int().min(0).max(5),
  reviewCount: z.number().int().nonnegative(), correctCount: z.number().int().nonnegative(), wrongCount: z.number().int().nonnegative(),
  isFavorite: z.boolean(), lastReview: z.string().datetime().nullable(), nextReview: z.string().datetime().nullable(),
  createdAt: z.string().datetime(), updatedAt: z.string().datetime() }).strict();
const answerSchema = z.object({ resultId: z.number().int().positive(), flashcardId: z.number().int().positive(),
  selectedFlashcardId: z.number().int().positive(), correct: z.boolean(), correctAnswer: z.string(), selectedAnswer: z.string(),
  answeredAt: z.string().datetime(), progress: progressSchema }).strict();
const scoreSchema = z.object({ totalXp: z.number().int().nonnegative(), level: z.number().int().positive(),
  xpIntoLevel: z.number().int().min(0).max(99), xpPerLevel: z.literal(100), currentStreak: z.number().int().nonnegative(),
  longestStreak: z.number().int().nonnegative(), lastActivityDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).nullable() }).strict();
const historySchema = z.array(z.object({ id: z.number().int().positive(), sourceType: z.enum(['QUIZ', 'REVIEW']),
  flashcardId: z.number().int().positive().nullable(), word: z.string().nullable(), xpAmount: z.number().int().positive(),
  activityDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/), createdAt: z.string().datetime() }).strict());
const boardSchema = z.array(z.object({ rank: z.number().int().positive(), userId: z.number().int().positive(),
  username: z.string(), fullName: z.string().nullable(), avatarUrl: z.string().nullable(), totalXp: z.number().int().positive(),
  level: z.number().int().positive(), currentStreak: z.number().int().nonnegative(), longestStreak: z.number().int().nonnegative() }).strict());
const authSchema = z.object({ user: z.object({ id: z.number().int().positive(), username: z.string(), email: z.string() }).passthrough(),
  accessToken: z.string() });

it('Milestone 5 PostgreSQL awards, UTC streaks, concurrent transactions, rollback and exact cleanup', async () => {
  // Guard deliberately precedes readEnv/createPool and all network access.
  if (process.env.GAMIFICATION_INTEGRATION_ALLOW_WRITES !== '1') {
    throw new Error('Requires GAMIFICATION_INTEGRATION_ALLOW_WRITES=1 after M5 migration is applied');
  }
  const env = readEnv(process.env); const pool = createPool(env);
  const checks = createGamificationIntegrationRepository(pool);
  const gamification = createGamificationRepository(pool);
  const tag = `gm_${randomUUID().replaceAll('-', '').slice(0, 24)}`;
  const identities = ['a', 'b'].map((suffix) => ({ username: `${tag}_${suffix}`, email: `${tag}_${suffix}@example.com` }));
  const password = randomBytes(20).toString('hex');
  let server: Server | undefined; let baseline: string | undefined; let cleanupAllowed = false;
  const registeredIds = new Map<string, number>();
  try {
    assert.equal(await checks.prerequisites(), true, 'DATABASE MIGRATION NOT APPLIED: all five user/activity tables required');
    baseline = await checks.datasetSnapshot();
    const dataset = JSON.parse(baseline) as { topics: unknown[]; flashcards: unknown[] };
    assert.equal(dataset.topics.length, 10); assert.equal(dataset.flashcards.length, 300);
    const startDay = await checks.utcDay();
    for (const identity of identities) assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
    cleanupAllowed = true;
    const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN);
    const auth = { tokens, service: createAuthService(createAuthRepository(pool), createPasswordService(), tokens) };
    server = createApp({ database: createHealthRepository(pool), topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), progress: createProgressRepository(pool),
      quiz: createQuizRepository(pool), gamification }, auth).listen(0, '127.0.0.1');
    const running = server;
    await new Promise<void>((resolve, reject) => { running.once('listening', resolve); running.once('error', reject); });
    const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
    const users: z.infer<typeof authSchema>[] = [];
    for (const identity of identities) {
      const response = await fetch(base + '/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...identity, password }) });
      assert.equal(response.status, 201); const user = authSchema.parse(await response.json());
      users.push(user); registeredIds.set(identity.username, user.user.id);
    }
    const a = users[0]!; const b = users[1]!;
    const ownerA = { ...identities[0]!, id: a.user.id }; const ownerB = { ...identities[1]!, id: b.user.id };
    const request = (path: string, token = a.accessToken, method = 'GET', body?: unknown) => fetch(base + path, {
      method, headers: { Authorization: `Bearer ${token}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    async function ok(path: string, token = a.accessToken) {
      const response = await request(path, token); assert.equal(response.status, 200); return response.json();
    }
    const me = async (token = a.accessToken) => scoreSchema.parse(await ok('/gamification/me', token));
    async function answer(card: number, selected = card, token = a.accessToken) {
      const response = await request('/quiz/answer', token, 'POST', { flashcardId: card, selectedFlashcardId: selected });
      assert.equal(response.status, 200); return answerSchema.parse(await response.json());
    }
    async function review(card: number, correct: boolean, token = a.accessToken) {
      const response = await request(`/progress/${card}/review`, token, 'POST', { correct });
      assert.equal(response.status, 200); return progressSchema.parse(await response.json());
    }
    assert.deepEqual(await me(), { totalXp: 0, level: 1, xpIntoLevel: 0, xpPerLevel: 100,
      currentStreak: 0, longestStreak: 0, lastActivityDate: null });
    assert.equal(await checks.score(a.user.id), undefined); assert.equal(await checks.score(b.user.id), undefined);
    assert.deepEqual(historySchema.parse(await ok('/gamification/xp-history')), []);
    boardSchema.parse(await ok('/gamification/leaderboard'));
    const questions = z.array(z.object({ flashcardId: z.number().int().positive(), questionType: z.literal('EN_TO_VI'),
      prompt: z.string(), options: z.array(z.object({ flashcardId: z.number(), text: z.string() })).length(4) }).strict())
      .length(5).parse(await ok('/quiz/questions?topicCode=FOOD&limit=5'));
    assert.equal(new Set(questions.map((q) => q.flashcardId)).size, 5);
    assert.equal(await checks.score(a.user.id), undefined);
    const cards = await checks.foodCards(); assert.equal(cards.length, 30);
    const ids = [questions[0]!.flashcardId, ...cards.map((c) => c.id).filter((id) => id !== questions[0]!.flashcardId)];
    const c = (index: number) => ids[index]!;
    const favoriteResponse = await request(`/progress/${c(0)}/favorite`, a.accessToken, 'PUT', { isFavorite: true });
    assert.equal(favoriteResponse.status, 200); assert.equal(await checks.score(a.user.id), undefined);
    const login = await fetch(base + '/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: identities[0]!.email, password }) });
    assert.equal(login.status, 200); assert.equal(await checks.score(a.user.id), undefined);

    const first = await answer(c(0)); assert.equal(first.correct, true); assert.equal(first.progress.isFavorite, true);
    assert.equal(first.progress.reviewCount, 1); assert.equal(await checks.quizCount(a.user.id), 1);
    assert.equal(await checks.historyCount(a.user.id, c(0), 'QUIZ'), 1);
    assert.deepEqual(await me(), { totalXp: 10, level: 1, xpIntoLevel: 10, xpPerLevel: 100,
      currentStreak: 1, longestStreak: 1, lastActivityDate: startDay });
    const repeated = await answer(c(0)); assert.notEqual(repeated.resultId, first.resultId);
    assert.equal(repeated.progress.reviewCount, 2); assert.equal(repeated.progress.isFavorite, true);
    assert.equal(await checks.quizCount(a.user.id), 2); assert.equal((await me()).totalXp, 10);
    assert.equal(await checks.historyCount(a.user.id, c(0), 'QUIZ'), 1);
    const wrong = await answer(c(1), c(0)); assert.equal(wrong.correct, false);
    assert.equal((await me()).totalXp, 10); assert.equal((await me()).currentStreak, 1);
    assert.equal(await checks.historyCount(a.user.id, c(1)), 0);
    await answer(c(1)); assert.equal((await me()).totalXp, 20);
    await review(c(2), false); assert.equal((await me()).totalXp, 25);
    const repeatedReview = await review(c(2), true); assert.equal(repeatedReview.reviewCount, 2);
    assert.equal(repeatedReview.correctCount, 1); assert.equal(repeatedReview.wrongCount, 1);
    assert.equal((await me()).totalXp, 25); assert.equal(await checks.historyCount(a.user.id, c(2), 'REVIEW'), 1);
    await review(c(0), true); assert.equal((await me()).totalXp, 30);
    assert.equal(await checks.historyCount(a.user.id, c(0)), 2);

    // Wait for every concurrent request to settle BEFORE assertions/cleanup.
    async function concurrent(actions: Promise<unknown>[]) {
      const outcomes = await Promise.allSettled(actions);
      for (const outcome of outcomes) if (outcome.status === 'rejected') throw outcome.reason;
    }
    const quizBefore = await checks.quizCount(a.user.id);
    await concurrent(Array.from({ length: 6 }, () => answer(c(3))));
    assert.equal(await checks.quizCount(a.user.id), quizBefore + 6);
    const quizProgress = await checks.progressRecord(a.user.id, c(3)); assert.ok(quizProgress);
    assert.equal(quizProgress.review_count, 6); assert.equal(quizProgress.correct_count, 6);
    assert.equal(await checks.historyCount(a.user.id, c(3), 'QUIZ'), 1); assert.equal((await me()).totalXp, 40);
    await concurrent(Array.from({ length: 6 }, (_, i) => review(c(4), i % 2 === 0)));
    const reviewProgress = await checks.progressRecord(a.user.id, c(4)); assert.ok(reviewProgress);
    assert.equal(reviewProgress.review_count, 6); assert.equal(reviewProgress.correct_count, 3); assert.equal(reviewProgress.wrong_count, 3);
    assert.equal(await checks.historyCount(a.user.id, c(4), 'REVIEW'), 1); assert.equal((await me()).totalXp, 45);
    await concurrent([answer(c(5)), answer(c(6))]); assert.equal((await me()).totalXp, 65);
    assert.equal((await me()).currentStreak, 1);

    await answer(c(0), c(1), b.accessToken);
    assert.equal((await me(b.accessToken)).totalXp, 0); assert.equal((await me(b.accessToken)).currentStreak, 1);
    assert.equal(await checks.historyCount(b.user.id), 0);
    // Read full production ranking in this integration only, avoiding assumptions
    // that low-scoring test users necessarily appear in the public top 100.
    const boardService = createGamificationService(gamification);
    assert.ok(!(await boardService.leaderboard(2147483647)).some((row) => row.userId === b.user.id));
    await checks.setOwnedStreak(ownerA, 1, 3, 3);
    await answer(c(0), c(1)); // Wrong still advances streak, with no XP.
    assert.equal((await me()).currentStreak, 4); assert.equal((await me()).longestStreak, 4);
    await review(c(0), false); assert.equal((await me()).currentStreak, 4);
    await checks.setOwnedStreak(ownerA, 3, 4, 7);
    await review(c(0), true); assert.equal((await me()).currentStreak, 1); assert.equal((await me()).longestStreak, 7);
    assert.equal((await me()).lastActivityDate, startDay);

    await answer(c(0), c(0), b.accessToken);
    const different = boardSchema.parse(await boardService.leaderboard(2147483647));
    const rowA = different.find((row) => row.userId === a.user.id)!;
    const rowB = different.find((row) => row.userId === b.user.id)!;
    assert.ok(rowA.rank < rowB.rank); assert.ok(different.indexOf(rowA) < different.indexOf(rowB));
    const savedB = (await checks.score(b.user.id))!;
    try {
      await checks.setOwnedXp(ownerB, (await me()).totalXp);
      const tied = boardSchema.parse(await boardService.leaderboard(2147483647));
      const tiedA = tied.find((row) => row.userId === a.user.id)!;
      const tiedB = tied.find((row) => row.userId === b.user.id)!;
      assert.equal(tiedA.rank, tiedB.rank);
      for (let i = 0; i < tied.length; i++) {
        const row = tied[i]!;
        assert.equal(row.rank, tied.findIndex((item) => item.totalXp === row.totalXp) + 1);
        assert.equal(row.level, Math.floor(row.totalXp / 100) + 1);
        if (i > 0) { const prev = tied[i - 1]!; assert.ok(prev.totalXp >= row.totalXp);
          if (prev.totalXp === row.totalXp) assert.ok(prev.userId < row.userId); }
      }
      assert.deepEqual(boardSchema.parse(await ok('/gamification/leaderboard')), tied.slice(0, 20));
      assert.deepEqual(boardSchema.parse(await ok('/gamification/leaderboard?limit=1')), tied.slice(0, 1));
    } finally { await checks.restoreOwnedScore(ownerB, savedB); }

    const historyA = historySchema.parse(await ok(`/gamification/xp-history?limit=100&userId=${b.user.id}`));
    const historyB = historySchema.parse(await ok(`/gamification/xp-history?userId=${a.user.id}`, b.accessToken));
    assert.equal(historyA.length, await checks.historyCount(a.user.id)); assert.equal(historyB.length, 1);
    assert.ok(historyA.every((row) => !historyB.some((other) => other.id === row.id)));
    assert.ok(historyA.every((row) => row.activityDate === startDay));
    for (let i = 1; i < historyA.length; i++) {
      const prev = historyA[i - 1]!; const row = historyA[i]!;
      assert.ok(prev.createdAt > row.createdAt || (prev.createdAt === row.createdAt && prev.id > row.id));
    }
    assert.deepEqual(historySchema.parse(await ok('/gamification/xp-history?limit=1')), historyA.slice(0, 1));
    assert.equal(scoreSchema.parse(await ok(`/gamification/me?userId=${b.user.id}`)).totalXp, 65);

    const savedA = (await checks.score(a.user.id))!;
    try {
      await checks.setOwnedXp(ownerA, 2147483647);
      for (const [mode, card] of [['QUIZ', c(7)], ['REVIEW', c(8)]] as const) {
        assert.equal(await checks.historyCount(a.user.id, card), 0);
        const before = { quizzes: await checks.quizCount(a.user.id), history: await checks.historyCount(a.user.id),
          progress: await checks.progressRecord(a.user.id, card), score: await checks.score(a.user.id) };
        const response = mode === 'QUIZ'
          ? await request('/quiz/answer', a.accessToken, 'POST', { flashcardId: card, selectedFlashcardId: card })
          : await request(`/progress/${card}/review`, a.accessToken, 'POST', { correct: true });
        assert.equal(response.status, 500);
        assert.deepEqual(await response.json(), { error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' } });
        assert.deepEqual({ quizzes: await checks.quizCount(a.user.id), history: await checks.historyCount(a.user.id),
          progress: await checks.progressRecord(a.user.id, card), score: await checks.score(a.user.id) }, before);
      }
    } finally { await checks.restoreOwnedScore(ownerA, savedA); }
    assert.equal(await checks.utcDay(), startDay, 'Suite crossed UTC midnight; rerun within one UTC day');
  } finally {
    const cleanupErrors: unknown[] = [];
    if (server) {
      const running = server;
      try { await new Promise<void>((resolve, reject) => { running.close((error) => error ? reject(error) : resolve()); running.closeAllConnections(); }); }
      catch (error) { cleanupErrors.push(error); }
    }
    if (cleanupAllowed) {
      for (const identity of identities) {
        try {
          // Discover exact identity even if register committed but response parsing failed.
          const row = await checks.testUserRecord(identity.username, identity.email);
          const id = row?.id ?? registeredIds.get(identity.username);
          if (id !== undefined) {
            await checks.cleanupOwnedUser(id, identity.username, identity.email);
            assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
            assert.equal(await checks.score(id), undefined); assert.equal(await checks.historyCount(id), 0);
            assert.equal(await checks.userRowCount(id), 0); assert.equal(await checks.quizCount(id), 0);
          }
        } catch (error) { cleanupErrors.push(error); }
      }
    }
    if (baseline !== undefined) {
      try { assert.equal(await checks.datasetSnapshot(), baseline, 'Baseline dataset changed'); }
      catch (error) { cleanupErrors.push(error); }
    }
    try { await pool.end(); } catch (error) { cleanupErrors.push(error); }
    if (cleanupErrors.length) throw new AggregateError(cleanupErrors, 'M5 integration cleanup or dataset verification failed');
  }
});
