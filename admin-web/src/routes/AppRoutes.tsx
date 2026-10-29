import { Navigate, Outlet, Route, Routes, Link } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthProvider';
import { AdminLayout } from '../components/layout/AdminLayout';
import { LoginPage } from '../pages/LoginPage';
import { DashboardPage } from '../pages/DashboardPage';
import { TopicsPage } from '../pages/TopicsPage';
import { FlashcardsPage } from '../pages/FlashcardsPage';
import { UsersPage } from '../pages/UsersPage';
import { UserDetailPage } from '../pages/UserDetailPage';
import { QuizStatsPage } from '../pages/QuizStatsPage';
import { LeaderboardPage } from '../pages/LeaderboardPage';
import { VocabularyReviewPage } from '../pages/VocabularyReviewPage';
import { Empty } from '../components/ui';
export function AdminGuard() {
  const auth = useAuth();
  if (auth.isLoading)
    return (
      <div className="session-loading" role="status">
        Đang xác nhận phiên quản trị…
      </div>
    );
  return auth.isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}
export function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<AdminGuard />}>
        <Route path="/admin" element={<AdminLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="dashboard" element={<Navigate to="/admin" replace />} />
          <Route path="topics" element={<TopicsPage />} />
          <Route path="flashcards" element={<FlashcardsPage />} />
          <Route path="vocabulary-review" element={<VocabularyReviewPage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="progress" element={<UsersPage progressMode />} />
          <Route path="users/:id" element={<UserDetailPage />} />
          <Route path="users/:id/progress" element={<UserDetailPage tab="progress" />} />
          <Route path="users/:id/quiz-history" element={<UserDetailPage tab="history" />} />
          <Route path="quiz-statistics" element={<QuizStatsPage />} />
          <Route path="leaderboard" element={<LeaderboardPage />} />
          <Route
            path="*"
            element={
              <>
                <Empty
                  title="Không tìm thấy trang"
                  description="Chọn một mục trong menu để tiếp tục."
                />
                <Link className="button secondary" to="/admin">
                  Về Tổng quan
                </Link>
              </>
            }
          />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/admin" replace />} />
    </Routes>
  );
}
