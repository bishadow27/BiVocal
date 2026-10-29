import assert from 'node:assert/strict';
import { beforeEach, afterEach, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { AuthModule } from '../src/modules/auth/auth.service.js';
import type { GamificationRepository } from '../src/modules/gamification/gamification.repository.js';
import type { ScoreRow, XpHistoryRow, LeaderboardRow } from '../src/modules/gamification/gamification.types.js';
import { levelForXp } from '../src/modules/gamification/gamification.service.js';

const secret = 'gamification-synthetic-unit-test-secret-only';
const tokens = createTokenService(secret, 3600);
const auth: AuthModule = { tokens, service: {
  async register() { throw new Error('Unused'); }, async login() { throw new Error('Unused'); },
  async currentUser() { throw new Error('Unused'); },
} as unknown as AuthModule['service'] };
const empty = { totalXp: 0, level: 1, xpIntoLevel: 0, xpPerLevel: 100,
  currentStreak: 0, longestStreak: 0, lastActivityDate: null };

describe('Milestone 5 HTTP API (real JWT, repository doubles, no database)', () => {
  let server: Server; let base: string;
  let scores: Map<number, ScoreRow>; let history: Map<number, XpHistoryRow[]>;
  let board: LeaderboardRow[]; let calls: unknown[][]; let fail: boolean;
  beforeEach(async () => {
    scores = new Map(); history = new Map(); board = []; calls = []; fail = false;
    const record = (...args: unknown[]) => { calls.push(args); if (fail) throw new Error('private SQL password_hash secret'); };
    const repository: GamificationRepository = {
      async findScore(id) { record('score', id); return scores.get(id); },
      async history(id, limit) { record('history', id, limit); return (history.get(id) ?? []).slice(0, limit); },
      async leaderboard(limit) { record('board', limit); return board.slice(0, limit); },
    };
    server = createApp({ gamification: repository, database: { async ping() {} },
      topics: { async findAll() { return []; }, async findCatalog() { return []; }, async findByCode() { return undefined; } },
      flashcards: { async findById() { return undefined; }, async findByTopicCode() { return []; },
        async search() { return { rows: [], total: 0 }; } } }, auth)
      .listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => { server.once('listening', resolve); server.once('error', reject); });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1/gamification`;
  });
  afterEach(async () => {
    await new Promise<void>((resolve, reject) => { server.close((error) => error ? reject(error) : resolve()); server.closeAllConnections(); });
  });
  const paths = ['/me', '/xp-history', '/leaderboard'];
  const get = (path: string, token: string | null = tokens.sign(41)) => fetch(base + path,
    { headers: { ...(token === null ? {} : { Authorization: `Bearer ${token}` }), 'X-User-Id': '999' } });
  async function json(path: string, token = tokens.sign(41)) {
    const response = await get(path, token); assert.equal(response.status, 200); return response.json();
  }
  for (const [name, token] of [ ['missing', null], ['invalid', 'invalid'],
    ['expired', jwt.sign({ sub: '41', exp: 1 }, secret)] ] as const) {
    it(`rejects ${name} JWT on all three endpoints before repository access`, async () => {
      for (const path of paths) assert.equal((await get(path, token)).status, 401);
      assert.deepEqual(calls, []);
    });
  }
  it('missing score returns exact zeros without creating anything', async () => {
    assert.deepEqual(await json('/me'), empty); assert.deepEqual(await json('/me'), empty);
    assert.equal(scores.size, 0); assert.deepEqual(calls, [['score', 41], ['score', 41]]);
  });
  it('maps stored score and derives level with no raw/private fields', async () => {
    scores.set(41, { total_xp: 125, current_streak: 4, longest_streak: 7, last_activity_date: '2026-10-05' });
    assert.deepEqual(await json('/me'), { totalXp: 125, level: 2, xpIntoLevel: 25, xpPerLevel: 100,
      currentStreak: 4, longestStreak: 7, lastActivityDate: '2026-10-05' });
  });
  for (const [xp, level, into] of [[0, 1, 0], [99, 1, 99], [100, 2, 0], [250, 3, 50]]) {
    it(`derives level and remainder for ${xp} XP`, async () => {
      scores.set(41, { total_xp: xp!, current_streak: 1, longest_streak: 1, last_activity_date: '2026-10-05' });
      const result = await json('/me'); assert.equal(result.level, level); assert.equal(result.xpIntoLevel, into);
      assert.equal(levelForXp(xp!), level);
    });
  }
  it('me and history use JWT identity despite query and header overrides', async () => {
    scores.set(42, { total_xp: 100, current_streak: 1, longest_streak: 1, last_activity_date: '2026-10-05' });
    assert.deepEqual(await json('/me?userId=42'), empty);
    assert.deepEqual(await json('/xp-history?userId=42'), []);
    assert.equal((await json('/me?userId=41', tokens.sign(42))).totalXp, 100);
    assert.deepEqual(calls, [['score', 41], ['history', 41, 20], ['score', 42]]);
  });
  for (const path of ['/xp-history', '/leaderboard']) {
    it(`${path} uses default 20 and accepts canonical boundaries 1 and 100`, async () => {
      for (const suffix of ['', '?limit=1', '?limit=100']) assert.deepEqual(await json(path + suffix), []);
      assert.deepEqual(calls.map((c) => c.at(-1)), [20, 1, 100]);
    });
    for (const bad of ['0', '-1', '1.5', '1e2', '01', 'abc', '101', '', '+1', ' 1', '999999999999999999999']) {
      it(`${path} rejects limit ${JSON.stringify(bad)}`, async () => {
        const response = await get(`${path}?limit=${encodeURIComponent(bad)}`);
        assert.equal(response.status, 400); assert.equal((await response.json()).error.code, 'VALIDATION_ERROR');
        assert.deepEqual(calls, []);
      });
    }
    it(`${path} rejects repeated limit parameters`, async () => {
      assert.equal((await get(path + '?limit=1&limit=2')).status, 400); assert.deepEqual(calls, []);
    });
  }
  it('history preserves repository order, nullable deleted-card references and explicit public mapping', async () => {
    const created = new Date('2026-10-05T01:00:00Z');
    history.set(41, [{ id: 8, source_type: 'QUIZ', flashcard_id: null, word: null, xp_amount: 10,
      activity_date: '2026-10-05', created_at: created }, { id: 7, source_type: 'REVIEW', flashcard_id: 1,
      word: 'apple', xp_amount: 5, activity_date: '2026-10-05', created_at: created }]);
    assert.deepEqual(await json('/xp-history'), [
      { id: 8, sourceType: 'QUIZ', flashcardId: null, word: null, xpAmount: 10, activityDate: '2026-10-05', createdAt: created.toISOString() },
      { id: 7, sourceType: 'REVIEW', flashcardId: 1, word: 'apple', xpAmount: 5, activityDate: '2026-10-05', createdAt: created.toISOString() },
    ]);
    assert.equal((await json('/xp-history?limit=1')).length, 1);
    assert.deepEqual(await json('/xp-history', tokens.sign(42)), []);
  });
  it('leaderboard preserves SQL ranks/tie order, derives level and excludes private fields', async () => {
    board = [41, 42, 43].map((id, index) => ({ rank: index < 2 ? '1' : '3', user_id: id,
      username: `user${id}`, full_name: null, avatar_url: null, total_xp: index < 2 ? 250 : 99,
      current_streak: 2, longest_streak: 5, email: 'private@example.com', password_hash: 'private' }));
    assert.deepEqual(await json('/leaderboard'), [41, 42, 43].map((id, index) => ({ rank: index < 2 ? 1 : 3,
      userId: id, username: `user${id}`, fullName: null, avatarUrl: null, totalXp: index < 2 ? 250 : 99,
      level: index < 2 ? 3 : 1, currentStreak: 2, longestStreak: 5 })));
  });
  it('malformed database rank is handled as a generic 500', async () => {
    board = [{ rank: 'NaN', user_id: 41, username: 'u', full_name: null, avatar_url: null,
      total_xp: 10, current_streak: 1, longest_streak: 1 }];
    assert.equal((await get('/leaderboard')).status, 500);
  });
  it('all read failures return generic 500 without database error leakage', async () => {
    fail = true;
    for (const path of paths) {
      const response = await get(path); assert.equal(response.status, 500);
      const body = await response.text(); assert.doesNotMatch(body, /SQL|password_hash|secret|stack/);
      assert.equal(JSON.parse(body).error.code, 'INTERNAL_SERVER_ERROR');
    }
  });
  it('no write endpoint permits client-supplied XP', async () => {
    assert.equal((await fetch(base + '/add-xp', { method: 'POST', headers: { Authorization: `Bearer ${tokens.sign(41)}`,
      'Content-Type': 'application/json' }, body: JSON.stringify({ xpAmount: 999, userId: 42 }) })).status, 404);
    assert.deepEqual(calls, []);
  });
});
