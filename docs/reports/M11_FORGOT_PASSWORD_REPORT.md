# M11 — Màn Quên mật khẩu BiVocal

**Ngày:** 2026-10-14  
**Trạng thái:** Giao diện và kiểm tra email đã triển khai; chức năng gửi email đặt lại mật khẩu **chưa có** vì Backend không có API/email service. Không ghi nhận gửi yêu cầu thành công.

## 1. Đối chiếu source và phạm vi

Đã đọc `AGENTS.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, LoginActivity/ViewModel, AuthApi/Repository, RegisterActivity, các layout/theme liên quan và Backend `auth.route.ts`, `auth.controller.ts`, `auth.schema.ts`, `auth.service.ts`. `POST /api/v1/auth/register`, `POST /api/v1/auth/login` và `GET /api/v1/auth/me` là ba route Auth hiện có. Không có route/request DTO/service reset password trong `backend/src`, cũng không có cấu hình SMTP/email provider trong source hoặc `.env.example`. Backend README cũng liệt kê password reset là chức năng chưa có.

Vì vậy ứng dụng **không gọi HTTP**, không tạo token, không gửi email và không hiện thông báo thành công giả. Chưa có luồng loading, lỗi mạng hay gửi trùng để kiểm thử: những trạng thái đó chỉ áp dụng sau khi Backend có API thật. Không sửa Backend, PostgreSQL, Login/Register business logic, SessionManager, JWT hoặc module M1–M10.

## 2. Giao diện và hành vi thực tế

- Login `Quên mật khẩu?` mở `ForgotPasswordActivity`; liên kết `Quay lại đăng nhập` dùng `finish()` trở về Login hiện có.
- Màn mới dùng lại theme/nền navy, logo ngang BiVocal, trăng/sao, ô nhập/viền focus và nút xanh của Login. `ScrollView`, `adjustResize` và xử lý system bar/IME insets hỗ trợ bàn phím, xoay và màn thấp.
- `ForgotPasswordViewModel` kiểm tra email trống và sai định dạng bằng LiveData. Email hợp lệ chỉ nhận thông báo: “Máy chủ chưa hỗ trợ khôi phục mật khẩu. Không có email nào được gửi.” Màn hình cũng có ghi chú cố định là chưa thể gửi email.
- Không điền sẵn email, không lưu email vào Room hoặc log, không hỏi mật khẩu. Nút gửi không tạo request nên không có yêu cầu trùng.

## 3. File thêm/sửa

- Thêm `android-app/app/src/main/java/com/example/bivoca/ui/auth/ForgotPasswordActivity.java`, `ForgotPasswordViewModel.java` và `android-app/app/src/main/res/layout/activity_forgot_password.xml`.
- Thêm `android-app/app/src/main/res/values/forgot_colors.xml`; thêm chuỗi giao diện trong `values/strings.xml`.
- Sửa tối thiểu `LoginActivity.java` để mở màn mới; khai báo Activity với theme Login trong `AndroidManifest.xml`.
- Cập nhật `M11_FORGOT_PASSWORD_REPORT.md`, `M11_LOGIN_UI_REPORT.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`.

## 4. Kiểm thử

| Lệnh/bước | Kết quả |
|---|---|
| `:app:assembleDebug` | PASS |
| `:app:testDebugUnitTest` | 60/60 PASS, 0 failure/error/skipped |
| `:app:lintDebug` | PASS, 0 lỗi/197 cảnh báo toàn app |
| `adb devices -l`; `adb install -r ...app-debug.apk` | OPPO CPH2557 ở trạng thái `device`; cài `Success`, giữ dữ liệu ứng dụng |
| `emulator -list-avds` | Không có AVD; chưa chạy Emulator |
| Login → Quên mật khẩu trên OPPO | PASS: `topResumedActivity` chuyển từ `LoginActivity` sang `ForgotPasswordActivity`; chụp ảnh thật `android-app/app/build/reports/m11-forgot/oppo-forgot-initial.png` ở 1080×2400, các thành phần chính hiển thị đầy đủ |
| Quay lại Login, email trống/sai/hợp lệ, bàn phím và xoay trên OPPO | Chưa xác minh trọn vẹn: ADB mất kết nối trong lúc thử; `adb devices -l` sau đó không còn thiết bị. Mã xử lý đã được kiểm tra và build/unit/lint PASS, nhưng không thay thế kết quả thao tác trên máy. |

Người dùng chọn để việc bổ sung API khôi phục mật khẩu sang giai đoạn sau. Màn hiện tại chỉ hướng dẫn và kiểm tra định dạng email; không gửi yêu cầu khôi phục.

## 5. Hướng triển khai reset password an toàn khi có Backend

Đề xuất cho milestone Backend riêng: bổ sung endpoint yêu cầu reset trả phản hồi giống nhau cho email tồn tại/không tồn tại; giới hạn tần suất theo tài khoản và IP. Sinh token ngẫu nhiên bằng CSPRNG, lưu dạng hash gắn với user, dùng một lần và hết hạn ngắn (ví dụ 15 phút). Chỉ khi dịch vụ email được cấu hình và gửi/đưa vào hàng đợi thật mới báo đã tiếp nhận; nếu mail service không sẵn sàng, trả lỗi khả dụng thay vì báo thành công giả. Link HTTPS trỏ tới domain cố định đáng tin, không xây từ Host header. Endpoint xác nhận token phải kiểm tra hết hạn/chưa dùng, đổi mật khẩu bằng password hashing hiện có trong transaction, vô hiệu hóa token sau khi dùng và thông báo đổi mật khẩu qua email. Kiểm thử trên DB riêng trước; sao lưu và xin chấp thuận riêng trước bất kỳ migration DB vận hành. Có thể dùng SMTP sink local cho bài tập, nhưng chỉ để kiểm thử và không coi nó là gửi email production.

Các kiểm soát về phản hồi đồng nhất, token ngẫu nhiên/một lần/hết hạn, rate limit và email side-channel dựa trên [OWASP Forgot Password Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).
