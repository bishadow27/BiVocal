import { AppError } from '../../shared/errors/app-error.js';
import type { HealthRepository } from './health.repository.js';

export function createHealthService(repository: HealthRepository) {
  return {
    async check(): Promise<{ status: 'ok'; database: 'connected' }> {
      try {
        await repository.ping();
      } catch {
        throw new AppError(503, 'DATABASE_UNAVAILABLE', 'Database unavailable');
      }
      return { status: 'ok', database: 'connected' };
    },
  };
}

export type HealthService = ReturnType<typeof createHealthService>;
