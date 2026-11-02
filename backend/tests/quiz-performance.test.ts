import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { AppError } from '../src/shared/errors/app-error.js';
import { createQuizService } from '../src/modules/quiz/quiz.service.js';
import type { QuizRepository } from '../src/modules/quiz/quiz.repository.js';
import type { QuizCard, QuizQuestionDto } from '../src/modules/quiz/quiz.types.js';

// Isolated service fixtures only. No PostgreSQL, HTTP writes, or restored application data.
function repository(cards: QuizCard[]): QuizRepository {
  return {
    async topicExists(code) { return code === 'FOOD'; },
    async activeCards() { return cards; },
    async answer() { throw new Error('Question generation must not write an answer'); },
    async history() { throw new Error('Unused history fixture'); },
    async summary() { throw new Error('Unused summary fixture'); },
  };
}
function cards(count: number): QuizCard[] {
  return Array.from({ length: count }, (_, index) => ({
    id: index + 1, topic_id: 1, english: `word ${index + 1}`, vietnamese: `nghĩa ${index + 1}`,
    part_of_speech: 'noun', level: 'A1',
  }));
}
const key = (text: string) => text.normalize('NFKC').trim().toLocaleLowerCase('vi')
  .replace(/[“”‘’]/g, "'").replace(/\s+/g, ' ');
const meaning = (text: string) => key(text).replace(/[.!?]+$/g, '');
function assertSafe(question: QuizQuestionDto, pool: QuizCard[]) {
  const original = pool.find(card => card.id === question.flashcardId)!;
  assert.equal(question.prompt, original.english);
  assert.equal(question.options.length, 4);
  assert.equal(new Set(question.options.map(option => option.flashcardId)).size, 4);
  assert.equal(question.options.filter(option => option.flashcardId === original.id).length, 1);
  for (const option of question.options) {
    const card = pool.find(candidate => candidate.id === option.flashcardId)!;
    assert.equal(card.topic_id, original.topic_id);
    assert.equal(option.text, card.vietnamese);
  }
  for (let left = 0; left < 4; left++) for (let right = left + 1; right < 4; right++) {
    const a = meaning(question.options[left]!.text), b = meaning(question.options[right]!.text);
    assert.notEqual(a, b);
    const parts = (value: string) => value.replace(/\([^)]*\)/g, ' ').split(/[,;/|]+/)
      .map(part => part.trim()).filter(Boolean);
    const rightParts = new Set(parts(b));
    assert.ok(!parts(a).some(part => rightParts.has(part)), 'All option pairs must have disjoint meaning components');
  }
}

describe('Quiz normalization work and semantic compatibility (in-memory, no database)', () => {
  it('bounds normalization work by input size rather than repeated option comparisons', async t => {
    const pool = cards(120);
    const service = createQuizService(repository(pool));
    const normalize = String.prototype.normalize;
    let normalizations = 0;
    let questions: QuizQuestionDto[];
    try {
      String.prototype.normalize = function (this: string, form?: string) {
        normalizations++;
        return normalize.call(this, form);
      };
      questions = await service.questions('FOOD', 10);
    } finally {
      String.prototype.normalize = normalize;
    }
    t.diagnostic(`${normalizations} normalization calls for ${pool.length} input cards`);
    assert.ok(normalizations <= pool.length * 12,
      `Normalization work must scale with input cards: ${normalizations} calls for ${pool.length} cards`);
    assert.equal(questions.length, 10);
    questions.forEach(question => assertSafe(question, pool));
  });

  it('retains Unicode ambiguity and punctuation/parenthetic/component safety rules', async () => {
    const pool = cards(10);
    pool[0]!.english = 'ＣＡＦÉ'; pool[1]!.english = 'cafe\u0301';
    pool[0]!.vietnamese = 'đồ uống (nóng), trà.'; pool[1]!.vietnamese = 'TRÀ';
    pool[2]!.vietnamese = '“sữa”'; pool[3]!.vietnamese = "'sữa'";
    const before = structuredClone(pool);
    const result = await createQuizService(repository(pool)).questions('FOOD', 8);
    assert.equal(result.length, 8);
    assert.ok(result.every(question => question.flashcardId !== 1 && question.flashcardId !== 2));
    result.forEach(question => assertSafe(question, pool));
    assert.deepEqual(pool, before, 'Preparing metadata must not mutate repository cards');
  });

  it('preserves exact shortage counts and global prompt deduplication across topics', async () => {
    const pool = cards(8);
    pool.slice(4).forEach(card => { card.topic_id = 2; });
    pool[2]!.english = 'shared'; pool[6]!.english = ' SHARED ';
    const service = createQuizService(repository(pool));
    const result = await service.questions(undefined, 7);
    assert.equal(new Set(result.map(question => key(question.prompt))).size, 7);
    result.forEach(question => assertSafe(question, pool));
    await assert.rejects(service.questions(undefined, 8), error => {
      assert.ok(error instanceof AppError);
      assert.equal(error.status, 400);
      assert.equal(error.code, 'QUIZ_NOT_ENOUGH_FLASHCARDS');
      assert.equal(error.message, 'Only 7 safe quiz question(s) are available; 8 requested');
      return true;
    });
  });

  it('uses current card meanings and POS/CEFR on every request without a stale shared cache', async () => {
    const pool = cards(8);
    const service = createQuizService(repository(pool));
    await service.questions('FOOD', 8);
    pool.forEach((card, index) => {
      card.vietnamese = `mới ${card.id}`;
      card.part_of_speech = index < 4 ? 'ＶＥＲＢ' : 'noun';
      card.level = index < 4 ? ' b2 ' : 'A1';
    });
    const result = await service.questions('FOOD', 8);
    for (const question of result) {
      assertSafe(question, pool);
      const original = pool.find(card => card.id === question.flashcardId)!;
      for (const option of question.options) {
        const card = pool.find(candidate => candidate.id === option.flashcardId)!;
        assert.ok(option.text.startsWith('mới '));
        assert.equal(key(card.part_of_speech!), key(original.part_of_speech!));
        assert.equal(key(card.level!), key(original.level!));
      }
    }
  });
});
