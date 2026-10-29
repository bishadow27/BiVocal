import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import jwt from 'jsonwebtoken';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import type { AuthRepository } from '../src/modules/auth/auth.repository.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { GoogleProfile, UserCredentialRow } from '../src/modules/auth/auth.types.js';
import { SlidingWindowRateLimiter } from '../src/modules/auth/rate-limiter.js';
import { AppError } from '../src/shared/errors/app-error.js';

const secret = 'm14-test-secret-that-is-longer-than-thirty-two';
const profile: GoogleProfile = { subject: 'google-subject-1', email: 'new@example.com',
  fullName: 'Google User', pictureUrl: 'https://example.com/a.png', nonce: 'server-nonce' };

function baseUser(overrides: Partial<UserCredentialRow> = {}): UserCredentialRow {
  return { id: 7, username: 'existing', email: 'existing@example.com', password_hash: null,
    full_name: 'Existing', avatar_url: null, created_at: new Date('2026-10-23T00:00:00Z'),
    auth_version: 1, ...overrides };
}

function repository(overrides: Partial<AuthRepository> = {}): AuthRepository {
  return {
    async create() { return baseUser(); },
    async findByEmail() { return undefined; },
    async findById() { return undefined; },
    async updateLastLogin() {},
    ...overrides,
  };
}

async function rejectsCode(action: () => Promise<unknown>, code: string) {
  await assert.rejects(action, (error: unknown) => error instanceof AppError && error.code === code);
}

describe('M14 Google Sign-In and password recovery security', () => {
  it('creates a server nonce, stores only its SHA-256 digest, and consumes it once', async () => {
    let stored: { id: string; hash: string; expires: Date } | undefined;
    let consumed = false;
    const repo = repository({
      async createGoogleChallenge(id, hash, expires) { stored = { id, hash, expires }; },
      async consumeGoogleChallenge(id, hash) {
        if (consumed || id !== stored?.id || hash !== stored.hash) return false;
        consumed = true; return true;
      },
      async loginWithGoogle() { return baseUser({ id: 8, email: profile.email }); },
    });
    const verifier = { async verify(_token: string, nonce: string) {
      assert.equal(nonce, 'server-nonce'); return profile;
    } };
    const service = createAuthService(repo, createPasswordService(), createTokenService(secret, 3600),
      { googleVerifier: verifier });
    const challenge = await service.createGoogleChallenge();
    assert.notEqual(stored?.hash, challenge.nonce);
    assert.match(stored?.hash ?? '', /^[a-f0-9]{64}$/);
    assert.ok((stored?.expires.getTime() ?? 0) > Date.now());

    // Use the fixed verifier nonce while preserving the challenge ID/digest pair under test.
    profile.nonce = challenge.nonce;
    const matchingVerifier = { async verify() { return { ...profile, nonce: challenge.nonce }; } };
    const matchingService = createAuthService(repo, createPasswordService(), createTokenService(secret, 3600),
      { googleVerifier: matchingVerifier });
    const first = await matchingService.googleLogin({ idToken: 'opaque-id-token',
      challengeId: challenge.challengeId, nonce: challenge.nonce });
    assert.equal(first.user.id, 8);
    await rejectsCode(() => matchingService.googleLogin({ idToken: 'opaque-id-token',
      challengeId: challenge.challengeId, nonce: challenge.nonce }), 'GOOGLE_CHALLENGE_INVALID');
  });

  it('does not merge an existing password account by matching Google email', async () => {
    const repo = repository({
      async consumeGoogleChallenge() { return true; },
      async loginWithGoogle() { return 'EMAIL_LINK_REQUIRED'; },
    });
    const service = createAuthService(repo, createPasswordService(), createTokenService(secret, 3600),
      { googleVerifier: { async verify() { return profile; } } });
    await rejectsCode(() => service.googleLogin({ idToken: 'opaque-id-token',
      challengeId: '3cf18b9d-7f35-4738-8be5-9077158ba856', nonce: 'server-nonce' }),
    'GOOGLE_ACCOUNT_LINK_REQUIRED');
  });

  it('requires password reauthentication and the same verified email when linking', async () => {
    const passwords = createPasswordService();
    const passwordHash = await passwords.hash('StrongPassword123');
    let linked = false;
    const user = baseUser({ password_hash: passwordHash });
    const repo = repository({
      async findById() { return user; }, async findByEmail() { return user; },
      async consumeGoogleChallenge() { return true; },
      async linkGoogle(_id, googleProfile) {
        if (googleProfile.email !== user.email) throw new AppError(409, 'GOOGLE_EMAIL_MISMATCH', 'mismatch');
        linked = true; return user;
      },
    });
    const service = createAuthService(repo, passwords, createTokenService(secret, 3600),
      { googleVerifier: { async verify() { return { ...profile, email: user.email }; } } });
    await rejectsCode(() => service.linkGoogle(user.id, { idToken: 'token', challengeId:
      '3cf18b9d-7f35-4738-8be5-9077158ba856', nonce: 'server-nonce', password: 'WrongPassword123' }),
    'LINK_REAUTH_FAILED');
    await service.linkGoogle(user.id, { idToken: 'token', challengeId:
      '3cf18b9d-7f35-4738-8be5-9077158ba856', nonce: 'server-nonce', password: 'StrongPassword123' });
    assert.equal(linked, true);
  });

  it('returns the same forgot response for missing and existing email and never exposes raw token storage', async () => {
    const storedHashes: string[] = [];
    const sentUrls: string[] = [];
    const user = baseUser({ password_hash: 'existing-hash' });
    const repo = repository({
      async findByEmail(email) { return email === user.email ? user : undefined; },
      async createPasswordReset(_id, hash) { storedHashes.push(hash); },
    });
    const service = createAuthService(repo, createPasswordService(), createTokenService(secret, 3600), {
      publicResetUrl: 'http://127.0.0.1:3001/reset-password',
      emailSender: { configured: true, async sendPasswordReset(_to, url) { sentUrls.push(url); } },
    });
    const known = await service.forgotPassword({ email: user.email });
    const missing = await service.forgotPassword({ email: 'missing@example.com' });
    assert.deepEqual(known, missing);
    assert.match(known.message, /Tiếp tục với Google/);
    assert.equal(storedHashes.length, 1); assert.equal(sentUrls.length, 1);
    const rawToken = new URL(sentUrls[0]!).searchParams.get('token');
    assert.ok(rawToken); assert.notEqual(storedHashes[0], rawToken);
    assert.match(storedHashes[0]!, /^[a-f0-9]{64}$/);
  });

  it('reports SMTP_CONFIGURATION_REQUIRED rather than a false success', async () => {
    const service = createAuthService(repository(), createPasswordService(), createTokenService(secret, 3600));
    await rejectsCode(() => service.forgotPassword({ email: 'missing@example.com' }),
      'SMTP_CONFIGURATION_REQUIRED');
  });

  it('accepts one reset token once and rejects reuse', async () => {
    let available = true; let storedHash = '';
    const repo = repository({ async resetPassword(hash, newHash) {
      storedHash = newHash; if (!available || hash.length !== 64) return false;
      available = false; return true;
    } });
    const service = createAuthService(repo, createPasswordService(), createTokenService(secret, 3600));
    await service.resetPassword({ token: 'a'.repeat(43), newPassword: 'NewStrongPassword123' });
    assert.equal(await createPasswordService().compare('NewStrongPassword123', storedHash), true);
    await rejectsCode(() => service.resetPassword({ token: 'a'.repeat(43),
      newPassword: 'AnotherStrongPassword123' }), 'RESET_TOKEN_INVALID');
  });

  it('invalidates an older JWT when auth_version changes', async () => {
    let version = 1;
    const tokens = createTokenService(secret, 3600, async (_id, tokenVersion) => tokenVersion === version);
    const oldToken = tokens.sign(7, version);
    assert.equal(await tokens.verifySession(oldToken), 7);
    version = 2;
    await rejectsCode(() => tokens.verifySession(oldToken), 'AUTH_TOKEN_INVALID');
    assert.equal(await tokens.verifySession(tokens.sign(7, version)), 7);
    const claims = jwt.decode(tokens.sign(7, version));
    assert.ok(typeof claims === 'object' && claims?.av === 2);
  });

  it('enforces sliding-window rate limits', () => {
    const limiter = new SlidingWindowRateLimiter(2, 60_000);
    limiter.consume('same-key'); limiter.consume('same-key');
    assert.throws(() => limiter.consume('same-key'), (error: unknown) =>
      error instanceof AppError && error.code === 'RATE_LIMITED');
    limiter.consume('different-key');
  });
});
