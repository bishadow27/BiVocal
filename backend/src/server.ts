import 'dotenv/config';
import { createApp } from './app.js';
import { readEnv } from './config/env.js';
import { createPool } from './config/database.js';
import { createTopicRepository } from './modules/topics/topic.repository.js';
import { createFlashcardRepository } from './modules/flashcards/flashcard.repository.js';
import { createHealthRepository } from './modules/health/health.repository.js';
import { createAuthRepository } from './modules/auth/auth.repository.js';
import { createAuthService } from './modules/auth/auth.service.js';
import { createPasswordService } from './modules/auth/password.service.js';
import { createTokenService } from './modules/auth/token.service.js';
import { createProgressRepository } from './modules/progress/progress.repository.js';
import { createQuizRepository } from './modules/quiz/quiz.repository.js';
import { createGamificationRepository } from './modules/gamification/gamification.repository.js';
import { createAdminRepository } from './modules/admin/admin.repository.js';
import { createVocabularyReviewRepository } from './modules/vocabulary-review/vocabulary-review.repository.js';
import { createGoogleTokenVerifier } from './modules/auth/google-token.service.js';
import { createEmailSender } from './modules/auth/email.service.js';
import { createLearningRepository } from './modules/learning/learning.repository.js';

async function main(): Promise<void> {
  let env;
  try {
    env = readEnv(process.env);
  } catch (error: unknown) {
    console.error(error instanceof Error ? error.message : 'Invalid environment configuration');
    process.exitCode = 1;
    return;
  }
  const pool = createPool(env);
  const database = createHealthRepository(pool);
  try {
    await database.ping();
  } catch {
    console.error('PostgreSQL connection failed. Check DB_* configuration and server availability.');
    await pool.end();
    process.exitCode = 1;
    return;
  }
  const authRepository = createAuthRepository(pool);
  const tokens = createTokenService(env.JWT_SECRET, env.JWT_EXPIRES_IN,
    async (userId, authVersion) => await authRepository.findAuthVersion?.(userId) === authVersion);
  const authService = createAuthService(authRepository, createPasswordService(), tokens, {
    googleVerifier: createGoogleTokenVerifier(env.GOOGLE_WEB_CLIENT_ID),
    emailSender: createEmailSender(env), ...(env.PUBLIC_RESET_URL ? { publicResetUrl: env.PUBLIC_RESET_URL } : {}),
  });
  const app = createApp({
    topics: createTopicRepository(pool), flashcards: createFlashcardRepository(pool), database,
    progress: createProgressRepository(pool),
    quiz: createQuizRepository(pool),
    gamification: createGamificationRepository(pool),
    admin: createAdminRepository(pool),
    vocabularyReview: createVocabularyReviewRepository(pool),
    learning: createLearningRepository(pool),
  }, { tokens, service: authService });
  const server = app.listen(env.PORT, env.HOST, () => {
    console.log(`Backend listening on ${env.HOST}:${env.PORT}; PostgreSQL connected`);
  });
  server.on('error', () => {
    console.error('HTTP server failed to listen');
    void pool.end().finally(() => { process.exitCode = 1; });
  });
  let stopping = false;
  const shutdown = (): void => {
    if (stopping) return;
    stopping = true;
    console.log('Shutting down backend');
    const timeout = setTimeout(() => process.exit(1), 10000);
    timeout.unref();
    server.close(() => {
      void pool.end().then(() => {
        clearTimeout(timeout);
      }).catch(() => {
        console.error('Database pool shutdown failed');
        process.exitCode = 1;
      });
    });
  };
  process.once('SIGINT', shutdown);
  process.once('SIGTERM', shutdown);
}

void main().catch(() => {
  console.error('Backend startup failed');
  process.exitCode = 1;
});
