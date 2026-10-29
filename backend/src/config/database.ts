import pg from 'pg';
import type { DatabaseEnv } from './env.js';

export function createPool(env: DatabaseEnv): pg.Pool {
  const pool = new pg.Pool({
    host: env.DB_HOST,
    port: env.DB_PORT,
    database: env.DB_NAME,
    user: env.DB_USER,
    password: env.DB_PASSWORD,
    max: 10,
    connectionTimeoutMillis: 5000,
    idleTimeoutMillis: 30000,
    statement_timeout: 5000,
    query_timeout: 6000,
    application_name: 'english-flashcard-backend',
    // Milestone 2 writes users through auth repositories; dataset repositories remain SELECT-only.
  });
  pool.on('error', () => {
    // Do not log the raw pg error/config/connection string.
    console.error('PostgreSQL idle connection failed');
  });
  return pool;
}
