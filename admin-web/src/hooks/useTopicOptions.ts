import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/admin.api';
import type { AdminTopic } from '../types/api';

export function useTopicOptions() {
  return useQuery({
    queryKey: ['admin-topics', 'options'],
    queryFn: async ({ signal }) => {
      const first = await adminApi.topics({ search: '', page: 1, limit: 100 }, signal);
      const topics: AdminTopic[] = [...first.items];
      for (let page = 2; page <= first.totalPages; page++)
        topics.push(...(await adminApi.topics({ search: '', page, limit: 100 }, signal)).items);
      return topics;
    },
  });
}
