# M11.1 — QUIZ RESULT E2E VÀ POSTGRESQL RIÊNG

**Ngày nghiệm thu:** 2026-10-17  
**Thiết bị:** AVD `BiVoca_API35_Pixel6`, `emulator-5554`, API 35  
**Trạng thái:** **DONE**  
**Phạm vi:** hoàn tất luồng Quiz Result thật từ Android → REST API → PostgreSQL riêng, không ghi vào `flashcard_english`.

## 1. Mục tiêu

Hoàn tất phần còn thiếu của M11 UI màn 4–10: chạy Quiz thật, nhận kết quả chấm từ Backend, hiển thị tổng kết, đối chiếu lịch sử/progress/XP/streak trong PostgreSQL và kiểm tra các tình huống gửi trùng, mất mạng, xoay màn hình, làm lại và quay về Home.

## 2. Trạng thái trước M11.1

`QuizActivity` đã tải câu hỏi và gửi đáp án thật, nhưng màn cuối chỉ có dòng “Hoàn thành!” và tỷ số đúng/tổng. Chưa có ảnh `06-quiz-result.png`, chưa được phép ghi Quiz vào DB vận hành nên M11 UI Redesign còn `IN PROGRESS`. Home cũng không tải lại profile/progress khi quay về từ Quiz, vì vậy XP có thể cũ cho đến khi mở lại màn.

## 3. Source và API đã kiểm tra

- Android giữ MVVM: `QuizActivity` → `QuizViewModel` → `QuizRepository` → `QuizApi`.
- Backend giữ Route → Controller → Service → Repository → PostgreSQL.
- `GET /api/v1/quiz/questions` trả 10 câu hỏi, mỗi câu 4 lựa chọn.
- `POST /api/v1/quiz/answer` chấm điểm ở Backend, ghi `quiz_results`, cập nhật `user_progress` và gọi gamification trong cùng transaction.
- `GET /api/v1/quiz/history`, `/quiz/summary`, `/gamification/me`, `/progress/summary`, `/progress/schedule` được dùng để đối chiếu.
- Đáp án đúng nhận 10 XP tối đa một lần cho cùng user/source/card/ngày theo unique index. Đáp án sai nhận 0 XP nhưng vẫn là hoạt động học cho streak.
- API answer không trả số XP trong response; Android không tự đoán hay hiển thị XP giả. XP được xác minh qua API gamification/progress và DB.

## 4. Cấu hình Backend test

Thêm script `backend/.m11-verification/e2e.mjs` để tạo, đăng ký, chụp snapshot và xác minh có thể lặp lại. Thông tin nhạy cảm nằm trong file `.env.m11` đã bị `.gitignore` loại trừ; báo cáo và log không chứa mật khẩu, JWT hay connection string.

Backend test chạy tạm trên `127.0.0.1:3001`. Backend vận hành tiếp tục chạy trên `127.0.0.1:3000` trong suốt quá trình.

## 5. Cách ly PostgreSQL

- Database test: `bivoca_m11_20261017_87faf608e230`.
- Role riêng: `bivoca_m11_87faf608e230`, chỉ sở hữu DB test; không có quyền superuser, createdb hay createrole.
- Dataset: 10 chủ đề, 300 flashcard, 6 migration.
- Snapshot trước E2E: 0 Quiz, 0 progress/review, 0 XP, streak 0.
- DB test được giữ lại sau nghiệm thu làm bằng chứng.

Không chạy migration, seed, INSERT, UPDATE hoặc DELETE trên `flashcard_english`.

## 6. Tạo tài khoản test

Tài khoản test được đăng ký qua `POST /api/v1/auth/register` thật trên Backend test. User ID trong DB test là 5. Username, email, mật khẩu và JWT không được ghi vào báo cáo.

## 7. Luồng Emulator

Tạo build type `e2e` với package `com.example.bivoca.e2e` và base URL `http://10.0.2.2:3001/api/v1/`. Build debug bình thường vẫn dùng resolver cũ ở cổng 3000 và package `com.example.bivoca`; dữ liệu ứng dụng thường không bị đụng tới.

APK E2E được cài bằng `adb install -r`. Đăng nhập qua API thật, mở Drawer → Làm Quiz, làm đủ hai lượt Quiz 10 câu và kiểm tra các màn Home/Progress sau đó.

### Ma trận nghiệm thu và mức bằng chứng

| Hạng mục | Kết quả | Bằng chứng |
|---|---|---|
| Test Backend health | PASS | API INTEGRATION PASS |
| Test PostgreSQL connection | PASS | API INTEGRATION + DB SNAPSHOT PASS |
| Android kết nối Test API | PASS | EMULATOR E2E PASS |
| Login bằng Test User | PASS | EMULATOR E2E PASS |
| Quiz questions | PASS | EMULATOR E2E PASS |
| Chọn đáp án | PASS | EMULATOR E2E PASS |
| Submit answer | PASS | EMULATOR E2E + DB PASS |
| Backend grading | PASS | API INTEGRATION + DB PASS |
| Chuyển câu hỏi | PASS | EMULATOR E2E PASS |
| Hoàn thành Quiz | PASS | EMULATOR E2E PASS |
| Quiz Result UI | PASS | EMULATOR E2E PASS |
| XP consistency | PASS | API INTEGRATION + DB PASS |
| Progress consistency | PASS | API INTEGRATION + DB PASS |
| Quiz history | PASS | API INTEGRATION + DB PASS |
| Retry/error handling | PASS với giới hạn timeout nêu ở mục 17/27 | EMULATOR E2E + UNIT TEST PASS |
| Back navigation | PASS cho QUESTION/RESULT/final; SUBMITTING được khóa theo state machine | EMULATOR E2E + SOURCE/UNIT VERIFIED |
| Quiz replay | PASS | EMULATOR E2E + DB PASS |
| Navigation Drawer regression | PASS | EMULATOR E2E PASS |

Các nhãn trên phân biệt rõ bằng chứng source/unit, API/DB integration và thao tác thật trên Emulator; không suy từ build để đánh dấu E2E PASS.

## 8. Tải câu hỏi

PASS: mỗi lượt tải đúng 10 câu, mỗi câu có 4 lựa chọn từ API test. Không dùng mock. Ảnh câu hỏi trước khi chọn nằm trong thư mục bằng chứng.

## 9. Gửi đáp án

PASS: chọn đáp án chuyển state `QUESTION → SUBMITTING → RESULT`; các nút lựa chọn bị khóa trong lúc gửi. Android chỉ sang câu tiếp theo sau khi có response Backend.

## 10. Chấm đúng/sai

PASS: highlight đáp án đúng và đáp án người dùng chọn dựa trên response Backend. Hai lượt đầy đủ đều đạt 9 đúng/1 sai. Android không tự giữ answer key và không tự chấm.

## 11. Giao diện Quiz Result

Màn cuối được hoàn thiện theo Dark Night Sky, gồm:

- tiêu đề `KẾT QUẢ QUIZ` và trạng thái hoàn thành;
- Tổng số câu, Số câu đúng, Số câu sai, Độ chính xác;
- nút `Làm lại` và `Về trang chủ` màu cyan đồng bộ;
- kết quả 9/10 hiển thị chính xác 90%.

`QuizResultSummary` gom phép tính và kiểm tra counter hợp lệ; có unit test cho tính toán, làm tròn và input sai.

## 12. Đối chiếu Backend

Sau hai lượt đầy đủ và một lần retry thủ công thành công ở bài kiểm tra mạng, API/DB cùng trả:

| Chỉ số | Kết quả |
|---|---:|
| Tổng Quiz result | 21 |
| Đúng | 18 |
| Sai | 3 |
| Accuracy | 85,71% |
| History | 21 bản ghi |

Hai lượt Quiz đầy đủ tạo 20 bản ghi. Bản ghi thứ 21 là lần gửi lại thủ công sau khi lần trước thất bại chắc chắn trước khi tới server.

## 13. Progress

PASS: `user_progress` có 21 hàng, tổng 21 lượt review, 18 đúng và 3 sai. API summary trả tracked/reviewed 21, accuracy 85,71%. Schedule trả dueNow 1, upcoming 20, reviewedToday 21 và phân bố memory level `[3,18,0,0,0,0]`.

## 14. XP

PASS: 18 đáp án đúng tạo 18 dòng XP, tổng 180 XP. `/gamification/me` trả level 2 và 80 XP trong level hiện tại. Các đáp án sai không cộng XP.

## 15. Streak

PASS: current streak 1, longest streak 1, ngày hoạt động cuối 2026-10-17. Kết quả khớp giữa DB và gamification API.

## 16. Chống gửi trùng

PASS ở UI: sau khi một câu đã sang RESULT, chạm nhanh 5 lần không tạo thêm bản ghi; DB vẫn tăng đúng một. Source/state test cũng xác nhận SUBMITTING khóa toàn bộ lựa chọn.

Backend hiện cho phép nhiều lần trả lời cùng card ở các request độc lập. Vì API chưa có idempotency key, client chỉ chặn thao tác đồng thời và không tự retry request không rõ kết quả.

## 17. Mất mạng và retry

Tắt Backend test trước khi submit:

- UI báo `Không kết nối được máy chủ. Kiểm tra kết nối rồi thử lại.`;
- giữ nguyên câu hỏi;
- DB không đổi ở 20 bản ghi;
- app không tự gửi lại.

Sau khi Backend hoạt động lại, người dùng gửi lại thủ công và DB tăng đúng một lên 21. Trường hợp response bị mất sau khi server đã commit vẫn còn rủi ro gửi lặp nếu người dùng thử lại; cần idempotency key phía Backend để giải quyết triệt để.

## 18. Xoay màn hình

PASS: xoay khi đang ở câu 1/10 giữ nguyên prompt, số thứ tự và state; không crash, không tự submit. Cuối lượt đã trả `accelerometer_rotation=1`, `user_rotation=0`, font scale 1.0.

## 19. Back, Làm lại và về Home

- `Làm lại` tải lại câu 1/10; trước khi chọn đáp án mới DB không đổi.
- Back ở state kết quả câu đưa về màn trước theo navigation hiện có, không ghi thêm DB.
- `Về trang chủ` mở Home; Home nay refresh profile/progress trong `onResume`, hiển thị XP 180, streak 1 và tracked 20 sau hai lượt đầy đủ.

## 20. Android build

Lệnh cuối:

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\android-app"
.\gradlew.bat :app:assembleDebug :app:assembleE2e :app:testDebugUnitTest :app:lintDebug --console=plain
```

Kết quả: `BUILD SUCCESSFUL`; cả `assembleDebug` và `assembleE2e` PASS. BuildConfig đã xác minh: debug không override URL, E2E dùng cổng 3001.

## 21. Android unit tests

**91/91 PASS**, 21 suites, 0 failure, 0 error, 0 skipped. Hai test mới bao phủ summary/accuracy và counter không hợp lệ; toàn bộ test cũ tiếp tục PASS.

## 22. Android lint

`:app:lintDebug` PASS: **0 lỗi / 268 cảnh báo**. Sáu hardcoded string mới đã chuyển sang resource nên không tăng baseline cảnh báo.

## 23. Backend tests

Các lệnh đã chạy:

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\backend"
npm run typecheck
npm run build
npm test
npx vitest run src/modules/quiz/quiz.database.integration.ts
npx vitest run src/modules/gamification/gamification.database.integration.ts
```

- Typecheck: PASS.
- Build: PASS.
- Unit/integration suite: **313/313 PASS**, 13 suites, 0 fail/skipped.
- Quiz database integration: **1/1 PASS**, gồm concurrency, transaction rollback và cleanup.
- Gamification database integration: **1/1 PASS**, gồm award, streak, concurrency, rollback và cleanup.

400/401/500 và repository/service error path được bao phủ bằng test Backend/Android hiện có. 403 không thuộc response contract của route Quiz cho user đã xác thực và không được dựng giả trong E2E.

## 24. Ảnh nghiệm thu

Thư mục `android-app/app/build/reports/m11-quiz-e2e/`:

1. `01-test-login.png`
2. `02-quiz-question.png`
3. `03-quiz-selected-answer.png`
4. `04-quiz-correct-answer.png`
5. `05-quiz-wrong-answer.png`
6. `06-quiz-result.png`
7. `07-quiz-result-details.png`
8. `08-home-after-quiz.png`
9. `09-progress-after-quiz.png`
10. `10-quiz-replay.png`
11. `11-navigation-regression.png`

`android-app/app/build/reports/m11-complete-ui/06-quiz-result.png` cũng đã được bổ sung từ màn chạy thật. Không có ảnh mock.

## 25. Lỗi tìm thấy

1. Màn kết quả thiếu chi tiết tổng/sai/accuracy.
2. Nút `Về trang chủ` còn màu Material tím, lệch palette M11.
3. Home giữ XP/progress cũ khi quay lại từ Quiz vì không refresh trong `onResume`.
4. API không có idempotency key nên không thể đảm bảo exactly-once khi client mất response sau commit.

## 26. Sửa tối thiểu

- Mở rộng đúng `QuizActivity` và `activity_quiz.xml`; thêm lớp tính thuần `QuizResultSummary` cùng unit test.
- Dùng resource string và màu/state list sẵn có.
- Cho `HomeActivity` reload profile/progress ở lần resume sau khi trở lại màn.
- Thêm URL override theo build type để E2E dùng DB riêng mà debug bình thường không đổi.
- Không sửa Backend business logic, migration, schema, M8 Room, M10 spaced repetition hay M12 Settings.

## 27. Giới hạn còn lại

- Chưa có idempotency key cho `POST /quiz/answer`; timeout sau commit vẫn cần Backend thay đổi contract ở milestone sau.
- E2E chạy trên Emulator, không lặp lại trên OPPO trong lượt này.
- Build E2E và DB test chỉ dành cho kiểm thử local, không dùng phát hành.

## 28. DB vận hành không đổi

Snapshot cuối `flashcard_english` khớp hoàn toàn snapshot đầu:

| Bảng/chỉ số | Trước | Sau |
|---|---:|---:|
| users | 3 | 3 |
| quiz_results | 8 | 8 |
| user_progress | 10 | 10 |
| user_scores | 2 | 2 |
| xp_history | 2 | 2 |
| topics | 10 | 10 |
| flashcards | 300 | 300 |
| migrations | 6 | 6 |

Hash nội dung topics và flashcards cũng trùng snapshot đầu. Script `production-check` trả `unchanged: true`.

## 29. Trạng thái môi trường cuối

- Backend vận hành cổng 3000: vẫn hoạt động, health `ok`, database `connected`.
- Backend test cổng 3001: đã dừng.
- DB test và evidence giữ lại.
- App E2E đã force-stop nhưng vẫn cài; app debug thường và dữ liệu của nó giữ nguyên.
- Emulator còn online; rotation/font scale đã phục hồi.

## 30. Kết luận

M11.1 Quiz Result E2E **DONE**. Quiz đã được nghiệm thu xuyên suốt bằng API và PostgreSQL riêng, UI kết quả hoàn chỉnh, dữ liệu Quiz/progress/XP/streak khớp, Home refresh đúng, build/test/lint đều PASS và DB vận hành được chứng minh không đổi. Vì đây là hạng mục cuối còn thiếu của báo cáo UI màn 4–10, **M11 UI Redesign được chuyển sang DONE**. M8, M9, M10, M12 và M12.1 giữ nguyên trạng thái trước đó.

## File thêm và chỉnh sửa

### Thêm

- `android-app/app/src/main/java/com/example/bivoca/ui/quiz/QuizResultSummary.java`
- `android-app/app/src/test/java/com/example/bivoca/ui/quiz/QuizResultSummaryTest.java`
- `backend/.m11-verification/e2e.mjs`
- `backend/.m11-verification/state.json`
- `M11_QUIZ_RESULT_E2E_REPORT.md`

### Chỉnh sửa

- `android-app/app/build.gradle.kts`
- `android-app/app/src/main/java/com/example/bivoca/data/api/ApiBaseUrl.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/quiz/QuizActivity.java`
- `android-app/app/src/main/java/com/example/bivoca/ui/home/HomeActivity.java`
- `android-app/app/src/main/res/layout/activity_quiz.xml`
- `android-app/app/src/main/res/values/strings.xml`
- `M11_UI_REDESIGN_4_TO_10_REPORT.md`
- `WORK_STATUS.md`
- `ANDROID_CONTEXT.md`

