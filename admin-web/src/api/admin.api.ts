import { request } from './index';
import { queryString } from './client';
import type {
  AdminDashboard,
  AdminTopic,
  AdminFlashcard,
  AdminUser,
  AdminProgress,
  AdminQuizHistory,
  UserDetail,
  QuizStats,
  LeaderboardItem,
  PagedResponse,
  Range,
  Pagination,
  TopicFilters,
  FlashcardFilters,
  UserFilters,
  TopicInput,
  CardInput,
  CreateCardInput,
  Role,
  Account,
  VocabularyReviewCandidate,VocabularyReviewStats,VocabularyReviewFilters,VocabularyReviewOptions,
  VocabularyReviewEdit,VocabularyPublishPreview,ReviewStatus,
} from '../types/api';

export const adminApi = {
  dashboard: (range: Range, signal: AbortSignal) =>
    request<AdminDashboard>('/admin/dashboard' + queryString({ range }), { signal }),
  topics: (filters: TopicFilters, signal: AbortSignal) =>
    request<PagedResponse<AdminTopic>>('/admin/topics' + queryString(filters), { signal }),
  createTopic: (body: TopicInput) => request<AdminTopic>('/admin/topics', { method: 'POST', body }),
  updateTopic: (id: number, body: TopicInput) =>
    request<AdminTopic>(`/admin/topics/${id}`, { method: 'PUT', body }),
  flashcards: (filters: FlashcardFilters, signal: AbortSignal) =>
    request<PagedResponse<AdminFlashcard>>('/admin/flashcards' + queryString(filters), { signal }),
  createCard: (body: CreateCardInput) =>
    request<AdminFlashcard>('/admin/flashcards', { method: 'POST', body }),
  updateCard: (id: number, body: CardInput) =>
    request<AdminFlashcard>(`/admin/flashcards/${id}`, { method: 'PUT', body }),
  cardStatus: (id: number, isActive: boolean) =>
    request<AdminFlashcard>(`/admin/flashcards/${id}/status`, {
      method: 'PATCH',
      body: { isActive },
    }),
  users: (filters: UserFilters, signal: AbortSignal) =>
    request<PagedResponse<AdminUser>>('/admin/users' + queryString(filters), { signal }),
  user: (id: number, signal: AbortSignal) => request<UserDetail>(`/admin/users/${id}`, { signal }),
  progress: (id: number, filters: TopicFilters, signal: AbortSignal) =>
    request<PagedResponse<AdminProgress>>(`/admin/users/${id}/progress` + queryString(filters), {
      signal,
    }),
  history: (id: number, filters: Pagination, signal: AbortSignal) =>
    request<PagedResponse<AdminQuizHistory>>(
      `/admin/users/${id}/quiz-history` + queryString(filters),
      { signal },
    ),
  role: (id: number, role: Role) =>
    request<Account & { role: Role }>(`/admin/users/${id}/role`, {
      method: 'PATCH',
      body: { role },
    }),
  stats: (range: Range, topicCode: string, signal: AbortSignal) =>
    request<QuizStats>('/admin/stats/quiz' + queryString({ range, topicCode }), { signal }),
  leaderboard: (filters: Pagination, signal: AbortSignal) =>
    request<PagedResponse<LeaderboardItem>>('/admin/leaderboard' + queryString(filters), {
      signal,
    }),
  reviewStats:(signal:AbortSignal)=>request<VocabularyReviewStats>('/admin/vocabulary-review/stats',{signal}),
  reviewOptions:(signal:AbortSignal)=>request<VocabularyReviewOptions>('/admin/vocabulary-review/filters',{signal}),
  reviewCandidates:(filters:VocabularyReviewFilters,signal:AbortSignal)=>request<PagedResponse<VocabularyReviewCandidate>>('/admin/vocabulary-review/candidates'+queryString(filters),{signal}),
  updateReviewCandidate:(id:string,body:VocabularyReviewEdit)=>request<VocabularyReviewCandidate>(`/admin/vocabulary-review/candidates/${id}`,{method:'PUT',body}),
  reviewStatus:(id:string,status:Exclude<ReviewStatus,'PUBLISHED'>,rowVersion:number)=>request<VocabularyReviewCandidate>(`/admin/vocabulary-review/candidates/${id}/status`,{method:'PATCH',body:{status,rowVersion}}),
  bulkReviewStatus:(candidateIds:string[],status:Exclude<ReviewStatus,'PUBLISHED'>)=>request<{changed:number;status:string}>('/admin/vocabulary-review/bulk-status',{method:'POST',body:{candidateIds,status}}),
  previewPublish:(candidateIds:string[])=>request<VocabularyPublishPreview>('/admin/vocabulary-review/publish/preview',{method:'POST',body:{candidateIds}}),
  confirmPublish:(batchId:string)=>request<{batchId:string;status:string;inserted:number;skipped:number;blocked:number;completedAt:string|null}>(`/admin/vocabulary-review/publish/${batchId}/confirm`,{method:'POST',body:{confirmation:'PUBLISH_APPROVED_BATCH'}}),
};
