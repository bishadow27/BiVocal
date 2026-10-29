import type { QueryClient } from '@tanstack/react-query';
export function invalidate(client: QueryClient, keys: string[]) {
  return Promise.all(keys.map((key) => client.invalidateQueries({ queryKey: [key] })));
}
export const contentKeys = [
  'admin-flashcards',
  'admin-topics',
  'admin-dashboard',
  'admin-quiz-stats',
  'admin-user',
  'admin-user-progress',
  'admin-user-quiz-history',
];
