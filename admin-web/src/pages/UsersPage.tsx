import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Search, ArrowRight } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import type { UserFilters } from '../types/api';
import {
  Avatar,
  Badge,
  DataTable,
  PageHeader,
  Pagination,
  Panel,
  QueryView,
  SelectField,
} from '../components/ui';
import { useListState } from '../hooks/useListState';
import { dateTime, number } from '../lib/format';
export function UsersPage({ progressMode = false }: { progressMode?: boolean }) {
  const list = useListState();
  const [role, setRole] = useState<UserFilters['role']>('ALL');
  const filters = { ...list.filters, role };
  const query = useQuery({
    queryKey: ['admin-users', filters],
    queryFn: ({ signal }) => adminApi.users(filters, signal),
    placeholderData: keepPreviousData,
  });
  return (
    <>
      <PageHeader
        title={progressMode ? 'Tiến độ học' : 'Người dùng'}
        description={
          progressMode
            ? 'Chọn người dùng để xem tiến độ học và lịch sử Quiz'
            : 'Tra cứu tài khoản, vai trò và kết quả học tập'
        }
      />
      <Panel>
        <div className="toolbar">
          <div className="search-field">
            <Search size={17} aria-hidden="true" />
            <input
              aria-label="Tìm người dùng"
              placeholder="Tìm username, email hoặc họ tên…"
              maxLength={200}
              value={list.search}
              onChange={(e) => list.setSearch(e.target.value)}
            />
          </div>
          <SelectField
            label="Vai trò"
            value={role}
            onChange={(v) => {
              setRole(v as UserFilters['role']);
              list.setPage(1);
            }}
            options={[
              { value: 'ALL', label: 'Tất cả vai trò' },
              { value: 'USER', label: 'USER' },
              { value: 'ADMIN', label: 'ADMIN' },
            ]}
          />
        </div>
        <QueryView query={query}>
          {(data) => (
            <>
              <DataTable
                rows={data.items}
                rowKey={(u) => u.id}
                columns={[
                  {
                    label: 'Người dùng',
                    render: (u) => (
                      <div className="user-cell">
                        <Avatar name={u.fullName || u.username} url={u.avatarUrl} />
                        <div>
                          <Link
                            className="table-link"
                            to={`/admin/users/${u.id}${progressMode ? '/progress' : ''}`}
                          >
                            {u.username}
                          </Link>
                          <small>{u.fullName || '—'}</small>
                        </div>
                      </div>
                    ),
                  },
                  { label: 'Email', render: (u) => <span className="nowrap">{u.email}</span> },
                  {
                    label: 'Role',
                    render: (u) => (
                      <Badge tone={u.role === 'ADMIN' ? 'blue' : 'neutral'}>{u.role}</Badge>
                    ),
                  },
                  { label: 'XP', render: (u) => number(u.totalXp), className: 'numeric' },
                  { label: 'Level', render: (u) => u.level, className: 'numeric' },
                  { label: 'Streak', render: (u) => u.currentStreak, className: 'numeric' },
                  {
                    label: 'Đăng nhập gần nhất',
                    render: (u) => <span className="nowrap muted">{dateTime(u.lastLogin)}</span>,
                  },
                  {
                    label: 'Thao tác',
                    render: (u) => (
                      <Link
                        className="button quiet"
                        to={`/admin/users/${u.id}${progressMode ? '/progress' : ''}`}
                      >
                        {progressMode ? 'Xem tiến độ' : 'Chi tiết'}
                        <ArrowRight size={15} />
                      </Link>
                    ),
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
    </>
  );
}
