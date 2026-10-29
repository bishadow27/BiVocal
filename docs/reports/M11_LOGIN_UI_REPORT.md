# M11 — Thiết kế lại màn hình đăng nhập BiVocal

**Cập nhật sau M11:** Liên kết `Quên mật khẩu?` nay mở màn hình mới thay cho Toast được mô tả trong báo cáo lịch sử này. Register cũng đã được thiết kế lại sau lượt Login và đang chờ nghiệm thu thiết bị. Xem `M11_FORGOT_PASSWORD_REPORT.md` và `M11_REGISTER_UI_REPORT.md` để biết trạng thái hiện tại.

**Ngày:** 2026-10-14  
**Trạng thái:** **DONE trên OPPO thật**. Đã đối chiếu ảnh mẫu được cung cấp sau lượt triển khai đầu với ảnh chạy thật, sửa logo/nền/trăng/sao, build, 60 unit tests và lint PASS. Đăng nhập sai qua Backend thật báo lỗi đúng; người dùng tự đăng nhập và xác nhận đã thấy Trang chủ rồi chủ động Đăng xuất/quay lại. Không có AVD cấu hình nên chưa chạy Android Emulator; không coi đây là kết quả Emulator.

## 1. Phạm vi và source đã đối chiếu

Đã đọc `AGENTS.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, `PROJECT_CONTEXT.md`, `M10_SPACED_REPETITION_REPORT.md`; `LoginActivity`, `LoginViewModel`, layout Login, `RegisterActivity`, `AuthRepository`, `AuthApi`, model xác thực, theme/colors/drawable, Gradle và Backend Auth route/schema. Backend chỉ nhận `POST /api/v1/auth/login` với `email/password`; không có API quên/đặt lại mật khẩu. Do đó nhãn Login là **Email**, khác nhãn “Tên đăng nhập” trong ảnh theo quy tắc contract của prompt M11. Không sửa Backend, database, API contract, Register business logic, JWT, SessionManager, AuthSessionExpiry hoặc các màn hình Android khác.

## 2. Giao diện và hành vi đã triển khai

- Nền gradient navy gần đen bằng XML, trăng khuyết vàng nhạt và năm điểm sao xanh trắng nhỏ; không dùng ảnh chụp màn hình làm nền. Chỉ theme của `LoginActivity` đổi màu status/navigation bar; màn hình khác dùng theme hiện tại.
- Ảnh mẫu đã duyệt được lưu tại `M11_LOGIN_REFERENCE.png` để bàn giao. File logo gốc khác trên máy chỉ có bản icon vuông `C:\Users\phuc\Downloads\BiVocal_logo_512.png`; theo lựa chọn của người dùng, đã tách **đúng vùng logo ngang từ ảnh mẫu** thành `drawable-nodpi/bivocal_wordmark.png` nền trong suốt. Không dùng hai ảnh dựng thử bằng AI vì mascot/chữ bị sai nét. Header dùng asset đã tách, đường chia dọc xám xanh và tiêu đề `ĐĂNG NHẬP` theo bố cục mẫu.
- Sau khi có ảnh mẫu, chỉnh nền navy gần đen, thêm glow nhẹ cho trăng, thu nhỏ/đặt lại sao và căn lại kích thước, khoảng cách của logo/tiêu đề. Không dùng ảnh mẫu làm background hoặc điền sẵn tài khoản/mật khẩu trong app.
- Hai ô nền xanh đen, viền xám xanh 12dp, icon người/khóa và viền cyan khi focus; chữ trắng, nhãn có dấu sao đỏ. Mật khẩu có nút mắt vùng chạm 48dp, chuyển hiện/ẩn mà vẫn giữ nội dung và vị trí con trỏ; trạng thái mắt được giữ khi Activity tái tạo.
- Liên kết **Quên mật khẩu?** có thể nhấn và hiện thông báo trung thực “Chức năng quên mật khẩu hiện chưa được hỗ trợ.” Không tạo email hay luồng reset giả.
- Nút xanh `#1677FF` cao 56dp bo pill, có trạng thái nhấn/loading và chặn gửi trùng. `LoginViewModel` kiểm tra email rỗng/sai định dạng và mật khẩu rỗng trước khi gọi `AuthRepository`/Backend thật; thành công tiếp tục lưu phiên và mở Home như trước, lỗi giữ thông báo hiện có. Dòng đăng ký chia màu xám trắng/cyan và mở `RegisterActivity` hiện có.
- `ScrollView` có `fillViewport` cho màn nhỏ, root xử lý system bars/display cutout/IME insets, Manifest giữ `adjustResize`. Không hardcode tài khoản, mật khẩu hoặc footer quảng cáo.

## 3. File tạo/sửa

- Sửa `android-app/app/src/main/java/com/example/bivoca/ui/auth/{LoginActivity,LoginViewModel}.java`, `android-app/app/src/main/res/layout/activity_login.xml`, `android-app/app/src/main/res/values/{strings,themes}.xml`, `android-app/app/src/main/AndroidManifest.xml`.
- Thêm `android-app/app/src/main/res/drawable-nodpi/bivocal_wordmark.png`; drawable XML `login_night_background`, `login_field_background`, `login_moon`, `login_moon_glow`, `login_star`, `login_user`, `login_lock`, `login_eye`, `login_eye_off`; color state list `res/color/login_button_tint.xml`; theme API 27+ `res/values-v27/login_theme.xml`.
- Tài liệu và ảnh tham chiếu: `M11_LOGIN_UI_REPORT.md`, `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, `M11_LOGIN_REFERENCE.png`. Không sửa `PROJECT_CONTEXT.md` vì kiến trúc không đổi.

## 4. Kiểm thử thực tế đến thời điểm này

| Lệnh/bước | Kết quả |
|---|---|
| `.\gradlew.bat :app:assembleDebug` | PASS sau khi cập nhật resource; APK debug tạo thành công |
| `.\gradlew.bat :app:testDebugUnitTest` | **60/60 PASS**, 0 FAIL/ERROR/SKIPPED |
| `.\gradlew.bat :app:lintDebug` | PASS, **0 lỗi/195 cảnh báo** ở lượt kiểm tra sau sửa API 27; các cảnh báo còn lại không chặn build |
| `adb devices -l`; `adb install -r ...app-debug.apk` | OPPO CPH2557 ở trạng thái device; cài `Success`, giữ dữ liệu app |
| Android Emulator | `emulator -list-avds` không liệt kê AVD; không chạy hoặc chụp ảnh Emulator |
| OPPO Login dọc 1080×2400 | Mở được `LoginActivity`; logo gốc, nền trăng/sao, hai trường, nút Login và Register nằm trọn màn hình. Ảnh: `android-app/app/build/reports/m11-login/oppo-login.png` |
| OPPO form/điều hướng | Email trống báo `Vui lòng nhập email.`; chỉ có email báo `Vui lòng nhập mật khẩu.`; email sai định dạng và mật khẩu mẫu báo `Email không hợp lệ.`. Nút mắt đổi content description `Hiện mật khẩu` ↔ `Ẩn mật khẩu` và cờ password của ô nhập tương ứng. Register mở `RegisterActivity`, Back trở về Login. |
| OPPO Forgot/IME/xoay | Chạm Forgot hiện Toast `Chức năng quên mật khẩu hiện chưa được hỗ trợ.` (ảnh `oppo-forgot.png`). Chạm Email mở bàn phím. Xoay ngang khi Login đang mở không crash; sau khi ẩn bàn phím có thể cuộn tới ô mật khẩu, nút Login và Register (ảnh `oppo-login-landscape-no-keyboard.png`, `oppo-login-landscape-bottom.png`). Đã khôi phục thiết lập xoay ban đầu. |
| OPPO bản cuối đối chiếu ảnh mẫu | Cài lại APK bằng `adb install -r` trả `Success`; ảnh `android-app/app/build/reports/m11-login/oppo-login-final.png` hiển thị logo ngang đúng mascot/chữ, thanh chia, tiêu đề, nền tối, trăng/sao, hai trường và nút như bố cục mẫu; không cắt nội dung trên OPPO 1080×2400. |
| OPPO nhập và đăng nhập | Dùng email/mật khẩu thử không tồn tại, Backend thật trả `Email hoặc mật khẩu không đúng.` trên Login. Bấm mắt giữ đúng nội dung đang nhập; xoay ngang vẫn giữ email, mật khẩu và trạng thái mắt. Sau đó đóng/mở lại app để xóa dữ liệu thử khỏi form. Người dùng tự nhập tài khoản thật và xác nhận vào Trang chủ, rồi chủ động Đăng xuất/quay lại Login; không đọc, ghi log hoặc yêu cầu mật khẩu thật. |

Gradle sandbox ban đầu không được tải distribution 9.5.0; chạy lại với JDK Android Studio và quyền tải dependency thì build/test/lint được. Lint đầu tiên phát hiện `windowLightNavigationBar` cần API 27 trong theme minSdk24; đã chuyển thuộc tính này sang `values-v27`, lint tiếp theo PASS.

Ban đầu phiên có sẵn tự điều hướng Login về Home theo logic cũ. Sau khi người dùng tự đăng xuất, đã kiểm tra được Login trên OPPO như bảng trên. Ảnh mẫu tới sau; bản cuối đã đối chiếu trực quan trên OPPO với `M11_LOGIN_REFERENCE.png`. Hai khác biệt có chủ ý: nhãn **Email** thay `Tên đăng nhập` vì Backend nhận email/password; các ô trống thay dữ liệu minh họa trong mockup. Các sai khác nhỏ về độ sáng glow/vị trí điểm sao và khoảng trống dọc tùy tỷ lệ màn hình không cản trở thao tác. Người dùng xác nhận luồng vào Trang chủ; vì họ chủ động rời Home trước lúc ADB đọc trạng thái, phần xác nhận đó là kiểm thử thủ công do người dùng thực hiện, không có ảnh ADB Home tại đúng thời điểm.

## 5. Giới hạn nghiệm thu

- Android Emulator có binary và system image nhưng `emulator -list-avds` trả danh sách rỗng; chưa tạo/chạy AVD. Ảnh và tương tác được kiểm trên OPPO CPH2557 Android 15, không phải Emulator.
- Ảnh logo ngang là cutout từ ảnh mẫu 992×1586, không phải file logo ngang độ phân giải gốc. Nếu có asset brand gốc ở lần sau, có thể thay đúng một file PNG mà không đổi layout/logic.
- Chưa có ảnh chụp Home bằng ADB trong thời điểm người dùng đăng nhập; kết quả vào Home do chính người dùng xác nhận trực tiếp. Các kiểm tra ADB độc lập còn lại ở bảng trên.
