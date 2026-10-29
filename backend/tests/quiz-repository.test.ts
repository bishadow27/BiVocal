import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type { Pool, PoolClient } from 'pg';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';
import type { ProgressRow } from '../src/modules/progress/progress.types.js';

// A scripted PoolClient double validates orchestration/error paths only.
// It cannot validate PostgreSQL SQL semantics, locks or transaction persistence.
function fixture() {
  const calls: { operation: string; parameters?: unknown[] }[] = [];
  const releases: (boolean | undefined)[] = [];
  const question = { id: 1, topic_id: 1, english: 'apple', vietnamese: 'quả táo', is_active: true };
  const selected = { id: 2, topic_id: 1, english: 'milk', vietnamese: 'sữa', is_active: true };
  const progress: ProgressRow = { flashcard_id: 1, memory_level: 1, review_count: 1,
    correct_count: 1, wrong_count: 0, is_favorite: true, last_review: new Date(), next_review: null,
    created_at: new Date(), updated_at: new Date() };
  let failAt: string | undefined; let failRollback = false; let resultMissing = false; let progressMissing = false;
  let questionMissing = false; let selectedMissing = false; let connectFailure = false;
  const client = {
    async query(sql: string, parameters?: unknown[]) {
      const operation = sql.trim() === 'BEGIN' || sql.trim() === 'COMMIT' || sql.trim() === 'ROLLBACK'
        ? sql.trim() : sql.includes('INSERT INTO quiz_results') ? 'result'
        : sql.includes('INSERT INTO user_progress') ? 'progress' : 'cards';
      calls.push({ operation, ...(parameters === undefined ? {} : { parameters }) });
      if (operation === failAt || (operation === 'ROLLBACK' && failRollback)) throw new Error('synthetic private database error');
      if (operation === 'cards') return { rows: [questionMissing ? undefined : question,
        selectedMissing ? undefined : selected].filter((row) => row !== undefined) };
      if (operation === 'result') return { rows: resultMissing ? [] : [{ id: 9, flashcard_id: 1,
        selected_flashcard_id: parameters?.[2], is_correct: parameters?.[3], question_type: 'EN_TO_VI', answered_at: new Date() }] };
      if (operation === 'progress') return { rows: progressMissing ? [] : [progress] };
      return { rows: [] };
    },
    release(discard?: boolean) { releases.push(discard); },
  } as unknown as PoolClient;
  const pool = {
    async connect() { if (connectFailure) throw new Error('synthetic connection failure'); return client; },
    async query() { throw new Error('Transaction must never use pool.query'); },
  } as unknown as Pool;
  // Keep M4 orchestration assertions isolated from the new reward dependency.
  // M5 tests exercise the real recorder and its transaction/rollback paths.
  return { repository: createQuizRepository(pool, async () => 0, async () => false), calls, releases, question, selected, progress,
    fail(operation: string, rollbackAlsoFails = false) { failAt = operation; failRollback = rollbackAlsoFails; },
    missingResult() { resultMissing = true; }, missingProgress() { progressMissing = true; },
    missingQuestion() { questionMissing = true; }, missingSelected() { selectedMissing = true; },
    failConnect() { connectFailure = true; } };
}

describe('Milestone 4 Quiz transaction orchestration (PoolClient doubles)', () => {
  it('valid answer validates/inserts/reviews/commits on one client and releases it', async () => {
    const f = fixture(); const result = await f.repository.answer(41, 1, 1);
    assert.equal(result.status, 'answered');
    assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'result', 'progress', 'COMMIT']);
    assert.deepEqual(f.calls.find((call) => call.operation === 'result')?.parameters, [41, 1, 1, true]);
    assert.deepEqual(f.calls.find((call) => call.operation === 'progress')?.parameters,
      [41, 1, true, [0, 1, 3, 7, 14, 30], 10]);
    assert.deepEqual(f.releases, [false]);
  });
  it('derives wrong grading from IDs and passes it to the shared progress helper', async () => {
    const f = fixture(); const submitted = await f.repository.answer(42, 1, 2);
    assert.equal(submitted.status, 'answered');
    if (submitted.status === 'answered') {
      assert.equal(submitted.result.is_correct, false); assert.equal(submitted.selectedAnswer, 'sữa');
    }
    assert.deepEqual(f.calls.find((call) => call.operation === 'result')?.parameters, [42, 1, 2, false]);
    assert.deepEqual(f.calls.find((call) => call.operation === 'progress')?.parameters,
      [42, 1, false, [0, 1, 3, 7, 14, 30], 10]);
  });
  it('missing/inactive question rolls back before any mutation', async () => {
    for (const mode of ['missing', 'inactive'] as const) {
      const f = fixture(); if (mode === 'missing') f.missingQuestion(); else f.question.is_active = false;
      assert.deepEqual(await f.repository.answer(41, 1, 2), { status: 'question_missing' });
      assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'ROLLBACK']);
      assert.deepEqual(f.releases, [false]);
    }
  });
  it('missing/inactive/different-topic option rolls back before mutations', async () => {
    for (const mode of ['missing', 'inactive', 'different'] as const) {
      const f = fixture(); if (mode === 'missing') f.missingSelected();
      else if (mode === 'inactive') f.selected.is_active = false; else f.selected.topic_id = 2;
      assert.deepEqual(await f.repository.answer(41, 1, 2), { status: 'option_invalid' });
      assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'ROLLBACK']);
    }
  });
  it('BEGIN failure attempts rollback and releases the client', async () => {
    const f = fixture(); f.fail('BEGIN'); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'ROLLBACK']); assert.deepEqual(f.releases, [false]);
  });
  it('card lookup failure rolls back with no result insert', async () => {
    const f = fixture(); f.fail('cards'); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'ROLLBACK']);
  });
  it('result insert failure rolls back before progress mutation', async () => {
    const f = fixture(); f.fail('result'); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'result', 'ROLLBACK']);
  });
  it('progress mutation failure rolls back the earlier result insert', async () => {
    const f = fixture(); f.fail('progress'); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.deepEqual(f.calls.map((call) => call.operation), ['BEGIN', 'cards', 'result', 'progress', 'ROLLBACK']);
    assert.deepEqual(f.releases, [false]);
  });
  it('missing result/progress RETURNING rows roll back rather than commit', async () => {
    for (const mode of ['result', 'progress'] as const) {
      const f = fixture(); if (mode === 'result') f.missingResult(); else f.missingProgress();
      await assert.rejects(f.repository.answer(41, 1, 1));
      assert.ok(!f.calls.some((call) => call.operation === 'COMMIT'));
      assert.equal(f.calls.at(-1)?.operation, 'ROLLBACK'); assert.deepEqual(f.releases, [false]);
    }
  });
  it('commit failure attempts rollback and preserves the error path', async () => {
    const f = fixture(); f.fail('COMMIT'); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.deepEqual(f.calls.slice(-2).map((call) => call.operation), ['COMMIT', 'ROLLBACK']);
  });
  it('rollback failure discards the client and does not mask the original failure', async () => {
    const f = fixture(); f.fail('progress', true);
    await assert.rejects(f.repository.answer(41, 1, 1), /synthetic private database error/);
    assert.deepEqual(f.releases, [true]);
  });
  it('connection acquisition failure does not release a nonexistent client', async () => {
    const f = fixture(); f.failConnect(); await assert.rejects(f.repository.answer(41, 1, 1));
    assert.equal(f.calls.length, 0); assert.equal(f.releases.length, 0);
  });
});
