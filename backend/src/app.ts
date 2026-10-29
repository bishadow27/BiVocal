import express from 'express';
import { createRoutes, type Repositories } from './routes.js';
import { AppError } from './shared/errors/app-error.js';
import { errorHandler } from './shared/middleware/error-handler.js';
import type { AuthModule } from './modules/auth/auth.service.js';
import { resetPasswordPage } from './modules/auth/reset-page.js';

export function createApp(repositories: Repositories, auth?: AuthModule): express.Express {
  const app = express();
  app.disable('x-powered-by');
  app.get('/reset-password', resetPasswordPage);
  app.use('/api/v1', createRoutes(repositories, auth));
  app.use((_request, _response) => {
    throw new AppError(404, 'ROUTE_NOT_FOUND', 'Route not found');
  });
  app.use(errorHandler);
  return app;
}
