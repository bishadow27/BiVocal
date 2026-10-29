# M16.2 — Register & Forgot Password Frontend

Ngày: 25/10/2026. Trạng thái: triển khai và kiểm thử kỹ thuật PASS; đã cài APK trên Emulator, chờ người dùng duyệt giao diện.

## Điều chỉnh tiêu đề sau nghiệm thu

Theo phản hồi mới: Register dùng “TẠO TÀI KHOẢN”; Forgot đổi chỗ hai tiêu đề, header là “QUÊN MẬT KHẨU?”, phần dưới icon là “Khôi phục mật khẩu”. Chỉ đổi auth_ui.xml và text reference trong activity_forgot_password.xml. assembleDebug PASS (11 giây), cài -r giữ dữ liệu; UI dumps titles-forgot.xml/titles-register.xml xác nhận đủ ba nhãn. Không chạy lại unit/lint/instrumentation cho chỉnh text này; kết quả và ảnh bên dưới thuộc bản trước đổi nhãn.

## Kết quả

Đã thay giao diện Đăng ký và Quên mật khẩu bằng Minimalist Dark Mode đồng bộ Login M16.1. Hai màn dùng Activity/ViewModel và API hiện có. Không thiết kế lại Login.

- Register giữ đúng 5 trường: họ tên không bắt buộc, username, email, password, confirm password.
- Quên mật khẩu có khóa phẳng 64dp, hướng dẫn, email, nút gửi, Google callout và nút quay lại Login.
- Cả hai có Back riêng phía trên, thương hiệu/slogan và tiêu đề tự co chữ 12–20sp trong tối đa hai dòng.
- Không còn trăng/sao/nền trang trí. Không thêm Google Sign-In vào Register.
- Nội dung nằm trong ScrollView; không neo footer xuống đáy, không gán tọa độ tuyệt đối.

## Tài nguyên dùng lại và thông số

| Thành phần | Áp dụng |
|---|---|
| Nền/input/border | #090D16 / #111726 / #334155 |
| Cyan/blue | #38BDF8 / #2563EB |
| Text chính/phụ | #F8FAFC / #94A3B8 |
| Slogan | Resource gạch chân của Login, 13sp, #7DD3FC |
| Mascot/brand | Crop từ bivocal_wordmark.png giống Login; 48dp, BiVocal 26sp bold |
| Divider | 1,5 × 32dp, #526782, margin hai bên 16dp |
| Input | 52dp, bo 12dp, padding 16dp, icon 20dp, text 16sp |
| Label/helper | 14sp medium / 12sp secondary |
| Button | Min-height 52dp, text 16sp medium; cho phép tăng cao khi chữ lớn cần xuống dòng |
| Grid/padding | Padding ngang/dọc 24dp; label → input 6dp; field spacing 12dp |
| Callout | #102033, viền #334155 1dp, radius 12dp, padding 16dp |

Tái sử dụng nguyên trạng login_ui_field.xml, login_ui_primary.xml, login_ui.xml, login_slogan, login_user/register_mail/login_lock/login_eye và theme Theme.BiVoca.Login.Reference. Không thêm thư viện.

auth_ui.xml định nghĩa style layout để hai màn dùng cùng kích thước/typography; các style trỏ về màu và drawable của M16.1. register_field_error đổi nền sang login_ui_input; màu lỗi và validation cũ giữ nguyên.

Header gồm hàng Back riêng rồi hàng thương hiệu + divider + screen title. Cột thương hiệu dùng ConstraintLayout để chiều cao slogan được đo lại đúng khi xuống dòng ở font scale 1.3. Slogan không tràn sang tiêu đề.

Register: header → form 24dp, label → field 6dp, field spacing 12dp, form → button 24dp, button → footer 24dp. Helper username phản ánh regex hiện có [A-Za-z0-9_]{3,30}; không thêm quy tắc validation.

Forgot: header → khóa 24dp, khóa → heading 16dp, heading → mô tả 8dp, mô tả → label email 24dp, input → nút gửi 16dp, nút gửi → callout 16dp, callout → nút quay lại 24dp.

## File source thêm/chỉnh sửa

Đường dẫn dưới đây tính từ thư mục gốc project:

| File | Loại thay đổi |
|---|---|
| android-app/app/src/main/res/layout/activity_register.xml | Thay UI, giữ ID view hiện có |
| android-app/app/src/main/res/layout/activity_forgot_password.xml | Thay UI, giữ ID view hiện có |
| android-app/app/src/main/res/layout/view_auth_brand_header.xml | Mới, header dùng chung hai màn |
| android-app/app/src/main/res/values/auth_ui.xml | Mới, styles và text UI |
| android-app/app/src/main/res/drawable/auth_arrow_back.xml | Mới, icon Back |
| android-app/app/src/main/res/drawable/auth_security_lock.xml | Mới, khóa phẳng |
| android-app/app/src/main/res/drawable/auth_info.xml | Mới, icon thông tin |
| android-app/app/src/main/res/drawable/auth_google_callout.xml | Mới, nền/viền callout |
| android-app/app/src/main/res/drawable/register_field_error.xml | Nền lỗi đồng bộ input mới |
| android-app/app/src/main/java/com/example/bivoca/ui/auth/RegisterActivity.java | Bind mascot/header/Back, Button mới, text loading và drawable focus |
| android-app/app/src/main/java/com/example/bivoca/ui/auth/ForgotPasswordActivity.java | Bind mascot/header/Back, Button mới và progress indicator |
| android-app/app/src/main/AndroidManifest.xml | Hai màn dùng theme M16.1 hiện có |
| android-app/app/src/androidTest/java/com/example/bivoca/AuthScreensUiTest.java | Mới, 5 bài kiểm thử UI |

Tài liệu cập nhật: báo cáo này, WORK_STATUS.md, ANDROID_CONTEXT.md và mục lục README.md. Bằng chứng nằm trong android-app/runtime-evidence/m16-2-auth/.

## Build và test

Lệnh chạy tại android-app, JAVA_HOME là Android Studio jbr:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Lần cuối: BUILD SUCCESSFUL trong 25 giây. Unit test task đã chạy, không chỉ dùng kết quả cũ.

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug | PASS |
| testDebugUnitTest | 76/76 PASS, 0 failures/errors |
| lintDebug | PASS, 0 errors; 322 warnings toàn app |
| assembleDebugAndroidTest | PASS |
| Instrumentation M16.2 trên Emulator | 5/5 PASS, 0 skip, 0 failure; 17,751 giây |
| adb install -r APK Debug/test APK | Success; không uninstall/clear data |

Lệnh instrumentation:

```powershell
adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.AuthScreensUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
```

Kết quả đầy đủ: instrumentation.txt trong thư mục bằng chứng.

Năm test:

1. Register cuộn tới confirm/button/footer với IME thực sự báo visible.
2. Login → Register → Back → Login → Forgot → nút quay lại Login.
3. Validation username sai, hai nút mắt, giữ đủ 5 giá trị form và trạng thái mắt sau Activity recreation.
4. Forgot chặn email trống/sai định dạng ngay trên client; Top Back đóng màn.
5. Đo XML Android thật cho cả hai màn ở 4 viewport × 2 font scale; kiểm tra title/slogan không cắt và footer cuộn tới được.

Trong các lần chạy đầu, test gặp popup Password Manager, thao tác khi IME/transition chưa ổn định và kiểm tra state đóng Activity quá sớm. Đã sửa bộ test để hủy Autofill session của dữ liệu thử, đóng IME/chờ chuyển màn theo trạng thái và chờ Activity kết thúc. Không đổi Autofill hoặc logic xác thực trong app vì các vấn đề của test harness.

Lint từng phát hiện padding shorthand trong style không hỗ trợ minSdk 24; đã đổi sang padding/margin từng cạnh. Ảnh font lớn phát hiện slogan bị cắt chiều cao; đã sửa header và thêm assertion cho slogan, sau đó chạy lại toàn bộ 5 test.

## Screenshot thực tế

AVD giữ nguyên BiVoca_API35_Pixel6, Android API 35, native 1080 × 1920, density 420. Không chỉnh AVD config trong M16.2.

- [Register — phần đầu](../../android-app/runtime-evidence/m16-2-auth/register-final.png)
- [Register — phần cuối, nút và footer](../../android-app/runtime-evidence/m16-2-auth/register-bottom.png)
- [Forgot — phần đầu](../../android-app/runtime-evidence/m16-2-auth/forgot-password-final.png)
- [Forgot — callout và nút quay lại](../../android-app/runtime-evidence/m16-2-auth/forgot-password-bottom.png)
- [Login regression](../../android-app/runtime-evidence/m16-2-auth/login-regression.png)
- [Forgot font scale 1.3](../../android-app/runtime-evidence/m16-2-auth/forgot-font-130.png)
- [Forgot landscape cuộn đến nút quay lại](../../android-app/runtime-evidence/m16-2-auth/forgot-landscape.png)

Đã mở ảnh để đối chiếu. Đây là screenshot Android thật, không phải ảnh thiết kế. Trên 1080 × 1920, Register/Forgot dài hơn viewport nên cần cuộn; ảnh phần đầu và phần cuối thể hiện các vị trí cuộn khác nhau, không ghép thành một màn giả.

Khác mockup: Back ở hàng riêng theo ưu tiên responsive trong yêu cầu; tiêu đề dài xuống hai dòng; khóa Forgot 64dp phẳng theo spec, không dùng vòng tròn lớn trang trí.

## Responsive và giới hạn bằng chứng

| Môi trường | Bằng chứng |
|---|---|
| Native 1080 × 1920/density 420 | Runtime navigation, screenshots và 5 instrumentation tests PASS |
| Màn nhỏ 320 × 640dp | Đo/inflate hai layout bằng Android resources trên Emulator, cả scale 1.0/1.3 PASS |
| Màn dài tương đương 1080 × 2400 | Đo layout tại 411 × 914dp, scale 1.0/1.3 PASS; không chạy một AVD native 1080 × 2400 khác |
| Landscape | Đo layout 731 × 411dp PASS; runtime Forgot root 1920 × 1080, cuộn tới nút quay lại đầy đủ |
| Font scale 1.3 | Đo cả hai layout PASS; thêm ảnh runtime Forgot với slogan xuống dòng đầy đủ |
| IME mở | Register runtime PASS: IME visible, cuộn tới confirm/button/footer; chưa có test IME riêng cho Forgot |
| Giữ dữ liệu form | Activity recreation giữ đủ 5 trường và trạng thái mắt PASS; không tuyên bố đã test riêng mọi tổ hợp rotation/IME |

Sau thử nghiệm đã trả font_scale=1.0, user_rotation=0, accelerometer_rotation=1. Resolution/density vẫn 1080 × 1920/420.

Crash buffer và last-ANR tại lần kiểm tra cuối không có bản ghi khớp package com.example.bivoca. Các test UI không crash/ANR app; không khẳng định đã kiểm tra mọi luồng ứng dụng.

## Bảo toàn Login và nghiệp vụ

So sánh SHA-256 trước/sau: 11/11 file được bảo vệ không thay đổi, gồm LoginActivity, activity_login.xml, tài nguyên M16.1, Login/Register/Forgot ViewModel, AuthApi, AuthRepository và SessionManager. Chi tiết: protected-files-verification.json.

- Không sửa Backend, PostgreSQL, Admin Web, Credential Manager, JWT, API contract, Flashcard/Quiz/XP/Progress/SRS/Daily Goal.
- Không truy vấn/ghi DB hoặc tạo user/reset password thật trong lượt này.
- Chỉ gửi form không hợp lệ để validation chặn trước khi gọi Repository; dữ liệu nhập trong test là dữ liệu thử cục bộ.
- Phản hồi chung của Forgot API giữ nguyên; không thêm tra cứu email tồn tại.
- Loading/disabled/progress được nối lại với LiveData hiện có. Chưa nghiệm thu loading và success/error bằng request ghi thật trong lượt UI này.
- Không kiểm thử lại Google Sign-In bằng tài khoản thật; logic và resource Login không thay đổi.
- Dữ liệu/session ứng dụng giữ nguyên qua adb install -r; không triển khai M16.3.

## Bàn giao

M16.2 hoàn thành phần UI và kiểm thử kỹ thuật nêu trên. Hai màn đã cài trên Emulator; dừng để người dùng xem và duyệt trước khi làm màn khác.
