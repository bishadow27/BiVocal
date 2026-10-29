import 'dotenv/config';
import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import type { AddressInfo } from 'node:net';
import type { Server } from 'node:http';
import type { Pool } from 'pg';
import { createApp } from '../src/app.js';
import { readDatabaseEnv } from '../src/config/env.js';
import { createPool } from '../src/config/database.js';
import { createHealthRepository } from '../src/modules/health/health.repository.js';
import { createTopicRepository } from '../src/modules/topics/topic.repository.js';
import { createFlashcardRepository } from '../src/modules/flashcards/flashcard.repository.js';
import { createQuizRepository } from '../src/modules/quiz/quiz.repository.js';

describe('M13 large vocabulary API against isolated PostgreSQL staging', () => {
  let pool: Pool;
  let server: Server;
  let base: string;

  before(async () => {
    const env = readDatabaseEnv(process.env);
    assert.match(env.DB_NAME, /^bivoca_m13_large_[a-z0-9_]+$/,
      'This suite refuses every database except an isolated M13 large staging database');
    pool = createPool(env);
    assert.equal((await pool.query('SELECT current_database() AS name')).rows[0]?.name, env.DB_NAME);
    const database = createHealthRepository(pool);
    server = createApp({ database, topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool) }).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => {
      server.once('listening', resolve); server.once('error', reject);
    });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1`;
  });

  after(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close(error => error ? reject(error) : resolve()); server.closeAllConnections();
    });
    await pool.end();
  });

  it('serves 16 topics with exact active-card counts', async () => {
    const response = await fetch(`${base}/topics/catalog`);
    assert.equal(response.status, 200);
    const topics = await response.json() as Array<{ code: string; flashcardCount: number }>;
    assert.equal(topics.length, 16);
    assert.equal(topics.find(topic => topic.code === 'FOOD')?.flashcardCount, 43);
    assert.equal(topics.find(topic => topic.code === 'SCHOOL')?.flashcardCount, 34);
  });

  it('keeps the legacy topic endpoint and exposes related-topic cards', async () => {
    const response = await fetch(`${base}/topics/FOOD/flashcards`);
    assert.equal(response.status, 200);
    const cards = await response.json() as Array<{ id: number; english: string }>;
    assert.equal(cards.length, 43);
    assert.ok(cards.some(card => card.id <= 300 && card.english === 'apple'));
    assert.ok(cards.some(card => card.id > 300));
  });

  it('paginates and searches English and Vietnamese with topic/CEFR filters', async () => {
    const page = await (await fetch(`${base}/flashcards?topicCode=FOOD&level=A1&page=1&limit=20`)).json() as
      { items: unknown[]; page: number; limit: number; total: number; totalPages: number };
    assert.equal(page.page, 1); assert.equal(page.limit, 20);
    assert.equal(page.items.length, Math.min(page.total, 20));
    assert.ok(page.total > 0); assert.equal(page.totalPages, Math.ceil(page.total / 20));
    const english = await (await fetch(`${base}/flashcards?search=book&limit=20`)).json() as { items: Array<{ english: string }> };
    assert.ok(english.items.some(card => card.english.toLocaleLowerCase('en') === 'book'));
    const vietnamese = await (await fetch(`${base}/flashcards?search=${encodeURIComponent('nhà')}&limit=20`)).json() as
      { items: Array<{ vietnamese: string }> };
    assert.ok(vietnamese.items.some(card => card.vietnamese.toLocaleLowerCase('vi').includes('nhà')));
  });

  it('only exposes quality-approved imported cards to Quiz', async () => {
    const cards = await createQuizRepository(pool).activeCards();
    assert.equal(cards.length, 358);
    const importedEligible = cards.filter(card => card.id > 300);
    assert.equal(importedEligible.length, 58);
  });
});
