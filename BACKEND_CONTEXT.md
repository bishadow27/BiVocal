# BACKEND_CONTEXT.md — Chi tiết Kỹ thuật Backend REST API BiVoca

> Các báo cáo milestone nằm trong `docs/reports/`; xem [mục lục](README.md). Tên báo cáo không kèm thư mục ở các mục lịch sử được tra trong thư mục này.

**Ngày cập nhật:** 2026-10-24  
**Trạng thái kiểm tra M9:** typecheck, build PASS; 311/311 tests PASS; 5/5 integration chỉ đọc trên DB gốc; 10/10 integration trên DB test riêng; 68/68 HTTP checks trên server thật. Chi tiết: `M9_INTEGRATION_SECURITY_REPORT.md`.

---

## M14 Google Sign-In & Password Recovery — GOOGLE_PRODUCTION_DONE / PASSWORD_RECOVERY_LOCAL_DONE / PASSWORD_RECOVERY_PUBLIC_HTTPS_PENDING

- Endpoint: Google challenge/login/link, forgot/reset password; `GET /reset-password` same-origin.
- Google token xác minh bằng `google-auth-library` với Web Client audience, verified email và
  nonce challenge lưu hash. Identity dùng Google `sub`; email collision không tự merge.
- Migration 9 thêm `auth_version`, nullable password cho Google-only user,
  `user_auth_identities`, `google_auth_challenges`, `password_reset_tokens`.
- Reset token 32 byte chỉ lưu SHA-256, 20 phút, một lần; reset tăng auth version để thu hồi JWT
  cũ. Rate limiter áp dụng cho challenge/Google/Forgot/Reset.
- Backup `flashcard_english_before_m14_20261023152956.dump` đã restore-check lại vào
  `bivoca_m14_restore_final_20261023`; checksum/counts khớp. Migration M14 đã áp dụng production;
  schema, constraint/index và dữ liệu cũ PASS.
- Backend cổng 3000 đã restart đúng `node backend/dist/server.js`; health/challenge 200,
  validation 400 và token Google thử sai 401. Typecheck/build PASS, 335/335 unit/API tests và
  1/1 M14 PostgreSQL integration PASS.
- Google production liên kết user 55 và logout/login lại PASS; 3 users/1 identity, user giữ
  10 progress/9 Quiz/30 XP. `EmailSender` bắt buộc STARTTLS cho cổng 587; helper nhập bí mật ở
  `maintenance/configure-smtp.ps1`; `npm run smtp:verify` kiểm tra không lộ secret. App Password
  mới verify PASS; email thật, local reset, token one-time, auth version 1→2, JWT cũ 401, JWT
  hiện hành 200 và password login PASS. Còn thiếu HTTPS public cho reset ngoài máy local. Chi tiết:
  `M14_GOOGLE_SIGNIN_PASSWORD_RECOVERY_REPORT.md`.
- Hotfix 2026-10-23: port 3000 là `node backend/dist/server.js` do launcher quản lý và tiến
  trình đang nạp build cũ, nên hai POST Google trả 404. Port 3001 là `tsx src/server.ts` với DB
  cô lập đã migration: challenge 200, body thiếu 400, token thử sai hợp lệ về hình thức 401.
  Tại thời điểm chẩn đoán hotfix, production chưa migration và port 3000 chưa restart; sau đó
  đã được triển khai theo phê duyệt như trạng thái hiện hành ở trên.
- Token verifier hotfix phân loại an toàn audience/time/claims/nonce mà không log token. Google
  E2E cổng 3001 PASS sau khi liên kết đúng user 55: login 200, 3 users/1 identity, dữ liệu
  10 progress/9 Quiz/30 XP giữ nguyên. M14 DB integration chạy lại 1/1 PASS.
- Kiểm tra bổ sung với tài khoản Google-only do người dùng tạo: production hiện có 4 users,
  2 identities, 1 passwordless user và 0 active reset token. Forgot vẫn trả HTTP 200 để chống
  dò email nhưng không tạo token/email cho tài khoản không có password. Generic response đã bổ
  sung hướng dẫn dùng `Tiếp tục với Google`; backend typecheck/build, 335/335 tests PASS và cổng
  3000 đã restart đúng build mới. Dữ liệu 5.000 cards/19 topics và lịch sử học không đổi.

---

## M15 Daily Learning Goals — DONE / PRODUCTION_DEPLOYED

- Module `/api/v1/learning`: goal GET/PUT, today, history và new-cards; tất cả lấy user từ JWT.
- Migration thêm settings, learned-card unique, daily snapshot và activation date. Review/Quiz
  gọi bộ đếm M15 trong cùng transaction; Favorite/mở thẻ không tính. Ngày học dùng
  `Asia/Ho_Chi_Minh`; SRS UTC cũ giữ nguyên.
- Legacy evidence từ Progress/Quiz được chặn tính mới bằng `first_source=LEGACY` và
  `first_learned_at=NULL`; ngày trước M15 trả `NO_DATA`.
- Backup production mới 5.816.580 byte đã restore-check vào `bivoca_m15_20261024`; migration và
  integration 1/1 PASS trên DB cô lập. Production sau đó áp dụng duy nhất migration M15.
  `pgmigrations` từ 9 lên 10; bốn bảng/config M15 tồn tại, 12 learned rows đều `LEGACY` và
  `first_learned_at=NULL`, 0 settings, 0 daily.
- Counts/checksum bảng nội dung và dữ liệu học khớp trước/sau migration: 5.000 cards/19 topics/
  5 users/3 identities/14 progress/12 Quiz/2 scores/5 XP. Google Login production chỉ cập nhật
  timestamp đăng nhập; counts không đổi và không tạo identity trùng.
- Backend typecheck/build, 338/338 tests PASS; M15 PostgreSQL integration 1/1 chỉ chạy trên DB
  cô lập. Launcher đã restart build mới ở `127.0.0.1:3000`; health, JWT và Learning API production
  PASS. Không chạy Review/Quiz/goal write trên production. Xem `M15_DAILY_GOALS_PROGRESS_REPORT.md`.

---

## M13 CEFR Dataset Integration Phase 1

- Dataset `backend/datasets/cefr/word_list_cefr.csv`: 7.988 records, chỉ A1–B2; SHA-256
  `c5e381905a87b47bd1321ac1cd34f870b107c0baa7b9f35f66e5b3a9b8ad8123`.
- Mapping bảo thủ theo normalized `english + part_of_speech`, có guard đa nghĩa/xung đột nguồn:
  293 exact, 0 uncertain, 7 unmatched; exact distribution A1 123/A2 66/B1 76/B2 28.
- 99 exact có nhãn nguồn khác nhãn hiện tại; Phase 1 chỉ audit và chỉ cho phép điền level NULL,
  nên 300 Flashcard gốc không đổi. Không có fallback A1.
- DB test giữ lại: `bivoca_m13_20261021_d4e222639863`, 10 topics/300 cards/6 migrations;
  import hai lần giữ 300 audit rows, 0 level update và hash dữ liệu không đổi. Guard chỉ cho apply
  lên local DB tên `bivoca_m13_*`; thử với `flashcard_english` bị từ chối trước khi ghi.
- `flashcard_english` chỉ đọc và snapshot trước/sau không đổi. Không tạo migration production.
- Báo cáo: `M13_CEFR_DATASET_INTEGRATION_REPORT.md`.

---

## 1. CÔNG NGHỆ & QUY ƯỚC
- **Ngôn ngữ & Runtime:** TypeScript (strict mode, target ES2023), Node.js (>=22.0.0), ESM native (`"type": "module"`).
- **Web Framework:** Express 5.2.1.
- **Database Driver:** `pg` 8.23.1 (PostgreSQL connection pool).
- **Validation:** Zod 4.6.5 (validate toàn bộ input body/query và biến môi trường).
- **Bảo mật:** `bcrypt` (băm mật khẩu), `jsonwebtoken` (xác thực token Bearer).
- **Quy tắc phân tầng:**
  - Route: Định nghĩa path, gắn middleware xác thực.
  - Controller: Parse input Zod, bắt lỗi, trả HTTP status và JSON DTO.
  - Service: Chứa business logic, kiểm tra quyền, tính toán nghiệp vụ.
  - Repository: Nơi DUY NHẤT chứa câu lệnh SQL (`SELECT`, `INSERT`, `UPDATE`).

---

## 2. DANH SÁCH ENDPOINTS THỰC TẾ TRONG HỆ THỐNG

### A. Health & Công khai
- `GET /api/v1/health` — Kiểm tra trạng thái server và kết nối PostgreSQL.
- `GET /api/v1/topics` — Lấy danh sách 10 chủ đề từ vựng.
- `GET /api/v1/topics/:code/flashcards` — Lấy danh sách 30 flashcard của một chủ đề.
- `GET /api/v1/flashcards/:id` — Chi tiết 1 flashcard theo ID.

### B. Xác thực người dùng (Auth)
- `POST /api/v1/auth/register` — Đăng ký tài khoản (body: `username`, `email`, `password`, `fullName` tùy chọn). Role mặc định USER, không nhận role từ request.
- `POST /api/v1/auth/login` — Đăng nhập (body: `email`, `password`) → trả `user` và `accessToken`. Không đăng nhập bằng username.
- `POST /api/v1/auth/google/challenge` — Tạo challenge/nonce Google 5 phút.
- `POST /api/v1/auth/google` — Xác minh Google ID Token và trả JWT BiVoca.
- `POST /api/v1/auth/google/link` — Liên kết Google vào tài khoản đang đăng nhập sau password re-auth.
- `POST /api/v1/auth/forgot-password` — Tạo reset token hash và gửi email khi SMTP đã cấu hình.
- `POST /api/v1/auth/reset-password` — Đặt mật khẩu mới bằng token một lần và thu hồi JWT cũ.
- `GET /api/v1/auth/me` — Lấy thông tin user đang đăng nhập (yêu cầu Bearer token).

### C. Tiến độ học tập (Progress - Yêu cầu Bearer Token)
- `GET /api/v1/progress` — Danh sách tiến độ học của user.
- `GET /api/v1/progress/summary` — Tổng hợp số từ đã học, số thẻ mastered, độ chính xác.
- `GET /api/v1/progress/:flashcardId` — Chi tiết tiến độ 1 từ.
- `POST /api/v1/progress/:flashcardId/review` — Ghi nhận kết quả ôn tập (body: `correct: boolean`).
- `PUT /api/v1/progress/:flashcardId/favorite` — Đánh dấu yêu thích thẻ (body: `isFavorite: boolean`).

### D. Trắc nghiệm Quiz (Quiz - Yêu cầu Bearer Token)
- `GET /api/v1/quiz/questions?topicCode={code}&limit={n}` — Lấy danh sách câu hỏi trắc nghiệm 4 đáp án.
- `POST /api/v1/quiz/answer` — Gửi câu trả lời (body: `{ flashcardId, selectedFlashcardId }`) → Server chấm điểm và tự động cập nhật tiến độ học.
- `GET /api/v1/quiz/history` — Lịch sử làm bài trắc nghiệm.
- `GET /api/v1/quiz/summary` — Tổng hợp số câu đúng/sai trong quiz.

### E. Gamification (XP & Leaderboard - Yêu cầu Bearer Token)
- `GET /api/v1/gamification/me` — Thông tin XP hiện tại, streak ngày hiện tại, chuỗi streak dài nhất.
- `GET /api/v1/gamification/xp-history` — Lịch sử cộng XP.
- `GET /api/v1/gamification/leaderboard` — Bảng xếp hạng người dùng theo XP.

### F. Quản trị (Admin - Yêu cầu Bearer Token + Role ADMIN)
- `GET /api/v1/admin/me`, `GET /api/v1/admin/dashboard`
- Quản lý topics: `GET/POST /api/v1/admin/topics`, `PUT /api/v1/admin/topics/:id`
- Quản lý flashcards: `GET/POST /api/v1/admin/flashcards`, `PUT /api/v1/admin/flashcards/:id`, `PATCH /api/v1/admin/flashcards/:id/status`
- Quản lý users: `GET /api/v1/admin/users`, `GET /api/v1/admin/users/:id`, `PATCH /api/v1/admin/users/:id/role`
- Chi tiết học tập: `GET /api/v1/admin/users/:id/progress`, `GET /api/v1/admin/users/:id/quiz-history`
- Thống kê: `GET /api/v1/admin/stats/quiz`, `GET /api/v1/admin/leaderboard`

Không có Admin GET chi tiết topic/card hoặc DELETE. PUT thay nội dung đầy đủ; không gửi `isActive` khi sửa card. Status nhận riêng `{isActive: boolean}`, role nhận `{role: "USER" | "ADMIN"}`. Tự hạ quyền trả 409 `ADMIN_SELF_ROLE_CHANGE_FORBIDDEN`.

Admin topics/cards/users/progress/history/leaderboard trả `{items, page, limit, total, totalPages}`; page/limit mặc định 1/20, limit tối đa 100. Search tối đa 200 ký tự; filters: `topicCode`, `status=ACTIVE|INACTIVE|ALL`, `level=A1|A2|B1|B2`, `role=USER|ADMIN|ALL`, `range=7d|30d|all`. Sort cố định ở repository, không có tham số sort tùy ý. Body strict Zod; các query không strict có thể bỏ qua trường không biết.

Public `/topics` trả array `{id, code, name, description, imageUrl}`, không có `flashcardCount`; Admin topic có trường này. Public flashcards chỉ trả thẻ Active. Quyền ADMIN được đọc lại từ PostgreSQL trên mỗi request sau xác minh JWT; không tin role trong JWT hay frontend. DTO không trả password/hash; JWT chỉ xuất hiện trong response register/login.

---

## 3. MIGRATION DATABASE
Quản lý bởi `node-pg-migrate` trong thư mục `backend/migrations/`:
1. `1791072000000_create_users.sql` — Bảng `users` (id, username, email, password_hash, created_at, last_login...). Chưa thêm role ở migration này.
2. `1791072001000_create_user_progress.sql` — Bảng `user_progress` (user_id, flashcard_id, memory_level, correct_count, wrong_count, is_favorite...).
3. `1791072002000_create_quiz_results.sql` — Bảng `quiz_results` (user_id, flashcard_id, selected_flashcard_id, is_correct, question_type, answered_at).
4. `1791072003000_create_gamification.sql` — Bảng `user_scores` (total_xp, current_streak, longest_streak) và `xp_history`.
5. `1791072004000_add_admin_authorization.sql` — Thêm `users.role NOT NULL DEFAULT 'USER'` và CHECK chỉ USER/ADMIN; không tự promote tài khoản.

M9 đọc catalog thực tế: PostgreSQL **18.6**, database **flashcard_english**, TimeZone **Asia/Bangkok**, `pgmigrations` có đủ 5 migration. M6A **đã apply** (run_on `2026-10-04T19:48:28.789Z`); thông tin “chưa apply” trong bàn giao cũ đã hết hiệu lực. Không chạy migration trên DB gốc trong M9.

Snapshot M9: 10 topics, 300 cards Active, 3 users (1 ADMIN/2 USER), 4 progress, 3 quiz results, 1 score, 1 XP history. Unique lower(email)/lower(username), lower(english)+lower(vietnamese), progress(user,card), daily XP và các FK/CHECK có trong catalog. Giữ `xp_history.quiz_result_id ON DELETE SET NULL`; không sửa schema.

DB kiểm thử được người dùng cho phép: **bivoca_m9_20261014_5fc9558e413d**. Được tạo mới, seed dataset và apply 5 migration chỉ tại DB này. Sau exact cleanup còn 10 topics/300 cards/5 migration, các bảng người dùng và học tập đều 0. DB test được giữ lại. Không chạy lại setup trên DB đã tồn tại; không chạy integration ghi trên DB gốc.

---

## 4. BỘ TEST & KIỂM THỬ
- Lệnh chạy kiểm thử chính: `npm test`
- Số lượng test suite: 13 suites
- Số lượng tests: **311 tests (100% PASS)**
- Sử dụng Repository Doubles cho phép chạy unit & mock integration test toàn diện độc lập không phụ thuộc vào PostgreSQL connection trực tiếp.

M9 bổ sung bằng chứng từ PostgreSQL thật, không thêm hoặc bỏ backend unit test:
- `npm run test:integration`: 5/5 PASS, chỉ đọc DB gốc.
- Harness `backend/.m9-verification/isolated.mjs test`: Auth, Progress, Quiz, Gamification, Admin mỗi file 1 test lifecycle + 5 test chỉ đọc = 10/10 PASS trên DB riêng. Có concurrency, rollback 500 và cleanup theo UUID/ID chính xác.
- `live-api.mjs`: 68/68 checks GET trên server 3000, dùng JWT thử nghiệm trong bộ nhớ; thiếu/sai/hết hạn token 401, USER 403, ADMIN 200, invalid query 400, not found 404.
- `postcheck.json`: số bản ghi, phân bố role và checksum dataset DB gốc khớp trước kiểm thử. Không suy ra mọi trường riêng tư bất biến từ phép so này; đăng nhập thủ công cập nhật `last_login` theo hành vi có sẵn.
- Log và JSON bằng chứng tại `backend/.m9-verification/`; không chứa JWT/secret/hash/connection URI theo phép scan đã chạy.

## 5. M10 SPACED REPETITION — 2026-10-14 (DONE)

- `memory_level` giữ nguyên quy tắc cũ: đúng +1 đến tối đa 5, sai -1 đến tối thiểu 0. `review_count` là số lần Review/Quiz đã ghi; `last_review` là lần ghi mới nhất; `next_review` trước M10 luôn NULL. Favorite-only có `review_count=0`.
- M10 dùng lịch mức sau khi trả lời đúng: 1/3/7/14/30 ngày cho mức 1..5; trả lời sai hẹn 10 phút bất kể mức. Cả Review và Quiz dùng chung `upsertProgressReview` trong transaction cũ; XP/streak/daily dedup không đổi.
- `GET /api/v1/progress/due?limit=1..300&topicCode=FOOD` (mặc định 100) yêu cầu JWT, trả card active đã ôn và đến hạn theo UTC. Dòng lịch cũ `review_count>0 AND next_review IS NULL` đến hạn ngay; favorite-only và card chưa từng học không đến hạn. Sắp xếp `next_review ASC NULLS FIRST`, rồi số lần sai giảm dần và card ID.
- `GET /api/v1/progress/schedule` yêu cầu JWT, trả `trackedCards`, `reviewedCards`, `dueNow`, `upcoming24Hours`, `reviewedToday` theo ngày UTC và `memoryLevels[0..5]`; chỉ tính card active, mỗi người tách theo JWT.
- Migration `1791964800000_schedule_progress_reviews.sql`: chuyển `last_review`/`next_review` TIMESTAMP sang TIMESTAMPTZ theo timezone session cũ, tạo partial index `(user_id,next_review,flashcard_id) WHERE review_count>0`; Down đảo lại và bỏ index. Đã chạy trên `flashcard_english` sau chấp thuận riêng của người dùng và sao lưu bằng `pg_dump`.
- DB được người dùng cho phép: `bivoca_m10_20261014_a18cbe62d430`, mới tạo, dataset 10/300, 6 migration. Migration Up/Down/Up bảo toàn thời điểm cũ PASS; 6/6 integration files PASS, exact UUID cleanup lúc đó để lại 0 users/0 progress. Sau đó nghiệm thu OPPO trên cùng DB: người dùng cho phép giữ tài khoản `duy` ID 18 và hai progress/XP; fixture agent ID 17 đã xóa đúng ID. Lịch đúng/sai, retry khi ngắt kết nối, Progress và XP thực tế PASS. `npm run typecheck`, `npm run build`, `npm test` 313/313 PASS. Admin Web hồi quy 42/42 PASS.
- DB vận hành `flashcard_english`: preflight 5 migration, TimeZone Asia/Bangkok, 10 topics/300 cards, 3 users/4 progress/3 quiz/1 score/1 XP history; backup custom format ở `C:\Users\phuc\AppData\Local\BiVoca\backups\flashcard_english_before_m10_20261014.dump` (50.814 bytes, `pg_restore --list` PASS). Sau `npm run migrate:up`: 6 migrations, hai cột review là TIMESTAMPTZ, counts không đổi. Launcher khởi động lại backend 3000/Admin 5173; health/topics/Admin 200, due thiếu JWT 401, due/schedule/summary JWT 200, 3 due khớp `schedule.dueNow`; read-only `npm run test:integration` 5/5 PASS. Không ghi lượt Review/Quiz mới vào DB vận hành. Chi tiết trong `M10_SPACED_REPETITION_REPORT.md`.

## 6. M13 LARGE VOCABULARY — 2026-10-21 (STAGING ACCEPTED)

- Phase 1 CEFR giữ kết quả 293/300 exact, 7 unmatched và 99 conflict không tự ghi đè. Nguồn Anh–Việt SQLite được audit: 104.829 words, 158.307 definitions, 175.735 links; SHA-256 và CC BY-SA attribution nằm trong `datasets/en-vi/`.
- Pipeline deterministic tạo 24.388 ứng viên: 58 APPROVED_AUTO, 23.990 NEEDS_REVIEW, 340 REJECTED. Từ đa nghĩa/topic không chắc chắn bị giữ review; không tự dịch, tự tạo example hay gán A1. Mục tiêu 5.000 chưa đạt.
- Migration 7 thêm `vocabulary_sources`, `flashcard_provenance`, `flashcard_topics` và index. Importer chỉ apply vào local DB tên `bivoca_m13_large_*`, transaction và idempotent. DB staging `bivoca_m13_large_20261021_b06075c1d427`: up/down/up, rollback injection và import hai lần PASS; 16 topics/358 cards, 58 imported Quiz-eligible, 0 FK/duplicate source record.
- Public API hiện có thêm `GET /api/v1/topics/catalog` trả `flashcardCount` và `GET /api/v1/flashcards` hỗ trợ `search`, `topicCode`, `level`, `page`, `limit`; endpoint topic legacy giữ nguyên contract. Quiz loại imported card không approved/eligible và giữ eligibility thẻ cũ.
- `npm run typecheck`, build PASS; `npm test` **320/320**; pipeline **8/8**; database integration **4/4**. Production `flashcard_english` chỉ đọc, giữ 10 topics/300 cards/checksum. Chưa chạy migration/import production. Xem `M13_LARGE_VOCABULARY_IMPLEMENTATION_REPORT.md`.
# M15.5 Vocabulary Pipeline — 2026-10-25

- `m15_5_vocabulary_pipeline.py` giữ CEFR MIT, nghĩa gốc CC BY-SA 4.0 và thêm đối chiếu
  VNEDICT CC BY 3.0; không tự dịch/suy luận CEFR. IPA/example chưa xác minh của 15.000 ứng viên
  mới không được publish và chỉ giữ trong audit payload.
- Pipeline loại thêm nghĩa tham chiếu/dạng chia/định dạng hỏng, nghĩa bằng headword và xung đột
  POS-nghĩa chắc chắn. Artifact cuối có 149.544 candidates, 22.000 approved cho staging,
  127.521 needs review và 26.214 rejected.
- DB cô lập `bivoca_m15_5_20261024` đạt 20.000 cards/9.869 headwords/20 topics/6.680 Quiz
  eligible; 0 duplicate, 0 missing required, 7.743 CEFR NULL. Checkpoint 10k/15k/20k PASS và
  checksum toàn bộ dữ liệu bảo vệ khớp production.
- Mẫu độc lập mới 500 hàng chỉ đạt 479 PASS, 21 needs review; đối chiếu VNEDICT vẫn có false
  positive. Vì vậy quality gate thất bại và production vẫn 5.000 cards/19 topics, không có write.
- Pipeline 15/15, Backend 338/338, M15.5 integration 5/5, typecheck/build PASS; Admin 43/43 và
  build PASS. Chi tiết cuối: `M15_5_VOCABULARY_20000_REPORT.md` mục 17.

## M15.5 DataFix gate — 2026-10-25

- DataFix 137 hàng match theo normalized English+POS+original Vietnamese; mọi bản gốc đều bị
  block và thay bằng candidate khác. 57 nghĩa đề xuất không được tự ghi; audit VNEDICT chỉ xác
  nhận exact 8 hàng và human-reviewed count vẫn 0.
- Pipeline bổ sung rule semantic guard, cross-POS duplicate guard và source audit. Target selection
  ghi marker `targetTotal + inputSha256`; publisher chỉ lấy đúng marker, không lấy stale approval.
- Artifact SHA `7aaa11a90eb1b75b07c3137d8d77e4ba0e2958cea5b073492649d14e56a5500c`;
  approved guard 22.000 rows báo 0 block/0 duplicate.
- Staging rebuild 10K/15K/20K PASS; cuối 20.000 cards, 9.928 headwords, 20 topics, 7.645 CEFR
  NULL, 6.660 Quiz eligible, 0 duplicate. Protected hashes all match production.
- Mẫu độc lập hậu-fix FAIL 24/500. Backend 338/338, DB integration 5/5, pipeline 17/17 và pack
  2/2 PASS. Production giữ 5.000 cards; không import.

## M15.5 Repair v2 semantic guard — 2026-10-25

- 57 hard block Repair v2 được match theo English+POS+Vietnamese gốc; không dùng staging ID.
- VNEDICT `term in meaning` được hạ thành lexical overlap. `independentMeaningConfirmed` không còn
  true từ substring; source ghi `independentLexicalOverlap*`, `semanticVerified=false`.
- Target preparation và publisher yêu cầu `semanticVerified=true`. Hiện eligibility = 0 nên mọi
  deployment fail-closed trước khi ghi DB.
- Repair data tests 10/10, pipeline 19/19, typecheck/build PASS. Artifact preflight 22K có 0 hard
  block nhưng 0 semantic verified; production vẫn 5K và không import.

## M15.5 production import — 2026-10-25

- `flashcard_english`: 20.000 cards/20 topics. 15.000 additions use `NEEDS_REVIEW` provenance,
  `IMPORTED_UNVERIFIED` reasons and `quiz_eligible=false`; generic M13/M16 publisher remains
  fail-closed for semantic publication.
- Dedicated plan/import/verify scripts support dry-run, exact DB confirmation, transaction batches,
  checkpoints and idempotency. Frozen plan SHA-256 is
  `26169eff61a487d6ae96359ea086d7655b53ec02d928af15aa9af3fd8df67132`.
- API flashcard DTO now exposes `contentStatus`; production search total=20.000. Backend
  typecheck/build and 338/338 tests PASS. See report section 20.

## M15.6 topic reclassification — 2026-10-25

- `classify-m15-6-topics.ts` phân loại 14.329 primary General bằng exact token/phrase trên English + Vietnamese + sourceTopics, rồi fallback theo POS; ghi CSV/JSONL mapping và hỗ trợ dry-run/apply idempotent theo batch transaction.
- Production: 20.000 cards, 38 topic, General primary/link 0; 1.471 thẻ chuyển ngữ nghĩa và 12.858 thẻ vào POS. 15.000 imported cards vẫn `IMPORTED_UNVERIFIED`, `quiz_eligible=false`.
- `/topics/catalog` trả `topicGroup`, semantic trước POS, loại General và topic rỗng. `/flashcards` tiếp tục paging server; POS_NOUN=6.000, page size 100 hoạt động tới trang 60.
- Backup SHA-256 `cbce90d6c3647a230fa2a514110aa6247f6375a05381dbb0d56c40ead8de3411`; restore-check và protected checksums PASS. Backend typecheck/build, 338/338 tests PASS; production 3000 chạy build mới.
- Chi tiết và bảng phân bố: `M15_6_TOPIC_RECLASSIFICATION_REPORT.md`.

## M15.7 semantic topic enrichment — 2026-10-25

- Dedicated classifier chỉ đọc 12.858 primary `POS_*`, dùng exact bilingual sense rules, domain-safe headwords, bounded morphology, exclusions và reviewed overrides; không dùng substring hoặc per-card AI. Frozen plan 426 rows, SHA-256 `ff14a95e67b485828473c1e4c01c66acf27292b23c1d2c5e9cd86c983f57d08b`.
- Production sau apply: 20.000 cards/38 topics, POS 12.432; primary integrity errors 0; unverified Quiz leak 0. Apply lần hai moved=0.
- Backup restore-check SHA-256 `589262097a02a5e64ef17169e7a17cc63b4d8778129d151880c0d365bf427dcd`; protected hashes card content/users/progress/Quiz/scores/XP/provenance khớp baseline.
- Backend typecheck/build và 338/338 tests PASS. API catalog/paging production PASS; Noun 5.729, 58 trang. Chi tiết: `M15_7_SEMANTIC_TOPIC_ENRICHMENT_REPORT.md`.

