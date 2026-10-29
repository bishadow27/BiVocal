import type { Pool } from 'pg';
import { AppError } from '../../shared/errors/app-error.js';
import type { GoogleProfile, UserRow, UserCredentialRow } from './auth.types.js';

export interface CreateUserData {
  username: string;
  email: string;
  passwordHash: string;
  fullName: string | null;
}

export interface AuthRepository {
  create(input: CreateUserData): Promise<UserRow>;
  findByEmail(email: string): Promise<UserCredentialRow | undefined>;
  findById(id: number): Promise<UserRow | undefined>;
  updateLastLogin(id: number): Promise<void>;
  findAuthVersion?(id: number): Promise<number | undefined>;
  createGoogleChallenge?(id: string, nonceHash: string, expiresAt: Date): Promise<void>;
  consumeGoogleChallenge?(id: string, nonceHash: string): Promise<boolean>;
  loginWithGoogle?(profile: GoogleProfile, usernameBase: string): Promise<UserRow | 'EMAIL_LINK_REQUIRED'>;
  linkGoogle?(userId: number, profile: GoogleProfile): Promise<UserRow>;
  createPasswordReset?(userId: number, tokenHash: string, expiresAt: Date): Promise<void>;
  resetPassword?(tokenHash: string, passwordHash: string): Promise<boolean>;
}

export function createAuthRepository(pool: Pool): AuthRepository {
  return {
    async create(input) {
      const result = await pool.query<UserRow>(
        `INSERT INTO users (username, email, password_hash, full_name)
         VALUES ($1, $2, $3, $4)
         RETURNING id, username, email, full_name, avatar_url, created_at, auth_version`,
        [input.username, input.email, input.passwordHash, input.fullName],
      );
      const user = result.rows[0];
      if (!user) throw new Error('User insert returned no row');
      return user;
    },
    async findByEmail(email) {
      const result = await pool.query<UserCredentialRow>(
        `SELECT id, username, email, password_hash, full_name, avatar_url, created_at, auth_version
         FROM users WHERE LOWER(email) = LOWER($1)`, [email],
      );
      return result.rows[0];
    },
    async findById(id) {
      const result = await pool.query<UserRow>(
        `SELECT id, username, email, full_name, avatar_url, created_at, auth_version
         FROM users WHERE id = $1`, [id],
      );
      return result.rows[0];
    },
    async updateLastLogin(id) {
      await pool.query('UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE id = $1', [id]);
    },
    async findAuthVersion(id) {
      const result = await pool.query<{ auth_version: number }>(
        'SELECT auth_version FROM users WHERE id = $1', [id]);
      return result.rows[0]?.auth_version;
    },
    async createGoogleChallenge(id, nonceHash, expiresAt) {
      await pool.query(
        `INSERT INTO google_auth_challenges (id, nonce_hash, expires_at) VALUES ($1, $2, $3)`,
        [id, nonceHash, expiresAt]);
    },
    async consumeGoogleChallenge(id, nonceHash) {
      const result = await pool.query(
        `UPDATE google_auth_challenges SET used_at = CURRENT_TIMESTAMP
         WHERE id = $1 AND nonce_hash = $2 AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP`,
        [id, nonceHash]);
      return result.rowCount === 1;
    },
    async loginWithGoogle(profile, usernameBase) {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', [`GOOGLE:${profile.subject}`]);
        const identity = await client.query<UserRow>(
          `SELECT u.id,u.username,u.email,u.full_name,u.avatar_url,u.created_at,u.auth_version
           FROM user_auth_identities i JOIN users u ON u.id=i.user_id
           WHERE i.provider='GOOGLE' AND i.provider_subject=$1 FOR UPDATE OF i,u`, [profile.subject]);
        let user = identity.rows[0];
        if (user) {
          await client.query(`UPDATE user_auth_identities SET last_login_at=CURRENT_TIMESTAMP
            WHERE provider='GOOGLE' AND provider_subject=$1`, [profile.subject]);
          await client.query('UPDATE users SET last_login=CURRENT_TIMESTAMP WHERE id=$1', [user.id]);
          await client.query('COMMIT');
          return user;
        }
        const collision = await client.query('SELECT id FROM users WHERE LOWER(email)=LOWER($1)', [profile.email]);
        if (collision.rowCount) {
          await client.query('ROLLBACK');
          return 'EMAIL_LINK_REQUIRED';
        }
        let username = usernameBase;
        for (let suffix = 0; suffix < 1000; suffix++) {
          const candidate = suffix === 0 ? usernameBase : `${usernameBase.slice(0, Math.max(3, 30-String(suffix).length-1))}_${suffix}`;
          const taken = await client.query('SELECT 1 FROM users WHERE LOWER(username)=LOWER($1)', [candidate]);
          if (!taken.rowCount) { username = candidate; break; }
          if (suffix === 999) throw new Error('Unable to allocate Google username');
        }
        const created = await client.query<UserRow>(
          `INSERT INTO users(username,email,password_hash,full_name,avatar_url)
           VALUES($1,$2,NULL,$3,$4)
           RETURNING id,username,email,full_name,avatar_url,created_at,auth_version`,
          [username, profile.email, profile.fullName, profile.pictureUrl]);
        user = created.rows[0];
        if (!user) throw new Error('Google user insert returned no row');
        await client.query(
          `INSERT INTO user_auth_identities(user_id,provider,provider_subject,email_at_link)
           VALUES($1,'GOOGLE',$2,$3)`, [user.id, profile.subject, profile.email]);
        await client.query('UPDATE users SET last_login=CURRENT_TIMESTAMP WHERE id=$1', [user.id]);
        await client.query('COMMIT');
        return user;
      } catch (error) {
        await client.query('ROLLBACK').catch(() => undefined);
        throw error;
      } finally { client.release(); }
    },
    async linkGoogle(userId, profile) {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        await client.query('SELECT pg_advisory_xact_lock(hashtext($1))', [`GOOGLE:${profile.subject}`]);
        const owner = await client.query<{ user_id: number }>(
          `SELECT user_id FROM user_auth_identities WHERE provider='GOOGLE' AND provider_subject=$1 FOR UPDATE`,
          [profile.subject]);
        if (owner.rows[0] && owner.rows[0].user_id !== userId) {
          throw new AppError(409, 'GOOGLE_IDENTITY_ALREADY_LINKED', 'Google account is already linked');
        }
        const userResult = await client.query<UserCredentialRow>(
          `SELECT id,username,email,password_hash,full_name,avatar_url,created_at,auth_version
           FROM users WHERE id=$1 FOR UPDATE`, [userId]);
        const user = userResult.rows[0];
        if (!user) throw new AppError(401, 'AUTH_USER_NOT_FOUND', 'Authenticated user not found');
        if (user.email.toLowerCase() !== profile.email.toLowerCase()) {
          throw new AppError(409, 'GOOGLE_EMAIL_MISMATCH', 'Google email must match the BiVoca account');
        }
        if (!owner.rows[0]) await client.query(
          `INSERT INTO user_auth_identities(user_id,provider,provider_subject,email_at_link)
           VALUES($1,'GOOGLE',$2,$3)`, [userId, profile.subject, profile.email]);
        await client.query('COMMIT');
        return user;
      } catch (error) { await client.query('ROLLBACK'); throw error; }
      finally { client.release(); }
    },
    async createPasswordReset(userId, tokenHash, expiresAt) {
      await pool.query(
        `INSERT INTO password_reset_tokens(user_id,token_hash,expires_at) VALUES($1,$2,$3)`,
        [userId, tokenHash, expiresAt]);
    },
    async resetPassword(tokenHash, passwordHash) {
      const client = await pool.connect();
      try {
        await client.query('BEGIN');
        const token = await client.query<{ id: string; user_id: number }>(
          `SELECT id,user_id FROM password_reset_tokens
           WHERE token_hash=$1 AND used_at IS NULL AND expires_at>CURRENT_TIMESTAMP FOR UPDATE`, [tokenHash]);
        const row = token.rows[0];
        if (!row) { await client.query('ROLLBACK'); return false; }
        await client.query(
          `UPDATE users SET password_hash=$1,auth_version=auth_version+1,updated_at=CURRENT_TIMESTAMP
           WHERE id=$2`, [passwordHash, row.user_id]);
        await client.query(
          `UPDATE password_reset_tokens SET used_at=CURRENT_TIMESTAMP
           WHERE user_id=$1 AND used_at IS NULL`, [row.user_id]);
        await client.query('COMMIT');
        return true;
      } catch (error) { await client.query('ROLLBACK'); throw error; }
      finally { client.release(); }
    },
  };
}
