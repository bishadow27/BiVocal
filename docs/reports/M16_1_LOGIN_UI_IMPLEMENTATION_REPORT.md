# M16.1 — Login UI theo ảnh tham chiếu và spacing fix

Ngày: 2026-10-25. Trạng thái: đã triển khai, build và chụp ảnh Emulator; chờ người dùng duyệt giao diện.

## Cập nhật mới nhất — Header + Labels (2026-10-25)

Slogan refinement: resource dùng underline span, màu #7DD3FC. Gom mascot + wordmark và slogan thành cột riêng; slogan căn giữa theo chiều rộng cột này, divider/heading vẫn ở bên phải và căn theo hàng brand. Build assembleDebug PASS (4 giây), APK cài -r và mở Login thành công. UI dump slogan-alignment.xml xác nhận slogan x=63..441px, heading bắt đầu x=529px; không còn kéo slogan sang phần ĐĂNG NHẬP.

Sửa display letterbox sau phản hồi: wm size override giữ khung Pixel 6 cũ gây hai dải đen, nên đã reset override và chỉnh native AVD config.ini: hw.lcd.width=1080, hw.lcd.height=1920, density=420, skin.name=1080x1920, showDeviceFrame=no. Backup config-before-compact-display-20261025.ini ở cùng thư mục AVD. Sau cold boot adb xác nhận Physical size 1080x1920, Login mở thành công, reverse 3000 khôi phục; không xóa app data. Đây là cấu hình hiện hành, thay thế thử nghiệm override ghi bên dưới.

Display nghiệm thu mới theo người dùng: đổi riêng Emulator sang override 1080x1920, density 420, bằng adb shell wm size 1080x1920. UI dump compact-display.xml: root [0,0][1080,1920], viewport [0,102][1080,1857], mascot bắt đầu y=210, footer kết thúc y=1748; toàn bộ nội dung nằm trong viewport. Không thay layout để ép theo một độ phân giải. Khôi phục bằng adb shell wm size reset.

Điều chỉnh vị trí tiếp theo: chuyển textForgotPassword xuống dưới passwordFieldContainer, căn phải, marginTop 6dp; label Mật khẩu ở trên input. Chỉ sửa XML, giữ ID và listener. assembleDebug PASS (6 giây), cài -r thành công. UI dump forgot-position.xml xác nhận bottom input=1164px, top link=1180px, đúng khoảng 6dp tại density 420.

Cập nhật sau khi người dùng báo Emulator vẫn hiện bản cũ: đã cài APK mới bằng adb install -r trên emulator-5554, force-stop và mở lại Login. UI dump header-labels-installed.xml xác nhận hiện cả “Học từ vựng mỗi ngày” và “Tài khoản”. Không clear data hoặc đăng nhập thử.

- Theo yêu cầu tiếp theo: mascot 48dp, BiVocal 26sp bold, ĐĂNG NHẬP 20sp bold; divider giữ nguyên.
- Thêm slogan “Học từ vựng mỗi ngày” 13sp dưới header, cách 6dp, canh dưới chữ BiVocal.
- Thêm label “Tài khoản” 14sp medium; input phía dưới cách 6dp. Label mật khẩu đổi 14sp.
- Slogan → form 24dp; email → hàng label password 16dp; password → Login 20dp; Login → divider 20dp; divider → Google 16dp; Google → footer giữ 32dp.
- Tiếp tục căn giữa toàn khối trong ScrollView, không spacer giãn.
- Chỉ sửa activity_login.xml và values/login_ui.xml. Không sửa Java hay business logic.
- assembleDebug, testDebugUnitTest, lintDebug BUILD SUCCESSFUL (7 giây); unit test task đã chạy lại.
- Người dùng yêu cầu chỉ build và thông báo, không cần chụp ảnh. Chưa cài lại APK hoặc nghiệm thu runtime cho bản Header + Labels; các ảnh và bảng runtime bên dưới phản ánh bản spacing fix trước cập nhật này.

## Giao diện

- Nền #090D16, input #111726/viền #334155, cyan #38BDF8 và blue #2563EB; Roboto hệ thống.
- Header gồm mascot gốc trong khung 40 × 40dp, BiVocal 24sp, divider 1,5 × 32dp và ĐĂNG NHẬP 18sp.
- Mascot được crop vùng trái của bitmap logo chính thức trong LoginActivity; asset dùng ở các màn khác giữ nguyên.
- Input 52dp, bo góc 12dp, email icon, password icon và vùng chạm mắt 48dp.
- Nút đăng nhập gradient ngang; giữ loading/disabled, thêm ripple pressed; Google có G bốn màu.
- Footer là một TextView có định dạng để tự xuống dòng khi tăng cỡ chữ.

## Spacing fix theo yêu cầu tiếp theo

Chỉ sửa activity_login.xml trong lần spacing fix:

- Xóa Space có layout_weight đẩy footer xuống đáy.
- Footer có layout_marginTop=32dp ngay sau Google.
- LinearLayout chứa toàn bộ header/form/Google/footer dùng gravity=center_vertical và paddingVertical=24dp.
- ScrollView fillViewport=true giữ nội dung ở giữa khi đủ chỗ; khi chiều cao không đủ, nội dung lớn hơn viewport và có thể cuộn.
- Không dùng tọa độ Y, bottom constraint hoặc spacer giãn.
- Header, typography, màu và nút giữ nguyên so với bản M16.1 trước spacing fix.

## File source thêm/chỉnh sửa trong toàn M16.1

- android-app/app/src/main/res/layout/activity_login.xml
- android-app/app/src/main/res/values/login_ui.xml (mới)
- android-app/app/src/main/res/drawable/login_ui_field.xml (mới)
- android-app/app/src/main/res/drawable/login_ui_primary.xml (mới)
- android-app/app/src/main/res/drawable/login_google_g.xml (mới)
- android-app/app/src/main/res/values/strings.xml
- android-app/app/src/main/AndroidManifest.xml (theme riêng cho Login)
- android-app/app/src/main/java/com/example/bivoca/ui/auth/LoginActivity.java (hiển thị mascot)

Theme mới kế thừa Login theme và chỉ áp dụng cho LoginActivity. Register/Forgot vẫn dùng theme cũ.

## Build và regression

Lệnh thực tế tại android-app, JAVA_HOME trỏ Android Studio jbr:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

- Sau triển khai UI: BUILD SUCCESSFUL, 76/76 unit tests PASS.
- Sau chỉnh footer hỗ trợ cỡ chữ: BUILD SUCCESSFUL.
- Sau spacing fix: BUILD SUCCESSFUL trong 25 giây; unit task UP-TO-DATE, giữ kết quả 76/76 tests, 0 failures/errors.
- Lint: 0 errors, 290 warnings toàn app; không tuyên bố lint không có cảnh báo.
- adb install -r thành công; không uninstall hoặc clear data.

## Runtime và ảnh thật

AVD BiVoca_API35_Pixel6, emulator-5554, API 35, 1080 × 2400 px, density 420.
Ảnh và UI dumps tại android-app/runtime-evidence/m16-1-login/.

| Kiểm tra | Kết quả |
|---|---|
| Login dọc sau spacing fix | PASS, 08-login-spacing-fixed.png |
| Khoảng cách Google → footer | PASS: bottom Google 1758px, top footer 1842px; 84px / 2,625 = 32dp |
| Landscape cuộn đến Google/footer | PASS, ScrollView scrollable=true; footer hiển thị đầy đủ ở 10-spacing-landscape-scrolled.png |
| Font scale 1,3 | PASS, 11-spacing-font-130.png; header, form, footer đọc được |
| Login → Register | PASS ở bản M16.1 trước spacing fix; registerRoot trong register.xml |
| Login → Forgot | PASS ở bản M16.1 trước spacing fix; forgotRoot trong forgot.xml |
| Hiện/ẩn password | PASS ở bản M16.1 trước spacing fix; chuỗi thử không phải mật khẩu tài khoản được giữ khi toggle |

Ảnh trước spacing fix 01-login.png cho thấy footer bị đẩy xuống đáy. Ảnh 08 là kết quả cuối để duyệt.

WindowInsets xử lý system bars/IME và adjustResize hiện có được giữ nguyên. Bàn phím mềm ở Emulator hiển thị không ổn định; không đánh dấu bài kiểm tra docked IME sau spacing fix là PASS. Ảnh 09 chỉ chứng minh focus/password field, không phải bằng chứng bàn phím docked. Landscape đã xác minh được cuộn tới footer.

Trong nghiệm thu, Emulator từng chụp ra wallpaper dù UI dump và focus là Login. Khởi động lại Emulator khắc phục; ảnh 08 đã được mở và đối chiếu trực tiếp. Cấu hình resolution/density, font scale và rotation đã trả về giá trị ban đầu. Session và app data giữ nguyên.

## Bảo toàn nghiệp vụ và giới hạn

Không sửa LoginViewModel, AuthApi, AuthRepository, Credential Manager, JWT, SessionManager, Forgot/Register API, Backend hoặc PostgreSQL. Không gửi request đăng nhập thử hay gửi email khôi phục trên tài khoản production.

Khác ảnh minh họa: dùng status/navigation bar Android, font Roboto theo yêu cầu, mascot từ asset raster gốc; gradient theo mã màu trong spec. Nội dung cuối căn giữa theo yêu cầu spacing mới, thay vị trí footer sát đáy của bản đầu. Không tuyên bố khớp từng pixel giữa Android và mockup iPhone.

Chưa triển khai giao diện Register/Forgot hay màn khác. Chờ người dùng duyệt ảnh Login.
