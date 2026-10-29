import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { readEnv } from '../src/config/env.js';

const input: NodeJS.ProcessEnv = {
  DB_HOST: 'localhost', DB_PORT: '5432', DB_NAME: 'flashcard_english',
  DB_USER: 'postgres', DB_PASSWORD: 'unit-test-only',
  JWT_SECRET: 'unit-test-only-secret-at-least-32-characters',
};

describe('Environment configuration', () => {
  it('reads all DB fields and applies HTTP defaults', () => {
    const env = readEnv(input);
    assert.equal(env.DB_NAME, 'flashcard_english');
    assert.equal(env.DB_PORT, 5432);
    assert.equal(env.PORT, 3000);
    assert.equal(env.NODE_ENV, 'development');
  });

  it('requires credentials rather than guessing a password', () => {
    assert.throws(() => readEnv({ ...input, DB_PASSWORD: '' }), /DB_PASSWORD/);
    assert.throws(() => readEnv({}), /DB_HOST/);
  });

  it('rejects invalid ports and modes', () => {
    for (const port of ['0', '65536', '1.5', '', 'abc']) {
      assert.throws(() => readEnv({ ...input, DB_PORT: port }), /DB_PORT/);
    }
    assert.throws(() => readEnv({ ...input, NODE_ENV: 'invalid' }), /NODE_ENV/);
  });

  it('configuration errors never include the supplied password', () => {
    const password = 'SECRET_DO_NOT_LOG';
    try {
      readEnv({ ...input, DB_PASSWORD: password, DB_PORT: 'invalid' });
      assert.fail('Expected validation error');
    } catch (error: unknown) {
      assert.ok(error instanceof Error);
      assert.ok(!error.message.includes(password));
    }
  });
});
