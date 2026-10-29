# M11 — Navigation Drawer dùng chung BiVocal

**Ngày nghiệm thu:** 2026-10-15  
**Trạng thái:** **DONE — build, unit test, lint và E2E trên Android Emulator PASS.**

## 1. Kết quả triển khai

- Một `NavigationDrawerController` dùng chung bọc layout hiện tại của từng Activity bằng AndroidX `DrawerLayout`. Không tạo Activity menu, không đổi kiến trúc MVVM và không sao chép menu cho từng màn hình.
- Một `view_navigation_drawer.xml` dùng chung chứa logo BiVocal, nền Dark Night Sky, tên người dùng, nút đóng, bảy mục chức năng và Đăng xuất.
- Drawer rộng 84% màn hình, tối đa 320dp, có scrim, mở từ trái, đóng khi chạm ngoài; nút Back đóng Drawer trước khi rời Activity.
- Danh sách menu nằm trong `ScrollView`, nên vẫn truy cập được toàn bộ mục và Đăng xuất khi xoay ngang.
- Mục hiện tại có nền xanh và mô tả accessibility `đang chọn`. Chọn lại mục hiện tại chỉ đóng Drawer.
- Flashcard theo chủ đề đánh dấu **Chủ đề từ vựng**; chế độ M10 đánh dấu **Ôn tập hôm nay**.

## 2. Tuyến điều hướng đã kiểm tra

| Mục | Đích thật | Kết quả Emulator |
|---|---|---|
| Tổng quan | `HomeActivity` | PASS |
| Chủ đề từ vựng | `TopicsActivity` | PASS |
| Ôn tập hôm nay | `FlashcardActivity` + `EXTRA_REVIEW_TODAY=true` | PASS |
| Làm Quiz | `QuizActivity` tổng hợp, không truyền topic giả | PASS |
| Tiến độ học tập | `ProgressActivity` | PASS |
| Bảng xếp hạng | `LeaderboardActivity` | PASS |
| Hồ sơ cá nhân | `ProfileActivity` | PASS |
| Đăng xuất | `SessionManager.logout()` → `LoginActivity`, xóa authenticated back stack | PASS |

Các Activity có Drawer: Home, Topics, Flashcard thường/M10, Quiz, Progress, Leaderboard và Profile. Login, Register và Forgot Password không có Drawer; UIAutomator xác nhận 0 control Drawer/hamburger trên ba màn xác thực.

## 3. File thêm và sửa

### Thêm

- `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationDrawerController.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationRoute.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/navigation/NavigationRoutePolicy.java`
- `android-app/app/src/main/res/layout/view_navigation_drawer.xml`
- `android-app/app/src/main/res/drawable/nav_item_background.xml`
- `android-app/app/src/main/res/color/nav_item_text.xml`
- `android-app/app/src/main/res/values/ids.xml`
- `android-app/app/src/test/java/com/example/bivoca/ui/navigation/NavigationRoutePolicyTest.java`

### Sửa

- `HomeActivity.java`, `TopicsActivity.java`, `FlashcardActivity.java`, `QuizActivity.java`, `ProgressActivity.java`, `LeaderboardActivity.java`, `ProfileActivity.java`.
- `activity_home.xml`, `activity_topics.xml`, `activity_flashcard.xml`, `activity_quiz.xml`, `activity_progress.xml`, `activity_leaderboard.xml`, `activity_profile.xml`.
- `strings.xml`, `app/build.gradle.kts`.
- `WORK_STATUS.md`, `ANDROID_CONTEXT.md`.

Không sửa Backend, PostgreSQL, migration, Admin Web, dataset, API, Repository, ViewModel, Room M8, lịch M10 hoặc logic Auth/JWT.

## 4. Build và kiểm thử tự động

Các lệnh đã chạy tại `android-app`:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb reverse tcp:3000 tcp:3000
```

| Kiểm thử | Kết quả |
|---|---|
| `:app:assembleDebug` | PASS |
| `:app:testDebugUnitTest` | **76/76 PASS**, 0 failure/error/skipped; thêm 3 test cho route policy |
| `:app:lintDebug` | PASS, 0 lỗi, 191 cảnh báo toàn app |
| `adb install -r` | PASS trên `emulator-5554`, giữ dữ liệu ứng dụng |
| `GET /api/v1/health` | HTTP 200, `status=ok`, `database=connected` |
| `adb reverse --list` | `tcp:3000 tcp:3000` |

## 5. Nghiệm thu E2E trên Emulator

Thiết bị: AVD `BiVoca_API35_Pixel6`, Android API 35, x86_64, 1080×2400, serial `emulator-5554`.

Các thao tác được thực hiện bằng `adb shell input`, xác minh Activity qua `dumpsys activity activities`, xác minh control/trạng thái chọn qua `uiautomator dump`, và chụp bằng `screencap`:

1. Home mở/đóng Drawer: PASS.
2. Chạm vùng scrim ngoài Drawer: đóng Drawer, vẫn ở Home — PASS.
3. Nhấn Back khi Drawer mở: chỉ đóng Drawer, vẫn ở Home — PASS.
4. Chọn lại Tổng quan: chỉ đóng Drawer, không đổi Activity — PASS.
5. Điều hướng Home → Topics → Review Today → Quiz → Progress → Leaderboard → Profile: đúng Activity — PASS.
6. Accessibility `đang chọn` đúng tại Home, Topics, Review, Quiz, Progress, Leaderboard và Profile — PASS.
7. Flashcard mở từ một chủ đề thật: Drawer đánh dấu Topics — PASS.
8. Flashcard M10: Drawer đánh dấu Review Today — PASS.
9. Xoay ngang khi Drawer mở: không crash; cuộn menu và truy cập được Đăng xuất tại bounds `[42,827][798,964]` — PASS.
10. Đăng xuất: về `LoginActivity`; Back về Launcher thay vì màn đã xác thực; mở lại app vẫn ở Login — PASS.

Đăng xuất chỉ xóa phiên qua `SessionManager`; không xóa Room cache hay dữ liệu PostgreSQL.

## 6. Ảnh bằng chứng

Thư mục: `android-app/app/build/reports/m11-navigation/`

- `01-home-closed.png`
- `02-home-drawer-open.png`
- `03-progress-drawer.png`
- `04-topics-drawer.png`
- `05-review-drawer.png`
- `06-quiz-drawer.png`
- `07-leaderboard-drawer.png`
- `08-profile-drawer.png`
- `09-flashcard-drawer.png`
- `10-drawer-landscape.png`
- `11-login-after-logout.png`

## 7. Kết luận

Navigation Drawer dùng chung của M11 đã đạt yêu cầu source, build, test tự động và nghiệm thu runtime trên Emulator. Không còn lỗi chặn trong phạm vi Navigation Drawer. Phiên thử nghiệm đã được đăng xuất ở cuối quy trình, vì vậy lần sử dụng tiếp theo cần đăng nhập lại.
