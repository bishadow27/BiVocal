# M11.2 — AVATAR RIGHT DRAWER & UNIFIED BACK NAVIGATION

**Ngày nghiệm thu:** 2026-10-17  
**Thiết bị:** AVD `BiVoca_API35_Pixel6`, `emulator-5554`, API 35  
**Trạng thái:** **DONE**

## 1. Mục tiêu triển khai

Thay hamburger bằng avatar tài khoản ở góc phải, chuyển Drawer sang cạnh phải, bổ sung Back thống nhất cho các màn chức năng, bảo vệ state Quiz và sửa Home để tên dài không bị mặt trăng che. Giữ Java/XML, MVVM, Retrofit, Room, M8/M10/M12 và toàn bộ Backend/PostgreSQL.

## 2. Navigation trước khi chỉnh sửa

`NavigationDrawerController` tạo một shared top bar có hamburger bên trái và mở Drawer bằng `GravityCompat.START`. Các nút Back có sẵn trong layout bị ẩn để dùng hamburger. Home có thêm avatar riêng mở thẳng Profile, nên nếu chỉ thêm avatar vào shared top bar sẽ tạo hai avatar. Settings Detail dùng header/insets riêng và chưa có Drawer chung.

## 3. Navigation mới

- Home: không có Back; chỉ có avatar ở góc phải.
- Màn chức năng: Back 48dp bên trái, tên màn hình giữa, avatar 48dp bên phải.
- Nút Back gọi cùng dispatcher/chính sách với Android System Back.
- Nếu một màn chức năng là task root, Back dùng fallback an toàn về Home.
- Shared top bar phủ header cũ; button cũ được ẩn nên không còn hamburger hoặc Back trùng.

## 4. Cơ chế Right Drawer

Controller duy nhất vẫn bọc nội dung hiện có bằng `DrawerLayout`. Drawer đổi sang `GravityCompat.END`; `open`, `close` và kiểm tra trạng thái đều dùng END. Chiều rộng là 84% màn hình, giới hạn 360dp. Scrim, animation, nội dung màn và ViewModel không bị reset.

Emulator PASS: Drawer trượt từ phải sang trái và đóng bằng nút X, chạm scrim hoặc System Back. Khi Drawer mở, Back đóng Drawer trước và không thoát Activity.

## 5. Avatar Button

Top bar dùng vùng chạm 48×48dp, ảnh 42dp có viền cyan và `centerCrop`. Nhấn avatar chỉ mở Drawer. Avatar Home cũ và hành vi mở Profile trực tiếp đã được loại bỏ; mục Hồ sơ cá nhân vẫn còn trong menu.

## 6. Avatar fallback và cập nhật

`LocalAvatarStore` tiếp tục đọc file theo `userId`. Khi có ảnh, Top Bar, Drawer và Profile dùng cùng bitmap local. Khi không có ảnh, `AvatarLabel` lấy ký tự đầu của username; chỉ dùng `B` nếu username trống.

Ảnh thử được chọn qua luồng Profile thật. Sau khi lưu, Profile, Top Bar và Drawer cập nhật ngay, không restart. Sau khi chụp bằng chứng, avatar thử được reset qua chính chức năng “Dùng avatar mặc định” để trả tài khoản debug về trạng thái fallback ban đầu.

## 7. Unified Back Navigation

`NavigationDrawerController` quản lý thứ tự:

1. Drawer đang mở → đóng Drawer.
2. Màn có custom Back policy → gọi policy.
3. Màn chức năng bình thường → quay lại Activity trước.
4. Không có back stack hợp lệ → về Home.

Topics, Flashcard, Review Today, Progress, Leaderboard, Profile, Settings và Settings Detail đều hiển thị Back bên trái. Home không hiển thị Back.

## 8. Quiz Back Protection

`QuizBackPolicy` quyết định ba hành vi:

- `SUBMITTING`: khóa Back và báo đang gửi.
- Đã có đáp án nhưng chưa FINISHED: hỏi xác nhận rời lượt Quiz.
- Chưa trả lời hoặc đã FINISHED: Back bình thường.

Thoát không gọi `submitAnswer`, không cộng XP và không tự tạo Quiz Result. Unit test bao phủ cả ba nhánh. Nghiệm thu UI không gửi đáp án nào vào DB vận hành.

## 9. Settings nested navigation

`SettingsDetailActivity` được gắn vào controller hiện có với title section thực tế. Insets cũ được bỏ để tránh cộng hai lần. Screenshot HỌC TẬP cho thấy chỉ một Back, title giữa và avatar phải; Back trở về Settings Main.

## 10. Offline behavior

Backend test cổng 3001 được dừng trước kiểm tra. App E2E vẫn mở Drawer, hiển thị avatar fallback, chọn Topics, selected state đúng, đóng Drawer và quay Home mà không mất session. Package E2E không có Room snapshot nên Topics báo đúng “chưa có chủ đề được lưu”; code M8 của package debug không thay đổi. Mở Drawer không phát sinh request Backend.

## 11. Account isolation

- `LocalAvatarStore.avatarFile(root, userId)` tiếp tục tách file `user_<id>.jpg`; unit test user 12/13 PASS.
- `AvatarLabel` lấy chữ từ username session hiện tại; unit test username thường, Unicode và fallback PASS.
- Logout được kiểm tra trên app E2E: đóng/clear task về Login; Back không mở lại màn authenticated của package E2E.
- Không có hai tài khoản test đang đăng nhập đồng thời để chụp ảnh A/B; phần cách ly nhiều tài khoản được xác minh bằng source và unit test.

## 12. Các file chỉnh sửa

- `NavigationDrawerController.java`
- `HomeActivity.java`
- `FlashcardActivity.java`
- `QuizActivity.java`
- `ProfileActivity.java`
- `SettingsDetailActivity.java`
- `view_navigation_top_bar.xml`
- `view_navigation_drawer.xml`
- `activity_home.xml`
- `activity_topics.xml`, `activity_flashcard.xml`, `activity_quiz.xml`
- `activity_progress.xml`, `activity_leaderboard.xml`, `activity_profile.xml`, `activity_settings.xml`
- `strings.xml`
- `WORK_STATUS.md`, `ANDROID_CONTEXT.md`

Các layout Activity chỉ đổi ký hiệu toolbar cũ sang Back; controller ẩn button cũ và hiển thị shared chrome mới.

## 13. Các file mới

- `ui/navigation/AvatarLabel.java`
- `ui/quiz/QuizBackPolicy.java`
- `AvatarLabelTest.java`
- `QuizBackPolicyTest.java`
- `M11_2_AVATAR_RIGHT_DRAWER_NAVIGATION_REPORT.md`

## 14. Kết quả build

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\android-app"
.\gradlew.bat :app:assembleDebug :app:assembleE2e :app:testDebugUnitTest :app:lintDebug --console=plain
```

Kết quả cuối: `BUILD SUCCESSFUL`; Debug và E2E APK đều PASS. APK Debug cuối được cài bằng `adb install -r`, giữ nguyên app data.

## 15. Kết quả unit tests

**96/96 PASS**, 23 suites, 0 failure, 0 error, 0 skipped. Baseline 91 + 2 test AvatarLabel + 3 test QuizBackPolicy. Không xóa hoặc sửa giảm test cũ.

## 16. Kết quả lint

`:app:lintDebug` PASS: **0 lỗi / 269 cảnh báo**. Số warning cao hơn baseline tham khảo 1; không có lint error mới. Các warning ở file liên quan đã được đọc, chủ yếu là `clipToOutline` theo API và các warning dịch chuỗi/hardcode đã tồn tại trong UI cũ.

## 17. Kết quả Emulator

| Hạng mục | Kết quả |
|---|---|
| Home không hamburger/Back, một avatar phải | PASS |
| Drawer mở từ phải | PASS |
| Đóng bằng X/scrim/System Back | PASS |
| Đủ 9 mục và selected state duy nhất | PASS |
| Topics/Flashcard Back–title–avatar | PASS |
| Flashcard giữ `1/30` và mặt đã lật qua open/close Drawer | PASS |
| Quiz Back UI, không mutation DB vận hành | PASS |
| Progress/Leaderboard/Profile/Settings | PASS |
| Settings Detail không Back trùng | PASS |
| Avatar ảnh cập nhật Top Bar/Drawer ngay | PASS |
| Avatar fallback theo username | PASS |
| Font scale 1.3 và tên xuống dòng | PASS |
| Landscape navigation | PASS |
| Offline Drawer/Back/session | PASS |
| Logout clear back stack package E2E | PASS |

## 18. Screenshots thật

Thư mục `android-app/app/build/reports/m11-2-avatar-right-navigation/` có đủ 15 ảnh yêu cầu:

1. `01-home-avatar-right.png`
2. `02-right-drawer-open.png`
3. `03-right-drawer-full-menu.png`
4. `04-topics-back-avatar.png`
5. `05-flashcard-back-avatar.png`
6. `06-quiz-back-avatar.png`
7. `07-progress-back-avatar.png`
8. `08-leaderboard-back-avatar.png`
9. `09-profile-back-avatar.png`
10. `10-settings-back-avatar.png`
11. `11-settings-detail-back.png`
12. `12-home-long-username.png`
13. `13-avatar-user-image.png`
14. `14-avatar-fallback.png`
15. `15-landscape-navigation.png`

Tất cả là screenshot trực tiếp từ Emulator, không dùng mockup/AI.

## 19. Accessibility

- Avatar và Back có vùng chạm 48dp.
- Content description riêng cho Back, avatar mở Drawer và ảnh đại diện.
- Title giới hạn một dòng/ellipsize giữa hai vùng điều khiển cố định.
- Drawer 84% và max 360dp; menu nằm trong ScrollView.
- Font scale 1.3: tên test xuống hai dòng, không đè lên trăng.
- Portrait/landscape đã chụp thật; system setting cuối trả font scale 1.0, auto rotation 1, user rotation 0.

## 20. Các lỗi đã sửa

1. Hamburger/Drawer trái → avatar/Drawer phải.
2. Avatar Home mở Profile và trùng shared avatar → chỉ giữ shared avatar mở Drawer.
3. Drawer không có fallback khi chưa có ảnh → thêm ký tự theo tài khoản.
4. Settings Detail dùng insets/header riêng → dùng shared controller, không Back trùng.
5. Quiz Back có thể rời trong lúc request đang gửi → khóa SUBMITTING và xác nhận lượt đang làm.
6. Tên Home dài có thể đi vào vùng trăng → thu nhỏ/đẩy trăng về mép, chừa vùng chữ; font 1.3 PASS.

## 21. Các giới hạn còn lại

- Account A/B chưa được thao tác E2E bằng hai tài khoản trong cùng lượt; cách ly được chứng minh bằng key `userId`, unit test và việc package E2E/debug có session/avatar độc lập.
- Package E2E không có Room cache nên offline Topics chỉ xác minh lỗi cache rỗng; M8 cache của app debug không bị sửa và unit/regression build vẫn PASS.
- 269 lint warnings nền chưa được dọn vì ngoài phạm vi; không có lint error.

## 22. Kết luận nghiệm thu

M11.2 **DONE**. Hamburger đã bị loại khỏi UI authenticated, avatar tài khoản mở Right Drawer, Back thống nhất hoạt động trên các màn chức năng, Quiz/Flashcard được bảo vệ, Settings Detail không trùng toolbar, tên dài không bị trăng che và đủ 15 ảnh Emulator. Không sửa Backend, PostgreSQL, API, Room, XP/Streak, M10 hoặc M12. `flashcard_english` được kiểm tra cuối với `unchanged: true`; Backend vận hành cổng 3000 vẫn `ok/connected`, test server 3001 đã dừng.

