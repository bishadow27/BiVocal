import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/admin.api';
import {
  Avatar,
  Badge,
  DataTable,
  PageHeader,
  Pagination,
  Panel,
  QueryView,
} from '../components/ui';
import { useListState } from '../hooks/useListState';
import { number } from '../lib/format';
export function LeaderboardPage() {
  const list = useListState(),
    filters = { page: list.page, limit: list.limit };
  const query = useQuery({
    queryKey: ['admin-leaderboard', filters],
    queryFn: ({ signal }) => adminApi.leaderboard(filters, signal),
    placeholderData: keepPreviousData,
  });
  return (
    <>
      <PageHeader
        title="Bảng xếp hạng"
        description="Người học có XP tích lũy · Hạng đồng điểm lấy trực tiếp từ hệ thống"
      />
      <Panel>
        <QueryView query={query}>
          {(data) => (
            <>
              <DataTable
                rows={data.items}
                rowKey={(u) => u.userId}
                columns={[
                  {
                    label: 'Hạng',
                    render: (u) => <Badge tone={u.rank <= 3 ? 'blue' : 'neutral'}>#{u.rank}</Badge>,
                  },
                  {
                    label: 'Người dùng',
                    render: (u) => (
                      <div className="user-cell">
                        <Avatar name={u.fullName || u.username} url={u.avatarUrl} />
                        <div>
                          <Link className="table-link" to={`/admin/users/${u.userId}`}>
                            {u.fullName || u.username}
                          </Link>
                          <small>@{u.username}</small>
                        </div>
                      </div>
                    ),
                  },
                  {
                    label: 'XP',
                    render: (u) => <strong>{number(u.totalXp)}</strong>,
                    className: 'numeric',
                  },
                  { label: 'Level', render: (u) => u.level, className: 'numeric' },
                  { label: 'Streak', render: (u) => u.currentStreak, className: 'numeric' },
                  { label: 'Longest Streak', render: (u) => u.longestStreak, className: 'numeric' },
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
    </>
  );
}
