# M11 — Home UI và chuẩn hóa vị trí Navigation

**Ngày nghiệm thu:** 2026-10-15  
**Trạng thái:** **DONE — Home UI, dữ liệu API thật, Top App Bar dùng chung, build/test/lint và E2E Emulator đều PASS.**

## 1. Home / Tổng quan

`HomeActivity` hiện có tiếp tục là đích của mục **Tổng quan**. Không tạo Activity Tổng quan mới.

Giao diện được dựng bằng Java/XML theo Dark Night Sky của BiVocal:

- nền navy, trăng khuyết, sao, avatar mặc định;
- lời chào ưu tiên `fullName`, fallback `username`;
- ba thẻ XP, ngày học liên tiếp và số từ đã học;
- thẻ **Tiếp tục học** mở Topics;
- thẻ **Ôn tập hôm nay** dùng `dueNow` và extra M10 hiện có;
- lưới Topics, Quiz tổng hợp, Progress và Leaderboard;
- logo/mascot và lời động viên cuối trang;
- loading, lỗi và Retry không khóa các chức năng điều hướng.

Không dùng số liệu mock, không tạo chủ đề học gần nhất giả và không tự tính phần trăm tiến độ.

## 2. API và DTO được dùng

Home tái sử dụng hai ViewModel/Repository đã có:

- `ProfileViewModel` → `ProfileRepository` → `AuthApi.getMe()` và `GamificationApi.getMyStats()`.
  - `User.fullName`, `User.username` cho lời chào/avatar.
  - `GamificationStats.totalXp`, `currentStreak` cho hai thẻ thống kê.
- `ProgressViewModel` → `ProgressRepository`.
  - `GET /progress/summary` → `ProgressSummary.trackedCards` cho số từ đã học.
  - `GET /progress/schedule` → `ReviewSchedule.dueNow` cho Ôn tập hôm nay.

Không sửa Backend, API contract, Auth/JWT, PostgreSQL, migration, Admin Web, Room M8 hoặc logic M10.

## 3. Nguyên nhân hamburger bị lệch

Trước thay đổi, mỗi layout tự đặt nút theo cấu trúc riêng:

- Home đặt ☰ bên trong `ScrollView` có padding 24dp;
- Topics đặt trong header dọc có `paddingTop=24dp`;
- Quiz dùng header `padding=16dp` nhưng không xử lý insets như Progress;
- Flashcard đặt header bên trong `ScrollView`;
- Progress/Leaderboard tự cộng system bar inset vào root;
- Profile không dùng cùng cơ chế inset.

Do đó cùng một AVD nhưng bounds từng nút khác nhau, và một số nút có thể cuộn theo nội dung.

## 4. Phương án chuẩn hóa

`NavigationDrawerController` vẫn là controller duy nhất. Controller nay:

1. bọc nội dung Activity trong `DrawerLayout` như trước;
2. thêm một `FrameLayout` làm content container;
3. inflate duy nhất `view_navigation_top_bar.xml` phía trên nội dung;
4. ẩn nút cũ nhưng giữ chỗ cho tiêu đề/layout cũ;
5. dùng nút `buttonNavigationMenu` chung: marginStart 16dp, vùng chạm 48×48dp, icon 24sp;
6. áp dụng system bar/display cutout insets một lần cho nội dung và Top App Bar;
7. giữ Top App Bar ngoài `ScrollView` nên vị trí không đổi khi cuộn.

Ba listener inset riêng ở Flashcard, Progress và Leaderboard được bỏ để tránh cộng status bar hai lần. Drawer, route policy, selected state, scrim, Back, Logout và điều hướng cũ giữ nguyên.

## 5. File thêm và sửa

### Thêm

- `android-app/app/src/main/res/layout/view_navigation_top_bar.xml`
- `android-app/app/src/main/res/values/home_styles.xml`
- `android-app/app/src/main/res/drawable/home_avatar_background.xml`
- `android-app/app/src/main/res/drawable/home_icon_background.xml`
- `android-app/app/src/main/res/drawable/home_stat_background.xml`
- `android-app/app/src/main/res/drawable/home_error_background.xml`
- `android-app/app/src/main/res/drawable/home_footer_background.xml`
- `M11_HOME_NAVIGATION_FIX_REPORT.md`

### Sửa

- `ui/home/HomeActivity.java`
- `ui/navigation/NavigationDrawerController.java`
- `ui/flashcard/FlashcardActivity.java`
- `ui/progress/ProgressActivity.java`
- `ui/leaderboard/LeaderboardActivity.java`
- `res/layout/activity_home.xml`
- `res/values/ids.xml`
- `res/values/strings.xml`
- `WORK_STATUS.md`
- `ANDROID_CONTEXT.md`

Không có `M11_HOME_UI_REPORT.md` trước nhiệm vụ nên không tạo một báo cáo trùng nội dung.

## 6. Build và test

Các lệnh cuối cùng:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb reverse tcp:3000 tcp:3000
```

| Kiểm tra | Kết quả |
|---|---|
| Assemble Debug | PASS |
| Unit tests | **76/76 PASS**, 0 failure/error/skipped, 14 suites |
| Lint | PASS, **0 errors, 178 warnings**; baseline trước nhiệm vụ là 191 warnings |
| Cài APK `-r` | PASS trên `emulator-5554`, giữ app data |
| Backend | `/api/v1/health` HTTP 200, database connected |

## 7. E2E trên Emulator

AVD `BiVoca_API35_Pixel6`, API 35, x86_64, portrait 1080×2400, density 420dpi.

- Home nhận dữ liệu thật: XP 10, streak 2, tracked cards 5, dueNow 4 tại thời điểm chụp.
- Avatar → Profile, Tiếp tục học → Topics, Quiz, Progress, Leaderboard và Review Today đều mở đúng Activity: PASS.
- Drawer mở/đóng và từng route tiếp tục hoạt động: PASS.
- Landscape không crash; Home cuộn được và hamburger vẫn ở Top App Bar: PASS.
- Ngắt `adb reverse tcp:3000`: Home báo không kết nối máy chủ và lỗi lịch ôn tập, các chức năng vẫn dùng được; bounds hamburger không đổi: PASS.
- Khôi phục reverse và nhấn Retry: XP/dueNow tải lại từ API: PASS.
- Login, Register và Forgot theo UIAutomator có 0 Drawer/control hamburger: PASS.
- Logout đã được dùng để kiểm tra ba màn xác thực. Sau đó AVD được khởi động lại từ snapshot giữ phiên, APK cuối được cài `-r` và smoke test lại Home cùng thẻ Chủ đề; app hiện ở Home.

## 8. Bảng tọa độ hamburger

System bars lúc đo: status inset 128px, navigation inset 63px. File máy đọc được: `android-app/app/build/reports/m11-home-navigation/hamburger-bounds.csv`.

| Màn hình | Bounds px | Trái dp | Trên dp | Vùng nhấn dp |
|---|---|---:|---:|---:|
| Home | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Topics | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Flashcard | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Quiz | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Progress | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Leaderboard | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |
| Profile | `[42,138][168,264]` | 16.00 | 52.57 | 48×48 |

Sai lệch portrait: **0px / 0dp**. Landscape Home: `[42,73][168,199]`, đúng quy tắc responsive với system inset ngang.

## 9. Ảnh nghiệm thu

Thư mục `android-app/app/build/reports/m11-home-navigation/`:

- `01-home-overview.png`
- `02-home-drawer-open.png`
- `03-topics-topbar.png`
- `04-quiz-topbar.png`
- `05-progress-topbar.png`
- `06-leaderboard-topbar.png`
- `07-profile-topbar.png`
- `08-home-landscape.png`
- `09-flashcard-topbar.png`

## 10. Giới hạn đối chiếu thiết kế

Attachment của lượt này chỉ chứa prompt văn bản, không có file ảnh Home độc lập. Vì vậy không thể thực hiện đối chiếu pixel-by-pixel với ảnh Home. Giao diện đã bám toàn bộ đặc tả văn bản và hệ visual Dark Night Sky đã duyệt ở Login/Register; screenshot Emulator thật được lưu để người dùng duyệt trực tiếp. Không có giới hạn chức năng hoặc lỗi chặn còn lại.
