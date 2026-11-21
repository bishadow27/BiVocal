import { Router } from 'express';
import type {QuizSessionRepository} from './modules/quiz/quiz-session.repository.js';
import {createQuizSessionRouter} from './modules/quiz/quiz-session.route.js';
import { createTopicService } from './modules/topics/topic.service.js';
import type { TopicRepository } from './modules/topics/topic.repository.js';
import { createTopicRouter } from './modules/topics/topic.route.js';
import { createFlashcardService } from './modules/flashcards/flashcard.service.js';
import type { FlashcardRepository } from './modules/flashcards/flashcard.repository.js';
import { createFlashcardRouter } from './modules/flashcards/flashcard.route.js';
import type { HealthRepository } from './modules/health/health.repository.js';
import { createHealthService } from './modules/health/health.service.js';
import { createHealthRouter } from './modules/health/health.route.js';
import { createAuthRouter } from './modules/auth/auth.route.js';
import type { AuthModule } from './modules/auth/auth.service.js';
import type { ProgressRepository } from './modules/progress/progress.repository.js';
import { createProgressService } from './modules/progress/progress.service.js';
import { createProgressRouter } from './modules/progress/progress.route.js';
import type { QuizRepository } from './modules/quiz/quiz.repository.js';
import { createQuizService } from './modules/quiz/quiz.service.js';
import { createQuizRouter } from './modules/quiz/quiz.route.js';
import type { GamificationRepository } from './modules/gamification/gamification.repository.js';
import { createGamificationService } from './modules/gamification/gamification.service.js';
import { createGamificationRouter } from './modules/gamification/gamification.route.js';
import type { AdminRepository } from './modules/admin/admin.repository.js';
import { createAdminService } from './modules/admin/admin.service.js';
import { createAdminRouter } from './modules/admin/admin.route.js';
import type { VocabularyReviewRepository } from './modules/vocabulary-review/vocabulary-review.repository.js';
import { createVocabularyReviewService } from './modules/vocabulary-review/vocabulary-review.service.js';
import { createVocabularyReviewRouter } from './modules/vocabulary-review/vocabulary-review.route.js';
import type { LearningRepository } from './modules/learning/learning.repository.js';
import { createLearningService } from './modules/learning/learning.service.js';
import { createLearningRouter } from './modules/learning/learning.route.js';

export interface Repositories {
  topics: TopicRepository;
  flashcards: FlashcardRepository;
  database: HealthRepository;
  progress?: ProgressRepository;
  quiz?: QuizRepository;
  quizSessions?: QuizSessionRepository;
  quizSessionsReady?: () => Promise<void>;
  gamification?: GamificationRepository;
  admin?: AdminRepository;
  vocabularyReview?: VocabularyReviewRepository;
  learning?: LearningRepository;
}

export function createRoutes(repositories: Repositories, auth?: AuthModule): Router {
  const router = Router();
  if (repositories.admin) {
    if (!auth) throw new Error('Admin routes require authentication');
    const adminService=createAdminService(repositories.admin);
    if(repositories.vocabularyReview) router.use('/admin/vocabulary-review',
      createVocabularyReviewRouter(createVocabularyReviewService(repositories.vocabularyReview),adminService,auth.tokens));
    router.use('/admin', createAdminRouter(adminService, auth.tokens));
  }
  const topics = createTopicService(repositories.topics);
  const flashcards = createFlashcardService(repositories.flashcards, topics);
  router.use('/health', createHealthRouter(createHealthService(repositories.database)));
  router.use('/topics', createTopicRouter(topics, flashcards));
  router.use('/flashcards', createFlashcardRouter(flashcards));
  if (auth) router.use('/auth', createAuthRouter(auth));
  if (repositories.progress) {
    if (!auth) throw new Error('Progress routes require authentication');
    router.use('/progress', createProgressRouter(createProgressService(repositories.progress), auth.tokens));
  }
  if (repositories.quizSessions) {
    if (!auth) throw new Error('Quiz session routes require authentication');
    router.use('/quiz', createQuizSessionRouter(repositories.quizSessions, auth.tokens, repositories.quizSessionsReady));
  }
  if (repositories.quiz) {
    if (!auth) throw new Error('Quiz routes require authentication');
    router.use('/quiz', createQuizRouter(createQuizService(repositories.quiz), auth.tokens));
  }
  if (repositories.gamification) {
    if (!auth) throw new Error('Gamification routes require authentication');
    router.use('/gamification', createGamificationRouter(createGamificationService(repositories.gamification), auth.tokens));
  }
  if (repositories.learning) {
    if (!auth) throw new Error('Learning routes require authentication');
    router.use('/learning', createLearningRouter(createLearningService(repositories.learning), auth.tokens));
  }
  return router;
}
