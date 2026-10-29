import { beforeEach, describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '../src/features/auth/AuthProvider';
import { ToastProvider } from '../src/components/ui';
import { AppRoutes } from '../src/routes/AppRoutes';
import { session } from '../src/api';
import { ApiError } from '../src/api/errors';
import { AuthSession, SESSION_KEY } from '../src/features/auth/session';
import type { AdminDashboard, AdminMe, AdminTopic, AdminUser, UserDetail } from '../src/types/api';

// These fixtures are ONLY in tests, never imported into the application bundle.
const admin: AdminMe = {
  id: 17,
  username: 'fixture_admin',
  email: 'admin-fixture@example.com',
  fullName: 'Fixture admin',
  avatarUrl: null,
  role: 'ADMIN',
};
const dashboard: AdminDashboard = {
  users: { total: 28 },
  flashcards: { total: 70, active: 61, inactive: 9 },
  quiz: { totalAnswers: 14, correctAnswers: 11, wrongAnswers: 3, accuracy: 78.57 },
  gamification: { totalXp: 700 },
  topUsers: [],
  topTopics: [{ code: 'FOOD', name: 'Food fixture', quizCount: 14 }],
  quizByDay: [],
};
const topic: AdminTopic = {
  id: 9,
  code: 'FOOD',
  name: 'Food fixture',
  description: null,
  imageUrl: null,
  flashcardCount: 70,
};
const user: AdminUser = {
  ...admin,
  createdAt: '2026-10-01T00:00:00Z',
  lastLogin: null,
  totalXp: 250,
  level: 3,
  currentStreak: 2,
  longestStreak: 4,
};
const detail: UserDetail = {
  user,
  progress: {
    trackedCards: 1,
    reviewedCards: 1,
    favoriteCards: 0,
    masteredCards: 0,
    totalReviews: 2,
    correctAnswers: 1,
    wrongAnswers: 1,
    accuracy: 50,
  },
  quiz: { totalAnswers: 14, correctAnswers: 11, wrongAnswers: 3, accuracy: 78.57 },
  gamification: {
    totalXp: 250,
    level: 3,
    xpIntoLevel: 50,
    xpPerLevel: 100,
    currentStreak: 2,
    longestStreak: 4,
    lastActivityDate: '2026-10-05',
  },
};
let forbidden = false,
  expired = false,
  saveFails = false;
let calls: { path: string; query: URLSearchParams; method: string; body: unknown }[] = [];
let targetRole: 'USER' | 'ADMIN' = 'USER';
beforeEach(() => {
  session.logout();
  forbidden = false;
  expired = false;
  saveFails = false;
  calls = [];
  targetRole = 'USER';
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = new URL(String(input), 'http://test.local');
      const path = url.pathname.replace('/api/v1', '');
      const method = init?.method ?? 'GET',
        body = typeof init?.body === 'string' ? (JSON.parse(init.body) as unknown) : null;
      calls.push({ path, query: url.searchParams, method, body });
      const json = (value: unknown, status = 200) =>
        new Response(JSON.stringify(value), {
          status,
          headers: { 'Content-Type': 'application/json' },
        });
      if (path === '/auth/login')
        return json({ user: admin, accessToken: 'synthetic-page-test-token' });
      if (path === '/admin/me')
        return forbidden ? json({ error: { code: 'ADMIN_FORBIDDEN' } }, 403) : json(admin);
      if (expired) return json({ error: { code: 'AUTH_TOKEN_INVALID' } }, 401);
      if (path === '/admin/dashboard') return json(dashboard);
      if (path === '/admin/topics') {
        if (method === 'POST')
          return saveFails
            ? json({ error: { code: 'TOPIC_CODE_EXISTS' } }, 409)
            : json({ ...topic, id: 10 }, 201);
        const page = Number(url.searchParams.get('page') ?? 1),
          limit = Number(url.searchParams.get('limit') ?? 20);
        return json({
          items:
            page === 1
              ? [topic]
              : [{ ...topic, id: 10, code: 'OTHER', name: 'Second page fixture' }],
          page,
          limit,
          total: 25,
          totalPages: Math.ceil(25 / limit),
        });
      }
      if (path === '/admin/flashcards')
        return json({
          items: [],
          page: Number(url.searchParams.get('page')),
          limit: Number(url.searchParams.get('limit')),
          total: 0,
          totalPages: 0,
        });
      if (path === '/admin/users')
        return json({ items: [user], page: 1, limit: 20, total: 1, totalPages: 1 });
      if (path === `/admin/users/${user.id}`) return json(detail);
      if (path === '/admin/users/18')
        return json({
          ...detail,
          user: {
            ...user,
            id: 18,
            username: 'fixture_member',
            fullName: 'Fixture member',
            role: targetRole,
          },
        });
      if (path === '/admin/users/18/role' && method === 'PATCH') {
        targetRole = (body as { role: 'USER' | 'ADMIN' }).role;
        return json({ ...admin, id: 18, role: targetRole });
      }
      if (path === '/admin/stats/quiz')
        return json({
          totalAnswers: 0,
          correctAnswers: 0,
          wrongAnswers: 0,
          accuracy: null,
          byDay: [],
          byTopic: [],
        });
      if (path.endsWith('/progress') || path.endsWith('/quiz-history'))
        return json({ items: [], page: 1, limit: 20, total: 0, totalPages: 0 });
      if (path === '/admin/leaderboard')
        return json({
          items: [
            {
              rank: 4,
              userId: 19,
              username: 'fixture_rank',
              fullName: null,
              avatarUrl: null,
              totalXp: 250,
              level: 3,
              currentStreak: 2,
              longestStreak: 4,
            },
          ],
          page: 1,
          limit: 20,
          total: 1,
          totalPages: 1,
        });
      return json({ error: { code: 'ROUTE_NOT_FOUND' } }, 404);
    }),
  );
});
function mount(path: string, store = session) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <AuthProvider store={store}>
        <ToastProvider>
          <MemoryRouter initialEntries={[path]}>
            <AppRoutes />
          </MemoryRouter>
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>,
  );
  return client;
}
async function authenticate() {
  expect(await session.login({ email: admin.email, password: 'synthetic' })).toBe(true);
}
describe('Admin routes and real UI contracts against isolated fetch fixtures', () => {
  it('unauthenticated protected route redirects to login without dashboard request', async () => {
    mount('/admin/flashcards');
    expect(await screen.findByRole('heading', { name: 'Đăng nhập quản trị' })).toBeVisible();
    expect(calls.some((c) => c.path === '/admin/flashcards')).toBe(false);
  });
  it('login confirms admin/me then navigates to dashboard', async () => {
    mount('/login');
    const actor = userEvent.setup();
    await actor.type(screen.getByLabelText(/Email/), admin.email);
    await actor.type(screen.getByLabelText(/Mật khẩu/), 'synthetic');
    await actor.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByRole('heading', { name: 'Tổng quan' })).toBeVisible();
    expect(calls.slice(0, 2).map((c) => c.path)).toEqual(['/auth/login', '/admin/me']);
  });
  it('ADMIN_FORBIDDEN rejected and login remains with no token', async () => {
    forbidden = true;
    mount('/login');
    const actor = userEvent.setup();
    await actor.type(screen.getByLabelText(/Email/), admin.email);
    await actor.type(screen.getByLabelText(/Mật khẩu/), 'synthetic');
    await actor.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('không có quyền');
    expect(session.getSnapshot().accessToken).toBeNull();
  });
  it('login validates fields before issuing API calls', async () => {
    mount('/login');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(screen.getByText('Nhập email hợp lệ.')).toBeVisible();
    expect(calls.length).toBe(0);
  });
  it('dashboard displays API metrics without fake growth or chart fallback', async () => {
    await authenticate();
    mount('/admin');
    expect(await screen.findByText('61')).toBeVisible();
    expect(screen.getByText('78,57%')).toBeVisible();
    expect(screen.getByText('28')).toBeVisible();
    expect(screen.getByText('Chưa có lượt trả lời Quiz')).toBeVisible();
    expect(screen.queryByText(/Recent Activity|tăng trưởng/i)).not.toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Khoảng thời gian'), '30d');
    await waitFor(() =>
      expect(
        calls.some((c) => c.path === '/admin/dashboard' && c.query.get('range') === '30d'),
      ).toBe(true),
    );
  });
  it('topics paginate on server and reset page after debounced search', async () => {
    await authenticate();
    mount('/admin/topics');
    expect(await screen.findByText('Food fixture')).toBeVisible();
    await userEvent.click(screen.getByRole('button', { name: 'Trang sau' }));
    expect(await screen.findByText('Second page fixture')).toBeVisible();
    await userEvent.type(screen.getByLabelText('Tìm chủ đề'), 'FOOD');
    await waitFor(() =>
      expect(
        calls.some(
          (c) =>
            c.path === '/admin/topics' &&
            c.query.get('search') === 'FOOD' &&
            c.query.get('page') === '1',
        ),
      ).toBe(true),
    );
  });
  it('topic duplicate keeps drawer/form open and maps field error', async () => {
    await authenticate();
    saveFails = true;
    mount('/admin/topics');
    await screen.findByText('Food fixture');
    await userEvent.click(screen.getByRole('button', { name: 'Thêm chủ đề' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText(/Mã chủ đề/), 'FOOD');
    await userEvent.type(within(dialog).getByLabelText(/Tên chủ đề/), 'New topic');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Lưu chủ đề' }));
    await waitFor(() =>
      expect(within(dialog).getByLabelText(/Mã chủ đề/)).toHaveAttribute('aria-invalid', 'true'),
    );
    expect(within(dialog).getByLabelText(/Tên chủ đề/)).toHaveValue('New topic');
    expect(screen.getByRole('dialog')).toBeVisible();
  });
  it('flashcard filters send exact status/topic/level query names', async () => {
    await authenticate();
    mount('/admin/flashcards');
    await screen.findByText('Không có kết quả phù hợp');
    await userEvent.selectOptions(screen.getByLabelText('Chủ đề'), 'FOOD');
    await userEvent.selectOptions(screen.getByLabelText('Trạng thái'), 'INACTIVE');
    await userEvent.selectOptions(screen.getByLabelText('Level'), 'B1');
    await waitFor(() =>
      expect(
        calls.some(
          (c) =>
            c.path === '/admin/flashcards' &&
            c.query.get('topicCode') === 'FOOD' &&
            c.query.get('status') === 'INACTIVE' &&
            c.query.get('level') === 'B1' &&
            c.query.get('page') === '1',
        ),
      ).toBe(true),
    );
  });
  it('user page maps username/email/role/XP and detail blocks own role change', async () => {
    await authenticate();
    mount('/admin/users');
    const link = await screen.findByRole('link', { name: user.username });
    expect(screen.getByText(user.email)).toBeVisible();
    await userEvent.click(link);
    await screen.findByRole('heading', { name: admin.fullName! });
    expect(screen.getByRole('button', { name: 'Đổi vai trò' })).toBeDisabled();
    expect(screen.getByText('Gamification Summary')).toBeVisible();
  });
  it('protected401 clears session, cache and redirects to login', async () => {
    await authenticate();
    expired = true;
    const client = mount('/admin');
    await screen.findByRole('heading', { name: 'Đăng nhập quản trị' });
    expect(session.getSnapshot().accessToken).toBeNull();
    expect(client.getQueryCache().getAll().length).toBe(0);
    expect(screen.getByRole('alert')).toHaveTextContent('hết hạn');
  });
  it('temporary restore failure retains session and retry returns to dashboard without login', async () => {
    let unavailable = true;
    const values = new Map([[SESSION_KEY, 'stored-page-fixture']]);
    const login = vi.fn();
    const restoring = new AuthSession(
      {
        login,
        me: async () => {
          if (unavailable) throw new ApiError(0, 'NETWORK_ERROR', 'temporary outage');
          return admin;
        },
      },
      {
        getItem: (key) => values.get(key) ?? null,
        setItem: (key, value) => {
          values.set(key, value);
        },
        removeItem: (key) => {
          values.delete(key);
        },
      },
    );
    const client = mount('/admin', restoring);
    await screen.findByRole('alert');
    expect(values.get(SESSION_KEY)).toBe('stored-page-fixture');
    const retry = screen.getByRole('button', { name: 'Thử kết nối lại' });
    unavailable = false;
    await userEvent.click(retry);
    await screen.findByRole('heading', { name: 'Tổng quan' });
    expect(login).not.toHaveBeenCalled();
    expect(client.getQueryCache().getAll().length).toBeGreaterThan(0);
  });
  it('logout ends session and navigates to login', async () => {
    await authenticate();
    mount('/admin');
    await screen.findByText('61');
    await userEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }));
    expect(await screen.findByRole('heading', { name: 'Đăng nhập quản trị' })).toBeVisible();
    expect(session.getSnapshot().accessToken).toBeNull();
  });
  it('leaderboard uses rank from API rather than index of current page', async () => {
    await authenticate();
    mount('/admin/leaderboard');
    expect(await screen.findByText('#4')).toBeVisible();
    expect(screen.queryByText('#1')).not.toBeInTheDocument();
  });
  it('role change for another user requires confirmation then refreshes detail', async () => {
    await authenticate();
    mount('/admin/users/18');
    await screen.findByRole('heading', { name: 'Fixture member' });
    await userEvent.click(screen.getByRole('button', { name: 'Đổi vai trò' }));
    expect(calls.some((c) => c.method === 'PATCH')).toBe(false);
    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveTextContent('USER sang ADMIN');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Xác nhận' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(calls.find((c) => c.path === '/admin/users/18/role')).toMatchObject({
      method: 'PATCH',
      body: { role: 'ADMIN' },
    });
    await waitFor(() =>
      expect(calls.filter((c) => c.path === '/admin/users/18').length).toBeGreaterThan(1),
    );
  });
  it('user progress and Quiz history tabs use the readonly endpoints', async () => {
    await authenticate();
    mount('/admin/users/17/progress');
    await screen.findByText('Chỉ xem · Bao gồm cả Flashcards Inactive');
    await screen.findByText('Không có kết quả phù hợp');
    await userEvent.click(screen.getByRole('link', { name: 'Lịch sử Quiz' }));
    await screen.findByRole('columnheader', { name: 'Đáp án đã chọn' });
    expect(calls.some((c) => c.path === '/admin/users/17/progress')).toBe(true);
    expect(calls.some((c) => c.path === '/admin/users/17/quiz-history')).toBe(true);
    expect(calls.some((c) => ['PUT', 'PATCH', 'DELETE'].includes(c.method))).toBe(false);
  });
  it('Quiz stats preserve null accuracy and use exact range/topic filters', async () => {
    await authenticate();
    mount('/admin/quiz-statistics');
    await screen.findByText('—');
    expect(screen.queryByText('0%')).not.toBeInTheDocument();
    await userEvent.selectOptions(screen.getByLabelText('Khoảng thời gian'), 'all');
    await userEvent.selectOptions(screen.getByLabelText('Chủ đề'), 'FOOD');
    await waitFor(() =>
      expect(
        calls.some(
          (c) =>
            c.path === '/admin/stats/quiz' &&
            c.query.get('range') === 'all' &&
            c.query.get('topicCode') === 'FOOD',
        ),
      ).toBe(true),
    );
  });
});
