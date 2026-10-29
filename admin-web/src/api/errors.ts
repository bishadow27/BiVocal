export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
const messages: Record<string, string> = {
  INVALID_CREDENTIALS: 'Email hoặc mật khẩu không đúng.',
  AUTH_TOKEN_INVALID: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
  AUTH_TOKEN_MISSING: 'Vui lòng đăng nhập để tiếp tục.',
  AUTH_USER_NOT_FOUND: 'Tài khoản không còn tồn tại.',
  ADMIN_FORBIDDEN: 'Tài khoản này không có quyền quản trị BiVoca.',
  ADMIN_SELF_ROLE_CHANGE_FORBIDDEN: 'Bạn không thể tự hạ quyền tài khoản đang đăng nhập.',
  TOPIC_CODE_EXISTS: 'Mã chủ đề đã tồn tại.',
  TOPIC_NAME_EXISTS: 'Tên chủ đề đã tồn tại.',
  FLASHCARD_EXISTS: 'Cặp từ tiếng Anh và nghĩa tiếng Việt đã tồn tại, kể cả Flashcard Inactive.',
  ADMIN_TOPIC_NOT_FOUND: 'Không tìm thấy chủ đề. Vui lòng tải lại danh sách.',
  ADMIN_FLASHCARD_NOT_FOUND: 'Không tìm thấy Flashcard.',
  ADMIN_USER_NOT_FOUND: 'Không tìm thấy người dùng.',
  ADMIN_REFERENCE_CONFLICT: 'Dữ liệu liên quan vừa thay đổi. Vui lòng tải lại và thử lại.',
  VALIDATION_ERROR: 'Dữ liệu chưa hợp lệ. Vui lòng kiểm tra các trường trong biểu mẫu.',
  PAYLOAD_TOO_LARGE: 'Nội dung biểu mẫu quá dài.',
  NETWORK_ERROR: 'Không kết nối được máy chủ. Kiểm tra backend và thử lại.',
  REQUEST_TIMEOUT: 'Máy chủ phản hồi quá lâu. Vui lòng thử lại.',
  REVIEW_SCHEMA_UNAVAILABLE:
    'Database chưa có schema kiểm duyệt M13.6. Cần backup, chạy migration và nạp review queue trước khi sử dụng.',
  INTERNAL_SERVER_ERROR:
    'Backend gặp lỗi nội bộ. Hãy kiểm tra log máy chủ và trạng thái database.',
  ROUTE_NOT_FOUND:
    'Backend đang chạy phiên bản chưa có endpoint được yêu cầu.',
};
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError)
    return (
      messages[error.code] ??
      (error.status === 401
        ? 'Email hoặc mật khẩu không đúng.'
        : error.status === 403
          ? 'Bạn không có quyền thực hiện thao tác này.'
          : error.status === 404
            ? 'Không tìm thấy endpoint hoặc dữ liệu được yêu cầu.'
            : error.status === 500
              ? 'Backend gặp lỗi nội bộ. Hãy kiểm tra log máy chủ.'
              : error.status === 503
                ? 'Database hoặc dịch vụ Backend chưa sẵn sàng.'
                : 'Không thể hoàn thành yêu cầu. Vui lòng thử lại.')
    );
  return 'Có lỗi xảy ra. Vui lòng thử lại.';
}
