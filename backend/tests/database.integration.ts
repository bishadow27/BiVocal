import 'dotenv/config';
import assert from 'node:assert/strict';
import { after, before, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import type { Pool } from 'pg';
import { z } from 'zod';
import { createApp } from '../src/app.js';
import { readDatabaseEnv } from '../src/config/env.js';
import { createPool } from '../src/config/database.js';
import { createHealthRepository } from '../src/modules/health/health.repository.js';
import { createTopicRepository } from '../src/modules/topics/topic.repository.js';
import { createFlashcardRepository } from '../src/modules/flashcards/flashcard.repository.js';

const topicSchema = z.object({ id: z.number().int(), code: z.string(), name: z.string(),
  description: z.string().nullable(), imageUrl: z.string().nullable() }).strict();
const cardSchema = z.object({
  id: z.number().int(), english: z.string(), vietnamese: z.string(),
  pronunciation: z.string().nullable(), partOfSpeech: z.string().nullable(),
  example: z.string().nullable(), exampleVi: z.string().nullable(), level: z.string().nullable(),
}).strict();

// No mocks or fallback dataset: running without credentials fails this suite.
describe('Read-only integration against configured PostgreSQL', () => {
  let pool: Pool | undefined;
  let server: Server | undefined;
  let base: string;
  let foodCards: z.infer<typeof cardSchema>[];

  before(async () => {
    const env = readDatabaseEnv(process.env);
    pool = createPool(env);
    const database = createHealthRepository(pool);
    await database.ping();
    server = createApp({ topics: createTopicRepository(pool),
      flashcards: createFlashcardRepository(pool), database }).listen(0, '127.0.0.1');
    const listeningServer = server;
    await new Promise<void>((resolve, reject) => {
      listeningServer.once('listening', resolve);
      listeningServer.once('error', reject);
    });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
    const response = await fetch(`${base}/api/v1/topics/FOOD/flashcards`);
    assert.equal(response.status, 200);
    foodCards = z.array(cardSchema).parse(await response.json());
  });

  after(async () => {
    if (server) {
      const runningServer = server;
      await new Promise<void>((resolve, reject) => {
        runningServer.close((error) => error ? reject(error) : resolve());
        runningServer.closeAllConnections();
      });
    }
    await pool?.end();
  });

  it('health actually checks PostgreSQL', async () => {
    const response = await fetch(`${base}/api/v1/health`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: 'ok', database: 'connected' });
  });

  it('topics includes the 10 seed topic codes', async () => {
    const response = await fetch(`${base}/api/v1/topics`);
    assert.equal(response.status, 200);
    const topics = z.array(topicSchema).parse(await response.json());
    for (const code of ['FOOD', 'ANIMALS', 'TRAVEL', 'SCHOOL', 'FAMILY', 'TECHNOLOGY',
                        'WORK', 'HEALTH', 'SHOPPING', 'WEATHER']) {
      assert.ok(topics.some((topic) => topic.code === code), code);
    }
  });

  it('FOOD returns the 30 real seed flashcards', () => {
    assert.equal(foodCards.length, 30);
    assert.ok(foodCards.some((card) => card.english === 'apple' && card.vietnamese === 'quả táo'));
  });

  it('a real flashcard ID returns its matching detail', async () => {
    const card = foodCards[0];
    assert.ok(card);
    const response = await fetch(`${base}/api/v1/flashcards/${card.id}`);
    assert.equal(response.status, 200);
    assert.deepEqual(cardSchema.parse(await response.json()), card);
  });

  it('missing ID and unknown topic return 404', async () => {
    const response = await fetch(`${base}/api/v1/flashcards/2147483647`);
    assert.equal(response.status, 404);
    assert.deepEqual(await response.json(), {
      error: { code: 'FLASHCARD_NOT_FOUND', message: 'Flashcard not found' },
    });
    const topic = await fetch(`${base}/api/v1/topics/ZZ_UNKNOWN_MILESTONE1/flashcards`);
    assert.equal(topic.status, 404);
    assert.deepEqual(await topic.json(), {
      error: { code: 'TOPIC_NOT_FOUND', message: 'Topic not found' },
    });
  });
});
