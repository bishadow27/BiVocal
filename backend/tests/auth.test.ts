import assert from 'node:assert/strict';
import { afterEach, before, beforeEach, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import bcrypt from 'bcrypt';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { AuthRepository, CreateUserData } from '../src/modules/auth/auth.repository.js';
import type { UserCredentialRow } from '../src/modules/auth/auth.types.js';
import type { Repositories } from '../src/routes.js';

const secret = 'auth-test-only-secret-with-32-plus-characters';
const password = 'StrongPassword123';
const registerData = { username: 'phuc123', email: 'phuc@example.com', password,
  fullName: 'Nguyen Duy Phuc' };
const datasets: Repositories = {
  database: { async ping() {} },
  topics: { async findAll() { return []; }, async findCatalog() { return []; }, async findByCode() { return undefined; } },
  flashcards: { async findByTopicCode() { return []; }, async search() { return { rows: [], total: 0 }; }, async findById() { return undefined; } },
};

describe('Milestone 2 auth HTTP API (real bcrypt/JWT; no PostgreSQL)', () => {
  let server: Server;
  let base: string;
  let users: UserCredentialRow[];
  let createError: unknown;
  let createdInputs: CreateUserData[];
  let lastLogins: number[];
  let existingHash: string;
  const tokens = createTokenService(secret, 3600);
  const passwords = createPasswordService();

  before(async () => { existingHash = await passwords.hash(password); });
  beforeEach(async () => {
    users = [{ id: 42, username: 'Existing', email: 'existing@example.com',
      password_hash: existingHash, full_name: 'Existing User', avatar_url: null,
      created_at: new Date('2026-10-01T00:00:00.000Z') }];
    createdInputs = [];
    lastLogins = [];
    createError = undefined;
    const repository: AuthRepository = {
      async create(input) {
        createdInputs.push(input);
        if (createError) throw createError;
        if (users.some((user) => user.email.toLowerCase() === input.email.toLowerCase())) {
          throw Object.assign(new Error('private PostgreSQL detail'),
            { code: '23505', constraint: 'idx_users_email_unique' });
        }
        if (users.some((user) => user.username.toLowerCase() === input.username.toLowerCase())) {
          throw Object.assign(new Error('private PostgreSQL detail'),
            { code: '23505', constraint: 'idx_users_username_unique' });
        }
        const user = { id: 50 + users.length, username: input.username, email: input.email,
          password_hash: input.passwordHash, full_name: input.fullName, avatar_url: null,
          created_at: new Date('2026-10-04T00:00:00.000Z') };
        users.push(user);
        return user;
      },
      async findByEmail(email) { return users.find((user) => user.email === email); },
      async findById(id) { return users.find((user) => user.id === id); },
      async updateLastLogin(id) { lastLogins.push(id); },
    };
    const service = createAuthService(repository, passwords, tokens);
    server = createApp(datasets, { service, tokens }).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => {
      server.once('listening', resolve); server.once('error', reject);
    });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });
  afterEach(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => error ? reject(error) : resolve());
      server.closeAllConnections();
    });
  });

  async function post(endpoint: string, body: unknown) {
    return fetch(`${base}/api/v1/auth/${endpoint}`, { method: 'POST',
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  }
  async function me(token?: string) {
    return fetch(`${base}/api/v1/auth/me`, token ? { headers: { Authorization: `Bearer ${token}` } } : {});
  }

  it('register returns 201, hashes the password and returns only the public user DTO', async () => {
    const response = await post('register', registerData);
    assert.equal(response.status, 201);
    const body: unknown = await response.json();
    assert.ok(typeof body === 'object' && body !== null && 'user' in body && 'accessToken' in body);
    assert.deepEqual(body.user, { id: 51, username: 'phuc123', email: 'phuc@example.com',
      fullName: 'Nguyen Duy Phuc', avatarUrl: null });
    assert.equal(typeof body.accessToken, 'string');
    assert.ok(!JSON.stringify(body).includes(password));
    assert.ok(!JSON.stringify(body).includes('password_hash'));
    assert.ok(!JSON.stringify(body).includes('passwordHash'));
    const input = createdInputs[0];
    assert.ok(input);
    assert.notEqual(input.passwordHash, password);
    assert.equal(bcrypt.getRounds(input.passwordHash), 12);
    assert.equal(await bcrypt.compare(password, input.passwordHash), true);
  });

  it('trims username/fullName and normalizes email to lowercase', async () => {
    const response = await post('register', { ...registerData, username: ' Phuc123 ',
      email: '  PHUC@EXAMPLE.COM  ', fullName: ' Nguyen Duy Phuc ' });
    assert.equal(response.status, 201);
    assert.equal(createdInputs[0]?.username, 'Phuc123');
    assert.equal(createdInputs[0]?.email, 'phuc@example.com');
    assert.equal(createdInputs[0]?.fullName, 'Nguyen Duy Phuc');
  });

  it('allows omitted fullName', async () => {
    const response = await post('register', { username: 'phuc123', email: 'phuc@example.com', password });
    assert.equal(response.status, 201);
    assert.equal(createdInputs[0]?.fullName, null);
  });

  it('rejects invalid email', async () => {
    assert.equal((await post('register', { ...registerData, email: 'invalid' })).status, 400);
    assert.equal(createdInputs.length, 0);
  });
  it('rejects invalid usernames', async () => {
    for (const username of ['ab', 'a'.repeat(31), 'phuc-name', 'phúc123', 'with space']) {
      assert.equal((await post('register', { ...registerData, username })).status, 400);
    }
    assert.equal(createdInputs.length, 0);
  });
  it('rejects short passwords', async () => {
    assert.equal((await post('register', { ...registerData, password: '1234567' })).status, 400);
    assert.equal((await post('register', { ...registerData, password: '😀'.repeat(4) })).status, 400);
  });
  it('rejects passwords above 72 UTF-8 bytes in both register and login', async () => {
    for (const unsafe of ['a'.repeat(73), '😀'.repeat(19), 'á'.repeat(37)]) {
      assert.equal((await post('register', { ...registerData, password: unsafe })).status, 400);
      assert.equal((await post('login', { email: 'existing@example.com', password: unsafe })).status, 400);
    }
    assert.equal(createdInputs.length, 0);
  });
  it('accepts the exact 72-byte boundary without truncation', async () => {
    const boundary = 'á'.repeat(36);
    assert.equal(Buffer.byteLength(boundary), 72);
    assert.equal((await post('register', { ...registerData, password: boundary })).status, 201);
    assert.equal(await bcrypt.compare(boundary, createdInputs[0]?.passwordHash ?? ''), true);
  });
  it('maps PostgreSQL email unique violation to 409', async () => {
    const response = await post('register', { ...registerData, email: 'EXISTING@example.com' });
    assert.equal(response.status, 409);
    assert.deepEqual(await response.json(), { error: { code: 'EMAIL_ALREADY_EXISTS', message: 'Email already exists' } });
  });
  it('maps PostgreSQL username unique violation to 409', async () => {
    const response = await post('register', { ...registerData, username: 'existing' });
    assert.equal(response.status, 409);
    assert.deepEqual(await response.json(), { error: { code: 'USERNAME_ALREADY_EXISTS', message: 'Username already exists' } });
  });
  it('does not expose raw SQL, DB passwords, JWT secrets or hashes on unexpected failure', async () => {
    createError = Object.assign(new Error(`SELECT password_hash; DB_PASSWORD=private; ${secret}; ${existingHash}`),
      { code: '23505', constraint: 'unexpected_index' });
    const response = await post('register', registerData);
    assert.equal(response.status, 500);
    assert.deepEqual(await response.json(), { error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' } });
  });
  it('logs in with normalized email, returns a token and updates last_login', async () => {
    const response = await post('login', { email: ' EXISTING@EXAMPLE.COM ', password });
    assert.equal(response.status, 200);
    const body = await response.json() as { user: { id: number }; accessToken: string };
    assert.equal(body.user.id, 42);
    assert.equal(tokens.verify(body.accessToken), 42);
    assert.deepEqual(lastLogins, [42]);
    assert.ok(!JSON.stringify(body).includes('password_hash'));
    assert.ok(!JSON.stringify(body).includes(existingHash));
  });
  it('wrong password and missing email return the same 401 code/message', async () => {
    const wrong = await post('login', { email: 'existing@example.com', password: 'IncorrectPassword123' });
    const missing = await post('login', { email: 'missing@example.com', password });
    assert.equal(wrong.status, 401); assert.equal(missing.status, 401);
    const expected = { error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password' } };
    assert.deepEqual(await wrong.json(), expected); assert.deepEqual(await missing.json(), expected);
    assert.deepEqual(lastLogins, []);
  });
  it('me accepts a valid token and returns createdAt without a password hash', async () => {
    const response = await me(tokens.sign(42));
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { id: 42, username: 'Existing', email: 'existing@example.com',
      fullName: 'Existing User', avatarUrl: null, createdAt: '2026-10-01T00:00:00.000Z' });
  });
  it('missing token returns AUTH_TOKEN_MISSING', async () => {
    const response = await me();
    assert.equal(response.status, 401);
    assert.deepEqual(await response.json(), { error: { code: 'AUTH_TOKEN_MISSING', message: 'Authentication token required' } });
  });
  it('malformed token returns AUTH_TOKEN_INVALID', async () => {
    const response = await me('not-a-token');
    assert.equal(response.status, 401);
    assert.deepEqual(await response.json(), { error: { code: 'AUTH_TOKEN_INVALID', message: 'Invalid or expired authentication token' } });
  });
  it('invalid signature returns 401', async () => {
    assert.equal((await me(createTokenService('different-test-secret-at-least-32-chars', 60).sign(42))).status, 401);
  });
  it('expired token returns 401', async () => {
    assert.equal((await me(jwt.sign({}, secret, { subject: '42', expiresIn: -1 }))).status, 401);
  });
  it('disallows unsupported algorithms, missing expiry and invalid subject claims', async () => {
    const invalid = [jwt.sign({}, secret, { algorithm: 'HS384', subject: '42', expiresIn: 60 }),
      jwt.sign({ sub: '42' }, secret), jwt.sign({ sub: 42 }, secret, { expiresIn: 60 }),
      jwt.sign({ sub: '1.5' }, secret, { expiresIn: 60 }), jwt.sign({ sub: '2147483648' }, secret, { expiresIn: 60 })];
    for (const token of invalid) assert.equal((await me(token)).status, 401);
  });
  it('a token for a removed user returns AUTH_USER_NOT_FOUND', async () => {
    const response = await me(tokens.sign(999));
    assert.equal(response.status, 401);
    assert.deepEqual(await response.json(), { error: { code: 'AUTH_USER_NOT_FOUND', message: 'Authenticated user not found' } });
  });
  it('does not accept request body IDs as the current identity', async () => {
    const response = await fetch(`${base}/api/v1/auth/me?userId=999`, {
      headers: { Authorization: `Bearer ${tokens.sign(42)}` } });
    assert.equal(response.status, 200);
    const body = await response.json() as { id: number };
    assert.equal(body.id, 42);
  });
  it('uses an auth-versioned token payload and respects configured expiry', () => {
    const payload = jwt.decode(tokens.sign(42));
    assert.ok(typeof payload === 'object' && payload !== null);
    assert.deepEqual(Object.keys(payload).sort(), ['av', 'exp', 'iat', 'sub']);
    assert.equal(payload.av, 1);
    assert.equal((payload.exp ?? 0) - (payload.iat ?? 0), 3600);
  });
  it('rejects malformed JSON without echoing request passwords', async () => {
    const response = await fetch(`${base}/api/v1/auth/register`, { method: 'POST',
      headers: { 'Content-Type': 'application/json' }, body: '{"password":"SECRET",' });
    assert.equal(response.status, 400);
    assert.deepEqual(await response.json(), { error: { code: 'INVALID_JSON', message: 'Invalid JSON body' } });
  });
  it('limits JSON bodies and rejects unknown registration fields', async () => {
    assert.equal((await post('register', { ...registerData, userId: 42 })).status, 400);
    assert.equal((await post('register', { ...registerData, fullName: 'a'.repeat(20000) })).status, 413);
  });
});
