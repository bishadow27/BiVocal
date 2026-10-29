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

try {
  const applied = await pool.query<{ name: string }>(
    'SELECT name FROM pgmigrations ORDER BY id',
  );
  const migrationName = '1792789200000_add_daily_learning_goals';
  console.log(JSON.stringify({
    database: env.DB_NAME,
    appliedCount: applied.rowCount,
    lastApplied: applied.rows.at(-1)?.name ?? null,
    m15Applied: applied.rows.some((row) => row.name === migrationName),
  }));
} finally {
  await pool.end();
}
