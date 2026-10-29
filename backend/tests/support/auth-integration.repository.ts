import type { Pool } from 'pg';

// All integration-only SQL lives here. No migration, DROP, TRUNCATE or seed operations.
export function createAuthIntegrationRepository(pool: Pool) {
  return {
    async usersMigrationExists(): Promise<boolean> {
      const result = await pool.query<{ present: boolean }>(
        "SELECT to_regclass('users') IS NOT NULL AS present",
      );
      return result.rows[0]?.present === true;
    },
    async datasetSnapshot(): Promise<string> {
      const topics = await pool.query('SELECT * FROM topics ORDER BY id');
      const flashcards = await pool.query('SELECT * FROM flashcards ORDER BY id');
      return JSON.stringify({ topics: topics.rows, flashcards: flashcards.rows });
    },
    async testUserRecord(username: string, email: string) {
      const result = await pool.query<{ id: number; password_hash: string; last_login: Date | null }>(
        'SELECT id, password_hash, last_login FROM users WHERE username = $1 AND email = $2',
        [username, email],
      );
      return result.rows[0];
    },
    async cleanupOwnedUser(id: number, username: string, email: string): Promise<void> {
      // Exact ID AND UUID username AND email. Never delete by prefix or all users.
      await pool.query('DELETE FROM users WHERE id = $1 AND username = $2 AND email = $3',
        [id, username, email]);
    },
  };
}
