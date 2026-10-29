import type { Pool } from 'pg';

export interface HealthRepository {
  ping(): Promise<void>;
}

export function createHealthRepository(pool: Pool): HealthRepository {
  return {
    async ping() {
      await pool.query('SELECT 1');
    },
  };
}
