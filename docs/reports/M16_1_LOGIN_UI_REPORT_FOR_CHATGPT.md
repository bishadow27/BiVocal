# BiVocal — Bàn giao M16.1 Login UI cho ChatGPT

Ngày cập nhật: 25/10/2026.

Đây là bản tổng hợp trạng thái cuối của M16.1, sau các lần người dùng chỉnh header, slogan, label, khoảng cách và display Emulator. Các thông số dưới đây thay thế thông số của những bản Login trước đó.

## 1. Bối cảnh dự án

- Project: BiVocal/BiVoca, ứng dụng Android học từ vựng tiếng Anh phục vụ đồ án.
- Thư mục: C:\Users\phuc\OneDrive\Documents\New project.
- Android: Java + XML, MVVM, ViewModel/LiveData, Repository, Retrofit.
- Backend: Express/TypeScript → PostgreSQL; Android chỉ giao tiếp qua REST API /api/v1/.
- Admin Web: React/Vite.
- Theo kết quả M15.7 trước đó: production có 20.000 Flashcard, 38 topic trong DB và 36 nhóm hiển thị; General Vocabulary bằng 0, còn 12.432 thẻ trong nhóm POS. M16.1 không truy vấn lại để chứng nhận các số liệu này và không thay đổi dữ liệu.
- Runtime học từ là online-only; không đưa Room/Offline trở lại.

M16.1 hiện chỉ triển khai giao diện Login. Register và Forgot Password vẫn giữ giao diện cũ. Chưa tự chuyển sang màn hình tiếp theo.

## 2. Giao diện Login hiện tại

Phong cách: Minimalist Dark Mode. Đã bỏ trăng/sao của màn Login cũ theo ảnh tham chiếu mới.

| Thành phần | Cấu hình cuối |
|---|---|
| Nền | #090D16 |
| Nền input | #111726 |
| Viền input/nút Google | #334155 |
| Cyan chính | #38BDF8 |
| Blue chính | #2563EB |
| Chữ chính | #F8FAFC |
| Chữ phụ | #94A3B8 |
| Divider header | #526782 |
| Font | Roboto/sans-serif hệ thống |
| Padding nội dung | Ngang 24dp, dọc 24dp |
| Bo góc | 12dp |

### Header

- Bên trái: mascot chính thức của BiVocal, khung 48 × 48dp; cạnh đó là chữ BiVocal 26sp bold, cyan.
- Mascot được lấy từ vùng trái của asset bivocal_wordmark.png; không vẽ mascot mới và không thay asset của màn khác.
- Bên phải: divider 1,5 × 32dp, margin ngang 16dp, sau đó ĐĂNG NHẬP 20sp bold.
- Không có tiêu đề Đăng nhập lặp lại phía dưới.
- Slogan “Học từ vựng mỗi ngày”: 13sp, gạch chân, màu cyan nhạt #7DD3FC.
- Mascot + BiVocal và slogan nằm trong một cột riêng. Slogan căn giữa theo cột thương hiệu, cách hàng logo 6dp; không kéo sang phần divider/ĐĂNG NHẬP.

### Form và liên kết

Thứ tự cuối:

1. Label “Tài khoản”, 14sp medium.
2. Ô “Nhập email”, cao 52dp, icon thư 20dp, bàn phím email và autofill email.
3. Label “Mật khẩu”, 14sp medium.
4. Ô “Nhập mật khẩu”, cao 52dp, icon khóa và nút mắt hiện/ẩn với vùng chạm 48dp.
5. Liên kết “Quên mật khẩu?” nằm DƯỚI ô mật khẩu, căn phải, margin trên 6dp. Không còn nằm ngang hàng label Mật khẩu.
6. Vùng lỗi validation/API chỉ hiện khi có lỗi.
7. Nút “Đăng nhập”, gradient cyan → blue, có loading/disabled/pressed.
8. Divider “Hoặc tiếp tục với”.
9. Nút “Tiếp tục với Google”, icon G bốn màu.
10. Footer “Chưa có tài khoản? Đăng ký ngay”, liên kết cyan.

Khoảng cách: header/slogan → form 24dp; label → input 6dp; email → label mật khẩu 16dp; khối liên kết/lỗi → nút Login 20dp; Login → divider 20dp; divider → Google 16dp; Google → footer 32dp.

### Bố cục dọc

- Toàn bộ nội dung nằm trong một LinearLayout dọc, căn giữa theo chiều dọc.
- ScrollView fillViewport=true; không dùng spacer giãn hoặc bottom constraint ép footer xuống đáy.
- Khi thiếu chiều cao, nội dung có thể cuộn.
- Giữ WindowInsets xử lý system bars/display cutout/IME và adjustResize hiện có.
- Dùng dp/sp; không cố định tọa độ Y theo một độ phân giải.

## 3. Emulator đã được chỉnh

- AVD: BiVoca_API35_Pixel6.
- Serial: emulator-5554, Android API 35.
- Ban đầu: màn hình 1080 × 2400, density 420. Tỷ lệ dài làm Login có nhiều khoảng trống trên/dưới.
- Thử wm size override 1080 × 1920 tạo letterbox vì khung Emulator vẫn theo tỷ lệ Pixel 6 cũ.
- Đã bỏ override, chỉnh cấu hình native AVD và cold boot:
  - hw.lcd.width=1080
  - hw.lcd.height=1920
  - hw.lcd.density=420
  - skin.name=1080x1920
  - showDeviceFrame=no
- Kiểm tra lại trong lượt tạo báo cáo: adb trả Physical size: 1080x1920, Physical density: 420.
- Cấu hình nằm ngoài repository: C:\Users\phuc\.android\avd\BiVoca_API35_Pixel6.avd\config.ini.
- Có backup cùng thư mục: config-before-compact-display-20261025.ini.

Lưu ý: đây là cấu hình thiết bị thử nghiệm, không phải lý do giới hạn app chỉ chạy ở 1080 × 1920. Các màn nhỏ/font lớn vẫn cần kiểm thử responsive.

## 4. File source thay đổi trong M16.1

Các đường dẫn dưới đây tính từ thư mục gốc project:

| File | Nội dung |
|---|---|
| android-app/app/src/main/res/layout/activity_login.xml | Layout Login, header/slogan/labels, spacing, vị trí Forgot |
| android-app/app/src/main/res/values/login_ui.xml | Màu/theme riêng Login, slogan có underline, footer |
| android-app/app/src/main/res/drawable/login_ui_field.xml | Viền/nền input, trạng thái focus |
| android-app/app/src/main/res/drawable/login_ui_primary.xml | Gradient, disabled và ripple nút Login |
| android-app/app/src/main/res/drawable/login_google_g.xml | Icon Google bốn màu |
| android-app/app/src/main/res/values/strings.xml | Chữ divider/footer |
| android-app/app/src/main/AndroidManifest.xml | Gán theme riêng cho LoginActivity |
| android-app/app/src/main/java/com/example/bivoca/ui/auth/LoginActivity.java | Lấy mascot từ bitmap logo gốc để hiển thị |

Các lần refinement cuối chỉ đổi activity_login.xml và login_ui.xml.

Không sửa LoginViewModel, AuthRepository, AuthApi, SessionManager, Credential Manager, JWT, Register/Forgot API, Backend, Admin Web hoặc PostgreSQL. Listener và ID view được giữ để sử dụng nghiệp vụ cũ.

## 5. Build, cài đặt và kiểm thử thực tế

Lệnh dùng tại android-app, với JAVA_HOME trỏ Android Studio jbr:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

| Mốc | Kết quả thực tế |
|---|---|
| Bản Header + Labels | Build PASS; 76/76 unit tests PASS, 0 failures/errors |
| Lint bản Header + Labels | 0 errors, 292 warnings toàn app |
| Chuyển Forgot xuống dưới input | assembleDebug PASS; cài -r và kiểm tra bounds PASS |
| Bản slogan gạch chân/căn trái theo nhóm thương hiệu mới nhất | assembleDebug PASS trong 4 giây; cài -r thành công |

Sau thay đổi vị trí Forgot và slogan, chưa chạy lại toàn bộ unit/lint; kết quả 76/76 và 292 warnings thuộc mốc Header + Labels trước các chỉnh XML nhỏ này.

APK Debug: android-app/app/build/outputs/apk/debug/app-debug.apk, package com.example.bivoca.

Đã dùng adb install -r và mở lại Login; không uninstall, không clear data. Một lần người dùng vẫn thấy bản cũ vì trước đó chỉ build; sau phản hồi đã cài APK mới. Bản slogan cuối cũng đã cài lên Emulator.

### Runtime có bằng chứng

- Header + Labels: UI dump có cả “Tài khoản” và “Học từ vựng mỗi ngày”.
- Forgot: bottom input 1164px, top liên kết 1180px ở density 420, tương đương margin khoảng 6dp.
- Slogan cuối: vùng x=63..441px, heading ĐĂNG NHẬP bắt đầu x=529px; hai vùng tách biệt.
- Ở bản spacing trước đó, đã kiểm tra landscape cuộn đến Google/footer và font scale 1,3.
- Ở bản M16.1 trước các refinement cuối, đã kiểm tra mở Register, mở Forgot và hiện/ẩn password giữ giá trị nhập.
- Chưa nghiệm thu lại toàn bộ các thao tác trên cho bản slogan cuối; không suy rộng kết quả cũ thành E2E mới.
- Bàn phím mềm trên Emulator từng hiển thị không ổn định; bài kiểm tra docked IME cuối chưa được xác nhận PASS.
- Không thực hiện đăng nhập thử, reset password hoặc thay đổi tài khoản production để kiểm tra UI.

Bằng chứng trong android-app/runtime-evidence/m16-1-login/:

- header-labels-installed.xml
- forgot-position.xml
- slogan-alignment.xml
- 08-login-spacing-fixed.png, 10-spacing-landscape-scrolled.png, 11-spacing-font-130.png: ảnh THỰC nhưng thuộc phiên bản spacing cũ, trước Header + Labels/slogan cuối.

Người dùng đã yêu cầu không cần chụp thêm, nên không có ảnh tự chụp của bản slogan cuối. Không dùng các ảnh cũ để tuyên bố chúng là giao diện mới nhất.

## 6. Trạng thái bàn giao

Giao diện Login đã chỉnh theo phản hồi cuối và APK đã cài trên Emulator. Chờ người dùng xem trực tiếp và duyệt.

Register/Forgot chưa được thiết kế lại theo Design System mới. Chỉ thực hiện khi người dùng yêu cầu riêng; giữ nguyên backend, auth và dữ liệu.

Khi tiếp tục, đọc AGENTS.md, WORK_STATUS.md và ANDROID_CONTEXT.md. Tài liệu milestone đã được gom vào docs/reports/; README.md ở gốc là mục lục.

Báo cáo này chỉ tổng hợp trạng thái để ChatGPT tư vấn bước tiếp theo; không chứa mật khẩu, JWT, SMTP secret hoặc dữ liệu cá nhân trong database.
