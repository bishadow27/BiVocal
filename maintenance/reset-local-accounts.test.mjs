import test from 'node:test';
import assert from 'node:assert/strict';
import { NEW_ADMIN, preview, resetAllAccounts, validatePassword } from './reset-local-accounts.mjs';

// In-memory failure injection only. No pg connection, env file or actual account.
function fakeClient({ insertFails = false, contentChanges = false, nextId = 53 } = {}) {
  const initial = {
    users: 3,
    max_user_id: 52,
    progress: 5,
    quiz: 7,
    scores: 3,
    xp_history: 9,
    topics: 2,
    flashcards: 20,
  };
  let state = { ...initial },
    snapshot,
    changed = false;
  const calls = [];
  return {
    calls,
    initial,
    get state() {
      return state;
    },
    async query(sql, parameters) {
      calls.push({ sql, parameters });
      if (sql.startsWith('BEGIN')) snapshot = { ...state };
      if (sql === 'ROLLBACK') state = { ...snapshot };
      if (sql === 'SELECT current_database() AS database')
        return { rows: [{ database: 'isolated_fixture_database' }] };
      if (sql.includes('AS max_user_id'))
        return { rows: [{ ...state, ...(changed && contentChanges ? { flashcards: 19 } : {}) }] };
      if (sql === 'DELETE FROM users') {
        state = {
          ...state,
          users: 0,
          max_user_id: 0,
          progress: 0,
          quiz: 0,
          scores: 0,
          xp_history: 0,
        };
        changed = true;
      }
      if (sql.startsWith('INSERT INTO users')) {
        if (insertFails) throw new Error('Injected insertion failure');
        state = { ...state, users: 1, max_user_id: nextId };
        return {
          rows: [
            { id: nextId, username: NEW_ADMIN.username, email: NEW_ADMIN.email, role: 'ADMIN' },
          ],
        };
      }
      return { rows: [] };
    },
  };
}
const hashPassword = async () => 'isolated-test-hash';

test('invalid or oversized Unicode password prevents every database query', async () => {
  const client = fakeClient();
  await assert.rejects(resetAllAccounts(client, 'short', hashPassword));
  await assert.rejects(resetAllAccounts(client, '界'.repeat(25), hashPassword));
  assert.equal(client.calls.length, 0);
  assert.doesNotThrow(() => validatePassword('synthetic-password'));
});

test('preview uses a readonly transaction and cannot delete/create accounts', async () => {
  const client = fakeClient();
  const result = await preview(client);
  assert.equal(result.current.users, 3);
  assert.equal(client.calls[0].sql, 'BEGIN READ ONLY');
  assert.equal(client.calls.at(-1).sql, 'ROLLBACK');
  assert.equal(
    client.calls.some((call) => /DELETE|INSERT|UPDATE|TRUNCATE|ALTER/i.test(call.sql)),
    false,
  );
  assert.deepEqual(client.state, client.initial);
});

test('success keeps content, creates exactly one ADMIN and does not reset identities', async () => {
  const client = fakeClient();
  const result = await resetAllAccounts(client, 'synthetic-password', hashPassword);
  assert.equal(result.after.users, 1);
  assert.equal(result.admin.role, 'ADMIN');
  assert.equal(result.admin.email, NEW_ADMIN.email);
  assert.equal(result.after.flashcards, result.before.flashcards);
  assert.equal(result.after.topics, result.before.topics);
  assert.equal(client.calls.at(-1).sql, 'COMMIT');
  assert.equal(
    client.calls.some((call) => /TRUNCATE|RESTART|setval|ALTER SEQUENCE/i.test(call.sql)),
    false,
  );
  const insertion = client.calls.find((call) => call.sql.startsWith('INSERT INTO users'));
  assert.equal(insertion.parameters[2], 'isolated-test-hash');
  assert.equal(insertion.parameters.includes('synthetic-password'), false);
});

test('insertion failure rolls deletion back and never commits', async () => {
  const client = fakeClient({ insertFails: true });
  await assert.rejects(resetAllAccounts(client, 'synthetic-password', hashPassword));
  assert.equal(client.calls.at(-1).sql, 'ROLLBACK');
  assert.equal(
    client.calls.some((call) => call.sql === 'COMMIT'),
    false,
  );
  assert.deepEqual(client.state, client.initial);
});

test('unexpected content count aborts the reset instead of committing', async () => {
  const client = fakeClient({ contentChanges: true });
  await assert.rejects(resetAllAccounts(client, 'synthetic-password', hashPassword));
  assert.equal(client.calls.at(-1).sql, 'ROLLBACK');
  assert.deepEqual(client.state, client.initial);
});

test('reused user ID cannot give an old JWT access to the new ADMIN', async () => {
  const client = fakeClient({ nextId: 1 });
  await assert.rejects(resetAllAccounts(client, 'synthetic-password', hashPassword));
  assert.equal(
    client.calls.some((call) => call.sql === 'COMMIT'),
    false,
  );
  assert.deepEqual(client.state, client.initial);
});
