import { AppError } from '../../shared/errors/app-error.js';
import type { Topic } from '../../shared/types/api.js';
import type { TopicRepository, TopicRow } from './topic.repository.js';

function toTopic(row: TopicRow): Topic {
  return { id: row.id, code: row.code, name: row.name,
    description: row.description, imageUrl: row.image_url };
}

export function createTopicService(repository: TopicRepository) {
  return {
    async list(): Promise<Topic[]> {
      return (await repository.findAll()).map(toTopic);
    },
    async catalog() {
      return (await repository.findCatalog()).map(row => ({ ...toTopic(row),
        flashcardCount: Number(row.flashcard_count), topicGroup: row.topic_group }));
    },
    async requireByCode(code: string): Promise<Topic> {
      const row = await repository.findByCode(code);
      if (!row) throw new AppError(404, 'TOPIC_NOT_FOUND', 'Topic not found');
      return toTopic(row);
    },
  };
}

export type TopicService = ReturnType<typeof createTopicService>;
