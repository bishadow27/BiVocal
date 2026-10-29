import { useState } from 'react';
import { Link, NavLink, useParams } from 'react-router-dom';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Search, ShieldCheck } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import { useAuth } from '../features/auth/AuthProvider';
import {
  Avatar,
  Badge,
  Button,
  ConfirmDialog,
  DataTable,
  Empty,
  PageHeader,
  Pagination,
  Panel,
  QueryView,
  Truncate,
  useToast,
} from '../components/ui';
import { useListState } from '../hooks/useListState';
import { dateTime, number, percent, validUserId } from '../lib/format';
import { invalidate } from '../lib/invalidate';

export function UserDetailPage({ tab = 'account' }: { tab?: 'account' | 'progress' | 'history' }) {
  const { id } = useParams(),
    valid = validUserId(id),
    userId = Number(id);
  const auth = useAuth(),
    client = useQueryClient(),
    toast = useToast();
  const [confirm, setConfirm] = useState(false);
  const query = useQuery({
    queryKey: ['admin-user', userId],
    queryFn: ({ signal }) => adminApi.user(userId, signal),
    enabled: valid,
  });
  const mutation = useMutation({
    mutationFn: (role: 'ADMIN' | 'USER') => adminApi.role(userId, role),
    onSuccess: () => {
      void invalidate(client, ['admin-users', 'admin-user']);
      toast('Đã cập nhật vai trò người dùng.');
      setConfirm(false);
    },
  });
  if (!valid)
    return (
      <Empty
        title="ID người dùng không hợp lệ"
        description="Chọn người dùng từ danh sách để xem thông tin."
      />
    );
  return (
    <>
      <Link className="back-link" to="/admin/users">
        <ArrowLeft size={16} />
        Người dùng
      </Link>
      <QueryView query={query}>
        {(data) => (
          <>
            <PageHeader
              title={data.user.fullName || data.user.username}
              description={`Thông tin tài khoản @${data.user.username}`}
              actions={
                <Button
                  disabled={data.user.id === auth.admin?.id}
                  title={
                    data.user.id === auth.admin?.id
                      ? 'Không thể đổi vai trò tài khoản đang đăng nhập'
                      : undefined
                  }
                  onClick={() => {
                    mutation.reset();
                    setConfirm(true);
                  }}
                >
                  <ShieldCheck size={16} />
                  Đổi vai trò
                </Button>
              }
            />
            <nav className="tabs" aria-label="Thông tin người dùng">
              <NavLink to={`/admin/users/${userId}`} end>
                Thông tin tài khoản
              </NavLink>
              <NavLink to={`/admin/users/${userId}/progress`}>Tiến độ học</NavLink>
              <NavLink to={`/admin/users/${userId}/quiz-history`}>Lịch sử Quiz</NavLink>
            </nav>
            {tab === 'account' ? (
              <>
                <Panel>
                  <div className="account-heading">
                    <Avatar
                      name={data.user.fullName || data.user.username}
                      url={data.user.avatarUrl}
                    />
                    <div>
                      <h2>{data.user.username}</h2>
                      <p>{data.user.email}</p>
                    </div>
                    <Badge tone={data.user.role === 'ADMIN' ? 'blue' : 'neutral'}>
                      {data.user.role}
                    </Badge>
                  </div>
                  <dl className="details-grid">
                    <Detail label="Họ và tên" value={data.user.fullName || '—'} />
                    <Detail label="Ngày tạo" value={dateTime(data.user.createdAt)} />
                    <Detail label="Đăng nhập gần nhất" value={dateTime(data.user.lastLogin)} />
                    <Detail label="ID tài khoản" value={String(data.user.id)} />
                  </dl>
                </Panel>
                <div className="summary-grid">
                  <Panel title="Progress Summary">
                    <dl>
                      <Detail
                        label="Cards đang theo dõi"
                        value={number(data.progress.trackedCards)}
                      />
                      <Detail label="Cards đã ôn" value={number(data.progress.reviewedCards)} />
                      <Detail
                        label="Cards ghi nhớ tốt"
                        value={number(data.progress.masteredCards)}
                      />
                      <Detail label="Favorite" value={number(data.progress.favoriteCards)} />
                      <Detail label="Tổng lượt ôn" value={number(data.progress.totalReviews)} />
                      <Detail
                        label="Đúng / Sai"
                        value={`${number(data.progress.correctAnswers)} / ${number(data.progress.wrongAnswers)}`}
                      />
                    </dl>
                    <p className="summary-note">Summary chỉ tính Flashcards Active.</p>
                  </Panel>
                  <Panel title="Quiz Summary">
                    <dl>
                      <Detail label="Lượt trả lời" value={number(data.quiz.totalAnswers)} />
                      <Detail label="Trả lời đúng" value={number(data.quiz.correctAnswers)} />
                      <Detail label="Trả lời sai" value={number(data.quiz.wrongAnswers)} />
                      <Detail label="Độ chính xác" value={percent(data.quiz.accuracy)} />
                    </dl>
                  </Panel>
                  <Panel title="Gamification Summary">
                    <dl>
                      <Detail label="Tổng XP" value={number(data.gamification.totalXp)} />
                      <Detail label="Level" value={String(data.gamification.level)} />
                      <Detail
                        label="Streak hiện tại"
                        value={String(data.gamification.currentStreak)}
                      />
                      <Detail
                        label="Longest Streak"
                        value={String(data.gamification.longestStreak)}
                      />
                      <Detail
                        label="Hoạt động gần nhất (UTC)"
                        value={data.gamification.lastActivityDate || '—'}
                      />
                    </dl>
                    <div className="xp-meter">
                      <progress
                        value={data.gamification.xpIntoLevel}
                        max={data.gamification.xpPerLevel}
                        aria-label="XP trong Level hiện tại"
                      />
                      <span>
                        {data.gamification.xpIntoLevel}/{data.gamification.xpPerLevel} XP trong
                        Level
                      </span>
                    </div>
                  </Panel>
                </div>
              </>
            ) : tab === 'progress' ? (
              <UserProgress key={userId} userId={userId} />
            ) : (
              <UserQuizHistory key={userId} userId={userId} />
            )}
            {confirm && (
              <ConfirmDialog
                title="Xác nhận đổi vai trò"
                description={`Chuyển @${data.user.username} từ ${data.user.role} sang ${data.user.role === 'ADMIN' ? 'USER' : 'ADMIN'}? Quyền mới có hiệu lực ở request tiếp theo.`}
                busy={mutation.isPending}
                error={mutation.error}
                onClose={() => setConfirm(false)}
                onConfirm={() => mutation.mutate(data.user.role === 'ADMIN' ? 'USER' : 'ADMIN')}
              />
            )}
          </>
        )}
      </QueryView>
    </>
  );
}
function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-row">
      <dt>{label}</dt>
      <dd>{value}</dd>
    </div>
  );
}
function UserProgress({ userId }: { userId: number }) {
  const list = useListState();
  const query = useQuery({
    queryKey: ['admin-user-progress', userId, list.filters],
    queryFn: ({ signal }) => adminApi.progress(userId, list.filters, signal),
    placeholderData: keepPreviousData,
  });
  return (
    <Panel>
      <div className="toolbar">
        <div className="search-field">
          <Search size={17} aria-hidden="true" />
          <input
            aria-label="Tìm tiến độ"
            placeholder="Tìm English hoặc Vietnamese…"
            maxLength={200}
            value={list.search}
            onChange={(e) => list.setSearch(e.target.value)}
          />
        </div>
        <span className="toolbar-note">Chỉ xem · Bao gồm cả Flashcards Inactive</span>
      </div>
      <QueryView query={query}>
        {(data) => (
          <>
            <DataTable
              rows={data.items}
              rowKey={(p) => p.flashcardId}
              columns={[
                {
                  label: 'English',
                  render: (p) => (
                    <strong>
                      <Truncate value={p.english} />
                    </strong>
                  ),
                },
                { label: 'Vietnamese', render: (p) => <Truncate value={p.vietnamese} /> },
                { label: 'Chủ đề', render: (p) => <Badge>{p.topicCode}</Badge> },
                {
                  label: 'Memory Level',
                  render: (p) => (
                    <div className="memory-level">
                      <progress
                        max={5}
                        value={p.memoryLevel}
                        aria-label={`Memory Level ${p.memoryLevel} trên 5`}
                      />
                      <span>{p.memoryLevel}/5</span>
                    </div>
                  ),
                },
                { label: 'Reviews', render: (p) => p.reviewCount, className: 'numeric' },
                { label: 'Đúng', render: (p) => p.correctCount, className: 'numeric' },
                { label: 'Sai', render: (p) => p.wrongCount, className: 'numeric' },
                { label: 'Favorite', render: (p) => (p.isFavorite ? 'Có' : '—') },
                {
                  label: 'Ôn gần nhất',
                  render: (p) => <span className="nowrap muted">{dateTime(p.lastReview)}</span>,
                },
              ]}
            />
            <Pagination
              {...data}
              busy={query.isFetching}
              onPage={list.setPage}
              onLimit={list.setLimit}
            />
          </>
        )}
      </QueryView>
    </Panel>
  );
}
function UserQuizHistory({ userId }: { userId: number }) {
  const list = useListState();
  const filters = { page: list.page, limit: list.limit };
  const query = useQuery({
    queryKey: ['admin-user-quiz-history', userId, filters],
    queryFn: ({ signal }) => adminApi.history(userId, filters, signal),
    placeholderData: keepPreviousData,
  });
  return (
    <Panel title="Lịch sử Quiz" subtitle="Lịch sử trả lời của người dùng · Chỉ xem">
      <QueryView query={query}>
        {(data) => (
          <>
            <DataTable
              rows={data.items}
              rowKey={(q) => q.id}
              columns={[
                {
                  label: 'Câu hỏi',
                  render: (q) => (
                    <strong>
                      <Truncate value={q.question} />
                    </strong>
                  ),
                },
                { label: 'Đáp án đã chọn', render: (q) => <Truncate value={q.selectedAnswer} /> },
                { label: 'Đáp án đúng', render: (q) => <Truncate value={q.correctAnswer} /> },
                {
                  label: 'Kết quả',
                  render: (q) => (
                    <Badge tone={q.correct ? 'green' : 'red'}>{q.correct ? 'Đúng' : 'Sai'}</Badge>
                  ),
                },
                {
                  label: 'Thời gian trả lời',
                  render: (q) => <span className="nowrap">{dateTime(q.answeredAt)}</span>,
                },
              ]}
            />
            <Pagination
              {...data}
              busy={query.isFetching}
              onPage={list.setPage}
              onLimit={list.setLimit}
            />
          </>
        )}
      </QueryView>
    </Panel>
  );
}
