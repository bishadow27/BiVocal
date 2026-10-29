import { AppError } from '../../shared/errors/app-error.js';

export class SlidingWindowRateLimiter {
  private readonly entries = new Map<string, number[]>();
  constructor(private readonly limit: number, private readonly windowMs: number) {}
  consume(key: string, now = Date.now()): void {
    const cutoff = now - this.windowMs;
    const active = (this.entries.get(key) ?? []).filter((time) => time > cutoff);
    if (active.length >= this.limit) throw new AppError(429, 'RATE_LIMITED', 'Too many requests. Please try again later');
    active.push(now);
    this.entries.set(key, active);
  }
}
