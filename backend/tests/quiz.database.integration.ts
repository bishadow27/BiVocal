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
import { createQuizIntegrationRepository } from './support/quiz-integration.repository.js';

const authSchema = z.object({ user: z.object({ id: z.number().int().positive(), username: z.string(),
  email: z.string(), fullName: z.string().nullable(), avatarUrl: z.string().nullable() }).strict(),
  accessToken: z.string() }).strict();
const questionSchema = z.object({ flashcardId: z.number().int().positive(), questionType: z.literal('EN_TO_VI'),
  prompt: z.string(), options: z.array(z.object({ flashcardId: z.number().int().positive(), text: z.string() }).strict()).length(4) }).strict();
const progressSchema = z.object({ flashcardId: z.number().int().positive(), memoryLevel: z.number().int().min(0).max(5),
  reviewCount: z.number().int().nonnegative(), correctCount: z.number().int().nonnegative(), wrongCount: z.number().int().nonnegative(),
  isFavorite: z.boolean(), lastReview: z.string().datetime().nullable(), nextReview: z.string().datetime().nullable(),
  createdAt: z.string().datetime(), updatedAt: z.string().datetime() }).strict();
const answerSchema = z.object({ resultId: z.number().int().positive(), flashcardId: z.number().int().positive(),
  selectedFlashcardId: z.number().int().positive(), correct: z.boolean(), correctAnswer: z.string(),
  selectedAnswer: z.string(), answeredAt: z.string().datetime(), progress: progressSchema }).strict();
const historySchema = z.object({ id: z.number().int().positive(), flashcardId: z.number().int().positive(),
  question: z.string(), selectedFlashcardId: z.number().int().positive().nullable(), selectedAnswer: z.string().nullable(),
  correctAnswer: z.string(), correct: z.boolean(), questionType: z.literal('EN_TO_VI'), answeredAt: z.string().datetime() }).strict();

it('Milestone 4 PostgreSQL Quiz lifecycle, concurrent answers, rollback and cleanup', async () => {
  if (process.env.QUIZ_INTEGRATION_ALLOW_WRITES !== '1') {
    throw new Error('Quiz integration requires QUIZ_INTEGRATION_ALLOW_WRITES=1 after quiz_results migration is applied');
  }
  const env = readEnv(process.env); const pool = createPool(env);
  const checks = createQuizIntegrationRepository(pool);
  const tag = `qz_${randomUUID().replaceAll('-', '').slice(0, 24)}`;
  const identities = ['a', 'b'].map((suffix) => {
    const username = `${tag}_${suffix}`; return { username, email: `${username}@example.com` };
  });
  const password = randomBytes(20).toString('hex');
  let baseline: string | undefined; let server: Server | undefined; let mayCleanup = false;
  const ownedIds = new Set<number>();
  try {
    assert.equal(await checks.quizMigrationExists(), true,
      'DATABASE MIGRATION NOT APPLIED: explicitly apply quiz_results before Quiz integration');
    assert.equal(await checks.usersMigrationExists(), true, 'Applied users migration is required');
    assert.equal(await checks.progressMigrationExists(), true, 'Applied user_progress migration is required');
    baseline = await checks.datasetSnapshot();
    for (const identity of identities) assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
    mayCleanup = true;
    const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN);
    const auth = { tokens, service: createAuthService(createAuthRepository(pool), createPasswordService(), tokens) };
    server = createApp({ database: createHealthRepository(pool), topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), progress: createProgressRepository(pool), quiz: createQuizRepository(pool) }, auth)
      .listen(0, '127.0.0.1');
    const running = server;
    await new Promise<void>((resolve, reject) => { running.once('listening', resolve); running.once('error', reject); });
    const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
    const users: z.infer<typeof authSchema>[] = [];
    for (const identity of identities) {
      const response = await fetch(base + '/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ ...identity, password }) });
      assert.equal(response.status, 201); const registered = authSchema.parse(await response.json());
      users.push(registered); ownedIds.add(registered.user.id);
    }
    const a = users[0]!; const b = users[1]!;
    const request = (path: string, token: string, method = 'GET', body?: unknown) => fetch(base + path, {
      method, headers: { Authorization: `Bearer ${token}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    });
    assert.equal(await checks.quizCount(a.user.id), 0);
    const questionsResponse = await request('/quiz/questions?topicCode=FOOD&limit=5', a.accessToken);
    assert.equal(questionsResponse.status, 200);
    const questions = z.array(questionSchema).length(5).parse(await questionsResponse.json());
    assert.equal(new Set(questions.map((q) => q.flashcardId)).size, 5);
    const food = await checks.foodCards(); assert.ok(food.length >= 5,
      'FOOD must contain enough active cards for a five-question integration quiz');
    const foodById = new Map(food.map((card) => [card.id, card]));
    for (const question of questions) {
      const card = foodById.get(question.flashcardId); assert.ok(card);
      assert.equal(question.prompt, card.english);
      assert.equal(new Set(question.options.map((option) => option.flashcardId)).size, 4);
      assert.equal(new Set(question.options.map((option) => option.text.trim().normalize('NFC').toLowerCase())).size, 4);
      assert.equal(question.options.filter((option) => option.flashcardId === question.flashcardId).length, 1);
      for (const option of question.options) {
        const selected = foodById.get(option.flashcardId); assert.ok(selected);
        assert.equal(selected.topic_id, card.topic_id); assert.equal(option.text, selected.vietnamese);
      }
    }
    assert.equal(await checks.quizCount(a.user.id), 0); assert.equal(await checks.userRowCount(a.user.id), 0);
    const question = questions[0]!;
    const wrongId = question.options.find((option) => option.flashcardId !== question.flashcardId)!.flashcardId;
    async function submit(selected: number, token = a.accessToken) {
      const response = await request('/quiz/answer', token, 'POST', {
        flashcardId: question.flashcardId, selectedFlashcardId: selected,
      });
      assert.equal(response.status, 200); return answerSchema.parse(await response.json());
    }
    const correct = await submit(question.flashcardId);
    assert.equal(correct.correct, true);
    assert.equal(correct.correctAnswer, foodById.get(question.flashcardId)!.vietnamese);
    const correctRow = await checks.resultRecord(a.user.id, correct.resultId); assert.ok(correctRow);
    assert.deepEqual([correctRow.user_id, correctRow.flashcard_id, correctRow.selected_flashcard_id, correctRow.is_correct],
      [a.user.id, question.flashcardId, question.flashcardId, true]);
    assert.equal(correctRow.answered_at.toISOString(), correct.answeredAt);
    let progress = await checks.progressRecord(a.user.id, question.flashcardId); assert.ok(progress);
    assert.deepEqual([progress.review_count, progress.correct_count, progress.wrong_count, progress.memory_level], [1, 1, 0, 1]);
    const wrong = await submit(wrongId); assert.equal(wrong.correct, false);
    assert.equal(wrong.selectedAnswer, foodById.get(wrongId)!.vietnamese);
    assert.notEqual(wrong.resultId, correct.resultId);
    const wrongRow = await checks.resultRecord(a.user.id, wrong.resultId); assert.ok(wrongRow);
    assert.equal(wrongRow.is_correct, false); assert.equal(wrongRow.selected_flashcard_id, wrongId);
    progress = await checks.progressRecord(a.user.id, question.flashcardId); assert.ok(progress);
    assert.deepEqual([progress.review_count, progress.correct_count, progress.wrong_count, progress.memory_level], [2, 1, 1, 0]);

    const favoriteResponse = await request(`/progress/${question.flashcardId}/favorite`, a.accessToken, 'PUT', { isFavorite: true });
    assert.equal(favoriteResponse.status, 200); progressSchema.parse(await favoriteResponse.json());
    const third = await submit(question.flashcardId);
    assert.equal(third.progress.isFavorite, true); assert.equal(third.progress.createdAt, correct.progress.createdAt);
    assert.ok(third.progress.nextReview);
    // Wait for every concurrent request to finish even when one fails, before cleanup.
    const concurrent = await Promise.allSettled(Array.from({ length: 6 }, () => submit(question.flashcardId)));
    assert.ok(concurrent.every((result) => result.status === 'fulfilled'));
    const resultIds = concurrent.flatMap((result) => result.status === 'fulfilled' ? [result.value.resultId] : []);
    assert.equal(new Set(resultIds).size, 6); assert.equal(await checks.quizCount(a.user.id), 9);
    progress = await checks.progressRecord(a.user.id, question.flashcardId); assert.ok(progress);
    assert.deepEqual([progress.review_count, progress.correct_count, progress.wrong_count, progress.memory_level], [9, 8, 1, 5]);
    assert.equal(progress.is_favorite, true); assert.ok(progress.next_review);
    assert.equal(progress.created_at.toISOString(), correct.progress.createdAt);

    // Force a failure AFTER quiz result insertion using only this test user's row.
    // No production hook, baseline change, trigger, or schema alteration.
    const originalProgress = progress;
    const identityA = identities[0]!;
    try {
      await checks.setOwnedCounters(a.user.id, identityA.username, identityA.email, question.flashcardId,
        2147483647, 2147483646, 1);
      const failure = await request('/quiz/answer', a.accessToken, 'POST', {
        flashcardId: question.flashcardId, selectedFlashcardId: question.flashcardId,
      });
      assert.equal(failure.status, 500);
      assert.deepEqual(await failure.json(), { error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' } });
      assert.equal(await checks.quizCount(a.user.id), 9, 'Failed progress upsert must roll back the preceding result INSERT');
      const afterFailure = await checks.progressRecord(a.user.id, question.flashcardId); assert.ok(afterFailure);
      assert.deepEqual([afterFailure.review_count, afterFailure.correct_count, afterFailure.wrong_count], [2147483647, 2147483646, 1]);
      assert.equal(afterFailure.last_review?.toISOString(), originalProgress.last_review?.toISOString());
      assert.equal(afterFailure.updated_at.toISOString(), originalProgress.updated_at.toISOString());
    } finally {
      await checks.setOwnedCounters(a.user.id, identityA.username, identityA.email, question.flashcardId,
        originalProgress.review_count, originalProgress.correct_count, originalProgress.wrong_count);
    }

    const historyResponse = await request('/quiz/history?limit=5', a.accessToken);
    assert.equal(historyResponse.status, 200); const history = z.array(historySchema).length(5).parse(await historyResponse.json());
    for (let n = 1; n < history.length; n++) {
      const previous = history[n - 1]!; const next = history[n]!;
      assert.ok(previous.answeredAt > next.answeredAt || (previous.answeredAt === next.answeredAt && previous.id > next.id));
    }
    const summary = await request('/quiz/summary', a.accessToken); assert.equal(summary.status, 200);
    assert.deepEqual(await summary.json(), { totalAnswers: 9, correctAnswers: 8, wrongAnswers: 1, accuracy: 88.89 });
    const bHistory = await request(`/quiz/history?userId=${a.user.id}`, b.accessToken);
    assert.equal(bHistory.status, 200); assert.deepEqual(await bHistory.json(), []);
    const bSummary = await request(`/quiz/summary?userId=${a.user.id}`, b.accessToken);
    assert.equal(bSummary.status, 200);
    assert.deepEqual(await bSummary.json(), { totalAnswers: 0, correctAnswers: 0, wrongAnswers: 0, accuracy: null });
    const override = await request('/quiz/answer', b.accessToken, 'POST', {
      flashcardId: question.flashcardId, selectedFlashcardId: question.flashcardId, userId: a.user.id,
    });
    assert.equal(override.status, 400);
    const bAnswer = await submit(wrongId, b.accessToken); assert.equal(bAnswer.correct, false);
    assert.equal(await checks.quizCount(b.user.id), 1); assert.equal(await checks.quizCount(a.user.id), 9);
    assert.equal((await checks.progressRecord(a.user.id, question.flashcardId))?.review_count, 9);

    const topics = await fetch(base + '/topics'); assert.equal(topics.status, 200);
    assert.ok(z.array(z.unknown()).parse(await topics.json()).length >= 10);
    const foodResponse = await fetch(base + '/topics/FOOD/flashcards'); assert.equal(foodResponse.status, 200);
    assert.ok(z.array(z.unknown()).parse(await foodResponse.json()).length >= food.length);
    assert.equal(await checks.datasetSnapshot(), baseline);
  } finally {
    let cleanupFailed = false;
    try {
      if (mayCleanup) {
        for (const identity of identities) {
          try {
            const owned = await checks.testUserRecord(identity.username, identity.email);
            if (owned) {
              ownedIds.add(owned.id);
              await checks.cleanupOwnedUser(owned.id, identity.username, identity.email);
              assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
            }
          } catch { cleanupFailed = true; }
        }
        for (const id of ownedIds) {
          try {
            assert.equal(await checks.quizCount(id), 0, 'Quiz results must be removed by user FK cascade');
            assert.equal(await checks.userRowCount(id), 0, 'Progress must be removed by user FK cascade');
          } catch { cleanupFailed = true; }
        }
      }
      if (baseline !== undefined) assert.equal(await checks.datasetSnapshot(), baseline);
      if (cleanupFailed) throw new Error('Quiz integration cleanup did not complete; inspect UUID test identities locally');
    } finally {
      try {
        if (server) {
          const running = server;
          await new Promise<void>((resolve, reject) => {
            running.close((error) => error ? reject(error) : resolve()); running.closeAllConnections();
          });
        }
      } finally { await pool.end(); }
    }
  }
});
