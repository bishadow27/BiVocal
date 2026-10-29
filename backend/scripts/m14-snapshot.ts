import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const env = readDatabaseEnv(process.env);
const pool = new pg.Pool({ host: env.DB_HOST, port: env.DB_PORT, database: env.DB_NAME,
  user: env.DB_USER, password: env.DB_PASSWORD, connectionTimeoutMillis: 5000 });
try {
  const counts = (await pool.query(`SELECT
    (SELECT COUNT(*)::int FROM flashcards) flashcards,
    (SELECT COUNT(*)::int FROM topics) topics,
    (SELECT COUNT(*)::int FROM users) users,
    (SELECT COUNT(*)::int FROM user_progress) progress,
    (SELECT COUNT(*)::int FROM quiz_results) quiz_results,
    (SELECT COUNT(*)::int FROM user_scores) user_scores,
    (SELECT COUNT(*)::int FROM xp_history) xp_history`)).rows[0];
  const schema = (await pool.query(`SELECT
    to_regclass('public.user_auth_identities') IS NOT NULL identities,
    to_regclass('public.google_auth_challenges') IS NOT NULL challenges,
    to_regclass('public.password_reset_tokens') IS NOT NULL reset_tokens,
    EXISTS(SELECT 1 FROM information_schema.columns
      WHERE table_schema='public' AND table_name='users' AND column_name='auth_version') auth_version`)).rows[0];
  const auth = schema.identities ? (await pool.query(`SELECT
    (SELECT COUNT(*)::int FROM user_auth_identities) identities,
    (SELECT COUNT(DISTINCT user_id)::int FROM user_auth_identities) linked_users,
    (SELECT COUNT(*)::int FROM users WHERE role = 'ADMIN') admins,
    (SELECT COUNT(*)::int FROM users WHERE password_hash IS NULL) passwordless_users,
    (SELECT COUNT(*)::int FROM user_progress p WHERE EXISTS
      (SELECT 1 FROM user_auth_identities i WHERE i.user_id = p.user_id)) linked_progress,
    (SELECT COUNT(*)::int FROM quiz_results q WHERE EXISTS
      (SELECT 1 FROM user_auth_identities i WHERE i.user_id = q.user_id)) linked_quiz_results,
    (SELECT COALESCE(SUM(s.total_xp), 0)::int FROM user_scores s WHERE EXISTS
      (SELECT 1 FROM user_auth_identities i WHERE i.user_id = s.user_id)) linked_total_xp,
    (SELECT COUNT(*)::int FROM password_reset_tokens
      WHERE used_at IS NULL AND expires_at > CURRENT_TIMESTAMP) active_reset_tokens,
    (SELECT COALESCE(MAX(u.auth_version), 0)::int FROM users u WHERE EXISTS
      (SELECT 1 FROM user_auth_identities i WHERE i.user_id = u.id)) linked_auth_version,
    (SELECT COUNT(*)::int FROM password_reset_tokens WHERE used_at IS NOT NULL) used_reset_tokens,
    EXISTS(SELECT 1 FROM users u JOIN user_auth_identities i ON i.user_id=u.id
      WHERE u.last_login > i.last_login_at + INTERVAL '1 second') password_login_after_google_identity`)).rows[0] : null;
  console.log(JSON.stringify({ database: env.DB_NAME, counts, m14Schema: schema, auth }));
} finally { await pool.end(); }
