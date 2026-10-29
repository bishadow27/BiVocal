export const number = (value: number) => new Intl.NumberFormat('vi-VN').format(value);
export const percent = (value: number | null) =>
  value === null
    ? '—'
    : `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(value)}%`;
export const dateTime = (value: string | null) =>
  value
    ? new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeStyle: 'short' }).format(
        new Date(value),
      )
    : '—';
export const validUserId = (value: string | undefined) =>
  value !== undefined && /^[1-9]\d*$/.test(value) && Number(value) <= 2147483647;
