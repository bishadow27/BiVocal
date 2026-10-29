import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../features/auth/AuthProvider';
import { Brand } from '../components/layout/AdminLayout';
import { Button, TextField } from '../components/ui';

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  if (auth.isAuthenticated) return <Navigate to="/admin" replace />;
  async function submit(event: FormEvent) {
    event.preventDefault();
    const validation: Record<string, string> = {};
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim()) || email.trim().length > 150)
      validation.email = 'Nhập email hợp lệ.';
    if (!password) validation.password = 'Nhập mật khẩu của bạn.';
    setErrors(validation);
    if (Object.keys(validation).length) return;
    if (await auth.login({ email: email.trim(), password })) {
      setPassword('');
      navigate('/admin', { replace: true });
    } else setPassword('');
  }
  return (
    <main className="login-page">
      <div className="login-card">
        <Brand />
        <div className="login-heading">
          <h1>Đăng nhập quản trị</h1>
          <p>Quản lý nội dung và hoạt động học tập BiVoca.</p>
        </div>
        {auth.notice && (
          <div className="form-error" role="alert">
            {auth.notice}
          </div>
        )}
        {auth.accessToken && !auth.isAuthenticated && (
          <Button
            variant="secondary"
            onClick={() => void auth.retryRestore()}
            busy={auth.isLoading}
          >
            Thử kết nối lại
          </Button>
        )}
        <form onSubmit={submit} noValidate>
          <TextField
            label="Email"
            type="email"
            value={email}
            onChange={setEmail}
            required
            autoComplete="username"
            maxLength={150}
            error={errors.email}
            disabled={auth.isLoading}
          />
          <TextField
            label="Mật khẩu"
            type="password"
            value={password}
            onChange={setPassword}
            required
            autoComplete="current-password"
            error={errors.password}
            disabled={auth.isLoading}
          />
          <Button variant="primary" type="submit" busy={auth.isLoading} className="login-submit">
            {auth.isLoading ? 'Đang xác nhận quyền…' : 'Đăng nhập'}
          </Button>
        </form>
        <p className="login-note">Dành cho tài khoản có quyền ADMIN.</p>
      </div>
      <footer>
        BiVoca <span>Admin Panel</span>
      </footer>
    </main>
  );
}
