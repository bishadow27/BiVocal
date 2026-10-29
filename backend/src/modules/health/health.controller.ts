import type { RequestHandler } from 'express';
import type { HealthService } from './health.service.js';

export function createHealthController(service: HealthService): { check: RequestHandler } {
  return {
    async check(_request, response) {
      response.json(await service.check());
    },
  };
}
