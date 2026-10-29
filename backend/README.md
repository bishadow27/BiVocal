# BIPASSION Backend — Milestone 6A

Ứng dụng chính thức: **BIPASSION**. Frontend quản trị tương lai: **BIPASSION Admin**.
M6A chỉ làm backend authorization/Admin API, chưa làm frontend hoặc Android.
M1–M5 **CLOSED/PASS PostgreSQL theo xác nhận của bạn**, gồm migration gamification
APPLIED, M5 integration 1/1 và manual M3/M4/M5 PASS. Không chạy lại DB trong task này.
**DATABASE MIGRATION NOT APPLIED. ADMIN DATABASE INTEGRATION NOT TESTED.**
Hai trạng thái trên chỉ nói về migration/integration M6A. Task không kết nối PostgreSQL,
không đọc `.env` thật, không promote account, không sửa database/dataset.
Local: npm install/typecheck/build PASS; **311/311 tests PASS** (210 M1–M5 +101 M6A).
Chi tiết nằm trong `verification-report.txt`.

Backend foundation bằng Node.js, TypeScript strict, Express 5, PostgreSQL,
node-postgres (`pg.Pool`), Zod và dotenv. Đây là **modular monolith** với API
đọc topics/flashcards từ database hiện có và authentication bằng bcrypt/JWT.

Milestone 2 thêm migration users, register, login và current user. Milestone 3 thêm
user_progress, review đúng/sai, favorite, detail/list/summary theo user trong JWT.
Milestone 4 thêm Quiz EN_TO_VI multiple choice, quiz_results, history/summary và
transaction chung cho quiz result + progress. Milestone 5 thêm XP, streak, level,
leaderboard và gắn reward vào transaction Quiz/Review. M6A thêm Admin API và role.
Chưa có Admin Web, dictionary,
refresh token, password reset, email verification, OAuth, frontend hoặc Android.

## Yêu cầu

- **Node.js 22+**; đã kiểm thử bằng Node.js 24.19.0 và npm 11.17.0.
- PostgreSQL đang hoạt động, database **`flashcard_english` đã tồn tại**.
- Database đã có bảng `topics`, `flashcards` và seed 10 topics / 300 flashcards.
- Có tài khoản PostgreSQL được phép kết nối và `SELECT` trên hai bảng đó.
- Authentication cần bảng `users` đã migrate và quyền SELECT/INSERT/UPDATE trên users,
  cùng quyền USAGE trên `users_id_seq`. Migration cần tài khoản có quyền tạo bảng/index.
- Progress cần bảng `user_progress` đã migrate, quyền SELECT/INSERT/UPDATE trên bảng
  và USAGE trên `user_progress_id_seq`. Integration cần DELETE test user để FK cascade
  xóa progress; các API progress không DELETE. Không cần dependency mới cho Milestone 3.
- Quiz cần SELECT/INSERT trên `quiz_results`, USAGE trên `quiz_results_id_seq`, cùng
  quyền Progress ở trên. `SELECT ... FOR SHARE` dùng lúc submit còn cần quyền UPDATE
  trên ít nhất một cột flashcards (ví dụ chỉ cột id), theo
  [PostgreSQL SELECT permissions](https://www.postgresql.org/docs/current/sql-select.html).
  Source không có câu UPDATE flashcards và task này không cấp/thay đổi quyền database.
  Milestone 4 không thêm dependency hoặc biến JWT mới.

Backend không tự tạo database hoặc chạy migration khi khởi động.
Pool ứng dụng hỗ trợ read-write cho auth/progress/quiz repositories.
Topics/flashcards repositories vẫn chỉ SELECT; dataset baseline không đổi.
Migration chạy riêng bằng node-pg-migrate, không lấy `dataset/schema.sql` làm migration.
Database của bạn cần để hai bảng truy cập được qua search_path của tài khoản kết nối
(thông thường là schema `public`).

## Cài dependencies

Mở terminal tại thư mục project, sau đó:

```powershell
cd backend
npm install
```

`package-lock.json` đã có để cố định cây dependencies. Sau lần cài đầu,
có thể dùng `npm ci` để cài đúng lockfile trên máy khác.

## Cấu hình `.env`

Trong thư mục `backend/`:

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
```

Chỉnh file `.env`:

```dotenv
NODE_ENV=development
HOST=0.0.0.0
PORT=3000
DB_HOST=localhost
DB_PORT=5432
DB_NAME=flashcard_english
DB_USER=postgres
DB_PASSWORD=
JWT_SECRET=
JWT_EXPIRES_IN=1h
```

Điền mật khẩu thật vào `DB_PASSWORD` trên máy của bạn. **Không commit `.env`**;
`.gitignore` đã loại bỏ `.env` và các biến thể, chỉ giữ `.env.example`.
Không gửi `.env` khi nhờ review code. Có thể dùng environment variables thay cho file;
dotenv không ghi đè biến đã có trong môi trường. Chạy các lệnh từ `backend/` để dotenv
đọc đúng `.env` của backend.

`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` đều bắt buộc;
backend không đoán mật khẩu và không fallback sang dataset CSV hoặc dữ liệu mock.
Zod kiểm tra cấu hình và chỉ báo tên field lỗi, không log giá trị bí mật.
HOST/PORT mặc định là `0.0.0.0:3000`. Khi chỉ thử trên máy, có thể đặt `HOST=127.0.0.1`.

Thêm JWT fields vào `.env` hiện có bằng tay; không ghi đè cấu hình DB của Milestone 1.
JWT_SECRET cần ít nhất 32 ký tự, không dùng mật khẩu dễ đoán. Tạo giá trị ngẫu nhiên
trên máy của bạn bằng lệnh dưới rồi lưu riêng vào `.env`:

```powershell
node -e "console.log(require('node:crypto').randomBytes(32).toString('hex'))"
```

Không gửi giá trị đó vào chat/report hoặc commit Git. `JWT_EXPIRES_IN` mặc định `1h`;
nhận số nguyên dương với hậu tố `s`, `m`, `h`, `d`, ví dụ `30s`, `15m`, `2h`, `1d`.
Thời hạn tối đa một năm; không nhận chuỗi số thiếu đơn vị như `3600`.
Migration chỉ cần DB_*, không yêu cầu JWT_SECRET.

## Development, typecheck và build

```powershell
npm run dev
npm run typecheck
npm run build
```

`npm run dev` dùng tsx watch. `typecheck` kiểm tra cả source và test.
`build` tạo JavaScript trong `dist/`. Source bật strict và không dùng `any` hoặc
`@ts-ignore`.

Backend kiểm tra PostgreSQL **trước khi mở HTTP port**. Nếu thiếu cấu hình hoặc
kết nối thất bại, tiến trình thoát với exit code 1 và thông báo an toàn.
Không có fallback trả health thành công khi PostgreSQL đang lỗi.
Pool có timeout kết nối/truy vấn và bắt lỗi idle connection để tránh lỗi không xử lý.

## Production

Trong `backend/`, cấu hình DB_* và JWT_SECRET hợp lệ rồi chạy:

```powershell
npm run build
$env:NODE_ENV = 'production'
npm start
```

Ứng dụng chạy `node dist/server.js`; SIGINT/SIGTERM đóng HTTP server và pool.
Đây là foundation cho môi trường local/môn học, chưa phải cấu hình deployment public.
Runtime không trả stack trace hoặc raw PostgreSQL error trong API, kể cả development.

## API `/api/v1`

| Method | Endpoint | Kết quả |
|---|---|---|
| GET | `/api/v1/health` | HTTP 200 khi PostgreSQL phản hồi; HTTP 503 nếu không truy cập được |
| GET | `/api/v1/topics` | Danh sách topic, sắp theo name rồi id |
| GET | `/api/v1/topics/:code/flashcards` | Các flashcard active thuộc topic code; sắp theo id |
| GET | `/api/v1/flashcards/:id` | Chi tiết flashcard active |
| POST | `/api/v1/auth/register` | Tạo user, trả 201 với user DTO và accessToken |
| POST | `/api/v1/auth/login` | Trả 200 với user DTO và accessToken |
| GET | `/api/v1/auth/me` | User hiện tại; cần Bearer access token |
| GET | `/api/v1/progress` | Progress của current user, chỉ flashcard active |
| GET | `/api/v1/progress/summary` | Tổng hợp progress của current user |
| GET | `/api/v1/progress/:flashcardId` | Progress một flashcard của current user |
| POST | `/api/v1/progress/:flashcardId/review` | Review đúng/sai, atomic UPSERT |
| PUT | `/api/v1/progress/:flashcardId/favorite` | Đặt favorite true/false, atomic UPSERT |

Health thành công:

```json
{"status":"ok","database":"connected"}
```

Topics trả một array; mỗi phần tử có `id`, `code`, `name`, `description`, `imageUrl`.
Không trả `created_at` hoặc `updated_at`.

Flashcards trả DTO camelCase:

```json
{
  "id": 1,
  "english": "apple",
  "vietnamese": "quả táo",
  "pronunciation": "/ˈæpəl/",
  "partOfSpeech": "noun",
  "example": "I eat an apple every morning.",
  "exampleVi": "Tôi ăn một quả táo mỗi sáng.",
  "level": "A1"
}
```

ID ở trên chỉ là ví dụ. Luôn lấy ID thực từ response danh sách; backend không
giả định FOOD có ID 1. Truy vấn flashcards dùng `topics.code` và join khóa ngoại.
Các cột nullable trong schema được trả `null` khi database có giá trị null.
Flashcard `is_active=false` hoặc null bị loại khỏi danh sách và detail.

Topic code phải đúng mã uppercase, dài 1–50 ký tự: `FOOD`, `TECHNOLOGY`, v.v.
Code có dạng không hợp lệ → 400 `INVALID_TOPIC_CODE`.
Code hợp lệ nhưng không tồn tại → 404 `TOPIC_NOT_FOUND`.
Topic có tồn tại nhưng không có flashcard active → 200 `[]`.

ID phải là số nguyên dương nằm trong miền PostgreSQL SERIAL `1..2147483647`.
Không nhận `1abc`, `1.5`, `0`, `-1`, `01`, `1e3` hoặc số vượt giới hạn.
ID không hợp lệ → 400 `INVALID_FLASHCARD_ID`.
ID hợp lệ không tồn tại hoặc không active → 404:

```json
{
  "error": {
    "code": "FLASHCARD_NOT_FOUND",
    "message": "Flashcard not found"
  }
}
```

Mọi lỗi dùng `{ "error": { "code": "...", "message": "..." } }`.
Route không tồn tại → 404 `ROUTE_NOT_FOUND`; path encoding lỗi → 400
`INVALID_REQUEST_PATH`; lỗi nội bộ khác → 500 `INTERNAL_SERVER_ERROR`.
Các route topics/flashcards không có thao tác ghi; POST/PUT/DELETE đến chúng trả 404.
Auth register/login và progress mutations nhận JSON, giới hạn body 16 KiB. JSON lỗi → 400 `INVALID_JSON`;
body quá lớn → 413 `PAYLOAD_TOO_LARGE`.

## Thử trực tiếp bằng PowerShell

Sau khi `npm run dev` hoặc `npm start` báo PostgreSQL connected,
mở terminal khác:

```powershell
Invoke-RestMethod http://localhost:3000/api/v1/health
Invoke-RestMethod http://localhost:3000/api/v1/topics
$cards = @(Invoke-RestMethod http://localhost:3000/api/v1/topics/FOOD/flashcards)
$cards.Count
Invoke-RestMethod "http://localhost:3000/api/v1/flashcards/$($cards[0].id)"
curl.exe -i http://localhost:3000/api/v1/flashcards/2147483647
curl.exe -i http://localhost:3000/api/v1/topics/UNKNOWN/flashcards
```

Trên snapshot seed chưa bổ sung/vô hiệu hóa, `$cards.Count` phải bằng **30**.
Hai lệnh curl cuối kỳ vọng 404. Nếu database đã được mở rộng, số flashcard
có thể thay đổi theo dữ liệu thực; backend không giới hạn 30/topic hoặc 300 tổng cộng.

## Test không cần database

```powershell
npm test
```

Các test khởi động **Express HTTP server thật trên port ngẫu nhiên** và dùng
repository doubles trong bộ nhớ. Không kết nối PostgreSQL và không sửa seed.
Kiểm tra health thành công/thất bại, topics, flashcards theo code, detail, unknown
topic, missing flashcard, topic rỗng, tham số lỗi, camelCase DTO, async error,
route lỗi và bảo mật thông báo cấu hình. Đây không phải test database integration.

## Test integration riêng, chỉ đọc database

Cấu hình `.env` hoặc DB_* cho database thật rồi chạy:

```powershell
npm run test:integration
```

Suite dùng `pg.Pool` thật và các repository thật qua HTTP. Không tạo schema,
không insert/update/delete/truncate/drop và không chạy lại seed.
Test không tự skip hay thay bằng mock nếu credentials thiếu: nó fail với lỗi cấu hình.
Suite kỳ vọng snapshot hiện tại có 10 mã topic seed và **30 FOOD flashcards active**;
khi dữ liệu runtime được mở rộng, có thể phải điều chỉnh kỳ vọng test đó.

Theo trạng thái bạn cung cấp, Milestone 1 integration đã PASS 5/5, cả sau migration users.
Lần làm Milestone 5 không chạy lại database integration và không áp dụng migration mới.
Các test regression HTTP vẫn chạy không cần PostgreSQL. Xem `verification-report.txt`.

## Migration users

Dependencies mới: `bcrypt`, `jsonwebtoken`; tooling `node-pg-migrate` và các types.
Migration đầu tiên: `migrations/1791072000000_create_users.sql`.
UP chỉ tạo users và hai unique indexes `idx_users_email_unique` / `idx_users_username_unique`
trên LOWER(email)/LOWER(username). Không tạo user seed hoặc admin mặc định.
Email và username do API chấp nhận dùng ký tự Latin/ASCII; unique indexes xử lý hoa/thường.

node-pg-migrate tự quản lý bảng lịch sử `pgmigrations`, advisory lock và transaction.
Hai bảng dataset có sẵn là baseline; không thêm migration recreate/reseed chúng.
Lệnh runner sử dụng DB_* trực tiếp, không cần hard-code DATABASE_URL hoặc mật khẩu.
Schema mặc định là public; database hiện tại phải có baseline trong schema/search_path phù hợp.

Theo trạng thái bạn cung cấp, migration users của Milestone 2 **đã apply thành công**
và auth integration **PASS 1/1**. File migration cũ được giữ nguyên byte-for-byte.
Theo kết quả bạn cung cấp, migration `user_progress` của Milestone 3 cũng **đã apply**,
progress integration **PASS 1/1**, auth regression **PASS 1/1**, M1 regression **PASS 5/5**
và manual Progress API **PASS**. Cả hai migration cũ giữ nguyên byte-for-byte.
Theo trạng thái bạn cung cấp, migration `quiz_results` của Milestone 4 **đã apply**;
Quiz integration 1/1, rollback, concurrency, cleanup, regression và manual API đều PASS.
Cả bốn migration M2–M5 giữ nguyên byte-for-byte. Migration gamification M5 đã APPLIED
theo xác nhận mới nhất; migration còn thiếu là **M6A add_admin_authorization**.
Sau khi review toàn bộ migration mới, bạn tự chọn database đúng và chạy từ `backend/`:

```powershell
npm run migrate:up
```

Lệnh trên là thao tác ghi schema thật. Nó không chạy từ `npm start`, `build` hoặc `test`.
Không chạy lại `dataset/schema.sql` để thêm users. Chạy UP lần sau chỉ áp dụng migration
chưa có trong lịch sử, không seed lại 300 flashcard.

Tạo migration mới sau này (chỉ tạo source, chưa chạy SQL):

```powershell
npm run migrate:create -- descriptive_name
```

Rollback **chỉ trong development**, khi chấp nhận mất các user của migration:

```powershell
npm run migrate:down
```

DOWN rollback một migration gần nhất. Migration users chỉ DROP users và các index/sequence
do bảng đó sở hữu; không dùng CASCADE, không DROP topics/flashcards/database.
Không có rollback tự động. Không đổi migration đã áp dụng; thay đổi schema tương lai
phải là một migration mới.

## Register, login và current user

Register:

```http
POST /api/v1/auth/register
Content-Type: application/json

{"username":"phuc123","email":"phuc@example.com","password":"StrongPassword123","fullName":"Nguyen Duy Phuc"}
```

Username trim, 3–30 ký tự, chỉ A–Z/a–z/0–9/underscore; giữ cách viết hoa khi lưu.
Email trim, validate, tối đa 150 ký tự và lưu lowercase.
Password không trim: tối thiểu 8 ký tự Unicode, tối đa **72 byte UTF-8** trước bcrypt.
Mật khẩu gồm emoji/dấu tiếng Việt vẫn phải thỏa byte limit, không bị truncate âm thầm.
fullName optional; nếu có thì trim, 1–100 ký tự. JSON có field không hỗ trợ bị từ chối.
Bcrypt hash với cost 12; plain password không được lưu.

Response HTTP 201:

```json
{
  "user": {"id":1,"username":"phuc123","email":"phuc@example.com","fullName":"Nguyen Duy Phuc","avatarUrl":null},
  "accessToken":"<JWT access token>"
}
```

Không có password/password_hash trong DTO. Nếu unique index trả PostgreSQL 23505,
service map tên index sang 409 `EMAIL_ALREADY_EXISTS` hoặc `USERNAME_ALREADY_EXISTS`.
Không dựa vào SELECT-before-INSERT, nên uniqueness vẫn do PostgreSQL đảm bảo khi có
request đồng thời. Lỗi PG khác dùng response 500 an toàn, không trả SQL/connection string.

Login:

```http
POST /api/v1/auth/login
Content-Type: application/json

{"email":"phuc@example.com","password":"StrongPassword123"}
```

Email normalize lowercase. Thành công trả HTTP 200 cùng dạng `{user, accessToken}`
và cập nhật `last_login = CURRENT_TIMESTAMP`.
Email không tồn tại và password sai đều trả 401 với cùng response:

```json
{"error":{"code":"INVALID_CREDENTIALS","message":"Invalid email or password"}}
```

GET current user:

```http
GET /api/v1/auth/me
Authorization: Bearer <accessToken>
```

Response gồm id, username, email, fullName, avatarUrl, createdAt (ISO timestamp).
Không trả hash hoặc raw PostgreSQL row. Identity chỉ lấy từ `sub` trong JWT đã verify;
không lấy user ID từ body/query.
Token ký HS256, payload chỉ sub/iat/exp, expiry từ env; verify chỉ cho HS256 và
kiểm tra sub là ID nguyên dương trong miền SERIAL. Không có refresh token.

| Trường hợp | HTTP | Error code |
|---|---:|---|
| Register/login validation lỗi | 400 | VALIDATION_ERROR |
| Email đã tồn tại | 409 | EMAIL_ALREADY_EXISTS |
| Username đã tồn tại | 409 | USERNAME_ALREADY_EXISTS |
| Sai thông tin login | 401 | INVALID_CREDENTIALS |
| Không có Authorization header | 401 | AUTH_TOKEN_MISSING |
| Header/token sai, chữ ký sai hoặc token hết hạn | 401 | AUTH_TOKEN_INVALID |
| Token hợp lệ nhưng user bị xóa | 401 | AUTH_USER_NOT_FOUND |

Ví dụ dùng PowerShell sau khi đã apply migration:

```powershell
$loginBody = @{ email='phuc@example.com'; password='StrongPassword123' } | ConvertTo-Json
$session = Invoke-RestMethod http://localhost:3000/api/v1/auth/login -Method Post -ContentType 'application/json' -Body $loginBody
Invoke-RestMethod http://localhost:3000/api/v1/auth/me -Headers @{ Authorization="Bearer $($session.accessToken)" }
```

## Test auth và phạm vi kiểm chứng

```powershell
npm run typecheck
npm run build
npm test
```

`npm test` chỉ chọn `tests/*.test.ts`: 17 test Milestone 1, 28 test auth/JWT Milestone 2,
43 test progress Milestone 3 và 57 test Quiz Milestone 4, cộng các test M5 mới.
Xem tổng kết kiểm thử M5 trong `verification-report.txt`.
Auth test chạy HTTP, bcrypt và JWT thật với repository doubles,
không kết nối PostgreSQL, không đọc `.env`, không chạy migration.
Test covers byte boundary, normalize, duplicate mapping, login, last_login, DTO,
token sai/hết hạn, JSON lỗi và việc không lộ hash/DB_PASSWORD/JWT_SECRET.

Auth PostgreSQL integration **PASS 1/1 theo kết quả bạn cung cấp trước Milestone 4**;
không chạy lại trong lần bàn giao này. Khi bạn muốn kiểm tra regression sau migration mới,
cấu hình JWT_SECRET trên máy rồi chủ động bật lệnh test có ghi user riêng:

```powershell
$env:AUTH_INTEGRATION_ALLOW_WRITES = '1'
npm run test:auth:integration
Remove-Item Env:AUTH_INTEGRATION_ALLOW_WRITES
```

Suite không apply migration. Nó kiểm tra users tồn tại, tạo user UUID riêng qua API,
kiểm tra hash trong DB, login/last_login/me và uniqueness hoa/thường.
Cleanup chỉ user có ID + username UUID + email chính xác của test, kể cả khi assertion
fail; không DELETE user theo prefix và không DELETE user thật. Không TRUNCATE/DROP.
Suite so sánh toàn bộ topics/flashcards trước/sau và xác nhận 10 topic/30 FOOD cards.
Như các INSERT PostgreSQL khác, test làm tăng users_id_seq dù cleanup user; không rollback
migration hoặc lịch sử dữ liệu production. Dùng database development để chạy test ghi.

## User progress — Milestone 3

Migration Milestone 3 đã apply theo trạng thái bạn cung cấp: `migrations/1791072001000_create_user_progress.sql`.
Không sửa migration `1791072000000_create_users.sql` đã apply; không sửa baseline/dataset.
UP chỉ tạo `user_progress`, unique index và indexes lookup. DOWN chỉ DROP user_progress
và các object do bảng này sở hữu; không CASCADE sang users/topics/flashcards.
Lệnh `migrate:down` rollback một migration gần nhất; với Milestone 3 sẽ mất progress.
Chỉ cân nhắc rollback trong dev sau khi hiểu migration nào đang ở cuối lịch sử.
Không chạy DOWN trong lần triển khai source này hoặc tự động khi test fail.

| Cột | Kiểu / quy tắc |
|---|---|
| id | SERIAL PRIMARY KEY |
| user_id | INTEGER NOT NULL, FK users(id), ON DELETE CASCADE |
| flashcard_id | INTEGER NOT NULL, FK flashcards(id), ON DELETE CASCADE |
| memory_level | SMALLINT NOT NULL DEFAULT 0, CHECK 0..5 |
| review_count / correct_count / wrong_count | INTEGER NOT NULL DEFAULT 0, từng cột CHECK >= 0 |
| is_favorite | BOOLEAN NOT NULL DEFAULT FALSE |
| last_review / next_review | TIMESTAMP nullable |
| created_at / updated_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

Unique index `idx_user_progress_user_flashcard_unique(user_id, flashcard_id)` đảm bảo
mỗi user chỉ có một row cho mỗi flashcard. Có indexes lookup user_id và flashcard_id.
Không có trigger updated_at: mọi mutation cập nhật timestamp bằng SQL.

Tất cả 5 progress endpoints cần `Authorization: Bearer <accessToken>`, tái sử dụng
JWT middleware Milestone 2. Thiếu/sai/hết hạn token trả các mã AUTH_TOKEN_* hiện có.
Owner luôn lấy từ JWT đã verify; body chứa userId bị strict schema từ chối.
Query/header userId tùy ý không tham gia xác định owner. API không trả userId/user_id.

Review:

```http
POST /api/v1/progress/1/review
Authorization: Bearer <accessToken>
Content-Type: application/json

{"correct":true}
```

Body chỉ có `correct: boolean`, không coercion hoặc field thừa.
Đúng: reviewCount +1, correctCount +1, memoryLevel +1 tối đa 5.
Sai: reviewCount +1, wrongCount +1, memoryLevel -1 tối thiểu 0.
Review đầu tiên đúng tạo counts 1/1/0 và memory 1; sai tạo counts 1/0/1 và memory 0.
lastReview/updatedAt lấy CURRENT_TIMESTAMP; createdAt và favorite được giữ nguyên khi update.
nextReview giữ null; chưa có scheduling/spaced repetition.

Repository dùng INSERT ... SELECT flashcard active ... ON CONFLICT DO UPDATE RETURNING.
Counters được cộng trực tiếp vào row hiện có trong PostgreSQL, không đọc rồi tính/ghi
ở JavaScript. ON CONFLICT bảo đảm thao tác INSERT/UPDATE atomic; xem
[PostgreSQL INSERT](https://www.postgresql.org/docs/current/sql-insert.html).
Khả năng xử lý request đồng thời trên DB thật chỉ được xác nhận khi chạy integration.

Favorite:

```http
PUT /api/v1/progress/1/favorite
Authorization: Bearer <accessToken>
Content-Type: application/json

{"isFavorite":true}
```

Body chỉ có `isFavorite: boolean`. Row mới có counters/memory 0, review dates null.
Row cũ chỉ cập nhật isFavorite/updatedAt; không reset counters, memory hoặc lastReview.
Gửi false đặt favorite=false, không xóa progress.

Review/favorite/detail thành công trả HTTP 200 với cùng DTO:

```json
{
  "flashcardId": 1,
  "memoryLevel": 1,
  "reviewCount": 1,
  "correctCount": 1,
  "wrongCount": 0,
  "isFavorite": false,
  "lastReview": "2026-10-04T00:00:00.000Z",
  "nextReview": null,
  "createdAt": "2026-10-04T00:00:00.000Z",
  "updatedAt": "2026-10-04T00:00:00.000Z"
}
```

ID phải đúng positive SERIAL 1..2147483647: malformed/0/-1/decimal/01/1e3/overflow
trả 400 INVALID_FLASHCARD_ID. ID hợp lệ nhưng không tồn tại hoặc inactive trả
404 FLASHCARD_NOT_FOUND. Mutations không tạo/update progress cho các card đó.
Disable flashcard không tự xóa progress cũ; list/summary/detail chỉ gồm card active.
GET detail của card active chưa có progress cho current user trả:

```json
{"error":{"code":"PROGRESS_NOT_FOUND","message":"Progress not found"}}
```

GET `/progress` trả array DTO, chỉ current user, order updated_at DESC rồi flashcard_id ASC.
Chưa cần pagination với baseline 300 flashcard. `/progress/summary` được đăng ký trước
route `/:flashcardId` và chỉ tổng hợp progress của current user gắn với card active:

```json
{"trackedCards":20,"reviewedCards":15,"favoriteCards":4,"masteredCards":3,
 "totalReviews":42,"correctAnswers":30,"wrongAnswers":12,"accuracy":71.43}
```

trackedCards = số rows; reviewedCards = rows có review_count > 0;
favoriteCards = rows is_favorite=true; masteredCards = rows memory_level=5.
totalReviews/correctAnswers/wrongAnswers là SUM các counters tương ứng.
accuracy = correctAnswers / totalReviews * 100, làm tròn 2 chữ số; null nếu chưa review.
Không có progress thì counts đều 0 và accuracy=null.
Validation body trả 400 VALIDATION_ERROR; lỗi repository bất ngờ dùng generic 500
INTERNAL_SERVER_ERROR, không trả raw SQL, stack hoặc secret.

Ví dụ manual API trên PowerShell sau khi tự apply migration:

```powershell
$headers = @{ Authorization="Bearer $($session.accessToken)" }
$cards = @(Invoke-RestMethod http://localhost:3000/api/v1/topics/FOOD/flashcards)
$id = $cards[0].id
Invoke-RestMethod "http://localhost:3000/api/v1/progress/$id/review" -Method Post -Headers $headers -ContentType 'application/json' -Body '{"correct":true}'
Invoke-RestMethod "http://localhost:3000/api/v1/progress/$id/favorite" -Method Put -Headers $headers -ContentType 'application/json' -Body '{"isFavorite":true}'
Invoke-RestMethod "http://localhost:3000/api/v1/progress/$id" -Headers $headers
Invoke-RestMethod http://localhost:3000/api/v1/progress -Headers $headers
Invoke-RestMethod http://localhost:3000/api/v1/progress/summary -Headers $headers
```

## Progress verification và integration riêng

Milestone 3 unit/API tests dùng HTTP server thật, JWT thật và repository doubles.
43 test cover identity isolation, validation, review/favorite rules, DTO, summary,
inactive cards, dates, body limits và safe errors. Chúng không chứng minh SQL/constraints
hoặc concurrency trên PostgreSQL; bộ integration riêng mới kiểm tra các phần đó.
17 tests Milestone 1 và 28 tests Milestone 2 được giữ nguyên và PASS.

Progress integration **PASS 1/1 theo kết quả bạn cung cấp trước Milestone 4**;
suite này không được chạy lại trong task Milestone 4. Khi bạn chủ động kiểm tra
regression sau migration Quiz, xác nhận user_progress vẫn tồn tại rồi chạy từ backend/:

```powershell
$env:PROGRESS_INTEGRATION_ALLOW_WRITES = '1'
try { npm run test:progress:integration }
finally { Remove-Item Env:PROGRESS_INTEGRATION_ALLOW_WRITES }
```

Suite fail rõ trước khi mở pool nếu chưa có write flag, và fail nếu user_progress
chưa tồn tại. Nó không tự apply migration. Hai user UUID được tạo bằng register API,
review/favorite/detail/list/summary dùng một flashcard FOOD thật; test kiểm tra DB row,
cap/floor, nhiều review đồng thời không mất count, unique pair và isolation A/B.
Topics/flashcards được snapshot toàn bộ trước/sau; FOOD vẫn 30 và topics vẫn 10.
Không disable/update card baseline để test inactive; trường hợp đó được kiểm tra bằng doubles.

Cleanup nằm trong finally, cố gắng xử lý cả hai test identities dù một cleanup lỗi.
DELETE user dùng đúng ID + UUID username + email; FK cascade chỉ xóa progress của
test user đó. Suite kiểm tra user và progress đã hết sau cleanup. Không DELETE prefix,
TRUNCATE, DROP hoặc sửa seed. INSERT/UPSERT có thể tăng sequences dù cleanup; dùng DB dev.

Sau khi progress integration hoàn tất, bạn chủ động chạy lại auth integration với
AUTH_INTEGRATION_ALLOW_WRITES=1 và suite đọc `npm run test:integration` để kiểm tra regression.
Không đưa bất kỳ integration có ghi nào vào `npm test` mặc định; không gửi/commit `.env`.

## Quiz — Milestone 4

Chỉ có multiple choice **English → Vietnamese (EN_TO_VI)**, chưa có reverse/free-text,
listening hoặc AI-generated quiz. XP/streak/gamification được bổ sung ở Milestone 5 bên dưới.

Migration mới: `migrations/1791072002000_create_quiz_results.sql`.

| Cột | Kiểu / quy tắc |
|---|---|
| id | SERIAL PRIMARY KEY |
| user_id | INTEGER NOT NULL, FK users(id), ON DELETE CASCADE |
| flashcard_id | INTEGER NOT NULL, FK flashcards(id), ON DELETE CASCADE |
| selected_flashcard_id | INTEGER nullable, FK flashcards(id), ON DELETE SET NULL |
| is_correct | BOOLEAN NOT NULL |
| question_type | VARCHAR(30) NOT NULL DEFAULT 'EN_TO_VI', CHECK chỉ EN_TO_VI |
| answered_at | TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

Indexes phục vụ user_id, (user_id, answered_at DESC), flashcard_id. Không unique
user/flashcard vì mỗi lần submit là một result mới. UP chỉ tạo quiz_results và các
object cần thiết; DOWN chỉ DROP quiz_results, không CASCADE sang baseline.
Không sửa hai migration users/user_progress đã apply. Không chạy lại seed/schema.sql.

Migration Quiz và gamification **APPLIED theo xác nhận của bạn trước M6A**. Migration
còn thiếu hiện tại là M6A; chỉ apply sau review. Không chạy tự động từ startup/build/test.
`migrate:down` luôn rollback migration cuối: khi Quiz ở cuối lịch sử, lệnh sẽ mất
quiz history. Không chạy DOWN trên database thật trong task này hoặc khi test fail.

Tất cả endpoints cần `Authorization: Bearer <accessToken>` và tái sử dụng middleware M2:

| Method | Endpoint | Query/body |
|---|---|---|
| GET | `/api/v1/quiz/questions` | topicCode optional; limit default 10, 1..20 |
| POST | `/api/v1/quiz/answer` | flashcardId, selectedFlashcardId |
| GET | `/api/v1/quiz/history` | limit default 20, 1..100 |
| GET | `/api/v1/quiz/summary` | Không cần query |

### Questions

`GET /api/v1/quiz/questions?topicCode=FOOD&limit=10` trả array câu hỏi, ví dụ:

```json
[
  {
    "flashcardId": 1,
    "questionType": "EN_TO_VI",
    "prompt": "apple",
    "options": [
      {"flashcardId": 7, "text": "quả chuối"},
      {"flashcardId": 1, "text": "quả táo"},
      {"flashcardId": 4, "text": "sữa"},
      {"flashcardId": 5, "text": "trứng"}
    ]
  }
]
```

IDs/text ở đây chỉ minh họa; lấy ID thật từ API. Mỗi question có đúng 4 options,
gồm chính flashcard đó và 3 distractors active **cùng topic**. Không trùng ID/text
trong một question; kiểm tra text bằng trim + Unicode NFC + lowercase.
Questions và options được shuffle bằng Fisher–Yates + `crypto.randomInt`.
Không có vị trí correct cố định hoặc field correctIndex/isCorrect/correctAnswer.
GET không ghi quiz_results/user_progress và không trả raw row/progress/owner.

Không truyền topicCode thì lấy candidates từ toàn bộ dataset nhưng options vẫn cùng
topic với question. topicCode theo validation M1; unknown topic trả 404 TOPIC_NOT_FOUND.
Topic cần ít nhất 4 nghĩa Việt khác nhau. Response có đúng số câu hỏi distinct yêu cầu;
nếu không đủ eligible cards cho limit, trả 400 QUIZ_NOT_ENOUGH_FLASHCARDS, không lặp câu.
Limit sai định dạng/range hoặc topicCode malformed trả 400 VALIDATION_ERROR.

**Giới hạn contract:** format được yêu cầu dùng cùng flashcardId cho câu hỏi và option
đúng, nên client có thể suy ra đáp án bằng cách so ID dù không có correct metadata.
Đây chưa phải protocol chống gian lận. Che hoàn toàn đáp án cần đổi contract sang ID
opaque/session; Milestone 4 giữ format đã yêu cầu. Các Read APIs M1 vẫn public.

### Submit và transaction

```http
POST /api/v1/quiz/answer
Authorization: Bearer <accessToken>
Content-Type: application/json

{"flashcardId":1,"selectedFlashcardId":7}
```

Body strict, hai ID phải là JSON number nguyên trong 1..2147483647, không coercion
từ string. Field thiếu/field thừa trả VALIDATION_ERROR; ID có giá trị sai trả
INVALID_FLASHCARD_ID. Không nhận userId/correct/isCorrect/score/xp/memoryLevel.
Question thiếu/inactive → 404 FLASHCARD_NOT_FOUND. Selected thiếu/inactive/khác topic
→ 400 INVALID_QUIZ_OPTION. Backend tự tính correct bằng hai ID đã validate.

Repository mở một PoolClient, BEGIN, đọc/validate question và option với FOR SHARE
theo thứ tự ID, INSERT quiz_results, gọi `upsertProgressReview(client, ...)`, COMMIT.
FOR SHARE giữ active/topic/text ổn định đến hết transaction. Bất kỳ lỗi nào đều đi
qua ROLLBACK; luôn release client, discard nếu rollback fail. Các statement trong
transaction dùng cùng client, không dùng pool.query; xem
[node-postgres transactions](https://node-postgres.com/features/transactions) và
[PostgreSQL row locks](https://www.postgresql.org/docs/current/explicit-locking.html).

Helper Progress là chính SQL UPSERT M3 được tách ra; standalone review dùng Pool,
Quiz dùng PoolClient. Không đổi counts/memory 0..5/favorite/createdAt/nextReview rule
hoặc DTO Progress. `toProgress` được export để Quiz dùng chung mapping.

Response HTTP 200:

```json
{
  "resultId": 25,
  "flashcardId": 1,
  "selectedFlashcardId": 7,
  "correct": false,
  "correctAnswer": "quả táo",
  "selectedAnswer": "quả chuối",
  "answeredAt": "2026-10-05T00:00:00.000Z",
  "progress": {
    "flashcardId": 1, "memoryLevel": 0, "reviewCount": 1,
    "correctCount": 0, "wrongCount": 1, "isFavorite": false,
    "lastReview": "2026-10-05T00:00:00.000Z", "nextReview": null,
    "createdAt": "2026-10-05T00:00:00.000Z", "updatedAt": "2026-10-05T00:00:00.000Z"
  }
}
```

correctAnswer chỉ được đặt tên/trả rõ sau submit. Theo contract M4, submit chấp nhận
bất kỳ option active cùng topic; chưa lưu quiz session để đối chiếu đúng 4 options
đã phát. Submit lại cùng card tạo result mới và tăng progress mỗi lần.

### History và summary

History chỉ thuộc JWT user, order answered_at DESC rồi id DESC, giới hạn 1..100:

```json
[
  {"id":25,"flashcardId":1,"question":"apple","selectedFlashcardId":7,
   "selectedAnswer":"quả chuối","correctAnswer":"quả táo","correct":false,
   "questionType":"EN_TO_VI","answeredAt":"2026-10-05T00:00:00.000Z"}
]
```

Selected card bị xóa: FK SET NULL và LEFT JOIN cho selectedFlashcardId/selectedAnswer=null.
History dùng text hiện tại của flashcard, còn is_correct là kết quả đã lưu lúc submit.
Disable card không tự xóa result; history/summary vẫn tính result đó. Nếu question card
bị delete, FK CASCADE xóa result theo schema được yêu cầu.

Summary đếm quiz_results của JWT user:

```json
{"totalAnswers":20,"correctAnswers":15,"wrongAnswers":5,"accuracy":75}
```

Accuracy = correctAnswers/totalAnswers*100, làm tròn 2 chữ số; chưa có result thì
counts 0 và accuracy=null; tất cả sai thì accuracy=0. Query/header userId bị bỏ qua,
body userId bị reject. Không DTO nào có userId/user_id, JWT info hoặc password hash.
Lỗi bất ngờ dùng generic 500 INTERNAL_SERVER_ERROR qua middleware hiện có.

## Quiz tests và PostgreSQL integration

Mốc trước M5: **145/145 PASS** (M1 17 + M2 28 + M3 43 + M4 57).
57 test M4 gồm 45 HTTP/API tests với JWT thật/repository doubles và 12 PoolClient
doubles kiểm tra thứ tự transaction, server-side grading, các failure/rollback/release
paths. Tests không đọc .env hoặc kết nối PostgreSQL và không chứng minh SQL/locks/atomicity thật.

Quiz PostgreSQL integration **PASS 1/1 theo xác nhận của bạn trước M5**;
không chạy lại trong task M5. Với source M5, Quiz/Review đều cần cả hai bảng gamification.
Source `tests/quiz.database.integration.ts` đã qua typecheck. Sau khi tự review,
apply migration và kiểm tra quiz_results/FK/indexes/pgmigrations, bạn mới chủ động chạy:

```powershell
$env:QUIZ_INTEGRATION_ALLOW_WRITES = '1'
try { npm run test:quiz:integration }
finally { Remove-Item Env:QUIZ_INTEGRATION_ALLOW_WRITES }
```

Suite fail rõ trước khi mở pool nếu thiếu flag; kiểm tra đủ ba bảng users/user_progress/
quiz_results; không tự migrate. Tạo hai user UUID qua register, xác minh question options
từ FOOD thật, correct/wrong results + progress, favorite preservation, history/summary,
JWT isolation, nhiều results cùng card và concurrent answers không mất increments.

Rollback test chỉ sửa counters của progress thuộc đúng user test (ID + UUID username +
email + card). Tạm đặt review_count sát giới hạn INTEGER rồi submit qua API để UPSERT
overflow sau INSERT result. Test kỳ vọng 500, result count không tăng và progress
không bị cập nhật; finally khôi phục counters. Không production hook/schema trigger
hoặc thay đổi baseline để ép lỗi. Rollback M4 đã PASS theo xác nhận của bạn;
chưa chạy lại với source M5 trong task này.

Cleanup finally cố gắng xóa chính xác cả hai test users, dùng ID + username + email,
xác nhận FK cascade xóa quiz_results/user_progress. Toàn bộ topics/flashcards được
snapshot trước/sau; FOOD vẫn 30, topics vẫn 10. Không disable/sửa/delete baseline,
không prefix/broad DELETE/TRUNCATE. Sequences có thể tăng kể cả khi rollback/cleanup;
dùng database development cho integration ghi.

Sau Quiz integration, chủ động chạy lại Progress integration (PROGRESS_INTEGRATION_ALLOW_WRITES=1),
Auth integration (AUTH_INTEGRATION_ALLOW_WRITES=1) rồi `npm run test:integration` M1.
Mỗi write flag nên được xóa sau khi chạy. Giữ nguyên các script cũ, integration không
thuộc npm test mặc định, không gửi hoặc commit .env/secret/token.

Manual Quiz API sau khi migration và integration đã được bạn kiểm tra:

```powershell
$headers = @{ Authorization="Bearer $($session.accessToken)" }
$questions = @(Invoke-RestMethod 'http://localhost:3000/api/v1/quiz/questions?topicCode=FOOD&limit=5' -Headers $headers)
$q = $questions[0]
$body = @{ flashcardId=$q.flashcardId; selectedFlashcardId=$q.options[0].flashcardId } | ConvertTo-Json
Invoke-RestMethod http://localhost:3000/api/v1/quiz/answer -Method Post -Headers $headers -ContentType 'application/json' -Body $body
Invoke-RestMethod 'http://localhost:3000/api/v1/quiz/history?limit=20' -Headers $headers
Invoke-RestMethod http://localhost:3000/api/v1/quiz/summary -Headers $headers
```

## Cấu trúc và luồng

```text
backend/
├── src/
│   ├── app.ts                 # Tạo Express app; không mở port lúc import
│   ├── server.ts              # Env, pool, startup, shutdown
│   ├── config/
│   │   ├── env.ts
│   │   └── database.ts
│   ├── modules/
│   │   ├── health/            # route/controller/service/repository
│   │   ├── topics/            # topic.route/controller/service/repository.ts
│   │   ├── flashcards/        # flashcard.route/controller/service/repository.ts
│   │   ├── auth/              # route/controller/service/repository/schema/types + password/token services
│   │   ├── progress/          # route/controller/service/repository/schema/types
│   │   └── quiz/              # route/controller/service/repository/schema/types
│   ├── shared/
│   │   ├── middleware/error-handler.ts
│   │   ├── middleware/authenticate.ts
│   │   ├── errors/app-error.ts
│   │   └── types/api.ts
│   └── routes.ts
├── tests/
│   ├── api.test.ts
│   ├── env.test.ts
│   ├── database.integration.ts
│   ├── auth.test.ts
│   ├── auth-env.test.ts
│   ├── auth.database.integration.ts
│   ├── support/auth-integration.repository.ts
│   ├── progress.test.ts
│   ├── progress.database.integration.ts
│   ├── support/progress-integration.repository.ts
│   ├── quiz.test.ts
│   ├── quiz-repository.test.ts
│   ├── quiz.database.integration.ts
│   └── support/quiz-integration.repository.ts
├── migrations/1791072000000_create_users.sql
├── migrations/1791072001000_create_user_progress.sql
├── migrations/1791072002000_create_quiz_results.sql
├── scripts/migrate.ts
├── .env.example
├── .gitignore
├── package.json
├── package-lock.json
├── tsconfig.json
├── tsconfig.test.json
├── verification-report.txt
└── README.md
```

Thêm module health nhỏ để endpoint này cũng tuân thủ luồng:

```text
Route → Controller → Service → Repository → PostgreSQL
```

Controller validate tham số với Zod; service xử lý not-found và tạo DTO;
repository giữ toàn bộ SQL tham số hóa bằng `$1`. App nhận các repository qua
constructor function để test không cần database; không có framework DI hoặc ORM.
Express 5 tự chuyển rejected async handlers đến centralized error middleware,
nên không cần try/catch lặp lại ở controller.

Tài liệu kỹ thuật tham khảo:
[Express error handling](https://expressjs.com/en/guide/error-handling/),
[pg.Pool](https://node-postgres.com/apis/pool),
[Zod](https://zod.dev/).
Auth/migration:
[bcrypt](https://github.com/kelektiv/node.bcrypt.js),
[jsonwebtoken](https://github.com/auth0/node-jsonwebtoken),
[node-pg-migrate](https://salsita.github.io/node-pg-migrate/api).

## Gamification — Milestone 5

M5 chỉ thêm XP, streak, longest streak, level và leaderboard; tiếp tục hỗ trợ toàn bộ
API M1–M4. Không có endpoint để client tự cộng XP. Không triển khai M6/Admin/Android.
SQL chỉ nằm trong repository, migration và integration-test repository.

### Migration và quyền truy cập

Migration mới: `migrations/1791072003000_create_gamification.sql`. UP tạo đúng hai bảng:

| Bảng | Cột và ràng buộc |
|---|---|
| user_scores | user_id INTEGER PK/FK users CASCADE; total_xp/current_streak/longest_streak INTEGER NOT NULL DEFAULT 0, không âm; longest_streak >= current_streak |
| user_scores | last_activity_date DATE nullable; created_at/updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP; không lưu level |
| xp_history | id SERIAL PK; user_id INTEGER NOT NULL FK users CASCADE; flashcard_id và quiz_result_id INTEGER nullable, FK tương ứng dùng SET NULL |
| xp_history | source_type VARCHAR(20) NOT NULL chỉ QUIZ/REVIEW; xp_amount INTEGER NOT NULL > 0; activity_date DATE NOT NULL; created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP |

Indexes: user_scores(total_xp DESC), xp_history(user_id), (user_id, created_at DESC),
(flashcard_id), unique (user_id, source_type, flashcard_id, activity_date) WHERE
flashcard_id IS NOT NULL, unique (quiz_result_id) WHERE quiz_result_id IS NOT NULL.
DOWN chỉ DROP xp_history rồi user_scores, không CASCADE. Task này không chạy UP/DOWN.
Ba migration M2–M4 đã apply được giữ nguyên byte-for-byte.

**Quyết định đã được bạn chấp thuận:** giữ `ON DELETE SET NULL`; helper bắt buộc
QUIZ có quizResultId hợp lệ khi tạo award. CHECK ở DB bắt buộc REVIEW có quiz_result_id
NULL, nhưng không ép QUIZ luôn NOT NULL vì điều đó sẽ chặn FK SET NULL khi xóa result.
QUIZ history cũ có thể còn lại với reference NULL sau khi result bị xóa. Việc xóa result
không trừ XP đã nhận. Ghi SQL trực tiếp có thể bỏ qua kiểm tra QUIZ ID của helper;
mọi award từ ứng dụng phải đi qua helper, không có write API riêng cho gamification.

Runtime cần SELECT/INSERT/UPDATE trên user_scores, SELECT/INSERT trên xp_history và
USAGE trên xp_history_id_seq, cùng quyền M1–M4. Integration cần quyền DELETE users
cho cleanup và UPDATE user_scores để thiết lập/khôi phục fixture của đúng user test.
Không tự cấp quyền hoặc sửa cấu hình database trong source task này.

### XP, UTC day và streak

| Hoạt động hợp lệ | XP | Có tính learning activity? |
|---|---|---|
| Quiz đúng đầu tiên/user/card/UTC day | 10 | Có |
| Quiz đúng lặp cùng ngày | 0 | Có |
| Quiz sai | 0, không tạo history | Có |
| Review đầu tiên/user/card/UTC day, đúng hoặc sai | 5 | Có |
| Review lặp cùng ngày | 0 | Có |
| Favorite, register, login, GET, sinh câu hỏi | 0 | Không |

QUIZ và REVIEW dedup riêng: tối đa **15 XP/card/UTC day**. Quiz sai trước rồi đúng
vẫn nhận 10 ở lần đúng đầu tiên. Mỗi Quiz lặp vẫn lưu result và tăng Progress;
mỗi Review lặp vẫn tăng counters/cập nhật memory level theo M3. Favorite/createdAt
của Progress được giữ nguyên. M3 Review DTO và M4 Answer DTO không thêm field XP.

Ngày luôn do PostgreSQL tính bằng `(CURRENT_TIMESTAMP AT TIME ZONE 'UTC')::date`.
CURRENT_TIMESTAMP cố định theo thời điểm bắt đầu transaction, không theo đồng hồ
client hoặc timezone trong request. SQL định dạng DATE thành YYYY-MM-DD khi đọc;
không chuyển DATE qua JS Date nên không lệch ngày do timezone/DateStyle.

Score tạo lazy khi có activity đầu tiên, kể cả Quiz sai (XP 0, streak 1).
Lần đầu streak 1; cùng ngày giữ nguyên; ngày kế tiếp tăng 1; gap > 1 ngày reset 1.
Longest luôn lấy max giá trị cũ và current mới. Transaction có ngày cũ hơn activity
đã lưu không làm lùi lastActivityDate hoặc reset streak mới hơn. Không cộng bù các
ngày quá khứ; tất cả tính theo ngày bắt đầu transaction.
GET trả streak đã lưu; khoảng nghỉ chỉ làm reset khi user học lại, GET không ghi DB.

Level = floor(totalXp / 100) + 1; xpIntoLevel = totalXp % 100; xpPerLevel = 100.
Ví dụ 0/99/100/250 XP tương ứng level 1/1/2/3. Không có bảng level.

### Ba GET endpoints có JWT

| Endpoint | Query | Response |
|---|---|---|
| `/api/v1/gamification/me` | Không nhận identity từ query | totalXp, level, xpIntoLevel, xpPerLevel, currentStreak, longestStreak, lastActivityDate |
| `/api/v1/gamification/xp-history` | limit default 20, 1..100 | id, sourceType, flashcardId, word, xpAmount, activityDate, createdAt |
| `/api/v1/gamification/leaderboard` | limit default 20, 1..100 | rank, userId, username, fullName, avatarUrl, totalXp, level, currentStreak, longestStreak |

Limit phải là canonical decimal string; reject 0/-1/1.5/1e2/01/abc, khoảng trắng,
dấu cộng, giá trị > 100 và query limit lặp. Các field query khác không làm identity.
Me/history luôn dùng userId từ JWT đã verify; query/body/header tùy ý không override.
Leaderboard cho phép public profile các user khác nhưng không email/hash/token/last_login.

Chưa có score: me trả zeros, level 1, xpPerLevel 100, lastActivityDate null, không INSERT.
History ORDER BY created_at DESC, id DESC; LEFT JOIN cho phép flashcardId/word NULL
khi card bị xóa; không trả quiz_result_id. Leaderboard chỉ total_xp > 0, PostgreSQL
RANK() theo total_xp DESC (tie cùng rank, có khoảng trống như 1/1/3); userId ASC khi tie.
Generic 500 giữ format error.code/error.message, không lộ SQL hoặc lỗi database gốc.

Ví dụ stats có dữ liệu:

```json
{"totalXp":125,"level":2,"xpIntoLevel":25,"xpPerLevel":100,"currentStreak":4,"longestStreak":7,"lastActivityDate":"2026-10-05"}
```

### Transaction và concurrency

Quiz: BEGIN → validate/lock active cards → INSERT quiz_results → UPSERT user_progress
→ recordLearningActivity → COMMIT. Review: BEGIN → UPSERT user_progress → helper → COMMIT.
Helper chỉ dùng PoolClient của caller, không mở transaction/pool mới. Quiz correct
được tính từ ID đã validate; request không được gửi correct/XP/date.

Helper INSERT xp_history ON CONFLICT DO NOTHING RETURNING xp_amount; chỉ cộng XP
thực sự được INSERT. Daily unique index và quiz_result unique index là nguồn quyết
định dedup; không dùng SELECT exists rồi INSERT. user_scores dùng UPSERT atomic
`total_xp = s.total_xp + EXCLUDED.total_xp`, CASE cho streak và GREATEST cho longest/date.
Không đọc score ra JavaScript rồi ghi đè nên tránh lost update từ hai card khác nhau.
Repeated same-card requests vẫn có result/counters riêng nhưng một award/source/day.

Bất kỳ lỗi reward đều đi qua ROLLBACK toàn transaction, kể cả result/progress/history
đã ghi trước đó. Client luôn release; nếu rollback fail thì release(discard=true).
Favorite không gọi helper và không tạo score. Không có retry làm duplicate result.

### Kiểm thử và integration tự chạy sau review

`npm install`, `npm run typecheck`, `npm run build`, `npm test`: PASS trên máy này.
Mốc M5: 210/210 tests, 10 suites, 0 fail/skip: M1 17, M2 28, M3 43, M4 57, M5 65.
M5 gồm 41 HTTP/API tests và 24 helper/transaction/read-query contract tests.
Các assertion cũ M1–M4 được giữ nguyên. Riêng fixture của 12 M4 transaction tests
inject no-op recorder để tiếp tục kiểm tra orchestration M4; 24 M5 tests bổ sung
kiểm tra helper thật và các transaction/failure paths mới. Production mặc định dùng
helper thật ở cả Quiz và Progress; no-op chỉ nằm trong test.

Unit doubles không thực thi SQL. Các nhánh CASE streak/UTC và RANK/filter/order có
source contract checks; concurrency, FK/index, streak thực tế, rollback persistence
chỉ được chứng minh khi chạy PostgreSQL integration. Typecheck không kiểm tra cú pháp SQL.
M5 migration và PostgreSQL integration **APPLIED/PASS theo xác nhận của bạn trước M6A**.
Lần bàn giao M5 trước đây chỉ kiểm chứng source; task M6A không chạy lại integration.

Sau khi review và chọn đúng database development, bạn chủ động:

1. Chạy lại typecheck/build/test.
2. Tự apply migration mới bằng `npm run migrate:up`; kiểm tra bảng/index/FK/check và pgmigrations.
3. Chạy M5 integration với flag dưới đây.
4. Chạy lại Quiz, Progress, Auth rồi M1 integration theo các lệnh ở phần trước.
5. Kiểm thử manual me/history/leaderboard sau Quiz hoặc Review.

```powershell
$env:GAMIFICATION_INTEGRATION_ALLOW_WRITES = '1'
try { npm run test:gamification:integration }
finally { Remove-Item Env:GAMIFICATION_INTEGRATION_ALLOW_WRITES }
```

Flag được kiểm tra trước khi mở pool. Suite không thuộc `npm test` mặc định và
không tự migrate/seed. Source kiểm tra tồn tại năm bảng user/activity, snapshot đủ
topics/flashcards, tạo hai user UUID riêng, GET không tạo score, XP theo source,
sáu Quiz và sáu Review đồng thời cùng card, hai Quiz khác card đồng thời, JWT isolation,
history/order/public DTO, zero-XP exclusion, leaderboard rank/ties, UTC streak.
Full ranking được đọc qua repository production trong integration để test users
không bị che bởi top 100 user có sẵn; public API vẫn giới hạn 100.

Streak fixture chỉ UPDATE score thuộc exact ID + UUID username + email, dùng ngày DB
yesterday/gap, sau đó gọi API production. Rollback fixture lưu score, đặt total_xp
của user test = 2147483647, dùng fresh card cho Quiz +10 và Review +5; kỳ vọng overflow
ở score sau history INSERT, rồi xác minh result/progress/history/score không đổi.
Score được restore trong finally. Không trigger, clock hook hay schema hack.

Cleanup cố gắng xử lý cả hai exact test identities, kể cả khi response register lỗi;
DELETE user bằng ID AND username AND email, verify cascade xóa cả user_scores,
xp_history, user_progress, quiz_results. Snapshot dataset phải khớp sau suite.
Không prefix delete/broad delete/truncate/reseed. Sequences tăng là bình thường.
Chạy suite trong cùng một UTC day và khi không có workload khác sửa leaderboard/dataset;
nếu chạy qua UTC midnight, rerun để tránh thay đổi kỳ vọng dedup/streak giữa các bước.

```powershell
# Sau khi có session đăng nhập hợp lệ trên máy bạn:
$headers = @{ Authorization="Bearer $($session.accessToken)" }
Invoke-RestMethod http://localhost:3000/api/v1/gamification/me -Headers $headers
Invoke-RestMethod 'http://localhost:3000/api/v1/gamification/xp-history?limit=20' -Headers $headers
Invoke-RestMethod 'http://localhost:3000/api/v1/gamification/leaderboard?limit=20' -Headers $headers
```

### Giới hạn đã biết

- M5 đã PASS PostgreSQL theo xác nhận của bạn; task M6A không chạy lại các DB regressions.
- Quyết định SET NULL/QUIZ ID ở helper đã được bạn duyệt; DB trực tiếp không enforce QUIZ ID lúc INSERT.
- Quiz M4 giữ contract option ID: có thể suy ra đáp án từ ID trùng question. Daily XP cap
  hạn chế lặp, không biến Quiz thành bài thi chống gian lận. Review vẫn là tự đánh giá.
- total_xp dùng INTEGER theo yêu cầu; vượt 2147483647 gây rollback/generic 500, không tự đổi sang BIGINT.
- Timestamp fields giữ TIMESTAMP như schema yêu cầu/các milestone trước; UTC date cho XP/streak
  được tính riêng trong DB. Dùng timezone cấu hình nhất quán giữa runtime và DB cho timestamp hiển thị.
- npm install exit 0, không thêm dependency; npm báo bcrypt/esbuild install scripts còn pending
  allowScripts. Binding hiện có hoạt động, toàn bộ tests/build PASS; không tự thay đổi allowScripts.

Tham khảo quy tắc FK và ngày transaction:
[PostgreSQL constraints](https://www.postgresql.org/docs/current/ddl-constraints.html),
[PostgreSQL date/time functions](https://www.postgresql.org/docs/current/functions-datetime.html).

## M6A — Admin authorization và Admin API

Tất cả 16 routes dưới /api/v1/admin dùng cùng login/JWT M2. Không có admin login mới.
Request đi qua authenticate → repository đọc users theo JWT sub → role ADMIN → controller.
Role không nằm trong JWT, không cache ở backend và không nhận từ header/query/body tùy ý.
Mỗi request kiểm tra DB nên token cũ nhận/mất quyền sau promote/demote; request đã được
authorize và đang xử lý không bị hủy giữa chừng. USER nhận 403 ADMIN_FORBIDDEN với
message “Administrator access required”; user đã bị xóa nhận 401 AUTH_USER_NOT_FOUND.
Lỗi DB dùng generic500. JSON chỉ parse sau JWT và role guard, giới hạn 32kb.

### Migration và bootstrap

Migration mới: migrations/1791072004000_add_admin_authorization.sql.
UP chỉ ALTER users thêm role VARCHAR(20) NOT NULL DEFAULT 'USER', CHECK USER/ADMIN.
Tài khoản hiện có mặc định USER. Không PostgreSQL ENUM, không seed admin, không nhận
role khi register. DOWN chỉ xóa column role và constraint đi kèm, không CASCADE.
Không thêm role index khi chưa có số liệu chứng minh cần index cho cột hai giá trị.
Bốn migration đã apply và toàn bộ dataset được đối chiếu SHA-256 trước/sau.

Không thêm unique index flashcard vì baseline đã có idx_flashcards_unique_word_meaning
trên LOWER(english), LOWER(vietnamese), toàn database, kể cả card inactive. Baseline
cũng unique topics.code và topics.name. Không ALTER topics/flashcards trong M6A.

CLI được chuẩn bị để bạn tự chạy sau review/migration; **chưa chạy trong task này**:

~~~powershell
npm run admin:set-role -- --email existing@example.com --role ADMIN
npm run admin:set-role -- --email existing@example.com --role USER
~~~

CLI trim/lowercase email, match exact LOWER(email), chỉ USER/ADMIN, user phải tồn tại.
Không tạo account, không in email/password/hash/token hoặc lỗi DB gốc; log ID và role.
CLI dùng DB_* với quyền UPDATE users.role; không yêu cầu JWT_SECRET. Có thể explicit
demote tài khoản đang dùng từ CLI; API chặn tự demote. Không xây chính sách last-admin.

Runtime role check cần SELECT users.role; role API cần UPDATE users.role. Admin CRUD
cần SELECT/INSERT/UPDATE topics/flashcards và USAGE topics_id_seq/flashcards_id_seq.
Các quyền M1–M5 vẫn cần. Nếu DB dùng GRANT theo từng column, phải cấp role column riêng.
Migration dùng account có quyền ALTER users. Source không tự cấp quyền database.

### Hợp đồng API cho BIPASSION Admin

| Method | Endpoint dưới /api/v1/admin | Query/body hoặc response |
|---|---|---|
| GET | /me | id, username, email của mình, fullName, avatarUrl, role |
| GET | /dashboard | range=7d (default), 30d, all |
| GET | /topics | search, page, limit |
| POST | /topics | code, name, description?, imageUrl? |
| PUT | /topics/:id | cùng body topic, full replacement |
| GET | /flashcards | search, topicCode, status, level, page, limit |
| POST | /flashcards | topicCode, english, vietnamese, pronunciation?, partOfSpeech?, example?, exampleVi?, level, isActive? |
| PUT | /flashcards/:id | cùng dữ liệu card nhưng không cho isActive |
| PATCH | /flashcards/:id/status | {isActive: boolean} |
| GET | /users | search, role, page, limit |
| GET | /users/:id | {user, progress, quiz, gamification} |
| GET | /users/:id/progress | search, page, limit |
| GET | /users/:id/quiz-history | page, limit |
| PATCH | /users/:id/role | {role: "USER" hoặc "ADMIN"} |
| GET | /stats/quiz | range, topicCode? |
| GET | /leaderboard | page, limit |

Create trả201, update/read trả200. Không DELETE topic/card/user; không mutate progress.
M2 register/login/me DTO và JWT minimal sub/iat/exp giữ nguyên; admin role chỉ đọc ở admin/me.
M1–M5 routes, grading, repeat Quiz, memory, XP, streak và response contracts không đổi.

Pagination: {items,page,limit,total,totalPages}; page default1, 1..2147483647, limit default20,
1..100. totalPages=0 khi không có dữ liệu; trang vượt tổng trả items=[] nhưng total đúng.
Canonical decimal string: reject 0/-1/01/1.5/1e2/abc, dấu cộng, khoảng trắng, repeated params.
Mỗi query field đã khai báo không nhận array; enum role/status/range/level whitelist.
Query fields ngoài contract bị bỏ qua, không dùng làm SQL hoặc identity. Không có raw sort.
Search trim tối đa200 ký tự; literal substring không phân biệt hoa/thường; escape %, _, backslash.
Thứ tự cố định: topic/card/user ID ASC; progress updatedAt DESC/cardId ASC;
quiz-history answeredAt DESC/id DESC; leaderboard XP DESC/userId ASC.

Topic DTO: id,code,name,description,imageUrl,flashcardCount (tất cả cards, cả inactive).
Code uppercase tối đa50, regex ^[A-Z][A-Z0-9_]*$ để khớp CHECK baseline đã apply.
Điểm này chặt hơn mô tả ký tự A-Z/0-9/underscore: không nhận chữ số/underscore ở đầu.
Không sửa schema cũ để nới rule. Name trim required1..100; description nullable tối đa5000;
imageUrl nullable tối đa2048, chỉ HTTP/HTTPS. Omitted nullable fields trong PUT được đặt NULL.
Duplicate code →409 TOPIC_CODE_EXISTS; duplicate name →409 TOPIC_NAME_EXISTS.
Missing topic →404 ADMIN_TOPIC_NOT_FOUND. Sửa topic không tự UPDATE flashcards.

Flashcard DTO: id,topicId,topicCode,topicName,english,vietnamese,pronunciation,partOfSpeech,
example,exampleVi,level,isActive,createdAt,updatedAt. Search english/vietnamese.
Status ACTIVE/INACTIVE/ALL(default); legacy NULL is_active được xem inactive.
English/Vietnamese trim required tối đa150/255. Pronunciation nullable150.
PartOfSpeech nullable, whitelist noun/verb/adjective/adverb/preposition/conjunction/pronoun/phrase
đúng baseline; example/exampleVi nullable5000; level required A1/A2/B1/B2 cho create/PUT.
isActive boolean, create defaulttrue; PUT không nhận isActive. Strict JSON rejects field lạ.
Duplicate word+meaning →409 FLASHCARD_EXISTS (global, cả inactive). Không coi chỉ English
trùng là duplicate khi Vietnamese khác. Topic phải tồn tại; reference race →409
ADMIN_REFERENCE_CONFLICT. Missing card →404 ADMIN_FLASHCARD_NOT_FOUND.

Disable chỉ UPDATE is_active/updated_at. Card không còn ở M1 learning, không quiz được,
Progress M3 list/summary active-only; Progress/Quiz/XP history cũ không bị xóa.
Admin progress list xem cả card inactive để quản trị; user detail progress summary
tái sử dụng active-only rule M3. Quiz history vẫn dùng current flashcard text như M4:
sửa nghĩa/câu hỏi có thể thay đổi text được hiển thị trong lịch sử tham chiếu.

Admin user list tìm username/email/fullName, role USER/ADMIN/ALL(default).
DTO: id,username,email,fullName,avatarUrl,role,createdAt,lastLogin,totalXp,level,
currentStreak,longestStreak. Admin được phép xem email trong user management, không hash.
Detail trả user DTO trên, progress summary M3, quiz summary M4, gamification summary M5.
Progress items: flashcardId,english,vietnamese,topicCode,memoryLevel,reviewCount,correctCount,
wrongCount,isFavorite,lastReview,nextReview. Quiz items: id,flashcardId,question,
selectedAnswer nullable,correctAnswer,correct,answeredAt. Missing user →404 ADMIN_USER_NOT_FOUND.

API role patch không cho actor tự demote về USER:409 ADMIN_SELF_ROLE_CHANGE_FORBIDDEN.
Giữ nguyên role ADMIN cho chính mình là no-op hợp lệ. Có thể đổi role user khác.
Role change response là admin account DTO, không hash. Không hard-code admin username/ID.

### Dashboard, Quiz statistics và leaderboard

Dashboard users.total, flashcards.total/active/inactive, gamification.totalXp và topUsers
là số liệu hiện tại toàn thời gian. topUsers tối đa5 có XP>0, gồm userId,username,fullName,
totalXp,level,currentStreak; không email/avatar/hash. Quiz totals, topTopics và quizByDay
theo range. topTopics tối đa5 gồm code,name,quizCount, sort count DESC/code ASC.

range default7d: bảy UTC calendar dates gồm hôm nay; 30d tương tự. Server map enum sang
số7/30 hoặc NULL, không đưa interval/SQL từ client vào query. /stats/quiz có thêm topicCode;
topic không tồn tại →404 ADMIN_TOPIC_NOT_FOUND. Totals: totalAnswers,correctAnswers,
wrongAnswers,accuracy (phần trăm làm tròn2 số; null khi0 answers). byTopic gồm code,name và
cùng các totals; byDay gồm date,answers,correct,wrong. 7d/30d sinh đủ ngày với zero counts;
all chỉ trả ngày có activity. Chỉ Quiz EN_TO_VI hiện hữu, không tạo type mới.

Quiz answered_at là TIMESTAMP without time zone của baseline. Stats diễn giải timestamp
theo DB session TimeZone rồi đổi UTC trước khi nhóm ngày. Giữ DB timezone ổn định qua
lịch sử ghi dữ liệu; nếu DB timezone thay đổi sau này cần kế hoạch chuyển đổi timestamp.
XP/streak M5 không sửa.

Leaderboard chỉ XP>0, RANK() theo XP DESC trước LIMIT/OFFSET, tie cùng rank (1/1/3),
userId ASC trong tie. Response phân trang với rank,userId,username,fullName,avatarUrl,
totalXp,level,currentStreak,longestStreak. Rank là rank toàn hệ thống, không reset mỗi trang.
Level derive floor(totalXp/100)+1, không lưu DB.

SQL tham số hóa toàn bộ giá trị request. Các fragment dynamic chỉ là constants nội bộ.
Admin DTO dùng projection tường minh, không SELECT * hoặc spread raw rows. Bigint/count
đổi Number có kiểm tra safe integer. Count/page và dashboard aggregates dùng các SELECT
riêng, có thể lệch nhẹ khi dữ liệu đang thay đổi; không hứa snapshot đồng nhất đa query.

### M6A verification và integration source

Local commands được phép trong source task: npm install, npm run typecheck,
npm run build, npm test. Xem kết quả riêng M1–M6 trong verification-report.txt.
Kết quả M6A: install/typecheck/build PASS, 311/311 tests, 13 suites, 0 fail/skip.
M6A thêm101 tests:88 HTTP/service,5 CLI argument validation,8 SQL/repository contracts.
Toàn bộ 210 test/assertions M1–M5 giữ nguyên. Unit/API dùng real Express/JWT với repository
doubles, thêm SQL parameter/ordering contracts và CLI parser tests; không chạy CLI thật.

**DATABASE MIGRATION NOT APPLIED**
**ADMIN DATABASE INTEGRATION NOT TESTED**

Integration source: tests/admin.database.integration.ts, support/admin-integration.repository.ts.
ADMIN_INTEGRATION_ALLOW_WRITES=1 được kiểm tra trước createPool. Không thuộc npm test mặc định.
Suite typechecked nhưng không thực thi trong task. Không claim SQL/database PASS từ doubles.

Sau review source, bạn tự chọn development DB và thực hiện tuần tự:
local checks → tự migrate:up → verify users.role/CHECK → Admin integration →
M5/M4/M3/M2/M1 database regressions → manual Admin API. Không migrate:down/reseed baseline.

~~~powershell
$env:ADMIN_INTEGRATION_ALLOW_WRITES = '1'
try { npm run test:admin:integration }
finally { Remove-Item Env:ADMIN_INTEGRATION_ALLOW_WRITES }
~~~

Suite source kiểm tra role, các bảng cũ, index dedup baseline, snapshot dataset;
tạo UUID users A/B, promote đúng A bằng exact ID+username+email. Tạo topic ADM_<random>
và5 cards qua Admin API; kiểm tra search/filter/page, duplicate, update/status,
public learning/Quiz inactive, Progress/Quiz/XP cho B qua API production, dashboard,
user detail/progress/history, stats/leaderboard. Promote/demote B qua API làm cùng JWT
nhận/mất quyền; tự demote A bị chặn. Baseline được đối chiếu cả trong và sau cleanup.

Cleanup theo user trước rồi cards/topic: delete user exact ID AND username AND email,
verify cả progress/results/scores/history cascade sạch. Card cleanup dùng exact card ID
AND topic ID AND unique code; topic cleanup dùng ID AND code. Nếu response create lỗi sau
commit, recover IDs qua đúng UUID identity/unique topic code trước cleanup. Không prefix
delete/broad delete/truncate. Không được xóa FOOD hoặc card/user có sẵn. Topic chỉ xóa sau
khi xác nhận các owned cards đã hết; errors được tập hợp thay vì bỏ qua cleanup user thứ hai.
Sequence tăng là bình thường. Chạy suite khi không có workload khác sửa baseline/ranking;
tránh qua UTC midnight trong lúc kiểm tra biểu đồ ngày.

### Chuẩn bị M6B

BIPASSION Admin sẽ dùng auth/login rồi admin/me để xác nhận quyền; dashboard, Topics,
Flashcards, Users, Progress user, Quiz Statistics, Leaderboard và Profile/Settings có
read API tương ứng. Chưa có API sửa profile/settings trong M6A và chưa xây UI.
Tên package, prefix /api/v1 và các public API hiện hữu được giữ nguyên.

Giới hạn: chưa thực thi PostgreSQL migration/integration hoặc CLI; kiểm chứng SQL thực tế
còn chờ bước user tự chạy. Không có last-admin policy/audit log/rate limiting mới.
Không lưu role trong JWT. Các giới hạn M5 trước đây về Quiz option IDs và INTEGER XP vẫn giữ.

