# M9 — Backend, PostgreSQL, Admin Web & JWT/Security Verification

**Ngày nghiệm thu:** 2026-10-14 (Asia/Saigon)  
**Dự án:** BiVoca / BIPASSION  
**Thư mục:** `C:\Users\phuc\OneDrive\Documents\New project`  
**Trạng thái:** **DONE trong phạm vi M9 local đã kiểm tra**. M8 vẫn **DONE**.

## 1. Tổng quan và căn cứ nghiệm thu

M9 đã xác minh source, PostgreSQL thật, HTTP server thật và các màn hình Admin trong trình duyệt; chạy integration có ghi trên DB riêng được người dùng chấp thuận. Phát hiện và sửa một lỗi xử lý session Admin khi restore gặp lỗi tạm thời. Không thay đổi Backend source, migration, dataset, Android hoặc kiến trúc.

Đã đọc AGENTS.md → CODEX_HANDOFF_REPORT.md → WORK_STATUS.md → PROJECT_CONTEXT.md → BACKEND_CONTEXT.md → ANDROID_CONTEXT.md → M8_ROOM_OFFLINE_REPORT.md → backend/README.md → admin-web/README.md → backend/verification-report.txt. Source route/controller/service/repository, schemas, migration và scripts là căn cứ khi tài liệu cũ sai khác.

| Tiêu chí M9 | Kết quả và bằng chứng |
| --- | --- |
| Backend typecheck/tests/build | PASS; 311/311 tests, 13 suites, 0 fail/skip |
| Admin build/tests/format | PASS; 42/42 tests, 3 files, 0 fail |
| Contract và phân quyền Admin | Đọc source, 68 HTTP checks trên server thật và integration Admin PostgreSQL riêng PASS |
| Xác định migration M6A | `pgmigrations` và catalog xác nhận đã apply |
| Integration DB an toàn | 5/5 chỉ đọc DB gốc; 10/10 trên DB test riêng PASS, cleanup xác minh |
| JWT và chính sách session | 401/403 thật; crypto/HTTP/UI tests cho expired, stale response, network/5xx PASS |
| Phạm vi dữ liệu | DB gốc giữ số bản ghi, phân bố role và checksum dataset; fixture chỉ ghi/cleanup trong DB riêng |
| Lỗi phát hiện | Lỗi restore đã sửa, red tests trước sửa và regression sau sửa có log |
| Bàn giao | Báo cáo này, WORK_STATUS, BACKEND_CONTEXT, PROJECT_CONTEXT và Admin README đã cập nhật |

Các giới hạn browser E2E được ghi ở mục 11. Không suy diễn unit tests thành bằng chứng mọi thao tác đã được bấm với DB thật.

## 2. PostgreSQL và migration M6A

Audit dùng `BEGIN READ ONLY`, đọc information_schema, pg_constraint, pg_indexes và pgmigrations; không đọc/in nội dung `.env`, password_hash hay token tài khoản. Kết nối dùng cấu hình hiện có trong bộ nhớ.

- Database vận hành: **flashcard_english**; PostgreSQL **18.6**; TimeZone **Asia/Bangkok**.
- Transaction audit: `transaction_read_only=on`.
- `pgmigrations` có đủ 5 migration:
  1. `1791072000000_create_users`
  2. `1791072001000_create_user_progress`
  3. `1791072002000_create_quiz_results`
  4. `1791072003000_create_gamification`
  5. `1791072004000_add_admin_authorization`
- M6A đã apply, `run_on=2026-10-04T19:48:28.789Z`. Không chạy migrate up/down trên DB vận hành.
- `users.role NOT NULL DEFAULT 'USER'`, CHECK chỉ USER/ADMIN. Unique case-insensitive username/email có trong catalog.
- Topic code/name unique; topic code CHECK chữ hoa bắt đầu bằng chữ cái. Card có unique lower(english)+lower(vietnamese), CHECK level/POS và các trường bắt buộc.
- Progress có unique(user_id,flashcard_id), memory 0..5 và counters không âm. Quiz/Progress/Scores/XP có FK tương ứng.
- XP giữ daily-award unique index và unique quiz_result_id; `quiz_result_id ON DELETE SET NULL` giữ nguyên. Không sửa schema, FK, migration hoặc nội dung từ vựng.

| Bảng / dữ liệu | DB gốc trước | DB gốc sau | DB test sau cleanup |
| --- | ---: | ---: | ---: |
| topics | 10 | 10 | 10 |
| flashcards | 300 | 300 | 300 |
| users | 3 | 3 | 0 |
| user_progress | 4 | 4 | 0 |
| quiz_results | 3 | 3 | 0 |
| user_scores | 1 | 1 | 0 |
| xp_history | 1 | 1 | 0 |
| pgmigrations | 5 | 5 | 5 |

DB gốc: 1 ADMIN, 2 USER; cả 10 chủ đề có 30 thẻ Active. SHA-256 toàn rows theo ID trước/sau khớp:

```text
topics:     82817eac96b829eb95f5a8af62d7abfc2f8a35ffc988810aa0e14d76fa68adf2
flashcards: e0eee0adf39d8b724ee1e7ac5ac27e8cb8b2f734792f383df97ff1f00fda1bed
```

Đối chiếu này chứng minh số bản ghi, phân bố role và dataset không đổi; không tuyên bố mọi trường riêng tư byte-equivalent. Đăng nhập thủ công của người dùng cập nhật `last_login` theo luồng có sẵn.

**DB test được chấp thuận:** `bivoca_m9_20261014_5fc9558e413d`. Kế hoạch đã nêu cụ thể tên DB, seed, 5 migration, các integration và exact cleanup trước khi chạy; người dùng trả lời “Cho phép DB test riêng và các test này”. Harness kiểm tra localhost, DB nguồn đúng tên, target chưa tồn tại; tạo mới rồi assert current_database trước seed/migration. CREATE DATABASE là DDL trên PostgreSQL local được chấp thuận; không ghi dữ liệu ứng dụng hoặc migration vào DB gốc.

Fixture dùng UUID/ID/username/email/topic code riêng. Cleanup chỉ khớp các định danh do test tạo, xử lý FK liên quan trong DB riêng và đối chiếu baseline sau mỗi suite. Không DROP/TRUNCATE/reset/broad DELETE. DB test được giữ lại theo kế hoạch; sequences có thể tăng sau fixture/rollback, không reset sequences. Không chạy lại phase `create` với DB này.

Bằng chứng: `backend/.m9-verification/database-audit-before.json`, `database-audit.json`, `isolated-state.json`, `postcheck.json`.

## 3. Backend API integration và contract

Đối chiếu Route → Controller → Service → Repository → PostgreSQL. DTO dùng camelCase; SQL parameterized và sort fragments cố định; tìm kiếm escape wildcard, không nội suy input vào SQL. Request body Auth/Admin/Review/Favorite/Quiz strict Zod. Không gọi unknown query stripping là body strict validation.

### Authentication và modules học tập

| Nhóm | Đã kiểm tra trên PostgreSQL thật |
| --- | --- |
| Auth | Register 201, bcrypt hash và compare, Login bằng email/password 200, Me 200, duplicate email/username 409, sai credentials 401; fixture cleanup |
| Topics/Flashcards public | 10 topics, FOOD/ANIMALS và detail matching ID; invalid/not found; chỉ thẻ Active trong luồng học |
| Progress | Favorite/Review, counters/memory, isolation giữa user, concurrent reviews, summary và cleanup |
| Quiz | Questions/answer/history/summary, correct/incorrect, concurrent answer, cập nhật progress và transaction rollback khi counter overflow |
| Gamification | Review/Quiz XP, daily idempotence, UTC streaks, concurrency, leaderboard/history, quiz ownership và rollback khi XP overflow |
| Admin | Tạo/sửa topic/card, status, user/detail/progress/history, role, dashboard/stats/leaderboard, permission và baseline preservation |

Source Admin chỉ có create/read/update và status/role; không có API xóa. Các write lifecycle trên được chạy ở DB test riêng, không phải DB gốc.

Login body đúng là `{email,password}`. Register nhận `{username,email,password,fullName?}`, không nhận role; role mặc định USER. Password tối thiểu 8 ký tự và tối đa 72 byte UTF-8 theo schema. Public topics không có `flashcardCount`; Admin topics có trường này.

### Toàn bộ 16 Admin routes hiện có

Mọi path bên dưới có prefix `/api/v1/admin`; middleware authenticate và fresh role lookup chạy trước body parser.

| Method | Path | Contract chính |
| --- | --- | --- |
| GET | `/me` | DTO có role đã xác nhận từ DB |
| GET | `/dashboard` | `range=7d|30d|all` |
| GET | `/topics` | page, limit, search |
| POST | `/topics` | code, name, description?, imageUrl? |
| PUT | `/topics/:id` | Thay nội dung topic đầy đủ |
| GET | `/flashcards` | page, limit, search, topicCode, status, level |
| POST | `/flashcards` | CardInput + isActive? mặc định true |
| PUT | `/flashcards/:id` | CardInput; không nhận isActive |
| PATCH | `/flashcards/:id/status` | `{isActive:boolean}` |
| GET | `/users` | page, limit, search, role |
| GET | `/users/:id` | user + progress/quiz/gamification summaries |
| GET | `/users/:id/progress` | page, limit, search; read-only |
| GET | `/users/:id/quiz-history` | page, limit; read-only |
| PATCH | `/users/:id/role` | `{role:"USER"|"ADMIN"}`; self-demotion 409 |
| GET | `/stats/quiz` | range, topicCode?; accuracy null nếu 0 answers |
| GET | `/leaderboard` | page, limit; rank lấy từ DB |

CardInput: `topicCode, english, vietnamese, pronunciation?, partOfSpeech?, example?, exampleVi?, level`. Các field text optional của topic/card khi bỏ qua mặc định null; PUT vẫn là replacement, không phải partial PATCH. Code chữ hoa bắt đầu bằng A–Z. Level A1/A2/B1/B2; status ACTIVE/INACTIVE/ALL; role USER/ADMIN/ALL khi lọc.

Page/limit canonical, mặc định 1/20, limit tối đa 100. List Admin trả `{items,page,limit,total,totalPages}`; sorting cố định trong repository, không có query sort tùy ý. Không có `GET /topics/:id`, `GET /flashcards/:id` trong router Admin, không có DELETE.

HTTP 400/401/403/404/409 đã có bằng chứng. Generic 500 và transaction rollback được kích hoạt bằng overflow trên row của fixture trong DB riêng; response chỉ `{error:{code:"INTERNAL_SERVER_ERROR",message:"Internal server error"}}`, không SQL/stack/driver secret. Dòng log `Unhandled API error` trong các test âm là hành vi mong đợi, không phải test FAIL.

**Server vận hành 127.0.0.1:3000:** 68/68 GET checks PASS, 0 failure. Dùng JWT kiểm thử ký/xử lý trong bộ nhớ và IDs đọc từ DB cho các phép đọc; không đổi role, JWT secret hay thời hạn phiên của người dùng. Kiểm tra public health, protected modules, Admin pagination/search/filter, status validation và missing resource. Bằng chứng: `live-api.json`.

## 4. Admin Web — kết quả thực tế trong trình duyệt

Tab `http://localhost:5173` gọi backend thật qua proxy `/api`. Người dùng tự đăng nhập trực tiếp bằng ADMIN hiện có; agent quan sát Dashboard và kiểm tra các màn hình sau. Không nhập/đọc mật khẩu người dùng, không lấy token từ browser storage.

| Phép thử | Kết quả browser thật |
| --- | --- |
| Dashboard | 3 users, 300 cards Active, 3 Quiz answers, accuracy 33,33%; chart/top learners/top topics hiển thị |
| Topics | 10 topics, mỗi topic 30 cards; search FOOD còn 1 kết quả |
| Topic editor | Sửa FOOD mở đúng dữ liệu và Hủy; tạo rỗng báo lỗi code/name |
| Flashcards | List tổng 300; FOOD + ACTIVE + A1 còn 12; limit 10, trang 2 có vegetable/breakfast, Previous/Next đúng trạng thái |
| Card editor | Sửa vegetable mở đúng IPA/POS/examples/level; Hủy; tạo rỗng báo lỗi topic/english/vietnamese/level |
| Inactive filter | FOOD/A1/INACTIVE trả 0 và empty state, không dữ liệu giả |
| Users | 3 users, lọc ADMIN còn 1; chi tiết người đang đăng nhập khóa nút đổi role |
| Account summaries | 4 tracked progress, 3 reviews, 3 Quiz answers, 10 XP/level 1/streak 1 |
| Progress/history | 4 dòng progress; 3 Quiz results với đáp án/kết quả/thời gian đúng |
| Quiz statistics | 7 ngày toàn bộ: 3 answers, 1 đúng/2 sai, 33,33%; FOOD/30 ngày: 0, accuracy `—`, chart/table empty |
| Leaderboard | 1 người có score, rank #1, 10 XP; rank từ API |
| Session restore | Điều hướng bằng tải trang mới vẫn xác nhận phiên ADMIN và tải dữ liệu |
| Logout và guard | Logout về Login; Back và mở `/admin/flashcards` đều về Login, không lộ protected data |
| Console | 0 error, 0 warning trong tối đa 20 entries gần nhất khi kiểm tra Dashboard |

Không lưu create/update/status/role bằng browser trên DB gốc. Mutation API được thực thi trên DB riêng thật; UI contract/form/confirm/error/cache được kiểm tra riêng bằng Vitest + fetch fixtures. Không tuyên bố mutation browser E2E đã PASS.

Ảnh bằng chứng: `admin-web/.m9-verification/dashboard-real.jpg`, `logout-guard-real.jpg`. Tab cuối nằm ở Login sau phép thử Logout; backend/Admin launcher vẫn phục vụ bình thường.

## 5. JWT và security verification

| Tình huống | Bằng chứng và kết quả |
| --- | --- |
| Không có token | HTTP thật trả 401 `AUTH_TOKEN_MISSING` ở Auth/Admin/Progress/Quiz/Gamification |
| Token sai | HTTP thật trả 401 `AUTH_TOKEN_INVALID` |
| Token hết hạn | JWT test có exp quá khứ, chữ ký hợp lệ, subject test không phải phiên thật; HTTP thật 401; token crypto/HTTP unit tests PASS |
| USER gọi ADMIN | HTTP thật 403 `ADMIN_FORBIDDEN`; write/read routes còn có unit và isolated Admin integration |
| ADMIN gọi API | HTTP thật 200; mutation integration PostgreSQL riêng PASS |
| Đổi role với cùng JWT | Isolated integration promote USER → Admin me 200 → demote → 403, chứng minh fresh role DB lookup |
| 401 ở frontend | API client thông báo session; session/cache clear và router Login có UI fixture tests |
| 403 | ADMIN_FORBIDDEN hiện lỗi quyền và kết thúc Admin session; generic403 giữ token, không tự xác nhận ADMIN và không gọi nó là hết hạn |
| Network/500/503 | Restore giữ token, không gọi onEnd; retry xác nhận ADMIN mà không login lại; API client không invalid session trên 5xx |
| Stale response | Token mismatch không xóa phiên mới; late restore401 sau login mới bị generation guard bỏ qua; pending login không phục hồi phiên đã Logout |
| Logs và DTO | DTO strict/mapping không trả password/hash/secret hoặc accessToken ngoài auth; scan 20 log files không thấy configured DB password/JWT secret, JWT/bcrypt pattern hoặc connection URI |
| Android | Đọc AuthInterceptor/AuthSessionExpiry và SessionManager logic đã có: invalidate chỉ Bearer+401, clear-if-token-matches, redirect guard; BASIC HTTP logging, không BODY/HEADERS. Không sửa hoặc chạy lại Android trong M9 |

JWT service cho phép HS256, kiểm tra sub là positive signed 32-bit ID và exp là số nguyên; không cấp quyền từ claim role. Auth dùng bcrypt; env validation chỉ báo tên field sai. Error handler chỉ log thông báo cố định cho lỗi không biết. Admin Web không log credentials hoặc raw backend errors.

Scan log có phạm vi xác định: 10 backend verification logs, 9 Admin verification logs và maintenance `.runtime/local-admin.log`, tổng 20. Đây là kiểm tra patterns và secret cấu hình hiện tại, không phải chứng minh mọi loại dữ liệu nhạy cảm hay toàn bộ log lịch sử đều an toàn. Không đọc password người dùng từ DB/log và không sửa JWT secret hệ thống.

## 6. Lỗi thực tế được phát hiện

**Admin mất session khi tải lại trong lúc backend tạm không truy cập được.**

Source cũ `AuthSession.restore()` gọi logout cho mọi exception từ `/admin/me`. Network error hoặc HTTP 500/503 cũng xóa token sessionStorage, chạy onEnd và xóa query cache. Người dùng phải đăng nhập lại dù chưa có bằng chứng token hết hạn hay mất quyền.

Đã thêm 3 test tái hiện cho network/500/503 trước sửa: **14 PASS / 3 FAIL**, token bị xóa. Log: `admin-web/.m9-verification/restore-before-fix.log`.

Không phát hiện lỗi Backend/schema/module học tập còn chưa xử lý trong các phép thử đã chạy. Không sửa Backend để khớp tài liệu cũ.

## 7. Cách sửa tối thiểu và hồi quy

- `AuthSession.restore`: chỉ logout khi 401 hoặc 403 `ADMIN_FORBIDDEN`; lỗi còn lại giữ token/state và báo notice. Cold restore vẫn không cấp quyền cho đến khi `/admin/me` thành công. Khi admin đã xác nhận, recheck không làm UI/cache bị tháo chỉ vì loading. Generation guard giữ nguyên.
- AuthProvider cung cấp retryRestore từ store hiện có; không tạo service/store mới.
- LoginPage dùng button hiện có “Thử kết nối lại” khi còn token mà chưa xác nhận admin; busy state, gọi lại restore và không yêu cầu nhập mật khẩu cho retry.
- Thêm 10 tests so với baseline 32: 3 transient restore/retry, 1 confirmed-session/cache, 1 UI retry, 1 late failed restore sau login mới, 1 generic403 restore, 3 authenticated API errors không invalid session. Không bỏ test cũ.
- Lượt test trung gian sau sửa ban đầu có 2 FAIL (34 PASS/36): fixture UI được mount với phiên đã xác nhận không phù hợp cold-restore case và loading làm detached pagination control. Đã chỉnh fixture cold restore và giữ UI khi recheck admin đã xác nhận; không bỏ assertion/test. Final toàn bộ **42/42 PASS**.

Không thêm thư viện, không đổi API contract công khai hoặc thiết kế giao diện ngoài nút retry cần cho lỗi.

## 8. Danh sách file thay đổi và bằng chứng

**Source ứng dụng (3):**

1. `admin-web/src/features/auth/session.ts`
2. `admin-web/src/features/auth/AuthProvider.tsx`
3. `admin-web/src/pages/LoginPage.tsx`

**Tests (2):**

4. `admin-web/tests/auth-client.test.ts`
5. `admin-web/tests/pages.test.tsx`

**Tài liệu (5, gồm báo cáo mới):**

6. `M9_INTEGRATION_SECURITY_REPORT.md` — mới
7. `WORK_STATUS.md`
8. `BACKEND_CONTEXT.md`
9. `PROJECT_CONTEXT.md`
10. `admin-web/README.md`

**Artifacts kiểm chứng mới, không phải production modules:**

- Backend `.m9-verification/{audit,live-api,isolated,postcheck,log-check}.mjs`.
- JSON: `database-audit.json`, `database-audit-before.json`, `isolated-state.json`, `live-api.json`, `postcheck.json`, `log-check.json`.
- Backend logs: `typecheck.log`, `tests.log`, `build.log`, `readonly-integration.log`, 6 `isolated-*.log`.
- Admin logs: `build.log`, `tests.log`, `restore-before-fix.log`, `build-after-fix.log`, `tests-after-fix.log`, `tests-session-fix.log`, `build-final.log`, `tests-final.log`, `format.log`; 2 screenshots ở mục 4.

Không chỉnh `ANDROID_CONTEXT.md`, M8 report, gradle.properties, Room/DAO/repositories/ViewModels, Backend src/migrations/package, dataset, maintenance source hoặc `.env`. Thư mục hiện không phải Git repository; danh sách trên dựa trên thao tác chỉnh file trong phiên M9, không phải `git diff`.

## 9. Các lệnh đã thực thi và kết quả

### Backend (working directory `backend`)

```powershell
npm run typecheck
npm test
npm run build
node .m9-verification/audit.mjs
node .m9-verification/live-api.mjs
npm run test:integration
node .m9-verification/isolated.mjs plan
```

Tất cả PASS. Backend `npm test`: 311 tests/13 suites, 0 fail/cancelled/skipped/todo; source Backend không thay đổi trong M9.

Sau khi người dùng chấp thuận kế hoạch, chạy đúng DB target đã trình bày:

```powershell
$env:M9_ALLOW_ISOLATED_DATABASE='1'
try {
    node .m9-verification/isolated.mjs create
    if ($LASTEXITCODE -eq 0) { node .m9-verification/isolated.mjs test }
} finally { Remove-Item Env:M9_ALLOW_ISOLATED_DATABASE }
```

Phase create PASS: 10 topics/300 cards/0 users/5 migrations. Phase test PASS tất cả 6 commands; harness đặt DB_NAME target và secret test riêng trong bộ nhớ rồi gọi tuần tự:

```text
node node_modules/tsx/dist/cli.mjs --test tests/auth.database.integration.ts
node node_modules/tsx/dist/cli.mjs --test tests/progress.database.integration.ts
node node_modules/tsx/dist/cli.mjs --test tests/quiz.database.integration.ts
node node_modules/tsx/dist/cli.mjs --test tests/gamification.database.integration.ts
node node_modules/tsx/dist/cli.mjs --test tests/admin.database.integration.ts
node node_modules/tsx/dist/cli.mjs --test tests/database.integration.ts
```

| Suite trên DB riêng | PASS | FAIL |
| --- | ---: | ---: |
| Auth lifecycle | 1 | 0 |
| Progress lifecycle | 1 | 0 |
| Quiz lifecycle | 1 | 0 |
| Gamification lifecycle | 1 | 0 |
| Admin lifecycle | 1 | 0 |
| Public read-only integration | 5 | 0 |
| **Tổng** | **10** | **0** |

Mỗi lifecycle là một Node test chứa nhiều HTTP/SQL assertions; không đếm từng assertion thành test mới. Public read-only 5 tests cũng đã chạy riêng trên DB gốc; đó là lượt kiểm thử ở môi trường khác, không phải 5 test case mới.

```powershell
Copy-Item -LiteralPath '.m9-verification/database-audit.json' -Destination '.m9-verification/database-audit-before.json'
node .m9-verification/postcheck.mjs
node .m9-verification/log-check.mjs
```

PASS: DB gốc đối chiếu khớp, DB test cleanup sạch, 20 logs không có findings theo patterns đã nêu. Audit ban đầu được giữ nguyên; file `database-audit-before.json` là bản sao để postcheck tham chiếu.

### Admin Web (working directory `admin-web`)

```powershell
npm run build
npm test
npm test -- tests/auth-client.test.ts
npm test -- tests/auth-client.test.ts tests/pages.test.tsx
npm run build
npm run format:check
npm test
npm run build
```

Đã dùng `npx prettier --write` chỉ cho 5 source/test files được sửa (lượt cuối chỉ auth-client.test.ts). Các commands test/build có lặp khi phát hiện/chỉnh lỗi, không coi lượt trung gian thất bại là final PASS.

- Baseline build PASS, tests **32/32 PASS**.
- Red reproduction: auth-client **3 FAIL / 14 PASS** như mục 6.
- Lượt intermediate toàn bộ **2 FAIL / 34 PASS**, nguyên nhân/cách giải quyết ở mục 7.
- Sau fix hoàn chỉnh: **37/37 PASS**, build PASS (script gồm `tsc --noEmit && vite build`).
- Thêm 5 test security còn thiếu: lượt cuối **42/42 PASS**, format PASS; chạy lại build sau bổ sung test cũng PASS. Không thay source/test sau lượt kiểm tra cuối.
- Browser kiểm thử qua CUA ở mục 4, không chạy Playwright CLI hoặc dùng HTTP mock trong app.

Kiểm tra uptime cuối (working directory backend) bằng lệnh sau: cả health backend và trang Admin HTTP **200**. Hai probe này không cộng vào 68 case của `live-api.json`.

```powershell
node -e 'const endpoints=["http://127.0.0.1:3000/api/v1/health","http://localhost:5173/"]; for(const url of endpoints){const r=await fetch(url); console.log(JSON.stringify({url,status:r.status})); if(r.status!==200)process.exitCode=1;}'
```

## 10. Phân biệt hệ thống thật và test doubles

| Bằng chứng | Backend | PostgreSQL | UI |
| --- | --- | --- | --- |
| Backend 311 tests | Source thật, crypto/Express tùy suite | Repository doubles, không DB thật | Không browser |
| DB gốc read-only 5 tests | Source Express thật, port riêng | flashcard_english thật, chỉ đọc | Không browser |
| 68 HTTP checks | Server đang chạy 3000 thật | flashcard_english thật | Không browser; JWT test trong memory |
| Isolated 10 integration tests | Source Express/services/repos thật, port riêng | DB test mới thật, có ghi/cleanup được phép | Không browser |
| Admin 42 tests | Fetch fixtures/error injection | Không PostgreSQL | React/jsdom, test doubles |
| Browser Admin | Proxy gọi server 3000 thật | flashcard_english thật | Browser thật; các thao tác ở mục 4 |
| Android security/M8 | Đọc source và tài liệu đã nghiệm thu | Không test Android mới | Không build/install/phone test M9 |

Fixture chỉ ở test suite, không được đưa vào source app hoặc dùng để hiển thị giả dữ liệu hoàn thành.

## 11. Những phần chưa xác minh / giới hạn

- Chưa bấm lưu mọi create/update/status/role trong browser trỏ DB riêng; hiện có backend write integration thật và UI contract/confirm tests riêng. Không ghi lên DB gốc chỉ để hoàn tất manual checklist.
- Chưa chạy USER login, expired-token redirect và network/5xx fault injection end-to-end trong browser thật. Quyền và expired token có HTTP thật; state/guard/cache/retry/stale request có test fixtures như mục 5.
- Không làm real-user JWT hết hạn giả, đổi JWT secret, thu hồi quyền thật hoặc ngắt server đang phục vụ Android/Admin.
- Không kiểm tra production Internet deployment, HTTPS/CORS origin khác, nhiều múi giờ, tải lớn hay mọi viewport/focus/accessibility. Đây là nghiệm thu local theo M9, không phải security pentest toàn hệ thống.
- Không chạy lại Android/M8 vì không có thay đổi liên quan; giữ bằng chứng M8 DONE, không suy ra lượt test điện thoại mới từ việc đọc report.

## 12. Công việc còn lại và bàn giao

Không còn lỗi M9 đã phát hiện chưa sửa hoặc thao tác DB bị chặn. DB test giữ lại, trạng thái cleanup có postcheck; các services vận hành 3000/5173 không đổi. Người dùng có thể đăng nhập lại ở `http://localhost:5173` sau phép thử Logout.

Nếu cần mở rộng browser E2E sau này, cấu hình môi trường Admin/backend riêng trỏ DB test và dùng tài khoản fixture; không chạy script có flag ghi trên flashcard_english. Không dùng báo cáo này để cho phép DROP/TRUNCATE/reset hoặc tự đổi quyền tài khoản thật.

M9 dừng tại phạm vi đã giao. M8 vẫn DONE; không triển khai M10.
