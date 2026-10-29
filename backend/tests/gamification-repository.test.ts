import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type { Pool, PoolClient } from 'pg';
import { createGamificationRepository, recordLearningActivity } from '../src/modules/gamification/gamification.repository.js';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import { createProgressRepository } from '../src/modules/progress/progress.repository.js';
import type { ProgressRow } from '../src/modules/progress/progress.types.js';

// Scripted query responses exercise real production orchestration. They do NOT
// execute SQL or prove PostgreSQL locking, streak CASE evaluation or rollback persistence.
function fixture() {
  const calls: { op: string; sql: string; params: unknown[] }[] = []; const releases: boolean[] = [];
  let duplicate = false; let failure: string | undefined; let rollbackFails = false; let missing = false;
  const now = new Date('2026-10-05T01:00:00Z');
  const progress: ProgressRow = { flashcard_id: 1, review_count: 1, correct_count: 1, wrong_count: 0,
    memory_level: 1, is_favorite: true, last_review: now, next_review: null, created_at: now, updated_at: now };
  async function query(sql: string, params: unknown[] = []) {
    const op = /^(BEGIN|COMMIT|ROLLBACK)$/.test(sql) ? sql : sql.includes('INSERT INTO xp_history') ? 'xp'
      : sql.includes('INSERT INTO user_scores') ? 'score' : sql.includes('INSERT INTO quiz_results') ? 'quiz'
      : sql.includes('INSERT INTO user_progress') ? 'progress' : 'cards';
    calls.push({ op, sql, params });
    if (op === failure || (op === 'ROLLBACK' && rollbackFails)) throw new Error(`synthetic ${op} failure`);
    if (op === 'xp') return { rows: duplicate ? [] : [{ xp_amount: params[4] }] };
    if (op === 'progress') return { rows: missing ? [] : [progress] };
    if (op === 'quiz') return { rows: [{ id: 9, flashcard_id: 1, selected_flashcard_id: params[2],
      is_correct: params[3], question_type: 'EN_TO_VI', answered_at: now }] };
    if (op === 'cards') return { rows: [1, 2].map((id) => ({ id, topic_id: 1, is_active: true,
      english: id === 1 ? 'apple' : 'milk', vietnamese: id === 1 ? 'táo' : 'sữa' })) };
    return { rows: [] };
  }
  const client = { query, release(discard: boolean) { releases.push(discard); } } as unknown as PoolClient;
  const pool = { async connect() { return client; }, async query(sql: string, params?: unknown[]) {
    // Favorite alone is allowed to use the pool; transactional paths must use client.query.
    if (!sql.includes('is_favorite, updated_at')) throw new Error('Unexpected pool query');
    return query(sql, params);
  } } as unknown as Pool;
  return { client, pool, calls, releases, progress,
    duplicate() { duplicate = true; }, missing() { missing = true; },
    fail(op: string, rollback = false) { failure = op; rollbackFails = rollback; } };
}
describe('Milestone 5 real award helper and transaction orchestration with client doubles', () => {
  it('first correct quiz requests exactly 10 and adds only INSERT RETURNING XP', async () => {
    const f = fixture(); assert.equal(await recordLearningActivity(f.client,
      { userId: 41, flashcardId: 1, sourceType: 'QUIZ', quizResultId: 9, correct: true }), 10);
    assert.deepEqual(f.calls.map((c) => c.op), ['xp', 'score']);
    assert.deepEqual(f.calls[0]!.params, [41, 1, 9, 'QUIZ', 10]); assert.deepEqual(f.calls[1]!.params, [41, 10]);
  });
  it('duplicate daily award still records activity with zero added XP', async () => {
    const f = fixture(); f.duplicate();
    assert.equal(await recordLearningActivity(f.client,
      { userId: 41, flashcardId: 1, sourceType: 'QUIZ', quizResultId: 10, correct: true }), 0);
    assert.deepEqual(f.calls.at(-1)!.params, [41, 0]);
  });
  it('wrong quiz skips history but records zero-XP activity; subsequent correct can award', async () => {
    const f = fixture();
    assert.equal(await recordLearningActivity(f.client,
      { userId: 41, flashcardId: 1, sourceType: 'QUIZ', quizResultId: 9, correct: false }), 0);
    assert.deepEqual(f.calls.map((c) => c.op), ['score']); assert.deepEqual(f.calls[0]!.params, [41, 0]);
    assert.equal(await recordLearningActivity(f.client,
      { userId: 41, flashcardId: 1, sourceType: 'QUIZ', quizResultId: 10, correct: true }), 10);
  });
  it('QUIZ requires valid result ID before any query, including wrong quiz', async () => {
    for (const id of [0, -1, 1.5, NaN, 2147483648, undefined]) {
      const f = fixture(); await assert.rejects(recordLearningActivity(f.client,
        { userId: 41, flashcardId: 1, sourceType: 'QUIZ', quizResultId: id as number, correct: false }));
      assert.deepEqual(f.calls, []);
    }
  });
  it('REVIEW uses fixed 5 XP and null quiz reference; duplicates add zero', async () => {
    const f = fixture(); const activity = { userId: 41, flashcardId: 1, sourceType: 'REVIEW' } as const;
    assert.equal(await recordLearningActivity(f.client, activity), 5);
    assert.deepEqual(f.calls[0]!.params, [41, 1, null, 'REVIEW', 5]);
    f.duplicate(); assert.equal(await recordLearningActivity(f.client, activity), 0);
    assert.deepEqual(f.calls.at(-1)!.params, [41, 0]);
  });
  it('SQL delegates UTC day, daily conflict arbitration and atomic score arithmetic to PostgreSQL', async () => {
    const f = fixture(); await recordLearningActivity(f.client, { userId: 41, flashcardId: 1, sourceType: 'REVIEW' });
    assert.match(f.calls[0]!.sql, /ON CONFLICT DO NOTHING RETURNING xp_amount/);
    for (const call of f.calls) assert.match(call.sql, /CURRENT_TIMESTAMP AT TIME ZONE 'UTC'/);
    assert.match(f.calls[1]!.sql, /total_xp = s.total_xp \+ EXCLUDED.total_xp/);
    assert.ok(f.calls.every((c) => !/\bSELECT\b|\bBEGIN\b|\bCOMMIT\b/.test(c.sql)));
  });
  it('streak SQL contains first, next-day, gap, same/older-day and longest-preservation branches', async () => {
    const f = fixture(); await recordLearningActivity(f.client, { userId: 41, flashcardId: 1, sourceType: 'REVIEW' });
    const sql = f.calls[1]!.sql;
    assert.match(sql, /VALUES \(\$1, \$2, 1, 1,/);
    assert.match(sql, /WHEN s.last_activity_date IS NULL THEN 1/);
    assert.match(sql, /WHEN EXCLUDED.last_activity_date = s.last_activity_date \+ 1 THEN s.current_streak \+ 1/);
    assert.match(sql, /WHEN EXCLUDED.last_activity_date > s.last_activity_date \+ 1 THEN 1/);
    assert.match(sql, /ELSE s.current_streak END/);
    assert.match(sql, /longest_streak = GREATEST\(s.longest_streak,/);
    assert.match(sql, /last_activity_date = GREATEST\(s.last_activity_date, EXCLUDED.last_activity_date\)/);
  });
  it('correct Quiz runs real reward helper on the same transaction client and preserves returned Progress', async () => {
    const f = fixture(); const result = await createQuizRepository(f.pool, recordLearningActivity, async () => false).answer(41, 1, 1);
    assert.equal(result.status, 'answered');
    assert.deepEqual(f.calls.map((c) => c.op), ['BEGIN', 'cards', 'quiz', 'progress', 'xp', 'score', 'COMMIT']);
    if (result.status === 'answered') assert.equal(result.progress.is_favorite, true);
    assert.deepEqual(f.releases, [false]);
  });
  it('wrong Quiz is server-graded and commits activity without XP history', async () => {
    const f = fixture(); await createQuizRepository(f.pool, recordLearningActivity, async () => false).answer(41, 1, 2);
    assert.deepEqual(f.calls.find((c) => c.op === 'quiz')!.params, [41, 1, 2, false]);
    assert.deepEqual(f.calls.map((c) => c.op), ['BEGIN', 'cards', 'quiz', 'progress', 'score', 'COMMIT']);
  });
  it('repeat correct Quiz still inserts result and updates Progress while award is deduplicated', async () => {
    const f = fixture(); f.duplicate(); await createQuizRepository(f.pool, recordLearningActivity, async () => false).answer(41, 1, 1);
    assert.deepEqual(f.calls.map((c) => c.op), ['BEGIN', 'cards', 'quiz', 'progress', 'xp', 'score', 'COMMIT']);
    assert.deepEqual(f.calls.find((c) => c.op === 'score')!.params, [41, 0]);
  });
  for (const correct of [true, false]) {
    it(`Review correct=${correct} awards 5 and returns unchanged Progress contract`, async () => {
      const f = fixture(); const result = await createProgressRepository(f.pool, recordLearningActivity, async () => false).review(41, 1, correct);
      assert.equal(result, f.progress); assert.equal(result.is_favorite, true);
      assert.deepEqual(f.calls.map((c) => c.op), ['BEGIN', 'progress', 'xp', 'score', 'COMMIT']);
      assert.deepEqual(f.calls.find((c) => c.op === 'progress')!.params,
        [41, 1, correct, [0, 1, 3, 7, 14, 30], 10]);
      assert.deepEqual(f.calls.find((c) => c.op === 'score')!.params, [41, 5]); assert.deepEqual(f.releases, [false]);
    });
  }
  it('repeat Review still updates Progress while score adds zero', async () => {
    const f = fixture(); f.duplicate(); await createProgressRepository(f.pool, recordLearningActivity, async () => false).review(41, 1, false);
    assert.equal(f.calls.filter((c) => c.op === 'progress').length, 1);
    assert.deepEqual(f.calls.find((c) => c.op === 'score')!.params, [41, 0]);
  });
  for (const source of ['Quiz', 'Review']) {
    for (const operation of ['xp', 'score', 'COMMIT']) {
      it(`${source} ${operation} failure rolls back and releases, never partially commits`, async () => {
        const f = fixture(); f.fail(operation);
        await assert.rejects(source === 'Quiz' ? createQuizRepository(f.pool).answer(41, 1, 1)
          : createProgressRepository(f.pool).review(41, 1, true), new RegExp(`synthetic ${operation} failure`));
        assert.equal(f.calls.at(-1)!.op, 'ROLLBACK'); assert.deepEqual(f.releases, [false]);
        if (operation !== 'COMMIT') assert.ok(!f.calls.some((c) => c.op === 'COMMIT'));
      });
    }
    it(`${source} failed rollback discards client and preserves original error`, async () => {
      const f = fixture(); f.fail('score', true);
      await assert.rejects(source === 'Quiz' ? createQuizRepository(f.pool).answer(41, 1, 1)
        : createProgressRepository(f.pool).review(41, 1, true), /synthetic score failure/);
      assert.deepEqual(f.releases, [true]);
    });
  }
  it('missing/inactive Review card rolls back without creating activity', async () => {
    const f = fixture(); f.missing(); assert.equal(await createProgressRepository(f.pool).review(41, 1, true), undefined);
    assert.deepEqual(f.calls.map((c) => c.op), ['BEGIN', 'progress', 'ROLLBACK']); assert.deepEqual(f.releases, [false]);
  });
  it('favorite never calls transaction/reward helper', async () => {
    const f = fixture(); await createProgressRepository(f.pool).favorite(41, 1, true);
    assert.deepEqual(f.calls.map((c) => c.op), ['progress']); assert.deepEqual(f.releases, []);
  });
});

describe('Milestone 5 read query contracts (not SQL execution)', () => {
  it('uses only parameterized SELECTs, UTC-date strings, user history isolation/order and public tied ranks', async () => {
    const calls: { sql: string; params: unknown[] }[] = [];
    const pool = { async query(sql: string, params: unknown[]) { calls.push({ sql, params }); return { rows: [] }; } } as unknown as Pool;
    const repository = createGamificationRepository(pool);
    assert.equal(await repository.findScore(41), undefined);
    assert.deepEqual(await repository.history(42, 7), []); assert.deepEqual(await repository.leaderboard(8), []);
    assert.ok(calls.every((c) => c.sql.trim().startsWith('SELECT')));
    assert.deepEqual(calls.map((c) => c.params), [[41], [42, 7], [8]]);
    assert.match(calls[0]!.sql, /to_char\(last_activity_date, 'YYYY-MM-DD'\)/);
    assert.match(calls[1]!.sql, /LEFT JOIN flashcards/);
    assert.match(calls[1]!.sql, /WHERE h.user_id = \$1 ORDER BY h.created_at DESC, h.id DESC LIMIT \$2/);
    assert.match(calls[2]!.sql, /RANK\(\) OVER \(ORDER BY s.total_xp DESC\)/);
    assert.match(calls[2]!.sql, /WHERE s.total_xp > 0 ORDER BY s.total_xp DESC, s.user_id ASC LIMIT \$1/);
    assert.doesNotMatch(calls[2]!.sql, /email|password|last_login|SELECT \*/);
  });
});
