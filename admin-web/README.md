# BiVoca Admin Panel — Milestone 6B

Admin Web riêng cho backend M1–M6A hiện có. Giao diện tiếng Việt, dữ liệu lấy trực tiếp qua API; không có dữ liệu demo hoặc chế độ mock trong ứng dụng.

## Dùng hằng ngày trên máy Windows đã cài tự khởi động

Mở **http://localhost:5173**. Admin và backend chạy nền khi đăng nhập Windows,
không cần mở terminal. Xem [hướng dẫn bật/tắt và cập nhật](../maintenance/LOCAL_ADMIN.md).

## Chạy development

Yêu cầu Node.js >= 22.12, npm và backend M6A đã cấu hình sẵn. Đã kiểm tra bằng Node.js 24.19.0 / npm 11.17.0.

Mở hai terminal từ thư mục project:

```powershell
# Terminal 1
cd backend
npm run dev
```

```powershell
# Terminal 2
cd admin-web
npm install
npm run dev
```

Mở <http://localhost:5173> và đăng nhập bằng tài khoản ADMIN hiện có. Không cần tạo file env cho frontend ở cấu hình mặc định.

Vite proxy `/api` tới `http://127.0.0.1:3000`; API client dùng `/api/v1`. Port 5173 có `strictPort`, báo lỗi nếu đã bị chiếm. Khi tái lập chính xác dependencies từ lockfile, dùng `npm ci` thay `npm install`.

## Stack và cấu trúc

React 19, TypeScript strict, Vite 8, React Router 7, TanStack Query 5, fetch wrapper, Recharts 3, Lucide React, Tailwind CSS 4. Vitest + React Testing Library kiểm tra auth, routes và hợp đồng request. Prettier định dạng source.

```text
src/
  api/                 Fetch, lỗi thân thiện, auth và Admin API
  components/
    layout/            Sidebar, topbar, Brand
    ui/                Table, form fields, pagination, dialog, toast, states
    charts/            Quiz chart, tải Recharts khi có dữ liệu
  features/auth/       AuthSession và AuthProvider
  hooks/               Debounce/pagination, topic options, responsive layout
  lib/                 Validation, format, query defaults, invalidation
  pages/               Các trang nghiệp vụ và form tạo/sửa
  routes/              Guard và route tree
  types/               DTO theo service/schema backend M6A
tests/                 Fixtures biệt lập, không được import vào src/
```

API contracts được đối chiếu với `backend/src/modules/admin/admin.route.ts`, `admin.schema.ts`, `admin.service.ts`, `admin.types.ts` và auth M2. Backend, migration và dataset không cần sửa.

## Routes

| Route | Chức năng |
| --- | --- |
| `/login` | Đăng nhập và xác nhận quyền |
| `/admin` | Dashboard, 4 KPI, Quiz chart, top learners/topics |
| `/admin/dashboard` | Redirect về `/admin` |
| `/admin/topics` | Danh sách, search, phân trang, tạo/sửa chủ đề |
| `/admin/flashcards` | Search, topic/status/level, tạo/sửa, bật/tắt |
| `/admin/users` | Danh sách người dùng, search/role |
| `/admin/progress` | Chọn người dùng để xem tiến độ |
| `/admin/users/:id` | Account và các summary, đổi role |
| `/admin/users/:id/progress` | Tiến độ read-only, search/phân trang |
| `/admin/users/:id/quiz-history` | Lịch sử Quiz read-only, phân trang |
| `/admin/quiz-statistics` | Range/topic, KPI, chart, bảng theo chủ đề |
| `/admin/leaderboard` | Phân trang, rank trực tiếp từ API |

`/` và route ngoài Admin redirect tới `/admin`, rồi guard quyết định quyền. Route Admin không tồn tại hiển thị trang không tìm thấy.

## API mapping

Tất cả các path bên dưới có prefix `/api/v1`.

| Method / path | Query hoặc body | UI |
| --- | --- | --- |
| POST `/auth/login` | `{email, password}` | Login |
| GET `/admin/me` | Bearer token | Kiểm tra quyền và restore |
| GET `/admin/dashboard` | `range=7d/30d/all` | Dashboard |
| GET `/admin/topics` | `page, limit, search` | Topics, topic options |
| POST `/admin/topics` | `code, name, description, imageUrl` | Tạo chủ đề |
| PUT `/admin/topics/:id` | Cùng body tạo | Sửa chủ đề |
| GET `/admin/flashcards` | `page, limit, search, topicCode?, status, level?` | Flashcards |
| POST `/admin/flashcards` | CardInput + `isActive` | Tạo từ vựng |
| PUT `/admin/flashcards/:id` | CardInput, **không có isActive** | Sửa nội dung |
| PATCH `/admin/flashcards/:id/status` | `{isActive: boolean}` | Bật/tắt có xác nhận |
| GET `/admin/users` | `page, limit, search, role=ALL/USER/ADMIN` | Users |
| GET `/admin/users/:id` | ID SERIAL hợp lệ | Account và summaries |
| GET `/admin/users/:id/progress` | `page, limit, search` | Progress |
| GET `/admin/users/:id/quiz-history` | `page, limit` | Quiz History |
| PATCH `/admin/users/:id/role` | `{role: USER/ADMIN}` | Role confirmation |
| GET `/admin/stats/quiz` | `range, topicCode?` | Quiz Statistics |
| GET `/admin/leaderboard` | `page, limit` | Leaderboard |

CardInput: `topicCode, english, vietnamese, pronunciation, partOfSpeech, example, exampleVi, level`.
Không gửi query rỗng cho các field optional. Page/limit là số nguyên canonical, mặc định 1/20; UI cho chọn 10/20/50. Topic options tải các trang limit 100 tới hết, không giới hạn ở 100 chủ đề đầu.

## Auth và cache

1. Login public, nhận token rồi gọi `/admin/me` bằng token đó.
2. Chỉ chấp nhận response có `role=ADMIN`; sau xác nhận mới lưu token vào `sessionStorage`. Không lưu role/admin object, không decode JWT để cấp quyền.
3. Refresh với token đã lưu phải gọi `/admin/me` trước khi render protected route. Storage bị chặn thì session hoạt động trong memory.
4. 401 hoặc 403 `ADMIN_FORBIDDEN` trên protected request kết thúc session, hủy queries, clear query cache và chuyển về login với thông báo.
5. Logout xóa token và cache. Abort/generation guard ngăn kết quả login cũ khôi phục session sau logout; 401 của token cũ không hủy session mới.
6. M9: restore gặp lỗi mạng/5xx hoặc 403 khác `ADMIN_FORBIDDEN` giữ token và báo lỗi. Nếu chưa xác nhận ADMIN, protected route vẫn bị chặn; nút “Thử kết nối lại” gọi lại `/admin/me` mà không cần nhập lại mật khẩu. Phiên ADMIN đã xác nhận không bị xóa cache vì lỗi tạm thời.

Bearer token trong sessionStorage là lựa chọn cho frontend hiện tại; không có refresh-token API trong phạm vi này. Các request có timeout 15 giây, cancellation và thông báo tiếng Việt theo error code. Không log token/password hay hiển thị raw SQL/internal error message.

Queries dùng keys `admin-dashboard`, `admin-topics`, `admin-flashcards`, `admin-users`, `admin-user`, `admin-user-progress`, `admin-user-quiz-history`, `admin-quiz-stats`, `admin-leaderboard`. Queries stale 30 giây, retry một lần cho lỗi tạm thời, không retry 4xx. Mutations không tự retry. Sau tạo/sửa topic, card, status: invalidate nội dung và summaries liên quan. Sau đổi role: invalidate users/detail. Không reload browser.

## UI và validation

Sidebar navy 242px, nền sáng `#F7F8FA`, primary blue `#2563EB`, table 13px, title 24px, border/radius nhẹ. Có empty/error/loading states, table scroll, tooltip/title cho text dài, labels và focus visible. Sidebar mobile hỗ trợ Escape, giữ focus trong menu và không cho tab vào nội dung ẩn. Form dùng native modal dialog để focus trap và Escape.

Search debounce 300ms, đổi search/filter/page size trở về page 1. Mutation lỗi giữ form mở; thành công có toast và refresh đúng query. Mã chủ đề phải uppercase theo regex backend, chỉ trim; không tự uppercase. Field optional trống gửi null. Dữ liệu legacy thiếu Level phải được chọn rõ khi sửa. `accuracy=null` hiện `—`; chart không có answers hiện empty state.

Đổi role cần confirm; target ID bằng current admin ID thì button bị disable, không hard-code ID. Backend tiếp tục là lớp bảo vệ quyền thực tế. Progress/history chỉ xem. Không có DELETE, reset XP, user giả, activity/notification giả hoặc tính năng thiếu API.

## Kiểm tra

```powershell
npm run typecheck
npm test
npm run build
npm run format:check
```

Kết quả M6B cũ ở `../admin_milestone6b_review.txt`. M9 ngày 2026-10-14: **42/42 tests**, build và format PASS, log ở `.m9-verification/`. Test dùng fetch fixtures riêng, không gọi PostgreSQL hay tạo/đổi role user thật. jsdom mô phỏng API dialog để kiểm tra form; không thay cho kiểm chứng focus native trong browser.

## Manual acceptance với backend thật

Checklist dưới đây là bản bàn giao M6B. M9 đã kiểm tra browser thật các màn hình đọc, search/filter/pagination, validation form, restore và Logout/guard; mutation API đã PASS trên PostgreSQL test riêng. Chi tiết từng bằng chứng và phép thử chưa chạy: `../M9_INTEGRATION_SECURITY_REPORT.md`. Không xem mọi checkbox cũ là kết quả M9.

- [ ] Chưa đăng nhập: mở `/admin/flashcards` chuyển về `/login`.
- [ ] Email/password trống, sai credentials, backend không kết nối: lỗi rõ và submit không bị kẹt.
- [ ] ADMIN login: `/auth/login` → `/admin/me` → Dashboard. USER bị chặn với thông báo quyền.
- [ ] Refresh: xác nhận lại `/admin/me`; token hết hạn/role bị thu hồi kết thúc session.
- [ ] Dashboard: so KPI/chart/top users/topics với response API; đổi 7d/30d/all.
- [ ] Topics: search/phân trang/page size; tạo/sửa hợp lệ; duplicate code/name giữ form và báo lỗi.
- [ ] Flashcards: topic/status/level/search; tạo; sửa PUT không gửi isActive; bật/tắt chỉ PATCH status sau confirm.
- [ ] Users: search/role/page; chi tiết hiển thị account và ba summaries.
- [ ] Current admin không tự hạ quyền. Với user được phép thử, role change chỉ gửi sau confirm; không đổi role thật chỉ để chạy checklist.
- [ ] Progress: search/phân trang, memory 0..5, favorite, last review; không có nút sửa.
- [ ] Quiz History: đáp án/kết quả/thời gian/phân trang; không có nút sửa/xóa.
- [ ] Quiz Statistics: range/topic, byDay/byTopic; null accuracy hiện dấu gạch.
- [ ] Leaderboard: rank từ API, kể cả trang sau/tie.
- [ ] Logout: về login, xóa token/cache. Back hoặc vào URL trực tiếp không lộ dữ liệu cũ.
- [ ] Desktop/tablet/mobile: sidebar, table scroll, drawer, keyboard Tab/Escape; không tràn page.

## Deployment và giới hạn

`npm run build` tạo `dist/`. Production cần phục vụ SPA fallback tới `index.html` cho các routes và reverse proxy `/api` tới backend. Vite development proxy không tự áp dụng ở production.

Nếu dùng API origin khác, cấu hình `VITE_API_BASE_URL` bằng base đầy đủ kết thúc ở `/api/v1` tại build time. `VITE_*` là thông tin public phía client, không đặt secrets vào đây. Origin khác cần cấu hình CORS/deployment tương ứng. Backend CORS không bị sửa trong M6B.

M9: người dùng đăng nhập trực tiếp bằng ADMIN hiện có; agent kiểm tra các protected screens trên backend/PostgreSQL thật. Chưa chạy browser E2E cho mọi mutation, USER login, expired-token hoặc fault injection network/5xx; các phần này có bằng chứng API integration và UI/security tests riêng. Không thiết lập ESLint; TypeScript strict, tests, build và Prettier là các checks đã chọn. Không có thay đổi Android trong M9.

Tài liệu chính thức đã tham khảo: [Vite](https://vite.dev/guide/), [Tailwind + Vite](https://tailwindcss.com/docs/installation/using-vite), [TanStack Query invalidation](https://tanstack.com/query/latest/docs/framework/react/guides/query-invalidation).
