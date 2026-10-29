import { QueryClient } from '@tanstack/react-query';
import { ApiError } from '../api/errors';
export const createQueryClient = () =>
  new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30000,
        retry: (attempt, error) =>
          !(error instanceof ApiError && error.status >= 400 && error.status < 500) && attempt < 1,
      },
      mutations: { retry: false },
    },
  });
