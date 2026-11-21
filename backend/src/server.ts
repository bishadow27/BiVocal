import 'dotenv/config';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
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
import { createQuizSessionRepository } from './modules/quiz/quiz-session.repository.js';
import { assertQuizSessionSchema } from './modules/quiz/quiz-session.runtime.js';
import { createQuizRepository } from './modules/quiz/quiz.repository.js';
import { createGamificationRepository } from './modules/gamification/gamification.repository.js';
import { createAdminRepository } from './modules/admin/admin.repository.js';
import { createVocabularyReviewRepository } from './modules/vocabulary-review/vocabulary-review.repository.js';
import { createGoogleTokenVerifier } from './modules/auth/google-token.service.js';
import { createEmailSender } from './modules/auth/email.service.js';
import { createLearningRepository } from './modules/learning/learning.repository.js';

const localControlChannel = 'bivoca-local-control';
const controlRequestId = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

/** Process lifecycle only. Does not create a pool or execute a query. */
export function installBackendControl(
  server: import('node:http').Server,
  pool: { end(): Promise<void> },
  options: { shutdownTimeoutMs?: number } = {},
) {
  let closing = false;
  let pending: Promise<void> | undefined;
  let failed = false;
  const requests = new Set<string>();
  const deliveredTerminal = new Set<string>();
  const send = (message: Record<string, string | number>): Promise<boolean> => new Promise(resolve => {
    if (!process.send || !process.connected) { resolve(false); return; }
    try { process.send(message, error => resolve(!error)); }
    catch { resolve(false); }
  });
  const reply = (type: string, requestId: string, code?: string) => send({
    channel: localControlChannel, version: 1, type, requestId, ...(code ? { code } : {}),
  });
  const terminal = async (type: string, code?: string): Promise<void> => {
    await Promise.all([...requests].filter(id => !deliveredTerminal.has(id)).map(id => {
      deliveredTerminal.add(id);
      return reply(type, id, code);
    }));
  };
  const begin = (errorCode?: string): Promise<void> => {
    if (errorCode) { failed = true; process.exitCode = 1; }
    if (pending) return pending;
    closing = true;
    console.log('Shutting down backend');
    const timer = setTimeout(() => {
      failed = true;
      process.exitCode = 1;
      console.error('Graceful shutdown timed out; no force exit was attempted');
      void terminal('shutdown-error', 'SHUTDOWN_TIMEOUT');
    }, options.shutdownTimeoutMs ?? 10000);
    timer.unref();
    pending = Promise.resolve().then(async () => {
      try {
        await new Promise<void>((resolve, reject) => server.close(error => {
          if (error && (error as NodeJS.ErrnoException).code !== 'ERR_SERVER_NOT_RUNNING') reject(error);
          else resolve();
        }));
      } catch {
        failed = true;
        process.exitCode = 1;
        console.error('Graceful HTTP shutdown failed');
      }
      // Even if HTTP close fails, release the pool exactly once.
      try { await pool.end(); }
      catch {
        failed = true;
        process.exitCode = 1;
        console.error('Database pool shutdown failed');
      }
      clearTimeout(timer);
      await terminal(failed ? 'shutdown-error' : 'shutdown-complete', failed ? 'SHUTDOWN_FAILED' : undefined);
      process.removeListener('message', onMessage);
      process.removeListener('SIGINT', onSignal);
      process.removeListener('SIGTERM', onSignal);
      if (process.connected) {
        try { process.disconnect(); } catch { /* Already disconnected by the parent. */ }
      }
    });
    return pending;
  };
  const onSignal = (): void => { void begin(); };
  const onMessage = (value: unknown): void => {
    if (!value || typeof value !== 'object' || Array.isArray(value)) return;
    const message = value as Record<string, unknown>;
    if (message.channel !== localControlChannel || message.version !== 1 || message.type !== 'shutdown'
      || typeof message.requestId !== 'string' || !controlRequestId.test(message.requestId)) return;
    if (!requests.has(message.requestId) && requests.size >= 32) return;
    requests.add(message.requestId);
    void reply('shutdown-accepted', message.requestId);
    void begin();
  };
  if (process.send) process.on('message', onMessage);
  process.once('SIGINT', onSignal);
  process.once('SIGTERM', onSignal);
  server.once('error', () => {
    console.error('HTTP server failed to listen');
    void begin('HTTP_SERVER_ERROR');
  });
  return {
    shutdown: () => begin(),
    notifyReady: (): void => {
      if (closing || !server.listening) return;
      const address = server.address();
      void send({ channel: localControlChannel, version: 1, type: 'backend-ready', pid: process.pid,
        port: address && typeof address === 'object' ? address.port : 0 });
    },
  };
}

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
    quizSessions: createQuizSessionRepository(pool, async (client) => assertQuizSessionSchema(client)),
    quizSessionsReady: () => assertQuizSessionSchema(pool),
    gamification: createGamificationRepository(pool),
    admin: createAdminRepository(pool),
    vocabularyReview: createVocabularyReviewRepository(pool),
    learning: createLearningRepository(pool),
  }, { tokens, service: authService });
  const server = app.listen(env.PORT, env.HOST, () => {
    console.log(`Backend listening on ${env.HOST}:${env.PORT}; PostgreSQL connected`);
    lifecycle.notifyReady();
  });
  const lifecycle = installBackendControl(server, pool);
}

const entrypoint = process.argv[1] ? path.resolve(process.argv[1]) : '';
const moduleFile = fileURLToPath(import.meta.url);
if (entrypoint && (process.platform === 'win32'
  ? entrypoint.toLowerCase() === moduleFile.toLowerCase() : entrypoint === moduleFile)) {
  void main().catch(() => {
    console.error('Backend startup failed');
    process.exitCode = 1;
  });
}
