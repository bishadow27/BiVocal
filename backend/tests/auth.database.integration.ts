import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import bcrypt from 'bcrypt';
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
import { createAuthIntegrationRepository } from './support/auth-integration.repository.js';

const userSchema = z.object({ id: z.number().int().positive(), username: z.string(), email: z.string(),
  fullName: z.string().nullable(), avatarUrl: z.string().nullable() }).strict();
const authSchema = z.object({ user: userSchema, accessToken: z.string() }).strict();

it('Milestone 2 PostgreSQL lifecycle; existing dataset preserved', async () => {
  if (process.env.AUTH_INTEGRATION_ALLOW_WRITES !== '1') {
    throw new Error('Auth integration requires AUTH_INTEGRATION_ALLOW_WRITES=1 after users migration is applied');
  }
  const env = readEnv(process.env);
  const pool = createPool(env);
  const checks = createAuthIntegrationRepository(pool);
  const tag = `it_${randomUUID().replaceAll('-', '').slice(0, 24)}`;
  const email = `${tag}@example.com`;
  const password = 'IntegrationOnlyPassword123!';
  const emailDuplicateUsername = `${tag.slice(0, 26)}_e`;
  const usernameDuplicateEmail = `${tag}_other@example.com`;
  const ownedIdentities = [
    { username: tag, email },
    { username: emailDuplicateUsername, email },
    { username: tag.toUpperCase(), email: usernameDuplicateEmail },
  ];
  let mayCleanup = false;
  let server: Server | undefined;
  let baseline: string | undefined;
  try {
    assert.equal(await checks.usersMigrationExists(), true,
      'DATABASE MIGRATION NOT APPLIED: apply users migration explicitly before auth integration');
    baseline = await checks.datasetSnapshot();
    for (const identity of ownedIdentities) {
      assert.equal(await checks.testUserRecord(identity.username, identity.email), undefined);
    }
    mayCleanup = true;
    const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN);
    const database = createHealthRepository(pool);
    const service = createAuthService(createAuthRepository(pool), createPasswordService(), tokens);
    server = createApp({ topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), database }, { service, tokens }).listen(0, '127.0.0.1');
    const running = server;
    await new Promise<void>((resolve, reject) => {
      running.once('listening', resolve); running.once('error', reject);
    });
    const base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    const post = (endpoint: string, body: unknown) => fetch(`${base}/api/v1/auth/${endpoint}`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
    });

    const register = await post('register', { username: tag, email, password, fullName: 'Integration Test' });
    assert.equal(register.status, 201);
    const registered = authSchema.parse(await register.json());
    let row = await checks.testUserRecord(tag, email);
    assert.ok(row);
    assert.notEqual(row.password_hash, password);
    assert.equal(await bcrypt.compare(password, row.password_hash), true);
    assert.equal(row.last_login, null);

    const duplicateEmail = await post('register', { username: emailDuplicateUsername,
      email: email.toUpperCase(), password });
    assert.equal(duplicateEmail.status, 409);
    assert.deepEqual(await duplicateEmail.json(), {
      error: { code: 'EMAIL_ALREADY_EXISTS', message: 'Email already exists' },
    });
    const duplicateUsername = await post('register', { username: tag.toUpperCase(),
      email: usernameDuplicateEmail, password });
    assert.equal(duplicateUsername.status, 409);
    assert.deepEqual(await duplicateUsername.json(), {
      error: { code: 'USERNAME_ALREADY_EXISTS', message: 'Username already exists' },
    });

    const login = await post('login', { email: email.toUpperCase(), password });
    assert.equal(login.status, 200);
    const loggedIn = authSchema.parse(await login.json());
    assert.equal(loggedIn.user.id, registered.user.id);
    row = await checks.testUserRecord(tag, email);
    assert.ok(row?.last_login instanceof Date);

    const wrong = await post('login', { email, password: 'IncorrectPassword123!' });
    assert.equal(wrong.status, 401);
    assert.deepEqual(await wrong.json(), {
      error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' },
    });
    const me = await fetch(`${base}/api/v1/auth/me`, {
      headers: { Authorization: `Bearer ${loggedIn.accessToken}` },
    });
    assert.equal(me.status, 200);
    const current = userSchema.extend({ createdAt: z.string().datetime() }).strict().parse(await me.json());
    assert.equal(current.id, registered.user.id);

    assert.equal((await fetch(`${base}/api/v1/health`)).status, 200);
    const topics = await fetch(`${base}/api/v1/topics`);
    assert.equal(topics.status, 200);
    assert.equal(z.array(z.unknown()).parse(await topics.json()).length, 10);
    const food = await fetch(`${base}/api/v1/topics/FOOD/flashcards`);
    assert.equal(food.status, 200);
    assert.equal(z.array(z.unknown()).parse(await food.json()).length, 30);
    assert.equal(await checks.datasetSnapshot(), baseline);

    await checks.cleanupOwnedUser(row.id, tag, email);
    assert.equal(await checks.testUserRecord(tag, email), undefined);
  } finally {
    // Cleanup only this test's UUID identity, including when assertions fail.
    // Does not run migrations and does not touch real users or topics/flashcards.
    try {
      if (baseline !== undefined) {
        if (mayCleanup) {
          // Also clean unexpected duplicate-test inserts if a unique index is broken.
          for (const identity of ownedIdentities) {
            const owned = await checks.testUserRecord(identity.username, identity.email);
            if (owned) await checks.cleanupOwnedUser(owned.id, identity.username, identity.email);
          }
        }
        assert.equal(await checks.datasetSnapshot(), baseline);
      }
    } finally {
      if (server) {
        const running = server;
        await new Promise<void>((resolve, reject) => {
          running.close((error) => error ? reject(error) : resolve());
          running.closeAllConnections();
        });
      }
      await pool.end();
    }
  }
});
