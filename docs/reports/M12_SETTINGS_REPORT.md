# M12 SETTINGS REPORT — BIVOCAL

**Ngày nghiệm thu:** 2026-10-17  
**Thiết bị:** Android Emulator `BiVoca_API35_Pixel6`, serial `emulator-5554`, API 35, 1080×2400  
**Trạng thái:** **M12 SETTINGS DONE** trong phạm vi Android. TTS được tích hợp và xử lý lỗi đúng, nhưng Emulator không có TTS engine nên chưa thể xác nhận âm thanh bằng tai.

## 1. Tổng quan nhiệm vụ

M12 bổ sung hệ thống Cài đặt đầy đủ cho BiVocal bằng Java/XML, giữ kiến trúc và giao diện Dark Night Sky hiện có. Phạm vi gồm trang tổng quan và sáu nhóm Học tập, Âm thanh, Nhắc học, Giao diện, Dữ liệu, Giới thiệu; các lựa chọn được lưu thật và áp dụng vào Flashcard, TTS, animation, Room cache và notification.

Không sửa Backend, PostgreSQL, Admin Web, dataset, migration, thuật toán XP/streak/M10 hoặc API contract. APK được cài bằng `adb install -r`; không clear app data và không xóa dữ liệu vận hành.

## 2. Trạng thái trước triển khai và kết quả audit

- Drawer M11 đã dùng chung `NavigationDrawerController`; trước M12 chưa có route Cài đặt.
- Flashcard đã có TTS và animation lật thẻ; chưa có cấu hình mặt trước, tự đọc, tốc độ hoặc giảm chuyển động.
- Room M8 chỉ chứa ba bảng public cache: topics, flashcards và sync markers. Vì vậy có thể đếm/xóa đúng public cache mà không đụng token, avatar hay dữ liệu cá nhân.
- Chưa có AlarmManager/WorkManager/notification cho nhắc học.
- Backend không có chỉ số đáng tin cậy “số thẻ mới học hôm nay”; M12 chỉ lưu mục tiêu và hiển thị rõ giới hạn, không tạo X giả.
- Project không có support email, privacy URL hoặc terms URL hợp lệ; About không phát minh liên kết.

## 3. Các màn Settings hoàn thành

1. **Cài đặt:** sáu card điều hướng trong Drawer, Dark Night Sky, card navy, icon, mô tả và chevron.
2. **Học tập:** mục tiêu 5/10/15/20; mặt trước English/Vietnamese.
3. **Âm thanh:** auto pronunciation và tốc độ 0.8x/1.0x/1.2x.
4. **Nhắc học:** bật/tắt, TimePicker 24 giờ, chọn bảy ngày, quyền Android 13+, notification thật.
5. **Giao diện:** cỡ chữ nội dung học Tiêu chuẩn/Lớn và Giảm hiệu ứng chuyển động.
6. **Dữ liệu:** số topics/cards và dung lượng file Room thực tế; xác nhận trước khi chỉ xóa ba bảng public cache.
7. **Giới thiệu:** logo, tên, mô tả và `versionName/versionCode` thực từ PackageManager.

Màn chi tiết dùng chung một Activity theo section để tránh tạo sáu Activity lặp. System bar insets được áp dụng vào root; nội dung cuộn và không nằm dưới status/navigation bar.

## 4. File mới

- `android-app/app/src/main/java/com/example/bivoca/settings/SettingsPreferenceManager.java`
- `android-app/app/src/main/java/com/example/bivoca/settings/ReminderScheduler.java`
- `android-app/app/src/main/java/com/example/bivoca/settings/ReminderReceiver.java`
- `android-app/app/src/main/java/com/example/bivoca/data/local/CacheStats.java`
- `android-app/app/src/main/java/com/example/bivoca/data/local/CacheSettingsRepository.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/settings/SettingsActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/settings/SettingsDetailActivity.java`
- `android-app/app/src/main/res/layout/activity_settings.xml`
- `android-app/app/src/main/res/layout/activity_settings_detail.xml`
- `android-app/app/src/main/res/drawable/settings_card_background.xml`
- `android-app/app/src/main/res/drawable/settings_option_background.xml`
- `android-app/app/src/main/res/drawable/ic_notification_bivocal.xml`
- `android-app/app/src/main/res/values/settings_styles.xml`
- `android-app/app/src/test/java/com/example/bivoca/settings/SettingsPreferencePolicyTest.java`
- `android-app/app/src/test/java/com/example/bivoca/settings/ReminderSchedulerTest.java`
- `android-app/app/src/test/java/com/example/bivoca/data/local/CacheStatsTest.java`
- `android-app/app/src/androidTest/java/com/example/bivoca/settings/ReminderReceiverTest.java`
- `M12_SETTINGS_REPORT.md`

## 5. File chỉnh sửa

- `android-app/app/src/main/AndroidManifest.xml`
- `android-app/app/src/main/java/com/example/bivoca/data/local/LearningCacheDao.java`
- `android-app/app/src/androidTest/java/com/example/bivoca/data/local/RoomLearningCacheTest.java`
- `android-app/app/src/main/java/com/example/bivoca/navigation/NavigationRoute.java`
- `android-app/app/src/main/java/com/example/bivoca/navigation/NavigationDrawerController.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/flashcard/FlashcardActivity.java`
- `android-app/app/src/main/res/layout/view_navigation_drawer.xml`
- `android-app/app/src/main/res/layout/activity_flashcard.xml`
- `android-app/app/src/main/res/values/strings.xml`
- `android-app/app/src/test/java/com/example/bivoca/navigation/NavigationRoutePolicyTest.java`
- `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, `PROJECT_CONTEXT.md`

## 6. Kiến trúc lưu cài đặt và giá trị mặc định

`SettingsPreferenceManager` dùng SharedPreferences theo tài khoản `bivoca_user_settings_<userId>` cho học tập, âm thanh và nhắc học; cỡ chữ/giảm chuyển động là thiết lập thiết bị trong `bivoca_device_settings`.

| Tùy chọn | Mặc định | Phạm vi |
|---|---:|---|
| Mục tiêu ngày | 10 | tài khoản |
| Mặt trước | English | tài khoản |
| Tự động phát âm | Tắt | tài khoản |
| Tốc độ đọc | 1.0x | tài khoản |
| Nhắc học | Tắt | tài khoản |
| Giờ nhắc | 20:00 theo timezone thiết bị | tài khoản |
| Ngày nhắc | 7 ngày | tài khoản |
| Cỡ chữ | Tiêu chuẩn | thiết bị |
| Giảm chuyển động | Tắt | thiết bị |

Runtime test đổi mục tiêu 10→15 và English→Vietnamese, force-stop/relaunch rồi xác minh 15/Vietnamese vẫn còn. Cuối nghiệm thu đã trả mục tiêu 10, English, auto pronunciation tắt, tốc độ 1.0x, reminder tắt/20:00/7 ngày, cỡ chữ tiêu chuẩn và giảm chuyển động tắt.

## 7. Tích hợp Flashcard, TTS và Display

- English First giữ mặt trước `apple`; Vietnamese First hiển thị `quả táo`, mặt sau trả về `apple`, IPA, từ loại/cấp độ và ví dụ đúng trường dữ liệu.
- Không tạo FlashcardActivity thứ hai; Previous/Next, flip, favorite, Review Today, remembered/not remembered và Quiz giữ nguyên luồng cũ.
- Auto pronunciation chỉ chạy một lần sau khi thẻ mới tải, không đọc nghĩa tiếng Việt và không lặp do LiveData/recreate. Manual TTS vẫn dùng engine hiện có; đặt speech rate trước `speak`, dừng khi Activity rời foreground và dùng flush để tránh chồng âm.
- Emulator không có TTS engine. App hiển thị đúng lỗi “Thiết bị chưa có công cụ đọc…” và không crash/giả báo phát thành công. Do đó logic và error path PASS; âm thanh thực tế chưa xác nhận bằng tai.
- Cỡ chữ lớn áp dụng vào nội dung Flashcard với hệ số tăng trên đơn vị sp, không giảm font scale hệ thống. Reduce Motion và system animator scale 0 đều bỏ animation lật nhưng vẫn đổi mặt rõ ràng.

## 8. Notification và vòng đời lịch nhắc

- `ReminderScheduler` tạo tối đa bảy weekly `RTC_WAKEUP` bằng `setWindow` 15 phút; không xin quyền exact alarm và không cam kết thời điểm tuyệt đối.
- PendingIntent phân biệt user/day, dùng cùng identity khi cập nhật nên đổi giờ/ngày thay lịch cũ, không nhân bản.
- Bật reminder trên API 35 mở hộp quyền `POST_NOTIFICATIONS`; chỉ bật state sau khi quyền được cấp. Từ chối quyền trả switch về tắt.
- Receiver kiểm tra session hiện tại, userId, enabled và day mask trước khi đăng notification; nội dung không chứa dữ liệu cá nhân. Tap notification có PendingIntent về Home.
- Receiver nghe boot, timezone và time change để reconcile. Home cũng reconcile sau login. Logout hủy lịch của user trước khi xóa session.
- Nghiệm thu runtime: cấp quyền PASS; bảy alarm hiện trong `dumpsys alarm`; notification thật với title/body yêu cầu xuất hiện; instrumentation xác nhận active notification ID 4100; reboot Emulator giữ app data và receiver khôi phục lịch; tắt switch tạo bảy `alarm_cancelled` và không còn alarm active.

## 9. Dữ liệu offline

- `CacheSettingsRepository` đọc Room trên worker, không gọi Backend. Kết quả thực tế cuối: **10 topics, 150 flashcards, 430.3 KB** tại thời điểm chụp.
- Dung lượng chỉ cộng database `bivoca-learning-cache.db`, WAL và SHM, không cộng toàn bộ app.
- DAO transaction `clearPublicCache()` chỉ xóa `cached_topics`, `cached_flashcards`, `cache_sync`. Không xóa database, session, avatar, progress, review, XP, streak hoặc server data.
- Nút xóa có dialog xác nhận. Dữ liệu cài app thật không bị xóa khi nghiệm thu; hành vi xóa được kiểm tra bằng Room in-memory riêng.

## 10. Build và test

Các lệnh cuối:

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\android-app"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb shell am instrument -w -e class com.example.bivoca.data.local.RoomLearningCacheTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
adb shell am instrument -w -e class com.example.bivoca.settings.ReminderReceiverTest com.example.bivoca.test/androidx.test.runner.AndroidJUnitRunner
```

Kết quả:

- `assembleDebug`: PASS.
- `testDebugUnitTest`: **89/89 PASS**, 0 failure/error/skipped.
- `lintDebug`: PASS, **0 errors, 268 warnings** toàn project.
- `assembleDebugAndroidTest`: PASS.
- Room instrumentation: **6/6 PASS** trên in-memory DB riêng.
- Reminder instrumentation: **1/1 PASS**; dùng session test tạm, phục hồi session/prefs sau test và không ghi PostgreSQL.
- Tổng instrumentation M12/M8 chạy trong lượt: **7/7 PASS**.

## 11. Emulator acceptance và hồi quy

- Drawer → Cài đặt và sáu nhóm: PASS.
- Back navigation, scroll, tiếng Việt, switch/radio, persistence sau force-stop: PASS.
- Portrait và xoay màn hình: không crash; Activity recreate đọc lại preference. Insets màn chi tiết được sửa sau khi phát hiện header sát system bar.
- English/Vietnamese First, flip và metadata: PASS bằng Room/API data thật.
- Notification permission, scheduling, notification content, reboot reconcile và cancel: PASS.
- Data Settings hoạt động từ Room mà không cần Internet: PASS.
- Home, Topics và Flashcard được mở lại trong nghiệm thu; các route Drawer M11 còn lại không đổi source nghiệp vụ. JWT cũ hết hạn trong lượt cuối và AuthInterceptor đưa app về Login đúng chính sách; không tạo token giả hoặc ghi dữ liệu server để tiếp tục các màn có auth.
- TTS engine thiếu trên Emulator: error handling PASS, âm thanh nghe được chưa kiểm chứng.

## 12. Screenshot thực tế

Thư mục: `android-app/app/build/reports/m12-settings/`

- `01-settings-main.png`
- `02-settings-study.png`
- `03-settings-audio.png`
- `04-settings-reminder.png`
- `05-settings-reminder-time.png`
- `06-settings-display.png`
- `07-settings-data.png`
- `08-settings-about.png`
- `09-flashcard-english-first.png`
- `10-flashcard-vietnamese-first.png`
- `11-notification.png`
- `12-navigation-drawer.png`

Tất cả là screencap trực tiếp từ `emulator-5554`, không dùng ảnh AI. Bộ ảnh được chụp lại sau khi phát hiện ảnh chuyển màn cũ bị chồng frame.

## 13. Lỗi đã sửa, khác biệt demo và rủi ro còn lại

- Sửa lint `paddingHorizontal` không tương thích minSdk 24 bằng `paddingStart`/`paddingEnd`.
- Bổ sung system bar insets cho Settings detail sau kiểm tra UIAutomator.
- Ảnh demo chỉ là định hướng; giao diện dùng asset/resource M11 hiện có, card native và một Activity detail dùng lại thay vì tạo sáu màn lặp.
- Daily goal chưa hiển thị X/Y vì không có dữ liệu “thẻ mới hôm nay” đáng tin cậy.
- Notification là inexact và có thể trễ tối đa theo cơ chế Android; exact alarm không thuộc phạm vi.
- Reminder preference vẫn lưu sau logout nhưng alarm bị hủy. Khi đăng nhập lại cùng tài khoản, Home reconcile và bật lại lịch theo cấu hình đã lưu.
- TTS phụ thuộc engine/voice English của thiết bị; cần thử nghe trên thiết bị có engine.
- Text size M12 áp dụng cho nội dung Flashcard, chưa đổi typography toàn bộ các màn M11.

## 14. Kết luận

M12 Settings có UI và logic thật, lưu bền, áp dụng trực tiếp, notification và cache được kiểm tra trên Emulator, build/test/lint PASS và không làm thay đổi dữ liệu Backend/PostgreSQL. Các giới hạn còn lại được hiển thị và báo cáo đúng, không giả lập chức năng hoặc số liệu.

## 15. Addendum M12.1 — 2026-10-17

Thông tin “Emulator thiếu TTS engine” ở thời điểm nghiệm thu M12 không còn đúng với môi trường hiện tại. Trong M12.1, AVD API 35 có `com.google.android.tts` và `GoogleTtsService`; initialization cùng utterance callback ở 0.8x, 1.0x, 1.2x và auto pronunciation đã PASS. Âm thanh nghe thực tế vẫn giữ **USER AUDIO VERIFICATION PENDING** vì callback không thay thế xác nhận bằng tai. Chi tiết tại `M12_1_SETTINGS_UI_POLISH_REPORT.md`.
