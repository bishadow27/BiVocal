import type { ErrorRequestHandler } from 'express';
import { AppError } from '../errors/app-error.js';

export const errorHandler: ErrorRequestHandler = (error: unknown, _request, response, next) => {
  if (response.headersSent) {
    next(error);
    return;
  }
  if (error instanceof AppError) {
    response.status(error.status).json({ error: { code: error.code, message: error.message } });
    return;
  }
  if (error instanceof URIError) {
    response.status(400).json({
      error: { code: 'INVALID_REQUEST_PATH', message: 'Invalid request path' },
    });
    return;
  }
  if (typeof error === 'object' && error !== null && 'type' in error) {
    if (error.type === 'entity.parse.failed') {
      response.status(400).json({ error: { code: 'INVALID_JSON', message: 'Invalid JSON body' } });
      return;
    }
    if (error.type === 'entity.too.large') {
      response.status(413).json({ error: { code: 'PAYLOAD_TOO_LARGE', message: 'Request body too large' } });
      return;
    }
  }
  // Stable log and response; never expose SQL, stack traces or credentials.
  console.error('Unhandled API error');
  response.status(500).json({
    error: { code: 'INTERNAL_SERVER_ERROR', message: 'Internal server error' },
  });
};
