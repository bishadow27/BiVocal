import 'dotenv/config';
import pg from 'pg';
import { readDatabaseEnv } from '../src/config/env.js';

const env = readDatabaseEnv(process.env);
const pool = new pg.Pool({
  host: env.DB_HOST,
  port: env.DB_PORT,
  database: env.DB_NAME,
  user: env.DB_USER,
  password: env.DB_PASSWORD,
});

const protectedTables = [
  'topics',
  'flashcards',
  'users',
  'user_auth_identities',
  'user_progress',
  'quiz_results',
  'user_scores',
  'xp_history',
] as const;

try {
  const tables: Record<string, { count: number; checksum: string }> = {};
  for (const table of protectedTables) {
    const result = await pool.query<{ count: number; checksum: string }>(`
      SELECT count(*)::int AS count,
             md5(COALESCE(string_agg(md5(to_jsonb(row_data)::text), ''
                 ORDER BY to_jsonb(row_data)::text), '')) AS checksum
      FROM ${table} AS row_data
    `);
    tables[table] = result.rows[0]!;
  }
  console.log(JSON.stringify({ database: env.DB_NAME, tables }));
} finally {
  await pool.end();
}
