import {
  ResponsiveContainer,
  BarChart,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from 'recharts';
import type { QuizDay } from '../../types/api';
export default function QuizChartPlot({ data }: { data: QuizDay[] }) {
  return (
    <div className="quiz-chart" aria-label="Biểu đồ lượt trả lời Quiz đúng và sai">
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 16, right: 14, left: -12, bottom: 4 }} barSize={16}>
          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e5e7eb" />
          <XAxis
            dataKey="date"
            tickFormatter={(value) => String(value).slice(5).split('-').reverse().join('/')}
            tickLine={false}
            axisLine={false}
            fontSize={12}
            minTickGap={28}
          />
          <YAxis allowDecimals={false} tickLine={false} axisLine={false} fontSize={12} />
          <Tooltip
            cursor={{ fill: '#f1f5f9' }}
            labelFormatter={(value) => `Ngày ${String(value)}`}
            contentStyle={{ borderRadius: 8, border: '1px solid #e5e7eb', fontSize: 13 }}
          />
          <Legend iconType="square" iconSize={9} wrapperStyle={{ fontSize: 12, paddingTop: 16 }} />
          <Bar
            dataKey="correct"
            name="Đúng"
            fill="#2563eb"
            radius={[2, 2, 0, 0]}
            isAnimationActive={false}
          />
          <Bar
            dataKey="wrong"
            name="Sai"
            fill="#94a3b8"
            radius={[2, 2, 0, 0]}
            isAnimationActive={false}
          />
        </BarChart>
      </ResponsiveContainer>
    </div>
  );
}
