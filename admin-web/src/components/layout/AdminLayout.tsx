import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  FolderOpen,
  Layers,
  Users,
  BookOpen,
  ChartNoAxesCombined,
  Trophy,
  LogOut,
  Menu,
  X,
  ClipboardCheck,
} from 'lucide-react';
import { useAuth } from '../../features/auth/AuthProvider';
import { Avatar, Button } from '../ui';
import { useCompactLayout } from '../../hooks/useCompactLayout';

const navigation = [
  { to: '/admin', label: 'Tổng quan', icon: LayoutDashboard, end: true },
  { to: '/admin/topics', label: 'Chủ đề', icon: FolderOpen },
  { to: '/admin/flashcards', label: 'Flashcards', icon: Layers },
  { to: '/admin/vocabulary-review', label: 'Kiểm duyệt Dataset', icon: ClipboardCheck },
  { to: '/admin/users', label: 'Người dùng', icon: Users },
  { to: '/admin/progress', label: 'Tiến độ học', icon: BookOpen },
  { to: '/admin/quiz-statistics', label: 'Thống kê Quiz', icon: ChartNoAxesCombined },
  { to: '/admin/leaderboard', label: 'Bảng xếp hạng', icon: Trophy },
];
export function Brand() {
  return (
    <div className="brand">
      <img className="brand-mark" src="/brand/bivocal-icon.svg" alt="" />
      <div>
        <strong>BiVocal</strong>
        <span>Admin Panel</span>
      </div>
    </div>
  );
}
export function AdminLayout() {
  const { admin, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const compact = useCompactLayout(),
    sidebar = useRef<HTMLElement>(null);
  useEffect(() => {
    if (!open || !compact) return;
    const previous = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const controls = () =>
      Array.from(
        sidebar.current?.querySelectorAll<HTMLElement>('a[href],button:not(:disabled)') ?? [],
      ).filter((element) => element.offsetParent !== null);
    controls()[0]?.focus();
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
      if (event.key !== 'Tab') return;
      const items = controls(),
        first = items[0],
        last = items.at(-1);
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    };
    document.addEventListener('keydown', handleKey);
    return () => {
      document.removeEventListener('keydown', handleKey);
      previous?.focus();
    };
  }, [open, compact]);
  const section = navigation
    .slice()
    .reverse()
    .find((item) =>
      item.end ? location.pathname === item.to : location.pathname.startsWith(item.to),
    );
  return (
    <div className="admin-shell">
      <a className="skip-link" href="#main-content">
        Đến nội dung chính
      </a>
      {open && (
        <button
          type="button"
          className="sidebar-backdrop"
          onClick={() => setOpen(false)}
          aria-label="Đóng menu"
        />
      )}
      <aside
        ref={sidebar}
        className={`sidebar ${open ? 'is-open' : ''}`}
        aria-label="Điều hướng quản trị"
        inert={compact && !open}
        aria-hidden={compact && !open}
        role={compact && open ? 'dialog' : undefined}
        aria-modal={compact && open ? true : undefined}
      >
        <Brand />
        <Button
          variant="quiet"
          className="mobile-close"
          aria-label="Đóng menu"
          onClick={() => setOpen(false)}
        >
          <X size={20} />
        </Button>
        <div className="nav-caption">QUẢN TRỊ HỆ THỐNG</div>
        <nav>
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end ?? false}
              onClick={() => setOpen(false)}
              className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
            >
              <item.icon size={18} strokeWidth={1.7} aria-hidden="true" />
              <span>{item.label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-account">
            <Avatar
              name={admin?.fullName || admin?.username || ''}
              url={admin?.avatarUrl ?? null}
            />
            <div>
              <strong>{admin?.fullName || admin?.username}</strong>
              <span>BiVoca Admin</span>
            </div>
          </div>
          <button type="button" className="logout-button" onClick={logout}>
            <LogOut size={17} aria-hidden="true" />
            Đăng xuất
          </button>
        </div>
      </aside>
      <div className="main-shell" inert={compact && open}>
        <header className="topbar">
          <div className="topbar-left">
            <Button
              variant="quiet"
              className="mobile-toggle"
              aria-label="Mở menu"
              aria-expanded={open}
              onClick={() => setOpen(true)}
            >
              <Menu size={21} />
            </Button>
            <span className="topbar-brand">BiVocal</span>
            <span className="breadcrumb-divider">/</span>
            <span>{section?.label ?? 'Chi tiết người dùng'}</span>
          </div>
          <span className="environment-label">Quản trị hệ thống</span>
        </header>
        <main id="main-content" className="main-content" tabIndex={-1}>
          <Outlet />
        </main>
        <footer className="main-footer">
          BiVoca <span>Admin Panel</span>
        </footer>
      </div>
    </div>
  );
}
