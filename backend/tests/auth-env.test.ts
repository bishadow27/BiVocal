import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { readEnv, readDatabaseEnv } from '../src/config/env.js';

const db = { DB_HOST: 'localhost', DB_PORT: '5432', DB_NAME: 'test_only',
  DB_USER: 'test_only', DB_PASSWORD: 'test-only-password' };
const secret = 'test-only-jwt-secret-at-least-32-characters';

describe('JWT configuration', () => {
  it('requires JWT_SECRET and rejects short/blank secrets without exposing values', () => {
    for (const value of [undefined, '', 'short', ' '.repeat(40)]) {
      const input = value === undefined ? db : { ...db, JWT_SECRET: value };
      assert.throws(() => readEnv(input), /JWT_SECRET/);
    }
    try {
      readEnv({ ...db, JWT_SECRET: secret, JWT_EXPIRES_IN: 'invalid' });
      assert.fail('Expected error');
    } catch (error: unknown) {
      assert.ok(error instanceof Error);
      assert.ok(!error.message.includes(secret));
      assert.ok(!error.message.includes(db.DB_PASSWORD));
      assert.match(error.message, /JWT_EXPIRES_IN/);
    }
  });
  it('parses explicit expiry units and defaults to one hour', () => {
    assert.equal(readEnv({ ...db, JWT_SECRET: secret }).JWT_EXPIRES_IN, 3600);
    for (const [duration, seconds] of [['30s', 30], ['15m', 900], ['2h', 7200], ['1d', 86400]] as const) {
      assert.equal(readEnv({ ...db, JWT_SECRET: secret, JWT_EXPIRES_IN: duration }).JWT_EXPIRES_IN, seconds);
    }
  });
  it('rejects ambiguous or unbounded expiry values', () => {
    for (const value of ['3600', '0s', '-1h', '1.5h', '999999999999999d']) {
      assert.throws(() => readEnv({ ...db, JWT_SECRET: secret, JWT_EXPIRES_IN: value }), /JWT_EXPIRES_IN/);
    }
  });
  it('migration DB configuration does not require JWT', () => {
    assert.equal(readDatabaseEnv(db).DB_NAME, 'test_only');
  });
});
