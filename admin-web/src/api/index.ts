import { createApiClient } from './client';
import { AuthSession } from '../features/auth/session';
import type { AdminMe, LoginResponse } from '../types/api';

let storage: Storage | null = null;
try {
  storage = window.sessionStorage;
} catch {
  /* unsupported storage stays in memory */
}
export const request = createApiClient({
  baseUrl: import.meta.env.VITE_API_BASE_URL ?? '',
  getToken: () => session.getSnapshot().accessToken,
  onAuthRejected: (token, error) => session.reject(token, error),
});
export const session = new AuthSession(
  {
    login: (body, signal) =>
      request<LoginResponse>('/auth/login', { method: 'POST', body, signal, public: true }),
    me: (token, signal) => request<AdminMe>('/admin/me', { token, signal }),
  },
  storage,
);
