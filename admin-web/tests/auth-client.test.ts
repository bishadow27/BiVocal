import { describe, it, expect, vi } from 'vitest';
import { AuthSession, SESSION_KEY, type AuthApi } from '../src/features/auth/session';
import { ApiError, errorMessage } from '../src/api/errors';
import { createApiClient, queryString } from '../src/api/client';
import type { AdminMe } from '../src/types/api';
const admin: AdminMe = {
  id: 77,
  username: 'fixture_admin',
  email: 'fixture@example.com',
  fullName: null,
  avatarUrl: null,
  role: 'ADMIN',
};
const loginResponse = { user: admin, accessToken: 'synthetic-test-token' };
function fixture(overrides: Partial<AuthApi> = {}, token: string | null = null) {
  const values = new Map<string, string>();
  if (token) values.set(SESSION_KEY, token);
  const storage = {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => {
      values.set(key, value);
    },
    removeItem: (key: string) => {
      values.delete(key);
    },
  };
  const api: AuthApi = {
    login: vi.fn(async () => loginResponse),
    me: vi.fn(async () => admin),
    ...overrides,
  };
  return { store: new AuthSession(api, storage), api, values };
}
describe('Verified session lifecycle (synthetic API fixtures only)', () => {
  it('accepts login only after ADMIN confirmation and persists only token', async () => {
    const f = fixture();
    expect(await f.store.login({ email: 'fixture@example.com', password: 'synthetic' })).toBe(true);
    expect(f.api.me).toHaveBeenCalledWith('synthetic-test-token', expect.any(AbortSignal));
    expect(f.store.getSnapshot().admin).toEqual(admin);
    expect([...f.values.keys()]).toEqual([SESSION_KEY]);
  });
  it('rejects ADMIN_FORBIDDEN and removes session', async () => {
    const f = fixture({
      me: async () => {
        throw new ApiError(403, 'ADMIN_FORBIDDEN', 'private');
      },
    });
    expect(await f.store.login({ email: 'fixture@example.com', password: 'synthetic' })).toBe(
      false,
    );
    expect(f.store.getSnapshot().accessToken).toBeNull();
    expect(f.store.getSnapshot().notice).toContain('không có quyền');
    expect(f.values.size).toBe(0);
  });
  it('does not accept client role without admin/me and rejects non-ADMIN response', async () => {
    const f = fixture({ me: async () => ({ ...admin, role: 'USER' }) as unknown as AdminMe });
    expect(await f.store.login({ email: 'fixture@example.com', password: 'synthetic' })).toBe(
      false,
    );
    expect(f.store.getSnapshot().admin).toBeNull();
  });
  it('refresh verifies stored token against server before authenticating', async () => {
    const f = fixture({}, 'stored-synthetic');
    expect(f.store.getSnapshot().admin).toBeNull();
    expect(f.store.getSnapshot().isLoading).toBe(true);
    await f.store.restore();
    expect(f.api.me).toHaveBeenCalledWith('stored-synthetic', expect.any(AbortSignal));
    expect(f.store.getSnapshot().admin).toEqual(admin);
  });
  for (const [status, code] of [
    [0, 'NETWORK_ERROR'],
    [500, 'INTERNAL_SERVER_ERROR'],
    [503, 'HTTP_ERROR'],
  ] as const)
    it(`refresh ${code}/${status} retains the stored session and can retry`, async () => {
      let unavailable = true;
      const f = fixture(
        {
          me: async () => {
            if (unavailable) throw new ApiError(status, code, 'temporary failure');
            return admin;
          },
        },
        'stored-synthetic',
      );
      const ended = vi.fn();
      f.store.onEnd(ended);
      await f.store.restore();
      expect(f.values.get(SESSION_KEY)).toBe('stored-synthetic');
      expect(f.store.getSnapshot().accessToken).toBe('stored-synthetic');
      expect(f.store.getSnapshot().admin).toBeNull();
      expect(f.store.getSnapshot().isLoading).toBe(false);
      expect(f.store.getSnapshot().notice).toBeTruthy();
      expect(ended).not.toHaveBeenCalled();
      unavailable = false;
      await f.store.restore();
      expect(f.store.getSnapshot().admin).toEqual(admin);
      expect(f.store.getSnapshot().notice).toBeNull();
      expect(f.api.login).not.toHaveBeenCalled();
    });
  for (const status of [401, 403])
    it(`refresh ${status} clears token and cache notification`, async () => {
      const f = fixture(
        {
          me: async () => {
            throw new ApiError(
              status,
              status === 403 ? 'ADMIN_FORBIDDEN' : 'AUTH_TOKEN_INVALID',
              'private',
            );
          },
        },
        'old-token',
      );
      const listener = vi.fn();
      f.store.onEnd(listener);
      await f.store.restore();
      expect(f.values.size).toBe(0);
      expect(f.store.getSnapshot().isLoading).toBe(false);
      expect(listener).toHaveBeenCalledOnce();
    });
  it('temporary recheck keeps an already confirmed ADMIN session and cached UI', async () => {
    let unavailable = false;
    const f = fixture({
      me: async () => {
        if (unavailable) throw new ApiError(503, 'HTTP_ERROR', 'temporary failure');
        return admin;
      },
    });
    await f.store.login({ email: 'fixture@example.com', password: 'synthetic' });
    const ended = vi.fn();
    f.store.onEnd(ended);
    unavailable = true;
    await f.store.restore();
    expect(f.store.getSnapshot().admin).toEqual(admin);
    expect(f.values.get(SESSION_KEY)).toBe('synthetic-test-token');
    expect(ended).not.toHaveBeenCalled();
  });
  it('ignores a late unauthorized response from a previous token', async () => {
    const f = fixture();
    await f.store.login({ email: 'fixture@example.com', password: 'synthetic' });
    f.store.reject('old-token', new ApiError(401, 'AUTH_TOKEN_INVALID', 'private'));
    expect(f.store.getSnapshot().admin).toEqual(admin);
  });
  it('a late failed restore cannot clear a newer login session', async () => {
    let rejectRestore: ((error: ApiError) => void) | undefined;
    let calls = 0;
    const f = fixture(
      {
        me: () => {
          if (++calls === 1)
            return new Promise<AdminMe>((_resolve, reject) => {
              rejectRestore = reject;
            });
          return Promise.resolve(admin);
        },
      },
      'previous-session',
    );
    const pending = f.store.restore();
    expect(await f.store.login({ email: 'fixture@example.com', password: 'synthetic' })).toBe(true);
    rejectRestore!(new ApiError(401, 'AUTH_TOKEN_INVALID', 'private'));
    await pending;
    expect(f.values.get(SESSION_KEY)).toBe('synthetic-test-token');
    expect(f.store.getSnapshot().admin).toEqual(admin);
  });
  it('a generic forbidden restore retains the token without confirming ADMIN access', async () => {
    const f = fixture(
      {
        me: async () => {
          throw new ApiError(403, 'OTHER_FORBIDDEN', 'private');
        },
      },
      'stored-synthetic',
    );
    await f.store.restore();
    expect(f.values.get(SESSION_KEY)).toBe('stored-synthetic');
    expect(f.store.getSnapshot().admin).toBeNull();
    expect(f.store.getSnapshot().isLoading).toBe(false);
  });
  it('logout prevents a pending login result from restoring a ended session', async () => {
    let resolve: ((value: typeof loginResponse) => void) | undefined;
    const f = fixture({
      login: () =>
        new Promise((r) => {
          resolve = r;
        }),
    });
    const pending = f.store.login({ email: 'fixture@example.com', password: 'synthetic' });
    f.store.logout();
    resolve!(loginResponse);
    expect(await pending).toBe(false);
    expect(f.store.getSnapshot().accessToken).toBeNull();
    expect(f.api.me).not.toHaveBeenCalled();
  });
  it('invalid credentials preserve no token and present a friendly error', async () => {
    const f = fixture({
      login: async () => {
        throw new ApiError(401, 'INVALID_CREDENTIALS', 'private');
      },
    });
    await f.store.login({ email: 'fixture@example.com', password: 'synthetic' });
    expect(f.store.getSnapshot().notice).toContain('Email hoặc mật khẩu');
  });
});
describe('Central API client', () => {
  it('explains a missing vocabulary review schema instead of showing a generic failure', () => {
    expect(errorMessage(new ApiError(503, 'REVIEW_SCHEMA_UNAVAILABLE', 'private'))).toContain(
      'schema kiểm duyệt M13.6',
    );
  });
  for (const [status, code] of [
    [403, 'OTHER_FORBIDDEN'],
    [500, 'INTERNAL_SERVER_ERROR'],
    [503, 'HTTP_ERROR'],
  ] as const)
    it(`${status}/${code} does not reject an existing authenticated session`, async () => {
      const reject = vi.fn();
      const client = createApiClient({
        getToken: () => 'synthetic',
        onAuthRejected: reject,
        fetcher: async () =>
          new Response(JSON.stringify({ error: { code, message: 'private' } }), { status }),
      });
      await expect(client('/admin/dashboard')).rejects.toMatchObject({ status, code });
      expect(reject).not.toHaveBeenCalled();
    });
  it('builds canonical filters with blank optional topic/level omitted', () => {
    expect(
      queryString({ page: 2, limit: 20, search: 'sữa', topicCode: '', level: '', status: 'ALL' }),
    ).toBe('?page=2&limit=20&search=s%E1%BB%AFa&status=ALL');
  });
  it('adds Authorization without role header and routes relative API root', async () => {
    const fetcher = vi.fn(async () => new Response(JSON.stringify({ ok: true }), { status: 200 }));
    const client = createApiClient({
      getToken: () => 'synthetic',
      onAuthRejected: vi.fn(),
      fetcher,
    });
    await client('/admin/me');
    expect(fetcher).toHaveBeenCalledWith(
      '/api/v1/admin/me',
      expect.objectContaining({
        headers: { Accept: 'application/json', Authorization: 'Bearer synthetic' },
        cache: 'no-store',
      }),
    );
  });
  it('protected401 notifies session, while public login401 does not invalidate another session', async () => {
    const reject = vi.fn();
    const client = createApiClient({
      getToken: () => 'synthetic',
      onAuthRejected: reject,
      fetcher: async () =>
        new Response(
          JSON.stringify({ error: { code: 'AUTH_TOKEN_INVALID', message: 'private' } }),
          { status: 401 },
        ),
    });
    await expect(client('/admin/me')).rejects.toBeInstanceOf(ApiError);
    expect(reject).toHaveBeenCalledOnce();
    await expect(
      client('/auth/login', { public: true, method: 'POST', body: {} }),
    ).rejects.toBeInstanceOf(ApiError);
    expect(reject).toHaveBeenCalledOnce();
  });
  it('server error never exposes backend message/SQL through Error object', async () => {
    const client = createApiClient({
      getToken: () => null,
      onAuthRejected: vi.fn(),
      fetcher: async () =>
        new Response(
          JSON.stringify({
            error: { code: 'INTERNAL_SERVER_ERROR', message: 'private SQL password_hash' },
          }),
          { status: 500 },
        ),
    });
    await expect(client('/admin/dashboard')).rejects.toThrow('API request failed');
  });
  it('network failure is surfaced instead of fake data', async () => {
    const client = createApiClient({
      getToken: () => null,
      onAuthRejected: vi.fn(),
      fetcher: async () => {
        throw new Error('offline');
      },
    });
    await expect(client('/admin/dashboard')).rejects.toMatchObject({ code: 'NETWORK_ERROR' });
  });
});
