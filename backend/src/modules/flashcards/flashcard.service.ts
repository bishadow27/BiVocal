import { AppError } from '../../shared/errors/app-error.js';
import type { Flashcard } from '../../shared/types/api.js';
import type { TopicService } from '../topics/topic.service.js';
import type { FlashcardRepository, FlashcardRow } from './flashcard.repository.js';

function toFlashcard(row: FlashcardRow): Flashcard {
  return {
    id: row.id, english: row.english, vietnamese: row.vietnamese,
    pronunciation: row.pronunciation, partOfSpeech: row.part_of_speech,
    example: row.example, exampleVi: row.example_vi, level: row.level,
    contentStatus: row.content_status ?? 'SEMANTIC_VERIFIED',
  };
}

export function createFlashcardService(repository: FlashcardRepository, topics: TopicService) {
  return {
    async listByTopic(code: string): Promise<Flashcard[]> {
      // Distinguish an unknown topic (404) from an existing empty topic ([]).
      await topics.requireByCode(code);
      return (await repository.findByTopicCode(code)).map(toFlashcard);
    },
    async search(query: { search: string | null; topicCode: string | null; level: string | null;
      page: number; limit: number }) {
      const result = await repository.search(query);
      return { items: result.rows.map(toFlashcard), page: query.page, limit: query.limit,
        total: result.total, totalPages: Math.ceil(result.total / query.limit) };
    },
    async detail(id: number): Promise<Flashcard> {
      const row = await repository.findById(id);
      if (!row) throw new AppError(404, 'FLASHCARD_NOT_FOUND', 'Flashcard not found');
      return toFlashcard(row);
    },
  };
}

export type FlashcardService = ReturnType<typeof createFlashcardService>;
