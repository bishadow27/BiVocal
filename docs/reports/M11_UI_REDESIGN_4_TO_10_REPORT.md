# M11 UI REDESIGN — MÀN HÌNH 4 ĐẾN 10

**Ngày nghiệm thu:** 2026-10-15  
**Thiết bị:** AVD `BiVoca_API35_Pixel6`, `emulator-5554`, 1080×2400  
**Trạng thái:** **DONE** — phần triển khai ngày 2026-10-15 được giữ làm lịch sử; Quiz Result E2E đã hoàn tất bằng PostgreSQL riêng trong Addendum M11.1 ngày 2026-10-17.

## Kết quả triển khai

- **Topics:** Dark Night Sky, dữ liệu API/Room thật, card navy và tìm kiếm cục bộ theo tên. Bộ lọc tạo snapshot mới, không sửa source, không phân biệt hoa/thường hoặc dấu tiếng Việt.
- **Flashcard:** giữ API/Room, Previous/Next, TTS, Favorite và Review. Card front/back hiển thị IPA, loại từ, CEFR, nghĩa và ví dụ thật; hiệu ứng lật nhẹ; position/trạng thái lật vẫn giữ qua recreation.
- **Review Today:** tiếp tục dùng `FlashcardActivity` M10; có vị trí, `memoryLevel`, empty/complete/error/pending và chỉ chuyển thẻ sau response thành công.
- **Quiz:** giữ backend grading/state machine. Bốn lựa chọn bị khóa khi SUBMITTING; lỗi gửi trả về QUESTION kèm thông báo; RESULT tô đúng/sai từ response backend.
- **Progress:** accuracy, due/upcoming/reviewedToday, bốn kho từ và chi tiết review đều từ API thật.
- **Leaderboard:** giữ `/gamification/leaderboard?limit=50`; top 3, XP/level/streak; chỉ highlight dòng có `userId` trùng session.
- **Profile + avatar:** dùng API thật cho user/stats. Avatar chọn qua SAF `OpenDocument`, decode có sampling, preview/xác nhận/hủy/reset, lưu app-private theo `userId`. Home/Profile/Drawer dùng chung và ảnh còn sau force-stop/relaunch.

## Avatar và backend

Backend được audit trước khi sửa. `User.avatarUrl` có trong DTO/truy vấn đọc, nhưng Auth chỉ có `POST /register`, `POST /login`, `GET /me`; không có endpoint upload/update/multipart. Avatar vì vậy chỉ lưu tại `files/avatars/user_<userId>.jpg`, không vào Room M8, PostgreSQL hoặc thiết bị khác.

`LocalAvatarStore` từ chối userId không hợp lệ, đọc bounds trước, chọn `inSampleSize`, ghi file tạm rồi rename. Unit test kiểm tra đường dẫn ổn định, cách ly user và sampling ảnh lớn.

## API/DTO giữ nguyên

- Topics/Flashcard public API qua repository hiện có và fallback Room M8.
- `/auth/me`, `/gamification/me`, `/gamification/leaderboard?limit=50`.
- `/progress/summary`, `/progress/schedule`, `/progress/due`.
- `/quiz/questions`, `/quiz/answer`; Android không tự chấm.
- Không sửa Backend, PostgreSQL, migration, Admin Web, JWT, XP/streak hoặc M10.

## File thêm

- `android-app/app/src/main/java/com/example/bivoca/ui/topics/TopicFilter.java`
- `android-app/app/src/main/java/com/example/bivoca/utils/LocalAvatarStore.java`
- `android-app/app/src/main/res/drawable/avatar_circle_background.xml`
- `android-app/app/src/main/res/drawable/dark_card_background.xml`
- `android-app/app/src/test/java/com/example/bivoca/ui/topics/TopicFilterTest.java`
- `android-app/app/src/test/java/com/example/bivoca/utils/LocalAvatarStoreTest.java`

## File chỉnh sửa

- `TopicsActivity`, `FlashcardActivity`, `QuizActivity`, `QuizViewModel`, `ProfileActivity`, `HomeActivity`, `LeaderboardActivity`, `LeaderboardAdapter`, `NavigationDrawerController`.
- `activity_topics.xml`, `item_topic.xml`, `activity_flashcard.xml`, `activity_quiz.xml`, `activity_progress.xml`, `activity_leaderboard.xml`, `item_leaderboard_user.xml`, `activity_profile.xml`, `activity_home.xml`, `view_navigation_drawer.xml`.
- `WORK_STATUS.md`, `ANDROID_CONTEXT.md`, báo cáo này.

## Build và test

```powershell
adb devices -l
Invoke-RestMethod http://127.0.0.1:3000/api/v1/health
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
adb -s emulator-5554 reverse --remove tcp:3000
adb -s emulator-5554 reverse tcp:3000 tcp:3000
```

- Health: `status=ok`, `database=connected`.
- APK: `install -r` Success, không uninstall/clear data.
- Build: PASS.
- Unit tests: **80/80 PASS**, 0 failed/skipped, 16 suites. Baseline 76 + 1 filter + 3 avatar tests. Test cũ tiếp tục bao phủ Flashcard/M8, Due Review M10, Quiz state/navigation, Progress/Leaderboard mapping và Navigation policy.
- Lint: PASS, **0 errors / 185 warnings**.
- ADB reverse cuối lượt: `tcp:3000 tcp:3000`.

## Nghiệm thu Emulator

| Hạng mục | Kết quả |
|---|---|
| Topics online | PASS: 10 topic, search, card dark |
| FOOD Flashcard | PASS: 30 thẻ, front/back, IPA/CEFR/example, navigation/TTS |
| Review Today | PASS read-only: 4 due, memory level thật |
| Quiz câu hỏi | PASS read-only: 10 câu, 4 lựa chọn |
| Quiz Result | Chưa E2E: không gửi answer vì chưa được phép ghi DB vận hành; unit tests cũ PASS |
| Progress | PASS: accuracy 20%, due 4, upcoming 1, reviewedToday 1 |
| Leaderboard | PASS: user 55 highlight bằng userId thật |
| Avatar | PASS picker/preview/save; hiện ở Profile/Home/Drawer và còn sau relaunch |
| Offline M8 | PASS: Topics/FOOD đọc Room, banner Offline; action online không giả thành công |
| Review offline | PASS: yêu cầu kết nối, không tạo kết quả |
| Xoay | PASS smoke; Flashcard giữ state, hamburger landscape `[42,73][168,199]` |
| Drawer regression | PASS routes/selected state/avatar |

JWT cũ hết hạn trong lúc test và app về Login đúng policy. Một JWT ngắn hạn được ký bằng service hiện có cho đúng user `duyphuc` ID 55 để tiếp tục kiểm thử đọc; token không được in/log và không ghi DB.

## Screenshots

Thư mục `android-app/app/build/reports/m11-complete-ui/` có:

- `01-topics.png`
- `02-flashcard-front.png`
- `03-flashcard-back.png`
- `04-review-today.png`
- `05-quiz-question.png`
- `07-progress.png`
- `08-leaderboard.png`
- `09-profile.png`
- `10-avatar-picker.png`
- `11-avatar-updated.png`
- `12-navigation-regression.png`

Không tạo `06-quiz-result.png`: trạng thái cần `POST /quiz/answer`, có thể đổi Quiz/progress/XP trên DB vận hành. Không dùng mock hoặc ảnh dựng thay bằng chứng.

## Lỗi phát hiện và sửa

- Topics card kế thừa surface sáng làm chữ trắng thiếu tương phản: đặt card navy và chụp lại.
- Progress schedule kế thừa chữ tối: đặt màu label/value rõ ràng.
- Leaderboard username dài làm chật row: giới hạn chiều rộng và ellipsize.
- Tìm `dong` ban đầu chưa khớp `Động vật`: bổ sung chuẩn hóa `đ` → `d`, test PASS.
- JWT hết hạn: xác minh đúng policy 401 rồi dùng session ngắn hạn chỉ đọc.

## Kết luận

Code UI/UX màn 4–10 và avatar local đã triển khai; build/test/lint, online read, offline, rotation, navigation và avatar persistence đều PASS. Tại thời điểm nghiệm thu 2026-10-15, Quiz Result E2E còn chờ một môi trường ghi được phép. Phần còn thiếu này đã được hoàn tất trong Addendum M11.1 bên dưới. M8, M9, M10 giữ nguyên DONE.

## Addendum M11.1 — Quiz Result E2E (2026-10-17)

- Tạo PostgreSQL riêng `bivoca_m11_20261017_87faf608e230`, role tối thiểu riêng, 10 chủ đề/300 từ/6 migration; đăng ký user test qua API thật. `flashcard_english` chỉ được chụp snapshot đọc trước/sau.
- Android có build type `e2e`, package riêng và URL `10.0.2.2:3001`; build debug thường vẫn dùng cổng 3000 và dữ liệu cũ không bị thay đổi.
- Hai lượt Quiz đủ 10 câu trên AVD đều 9 đúng/1 sai. Kiểm tra UI lock gửi trùng, mất mạng trước khi gửi, retry thủ công, xoay, Back, Làm lại, về Home và refresh XP/progress đều PASS.
- Snapshot cuối DB test: 21 Quiz result, 18 đúng, 3 sai, 21 progress/review, 180 XP, streak 1. Bản ghi thứ 21 là retry thủ công sau một lần lỗi mạng được xác nhận chưa tới server.
- Màn `KẾT QUẢ QUIZ` hiển thị tổng/đúng/sai/accuracy, nút Làm lại và Về trang chủ đúng theme. Ảnh thật `android-app/app/build/reports/m11-complete-ui/06-quiz-result.png` đã được bổ sung.
- Android `assembleDebug`, `assembleE2e`, unit **91/91**, lint **0 lỗi/268 cảnh báo** PASS. Backend typecheck/build và **313/313** tests PASS; hai database integration suite Quiz/Gamification đều 1/1 PASS.
- Snapshot `flashcard_english` cuối khớp đầu: 3 users, 8 quiz results, 10 progress, 2 scores, 2 XP history, 10 topics, 300 cards, 6 migrations và hash dataset không đổi.
- Giới hạn còn lại: API answer chưa có idempotency key nên timeout mất response sau khi server commit vẫn cần xử lý ở milestone Backend sau. App không tự retry.

Kết quả chi tiết, lệnh và 11 ảnh runtime: `M11_QUIZ_RESULT_E2E_REPORT.md`. Với addendum này, **M11 UI Redesign DONE**.
