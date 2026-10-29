import 'dotenv/config';
import { fileURLToPath } from 'node:url';
import { runner } from 'node-pg-migrate';
import { readDatabaseEnv } from '../src/config/env.js';

const direction = process.argv[2];
if (direction !== 'up' && direction !== 'down') {
  console.error('Usage: npm run migrate:up or npm run migrate:down');
  process.exitCode = 1;
} else {
  try {
    const env = readDatabaseEnv(process.env);
    await runner({
      databaseUrl: {
        host: env.DB_HOST, port: env.DB_PORT, database: env.DB_NAME,
        user: env.DB_USER, password: env.DB_PASSWORD, connectionTimeoutMillis: 5000,
      },
      dir: fileURLToPath(new URL('../migrations', import.meta.url)),
      migrationsTable: 'pgmigrations',
      direction,
      ...(direction === 'down' ? { count: 1 } : {}),
      singleTransaction: true,
      checkOrder: true,
      verbose: false,
      logger: {
        debug: () => {}, info: () => {}, warn: () => {}, error: () => {},
      },
    });
    console.log(`Migration ${direction} completed`);
  } catch {
    // Driver errors may contain connection details; do not print raw errors.
    console.error('Migration failed. Check DB_* configuration, permissions and migration state.');
    process.exitCode = 1;
  }
}
