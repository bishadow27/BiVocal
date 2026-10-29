import { Router,json } from 'express';
import type { ErrorRequestHandler } from 'express';
import type { TokenService } from '../auth/token.service.js';
import type { AdminService } from '../admin/admin.service.js';
import { authenticate } from '../../shared/middleware/authenticate.js';
import { requireAdmin } from '../admin/require-admin.js';
import { AppError } from '../../shared/errors/app-error.js';
import type { VocabularyReviewService } from './vocabulary-review.service.js';
import { createVocabularyReviewController } from './vocabulary-review.controller.js';

const mapReviewDatabaseError: ErrorRequestHandler = (error: unknown, _request, _response, next) => {
  if (
    typeof error === 'object' &&
    error !== null &&
    'code' in error &&
    error.code === '42P01'
  ) {
    next(
      new AppError(
        503,
        'REVIEW_SCHEMA_UNAVAILABLE',
        'Vocabulary review database schema is not available',
      ),
    );
    return;
  }
  next(error);
};

export function createVocabularyReviewRouter(service:VocabularyReviewService,admin:AdminService,tokens:TokenService):Router{
  const r=Router(),c=createVocabularyReviewController(service);r.use(authenticate(tokens),requireAdmin(admin),json({limit:'64kb'}));
  r.get('/stats',c.stats!);r.get('/filters',c.filters!);r.get('/candidates',c.list!);r.get('/candidates/:candidateId',c.one!);
  r.put('/candidates/:candidateId',c.edit!);r.patch('/candidates/:candidateId/status',c.status!);
  r.post('/bulk-status',c.bulk!);r.post('/publish/preview',c.preview!);r.post('/publish/:batchId/confirm',c.publish!);
  r.get('/candidates/:candidateId/audit',c.audit!);r.use(mapReviewDatabaseError);return r;
}

