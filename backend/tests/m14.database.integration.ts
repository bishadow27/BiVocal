import 'dotenv/config';
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { it } from 'node:test';
import { createPool } from '../src/config/database.js';
import { readEnv } from '../src/config/env.js';
import { createAuthRepository } from '../src/modules/auth/auth.repository.js';
import { createAuthService } from '../src/modules/auth/auth.service.js';
import { createPasswordService } from '../src/modules/auth/password.service.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { GoogleProfile } from '../src/modules/auth/auth.types.js';
import { AppError } from '../src/shared/errors/app-error.js';

async function rejectsCode(action: () => Promise<unknown>, code: string) {
  await assert.rejects(action, (error: unknown) => error instanceof AppError && error.code === code);
}

it('M14 PostgreSQL isolated lifecycle preserves existing data and users', async () => {
  if (process.env.M14_INTEGRATION_ALLOW_WRITES !== '1' || process.env.DB_NAME !== 'bivoca_m14_20261023') {
    throw new Error('M14 integration is restricted to the verified isolated database');
  }
  const env = readEnv(process.env);
  const pool = createPool(env);
  const repo = createAuthRepository(pool);
  const passwords = createPasswordService();
  const tokens = createTokenService('m14-isolated-jwt-secret-with-at-least-32-chars', 3600,
    async (id, version) => await repo.findAuthVersion!(id) === version);
  const tag = randomUUID().replaceAll('-', '').slice(0, 20);
  const googleEmail = `m14_google_${tag}@example.com`;
  const passwordEmail = `m14_password_${tag}@example.com`;
  const password = 'M14IntegrationPassword123!';
  const newPassword = 'M14IntegrationNewPassword123!';
  const userIds: number[] = [];
  const challengeIds: string[] = [];
  const emailUrls: string[] = [];
  let activeProfile: GoogleProfile = { subject: `subject-${tag}`, email: googleEmail,
    fullName: 'M14 Google User', pictureUrl: null, nonce: '' };
  const service = createAuthService(repo, passwords, tokens, {
    googleVerifier: { async verify(_idToken, nonce) { return { ...activeProfile, nonce }; } },
    publicResetUrl: 'http://127.0.0.1:3001/reset-password',
    emailSender: { configured: true, async sendPasswordReset(_to, url) { emailUrls.push(url); } },
  });
  const snapshot = async () => (await pool.query(`SELECT
    (SELECT COUNT(*)::int FROM flashcards) flashcards,
    (SELECT COUNT(*)::int FROM topics) topics,
    (SELECT COUNT(*)::int FROM users) users,
    (SELECT COUNT(*)::int FROM user_progress) progress,
    (SELECT COUNT(*)::int FROM quiz_results) quiz,
    (SELECT COUNT(*)::int FROM user_scores) scores,
    (SELECT COUNT(*)::int FROM xp_history) xp`)).rows[0];
  const baseline = await snapshot();
  try {
    const challenge1 = await service.createGoogleChallenge(); challengeIds.push(challenge1.challengeId);
    const first = await service.googleLogin({ idToken: 'opaque-integration-token',
      challengeId: challenge1.challengeId, nonce: challenge1.nonce });
    userIds.push(first.user.id);
    const created = await pool.query<{ password_hash: string | null; role: string }>(
      'SELECT password_hash,role FROM users WHERE id=$1', [first.user.id]);
    assert.equal(created.rows[0]?.password_hash, null);
    assert.equal(created.rows[0]?.role, 'USER');

    const challenge2 = await service.createGoogleChallenge(); challengeIds.push(challenge2.challengeId);
    const repeat = await service.googleLogin({ idToken: 'opaque-integration-token',
      challengeId: challenge2.challengeId, nonce: challenge2.nonce });
    assert.equal(repeat.user.id, first.user.id);
    assert.equal((await pool.query('SELECT COUNT(*)::int count FROM users WHERE email=$1',
      [googleEmail])).rows[0]?.count, 1);

    const registered = await service.register({ username: `m14_${tag}`, email: passwordEmail,
      password, fullName: 'M14 Password User' });
    userIds.push(registered.user.id);
    activeProfile = { ...activeProfile, subject: `link-${tag}`, email: passwordEmail };
    const collisionChallenge = await service.createGoogleChallenge(); challengeIds.push(collisionChallenge.challengeId);
    await rejectsCode(() => service.googleLogin({ idToken: 'collision-token',
      challengeId: collisionChallenge.challengeId, nonce: collisionChallenge.nonce }),
    'GOOGLE_ACCOUNT_LINK_REQUIRED');

    const linkChallenge = await service.createGoogleChallenge(); challengeIds.push(linkChallenge.challengeId);
    const linked = await service.linkGoogle(registered.user.id, { idToken: 'link-token',
      challengeId: linkChallenge.challengeId, nonce: linkChallenge.nonce, password });
    assert.equal(linked.user.id, registered.user.id);
    assert.equal((await pool.query('SELECT COUNT(*)::int count FROM user_auth_identities WHERE user_id=$1',
      [registered.user.id])).rows[0]?.count, 1);

    const oldJwt = (await service.login({ email: passwordEmail, password })).accessToken;
    const forgot = await service.forgotPassword({ email: passwordEmail });
    assert.match(forgot.message, /^Nếu email/);
    const resetToken = new URL(emailUrls.at(-1)!).searchParams.get('token');
    assert.ok(resetToken);
    await service.resetPassword({ token: resetToken, newPassword });
    await rejectsCode(() => tokens.verifySession(oldJwt), 'AUTH_TOKEN_INVALID');
    await rejectsCode(() => service.login({ email: passwordEmail, password }), 'INVALID_CREDENTIALS');
    assert.equal((await service.login({ email: passwordEmail, password: newPassword })).user.id,
      registered.user.id);
    await rejectsCode(() => service.resetPassword({ token: resetToken,
      newPassword: 'M14AnotherPassword123!' }), 'RESET_TOKEN_INVALID');

    assert.equal((await pool.query('SELECT role FROM users WHERE id=$1', [registered.user.id])).rows[0]?.role,
      'USER');
  } finally {
    if (challengeIds.length) await pool.query('DELETE FROM google_auth_challenges WHERE id=ANY($1::uuid[])',
      [challengeIds]);
    if (userIds.length) await pool.query('DELETE FROM users WHERE id=ANY($1::int[])', [userIds]);
    assert.deepEqual(await snapshot(), baseline);
    await pool.end();
  }
});
