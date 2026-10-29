import { Router } from 'express';
import type { TopicService } from './topic.service.js';
import type { FlashcardService } from '../flashcards/flashcard.service.js';
import { createTopicController } from './topic.controller.js';
import { createFlashcardController } from '../flashcards/flashcard.controller.js';

export function createTopicRouter(topics: TopicService, flashcards: FlashcardService): Router {
  const router = Router();
  router.get('/', createTopicController(topics).list);
  router.get('/catalog', createTopicController(topics).catalog);
  router.get('/:code/flashcards', createFlashcardController(flashcards).listByTopic);
  return router;
}
