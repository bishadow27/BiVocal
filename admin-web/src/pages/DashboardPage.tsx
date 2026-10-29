import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Users, Layers, CheckCheck, Target } from 'lucide-react';
import { adminApi } from '../api/admin.api';
import type { Range } from '../types/api';
import {
  Avatar,
  Badge,
  DataTable,
  Empty,
  PageHeader,
  Panel,
  QueryView,
  SelectField,
} from '../components/ui';
import { QuizChart } from '../components/charts/QuizChart';
import { number, percent } from '../lib/format';
export const ranges = [
  { value: '7d', label: '7 ngày' },
  { value: '30d', label: '30 ngày' },
  { value: 'all', label: 'Tất cả' },
];
export function DashboardPage() {
  const [range, setRange] = useState<Range>('7d');
  const query = useQuery({
    queryKey: ['admin-dashboard', range],
    queryFn: ({ signal }) => adminApi.dashboard(range, signal),
  });
  return (
    <>
      <PageHeader
        title="Tổng quan"
        description="Thống kê tổng quan về hệ thống BiVoca"
        actions={
          <SelectField
            label="Khoảng thời gian"
            value={range}
            onChange={(v) => setRange(v as Range)}
            options={ranges}
          />
        }
      />
      <QueryView query={query}>
        {(data) => (
          <>
            <div className="kpi-grid">
              {[
                [Users, 'Người dùng', number(data.users.total), 'Tất cả tài khoản'],
                [
                  Layers,
                  'Flashcards đang hoạt động',
                  number(data.flashcards.active),
                  `${number(data.flashcards.total)} Flashcards trong hệ thống`,
                ],
                [
                  CheckCheck,
                  'Lượt trả lời Quiz',
                  number(data.quiz.totalAnswers),
                  'Trong khoảng thời gian đã chọn',
                ],
                [Target, 'Độ chính xác', percent(data.quiz.accuracy), 'Tỷ lệ trả lời đúng'],
              ].map(([Icon, title, value, note]) => {
                const KpiIcon = Icon as typeof Users;
                return (
                  <section className="kpi-card" key={String(title)}>
                    <div className="kpi-label">
                      <span>{String(title)}</span>
                      <KpiIcon size={18} strokeWidth={1.6} aria-hidden="true" />
                    </div>
                    <strong>{String(value)}</strong>
                    <p>{String(note)}</p>
                  </section>
                );
              })}
            </div>
            <div className="dashboard-grid">
              <Panel title="Hoạt động Quiz" subtitle="Lượt trả lời đúng và sai theo ngày UTC">
                <QuizChart data={data.quizByDay} />
              </Panel>
              <Panel title="Top người học" subtitle="Xếp theo tổng XP tích lũy">
                {data.topUsers.length ? (
                  <ol className="top-users">
                    {data.topUsers.map((user, i) => (
                      <li key={user.userId}>
                        <span className="top-order">{i + 1}</span>
                        <Avatar name={user.fullName || user.username} />
                        <div className="top-user-name">
                          <Link to={`/admin/users/${user.userId}`}>
                            {user.fullName || user.username}
                          </Link>
                          <span>
                            @{user.username} · Level {user.level}
                          </span>
                        </div>
                        <strong>
                          {number(user.totalXp)}
                          <small> XP</small>
                        </strong>
                      </li>
                    ))}
                  </ol>
                ) : (
                  <Empty title="Chưa có người học nhận XP" />
                )}
              </Panel>
            </div>
            <Panel
              title="Top chủ đề"
              subtitle="Chủ đề có nhiều lượt trả lời Quiz trong khoảng thời gian đã chọn"
            >
              <DataTable
                rows={data.topTopics}
                rowKey={(t) => t.code}
                columns={[
                  { label: 'Mã chủ đề', render: (t) => <Badge>{t.code}</Badge> },
                  { label: 'Chủ đề', render: (t) => t.name },
                  {
                    label: 'Lượt trả lời Quiz',
                    render: (t) => number(t.quizCount),
                    className: 'numeric',
                  },
                ]}
              />
            </Panel>
          </>
        )}
      </QueryView>
    </>
  );
}
