import assert from 'node:assert/strict';
import { afterEach, beforeEach, describe, it } from 'node:test';
import type { Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import jwt from 'jsonwebtoken';
import { createApp } from '../src/app.js';
import { createTokenService } from '../src/modules/auth/token.service.js';
import type { AuthModule } from '../src/modules/auth/auth.service.js';
import type { QuizRepository } from '../src/modules/quiz/quiz.repository.js';
import type { QuizCard, QuizHistoryRow, QuizQuestionDto, QuizAnswerDto, QuizSummaryDto } from '../src/modules/quiz/quiz.types.js';
import type { ProgressRow } from '../src/modules/progress/progress.types.js';
import type { Repositories } from '../src/routes.js';

const secret = 'quiz-test-only-secret-over-32-characters';
const tokens = createTokenService(secret, 3600);
const userA = 41; const userB = 42;
const auth: AuthModule = { tokens, service: {
  async register() { throw new Error('Unused auth double'); },
  async login() { throw new Error('Unused auth double'); },
  async currentUser() { throw new Error('Unused auth double'); },
} as unknown as AuthModule['service'] };
function quizDouble() {
  const cards: (QuizCard & { active: boolean })[] = Array.from({ length: 24 }, (_, n) => ({
    id: n + 1, topic_id: n < 16 ? 1 : 2, english: `word${n + 1}`, vietnamese: `meaning${n + 1}`, active: true,
  }));
  const topics = new Map([['FOOD', 1], ['TRAVEL', 2], ['EMPTY', 3]]);
  const history: (QuizHistoryRow & { user_id: number })[] = [];
  const progress = new Map<string, ProgressRow>();
  const calls: { operation: string; userId?: number; limit?: number; topicCode?: string }[] = [];
  let failure: unknown;
  let tick = Date.parse('2026-10-05T00:00:00.000Z');
  const repository: QuizRepository = {
    async topicExists(code) { calls.push({ operation: 'topic', topicCode: code }); if (failure) throw failure; return topics.has(code); },
    async activeCards(code) {
      calls.push({ operation: 'cards', ...(code === undefined ? {} : { topicCode: code }) });
      if (failure) throw failure;
      return cards.filter((card) => card.active && (code === undefined || card.topic_id === topics.get(code)));
    },
    async answer(userId, questionId, selectedId) {
      calls.push({ operation: 'answer', userId }); if (failure) throw failure;
      const question = cards.find((card) => card.id === questionId && card.active);
      if (!question) return { status: 'question_missing' };
      const selected = cards.find((card) => card.id === selectedId && card.active && card.topic_id === question.topic_id);
      if (!selected) return { status: 'option_invalid' };
      const correct = questionId === selectedId;
      const now = new Date(++tick);
      const key = `${userId}:${questionId}`;
      const row = progress.get(key) ?? { flashcard_id: questionId, memory_level: 0,
        review_count: 0, correct_count: 0, wrong_count: 0, is_favorite: false,
        last_review: null, next_review: null, created_at: now, updated_at: now };
      row.review_count++;
      if (correct) { row.correct_count++; row.memory_level = Math.min(5, row.memory_level + 1); }
      else { row.wrong_count++; row.memory_level = Math.max(0, row.memory_level - 1); }
      row.last_review = row.updated_at = now;
      row.next_review = new Date(now.getTime() + (correct
        ? [0, 1, 3, 7, 14, 30][row.memory_level]! * 86400000 : 600000));
      progress.set(key, row);
      const result: QuizHistoryRow & { user_id: number } = { id: history.length + 1, user_id: userId,
        flashcard_id: questionId, selected_flashcard_id: selectedId, is_correct: correct,
        question_type: 'EN_TO_VI', answered_at: now, question: question.english,
        correct_answer: question.vietnamese, selected_answer: selected.vietnamese };
      history.push(result);
      return { status: 'answered', result, progress: row,
        correctAnswer: question.vietnamese, selectedAnswer: selected.vietnamese };
    },
    async history(userId, limit) {
      calls.push({ operation: 'history', userId, limit }); if (failure) throw failure;
      return history.filter((row) => row.user_id === userId).sort((a, b) =>
        b.answered_at.getTime() - a.answered_at.getTime() || b.id - a.id).slice(0, limit);
    },
    async summary(userId) {
      calls.push({ operation: 'summary', userId }); if (failure) throw failure;
      const rows = history.filter((row) => row.user_id === userId);
      return { total_answers: String(rows.length), correct_answers: String(rows.filter((row) => row.is_correct).length),
        wrong_answers: String(rows.filter((row) => !row.is_correct).length) };
    },
  };
  return { repository, cards, history, progress, calls, fail(error: unknown) { failure = error; } };
}

describe('Milestone 4 Quiz HTTP API (real JWT; repository doubles; no PostgreSQL)', () => {
  let server: Server; let base: string; let fake: ReturnType<typeof quizDouble>;
  beforeEach(async () => {
    fake = quizDouble();
    const repositories: Repositories = { quiz: fake.repository, database: { async ping() {} },
      topics: { async findAll() { return []; }, async findCatalog() { return []; }, async findByCode() { return undefined; } },
      flashcards: { async findById() { return undefined; }, async findByTopicCode() { return []; },
        async search() { return { rows: [], total: 0 }; } } };
    server = createApp(repositories, auth).listen(0, '127.0.0.1');
    await new Promise<void>((resolve, reject) => { server.once('listening', resolve); server.once('error', reject); });
    base = `http://127.0.0.1:${(server.address() as AddressInfo).port}/api/v1/quiz`;
  });
  afterEach(async () => {
    await new Promise<void>((resolve, reject) => {
      server.close((error) => error ? reject(error) : resolve()); server.closeAllConnections();
    });
  });
  function request(path: string, method = 'GET', body?: unknown,
    header: string | null = `Bearer ${tokens.sign(userA)}`, extraHeaders: Record<string, string> = {}) {
    return fetch(base + path, { method, headers: { ...extraHeaders,
      ...(header === null ? {} : { Authorization: header }),
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
  }
  const answer = (question = 1, selected = 1, token?: string) => request('/answer', 'POST',
    { flashcardId: question, selectedFlashcardId: selected }, token);
  async function dto(response: globalThis.Response): Promise<QuizAnswerDto> {
    assert.equal(response.status, 200); return await response.json() as QuizAnswerDto;
  }
  async function error(response: globalThis.Response, status: number, code: string) {
    assert.equal(response.status, status);
    assert.equal((await response.json() as { error: { code: string } }).error.code, code);
  }
  const endpoints: [string, string, unknown?][] = [
    ['/questions', 'GET'], ['/answer', 'POST', { flashcardId: 1, selectedFlashcardId: 1 }],
    ['/history', 'GET'], ['/summary', 'GET'],
  ];
  function assertQuestion(question: QuizQuestionDto) {
    assert.deepEqual(Object.keys(question).sort(), ['flashcardId', 'questionType', 'prompt', 'options'].sort());
    assert.equal(question.questionType, 'EN_TO_VI'); assert.equal(question.options.length, 4);
    assert.equal(new Set(question.options.map((option) => option.flashcardId)).size, 4);
    assert.equal(new Set(question.options.map((option) => option.text.trim().normalize('NFC').toLowerCase())).size, 4);
    assert.equal(question.options.filter((option) => option.flashcardId === question.flashcardId).length, 1);
    const original = fake.cards.find((card) => card.id === question.flashcardId)!;
    assert.equal(question.prompt, original.english);
    for (const option of question.options) {
      assert.deepEqual(Object.keys(option).sort(), ['flashcardId', 'text']);
      const card = fake.cards.find((candidate) => candidate.id === option.flashcardId)!;
      assert.equal(card.active, true); assert.equal(card.topic_id, original.topic_id); assert.equal(option.text, card.vietnamese);
    }
  }

  it('requires JWT on all four endpoints', async () => {
    for (const [path, method, body] of endpoints) await error(await request(path, method, body, null), 401, 'AUTH_TOKEN_MISSING');
    assert.equal(fake.calls.length, 0);
  });
  it('rejects invalid JWT on every endpoint', async () => {
    for (const [path, method, body] of endpoints) await error(await request(path, method, body, 'Bearer invalid'), 401, 'AUTH_TOKEN_INVALID');
    assert.equal(fake.calls.length, 0);
  });
  it('rejects expired JWT on every endpoint', async () => {
    const token = jwt.sign({ sub: String(userA), exp: 1 }, secret);
    for (const [path, method, body] of endpoints) await error(await request(path, method, body, `Bearer ${token}`), 401, 'AUTH_TOKEN_INVALID');
  });
  it('authenticates answer before JSON parsing', async () => {
    await error(await fetch(base + '/answer', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{bad' }),
      401, 'AUTH_TOKEN_MISSING');
  });
  it('questions use default limit 10 without writes', async () => {
    const response = await request('/questions'); assert.equal(response.status, 200);
    const questions = await response.json() as QuizQuestionDto[]; assert.equal(questions.length, 10);
    assert.equal(new Set(questions.map((q) => q.flashcardId)).size, 10); questions.forEach(assertQuestion);
    assert.equal(fake.history.length, 0); assert.equal(fake.progress.size, 0);
  });
  it('questions accept custom limits including 1 and 20', async () => {
    for (const limit of [1, 4, 20]) {
      const response = await request(`/questions?limit=${limit}`); assert.equal(response.status, 200);
      const questions = await response.json() as QuizQuestionDto[]; assert.equal(questions.length, limit); questions.forEach(assertQuestion);
    }
  });
  it('questions reject zero/negative/overflow/noninteger/ambiguous limits', async () => {
    for (const limit of ['0', '-1', '21', '1.5', 'abc', '01', '1e1', '999999999999999999']) {
      await error(await request(`/questions?limit=${limit}`), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('questions reject repeated query limits or topic codes', async () => {
    for (const query of ['limit=1&limit=2', 'topicCode=FOOD&topicCode=TRAVEL']) {
      await error(await request(`/questions?${query}`), 400, 'VALIDATION_ERROR');
    }
  });
  it('questions validate topic codes consistently with existing topics', async () => {
    for (const topic of ['food', 'INVALID-CODE', '01', 'A'.repeat(51), '']) {
      await error(await request(`/questions?topicCode=${topic}`), 400, 'VALIDATION_ERROR');
    }
  });
  it('questions respect a valid topic filter', async () => {
    const response = await request('/questions?topicCode=FOOD&limit=5'); assert.equal(response.status, 200);
    const questions = await response.json() as QuizQuestionDto[]; assert.equal(questions.length, 5);
    questions.forEach((q) => { assertQuestion(q); assert.ok(q.flashcardId <= 16); });
    assert.ok(fake.calls.some((call) => call.operation === 'cards' && call.topicCode === 'FOOD'));
  });
  it('unknown topic is 404 TOPIC_NOT_FOUND', async () => {
    await error(await request('/questions?topicCode=UNKNOWN'), 404, 'TOPIC_NOT_FOUND');
    assert.ok(!fake.calls.some((call) => call.operation === 'cards'));
  });
  it('existing empty topic fails with QUIZ_NOT_ENOUGH_FLASHCARDS', async () => {
    await error(await request('/questions?topicCode=EMPTY'), 400, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
  });
  it('questions only include active question and distractor cards', async () => {
    fake.cards[0]!.active = false; fake.cards[1]!.active = false;
    const response = await request('/questions?topicCode=FOOD'); assert.equal(response.status, 200);
    (await response.json() as QuizQuestionDto[]).forEach(assertQuestion);
  });
  it('a topic with fewer than four active candidates cannot generate even one question', async () => {
    fake.cards.forEach((card) => { if (card.topic_id === 1 && card.id > 3) card.active = false; });
    await error(await request('/questions?topicCode=FOOD&limit=1'), 400, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
  });
  it('rejects insufficient distinct option texts, including case/Unicode normalization duplicates', async () => {
    fake.cards.filter((card) => card.topic_id === 1).forEach((card, n) => {
      card.vietnamese = ['café', 'CAFE\u0301', 'milk'][n % 3]!;
    });
    await error(await request('/questions?topicCode=FOOD&limit=1'), 400, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
  });
  it('avoids duplicate meanings when enough distinct options remain', async () => {
    fake.cards[1]!.vietnamese = fake.cards[0]!.vietnamese.toUpperCase();
    const response = await request('/questions?topicCode=FOOD&limit=10'); assert.equal(response.status, 200);
    (await response.json() as QuizQuestionDto[]).forEach(assertQuestion);
  });
  it('excludes ambiguous duplicate English prompts from the question pool', async () => {
    fake.cards[0]!.english = 'bank'; fake.cards[1]!.english = 'BANK';
    const response = await request('/questions?topicCode=FOOD&limit=10');
    assert.equal(response.status, 200);
    const questions = await response.json() as QuizQuestionDto[];
    assert.ok(questions.every((question) => question.flashcardId !== 1 && question.flashcardId !== 2));
  });
  it('rejects distractors that share a normalized meaning component', async () => {
    fake.cards[0]!.vietnamese = 'đồ ăn, thức ăn';
    fake.cards[1]!.vietnamese = 'thức ăn';
    const response = await request('/questions?topicCode=FOOD&limit=10');
    assert.equal(response.status, 200);
    const questions = await response.json() as QuizQuestionDto[];
    for (const question of questions) {
      const meanings = question.options.map((option) => new Set(option.text.toLocaleLowerCase('vi')
        .split(/[,;/|]+/).map((part) => part.trim())));
      for (let left = 0; left < meanings.length; left++) for (let right = left + 1; right < meanings.length; right++) {
        assert.ok(![...meanings[left]!].some((part) => meanings[right]!.has(part)));
      }
    }
  });
  it('prefers distractors with the same POS and CEFR when enough exist', async () => {
    fake.cards.filter((card) => card.topic_id === 1).forEach((card, index) => {
      card.part_of_speech = index < 8 ? 'noun' : 'verb'; card.level = 'A1';
    });
    const response = await request('/questions?topicCode=FOOD&limit=10');
    assert.equal(response.status, 200);
    for (const question of await response.json() as QuizQuestionDto[]) {
      const original = fake.cards.find((card) => card.id === question.flashcardId)!;
      for (const option of question.options) {
        const card = fake.cards.find((candidate) => candidate.id === option.flashcardId)!;
        assert.equal(card.part_of_speech, original.part_of_speech);
        assert.equal(card.level, original.level);
      }
    }
  });
  it('does not borrow distractors from another topic to mask a short topic', async () => {
    fake.cards.filter((card) => card.topic_id === 1).forEach((card) => { card.active = card.id <= 3; });
    await error(await request('/questions?topicCode=FOOD&limit=1'), 400, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
  });
  it('fails when requested distinct question count exceeds eligible cards', async () => {
    await error(await request('/questions?topicCode=TRAVEL&limit=10'), 400, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
  });
  it('questions contain exactly the specified public fields without answer/progress metadata', async () => {
    fake.cards.forEach((card) => Object.assign(card, { password_hash: 'private-test-hash', correctIndex: 0,
      JWT_SECRET: secret, user_progress: 'private-progress' }));
    const response = await request('/questions'); assert.equal(response.status, 200);
    const text = await response.text(); const questions = JSON.parse(text) as QuizQuestionDto[];
    questions.forEach(assertQuestion);
    for (const field of ['correct', 'isCorrect', 'correctIndex', 'correctOption', 'answerId', 'correctFlashcardId',
      'answer', 'correctAnswer', 'password_hash', 'user_progress', secret]) {
      assert.ok(!text.includes(`"${field}"`));
    }
  });
  it('returns the correct answer DTO and first progress increments', async () => {
    const result = await dto(await answer());
    assert.deepEqual([result.resultId, result.flashcardId, result.selectedFlashcardId, result.correct], [1, 1, 1, true]);
    assert.equal(result.correctAnswer, 'meaning1'); assert.equal(result.selectedAnswer, 'meaning1');
    assert.deepEqual([result.progress.reviewCount, result.progress.correctCount, result.progress.wrongCount,
      result.progress.memoryLevel], [1, 1, 0, 1]);
    assert.equal(new Date(result.answeredAt).toISOString(), result.answeredAt);
  });
  it('backend grades wrong selection and reveals correct/selected answers only after submit', async () => {
    const result = await dto(await answer(1, 2));
    assert.equal(result.correct, false); assert.equal(result.correctAnswer, 'meaning1'); assert.equal(result.selectedAnswer, 'meaning2');
    assert.deepEqual([result.progress.reviewCount, result.progress.correctCount, result.progress.wrongCount,
      result.progress.memoryLevel], [1, 0, 1, 0]);
  });
  it('rejects all client grading/business fields and userId override', async () => {
    for (const field of ['userId', 'isCorrect', 'correct', 'correctAnswer', 'memoryLevel', 'score', 'xp', 'extra']) {
      await error(await request('/answer', 'POST', { flashcardId: 1, selectedFlashcardId: 2, [field]: true }),
        400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.history.length, 0); assert.equal(fake.calls.length, 0);
  });
  it('missing required IDs are validation errors', async () => {
    for (const body of [{}, { flashcardId: 1 }, { selectedFlashcardId: 1 }, [], undefined]) {
      await error(await request('/answer', 'POST', body), 400, 'VALIDATION_ERROR');
    }
    assert.equal(fake.calls.length, 0);
  });
  it('rejects malformed, nonnumber, fractional and overflowing IDs in both fields', async () => {
    for (const id of [0, -1, 1.5, 2147483648, '1abc', '01', '1e3', '1', null, true, [], {}]) {
      for (const field of ['flashcardId', 'selectedFlashcardId']) {
        await error(await request('/answer', 'POST', { flashcardId: 1, selectedFlashcardId: 1, [field]: id }),
          400, 'INVALID_FLASHCARD_ID');
      }
    }
    assert.equal(fake.calls.length, 0);
  });
  it('accepts SERIAL upper boundary format then applies existence validation', async () => {
    await error(await answer(2147483647, 1), 404, 'FLASHCARD_NOT_FOUND');
    await error(await answer(1, 2147483647), 400, 'INVALID_QUIZ_OPTION');
  });
  it('missing/inactive question returns 404 and creates no data', async () => {
    await error(await answer(999, 1), 404, 'FLASHCARD_NOT_FOUND');
    fake.cards[0]!.active = false;
    await error(await answer(), 404, 'FLASHCARD_NOT_FOUND');
    assert.equal(fake.history.length, 0); assert.equal(fake.progress.size, 0);
  });
  it('missing/inactive selected option returns INVALID_QUIZ_OPTION without writes', async () => {
    await error(await answer(1, 999), 400, 'INVALID_QUIZ_OPTION'); fake.cards[1]!.active = false;
    await error(await answer(1, 2), 400, 'INVALID_QUIZ_OPTION');
    assert.equal(fake.history.length, 0); assert.equal(fake.progress.size, 0);
  });
  it('different-topic selected option is rejected', async () => {
    await error(await answer(1, 17), 400, 'INVALID_QUIZ_OPTION');
    assert.equal(fake.history.length, 0);
  });
  it('repeated answers are allowed and have distinct result IDs', async () => {
    const first = await dto(await answer()); const next = await dto(await answer());
    assert.notEqual(first.resultId, next.resultId); assert.equal(fake.history.length, 2);
    assert.equal(next.progress.reviewCount, 2);
  });
  it('correct/wrong quiz answers update the progress counters consistently', async () => {
    await dto(await answer()); const result = await dto(await answer(1, 2));
    assert.deepEqual([result.progress.reviewCount, result.progress.correctCount, result.progress.wrongCount,
      result.progress.memoryLevel], [2, 1, 1, 0]);
  });
  it('quiz progress memory caps at five and floors at zero', async () => {
    for (let n = 0; n < 7; n++) await dto(await answer());
    assert.equal((await dto(await answer())).progress.memoryLevel, 5);
    for (let n = 0; n < 7; n++) await dto(await answer(1, 2));
    assert.equal((await dto(await answer(1, 2))).progress.memoryLevel, 0);
  });
  it('quiz preserves favorite/createdAt and schedules a wrong answer promptly', async () => {
    const first = await dto(await answer()); fake.progress.get(`${userA}:1`)!.is_favorite = true;
    const next = await dto(await answer(1, 2));
    assert.equal(next.progress.isFavorite, true); assert.equal(next.progress.createdAt, first.progress.createdAt);
    assert.ok(next.progress.updatedAt > first.progress.updatedAt); assert.equal(next.progress.lastReview, next.progress.updatedAt);
    assert.equal(Date.parse(next.progress.nextReview!), Date.parse(next.progress.lastReview!) + 600000);
  });
  it('history isolates JWT users and does not accept query/header identity', async () => {
    await dto(await answer()); await dto(await answer(2, 2, `Bearer ${tokens.sign(userB)}`));
    const response = await request(`/history?userId=${userB}`, 'GET', undefined, undefined, { 'X-User-Id': String(userB) });
    assert.equal(response.status, 200); const history = await response.json() as { flashcardId: number }[];
    assert.deepEqual(history.map((row) => row.flashcardId), [1]);
    const b = await request('/history', 'GET', undefined, `Bearer ${tokens.sign(userB)}`);
    assert.deepEqual((await b.json() as { flashcardId: number }[]).map((row) => row.flashcardId), [2]);
  });
  it('answer/summary use JWT identity despite query/header overrides', async () => {
    await dto(await request(`/answer?userId=${userB}`, 'POST', { flashcardId: 1, selectedFlashcardId: 1 },
      undefined, { 'X-User-Id': String(userB) }));
    const a = await (await request(`/summary?userId=${userB}`)).json() as QuizSummaryDto; assert.equal(a.totalAnswers, 1);
    const b = await (await request('/summary', 'GET', undefined, `Bearer ${tokens.sign(userB)}`)).json() as QuizSummaryDto;
    assert.equal(b.totalAnswers, 0); assert.ok(fake.calls.filter((call) => call.userId !== undefined).every((call) => call.userId === userA || call.userId === userB));
  });
  it('history defaults to limit 20 and honors custom limit and stable ordering', async () => {
    for (let n = 0; n < 4; n++) await dto(await answer());
    fake.history.forEach((row) => { row.answered_at = new Date('2026-10-05T01:00:00.000Z'); });
    const defaults = await request('/history'); assert.equal(defaults.status, 200);
    assert.deepEqual((await defaults.json() as { id: number }[]).map((row) => row.id), [4, 3, 2, 1]);
    assert.ok(fake.calls.some((call) => call.operation === 'history' && call.limit === 20));
    const limited = await request('/history?limit=2'); assert.equal(limited.status, 200);
    assert.deepEqual((await limited.json() as { id: number }[]).map((row) => row.id), [4, 3]);
  });
  it('history accepts limits 1 and 100 but rejects invalid limits', async () => {
    for (const limit of ['1', '100']) assert.equal((await request(`/history?limit=${limit}`)).status, 200);
    for (const limit of ['0', '-1', '101', '1.5', 'abc', '01', '1e2']) {
      await error(await request(`/history?limit=${limit}`), 400, 'VALIDATION_ERROR');
    }
  });
  it('history maps camelCase and nullable selected options', async () => {
    await dto(await answer(1, 2)); fake.history[0]!.selected_flashcard_id = null; fake.history[0]!.selected_answer = null;
    const response = await request('/history'); assert.equal(response.status, 200);
    const rows = await response.json() as Record<string, unknown>[]; const row = rows[0]!;
    assert.deepEqual(Object.keys(row).sort(), ['id', 'flashcardId', 'question', 'selectedFlashcardId', 'selectedAnswer',
      'correctAnswer', 'correct', 'questionType', 'answeredAt'].sort());
    assert.equal(row.selectedFlashcardId, null); assert.equal(row.selectedAnswer, null);
    assert.equal(row.correctAnswer, 'meaning1'); assert.equal(row.correct, false);
  });
  it('empty history returns an array', async () => { assert.deepEqual(await (await request('/history')).json(), []); });
  it('summary returns correct counts and rounded accuracy', async () => {
    await dto(await answer()); await dto(await answer(1, 2)); await dto(await answer(1, 2));
    assert.deepEqual(await (await request('/summary')).json(), { totalAnswers: 3, correctAnswers: 1, wrongAnswers: 2, accuracy: 33.33 });
  });
  it('empty summary has null accuracy', async () => {
    assert.deepEqual(await (await request('/summary')).json(), { totalAnswers: 0, correctAnswers: 0, wrongAnswers: 0, accuracy: null });
  });
  it('all-wrong summary has zero accuracy', async () => {
    await dto(await answer(1, 2));
    assert.deepEqual(await (await request('/summary')).json(), { totalAnswers: 1, correctAnswers: 0, wrongAnswers: 1, accuracy: 0 });
  });
  it('all-correct summary has 100 percent accuracy and ignores other users', async () => {
    await dto(await answer()); await dto(await answer(1, 2, `Bearer ${tokens.sign(userB)}`));
    assert.deepEqual(await (await request('/summary')).json(), { totalAnswers: 1, correctAnswers: 1, wrongAnswers: 0, accuracy: 100 });
  });
  it('answer/history/summary DTOs exclude owner, DB fields and synthetic secrets', async () => {
    const first = await dto(await answer());
    Object.assign(fake.history[0]!, { password_hash: 'private-test-hash', DB_PASSWORD: 'private-test-password', JWT_SECRET: secret });
    Object.assign(fake.progress.get(`${userA}:1`)!, { user_id: userA, password_hash: 'private-test-hash' });
    assert.deepEqual(Object.keys(first).sort(), ['resultId', 'flashcardId', 'selectedFlashcardId', 'correct', 'correctAnswer',
      'selectedAnswer', 'answeredAt', 'progress'].sort());
    for (const response of [await answer(), await request('/history'), await request('/summary')]) {
      assert.equal(response.status, 200); const text = await response.text();
      for (const hidden of ['user_id', 'userId', 'password_hash', 'private-test-hash', 'private-test-password', secret]) assert.ok(!text.includes(hidden));
    }
  });
  it('unexpected errors on each endpoint use safe generic 500 responses', async () => {
    fake.fail(new Error(`private SQL connection string DB_PASSWORD=private-test-password JWT_SECRET=${secret} password_hash`));
    for (const [path, method, body] of endpoints) {
      const response = await request(path, method, body); assert.equal(response.status, 500);
      assert.deepEqual(await response.json(), { error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' } });
    }
  });
  it('rejects malformed JSON and oversized answer bodies without touching repository', async () => {
    await error(await fetch(base + '/answer', { method: 'POST', headers: { Authorization: `Bearer ${tokens.sign(userA)}`,
      'Content-Type': 'application/json' }, body: '{"flashcardId":' }), 400, 'INVALID_JSON');
    await error(await request('/answer', 'POST', { flashcardId: 1, selectedFlashcardId: 1, extra: 'x'.repeat(17000) }), 413, 'PAYLOAD_TOO_LARGE');
    assert.equal(fake.calls.length, 0);
  });
});
