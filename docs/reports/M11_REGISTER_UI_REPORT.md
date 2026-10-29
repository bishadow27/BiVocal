# M11 — Giao diện Đăng ký BiVocal

**Ngày:** 2026-10-15  
**Trạng thái:** **DONE trên Android Emulator API 35.** Build, 73/73 unit tests, lint và nghiệm thu giao diện/điều hướng/IME/validation/mắt mật khẩu/xoay đều PASS. Không tạo tài khoản test trên DB vận hành.

## 1. Source đã đọc và contract thực tế

Đã đọc `AGENTS.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, `PROJECT_CONTEXT.md`, `BACKEND_CONTEXT.md`, `M10_SPACED_REPETITION_REPORT.md`, `M11_LOGIN_UI_REPORT.md`; Android `LoginActivity`, `LoginViewModel`, `RegisterActivity`, `RegisterViewModel`, `AuthApi`, `AuthRepository`, `RegisterRequest`, `AuthSessionExpiry`, Manifest, layout Login/Register, strings/theme/drawable; Backend `auth.route.ts`, `auth.controller.ts`, `auth.schema.ts`, `auth.service.ts`.

Backend `POST /api/v1/auth/register` nhận `username`, `email`, `password` và `fullName` tùy chọn, trả HTTP 201 cùng `user` và `accessToken`. Backend dùng username ASCII chữ/số/`_` dài 3–30, email tối đa 150 ký tự, tên tối đa 100 ký tự; mật khẩu ít nhất 8 ký tự và tối đa 72 byte UTF-8. Role không có trong request và Backend tự gán USER. Android giữ luồng cũ: đăng ký thành công báo người dùng rồi quay về Login, không tự lưu JWT đăng ký vào SessionManager.

## 2. Giao diện và hành vi đã triển khai

- Lưu ảnh người dùng cung cấp thành `M11_REGISTER_REFERENCE.png` để đối chiếu. Register dùng lại `login_night_background`, `login_moon`, `login_moon_glow`, `login_star`, `bivocal_wordmark`, `login_field_background`, icon người/khóa/mắt, màu nút xanh và `Theme.BiVoca.Login`. Chỉ thêm icon thư `register_mail` và viền lỗi `register_field_error`. Không dùng ảnh mẫu làm background hay form giả.
- Bố cục `[logo BiVocal] | ĐĂNG KÝ`, năm trường theo thứ tự Họ và tên tùy chọn, Tên đăng nhập, Email, Mật khẩu, Xác nhận mật khẩu; nhãn/dấu sao/placeholder tiếng Việt. Hai nút mắt 48dp giữ ký tự và con trỏ; trạng thái mắt và các giá trị nhập được giữ khi Activity tái tạo. Nút pill xanh có loading. Dòng Đăng nhập quay về Login có sẵn, không đẩy thêm Login lên back stack khi đến từ Login.
- `ScrollView`, `adjustResize`, xử lý system bar/cutout/IME insets cho bàn phím và màn thấp. Logo co theo chiều ngang trên màn hẹp, trang trí nằm sau form. Lỗi validation hiển thị ngay dưới trường với viền đỏ; thay đổi nội dung xóa lỗi cũ.
- `RegisterViewModel` cho phép bỏ trống `fullName`, kiểm tra username/email/mật khẩu/khớp xác nhận theo contract, không gửi `confirmPassword`, chặn request trùng khi loading. `AuthRepository` dùng Retrofit thật; HTTP 409 phân biệt email và username theo mã lỗi Backend, HTTP 400/502/503/504 và lỗi mạng có thông báo tiếng Việt. Không sửa Backend, PostgreSQL, API contract, Login, JWT hoặc các module M1–M10.

## 3. File thêm và sửa

| Loại | File |
|---|---|
| Sửa | `android-app/app/src/main/java/com/example/bivoca/ui/auth/RegisterActivity.java`, `RegisterViewModel.java` |
| Sửa | `android-app/app/src/main/java/com/example/bivoca/data/repository/AuthRepository.java` |
| Sửa | `android-app/app/src/main/res/layout/activity_register.xml`, `android-app/app/src/main/res/values/strings.xml`, `android-app/app/src/main/AndroidManifest.xml` |
| Thêm | `android-app/app/src/main/res/drawable/register_mail.xml`, `register_field_error.xml` |
| Thêm test | `android-app/app/src/test/java/com/example/bivoca/ui/auth/RegisterViewModelValidationTest.java`, `RegisterViewModelStateTest.java`, `android-app/app/src/test/java/com/example/bivoca/data/repository/AuthRepositoryRegistrationErrorTest.java` |
| Thêm/cập nhật tài liệu | `M11_REGISTER_REFERENCE.png`, `M11_REGISTER_UI_REPORT.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, `M11_LOGIN_UI_REPORT.md` |

## 4. Kiểm thử và bằng chứng Emulator

| Lệnh / hạng mục | Kết quả |
|---|---|
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain` | **BUILD SUCCESSFUL**; từng task assemble, unit test, lint PASS |
| Unit tests Android | **73/73 PASS**, 0 failure/error/skipped; trong đó 13 test mới cho validation, state/loading/chống gửi trùng, callback API, 409, mạng và lỗi server |
| Lint Debug | 0 lỗi, 186 cảnh báo toàn app |
| `adb devices -l` | **PASS:** `emulator-5554 device`, Pixel 6, API 35, Google APIs x86_64, 1080×2400 |
| Kết nối Backend | **PASS:** health `ok`/database `connected`; `adb reverse --list` có `tcp:3000 tcp:3000` |
| Login → Register → Login | **PASS:** mở đúng Activity; link về Login không tạo task trùng |
| UI Register | **PASS:** đối chiếu ảnh mẫu trên Emulator; bố cục dọc đầy đủ, theme Login giữ nguyên |
| IME và cuộn | **PASS:** focus username mở bàn phím; nút Đăng ký vẫn truy cập được phía trên IME |
| Validation | **PASS:** submit trống hiện viền lỗi và `Vui lòng nhập tên đăng nhập.` đúng vị trí |
| Nút mắt | **PASS:** cả hai nút đổi `Hiện mật khẩu` ↔ `Ẩn mật khẩu`, không xóa nội dung |
| Xoay màn hình | **PASS:** Activity không crash, dữ liệu trường đang nhập được giữ, form ngang cuộn được; đã trả về dọc |
| Screenshot thực tế | `android-app/app/build/reports/m11-emulator/01-login.png` đến `05-register-landscape.png` |
| Đăng ký API thật | Không gọi tạo tài khoản trên DB vận hành. `AuthRepository`/Retrofit thật và validation/loading/chống request trùng/error mapping được kiểm bằng unit tests. |

Nghiệm thu chi tiết và lệnh đã chạy nằm trong `M11_EMULATOR_ACCEPTANCE_REPORT.md`. Không phát hiện lỗi code nên lượt nghiệm thu chỉ bổ sung ảnh và tài liệu. Kiểm thử tạo tài khoản thành công vẫn cần DB test riêng được phép; đây là giới hạn môi trường, không thay bằng mock hoặc thông báo thành công giả.

## 5. So với Login và điểm còn khác

Màu nền, logo, trăng/sao, ô nhập, viền focus, icon mắt, nút chính và thanh hệ thống dùng chung tài nguyên Login. Register có form dài hơn nên giảm khoảng trên và cho cuộn. Screenshot Emulator đã được so sánh với `M11_REGISTER_REFERENCE.png`; khác biệt hợp lý là tỷ lệ theo màn Pixel 6 và vùng system bar/IME thực tế. M11 Register UI được đánh dấu DONE trên Emulator; Forgot Password API/email vẫn là phần phát triển sau.
