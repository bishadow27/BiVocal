import { lazy, Suspense } from 'react';
import type { QuizDay } from '../../types/api';
import { Empty } from '../ui';
const Chart = lazy(() => import('./QuizChartPlot'));
export function QuizChart({ data }: { data: QuizDay[] }) {
  if (!data.some((day) => day.answers > 0))
    return (
      <Empty
        title="Chưa có lượt trả lời Quiz"
        description="Biểu đồ sẽ hiển thị khi có hoạt động trong khoảng thời gian đã chọn."
      />
    );
  return (
    <Suspense
      fallback={
        <div className="chart-loading" role="status">
          Đang tải biểu đồ…
        </div>
      }
    >
      <Chart data={data} />
    </Suspense>
  );
}
