# BiVocal — Login theo thiết kế đã duyệt, Phase B

## Cập nhật nghiệm thu và bàn giao — 2026-10-29

Người dùng xác nhận trực tiếp **Home Phase A và Login Phase B nghiệm thu giao diện PASS**. Login không còn chờ duyệt UI. Người dùng đã duyệt đúng danh sách37 file và cho phép commit/push trên `origin/feature/initial-import`; không bao gồm ảnh/log runtime-evidence, build/APK, Skill untracked hoặc dữ liệu nhạy cảm.

Chỉ bổ sung ghi nhận nghiệm thu, không thay source hay kết quả kiểm thử. Các giới hạn Google/password success/SMTP E2E và AVD nhỏ riêng vẫn giữ nguyên NOT_RUN. Phần bên dưới là báo cáo ban đầu trước khi người dùng nghiệm thu; các trạng thái chờ duyệt/CHƯA COMMIT/CHƯA PUSH trong đó được giữ làm lịch sử. Kết quả commit/push sẽ bàn giao trong chat.

Ngày kiểm tra: 2026-10-29. **Login: READY_FOR_ACCEPTANCE, chờ người dùng duyệt giao diện.** Home Phase A đã được người dùng nghiệm thu trực tiếp trong chat cùng ngày, không chỉnh lại Home trong Phase B.

## Kết quả triển khai

- Header cùng hàng: mascot chính thức + wordmark/slogan ở trái, divider giữa, “Đăng nhập” bên phải; căn giữa theo chiều dọc. Mascot40×48dp, wordmark20–24sp tự co vừa chiều rộng, title20sp. Slogan12sp không gạch chân, tối đa hai dòng khi hẹp.
- Giữ subtitle “Tiếp tục hành trình học của bạn.”14sp. Label Tài khoản/Mật khẩu14sp; input15sp, minHeight56dp để hỗ trợ font lớn, icon20dp, viền1dp/radius12dp.
- Form là trọng tâm; cả cụm nội dung căn giữa vùng còn lại khi đủ chỗ, ScrollView hoạt động ở màn hẹp/ngang hoặc bàn phím mở. Không neo footer xuống đáy; Google → footer32dp.
- CTA52dp xanh #2457E6; text trắng có tương phản tốt hơn nền blue sáng. Input #121A26, viền #243246, nền #070B12; link cyan. Reuse palette đã có của Home, không sửa các token Home.
- Giữ nguyên mascot asset, icon mail/khóa/mắt/Google và tất cả ID/callback UI hiện có. Login dùng Theme.BiVoca.Login.Approved riêng; Register/Forgot giữ theme cũ.

Không sửa bất kỳ file Java ứng dụng nào: LoginActivity, LoginViewModel, Repository, AuthApi, Credential Manager, SessionManager và API contract giữ hash. Chỉ cập nhật XML Login, entry theme Login trong Manifest và thêm resource/test. Home source/resource, Drawer, Bottom Navigation, Topics, Flashcard, Backend, Admin và database giữ nguyên.

## Kiểm thử

| Kiểm tra | Kết quả thật | Phạm vi/giới hạn |
|---|---|---|
| assembleDebug | PASS | Main APK đã build/cài; lượt cuối UP-TO-DATE do chỉ sửa test |
| testDebugUnitTest | PASS 84/84 | Thực sự executed ở lượt build đầu của Phase B; các lượt cuối UP-TO-DATE, không coi là chạy mới |
| assembleDebugAndroidTest + lintDebug, lượt cuối | PASS |14s,7 task executed/71 up-to-date; lint0 lỗi/428 warnings, report lint dùng kết quả còn hợp lệ |
| Guard giữ dữ liệu | PASS |keepInstalledApks=true, uninstallIncompatibleApks=false; leaveApksInstalledAfterRun=true |
| install APK/test APK | PASS |`adb install -r`, không uninstall/clear data |
| Login + Auth navigation + Home layout, lượt cuối | PASS 4/4 |23.219s; validation cục bộ, eye toggle, recreation, keyboard, Register/Forgot quay lại, hồi quy layout Home |
| Login runtime font hệ thống1.3, lượt cuối | PASS 1/1 |10.567s; đã chụp/đọc ảnh, font trả1.0 |
| Login resource measurement | PASS 28 cấu hình |7 size×2 font×2 trạng thái: bình thường/lỗi dài; header/text/CTA/footer không cắt và cuộn tới được |
| Kiểm tra hash source | PASS |2 file sửa,4 file mới;421/423 file gốc giữ hash so với bản Home đã duyệt |
| Google Sign-In/password success/SMTP E2E | NOT_RUN |Không gửi POST đăng nhập/challenge/reset hoặc chọn Google account; không lấy UI test làm bằng chứng E2E xác thực |
| Backend/DB test/migration | NOT_RUN |Không thay đổi module này và không chạy SQL/import |

Kích thước đo layout:320×640,360×800,411×731,412×915,440×915,467×830,731×411dp. Đây là measurement instrumentation trên Emulator, chưa khởi chạy một AVD nhỏ riêng. Runtime kiểm tra trực tiếp trên AVD hiện có, không đổi size/density/config để làm đẹp ảnh.

Các test chỉ dùng email trống/không đúng định dạng nên bị LoginViewModel chặn trước API. Không bypass auth, không tạo tài khoản hoặc mock thành công. Nút Google vẫn có listener và enabled; code Credential Manager/DTO/endpoint giữ nguyên nhưng chưa thực hiện chọn tài khoản trong phiên này.

### Bàn phím và đối chiếu ảnh

Ảnh đầu cho thấy Gboard đang ở chế độ nổi và phủ lên form, dù phép kiểm tra isDisplayed vẫn PASS. Không coi ảnh đó là bằng chứng IME an toàn. Đã chuyển tạm Gboard về gắn đáy, bổ sung assertion IME bottom>0 và kiểm tra tọa độ input/nút/footer nằm trên mép IME sau khi cuộn. Các kết quả cuối trong bảng dùng bộ test chặt hơn và bàn phím gắn đáy. Font1.0/1.3 đều PASS; ảnh đã xem trực tiếp. Sau nghiệm thu, trả Gboard về chế độ nổi ban đầu và ẩn bàn phím; không thay cấu hình AVD.

Giữ các log phát triển: lỗi compile do import hai closeSoftKeyboard đã sửa trong test; measurement đầu báo rộng ở khoảng trắng cuối dòng của textError, đã sửa phép đo sang getLineMax (phần chữ thực sự vẽ), vẫn kiểm tra tràn/cắt chữ. Không chỉnh app để né lỗi hoặc bỏ assertion.

Đã so ảnh thiết kế với Android trước/sau. Không tuyên bố pixel match100%: hình tham chiếu có tỷ lệ màn hình/font/rendering khác; bản thật dùng Roboto và resource native, không ánh sáng/ảnh nền giả. Header nhỏ hoặc font lớn được phép xuống dòng bên trong nhóm để không đè nhau, không ép cỡ màn AVD.

## File thay đổi trong Phase B

Sửa:

- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/main/res/layout/activity_login.xml`

Thêm:

- `android-app/app/src/main/res/drawable/login_approved_field.xml`
- `android-app/app/src/main/res/drawable/login_approved_primary.xml`
- `android-app/app/src/main/res/values/login_approved.xml`
- `android-app/app/src/androidTest/java/com/example/bivoca/LoginApprovedUiTest.java`

Bàn giao: cập nhật WORK_STATUS.md, ANDROID_CONTEXT.md, bổ sung ghi nhận người dùng duyệt vào HOME_APPROVED_PHASE_A_REPORT.md; báo cáo này là hồ sơ Phase B. Không xóa lịch sử các milestone cũ.

## Lệnh và bằng chứng

Từ `android-app`, JAVA_HOME dùng Android Studio jbr:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest --dry-run --no-configuration-cache -I runtime-evidence/m16-3-home/final-polish/test-guards.init.gradle
& $adb devices -l
& $adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s emulator-5554 shell am instrument -w -r -e class 'com.example.bivoca.LoginApprovedUiTest,com.example.bivoca.AuthScreensUiTest#loginNavigationUsesExistingActivities,com.example.bivoca.HomeResponsiveUiTest' com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s emulator-5554 shell settings put system font_scale 1.3
& $adb -s emulator-5554 shell am instrument -w -r -e class 'com.example.bivoca.LoginApprovedUiTest#actualLoginValidationVisibilityRecreationAndKeyboard' com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s emulator-5554 shell settings put system font_scale 1.0
```

Evidence local, ignored: `android-app/runtime-evidence/login-approved-20261029/`:

- `login-before-after-comparison.png`: thiết kế / Android trước / Android sau, cùng chiều rộng và giữ tỷ lệ ảnh.
- `font10-docked/`, `font13-docked/`: login-top, login-local-validation, login-keyboard-footer. Đây là ảnh Login thật, form trống/lỗi cục bộ, không dùng dữ liệu sản xuất giả.
- `font10/`, `font13/`: ảnh các lượt trước với bàn phím nổi, giữ để giải thích khác biệt; không dùng làm nghiệm thu IME gắn đáy.
- `instrumentation-docked-font10.txt`, `instrumentation-docked-font13.txt`, `build-docked.txt`, `test-guards.txt`.
- `source-before.json`, `source-after.json`, `verification-summary.json` (có SHA-256 APK); config AVD giữ hash như Phase A.

## Nghiệm thu còn chờ và Git

Người dùng cần duyệt ảnh Login mới. Cuối phiên đã có phiên đăng nhập trên Emulator; giữ phiên đó, không tự đăng xuất để mở lại Login. Hình Login đã chụp khi người dùng chủ động đăng xuất trước bài test. Chưa xác nhận phương thức đăng nhập của phiên cuối, không tự đánh dấu Google E2E PASS.

Nhánh `feature/initial-import`, giữ mọi thay đổi chưa commit của Phase A và các Skill untracked; CHƯA COMMIT/CHƯA PUSH. Nếu cần quay lại Login, review/hoàn tác riêng các hunk Phase B; không restore toàn Manifest từ commit gốc vì sẽ mất entry theme Home đã duyệt. Không reset/clean hoặc chỉnh module khác.
