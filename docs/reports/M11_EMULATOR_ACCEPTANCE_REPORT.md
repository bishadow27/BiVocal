# M11 — Báo cáo nghiệm thu Android Emulator

**Ngày:** 2026-10-15  
**Kết quả:** **PASS cho Login UI và Register UI trên Emulator.** Chức năng đăng ký thành công end-to-end không được gọi vì môi trường hiện tại dùng PostgreSQL vận hành và nhiệm vụ không cho phép tạo tài khoản thử trên DB này.

## 1. Môi trường nghiệm thu

- AVD: `BiVoca_API35_Pixel6`.
- Thiết bị mô phỏng: Pixel 6, Android API 35, Google APIs, ABI `x86_64`.
- Màn hình: 1080 × 2400; RAM AVD: 2048 MB; `hw.keyboard=yes` để nhận bàn phím máy tính.
- Serial ADB: `emulator-5554`.
- Backend: `http://127.0.0.1:3000/api/v1/health` trả `status=ok`, `database=connected`.
- Vì Android dùng `http://127.0.0.1:3000`, Emulator được nối tới Backend máy Windows bằng `adb -s emulator-5554 reverse tcp:3000 tcp:3000`.

`adb reverse` chỉ tồn tại trong phiên Emulator hiện tại. Sau khi tắt/mở lại máy ảo, phải chạy lại lệnh này trước khi đăng nhập hoặc gọi API.

Ngày 2026-10-15, AVD ban đầu có `hw.keyboard=no` nên không nhận phím từ máy tính. Đã đổi thành `hw.keyboard=yes`, cold boot AVD, xác minh Android nhận `AT Translated Set 2 keyboard` với `KeyboardType: 2`, giữ bàn phím ảo bằng `show_ime_with_hard_keyboard=1`, rồi khôi phục reverse cổng 3000.

## 2. Lệnh đã chạy

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse tcp:3000 tcp:3000
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 reverse --list
Invoke-RestMethod http://127.0.0.1:3000/api/v1/health

cd "C:\Users\phuc\OneDrive\Documents\New project\android-app"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain
```

ADB cũng được dùng để mở app, dump UI hierarchy, nhập dữ liệu tổng hợp, chạm nút, xoay bằng `settings put system user_rotation`, chụp bằng `screencap` và kéo ảnh về bằng `adb pull`. Xoay màn hình được trả về dọc sau kiểm thử.

## 3. Kết quả từng bước

| Hạng mục | Kết quả | Bằng chứng |
|---|---|---|
| Emulator xuất hiện trong ADB | PASS | `emulator-5554 device`, API 35, `x86_64` |
| Backend local | PASS | health trả `ok`, database `connected` |
| ADB reverse cổng 3000 | PASS | `host-18 tcp:3000 tcp:3000` |
| Login Dark Night Sky | PASS | Logo, tiêu đề, hai trường, mắt mật khẩu, Forgot, nút Login và Register hiển thị đúng ở 1080 × 2400 |
| Login → Register | PASS | `topResumedActivity` chuyển từ `LoginActivity` sang `RegisterActivity` |
| Register theo ảnh mẫu | PASS | Logo/trăng/sao, năm trường, nút và liên kết Login đúng bố cục; không vỡ ở dọc |
| Bàn phím | PASS | Khi nhập username, form tự cuộn và nút Đăng ký vẫn ở phía trên IME |
| Validation | PASS | Gửi form trống đặt viền lỗi và hiện `Vui lòng nhập tên đăng nhập.` ngay dưới trường |
| Hai nút mắt | PASS | `content-desc` của cả hai nút đổi từ `Hiện mật khẩu` sang `Ẩn mật khẩu`; dữ liệu trường không bị xóa |
| Xoay màn hình | PASS | `RegisterActivity` vẫn là Activity trước/sau xoay; dữ liệu trường đang hiện được giữ; form cuộn được ở ngang và không crash |
| Register → Login | PASS | Link `Đăng nhập` trở lại đúng `LoginActivity`, không tạo màn Login trùng |
| Đăng ký API thành công | KHÔNG CHẠY | Không tạo tài khoản test trên DB vận hành. Retrofit thật và state/error/loading/chống gửi trùng được kiểm bằng unit test |

Một chuỗi phím ADB thử Login bằng dữ liệu tổng hợp đã đóng Activity do phím Back được gửi sau khi IME không còn mở. Logcat không có `FATAL EXCEPTION`; kết quả đó bị loại và không được tính là bằng chứng chức năng.

## 4. Build và test

- `:app:assembleDebug`: **PASS**.
- `:app:testDebugUnitTest`: **73/73 PASS**, 0 failure, 0 error, 0 skipped, 13 test suites.
- `:app:lintDebug`: **PASS**, 0 error, 186 warning toàn ứng dụng.
- APK: `android-app/app/build/outputs/apk/debug/app-debug.apk`.

## 5. Ảnh thực tế

Các ảnh đều được chụp từ `emulator-5554`, không phải mockup:

- `android-app/app/build/reports/m11-emulator/01-login.png` — Login dọc.
- `android-app/app/build/reports/m11-emulator/02-register.png` — Register dọc.
- `android-app/app/build/reports/m11-emulator/03-register-keyboard.png` — form khi IME mở.
- `android-app/app/build/reports/m11-emulator/04-register-validation.png` — lỗi validation tại trường.
- `android-app/app/build/reports/m11-emulator/05-register-landscape.png` — form ngang và khả năng cuộn.

## 6. File thay đổi trong lượt nghiệm thu

- Thêm `M11_EMULATOR_ACCEPTANCE_REPORT.md`.
- Cập nhật `M11_REGISTER_UI_REPORT.md`.
- Cập nhật `WORK_STATUS.md`.
- Cập nhật `ANDROID_CONTEXT.md`.
- Thêm ảnh nghiệm thu trong `android-app/app/build/reports/m11-emulator/`.

Không sửa source Android, Backend, PostgreSQL, migration, dataset hoặc Admin Web trong lượt nghiệm thu này. Không phát hiện lỗi code cần sửa tối thiểu.

## 7. Kết luận và giới hạn

M11 Login/Register UI đạt nghiệm thu trên Emulator API 35. Điều hướng, validation, bàn phím, mắt mật khẩu, xoay và quay lại Login đều PASS. Phần tạo tài khoản thật vẫn giữ giới hạn an toàn: chỉ nên kiểm end-to-end khi có DB test riêng được phép; không dùng DB vận hành để tạo tài khoản thử. Forgot Password vẫn chỉ có UI vì Backend chưa có reset-password API/email service, đúng quyết định trước đó của người dùng.
