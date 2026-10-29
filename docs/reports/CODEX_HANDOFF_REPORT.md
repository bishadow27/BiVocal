# Báo cáo bàn giao dự án BiVoca / BIPASSION cho Codex khác

**Cập nhật:** 14/10/2026 (Asia/Saigon)  
**Thư mục trên máy này:** `C:\Users\phuc\OneDrive\Documents\New project`  
**Mục đích:** Mang báo cáo này sang tài khoản Codex khác để tiếp tục dự án. Đây là bản tóm tắt; source và tài liệu chi tiết vẫn nằm trong các thư mục dự án.

> Không có mật khẩu, JWT, connection string hoặc giá trị `.env` trong báo cáo. Không chia sẻ `.env`, `local.properties`, token hay log chứa bí mật.

## 1. Bắt đầu ở tài khoản Codex mới

Mở project rồi yêu cầu Codex đọc theo thứ tự:

1. `AGENTS.md` — quy tắc bắt buộc.
2. `CODEX_HANDOFF_REPORT.md` — báo cáo này.
3. `WORK_STATUS.md`, `PROJECT_CONTEXT.md`, `BACKEND_CONTEXT.md`, `ANDROID_CONTEXT.md`.
4. Source thật của module sắp sửa; xác minh route/schema/API trước khi sửa contract.

Root hiện không có `.git` (`Test-Path .git` trả false). Kiểm tra lại Git/source control trên máy đích trước khi commit/branch. Hai thư mục `Cu1TH6` và `Cu2TH6` có cấu trúc Android riêng, chưa có bằng chứng thuộc BiVoca; không sửa chúng nếu chưa xác nhận phạm vi.

## 2. Tổng quan dự án và cấu trúc

Tên Android hiện dùng là **BiVoca** (`com.example.bivoca`); Backend/Admin dùng tên **BIPASSION**. Sản phẩm học từ vựng tiếng Anh bằng flashcard, quiz, tiến độ cá nhân, XP/streak/leaderboard và trang Admin.

| Thư mục | Vai trò/công nghệ |
|---|---|
| `backend/` | REST API Node.js 22+, Express 5, TypeScript strict, `pg`, Zod, PostgreSQL. Phân tầng Route → Controller → Service → Repository. |
| `android-app/` | Android Java/XML, MVVM (Activity + ViewModel + LiveData + Repository + Retrofit), package `com.example.bivoca`. Compile/target SDK 37, min SDK 24, Java 11, AGP 9.3.3. Wrapper hiện cấu hình Gradle 9.5.0. |
| `admin-web/` | React 19, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS, Recharts, Vitest. UI dùng API thật, không mock dữ liệu. |
| `dataset/` | CSV/SQL seed và validator cho topics/flashcards. |
| `maintenance/` | Launcher Windows chạy Backend + bản Admin build nền, shortcut mở/dừng/cập nhật. Không migrate DB hoặc reset tài khoản. |
| `Cu1TH6/`, `Cu2TH6/` | Hai project Android riêng tại root; chưa xác nhận là module BiVoca. |

Luồng dữ liệu: Android/Admin → REST API `/api/v1/` → Backend → PostgreSQL. Android không kết nối DB trực tiếp. JSON/API dùng `camelCase`, database dùng `snake_case`.

## 3. Công việc đã hoàn thành

### Backend M1–M6A

- **M1:** API health/topics/flashcards đọc PostgreSQL; topics và flashcards public; danh sách flashcard theo topic code, chỉ thẻ active.
- **M2:** Register/login, bcrypt, JWT Bearer, `GET /auth/me`.
- **M3:** Progress theo user JWT: review nhớ/không nhớ, favorite, list/detail/summary.
- **M4:** Quiz EN→VI nhiều lựa chọn, backend chấm đáp án, history/summary, transaction kết hợp quiz result và progress.
- **M5:** XP, streak, level, XP history, leaderboard; award gắn transaction Quiz/Review, dedup dựa trên constraint DB. Quyết định nghiệp vụ đã thống nhất: giữ `ON DELETE SET NULL` cho liên kết quiz trong XP history và kiểm tra QUIZ ID hợp lệ khi tạo award.
- **M6A:** Role `USER/ADMIN`, admin authorization/API và CLI cập nhật role. Admin API cho dashboard/topics/flashcards/users/progress/quiz stats/leaderboard; không có API xóa topic/card/user hoặc chỉnh XP.
- Tài liệu Backend ghi `npm run typecheck` PASS và `npm test` **311/311** (13 suites). Đây là trạng thái được ghi nhận trước lượt bàn giao; không chạy lại backend tests lần này.

**Trạng thái DB quan trọng:** `backend/README.md` ghi M1–M5 đã apply/pass trên PostgreSQL theo xác nhận trước đó. Migration M6A `1791072004000_add_admin_authorization.sql` **chưa apply** và Admin DB integration **chưa chạy**. 311 unit/API tests không chứng minh migration M6A đã hoạt động trên DB. Không tự apply migration, chạy integration có ghi dữ liệu hay xóa tài khoản nếu chưa xác nhận đúng DB development và cleanup.

### Dataset

- 10 chủ đề: `ANIMALS`, `FAMILY`, `FOOD`, `HEALTH`, `SCHOOL`, `SHOPPING`, `TECHNOLOGY`, `TRAVEL`, `WEATHER`, `WORK`.
- Seed: 30 từ/chủ đề, tổng 300; gồm IPA Anh-Anh, từ loại, ví dụ EN/VI và CEFR.
- `dataset/validation_report.txt` (04/10/2026): 10 topics/300 cards PASS, 30/topic, không duplicate word+meaning/field thiếu/sai mã chủ đề/CEFR/POS/IPA. CEFR: A1 102, A2 84, B1 81, B2 33.
- Không thay schema/seed để né migration.

### Android

- Register/Login lưu session bằng `SessionManager`.
- Home mở Topics, Quiz, Profile, Progress, Leaderboard và Logout.
- Topics tải API thật; chọn chủ đề truyền `topicCode`/`topicName` sang Flashcard.
- Flashcard: lật mặt Anh/Việt, IPA/POS/CEFR, ví dụ, favorite, review, trước/tiếp, mở Quiz và Android Text-to-Speech.
- Quiz: câu hỏi 4 lựa chọn, gửi đáp án backend và hiển thị kết quả.
- Profile/Progress tải thông tin và số liệu thật.
- Leaderboard đã tồn tại và được tái sử dụng (không tạo lớp trùng): `LeaderboardActivity`, `LeaderboardViewModel`, `LeaderboardAdapter`, `LeaderboardRepository`, `LeaderboardUser`. Gọi `GET /api/v1/gamification/leaderboard?limit=50` qua Retrofit đã xác thực; hiển thị rank/tên/XP/level/streak từ DTO backend; có loading, empty, error/retry, scroll và ViewModel giữ request qua xoay.
- Lượt 14/10 thêm `AuthSessionExpiry`: 401 chỉ xóa session nếu request gửi Bearer token và token bị từ chối vẫn là token lưu hiện tại. Ngăn điều hướng Login lặp, đồng bộ thao tác session, đăng nhập mới reset cờ. 403, lỗi mạng/timeout và 5xx không xóa session. Không log JWT/password; OkHttp log BASIC.

### Admin Web và launcher

- `admin-web/README.md` mô tả login/role guard, dashboard, CRUD topic/card (không delete), quản lý user, progress/quiz history read-only, thống kê quiz và leaderboard phân trang.
- Auth xác minh quyền ADMIN bằng API; lỗi auth kết thúc session và clear cache. UI có loading/empty/error, phân trang, validation và query invalidation.
- `maintenance/LOCAL_ADMIN.md`: sau khi cài launcher Windows, mở `http://localhost:5173`; Backend ở `127.0.0.1:3000`; không cần mở terminal mỗi lần. Launcher chạy bản build, không tự cập nhật source và không migrate DB. Sau khi sửa cần build backend/Admin rồi cập nhật launcher.
- `WORK_STATUS.md` đánh dấu M6B hoàn tất. Không build/test Admin Web trong lượt bàn giao này; kiểm tra lại README/evidence trước khi tuyên bố kiểm chứng mới.

## 4. Tác vụ mới nhất và bằng chứng ngày 14/10/2026

Yêu cầu gần nhất là hoàn thiện Leaderboard và global JWT expiry. Leaderboard đã có nên chỉ kiểm tra/tái sử dụng. Source mới sửa/thêm:

- `android-app/app/src/main/java/com/example/bivoca/data/api/AuthInterceptor.java`
- `android-app/app/src/main/java/com/example/bivoca/data/api/AuthenticatedRetrofitClient.java`
- `android-app/app/src/main/java/com/example/bivoca/utils/SessionManager.java`
- Thêm `android-app/app/src/main/java/com/example/bivoca/utils/AuthSessionExpiry.java`
- Thêm test `android-app/app/src/test/java/com/example/bivoca/data/api/AuthInterceptorTest.java`
- Thêm test `android-app/app/src/test/java/com/example/bivoca/data/repository/LeaderboardRepositoryTest.java`
- Mở rộng `android-app/app/src/test/java/com/example/bivoca/data/model/LeaderboardUserTest.java`
- Cập nhật `WORK_STATUS.md`, `ANDROID_CONTEXT.md`.

| Kiểm tra | Kết quả |
|---|---|
| `:app:assembleDebug :app:testDebugUnitTest` | PASS; 20 tests, 0 failures/errors. |
| `:app:lintDebug` | PASS; 0 lint errors, 195 warnings toàn project. |
| APK trên OPPO CPH2557 Android 15 | Cài thành công bằng ADB; dữ liệu/session cũ trên máy được giữ. |
| Home → Leaderboard → Back | Leaderboard mở bằng session có sẵn, hiện một hàng rank từ API, không ở trạng thái lỗi; quay về Home thành công. |
| Home → Topics → Food → Flashcard | Màn hình Flashcard mở, có từ và không có error state. |
| Gọi endpoint leaderboard không token | HTTP 401, xác nhận endpoint cần authentication. |
| TTS | Không thử lại âm thanh ngày 14/10. Yêu cầu M7C mới nhất user đưa nói TTS đã phát thành công trên điện thoại; `WORK_STATUS.md` cũ hơn ghi chưa xác nhận. Ghi nhận user đã xác nhận, chưa có phép thử mới trong lượt gần nhất. |

Unit tests cover DTO JSON, empty response, network failure, HTTP 401/403 và classifier không logout cho 403/500/503. Không làm JWT trên điện thoại hết hạn giả; điều hướng sau expired JWT chưa được xác minh end-to-end bằng token hết hạn thật.

## 5. API chính

- Public: `GET /api/v1/health`, `/topics`, `/topics/{code}/flashcards`, `/flashcards/{id}`.
- Auth: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `GET /api/v1/auth/me`.
- Progress: `/api/v1/progress`, `/progress/summary`, `/progress/{flashcardId}`, POST `/review`, PUT `/favorite`.
- Quiz: `/api/v1/quiz/questions`, POST `/quiz/answer`, `/quiz/history`, `/quiz/summary`.
- Gamification: `/api/v1/gamification/me`, `/xp-history`, `/leaderboard` — router yêu cầu JWT. Leaderboard `limit` 1–100, mặc định 20; response: `rank,userId,username,fullName,avatarUrl,totalXp,level,currentStreak,longestStreak`.
- Admin: `/api/v1/admin/*` yêu cầu JWT + role ADMIN; chi tiết DTO/query/body trong `BACKEND_CONTEXT.md`, `admin-web/README.md`, `backend/src/modules/admin/`.
- Flashcard theo topic trả array `id,english,vietnamese,pronunciation,partOfSpeech,example,exampleVi,level`; metadata có thể null. Endpoint public. Source backend là chuẩn nếu tài liệu/API khác nhau.

## 6. Lệnh kiểm tra/chạy

### Backend

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\backend"
npm install                 # hoặc npm ci từ lockfile
npm run typecheck
npm test
npm run dev                  # cần PostgreSQL sẵn sàng
```

Không chạy `npm run migrate:up` trước khi đọc `backend/README.md` và xác nhận đúng DB. Không in `.env`.

### Android

```powershell
cd "C:\Users\phuc\OneDrive\Documents\New project\android-app"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
```

Thiết bị Android thật qua USB:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" reverse tcp:3000 tcp:3000
```

### Admin Web

Development (hai terminal): `cd backend; npm run dev` và `cd admin-web; npm install; npm run dev`.

Build cho launcher:

```powershell
npm --prefix backend run build
npm --prefix admin-web run build
& .\maintenance\install-local-admin.ps1
```

## 7. Việc còn lại và giới hạn

1. **Việc kế tiếp trong `WORK_STATUS.md`:** Room offline cache cho topics/flashcards; chưa triển khai.
2. **M6A database:** migration Admin và Admin DB integration còn pending theo `backend/README.md`. Nếu làm, xác nhận DB development, đọc integration script/snapshot/cleanup trước.
3. **401 e2e:** unit test policy + backend probe unauthenticated đã chạy; chưa thử expired token end-to-end bằng tài khoản thật.
4. **Lint:** còn 195 warnings toàn project, 0 errors; không mở rộng sửa lan man.
5. **TTS:** user đã nói kiểm thử thật PASS trong yêu cầu M7C mới nhất; lượt này không kiểm tra âm thanh lại.
6. **Accounts:** trước đây user yêu cầu xóa tài khoản test nhưng không có danh sách định danh rõ; **không có thao tác xóa hàng loạt nào được thực hiện**. Không giả định database reset và không xóa account nếu chưa đối chiếu đúng danh tính/phạm vi.
7. Không triển khai lại M7C/Leaderboard, không đổi API/schema/dataset nếu không có lỗi cụ thể. Không dùng dữ liệu mock trong ứng dụng.

## 8. Quy tắc dự án

Đọc source trước khi sửa; giữ kiến trúc; không rewrite phần chạy tốt hoặc tạo lớp trùng; Android Java/XML MVVM; backend là cổng duy nhất đến DB; không lộ secrets; chạy build/test trước khi báo PASS; cập nhật `WORK_STATUS.md`. Xem nguyên văn `AGENTS.md` để có đủ quy tắc.

## 9. Tài liệu nguồn

- `AGENTS.md`
- `WORK_STATUS.md`
- `PROJECT_CONTEXT.md`
- `BACKEND_CONTEXT.md`
- `ANDROID_CONTEXT.md`
- `M7C_FLASHCARD_REPORT_FOR_CHATGPT.md`
- `backend/README.md`, `backend/verification-report.txt`
- `admin-web/README.md`
- `maintenance/LOCAL_ADMIN.md`
- `dataset/README.md`, `dataset/validation_report.txt`
