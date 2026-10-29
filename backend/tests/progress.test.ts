import assert from 'node:assert/strict';
import { afterEach, beforeEach, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { AuthModule } from '../src/modules/auth/auth.service.js';
import type { ProgressRepository } from '../src/modules/progress/progress.repository.js';
import type { ProgressRow, ProgressDto, ProgressSummaryDto } from '../src/modules/progress/progress.types.js';
import type { Repositories } from '../src/routes.js';

const secret = 'progress-test-only-secret-over-32-characters';
const tokens = createTokenService(secret, 3600);
const userA = 41;
const userB = 42;
const auth: AuthModule = { tokens, service: {
  async register() { throw new Error('Auth is outside this repository double'); },
  async login() { throw new Error('Auth is outside this repository double'); },
  async currentUser() { throw new Error('Auth is outside this repository double'); },
} as unknown as AuthModule['service'] };

// A functional in-memory repository double. It does not execute or verify SQL;
// PostgreSQL atomicity/constraints are exercised only by the separate integration.
function progressDouble() {
  const rows = new Map<string, ProgressRow>();
  const active = new Set([10, 11, 12, 13, 14]);
  const calls: { operation: string; userId: number; flashcardId?: number }[] = [];
  let tick = Date.parse('2026-10-04T00:00:00.000Z');
  let failure: unknown;
  const key = (userId: number, flashcardId: number) => `${userId}:${flashcardId}`;
  function newRow(flashcardId: number): ProgressRow {
    const now = new Date(++tick);
    return { flashcard_id: flashcardId, memory_level: 0, review_count: 0,
      correct_count: 0, wrong_count: 0, is_favorite: false,
      last_review: null, next_review: null, created_at: now, updated_at: now };
  }
  function forUser(userId: number) {
    return [...rows.entries()].filter(([id, row]) =>
      id.startsWith(`${userId}:`) && active.has(row.flashcard_id)).map(([, row]) => row);
  }
  function record(operation: string, userId: number, flashcardId?: number) {
    calls.push({ operation, userId, ...(flashcardId === undefined ? {} : { flashcardId }) });
    if (failure) throw failure;
  }
  const repository: ProgressRepository = {
    async activeFlashcardExists(id) { record('active', 0, id); return active.has(id); },
    async findByUserAndFlashcard(userId, id) {
      record('detail', userId, id); return active.has(id) ? rows.get(key(userId, id)) : undefined;
    },
    async listByUser(userId) {
      record('list', userId);
      return forUser(userId).sort((a, b) =>
        b.updated_at.getTime() - a.updated_at.getTime() || a.flashcard_id - b.flashcard_id);
    },
    async summaryByUser(userId) {
      record('summary', userId);
      const items = forUser(userId);
      const sum = (field: 'review_count' | 'correct_count' | 'wrong_count') =>
        String(items.reduce((total, row) => total + row[field], 0));
      return { tracked_cards: String(items.length),
        reviewed_cards: String(items.filter((row) => row.review_count > 0).length),
        favorite_cards: String(items.filter((row) => row.is_favorite).length),
        mastered_cards: String(items.filter((row) => row.memory_level === 5).length),
        total_reviews: sum('review_count'), correct_answers: sum('correct_count'),
        wrong_answers: sum('wrong_count') };
    },
    async dueByUser(userId, limit, topicCode) {
      record('due', userId);
      if (topicCode && topicCode !== 'FOOD') return [];
      const now = new Date(tick);
      return forUser(userId).filter(row => row.review_count > 0 &&
        (!row.next_review || row.next_review <= now))
        .sort((a, b) => (a.next_review?.getTime() ?? 0) - (b.next_review?.getTime() ?? 0))
        .slice(0, limit).map(row => ({
          id: row.flashcard_id, english: `word-${row.flashcard_id}`, vietnamese: 'nghĩa',
          pronunciation: null, part_of_speech: null, example: null, example_vi: null,
          level: null, topic_code: 'FOOD', memory_level: row.memory_level,
          review_count: row.review_count, wrong_count: row.wrong_count,
          last_review: row.last_review, next_review: row.next_review,
        }));
    },
    async scheduleByUser(userId) {
      record('schedule', userId);
      const items = forUser(userId);
      const reviewed = items.filter(row => row.review_count > 0);
      const now = new Date(tick);
      const countRows = (predicate: (row: ProgressRow) => boolean) => String(reviewed.filter(predicate).length);
      return { tracked_cards: String(items.length), reviewed_cards: String(reviewed.length),
        due_now: countRows(row => !row.next_review || row.next_review <= now),
        upcoming_24_hours: countRows(row => !!row.next_review && row.next_review > now &&
          row.next_review.getTime() <= now.getTime() + 86400000),
        reviewed_today: countRows(row => !!row.last_review && row.last_review.toISOString().slice(0, 10) === now.toISOString().slice(0, 10)),
        level_0: countRows(row => row.memory_level === 0), level_1: countRows(row => row.memory_level === 1),
        level_2: countRows(row => row.memory_level === 2), level_3: countRows(row => row.memory_level === 3),
        level_4: countRows(row => row.memory_level === 4), level_5: countRows(row => row.memory_level === 5) };
    },
    async review(userId, id, correct) {
      record('review', userId, id);
      if (!active.has(id)) return undefined;
      const row = rows.get(key(userId, id)) ?? newRow(id);
      row.review_count++;
      if (correct) { row.correct_count++; row.memory_level = Math.min(5, row.memory_level + 1); }
      else { row.wrong_count++; row.memory_level = Math.max(0, row.memory_level - 1); }
      row.last_review = row.updated_at = new Date(++tick);
      row.next_review = new Date(row.last_review.getTime() + (correct
        ? [0, 1, 3, 7, 14, 30][row.memory_level]! * 86400000 : 600000));
      rows.set(key(userId, id), row);
      return row;
    },
    async favorite(userId, id, favorite) {
      record('favorite', userId, id);
      if (!active.has(id)) return undefined;
      const row = rows.get(key(userId, id)) ?? newRow(id);
      row.is_favorite = favorite; row.updated_at = new Date(++tick);
      rows.set(key(userId, id), row);
      return row;
    },
  };
  return { repository, rows, active, calls,
    fail(error: unknown) { failure = error; },
    seed(userId: number, id: number, fields: Partial<ProgressRow> = {}) {
      const row = { ...newRow(id), ...fields }; rows.set(key(userId, id), row); return row;
    },
  };
}

describe('Milestone 3 progress HTTP API (real JWT; repository doubles; no PostgreSQL)', () => {
  let server: Server;
  let base: string;
  let fake: ReturnType<typeof progressDouble>;

  beforeEach(async () => {
    fake = progressDouble();
    const repositories: Repositories = { progress: fake.repository, database: { async ping() {} },
      topics: { async findAll() { return []; }, async findCatalog() { return []; }, async findByCode() { return undefined; } },
      flashcards: { async findById() { return undefined; }, async findByTopicCode() { return []; },
        async search() { return { rows: [], total: 0 }; } } };
    server = createApp(repositories, auth).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => {
      server.once('listening', resolve); server.once('error', reject);
    });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1/progress`;
  });
  afterEach(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => error ? reject(error) : resolve()); server.closeAllConnections();
    });
  });

  function request(path: string, method = 'GET', body?: unknown,
    authorization: string | null = `Bearer ${tokens.sign(userA)}`, extraHeaders: Record<string, string> = {}) {
    const headers: Record<string, string> = { ...extraHeaders };
    if (authorization !== null) headers.Authorization = authorization;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    return fetch(base + path, { method, headers,
      ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
  }
  async function readDto(response: globalThis.Response): Promise<ProgressDto> {
    assert.equal(response.status, 200); return await response.json() as ProgressDto;
  }
  const review = (correct: boolean, id = 10) => request(`/${id}/review`, 'POST', { correct });
  const favorite = (isFavorite: boolean, id = 10) => request(`/${id}/favorite`, 'PUT', { isFavorite });
  const endpoints: [string, string, unknown?][] = [
    ['', 'GET'], ['/summary', 'GET'], ['/due', 'GET'], ['/schedule', 'GET'], ['/10', 'GET'],
    ['/10/review', 'POST', { correct: true }], ['/10/favorite', 'PUT', { isFavorite: true }],
  ];
  async function expectError(response: globalThis.Response, status: number, code: string) {
    assert.equal(response.status, status);
    const body = await response.json() as { error: { code: string } };
    assert.equal(body.error.code, code);
  }

  it('requires JWT on every progress endpoint', async () => {
    for (const [path, method, body] of endpoints) {
      await expectError(await request(path, method, body, null), 401, 'AUTH_TOKEN_MISSING');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('rejects invalid JWT on every endpoint', async () => {
    const token = createTokenService('another-test-only-secret-over-32-characters', 3600).sign(userA);
    for (const [path, method, body] of endpoints) {
      await expectError(await request(path, method, body, `Bearer ${token}`), 401, 'AUTH_TOKEN_INVALID');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('rejects expired JWT on every endpoint', async () => {
    const token = jwt.sign({ sub: String(userA), exp: 1 }, secret, { algorithm: 'HS256' });
    for (const [path, method, body] of endpoints) {
      await expectError(await request(path, method, body, `Bearer ${token}`), 401, 'AUTH_TOKEN_INVALID');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('rejects malformed Authorization headers', async () => {
    for (const header of ['Basic credentials', 'Bearer', 'Bearer a b', 'Bearer invalid']) {
      await expectError(await request('', 'GET', undefined, header), 401, 'AUTH_TOKEN_INVALID');
    }
  });
  it('authenticates before parsing a mutation body', async () => {
    const response = await fetch(base + '/10/review', {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{invalid-json',
    });
    await expectError(response, 401, 'AUTH_TOKEN_MISSING');
    assert.equal(fake.calls.length, 0);
  });
  it('rejects malformed/negative/decimal/overflow IDs on all detail and mutation routes', async () => {
    for (const id of ['0', '-1', '1.5', '1abc', '01', '1e3', '2147483648', '9'.repeat(400)]) {
      await expectError(await request(`/${id}`), 400, 'INVALID_FLASHCARD_ID');
      await expectError(await request(`/${id}/review`, 'POST', { correct: true }), 400, 'INVALID_FLASHCARD_ID');
      await expectError(await request(`/${id}/favorite`, 'PUT', { isFavorite: true }), 400, 'INVALID_FLASHCARD_ID');
    }
    assert.equal(fake.calls.length, 0); assert.equal(fake.rows.size, 0);
  });
  it('accepts the SERIAL upper boundary as valid format, then returns not found', async () => {
    await expectError(await request('/2147483647'), 404, 'FLASHCARD_NOT_FOUND');
  });
  it('rejects missing correct', async () => {
    for (const body of [undefined, {}, null, []]) {
      await expectError(await request('/10/review', 'POST', body), 400,
        body === null ? 'INVALID_JSON' : 'VALIDATION_ERROR');
    }
    assert.equal(fake.rows.size, 0);
  });
  it('requires correct to be a boolean without coercion', async () => {
    for (const correct of ['true', 'false', 0, 1, null, [], {}]) {
      await expectError(await request('/10/review', 'POST', { correct }), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('rejects unknown review fields including client-supplied userId', async () => {
    for (const body of [{ correct: true, userId: userB }, { correct: true, extra: 'x' }]) {
      await expectError(await request('/10/review', 'POST', body), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0); assert.equal(fake.rows.size, 0);
  });
  it('rejects unknown favorite fields including client-supplied userId', async () => {
    await expectError(await request('/10/favorite', 'PUT', { isFavorite: true, userId: userB }),
      400, 'VALIDATION_ERROR');
    assert.equal(fake.calls.length, 0);
  });
  it('requires favorite boolean without coercion', async () => {
    for (const body of [{}, { isFavorite: 'true' }, { isFavorite: 1 }, { isFavorite: null }]) {
      await expectError(await request('/10/favorite', 'PUT', body), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('handles malformed JSON safely', async () => {
    const response = await fetch(base + '/10/review', { method: 'POST',
      headers: { Authorization: `Bearer ${tokens.sign(userA)}`, 'Content-Type': 'application/json' },
      body: '{"correct":' });
    await expectError(response, 400, 'INVALID_JSON');
    assert.equal(fake.calls.length, 0);
  });
  it('limits mutation body sizes', async () => {
    await expectError(await request('/10/favorite', 'PUT', { isFavorite: true, extra: 'x'.repeat(17000) }),
      413, 'PAYLOAD_TOO_LARGE');
  });
  it('returns flashcard not found for nonexistent cards and never creates progress', async () => {
    await expectError(await request('/999'), 404, 'FLASHCARD_NOT_FOUND');
    await expectError(await review(true, 999), 404, 'FLASHCARD_NOT_FOUND');
    await expectError(await favorite(true, 999), 404, 'FLASHCARD_NOT_FOUND');
    assert.equal(fake.rows.size, 0);
  });
  it('returns flashcard not found for inactive cards and preserves existing progress', async () => {
    const original = fake.seed(userA, 10, { review_count: 2, correct_count: 2, memory_level: 2 });
    const before = structuredClone(original); fake.active.delete(10);
    await expectError(await request('/10'), 404, 'FLASHCARD_NOT_FOUND');
    await expectError(await review(false), 404, 'FLASHCARD_NOT_FOUND');
    await expectError(await favorite(true), 404, 'FLASHCARD_NOT_FOUND');
    assert.deepEqual(original, before); assert.equal(fake.rows.size, 1);
  });
  it('first correct review creates expected counters and memory level', async () => {
    const dto = await readDto(await review(true));
    assert.deepEqual([dto.reviewCount, dto.correctCount, dto.wrongCount, dto.memoryLevel], [1, 1, 0, 1]);
    assert.equal(dto.isFavorite, false); assert.equal(fake.rows.size, 1);
  });
  it('first wrong review creates expected counters and memory floor', async () => {
    const dto = await readDto(await review(false));
    assert.deepEqual([dto.reviewCount, dto.correctCount, dto.wrongCount, dto.memoryLevel], [1, 0, 1, 0]);
  });
  it('correct review increments review and correct counters', async () => {
    await readDto(await review(false));
    const dto = await readDto(await review(true));
    assert.deepEqual([dto.reviewCount, dto.correctCount, dto.wrongCount, dto.memoryLevel], [2, 1, 1, 1]);
  });
  it('wrong review increments review and wrong counters and decrements memory', async () => {
    await readDto(await review(true)); await readDto(await review(true));
    const dto = await readDto(await review(false));
    assert.deepEqual([dto.reviewCount, dto.correctCount, dto.wrongCount, dto.memoryLevel], [3, 2, 1, 1]);
  });
  it('caps memory level at five while continuing to count correct reviews', async () => {
    for (let n = 0; n < 7; n++) await readDto(await review(true));
    const dto = await readDto(await request('/10'));
    assert.equal(dto.memoryLevel, 5); assert.equal(dto.reviewCount, 7); assert.equal(dto.correctCount, 7);
  });
  it('floors memory level at zero while continuing to count wrong reviews', async () => {
    await readDto(await review(true));
    for (let n = 0; n < 3; n++) await readDto(await review(false));
    const dto = await readDto(await request('/10'));
    assert.equal(dto.memoryLevel, 0); assert.equal(dto.reviewCount, 4); assert.equal(dto.wrongCount, 3);
  });
  it('review preserves favorite state', async () => {
    await readDto(await favorite(true));
    assert.equal((await readDto(await review(true))).isFavorite, true);
    assert.equal((await readDto(await review(false))).isFavorite, true);
  });
  it('review preserves createdAt and updates updatedAt and lastReview', async () => {
    const first = await readDto(await review(true));
    const next = await readDto(await review(false));
    assert.equal(next.createdAt, first.createdAt);
    assert.ok(next.updatedAt > first.updatedAt);
    assert.ok(next.lastReview !== null); assert.equal(next.lastReview, next.updatedAt);
  });
  it('review schedules nextReview from its resulting level', async () => {
    const dto = await readDto(await review(true));
    assert.ok(dto.lastReview); assert.equal(new Date(dto.lastReview).toISOString(), dto.lastReview);
    assert.equal(Date.parse(dto.nextReview!), Date.parse(dto.lastReview) + 86400000);
  });
  it('favorite true creates progress with zero counters and no review dates', async () => {
    const dto = await readDto(await favorite(true));
    assert.deepEqual([dto.memoryLevel, dto.reviewCount, dto.correctCount, dto.wrongCount], [0, 0, 0, 0]);
    assert.equal(dto.isFavorite, true); assert.equal(dto.lastReview, null); assert.equal(dto.nextReview, null);
  });
  it('favorite false creates a default row and is idempotent for counters', async () => {
    const first = await readDto(await favorite(false)); const second = await readDto(await favorite(false));
    assert.equal(second.isFavorite, false); assert.equal(second.reviewCount, 0);
    assert.equal(second.createdAt, first.createdAt); assert.equal(fake.rows.size, 1);
  });
  it('favorite true/false preserves review counters, memory and lastReview', async () => {
    await readDto(await review(true)); const reviewed = await readDto(await review(false));
    const marked = await readDto(await favorite(true)); const cleared = await readDto(await favorite(false));
    assert.equal(marked.isFavorite, true); assert.equal(cleared.isFavorite, false);
    for (const dto of [marked, cleared]) {
      assert.deepEqual([dto.reviewCount, dto.correctCount, dto.wrongCount, dto.memoryLevel, dto.lastReview],
        [reviewed.reviewCount, reviewed.correctCount, reviewed.wrongCount, reviewed.memoryLevel, reviewed.lastReview]);
      assert.equal(dto.createdAt, reviewed.createdAt); assert.ok(dto.updatedAt > reviewed.updatedAt);
    }
  });
  it('detail returns the current user progress', async () => {
    const dto = await readDto(await review(true));
    assert.deepEqual(await readDto(await request('/10')), dto);
  });
  it('detail of an active card with no progress returns PROGRESS_NOT_FOUND', async () => {
    await expectError(await request('/10'), 404, 'PROGRESS_NOT_FOUND'); assert.equal(fake.rows.size, 0);
  });
  it('user B cannot read user A detail', async () => {
    await readDto(await review(true));
    await expectError(await request('/10', 'GET', undefined, `Bearer ${tokens.sign(userB)}`),
      404, 'PROGRESS_NOT_FOUND');
  });
  it('users can independently track the same card', async () => {
    await readDto(await review(true));
    const b = await readDto(await request('/10/review', 'POST', { correct: false }, `Bearer ${tokens.sign(userB)}`));
    assert.equal(b.wrongCount, 1); assert.equal(b.correctCount, 0);
    const a = await readDto(await request('/10')); assert.equal(a.correctCount, 1); assert.equal(a.wrongCount, 0);
    assert.equal(fake.rows.size, 2);
  });
  it('list isolates the JWT user and returns camelCase DTOs', async () => {
    fake.seed(userA, 10); fake.seed(userA, 11); fake.seed(userB, 12);
    const a = await request(''); assert.equal(a.status, 200);
    const list = await a.json() as ProgressDto[];
    assert.deepEqual(list.map((row) => row.flashcardId), [11, 10]);
    assert.ok(!JSON.stringify(list).includes('userId')); assert.ok(!JSON.stringify(list).includes('user_id'));
    assert.ok(!JSON.stringify(list).includes('flashcard_id'));
    const b = await request('', 'GET', undefined, `Bearer ${tokens.sign(userB)}`);
    assert.deepEqual((await b.json() as ProgressDto[]).map((row) => row.flashcardId), [12]);
  });
  it('list uses stable updatedAt descending then flashcardId ascending ordering', async () => {
    const date = new Date('2026-10-04T01:00:00.000Z');
    fake.seed(userA, 12, { updated_at: date }); fake.seed(userA, 10, { updated_at: date });
    fake.seed(userA, 11, { updated_at: new Date('2026-10-04T02:00:00.000Z') });
    assert.deepEqual((await (await request('')).json() as ProgressDto[]).map((row) => row.flashcardId), [11, 10, 12]);
  });
  it('list and summary exclude inactive cards without deleting their progress', async () => {
    fake.seed(userA, 10, { review_count: 2, correct_count: 2, is_favorite: true });
    fake.seed(userA, 11); fake.active.delete(10);
    assert.deepEqual((await (await request('')).json() as ProgressDto[]).map((row) => row.flashcardId), [11]);
    const summary = await (await request('/summary')).json() as ProgressSummaryDto;
    assert.equal(summary.trackedCards, 1); assert.equal(summary.totalReviews, 0);
    assert.equal(summary.favoriteCards, 0); assert.equal(fake.rows.size, 2);
  });
  it('empty list returns an array', async () => {
    const response = await request(''); assert.equal(response.status, 200); assert.deepEqual(await response.json(), []);
  });
  it('summary returns all counts and rounded accuracy for only the JWT user', async () => {
    fake.seed(userA, 10, { review_count: 20, correct_count: 15, wrong_count: 5, memory_level: 5, is_favorite: true });
    fake.seed(userA, 11, { review_count: 22, correct_count: 15, wrong_count: 7, memory_level: 3 });
    fake.seed(userA, 12, { is_favorite: true });
    fake.seed(userB, 13, { review_count: 100, correct_count: 100, memory_level: 5, is_favorite: true });
    const response = await request('/summary'); assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { trackedCards: 3, reviewedCards: 2, favoriteCards: 2,
      masteredCards: 1, totalReviews: 42, correctAnswers: 30, wrongAnswers: 12, accuracy: 71.43 });
  });
  it('empty summary has zero counts and null accuracy', async () => {
    assert.deepEqual(await (await request('/summary')).json(), { trackedCards: 0, reviewedCards: 0,
      favoriteCards: 0, masteredCards: 0, totalReviews: 0, correctAnswers: 0, wrongAnswers: 0, accuracy: null });
  });
  it('favorite-only summary has null accuracy and zero reviewedCards', async () => {
    await readDto(await favorite(true));
    assert.deepEqual(await (await request('/summary')).json(), { trackedCards: 1, reviewedCards: 0,
      favoriteCards: 1, masteredCards: 0, totalReviews: 0, correctAnswers: 0, wrongAnswers: 0, accuracy: null });
  });
  it('due list includes legacy reviewed rows, excludes favorite-only and isolates users', async () => {
    fake.seed(userA, 10, { review_count: 1, memory_level: 2, next_review: null });
    fake.seed(userA, 11, { review_count: 1, next_review: new Date('2026-10-05T00:00:00.000Z') });
    fake.seed(userA, 12, { is_favorite: true });
    fake.seed(userB, 13, { review_count: 1, next_review: null });
    const response = await request('/due?limit=1');
    assert.equal(response.status, 200);
    const rows = await response.json() as { id: number; memoryLevel: number }[];
    assert.deepEqual(rows.map(row => row.id), [10]);
    assert.equal(rows[0]?.memoryLevel, 2);
    assert.deepEqual(await (await request('/due?topicCode=ANIMALS')).json(), []);
    assert.deepEqual(await (await request('/schedule')).json(), {
      trackedCards: 3, reviewedCards: 2, dueNow: 1, upcoming24Hours: 1,
      reviewedToday: 0, memoryLevels: [1, 0, 1, 0, 0, 0],
    });
  });
  it('validates due filters and never accepts a client user selector', async () => {
    for (const query of ['limit=0', 'limit=301', 'limit=-1', 'limit=1.5',
      'topicCode=../FOOD', 'userId=42', 'limit=1&limit=2']) {
      await expectError(await request(`/due?${query}`), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('all-wrong summary has zero accuracy rather than null', async () => {
    await readDto(await review(false));
    const summary = await (await request('/summary')).json() as ProgressSummaryDto;
    assert.equal(summary.accuracy, 0); assert.equal(summary.wrongAnswers, 1);
  });
  it('ignores arbitrary query/header user IDs on reads and writes', async () => {
    fake.seed(userB, 11, { review_count: 3, correct_count: 3 });
    await readDto(await request(`/${10}/review?userId=${userB}`, 'POST', { correct: true },
      undefined, { 'X-User-Id': String(userB) }));
    await readDto(await request(`/10/favorite?userId=${userB}`, 'PUT', { isFavorite: true }));
    await expectError(await request(`/11?userId=${userB}`), 404, 'PROGRESS_NOT_FOUND');
    const list = await (await request(`?userId=${userB}`)).json() as ProgressDto[];
    assert.deepEqual(list.map((row) => row.flashcardId), [10]);
    const summary = await (await request(`/summary?userId=${userB}`)).json() as ProgressSummaryDto;
    assert.equal(summary.totalReviews, 1);
    assert.ok(fake.calls.filter((call) => call.operation !== 'active').every((call) => call.userId === userA));
    assert.equal(fake.rows.get(`${userB}:11`)?.review_count, 3);
  });
  it('does not expose internal row fields or credentials in any DTO', async () => {
    const row = fake.seed(userA, 10);
    Object.assign(row, { id: 999, user_id: userA, password_hash: 'private-test-hash',
      DB_PASSWORD: 'private-test-db-password', JWT_SECRET: secret });
    for (const response of [await request('/10'), await request(''), await request('/summary'),
      await review(true), await favorite(true)]) {
      assert.equal(response.status, 200); const text = await response.text();
      for (const hidden of ['user_id', 'userId', 'password_hash', 'private-test-hash',
        'private-test-db-password', secret, 'DB_PASSWORD', 'JWT_SECRET']) assert.ok(!text.includes(hidden));
    }
    const dto = await readDto(await request('/10'));
    assert.deepEqual(Object.keys(dto).sort(), ['flashcardId', 'memoryLevel', 'reviewCount', 'correctCount',
      'wrongCount', 'isFavorite', 'lastReview', 'nextReview', 'createdAt', 'updatedAt'].sort());
  });
  it('unexpected repository errors return a generic 500 on every endpoint', async () => {
    fake.fail(new Error(`private SQL/password_hash DB_PASSWORD=private-test-db-password JWT_SECRET=${secret}`));
    for (const [path, method, body] of endpoints) {
      const response = await request(path, method, body);
      assert.equal(response.status, 500);
      assert.deepEqual(await response.json(), { error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' } });
    }
  });
});
