import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../api/admin.api';
import type { Range } from '../types/api';
import {
  Badge,
  DataTable,
  ErrorPanel,
  PageHeader,
  Panel,
  QueryView,
  SelectField,
} from '../components/ui';
import { QuizChart } from '../components/charts/QuizChart';
import { useTopicOptions } from '../hooks/useTopicOptions';
import { number, percent } from '../lib/format';
import { ranges } from './DashboardPage';
export function QuizStatsPage() {
  const [range, setRange] = useState<Range>('7d'),
    [topic, setTopic] = useState('');
  const topics = useTopicOptions();
  const query = useQuery({
    queryKey: ['admin-quiz-stats', range, topic],
    queryFn: ({ signal }) => adminApi.stats(range, topic, signal),
  });
  return (
    <>
      <PageHeader
        title="Thống kê Quiz"
        description="Theo dõi lượt trả lời và độ chính xác theo chủ đề"
        actions={
          <>
            <SelectField
              label="Khoảng thời gian"
              value={range}
              onChange={(v) => setRange(v as Range)}
              options={ranges}
            />
            <SelectField
              label="Chủ đề"
              value={topic}
              onChange={setTopic}
              disabled={topics.isPending}
              options={[
                { value: '', label: 'Tất cả chủ đề' },
                ...(topics.data ?? []).map((t) => ({ value: t.code, label: t.name })),
              ]}
            />
          </>
        }
      />
      {topics.isError && (
        <ErrorPanel
          error={topics.error}
          retry={() => {
            void topics.refetch();
          }}
        />
      )}
      <QueryView query={query}>
        {(data) => (
          <>
            <div className="kpi-grid">
              {[
                ['Tổng lượt trả lời', number(data.totalAnswers)],
                ['Trả lời đúng', number(data.correctAnswers)],
                ['Trả lời sai', number(data.wrongAnswers)],
                ['Độ chính xác', percent(data.accuracy)],
              ].map(([label, value]) => (
                <section className="kpi-card" key={label}>
                  <span className="kpi-label">{label}</span>
                  <strong>{value}</strong>
                  <p>Trong khoảng thời gian đã chọn</p>
                </section>
              ))}
            </div>
            <Panel title="Hoạt động Quiz" subtitle="Lượt trả lời đúng và sai theo ngày UTC">
              <QuizChart data={data.byDay} />
            </Panel>
            <Panel title="Kết quả theo chủ đề">
              <DataTable
                rows={data.byTopic}
                rowKey={(t) => t.code}
                columns={[
                  {
                    label: 'Chủ đề',
                    render: (t) => (
                      <div className="topic-cell">
                        <Badge>{t.code}</Badge>
                        <span>{t.name}</span>
                      </div>
                    ),
                  },
                  { label: 'Tổng', render: (t) => number(t.totalAnswers), className: 'numeric' },
                  { label: 'Đúng', render: (t) => number(t.correctAnswers), className: 'numeric' },
                  { label: 'Sai', render: (t) => number(t.wrongAnswers), className: 'numeric' },
                  {
                    label: 'Độ chính xác',
                    render: (t) => percent(t.accuracy),
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
