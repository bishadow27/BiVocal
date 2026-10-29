# BiVocal — Home theo thiết kế đã duyệt, Phase A

## Cập nhật nghiệm thu — 2026-10-29

Người dùng đã duyệt Home trực tiếp trong chat sau khi nhận báo cáo/ảnh cuối. **Phase A Home: người dùng nghiệm thu PASS.** Cho phép tiếp tục Phase B Login theo kế hoạch đã duyệt; không cấp quyền Git hoặc sửa nghiệp vụ. Phần bên dưới giữ nguyên hồ sơ kỹ thuật và trạng thái tại thời điểm bàn giao ban đầu.

Ngày kiểm tra: 2026-10-29. Trạng thái: **READY_FOR_ACCEPTANCE — chờ người dùng duyệt Home**. Chưa triển khai Phase B Login, chưa commit/push.

## Phạm vi và kết quả

- Áp dụng ảnh Login/Home cuối đã duyệt, chỉ triển khai nửa Home bằng Java/XML. Không tạo concept mới hoặc dùng ảnh demo làm background.
- Giữ mascot chính thức, thêm wordmark BiVocal, slogan không gạch chân; streak lấy API và hiển thị `N Day`. Avatar giữ tác vụ mở Drawer bên phải.
- Daily Goal thành section gọn: số tiến độ, phần trăm, thanh tiến độ và CTA xanh dương; giữ hành vi học thêm sau khi hoàn tất mục tiêu.
- SRS nền #121A26, viền #243246, radius20dp; bốn nhóm giữ mapping 0–1 / 2–3 / 4 / 5, các tỷ lệ dùng count API. Legend không còn ô màu nền, Info là vector viền/vùng chạm48dp; hàng ôn tập trung tính, dueNow=0 vẫn disabled.
- Leaderboard giữ ba người đầu theo thứ tự API, dùng rank thật kể cả đồng hạng. Chỉ thêm hàng riêng tài khoản hiện tại nếu ID không nằm trong ba người đầu. Nếu chưa có rank, hiển thị `—`, tên/XP thật và chú thích; không suy ra rank từ XP.
- Bốn tab cố định: Trang chủ, Chủ đề, Ôn tập, Trắc nghiệm. Chỉ Home opt-in màu/icon mới; không thêm hệ điều hướng, Quick Stats, tab Hồ sơ hay card slogan cuối.
- Gutter24dp, section gap24dp, button52dp, chữ body13–14sp/title15–16sp; CTA dùng #2457E6, link cyan; SRS xanh/tím/lá/vàng. Sử dụng Roboto và vector native, không thêm dependency/font tải ngoài.

## Dữ liệu và tương thích

Giữ ProfileViewModel/ProgressViewModel, Repository, Retrofit và API contract. Home dùng `GET auth/me`, `gamification/me`, `learning/today`, `progress/schedule` cùng các GET summary/history hiện có. Leaderboard dùng endpoint hiện có `GET gamification/leaderboard?limit=100`; màn Leaderboard riêng giữ mặc định50.

API hiện chưa có endpoint rank cá nhân độc lập, danh sách tối đa100 và bỏ người có0 XP. Vì vậy ngoài phạm vi API sẽ hiện `—` theo phương án đã được người dùng duyệt. Runtime hiện kiểm tra trường hợp tài khoản thuộc Top3, không tạo thêm hàng trùng. Unit test kiểm tra thứ tự/đồng hạng/tài khoản ngoài Top3 và thiếu rank; chưa xác minh UI hàng riêng bằng một tài khoản thật ngoài Top100.

Không sửa Backend/Admin/SQL/DB/auth/SessionManager/Login/Register/Forgot/Topics/Flashcard hoặc thuật toán SRS/XP/Daily Goal. Không submit Review, Quiz, Favorite hay xóa dữ liệu để tạo ảnh. Không chạy migration, import hoặc các test DB ghi dữ liệu.

## Kiểm thử thực tế

| Kiểm tra | Kết quả | Bằng chứng/giới hạn |
|---|---|---|
| assembleDebug + assembleDebugAndroidTest | PASS | Build cuối25s:24 task executed,54 up-to-date; APK test một số task up-to-date |
| testDebugUnitTest | PASS 84/84 | Task thực sự executed,0 failure/error/skipped |
| lintDebug | PASS,0 lỗi /425 warnings | Còn cảnh báo toàn app; không tuyên bố sạch warning |
| Guard instrumentation | PASS | keepInstalledApks=true, uninstallIncompatibleApks=false; leaveApksInstalledAfterRun=true |
| Cài Debug/test APK | PASS | `adb install -r` cho cả hai APK; giữ session và data |
| HomeResponsive + HomeRuntime + TopicsUiTest, lượt cuối | PASS 4/4 |83.474s; API thật, điều hướng, Drawer, Top3, SRS/due và hồi quy Topics |
| HomeRuntime font hệ thống1.3, lượt cuối | PASS 1/1 |30.961s, đã chụp/đọc ảnh; font trả1.0 |
| Home responsive layout | PASS |7 kích thước ×2 font:320×640,360×800,411×731,412×915,440×915,467×830,731×411; kiểm tra text/inset/nav cố định/cuộn |
| Backend health | PASS HTTP200 | `http://127.0.0.1:3000/api/v1/health`; không thay server/DB |
| Git diff/hash | PASS |14 file source sửa,14 mới;395/409 file gốc giữ hash; Backend/Admin/Auth/Topics/Flashcard giữ nguyên |

Màn nhỏ được kiểm tra bằng inflate/measure instrumentation trên Emulator; chưa chạy một AVD nhỏ riêng. Không đổi AVD để làm đẹp ảnh: size1080×1920, density override370 (physical420) có sẵn, config.ini giữ nguyên SHA-256. Kiểm thử runtime font1.3 dùng chính Emulator hiện tại.

Các lệnh đã chạy từ `android-app`:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest --dry-run --no-configuration-cache -I runtime-evidence/m16-3-home/final-polish/test-guards.init.gradle
# $adb là platform-tools/adb.exe trong Android SDK local
& $adb devices -l
& $adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.HomeResponsiveUiTest,com.example.bivoca.HomeRuntimeUiTest,com.example.bivoca.TopicsUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s emulator-5554 shell settings put system font_scale 1.3
& $adb -s emulator-5554 shell am instrument -w -r -e class com.example.bivoca.HomeRuntimeUiTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
& $adb -s emulator-5554 shell settings put system font_scale 1.0
& $adb -s emulator-5554 shell am start -W -n com.example.bivoca/.ui.auth.LoginActivity
```

Giữ log các lượt FAIL: Home/Topics mất window focus; chẩn đoán thấy NotificationShade, Launcher ANR và một ứng dụng khác giành focus. Sau khi người dùng đồng ý để nguyên Emulator, máy ảo bị tắt; khởi động lại AVD hiện có (không load/save snapshot, không wipe) rồi chạy lại đạt kết quả cuối ở bảng trên. Không sửa app để né assertion. Ảnh font1.3 dùng lượt `cleanboot` PASS, không lấy ảnh cũ từ các lượt FAIL.

## Đối chiếu hình ảnh

- Đã bỏ các khung/emoji/gradient không có trong thiết kế được duyệt; chỉnh header, Daily Goal, SRS, các hàng XP và icon bốn tab.
- Info ban đầu bị tint thành chấm đặc, đã phát hiện bằng ảnh thật và sửa thành icon viền; dấu màu SRS đã sửa thành chấm tròn.
- Không tuyên bố giống100%: Roboto/native vector khác nét chữ/icon của ảnh tạo demo; khung Emulator ngắn hơn ảnh nên phải cuộn để xem đủ leaderboard. Header hẹp cho slogan xuống hàng để không đè avatar.
- Dữ liệu thật khác số liệu minh họa. Nhóm SRS count0 không vẽ đoạn giả để đủ bốn màu.

Evidence local tại `android-app/runtime-evidence/home-approved-20261029/` (ignored, không đưa ảnh tài khoản/log vào Git):

- `home-before-after-comparison.png`: thiết kế / trước / sau, cùng chiều rộng hiển thị; không kéo méo ảnh để giả pixel match.
- `font10/` và `font13/`: home-top, home-srs, home-leaderboard, home-bottom-navigation, home-drawer-open.
- `build-final.txt`, `instrumentation-final-font10-cleanboot.txt`, `instrumentation-font13-cleanboot.txt`.
- `source-before.json`, `source-after.json`, `avd-before.json`, `verification-summary.json` gồm SHA-256 APK.

## File thay đổi

Sửa source/test (đường dẫn từ gốc project):

- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/test/java/com/example/bivoca/ui/home/HomePresentationTest.java`
- `android-app/app/src/main/res/layout/activity_home.xml`
- `android-app/app/src/main/res/layout/item_home_leaderboard.xml`
- `android-app/app/src/main/res/layout/view_home_header.xml`
- `android-app/app/src/main/res/layout/view_home_header_actions.xml`
- `android-app/app/src/main/res/layout/view_home_srs.xml`
- `android-app/app/src/main/res/layout-w440dp/view_home_header.xml`
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomePresentation.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/leaderboard/LeaderboardViewModel.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/navigation/LearningBottomNavigation.java`
- `android-app/app/src/androidTest/java/com/example/bivoca/HomeResponsiveUiTest.java`
- `android-app/app/src/androidTest/java/com/example/bivoca/HomeRuntimeUiTest.java`

Thêm resource Home riêng:

- `android-app/app/src/main/res/drawable/home_approved_arrow.xml`
- `android-app/app/src/main/res/drawable/home_approved_avatar.xml`
- `android-app/app/src/main/res/drawable/home_approved_clock.xml`
- `android-app/app/src/main/res/drawable/home_approved_dot.xml`
- `android-app/app/src/main/res/drawable/home_approved_due.xml`
- `android-app/app/src/main/res/drawable/home_approved_flame.xml`
- `android-app/app/src/main/res/drawable/home_approved_info.xml`
- `android-app/app/src/main/res/drawable/home_approved_primary.xml`
- `android-app/app/src/main/res/drawable/home_approved_progress.xml`
- `android-app/app/src/main/res/drawable/home_approved_quiz.xml`
- `android-app/app/src/main/res/drawable/home_approved_review.xml`
- `android-app/app/src/main/res/drawable/home_approved_self.xml`
- `android-app/app/src/main/res/drawable/home_approved_track.xml`
- `android-app/app/src/main/res/values/home_approved.xml`

Tài liệu: báo cáo này, WORK_STATUS.md và ANDROID_CONTEXT.md được bổ sung, giữ lịch sử cũ.

## Bàn giao và quay lại

Nhánh `feature/initial-import`, commit gốc7dabeae; không stage/commit/push, không đụng các Skill/skills-lock.json chưa được theo dõi. Khi cần quay lại, review riêng diff của các file trong danh sách trên; không reset/clean hoặc hoàn tác thay đổi khác. Source trước được bảo vệ bằng Git và file hash, APK cũ có thể rebuild từ commit gốc trong checkout riêng.

Người dùng cần duyệt Home hiện đang mở trên Emulator và ảnh font1.0/1.3. Chỉ sau khi nghiệm thu Home và phê duyệt Phase B mới chỉnh Login. Không tự nâng trạng thái milestone lịch sử.
