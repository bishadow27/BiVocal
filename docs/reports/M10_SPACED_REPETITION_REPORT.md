# M10 — Smart Spaced Repetition: báo cáo triển khai và nghiệm thu

**Ngày:** 2026-10-14  
**Trạng thái:** **DONE** theo tiêu chí nghiệm thu M10. Backend, Android, kiểm thử tự động, migration trên DB riêng và luồng end-to-end trên OPPO đều PASS. Sau khi người dùng cho phép riêng, DB vận hành `flashcard_english` đã được sao lưu và chạy migration M10; backend cổng 3000 và Admin Web đã khởi động lại, API M10 trên DB vận hành qua smoke test chỉ đọc.

## 1. Tổng quan và audit mã nguồn

Đã đọc `AGENTS.md`, `WORK_STATUS.md`, `PROJECT_CONTEXT.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`, `CODEX_HANDOFF_REPORT.md`, báo cáo M8/M9, README Backend/Admin rồi đối chiếu route/service/repository, migration, test, Android Java/XML và Room. Backend vẫn theo Route → Controller → Service → Repository → PostgreSQL; Android vẫn Java/XML/MVVM/Retrofit/LiveData. Admin Web, dataset, Room và cơ chế xác thực không đổi.

Trước M10, `user_progress.memory_level` tăng 1 khi đúng (tối đa 5), giảm 1 khi sai (tối thiểu 0); `review_count/correct_count/wrong_count` đếm mọi lượt Review và Quiz. `last_review` có thời gian ghi gần nhất, `next_review` tồn tại nhưng luôn được đặt `NULL`. Dòng chỉ đánh dấu yêu thích có `review_count=0`. Không có trường `box` trong model Android: `ProgressDto` ánh xạ `memoryLevel` từ API. `FlashcardRepository` và Room chỉ cache nội dung công khai.

Review gọi `upsertProgressReview` trong transaction cùng `recordLearningActivity`; Quiz chấm điểm rồi dùng chính helper đó trong transaction với quiz result và XP. PostgreSQL `ON CONFLICT` tuần tự hóa cập nhật cùng user/card. Review cấp 5 XP tối đa một lần/card/ngày; Quiz đúng cấp 10 XP theo quiz result, sai 0; streak dựa ngày UTC. Các quy tắc này được giữ nguyên.

## 2. Thuật toán và tương thích dữ liệu cũ

Đây là lịch Leitner đơn giản theo lựa chọn đúng/sai, **không phải SM-2**:

| Kết quả | Mức ghi nhớ sau ghi | Lịch kế tiếp |
|---|---:|---|
| Đúng | tăng 1, tối đa 5 | mức 1/2/3/4/5: 1/3/7/14/30 ngày |
| Sai | giảm 1, tối thiểu 0 | sau 10 phút |

Lịch được tính từ `clock_timestamp()` ở Backend khi câu trả lời được ghi thành công; Android không tính ngày hoặc XP. Review và Quiz dùng một SQL helper để không lệch lịch. `last_review`/`next_review` được lưu dưới dạng `TIMESTAMPTZ`, API trả ISO UTC. `reviewedToday` là ngày UTC; giao diện ghi rõ UTC. Nếu transaction thất bại, cả tiến độ, lịch và XP không được commit.

Dữ liệu cũ có `review_count>0` nhưng `next_review IS NULL` được coi là **đến hạn ngay** lúc đọc, không backfill tùy tiện và không sửa lịch sử. Favorite-only và từ chưa học không vào hàng ôn. Chỉ card active được hiển thị; mỗi truy vấn gắn `user_id` từ JWT, không chấp nhận user ID phía client.

## 3. Database, API và Android

Migration mới `backend/migrations/1791964800000_schedule_progress_reviews.sql` đổi đúng hai cột thời gian Review sang `TIMESTAMPTZ` bằng timezone session của PostgreSQL cũ, thêm partial index cho truy vấn đến hạn. Down migration bỏ index và đổi về kiểu cũ. Không thêm cột trùng nghĩa, không thêm bảng lịch sử. DB `flashcard_english` đã chạy migration này **sau khi người dùng cho phép riêng**; chi tiết xác minh ở mục 4.

Hai API đọc được thêm vào router Progress có JWT:

- `GET /api/v1/progress/due?limit=1..300&topicCode=FOOD` (limit mặc định 100) trả card đã học và đến hạn, gồm nội dung công khai, `topicCode`, `memoryLevel`, `reviewCount`, `wrongCount`, `lastReview`, `nextReview`. Sắp theo lịch cũ NULL trước, ngày đến hạn, số lần sai giảm dần, ID.
- `GET /api/v1/progress/schedule` trả `trackedCards`, `reviewedCards`, `dueNow`, `upcoming24Hours`, `reviewedToday` và `memoryLevels[0..5]`. Favorite-only nằm trong tracked nhưng không vào reviewed/due/phân bố mức.

`POST /progress/:flashcardId/review` và `POST /quiz/answer` giữ contract cũ; `nextReview` trong response nay có ISO timestamp sau lượt ghi thành công. Không tạo endpoint ghi mới; summary Progress và Admin hiện tại giữ shape cũ.

Android thêm nút **Ôn tập hôm nay** ở Home và tái sử dụng `FlashcardActivity`. Chế độ M10 tải tối đa 300 card đến hạn qua `ProgressRepository`, hiện mức ghi nhớ, mặt trước/sau, ví dụ, TTS và hai lựa chọn. `FlashcardViewModel` giữ call và pending state qua xoay; một thẻ chỉ rời hàng sau callback ghi thành công, lỗi giữ thẻ với thông báo thử lại, callback cũ bị bỏ qua. Khi hết danh sách hiển thị hoàn thành lượt ôn; rỗng ban đầu hiển thị không có từ đến hạn. Progress hiện số đến hạn/sắp đến hạn 24 giờ/đã ôn trong ngày UTC theo API. Dữ liệu due và kết quả chưa đồng bộ không ghi Room; flashcard public offline M8 không đổi.

## 4. Kiểm thử và bằng chứng

Người dùng đã cho phép tạo DB riêng **`bivoca_m10_20261014_a18cbe62d430`**, seed dataset và kiểm thử có ghi. Harness `backend/.m10-verification/isolated.mjs` từ chối DB nguồn khác `flashcard_english`, từ chối tái dùng tên DB đã tồn tại, chỉ đọc catalog nguồn; mọi seed, migration, fixture và cleanup đều ở DB M10. DB riêng đã có 10 topics, 300 cards, 6 migrations. Fixture thời gian cũ kiểm tra epoch trước/sau Up, Down trả lại timestamp cũ, Up lại: PASS. Ngay sau cleanup của harness: 0 users, 0 progress. Sau đó người dùng đăng nhập/đăng ký `duy` (ID 18) để kiểm thử trên điện thoại và cho phép giữ tài khoản này cùng dữ liệu; fixture ID 17 do agent tạo đã được xóa đúng ID. DB thử nghiệm được giữ lại như đã cho phép.

Lệnh thực tế và kết quả:

| Kiểm tra | Kết quả |
|---|---|
| `npm run typecheck`, `npm run build` tại `backend` | PASS |
| `npm test` tại `backend` | **313/313 PASS**, 0 FAIL |
| `M10_ALLOW_ISOLATED_DATABASE=1 node .m10-verification/isolated.mjs create` | Migration Up/Down/Up, bảo toàn dữ liệu cũ, cleanup PASS |
| `M10_ALLOW_ISOLATED_DATABASE=1 node .m10-verification/isolated.mjs test` | **6/6 integration files PASS**: Auth, Progress, Quiz, Gamification, Admin, M10; 0 users/0 progress sau cleanup |
| `M10_INTEGRATION_ALLOW_WRITES=1` chạy riêng `m10.database.integration.ts` sau bổ sung ranh giới UTC/mức cao | **1/1 PASS** |
| `npm test` tại `admin-web` | **42/42 PASS**, không sửa Admin Web |
| `:app:assembleDebug`, `:app:testDebugUnitTest`, `:app:lintDebug` | Build PASS, **60/60 unit tests PASS**, lint **0 lỗi/203 cảnh báo** |

Test PostgreSQL M10 xác minh JWT thiếu/bị giả, user isolation, favorite-only, legacy NULL, limit/topic validation, lịch đúng/sai, ranh giới ngày UTC, Quiz dùng cùng lịch, 6 concurrent Review cùng card, XP một lần/card/ngày, rollback lỗi sau tiến độ và cleanup chính xác. Test Android M10 xác minh loading/rỗng/retry, request cũ, không dùng Room public cho due, không gửi Review trùng khi pending, lỗi giữ thẻ, callback thành công chuyển thẻ, completion và trạng thái được ViewModel giữ khi Activity tạo lại. Chưa chạy instrumentation M10; guard giữ APK ở `android-app/gradle.properties` không đổi.

Gradle wrapper ban đầu thử tải lại bản 9.5.0 và JDK 25 nhưng network sandbox chặn. Chạy lại với JDK 25 cài trong Android Studio và quyền tải dependency được phép thì build/test/lint PASS. Đây là lỗi môi trường build, không phải lỗi mã nguồn.

Sau lần rời ADB đầu tiên, người dùng đã cắm lại OPPO CPH2557 và chấp nhận USB debugging. `adb devices -l` trở về trạng thái `device`; `adb install -r android-app/app/build/outputs/apk/debug/app-debug.apk` trả `Success`, giữ dữ liệu ứng dụng. Backend M10 được chạy tạm ở `127.0.0.1:3001` trên DB riêng và điện thoại được nối bằng `adb reverse tcp:3000 tcp:3001`. Không chạy instrumentation, uninstall, clear data hoặc ghi vào `flashcard_english` trong M10.

**Nghiệm thu OPPO trên DB M10 riêng:**

| Bước | Bằng chứng và kết quả |
|---|---|
| Home → Ôn tập hôm nay khi chưa có lịch | Màn hình rỗng từ API thật; Progress hiện 0 đến hạn/0 sắp đến hạn/0 đã ôn hôm nay: PASS |
| FOOD → `apple` → lật → Chưa nhớ | Sang `bread` 2/30 chỉ sau lưu thành công. DB: `apple` mức 0, 1 lượt sai, `next_review` sau 10 phút: PASS |
| Đến hạn, lật và xoay ngang/dọc | Chỉ tại DB M10 riêng, đưa lịch `apple` về quá khứ 1 giây để kiểm thử. Hàng due hiện `apple` 1/1, mặt sau `quả táo`; xoay vẫn giữ thẻ và mặt sau: PASS |
| Đã nhớ trên due | Người dùng xác nhận đã nhấn cho `apple`. DB: mức 1, 2 lượt review, lần tiếp theo sau 1 ngày; hàng due rỗng: PASS |
| Thử lỗi mạng và retry | `bread` được đưa đến hạn tương tự trên DB riêng. Sau `adb reverse --remove tcp:3000`, nhấn Đã nhớ: UI giữ `bread` 1/1 và báo lỗi kết nối, DB vẫn 1 lượt review/mức 0. Khôi phục reverse tới 3001, nhấn lại: màn hình hoàn thành, DB 2 lượt review/mức 1, hẹn sau 1 ngày: PASS |
| Progress và XP | Progress hiện 0 đến hạn, 2 sắp đến hạn, 2 đã ôn hôm nay (UTC). DB có 2 progress, tổng 10 XP, mỗi card chỉ một award Review trong ngày: PASS |
| Hồi quy M8 offline | Ngắt reverse, force-stop/relaunch giữ data: Topics và FOOD/FAMILY đã cache đọc từ Room, banner Offline; Favorite bị khóa. Due báo cần kết nối và có Retry. Nối lại, Retry trả trạng thái không còn từ đến hạn: PASS |

Thao tác thời gian chỉ cập nhật `next_review` trên đúng `user_id=18`, `flashcard_id=1/2`, `review_count=1` trong DB M10 riêng; không chỉnh đồng hồ điện thoại hay DB vận hành. Âm thanh TTS chưa được xác nhận bằng tai trong lượt này; M10 dùng nút TTS đã có. Không chạy một lượt Quiz thủ công trên OPPO trong M10; Quiz dùng cùng lịch được xác minh bằng PostgreSQL integration.

Kết thúc: trên OPPO trở về Login sau khi đăng xuất phiên DB thử nghiệm; dữ liệu Room/app còn nguyên. `adb reverse tcp:3000 tcp:3000` đã khôi phục và `adb reverse --list` xác nhận, backend gốc `/api/v1/health` HTTP 200; server M10 tạm cổng 3001 đã dừng. Tài khoản `duy` và hai progress/XP vẫn ở DB M10 riêng theo cho phép của người dùng.

Lệnh thiết bị chính đã chạy (ADB từ `$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe`): `adb devices -l`; `adb install -r android-app/app/build/outputs/apk/debug/app-debug.apk`; `adb reverse tcp:3000 tcp:3001`; `adb reverse --remove tcp:3000`; `adb shell am force-stop com.example.bivoca`; `adb shell input keyevent`/`input tap` để điều hướng, lật, chọn đáp án và xoay màn hình qua `adb shell settings put system user_rotation`; `adb reverse tcp:3000 tcp:3000`; `adb reverse --list`. Đã trả `user_rotation=0`, trùng giá trị ban đầu. Các câu SQL kiểm thử lịch chỉ chạy qua pool với DB_NAME cố định là DB M10 riêng và điều kiện chính xác user/card/review_count nêu trên.

### Triển khai DB vận hành sau khi được cho phép

Người dùng đã cho phép sao lưu, chạy migration M10 trên `flashcard_english`, khởi động lại backend cổng 3000 và kiểm tra API/app. Trước migration, preflight đọc catalog: 5 migrations; `last_review`/`next_review` đều là `timestamp without time zone`; TimeZone `Asia/Bangkok`; 10 topics, 300 flashcards, 3 users, 4 progress, 3 quiz results, 1 score, 1 XP history. Bản dump custom tạo bằng PostgreSQL 18 `pg_dump` tại `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m10_20261014.dump` (50.814 bytes, SHA-256 `c79d3e9db6396390daee24cf6573003515a22089ca983bcbd456028a167152ac`); `pg_restore --list` đọc được TOC. File nằm ngoài OneDrive/repository và không bị in nội dung.

Launcher BiVoca xác nhận quản lý backend PID 20616. Đã `node maintenance/local-admin.mjs --stop` rồi kiểm tra cả cổng 3000/5173 dừng; `npm run migrate:up` trả `Migration up completed`; postcheck xác nhận 6 migrations, hai cột là `timestamp with time zone`, index `idx_user_progress_due` có trong catalog, số bản ghi của bảy bảng trên không đổi. Khởi động lại launcher ẩn và trạng thái `frontendReady=true`, `backendMode=managed`; `/api/v1/health`, `/api/v1/topics`, `http://127.0.0.1:5173/` đều HTTP 200.

Smoke test chỉ đọc trên backend cổng 3000: `GET /progress/due` không JWT trả 401; với JWT tạm tạo trong bộ nhớ từ secret local cho một user có progress, `GET /progress/due?limit=300`, `/progress/schedule`, `/progress/summary` đều 200; có 3 thẻ đến hạn và bằng `schedule.dueNow`. JWT không được ghi vào file/log. `npm run test:integration` trên DB vận hành **5/5 PASS**, chỉ chạy SELECT/GET. `adb reverse --list` cuối lượt là `tcp:3000 tcp:3000`. Không ghi một lượt Review/Quiz mới vào DB vận hành; E2E ghi lịch trên OPPO đã PASS ở DB M10 riêng.

## 5. File thay đổi

Backend: `backend/migrations/1791964800000_schedule_progress_reviews.sql`; `backend/src/modules/progress/{progress.types,progress.schema,progress.repository,progress.service,progress.controller,progress.route}.ts`; `backend/tests/{progress.test,quiz.test,gamification-repository.test,quiz-repository.test,progress.database.integration,quiz.database.integration,gamification.database.integration,m10.database.integration}.ts`; `backend/.m10-verification/isolated.mjs`.

Android: `data/api/ProgressApi.java`, `data/repository/ProgressRepository.java`, `data/model/{Flashcard,ReviewSchedule}.java`, `ui/flashcard/{FlashcardActivity,FlashcardViewModel}.java`, `ui/home/HomeActivity.java`, `ui/progress/{ProgressActivity,ProgressViewModel}.java`, `res/layout/{activity_flashcard,activity_home,activity_progress}.xml`, `res/values/strings.xml`, `app/src/test/java/com/example/bivoca/ui/flashcard/DueReviewViewModelTest.java` dưới `android-app/app/src/main` tương ứng.

Tài liệu: `WORK_STATUS.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`, báo cáo này. Công cụ xác minh/sao lưu local: `backend/.m10-verification/live-deploy.mjs` và snapshot counts/catalog `backend/.m10-verification/live-preflight.json`. Không sửa `PROJECT_CONTEXT.md` vì kiến trúc hệ thống không đổi.

## 6. Giới hạn còn lại

1. OPPO đã được đăng xuất khỏi phiên DB M10 riêng trước khi chuyển lại cổng 3000. Người dùng có thể đăng nhập tài khoản ở DB vận hành để dùng M10; chưa thực hiện một lượt Review/Quiz **ghi** trên DB vận hành, nhằm giữ tiến độ thật. Lịch ghi trên OPPO đã được nghiệm thu với DB riêng; API đọc trên DB vận hành đã smoke PASS.
2. Âm thanh TTS chưa được xác nhận bằng tai; M10 dùng lại nút TTS có sẵn. Không có push notification, WorkManager hay đồng bộ lịch khi offline trong phạm vi M10.
