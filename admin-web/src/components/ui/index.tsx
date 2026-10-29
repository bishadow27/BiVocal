import {
  createContext,
  useContext,
  useEffect,
  useId,
  useRef,
  useState,
  type ButtonHTMLAttributes,
  type ReactNode,
} from 'react';
import {
  AlertCircle,
  Check,
  ChevronLeft,
  ChevronRight,
  Inbox,
  LoaderCircle,
  X,
} from 'lucide-react';
import type { UseQueryResult } from '@tanstack/react-query';
import { errorMessage } from '../../api/errors';
import { number } from '../../lib/format';

export function Button({
  children,
  variant = 'secondary',
  busy = false,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'quiet';
  busy?: boolean;
}) {
  return (
    <button
      type="button"
      {...props}
      disabled={props.disabled || busy}
      className={`button ${variant} ${props.className ?? ''}`}
    >
      {busy && <LoaderCircle size={16} className="animate-spin" aria-hidden="true" />}
      {children}
    </button>
  );
}
export function Badge({
  children,
  tone = 'neutral',
}: {
  children: ReactNode;
  tone?: 'neutral' | 'blue' | 'green' | 'red';
}) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
export function Avatar({ name, url = null }: { name: string; url?: string | null }) {
  const [failed, setFailed] = useState(false);
  return (
    <span className="avatar" aria-hidden="true">
      {url && !failed ? (
        <img src={url} alt="" referrerPolicy="no-referrer" onError={() => setFailed(true)} />
      ) : (
        name.slice(0, 2).toUpperCase()
      )}
    </span>
  );
}
export function PageHeader({
  title,
  description,
  actions,
}: {
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        <p>{description}</p>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}
export function Panel({
  title,
  subtitle,
  children,
  className = '',
}: {
  title?: string;
  subtitle?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      {title && (
        <div className="panel-heading">
          <h2>{title}</h2>
          {subtitle && <p>{subtitle}</p>}
        </div>
      )}
      {children}
    </section>
  );
}
export function Empty({
  title = 'Chưa có dữ liệu',
  description = 'Dữ liệu sẽ xuất hiện tại đây khi có hoạt động phù hợp.',
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="empty-state">
      <Inbox size={27} strokeWidth={1.4} aria-hidden="true" />
      <h3>{title}</h3>
      <p>{description}</p>
    </div>
  );
}
export function ErrorPanel({ error, retry }: { error: unknown; retry?: () => void }) {
  return (
    <div className="error-panel" role="alert">
      <AlertCircle size={18} aria-hidden="true" />
      <div>
        <strong>Không thể tải dữ liệu</strong>
        <p>{errorMessage(error)}</p>
        {retry && <Button onClick={retry}>Thử lại</Button>}
      </div>
    </div>
  );
}
export function QueryView<T>({
  query,
  children,
}: {
  query: UseQueryResult<T, Error>;
  children: (data: T) => ReactNode;
}) {
  if (query.isPending)
    return (
      <div className="panel skeleton" role="status" aria-label="Đang tải dữ liệu">
        <div />
        <div />
        <div />
        <div />
      </div>
    );
  if (query.isError)
    return (
      <ErrorPanel
        error={query.error}
        retry={() => {
          void query.refetch();
        }}
      />
    );
  return <div aria-busy={query.isFetching}>{children(query.data)}</div>;
}
export interface Column<T> {
  label: string;
  render: (row: T) => ReactNode;
  className?: string;
}
export function DataTable<T>({
  rows,
  columns,
  rowKey,
  empty = 'Không có kết quả phù hợp',
}: {
  rows: T[];
  columns: Column<T>[];
  rowKey: (row: T) => string | number;
  empty?: string;
}) {
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th scope="col" key={c.label} className={c.className}>
                {c.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row) => (
              <tr key={rowKey(row)}>
                {columns.map((c) => (
                  <td key={c.label} className={c.className}>
                    {c.render(row)}
                  </td>
                ))}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={columns.length}>
                <Empty title={empty} description="Thử thay đổi bộ lọc hoặc từ khóa tìm kiếm." />
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
export function Truncate({ value }: { value: string | null }) {
  return (
    <span className="truncate-cell" title={value ?? undefined}>
      {value || '—'}
    </span>
  );
}
export function Pagination({
  page,
  limit,
  total,
  totalPages,
  onPage,
  onLimit,
  busy = false,
}: {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
  onPage: (page: number) => void;
  onLimit: (limit: number) => void;
  busy?: boolean;
}) {
  return (
    <div className="pagination">
      <div className="pagination-count">
        {number(total)} kết quả <span className="separator">·</span>
        <label>
          Số dòng{' '}
          <select
            aria-label="Số dòng mỗi trang"
            value={limit}
            disabled={busy}
            onChange={(e) => onLimit(Number(e.target.value))}
          >
            {[10, 20, 50, 100].map((n) => (
              <option key={n}>{n}</option>
            ))}
          </select>
        </label>
      </div>
      <div className="pagination-controls">
        <span>
          Trang {page} / {Math.max(1, totalPages)}
        </span>
        <Button
          aria-label="Trang trước"
          disabled={busy || page <= 1}
          onClick={() => onPage(page - 1)}
        >
          <ChevronLeft size={16} />
        </Button>
        <Button
          aria-label="Trang sau"
          disabled={busy || page >= totalPages}
          onClick={() => onPage(page + 1)}
        >
          <ChevronRight size={16} />
        </Button>
      </div>
    </div>
  );
}
export function TextField({
  label,
  value,
  onChange,
  error,
  multiline = false,
  ...props
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  error?: string | undefined;
  multiline?: boolean;
  type?: string;
  maxLength?: number;
  required?: boolean;
  autoComplete?: string;
  disabled?: boolean;
  placeholder?: string;
  help?: string;
}) {
  const id = useId();
  const { help, ...attributes } = props;
  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {props.required && <span className="required"> *</span>}
      </label>
      {multiline ? (
        <textarea
          id={id}
          rows={3}
          value={value}
          {...attributes}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : help ? `${id}-help` : undefined}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : (
        <input
          id={id}
          value={value}
          {...attributes}
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : help ? `${id}-help` : undefined}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {error ? (
        <p id={`${id}-error`} className="field-error">
          {error}
        </p>
      ) : (
        help && (
          <p id={`${id}-help`} className="field-help">
            {help}
          </p>
        )
      )}
    </div>
  );
}
export function SelectField({
  label,
  value,
  onChange,
  options,
  disabled = false,
  error,
  required = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: { value: string; label: string }[];
  disabled?: boolean;
  error?: string | undefined;
  required?: boolean;
}) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>
        {label}
        {required && <span className="required"> *</span>}
      </label>
      <select
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        required={required}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${id}-error` : undefined}
      >
        {options.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {error && (
        <p className="field-error" id={`${id}-error`}>
          {error}
        </p>
      )}
    </div>
  );
}
export function Dialog({
  title,
  children,
  onClose,
  busy = false,
  drawer = false,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  busy?: boolean;
  drawer?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const headingId = useId();
  useEffect(() => {
    const dialog = ref.current;
    dialog?.showModal();
    return () => {
      dialog?.close();
    };
  }, []);
  return (
    <dialog
      ref={ref}
      className={drawer ? 'drawer' : 'confirm-dialog'}
      aria-labelledby={headingId}
      onCancel={(e) => {
        e.preventDefault();
        if (!busy) onClose();
      }}
    >
      <header className="dialog-header">
        <h2 id={headingId}>{title}</h2>
        <Button variant="quiet" disabled={busy} onClick={onClose} aria-label="Đóng hộp thoại">
          <X size={18} />
        </Button>
      </header>
      {children}
    </dialog>
  );
}
export function ConfirmDialog({
  title,
  description,
  busy,
  onClose,
  onConfirm,
  error,
}: {
  title: string;
  description: string;
  busy: boolean;
  onClose: () => void;
  onConfirm: () => void;
  error?: unknown;
}) {
  return (
    <Dialog title={title} onClose={onClose} busy={busy}>
      <div className="dialog-body">
        <p>{description}</p>
        {Boolean(error) && (
          <p className="form-error" role="alert">
            {errorMessage(error)}
          </p>
        )}
      </div>
      <footer className="dialog-footer">
        <Button disabled={busy} onClick={onClose}>
          Hủy
        </Button>
        <Button variant="primary" busy={busy} onClick={onConfirm}>
          Xác nhận
        </Button>
      </footer>
    </Dialog>
  );
}
const ToastContext = createContext<(message: string) => void>(() => {});
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState('');
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(''), 4000);
    return () => clearTimeout(timer);
  }, [toast]);
  return (
    <ToastContext.Provider value={setToast}>
      {children}
      {toast && (
        <div className="toast" role="status">
          <Check size={17} />
          <span>{toast}</span>
          <button type="button" aria-label="Đóng thông báo" onClick={() => setToast('')}>
            <X size={16} />
          </button>
        </div>
      )}
    </ToastContext.Provider>
  );
}
export const useToast = () => useContext(ToastContext);
