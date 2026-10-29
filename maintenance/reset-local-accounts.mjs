import { createRequire } from 'node:module';
import { pathToFileURL } from 'node:url';
import { askNewPassword, OperatorInputError } from './password-prompt.mjs';

export const NEW_ADMIN = Object.freeze({
  username: 'bipassion_admin_new',
  email: 'admin.new@bipassion.local',
  fullName: 'BIPASSION Admin',
});

const countSql = `SELECT
  (SELECT COUNT(*) FROM users)::text AS users,
  (SELECT COALESCE(MAX(id), 0) FROM users)::text AS max_user_id,
  (SELECT COUNT(*) FROM user_progress)::text AS progress,
  (SELECT COUNT(*) FROM quiz_results)::text AS quiz,
  (SELECT COUNT(*) FROM user_scores)::text AS scores,
  (SELECT COUNT(*) FROM xp_history)::text AS xp_history,
  (SELECT COUNT(*) FROM topics)::text AS topics,
  (SELECT COUNT(*) FROM flashcards)::text AS flashcards`;

export function validatePassword(password) {
  if (
    typeof password !== 'string' ||
    Array.from(password).length < 8 ||
    password.length > 72 ||
    Buffer.byteLength(password, 'utf8') > 72
  ) {
    throw new OperatorInputError(
      'Mat khau can it nhat 8 ky tu, toi da 72 byte UTF-8. Chua xoa tai khoan.',
    );
  }
}

async function counts(client) {
  const result = await client.query(countSql);
  const values = Object.fromEntries(
    Object.entries(result.rows[0] ?? {}).map(([key, value]) => [key, Number(value)]),
  );
  const keys = [
    'users',
    'max_user_id',
    'progress',
    'quiz',
    'scores',
    'xp_history',
    'topics',
    'flashcards',
  ];
  if (keys.some((key) => !Number.isSafeInteger(values[key]) || values[key] < 0)) {
    throw new Error('Unable to verify account and content counts.');
  }
  return values;
}

export async function preview(client) {
  await client.query('BEGIN READ ONLY');
  try {
    const database = (await client.query('SELECT current_database() AS database')).rows[0]
      ?.database;
    const current = await counts(client);
    await client.query('ROLLBACK');
    return { database, current, newAdmin: NEW_ADMIN };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  }
}

// Operator-only maintenance. No imports into the backend or Admin Web.
// DELETE cascades only account data under the existing M2-M5 foreign keys.
// Do not reset sequences: existing JWT subjects must never become the new ADMIN ID.
export async function resetAllAccounts(client, password, hashPassword) {
  validatePassword(password);
  const passwordHash = await hashPassword(password);
  await client.query('BEGIN');
  try {
    await client.query("SET LOCAL lock_timeout = '5s'");
    await client.query("SET LOCAL statement_timeout = '30s'");
    await client.query('LOCK TABLE topics, flashcards IN SHARE MODE');
    await client.query(
      'LOCK TABLE users, user_progress, quiz_results, user_scores, xp_history IN ACCESS EXCLUSIVE MODE',
    );
    const before = await counts(client);
    await client.query('DELETE FROM users');
    const inserted = await client.query(
      `INSERT INTO users (username, email, password_hash, full_name, role)
       VALUES ($1, $2, $3, $4, 'ADMIN') RETURNING id, username, email, role`,
      [NEW_ADMIN.username, NEW_ADMIN.email, passwordHash, NEW_ADMIN.fullName],
    );
    const admin = inserted.rows[0];
    const after = await counts(client);
    if (
      !admin ||
      admin.role !== 'ADMIN' ||
      admin.email !== NEW_ADMIN.email ||
      !Number.isSafeInteger(admin.id) ||
      admin.id <= before.max_user_id ||
      after.users !== 1 ||
      after.progress !== 0 ||
      after.quiz !== 0 ||
      after.scores !== 0 ||
      after.xp_history !== 0 ||
      before.topics !== after.topics ||
      before.flashcards !== after.flashcards
    ) {
      throw new Error('Reset verification failed; do not commit.');
    }
    await client.query('COMMIT');
    return { before, after, admin };
  } catch (error) {
    await client.query('ROLLBACK').catch(() => {});
    throw error;
  }
}

async function run() {
  const mode = process.argv[2];
  if (!['--preview', '--apply'].includes(mode) || process.argv.length !== 3) {
    throw new OperatorInputError('Hay dung --preview hoac --apply.');
  }
  if (
    process.env.NODE_ENV === 'production' ||
    !['localhost', '127.0.0.1', '::1'].includes(process.env.DB_HOST)
  ) {
    throw new OperatorInputError('Script chi dung cho database local, khong phai production.');
  }
  const port = Number(process.env.DB_PORT);
  if (
    !process.env.DB_NAME ||
    !process.env.DB_USER ||
    !process.env.DB_PASSWORD ||
    !Number.isInteger(port) ||
    port < 1 ||
    port > 65535
  ) {
    throw new OperatorInputError(
      'Cau hinh DB chua du. Hay chay lenh trong thu muc backend voi --env-file=.env.',
    );
  }
  let password;
  if (mode === '--apply') {
    if (process.stdin.isTTY) {
      console.log('Xoa TOAN BO tai khoan va du lieu hoc; giu nguyen topics/flashcards.');
      console.log(`Tai khoan ADMIN moi: ${NEW_ADMIN.email}`);
      password = await askNewPassword();
    } else {
      const chunks = [];
      let size = 0;
      for await (const chunk of process.stdin) {
        size += chunk.length;
        if (size > 4096) throw new Error('Unexpected password input.');
        chunks.push(chunk);
      }
      const encoded = Buffer.concat(chunks).toString('ascii').trim();
      if (!/^[A-Za-z0-9+/]+={0,2}$/.test(encoded)) throw new Error('Missing password input.');
      password = Buffer.from(encoded, 'base64').toString('utf8');
    }
    validatePassword(password);
  }
  const backendRequire = createRequire(new URL('../backend/package.json', import.meta.url));
  const { Pool } = backendRequire('pg');
  const bcrypt = backendRequire('bcrypt');
  const pool = new Pool({
    host: process.env.DB_HOST,
    port,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    connectionTimeoutMillis: 5000,
    max: 1,
  });
  let client;
  try {
    client = await pool.connect();
    const result =
      mode === '--preview'
        ? await preview(client)
        : await resetAllAccounts(client, password, (value) => bcrypt.hash(value, 12));
    console.log(JSON.stringify({ mode: mode.slice(2), ...result }, null, 2));
    if (mode === '--apply')
      console.log('RESET SUCCESS: all old accounts removed; new ADMIN created.');
  } finally {
    password = undefined;
    client?.release();
    await pool.end();
  }
}

if (process.argv[1] && pathToFileURL(process.argv[1]).href === import.meta.url) {
  run().catch((error) => {
    // Never expose PostgreSQL errors, password/hash, env values or SQL parameters.
    console.error(
      error instanceof OperatorInputError
        ? error.message
        : 'Maintenance did not report success. Check local database configuration, migrations and active database locks. No successful reset should be assumed.',
    );
    process.exitCode = 1;
  });
}
