import assert from 'node:assert/strict';
import { afterEach, beforeEach, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { createApp } from '../src/app.js';
import type { Repositories } from '../src/routes.js';
import type { TopicRow } from '../src/modules/topics/topic.repository.js';
import type { FlashcardRow } from '../src/modules/flashcards/flashcard.repository.js';

const food: TopicRow = {
  id: 101, code: 'FOOD', name: 'Food', description: 'Thức ăn', image_url: null,
};
const apple: FlashcardRow = {
  id: 501, english: 'apple', vietnamese: 'quả táo', pronunciation: '/ˈæpəl/',
  part_of_speech: 'noun', example: 'I eat an apple every morning.',
  example_vi: 'Tôi ăn một quả táo mỗi sáng.', level: 'A1',
  content_status: 'SEMANTIC_VERIFIED',
};
const appleDto = {
  id: 501, english: 'apple', vietnamese: 'quả táo', pronunciation: '/ˈæpəl/',
  partOfSpeech: 'noun', example: 'I eat an apple every morning.',
  exampleVi: 'Tôi ăn một quả táo mỗi sáng.', level: 'A1',
  contentStatus: 'SEMANTIC_VERIFIED',
};

describe('HTTP API with repository doubles (no PostgreSQL)', () => {
  let server: Server;
  let base: string;
  let databaseDown: boolean;
  let topicsFail: boolean;
  let requestedCodes: string[];
  let requestedIds: number[];
  let repositories: Repositories;

  beforeEach(async () => {
    databaseDown = false;
    topicsFail = false;
    requestedCodes = [];
    requestedIds = [];
    repositories = {
      database: {
        async ping() { if (databaseDown) throw new Error('password=should-never-be-exposed'); },
      },
      topics: {
        async findAll() {
          if (topicsFail) throw new Error('SELECT secret FROM topics; password=private');
          return [{ ...food, created_at: 'internal-field' }];
        },
        async findCatalog() {
          if (topicsFail) throw new Error('SELECT secret FROM topics; password=private');
          return [{ ...food, flashcard_count: '1', topic_group: 'SEMANTIC' }];
        },
        async findByCode(code) {
          requestedCodes.push(code);
          if (code === 'FOOD') return food;
          if (code === 'EMPTY') return { ...food, id: 999, code: 'EMPTY' };
          return undefined;
        },
      },
      flashcards: {
        async findByTopicCode(code) {
          requestedCodes.push(code);
          return code === 'FOOD' ? [{ ...apple, topic_id: 101, is_active: true }] : [];
        },
        async search(query) {
          requestedCodes.push(query.topicCode ?? '');
          return { rows: query.search === 'missing' ? [] : [apple], total: query.search === 'missing' ? 0 : 1 };
        },
        async findById(id) {
          requestedIds.push(id);
          return id === 501 ? apple : undefined;
        },
      },
    };
    server = createApp(repositories).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => {
      server.once('listening', resolve);
      server.once('error', reject);
    });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}`;
  });

  afterEach(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => error ? reject(error) : resolve());
      server.closeAllConnections();
    });
  });

  it('health verifies repository and returns the exact successful response', async () => {
    const response = await fetch(`${base}/api/v1/health`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), { status: 'ok', database: 'connected' });
    assert.equal(response.headers.get('x-powered-by'), null);
  });

  it('health returns 503 without connection details when PostgreSQL fails', async () => {
    databaseDown = true;
    const response = await fetch(`${base}/api/v1/health`);
    assert.equal(response.status, 503);
    assert.deepEqual(await response.json(), {
      error: { code: 'DATABASE_UNAVAILABLE', message: 'Database unavailable' },
    });
  });

  it('topics converts database columns and excludes internal fields', async () => {
    const response = await fetch(`${base}/api/v1/topics`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), [{
      id: 101, code: 'FOOD', name: 'Food', description: 'Thức ăn', imageUrl: null,
    }]);
  });

  it('topic catalog returns a numeric flashcard count without changing the legacy list', async () => {
    const response = await fetch(`${base}/api/v1/topics/catalog`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), [{
      id: 101, code: 'FOOD', name: 'Food', description: 'Thức ăn', imageUrl: null,
      flashcardCount: 1,
      topicGroup: 'SEMANTIC',
    }]);
  });

  it('search returns a stable paginated DTO and accepts supported filters', async () => {
    const response = await fetch(`${base}/api/v1/flashcards?search=apple&topicCode=FOOD&level=A1&page=2&limit=10`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      items: [appleDto], page: 2, limit: 10, total: 1, totalPages: 1,
    });
    assert.deepEqual(requestedCodes, ['FOOD']);
  });

  it('search returns an empty page and rejects malformed query values', async () => {
    const empty = await fetch(`${base}/api/v1/flashcards?search=missing`);
    assert.equal(empty.status, 200);
    assert.deepEqual(await empty.json(), {
      items: [], page: 1, limit: 20, total: 0, totalPages: 0,
    });
    for (const query of ['page=0', 'page=01', 'limit=101', 'level=C1', 'topicCode=food',
      'search=', 'search=a&search=b', 'unknown=value']) {
      const response = await fetch(`${base}/api/v1/flashcards?${query}`);
      assert.equal(response.status, 400, query);
      assert.deepEqual(await response.json(), {
        error: { code: 'INVALID_FLASHCARD_QUERY', message: 'Invalid flashcard query' },
      });
    }
  });

  it('flashcards uses topic code and returns camelCase DTOs', async () => {
    const response = await fetch(`${base}/api/v1/topics/FOOD/flashcards`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), [appleDto]);
    assert.deepEqual(requestedCodes, ['FOOD', 'FOOD']);
  });

  it('an existing topic without flashcards returns an empty array', async () => {
    const response = await fetch(`${base}/api/v1/topics/EMPTY/flashcards`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), []);
  });

  it('unknown topic returns 404 before reading its flashcards', async () => {
    const response = await fetch(`${base}/api/v1/topics/UNKNOWN/flashcards`);
    assert.equal(response.status, 404);
    assert.deepEqual(await response.json(), {
      error: { code: 'TOPIC_NOT_FOUND', message: 'Topic not found' },
    });
    assert.deepEqual(requestedCodes, ['UNKNOWN']);
  });

  it('valid flashcard detail has the specified DTO', async () => {
    const response = await fetch(`${base}/api/v1/flashcards/501`);
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), appleDto);
    assert.deepEqual(requestedIds, [501]);
  });

  it('missing flashcard returns the required 404 error', async () => {
    const response = await fetch(`${base}/api/v1/flashcards/2147483647`);
    assert.equal(response.status, 404);
    assert.deepEqual(await response.json(), {
      error: { code: 'FLASHCARD_NOT_FOUND', message: 'Flashcard not found' },
    });
  });

  it('rejects malformed, negative, decimal and overflowing IDs before querying', async () => {
    for (const id of ['0', '-1', '1.5', '1abc', '2147483648', '01', '1e3']) {
      const response = await fetch(`${base}/api/v1/flashcards/${id}`);
      assert.equal(response.status, 400, id);
      assert.deepEqual(await response.json(), {
        error: { code: 'INVALID_FLASHCARD_ID', message: 'Invalid flashcard id' },
      });
    }
    assert.deepEqual(requestedIds, []);
  });

  it('rejects invalid topic codes before calling a repository', async () => {
    for (const code of ['food', "FOOD' OR 1=1--", 'A'.repeat(51)]) {
      const response = await fetch(`${base}/api/v1/topics/${encodeURIComponent(code)}/flashcards`);
      assert.equal(response.status, 400);
      assert.deepEqual(await response.json(), {
        error: { code: 'INVALID_TOPIC_CODE', message: 'Invalid topic code' },
      });
    }
    assert.deepEqual(requestedCodes, []);
  });

  it('central middleware handles async rejection without exposing the raw error', async () => {
    topicsFail = true;
    const response = await fetch(`${base}/api/v1/topics`);
    assert.equal(response.status, 500);
    assert.deepEqual(await response.json(), {
      error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' },
    });
  });

  it('malformed percent encoding returns a client error', async () => {
    const response = await fetch(`${base}/api/v1/topics/%E0%A4%A/flashcards`);
    assert.equal(response.status, 400);
    assert.deepEqual(await response.json(), {
      error: { code: 'INVALID_REQUEST_PATH', message: 'Invalid request path' },
    });
  });

  it('unknown routes and write requests return the consistent 404 format', async () => {
    for (const [path, method] of [['/topics', 'GET'], ['/api/v1/unknown', 'GET'],
                                 ['/api/v1/topics', 'POST']] as const) {
      const response = await fetch(`${base}${path}`, { method });
      assert.equal(response.status, 404);
      assert.deepEqual(await response.json(), {
        error: { code: 'ROUTE_NOT_FOUND', message: 'Route not found' },
      });
    }
  });
});
