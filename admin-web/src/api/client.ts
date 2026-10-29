import { ApiError } from './errors';

export interface ClientOptions {
  baseUrl?: string;
  getToken: () => string | null;
  onAuthRejected: (token: string, error: ApiError) => void;
  fetcher?: typeof fetch;
}
export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH';
  body?: unknown;
  signal?: AbortSignal;
  token?: string | null;
  public?: boolean;
}
export function queryString(params: object): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value));
  }
  return query.size ? `?${query.toString()}` : '';
}
export function createApiClient(options: ClientOptions) {
  const base = (options.baseUrl?.trim() || '/api/v1').replace(/\/$/, '');
  const fetcher = options.fetcher ?? ((...args: Parameters<typeof fetch>) => fetch(...args));
  return async function request<T>(path: string, input: RequestOptions = {}): Promise<T> {
    const token = input.public
      ? null
      : input.token === undefined
        ? options.getToken()
        : input.token;
    const abort = new AbortController();
    const cancel = () => abort.abort(input.signal?.reason);
    input.signal?.addEventListener('abort', cancel, { once: true });
    if (input.signal?.aborted) cancel();
    let timedOut = false;
    const timeout = setTimeout(() => {
      timedOut = true;
      abort.abort();
    }, 15000);
    try {
      const response = await fetcher(base + path, {
        method: input.method ?? 'GET',
        signal: abort.signal,
        cache: 'no-store',
        headers: {
          Accept: 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
          ...(input.body === undefined ? {} : { 'Content-Type': 'application/json' }),
        },
        ...(input.body === undefined ? {} : { body: JSON.stringify(input.body) }),
      });
      const value: unknown = await response.json().catch(() => null);
      if (!response.ok) {
        const record = value && typeof value === 'object' && 'error' in value ? value.error : null;
        const code =
          record &&
          typeof record === 'object' &&
          'code' in record &&
          typeof record.code === 'string'
            ? record.code
            : 'HTTP_ERROR';
        const error = new ApiError(response.status, code, 'API request failed');
        if (
          token &&
          (response.status === 401 || (response.status === 403 && code === 'ADMIN_FORBIDDEN'))
        )
          options.onAuthRejected(token, error);
        throw error;
      }
      if (value === null)
        throw new ApiError(response.status, 'INVALID_RESPONSE', 'Invalid API response');
      return value as T;
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (input.signal?.aborted) throw error;
      throw new ApiError(
        0,
        timedOut ? 'REQUEST_TIMEOUT' : 'NETWORK_ERROR',
        'Network request failed',
      );
    } finally {
      clearTimeout(timeout);
      input.signal?.removeEventListener('abort', cancel);
    }
  };
}
export type ApiClient = ReturnType<typeof createApiClient>;
