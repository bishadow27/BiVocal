import { Router } from 'express';
import { createFlashcardController } from './flashcard.controller.js';
import type { FlashcardService } from './flashcard.service.js';

export function createFlashcardRouter(service: FlashcardService): Router {
  const router = Router();
  router.get('/', createFlashcardController(service).search);
  router.get('/:id', createFlashcardController(service).detail);
  return router;
}
